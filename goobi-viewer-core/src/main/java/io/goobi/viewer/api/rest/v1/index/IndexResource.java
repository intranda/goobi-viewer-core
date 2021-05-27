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
package io.goobi.viewer.api.rest.v1.index;

import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_FIELDS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_QUERY;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_STATISTICS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEX_STREAM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.SolrStream;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.RecordsRequestParameters;
import io.goobi.viewer.api.rest.model.index.SolrFieldInfo;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@Path(INDEX)
@CORSBinding
@ViewerRestServiceBinding
public class IndexResource {

    private static final Logger logger = LoggerFactory.getLogger(IndexResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * 
     * @param query
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @Path(INDEX_STATISTICS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "index" },
            summary = "Statistics about indexed records")
    public String getStatistics(
            @Parameter(description = "SOLR Query to filter results (optional)") @QueryParam("query") String query)
            throws IndexUnreachableException, PresentationException {

        if (query == null) {
            query = "+(ISWORK:*) ";
        }

        String finalQuery =
                new StringBuilder().append(query).append(SearchHelper.getAllSuffixes(servletRequest, true, true)).toString();
        logger.debug("q: {}", finalQuery);
        long count = DataManager.getInstance().getSearchIndex().search(query, 0, 0, null, null, null).getResults().getNumFound();
        JSONObject json = new JSONObject();
        json.put("count", count);
        return json.toString();
    }

    /**
     * 
     * @param params
     * @return
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws IllegalRequestException
     */
    @POST
    @Path(INDEX_QUERY)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "index" },
            summary = "Post a query directly the SOLR index")
    @ApiResponse(responseCode = "400", description = "Illegal query or query parameters")
    public String getRecordsForQuery(RecordsRequestParameters params)
            throws IndexUnreachableException, ViewerConfigurationException, DAOException, IllegalRequestException {
        JSONObject ret = new JSONObject();
        if (params == null || params.query == null) {
            ret.put("status", HttpServletResponse.SC_BAD_REQUEST);
            ret.put("message", "Invalid JSON request object");
            return ret.toString();
        }

        // Custom query does not filter by the sub-theme discriminator value by default, it has to be added to the custom query via #{navigationHelper.subThemeDiscriminatorValueSubQuery}
        //        String query =
        //                new StringBuilder().append("+").append(params.getQuery()).append(SearchHelper.getAllSuffixes(servletRequest, null, true, true, false)).toString();
        String query = SearchHelper.buildFinalQuery(params.query, params.includeChildHits);

        logger.trace("query: {}", query);

        int count = params.count;
        if (count <= 0) {
            count = SolrSearchIndex.MAX_HITS;
        }

        List<StringPair> sortFieldList = new ArrayList<>();
        for (String sortField : params.sortFields) {
            if (StringUtils.isNotEmpty(sortField)) {
                sortFieldList.add(new StringPair(sortField, params.sortOrder));
            }
        }
        if (params.randomize) {
            sortFieldList.clear();
            sortFieldList.add(new StringPair(SolrTools.generateRandomSortField(), ("desc".equals(params.sortOrder) ? "desc" : "asc")));
        }
        try {
            List<String> fieldList = params.resultFields;

            Map<String, String> paramMap = null;
            if (params.includeChildHits) {
                paramMap = SearchHelper.getExpandQueryParams(params.query);
            }
            //                        QueryResponse response = DataManager.getInstance().getSearchIndex().search(query, params.offset, count, sortFieldList, null, fieldList );
            QueryResponse response =
                    DataManager.getInstance()
                            .getSearchIndex()
                            .search(query, params.offset, count, sortFieldList, params.facetFields, fieldList, null, paramMap);

            JSONObject object = new JSONObject();
            object.put("numFound", response.getResults().getNumFound());
            object.put("docs", getQueryResults(params, response));
            getFacetResults(response).ifPresent(facets -> object.put("facets", facets));

            return object.toString();
        } catch (PresentationException e) {
            throw new IllegalRequestException(e.getMessage());
        }
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

    private JSONArray getQueryResults(RecordsRequestParameters params, QueryResponse response)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        SolrDocumentList result = response.getResults();
        Map<String, SolrDocumentList> expanded = response.getExpandedResults();
        logger.trace("hits: {}", result.size());
        JSONArray jsonArray = null;
        if (params.jsonFormat != null) {
            switch (params.jsonFormat) {
                case "datecentric":
                    jsonArray = JsonTools.getDateCentricRecordJsonArray(result, servletRequest);
                    break;
                default:
                    jsonArray = JsonTools.getRecordJsonArray(result, expanded, servletRequest, params.language);
                    break;
            }
        } else {
            jsonArray = JsonTools.getRecordJsonArray(result, expanded, servletRequest, params.language);
        }
        if (jsonArray == null) {
            jsonArray = new JSONArray();
        }
        return jsonArray;
    }

    /**
     * 
     * @param expression
     * @return
     */
    @POST
    @Path(INDEX_STREAM)
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "index" },
            summary = "Post a streaming expression to the SOLR index and forward its response")
    @ApiResponse(responseCode = "400", description = "Illegal query or query parameters")
    @ApiResponse(responseCode = "500", description = "Solr not available or unable to respond")
    public StreamingOutput stream(
            @Schema(description = "Raw SOLR streaming expression",
                    example = "search(collection1,q=\"+ISANCHOR:*\", sort=\"YEAR asc\", fl=\"YEAR,PI,DOCTYPE\", rows=5, qt=\"/select\")") String expression) {
        String solrUrl = DataManager.getInstance().getSearchIndex().getSolrServerUrl();
        logger.trace("Call solr " + solrUrl);
        logger.trace("Streaming expression " + expression);
        return executeStreamingExpression(expression, solrUrl);
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    @GET
    @Path(INDEX_FIELDS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieves a JSON list of all existing Solr fields.", tags = { "index" })
    public List<SolrFieldInfo> getAllIndexFields() throws IOException {
        logger.trace("getAllIndexFields");

        try {
            return collectFieldInfo();
        } catch (DAOException e) {
            logger.error(e.getMessage());
            servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 
     * @return
     * @throws DAOException
     * @should create list correctly
     */
    static List<SolrFieldInfo> collectFieldInfo() throws DAOException {
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
     * @param expr
     * @param solrUrl
     * @return
     */
    private static StreamingOutput executeStreamingExpression(String expr, String solrUrl) {
        return (out) -> {
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
