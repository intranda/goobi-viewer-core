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
package io.goobi.viewer.model.sitemap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Sitemap generation.
 */
public class Sitemap {

    private static final Logger logger = LogManager.getLogger(Sitemap.class);

    static final Namespace NS_SITEMAP = Namespace.getNamespace(null, "http://www.sitemaps.org/schemas/sitemap/0.9");

    private String viewerRootUrl = "http://localhost:8080/viewer";
    private Document docIndex = new Document();
    private List<Document> docListSitemap = new ArrayList<>();

    private int index = -1;
    private Document currentDocSitemap = null;
    private Element eleCurrectIndexSitemap = null;

    /**
     * Generates sitemap files and writes them to the given outputPath (or web root).
     *
     * @param viewerRootUrl Root URL of the Goobi viewer instance
     * @param outputPath Destination folder path for the sitemap files.
     * @return File list
     * @should generate sitemap element for each sitemap in index file
     * @should set correct lastmod date for each sitemap in index file
     * @should generate sitemap files correctly
     * @should contain no more than 50000 urls per sitemap file
     * @should only create toc url for anchors
     * @should only create toc url for groups
     * @should only create full-text entries if full-text available
     * @should throw IOException if outputPath invalid
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<File> generate(String viewerRootUrl, String outputPath)
            throws IOException, PresentationException, IndexUnreachableException, DAOException {
        this.viewerRootUrl = viewerRootUrl;
        if (this.viewerRootUrl != null && !this.viewerRootUrl.endsWith("/")) {
            this.viewerRootUrl += "/";
        }
        // Sitemap index root
        docIndex.setRootElement(new Element("sitemapindex", NS_SITEMAP));

        index = -1;
        long timestampModified = 0;
        if (index == -1) {
            increment(timestampModified);
        }

        // CMS pages
        try {
            List<CMSPage> pages = DataManager.getInstance().getDao().getAllCMSPages();
            if (!pages.isEmpty()) {
                for (CMSPage page : pages) {
                    String url = viewerRootUrl + "/" + page.getRelativeUrlPath();
                    String dateUpdated = "";
                    if (page.getDateUpdated() != null) {
                        dateUpdated = DateTools.format(page.getDateUpdated(), DateTools.formatterISO8601Date, false);
                    } else if (page.getDateCreated() != null) {
                        DateTools.format(page.getDateCreated(), DateTools.formatterISO8601Date, false);
                    }
                    currentDocSitemap.getRootElement().addContent(createUrlElement(url, dateUpdated, "weekly", "0.5"));
                    increment(timestampModified);
                    logger.debug("Sitemap: added CMS page: {}", page.getTitle());
                }
            }
        } catch (DAOException e) {
            logger.warn("Sitemap: unable to read DAO, cannot include cms pages in sitemap", e);
        }

        // Create query that filters out blacklisted collections and any records that do not allow listing by default
        // (ignore any individual agent's privileges for the sitemap).
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append(SolrConstants.PI)
                .append(":* AND NOT(")
                .append(SolrConstants.DATEDELETED)
                .append(":*)")
                .append(SearchHelper.getAllSuffixes(null, true, true));
        logger.debug("Sitemap: sitemap query: {}", sbQuery);
        String[] fields = { SolrConstants.PI, SolrConstants.DATECREATED, SolrConstants.DATEUPDATED, SolrConstants.FULLTEXTAVAILABLE,
                SolrConstants.DOCTYPE, SolrConstants.ISANCHOR, SolrConstants.THUMBPAGENO };
        String[] pageFields = { SolrConstants.ORDER };

        QueryResponse qr = DataManager.getInstance()
                .getSearchIndex()
                .search(sbQuery.toString(), 0, SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "asc")),
                        null, null, Arrays.asList(fields), null, null);
        logger.debug("Sitemap: found {} records.", qr.getResults().size());

        long latestTimestampModified = 0;
        int recordIndex = 0;
        long start = System.nanoTime();
        for (SolrDocument solrDoc : qr.getResults()) {
            if (Thread.interrupted()) {
                break;
            }
            String pi = (String) solrDoc.getFieldValue(SolrConstants.PI);
            String dateModified = null;
            Collection<Object> dateUpdatedValues = solrDoc.getFieldValues(SolrConstants.DATEUPDATED);
            if (dateUpdatedValues != null && !dateUpdatedValues.isEmpty()) {
                // Get latest DATEUPDATED values
                for (Object dateUpdated : dateUpdatedValues) {
                    if (((long) dateUpdated) > timestampModified) {
                        timestampModified = (long) dateUpdated;
                    }
                }
                dateModified =
                        DateTools.format(DateTools.getLocalDateTimeFromMillis(timestampModified, false), DateTools.formatterISO8601Date, false);
                if (timestampModified > latestTimestampModified) {
                    latestTimestampModified = timestampModified;
                    eleCurrectIndexSitemap.getChild("lastmod", NS_SITEMAP).setText(dateModified);
                    //                        logger.debug("Sitemap: set latest modified date: " + dateModified);
                }
            }
            if (solrDoc.getFieldValue(SolrConstants.ISANCHOR) != null && (Boolean) solrDoc.getFieldValue(SolrConstants.ISANCHOR)) {
                // Anchor

                // Anchor TOC URL
                currentDocSitemap.getRootElement().addContent(createUrlElement(pi, 1, dateModified, PageType.viewToc.getName(), "weekly", "0.5"));
                increment(timestampModified);

                // Anchor metadata URL
                currentDocSitemap.getRootElement()
                        .addContent(createUrlElement(pi, 1, dateModified, PageType.viewMetadata.getName(), "weekly", "0.5"));
                increment(timestampModified);
            } else if (DocType.GROUP.toString().equals(solrDoc.getFieldValue(SolrConstants.DOCTYPE))) {
                // Group

                // Group TOC URL
                currentDocSitemap.getRootElement().addContent(createUrlElement(pi, 1, dateModified, PageType.viewToc.getName(), "weekly", "0.5"));
                increment(timestampModified);
            } else {
                // RECORD
                // Record object URL (representative page)
                int recOrder = solrDoc.containsKey(SolrConstants.THUMBPAGENO) ? (int) solrDoc.getFieldValue(SolrConstants.THUMBPAGENO) : 1;
                currentDocSitemap.getRootElement()
                        .addContent(createUrlElement(pi, recOrder, dateModified, PageType.viewObject.getName(), "weekly", "0.5"));
                increment(timestampModified);

                // Record metadata URL
                currentDocSitemap.getRootElement()
                        .addContent(createUrlElement(pi, 1, dateModified, PageType.viewMetadata.getName(), "weekly", "0.5"));
                increment(timestampModified);

                // Record TOC URL
                currentDocSitemap.getRootElement().addContent(createUrlElement(pi, 1, dateModified, PageType.viewToc.getName(), "weekly", "0.5"));
                increment(timestampModified);

                QueryResponse qrPages;
                // PAGES
                StringBuilder sbPagesQuery = new StringBuilder();
                sbPagesQuery.append(SolrConstants.PI_TOPSTRUCT)
                        .append(':')
                        .append(pi)
                        .append(" AND ")
                        .append(SolrConstants.DOCTYPE)
                        .append(':')
                        .append(SolrConstants.DocType.PAGE)
                        .append(" AND ")
                        .append(SolrConstants.FULLTEXTAVAILABLE)
                        .append(":true");
                // logger.trace("Sitemap: pages query: {}", sbPagesQuery.toString()); //NOSONAR Debug
                qrPages = DataManager.getInstance()
                        .getSearchIndex()
                        .search(sbPagesQuery.toString(), 0, SolrSearchIndex.MAX_HITS,
                                Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), null, null, Arrays.asList(pageFields), null,
                                null);
                if (!qrPages.getResults().isEmpty()) {
                    logger.debug("Sitemap: found {} pages with full-text for '{}'.", qrPages.getResults().size(), pi);
                    for (SolrDocument solrPageDoc : qrPages.getResults()) {
                        int pageOrder = (int) solrPageDoc.getFieldValue(SolrConstants.ORDER);
                        // Page full-text URL
                        currentDocSitemap.getRootElement()
                                .addContent(createUrlElement(pi, pageOrder, dateModified, PageType.viewFulltext.getName(), "weekly", "0.5"));
                        increment(timestampModified);
                    }
                }
            }
            recordIndex++;
            if (recordIndex % 50 == 0) {
                logger.debug("Sitemap: parsed record {}", recordIndex);
                long end = System.nanoTime();
                logger.debug("Sitemap: parsing 50 records took {}" + " seconds", ((end - start) / 1e9));
                start = end;
            }
        }

        logger.info("Sitemap: writing sitemap to '{}'...", outputPath);
        return writeFiles(outputPath, docIndex, docListSitemap);
    }

    /**
     * Index counter for items per XML file.
     *
     * @param timestamp
     */
    private void increment(long timestamp) {
        index++;
        if (index == 0 || index == 50000) {
            // Create new sitemap doc
            currentDocSitemap = new Document();
            docListSitemap.add(currentDocSitemap);
            Element eleUrlset = new Element("urlset", NS_SITEMAP);
            currentDocSitemap.setRootElement(eleUrlset);

            // Add new element to the index doc

            eleCurrectIndexSitemap = new Element("sitemap", NS_SITEMAP);
            docIndex.getRootElement().addContent(eleCurrectIndexSitemap);

            // loc
            Element eleLoc = new Element("loc", NS_SITEMAP);
            eleCurrectIndexSitemap.addContent(eleLoc);
            eleLoc.setText(viewerRootUrl + "sitemap" + docListSitemap.size() + ".xml.gz");

            // lastmod
            Element eleLastmod = new Element("lastmod", NS_SITEMAP);
            eleCurrectIndexSitemap.addContent(eleLastmod);
            if (timestamp > 0) {
                // If switching sitemap files within a record, use the current record's timestamp
                eleLastmod
                        .setText(DateTools.format(DateTools.getLocalDateTimeFromMillis(timestamp, false), DateTools.formatterISO8601Date, false));
            } else {
                eleLastmod.setText("");
            }

            index = 0;
        }
    }

