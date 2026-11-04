/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.resourcebuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Verifier;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONObject;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds the RSS 2.0 feed (with iTunes elements) of a podcast show.
 *
 * <p>
 * The feed serves the legal deposit mandate of the library. It is blocked from podcast directories ({@code itunes:block}, {@code podcast:block}), the
 * official listing remains the producers' own feed.
 */
public class PodcastFeedResourceBuilder {

    private static final Logger logger = LogManager.getLogger(PodcastFeedResourceBuilder.class);

    /** Media type of RSS feeds. */
    public static final String MEDIA_TYPE_RSS = "application/rss+xml";

    static final Namespace NS_ITUNES = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd");
    static final Namespace NS_ATOM = Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");
    static final Namespace NS_PODCAST = Namespace.getNamespace("podcast", "https://podcastindex.org/namespace/1.0");

    private static final List<String> PREFERRED_ENCLOSURE_TYPES = List.of("audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/m4a");

    /**
     * Channel data of the feed.
     *
     * @param title show title
     * @param link show URL in the viewer
     * @param feedUrl URL of the feed itself
     * @param description show description (required)
     * @param subtitle show subtitle; may be null
     * @param language ISO 639-1 language code; may be null
     * @param authors show authors
     * @param ownerEmail owner e-mail; may be null
     * @param ownerName owner name; may be null
     * @param categories iTunes categories, taken over unchanged
     * @param explicit explicit flag
     */
    record Channel(String title, String link, String feedUrl, String description, String subtitle, String language, List<String> authors,
            String ownerEmail, String ownerName, List<String> categories, boolean explicit) {
    }

    /**
     * Item (episode) data of the feed.
     *
     * @param guid episode PI
     * @param title episode title
     * @param link episode URL in the viewer
     * @param description episode description; may be null
     * @param subtitle episode subtitle; may be null
     * @param pubDate publication date; may be null
     * @param enclosureUrl audio URL
     * @param enclosureLength audio file size in bytes
     * @param enclosureType audio mime type
     * @param duration duration as "hh:mm:ss"; may be null
     * @param image episode image URL; may be null
     * @param explicit explicit flag
     */
    record Item(String guid, String title, String link, String description, String subtitle, LocalDateTime pubDate, String enclosureUrl,
            long enclosureLength, String enclosureType, String duration, String image, boolean explicit) {
    }

    private final AbstractApiUrlManager urls;
    private final HttpServletRequest request;

    /**
     * @param urls API URL manager used to build the audio file URLs
     * @param request current request, used for access checks
     */
    public PodcastFeedResourceBuilder(AbstractApiUrlManager urls, HttpServletRequest request) {
        this.urls = urls;
        this.request = request;
    }

