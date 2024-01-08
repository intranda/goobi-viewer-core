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
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.presentation.v2.Collection2;
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
import io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;

public class UserBookmarkResourceBuilder extends AbstractBookmarkResourceBuilder {

    private static final Logger logger = LogManager.getLogger(UserBookmarkResourceBuilder.class);

    private final User user;

    public UserBookmarkResourceBuilder(User user) {
        this.user = user;
    }

    /**
     * Returns all BookmarkList owned by the current user
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @Override
    public List<BookmarkList> getAllBookmarkLists() throws DAOException, IOException, RestApiException {
        List<BookmarkList> ret = DataManager.getInstance().getDao().getBookmarkLists(user);
        BookmarkList.sortBookmarkLists(ret);

        return ret;
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
    @Override
    public BookmarkList getBookmarkListById(Long id) throws DAOException, IOException, RestApiException {

        BookmarkList bookmarkList = DataManager.getInstance().getDao().getBookmarkList(id);

        if (bookmarkList == null) {
            throw new RestApiException("No Bookmark list foud with id " + id, HttpServletResponse.SC_NOT_FOUND);
        }
        if (bookmarkList.isIsPublic()) {
            logger.trace("Serving public bookmark list {}", id);
            return bookmarkList;
        }

        if (user.equals(bookmarkList.getOwner())) {
            logger.trace("Serving bookmark list {} owned by user {}", id, user);
            return bookmarkList;
        }
        throw new RestApiException("User has no access to bookmark list " + id + " - request refused", HttpServletResponse.SC_FORBIDDEN);
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
    @Override
    public SuccessMessage addBookmarkToBookmarkList(Long id, String pi, String logId,
            String pageString) throws DAOException, IOException, RestApiException {
        Optional<BookmarkList> o = getAllBookmarkLists().stream().filter(bs -> bs.getId().equals(id)).findFirst();
        if (!o.isPresent()) {
            throw new RestApiException("No bookmark list with id '" + id + "' found for current user", HttpServletResponse.SC_NO_CONTENT);
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), false);
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
    @Override
    public SuccessMessage addBookmarkToBookmarkList(Long id, String pi)
            throws DAOException, IOException, RestApiException {
        return addBookmarkToBookmarkList(id, pi, null, null);
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
    @Override
    public SuccessMessage deleteBookmarkFromBookmarkList(Long id, String pi, String logId,
            String pageString) throws DAOException, IOException, RestApiException {
        Optional<BookmarkList> o = getAllBookmarkLists().stream().filter(bs -> bs.getId().equals(id)).findFirst();
        if (!o.isPresent()) {
            throw new RestApiException("No bookmark list with id '" + id + "' found for current user", HttpServletResponse.SC_NO_CONTENT);
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), false);
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
    @Override
    public SuccessMessage deleteBookmarkFromBookmarkList(Long id, String pi)
            throws DAOException, IOException, RestApiException {
        return deleteBookmarkFromBookmarkList(id, pi, null, null);
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
    @Override
    public SuccessMessage addBookmarkList(String name) throws DAOException, IOException, RestApiException {

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
    @Override
    public SuccessMessage addBookmarkList() throws DAOException, IOException, RestApiException {
        String name = SessionStoreBookmarkManager.generateNewBookmarkListName(getAllBookmarkLists());
        return addBookmarkList(name);
    }

    /**
     * Adds the current session bookmark list to the current user bookmark lists under a newly generated name
     * 
     * @param session
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public SuccessMessage addUserBookmarkListFromSession(HttpSession session) throws DAOException, IOException, RestApiException {
        String name = SessionStoreBookmarkManager.generateNewBookmarkListName(getAllBookmarkLists());
        return addUserBookmarkListFromSession(name, session);
    }

    /**
     * Adds the current session bookmark list to the current user's bookmark lists under the given name
     *
     * @param name a {@link java.lang.String} object.
     * @param session
     * @return a {@link io.goobi.viewer.servlets.rest.SuccessMessage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    public SuccessMessage addUserBookmarkListFromSession(String name, HttpSession session) throws DAOException, IOException, RestApiException {

        Optional<BookmarkList> bookmarkList = DataManager.getInstance().getBookmarkManager().getBookmarkList(session);
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
    @Override
    public SuccessMessage deleteBookmarkList(Long id) throws DAOException, IOException, RestApiException {

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
    @Override
    public String getBookmarkListForMirador(Long id, AbstractApiUrlManager urls)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException {

        Optional<BookmarkList> bookmarkList = getBookmarkList(user, id);
        if (bookmarkList.isPresent()) {
            return bookmarkList.get().getMiradorJsonObject(urls.getApplicationUrl(), urls.getApiUrl());
        }

        throw new RestApiException("No bookmark list with id '" + id + "' found for user " + user, HttpServletResponse.SC_NOT_FOUND);
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
    public List<BookmarkList> getContainingUserBookmarkLists(String pi, String logId,
            String pageString) throws DAOException, IOException, RestApiException {
        logger.trace("getContainingUserBookmarkList: {}/{}/{}", pi, pageString, logId);
        List<BookmarkList> bookmarkLists = getAllBookmarkLists();
        if (bookmarkLists == null) {
            return Collections.emptyList();
        }

        try {
            Bookmark item = new Bookmark(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), false);
            return bookmarkLists.stream().filter(bs -> bs.getItems().contains(item)).collect(Collectors.toList());
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
    public List<BookmarkList> getContainingUserBookmarkLists(String pi) throws DAOException, IOException, RestApiException {
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
    public Long countUserBookmarks(Long id) throws RestApiException, DAOException, IOException {
        return getBookmarkListById(id).getItems().stream().count();
    }

    /**
     * @param user
     * @param name
     * @return true if given user has bookmark list with given name; false otherwise
     * @throws RestApiException
     * @throws IOException
     * @throws DAOException
     */
    private boolean userHasBookmarkList(User user, String name) throws DAOException, IOException, RestApiException {
        return getAllBookmarkLists().stream().anyMatch(bs -> bs.getName() != null && bs.getName().equals(name));
    }