    /**
     * Creates an XML element containing a sitemap item.
     *
     * @param pi Record identifier
     * @param order Page number
     * @param dateModified
     * @param type Target page type
     * @param changefreq
     * @param priority
     * @return Created {@link Element}
     */
    private Element createUrlElement(String pi, int order, String dateModified, String type, String changefreq, String priority) {
        return createUrlElement(viewerRootUrl + '/' + type + '/' + pi + '/' + order + '/', dateModified, changefreq, priority);
    }

    /**
     * Creates an XML element containing a sitemap item.
     *
     * @param url Final URL
     * @param dateModified
     * @param changefreq
     * @param priority
     * @should create loc element correctly
     * @should create lastmod element correctly
     * @return Created {@link Element}
     */
    Element createUrlElement(String url, String dateModified, String changefreq, String priority) {
        Element eleUrl = new Element("url", NS_SITEMAP);

        // loc
        Element eleLoc = new Element("loc", NS_SITEMAP);
        eleUrl.addContent(eleLoc);
        eleLoc.setText(url);

        // lastmod
        if (dateModified != null) {
            Element eleLastmod = new Element("lastmod", NS_SITEMAP);
            eleUrl.addContent(eleLastmod);
            eleLastmod.setText(dateModified);
        }

        // changefreq
        //        Element eleChangefreq = new Element("changefreq", nsSitemap);
        //        eleUrl.addContent(eleChangefreq);
        //        eleChangefreq.setText(changefreq);

        // priority
        //        Element elePriority = new Element("priority", nsSitemap);
        //        eleUrl.addContent(elePriority);
        //        elePriority.setText(priority);

        return eleUrl;
    }

