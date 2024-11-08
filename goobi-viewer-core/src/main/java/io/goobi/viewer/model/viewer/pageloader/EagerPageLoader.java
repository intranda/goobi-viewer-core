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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.DataManager;
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
 * Old style page loading strategy (load all pages and keep them in memory).
 */
public class EagerPageLoader extends AbstractPageLoader implements Serializable {

    private static final long serialVersionUID = -7099340930806898778L;

    private static final Logger logger = LogManager.getLogger(EagerPageLoader.class);

    private String pi;
    private Map<Integer, PhysicalElement> pages = new HashMap<>();
    /** Map that holds references to the IDDOC for every page ORDER. */
    private Map<Integer, String> pageOwnerIddocMap = new HashMap<>();
    private int firstPageOrder = 1;
    private int lastPageOrder = 1;

    /**
     * <p>
     * Package private constructor for EagerPageLoader.
     * </p>
     *
     * @param topElement a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */

    EagerPageLoader(StructElement topElement) throws PresentationException, IndexUnreachableException, DAOException {
        pi = topElement.getPi();
        pages = loadAllPages(topElement);
        setFirstAndLastPageOrder();
    }

    /** {@inheritDoc} */
    @Override
    public int getNumPages() {
        return pages.size();
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
    public PhysicalElement getPage(int pageOrder) {
        return pages.get(pageOrder);
    }

    /** {@inheritDoc} */
    @Override
    public PhysicalElement getPageForFileName(String fileName) {
        for (Entry<Integer, PhysicalElement> entry : pages.entrySet()) {
            PhysicalElement page = entry.getValue();
            if (fileName.equals(page.getFileName())) {
                return page;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getOwnerIddocForPage(int pageOrder) throws IndexUnreachableException, PresentationException {
        if (pageOwnerIddocMap.get(pageOrder) == null) {
            logger.warn("IDDOC for page {} not found, retrieving from Solr...", pageOrder);
            String iddoc = DataManager.getInstance().getSearchIndex().getImageOwnerIddoc(pi, pageOrder);
            pageOwnerIddocMap.put(pageOrder, iddoc);
        }

        return pageOwnerIddocMap.get(pageOrder);
    }

    /** {@inheritDoc} */
    @Override
    public void generateSelectItems(List<SelectPageItem> dropdownPages, List<SelectPageItem> dropdownFulltext, String urlRoot,
            boolean recordBelowFulltextThreshold, Locale locale) throws IndexUnreachableException {
        List<Integer> keys = new ArrayList<>(pages.keySet());
        Collections.sort(keys);
        String labelTemplate = buildPageLabelTemplate(DataManager.getInstance().getConfiguration().getPageSelectionFormat(), locale);
        for (int key : keys) {
            PhysicalElement page = pages.get(key);
            SelectPageItem si = buildPageSelectItem(labelTemplate, page.getOrder(), page.getOrderLabel(), null, null);
            dropdownPages.add(si);
            if (dropdownFulltext != null && !(recordBelowFulltextThreshold && !page.isFulltextAvailable())) {
                SelectPageItem full = buildPageSelectItem(labelTemplate, page.getOrder(), page.getOrderLabel(), null, null);
                dropdownFulltext.add(full);
            }
        }
    }

    /**
     * <p>
     * setFirstAndLastPageOrder.
     * </p>
     *
     * @should set first page order correctly
     * @should set last page order correctly
     */
    protected final void setFirstAndLastPageOrder() {
        if (pages != null && !pages.isEmpty()) {
            List<Integer> pagesKeys = new ArrayList<>(pages.keySet());
            Collections.sort(pagesKeys);
            firstPageOrder = pagesKeys.get(0);
            lastPageOrder = pagesKeys.get(pagesKeys.size() - 1);
        }
    }

    /**
     * Generates a list of PhysicalElement objects that belong to this structure element.
     * 
     * @param topElement
     * @return Map<Integer, PhysicalElement> containing all pages
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private Map<Integer, PhysicalElement> loadAllPages(StructElement topElement)
            throws PresentationException, IndexUnreachableException {
        Map<Integer, PhysicalElement> ret = new HashMap<>();

        if (topElement.isAnchor() || topElement.isGroup()) {
            logger.trace("Anchor or group document, no pages.");
            return ret;
        }

        String pi = topElement.getPi();
        if (StringUtils.isEmpty(pi)) {
            logger.debug("PI not found, no pages.");
            return ret;
        }
        logger.trace("Loading pages for '{}'...", pi);
        List<String> fields = new ArrayList<>(Arrays.asList(FIELDS));

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
                .search(sbQuery.toString(), SolrSearchIndex.MAX_HITS, Collections.singletonList(new StringPair(SolrConstants.ORDER, "asc")), fields);
        if (result.isEmpty()) {
            return ret;
        }

        boolean flipRectoVerso = false;
        for (SolrDocument doc : result) {
            PhysicalElement pe = loadPageFromDoc(doc, pi, topElement, pageOwnerIddocMap);
            ret.put(pe.getOrder(), pe);
            if (!pe.isDoubleImage()) {
                pe.setFlipRectoVerso(flipRectoVerso);
                // logger.trace("page {} flipped: {}", pe.getOrder(), pe.isFlipRectoVerso()); //NOSONAR Debug
            }
            if (pe.isDoubleImage()) {
                flipRectoVerso = !flipRectoVerso;
            }
        }

        logger.debug("Loaded {} pages for '{}'.", ret.size(), pi);
        return ret;
    }

    @Override
    public PhysicalElement findPageForFilename(String filename) {
        return this.pages.values().stream().filter(page -> filename.equalsIgnoreCase(page.getFileName())).findAny().orElse(null);
    }
}
