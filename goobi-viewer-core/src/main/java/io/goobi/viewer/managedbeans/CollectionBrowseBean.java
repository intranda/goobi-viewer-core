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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordDeletedException;
import io.goobi.viewer.exceptions.RecordLimitExceededException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.collections.DynamicCollection;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.collections.BrowseDcElement;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.CollectionView.BrowseDataProvider;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

/**
 * This bean provides the data for collection browsing (the collection hierarchy derived from a Solr field, plus database-defined dynamic
 * collections). Term browsing is handled by {@link TermBrowseBean}.
 */
@Named
@SessionScoped
public class CollectionBrowseBean implements Serializable {

    private static final long serialVersionUID = 3143286096889105848L;

    private static final Logger logger = LogManager.getLogger(CollectionBrowseBean.class);

    /** Pretty URL variable. */
    private String collectionToExpand = null;
    private String topVisibleCollection = null;
    private String targetCollection = null;

    private Map<String, CollectionView> collections = new HashMap<>();
    private String collectionField = SolrConstants.DC;

    /** Cached collection views for database-defined dynamic collections, keyed by collection name. */
    private Map<String, CollectionView> dynamicCollectionViews = new HashMap<>();
    /** Pretty URL variable identifying the dynamic collection to browse. */
    private String dynamicCollectionName = null;

    /**
     * Empty constructor.
     */
    public CollectionBrowseBean() {
        // the emptiness inside
    }

    /**
     * resetAllLists.
     */
    public void resetAllLists() {
        for (Entry<String, CollectionView> entry : collections.entrySet()) {
            entry.getValue().resetCollectionList();
        }
    }

    /**
     * resetDcList.
     */
    public void resetDcList() {
        logger.trace("resetDcList");
        resetList(SolrConstants.DC);
    }

