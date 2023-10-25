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
package io.goobi.viewer.model.viewer.pageloader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * Memory-saving page loader that only loads one page at a time.
 */
public class LeanPageLoader extends AbstractPageLoader implements Serializable {

    private static final long serialVersionUID = 7841595315092878078L;

    private static final Logger logger = LogManager.getLogger(LeanPageLoader.class);

    private static final String[] SELECT_ITEM_FIELDS = { SolrConstants.ORDER, SolrConstants.ORDERLABEL };

    private StructElement topElement;
    private int numPages = -1;
    private PhysicalElement currentPage;
    private int currentPageNumber = -1;
    private int firstPageOrder = 1;
    private int lastPageOrder = 1;

    /**
     * <p>
     * Package private constructor for LeanPageLoader.
     * </p>
     *
     * @param topElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param numPages a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    LeanPageLoader(StructElement topElement, int numPages) throws IndexUnreachableException {
        this.topElement = topElement;
        this.numPages = numPages;
        setFirstAndLastPageOrder();
    }

    /** {@inheritDoc} */
    @Override
    public int getNumPages() throws IndexUnreachableException {
        if (numPages < 0) {
            numPages = topElement.getNumPages();
        }
        return numPages;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.pageloader.IPageLoader#getFirstPageOrder()
     */
    /** {@inheritDoc} */
    @Override
    public int getFirstPageOrder() {
        return firstPageOrder;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.pageloader.IPageLoader#getLastPageOrder()
     */
    /** {@inheritDoc} */
    @Override
    public int getLastPageOrder() {
        return lastPageOrder;
    }

    /** {@inheritDoc} */
    @Override
    public PhysicalElement getPage(int pageOrder) throws IndexUnreachableException {
        if (pageOrder != currentPageNumber && pageOrder >= firstPageOrder && pageOrder <= lastPageOrder) {
            try {
                currentPage = loadPage(pageOrder, null);
                currentPageNumber = pageOrder;
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            }
            return currentPage;
        } else if (pageOrder == currentPageNumber) {
            return currentPage;
        } else {
            return null;
        }

    }

    /** {@inheritDoc} */
    @Override
    public PhysicalElement getPageForFileName(String fileName) throws PresentationException, IndexUnreachableException, DAOException {
        return loadPage(-1, fileName);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.pageloader.IPageLoader#getIddocForPage(int)
     */
    /** {@inheritDoc} */
    @Override
    public Long getOwnerIddocForPage(int pageOrder) throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance().getSearchIndex().getImageOwnerIddoc(topElement.getPi(), pageOrder);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.IPageLoader#generateSelectItems(java.util.List, java.util.List, java.lang.String, java.lang.Boolean, java.util.Locale)
     */
    /** {@inheritDoc} */
    @Override
    public void generateSelectItems(List<SelectPageItem> dropdownPages, List<SelectPageItem> dropdownFulltext, String urlRoot,
            boolean recordBelowFulltextThreshold, Locale locale) throws IndexUnreachableException {
        logger.trace("Generating drop-down page selector...");
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
            SolrDocumentList result = DataManager.getInstance()
                    .getSearchIndex()
                    .search(sbQuery.toString(), SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")),
                            Arrays.asList(SELECT_ITEM_FIELDS));
            String labelTemplate = buildPageLabelTemplate(DataManager.getInstance().getConfiguration().getPageSelectionFormat(), locale);
            for (SolrDocument doc : result) {
                int order = (Integer) doc.getFieldValue(SolrConstants.ORDER);
                String orderLabel = (String) doc.getFieldValue(SolrConstants.ORDERLABEL);
                boolean fulltextAvailable =
                        doc.containsKey(SolrConstants.FULLTEXTAVAILABLE) ? (boolean) doc.getFieldValue(SolrConstants.FULLTEXTAVAILABLE) : false;
                StringBuilder sbPurlPart = new StringBuilder();
                sbPurlPart.append('/').append(pi).append('/').append(order).append('/');

                SelectPageItem siPage = buildPageSelectItem(labelTemplate, order, orderLabel, null, null);
                dropdownPages.add(siPage);
                if (dropdownFulltext != null && !(recordBelowFulltextThreshold && !fulltextAvailable)) {
                    SelectPageItem siFull = buildPageSelectItem(labelTemplate, order, orderLabel, null, null);
                    dropdownFulltext.add(siFull);
                }
            }
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
        }
    }

    /**
     *
     * @param labelTemplate
     * @param pageNo
     * @param nextPageNo
     * @param orderLabel
     * @param nextOderLabel
     * @return
     */
    static SelectItem buildPageSelectItem(String labelTemplate, int pageNo, Integer nextPageNo, String orderLabel, String nextOderLabel) {
        if (labelTemplate == null) {
            throw new IllegalArgumentException("labelTemplate may not be null");
        }

        SelectItem si = new SelectItem();
        if (nextPageNo != null && nextOderLabel != null) {
            si.setLabel(labelTemplate.replace("{order}", pageNo + "-" + nextPageNo)
                    .replace("{orderlabel}", orderLabel + " - " + nextOderLabel));
            si.setValue(pageNo + "-" + nextPageNo);
        } else {
            si.setLabel(labelTemplate.replace("{order}", String.valueOf(pageNo)).replace("{orderlabel}", orderLabel));
            si.setValue(pageNo);
        }

        return si;
    }

    /**
     * <p>
     * setFirstAndLastPageOrder.
     * </p>
     *
     * @should set first page order correctly
     * @should set last page order correctly
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
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
            SolrDocumentList result = DataManager.getInstance()
                    .getSearchIndex()
                    .search(sbQuery.toString(), 1, Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")),
                            Collections.singletonList(SolrConstants.ORDER));
            if (!result.isEmpty()) {
                firstPageOrder = (int) result.get(0).getFieldValue(SolrConstants.ORDER);
            }

            result = DataManager.getInstance()
                    .getSearchIndex()
                    .search(sbQuery.toString(), 1, Collections.singletonList(new StringPair(SolrConstants.ORDER, "desc")),
                            Collections.singletonList(SolrConstants.ORDER));
            if (!result.isEmpty()) {
                lastPageOrder = (int) result.get(0).getFieldValue(SolrConstants.ORDER);
            }
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
        }
    }

    /**
     * <p>
     * loadPage.
     * </p>
     *
     * @param pageNumber a int.
     * @param fileName a {@link java.lang.String} object.
     * @should load page correctly via page number
     * @should load page correctly via file name
     * @should return null if page not found
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    protected PhysicalElement loadPage(int pageNumber, String fileName) throws PresentationException, IndexUnreachableException {
        String pi = topElement.getPi();
        if (pageNumber >= 0) {
            logger.trace("Loading page {} for '{}'...", pageNumber, pi);
        }
        List<String> fields = new ArrayList<>(Arrays.asList(FIELDS));

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append('+')
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(':')
                .append(pi)
                .append(" +")
                .append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.PAGE);
        if (pageNumber >= 0) {
            sbQuery.append(" +").append(SolrConstants.ORDER).append(':').append(pageNumber);
        }
        if (fileName != null) {
            sbQuery.append(" +").append(SolrConstants.FILENAME).append(":\"").append(fileName).append("\"");
        }
        SolrDocumentList result = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1, null, fields);
        if (result.isEmpty()) {
            return null;
        }

        return loadPageFromDoc(result.get(0), pi, topElement, null);
    }

    @Override
    public PhysicalElement findPageForFilename(String filename) {
        try {
            return loadPage(-1, filename);
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Failed to load page with filename {}. Cause: {}", filename, e.toString());
            return null;
        }
    }
}
