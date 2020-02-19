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
package io.goobi.viewer.servlets.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * <p>
 * SingleWorkCollectionRedirect class.
 * </p>
 */
@Path("/redirect/toFirstWork")
@ViewerRestServiceBinding
public class SingleWorkCollectionRedirect {

    @Context
    private HttpServletRequest servletRequest;

    /**
     * <p>
     * redirectToWork.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a {@link javax.ws.rs.core.Response} object.
     */
    @GET
    @Path("/{luceneField}/{fieldValue}")
    public Response redirectToWork(@PathParam("luceneField") String field, @PathParam("fieldValue") String value, @Context HttpServletRequest request,
            @Context HttpServletResponse response) {
        try {
            String url = SearchHelper.getFirstWorkUrlWithFieldValue(field, value, true, true,
                    DataManager.getInstance().getConfiguration().getCollectionSplittingChar(field), Locale.getDefault());
            URI uri = new URI(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest) + url);
            HttpServletResponse httpResponse = response;
            httpResponse.sendRedirect(uri.toString());
            return Response.ok().build();
            //            return Response.temporaryRedirect(uri).build();
            //            return (Response) response;
        } catch (URISyntaxException | IOException e) {
            return null;
        } catch (IndexUnreachableException e) {
            return null;
        } catch (PresentationException e) {
            return null;
        }
    }

}
