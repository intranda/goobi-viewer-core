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
package de.intranda.digiverso.presentation.model.rss;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

public class RSSFeed {
    private static final Logger logger = LoggerFactory.getLogger(RSSFeed.class);

    public static final String[] FIELDS = { SolrConstants.ACCESSCONDITION, SolrConstants.DATECREATED, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE,
            SolrConstants.FILENAME, SolrConstants.FULLTEXT, SolrConstants.IDDOC, SolrConstants.LABEL, SolrConstants.IDDOC_PARENT,
            SolrConstants.ISANCHOR, SolrConstants.ISWORK, SolrConstants.LOGID, SolrConstants.MIMETYPE, SolrConstants.PERSON_ONEFIELD,
            SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.PLACEPUBLISH, SolrConstants.PUBLISHER, SolrConstants.TITLE,
            SolrConstants.THUMBNAIL, SolrConstants.URN, SolrConstants.YEARPUBLISH, "MD_SHELFMARK" };

    /**
     *
     * @param rootPath
     * @param query
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public static SyndFeed createRss(String rootPath, String query)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return createRss(rootPath, query, null);
    }

    public static SyndFeed createRss(String rootPath, String query, String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        String feedType = "rss_2.0";

        Locale locale = null;
        if (StringUtils.isNotBlank(language)) {
            locale = Locale.forLanguageTag(language);
        }
        if (locale == null) {
            locale = Locale.GERMANY;
        }

        logger.trace("RSS query: {}", query);

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

        int rssFeedItems = DataManager.getInstance().getConfiguration().getRssFeedItems();
        SolrDocumentList docs = DataManager.getInstance()
                .getSearchIndex()
                .search(query, 0, rssFeedItems, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "desc")), null,
                        Arrays.asList(FIELDS))
                .getResults();
        if (docs != null) {
            for (SolrDocument doc : docs) {
                boolean anchor = doc.containsKey(SolrConstants.ISANCHOR) && ((Boolean) doc.getFieldValue(SolrConstants.ISANCHOR));
                boolean child = !anchor
                        && (DocType.DOCSTRCT.name().equals(doc.getFieldValue(SolrConstants.DOCTYPE))
                                || doc.getFieldValue(SolrConstants.LOGID) != null)
                        && (!doc.containsKey(SolrConstants.ISWORK) || !((Boolean) doc.getFieldValue(SolrConstants.ISWORK)));
                boolean page = DocType.PAGE.name().equals(doc.getFieldValue(SolrConstants.DOCTYPE)) || doc.containsKey(SolrConstants.ORDER);
                boolean ePublication = PhysicalElement.MIME_TYPE_APPLICATION.equals(doc.getFieldValue(SolrConstants.MIMETYPE));
                SolrDocument topDoc = null;
                SolrDocument ownerDoc = null;
                if (child || page) {
                    // Find top level docstruct to extract metadata such as DATECREATED
                    List<SolrDocument> topDocs = DataManager.getInstance()
                            .getSearchIndex()
                            .search(new StringBuilder(SolrConstants.PI).append(':').append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT)).toString(),
                                    0, 1, null, null, Arrays.asList(FIELDS))
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
                boolean hasImages = isHasImages(doc);
                String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
                String mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
                // If the document has an overview page, use the overview view
                boolean hasOverviewPage = false;
                if (pi != null && DataManager.getInstance().getConfiguration().isSidebarOverviewLinkVisible()
                        && DataManager.getInstance().getDao().getOverviewPageForRecord(pi, null, null) != null) {
                    hasOverviewPage = true;
                }
                PageType pageType = PageType.determinePageType(docStructType, mimeType, anchor, hasImages, hasOverviewPage, false);

                for (String field : FIELDS) {
                    Object value = doc.getFirstValue(field);
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
                                SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(
                                        new StringBuilder(SolrConstants.IDDOC).append(':').append(value).toString(), 1, null,
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
                }
                if ("<strong>Published: </strong>".length() == placeAndTime.length()) {
                    placeAndTime = "";
                } else {
                    placeAndTime = new StringBuilder(placeAndTime).append("<br />").toString();
                }

                String imageUrl = BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(doc, thumbWidth, thumbHeight);

                String imageHtmlElement = null;
                if (StringUtils.isNotEmpty(pi) && StringUtils.isNotEmpty(imageUrl)) {
                    imageHtmlElement = new StringBuilder("<a href=\"").append(rootPath)
                            .append('/')
                            .append(pageType.getName())
                            .append('/')
                            .append(pi)
                            .append('/')
                            .append("1")
                            .append('/')
                            .append("\">")
                            .append("<img style=\"margin-right:10px;\" src=\"")
                            .append(imageUrl)
                            .append("\" align=\"left\" border=\"0\" /></a>")
                            .toString();
                }

                entry.setAuthor(authorRss);
                entry.setTitle(label);
                entry.setLink(new StringBuilder(rootPath).append('/')
                        .append(pageType.getName())
                        .append('/')
                        .append(pi)
                        .append('/')
                        .append("1")
                        .append('/')
                        .toString());
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

    public static Channel createRssFeed(String rootPath, String query, int rssFeedItems)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return createRssFeed(rootPath, query, rssFeedItems, null);
    }

    public static Channel createRssFeed(String rootPath, String query, int rssFeedItems, String language)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        String feedType = "rss_2.0";

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
                        Arrays.asList(FIELDS))
                .getResults();
        if (docs != null) {
            for (SolrDocument doc : docs) {
                boolean anchor = doc.containsKey(SolrConstants.ISANCHOR) && ((Boolean) doc.getFieldValue(SolrConstants.ISANCHOR));
                boolean child = !anchor
                        && (DocType.DOCSTRCT.name().equals(doc.getFieldValue(SolrConstants.DOCTYPE))
                                || doc.getFieldValue(SolrConstants.LOGID) != null)
                        && (!doc.containsKey(SolrConstants.ISWORK) || !((Boolean) doc.getFieldValue(SolrConstants.ISWORK)));
                boolean page = DocType.PAGE.name().equals(doc.getFieldValue(SolrConstants.DOCTYPE)) || doc.containsKey(SolrConstants.ORDER);
                boolean ePublication = PhysicalElement.MIME_TYPE_APPLICATION.equals(doc.getFieldValue(SolrConstants.MIMETYPE));
                SolrDocument topDoc = null;
                SolrDocument ownerDoc = null;
                if (child || page) {
                    // Find top level docstruct to extract metadata such as DATECREATED
                    List<SolrDocument> topDocs = DataManager.getInstance()
                            .getSearchIndex()
                            .search(new StringBuilder(SolrConstants.PI).append(':').append(doc.getFieldValue(SolrConstants.PI_TOPSTRUCT)).toString(),
                                    0, 1, null, null, Arrays.asList(FIELDS))
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
                String descValue = "";
                String thumbnail = "";
                String urn = "";
                String urnLink = "";
                String bookSeries = "";
                String shelfmark = "";
                
                int thumbWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
                int thumbHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
                boolean hasImages = isHasImages(doc);
                String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
                String mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
                // If the document has an overview page, use the overview view
                boolean hasOverviewPage = false;
                if (pi != null && DataManager.getInstance().getConfiguration().isSidebarOverviewLinkVisible()
                        && DataManager.getInstance().getDao().getOverviewPageForRecord(pi, null, null) != null) {
                    hasOverviewPage = true;
                }
                PageType pageType = PageType.determinePageType(docStructType, mimeType, anchor, hasImages, hasOverviewPage, false);
                entry.setDocType(Helper.getTranslation(docStructType, locale));

                
                
                for (String field : FIELDS) {
                    Object value = doc.getFirstValue(field);
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
                                SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(
                                        new StringBuilder(SolrConstants.IDDOC).append(':').append(value).toString(), 1, null,
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

                if (StringUtils.isNotBlank(placeAndTime)) {
                    description.addMetadata(new RssMetadata(Helper.getTranslation("rss_published", locale), placeAndTime));
                }
                if (StringUtils.isNotBlank(bookSeries)) {
                    description.addMetadata(new RssMetadata(Helper.getTranslation("rss_bookSeries", locale), bookSeries));
                }
                if (StringUtils.isNotBlank(author)) {
                    description.addMetadata(new RssMetadata(Helper.getTranslation("MD_AUTHOR", locale), author));
                }
                if (StringUtils.isNotBlank(publisher)) {
                    description.addMetadata(new RssMetadata(Helper.getTranslation("MD_PUBLISHER", locale), publisher));
                }
                if (StringUtils.isNotBlank(urn)) {
                    description.addMetadata(new RssMetadata(Helper.getTranslation("rss_url", locale), urn, urnLink));
                }
                if (StringUtils.isNotBlank(shelfmark)) {
                    description.addMetadata(new RssMetadata(Helper.getTranslation("MD_SHELFMARK", locale), shelfmark));
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
     * @param rootPath
     * @param pi
     * @param pageType
     */
    public static String createLink(String rootPath, String pi, PageType pageType) {
        StringBuilder link = new StringBuilder();
        link.append(rootPath).append('/').append(pageType.getName()).append('/').append(pi).append('/').append("1").append('/');
        return link.toString();
    }

    /**
     * @param rootPath
     * @param anchor
     * @param ePublication
     * @param pi
     * @param thumbnail
     * @param thumbWidth
     * @param thumbHeight
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
     * @param doc
     * @return
     * @throws IndexUnreachableException
     */
    private static boolean isHasImages(SolrDocument doc) throws IndexUnreachableException {

        StructElement structElement = new StructElement(0, doc);
        String fileExtension = "";

        String filename = structElement.getMetadataValue(SolrConstants.FILENAME);
        if (StringUtils.isEmpty(filename)) {
            filename = structElement.getMetadataValue(SolrConstants.THUMBNAIL);
        }
        if (filename != null) {
            fileExtension = FilenameUtils.getExtension(filename).toLowerCase();
        }

        return fileExtension != null && fileExtension.toLowerCase().matches("(tiff?|jpe?g|png|jp2|gif)");
    }
}
