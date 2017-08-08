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
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.ExpandParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
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
import de.intranda.digiverso.presentation.model.viewer.BrowseDcElement;
import de.intranda.digiverso.presentation.model.viewer.BrowsingMenuFieldConfig;
import de.intranda.digiverso.presentation.model.viewer.LabeledLink;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * SearchBean
 */
@ManagedBean(name = "searchBean")
@SessionScoped
public class SearchBean implements Serializable {

    private static final long serialVersionUID = 6962223613432267768L;

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SearchBean.class);

    public static final String URL_ENCODING = "UTF8";

    @ManagedProperty("#{navigationHelper}")
    private NavigationHelper navigationHelper;

    /** Max number of search hits to be displayed on one page. */
    private int hitsPerPage = DataManager.getInstance().getConfiguration().getSearchHitsPerPage();
    /** Currently selected search type (regular, advanced, timeline, ...). */
    private int activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
    /** Currently selected filter for the regular search. Possible values can be configured. */
    private SearchFilter currentSearchFilter = SearchHelper.SEARCH_FILTER_ALL;
    /** Solr query generated from the user's input (does not include facet filters or blacklists). */
    private String searchString = "";
    /** User-entered search query that is displayed in the search field after the search. */
    private String guiSearchString = "";
    /** Fully constructed Solr search query (includes facets but not blacklists). */
    private String currentQuery = null;
    /** Individual terms extracted from the user query (used for highlighting). */
    private Map<String, Set<String>> searchTerms = new HashMap<>();
    /** Current search result page. */
    private int currentPage = 1;
    /** Index of the currently open search result (used for search result browsing). */
    private int currentHitIndex = -1;
    private SearchFacets facets = new SearchFacets();
    /** User-selected Solr field name by which the search results shall be sorted. A leading exclamation mark means descending sorting. */
    private String sortString = "";
    /** Solr fields for seach result sorting (usually the field from sortString and some backup fields such as ORDER and FILENAME). */
    protected final List<StringPair> sortFields = new ArrayList<>();
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
     * Pretty-URL entry point.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public String newSearch() throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("newSearch");
        updateBreadcrumbsForSearchHits();

        // set the current page for the horizontal template navigation, therefore this determines the current-cat css class
        Object o = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("navigationHelper");
        if (o != null) {
            NavigationHelper nh = (NavigationHelper) o;
            nh.setCurrentPage("search");
        }

        return search();
    }

    /**
     * Action method for the "reset" button in search forms.
     * 
     * @return
     * @should return correct Pretty URL ID
     */
    public String resetSearchAction() {
        logger.trace("resetSearchAction");
        setSearchStringKeepCurrentPage("");
        setCurrentPage(1);
        setExactSearchString("");
        facets.resetCurrentCollection();
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
        facets.reset();

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
        setSearchStringKeepCurrentPage("");
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
     *
     * @return {@link String} null
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public String search() throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("search");
        resetSearchResults();
        currentQuery = generateQuery();
        // Only execute search if the query is not empty
        // TODO replace dirty hack with something more elegant
        if (!currentQuery.contains("()")) {
            executeSearch();
        }

        return "";
    }

    /**
     * Action method for search buttons (simple search).
     * 
     * @return
     */
    public String searchSimple() {
        logger.trace("searchSimple");
        resetSearchResults();
        resetSearchParameters();
        facets.resetCurrentFacetString();
        return "pretty:newSearch5";
    }

    /**
     * Same as <code>searchSimple()</code> but resets the current facets.
     * 
     * @return
     */
    public String searchSimpleResetCollections() {
        facets.resetCurrentCollection();
        return searchSimple();
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
     * Generates the final Solr query for all search types. The query includes facets/collections, blacklists and any user-specific restrictions.
     * 
     * @return
     * @throws IndexUnreachableException
     */
    private String generateQuery() throws IndexUnreachableException {
        logger.trace("generateQuery");
        StringBuilder sbQuery = new StringBuilder();

        // Add hierarchical facets
        if (!facets.getCurrentHierarchicalFacets().isEmpty()) {
            if (StringUtils.isNotEmpty(searchString)) {
                sbQuery.append('(').append(searchString).append(')');
            } else {
                // Collection browsing (no search query)
                sbQuery.append('(').append(SolrConstants.ISWORK).append(":true OR ").append(SolrConstants.ISANCHOR).append(":true)");
                sbQuery.append(SearchHelper.getDocstrctWhitelistFilterSuffix());
            }
            sbQuery.append(" AND (");
            int count = 0;
            for (FacetItem facetItem : facets.getCurrentHierarchicalFacets()) {
                if (count > 0) {
                    if (advancedSearchGroupOperator == 1) {
                        sbQuery.append(" OR ");
                    } else {
                        sbQuery.append(" AND ");
                    }
                }
                sbQuery.append('(').append(facetItem.getField()).append(':').append(facetItem.getValue()).append(" OR ").append(facetItem.getField())
                        .append(':').append(facetItem.getValue()).append(".*)");
                count++;
            }
            sbQuery.append(')');
        } else if (StringUtils.isNotEmpty(searchString)) {
            sbQuery.append(searchString);
        }

        // Add regular facets
        if (!facets.getCurrentFacets().isEmpty()) {
            if (sbQuery.length() > 0) {
                sbQuery.insert(0, '(');
                sbQuery.append(')');
            }
            for (FacetItem facetItem : facets.getCurrentFacets()) {
                if (sbQuery.length() > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append(facetItem.getQueryEscapedLink());
                logger.trace("Added facet: {}", facetItem.getLink());
            }
        }

        logger.trace("{}", sbQuery.toString());
        return sbQuery.toString();
    }

    public String searchAdvanced() {
        logger.trace("searchAdvanced");
        resetSearchResults();
        resetSearchParameters();
        searchString = generateAdvancedSearchString(DataManager.getInstance().getConfiguration().isAggregateHits());

        return "pretty:newSearch5";
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
                if (StringUtils.isNotEmpty(queryItem.getField()) && StringUtils.isNotBlank(queryItem.getValue())) {
                    if (!sbInfo.toString().endsWith("(")) {
                        sbInfo.append(' ').append(Helper.getTranslation("searchOperator_" + queryGroup.getOperator().name(), BeanUtils.getLocale()))
                                .append(' ');
                    }
                    // Generate the hierarchical facet parameter from query items
                    if (queryItem.isHierarchical()) {
                        logger.trace("{} is hierarchical", queryItem.getField());
                        sbCurrentCollection.append(queryItem.getField()).append(':').append(queryItem.getValue().trim()).append(";;").toString();
                        sbInfo.append(Helper.getTranslation(queryItem.getField(), BeanUtils.getLocale())).append(": \"").append(Helper.getTranslation(
                                queryItem.getValue(), BeanUtils.getLocale())).append('"');
                        continue;
                    }

                    // Non-hierarchical fields
                    if (searchTerms.get(SolrConstants.FULLTEXT) == null) {
                        searchTerms.put(SolrConstants.FULLTEXT, new HashSet<String>());
                    }
                    String itemQuery = queryItem.generateQuery(searchTerms.get(SolrConstants.FULLTEXT), aggregateHits);
                    // logger.trace("Item query: {}", itemQuery);
                    switch (queryItem.getOperator()) {
                        case IS:
                        case PHRASE:
                            sbInfo.append(Helper.getTranslation(queryItem.getField(), BeanUtils.getLocale())).append(": \"").append(queryItem
                                    .getValue()).append('"');
                            break;
                        default:
                            sbInfo.append(Helper.getTranslation(queryItem.getField(), BeanUtils.getLocale())).append(": ").append(queryItem
                                    .getValue());
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
            facets.setCurrentCollection(sbCurrentCollection.toString());
        } else {
            facets.setCurrentCollection("-");
        }

        // Add faceting
        if (!facets.getCurrentFacets().isEmpty()) {
            if (sb.length() > 0) {
                sb.insert(0, '(');
                sb.append(')');
            }
            for (FacetItem facetItem : facets.getCurrentFacets()) {
                if (!facetItem.isHierarchial()) {
                    if (sb.length() > 0) {
                        sb.append(" AND ");
                    }
                    sb.append(facetItem.getLink());
                    logger.debug("Added facet: {}", facetItem.getLink());
                }
            }
        }

        // Add discriminator subquery, if set and configured to be part of the visible query
        if (DataManager.getInstance().getConfiguration().isSubthemeFilterQueryVisible()) {
            try {
                String discriminatorValueSubQuery = SearchHelper.getDiscriminatorFieldFilterSuffix(navigationHelper, DataManager.getInstance()
                        .getConfiguration().getSubthemeDiscriminatorField());
                if (StringUtils.isNotEmpty(discriminatorValueSubQuery)) {
                    sb.insert(0, '(');
                    sb.append(')').append(discriminatorValueSubQuery);
                }
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
        }

        advancedSearchQueryInfo = sbInfo.toString();
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
        logger.debug("executeSearch; currentQuery: {}", currentQuery);
        mirrorAdvancedSearchCurrentHierarchicalFacets();
        if (StringUtils.isBlank(currentQuery)) {
            return;
        }

        currentSearch = new Search();

        if (StringUtils.isEmpty(sortString)) {
            setSortString(DataManager.getInstance().getConfiguration().getDefaultSortField());
            logger.trace("Using default sorting: {}", sortString);
        }

        // Collect regular and hierarchical facet field names and combine them
        // into one list
        List<String> hierarchicalFacetFields = DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields();
        List<String> allFacetFields = getAllFacetFields(hierarchicalFacetFields);

        Map<String, String> params = SearchHelper.generateQueryParams();
        List<StructElement> luceneElements = new ArrayList<>();
        QueryResponse resp = null;
        String query = SearchHelper.buildFinalQuery(currentQuery, DataManager.getInstance().getConfiguration().isAggregateHits());
        if (currentSearch.getHitsCount() == 0) {
            logger.trace("Final main query: {}", query);
            resp = DataManager.getInstance().getSearchIndex().search(query, 0, 0, null, allFacetFields, Collections.singletonList(
                    SolrConstants.IDDOC), params);
            if (resp != null && resp.getResults() != null) {
                currentSearch.setHitsCount(resp.getResults().getNumFound());
                logger.trace("Pre-grouping search hits: {}", currentSearch.getHitsCount());
                // Check for duplicate values in the GROUPFIELD facet and
                // substract the number from the total hits.
                for (FacetField facetField : resp.getFacetFields()) {
                    if (SolrConstants.GROUPFIELD.equals(facetField.getName())) {
                        for (Count count : facetField.getValues()) {
                            if (count.getCount() > 1) {
                                currentSearch.setHitsCount(currentSearch.getHitsCount() - (count.getCount() - 1));
                            }
                        }
                    }
                }
                logger.debug("Total search hits: {}", currentSearch.getHitsCount());
            }
        }
        // logger.debug("Hits count query END");
        if (currentSearch.getHitsCount() > 0 && resp != null) {
            // Facets
            for (FacetField facetField : resp.getFacetFields()) {
                if (SolrConstants.GROUPFIELD.equals(facetField.getName()) || facetField.getValues() == null) {
                    continue;
                }
                Map<String, Long> facetResult = new TreeMap<>();
                for (Count count : facetField.getValues()) {
                    if (StringUtils.isEmpty(count.getName())) {
                        logger.warn("Facet for {} has no name, skipping...", facetField.getName());
                        continue;
                    }
                    facetResult.put(count.getName(), count.getCount());
                }
                // Use non-FACET_ field names outside of the actual faceting
                // query
                String fieldName = SearchHelper.defacetifyField(facetField.getName());
                if (hierarchicalFacetFields.contains(fieldName)) {
                    facets.getAvailableHierarchicalFacets().put(fieldName, FacetItem.generateFilterLinkList(fieldName, facetResult, false));
                } else {
                    facets.getAvailableFacets().put(fieldName, FacetItem.generateFilterLinkList(fieldName, facetResult, false));
                }
            }

            if (currentPage > getLastPage()) {
                currentPage = getLastPage();
                logger.trace(" currentPage = getLastPage()");
            }

            // Hits for the current page
            int from = (currentPage - 1) * hitsPerPage;
            if (DataManager.getInstance().getConfiguration().isAggregateHits() && !searchTerms.isEmpty()) {
                // Add search hit aggregation parameters, if enabled
                String expandQuery = activeSearchType == 1 ? SearchHelper.generateAdvancedExpandQuery(advancedQueryGroups) : SearchHelper
                        .generateExpandQuery(SearchHelper.getExpandQueryFieldList(activeSearchType, currentSearchFilter, advancedQueryGroups),
                                searchTerms);
                logger.trace("Expand query: {}", expandQuery);
                if (StringUtils.isNotEmpty(expandQuery)) {
                    params.put(ExpandParams.EXPAND_Q, expandQuery);
                    params.put(ExpandParams.EXPAND, "true");
                    params.put(ExpandParams.EXPAND_FIELD, SolrConstants.PI_TOPSTRUCT);
                    params.put(ExpandParams.EXPAND_ROWS, String.valueOf(SolrSearchIndex.MAX_HITS));
                    params.put(ExpandParams.EXPAND_SORT, SolrConstants.ORDER + " asc");
                }
            }
            List<SearchHit> hits = DataManager.getInstance().getConfiguration().isAggregateHits() ? SearchHelper.searchWithAggregation(query, from,
                    hitsPerPage, sortFields, null, params, searchTerms, null, BeanUtils.getLocale()) : SearchHelper.searchWithFulltext(query, from,
                            hitsPerPage, sortFields, null, params, searchTerms, null, BeanUtils.getLocale());
            currentSearch.getHits().addAll(hits);
            // logger.debug("seList: " + seList );
            // logger.debug("Current page query END");
        }
        // logger.debug("Filling elementList END");
    }

    /**
     * @param hierarchicalFacetFields
     * @return
     */
    public List<String> getAllFacetFields(List<String> hierarchicalFacetFields) {
        List<String> facetFields = DataManager.getInstance().getConfiguration().getDrillDownFields();
        List<String> allFacetFields = new ArrayList<>(hierarchicalFacetFields.size() + facetFields.size());
        allFacetFields.addAll(hierarchicalFacetFields);
        allFacetFields.addAll(facetFields);
        allFacetFields = SearchHelper.facetifyList(allFacetFields);
        return allFacetFields;
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
        if (this.activeSearchType != activeSearchType) {
            switch (activeSearchType) {
                case 1:
                    if (DataManager.getInstance().getConfiguration().isAdvancedSearchEnabled()) {
                        this.activeSearchType = activeSearchType;
                    } else {
                        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
                    }
                    break;
                case 2:
                    if (DataManager.getInstance().getConfiguration().isTimelineSearchEnabled()) {
                        this.activeSearchType = activeSearchType;
                    } else {
                        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
                    }
                    break;
                case 3:
                    if (DataManager.getInstance().getConfiguration().isCalendarSearchEnabled()) {
                        this.activeSearchType = activeSearchType;
                    } else {
                        this.activeSearchType = SearchHelper.SEARCH_TYPE_REGULAR;
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
        List<String> result = SearchHelper.searchAutosuggestion(suggest, facets.getCurrentHierarchicalFacets(), facets.getCurrentFacets());
        Collections.sort(result);

        return result;
    }

    public boolean isSearchInDcFlag() {
        return !facets.getCurrentHierarchicalFacets().isEmpty();
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
        setSearchStringKeepCurrentPage(searchString);
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
        setSearchStringKeepCurrentPage(searchString);
    }

    /**
     * @param inSearchString the searchString to set
     */
    public void setSearchStringKeepCurrentPage(String inSearchString) {
        logger.trace("setSearchStringKeepCurrentPage: {}", inSearchString);
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

        inSearchString = inSearchString.trim();
        if (StringUtils.isNotEmpty(inSearchString)) {
            if ("*".equals(inSearchString)) {
                searchString = new StringBuilder("(").append(SolrConstants.ISWORK).append(":true OR ").append(SolrConstants.ISANCHOR).append(":true)")
                        .append(SearchHelper.getDocstrctWhitelistFilterSuffix()).toString();
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
                String[] toSearch = inSearchString.split("\"");
                StringBuilder sb = new StringBuilder();
                for (String phrase : toSearch) {
                    phrase = phrase.replace("\"", "");
                    if (phrase.length() > 0) {
                        if (currentSearchFilter == null || currentSearchFilter.equals(SearchHelper.SEARCH_FILTER_ALL)) {
                            if (DataManager.getInstance().getConfiguration().isAggregateHits()) {
                                sb.append(SolrConstants.SUPERDEFAULT).append(":(\"").append(phrase).append("\") OR ");
                                sb.append(SolrConstants.SUPERFULLTEXT).append(":(\"").append(phrase).append("\") OR ");
                            } else {
                                sb.append(SolrConstants.DEFAULT).append(":(\"").append(phrase).append("\") OR ");
                                sb.append(SolrConstants.FULLTEXT).append(":(\"").append(phrase).append("\") OR ");
                            }
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
                                            sb.append(SolrConstants.SUPERDEFAULT).append(":(\"").append(phrase).append("\")");
                                            break;
                                        case SolrConstants.FULLTEXT:
                                            sb.append(SolrConstants.SUPERFULLTEXT).append(":(\"").append(phrase).append("\")");
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
                        } else {
                            sbOuter.append(SolrConstants.DEFAULT).append(":(").append(sbInner.toString());
                            sbOuter.append(") OR ").append(SolrConstants.FULLTEXT).append(":(").append(sbInner.toString());
                        }
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
                                    sbOuter.append(SolrConstants.SUPERDEFAULT).append(":(").append(sbInner.toString()).append(')');
                                    break;
                                case SolrConstants.FULLTEXT:
                                    sbOuter.append(SolrConstants.SUPERFULLTEXT).append(":(").append(sbInner.toString()).append(')');
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
                    String discriminatorValueSubQuery = SearchHelper.getDiscriminatorFieldFilterSuffix(navigationHelper, DataManager.getInstance()
                            .getConfiguration().getSubthemeDiscriminatorField());
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
     * @should split value correctly
     */
    public void setSortString(String sortString) {
        if ("-".equals(sortString)) {
            this.sortString = "";
        } else {
            this.sortString = sortString;
        }
        sortFields.clear();
        if (StringUtils.isNotEmpty(this.sortString)) {
            String[] sortStringSplit = this.sortString.split(";");
            if (sortStringSplit.length > 0) {
                for (String field : sortStringSplit) {
                    sortFields.add(new StringPair(field.replace("!", ""), field.charAt(0) == '!' ? "desc" : "asc"));
                    logger.trace("Added sort field: {}", field);
                }
            }
        }
    }

    /**
     * @return the sortString
     */
    public String getSortString() {
        if (sortString.length() == 0) {
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
        logger.trace("mirrorAdvancedSearchCurrentHierarchicalFacets: {}", facets.getCurrentCollection());
        if (!facets.getCurrentHierarchicalFacets().isEmpty()) {
            if (!advancedQueryGroups.isEmpty()) {
                SearchQueryGroup queryGroup = advancedQueryGroups.get(0);
                if (!queryGroup.getQueryItems().isEmpty()) {
                    int index = 0;
                    for (FacetItem facetItem : facets.getCurrentHierarchicalFacets()) {
                        if (index < queryGroup.getQueryItems().size()) {
                            // Fill existing search query items
                            SearchQueryItem item = queryGroup.getQueryItems().get(index);
                            while (!item.isHierarchical() && StringUtils.isNotEmpty(item.getValue()) && index + 1 < queryGroup.getQueryItems()
                                    .size()) {
                                // Skip items that already have values
                                ++index;
                                item = queryGroup.getQueryItems().get(index);
                            }
                            item.setField(facetItem.getField());
                            item.setOperator(facetItem.isHierarchial() ? SearchItemOperator.IS : SearchItemOperator.AND);
                            item.setValue(facetItem.getValue());
                        } else {
                            // If no search field is set up for collection search, add new field containing the currently selected collection
                            SearchQueryItem item = new SearchQueryItem(BeanUtils.getLocale());
                            item.setField(facetItem.getField());
                            item.setOperator(facetItem.isHierarchial() ? SearchItemOperator.IS : SearchItemOperator.AND);
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
    public String removeHierarchicalFacetAction(String facetQuery) {
        return facets.removeHierarchicalFacetAction(facetQuery, "pretty:newSearch5");
    }

    /**
     * 
     * @param facetQuery
     * @return
     * @should remove facet correctly
     */
    public String removeFacetAction(String facetQuery) {
        return facets.removeFacetAction(facetQuery, "pretty:newSearch5");
    }

    /*
     * Paginator methods
     */

    public int getLastPage() {
        int answer = 0;
        if (currentSearch != null) {
            int hitsPerPageLocal = hitsPerPage;
            answer = new Double(Math.floor(currentSearch.getHitsCount() / hitsPerPageLocal)).intValue();
            if (currentSearch.getHitsCount() % hitsPerPageLocal != 0 || answer == 0) {
                answer++;
            }
        }

        return answer;
    }

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

    public String cmdScrollNext() throws DAOException {
        int oldCurrentPage = currentPage;
        if (currentPage < getLastPage()) {
            currentPage++;
            try {
                search();
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                currentPage = oldCurrentPage;
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                currentPage = oldCurrentPage;
            }

        }
        return "";
    }

    public String cmdScrollPrevious() throws DAOException {
        int oldCurrentPage = currentPage;
        if (currentPage > 1) {
            currentPage--;
            try {
                search();
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                currentPage = oldCurrentPage;
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                currentPage = oldCurrentPage;
            }

        }
        return "";
    }

    /**
     * @return the hitsCount
     */
    public long getHitsCount() {
        if (currentSearch != null) {
            return currentSearch.getHitsCount();
        }

        return 0;
    }

    /**
     * @param hitsCount the hitsCount to set
     */
    public void setHitsCount(long hitsCount) {
        currentSearch.setHitsCount(hitsCount);
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

    public void increaseCurrentHitIndex() {
        if (currentHitIndex < currentSearch.getHitsCount() - 1) {
            currentHitIndex++;
        }
    }

    public void decreaseCurrentHitIndex() {
        if (currentHitIndex > 0) {
            currentHitIndex--;
        }
    }

    /**
     * Returns the index of the currently displayed BrowseElement, if it is present in the search hit list.
     *
     * @param currentElementIddoc
     * @param currentImageNo
     * @return The index of the currently displayed BrowseElement in the search hit list; -1 if not present.
     */
    public void findCurrentHitIndex(String currentElementPi, int currentImageNo) {
        logger.trace("findCurrentHitIndex: {}/{}", currentElementPi, currentImageNo);
        currentHitIndex = 0;
        if (currentSearch != null && !currentSearch.getHits().isEmpty()) {
            for (SearchHit hit : currentSearch.getHits()) {
                BrowseElement el = hit.getBrowseElement();
                logger.trace("BrowseElement: {}/{}", el.getPi(), el.getImageNo());
                if (el.getPi().equals(currentElementPi) && el.getImageNo() == currentImageNo) {
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
        if (currentHitIndex > -1) {
            if (currentHitIndex < currentSearch.getHitsCount() - 1) {
                return SearchHelper.getBrowseElement(currentQuery, currentHitIndex + 1, sortFields, SearchHelper.generateQueryParams(), searchTerms,
                        BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits());
            }
            return SearchHelper.getBrowseElement(currentQuery, currentHitIndex, sortFields, SearchHelper.generateQueryParams(), searchTerms, BeanUtils
                    .getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits());
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
        if (currentHitIndex > -1) {
            if (currentHitIndex > 0) {
                return SearchHelper.getBrowseElement(currentQuery, currentHitIndex - 1, sortFields, SearchHelper.generateQueryParams(), searchTerms,
                        BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits());
            } else if (currentSearch.getHitsCount() > 0) {
                return SearchHelper.getBrowseElement(currentQuery, currentHitIndex, sortFields, SearchHelper.generateQueryParams(), searchTerms,
                        BeanUtils.getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits());
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

    public boolean isCollectionDrilldownEnabled() {
        return DataManager.getInstance().getConfiguration().isCollectionDrilldownEnabled();
    }

    public boolean isSortingEnabled() {
        return DataManager.getInstance().getConfiguration().isSortingEnabled();
    }

    /**
     * This is used for flipping search result pages (so that the breadcrumb always has the last visited result page as its URL).
     */
    public void updateBreadcrumbsForSearchHits() {
        if (!facets.getCurrentHierarchicalFacets().isEmpty()) {
            updateBreadcrumbsWithCurrentUrl(facets.getCurrentHierarchicalFacets().get(0).getValue().replace("*", ""),
                    NavigationHelper.WEIGHT_ACTIVE_COLLECTION);
        } else {
            updateBreadcrumbsWithCurrentUrl("searchHitNavigation", NavigationHelper.WEIGHT_SEARCH_RESULTS);
        }
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

    public String getCurrentQuery() {
        // logger.debug("getCurrentQuery: " + currentQuery);
        return currentQuery;

    }

    // temporary needed to set search string for calendar
    public void setCurrentQuery(String query) {
        currentQuery = query;
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
        return advancedQueryGroups.add(new SearchQueryGroup(BeanUtils.getLocale(), DataManager.getInstance().getConfiguration()
                .getAdvancedSearchDefaultItemNumber()));
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
    public List<StringPair> getAdvancedSearchSelectItems(String field, String language, boolean hierarchical) throws PresentationException,
            IndexUnreachableException {
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
        NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
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
        this.currentSearch = currentSearch;
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public String saveSearchAction() throws DAOException {
        if (StringUtils.isBlank(currentSearch.getName())) {
            Messages.error("nameRequired");
            return "";
        }
        currentSearch.setUserInput(guiSearchString);
        currentSearch.setQuery(searchString);
        currentSearch.setPage(currentPage);
        for (FacetItem facetItem : facets.getCurrentFacets()) {
            if (SolrConstants.DOCSTRCT.equals(facetItem.getField())) {
                currentSearch.setFilter(facetItem.getValue());
            }
        }
        if (!facets.getCurrentHierarchicalFacets().isEmpty()) {
            currentSearch.setCollection(facets.getCurrentCollection());
        }
        if (StringUtils.isNotEmpty(sortString)) {
            currentSearch.setSortField(sortString);
        }
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
        try {
            return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(NavigationHelper.URL_RSS).append("?q=")
                    .append(URLEncoder.encode(currentQuery, URL_ENCODING)).toString();
        } catch (UnsupportedEncodingException e) {
            logger.warn("Could not encode query '{}' for URL", currentQuery);
            return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append('/').append(NavigationHelper.URL_RSS).append("?q=")
                    .append(currentQuery).toString();
        }
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
    
    <span><div id="user_login_wrapp" class="cms-nav-tabs">
    <ul class="nav nav-tabs">
    <li class="active"><a href="#baende" data-toggle="tab">Übersicht über die Bände</a></li>
    <li class=""><a href="#a-e" data-toggle="tab">A-E</a></li>
    <li class=""><a href="#f-k" data-toggle="tab">F-K</a></li>
    <li class=""><a href="#l-p" data-toggle="tab">L-P</a></li>
    <li class=""><a href="#q-t" data-toggle="tab">Q-T</a></li>
    <li class=""><a href="#u-z" data-toggle="tab">U-Z</a></li>
    </ul>
    <div class="tab-content">
    <div id="baende" class="tab-pane active">
    <h3>Übersicht über die Bände der Sammlung</h3>
    <ul>
    <li><a href="/viewer/image/14779821_01/1/LOG_0003/">Band 1 (1857-1858)</a></li>
    <li><a href="/viewer/image/14779821_02/1/LOG_0003/">Band 2 (1859-1860)</a></li>
    <li><a href="/viewer/image/14779821_03/1/LOG_0003/">Band 3 (1860-1861)</a></li>
    <li><a href="/viewer/image/14779821_04/1/LOG_0003/">Band 4 (1861-1862)</a></li>
    <li><a href="/viewer/image/14779821_05/1/LOG_0003/">Band 5 (1862-1863)</a></li>
    <li><a href="/viewer/image/14779821_06/1/LOG_0003/">Band 6 (1863-1864)</a></li>
    <li><a href="/viewer/image/14779821_07/1/LOG_0003/">Band 7 (1864-1865)</a></li>
    <li><a href="/viewer/image/14779821_08/1/LOG_0003/">Band 8 (1865-1866)</a></li>
    <li><a href="/viewer/image/14779821_09/1/LOG_0003/">Band 9 (1866-1867)</a></li>
    <li><a href="/viewer/image/14779821_10/1/LOG_0003/">Band 10 (1867-1868)</a></li>
    <li><a href="/viewer/image/14779821_11/1/LOG_0003/">Band 11 (1869-1870)</a></li>
    <li><a href="/viewer/image/14779821_12/1/LOG_0003/">Band 12 (1871-1873)</a></li>
    <li><a href="/viewer/image/14779821_13/1/LOG_0003/">Band 13 (1873-1874)</a></li>
    <li><a href="/viewer/image/14779821_14/1/LOG_0003/">Band 14 (1875-1877)</a></li>
    <li><a href="/viewer/image/14779821_15/1/LOG_0003/">Band 15 (1878-1880)</a></li>
    <li><a href="/viewer/image/14779821_16/1/LOG_0003/">Band 16 (1871-1883)</a></li>
    </ul>
    </div>
    <div id="a-e" class="tab-pane">
    <h3>A-E</h3>
    <p><a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128251" target="_blank">Adelsborn (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Worbis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121396" target="_blank">Adelwitz (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Torgau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131216" target="_blank">Adendorf (Rheinprovinz - Regierungsbezirk Cöln - Kreis Rheinbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106484" target="_blank">Ahrensburg (Provinz Schleswig-Holstein - Regierungsbezirk Schleswig - Kreis Stormarn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109996" target="_blank">Ahrenthal (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Ahrweiler)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117523" target="_blank">Allner (Rheinprovinz - Regierungsbezirk Köln - Sieg-Kreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121194" target="_blank">Alt-Doebern (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123459" target="_blank">Altengottern (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Langensalza)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121530" target="_blank">Altenhausen (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106095" target="_blank">Altenhof (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Kempen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115190" target="_blank">Althaldensleben (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112214" target="_blank">Alt-Jessnitz (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Bitterfeld)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124838" target="_blank">Alt-Raudten (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Steinau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128958" target="_blank">Alt-Tomysl (Provinz Posen - Regierungsbezirk Posen - Kreis Buk)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117505" target="_blank">Alt-Warthau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Bunzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120900" target="_blank">Altwasser (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Waldenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107792" target="_blank">Amt Walbeck (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108712" target="_blank">Angern (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Wolmirstedt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119332" target="_blank">Antonin (Provinz Posen - Regierungsbezirk Posen - Kreis Adelnau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120842" target="_blank">Arenfels (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Neuwied)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104362" target="_blank">Armenruh (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Goldberg-Haynau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114422" target="_blank">Arnsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Görlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113283" target="_blank">Ascherode (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Nordhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121728" target="_blank">Assen (Provinz Westphalen - Regierungsbezirk Münster - Kreis Beckum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126873" target="_blank">Auerose (Provinz Pommern - Regierungsbezirk Stettin - Kreis Anklam)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114045" target="_blank">Augusthöhe (Provinz Posen - Regierungsbezirk Posen - Kreis Bomst)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102573" target="_blank">Auras (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Wohlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131159" target="_blank">Balken (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Geldern)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113741" target="_blank">Bangert (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Kreuznach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106962" target="_blank">Baranowitz (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Rybnik)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109614" target="_blank">Barby (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Calbe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110063" target="_blank">Bärsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Goldberg-Haynau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102499" target="_blank">Baruth (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Jüterbog-Luckenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119119" target="_blank">Basentin (Provinz Pommern - Regierungsbezirk Stettin - Kreis Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122699" target="_blank">Bauditten (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Mohrungen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128504" target="_blank">Bayer Naumburg (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Sangerhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110312" target="_blank">Bechau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Neisse-Grottkau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110689" target="_blank">Bedra (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119274" target="_blank">Beesdau (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105932" target="_blank">Behlendorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129308" target="_blank">Behle (Provinz Posen - Regierungsbezirk Bromberg - Kreis Czarnikau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124795" target="_blank">Bellschwitz (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Rosenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129173" target="_blank">Bemstedt (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Seekreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108672" target="_blank">Benkendorf (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Merseburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130834" target="_blank">Bentlage (Provinz Westphalen - Regierungsbezirk Münster - Kreis Steinfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110195" target="_blank">Bergerhausen (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bergheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128211" target="_blank">Berthelsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126375" target="_blank">Besswitz (Provinz Pommern - Regierungsbezirk Köslin - Kreis Schlawe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102652" target="_blank">Betzendorf (Apenburger Hof) (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Salzwedel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119036" target="_blank">Bialokosz (Provinz Posen - Regierungsbezirk Posen - Kreis Birnbaum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112578" target="_blank">Birkholz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Beeskow-Storkow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102297" target="_blank">Blankenfelde (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Teltow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127327" target="_blank">Blumberg (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Nieder-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128993" target="_blank">Blumberg (Provinz Pommern - Regierungsbezirk Stettin - Kreis Randow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116018" target="_blank">Blumberg (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104388" target="_blank">Bockum (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Meschede)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124819" target="_blank">Bödeken (Provinz Westphalen - Regierungsbezirk Minden - Kreis Buren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103070" target="_blank">Bodelschwingh (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Dortmund)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114474" target="_blank">Bodendorf (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113969" target="_blank">Bodenheim (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130799" target="_blank">Bogenau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121271" target="_blank">Boldewitz (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Rügen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113944" target="_blank">Bollheim (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126452" target="_blank">Bootz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124919" target="_blank">Borkau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108739" target="_blank">Borlinghausen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Warburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130422" target="_blank">Bornheim (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118267" target="_blank">Bornsdorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102252" target="_blank">Bornstädt (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129057" target="_blank">Bornzin (Provinz Pommern - Regierungsbezirk Köslin - Kreis Stolp)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121889" target="_blank">Boyadel (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Grünberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124441" target="_blank">Boytzenburg (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Templin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111589" target="_blank">Brauchitschdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lüben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106374" target="_blank">Bremenhain (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Rothenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108095" target="_blank">Briesen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Cottbus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112530" target="_blank">Briesen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109955" target="_blank">Briest (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Stendal)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114383" target="_blank">Britz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Teltow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116054" target="_blank">Britz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Teltow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126637" target="_blank">Brockau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110291" target="_blank">Brostowo (Provinz Posen - Regierungsbezirk Bromberg - Kreis Wirsitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102595" target="_blank">Brüninghausen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Dortmund)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117296" target="_blank">Brunn (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125018" target="_blank">Brunzelwaldau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Freystadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112134" target="_blank">Brynnek (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Tost-Gleiwitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121823" target="_blank">Buchelsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Grünberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114244" target="_blank">Buchwald (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hirschberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104953" target="_blank">Burg Argendorf (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Neuwied)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129231" target="_blank">Burgau (Rheinprovinz - Regierungsbezirk Aachen - Kreis Düren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109791" target="_blank">Burg Brumby (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Calbe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104245" target="_blank">Burg Eltz (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Mayen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112099" target="_blank">Burg Frentz (Rheinprovinz - Regierungsbezirk Aachen - Kreis Düren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107772" target="_blank">Burg Hemmerich (Rheinprovinz - Regierungsbezirk Köln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111862" target="_blank">Burg Kemnitz (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Bitterfeld)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104628" target="_blank">Burg Loersfeld (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bergheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105107" target="_blank">Burg Metternich (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102234" target="_blank">Burg Oerner (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113985" target="_blank">Burg Ranis (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Ziegenrück)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112552" target="_blank">Burg Rübenach (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Coblenz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117747" target="_blank">Burgscheidungen (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121111" target="_blank">Burg Steinfurt (Provinz Westphalen - Regierungsbezirk Münster - Kreis Steinfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110106" target="_blank">Burg Vlatten (Rheinprovinz - Regierungsbezirk Aachen - Kreis Schleiden)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114404" target="_blank">Burg Ziesar (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow I)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125513" target="_blank">Bürresheim (Rheinprovinz - Regierungsbezirk Koblenz - Kreis Mayen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112472" target="_blank">Buslar (Rheinprovinz - Regierungsbezirk Aachen - Kreis Erkelenz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112151" target="_blank">Cadinen (Provinz Preussen - Regierungsbezirk Danzig - Kreis Elbing)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130193" target="_blank">Calcum (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Düsseldorf)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125098" target="_blank">Cammer (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118202" target="_blank">Cappenberg (Provinz Westphalen - Regierungsbezirk Münster - Kreis Lüdinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102893" target="_blank">Carlsburg (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Greifswald)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110415" target="_blank">Carlsburg (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Sangerhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103175" target="_blank">Carolath (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Freystadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102208" target="_blank">Carow (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II (Genthin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103337" target="_blank">Cartlow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Demmin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104304" target="_blank">Carwesee (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121032" target="_blank">Carwinden (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104265" target="_blank">Carwitz (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Dramburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106056" target="_blank">Carzin (Provinz Pommern - Regierungsbezirk Köslin - Kreis Fürstenthum-Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127364" target="_blank">Carzitz (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Rügen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119193" target="_blank">Casel (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108051" target="_blank">Cawallen (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Trebnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121259" target="_blank">Charcice (Provinz Posen - Regierungsbezirk Posen - Kreis Birnbaum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123059" target="_blank">Charlottenhoff (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Landsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117815" target="_blank">Chemnitz (Kemnitz) (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127349" target="_blank">Chrost (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Cosel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130911" target="_blank">Chrzastowo (Provinz Posen - Regierungsbezirk Posen - Kreis Schrimm)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117589" target="_blank">Chutow (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Beuthen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120789" target="_blank">Clevenow (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Grimmen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130344" target="_blank">Collande (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Militsch-Trachenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104114" target="_blank">Commende zu Ramersdorf (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119892" target="_blank">Condehnen (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Fischhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113505" target="_blank">Conradsheim (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127448" target="_blank">Coseeger (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Fürstenthum-Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103215" target="_blank">Cöthen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ober-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127021" target="_blank">Cremzow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110174" target="_blank">Crüden (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Osterburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123378" target="_blank">Czerbienczin (Provinz Preussen - Regierungsbezirk Danzig - Kreis Stargardt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112631" target="_blank">Dahlwitz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Nieder-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111477" target="_blank">Dahme (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Wohlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121923" target="_blank">Dalkau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121945" target="_blank">Dambrau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130527" target="_blank">Dambritsch (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Neumarkt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123194" target="_blank">Damerow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119370" target="_blank">Dammer (Provinz Posen - Regierungsbezirk Posen - Kreis Meseritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118937" target="_blank">Darfeld (Provinz Westphalen - Regierungsbezirk Münster - Kreis Coesfeld)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116410" target="_blank">Das Marmorpalais (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Potsdam)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113929" target="_blank">Das Schloss in Bernstadt (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Oels)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107969" target="_blank">Dedelow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106749" target="_blank">Deichslau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Steinau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119535" target="_blank">Demerthin (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104468" target="_blank">Deutsch-Jaegel (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Strehlen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102275" target="_blank">Deutsch-Wartenberg (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Grünberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119677" target="_blank">Dieban (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Steinau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114013" target="_blank">Die Niederburg zu Gondorf (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Mayen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117164" target="_blank">Die Plattenburg (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122711" target="_blank">Die Schweppenburg (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Mayen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115803" target="_blank">Dieskau (Provinz Sachsen - Regierungsbezirk Merseburg - Saalkreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122828" target="_blank">Dietersdorf (Provinz Brandenburg - Regierungsbezirk Köslin - Kreis Dramburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119591" target="_blank">Die Vitzenburg (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130855" target="_blank">Dittersbach (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lüben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106902" target="_blank">Dittersbach (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Wohlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123598" target="_blank">Divitz (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Franzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118064" target="_blank">Döbernitz (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Delitzsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102315" target="_blank">Dobrau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126295" target="_blank">Dobrau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115243" target="_blank">Döbschütz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Görlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117310" target="_blank">Doelkau (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Merseburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119072" target="_blank">Doelzig (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Soldin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118088" target="_blank">Döllingen (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Liebenwerda)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124628" target="_blank">Dolzig (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Sorau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102910" target="_blank">Domanze (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Schweidnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102750" target="_blank">Dönhoffstädt (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Rastenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121841" target="_blank">Dönstedt (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104642" target="_blank">Draulitten (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120884" target="_blank">Drebkau (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122928" target="_blank">Drehna (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109778" target="_blank">Dreiborn (Rheinprovinz - Regierungsbezirk Aachen - Kreis Schleiden)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102418" target="_blank">Dretzel (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II (Genthin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127088" target="_blank">Drewen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124600" target="_blank">Dyck (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Grevenbroich)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107813" target="_blank">Echthausen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Arnsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110702" target="_blank">Eckersdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Namslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112397" target="_blank">Eckersdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Neurode)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108400" target="_blank">Ehrenstein (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Neuwied)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110606" target="_blank">Ehreshofen (Rheinprovinz - Regierungsbezirk Cöln - Kreis Wipperpürth)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106119" target="_blank">Eichholz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Liegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113265" target="_blank">Eicks (Rheinprovinz - Regierungsbezirk Aachen - Kreis Schleiden)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112175" target="_blank">Elsum (Rheinprovinz - Regierungsbezirk Aachen - Kreis Heinsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124993" target="_blank">Elvershagen (Provinz Pommern - Regierungsbezirk Stettin - Kreis Regenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114442" target="_blank">Emden (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113388" target="_blank">Emmaburg (Rheinprovinz - Regierungsbezirk Aachen - Kreis Eupen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104051" target="_blank">Endenich (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108428" target="_blank">Erdmannsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hirschberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104034" target="_blank">Eringerfeld (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Lippstadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124568" target="_blank">Erpernburg (Provinz Westphalen - Regierungsbezirk Minden - Kreis Buren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128716" target="_blank">Erprath (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Mors)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130065" target="_blank">Erxleben (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a></p>
    </div>
    <div id="f-k" class="tab-pane">
    <h3>F-K</h3>
    <p><a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106589" target="_blank">Fahrenstedt (Provinz Schleswig-Holstein - Regierungsbezirk Schleswig - Kreis Schleswig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117548" target="_blank">Falkenberg (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116139" target="_blank">Falkenberg (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122755" target="_blank">Falkenburg (Provinz Brandenburg - Regierungsbezirk Köslin - Kreis Dramburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102717" target="_blank">Falkenhagen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128577" target="_blank">Falkenhof (Provinz Westphalen - Regierungsbezirk Münster - Kreis Steinfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106162" target="_blank">Felchow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Angermünde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130973" target="_blank">Filehne (Provinz Posen - Regierungsbezirk Bromberg - Kreis Czarnikau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121214" target="_blank">Finkenstein (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Rosenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103318" target="_blank">Fischbach (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hirschberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109898" target="_blank">Flamersheim (Rheinprovinz - Regierungsbezirk Cöln - Kreis Rheinbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102477" target="_blank">Flechtingen (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Gardelegen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129197" target="_blank">Fredenwalde (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Templin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125054" target="_blank">Fredersdorf (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127288" target="_blank">Frehne (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117953" target="_blank">Freienstein (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117667" target="_blank">Fretzdorf (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102630" target="_blank">Friedersdorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114068" target="_blank">Friedersdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127476" target="_blank">Friedland (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122789" target="_blank">Friedrichsdorf (Provinz Brandenburg - Regierungsbezirk Köslin - Kreis Dramburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102371" target="_blank">Friedrichsfelde (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Nieder-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119935" target="_blank">Friedrichsstein (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Königsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102797" target="_blank">Friedrichstein (Provinz Preussen - Regierungsbezirk Königsberg - Landkreis Königsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109650" target="_blank">Fuchsberg (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Fjschhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130017" target="_blank">Fürstenberg (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Mors)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128483" target="_blank">Fürstenstein (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Waldenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121410" target="_blank">Gabel (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Guhrau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123290" target="_blank">Gäbersdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Striegau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117354" target="_blank">Gadow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107001" target="_blank">Gaffron (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Steinau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116178" target="_blank">Gallingen (Provinz Ostpreussen - Regierungsbezirk Königsberg - Kreis Friedland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105222" target="_blank">Gallowitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130151" target="_blank">Gartrop (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Duisburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115723" target="_blank">Garz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114183" target="_blank">Gay (Provinz Posen - Regierungsbezirk Posen - Kreis Samter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129017" target="_blank">Geiglitz (Provinz Pommern - Regierungsbezirk Stettin - Kreis Regenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104979" target="_blank">Geisseln (Provinz Preussen - Regierungsbezirk Köngisberg - Kreis Mohrungen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106441" target="_blank">Geist (Provinz Westphalen - Regierungsbezirk Münster - Kreis Münster)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131135" target="_blank">Gelting (Provinz Schleswig-Holstein - Kreis Hadersleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123165" target="_blank">Gemen (Provinz Westphalen - Regierungsbezirk Münster - Kreis Borken)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110270" target="_blank">Genslack (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Wehlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109758" target="_blank">Georgenburg (Provinz Preussen - Regierungsbezirk Gumbinnen - Kreis Insterburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120026" target="_blank">Georgenfelde (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Gerdauen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108853" target="_blank">Gerbstedt (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Seekreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121650" target="_blank">Giesdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Namslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104853" target="_blank">Giessmannsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Bunzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106942" target="_blank">Gleissen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Sternberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115829" target="_blank">Glumbowitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Wohlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130955" target="_blank">Gnadenthal (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Cleve)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123518" target="_blank">Goeritz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103383" target="_blank">Goerlsdorf (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Angermünde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130444" target="_blank">Goldensee ( - - )</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125290" target="_blank">Gollgowitz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129269" target="_blank">Golssen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125178" target="_blank">Goltzow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115511" target="_blank">Gora (Provinz Posen - Regierungsbezirk Posen - Kreis Pleschen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121175" target="_blank">Görlsdorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110219" target="_blank">Gorzechowko (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Strasburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115227" target="_blank">Gorzyn (Provinz Posen - Regierungsbezirk Posen - Kreis Birnbaum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103147" target="_blank">Goseck (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121357" target="_blank">Grabowo (Provinz Posen - Regierungsbezirk Bromberg - Kreis Wirsitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126333" target="_blank">Gräfendorf (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Jüterbog-Luckenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107935" target="_blank">Granitz (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Rügen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128750" target="_blank">Grassee (Provinz Pommern - Regierungsbezirk Stettin - Kreis Saatzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115161" target="_blank">Greiffenstein (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Löwenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123492" target="_blank">Grevenburg (Provinz Westphalen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103288" target="_blank">Griebenow (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Grimmen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125037" target="_blank">Grieben (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Stendal)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123144" target="_blank">Grocholin (Provinz Posen - Regierungsbezirk Bromberg - Kreis Schubin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105045" target="_blank">Gross Arnsdorf (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Mohrungen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117936" target="_blank">Gross Bartensleben (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113443" target="_blank">Grosse Burg Klein-Büllesheim (Rheinprovinz - Regierungsbezirk Cöln - Kreis Rheinbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106461" target="_blank">Gross-Ellguth (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Cosel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118222" target="_blank">Grossen Kreutz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110584" target="_blank">Gross-Glienicke (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106418" target="_blank">Gross-Grauden (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Cosel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126319" target="_blank">Groß-Münsterberg (Provinz Preussen - Regierungsbezirk Köngisberg - Kreis Mohrungen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110784" target="_blank">Gross-Peterwitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Trebnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124958" target="_blank">Gross Saalau (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Friedland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104484" target="_blank">Gross-Silber (Provinz Pommern - Regierungsbezirk Stettin - Kreis Saatzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104075" target="_blank">Gross-Stein (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Gross-Strehlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122944" target="_blank">Gross-Tinz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Nimptsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130583" target="_blank">Gross Ulbersdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Oels)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123009" target="_blank">Groß- und Klein-Behnitz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103066" target="_blank">Gross-Weckow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113409" target="_blank">Grubno (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Culm)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128891" target="_blank">Grünhoff (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Fischhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108755" target="_blank">Gross Schwansfeld (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Friedland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113670" target="_blank">Gudenau (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112079" target="_blank">Gulben (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Cottbus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110436" target="_blank">Gültz (Provinz Pommern - Regierungsbezirk Stettin - Kreis Demmin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119513" target="_blank">Günthersdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Grünberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103162" target="_blank">Gusow mit Platkow (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111678" target="_blank">Gütergotz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Teltow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114126" target="_blank">Gymnich (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130875" target="_blank">Haag (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Geldern)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130402" target="_blank">Hackpfüffel (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Sangerhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115498" target="_blank">Hainrode (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Worbis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112195" target="_blank">Hammer (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Züllichau-Schwiebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110131" target="_blank">Hansdorf (Provinz Preussen - Regierungsbezirk Danzig - Kreis Elbing)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104768" target="_blank">Hanseberg (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Königsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117972" target="_blank">Harbke (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108936" target="_blank">Hardehausen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Warburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126699" target="_blank">Harff (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bergheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124671" target="_blank">Harkerode (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106640" target="_blank">Harkotten (Provinz Westphalen - Regierungsbezirk Münster - Kreis Warendorf)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108815" target="_blank">Haselberg (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ober-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114165" target="_blank">Haus Boegge (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Hamm)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114518" target="_blank">Haus Carwe (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105145" target="_blank">Haus Diesdonk (Rheinprovinz - Regierungsbezirk Dusseldorf - Kreis Geldern)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105014" target="_blank">Haus Horst (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Gladbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119230" target="_blank">Haus Kropstädt (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Wittenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112357" target="_blank">Haus Linzenich (Rheinprovinz - Regierungsbezirk Aachen - Kreis Juelich)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113589" target="_blank">Haus Neuhaus (Rheinprovinz - Regierungsbezirk Aachen - Kreis Eupen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111967" target="_blank">Haus Öfte (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Elberfeld)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115884" target="_blank">Haus Wehnde (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Worbis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123735" target="_blank">Haus Würdenburg (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Seekreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121963" target="_blank">Haus Zeitz (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Seekreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106985" target="_blank">Havixbeck (Provinz Westphalen - Regierungsbezirk Münster - Kreis Münster)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123339" target="_blank">Heeren (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Hamm)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126434" target="_blank">Heessen (Provinz Westphalen - Regierungsbezirk Münster - Kreis Beckum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113427" target="_blank">Heidehaus (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111511" target="_blank">Heiligenhoven (Rheinprovinz - Regierungsbezirk Cöln - Kreis Wipperfurth)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129366" target="_blank">Heimerzheim (Rheinprovinz - Regierungsbezirk Cöln - Kreis Rheinbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128554" target="_blank">Heinersdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Liegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122884" target="_blank">Heinrichsdorf (Provinz Pommern - Regierungsbezirk Köslin - Kreis Neu-stettin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106540" target="_blank">Helmern (Provinz Westphalen - Regierungsbezirk Minden - Kreis Warburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123312" target="_blank">Helmsdorf (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Seekreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123254" target="_blank">Heltorf (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Düsseldorf)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111555" target="_blank">Hemmersbach (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bergheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121807" target="_blank">Hemsendorf (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Schweinitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117764" target="_blank">Herdringen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Arnsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120048" target="_blank">Herten (Provinz Westphalen - Regierungsbezirk Münster - Kreis Recklinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123694" target="_blank">Hinnenburg (Provinz Westphalen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127105" target="_blank">Hirschfeldau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Sagan)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105990" target="_blank">Hirschfelde (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ober-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128467" target="_blank">Hohenberg (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Osterburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116111" target="_blank">Hohenbocka (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hoyerswerda)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126657" target="_blank">Hohendorff (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Franzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131051" target="_blank">Hohendorf (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114222" target="_blank">Hohenhausen (Provinz Posen - Regierungsbezirk Bromberg - Kreis Bromberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102870" target="_blank">Hohen-Jesar (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129077" target="_blank">Hohen-Kamern (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121054" target="_blank">Hohen-Landin (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Angermünde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128408" target="_blank">Hohenliebenthal (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126536" target="_blank">Hohennauen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107990" target="_blank">Hohenpriessnitz (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Delitzsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112410" target="_blank">Hohensolms (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Wetzlar)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125357" target="_blank">Hohenwalde (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Landsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123752" target="_blank">Hohen-Zieten (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Soldin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123415" target="_blank">Holthausen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Büren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125636" target="_blank">Holzhausen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126830" target="_blank">Holzhausen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Minden)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122657" target="_blank">Holzkirch (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117375" target="_blank">Hoppenrade (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106708" target="_blank">Horno (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Spremberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123611" target="_blank">Hovestadt (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Soest)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105975" target="_blank">Hülhoven (Rheinprovinz - Regierungsbezirk Aachen - Kreis Heinsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104443" target="_blank">Hülshoff (Provinz Westphalen - Regierungsbezirk Münster - Kreis Münster)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114308" target="_blank">Hundisburg (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115392" target="_blank">Isterbies (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow I)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108831" target="_blank">Ittlingen (Provinz Westphalen - Regierungsbezirk Münster - Kreis Lüdinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121761" target="_blank">Jablonken (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Ortelsburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111539" target="_blank">Jacobsdorf (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125492" target="_blank">Jagow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115479" target="_blank">Jahmen (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Rothenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106804" target="_blank">Jakobsdorf (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Kreuzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116072" target="_blank">Jankendorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Rothenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110518" target="_blank">Jannewitz (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Lauenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116202" target="_blank">Jannowitz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120083" target="_blank">Janow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Anklam)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119178" target="_blank">Jarocin (Provinz Posen - Regierungsbezirk Posen - Kreis Pleschen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110254" target="_blank">Jerskewitz (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Stolp)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113829" target="_blank">Jeschkendorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Liegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106568" target="_blank">Jordansmühl (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Nimptsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104523" target="_blank">Kahren (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Cottbus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130213" target="_blank">Kalbe (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Salzwedel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112337" target="_blank">Kaltwasser (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lüben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123396" target="_blank">Kamienietz (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Tost-Gleiwitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108915" target="_blank">Kannenberg (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Osterburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107043" target="_blank">Karbowo (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Strasburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128819" target="_blank">Kartzow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129324" target="_blank">Kelbra (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Sangerhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127403" target="_blank">Kellenberg (Rheinprovinz - Regierungsbezirk Aachen - Kreis Jülich)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126755" target="_blank">Kendenich (Rheinprovinz - Regierungsbezirk Cöln - Kreis Cöln)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104836" target="_blank">Kerschitten (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124891" target="_blank">Kerstin (Provinz Pommern - Regierungsbezirk Köslin - Kreis Fürstenthum-Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108778" target="_blank">Ketschdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115664" target="_blank">Kieslingswalde (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Görlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109839" target="_blank">Kilgis (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Eylau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118977" target="_blank">Kittelau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Nimptsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112599" target="_blank">Kittlitztreben (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Bunzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113840" target="_blank">Kitzburg (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114288" target="_blank">Kleeburg (Rheinprovinz - Regierungsbezirk Cöln - Kreis Rheinbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118999" target="_blank">Klein Katz (Provinz Preussen - Regierungsbezirk Danzig - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112454" target="_blank">Klein Pramsen (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115418" target="_blank">Klein-Rosen (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Striegau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117333" target="_blank">Klein-Tschirnau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111985" target="_blank">Klein-Werther (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Nordhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128795" target="_blank">Klein-Wzunkowe (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Militsch-Trachenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130306" target="_blank">Klieschau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Steinau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129037" target="_blank">Klingewalde (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Görlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122984" target="_blank">Klitschdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Bunzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120069" target="_blank">Kloetzen (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Marienwerder)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130488" target="_blank">Kloster Haeseler (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Eckartsberga)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112271" target="_blank">Kloster Haeseler (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Eckartsberga)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105203" target="_blank">Kloxin (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115867" target="_blank">Kniegnitz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lüben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106523" target="_blank">Knoop (Provinz Schleswig-Holstein - Regierungsbezirk Schleswig - Kreis Schleswig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114368" target="_blank">Koenigsborn (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow I)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102732" target="_blank">Königliches Schloss zu Brühl (Rheinprovinz - Regierungsbezirk Cöln - Landkreis Cöln)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128447" target="_blank">Königshain (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Görlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111418" target="_blank">Königswalde (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Sternberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106188" target="_blank">Koppen (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Brieg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119212" target="_blank">Körtlinghausen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Lippstadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130462" target="_blank">Kostau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Kreuzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123431" target="_blank">Kotzen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121741" target="_blank">Krassenstein (Provinz Westphalen - Regierungsbezirk Münster - Kreis Beckum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126891" target="_blank">Kratzkau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Schweidnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127183" target="_blank">Krausendorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Landeshut)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115432" target="_blank">Krenzlin (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110566" target="_blank">Kreppelhof (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Landeshut)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123579" target="_blank">Kriegsdorf (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Merseburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125583" target="_blank">Kriegshoven (Rheinprovinz - Regierungsbezirk Cöln - Kreis Rheinbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115707" target="_blank">Krischa (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Görlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111789" target="_blank">Krischow (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Cottbus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126914" target="_blank">Kröchlendorff (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Templin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121290" target="_blank">Krockow (Provinz Preussen - Regierungsbezirk Danzig - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118041" target="_blank">Krosigk (Provinz Sachsen - Regierungsbezirk Merseburg - Saalkreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112438" target="_blank">Krossen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115540" target="_blank">Kuchelberg (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Liegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115589" target="_blank">Kühlseggen (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107088" target="_blank">Kuttlau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104684" target="_blank">Kutzerow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111498" target="_blank">Kützkow (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II (Genthin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129111" target="_blank">Kynast und Warmbrunn (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hirschberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129139" target="_blank">Kynast und Warmbrunn (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hirschberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128684" target="_blank">Kynau mit der Kynsburg (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Waldenburg)</a></p>
    </div>
    <div id="l-p" class="tab-pane">
    <h3>L-P</h3>
    <p><a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117853" target="_blank">Laasan (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Striegau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111743" target="_blank">Laasow (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128919" target="_blank">Labehn (Provinz Pommern - Regierungsbezirk Köslin - Kreis Stolp)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125475" target="_blank">Lablacken (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Labiau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104929" target="_blank">Lähnhaus (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Löwenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107830" target="_blank">Lampersdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Frankenstein)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119755" target="_blank">Landsberg (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Düsseldorf)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106243" target="_blank">Langenau (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Rosenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117914" target="_blank">Langenbielau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Reichenbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106884" target="_blank">Langenbrück (Provinz Westphalen - Regierungsbezirk Münster - Kreis Tecklenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106684" target="_blank">Langendorf (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Tost-Gleiwitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117121" target="_blank">Langheim (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Rastenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106761" target="_blank">Lang-Heinersdorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Züllichau-Schwiebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130939" target="_blank">Lankau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Namslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120993" target="_blank">Lanke (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Nieder-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117464" target="_blank">Laskowitz (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Schwetz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129994" target="_blank">Laskowitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Ohlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107738" target="_blank">Laskowitz (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Schwetz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123714" target="_blank">Lauck (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104902" target="_blank">Lembeck (Provinz Westphalen - Regierungsbezirk Münster - Kreis Recklinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119953" target="_blank">Leuthen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lübben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121605" target="_blank">Lewitz (Provinz Posen - Regierungsbezirk Posen - Kreis Meseritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102967" target="_blank">Libbnow (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Greifswald)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111929" target="_blank">Liebenow (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Sternberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120928" target="_blank">Lieberose (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lübben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120004" target="_blank">Liebesitz (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Guben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109597" target="_blank">Liebstein (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Görlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130110" target="_blank">Lienichen (Provinz Pommern - Regierungsbezirk Stettin - Kreis Saatzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106664" target="_blank">Lindau (Provinz Schleswig-Holstein - Regierungsbezirk Schleswig - Kreis Eckernförde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116230" target="_blank">Lipsa (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hoyerswerda)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124872" target="_blank">Löbnitz (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Delitzsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125194" target="_blank">Lohe (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126576" target="_blank">Lohe (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Soest)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116273" target="_blank">Lohm (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119290" target="_blank">Lomnitz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hirschberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125317" target="_blank">Lontzen (Rheinprovinz - Regierungsbezirk Aachen - Kreis Eupen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128875" target="_blank">Lorzendorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Neumarkt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123238" target="_blank">Loslau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Rybnik)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106016" target="_blank">Löwen (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Brieg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117083" target="_blank">Lübbenau (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128538" target="_blank">Lübbenow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124504" target="_blank">Lübchow (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Fürstenthum-Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110375" target="_blank">Lübtow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115848" target="_blank">Lüchfeld (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115994" target="_blank">Lübchen (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Guhrau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128388" target="_blank">Lüftelberg (Rheinprovinz - Regierungsbezirk Cöln - Kreis Rheinbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113542" target="_blank">Luschwitz (Provinz Posen - Regierungsbezirk Posen - Kreis Fraustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102842" target="_blank">Madlitz (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111803" target="_blank">Maiwaldau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130757" target="_blank">Maldeuten (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Mohrungen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115359" target="_blank">Malinie (Provinz Posen - Regierungsbezirk Posen - Kreis Pleschen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104135" target="_blank">Malitsch (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Jauer)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121868" target="_blank">Mallenchen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108638" target="_blank">Mallinkrodt (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Hagen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124750" target="_blank">Margoninsdorf (Provinz Posen - Regierungsbezirk Bromberg - Kreis Chodziesen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123655" target="_blank">Marienborn (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102946" target="_blank">Markendorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128731" target="_blank">Markowitz (Provinz Posen - Regierungsbezirk Bromberg - Kreis Inowraclaw)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130718" target="_blank">Markowo (Provinz Posen - Regierungsbezirk Bromberg - Kreis Inowraclaw)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110233" target="_blank">Maxdorf (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Calbe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128833" target="_blank">Maygadessen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114493" target="_blank">Meffersdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104708" target="_blank">Megow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104428" target="_blank">Mehrenthin (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Friedeberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128973" target="_blank">Mehrum (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Duisburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127267" target="_blank">Mellenthin (Provinz Pommern - Regierungsbezirk Stettin - Kreis Usedom-Wollin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126515" target="_blank">Melno (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Graudenz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111614" target="_blank">Menzlin (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Greifswald)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118000" target="_blank">Merbitz (Provinz Sachsen - Regierungsbezirk Merseburg - Saalkreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125396" target="_blank">Merode (Rheinprovinz - Regierungsbezirk Aachen - Kreis Eupen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115374" target="_blank">Merödgen (Röthgen) (Rheinprovinz - Regierungsbezirk Aachen - Kreis Aachen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108874" target="_blank">Mertensdorf (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Friedland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115568" target="_blank">Meseberg (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125379" target="_blank">Mettkau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Neumarkt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104322" target="_blank">Mickrow (Provinz Pommern - Regierungsbezirk Köslin - Kreis Stolp)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105069" target="_blank">Miechowitz (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Beuthen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130047" target="_blank">Mittel-Herwigsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Freystadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127140" target="_blank">Möckern (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow I)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108388" target="_blank">Mockrehna (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Torgau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105183" target="_blank">Moisdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Jauer)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124776" target="_blank">Moldau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Bunzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128938" target="_blank">Mönau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hoyerswerda)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121578" target="_blank">Morsbroich (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Solingen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110334" target="_blank">Mückenberg (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Liebenwerda)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108893" target="_blank">Müddersheim (Rheinprovinz - Regierungsbezirk Aachen - Kreis Düren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106138" target="_blank">Muffendorf (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104602" target="_blank">Mühlrädlitz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lüben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111901" target="_blank">Muhrau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Striegau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112111" target="_blank">Muldenstein (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Bitterfeld)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104741" target="_blank">Murchin (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Greifswald)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123098" target="_blank">Muskau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Rothenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126936" target="_blank">Myllendonk (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Gladbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106227" target="_blank">Nassadel (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Kreuzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126593" target="_blank">Nassenheide (Provinz Pommern - Regierungsbezirk Stettin - Kreis Randow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119494" target="_blank">Neetzow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Anklam)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122679" target="_blank">Nemitz (Provinz Pommern - Regierungsbezirk Köslin - Kreis Schlawe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119138" target="_blank">Nennhausen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130365" target="_blank">Neudeck (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Schweinitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108114" target="_blank">Neudeck (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Rosenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112259" target="_blank">Neu-Döbern (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103203" target="_blank">Neudörfchen (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Marienwerder)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126953" target="_blank">Neugattersleben (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Calbe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102829" target="_blank">Neuhardenberg (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109672" target="_blank">Neukirchen (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Osterburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120946" target="_blank">Neustadt (Provinz Preussen - Regierungsbezirk Danzig - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119354" target="_blank">Nieder-Adelsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Goldberg-Haynau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110493" target="_blank">Niedergebra (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Nordhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119256" target="_blank">Nieder-Heidersdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122593" target="_blank">Nieder-Kunzendorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Schweidnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128270" target="_blank">Nieder-Lichtenau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106076" target="_blank">Nieder-Neuendorf (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110644" target="_blank">Nieder-Peilau Schlössel (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Reichenbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107710" target="_blank">Nieder-Schönhausen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Nieder-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119637" target="_blank">Nieder-Schüttlau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Guhrau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130133" target="_blank">Niedertopfstedt (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Weissensee)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113523" target="_blank">Niewodnik (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118288" target="_blank">Nordkirchen (Provinz Westphalen - Regierungsbezirk Münster - Kreis Lüdinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113362" target="_blank">Norock (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104344" target="_blank">Nothwendig (Provinz Posen - Regierungsbezirk Bromberg - Kreis Czarnikau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113657" target="_blank">Ober-Frankleben (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Merseburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115315" target="_blank">Ober-Gebelzig (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Rothenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130732" target="_blank">Ober-Herrndorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107898" target="_blank">Ober-Lichtenau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104789" target="_blank">Obernigk (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Trebnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126984" target="_blank">Ober-Schüttlau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Guhrau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105088" target="_blank">Ober-Stentsch (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Züllichau-Schwiebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126778" target="_blank">Ober-Stephansdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Neumarkt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129285" target="_blank">Ober-Weistritz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Schweidnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112054" target="_blank">Ober-Wiederstedt (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113901" target="_blank">Oels (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Oels)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111829" target="_blank">Ogrosen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122864" target="_blank">Oppin (Provinz Sachsen - Regierungsbezirk Merseburg - Saalkreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128322" target="_blank">Ornshagen (Provinz Pommern - Regierungsbezirk Stettin - Kreis Regenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112616" target="_blank">Ossenberg (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Mörs)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104095" target="_blank">Ostramondra (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Eckartsberga)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111765" target="_blank">Ostrau (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Bitterfeld)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125111" target="_blank">Ostrawe (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Wohlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116259" target="_blank">Ostrichen (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106359" target="_blank">Ostrometzko (Provinz Preussen - Regierungbezirk Marienwerder - Kreis Kulm)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128360" target="_blank">Oswitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102445" target="_blank">Ottmachau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Grottkau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128174" target="_blank">Ovelgünne (Provinz Westphalen - Regierungsbezirk Minden - Kreis Minden)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126354" target="_blank">Overbach (Rheinprovinz - Regierungsbezirk Aachen - Kreis Jülich)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121472" target="_blank">Overhagen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Lippstadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119451" target="_blank">Owinsk (Provinz Posen - Regierungsbezirk Posen - Kreis Posen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104159" target="_blank">Paffendorf (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bergheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123352" target="_blank">Palzig (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Züllichau-Schwiebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127422" target="_blank">Pammin (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Arnswalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117413" target="_blank">Panthenau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Goldberg-Haynau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123474" target="_blank">Pantzkau (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Striegau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117789" target="_blank">Paretz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ost-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102562" target="_blank">Parey (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II (Genthin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104171" target="_blank">Partheinen (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Heiligenbeil)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125072" target="_blank">Pasterwitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119015" target="_blank">Patthorst (Provinz Westphalen - Regierungsbezirk Minden - Kreis Halle)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104662" target="_blank">Peilau-Gladishof (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Reichenbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119795" target="_blank">Penkun (Provinz Pommern - Regierungsbezirk Stettin - Kreis Randow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108340" target="_blank">Peppenhoven (Rheinprovinz - Regierungsbezirk Cöln - Kreis Rheinbach)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125454" target="_blank">Pesch (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Krepeld)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105165" target="_blank">Petershagen (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109738" target="_blank">Petzkendorf (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104724" target="_blank">Petznick (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Templin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125549" target="_blank">Pfaffendorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Landeshut)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123272" target="_blank">Pfoerten (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Sorau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107758" target="_blank">Pietrunke (Provinz Posen - Regierungsbezirk Bromberg - Kreis Chodziesen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127064" target="_blank">Pilgramsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Goldberg-Haynau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125332" target="_blank">Pilgramshain (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Striegau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131198" target="_blank">Pilsnitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121513" target="_blank">Pinne (Provinz Posen - Regierungsbezirk Posen - Kreis Samter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119879" target="_blank">Pirschen (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Trebnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112235" target="_blank">Pitschen (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Striegau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127492" target="_blank">Pläswitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Striegau)</a>   <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117831" target="_blank">Plaue (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113865" target="_blank">Plauen (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Wehlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117141" target="_blank">Pleischwitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117723" target="_blank">Plessow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121784" target="_blank">Plüggentin (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Rügen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117257" target="_blank">Podangen (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106506" target="_blank">Pohlschildern (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Liegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116397" target="_blank">Polnisch-Tschammendorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Strehlen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119397" target="_blank">Polssen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Angermünde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117392" target="_blank">Ponarien (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Mohrungen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118021" target="_blank">Poplitz (Provinz Sachsen - Regierungsbezirk Merseburg - Saalkreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114106" target="_blank">Posadowo (Provinz Posen - Regierungsbezirk Posen - Kreis Buk)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113803" target="_blank">Potulice (Provinz Posen - Regierungsbezirk Bromberg - Kreis Bromberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108516" target="_blank">Pouch (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Bitterfeld)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125230" target="_blank">Premslaff (Provinz Pommern - Regierungsbezirk Stettin - Kreis Regenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108283" target="_blank">Priemern (Provinz Brandenburg - Regierungsbezirk Magdeburg - Kreis Osterburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117647" target="_blank">Primkenau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Sprottau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102708" target="_blank">Probstei Salzwedel (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Salzwedel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108471" target="_blank">Prötzel (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ober-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128630" target="_blank">Puschine (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104565" target="_blank">Pustow (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Grimmen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118106" target="_blank">Putbus (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Rügen)</a></p>
    </div>
    <div id="q-t" class="tab-pane">
    <h3>Q-T</h3>
    <p><a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129344" target="_blank">Quakenburg (Provinz Pommern - Regierungsbezirk Köslin - Kreis Rummelsburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119571" target="_blank">Quaritz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Gross-Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119830" target="_blank">Quatzow (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Schlawe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123817" target="_blank">Quittainen (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108011" target="_blank">Raackow (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130622" target="_blank">Rabenstein (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121552" target="_blank">Rackschütz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Neumarkt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111946" target="_blank">Radis (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Wittenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119616" target="_blank">Radojewo (Provinz Posen - Regierungsbezirk Posen - Kreis Posen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125410" target="_blank">Ralswiek (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Rügen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114202" target="_blank">Ramersdorf (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103001" target="_blank">Rammelburg (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102466" target="_blank">Ramstedt (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Wolmirstedt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127126" target="_blank">Rath (Rheinprovinz - Regierungsbezirk Aachen - Kreis Düren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102862" target="_blank">Rauden (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Rybnik)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115649" target="_blank">Rauschendorf (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122900" target="_blank">Rautenburg (Provinz Preussen - Regierungsbezirk Gumbinnen - Kreis Niederung)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117707" target="_blank">Reckahn (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118951" target="_blank">Reddentin (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Schlawe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102227" target="_blank">Redekin (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II (Genthin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104400" target="_blank">Redel (Provinz Pommern - Regierungsbezirk Köslin - Kreis Belgard)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123071" target="_blank">Reinfeld (Provinz Pommern - Regierungsbezirk Köslin - Kreis Belgard)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118917" target="_blank">Reisen (Provinz Posen - Regierungsbezirk Posen - Kreis Fraustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126559" target="_blank">Reisicht (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Goldberg-Haynau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102684" target="_blank">Reitwein (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127201" target="_blank">Reitzenstein (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Sternberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109971" target="_blank">Rengerslage (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Osterburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112493" target="_blank">Reuden (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117449" target="_blank">Rheder (Provinz Westphälen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108304" target="_blank">Rheineck (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Ahrweiler)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104871" target="_blank">Rheinsberg (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108691" target="_blank">Rheinstein (Rheinprovinz - Regierungsbezirk Coblenz - Kreis St. Goar)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108595" target="_blank">Ribbeck (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131171" target="_blank">Ribbekardt (Provinz Pommern - Regierungsbezirk Stettin - Kreis Greifenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120828" target="_blank">Ringenwalde (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Templin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109718" target="_blank">Rittershain (Provinz Hessen - Regierungsbezirk Kassel - Kreis Rothenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106724" target="_blank">Rochholz (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Hagen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113601" target="_blank">Rodeland (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123126" target="_blank">Roederhof (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Aschersleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107021" target="_blank">Rogaesen (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124857" target="_blank">Rohrbeck (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Königsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121438" target="_blank">Rohr (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Rummelsburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108619" target="_blank">Roitzsch (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Torgau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119559" target="_blank">Rokossowo (Provinz Posen - Regierungsbezirk Posen - Kreis Kroeben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131075" target="_blank">Rollwitz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105951" target="_blank">Rombczyn (Provinz Posen - Regierungsbezirk Bromberg - Kreis Wongrowitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130688" target="_blank">Rösberg (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115457" target="_blank">Rosbitek (Provinz Posen - Regierungsbezirk Posen - Kreis Birnbaum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105917" target="_blank">Rosenthal (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Schweidnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117561" target="_blank">Roskow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111842" target="_blank">Rossoszyca (Provinz Posen - Regierungsbezirk Posen - Kreis Adelnau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114327" target="_blank">Rostin (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Soldin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127161" target="_blank">Rothenhoff (Provinz Westphalen - Regierungsbezirk Minden - Kreis Minden)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117068" target="_blank">Rothkirch (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Liegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130543" target="_blank">Rothmannshagen (Provinz Pommern - Regierungsbezirk Stettin - Kreis Demmin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127249" target="_blank">Rudelsdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Polnisch Wartenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121454" target="_blank">Rudelstadt (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Bolkenhain)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117483" target="_blank">Rühstädt (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111652" target="_blank">Ruine Bolzenschloss (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124588" target="_blank">Runstedt (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Merseburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105244" target="_blank">Rurich (Rheinprovinz - Regierungsbezirk Aachen - Kreis Erkelenz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102400" target="_blank">Rutzau (Provinz Preussen - Regierungsbezirk Danzig - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103021" target="_blank">Saabor (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Grünberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117601" target="_blank">Sagan (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Sagan)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113628" target="_blank">Samter (Provinz Posen - Regierungsbezirk Posen - Kreis Samter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104815" target="_blank">Sandfort (Provinz Westphalen - Regierungsbezirk Münster - Kreis Lüdinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119773" target="_blank">Sanditten (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Wehlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116099" target="_blank">Sandow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117207" target="_blank">Satzfey (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106331" target="_blank">Schaffhausen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Soest)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119096" target="_blank">Schedlau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124483" target="_blank">Scheibau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Freystadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111437" target="_blank">Schellenberg (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Duisburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104011" target="_blank">Schildau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119657" target="_blank">Schilfa (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Weissensee)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123790" target="_blank">Schkopau (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Merseburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119433" target="_blank">Schlagenthin (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Arnswalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119911" target="_blank">Schlanz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120100" target="_blank">Schlemmin (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Franzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131010" target="_blank">Schlenderhan (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bergheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126852" target="_blank">Schlesisch-Halbau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Sagan)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103246" target="_blank">Schlobitten (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117684" target="_blank">Schlodien (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Holland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108499" target="_blank">Schloss Alme (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Brilon)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128652" target="_blank">Schloss Arendsee (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106268" target="_blank">Schloss Arff (Rheinprovinz - Regierungsbezirk Cöln - Landkreis Cöln)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119853" target="_blank">Schloss Beichlingen (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Eckartsberga)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114343" target="_blank">Schloss Berlepsch (Provinz Hessen-Nassau - Regierungsbezirk Kassel - Kreis Witzenhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130326" target="_blank">Schloss Borowko (Provinz Posen - Regierungsbezirk Posen - Kreis Kosten)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109567" target="_blank">Schloss Braunfels (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Wetzlar)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110728" target="_blank">Schloss Breill (Rheinprovinz - Regierungsbezirk Aachen - Kreis Geilenkirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106602" target="_blank">Schloss Canstein (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Brilon)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121235" target="_blank">Schloss Carlsruhe (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Oppeln)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112032" target="_blank">Schloss Coblenz (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Coblenz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117227" target="_blank">Schloss Corvey (Provinz Westphalen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106829" target="_blank">Schloss Cossenblatt (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Beeskow-Storkow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123675" target="_blank">Schloss Cummerow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Demmin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109919" target="_blank">Schloss Czestram-Golejewko (Provinz Posen - Regierungsbezirk Posen - Kreis Kroeben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115331" target="_blank">Schloss Deuna (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Worbis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109855" target="_blank">Schloss Domnau (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Friedland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130065" target="_blank">Schloss Erxleben (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119977" target="_blank">Schloss Falkenstein (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109636" target="_blank">Schloss Frankenberg (Rheinprovinz - Regierungsbezirk Aachen - Kreis Aachen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128295" target="_blank">Schloss Frentz (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bergheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109694" target="_blank">Schloss Freusburg (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Altenkirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126711" target="_blank">Schloss Gebesee (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Weissensee)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110088" target="_blank">Schloss Gerdauen (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Gerdauen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110397" target="_blank">Schloss Gracht (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115970" target="_blank">Schloss Hardenberg (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Mettmann)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115684" target="_blank">Schloss Hiller Gaertringen (Provinz Posen - Regierungsbezirk Posen - Kreis Meseritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127307" target="_blank">Schloss Holte (Provinz Westphalen - Regierungsbezirk Minden - Kreis Wiedenbrück)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115268" target="_blank">Schloss Ilsenburg (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Wernigerode)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108797" target="_blank">Schloss Krickenbeck (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Geldern)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119813" target="_blank">Schloss Kuhna (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Görlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107878" target="_blank">Schloss Leerodt (Rheinprovinz - Regierungsbezirk Aachen - Kreis Geilenkirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129214" target="_blank">Schloss Lessendorf (Provinz Schlesien - Regierungsbezirk Lieqnitz - Kreis Freystadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110013" target="_blank">Schloss Mansfeld (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121631" target="_blank">Schloss Meisdorf (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102985" target="_blank">Schloss Mon-Choix (Harnekop) (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ober-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111723" target="_blank">Schloss Mroczen (Provinz Posen - Regierungsbezirk Posen - Kreis Schildberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116332" target="_blank">Schloss Nebra (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111707" target="_blank">Schloss Neindorf (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Oschersleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113478" target="_blank">Schloss Neuhoff (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hirschberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110030" target="_blank">Schloss Neuwied (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Neuwied)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113322" target="_blank">Schloss Nörvenich und Burg Nörvenich (Rheinprovinz - Regierungsbezirk Aachen - Kreis Düren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113344" target="_blank">Schloss Nörvenich und Burg Nörvenich (Rheinprovinz - Regierungsbezirk Aachen - Kreis Düren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121317" target="_blank">Schloss Parchen (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113884" target="_blank">Schloss Runowo (Provinz Posen - Regierungsbezirk Bromberg - Kreis Wirsitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108449" target="_blank">Schloss Sayn (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Coblenz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130779" target="_blank">Schloss Schönstein (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Altenkirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129090" target="_blank">Schloss Sommerfeld (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Krossen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124652" target="_blank">Schloss Spycker (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Rügen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126795" target="_blank">Schloss Teupitz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Teltow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110537" target="_blank">Schloss Wilhelmshöhe (Provinz Hessen - Regierungsbezirk Kassel - Kreis Kassel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130895" target="_blank">Schloss Wotersen ( - - )</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131092" target="_blank">Schlüsselburg (Provinz Westphalen - Regierungsbezirk Minden - Kreis Minden)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126470" target="_blank">Schmenzin (Provinz Pommern - Regierungsbezirk Köslin - Kreis Belgard)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128234" target="_blank">Schoetzow (Provinz Pommern - Regierungsbezirk Köslin - Kreis Fürstenthum-Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122734" target="_blank">Schomberg-Orzegow (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Beuthen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122840" target="_blank">Schönau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106393" target="_blank">Schönau (Rheinprovinz - Regierungsbezirk Aachen - Kreis Aachen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106929" target="_blank">Schönberg (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Rosenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116357" target="_blank">Schönbruch (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Friedland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124522" target="_blank">Schönbrunn (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117628" target="_blank">Schöneiche (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Nieder-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128121" target="_blank">Schönhausen (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124970" target="_blank">Schönthal (Rheinprovinz - Regierungsbezirk Aachen - Kreis Aachen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115933" target="_blank">Schönwerder A. (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115958" target="_blank">Schönwerder B. (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106283" target="_blank">Schosdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Löwenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121098" target="_blank">Schosdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Löwenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108550" target="_blank">Schräbsdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Frankenstein)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128427" target="_blank">Schübben (Provinz Pommern - Regierungsbezirk Köslin - Kreis Fürstenthum-Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109811" target="_blank">Schulzendorf (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ober-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121151" target="_blank">Schwarzenraben (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Lippstadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125151" target="_blank">Schwarzwaldau (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Landeshut)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108576" target="_blank">Schweckhausen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Peckelsheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106628" target="_blank">Schweinitz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Grünberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116151" target="_blank">Schwengfeld (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Schweidnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103365" target="_blank">Schwerinsburg (Provinz Pommern - Regierungsbezirk Stettin - Kreis Anklam)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114144" target="_blank">Schwierse (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Oels)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126618" target="_blank">Seebach (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Langensalza)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130604" target="_blank">Seeburg (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Seekreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122969" target="_blank">Seedorf (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118240" target="_blank">Seehoff (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Schlawe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117892" target="_blank">Seese (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118131" target="_blank">Segenthin (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Schlawe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107855" target="_blank">Sellendorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103127" target="_blank">Semlow (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Franzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104589" target="_blank">Senden (Provinz Westphalen - Regierungsbezirk Münster - Kreis Lüdinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105127" target="_blank">Seubersdorf (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Marienwerder)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128593" target="_blank">Sglietz (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lübben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104995" target="_blank">Sibyllenort (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Oels)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105877" target="_blank">Siedkow (Provinz Pommern - Regierungsbezirk Köslin - Kreis Belgard)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130175" target="_blank">Sienno (Provinz Posen - Regierungsbezirk Bromberg - Kreis Bromberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123634" target="_blank">Sieversdorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118156" target="_blank">Silbitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Nimptsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131033" target="_blank">Simmenau (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Kreuzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125564" target="_blank">S Lagow (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Sternberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130235" target="_blank">Slawentzitz (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Cosel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119710" target="_blank">S Neudorf (Provinz Posen - Regierungsbezirk Posen - Kreis Meseritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113690" target="_blank">S Neudorf (Provinz Posen - Regierungsbezirk Posen - Kreis Samter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115628" target="_blank">Sobotka (Provinz Posen - Regierungsbezirk Posen - Kreis Pleschen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114088" target="_blank">Sollstedt (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Nordhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103107" target="_blank">Sommerschenburg (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Neuhaldensleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120750" target="_blank">Sonnewalde (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110156" target="_blank">Sontra (Provinz Hessen - Regierungsbezirk Kassel - Kreis Rothenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126391" target="_blank">Sorquitten (Provinz Preussen - Regierungsbezirk Gumbinnen - Kreis Sensburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121079" target="_blank">S Platen (Provinz Preussen - Regierungsbezirk Danzig - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115601" target="_blank">Stabelwitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Breslau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125214" target="_blank">Stammheim (Rheinprovinz - Regierungsbezirk Cöln - Kreis Mülheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123219" target="_blank">Stargord (Provinz Pommern - Regierungsbezirk Stettin - Kreis Regenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106208" target="_blank">Starnitz (Provinz Pommern - Regierungsbezirk Köslin - Kreis Stolp)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126672" target="_blank">Starpel (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Züllichau-Schwiebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123035" target="_blank">Starzin (Provinz Preussen - Regierungsbezirk Danzig - Kreis Neustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113565" target="_blank">Steine (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Oels)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125259" target="_blank">Steinhausen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Hagen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108368" target="_blank">Steinhausen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Halle)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5129155" target="_blank">Steinhöfel (Provinz Pommern - Regierungsbezirk Stettin - Kreis Saatzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102628" target="_blank">Steinhöffel (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lebus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124716" target="_blank">Stephanshayn (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Schweidnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111884" target="_blank">St. Matthias (Rheinprovinz - Regierungsbezirk Trier - Kreis Trier)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130268" target="_blank">Stockhausen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Lübbecke)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115296" target="_blank">Stoellen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120862" target="_blank">Stolpe (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Angermünde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108217" target="_blank">Stolzenfels (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Coblenz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125139" target="_blank">Strachmin (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Fürstenthum-Cammin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125433" target="_blank">Straupitz (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Lübben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128857" target="_blank">Straussfurt (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Weissensee)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123532" target="_blank">Streidelsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Freystadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119698" target="_blank">Strelitz (Provinz Posen - Regierungsbezirk Bromberg - Kreis Chodziesen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121018" target="_blank">Stremlow (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Grimmen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108191" target="_blank">Strünkede (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Bochum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114262" target="_blank">Strzelewo (Provinz Posen - Regierungsbezirk Bromberg - Kreis Bromberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130997" target="_blank">Stubendorf (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Gross-Strehlitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5118183" target="_blank">Stülpe (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Jüterbog-Luckenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102523" target="_blank">St. Ulrich (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116037" target="_blank">Succow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Pyritz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121134" target="_blank">Suckow (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Templin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116379" target="_blank">Szrodke (Provinz Posen - Regierungsbezirk Posen - Kreis Birnbaum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130097" target="_blank">Taczanowo (Provinz Posen - Regierungsbezirk Posen - Kreis Pleschen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125650" target="_blank">Tamsel (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Landsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106315" target="_blank">Tannhausen (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Waldenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103269" target="_blank">Tantow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Randow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121905" target="_blank">Tautschken (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Neidenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102544" target="_blank">Tegel (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Nieder-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108241" target="_blank">Thale (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Aschersleben)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108322" target="_blank">Thamm (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hirschberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130811" target="_blank">Theessen (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow I)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5131113" target="_blank">Tiefhartmannsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113789" target="_blank">Tiefhartmannsdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Schönau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130508" target="_blank">Tile Moyland (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Cleve)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119419" target="_blank">Tillowitz (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Falkenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117188" target="_blank">Tilsen (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Salzwedel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108657" target="_blank">Toltz (Provinz Pommern - Regierungsbezirk Stettin - Kreis Saatzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117279" target="_blank">Trachenberg (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Militsch-Trachenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124736" target="_blank">Trampe (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ober-Barnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125273" target="_blank">Treben (Provinz Posen - Regierungsbezirk Posen - Kreis Fraustadt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104542" target="_blank">Trips (Rheinprovinz - Regierungsbezirk Aachen - Kreis Geilenkirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116291" target="_blank">Tschirnitz (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Glogau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119316" target="_blank">Turawa (Provinz Schlesien - Regierungsbezirk Oppeln - Kreis Oppeln)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108536" target="_blank">Türnich (Rheinprovinz - Regierungsbezirk Coblenz - Kreis Bergheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109934" target="_blank">Tussainen (Provinz Ostpreussen - Regierungsbezirk Gumbinnen - Kreis Ragnit)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115901" target="_blank">Tzschocha (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Lauban)</a></p>
    </div>
    <div id="u-z" class="tab-pane">
    <h3>U-Z</h3>
    <p><a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5114532" target="_blank">Uhyst an der Spree (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Hoyerswerda)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127041" target="_blank">Ulenburg (Provinz Westphalen - Regierungsbezirk Minden - Kreis Herford)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115749" target="_blank">Ullersdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Rothenburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119739" target="_blank">Unterfrankleben (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Merseburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124699" target="_blank">Unterschloss Arnstein (Provinz Sachsen - Regierungsbezirk Merseburg - Mansfelder Gebirgskreis)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5105898" target="_blank">Üntrop (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Hamm)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121701" target="_blank">Vahnerow (Provinz Pommern - Regierungsbezirk Stettin - Kreis Greifenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119050" target="_blank">Varzin (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Schlawe)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108078" target="_blank">Velbrueggen (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Neuss)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121674" target="_blank">Velen (Provinz Westphalen - Regierungsbezirk Münster - Kreis Borken)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110459" target="_blank">Venne (Provinz Westphalen - Regierungsbezirk Münster - Kreis Lüdinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117042" target="_blank">Veynau (Rheinprovinz - Regierungsbezirk Cöln - Kreis Euskirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117101" target="_blank">Vietnitz (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Königsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104506" target="_blank">Vinsebeck (Provinz Westphalen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119158" target="_blank">Vinzelberg (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Gardelegen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102346" target="_blank">Vogelsang (Provinz Pommern - Regierungsbezirk Stettin - Kreis Uckermünde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106036" target="_blank">Volkardey (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Düsseldorf)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104217" target="_blank">Vorhaus (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Goldberg-Haynau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127221" target="_blank">Vorhelm (Provinz Westphalen - Regierungsbezirk Münster - Kreis Beckum)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104283" target="_blank">Vornholz (Provinz Westphalen - Regierungsbezirk Münster - Kreis Warendorf)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120966" target="_blank">Wagenitz (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Havelland)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126497" target="_blank">Wahn (Rheinprovinz - Regierungsbezirk Cöln - Kreis Mülheim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115789" target="_blank">Waltersdorf (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Löwenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106789" target="_blank">Wammen (Rheinprovinz - Regierungsbezirk Aachen - Kreis Heinsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127383" target="_blank">Waplitz (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Stuhm)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126814" target="_blank">Warchau (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122632" target="_blank">Warnitz (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Königsberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126412" target="_blank">Wartenberg (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Polnisch Wartenberg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5126739" target="_blank">Wartin (Provinz Pommern - Regierungsbezirk Stettin - Kreis Randow)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113768" target="_blank">Wasowo (Provinz Posen - Regierungsbezirk Posen - Kreis Buk)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106848" target="_blank">Wättrisch (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Nimptsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123557" target="_blank">Wehrden (Provinz Westphalen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110471" target="_blank">Weigelsdorf (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Reichenbaoh)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124933" target="_blank">Weissensee (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Niederbarnim)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112000" target="_blank">Werben (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Cottbus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5102369" target="_blank">Werdringen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Hagen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111457" target="_blank">Wernburg (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Ziegenrück)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120731" target="_blank">Wernigerode (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Wernigerode)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5125605" target="_blank">Wesslienen (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Heiligenbeil)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5117874" target="_blank">Westerholt (Provinz Westphalen - Regierungsbezirk Münster - Kreis Recklinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110744" target="_blank">Westerwinkel (Provinz Westphalen - Regierungsbezirk Münster - Kreis Lüdinghausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110662" target="_blank">Westheim (Provinz Westphalen - Regierungsbezirk Minden - Kreis Buren)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5123771" target="_blank">Wewer (Provinz Westphalen - Regierungsbezirk Minden - Kreis Paderborn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128613" target="_blank">Wiedersee (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Graudenz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130383" target="_blank">Wierzbiczany (Provinz Posen - Regierungsbezirk Bromberg - Kreis Inowraclaw)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5127009" target="_blank">Wiesenburg (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Zauche-Belzig)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5120802" target="_blank">Wildenhoff (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Preussisch Eylau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121337" target="_blank">Wildschütz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Oels)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5113712" target="_blank">Wilhelmsburg (Provinz Schlesien - Regierungsbezirk Liegnitz - Kreis Bolkenhain)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130665" target="_blank">Willkamm (Provinz Preussen - Regierungsbezirk Königsberg - Kreis Gerdauen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5104191" target="_blank">Winnenthal (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Mors)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112377" target="_blank">Wintdorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Cottbus)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110352" target="_blank">Winterbüren (Provinz Hessen - Regierungsbezirk Kassel - Landkreis Kassel)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107912" target="_blank">Wischelingen (Provinz Westphalen - Regierungsbezirk Arnsberg - Kreis Dortmund)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5109873" target="_blank">Wittenmoor (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Stendal)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108159" target="_blank">Witzschersdorf (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Merseburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130568" target="_blank">Wohnung (Rheinprovinz - Regierungsbezirk Düsseldorf - Kreis Duisburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130288" target="_blank">Wolde (Provinz Pommern - Regierungsbezirk Stettin - Kreis Demmin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108261" target="_blank">Wolfsburg (Rheinprovinz - Regierungsbezirk Cöln - Kreis Bonn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103047" target="_blank">Wolfshagen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis West-Priegnitz)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128348" target="_blank">Wolfshagen (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Prenzlau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112513" target="_blank">Wolkramshausen (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Nordhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110623" target="_blank">Wolkramshausen (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Nordhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5119475" target="_blank">Wollmirstedt (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Eckartsberga)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128199" target="_blank">Wolmirstedt (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Wolmirstedt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108039" target="_blank">Wottnogge (Provinz Pommern - Regierungsbezirk Köslin - Kreis Stolp)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5110769" target="_blank">Wülfingerode (Provinz Sachsen - Regierungsbezirk Erfurt - Kreis Nordhausen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5130642" target="_blank">Würgassen (Provinz Westphalen - Regierungsbezirk Minden - Kreis Höxter)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108130" target="_blank">Wustrau (Provinz Brandenburg - Regierungsbezirk Potsdam - Kreis Ruppin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5111630" target="_blank">Wust (Provinz Sachsen - Regierungsbezirk Magdeburg - Kreis Jerichow II (Genthin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124461" target="_blank">Wybcz (Provinz Preussen - Regierungsbezirk Marienwerder - Kreis Thorn)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112317" target="_blank">Zernikow (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Soldin)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5115761" target="_blank">Zezenow (Provinz Pommern - Regierungsbezirk Cöslin - Kreis Stolp)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5103309" target="_blank">Zieckau (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Luckau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121492" target="_blank">Zieserwitz (Provinz Schlesien - Regierungsbezirk Breslau - Kreis Neumarkt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5107061" target="_blank">Zilmsdorf (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Sorau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5124540" target="_blank">Zimckendorf (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Franzburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5121377" target="_blank">Zimmerhausen (Provinz Pommern - Regierungsbezirk Stettin - Kreis Regenwalde)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5116317" target="_blank">Zingst (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5106868" target="_blank">Zinnitz (Provinz Brandenburg - Regierungsbezirk Frankfurt a. O. - Kreis Calau)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5108173" target="_blank">Zscheiplitz (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Querfurt)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122618" target="_blank">Zschepplin (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Delitzsch)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128154" target="_blank">Zubzow (Provinz Pommern - Regierungsbezirk Stralsund - Kreis Rügen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5122802" target="_blank">Zülshagen (Provinz Brandenburg - Regierungsbezirk Köslin - Kreis Dramburg)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5112291" target="_blank">Zweibrüggen (Rheinprovinz - Regierungsbezirk Aachen - Kreis Geilenkirchen)</a><br/> <a href="/viewer/resolver?urn=urn:nbn:de:kobv:109-1-5128779" target="_blank">Zwiesigkow (Provinz Sachsen - Regierungsbezirk Merseburg - Kreis Schweinitz)</a></p>
    </div>
    </div>
    </div></span>

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
            final String query = SearchHelper.buildFinalQuery(currentQuery, DataManager.getInstance().getConfiguration().isAggregateHits());
            Map<String, String> params = SearchHelper.generateQueryParams();
            final SXSSFWorkbook wb = SearchHelper.exportSearchAsExcel(query, currentQuery, sortFields, params, searchTerms, navigationHelper
                    .getLocale(), DataManager.getInstance().getConfiguration().isAggregateHits());
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            facesContext.getExternalContext().responseReset();
            facesContext.getExternalContext().setResponseContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            facesContext.getExternalContext().setResponseHeader("Content-Disposition", "attachment;filename=\"viewer_search_"
                    + Helper.formatterISO8601DateTime.print(System.currentTimeMillis()) + ".xlsx\"");
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

    /**
     * @return the advancedSearchQueryInfo
     */
    public String getAdvancedSearchQueryInfo() {
        return advancedSearchQueryInfo;
    }

    /**
     * @return
     */
    public List<StringPair> getSortFields() {
        return this.sortFields;
    }

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

}
