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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.CmsElementNotFoundException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.CollectionView.BrowseDataProvider;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;

/**
 * Creates and stored {@link io.goobi.viewer.model.viewer.collections.CollectionView}s for a session.
 *
 * @author florian
 */
@Named
@SessionScoped
public class CollectionViewBean implements Serializable {

    private static final String HIERARCHY_LEVEL_PREFIX = "- ";

    private static final long serialVersionUID = 6707278968715712945L;

    private static final Logger logger = LogManager.getLogger(CollectionViewBean.class);

    /**
     * {@link CollectionView}s mapped to contentItem-Ids of {@link CMSCollectionContent} used to create the CollectionView.
     */
    private Map<String, CollectionView> collections = new HashMap<>();

    /**
     * Solr statistics (in the form of {@link CollectionResult}) mapped to collection names which are in turn mapped to contentItem-Ids because each
     * contentItem may have different statistics for its collections due to different filter queries and excluded subcollections.
     */
    private Map<String, Map<String, CollectionResult>> collectionStatistics = new HashMap<>();

    /**
     * Get the {@link io.goobi.viewer.model.viewer.collections.CollectionView} of the given content item in the given page. If the view hasn't been
     * initialized yet, do so and add it to the Bean's CollectionView map
     *
     * @param content a {@link io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent} instance providing the base data for this
     *            collection
     * @return The CollectionView or null if no matching ContentItem was found
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException
     */
    public CollectionView getCollection(CMSCollectionContent content)
            throws PresentationException, IndexUnreachableException, IllegalRequestException {
        return getCollection(content, content.getCollectionName());
    }

    /**
     * <p>
     * getCollection.
     * </p>
     *
     * @param content a {@link io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent} object
     * @param topVisibleElement a {@link java.lang.String} object
     * @return a {@link io.goobi.viewer.model.viewer.collections.CollectionView} object
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException if any.
     */
    public CollectionView getCollection(CMSCollectionContent content, final String topVisibleElement)
            throws PresentationException, IndexUnreachableException, IllegalRequestException {
        String myId = getCollectionId(content);
        String useTopVisibleElement = topVisibleElement;
        if (StringUtils.isBlank(useTopVisibleElement)) {
            useTopVisibleElement = content.getCollectionName();
        }
        CollectionView collection = collections.get(myId);
        if (collection == null) {
            try {
                collection = initializeCollection(content, useTopVisibleElement);
                collections.put(myId, collection);
            } catch (CmsElementNotFoundException e) {
                logger.debug("Not matching collection element for id {} on page {}", content.getItemId(),
                        Optional.ofNullable(content.getOwningPage()).map(CMSPage::getId).orElse(null));
            }
        } else {
            if (!Objects.equals(collection.getBaseElementName(), useTopVisibleElement)) {
                collection.setBaseElementName(useTopVisibleElement);
                collection.populateCollectionList();
            }

        }
        return collection;
    }

    /**
     * <p>
     * getCollectionId.
     * </p>
     *
     * @param content a {@link io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent} object
     * @return a {@link java.lang.String} object
     */
    public static String getCollectionId(CMSCollectionContent content) {
        String componentId = content.getOwningComponent() == null ? "component" : "component" + content.getOwningComponent().getId();
        if (content.getOwningComponent().getOwningPage() != null) {
            return content.getOwningComponent().getOwningPage().getId() + "_" + componentId + content.getItemId();
        } else if (content.getOwningComponent().getOwningTemplate() != null) {
            return content.getOwningComponent().getOwningTemplate().getId() + "_" + componentId + content.getItemId();
        } else {
            return componentId + "_" + content.getItemId();
        }
    }

    /**
     * <p>
     * getCollectionIfStored.
     * </p>
     *
     * @param content a {@link io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent} object
     * @return a {@link java.util.Optional} object
     */
    public Optional<CollectionView> getCollectionIfStored(CMSCollectionContent content) {
        String myId = getCollectionId(content);
        CollectionView collection = collections.get(myId);
        return Optional.ofNullable(collection);
    }

