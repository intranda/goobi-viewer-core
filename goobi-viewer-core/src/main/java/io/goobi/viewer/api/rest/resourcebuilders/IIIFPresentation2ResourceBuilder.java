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
package io.goobi.viewer.api.rest.resourcebuilders;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CMDI_LANG;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TEI_LANG;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.presentation.v2.AbstractPresentationModelElement2;
import de.intranda.api.iiif.presentation.v2.AnnotationList;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Collection2;
import de.intranda.api.iiif.presentation.v2.Layer;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.api.iiif.presentation.v2.Range2;
import de.intranda.api.iiif.presentation.v2.Sequence;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.iiif.presentation.v2.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.v2.builder.CollectionBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LayerBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.ManifestBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.SequenceBuilder;
import io.goobi.viewer.model.iiif.presentation.v2.builder.StructureBuilder;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * @author florian
 *
 */
public class IIIFPresentation2ResourceBuilder {

    private static final Logger logger = LogManager.getLogger(IIIFPresentation2ResourceBuilder.class); //NOSONAR Sometimes used for debugging

    private ManifestBuilder manifestBuilder;
    private StructureBuilder structureBuilder;
    private SequenceBuilder sequenceBuilder;
    private LayerBuilder layerBuilder;
    private CollectionBuilder collectionBuilder;
    private final AbstractApiUrlManager urls;
    private final HttpServletRequest request;

    public IIIFPresentation2ResourceBuilder(AbstractApiUrlManager urls, HttpServletRequest request) {
        this.urls = urls;
        this.request = request;
    }

    /**
     * 
     * @param pi
     * @param pagesToInclude
     * @param mode
     * @return {@link IPresentationModelElement}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentNotFoundException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    public IPresentationModelElement getManifest(String pi, List<Integer> pagesToInclude, BuildMode mode)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, URISyntaxException, ViewerConfigurationException,
            DAOException {
        getManifestBuilder().setBuildMode(mode);
        getSequenceBuilder().setBuildMode(mode);
        List<StructElement> docs = BuildMode.IIIF.equals(mode) || BuildMode.THUMBS.equals(mode) || !pagesToInclude.isEmpty()
                ? getManifestBuilder().getDocumentWithChildren(pi) : Arrays.asList(getManifestBuilder().getDocument(pi));
        if (docs.isEmpty()) {
            throw new ContentNotFoundException("No document found for pi " + pi);
        }
        StructElement mainDoc = docs.get(0);
        IPresentationModelElement manifest = getManifestBuilder().generateManifest(mainDoc, pagesToInclude);

        if (manifest instanceof Collection2 col2 && docs.size() > 1) {
            getManifestBuilder().addVolumes(col2, docs.subList(1, docs.size()));
        } else if (manifest instanceof Manifest2 col2) {
            getManifestBuilder().addAnchor(col2, mainDoc.getMetadataValue(SolrConstants.PI_ANCHOR));
            getSequenceBuilder().addBaseSequence((Manifest2) manifest, mainDoc, manifest.getId().toString(), pagesToInclude, request);
            String topLogId = mainDoc.getMetadataValue(SolrConstants.LOGID);
            if (StringUtils.isNotBlank(topLogId) && BuildMode.IIIF.equals(mode) && pagesToInclude.isEmpty()) {
                List<Range2> ranges = getStructureBuilder().generateStructure(docs, pi, false);
                ranges.forEach(range -> {
                    ((Manifest2) manifest).addStructure(range);
                });
            }
        }

        return manifest;
    }

    /**
     * 
     * @param pi
     * @param logId
     * @return {@link Range2}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentNotFoundException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    public Range2 getRange(String pi, String logId) throws PresentationException, IndexUnreachableException,
            ContentNotFoundException, URISyntaxException, ViewerConfigurationException, DAOException {
        List<StructElement> docs = getStructureBuilder().getDocumentWithChildren(pi);

        if (docs.isEmpty()) {
            throw new ContentNotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found");
        }
        List<Range2> ranges = getStructureBuilder().generateStructure(docs, pi, false);
        Optional<Range2> range = ranges.stream().filter(r -> r.getId().toString().contains(logId + "/")).findFirst();
        return range.orElseThrow(() -> new ContentNotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found"));
    }

    /**
     * 
     * @param pi
     * @param buildMode
     * @param preferedViewName
     * @return {@link Sequence}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws IllegalRequestException
     * @throws ContentNotFoundException
     */
    public Sequence getBaseSequence(String pi, BuildMode buildMode, String preferedViewName)
            throws PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException, IllegalRequestException, ContentNotFoundException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        PageType preferedView = getPreferedPageTypeForCanvas(preferedViewName);

        IPresentationModelElement manifest = new ManifestBuilder(urls).setBuildMode(buildMode).generateManifest(doc, Collections.emptyList());

