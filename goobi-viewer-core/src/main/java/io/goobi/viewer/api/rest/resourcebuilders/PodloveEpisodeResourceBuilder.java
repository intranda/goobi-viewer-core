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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TRANSCRIPT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TRANSCRIPT_VTT;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.imaging.MediaHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.metadata.ComplexMetadata;
import io.goobi.viewer.model.metadata.ComplexMetadataContainer;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds the episode configuration (version 5) consumed by the Podlove Web Player for a podcast episode record.
 *
 * <p>
 * The episode is expected to be a top-level record whose first page holds the audio file. Chapters are read from the structure elements of the record
 * that carry a start time in {@link #FIELD_CHAPTER_START}. If the record belongs to a series (referenced in {@link #FIELD_SERIES_ID}), the series
 * group record is used as the podcast show. If the first page lists a WebVTT file in FILENAME_VTT or a JSON transcript in
 * {@link SolrConstants#FILENAME_JSON} and the caller may view the fulltext, the episode references the matching transcript endpoint, from which the
 * player loads the transcript itself. WebVTT files are preferred because they contain speakers.
 */
public class PodloveEpisodeResourceBuilder {

    private static final Logger logger = LogManager.getLogger(PodloveEpisodeResourceBuilder.class);

    /** Version of the Podlove Web Player configuration format. */
    public static final int PODLOVE_CONFIG_VERSION = 5;
    /** Solr field containing the title of episodes, shows and chapters. */
    public static final String FIELD_TITLE = "MD_TITLE";
    /** Solr field containing the publication date of an episode. */
    public static final String FIELD_PUBLICATION_DATE = "MD_DATEPUBLISH";
    /** Solr field containing the start time of a chapter within the episode audio. */
    public static final String FIELD_CHAPTER_START = "MD_TIME_START";
    /** Solr field of an episode containing the PI of the series (podcast) it belongs to. */
    public static final String FIELD_SERIES_ID = SolrConstants.PREFIX_GROUPID + "PODCAST";
    /** DOC STRUCT type of an audio record which represents a podcast episode and should therefore be presented by a podlove player */
    public static final String DOCSTRCT_PODCAST_EPISODE = "Podcastfolge";

    /** Metadata fields whose metadata documents are listed as contributors, in the order of the contributor groups. */
    static final List<String> CONTRIBUTOR_FIELDS = List.of("MD_AUTHOR", "MD_PUBLISHER");
    static final String FIELD_DISPLAY_FORM = "MD_DISPLAYFORM";
    /** Solr field of a page containing the file name of a WebVTT transcript. */
    static final String FIELD_FILENAME_VTT = SolrConstants.FILENAME + "_VTT";
    static final String FIELD_YEAR_PUBLISH = "MD_YEARPUBLISH";
    static final String FIELD_DURATION = "MD_SIZESOURCEPRINT";
    static final String FIELD_SUBTITLE = "MD_SUBTITLE";
    static final String FIELD_SUMMARY = "MD_ABSTRACT";
    static final String FIELD_FIRST_NAME = "MD_FIRSTNAME";
    static final String FIELD_LAST_NAME = "MD_LASTNAME";

    static final String FIELD_LANGUAGE = "MD_LANGUAGE";
    static final String FIELD_AUTHOR = "MD_AUTHOR";
    static final String FIELD_OWNER = "MD_OWNER";
    static final String FIELD_TOPIC = "MD_TOPIC";
    static final String FIELD_EXPLICIT = "MD_EXPLICIT";

    static final int POSTER_SIZE = 500;
    static final Pattern PATTERN_ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    static final Pattern PATTERN_CLOCK_TIME = Pattern.compile("^(?:(\\d+):)?(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,3}))?$");
    static final Pattern PATTERN_SECONDS = Pattern.compile("^\\d+(?:\\.\\d+)?$");

    private final AbstractApiUrlManager urls;
    private final HttpServletRequest request;

    /**
     * @param urls API URL manager used to build the audio file URLs
     * @param request current request, used for access checks
     */
    public PodloveEpisodeResourceBuilder(AbstractApiUrlManager urls, HttpServletRequest request) {
        this.urls = urls;
        this.request = request;
    }

    /**
     * Builds the Podlove episode configuration for the record with the given PI.
     *
     * @param pi persistent identifier of the episode record
     * @return Podlove episode configuration as JSON
     * @throws ContentNotFoundException if the record does not exist, is not visible to the caller or has no audio
     * @throws ServiceNotAllowedException if the caller may not listen to the audio of the record
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public JSONObject build(String pi)
            throws ContentNotFoundException, ServiceNotAllowedException, PresentationException, IndexUnreachableException, DAOException {
        String query = SearchHelper.buildFinalQuery("+" + SolrConstants.PI + ":\"" + pi + "\"", false, request, null);
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, null);
        if (doc == null) {
            throw new ContentNotFoundException("No record found for PI: " + pi);
        }
        StructElement episode = new StructElement(doc);
        PhysicalElement page = AbstractPageLoader.loadPage(episode, 1);
        if (page == null || page.getMediaType() == null || !page.getMediaType().isAudio()) {
            throw new ContentNotFoundException("No audio found for PI: " + pi);
        }
        if (!hasPermission(request, pi, page.getFileName(), IPrivilegeHolder.PRIV_VIEW_AUDIO)) {
            throw new ServiceNotAllowedException("Listening to audio not allowed for PI: " + pi);
        }

        JSONObject json = new JSONObject();
        json.put("version", PODLOVE_CONFIG_VERSION);
        json.put("title", getTitle(episode));
        String publicationDate = formatPublicationDate(episode.getMetadataValue(FIELD_PUBLICATION_DATE));
        if (publicationDate != null) {
            json.put("publicationDate", publicationDate);
        }
        putIfNotBlank(json, "poster", getPoster(episode));
        putIfNotBlank(json, "link", getLink(episode));

        StructElement show = loadSeries(episode);
        if (show != null) {
            JSONObject showJson = new JSONObject();
            showJson.put("title", getTitle(show));
            putIfNotBlank(showJson, "poster", getPoster(show));
            putIfNotBlank(showJson, "link", getLink(show));
            putIfNotBlank(showJson, "subtitle", show.getMetadataValue(FIELD_SUBTITLE));
            putIfNotBlank(showJson, "summary", show.getMetadataValue(FIELD_SUMMARY));
            json.put("show", showJson);
        }

        JSONArray audio = buildAudio(urls, pi, page);
        json.put("audio", audio);
        if (hasPermission(request, pi, page.getFileName(), IPrivilegeHolder.PRIV_DOWNLOAD_IMAGES)) {
            json.put("files", audio);
        }

        //metadata
        putIfNotBlank(json, "publicationDate", buildMetadataField(doc, FIELD_YEAR_PUBLISH, PATTERN_ISO_DATE));
        putIfNotBlank(json, "duration", buildMetadataField(doc, FIELD_DURATION, PATTERN_CLOCK_TIME));
        putIfNotBlank(json, "subtitle", buildMetadataField(doc, FIELD_SUBTITLE));
        putIfNotBlank(json, "summary", buildMetadataField(doc, FIELD_SUMMARY));

        json.put("chapters", buildChapters(loadChapterDocs(pi)));
        json.put("contributors", buildContributors(episode, BeanUtils.getLocale()));

        // Podlove fetches the transcript itself if the value is a URL; access is checked again by the transcript endpoints
        putIfNotBlank(json, "transcripts", getTranscriptUrl(pi, loadPageDoc(pi, page.getOrder())));

        return json;
    }

    /**
     * Creates Podlove contributors from the metadata documents of the given episode. Each field in {@link #CONTRIBUTOR_FIELDS} becomes a contributor
     * group titled with the translated field name.
     *
     * @param episode episode record
     * @param locale locale for group titles and metadata values
     * @return Podlove contributors
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static JSONArray buildContributors(StructElement episode, Locale locale) throws PresentationException, IndexUnreachableException {
        ComplexMetadataContainer mdDocs = episode.getMetadataDocuments();
        JSONArray ret = new JSONArray();
        if (mdDocs == null) {
            return ret;
        }
        for (String field : CONTRIBUTOR_FIELDS) {
            List<ComplexMetadata> metadata = mdDocs.getMetadata(field);
            if (metadata.isEmpty()) {
                continue;
            }
            String groupTitle = ViewerResourceBundle.getTranslation(field, locale);
            for (ComplexMetadata md : metadata) {
                JSONObject contributor = buildContributor(md, field, groupTitle, locale);
                if (contributor != null) {
                    ret.put(contributor);
                }
            }
        }

        return ret;
    }

    /**
     * Creates a single Podlove contributor. The player only lists contributors that belong to a group, so the metadata field is used as group. No
     * avatar is set because persons have no images.
     *
     * @param md metadata document of the person
     * @param field metadata field, used as group id
     * @param groupTitle displayed title of the group
     * @param locale locale for metadata values
     * @return Podlove contributor; null if no name could be determined
     * @should build contributor with group
     * @should return null if name is missing
     */
    static JSONObject buildContributor(ComplexMetadata md, String field, String groupTitle, Locale locale) {
        String name = getContributorName(md, locale);
        if (StringUtils.isBlank(name)) {
            return null;
        }
        JSONObject group = new JSONObject();
        group.put("id", field);
        group.put("slug", field.toLowerCase());
        group.put("title", StringUtils.isNotBlank(groupTitle) ? groupTitle : field);

        JSONObject contributor = new JSONObject();
        contributor.put("id", StringUtils.isNotBlank(md.getId()) ? md.getId() : field + "_" + name);
        contributor.put("name", name);
        contributor.put("group", group);
        return contributor;
    }

    /**
     * @param md metadata document of a person or corporation
     * @param locale locale for metadata values
     * @return display form, else first and last name, else the plain metadata value; empty string if none exists
     * @should prefer display form
     * @should combine first and last name
     * @should fall back to metadata value
     */
    static String getContributorName(ComplexMetadata md, Locale locale) {
        String displayForm = md.getFirstValue(FIELD_DISPLAY_FORM, locale);
        if (StringUtils.isNotBlank(displayForm)) {
            return displayForm.trim();
        }
        // MD_VALUE of persons is usually "Last name, First name", so the name parts are preferred
        String name = StringUtils.normalizeSpace(md.getFirstValue(FIELD_FIRST_NAME, locale) + " " + md.getFirstValue(FIELD_LAST_NAME, locale));
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        return StringUtils.trimToEmpty(md.getFirstValue(SolrConstants.MD_VALUE, locale));
    }

    /**
     * Creates one audio source per available mime type of the given page.
     *
     * @param urls API URL manager used to build the audio file URLs
     * @param pi persistent identifier of the record
     * @param page page holding the audio file
     * @return audio sources with url, mimeType, size and title
     */
    static JSONArray buildAudio(AbstractApiUrlManager urls, String pi, PhysicalElement page) {
        MediaHandler mediaHandler = new MediaHandler(urls);
        // Several formats may map to the same mime type (e.g. mp3 and mpeg); only the first one is used
        Map<String, String> formatsByMimeType = new LinkedHashMap<>();
        for (String format : page.getMediaFormats("audio")) {
            formatsByMimeType.putIfAbsent(getAudioMimeType(format), format);
        }

        JSONArray ret = new JSONArray();
        for (Map.Entry<String, String> entry : formatsByMimeType.entrySet()) {
            String format = entry.getValue();
            try {
                JSONObject source = new JSONObject();
                source.put("url", mediaHandler.getMediaUrl("audio", format, pi, page.getFileNameForFormat(format)));
                source.put("mimeType", entry.getKey());
                source.put("size", page.getFileSize());
                source.put("title", format.toUpperCase());
                ret.put(source);
            } catch (IllegalRequestException e) {
                logger.error("Cannot build audio URL for {} in format {}: {}", pi, format, e.getMessage());
            }
        }

        return ret;
    }

    /**
     * Loads the series group record the given episode belongs to.
     *
     * @param episode episode record
     * @return series record; null if the episode does not belong to a series or the series record does not exist
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static StructElement loadSeries(StructElement episode) throws PresentationException, IndexUnreachableException {
        String seriesPi = episode.getMetadataValue(FIELD_SERIES_ID);
        if (StringUtils.isBlank(seriesPi)) {
            return null;
        }
        String query = new StringBuilder().append('+')
                .append(SolrConstants.PI)
                .append(":\"")
                .append(seriesPi)
                .append("\" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.GROUP)
                .toString();
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, null);
        if (doc == null) {
            logger.warn("Series {} referenced by {} not found", seriesPi, episode.getPi());
            return null;
        }

        return new StructElement(doc);
    }

    /**
     * Loads the transcript related fields of the page with the given order.
     *
     * @param pi persistent identifier of the record
     * @param order order of the page
     * @return Solr document of the page; null if not found
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static SolrDocument loadPageDoc(String pi, int order) throws PresentationException, IndexUnreachableException {
        String query = new StringBuilder().append('+')
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(":\"")
                .append(pi)
                .append("\" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.PAGE)
                .append(" +")
                .append(SolrConstants.ORDER)
                .append(':')
                .append(order)
                .toString();
        return DataManager.getInstance().getSearchIndex().getFirstDoc(query, Arrays.asList(FIELD_FILENAME_VTT, SolrConstants.FILENAME_JSON));
    }

    /**
     * Returns the URL of the transcript endpoint for the given page. A WebVTT file is preferred over a JSON transcript because it contains speakers;
     * each file is only used if the caller may view its fulltext.
     *
     * @param pi persistent identifier of the record
     * @param pageDoc Solr document of the page
     * @return transcript URL; null if the page has no accessible transcript
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private String getTranscriptUrl(String pi, SolrDocument pageDoc) throws IndexUnreachableException, DAOException {
        String vttFileName = getTranscriptFileName(pageDoc, FIELD_FILENAME_VTT, "vtt");
        if (vttFileName != null && hasPermission(request, pi, vttFileName, IPrivilegeHolder.PRIV_VIEW_FULLTEXT)) {
            return buildTranscriptUrl(urls, pi, RECORDS_FILES_TRANSCRIPT_VTT, vttFileName);
        }
        String jsonFileName = getTranscriptFileName(pageDoc, SolrConstants.FILENAME_JSON, "json");
        if (jsonFileName != null && hasPermission(request, pi, jsonFileName, IPrivilegeHolder.PRIV_VIEW_FULLTEXT)) {
            return buildTranscriptUrl(urls, pi, RECORDS_FILES_TRANSCRIPT, jsonFileName);
        }
        return null;
    }

    /**
     * Returns the simple name of the transcript file listed in the given field of the page document.
     *
     * @param pageDoc Solr document of the page
     * @param field Solr field containing the file name
     * @param extension required file extension
     * @return transcript file name without path; null if the page has no such transcript
     * @should return file name without path
     * @should return null if page has no transcript
     * @should return null if file has wrong extension
     */
    static String getTranscriptFileName(SolrDocument pageDoc, String field, String extension) {
        if (pageDoc == null) {
            return null;
        }
        Object value = pageDoc.getFirstValue(field);
        if (value == null || StringUtils.isBlank(value.toString())) {
            return null;
        }
        String fileName = FilenameUtils.getName(value.toString().trim());
        if (!extension.equalsIgnoreCase(FilenameUtils.getExtension(fileName))) {
            logger.warn("Ignoring transcript in {} that is not a {} file: {}", field, extension, value);
            return null;
        }
        return fileName;
    }

    /**
     * @param urls API URL manager
     * @param pi persistent identifier of the record
     * @param endpoint transcript endpoint path below {@link io.goobi.viewer.api.rest.v1.ApiUrls#RECORDS_FILES}
     * @param fileName file name of the transcript
     * @return URL of the transcript endpoint for the given file
     * @should build transcript endpoint url
     * @should build vtt transcript endpoint url
     */
    static String buildTranscriptUrl(AbstractApiUrlManager urls, String pi, String endpoint, String fileName) {
        return urls.path(RECORDS_FILES, endpoint).params(pi, fileName).build();
    }

    /**
     * Loads all structure elements of the given record that carry a chapter start time.
     *
     * @param pi persistent identifier of the record
     * @return Solr documents of the chapters
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static List<SolrDocument> loadChapterDocs(String pi) throws PresentationException, IndexUnreachableException {
        String query = new StringBuilder().append('+')
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(":\"")
                .append(pi)
                .append("\" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.DOCSTRCT)
                .append(" +")
                .append(FIELD_CHAPTER_START)
                .append(":*")
                .toString();
        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, SolrSearchIndex.MAX_HITS, null, Arrays.asList(SolrConstants.LABEL, FIELD_TITLE, FIELD_CHAPTER_START));
        return docs != null ? docs : new ArrayList<>();
    }

    /**
     * Creates Podlove chapters from the given Solr documents, sorted by start time. Documents without a valid start time are skipped.
     *
     * @param docs Solr documents of the chapters
     * @return Podlove chapters
     * @should sort chapters by start time and skip invalid ones
     * @should fall back to label if title is missing
     */
    static JSONArray buildChapters(List<SolrDocument> docs) {
        List<JSONObject> chapters = new ArrayList<>();
        for (SolrDocument doc : docs) {
            String start = formatNormalPlayTime((String) doc.getFirstValue(FIELD_CHAPTER_START));
            if (start == null) {
                logger.warn("Skipping chapter with invalid start time: {}", doc.getFirstValue(FIELD_CHAPTER_START));
                continue;
            }
            String title = (String) doc.getFirstValue(FIELD_TITLE);
            if (StringUtils.isBlank(title)) {
                title = (String) doc.getFirstValue(SolrConstants.LABEL);
            }
            JSONObject chapter = new JSONObject();
            chapter.put("start", start);
            chapter.put("title", StringUtils.defaultString(title));
            chapters.add(chapter);
        }
        // The normalized format has fixed width (unless above 99 hours), so lexicographic order equals chronological order
        chapters.sort(Comparator.comparing(c -> c.getString("start")));

        return new JSONArray(chapters);
    }

    /**
     * Returns the first value of the given Solr field.
     *
     * @param doc The solr document containing the values
     * @param solrField The name of the solr field
     * @return the first value as string; an empty string if the field has no value
     */
    static String buildMetadataField(SolrDocument doc, String solrField) {
        return buildMetadataField(doc, solrField, null);
    }

    /**
     * Returns the first value of the given Solr field that matches the given pattern entirely.
     *
     * @param doc The solr document containing the values
     * @param solrField The name of the solr field
     * @param filterRegex pattern that allowed values must match; if null, every value is allowed
     * @return the first allowed value as string; an empty string if there is none
     */
    static String buildMetadataField(SolrDocument doc, String solrField, Pattern filterRegex) {
        Collection<Object> values = doc.getFieldValues(solrField);
        if (values == null) {
            return "";
        }
        return values.stream()
                .map(Object::toString)
                .filter(v -> filterRegex == null || filterRegex.matcher(v).matches())
                .findFirst()
                .orElse("");
    }

    /**
     * Converts a time value into the normal play time format "HH:MM:SS.mmm" expected by Podlove. Accepted inputs are "H:MM:SS(.mmm)", "MM:SS(.mmm)"
     * and plain seconds (optionally with fraction).
     *
     * @param value time value to convert
     * @return normalized time, or null if the value cannot be parsed
     * @should normalize hours minutes seconds
     * @should normalize minutes seconds
     * @should convert plain seconds
     * @should return null for invalid input
     */
    static String formatNormalPlayTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        long millis;
        if (PATTERN_SECONDS.matcher(trimmed).matches()) {
            millis = Math.round(Double.parseDouble(trimmed) * 1000);
        } else {
            Matcher matcher = PATTERN_CLOCK_TIME.matcher(trimmed);
            if (!matcher.matches()) {
                return null;
            }
            long hours = matcher.group(1) != null ? Long.parseLong(matcher.group(1)) : 0;
            long minutes = Long.parseLong(matcher.group(2));
            long seconds = Long.parseLong(matcher.group(3));
            if (seconds > 59 || (matcher.group(1) != null && minutes > 59)) {
                return null;
            }
            long fraction = matcher.group(4) != null ? Long.parseLong(StringUtils.rightPad(matcher.group(4), 3, '0')) : 0;
            millis = ((hours * 60 + minutes) * 60 + seconds) * 1000 + fraction;
        }

        return String.format("%02d:%02d:%02d.%03d", millis / 3_600_000, (millis / 60_000) % 60, (millis / 1000) % 60, millis % 1000);
    }

    /**
     * @param format media format key as used in the media URLs
     * @return mime type for the given audio format
     * @should map formats to mime types
     */
    static String getAudioMimeType(String format) {
        if ("mp3".equalsIgnoreCase(format)) {
            return "audio/mpeg";
        }
        return "audio/" + format.toLowerCase();
    }

    /**
     * @param value date value as indexed
     * @return ISO date if the value can be parsed, otherwise the value as is
     */
    private static String formatPublicationDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        LocalDateTime date = DateTools.parseDateFromString(value);
        if (date == null) {
            return value;
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * @param request current request; may be null
     * @param pi persistent identifier of the record
     * @param fileName name of the file to check
     * @param privilege privilege to check
     * @return true if the request has the given privilege for the file
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    static boolean hasPermission(HttpServletRequest request, String pi, String fileName, String privilege)
            throws IndexUnreachableException, DAOException {
        return AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession(false) : null, pi,
                fileName, privilege, request != null ? NetTools.getIpAddress(request) : null).isGranted();
    }

    static String getTitle(StructElement element) {
        String title = element.getMetadataValue(FIELD_TITLE);
        return StringUtils.isNotBlank(title) ? title : element.getLabel();
    }

    static String getPoster(StructElement element) {
        return getPoster(element, POSTER_SIZE);
    }

    static String getPoster(StructElement element, int size) {
        try {
            return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(element, size, size);
        } catch (RuntimeException e) {
            logger.warn("Cannot build poster URL for {}: {}", element.getPi(), e.getMessage());
            return null;
        }
    }

    static String getLink(StructElement element) {
        try {
            return element.getUrl();
        } catch (RuntimeException e) {
            logger.warn("Cannot build record URL for {}: {}", element.getPi(), e.getMessage());
            return null;
        }
    }

    private static void putIfNotBlank(JSONObject json, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            json.put(key, value);
        }
    }
}
