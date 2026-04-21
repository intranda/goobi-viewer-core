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
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.iiif.presentation.v2.Collection2;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
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

    private AbstractBookmarkResourceBuilder builder;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;

    @Inject
    private ApiUrls urls;

    public BookmarkResource(@Context HttpServletRequest servletRequest, @Context HttpServletResponse servletResponse) {
        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        UserBean bean = BeanUtils.getUserBeanFromSession(servletRequest.getSession());
        if (bean != null) {
            User currentUser = bean.getUser();
            if (currentUser != null) {
                builder = new UserBookmarkResourceBuilder(currentUser);
            }
        }
        if (builder == null) {
            HttpSession session = servletRequest.getSession();
            builder = new SessionBookmarkResourceBuilder(session);
        }
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get all bookmark lists owned by the current user. If not logged in, a single temporary bookmark list is stored"
                    + " in the http session which is returned")
    @ApiResponse(responseCode = "200", description = "List of bookmark lists owned by the current user")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public List<BookmarkList> getOwnedBookmarkLists() throws DAOException, IOException, RestApiException {
        return builder.getAllBookmarkLists();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Add a new bookmark list for the current user.")
    @ApiResponse(responseCode = "201", description = "Bookmark list created successfully")
    @ApiResponse(responseCode = "400", description = "Missing or invalid request body")
    @ApiResponse(responseCode = "409", description = "Session users may only have one bookmark list")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    // Provide explicit content spec to avoid OpenAPI schema validation error ("Invalid requestBody definition")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BookmarkList.class)))
    public Response addBookmarkList(BookmarkList list) throws DAOException, IOException, RestApiException, IllegalRequestException {
        // Reject null body (e.g. JSON literal "null") with 400 instead of NPE → 500
        if (list == null) {
            throw new BadRequestException("Request body must not be null");
        }
        SuccessMessage result;
        if (StringUtils.isNotBlank(list.getName())) {
            result = builder.addBookmarkList(list.getName());
        } else {
            result = builder.addBookmarkList();
        }
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @GET
    @Path(USERS_BOOKMARKS_LIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmarklist owned by the current user by its id. If not logged in, the single bookmark list stored"
                    + " in the session is always returned")
    @ApiResponse(responseCode = "200", description = "Bookmark list")
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

    @PATCH
    @Path(USERS_BOOKMARKS_LIST)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Set passed attributes to the bookmarkList")
    @ApiResponse(responseCode = "200", description = "Updated bookmark list")
    @ApiResponse(responseCode = "400", description = "Missing or invalid request body")
    @ApiResponse(responseCode = "404", description = "No bookmark list found for the given id")
    @ApiResponse(responseCode = "409", description = "Session bookmark lists cannot be updated")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    // Provide explicit content spec to avoid OpenAPI schema validation error ("Invalid requestBody definition").
    // type = "object" prevents schemathesis from sending primitives (e.g. the integer 0) as the request body.
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = BookmarkList.class, type = "object")))
    public BookmarkList patchBookmarkList(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id,
            BookmarkList list) throws DAOException, IOException, RestApiException, IllegalRequestException {
        // Guard against NPE when the client sends a PATCH request without a JSON body
        if (list == null) {
            throw new IllegalRequestException("Request body required");
        }
        BookmarkList orig = getBookmarkList(id);
        if (StringUtils.isNotBlank(list.getName())) {
            orig.setName(list.getName());
        }
        if (StringUtils.isNotBlank(list.getDescription())) {
            orig.setDescription(list.getDescription());
        }
        orig.setIsPublic(list.isIsPublic());
        if (StringUtils.isNotBlank(list.getShareKey())) {
            orig.setShareKey(list.getShareKey());
        }
        builder.updateBookmarkList(orig);
        return orig;
    }

    @DELETE
    @Path(USERS_BOOKMARKS_LIST)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Delete a bookmark list")
    @ApiResponse(responseCode = "200", description = "Bookmark list deleted successfully")
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

    @POST
    @Path(USERS_BOOKMARKS_LIST)
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Add bookmark to list. Only pi, LogId and order are used")
    @ApiResponse(responseCode = "201", description = "Bookmark added; returns the updated bookmark list")
    // 400 is returned when the path parameter {listId} cannot be parsed as a valid integer
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID or bookmark data")
    @ApiResponse(responseCode = "404", description = "Bookmark list or record not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    // Provide explicit content spec to avoid OpenAPI schema validation error ("Invalid requestBody definition").
    // type = "object" prevents schemathesis from sending primitives (e.g. the integer 0) as the request body.
    @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = Bookmark.class, type = "object")))
    public Response addItemToBookmarkList(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long id,
            Bookmark item) throws DAOException, IOException, RestApiException {
        // Reject null body (e.g. JSON literal "null") with 400 instead of NPE → 500
        if (item == null) {
            throw new BadRequestException("Request body must not be null");
        }
        builder.addBookmarkToBookmarkList(id, item.getPi(), item.getLogId(),
                Optional.ofNullable(item.getOrder()).map(Object::toString).orElse(null));
        BookmarkList updatedList = builder.getBookmarkListById(id);
        return Response.status(Response.Status.CREATED).entity(updatedList).build();
    }

    @GET
    @Path(USERS_BOOKMARKS_ITEM)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmark by its id and the id of the containing list")
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID or bookmark ID")
    @ApiResponse(responseCode = "404", description = "Bookmark not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public Bookmark getBookmarkItem(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long listId,
            @Parameter(description = "The id of the bookmark",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("bookmarkId") Long bookmarkId)
            throws RestApiException, DAOException, IOException {
        BookmarkList list = getBookmarkList(listId);
        Bookmark item = list.getItems().stream().filter(i -> i.getId().equals(bookmarkId)).findAny().orElse(null);
        if (item != null) {
            return item;
        }
        throw new RestApiException("No item found in list " + listId + "with id" + bookmarkId, HttpServletResponse.SC_NOT_FOUND);
    }

    @DELETE
    @Path(USERS_BOOKMARKS_ITEM)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Delete a bookmark from a list")
    @ApiResponse(responseCode = "400", description = "Invalid bookmark list ID or bookmark ID")
    @ApiResponse(responseCode = "404", description = "Bookmark not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public SuccessMessage deleteBookmarkItem(
            @Parameter(description = "The id of the bookmark list",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("listId") Long listId,
            @Parameter(description = "The id of the bookmark",
                    schema = @Schema(minimum = "1", maximum = "9223372036854775807")) @PathParam("bookmarkId") Long bookmarkId)
            throws RestApiException, DAOException, IOException {
        BookmarkList list = getBookmarkList(listId);
        Bookmark item = list.getItems().stream().filter(i -> i.getId().equals(bookmarkId)).findAny().orElse(null);
        if (item != null) {
            return builder.deleteBookmarkFromBookmarkList(list.getId(), item.getPi(), item.getLogId(),
                    Optional.ofNullable(item.getOrder()).map(Object::toString).orElse(null));
        }
        throw new RestApiException("No item found in list " + listId + "with id" + bookmarkId, HttpServletResponse.SC_NOT_FOUND);
    }

    @GET
    @Path(USERS_BOOKMARKS_LIST_IIIF)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks", "iiif" },
            summary = "Get a bookmarklist owned by the current user by its id and return it as a IIIF Presentation 2.1.1 collection resource."
                    + " If not logged in, the single bookmark list stored in the session is always returned")
    @ApiResponse(responseCode = "200", description = "Bookmark list as IIIF collection")
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

    @GET
    @Path(USERS_BOOKMARKS_LIST_MIRADOR)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a bookmarklist owned by the current user by its id and return it as a Mirador viewe config object. If not logged in,"
                    + " the single bookmark list stored in the session is always returned")
    @ApiResponse(responseCode = "200", description = "Bookmark list as Mirador viewer config")
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

    @GET
    @Path(USERS_BOOKMARKS_LIST_RSS)
    @Produces({ MediaType.TEXT_XML })
    @Operation(
            tags = { "bookmarks", "rss" },
            summary = "Get a bookmarklist owned by the current user by its id and return it as an RSS feed. If not logged in,"
                    + " the single bookmark list stored in the session is always returned")
    @ApiResponse(responseCode = "200", description = "RSS feed for the bookmark list")
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
        BookmarkList list = getBookmarkList(id);
        String query = list.generateSolrQueryForItems();
        return RSSFeed.createRssFeedString(language, parseMaxHits(maxStr), null, query, null, servletRequest, null, true);
    }

    @GET
    @Path(USERS_BOOKMARKS_LIST_RSS_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks", "rss" },
            summary = "Get a bookmarklist owned by the current user by its id and return it as an RSS feed in json format. If not logged in,"
                    + " the single bookmark list stored in the session is always returned")
    @ApiResponse(responseCode = "200", description = "RSS feed for the bookmark list as JSON")
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
        BookmarkList list = getBookmarkList(id);
        String query = list.generateSolrQueryForItems();
        return RSSFeed.createRssResponse(language, parseMaxHits(maxStr), null, query, null, servletRequest, null, true);
    }

    @GET
    @Path(USERS_BOOKMARKS_PUBLIC)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get all public bookmark lists")
    @ApiResponse(responseCode = "200", description = "List of all public bookmark lists")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public List<BookmarkList> getPublicBookmarkLists()
            throws DAOException, IOException, RestApiException {
        return builder.getAllPublicBookmarkLists();
    }

    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a public or shared bookmark list by its share key")
    @ApiResponse(responseCode = "400", description = "Invalid share key format")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public BookmarkList getSharedBookmarkListByKey(
            @Parameter(description = "The share key assigned to the bookmark list",
                    schema = @Schema(pattern = "^[A-Za-z0-9_-]+$")) @PathParam("key") String key)
            throws DAOException, RestApiException, ContentLibException {
        return builder.getSharedBookmarkList(key);
    }

    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_MIRADOR)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks" },
            summary = "Get a public or shared bookmark list by its share key as a Mirador viewer config")
    @ApiResponse(responseCode = "400", description = "Invalid share key format")
    @ApiResponse(responseCode = "404", description = "Bookmark list not found")
    @ApiResponse(responseCode = "500", description = "Error querying database")
    public String getSharedBookmarkListForMirador(
            @Parameter(description = "The share key assigned to the bookmark list",
                    schema = @Schema(pattern = "^[A-Za-z0-9_-]+$")) @PathParam("key") String key)
            throws DAOException, ViewerConfigurationException, IndexUnreachableException, PresentationException, ContentLibException {
        return builder.getSharedBookmarkListForMirador(key, urls);
    }

    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_IIIF)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks", "iiif" },
            summary = "Get a public or shared bookmark list by its share key as a IIIF Presentation 2.1.1 collection")
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

    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_RSS_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "bookmarks", "rss" },
            summary = "Get a public or shared bookmark list by its share key as an RSS feed in json format")
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

    @GET
    @Path(USERS_BOOKMARKS_LIST_SHARED_RSS)
    @Produces({ MediaType.TEXT_XML })
    @Operation(
            tags = { "bookmarks", "rss" },
            summary = "Get a  bookmark list by its share key as an RSS feed")
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