        if (manifest instanceof Collection2) {
            throw new IllegalRequestException("Identifier refers to a collection which does not have a sequence");
        } else if (manifest instanceof Manifest2 manifest2) {
            new SequenceBuilder(urls).setBuildMode(buildMode)
                    .setPreferedView(preferedView)
                    .addBaseSequence(manifest2, doc, manifest.getId().toString(), Collections.emptyList(), request);
            return manifest2.getSequences().get(0);
        }
        throw new ContentNotFoundException("Not manifest with identifier " + pi + " found");

    }

    /**
     * @param preferredViewName
     * @return Preferred {@link PageType} for given preferredViewName
     */
    public PageType getPreferedPageTypeForCanvas(String preferredViewName) {
        PageType preferedView = PageType.viewObject;
        if (StringUtils.isNotBlank(preferredViewName)) {
            preferedView = PageType.getByName(preferredViewName);
            if (preferedView == PageType.other) {
                preferedView = PageType.viewObject;
            }
        }
        return preferedView;
    }

    /**
     * 
     * @param pi
     * @param typeName
     * @return {@link Layer}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws ContentNotFoundException
     * @throws IllegalRequestException
     * @throws IOException
     */
    public Layer getLayer(String pi, String typeName) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException, IOException {
        StructElement doc = getStructureBuilder().getDocument(pi);
        AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
        if (type == null) {
            throw new IllegalRequestException("No valid annotation type: " + typeName);
        }
        if (doc == null) {
            throw new ContentNotFoundException("Not document with PI = " + pi + " found");
        } else if (AnnotationType.TEI.equals(type)) {
            return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.PAINTING, (id, repo) -> new TextResourceBuilder().getTEIFiles(id),
                    (id, lang) -> urls.path(RECORDS_RECORD, RECORDS_TEI_LANG).params(id, lang).buildURI());
        } else if (AnnotationType.CMDI.equals(type)) {
            return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.DESCRIBING, (id, repo) -> new TextResourceBuilder().getCMDIFiles(id),
                    (id, lang) -> urls.path(RECORDS_RECORD, RECORDS_CMDI_LANG).params(id, lang).buildURI());

        } else {
            Map<AnnotationType, List<AnnotationList>> annoLists =
                    getSequenceBuilder().addBaseSequence(null, doc, "", Collections.emptyList(), request);
            return getLayerBuilder().generateLayer(pi, annoLists, type);
        }
    }

    /**
     * @param pi
     * @param pageNo
     * @return {@link IPresentationModelElement}
     * @throws ViewerConfigurationException
     * @throws URISyntaxException
     * @throws ContentNotFoundException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public IPresentationModelElement getCanvas(String pi, Integer pageNo) throws URISyntaxException, ViewerConfigurationException,
            ContentNotFoundException, PresentationException, IndexUnreachableException, DAOException {
        StructElement doc = getManifestBuilder().getDocument(pi);
        if (doc != null) {
            PhysicalElement page = getSequenceBuilder().getPage(doc, pageNo);
            Canvas2 canvas = getSequenceBuilder().generateCanvas(doc.getPi(), page);
            if (canvas != null) {
                getSequenceBuilder().addSeeAlsos(canvas, page);
                getSequenceBuilder().addOtherContent(doc, page, canvas, false);
                getSequenceBuilder().addCrowdourcingAnnotations(Collections.singletonList(canvas),
                        new OpenAnnotationBuilder(urls).getCrowdsourcingAnnotations(pi, false, request), null);
                return canvas;
            }
        }
        throw new ContentNotFoundException("No page found with order= " + pageNo + " and pi = " + pi);
    }

    private StructureBuilder getStructureBuilder() {
        if (this.structureBuilder == null) {
            this.structureBuilder = new StructureBuilder(urls);
        }
        return this.structureBuilder;
    }

    /**
     * <p>
     * Getter for the field <code>manifestBuilder</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.ManifestBuilder} object.
     */
    public ManifestBuilder getManifestBuilder() {
        if (this.manifestBuilder == null) {
            this.manifestBuilder = new ManifestBuilder(urls);
        }
        return manifestBuilder;
    }

    /**
     * <p>
     * Getter for the field <code>sequenceBuilder</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.SequenceBuilder} object.
     */
    public SequenceBuilder getSequenceBuilder() {
        if (this.sequenceBuilder == null) {
            this.sequenceBuilder = new SequenceBuilder(urls);
        }
        return sequenceBuilder;
    }

    /**
     * <p>
     * Getter for the field <code>layerBuilder</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.iiif.presentation.v2.builder.LayerBuilder} object.
     */
    public LayerBuilder getLayerBuilder() {
        if (this.layerBuilder == null) {
            this.layerBuilder = new LayerBuilder(urls);
        }
        return layerBuilder;
    }

    /**
     * Returns a iiif collection of all collections from the given solr-field The response includes the metadata and subcollections of the topmost
     * collections. Child collections may be accessed following the links in the @id properties in the member-collections Requires passing a language
     * to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param ignore
     * @return a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Collection2 getCollections(String collectionField, List<String> ignore)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, IllegalRequestException {
        return getCollectionBuilder().generateCollection(collectionField, null, null,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField), ignore);
    }

    /**
     * Returns a iiif collection of all collections from the given solr-field The response includes the metadata and subcollections of the topmost
     * collections. Child collections may be accessed following the links in the @id properties in the member-collections Requires passing a language
     * to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param ignore
     * @param groupingField
     * @return a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Collection2 getCollectionsWithGrouping(String collectionField, List<String> ignore, String groupingField)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, IllegalRequestException {

        Collection2 collection = getCollectionBuilder().generateCollection(collectionField, null, groupingField,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField), ignore);

        getCollectionBuilder().addTagListService(collection, collectionField, groupingField, "grouping");

        return collection;

    }

    /**
     * Returns a iiif collection of the given topCollection for the give collection field The response includes the metadata and subcollections of the
     * direct child collections. Collections further down the hierarchy may be accessed following the links in the @id properties in the
     * member-collections Requires passing a language to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @param ignore
     * @return a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException If the topElement is not a collection
     */
    public Collection2 getCollection(String collectionField, String topElement, List<String> ignore)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException, IllegalRequestException {
        return getCollectionBuilder().generateCollection(collectionField, topElement, null,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField), ignore);
    }

    /**
     * 
     * @param query
     * @param sortFields
     * @param first
     * @param rows
     * @return List<IPresentationModelElement>
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     * @throws ViewerConfigurationException
     */
    public List<IPresentationModelElement> getManifestsForQuery(String query, String sortFields, int first, int rows)
            throws DAOException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException {

        String finalQuery = SearchHelper.buildFinalQuery(query, false, request, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);

        List<StringPair> sortFieldList = SolrTools.getSolrSortFieldsAsList(sortFields == null ? "" : sortFields, ",", " ");
        SolrDocumentList queryResults = DataManager.getInstance()
                .getSearchIndex()
                .search(finalQuery, first, rows, sortFieldList, null, getContainedRecordsFieldList())
                .getResults();

        List<IPresentationModelElement> manifests = new ArrayList<>(queryResults.size());
        ManifestBuilder builder = new ManifestBuilder(urls);
        for (SolrDocument doc : queryResults) {
            long luceneId = Long.parseLong(doc.getFirstValue(SolrConstants.IDDOC).toString());
            StructElement ele = new StructElement(luceneId, doc);
            AbstractPresentationModelElement2 manifest = builder.generateManifest(ele, Collections.emptyList());

            if (this.urls != null && manifest.getThumbnails().isEmpty()) {
                int thumbsWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
                int thumbsHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
                String thumbnailUrl = BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(ele.getPi(), thumbsWidth, thumbsHeight);
                ImageContent thumbnail = new ImageContent(URI.create(thumbnailUrl));
                String imageInfoURI = IIIFUrlResolver.getIIIFImageBaseUrl(thumbnailUrl);
                thumbnail.setService(new ImageInformation(imageInfoURI));
                manifest.addThumbnail(thumbnail);
            }

            manifests.add(manifest);
        }

        return manifests;

    }

    private static List<String> getContainedRecordsFieldList() {
        List<String> list = new ArrayList<>(Arrays.asList(CollectionBuilder.CONTAINED_WORKS_QUERY_FIELDS));
        list.add(SolrConstants.BOOL_IMAGEAVAILABLE);
        return list;
    }

    /**
     * Returns a iiif collection of the given topCollection for the give collection field The response includes the metadata and subcollections of the
     * direct child collections. Collections further down the hierarchy may be accessed following the links in the @id properties in the
     * member-collections Requires passing a language to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @param facetField
     * @param ignore
     * @return a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException if the topElement is not a collection
     */
    public Collection2 getCollectionWithGrouping(String collectionField, String topElement, String facetField, List<String> ignore)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException, IllegalRequestException {

        Collection2 collection = getCollectionBuilder().generateCollection(collectionField, topElement, facetField,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField), ignore);

        getCollectionBuilder().addTagListService(collection, collectionField, facetField, "grouping");

        return collection;

    }

    /**
     * <p>
     * Getter for the field <code>collectionBuilder</code>.
     * </p>
     *
     * @return the manifestBuilder
     */
    public CollectionBuilder getCollectionBuilder() {
        if (this.collectionBuilder == null) {
            try {
                this.collectionBuilder = new CollectionBuilder(urls);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
        }
        return collectionBuilder;
    }

}
