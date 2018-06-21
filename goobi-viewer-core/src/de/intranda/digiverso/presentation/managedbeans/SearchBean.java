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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.cms.itemfunctionality.SearchFunctionality;
import de.intranda.digiverso.presentation.model.search.BrowseElement;
import de.intranda.digiverso.presentation.model.search.FacetItem;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.search.SearchFacets;
import de.intranda.digiverso.presentation.model.search.SearchFilter;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.search.SearchHit;
import de.intranda.digiverso.presentation.model.search.SearchQueryGroup;
import de.intranda.digiverso.presentation.model.search.SearchQueryItem;
import de.intranda.digiverso.presentation.model.search.SearchQueryItem.SearchItemOperator;
import de.intranda.digiverso.presentation.model.urlresolution.ViewHistory;
import de.intranda.digiverso.presentation.model.urlresolution.ViewerPath;
import de.intranda.digiverso.presentation.model.viewer.BrowseDcElement;
import de.intranda.digiverso.presentation.model.viewer.BrowsingMenuFieldConfig;
import de.intranda.digiverso.presentation.model.viewer.LabeledLink;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * SearchBean
 */
@Named
@SessionScoped
public class SearchBean implements Serializable {

    private static final long serialVersionUID = 6962223613432267768L;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SearchBean.class);

    public static final String URL_ENCODING = "UTF8";

    @Inject
    private NavigationHelper navigationHelper;

    /** Max number of search hits to be displayed on one page. */
    private int hitsPerPage = DataManager.getInstance().getConfiguration().getSearchHitsPerPage();
    /**
     * Currently selected search type (regular, advanced, timeline, ...). This property is not private so it can be altered in unit tests (the setter
     * checks the config and may prevent setting certain values).
     */
    int activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
    /** Currently selected filter for the regular search. Possible values can be configured. */
    private SearchFilter currentSearchFilter = SearchHelper.SEARCH_FILTER_ALL;
    /** Solr query generated from the user's input (does not include facet filters or blacklists). */
    private String searchString = "";
    /** User-entered search query that is displayed in the search field after the search. */
    private String guiSearchString = "";
    /** Individual terms extracted from the user query (used for highlighting). */
    private Map<String, Set<String>> searchTerms = new HashMap<>();

    private boolean phraseSearch = false;
    /** Current search result page. */
    private int currentPage = 1;
    /** Index of the currently open search result (used for search result browsing). */
    private int currentHitIndex = -1;
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

    /** Empty constructor. */
    public SearchBean() {
        // the emptiness inside
    }

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
     * 
     */
    public void clearSearchItemLists() {
        advancedSearchSelectItems.clear();
    }

    /**
     * Executes the search using already set parameters. Usually called from Pretty URLs.
     * 
     * @return {@link String} null
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public String search() throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("search");
        updateBreadcrumbsForSearchHits();
        resetSearchResults();
        executeSearch();

        return "";
    }

    /**
     * Action method for search buttons (simple search).
     * 
     * @return
     */
    public String searchSimple() {
        return searchSimple(true);
    }

    public String searchSimple(boolean resetParameters) {
        logger.trace("searchSimple");
        resetSearchResults();
        if (resetParameters) {
            resetSearchParameters();
            facets.resetSliderRange();
        }
        generateSimpleSearchString(guiSearchString);
        
        return "pretty:newSearch5";
    }

    /**
     * Same as <code>searchSimple()</code> but resets the current facets.
     * 
     * @return
     */
    public String searchSimpleResetCollections() {
        facets.resetCurrentFacetString();
        return searchSimple();
    }

    /**
     * Same as <code>{@link #searchSimple()}</code> but sets the current facets to the given string
     * 
     * @return
     */
    public String searchSimpleSetFacets(String facetString) {
        facets.resetCurrentFacetString();
        facets.setCurrentFacetString(facetString);
        return searchSimple();
    }

    public String searchAdvanced() {
        return searchAdvanced(true);
    }

    public String searchAdvanced(boolean resetParameters) {
        logger.trace("searchAdvanced");
        updateBreadcrumbsForSearchHits();
        resetSearchResults();
        if (resetParameters) {
            resetSearchParameters();
            facets.resetSliderRange();
        }
        searchString = generateAdvancedSearchString(DataManager.getInstance().getConfiguration().isAggregateHits());

        return "pretty:searchAdvanced5";
    }

    /**
     * Search using currently set search string
     *
     * @return
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
     * @return
     * @should return correct Pretty URL ID
     */
    public String resetSearchAction() {
        logger.trace("resetSearchAction");
        generateSimpleSearchString("");
        setCurrentPage(1);
        setExactSearchString("");
        facets.resetCurrentFacets();
        mirrorAdvancedSearchCurrentHierarchicalFacets();
        resetSearchResults();
        resetSearchParameters(true);
        searchInCurrentItemString = null;

        switch (activeSearchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                return "pretty:" + PageType.advancedSearch.name();
            case SearchHelper.SEARCH_TYPE_TIMELINE:
                return "pretty:" + PageType.timelinesearch.name();
            case SearchHelper.SEARCH_TYPE_CALENDAR:
                return "pretty:" + PageType.calendarsearch.name();
            default:
                return "pretty:" + PageType.search.name();
        }
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

        guiSearchString = "";
    }

    /**
     * Resets search options for the advanced search.
     *
     * @param initialGroupNumber
     * @param initialItemNumber
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
     * @param reset
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
     */
    String generateAdvancedSearchString(boolean aggregateHits) {
        logger.trace("generateAdvancedSearchString");
        StringBuilder sb = new StringBuilder();
        StringBuilder sbInfo = new StringBuilder();

        searchTerms.clear();
        StringBuilder sbCurrentCollection = new StringBuilder();

        for (SearchQueryGroup queryGroup : advancedQueryGroups) {
            StringBuilder sbGroup = new StringBuilder();
            if (sb.length() > 0) {
                switch (advancedSearchGroupOperator) {
                    case 0:
                        sbInfo.append(' ').append(Helper.getTranslation("searchOperator_AND", BeanUtils.getLocale())).append("\n<br />");
                        break;
                    case 1:
                        sbInfo.append(' ').append(Helper.getTranslation("searchOperator_OR", BeanUtils.getLocale())).append("\n<br />");
                        break;
                    default:
                        sbInfo.append(' ').append(Helper.getTranslation("searchOperator_AND", BeanUtils.getLocale()).toUpperCase()).append(
                                "\n<br />");
                        break;
                }
            }
            sbInfo.append('(');

            for (SearchQueryItem queryItem : queryGroup.getQueryItems()) {
                // logger.trace("Query item: {}", queryItem.toString());
                if (StringUtils.isEmpty(queryItem.getField()) || StringUtils.isBlank(queryItem.getValue())) {
                    continue;
                }
                if (!sbInfo.toString().endsWith("(")) {
                    sbInfo.append(' ')
                            .append(Helper.getTranslation("searchOperator_" + queryGroup.getOperator().name(), BeanUtils.getLocale()))
                            .append(' ');
                }
                // Generate the hierarchical facet parameter from query items
                if (queryItem.isHierarchical()) {
                    logger.trace("{} is hierarchical", queryItem.getField());
                    sbCurrentCollection.append(queryItem.getField()).append(':').append(queryItem.getValue().trim()).append(";;").toString();
                    sbInfo.append(Helper.getTranslation(queryItem.getField(), BeanUtils.getLocale()))
                            .append(": \"")
                            .append(Helper.getTranslation(queryItem.getValue(), BeanUtils.getLocale()))
                            .append('"');
                    continue;
                }

                // Non-hierarchical fields
                if (searchTerms.get(SolrConstants.FULLTEXT) == null) {
                    searchTerms.put(SolrConstants.FULLTEXT, new HashSet<String>());
                }
                String itemQuery = queryItem.generateQuery(searchTerms.get(SolrConstants.FULLTEXT), aggregateHits);
                // logger.trace("Item query: {}", itemQuery);
                sbInfo.append(Helper.getTranslation(queryItem.getField(), BeanUtils.getLocale())).append(": ");
                switch (queryItem.getOperator()) {
                    case IS:
                    case PHRASE:
                        if (!queryItem.getValue().startsWith("\"")) {
                            sbInfo.append('"');
                        }
                        sbInfo.append(Helper.getTranslation(queryItem.getValue(), BeanUtils.getLocale()));
                        if (!queryItem.getValue().endsWith("\"")) {
                            sbInfo.append('"');
                        }
                        break;
                    default:
                        sbInfo.append(Helper.getTranslation(queryItem.getValue(), BeanUtils.getLocale()));
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
            facets.setCurrentFacetString(facets.getCurrentFacetStringPrefix() + sbCurrentCollection.toString());
        } else {
            facets.setCurrentFacetString("-");
        }

        // Add discriminator subquery, if set and configured to be part of the visible query
        if (DataManager.getInstance().getConfiguration().isSubthemeFilterQueryVisible()) {
            try {
                String discriminatorValueSubQuery = SearchHelper.getDiscriminatorFieldFilterSuffix(navigationHelper,
                        DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
                if (StringUtils.isNotEmpty(discriminatorValueSubQuery)) {
                    sb.insert(0, '(');
                    sb.append(')').append(discriminatorValueSubQuery);
                }
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
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

    /**
     * 
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public void executeSearch() throws PresentationException, IndexUnreachableException, DAOException {
        logger.debug("executeSearch; searchString: {}", searchString);
        mirrorAdvancedSearchCurrentHierarchicalFacets();

        String currentQuery = SearchHelper.prepareQuery(searchString, SearchHelper.getDocstrctWhitelistFilterSuffix());

        if (StringUtils.isEmpty(sortString)) {
            setSortString(DataManager.getInstance().getConfiguration().getDefaultSortField());
            logger.trace("Using default sorting: {}", sortString);
        }

        // Init search object
        currentSearch = new Search(activeSearchType, currentSearchFilter);
        currentSearch.setUserInput(guiSearchString);
        currentSearch.setQuery(searchString);
        currentSearch.setPage(currentPage);
        currentSearch.setSortString(sortString);
        currentSearch.setFacetString(facets.getCurrentFacetString());

        // Add search hit aggregation parameters, if enabled
        if (DataManager.getInstance().getConfiguration().isAggregateHits() && !searchTerms.isEmpty()) {
            String expandQuery = activeSearchType == 1 ? SearchHelper.generateAdvancedExpandQuery(advancedQueryGroups, advancedSearchGroupOperator)
                    : SearchHelper.generateExpandQuery(
                            SearchHelper.getExpandQueryFieldList(activeSearchType, currentSearchFilter, advancedQueryGroups), searchTerms,
                            phraseSearch);
            currentSearch.setExpandQuery(expandQuery);
        }

        currentSearch.execute(facets, searchTerms, hitsPerPage, advancedSearchGroupOperator, advancedQueryGroups, navigationHelper.getLocale());
    }

    /**
     * @return the activeSearchType
     */
    public int getActiveSearchType() {
        return activeSearchType;
    }

    /**
     * @param activeSearchType the activeSearchType to set
     */
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

    public void resetActiveSearchType() {
        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
    }

    public List<String> autocomplete(String suggest) throws IndexUnreachableException {
        logger.trace("autocomplete: {}", suggest);
        List<String> result = SearchHelper.searchAutosuggestion(suggest, facets.getCurrentFacets());
        Collections.sort(result);

        return result;
    }

    public boolean isSearchInDcFlag() {
        for (FacetItem item : facets.getCurrentFacets()) {
            if (item.getField().equals(SolrConstants.DC)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return the searchString
     */
    public String getSearchString() {
        return guiSearchString;
    }

    /**
     * Wrapper method for Pretty URL mappings (so that the values is never empty).
     *
     * @return
     */
    public String getSearchStringForUrl() {
        if (StringUtils.isEmpty(guiSearchString)) {
            return "-";
        }
        return guiSearchString;
    }

    /**
     * Wrapper method for Pretty URL mappings.
     *
     * @param searchString
     */
    public void setSearchStringForUrl(String searchString) {
        logger.trace("setSearchStringForUrl: {}", searchString);
        generateSimpleSearchString(searchString);
    }

    /**
     * Wrapper for setSearchStringKeepCurrentPage() that also resets <code>currentPage</code>.
     *
     * @param searchString
     */
    public void setSearchString(String searchString) {
        logger.trace("setSearchString: {}", searchString);
        // Reset search result page
        currentPage = 1;
        guiSearchString = searchString;
        generateSimpleSearchString(searchString);
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

        logger.trace("Resetting selected facets...");
        facets.setCurrentFacetString("-");

        guiSearchString = inSearchString;
        searchString = "";
        searchTerms.clear();
        phraseSearch = false;

        inSearchString = inSearchString.trim();
        if (StringUtils.isNotEmpty(inSearchString)) {
            if ("*".equals(inSearchString)) {
                searchString = new StringBuilder("(").append(SolrConstants.ISWORK)
                        .append(":true OR ")
                        .append(SolrConstants.ISANCHOR)
                        .append(":true)")
                        .append(SearchHelper.getDocstrctWhitelistFilterSuffix())
                        .toString();
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
                if (searchTerms.get(SolrConstants.OVERVIEWPAGE_DESCRIPTION) == null) {
                    searchTerms.put(SolrConstants.OVERVIEWPAGE_DESCRIPTION, new HashSet<String>());
                }
                if (searchTerms.get(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT) == null) {
                    searchTerms.put(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT, new HashSet<String>());
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
                            }
                            sb.append(SolrConstants.DEFAULT).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.FULLTEXT).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.NORMDATATERMS).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.UGCTERMS).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.OVERVIEWPAGE_DESCRIPTION).append(":(\"").append(phrase).append("\") OR ");
                            sb.append(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT).append(":(\"").append(phrase).append("\")");
                            for (String field : searchTerms.keySet()) {
                                searchTerms.get(field).add(phrase);
                            }
                        } else {
                            // Specific filter selected
                            if (searchTerms.get(SolrConstants.FULLTEXT) == null) {
                                Set<String> terms = new HashSet<>();
                                searchTerms.put(SolrConstants.FULLTEXT, terms);
                            }
                            if (currentSearchFilter.getField().equals(SolrConstants.OVERVIEWPAGE)) {
                                sb.append(SolrConstants.OVERVIEWPAGE_DESCRIPTION).append(":(\"").append(phrase).append("\") OR ");
                                sb.append(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT).append(":(\"").append(phrase).append("\")");
                            } else {
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
                                        default:
                                            sb.append(currentSearchFilter.getField()).append(":(\"").append(phrase).append("\")");
                                            break;
                                    }
                                } else {
                                    sb.append(currentSearchFilter.getField()).append(":(\"").append(phrase).append("\")");
                                }

                            }
                            searchTerms.get(currentSearchFilter.getField()).add(phrase);
                        }
                        sb.append(" AND ");
                    }
                }
                searchString = sb.toString();
            } else {
                // Non-phrase search
                inSearchString = inSearchString.replace(" &&", "");
                String[] termsSplit = inSearchString.split(SearchHelper.SEARCH_TERM_SPLIT_REGEX);

                // Clean up terms and create OR-connected groups
                List<String> preparedTerms = new ArrayList<>(termsSplit.length);
                for (int i = 0; i < termsSplit.length; ++i) {
                    String term = termsSplit[i].trim();
                    String unescapedTerm = cleanUpSearchTerm(term);
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
                            String unescapedNextTerm = cleanUpSearchTerm(termsSplit[i + 1]);
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
                            sbOuter.append(") OR ");
                        }
                        sbOuter.append(SolrConstants.DEFAULT).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.FULLTEXT).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.NORMDATATERMS).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.UGCTERMS).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.OVERVIEWPAGE_DESCRIPTION).append(":(").append(sbInner.toString());
                        sbOuter.append(") OR ").append(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT).append(":(").append(sbInner.toString()).append(
                                ')');
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
                                case SolrConstants.OVERVIEWPAGE:
                                    if (currentSearchFilter.getField().equals(SolrConstants.OVERVIEWPAGE)) {
                                        sbOuter.append(SolrConstants.OVERVIEWPAGE_DESCRIPTION).append(":(").append(sbInner.toString()).append(
                                                ") OR ");
                                        sbOuter.append(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT).append(":(").append(sbInner.toString()).append(
                                                ')');
                                    }
                                    break;
                                default:
                                    sbOuter.append(currentSearchFilter.getField()).append(":(").append(sbInner.toString()).append(')');
                                    break;
                            }
                        } else {
                            sbOuter.append(currentSearchFilter.getField()).append(":(").append(sbInner.toString()).append(')');
                        }
                    }
                    searchString += sbOuter.toString();
                }

            }
            if (searchString.endsWith(" OR ")) {
                searchString = searchString.substring(0, searchString.length() - 4);
            } else if (searchString.endsWith(" AND ")) {
                searchString = searchString.substring(0, searchString.length() - 5);
            }

            // Add discriminator subquery, if set and configurated to be part of the visible query
            if (DataManager.getInstance().getConfiguration().isSubthemeFilterQueryVisible()) {
                try {
                    String discriminatorValueSubQuery = SearchHelper.getDiscriminatorFieldFilterSuffix(navigationHelper,
                            DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
                    if (StringUtils.isNotEmpty(discriminatorValueSubQuery)) {
                        searchString = new StringBuilder("(").append(searchString).append(')').append(discriminatorValueSubQuery).toString();
                    }
                } catch (IndexUnreachableException e) {
                    logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                }

            }

            logger.trace("search string: {}", searchString);
            logger.trace("search terms: {}", searchTerms.toString());
        } else {
            guiSearchString = "";
        }
    }

    /**
     * Removes illegal characters from an individual search term. Do not use on whole queries!
     *
     * @param s The term to clean up.
     * @return Cleaned up term.
     * @should remove illegal chars correctly
     * @should preserve truncation
     * @should preserve negation
     */
    protected static String cleanUpSearchTerm(String s) {
        if (StringUtils.isNotEmpty(s)) {
            boolean addNegation = false;
            boolean addLeftTruncation = false;
            boolean addRightTruncation = false;
            if (s.charAt(0) == '-') {
                addNegation = true;
                s = s.substring(1);
            } else if (s.charAt(0) == '*') {
                addLeftTruncation = true;
            }
            if (s.endsWith("*")) {
                addRightTruncation = true;
            }
            s = s.replace("*", "");
            // s = s.replace(".", "");
            s = s.replace("(", "");
            s = s.replace(")", "");
            if (addNegation) {
                s = '-' + s;
            } else if (addLeftTruncation) {
                s = '*' + s;
            }
            if (addRightTruncation) {
                s += '*';
            }
        }

        return s;
    }

    public String getExactSearchString() {
        if (searchString.length() == 0) {
            return "-";
        }
        String ret = BeanUtils.escapeCriticalUrlChracters(searchString);
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
     * @param searchString the searchString to set
     */
    public void setExactSearchString(String inSearchString) {
        logger.debug("setExactSearchString: {}", inSearchString);
        if ("-".equals(inSearchString)) {
            inSearchString = "";
            guiSearchString = "";
        }
        searchString = BeanUtils.unescapeCriticalUrlChracters(inSearchString);
        try {
            searchString = URLDecoder.decode(searchString, URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
        } catch (IllegalArgumentException e) {
        }
        // Parse search terms from the query (unescape spaces first)
        String discriminatorValue = null;
        if (navigationHelper != null) {
            try {
                discriminatorValue = navigationHelper.getSubThemeDiscriminatorValue();
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
        }
        searchTerms = SearchHelper.extractSearchTermsFromQuery(searchString.replace("\\", ""), discriminatorValue);
        logger.trace("searchTerms: {}", searchTerms);

        // TODO reset mode?
    }

    /**
     * JSF expects a getter, too.
     *
     * @return
     */
    public String getExactSearchStringResetGui() {
        return getExactSearchString();
    }

    /**
     * Works exactly like setExactSearchString() but guiSearchString is always reset.
     *
     * @param inSearchString
     */
    public void setExactSearchStringResetGui(String inSearchString) {
        setExactSearchString(inSearchString);
        guiSearchString = "";
    }

    /**
     * @param sortString the sortString to set
     */
    public void setSortString(String sortString) {
        if ("-".equals(sortString)) {
            this.sortString = "";
        } else if (sortString != null && "RANDOM".equals(sortString.toUpperCase())) {
            Random random = new Random();
            this.sortString = new StringBuilder().append("random_").append(random.nextInt(Integer.MAX_VALUE)).toString();
        } else {
            this.sortString = sortString;
        }
        //        sortFields = SearchHelper.parseSortString(this.sortString, navigationHelper);
    }

    /**
     * @return the sortString
     */
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
     */
    public void mirrorAdvancedSearchCurrentHierarchicalFacets() {
        logger.trace("mirrorAdvancedSearchCurrentHierarchicalFacets");
        if (!facets.getCurrentFacets().isEmpty()) {
            if (!advancedQueryGroups.isEmpty()) {
                SearchQueryGroup queryGroup = advancedQueryGroups.get(0);
                if (!queryGroup.getQueryItems().isEmpty()) {
                    int index = 0;
                    for (FacetItem facetItem : facets.getCurrentFacets()) {
                        if (!facetItem.isHierarchial()) {
                            continue;
                        }
                        if (index < queryGroup.getQueryItems().size()) {
                            // Fill existing search query items
                            SearchQueryItem item = queryGroup.getQueryItems().get(index);
                            while (!item.isHierarchical() && StringUtils.isNotEmpty(item.getValue())
                                    && index + 1 < queryGroup.getQueryItems().size()) {
                                // Skip items that already have values
                                ++index;
                                item = queryGroup.getQueryItems().get(index);
                            }
                            item.setField(facetItem.getField());
                            item.setOperator(SearchItemOperator.IS);
                            item.setValue(facetItem.getValue());
                        } else {
                            // If no search field is set up for collection search, add new field containing the currently selected collection
                            SearchQueryItem item = new SearchQueryItem(BeanUtils.getLocale());
                            item.setField(facetItem.getField());
                            item.setOperator(SearchItemOperator.IS);
                            item.setValue(facetItem.getValue());
                            queryGroup.getQueryItems().add(item);
                        }
                        ++index;
                    }
                    // If additional query items are configured for collections but are no longer in use, reset them
                    if (index < queryGroup.getQueryItems().size()) {
                        for (int i = index; i < queryGroup.getQueryItems().size(); ++i) {
                            SearchQueryItem item = queryGroup.getQueryItems().get(i);
                            if (item.isHierarchical()) {
                                item.reset();
                                logger.trace("Reset advanced query item {}", i);
                            }
                        }
                    }
                    // If all items are full, add a new one for user query
                    // if (currentHierarchicalFacets.size() ==
                    // queryGroup.getQueryItems().size()) {
                    // queryGroup.getQueryItems().add(new
                    // SearchQueryItem(BeanUtils.getLocale()));
                    // }
                }
            }
        } else if (!advancedQueryGroups.isEmpty()) {
            SearchQueryGroup queryGroup = advancedQueryGroups.get(0);
            for (SearchQueryItem item : queryGroup.getQueryItems()) {
                if (item.isHierarchical()) {
                    logger.trace("resetting current field in advanced search: {}", item.getField());
                    item.reset();
                }
            }
        }
    }

    /**
     * 
     * @param facetQuery
     * @return
     * @should remove facet correctly
     */
    public String removeFacetAction(String facetQuery) {

        //redirect to current cms page if this action takes place on a cms page
        Optional<ViewerPath> oPath = ViewHistory.getCurrentView(BeanUtils.getRequest());
        if (oPath.isPresent() && oPath.get().isCmsPage()) {
            facets.removeFacetAction(facetQuery, "pretty:browse4");
            SearchFunctionality search = oPath.get().getCmsPage().getSearch();
            search.redirectToSearchUrl();
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

    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * @param currentPage the currentPage to set
     */
    public void setCurrentPage(int currentPage) {
        logger.trace("setCurrentPage: {}", currentPage);
        this.currentPage = currentPage;
    }

    /**
     * @return the hitsCount
     */
    public long getHitsCount() {
        if (currentSearch != null) {
            // logger.trace("Hits count = {}", currentSearch.getHitsCount());
            return currentSearch.getHitsCount();
        }
        // logger.warn("No Search object available");

        return 0;
    }

    /**
     * @param hitsCount the hitsCount to set
     */
    public void setHitsCount(long hitsCount) {
        if (currentSearch != null) {
            currentSearch.setHitsCount(hitsCount);
        }
    }

    /**
     * @return the searchTerms
     */
    public Map<String, Set<String>> getSearchTerms() {
        return searchTerms;
    }

    /**
     * @return the currentHitIndex
     */
    public int getCurrentHitIndex() {
        return currentHitIndex;
    }

    public int getCurrentHitIndexDisplay() {
        return currentHitIndex + 1;
    }

    public void increaseCurrentHitIndex() {
        if (hitIndexOperand != 0 && currentSearch != null && currentHitIndex < currentSearch.getHitsCount() - 1) {
            int old = currentHitIndex;
            currentHitIndex += hitIndexOperand;
            if (currentHitIndex < 0) {
                currentHitIndex = 0;
            } else if (currentHitIndex >= currentSearch.getHitsCount()) {
                currentHitIndex = (int) (currentSearch.getHitsCount() - 1);
            }
            hitIndexOperand = 0; // reset operand

            logger.trace("increaseCurrentHitIndex: {}->{}", old, currentHitIndex);
        }
    }

    /**
     * @return the hitIndexOperand
     */
    public int getHitIndexOperand() {
        return hitIndexOperand;
    }

    /**
     * @param hitIndexOperand the hitIndexOperand to set
     */
    public void setHitIndexOperand(int hitIndexOperand) {
        this.hitIndexOperand = hitIndexOperand;
    }

    /**
     * Returns the index of the currently displayed BrowseElement, if it is present in the search hit list.
     *
     * @param pi Record identifier of the loaded record.
     * @param page Page number of he loaded record.
     * @param aggregateHits If true, only the identifier has to match, page number is ignored.
     * @return The index of the currently displayed BrowseElement in the search hit list; -1 if not present.
     */
    public void findCurrentHitIndex(String pi, int page, boolean aggregateHits) {
        logger.trace("findCurrentHitIndex: {}/{}", pi, page);
        currentHitIndex = 0;
        if (currentSearch != null && !currentSearch.getHits().isEmpty()) {
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
        }

        currentHitIndex = -1;
    }

    /**
     * Returns the next BrowseElement in the hit list relative to the given index.
     *
     * @return Next BrowseElement in the list; same BrowseElement if this is the last index in the list.
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public BrowseElement getNextElement() throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("getNextElement: {}", currentHitIndex);
        if (currentHitIndex > -1 && currentSearch != null) {
            if (currentHitIndex < currentSearch.getHitsCount() - 1) {
                return SearchHelper.getBrowseElement(searchString, currentHitIndex + 1, currentSearch.getSortFields(),
                        facets.generateFacetFilterQueries(advancedSearchGroupOperator), SearchHelper.generateQueryParams(), searchTerms,
                        BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
            }
            return SearchHelper.getBrowseElement(searchString, currentHitIndex, currentSearch.getSortFields(),
                    facets.generateFacetFilterQueries(advancedSearchGroupOperator), SearchHelper.generateQueryParams(), searchTerms,
                    BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
        }

        return null;
    }

    /**
     * Returns the previous BrowseElement in the hit list relative to the given index.
     *
     * @return Previous BrowseElement in the list; same BrowseElement if this is the first index in the list.
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public BrowseElement getPreviousElement() throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("getPreviousElement: {}", currentHitIndex);
        if (currentHitIndex > -1 && currentSearch != null) {
            if (currentHitIndex > 0) {
                return SearchHelper.getBrowseElement(searchString, currentHitIndex - 1, currentSearch.getSortFields(),
                        facets.generateFacetFilterQueries(advancedSearchGroupOperator), SearchHelper.generateQueryParams(), searchTerms,
                        BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
            } else if (currentSearch.getHitsCount() > 0) {
                return SearchHelper.getBrowseElement(searchString, currentHitIndex, currentSearch.getSortFields(),
                        facets.generateFacetFilterQueries(advancedSearchGroupOperator), SearchHelper.generateQueryParams(), searchTerms,
                        BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
            }
        }

        return null;
    }

    public List<SearchFilter> getSearchFilters() {
        return DataManager.getInstance().getConfiguration().getSearchFilters();
    }

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
     * Sets <code>currentSearchFilter</code> via the given label value.
     *
     * @param searchFilterLabel
     */
    public void setCurrentSearchFilterString(String searchFilterLabel) {
        logger.trace("setCurrentSearchFilterString: {}", searchFilterLabel);
        for (SearchFilter filter : getSearchFilters()) {
            if (filter.getLabel().equals(searchFilterLabel)) {
                this.currentSearchFilter = filter;
                logger.debug("currentSearchFilter: {}", this.currentSearchFilter.getField());
                break;
            }
        }
    }

    public void resetSearchFilter() {
        currentSearchFilter = null;
        for (SearchFilter filter : getSearchFilters()) {
            if (filter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                currentSearchFilter = filter;
                break;
            }
        }
    }

    public void resetCurrentHitIndex() {
        currentHitIndex = -1;
    }

    public boolean isSortingEnabled() {
        return DataManager.getInstance().getConfiguration().isSortingEnabled();
    }

    /**
     * This is used for flipping search result pages (so that the breadcrumb always has the last visited result page as its URL).
     */
    public void updateBreadcrumbsForSearchHits() {
        //        if (!facets.getCurrentHierarchicalFacets().isEmpty()) {
        //            updateBreadcrumbsWithCurrentUrl(facets.getCurrentHierarchicalFacets().get(0).getValue().replace("*", ""),
        //                    NavigationHelper.WEIGHT_ACTIVE_COLLECTION);
        //        } else {
        updateBreadcrumbsWithCurrentUrl("searchHitNavigation", NavigationHelper.WEIGHT_SEARCH_RESULTS);
        //        }
    }

    /**
     * Adds a new breadcrumb for the current Pretty URL.
     *
     * @param name Breadcrumb name.
     * @param weight The weight of the link.
     */
    private void updateBreadcrumbsWithCurrentUrl(String name, int weight) {
        if (navigationHelper != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            URL url = PrettyContext.getCurrentInstance(request).getRequestURL();
            navigationHelper.updateBreadcrumbs(new LabeledLink(name, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + url.toURL(), weight));
        }
    }

    @Deprecated
    public String getCurrentQuery() {
        return getSearchString();

    }

    // temporary needed to set search string for calendar
    @Deprecated
    public void setCurrentQuery(String query) {
        setSearchString(query);
    }

    public String getDocstrctWhitelistFilterSuffix() {
        return SearchHelper.getDocstrctWhitelistFilterSuffix().substring(5);
    }

    /**
     * @return the advancedQueryGroups
     */
    public List<SearchQueryGroup> getAdvancedQueryGroups() {
        // logger.trace("getAdvancedQueryGroups: {}", advancedQueryGroups.size());
        return advancedQueryGroups;
    }

    /**
     * 
     * @return
     * @should add group correctly
     */
    public boolean addNewAdvancedQueryGroup() {
        return advancedQueryGroups
                .add(new SearchQueryGroup(BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber()));
    }

    /**
     * 
     * @param group
     * @return
     * @should remove group correctly
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
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<StringPair> getAdvancedSearchSelectItems(String field, String language, boolean hierarchical)
            throws PresentationException, IndexUnreachableException {
        // logger.trace("getAdvancedSearchSelectItems: {}", field);
        if (field == null) {
            throw new IllegalArgumentException("field may not be null.");
        }
        if (language == null) {
            throw new IllegalArgumentException("language may not be null.");
        }
        String key = new StringBuilder(language).append('_').append(field).toString();
        List<StringPair> ret = advancedSearchSelectItems.get(key);
        if (ret == null) {
            ret = new ArrayList<>();
            logger.trace("Generating drop-down values for {}", field);
            if (hierarchical) {
                BrowseBean browseBean = BeanUtils.getBrowseBean();
                if (browseBean == null) {
                    browseBean = new BrowseBean();
                }
                // Make sure displayDepth is at configured to the desired depth for this field (or -1 for complete depth)
                int displayDepth = DataManager.getInstance().getConfiguration().getCollectionDisplayDepthForSearch(field);
                List<BrowseDcElement> elementList = browseBean.getList(field, displayDepth);
                StringBuilder sbItemLabel = new StringBuilder();
                for (BrowseDcElement dc : elementList) {
                    for (int i = 0; i < dc.getLevel(); ++i) {
                        sbItemLabel.append("- ");
                    }
                    sbItemLabel.append(Helper.getTranslation(dc.getName(), null));
                    ret.add(new StringPair(dc.getName(), sbItemLabel.toString()));
                    sbItemLabel.setLength(0);
                }
                advancedSearchSelectItems.put(key, ret);
            } else {
                new BrowsingMenuFieldConfig(field, null, null, false);
                String suffix = SearchHelper.getAllSuffixes(DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery());
                String facetField = field.replace(SolrConstants._UNTOKENIZED, "").replace("MD_", "FACET_");
                List<String> values = SearchHelper.getFacetValues(field + ":[* TO *]" + suffix, facetField, 0);
                for (String value : values) {
                    ret.add(new StringPair(value, Helper.getTranslation(value, null)));
                }

                Collections.sort(ret);
                advancedSearchSelectItems.put(key, ret);
            }
            logger.trace("Generated {} values", ret.size());
        }

        return ret;
    }

    /**
     * Returns drop-down items for all collection names. Convenience method that retrieves the current language from <code>NavigationHelper</code>.
     * This method shouldn't throw exceptions, otherwise it can cause an IllegalStateException.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<StringPair> getAllCollections() {
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
        }

        return new ArrayList<>();
    }

    /**
     * Returns drop-down items for all collection names. The displayed values are translated into the given language.
     *
     * @param language
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<StringPair> getAllCollections(String language) throws PresentationException, IndexUnreachableException {
        return getAdvancedSearchSelectItems(SolrConstants.DC, language, true);
    }

    /**
     * @return the advancedSearchGroupOperator
     */
    public int getAdvancedSearchGroupOperator() {
        return advancedSearchGroupOperator;
    }

    /**
     * @param advancedSearchGroupOperator the advancedSearchGroupOperator to set
     */
    public void setAdvancedSearchGroupOperator(int advancedSearchGroupOperator) {
        this.advancedSearchGroupOperator = advancedSearchGroupOperator;
    }

    /**
     * Returns index field names allowed for advanced search use. TODO from config
     *
     * @return
     */
    public List<String> getAdvancedSearchAllowedFields() {
        List<String> fields = DataManager.getInstance().getConfiguration().getAdvancedSearchFields();
        if (fields == null) {
            fields = new ArrayList<>();
        }
        fields.add(0, SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS);

        return fields;
    }

    /**
     * @return the searchInCurrentItemString
     */
    public String getSearchInCurrentItemString() {
        return searchInCurrentItemString;
    }

    /**
     * @param searchInCurrentItemString the searchInCurrentItemString to set
     */
    public void setSearchInCurrentItemString(String searchInCurrentItemString) {
        // Reset the advanced search parameters prior to setting
        resetAdvancedSearchParameters(1, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
        this.searchInCurrentItemString = searchInCurrentItemString;
    }

    /**
     * @return the currentSearch
     */
    public Search getCurrentSearch() {
        return currentSearch;
    }

    /**
     * @param currentSearch the currentSearch to set
     */
    public void setCurrentSearch(Search currentSearch) {
        logger.trace("Setting current search to {}", currentSearch);
        this.currentSearch = currentSearch;
    }

    /**
     *
     * @return
     * @throws DAOException
     * @should add all values correctly
     */
    public String saveSearchAction() throws DAOException {
        if (StringUtils.isBlank(currentSearch.getName())) {
            Messages.error("nameRequired");
            return "";
        }

        //        currentSearch.setUserInput(guiSearchString);
        //        currentSearch.setQuery(searchString);
        //        currentSearch.setPage(currentPage);
        //        currentSearch.setSearchType(activeSearchType);
        //        currentSearch.setSearchFilter(currentSearchFilter);
        //        // regular facets
        //        if (!facets.getCurrentFacets()
        //                .isEmpty()) {
        //            currentSearch.setFacetString(facets.getCurrentFacetString());
        //        }
        //        // hierarchical facets
        //        if (!facets.getCurrentHierarchicalFacets()
        //                .isEmpty()) {
        //            currentSearch.setHierarchicalFacetString(facets.getCurrentHierarchicalFacetString());
        //        }
        //        // sorting
        //        if (StringUtils.isNotEmpty(sortString)) {
        //            currentSearch.setSortString(sortString);
        //        }

        currentSearch.setLastHitsCount(currentSearch.getHitsCount());

        UserBean ub = BeanUtils.getUserBean();
        if (ub != null) {
            currentSearch.setOwner(ub.getUser());
        }
        currentSearch.setDateUpdated(new Date());
        if (DataManager.getInstance().getDao().addSearch(currentSearch)) {
            currentSearch.setSaved(true);
            Messages.info("saveSearchSuccess");
        } else {
            Messages.error("errSave");
        }

        return "";
    }

    public String getRssUrl() {
        if (searchString != null) {
            String currentQuery = SearchHelper.prepareQuery(searchString, SearchHelper.getDocstrctWhitelistFilterSuffix());
            try {
                return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/')
                        .append(NavigationHelper.URL_RSS)
                        .append("?q=")
                        .append(URLEncoder.encode(currentQuery, URL_ENCODING))
                        .toString();
            } catch (UnsupportedEncodingException e) {
                logger.warn("Could not encode query '{}' for URL", currentQuery);
                return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/')
                        .append(NavigationHelper.URL_RSS)
                        .append("?q=")
                        .append(currentQuery)
                        .toString();
            }
        }

        return null;
    }

    /**
     *
     * @return
     */
    public boolean isSearchSavingEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchSavingEnabled();
    }

    public String executeSavedSearchAction(Search search) {
        logger.trace("executeSavedSearchAction");
        if (search == null) {
            throw new IllegalArgumentException("search may not be null");
        }

        guiSearchString = search.getUserInput();
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
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String exportSearchAsExcelAction() throws IndexUnreachableException {
        logger.trace("exportSearchAsExcelAction");
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        downloadReady = new FutureTask<>(new Callable<Boolean>() {

            @Override
            public Boolean call() throws InterruptedException {
                if (!facesContext.getResponseComplete()) {
                    final SXSSFWorkbook wb = buildExcelSheet(facesContext);
                    if (wb == null) {
                        return Boolean.FALSE;
                    } else if (Thread.interrupted()) {
                        return Boolean.FALSE;
                    }
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
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
        });
        executor.submit(downloadReady);

        try {
            int timeout = DataManager.getInstance().getConfiguration().getExcelDownloadTimeout(); //[s]
            if (downloadReady.get(timeout, TimeUnit.SECONDS)) {
                logger.trace("Download ready");
                //            	Messages.info("download_ready");
                downloadComplete.get(timeout, TimeUnit.SECONDS);
                logger.trace("Download complete");
            }
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
     * @return
     * @throws InterruptedException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    private SXSSFWorkbook buildExcelSheet(final FacesContext facesContext) throws InterruptedException {
        try {
            String currentQuery = SearchHelper.prepareQuery(searchString, SearchHelper.getDocstrctWhitelistFilterSuffix());
            final String query = SearchHelper.buildFinalQuery(currentQuery, DataManager.getInstance().getConfiguration().isAggregateHits());
            Map<String, String> params = SearchHelper.generateQueryParams();
            final SXSSFWorkbook wb = SearchHelper.exportSearchAsExcel(query, currentQuery, currentSearch.getSortFields(),
                    facets.generateFacetFilterQueries(advancedSearchGroupOperator), params, searchTerms, navigationHelper.getLocale(),
                    DataManager.getInstance().getConfiguration().isAggregateHits(), BeanUtils.getRequest());
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            facesContext.getExternalContext().responseReset();
            facesContext.getExternalContext().setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            facesContext.getExternalContext().setResponseHeader("Content-Disposition",
                    "attachment;filename=\"viewer_search_" + DateTools.formatterISO8601DateTime.print(System.currentTimeMillis()) + ".xlsx\"");
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
     * @return the hitsPerPage
     */
    public int getHitsPerPage() {
        return hitsPerPage;
    }

    /**
     * @param hitsPerPage the hitsPerPage to set
     */
    public void setHitsPerPage(int hitsPerPage) {
        this.hitsPerPage = hitsPerPage;
    }

    public void resetHitsPerPage() {
        setHitsPerPage(DataManager.getInstance().getConfiguration().getSearchHitsPerPage());
    }

    /**
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

    /**
     * @return the facets
     */
    public SearchFacets getFacets() {
        return facets;
    }

    /**
     * 
     * @return
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

    public long getTotalNumberOfVolumes() throws IndexUnreachableException, PresentationException {
        String query = "{!join from=PI_TOPSTRUCT to=PI}DOCTYPE:DOCSTRCT";
        return DataManager.getInstance().getSearchIndex().count(query);
    }

    /**
     * Returns the proper search URL part for the current search type.
     * 
     * @return
     * @should return correct url
     * @should return null if navigationHelper is null
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

    public int getLastPage() {
        if (currentSearch != null) {
            return currentSearch.getLastPage(hitsPerPage);
        }

        return 0;
    }

    public StructElement getStructElement(String pi) throws IndexUnreachableException, PresentationException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        StructElement struct = new StructElement(Long.parseLong(doc.getFirstValue(SolrConstants.IDDOC).toString()), doc);
        return struct;
    }
}
