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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.iiif.presentation.Canvas;
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

    /**
     * Generates the topmost range from the given baseElement. This is an abstract "CONTENT" range if baseElement is a work, or the range representing
     * the given baseElement otherwise
     * 
     * @param baseElement
     * @param uri
     * @param useMembers
     * @return
     * @throws ConfigurationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @throws URISyntaxException 
     */
    public Range generateStructure(StructElement baseElement, URI uri, boolean useMembers)
            throws ConfigurationException, IndexUnreachableException, DAOException, PresentationException, URISyntaxException {
        Range range = new Range(uri);
        range.setUseMembers(useMembers);
        IMetadataValue label = baseElement.getMultiLanguageDisplayLabel();
        range.setLabel(label);
        populate(baseElement, range);
        populateChildren(baseElement, range, useMembers);
        return range;
    }

    /**
     * Adds Metadata and links to external services to a range
     * 
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

        if (range.isUseMembers()) {
            try {
                int startPageNo = ele.getImageNumber();
                if (startPageNo > 0) {
                    URI pageURI = getCanvasURI(ele.getPi(), startPageNo);
                    range.setStartCanvas(new Canvas(pageURI));
                }
            } catch (URISyntaxException e) {
                logger.error("Unable to create start page URI for {}", ele);
            }
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

    /**
     * @param doc
     * @param topRange
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws URISyntaxException
     * @throws DAOException
     * @throws ConfigurationException
     */
    public void populateChildren(StructElement doc, Range topRange, boolean useMembers)
            throws PresentationException, IndexUnreachableException, ConfigurationException, DAOException, URISyntaxException {
        List<StructElement> children = doc.getChildren(getSolrFieldList());
        for (StructElement structElement : children) {
            Range child = generateStructure(structElement, getRangeURI(doc.getPi(), structElement.getLogid()), useMembers);
            topRange.addRange(child);
        }

    }

    /**
     * @param doc
     * @param topRange
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     */
    public void populatePages(StructElement doc, Range range) throws URISyntaxException, IndexUnreachableException {
        int startPageNo = doc.getImageNumber();
        int numPages = doc.getNumPages();
        if (startPageNo > 0) {
            for (int i = startPageNo; i < numPages; i++) {
                URI pageURI = getCanvasURI(doc.getPi(), i);
                Canvas canvas = new Canvas(pageURI);
                range.addCanvas(canvas);
            }
        }
    }

}
