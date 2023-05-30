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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.search.SearchHitChildList;
import io.goobi.viewer.api.rest.resourcebuilders.RisResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.export.RISExport;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchFacets;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.swagger.v3.oas.annotations.Operation;

/**
 * <p>
 * SearchResultResource class.
 * </p>
 */
@Path(ApiUrls.SEARCH)
@ViewerRestServiceBinding
public class SearchResultResource {

    private static final Logger logger = LogManager.getLogger(SearchResultResource.class); //NOSONAR Sometimes used for debugging

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * getTagsForPageJson.
     * </p>
     *
     * @param hitId a {@link java.lang.String} object.
     * @param numChildren a int.
     * @return a {@link io.goobi.viewer.servlets.rest.search.SearchHitChildList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    @GET
    @Path(ApiUrls.SEARCH_HIT_CHILDREN)
    @Produces({ MediaType.APPLICATION_JSON })
    public SearchHitChildList getTagsForPageJson(@PathParam("id") String hitId, @PathParam("numChildren") int numChildren)
            throws DAOException, PresentationException, IndexUnreachableException, IOException, ViewerConfigurationException {
        // logger.trace("/search/hit/{}/{}/", hitId, numChildren); //NOSONAR Sometimes used for debugging
        SearchBean searchBean = BeanUtils.getSearchBean();
        if (searchBean == null) {
            servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "No instance of SearchBean found in the user session. Execute a search first.");
            return null;
        }
        Locale locale = null;
        NavigationHelper nh = BeanUtils.getNavigationHelper();
        if (nh != null) {
            locale = nh.getLocale();
        }

        for (SearchResultGroup resultGroup : searchBean.getCurrentSearch().getResultGroups()) {
            SearchHitChildList ret = getSearchHitChildren(resultGroup.getHits(), hitId, numChildren, locale, servletRequest);
            if (ret != null) {
                return ret;
            }
        }

        servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "IDDOC " + hitId + " is not in the current search result set.");
        return null;
    }

    /**
     * 
     * @param hitId IDDOC of the main search hit
     * @param numChildren Number of child hits to load
     * @param resultGroupName Requested result group name
     * @return
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws IOException
     * @throws ViewerConfigurationException
     */
    @GET
    @Path(ApiUrls.SEARCH_HIT_CHILDREN_GROUP)
    @Produces({ MediaType.APPLICATION_JSON })
    public SearchHitChildList getTagsForPageJson(@PathParam("id") String hitId, @PathParam("numChildren") int numChildren,
            @PathParam("resultGroup") String resultGroupName)
            throws DAOException, PresentationException, IndexUnreachableException, IOException, ViewerConfigurationException {
        // logger.trace("/search/hit/{}/{}/", hitId, numChildren); //NOSONAR Sometimes used for debugging
        SearchBean searchBean = BeanUtils.getSearchBean();
        if (searchBean == null) {
            servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "No instance of SearchBean found in the user session. Execute a search first.");
            return null;
        }
        Locale locale = null;
        NavigationHelper nh = BeanUtils.getNavigationHelper();
        if (nh != null) {
            locale = nh.getLocale();
        }

        SearchResultGroup group = null;
        for (SearchResultGroup g : searchBean.getCurrentSearch().getResultGroups()) {
            if (g.getName().equals(resultGroupName)) {
                group = g;
                break;
            }
        }

        if (group == null) {
            servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Result group configuration '" + resultGroupName + "' was not found.");
            return null;
        }

        List<SearchHit> searchHits = group.getHits();
        if (searchHits != null) {
            return getSearchHitChildren(searchHits, hitId, numChildren, locale, servletRequest);
        }

        servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "IDDOC " + hitId + " is not in the current search result set.");
        return null;
    }

    /**
     * 
     * @param searchHits
     * @param hitId
     * @param numChildren
     * @param locale
     * @param servletRequest
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @should return null if searchHits null
     */
    static SearchHitChildList getSearchHitChildren(List<SearchHit> searchHits, String hitId, int numChildren, Locale locale,
            HttpServletRequest servletRequest)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (searchHits == null) {
            return null;
        }
        for (SearchHit searchHit : searchHits) {
            if (hitId.equals(Long.toString(searchHit.getBrowseElement().getIddoc()))) {
                // logger.trace("found: {}", hitId); //NOSONAR Sometimes used for debugging
                if (searchHit.getHitsPopulated() < numChildren) {
                    searchHit.populateChildren(numChildren - searchHit.getHitsPopulated(), searchHit.getHitsPopulated(), locale, servletRequest);
                }
                Collections.sort(searchHit.getChildren());
                return new SearchHitChildList(searchHit.getChildren(), searchHit.getHitsPopulated(), searchHit.isHasMoreChildren());
            }
        }

        return null;
    }

    @GET
    @javax.ws.rs.Path(RECORDS_RIS_FILE)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "search" }, summary = "Download current search as RIS export file")
    @AccessConditionBinding
    public String getRISAsFile(@PathParam("query") String query, @PathParam("sortString") String sortString,
            @PathParam("activeFacetString") String activeFacetString, @PathParam("proximitySearchDistance") int proximitySearchDistance)
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException, ViewerConfigurationException {
        String currentQuery = SearchHelper.prepareQuery(query);
        String finalQuery = SearchHelper.buildFinalQuery(currentQuery, true, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        Locale locale = Locale.ENGLISH;
        // navigationHelper.getLocale(); // TODO

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
        return null;
    }
}
