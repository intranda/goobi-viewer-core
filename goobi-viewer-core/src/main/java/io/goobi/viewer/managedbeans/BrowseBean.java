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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.AlphanumCollatorComparator;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordDeletedException;
import io.goobi.viewer.exceptions.RecordLimitExceededException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.RedirectException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.termbrowsing.BrowseTerm;
import io.goobi.viewer.model.termbrowsing.BrowseTermComparator;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.collections.BrowseDcElement;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.CollectionView.BrowseDataProvider;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * This bean provides the data for collection and term browsing.
 */
@Named
@SessionScoped
public class BrowseBean implements Serializable {

    private static final long serialVersionUID = 7613678633319477862L;

    private static final Logger logger = LogManager.getLogger(BrowseBean.class);

    private static final String MSG_ERR_FIELDS_NOT_CONFIGURED = "browse_errFieldNotConfigured";

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private BreadcrumbBean breadcrumbBean;
    @Inject
    private SearchBean searchBean;

    /** Hits per page in the browsing menu. */
    private int browsingMenuHitsPerPage = DataManager.getInstance().getConfiguration().getBrowsingMenuHitsPerPage();

    /** Pretty URL variable. */
    private String collectionToExpand = null;
    private String topVisibleCollection = null;
    private String targetCollection = null;

    /** Solr field to browse. */
    private String browsingMenuField = null;
    /** Term list for the current result page (browsing menu). Used for displaying. */
    private List<String> browseTermList;
    /** Escaped term list for the current result page (browsing menu). Used for URL construction. */
    private List<String> browseTermListEscaped;
    private List<Long> browseTermHitCountList;
    private Map<String, List<String>> availableStringFilters = new HashMap<>();
    /** This is used for filtering term browsing by the starting letter. */
    private String currentStringFilter = "";
    /** Optional filter query */
    private String filterQuery;
    private int hitsCount = 0;
    private int currentPage = -1;

    private Map<String, CollectionView> collections = new HashMap<>();
    private String collectionField = SolrConstants.DC;

    /**
     * Empty constructor.
     */
    public BrowseBean() {
        // the emptiness inside
    }

    /**
     * Required setter for ManagedProperty injection
     *
     * @param breadcrumbBean the breadcrumbBean to set
     */
    public void setBreadcrumbBean(BreadcrumbBean breadcrumbBean) {
        this.breadcrumbBean = breadcrumbBean;
    }

    /**
     * Required setter for ManagedProperty injection
     *
     * @param searchBean the searchBean to set
     */
    public void setSearchBean(SearchBean searchBean) {
        this.searchBean = searchBean;
    }

    /**
     * Resets all lists for term browsing.
     */
    public void resetTerms() {
        if (browseTermList != null) {
            browseTermList.clear();
        }
        if (browseTermListEscaped != null) {
            browseTermListEscaped.clear();
        }
        if (browseTermHitCountList != null) {
            browseTermHitCountList.clear();
        }
        if (availableStringFilters != null) {
            availableStringFilters.clear();
        }
    }

    /**
     * <p>
     * resetAllLists.
     * </p>
     */
    public void resetAllLists() {
        for (Entry<String, CollectionView> entry : collections.entrySet()) {
            entry.getValue().resetCollectionList();
        }
    }

    /**
     * <p>
     * resetDcList.
     * </p>
     */
    public void resetDcList() {
        logger.trace("resetDcList");
        resetList(SolrConstants.DC);
    }

    /**
     * <p>
     * resetList.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
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
     * <p>
     * getDcList.
     * </p>
     *
     * @return the dcList (Collections)
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<BrowseDcElement> getDcList() throws IndexUnreachableException {
        return getList(SolrConstants.DC);
    }

    /**
     * <p>
     * getList.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<BrowseDcElement> getList(String field) throws IndexUnreachableException {
        return getList(field, -1);
    }

    /**
     * <p>
     * getList.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param depth a int.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public List<BrowseDcElement> getList(String field, int depth) throws IndexUnreachableException {
        try {
            if (collections.get(field) == null) {
                initializeCollection(field, null);
                populateCollection(field);
            }
            if (collections.get(field) != null) {
                CollectionView collection = collections.get(field);
                collection.expandAll(depth);
                collection.calculateVisibleDcElements();
                return new ArrayList<>(collection.getVisibleDcElements());
            }
        } catch (IllegalRequestException e) {
            logger.error(e.toString(), e);
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * populateCollection.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void populateCollection(String field) throws IndexUnreachableException, IllegalRequestException {
        if (collections.containsKey(field)) {
            collections.get(field).populateCollectionList();
        }
    }

    /**
     * <p>
     * Getter for the field <code>collectionToExpand</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getCollectionToExpand() {
        synchronized (this) {
            return collectionToExpand;
        }
    }

    /**
     * <p>
     * Setter for the field <code>collectionToExpand</code>.
     * </p>
     *
     * @param collectionToExpand a {@link java.lang.String} object.
     */
    public void setCollectionToExpand(String collectionToExpand) {
        synchronized (this) {
            this.collectionToExpand = collectionToExpand;
            this.topVisibleCollection = collectionToExpand;
        }
    }