    /**
     * <p>
     * removeCollection.
     * </p>
     *
     * @param content a {@link io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent} object
     * @return a boolean
     */
    public boolean removeCollection(CMSCollectionContent content) {
        String myId = getCollectionId(content);
        return removeCollection(myId);
    }

    private boolean removeCollection(String myId) {
        this.collectionStatistics.remove(myId);
        return collections.remove(myId) != null;
    }

    /**
     * Creates a collection view object from the item's collection related properties.
     *
     * @param content a {@link io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent} object
     * @param topVisibleElement a {@link java.lang.String} object
     * @return a {@link io.goobi.viewer.model.viewer.collections.CollectionView} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException
     */
    public CollectionView initializeCollection(CMSCollectionContent content, String topVisibleElement)
            throws PresentationException, IllegalRequestException, IndexUnreachableException {
        if (StringUtils.isBlank(content.getSolrField())) {
            throw new PresentationException("No solr field provided to create collection view");
        }
        CollectionView collection = initializeCollection(content);
        collection.setBaseElementName(content.getCollectionName());
        collection.setIgnore(content.getIgnoreCollectionsAsList());
        collection.setIgnoreHierarchy(content.isIgnoreHierarchy());
        collection.setShowAllHierarchyLevels(content.isOpenExpanded());
        collection.populateCollectionList();
        return collection;
    }

    /**
     * Adds a CollecitonView object for the given field to the map and populates its values.
     *
     * @param content
     * @return {@link CollectionView}
     */
    private static CollectionView initializeCollection(final CMSCollectionContent content) {
        // Use FACET_* instead of MD_*, otherwise the hierarchy may be broken
        String useCollectionField = SearchHelper.facetifyField(content.getSolrField());

        BrowseDataProvider provider = new BrowseDataProvider() {
            @Override
            public Map<String, CollectionResult> getData() throws IndexUnreachableException {
                return SearchHelper.findAllCollectionsFromField(useCollectionField, content.getGroupingField(), content.getCombinedFilterQuery(),
                        true, true,
                        DataManager.getInstance().getConfiguration().getCollectionSplittingChar(content.getSolrField()));
            }
        };

        CollectionView collection = new CollectionView(content.getSolrField(), provider);
        String subtheme = Optional.ofNullable(content.getOwningPage()).map(CMSPage::getSubThemeDiscriminatorValue).orElse("");
        if (StringUtils.isNotBlank(subtheme)) {
            try {
                Optional<CMSPage> searchPage = DataManager.getInstance()
                        .getDao()
                        .getCMSPagesForSubtheme(subtheme)
                        .stream()
                        .filter(CMSPage::isPublished)
                        .filter(CMSPage::hasSearchFunctionality)
                        .findFirst();
                searchPage.ifPresent(p -> collection.setSearchUrl(p.getPageUrl()));
            } catch (DAOException e) {
                logger.debug("Error getting subtheme search page: {}", e.toString());
            }
        }
        return collection;
    }

