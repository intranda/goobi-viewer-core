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

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import de.intranda.digiverso.presentation.controller.AlphanumCollatorComparator;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.BrowseDcElement;
import de.intranda.digiverso.presentation.model.viewer.BrowseTerm;
import de.intranda.digiverso.presentation.model.viewer.BrowseTerm.BrowseTermRawComparator;
import de.intranda.digiverso.presentation.model.viewer.BrowsingMenuFieldConfig;
import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.CollectionView.BrowseDataProvider;
import de.intranda.digiverso.presentation.model.viewer.LabeledLink;

/**
 * This bean provides the data for collection and term browsing.
 */
@Named
@SessionScoped
public class BrowseBean implements Serializable {

    private static final long serialVersionUID = 7613678633319477862L;

    private static final Logger logger = LoggerFactory.getLogger(BrowseBean.class);

    @Inject
    private NavigationHelper navigationHelper;
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
    private String collectionField;

    /** Empty constructor. */
    public BrowseBean() {
        // the emptiness inside
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

    public void resetAllLists() {
        for (String field : collections.keySet()) {
            collections.get(field).resetCollectionList();
        }
    }

    public void resetDcList() {
        logger.trace("resetDcList");
        resetList(SolrConstants.DC);
    }

    public void resetList(String field) {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (collections.get(field) != null) {
            collections.get(field).resetCollectionList();
        }
    }

    /**
     * @return the dcList (Collections)
     * @throws IndexUnreachableException
     */
    public List<BrowseDcElement> getDcList() throws IndexUnreachableException {
        return getList(SolrConstants.DC);
    }

    public List<BrowseDcElement> getList(String field) throws IndexUnreachableException {
        return getList(field, -1);
    }

    public List<BrowseDcElement> getList(String field, int depth) throws IndexUnreachableException {
        if (collections.get(field) == null) {
            initializeCollection(field, field);
            populateCollection(field);
        }
        if (collections.get(field) != null) {
            CollectionView collection = collections.get(field);
            collection.expandAll(depth);
            collection.calculateVisibleDcElements();
            return new ArrayList<>(collection.getVisibleDcElements());
        }

        return Collections.emptyList();
    }

    /**
     * @throws IndexUnreachableException
     *
     */
    public void populateCollection(String field) throws IndexUnreachableException {
        if (collections.containsKey(field)) {
            collections.get(field).populateCollectionList();
        }
    }

    @Deprecated
    public List<BrowseDcElement> getVisibleDcList() {
        logger.debug("getVisibleDcList");
        if (!collections.containsKey(SolrConstants.DC)) {
            initializeDCCollection();
        }
        return new ArrayList<>(collections.get(SolrConstants.DC).getVisibleDcElements());
    }

    /**
     * Populates <code>visibledcList</code> with elements to be currently show in the UI. Prior to using this method, <code>dcList</code> must be
     * sorted and each <code>BrowseDcElement.hasSubElements</code> must be set correctly.
     *
     * @throws IndexUnreachableException
     */
    @Deprecated
    public void calculateVisibleDcElements() throws IndexUnreachableException {
        if (!collections.containsKey(SolrConstants.DC)) {
            initializeDCCollection();
        }
    }

    public String getCollectionToExpand() {
        return collectionToExpand;
    }

    public void setCollectionToExpand(String collectionToExpand) {
        synchronized (this) {
            this.collectionToExpand = collectionToExpand;
            this.topVisibleCollection = collectionToExpand;
        }
    }

    /**
     * @return the topVisibleCollecion
     */
    public String getTopVisibleCollection() {
        if (topVisibleCollection == null && collectionToExpand != null) {
            return collectionToExpand;
        }
        return topVisibleCollection;
    }

    /**
     * @param topVisibleCollecion the topVisibleCollecion to set
     */
    public void setTopVisibleCollection(String topVisibleCollecion) {
        this.topVisibleCollection = topVisibleCollecion;
    }

    /**
     * Use this method of a certain collections needs to be expanded via URL.
     *
     * @throws IndexUnreachableException
     */
    public void expandCollection(int levels) throws IndexUnreachableException {
        expandCollection(SolrConstants.DC, SolrConstants.FACET_DC, levels);
    }

    public void expandCollection(String collectionField, String facetField, int levels) throws IndexUnreachableException {
        synchronized (this) {
            initializeCollection(collectionField, facetField);
            collections.get(collectionField).setBaseLevels(levels);
            collections.get(collectionField).setBaseElementName(getCollectionToExpand());
            collections.get(collectionField).setTopVisibleElement(getTopVisibleCollection());
            collections.get(collectionField).populateCollectionList();
        }
    }

    /**
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String searchTerms() throws PresentationException, IndexUnreachableException {
        synchronized (this) {
            logger.trace("searchTerms");
            updateBreadcrumbsWithCurrentUrl("browseTitle", NavigationHelper.WEIGHT_SEARCH_TERMS);
            if (searchBean != null) {
                searchBean.setSearchString("");
                searchBean.resetSearchParameters(true);
            }
            hitsCount = 0;

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
                Messages.error(Helper.getTranslation("browse_errFieldNotConfigured", null).replace("{0}", browsingMenuField));
                return "searchTermList";
            }
            if (StringUtils.isEmpty(currentStringFilter) || availableStringFilters.get(browsingMenuField) == null) {
                terms = SearchHelper.getFilteredTerms(currentBmfc, "", filterQuery, new BrowseTermRawComparator(),
                        DataManager.getInstance().getConfiguration().isAggregateHits());

                // Populate the list of available starting characters with ones that actually exist in the complete terms list
                if (availableStringFilters.get(browsingMenuField) == null || filterQuery != null) {
                    logger.debug("Populating search term filters for field '{}'...", browsingMenuField);
                    availableStringFilters.put(browsingMenuField, new ArrayList<String>());
                    for (BrowseTerm term : terms) {
                        String firstChar;
                        if (StringUtils.isNotEmpty(term.getSortTerm())) {
                            firstChar = term.getSortTerm().substring(0, 1).toUpperCase();
                        } else {
                            firstChar = term.getTerm().substring(0, 1).toUpperCase();
                        }
                        // logger.debug(term.getTerm() + ": " + firstChar);
                        if (!availableStringFilters.get(browsingMenuField).contains(firstChar) && !"-".equals(firstChar)) {
                            availableStringFilters.get(browsingMenuField).add(firstChar);
                        }
                    }
                }

                // Sort filters
                Locale locale = null;
                NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
                if (navigationHelper != null) {
                    locale = navigationHelper.getLocale();
                } else {
                    locale = Locale.GERMAN;
                }
                Collections.sort(availableStringFilters.get(browsingMenuField), new AlphanumCollatorComparator(Collator.getInstance(locale)));
                // logger.debug(availableStringFilters.toString());
            }

            // Get the terms again, this time using the requested filter. The search over all terms the first time is necessary to get the list of available filters.
            if (StringUtils.isNotEmpty(currentStringFilter)) {
                terms = SearchHelper.getFilteredTerms(currentBmfc, currentStringFilter, filterQuery, new BrowseTermRawComparator(),
                        DataManager.getInstance().getConfiguration().isAggregateHits());
            }
            hitsCount = terms.size();
            if (hitsCount > 0) {
                if (currentPage > getLastPage()) {
                    currentPage = getLastPage();
                }

                int start = (currentPage - 1) * browsingMenuHitsPerPage;
                int end = currentPage * browsingMenuHitsPerPage;
                if (end > terms.size()) {
                    end = terms.size();
                }

                browseTermList = new ArrayList<>(end - start);
                browseTermHitCountList = new ArrayList<>(browseTermList.size());
                for (int i = start; i < end; ++i) {
                    browseTermList.add(terms.get(i).getTerm().intern());
                    browseTermHitCountList.add(terms.get(i).getHitCount());
                }

                browseTermListEscaped = new ArrayList<>(browseTermList.size());
                // URL encode all terms
                for (String s : browseTermList) {
                    // Escape characters such as quotation marks
                    String term = ClientUtils.escapeQueryChars(s);
                    term = BeanUtils.escapeCriticalUrlChracters(term);
                    try {
                        term = URLEncoder.encode(term, SearchBean.URL_ENCODING);
                    } catch (UnsupportedEncodingException e) {
                        logger.error(e.getMessage());
                    }
                    browseTermListEscaped.add(term.intern());
                }
            } else {
                resetTerms();
            }

            return "searchTermList";
        }
    }

    /**
     * @return the browsingMenuField
     */
    public String getBrowsingMenuField() {
        if (StringUtils.isEmpty(browsingMenuField)) {
            return "-";
        }

        return browsingMenuField;
    }

    /**
     * @param browsingMenuField the browsingMenuField to set
     */
    public void setBrowsingMenuField(String browsingMenuField) {
        synchronized (this) {
            if ("-".equals(browsingMenuField)) {
                browsingMenuField = "";
            }
            try {
                this.browsingMenuField = URLDecoder.decode(browsingMenuField, SearchBean.URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                this.browsingMenuField = browsingMenuField;
            }
        }
    }

    /**
     * @return the browseTermList
     */
    public List<String> getBrowseTermList() {
        return browseTermList;
    }

    /**
     * @return the browseTermListEscaped
     */
    public List<String> getBrowseTermListEscaped() {
        return browseTermListEscaped;
    }

    /**
     * @return the browseTermHitCountList
     */
    public List<Long> getBrowseTermHitCountList() {
        return browseTermHitCountList;
    }

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
     * @return the availableStringFilters
     */
    public List<String> getAvailableStringFilters() {
        return availableStringFilters.get(browsingMenuField);
    }

    /**
     * @return the currentStringFilter
     */
    public String getCurrentStringFilter() {
        if (StringUtils.isEmpty(currentStringFilter)) {
            return "-";
        }
        return currentStringFilter;
    }

    /**
     * @param currentStringFilter the currentStringFilter to set
     */
    public void setCurrentStringFilter(String currentStringFilter) {
        synchronized (this) {
            if (StringUtils.equals(currentStringFilter, "-")) {
                currentStringFilter = "";
            }
            try {
                this.currentStringFilter = URLDecoder.decode(currentStringFilter, SearchBean.URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                this.currentStringFilter = currentStringFilter;
            }
        }
    }

    /**
     * @return the filterQuery
     */
    public String getFilterQuery() {
        if (StringUtils.isEmpty(filterQuery)) {
            return "-";
        }
        return filterQuery;
    }

    /**
     * @param filterQuery the filterQuery to set
     */
    public void setFilterQuery(String filterQuery) {
        this.filterQuery = "-".equals(filterQuery) ? null : filterQuery;
    }

    public int getCurrentPageResetFilter() {
        return currentPage;
    }

    /**
     * This is used when a term search query doesn't contain a filter value. In this case, the value is reset.
     *
     * @param currentPage
     * @deprecated Reset the currentStringFilter value in the PrettyFaces mapping
     */
    @Deprecated
    public void setCurrentPageResetFilter(int currentPage) {
        synchronized (this) {
            currentStringFilter = null;
            this.currentPage = currentPage;
        }
    }

    /**
     * @return the currentPage
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * @param currentPage the currentPage to set
     */
    public void setCurrentPage(int currentPage) {
        synchronized (this) {
            this.currentPage = currentPage;
        }
    }

    public int getLastPage() {
        int hitsPerPageLocal = browsingMenuHitsPerPage;
        int answer = new Double(Math.floor(hitsCount / hitsPerPageLocal)).intValue();
        if (hitsCount % hitsPerPageLocal != 0 || answer == 0) {
            answer++;
        }

        //        logger.trace(hitsCount + "/" + hitsPerPageLocal + "=" + answer);
        return answer;
    }

    public boolean isBrowsingMenuEnabled() {
        return DataManager.getInstance().getConfiguration().isBrowsingMenuEnabled();
    }

    /**
     * 
     * @param language
     * @return List of browsing menu items
     * @should skip items for language-specific fields if no language was given
     * @should skip items for language-specific fields if they don't match given language
     */
    public List<String> getBrowsingMenuItems(String language) {
        if (language != null) {
            language = language.toUpperCase();
        }
        List<String> ret = new ArrayList<>();
        for (BrowsingMenuFieldConfig bmfc : DataManager.getInstance().getConfiguration().getBrowsingMenuFields()) {
            if (bmfc.getField().contains(SolrConstants._LANG_) && (language == null || !bmfc.getField().contains(SolrConstants._LANG_ + language))) {
                logger.trace("Skipped {}", bmfc.getField());
                continue;
            }
            ret.add(bmfc.getField());
        }
        return ret;
    }

    public String getTargetCollection() {
        return targetCollection;
    }

    public void setTargetCollection(String targetCollection) {
        this.targetCollection = targetCollection;
    }

    public String openWorkInTargetCollection() throws IndexUnreachableException, PresentationException {
        if (StringUtils.isBlank(getTargetCollection())) {
            return null;
        }
        String url = SearchHelper.getFirstWorkUrlWithFieldValue(SolrConstants.DC, getTargetCollection(), true, true, true, true,
                DataManager.getInstance().getConfiguration().getSplittingCharacter(), BeanUtils.getLocale());
        url = url.replace("http://localhost:8082/viewer/", "");
        return "pretty:" + url;
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

    public CollectionView getDcCollection() {
        return getCollection(SolrConstants.DC);
    }

    public CollectionView getCollection(String field) {
        return collections.get(field);
    }

    public void initializeDCCollection() {
        initializeCollection(SolrConstants.DC, SolrConstants.FACET_DC);
    }

    /**
     * Adds a CollectionView object for the given field to the map and populates its values.
     *
     * @param collectionField
     * @param facetField
     * @param sortField
     */
    public void initializeCollection(final String collectionField, final String facetField) {
        logger.trace("initializeCollection: {}", collectionField);
        collections.put(collectionField, new CollectionView(collectionField, new BrowseDataProvider() {

            @Override
            public Map<String, Long> getData() throws IndexUnreachableException {
                Map<String, Long> dcStrings = SearchHelper.findAllCollectionsFromField(collectionField, facetField, true, true, true, true);
                return dcStrings;
            }
        }));
    }

    /**
     * @return the collectionField
     */
    public String getCollectionField() {
        return collectionField;
    }

    /**
     * @param collectionField the collectionField to set
     */
    public void setCollectionField(String collectionField) {
        this.collectionField = collectionField;
    }
}
