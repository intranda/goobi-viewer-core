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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CALENDAR;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CALENDAR_MONTHS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CALENDAR_YEAR_REGEX;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for calendar data of a record (newspapers, periodicals, multi-volume works, etc.).
 */
@jakarta.ws.rs.Path(RECORDS_CALENDAR)
@ViewerRestServiceBinding
@CORSBinding
public class CalendarResource {

    private static final Logger logger = LogManager.getLogger(CalendarResource.class);

    private final String pi;

    public CalendarResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
    }

    /**
     * Returns all issues/volumes with a YEARMONTHDAY value for the given year as a flat JSON array.
     *
     * @param year Publication year
     * @return JSON array of calendar entries
     * @throws PresentationException if any.
     * @throws IndexUnreachableException if any.
     * @should return all issues for given anchor pi and year
     * @should return all issues for given group pi and year
     * @should return 404 if pi not found
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_CALENDAR_YEAR_REGEX)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records" }, summary = "Get calendar entries for a record and year")
    public Response getCalendarEntries(
            @Parameter(description = "Year to retrieve calendar entries for") @PathParam("year") int year)
            throws PresentationException, IndexUnreachableException {
        logger.trace("getCalendarEntries: {}/{}", pi, year);

        // Look up the PI document to determine whether it is an anchor or group
        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc("+" + SolrConstants.PI + ":\"" + pi + "\"",
                        List.of(SolrConstants.ISANCHOR, SolrConstants.DOCTYPE, SolrConstants.GROUPTYPE));
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Record not found: " + pi).build();
        }

        // Build query depending on record type (anchor, group, or regular)
        logger.debug("Record lookup for PI {}: isAnchor={}, DOCTYPE={}, GROUPTYPE={}",
                pi, doc.getFieldValue(SolrConstants.ISANCHOR), doc.getFieldValue(SolrConstants.DOCTYPE),
                doc.getFieldValue(SolrConstants.GROUPTYPE));
        String piField;
        if (SolrTools.isAnchor(doc)) {
            piField = SolrConstants.PI_ANCHOR;
        } else if (SolrTools.isGroup(doc)) {
            piField = SolrTools.getSingleFieldStringValue(doc, SolrConstants.GROUPTYPE);
        } else {
            piField = SolrConstants.PI_TOPSTRUCT;
        }
        String query = "+" + piField + ":\"" + pi + "\" +" + SolrConstants.YEAR + ":\"" + year + "\" +" + SolrConstants.CALENDAR_DAY + ":*";
        logger.debug("Calendar query: {}", query);

        List<String> fieldList = List.of(
                SolrConstants.PI_TOPSTRUCT,
                SolrConstants.CALENDAR_DAY,
                SolrConstants.THUMBPAGENO,
                SolrConstants.LOGID,
                SolrConstants.LABEL);

        SolrDocumentList results = DataManager.getInstance().getSearchIndex().search(query, fieldList);
        logger.trace("Found {} issues.", results.size());

        JSONArray jsonArray = new JSONArray();
        for (SolrDocument result : results) {
            String yearMonthDay = SolrTools.getSingleFieldStringValue(result, SolrConstants.CALENDAR_DAY);
            String label = SolrTools.getSingleFieldStringValue(result, SolrConstants.LABEL);
            String piTopstruct = SolrTools.getSingleFieldStringValue(result, SolrConstants.PI_TOPSTRUCT);
            Integer thumbPageNo = SolrTools.getSingleFieldIntegerValue(result, SolrConstants.THUMBPAGENO);
            String logId = SolrTools.getSingleFieldStringValue(result, SolrConstants.LOGID);

            JSONObject entry = new JSONObject();
            entry.put("date", convertYearMonthDayToIsoDate(yearMonthDay));
            entry.put("label", label != null ? label : "");
            entry.put("url", "/image/" + piTopstruct + "/" + (thumbPageNo != null ? thumbPageNo : 1) + "/" + (logId != null ? logId : "-") + "/");
            jsonArray.put(entry);
        }

        return Response.ok(jsonArray.toString(), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Returns all year-month combinations that have calendar entries for this record as a sorted JSON array
     * of "YYYY-MM" strings. Uses Solr facet query on the YEARMONTH field — no documents loaded, just facet counts.
     *
     * @return JSON array of "YYYY-MM" strings
     * @throws PresentationException if any.
     * @throws IndexUnreachableException if any.
     * @should return sorted year-month list
     * @should return 404 if pi not found
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_CALENDAR_MONTHS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records" }, summary = "Get all year-month combinations that have calendar entries")
    public Response getAvailableMonths() throws PresentationException, IndexUnreachableException {
        logger.trace("getAvailableMonths: {}", pi);

        SolrDocument doc = DataManager.getInstance()
                .getSearchIndex()
                .getFirstDoc("+" + SolrConstants.PI + ":\"" + pi + "\"",
                        List.of(SolrConstants.ISANCHOR, SolrConstants.DOCTYPE, SolrConstants.GROUPTYPE));
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Record not found: " + pi).build();
        }

        String piField;
        if (SolrTools.isAnchor(doc)) {
            piField = SolrConstants.PI_ANCHOR;
        } else if (SolrTools.isGroup(doc)) {
            piField = SolrTools.getSingleFieldStringValue(doc, SolrConstants.GROUPTYPE);
        } else {
            piField = SolrConstants.PI_TOPSTRUCT;
        }
        String query = "+" + piField + ":\"" + pi + "\" +" + SolrConstants.CALENDAR_DAY + ":*";

        QueryResponse qr = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(query, null, List.of(SolrConstants.CALENDAR_MONTH), 1, false);

        JSONArray months = new JSONArray();
        FacetField facetField = qr.getFacetField(SolrConstants.CALENDAR_MONTH);
        if (facetField != null) {
            List<String> sorted = facetField.getValues().stream()
                    .map(FacetField.Count::getName)
                    .sorted()
                    .toList();
            for (String ym : sorted) {
                if (ym.length() == 6) {
                    months.put(ym.substring(0, 4) + "-" + ym.substring(4));
                }
            }
        }

        return Response.ok(months.toString(), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Converts a YEARMONTHDAY value (e.g. "18930301") to an ISO date string (e.g. "1893-03-01").
     *
     * @param yearMonthDay Solr YEARMONTHDAY value in yyyyMMdd format
     * @return ISO date string (yyyy-MM-dd), or the original value if parsing fails
     * @should convert valid yearmonthday to iso date
     * @should return original value if parsing fails
     */
    static String convertYearMonthDayToIsoDate(String yearMonthDay) {
        if (yearMonthDay == null || yearMonthDay.length() != 8) {
            return yearMonthDay;
        }
        try {
            LocalDate date = LocalDate.parse(yearMonthDay, DateTools.FORMATTERISO8601BASICDATE);
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            logger.warn("Unable to parse YEARMONTHDAY value: {}", yearMonthDay);
            return yearMonthDay;
        }
    }
}
