/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.rss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.SimpleDateFormat;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrTools;

/**
 * <p>
 * RSSFeed class.
 * </p>
 */
public class RSSFeed {
    /**
     * 
     */
    private static final String DATE_FORMAT_STRING = "dd.MM.yyyy, hh:mm:ss";

    private static final Logger logger = LoggerFactory.getLogger(RSSFeed.class);

    /** Constant <code>FIELDS</code> */
    public static final String[] FIELDS = { SolrConstants.ACCESSCONDITION, SolrConstants.DATECREATED, SolrConstants.FILENAME, SolrConstants.FULLTEXT,
            SolrConstants.IDDOC, SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.IDDOC_PARENT,
            SolrConstants.ISANCHOR, SolrConstants.ISWORK, SolrConstants.LOGID, SolrConstants.MIMETYPE, SolrConstants.NUMVOLUMES,
            SolrConstants.PERSON_ONEFIELD,
            SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.PLACEPUBLISH, SolrConstants.PUBLISHER, SolrConstants.THUMBNAIL,
            SolrConstants.URN, SolrConstants.YEARPUBLISH, "MD_SHELFMARK" };

    /**
     * <p>
     * createRss.
     * </p>
     *
     * @param rootPath a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @return a {@link com.rometools.rome.feed.synd.SyndFeed} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static SyndFeed createRss(String rootPath, String query, int maxItems)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return createRss(rootPath, query, null, null, maxItems);
    }

    /**
     * <p>
     * getFieldsWithTranslation.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getFieldsWithTranslation(Locale locale) {
        List<String> allFields = new ArrayList<>();
        for (String string : FIELDS) {
            allFields.add(string);
            allFields.add(string + "_LANG_" + locale.getLanguage().toUpperCase());
        }
        return allFields;
    }

    /**
     * <p>
     * createRss.
     * </p>
     *
     * @param rootPath a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param language a {@link java.lang.String} object.
     * @return a {@link com.rometools.rome.feed.synd.SyndFeed} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static SyndFeed createRss(String rootPath, String query, List<String> filterQueries, String language, int maxItems)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        String feedType = "rss_2.0";

        Locale locale = null;
        if (StringUtils.isNotBlank(language)) {
            locale = Locale.forLanguageTag(language);
        }
        if (locale == null) {
            locale = Locale.GERMANY;
        }

        logger.trace("RSS query: {}", query);
        if (filterQueries != null) {
            logger.trace("RSS filter queries: {}", filterQueries.toString());
        }

        SyndFeed feed = new SyndFeedImpl();
        feed.setEncoding("utf-8");
        feed.setFeedType(feedType);

        feed.setTitle(DataManager.getInstance().getConfiguration().getRssTitle());
        feed.setLink(rootPath);
        feed.setDescription(DataManager.getInstance().getConfiguration().getRssDescription());
        feed.setLanguage(locale.getLanguage());
        feed.setCopyright(DataManager.getInstance().getConfiguration().getRssCopyrightText());
        feed.setPublishedDate(new Date());

        // feed.setImage(arg0)

        List<SyndEntry> entries = new ArrayList<>();

        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, 0, maxItems, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "desc")), null,
                        getFieldsWithTranslation(locale), filterQueries, null)
                .getResults();
        if (docs == null || docs.isEmpty()) {
            logger.trace("No hits");
            feed.setEntries(entries);
            return feed;
        }

        for (SolrDocument doc : docs) {
            boolean anchor = doc.containsKey(SolrConstants.ISANCHOR) && ((Boolean) doc.getFieldValue(SolrConstants.ISANCHOR));
            boolean child = !anchor
                    && (DocType.DOCSTRCT.name().equals(doc.getFieldValue(SolrConstants.DOCTYPE)) || doc.getFieldValue(SolrConstants.LOGID) != null)
                    && (!doc.containsKey(SolrConstants.ISWORK) || !((Boolean) doc.getFieldValue(SolrConstants.ISWORK)));
            boolean page = DocType.PAGE.name().equals(doc.getFieldValue(SolrConstants.DOCTYPE)) || doc.containsKey(SolrConstants.ORDER);
            // boolean ePublication = MimeType.APPLICATION.getName().equals(doc.getFieldValue(SolrConstants.MIMETYPE));
            SolrDocument topDoc = null;
            SolrDocument ownerDoc = null;
            if (child || page) {
                // Find top level docstruct to extract metadata such as DATECREATED
                List<SolrDocument> topDocs = DataManager.getInstance()
                        .getSearchIndex()
                        .search(new StringBuilder(SolrConstants.PI).append(':').append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT)).toString(), 0,
                                1, null, null, Arrays.asList(FIELDS))
                        .getResults();
                if (!topDocs.isEmpty()) {
                    topDoc = topDocs.get(0);
                }
                if (page) {
                    // Find page owner docstruct to extract its metadata
                    List<SolrDocument> ownerDocs = DataManager.getInstance()
                            .getSearchIndex()
                            .search(new StringBuilder(SolrConstants.IDDOC).append(':')
                                    .append(doc.getFieldValue(SolrConstants.IDDOC_PARENT))
                                    .toString(), 0, 1, null, null, Arrays.asList(FIELDS))
                            .getResults();
                    if (!ownerDocs.isEmpty()) {
                        ownerDoc = ownerDocs.get(0);
                    }
                }
            }

            String pi = (String) doc.getFirstValue(SolrConstants.PI_TOPSTRUCT);
            SyndEntry entry = new SyndEntryImpl();
            SyndContent description = new SyndContentImpl();
            String label = "";
            Long modified = null;
            String author = "";
            String authorRss = "";
            String publisher = "";
            String placeAndTime = "<strong>Published: </strong>";
            String descValue = "";
            String thumbnail = "";
            String urn = "";
            String bookSeries = "";
            int thumbWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
            int thumbHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
            boolean hasImages = SolrTools.isHasImages(doc);
            String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
            String mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
            PageType pageType = PageType.determinePageType(docStructType, mimeType, anchor, hasImages, false);

            for (String field : FIELDS) {
                Object value = doc.getFirstValue(field);
                Optional<Object> translatedValue = Optional.ofNullable(doc.getFirstValue(field + "_LANG_" + locale.getLanguage().toUpperCase()));
                value = translatedValue.orElse(value);
                // If the doc has no field value, try the owner doc (in case of pages)
                if (value == null && ownerDoc != null) {
                    value = ownerDoc.getFirstValue(field);
                }
                // If there is still no value, try the root doc
                if (value == null && topDoc != null) {
                    value = topDoc.getFirstValue(field);
                }
                if (value == null) {
                    continue;
                }

                switch (field) {
                    case SolrConstants.LABEL:
                        // It is important that LABEL comes before IDDOC_PARENT in the static field list!
                        label = (String) value;
                        break;
                    case SolrConstants.IDDOC_PARENT:
                        // TODO This query is executed O(size of feed) times.
                        SolrDocumentList hits = DataManager.getInstance()
                                .getSearchIndex()
                                .search(new StringBuilder(SolrConstants.IDDOC).append(':').append(value).toString(), 1, null,
                                        Collections.singletonList(SolrConstants.LABEL));
                        if (hits != null && hits.getNumFound() > 0) {
                            SolrDocument parent = hits.get(0);
                            Object fieldParentLabel = parent.getFieldValue(SolrConstants.LABEL);
                            if (fieldParentLabel != null) {
                                label = new StringBuilder((String) fieldParentLabel).append("; ").append(label).toString();
                                bookSeries = new StringBuilder("<strong>Book series: </strong>").append((String) fieldParentLabel)
                                        .append("<br />")
                                        .toString();
                            }
                        }
                        break;
                    case SolrConstants.PERSON_ONEFIELD:
                        authorRss = (String) value;
                        author = new StringBuilder(author).append("<strong>Author: </strong>").append(value).append("<br />").toString();
                        break;
                    case SolrConstants.PUBLISHER:
                        publisher = new StringBuilder("<strong>Publisher: </strong>").append(value).append("<br />").toString();
                        break;
                    case SolrConstants.DATECREATED:
                        modified = (Long) value;
                        break;
                    case SolrConstants.YEARPUBLISH:
                    case SolrConstants.PLACEPUBLISH:
                        if ("<strong>Published: </strong>".length() == placeAndTime.length()) {
                            placeAndTime = new StringBuilder(placeAndTime).append(value).toString();
                        } else {
                            placeAndTime = new StringBuilder(placeAndTime).append(", ").append(value).toString();
                        }
                        break;
                    case SolrConstants.URN:
                        urn = new StringBuilder("<strong>URL: </strong><a href=\"").append(rootPath)
                                .append("/resolver?urn=")
                                .append(value)
                                .append("\">")
                                .append(value)
                                .append("</a><br />")
                                .toString();
                        break;
                    case SolrConstants.PI:
                        pi = (String) value;
                        break;
                    case SolrConstants.TITLE:
                        if (StringUtils.isEmpty(label)) {
                            label = (String) value;
                        }
                        break;
                    case SolrConstants.FILENAME:
                    case SolrConstants.THUMBNAIL:
                        if (StringUtils.isEmpty(thumbnail)) {
                            thumbnail = (String) value;
                        }
                        break;
                    case SolrConstants.DOCSTRCT:
                        if (StringUtils.isEmpty(label)) {
                            label = (String) value;
                            label = ViewerResourceBundle.getTranslation(label, locale);
                        }
                        break;
                    default: // nothing
                }
            }
            if ("<strong>Published: </strong>".length() == placeAndTime.length()) {
                placeAndTime = "";
            } else {
                placeAndTime = new StringBuilder(placeAndTime).append("<br />").toString();
            }

            String recordUrl = DataManager.getInstance().getUrlBuilder().buildPageUrl(pi, 1, null, pageType);

            String imageUrl = BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(doc, thumbWidth, thumbHeight);

            String imageHtmlElement = null;
            if (StringUtils.isNotEmpty(pi) && StringUtils.isNotEmpty(imageUrl)) {
                imageHtmlElement = new StringBuilder("<a href=\"").append(rootPath)
                        .append('/')
                        .append(recordUrl)
                        .append("\">")
                        .append("<img style=\"margin-right:10px;\" src=\"")
                        .append(imageUrl)
                        .append("\" align=\"left\" border=\"0\" /></a>")
                        .toString();
            }

            entry.setAuthor(authorRss);
            entry.setTitle(label);
            entry.setLink(rootPath + "/" + recordUrl);
            if (modified != null) {
                try {
                    entry.setPublishedDate(new Date(modified));
                    entry.setUpdatedDate(entry.getPublishedDate());
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage());
                }
            }
            description.setType("text/html");
            descValue = new StringBuilder(imageHtmlElement != null
                    ? new StringBuilder(imageHtmlElement).append("<div style=\"display:block;margin-left:5px;\">").toString() : "").append("<p>")
                            .append(bookSeries)
                            .append(author)
                            .append(publisher)
                            .append(placeAndTime)
                            .append(urn)
                            .append("</p></div>")
                            .toString();
            description.setValue(descValue);
            entry.setDescription(description);
            entries.add(entry);
        }
        feed.setEntries(entries);

        // // TODO read information from config file

        // Image digiversoLogo = new Image();
        // // TODO ico funktioniert nicht, muss gif/png sein
        // digiversoLogo.setURL("http://www.intranda.com/images/favicon.ico");
        // digiversoLogo.setTitle("digiverso");
        // digiversoLogo.setLink("http://intranda.com");
        // myChannel.setImage(digiversoLogo);
        //

        return feed;
    }

    /**
     * <p>
     * createRssFeed.
     * </p>
     *
     * @param rootPath a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param rssFeedItems a int.
     * @return a {@link io.goobi.viewer.model.rss.Channel} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static Channel createRssFeed(String rootPath, String query, int rssFeedItems)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        return createRssFeed(rootPath, query, null, rssFeedItems, null);
    }

    /**
     * <p>
     * createRssFeed.
     * </p>
     *
     * @param rootPath a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param rssFeedItems a int.
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.rss.Channel} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static Channel createRssFeed(String rootPath, String query, List<String> filterQueries, int rssFeedItems, String language)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        // String feedType = "rss_2.0";

        Locale locale = null;
        if (StringUtils.isNotBlank(language)) {
            locale = Locale.forLanguageTag(language);
        }
        if (locale == null) {
            locale = Locale.GERMANY;
        }

        logger.trace("RSS query: {}", query);

        Channel feed = new Channel();
        feed.setTitle(DataManager.getInstance().getConfiguration().getRssTitle());
        feed.setLink(rootPath);
        feed.setDescription(DataManager.getInstance().getConfiguration().getRssDescription());
        feed.setLanguage(locale.getLanguage());
        feed.setCopyright(DataManager.getInstance().getConfiguration().getRssCopyrightText());
        feed.setPubDate(new Date());

        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, 0, rssFeedItems, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "desc")), null,
                        getFieldsWithTranslation(locale), filterQueries, null)
                .getResults();
        if (docs == null || docs.isEmpty()) {
            logger.trace("No hits");
            return feed;
        }

        for (SolrDocument doc : docs) {
            boolean anchor = doc.containsKey(SolrConstants.ISANCHOR) && ((Boolean) doc.getFieldValue(SolrConstants.ISANCHOR));
            boolean child = !anchor
                    && (DocType.DOCSTRCT.name().equals(doc.getFieldValue(SolrConstants.DOCTYPE)) || doc.getFieldValue(SolrConstants.LOGID) != null)
                    && (!doc.containsKey(SolrConstants.ISWORK) || !((Boolean) doc.getFieldValue(SolrConstants.ISWORK)));
            boolean page = DocType.PAGE.name().equals(doc.getFieldValue(SolrConstants.DOCTYPE)) || doc.containsKey(SolrConstants.ORDER);
            // boolean ePublication = MimeType.APPLICATION.getName().equals(doc.getFieldValue(SolrConstants.MIMETYPE));
            SolrDocument topDoc = null;
            SolrDocument ownerDoc = null;
            if (child || page) {
                // Find top level docstruct to extract metadata such as DATECREATED
                List<SolrDocument> topDocs = DataManager.getInstance()
                        .getSearchIndex()
                        .search(new StringBuilder(SolrConstants.PI).append(':').append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT)).toString(), 0,
                                1, null, null, Arrays.asList(FIELDS))
                        .getResults();
                if (!topDocs.isEmpty()) {
                    topDoc = topDocs.get(0);
                }
                if (page) {
                    // Find page owner docstruct to extract its metadata
                    List<SolrDocument> ownerDocs = DataManager.getInstance()
                            .getSearchIndex()
                            .search(new StringBuilder(SolrConstants.IDDOC).append(':')
                                    .append(doc.getFieldValue(SolrConstants.IDDOC_PARENT))
                                    .toString(), 0, 1, null, null, Arrays.asList(FIELDS))
                            .getResults();
                    if (!ownerDocs.isEmpty()) {
                        ownerDoc = ownerDocs.get(0);
                    }
                }
            }

            String pi = (String) doc.getFirstValue(SolrConstants.PI_TOPSTRUCT);
            RssItem entry = new RssItem();
            Description description = new Description();
            String label = "";
            Long modified = null;
            String author = "";
            String authorRss = "";
            String publisher = "";
            String placeAndTime = "";
            // String descValue = "";
            String thumbnail = "";
            String urn = "";
            String urnLink = "";
            String bookSeries = "";
            String shelfmark = "";

            int thumbWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
            int thumbHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
            boolean hasImages = SolrTools.isHasImages(doc);
            String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
            String mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
            PageType pageType = PageType.determinePageType(docStructType, mimeType, anchor, hasImages, false);
            entry.setDocType(ViewerResourceBundle.getTranslation(docStructType, locale));

            for (String field : FIELDS) {
                Object value = doc.getFirstValue(field);
                Optional<Object> translatedValue = Optional.ofNullable(doc.getFirstValue(field + "_LANG_" + locale.getLanguage().toUpperCase()));
                value = translatedValue.orElse(value);
                // If the doc has no field value, try the owner doc (in case of pages)
                if (value == null && ownerDoc != null) {
                    value = ownerDoc.getFirstValue(field);
                }
                // If there is still no value, try the root doc
                if (value == null && topDoc != null) {
                    value = topDoc.getFirstValue(field);
                }
                if (value != null) {
                    switch (field) {
                        case SolrConstants.LABEL:
                            // It is important that LABEL comes before IDDOC_PARENT in the static field list!
                            label = (String) value;
                            break;
                        case SolrConstants.IDDOC_PARENT:
                            // TODO This query is executed O(size of feed) times.
                            SolrDocumentList hits = DataManager.getInstance()
                                    .getSearchIndex()
                                    .search(new StringBuilder(SolrConstants.IDDOC).append(':').append(value).toString(), 1, null,
                                            Collections.singletonList(SolrConstants.LABEL));
                            if (hits != null && hits.getNumFound() > 0) {
                                SolrDocument parent = hits.get(0);
                                Object fieldParentLabel = parent.getFieldValue(SolrConstants.LABEL);
                                if (fieldParentLabel != null) {
                                    label = new StringBuilder((String) fieldParentLabel).append("; ").append(label).toString();
                                    bookSeries = fieldParentLabel.toString();
                                }
                            }
                            break;
                        case SolrConstants.PERSON_ONEFIELD:
                            authorRss = (String) value;
                            author = value.toString();
                            break;
                        case SolrConstants.PUBLISHER:
                            publisher = value.toString();
                            break;
                        case SolrConstants.DATECREATED:
                            modified = (Long) value;
                            break;
                        case SolrConstants.YEARPUBLISH:
                        case SolrConstants.PLACEPUBLISH:
                            if (StringUtils.isBlank(placeAndTime)) {
                                placeAndTime = value.toString();
                            } else {
                                placeAndTime = placeAndTime + ", " + value;
                            }
                            break;
                        case SolrConstants.URN:
                            urn = value.toString();
                            urnLink = new StringBuilder().append(rootPath).append("/resolver?urn=").append(value).toString();
                            break;
                        case SolrConstants.PI:
                            pi = (String) value;
                            break;
                        case SolrConstants.TITLE:
                            if (StringUtils.isEmpty(label)) {
                                label = (String) value;
                            }
                            break;
                        case SolrConstants.FILENAME:
                        case SolrConstants.THUMBNAIL:
                            if (StringUtils.isEmpty(thumbnail)) {
                                thumbnail = (String) value;
                            }
                            break;
                        case SolrConstants.DOCSTRCT:
                            if (StringUtils.isEmpty(label)) {
                                label = (String) value;
                                label = ViewerResourceBundle.getTranslation(label, locale);
                            }
                            break;
                        case "MD_SHELFMARK":
                            shelfmark = value.toString();
                            break;
                        default: // nothing
                    }
                }
            }

            String link = createLink(rootPath, pi, pageType);

            description.setImage(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(doc, thumbWidth, thumbHeight));

            if (modified != null) {
                SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT_STRING);
                String imported = format.format(new Date(modified));
                description.addMetadata(new RssMetadata(ViewerResourceBundle.getTranslation("DATECREATED", locale), imported));
            }

            if (StringUtils.isNotBlank(placeAndTime)) {
                description.addMetadata(new RssMetadata(ViewerResourceBundle.getTranslation("rss_published", locale), placeAndTime));
            }
            if (StringUtils.isNotBlank(bookSeries)) {
                description.addMetadata(new RssMetadata(ViewerResourceBundle.getTranslation("rss_bookSeries", locale), bookSeries));
            }
            if (StringUtils.isNotBlank(author)) {
                description.addMetadata(new RssMetadata(ViewerResourceBundle.getTranslation("MD_AUTHOR", locale), author));
            }
            if (StringUtils.isNotBlank(publisher)) {
                description.addMetadata(new RssMetadata(ViewerResourceBundle.getTranslation("MD_PUBLISHER", locale), publisher));
            }
            if (StringUtils.isNotBlank(urn)) {
                description.addMetadata(new RssMetadata(ViewerResourceBundle.getTranslation("rss_url", locale), urn, urnLink));
            }
            if (StringUtils.isNotBlank(shelfmark)) {
                description.addMetadata(new RssMetadata(ViewerResourceBundle.getTranslation("MD_SHELFMARK", locale), shelfmark));
            }
            entry.setDescription(description);

            if (StringUtils.isNotBlank(authorRss)) {
                entry.setCreator(authorRss);
            }
            entry.setTitle(label);
            entry.setLink(link);
            if (modified != null) {
                try {
                    entry.setPubDate(new Date(modified));
                } catch (NumberFormatException e) {
                    logger.error(e.getMessage());
                }
            }

            feed.addItem(entry);
        }

        // // TODO read information from config file

        // Image digiversoLogo = new Image();
        // // TODO ico funktioniert nicht, muss gif/png sein
        // digiversoLogo.setURL("http://www.intranda.com/images/favicon.ico");
        // digiversoLogo.setTitle("digiverso");
        // digiversoLogo.setLink("http://intranda.com");
        // myChannel.setImage(digiversoLogo);
        //
        Collections.sort(feed.getItems());

        return feed;
    }

    /**
     * <p>
     * createLink.
     * </p>
     *
     * @param rootPath a {@link java.lang.String} object.
     * @param pi a {@link java.lang.String} object.
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return a {@link java.lang.String} object.
     */
    public static String createLink(String rootPath, String pi, PageType pageType) {
        StringBuilder link = new StringBuilder();
        link.append(rootPath).append('/').append(pageType.getName()).append('/').append(pi).append('/').append("1").append('/');
        return link.toString();
    }

