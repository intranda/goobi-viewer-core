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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST_AUTOCOMPLETE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_MANIFEST_SEARCH;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_ANNOTATIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_ANNOTATIONS_PAGE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_COMMENTS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_COMMENTS_PAGE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_MANIFEST;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_RECORD;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.annotation.IAnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchResult;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;
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
import io.goobi.viewer.model.iiif.presentation.v3.builder.ManifestBuilder;
import io.goobi.viewer.model.iiif.search.IIIFSearchBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * @author florian
 *
 */
@jakarta.ws.rs.Path(RECORDS_RECORD)
@ViewerRestServiceBinding
@CORSBinding
public class RecordResource {

    private static final Logger logger = LogManager.getLogger(RecordResource.class);
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
    @jakarta.ws.rs.Path(RECORDS_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 3.0 manifest for record")
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest()
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException, IllegalPathSyntaxException, ContentLibException {
        try {
            Optional<URI> forwardURI = new ManifestBuilder(urls).getExternalManifestURI(pi);
            if (forwardURI.isPresent()) {
                servletResponse.sendRedirect(forwardURI.get().toString());
                return null;
            }
        } catch (IOException e) {
            logger.error("Error forwarding manifest url", e);
        }
        return new ManifestBuilder(urls).build(pi, servletRequest);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a record as annotation collection")
    public IAnnotationCollection getAnnotationsForRecord() throws DAOException, IllegalRequestException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi);
        URI uri = URI.create(apiPath.build());
        //        return new WebAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, false);
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForRecord(pi, uri);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a record as an annotation collection")
    public IAnnotationCollection getCommentsForRecord() throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        URI uri = URI.create(apiPath.build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForRecordComments(pi, uri);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_ANNOTATIONS_PAGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a record as an annotation collection page")
    public AnnotationPage getAnnotationsPageForRecord() throws DAOException, IllegalRequestException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi);
        URI uri = URI.create(apiPath.build());
        //        return new WebAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, false);
        AnnotationPage annoPage = new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForRecord(pi, uri).getFirst();
        if (annoPage != null) {
            return annoPage;
        } else {
            return new AnnotationPage(uri);
        }
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_COMMENTS_PAGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a record as an annotation collection page")
    public IAnnotationCollection getCommentsForRecordPage()
            throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        URI uri = URI.create(apiPath.build());
        AnnotationPage annoPage =
                new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForRecordComments(pi, uri).getFirst();
        if (annoPage != null) {
            return annoPage;
        } else {
            return new AnnotationPage(uri);
        }
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
    @jakarta.ws.rs.Path(RECORDS_MANIFEST_SEARCH)
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
    @jakarta.ws.rs.Path(RECORDS_MANIFEST_AUTOCOMPLETE)
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

}
