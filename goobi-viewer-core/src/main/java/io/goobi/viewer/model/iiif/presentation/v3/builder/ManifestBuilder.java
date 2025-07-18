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

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_ALTO;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_IMAGE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_IMAGE_PDF;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PDF;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PLAINTEXT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_RECORD;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v3.AbstractPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.IPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.LabeledResource;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import de.intranda.api.iiif.presentation.v3.Range3;
import de.intranda.api.iiif.search.AutoSuggestService;
import de.intranda.api.iiif.search.SearchService;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v2.auth.AuthorizationFlowTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.model.ManifestLinkConfiguration;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.iiif.presentation.v3.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.variables.VariableReplacer;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;

/**
 * <p>
 * ManifestBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class ManifestBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(ManifestBuilder.class);

    private final CanvasBuilder canvasBuilder;

    /**
     * <p>
     * Constructor for ManifestBuilder.
     * </p>
     *
     * @param apiUrlManager
     */
    public ManifestBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);
        this.canvasBuilder = new CanvasBuilder(urls);

    }

    /**
     * 
     * @param pi
     * @param request
     * @return {@link IPresentationModelElement3}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentLibException
     * @throws URISyntaxException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public IPresentationModelElement3 build(String pi, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, ContentLibException, URISyntaxException, DAOException {

        List<StructElement> documents = this.dataRetriever.getDocumentWithChildren(pi);

        StructElement mainDocument = documents.get(0);
        List<StructElement> childDocuments = documents.subList(1, documents.size());

        AbstractPresentationModelElement3 manifest = generateManifest(mainDocument, Optional.empty());

        if (manifest instanceof Manifest3 manifest3) {
            addPages(mainDocument, manifest3);
            addStructures(mainDocument, childDocuments, manifest3);
            addAnnotations(mainDocument.getPi(), (Manifest3) manifest, request);
        } else if (manifest instanceof Collection3 col3) {
            addVolumes(childDocuments, col3);
        }

        return manifest;
    }

    public IPresentationModelElement build(String pi, Integer pageNo, HttpServletRequest servletRequest) throws PresentationException,
            IndexUnreachableException, ContentLibException, URISyntaxException, DAOException {
        StructElement mainDocument = this.dataRetriever.getDocument(pi);

        AbstractPresentationModelElement3 manifest = generateManifest(mainDocument, Optional.ofNullable(pageNo));

        if (manifest instanceof Manifest3 manifest3) {
            addPage(manifest3, mainDocument, pageNo);
            addAnnotations(mainDocument.getPi(), pageNo, manifest3, servletRequest);
        } else if (manifest instanceof Collection3) {
            throw new IllegalRequestException("Cannot build a page manifest: PI refers to an anchor record without pages");
        }

        return manifest;
    }

    /**
     * 
     * @param pi
     * @param manifest
     * @param request
     */
    private void addAnnotations(String pi, Manifest3 manifest, HttpServletRequest request) {
        try {
            ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi);
            URI uri = URI.create(apiPath.build());
            AnnotationPage crowdAnnos = new AnnotationsResourceBuilder(urls, request).getWebAnnotationCollectionForRecord(pi, uri).getFirst();
            if (crowdAnnos != null && !crowdAnnos.getItems().isEmpty()) {
                manifest.addAnnotations(new InternalAnnotationPage(crowdAnnos));
            }
            AnnotationPage comments = new AnnotationsResourceBuilder(urls, request).getWebAnnotationCollectionForRecordComments(pi, uri).getFirst();
            if (comments != null && !comments.getItems().isEmpty()) {
                manifest.addAnnotations(new InternalAnnotationPage(comments));
            }
        } catch (DAOException e) {
            logger.error("Error adding annotations to manifest: {}", e.toString(), e);
        }
    }

    /**
     * 
     * @param pi
     * @param pageNo
     * @param manifest
     * @param request
     */
    private void addAnnotations(String pi, int pageNo, Manifest3 manifest, HttpServletRequest request) {
        try {
            ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi);
            URI uri = URI.create(apiPath.build());
            AnnotationPage crowdAnnos = new AnnotationsResourceBuilder(urls, request).getWebAnnotationCollectionForPage(pi, pageNo, uri).getFirst();
            if (crowdAnnos != null && !crowdAnnos.getItems().isEmpty()) {
                manifest.addAnnotations(new InternalAnnotationPage(crowdAnnos));
            }
            AnnotationPage comments =
                    new AnnotationsResourceBuilder(urls, request).getWebAnnotationCollectionForPageComments(pi, pageNo, uri).getFirst();
            if (comments != null && !comments.getItems().isEmpty()) {
                manifest.addAnnotations(new InternalAnnotationPage(comments));
            }
        } catch (DAOException e) {
            logger.error("Error adding annotations to manifest: {}", e.toString(), e);
        }
    }

    /**
     * @param childDocuments
     * @param manifest
     */
    private void addVolumes(List<StructElement> childDocuments, Collection3 manifest) {
        for (StructElement volume : childDocuments) {
            try {
                IPresentationModelElement child = generateManifest(volume, Optional.empty());
                if (child instanceof Manifest3 manifest3) {
                    manifest.addItem(manifest3);
                }
            } catch (PresentationException e) {
                logger.error("Error creating child manigest for {}", volume);
            }
        }
    }

    /**
     * @param mainDocument
     * @param childDocuments
     * @param manifest
     * @throws ContentNotFoundException
     * @throws PresentationException
     */
    private void addStructures(StructElement mainDocument, List<StructElement> childDocuments, Manifest3 manifest)
            throws ContentNotFoundException, PresentationException {
        RangeBuilder rangeBuilder = new RangeBuilder(urls);
        Range3 topRange = rangeBuilder.build(mainDocument, childDocuments, null);
        topRange.getItems()
                .stream()
                .filter(Range3.class::isInstance)
                .map(Range3.class::cast)
                .forEach(manifest::addRange);
    }

    /**
     * <p>
     * generateManifest.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link de.intranda.api.iiif.presentation.IPresentationModelElement} object.
     * @throws PresentationException
     */
    private AbstractPresentationModelElement3 generateManifest(StructElement ele, Optional<Integer> pageNo) throws PresentationException {

        final AbstractPresentationModelElement3 manifest;

        if (ele.isAnchor()) {
            manifest = new Collection3(getManifestURI(ele.getPi()), ele.getPi());
            manifest.addBehavior(ViewingHint.multipart);
        } else {
            ele.setImageNumber(1);
            manifest = new Manifest3(getManifestURI(ele.getPi()));
            SearchService search = new SearchService(this.getSearchServiceURI(ele.getPi()));
            AutoSuggestService autoComplete = new AutoSuggestService(this.getAutoCompleteServiceURI(ele.getPi()));
            search.addService(autoComplete);
            manifest.addService(search);
        }

        populateData(ele, manifest, pageNo);

        return manifest;
    }

    /**
     * @param ele
     * @param manifest
     * @throws DAOException
     * @throws URISyntaxException
     * @throws ContentLibException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private void addPages(StructElement ele, Manifest3 manifest) throws PresentationException, IndexUnreachableException,
            ContentLibException, URISyntaxException, DAOException {

        IPageLoader pageLoader = AbstractPageLoader.create(ele);
        for (int order = pageLoader.getFirstPageOrder(); order <= pageLoader.getLastPageOrder(); order++) {
            PhysicalElement page = pageLoader.getPage(order);
            addPage(manifest, page);
        }
    }

    public void addPage(Manifest3 manifest, StructElement ele, int pageNo)
            throws IndexUnreachableException, ContentLibException, URISyntaxException, PresentationException, DAOException {
        IPageLoader pageLoader = AbstractPageLoader.create(ele, List.of(pageNo));
        PhysicalElement page = pageLoader.getPage(pageNo);
        if (page != null) {
            Canvas3 canvas = canvasBuilder.build(page);
            manifest.addItem(canvas);
        }
    }

    public void addPage(Manifest3 manifest, PhysicalElement page)
            throws IndexUnreachableException, ContentLibException, URISyntaxException, PresentationException, DAOException {
        if (page != null) {
            Canvas3 canvas = canvasBuilder.build(page);
            manifest.addItem(canvas);
        }
    }

    /**
     * <p>
     * populate.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param manifest a {@link de.intranda.api.iiif.presentation.AbstractPresentationModelElement} object.
     * @return {@link AbstractPresentationModelElement3}
     * @throws PresentationException
     */
    private AbstractPresentationModelElement3 populateData(StructElement ele, final AbstractPresentationModelElement3 manifest,
            Optional<Integer> pageNo) throws PresentationException {

        VariableReplacer variableReplacer = new VariableReplacer(ele);

        IMetadataValue label = getLabel(ele).orElse(ele.getMultiLanguageDisplayLabel());
        manifest.setLabel(label);
        getDescription(ele).ifPresent(manifest::setDescription);

        manifest.addThumbnail(pageNo.map(p -> getThumbnail(ele, p)).orElse(getThumbnail(ele)));

        de.intranda.metadata.multilanguage.Metadata requiredStatement = variableReplacer.replace(getRequiredStatement());
        if (!requiredStatement.getValue().isEmpty()) {
            manifest.setRequiredStatement(requiredStatement);
        }
        manifest.setRights(getRightsStatement(ele).orElse(null));
        manifest.setNavDate(getNavDate(ele));
        manifest.addBehavior(getViewingBehavior(ele));
        //TODO: add provider from config
        DataManager.getInstance()
                .getConfiguration()
                .getIIIFProvider(variableReplacer)
                .forEach(providerConfig -> manifest.addProvider(getProvider(providerConfig)));

        addMetadata(manifest, ele);

        pageNo
                .map(no -> {
                    try {
                        return DataManager.getInstance().getSearchIndex().getPage(ele.getPi(), no);
                    } catch (IndexUnreachableException | PresentationException | DAOException e) {
                        logger.error("Error retrieving page from solr: {}", e.toString());
                        return null;
                    }
                })
                .ifPresentOrElse(
                        page -> {
                            try {
                                addRelatedResources(manifest, ele, page);
                            } catch (IndexUnreachableException | DAOException e) {
                                logger.error(e.toString());
                            }
                        },
                        () -> addRelatedResources(manifest, ele));

        return manifest;
    }

    /**
     * @param manifest
     * @param ele
     */
    private void addRelatedResources(AbstractPresentationModelElement3 manifest, StructElement ele) {
        logger.trace("addRelatedResources (record)");
        // metadata document
        if (ele.isLidoRecord() && DataManager.getInstance().getConfiguration().isVisibleIIIFSeeAlsoLido()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFSeeAlsoLido());
            LabeledResource resolver =
                    new LabeledResource(getLidoResolverUrl(ele), "Dataset", Format.TEXT_XML.getLabel(), "http://www.lido-schema.org", label);
            manifest.addSeeAlso(resolver);
        } else if (DataManager.getInstance().getConfiguration().isVisibleIIIFSeeAlsoMets()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFSeeAlsoMets());
            LabeledResource resolver =
                    new LabeledResource(getMetsResolverUrl(ele), "Dataset", Format.TEXT_XML.getLabel(), "http://www.loc.gov/METS/", label);
            manifest.addSeeAlso(resolver);
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer()) {
            PageType pageType = PageType.viewMetadata;
            if (ele.isHasImages()) {
                pageType = PageType.viewImage;
            } else if (ele.isAnchor()) {
                pageType = PageType.viewToc;
            }
            URI recordURI = UriBuilder.fromPath(urls.getApplicationUrl()).path("{pageType}").path("{pi}").build(pageType.getName(), ele.getPi());
            LinkingProperty homepage = new LinkingProperty(LinkingTarget.VIEWER,
                    createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingViewer()));
            manifest.addHomepage(homepage.getResource(recordURI));

            getCmsPageLinks(ele.getPi()).forEach(manifest::addHomepage);
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF() && ele.isHasImages()) {
            URI uri = urls.path(RECORDS_RECORD, RECORDS_PDF).params(ele.getPi()).buildURI();
            LinkingProperty pdf =
                    new LinkingProperty(LinkingTarget.PDF, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPDF()));
            manifest.addRendering(pdf.getResource(uri));
        }

        try {
            if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto() && ele.isAltoAvailable()) {
                URI uri = urls.path(RECORDS_RECORD, RECORDS_ALTO).params(ele.getPi()).buildURI();
                LinkingProperty alto =
                        new LinkingProperty(LinkingTarget.ALTO,
                                createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingAlto()));
                manifest.addSeeAlso(alto.getResource(uri));
            }
        } catch (IndexUnreachableException | PresentationException e) {
            logger.error("Unable to check existence of ALTO for {}. Reason: {}", ele.getPi(), e.toString());
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext() && ele.isFulltextAvailable()) {
            URI uri = urls.path(RECORDS_RECORD, RECORDS_PLAINTEXT).params(ele.getPi()).buildURI();
            LinkingProperty text = new LinkingProperty(LinkingTarget.PLAINTEXT,
                    createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPlaintext()));
            manifest.addRendering(text.getResource(uri));
        }

        List<ManifestLinkConfiguration> linkConfigurations = DataManager.getInstance().getConfiguration().getIIIFSeeAlsoMetadataConfigurations();
        for (ManifestLinkConfiguration config : linkConfigurations) {
            try {
                Metadata md = config.getMetadata();
                md.populate(ele, "", null, null);
                String label = config.getLabel();
                String format = config.getFormat();
                String value = md.getCombinedValue(", ");
                if (StringUtils.isNotBlank(value)) {
                    IMetadataValue translations = ViewerResourceBundle.getTranslations(label, false);
                    LabeledResource seeAlso = new LabeledResource(new URI(value), getType(format), format, translations);
                    manifest.addSeeAlso(seeAlso);
                }
            } catch (IndexUnreachableException | PresentationException | URISyntaxException e) {
                logger.error("Unable to create seeAlso link for {}", config.getLabel(), e);
            }
        }

    }

    /**
     * Page manifest resources.
     * 
     * @param manifest
     * @param ele
     * @param page
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private void addRelatedResources(AbstractPresentationModelElement3 manifest, StructElement ele, PhysicalElement page)
            throws IndexUnreachableException, DAOException {
        logger.trace("addRelatedResources (page)");

        // metadata document
        if (ele.isLidoRecord() && DataManager.getInstance().getConfiguration().isVisibleIIIFSeeAlsoLido()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFSeeAlsoLido());
            LabeledResource resolver =
                    new LabeledResource(getLidoResolverUrl(ele), "Dataset", Format.TEXT_XML.getLabel(), "http://www.lido-schema.org", label);
            manifest.addSeeAlso(resolver);
        } else if (DataManager.getInstance().getConfiguration().isVisibleIIIFSeeAlsoMets()) {
            IMetadataValue label = getLabel(DataManager.getInstance().getConfiguration().getLabelIIIFSeeAlsoMets());
            LabeledResource resolver =
                    new LabeledResource(getMetsResolverUrl(ele), "Dataset", Format.TEXT_XML.getLabel(), "http://www.loc.gov/METS/", label);
            manifest.addSeeAlso(resolver);
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer()) {
            PageType pageType = PageType.viewMetadata;
            if (ele.isHasImages()) {
                pageType = PageType.viewImage;
            } else if (ele.isAnchor()) {
                pageType = PageType.viewToc;
            }
            URI pageURI = UriBuilder.fromPath(urls.getApplicationUrl())
                    .path("{pageType}")
                    .path("{pi}")
                    .path("{pageNo}")
                    .build(pageType.getName(), ele.getPi(), page.getOrder());
            LinkingProperty homepage = new LinkingProperty(LinkingTarget.VIEWER,
                    createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingViewer()));
            manifest.addHomepage(homepage.getResource(pageURI));

            getCmsPageLinks(ele.getPi()).forEach(manifest::addHomepage);
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF()) {
            URI uri = urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_PDF).params(ele.getPi(), escapeURI(page.getFileName())).buildURI();
            LinkingProperty pdf =
                    new LinkingProperty(LinkingTarget.PDF, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPDF()));
            LabeledResource resource = pdf.getResource(uri);
            if (!page.isAccessPermissionPdf()) {
                // TODO Find correct PDF file name and add auth services
                // resource.addService(AuthorizationFlowTools.getAuthServices(ele.getPi(), page.getImageToPdfUrl()));
                logger.trace("Added auth services for PDF.");
            }
            manifest.addRendering(resource);
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto() && page.isAltoAvailable()) {
            URI uri = urls.path(RECORDS_FILES, RECORDS_FILES_ALTO)
                    .params(ele.getPi(), Path.of(escapeURI(page.getAltoFileName())).getFileName())
                    .buildURI();
            LinkingProperty alto =
                    new LinkingProperty(LinkingTarget.ALTO, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingAlto()));
            LabeledResource resource = alto.getResource(uri);
            if (!page.isAccessPermissionFulltext()) {
                // Add auth services
                resource.addService(AuthorizationFlowTools.getAuthServices(ele.getPi(), page.getAltoFileName()));
            }
            manifest.addSeeAlso(resource);
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext() && page.isFulltextAvailable()) {
            URI uri = urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT)
                    .params(ele.getPi(),
                            Path.of(Optional.ofNullable(page.getFulltextFileName()).orElse(escapeURI(page.getAltoFileName()))).getFileName())
                    .buildURI();
            LinkingProperty text = new LinkingProperty(LinkingTarget.PLAINTEXT,
                    createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPlaintext()));
            LabeledResource resource = text.getResource(uri);
            if (!page.isAccessPermissionFulltext()) {
                // Add auth services
                resource.addService(AuthorizationFlowTools.getAuthServices(ele.getPi(), page.getAltoFileName()));
            }
            manifest.addRendering(resource);
        }

        List<ManifestLinkConfiguration> linkConfigurations = DataManager.getInstance().getConfiguration().getIIIFSeeAlsoMetadataConfigurations();
        for (ManifestLinkConfiguration config : linkConfigurations) {
            try {
                Metadata md = config.getMetadata();
                md.populate(ele, "", null, null);
                String label = config.getLabel();
                String format = config.getFormat();
                String value = md.getCombinedValue(", ");
                if (StringUtils.isNotBlank(value)) {
                    IMetadataValue translations = ViewerResourceBundle.getTranslations(label, false);
                    LabeledResource seeAlso = new LabeledResource(new URI(value), getType(format), format, translations);
                    manifest.addSeeAlso(seeAlso);
                }
            } catch (IndexUnreachableException | PresentationException | URISyntaxException e) {
                logger.error("Unable to create seeAlso link for {}", config.getLabel(), e);
            }
        }

    }

    /**
     * 
     * @param format
     * @return {@link String}
     */
    private static String getType(String format) {
        if (StringUtils.isBlank(format)) {
            return "";
        }
        String typeString = format.replaceAll("\\/.*", "").toLowerCase();
        switch (typeString) {
            case "image":
                return "Image";
            case "object":
                return "Model";
            case "video":
                return "Video";
            case "audio":
                return "Sound";
            case "text":
                return "Text";
            default:
                return "Dataset";
        }
    }

    /**
     * TODO: config for docStruct types that should be presented as "paged"
     *
     * @param ele
     * @return {@link ViewingHint}
     */
    private static ViewingHint getViewingBehavior(StructElement ele) {
        String docStructType = ele.getDocStructType().toLowerCase();
        switch (docStructType) {
            case "monograph":
            case "volume":
            case "newspaper_volume":
            case "periodical_volume":
                return ViewingHint.paged;
            default:
                return ViewingHint.individuals;
        }
    }

    /**
     * 
     * @param ele
     * @return {@link LocalDateTime}
     */
    private static LocalDateTime getNavDate(StructElement ele) {
        String navDateField = DataManager.getInstance().getConfiguration().getIIIFNavDateField();
        if (StringUtils.isNotBlank(navDateField) && StringUtils.isNotBlank(ele.getMetadataValue(navDateField))) {
            try {
                String eleValue = ele.getMetadataValue(navDateField);
                LocalDate date = LocalDate.parse(eleValue);
                return date.atStartOfDay();
            } catch (NullPointerException | DateTimeParseException e) {
                logger.warn("Unable to parse {} as Date", ele.getMetadataValue(navDateField));
            }
        }
        return null;
    }

    /**
     * 
     * @param pi
     * @return List<LabeledResource>
     */
    private static List<LabeledResource> getCmsPageLinks(String pi) {
        try {
            return DataManager.getInstance()
                    .getDao()
                    .getCMSPagesForRecord(pi, null)
                    .stream()
                    .filter(CMSPage::isPublished)
                    .map(page -> {
                        LinkingProperty cmsPageLink = new LinkingProperty(LinkingTarget.VIEWER, page.getTitleTranslations());
                        URI uri = URI.create(page.getUrl());
                        if (!uri.isAbsolute()) {
                            uri = UriBuilder.fromUri(uri).path(page.getUrl()).build();
                        }
                        return cmsPageLink.getResource(uri);
                    })
                    .toList();
        } catch (DAOException e) {
            logger.warn(e.toString());
            return Collections.emptyList();
        }
    }

}
