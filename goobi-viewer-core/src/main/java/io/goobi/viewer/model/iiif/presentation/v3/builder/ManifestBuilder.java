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
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PDF;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_PLAINTEXT;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_RECORD;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.api.iiif.presentation.v3.AbstractPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.IIIFAgent;
import de.intranda.api.iiif.presentation.v3.IPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.LabeledResource;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import de.intranda.api.iiif.presentation.v3.Range3;
import de.intranda.api.iiif.search.AutoSuggestService;
import de.intranda.api.iiif.search.SearchService;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.model.ManifestLinkConfiguration;
import io.goobi.viewer.controller.model.ProviderConfiguration;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.iiif.presentation.v2.builder.WebAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v3.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;

/**
 * <p>
 * ManifestBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class ManifestBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(ManifestBuilder.class);

    /**
     * <p>
     * Constructor for ManifestBuilder.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public ManifestBuilder(AbstractApiUrlManager apiUrlManager) {
        super(apiUrlManager);

    }

    public IPresentationModelElement3 build(String pi, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException,
            IllegalPathSyntaxException, ContentLibException, URISyntaxException, DAOException {

        List<StructElement> documents = this.dataRetriever.getDocumentWithChildren(pi);

        StructElement mainDocument = documents.get(0);
        List<StructElement> childDocuments = documents.subList(1, documents.size());

        AbstractPresentationModelElement3 manifest = generateManifest(mainDocument);

        if (manifest instanceof Manifest3) {
            addPages(mainDocument, (Manifest3) manifest);
            addStructures(mainDocument, childDocuments, (Manifest3) manifest);
            addAnnotations(mainDocument.getPi(), (Manifest3) manifest, request);
        } else if (manifest instanceof Collection3) {
            addVolumes(mainDocument, childDocuments, (Collection3) manifest);
        }

        return manifest;
    }

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
     * @param mainDocument
     * @param childDocuments
     * @param manifest
     */
    private void addVolumes(StructElement mainDocument, List<StructElement> childDocuments, Collection3 manifest) {
        for (StructElement volume : childDocuments) {
            try {
                IPresentationModelElement child = generateManifest(volume);
                if (child instanceof Manifest3) {
                    //                    addBaseSequence((Manifest)child, volume, child.getId().toString());
                    manifest.addItem((Manifest3) child);
                }
            } catch (ViewerConfigurationException | URISyntaxException | PresentationException | IndexUnreachableException | ContentLibException e) {
                logger.error("Error creating child manigest for " + volume);
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
                .filter(item -> item instanceof Range3)
                .map(item -> (Range3) item)
                .forEach(manifest::addRange);
    }

    /**
     * <p>
     * generateManifest.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link de.intranda.api.iiif.presentation.IPresentationModelElement} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws ContentLibException
     * @throws IllegalPathSyntaxException
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    private AbstractPresentationModelElement3 generateManifest(StructElement ele)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IllegalPathSyntaxException, ContentLibException,
            URISyntaxException {

        final AbstractPresentationModelElement3 manifest;

        if (ele.isAnchor()) {
            manifest = new Collection3(getManifestURI(ele.getPi()), ele.getPi());
            manifest.addBehavior(ViewingHint.multipart);
        } else {
            ele.setImageNumber(1);
            manifest = new Manifest3(getManifestURI(ele.getPi()));
            SearchService search = new SearchService(v1Builder.getSearchServiceURI(ele.getPi()));
            AutoSuggestService autoComplete = new AutoSuggestService(v1Builder.getAutoCompleteServiceURI(ele.getPi()));
            search.addService(autoComplete);
            manifest.addService(search);
        }

        populateData(ele, manifest);

        return manifest;
    }

    /**
     * @throws DAOException @throws URISyntaxException @throws ContentLibException @throws IllegalPathSyntaxException @param ele @param
     *             manifest @throws IndexUnreachableException @throws PresentationException @throws
     */
    private void addPages(StructElement ele, Manifest3 manifest) throws PresentationException, IndexUnreachableException, IllegalPathSyntaxException,
            ContentLibException, URISyntaxException, DAOException {
        CanvasBuilder canvasBuilder = new CanvasBuilder(urls);
        IPageLoader pageLoader = AbstractPageLoader.create(ele);
        for (int order = pageLoader.getFirstPageOrder(); order <= pageLoader.getLastPageOrder(); order++) {
            PhysicalElement page = pageLoader.getPage(order);
            if (page != null) {
                Canvas3 canvas = canvasBuilder.build(page);
                manifest.addItem(canvas);
            }
        }
    }

    /**
     * <p>
     * populate.
     * </p>
     *
     * @param ele a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param manifest a {@link de.intranda.api.iiif.presentation.AbstractPresentationModelElement} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    private AbstractPresentationModelElement3 populateData(StructElement ele, final AbstractPresentationModelElement3 manifest)
            throws ViewerConfigurationException, IndexUnreachableException, PresentationException {

        IMetadataValue label = getLabel(ele).orElse(ele.getMultiLanguageDisplayLabel());
        manifest.setLabel(label);
        getDescription(ele).ifPresent(desc -> manifest.setDescription(desc));

        manifest.addThumbnail(getThumbnail(ele));

        manifest.setRequiredStatement(getRequiredStatement());
        manifest.setRights(getRightsStatement(ele).orElse(null));
        manifest.setNavDate(getNavDate(ele));
        manifest.addBehavior(getViewingBehavior(ele));
        //TODO: add provider from config
        DataManager.getInstance().getConfiguration().getIIIFProvider().forEach(providerConfig -> manifest.addProvider(getProvider(providerConfig)));

        addMetadata(manifest, ele);

        addRelatedResources(manifest, ele);

        return manifest;
    }

    /**
     * @param manifest
     * @param ele
     */
    private void addRelatedResources(AbstractPresentationModelElement3 manifest, StructElement ele) {

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

            getCmsPageLinks(ele.getPi()).forEach(link -> manifest.addHomepage(link));
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF()) {
            URI uri = urls.path(RECORDS_RECORD, RECORDS_PDF).params(ele.getPi()).buildURI();
            LinkingProperty pdf =
                    new LinkingProperty(LinkingTarget.PDF, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPDF()));
            manifest.addRendering(pdf.getResource(uri));
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto()) {
            URI uri = urls.path(RECORDS_RECORD, RECORDS_ALTO).params(ele.getPi()).buildURI();
            LinkingProperty alto =
                    new LinkingProperty(LinkingTarget.ALTO, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingAlto()));
            manifest.addSeeAlso(alto.getResource(uri));
        }

        if (DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext()) {
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
                logger.error("Unable to create seeAlso link for " + config.getLabel(), e);
            }
        }

    }

    private String getType(String format) {
        if (StringUtils.isBlank(format)) {
            return "";
        } else {
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
    }

    /**
     * TODO: config for docStruct types that should be presented as "paged"
     *
     * @param ele
     * @return
     */
    private ViewingHint getViewingBehavior(StructElement ele) {
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

    private LocalDateTime getNavDate(StructElement ele) {
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

    private List<LabeledResource> getCmsPageLinks(String pi) {
        try {
            return DataManager.getInstance()
                    .getDao()
                    .getCMSPagesForRecord(pi, null)
                    .stream()
                    .filter(page -> page.isPublished())
                    .map(page -> {
                        LinkingProperty cmsPageLink = new LinkingProperty(LinkingTarget.VIEWER, page.getTitleTranslations());
                        URI uri = URI.create(page.getUrl());
                        if(!uri.isAbsolute()) {
                            uri = UriBuilder.fromUri(uri).path(page.getUrl()).build();
                        }
                        LabeledResource cmsPage = cmsPageLink.getResource(uri);
                        return cmsPage;
                    })
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            logger.warn(e.toString());
            return Collections.emptyList();
        }
    }

}
