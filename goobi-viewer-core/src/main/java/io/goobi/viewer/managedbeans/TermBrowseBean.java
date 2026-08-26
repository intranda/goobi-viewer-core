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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RedirectException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.termbrowsing.BrowseTerm;
import io.goobi.viewer.model.termbrowsing.BrowseTermComparator;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * This bean provides the data for alphabetical term browsing (the browsing menu). Collection browsing is handled by {@link CollectionBrowseBean}.
 */
@Named
@SessionScoped
public class TermBrowseBean implements Serializable {

    private static final long serialVersionUID = -6621885986357126555L;

    private static final Logger logger = LogManager.getLogger(TermBrowseBean.class);

    private static final String MSG_ERR_FIELDS_NOT_CONFIGURED = "browse_errFieldNotConfigured";

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private BreadcrumbBean breadcrumbBean;

    /** Hits per page in the browsing menu. */
    private int browsingMenuHitsPerPage = DataManager.getInstance().getConfiguration().getBrowsingMenuHitsPerPage();

    /** Solr field to browse. */
    private String browsingMenuField = null;
    /** Term list for the current result page (browsing menu). Used for displaying. */
    // volatile: published atomically to prevent ConcurrentModificationException when JSF iterates
    // these lists via c:forEach while another session request replaces or populates them
    private volatile List<String> browseTermList; //NOSONAR S3077: list replaced, never mutated
    /** Escaped term list for the current result page (browsing menu). Used for URL construction. */
    private volatile List<String> browseTermListEscaped; //NOSONAR S3077: list replaced, never mutated
    private volatile List<Long> browseTermHitCountList; //NOSONAR S3077: list replaced, never mutated
    private Map<String, List<String>> availableStringFilters = new HashMap<>();
    /** This is used for filtering term browsing by the starting letter. */
    private String currentStringFilter = "";
    /** Optional filter query. */
    private String filterQuery;
    private int hitsCount = 0;
    private int currentPage = -1;

    /**
     * Empty constructor.
     */
    public TermBrowseBean() {
        // the emptiness inside
    }

    /**
     * Required setter for ManagedProperty injection.
     *
     * @param breadcrumbBean the BreadcrumbBean instance to inject for testing
     */
    public void setBreadcrumbBean(BreadcrumbBean breadcrumbBean) {
        this.breadcrumbBean = breadcrumbBean;
    }

    /**
     * Resets all lists for term browsing.
     */
    public void resetTerms() {
        // Assign null rather than calling clear() on the existing list instances.
        // Calling clear() on a live reference while a JSF render thread holds an iterator
        // to the same list causes ConcurrentModificationException.
        browseTermList = null;
        browseTermListEscaped = null;
        browseTermHitCountList = null;
        if (availableStringFilters != null) {
            availableStringFilters.clear();
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
     * searchTerms.
     *
     * @return the navigation outcome after executing the term browse search
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
            useFilterQuery = useFilterQuery + " " + this.navigationHelper.getSubThemeDiscriminatorQuerySuffix();
            // logger.trace("useFilterQuery: {}", useFilterQuery); //NOSONAR Debug

            // Populate the list of available starting characters with ones that actually exist in the complete terms list
            String browsingMenuFieldForCurrentLanguage = getBrowsingMenuFieldForLanguage(locale.getLanguage());
            if (availableStringFilters.get(browsingMenuFieldForCurrentLanguage) == null) {
                logger.trace("Collecting available filters for {}", browsingMenuFieldForCurrentLanguage);
                availableStringFilters.put(browsingMenuFieldForCurrentLanguage,
                        SearchHelper.collectAvailableTermFilters(currentBmfc, useFilterQuery, locale));
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
            // Build lists as local variables; assign to fields only when complete.
            // This prevents a concurrent render thread from observing a partially-built list
            // via getBrowseTermList() and then hitting ConcurrentModificationException
            // when elements are added to the same list instance inside this synchronized block.
            List<String> newBrowseTermList = new ArrayList<>(browsingMenuHitsPerPage);
            List<String> newBrowseTermListEscaped = new ArrayList<>(browsingMenuHitsPerPage);
            List<Long> newBrowseTermHitCountList = new ArrayList<>(browsingMenuHitsPerPage);

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
                    newBrowseTermList.add(translation.get());
                } else {
                    newBrowseTermList.add(term.getTerm());
                }
                newBrowseTermHitCountList.add(terms.get(i).getHitCount());

                // Escape characters such as quotation marks
                String escapedTerm = ClientUtils.escapeQueryChars(term.getTerm().intern());
                escapedTerm = BeanUtils.escapeCriticalUrlChracters(escapedTerm);
                try {
                    escapedTerm = URLEncoder.encode(escapedTerm, SearchBean.URL_ENCODING);
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage());
                }
                newBrowseTermListEscaped.add(escapedTerm.intern());
            }

            // Atomically publish the fully-built lists so no reader ever sees a partial state
            this.browseTermList = newBrowseTermList;
            this.browseTermListEscaped = newBrowseTermListEscaped;
            this.browseTermHitCountList = newBrowseTermHitCountList;

