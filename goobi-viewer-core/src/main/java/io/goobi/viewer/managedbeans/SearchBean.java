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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
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
import io.goobi.viewer.model.search.AdvancedSearchOrigin;
import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.search.FacetItem;
import io.goobi.viewer.model.search.FilterQueryParser;
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
import io.goobi.viewer.model.search.SearchQueryItemLine;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.search.SearchSortingOption;
import io.goobi.viewer.model.search.query.QueryResult;
import io.goobi.viewer.model.search.query.SimpleQueryBuilder;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.collections.BrowseDcElement;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JSF session-scoped backing bean for the search interface, managing search queries, facets,
 * sorting, and result pagination. Initialised via {@code @PostConstruct init()} which resets the
 * advanced search parameters to their defaults.
 *
 * <p><b>Lifecycle:</b> Created once per HTTP session by the CDI container; survives across
 * multiple page navigations within the same session and is destroyed when the session expires.
 *
 * <p><b>Thread safety:</b> Mostly confined to the JSF request thread of the owning session.
 * The async Excel-export download state ({@code downloadReady}, {@code downloadComplete}) is
 * held in {@code volatile} fields and guarded by a {@code synchronized} block to allow safe
 * hand-off between the JSF thread and the background download thread.
 */
@Named
@SessionScoped
public class SearchBean implements SearchInterface, Serializable {

