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
package de.intranda.digiverso.presentation.model.cms;

import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.cms.itemfunctionality.Functionality;
import de.intranda.digiverso.presentation.model.cms.itemfunctionality.QueryListFunctionality;
import de.intranda.digiverso.presentation.model.cms.itemfunctionality.SearchFunctionality;
import de.intranda.digiverso.presentation.model.cms.itemfunctionality.TocFunctionality;
import de.intranda.digiverso.presentation.model.cms.itemfunctionality.TrivialFunctionality;
import de.intranda.digiverso.presentation.model.glossary.Glossary;
import de.intranda.digiverso.presentation.model.glossary.GlossaryManager;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.CollectionView.BrowseDataProvider;
import de.intranda.digiverso.presentation.servlets.rest.dao.TileGridResource;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;

/**
 * This class represents both template content configuration items and instance items of actual pages. Only the latter are persisted to the DB.
 */
@Entity
@Table(name = "cms_content_items")
public class CMSContentItem implements Comparable<CMSContentItem> {

    /**
     * Separates the individual classifications in the classification string
     */
    private static final String CLASSIFICATION_SEPARATOR = "::";

    /**
     * The different types if content items. The names of these types need to be entered into the cms-template xml files to define the type of content
     * item
     * 
     * @author Florian Alpers
     *
     */
    public enum CMSContentItemType {
        TEXT,
        HTML,
        MEDIA,
        SOLRQUERY,
        PAGELIST,
        COLLECTION,
        TILEGRID,
        TOC,
        RSS,
        SEARCH,
        GLOSSARY,
        COMPONENT;

        /**
         * This method evaluates the text from cms-template xml files to select the correct item type
         * 
         * @param name
         * @return
         */
        public static CMSContentItemType getByName(String name) {
            if (name != null) {
                return CMSContentItemType.valueOf(name.toUpperCase());
            }

            return null;
        }

        /**
         * Returns the required functionality object for this content item
         * 
         * @return
         */
        public Functionality createFunctionality(CMSContentItem item) {
            switch (this) {
                case SOLRQUERY:
                    return new QueryListFunctionality();
                case TOC:
                    return new TocFunctionality(item.getTocPI());
                case SEARCH:
                    return new SearchFunctionality(item.getSearchPrefix(), item.getOwnerPageLanguageVersion().getOwnerPage().getPageUrl());
                default:
                    return new TrivialFunctionality();
            }
        }
    }

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(CMSContentItem.class);

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_content_item_id")
    private Long id;

    /** Item ID from the template. */
    @Column(name = "item_id")
    private String itemId;

    /** Label to display during page creation */
    @Column(name = "item_label")
    private String itemLabel;

    /** Content item type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CMSContentItemType type;

    /** Mandatory items must be filled with actual content in a page. */
    @Column(name = "mandatory", nullable = false)
    private boolean mandatory = false;

    /** Reference to the owning <code>CMSPage</code>. */
    @ManyToOne
    @JoinColumn(name = "owner_page_language_version_id")
    private CMSPageLanguageVersion ownerPageLanguageVersion;

    /** Displayed elements per paginator page (records, pages, etc.). */
    @Column(name = "elements_per_page")
    private int elementsPerPage = 10;

    /** HTML fragment for HTML type content items. */
    @Column(name = "html_fragment", columnDefinition = "LONGTEXT")
    private String htmlFragment;

    /** Solr query definition for content items that list records. */
    @Column(name = "solr_query", columnDefinition = "LONGTEXT")
    private String solrQuery;

    /** Optional list of Solr fields by which to sort the results of <code>solrQuery</code>. */
    @Column(name = "solr_sort_fields")
    private String solrSortFields;

    /** Media item reference for media content items. */
    @JoinColumn(name = "media_item_id")
    private CMSMediaItem mediaItem;

    /** Page classification for page list content items. */
    @Column(name = "page_classification")
    private String pageClassification = "";

    /** Lucence field on which to base a collecion view */
    @Column(name = "collection_field")
    private String collectionField = null;

    /** Number of hierarchy levels from the top which open in their own collection view */
    @Column(name = "collection_base_levels")
    private Integer collectionBaseLevels = 0;

    /** whether this collection should open with all subcollections expanded. Base levels don't expand */
    @Column(name = "collection_open_expanded")
    private boolean collectionOpenExpanded = false;

