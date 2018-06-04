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
package de.intranda.digiverso.presentation.model.iiif.presentation.builder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Collection;
import de.intranda.digiverso.presentation.model.iiif.presentation.CollectionExtent;
import de.intranda.digiverso.presentation.model.iiif.presentation.Manifest;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.content.LinkingContent;
import de.intranda.digiverso.presentation.model.iiif.presentation.enums.ViewingHint;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.IMetadataValue;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.HierarchicalBrowseDcElement;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;

/**
 * @author Florian Alpers
 *
 */
public class CollectionBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CollectionBuilder.class);
    
    private static final String[] CONTAINED_WORKS_QUERY_FIELDS = {SolrConstants.PI, SolrConstants.ISANCHOR, SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.DOCSTRCT};
    public final static String RSS_FEED_LABEL = "Rss feed";
    public final static String RSS_FEED_FORMAT = "Rss feed";
    
    /**
     * Caching for collections
     */
    private static Map<String, String> facetFieldMap = new HashMap<>();
    private static Map<String, CollectionView> collectionViewMap = new HashMap<>();
    
    /**
     * @param request
     * @throws URISyntaxException
     */
    public CollectionBuilder(HttpServletRequest request) throws URISyntaxException {
        super(request);
    }

    /**
     * @param servletUri
     * @param requestURI
     */
    public CollectionBuilder(URI servletUri, URI requestURI) {
        super(servletUri, requestURI);
    }

    /**
     * @param collectionField
     * @param topElement
     * @param url
     * @param locale
     * @param facetField
     * @return
     * @throws IndexUnreachableException
     * @throws MalformedURLException
     * @throws URISyntaxException
     * @throws PresentationException 
     */
    public Collection generateCollection(String collectionField, final String topElement)
            throws IndexUnreachableException, URISyntaxException, PresentationException {

        CollectionView collectionView = getCollectionView(collectionField, getFacetField(collectionField));

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

        Collection collection;
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
            Collection parent =
                    createCollection(collectionView, baseElement.getParent(), getCollectionURI(collectionField, parentName));
            collection.addWithin(parent);

            for (HierarchicalBrowseDcElement childElement : baseElement.getChildren()) {
                Collection child = createCollection(collectionView, childElement, getCollectionURI(collectionField, childElement.getName()));
                collection.addCollection(child);
            }
            
            addContainedWorks(collectionField, topElement, collection);
            
        } else {
            collection = createCollection(collectionView, null, getCollectionURI(collectionField, null));

            for (HierarchicalBrowseDcElement childElement : collectionView.getVisibleDcElements()) {
                Collection child = createCollection(collectionView, childElement, getCollectionURI(collectionField, childElement.getName()));
                collection.addCollection(child);
            }
        }

        return collection;
    }

    /**
     * @param collectionField
     * @param topElement
     * @param collection
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws URISyntaxException
     */
    public void addContainedWorks(String collectionField, final String topElement, Collection collection)
            throws PresentationException, IndexUnreachableException, URISyntaxException {
        SolrDocumentList containedWorks = getContainedWorks(createCollectionQuery(collectionField, topElement));
        if(containedWorks != null) {
            for (SolrDocument solrDocument : containedWorks) {
                
                AbstractPresentationModelElement work;
                Boolean anchor = (Boolean)solrDocument.getFirstValue(SolrConstants.ISANCHOR);
                String pi = solrDocument.getFirstValue(SolrConstants.PI).toString();
                URI uri = getManifestURI(pi);
                if(Boolean.TRUE.equals(anchor)) {
                    work = new Collection(uri);
                    work.setViewingHint(ViewingHint.multipart);
                    collection.addCollection((Collection) work);
                } else {
                    work = new Manifest(uri);
                    collection.addManifest((Manifest) work);
                }
                getLabelIfExists(solrDocument).ifPresent(label -> work.setLabel(label));
            }
        }
    }


    /**
     * @param createCollectionQuery
     * @return
     * @throws IndexUnreachableException 
     * @throws PresentationException 
     */
    private SolrDocumentList getContainedWorks(String query) throws PresentationException, IndexUnreachableException {
        return DataManager.getInstance().getSearchIndex().getDocs(query, Arrays.asList(CONTAINED_WORKS_QUERY_FIELDS));
    }

    /**
     * @param collectionField
     * @param topElement
     * @return
     */
    public String createCollectionQuery(String collectionField, final String topElement) {
        String query;
        if(topElement != null) {            
            query = collectionField + ":" + topElement + " OR " + collectionField + ":" + topElement + ".*";
        } else {
            query = collectionField + ":*";
        }
        query = "(" + query + ") AND (ISWORK:true OR ISANCHOR:true)";
        return query;
    }

    /**
     * @param url
     * @param baseElement
     * @return
     * @throws URISyntaxException
     * @throws ContentLibException
     */
    public Collection createCollection(CollectionView collectionView, HierarchicalBrowseDcElement baseElement, URI uri) throws URISyntaxException {
        Collection collection = null;
        try {
            collection = new Collection(uri);
            collection.setAttribution(getAttribution());

            if (baseElement != null) {
                
                collection.setLabel(baseElement.getInfo().getTranslationsForName());

                URI thumbURI = absolutize(baseElement.getInfo().getIconURI());
                if(thumbURI != null) {                    
                    ImageContent thumb = new ImageContent(thumbURI, true);
                    collection.setThumbnail(thumb);
                }

                long volumes = baseElement.getNumberOfVolumes();
                int subCollections = baseElement.getChildren().size();
                collection.addService(new CollectionExtent(subCollections, (int)volumes));
                
                LinkingContent rss = new LinkingContent(absolutize(baseElement.getRssUrl()), new SimpleMetadataValue(RSS_FEED_LABEL));
                collection.addRelated(rss);

                LinkingContent viewer =
                        new LinkingContent(absolutize(collectionView.getCollectionUrl(baseElement)), new SimpleMetadataValue(baseElement.getName()));
                collection.addRendering(viewer);

            } else {
                collection.setViewingHint(ViewingHint.top);
//                collection.addService(new CollectionExtent(collectionView.getVisibleDcElements().size(), 0));
            }

        } catch (URISyntaxException e) {
            logger.error(e.toString(), e);
        }
        return collection;
    }



    /**
     * @param collectionField
     * @param facetField
     * @return
     * @throws IndexUnreachableException
     */
    public CollectionView getCollectionView(String collectionField, final String facetField) throws IndexUnreachableException {

        synchronized (collectionViewMap) {
            if (collectionViewMap.containsKey(collectionField)) {
                return new CollectionView(collectionViewMap.get(collectionField));
            }
        }

        CollectionView view = new CollectionView(collectionField,
                () -> SearchHelper.findAllCollectionsFromField(collectionField, facetField, true, true, true, true));
        view.populateCollectionList();

        synchronized (collectionViewMap) {
            if (collectionViewMap.containsKey(collectionField)) {
                return new CollectionView(collectionViewMap.get(collectionField));
            }
            collectionViewMap.put(collectionField, view);
            return view;
        }
    }


    /**
     * @param collectionField
     * @return
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
            } catch (SolrServerException | IOException e) {
                logger.warn("Unable to query for facet field", e);
                facetField = collectionField;
            }
            facetFieldMap.put(collectionField, facetField);
            return facetField;
        }

    }
}
