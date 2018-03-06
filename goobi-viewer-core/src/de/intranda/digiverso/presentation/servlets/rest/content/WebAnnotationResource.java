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
package de.intranda.digiverso.presentation.servlets.rest.content;

import java.net.MalformedURLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.annotation.Comment;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;

/**
 * Resource for delivering content documents such as ALTO and plain full-text.
 */
@Path("/webannotation")
@ViewerRestServiceBinding
public class WebAnnotationResource {

    private static final Logger logger = LoggerFactory.getLogger(WebAnnotationResource.class);

    private static final String CONTEXT_URI = "http://www.w3.org/ns/anno.jsonld";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    public WebAnnotationResource() {
    }

    /**
     * For testing
     * 
     * @param request
     */
    protected WebAnnotationResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * API method for retrieving any type of content by its relative path within its data repository.
     * 
     * @param pi Record identifier
     * @param page Record page number
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("/comments/{pi}/{page}/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAnnotation(@PathParam("id") Long id)
            throws PresentationException, IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }

        Comment comment = DataManager.getInstance().getDao().getComment(id);
        if (comment == null) {
            throw new ContentNotFoundException("Resource not found");
        }
        String idUrl = new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest))
                .append(servletRequest.getRequestURI().substring(servletRequest.getContextPath().length()))
                .toString();
        String targetUrl = new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest)).append('/')
                .append(PageType.viewImage.getName())
                .append('/')
                .append(comment.getPi())
                .append('/')
                .append(comment.getPage())
                .append('/')
                .toString();
        JSONObject json = new JSONObject();
        json.put("@content", CONTEXT_URI);
        json.put("id", idUrl);
        json.put("creator", comment.getOwner().getDisplayNameObfuscated());
        json.put("created", DateTools.formatterISO8601DateTimeFullWithTimeZone.print(comment.getDateCreated().getTime()));
        if (comment.getDateUpdated() != null) {
            json.put("modified", DateTools.formatterISO8601DateTimeFullWithTimeZone.print(comment.getDateUpdated().getTime()));
        }
        {
            JSONObject body = new JSONObject();
            body.put("type", "TextualBody");
            body.put("format", "text/plain");
            body.put("value", comment.getText());
            json.put("body", body);
        }
        json.put("target", targetUrl);

        return json.toJSONString();
    }

    /**
     * API method for retrieving any type of content by its relative path within its data repository.
     * 
     * @param pi Record identifier
     * @param page Record page number
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws MalformedURLException
     * @throws ContentNotFoundException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("/comments/{pi}/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getAnnotationsForRecord(@PathParam("pi") String pi, @PathParam("page") Integer page)
            throws PresentationException, IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException {
        if (servletResponse != null) {
            servletResponse.addHeader("Access-Control-Allow-Origin", "*");
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }

        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage(pi, page, false);
        if (comments.isEmpty()) {
            throw new ContentNotFoundException("Resource not found");
        }
        JSONArray jsonArray = new JSONArray();
        for (Comment comment : comments) {
            String idUrl = new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest))
                    .append(servletRequest.getRequestURI().substring(servletRequest.getContextPath().length()))
                    .toString();
            String targetUrl = new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest)).append('/')
                    .append(PageType.viewImage.getName())
                    .append('/')
                    .append(comment.getPi())
                    .append('/')
                    .append(comment.getPage())
                    .append('/')
                    .toString();
            JSONObject json = new JSONObject();
            json.put("@content", CONTEXT_URI);
            json.put("id", idUrl);
            json.put("type", "Annotation");
            json.put("creator", comment.getOwner().getDisplayNameObfuscated());
            json.put("created", DateTools.formatterISO8601DateTimeFullWithTimeZone.print(comment.getDateCreated().getTime()));
            if (comment.getDateUpdated() != null) {
                json.put("modified", DateTools.formatterISO8601DateTimeFullWithTimeZone.print(comment.getDateUpdated().getTime()));
            }
            {
                JSONObject body = new JSONObject();
                body.put("type", "TextualBody");
                body.put("format", "text/plain");
                body.put("value", comment.getText());
                json.put("body", body);
            }
            json.put("target", targetUrl);

            jsonArray.add(json);
        }

        return jsonArray.toJSONString();
    }
}
