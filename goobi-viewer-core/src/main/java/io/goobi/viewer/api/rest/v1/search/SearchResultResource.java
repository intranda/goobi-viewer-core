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
package io.goobi.viewer.api.rest.v1.search;

import static io.goobi.viewer.api.rest.v1.ApiUrls.SEARCH_EXPORT_FORMAT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.SEARCH_EXPORT_XML;

import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocumentList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.export.ExportFormat;
import io.goobi.viewer.model.export.SolrDocXmlExport;
import io.goobi.viewer.model.export.XsltSearchExport;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.StringPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * REST resource for exporting search results in various bibliographic and data formats.
 *
 * <p>
 * Provides a fixed endpoint for raw Solr XML export and a generic, config-driven endpoint that applies an XSLT stylesheet for formats such as RIS,
 * Endnote XML or BibTeX. New export formats can be added at runtime by configuring a {@code <format>} element in {@code config_viewer.xml} and
 * dropping the corresponding XSLT file into the config or classpath directory — no Java code changes required.
 */
@Path(ApiUrls.SEARCH)
@ViewerRestServiceBinding
public class SearchResultResource {

    private static final Logger logger = LogManager.getLogger(SearchResultResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * Default constructor required by JAX-RS.
     */
    public SearchResultResource() {
        // Required by JAX-RS
    }

    /**
     * Constructor for programmatic instantiation (e.g. in tests).
     *
     * @param servletRequest the HTTP servlet request
     * @param servletResponse the HTTP servlet response
     */
    public SearchResultResource(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    /**
     * Exports the current search results as raw Solr-style XML.
     *
     * @param query the Solr search query string
     * @param activeFacetString the active facet filter string
     * @param sortString semicolon-separated sort fields (e.g. {@code "SORT_TITLE;!IDDOC"}); prefix a field with {@code !} for descending order
     * @param proximitySearchDistance maximum word distance for proximity-search snippet highlighting; has no effect on the Solr query or the exported
     *            field values
     * @param rows maximum number of results to return (default 100); use a value {@code <= 0} to fetch all results in batches of 100
     * @return a {@link Response} containing the Solr XML
     * @throws PresentationException if the query cannot be parsed
     * @throws IndexUnreachableException if the Solr index is unreachable
     */
    @GET
    @jakarta.ws.rs.Path(SEARCH_EXPORT_XML)
    @Produces({ MediaType.APPLICATION_XML })
    @Operation(tags = { "search" }, summary = "Export search results as Solr XML")
    @ApiResponse(responseCode = "200", description = "Solr XML containing the matching documents")
    @ApiResponse(responseCode = "400", description = "Invalid search query or parameters")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable or XML serialisation error")
    @AccessConditionBinding
    public Response getSearchResultsAsXml(
            @Parameter(description = "Search query string") @QueryParam("query") @DefaultValue("*:*") String query,
            @Parameter(description = "Active facet filter string") @QueryParam("activeFacetString") @DefaultValue("") String activeFacetString,
            @Parameter(description = "Semicolon-separated sort fields; prefix with ! for descending")
            @QueryParam("sortString") @DefaultValue("") String sortString,
            @Parameter(description = "Proximity-search highlight distance (no effect on exported values)")
            @QueryParam("proximitySearchDistance") @DefaultValue("0") int proximitySearchDistance,
            @Parameter(description = "Maximum number of results; <= 0 fetches all") @QueryParam("rows") @DefaultValue("100") int rows)
            throws PresentationException, IndexUnreachableException {
        SolrDocumentList docs = executeSolrQuery(query, activeFacetString, sortString, proximitySearchDistance, rows);

        try {
            String xml = SolrDocXmlExport.toXmlString(docs);
            return Response.ok(xml, MediaType.APPLICATION_XML).build();
        } catch (ParserConfigurationException | TransformerException e) {
            logger.error("Error serialising Solr results to XML", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("XML serialisation error").build();
        }
    }

    /**
     * Generic export endpoint that transforms search results via a config-driven XSLT stylesheet.
     *
     * <p>
     * The {@code format} path parameter is matched against the {@code name} attribute of {@code <format>} elements in {@code config_viewer.xml}. If
     * the format is not configured the endpoint returns 404; if it is configured but disabled it returns 403.
     *
     * <p>
     * To add a new export format, simply add a {@code <format>} element to the configuration and drop the XSLT stylesheet into the viewer config
     * directory or the classpath:
     * 
     * <pre>{@code
     * <format name="marc" enabled="true" xslt="solr2marc.xsl"
     *         contentType="application/xml" fileExtension="xml" />
     * }</pre>
     *
     * @param format the export format name (e.g. "endnote", "bibtex", "ris")
     * @param query the Solr search query string
     * @param activeFacetString the active facet filter string
     * @param sortString semicolon-separated sort fields (e.g. {@code "SORT_TITLE;!IDDOC"}); prefix a field with {@code !} for descending order
     * @param proximitySearchDistance maximum word distance for proximity-search snippet highlighting;
     *        has no effect on the Solr query or the exported field values
     * @return a {@link Response} with the transformed content
     * @throws PresentationException if the query cannot be parsed
     * @throws IndexUnreachableException if the Solr index is unreachable
     */
    @GET
    @jakarta.ws.rs.Path(SEARCH_EXPORT_FORMAT)
    @Operation(tags = { "search" }, summary = "Export search results in a configured format (e.g. endnote, bibtex, ris)")
    @ApiResponse(responseCode = "200", description = "Transformed export in the requested format")
    @ApiResponse(responseCode = "400", description = "Invalid search query or parameters")
    @ApiResponse(responseCode = "403", description = "The requested export format is disabled")
    @ApiResponse(responseCode = "404", description = "The requested export format is not configured")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable or XSLT transformation error")
    @AccessConditionBinding
    public Response getSearchResultsAsFormat(
            @Parameter(description = "Export format name as configured in config_viewer.xml") @PathParam("format") String format,
            @Parameter(description = "Search query string") @QueryParam("query") @DefaultValue("*:*") String query,
            @Parameter(description = "Active facet filter string") @QueryParam("activeFacetString") @DefaultValue("") String activeFacetString,
            @Parameter(description = "Semicolon-separated sort fields; prefix with ! for descending")
            @QueryParam("sortString") @DefaultValue("") String sortString,
            @Parameter(
                    description = "Proximity-search highlight distance (no effect on exported values)")
            @QueryParam("proximitySearchDistance") @DefaultValue("0") int proximitySearchDistance)
            throws PresentationException, IndexUnreachableException {

        // Look up the format in all configured formats (including disabled ones) for proper error reporting
        List<ExportFormat> allFormats = DataManager.getInstance().getConfiguration().getSearchExportFormats();
        Optional<ExportFormat> match = allFormats.stream()
                .filter(f -> format.equals(f.getName()))
                .findFirst();

        if (match.isEmpty()) {
            return Response.status(Status.NOT_FOUND).entity("Unknown export format: " + format).build();
        }
        ExportFormat exportFormat = match.get();
        if (!exportFormat.isEnabled()) {
            return Response.status(Status.FORBIDDEN).entity("Export format is disabled: " + format).build();
        }

        SolrDocumentList docs = executeSolrQuery(query, activeFacetString, sortString, proximitySearchDistance, 0);

        try {
            String result = XsltSearchExport.transform(docs, exportFormat.getXslt());
            String fileName = "search_export." + exportFormat.getFileExtension();
            return Response.ok(result, exportFormat.getContentType())
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();
        } catch (ParserConfigurationException | TransformerException e) {
            logger.error("Error transforming Solr results to format '{}'", format, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("XSLT transformation error").build();
        }
    }

    /**
     * Executes a Solr query with optional facet filters and returns the raw document list.
     *
     * <p>
     * When {@code rows <= 0} all matching documents are fetched in batches of 100. Otherwise exactly {@code rows} documents are returned in a single
     * query.
     *
     * @param query the raw search query string
     * @param activeFacetString the active facet filter string (may be empty)
     * @param sortString semicolon-separated sort fields (e.g. {@code "SORT_TITLE;!IDDOC"}); prefix a field with {@code !} for descending order;
     *            {@code null} or blank for default Solr ordering
     * @param proximitySearchDistance accepted for API compatibility; has no effect on the Solr query or returned fields
     * @param rows maximum number of documents to return; {@code <= 0} fetches all results
     * @return the matching Solr documents
     * @throws PresentationException if the query cannot be parsed
     * @throws IndexUnreachableException if the Solr index is unreachable
     */
    private static SolrDocumentList executeSolrQuery(String query, String activeFacetString, String sortString,
            int proximitySearchDistance, int rows)
            throws PresentationException, IndexUnreachableException {
        String currentQuery = SearchHelper.prepareQuery(query);
        String finalQuery = SearchHelper.buildFinalQuery(currentQuery, true, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);

        SearchFacets facets = new SearchFacets();
        if (activeFacetString != null && !activeFacetString.isEmpty()) {
            facets.setActiveFacetString(activeFacetString);
        }
        List<String> filterQueries = facets.generateFacetFilterQueries(true);

        List<StringPair> sortFields = (sortString != null && !sortString.isBlank())
                ? SearchHelper.parseSortString(sortString, null)
                : null;

        if (rows <= 0) {
            long totalHits = DataManager.getInstance().getSearchIndex().getHitCount(finalQuery, filterQueries);
            int batchSize = 100;
            int totalBatches = (int) Math.ceil((double) totalHits / batchSize);
            SolrDocumentList all = new SolrDocumentList();
            all.setNumFound(totalHits);
            all.setStart(0);
            for (int i = 0; i < totalBatches; i++) {
                int first = i * batchSize;
                int thisBatch = (int) Math.min(batchSize, totalHits - first);
                SolrDocumentList batch = DataManager.getInstance()
                        .getSearchIndex()
                        .search(finalQuery, first, thisBatch, sortFields, null, null, filterQueries, null)
                        .getResults();
                all.addAll(batch);
            }
            return all;
        }

        return DataManager.getInstance()
                .getSearchIndex()
                .search(finalQuery, 0, rows, sortFields, null, null, filterQueries, null)
                .getResults();
    }
}
