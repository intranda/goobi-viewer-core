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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_ALTO;
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
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.presentation.enums.Format;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v3.AbstractPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.Canvas3;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.IPresentationModelElement3;
import de.intranda.api.iiif.presentation.v3.LabeledResource;
import de.intranda.api.iiif.presentation.v3.Manifest3;
import de.intranda.api.iiif.search.AutoSuggestService;
import de.intranda.api.iiif.search.SearchService;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.iiif.presentation.v3.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
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

    private static final Logger logger = LoggerFactory.getLogger(ManifestBuilder.class);

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

    public IPresentationModelElement3 build(String pi) throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IllegalPathSyntaxException, ContentLibException, URISyntaxException {

        List<StructElement> documents = this.dataRetriever.getDocumentWithChildren(pi);

        StructElement mainDocument = documents.get(0);
        List<StructElement> childDocuments = documents.subList(1, documents.size());

        AbstractPresentationModelElement3 manifest = generateManifest(mainDocument);

        return manifest;
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
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IllegalPathSyntaxException, ContentLibException, URISyntaxException {

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

        populateItems(ele, manifest);
        
        return manifest;
    }

    /**
     * @throws URISyntaxException 
     * @throws ContentLibException 
     * @throws IllegalPathSyntaxException 
     * @param ele
     * @param manifest
     * @throws IndexUnreachableException 
     * @throws PresentationException 
     * @throws  
     */
    private void populateItems(StructElement ele, AbstractPresentationModelElement3 manifest) throws PresentationException, IndexUnreachableException, IllegalPathSyntaxException, ContentLibException, URISyntaxException {
        CanvasBuilder canvasBuilder = new CanvasBuilder(urls);
        if(manifest instanceof Manifest3) {
            IPageLoader pageLoader = new EagerPageLoader(ele);
            for (int order = pageLoader.getFirstPageOrder(); order <= pageLoader.getLastPageOrder(); order++) {
                PhysicalElement page = pageLoader.getPage(order);
                if(page != null) {
                    Canvas3 canvas = canvasBuilder.build(page);
                    ((Manifest3)manifest).addItem(canvas);
                }
            }            
        } else if(manifest instanceof Collection3) {
            
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
        manifest.setLabel(ele.getMultiLanguageDisplayLabel());
        getDescription(ele).ifPresent(desc -> manifest.setDescription(desc));

        manifest.addThumbnail(getThumbnail(ele));

        manifest.setRequiredStatement(getRequiredStatement());
        manifest.setRights(DataManager.getInstance().getConfiguration().getIIIFLicenses().stream().findFirst().map(URI::create).orElse(null));
        manifest.setNavDate(getNavDate(ele));
        manifest.addBehavior(getViewingBehavior(ele));
        //TODO: add provider from config

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
        if (ele.isLidoRecord()) {
            LabeledResource resolver = new LabeledResource(getLidoResolverUrl(ele), "Dataset", Format.TEXT_XML.getLabel(), "http://www.lido-schema.org", null);
            manifest.addSeeAlso(resolver);
        } else {
            LabeledResource resolver = new LabeledResource(getMetsResolverUrl(ele), "Dataset", Format.TEXT_XML.getLabel(), "http://www.loc.gov/METS/", null);
            manifest.addSeeAlso(resolver);
        }
        
        if(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer()) {
            PageType pageType = PageType.viewMetadata;
            if(ele.isHasImages()) {
                pageType = PageType.viewImage;
            } else if(ele.isAnchor()) {
                pageType = PageType.viewToc;
            } 
            URI recordURI = UriBuilder.fromPath(urls.getApplicationUrl()).path("{pageType}").path("{pi}").build(pageType.getName(), ele.getPi());
            LinkingProperty homepage = new LinkingProperty(LinkingTarget.VIEWER, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingViewer()));            
            manifest.addHomepage(homepage.getResource(recordURI));
            
            getCmsPageLinks(ele.getPi()).forEach(link -> manifest.addHomepage(link));
        }
        
        if(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF()) {
            URI uri = urls.path(RECORDS_RECORD, RECORDS_PDF).params(ele.getPi()).buildURI();
            LinkingProperty pdf = new LinkingProperty(LinkingTarget.PDF, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPDF()));            
            manifest.addRendering(pdf.getResource(uri));
        }

        if(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto()) {
            URI uri = urls.path(RECORDS_RECORD, RECORDS_ALTO).params(ele.getPi()).buildURI();
            LinkingProperty alto = new LinkingProperty(LinkingTarget.ALTO, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingAlto()));            
            manifest.addSeeAlso(alto.getResource(uri));
        }
        
        if(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext()) {
            URI uri = urls.path(RECORDS_RECORD, RECORDS_PLAINTEXT).params(ele.getPi()).buildURI();
            LinkingProperty text = new LinkingProperty(LinkingTarget.PLAINTEXT, createLabel(DataManager.getInstance().getConfiguration().getLabelIIIFRenderingPlaintext()));            
            manifest.addRendering(text.getResource(uri));
        }

        
    }

    /**
     * @param labelIIIFRenderingViewer
     * @return
     */
    private IMetadataValue createLabel(String text) {
        if(StringUtils.isBlank(text)) {
            return null;
        } else {
            return new SimpleMetadataValue(text);
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
                            LabeledResource cmsPage = cmsPageLink.getResource(URI.create(this.urls.getApplicationUrl() + "/" + page.getUrl()));
                            return cmsPage;
                    })
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            logger.warn(e.toString());
            return Collections.emptyList();
        }
    }

}