    /**
     * <p>
     * Getter for the field <code>topVisibleCollection</code>.
     * </p>
     *
     * @return the topVisibleCollecion
     */
    public String getTopVisibleCollection() {
        if (topVisibleCollection == null && collectionToExpand != null) {
            return collectionToExpand;
        }
        return topVisibleCollection;
    }

    /**
     * <p>
     * Setter for the field <code>topVisibleCollection</code>.
     * </p>
     *
     * @param topVisibleCollecion the topVisibleCollecion to set
     */
    public void setTopVisibleCollection(String topVisibleCollecion) {
        this.topVisibleCollection = topVisibleCollecion;
    }

    /**
     * Use this method of a certain collections needs to be expanded via URL.
     *
     * @param levels a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void expandCollection(int levels) throws IndexUnreachableException, IllegalRequestException {
        expandCollection(SolrConstants.DC, null, levels);
    }

    /**
     * <p>
     * expandCollection.
     * </p>
     *
     * @param collectionField a {@link java.lang.String} object.
     * @param facetField a {@link java.lang.String} object.
     * @param levels a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws IllegalRequestException
     */
    public void expandCollection(String collectionField, String facetField, int levels) throws IndexUnreachableException, IllegalRequestException {
        synchronized (this) {
            initializeCollection(collectionField, facetField);
            collections.get(collectionField).setBaseLevels(levels);
            collections.get(collectionField).setBaseElementName(getCollectionToExpand());
            collections.get(collectionField).setTopVisibleElement(getTopVisibleCollection());
            collections.get(collectionField).populateCollectionList();
        }
    }

