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
package io.goobi.viewer.model.search;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.maps.IArea;
import io.goobi.viewer.model.maps.Location;
import io.goobi.viewer.model.maps.Point;
import io.goobi.viewer.model.maps.Polygon;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

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

    /** Optional custom filter query. */
    @Column(name = "custom_filter_query", columnDefinition = "LONGTEXT")
    private String customFilterQuery;

    @Column(name = "page", nullable = false)
    private int page;
    //
    //    @Deprecated
    //    @Column(name = "collection")
    //    private String hierarchicalFacetString;

    @Column(name = "filter")
    private String facetString;

    @Column(name = "sort_field")
    private String sortString;

    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;

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
     * List of geo-locations found by the last search
     */
    @Transient
    private List<Location> hitLocationList = new ArrayList<>();

    /**
     * Empty constructor for JPA.
     */
    public Search() {
    }

    /**
     * cloning constructor. Creates a new search in a state as it might be loaded from database, i.e. without any transient fields set. In particular
     * with empty {@link #hits}
     * 
     * @param blueprint
     */
    public Search(Search blueprint) {
        this.id = blueprint.id;
        this.owner = blueprint.owner;
        this.name = blueprint.name;
        this.userInput = blueprint.userInput;
        this.searchType = blueprint.searchType;
        this.query = blueprint.query;
        this.expandQuery = blueprint.expandQuery;
        this.searchFilter = blueprint.searchFilter;
        this.page = blueprint.page;
        this.facetString = blueprint.facetString;
        this.sortString = blueprint.sortString;
        this.sortFields = SearchHelper.parseSortString(blueprint.sortString, null);
        this.dateUpdated = blueprint.dateUpdated;
        this.lastHitsCount = blueprint.lastHitsCount;
        this.newHitsNotification = blueprint.newHitsNotification;

    }

    /**
     * <p>
     * Constructor for Search.
     * </p>
     *
     * @param searchType a int.
     * @param searchFilter a {@link io.goobi.viewer.model.search.SearchFilter} object.
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
    /** {@inheritDoc} */
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
    /** {@inheritDoc} */
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
     * <p>
     * execute.
     * </p>
     *
     * @param facets a {@link io.goobi.viewer.model.search.SearchFacets} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param hitsPerPage a int.
     * @param advancedSearchGroupOperator a int.
     * @param locale Selected locale
     * @param aggregateHits
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void execute(SearchFacets facets, Map<String, Set<String>> searchTerms, int hitsPerPage, int advancedSearchGroupOperator, Locale locale,
            boolean aggregateHits) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        execute(facets, searchTerms, hitsPerPage, advancedSearchGroupOperator, locale, aggregateHits, false);
    }

    /**
     * <p>
     * execute.
     * </p>
     *
     * @param facets a {@link io.goobi.viewer.model.search.SearchFacets} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param hitsPerPage a int.
     * @param advancedSearchGroupOperator a int.
     * @param locale Selected locale
     * @param aggregateHits
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void execute(SearchFacets facets, Map<String, Set<String>> searchTerms, int hitsPerPage, int advancedSearchGroupOperator, Locale locale,
            boolean aggregateHits, boolean keepSolrDoc)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("execute");
        if (facets == null) {
            throw new IllegalArgumentException("facets may not be null");
        }
        String currentQuery = SearchHelper.prepareQuery(this.query);

        // Collect regular and hierarchical facet field names and combine them into one list
        List<String> hierarchicalFacetFields = DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields();
        List<String> allFacetFields = SearchHelper.getAllFacetFields(hierarchicalFacetFields);

        Map<String, String> params = SearchHelper.generateQueryParams();
        QueryResponse resp = null;
        String query = SearchHelper.buildFinalQuery(currentQuery, aggregateHits);

        // Apply current facets
        List<String> activeFacetFilterQueries = facets.generateFacetFilterQueries(advancedSearchGroupOperator, true, true);
        String subElementQueryFilterSuffix = facets.generateSubElementFacetFilterQuery();
        if (StringUtils.isNotEmpty(subElementQueryFilterSuffix)) {
            subElementQueryFilterSuffix = " +(" + subElementQueryFilterSuffix + ")";
        }
        if (logger.isTraceEnabled()) {
            for (String fq : activeFacetFilterQueries) {
                logger.debug("Facet query: {}", fq);
            }
            logger.debug("Subelement facet query: {}", subElementQueryFilterSuffix);
        }

        String finalQuery = query + subElementQueryFilterSuffix;
        if (hitsCount == 0) {
            logger.debug("Final main query: {}", finalQuery);

            // Search without range facet queries to determine absolute slider range
            List<String> rangeFacetFields = DataManager.getInstance().getConfiguration().getRangeFacetFields();
            List<String> nonRangeFacetFilterQueries = facets.generateFacetFilterQueries(advancedSearchGroupOperator, false, true);

            // Add custom filter query
            if (StringUtils.isNotEmpty(customFilterQuery)) {
                nonRangeFacetFilterQueries.add(customFilterQuery);
                activeFacetFilterQueries.add(customFilterQuery);
            }

            resp = DataManager.getInstance()
                    .getSearchIndex()
                    .search(finalQuery, 0, 0, null, rangeFacetFields, Collections.singletonList(SolrConstants.IDDOC), nonRangeFacetFilterQueries,
                            params);
            if (resp != null && resp.getFacetFields() != null) {
                for (FacetField facetField : resp.getFacetFields()) {
                    if (rangeFacetFields.contains(facetField.getName())) {
                        Map<String, Long> counts = new HashMap<>();
                        List<String> values = new ArrayList<>();
                        for (Count count : facetField.getValues()) {
                            if (count.getCount() > 0) {
                                counts.put(count.getName(), count.getCount());
                                values.add(count.getName());
                            }
                        }
                        if (!values.isEmpty()) {
                            facets.populateAbsoluteMinMaxValuesForField(facetField.getName(), values);
                        }
                    }
                }
            }

            // Extra search for child element facet values
            if (DataManager.getInstance().getConfiguration().isAggregateHits() && !facets.getConfiguredSubelementFacetFields().isEmpty()) {
                String extraQuery =
                        new StringBuilder().append(SearchHelper.buildFinalQuery(currentQuery, false)).append(subElementQueryFilterSuffix).toString();
                logger.trace("extra query: {}", extraQuery);
                resp = DataManager.getInstance()
                        .getSearchIndex()
                        .search(extraQuery, 0, 0, null, facets.getConfiguredSubelementFacetFields(), Collections.singletonList(SolrConstants.IDDOC),
                                activeFacetFilterQueries, params);
                if (resp != null && resp.getFacetFields() != null) {
                    //                    logger.trace("hits: {}", resp.getResults().getNumFound());
                    for (FacetField facetField : resp.getFacetFields()) {
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
                        facets.getAvailableFacets()
                                .put(fieldName, FacetItem.generateFilterLinkList(fieldName, facetResult, hierarchicalFacetFields.contains(fieldName),
                                        locale, facets.getLabelMap()));
                        //                        allFacetFields.remove("FACET_" + SolrConstants.DOCSTRCT_SUB);
                        allFacetFields.remove(facetField.getName());
                    }
                }

            }

            List<String> fieldList = Arrays.asList(SolrConstants.IDDOC);
            int maxResults = 0;
            if (facets.getGeoFacetting().isActive()) {
                fieldList = Arrays.asList(SolrConstants.IDDOC, SolrConstants.WKT_COORDS, SolrConstants.LABEL, SolrConstants.PI_TOPSTRUCT,
                        SolrConstants.ISANCHOR, SolrConstants.DOCSTRCT, SolrConstants.DOCTYPE, SolrConstants.BOOL_IMAGEAVAILABLE,
                        SolrConstants.MIMETYPE);
                maxResults = Integer.MAX_VALUE;
            }

            // Actual search
            resp = DataManager.getInstance()
                    .getSearchIndex()
                    .search(finalQuery, 0, maxResults, null, allFacetFields, fieldList, activeFacetFilterQueries, params);
            if (resp.getResults() != null) {
                Map expanded = resp.getExpandedResults();
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
                if (facets.getGeoFacetting().isActive()) {
                    this.hitLocationList = getLocations(facets.getGeoFacetting().getField(), resp.getResults());
                    this.hitLocationList.sort((l1, l2) -> Double.compare(l2.getArea().getDiameter(), l1.getArea().getDiameter()));
                }
                logger.debug("Total search hits: {}", hitsCount);
            }
        }

        if (hitsCount == 0) {
            return;
        }

        // Collect available facets
        //        String language = null;
        //        if (locale != null) {
        //            language = locale.getLanguage().toUpperCase();
        //        }
        for (FacetField facetField : resp.getFacetFields()) {
            if (SolrConstants.GROUPFIELD.equals(facetField.getName()) || facetField.getValues() == null) {
                continue;
            }
            //                // Skip top element docstrct faceting if sub-element docstrct faceting is active
            //                if (("FACET_" + SolrConstants.DOCSTRCT).equals(facetField.getName()) && subElementQueryFilterSuffix.contains(facetField.getName())) {
            //                    continue;
            //                }
            //                // Skip language-specific facet fields if they don't match the given language
            //                if (facetField.getName().contains(SolrConstants._LANG_)
            //                        && (language == null || !facetField.getName().contains(SolrConstants._LANG_ + language))) {
            //                    continue;
            //                }
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
            facets.getAvailableFacets()
                    .put(fieldName,
                            FacetItem.generateFilterLinkList(fieldName, facetResult, hierarchicalFacetFields.contains(fieldName), locale,
                                    facets.getLabelMap()));
        }

        int lastPage = getLastPage(hitsPerPage);
        if (page > lastPage) {
            page = lastPage;
            logger.trace(" page = getLastPage()");
        }

        // Hits for the current page
        int from = (page - 1) * hitsPerPage;

        // Search for child hits only if initial search query is not empty (empty query means collection listing)
        if (StringUtils.isNotEmpty(expandQuery)) {
            String useExpandQuery = expandQuery + subElementQueryFilterSuffix;
            if (StringUtils.isNotEmpty(useExpandQuery)) {
                logger.trace("Expand query: {}", useExpandQuery);
                params.putAll(SearchHelper.getExpandQueryParams(useExpandQuery));
            }
        }

        List<StringPair> useSortFields = getAllSortFields();
        List<SearchHit> hits = aggregateHits
                ? SearchHelper.searchWithAggregation(finalQuery, from, hitsPerPage, useSortFields, null, activeFacetFilterQueries, params,
                        searchTerms, null, BeanUtils.getLocale(), keepSolrDoc)
                : SearchHelper.searchWithFulltext(finalQuery, from, hitsPerPage, useSortFields, null, activeFacetFilterQueries, params,
                        searchTerms, null, BeanUtils.getLocale(), BeanUtils.getRequest(), keepSolrDoc);
        this.hits.addAll(hits);
    }

    private List<Location> getLocations(String solrField, SolrDocumentList results) {
        List<Location> locations = new ArrayList<>();
        for (SolrDocument doc : results) {
            try {
                String label = (String) doc.getFieldValue(SolrConstants.LABEL);
                String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
                String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
                String mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
                boolean anchorOrGroup = SolrTools.isAnchor(doc) || SolrTools.isGroup(doc);
                Boolean hasImages = (Boolean) doc.getFieldValue(SolrConstants.BOOL_IMAGEAVAILABLE);
                locations.addAll(getLocations(doc.getFieldValue(solrField))
                        .stream()
                        .map(p -> new Location(p, label,
                                Location.getRecordURI(pi, PageType.determinePageType(docStructType, mimeType, anchorOrGroup, hasImages, false))))
                        .collect(Collectors.toList()));
            } catch (IllegalArgumentException e) {
                System.out.println("\"" + doc.getFieldValue(solrField) + "\"");
                logger.error("Error parsing field {} of document {}: {}", solrField, doc.get("IDDOC"), e.getMessage());
                logger.error(e.toString(), e);
            }
        }
        return locations;
    }

    protected static List<IArea> getLocations(Object o) {
        List<IArea> locs = new ArrayList<>();
        if (o == null) {
            return locs;
        } else if (o instanceof List) {
            for (int i = 0; i < ((List) o).size(); i++) {
                locs.addAll(getLocations(((List) o).get(i)));
            }
            return locs;
        } else if (o instanceof String) {
            String s = (String) o;
            Matcher polygonMatcher = Pattern.compile("POLYGON\\(\\(([0-9.-]+\\s[0-9.-]+(?:,\\s)?)+\\)\\)").matcher(s);
            while (polygonMatcher.find()) {
                String match = polygonMatcher.group();
                locs.add(new Polygon(getPoints(match)));
                s = s.replace(match, "");
                polygonMatcher = Pattern.compile("POLYGON\\(\\(([0-9.-]+\\s[0-9.-]+(?:,\\s)?)+\\)\\)").matcher(s);
            }
            if (StringUtils.isNotBlank(s)) {
                locs.addAll(Arrays.asList(getPoints(s)).stream().map(p -> new Point(p[0], p[1])).collect(Collectors.toList()));
            }
            return locs;
        }
        throw new IllegalArgumentException(String.format("Unable to parse %s of type %s as location", o.toString(), o.getClass()));
    }

    protected static double[][] getPoints(String value) {
        List<double[]> points = new ArrayList<>();
        Matcher matcher = Pattern.compile("([\\d\\.\\-]+)\\s([\\d\\.\\-]+)").matcher(value);
        while (matcher.find() && matcher.groupCount() == 2) {
            points.add(parsePoint(matcher.group(1), matcher.group(2)));
        }
        return points.toArray(new double[points.size()][2]);
    }

    protected static double[] parsePoint(Object x, Object y) {
        if (x instanceof Number) {
            double[] loc = new double[2];
            loc[0] = ((Number) x).doubleValue();
            loc[1] = ((Number) y).doubleValue();
            return loc;
        } else if (x instanceof String) {
            try {
                double[] loc = new double[2];
                loc[0] = Double.parseDouble((String) x);
                loc[1] = Double.parseDouble((String) y);
                return loc;
            } catch (NumberFormatException e) {
            }
        }
        throw new IllegalArgumentException(String.format("Unable to parse objects %s, %s to double array", x, y));
    }

    /**
     * Constructs a search URL using the query parameters contained in this object.
     *
     * @return a {@link java.lang.String} object.
     * @throws java.io.UnsupportedEncodingException if any.
     */
    public String getUrl() throws UnsupportedEncodingException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        sbUrl.append('/').append(PageType.search.getName());
        sbUrl.append("/-");
        sbUrl.append('/').append(StringUtils.isNotEmpty(query) ? URLEncoder.encode(query, SearchBean.URL_ENCODING) : "-").append('/').append(page);
        sbUrl.append('/').append((StringUtils.isNotEmpty(sortString) ? sortString : "-"));
        sbUrl.append('/').append((StringUtils.isNotEmpty(facetString) ? URLEncoder.encode(facetString, SearchBean.URL_ENCODING) : "-")).append('/');
        return sbUrl.toString();
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
     * Getter for the field <code>owner</code>.
     * </p>
     *
     * @return the owner
     */
    public User getOwner() {
        return owner;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     *
     * @param owner the owner to set
     */
    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * <p>
     * Getter for the field <code>userInput</code>.
     * </p>
     *
     * @return the userInput
     */
    public String getUserInput() {
        return userInput;
    }

    /**
     * <p>
     * Setter for the field <code>userInput</code>.
     * </p>
     *
     * @param userInput the userInput to set
     */
    public void setUserInput(String userInput) {
        this.userInput = userInput;
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
     * Getter for the field <code>searchFilter</code>.
     * </p>
     *
     * @return the searchFilter
     */
    public String getSearchFilter() {
        return searchFilter;
    }

    /**
     * <p>
     * Setter for the field <code>searchFilter</code>.
     * </p>
     *
     * @param searchFilter the searchFilter to set
     */
    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    /**
     * <p>
     * Getter for the field <code>query</code>.
     * </p>
     *
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * <p>
     * Setter for the field <code>query</code>.
     * </p>
     *
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * <p>
     * Getter for the field <code>expandQuery</code>.
     * </p>
     *
     * @return the expandQuery
     */
    public String getExpandQuery() {
        return expandQuery;
    }

    /**
     * <p>
     * Setter for the field <code>expandQuery</code>.
     * </p>
     *
     * @param expandQuery the expandQuery to set
     */
    public void setExpandQuery(String expandQuery) {
        this.expandQuery = expandQuery;
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

    /**
     * <p>
     * Getter for the field <code>page</code>.
     * </p>
     *
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * <p>
     * Setter for the field <code>page</code>.
     * </p>
     *
     * @param page the page to set
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * <p>
     * Getter for the field <code>facetString</code>.
     * </p>
     *
     * @return the facetString
     */
    public String getFacetString() {
        return facetString;
    }

    /**
     * <p>
     * Setter for the field <code>facetString</code>.
     * </p>
     *
     * @param facetString the facetString to set
     */
    public void setFacetString(String facetString) {
        this.facetString = facetString;
    }

    /**
     * <p>
     * Getter for the field <code>sortString</code>.
     * </p>
     *
     * @return the sortString
     */
    public String getSortString() {
        return sortString;
    }

    /**
     * <p>
     * Setter for the field <code>sortString</code>.
     * </p>
     *
     * @param sortString the sortString to set
     */
    public void setSortString(String sortString) {
        this.sortString = sortString;
        sortFields = SearchHelper.parseSortString(this.sortString, null);
    }

    /**
     * Returns a list of currently selected sort fields with any configured static sort fields.
     * 
     * @return A list of both static and selected fields
     * @should return all fields
     */
    public List<StringPair> getAllSortFields() {
        List<String> staticSortFields = DataManager.getInstance().getConfiguration().getStaticSortFields();
        List<StringPair> ret = new ArrayList<>(staticSortFields.size() + sortFields.size());
        if (!staticSortFields.isEmpty()) {
            for (String s : staticSortFields) {
                ret.add(new StringPair(s, "asc"));
                logger.trace("Added static sort field: {}", s);
            }
        }
        ret.addAll(sortFields);

        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>sortFields</code>.
     * </p>
     *
     * @return the sortFields
     */
    public List<StringPair> getSortFields() {
        return sortFields;
    }

    /**
     * <p>
     * Getter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @return the dateUpdated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * <p>
     * Setter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * <p>
     * Getter for the field <code>lastHitsCount</code>.
     * </p>
     *
     * @return the lastHitsCount
     */
    public long getLastHitsCount() {
        return lastHitsCount;
    }

    /**
     * <p>
     * Setter for the field <code>lastHitsCount</code>.
     * </p>
     *
     * @param lastHitsCount the lastHitsCount to set
     */
    public void setLastHitsCount(long lastHitsCount) {
        this.lastHitsCount = lastHitsCount;
    }

    /**
     * <p>
     * isNewHitsNotification.
     * </p>
     *
     * @return the newHitsNotification
     */
    public boolean isNewHitsNotification() {
        return newHitsNotification;
    }

    /**
     * <p>
     * Setter for the field <code>newHitsNotification</code>.
     * </p>
     *
     * @param newHitsNotification the newHitsNotification to set
     */
    public void setNewHitsNotification(boolean newHitsNotification) {
        this.newHitsNotification = newHitsNotification;
    }

    /**
     * <p>
     * isSaved.
     * </p>
     *
     * @return the saved
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * <p>
     * Setter for the field <code>saved</code>.
     * </p>
     *
     * @param saved the saved to set
     */
    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    /**
     * <p>
     * Getter for the field <code>hitsCount</code>.
     * </p>
     *
     * @return the hitsCount
     */
    public long getHitsCount() {
        return hitsCount;
    }

    /**
     * <p>
     * Setter for the field <code>hitsCount</code>.
     * </p>
     *
     * @param hitsCount the hitsCount to set
     */
    public void setHitsCount(long hitsCount) {
        this.hitsCount = hitsCount;
    }

    /**
     * <p>
     * Getter for the field <code>hits</code>.
     * </p>
     *
     * @return the hits
     */
    public List<SearchHit> getHits() {
        logger.trace("hits: {}", hits.size());
        return hits;
    }

    /**
     * <p>
     * getLastPage.
     * </p>
     *
     * @param hitsPerPage a int.
     * @return a int.
     */
    public int getLastPage(int hitsPerPage) {
        int answer = 0;
        if (hitsPerPage > 0) {
            int hitsPerPageLocal = hitsPerPage;
            answer = (int) Math.floor((double) hitsCount / hitsPerPageLocal);
            if (hitsCount % hitsPerPageLocal != 0 || answer == 0) {
                answer++;
            }
        }
        return answer;
    }

    /**
     * Toggles the status of newHitsNotification and persists this search.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void toggleNotifications() throws DAOException {
        this.newHitsNotification = !this.newHitsNotification;
        DataManager.getInstance().getDao().updateSearch(this);
    }

    /**
     * @return the hitGeoCoordinateList
     */
    public List<Location> getHitsLocationList() {
        return hitLocationList;
    }
}