    /**
     * <p>
     * createImageUrl.
     * </p>
     *
     * @param rootPath a {@link java.lang.String} object.
     * @param anchor a boolean.
     * @param ePublication a boolean.
     * @param pi a {@link java.lang.String} object.
     * @param thumbnail a {@link java.lang.String} object.
     * @param thumbWidth a int.
     * @param thumbHeight a int.
     * @return a {@link java.lang.String} object.
     */
    @Deprecated
    public static String createImageUrl(String rootPath, boolean anchor, boolean ePublication, String pi, String thumbnail, int thumbWidth,
            int thumbHeight) {
        String imageUrl = null;
        if (ePublication) {
            // E-publication thumbnail
            imageUrl =
                    new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=image&sourcepath=")
                            .append(rootPath)
                            .append("/resources/themes/")
                            .append(DataManager.getInstance().getConfiguration().getTheme())
                            .append("/images/thumbnail_epub.jpg")
                            .append("&width=")
                            .append(thumbWidth)
                            .append("&height=")
                            .append(thumbHeight)
                            .append("&rotate=0&format=png&resolution=72&ignoreWatermark=true")
                            .toString();
        } else if (anchor && StringUtils.isEmpty(thumbnail)) {
            // Anchor thumbnail
            imageUrl =
                    new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=image&sourcepath=")
                            .append(rootPath)
                            .append("/resources/themes/")
                            .append(DataManager.getInstance().getConfiguration().getTheme())
                            .append("/images/multivolume_thumbnail.jpg")
                            .append("&width=")
                            .append(thumbWidth)
                            .append("&height=")
                            .append(thumbHeight)
                            .append("&rotate=0&format=png&resolution=72&ignoreWatermark=true")
                            .toString();

        } else if (StringUtils.isNotEmpty(thumbnail)) {
            StringBuilder sbImageUrl =
                    new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=image&sourcepath=");
            if (!thumbnail.startsWith("http")) {
                sbImageUrl.append(pi).append('/');
            }
            sbImageUrl.append(thumbnail)
                    .append("&width=")
                    .append(thumbWidth)
                    .append("&height=")
                    .append(thumbHeight)
                    .append("&rotate=0&resolution=72&thumbnail=true&ignoreWatermark=true")
                    .append(DataManager.getInstance().getConfiguration().isForceJpegConversion() ? "&format=jpg" : "")
                    .toString();
            imageUrl = sbImageUrl.toString();
        }
        return imageUrl;
    }

