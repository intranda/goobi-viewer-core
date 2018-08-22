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
package de.intranda.digiverso.presentation.model.sitemap;

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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StringPair;

/**
 * Sitemap generation.
 */
public class Sitemap {

    private static final Logger logger = LoggerFactory.getLogger(Sitemap.class);

    static Namespace nsSitemap = Namespace.getNamespace(null, "http://www.sitemaps.org/schemas/sitemap/0.9");

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
     * @throws IOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @should generate sitemap element for each sitemap in index file
     * @should set correct lastmod date for each sitemap in index file
     * @should generate sitemap files correctly
     * @should contain no more than 50000 urls per sitemap file
     * @should only create toc url for anchors
     * @should only create full-text entries if full-text available
     * @should throw IOException if outputPath invalid
     */
    public List<File> generate(String viewerRootUrl, String outputPath)
            throws IOException, PresentationException, IndexUnreachableException, DAOException {
        this.viewerRootUrl = viewerRootUrl;
        // Sitemap index root
        docIndex.setRootElement(new Element("sitemapindex", nsSitemap));

        index = -1;
        long timestampModified = 0;
        if (index == -1) {
            increment(timestampModified);
        }

        // CMS pages
        List<CMSPage> pages = DataManager.getInstance().getDao().getAllCMSPages();
        if (!pages.isEmpty()) {
            for (CMSPage page : pages) {
                String url = viewerRootUrl + page.getPageUrl();
                String dateUpdated = getDateString(page.getDateUpdated().getTime());
                currentDocSitemap.getRootElement().addContent(createUrlElement(url, dateUpdated, "weekly", "0.5"));
                increment(timestampModified);
                logger.debug("Added CMS page: {}", page.getTitle());
            }
        }

        // Create query that filters out blacklisted collections and any records that do not allow listing by default (ignore any individual agent's privileges for the sitemap).
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append(SolrConstants.PI).append(":* AND NOT(").append(SolrConstants.DATEDELETED).append(":*)").append(
                SearchHelper.getAllSuffixes(false));
        logger.debug("Sitemap query: {}", sbQuery.toString());
        String[] fields = { SolrConstants.PI, SolrConstants.DATECREATED, SolrConstants.DATEUPDATED, SolrConstants.FULLTEXTAVAILABLE,
                SolrConstants.ISANCHOR, SolrConstants.THUMBPAGENO };
        String[] pageFields = { SolrConstants.ORDER };

        QueryResponse qr = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 0, SolrSearchIndex.MAX_HITS,
                Collections.singletonList(new StringPair(SolrConstants.DATECREATED, "asc")), null, null, Arrays.asList(fields), null, null);
        logger.debug("Found {} records.", qr.getResults().size());

