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
package io.goobi.viewer.servlets.rest.bookmarks;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.presentation.Collection;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.SuccessMessage;
import io.goobi.viewer.api.rest.v1.ApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.servlets.rest.iiif.presentation.IIIFPresentationBinding;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * <p>
 * BookmarkResource class.
 * </p>
 *
 * @author Florian Alpers
 */
@Path("/bookmarks")
@ViewerRestServiceBinding
public class BookmarkResource {

    private static final Logger logger = LoggerFactory.getLogger(BookmarkResource.class);
    private final boolean testing;
    private final UserBean userBean;

    @Context
    private HttpServletRequest servletRequest;

    /**
     * <p>
     * Constructor for BookmarkResource.
     * </p>
     */
    public BookmarkResource() {
        this.testing = false;
        this.userBean = BeanUtils.getUserBean();
    }

    /**
     * <p>
     * Constructor for BookmarkResource.
     * </p>
     *
     * @param userBean a {@link io.goobi.viewer.managedbeans.UserBean} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public BookmarkResource(UserBean userBean, HttpServletRequest request) {
        this.testing = true;
        this.userBean = userBean;
        this.servletRequest = request;
    }

    /**
     * Returns the session stored bookmark list, creating a new empty one if needed
     *
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/session/get/")
    @Produces({ MediaType.APPLICATION_JSON })
    public BookmarkList getSessionBookmarkList() throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            BookmarkList bookmarkList = DataManager.getInstance().getBookmarkManager().getOrCreateBookmarkList(session);
            return bookmarkList;
        }
        throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns the session stored bookmark list, creating a new empty one if needed
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @GET
    @Path("/mirador/session")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getSessionBookmarkListForMirador()
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            BookmarkList bookmarkList = DataManager.getInstance().getBookmarkManager().getOrCreateBookmarkList(session);
            if (bookmarkList != null) {
                return bookmarkList.getMiradorJsonObject(servletRequest.getContextPath());
            }
            return "";
        }
        throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Adds an item with the given pi to the session stored bookmark list, creating a new bookmark list if needed
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @POST
    @Path("/session/add/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addToSessionBookmarkList(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return addToSessionBookmarkList(pi, null, null);
    }

    /**
     * Adds an item with the given pi, logid and page number to the session stored bookmark list, creating a new bookmark list if needed
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param pageString a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @POST
    @Path("/session/add/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addToSessionBookmarkList(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString)
            throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session == null) {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = DataManager.getInstance().getBookmarkManager().addToBookmarkList(item, session);
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            String errorMessage = "Unable to create bookmark for pi = " + pi + ", page = " + pageString + " and logid = " + logId;
            logger.error(errorMessage);
            throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Deletes the bookmark with the given pi from the session store bookmark list This operation returns an object with the property "success: false"
     * if the operation failed (usually because the object wasn't in the bookmark list to begin with). Otherwise the return object contains "success:
     * true"
     *
     * @param pi a {@link java.lang.String} object.
     * @return an object containing the boolean property 'success', detailing wether the operation was successfull
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @DELETE
    @Path("/session/delete/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteFromSessionBookmarkList(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return deleteFromSessionBookmarkList(pi, null, null);
    }

    /**
     * Deletes the bookmark with the given pi, logid and page number from the session store bookmark list This operation returns an object with the
     * property "success: false" if the operation failed (usually because the object wasn't in the bookmark list to begin with). Otherwise the return
     * object contains "success: true"
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param pageString a {@link java.lang.String} object.
     * @return an object containing the boolean property 'success', detailing wether the operation was successfull
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/session/delete/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteFromSessionBookmarkList(@PathParam("pi") String pi, @PathParam("logid") String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session == null) {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = DataManager.getInstance().getBookmarkManager().removeFromBookself(item, session);
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            String errorMessage = "Unable to delete bookmark with pi = " + pi + " and logid = " + logId;
            logger.error(errorMessage);
            throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Deletes the entry bookmark list from the session store. Always returns an object with the property "success: true", unless an error occurs in
     * which case an error status code and an error object is returned
     *
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @DELETE
    @Path("/session/delete")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteSessionBookmarkList() throws RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            DataManager.getInstance().getBookmarkManager().deleteBookmarkList(session);
            return new SuccessMessage(true);
        }
        throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns "true" if the object with the given IP is in the session store bookmark list, "false" otherwise
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/session/contains/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Boolean isInSessionBookmarkList(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return isInSessionBookmarkList(pi, null, null);
    }

    /**
     * Returns "true" if the object with the given IP, logid and page number is in the session store bookmark list, "false" otherwise
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param pageString a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/session/contains/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Boolean isInSessionBookmarkList(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString)
            throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session == null) {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = DataManager.getInstance().getBookmarkManager().isInBookmarkList(item, session);
            return success;
        } catch (PresentationException e) {
            //no such document
            return false;
        } catch (IndexUnreachableException e) {
            String errorMessage = "Error looking for bookmark pi = " + pi + " and logid = " + logId;
            logger.error(errorMessage);
            throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Counts the items contained in the session store bookmark list and returns the number as plain integer If no session store bookmark list exists,
     * 0 is returned
     *
     * @return a {@link java.lang.Integer} object.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/session/count")
    @Produces({ MediaType.APPLICATION_JSON })
    public Integer countSessionBookmarks() throws RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            int count = DataManager.getInstance().getBookmarkManager().getBookmarkList(session).map(bs -> bs.getItems().size()).orElse(0);
            return count;
        }
        throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns all BookmarkList owned by the current user
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/user/get/")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<BookmarkList> getAllUserBookmarkLists() throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user != null) {
            return DataManager.getInstance().getDao().getBookmarkLists(user);
        }
        throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns all bookmark lists shared with the current user and not owned by him
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/shared/get/")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<BookmarkList> getAllSharedBookmarkLists() throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        return DataManager.getInstance()
                .getDao()
                .getAllBookmarkLists()
                .stream()
                .filter(bl -> !user.equals(bl.getOwner()))
                .filter(bs -> isSharedTo(bs, user))
                .collect(Collectors.toList());
    }

    /**
     * Returns all public bookmark lists
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/public/get/")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<BookmarkList> getAllPublicBookmarkLists() throws DAOException, IOException, RestApiException {
        return DataManager.getInstance().getDao().getPublicBookmarkLists();
    }

    /**
     * Returns the bookmark list with the given id, provided it is owned by the user or it is public or shared to him
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/user/get/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public BookmarkList getUserBookmarkListById(@PathParam("id") Long id) throws DAOException, IOException, RestApiException {

        BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkList(id);

        if (bookmarkList.isIsPublic()) {
            logger.trace("Serving public bookmark list " + id);
            return bookmarkList;
        }
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        if (user.equals(bookmarkList.getOwner())) {
            logger.trace("Serving bookmark list " + id + " owned by user " + user);
            return bookmarkList;
        } else if (isSharedTo(bookmarkList, user)) {
            logger.trace("Serving bookmark list " + id + " shared to user " + user);
            return bookmarkList;
        } else {
            throw new RestApiException("User has no access to bookmark list " + id + " - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * @param shareKey
     * @return
     * @throws DAOException If an error occured talking to the database
     * @throws RestApiException If no user session exists or if the user has no access to the requested list
     * @throws ContentNotFoundException If no list with the given key was found
     */
    public BookmarkList getSharedBookmarkList(String shareKey) throws DAOException, RestApiException, ContentNotFoundException {
        BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkListByShareKey(shareKey);

        if (bookmarkList == null) {
            throw new ContentNotFoundException("No bookmarklist found for key " + shareKey);
        }
        if (bookmarkList.hasShareKey()) {
            logger.trace("Serving shared bookmark list " + bookmarkList.getId());
            return bookmarkList;
        }
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        if (user.equals(bookmarkList.getOwner())) {
            logger.trace("Serving bookmark list " + bookmarkList.getId() + " owned by user " + user);
            return bookmarkList;
        } else if (isSharedTo(bookmarkList, user)) {
            logger.trace("Serving bookmark list " + bookmarkList.getId() + " shared to user " + user);
            return bookmarkList;
        } else {
            throw new RestApiException("User has no access to bookmark list " + bookmarkList.getId() + " - request refused",
                    HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Sets the name of the BookmarkList with the given id to the given name - provided the user owns such a bookmark list; otherwise 204 is returned
     *
     * @param id a {@link java.lang.Long} object.
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/user/get/{id}/set/name/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage setUserBookmarkListName(@PathParam("id") Long id, @PathParam("name") String name)
            throws DAOException, IOException, RestApiException {
        Optional<BookmarkList> o = getAllUserBookmarkLists().stream().filter(bs -> bs.getId().equals(id)).findFirst();
        if (!o.isPresent()) {
            throw new RestApiException("No bookmark list with id '" + id + "' found for current user", HttpServletResponse.SC_NO_CONTENT);
        }

        o.get().setName(name);
        DataManager.getInstance().getDao().updateBookmarkList(o.get());
        return new SuccessMessage(true);
    }

    /**
     * Adds a new Bookmark with the given pi, LOGID and page number to the current user's bookmark list with the given id Returns 203 if no matching
     * bookmark list was found or 400 if the Bookmark could not be created (wrong pi/logid/page)
     *
     * @param id a {@link java.lang.Long} object.
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param pageString a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @POST
    @Path("/user/get/{id}/add/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addBookmarkToUserBookmarkList(@PathParam("id") Long id, @PathParam("pi") String pi, @PathParam("logid") String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {
        Optional<BookmarkList> o = getAllUserBookmarkLists().stream().filter(bs -> bs.getId().equals(id)).findFirst();
        if (!o.isPresent()) {
            throw new RestApiException("No bookmark list with id '" + id + "' found for current user", HttpServletResponse.SC_NO_CONTENT);
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = o.get().addItem(item);
            DataManager.getInstance().getDao().updateBookmarkList(o.get());
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            throw new RestApiException(
                    "Failed to create bookmark with pi '" + pi + "', logid '" + logId + "' and page number '" + pageString + "': " + e.getMessage(),
                    HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Adds a new Bookmark with the given pi to the current users bookmark list with the given id Returns 203 if no matching bookmark list was found
     * or 400 if the Bookmark could not be created (wrong pi)
     *
     * @param id a {@link java.lang.Long} object.
     * @param pi a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @POST
    @Path("/user/get/{id}/add/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addBookmarkToUserBookmarkList(@PathParam("id") Long id, @PathParam("pi") String pi)
            throws DAOException, IOException, RestApiException {
        return addBookmarkToUserBookmarkList(id, pi, null, null);
    }

    /**
     * Removes a Bookmark with the given pi, logid and page number from the current users bookmark list with the given id Returns 203 if no matching
     * bookmark list was found or 400 if the requested Bookmark is invalid (wrong pi/logid/page)
     *
     * @param id a {@link java.lang.Long} object.
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param pageString a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @DELETE
    @Path("/user/get/{id}/delete/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteBookmarkFromUserBookmarkList(@PathParam("id") Long id, @PathParam("pi") String pi, @PathParam("logid") String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {
        Optional<BookmarkList> o = getAllUserBookmarkLists().stream().filter(bs -> bs.getId().equals(id)).findFirst();
        if (!o.isPresent()) {
            throw new RestApiException("No bookmark list with id '" + id + "' found for current user", HttpServletResponse.SC_NO_CONTENT);
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = o.get().removeItem(item);
            DataManager.getInstance().getDao().updateBookmarkList(o.get());
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            throw new RestApiException(
                    "Failed to create bookmark with pi '" + pi + "', logid '" + logId + "' and page number '" + pageString + "': " + e.getMessage(),
                    HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Removes a Bookmark with the given pi from the current users bookmark list with the given id Returns 203 if no matching bookmark list was found
     * or 400 if the requested Bookmark is invalid (wrong pi)
     *
     * @param id a {@link java.lang.Long} object.
     * @param pi a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @DELETE
    @Path("/user/get/{id}/delete/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteBookmarkFromUserBookmarkList(@PathParam("id") Long id, @PathParam("pi") String pi)
            throws DAOException, IOException, RestApiException {
        return deleteBookmarkFromUserBookmarkList(id, pi, null, null);
    }

    /**
     * Adds a new BookmarkList with the given name to the current users bookmark lists
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @POST
    @Path("/user/add/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addUserBookmarkList(@PathParam("name") String name) throws DAOException, IOException, RestApiException {

        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
        if (userHasBookmarkList(user, name)) {
            throw new RestApiException("BookmarkList '" + name + "' already exists for the current user", HttpServletResponse.SC_BAD_REQUEST);
        }

        BookmarkList bookmarkList = new BookmarkList();
        bookmarkList.setName(name);
        bookmarkList.setOwner(user);
        bookmarkList.setIsPublic(false);
        boolean success = DataManager.getInstance().getDao().addBookmarkList(bookmarkList);
        return new SuccessMessage(success);
    }

    /**
     * Adds a new BookmarkList with the given name to the current users bookmark lists
     *
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @POST
    @Path("/user/add")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addUserBookmarkList() throws DAOException, IOException, RestApiException {
        String name = SessionStoreBookmarkManager.generateNewBookmarkListName(getAllUserBookmarkLists());
        return addUserBookmarkList(name);
    }

    /**
     * Adds the current session bookmark list to the current user bookmark lists under a newly generated name
     *
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/user/addSessionBookmarkList")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addUserBookmarkListFromSession() throws DAOException, IOException, RestApiException {
        String name = SessionStoreBookmarkManager.generateNewBookmarkListName(getAllUserBookmarkLists());
        return addUserBookmarkListFromSession(name);
    }

    /**
     * Adds the current session bookmark list to the current user's bookmark lists under the given name
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/user/addSessionBookmarkList/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addUserBookmarkListFromSession(@PathParam("name") String name) throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        Optional<BookmarkList> bookmarkList = DataManager.getInstance().getBookmarkManager().getBookmarkList(servletRequest.getSession());
        if (bookmarkList.isPresent()) {
            bookmarkList.get().setName(name);
            boolean success = DataManager.getInstance().getDao().addBookmarkList(bookmarkList.get());
            return new SuccessMessage(success);
        }

        throw new RestApiException("No session bookmark list found", HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Deletes the current user's bookmark list with the given id. If no such bookmark list could be found a message with 'success:false' is returned,
     * otherwise one with 'success:true'
     *
     * @param id The bookmark list id
     * @return an object containing the boolean property 'success', detailing wether the operation was successfull
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @DELETE
    @Path("/user/delete/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteUserBookmarkList(@PathParam("id") Long id) throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        Optional<BookmarkList> bookmarkList = getBookmarkList(user, id);
        if (bookmarkList.isPresent()) {
            DataManager.getInstance().getDao().deleteBookmarkList(bookmarkList.get());
            return new SuccessMessage(true);
        }

        throw new RestApiException("No bookmark list with id '" + id + "' found for user " + user, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Returns the user bookmark list with the given ID.
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    @GET
    @Path("/mirador/user/{id}/")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getUserBookmarkListForMirador(@PathParam("id") Long id)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
        Optional<BookmarkList> bookmarkList = getBookmarkList(user, id);
        if (bookmarkList.isPresent()) {
            return bookmarkList.get().getMiradorJsonObject(servletRequest.getContextPath());
        }

        throw new RestApiException("No bookmark list with id '" + id + "' found for user " + user, HttpServletResponse.SC_NOT_FOUND);
    }

    @GET
    @Path("/mirador/shared/{key}/")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getSharedBookmarkListForMirador(@PathParam("key") String key)
            throws DAOException, PresentationException, ContentNotFoundException, ViewerConfigurationException, IndexUnreachableException {
        try {
            BookmarkList bookmarkList = getSharedBookmarkList(key);
            return bookmarkList.getMiradorJsonObject(servletRequest.getContextPath());
        } catch (ContentNotFoundException | RestApiException e) {
            throw new ContentNotFoundException("No matching bookmark list found");
        }
    }

    /**
     * Returns the bookmark list containing the object with the given pi, logid and page number if is contained in any bookmark list of the current
     * user Otherwise an json object with the property "success:false" is returned
     *
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @param pageString a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/user/contains/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<BookmarkList> getContainingUserBookmarkLists(@PathParam("pi") String pi, @PathParam("logid") String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {
        logger.trace("getContainingUserBookmarkList: {}/{}/{}", pi, pageString, logId);
        List<BookmarkList> bookmarkLists = getAllUserBookmarkLists();
        if (bookmarkLists == null) {
            return Collections.emptyList();
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            List<BookmarkList> containingShelves = bookmarkLists.stream().filter(bs -> bs.getItems().contains(item)).collect(Collectors.toList());
            return containingShelves;
        } catch (IndexUnreachableException | PresentationException e) {
            throw new RestApiException("Error retrieving bookmark lists: " + e.toString(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the bookmark list containing the object with the given pi if is contained in any bookmark list of the current user Otherwise an json
     * object with the property "success:false" is returned
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/user/contains/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<BookmarkList> getContainingUserBookmarkLists(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return getContainingUserBookmarkLists(pi, null, null);
    }

    /**
     * Counts the items contained in the current user's bookmark list with the given id and returns the number as plain integer If no session store
     * bookmark list exists, 0 is returned
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link java.lang.Long} object.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     */
    @GET
    @Path("/user/get/{id}/count")
    @Produces({ MediaType.APPLICATION_JSON })
    public Long countUserBookmarks(@PathParam("id") Long id) throws RestApiException, DAOException, IOException {
        return getUserBookmarkListById(id).getItems().stream().count();
    }

    /**
     * <p>
     * getPageOrder.
     * </p>
     *
     * @param pageString a {@link java.lang.String} object.
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getPageOrder(String pageString) {
        Integer order = null;
        if (StringUtils.isNotBlank(pageString) && pageString.matches("\\d+")) {
            try {
                order = Integer.parseInt(pageString);
            } catch (NumberFormatException e) {
                //not in integer range
            }
        }
        return order;
    }

    /**
     * Determines the current User using the UserBean instance stored in the session store. If no session is available, no UserBean could be found or
     * no user is logged in, NULL is returned
     * 
     * @param session
     * @return
     */
    private User getUser() {
        if (userBean == null) {
            logger.trace("Unable to get user: No UserBean found in session store.");
            return null;
        }
        User user = userBean.getUser();
        if (user == null) {
            logger.trace("Unable to get user: No user found in session store UserBean instance");
            return null;
        }
        // logger.trace("Found user {}", user);
        return user;
    }

    /**
     * @param bookmarkList
     * @param user
     * @return
     */
    private boolean isSharedTo(BookmarkList bookmarkList, User user) {
        return bookmarkList.getGroupShares().stream().anyMatch(group -> isInGroup(user, group));
    }

    /**
     * @param user
     * @param name
     * @return
     * @throws RestApiException
     * @throws IOException
     * @throws DAOException
     */
    private boolean userHasBookmarkList(User user, String name) throws DAOException, IOException, RestApiException {
        return getAllUserBookmarkLists().stream().anyMatch(bs -> bs.getName() != null && bs.getName().equals(name));
    }

    /**
     * @param user
     * @param id
     * @return
     * @throws DAOException
     */
    private static Optional<BookmarkList> getBookmarkList(User user, Long id) throws DAOException {
        List<BookmarkList> bookmarkLists = DataManager.getInstance().getDao().getBookmarkLists(user);
        if (bookmarkLists != null) {
            return bookmarkLists.stream().filter(bs -> bs.getId().equals(id)).findFirst();
        }

        return Optional.empty();
    }

    /**
     * <p>
     * isInGroup.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param group a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return a boolean.
     */
    public boolean isInGroup(User user, UserGroup group) {
        try {
            return group.getMembers().contains(user);
        } catch (DAOException e) {
            logger.error("Cannot determine user affiliation with group: " + e.toString());
            return false;
        }
    }

    /**
     * <p>
     * getAsCollection.
     * </p>
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/user/get/{id}/collection")
    @Produces({ MediaType.APPLICATION_JSON })
    @IIIFPresentationBinding
    public Collection getAsCollection(@PathParam("id") Long id) throws DAOException, RestApiException {

        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
        Optional<BookmarkList> list = getBookmarkList(user, id);
        if (list.isPresent()) {
            return createCollection(list.get());
        }

        throw new RestApiException("No bookmark list with id '" + id + "' found for user " + user, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * <p>
     * getAsCollection.
     * </p>
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link de.intranda.api.iiif.presentation.Collection} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @GET
    @Path("/key/{sharedKey}")
    @Produces({ MediaType.APPLICATION_JSON })
    @IIIFPresentationBinding
    public Collection getAsCollection(@PathParam("sharedKey") String sharedKey) throws ContentNotFoundException, DAOException {

        try {
            BookmarkList list = getSharedBookmarkList(sharedKey);
            Collection collection = createCollection(list);
            return collection;
        } catch (ContentNotFoundException | RestApiException e) {
            throw new ContentNotFoundException("No matching bookmark list found");
        }
    }

    /**
     * @param list
     * @return
     */
    public Collection createCollection(BookmarkList list) {
        ManifestBuilder builder = new ManifestBuilder(new ApiUrlManager(DataManager.getInstance().getConfiguration().getRestApiUrl()));
        Collection collection = new Collection(getCollectionURI(), list.getName());
        collection.setLabel(new SimpleMetadataValue(list.getName()));
        collection.setDescription(new SimpleMetadataValue(list.getDescription()));
        list.getItems().forEach(item -> {
            try {
                URI manifestURI = builder.getManifestURI(item.getPi());
                Manifest manifest = new Manifest(manifestURI);
                manifest.setLabel(new SimpleMetadataValue(item.getName()));
                manifest.setThumbnail(new ImageContent(URI.create(item.getRepresentativeImageUrl())));
                collection.addManifest(manifest);
            } catch (PresentationException | IndexUnreachableException | ViewerConfigurationException | DAOException e) {
                logger.error("Failed to add item " + item.getId() + " to manifest");
            }
        });
        return collection;
    }

    /**
     * <p>
     * getCollectionURI.
     * </p>
     *
     * @return a {@link java.net.URI} object.
     */
    public URI getCollectionURI() {
        String baseURI = ServletUtils.getServletPathWithoutHostAsUrlFromRequest(servletRequest);
        return URI.create(baseURI + servletRequest.getRequestURI());
    }
}
