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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RIS_FILE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.SEARCH_EXPORT_FORMAT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.SEARCH_EXPORT_XML;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
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

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.RisResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.export.ExportFormat;
import io.goobi.viewer.model.export.RISExport;
import io.goobi.viewer.model.export.SolrDocXmlExport;
import io.goobi.viewer.model.export.XsltSearchExport;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StringPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * REST resource for exporting search results in OpenSearch and RIS bibliographic formats.
 */
@Path(ApiUrls.SEARCH)
@ViewerRestServiceBinding
public class SearchResultResource {

    private static final Logger logger = LogManager.getLogger(SearchResultResource.class);

    /** Shared daemon thread pool used to generate exports off the request thread, with a bounded timeout. */
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "search-export");
        t.setDaemon(true);
        return t;
    });

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public SearchResultResource() {

    }

    public SearchResultResource(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_RIS_FILE)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "search" }, summary = "Download current search as RIS export file")
    @ApiResponse(responseCode = "200", description = "RIS export file for the current search results")
    @ApiResponse(responseCode = "400", description = "Invalid search query or parameters")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable")
    @AccessConditionBinding
    public Response getRISAsFile(
            // Previously declared as @PathParam but the path template /search/ris has no {param} segments,
            // so these were never populated. Changed to @QueryParam so callers can actually pass them.
            @Parameter(description = "Search query string") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(description = "Sort string for the search results") @QueryParam("sortString") @DefaultValue("") String sortString,
            @Parameter(description = "Active facet filter string") @QueryParam("activeFacetString") @DefaultValue("") String activeFacetString,
            @Parameter(description = "Maximum word distance for proximity search") @QueryParam("proximitySearchDistance")
                    @DefaultValue("0") int proximitySearchDistance)
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException, ViewerConfigurationException {
        String currentQuery = SearchHelper.prepareQuery(query);
        String finalQuery = SearchHelper.buildFinalQuery(currentQuery, true, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        Locale locale = Locale.ENGLISH;

        Search search = new Search();
        search.setSortString(sortString);

        SearchFacets facets = new SearchFacets();
        facets.setActiveFacetString(activeFacetString);
        List<String> filterQueries = facets.generateFacetFilterQueries(true);

        RISExport export = new RISExport();
        export.executeSearch(finalQuery, null, filterQueries, null, null, locale, proximitySearchDistance);
        if (export.isHasResults()) {
            new RisResourceBuilder(servletRequest, servletResponse).writeRIS(export.getSearchHits());
        }
        return Response.status(Status.OK).build();
    }

    /**
     * Exports the current search results as raw Solr-style XML.
     *
     * @param query the Solr search query string
     * @param activeFacetString the active facet filter string
     * @param rows maximum number of results to return (default 100)
     * @return a {@link Response} containing the Solr XML
     * @throws PresentationException if the query cannot be parsed
     * @throws IndexUnreachableException if the Solr index is unreachable
     * @throws DAOException if license types cannot be loaded for access filtering
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
            @Parameter(description = "Maximum number of results") @QueryParam("rows") @DefaultValue("100") int rows)
            throws PresentationException, IndexUnreachableException, DAOException {
        SolrDocumentList docs = executeSolrQuery(query, activeFacetString, rows);

        try {
            String xml = SolrDocXmlExport.toXmlString(docs);
            return Response.ok(xml, MediaType.APPLICATION_XML).build();
        } catch (ParserConfigurationException | TransformerException e) {
            logger.error("Error serialising Solr results to XML", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("XML serialisation error").build();
        }
    }

    /**
     * Generic, config-driven export endpoint for search results. The {@code format} path parameter is matched against the {@code name}
     * attribute of {@code <format>} elements in {@code config_viewer.xml}. Both XSLT-based formats (ris/endnote/bibtex) and Java field-mapped
     * formats (excel/csv) are served here.
     *
     * <p>Unknown formats return 404, disabled formats return 403. The export runs synchronously on the request thread, consistent with the
     * other export endpoints in this resource (the request filter chain does not support asynchronous processing).
     *
     * @param format the export format name (e.g. "endnote", "bibtex", "ris", "excel", "csv")
     * @param query the Solr search query string
     * @param sortString the sort order string (used by field-mapped formats)
     * @param activeFacetString the active facet filter string
     * @param proximitySearchDistance maximum word distance for proximity search (used by field-mapped formats)
     * @param rows maximum number of results for XSLT formats (default 100; field-mapped formats always export all hits)
     * @return a {@link Response} with the export content
     */
    @GET
    @jakarta.ws.rs.Path(SEARCH_EXPORT_FORMAT)
    @Operation(tags = { "search" }, summary = "Export search results in a configured format (e.g. excel, csv, endnote, bibtex, ris)")
    @ApiResponse(responseCode = "200", description = "Export in the requested format")
    @ApiResponse(responseCode = "400", description = "Invalid search query or parameters")
    @ApiResponse(responseCode = "403",
            description = "The requested export format is disabled, or the caller may not download metadata for the matching records")
    @ApiResponse(responseCode = "404", description = "The requested export format is not configured")
    @ApiResponse(responseCode = "500", description = "Solr index unreachable or export generation error")
    @ApiResponse(responseCode = "503", description = "Export generation timed out")
    @AccessConditionBinding
    public Response getSearchResultsAsFormat(
            @Parameter(description = "Export format name as configured in config_viewer.xml") @PathParam("format") String format,
            @Parameter(description = "Search query string") @QueryParam("query") @DefaultValue("*:*") String query,
            @Parameter(description = "Sort string for the search results") @QueryParam("sortString") @DefaultValue("") String sortString,
            @Parameter(description = "Active facet filter string") @QueryParam("activeFacetString") @DefaultValue("") String activeFacetString,
            @Parameter(description = "Maximum word distance for proximity search") @QueryParam("proximitySearchDistance")
                    @DefaultValue("0") int proximitySearchDistance,
            @Parameter(description = "Maximum number of results (XSLT formats only)") @QueryParam("rows") @DefaultValue("100") int rows,
            @Parameter(description = "Record identifier used as the download file name base (single-hit exports)")
                    @QueryParam("identifier") @DefaultValue("") String identifier) {

        // Look up the format in all configured formats (including disabled ones) for proper error reporting
        Optional<ExportFormat> match = DataManager.getInstance().getConfiguration().getSearchExportFormats().stream()
                .filter(f -> format.equals(f.getName()))
                .findFirst();
        if (match.isEmpty()) {
            return Response.status(Status.NOT_FOUND).entity("Unknown export format: " + format).build();
        }
        ExportFormat exportFormat = match.get();
        if (!exportFormat.isEnabled()) {
            return Response.status(Status.FORBIDDEN).entity("Export format is disabled: " + format).build();
        }

        try {
            // Deny (403) when the search matches records the caller may list but not download as metadata, so the export
            // does not silently produce an empty file (e.g. a single hit the user is not allowed to download).
            Response denied = denyIfNoDownloadableRecords(query, activeFacetString);
            if (denied != null) {
                return denied;
            }
        } catch (PresentationException | IndexUnreachableException | DAOException e) {
            logger.error("Error preparing export for format '{}'", format, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Export generation error").build();
        }

        // Generate on a background thread with a timeout, mirroring the old bean's async download behaviour.
        // This uses a plain executor (not servlet/JAX-RS async), so it does not call request.startAsync() and
        // therefore does not require the servlet filter chain to support asynchronous processing.
        // Shared export timeout (config <search><export> @timeout), applies to all formats
        int timeoutSeconds = DataManager.getInstance().getConfiguration().getSearchExportTimeout();
        String fileNameBase = exportFileNameBase(identifier);
        Future<Response> future = EXECUTOR.submit(() -> exportFormat.isXsltBased()
                ? buildXsltExport(exportFormat, query, activeFacetString, rows, fileNameBase)
                : buildFieldMappedExport(exportFormat, query, sortString, activeFacetString, proximitySearchDistance, fileNameBase));
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("Export generation for format '{}' timed out after {}s", format, timeoutSeconds);
            return Response.status(Status.SERVICE_UNAVAILABLE).entity("Export timed out").build();
        } catch (ExecutionException e) {
            logger.error("Error building export for format '{}'", format, e.getCause());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Export generation error").build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.status(Status.SERVICE_UNAVAILABLE).entity("Export interrupted").build();
        }
    }

    /**
     * Returns a 403 response if the search matches records the caller may list but is not permitted to download as metadata (which would
     * otherwise yield an empty export file), or {@code null} if the export may proceed. A genuinely empty search (no listable matches) is
     * allowed to proceed and simply yields an empty export.
     *
     * @param query the Solr search query string
     * @param activeFacetString the active facet filter string
     * @return a 403 {@link Response}, or {@code null} to proceed
     * @throws PresentationException if the query cannot be parsed
     * @throws IndexUnreachableException if the Solr index is unreachable
     * @throws DAOException if license types cannot be loaded for access filtering
     */
    private Response denyIfNoDownloadableRecords(String query, String activeFacetString)
            throws PresentationException, IndexUnreachableException, DAOException {
        String currentQuery = SearchHelper.prepareQuery(query);
        String listQuery = SearchHelper.buildFinalQuery(currentQuery, true, servletRequest, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        String downloadQuery = addDownloadMetadataFilter(listQuery);
        List<String> filterQueries = exportFilterQueries(activeFacetString);
        if (DataManager.getInstance().getSearchIndex().getHitCount(downloadQuery, filterQueries) == 0
                && DataManager.getInstance().getSearchIndex().getHitCount(listQuery, filterQueries) > 0) {
            return Response.status(Status.FORBIDDEN)
                    .entity("You are not permitted to download metadata for the matching record(s)").build();
        }
        return null;
    }

    /**
     * Builds the Solr filter queries for the given active facet string.
     *
     * @param activeFacetString the active facet filter string (may be empty)
     * @return the list of Solr filter query strings
     */
    private static List<String> exportFilterQueries(String activeFacetString) {
        SearchFacets facets = new SearchFacets();
        if (StringUtils.isNotEmpty(activeFacetString)) {
            facets.setActiveFacetString(activeFacetString);
        }
        return facets.generateFacetFilterQueries(true);
    }

    /**
     * Builds an XSLT-based export (ris/endnote/bibtex) by transforming the Solr result XML with the format's stylesheet.
     *
     * @param exportFormat the configured XSLT format
     * @param query the Solr search query string
     * @param activeFacetString the active facet filter string
     * @param rows maximum number of documents to include
     * @param fileNameBase base name for the download file (without extension)
     * @return a ready {@link Response} carrying the transformed content
     */
    private Response buildXsltExport(ExportFormat exportFormat, String query, String activeFacetString, int rows, String fileNameBase)
            throws PresentationException, IndexUnreachableException, DAOException, ParserConfigurationException, TransformerException {
        SolrDocumentList docs = executeSolrQuery(query, activeFacetString, rows);
        String result = XsltSearchExport.transform(docs, exportFormat.getXslt());
        return attachment(Response.ok(result, exportFormat.getContentType()), exportFormat, fileNameBase);
    }

    /**
     * Builds a Java field-mapped export (excel/csv) over all matching hits. The full export is materialised in the worker thread so that it
     * is covered by the request timeout and any failure can be reported as an HTTP status before the response is committed.
     *
     * @param exportFormat the configured field-mapped format ("excel" or "csv")
     * @param query the Solr search query string
     * @param sortString the sort order string
     * @param activeFacetString the active facet filter string
     * @param proximitySearchDistance maximum word distance for proximity search
     * @param fileNameBase base name for the download file (without extension)
     * @return a ready {@link Response} carrying the export bytes
     */
    private Response buildFieldMappedExport(ExportFormat exportFormat, String query, String sortString, String activeFacetString,
            int proximitySearchDistance, String fileNameBase)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException, IOException {
        String currentQuery = SearchHelper.prepareQuery(query);
        // Pass the servlet request so the access-condition filter suffix is resolved from the caller's
        // session (same as the JSF bean did via FacesContext) or, when sessionless, computed on the fly.
        String finalQuery = SearchHelper.buildFinalQuery(currentQuery, true, servletRequest, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        finalQuery = addDownloadMetadataFilter(finalQuery);
        Locale locale = Locale.ENGLISH;

        List<String> filterQueries = exportFilterQueries(activeFacetString);
        List<StringPair> sortFields = SearchHelper.parseSortString(sortString, null);

        byte[] content;
        if ("csv".equals(exportFormat.getName())) {
            StringWriter writer = new StringWriter();
            SearchHelper.exportSearchAsCsv(writer, finalQuery, currentQuery, sortFields, filterQueries, null, null, locale,
                    proximitySearchDistance);
            content = writer.toString().getBytes(StandardCharsets.UTF_8);
        } else if ("excel".equals(exportFormat.getName())) {
            try (SXSSFWorkbook wb = new SXSSFWorkbook(25); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                SearchHelper.exportSearchAsExcel(wb, finalQuery, currentQuery, sortFields, filterQueries, null, null, locale,
                        proximitySearchDistance);
                wb.write(baos);
                content = baos.toByteArray();
            }
        } else {
            return Response.status(Status.NOT_IMPLEMENTED).entity("No handler for export format: " + exportFormat.getName()).build();
        }

        return attachment(Response.ok(content, exportFormat.getContentType()), exportFormat, fileNameBase);
    }

    /**
     * Adds the {@code Content-Disposition} attachment header for the given export format.
     *
     * @param builder the response builder to decorate
     * @param exportFormat the export format providing the file extension
     * @param fileNameBase base name for the download file (without timestamp or extension)
     * @return the built {@link Response}
     */
    private static Response attachment(Response.ResponseBuilder builder, ExportFormat exportFormat, String fileNameBase) {
        String fileName = fileNameBase + "_" + LocalDateTime.now().format(DateTools.FORMATTERFILENAME) + "." + exportFormat.getFileExtension();
        return builder.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"").build();
    }

    /**
     * Determines the download file-name base. When an identifier is supplied (single-hit exports) it is sanitised to safe file-name
     * characters and used; otherwise the generic {@code "search_export"} base is used (whole-list exports).
     *
     * @param identifier the record identifier passed by the caller, or blank
     * @return the file-name base (without timestamp or extension)
     */
    private static String exportFileNameBase(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return "search_export";
        }
        String sanitized = identifier.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        return StringUtils.isBlank(sanitized) ? "search_export" : sanitized;
    }

    /**
     * Appends the {@code PRIV_DOWNLOAD_METADATA} access-filter suffix to an export query. The query produced by
     * {@link SearchHelper#buildFinalQuery} already carries the {@code PRIV_LIST} listing filter; exports are metadata downloads, so an
     * exported record must additionally be downloadable as metadata. ANDing the two suffixes restricts the result to records the caller may
     * both list and download.
     *
     * @param finalQuery the query already carrying the standard listing suffix
     * @return the query with the download-metadata access suffix appended
     * @throws PresentationException if the access condition query cannot be built
     * @throws IndexUnreachableException if the Solr index is unreachable
     * @throws DAOException if license types cannot be loaded
     */
    private String addDownloadMetadataFilter(String finalQuery)
            throws PresentationException, IndexUnreachableException, DAOException {
        String suffix = SearchHelper.getPersonalFilterQuerySuffixForPrivilege(servletRequest, IPrivilegeHolder.PRIV_DOWNLOAD_METADATA);
        return StringUtils.isNotBlank(suffix) ? finalQuery + suffix : finalQuery;
    }

    /**
     * Executes a Solr query with optional facet filters and returns the raw document list.
     *
     * @param query the raw search query string
     * @param activeFacetString the active facet filter string (may be empty)
     * @param rows maximum number of documents to return
     * @return the matching Solr documents
     * @throws PresentationException if the query cannot be parsed
     * @throws IndexUnreachableException if the Solr index is unreachable
     */
    private SolrDocumentList executeSolrQuery(String query, String activeFacetString, int rows)
            throws PresentationException, IndexUnreachableException, DAOException {
        String currentQuery = SearchHelper.prepareQuery(query);
        // Pass the servlet request so the access-condition filter suffix is resolved from the caller's session.
        String finalQuery = SearchHelper.buildFinalQuery(currentQuery, true, servletRequest, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        finalQuery = addDownloadMetadataFilter(finalQuery);

        List<String> filterQueries = exportFilterQueries(activeFacetString);
        return DataManager.getInstance().getSearchIndex().search(finalQuery, 0, rows, null, null, null, null, filterQueries, null)
                .getResults();
    }
}