    /**
     * @param user
     * @param id
     * @return Optional<BookmarkList>
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
            logger.error("Cannot determine user affiliation with group: {}", e.toString());
            return false;
        }
    }

    /**
     * <p>
     * getAsCollection.
     * </p>
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link de.intranda.api.iiif.presentation.v2.Collection2} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.RestApiException if any.
     */
    @Override
    public Collection2 getAsCollection(Long id, AbstractApiUrlManager urls) throws DAOException, RestApiException {

        Optional<BookmarkList> list = getBookmarkList(user, id);
        if (list.isPresent()) {
            return createCollection(list.get(), urls);
        }

        throw new RestApiException("No bookmark list with id '" + id + "' found for user " + user, HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    public Collection2 createCollection(BookmarkList list, AbstractApiUrlManager urls) {
        String url = urls.path(ApiUrls.USERS_BOOKMARKS, ApiUrls.USERS_BOOKMARKS_LIST_IIIF).params(user.getId(), list.getId()).build();
        return createCollection(list, url);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.api.rest.resourcebuilders.AbstractBookmarkResourceBuilder#updateBookmarkList(io.goobi.viewer.model.bookmark.BookmarkList)
     */
    @Override
    public void updateBookmarkList(BookmarkList bookmarkList) throws IllegalRequestException, DAOException {
        if (bookmarkList.getOwner() != null && bookmarkList.getOwner().getId() != null && bookmarkList.getOwner().getId().equals(this.user.getId())) {
            DataManager.getInstance().getDao().updateBookmarkList(bookmarkList);
        } else {
            throw new IllegalRequestException("Cannot update foreign bookmark list");

        }
    }

}
