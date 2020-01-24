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
package io.goobi.viewer.model.iiif.presentation.builder;

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

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.AbstractPresentationModelElement;
import de.intranda.api.iiif.presentation.Collection;
import de.intranda.api.iiif.presentation.CollectionExtent;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import de.intranda.api.iiif.presentation.enums.ViewingHint;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.BrowseElementInfo;
import io.goobi.viewer.model.viewer.CollectionView;
import io.goobi.viewer.model.viewer.HierarchicalBrowseDcElement;
import io.goobi.viewer.model.viewer.SimpleBrowseElementInfo;

/**
 * <p>CollectionBuilder class.</p>
 *
 * @author Florian Alpers
 */
public class CollectionBuilder extends AbstractBuilder {

    private static final Logger logger = LoggerFactory.getLogger(CollectionBuilder.class);

    private static final String[] CONTAINED_WORKS_QUERY_FIELDS =
            { SolrConstants.PI, SolrConstants.ISANCHOR, SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.DOCSTRCT };
    /** Constant <code>RSS_FEED_LABEL="Rss feed"</code> */
    public final static String RSS_FEED_LABEL = "Rss feed";
    /** Constant <code>RSS_FEED_FORMAT="Rss feed"</code> */
    public final static String RSS_FEED_FORMAT = "Rss feed";

    /**
     * Caching for collections
     */
    private static Map<String, String> facetFieldMap = new HashMap<>();
    private static Map<String, CollectionView> collectionViewMap = new HashMap<>();

    /**
     * <p>Constructor for CollectionBuilder.</p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @throws java.net.URISyntaxException if any.
     */
    public CollectionBuilder(HttpServletRequest request) throws URISyntaxException {
        super(request);
    }

    /**
     * <p>Constructor for CollectionBuilder.</p>
     *
     * @param servletUri a {@link java.net.URI} object.
     * @param requestURI a {@link java.net.URI} object.
     */
    public CollectionBuilder(URI servletUri, URI requestURI) {
        super(servletUri, requestURI);
    }

    /**
     * <p>generateCollection.</p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @param splittingChar a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Collection generateCollection(String collectionField, final String topElement, final String splittingChar)
            throws IndexUnreachableException, URISyntaxException, PresentationException, ViewerConfigurationException {

        CollectionView collectionView = getCollectionView(collectionField, getFacetField(collectionField), splittingChar);

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
            Collection parent = createCollection(collectionView, baseElement.getParent(), getCollectionURI(collectionField, parentName));
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
     * <p>addContainedWorks.</p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @param collection a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     */
    public void addContainedWorks(String collectionField, final String topElement, Collection collection)
            throws PresentationException, IndexUnreachableException, URISyntaxException {
        SolrDocumentList containedWorks = getContainedWorks(createCollectionQuery(collectionField, topElement));
        if (containedWorks != null) {
            for (SolrDocument solrDocument : containedWorks) {

                AbstractPresentationModelElement work;
                Boolean anchor = (Boolean) solrDocument.getFirstValue(SolrConstants.ISANCHOR);
                String pi = solrDocument.getFirstValue(SolrConstants.PI).toString();
                URI uri = getManifestURI(pi);
                if (Boolean.TRUE.equals(anchor)) {
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
    private static SolrDocumentList getContainedWorks(String query) throws PresentationException, IndexUnreachableException {
        return DataManager.getInstance().getSearchIndex().getDocs(query, Arrays.asList(CONTAINED_WORKS_QUERY_FIELDS));
    }

    /**
     * <p>createCollectionQuery.</p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param topElement a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String createCollectionQuery(String collectionField, final String topElement) {
        String query;
        if (topElement != null) {
            query = collectionField + ":" + topElement + " OR " + collectionField + ":" + topElement + ".*";
        } else {
            query = collectionField + ":*";
        }
        query = "(" + query + ") AND (ISWORK:true OR ISANCHOR:true)";
        return query;
    }

    /**
     * <p>createCollection.</p>
     *
     * @param baseElement a {@link io.goobi.viewer.model.viewer.HierarchicalBrowseDcElement} object.
     * @param collectionView a {@link io.goobi.viewer.model.viewer.CollectionView} object.
     * @param uri a {@link java.net.URI} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Collection createCollection(CollectionView collectionView, HierarchicalBrowseDcElement baseElement, URI uri)
            throws URISyntaxException, ViewerConfigurationException {
        Collection collection = null;
        try {
            collection = new Collection(uri);
            collection.setAttribution(getAttribution());
            if (baseElement != null) {

                BrowseElementInfo info = baseElement.getInfo();
                if (info != null && (info instanceof SimpleBrowseElementInfo || info instanceof CMSCollection)) {
                    collection.setLabel(info.getTranslationsForName());
                } else {
                    collection.setLabel(ViewerResourceBundle.getTranslations(baseElement.getName()));
                }

                URI thumbURI = absolutize(baseElement.getInfo().getIconURI());
                if (thumbURI != null) {
                    ImageContent thumb = new ImageContent(thumbURI);
                    collection.setThumbnail(thumb);
                    if (IIIFUrlResolver.isIIIFImageUrl(thumbURI.toString())) {
                        URI imageInfoURI = new URI(IIIFUrlResolver.getIIIFImageBaseUrl(thumbURI.toString()));
                        thumb.setService(new ImageInformation(imageInfoURI.toString()));
                    }
                }

                long volumes = baseElement.getNumberOfVolumes();
                int subCollections = baseElement.getChildren().size();
                CollectionExtent extentService = new CollectionExtent(subCollections, (int) volumes);
                extentService.setBaseURI(getBaseUrl().toString().replace("rest", "api"));
                collection.addService(extentService);

                LinkingContent rss =
                        new LinkingContent(absolutize(baseElement.getRssUrl(getRequest().orElse(null))), new SimpleMetadataValue(RSS_FEED_LABEL));
                collection.addRelated(rss);

                //              if(info != null && info.getLinkURI(getRequest().orElse(null)) != null) {
                LinkingContent viewerPage = new LinkingContent(absolutize(collectionView.getCollectionUrl(baseElement)));
                viewerPage.setLabel(new SimpleMetadataValue("goobi viewer"));
                collection.addRendering(viewerPage);
                //          }

                //                LinkingContent viewer =
                //                        new LinkingContent(absolutize(collectionView.getCollectionUrl(baseElement)), new SimpleMetadataValue(baseElement.getName()));
                //                collection.addRendering(viewer);

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
     * <p>getCollectionView.</p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param facetField a {@link java.lang.String} object.
     * @param splittingChar a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.CollectionView} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public CollectionView getCollectionView(String collectionField, final String facetField, final String splittingChar)
            throws IndexUnreachableException {

        synchronized (collectionViewMap) {
            if (collectionViewMap.containsKey(collectionField)) {
                return new CollectionView(collectionViewMap.get(collectionField));
            }
        }

        CollectionView view = new CollectionView(collectionField,
                () -> SearchHelper.findAllCollectionsFromField(collectionField, facetField, null, true, true, splittingChar));
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
     * <p>getFacetField.</p>
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
            } catch (SolrServerException | IOException e) {
                logger.warn("Unable to query for facet field", e);
                facetField = collectionField;
            }
            facetFieldMap.put(collectionField, facetField);
            return facetField;
        }

    }
}