    /**
     * Action method for JSF.
     *
     * @return Navigation outcome
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String searchTermsAction() throws PresentationException, IndexUnreachableException {
        try {
            return searchTerms();
        } catch (RedirectException e) {
            // Redirect to filter URL requested
            if (MSG_ERR_FIELDS_NOT_CONFIGURED.equals(e.getMessage())) {
                return "pretty:error";
            }
            return "pretty:searchTerm4";
        }
    }

    /**
     * <p>
     * searchTerms.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws RedirectException
     */
    public String searchTerms() throws PresentationException, IndexUnreachableException, RedirectException {
        synchronized (this) {
            logger.trace("searchTerms");
            if (breadcrumbBean != null) {
                breadcrumbBean.updateBreadcrumbsWithCurrentUrl("browseTitle", BreadcrumbBean.WEIGHT_SEARCH_TERMS);
            }
            if (searchBean != null) {
                searchBean.setSearchString("");
                searchBean.resetSearchParameters(true);
            }
            hitsCount = 0;

            // Sort filters
            Locale locale = null;
            if (navigationHelper != null) {
                locale = navigationHelper.getLocale();
            } else {
                locale = ViewerResourceBundle.getDefaultLocale();
            }

            List<BrowseTerm> terms = null;
            BrowsingMenuFieldConfig currentBmfc = null;
            List<BrowsingMenuFieldConfig> bmfcList = DataManager.getInstance().getConfiguration().getBrowsingMenuFields();
            for (BrowsingMenuFieldConfig bmfc : bmfcList) {
                if (bmfc.getField().equals(browsingMenuField)) {
                    currentBmfc = bmfc;
                    break;
                }
            }
            if (currentBmfc == null) {
                logger.error("No configuration found for term field '{}'.", browsingMenuField);
                resetTerms();
                Messages.error(ViewerResourceBundle.getTranslation(MSG_ERR_FIELDS_NOT_CONFIGURED, null).replace("{0}", browsingMenuField));
                throw new RedirectException(MSG_ERR_FIELDS_NOT_CONFIGURED);
            }

            String useFilterQuery = generateFilterQuery((!DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled()
                    || DataManager.getInstance().getConfiguration().getSearchResultGroups().isEmpty())
                            ? Collections.singletonList(SearchResultGroup.createDefaultGroup())
                            : DataManager.getInstance().getConfiguration().getSearchResultGroups());
            // logger.trace("useFilterQuery: {}", useFilterQuery); //NOSONAR Sometimes needed for debugging

            // Populate the list of available starting characters with ones that actually exist in the complete terms list
            String browsingMenuFieldForCurrentLanguage = getBrowsingMenuFieldForLanguage(locale.getLanguage());
            if (availableStringFilters.get(browsingMenuFieldForCurrentLanguage) == null) {
                logger.trace("Collecting available filters for {}", browsingMenuFieldForCurrentLanguage);
                int numRows = StringUtils.isNotEmpty(currentBmfc.getSortField()) ? SolrSearchIndex.MAX_HITS : 0;
                terms = SearchHelper.getFilteredTerms(currentBmfc, "", useFilterQuery, 0, numRows, new BrowseTermComparator(locale),
                        locale.getLanguage());
                if (availableStringFilters.get(browsingMenuFieldForCurrentLanguage) == null || filterQuery != null) {
                    logger.trace("Populating search term filters for field '{}'...", browsingMenuFieldForCurrentLanguage);
                    availableStringFilters.put(browsingMenuFieldForCurrentLanguage, new ArrayList<>());
                    for (BrowseTerm term : terms) {
                        String rawTerm;
                        if (StringUtils.isNotEmpty(term.getSortTerm())) {
                            rawTerm = term.getSortTerm();
                        } else {
                            rawTerm = term.getTerm();
                        }
                        if (StringUtils.isEmpty(rawTerm)) {
                            continue;
                        }
                        String firstChar;
                        if (StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars())) {
                            // Exclude leading characters from filters explicitly configured to be ignored
                            firstChar = BrowseTermComparator.normalizeString(rawTerm,
                                    DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars())
                                    .trim()
                                    .substring(0, 1)
                                    .toUpperCase();
                        } else {
                            firstChar = rawTerm.substring(0, 1).toUpperCase();
                        }
                        if (!availableStringFilters.get(browsingMenuFieldForCurrentLanguage).contains(firstChar) && !"-".equals(firstChar)) {
                            availableStringFilters.get(browsingMenuFieldForCurrentLanguage).add(firstChar);
                        }
                    }
                }

                Collections.sort(availableStringFilters.get(browsingMenuFieldForCurrentLanguage),
                        new AlphanumCollatorComparator(Collator.getInstance(locale)));
            }

            // If no filter is set, redirect to first available filter (if so configured)
            if (StringUtils.isEmpty(currentStringFilter) && currentBmfc.isAlwaysApplyFilter()
                    && availableStringFilters.get(browsingMenuFieldForCurrentLanguage) != null
                    && !availableStringFilters.get(browsingMenuFieldForCurrentLanguage).isEmpty()) {
                currentStringFilter = selectRedirectFilter();
                logger.trace("Redirecting to filter: {}", currentStringFilter);
                throw new RedirectException("");
            }

            hitsCount = SearchHelper.getFilteredTermsCount(currentBmfc, currentStringFilter, useFilterQuery, locale.getLanguage());
            if (hitsCount == 0) {
                resetTerms();
                return "searchTermList";
            }

            if (currentPage > getLastPage()) {
                currentPage = getLastPage();
            }
            int start = (currentPage - 1) * browsingMenuHitsPerPage;
            int end = currentPage * browsingMenuHitsPerPage;
            if (end > hitsCount) {
                end = hitsCount;
            }
            browseTermList = new ArrayList<>(browsingMenuHitsPerPage);
            browseTermListEscaped = new ArrayList<>(browseTermList.size());
            browseTermHitCountList = new ArrayList<>(browseTermList.size());

            // Get terms for the current page
            logger.trace("Fetching terms for page {} ({} - {})", currentPage, start, end - 1);
            terms = SearchHelper.getFilteredTerms(currentBmfc, currentStringFilter, useFilterQuery, 0, SolrSearchIndex.MAX_HITS,
                    new BrowseTermComparator(locale), locale.getLanguage());

            for (int i = start; i < end; ++i) {
                if (i >= terms.size()) {
                    //filtered queries may return less results than max (why? SearchHelper.getFilteredTermsCount should already account for filtering)
                    break;
                }
                BrowseTerm term = terms.get(i);
                Optional<String> translation = term.getTranslations() != null ? term.getTranslations().getValue(locale) : Optional.empty();
                if (translation.isPresent()) {
                    // Use translated label, if present
                    browseTermList.add(translation.get());
                } else {
                    browseTermList.add(term.getTerm());
                }
                browseTermHitCountList.add(terms.get(i).getHitCount());

                // Escape characters such as quotation marks
                String escapedTerm = ClientUtils.escapeQueryChars(term.getTerm().intern());
                escapedTerm = BeanUtils.escapeCriticalUrlChracters(escapedTerm);
                try {
                    escapedTerm = URLEncoder.encode(escapedTerm, SearchBean.URL_ENCODING);
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage());
                }
                browseTermListEscaped.add(escapedTerm.intern());
            }

