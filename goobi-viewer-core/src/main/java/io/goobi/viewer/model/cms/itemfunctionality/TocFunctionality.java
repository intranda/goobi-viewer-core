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
package io.goobi.viewer.model.cms.itemfunctionality;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * TocFunctionality class.
 * </p>
 *
 * @author Florian Alpers
 */
public class TocFunctionality implements Functionality {

    private static Logger logger = LogManager.getLogger(TocFunctionality.class);

    private TOC toc = null;

    private StructElement docStruct = null;

    private int currentPage = 1;

    private final String pi;

    /**
     * <p>
     * Constructor for TocFunctionality.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     */
    public TocFunctionality(String pi) {
        this.pi = pi;
    }

    /**
     * <p>
     * Constructor for TocFunctionality.
     * </p>
     *
     * @param blueprint a {@link io.goobi.viewer.model.cms.itemfunctionality.TocFunctionality} object.
     */
    public TocFunctionality(TocFunctionality blueprint) {
        this.toc = blueprint.toc;
        this.docStruct = blueprint.docStruct;
        this.currentPage = blueprint.currentPage;
        this.pi = blueprint.pi;
    }

    /**
     * <p>
     * getBannerUrl.
     * </p>
     *
     * @param width a int.
     * @param height a int.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
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

    /**
     * <p>
     * Getter for the field <code>toc</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.toc.TOC} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public TOC getToc() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (toc == null || toc.getCurrentPage() != this.getPageNo()) {
            toc = createToc();
        }
        return toc;
    }

    /**
     * <p>
     * Getter for the field <code>docStruct</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public StructElement getDocStruct() throws IndexUnreachableException, PresentationException {
        if (docStruct == null) {
            docStruct = createDocStruct();
        }
        return docStruct;
    }

    /**
     * @return Created {@link StructElement}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private StructElement createDocStruct() throws IndexUnreachableException, PresentationException {
        if (StringUtils.isNotBlank(getPi())) {
            long topDocumentIddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(getPi());
            return new StructElement(topDocumentIddoc);
        }

        return new StructElement();
    }

    /**
     * @return Created {@link TOC}
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    private TOC createToc() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        TOC ret = new TOC();
        ret.generate(getDocStruct(), false, getDocStruct().getMetadataValue(SolrConstants.MIMETYPE), currentPage);
        ret.setCurrentPage(currentPage);
        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the piPeriodical
     */
    public String getPi() {
        return pi == null ? "" : pi;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.itemfunctionality.Functionality#setPageNo(int)
     */
    /** {@inheritDoc} */
    @Override
    public void setPageNo(int pageNo) {
        this.currentPage = pageNo;
    }

    /** {@inheritDoc} */
    @Override
    public int getPageNo() {
        return this.currentPage;
    }

}