    /**
     * Builds the RSS feed of the podcast show with the given PI.
     *
     * @param showPi persistent identifier of the show (GROUP document with GROUPTYPE GROUPID_PODCAST)
     * @param feedUrl URL of the feed itself, written as atom:link rel="self"
     * @return feed as UTF-8 XML
     * @throws ContentNotFoundException if no podcast show with this PI exists
     * @throws ServiceNotAllowedException if the request may not access the show
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should throw ContentNotFoundException if show missing
     * @should throw ContentNotFoundException if record is no podcast group
     */
    public String build(String showPi, String feedUrl)
            throws ContentNotFoundException, ServiceNotAllowedException, PresentationException, IndexUnreachableException, DAOException {
        // Loaded without access filter so that a show the request may not see results in 403 instead of 404
        String showQuery = new StringBuilder().append('+')
                .append(SolrConstants.PI)
                .append(":\"")
                .append(showPi)
                .append("\" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.GROUP)
                .append(" +")
                .append(SolrConstants.GROUPTYPE)
                .append(':')
                .append(PodloveEpisodeResourceBuilder.FIELD_SERIES_ID)
                .toString();
        SolrDocument showDoc = DataManager.getInstance().getSearchIndex().getFirstDoc(showQuery, null);
        if (showDoc == null) {
            throw new ContentNotFoundException("No podcast found for PI: " + showPi);
        }
        if (!AccessConditionUtils.checkAccessPermissionBySolrDoc(showDoc, showQuery, IPrivilegeHolder.PRIV_LIST, request).isGranted()) {
            throw new ServiceNotAllowedException("Access to podcast not allowed: " + showPi);
        }

        Channel channel = buildChannelData(new StructElement(showDoc), feedUrl);
        return toXml(buildDocument(channel, loadItems(showPi)));
    }

    private static Channel buildChannelData(StructElement show, String feedUrl) {
        String title = PodloveEpisodeResourceBuilder.getTitle(show);
        String summary = show.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_SUMMARY);
        List<String> authors = show.getMetadataValues(PodloveEpisodeResourceBuilder.FIELD_AUTHOR);
        return new Channel(title, PodloveEpisodeResourceBuilder.getLink(show), feedUrl, StringUtils.isNotBlank(summary) ? summary : title,
                show.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_SUBTITLE),
                toFeedLanguage(show.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_LANGUAGE)), authors,
                show.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_OWNER), authors.isEmpty() ? null : String.join(", ", authors),
                show.getMetadataValues(PodloveEpisodeResourceBuilder.FIELD_TOPIC),
                isExplicit(show.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_EXPLICIT)));
    }

    private List<Item> loadItems(String showPi) throws PresentationException, IndexUnreachableException, DAOException {
        String query = SearchHelper.buildFinalQuery(
                "+" + PodloveEpisodeResourceBuilder.FIELD_SERIES_ID + ":\"" + showPi + "\" +" + SolrConstants.ISWORK + ":true", false, request, null);
        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, SolrSearchIndex.MAX_HITS, List.of(new StringPair("GROUPORDER_PODCAST", "desc")), null);
        List<Item> ret = new ArrayList<>(docs.size());
        for (SolrDocument doc : docs) {
            StructElement episode = new StructElement(doc);
            PhysicalElement page = AbstractPageLoader.loadPage(episode, 1);
            if (page == null || page.getMediaType() == null || !page.getMediaType().isAudio()) {
                logger.debug("Episode {} has no audio, skipped", episode.getPi());
                continue;
            }
            if (!isIncluded(episode.getPi(), page)) {
                continue;
            }
            Item item = buildItemData(episode, doc, page);
            if (item != null) {
                ret.add(item);
            }
        }
        return ret;
    }

    /**
     * Decides whether an episode is part of the feed. Provisional rule: episodes the request may not listen to are left out entirely. This may change
     * to "include, but with a blocked link"; keep the decision in this method.
     *
     * @param pi persistent identifier of the episode
     * @param page page holding the audio file
     * @return true if the episode is included in the feed
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private boolean isIncluded(String pi, PhysicalElement page) throws IndexUnreachableException, DAOException {
        return PodloveEpisodeResourceBuilder.hasPermission(request, pi, page.getFileName(), IPrivilegeHolder.PRIV_VIEW_AUDIO);
    }

    private Item buildItemData(StructElement episode, SolrDocument doc, PhysicalElement page) {
        JSONArray audio = PodloveEpisodeResourceBuilder.buildAudio(urls, episode.getPi(), page);
        if (audio.isEmpty()) {
            logger.warn("No audio URL for episode {}, skipped", episode.getPi());
            return null;
        }
        // RSS allows only one enclosure per item
        JSONObject source = selectEnclosure(audio);
        return new Item(episode.getPi(), PodloveEpisodeResourceBuilder.getTitle(episode), PodloveEpisodeResourceBuilder.getLink(episode),
                episode.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_SUMMARY),
                episode.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_SUBTITLE), getPublicationDate(doc), source.getString("url"),
                source.optLong("size", 0), source.getString("mimeType"),
                formatDuration(episode.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_DURATION)),
                PodloveEpisodeResourceBuilder.getPoster(episode), isExplicit(episode.getMetadataValue(PodloveEpisodeResourceBuilder.FIELD_EXPLICIT)));
    }

    /**
     * Selects the audio source used as enclosure. Apple Podcasts and Spotify do not accept Ogg, so MP3 and MP4/M4A sources are preferred.
     *
     * @param audio Podlove audio sources (url, mimeType, size)
     * @return preferred source; the first source if none is preferred; null if there is none
     * @should prefer mpeg over ogg
     * @should fall back to first source
     */
    static JSONObject selectEnclosure(JSONArray audio) {
        if (audio.isEmpty()) {
            return null;
        }
        for (int i = 0; i < audio.length(); i++) {
            JSONObject source = audio.getJSONObject(i);
            if (PREFERRED_ENCLOSURE_TYPES.contains(source.optString("mimeType"))) {
                return source;
            }
        }
        return audio.getJSONObject(0);
    }

    /**
     * Converts an indexed language code into the ISO 639-1 code expected in the RSS {@code language} element.
     *
     * @param code language code as indexed (usually ISO 639-2)
     * @return ISO 639-1 code if known, otherwise the trimmed input; null if blank
     * @should convert iso 639-2 to 639-1
     * @should keep unknown or two letter codes
     * @should return null for blank
     */
    static String toFeedLanguage(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        String trimmed = code.trim();
        Language language = DataManager.getInstance().getLanguageHelper().getLanguage(trimmed);
        if (language != null && StringUtils.isNotBlank(language.getIsoCode6391())) {
            return language.getIsoCode6391();
        }
        return trimmed;
    }

    /**
     * @param value duration as indexed ("hh:mm:ss", "mm:ss" or seconds)
     * @return duration as "hh:mm:ss"; null if the value cannot be parsed
     * @should normalize to hours minutes seconds
     * @should return null for invalid input
     */
    static String formatDuration(String value) {
        String normalPlayTime = PodloveEpisodeResourceBuilder.formatNormalPlayTime(value);
        return normalPlayTime != null ? StringUtils.substringBefore(normalPlayTime, ".") : null;
    }

    /**
     * Determines the publication date of an episode: an ISO date in MD_YEARPUBLISH, else MD_DATEPUBLISH, else DATECREATED.
     *
     * @param doc Solr document of the episode
     * @return publication date; null if none is available
     * @should prefer iso year publish
     * @should fall back to date publish
     * @should fall back to date created
     * @should return null if no date
     */
    static LocalDateTime getPublicationDate(SolrDocument doc) {
        String yearPublish =
                PodloveEpisodeResourceBuilder.buildMetadataField(doc, PodloveEpisodeResourceBuilder.FIELD_YEAR_PUBLISH,
                        PodloveEpisodeResourceBuilder.PATTERN_ISO_DATE);
        if (StringUtils.isNotBlank(yearPublish)) {
            try {
                return LocalDate.parse(yearPublish).atStartOfDay();
            } catch (DateTimeParseException e) {
                // fall through
            }
        }
        String datePublish = PodloveEpisodeResourceBuilder.buildMetadataField(doc, PodloveEpisodeResourceBuilder.FIELD_PUBLICATION_DATE);
        if (StringUtils.isNotBlank(datePublish)) {
            LocalDateTime date = DateTools.parseDateFromString(datePublish);
            if (date != null) {
                return date;
            }
        }
        Object dateCreated = doc.getFirstValue(SolrConstants.DATECREATED);
        if (dateCreated instanceof Number millis) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis.longValue()), ZoneOffset.UTC);
        }
        return null;
    }

    /**
     * @param date date in UTC
     * @return date in RFC 822 format as required by RSS
     * @should format date
     */
    static String formatRfc822(LocalDateTime date) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(date.atZone(ZoneOffset.UTC));
    }

    /**
     * @param value value of MD_EXPLICIT; may be null
     * @return true if the value is "true" or "yes" (case-insensitive)
     * @should accept true and yes
     */
    static boolean isExplicit(String value) {
        return "true".equalsIgnoreCase(StringUtils.trim(value)) || "yes".equalsIgnoreCase(StringUtils.trim(value));
    }

    /**
     * Renders the feed document. Items are sorted by publication date, newest first, undated items last.
     *
     * @param channel channel data
     * @param items episodes
     * @return RSS 2.0 document
     * @should write required channel elements
     * @should write item elements
     * @should omit empty optional elements
     * @should write channel without items
     * @should sort items newest first with undated last
     * @should escape special characters
     * @should strip characters not allowed in xml
     */
    static Document buildDocument(Channel channel, List<Item> items) {
        Element rss = new Element("rss");
        rss.setAttribute("version", "2.0");
        rss.addNamespaceDeclaration(NS_ITUNES);
        rss.addNamespaceDeclaration(NS_ATOM);
        rss.addNamespaceDeclaration(NS_PODCAST);
        Element eleChannel = buildChannel(channel);
        rss.addContent(eleChannel);

        List<Item> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(Item::pubDate, Comparator.nullsLast(Comparator.reverseOrder())));
        for (Item item : sorted) {
            eleChannel.addContent(buildItem(item));
        }

        return new Document(rss);
    }

    /**
     * Creates all channel level elements. A channel image (itunes:image / image) belongs here once its source is clarified.
     *
     * @param channel channel data
     * @return channel element without items
     */
    private static Element buildChannel(Channel channel) {
        Element ele = new Element("channel");
        addText(ele, "title", null, channel.title());
        addText(ele, "link", null, channel.link());
        addText(ele, "description", null, channel.description());
        addText(ele, "language", null, channel.language());
        ele.addContent(new Element("link", NS_ATOM).setAttribute("href", cleanXmlText(channel.feedUrl()))
                .setAttribute("rel", "self")
                .setAttribute("type", "application/rss+xml"));
        addText(ele, "subtitle", NS_ITUNES, channel.subtitle());
        addText(ele, "summary", NS_ITUNES, channel.description());
        if (channel.authors() != null && !channel.authors().isEmpty()) {
            addText(ele, "author", NS_ITUNES, String.join(", ", channel.authors()));
        }
        if (StringUtils.isNotBlank(channel.ownerEmail())) {
            Element owner = new Element("owner", NS_ITUNES);
            addText(owner, "name", NS_ITUNES, channel.ownerName());
            addText(owner, "email", NS_ITUNES, channel.ownerEmail());
            ele.addContent(owner);
        }
        if (channel.categories() != null) {
            for (String category : channel.categories()) {
                if (StringUtils.isNotBlank(category)) {
                    ele.addContent(new Element("category", NS_ITUNES).setAttribute("text", cleanXmlText(category.trim())));
                }
            }
        }
        addText(ele, "explicit", NS_ITUNES, String.valueOf(channel.explicit()));
        addText(ele, "type", NS_ITUNES, "episodic");
        // The library feed is a legal deposit copy and must not be listed in podcast directories
        addText(ele, "block", NS_ITUNES, "Yes");
        addText(ele, "block", NS_PODCAST, "yes");
        return ele;
    }

    private static Element buildItem(Item item) {
        Element ele = new Element("item");
        addText(ele, "title", null, item.title());
        addText(ele, "link", null, item.link());
        addText(ele, "description", null, item.description());
        ele.addContent(new Element("guid").setAttribute("isPermaLink", "false").setText(cleanXmlText(item.guid())));
        if (item.pubDate() != null) {
            addText(ele, "pubDate", null, formatRfc822(item.pubDate()));
        }
        ele.addContent(new Element("enclosure").setAttribute("url", cleanXmlText(item.enclosureUrl()))
                .setAttribute("length", String.valueOf(item.enclosureLength()))
                .setAttribute("type", cleanXmlText(item.enclosureType())));
        addText(ele, "subtitle", NS_ITUNES, item.subtitle());
        addText(ele, "summary", NS_ITUNES, item.description());
        addText(ele, "duration", NS_ITUNES, item.duration());
        if (StringUtils.isNotBlank(item.image())) {
            ele.addContent(new Element("image", NS_ITUNES).setAttribute("href", cleanXmlText(item.image())));
        }
        addText(ele, "explicit", NS_ITUNES, String.valueOf(item.explicit()));
        addText(ele, "episodeType", NS_ITUNES, "full");
        return ele;
    }

    /**
     * Adds a child element with the given text, unless the text is blank.
     */
    private static void addText(Element parent, String name, Namespace ns, String text) {
        if (StringUtils.isNotBlank(text)) {
            parent.addContent((ns != null ? new Element(name, ns) : new Element(name)).setText(cleanXmlText(text)));
        }
    }

    /**
     * Removes characters that are not allowed in XML (e.g. control characters from imported metadata), which would otherwise make JDOM fail the whole
     * feed.
     *
     * @param text text to clean; may be null
     * @return text without illegal XML characters
     */
    private static String cleanXmlText(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().filter(Verifier::isXMLCharacter).forEach(sb::appendCodePoint);
        return sb.toString();
    }

    /**
     * @param doc feed document
     * @return document serialized as UTF-8 XML
     */
    static String toXml(Document doc) {
        return new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8")).outputString(doc);
    }
}
