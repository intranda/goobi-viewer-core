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
package io.goobi.viewer.model.viewer.pageloader;

import java.util.List;
import java.util.Locale;

import javax.faces.model.SelectItem;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * IPageLoader interface.
 * </p>
 */
public interface IPageLoader {

    /** All fields to be fetched when loading page documents. Any new required fields must be added to this array. */
    public static final String[] FIELDS = { SolrConstants.PI_TOPSTRUCT, SolrConstants.PHYSID, SolrConstants.ORDER, SolrConstants.ORDERLABEL,
            SolrConstants.IDDOC_OWNER, SolrConstants.MIMETYPE, SolrConstants.FILEIDROOT, SolrConstants.FILENAME, SolrConstants.FILENAME_ALTO,
            SolrConstants.FILENAME_FULLTEXT, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.FILENAME_MPEG, SolrConstants.FILENAME_MPEG3,
            SolrConstants.FILENAME_MP4, SolrConstants.FILENAME_OGG, SolrConstants.FILENAME_WEBM, SolrConstants.FULLTEXTAVAILABLE,
            SolrConstants.DATAREPOSITORY, SolrConstants.IMAGEURN, SolrConstants.WIDTH, SolrConstants.HEIGHT, SolrConstants.ACCESSCONDITION,
            SolrConstants.MDNUM_FILESIZE, SolrConstants.BOOL_IMAGEAVAILABLE, SolrConstants.BOOL_DOUBLE_IMAGE };

    /**
     * <p>
     * getNumPages.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getNumPages() throws IndexUnreachableException;

    /**
     * <p>
     * getFirstPageOrder.
     * </p>
     *
     * @return a int.
     */
    public int getFirstPageOrder();

    /**
     * <p>
     * getLastPageOrder.
     * </p>
     *
     * @return a int.
     */
    public int getLastPageOrder();

    /**
     * <p>
     * getPage.
     * </p>
     *
     * @param pageOrder a int.
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getPage(int pageOrder) throws IndexUnreachableException;

    /**
     * <p>
     * getPageForFileName.
     * </p>
     *
     * @param fileName a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getPageForFileName(String fileName) throws PresentationException, IndexUnreachableException, DAOException;

    /**
     * <p>
     * getOwnerIddocForPage.
     * </p>
     *
     * @param pageOrder a int.
     * @return a {@link java.lang.Long} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public Long getOwnerIddocForPage(int pageOrder) throws IndexUnreachableException, PresentationException;

    /**
     * <p>
     * generateSelectItems.
     * </p>
     *
     * @param dropdownPages Image view drop-down item
     * @param dropdownFulltext Full-text view drop-down item list
     * @param urlRoot a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @param recordBelowFulltextThreshold a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void generateSelectItems(List<SelectItem> dropdownPages, List<SelectItem> dropdownFulltext, String urlRoot,
            boolean recordBelowFulltextThreshold, Locale locale) throws IndexUnreachableException;
}
