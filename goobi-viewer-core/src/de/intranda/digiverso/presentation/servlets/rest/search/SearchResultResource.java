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

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.search.SearchHit;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

@Path("/search")
@ViewerRestServiceBinding
public class SearchResultResource {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @GET
    @Path("/hit/{id}/{numChildren}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SearchHitChildList getTagsForPageJson(@PathParam("id") String hitId, @PathParam("numChildren") int numChildren) throws DAOException,
            PresentationException, IndexUnreachableException, IOException, ViewerConfigurationException {
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
        List<SearchHit> searchHits = searchBean.getCurrentSearch().getHits();
        if (searchHits != null) {
            for (SearchHit searchHit : searchHits) {
                if (hitId.equals(Long.toString(searchHit.getBrowseElement().getIddoc()))) {
                    if (searchHit.getHitsPopulated() < numChildren) {
                        searchHit.populateChildren(numChildren - searchHit.getHitsPopulated(), searchHit.getHitsPopulated(), locale, servletRequest);
                    }
                    Collections.sort(searchHit.getChildren());
                    SearchHitChildList searchHitChildren = new SearchHitChildList(searchHit.getChildren(), searchHit.getHitsPopulated(), searchHit
                            .isHasMoreChildren());
                    return searchHitChildren;
                }
            }
        }

        servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "IDDOC " + hitId + " is not in the current search result set.");
        return null;
    }
}
