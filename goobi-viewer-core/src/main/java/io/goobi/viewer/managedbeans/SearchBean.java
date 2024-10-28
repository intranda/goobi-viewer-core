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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.model.tasks.Task;
import io.goobi.viewer.api.rest.model.tasks.TaskParameter;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.export.ExcelExport;
import io.goobi.viewer.model.export.RISExport;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.maps.Location;
import io.goobi.viewer.model.maps.ManualFeatureSet;
import io.goobi.viewer.model.search.AdvancedSearchFieldConfiguration;
import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.search.FacetItem;
import io.goobi.viewer.model.search.IFacetItem;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchFilter;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.search.SearchInterface;
import io.goobi.viewer.model.search.SearchQueryGroup;
import io.goobi.viewer.model.search.SearchQueryItem;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.search.SearchSortingOption;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.collections.BrowseDcElement;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;

/**
 * SearchBean
 */
@Named
@SessionScoped
public class SearchBean implements SearchInterface, Serializable {

    private static final long serialVersionUID = 6962223613432267768L;

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SearchBean.class);

    /** Constant <code>URL_ENCODING="UTF8"</code> */
    public static final String URL_ENCODING = "UTF8";

    private static final String PREFIX_KEY = "KEY::";

    private static final boolean FUZZY_SEARCH_ENABLED_INITIAL = false;

    private static final String LOG_SEARCH_TYPE_DISABLED = "Cannot set search type {} because it's disabled.";

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private BreadcrumbBean breadcrumbBean;
    @Inject
    private UserBean userBean;

    /** Max number of search hits to be displayed on one page. */
    private int hitsPerPage = DataManager.getInstance().getConfiguration().getSearchHitsPerPageDefaultValue();
    /** Variable is set to true if the use manually changes the value. Used so that the default CMS setting doesn't override the value. */
    private boolean hitsPerPageSetterCalled = false;
    /**
     * Currently selected search type (regular, advanced, timeline, ...). This property is not private so it can be altered in unit tests (the setter
     * checks the config and may prevent setting certain values).
     */
    private int activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
    /** Currently selected filter for the regular search. Possible values can be configured. */
    private SearchFilter currentSearchFilter = DataManager.getInstance().getConfiguration().getDefaultSearchFilter();
    /** Solr query generated from the user's input (does not include facet filters or blacklists). */
    private String searchStringInternal = "";
    /** User-entered search query that is displayed in the search field after the search. */
    private String searchString = "";
    /** Individual terms extracted from the user query (used for highlighting). */
    private Map<String, Set<String>> searchTerms = new HashMap<>();

    private SearchResultGroup activeResultGroup;
    /** Selected advanced search field configuration template. */
    private String advancedSearchFieldTemplate = StringConstants.DEFAULT_NAME;
    private boolean phraseSearch = false;
    /** Current search result page. */
    private int currentPage = 1;
    /** Index of the currently open search result (used for search result browsing). */
    private int currentHitIndex = -1;
    /** Number by which currentHitIndex shall be increased or decreased. */
    private int hitIndexOperand = 0;
    private SearchFacets facets = new SearchFacets();
    /** User-selected Solr field name by which the search results shall be sorted. A leading exclamation mark means descending sorting. */
    private SearchSortingOption searchSortingOption;
    /** Keep lists of select values, once generated, for performance reasons. */
    private final Map<String, List<StringPair>> advancedSearchSelectItems = new HashMap<>();
    /** Group of query item clusters for the advanced search. */
    private final SearchQueryGroup advancedSearchQueryGroup =
            new SearchQueryGroup(
                    DataManager.getInstance()
                            .getConfiguration()
                            .getAdvancedSearchFields(advancedSearchFieldTemplate, true, BeanUtils.getLocale().getLanguage()),
                    advancedSearchFieldTemplate);
    /** Human-readable representation of the advanced search query for displaying. */
    private String advancedSearchQueryInfo;

    private String searchInCurrentItemString;
    /** Current search object. Contains the results and can be used to persist search parameters in the DB. */
    private Search currentSearch;
    /** If >0, proximity search will be applied to phrase searches. */
    private int proximitySearchDistance = 0;
    /** Fuzzy search switch. */
    private boolean fuzzySearchEnabled = FUZZY_SEARCH_ENABLED_INITIAL;

    private volatile FutureTask<Boolean> downloadReady; //NOSONAR   Future is thread-save
    private volatile FutureTask<Boolean> downloadComplete; //NOSONAR   Future is thread-save

    /** Reusable Random object. */
    private Random random = new SecureRandom();

    /**
     * The current {@link ViewerPath} at the time {@link #executeSearch()} was last called. Used when returning to search list from record via the
     * widget_searchResultNavigation widget
     */
    private Optional<ViewerPath> lastUsedSearchPage = Optional.empty();

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
        resetAdvancedSearchParameters();
    }

    /**
     * Required setter for ManagedProperty injection for unit tests.
     *
     * @param navigationHelper the navigationHelper to set
     */
    public void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    /**
     * Getter for unit tests.
     * 
     * @return the advancedSearchSelectItems
     */
    Map<String, List<StringPair>> getAdvancedSearchSelectItems() {
        return advancedSearchSelectItems;
    }

    /**
     * <p>
     * clearSearchItemLists.
     * </p>
     *
     * @should clear map correctly
     */
    public void clearSearchItemLists() {
        advancedSearchSelectItems.clear();
    }

    /**
     * Dummy method for component cross-compatibility with CMS searches.
     *
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException
     */
    public String search() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return search("");
    }

    /**
     * Executes the search using already set parameters. Usually called from Pretty URLs.
     *
     * @param filterQuery a {@link java.lang.String} object
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String search(String filterQuery) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("search");
        if (breadcrumbBean != null) {
            breadcrumbBean.updateBreadcrumbsForSearchHits(StringTools.decodeUrl(facets.getActiveFacetString()));
        }
        resetSearchResults();
        executeSearch(filterQuery);

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
     * @return Navigation outcome
     */
    public String searchSimple(boolean resetParameters) {
        return searchSimple(resetParameters, true);
    }

    /**
     * Action method for search buttons (simple search) with an option to reset search parameters and active facets.
     *
     * @param resetParameters a boolean.
     * @param resetFacets a boolean.
     * @return Navigation outcome
     * @should not reset facets if resetFacets false
     * @should not produce results if search terms not in index
     */
    public String searchSimple(boolean resetParameters, boolean resetFacets) {
        logger.trace("searchSimple");
        resetSearchResults();
        if (resetParameters) {
            resetSearchParameters();
            facets.resetSliderRange();
            setActiveResultGroupName("-");
        }
        if (resetFacets) {
            facets.resetActiveFacetString();
        }
        generateSimpleSearchString(searchString);
        return StringConstants.PRETTY_NEWSEARCH5;
    }

    /**
     * <p>
     * simpleSearch.
     * </p>
     *
     * @param search a {@link io.goobi.viewer.model.search.SearchInterface} object
     * @return Navigation outcome
     */
    public String simpleSearch(SearchInterface search) {
        return search.searchSimple();
    }

    /**
     * Same as <code>searchSimple()</code> but resets the current facets.
     *
     * @return Navigation outcome
     */
    public String searchSimpleResetCollections() {
        facets.resetActiveFacetString();
        return searchSimple(true, true);
    }

    /**
     * Same as <code>{@link #searchSimple()}</code> but sets the current facets to the given string
     *
     * @param facetString a {@link java.lang.String} object.
     * @return Navigation outcome
     */
    public String searchSimpleSetFacets(String facetString) {
        // logger.trace("searchSimpleSetFacets:{}", facetString); //NOSONAR Debug
        facets.resetActiveFacetString();
        facets.setActiveFacetString(facetString);
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
     * @return Navigation outcome
     * @should generate search string correctly
     * @should reset search parameters
     */
    public String searchAdvanced(boolean resetParameters) {
        logger.trace("searchAdvanced");
        resetSearchResults();
        if (resetParameters) {
            resetSearchParameters();
            facets.resetSliderRange();
        }
        searchStringInternal = generateAdvancedSearchString();

        return StringConstants.PRETTY_SEARCHADVANCED5;
    }

    /**
     * Search using currently set search string
     *
     * @return Navigation outcome
     * @should reset search results
     */
    public String searchDirect() {
        logger.trace("searchDirect");
        resetSearchResults();
        //facets.resetCurrentFacetString();
        return StringConstants.PRETTY_NEWSEARCH5;
    }

    /**
     * Executes a search for any content tagged with today's month and day.
     *
     * @return Navigation outcome
     * @should set search string correctly
     */
    public String searchToday() {
        logger.trace("searchToday");
        resetSearchResults();
        resetSearchParameters();
        facets.resetSliderRange();
        facets.resetActiveFacetString();
        generateSimpleSearchString(searchString);

        searchStringInternal = SolrConstants.MONTHDAY + ":" + DateTools.FORMATTERMONTHDAYONLY.format(LocalDateTime.now());

        return StringConstants.PRETTY_NEWSEARCH5;
    }

    /**
     * Action method for the "reset" button in search forms.
     *
     * @return Navigation outcome
     * @should return correct Pretty URL ID
     */
    public String resetSearchAction() {
        logger.trace("resetSearchAction");
        reset();

        // After resetting, return to the correct search entry page
        switch (activeSearchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return StringConstants.PREFIX_PRETTY + PageType.advancedSearch.getName();
            case SearchHelper.SEARCH_TYPE_CALENDAR:
                return StringConstants.PREFIX_PRETTY + PageType.searchCalendar.getName();
            case SearchHelper.SEARCH_TYPE_TERMS:
                return StringConstants.PREFIX_PRETTY + PageType.term.getName();
            default:
                return StringConstants.PREFIX_PRETTY + PageType.search.getName();
        }
    }

    /**
     * Same as {@link #resetSearchAction()} without the redirect
     */
    public void reset() {
        generateSimpleSearchString("");
        setCurrentPage(1);
        setExactSearchString("");
        mirrorAdvancedSearchCurrentHierarchicalFacets();
        resetSearchResults();
        resetSearchParameters(true, true);
        searchInCurrentItemString = null;
        proximitySearchDistance = 0;
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
        resetSearchParameters(false, true);
    }

    /**
     * <p>
     * resetSearchParameters.
     * </p>
     *
     * @param resetAllSearchTypes a boolean
     */
    public void resetSearchParameters(boolean resetAllSearchTypes) {
        resetSearchParameters(resetAllSearchTypes, true);
    }

    /**
     * Resets general search options and type specific options for currently unused types (all options if <resetAll> is true).
     *
     * @param resetAllSearchTypes If true, parameters for the currently used search type are also reset.
     * @param resetCurrentPage If true, currentPage will be reset to 1
     */
    public void resetSearchParameters(boolean resetAllSearchTypes, boolean resetCurrentPage) {
        logger.trace("resetSearchParameters; resetAllSearchTypes: {}", resetAllSearchTypes);
        CalendarBean calendarBean = BeanUtils.getCalendarBean();
        if (resetAllSearchTypes) {
            resetSimpleSearchParameters();
            resetAdvancedSearchParameters();
            this.fuzzySearchEnabled = FUZZY_SEARCH_ENABLED_INITIAL;
            if (calendarBean != null) {
                calendarBean.resetCurrentSelection();
            }
            setSortString("");
        } else {
            switch (activeSearchType) {
                case SearchHelper.SEARCH_TYPE_REGULAR:
                case SearchHelper.SEARCH_TYPE_TERMS:
                    resetAdvancedSearchParameters();
                    if (calendarBean != null) {
                        calendarBean.resetCurrentSelection();
                    }
                    setSortString("");
                    break;
                case SearchHelper.SEARCH_TYPE_ADVANCED:
                    resetSimpleSearchParameters();
                    if (calendarBean != null) {
                        calendarBean.resetCurrentSelection();
                    }
                    break;
                case SearchHelper.SEARCH_TYPE_TIMELINE:
                case SearchHelper.SEARCH_TYPE_CALENDAR:
                    resetSimpleSearchParameters();
                    resetAdvancedSearchParameters();
                    setSortString("");
                    break;
                default: // nothing
            }
        }
        if (resetCurrentPage) {
            setCurrentPage(1);
        }
    }

    /**
     * Resets search options for the simple search.
     *
     * @should reset variables correctly
     */
    protected void resetSimpleSearchParameters() {
        logger.trace("resetSimpleSearchParameters");
        currentSearchFilter = DataManager.getInstance().getConfiguration().getDefaultSearchFilter();
        generateSimpleSearchString("");

        searchString = "";
    }

    /**
     * Resets search options for the advanced search.
     *
     * @should reset variables correctly
     * @should re-select collection correctly
     */
    protected void resetAdvancedSearchParameters() {
        logger.trace("resetAdvancedSearchParameters");
        advancedSearchQueryGroup.init(
                DataManager.getInstance()
                        .getConfiguration()
                        .getAdvancedSearchFields(advancedSearchFieldTemplate, true, BeanUtils.getLocale().getLanguage()),
                advancedSearchFieldTemplate);
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
            resetAdvancedSearchParameters();
        }
    }

    /**
     * Generates a Solr query string out of advancedQueryItems (does not contains facets or blacklists).
     *
     * @return Generated query
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
    String generateAdvancedSearchString() {
        logger.trace("generateAdvancedSearchString");
        StringBuilder sb = new StringBuilder("(");
        StringBuilder sbInfo = new StringBuilder();
        searchTerms.clear();
        StringBuilder sbCurrentCollection = new StringBuilder();
        Set<String> usedHierarchicalFields = new HashSet<>();
        Set<String> usedFieldValuePairs = new HashSet<>();
        this.proximitySearchDistance = 0;
        for (SearchQueryItem queryItem : advancedSearchQueryGroup.getQueryItems()) {
            // logger.trace("Query item: {}", queryItem.toString()); //NOSONAR Debug
            if (StringUtils.isEmpty(queryItem.getField()) || StringUtils.isBlank(queryItem.getValue())) {
                continue;
            }
            if (sbInfo.length() > 1) {
                sbInfo.append(' ');
            }
            sbInfo.append(ViewerResourceBundle.getTranslation("searchOperator_" + queryItem.getOperator().name(),
                    BeanUtils.getLocale()))
                    .append(' ');

            // Generate the hierarchical facet parameter from query items
            if (queryItem.isHierarchical()) {
                // logger.trace("{} is hierarchical", queryItem.getField()); //NOSONAR Debug
                // Skip identical hierarchical items

                // Find existing facet items that can be re-purposed for the existing facets
                boolean skipQueryItem = false;
                for (IFacetItem facetItem : facets.getActiveFacets()) {
                    // logger.trace("checking facet item: {}", facetItem.getLink()); //NOSONAR Debug
                    if (!facetItem.getField().equals(queryItem.getField())) {
                        continue;
                    }
                    if (usedFieldValuePairs.contains(facetItem.getLink())) {
                        // logger.trace("facet item already handled: {}", facetItem.getLink()); //NOSONAR Debug
                        continue;
                    }
                    if (!usedFieldValuePairs.contains(queryItem.getField() + ":" + queryItem.getValue())) {
                        facetItem.setLink(queryItem.getField() + ":" + queryItem.getValue());
                        usedFieldValuePairs.add(facetItem.getLink());
                        usedHierarchicalFields.add(queryItem.getField());
                        // logger.trace("reuse facet item: {}", facetItem); //NOSONAR Debug
                        skipQueryItem = true;
                        break;
                    }
                }

                if (!skipQueryItem) {
                    String itemQuery =
                            new StringBuilder().append(queryItem.getField()).append(':').append(queryItem.getValue().trim()).toString();
                    // logger.trace("item query: {}", itemQuery); //NOSONAR Debug

                    // Check whether this combination already exists and skip, if that's the case
                    if (usedFieldValuePairs.contains(itemQuery)) {
                        // logger.trace("facet item already exists: {}", itemQuery); //NOSONAR Debug
                        continue;
                    }
                    usedFieldValuePairs.add(itemQuery);
                    usedHierarchicalFields.add(queryItem.getField());

                    sbCurrentCollection.append(itemQuery + ";;");

                    sbInfo.append('(')
                            .append(ViewerResourceBundle.getTranslation(queryItem.getField(), BeanUtils.getLocale()))
                            .append(": \"")
                            .append(ViewerResourceBundle.getTranslation(queryItem.getValue(), BeanUtils.getLocale()))
                            .append('"')
                            .append(')');
                }
                continue;
            }

            // Non-hierarchical fields
            if (searchTerms.get(SolrConstants.FULLTEXT) == null) {
                searchTerms.put(SolrConstants.FULLTEXT, new HashSet<>());
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
                itemQuery = queryItem.generateQuery(searchTerms.get(SolrConstants.FULLTEXT), true, fuzzySearchEnabled);
                this.proximitySearchDistance = Math.max(this.proximitySearchDistance, queryItem.getProximitySearchDistance());
            }

            logger.trace("Item query: {}", itemQuery);
            String infoFieldLabel =
                    SearchHelper.SEARCH_FILTER_ALL.getField().equals(queryItem.getField()) ? queryItem.getLabel() : queryItem.getField();
            sbInfo.append('(').append(ViewerResourceBundle.getTranslation(infoFieldLabel, BeanUtils.getLocale())).append(": ");
            switch (queryItem.getOperator()) {
                case AND:
                    if (SolrConstants.BOOKMARKS.equals(queryItem.getField()) && !userBean.isLoggedIn()) {
                        // Session bookmark list value
                        sbInfo.append(ViewerResourceBundle.getTranslation("bookmarkList_session", BeanUtils.getLocale()));
                    } else if (queryItem.isRange()) {
                        sbInfo.append('[').append(queryItem.getValue()).append(" - ").append(queryItem.getValue2()).append(']');
                    } else {
                        if (queryItem.isDisplaySelectItems()) {
                            sbInfo.append(ViewerResourceBundle.getTranslation(queryItem.getValue(), BeanUtils.getLocale()));
                        } else {
                            sbInfo.append(queryItem.getValue());
                        }
                    }
                    break;
                case NOT:
                    if (queryItem.isDisplaySelectItems()) {
                        sbInfo.append(ViewerResourceBundle.getTranslation(queryItem.getValue(), BeanUtils.getLocale()));
                    } else {
                        sbInfo.append(queryItem.getValue());
                    }
                    break;
                default:
                    if (queryItem.isRange()) {
                        sbInfo.append('[').append(queryItem.getValue()).append(" - ").append(queryItem.getValue2()).append(']');
                    } else {
                        if (queryItem.isDisplaySelectItems()) {
                            sbInfo.append(ViewerResourceBundle.getTranslation(queryItem.getValue(), BeanUtils.getLocale()));
                        } else {
                            sbInfo.append(queryItem.getValue());
                        }
                    }
            }
            sbInfo.append(')');

            // Add item query part to the group query
            if (itemQuery.length() > 0) {
                if (sb.length() > 1) {
                    // If this is not the first item, add the group's operator
                    sb.append(' ');
                }
                sb.append(itemQuery);
            }
        }

        // Clean up hierarchical facet items whose field has been matched to existing query items but not its value (obsolete facets)
        Set<IFacetItem> toRemove = new HashSet<>();
        for (IFacetItem facetItem : facets.getActiveFacets()) {
            if (facetItem.isHierarchial() && usedHierarchicalFields.contains(facetItem.getField())
                    && !usedFieldValuePairs.contains(facetItem.getLink())) {
                toRemove.add(facetItem);
            }
        }
        if (!toRemove.isEmpty()) {
            facets.getActiveFacets().removeAll(toRemove);
        }

        // Add this group's query part to the main query
        if (sb.length() > 0) {
            sb.append(')');
        }
        if (sb.toString().equals("()")) {
            sb.delete(0, 2);
        }
        if (sbCurrentCollection.length() > 0) {
            logger.trace("{} + {}", facets.getActiveFacetStringPrefix(), sbCurrentCollection);
            facets.setActiveFacetString(facets.getActiveFacetStringPrefix() + sbCurrentCollection.toString());
        } else {
            facets.setActiveFacetString(facets.getActiveFacetString());
        }

        advancedSearchQueryInfo = sbInfo.toString();
        // Quickfix for single hierarchical item query info having an opening parenthesis only
        if (advancedSearchQueryInfo.startsWith("(") && !advancedSearchQueryInfo.endsWith(")")) {
            advancedSearchQueryInfo = advancedSearchQueryInfo.substring(1);
        }
        logger.trace("query info: {}", advancedSearchQueryInfo);
        logger.debug("advanced query: {}", sb);

        return sb.toString();
    }

    /**
     * <p>
     * hitsPerPageListener.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void hitsPerPageListener()
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("hitsPerPageListener");
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
        executeSearch("");
    }

    /**
     * <p>
     * executeSearch.
     * </p>
     *
     * @param filterQuery a {@link java.lang.String} object
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void executeSearch(String filterQuery)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.debug("executeSearch; searchString: {}", searchStringInternal);
        mirrorAdvancedSearchCurrentHierarchicalFacets();

        // Create SearchQueryGroup from query
        if (activeSearchType == SearchHelper.SEARCH_TYPE_ADVANCED && advancedSearchQueryGroup.isBlank()) {
            SearchQueryGroup parsedGroup =
                    SearchHelper.parseSearchQueryGroupFromQuery(searchStringInternal.replace("\\", ""), facets.getActiveFacetString(),
                            advancedSearchFieldTemplate, navigationHelper.getLocaleString());
            advancedSearchQueryGroup.injectItems(parsedGroup.getQueryItems());
        }

        //remember the current page to return to hit list in widget_searchResultNavigation
        setLastUsedSearchPage();

        // If hitsPerPage is not one of the available values, reset to default
        if (!hitsPerPageSetterCalled && !DataManager.getInstance().getConfiguration().getSearchHitsPerPageValues().contains(hitsPerPage)) {
            hitsPerPage = DataManager.getInstance().getConfiguration().getSearchHitsPerPageDefaultValue();
            logger.trace("hitsPerPage reset to {}", hitsPerPage);
        }
        // setHitsPerPageSetterCalled(false);

        if (searchSortingOption != null && StringUtils.isEmpty(searchSortingOption.getSortString())) {
            setSortString(DataManager.getInstance().getConfiguration().getDefaultSortField(BeanUtils.getLocale().getLanguage()));
            logger.trace("Using default sorting: {}", searchSortingOption.getSortString());
        }

        // Init search object
        currentSearch = new Search(activeSearchType, currentSearchFilter, getResultGroupsForSearchExecution());
        currentSearch.setUserInput(searchString);
        currentSearch.setQuery(searchStringInternal);
        currentSearch.setPage(currentPage);
        currentSearch.setSortString(searchSortingOption != null ? searchSortingOption.getSortString() : null);
        currentSearch.setFacetString(facets.getActiveFacetString());
        currentSearch.setCustomFilterQuery(filterQuery);
        currentSearch.setProximitySearchDistance(proximitySearchDistance);

        // When searching in MONTHDAY, add a term so that an expand query is created
        if (searchStringInternal.startsWith(SolrConstants.MONTHDAY)) {
            searchTerms.put(SolrConstants.MONTHDAY, Collections.singleton(searchStringInternal.substring(SolrConstants.MONTHDAY.length() + 1)));
            if (logger.isTraceEnabled()) {
                logger.trace("monthday terms: {}", searchTerms.get(SolrConstants.MONTHDAY).iterator().next());
            }
        }

        // Add search hit aggregation parameters, if enabled
        if (!searchTerms.isEmpty()) {
            List<String> additionalExpandQueryfields = new ArrayList<>();
            // Add MONTHDAY to the list of expand query fields
            if (searchStringInternal.startsWith(SolrConstants.MONTHDAY)) {
                additionalExpandQueryfields.add(SolrConstants.MONTHDAY);
            }
            // If no user input available, add terms fields to additional expand query fields
            if (StringUtils.isEmpty(searchString)) {
                for (String key : searchTerms.keySet()) {
                    if (!SearchHelper.TITLE_TERMS.equals(key)) {
                        additionalExpandQueryfields.add(key);
                    }
                }
            }
            String expandQuery = activeSearchType == 1
                    ? SearchHelper.generateAdvancedExpandQuery(advancedSearchQueryGroup, fuzzySearchEnabled)
                    : SearchHelper.generateExpandQuery(
                            SearchHelper.getExpandQueryFieldList(activeSearchType, currentSearchFilter, advancedSearchQueryGroup,
                                    additionalExpandQueryfields),
                            searchTerms, phraseSearch, proximitySearchDistance);
            if (StringUtils.isEmpty(expandQuery) && activeSearchType == SearchHelper.SEARCH_TYPE_TERMS) {
                expandQuery = searchStringInternal;
            }
            currentSearch.setExpandQuery(expandQuery);
        }

        // Override default result groups config if active group selected
        if (activeResultGroup != null) {
            currentSearch.setResultGroups(Collections.singletonList(activeResultGroup));
        }

        currentSearch.execute(facets, searchTerms, hitsPerPage, navigationHelper.getLocale());
    }

    /**
     * Set the current {@link io.goobi.viewer.model.urlresolution.ViewerPath} as the {@link #lastUsedSearchPage}. This is where returning to search
     * hit list from record will direct to
     */
    public void setLastUsedSearchPage() {
        this.lastUsedSearchPage = ViewHistory.getCurrentView(BeanUtils.getRequest());
    }

    /**
     * <p>
     * getFinalSolrQuery.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getFinalSolrQuery() {
        if (this.currentSearch != null) {
            return this.currentSearch.generateFinalSolrQuery(null);
        }

        return new Search().generateFinalSolrQuery(null);
    }

    /**
     * <p>
     * getFilterQueries.
     * </p>
     *
     * @return a {@link java.util.List} object
     */
    public List<String> getFilterQueries() {
        List<String> queries = new ArrayList<>();
        if (this.currentSearch != null) {
            String customQuery = this.currentSearch.getCustomFilterQuery();
            if (StringUtils.isNotBlank(customQuery)) {
                queries.add(customQuery);
            }
        }
        if (this.facets != null) {
            List<String> facetQueries = this.facets.generateFacetFilterQueries(true);
            queries.addAll(facetQueries);
        }
        return queries;
    }

    /**
     * <p>
     * getCombinedFilterQuery.
     * </p>
     *
     * @return Generated query
     */
    public String getCombinedFilterQuery() {
        String query = "";
        if (this.currentSearch != null) {
            String customQuery = this.currentSearch.getCustomFilterQuery();
            if (StringUtils.isNotBlank(customQuery)) {
                query += " +(" + customQuery + ")";
            }
        }
        if (this.facets != null) {
            List<String> facetQueries = this.facets.generateFacetFilterQueries(true);
            String facetQuery = StringUtils.join(facetQueries, SolrConstants.SOLR_QUERY_AND);
            if (StringUtils.isNotBlank(facetQuery)) {
                query += " +(" + facetQuery + ")";
            }
        }
        return query;
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
                        logger.debug(LOG_SEARCH_TYPE_DISABLED, activeSearchType);
                    }
                    break;
                case 2:
                    if (DataManager.getInstance().getConfiguration().isTimelineSearchEnabled()) {
                        this.activeSearchType = activeSearchType;
                    } else {
                        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
                        logger.debug(LOG_SEARCH_TYPE_DISABLED, activeSearchType);
                    }
                    break;
                case 3:
                    if (DataManager.getInstance().getConfiguration().isCalendarSearchEnabled()) {
                        this.activeSearchType = activeSearchType;
                    } else {
                        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
                        logger.debug(LOG_SEARCH_TYPE_DISABLED, activeSearchType);
                    }
                    break;
                default:
                    this.activeSearchType = activeSearchType;
            }
            // Resetting facet string here will result in collection listings returning all records in the index, if the collection page doesn't set
            // activeSearchType=0 beforehand
            // facets.resetActiveFacetString();
        }
        logger.trace("activeSearchType: {}", activeSearchType);
    }

    /**
     * For unit tests, only sets the value.
     * 
     * @param activeSearchType
     */
    void setActiveSearchTypeTest(int activeSearchType) {
        this.activeSearchType = activeSearchType;
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
        return SearchHelper.searchAutosuggestion(suggest, facets.getActiveFacets());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSearchInDcFlag() {
        for (IFacetItem item : facets.getActiveFacets()) {
            if (item.getField().equals(SolrConstants.DC)) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSearchInFacetFieldFlag(String fieldName) {
        for (IFacetItem item : facets.getActiveFacets()) {
            if (item.getField().equals(fieldName)) {
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
     * @should generate phrase search query without filter correctly
     * @should generate phrase search query with specific filter correctly
     * @should generate non-phrase search query without filter correctly
     * @should generate non-phrase search query with specific filter correctly
     * @should add proximity search token correctly
     * @should reset exactSearchString if input empty
     */
    void generateSimpleSearchString(final String inSearchString) {
        logger.trace("generateSimpleSearchString: {}", inSearchString);
        logger.trace("currentSearchFilter: {}", currentSearchFilter.getLabel());
        String tempSearchString = inSearchString;
        if (tempSearchString == null) {
            tempSearchString = "";
        }
        try {
            tempSearchString = URLDecoder.decode(tempSearchString, URL_ENCODING);
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            logger.warn(e.getMessage());
        }
        if ("-".equals(tempSearchString)) {
            searchString = "";
        }

        searchString = StringTools.stripJS(tempSearchString).trim();
        if (StringUtils.isEmpty(tempSearchString)) {
            searchString = "";
            setExactSearchString("");
            return;
        }

        // Reset internal query etc. only after confirming the given search string is not empty
        searchStringInternal = "";
        searchTerms.clear();
        phraseSearch = false;

        if ("*".equals(tempSearchString)) {
            searchStringInternal = SearchHelper.prepareQuery("");
            setExactSearchString("");
            return;
        }

        tempSearchString = tempSearchString.replace(SolrConstants.SOLR_QUERY_OR, " || ");
        tempSearchString = tempSearchString.replace(SolrConstants.SOLR_QUERY_AND, " && ");
        tempSearchString = tempSearchString.toLowerCase(); // Regular tokens are lowercase

        if (tempSearchString.contains("\"")) {
            // Phrase search
            phraseSearch = true;
            // Determine proximity search distance if token present, then remove it from the term
            proximitySearchDistance = SearchHelper.extractProximitySearchDistanceFromQuery(tempSearchString);
            if (proximitySearchDistance > 0) {
                tempSearchString = SearchHelper.removeProximitySearchToken(tempSearchString);
            }
            String[] toSearch = tempSearchString.split("\"");
            StringBuilder sb = new StringBuilder();
            for (String p : toSearch) {
                String phrase = p.replace("\"", "");
                if (phrase.length() > 0) {
                    if (currentSearchFilter == null || currentSearchFilter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                        // For aggregated searches include both SUPER and regular DEFAULT/FULLTEXT fields
                        sb.append(SolrConstants.SUPERDEFAULT).append(":(\"").append(phrase).append("\") OR ");
                        sb.append(SolrConstants.SUPERFULLTEXT).append(":(\"").append(phrase).append('"');
                        if (proximitySearchDistance > 0) {
                            // Proximity search term augmentation
                            sb.append('~').append(proximitySearchDistance);
                        }
                        sb.append(')').append(SolrConstants.SOLR_QUERY_OR);
                        sb.append(SolrConstants.SUPERUGCTERMS).append(":(\"").append(phrase).append("\") OR ");
                        sb.append(SolrConstants.SUPERSEARCHTERMS_ARCHIVE).append(":(\"").append(phrase).append("\") OR ");
                        sb.append(SolrConstants.DEFAULT).append(":(\"").append(phrase).append("\") OR ");
                        sb.append(SolrConstants.FULLTEXT).append(":(\"").append(phrase).append('"');
                        if (proximitySearchDistance > 0) {
                            // Proximity search term augmentation
                            sb.append('~').append(proximitySearchDistance);
                        }
                        sb.append(')').append(SolrConstants.SOLR_QUERY_OR);
                        sb.append(SolrConstants.NORMDATATERMS).append(":(\"").append(phrase).append("\") OR ");
                        sb.append(SolrConstants.UGCTERMS).append(":(\"").append(phrase).append("\") OR ");
                        sb.append(SolrConstants.SEARCHTERMS_ARCHIVE).append(":(\"").append(phrase).append("\") OR ");
                        sb.append(SolrConstants.CMS_TEXT_ALL).append(":(\"").append(phrase).append("\")");
                    } else {
                        // Specific filter selected
                        switch (currentSearchFilter.getField()) {
                            case SolrConstants.DEFAULT:
                                sb.append(SolrConstants.SUPERDEFAULT).append(":(\"").append(phrase).append("\") OR ");
                                sb.append(SolrConstants.DEFAULT).append(":(\"").append(phrase).append("\")");
                                break;
                            case SolrConstants.FULLTEXT:
                                sb.append(SolrConstants.SUPERFULLTEXT)
                                        .append(":(\"")
                                        .append(phrase)
                                        .append('"');
                                if (proximitySearchDistance > 0) {
                                    // Proximity search term augmentation
                                    sb.append('~').append(proximitySearchDistance);
                                }
                                sb.append(')')
                                        .append(SolrConstants.SOLR_QUERY_OR)
                                        .append(SolrConstants.FULLTEXT)
                                        .append(":(\"")
                                        .append(phrase)
                                        .append('"');
                                if (proximitySearchDistance > 0) {
                                    // Proximity search term augmentation
                                    sb.append('~').append(proximitySearchDistance);
                                }
                                sb.append(')');
                                break;
                            case SolrConstants.UGCTERMS:
                                sb.append(SolrConstants.SUPERUGCTERMS).append(":(\"").append(phrase).append("\") OR ");
                                sb.append(SolrConstants.UGCTERMS).append(":(\"").append(phrase).append("\")");
                                break;
                            case SolrConstants.SEARCHTERMS_ARCHIVE:
                                sb.append(SolrConstants.SUPERSEARCHTERMS_ARCHIVE).append(":(\"").append(phrase).append("\") OR ");
                                sb.append(SolrConstants.SEARCHTERMS_ARCHIVE).append(":(\"").append(phrase).append("\")");
                                break;
                            default:
                                sb.append(currentSearchFilter.getField()).append(":(\"").append(phrase).append("\")");
                                break;
                        }
                    }
                    sb.append(SolrConstants.SOLR_QUERY_AND);
                }
            }
            searchStringInternal = sb.toString();
        } else {
            // Non-phrase search
            tempSearchString = tempSearchString.replace(" &&", "");
            String[] termsSplit = tempSearchString.split(SearchHelper.SEARCH_TERM_SPLIT_REGEX);

            // Clean up terms and create OR-connected groups
            List<String> preparedTerms = new ArrayList<>(termsSplit.length);
            for (int i = 0; i < termsSplit.length; ++i) {
                String term = termsSplit[i].trim();
                term = SearchHelper.cleanUpSearchTerm(term);
                String unescapedTerm = term;
                term = term.replace("\\*", "*"); // unescape falsely escaped truncation
                if (term.length() > 0 && !DataManager.getInstance().getConfiguration().getStopwords().contains(term)) {
                    if (fuzzySearchEnabled) {
                        // Fuzzy search term augmentation
                        String[] wildcards = SearchHelper.getWildcardsTokens(term);
                        term = SearchHelper.addFuzzySearchToken(wildcards[1], wildcards[0], wildcards[2]);
                    }
                    logger.trace("term: {}", term);
                    if (!"\\|\\|".equals(term)) {
                        // Avoid duplicate terms
                        if (!preparedTerms.contains(term)) {
                            preparedTerms.add(term);
                        }
                        for (Entry<String, Set<String>> entry : searchTerms.entrySet()) {
                            entry.getValue().add(unescapedTerm);
                        }
                    } else if (i > 0 && i < termsSplit.length - 1) {
                        // Two terms separated by OR: remove previous term and add it together with the next term as a group
                        int previousIndex = preparedTerms.size() - 1;
                        String prevTerm = preparedTerms.get(previousIndex);
                        String unescapedNextTerm = SearchHelper.cleanUpSearchTerm(termsSplit[i + 1]);
                        String nextTerm = ClientUtils.escapeQueryChars(unescapedNextTerm);
                        nextTerm = nextTerm.replace("\\*", "*"); // unescape falsely escaped truncation
                        preparedTerms.remove(previousIndex);
                        preparedTerms.add(prevTerm + " OR " + nextTerm);
                        for (Entry<String, Set<String>> entry : searchTerms.entrySet()) {
                            entry.getValue().add(unescapedNextTerm);
                        }
                        i++;
                    }
                }
            }
            // Construct inner query part
            String innerQuery = SearchHelper.buildTermQuery(preparedTerms);
            if (innerQuery.length() > 0) {
                StringBuilder sbOuter = new StringBuilder();
                if (currentSearchFilter == null || currentSearchFilter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                    // No filters defined or ALL
                    sbOuter.append(SolrConstants.SUPERDEFAULT).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.SUPERFULLTEXT).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.SUPERUGCTERMS).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.SUPERSEARCHTERMS_ARCHIVE).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.DEFAULT).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.FULLTEXT).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.NORMDATATERMS).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.UGCTERMS).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.SEARCHTERMS_ARCHIVE).append(":(").append(innerQuery);
                    sbOuter.append(") ").append(SolrConstants.CMS_TEXT_ALL).append(":(").append(innerQuery).append(')');
                } else {
                    // Specific filter selected
                    switch (currentSearchFilter.getField()) {
                        case SolrConstants.DEFAULT:
                            sbOuter.append(SolrConstants.SUPERDEFAULT)
                                    .append(":(")
                                    .append(innerQuery)
                                    .append(')')
                                    .append(SolrConstants.SOLR_QUERY_OR);
                            sbOuter.append(SolrConstants.DEFAULT).append(":(").append(innerQuery).append(')');
                            break;
                        case SolrConstants.FULLTEXT:
                            sbOuter.append(SolrConstants.SUPERFULLTEXT)
                                    .append(":(")
                                    .append(innerQuery)
                                    .append(')')
                                    .append(SolrConstants.SOLR_QUERY_OR);
                            sbOuter.append(SolrConstants.FULLTEXT).append(":(").append(innerQuery).append(')');
                            break;
                        case SolrConstants.UGCTERMS:
                            sbOuter.append(SolrConstants.SUPERUGCTERMS)
                                    .append(":(")
                                    .append(innerQuery)
                                    .append(')')
                                    .append(SolrConstants.SOLR_QUERY_OR);
                            sbOuter.append(SolrConstants.UGCTERMS).append(":(").append(innerQuery).append(')');
                            break;
                        case SolrConstants.SEARCHTERMS_ARCHIVE:
                            sbOuter.append(SolrConstants.SUPERSEARCHTERMS_ARCHIVE)
                                    .append(":(")
                                    .append(innerQuery)
                                    .append(')')
                                    .append(SolrConstants.SOLR_QUERY_OR);
                            sbOuter.append(SolrConstants.SEARCHTERMS_ARCHIVE).append(":(").append(innerQuery).append(')');
                            break;
                        default:
                            sbOuter.append(currentSearchFilter.getField()).append(":(").append(innerQuery).append(')');
                            break;
                    }
                }
                searchStringInternal += sbOuter.toString();
            }

        }
        if (searchStringInternal.endsWith(SolrConstants.SOLR_QUERY_OR)) {
            searchStringInternal = searchStringInternal.substring(0, searchStringInternal.length() - 4);
        } else if (searchStringInternal.endsWith(SolrConstants.SOLR_QUERY_AND)) {
            searchStringInternal = searchStringInternal.substring(0, searchStringInternal.length() - 5);
        }

        logger.trace("search string: {}", searchStringInternal);
        // logger.trace("search terms: {}", searchTerms.toString()); //NOSONAR Debug
    }

    /**
     * {@inheritDoc}
     * 
     * @should escape critical chars
     * @should url escape string
     */
    @Override
    public String getExactSearchString() {
        // logger.trace("getExactSearchString: {}", searchStringInternal); //NOSONAR Debug
        if (searchStringInternal.length() == 0) {
            return "-";
        }

        String ret = BeanUtils.escapeCriticalUrlChracters(searchStringInternal);
        try {
            // Escape the query here, otherwise Rewrite will spam warnings into catalina.out
            if (!StringTools.isStringUrlEncoded(ret, URL_ENCODING)) {
                // logger.trace("url pre-encoding: {}", ret); //NOSONAR Debug
                ret = StringTools.encodeUrl(ret);
                // logger.trace("url encoded: {}", ret); //NOSONAR Debug
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
        }
        return ret;
    }

    /**
     * Sets the current <code>searchStringInternal</code> to the given query, without parsing it like the regular setSearchString() method. This
     * method performs URL-unescaping, so using it directly with unescaped queries containing '+' etc. will change the logic.
     *
     * @param inSearchString a {@link java.lang.String} object.
     * @should perform double unescaping if necessary
     */
    public void setExactSearchString(final String inSearchString) {
        logger.debug("setExactSearchString: {}", inSearchString);
        String tempSearchString = inSearchString;
        if ("-".equals(tempSearchString)) {
            tempSearchString = "";
            searchString = "";
        }
        searchStringInternal = tempSearchString;
        // First apply regular URL decoder
        try {
            searchStringInternal = URLDecoder.decode(tempSearchString, URL_ENCODING);
            // Second decoding pass in case the query was encoded twice
            if (StringTools.isStringUrlEncoded(searchStringInternal, URL_ENCODING)) {
                searchStringInternal = URLDecoder.decode(searchStringInternal, URL_ENCODING);
            }
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            logger.error(e.getMessage());
        }
        // Then unescape custom sequences
        searchStringInternal = StringTools.unescapeCriticalUrlChracters(searchStringInternal);

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
     * @derecated user SearchBean.getExactSearchString()
     */
    @Deprecated(since = "24.01")
    public String getExactSearchStringResetGui() {
        return getExactSearchString();
    }

    /**
     * For unit tests.
     * 
     * @return the searchStringInternal
     */
    String getSearchStringInternal() {
        return searchStringInternal;
    }

    /**
     * For unit tests.
     * 
     * @param searchStringInternal the searchStringInternal to set
     */
    void setSearchStringInternal(String searchStringInternal) {
        this.searchStringInternal = searchStringInternal;
    }

    /** {@inheritDoc} */
    @Override
    public void setSortString(final String sortString) {
        logger.trace("setSortString: {}", sortString);
        String tempSortString = sortString;
        if (StringUtils.isEmpty(tempSortString) || "-".equals(tempSortString)) {
            String defaultSortField = DataManager.getInstance().getConfiguration().getDefaultSortField(BeanUtils.getLocale().getLanguage());
            if (StringUtils.isNotEmpty(defaultSortField)) {
                tempSortString = defaultSortField;
                logger.trace("Using default sort field: {}", defaultSortField);
            }
        }

        if (!"-".equals(tempSortString)) {
            if (SolrConstants.SORT_RANDOM.equalsIgnoreCase(tempSortString)) {
                tempSortString = new StringBuilder().append("random_").append(random.nextInt(Integer.MAX_VALUE)).toString();
            }
            SearchSortingOption option = new SearchSortingOption(tempSortString);
            option.setDefaultOption(StringUtils.isEmpty(sortString) || "-".equals(sortString)); // if the given sort string was empty, remember this
            setSearchSortingOption(option);
        } else {
            setSearchSortingOption(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getSortString() {
        // logger.trace("getSortString: {}", searchSortingOption); //NOSONAR Debug
        if (searchSortingOption == null) {
            setSortString("-");
        }

        if (searchSortingOption != null && StringUtils.isNotEmpty(searchSortingOption.getSortString())) {
            return searchSortingOption.getSortString();
        }

        return "-";
    }

    /**
     * <p>
     * Getter for the field <code>searchSortingOption</code>.
     * </p>
     *
     * @return the searchSortingOption
     */
    public SearchSortingOption getSearchSortingOption() {
        return searchSortingOption;
    }

    /**
     * <p>
     * Setter for the field <code>searchSortingOption</code>.
     * </p>
     *
     * @param searchSortingOption the searchSortingOption to set
     */
    public void setSearchSortingOption(SearchSortingOption searchSortingOption) {
        logger.trace("setSearchSortingOption: {}", searchSortingOption);
        this.searchSortingOption = searchSortingOption;
        // Sync with currentSearch, if available
        if (currentSearch != null) {
            currentSearch.setSortString(searchSortingOption != null ? searchSortingOption.getSortString() : null);
        }
    }

    /**
     * Returns relevant search result groups for search execution. If an active group is set, return just that. Otherwise, return either all
     * configured groups or default group (if groups disabled).
     *
     * @return Relevant search result groups
     */
    public List<SearchResultGroup> getResultGroupsForSearchExecution() {
        if (activeResultGroup != null) {
            return Collections.singletonList(activeResultGroup);
        }

        return (!DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled()
                || DataManager.getInstance().getConfiguration().getSearchResultGroups().isEmpty())
                        ? Collections.singletonList(SearchResultGroup.createDefaultGroup())
                        : DataManager.getInstance().getConfiguration().getSearchResultGroups();
    }

    /**
     * <p>
     * isDisplayResultGroupNames.
     * </p>
     *
     * @return true if activeResultGroup null; false otherwise
     */
    public boolean isDisplayResultGroupNames() {
        return activeResultGroup == null && DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled();
    }

    /**
     * <p>
     * getActiveResultGroupName.
     * </p>
     *
     * @return activeResultGroup name; "-" if none set
     */
    public String getActiveResultGroupName() {
        if (activeResultGroup != null) {
            return activeResultGroup.getName();
        }

        return "-";
    }

    /**
     * <p>
     * Setter for the field <code>activeResultGroup</code>.
     * </p>
     *
     * @param activeResultGroup a {@link io.goobi.viewer.model.search.SearchResultGroup} object
     */
    public void setActiveResultGroup(SearchResultGroup activeResultGroup) {
        this.activeResultGroup = activeResultGroup;
    }

    /**
     * <p>
     * setActiveResultGroupName.
     * </p>
     *
     * @param activeResultGroupName a {@link java.lang.String} object
     * @should select result group correctly
     * @should reset result group if new name not configured
     * @should reset result group if empty name given
     * @should reset advanced search query items if new group used as field template
     * @should reset advanced search query items if old group used as field template
     */
    public void setActiveResultGroupName(String activeResultGroupName) {
        logger.trace("setActiveResultGroupName: {}", activeResultGroupName);
        if (activeResultGroup != null && activeResultGroup.getName().equals(activeResultGroupName)) {
            return;
        }

        if (activeResultGroupName != null && !"-".equals(activeResultGroupName)) {
            for (SearchResultGroup resultGroup : DataManager.getInstance().getConfiguration().getSearchResultGroups()) {
                if (resultGroup.getName().equals(activeResultGroupName)) {
                    activeResultGroup = resultGroup;
                    if (resultGroup.isUseAsAdvancedSearchTemplate()) {
                        this.advancedSearchFieldTemplate = resultGroup.getName();
                        // Reset query items
                        resetAdvancedSearchParameters();
                        // Reset slider ranges
                        facets.resetSliderRange();
                        // Reset avalable facets
                        facets.resetAvailableFacets();
                    }
                    return;
                }
            }
            logger.warn("Search result group name not found: {}", activeResultGroupName);
        }

        // Reset query items and slider ranges if active group is used as item field template
        if (activeResultGroup != null && activeResultGroup.isUseAsAdvancedSearchTemplate()) {
            resetAdvancedSearchParameters();
            facets.resetSliderRange();
        }
        activeResultGroup = null;
        this.advancedSearchFieldTemplate = StringConstants.DEFAULT_NAME;
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
        if (facets.getActiveFacets().isEmpty()) {
            for (SearchQueryItem item : advancedSearchQueryGroup.getQueryItems()) {
                if (item.isHierarchical()) {
                    logger.trace("resetting current field value in advanced search: {}", item.getField());
                    item.setValue(null);
                }
            }
            return;
        }

        Set<SearchQueryItem> populatedQueryItems = new HashSet<>();
        for (IFacetItem facetItem : facets.getActiveFacets()) {
            if (!facetItem.isHierarchial()) {
                continue;
            }
            // logger.trace("facet item: {}", facetItem); //NOSONAR Debug
            // Look up and re-purpose existing query items with the same field first
            boolean matched = false;
            for (SearchQueryItem queryItem : advancedSearchQueryGroup.getQueryItems()) {
                // field:value pair already exists
                if (!populatedQueryItems.contains(queryItem) && (queryItem.getField() == null || StringUtils.isEmpty(queryItem.getValue()))) {
                    // Override existing items without a field or with the same field with current facet value
                    // logger.trace("updating query item: {}", queryItem); //NOSONAR Debug
                    queryItem.setField(facetItem.getField());
                    queryItem.setValue(facetItem.getValue());
                    // logger.trace("updated query item: {}", queryItem); //NOSONAR Debug
                    populatedQueryItems.add(queryItem);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // If no search field is set up for collection search, add new field containing the currently selected collection
                SearchQueryItem item = new SearchQueryItem();
                item.setField(facetItem.getField());
                item.setValue(facetItem.getValue());
                // ...but only if there is no exact field:value pair already among the query items
                if (!populatedQueryItems.contains(item)) {
                    advancedSearchQueryGroup.getQueryItems().add(item);
                    // logger.trace("added new item: {}", item); //NOSONAR Debug
                    populatedQueryItems.add(item);
                }
            }
        }
    }

    /**
     * <p>
     * removeChronologyFacetAction.
     * </p>
     *
     * @return Navigation outcome
     * @deprecated No longer relevant for current implementation
     */
    @Deprecated(since = "2023.01")
    public String removeChronologyFacetAction() {
        String facet = SolrConstants.YEAR + ":" + facets.getTempValue();
        facets.setTempValue("");
        return removeFacetAction(facet);
    }

    /**
     * <p>
     * removeRangeFacetAction.
     * </p>
     *
     * @param field a {@link java.lang.String} object
     * @return Navigation outcome
     */
    public String removeRangeFacetAction(String field) {
        return facets.getActiveFacetsForField(field).stream().findAny().map(item -> {
            String facet = item.getQueryEscapedLink();
            facets.setTempValue("");
            return removeFacetAction(facet);
        }).orElse("");
    }

    /**
     * <p>
     * removeFacetAction.
     * </p>
     *
     * @param facetQuery a {@link java.lang.String} object.
     * @return Navigation outcome
     * @should remove facet correctly
     */
    public String removeFacetAction(String facetQuery) {
        logger.trace("removeFacetAction: {}", facetQuery);
        //reset the search result list to page one since the result list will necessarily change when removing the facet
        setCurrentPage(1);
        //redirect to current cms page if this action takes place on a cms page
        Optional<ViewerPath> oPath = ViewHistory.getCurrentView(BeanUtils.getRequest());
        if (oPath.isPresent() && oPath.get().isCmsPage()) {
            facets.removeFacetAction(facetQuery, "");
            String url = PrettyUrlTools.getAbsolutePageUrl(StringConstants.PREFIX_PRETTY + "cmsOpenPage6", oPath.get().getCmsPage().getId(),
                    getActiveResultGroupName(), this.getExactSearchString(), oPath.get().getCmsPage().getListPage(), this.getSortString(),
                    this.getFacets().getActiveFacetString());
            logger.trace("redirecting to url: {}", url);
            PrettyUrlTools.redirectToUrl(url);
            return "";
        } else if (PageType.browse.equals(oPath.map(ViewerPath::getPageType).orElse(PageType.other))) {
            return facets.removeFacetAction(facetQuery, StringConstants.PREFIX_PRETTY + "browse4");
        } else {
            String ret = StringConstants.PRETTY_NEWSEARCH5;
            switch (activeSearchType) {
                case SearchHelper.SEARCH_TYPE_ADVANCED:
                    ret = StringConstants.PRETTY_SEARCHADVANCED5;
                    break;
                case SearchHelper.SEARCH_TYPE_TERMS:
                    ret = StringConstants.PRETTY_SEARCHTERM5;
                    break;
                default:
                    break;
            }
            return facets.removeFacetAction(facetQuery, ret);
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
        if (currentPage <= 0) {
            this.currentPage = 1;
            logger.debug("currentPage set to 1");
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getHitsCount() {
        if (activeResultGroup != null) {
            return activeResultGroup.getHitsCount();
        }
        if (currentSearch != null) {
            return currentSearch.getHitsCount();
        }

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
     * For unit tests.
     * 
     * @param currentHitIndex the currentHitIndex to set
     */
    void setCurrentHitIndex(int currentHitIndex) {
        this.currentHitIndex = currentHitIndex;
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

        String termQuery = null;
        if (searchTerms != null) {
            termQuery = SearchHelper.buildTermQuery(searchTerms.get(SearchHelper.TITLE_TERMS));
        }

        List<String> filterQueries = facets.generateFacetFilterQueries(true);
        // Add customFilterQuery to filter queries so that CMS filter queries are also applied
        if (StringUtils.isNotBlank(currentSearch.getCustomFilterQuery())) {
            filterQueries.add(currentSearch.getCustomFilterQuery());
        }
        if (currentHitIndex < currentSearch.getHitsCount() - 1) {
            return SearchHelper.getBrowseElement(searchStringInternal, currentHitIndex + 1, currentSearch.getAllSortFields(), filterQueries,
                    SearchHelper.generateQueryParams(termQuery), searchTerms, BeanUtils.getLocale(), proximitySearchDistance);
        }
        return SearchHelper.getBrowseElement(searchStringInternal, currentHitIndex, currentSearch.getAllSortFields(), filterQueries,
                SearchHelper.generateQueryParams(termQuery), searchTerms, BeanUtils.getLocale(), proximitySearchDistance);
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

        String termQuery = null;
        if (searchTerms != null) {
            termQuery = SearchHelper.buildTermQuery(searchTerms.get(SearchHelper.TITLE_TERMS));
        }

        List<String> filterQueries = facets.generateFacetFilterQueries(true);
        // Add customFilterQuery to filter queries so that CMS filter queries are also applied
        if (StringUtils.isNotBlank(currentSearch.getCustomFilterQuery())) {
            filterQueries.add(currentSearch.getCustomFilterQuery());
        }
        if (currentHitIndex > 0) {
            return SearchHelper.getBrowseElement(searchStringInternal, currentHitIndex - 1, currentSearch.getAllSortFields(), filterQueries,
                    SearchHelper.generateQueryParams(termQuery), searchTerms, BeanUtils.getLocale(), proximitySearchDistance);
        } else if (currentSearch.getHitsCount() > 0) {
            return SearchHelper.getBrowseElement(searchStringInternal, currentHitIndex, currentSearch.getAllSortFields(), filterQueries,
                    SearchHelper.generateQueryParams(termQuery), searchTerms, BeanUtils.getLocale(), proximitySearchDistance);
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
     * Getter for the field <code>advancedSearchQueryGroup</code>.
     * </p>
     *
     * @return the advancedQueryGroups
     */
    public SearchQueryGroup getAdvancedSearchQueryGroup() {
        return advancedSearchQueryGroup;
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
     */
    public List<StringPair> getAdvancedSearchSelectItems(String field, String language, boolean hierarchical)
            throws PresentationException, IndexUnreachableException, DAOException {
        // logger.trace("getAdvancedSearchSelectItems: {}", field); //NOSONAR Debug
        if (field == null) {
            throw new IllegalArgumentException("field may not be null.");
        }
        if (language == null) {
            throw new IllegalArgumentException("language may not be null.");
        }

        // Check for pre-generated items
        String key = new StringBuilder(getActiveResultGroupName()).append('_').append(language).append('_').append(field).toString();
        List<StringPair> ret = advancedSearchSelectItems.get(key);
        if (ret != null) {
            return ret;
        }

        ret = new ArrayList<>();
        logger.trace("Generating drop-down values for {}", field);
        Locale locale = Locale.forLanguageTag(language);
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
                            ViewerResourceBundle.getTranslation("bookmarkList_session", locale)));
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
                sbItemLabel.append(ViewerResourceBundle.getTranslation(dc.getName(), locale));
                ret.add(new StringPair(dc.getName(), sbItemLabel.toString()));
                sbItemLabel.setLength(0);
            }
            advancedSearchSelectItems.put(key, ret);
        } else {
            String suffix = SearchHelper.getAllSuffixes();
            if (activeResultGroup != null) {
                suffix = suffix + " +(" + activeResultGroup.getQuery() + ")";
            }
            String query = field + ":[* TO *]" + suffix;
            List<String> values = SearchHelper.getFacetValues(query, field, 1);
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
     */
    public List<StringPair> getAllCollections() {
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
     */
    public List<StringPair> getAllCollections(String language)
            throws PresentationException, IndexUnreachableException, DAOException {
        return getAdvancedSearchSelectItems(SolrConstants.DC, language, true);
    }

    /**
     * <p>
     * getAdvancedSearchAllowedFields.
     * </p>
     *
     * @return List of allowed advanced search fields
     */
    public List<AdvancedSearchFieldConfiguration> getAdvancedSearchAllowedFields() {
        return getAdvancedSearchAllowedFields(navigationHelper.getLocaleString(), advancedSearchFieldTemplate, false);
    }

    public List<AdvancedSearchFieldConfiguration> getAdvancedSearchFirstItemAllowedFields() {
        return getAdvancedSearchAllowedFields(navigationHelper.getLocaleString(), advancedSearchFieldTemplate, true);
    }

    /**
     * Returns index field names allowed for advanced search use. If language-specific index fields are used, those that don't match the current
     * locale are omitted.
     *
     * @param language Optional language code for filtering language-specific fields
     * @param template a {@link java.lang.String} object
     * @param addSearchFilters
     * @return List of allowed advanced search fields
     * @should omit languaged fields for other languages
     * @should add search filters
     */
    public static List<AdvancedSearchFieldConfiguration> getAdvancedSearchAllowedFields(final String language, String template,
            boolean addSearchFilters) {
        List<AdvancedSearchFieldConfiguration> fields =
                DataManager.getInstance().getConfiguration().getAdvancedSearchFields(template, false, language);
        if (fields == null) {
            return Collections.emptyList();
        }

        // Omit other languages
        if (!fields.isEmpty() && StringUtils.isNotEmpty(language)) {
            List<AdvancedSearchFieldConfiguration> toRemove = new ArrayList<>();
            for (AdvancedSearchFieldConfiguration field : fields) {
                if (field.getField().contains(SolrConstants.MIDFIX_LANG) && !field.getField().endsWith(language.toUpperCase())) {
                    toRemove.add(field);
                }
            }
            if (!toRemove.isEmpty()) {
                fields.removeAll(toRemove);
            }
        }

        if (addSearchFilters) {
            int i = 0;
            for (SearchFilter sf : DataManager.getInstance().getConfiguration().getSearchFilters()) {
                fields.add(i++, new AdvancedSearchFieldConfiguration(sf.getField()).setLabel(sf.getLabel()));
            }
        }

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
        resetAdvancedSearchParameters();
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
     * isFuzzySearchEnabled.
     * </p>
     *
     * @return the fuzzySearchEnabled
     */
    public boolean isFuzzySearchEnabled() {
        return fuzzySearchEnabled;
    }

    /**
     * <p>
     * Setter for the field <code>fuzzySearchEnabled</code>.
     * </p>
     *
     * @param fuzzySearchEnabled the fuzzySearchEnabled to set
     */
    public void setFuzzySearchEnabled(boolean fuzzySearchEnabled) {
        this.fuzzySearchEnabled = fuzzySearchEnabled;
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
                        .append(URLEncoder.encode(facets.getActiveFacetString(), URL_ENCODING))
                        .append("/-/")
                        .toString();
            } catch (UnsupportedEncodingException e) {
                logger.warn("Could not encode query '{}' for URL", currentQuery);
                return new StringBuilder().append(DataManager.getInstance().getConfiguration().getRestApiUrl())
                        .append("rss/search/")
                        .append(currentQuery)
                        .append('/')
                        .append(facets.getActiveFacetString())
                        .append("/-/")
                        .toString();
            }

        }

        String facetQuery = StringUtils.isBlank(facets.getActiveFacetString().replace("-", "")) ? null : facets.getActiveFacetString();
        return urls.path(ApiUrls.RECORDS_RSS)
                .query("query", currentQuery)
                .query("facets", facetQuery)
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
     * exportSearchAsRisAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String exportSearchAsRisAction() throws IndexUnreachableException {
        logger.trace("exportSearchAsRisAction");
        final FacesContext facesContext = FacesContext.getCurrentInstance();

        String currentQuery = SearchHelper.prepareQuery(searchStringInternal);
        String finalQuery = SearchHelper.buildFinalQuery(currentQuery, true, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        Locale locale = navigationHelper.getLocale();
        int timeout = DataManager.getInstance().getConfiguration().getExcelDownloadTimeout(); //[s]

        BiConsumer<HttpServletRequest, Task> task = (request, job) -> {
            if (!facesContext.getResponseComplete()) {
                try {
                    if (Thread.interrupted()) {
                        job.setError("Execution cancelled");
                    } else {
                        Callable<Boolean> download = new Callable<Boolean>() {

                            @Override
                            public Boolean call() {
                                try {
                                    RISExport export = new RISExport();
                                    export.executeSearch(finalQuery, currentSearch.getAllSortFields(),
                                            facets.generateFacetFilterQueries(true), null, searchTerms, locale, proximitySearchDistance);
                                    if (export.isHasResults()) {
                                        ((HttpServletResponse) facesContext.getExternalContext().getResponse())
                                                .addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION,
                                                        NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + export.getFileName() + "\"");
                                        return export.writeToResponse(facesContext.getExternalContext().getResponseOutputStream());
                                    }
                                    return false;
                                } catch (IndexUnreachableException | DAOException | PresentationException | ViewerConfigurationException
                                        | IOException e) {
                                    logger.error(e.getMessage(), e);
                                    return false;
                                } finally {
                                    facesContext.responseComplete();
                                }
                            }
                        };

                        downloadComplete = new FutureTask<>(download);
                        EXECUTOR.submit(downloadComplete);
                        downloadComplete.get(timeout, TimeUnit.SECONDS);
                    }
                } catch (TimeoutException e) {
                    job.setError("Timeout for RIS download");
                } catch (InterruptedException e) {
                    job.setError("Timeout for RIS download");
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    logger.error(e.getMessage(), e);
                    job.setError("Failed to create RIS export");
                }
            } else {
                job.setError("Response is already committed");
            }
        };

        try {
            Task excelCreationJob = new Task(new TaskParameter(TaskType.SEARCH_EXCEL_EXPORT), task);
            Long jobId = DataManager.getInstance().getRestApiJobManager().addTask(excelCreationJob);
            Future<?> ready = DataManager.getInstance()
                    .getRestApiJobManager()
                    .triggerTaskInThread(jobId, (HttpServletRequest) facesContext.getExternalContext().getRequest());
            ready.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.debug("Download interrupted");
            Thread.currentThread().interrupt();
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
        String finalQuery = SearchHelper.buildFinalQuery(currentQuery, true, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        Locale locale = navigationHelper.getLocale();
        int timeout = DataManager.getInstance().getConfiguration().getExcelDownloadTimeout(); //[s]

        BiConsumer<HttpServletRequest, Task> task = (request, job) -> {
            if (!facesContext.getResponseComplete()) {
                try (SXSSFWorkbook wb = buildExcelSheet(facesContext, finalQuery, currentQuery, proximitySearchDistance, locale)) {
                    if (wb == null) {
                        job.setError("Failed to create excel sheet");
                    } else if (Thread.interrupted()) {
                        job.setError("Execution cancelled");
                    } else {
                        Callable<Boolean> download = new Callable<Boolean>() {

                            @Override
                            public Boolean call() {
                                ExcelExport export = new ExcelExport();
                                try {
                                    logger.debug("Writing Excel...");
                                    export.setWorkbook(wb);
                                    return export.writeToResponse(facesContext.getExternalContext().getResponseOutputStream());
                                } catch (IOException e) {
                                    logger.error(e.getMessage(), e);
                                    return false;
                                } finally {
                                    facesContext.responseComplete();
                                    try {
                                        export.close();
                                    } catch (IOException e) {
                                        logger.error(e.getMessage());
                                    }
                                }
                            }
                        };

                        downloadComplete = new FutureTask<>(download);
                        EXECUTOR.submit(downloadComplete);
                        downloadComplete.get(timeout, TimeUnit.SECONDS);
                    }
                } catch (TimeoutException e) {
                    job.setError("Timeout for excel download");
                } catch (InterruptedException e) {
                    job.setError("Timeout for excel download");
                    Thread.currentThread().interrupt();
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
            Future<?> ready = DataManager.getInstance()
                    .getRestApiJobManager()
                    .triggerTaskInThread(jobId, (HttpServletRequest) facesContext.getExternalContext().getRequest());
            ready.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.debug("Download interrupted");
            Thread.currentThread().interrupt();
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
     * @param finalQuery Complete query with suffixes.
     * @param exportQuery Query constructed from the user's input, without any secret suffixes.
     * @param proximitySearchDistance
     * @param locale
     * @return {@link SXSSFWorkbook}
     * @throws InterruptedException
     * @throws ViewerConfigurationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    private SXSSFWorkbook buildExcelSheet(final FacesContext facesContext, String finalQuery, String exportQuery, int proximitySearchDistance,
            Locale locale) throws InterruptedException, ViewerConfigurationException {
        try {
            String termQuery = null;
            if (searchTerms != null) {
                termQuery = SearchHelper.buildTermQuery(searchTerms.get(SearchHelper.TITLE_TERMS));
            }
            Map<String, String> params = SearchHelper.generateQueryParams(termQuery);
            SXSSFWorkbook wb = new SXSSFWorkbook(25); //NOSONAR try-with-resources in the calling method
            SearchHelper.exportSearchAsExcel(wb, finalQuery, exportQuery, currentSearch.getAllSortFields(), facets.generateFacetFilterQueries(true),
                    params, searchTerms, locale, proximitySearchDistance);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            facesContext.getExternalContext().responseReset();
            facesContext.getExternalContext().setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            facesContext.getExternalContext()
                    .setResponseHeader("Content-Disposition", "attachment;filename=\"viewer_search_"
                            + LocalDateTime.now().format(DateTools.FORMATTERFILENAME)
                            + ".xlsx\"");
            return wb;
        } catch (IndexUnreachableException | DAOException | PresentationException e) {
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
        setHitsPerPageSetterCalled(true);
    }

    /**
     * Like setHitsPerPage() but doesn't trigger the boolean.
     *
     * @param hitsPerPage the hitsPerPage to set
     * @should not change hitsPerPageSetterCalled value
     */
    public void setHitsPerPageNoTrigger(int hitsPerPage) {
        // logger.trace("setHitsPerPageNoTrigger: {}", hitsPerPage); //NOSONAR Debug
        this.hitsPerPage = hitsPerPage;
    }

    /**
     * <p>
     * isHitsPerPageSetterCalled.
     * </p>
     *
     * @return the hitsPerPageSetterCalled
     */
    public boolean isHitsPerPageSetterCalled() {
        return hitsPerPageSetterCalled;
    }

    /**
     * <p>
     * Setter for the field <code>hitsPerPageSetterCalled</code>.
     * </p>
     *
     * @param hitsPerPageSetterCalled the hitsPerPageSetterCalled to set
     */
    public void setHitsPerPageSetterCalled(boolean hitsPerPageSetterCalled) {
        // logger.trace("setHitsPerPageSetterCalled: {}", hitsPerPageSetterCalled); //NOSONAR Debug
        this.hitsPerPageSetterCalled = hitsPerPageSetterCalled;
    }

    /**
     * <p>
     * Getter for the field <code>advancedSearchQueryInfo</code>.
     * </p>
     *
     * @return the advancedSearchQueryInfo
     * @should html escape string
     */
    public String getAdvancedSearchQueryInfo() {
        return StringEscapeUtils.escapeHtml4(advancedSearchQueryInfo);
    }

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
        String query = SearchHelper.buildFinalQuery(SearchHelper.ALL_RECORDS_QUERY, false, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        return DataManager.getInstance().getSearchIndex().getHitCount(query);
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
            case SearchHelper.SEARCH_TYPE_TERMS:
                return navigationHelper.getTermUrl();
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
        return new StructElement((String) doc.getFirstValue(SolrConstants.IDDOC), doc);
    }

    /** {@inheritDoc} */
    @Override
    public String getCurrentSearchUrlRoot() {
        switch (activeSearchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return navigationHelper.getAdvancedSearchUrl();
            case SearchHelper.SEARCH_TYPE_TERMS:
                return navigationHelper.getTermUrl();
            default:
                return navigationHelper.getSearchUrl();
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
                .append(facets.getActiveFacetString())
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
     * @return Current search URL
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
        return StringConstants.PREFIX_PRETTY + "search5";
    }

    /**
     * 
     * @param basePath
     * @return {@link URI}
     */
    private URI getParameterPath(final URI basePath) {
        URI ret = ViewerPathBuilder.resolve(basePath, getActiveResultGroupName());
        // URL-encode query if not yet encoded
        String exactSearchString = getExactSearchString();
        ret = ViewerPathBuilder.resolve(ret, exactSearchString);
        ret = ViewerPathBuilder.resolve(ret, Integer.toString(getCurrentPage()));
        ret = ViewerPathBuilder.resolve(ret, getSortString());
        ret = ViewerPathBuilder.resolve(ret, StringTools.encodeUrl(getFacets().getActiveFacetString()));

        return ret;
    }

    /**
     * 
     * @param url
     */
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
    public List<IFacetItem> getStaticFacets(final String field, final String subQuery, Integer resultLimit, final Boolean reverseOrder)
            throws PresentationException, IndexUnreachableException {
        StringBuilder sbQuery = new StringBuilder(100);
        sbQuery.append(SearchHelper.ALL_RECORDS_QUERY)
                .append(SearchHelper.getAllSuffixes(BeanUtils.getRequest(), true, true));

        if (StringUtils.isNotEmpty(subQuery)) {
            sbQuery.append(" AND (").append(subQuery.startsWith(SolrConstants.SOLR_QUERY_AND) ? subQuery.substring(5) : subQuery).append(')');
        }
        String useField = SearchHelper.facetifyField(field);
        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .search(sbQuery.toString(), 0, 0, null, Collections.singletonList(useField), Collections.singletonList(SolrConstants.IDDOC));
        if (resp == null || resp.getFacetField(useField) == null || resp.getFacetField(useField).getValues() == null) {
            return Collections.emptyList();
        }

        Map<String, Long> result =
                resp.getFacetField(useField).getValues().stream().filter(count -> count.getName().charAt(0) != 1).sorted((count1, count2) -> {
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
                        .limit(resultLimit > 0 ? resultLimit : resp.getFacetField(useField).getValues().size())
                        .collect(Collectors.toMap(Count::getName, Count::getCount));
        List<String> hierarchicalFields = DataManager.getInstance().getConfiguration().getHierarchicalFacetFields();

        return FacetItem.generateFacetItems(useField, result, true, reverseOrder, hierarchicalFields.contains(useField));
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
     * setFirstQueryItemValue.
     * </p>
     *
     * @param value a {@link java.lang.String} object.
     */
    public void setFirstQueryItemValue(String value) {
        this.advancedSearchQueryGroup.getQueryItems().get(0).setValue(value);
    }

    /**
     * <p>
     * getFirstQueryItemValue.
     * </p>
     */
    public void getFirstQueryItemValue() {
        this.advancedSearchQueryGroup.getQueryItems().get(0).getValue();
    }

    /**
     * <p>
     * setBookmarkListName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setBookmarkListName(String name) {
        SearchQueryItem item = this.advancedSearchQueryGroup.getQueryItems().get(0);
        item.setValue(name);
        item.setField(SolrConstants.BOOKMARKS);

    }

    /**
     * <p>
     * getBookmarkListName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBookmarkListName() {
        return this.advancedSearchQueryGroup.getQueryItems()
                .stream()
                .filter(item -> item.getField() != null && item.getField().equals(SolrConstants.BOOKMARKS))
                .filter(item -> item.getValue() != null && !item.getValue().startsWith(PREFIX_KEY))
                .findFirst()
                .map(SearchQueryItem::getValue)
                .orElse("");
    }

    /**
     * <p>
     * setBookmarkListName.
     * </p>
     *
     * @param key The sharing key to set
     */
    public void setBookmarkListSharedKey(String key) {
        SearchQueryItem item = this.advancedSearchQueryGroup.getQueryItems().get(0);
        item.setValue(PREFIX_KEY + key);
        item.setField(SolrConstants.BOOKMARKS);

    }

    /**
     * <p>
     * getBookmarkListName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getBookmarkListSharedKey() {
        String value = this.advancedSearchQueryGroup.getQueryItems()
                .stream()
                .filter(item -> item.getField() != null && item.getField().equals(SolrConstants.BOOKMARKS))
                .filter(item -> item.getValue() != null && item.getValue().startsWith(PREFIX_KEY))
                .findFirst()
                .map(SearchQueryItem::getValue)
                .orElse("");
        return value.replace(PREFIX_KEY, "");
    }

    /**
     * <p>
     * Getter for the field <code>proximitySearchDistance</code>.
     * </p>
     *
     * @return a int
     */
    public int getProximitySearchDistance() {
        return proximitySearchDistance;
    }

    /**
     * <p>
     * searchInRecord.
     * </p>
     *
     * @param queryField a {@link java.lang.String} object
     * @param queryValue a {@link java.lang.String} object
     * @return Navigation outcome
     */
    public String searchInRecord(String queryField, String queryValue) {
        this.advancedSearchQueryGroup.getQueryItems().get(0).setField(queryField);
        if (StringUtils.isNotBlank(queryValue)) {
            this.advancedSearchQueryGroup.getQueryItems().get(0).setValue(queryValue);
        }
        this.advancedSearchQueryGroup.getQueryItems().get(0).setOperator(SearchItemOperator.AND);
        this.advancedSearchQueryGroup.getQueryItems().get(1).setField(SearchHelper.SEARCH_FILTER_ALL.getField());
        this.advancedSearchQueryGroup.getQueryItems().get(1).setLabel(SearchHelper.SEARCH_FILTER_ALL.getLabel());
        this.advancedSearchQueryGroup.getQueryItems().get(1).setOperator(SearchItemOperator.AND);
        this.setActiveSearchType(1);

        return this.searchAdvanced();
    }

    /**
     * Executes an advanced search using the given field. The search value is set via the HTML component using this method.
     * 
     * @param queryField Advanced search field to query
     * @return Navigation outcome
     */
    public String searchInField(String queryField) {
        this.advancedSearchQueryGroup.getQueryItems().get(0).setField(queryField);
        this.advancedSearchQueryGroup.getQueryItems().get(0).setOperator(SearchItemOperator.AND);
        this.setActiveSearchType(1);

        return this.searchAdvanced();
    }

    /**
     * <p>
     * isSolrIndexReachable.
     * </p>
     *
     * @return true if Solr ping successful; false otherwise
     */
    public boolean isSolrIndexReachable() {
        return DataManager.getInstance().getSearchIndex().pingSolrIndex();
    }

    /**
     * <p>
     * hasGeoLocationHits.
     * </p>
     *
     * @return a boolean
     */
    public boolean hasGeoLocationHits() {
        return this.currentSearch != null && !this.currentSearch.isHasGeoLocationHits();
    }

    /**
     * <p>
     * getHitsLocations.
     * </p>
     *
     * @return a {@link java.util.List} object
     */
    public List<String> getHitsLocations() {
        if (this.currentSearch != null) {
            return this.currentSearch.getHitsLocationList()
                    .stream()
                    //                    .distinct()
                    .map(Location::getGeoJson)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Display the geo facet map if there are any hits available with geo coordinates
     *
     * @return true if search hits with coordinates available; false otherwise
     */
    public boolean isShowGeoFacetMap() {
        return currentSearch != null && facets != null && (currentSearch.isHasGeoLocationHits() || facets.getGeoFacetting().hasFeature());
    }

    /**
     * <p>
     * getHitsMap.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.maps.GeoMap} object
     */
    public GeoMap getHitsMap() {
        GeoMap map = new GeoMap();
        ManualFeatureSet featureSet = new ManualFeatureSet();
        map.addFeatureSet(featureSet);
        map.setShowPopover(true);
        //set initial zoom to max zoom so map will be as zoomed in as possible
        map.setInitialView("{"
                + "\"zoom\": 5,"
                + "\"center\": [11.073397, -49.451993]"
                + "}");

        if (this.currentSearch != null) {

            List<String> features = this.currentSearch.getHitsLocationList()
                    .stream()
                    .map(Location::getGeoJson)
                    //            .distinct()
                    .collect(Collectors.toList());

            featureSet.setFeatures(features);
        }
        return map;
    }

    /**
     * <p>
     * facetifyField.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object
     * @return Facet variant of the given fieldName
     */
    public String facetifyField(String fieldName) {
        return SearchHelper.facetifyField(fieldName);
    }

    /**
     * <p>
     * getFieldFacetValues.
     * </p>
     *
     * @param field a {@link java.lang.String} object
     * @param num a int
     * @return List of facet values for the given field
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     */
    public List<FacetItem> getFieldFacetValues(String field, int num) throws IndexUnreachableException {
        return getFieldFacetValues(field, num, "");
    }

    /**
     * <p>
     * getFieldFacetValues.
     * </p>
     *
     * @param field a {@link java.lang.String} object
     * @param num a int
     * @param filterQuery a {@link java.lang.String} object
     * @return List of facet values for the given field
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     */
    public List<FacetItem> getFieldFacetValues(String field, final int num, String filterQuery) throws IndexUnreachableException {
        try {
            int useNum = num <= 0 ? Integer.MAX_VALUE : num;
            String query = "+(ISWORK:* OR ISANCHOR:*) " + SearchHelper.getAllSuffixes();
            if (StringUtils.isNotBlank(filterQuery)) {
                query += " +(" + filterQuery + ")";
            }
            String facetField = SearchHelper.facetifyField(field);
            QueryResponse response =
                    DataManager.getInstance()
                            .getSearchIndex()
                            .searchFacetsAndStatistics(query, null, Collections.singletonList(facetField), 1, false);
            return response.getFacetField(facetField)
                    .getValues()
                    .stream()
                    .filter(count -> !StringTools.checkValueEmptyOrInverted(count.getName()))
                    .map(FacetItem::new)
                    .sorted((f1, f2) -> Long.compare(f2.getCount(), f1.getCount()))
                    .limit(useNum)
                    .collect(Collectors.toList());
        } catch (PresentationException e) {
            logger.warn("Error rendering field facet values: {}", e.toString());
            return Collections.emptyList();
        }
    }

    /**
     * <p>
     * getSearchSortingOptions.
     * </p>
     *
     * @param language a {@link java.lang.String} object
     * @return List of sorting options for the given language
     * @should return options correctly
     * @should use current random seed option instead of default
     */
    public Collection<SearchSortingOption> getSearchSortingOptions(String language) {
        Collection<SearchSortingOption> options = DataManager.getInstance().getConfiguration().getSearchSortingOptions(language);
        Collection<SearchSortingOption> ret = new ArrayList<>(options.size());
        for (SearchSortingOption option : options) {
            // If random sorting is currently in use, use that particular seed
            if (option.getField().equals(SolrConstants.SORT_RANDOM) && searchSortingOption != null
                    && searchSortingOption.getField().startsWith("random")) {
                ret.add(searchSortingOption);
            } else {
                ret.add(option);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getQueryResultCount.
     * </p>
     *
     * @param query a {@link java.lang.String} object
     * @return Number of hits for the given query
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.PresentationException
     */
    public long getQueryResultCount(String query) throws IndexUnreachableException, PresentationException {
        String finalQuery = SearchHelper.buildFinalQuery(query, false, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        return DataManager.getInstance().getSearchIndex().getHitCount(finalQuery);
    }

    /**
     * <p>
     * getFinalSolrQueryEscaped.
     * </p>
     *
     * @return URL-encoded final query
     */
    public String getFinalSolrQueryEscaped() {
        return StringTools.encodeUrl(getFinalSolrQuery());
    }

    /**
     * <p>
     * getCombinedFilterQueryEscaped.
     * </p>
     *
     * @return URL-encoded combined filter query
     */
    public String getCombinedFilterQueryEscaped() {
        return StringTools.encodeUrl(getCombinedFilterQuery());
    }

    /**
     * The url of the viewer page loaded when the last search operation was performed, stored ing {@link #lastUsedSearchPage} or the url of the
     * default search or searchAdvanved page depending on the state of this bean
     *
     * @return a URL string
     */
    public String getLastUsedSearchUrl() {
        return this.lastUsedSearchPage
                .map(view -> ServletUtils.getServletPathWithHostAsUrlFromRequest(BeanUtils.getRequest()) + view.getCombinedPrettyfiedUrl())
                .orElse(getLastUsedDefaultSearchUrl());
    }

    private String getLastUsedDefaultSearchUrl() {
        switch (activeSearchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return PrettyUrlTools.getAbsolutePageUrl(
                        StringConstants.PRETTY_SEARCHADVANCED5,
                        getActiveResultGroupName(),
                        getExactSearchString(),
                        getCurrentPage(),
                        getSortString(),
                        facets.getActiveFacetString());
            case SearchHelper.SEARCH_TYPE_TERMS:
                return PrettyUrlTools.getAbsolutePageUrl(
                        StringConstants.PRETTY_SEARCHTERM5,
                        getActiveResultGroupName(),
                        getExactSearchString(),
                        getCurrentPage(),
                        getSortString(),
                        facets.getActiveFacetString());
            default:
                return PrettyUrlTools.getAbsolutePageUrl(
                        StringConstants.PRETTY_NEWSEARCH5,
                        getActiveResultGroupName(),
                        getExactSearchString(),
                        getCurrentPage(),
                        getSortString(),
                        facets.getActiveFacetString());
        }
    }

    /** {@inheritDoc} */
    @Override
    public String changeSorting() throws IOException {
        logger.trace("changeSorting");
        switch (getActiveSearchType()) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return StringConstants.PRETTY_SEARCHADVANCED5;
            case SearchHelper.SEARCH_TYPE_TERMS:
                return StringConstants.PRETTY_SEARCHTERM5;
            default:
                return StringConstants.PRETTY_NEWSEARCH5;
        }
    }
}
