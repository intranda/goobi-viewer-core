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

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.RisResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.export.RISExport;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.swagger.v3.oas.annotations.Operation;

/**
 * <p>
 * SearchResultResource class.
 * </p>
 */
@Path(ApiUrls.SEARCH)
@ViewerRestServiceBinding
public class SearchResultResource {

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
    @javax.ws.rs.Path(RECORDS_RIS_FILE)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "search" }, summary = "Download current search as RIS export file")
    @AccessConditionBinding
    public Response getRISAsFile(@PathParam("query") String query, @PathParam("sortString") String sortString,
            @PathParam("activeFacetString") String activeFacetString, @PathParam("proximitySearchDistance") int proximitySearchDistance)
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
}
