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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.model.tasks.Task;
import io.goobi.viewer.api.rest.model.tasks.Task.TaskType;
import io.goobi.viewer.api.rest.model.tasks.TaskParameter;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.cms.itemfunctionality.SearchFunctionality;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.maps.GeoMap.GeoMapType;
import io.goobi.viewer.model.maps.Location;
import io.goobi.viewer.model.search.AdvancedSearchFieldConfiguration;
import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.search.FacetItem;
import io.goobi.viewer.model.search.IFacetItem;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchFilter;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.search.SearchInterface;
import io.goobi.viewer.model.search.SearchQueryGroup;
import io.goobi.viewer.model.search.SearchQueryItem;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;
import io.goobi.viewer.model.viewer.BrowseDcElement;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

/**
 * SearchBean
 */
@Named
@SessionScoped
public class SearchBean implements SearchInterface, Serializable {

    private static final long serialVersionUID = 6962223613432267768L;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SearchBean.class);

    /** Constant <code>URL_ENCODING="UTF8"</code> */
    public static final String URL_ENCODING = "UTF8";

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private BreadcrumbBean breadcrumbBean;
    @Inject
    private UserBean userBean;

    /** Max number of search hits to be displayed on one page. */
    private int hitsPerPage = DataManager.getInstance().getConfiguration().getSearchHitsPerPageDefaultValue();
    /**
     * Currently selected search type (regular, advanced, timeline, ...). This property is not private so it can be altered in unit tests (the setter
     * checks the config and may prevent setting certain values).
     */
    int activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
    /** Currently selected filter for the regular search. Possible values can be configured. */
    private SearchFilter currentSearchFilter = SearchHelper.SEARCH_FILTER_ALL;
    /** Solr query generated from the user's input (does not include facet filters or blacklists). */
    private String searchStringInternal = "";
    /** User-entered search query that is displayed in the search field after the search. */
    private String searchString = "";
    /** Optional custom filter query. */
    private String customFilterQuery = null;
    /** Individual terms extracted from the user query (used for highlighting). */
    private Map<String, Set<String>> searchTerms = new HashMap<>();

    private boolean phraseSearch = false;
    /** Current search result page. */
    private int currentPage = 1;
    /** Index of the currently open search result (used for search result browsing). */
    int currentHitIndex = -1;
    /** Number by which currentHitIndex shall be increased or decreased. */
    private int hitIndexOperand = 0;
    private SearchFacets facets = new SearchFacets();
    /** User-selected Solr field name by which the search results shall be sorted. A leading exclamation mark means descending sorting. */
    private String sortString = "";
    /** Keep lists of select values, once generated, for performance reasons. */
    private final Map<String, List<StringPair>> advancedSearchSelectItems = new HashMap<>();
    /** Groups of query item clusters for the advanced search. */
    private final List<SearchQueryGroup> advancedQueryGroups = new ArrayList<>();

    private int advancedSearchGroupOperator = 0;
    /** Human-readable representation of the advanced search query for displaying. */
    private String advancedSearchQueryInfo;

    private String searchInCurrentItemString;
    /** Current search object. Contains the results and can be used to persist search parameters in the DB. */
    private Search currentSearch;

    private volatile FutureTask<Boolean> downloadReady;
    private volatile FutureTask<Boolean> downloadComplete;

    /**
     * Whether to only display the current search parameters rather than the full input mask
     */
    private boolean showReducedSearchOptions = false;
    /** Reusable Random object. */
    private Random random = new SecureRandom();

    /**
     * Empty constructor.
     */
    public SearchBean() {
        // the emptiness inside
    }

    /**
     * <p>
     * init.
     * </p>
     */
    @PostConstruct
    public void init() {
        resetAdvancedSearchParameters(1, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
    }

    /**
     * Required setter for ManagedProperty injection
     *
     * @param navigationHelper the navigationHelper to set
     */
    public void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    /**
     * <p>
     * clearSearchItemLists.
     * </p>
     */
    public void clearSearchItemLists() {
        advancedSearchSelectItems.clear();
    }

    /**
     * Dummy method for component cross-compatibility with CMS searches.
     * 
     * @param subtheme
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public String search(String subtheme) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return search();
    }

    /**
     * Executes the search using already set parameters. Usually called from Pretty URLs.
     *
     * @return {@link java.lang.String} null
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String search() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("search");
        if (breadcrumbBean != null) {
            breadcrumbBean.updateBreadcrumbsForSearchHits(StringTools.decodeUrl(facets.getCurrentFacetString()));
        }
        resetSearchResults();
        executeSearch();

        return "";
    }

    /**
     * {@inheritDoc}
     *
     * Action method for search buttons (simple search).
     * 
     * @should not reset facets
     */
    @Override
    public String searchSimple() {
        return searchSimple(true, false);
    }

    /**
     * Action method for search buttons (simple search) with an option to reset search parameters.
     *
     * @param resetParameters a boolean.
     * @return Target URL
     */
    public String searchSimple(boolean resetParameters) {
        return searchSimple(resetParameters, true);
    }

    /**
     * Action method for search buttons (simple search) with an option to reset search parameters and active facets.
     *
     * @param resetParameters a boolean.
     * @param resetFacets a boolean.
     * @return Target URL
     * @should not reset facets if resetFacets false
     */
    public String searchSimple(boolean resetParameters, boolean resetFacets) {
        logger.trace("searchSimple");
        resetSearchResults();
        if (resetParameters) {
            resetSearchParameters();
            facets.resetSliderRange();
        }
        if (resetFacets) {
            facets.resetCurrentFacetString();
        }
        generateSimpleSearchString(searchString);
        return "pretty:newSearch5";
    }

    /**
     * Same as <code>searchSimple()</code> but resets the current facets.
     *
     * @return a {@link java.lang.String} object.
     */
    public String searchSimpleResetCollections() {
        facets.resetCurrentFacetString();
        return searchSimple(true, true);
    }

    /**
     * Same as <code>{@link #searchSimple()}</code> but sets the current facets to the given string
     *
     * @param facetString a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String searchSimpleSetFacets(String facetString) {
        facets.resetCurrentFacetString();
        facets.setCurrentFacetString(facetString);
        return searchSimple(true, false);
    }

    /** {@inheritDoc} */
    @Override
    public String searchAdvanced() {
        return searchAdvanced(true);
    }

    /**
     * <p>
     * searchAdvanced.
     * </p>
     *
     * @param resetParameters a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String searchAdvanced(boolean resetParameters) {
        logger.trace("searchAdvanced");
        if (breadcrumbBean != null) {
            breadcrumbBean.updateBreadcrumbsForSearchHits(StringTools.decodeUrl(facets.getCurrentFacetString()));
        }
        resetSearchResults();
        if (resetParameters) {
            resetSearchParameters();
            facets.resetSliderRange();
        }
        searchStringInternal = generateAdvancedSearchString(DataManager.getInstance().getConfiguration().isAggregateHits());

        return "pretty:searchAdvanced5";
    }

    /**
     * Search using currently set search string
     *
     * @return a {@link java.lang.String} object.
     */
    public String searchDirect() {
        logger.trace("searchDirect");
        resetSearchResults();
        facets.resetCurrentFacetString();
        return "pretty:newSearch5";
    }

    /**
     * Action method for the "reset" button in search forms.
     *
     * @should return correct Pretty URL ID
     * @return a {@link java.lang.String} object.
     */
    public String resetSearchAction() {
        logger.trace("resetSearchAction");
        generateSimpleSearchString("");
        setCurrentPage(1);
        setExactSearchString("");
        mirrorAdvancedSearchCurrentHierarchicalFacets();
        resetSearchResults();
        resetSearchParameters(true);
        searchInCurrentItemString = null;
        customFilterQuery = null;

        // After resetting, return to the correct search entry page
        switch (activeSearchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return "pretty:" + PageType.advancedSearch.name();
            case SearchHelper.SEARCH_TYPE_CALENDAR:
                return "pretty:" + PageType.searchCalendar.name();
            default:
                return "pretty:" + PageType.search.name();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Alias for {@link #resetSearchAction()}
     */
    @Override
    public String resetSearch() {
        return resetSearchAction();
    }

    /**
     * Resets variables that hold search result data. Does not reset search parameter variables such as type, filter or collection.
     */
    public void resetSearchResults() {
        logger.trace("resetSearchResults");
        currentHitIndex = -1;
        if (currentSearch != null) {
            currentSearch.setHitsCount(0);
            currentSearch.getHits().clear();
            currentSearch = null; //to indicate that no search results are expected. search is initially null anyway
        }
        // Only reset available facets here, not selected facets!
        facets.resetAvailableFacets();

        // Reset preferred record view when doing a new search
        if (navigationHelper != null) {
            navigationHelper.setPreferredView(null);
        }
    }

    /**
     * Resets general search options and type specific options for currently unused types.
     */
    public void resetSearchParameters() {
        resetSearchParameters(false);
    }

    /**
     * Resets general search options and type specific options for currently unused types (all options if <resetAll> is true).
     *
     * @param resetAll If true, parameters for the currently used search type are also reset.
     */
    public void resetSearchParameters(boolean resetAll) {
        logger.trace("resetSearchParameters");
        CalendarBean calendarBean = BeanUtils.getCalendarBean();
        if (resetAll) {
            resetSimpleSearchParameters();
            resetAdvancedSearchParameters(1, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
            if (calendarBean != null) {
                calendarBean.resetCurrentSelection();
            }
        } else {
            switch (activeSearchType) {
                case 0:
                    resetAdvancedSearchParameters(1, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
                    if (calendarBean != null) {
                        calendarBean.resetCurrentSelection();
                    }
                    break;
                case 1:
                    resetSimpleSearchParameters();
                    if (calendarBean != null) {
                        calendarBean.resetCurrentSelection();
                    }
                    break;
                case 2:
                    resetSimpleSearchParameters();
                    resetAdvancedSearchParameters(1, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
                    break;
                case 3:
                    resetSimpleSearchParameters();
                    resetAdvancedSearchParameters(1, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
                    break;
                default: // nothing
            }
        }
        setSortString("");
    }

    /**
     * Resets search options for the simple search.
     *
     * @should reset variables correctly
     */
    protected void resetSimpleSearchParameters() {
        logger.trace("resetSimpleSearchParameters");
        currentSearchFilter = SearchHelper.SEARCH_FILTER_ALL;
        generateSimpleSearchString("");
        setCurrentPage(1);

        searchString = "";
    }

    /**
     * Resets search options for the advanced search.
     *
     * @param initialGroupNumber a int.
     * @param initialItemNumber a int.
     * @should reset variables correctly
     * @should re-select collection correctly
     */
    protected void resetAdvancedSearchParameters(int initialGroupNumber, int initialItemNumber) {
        logger.trace("resetAdvancedSearchParameters");
        advancedSearchGroupOperator = 0;
        advancedQueryGroups.clear();
        for (int i = 0; i < initialGroupNumber; ++i) {
            advancedQueryGroups.add(new SearchQueryGroup(BeanUtils.getLocale(), initialItemNumber));
        }
        // If currentCollection is set, pre-select it in the advanced search menu
        mirrorAdvancedSearchCurrentHierarchicalFacets();
    }

    /**
     * "Setter" for resetting the query item list via a f:setPropertyActionListener.
     *
     * @param reset a boolean.
     */
    public void setAdvancedQueryItemsReset(boolean reset) {
        logger.trace("setAdvancedQueryItemsReset");
        if (reset) {
            resetAdvancedSearchParameters(1, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
        }
    }

    /**
     * Generates a Solr query string out of advancedQueryItems (does not contains facets or blacklists).
     * 
     * @param aggregateHits
     * @return
     * @throws IndexUnreachableException
     * @should construct query correctly
     * @should construct query info correctly
     * @should add multiple facets for the same field correctly
     * @should add multiple facets for the same field correctly if field already in current facets
     * @should only add identical facets once
     * @should not add more facets if field value combo already in current facets
     * @should not replace obsolete facets with duplicates
     * @should remove facets that are not matched among query items
     */
    String generateAdvancedSearchString(boolean aggregateHits) {
        logger.trace("generateAdvancedSearchString");
        StringBuilder sb = new StringBuilder();
        StringBuilder sbInfo = new StringBuilder();

        searchTerms.clear();
        StringBuilder sbCurrentCollection = new StringBuilder();
        //        String currentFacetString = facets.getCurrentFacetStringPrefix(false);

        for (SearchQueryGroup queryGroup : advancedQueryGroups) {
            StringBuilder sbGroup = new StringBuilder();
            if (sb.length() > 0) {
                switch (advancedSearchGroupOperator) {
                    case 0:
                        sbInfo.append(' ')
                                .append(ViewerResourceBundle.getTranslation("searchOperator_AND", BeanUtils.getLocale()))
                                .append("\n<br />");
                        break;
                    case 1:
                        sbInfo.append(' ').append(ViewerResourceBundle.getTranslation("searchOperator_OR", BeanUtils.getLocale())).append("\n<br />");
                        break;
                    default:
                        sbInfo.append(' ')
                                .append(ViewerResourceBundle.getTranslation("searchOperator_AND", BeanUtils.getLocale()).toUpperCase())
                                .append("\n<br />");
                        break;
                }
            }
            sbInfo.append('(');

            Set<String> usedHierarchicalFields = new HashSet<>();
            Set<String> usedFieldValuePairs = new HashSet<>();
            for (SearchQueryItem queryItem : queryGroup.getQueryItems()) {
                // logger.trace("Query item: {}", queryItem.toString());
                if (StringUtils.isEmpty(queryItem.getField()) || StringUtils.isBlank(queryItem.getValue())) {
                    continue;
                }
                if (!sbInfo.toString().endsWith("(")) {
                    sbInfo.append(' ')
                            .append(ViewerResourceBundle.getTranslation("searchOperator_" + queryGroup.getOperator().name(), BeanUtils.getLocale()))
                            .append(' ');
                }
                // Generate the hierarchical facet parameter from query items
                if (queryItem.isHierarchical()) {
                    // logger.trace("{} is hierarchical", queryItem.getField());
                    // Skip identical hierarchical items

                    // Find existing facet items that can be repurposed for the existing facets
                    boolean skipQueryItem = false;
                    for (IFacetItem facetItem : facets.getCurrentFacets()) {
                        // logger.trace("checking facet item: {}", facetItem.getLink());
                        if (!facetItem.getField().equals(queryItem.getField())) {
                            continue;
                        }
                        if (usedFieldValuePairs.contains(facetItem.getLink())) {
                            // logger.trace("facet item already handled: {}", facetItem.getLink());
                            continue;
                        }
                        if (!usedFieldValuePairs.contains(queryItem.getField() + ":" + queryItem.getValue())) {
                            facetItem.setLink(queryItem.getField() + ":" + queryItem.getValue());
                            usedFieldValuePairs.add(facetItem.getLink());
                            usedHierarchicalFields.add(queryItem.getField());
                            // logger.trace("reuse facet item: {}", facetItem);
                            skipQueryItem = true;
                            break;
                        }
                    }
                    if (skipQueryItem) {
                        continue;
                    }

                    String itemQuery =
                            new StringBuilder().append(queryItem.getField()).append(':').append(queryItem.getValue().trim()).toString();
                    // logger.trace("item query: {}", itemQuery);

                    // Check whether this combination already exists and skip, if that's the case
                    if (usedFieldValuePairs.contains(itemQuery)) {
                        // logger.trace("facet item already exists: {}", itemQuery);
                        continue;
                    }
                    usedFieldValuePairs.add(itemQuery);
                    usedHierarchicalFields.add(queryItem.getField());

                    sbCurrentCollection.append(itemQuery + ";;");

                    sbInfo.append(ViewerResourceBundle.getTranslation(queryItem.getField(), BeanUtils.getLocale()))
                            .append(": \"")
                            .append(ViewerResourceBundle.getTranslation(queryItem.getValue(), BeanUtils.getLocale()))
                            .append('"');
                    continue;
                }

                // Non-hierarchical fields
                if (searchTerms.get(SolrConstants.FULLTEXT) == null) {
                    searchTerms.put(SolrConstants.FULLTEXT, new HashSet<String>());
                }

                String itemQuery = null;
                if (SolrConstants.BOOKMARKS.equals(queryItem.getField())) {

                    // Bookmark list search
                    if (StringUtils.isEmpty(queryItem.getValue())) {
                        continue;
                    }

                    String key = getBookmarkListSharedKey();
                    String name = getBookmarkListName();

                    if (StringUtils.isNotBlank(key)) {
                        try {
                            BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkListByShareKey(key);
                            if (bookmarkList != null) {
                                queryItem.setValue(bookmarkList.getName());
                                itemQuery = bookmarkList.getFilterQuery();
                            }
                        } catch (DAOException e) {
                            logger.error(e.toString(), e);
                        }
                    } else if (StringUtils.isNotBlank(name) && !"session".equals(name)) {
                        try {
                            BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkList(name, null);
                            if (bookmarkList != null) {
                                queryItem.setValue(bookmarkList.getName());
                                itemQuery = bookmarkList.getFilterQuery();
                            }
                        } catch (DAOException e) {
                            logger.error(e.toString(), e);
                        }
                    } else if (userBean.isLoggedIn()) {
                        // User bookmark list
                        try {
                            BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkList(queryItem.getValue(), userBean.getUser());
                            if (bookmarkList != null) {
                                itemQuery = bookmarkList.getFilterQuery();
                            }
                        } catch (DAOException e) {
                            logger.error(e.getMessage(), e);
                        }
                    } else {
                        // Session bookmark list
                        Optional<BookmarkList> obs =
                                DataManager.getInstance().getBookmarkManager().getBookmarkList(BeanUtils.getRequest().getSession());
                        if (obs.isPresent()) {
                            itemQuery = obs.get().getFilterQuery();
                        }
                    }
                    if (StringUtils.isEmpty(itemQuery)) {
                        // Skip empty bookmark list
                        continue;
                    }
                } else {
                    // Generate item query
                    itemQuery = queryItem.generateQuery(searchTerms.get(SolrConstants.FULLTEXT), aggregateHits);
                }

                logger.trace("Item query: {}", itemQuery);
                sbInfo.append(ViewerResourceBundle.getTranslation(queryItem.getField(), BeanUtils.getLocale())).append(": ");
                switch (queryItem.getOperator()) {
                    case IS:
                    case PHRASE:
                        if (!queryItem.getValue().startsWith("\"")) {
                            sbInfo.append('"');
                        }
                        if (SolrConstants.BOOKMARKS.equals(queryItem.getField()) && !userBean.isLoggedIn()) {
                            // Session bookmark list value
                            sbInfo.append(ViewerResourceBundle.getTranslation("bookmarkList_session", BeanUtils.getLocale()));
                        } else {
                            sbInfo.append(ViewerResourceBundle.getTranslation(queryItem.getValue(), BeanUtils.getLocale()));
                        }
                        if (!queryItem.getValue().endsWith("\"")) {
                            sbInfo.append('"');
                        }
                        break;
                    default:
                        if (queryItem.isRange()) {
                            sbInfo.append('[').append(queryItem.getValue()).append(" - ").append(queryItem.getValue2()).append(']');
                        } else {
                            sbInfo.append(ViewerResourceBundle.getTranslation(queryItem.getValue(), BeanUtils.getLocale()));
                        }
                }

                // Add item query part to the group query
                if (itemQuery.length() > 0) {
                    if (sbGroup.length() > 0) {
                        // If this is not the first item, add the group's operator
                        sbGroup.append(' ').append(queryGroup.getOperator()).append(' ');
                    }
                    sbGroup.append('(').append(itemQuery).append(')');
                }
            }

            // Clean up hierarchical facet items whose field has been matched to existing query items but not its value (obsolete facets)
            Set<IFacetItem> toRemove = new HashSet<>();
            for (IFacetItem facetItem : facets.getCurrentFacets()) {
                if (facetItem.isHierarchial() && usedHierarchicalFields.contains(facetItem.getField())
                        && !usedFieldValuePairs.contains(facetItem.getLink())) {
                    toRemove.add(facetItem);
                }
            }
            if (!toRemove.isEmpty()) {
                facets.getCurrentFacets().removeAll(toRemove);
            }

            // Add this group's query part to the main query
            if (sbGroup.length() > 0) {
                sbInfo.append(')');
                if (sb.length() > 0) {
                    // If this is not the first group, add the inter-group operator
                    switch (advancedSearchGroupOperator) {
                        case 0:
                            sb.append(" AND ");
                            break;
                        case 1:
                            sb.append(" OR ");
                            break;
                        default:
                            sb.append(" AND ");
                            break;
                    }
                }
                sb.append('(').append(sbGroup).append(')');
            }
        }
        if (sbCurrentCollection.length() > 0) {
            logger.trace(facets.getCurrentFacetStringPrefix() + " + " + sbCurrentCollection.toString());
            facets.setCurrentFacetString(facets.getCurrentFacetStringPrefix() + sbCurrentCollection.toString());
        } else {
            logger.trace(facets.getCurrentFacetString());
            facets.setCurrentFacetString(facets.getCurrentFacetString());
        }

        advancedSearchQueryInfo = sbInfo.toString();
        // Quickfix for single hierarchical item query info having an opening parenthesis only
        if (advancedSearchQueryInfo.startsWith("(") && !advancedSearchQueryInfo.endsWith(")")) {
            advancedSearchQueryInfo = advancedSearchQueryInfo.substring(1);
        }
        logger.trace("query info: {}", advancedSearchQueryInfo);

        logger.debug("advanced query: {}", sb.toString());
        return sb.toString();
    }

    public void hitsPerPageListener()
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("hitsPerPageListener");
        //        setHitsPerPage(hitsPerPage);
        executeSearch();
    }

    /**
     * <p>
     * executeSearch.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void executeSearch() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.debug("executeSearch; searchString: {}", searchStringInternal);
        mirrorAdvancedSearchCurrentHierarchicalFacets();

        //        String currentQuery = SearchHelper.prepareQuery(searchString);

        if (StringUtils.isEmpty(sortString)) {
            setSortString(DataManager.getInstance().getConfiguration().getDefaultSortField());
            logger.trace("Using default sorting: {}", sortString);
        }

        // Init search object
        currentSearch = new Search(activeSearchType, currentSearchFilter);
        currentSearch.setUserInput(searchString);
        currentSearch.setQuery(searchStringInternal);
        currentSearch.setPage(currentPage);
        currentSearch.setSortString(sortString);
        currentSearch.setFacetString(facets.getCurrentFacetString());
        currentSearch.setCustomFilterQuery(customFilterQuery);

        // Add search hit aggregation parameters, if enabled
        if (DataManager.getInstance().getConfiguration().isAggregateHits() && !searchTerms.isEmpty()) {
            String expandQuery = activeSearchType == 1 ? SearchHelper.generateAdvancedExpandQuery(advancedQueryGroups, advancedSearchGroupOperator)
                    : SearchHelper.generateExpandQuery(
                            SearchHelper.getExpandQueryFieldList(activeSearchType, currentSearchFilter, advancedQueryGroups), searchTerms,
                            phraseSearch);
            currentSearch.setExpandQuery(expandQuery);
        }

        currentSearch.execute(facets, searchTerms, hitsPerPage, advancedSearchGroupOperator, navigationHelper.getLocale(),
                DataManager.getInstance().getConfiguration().isAggregateHits());
    }

    /** {@inheritDoc} */
    @Override
    public int getActiveSearchType() {
        return activeSearchType;
    }

    /** {@inheritDoc} */
    @Override
    public void setActiveSearchType(int activeSearchType) {
        logger.trace("setActiveSearchType: {}", activeSearchType);
        if (this.activeSearchType != activeSearchType) {
            switch (activeSearchType) {
                case 1:
                    if (DataManager.getInstance().getConfiguration().isAdvancedSearchEnabled()) {
                        this.activeSearchType = activeSearchType;
                    } else {
                        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
                        logger.debug("Cannot set search type {} because it's disabled.", activeSearchType);
                    }
                    break;
                case 2:
                    if (DataManager.getInstance().getConfiguration().isTimelineSearchEnabled()) {
                        this.activeSearchType = activeSearchType;
                    } else {
                        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
                        logger.debug("Cannot set search type {} because it's disabled.", activeSearchType);
                    }
                    break;
                case 3:
                    if (DataManager.getInstance().getConfiguration().isCalendarSearchEnabled()) {
                        this.activeSearchType = activeSearchType;
                    } else {
                        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
                        logger.debug("Cannot set search type {} because it's disabled.", activeSearchType);
                    }
                    break;
                default:
                    this.activeSearchType = activeSearchType;
            }
            facets.resetCurrentFacetString();
        }
        logger.trace("activeSearchType: {}", activeSearchType);
    }

    /**
     * <p>
     * resetActiveSearchType.
     * </p>
     */
    public void resetActiveSearchType() {
        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> autocomplete(String suggest) throws IndexUnreachableException {
        logger.trace("autocomplete: {}", suggest);
        List<String> result = SearchHelper.searchAutosuggestion(suggest, facets.getCurrentFacets());
        //Collections.sort(result);

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSearchInDcFlag() {
        for (IFacetItem item : facets.getCurrentFacets()) {
            if (item.getField().equals(SolrConstants.DC)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Getter for the invisible (empty) search string. Used for the search field widget for when no search input display is desired.
     *
     * @return empty string
     */
    public String getInvisibleSearchString() {
        return "";
    }

    /**
     * The setter for the invisible search string. Performs all regular settings.
     *
     * @param invisibleSearchString a {@link java.lang.String} object.
     */
    public void setInvisibleSearchString(String invisibleSearchString) {
        setSearchString(invisibleSearchString);
    }

    /** {@inheritDoc} */
    @Override
    public String getSearchString() {
        return searchString;
    }

    /**
     * Wrapper method for Pretty URL mappings (so that the values is never empty).
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSearchStringForUrl() {
        if (StringUtils.isEmpty(searchString)) {
            return "-";
        }
        return StringTools.stripJS(searchString);
    }

    /**
     * Wrapper method for Pretty URL mappings.
     *
     * @param searchString a {@link java.lang.String} object.
     */
    public void setSearchStringForUrl(String searchString) {
        logger.trace("setSearchStringForUrl: {}", searchString);
        generateSimpleSearchString(searchString);
    }

    /**
     * {@inheritDoc}
     *
     * Wrapper for setSearchStringKeepCurrentPage() that also resets <code>currentPage</code>.
     */
    @Override
    public void setSearchString(String searchString) {
        logger.trace("setSearchString: {}", searchString);
        // Reset search result page
        currentPage = 1;
        this.searchString = StringTools.stripJS(searchString);
        generateSimpleSearchString(this.searchString);
    }

    /**
     * @param inSearchString the searchString to set
     */
    void generateSimpleSearchString(String inSearchString) {
        logger.trace("setSearchStringKeepCurrentPage: {}", inSearchString);
        logger.trace("currentSearchFilter: {}", currentSearchFilter.getLabel());
        if (inSearchString == null) {
            inSearchString = "";
        }
        try {
            inSearchString = URLDecoder.decode(inSearchString, URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
        } catch (IllegalArgumentException e) {
        }
        if ("-".equals(inSearchString)) {
            inSearchString = "";
        }

        searchString = StringTools.stripJS(inSearchString);
        searchStringInternal = "";
        searchTerms.clear();
        phraseSearch = false;

        inSearchString = inSearchString.trim();
        if (StringUtils.isNotEmpty(inSearchString)) {
            if ("*".equals(inSearchString)) {
                searchStringInternal = SearchHelper.prepareQuery("");
                return;
            }

            // Prepare search term sets for all relevant fields
            if (currentSearchFilter == null || currentSearchFilter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                if (searchTerms.get(SolrConstants.DEFAULT) == null) {
                    searchTerms.put(SolrConstants.DEFAULT, new HashSet<String>());
                }
                if (searchTerms.get(SolrConstants.FULLTEXT) == null) {
                    searchTerms.put(SolrConstants.FULLTEXT, new HashSet<String>());
                }
                if (searchTerms.get(SolrConstants.NORMDATATERMS) == null) {
                    searchTerms.put(SolrConstants.NORMDATATERMS, new HashSet<String>());
                }
                if (searchTerms.get(SolrConstants.UGCTERMS) == null) {
                    searchTerms.put(SolrConstants.UGCTERMS, new HashSet<String>());
                }
                if (searchTerms.get(SolrConstants.CMS_TEXT_ALL) == null) {
                    searchTerms.put(SolrConstants.CMS_TEXT_ALL, new HashSet<String>());
                }
            } else {
                if (searchTerms.get(currentSearchFilter.getField()) == null) {
                    searchTerms.put(currentSearchFilter.getField(), new HashSet<String>());
                }
            }

            inSearchString = inSearchString.replace(" OR ", " || ");
            inSearchString = inSearchString.replace(" AND ", " && ");
            inSearchString = inSearchString.toLowerCase(); // Solr won't find non-lowercase strings

            if (inSearchString.contains("\"")) {
                // Phrase search
                phraseSearch = true;
                String[] toSearch = inSearchString.split("\"");
                StringBuilder sb = new StringBuilder();
                for (String phrase : toSearch) {
                    phrase = phrase.replace("\"", "");
                    if (phrase.length() > 0) {
                        if (currentSearchFilter == null || currentSearchFilter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                            if (DataManager.getInstance().getConfiguration().isAggregateHits()) {
                                // For aggregated searches include both SUPER and regular DEFAULT/FULLTEXT fields
                                sb.append(SolrConstants.SUPERDEFAULT).append(":(\"").append(phrase).append("\") OR ");
                                sb.append(SolrConstants.SUPERFULLTEXT).append(":(\"").append(phrase).append("\") OR ");
                                sb.append(SolrConstants.SUPERUGCTERMS).append(":(\"").append(phrase).append("\") OR ");
                            }
                            sb.append(SolrConstants.DEFAULT).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.FULLTEXT).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.NORMDATATERMS).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.UGCTERMS).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.CMS_TEXT_ALL).append(":(\"").append(phrase).append("\")");
                            for (String field : searchTerms.keySet()) {
                                searchTerms.get(field).add(phrase);
                            }
                        } else {
                            // Specific filter selected
                            if (searchTerms.get(SolrConstants.FULLTEXT) == null) {
                                Set<String> terms = new HashSet<>();
                                searchTerms.put(SolrConstants.FULLTEXT, terms);
                            }
                            if (DataManager.getInstance().getConfiguration().isAggregateHits()) {
                                switch (currentSearchFilter.getField()) {
                                    case SolrConstants.DEFAULT:
                                        sb.append(SolrConstants.SUPERDEFAULT).append(":(\"").append(phrase).append("\") OR ");
                                        sb.append(SolrConstants.DEFAULT).append(":(\"").append(phrase).append("\")");
                                        break;
                                    case SolrConstants.FULLTEXT:
                                        sb.append(SolrConstants.SUPERFULLTEXT).append(":(\"").append(phrase).append("\") OR ");
                                        sb.append(SolrConstants.FULLTEXT).append(":(\"").append(phrase).append("\")");
                                        break;
                                    case SolrConstants.UGCTERMS:
                                        sb.append(SolrConstants.SUPERUGCTERMS).append(":(\"").append(phrase).append("\") OR ");
                                        sb.append(SolrConstants.UGCTERMS).append(":(\"").append(phrase).append("\")");
                                        break;
                                    default:
                                        sb.append(currentSearchFilter.getField()).append(":(\"").append(phrase).append("\")");
                                        break;
                                }
                            } else {
                                sb.append(currentSearchFilter.getField()).append(":(\"").append(phrase).append("\")");
                            }
                            searchTerms.get(currentSearchFilter.getField()).add(phrase);
                        }
                        sb.append(" AND ");
                    }
                }
                searchStringInternal = sb.toString();
            } else {
                // Non-phrase search
                inSearchString = inSearchString.replace(" &&", "");
                String[] termsSplit = inSearchString.split(SearchHelper.SEARCH_TERM_SPLIT_REGEX);

                // Clean up terms and create OR-connected groups
                List<String> preparedTerms = new ArrayList<>(termsSplit.length);
                for (int i = 0; i < termsSplit.length; ++i) {
                    String term = termsSplit[i].trim();
                    String unescapedTerm = SearchHelper.cleanUpSearchTerm(term);
                    term = ClientUtils.escapeQueryChars(unescapedTerm);
                    term = term.replace("\\*", "*"); // unescape falsely escaped truncation
                    if (term.length() > 0 && !DataManager.getInstance().getConfiguration().getStopwords().contains(term)) {
                        logger.trace("term: {}", term);
                        if (!"\\|\\|".equals(term)) {
                            preparedTerms.add(term);
                            for (String field : searchTerms.keySet()) {
                                searchTerms.get(field).add(unescapedTerm);
                            }
                        } else if (i > 0 && i < termsSplit.length - 1) {
                            // Two terms separated by OR: remove previous term and add it together with the next term as a group
                            int previousIndex = preparedTerms.size() - 1;
                            String prevTerm = preparedTerms.get(previousIndex);
                            String unescapedNextTerm = SearchHelper.cleanUpSearchTerm(termsSplit[i + 1]);
                            String nextTerm = ClientUtils.escapeQueryChars(unescapedNextTerm);
                            nextTerm = nextTerm.replace("\\*", "*"); // unescape falsely escaped runcation
                            preparedTerms.remove(previousIndex);
                            preparedTerms.add(prevTerm + " OR " + nextTerm);
                            for (String field : searchTerms.keySet()) {
                                searchTerms.get(field).add(unescapedNextTerm);
                            }
                            i++;
                        }
                    }
                }
                // Construct inner query part
                StringBuilder sbInner = new StringBuilder();
                for (String term : preparedTerms) {
                    if (sbInner.length() > 0) {
                        sbInner.append(" AND ");
                    }
                    if (!term.contains(" OR ")) {
                        sbInner.append(term);
                    } else {
                        sbInner.append('(').append(term).append(')');
                    }
                }
                if (sbInner.length() > 0) {
                    StringBuilder sbOuter = new StringBuilder();
                    if (currentSearchFilter == null || currentSearchFilter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                        // No filters defined or ALL
                        if (DataManager.getInstance().getConfiguration().isAggregateHits()) {
                            sbOuter.append(SolrConstants.SUPERDEFAULT).append(":(").append(sbInner.toString());
                            sbOuter.append(") OR ").append(SolrConstants.SUPERFULLTEXT).append(":(").append(sbInner.toString());
                            sbOuter.append(") OR ").append(SolrConstants.SUPERUGCTERMS).append(":(").append(sbInner.toString());
                            sbOuter.append(") OR ");
                        }
                        sbOuter.append(SolrConstants.DEFAULT).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.FULLTEXT).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.NORMDATATERMS).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.UGCTERMS).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.CMS_TEXT_ALL).append(":(").append(sbInner.toString()).append(')');
                    } else {
                        // Specific filter selected
                        if (DataManager.getInstance().getConfiguration().isAggregateHits()) {
                            switch (currentSearchFilter.getField()) {
                                case SolrConstants.DEFAULT:
                                    sbOuter.append(SolrConstants.SUPERDEFAULT).append(":(").append(sbInner.toString()).append(") OR ");
                                    sbOuter.append(SolrConstants.DEFAULT).append(":(").append(sbInner.toString()).append(')');
                                    break;
                                case SolrConstants.FULLTEXT:
                                    sbOuter.append(SolrConstants.SUPERFULLTEXT).append(":(").append(sbInner.toString()).append(") OR ");
                                    sbOuter.append(SolrConstants.FULLTEXT).append(":(").append(sbInner.toString()).append(')');
                                    break;
                                case SolrConstants.UGCTERMS:
                                    sbOuter.append(SolrConstants.SUPERUGCTERMS).append(":(").append(sbInner.toString()).append(") OR ");
                                    sbOuter.append(SolrConstants.UGCTERMS).append(":(").append(sbInner.toString()).append(')');
                                    break;
                                default:
                                    sbOuter.append(currentSearchFilter.getField()).append(":(").append(sbInner.toString()).append(')');
                                    break;
                            }
                        } else {
                            sbOuter.append(currentSearchFilter.getField()).append(":(").append(sbInner.toString()).append(')');
                        }
                    }
                    searchStringInternal += sbOuter.toString();
                }

            }
            if (searchStringInternal.endsWith(" OR ")) {
                searchStringInternal = searchStringInternal.substring(0, searchStringInternal.length() - 4);
            } else if (searchStringInternal.endsWith(" AND ")) {
                searchStringInternal = searchStringInternal.substring(0, searchStringInternal.length() - 5);
            }

            logger.trace("search string: {}", searchStringInternal);
            logger.trace("search terms: {}", searchTerms.toString());
        } else {
            searchString = "";
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getExactSearchString() {
        if (searchStringInternal.length() == 0) {
            return "-";
        }
        String ret = BeanUtils.escapeCriticalUrlChracters(searchStringInternal);
        try {
            ret = URLEncoder.encode(ret, URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
        return ret;
    }

    /**
     * Sets the current searchString to the given query, without parsing it like the regular setSearchString() method. To be used for sorting,
     * drill-down, etc.
     *
     * @param inSearchString a {@link java.lang.String} object.
     */
    public void setExactSearchString(String inSearchString) {
        logger.debug("setExactSearchString: {}", inSearchString);
        if ("-".equals(inSearchString)) {
            inSearchString = "";
            searchString = "";
        }
        searchStringInternal = inSearchString;
        // First apply regular URL decoder
        try {
            searchStringInternal = URLDecoder.decode(inSearchString, URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
        } catch (IllegalArgumentException e) {
        }
        // Then unescape custom sequences
        searchStringInternal = BeanUtils.unescapeCriticalUrlChracters(searchStringInternal);

        // Parse search terms from the query (unescape spaces first)
        String discriminatorValue = null;
        if (navigationHelper != null) {
            try {
                discriminatorValue = navigationHelper.getSubThemeDiscriminatorValue();
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
        }
        searchTerms = SearchHelper.extractSearchTermsFromQuery(searchStringInternal.replace("\\", ""), discriminatorValue);
        logger.trace("searchTerms: {}", searchTerms);

        // TODO reset mode?
    }

    /**
     * JSF expects a getter, too.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getExactSearchStringResetGui() {
        return getExactSearchString();
    }

    /**
     * Works exactly like setExactSearchString() but guiSearchString is always reset.
     *
     * @param inSearchString a {@link java.lang.String} object.
     */
    public void setExactSearchStringResetGui(String inSearchString) {
        setExactSearchString(inSearchString);
        searchString = "";
    }

    /**
     * @return the customFilterQuery
     */
    public String getCustomFilterQuery() {
        return customFilterQuery;
    }

    /**
     * @param customFilterQuery the customFilterQuery to set
     */
    public void setCustomFilterQuery(String customFilterQuery) {
        this.customFilterQuery = customFilterQuery;
    }

    /** {@inheritDoc} */
    @Override
    public void setSortString(String sortString) {
        if ("-".equals(sortString)) {
            this.sortString = "";
        } else if (sortString != null && "RANDOM".equals(sortString.toUpperCase())) {
            this.sortString = new StringBuilder().append("random_").append(random.nextInt(Integer.MAX_VALUE)).toString();
        } else {
            this.sortString = sortString;
        }
        //        sortFields = SearchHelper.parseSortString(this.sortString, navigationHelper);
    }

    /** {@inheritDoc} */
    @Override
    public String getSortString() {
        if (StringUtils.isEmpty(sortString)) {
            return "-";
        }

        return sortString;
    }

    /**
     * Matches the selected collection item in the advanced search to the current value of <code>currentCollection</code>.
     *
     * @should mirror facet items to search query items correctly
     * @should remove facet items from search query items correctly
     * @should add extra search query item if all items full
     * @should not replace query items already in use
     * @should not add identical hierarchical query items
     */
    public void mirrorAdvancedSearchCurrentHierarchicalFacets() {
        logger.trace("mirrorAdvancedSearchCurrentHierarchicalFacets");
        if (advancedQueryGroups.isEmpty()) {
            return;
        }
        if (facets.getCurrentFacets().isEmpty()) {
            SearchQueryGroup queryGroup = advancedQueryGroups.get(0);
            for (SearchQueryItem item : queryGroup.getQueryItems()) {
                if (item.isHierarchical()) {
                    logger.trace("resetting current field in advanced search: {}", item.getField());
                    item.reset();
                }
            }
            return;
        }

        SearchQueryGroup queryGroup = advancedQueryGroups.get(0);
        Set<SearchQueryItem> populatedQueryItems = new HashSet<>();
        for (IFacetItem facetItem : facets.getCurrentFacets()) {
            if (!facetItem.isHierarchial()) {
                continue;
            }
            // logger.trace("facet item: {}", facetItem.toString());
            // Look up and re-purpose existing query items with the same field first
            boolean matched = false;
            for (SearchQueryItem queryItem : queryGroup.getQueryItems()) {
                // field:value pair already exists
                if (populatedQueryItems.contains(queryItem)) {
                    // logger.trace("query item already populated: {}", queryItem);
                    continue;
                }
                // Ignore items for other fields
                if (queryItem.getField() != null && !queryItem.getField().equals(facetItem.getField())) {
                    // logger.trace("query item field mismatch: {}", queryItem);
                    continue;
                }
                // Override existing items without a field or with the same field with current facet value
                // logger.trace("updating query item: {}", queryItem);
                if (queryItem.getField() == null) {
                    queryItem.setField(facetItem.getField());
                }
                queryItem.setOperator(SearchItemOperator.IS);
                queryItem.setValue(facetItem.getValue());
                // logger.trace("updated query item: {}", queryItem);
                populatedQueryItems.add(queryItem);
                matched = true;
                break;
            }
            if (!matched) {
                // If no search field is set up for collection search, add new field containing the currently selected collection
                SearchQueryItem item = new SearchQueryItem(BeanUtils.getLocale());
                item.setField(facetItem.getField());
                item.setOperator(SearchItemOperator.IS);
                item.setValue(facetItem.getValue());
                // ...but only if there is no exact field:value pair already among the query items
                if (!populatedQueryItems.contains(item)) {
                    queryGroup.getQueryItems().add(item);
                    // logger.trace("added new item: {}", item);
                }
            }
        }
        // Reset any hierarchical query items that could not be used for existing facets
        for (SearchQueryItem queryItem : queryGroup.getQueryItems()) {
            if (queryItem.isHierarchical() && !populatedQueryItems.contains(queryItem)) {
                logger.trace("Resetting advanced query item {}", queryItem);
                queryItem.reset();
            }
        }
    }

    public String removeChronologyFacetAction() {
        String facet = SolrConstants.YEAR + ":" + facets.getTempValue();
        facets.setTempValue("");
        return removeFacetAction(facet);
    }

    /**
     * <p>
     * removeFacetAction.
     * </p>
     *
     * @param facetQuery a {@link java.lang.String} object.
     * @should remove facet correctly
     * @return a {@link java.lang.String} object.
     */
    public String removeFacetAction(String facetQuery) {

        //redirect to current cms page if this action takes place on a cms page
        Optional<ViewerPath> oPath = ViewHistory.getCurrentView(BeanUtils.getRequest());
        if (oPath.isPresent() && oPath.get().isCmsPage()) {
            facets.removeFacetAction(facetQuery, "pretty:browse4");
            SearchFunctionality search = oPath.get().getCmsPage().getSearch();
            search.redirectToSearchUrl(true);
            return "";
        } else if (PageType.browse.equals(oPath.map(path -> path.getPageType()).orElse(PageType.other))) {
            String ret = facets.removeFacetAction(facetQuery, "pretty:browse4");
            return ret;
        } else {
            String ret = facets.removeFacetAction(facetQuery,
                    activeSearchType == SearchHelper.SEARCH_TYPE_ADVANCED ? "pretty:searchAdvanced5" : "pretty:newSearch5");
            return ret;
        }
    }

    /*
     * Paginator methods
     */

    /** {@inheritDoc} */
    @Override
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * <p>
     * Setter for the field <code>currentPage</code>.
     * </p>
     *
     * @param currentPage the currentPage to set
     */
    public void setCurrentPage(int currentPage) {
        logger.trace("setCurrentPage: {}", currentPage);
        this.currentPage = currentPage;
    }

    /** {@inheritDoc} */
    @Override
    public long getHitsCount() {
        if (currentSearch != null) {
            // logger.trace("Hits count = {}", currentSearch.getHitsCount());
            return currentSearch.getHitsCount();
        }
        // logger.warn("No Search object available");

        return 0;
    }

    /**
     * <p>
     * setHitsCount.
     * </p>
     *
     * @param hitsCount the hitsCount to set
     */
    public void setHitsCount(long hitsCount) {
        if (currentSearch != null) {
            currentSearch.setHitsCount(hitsCount);
        }
    }

    /**
     * <p>
     * Getter for the field <code>searchTerms</code>.
     * </p>
     *
     * @return the searchTerms
     */
    public Map<String, Set<String>> getSearchTerms() {
        return searchTerms;
    }

    /**
     * <p>
     * Getter for the field <code>currentHitIndex</code>.
     * </p>
     *
     * @return the currentHitIndex
     */
    public int getCurrentHitIndex() {
        return currentHitIndex;
    }

    /**
     * <p>
     * getCurrentHitIndexDisplay.
     * </p>
     *
     * @return a int.
     */
    public int getCurrentHitIndexDisplay() {
        return currentHitIndex + 1;
    }

    /**
     * <p>
     * increaseCurrentHitIndex.
     * </p>
     *
     * @should increase index correctly
     * @should decrease index correctly
     * @should reset operand afterwards
     * @should do nothing if hit index at the last hit
     * @should do nothing if hit index at 0
     */
    public void increaseCurrentHitIndex() {
        logger.trace("increaseCurrentHitIndex");
        if (hitIndexOperand != 0 && currentSearch != null) {
            try {
                if (hitIndexOperand > 0 && currentHitIndex >= currentSearch.getHitsCount() - 1) {
                    currentHitIndex = (int) (currentSearch.getHitsCount() - 1);
                    return;
                }
                int old = currentHitIndex;
                currentHitIndex += hitIndexOperand;
                if (currentHitIndex < 0) {
                    currentHitIndex = 0;
                } else if (currentHitIndex >= currentSearch.getHitsCount()) {
                    currentHitIndex = (int) (currentSearch.getHitsCount() - 1);
                }
                logger.trace("increaseCurrentHitIndex: {}->{}", old, currentHitIndex);
            } finally {
                hitIndexOperand = 0; // reset operand
            }
        }
    }

    /**
     * <p>
     * Getter for the field <code>hitIndexOperand</code>.
     * </p>
     *
     * @return the hitIndexOperand
     */
    public int getHitIndexOperand() {
        return hitIndexOperand;
    }

    /**
     * <p>
     * Setter for the field <code>hitIndexOperand</code>.
     * </p>
     *
     * @param hitIndexOperand the hitIndexOperand to set
     */
    public void setHitIndexOperand(int hitIndexOperand) {
        logger.trace("setHitIndexOperand: {}", hitIndexOperand);
        this.hitIndexOperand = hitIndexOperand;
    }

    /**
     * Returns the index of the currently displayed BrowseElement, if it is present in the search hit list.
     *
     * @param pi Record identifier of the loaded record.
     * @param page Page number of he loaded record.
     * @param aggregateHits If true, only the identifier has to match, page number is ignored.
     * @should set currentHitIndex to minus one if no search hits
     * @should set currentHitIndex correctly
     */
    public void findCurrentHitIndex(String pi, int page, boolean aggregateHits) {
        logger.trace("findCurrentHitIndex: {}/{}", pi, page);
        if (currentSearch == null || currentSearch.getHits().isEmpty()) {
            currentHitIndex = -1;
            return;
        }

        currentHitIndex = 0;
        for (SearchHit hit : currentSearch.getHits()) {
            BrowseElement be = hit.getBrowseElement();
            logger.trace("BrowseElement: {}/{}", be.getPi(), be.getImageNo());
            if (be.getPi().equals(pi) && (aggregateHits || be.getImageNo() == page)) {
                logger.trace("currentPage: {}", currentPage);
                currentHitIndex += (currentPage - 1) * hitsPerPage;
                logger.trace("currentHitIndex: {}", currentHitIndex);
                return;
            }
            currentHitIndex++;
        }
        //if we leave the loop without returning, no hit was found and currentHitIndex should be -1
        currentHitIndex = -1;
    }

    /**
     * Returns the next BrowseElement in the hit list relative to the given index.
     *
     * @return Next BrowseElement in the list; same BrowseElement if this is the last index in the list.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public BrowseElement getNextElement() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("getNextElement: {}", currentHitIndex);
        if (currentHitIndex <= -1 || currentSearch == null || currentSearch.getHits().isEmpty()) {
            return null;
        }

        if (currentHitIndex < currentSearch.getHitsCount() - 1) {
            //            return currentSearch.getHits().get(currentHitIndex + 1).getBrowseElement();
            return SearchHelper.getBrowseElement(searchStringInternal, currentHitIndex + 1, currentSearch.getAllSortFields(),
                    facets.generateFacetFilterQueries(advancedSearchGroupOperator, true, true), SearchHelper.generateQueryParams(), searchTerms,
                    BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
        }
        //        return currentSearch.getHits().get(currentHitIndex).getBrowseElement();
        return SearchHelper.getBrowseElement(searchStringInternal, currentHitIndex, currentSearch.getAllSortFields(),
                facets.generateFacetFilterQueries(advancedSearchGroupOperator, true, true), SearchHelper.generateQueryParams(), searchTerms,
                BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
    }

    /**
     * Returns the previous BrowseElement in the hit list relative to the given index.
     *
     * @return Previous BrowseElement in the list; same BrowseElement if this is the first index in the list.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public BrowseElement getPreviousElement() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("getPreviousElement: {}", currentHitIndex);
        if (currentHitIndex <= -1 || currentSearch == null || currentSearch.getHits().isEmpty()) {
            return null;
        }

        if (currentHitIndex > 0) {
            //            return currentSearch.getHits().get(currentHitIndex - 1).getBrowseElement();
            return SearchHelper.getBrowseElement(searchStringInternal, currentHitIndex - 1, currentSearch.getAllSortFields(),
                    facets.generateFacetFilterQueries(advancedSearchGroupOperator, true, true), SearchHelper.generateQueryParams(), searchTerms,
                    BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
        } else if (currentSearch.getHitsCount() > 0) {
            //            return currentSearch.getHits().get(currentHitIndex).getBrowseElement();
            return SearchHelper.getBrowseElement(searchStringInternal, currentHitIndex, currentSearch.getAllSortFields(),
                    facets.generateFacetFilterQueries(advancedSearchGroupOperator, true, true), SearchHelper.generateQueryParams(), searchTerms,
                    BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<SearchFilter> getSearchFilters() {
        return DataManager.getInstance().getConfiguration().getSearchFilters();
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrentSearchFilterString() {
        if (currentSearchFilter != null) {
            return currentSearchFilter.getLabel();
        }

        for (SearchFilter filter : getSearchFilters()) {
            if (filter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                return SearchHelper.SEARCH_FILTER_ALL.getLabel();
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     *
     * Sets <code>currentSearchFilter</code> via the given label value.
     */
    @Override
    public void setCurrentSearchFilterString(String searchFilterLabel) {
        logger.trace("setCurrentSearchFilterString: {}", searchFilterLabel);
        for (SearchFilter filter : getSearchFilters()) {
            if (filter.getLabel().equals(searchFilterLabel)) {
                this.currentSearchFilter = filter;
                logger.trace("currentSearchFilter: {}", this.currentSearchFilter.getField());
                break;
            }
        }
    }

    /**
     * <p>
     * resetSearchFilter.
     * </p>
     */
    public void resetSearchFilter() {
        currentSearchFilter = null;
        for (SearchFilter filter : getSearchFilters()) {
            if (filter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                currentSearchFilter = filter;
                break;
            }
        }
    }

    /**
     * <p>
     * resetCurrentHitIndex.
     * </p>
     */
    public void resetCurrentHitIndex() {
        currentHitIndex = -1;
    }

    /**
     * <p>
     * isSortingEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSortingEnabled() {
        return DataManager.getInstance().getConfiguration().isSortingEnabled();
    }

    /**
     * <p>
     * Getter for the field <code>advancedQueryGroups</code>.
     * </p>
     *
     * @return the advancedQueryGroups
     */
    public List<SearchQueryGroup> getAdvancedQueryGroups() {
        // logger.trace("getAdvancedQueryGroups: {}", advancedQueryGroups.size());
        return advancedQueryGroups;
    }

    /**
     * <p>
     * addNewAdvancedQueryGroup.
     * </p>
     *
     * @should add group correctly
     * @return a boolean.
     */
    public boolean addNewAdvancedQueryGroup() {
        return advancedQueryGroups
                .add(new SearchQueryGroup(BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber()));
    }

    /**
     * <p>
     * removeAdvancedQueryGroup.
     * </p>
     *
     * @param group a {@link io.goobi.viewer.model.search.SearchQueryGroup} object.
     * @should remove group correctly
     * @return a boolean.
     */
    public boolean removeAdvancedQueryGroup(SearchQueryGroup group) {
        if (advancedQueryGroups.size() > 1) {
            return advancedQueryGroups.remove(group);
        }

        return false;
    }

    /**
     * Populates the list of advanced search drop-down values for the given field. List is only generated once per user session.
     *
     * @param field The index field for which to get drop-down values.
     * @param language Translation language for the values.
     * @param hierarchical If true, the menu items will be listed in their corresponding hierarchy (e.g. DC)
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws IllegalRequestException
     */
    public List<StringPair> getAdvancedSearchSelectItems(String field, String language, boolean hierarchical)
            throws PresentationException, IndexUnreachableException, DAOException {
        // logger.trace("getAdvancedSearchSelectItems: {}", field);
        if (field == null) {
            throw new IllegalArgumentException("field may not be null.");
        }
        if (language == null) {
            throw new IllegalArgumentException("language may not be null.");
        }
        String key = new StringBuilder(language).append('_').append(field).toString();
        List<StringPair> ret = advancedSearchSelectItems.get(key);
        if (ret != null) {
            return ret;
        }

        ret = new ArrayList<>();
        logger.trace("Generating drop-down values for {}", field);
        if (SolrConstants.BOOKMARKS.equals(field)) {
            if (userBean != null && userBean.isLoggedIn()) {
                // User bookshelves
                List<BookmarkList> bookmarkLists = DataManager.getInstance().getDao().getBookmarkLists(userBean.getUser());
                if (!bookmarkLists.isEmpty()) {
                    for (BookmarkList bookmarkList : bookmarkLists) {
                        if (!bookmarkList.getItems().isEmpty()) {
                            ret.add(new StringPair(bookmarkList.getName(), bookmarkList.getName()));
                        }
                    }
                }
            } else {
                // Session bookmark list
                Optional<BookmarkList> bookmarkList =
                        DataManager.getInstance().getBookmarkManager().getBookmarkList(BeanUtils.getRequest().getSession());
                if (bookmarkList.isPresent() && !bookmarkList.get().getItems().isEmpty()) {
                    ret.add(new StringPair(bookmarkList.get().getName(),
                            ViewerResourceBundle.getTranslation("bookmarkList_session", null)));
                }
            }
            // public bookmark lists
            List<BookmarkList> publicBookmarkLists = DataManager.getInstance().getDao().getPublicBookmarkLists();
            if (!publicBookmarkLists.isEmpty()) {
                for (BookmarkList bookmarkList : publicBookmarkLists) {
                    StringPair pair = new StringPair(bookmarkList.getName(), bookmarkList.getName());
                    if (!bookmarkList.getItems().isEmpty() && !ret.contains(pair)) {
                        ret.add(pair);
                    }
                }
            }
        } else if (hierarchical) {
            BrowseBean browseBean = BeanUtils.getBrowseBean();
            if (browseBean == null) {
                browseBean = new BrowseBean();
            }
            // Make sure displayDepth is at configured to the desired depth for this field (or -1 for complete depth)
            int displayDepth = DataManager.getInstance().getConfiguration().getCollectionDisplayDepthForSearch(field);
            List<BrowseDcElement> elementList = browseBean.getList(field, displayDepth);
            StringBuilder sbItemLabel = new StringBuilder();
            for (BrowseDcElement dc : elementList) {
                // Skip reversed values that MD_* and MD2_* fields will return
                if (StringUtils.isEmpty(dc.getName()) || dc.getName().charAt(0) == 1) {
                    continue;
                }
                for (int i = 0; i < dc.getLevel(); ++i) {
                    sbItemLabel.append("- ");
                }
                sbItemLabel.append(ViewerResourceBundle.getTranslation(dc.getName(), null));
                ret.add(new StringPair(dc.getName(), sbItemLabel.toString()));
                sbItemLabel.setLength(0);
            }
            advancedSearchSelectItems.put(key, ret);
        } else {
            String suffix = SearchHelper.getAllSuffixes();

            List<String> values = SearchHelper.getFacetValues(field + ":[* TO *]" + suffix, field, 1);
            for (String value : values) {
                ret.add(new StringPair(value, ViewerResourceBundle.getTranslation(value, null)));
            }

            Collections.sort(ret);
            advancedSearchSelectItems.put(key, ret);
        }
        logger.trace("Generated {} values", ret.size());

        return ret;
    }

    /**
     * Returns drop-down items for all collection names. Convenience method that retrieves the current language from <code>NavigationHelper</code>.
     * This method shouldn't throw exceptions, otherwise it can cause an IllegalStateException.
     *
     * @return a {@link java.util.List} object.
     * @throws IllegalRequestException
     */
    public List<StringPair> getAllCollections() throws IllegalRequestException {
        //        NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
        try {
            if (navigationHelper != null) {
                return getAdvancedSearchSelectItems(SolrConstants.DC, navigationHelper.getLocale().getLanguage(), true);

            }
            return getAdvancedSearchSelectItems(SolrConstants.DC, "de", true);
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here");
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here");
        } catch (DAOException e) {
            logger.debug("DAOException thrown here");
        }

        return new ArrayList<>();
    }

    /**
     * Returns drop-down items for all collection names. The displayed values are translated into the given language.
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws IllegalRequestException
     */
    public List<StringPair> getAllCollections(String language)
            throws PresentationException, IndexUnreachableException, DAOException, IllegalRequestException {
        return getAdvancedSearchSelectItems(SolrConstants.DC, language, true);
    }

    /**
     * <p>
     * Getter for the field <code>advancedSearchGroupOperator</code>.
     * </p>
     *
     * @return the advancedSearchGroupOperator
     */
    public int getAdvancedSearchGroupOperator() {
        return advancedSearchGroupOperator;
    }

    /**
     * <p>
     * Setter for the field <code>advancedSearchGroupOperator</code>.
     * </p>
     *
     * @param advancedSearchGroupOperator the advancedSearchGroupOperator to set
     */
    public void setAdvancedSearchGroupOperator(int advancedSearchGroupOperator) {
        this.advancedSearchGroupOperator = advancedSearchGroupOperator;
    }

    /**
     * <p>
     * getAdvancedSearchAllowedFields.
     * </p>
     *
     * @return List of allowed advanced search fields
     */
    public List<AdvancedSearchFieldConfiguration> getAdvancedSearchAllowedFields() {
        return getAdvancedSearchAllowedFields(navigationHelper.getLocaleString());
    }

    /**
     * Returns index field names allowed for advanced search use. If language-specific index fields are used, those that don't match the current
     * locale are omitted.
     *
     * @param language a {@link java.lang.String} object.
     * @return List of allowed advanced search fields
     * @should omit languaged fields for other languages
     */
    public static List<AdvancedSearchFieldConfiguration> getAdvancedSearchAllowedFields(String language) {
        List<AdvancedSearchFieldConfiguration> fields = DataManager.getInstance().getConfiguration().getAdvancedSearchFields();
        if (fields == null) {
            return Collections.emptyList();
        }

        // Omit other languages
        if (!fields.isEmpty() && StringUtils.isNotEmpty(language)) {
            List<AdvancedSearchFieldConfiguration> toRemove = new ArrayList<>();
            language = language.toUpperCase();
            for (AdvancedSearchFieldConfiguration field : fields) {
                if (field.getField().contains(SolrConstants._LANG_) && !field.getField().endsWith(language)) {
                    toRemove.add(field);
                }
            }
            if (!toRemove.isEmpty()) {
                fields.removeAll(toRemove);
            }
        }

        fields.add(0, new AdvancedSearchFieldConfiguration(SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS));

        return fields;
    }

    /**
     * <p>
     * Getter for the field <code>searchInCurrentItemString</code>.
     * </p>
     *
     * @return the searchInCurrentItemString
     */
    public String getSearchInCurrentItemString() {
        return searchInCurrentItemString;
    }

    /**
     * <p>
     * Setter for the field <code>searchInCurrentItemString</code>.
     * </p>
     *
     * @param searchInCurrentItemString the searchInCurrentItemString to set
     */
    public void setSearchInCurrentItemString(String searchInCurrentItemString) {
        // Reset the advanced search parameters prior to setting
        resetAdvancedSearchParameters(1, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
        this.searchInCurrentItemString = searchInCurrentItemString;
    }

    /**
     * <p>
     * Getter for the field <code>currentSearch</code>.
     * </p>
     *
     * @return the currentSearch
     */
    public Search getCurrentSearch() {
        return currentSearch;
    }

    /**
     * <p>
     * Setter for the field <code>currentSearch</code>.
     * </p>
     *
     * @param currentSearch the currentSearch to set
     */
    public void setCurrentSearch(Search currentSearch) {
        logger.trace("Setting current search to {}", currentSearch);
        this.currentSearch = currentSearch;
    }

    /**
     * <p>
     * saveSearchAction.
     * </p>
     *
     * @should add all values correctly
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveSearchAction() throws DAOException {
        if (StringUtils.isBlank(currentSearch.getName())) {
            Messages.error("nameRequired");
            return "";
        }

        currentSearch.setLastHitsCount(currentSearch.getHitsCount());

        UserBean ub = BeanUtils.getUserBean();
        if (ub != null) {
            currentSearch.setOwner(ub.getUser());
        }
        currentSearch.setDateUpdated(LocalDateTime.now());
        if (DataManager.getInstance().getDao().addSearch(currentSearch)) {
            currentSearch.setSaved(true);
            Messages.info("saveSearchSuccess");
        } else {
            Messages.error("errSave");
        }

        return "";
    }

    /**
     * <p>
     * getRssUrl.
     * </p>
     *
     * @return URL to the RSS feed for the current search
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRssUrl() throws ViewerConfigurationException {
        if (searchStringInternal == null) {
            return null;
        }

        String currentQuery = SearchHelper.prepareQuery(searchStringInternal);
        AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null);
        if (urls == null) {

            try {
                return new StringBuilder().append(DataManager.getInstance().getConfiguration().getRestApiUrl())
                        .append("rss/search/")
                        .append(URLEncoder.encode(currentQuery, URL_ENCODING))
                        .append('/')
                        .append(URLEncoder.encode(facets.getCurrentFacetString(), URL_ENCODING))
                        .append('/')
                        .append(advancedSearchGroupOperator)
                        .append("/-/")
                        .toString();
            } catch (UnsupportedEncodingException e) {
                logger.warn("Could not encode query '{}' for URL", currentQuery);
                return new StringBuilder().append(DataManager.getInstance().getConfiguration().getRestApiUrl())
                        .append("rss/search/")
                        .append(currentQuery)
                        .append('/')
                        .append(facets.getCurrentFacetString())
                        .append('/')
                        .append(advancedSearchGroupOperator)
                        .append("/-/")
                        .toString();
            }

        }

        String facetQuery = StringUtils.isBlank(facets.getCurrentFacetString().replace("-", "")) ? null : facets.getCurrentFacetString();
        return urls.path(ApiUrls.RECORDS_RSS)
                .query("query", currentQuery)
                .query("facets", facetQuery)
                .query("facetQueryOperator", advancedSearchGroupOperator)
                .build();
    }

    /**
     * <p>
     * isSearchSavingEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSearchSavingEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchSavingEnabled();
    }

    /**
     * <p>
     * executeSavedSearchAction.
     * </p>
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a {@link java.lang.String} object.
     */
    public String executeSavedSearchAction(Search search) {
        logger.trace("executeSavedSearchAction");
        if (search == null) {
            throw new IllegalArgumentException("search may not be null");
        }

        searchString = search.getUserInput();
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        try {
            response.sendRedirect(search.getUrl());
            return null;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return "";
    }

    /**
     * <p>
     * exportSearchAsExcelAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String exportSearchAsExcelAction() throws IndexUnreachableException {
        logger.trace("exportSearchAsExcelAction");
        final FacesContext facesContext = FacesContext.getCurrentInstance();

        String currentQuery = SearchHelper.prepareQuery(searchStringInternal);
        String finalQuery = SearchHelper.buildFinalQuery(currentQuery, DataManager.getInstance().getConfiguration().isAggregateHits());
        Locale locale = navigationHelper.getLocale();
        int timeout = DataManager.getInstance().getConfiguration().getExcelDownloadTimeout(); //[s]

        BiConsumer<HttpServletRequest, Task> task = (request, job) -> {
            if (!facesContext.getResponseComplete()) {
                try (SXSSFWorkbook wb = buildExcelSheet(facesContext, finalQuery, currentQuery, locale)) {
                    if (wb == null) {
                        job.setError("Failed to create excel sheet");
                    } else if (Thread.interrupted()) {
                        job.setError("Execution cancelled");
                    } else {
                        Callable<Boolean> download = new Callable<Boolean>() {

                            @Override
                            public Boolean call() {
                                try {
                                    logger.debug("Writing excel");
                                    return writeExcelSheet(facesContext, wb);
                                } finally {
                                    facesContext.responseComplete();
                                }
                            }
                        };

                        downloadComplete = new FutureTask<>(download);
                        executor.submit(downloadComplete);
                        downloadComplete.get(timeout, TimeUnit.SECONDS);
                    }
                } catch (TimeoutException | InterruptedException e) {
                    job.setError("Timeout for excel download");
                } catch (ExecutionException | ViewerConfigurationException e) {
                    logger.error(e.getMessage(), e);
                    job.setError("Failed to create excel sheet");
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                job.setError("Response is already committed");
            }
        };

        try {
            Task excelCreationJob = new Task(new TaskParameter(TaskType.SEARCH_EXCEL_EXPORT), task);
            Long jobId = DataManager.getInstance().getRestApiJobManager().addTask(excelCreationJob);
            Future ready = DataManager.getInstance()
                    .getRestApiJobManager()
                    .triggerTaskInThread(jobId, (HttpServletRequest) facesContext.getExternalContext().getRequest());
            ready.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.debug("Download interrupted");
        } catch (ExecutionException e) {
            logger.debug("Download execution error", e);
            Messages.error("download_internal_error");
        } catch (TimeoutException e) {
            logger.debug("Downloadtimed out");
            Messages.error("download_timeout");

        } finally {
            if (downloadReady != null && !downloadReady.isDone()) {
                downloadReady.cancel(true);
            }
            if (downloadComplete != null && !downloadComplete.isDone()) {
                downloadComplete.cancel(true);
            }
            this.downloadComplete = null;
            this.downloadReady = null;
        }
        return "";
    }

    /**
     * @param facesContext
     * @param wb
     * @throws IOException
     */
    private static boolean writeExcelSheet(final FacesContext facesContext, final SXSSFWorkbook wb) {
        try {
            wb.write(facesContext.getExternalContext().getResponseOutputStream());
            if (Thread.interrupted()) {
                return false;
            }
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage(), e.getCause());
            return false;
        } finally {
            wb.dispose();
        }
    }

    /**
     * @param facesContext
     * @param finalQuery Complete query with suffixes.
     * @param exportQuery Query constructed from the user's input, without any secret suffixes.
     * @param locale
     * @return
     * @throws InterruptedException
     * @throws ViewerConfigurationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    private SXSSFWorkbook buildExcelSheet(final FacesContext facesContext, String finalQuery, String exportQuery, Locale locale)
            throws InterruptedException, ViewerConfigurationException {
        try {
            HttpServletRequest request = BeanUtils.getRequest(facesContext);
            if (request == null) {
                request = BeanUtils.getRequest();
            }
            Map<String, String> params = SearchHelper.generateQueryParams();
            final SXSSFWorkbook wb = SearchHelper.exportSearchAsExcel(finalQuery, exportQuery, currentSearch.getAllSortFields(),
                    facets.generateFacetFilterQueries(advancedSearchGroupOperator, true, true), params, searchTerms, locale,
                    DataManager.getInstance().getConfiguration().isAggregateHits(), request);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            facesContext.getExternalContext().responseReset();
            facesContext.getExternalContext().setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            facesContext.getExternalContext()
                    .setResponseHeader("Content-Disposition", "attachment;filename=\"viewer_search_"
                            + LocalDateTime.now().format(DateTools.formatterISO8601DateTime)
                            + ".xlsx\"");
            return wb;
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        } catch (PresentationException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * <p>
     * Getter for the field <code>hitsPerPage</code>.
     * </p>
     *
     * @return the hitsPerPage
     */
    public int getHitsPerPage() {
        return hitsPerPage;
    }

    /**
     * <p>
     * Setter for the field <code>hitsPerPage</code>.
     * </p>
     *
     * @param hitsPerPage the hitsPerPage to set
     */
    public void setHitsPerPage(int hitsPerPage) {
        logger.trace("setHitsPerPage: {}", hitsPerPage);
        this.hitsPerPage = hitsPerPage;
    }

    /**
     * <p>
     * Getter for the field <code>advancedSearchQueryInfo</code>.
     * </p>
     *
     * @return the advancedSearchQueryInfo
     */
    public String getAdvancedSearchQueryInfo() {
        return advancedSearchQueryInfo;
    }

    //    /**
    //     * @return
    //     */
    //    public List<StringPair> getSortFields() {
    //        return this.sortFields;
    //    }

    /** {@inheritDoc} */
    @Override
    public SearchFacets getFacets() {
        return facets;
    }

    /**
     * <p>
     * isDownloadReady.
     * </p>
     *
     * @return a {@link java.util.concurrent.Future} object.
     */
    public Future<Boolean> isDownloadReady() {
        try {
            synchronized (downloadReady) {
                return downloadReady;
            }
        } catch (NullPointerException e) {
            return downloadReady;
        }
    }

    /**
     * <p>
     * getTotalNumberOfVolumes.
     * </p>
     *
     * @return a long.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public long getTotalNumberOfVolumes() throws IndexUnreachableException, PresentationException {
        String query = SearchHelper.buildFinalQuery(SearchHelper.ALL_RECORDS_QUERY, DataManager.getInstance().getConfiguration().isAggregateHits());
        return DataManager.getInstance().getSearchIndex().count(query);
    }

    /**
     * Returns the proper search URL part for the current search type.
     *
     * @should return correct url
     * @should return null if navigationHelper is null
     * @return a {@link java.lang.String} object.
     */
    public String getSearchUrl() {
        if (navigationHelper == null) {
            return null;
        }
        switch (activeSearchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return navigationHelper.getAdvancedSearchUrl();
            default:
                return navigationHelper.getSearchUrl();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getLastPage() {
        if (currentSearch != null) {
            return currentSearch.getLastPage(hitsPerPage);
        }

        return 0;
    }

    /**
     * <p>
     * getStructElement.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public StructElement getStructElement(String pi) throws IndexUnreachableException, PresentationException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        StructElement struct = new StructElement(Long.parseLong(doc.getFirstValue(SolrConstants.IDDOC).toString()), doc);
        return struct;
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrentSearchUrlRoot() {
        switch (activeSearchType) {
            case 1:
                return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/searchadvanced";
            default:
                return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/search";
        }

    }

    /**
     * <p>
     * getCurrentSearchUrlPart.
     * </p>
     *
     * @return Parameter string for pretty:search5 URLs.
     */
    public String getCurrentSearchUrlPart() {
        return new StringBuilder().append("/-/")
                .append(getExactSearchString())
                .append('/')
                .append(getCurrentPage())
                .append('/')
                .append(getSortString())
                .append('/')
                .append(facets.getCurrentFacetString())
                .append('/')
                .toString();
    }

    /**
     * <p>
     * updateFacetItem.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param hierarchical a boolean.
     */
    public void updateFacetItem(String field, boolean hierarchical) {
        getFacets().updateFacetItem(field, hierarchical);
        String url = getCurrentSearchUrl();
        redirectToURL(url);
    }

    /**
     * @return
     */
    private String getCurrentSearchUrl() {
        Optional<ViewerPath> oCurrentPath = ViewHistory.getCurrentView(BeanUtils.getRequest());
        if (oCurrentPath.isPresent()) {
            ViewerPath currentPath = oCurrentPath.get();
            StringBuilder sb = new StringBuilder();
            sb.append(currentPath.getApplicationUrl()).append("/").append(currentPath.getPrettifiedPagePath());
            URI uri = URI.create(sb.toString());
            uri = getParameterPath(uri);
            return StringTools.appendTrailingSlash(uri.toString());
        }

        //fallback
        return "pretty:search5";
    }

    private URI getParameterPath(URI basePath) {
        //        path = ViewerPathBuilder.resolve(path, getCollection());
        basePath = ViewerPathBuilder.resolve(basePath, "-");
        basePath = ViewerPathBuilder.resolve(basePath, getExactSearchString());
        basePath = ViewerPathBuilder.resolve(basePath, Integer.toString(getCurrentPage()));
        basePath = ViewerPathBuilder.resolve(basePath, getSortString());
        basePath = ViewerPathBuilder.resolve(basePath, StringTools.encodeUrl(getFacets().getCurrentFacetString()));
        return basePath;
    }

    private static void redirectToURL(String url) {
        final FacesContext context = FacesContext.getCurrentInstance();
        try {
            context.getExternalContext().redirect(url);
        } catch (IOException e) {
            logger.error("Failed to redirect to url", e);
        }
    }

    /**
     * Returns a list of FilterLink elements for the given field over all documents in the index (optionally filtered by a subquery). Replaces the
     * method in the old TagLib class.
     *
     * @param field a {@link java.lang.String} object.
     * @param subQuery a {@link java.lang.String} object.
     * @param resultLimit a {@link java.lang.Integer} object.
     * @param reverseOrder a {@link java.lang.Boolean} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<IFacetItem> getStaticDrillDown(String field, String subQuery, Integer resultLimit, final Boolean reverseOrder)
            throws PresentationException, IndexUnreachableException {
        StringBuilder sbQuery = new StringBuilder(100);
        sbQuery.append(SearchHelper.ALL_RECORDS_QUERY)
                .append(SearchHelper.getAllSuffixes(BeanUtils.getRequest(), true, true));

        if (StringUtils.isNotEmpty(subQuery)) {
            if (subQuery.startsWith(" AND ")) {
                subQuery = subQuery.substring(5);
            }
            sbQuery.append(" AND (").append(subQuery).append(')');
        }
        // logger.debug("getDrillDown query: " + query);
        field = SearchHelper.facetifyField(field);
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .search(sbQuery.toString(), 0, 0, null, Collections.singletonList(field), Collections.singletonList(SolrConstants.IDDOC));
        if (resp == null || resp.getFacetField(field) == null || resp.getFacetField(field).getValues() == null) {
            return Collections.emptyList();
        }

        Map<String, Long> result =
                resp.getFacetField(field).getValues().stream().filter(count -> count.getName().charAt(0) != 1).sorted((count1, count2) -> {
                    int compValue;
                    if (count1.getName().matches("\\d+") && count2.getName().matches("\\d+")) {
                        compValue = Long.compare(Long.parseLong(count1.getName()), Long.parseLong(count2.getName()));
                    } else {
                        compValue = count1.getName().compareToIgnoreCase(count2.getName());
                    }
                    if (Boolean.TRUE.equals(reverseOrder)) {
                        compValue *= -1;
                    }
                    return compValue;
                })
                        .limit(resultLimit > 0 ? resultLimit : resp.getFacetField(field).getValues().size())
                        .collect(Collectors.toMap(Count::getName, Count::getCount));
        List<String> hierarchicalFields = DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields();
        Locale locale = null;
        NavigationHelper nh = BeanUtils.getNavigationHelper();
        if (nh != null) {
            locale = nh.getLocale();
        }

        return FacetItem.generateFacetItems(field, result, true, reverseOrder, hierarchicalFields.contains(field) ? true : false, locale);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#isSearchPerformed()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isSearchPerformed() {
        return currentSearch != null;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.SearchInterface#isExplicitSearchPerformed()
     */
    /** {@inheritDoc} */
    @Override
    public boolean isExplicitSearchPerformed() {
        return StringUtils.isNotBlank(getExactSearchString().replace("-", ""));
    }

    /**
     * <p>
     * isShowReducedSearchOptions.
     * </p>
     *
     * @return the showReducedSearchOptions
     */
    public boolean isShowReducedSearchOptions() {
        return showReducedSearchOptions;
    }

    /**
     * <p>
     * Setter for the field <code>showReducedSearchOptions</code>.
     * </p>
     *
     * @param showReducedSearchOptions the showReducedSearchOptions to set
     */
    public void setShowReducedSearchOptions(boolean showReducedSearchOptions) {
        this.showReducedSearchOptions = showReducedSearchOptions;
    }

    /**
     * <p>
     * setFirstQueryItemValue.
     * </p>
     *
     * @param value a {@link java.lang.String} object.
     */
    public void setFirstQueryItemValue(String value) {
        this.advancedQueryGroups.get(0).getQueryItems().get(0).setValue(value);
    }

    /**
     * <p>
     * getFirstQueryItemValue.
     * </p>
     */
    public void getFirstQueryItemValue() {
        this.advancedQueryGroups.get(0).getQueryItems().get(0).getValue();
    }

    /**
     * <p>
     * setBookmarkListName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setBookmarkListName(String name) {
        SearchQueryItem item = this.advancedQueryGroups.get(0).getQueryItems().get(0);
        item.setValue(name);
        item.setField(SolrConstants.BOOKMARKS);
        item.setOperator(SearchItemOperator.IS);

    }

    /**
     * <p>
     * getBookmarkListName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBookmarkListName() {
        String value = this.advancedQueryGroups.stream()
                .flatMap(group -> group.getQueryItems().stream())
                .filter(item -> item.getField() != null && item.getField().equals(SolrConstants.BOOKMARKS))
                .filter(item -> item.getValue() != null && !item.getValue().startsWith("KEY::"))
                .findFirst()
                .map(SearchQueryItem::getValue)
                .orElse("");
        return value;
    }

    /**
     * <p>
     * setBookmarkListName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setBookmarkListSharedKey(String key) {
        SearchQueryItem item = this.advancedQueryGroups.get(0).getQueryItems().get(0);
        item.setValue("KEY::" + key);
        item.setField(SolrConstants.BOOKMARKS);
        item.setOperator(SearchItemOperator.IS);

    }

    /**
     * <p>
     * getBookmarkListName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBookmarkListSharedKey() {
        String value = this.advancedQueryGroups.stream()
                .flatMap(group -> group.getQueryItems().stream())
                .filter(item -> item.getField() != null && item.getField().equals(SolrConstants.BOOKMARKS))
                .filter(item -> item.getValue() != null && item.getValue().startsWith("KEY::"))
                .findFirst()
                .map(SearchQueryItem::getValue)
                .orElse("");
        return value.replace("KEY::", "");
    }

    public String searchInRecord(String queryField, String queryValue) {

        this.getAdvancedQueryGroups().get(0).getQueryItems().get(0).setField(queryField);
        if (StringUtils.isNotBlank(queryValue)) {
            this.getAdvancedQueryGroups().get(0).getQueryItems().get(0).setValue(queryValue);
        }
        this.getAdvancedQueryGroups().get(0).getQueryItems().get(0).setOperator(SearchItemOperator.IS);
        this.getAdvancedQueryGroups().get(0).getQueryItems().get(1).setField("searchAdvanced_allFields");
        this.getAdvancedQueryGroups().get(0).getQueryItems().get(1).setOperator(SearchItemOperator.AUTO);
        this.setActiveSearchType(1);

        return this.searchAdvanced();
    }

    public boolean isSolrIndexReachable() {
        return DataManager.getInstance().getSearchIndex().pingSolrIndex();
    }

    public boolean hasGeoLocationHits() {
        return this.currentSearch != null && !this.currentSearch.getHitsLocationList().isEmpty();
    }

    public List<String> getHitsLocations() {
        if(this.currentSearch != null) {
            return this.currentSearch.getHitsLocationList().stream().map(l -> l.getGeoJson()).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
    
    public GeoMap getHitsMap() {
        GeoMap map = new GeoMap();
        map.setType(GeoMapType.MANUAL);
        map.setShowPopover(true);
        //set initial zoom to max zoom so map will be as zoomed in as possible
        map.setInitialView("{" +
                "\"zoom\": 5," +
                "\"center\": [11.073397, -49.451993]" +
                "}");
        List<String> features = new ArrayList<>();
        if (this.currentSearch != null) {

            for (Location location : this.currentSearch.getHitsLocationList()) {
                features.add(location.getGeoJson());
            }
            map.setFeatures(features);
        }
        return map;
    }

    /**
     * 
     * @param fieldName
     * @return
     */
    public String facetifyField(String fieldName) {
        return SearchHelper.facetifyField(fieldName);
    }
}
