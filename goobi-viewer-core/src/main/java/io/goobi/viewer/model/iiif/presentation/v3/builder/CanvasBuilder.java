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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.wa.ImageResource;
import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ImageHandler;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v3.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;

/**
 * @author florian
 *
 */
public class CanvasBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(ManifestBuilder.class);

    private final ImageHandler images;
    private final AbstractApiUrlManager imageUrlManager = DataManager.getInstance().getRestApiManager().getIIIFContentApiManager();

    /**
     * @param apiUrlManager
     */
    public CanvasBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
        this.images = new ImageHandler(urls);
    }

    public Canvas3 build(String pi, int order)
            throws PresentationException, IndexUnreachableException, IllegalPathSyntaxException, ContentLibException, URISyntaxException {
        StructElement topStruct = this.dataRetriever.getDocument(pi);
        PhysicalElement page = AbstractPageLoader.loadPage(topStruct, order);
        if (page != null) {
            return build(page);
        }

        throw new ContentNotFoundException(String.format("Not canvas found at order %d in record %s", order, pi));
    }

    public AnnotationPage buildFulltextAnnotations(String pi, int order)
            throws IllegalPathSyntaxException, ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException {
        StructElement topStruct = this.dataRetriever.getDocument(pi);
        PhysicalElement page = AbstractPageLoader.loadPage(topStruct, order);
        Canvas3 canvas = build(page);
        AnnotationPage fulltext = getFulltextAnnotations(canvas, page);
        return fulltext;
    }

    public Canvas3 build(PhysicalElement page)
            throws IllegalPathSyntaxException, ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException {
        URI canvasUri = this.urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_CANVAS).params(page.getPi(), page.getOrder()).buildURI();
        Canvas3 canvas = new Canvas3(canvasUri);
        canvas.setLabel(new SimpleMetadataValue(page.getOrderLabel()));

        if (page.getMimeType().matches("(?i)image(\\/.*)?")) {
            addImageResource(canvas, page);
        } else {
            //not implemented
        }

        canvas.addAnnotations(getFulltextAnnotationsReference(page));
        canvas.addAnnotations(getCommentAnnotationsReference(page));
        canvas.addAnnotations(getCrowdsourcingAnnotationsReference(page));

        addRelatedResources(canvas, page);

        return canvas;

    }

    /**
     * @param page
     * @return
     */
    private AnnotationPage getCommentAnnotationsReference(PhysicalElement page) {
        URI annoPageUri = this.urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_COMMENTS).params(page.getPi(), page.getOrder()).buildURI();
        AnnotationPage annoPage = new AnnotationPage(annoPageUri, false);
        return annoPage;
    }

    private AnnotationPage getCrowdsourcingAnnotationsReference(PhysicalElement page) {
        URI annoPageUri = this.urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_ANNOTATIONS).params(page.getPi(), page.getOrder()).buildURI();
        AnnotationPage annoPage = new AnnotationPage(annoPageUri, false);
        return annoPage;
    }

    /**
     * Get a reference to an annotation page containing all fulltext annotations for the given page
     *
     * @param page
     * @return The annotation page, or null if no fulltext is available for the given page
     */
    private AnnotationPage getFulltextAnnotationsReference(PhysicalElement page) {
        if (page.isFulltextAvailable()) {
            URI annoPageUri = this.urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_TEXT).params(page.getPi(), page.getOrder()).buildURI();
            AnnotationPage annoPage = new AnnotationPage(annoPageUri, false);
            return annoPage;
        }

        return null;
    }

    /**
     * Return an annotation page with any fulltext annotations for the given page. The annotation page contains the @context property
     *
     * @param canvas
     * @param page
     * @return
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
                logger.trace("No alto file found: " + page.getAltoFileName());
            } catch (PresentationException | IOException | JDOMException e) {
                logger.error("Error loading alto text from " + page.getAltoFileName(), e);
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
                logger.error("Error loading plaintext from " + page.getFulltextFileName(), e);
            }
        }
        return annoPage;
    }

    /**
     * @param canvas
     * @param page
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws URISyntaxException
     * @throws ContentLibException
     * @throws IllegalPathSyntaxException
     */
    private void addImageResource(Canvas3 canvas, PhysicalElement page)
            throws IllegalPathSyntaxException, ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException {
        if (page.getImageWidth() > 0 && page.getImageHeight() > 0) {
            canvas.setWidth(page.getImageWidth());
            canvas.setHeight(page.getImageHeight());
        } else {
            try {
                ImageInformation info = images.getImageInformation(page);
                canvas.setWidth(info.getWidth());
                canvas.setHeight(info.getHeight());
            } catch (ContentLibException e) {
                logger.warn("Cannot set canvas size", e);
            }
        }

        if (page.isHasImage()) {
            String filename = page.getFileName();
            String escFilename = StringTools.encodeUrl(filename);
            String imageId = imageUrlManager.path(ApiUrls.RECORDS_FILES_IMAGE).params(page.getPi(), escFilename).build();
            URI mediaId = imageUrlManager.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_MEDIA).params(page.getPi(), page.getOrder()).buildURI();
            canvas.addMedia(mediaId, new ImageResource(imageId, thumbWidth, thumbHeight));
        }

    }

    private void addRelatedResources(Canvas3 canvas, PhysicalElement page) {

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
            canvas.addRendering(pdf.getResource(uri));
        }

        if (StringUtils.isNotBlank(page.getAltoFileName()) && DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto()) {
            URI uri = urls.path(RECORDS_FILES, RECORDS_FILES_ALTO).params(page.getPi(), getFilename(page.getAltoFileName())).buildURI();
            LinkingProperty alto =
                    new LinkingProperty(LinkingTarget.ALTO, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingAlto()));
            canvas.addSeeAlso(alto.getResource(uri));
        }

        if (page.isFulltextAvailable() && StringUtils.isNotBlank(page.getFulltextFileName())
                && DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext()) {
            URI uri = urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT).params(page.getPi(), getFilename(page.getFulltextFileName())).buildURI();
            LinkingProperty text = new LinkingProperty(LinkingTarget.PLAINTEXT,
                    createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPlaintext()));
            canvas.addRendering(text.getResource(uri));
        }

    }

}
