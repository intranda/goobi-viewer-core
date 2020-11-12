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
package io.goobi.viewer.servlets.rest.iiif.presentation;

import static io.goobi.viewer.api.rest.v1.ApiUrls.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.faces.config.rules.ApplicationRule;

import de.intranda.api.annotation.oa.Motivation;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.Collection;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Layer;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.Range;
import de.intranda.api.iiif.presentation.Sequence;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchResult;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.api.rest.v1.records.RecordPageResource;
import io.goobi.viewer.api.rest.v1.records.RecordResource;
import io.goobi.viewer.api.rest.v1.records.RecordSectionResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.iiif.presentation.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.builder.LayerBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.SequenceBuilder;
import io.goobi.viewer.model.iiif.presentation.builder.StructureBuilder;
import io.goobi.viewer.model.iiif.search.IIIFSearchBuilder;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.rest.content.ContentResource;

/**
 * <p>
 * ManifestResource class.
 * </p>
 *
 *@deprecated use iiif presentation calls in {@link RecordResource}, {@link RecordSectionResource} and {@link RecordPageResource} instead
 *
 * @author Florian Alpers
 */
@Path("/iiif/manifests")
@ViewerRestServiceBinding
@CORSBinding
@Deprecated
public class ManifestResource extends AbstractResource {

    private static Logger logger = LoggerFactory.getLogger(ManifestResource.class);

    /**
     * Default constructor
     */
    public ManifestResource() {
        super();
    }

    /**
     * Unit test constructor injecting request and response
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     */
    public ManifestResource(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /**
     * forwards to {@link #getManifest(String)}
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param pi a {@link java.lang.String} object.
     * @return a {@link javax.ws.rs.core.Response} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws org.apache.commons.configuration.ConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @Path("/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response geManifestAlt(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("pi") String pi)
            throws PresentationException, IndexUnreachableException, URISyntaxException, ConfigurationException, DAOException {
        URI canonical = URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();

    }

    /**
     * Returns the entire IIIF manifest for the given pi. If the given pi points to an anchor, a IIIF collection is returned instead
     *
     * @param pi a {@link java.lang.String} object.
     * @return The manifest or collection
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path("/{pi}/manifest")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getManifest(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {
        URI canonical = URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();
    }

    /**
     * Returns the entire IIIF manifest for the given pi, excluding all "seeAlso" references and annotation lists other than the images themselves. If
     * the given pi points to an anchor, a IIIF collection is returned instead
     *
     * @param pi a {@link java.lang.String} object.
     * @return The manifest or collection
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path("/{pi}/manifest/simple")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getManifestSimple(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {
        URI canonical = URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).query("mode", "simple").build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();
    }

    /**
     * Returns the entire IIIF manifest for the given pi without the sequence and structure lists. If the given pi points to an anchor, a IIIF
     * collection is returned instead
     *
     * @param pi a {@link java.lang.String} object.
     * @return The manifest or collection
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path("/{pi}/manifest/base")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getManifestBase(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {
        URI canonical = URI.create(urls.path(RECORDS_RECORD, RECORDS_MANIFEST).params(pi).query("mode", "base").build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();
    }


    /**
     * Creates A IIIF sequence containing all pages belonging to the given pi
     *
     * @param pi a {@link java.lang.String} object.
     * @return A IIIF sequence with all pages of the book (if applicable)
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path("/{pi}/sequence/basic")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getBasicSequence(@PathParam("pi") String pi) throws PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException, IllegalRequestException, ContentNotFoundException {
        URI canonical = URI.create(urls.path(RECORDS_PAGES, RECORDS_PAGES_SEQUENCE).params(pi).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();

    }

    /**
     * Creates a IIIF range for the structural element denoted by the given pi and logid
     *
     * @param pi The pi of the containing work
     * @param logId The METS logid of the structural element to return
     * @return A IIIF range
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path("/{pi}/range/{logId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getRange(@PathParam("pi") String pi, @PathParam("logId") String logId) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {
        URI canonical = URI.create(urls.path(RECORDS_SECTIONS, RECORDS_SECTIONS_RANGE).params(pi, logId).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();
    }

    /**
     * Creates a canvas for the page with the given pyhsPageNo (order) within the work with the given pi
     *
     * @param pi The pi of the containing work
     * @param physPageNo The physical ordering of the page (1-based)
     * @return A IIIF canvas
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path("/{pi}/canvas/{physPageNo}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getCanvas(@PathParam("pi") String pi, @PathParam("physPageNo") int physPageNo) throws PresentationException,
            IndexUnreachableException, URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException {
        URI canonical = URI.create(urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).params(pi, physPageNo).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();
    }

    /**
     * Creates a layer containing all annnotations of the given {@link AnnotationType type} for the work with the given pi. The annotations are groupd
     * into annotation lists by page, if they belong to a page. Otherwise they are grouped in a single annotation list
     *
     * @param pi The pi of the containing work
     * @param typeName The name of the {@link de.intranda.api.iiif.presentation.enums.AnnotationType} for which annotations should be returned
     * @return A IIIF layer
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException if any.
     * @throws java.io.IOException if any.
     */
    @GET
    @Path("/{pi}/layer/{type}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getLayer(@PathParam("pi") String pi, @PathParam("type") String typeName) throws PresentationException, IndexUnreachableException,
            URISyntaxException, ViewerConfigurationException, DAOException, ContentNotFoundException, IllegalRequestException, IOException {
        URI canonical = URI.create(urls.path(RECORDS_PAGES, RECORDS_LAYER).params(pi, typeName).build());
        return Response.status(Status.MOVED_PERMANENTLY).location(canonical).build();
    }


}
