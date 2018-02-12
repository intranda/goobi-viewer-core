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
package de.intranda.digiverso.presentation.servlets.rest.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.search.SearchFacets;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

@Path("/search")
@ViewerRestServiceBinding
public class SearchHitsNotificationResource {

    private static final Logger logger = LoggerFactory.getLogger(SearchHitsNotificationResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @GET
    @Path("/cronjob/sendnotifications/")
    @Produces({ MediaType.APPLICATION_JSON })
    public void sendNewHitsNotifications() throws DAOException, PresentationException, IndexUnreachableException {
        logger.trace("sendNewHitsNotifications");
        Map<String, String> filters = new HashMap<>();
        filters.put("newHitsNotification", "1");
        long searchCount = DataManager.getInstance()
                .getDao()
                .getSearchCount(null, filters);
        logger.info("Found {} saved searches with notifications enabled.", searchCount);
        int pageSize = 100;

        for (int i = 0; i < searchCount; i += pageSize) {
            logger.debug("Getting searches {}-{}", i, i + pageSize);
            List<Search> searches = DataManager.getInstance()
                    .getDao()
                    .getSearches(null, i, pageSize, null, false, filters);
            for (Search search : searches) {
                // TODO filters for each user
                SearchFacets facets = new SearchFacets();
                facets.setCurrentFacetString(search.getFacetString());
                facets.setCurrentHierarchicalFacetString(search.getHierarchicalFacetString());
                search.execute(facets, null, 100, 0, null);
                if (search.getHitsCount() > search.getLastHitsCount()) {
                    // Send notification
                }
            }
        }
    }
}
