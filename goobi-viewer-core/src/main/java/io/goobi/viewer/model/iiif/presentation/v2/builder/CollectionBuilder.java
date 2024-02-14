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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.CollectionExtent;
import de.intranda.api.iiif.presentation.TagListService;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.api.iiif.presentation.v2.AbstractPresentationModelElement2;
import de.intranda.api.iiif.presentation.v2.Collection2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.collections.CMSCollection;
import io.goobi.viewer.model.iiif.presentation.v2.builder.LinkingProperty.LinkingTarget;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.collections.BrowseElementInfo;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * CollectionBuilder class.
 * </p>
 *
 * @author Florian Alpers
 */
public class CollectionBuilder extends AbstractBuilder {

    private static final Logger logger = LogManager.getLogger(CollectionBuilder.class);

    /**
     * Required field to create manifest stubs for works in collection
     */
    public static final String[] CONTAINED_WORKS_QUERY_FIELDS =
            { SolrConstants.PI, SolrConstants.ISANCHOR, SolrConstants.ISWORK, SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.DOCSTRCT,
                    SolrConstants.IDDOC };
    /** Constant <code>RSS_FEED_LABEL="Rss feed"</code> */
    public static final String RSS_FEED_LABEL = "Rss feed";
    /** Constant <code>RSS_FEED_FORMAT="Rss feed"</code> */
    public static final String RSS_FEED_FORMAT = "Rss feed";

    /**
     * Caching for collections
     */
    private static Map<String, String> facetFieldMap = new HashMap<>();
    //    private static Map<String, CollectionView> collectionViewMap = new HashMap<>();

    /**
     * <p>
     * Constructor for CollectionBuilder.
     * </p>
     *
     * @param apiUrlManager
     * @throws java.net.URISyntaxException if any.
     */
    public CollectionBuilder(AbstractApiUrlManager apiUrlManager) throws URISyntaxException {
        super(apiUrlManager);
    }

    /**
     * <p>
     * generateCollection.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @param splittingChar a {@link java.lang.String} object.
     * @param facetField A SOLR field which values are requested for all records within the collection and stored within the collection for later use
     * @param ignoreCollections
     * @return a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IllegalRequestException if the top element is not empty and is not a collection
     */
    public Collection2 generateCollection(String collectionField, final String topElement, final String facetField, final String splittingChar,
            final List<String> ignoreCollections)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException, IllegalRequestException {

        CollectionView collectionView = getCollectionView(collectionField, facetField, splittingChar);
        collectionView.setIgnore(ignoreCollections);
        //        CollectionView collectionView = createCollectionView(collectionField, facetField, splittingChar);
        if (StringUtils.isNotBlank(topElement) && !"-".equals(topElement)) {
            collectionView.setTopVisibleElement(topElement);
            collectionView.setDisplayParentCollections(false);
        }
        collectionView.calculateVisibleDcElements(true);

        HierarchicalBrowseDcElement baseElement = null;
        if (StringUtils.isNotBlank(collectionView.getTopVisibleElement())) {
            baseElement = collectionView.getCompleteList()
                    .stream()
                    .filter(element -> topElement.startsWith(element.getName()))
                    .flatMap(element -> element.getAllDescendents(true).stream())
                    .filter(element -> topElement.equals(element.getName()))
                    .findFirst()
                    .orElse(null);

        }

        Collection2 collection;
        if (baseElement != null) {
            /*
             * First make sure that the base Element is contained within visibleElements, then recalculate the visibleElements to
             * get CMS-Information for the base Element and its children
             */
            collectionView.setDisplayParentCollections(true);
            collectionView.calculateVisibleDcElements(true);
            collection = createCollection(collectionView, baseElement, getCollectionURI(collectionField, baseElement.getName()));

            String parentName = null;
            if (baseElement.getParent() != null) {
                parentName = baseElement.getParent().getName();
            }
            Collection2 parent = createCollection(collectionView, baseElement.getParent(), getCollectionURI(collectionField, parentName));
            collection.addWithin(parent);

            for (HierarchicalBrowseDcElement childElement : baseElement.getChildren()) {
                Collection2 child = createCollection(collectionView, childElement, getCollectionURI(collectionField, childElement.getName()));
                collection.addCollection(child);
            }

            addContainedWorks(collectionField, topElement, collection);

        } else {
            collection = createCollection(collectionView, null, getCollectionURI(collectionField, null));

            for (HierarchicalBrowseDcElement childElement : collectionView.getVisibleDcElements()) {
                Collection2 child = createCollection(collectionView, childElement, getCollectionURI(collectionField, childElement.getName()));
                collection.addCollection(child);
            }
        }

        return collection;
    }