    /**
     * @param language
     * @param maxHits
     * @param query
     * @param facets
     * @param searchOperator
     * @return
     * @throws ContentLibException
     */
    public static Channel createRssResponse(String language, Integer maxHits, String subtheme, String query, String facets,
            Integer facetQueryOperator, HttpServletRequest servletRequest)
            throws ContentLibException {
        try {
            if (maxHits == null) {
                maxHits = DataManager.getInstance().getConfiguration().getRssFeedItems();
            }
            if (language == null) {
                language = servletRequest.getLocale().getLanguage();
            }
            query = createQuery(query, null, subtheme, servletRequest, false);
            if (StringUtils.isNotBlank(query)) {
                query = SearchHelper.buildFinalQuery(query, DataManager.getInstance().getConfiguration().isAggregateHits(), servletRequest);
            }

            // Optional faceting
            List<String> filterQueries = null;
            if (StringUtils.isNotBlank(facets)) {
                SearchFacets searchFacets = new SearchFacets();
                searchFacets.setCurrentFacetString(facets);
                filterQueries = searchFacets.generateFacetFilterQueries(facetQueryOperator != null ? facetQueryOperator : 0, true, false);
            }

            Channel rss = RSSFeed.createRssFeed(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest),
                    query, filterQueries, maxHits, language);
            return rss;
        } catch (PresentationException | IndexUnreachableException | ViewerConfigurationException | DAOException e) {
            throw new ContentLibException(e.toString());
        }
    }

    public static String createRssFeed(String language, Integer maxHits, String subtheme, String query, String facets, Integer facetQueryOperator,
            HttpServletRequest servletRequest)
            throws ContentLibException {
        try {
            if (maxHits == null) {
                maxHits = DataManager.getInstance().getConfiguration().getRssFeedItems();
            }
            if (language == null) {
                language = servletRequest.getLocale().getLanguage();
            }
            query = createQuery(query, null, subtheme, servletRequest, false);
            if (StringUtils.isNotBlank(query)) {
                query = SearchHelper.buildFinalQuery(query, DataManager.getInstance().getConfiguration().isAggregateHits(), servletRequest);
            }

            // Optional faceting
            List<String> filterQueries = null;
            if (StringUtils.isNotBlank(facets)) {
                SearchFacets searchFacets = new SearchFacets();
                searchFacets.setCurrentFacetString(facets);
                filterQueries = searchFacets.generateFacetFilterQueries(facetQueryOperator != null ? facetQueryOperator : 0, true, false);
            }

            SyndFeedOutput output = new SyndFeedOutput();
            return output
                    .outputString(RSSFeed.createRss(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest), query, filterQueries,
                            language, maxHits));

        } catch (PresentationException | IndexUnreachableException | ViewerConfigurationException | DAOException | FeedException e) {
            throw new ContentLibException(e.toString());
        }
    }

    private static String createQuery(String query, Long bookshelfId, String partnerId, HttpServletRequest servletRequest, boolean addSuffixes)
            throws IndexUnreachableException, PresentationException, DAOException {
        // Build query, if none given
        if (StringUtils.isEmpty(query)) {
            if (bookshelfId != null) {
                // Bookshelf RSS feed
                BookmarkList bookshelf = DataManager.getInstance().getDao().getBookmarkList(bookshelfId);
                if (bookshelf == null) {
                    throw new PresentationException("Requested bookshelf not found: " + bookshelfId);
                }
                if (!bookshelf.isIsPublic()) {
                    throw new PresentationException("Requested bookshelf not public: " + bookshelfId);
                }
                query = bookshelf.generateSolrQueryForItems();
            } else {
                // Main RSS feed
                query = SolrConstants.ISWORK + ":true";
            }
        }

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append("(").append(query).append(")");

        if (StringUtils.isNotBlank(partnerId)) {
            sbQuery.append(" AND ")
                    .append(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField())
                    .append(':')
                    .append(partnerId.trim());
        }

        if (addSuffixes) {
            sbQuery.append(
                    SearchHelper.getAllSuffixes(servletRequest, true, true));
        }

        return sbQuery.toString();
    }

}
