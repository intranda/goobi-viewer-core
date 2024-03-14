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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Page loader that doesn't actually load any pages (for special purposes like TOC generation).
 */
public class EmptyPageLoader extends AbstractPageLoader implements Serializable {

    private static final long serialVersionUID = -2232412689370105369L;

    private static final Logger logger = LogManager.getLogger(EmptyPageLoader.class);

    private String pi;
    private Map<Integer, PhysicalElement> pages = new HashMap<>();
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

    EmptyPageLoader(StructElement topElement) throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("EmptyPageLoader, skipping page loading...");
        pi = topElement.getPi();
        pages = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public int getNumPages() {
        return pages.size();
    }

    /** {@inheritDoc} */
    @Override
    public int getFirstPageOrder() {
        return firstPageOrder;
    }

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
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Long getOwnerIddocForPage(int pageOrder) throws IndexUnreachableException, PresentationException {
        return null;
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

    /** {@inheritDoc} */
    @Override
    public PhysicalElement findPageForFilename(String filename) {
        return null;
    }
}
