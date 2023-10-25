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
package io.goobi.viewer.api.rest.v1.annotations;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ANNOTATION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_COMMENT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.IncomingAnnotation;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.v2.Canvas2;
import de.intranda.api.iiif.presentation.v2.Manifest2;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ServiceNotAllowedException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.NotImplementedException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationDeleter;
import io.goobi.viewer.model.security.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(ANNOTATIONS)
@ViewerRestServiceBinding
public class AnnotationResource {

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final AbstractApiUrlManager urls;

    public AnnotationResource() {
        this.urls = DataManager.getInstance().getRestApiManager().getContentApiManager().orElse(null);
    }

    /**
     *
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get an annotation collection over all annotations")
    public AnnotationCollection getAnnotationCollection() throws PresentationException, IndexUnreachableException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotationCollection();
    }

    /**
     *
     * @param page
     * @return
     * @throws DAOException
     * @throws ContentLibException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @javax.ws.rs.Path("/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get a page within the annotation collection over all annotations")
    @ApiResponse(responseCode = "400", description = "If the page number is out of bounds")
    public AnnotationPage getAnnotationCollectionPage(@PathParam("page") Integer page) throws ContentLibException, DAOException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotationPage(page);
    }

    /**
     *
     * @param id
     * @return
     * @throws DAOException
     * @throws ContentLibException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @javax.ws.rs.Path(ANNOTATIONS_ANNOTATION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get an annotation by its identifier")
    @ApiResponse(responseCode = "404", description = "If the page number is out of bounds")
    public IAnnotation getAnnotation(@Parameter(description = "Identifier of the annotation") @PathParam("id") Long id)
            throws DAOException, ContentLibException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getWebAnnotation(id).orElseThrow(() -> new ContentNotFoundException("Not annotation with id = " + id + "found"));
    }

    /**
     *
     * @param id
     * @return
     * @throws DAOException
     * @throws ContentLibException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    @GET
    @javax.ws.rs.Path(ANNOTATIONS_COMMENT)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Get an annotation by its identifier")
    @ApiResponse(responseCode = "404", description = "If the page number is out of bounds")
    public IAnnotation getComment(@Parameter(description = "Identifier of the annotation") @PathParam("id") Long id)
            throws DAOException, ContentLibException {
        AnnotationsResourceBuilder builder = new AnnotationsResourceBuilder(urls, servletRequest);
        return builder.getCommentWebAnnotation(id).orElseThrow(() -> new ContentNotFoundException("Not annotation with id = " + id + "found"));
    }

    /**
     *
     * @param anno
     * @return
     * @throws DAOException
     * @throws NotImplementedException
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Create a new annotation")
    @ApiResponse(responseCode = "501",
            description = "Persisting this king of annotation or its target is not implemented. Only W3C Web Annotations targeting a manifest, canvas or part of a canvas may be persisted")
    public IAnnotation addAnnotation(IncomingAnnotation anno) throws DAOException, NotImplementedException {
        AnnotationConverter converter = new AnnotationConverter(urls);
        CrowdsourcingAnnotation pAnno = createPersistentAnnotation(anno);
        if (pAnno != null) {
            DataManager.getInstance().getDao().addAnnotation(pAnno);
            return converter.getAsWebAnnotation(pAnno);
        }
        throw new NotImplementedException();
    }

    /**
     *
     * @param id
     * @return
     * @throws DAOException
     * @throws ContentLibException
     * @throws ViewerConfigurationException
     */
    @DELETE
    @Path(ANNOTATIONS_ANNOTATION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "annotations" }, summary = "Delete an existing annotation")
    @ApiResponse(responseCode = "200", description = "Return the deleted annotation")
    @ApiResponse(responseCode = "404", description = "Annotation not found by the given id")
    @ApiResponse(responseCode = "405", description = "May not delete the annotation because it was created by another user")
    public IAnnotation deleteAnnotation(@Parameter(description = "Identifier of the annotation") @PathParam("id") Long id)
            throws DAOException, ContentLibException {
        AnnotationConverter converter = new AnnotationConverter(urls);
        CrowdsourcingAnnotation pAnno = DataManager.getInstance().getDao().getAnnotation(id);
        if (pAnno == null) {
            throw new ContentNotFoundException();
        }

        IAnnotation anno = converter.getAsWebAnnotation(pAnno);
        User creator = pAnno.getCreator();
        if (creator != null) {
            User user = getUser();
            if (user == null) {
                throw new ServiceNotAllowedException("May not delete annotations made by a user if not logged in");
            } else if (!user.getId().equals(creator.getId()) && !user.isSuperuser()) {
                throw new ServiceNotAllowedException("May not delete annotations made by another user if not logged in as admin");
            } else {
                try {
                    new SqlAnnotationDeleter(DataManager.getInstance().getDao()).delete(pAnno);
                } catch (IOException e) {
                    throw new DAOException(e.toString());
                }
            }
        }

        return anno;
    }

    /**
     *
     * @param anno
     * @param builder
     * @return
     */
    public CrowdsourcingAnnotation createPersistentAnnotation(IAnnotation anno) {
        CrowdsourcingAnnotation pAnno = null;
        IResource target = anno.getTarget();
        String template;
        if (target instanceof Manifest2) {
            template = urls.path(RECORDS_RECORD, RECORDS_MANIFEST).build();
        } else if (target instanceof Canvas2) {
            template = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).build();
        } else if (target instanceof SpecificResource) {
            //assume specific resources are on a canvas
            template = urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).build();
        } else {
            //TODO: implement handling IIIF 3 resources
            return null;//not implemented
        }

        String pi = urls.parseParameter(template, target.getId().toString(), "pi");
        String pageNoString = urls.parseParameter(template, target.getId().toString(), "pageNo");
        Integer pageNo = null;
        if (StringUtils.isNotBlank(pageNoString) && pageNoString.matches("\\d+")) {
            pageNo = Integer.parseInt(pageNoString);
        }
        pAnno = new CrowdsourcingAnnotation((AbstractAnnotation) anno, null, pi, pageNo);
        User user = getUser();
        if (user != null) {
            pAnno.setCreator(user);
        }
        return pAnno;
    }

    /**
     *
     * @return
     */
    public User getUser() {
        UserBean userBean = BeanUtils.getUserBeanFromRequest(servletRequest);
        if (userBean != null) {
            return userBean.getUser();
        }
        return null;
    }

}
