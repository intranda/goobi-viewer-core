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
package io.goobi.viewer.model.cms;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.itemfunctionality.BookmarksFunktionality;
import io.goobi.viewer.model.cms.itemfunctionality.BrowseFunctionality;
import io.goobi.viewer.model.cms.itemfunctionality.Functionality;
import io.goobi.viewer.model.cms.itemfunctionality.QueryListFunctionality;
import io.goobi.viewer.model.cms.itemfunctionality.SearchFunctionality;
import io.goobi.viewer.model.cms.itemfunctionality.TocFunctionality;
import io.goobi.viewer.model.cms.itemfunctionality.TrivialFunctionality;
import io.goobi.viewer.model.glossary.Glossary;
import io.goobi.viewer.model.glossary.GlossaryManager;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.CollectionView;
import io.goobi.viewer.model.viewer.CollectionView.BrowseDataProvider;

/**
 * This class represents both template content configuration items and instance items of actual pages. Only the latter are persisted to the DB.
 */
@Entity
@Table(name = "cms_content_items")
public class CMSContentItem implements Comparable<CMSContentItem>, CMSMediaHolder {

    private static final String DEFAULT_METADATA_FIELD_SELECTION = "URN,PI,MD_TITLE,DOCSTRCT_TOP";

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
        COMPONENT,
        TAGS,
        METADATA,
        CAMPAIGNOVERVIEW,
        BOOKMARKLISTS,
        BROWSETERMS,
        GEOMAP;

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
                    return new QueryListFunctionality(item.getSearchPrefix(), item.getOwnerPageLanguageVersion().getOwnerPage().getPageUrl());
                case TOC:
                    return new TocFunctionality(item.getTocPI());
                case SEARCH:
                    return new SearchFunctionality(item.getSearchPrefix(), item.getOwnerPageLanguageVersion().getOwnerPage().getPageUrl());
                case BOOKMARKLISTS:
                    return new BookmarksFunktionality();
                case BROWSETERMS:
                    return new BrowseFunctionality(item.getCollectionField());
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

    /** Content item type. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CMSContentItemType type;

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

    /** GeoMap reference for GeoMap content items */
    @JoinColumn(name = "geomap_id")
    private GeoMap geoMap;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cms_content_item_cms_categories", joinColumns = @JoinColumn(name = "content_item_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CMSCategory> categories = new ArrayList<>();

    /** Lucene field on which to base a collection view */
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

    /** Comma separated list of collection names to ignore for display */
    @Column(name = "ignore_collections")
    private String ignoreCollections = null;

    /** Comma separated list of metadata field names to display in overview pages **/
    @Column(name = "metadataFields", columnDefinition = "LONGTEXT")
    private String metadataFields = DEFAULT_METADATA_FIELD_SELECTION;

    @Column(name = "toc_pi")
    private String tocPI = "";

    @Column(name = "search_prefix")
    private String searchPrefix;

    @Column(name = "glossary")
    private String glossaryName;

    /**
     * Name of SOLR field by which to group results of a search or collection
     */
    @Column(name = "group_by")
    private String groupBy = "";

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

    @Column(name = "displayEmptySearchResults")
    private boolean displayEmptySearchResults = false;

    @Column(name = "searchType")
    private int searchType = SearchHelper.SEARCH_TYPE_REGULAR;

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
     * Wrapper for the media item which keeps track of the currently selected language
     */
    @Transient
    private CategorizableTranslatedSelectable<CMSMediaItem> mediaItemWrapper = null;

    @Transient
    private List<Selectable<CMSCategory>> selectableCategories = null;
    /**
     *  
     */
    @Transient
    private boolean visible = false;

    @Transient
    List<CMSPage> nestedPages = null;

