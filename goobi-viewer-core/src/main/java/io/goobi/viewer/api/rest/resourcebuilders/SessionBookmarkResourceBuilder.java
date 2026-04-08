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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.iiif.presentation.v2.Collection2;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.model.SuccessMessage;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;

public class SessionBookmarkResourceBuilder extends AbstractBookmarkResourceBuilder {

    private static final Logger logger = LogManager.getLogger(SessionBookmarkResourceBuilder.class);

    private final HttpSession session;

    public SessionBookmarkResourceBuilder(HttpSession session) {
        this.session = session;
    }

    /**
     * Returns the session stored bookmark list, creating a new empty one if needed.
     *
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public List<BookmarkList> getAllBookmarkLists() throws DAOException, IOException, RestApiException {
        BookmarkList bookmarkList = DataManager.getInstance().getBookmarkManager().getOrCreateBookmarkList(session);
        return Collections.singletonList(bookmarkList);
    }

    /**
     * Returns the session stored bookmark list, creating a new empty one if needed.
     *
     * @param id ignored; session has only one bookmark list
     * @param urls URL manager used to build application and API URLs
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public String getBookmarkListForMirador(Long id, AbstractApiUrlManager urls)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException {
        BookmarkList bookmarkList = DataManager.getInstance().getBookmarkManager().getOrCreateBookmarkList(session);
        if (bookmarkList != null) {
            return bookmarkList.getMiradorJsonObject(urls.getApplicationUrl(), urls.getApiUrl());
        }
        return "";
    }

    /**
     * Adds an item with the given pi to the session stored bookmark list, creating a new bookmark list if needed.
     *
     * @param id ignored; session has only one bookmark list
     * @param pi persistent identifier of the record to bookmark
     * @return a {@link io.goobi.viewer.api.rest.model.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public SuccessMessage addBookmarkToBookmarkList(Long id, String pi) throws DAOException, IOException, RestApiException {
        return addBookmarkToBookmarkList(id, pi, null, null);
    }

    /**
     * Adds an item with the given pi, logid and page number to the session stored bookmark list, creating a new bookmark list if needed.
     *
     * @param id ignored; session has only one bookmark list
     * @param pi persistent identifier of the record to bookmark
     * @param logId structural element log ID, or "-" to indicate none
     * @param pageString page order number as string, may be null
     * @return a {@link io.goobi.viewer.api.rest.model.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public SuccessMessage addBookmarkToBookmarkList(Long id, String pi, String logId, String pageString)
            throws DAOException, IOException, RestApiException {
        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), false);
            item.setId(System.nanoTime());
            boolean success = DataManager.getInstance().getBookmarkManager().addToBookmarkList(item, session);
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            String errorMessage = "Unable to create bookmark for pi = " + pi + ", page = " + pageString + " and logid = " + logId;
            logger.error(errorMessage);
            throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Deletes the bookmark with the given pi from the session store bookmark list. This operation returns an object with the property
     * "success: false" if the operation failed (usually because the object wasn't in the bookmark list to begin with). Otherwise the
     * return object contains "success: true"
     *
     * @param id ignored; session has only one bookmark list
     * @param pi persistent identifier of the record to remove
     * @return an object containing the boolean property 'success', detailing whether the operation was successful
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public SuccessMessage deleteBookmarkFromBookmarkList(Long id, String pi) throws DAOException, IOException, RestApiException {
        return deleteBookmarkFromBookmarkList(id, pi, null, null);
    }

    /**
     * Deletes the bookmark with the given pi, logid and page number from the session store bookmark list. This operation returns an object with the
     * property "success: false" if the operation failed (usually because the object wasn't in the bookmark list to begin with). Otherwise the return
     * object contains "success: true"
     *
     * @param id ignored; session has only one bookmark list
     * @param pi persistent identifier of the record to remove
     * @param logId structural element log ID, or "-" to indicate none
     * @param pageString page order number as string, may be null
     * @return an object containing the boolean property 'success', detailing whether the operation was successful
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public SuccessMessage deleteBookmarkFromBookmarkList(Long id, String pi, String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), false);
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
     * @return a {@link io.goobi.viewer.api.rest.model.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public SuccessMessage deleteSessionBookmarkList() throws RestApiException {
        DataManager.getInstance().getBookmarkManager().deleteBookmarkList(session);
        return new SuccessMessage(true);
    }

    /**
     * Returns "true" if the object with the given IP is in the session store bookmark list, "false" otherwise.
     *
     * @param pi persistent identifier of the record to look up
     * @return a {@link java.lang.Boolean} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public Boolean isInSessionBookmarkList(String pi) throws DAOException, IOException, RestApiException {
        return isInSessionBookmarkList(pi, null, null);
    }

    /**
     * Returns "true" if the object with the given IP, logid and page number is in the session store bookmark list, "false" otherwise.
     *
     * @param pi persistent identifier of the record to look up
     * @param logId structural element log ID, or "-" to indicate none
     * @param pageString page order number as string, may be null
     * @return a {@link java.lang.Boolean} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public Boolean isInSessionBookmarkList(String pi, String logId, String pageString)
            throws DAOException, IOException, RestApiException {
        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), false);
            return DataManager.getInstance().getBookmarkManager().isInBookmarkList(item, session);
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
     * Counts the items contained in the session store bookmark list and returns the number as plain integer. If no session store bookmark
     * list exists, 0 is returned
     *
     * @return a {@link java.lang.Integer} object.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public Integer countSessionBookmarks() throws RestApiException {
        return DataManager.getInstance().getBookmarkManager().getBookmarkList(session).map(bs -> bs.getItems().size()).orElse(0);
    }

    /**
     * Returns the bookmark list with the given id, provided it is owned by the user or it is public or shared to him.
     *
     * @param id database ID of the requested bookmark list
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public BookmarkList getUserBookmarkListById(Long id) throws DAOException, IOException, RestApiException {

        BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkList(id);

        if (bookmarkList.isIsPublic()) {
            logger.trace("Serving public bookmark list {}", id);
            return bookmarkList;
        }
        throw new RestApiException("Bookmark list " + id + " is not publicly accessible", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * @param shareKey the share key identifying the bookmark list
     * @return {@link BookmarkList} that match shareKey
     * @throws DAOException If an error occurred talking to the database
     * @throws RestApiException If no user session exists or if the user has no access to the requested list
     * @throws ContentNotFoundException If no list with the given key was found
     */
    @Override
    public BookmarkList getSharedBookmarkList(String shareKey) throws DAOException, RestApiException, ContentNotFoundException {
        BookmarkList bookmarkList;
        try {
            bookmarkList = DataManager.getInstance().getDao().getBookmarkListByShareKey(shareKey);
        } catch (jakarta.persistence.PersistenceException e) {
            // DB collation mismatch (utf8mb3 vs utf8mb4) causes PersistenceException for unicode share keys.
            // Treat as "not found" since such keys can never match a valid entry.
            throw new ContentNotFoundException("No bookmarklist found for key " + shareKey);
        }

        if (bookmarkList == null) {
            throw new ContentNotFoundException("No bookmarklist found for key " + shareKey);
        }
        if (bookmarkList.hasShareKey()) {
            logger.trace("Serving shared bookmark list {}", bookmarkList.getId());
            return bookmarkList;
        }
        throw new RestApiException("Bookmark list " + bookmarkList.getId() + " is not publicly accessible", HttpServletResponse.SC_FORBIDDEN);
    }