            return "searchTermList";
        }
    }

    /**
     * @param resultGroups
     * @return Generated filter query or empty string
     * @should return empty string if no filterQuery or result groups available
     * @should generate filter query correctly
     */
    String generateFilterQuery(List<SearchResultGroup> resultGroups) {
        if (StringUtils.isEmpty(filterQuery) && (resultGroups == null || resultGroups.size() < 2)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(filterQuery)) {
            sb.append("+(").append(filterQuery).append(")");
        }
        if (resultGroups.size() > 1) {
            sb.append(" +(");
            for (SearchResultGroup resultGroup : resultGroups) {
                if (StringUtils.isNotEmpty(resultGroup.getQuery())) {
                    sb.append(" (").append(resultGroup.getQuery()).append(")");
                }
            }
            sb.append(")");
        }

        return "+(" + sb.toString().trim() + ")";
    }

    /**
     * Selects a filter string for automatic redirecting, prioritizing letters, followed by numbers and finally by the first available filter.
     *
     * @return Selected filter string
     * @should return first available alphabetical filter if available
     * @should return numerical filter if available
     * @should return first filter if no other available
     */
    public String selectRedirectFilter() {
        if (availableStringFilters.isEmpty()) {
            return null;
        }

        String numericalFilter = null;
        String alphaFilter = null;
        String browsingMenuFieldForCurrentLanguage =
                getBrowsingMenuFieldForLanguage(navigationHelper != null ? navigationHelper.getLocaleString() : null);
        for (String filter : availableStringFilters.get(browsingMenuFieldForCurrentLanguage)) {
            switch (filter) {
                case "0-9":
                    numericalFilter = filter;
                    break;
                default:
                    if (filter.matches("[A-ZÄÁÀÂÖÓÒÔÜÚÙÛÉÈÊ]") && alphaFilter == null) {
                        alphaFilter = filter;
                    }
                    break;
            }
        }

        if (alphaFilter != null) {
            return alphaFilter;
        } else if (numericalFilter != null) {
            return numericalFilter;
        } else {
            return availableStringFilters.get(browsingMenuFieldForCurrentLanguage).get(0);
        }
    }

    /**
     * 
     * @param language Requested language
     * @return browsingMenuField (modified for given language if placeholder found)
     * @should return field for given language if placeholder found
     * @should return browsingMenuField if no language placeholder
     */
    public String getBrowsingMenuFieldForLanguage(final String language) {
        String useLanguage = language;
        if (useLanguage == null) {
            useLanguage = "";
        }
        useLanguage = useLanguage.toUpperCase();

        synchronized (this) {
            if (StringUtils.isEmpty(browsingMenuField)) {
                return "-";
            }

            if (browsingMenuField.endsWith(SolrConstants.MIDFIX_LANG + "{}")) {
                return browsingMenuField.replace("{}", useLanguage);
            }
            return browsingMenuField;
        }
    }

    /**
     * <p>
     * Getter for the field <code>browsingMenuField</code>.
     * </p>
     *
     * @return the browsingMenuField
     */
    public String getBrowsingMenuField() {
        synchronized (this) {
            if (StringUtils.isEmpty(browsingMenuField)) {
                return "-";
            }

            return browsingMenuField;
        }
    }

    /**
     * <p>
     * Setter for the field <code>browsingMenuField</code>.
     * </p>
     *
     * @param browsingMenuField the browsingMenuField to set
     */
    public void setBrowsingMenuField(final String browsingMenuField) {
        synchronized (this) {
            String useBrowsingMenuField = browsingMenuField;
            if (useBrowsingMenuField == null || "-".equals(useBrowsingMenuField)) {
                useBrowsingMenuField = "";
            }
            try {
                this.browsingMenuField = URLDecoder.decode(useBrowsingMenuField, SearchBean.URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                this.browsingMenuField = useBrowsingMenuField;
            }
        }
    }

    /**
     *
     * @return true if <code>browsingMenuField</code> is set and configured to be translated; false otherwise
     */
    public boolean isBrowsingMenuFieldTranslated() {
        if (StringUtils.isEmpty(browsingMenuField)) {
            return false;
        }

        List<BrowsingMenuFieldConfig> bmfcList = DataManager.getInstance().getConfiguration().getBrowsingMenuFields();
        for (BrowsingMenuFieldConfig bmfc : bmfcList) {
            if (bmfc.getField().equals(browsingMenuField)) {
                return bmfc.isTranslate();
            }
        }

        return false;
    }

    /**
     * <p>
     * Getter for the field <code>browseTermList</code>.
     * </p>
     *
     * @return the browseTermList
     */
    public List<String> getBrowseTermList() {
        return browseTermList;
    }

    /**
     * <p>
     * Getter for the field <code>browseTermListEscaped</code>.
     * </p>
     *
     * @return the browseTermListEscaped
     */
    public List<String> getBrowseTermListEscaped() {
        return browseTermListEscaped;
    }

    /**
     * <p>
     * Getter for the field <code>browseTermHitCountList</code>.
     * </p>
     *
     * @return the browseTermHitCountList
     */
    public List<Long> getBrowseTermHitCountList() {
        return browseTermHitCountList;
    }

    /**
     * <p>
     * getPrevTermUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPrevTermUrl() {
        int page = 1;
        if (currentPage > 1) {
            page = currentPage - 1;
        }
        return new StringBuilder("/").append(browsingMenuField)
                .append('/')
                .append(getCurrentStringFilter())
                .append('/')
                .append(page)
                .append('/')
                .toString();
    }

    /**
     * <p>
     * getNextTermUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getNextTermUrl() {
        int page = getLastPage();
        if (currentPage < page) {
            page = currentPage + 1;
        }
        return new StringBuilder("/").append(browsingMenuField)
                .append('/')
                .append(getCurrentStringFilter())
                .append('/')
                .append(page)
                .append('/')
                .toString();
    }

    /**
     * <p>
     * Getter for the field <code>availableStringFilters</code>.
     * </p>
     *
     * @return the availableStringFilters
     */
    public List<String> getAvailableStringFilters() {
        String field = getBrowsingMenuFieldForLanguage(navigationHelper != null ? navigationHelper.getLocaleString() : null);
        if (availableStringFilters.get(field) == null) {
            try {
                searchTerms();
            } catch (PresentationException | IndexUnreachableException | RedirectException e) {
                //
            }
        }
        return availableStringFilters.get(field);
    }

    /**
     * Getter for unit tests.
     * @return the availableStringFilters
     */
    Map<String, List<String>> getAvailableStringFiltersMap() {
        return availableStringFilters;
    }

    /**
     * <p>
     * Getter for the field <code>currentStringFilter</code>.
     * </p>
     *
     * @return the currentStringFilter
     */
    public String getCurrentStringFilter() {
        synchronized (this) {
            if (StringUtils.isEmpty(currentStringFilter)) {
                return "-";
            }
            return currentStringFilter;
        }
    }

    /**
     * <p>
     * Setter for the field <code>currentStringFilter</code>.
     * </p>
     *
     * @param currentStringFilter the currentStringFilter to set
     */
    public void setCurrentStringFilter(final String currentStringFilter) {
        synchronized (this) {
            String useCurrentStringFilter = currentStringFilter;
            if (StringUtils.equals(useCurrentStringFilter, "-")) {
                useCurrentStringFilter = "";
            }
            try {
                this.currentStringFilter = URLDecoder.decode(useCurrentStringFilter, SearchBean.URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                this.currentStringFilter = useCurrentStringFilter;
            }
        }
    }

    /**
     * <p>
     * Getter for the field <code>filterQuery</code>.
     * </p>
     *
     * @return the filterQuery
     */
    public String getFilterQuery() {
        if (StringUtils.isEmpty(filterQuery)) {
            return "-";
        }
        return filterQuery;
    }

    /**
     * <p>
     * Setter for the field <code>filterQuery</code>.
     * </p>
     *
     * @param filterQuery the filterQuery to set
     */
    public void setFilterQuery(String filterQuery) {
        this.filterQuery = "-".equals(filterQuery) ? null : filterQuery;
    }

    /**
     * <p>
     * Getter for the field <code>currentPage</code>.
     * </p>
     *
     * @return the currentPage
     */
    public int getCurrentPage() {
        synchronized (this) {
            return currentPage;
        }
    }

    /**
     * <p>
     * Setter for the field <code>currentPage</code>.
     * </p>
     *
     * @param currentPage the currentPage to set
     */
    public void setCurrentPage(int currentPage) {
        synchronized (this) {
            this.currentPage = currentPage;
        }
    }

    /**
     * <p>
     * getLastPage.
     * </p>
     *
     * @return a int.
     */
    public int getLastPage() {
        int hitsPerPageLocal = browsingMenuHitsPerPage;
        int answer = (int) Math.floor((double) hitsCount / hitsPerPageLocal);
        if (hitsCount % hitsPerPageLocal != 0 || answer == 0) {
            answer++;
        }

        //        logger.trace(hitsCount + "/" + hitsPerPageLocal + "=" + answer);
        return answer;
    }

    /**
     * <p>
     * isBrowsingMenuEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isBrowsingMenuEnabled() {
        return DataManager.getInstance().getConfiguration().isBrowsingMenuEnabled();
    }

    /**
     * <p>
     * getBrowsingMenuItems.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return List of browsing menu items
     * @should skip items for language-specific fields if no language was given
     * @should skip items for language-specific fields if they don't match given language
     * @should return language-specific fields with placeholder
     */
    public List<String> getBrowsingMenuItems(final String language) {
        String useLanguage = language;
        if (useLanguage != null) {
            useLanguage = useLanguage.toUpperCase();
        }
        List<String> ret = new ArrayList<>();
        for (BrowsingMenuFieldConfig bmfc : DataManager.getInstance().getConfiguration().getBrowsingMenuFields()) {
            if (bmfc.getField().contains(SolrConstants.MIDFIX_LANG)
                    && (useLanguage == null || !(bmfc.getField().contains(SolrConstants.MIDFIX_LANG + useLanguage)
                            || bmfc.getField().contains(SolrConstants.MIDFIX_LANG + "{}")))) {
                logger.trace("Skipped term browsing field {} due to language mismatch.", bmfc.getField());
                continue;
            }
            ret.add(bmfc.getField());
        }

        return ret;
    }

    /**
     * 
     * @return List of configured browsing menu fields
     */
    public List<String> getConfiguredBrowsingMenuFields() {
        List<String> ret = new ArrayList<>();
        for (BrowsingMenuFieldConfig bmfc : DataManager.getInstance().getConfiguration().getBrowsingMenuFields()) {
            ret.add(bmfc.getField());
        }

        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>targetCollection</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTargetCollection() {
        return targetCollection;
    }

    /**
     * <p>
     * Setter for the field <code>targetCollection</code>.
     * </p>
     *
     * @param targetCollection a {@link java.lang.String} object.
     */
    public void setTargetCollection(String targetCollection) {
        this.targetCollection = targetCollection;
    }

    /**
     * <p>
     * openWorkInTargetCollection.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * getDcCollection.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.collections.CollectionView} object.
     */
    public CollectionView getDcCollection() {
        return getCollection(SolrConstants.DC);
    }

    /**
     * <p>
     * getCollection.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.collections.CollectionView} object.
     */
    public CollectionView getCollection(String field) {
        return collections.get(field);
    }

    /**
     * 
     * @param field
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
     * <p>
     * initializeDCCollection.
     * </p>
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
     * @param collectionField a {@link java.lang.String} object.
     * @param groupingField a {@link java.lang.String} object. Used for grouping results
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
     * <p>
     * Getter for the field <code>collectionField</code>.
     * </p>
     *
     * @return the collectionField
     */
    public String getCollectionField() {
        return collectionField;
    }

    /**
     * <p>
     * Setter for the field <code>collectionField</code>.
     * </p>
     *
     * @param collectionField the collectionField to set
     */
    public void setCollectionField(String collectionField) {
        this.collectionField = collectionField;
    }

    /**
     * TODO translation from DB
     *
     * @param collectionField
     * @param collectionValue
     * @return {@link String}
     * @should return hierarchy correctly
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