    /**
     * <p>
     * addContainedWorks.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @param collection a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     */
    public void addContainedWorks(String collectionField, final String topElement, Collection2 collection)
            throws PresentationException, IndexUnreachableException, URISyntaxException {
        SolrDocumentList containedWorks = getContainedWorks(createCollectionQuery(collectionField, topElement));
        if (containedWorks != null) {
            for (SolrDocument solrDocument : containedWorks) {

                AbstractPresentationModelElement2 work;
                Boolean anchor = (Boolean) solrDocument.getFirstValue(SolrConstants.ISANCHOR);
                String pi = solrDocument.getFirstValue(SolrConstants.PI).toString();
                URI uri = getManifestURI(pi);
                if (Boolean.TRUE.equals(anchor)) {
                    work = new Collection2(uri, pi);
                    work.addViewingHint(ViewingHint.multipart);
                    collection.addCollection((Collection2) work);
                } else {
                    work = new Manifest2(uri);
                    collection.addManifest((Manifest2) work);
                }
                getLabelIfExists(solrDocument).ifPresent(label -> work.setLabel(label));
            }
        }
    }

    /**
     * @param query
     * @return {@link SolrDocumentList}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private static SolrDocumentList getContainedWorks(String query) throws PresentationException, IndexUnreachableException {
        return DataManager.getInstance().getSearchIndex().getDocs(query, Arrays.asList(CONTAINED_WORKS_QUERY_FIELDS));
    }

    /**
     * <p>
     * createCollectionQuery.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String createCollectionQuery(String collectionField, final String topElement) {
        String query;
        if (topElement != null) {
            query = collectionField + ":\"" + topElement + "\" OR " + collectionField + ":\"" + topElement + ".*\"";
        } else {
            query = collectionField + ":*";
        }
        query = "(" + query + ") AND (ISWORK:true OR ISANCHOR:true)";
        return query;
    }

    /**
     * <p>
     * createCollection.
     * </p>
     *
     * @param baseElement a {@link io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement} object.
     * @param collectionView a {@link io.goobi.viewer.model.viewer.collections.CollectionView} object.
     * @param uri a {@link java.net.URI} object.
     * @return a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Collection2 createCollection(CollectionView collectionView, HierarchicalBrowseDcElement baseElement, URI uri)
            throws URISyntaxException, ViewerConfigurationException {
        try {
            Collection2 collection = new Collection2(uri, baseElement == null ? null : baseElement.getName());
            this.getAttributions().forEach(collection::addAttribution);
            if (baseElement != null) {

                BrowseElementInfo info = baseElement.getInfo();
                if (info instanceof CMSCollection) {
                    collection.setDescription(info.getTranslationsForDescription());
                }
                collection.setLabel(getLabel(baseElement.getName()));

                URI thumbURI = absolutize(baseElement.getInfo().getIconURI());
                if (thumbURI != null) {
                    ImageContent thumb = new ImageContent(thumbURI);
                    collection.addThumbnail(thumb);
                    if (IIIFUrlResolver.isIIIFImageUrl(thumbURI.toString())) {
                        URI imageInfoURI = new URI(IIIFUrlResolver.getIIIFImageBaseUrl(thumbURI.toString()));
                        thumb.setService(new ImageInformation(imageInfoURI.toString()));
                    }
                }

                long volumes = baseElement.getNumberOfVolumes();
                int subCollections = baseElement.getChildren().size();
                CollectionExtent extentService = new CollectionExtent(subCollections, (int) volumes);
                extentService.setBaseURI(urls.path(ApiUrls.CONTEXT).build());
                collection.addService(extentService);

                String rssUrl = urls.path(ApiUrls.RECORDS_RSS).query("query", baseElement.getSolrFilterQuery()).build();
                LinkingContent rss =
                        new LinkingContent(URI.create(rssUrl), new SimpleMetadataValue(RSS_FEED_LABEL));
                collection.addRelated(rss);

                addRenderings(baseElement, collectionView, collection);

            } else {
                collection.addViewingHint(ViewingHint.top);
            }
            return collection;

        } catch (URISyntaxException e) {
            logger.error(e.toString(), e);
        }
        return null;
    }

    /**
     * @param baseElement
     * @param collection
     */
    private void addRenderings(HierarchicalBrowseDcElement baseElement, CollectionView collectionView, Collection2 collection) {

        this.getRenderings().forEach(link -> {
            URI id = getLinkingPropertyUri(baseElement, collectionView, link.target);
            if (id != null) {
                collection.addRendering(link.getLinkingContent(id));
            }
        });

    }

