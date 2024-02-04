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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PLAINTEXT;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.oa.FragmentSelector;
import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.oa.SpecificResource;
import de.intranda.api.annotation.oa.TextualResource;
import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.enums.DcType;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.api.iiif.presentation.v2.Sequence;
import de.intranda.digiverso.ocr.alto.model.structureclasses.logical.AltoDocument;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.resourcebuilders.TextResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.AltoAnnotationBuilder;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.viewer.BaseMimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;

/**
 * <p>
 * SequenceBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class SequenceBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(SequenceBuilder.class);

    protected ImageDeliveryBean imageDelivery = BeanUtils.getImageDeliveryBean();
    private BuildMode buildMode = BuildMode.IIIF;
    private PageType preferedView = PageType.viewObject;

    /**
     * <p>
     * Constructor for SequenceBuilder.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public SequenceBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
    }

    /**
     * Creates a sequence from all pages within the given doc and appends it to manifest
     *
     * @param manifest The manifest to include the sequence. May be null
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param manifestId a {@link java.lang.String} object.
     * @return a {@link java.util.Map} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<AnnotationType, List<AnnotationList>> addBaseSequence(Manifest2 manifest, StructElement doc, String manifestId,
            HttpServletRequest request)
            throws URISyntaxException, PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {

        Map<AnnotationType, List<AnnotationList>> annotationMap = new HashMap<>();

        Sequence sequence = new Sequence(getSequenceURI(doc.getPi(), null));

        sequence.addWithin(manifest);

        if (manifest != null) {
            manifest.setSequence(sequence);
        }

        if (BuildMode.IIIF.equals(buildMode) || BuildMode.THUMBS.equals(buildMode)) {

            IPageLoader pageLoader = AbstractPageLoader.create(doc);
            Map<Integer, Canvas2> canvasMap = new HashMap<>();
            for (int i = pageLoader.getFirstPageOrder(); i <= pageLoader.getLastPageOrder(); ++i) {
                PhysicalElement page = pageLoader.getPage(i);

                Canvas2 canvas = generateCanvas(doc.getPi(), page);
                if (canvas != null && getBuildMode().equals(BuildMode.IIIF)) {
                    addSeeAlsos(canvas, page);
                    Map<AnnotationType, AnnotationList> content = addOtherContent(doc, page, canvas, false);

                    merge(annotationMap, content);
                    canvasMap.put(i, canvas);
                }
                if (canvas != null) {
                    sequence.addCanvas(canvas);
                }
            }

            if (getBuildMode().equals(BuildMode.IIIF)) {
                try {
                    annotationMap.put(AnnotationType.COMMENT, addComments(canvasMap, doc.getPi(), false));
                } catch (DAOException e) {
                    logger.error(e.toString());
                }
                if (sequence.getCanvases() != null) {
                    OpenAnnotationBuilder annoBuilder = new OpenAnnotationBuilder(urls);
                    addCrowdourcingAnnotations(sequence.getCanvases(), annoBuilder.getCrowdsourcingAnnotations(doc.getPi(), false, request),
                            annotationMap);
                }
            }
        }

        return annotationMap;
    }

    /**
     * <p>
     * addSeeAlsos.
     * </p>
     *
     * @param canvas a {@link de.intranda.api.iiif.presentation.v2.Canvas} object.
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void addSeeAlsos(Canvas2 canvas, PhysicalElement page) throws URISyntaxException, ViewerConfigurationException {

        this.getSeeAlsos().forEach(link -> {
            try {
                URI id = getCanvasLinkingPropertyUri(page, link.target);
                if (id != null) {
                    canvas.addSeeAlso(link.getLinkingContent(id));
                }
            } catch (URISyntaxException e) {
                logger.error("Error building linking property url", e);
            }
        });
    }

    /**
     * @param page
     * @param canvas
     * @throws URISyntaxException
     */
    public void addRenderings(PhysicalElement page, Canvas2 canvas) throws URISyntaxException {

        this.getRenderings().forEach(link -> {
            try {
                URI id = getCanvasLinkingPropertyUri(page, link.target);
                if (id != null) {
                    canvas.addRendering(link.getLinkingContent(id));
                }
            } catch (URISyntaxException e) {
                logger.error("Error building linking property url", e);
            }
        });
    }

    /**
     * Adds a comment annotation to all cavases which contain comments
     *
     * @param canvases All canvases which may get comments, mapped by their page order
     * @param pi The pi of the work containing the pages
     * @param populate if true, the actual annotations will be included in the resources property
     * @return a map with the list of all annotationlists (one list per page)
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<AnnotationList> addComments(Map<Integer, Canvas2> canvases, String pi, boolean populate)
            throws DAOException, URISyntaxException, ViewerConfigurationException {
        List<AnnotationList> list = new ArrayList<>();
        List<Integer> pages = DataManager.getInstance().getDao().getPagesWithComments(pi);
        for (Integer order : pages) {
            Canvas2 canvas = canvases.get(order);
            if (canvas != null) {
                AnnotationList annoList = new AnnotationList(getAnnotationListURI(pi, order, AnnotationType.COMMENT, true));
                annoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.COMMENT.name()));
                if (populate) {
                    List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage(pi, order);
                    for (Comment comment : comments) {
                        OpenAnnotation anno = new OpenAnnotation(getCommentAnnotationURI(comment.getId()));
                        anno.setMotivation(Motivation.COMMENTING);
                        anno.setTarget(createSpecificResource(canvas, 0, 0, canvas.getWidth(), canvas.getHeight()));
                        TextualResource body = new TextualResource(comment.getContentString());
                        anno.setBody(body);
                        annoList.addResource(anno);
                    }
                }
                canvas.addOtherContent(annoList);
                list.add(annoList);
            }
        }
        return list;
    }

    /**
     * @param canvas
     * @param i
     * @param j
     * @param width
     * @param height
     * @return
     */
    private static SpecificResource createSpecificResource(Canvas2 canvas, int x, int y, int width, int height) {
        SpecificResource part = new SpecificResource(canvas.getId(), new FragmentSelector(new Rectangle(x, y, width, height)));
        return part;
    }

    /**
     * <p>
     * merge.
     * </p>
     *
     * @param annotationMap a {@link java.util.Map} object.
     * @param content a {@link java.util.Map} object.
     */
    public void merge(Map<AnnotationType, List<AnnotationList>> annotationMap, Map<AnnotationType, AnnotationList> content) {
        for (AnnotationType type : content.keySet()) {
            List<AnnotationList> list = annotationMap.get(type);
            if (list == null) {
                list = new ArrayList<>();
                annotationMap.put(type, list);
            }
            list.add(content.get(type));
        }
    }

    /**
     * <p>
     * getPage.
     * </p>
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param order a int.
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws PresentationException
     */
    public PhysicalElement getPage(StructElement doc, int order) throws IndexUnreachableException, DAOException, PresentationException {
        IPageLoader loader = AbstractPageLoader.create(doc); // new LeanPageLoader(doc, 1);
        return loader.getPage(order);
    }

    /**
     * <p>
     * generateCanvas.
     * </p>
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return a {@link de.intranda.api.iiif.presentation.v2.Canvas} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public Canvas2 generateCanvas(String pi, PhysicalElement page)
            throws URISyntaxException, ViewerConfigurationException, IndexUnreachableException, PresentationException {
        if (pi == null || page == null) {
            return null;
        }
        URI canvasId = getCanvasURI(pi, page.getOrder());
        Canvas2 canvas = new Canvas2(canvasId);
        canvas.setLabel(new SimpleMetadataValue(page.getOrderLabel()));
        canvas.addThumbnail(new ImageContent(new URI(imageDelivery.getThumbs().getThumbnailUrl(page))));

        Sequence parent = new Sequence(getSequenceURI(pi, null));
        canvas.addWithin(parent);

        addRenderings(page, canvas);

        if (!getBuildMode().equals(BuildMode.THUMBS)) {
            Dimension size = getSize(page);
            if (size.getWidth() * size.getHeight() > 0) {
                canvas.setWidth(size.width);
                canvas.setHeight(size.height);
            }

            if (page.getMimeType().toLowerCase().startsWith("image") && StringUtils.isNotBlank(page.getFilepath())) {

                String thumbnailUrl = page.getThumbnailUrl();
                ImageContent resource = new ImageContent(new URI(thumbnailUrl));
                if (size.getWidth() * size.getHeight() > 0) {
                    resource.setWidth(size.width);
                    resource.setHeight(size.height);
                    if (IIIFUrlResolver.isIIIFImageUrl(thumbnailUrl)) {
                        URI imageInfoURI = new URI(IIIFUrlResolver.getIIIFImageBaseUrl(thumbnailUrl));
                        resource.setService(new ImageInformation(imageInfoURI.toString()));
                    }
                } else {
                    try {
                        ImageInformation imageInfo = imageDelivery.getImages().getImageInformation(page);
                        resource.setService(imageInfo);
                    } catch (NoClassDefFoundError | ContentLibException e) {
                        logger.error("Error reading image information from {}: {}", thumbnailUrl, e.toString());
                        resource.setWidth(size.width);
                        resource.setHeight(size.height);
                        if (IIIFUrlResolver.isIIIFImageUrl(thumbnailUrl)) {
                            URI imageInfoURI = new URI(IIIFUrlResolver.getIIIFImageBaseUrl(thumbnailUrl));
                            resource.setService(new ImageInformation(imageInfoURI.toString()));
                        }
                    }
                }
                resource.setFormat(Format.fromMimeType(page.getDisplayMimeType()));

                OpenAnnotation imageAnnotation = new OpenAnnotation(getImageAnnotationURI(page.getPi(), page.getOrder()));
                imageAnnotation.setMotivation(Motivation.PAINTING);
                imageAnnotation.setTarget(new SimpleResource(canvas.getId()));
                imageAnnotation.setBody(resource);
                canvas.addImage(imageAnnotation);
            }

        }
        return canvas;
    }

    /**
     * @param page
     * @param doc
     * @param link
     * @throws URISyntaxException
     */
    private URI getCanvasLinkingPropertyUri(PhysicalElement page, LinkingProperty.LinkingTarget target) throws URISyntaxException {
        if (target.equals(LinkingTarget.PLAINTEXT) && StringUtils.isAllBlank(page.getFulltextFileName(), page.getAltoFileName())) {
            return null;
        }
        if (target.equals(LinkingTarget.ALTO) && StringUtils.isBlank(page.getAltoFileName())) {
            return null;
        }
        if (target.equals(LinkingTarget.PDF) && !(BaseMimeType.IMAGE.getName().equals(page.getBaseMimeType()))) {
            return null;
        }

        URI uri = null;
        switch (target) {
            case VIEWER:
                uri = URI.create(getViewUrl(page, getPreferedView()));
                break;
            case ALTO:
                uri = this.urls.path(RECORDS_FILES, RECORDS_FILES_ALTO).params(page.getPi(), page.getFileName("xml")).buildURI();
                break;
            case PLAINTEXT:
                uri = this.urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT).params(page.getPi(), page.getFileName("txt")).buildURI();
                break;
            case PDF:
                uri = URI.create(imageDelivery.getPdf().getPdfUrl(null, page));
        }
        return uri;
    }

    /**
     * <p>
     * addOtherContent.
     * </p>
     *
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param canvas a {@link de.intranda.api.iiif.presentation.v2.Canvas} object.
     * @param populate a boolean.
     * @return a {@link java.util.Map} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<AnnotationType, AnnotationList> addOtherContent(StructElement doc, PhysicalElement page, Canvas2 canvas, boolean populate)
            throws URISyntaxException, IndexUnreachableException, ViewerConfigurationException {

        Map<AnnotationType, AnnotationList> annotationMap = new HashMap<>();
        TextResourceBuilder builder = new TextResourceBuilder();

        if (StringUtils.isNotBlank(page.getFulltextFileName()) || StringUtils.isNotBlank(page.getAltoFileName())) {
            AnnotationList annoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.FULLTEXT, true));
            annoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.FULLTEXT.name()));
            annotationMap.put(AnnotationType.FULLTEXT, annoList);
            if (populate) {
                if (StringUtils.isNotBlank(page.getAltoFileName())) {
                    try {
                        String altoFilename = Paths.get(page.getAltoFileName()).getFileName().toString();
                        StringPair altoPair = builder.getAltoDocument(doc.getPi(), altoFilename);
                        AltoDocument alto = AltoDocument.getDocumentFromString(altoPair.getOne(), altoPair.getTwo());
                        if (alto.getFirstPage() != null && StringUtils.isNotBlank(alto.getFirstPage().getContent())) {
                            List<AbstractAnnotation> annos =
                                    new AltoAnnotationBuilder(urls, "oa").createAnnotations(alto.getFirstPage(), doc.getPi(), page.getOrder(), canvas,
                                            AltoAnnotationBuilder.Granularity.LINE, false);
                            for (AbstractAnnotation annotation : annos) {
                                annoList.addResource(annotation);
                            }
                        }
                    } catch (ContentNotFoundException e) {
                        logger.trace("No alto file found: " + page.getAltoFileName());
                    } catch (PresentationException | IOException | JDOMException e) {
                        logger.error("Error loading alto text from " + page.getAltoFileName(), e);
                    }

                } else if (StringUtils.isNotBlank(page.getFulltextFileName())) {
                    try {
                        OpenAnnotation anno = new OpenAnnotation(URI.create(annoList.getId().toString() + "/text"));
                        anno.setMotivation(Motivation.PAINTING);
                        anno.setTarget(createSpecificResource(canvas, 0, 0, canvas.getWidth(), canvas.getHeight()));
                        String fulltextFilename = Paths.get(page.getFulltextFileName()).getFileName().toString();
                        String fulltext = builder.getFulltext(doc.getPi(), fulltextFilename);
                        TextualResource body = new TextualResource(fulltext);
                        anno.setBody(body);
                        annoList.addResource(anno);
                    } catch (ContentNotFoundException | PresentationException e) {
                        logger.error("Error loading fulltext from " + page.getFulltextFileName(), e);

                    }
                }
            }
        }

        if (BaseMimeType.AUDIO.getName().equals(page.getMimeType())) {
            AnnotationList annoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.AUDIO, true));
            annoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.AUDIO.name()));
            OpenAnnotation annotation = new OpenAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.AUDIO, 1));
            annotation.setMotivation(Motivation.PAINTING);
            annotation.setTarget(canvas);
            annoList.addResource(annotation);
            annotationMap.put(AnnotationType.AUDIO, annoList);
            if (populate) {
                String url = page.getMediaUrl(page.getFileNames().keySet().stream().findFirst().orElse(""));
                Format format = Format.fromFilename(url);
                LinkingContent audioLink = new LinkingContent(new URI(url));
                audioLink.setFormat(format);
                audioLink.setType(DcType.SOUND.getLabel());
                audioLink.setLabel(ViewerResourceBundle.getTranslations("AUDIO"));
                annotation.setBody(audioLink);
            }

        }

        AnnotationList videoList = new AnnotationList(getAnnotationListURI(page.getPi(), page.getOrder(), AnnotationType.VIDEO, true));
        videoList.setLabel(ViewerResourceBundle.getTranslations(AnnotationType.VIDEO.name()));
        if (BaseMimeType.VIDEO.getName().equals(page.getMimeType())) {
            OpenAnnotation annotation = new OpenAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.VIDEO, 1));
            annotation.setMotivation(Motivation.PAINTING);
            annotation.setTarget(canvas);
            videoList.addResource(annotation);
            if (populate) {
                String url = page.getMediaUrl(page.getFileNames().keySet().stream().findFirst().orElse(""));
                Format format = Format.fromFilename(url);
                LinkingContent link = new LinkingContent(new URI(url));
                link.setFormat(format);
                link.setType(DcType.MOVING_IMAGE.getLabel());
                link.setLabel(ViewerResourceBundle.getTranslations("VIDEO"));
                annotation.setBody(link);
            }

        }
        if (BaseMimeType.SANDBOXED_HTML.getName().equals(page.getMimeType())) {
            try {
                OpenAnnotation annotation = new OpenAnnotation(getAnnotationURI(page.getPi(), page.getOrder(), AnnotationType.VIDEO, 1));
                annotation.setMotivation(Motivation.PAINTING);
                annotation.setTarget(canvas);
                videoList.addResource(annotation);
                if (populate) {
                    String url = page.getUrl();
                    if (url.startsWith("//")) {
                        url = "http:" + url;
                    }

                    LinkingContent link = new LinkingContent(new URI(url));
                    link.setFormat(Format.TEXT_HTML);
                    link.setType(DcType.MOVING_IMAGE.getLabel());
                    link.setLabel(ViewerResourceBundle.getTranslations("VIDEO"));
                    annotation.setBody(link);
                }
            } catch (ViewerConfigurationException e) {
                logger.error(e.toString(), e);
            }
        }
        if (videoList.getResources() != null) {
            annotationMap.put(AnnotationType.VIDEO, videoList);
        }

        //        addCrowdsourcingAnnotations(page, populate, annotationMap);

        for (AnnotationType type : annotationMap.keySet()) {
            canvas.addOtherContent(annotationMap.get(type));
        }
        return annotationMap;
    }

    /**
     * @param page
     * @return
     * @throws ViewerConfigurationException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private Dimension getSize(PhysicalElement page) throws ViewerConfigurationException, PresentationException, IndexUnreachableException {
        Dimension size = new Dimension(0, 0);
        if (page.getMimeType().toLowerCase().startsWith("video") || page.getMimeType().toLowerCase().startsWith("text")) {
            size.setSize(page.getVideoWidth(), page.getVideoHeight());
        } else if (page.getMimeType().toLowerCase().startsWith("image")) {
            if (page.hasIndividualSize()) {
                size.setSize(page.getImageWidth(), page.getImageHeight());
            } else {
                try {
                    ImageInformation info = imageDelivery.getImages().getImageInformation(page);
                    size.setSize(info.getWidth(), info.getHeight());
                    /*
                     * Catch NoClassDefFoundError which occurs if imageio-libs are missing.
                     * Currently this is true in a testing environment, so we just catch it here to pass the tests
                     */
                } catch (NoClassDefFoundError | ContentLibException | URISyntaxException e) {
                    logger.error("Unable to retrieve image size for {}: {}", page, e.toString());
                }
            }
        }
        return size;
    }

    /**
     * <p>
     * Getter for the field <code>buildMode</code>.
     * </p>
     *
     * @return the buildMode
     */
    public BuildMode getBuildMode() {
        return buildMode;
    }

    /**
     * <p>
     * Setter for the field <code>buildMode</code>.
     * </p>
     *
     * @param buildMode the buildMode to set
     * @return a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.SequenceBuilder} object.
     */
    public SequenceBuilder setBuildMode(BuildMode buildMode) {
        this.buildMode = buildMode;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>preferredView</code>.
     * </p>
     *
     * @return the preferredView
     */
    public PageType getPreferedView() {
        return preferedView;
    }

    /**
     * <p>
     * Setter for the field <code>preferredView</code>.
     * </p>
     *
     * @param preferredView the preferredView to set
     * @return a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.SequenceBuilder} object.
     */
    public SequenceBuilder setPreferedView(PageType preferredView) {
        this.preferedView = preferredView;
        return this;
    }

}
