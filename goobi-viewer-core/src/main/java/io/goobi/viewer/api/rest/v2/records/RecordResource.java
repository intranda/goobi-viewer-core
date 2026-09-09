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
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.v3.Collection3;
import de.intranda.api.iiif.presentation.v3.Manifest3;
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
import io.goobi.viewer.faces.validators.PIValidator;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * REST resource providing IIIF Presentation v3 manifests and annotations for digitized records.
 *
 * @author Florian Alpers
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
        // Reject PIs that contain characters illegal in file-system URI paths before any
        // file-system access occurs, to prevent ContentLib path injection (e.g. space, pipe,
        // null byte via double-encoding). BadRequestException (HTTP 400, unchecked
        // WebApplicationException) is used so that Jersey maps it to 400 before invoking the endpoint.
        validatePi(pi);
        this.pi = pi;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
    }

    /**
     * Validates the PI path parameter using {@link PIValidator#validatePi(String)}.
     *
     * <p>Delegates to the central PI validator to keep validation logic in one place.
     * Throws BadRequestException (HTTP 400, unchecked WebApplicationException) so that
     * Jersey maps it to a 400 response regardless of where it is thrown (constructor or method).
     *
     * @param pi the persistent identifier to validate; null or blank is rejected
     * @throws BadRequestException if the PI is null, blank, or contains illegal characters
     */
    static void validatePi(String pi) {
        if (!PIValidator.validatePi(pi)) {
            throw new BadRequestException("Invalid record identifier: " + pi);
        }
    }

    /**
     * Returns the IIIF Presentation 3.0 manifest (or collection, for anchor records) for the record.
     *
     * <p>When the record is configured to delegate to an external IIIF service, the response is instead an HTTP
     * redirect to that service's manifest URL rather than a locally generated manifest. The manifest embeds the
     * record's crowdsourcing annotations and user comments as annotation pages, hiding the annotation items behind
     * an authorization service when the requester lacks the record's user-generated-content view privilege.
     *
     * @return the generated manifest or collection, or {@code null} when a redirect to an external manifest was issued instead
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_MANIFEST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 3.0 manifest for record",
            description = "When the record is configured to delegate to an external IIIF service, the response is an HTTP redirect to"
                    + " that service's manifest instead of a locally generated one. Anchor records (the parent of a multi-volume work)"
                    + " return a IIIF collection with the volumes added as manifests; the manifest embeds the record's crowdsourcing"
                    + " annotations and comments, hiding the annotation items behind an authorization service when the requester lacks"
                    + " the record's user-generated-content view privilege.")
    @ApiResponse(responseCode = "200", description = "IIIF 3.0 manifest of the record, or a collection for anchor records",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(oneOf = { Manifest3.class, Collection3.class })))
    @IIIFPresentationBinding
    public IPresentationModelElement getManifest()
            throws PresentationException, IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException, IllegalPathSyntaxException, ContentLibException {
        try {
            Optional<URI> forwardURI = new ManifestBuilder(urls, servletRequest).getExternalManifestURI(pi);
            if (forwardURI.isPresent()) {
                servletResponse.sendRedirect(forwardURI.get().toString());
                return null;
            }
        } catch (IOException e) {
            logger.error("Error forwarding manifest url", e);
        }
        return new ManifestBuilder(urls, servletRequest).build(pi);
    }

    /**
     * Returns all W3C Web Annotations recorded for the record as a single annotation collection.
     *
     * <p>The collection contains every annotation for the record on one page (its item count equals the total number
     * of annotations).
     *
     * @return the record's annotation collection
     * @throws DAOException if the annotations cannot be read from the database
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_ANNOTATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a record as annotation collection",
            description = "All annotations recorded for the record are returned as a single collection page, whose item count equals the"
                    + " total annotation count.")
    @ApiResponse(responseCode = "200", description = "Annotation collection of the record",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AnnotationCollection.class)))
    public IAnnotationCollection getAnnotationsForRecord() throws DAOException, IllegalRequestException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_ANNOTATIONS).params(pi);
        URI uri = URI.create(apiPath.build());
        //        return new WebAnnotationBuilder(urls).getCrowdsourcingAnnotationCollection(uri, pi, false);
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForRecord(pi, uri);
    }

    /**
     * Returns all user comments recorded for the record as a single annotation collection.
     *
     * <p>The collection contains every comment for the record on one page (its item count equals the total number of
     * comments).
     *
     * @return the record's comment collection
     * @throws DAOException if the comments cannot be read from the database
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_COMMENTS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a record as an annotation collection",
            description = "All comments recorded for the record are returned as a single collection page, whose item count equals the"
                    + " total comment count.")
    @ApiResponse(responseCode = "200", description = "Comment collection of the record",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AnnotationCollection.class)))
    public IAnnotationCollection getCommentsForRecord() throws DAOException {

        ApiPath apiPath = urls.path(RECORDS_RECORD, RECORDS_COMMENTS).params(pi);
        URI uri = URI.create(apiPath.build());
        return new AnnotationsResourceBuilder(urls, servletRequest).getWebAnnotationCollectionForRecordComments(pi, uri);
    }

    /**
     * Returns the single page of the record's annotation collection, containing all of its annotations.
     *
     * <p>Because the collection has exactly one page, this endpoint always returns page 1; there is no parameter for
     * requesting further pages. A record without annotations yields an empty page instead of an error.
     *
     * @return the record's annotation page
     * @throws DAOException if the annotations cannot be read from the database
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_ANNOTATIONS_PAGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List annotations for a record as an annotation collection page",
            description = "Since the record's annotation collection contains all of its annotations on a single page, this endpoint"
                    + " always returns that first page; there is no parameter for requesting further pages. A record without annotations"
                    + " yields an empty page rather than an error.")
    @ApiResponse(responseCode = "200", description = "First page of the record's annotation collection", useReturnTypeSchema = true)
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

    /**
     * Returns the single page of the record's comment collection, containing all of its comments.
     *
     * <p>Because the collection has exactly one page, this endpoint always returns page 1; there is no parameter for
     * requesting further pages. A record without comments yields an empty page instead of an error.
     *
     * @return the record's comment page
     * @throws DAOException if the comments cannot be read from the database
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_COMMENTS_PAGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "annotations" }, summary = "List comments for a record as an annotation collection page",
            description = "Since the record's comment collection contains all of its comments on a single page, this endpoint always"
                    + " returns that first page; there is no parameter for requesting further pages. A record without comments yields an"
                    + " empty page rather than an error.")
    @ApiResponse(responseCode = "200", description = "First page of the record's comment collection",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AnnotationPage.class)))
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
     * Serves the IIIF Search API of the record's manifest.
     *
     * <p>Depending on the given motivation parameter, fulltext (motivation=painting) and/or crowdsourcing annotations, indexed metadata and
     * user comments together (motivation=non-painting or motivation=describing) may be searched; omitting the parameter searches everything.
     * Fulltext is only searched if the fulltext view permission is granted for the record.
     *
     * @param query The search query; a list of space separated terms. The search is for all complete words which match any of the query terms. Terms
     *            may contain the wildcard character '*' to represent an arbitrary number of characters within the word
     * @param motivation a space separated list of motivations of annotations to search for. Search for the following motivations is implemented:
     *            <ul>
     *            <li>painting: fulltext resources, searched only if the fulltext view permission is granted</li>
     *            <li>non-painting or describing: crowdsourcing annotations, indexed metadata and user comments</li>
     *            </ul>
     * @param date not supported. If this parameter is given, its name is listed in the 'ignored' property of the 'within' property of the answer
     * @param user not supported. If this parameter is given, its name is listed in the 'ignored' property of the 'within' property of the answer
     * @param page the page number for paged result sets. if this is empty, page=1 is assumed
     * @return a {@link de.intranda.api.iiif.search.SearchResult} containing all annotations matching the query in the 'resources' property
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_MANIFEST_SEARCH)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "IIIF Search API: search within the manifest of the given record",
            description = "The motivation parameter selects which resource types are searched: 'painting' searches fulltext, but only if"
                    + " the fulltext view permission is granted for the record, while 'non-painting' or 'describing' together search"
                    + " crowdsourcing annotations, indexed metadata and user comments; omitting the parameter searches everything. The date"
                    + " and user parameters are accepted for IIIF Search API compatibility but are not evaluated; only their names are listed"
                    + " in the response's 'within' object under its 'ignored' property.")
    @ApiResponse(responseCode = "200", description = "IIIF Search result containing matching annotations", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "Record not found")
    public SearchResult searchInManifest(
            @Parameter(description = "Search query string") @QueryParam("q") String query,
            @Parameter(description = "Space- or plus-separated list of annotation motivations to search")
            @QueryParam("motivation") String motivation,
            @Parameter(description = "Date filter (not supported; included in 'ignored' property if given)") @QueryParam("date") String date,
            @Parameter(description = "User filter (not supported; included in 'ignored' property if given)") @QueryParam("user") String user,
            @Parameter(description = "Page number for paged result sets (default: 1)") @QueryParam("page") Integer page)
            throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(urls, query, pi, servletRequest).setMotivation(motivation).setDate(date).setUser(user).setPage(page).build();
    }

    /**
     * Returns auto-complete suggestions for search terms within a IIIF manifest.
     *
     * @param query partial query string for auto-completion
     * @param motivation space-separated list of annotation motivations to filter
     * @param date date filter (not supported; passed to 'ignored' property)
     * @param user user filter (not supported; passed to 'ignored' property)
     * @param page page number; not supported for auto-completion, the full term list is always returned
     * @return the IIIF AutoSuggest result containing auto-completion candidates for the given query
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_MANIFEST_AUTOCOMPLETE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "IIIF Search API: autocomplete search within the manifest of the given record",
            description = "Unlike the manifest search endpoint, this operation ignores the page parameter and always returns the complete"
                    + " list of matching terms in one response. The date and user parameters are accepted for compatibility but are not"
                    + " evaluated; only their names are listed in the 'ignored' property instead.")
    @ApiResponse(responseCode = "200", description = "IIIF AutoSuggest result containing matching terms", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "Record not found")
    public AutoSuggestResult autoCompleteInManifest(
            @Parameter(description = "Partial search query string for auto-completion") @QueryParam("q") String query,
            @Parameter(description = "Space- or plus-separated list of annotation motivations to search")
            @QueryParam("motivation") String motivation,
            @Parameter(description = "Date filter (not supported; included in 'ignored' property if given)") @QueryParam("date") String date,
            @Parameter(description = "User filter (not supported; included in 'ignored' property if given)") @QueryParam("user") String user,
            @Parameter(description = "Page number; not supported for auto-completion, the full term list is always returned")
            @QueryParam("page") Integer page) throws IndexUnreachableException, PresentationException {
        return new IIIFSearchBuilder(urls, query, pi, servletRequest).setMotivation(motivation)
                .setDate(date)
                .setUser(user)
                .setPage(page)
                .buildAutoSuggest();
    }

}