    /** whether any subcollections opened as base levels should display its parent collections */
    @Column(name = "collection_display_parents")
    private boolean collectionDisplayParents = true;

    /** whether this collection should open with all subcollections expanded. Base levels don't expand */
    @Column(name = "base_collection")
    private String baseCollection = null;

    @Column(name = "toc_pi")
    private String tocPI = "";

    @Column(name = "search_prefix")
    private String searchPrefix;

    @Column(name = "glossary")
    private String glossaryName;

    /**
     * For TileGrid
     */
    @Column(name = "allowed_tags")
    private String allowedTags = "-";

    /**
     * For TileGrid
     */
    @Column(name = "important_count")
    private int numberOfImportantTiles = 0;

    /**
     * For TileGrid
     */
    @Column(name = "tile_count")
    private int numberOfTiles = 9;

    @Column(name = "component")
    private String component = null;

    /**
     * This object may contain item type specific functionality (methods and transient properties)
     * 
     */
    @Transient
    private Functionality functionality = null;

    /**
     * The collection for a collection view item TODO: Migrate this into a Functionality
     */
    @Transient
    private CollectionView collection = null;

    /**
     *  
     */
    @Transient
    private boolean visible = false;

    @Transient
    List<CMSPage> nestedPages = null;

    @Transient
    private int nestedPagesCount = 0;

    @Transient
    private int order = 0;

    /**
     * Noop constructor for javax.persistence
     */
    public CMSContentItem() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Contructs a copy of the given item, inheriting all non-transient properties This is a shallow copy, but all affected properties are either
     * primitives or strings anyway Except mediaItem which is a shared resource
     * 
     * @param blueprint
     */
    public CMSContentItem(CMSContentItem blueprint, CMSPageLanguageVersion owner) {
        if (blueprint.id != null) {
            this.id = blueprint.id;
        }
        this.setItemId(blueprint.itemId);
        this.setItemLabel(blueprint.itemLabel);
        this.setType(blueprint.type);
        this.setMandatory(blueprint.mandatory);
        this.setOrder(blueprint.order);
        this.setHtmlFragment(blueprint.getHtmlFragment());
        this.setElementsPerPage(blueprint.elementsPerPage);
        this.setBaseCollection(blueprint.getBaseCollection());
        this.setCollectionBaseLevels(blueprint.getCollectionBaseLevels());
        this.setCollectionField(blueprint.getCollectionField());
        this.setCollectionOpenExpanded(blueprint.isCollectionOpenExpanded());
        this.setBaseCollection(blueprint.getBaseCollection());
        this.setMediaItem(blueprint.getMediaItem());
        this.setPageClassification(blueprint.getPageClassification());
        this.setComponent(blueprint.component);
        this.setNumberOfTiles(blueprint.numberOfTiles);
        this.setNumberOfImportantTiles(blueprint.numberOfImportantTiles);
        this.allowedTags = blueprint.allowedTags;
        this.glossaryName = blueprint.glossaryName;
        this.searchPrefix = blueprint.searchPrefix;
        this.tocPI = blueprint.tocPI;
        this.collectionDisplayParents = blueprint.collectionDisplayParents;
        this.ownerPageLanguageVersion = owner;
        this.solrQuery = blueprint.solrQuery;
        this.solrSortFields = blueprint.solrSortFields;

    }

    /**
     * @return the functionality
     */
    public Functionality getFunctionality() {
        if (functionality == null) {
            initFunctionality();
        }
        return functionality;
    }

    /**
     * @param type2
     */
    public CMSContentItem(CMSContentItemType type) {
        this.type = type;
    }

