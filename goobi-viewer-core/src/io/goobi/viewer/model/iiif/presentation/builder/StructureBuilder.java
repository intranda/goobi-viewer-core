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
package io.goobi.viewer.model.iiif.presentation.builder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.Range;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * @author Florian Alpers
 *
 */
public class StructureBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(StructureBuilder.class);
    protected ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();

    public static final String BASE_RANGE_LABEL = "CONTENT";

    /**
     * @param request
     * @throws URISyntaxException
     */
    public StructureBuilder(HttpServletRequest request) {
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
     * Generates the topmost range from the given elements. This is an abstract "CONTENT" range if baseElement is a work, or the range representing
     * the given baseElement otherwise
     * 
     * @param elements All elements to include in the list
     * @param uri
     * @param useMembers
     * @return
     * @throws ViewerConfigurationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @throws URISyntaxException
     */
    public List<Range> generateStructure(List<StructElement> elements, String pi, boolean useMembers)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException, PresentationException, URISyntaxException {
        List<Range> ranges = new ArrayList<>();
        Map<String, String> idMap = new HashMap<>();
        if (elements != null && !elements.isEmpty()) {

            for (StructElement structElement : elements) {
                URI rangeURI = getRangeURI(pi, structElement.getLogid());
                Range range = new Range(rangeURI);
                range.setUseMembers(useMembers);
                idMap.put(Long.toString(structElement.getLuceneId()), structElement.getLogid());
                if (structElement.isWork()) {
                    IMetadataValue label = ViewerResourceBundle.getTranslations(BASE_RANGE_LABEL);
                    range.setLabel(label);
                    range.setViewingHint(ViewingHint.top);
                } else {
                    IMetadataValue label = structElement.getMultiLanguageDisplayLabel();
                    range.setLabel(label);
                    String parentId = idMap.get(structElement.getMetadataValue(SolrConstants.IDDOC_PARENT));
                    if(StringUtils.isNotBlank(parentId)) {                        
                        range.addWithin(new Range(getRangeURI(pi, parentId)));
                    }
                    populatePages(structElement, pi, range);
                    populate(structElement, pi, range);
                }                
                populateChildren(elements, structElement.getLuceneId(), pi, range);
                ranges.add(range);
            }

        }
        return ranges;
    }

    /**
     * Generates the list of child ranges of the given range from the given elements which have the given parentiddoc
     * 
     * @param elements
     * @param luceneId
     * @param range
     */
    private void populateChildren(List<StructElement> elements, long parentIddoc, String pi, Range range) {
        elements.stream().filter(element -> Long.toString(parentIddoc).equals(element.getMetadataValue(SolrConstants.IDDOC_PARENT)))
        .map(element -> element.getLogid())
        .forEach(logId -> {
                range.addRange(new Range(getRangeURI(pi, logId)));
        });
        
    }

    /**
     * Adds Metadata and links to external services to a range
     * 
     * @param ele
     * @param manifest
     * @throws ViewerConfigurationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public void populate(StructElement ele, String pi, final Range range)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException, PresentationException {

        addMetadata(range, ele);

        try {
            String thumbUrl = imageDelivery.getThumbs().getThumbnailUrl(ele, pi);
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageContent thumb = new ImageContent(new URI(thumbUrl));
                range.setThumbnail(thumb);
                if(IIIFUrlResolver.isIIIFImageUrl(thumbUrl)) {   
                    URI imageInfoURI = new URI(IIIFUrlResolver.getIIIFImageBaseUrl(thumbUrl));
                    thumb.setService(new ImageInformation(imageInfoURI.toString()));
                }
            }
        } catch (URISyntaxException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }

        /*VIEWER*/
        try {
        	String url = DataManager.getInstance().getUrlBuilder().buildPageUrl(pi, ele.getImageNumber(), ele.getLogid(), PageType.viewObject);
            LinkingContent viewerPage = new LinkingContent(new URI(url));
            viewerPage.setLabel(new SimpleMetadataValue("goobi viewer"));
            range.addRendering(viewerPage);
        } catch (URISyntaxException e) {
            logger.error("Unable to retrieve viewer url for {}", ele);
        }

        /*PDF*/
        try {
            String pdfDownloadUrl = imageDelivery.getPdf().getPdfUrl(ele, pi, range.getLabel().getValue().orElse(null));
            LinkingContent pdfDownload = new LinkingContent(new URI(pdfDownloadUrl));
            pdfDownload.setFormat(Format.APPLICATION_PDF);
            pdfDownload.setLabel(new SimpleMetadataValue("PDF"));
            range.addRendering(pdfDownload);
        } catch (URISyntaxException e) {
            logger.error("Unable to retrieve pdf download url for {}", ele);
        }


    }


    /**
     * @param doc
     * @param topRange
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     */
    public void populatePages(StructElement doc, String pi, Range range) throws URISyntaxException, IndexUnreachableException {
        int startPageNo = doc.getImageNumber();
        int numPages = 1;
        try {            
            numPages = Integer.parseInt(doc.getMetadataValue(SolrConstants.NUMPAGES));
        } catch(NullPointerException | NumberFormatException e) {
            //can't determine number of pages. Ignore
        }
        if (startPageNo > 0) {
            for (int i = startPageNo; i < startPageNo + numPages; i++) {
                URI pageURI = getCanvasURI(pi, i);
                Canvas canvas = new Canvas(pageURI);
                range.addCanvas(canvas);
            }
        }
    }

    /**
     * @param topRange
     */
    public List<Range> getDescendents(Range range) {
        List<Range> children = new ArrayList<>();
        for (Range child : range.getRangeList()) {
            children.add(child);
            children.addAll(getDescendents(child));
        }
        return children;
    }

}