            return "searchTermList";
        }
    }

    /**
     * @param resultGroups Search result groups used to build group sub-queries
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
            if (filter.matches("[A-ZÄÁÀÂÖÓÒÔÜÚÙÛÉÈÊ]") && alphaFilter == null) {
                alphaFilter = filter;
            } else if (filter.matches("[\\d]") && alphaFilter == null) {
                numericalFilter = filter;
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
     * Getter for the field <code>browsingMenuField</code>.
     *
     * @return the Solr field name used for term browsing, or "-" if none is set
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
     * Setter for the field <code>browsingMenuField</code>.
     *
     * @param browsingMenuField Solr field name to use for term browsing, or "-" / null for none
     * @should normalize field name to uppercase
     */
    public void setBrowsingMenuField(final String browsingMenuField) {
        synchronized (this) {
            String useBrowsingMenuField = browsingMenuField;
            if (useBrowsingMenuField == null || "-".equals(useBrowsingMenuField)) {
                useBrowsingMenuField = "";
            }
            try {
                // Normalize to uppercase so that lowercase URLs (e.g. from bots) match the configured Solr field names
                this.browsingMenuField = URLDecoder.decode(useBrowsingMenuField, SearchBean.URL_ENCODING).toUpperCase();
            } catch (UnsupportedEncodingException e) {
                this.browsingMenuField = useBrowsingMenuField.toUpperCase();
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
     * Getter for the field <code>browseTermList</code>.
     *
     * @return list of browse terms for the current page and field, or null if not yet loaded
     */
    public List<String> getBrowseTermList() {
        return browseTermList;
    }

    /**
     * Getter for the field <code>browseTermListEscaped</code>.
     *
     * @return list of URL-escaped browse terms for the current page and field, or null if not yet loaded
     */
    public List<String> getBrowseTermListEscaped() {
        return browseTermListEscaped;
    }

    /**
     * Getter for the field <code>browseTermHitCountList</code>.
     *
     * @return list of hit counts corresponding to each browse term in the current browse term list
     */
    public List<Long> getBrowseTermHitCountList() {
        return browseTermHitCountList;
    }

    /**
     * getPrevTermUrl.
     *
     * @return the relative URL to the previous page of the current term browse listing
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
     * getNextTermUrl.
     *
     * @return the relative URL to the next page of the current term browse listing
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
     * Getter for the field <code>availableStringFilters</code>.
     *
     * @return list of available alphabetical filter characters for the current browse field, or null if not yet loaded
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
     *
     * @return the map of available string filters keyed by browse field
     */
    Map<String, List<String>> getAvailableStringFiltersMap() {
        return availableStringFilters;
    }

    /**
     * Getter for the field <code>currentStringFilter</code>.
     *
     * @return the active alphabetical filter character(s) for the browse term list, or "-" if none is set
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
     * Setter for the field <code>currentStringFilter</code>.
     *
     * @param currentStringFilter the alphabetical filter character(s) to apply to the browse term list
     */
    public void setCurrentStringFilter(final String currentStringFilter) {
        synchronized (this) {
            String useCurrentStringFilter = currentStringFilter;
            if (Strings.CS.equals(useCurrentStringFilter, "-")) {
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
     * Getter for the field <code>filterQuery</code>.
     *
     * @return the Solr filter query restricting the browse results, or "-" if none is set
     */
    public String getFilterQuery() {
        if (StringUtils.isEmpty(filterQuery)) {
            return "-";
        }
        return filterQuery;
    }

    /**
     * Setter for the field <code>filterQuery</code>.
     *
     * @param filterQuery Solr filter query to restrict the browse results, or "-" for none
     */
    public void setFilterQuery(String filterQuery) {
        this.filterQuery = "-".equals(filterQuery) ? null : filterQuery;
    }

    /**
     * Getter for the field <code>currentPage</code>.
     *
     * @return the 1-based current page number in the browse term list
     */
    public int getCurrentPage() {
        synchronized (this) {
            return currentPage;
        }
    }

    /**
     * Setter for the field <code>currentPage</code>.
     *
     * @param currentPage the 1-based page number to display in the browse term list
     */
    public void setCurrentPage(int currentPage) {
        synchronized (this) {
            this.currentPage = currentPage;
        }
    }

    /**
     * getLastPage.
     *
     * @return a int.
     */
    public int getLastPage() {
        int hitsPerPageLocal = browsingMenuHitsPerPage;
        int answer = (int) Math.floor((double) hitsCount / hitsPerPageLocal);
        if (hitsCount % hitsPerPageLocal != 0 || answer == 0) {
            answer++;
        }

        return answer;
    }

    /**
     * isBrowsingMenuEnabled.
     *
     * @return true if the browsing menu is enabled in the configuration, false otherwise
     */
    public boolean isBrowsingMenuEnabled() {
        return DataManager.getInstance().getConfiguration().isBrowsingMenuEnabled();
    }

    /**
     * Returns the list of fields configured for term browsing to be listed in term browsing widgets.
     *
     * @param language BCP-47 language code to filter language-specific fields
     * @return List of browsing menu items
     * @should skip items that have skipInWidget true
     * @should skip items for languagespecific fields if no language was given
     * @should skip items for languagespecific fields if they dont match given language
     * @should return languagespecific fields with placeholder
     */
    public List<String> getBrowsingMenuItems(final String language) {
        String useLanguage = language;
        if (useLanguage != null) {
            useLanguage = useLanguage.toUpperCase();
        }
        List<String> ret = new ArrayList<>();
        for (BrowsingMenuFieldConfig bmfc : DataManager.getInstance().getConfiguration().getBrowsingMenuFields()) {
            if (bmfc.isSkipInWidget()) {
                continue;
            }
            if (bmfc.getField().contains(SolrConstants.MIDFIX_LANG)
                    && (useLanguage == null || !(bmfc.getField().contains(SolrConstants.MIDFIX_LANG + useLanguage)
                            || bmfc.getField().contains(SolrConstants.MIDFIX_LANG + "{}")))) {
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
}