    /**
     * Creates the child class providing item-type specific functionality
     */
    public void initFunctionality() {
        this.functionality = getType().createFunctionality(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(CMSContentItem o) {
        if (this == o) {
            return 0;
        }
        if (this.getOrder() != o.getOrder()) {
            return Integer.compare(this.getOrder(), o.getOrder());
        }
        return itemId.compareTo(o.getItemId());
    }

    /**
     * Returns a copy of this object's configuration. Use this to create content item instances of template items for pages.
     *
     * @should clone item correctly
     */
    @Override
    public CMSContentItem clone() {
        CMSContentItem clone = new CMSContentItem(this, null);
        return clone;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the itemId
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * @param itemId the itemId to set
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    /**
     * @return the itemLabel
     */
    public String getItemLabel() {
        if (itemLabel != null && !itemLabel.isEmpty()) {
            return itemLabel;
        }

        return itemId;
    }

    /**
     * @param itemLabel the itemLabel to set
     */
    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
    }

    /**
     * @return the type
     */
    public CMSContentItemType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(CMSContentItemType type) {
        this.type = type;
    }

    /**
     * @return the mandatory
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * @param mandatory the mandatory to set
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * @return the ownerPageLanguageVersion
     */
    public CMSPageLanguageVersion getOwnerPageLanguageVersion() {
        return ownerPageLanguageVersion;
    }

    /**
     * @param ownerPageLanguageVersion the ownerPageLanguageVersion to set
     */
    public void setOwnerPageLanguageVersion(CMSPageLanguageVersion ownerPageLanguageVersion) {
        this.ownerPageLanguageVersion = ownerPageLanguageVersion;
    }

    /**
     * @return the elementsPerPage
     */
    public int getElementsPerPage() {
        return elementsPerPage;
    }

    /**
     * @param elementsPerPage the elementsPerPage to set
     */
    public void setElementsPerPage(int elementsPerPage) {
        this.elementsPerPage = elementsPerPage;
    }

    /**
     * @return the htmlFragment
     */
    public String getHtmlFragment() {
        return htmlFragment;
    }

    /**
     * @param htmlFragment the htmlFragment to set
     */
    public void setHtmlFragment(String htmlFragment) {
        this.htmlFragment = htmlFragment != null ? Normalizer.normalize(htmlFragment, Form.NFC) : "";
        //replace unicode character u2028 (line separator) with java line break, because u2028 breaks mysql 
        this.htmlFragment = this.htmlFragment.replace("" + '\u2028', "\n");
    }

    /**
     * @return the solrQuery
     */
    public String getSolrQuery() {
        if (getType().equals(CMSContentItemType.SEARCH)) {
            return ((SearchFunctionality) getFunctionality()).getQueryString();
        }
        return solrQuery;
    }

    /**
     * @param solrQuery the solrQuery to set
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    /**
     * @return the solrSortFields
     */
    public String getSolrSortFields() {
        if (getType().equals(CMSContentItemType.SEARCH)) {
            return ((SearchFunctionality) getFunctionality()).getSolrSortFields();
        }
        return solrSortFields;
    }

    /**
     * @param solrSortFields the solrSortFields to set
     */
    public void setSolrSortFields(String solrSortFields) {
        this.solrSortFields = solrSortFields;
    }

    /**
     * @return the mediaItem
     */
    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }

    /**
     * @param mediaItem the mediaItem to set
     */
    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    /**
     * @return the pageClassification
     */
    public String[] getPageClassification() {
        if (StringUtils.isNotBlank(pageClassification)) {
            return pageClassification.split(CLASSIFICATION_SEPARATOR);
        } else {
            return new String[0];
        }
    }

    public List<String> getSortedPageClassifications() throws DAOException {
        if (StringUtils.isNotBlank(pageClassification)) {
            SortedMap<Long, String> sortMap = new TreeMap<>();
            for (String classification : getPageClassification()) {
                long order = getNestedPages(classification).stream()
                        .filter(page -> page.getPageSorting() != null)
                        .mapToLong(CMSPage::getPageSorting)
                        .sorted()
                        .findFirst()
                        .orElse(Long.MAX_VALUE);
                while(sortMap.containsKey(order)) {
                    order++;
                }
                sortMap.put(order, classification);
            }
            return new ArrayList<>(sortMap.values());
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * @param pageClassification the pageClassification to set
     */
    public void setPageClassification(String[] pageClassification) {
        if (pageClassification != null && pageClassification.length > 0) {
            this.pageClassification = StringUtils.join(pageClassification, CLASSIFICATION_SEPARATOR);
        } else {
            this.pageClassification = "";
        }
    }

    public int getListPage() {
        return getOwnerPageLanguageVersion().getOwnerPage().getListPage();
    }

    public void setListPage(int listPage) {
        getOwnerPageLanguageVersion().getOwnerPage().setListPage(listPage);
    }

    public int getListOffset() {
        return (getListPage() - 1) * elementsPerPage;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<CMSPage> getNestedPages() throws DAOException {
        if (nestedPages == null) {
            return loadNestedPages();
        }
        return nestedPages;
    }

    public List<CMSPage> getNestedPages(String classification) throws DAOException {
        if (nestedPages == null) {
            return loadNestedPages();
        }
        List<CMSPage> pages =  nestedPages.stream()
                .filter(page -> page.getClassifications() != null && page.getClassifications().contains(classification))
                .collect(Collectors.toList());
        return pages;
    }

    public void resetData() {
        nestedPages = null;
    }

    private List<CMSPage> loadNestedPages() throws DAOException {
        int size = getElementsPerPage();
        int offset = getListOffset();
        
        List<CMSPage> allPages = new ArrayList<>();
        if (StringUtils.isBlank(pageClassification)) {
            allPages = DataManager.getInstance().getDao().getAllCMSPages();
        } else {
            for (String classification : getPageClassification()) {
                if (StringUtils.isNotBlank(classification)) {
                    allPages.addAll(DataManager.getInstance().getDao().getCMSPagesByClassification(classification));
                }
            }
        }
        
        nestedPages = new ArrayList<>();
        int counter = 0;
        Collections.sort(allPages, new CMSPage.PageComparator());
        for (CMSPage cmsPage : allPages) {
            if (cmsPage.isPublished() && !nestedPages.contains(cmsPage)) {
                counter++;
                if (counter > offset && counter <= size + offset) {
                    nestedPages.add(cmsPage);
                }
            }
        }
        setNestedPagesCount((int) Math.ceil(counter / (double) size));
        return nestedPages;
    }

    public int getNestedPagesCount() {
        return nestedPagesCount;
    }

    public void setNestedPagesCount(int nestedPages) {
        this.nestedPagesCount = nestedPages;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getCollectionField() {
        return collectionField;
    }

    public void setCollectionField(String collectionField) {
        this.collectionField = collectionField;
        this.collection = null;
    }

    public Integer getCollectionBaseLevels() {
        return collectionBaseLevels;
    }

    public void setCollectionBaseLevels(Integer collectionBaseLevels) {
        this.collectionBaseLevels = collectionBaseLevels;
        this.collection = null;
    }

    public boolean isCollectionOpenExpanded() {
        return collectionOpenExpanded;
    }

    public void setCollectionOpenExpanded(boolean collectionOpenExpanded) {
        this.collectionOpenExpanded = collectionOpenExpanded;
        this.collection = null;
    }

    /**
     * @return the collectionDisplayParents
     */
    public boolean isCollectionDisplayParents() {
        return collectionDisplayParents;
    }

    /**
     * @param collectionDisplayParents the collectionDisplayParents to set
     */
    public void setCollectionDisplayParents(boolean collectionDisplayParents) {
        this.collectionDisplayParents = collectionDisplayParents;
    }

    /**
     * @return the baseCollection
     */
    public String getBaseCollection() {
        return baseCollection;
    }

    /**
     * @param baseCollection the baseCollection to set
     */
    public void setBaseCollection(String baseCollection) {
        this.baseCollection = baseCollection;
        this.collection = null;
    }

    /**
     * Querys solr for a list of all values of the set collectionField which my serve as a collection
     * 
     * @return
     * @throws IndexUnreachableException
     */
    public List<String> getPossibleBaseCollectionList() throws IndexUnreachableException {
        if (StringUtils.isBlank(collectionField)) {
            return Collections.singletonList("");
        }
        Map<String, Long> dcStrings = SearchHelper.findAllCollectionsFromField(collectionField, collectionField, true, true, true, true);
        List<String> list = new ArrayList<>(dcStrings.keySet());
        list.add(0, "");
        Collections.sort(list);
        return list;
    }

    /**
     * Gets the current collection, creating it if neccessary
     * 
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public CollectionView getCollection() throws PresentationException, IndexUnreachableException {
        if (this.collection == null) {
            this.collection = initializeCollection();
        }
        return this.collection;
    }

    /**
     * Creates a collection view object from the item's collection related properties
     * 
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public CollectionView initializeCollection() throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(getCollectionField())) {
            throw new PresentationException("No solr field provided to create collection view");
        }
        CollectionView collection = initializeCollection(getCollectionField(), getCollectionField());
        collection.setBaseElementName(getBaseCollection());
        collection.setBaseLevels(getCollectionBaseLevels());
        collection.setDisplayParentCollections(isCollectionDisplayParents());
        if (isCollectionOpenExpanded()) {
            collection.setShowAllHierarchyLevels(true);
        }
        collection.populateCollectionList();
        return collection;
    }

    /**
     * Adds a CollecitonView object for the given field to the map and populates its values.
     *
     * @param collectionField
     * @param facetField
     * @param sortField
     */
    private static CollectionView initializeCollection(final String collectionField, final String facetField) {
        CollectionView collection = new CollectionView(collectionField, new BrowseDataProvider() {

            @Override
            public Map<String, Long> getData() throws IndexUnreachableException {
                Map<String, Long> dcStrings = SearchHelper.findAllCollectionsFromField(collectionField, facetField, true, true, true, true);
                return dcStrings;
            }
        });
        return collection;
    }

    /**
     * @return the allowedTags
     */
    public String getAllowedTags() {
        return allowedTags;
    }

    /**
     * @param allowedTags the allowedTags to set
     */
    public void setAllowedTags(String allowedTags) {
        this.allowedTags = allowedTags;
    }

    public String[] getAllowedTagsAsArray() {
        if (StringUtils.isBlank(this.allowedTags)) {
            return new String[0];
        }
        return this.allowedTags.split(TileGridResource.TAG_SEPARATOR_REGEX);
    }

    public void setAllowedTagsAsArray(String[] tags) {
        if (tags == null || tags.length == 0) {
            this.allowedTags = "-";
        }
        this.allowedTags = StringUtils.join(tags, TileGridResource.TAG_SEPARATOR);
    }

    /**
     * @param numberOfImportantTiles the numberOfImportantTiles to set
     */
    public void setNumberOfImportantTiles(int numberOfImportantTiles) {
        this.numberOfImportantTiles = numberOfImportantTiles;
    }

    /**
     * @return the numberOfImportantTiles
     */
    public int getNumberOfImportantTiles() {
        return numberOfImportantTiles;
    }

    /**
     * @return the numberOfTiles
     */
    public int getNumberOfTiles() {
        return numberOfTiles;
    }

    /**
     * @param numberOfTiles the numberOfTiles to set
     */
    public void setNumberOfTiles(int numberOfTiles) {
        this.numberOfTiles = numberOfTiles;
    }

    /**
     * @return the piPeriodical
     */
    public String getTocPI() {
        return tocPI;
    }

    /**
     * @param piPeriodical the piPeriodical to set
     */
    public void setTocPI(String pi) {
        this.tocPI = pi;
        initFunctionality();
    }

    /**
     * @return the searchPrefix
     */
    public String getSearchPrefix() {
        return searchPrefix;
    }

    /**
     * @param searchPrefix the searchPrefix to set
     */
    public void setSearchPrefix(String searchPrefix) {
        this.searchPrefix = searchPrefix;
        initFunctionality();
    }

    @Override
    public String toString() {
        return CMSContentItem.class.getSimpleName() + ": " + getType() + " (" + getItemId() + ")";
    }

    /**
     * Returns the content item mode from the template associated with the owning cmsPage (i.e. The value always reflects the mode for this
     * contentItem in the template xml for this page) Mode offers the ability to allow special options for a content item in some templates (for
     * example for the collection item, the extended mode allows finer control of the way the collection hierarchy is handled)
     * 
     * @return
     */
    public ContentItemMode getMode() {
        return getOwnerPageLanguageVersion().getOwnerPage().getTemplate().getContentItem(getItemId()).getMode();
    }

    /**
     * @return the component
     */
    public String getComponent() {
        return component;
    }

    /**
     * @param component the component to set
     */
    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * @return the glossaryName
     */
    public String getGlossaryName() {
        return glossaryName;
    }

    /**
     * @param glossaryName the glossaryName to set
     */
    public void setGlossaryName(String glossaryName) {
        this.glossaryName = glossaryName;
    }

    public Glossary getGlossary() throws ContentNotFoundException, IOException, ParseException {
        Glossary g = new GlossaryManager().getGlossary(getGlossaryName());
        return g;
    }

}
