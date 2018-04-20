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
package de.intranda.digiverso.presentation.model.iiif.presentation.builder;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.iiif.presentation.Manifest;
import de.intranda.digiverso.presentation.model.iiif.presentation.Range;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.LinkingContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.Format;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * @author Florian Alpers
 *
 */
public class StructureBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StructureBuilder.class);
    
    public static final String BASE_RANGE_LABEL = "CONTENT";
    
    /**
     * @param request
     * @throws URISyntaxException
     */
    public StructureBuilder(HttpServletRequest request) throws URISyntaxException {
        super(request);
    }

    /**
     * @param servletUri
     * @param requestURI
     */
    public StructureBuilder(URI servletUri, URI requestURI) {
        super(servletUri, requestURI);
    }
    
    public Range generateStructure(StructElement baseElement, URI uri) throws ConfigurationException, IndexUnreachableException, DAOException, PresentationException {
        Range range = new Range(uri);
        IMetadataValue label = baseElement.isWork() ? IMetadataValue.getTranslations(BASE_RANGE_LABEL) : baseElement.getMultiLanguageMetadataValue(SolrConstants.TITLE);
        range.setLabel(label);
        if(baseElement.isWork()) {
            range.setViewingHint(ViewingHint.top);
        } else {
            populate(baseElement, range);
        }
        return range;
    }
    
    /**
     * @param ele
     * @param manifest
     * @throws ConfigurationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public void populate(StructElement ele, final Range range)
            throws ConfigurationException, IndexUnreachableException, DAOException, PresentationException {

        addMetadata(range, ele);

        try {
            String thumbUrl = BeanUtils.getImageDeliveryBean().getThumb().getThumbnailUrl(ele);
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageContent thumb = new ImageContent(new URI(thumbUrl), true);
                range.setThumbnail(thumb);
            }
        } catch (URISyntaxException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }

        /*VIEWER*/
        try {
            LinkingContent viewerPage = new LinkingContent(new URI(getServletURI() + ele.getUrl()));
            viewerPage.setLabel(new SimpleMetadataValue("goobi viewer"));
            range.addRendering(viewerPage);
        } catch (URISyntaxException e) {
            logger.error("Unable to retrieve viewer url for {}", ele);
        }

            /*PDF*/
            try {
                String pdfDownloadUrl = BeanUtils.getImageDeliveryBean().getPdf().getPdfUrl(ele, range.getLabel().getValue().orElse(null));
                LinkingContent pdfDownload = new LinkingContent(new URI(pdfDownloadUrl));
                pdfDownload.setFormat(Format.APPLICATION_PDF);
                pdfDownload.setLabel(new SimpleMetadataValue("PDF"));
                range.addRendering(pdfDownload);
            } catch (URISyntaxException e) {
                logger.error("Unable to retrieve pdf download url for {}", ele);
            }

    }
    

    /**
     * @param logid
     * @return
     */
    private URI generateRangeId(String logid, URI manifestURI) {
        URI uri = manifestURI.resolve("range").resolve(logid);
        return uri;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.iiif.presentation.builder.AbstractBuilder#getPath()
     */
    @Override
    protected String getPath() {
        return "/manifests";
    }

}