    /**
     * resetList.
     *
     * @param field Solr field name identifying the collection to reset
     */
    public void resetList(String field) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (collections.get(field) != null) {
            collections.get(field).resetCollectionList();
        }
    }

    /**
     * getDcList.
     *
     * @return the dcList (Collections)
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<BrowseDcElement> getDcList() throws IndexUnreachableException {
        return getList(SolrConstants.DC);
    }

    /**
     * getList.
     *
     * @param field Solr field name identifying the collection
     * @return a list of BrowseDcElement objects for all collections in the given Solr field, expanded to unlimited depth
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<BrowseDcElement> getList(String field) throws IndexUnreachableException {
        return getList(field, -1);
    }

    /**
     * getList.
     *
     * @param field Solr field name identifying the collection
     * @param depth maximum hierarchy depth to expand; -1 for unlimited
     * @return a list of BrowseDcElement objects for all visible collections in the given Solr field up to the specified depth
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<BrowseDcElement> getList(String field, int depth) throws IndexUnreachableException {
        logger.trace("getlist: {}", field);
        try {
            if (collections.get(field) == null) {
                initializeCollection(field, null);
                populateCollection(field);
            }
            if (collections.get(field) != null) {
                CollectionView collection = collections.get(field);
                // Loading CMS collection descriptions is expensive, therefore 'false'
                collection.expandAll(depth, false);
                collection.calculateVisibleDcElements(false);
                return new ArrayList<>(collection.getVisibleDcElements());
            }
        } catch (IllegalRequestException e) {
            logger.error(e.toString(), e);
        }

        return Collections.emptyList();
    }

    /**
     * populateCollection.
     *
     * @param field Solr field name identifying the collection to populate
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void populateCollection(String field) throws IndexUnreachableException, IllegalRequestException {
        if (collections.containsKey(field)) {
            collections.get(field).populateCollectionList();
        }
    }

    /**
     * Getter for the field <code>collectionToExpand</code>.
     *
     * @return the name of the collection currently marked for expansion in the view
     */
    public String getCollectionToExpand() {
        synchronized (this) {
            return collectionToExpand;
        }
    }

    /**
     * Setter for the field <code>collectionToExpand</code>.
     *
     * @param collectionToExpand name of the collection to expand in the view
     */
    public void setCollectionToExpand(String collectionToExpand) {
        synchronized (this) {
            this.collectionToExpand = collectionToExpand;
            this.topVisibleCollection = collectionToExpand;
        }
    }

    /**
     * Getter for the field <code>topVisibleCollection</code>.
     *
     * @return the name of the top-level collection currently visible in the collection view, or the collection to expand if not yet set
     */
    public String getTopVisibleCollection() {
        if (topVisibleCollection == null && collectionToExpand != null) {
            return collectionToExpand;
        }
        return topVisibleCollection;
    }

    /**
     * Setter for the field <code>topVisibleCollection</code>.
     *
     * @param topVisibleCollecion name of the top-level collection currently visible in the collection view
     */
    public void setTopVisibleCollection(String topVisibleCollecion) {
        this.topVisibleCollection = topVisibleCollecion;
    }

    /**
     * Use this method if a certain collection needs to be expanded via URL.
     *
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void expandCollection() throws IndexUnreachableException, IllegalRequestException {
        expandCollection(SolrConstants.DC, null);
    }

    /**
     * expandCollection.
     *
     * @param collectionField Solr field name identifying the collection to expand
     * @param facetField Solr field used for grouping or faceting within the collection
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void expandCollection(String collectionField, String facetField) throws IndexUnreachableException, IllegalRequestException {
        synchronized (this) {
            initializeCollection(collectionField, facetField);
            collections.get(collectionField).setBaseElementName(getCollectionToExpand());
            collections.get(collectionField).populateCollectionList();
        }
    }

    /**
     * Getter for the field <code>targetCollection</code>.
     *
     * @return the name of the target collection whose first record should be opened
     */
    public String getTargetCollection() {
        return targetCollection;
    }

    /**
     * Setter for the field <code>targetCollection</code>.
     *
     * @param targetCollection collection name whose first record should be opened
     */
    public void setTargetCollection(String targetCollection) {
        this.targetCollection = targetCollection;
    }

    /**
     * openWorkInTargetCollection.
     *
     * @return the navigation URL to the first record in the target collection, or null if none found
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws RecordDeletedException
     * @throws RecordLimitExceededException
     */
    public String openWorkInTargetCollection()
            throws IndexUnreachableException, PresentationException, RecordDeletedException, DAOException, ViewerConfigurationException,
            RecordLimitExceededException {
        if (StringUtils.isBlank(getTargetCollection())) {
            return null;
        }

        StringPair result =
                SearchHelper.getFirstRecordPiAndPageType(getCollectionField(), getTargetCollection(), true, true,
                        DataManager.getInstance().getConfiguration().getCollectionSplittingChar(getCollectionField()));
        if (result == null) {
            return null;
        }

        try {
            ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
            if (adb != null) {
                adb.setPersistentIdentifier(result.getOne());
                adb.open(); // open to persist PI on ViewManager
            }

            PageType pageType = PageType.getByName(result.getTwo());
            switch (pageType) {
                case viewToc:
                    return "pretty:toc1";
                case viewMetadata:
                    return "pretty:metadata1";
                default:
                    return "pretty:object1";
            }
            // TODO Return and forward to foo URL instead of switch+pretty
        } catch (RecordNotFoundException e) {
            logger.error("No record found for ID: {}", result.getOne());
            return null;
        }
    }

    /**
     * getDcCollection.
     *
     * @return the CollectionView for the DC (Dublin Core) collection field
     */
    public CollectionView getDcCollection() {
        return getCollection(SolrConstants.DC);
    }

    /**
     * getCollection.
     *
     * @param field Solr field name identifying the collection
     * @return the CollectionView for the given Solr field, or null if not initialized
     */
    public CollectionView getCollection(String field) {
        return collections.get(field);
    }

    /**
     *
     * @param field Solr field name identifying the collection
     * @return {@link CollectionView}
     */
    public CollectionView getOrCreateCollection(String field) {
        CollectionView collection = getCollection(field);
        if (collection == null) {
            initializeCollection(field, null);
            collection = getCollection(field);
        }
        return collection;
    }

    /**
     * initializeDCCollection.
     */
    public void initializeDCCollection() {
        initializeCollection(SolrConstants.DC, null);
    }

    public void initializeCollection(final String collectionField) {
        initializeCollection(collectionField, null);
    }

    /**
     * Adds a CollectionView object for the given field to the map and populates its values.
     *
     * @param collectionField Solr field name identifying the collection
     * @param groupingField Solr field used to group collection results; may be null
     */
    public void initializeCollection(final String collectionField, final String groupingField) {
        logger.trace("initializeCollection: {}", collectionField);
        collections.put(collectionField, new CollectionView(collectionField, new BrowseDataProvider() {

            @Override
            public Map<String, CollectionResult> getData() throws IndexUnreachableException {
                return SearchHelper.findAllCollectionsFromField(collectionField, groupingField, null, true, true,
                        DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));
            }
        }));
    }

    /**
     * Getter for the field <code>collectionField</code>.
     *
     * @return the Solr field name used to identify the collection hierarchy
     */
    public String getCollectionField() {
        return collectionField;
    }

    /**
     * Setter for the field <code>collectionField</code>.
     *
     * @param collectionField Solr field name used to identify the collection hierarchy
     */
    public void setCollectionField(String collectionField) {
        this.collectionField = collectionField;
    }

    /**
     * Getter for the field <code>dynamicCollectionName</code>.
     *
     * @return the name of the dynamic collection to browse
     */
    public String getDynamicCollectionName() {
        return dynamicCollectionName;
    }

    /**
     * Setter for the field <code>dynamicCollectionName</code>. Called by the dynamic-collection browse pretty-URL route.
     *
     * @param dynamicCollectionName the name of the dynamic collection to browse
     */
    public void setDynamicCollectionName(String dynamicCollectionName) {
        this.dynamicCollectionName = dynamicCollectionName;
    }

    /**
     * Returns the {@link CollectionView} for the current {@link #dynamicCollectionName}.
     *
     * @return the collection view for the current dynamic collection, or null if none is set
     */
    public CollectionView getDynamicCollectionView() {
        return getDynamicCollectionView(dynamicCollectionName);
    }

    /**
     * Returns (and lazily builds and caches) the {@link CollectionView} for the database-defined dynamic collection with the given name. The view is
     * backed by the collection's stored Solr query used as a filter query over the configured collection field.
     *
     * @param name unique name of the dynamic collection
     * @return the collection view, or null if no such collection exists or it could not be built
     */
    public CollectionView getDynamicCollectionView(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        CollectionView cached = dynamicCollectionViews.get(name);
        if (cached != null) {
            return cached;
        }
        try {
            DynamicCollection collection = DataManager.getInstance().getDao().getDynamicCollection(name);
            if (collection == null) {
                return null;
            }
            final String field = getCollectionField();
            final String query = collection.getSolrQuery();
            CollectionView view = new CollectionView(field, new BrowseDataProvider() {

                @Override
                public Map<String, CollectionResult> getData() throws IndexUnreachableException {
                    return SearchHelper.findAllCollectionsFromField(field, null, query, true, true,
                            DataManager.getInstance().getConfiguration().getCollectionSplittingChar(field));
                }
            });
            view.populateCollectionList();
            view.setCollectionInfo(name, collection);
            dynamicCollectionViews.put(name, view);
            return view;
        } catch (DAOException | IndexUnreachableException | IllegalRequestException e) {
            logger.error("Error building dynamic collection view '{}': {}", name, e.getMessage());
            return null;
        }
    }

    /**
     * Removes the cached collection view for the dynamic collection with the given name, forcing a rebuild on next access.
     *
     * @param name unique name of the dynamic collection
     */
    public void removeDynamicCollectionView(String name) {
        if (name != null) {
            dynamicCollectionViews.remove(name);
        }
    }

    /**
     * TODO translation from DB.
     *
     * @param collectionField Solr field name of the collection
     * @param collectionValue Raw collection value (may be hierarchical)
     * @return {@link String}
     * @should return slash-separated ancestor chain for dot-delimited collection name
     */
    public String getCollectionHierarchy(String collectionField, String collectionValue) {
        logger.trace("getCollectionHierarchy: {}:{}", collectionField, collectionValue);
        if (StringUtils.isEmpty(collectionField) || StringUtils.isEmpty(collectionValue)) {
            return "";
        }
        String separator = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField);
        if (separator.equals(".")) {
            separator = "\\.";
        }
        String[] valueSplit = collectionValue.split(separator);
        if (valueSplit.length == 0) {
            return ViewerResourceBundle.getTranslation(collectionValue, null);
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder sbCollectionName = new StringBuilder();
        for (String value : valueSplit) {
            if (sb.length() > 0) {
                sb.append(" / ");
                sbCollectionName.append('.');
            }
            sbCollectionName.append(value);
            sb.append(ViewerResourceBundle.getTranslation(sbCollectionName.toString(), null));
        }

        return sb.toString();
    }

    /**
     *
     * @param field Collection field name
     * @param value Collection raw name
     * @return Translated collection name
     */
    public String getTranslationForCollectionName(String field, String value) {
        logger.trace("getTranslationForCollectionName: {}:{}", field, value);
        if (field == null || value == null) {
            return null;
        }
        CollectionView collectionView = collections.get(field);
        if (collectionView != null && collectionView.getCompleteList() != null) {
            return collectionView.getTranslationForName(value);
        }

        return null;
    }

    public long getRecordCount(String collectionField, String collectionName) {
        CollectionView view = this.getOrCreateCollection(collectionField);
        return Optional.ofNullable(view.getCollectionElement(collectionName))
                .map(BrowseDcElement::getNumberOfVolumes)
                .orElse(0L);
    }
}
