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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_COMMENTS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST_AUTOCOMPLETE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST_SEARCH;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_RECORD;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchResult;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.resourcebuilders.AnnotationsResourceBuilder;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.iiif.presentation.v2.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.v2.builder.WebAnnotationBuilder;
import io.goobi.viewer.model.iiif.search.IIIFSearchBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_RECORD)
@ViewerRestServiceBinding
@CORSBinding
public class RecordResource {

    private static final Logger logger = LoggerFactory.getLogger(RecordResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    private final String pi;


    public RecordResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a record")
    public IAnnotationCollection getAnnotationsForRecord(
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations, otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws PresentationException, IndexUnreachableException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi);
        URI uri = URI.create(apiPath.build());
        return new WebAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, false, servletRequest);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a record")
    public IAnnotationCollection getCommentsForRecord(
            @Parameter(
                    description = "annotation format of the response. If it is 'oa' the comments will be delivered as OpenAnnotations, otherwise as W3C-Webannotations") @QueryParam("format") String format)
            throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        URI uri = URI.create(apiPath.build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForRecordComments(pi, uri);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_COMMENTS + "/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    @ApiResponse(responseCode = "400", description = "If the page number is out of bounds")
    public AnnotationPage getCommentPageForRecord(@PathParam("page") Integer page)
            throws DAOException, IllegalRequestException {

        URI uri = URI.create(urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi).build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationPageForRecordComments(pi, uri, page);
    }


    @GET
    @javax.ws.rs.Path(RECORDS_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF manifest for record")
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest(
            @Parameter(
                    description = "Build mode for manifest to select type of resources to include. Default is 'iiif' which returns the full IIIF manifest with all resources. 'thumbs' Does not read width and height of canvas resources and 'iiif_simple' ignores all resources from files") @QueryParam("mode") String mode)
            throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException {
//        IIIFPresentation3ResourceBuilder builder = new IIIFPresentation3ResourceBuilder(urls, servletRequest);
//        BuildMode buildMode = getBuildeMode(mode);
//        return builder.getManifest(pi, buildMode);
        return null;
    }


    /**
     * Endpoint for IIIF Search API service in a manifest. Depending on the given motivation parameters, fulltext (motivation=painting), user comments
     * (motivation=commenting) and general (crowdsourcing-) annotations (motivation=describing) may be searched.
     *
     * @param pi The pi of the manifest to search
     * @param query The search query; a list of space separated terms. The search is for all complete words which match any of the query terms. Terms
     *            may contain the wildcard charachter '*' to represent an arbitrary number of characters within the word
     * @param motivation a space separated list of motivations of annotations to search for. Search for the following motivations is implemented:
     *            <ul>
     *            <li>painting: fulltext resources</li>
     *            <li>non-painting: all supported resources except fulltext</li>
     *            <li>commenting: user comments</li>
     *            <li>describing: Crowdsourced or other general annotations</li>
     *            </ul>
     * @param date not supported. If this parameter is given, it will be included in the 'ignored' property of the 'within' property of the answer
     * @param user not supported. If this parameter is given, it will be included in the 'ignored' property of the 'within' property of the answer
     * @param page the page number for paged result sets. if this is empty, page=1 is assumed
     * @return a {@link de.intranda.api.iiif.search.SearchResult} containing all annotations matching the query in the 'resources' property
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_MANIFEST_SEARCH)
    @Produces({ MediaType.APPLICATION_JSON })
    public SearchResult searchInManifest(@PathParam("pi") String pi, @QueryParam("q") String query, @QueryParam("motivation") String motivation,
            @QueryParam("date") String date, @QueryParam("user") String user, @QueryParam("page") Integer page)
            throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(urls, query, pi, servletRequest).setMotivation(motivation).setDate(date).setUser(user).setPage(page).build();
    }

    /**
     * <p>
     * autoCompleteInManifest.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param query a {@link java.lang.String} object.
     * @param motivation a {@link java.lang.String} object.
     * @param date a {@link java.lang.String} object.
     * @param user a {@link java.lang.String} object.
     * @param page a {@link java.lang.Integer} object.
     * @return a {@link de.intranda.api.iiif.search.AutoSuggestResult} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_MANIFEST_AUTOCOMPLETE)
    @Produces({ MediaType.APPLICATION_JSON })
    public AutoSuggestResult autoCompleteInManifest(@PathParam("pi") String pi, @QueryParam("q") String query,
            @QueryParam("motivation") String motivation, @QueryParam("date") String date, @QueryParam("user") String user,
            @QueryParam("page") Integer page) throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(urls, query, pi, servletRequest).setMotivation(motivation)
                .setDate(date)
                .setUser(user)
                .setPage(page)
                .buildAutoSuggest();
    }

    /**
     * @param mode
     * @return
     */
    public static BuildMode getBuildeMode(String mode) {
        if (StringUtils.isNotBlank(mode)) {
            switch (mode.toLowerCase()) {
                case "iiif-simple":
                case "iiif_simple":
                case "simple":
                    return BuildMode.IIIF_SIMPLE;
                case "iiif-thumbs":
                case "iiif_thumbs":
                case "thumbs":
                case "thumbnails":
                    return BuildMode.THUMBS;
                default:
                    return BuildMode.IIIF;
            }
        }
        return BuildMode.IIIF;
    }

}
