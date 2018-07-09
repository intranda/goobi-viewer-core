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
package de.intranda.digiverso.presentation.model.cms.itemfunctionality;

import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.model.toc.TOC;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * @author Florian Alpers
 *
 */
public class TocFunctionality implements Functionality {

    private static Logger logger = LoggerFactory.getLogger(TocFunctionality.class);

    private TOC toc = null;

    private StructElement docStruct = null;

    private int currentPage = 1;

    private final String pi;

    public TocFunctionality(String pi) {
        this.pi = pi;
    }

    /**
     * @param cmsContentItemPeriodical
     */
    public TocFunctionality(TocFunctionality blueprint) {
        this.toc = blueprint.toc;
        this.docStruct = blueprint.docStruct;
        this.currentPage = blueprint.currentPage;
        this.pi = blueprint.pi;
    }

    public String getBannerUrl(int width, int height)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        String url = getDocStruct().getImageUrl(width, height);
        if (StringUtils.isBlank(url)) {
            Optional<String> thumb = getToc().getTocElements()
                    .stream()
                    .filter(element -> StringUtils.isNotBlank(element.getThumbnailUrl()))
                    .map(element -> element.getThumbnailUrl(width, height))
                    .findFirst();
            url = thumb.orElseGet(() -> "");
        }
        return url;
    }

    public TOC getToc() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (toc == null || toc.getCurrentPage() != this.getPageNo()) {
            toc = createToc();
        }
        return toc;
    }

    public StructElement getDocStruct() throws IndexUnreachableException, PresentationException {
        if (docStruct == null) {
            docStruct = createDocStruct();
        }
        return docStruct;
    }

    /**
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private StructElement createDocStruct() throws IndexUnreachableException, PresentationException {
        if (StringUtils.isNotBlank(getPi())) {
            long topDocumentIddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(getPi());
            StructElement struct = new StructElement(topDocumentIddoc);
            return struct;
        }

        return new StructElement();
    }

    /**
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    private TOC createToc() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        TOC toc = new TOC();
        toc.generate(getDocStruct(), false, getDocStruct().getMetadataValue(SolrConstants.MIMETYPE), currentPage);
        toc.setCurrentPage(currentPage);
        return toc;
    }

    @Override
    public TocFunctionality clone() {
        TocFunctionality clone = new TocFunctionality(this);
        return clone;
    }

    /**
     * @return the piPeriodical
     */
    public String getPi() {
        return pi == null ? "" : pi;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.cms.itemfunctionality.Functionality#setPageNo(int)
     */
    @Override
    public void setPageNo(int pageNo) {
        this.currentPage = pageNo;
    }

    @Override
    public int getPageNo() {
        return this.currentPage;
    }

}