    /**
     * Queries Solr for a list of all values of the set collectionField which my serve as a collection.
     *
     * @param content a {@link io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent} object
     * @param includeSubcollections
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<SelectItem> getPossibleIgnoreCollectionList(CMSCollectionContent content, boolean includeSubcollections)
            throws IndexUnreachableException {
        if (StringUtils.isBlank(content.getSolrField())) {
            return Collections.singletonList(new SelectItem("", ""));
        }
        Locale locale = BeanUtils.getLocale();
        String splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(content.getSolrField());
        Map<String, CollectionResult> dcStrings = getColletionMap(content);
        return dcStrings.keySet()
                .stream()
                .filter(c -> StringUtils.isBlank(content.getCollectionName()) || c.startsWith(content.getCollectionName() + "."))
                .filter(c -> includeSubcollections || getLevelDifference(content.getCollectionName(), c, splittingChar) <= 1)
                .sorted()
                .map(c -> new SelectItem(c, addHierarchyPrefix(c, getLevelDifference(content.getCollectionName(), c, splittingChar) - 1, locale)))
                .collect(Collectors.toList());
    }

    private static String addHierarchyPrefix(String collection, int levelDifference, Locale locale) {
        if (levelDifference <= 0) {
            return ViewerResourceBundle.getTranslation(collection, locale);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < levelDifference; i++) {
            sb.append(HIERARCHY_LEVEL_PREFIX);
        }
        sb.append(ViewerResourceBundle.getTranslation(collection, locale));
        return sb.toString();
    }

    /**
     * Queries Solr for a list of all values of the set collectionField which my serve as a collection.
     *
     * @param content
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<SelectItem> getPossibleBaseCollectionList(CMSCollectionContent content) throws IndexUnreachableException {
        if (StringUtils.isBlank(content.getSolrField())) {
            return Collections.singletonList(new SelectItem("", ""));
        }
        Locale locale = BeanUtils.getLocale();
        String splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(content.getSolrField());
        Map<String, CollectionResult> dcStringMap = content.getColletionMap();
        List<String> list = new ArrayList<>(dcStringMap.keySet());
        list.add(0, "");
        return list.stream()
                .sorted()
                .map(c -> new SelectItem(c, addHierarchyPrefix(c, getLevelDifference("", c, splittingChar) - 1, locale)))
                .toList();
    }

    /**
     * Count the hierarchy level difference between the given collections. Positive return values mean the the second collection has a higher level.
     * Does not consider whether one collection is a child of the other
     * 
     * @param collection1
     * @param collection2
     * @param splittingChar
     * @return an int < 1 if collection2 has a lower hierarchy level than collection1, 0 if both have the same hierarchy level, and an int > 1 if
     *         collection1 has a lower hierarchy level than collection 2.
     */
    private static int getLevelDifference(String collection1, String collection2, String splittingChar) {
        int level1 = CollectionView.getLevel(collection1, splittingChar);
        int level2 = CollectionView.getLevel(collection2, splittingChar);
        return level2 - level1;
    }

    /**
     * <p>
     * getColletionMap.
     * </p>
     *
     * @param content a {@link io.goobi.viewer.model.cms.pages.content.types.CMSCollectionContent} object
     * @return Map&lt;String, CollectionResult&gt;
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     */
    public Map<String, CollectionResult> getColletionMap(CMSCollectionContent content) throws IndexUnreachableException {

        String contentId = getCollectionId(content);
        Map<String, CollectionResult> map = this.collectionStatistics.get(contentId);
        if (map == null) {
            map = SearchHelper.findAllCollectionsFromField(content.getSolrField(), null, content.getCombinedFilterQuery(), true, true,
                    DataManager.getInstance().getConfiguration().getCollectionSplittingChar(content.getSolrField()));
            this.collectionStatistics.put(contentId, map);
        }
        return map;
    }

    /**
     * <p>
     * removeCollectionsForPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object
     */
    public void removeCollectionsForPage(CMSPage page) {
        String idRegex = page.getId() + "_" + "\\w";
        new ArrayList<>(this.collections.keySet()).forEach(id -> {
            if (id.matches(idRegex)) {
                removeCollection(id);
            }
        });

    }

    /**
     * <p>
     * getLoadedCollectionsForPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object
     * @return a {@link java.util.List} object
     */
    public List<CollectionView> getLoadedCollectionsForPage(CMSPage page) {
        String idRegex = page.getId() + "_" + "\\w";
        return new ArrayList<>(this.collections.keySet()).stream()
                .filter(id -> id.matches(idRegex))
                .map(id -> this.collections.get(id))
                .collect(Collectors.toList());
    }

    /**
     * get a list of all {@link io.goobi.viewer.model.viewer.collections.CollectionView}s with the given solr field which are already loaded via
     * {@link #getCollection(CMSCollectionContent)}.
     *
     * @param field The solr field the collection is based on
     * @return a {@link java.util.List} object.
     */
    public List<CollectionView> getCollections(String field) {
        return collections.values().stream().filter(collection -> field.equals(collection.getField())).collect(Collectors.toList());
    }

    /**
     * <p>
     * invalidate.
     * </p>
     */
    public void invalidate() {
        this.collections = new HashMap<>();
        this.collectionStatistics = new HashMap<>();
    }

}
