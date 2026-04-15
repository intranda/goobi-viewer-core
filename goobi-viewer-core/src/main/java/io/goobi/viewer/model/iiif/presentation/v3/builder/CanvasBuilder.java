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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_IMAGE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_IMAGE_PDF;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_PLAINTEXT;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.wa.ImageResource;
import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.api.iiif.presentation.v3.LabeledResource;
import de.intranda.api.services.Service;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.api.rest.v2.auth.AuthorizationFlowTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ImageHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v3.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.PagePermissions;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;

/**
 * @author Florian Alpers
 */
public class CanvasBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(CanvasBuilder.class);

    private final ImageHandler images;
    private final AbstractApiUrlManager imageUrlManager = DataManager.getInstance().getRestApiManager().getIIIFContentApiManager();

    private PagePermissions pagePermissions = PagePermissions.EMPTY;
    // Cached page dimensions keyed by ORDER; populated by preparePageDimensions() before the page loop.
    // Empty map means no batch data available — falls back to per-page disk I/O in addImageResource().
    private Map<Integer, Dimension> pageDimensions = Map.of();

    /**
     * Injects pre-fetched page permissions; package-private to allow test injection without reflection.
     *
     * @param pagePermissions pre-fetched permissions to use instead of per-page Solr queries
     */
    void setPagePermissions(PagePermissions pagePermissions) {
        this.pagePermissions = pagePermissions;
    }

    /**
     * Injects pre-fetched page dimensions; package-private to allow test injection without reflection.
     *
     * @param pageDimensions map of page ORDER to Dimension, replacing the per-page disk I/O fallback
     */
    void setPageDimensions(Map<Integer, Dimension> pageDimensions) {
        this.pageDimensions = pageDimensions;
    }

    /**
     * @param apiUrlManager URL manager providing API endpoint paths
     * @param request current HTTP servlet request
     */
    public CanvasBuilder(AbstractApiUrlManager apiUrlManager, HttpServletRequest request) {
        super(apiUrlManager, request);
        this.images = new ImageHandler(urls);
    }

    /**
     * Pre-fetches access permissions for all pages of the given record in one Solr query.
     * Must be called before the page loop (from
     * {@link ManifestBuilder}) to enable O(1) per-page lookups instead of O(n) Solr queries.
     *
     * @param pi persistent identifier of the record whose pages to pre-fetch
     */
    public void preparePagePermissions(String pi) {
        // Single batch fetch instead of one Solr query per page per privilege type
        this.pagePermissions = AccessConditionUtils.fetchPagePermissions(pi, this.request);
    }

    /**
     * Pre-fetches image dimensions (WIDTH, HEIGHT) for all pages of the given record in one Solr
     * query. Must be called before the page loop (from {@link ManifestBuilder}) to avoid O(n)
     * disk reads via ImageHandler.getImageInformation() in addImageResource().
     *
     * @param pi persistent identifier of the record whose page dimensions to pre-fetch
     */
    public void preparePageDimensions(String pi) {
        if (org.apache.commons.lang3.StringUtils.isBlank(pi)) {
            return;
        }
        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi
                + " +" + SolrConstants.DOCTYPE + ":" + DocType.PAGE;
        try {
            org.apache.solr.common.SolrDocumentList docs = DataManager.getInstance().getSearchIndex()
                    .search(query, SolrSearchIndex.MAX_HITS, null,
                            List.of(SolrConstants.ORDER, SolrConstants.WIDTH, SolrConstants.HEIGHT));
            if (docs == null || docs.isEmpty()) {
                return;
            }
            Map<Integer, Dimension> map = new HashMap<>();
            for (org.apache.solr.common.SolrDocument doc : docs) {
                Object orderObj = doc.getFieldValue(SolrConstants.ORDER);
                Object widthObj = doc.getFieldValue(SolrConstants.WIDTH);
                Object heightObj = doc.getFieldValue(SolrConstants.HEIGHT);
                if (orderObj == null || widthObj == null || heightObj == null) {
                    continue;
                }
                int w = ((Number) widthObj).intValue();
                int h = ((Number) heightObj).intValue();
                if (w > 0 && h > 0) {
                    map.put(((Number) orderObj).intValue(), new Dimension(w, h));
                }
            }
            this.pageDimensions = map;
        } catch (IndexUnreachableException | PresentationException e) {
            logger.warn("Failed to pre-fetch page dimensions for {}: {}", pi, e.getMessage());
        }
    }

    /**
     *
     * @param pi persistent identifier of the record
     * @param order physical page order number of the canvas to build
     * @return {@link Canvas3}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentLibException
     * @throws URISyntaxException
     * @throws DAOException
     * @should return 1200 for given input
     * @should include image
     * @should build for given input
     */
    public Canvas3 build(String pi, int order)
            throws PresentationException, IndexUnreachableException, ContentLibException, URISyntaxException, DAOException {
        StructElement topStruct = this.dataRetriever.getDocument(pi);
        PhysicalElement page = AbstractPageLoader.loadPage(topStruct, order);
        if (page != null) {
            return build(page);
        }

        throw new ContentNotFoundException(String.format("Not canvas found at order %d in record %s", order, pi));
    }

    /**
     *
     * @param pi persistent identifier of the record
     * @param order physical page order number whose fulltext annotations to build
     * @return {@link AnnotationPage}
     * @throws ContentLibException
     * @throws URISyntaxException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public AnnotationPage buildFulltextAnnotations(String pi, int order)
            throws ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException, DAOException {
        StructElement topStruct = this.dataRetriever.getDocument(pi);
        PhysicalElement page = AbstractPageLoader.loadPage(topStruct, order);
        Canvas3 canvas = build(page);
        return getFulltextAnnotations(canvas, page);
    }

    /**
     *
     * @param page physical page element to build the canvas from
     * @return {@link Canvas3}
     * @throws ContentLibException
     * @throws URISyntaxException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public Canvas3 build(PhysicalElement page)
            throws ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException, DAOException {
        URI canvasUri = this.urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_CANVAS).params(page.getPi(), page.getOrder()).buildURI();
        Canvas3 canvas = new Canvas3(canvasUri);
        canvas.setLabel(new SimpleMetadataValue(page.getOrderLabel()));

        if (page.getMediaType().isAllowsImageView()) {
            addImageResource(canvas, page);
        }

        canvas.addThumbnail(getThumbnail(page));

        canvas.addAnnotations(getFulltextAnnotationsReference(page));
        canvas.addAnnotations(getCommentAnnotationsReference(page));
        canvas.addAnnotations(getCrowdsourcingAnnotationsReference(page));

        addRelatedResources(canvas, page);

        return canvas;

    }

    /**
     * @param page physical page element for which the comment annotations reference is built
     * @return {@link AnnotationPage}
     */
    private AnnotationPage getCommentAnnotationsReference(PhysicalElement page) {
        URI annoPageUri = this.urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_COMMENTS).params(page.getPi(), page.getOrder()).buildURI();
        return new AnnotationPage(annoPageUri, false);
    }

    /**
     *
     * @param page physical page element for which the crowdsourcing annotations reference is built
     * @return {@link AnnotationPage}
     */
    private AnnotationPage getCrowdsourcingAnnotationsReference(PhysicalElement page) {
        URI annoPageUri = this.urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_ANNOTATIONS).params(page.getPi(), page.getOrder()).buildURI();
        return new AnnotationPage(annoPageUri, false);
    }

    /**
     * Gets a reference to an annotation page containing all fulltext annotations for the given page.
     *
     * @param page physical page element to check for fulltext availability
     * @return The annotation page, or null if no fulltext is available for the given page
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    private AnnotationPage getFulltextAnnotationsReference(PhysicalElement page) throws IndexUnreachableException, DAOException {
        if (page.isFulltextAvailable()) {
            URI annoPageUri = this.urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_TEXT).params(page.getPi(), page.getOrder()).buildURI();
            AnnotationPage ret = new AnnotationPage(annoPageUri, false);
            // Use pre-fetched permissions when available; fall back to per-page check for single-canvas builds
            if (!(pagePermissions.isEmpty() ? page.isAccessPermissionFulltext()
                    : pagePermissions.isFulltextGranted(page.getOrder()))) {
                // Add auth services
                for (Service service : AuthorizationFlowTools.getAuthServices(page.getPi(), page.getAltoFileName())) {
                    ret.addService(service);
                }
            }
            return ret;
        }

        return null;
    }

    /**
     * Returns an annotation page with any fulltext annotations for the given page. The annotation page contains the @context property
     *
     * @param canvas canvas to target when building annotation bodies
     * @param page physical page element providing ALTO or plaintext content
     * @return {@link AnnotationPage}
     * @throws IndexUnreachableException
     */
    private AnnotationPage getFulltextAnnotations(Canvas3 canvas, PhysicalElement page) throws IndexUnreachableException {
        TextResourceBuilder builder = new TextResourceBuilder();
        URI annoPageUri = this.urls.path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_PAGES_TEXT).params(page.getPi(), page.getOrder()).buildURI();
        AnnotationPage annoPage = new AnnotationPage(annoPageUri, true);
        if (StringUtils.isNotBlank(page.getAltoFileName())) {
            try {
                String altoFilename = Paths.get(page.getAltoFileName()).getFileName().toString();
                StringPair altoPair = builder.getAltoDocument(page.getPi(), altoFilename);
                if (StringUtils.isNotBlank(altoPair.getOne())) {
                    AltoDocument alto = AltoDocument.getDocumentFromString(altoPair.getOne(), altoPair.getTwo());
                    if (alto.getFirstPage() != null && StringUtils.isNotBlank(alto.getFirstPage().getContent())) {
                        AltoAnnotationBuilder altoBuilder = new AltoAnnotationBuilder(urls, null);
                        List<AbstractAnnotation> annos =
                                altoBuilder.createAnnotations(alto.getFirstPage(), page.getPi(), page.getOrder(), canvas,
                                        AltoAnnotationBuilder.Granularity.LINE, false);
                        for (AbstractAnnotation annotation : annos) {
                            annotation.setMotivation(Motivation.SUPPLEMENTING);
                            annoPage.addItem(annotation);
                        }
                    }
                }
            } catch (ContentNotFoundException e) {
                logger.trace("No alto file found: {}", page.getAltoFileName());
            } catch (PresentationException | IOException | JDOMException e) {
                logger.error("Error loading alto text from {}", page.getAltoFileName(), e);
            }

        } else if (StringUtils.isNotBlank(page.getFulltextFileName())) {
            String fulltextFilename = Paths.get(page.getFulltextFileName()).getFileName().toString();
            try {
                String fulltext = builder.getFulltext(page.getPi(), fulltextFilename);
                if (StringUtils.isNotBlank(fulltext)) {
                    WebAnnotation anno = new WebAnnotation(UriBuilder.fromUri(annoPageUri).path("plain").build());
                    anno.setMotivation(Motivation.SUPPLEMENTING);
                    TextualResource body = new TextualResource(fulltext);
                    anno.setBody(body);
                    anno.setTarget(new SimpleResource(canvas.getId()));
                    annoPage.addItem(anno);
                }
            } catch (ContentNotFoundException | PresentationException | IndexUnreachableException e) {
                logger.error("Error loading plaintext from {}", page.getFulltextFileName(), e);
            }
        }
        return annoPage;
    }

    /**
     * @param canvas canvas to attach the image resource to
     * @param page physical page element providing image file and dimension data
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws URISyntaxException
     * @throws ContentLibException
     * @throws DAOException
     */
    private void addImageResource(Canvas3 canvas, PhysicalElement page)
            throws ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException, DAOException {
        if (page.getImageWidth() > 0 && page.getImageHeight() > 0) {
            canvas.setWidth(page.getImageWidth());
            canvas.setHeight(page.getImageHeight());
        } else {
            // Use pre-fetched dimension cache before falling back to per-page disk I/O.
            // The cache is populated by preparePageDimensions() before the manifest page loop.
            Dimension cached = pageDimensions.get(page.getOrder());
            if (cached != null) {
                canvas.setWidth(cached.width);
                canvas.setHeight(cached.height);
            } else {
                try {
                    ImageInformation info = images.getImageInformation(page);
                    canvas.setWidth(info.getWidth());
                    canvas.setHeight(info.getHeight());
                } catch (ContentLibException e) {
                    logger.warn("Cannot set canvas size", e);
                }
            }
        }

        if (page.getMediaType().isAllowsImageView() && StringUtils.isNotBlank(page.getFileName())) {
            String filename = page.getFileName();
            URI mediaId = imageUrlManager.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_MEDIA).params(page.getPi(), page.getOrder()).buildURI();
            if (ImageHandler.isExternalUrl(filename)) {
                String imageId = ImageHandler.getIIIFBaseUrl(filename);
                ImageResource imageResource = new ImageResource(imageId, thumbWidth, thumbHeight);
                canvas.addMedia(mediaId, imageResource);
            } else {
                String escFilename = StringTools.encodeUrl(filename);
                String imageId = imageUrlManager.path(ApiUrls.RECORDS_FILES_IMAGE).params(page.getPi(), escFilename).build();
                ImageResource imageResource = new ImageResource(imageId, thumbWidth, thumbHeight);
                // Use pre-fetched permissions when available; fall back to per-page check for single-canvas builds
                boolean access = pagePermissions.isEmpty() ? page.isAccessPermissionImage()
                        : pagePermissions.isImageGranted(page.getOrder());
                if (!access) {
                    for (ImageInformation ii : imageResource.getServices()) {
                        for (Service service : AuthorizationFlowTools.getAuthServices(page.getPi(), page.getFileName())) {
                            ii.addService(service);
                        }
                    }
                }
                canvas.addMedia(mediaId, imageResource);
            }
        }

    }

    /**
     *
     * @param canvas canvas to attach linking and rendering properties to
     * @param page physical page element providing file names and access state
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    private void addRelatedResources(Canvas3 canvas, PhysicalElement page) throws IndexUnreachableException, DAOException {

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer()) {
            PageType pageType = PageType.viewMetadata;
            if (page.isHasImage() || page.getMimeType().equalsIgnoreCase("video") || page.getMimeType().equalsIgnoreCase("audio")) {
                pageType = PageType.viewImage;
            }
            URI recordURI = UriBuilder.fromPath(urls.getApplicationUrl())
                    .path("{pageType}")
                    .path("{pi}")
                    .path("{pageNo}")
                    .path("/")
                    .build(pageType.getName(), page.getPi(), page.getOrder());
            LinkingProperty homepage = new LinkingProperty(LinkingTarget.VIEWER,
                    createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingViewer()));
            canvas.addHomepage(homepage.getResource(recordURI));
        }

        if (page.isHasImage() && DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF()) {
            URI uri = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_PDF)
                    .params(page.getPi(), StringTools.encodeUrl(page.getFileName()))
                    .buildURI();
            LinkingProperty pdf =
                    new LinkingProperty(LinkingTarget.PDF, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPDF()));
            LabeledResource resource = pdf.getResource(uri);
            // Use pre-fetched permissions when available; fall back to per-page check for single-canvas builds
            if (!(pagePermissions.isEmpty() ? page.isAccessPermissionFulltext()
                    : pagePermissions.isFulltextGranted(page.getOrder()))) {
                resource.addService(AuthorizationFlowTools.getAuthServices("/pdf/" + page.getPi() + "/" + page.getAltoFileName() + "/").get(0));
                logger.trace("Added auth services for PDF.");
            }
            canvas.addRendering(resource);
        }

        if (StringUtils.isNotBlank(page.getAltoFileName()) && DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto()) {
            URI uri = urls.path(RECORDS_FILES, RECORDS_FILES_ALTO).params(page.getPi(), getFilename(page.getAltoFileName())).buildURI();
            LinkingProperty alto =
                    new LinkingProperty(LinkingTarget.ALTO, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingAlto()));
            LabeledResource resource = alto.getResource(uri);
            // Use pre-fetched permissions when available; fall back to per-page check for single-canvas builds
            if (!(pagePermissions.isEmpty() ? page.isAccessPermissionFulltext()
                    : pagePermissions.isFulltextGranted(page.getOrder()))) {
                // Add auth services
                for (Service service : AuthorizationFlowTools.getAuthServices(page.getPi(), page.getAltoFileName())) {
                    resource.addService(service);
                }
            }
            canvas.addSeeAlso(resource);
        }

        if (StringUtils.isNotBlank(page.getFulltextFileName())
                && DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext()) {
            URI uri = urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT).params(page.getPi(), getFilename(page.getFulltextFileName())).buildURI();
            LinkingProperty text = new LinkingProperty(LinkingTarget.PLAINTEXT,
                    createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPlaintext()));
            canvas.addRendering(text.getResource(uri));
        } else if (StringUtils.isNotBlank(page.getAltoFileName())
                && DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext()) {
            URI uri = urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT).params(page.getPi(), getFilename(page.getAltoFileName())).buildURI();
            LinkingProperty text = new LinkingProperty(LinkingTarget.PLAINTEXT,
                    createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPlaintext()));
            LabeledResource resource = text.getResource(uri);
            // Use pre-fetched permissions when available; fall back to per-page check for single-canvas builds
            if (!(pagePermissions.isEmpty() ? page.isAccessPermissionFulltext()
                    : pagePermissions.isFulltextGranted(page.getOrder()))) {
                // Add auth services
                for (Service service : AuthorizationFlowTools.getAuthServices(page.getPi(), page.getAltoFileName())) {
                    resource.addService(service);
                }
            }
            canvas.addRendering(resource);
        }
    }
}
