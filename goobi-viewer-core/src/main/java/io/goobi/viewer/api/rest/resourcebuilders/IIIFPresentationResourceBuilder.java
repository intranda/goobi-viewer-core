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
package io.goobi.viewer.api.rest.resourcebuilders;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_IMAGE_IIIF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.Collection;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Layer;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.Range;
import de.intranda.api.iiif.presentation.Sequence;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.iiif.presentation.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.builder.CollectionBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.LayerBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.SequenceBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.StructureBuilder;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.BrowseDcElement;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.rest.content.ContentResource;

/**
 * @author florian
 *
 */
public class IIIFPresentationResourceBuilder {

    private ManifestBuilder manifestBuilder;
    private StructureBuilder structureBuilder;
    private SequenceBuilder sequenceBuilder;
    private LayerBuilder layerBuilder;
    private CollectionBuilder collectionBuilder;
    private final AbstractApiUrlManager urls;
    private final HttpServletRequest request;

    public IIIFPresentationResourceBuilder(AbstractApiUrlManager urls, HttpServletRequest request) {
        this.urls = urls;
        this.request = request;
    }

    public IPresentationModelElement getManifest(String pi, BuildMode mode) throws PresentationException, IndexUnreachableException,
            ContentNotFoundException, URISyntaxException, ViewerConfigurationException, DAOException {
        getManifestBuilder().setBuildMode(mode);
        getSequenceBuilder().setBuildMode(mode);
        List<StructElement> docs = getManifestBuilder().getDocumentWithChildren(pi);
        if (docs.isEmpty()) {
            throw new ContentNotFoundException("No document found for pi " + pi);
        }
        StructElement mainDoc = docs.get(0);
        IPresentationModelElement manifest = getManifestBuilder().generateManifest(mainDoc);

        if (manifest instanceof Collection && docs.size() > 1) {
            getManifestBuilder().addVolumes((Collection) manifest, docs.subList(1, docs.size()));
        } else if (manifest instanceof Manifest) {
            getManifestBuilder().addAnchor((Manifest) manifest, mainDoc.getMetadataValue(SolrConstants.PI_ANCHOR));

            getSequenceBuilder().addBaseSequence((Manifest) manifest, mainDoc, manifest.getId().toString(), request);

            String topLogId = mainDoc.getMetadataValue(SolrConstants.LOGID);
            if (StringUtils.isNotBlank(topLogId)) {
                List<Range> ranges = getStructureBuilder().generateStructure(docs, pi, false);
                ranges.forEach(range -> {
                    ((Manifest) manifest).addStructure(range);
                });
            }
        }

        return manifest;
    }

    public Range getRange(String pi, String logId) throws PresentationException, IndexUnreachableException,
            ContentNotFoundException, URISyntaxException, ViewerConfigurationException, DAOException {
        List<StructElement> docs = getStructureBuilder().getDocumentWithChildren(pi);

        if (docs.isEmpty()) {
            throw new ContentNotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found");
        }
        List<Range> ranges = getStructureBuilder().generateStructure(docs, pi, false);
        Optional<Range> range = ranges.stream().filter(r -> r.getId().toString().contains(logId + "/")).findFirst();
        return range.orElseThrow(() -> new ContentNotFoundException("Not document with PI = " + pi + " and logId = " + logId + " found"));
    }

    public Sequence getBaseSequence(String pi, BuildMode buildMode, String preferedViewName)
            throws PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException, IllegalRequestException, ContentNotFoundException {

        StructElement doc = getManifestBuilder().getDocument(pi);
        PageType preferedView = getPreferedPageTypeForCanvas(preferedViewName);

        IPresentationModelElement manifest = new ManifestBuilder(urls).setBuildMode(buildMode).generateManifest(doc);

        if (manifest instanceof Collection) {
            throw new IllegalRequestException("Identifier refers to a collection which does not have a sequence");
        } else if (manifest instanceof Manifest) {
            new SequenceBuilder(urls).setBuildMode(buildMode)
                    .setPreferedView(preferedView)
                    .addBaseSequence((Manifest) manifest, doc, manifest.getId().toString(), request);
            return ((Manifest) manifest).getSequences().get(0);
        }
        throw new ContentNotFoundException("Not manifest with identifier " + pi + " found");

    }

    /**
     * @param preferedViewName
     * @return
     */
    public PageType getPreferedPageTypeForCanvas(String preferedViewName) {
        PageType preferedView = PageType.viewObject;
        if (StringUtils.isNotBlank(preferedViewName)) {
            preferedView = PageType.getByName(preferedViewName);
            if (preferedView == PageType.other) {
                preferedView = PageType.viewObject;
            }
        }
        return preferedView;
    }