    private static final long serialVersionUID = 6962223613432267768L;

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    /**
     * Shuts down the static executor. Should be called from the servlet context listener on undeploy.
     */
    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SearchBean.class);

    /** Constant <code>URL_ENCODING="UTF8"</code>. */
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
    @Inject
    private HttpServletRequest request;

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
    private String advancedSearchFieldTemplate = DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultTemplateName();
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
    /** Origin record from which the search was triggered (for back-link to TOC view). Null if not searching within a record. */
    private AdvancedSearchOrigin advancedSearchOrigin;
    /** Group of query item clusters for the advanced search. */
    private SearchQueryGroup advancedSearchQueryGroup =
            new SearchQueryGroup(
                    DataManager.getInstance()
                            .getConfiguration()
                            .getAdvancedSearchFields(advancedSearchFieldTemplate, true, BeanUtils.getLocale().getLanguage()),
                    advancedSearchFieldTemplate);
    /** Human-readable representation of the advanced search query for displaying. */
    private String advancedSearchQueryInfo;
    /** Current search object. Contains the results and can be used to persist search parameters in the DB. */
    private Search currentSearch;
    /** If >0, proximity search will be applied to phrase searches. */
    private int proximitySearchDistance = 0;
    /** Fuzzy search switch. */
    private boolean fuzzySearchEnabled = FUZZY_SEARCH_ENABLED_INITIAL;

    private volatile FutureTask<Boolean> downloadReady; //NOSONAR   Future is thread-save
    private volatile FutureTask<Boolean> downloadComplete; //NOSONAR   Future is thread-save

    private String filterQuery = "";

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
     * init.
     */
    @PostConstruct
    public void init() {
        resetAdvancedSearchParameters();
    }

    /**
     * Required setter for ManagedProperty injection for unit tests.
     *
     * @param navigationHelper the NavigationHelper instance to inject for testing
     */
    public void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    /**
     * Getter for unit tests.
     * 

     */
    Map<String, List<StringPair>> getAdvancedSearchSelectItems() {
        return advancedSearchSelectItems;
    }

    /**
     * clearSearchItemLists.
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
        return search(getEffectiveFilterQuery());
    }

    public String getEffectiveFilterQuery() {
        if (StringUtils.isNotBlank(this.filterQuery)) {
            return this.filterQuery;
        } else if (this.request != null) {
            return new FilterQueryParser().getFilterQuery(this.request).orElse("");
        } else {
            return "";
        }
    }

    /**
     * Executes the search using already set parameters. Usually called from Pretty URLs.
     *
     * @param filterQuery additional Solr filter query to restrict results
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
     * <p>Action method for search buttons (simple search).
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
     * @param resetParameters true to reset sort, filter and page before searching
     * @return Navigation outcome
     */
    public String searchSimple(boolean resetParameters) {
        return searchSimple(resetParameters, true);
    }

    /**
     * Action method for search buttons (simple search) with an option to reset search parameters and active facets.
     *
     * @param resetParameters true to reset sort, filter and page before searching
     * @param resetFacets true to clear all active facet selections
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
     * simpleSearch.
     *
     * @param search search interface to trigger simple search on
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
     * Same as <code>{@link #searchSimple()}</code> but sets the current facets to the given string.
     *
     * @param facetString encoded facet selection string to apply before searching
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
     * searchAdvanced.
     *
     * @param resetParameters true to reset sort, filter and page before searching
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
        searchStringInternal = generateAdvancedSearchMainQuery();

        return StringConstants.PRETTY_SEARCHADVANCED5;
    }

    /**
     * Searches using currently set search string.
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
     * Same as {@link #resetSearchAction()} without the redirect.
     */
    public void reset() {
        generateSimpleSearchString("");
        setCurrentPage(1);
        setExactSearchString("");
        mirrorAdvancedSearchCurrentHierarchicalFacets();
        resetSearchResults();
        resetSearchParameters(true, true);
        proximitySearchDistance = 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Alias for {@link #resetSearchAction()}
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
     * resetSearchParameters.
     *
     * @param resetAllSearchTypes true to also reset parameters for the currently active search type
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
        this.advancedSearchOrigin = null;
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
     * @param reset true to reset advanced search query items to defaults
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
    String generateAdvancedSearchMainQuery() {
        logger.trace("generateAdvancedSearchMainQuery");
        StringBuilder sb = new StringBuilder("(");
        StringBuilder sbInfo = new StringBuilder();
        searchTerms.clear();
        StringBuilder sbCurrentCollection = new StringBuilder();
        Set<String> usedHierarchicalFields = new HashSet<>();
        Set<String> usedFieldValuePairs = new HashSet<>();
        this.proximitySearchDistance = 0;
        for (SearchQueryItem item : advancedSearchQueryGroup.getQueryItems()) {
            for (SearchQueryItemLine line : item.getLines()) {
                logger.trace("Query item line: {}:{}", item.getField(), line.getValue()); //NOSONAR Debug
                if (StringUtils.isEmpty(item.getField())) {
                    continue;
                }
                if (sbInfo.length() > 1) {
                    sbInfo.append(' ');
                }
                if (StringUtils.isNotEmpty(item.getValue())) {
                    sbInfo.append(ViewerResourceBundle.getTranslation("searchOperator_" + line.getOperator().name(),
                            BeanUtils.getLocale()))
                            .append(' ');
                }

                // Generate the hierarchical facet parameter from query items
                if (item.isHierarchical()) {
                    // logger.trace("{} is hierarchical", queryItem.getField()); //NOSONAR Debug
                    usedHierarchicalFields.add(item.getField());
                    if (StringUtils.isBlank(item.getValue())) {
                        continue;
                    }

                    // Skip identical hierarchical items

                    // Find existing facet items that can be re-purposed for the existing facets
                    boolean skipQueryItem = false;
                    for (IFacetItem facetItem : facets.getActiveFacetsCopy()) {
                        // logger.trace("checking facet item: {}", facetItem.getLink()); //NOSONAR Debug
                        if (!facetItem.getField().equals(item.getField())) {
                            continue;
                        }
                        if (usedFieldValuePairs.contains(facetItem.getLink())) {
                            // logger.trace("facet item already handled: {}", facetItem.getLink()); //NOSONAR Debug
                            continue;
                        }
                        if (!usedFieldValuePairs.contains(item.getField() + ":" + item.getValue())) {
                            facetItem.setLink(item.getField() + ":" + item.getValue());
                            usedFieldValuePairs.add(facetItem.getLink());
                            // logger.trace("reuse facet item: {}", facetItem); //NOSONAR Debug
                            skipQueryItem = true;
                            break;
                        }
                    }

                    if (!skipQueryItem) {
                        String itemQuery =
                                new StringBuilder().append(item.getField()).append(':').append(item.getValue().trim()).toString();
                        // logger.trace("item query: {}", itemQuery); //NOSONAR Debug

                        // Check whether this combination already exists and skip, if that's the case
                        if (usedFieldValuePairs.contains(itemQuery)) {
                            // logger.trace("facet item already exists: {}", itemQuery); //NOSONAR Debug
                            continue;
                        }
                        usedFieldValuePairs.add(itemQuery);

                        sbCurrentCollection.append(itemQuery).append(";;");

                        sbInfo.append('(')
                                .append(ViewerResourceBundle.getTranslation(item.getField(), BeanUtils.getLocale()))
                                .append(": \"")
                                .append(ViewerResourceBundle.getTranslation(item.getValue(), BeanUtils.getLocale()))
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
                if (SolrConstants.BOOKMARKS.equals(item.getField())) {

                    // Bookmark list search
                    if (StringUtils.isEmpty(item.getValue())) {
                        continue;
                    }

                    String key = getBookmarkListSharedKey();
                    String name = getBookmarkListName();

                    if (StringUtils.isNotBlank(key)) {
                        try {
                            BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkListByShareKey(key);
                            if (bookmarkList != null) {
                                item.setValue(bookmarkList.getName());
                                itemQuery = bookmarkList.getFilterQuery();
                            }
                        } catch (DAOException e) {
                            logger.error(e.toString(), e);
                        }
                    } else if (StringUtils.isNotBlank(name) && !"session".equals(name)) {
                        try {
                            BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkList(name, null);
                            if (bookmarkList != null) {
                                item.setValue(bookmarkList.getName());
                                itemQuery = bookmarkList.getFilterQuery();
                            }
                        } catch (DAOException e) {
                            logger.error(e.toString(), e);
                        }
                    } else if (userBean.isLoggedIn()) {
                        // User bookmark list
                        try {
                            BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkList(item.getValue(), userBean.getUser());
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
                    itemQuery = item.generateQuery(searchTerms.get(SolrConstants.FULLTEXT), true, fuzzySearchEnabled);
                    this.proximitySearchDistance = Math.max(this.proximitySearchDistance, item.getProximitySearchDistance());
                }

                logger.trace("Item query: {}", itemQuery);
                if (StringUtils.isNotEmpty(item.getValue())) {
                    String infoFieldLabel =
                            SearchHelper.SEARCH_FILTER_ALL.getField().equals(item.getField()) ? item.getLabel() : item.getField();
                    sbInfo.append('(').append(ViewerResourceBundle.getTranslation(infoFieldLabel, BeanUtils.getLocale())).append(": ");
                    switch (line.getOperator()) {
                        case AND:
                            if (SolrConstants.BOOKMARKS.equals(item.getField()) && !userBean.isLoggedIn()) {
                                // Session bookmark list value
                                sbInfo.append(ViewerResourceBundle.getTranslation("bookmarkList_session", BeanUtils.getLocale()));
                            } else if (item.isRange()) {
                                sbInfo.append('[').append(item.getValue()).append(" - ").append(item.getValue2()).append(']');
                            } else {
                                if (item.isDisplaySelectItems()) {
                                    sbInfo.append(ViewerResourceBundle.getTranslation(item.getValue(), BeanUtils.getLocale()));
                                } else {
                                    sbInfo.append(item.getValue());
                                }
                            }
                            break;
                        case NOT:
                            if (item.isDisplaySelectItems()) {
                                sbInfo.append(ViewerResourceBundle.getTranslation(item.getValue(), BeanUtils.getLocale()));
                            } else {
                                sbInfo.append(item.getValue());
                            }
                            break;
                        default:
                            if (item.isRange()) {
                                sbInfo.append('[').append(item.getValue()).append(" - ").append(item.getValue2()).append(']');
                            } else {
                                if (item.isDisplaySelectItems()) {
                                    sbInfo.append(ViewerResourceBundle.getTranslation(item.getValue(), BeanUtils.getLocale()));
                                } else {
                                    sbInfo.append(item.getValue());
                                }
                            }
                    }
                    sbInfo.append(')');
                }

                if (!itemQuery.isEmpty()) {
                    if (sb.length() > 1) {
                        sb.append(' ');
                    }
                    sb.append(itemQuery);
                }
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
        if (!sb.isEmpty()) {
            sb.append(')');
        }
        if (sb.toString().equals("()")) {
            sb.delete(0, 2);
        }
        if (!sbCurrentCollection.isEmpty()) {
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
     * executeSearch.
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
     * executeSearch.
     *
     * @param filterQuery additional Solr filter query to restrict results
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

        // Init search object using a local variable first to avoid NPE from concurrent
        // resetSearchResults() calls (SearchBean is @SessionScoped and may be accessed by
        // multiple request threads simultaneously; assigning to currentSearch only once,
        // after full initialization, ensures the local reference stays non-null throughout).
        Search newSearch = new Search(activeSearchType, currentSearchFilter, getResultGroupsForSearchExecution());
        newSearch.setUserInput(searchString);
        newSearch.setQuery(searchStringInternal);
        newSearch.setPage(currentPage);
        newSearch.setSortString(searchSortingOption != null ? searchSortingOption.getSortString() : null);
        newSearch.setFacetString(facets.getActiveFacetString());
        newSearch.setProximitySearchDistance(proximitySearchDistance);
        StringBuilder sbFilterQuery = new StringBuilder();
        String templateQuery = DataManager.getInstance().getConfiguration().getAdvancedSearchTemplateQuery(advancedSearchFieldTemplate);
        if (StringUtils.isNotEmpty(templateQuery)) {
            sbFilterQuery.append(" +(").append(templateQuery).append(")");
        }
        if (StringUtils.isNotEmpty(filterQuery) && !filterQuery.startsWith("{!")) {
            sbFilterQuery.append(" +(").append(filterQuery).append(")");
        } else if (StringUtils.isNotEmpty(filterQuery)) {
            sbFilterQuery.append(filterQuery);
        }

        if (StringUtils.isNotEmpty(this.filterQuery) && !this.filterQuery.startsWith("{!")) {
            sbFilterQuery.append(" +(").append(this.filterQuery).append(")");
        } else if (StringUtils.isNotEmpty(this.filterQuery)) {
            sbFilterQuery.append(this.filterQuery);
        }

        newSearch.setCustomFilterQuery(sbFilterQuery.toString().trim());
        // logger.trace("Custom filter query: {}", sbFilterQuery.toString().trim());

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
                            searchTerms, proximitySearchDistance);
            if (StringUtils.isEmpty(expandQuery) && activeSearchType == SearchHelper.SEARCH_TYPE_TERMS) {
                expandQuery = searchStringInternal;
            }
            newSearch.setExpandQuery(expandQuery);
        }

        // Override default result groups config if active group selected
        if (activeResultGroup != null) {
            newSearch.setResultGroups(Collections.singletonList(activeResultGroup));
        }

        // Publish the fully initialized search object to the instance field
        currentSearch = newSearch;
        currentSearch.execute(facets, searchTerms, hitsPerPage, navigationHelper.getLocale());

        // Make sure the current page isn't higher than the number of pages (e.g. when changing the number of hits per page)
        if (currentPage > getLastPage()) {
            setCurrentPage(getLastPage());
        }
    }

    /**
     * Set the current {@link io.goobi.viewer.model.urlresolution.ViewerPath} as the {@link #lastUsedSearchPage}. This is where returning to search
     * hit list from record will direct to
     */
    public void setLastUsedSearchPage() {
        this.lastUsedSearchPage = ViewHistory.getCurrentView(BeanUtils.getRequest());
    }

    /**
     * getFinalSolrQuery.
     *
     * @return the final Solr query string generated from the current or an empty search
     */
    public String getFinalSolrQuery() {
        if (this.currentSearch != null) {
            return this.currentSearch.generateFinalSolrQuery(null);
        }

        return new Search().generateFinalSolrQuery(null);
    }

    /**
     * getFilterQueries.
     *
     * @return a list of active Solr filter query strings from the current search and active facets
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
     * getCombinedFilterQuery.
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

    /**
     * @return the origin record from which the search was triggered, or null
     */
    public AdvancedSearchOrigin getAdvancedSearchOrigin() {
        return advancedSearchOrigin;
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
     * @param activeSearchType Active search type value to set
     */
    void setActiveSearchTypeTest(int activeSearchType) {
        this.activeSearchType = activeSearchType;
    }

    /**
     * resetActiveSearchType.
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
     * @param invisibleSearchString search string value to apply silently
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
     * @return the current search query string sanitized for use in a Pretty URL, or "-" if blank
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
     * @param searchString URL-decoded search string from the Pretty URL
     */
    public void setSearchStringForUrl(String searchString) {
        logger.trace("setSearchStringForUrl: {}", searchString);
        generateSimpleSearchString(searchString);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Wrapper for setSearchStringKeepCurrentPage() that also resets <code>currentPage</code>.
     */
    @Override
    public void setSearchString(String searchString) {
        logger.trace("setSearchString: {}", searchString);
        // Reset search result page
        currentPage = 1;
        this.searchString = StringTools.stripJS(searchString);
        generateSimpleSearchString(this.searchString);
    }

    void generateSimpleSearchString(final String inSearchString) {
        logger.trace("generateSimpleSearchString: {}", inSearchString);
        logger.trace("currentSearchFilter: {}", currentSearchFilter.getLabel());

        SimpleQueryBuilder builder = SimpleQueryBuilder.builder()
                .withSearchFilter(currentSearchFilter)
                .withFuzzySearchEnabled(fuzzySearchEnabled)
                .withSearchTerms(searchTerms)
                .build();

        QueryResult result = builder.build(inSearchString);

        searchString = result.getDisplaySearchString();
        searchStringInternal = result.getInternalQuery();
        proximitySearchDistance = result.getProximityDistance();
        this.searchTerms = result.getSearchTerms();

        if (StringUtils.isBlank(searchStringInternal) || "*".equals(searchStringInternal)) {
            setExactSearchString("");
        }

        logger.trace("search string: {}", searchStringInternal);
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
     * @param inSearchString URL-encoded Solr query string from the Pretty URL
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
            logger.warn(e.getMessage());
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

        // TODO re-add removing backslashes (but preserving escaped double quotes)?
        searchTerms = SearchHelper.extractSearchTermsFromQuery(searchStringInternal, discriminatorValue);
        logger.trace("searchTerms: {}", searchTerms);

        // TODO reset mode?
    }

    /**
     * For unit tests.
     * 

     */
    String getSearchStringInternal() {
        return searchStringInternal;
    }

    /**
     * For unit tests.
     * 

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
     * Getter for the field <code>searchSortingOption</code>.
     *
     * @return the currently active sort option applied to search results
     */
    public SearchSortingOption getSearchSortingOption() {
        return searchSortingOption;
    }

    /**
     * Setter for the field <code>searchSortingOption</code>.
     *
     * @param searchSortingOption the sort option to apply to the current search results
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
     * isDisplayResultGroupNames.
     *
     * @return true if activeResultGroup null; false otherwise
     */
    public boolean isDisplayResultGroupNames() {
        return activeResultGroup == null && DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled();
    }

    /**
     * getActiveContext.
     *
     * @return activeResultGroup name; "-" if none set
     */
    public String getActiveContext() {
        if (activeResultGroup != null) {
            return activeResultGroup.getName();
        } else if (advancedSearchFieldTemplate != null && !StringConstants.DEFAULT_NAME.equals(advancedSearchFieldTemplate)) {
            return advancedSearchFieldTemplate;
        }

        return "-";
    }

    /**
     * Setter for the field <code>activeResultGroup</code>.
     *
     * @param activeResultGroup search result group to activate, or null to deactivate
     */
    public void setActiveResultGroup(SearchResultGroup activeResultGroup) {
        this.activeResultGroup = activeResultGroup;
    }

    /**
     * Depending on configuration settings, sets the given value as the active search result group name and/or active advanced search template.
     *
     * @param activeContext Name of the active context
     */
    public void setActiveContext(String activeContext) {
        logger.trace("setActiveContext: {}", activeContext);
        if (DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled()) {
            setActiveResultGroupName(activeContext);
        }
        if (DataManager.getInstance().getConfiguration().getAdvancedSearchTemplateNames().size() > 1) {
            setAdvancedSearchFieldTemplate(activeContext);
        }
    }

    /**
     * getActiveResultGroupName. For URL building, use getActiveContext() instead.
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
     * Sets activeResultGroup via the given name.
     *
     * @param activeResultGroupName Name of the active context
     * @should select result group correctly
     * @should reset result group if new name not configured
     * @should reset result group if empty name given
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
                    return;
                }
            }
            logger.warn("Search result group name not found: {}", activeResultGroupName);
        }

        activeResultGroup = null;
    }

    
    public String getAdvancedSearchFieldTemplate() {
        return advancedSearchFieldTemplate;
    }

    /**
     * 
     * @param advancedSearchFieldTemplate Template name for advanced search fields
     */
    public void setAdvancedSearchFieldTemplate(String advancedSearchFieldTemplate) {
        logger.trace("setAdvancedSearchFieldTemplate: {}", advancedSearchFieldTemplate);
        if (advancedSearchFieldTemplate != null && advancedSearchFieldTemplate.equals(this.advancedSearchFieldTemplate)) {
            return;
        }

        if (advancedSearchFieldTemplate != null && !"-".equals(advancedSearchFieldTemplate)) {
            this.advancedSearchFieldTemplate = advancedSearchFieldTemplate;
            // Reset query items
            resetAdvancedSearchParameters();
            // Reset slider ranges
            facets.resetSliderRange();
            // Reset available facets
            facets.resetAvailableFacets();
            return;
        }

        this.advancedSearchFieldTemplate = DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultTemplateName();
        // Reset query items and slider ranges if active group is used as item field template
        resetAdvancedSearchParameters();
        facets.resetSliderRange();
    }

    /**
     * Matches the selected collection item in the advanced search to the current value of <code>currentCollection</code>.
     *
     * @should mirror facet items to search query items correctly
     * @should remove facet items from search query items correctly
     * @should add extra search query item if all items full
     * @should not replace query items already in use
     * @should not add identical hierarchical query items
     * @should change nothing if facet already exists in query items
     */
    public void mirrorAdvancedSearchCurrentHierarchicalFacets() {
        logger.trace("mirrorAdvancedSearchCurrentHierarchicalFacets");
        if (facets.getActiveFacets().isEmpty()) {
            // Reset hierarchical query items if no active facets selected
            List<SearchQueryItem> queryItems = new ArrayList<>(advancedSearchQueryGroup.getQueryItems());
            for (SearchQueryItem item : queryItems) {
                if (item.isHierarchical()) {
                    logger.trace("resetting current field value in advanced search: {}", item.getField());
                    item.setValue(null);
                }
            }
            return;
        }

        Set<SearchQueryItem> populatedQueryItems = new HashSet<>();
        List<IFacetItem> facetsItems = new ArrayList<>(facets.getActiveFacets());
        for (IFacetItem facetItem : facetsItems) {
            if (!facetItem.isHierarchial()) {
                continue;
            }
            // logger.trace("facet item: {}", facetItem); //NOSONAR Debug

            SearchQueryItem match = null;

            // First try to match item with exact field
            for (SearchQueryItem queryItem : advancedSearchQueryGroup.getQueryItems()) {
                if (!populatedQueryItems.contains(queryItem)
                        && (facetItem.getField().equals(queryItem.getField()) && facetItem.getValue().equals(queryItem.getValue()))) {
                    match = queryItem;
                    logger.trace("Found query item with same field+value: {}:{}", match.getField(), match.getValue());
                    break;
                }
            }

            // Match same field with no value selected
            if (match == null) {
                for (SearchQueryItem queryItem : advancedSearchQueryGroup.getQueryItems()) {
                    if (!populatedQueryItems.contains(queryItem)
                            && facetItem.getField().equals(queryItem.getField())
                            && StringUtils.isEmpty(queryItem.getValue())) {

                        match = queryItem;
                        logger.trace("Found same field with empty value: {}", queryItem.getField());
                        break;
                    }
                }
            }

            // If no exact field match found, try to re-purpose an unused item
            if (match == null) {
                for (SearchQueryItem queryItem : advancedSearchQueryGroup.getQueryItems()) {
                    // field:value pair already exists
                    if (!populatedQueryItems.contains(queryItem) && (queryItem.getField() == null || StringUtils.isEmpty(queryItem.getValue()))) {
                        match = queryItem;
                        logger.trace("updating query item: {}:{}", match.getField(), match.getValue());
                        break;
                    }
                }
            }

            if (match == null) {
                // If no search field is set up for collection search, add new field containing the currently selected collection
                match = new SearchQueryItem();
                if (!populatedQueryItems.contains(match)) {
                    advancedSearchQueryGroup.getQueryItems().add(match);
                }
            }

            match.setField(facetItem.getField());
            match.setValue(facetItem.getValue());
            populatedQueryItems.add(match);
        }
    }

    /**
     * removeRangeFacetAction.
     *
     * @param field Solr field name of the range facet to remove
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
     * removeFacetAction.
     *
     * @param facetQuery encoded facet query string identifying the facet to remove
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
                    getActiveContext(), this.getExactSearchString(), oPath.get().getCmsPage().getListPage(), this.getSortString(),
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
     * Setter for the field <code>currentPage</code>.
     *
     * @param currentPage the 1-based page number to navigate to in the search results
     */
    public void setCurrentPage(int currentPage) {
        logger.trace("setCurrentPage: {}", currentPage);
        this.currentPage = currentPage > 1 ? currentPage : 1;
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
     * setHitsCount.
     *
     * @param hitsCount the total number of search hits to store on the current search
     */
    public void setHitsCount(long hitsCount) {
        if (currentSearch != null) {
            currentSearch.setHitsCount(hitsCount);
        }
    }

    /**
     * Getter for the field <code>searchTerms</code>.
     *
     * @return map of Solr field names to the set of search terms entered for each field
     */
    public Map<String, Set<String>> getSearchTerms() {
        return searchTerms;
    }

    /**
     * Getter for the field <code>currentHitIndex</code>.
     *
     * @return the zero-based index of the currently displayed search hit
     */
    public int getCurrentHitIndex() {
        return currentHitIndex;
    }

    /**
     * For unit tests.
     * 

     */
    void setCurrentHitIndex(int currentHitIndex) {
        this.currentHitIndex = currentHitIndex;
    }

    /**
     * getCurrentHitIndexDisplay.
     *
     * @return a int.
     */
    public int getCurrentHitIndexDisplay() {
        return currentHitIndex + 1;
    }

    /**
     * increaseCurrentHitIndex.
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
     * Getter for the field <code>hitIndexOperand</code>.
     *
     * @return the operand used to calculate the next or previous hit index when navigating between hits
     */
    public int getHitIndexOperand() {
        return hitIndexOperand;
    }

    /**
     * Setter for the field <code>hitIndexOperand</code>.
     *
     * @param hitIndexOperand the operand used to calculate the next or previous hit index to navigate to
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
     * <p>Sets <code>currentSearchFilter</code> via the given label value.
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
     * resetSearchFilter.
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
     * resetCurrentHitIndex.
     */
    public void resetCurrentHitIndex() {
        currentHitIndex = -1;
    }

    /**
     * isSortingEnabled.
     *
     * @return true if search result sorting is enabled in the configuration, false otherwise
     */
    public boolean isSortingEnabled() {
        return DataManager.getInstance().getConfiguration().isSortingEnabled();
    }

    /**
     * Getter for the field <code>advancedSearchQueryGroup</code>.
     *
     * @return the root query group containing all advanced search field conditions
     */
    public SearchQueryGroup getAdvancedSearchQueryGroup() {
        return advancedSearchQueryGroup;
    }

    /**
     * For unit tests.
     * 

     */
    void setAdvancedSearchQueryGroup(SearchQueryGroup advancedSearchQueryGroup) {
        this.advancedSearchQueryGroup = advancedSearchQueryGroup;
    }

    /**
     * Populates the list of advanced search drop-down values for the given field. List is only generated once per user session.
     *
     * @param field The index field for which to get drop-down values.
     * @param language Translation language for the values.
     * @param hierarchical If true, the menu items will be listed in their corresponding hierarchy (e.g. DC)
     * @return a list of label/value pairs for use in advanced search drop-down menus for the given field
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
     *
     * <p>This method shouldn't throw exceptions, otherwise it can cause an IllegalStateException.
     *
     * @return a list of label/value pairs for all available collection names in the current user's language
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
     * @param language BCP 47 language tag for translating collection labels
     * @return a list of label/value pairs for all available collection names translated into the given language
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<StringPair> getAllCollections(String language)
            throws PresentationException, IndexUnreachableException, DAOException {
        return getAdvancedSearchSelectItems(SolrConstants.DC, language, true);
    }

    /**
     * getAdvancedSearchAllowedFields.
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
     * @param template advanced search field configuration template name
     * @param addSearchFilters If true, prepend configured search filters as fields
     * @return List of allowed advanced search fields
     * @should omit languaged fields for other languages
     * @should add search filters
     */
    public static List<AdvancedSearchFieldConfiguration> getAdvancedSearchAllowedFields(final String language, String template,
            boolean addSearchFilters) {
        // logger.trace("getAdvancedSearchAllowedFields: {} / {}", language, template);
        List<AdvancedSearchFieldConfiguration> fields =
                DataManager.getInstance().getConfiguration().getAdvancedSearchFields(template, false, language);
        if (fields == null) {
            return Collections.emptyList();
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
     * Getter for the field <code>currentSearch</code>.
     *
     * @return the Search object representing the active search query and its results
     */
    public Search getCurrentSearch() {
        return currentSearch;
    }

    /**
     * Setter for the field <code>currentSearch</code>.
     *
     * @param currentSearch the Search object representing the active search query and its results
     */
    public void setCurrentSearch(Search currentSearch) {
        logger.trace("Setting current search to {}", currentSearch);
        this.currentSearch = currentSearch;
    }

    /**
     * isFuzzySearchEnabled.
     *
     * @return true if fuzzy matching is enabled for search queries, false otherwise
     */
    public boolean isFuzzySearchEnabled() {
        return fuzzySearchEnabled;
    }

    /**
     * Setter for the field <code>fuzzySearchEnabled</code>.
     *
     * @param fuzzySearchEnabled true to enable fuzzy matching in search queries, false for exact matching
     */
    public void setFuzzySearchEnabled(boolean fuzzySearchEnabled) {
        this.fuzzySearchEnabled = fuzzySearchEnabled;
    }

    /**
     * saveSearchAction.
     *
     * @should add all values correctly
     * @return the empty navigation outcome string after persisting the current search
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
     * getRssUrl.
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
     * isSearchSavingEnabled.
     *
     * @return true if saving searches is enabled in the configuration, false otherwise
     */
    public boolean isSearchSavingEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchSavingEnabled();
    }

    /**
     * executeSavedSearchAction.
     *
     * @param search previously saved search object to re-execute
     * @return the empty navigation outcome string after restoring and executing the saved search
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
     * exportSearchAsRisAction.
     *
     * @return the empty navigation outcome string after writing the RIS export to the HTTP response
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
     * exportSearchAsExcelAction.
     *
     * @return an empty string after initiating the Excel export response
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
     * @param facesContext Current JSF FacesContext for writing the response
     * @param finalQuery Complete query with suffixes.
     * @param exportQuery Query constructed from the user's input, without any secret suffixes.
     * @param proximitySearchDistance Maximum word distance for proximity searches
     * @param locale Locale used for formatting exported cell values
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
     * Getter for the field <code>hitsPerPage</code>.
     *
     * @return the number of search results displayed per page
     */
    public int getHitsPerPage() {
        return hitsPerPage;
    }

    /**
     * Setter for the field <code>hitsPerPage</code>.
     *
     * @param hitsPerPage the number of search results to display per page
     */
    public void setHitsPerPage(int hitsPerPage) {
        logger.trace("setHitsPerPage: {}", hitsPerPage);
        this.hitsPerPage = hitsPerPage;
        setHitsPerPageSetterCalled(true);
    }

    /**
     * Like setHitsPerPage() but doesn't trigger the boolean.
     *
     * @param hitsPerPage the number of search results to display per page
     * @should not change hitsPerPageSetterCalled value
     */
    public void setHitsPerPageNoTrigger(int hitsPerPage) {
        // logger.trace("setHitsPerPageNoTrigger: {}", hitsPerPage); //NOSONAR Debug
        this.hitsPerPage = hitsPerPage;
    }

    /**
     * isHitsPerPageSetterCalled.
     *
     * @return true if the hits-per-page setter has been explicitly invoked during the current request, false otherwise
     */
    public boolean isHitsPerPageSetterCalled() {
        return hitsPerPageSetterCalled;
    }

    /**
     * Setter for the field <code>hitsPerPageSetterCalled</code>.
     *
     * @param hitsPerPageSetterCalled true if the hits-per-page setter has been explicitly invoked during the current request
     */
    public void setHitsPerPageSetterCalled(boolean hitsPerPageSetterCalled) {
        // logger.trace("setHitsPerPageSetterCalled: {}", hitsPerPageSetterCalled); //NOSONAR Debug
        this.hitsPerPageSetterCalled = hitsPerPageSetterCalled;
    }

    /**
     * Getter for the field <code>advancedSearchQueryInfo</code>.
     *
     * @return the HTML-escaped human-readable description of the current advanced search query
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
     * isDownloadReady.
     *
     * @return a Future resolving to true when the export download is ready
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
     * getTotalNumberOfVolumes.
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
     * @return the URL for the search page matching the currently active search type
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
     * getStructElement.
     *
     * @param pi persistent identifier of the record
     * @return the top-level StructElement for the record with the given PI
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
     * getCurrentSearchUrlPart.
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
     * updateFacetItem.
     *
     * @param field Solr field name of the facet to update
     * @param hierarchical true if the field uses hierarchical faceting
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
     * @param basePath Base URI to append search parameters to
     * @return {@link URI}
     */
    private URI getParameterPath(final URI basePath) {
        URI ret = ViewerPathBuilder.resolve(basePath, getActiveContext());
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
     * @param url Target URL to redirect to
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
     * @param field Solr field name to retrieve facet values for
     * @param subQuery optional Solr sub-query to filter the facet base set
     * @param resultLimit maximum number of facet values to return
     * @param reverseOrder true to sort facet values in reverse order
     * @return a list of facet items for the given field over all indexed documents, optionally filtered by the sub-query
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

    /** {@inheritDoc} */
    @Override
    public boolean isSearchPerformed() {
        return currentSearch != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExplicitSearchPerformed() {
        return StringUtils.isNotBlank(getExactSearchString().replace("-", ""));
    }

    /**
     * setFirstQueryItemValue.
     *
     * @param value search term to set on the first advanced query item
     */
    public void setFirstQueryItemValue(String value) {
        this.advancedSearchQueryGroup.getQueryItems().get(0).setValue(value);
    }

    /**
     * getFirstQueryItemValue.
     */
    public void getFirstQueryItemValue() {
        this.advancedSearchQueryGroup.getQueryItems().get(0).getValue();
    }

    /**
     * setBookmarkListName.
     *
     * @param name bookmark list name to set as advanced search query value
     */
    public void setBookmarkListName(String name) {
        SearchQueryItem item = this.advancedSearchQueryGroup.getQueryItems().get(0);
        item.setValue(name);
        item.setField(SolrConstants.BOOKMARKS);

    }

    /**
     * getBookmarkListName.
     *
     * @return name of the bookmark list set in the advanced search query
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
     * setBookmarkListName.
     *
     * @param key The sharing key to set
     */
    public void setBookmarkListSharedKey(String key) {
        SearchQueryItem item = this.advancedSearchQueryGroup.getQueryItems().get(0);
        item.setValue(PREFIX_KEY + key);
        item.setField(SolrConstants.BOOKMARKS);

    }

    /**
     * getBookmarkListName.
     *
     * @return sharing key of the bookmark list set in the advanced search query
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
     * Getter for the field <code>proximitySearchDistance</code>.
     *
     * @return a int
     */
    public int getProximitySearchDistance() {
        return proximitySearchDistance;
    }

    /**
     * searchInRecord.
     *
     * @param piField Solr field name holding the record identifier
     * @param piValue persistent identifier value to restrict the search to
     * @return Navigation outcome
     */
    public String searchInRecord(String piField, String piValue) {
        return searchInRecord(piField, piValue, null, null);
    }

    /**
     * searchInRecord.
     *
     * @param piField Solr field name holding the record identifier
     * @param piValue persistent identifier value to restrict the search to
     * @param date1 Start date for the calendar day range filter
     * @param date2 End date for the calendar day range filter
     * @return Navigation outcome
     */
    public String searchInRecord(String piField, String piValue, String date1, String date2) {
        logger.trace("searchInRecord: {}:{}", piField, piValue);
        // Clear any active facets from the browsing context so they don't pollute the search
        this.facets.resetActiveFacets();
        // reset all items except those containing values from the search input fields
        int index = 0;
        for (SearchQueryItem item : this.advancedSearchQueryGroup.getQueryItems()) {
            if (index != 1 && index != 2) {
                item.reset();
            }
            index++;
        }
        this.advancedSearchQueryGroup.getQueryItems().get(0).setField(piField);
        if (StringUtils.isNotBlank(piValue)) {
            this.advancedSearchQueryGroup.getQueryItems().get(0).setValue(piValue);
        }
        this.advancedSearchQueryGroup.getQueryItems().get(0).getLines().get(0).setOperator(SearchItemOperator.AND);
        this.advancedSearchQueryGroup.getQueryItems().get(1).setField(SearchHelper.SEARCH_FILTER_ALL.getField());
        this.advancedSearchQueryGroup.getQueryItems().get(1).setLabel(SearchHelper.SEARCH_FILTER_ALL.getLabel());
        this.advancedSearchQueryGroup.getQueryItems().get(1).getLines().get(0).setOperator(SearchItemOperator.AND);
        // Configure queryItems[2] for YEARMONTHDAY date range (range + datepicker derived from field config)
        if (StringUtils.isNotEmpty(date1) && StringUtils.isNotEmpty(date2)) {
            this.advancedSearchQueryGroup.getQueryItems().get(2).setField(SolrConstants.CALENDAR_DAY);
            this.advancedSearchQueryGroup.getQueryItems().get(2).setValue(date1);
            this.advancedSearchQueryGroup.getQueryItems().get(2).setValue2(date2);
            this.advancedSearchQueryGroup.getQueryItems().get(2).getLines().get(0).setOperator(SearchItemOperator.AND);
        }
        this.setActiveSearchType(1);
        logger.trace("Searching for: {}", this.advancedSearchQueryGroup.getQueryItems().get(1).getValue());

        String outcome = this.searchAdvanced();
        // Set advancedSearchOrigin AFTER searchAdvanced() because it calls resetSearchParameters() which would null it
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null && adb.getViewManager() != null) {
            this.advancedSearchOrigin = new AdvancedSearchOrigin(
                    piValue,
                    adb.getViewManager().getTopStructElement().getLabel(),
                    adb.getViewManager().getTopStructElement().getDocStructType());
        }
        return outcome;
    }

    /**
     * Executes an advanced search using the given field. The search value is set via the HTML component using this method.
     * 
     * @param queryField Advanced search field to query
     * @return Navigation outcome
     */
    public String searchInField(String queryField) {
        this.advancedSearchQueryGroup.getQueryItems().get(0).setField(queryField);
        this.advancedSearchQueryGroup.getQueryItems().get(0).getLines().get(0).setOperator(SearchItemOperator.AND);
        this.setActiveSearchType(1);

        return this.searchAdvanced();
    }

    /**
     * isSolrIndexReachable.
     *
     * @return true if Solr ping successful; false otherwise
     */
    public boolean isSolrIndexReachable() {
        return DataManager.getInstance().getSearchIndex().pingSolrIndex();
    }

    /**
     * hasGeoLocationHits.
     *
     * @return a boolean
     */
    public boolean hasGeoLocationHits() {
        return this.currentSearch != null && !this.currentSearch.isHasGeoLocationHits();
    }

    /**
     * getHitsLocations.
     *
     * @return a list of GeoJSON strings for each search hit that has geographic coordinates
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
     * Display the geo facet map if there are any hits available with geo coordinates.
     *
     * @return true if search hits with coordinates available; false otherwise
     */
    public boolean isShowGeoFacetMap() {
        return currentSearch != null && facets != null && (currentSearch.isHasGeoLocationHits() || facets.getGeoFacetting().hasFeature());
    }

    /**
     * getHitsMap.
     *
     * @return a GeoMap populated with the geo-location hits from the current search result
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
     * facetifyField.
     *
     * @param fieldName Solr field name to convert to its facet variant
     * @return Facet variant of the given fieldName
     */
    public String facetifyField(String fieldName) {
        return SearchHelper.facetifyField(fieldName);
    }

    /**
     * getFieldFacetValues.
     *
     * @param field Solr field name to retrieve facet values for
     * @param num maximum number of facet values to return; 0 for unlimited
     * @return List of facet values for the given field
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     */
    public List<FacetItem> getFieldFacetValues(String field, int num) throws IndexUnreachableException {
        return getFieldFacetValues(field, num, "");
    }

    /**
     * getFieldFacetValues.
     *
     * @param field Solr field name to retrieve facet values for
     * @param num maximum number of facet values to return; 0 for unlimited
     * @param filterQuery additional Solr filter query to restrict the facet base set
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
     * getSearchSortingOptions.
     *
     * @param language BCP 47 language tag for translating sort option labels
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
     * getQueryResultCount.
     *
     * @param query Solr query to count results for
     * @return Number of hits for the given query
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.PresentationException
     */
    public long getQueryResultCount(String query) throws IndexUnreachableException, PresentationException {
        String finalQuery = SearchHelper.buildFinalQuery(query, false, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        return DataManager.getInstance().getSearchIndex().getHitCount(finalQuery);
    }

    /**
     * getFinalSolrQueryEscaped.
     *
     * @return URL-encoded final query
     */
    public String getFinalSolrQueryEscaped() {
        return StringTools.encodeUrl(getFinalSolrQuery());
    }

    /**
     * getCombinedFilterQueryEscaped.
     *
     * @return URL-encoded combined filter query
     */
    public String getCombinedFilterQueryEscaped() {
        return StringTools.encodeUrl(getCombinedFilterQuery());
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    /**
     * The url of the viewer page loaded when the last search operation was performed, stored in {@link #lastUsedSearchPage} or the url of the
     * default search or searchAdvanced page depending on the state of this bean.
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
                        getActiveContext(),
                        getExactSearchString(),
                        getCurrentPage(),
                        getSortString(),
                        facets.getActiveFacetString());
            case SearchHelper.SEARCH_TYPE_TERMS:
                return PrettyUrlTools.getAbsolutePageUrl(
                        StringConstants.PRETTY_SEARCHTERM5,
                        getActiveContext(),
                        getExactSearchString(),
                        getCurrentPage(),
                        getSortString(),
                        facets.getActiveFacetString());
            default:
                return PrettyUrlTools.getAbsolutePageUrl(
                        StringConstants.PRETTY_NEWSEARCH5,
                        getActiveContext(),
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

    public HttpServletRequest getHttpRequest() {
        return this.request;
    }
}
