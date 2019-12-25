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
package io.goobi.viewer.servlets.rest.content;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationCollectionBuilder;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.iiif.presentation.builder.SequenceBuilder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * Resource for delivering content documents such as ALTO and plain full-text.
 */
@Path("/webannotation")
@ViewerRestServiceBinding
public class WebAnnotationResource {

    private static final Logger logger = LoggerFactory.getLogger(WebAnnotationResource.class);

    private static final String CONTEXT_URI = "http://www.w3.org/ns/anno.jsonld";
    private static final String GENERATOR_URI = "https://www.intranda.com/en/digiverso/goobi-viewer/goobi-viewer-overview/";

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>Constructor for WebAnnotationResource.</p>
     */
    public WebAnnotationResource() {
    }

    /**
     * For testing
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    protected WebAnnotationResource(HttpServletRequest request) {
        this.servletRequest = request;
    }

    /**
     * Returns an annotation for the comment with the given database ID.
     *
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws java.net.MalformedURLException
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     * @param id a {@link java.lang.Long} object.
     * @return a {@link de.intranda.api.annotation.IAnnotation} object.
     */
    @GET
    @Path("comments/{pi}/{page}/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public IAnnotation getAnnotation(@PathParam("id") Long id)
            throws PresentationException, IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException {
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }

        Comment comment = DataManager.getInstance().getDao().getComment(id);
        if (comment == null) {
            throw new ContentNotFoundException("Resource not found");
        }
        
        WebAnnotation anno = createAnnotation(comment);
        
        return anno;
    }

    /**
     * @param comment
     * @return
     * @throws URISyntaxException
     */
    private WebAnnotation createAnnotation(Comment comment) {
            URI resourceId = URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl() + "webannotation/comments/" + comment.getPi() + "/" + comment.getPage() + "/" + comment.getId());
            WebAnnotation anno = new WebAnnotation(resourceId);
            anno.setBody(new TextualResource(comment.getText()));
            anno.setTarget(new SimpleResource(new SequenceBuilder(servletRequest).getCanvasURI(comment.getPi(), comment.getPage())));
            anno.setCreated(comment.getDateCreated());
            anno.setModified(comment.getDateUpdated());
            anno.setCreator(createAgent(comment.getOwner()));
            anno.setGenerator(new Agent(URI.create(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest)), AgentType.SOFTWARE, "Goobi viewer"));
            return anno;
    }

    /**
     * @param owner
     * @return
     */
    private Agent createAgent(User owner) {
            Agent agent = new Agent(URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl() + "users/" + owner.getId()), AgentType.PERSON, owner.getDisplayName());
            return agent;
    }

    /**
     * Returns an annotation collection containing comment annotations for the given record page.
     *
     * @param pi Record identifier
     * @param page Record page number
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws java.net.MalformedURLException
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException
     * @throws java.net.URISyntaxException
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     * @return a {@link de.intranda.api.annotation.wa.collection.AnnotationCollection} object.
     */
    @GET
    @Path("comments/{pi}/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public AnnotationCollection getAnnotationsForPage(@PathParam("pi") String pi, @PathParam("page") Integer page)
            throws PresentationException, DAOException, MalformedURLException, ContentNotFoundException, URISyntaxException, ViewerConfigurationException {
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }

        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage(pi, page, false);
        
        URI resourceId = URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl() + "webannotation/comments/" + pi + "/" + page);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(resourceId, comments.size())
                .setLabel(ViewerResourceBundle.getTranslations("userComments"))
                .setItemsPerPage(comments.size())
                .setPageUrlPrefix("page");
        AnnotationCollection collection = builder.buildCollection();
        AnnotationPage first = builder.buildPage(comments.stream().map(this::createAnnotation).collect(Collectors.toList()), 1);
        collection.setFirst(first);
        
        return collection;
    }

    /**
     * Returns an annotation collection containing comment annotations for the given record.
     *
     * @param pi Record identifier
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws java.net.MalformedURLException
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException
     * @throws java.net.URISyntaxException
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException
     * @should return document correctly
     * @should throw ContentNotFoundException if file not found
     * @return a {@link de.intranda.api.annotation.wa.collection.AnnotationCollection} object.
     */
    @GET
    @Path("comments/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public AnnotationCollection getAnnotationsForRecord(@PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, DAOException, MalformedURLException, ContentNotFoundException, URISyntaxException, ViewerConfigurationException {
        

        
        if (servletResponse != null) {
            servletResponse.setCharacterEncoding(Helper.DEFAULT_ENCODING);
        }

        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork(pi, false);
        
        URI resourceId = URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl() + "webannotation/comments/" + pi);
        
        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(resourceId, comments.size())
                .setLabel(ViewerResourceBundle.getTranslations("userComments"))
                .setItemsPerPage(comments.size())
                .setPageUrlPrefix("page");
        AnnotationCollection collection = builder.buildCollection();
        AnnotationPage first = builder.buildPage(comments.stream().map(this::createAnnotation).collect(Collectors.toList()), 1);
        collection.setFirst(first);
        
        return collection;
    }

    /**
     * 
     * @param comment
     * @param servletRequest
     * @return
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    static JSONObject generateCommentAnnotation(Comment comment, HttpServletRequest servletRequest) {
        if (comment == null) {
            throw new IllegalArgumentException("comment may not be null");
        }
        if (servletRequest == null) {
            throw new IllegalArgumentException("servletRequest may not be null");
        }

        String idUrl = new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest))
                .append(servletRequest.getRequestURI().substring(servletRequest.getContextPath().length()))
                .toString();
        String targetUrl = new StringBuilder(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest)).append('/')
                .append(PageType.viewObject.getName())
                .append('/')
                .append(comment.getPi())
                .append('/')
                .append(comment.getPage())
                .append('/')
                .toString();

        JSONObject json = new JSONObject();
        json.put("@context", CONTEXT_URI);
        json.put("id", idUrl);
        json.put("creator", comment.getOwner().getDisplayNameObfuscated());
        json.put("created", DateTools.formatterISO8601DateTimeFullWithTimeZone.print(comment.getDateCreated().getTime()));
        if (comment.getDateUpdated() != null) {
            json.put("modified", DateTools.formatterISO8601DateTimeFullWithTimeZone.print(comment.getDateUpdated().getTime()));
        }
        json.put("generator", GENERATOR_URI);
        {
            JSONObject body = new JSONObject();
            body.put("type", "TextualBody");
            body.put("format", "text/plain");
            body.put("value", comment.getText());
            json.put("body", body);
        }
        json.put("target", targetUrl);

        return json;
    }
}