        long latestTimestampModified = 0;
        for (SolrDocument solrDoc : qr.getResults()) {
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
                dateModified = getDateString(timestampModified);
                if (timestampModified > latestTimestampModified) {
                    latestTimestampModified = timestampModified;
                    eleCurrectIndexSitemap.getChild("lastmod", nsSitemap).setText(dateModified);
                    //                        logger.debug("Set latest modified date: " + dateModified);
                }
            }
            if (solrDoc.getFieldValue(SolrConstants.ISANCHOR) != null && (Boolean) solrDoc.getFieldValue(SolrConstants.ISANCHOR)) {
                // Anchors
                // Anchor TOC URL
                currentDocSitemap.getRootElement().addContent(createUrlElement(pi, 1, dateModified, PageType.viewToc.getName(), "weekly", "0.5"));
                increment(timestampModified);
                // Anchor metadata URL
                currentDocSitemap.getRootElement()
                        .addContent(createUrlElement(pi, 1, dateModified, PageType.viewMetadata.getName(), "weekly", "0.5"));
                increment(timestampModified);
            } else {
                // Record
                {
                    //  Record object URL (representative page)
                    int order = solrDoc.containsKey(SolrConstants.THUMBPAGENO) ? (int) solrDoc.getFieldValue(SolrConstants.THUMBPAGENO) : 1;
                    currentDocSitemap.getRootElement()
                            .addContent(createUrlElement(pi, order, dateModified, PageType.viewObject.getName(), "weekly", "0.5"));
                    increment(timestampModified);
                }
                {
                    // Record metadata URL
                    currentDocSitemap.getRootElement()
                            .addContent(createUrlElement(pi, 1, dateModified, PageType.viewMetadata.getName(), "weekly", "0.5"));
                    increment(timestampModified);
                }
                {
                    // Record TOC URL
                    currentDocSitemap.getRootElement().addContent(createUrlElement(pi, 1, dateModified, PageType.viewToc.getName(), "weekly", "0.5"));
                    increment(timestampModified);
                }

                // Pages
                StringBuilder sbPagesQuery = new StringBuilder();
                sbPagesQuery.append(SolrConstants.PI_TOPSTRUCT)
                        .append(':')
                        .append(pi)
                        .append(" AND ")
                        .append(SolrConstants.DOCTYPE)
                        .append(':')
                        .append(SolrConstants.DocType.PAGE)
                        .append(" AND ")
                        .append(SolrConstants.FULLTEXT)
                        .append(":*");
                //                logger.trace("Pages query: {}", sbPagesQuery.toString());
                QueryResponse qrPages = DataManager.getInstance().getSearchIndex().search(sbPagesQuery.toString(), 0, SolrSearchIndex.MAX_HITS,
                        Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), null, null, Arrays.asList(pageFields), null, null);
                if (!qrPages.getResults().isEmpty()) {
                    logger.debug("Found {} pages with full-text for '{}'.", qrPages.getResults().size(), pi);
                    for (SolrDocument solrPageDoc : qrPages.getResults()) {
                        int order = (int) solrPageDoc.getFieldValue(SolrConstants.ORDER);
                        // Page full-text URL 
                        currentDocSitemap.getRootElement()
                                .addContent(createUrlElement(pi, order, dateModified, PageType.viewFulltext.getName(), "weekly", "0.5"));
                        increment(timestampModified);
                    }
                }
            }
        }

        logger.info("Writing sitemap to '{}'...", outputPath);
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
            Element eleUrlset = new Element("urlset", nsSitemap);
            currentDocSitemap.setRootElement(eleUrlset);

            // Add new element to the index doc
            {
                eleCurrectIndexSitemap = new Element("sitemap", nsSitemap);
                docIndex.getRootElement().addContent(eleCurrectIndexSitemap);

                // loc
                Element eleLoc = new Element("loc", nsSitemap);
                eleCurrectIndexSitemap.addContent(eleLoc);
                eleLoc.setText(viewerRootUrl + '/' + "sitemap" + docListSitemap.size() + ".xml.gz");

                // lastmod
                Element eleLastmod = new Element("lastmod", nsSitemap);
                eleCurrectIndexSitemap.addContent(eleLastmod);
                if (timestamp > 0) {
                    // If switching sitemap files within a record, use the current record's timestamp
                    eleLastmod.setText(getDateString(timestamp));
                } else {
                    eleLastmod.setText("");
                }
            }

            index = 0;
        }
    }

    /**
     * Extracts date string from the given timestamp.
     * 
     * @param timestamp
     * @return ISO date string
     */
    private static String getDateString(long timestamp) {
        return DateTools.formatterISO8601Date.print(timestamp);
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
     * @return
     */
    Element createUrlElement(String url, String dateModified, String changefreq, String priority) {
        Element eleUrl = new Element("url", nsSitemap);

        // loc
        Element eleLoc = new Element("loc", nsSitemap);
        eleUrl.addContent(eleLoc);
        eleLoc.setText(url);

        // lastmod
        if (dateModified != null) {
            Element eleLastmod = new Element("lastmod", nsSitemap);
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
     * @param outputDirPath
     * @param docIndex
     * @param docListSitemap
     * @throws IOException
     * @should write index file correctly
     * @should write gzip files correctly
     */
    protected List<File> writeFiles(String outputDirPath, Document docIndex, List<Document> docListSitemap) throws IOException {
        List<File> ret = null;

        // Write index file
        File indexFile = new File(outputDirPath, "sitemap_index.xml");
        FileTools.writeXmlFile(docIndex, indexFile.getAbsolutePath());

        // Write sitemap files
        if (docListSitemap != null) {
            ret = new ArrayList<>(docListSitemap.size());
            ret.add(indexFile);
            for (Document docSitemap : docListSitemap) {
                int index = docListSitemap.indexOf(docSitemap) + 1;
                File file = new File(outputDirPath, "sitemap" + index + ".xml");
                if (FileTools.writeXmlFile(docSitemap, file.getAbsolutePath())) {
                    Map<File, String> fileMap = new HashMap<>();
                    fileMap.put(file, "/");
                    File gzipFile = new File(outputDirPath, "sitemap" + index + ".xml.gz");
                    FileTools.compressGzipFile(file, gzipFile);
                    ret.add(gzipFile);
                    logger.info("Sitemap file {} written to '{}'", index, gzipFile.getAbsolutePath());
                    FileUtils.deleteQuietly(file);
                }
            }
        } else {
            ret = new ArrayList<>(1);
            ret.add(indexFile);
        }

        return ret;
    }

    public static void main(String[] args) throws IOException, PresentationException, IndexUnreachableException, DAOException {
        Sitemap sitemap = new Sitemap();
        sitemap.generate("http://localhost:8080/viewer", "C:\\Users\\andrey\\Documents");
    }
}
