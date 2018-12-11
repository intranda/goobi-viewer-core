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
package de.intranda.digiverso.presentation.model.viewer.pageloader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Memory-saving page loader that only loads one page at a time.
 */
public class LeanPageLoader extends AbstractPageLoader implements Serializable {

    private static final long serialVersionUID = 7841595315092878078L;

    private static final Logger logger = LoggerFactory.getLogger(LeanPageLoader.class);

    private static final String[] SELECT_ITEM_FIELDS = { SolrConstants.ORDER, SolrConstants.ORDERLABEL };

    private StructElement topElement;
    private int numPages = -1;
    private PhysicalElement currentPage;
    private int currentPageNumber = -1;
    private int firstPageOrder = 1;
    private int lastPageOrder = 1;

    public LeanPageLoader(StructElement topElement, int numPages) throws IndexUnreachableException {
        this.topElement = topElement;
        this.numPages = numPages;
        setFirstAndLastPageOrder();
    }

    /**
     * @throws IndexUnreachableException
     * @see de.intranda.digiverso.presentation.model.viewer.IPageLoader#getNumPages()
     * @should return size correctly
     */
    @Override
    public int getNumPages() throws IndexUnreachableException {
        if (numPages < 0) {
            numPages = topElement.getNumPages();
        }
        return numPages;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.pageloader.IPageLoader#getFirstPageOrder()
     */
    @Override
    public int getFirstPageOrder() {
        return firstPageOrder;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.pageloader.IPageLoader#getLastPageOrder()
     */
    @Override
    public int getLastPageOrder() {
        return lastPageOrder;
    }

    /**
     * @throws IndexUnreachableException
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.model.viewer.IPageLoader#getPage(int)
     * @should return correct page
     * @should return null if pageOrder smaller than firstPageOrder
     * @should return null if pageOrder larger than lastPageOrder
     */
    @Override
    public PhysicalElement getPage(int pageOrder) throws IndexUnreachableException, DAOException {
        if (pageOrder != currentPageNumber && pageOrder >= firstPageOrder && pageOrder <= lastPageOrder) {
            try {
                currentPage = loadPage(pageOrder, null);
                currentPageNumber = pageOrder;
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
            return currentPage;
        } else if (pageOrder == currentPageNumber) {
            ;
            return currentPage;
        } else {
            return null;
        }

    }

    /**
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @see de.intranda.digiverso.presentation.model.viewer.IPageLoader#getPageForFileName(java.lang.String)
     * @should return the correct page
     * @should return null if file name not found
     */
    @Override
    public PhysicalElement getPageForFileName(String fileName) throws PresentationException, IndexUnreachableException, DAOException {
        return loadPage(-1, fileName);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.pageloader.IPageLoader#getIddocForPage(int)
     */
    @Override
    public Long getOwnerIddocForPage(int pageOrder) throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance().getSearchIndex().getImageOwnerIddoc(topElement.getPi(), pageOrder);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.IPageLoader#generateSelectItems(java.util.List, java.util.List, java.lang.String, java.lang.Boolean, java.util.Locale)
     */
    @Override
    public void generateSelectItems(List<SelectItem> dropdownPages, List<SelectItem> dropdownFulltext, String urlRoot,
            boolean recordBelowFulltextThreshold, Locale locale) throws IndexUnreachableException {
        logger.debug("Generating drop-down page selector...");
        try {
            String pi = topElement.getPi();
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append(SolrConstants.PI_TOPSTRUCT)
                    .append(':')
                    .append(topElement.getPi())
                    .append(" AND ")
                    .append(SolrConstants.DOCTYPE)
                    .append(':')
                    .append(DocType.PAGE);
            SolrDocumentList result = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), SolrSearchIndex.MAX_HITS,
                    Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), Arrays.asList(SELECT_ITEM_FIELDS));
            if (result == null || result.isEmpty()) {
                sbQuery = new StringBuilder();
                sbQuery.append(SolrConstants.PI_TOPSTRUCT)
                        .append(':')
                        .append(topElement.getPi())
                        .append(" AND ")
                        .append(SolrConstants.FILENAME)
                        .append(":*");
                result = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), SolrSearchIndex.MAX_HITS,
                        Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), Arrays.asList(SELECT_ITEM_FIELDS));
            }
            String labelTemplate = buildPageLabelTemplate(DataManager.getInstance().getConfiguration().getPageSelectionFormat(), locale);
            for (SolrDocument doc : result) {
                int order = (Integer) doc.getFieldValue(SolrConstants.ORDER);
                String orderLabel = (String) doc.getFieldValue(SolrConstants.ORDERLABEL);
                boolean fulltextAvailable =
                        doc.containsKey(SolrConstants.FULLTEXTAVAILABLE) ? (boolean) doc.getFieldValue(SolrConstants.FULLTEXTAVAILABLE) : false;
                StringBuilder sbPurlPart = new StringBuilder();
                sbPurlPart.append('/').append(pi).append('/').append(order).append('/');

                SelectItem si = new SelectItem();
                si.setLabel(labelTemplate.replace("{order}", String.valueOf(order)).replace("{orderlabel}", orderLabel));
                si.setValue(order);
                dropdownPages.add(si);
                if (dropdownFulltext != null && !(recordBelowFulltextThreshold && !fulltextAvailable)) {
                    SelectItem full = new SelectItem();
                    full.setLabel(order + ":" + orderLabel);
                    full.setValue(order);//urlRoot + "/" + PageType.viewFulltext.getName() + sbPurlPart.toString());
                    dropdownFulltext.add(full);
                }
            }
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }
    }

    /**
     * @throws IndexUnreachableException
     * @should set first page order correctly
     * @should set last page order correctly
     */
    protected final void setFirstAndLastPageOrder() throws IndexUnreachableException {
        try {
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append(SolrConstants.PI_TOPSTRUCT)
                    .append(':')
                    .append(topElement.getPi())
                    .append(" AND ")
                    .append(SolrConstants.DOCTYPE)
                    .append(':')
                    .append(DocType.PAGE);
            SolrDocumentList result = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1,
                    Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), Collections.singletonList(SolrConstants.ORDER));
            if (!result.isEmpty()) {
                firstPageOrder = (int) result.get(0).getFieldValue(SolrConstants.ORDER);
            }

            result = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1,
                    Collections.singletonList(new StringPair(SolrConstants.ORDER, "desc")), Collections.singletonList(SolrConstants.ORDER));
            if (!result.isEmpty()) {
                lastPageOrder = (int) result.get(0).getFieldValue(SolrConstants.ORDER);
            }
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }
    }

    /**
     * @param pageNumber
     * @param fileName
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should load page correctly via page number
     * @should load page correctly via file name
     * @should return null if page not found
     */
    protected PhysicalElement loadPage(int pageNumber, String fileName) throws PresentationException, IndexUnreachableException, DAOException {
        String pi = topElement.getPi();
        if (pageNumber >= 0) {
            logger.trace("Loading page {} for '{}'...", pageNumber, pi);
        }
        List<String> fields = new ArrayList<>(Arrays.asList(FIELDS));

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append(SolrConstants.PI_TOPSTRUCT).append(':').append(pi).append(" AND ").append(SolrConstants.DOCTYPE).append(':').append(
                DocType.PAGE);
        if (pageNumber >= 0) {
            sbQuery.append(" AND ").append(SolrConstants.ORDER).append(':').append(pageNumber);
        }
        if (fileName != null) {
            sbQuery.append(" AND ").append(SolrConstants.FILENAME).append(':').append(fileName);
        }
        SolrDocumentList result = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1, null, fields);
        if (result == null || result.isEmpty()) {
            sbQuery = new StringBuilder();
            sbQuery.append(SolrConstants.PI_TOPSTRUCT).append(':').append(pi);
            if (pageNumber >= 0) {
                sbQuery.append(" AND ").append(SolrConstants.ORDER).append(':').append(pageNumber);
            }
            if (fileName != null) {
                sbQuery.append(" AND ").append(SolrConstants.FILENAME).append(':').append(fileName);
            } else {
                sbQuery.append(" AND ").append(SolrConstants.FILENAME).append(":*");
            }
        }
        result = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1,
                Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), fields);
        if (!result.isEmpty()) {
            SolrDocument doc = result.get(0);
            // PHYSID
            String physId = "";
            if (doc.getFieldValue(SolrConstants.PHYSID) != null) {
                physId = (String) doc.getFieldValue(SolrConstants.PHYSID);
            }
            // ORDER
            int order = (Integer) doc.getFieldValue(SolrConstants.ORDER);
            // ORDERLABEL
            String orderLabel = "";
            if (doc.getFieldValue(SolrConstants.ORDERLABEL) != null) {
                orderLabel = (String) doc.getFieldValue(SolrConstants.ORDERLABEL);
            }
            // Mime type
            String mimeType = null;
            if (doc.getFieldValue(SolrConstants.MIMETYPE) != null) {
                mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
            }
            // Main file name
            //            if (doc.getFieldValue(LuceneConstants.FILENAME) != null) {
            //                fileName = (String) doc.getFieldValue(LuceneConstants.FILENAME);
            //            }
            if (doc.getFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED) != null) {
                fileName = (String) doc.getFieldValue(SolrConstants.FILENAME_HTML_SANDBOXED);
            } else if (doc.getFieldValue(SolrConstants.FILENAME) != null) {
                fileName = (String) doc.getFieldValue(SolrConstants.FILENAME);
            }

            String dataRepository = "";
            if (doc.getFieldValue(SolrConstants.DATAREPOSITORY) != null) {
                dataRepository = (String) doc.getFieldValue(SolrConstants.DATAREPOSITORY);
            } else {
                dataRepository = topElement.getDataRepository();
            }

            // URN
            String urn = "";
            if (doc.getFieldValue(SolrConstants.IMAGEURN) != null && !doc.getFirstValue(SolrConstants.IMAGEURN).equals("NULL")) {
                urn = (String) doc.getFieldValue(SolrConstants.IMAGEURN);
            }
            StringBuilder sbPurlPart = new StringBuilder();
            sbPurlPart.append('/').append(pi).append('/').append(order).append('/');

            PhysicalElement pe = new PhysicalElement(physId, fileName, order, orderLabel, urn, sbPurlPart.toString(), pi, mimeType, dataRepository);

            if (doc.getFieldValue(SolrConstants.WIDTH) != null) {
                pe.setWidth((Integer) doc.getFieldValue(SolrConstants.WIDTH));
            }
            if (doc.getFieldValue(SolrConstants.HEIGHT) != null) {
                pe.setHeight((Integer) doc.getFieldValue(SolrConstants.HEIGHT));
            }

            // Full-text filename
            pe.setFulltextFileName((String) doc.getFirstValue(SolrConstants.FILENAME_FULLTEXT));
            // ALTO filename
            pe.setAltoFileName((String) doc.getFirstValue(SolrConstants.FILENAME_ALTO));

            // Access conditions
            if (doc.getFieldValues(SolrConstants.ACCESSCONDITION) != null) {
                for (Object o : doc.getFieldValues(SolrConstants.ACCESSCONDITION)) {
                    String accessCondition = (String) o;
                    if (StringUtils.isNotEmpty(accessCondition)) {
                        pe.getAccessConditions().add(accessCondition);
                    }
                }
            }

            // File names for different formats (required for A/V)
            String filenameRoot = new StringBuilder(SolrConstants.FILENAME).append('_').toString();
            for (String fieldName : doc.getFieldNames()) {
                //                logger.trace(fieldName);
                if (fieldName.startsWith(filenameRoot)) {
                    String format = fieldName.split("_")[1].toLowerCase();
                    String value = (String) doc.getFieldValue(fieldName);
                    pe.getFileNames().put(format, value);
                }
            }
            // METS file ID root
            if (doc.getFieldValue(SolrConstants.FILEIDROOT) != null) {
                pe.setFileIdRoot((String) doc.getFieldValue(SolrConstants.FILEIDROOT));
            }

            // File size
            if (doc.getFieldValue("MDNUM_FILESIZE") != null) {
                pe.setFileSize((long) doc.getFieldValue("MDNUM_FILESIZE"));
            }

            // Full-text available
            if (doc.containsKey(SolrConstants.FULLTEXTAVAILABLE)) {
                pe.setFulltextAvailable((boolean) doc.getFieldValue(SolrConstants.FULLTEXTAVAILABLE));
            }

            //            // Eager load user generated contents from the DB
            //            if (!PhysicalElement.lazyUserGeneratedContents) {
            //                try {
            //                    IUserGeneratedContent latestUGC = DataManager.getInstance().getCrowdsourcingDao().getLatestUserGeneratedContentForPage(pi, order);
            //                    if (latestUGC != null && latestUGC.isPageCompleted()) {
            //                        for (IUserGeneratedContent ugcContent : DataManager.getInstance().getCrowdsourcingDao().getUserGeneratedContents(pi, order,
            //                                null, null)) {
            //                            if (pe.getUserGeneratedContentsForDisplay() == null) {
            //                                pe.setUserGeneratedContentsForDisplay(new ArrayList<IUserGeneratedContent>());
            //                            }
            //                            pe.getUserGeneratedContentsForDisplay().add(ugcContent);
            //                        }
            //                        logger.trace("Loaded {} user generated contents for page {}", pe.getUserGeneratedContentsForDisplay().size(), order);
            //                    }
            //                } catch (ModuleMissingException e) {
            //                    logger.trace(e.getMessage());
            //                }
            //            }

            return pe;
        }

        return null;
    }
}