    public Layer getLayer(String pi, String typeName, BuildMode buildMode) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException, IOException {
        StructElement doc = getStructureBuilder().getDocument(pi);
        AnnotationType type = AnnotationType.valueOf(typeName.toUpperCase());
        if (type == null) {
            throw new IllegalRequestException("No valid annotation type: " + typeName);
        }
        if (doc == null) {
            throw new ContentNotFoundException("Not document with PI = " + pi + " found");
        } else if (AnnotationType.TEI.equals(type)) {
            return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.PAINTING, (id, repo) -> ContentResource.getTEIFiles(id),
                    (id, lang) -> ContentResource.getTEIURI(id, lang));
        } else if (AnnotationType.CMDI.equals(type)) {
            return getLayerBuilder().createAnnotationLayer(pi, type, Motivation.DESCRIBING, (id, repo) -> ContentResource.getCMDIFiles(id),
                    (id, lang) -> ContentResource.getCMDIURI(id, lang));

        } else {
            Map<AnnotationType, List<AnnotationList>> annoLists = getSequenceBuilder().addBaseSequence(null, doc, "", request);
            Layer layer = getLayerBuilder().generateLayer(pi, annoLists, type);
            return layer;
        }
    }

    /**
     * @param pi
     * @param pageNo
     * @return
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
            Canvas canvas = getSequenceBuilder().generateCanvas(doc, page);
            if (canvas != null) {
                getSequenceBuilder().addSeeAlsos(canvas, doc, page);
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
     * @return a {@link io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder} object.
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
     * @return a {@link io.goobi.viewer.model.iiif.presentation.builder.SequenceBuilder} object.
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
     * @return a {@link io.goobi.viewer.model.iiif.presentation.builder.LayerBuilder} object.
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
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Collection getCollections(String collectionField)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, IllegalRequestException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, null, null,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

        return collection;

    }

    /**
     * Returns a iiif collection of all collections from the given solr-field The response includes the metadata and subcollections of the topmost
     * collections. Child collections may be accessed following the links in the @id properties in the member-collections Requires passing a language
     * to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Collection getCollectionsWithGrouping(String collectionField, String groupingField)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException, IllegalRequestException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, null, groupingField,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

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
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException If the topElement is not a collection
     */
    public Collection getCollection(String collectionField, String topElement)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException, IllegalRequestException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, topElement, null,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

        return collection;

    }

    public List<IPresentationModelElement> getManifestsForQuery(String query, String sortFields, int first, int rows)
            throws DAOException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException {

        String finalQuery = query + " " + SearchHelper.ALL_RECORDS_QUERY;

        List<StringPair> sortFieldList = SolrSearchIndex.getSolrSortFieldsAsList(sortFields == null ? "" : sortFields, ",", " ");
        SolrDocumentList queryResults = DataManager.getInstance()
                .getSearchIndex()
                .search(finalQuery, first, rows, sortFieldList, null, Arrays.asList(CollectionBuilder.CONTAINED_WORKS_QUERY_FIELDS))
                .getResults();

        List<IPresentationModelElement> manifests = new ArrayList<>(queryResults.size());
        ManifestBuilder builder = new ManifestBuilder(urls);
        for (SolrDocument doc : queryResults) {
            long luceneId = Long.parseLong(doc.getFirstValue(SolrConstants.IDDOC).toString());
            StructElement ele = new StructElement(luceneId, doc);
            AbstractPresentationModelElement manifest = builder.generateManifest(ele);

            AbstractApiUrlManager imageUrls = DataManager.getInstance().getRestApiManager().getContentApiManager();

            if (imageUrls != null && manifest.getThumbnails().isEmpty()) {
                int thumbsWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
                int thumbsHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
                String thumbnailUrl = imageUrls.path(RECORDS_RECORD, RECORDS_IMAGE_IIIF)
                        .params(ele.getPi(), "full", "!" + thumbsWidth + "," + thumbsHeight, 0, "default", "jpg")
                        .build();
                manifest.addThumbnail(new ImageContent(URI.create(thumbnailUrl)));
            }

            manifests.add(manifest);
        }

        return manifests;

    }

    /**
     * Returns a iiif collection of the given topCollection for the give collection field The response includes the metadata and subcollections of the
     * direct child collections. Collections further down the hierarchy may be accessed following the links in the @id properties in the
     * member-collections Requires passing a language to set the language for all metadata values
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @param groupingField a solr field by which the collections may be grouped. Included in the response for each {@link BrowseDcElement} to enable
     *            grouping by client
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException if the topElement is not a collection
     */
    public Collection getCollectionWithGrouping(String collectionField, String topElement, String facetField)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException, IllegalRequestException {

        Collection collection = getCollectionBuilder().generateCollection(collectionField, topElement, facetField,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));

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