    /**
     * @param baseElement
     * @param collectionView
     * @param target
     * @return {@link URI}
     */
    private URI getLinkingPropertyUri(HierarchicalBrowseDcElement baseElement, CollectionView collectionView, LinkingTarget target) {

        URI uri = null;
        switch (target) {
            case VIEWER:
                uri = absolutize(collectionView.getCollectionUrl(baseElement));
                break;
            default:
                break;
        }
        return uri;
    }

    /**
     * Add a taglist service to the collection and all subcollections. The taglist service provides a list of
     *
     * @param collection
     * @param collectionField
     * @param facetField
     * @param label
     * @throws IndexUnreachableException
     * @throws IllegalRequestException
     */
    public void addTagListService(Collection2 collection, String collectionField, final String facetField, String label)
            throws IndexUnreachableException, IllegalRequestException {
        CollectionView view = getCollectionView(collectionField, facetField,
                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));
        addTagListService(collection, view, label);
        collection.collections.forEach(c -> addTagListService(c, view, label));

    }

    /**
     * @param collection
     * @param view
     * @param label
     */
    private void addTagListService(Collection2 collection, CollectionView view, String label) {
        if (collection.getInternalName() != null) {
            view.getCompleteList().stream().filter(e -> collection.getInternalName().equals(e.getName())).findAny().ifPresent(ele -> {
                TagListService tagsService = new TagListService(label, urls.path(ApiUrls.CONTEXT).build());
                tagsService.setTags(ele.getFacetValues());
                collection.addService(tagsService);

            });
        }

    }

    /**
     * <p>
     * getCollectionView.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param groupingField a {@link java.lang.String} object.
     * @param splittingChar a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.collections.CollectionView} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public CollectionView getCollectionView(String collectionField, final String groupingField, final String splittingChar)
            throws IndexUnreachableException, IllegalRequestException {

        String key = collectionField + "::" + groupingField;
        if (BeanUtils.getSessionBean().containsKey(key)) {
            return new CollectionView((CollectionView) BeanUtils.getSessionBean().get(key));
        }

        return createCollectionView(collectionField, groupingField, splittingChar);
    }

    /**
     * @param collectionField
     * @param facetField A SOLR field which values are queried and kept in the collectionView for later use
     * @param splittingChar
     * @return {@link CollectionView}
     * @throws IndexUnreachableException
     * @throws IllegalRequestException
     */
    public CollectionView createCollectionView(String collectionField, final String facetField, final String splittingChar)
            throws IndexUnreachableException, IllegalRequestException {
        CollectionView view = new CollectionView(collectionField,
                () -> SearchHelper.findAllCollectionsFromField(collectionField, facetField, null, true, true, splittingChar));
        view.populateCollectionList();

        String key = collectionField + "::" + facetField;
        BeanUtils.getSessionBean().put(key, view);
        return view;
    }

    /**
     * <p>
     * getFacetField.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getFacetField(String collectionField) {

        synchronized (facetFieldMap) {
            if (facetFieldMap.containsKey(collectionField)) {
                return facetFieldMap.get(collectionField);
            }
            String facetField = collectionField;
            if (collectionField.startsWith("MD_")) {
                facetField = collectionField.replace("MD_", "FACET_");
            } else {
                facetField = "MD_" + collectionField;
            }
            try {
                if (!DataManager.getInstance().getSearchIndex().getAllFieldNames().contains(facetField)) {
                    facetField = collectionField;
                }
            } catch (IndexUnreachableException e) {
                logger.warn("Unable to query for facet field", e);
                facetField = collectionField;
            }
            facetFieldMap.put(collectionField, facetField);
            return facetField;
        }

    }
}
