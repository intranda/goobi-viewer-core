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

import java.util.List;

import javax.faces.model.SelectItem;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;

public interface IPageLoader {

    public static final String[] FIELDS = { SolrConstants.PI_TOPSTRUCT, SolrConstants.PHYSID, SolrConstants.ORDER, SolrConstants.ORDERLABEL,
            SolrConstants.IDDOC_OWNER, SolrConstants.MIMETYPE, SolrConstants.FILEIDROOT, SolrConstants.FILENAME, SolrConstants.FILENAME_ALTO,
            SolrConstants.FILENAME_FULLTEXT, SolrConstants.FILENAME_HTML_SANDBOXED, SolrConstants.FILENAME_MPEG, SolrConstants.FILENAME_MPEG3,
            SolrConstants.FILENAME_MP4, SolrConstants.FILENAME_OGG, SolrConstants.FILENAME_WEBM, SolrConstants.FULLTEXTAVAILABLE,
            SolrConstants.DATAREPOSITORY, SolrConstants.IMAGEURN, SolrConstants.WIDTH, SolrConstants.HEIGHT, SolrConstants.ACCESSCONDITION,
            SolrConstants.MDNUM_FILESIZE };

    public int getNumPages() throws IndexUnreachableException;

    public int getFirstPageOrder();

    public int getLastPageOrder();

    public PhysicalElement getPage(int pageOrder) throws IndexUnreachableException, DAOException;

    public PhysicalElement getPageForFileName(String fileName) throws PresentationException, IndexUnreachableException, DAOException;

    public Long getOwnerIddocForPage(int pageOrder) throws IndexUnreachableException, PresentationException;

    /**
     * 
     * @param dropdownPages Image view drop-down item
     * @param dropdownFulltext Full-text view drop-down item list
     * @param urlRoot
     * @param recordBelowFulltextThreshold
     * @throws IndexUnreachableException
     */
    public void generateSelectItems(List<SelectItem> dropdownPages, List<SelectItem> dropdownFulltext, String urlRoot,
            boolean recordBelowFulltextThreshold) throws IndexUnreachableException;
}
