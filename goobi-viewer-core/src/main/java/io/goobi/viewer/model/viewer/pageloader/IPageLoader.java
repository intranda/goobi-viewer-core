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
import java.util.List;
import java.util.Locale;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.PhysicalElement;

/**
 * IPageLoader interface.
 */
public interface IPageLoader extends Serializable {

    /**
     * getNumPages.
     *
     * @return a int.
     */
    public int getNumPages();

    /**
     * getFirstPageOrder.
     *
     * @return a int.
     */
    public int getFirstPageOrder();

    /**
     * getLastPageOrder.
     *
     * @return a int.
     */
    public int getLastPageOrder();

    /**
     * getPage.
     *
     * @param pageOrder physical page order number (1-based)
     * @return the PhysicalElement at the given page order, or null if not found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getPage(int pageOrder) throws IndexUnreachableException;

    /**
     * getPageForFileName.
     *
     * @param fileName base file name of the page to look up
     * @return the PhysicalElement with the given file name, or null if not found
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getPageForFileName(String fileName) throws PresentationException, IndexUnreachableException, DAOException;

    /**
     * getOwnerIddocForPage.
     *
     * @param pageOrder physical page order number to look up
     * @return the IDDOC of the owning struct element for the given page order
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public String getOwnerIddocForPage(int pageOrder) throws IndexUnreachableException, PresentationException;

    /**
     * generateSelectItems.
     *
     * @param dropdownPages Image view drop-down item
     * @param dropdownFulltext Full-text view drop-down item list
     * @param urlRoot base URL prepended to page navigation links
     * @param locale locale used for page label translations
     * @param recordBelowFulltextThreshold true if fulltext availability is below the configured threshold
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void generateSelectItems(List<SelectPageItem> dropdownPages, List<SelectPageItem> dropdownFulltext, String urlRoot,
            boolean recordBelowFulltextThreshold, Locale locale) throws IndexUnreachableException;

    public PhysicalElement findPageForFilename(String filename);
}
