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
package de.intranda.digiverso.presentation.model.search;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ExpandParams;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Persistable search query.
 */
@Entity
@Table(name = "searches")
public class Search implements Serializable {

    private static final long serialVersionUID = -8968560376731964763L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(Search.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id")
    private Long id;

    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "user_input")
    private String userInput;

    /**
     * Currently selected search type (regular, advanced, timeline, ...). This property is not private so it can be altered in unit tests (the setter
     * checks the config and may prevent setting certain values).
     */
    @Column(name = "search_type")
    private int searchType = SearchHelper.SEARCH_TYPE_REGULAR;

    /** Currently selected filter for the regular search. Possible values can be configured. */
    @Column(name = "search_filter")
    private String searchFilter = SearchHelper.SEARCH_FILTER_ALL.getField();

    @Column(name = "query", nullable = false, columnDefinition = "LONGTEXT")
    private String query;

    @Column(name = "expand_query", nullable = false, columnDefinition = "LONGTEXT")
    private String expandQuery;

    @Column(name = "page", nullable = false)
    private int page;

    @Deprecated
    @Column(name = "collection")
    private String hierarchicalFacetString;

    @Column(name = "filter")
    private String facetString;

    @Column(name = "sort_field")
    private String sortString;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated", nullable = false)
    private Date dateUpdated;

    @Column(name = "last_hits_count")
    private long lastHitsCount;

    @Column(name = "new_hits_notification")
    private boolean newHitsNotification = false;

    /** Solr fields for search result sorting (usually the field from sortString and some backup fields such as ORDER and FILENAME). */
    @Transient
    private List<StringPair> sortFields = new ArrayList<>();

    @Transient
    private boolean saved = false;

    /** Total hits count for the current search. */
    @Transient
    private long hitsCount = 0;

    /** BrowseElement list for the current search result page. */
    @Transient
    private final List<SearchHit> hits = new ArrayList<>();

    /**
     * Empty constructor for JPA.
     */
    public Search() {
    }