    @Transient
    private int nestedPagesCount = 0;

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
     * @param blueprint a {@link io.goobi.viewer.model.cms.CMSContentItem} object.
     * @param owner a {@link io.goobi.viewer.model.cms.CMSPageLanguageVersion} object.
     */
    public CMSContentItem(CMSContentItem blueprint, CMSPageLanguageVersion owner) {
        if (blueprint.id != null) {
            this.id = blueprint.id;
        }
        this.setItemId(blueprint.itemId);
        this.setType(blueprint.type);
        this.setHtmlFragment(blueprint.getHtmlFragment());
        this.setElementsPerPage(blueprint.elementsPerPage);
        this.setBaseCollection(blueprint.getBaseCollection());
        this.setCollectionBaseLevels(blueprint.getCollectionBaseLevels());
        this.setCollectionField(blueprint.getCollectionField());
        this.setCollectionOpenExpanded(blueprint.isCollectionOpenExpanded());
        this.setBaseCollection(blueprint.getBaseCollection());
        this.setMediaItem(blueprint.getMediaItem());
        this.setCategories(new ArrayList<>(blueprint.getCategories()));
        this.setComponent(blueprint.component);
        this.setNumberOfTiles(blueprint.numberOfTiles);
        this.setNumberOfImportantTiles(blueprint.numberOfImportantTiles);
        this.setIgnoreCollections(blueprint.getIgnoreCollections());
        this.glossaryName = blueprint.glossaryName;
        this.searchPrefix = blueprint.searchPrefix;
        this.tocPI = blueprint.tocPI;
        this.collectionDisplayParents = blueprint.collectionDisplayParents;
        this.ownerPageLanguageVersion = owner;
        this.solrQuery = blueprint.solrQuery;
        this.solrSortFields = blueprint.solrSortFields;
        this.setDisplayEmptySearchResults(blueprint.isDisplayEmptySearchResults());
        this.setSearchType(blueprint.getSearchType());
        this.setMetadataFields(blueprint.getMetadataFields());
        this.setGroupBy(blueprint.groupBy);
        this.setGeoMap(blueprint.getGeoMap());

    }

    /**
     * <p>
     * Getter for the field <code>functionality</code>.
     * </p>
     *
     * @return the functionality
     */
    public Functionality getFunctionality() {
        if (functionality == null) {
            initFunctionality();
        }
        return functionality;
    }

    /**
     * <p>
     * Constructor for CMSContentItem.
     * </p>
     *
     * @param type a {@link io.goobi.viewer.model.cms.CMSContentItem.CMSContentItemType} object.
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
    /** {@inheritDoc} */
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
     * {@inheritDoc}
     *
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
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>itemId</code>.
     * </p>
     *
     * @return the itemId
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * <p>
     * Setter for the field <code>itemId</code>.
     * </p>
     *
     * @param itemId the itemId to set
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    /**
     * <p>
     * Getter for the field <code>itemLabel</code>.
     * </p>
     *
     * @return the itemLabel
     */
    public String getItemLabel() {
        return getItemTemplate().getItemLabel();
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public CMSContentItemType getType() {
        return type;
    }

    /**
     * <p>
     * Setter for the field <code>type</code>.
     * </p>
     *
     * @param type the type to set
     */
    public void setType(CMSContentItemType type) {
        this.type = type;
    }

    /**
     * <p>
     * isMandatory.
     * </p>
     *
     * @return the mandatory
     */
    public boolean isMandatory() {
        return getItemTemplate().isMandatory();
    }

    /**
     * <p>
     * Getter for the field <code>ownerPageLanguageVersion</code>.
     * </p>
     *
     * @return the ownerPageLanguageVersion
     */
    public CMSPageLanguageVersion getOwnerPageLanguageVersion() {
        return ownerPageLanguageVersion;
    }

    /**
     * <p>
     * Setter for the field <code>ownerPageLanguageVersion</code>.
     * </p>
     *
     * @param ownerPageLanguageVersion the ownerPageLanguageVersion to set
     */
    public void setOwnerPageLanguageVersion(CMSPageLanguageVersion ownerPageLanguageVersion) {
        this.ownerPageLanguageVersion = ownerPageLanguageVersion;
    }

    /**
     * <p>
     * Getter for the field <code>elementsPerPage</code>.
     * </p>
     *
     * @return the elementsPerPage
     */
    public int getElementsPerPage() {
        return elementsPerPage;
    }

    /**
     * <p>
     * Setter for the field <code>elementsPerPage</code>.
     * </p>
     *
     * @param elementsPerPage the elementsPerPage to set
     */
    public void setElementsPerPage(int elementsPerPage) {
        this.elementsPerPage = elementsPerPage;
    }

    /**
     * <p>
     * Getter for the field <code>htmlFragment</code>.
     * </p>
     *
     * @return the htmlFragment
     */
    public String getHtmlFragment() {
        return htmlFragment;
    }

    /**
     * <p>
     * Setter for the field <code>htmlFragment</code>.
     * </p>
     *
     * @param htmlFragment the htmlFragment to set
     */
    public void setHtmlFragment(String htmlFragment) {
        this.htmlFragment = htmlFragment != null ? Normalizer.normalize(htmlFragment, Form.NFC) : "";
        //replace unicode character u2028 (line separator) with java line break, because u2028 breaks mysql 
        this.htmlFragment = this.htmlFragment.replace("" + '\u2028', "\n");
    }

    /**
     * <p>
     * Getter for the field <code>solrQuery</code>.
     * </p>
     *
     * @return the solrQuery
     */
    public String getSolrQuery() {
        if (getType().equals(CMSContentItemType.SEARCH)) {
            return ((SearchFunctionality) getFunctionality()).getQueryString();
        }
        return solrQuery;
    }

    /**
     * <p>
     * Setter for the field <code>solrQuery</code>.
     * </p>
     *
     * @param solrQuery the solrQuery to set
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    /**
     * <p>
     * Getter for the field <code>solrSortFields</code>.
     * </p>
     *
     * @return the solrSortFields
     */
    public String getSolrSortFields() {
        if (getType().equals(CMSContentItemType.SEARCH)) {
            return ((SearchFunctionality) getFunctionality()).getSortString();
        }
        return solrSortFields;
    }

    /**
     * <p>
     * Setter for the field <code>solrSortFields</code>.
     * </p>
     *
     * @param solrSortFields the solrSortFields to set
     */
    public void setSolrSortFields(String solrSortFields) {
        this.solrSortFields = solrSortFields;
    }

    /** {@inheritDoc} */
    @Override
    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }

    /** {@inheritDoc} */
    @Override
    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
        if (mediaItem != null) {
            this.mediaItemWrapper = new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
            ;
        } else {
            this.mediaItemWrapper = null;
        }

    }

    /** {@inheritDoc} */
    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        return mediaItemWrapper;
    }

    /**
     * <p>
     * Setter for the field <code>mediaItemWrapper</code>.
     * </p>
     *
     * @param mediaItemWrapper the mediaItemWrapper to set
     */
    public void setMediaItemWrapper(CategorizableTranslatedSelectable<CMSMediaItem> mediaItemWrapper) {
        this.mediaItemWrapper = mediaItemWrapper;
        if (mediaItemWrapper != null) {
            this.mediaItem = this.mediaItemWrapper.getValue();
        } else {
            this.mediaItem = null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>categories</code>.
     * </p>
     *
     * @return the pageClassification
     */
    public List<CMSCategory> getCategories() {
        return this.categories;
    }

    /**
     * <p>
     * Setter for the field <code>categories</code>.
     * </p>
     *
     * @param categories a {@link java.util.List} object.
     */
    public void setCategories(List<CMSCategory> categories) {
        this.categories = categories;
    }

    /**
     * <p>
     * addCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     */
    public void addCategory(CMSCategory category) {
        if (!categories.contains(category)) {
            categories.add(category);
        }
    }

    /**
     * <p>
     * removeCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     */
    public void removeCategory(CMSCategory category) {
        categories.remove(category);
    }

    /**
     * <p>
     * getSortedCategories.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCategory> getSortedCategories() throws DAOException {
        if (!this.categories.isEmpty()) {
            SortedMap<Long, CMSCategory> sortMap = new TreeMap<>();
            for (CMSCategory category : getCategories()) {
                long order = getNestedPages(category).stream()
                        .filter(page -> page.getPageSorting() != null)
                        .mapToLong(CMSPage::getPageSorting)
                        .sorted()
                        .findFirst()
                        .orElse(Long.MAX_VALUE);
                while (sortMap.containsKey(order)) {
                    order++;
                }
                sortMap.put(order, category);
            }
            return new ArrayList<>(sortMap.values());
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * getListPage.
     * </p>
     *
     * @return a int.
     */
    public int getListPage() {
        return getOwnerPageLanguageVersion().getOwnerPage().getListPage();
    }

    /**
     * <p>
     * setListPage.
     * </p>
     *
     * @param listPage a int.
     */
    public void setListPage(int listPage) {
        getOwnerPageLanguageVersion().getOwnerPage().setListPage(listPage);
    }

    /**
     * <p>
     * getListOffset.
     * </p>
     *
     * @return a int.
     */
    public int getListOffset() {
        return (getListPage() - 1) * elementsPerPage;
    }

    /**
     * <p>
     * isVisible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * <p>
     * Setter for the field <code>visible</code>.
     * </p>
     *
     * @param visible a boolean.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * <p>
     * Getter for the field <code>nestedPages</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getNestedPages() throws DAOException {
        if (nestedPages == null) {
            return loadNestedPages();
        }
        return nestedPages;
    }

    /**
     * <p>
     * getNestedPagesShuffled.
     * </p>
     *
     * @return nestedPages in a random order
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getNestedPagesShuffled() throws DAOException {
        List<CMSPage> ret = new ArrayList<>(getNestedPages());
        Collections.shuffle(ret);
        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>nestedPages</code>.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getNestedPages(CMSCategory category) throws DAOException {
        if (nestedPages == null) {
            return loadNestedPages();
        }
        List<CMSPage> pages = nestedPages.stream()
                .filter(page -> page.getCategories() != null && page.getCategories().contains(category))
                .collect(Collectors.toList());
        return pages;
    }

    /**
     * <p>
     * resetData.
     * </p>
     */
    public void resetData() {
        nestedPages = null;
        functionality = null;
    }

    private List<CMSPage> loadNestedPages() throws DAOException {
        int size = getElementsPerPage();
        int offset = getListOffset();

        List<CMSPage> allPages = new ArrayList<>();
        if (getCategories().isEmpty()) {
            allPages = DataManager.getInstance().getDao().getAllCMSPages();
        } else {
            for (CMSCategory category : getCategories()) {
                allPages.addAll(DataManager.getInstance().getDao().getCMSPagesByCategory(category));
            }
        }

        nestedPages = new ArrayList<>();
        int counter = 0;
        Collections.sort(allPages, new CMSPage.PageComparator());
        for (CMSPage cmsPage : allPages) {
            if (cmsPage.isPublished() && !nestedPages.contains(cmsPage)) {
                counter++;
                if (!isPaginated()) {
                    nestedPages.add(cmsPage);
                } else if (counter > offset && counter <= size + offset) {
                    nestedPages.add(cmsPage);
                }
            }
        }
        setNestedPagesCount((int) Math.ceil(counter / (double) size));
        return nestedPages;
    }

    /**
     * <p>
     * Getter for the field <code>nestedPagesCount</code>.
     * </p>
     *
     * @return a int.
     */
    public int getNestedPagesCount() {
        return nestedPagesCount;
    }

    /**
     * <p>
     * Setter for the field <code>nestedPagesCount</code>.
     * </p>
     *
     * @param nestedPages a int.
     */
    public void setNestedPagesCount(int nestedPages) {
        this.nestedPagesCount = nestedPages;
    }

    /**
     * <p>
     * Getter for the field <code>order</code>.
     * </p>
     *
     * @return a int.
     */
    public int getOrder() {
        CMSContentItemTemplate template = getItemTemplate();
        if (template != null) {
            return template.getOrder();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * <p>
     * Getter for the field <code>collectionField</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCollectionField() {
        return collectionField;
    }

    /**
     * <p>
     * Setter for the field <code>collectionField</code>.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     */
    public void setCollectionField(String collectionField) {
        this.collectionField = collectionField;
        this.collection = null;
    }

    /**
     * <p>
     * Getter for the field <code>collectionBaseLevels</code>.
     * </p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getCollectionBaseLevels() {
        return collectionBaseLevels;
    }

    /**
     * <p>
     * Setter for the field <code>collectionBaseLevels</code>.
     * </p>
     *
     * @param collectionBaseLevels a {@link java.lang.Integer} object.
     */
    public void setCollectionBaseLevels(Integer collectionBaseLevels) {
        this.collectionBaseLevels = collectionBaseLevels;
        this.collection = null;
    }

    /**
     * <p>
     * isCollectionOpenExpanded.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isCollectionOpenExpanded() {
        return collectionOpenExpanded;
    }

    /**
     * <p>
     * Setter for the field <code>collectionOpenExpanded</code>.
     * </p>
     *
     * @param collectionOpenExpanded a boolean.
     */
    public void setCollectionOpenExpanded(boolean collectionOpenExpanded) {
        this.collectionOpenExpanded = collectionOpenExpanded;
        this.collection = null;
    }

    /**
     * <p>
     * isCollectionDisplayParents.
     * </p>
     *
     * @return the collectionDisplayParents
     */
    public boolean isCollectionDisplayParents() {
        return collectionDisplayParents;
    }

    /**
     * <p>
     * Setter for the field <code>collectionDisplayParents</code>.
     * </p>
     *
     * @param collectionDisplayParents the collectionDisplayParents to set
     */
    public void setCollectionDisplayParents(boolean collectionDisplayParents) {
        this.collectionDisplayParents = collectionDisplayParents;
    }

    /**
     * <p>
     * Getter for the field <code>baseCollection</code>.
     * </p>
     *
     * @return the baseCollection
     */
    public String getBaseCollection() {
        return baseCollection;
    }

    /**
     * <p>
     * Setter for the field <code>baseCollection</code>.
     * </p>
     *
     * @param baseCollection the baseCollection to set
     */
    public void setBaseCollection(String baseCollection) {
        this.baseCollection = baseCollection;
        this.collection = null;
    }

    /**
     * Querys solr for a list of all values of the set collectionField which my serve as a collection
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getPossibleBaseCollectionList() throws IndexUnreachableException {
        if (StringUtils.isBlank(collectionField)) {
            return Collections.singletonList("");
        }
        Map<String, CollectionResult> dcStrings =
                SearchHelper.findAllCollectionsFromField(collectionField, collectionField, getSearchPrefix(), true, true,
                        DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));
        List<String> list = new ArrayList<>(dcStrings.keySet());
        list.add(0, "");
        Collections.sort(list);
        return list;
    }

    /**
     * Querys solr for a list of all values of the set collectionField which my serve as a collection
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getPossibleIgnoreCollectionList() throws IndexUnreachableException {
        if (StringUtils.isBlank(collectionField)) {
            return Collections.singletonList("");
        }
        Map<String, CollectionResult> dcStrings =
                SearchHelper.findAllCollectionsFromField(collectionField, collectionField, getSearchPrefix(), true, true,
                        DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));
        List<String> list = new ArrayList<>(dcStrings.keySet());
        list = list.stream()
                .filter(c -> StringUtils.isBlank(getBaseCollection()) || c.startsWith(getBaseCollection() + "."))
                .filter(c -> StringUtils.isBlank(getBaseCollection()) ? !c.contains(".") : !c.replace(getBaseCollection() + ".", "").contains("."))
                .collect(Collectors.toList());
        //        list.add(0, "");
        Collections.sort(list);
        return list;
    }

    /**
     * Gets the current collection, creating it if neccessary
     *
     * @return a {@link io.goobi.viewer.model.viewer.CollectionView} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException 
     */
    public CollectionView getCollection() throws PresentationException, IndexUnreachableException, IllegalRequestException {
        if (this.collection == null) {
            this.collection = initializeCollection();
        }
        return this.collection;
    }

    public CollectionView initializeCollection() throws PresentationException, IndexUnreachableException, IllegalRequestException {
        return initializeCollection(null);
    }

    /**
     * Creates a collection view object from the item's collection related properties
     *
     * @return a {@link io.goobi.viewer.model.viewer.CollectionView} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException 
     */
    public CollectionView initializeCollection(String subThemeDiscriminatorValue) throws PresentationException, IndexUnreachableException, IllegalRequestException {
        if (StringUtils.isBlank(getCollectionField())) {
            throw new PresentationException("No solr field provided to create collection view");
        }
        CollectionView collection = initializeCollection(getCollectionField(), getCollectionField(), getFilterQuery(subThemeDiscriminatorValue));
        collection.setBaseElementName(getBaseCollection());
        collection.setBaseLevels(getCollectionBaseLevels());
        collection.setDisplayParentCollections(isCollectionDisplayParents());
        collection.setIgnore(getIgnoreCollectionsAsList());
        if (isCollectionOpenExpanded()) {
            collection.setShowAllHierarchyLevels(true);
        }
        collection.populateCollectionList();
        return collection;
    }

    /**
     * @param subThemeDiscriminatorValue
     * @return
     */
    private String getFilterQuery(String subThemeDiscriminatorValue) {
        String searchPrefix = getSearchPrefix();
        if (StringUtils.isNoneBlank(subThemeDiscriminatorValue, searchPrefix)) {
            String filter = "(" + searchPrefix + ") AND " + DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField() + ":"
                    + subThemeDiscriminatorValue;
            return filter;
        } else if (StringUtils.isNotBlank(subThemeDiscriminatorValue)) {
            return DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField() + ":" + subThemeDiscriminatorValue;
        } else {
            return searchPrefix;
        }
    }

    /**
     * Adds a CollecitonView object for the given field to the map and populates its values.
     *
     * @param collectionField
     * @param facetField
     * @param sortField
     * @param filterQuery
     */
    private static CollectionView initializeCollection(final String collectionField, final String facetField, final String filterQuery) {
        CollectionView collection = new CollectionView(collectionField, new BrowseDataProvider() {
            @Override
            public Map<String, CollectionResult> getData() throws IndexUnreachableException {
                Map<String, CollectionResult> dcStrings =
                        SearchHelper.findAllCollectionsFromField(collectionField, facetField, filterQuery, true, true,
                                DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionField));
                return dcStrings;
            }
        });
        return collection;
    }

    /**
     * <p>
     * Setter for the field <code>numberOfImportantTiles</code>.
     * </p>
     *
     * @param numberOfImportantTiles the numberOfImportantTiles to set
     */
    public void setNumberOfImportantTiles(int numberOfImportantTiles) {
        this.numberOfImportantTiles = numberOfImportantTiles;
    }

    /**
     * <p>
     * Getter for the field <code>numberOfImportantTiles</code>.
     * </p>
     *
     * @return the numberOfImportantTiles
     */
    public int getNumberOfImportantTiles() {
        return numberOfImportantTiles;
    }

    /**
     * <p>
     * Getter for the field <code>numberOfTiles</code>.
     * </p>
     *
     * @return the numberOfTiles
     */
    public int getNumberOfTiles() {
        return numberOfTiles;
    }

    /**
     * <p>
     * Setter for the field <code>numberOfTiles</code>.
     * </p>
     *
     * @param numberOfTiles the numberOfTiles to set
     */
    public void setNumberOfTiles(int numberOfTiles) {
        this.numberOfTiles = numberOfTiles;
    }

    /**
     * <p>
     * Getter for the field <code>tocPI</code>.
     * </p>
     *
     * @return the piPeriodical
     */
    public String getTocPI() {
        return tocPI;
    }

    /**
     * <p>
     * Setter for the field <code>tocPI</code>.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     */
    public void setTocPI(String pi) {
        this.tocPI = pi;
        initFunctionality();
    }

    /**
     * <p>
     * Getter for the field <code>searchPrefix</code>.
     * </p>
     *
     * @return the searchPrefix
     */
    public String getSearchPrefix() {
        return searchPrefix;
    }

    /**
     * <p>
     * Setter for the field <code>searchPrefix</code>.
     * </p>
     *
     * @param searchPrefix the searchPrefix to set
     */
    public void setSearchPrefix(String searchPrefix) {
        this.searchPrefix = searchPrefix;
        initFunctionality();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return CMSContentItem.class.getSimpleName() + ": " + getType() + " (" + getItemId() + ")";
    }

    /**
     * Returns the content item mode from the template associated with the owning cmsPage (i.e. The value always reflects the mode for this
     * contentItem in the template xml for this page) Mode offers the ability to allow special options for a content item in some templates (for
     * example for the collection item, the extended mode allows finer control of the way the collection hierarchy is handled)
     *
     * @return a {@link io.goobi.viewer.model.cms.ContentItemMode} object.
     */
    public ContentItemMode getMode() {
        return getItemTemplate().getMode();
    }

    /**
     * Message key to display when clicking the inline help button. Taken from contentItem of template
     *
     * @return a {@link java.lang.String} object.
     */
    public String getInlineHelp() {
        return getItemTemplate().getInlineHelp();
    }

    /**
     * <p>
     * isHasInlineHelp.
     * </p>
     *
     * @return true if the item has a non-empty inline help text. Taken from contentItem of template
     */
    public boolean isHasInlineHelp() {
        CMSContentItem item = getItemTemplate();
        if (item != null) {
            return item.isHasInlineHelp();
        }
        return false;
    }

    /**
     * <p>
     * Getter for the field <code>component</code>.
     * </p>
     *
     * @return the component
     */
    public String getComponent() {
        return component;
    }

    /**
     * <p>
     * Setter for the field <code>component</code>.
     * </p>
     *
     * @param component the component to set
     */
    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * <p>
     * Getter for the field <code>glossaryName</code>.
     * </p>
     *
     * @return the glossaryName
     */
    public String getGlossaryName() {
        return glossaryName;
    }

    /**
     * <p>
     * Setter for the field <code>glossaryName</code>.
     * </p>
     *
     * @param glossaryName the glossaryName to set
     */
    public void setGlossaryName(String glossaryName) {
        this.glossaryName = glossaryName;
    }

    /**
     * <p>
     * getGlossary.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.glossary.Glossary} object.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws java.io.IOException if any.
     * @throws org.json.JSONException if any.
     */
    public Glossary getGlossary() throws ContentNotFoundException, IOException, JSONException {
        Glossary g = new GlossaryManager().getGlossary(getGlossaryName());
        return g;
    }

    /**
     * <p>
     * Getter for the field <code>ignoreCollections</code>.
     * </p>
     *
     * @return the ignoreCollections
     */
    public String getIgnoreCollections() {
        return ignoreCollections;
    }

    /**
     * <p>
     * Setter for the field <code>ignoreCollections</code>.
     * </p>
     *
     * @param ignoreCollections the ignoreCollections to set
     */
    public void setIgnoreCollections(String ignoreCollections) {
        this.ignoreCollections = ignoreCollections;
    }

    /**
     * <p>
     * getIgnoreCollectionsAsList.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getIgnoreCollectionsAsList() {
        if (StringUtils.isNotBlank(ignoreCollections)) {
            List<String> ret = Arrays.stream(ignoreCollections.split(",")).collect(Collectors.toList());
            //            List<String> ret = new ArrayList<>(Arrays.asList(ignoreCollections.split(",")));
            //          List<String> ret = Arrays.asList(ignoreCollections.split(","));

            return ret;
        }

        return new ArrayList<>();
    }

    /**
     * <p>
     * setIgnoreCollectionsAsList.
     * </p>
     *
     * @param toIgnore a {@link java.util.List} object.
     */
    public void setIgnoreCollectionsAsList(List<String> toIgnore) {
        if (toIgnore == null || toIgnore.isEmpty()) {
            this.ignoreCollections = null;
        } else {
            this.ignoreCollections = StringUtils.join(toIgnore, ",");
        }
        this.collection = null;
    }

    /**
     * <p>
     * Getter for the field <code>metadataFields</code>.
     * </p>
     *
     * @return the metadataFields
     */
    public String getMetadataFields() {
        return metadataFields;
    }

    /**
     * <p>
     * Setter for the field <code>metadataFields</code>.
     * </p>
     *
     * @param metadataFields the metadataFields to set
     */
    public void setMetadataFields(String metadataFields) {
        this.metadataFields = metadataFields;
    }

    /**
     * <p>
     * getMetadataFieldsAsList.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getMetadataFieldsAsList() {
        if (StringUtils.isNotBlank(metadataFields)) {
            List<String> ret = Arrays.stream(metadataFields.split(",")).collect(Collectors.toList());
            return ret;
        }
        return new ArrayList<>();
    }

    /**
     * <p>
     * setMetadataFieldsAsList.
     * </p>
     *
     * @param fields a {@link java.util.List} object.
     */
    public void setMetadataFieldsAsList(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            this.metadataFields = null;
        } else {
            this.metadataFields = StringUtils.join(fields, ",");
        }
    }

    /**
     * Get the list of metadata fields which may be displayed. This is the main metadata list
     *
     * @return the main metadata list
     */
    public List<String> getAvailableMetadataFields() {
        return DataManager.getInstance()
                .getConfiguration()
                .getMainMetadataForTemplate(null)
                .stream()
                .map(md -> md.getLabel())
                .map(md -> md.replaceAll("_LANG_.*", ""))
                .distinct()
                .collect(Collectors.toList());

    }

    /**
     * <p>
     * isDisplayEmptySearchResults.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayEmptySearchResults() {
        return this.displayEmptySearchResults;
    }

    /**
     * <p>
     * Setter for the field <code>displayEmptySearchResults</code>.
     * </p>
     *
     * @param displayEmptySearchResults the displayEmptySearchResults to set
     */
    public void setDisplayEmptySearchResults(boolean displayEmptySearchResults) {
        this.displayEmptySearchResults = displayEmptySearchResults;
    }

    /**
     * <p>
     * Getter for the field <code>searchType</code>.
     * </p>
     *
     * @return the searchType
     */
    public int getSearchType() {
        return searchType;
    }

    /**
     * <p>
     * Setter for the field <code>searchType</code>.
     * </p>
     *
     * @param searchType the searchType to set
     */
    public void setSearchType(int searchType) {
        this.searchType = searchType;
    }

    /**
     * <p>
     * setAdvancedSearch.
     * </p>
     *
     * @param advanced a boolean.
     */
    public void setAdvancedSearch(boolean advanced) {
        if (advanced) {
            this.searchType = SearchHelper.SEARCH_TYPE_ADVANCED;
        } else {
            this.searchType = SearchHelper.SEARCH_TYPE_REGULAR;
        }
    }

    /**
     * <p>
     * isAdvancedSearch.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isAdvancedSearch() {
        return SearchHelper.SEARCH_TYPE_ADVANCED == this.searchType;
    }

    /** {@inheritDoc} */
    @Override
    public String getMediaFilter() {
        CMSContentItemTemplate template = getItemTemplate();
        return template.getMediaFilter();
    }

    /**
     * @return
     */
    public CMSContentItemTemplate getItemTemplate() {
        try {
            return getOwnerPageLanguageVersion().getOwnerPage().getTemplate().getContentItem(getItemId());
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * @return true if this contentItem should only appear in a preview of this page
     */
    public boolean isPreview() {
        return getItemTemplate().isPreview();
    }

    /**
     * Retrieve all categories fresh from the DAO and write them to this depending on the state of the selectableCategories list. Saving the
     * categories from selectableCategories directly leads to ConcurrentModificationexception when persisting page
     */
    public void writeSelectableCategories() {

        if (this.selectableCategories != null) {
            try {
                List<CMSCategory> allCats = DataManager.getInstance().getDao().getAllCategories();
                List<CMSCategory> tempCats = new ArrayList<>();
                for (CMSCategory cat : allCats) {
                    if (this.categories.contains(cat) && this.selectableCategories.stream().noneMatch(s -> s.getValue().equals(cat))) {
                        tempCats.add(cat);
                    } else if (this.selectableCategories.stream().anyMatch(s -> s.getValue().equals(cat) && s.isSelected())) {
                        tempCats.add(cat);
                    }
                }
                this.categories = tempCats;
            } catch (DAOException e) {
                logger.error(e.toString(), e);
            }
        }
        this.selectableCategories = null;
    }

    /**
     * <p>
     * Getter for the field <code>selectableCategories</code>.
     * </p>
     *
     * @return the selectableCategories
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Selectable<CMSCategory>> getSelectableCategories() throws DAOException {
        if (selectableCategories == null) {
            List<CMSCategory> allowedCategories = BeanUtils.getCmsBean().getAllowedCategories(BeanUtils.getUserBean().getUser());
            selectableCategories =
                    allowedCategories.stream().map(cat -> new Selectable<>(cat, this.categories.contains(cat))).collect(Collectors.toList());
        }
        return selectableCategories;
    }

    /**
     * Writes HTML fragment value as file for re-indexing. HTML/text fragments are exported directly. Attached media items are exported as long as
     * their content type is one of the supported text document formats.
     *
     * @param pageId ID of the owning CMS page
     * @param outputFolderPath a {@link java.lang.String} object.
     * @param namingScheme a {@link java.lang.String} object.
     * @return Exported Files
     * @should write files correctly
     * @throws java.io.IOException if any.
     */
    public List<File> exportHtmlFragment(long pageId, String outputFolderPath, String namingScheme) throws IOException {
        if (StringUtils.isEmpty(outputFolderPath)) {
            throw new IllegalArgumentException("hotfolderPath may not be null or emptys");
        }
        if (StringUtils.isEmpty(namingScheme)) {
            throw new IllegalArgumentException("namingScheme may not be null or empty");
        }
        if (StringUtils.isEmpty(htmlFragment) && mediaItem == null) {
            return Collections.emptyList();
        }

        List<File> ret = new ArrayList<>(2);
        Path cmsDataDir = Paths.get(outputFolderPath, namingScheme + IndexerTools.SUFFIX_CMS);
        if (!Files.isDirectory(cmsDataDir)) {
            Files.createDirectory(cmsDataDir);
            logger.trace("Created overview page subdirectory: {}", cmsDataDir.toAbsolutePath().toString());
        }
        // Export HTML fragment
        if (StringUtils.isNotEmpty(htmlFragment)) {
            File file = new File(cmsDataDir.toFile(), pageId + "-" + itemId + ".xml");
            try {
                FileUtils.writeStringToFile(file, htmlFragment, StringTools.DEFAULT_ENCODING);
                logger.debug("Wrote HTML fragment: {}", file.getName());
                ret.add(file);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        // Export media item HTML content
        if (mediaItem != null && mediaItem.isHasExportableText()) {
            String html = null;
            try {
                html = CmsMediaBean.getMediaFileAsString(mediaItem);
            } catch (ViewerConfigurationException e) {
                logger.error(e.getMessage(), e);
            }
            if (StringUtils.isNotEmpty(html)) {
                File file = new File(cmsDataDir.toFile(), pageId + "-" + itemId + ".html");
                FileUtils.writeStringToFile(file, html, StringTools.DEFAULT_ENCODING);
                logger.debug("Wrote media content: {}", file.getName());
                ret.add(file);
            }
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#hasMediaItem()
     */
    /** {@inheritDoc} */
    @Override
    public boolean hasMediaItem() {
        return this.mediaItem != null;
    }

    /**
     * @param groupBy the {@link #groupBy} to set
     */
    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    /**
     * @return the {@link #groupBy}
     */
    public String getGroupBy() {
        return groupBy;
    }

    /**
     * 
     * @return true if {@link #groupBy} is not blank an grouping should therefore be done
     */
    public boolean isGroupBySelected() {
        return StringUtils.isNotBlank(this.groupBy);
    }

    /**
     * @return the geoMap
     */
    public GeoMap getGeoMap() {
        return geoMap;
    }

    /**
     * @param geoMap the geoMap to set
     */
    public void setGeoMap(GeoMap geoMap) {
        this.geoMap = geoMap;
    }

    public Long getGeoMapId() {
        if (this.geoMap == null) {
            return null;
        } else {
            return this.geoMap.getId();
        }
    }

    public void setGeoMapId(Long id) throws DAOException {
        this.geoMap = DataManager.getInstance().getDao().getGeoMap(id);
    }

    public boolean isPaginated() {
        return ContentItemMode.paginated.equals(getMode());
    }

}
