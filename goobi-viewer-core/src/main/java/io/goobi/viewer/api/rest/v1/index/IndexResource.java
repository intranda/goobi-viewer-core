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
package io.goobi.viewer.api.rest.v1.index;

import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_FIELDS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_QUERY;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_SPATIAL_HEATMAP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_SPATIAL_SEARCH;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_STATISTICS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_STREAM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.SolrStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.omnifaces.el.functions.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.RecordsRequestParameters;
import io.goobi.viewer.api.rest.model.index.SolrFieldInfo;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.maps.GeoMapFeature;
import io.goobi.viewer.model.maps.GeoMapFeatureItem;
import io.goobi.viewer.model.maps.SolrSearchScope;
import io.goobi.viewer.model.maps.features.AbstractFeatureDataProvider;
import io.goobi.viewer.model.maps.features.FeatureGenerator;
import io.goobi.viewer.model.maps.features.IFeatureDataProvider;
import io.goobi.viewer.model.maps.features.LabelCreator;
import io.goobi.viewer.model.maps.features.MetadataDocument;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * @author florian
 */
@Path(INDEX)
@CORSBinding
@ViewerRestServiceBinding
public class IndexResource {

    private static final Logger logger = LogManager.getLogger(IndexResource.class);

    //limits of hits per clickable marker. This does not affect the number of total hits found by the heatmap
    private static final int MAX_RECORD_HITS = 50_000;

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     *
     * @param query optional Solr query to filter counted records
     * @return Indexed records statistics as JSON
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @Path(INDEX_STATISTICS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "index" },
            summary = "Statistics about indexed records")
    @ApiResponse(responseCode = "200", description = "JSON object with record count statistics")
    @ApiResponse(responseCode = "400", description = "Invalid Solr query syntax")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public String getStatistics(
            @Parameter(description = "Solr query to filter results (optional)",
                    schema = @Schema(pattern = "^[ -~]*$")) @QueryParam("query") final String query)
            throws IndexUnreachableException, PresentationException, IllegalRequestException {

        // Enforce the documented pattern: only printable ASCII (0x20–0x7E). Non-ASCII and
        // control characters could cause Solr parse errors or unexpected Lucene behavior.
        if (query != null && !query.matches("[ -~]*")) {
            throw new IllegalRequestException("Query must contain only printable ASCII characters");
        }
        String useQuery = query;
        // Treat empty/blank string same as null to avoid Solr syntax error from "+()".
        if (StringUtils.isBlank(useQuery)) {
            useQuery = "+(ISWORK:*) ";
        } else {
            useQuery = String.format("+(%s)", useQuery);
        }

        String finalQuery =
                new StringBuilder().append(useQuery).append(SearchHelper.getAllSuffixes(servletRequest, true, true)).toString();
        long count;
        try {
            count = DataManager.getInstance().getSearchIndex().search(finalQuery, 0, 0, null, null, null).getResults().getNumFound();
        } catch (PresentationException e) {
            // Invalid query syntax (e.g. bare ")" or unknown field) causes Solr to reject the request.
            if (SolrTools.isQuerySyntaxError(e)) {
                throw new IllegalRequestException("Invalid query: " + e.getMessage());
            }
            throw e;
        }
        JSONObject json = new JSONObject();
        json.put("count", count);
        return json.toString();
    }

    /**
     *
     * @param params query parameters including query, sort, facet, and result field configuration
     * @return Records as JSON
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws IllegalRequestException
     */
    @POST
    @CORSBinding
    @Path(INDEX_QUERY)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "index" },
            summary = "Post a query directly to the Solr index")
    @ApiResponse(responseCode = "200", description = "JSON object with matched documents and optional facets")
    @ApiResponse(responseCode = "400", description = "Illegal query or query parameters")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public String getRecordsForQuery(RecordsRequestParameters params)
            throws IndexUnreachableException, ViewerConfigurationException, DAOException, IllegalRequestException {
        JSONObject ret = new JSONObject();
        if (params == null || params.getQuery() == null) {
            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
            ret.put("message", "Invalid JSON request object");
            return ret.toString();
        }

        String query = SearchHelper.buildFinalQuery(params.getQuery(), params.isBoostTopLevelDocstructs(),
                params.isIncludeChildHits() ? SearchAggregationType.AGGREGATE_TO_TOPSTRUCT : SearchAggregationType.NO_AGGREGATION);

        logger.trace("query: {}", query);

        int count = params.getCount();
        if (count < 0) {
            count = SolrSearchIndex.MAX_HITS;
        }

        List<StringPair> sortFieldList = new ArrayList<>();
        for (String sortField : params.getSortFields()) {
            if (StringUtils.isNotEmpty(sortField)) {
                sortFieldList.add(new StringPair(sortField, params.getSortOrder()));
            }
        }
        if (params.isRandomize()) {
            sortFieldList.clear();
            sortFieldList.add(new StringPair(SolrTools.generateRandomSortField(), ("desc".equals(params.getSortOrder()) ? "desc" : "asc")));
        }
        try {
            List<String> fieldList = params.getResultFields();

            // Validate facetFields: the schema defines this as an array of strings (not nullable).
            // Reject if the field itself is null, contains null items, or contains strings that
            // are not valid Solr field name identifiers (which would cause a Solr syntax error).
            List<String> facetFields = params.getFacetFields();
            if (facetFields == null) {
                throw new IllegalRequestException("facetFields must be an array, not null");
            }
            for (String field : facetFields) {
                if (field == null) {
                    throw new IllegalRequestException("facetFields must not contain null items");
                }
                // Solr field names are identifiers: letters/digits/underscores, optional wildcard '*'
                if (!field.matches("[A-Za-z][A-Za-z0-9_]*\\*?")) {
                    throw new IllegalRequestException("Invalid facet field name: '" + field + "'");
                }
            }

            // Validate resultFields: reject null list, null items, empty strings, and values that
            // are not valid Solr field name identifiers (which could cause a 500 when Solr receives
            // invalid field names like surrogates, control characters, or empty strings).
            if (fieldList == null) {
                throw new IllegalRequestException("resultFields must be an array, not null");
            }
            for (String field : fieldList) {
                if (field == null) {
                    throw new IllegalRequestException("resultFields must not contain null items");
                }
                if (field.isEmpty()) {
                    throw new IllegalRequestException("resultFields must not contain empty strings");
                }
                // Solr field names are identifiers: letters/digits/underscores, optional wildcard '*'
                if (!field.matches("[A-Za-z][A-Za-z0-9_]*\\*?")) {
                    throw new IllegalRequestException("Invalid result field name: '"
                            + field.replaceAll("[\\p{Cc}\\p{Cs}]", "?") + "'");
                }
            }

            Map<String, String> paramMap = null;
            if (params.isIncludeChildHits()) {
                paramMap = SearchHelper.getExpandQueryParams(params.getQuery());
            }
            QueryResponse response =
                    DataManager.getInstance()
                            .getSearchIndex()
                            .search(query, params.getOffset(), count, sortFieldList, facetFields, fieldList, null, paramMap);

            JSONObject object = new JSONObject();
            object.put("numFound", response.getResults().getNumFound());
            object.put("docs", getQueryResults(params, response));
            getFacetResults(response).ifPresent(facets -> object.put("facets", facets));

            return object.toString();
        } catch (PresentationException e) {
            throw new IllegalRequestException(e.getMessage());
        }
    }

    /**
     *
     * @param expression raw Solr streaming expression to execute
     * @return {@link StreamingOutput}
     */
    @POST
    @Path(INDEX_STREAM)
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "index" },
            summary = "Post a streaming expression to the Solr index and forward its response")
    @ApiResponse(responseCode = "200", description = "Newline-delimited JSON tuples streamed from Solr")
    @ApiResponse(responseCode = "400", description = "Illegal query or query parameters")
    @ApiResponse(responseCode = "500", description = "Solr not available or unable to respond")
    @RequestBody(required = true, content = @Content(mediaType = "text/plain"))
    public StreamingOutput stream(
            @Schema(description = "Raw Solr streaming expression",
                    example = "search(current,q=\"+ISANCHOR:*\", sort=\"YEAR asc\", fl=\"YEAR,PI,DOCTYPE\""
                            + ", rows=5, qt=\"/select\")") String expression) {
        String solrUrl = DataManager.getInstance().getSearchIndex().getSolrServerUrl();
        logger.trace("Call solr {}", solrUrl);
        logger.trace("Streaming expression {}", expression);
        return executeStreamingExpression(expression, solrUrl);
    }

    /**
     *
     * @return List<SolrFieldInfo>
     * @throws IOException
     */
    @GET
    @Path(INDEX_FIELDS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieves a JSON list of all existing Solr fields.", tags = { "index" })
    @ApiResponse(responseCode = "200", description = "JSON array of Solr field metadata")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public List<SolrFieldInfo> getAllIndexFields() throws IOException {
        logger.trace("getAllIndexFields");

        try {
            return collectFieldInfo();
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage());
            servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * @param solrField Solr field containing spatial coordinate data
     * @param wktRegion WKT coordinate string restricting the search area
     * @param filterQuery additional Solr query to filter results
     * @param facetQuery facetting expression applied to heatmap results
     * @param gridLevel heatmap grid resolution level
     * @return Heatmap as {@link String}
     * @throws IOException
     * @throws IndexUnreachableException
     */
    @GET
    @Path(INDEX_SPATIAL_HEATMAP)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Returns a heatmap of geospatial search results", tags = { "index" })
    @ApiResponse(responseCode = "200", description = "JSON heatmap data for the given spatial query")
    @ApiResponse(responseCode = "400", description = "Invalid heatmap parameters or Solr field name format")
    @ApiResponse(responseCode = "404", description = "Solr field not found in index")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public String getHeatmap(
            @Parameter(description = "Solr field containing spatial coordinates",
                    schema = @Schema(pattern = "^[A-Za-z_][A-Za-z0-9_]*$")) @PathParam("solrField") String solrField,
            @Parameter(description = "Coordinate string in WKT format describing the area within which to search. If not given, assumed to contain"
                    + " the whole world") @QueryParam("region") @DefaultValue("[\"-180 -90\" TO \"180 90\"]") String wktRegion,
            // Restrict query to printable ASCII to prevent unicode from causing Solr parse errors.
            // When omitted or empty, defaults to "*:*" (match everything).
            @Parameter(description = "Additional query to filter results by",
                    schema = @Schema(pattern = "^[ -~]*$")) @QueryParam("query") @DefaultValue("*:*") String filterQuery,
            @Parameter(description = "Facetting to be applied to results",
                    schema = @Schema(pattern = "^[ -~]*$")) @QueryParam("facetQuery") @DefaultValue("") String facetQuery,
            // Minimum of 1: HeatmapFacetMap.setGridLevel() throws IllegalArgumentException for 0 or negative values.
            // Maximum of 2^31-1 because gridLevel is stored as Java int; larger values overflow and cause a 400.
            @Parameter(description = "The granularity of each grid cell (minimum: 1)",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "2147483647"))
            @QueryParam("gridLevel") Integer gridLevel)
            throws IndexUnreachableException, IllegalRequestException, ContentNotFoundException {
        // Validate solrField before sending to Solr: an invalid name (e.g. "0") causes an
        // unhandled exception deep in the Solr client that surfaces as HTTP 500.
        // Solr field names must start with a letter or underscore and contain only
        // letters, digits, and underscores.
        if (solrField == null || !solrField.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalRequestException("Not a valid Solr field name: " + solrField);
        }
        servletResponse.addHeader("Cache-Control", "max-age=300");

        // When query is explicitly set to empty string (e.g. ?query=), treat it as "*:*" to avoid
        // Solr syntax error from building "+() ..." which is invalid syntax.
        String finalQuery = org.apache.commons.lang3.StringUtils.isBlank(filterQuery) ? "*:*" : filterQuery;
        if (!finalQuery.startsWith("{!join")) {
            finalQuery =
                    new StringBuilder().append("+(")
                            .append(filterQuery)
                            .append(") +(-MD_GEOJSON_POLYGON:* -MD_GPS_POLYGON:* *:*)")
                            .append(SearchHelper.getAllSuffixes(servletRequest, true, true))
                            .toString();
        } else {
            //search query. Ignore all polygon results or the heatmap will have hits everywhere
            if (finalQuery.endsWith(")")) {
                finalQuery = finalQuery.substring(0, finalQuery.length() - 1) + "-MD_GEOJSON_POLYGON:* -MD_GPS_POLYGON:*)";
            } else {
                finalQuery = finalQuery + " -MD_GEOJSON_POLYGON:* -MD_GPS_POLYGON:*";

            }
        }
        String queryEscaped = StringTools.unescapeCriticalUrlChracters(finalQuery);
        try {
            return DataManager.getInstance()
                    .getSearchIndex()
                    .getHeatMap(solrField, wktRegion, queryEscaped, facetQuery, gridLevel);
        } catch (IllegalArgumentException e) {
            // HeatmapFacetMap.setGridLevel() throws IllegalArgumentException for out-of-range values
            throw new IllegalRequestException("Invalid heatmap parameters: " + e.getMessage());
        } catch (IndexUnreachableException e) {
            // Solr rejects unknown field names with RemoteSolrException wrapped in IndexUnreachableException.
            // Undefined/unknown fields → 404 (the resource doesn't exist); other syntax errors → 400.
            if (SolrTools.isQuerySyntaxError(e)) {
                if (e.getMessage() != null
                        && (e.getMessage().contains("undefined field") || e.getMessage().contains("field can't be found"))) {
                    throw new ContentNotFoundException("Solr field not found in index: " + solrField);
                }
                throw new IllegalRequestException("Invalid Solr field or query: " + e.getMessage());
            }
            throw e;
        }

    }

    @GET
    @Path(INDEX_SPATIAL_SEARCH)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Returns results of a geospatial search as GeoJson objects", tags = { "index" })
    @ApiResponse(responseCode = "200", description = "JSON array of GeoJSON feature objects")
    @ApiResponse(responseCode = "400", description = "Invalid Solr field name format or query syntax")
    @ApiResponse(responseCode = "404", description = "Solr field not found in index")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    public String getGeoJsonResuls(
            @Parameter(description = "Solr field containing spatial coordinates",
                    schema = @Schema(pattern = "^[A-Za-z_][A-Za-z0-9_]*$")) @PathParam("solrField") String solrField,
            @Parameter(
                    description = "Coordinate string in WKT format describing the area within which to search. If not given, assumed to contain"
                            + " the whole world") @QueryParam("region") @DefaultValue("[\"-180 -90\" TO \"180 90\"]") String wktRegion,
            // Restrict query to printable ASCII to prevent unicode from causing Solr parse errors.
            @Parameter(description = "Additional query to filter results by",
                    schema = @Schema(pattern = "^[ -~]*$")) @QueryParam("query") @DefaultValue("*:*") String filterQuery,
            @Parameter(description = "Facetting to be applied to results",
                    schema = @Schema(pattern = "^[ -~]*$")) @QueryParam("facetQuery") @DefaultValue("") String facetQuery,
            @Parameter(description = "The Solr field to be used as label for each feature") @QueryParam("labelField") String labelField,
            @Parameter(description = "The scope of documents to search in. "
                    + "One of 'RECORDS', 'DOCSTRUCTS' and 'METADATA'") @QueryParam("scope") String searchScope)
            throws IndexUnreachableException, PresentationException, IllegalRequestException, ContentNotFoundException {
        servletResponse.addHeader("Cache-Control", "max-age=300");

        String finalQuery = StringTools.unescapeCriticalUrlChracters(filterQuery);
        List<String> facetQueries = new ArrayList<>();

        if (StringUtils.isNotBlank(facetQuery)) {
            facetQueries.add(facetQuery);
        }

        String coordQuery = "*:*";
        if (!finalQuery.startsWith("{!join")) {
            finalQuery =
                    new StringBuilder()
                            .append(finalQuery)
                            .append(" +({wktField}:{wktCoords}) ".replace("{wktField}", solrField).replace("{wktCoords}", wktRegion))
                            .append(SearchHelper.getAllSuffixes(servletRequest, true, true))
                            .toString();
        } else {
            coordQuery = "{wktField}:{wktCoords}".replace("{wktField}", solrField).replace("{wktCoords}", wktRegion);
            facetQueries.add(coordQuery);
        }

        Collection<GeoMapFeature> features;
        try {
            features = createFeatures(StringUtils.isBlank(finalQuery) ? "*:*" : finalQuery, coordQuery, labelField, searchScope);
        } catch (PresentationException e) {
            // Solr rejects unknown field names or bad queries via RemoteSolrException → PresentationException.
            // Undefined/unknown fields → 404; other syntax errors → 400.
            if (SolrTools.isQuerySyntaxError(e)) {
                if (e.getMessage() != null
                        && (e.getMessage().contains("undefined field") || e.getMessage().contains("field can't be found"))) {
                    throw new ContentNotFoundException("Solr field not found in index: " + solrField);
                }
                throw new IllegalRequestException("Invalid Solr field or query: " + e.getMessage());
            }
            throw e;
        } catch (IndexUnreachableException e) {
            if (SolrTools.isQuerySyntaxError(e)) {
                if (e.getMessage() != null
                        && (e.getMessage().contains("undefined field") || e.getMessage().contains("field can't be found"))) {
                    throw new ContentNotFoundException("Solr field not found in index: " + solrField);
                }
                throw new IllegalRequestException("Invalid Solr field or query: " + e.getMessage());
            }
            throw e;
        }

        String objects = features
                .stream()
                .map(GeoMapFeature::getJsonObject)
                .map(Object::toString)
                .collect(Collectors.joining(","));
        return "[" + objects + "]";
    }

    protected synchronized Collection<GeoMapFeature> createFeatures(String query, String coordinateQuery, String labelConfig, String searchScope)
            throws PresentationException, IndexUnreachableException {

        // If the query already carries a join prefix (e.g. from a search-results page), strip it
        // and fall back to RECORDS scope. The data providers add their own join internally, so a
        // nested join would produce invalid Solr syntax and a 400 error.
        String effectiveQuery = query;
        String effectiveScope = searchScope;
        if (query.startsWith(SearchHelper.AGGREGATION_QUERY_PREFIX)) {
            effectiveQuery = query.substring(SearchHelper.AGGREGATION_QUERY_PREFIX.length());
            effectiveScope = SolrSearchScope.RECORDS.name();
        }

        String finalQuery = "+(%s) +(%s)".formatted(effectiveQuery, coordinateQuery);

        LabelCreator markerLabels =
                new LabelCreator(DataManager.getInstance().getConfiguration().getMetadataTemplates(getMarkerMetadataList(labelConfig)));
        LabelCreator itemLabels =
                new LabelCreator(DataManager.getInstance().getConfiguration().getMetadataTemplates(getItemMetadataList(labelConfig)));
        List<String> coordinateFields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();

        SolrSearchScope scope = SolrSearchScope.DOCSTRUCTS;
        if (StringUtils.isNotBlank(effectiveScope) && Arrays.contains(SolrSearchScope.values(), effectiveScope.toUpperCase())) {
            scope = SolrSearchScope.valueOf(effectiveScope.toUpperCase());
        }

        List<MetadataDocument> hits;
        try {
            IFeatureDataProvider queryGenerator =
                    AbstractFeatureDataProvider.getDataProvider(scope,
                            ListUtils.union(coordinateFields, ListUtils.union(markerLabels.getFieldsToQuery(), itemLabels.getFieldsToQuery())));
            hits = queryGenerator.getResults(finalQuery, MAX_RECORD_HITS);
        } catch (SolrException e) {
            throw new IndexUnreachableException("SOLR communication failed:" + e.toString());
        }
        FeatureGenerator featureGenerator = new FeatureGenerator(coordinateFields, Collections.emptyList(), markerLabels, itemLabels);

        Collection<GeoMapFeature> featuresFromSolr = new ArrayList<>();
        for (MetadataDocument hit : hits) {
            Collection<GeoMapFeature> features = featureGenerator.getFeatures(hit, scope);
            featuresFromSolr.addAll(features);
        }

        Collection<GeoMapFeature> combinedFeatures = combineFeatures(featuresFromSolr);

        return combinedFeatures;
    }

    private Collection<GeoMapFeature> combineFeatures(Collection<GeoMapFeature> singleFeatures) {
        Map<GeoMapFeature, List<GeoMapFeature>> featureMap = singleFeatures.stream().collect(Collectors.groupingBy(Function.identity()));
        Collection<GeoMapFeature> features = new ArrayList<>();
        for (Entry<GeoMapFeature, List<GeoMapFeature>> entry : featureMap.entrySet()) {
            GeoMapFeature feature = entry.getKey();
            Collection<GeoMapFeatureItem> items = entry.getValue().stream().flatMap(f -> f.getItems().stream()).toList();
            feature.setItems(items);
            feature.setCount(items.size());
            if (feature.getItems().size() == 1) {
                feature.getItems().stream().findAny().map(item -> item.getLink()).ifPresent(link -> feature.setLink(link));
            }
            features.add(feature);
        }
        return features;
    }

    public String getMarkerMetadataList(String config) {
        return DataManager.getInstance().getConfiguration().getMetadataListForGeomapMarkerConfig(config);
    }

    public String getItemMetadataList(String config) {
        return DataManager.getInstance().getConfiguration().getMetadataListForGeomapItemConfig(config);
    }

    private static Optional<JSONArray> getFacetResults(QueryResponse response) {
        List<FacetField> facetFields = response.getFacetFields();
        if (facetFields != null && !facetFields.isEmpty()) {
            JSONArray facets = new JSONArray();
            facetFields.forEach(ff -> {
                JSONObject facet = new JSONObject();
                facets.put(facet);
                String facetName = ff.getName();
                int facetCount = ff.getValueCount();
                facet.put("field", facetName);
                facet.put("count", facetCount);
                JSONArray facetList = new JSONArray();
                facet.put("values", facetList);
                ff.getValues().forEach(c -> {
                    String value = c.getName();
                    long num = c.getCount();
                    JSONObject countObject = new JSONObject();
                    countObject.put("value", value);
                    countObject.put("count", num);
                    facetList.put(countObject);
                });
            });
            return Optional.of(facets);
        }
        return Optional.empty();
    }

    /**
     *
     * @param params original request parameters controlling output format and language
     * @param response Solr query response containing matched documents
     * @return {@link JSONArray} with query results
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    private JSONArray getQueryResults(RecordsRequestParameters params, QueryResponse response)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        SolrDocumentList result = response.getResults();
        Map<String, SolrDocumentList> expanded = response.getExpandedResults();
        logger.trace("hits: {}", result.size());
        JSONArray jsonArray = null;
        if (params.getJsonFormat() != null) {
            if ("datecentric".equals(params.getJsonFormat())) {
                jsonArray = JsonTools.getDateCentricRecordJsonArray(result, servletRequest);
            } else {
                jsonArray = JsonTools.getRecordJsonArray(result, expanded, servletRequest, params.getLanguage());
            }
        } else {
            jsonArray = JsonTools.getRecordJsonArray(result, expanded, servletRequest, params.getLanguage());
        }
        if (jsonArray == null) {
            jsonArray = new JSONArray();
        }
        return jsonArray;
    }

    /**
     *
     * @return List<SolrFieldInfo>
     * @throws DAOException
     * @should create list correctly
     */
    static List<SolrFieldInfo> collectFieldInfo() throws IndexUnreachableException {
        List<String> fieldNames = DataManager.getInstance().getSearchIndex().getAllFieldNames();
        if (fieldNames == null) {
            return Collections.emptyList();
        }
        logger.trace("{} field names collectied", fieldNames.size());
        Collections.sort(fieldNames);

        Set<String> reference = new HashSet<>(fieldNames);
        List<SolrFieldInfo> ret = new ArrayList<>();
        for (String fieldName : fieldNames) {
            if (fieldName.startsWith("SORT_") || fieldName.startsWith("FACET_") || fieldName.endsWith("_UNTOKENIZED")) {
                continue;
            }

            SolrFieldInfo sfi = new SolrFieldInfo(fieldName);
            ret.add(sfi);

            sfi.setStored(!SolrConstants.FULLTEXT.equals(fieldName)); // All fields except for FULLTEXT are stored

            String sortFieldName = SearchHelper.sortifyField(fieldName);
            if (!sortFieldName.equals(fieldName) && reference.contains(sortFieldName)) {
                sfi.setSortField(sortFieldName);
            }
            String facetFieldName = SearchHelper.facetifyField(fieldName);
            if (!facetFieldName.equals(fieldName) && reference.contains(facetFieldName)) {
                sfi.setFacetField(facetFieldName);
            }
            String boolFieldName = SearchHelper.boolifyField(fieldName);
            if (!boolFieldName.equals(fieldName) && reference.contains(boolFieldName)) {
                sfi.setBoolField(boolFieldName);
            }
            for (Locale locale : ViewerResourceBundle.getAllLocales()) {
                String translation = ViewerResourceBundle.getTranslation(fieldName, locale, false);
                if (translation != null && !translation.equals(fieldName)) {
                    sfi.getTranslations().put(locale.getLanguage(), translation);
                }
            }
        }

        return ret;
    }

    /**
     *
     * @param expr raw Solr streaming expression to execute
     * @param solrUrl base URL of the Solr server
     * @return {@link StreamingOutput}
     */
    private static StreamingOutput executeStreamingExpression(String expr, String solrUrl) {
        return out -> {
            ObjectMapper mapper = new ObjectMapper();
            ModifiableSolrParams paramsLoc = new ModifiableSolrParams();
            paramsLoc.set("expr", expr);
            paramsLoc.set("qt", "/stream");
            // Note, the "/collection" below can be an alias.
            try (TupleStream solrStream = new SolrStream(solrUrl, paramsLoc)) {
                StreamContext context = new StreamContext();
                solrStream.setStreamContext(context);
                solrStream.open();
                Tuple tuple;
                do {
                    tuple = solrStream.read();
                    String json = mapper.writeValueAsString(tuple);
                    out.write((json + "\n").getBytes());
                    out.flush();
                } while (!tuple.EOF);
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("not a proper expression clause")) {
                    throw new WebApplicationException(new IllegalRequestException(e.getMessage()));
                }
                throw new WebApplicationException(new IndexUnreachableException(e.toString()));
            }
        };
    }
}