    /**
     * 
     * @param searchType
     * @param searchFilter
     */
    public Search(int searchType, SearchFilter searchFilter) {
        this.searchType = searchType;
        if (searchFilter != null) {
            this.searchFilter = searchFilter.getField();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Search other = (Search) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        return true;
    }

    /**
     * @param facets
     * @param searchTerms
     * @param hitsPerPage
     * @param advancedSearchGroupOperator
     * @param advancedQueryGroups
     * @param locale Selected locale
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public void execute(SearchFacets facets, Map<String, Set<String>> searchTerms, int hitsPerPage, int advancedSearchGroupOperator,
            List<SearchQueryGroup> advancedQueryGroups, Locale locale) throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("execute");
        if (facets == null) {
            throw new IllegalArgumentException("facets may not be null");
        }

        String currentQuery = SearchHelper.prepareQuery(query, SearchHelper.getDocstrctWhitelistFilterSuffix());

        // Collect regular and hierarchical facet field names and combine them into one list
        List<String> hierarchicalFacetFields = DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields();
        List<String> allFacetFields = SearchHelper.getAllFacetFields(hierarchicalFacetFields);

        Map<String, String> params = SearchHelper.generateQueryParams();
        List<StructElement> luceneElements = new ArrayList<>();
        QueryResponse resp = null;
        String query = SearchHelper.buildFinalQuery(currentQuery, DataManager.getInstance().getConfiguration().isAggregateHits());

        // Apply current facets
        List<String> facetFilterQueries = facets.generateFacetFilterQueries(advancedSearchGroupOperator);
        for (String fq : facetFilterQueries) {
            logger.trace("Facet query: {}", fq);
        }

        if (hitsCount == 0) {
            logger.trace("Final main query: {}", query);
            resp = DataManager.getInstance().getSearchIndex().search(query, 0, 0, null, allFacetFields,
                    Collections.singletonList(SolrConstants.IDDOC), facetFilterQueries, params);
            if (resp != null && resp.getResults() != null) {
                hitsCount = resp.getResults().getNumFound();
                logger.trace("Pre-grouping search hits: {}", hitsCount);
                // Check for duplicate values in the GROUPFIELD facet and subtract the number from the total hits.
                for (FacetField facetField : resp.getFacetFields()) {
                    if (SolrConstants.GROUPFIELD.equals(facetField.getName())) {
                        for (Count count : facetField.getValues()) {
                            if (count.getCount() > 1) {
                                setHitsCount(hitsCount - (count.getCount() - 1));
                            }
                        }
                    }
                }
                logger.debug("Total search hits: {}", hitsCount);
            }
        }
        if (hitsCount > 0 && resp != null) {
            // Collect available facets
            String language = null;
            if (locale != null) {
                language = locale.getLanguage().toUpperCase();
            }
            for (FacetField facetField : resp.getFacetFields()) {
                if (SolrConstants.GROUPFIELD.equals(facetField.getName()) || facetField.getValues() == null) {
                    continue;
                }
                // Skip language-specific facet fields if they don't match the given language
                if (facetField.getName().contains(SolrConstants._LANG_)
                        && (language == null || !facetField.getName().contains(SolrConstants._LANG_ + language))) {
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
                // Use non-FACET_ field names outside of the actual faceting query
                String fieldName = SearchHelper.defacetifyField(facetField.getName());
                facets.getAvailableFacets().put(fieldName,
                        FacetItem.generateFilterLinkList(fieldName, facetResult, hierarchicalFacetFields.contains(fieldName), locale));
            }

            int lastPage = getLastPage(hitsPerPage);
            if (page > lastPage) {
                page = lastPage;
                logger.trace(" page = getLastPage()");
            }

            // Hits for the current page
            int from = (page - 1) * hitsPerPage;

            if (StringUtils.isNotEmpty(expandQuery)) {
                logger.trace("Expand query: {}", expandQuery);
                params.put(ExpandParams.EXPAND, "true");
                params.put(ExpandParams.EXPAND_Q, expandQuery);
                params.put(ExpandParams.EXPAND_FIELD, SolrConstants.PI_TOPSTRUCT);
                params.put(ExpandParams.EXPAND_ROWS, String.valueOf(SolrSearchIndex.MAX_HITS));
                params.put(ExpandParams.EXPAND_SORT, SolrConstants.ORDER + " asc");
                params.put(ExpandParams.EXPAND_FQ, ""); // The main filter query may not apply to the expand query to produce child hits
            }

            List<SearchHit> hits = DataManager.getInstance().getConfiguration().isAggregateHits()
                    ? SearchHelper.searchWithAggregation(query, from, hitsPerPage, sortFields, null, facetFilterQueries, params, searchTerms, null,
                            BeanUtils.getLocale())
                    : SearchHelper.searchWithFulltext(query, from, hitsPerPage, sortFields, null, facetFilterQueries, params, searchTerms, null,
                            BeanUtils.getLocale(), BeanUtils.getRequest());
            this.hits.addAll(hits);
        }
    }

    /**
     * Constructs a search URL using the query parameters contained in this object.
     *
     * @return
     * @throws UnsupportedEncodingException
     */
    public String getUrl() throws UnsupportedEncodingException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        sbUrl.append('/').append(PageType.search.getName());
        sbUrl.append('/').append(
                (StringUtils.isNotEmpty(hierarchicalFacetString) ? URLEncoder.encode(hierarchicalFacetString, SearchBean.URL_ENCODING) : "-"));
        sbUrl.append('/').append(StringUtils.isNotEmpty(query) ? URLEncoder.encode(query, SearchBean.URL_ENCODING) : "-").append('/').append(page);
        sbUrl.append('/').append((StringUtils.isNotEmpty(sortString) ? sortString : "-"));
        sbUrl.append('/').append((StringUtils.isNotEmpty(facetString) ? URLEncoder.encode(facetString, SearchBean.URL_ENCODING) : "-")).append('/');
        return sbUrl.toString();
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
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the userInput
     */
    public String getUserInput() {
        return userInput;
    }

    /**
     * @param userInput the userInput to set
     */
    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    /**
     * @return the searchType
     */
    public int getSearchType() {
        return searchType;
    }

    /**
     * @param searchType the searchType to set
     */
    public void setSearchType(int searchType) {
        this.searchType = searchType;
    }

    /**
     * @return the searchFilter
     */
    public String getSearchFilter() {
        return searchFilter;
    }

    /**
     * @param searchFilter the searchFilter to set
     */
    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return the expandQuery
     */
    public String getExpandQuery() {
        return expandQuery;
    }

    /**
     * @param expandQuery the expandQuery to set
     */
    public void setExpandQuery(String expandQuery) {
        this.expandQuery = expandQuery;
    }

    /**
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * @return the hierarchicalFacetString
     */
    @Deprecated
    public String getHierarchicalFacetString() {
        return hierarchicalFacetString;
    }

    /**
     * @param hierarchicalFacetString the hierarchicalFacetString to set
     */
    @Deprecated
    public void setHierarchicalFacetString(String hierarchicalFacetString) {
        this.hierarchicalFacetString = hierarchicalFacetString;
    }

    /**
     * @return the facetString
     */
    public String getFacetString() {
        return facetString;
    }

    /**
     * @param facetString the facetString to set
     */
    public void setFacetString(String facetString) {
        this.facetString = facetString;
    }

    /**
     * @return the sortString
     */
    public String getSortString() {
        return sortString;
    }

    /**
     * @param sortString the sortString to set
     */
    public void setSortString(String sortString) {
        this.sortString = sortString;
        sortFields = SearchHelper.parseSortString(this.sortString, null);
    }

    /**
     * @return the sortFields
     */
    public List<StringPair> getSortFields() {
        return sortFields;
    }

    /**
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the lastHitsCount
     */
    public long getLastHitsCount() {
        return lastHitsCount;
    }

    /**
     * @param lastHitsCount the lastHitsCount to set
     */
    public void setLastHitsCount(long lastHitsCount) {
        this.lastHitsCount = lastHitsCount;
    }

    /**
     * @return the newHitsNotification
     */
    public boolean isNewHitsNotification() {
        return newHitsNotification;
    }

    /**
     * @param newHitsNotification the newHitsNotification to set
     */
    public void setNewHitsNotification(boolean newHitsNotification) {
        this.newHitsNotification = newHitsNotification;
    }

    /**
     * @return the saved
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * @param saved the saved to set
     */
    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    /**
     * @return the hitsCount
     */
    public long getHitsCount() {
        return hitsCount;
    }

    /**
     * @param hitsCount the hitsCount to set
     */
    public void setHitsCount(long hitsCount) {
        this.hitsCount = hitsCount;
    }

    /**
     * @return the hits
     */
    public List<SearchHit> getHits() {
        logger.trace("hits: {}", hits.size());
        return hits;
    }

    /**
     * 
     * @param hitsPerPage
     * @return
     */
    public int getLastPage(int hitsPerPage) {
        int answer = 0;
        int hitsPerPageLocal = hitsPerPage;
        answer = new Double(Math.floor(hitsCount / hitsPerPageLocal)).intValue();
        if (hitsCount % hitsPerPageLocal != 0 || answer == 0) {
            answer++;
        }

        return answer;
    }

    public void toggleNotifications() {
        this.newHitsNotification = !this.newHitsNotification;
    }
}
