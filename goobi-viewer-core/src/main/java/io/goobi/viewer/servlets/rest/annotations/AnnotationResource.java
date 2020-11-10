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
package io.goobi.viewer.servlets.rest.annotations;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollectionBuilder;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.iiif.presentation.builder.AbstractBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.OpenAnnotationBuilder;

/**
 * <p>
 * AnnotationResource class.
 * </p>
 *
 * @author florian
 */
@Path("/annotations")
public class AnnotationResource {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private AbstractApiUrlManager urls;
    private AnnotationsResourceBuilder annoBuilder;
    
    public AnnotationResource() {
        annoBuilder = new AnnotationsResourceBuilder(urls);
    }
    
    /**
     * <p>
     * getAnnotation.
     * </p>
     *
     * @param id a {@link java.lang.Long} object.
     * @param type a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.annotation.IAnnotation} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     * @throws IndexUnreachableException 
     * @throws PresentationException 
     */
    @GET
    @Path("/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public IAnnotation getAnnotation(@PathParam("id") Long id, @QueryParam("type") String type)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException, PresentationException, IndexUnreachableException {

        PersistentAnnotation data = DataManager.getInstance().getDao().getAnnotation(id);
        if(data != null) {
            IAnnotation anno;
            if ("OpenAnnotation".equalsIgnoreCase(type) || "oa".equalsIgnoreCase(type)) {
                anno = annoBuilder.getAsOpenAnnotation(data);
            } else {
                anno = annoBuilder.getAsWebAnnotation(data);
            }
            return anno;
        } else { 
            OpenAnnotationBuilder builder = new OpenAnnotationBuilder(new ApiUrls(DataManager.getInstance().getConfiguration().getRestApiUrl())) {};
            IAnnotation anno = builder.getCrowdsourcingAnnotation(id.toString());
            if(anno != null) {                
                return anno;
            }
        } 
        throw new NotFoundException("No annotation with id " + id + " found");

    }

    /**
     * <p>
     * getOpenAnnotation.
     * </p>
     *
     * @param type a {@link java.lang.String} object.
     * @param id a {@link java.lang.Long} object.
     * @return a {@link de.intranda.api.annotation.IAnnotation} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    @GET
    @Path("/{type}/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public IAnnotation getOpenAnnotation(@PathParam("type") String type, @PathParam("id") Long id)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        PersistentAnnotation data = DataManager.getInstance().getDao().getAnnotation(id);

        IAnnotation anno;
        if ("OpenAnnotation".equalsIgnoreCase(type) || "oa".equalsIgnoreCase(type)) {
            anno = annoBuilder.getAsOpenAnnotation(data);
        } else {
            anno = annoBuilder.getAsWebAnnotation(data);
        }

        return anno;
    }

    /**
     * <p>
     * getAnnotationsForPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param pageString a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.annotation.wa.collection.AnnotationPage} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    @GET
    @Path("/collection/{pi}/{page}/")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public AnnotationPage getAnnotationsForPage(@PathParam("pi") String pi, @PathParam("page") String pageString)
            throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {

        Integer page = StringUtils.isBlank(pageString.replace("-", "")) ? null : Integer.parseInt(pageString);

        List<PersistentAnnotation> data = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, page);

        AnnotationCollectionBuilder builder = new AnnotationCollectionBuilder(URI.create(servletRequest.getRequestURL().toString()), data.size());
        AnnotationPage annoPage = builder.buildPage(data.stream().map(annoBuilder::getAsOpenAnnotation).collect(Collectors.toList()), 1);

        return annoPage;
    }

}
