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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v2.AbstractPresentationModelElement2;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Range2;
import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.StructElementStub;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * StructureBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class StructureBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(StructureBuilder.class);
    protected ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();

    /** Constant <code>BASE_RANGE_LABEL="CONTENT"</code>. */
    public static final String BASE_RANGE_LABEL = "CONTENT";

    /**
     * <p>
     * Constructor for StructureBuilder.
     * </p>
     *
     * @param apiUrlManager
     */
    public StructureBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }

    /**
     * Generates the topmost range from the given elements. This is an abstract "CONTENT" range if baseElement is a work, or the range representing
     * the given baseElement otherwise.
     *
     * @param elements All elements to include in the list
     * @param useMembers a boolean.
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws java.net.URISyntaxException if any.
     */
    public List<Range2> generateStructure(List<StructElement> elements, String pi, boolean useMembers)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException, PresentationException, URISyntaxException {
        List<Range2> ranges = new ArrayList<>();
        Map<String, String> idMap = new HashMap<>();
        if (elements != null && !elements.isEmpty()) {
            Optional<StructElement> work = Optional.empty();
            for (StructElement structElement : elements) {
                URI rangeURI = getRangeURI(pi, structElement.getLogid());
                Range2 range = new Range2(rangeURI);
                range.setUseMembers(useMembers);
                idMap.put(Long.toString(structElement.getLuceneId()), structElement.getLogid());
                if (structElement.isWork()) {
                    IMetadataValue label = getLabel(BASE_RANGE_LABEL);
                    range.setLabel(label);
                    range.addViewingHint(ViewingHint.top);
                    work = Optional.of(structElement);
                } else {
                    IMetadataValue label = structElement.getMultiLanguageDisplayLabel();
                    range.setLabel(label);
                    String parentId = idMap.get(structElement.getMetadataValue(SolrConstants.IDDOC_PARENT));
                    if (StringUtils.isNotBlank(parentId)) {
                        range.addWithin(new Range2(getRangeURI(pi, parentId)));
                    }
                    work.ifPresent(structElement::setTopStruct);
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
     * Generates the list of child ranges of the given range from the given elements which have the given parent iddoc.
     *
     * @param elements
     * @param parentIddoc
     * @param pi
     * @param range
     */
    private void populateChildren(List<StructElement> elements, long parentIddoc, String pi, Range2 range) {
        elements.stream()
                .filter(element -> Long.toString(parentIddoc).equals(element.getMetadataValue(SolrConstants.IDDOC_PARENT)))
                .map(StructElementStub::getLogid)
                .forEach(logId -> range.addRange(new Range2(getRangeURI(pi, logId))));

    }

    /**
     * Adds Metadata and links to external services to a range.
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pi a {@link java.lang.String} object.
     * @param range a {@link de.intranda.api.iiif.presentation.v2.Range2} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public void populate(StructElement ele, String pi, final Range2 range)
            throws ViewerConfigurationException, IndexUnreachableException, DAOException, PresentationException {

        addMetadata(range, ele);

        try {
            String thumbUrl = imageDelivery.getThumbs().getThumbnailUrl(ele, pi);
            if (StringUtils.isNotBlank(thumbUrl)) {
                ImageContent thumb = new ImageContent(new URI(thumbUrl));
                range.addThumbnail(thumb);
                if (IIIFUrlResolver.isIIIFImageUrl(thumbUrl)) {
                    URI imageInfoURI = new URI(IIIFUrlResolver.getIIIFImageBaseUrl(thumbUrl));
                    thumb.setService(new ImageInformation(imageInfoURI.toString()));
                }
            }
        } catch (URISyntaxException e) {
            logger.warn("Unable to retrieve thumbnail url", e);
        }

        addRenderings(range, ele);

    }

    /**
     * @param range
     * @param ele
     */
    public void addRenderings(AbstractPresentationModelElement2 range, StructElement ele) {

        this.getRenderings().forEach(link -> {
            try {
                URI id = getLinkingPropertyUri(ele, link.getTarget());
                if (id != null) {
                    range.addRendering(link.getLinkingContent(id));
                }
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Error building linking property url", e);
            }
        });
    }

    private URI getLinkingPropertyUri(StructElement ele, LinkingTarget target) throws PresentationException, IndexUnreachableException {

        if (target.equals(LinkingTarget.PDF) && !ele.getTopStruct().isHasImages()) {
            return null;
        }

        URI uri = null;
        switch (target) {
            case VIEWER:
                String applicationUrl = this.urls.getApplicationUrl();
                String pageUrl = ele.getUrl();
                uri = URI.create(applicationUrl + pageUrl);
                break;
            case PDF:
                String pdfDownloadUrl = imageDelivery.getPdf().getPdfUrl(ele, ele.getPi(), ele.getLabel());
                uri = URI.create(pdfDownloadUrl);
                break;
            default:
                break;
        }
        return uri;
    }

    /**
     * <p>
     * populatePages.
     * </p>
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pi a {@link java.lang.String} object.
     * @param range a {@link de.intranda.api.iiif.presentation.v2.Range2} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void populatePages(StructElement doc, String pi, Range2 range) throws URISyntaxException, IndexUnreachableException {
        int startPageNo = doc.getImageNumber();
        int numPages = 1;
        try {
            numPages = Integer.parseInt(doc.getMetadataValue(SolrConstants.NUMPAGES));
        } catch (NullPointerException | NumberFormatException e) {
            //can't determine number of pages. Ignore
        }
        if (startPageNo > 0) {
            for (int i = startPageNo; i < startPageNo + numPages; i++) {
                URI pageURI = getCanvasURI(pi, i);
                Canvas2 canvas = new Canvas2(pageURI);
                range.addCanvas(canvas);
            }
        }
    }

    /**
     * <p>
     * getDescendents.
     * </p>
     *
     * @param range a {@link de.intranda.api.iiif.presentation.v2.Range2} object.
     * @return a {@link java.util.List} object.
     */
    public List<Range2> getDescendents(Range2 range) {
        List<Range2> children = new ArrayList<>();
        for (Range2 child : range.getRangeList()) {
            children.add(child);
            children.addAll(getDescendents(child));
        }
        return children;
    }

}
