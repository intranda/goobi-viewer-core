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
package io.goobi.viewer.api.rest.v1.bookmarks;

import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_ITEM;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_IIIF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_MIRADOR;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_RSS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_RSS_JSON;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_SHARED;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_SHARED_IIIF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_SHARED_MIRADOR;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_SHARED_RSS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_LIST_SHARED_RSS_JSON;
import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_BOOKMARKS_PUBLIC;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.presentation.v2.Collection2;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.UserLoggedInFilter;
import io.goobi.viewer.api.rest.model.SuccessMessage;
import io.goobi.viewer.api.rest.resourcebuilders.AbstractBookmarkResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.SessionBookmarkResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.UserBookmarkResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.rss.Channel;
import io.goobi.viewer.model.rss.RSSFeed;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for managing user bookmark lists including creation, sharing, and export in multiple formats.
 *
 * @author Florian Alpers
 */
@Path(USERS_BOOKMARKS)
@ViewerRestServiceBinding
public class BookmarkResource {

    private static final Logger logger = LogManager.getLogger(BookmarkResource.class);

    private AbstractBookmarkResourceBuilder builder;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    @Inject
    private ApiUrls urls;

    public BookmarkResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;

        User currentUser = getUser(servletRequest);
        if (currentUser != null) {
            builder = new UserBookmarkResourceBuilder(currentUser);
        } else {
            HttpSession session = servletRequest.getSession();
            builder = new SessionBookmarkResourceBuilder(session);
        }
    }

    private static User getUser(HttpServletRequest request) {
        User user = null;
        try {
            user = UserLoggedInFilter.getValidUserToken(request)
                    .map(UserToken::getUser)
                    .orElse(null);
        } catch (DAOException e) {
            logger.warn("Error getting user from authorization token", e);
        }
        if (user == null) {
            UserBean bean = BeanUtils.getUserBeanFromSession(request.getSession());
            if (bean != null) {
                user = bean.getUser();
            }
        }
        return user;
    }

    /**
     * Returns the bookmark lists of the current user, or the single session list of an anonymous request.
     *
     * <p>Which lists exist depends on the request: for a logged-in user all persisted lists are returned, for an
     * anonymous request a single list is created in the HTTP session on first access. That session list has neither an
     * id nor a share key, so it cannot be addressed or shared like a stored list; if it is not empty when the same
     * session logs in, it is taken over into that account as a regular bookmark list.
     *
     * @return the bookmark lists visible to the current request
     * @throws DAOException if the lists of a logged-in user cannot be read from the database
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get all bookmark lists owned by the current user. If not logged in, a single temporary bookmark list is stored"
                    + " in the http session which is returned",
            description = "Anonymous requests get a session-scoped list that is created on first access. It has neither an id nor a share"
                    + " key, so it cannot be addressed or shared like a stored list; if it is not empty when the same session logs in, it"
                    + " is taken over into that account. Logged-in users get all lists stored for their account.")
    @ApiResponse(responseCode = "200", description = "List of bookmark lists owned by the current user", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public List<BookmarkList> getOwnedBookmarkLists() throws DAOException, IOException, RestApiException {
        return builder.getAllBookmarkLists();
    }

    /**
     * Creates a new bookmark list for the current user.
     *
     * <p>Anonymous session requests always fail with 409, since a session can hold only a single bookmark list.
     * Logged-in users get a new persisted list; if no name is given in the request body, one is generated
     * automatically, and a name already used by one of the user's lists is rejected with 400.
     *
     * @param dto the bookmark list to create; only the optional {@code name} field is used
     * @return the created bookmark list, with a {@code Location} header pointing to it if it has an id
     * @throws BadRequestException if the request body is null
     * @throws RestApiException if the requested name is already used by one of the user's bookmark lists
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Add a new bookmark list for the current user.",
            description = "Anonymous session requests always fail with 409, since a session can hold only a single bookmark list."
                    + " Logged-in users get a new persisted list; if no name is given, one is generated automatically, and a name"
                    + " already used by one of the user's lists is rejected with 400.")
    @ApiResponse(responseCode = "201", description = "Bookmark list created successfully, returns the new list",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BookmarkList.class)))
    @ApiResponse(responseCode = "400", description = "Missing or invalid request body")
    @ApiResponse(responseCode = "409", description = "Session users may only have one bookmark list")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    // type = "object" prevents schemathesis from sending primitives (integer/string) as the request body —
    // mirrors the PATCH endpoint's annotation.
    @RequestBody(required = true,
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = BookmarkListCreateDto.class, type = "object")))
    public Response addBookmarkList(BookmarkListCreateDto dto) throws DAOException, IOException, RestApiException, IllegalRequestException {
        if (dto == null) {
            throw new BadRequestException("Request body must not be null");
        }
        BookmarkList created;
        if (StringUtils.isNotBlank(dto.getName())) {
            created = builder.addBookmarkList(dto.getName());
        } else {
            created = builder.addBookmarkList();
        }
        Response.ResponseBuilder response = Response.status(Response.Status.CREATED).entity(created);
        if (created.getId() != null) {
            URI location = URI.create(urls.path(ApiUrls.USERS_BOOKMARKS, ApiUrls.USERS_BOOKMARKS_LIST).params(created.getId()).build());
            response = response.location(location);
        }
        return response.build();
    }

    /**
     * Returns the bookmark list with the given id.
     *
     * <p>For a logged-in user, the list is returned if it is public or owned by the requesting user; a private
     * list owned by someone else results in 403, a non-existent id in 404. For an anonymous request the id is
     * ignored and the session's single bookmark list is returned, creating it if needed.
     *
     * @param id the id of the bookmark list
     * @return the bookmark list with the given id
     * @throws RestApiException if the bookmark list does not exist, or exists but is neither public nor owned by
     *     the current user
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmarklist owned by the current user by its id. If not logged in, the single bookmark list stored"
                    + " in the session is always returned",
            description = "For a logged-in user, the list is returned if it is public or owned by the requesting user; a private"
                    + " list owned by someone else results in 403, a non-existent id in 404. For an anonymous request the id is"
                    + " ignored and the session's single bookmark list is always returned.")
    @ApiResponse(responseCode = "200", description = "Bookmark list", useReturnTypeSchema = true)
    // 400 is returned when the path parameter {listId} cannot be parsed as a valid integer
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public BookmarkList getBookmarkList(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id)
            throws DAOException, IOException, RestApiException {
        requireValidListId(id);
        return builder.getBookmarkListById(id);
    }

    /**
     * Applies a partial update to the bookmark list with the given id.
     *
     * <p>Only a non-blank {@code name} or {@code description} and a present {@code isPublic} are applied; the share
     * key and ownership cannot be patched here. A blank {@code name} or {@code description} in the request body is
     * silently ignored rather than clearing the field. Anonymous session requests always fail with 409, since the
     * session bookmark list cannot be updated.
     *
     * @param id the id of the bookmark list
     * @param patch the attributes to change; a blank {@code name}/{@code description} or an absent {@code isPublic}
     *     is left unchanged
     * @return the updated bookmark list
     * @throws IllegalRequestException if the request body is missing, or a logged-in user does not own the list
     */
    @PATCH
    @Path(USERS_BOOKMARKS_LIST)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Set passed attributes to the bookmarkList",
            description = "Only a non-blank name or description and a present isPublic are applied; the share key and ownership"
                    + " cannot be patched here. A blank name or description in the request body is silently ignored rather than"
                    + " clearing the field. Anonymous session requests always fail with 409, since the session bookmark list"
                    + " cannot be updated.")
    @ApiResponse(responseCode = "200", description = "Updated bookmark list", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Missing or invalid request body")
    @ApiResponse(responseCode = "404", description = "No bookmark list found for the given id")
    @ApiResponse(responseCode = "409", description = "Session bookmark lists cannot be updated")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    // Use BookmarkListPatchDto to express the partial-update contract precisely: each field is optional
    // (boxed Boolean for isPublic distinguishes "field absent" from "set to false"), and shareKey /
    // ownership fields are intentionally not patchable.
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = BookmarkListPatchDto.class, type = "object")))
    public BookmarkList patchBookmarkList(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id,
            BookmarkListPatchDto patch) throws DAOException, IOException, RestApiException, IllegalRequestException {
        requireValidListId(id);
        if (patch == null) {
            throw new IllegalRequestException("Request body required");
        }
        BookmarkList orig = getBookmarkList(id);
        // Partial update: only fields explicitly present in the patch DTO are applied.
        // shareKey is intentionally not patchable here — server-side generators (BookmarkBean,
        // BookmarkList.generateShareKey) are the only legitimate path.
        if (StringUtils.isNotBlank(patch.getName())) {
            orig.setName(patch.getName());
        }
        if (StringUtils.isNotBlank(patch.getDescription())) {
            orig.setDescription(patch.getDescription());
        }
        if (patch.getIsPublic() != null) {
            orig.setIsPublic(patch.getIsPublic());
        }
        builder.updateBookmarkList(orig);
        return orig;
    }

    /**
     * Deletes the bookmark list with the given id.
     *
     * <p>For a logged-in user this removes their persisted list with the given id, or fails with 404 if no such
     * list is owned by the user. For an anonymous request the id is ignored and the session's bookmark list is
     * always cleared and immediately replaced with a new empty one.
     *
     * @param id the id of the bookmark list
     * @return a success message confirming the deletion
     * @throws RestApiException if a logged-in user owns no bookmark list with the given id
     */
    @DELETE
    @Path(USERS_BOOKMARKS_LIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Delete a bookmark list",
            description = "For a logged-in user this removes their persisted list with the given id, or fails with 404 if no such"
                    + " list is owned by the user. For an anonymous request the id is ignored and the session's bookmark list is"
                    + " always cleared and immediately replaced with a new empty one.")
    @ApiResponse(responseCode = "200", description = "Bookmark list deleted successfully", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Not logged in, session bookmark list may not be deleted")
    // 404 is returned when JAX-RS cannot parse {listId} as a valid Long (non-integer path parameter value)
    @ApiResponse(responseCode = "404", description = "Bookmark list not found or list ID could not be parsed")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public SuccessMessage deleteBookmarkList(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id)
            throws DAOException, IOException, RestApiException, IllegalRequestException {
        requireValidListId(id);
        return builder.deleteBookmarkList(id);
    }

    /**
     * Adds a bookmark for the given record to the bookmark list with the given id.
     *
     * <p>A {@code logId} of {@code "-"} is treated as "no structural element". Adding an item already present in
     * the list fails with 409. Whether an unresolvable {@code pi} results in 404 or 400 depends on the builder: a
     * logged-in user's list reports 404, the anonymous session list reports 400.
     *
     * @param id the id of the bookmark list
     * @param item the bookmark to add; only {@code pi}, {@code logId} and {@code order} are used
     * @return the created bookmark, with a {@code Location} header pointing to it if it has an id
     * @throws BadRequestException if the request body is null
     * @throws RestApiException if the item already exists in the list, or the record cannot be resolved
     */
    @POST
    @Path(USERS_BOOKMARKS_LIST)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Add bookmark to list. Only pi, LogId and order are used",
            description = "A logId of \"-\" is treated as \"no structural element\". Adding an item already present in the list"
                    + " fails with 409. Whether an unresolvable pi results in 404 or 400 depends on the builder: a logged-in user's"
                    + " list reports 404, the anonymous session list reports 400.")
    @ApiResponse(responseCode = "201", description = "Bookmark added; returns the created bookmark",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Bookmark.class)))
    // 400 is returned when the path parameter {listId} cannot be parsed as a valid integer
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID or bookmark data")
    @ApiResponse(responseCode = "404", description = "Bookmark list or record not found")
    @ApiResponse(responseCode = "409", description = "Bookmark already exists in list")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    // Provide explicit content spec to avoid OpenAPI schema validation error ("Invalid requestBody definition").
    // type = "object" prevents schemathesis from sending primitives (e.g. the integer 0) as the request body.
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = Bookmark.class, type = "object")))
    public Response addItemToBookmarkList(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id,
            Bookmark item) throws DAOException, IOException, RestApiException, IllegalRequestException {
        requireValidListId(id);
        // Reject null body (e.g. JSON literal "null") with 400 instead of NPE → 500
        if (item == null) {
            throw new BadRequestException("Request body must not be null");
        }
        Bookmark created = builder.addBookmarkToBookmarkList(id, item.getPi(), item.getLogId(),
                Optional.ofNullable(item.getOrder()).map(Object::toString).orElse(null));
        Response.ResponseBuilder response = Response.status(Response.Status.CREATED).entity(created);
        if (created.getId() != null) {
            URI location = URI.create(urls.path(ApiUrls.USERS_BOOKMARKS, ApiUrls.USERS_BOOKMARKS_ITEM).params(id, created.getId()).build());
            response = response.location(location);
        }
        return response.build();
    }

    /**
     * Returns the bookmark with the given id from the bookmark list with the given id.
     *
     * <p>Access to the containing list follows the same rules as the plain bookmark-list lookup. The bookmark is
     * then looked up among the list's items by id; if none matches, the request fails with 404.
     *
     * @param listId the id of the bookmark list
     * @param bookmarkId the id of the bookmark
     * @return the bookmark with the given id
     * @throws RestApiException if no bookmark with the given id exists in the list
     */
    @GET
    @Path(USERS_BOOKMARKS_ITEM)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmark by its id and the id of the containing list",
            description = "Access to the containing list follows the same rules as the plain bookmark-list lookup. The bookmark"
                    + " is then looked up among the list's items by id; if none matches, the request fails with 404.")
    @ApiResponse(responseCode = "200", description = "The bookmark", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID or bookmark ID")
    @ApiResponse(responseCode = "404", description = "Bookmark not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public Bookmark getBookmarkItem(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long listId,
            @Parameter(description = "The id of the bookmark",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("bookmarkId") Long bookmarkId)
            throws RestApiException, DAOException, IOException {
        requireValidListId(listId);
        BookmarkList list = getBookmarkList(listId);
        Bookmark item = list.getItems().stream().filter(i -> i.getId().equals(bookmarkId)).findAny().orElse(null);
        if (item != null) {
            return item;
        }
        throw new RestApiException("No item found in list " + listId + "with id" + bookmarkId, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Deletes the bookmark with the given id from the bookmark list with the given id.
     *
     * <p>Access to the containing list follows the same rules as the plain bookmark-list lookup. The bookmark is
     * first located within the list by id (404 if absent), then removed by matching its record, structural
     * element and page rather than by its own id.
     *
     * @param listId the id of the bookmark list
     * @param bookmarkId the id of the bookmark
     * @return a success message confirming the removal
     * @throws RestApiException if no bookmark with the given id exists in the list
     */
    @DELETE
    @Path(USERS_BOOKMARKS_ITEM)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Delete a bookmark from a list",
            description = "Access to the containing list follows the same rules as the plain bookmark-list lookup. The bookmark is"
                    + " first located within the list by id (404 if absent), then removed by matching its record, structural"
                    + " element and page rather than by its own id.")
    @ApiResponse(responseCode = "200", description = "Bookmark deleted", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID or bookmark ID")
    @ApiResponse(responseCode = "404", description = "Bookmark not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public SuccessMessage deleteBookmarkItem(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long listId,
            @Parameter(description = "The id of the bookmark",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("bookmarkId") Long bookmarkId)
            throws RestApiException, DAOException, IOException {
        requireValidListId(listId);
        BookmarkList list = getBookmarkList(listId);
        Bookmark item = list.getItems().stream().filter(i -> i.getId().equals(bookmarkId)).findAny().orElse(null);
        if (item != null) {
            return builder.deleteBookmarkFromBookmarkList(list.getId(), item.getPi(), item.getLogId(),
                    Optional.ofNullable(item.getOrder()).map(Object::toString).orElse(null));
        }
        throw new RestApiException("No item found in list " + listId + "with id" + bookmarkId, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Returns the bookmark list with the given id as a IIIF Presentation 2.1.1 collection.
     *
     * <p>For a logged-in user, only lists owned by that user are considered here; unlike the plain bookmark-list
     * endpoint, a public list owned by someone else is not accessible through this operation and results in 404.
     * For an anonymous request the id is ignored and the session's bookmark list is returned as a collection.
     *
     * @param id the id of the bookmark list
     * @return the bookmark list as a IIIF Presentation 2.1.1 collection
     * @throws RestApiException if no bookmark list with the given id is owned by the current user
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_IIIF)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks", "iiif" },
            summary = "Get a bookmarklist owned by the current user by its id and return it as a IIIF Presentation 2.1.1 collection resource."
                    + " If not logged in, the single bookmark list stored in the session is always returned",
            description = "For a logged-in user, only lists owned by that user are considered; unlike the plain bookmark-list"
                    + " endpoint, a public list owned by someone else is not accessible here and results in 404. For an anonymous"
                    + " request the id is ignored and the session's single list is returned.")
    @ApiResponse(responseCode = "200", description = "Bookmark list as IIIF collection", useReturnTypeSchema = true)
    // 400 is returned when the path parameter {listId} cannot be parsed as a valid integer
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public Collection2 getBookmarkListAsIIIFCollection(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id)
            throws DAOException, IOException, RestApiException {
        requireValidListId(id);
        return builder.getAsCollection(id, urls);
    }

    /**
     * Returns the bookmark list with the given id as a Mirador viewer configuration.
     *
     * <p>For a logged-in user, only lists owned by that user are considered here; unlike the plain bookmark-list
     * endpoint, a public list owned by someone else is not accessible through this operation and results in 404.
     * For an anonymous request the id is ignored and the session's bookmark list is returned.
     *
     * @param id the id of the bookmark list
     * @return the Mirador-compatible JSON configuration for the bookmark list
     * @throws RestApiException if no bookmark list with the given id is owned by the current user
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_MIRADOR)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmarklist owned by the current user by its id and return it as a Mirador viewe config object. If not logged in,"
                    + " the single bookmark list stored in the session is always returned",
            description = "For a logged-in user, only lists owned by that user are considered; unlike the plain bookmark-list"
                    + " endpoint, a public list owned by someone else is not accessible here and results in 404. For an anonymous"
                    + " request the id is ignored and the session's single list is returned.")
    @ApiResponse(responseCode = "200", description = "Bookmark list as Mirador viewer config",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object")))
    // 400 is returned when the path parameter {listId} cannot be parsed as a valid integer
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public String getBookmarkListForMirador(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException,
            PresentationException {
        requireValidListId(id);
        return builder.getBookmarkListForMirador(id, urls);
    }

    /**
     * Returns the bookmark list with the given id as an RSS feed in XML format.
     *
     * <p>Access to the list follows the same rules as the plain bookmark-list lookup, so a public list owned by
     * another user is accessible here. The feed is generated from a Solr query matching the list's items, so
     * entries reflect the current index state of those records rather than a snapshot of the list.
     *
     * @param id the id of the bookmark list
     * @param language language for RSS metadata
     * @param maxStr limit for results to return
     * @return the RSS feed for the bookmark list as XML
     * @throws RestApiException if the bookmark list does not exist, or exists but is neither public nor owned by
     *     the current user
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_RSS)
    @Produces({ MediaType.TEXT_XML })
    @Operation(
            tags = { "bookmarks", "rss" },
            summary = "Get a bookmarklist owned by the current user by its id and return it as an RSS feed. If not logged in,"
                    + " the single bookmark list stored in the session is always returned",
            description = "Access to the list follows the same rules as the plain bookmark-list lookup, so a public list owned"
                    + " by another user is accessible here. The feed is generated from a Solr query matching the list's items, so"
                    + " entries reflect the current index state of those records rather than a snapshot of the list.")
    @ApiResponse(responseCode = "200", description = "RSS feed for the bookmark list", useReturnTypeSchema = true)
    // 400 is returned when the path parameter {listId} cannot be parsed as a valid integer
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public String getBookmarkListAsRSS(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id,
            @Parameter(description = "Language for RSS metadata") @QueryParam("lang") String language,
            // Accept max as String to gracefully handle the literal string "null" sent by some clients,
            // which cannot be parsed directly into Integer by JAX-RS and would cause a 500 error.
            @Parameter(description = "Limit for results to return") @QueryParam("max") String maxStr)
            throws DAOException, IOException, RestApiException, ContentLibException {
        requireValidListId(id);
        BookmarkList list = getBookmarkList(id);
        String query = list.generateSolrQueryForItems();
        return RSSFeed.createRssFeedString(language, parseMaxHits(maxStr), null, query, null, servletRequest, null, true);
    }

    /**
     * Returns the bookmark list with the given id as an RSS feed in JSON format.
     *
     * <p>Access to the list follows the same rules as the plain bookmark-list lookup, so a public list owned by
     * another user is accessible here. The feed is generated from a Solr query matching the list's items, so
     * entries reflect the current index state of those records rather than a snapshot of the list.
     *
     * @param id the id of the bookmark list
     * @param language language for RSS metadata
     * @param maxStr limit for results to return
     * @return the RSS channel for the bookmark list
     * @throws RestApiException if the bookmark list does not exist, or exists but is neither public nor owned by
     *     the current user
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_RSS_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks", "rss" },
            summary = "Get a bookmarklist owned by the current user by its id and return it as an RSS feed in json format. If not logged in,"
                    + " the single bookmark list stored in the session is always returned",
            description = "Access to the list follows the same rules as the plain bookmark-list lookup, so a public list owned"
                    + " by another user is accessible here. The feed is generated from a Solr query matching the list's items, so"
                    + " entries reflect the current index state of those records rather than a snapshot of the list.")
    @ApiResponse(responseCode = "200", description = "RSS feed for the bookmark list as JSON", useReturnTypeSchema = true)
    // 400 is returned when the path parameter {listId} cannot be parsed as a valid integer
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public Channel getBookmarkListAsRSSJson(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id,
            @Parameter(description = "Language for RSS metadata") @QueryParam("lang") String language,
            // Accept max as String to gracefully handle the literal string "null" sent by some clients,
            // which cannot be parsed directly into Integer by JAX-RS and would cause a 500 error.
            @Parameter(description = "Limit for results to return") @QueryParam("max") String maxStr)
            throws DAOException, IOException, RestApiException, ContentLibException {
        requireValidListId(id);
        BookmarkList list = getBookmarkList(id);
        String query = list.generateSolrQueryForItems();
        return RSSFeed.createRssResponse(language, parseMaxHits(maxStr), null, query, null, servletRequest, null, true);
    }

    /**
     * Returns all bookmark lists marked as public, regardless of the requesting user.
     *
     * <p>The result is returned in the order the database provides it in and is not sorted by update date, unlike
     * {@link #getOwnedBookmarkLists()} which sorts the current user's lists by most recent update descending.
     *
     * @return all publicly visible bookmark lists stored in the database
     * @throws DAOException if the lists cannot be read from the database
     */
    @GET
    @Path(USERS_BOOKMARKS_PUBLIC)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get all public bookmark lists",
            description = "Public lists are returned regardless of whether a user is logged in or a session list exists; this"
                    + " operation reads directly from the database and ignores the current user or session. Unlike the owned"
                    + " bookmark lists endpoint, the result is not sorted by update date.")
    @ApiResponse(responseCode = "200", description = "List of all public bookmark lists", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public List<BookmarkList> getPublicBookmarkLists()
            throws DAOException, IOException, RestApiException {
        return builder.getAllPublicBookmarkLists();
    }

    /**
     * Returns the bookmark list identified by the given share key, independent of the current user or session.
     *
     * <p>The list is looked up solely by its share key, independent of the current user or session; a key matching no
     * list at all results in 404.
     *
     * @param key the share key assigned to the bookmark list
     * @return the bookmark list matching the share key
     * @throws ContentLibException if no bookmark list with the given key exists
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a public or shared bookmark list by its share key",
            description = "The list is looked up solely by its share key, independent of the current user or session; a key matching no"
                    + " list at all results in 404.")
    @ApiResponse(responseCode = "200", description = "The shared bookmark list", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid share key format")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public BookmarkList getSharedBookmarkListByKey(
            @Parameter(description = "The share key assigned to the bookmark list",
                    schema = @Schema(pattern = "^[A-Za-z0-9_-]+$")) @PathParam("key") String key)
            throws DAOException, RestApiException, ContentLibException {
        return builder.getSharedBookmarkList(key);
    }

    /**
     * Returns the bookmark list identified by the given share key as a Mirador viewer configuration.
     *
     * <p>The list is looked up solely by its share key, independent of the current user or session; a key matching no
     * list at all results in 404.
     *
     * @param key the share key assigned to the bookmark list
     * @return the Mirador-compatible JSON configuration for the shared bookmark list
     * @throws ContentLibException if no accessible bookmark list matches the given key
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_MIRADOR)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a public or shared bookmark list by its share key as a Mirador viewer config",
            description = "The list is looked up solely by its share key, independent of the current user or session; a key matching no"
                    + " list at all results in 404.")
    @ApiResponse(responseCode = "200", description = "Mirador configuration for the shared bookmark list",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = "object")))
    @ApiResponse(responseCode = "400", description = "Invalid share key format")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public String getSharedBookmarkListForMirador(
            @Parameter(description = "The share key assigned to the bookmark list",
                    schema = @Schema(pattern = "^[A-Za-z0-9_-]+$")) @PathParam("key") String key)
            throws DAOException, ViewerConfigurationException, IndexUnreachableException, PresentationException, ContentLibException {
        return builder.getSharedBookmarkListForMirador(key, urls);
    }

    /**
     * Returns the bookmark list identified by the given share key as a IIIF Presentation 2.1.1 collection.
     *
     * <p>The list is looked up solely by its share key, independent of the current user or session; a key matching no
     * list at all results in 404.
     *
     * @param key the share key assigned to the bookmark list
     * @return the bookmark list as a IIIF Presentation 2.1.1 collection
     * @throws ContentLibException if no accessible bookmark list matches the given key
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_IIIF)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks", "iiif" },
            summary = "Get a public or shared bookmark list by its share key as a IIIF Presentation 2.1.1 collection",
            description = "The list is looked up solely by its share key, independent of the current user or session; a key matching no"
                    + " list at all results in 404.")
    @ApiResponse(responseCode = "200", description = "The shared bookmark list as IIIF Presentation 2.1.1 collection", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid share key format")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    @IIIFPresentationBinding
    public Collection2 getSharedBookmarkListAsCollection(
            @Parameter(description = "The share key assigned to the bookmark list",
                    schema = @Schema(pattern = "^[A-Za-z0-9_-]+$")) @PathParam("key") String key)
            throws DAOException, ContentLibException {
        return builder.getAsCollection(key, urls);
    }

    /**
     * Validates that the given bookmark list ID is at least 1 if a user bookmarklist is requested
     *
     * <p>
     * The schema documents minimum=1, but JAX-RS does not enforce schema constraints server-side. Without this check, listId=0 silently returns the
     * session list instead of a 400.
     *
     * @param id the listId path parameter value
     * @throws BadRequestException if id is null or less than 1 and {@link #builder} is no {@link SessionBookmarkResourceBuilder}
     */
    private void requireValidListId(Long id) {
        if (this.builder instanceof SessionBookmarkResourceBuilder) {
            return;
        } else if (id != null && id < 1) {
            throw new BadRequestException("Bookmark list ID must be at least 1, got: " + id);
        }
    }

    /**
     * Parses the "max" query parameter string to an Integer.
     *
     * <p>
     * Returns null if the string is null, blank, the literal "null", or not a valid integer. This is needed because some clients send ?max=null (the
     * string "null") which JAX-RS cannot auto-convert to Integer and would throw a NumberFormatException (HTTP 500).
     *
     * @param maxStr the raw query parameter value
     * @return parsed Integer, or null if absent or invalid
     */
    static Integer parseMaxHits(String maxStr) {
        if (maxStr == null || maxStr.isBlank() || "null".equalsIgnoreCase(maxStr)) {
            return null;
        }
        try {
            return Integer.parseInt(maxStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the bookmark list identified by the given share key as an RSS feed in JSON format.
     *
     * <p>The list is looked up solely by its share key, independent of the current user or session; a key matching no
     * list at all results in 404. The feed itself is generated from a Solr query matching the list's items, so entries
     * reflect the current index state of those records rather than a snapshot of the list.
     *
     * @param key the share key assigned to the bookmark list
     * @param language language for RSS metadata
     * @param maxStr limit for results to return
     * @return the RSS channel for the shared bookmark list
     * @throws ContentLibException if no bookmark list with the given key exists
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_RSS_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks", "rss" },
            summary = "Get a public or shared bookmark list by its share key as an RSS feed in json format",
            description = "The list is looked up solely by its share key, independent of the current user or session; a key matching no"
                    + " list at all results in 404. The feed is generated from a Solr query matching the list's items, so entries reflect"
                    + " the current index state of those records rather than a snapshot of the list.")
    @ApiResponse(responseCode = "200", description = "The shared bookmark list as RSS feed in JSON", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid share key format")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public Channel getSharedBookmarkListAsRSSJson(
            @Parameter(description = "The share key assigned to the bookmark list") @PathParam("key") String key,
            @Parameter(description = "Language for RSS metadata") @QueryParam("lang") String language,
            // Accept max as String to gracefully handle the literal string "null" sent by some clients,
            // which cannot be parsed directly into Integer by JAX-RS and would cause a 500 error.
            @Parameter(description = "Limit for results to return",
                    schema = @Schema(type = "integer", minimum = "0", maximum = "2147483647")) @QueryParam("max") String maxStr)
            throws DAOException, RestApiException, ContentLibException {
        BookmarkList list = getSharedBookmarkListByKey(key);
        String query = list.generateSolrQueryForItems();
        return RSSFeed.createRssResponse(language, parseMaxHits(maxStr), null, query, null, servletRequest, null, true);
    }

    /**
     * Returns the bookmark list identified by the given share key as an RSS feed in XML format.
     *
     * <p>The list is looked up solely by its share key, independent of the current user or session; a key matching no
     * list at all results in 404. The feed itself is generated from a Solr query matching the list's items, so entries
     * reflect the current index state of those records rather than a snapshot of the list.
     *
     * @param key the share key assigned to the bookmark list
     * @param language language for RSS metadata
     * @param maxStr limit for results to return
     * @return the RSS feed for the shared bookmark list as XML
     * @throws ContentLibException if no bookmark list with the given key exists
     */
    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_RSS)
    @Produces({ MediaType.TEXT_XML })
    @Operation(
            tags = { "bookmarks", "rss" },
            summary = "Get a  bookmark list by its share key as an RSS feed",
            description = "The list is looked up solely by its share key, independent of the current user or session; a key matching no"
                    + " list at all results in 404. The feed is generated from a Solr query matching the list's items, so entries reflect"
                    + " the current index state of those records rather than a snapshot of the list.")
    @ApiResponse(responseCode = "200", description = "The shared bookmark list as RSS feed", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Invalid share key format")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public String getSharedBookmarkListAsRSS(
            @Parameter(description = "The share key assigned to the bookmark list") @PathParam("key") String key,
            @Parameter(description = "Language for RSS metadata") @QueryParam("lang") String language,
            // Accept max as String to gracefully handle the literal string "null" sent by some clients,
            // which cannot be parsed directly into Integer by JAX-RS and would cause a 500 error.
            @Parameter(description = "Limit for results to return",
                    schema = @Schema(type = "integer", minimum = "0", maximum = "2147483647")) @QueryParam("max") String maxStr)
            throws DAOException, RestApiException, ContentLibException {
        BookmarkList list = getSharedBookmarkListByKey(key);
        String query = list.generateSolrQueryForItems();
        return RSSFeed.createRssFeedString(language, parseMaxHits(maxStr), null, query, null, servletRequest, null, true);
    }
}