    /**
     * Writes givent sitemap XML documents into the file system.
     *
     * @param outputDirPath a {@link java.lang.String} object.
     * @param docIndex a {@link org.jdom2.Document} object.
     * @param docListSitemap a {@link java.util.List} object.
     * @should write index file correctly
     * @should write gzip files correctly
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    protected List<File> writeFiles(String outputDirPath, Document docIndex, List<Document> docListSitemap) throws IOException {
        List<File> ret = null;

        // Write index file
        File indexFile = new File(outputDirPath, "sitemap_index.xml");
        XmlTools.writeXmlFile(docIndex, indexFile.getAbsolutePath());

        // Write sitemap files
        if (docListSitemap != null) {
            ret = new ArrayList<>(docListSitemap.size());
            ret.add(indexFile);
            for (Document docSitemap : docListSitemap) {
                int i = docListSitemap.indexOf(docSitemap) + 1;
                File file = new File(outputDirPath, "sitemap" + i + ".xml");
                file = XmlTools.writeXmlFile(docSitemap, file.getAbsolutePath());
                if (file.exists()) {
                    Map<File, String> fileMap = new HashMap<>();
                    fileMap.put(file, "/");
                    File gzipFile = new File(outputDirPath, "sitemap" + i + ".xml.gz");
                    FileTools.compressGzipFile(file, gzipFile);
                    ret.add(gzipFile);
                    logger.info("Sitemap: file {} written to '{}'", i, gzipFile.getAbsolutePath());
                    FileUtils.deleteQuietly(file);
                }

            }
        } else {
            ret = new ArrayList<>(1);
            ret.add(indexFile);
        }

        return ret;
    }

}