    @Override
    public Collection2 createCollection(BookmarkList list, AbstractApiUrlManager urls) {
        String url = urls.path(ApiUrls.USERS_BOOKMARKS, ApiUrls.USERS_BOOKMARKS_LIST_IIIF).params("-", list.getId()).build();
        return createCollection(list, url);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.api.rest.resourcebuilders.AbstractBookmarkResourceBuilder#addBookmarkList()
     */
    @Override
    public SuccessMessage addBookmarkList() throws DAOException, IOException, RestApiException, IllegalRequestException {
        // Session users may only have one bookmark list; adding another is a conflict, not a bad request
        throw new WebApplicationException("Cannot add additional session bookmark lists", Response.Status.CONFLICT);
    }

    @Override
    public SuccessMessage addBookmarkList(String name) throws DAOException, IOException, RestApiException, IllegalRequestException {
        // Session users may only have one bookmark list; adding another is a conflict, not a bad request
        throw new WebApplicationException("Cannot add additional session bookmark lists", Response.Status.CONFLICT);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.api.rest.resourcebuilders.AbstractBookmarkResourceBuilder#getBookmarkListById(java.lang.Long)
     */
    @Override
    public BookmarkList getBookmarkListById(Long id) throws DAOException, IOException, RestApiException {
        return DataManager.getInstance().getBookmarkManager().getOrCreateBookmarkList(session);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.api.rest.resourcebuilders.AbstractBookmarkResourceBuilder#updateBookmarkList(io.goobi.viewer.model.bookmark.BookmarkList)
     */
    @Override
    public void updateBookmarkList(BookmarkList list) throws IllegalRequestException {
        // Session bookmark lists cannot be updated; treat as conflict, not bad request
        throw new WebApplicationException("Cannot update session bookmark list", Response.Status.CONFLICT);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.api.rest.resourcebuilders.AbstractBookmarkResourceBuilder#deleteBookmarkList(java.lang.Long)
     */
    @Override
    public SuccessMessage deleteBookmarkList(Long id) throws DAOException, IOException, RestApiException, IllegalRequestException {
        DataManager.getInstance().getBookmarkManager().deleteBookmarkList(session);
        DataManager.getInstance().getBookmarkManager().createBookmarkList(session);
        return new SuccessMessage(true);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.api.rest.resourcebuilders.AbstractBookmarkResourceBuilder#getAsCollection(java.lang.Long, AbstractApiUrlManager)
     */
    @Override
    public Collection2 getAsCollection(Long id, AbstractApiUrlManager urls) throws DAOException, RestApiException, IOException {
        BookmarkList list = getBookmarkListById(id);
        return createCollection(list, urls);
    }
}
