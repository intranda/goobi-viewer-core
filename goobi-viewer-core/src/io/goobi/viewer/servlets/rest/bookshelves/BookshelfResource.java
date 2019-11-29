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
package io.goobi.viewer.servlets.rest.bookshelves;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.bookshelf.Bookshelf;
import io.goobi.viewer.model.bookshelf.BookshelfItem;
import io.goobi.viewer.model.bookshelf.SessionStoreBookshelfManager;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.servlets.rest.ErrorMessage;
import io.goobi.viewer.servlets.rest.SuccessMessage;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

/**
 * @author Florian Alpers
 *
 */
@Path("/bookshelves")
@ViewerRestServiceBinding
public class BookshelfResource {

    private static final Logger logger = LoggerFactory.getLogger(BookshelfResource.class);
    private final boolean testing;
    private final UserBean userBean;

    @Context
    private HttpServletRequest servletRequest;

    public BookshelfResource() {
        this.testing = false;
        this.userBean = BeanUtils.getUserBean();
    }

    public BookshelfResource(UserBean userBean, HttpServletRequest request) {
        this.testing = true;
        this.userBean = userBean;
        this.servletRequest = request;
    }

    /**
     * Returns the session stored bookshelf, creating a new empty one if needed
     * 
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/get/")
    @Produces({ MediaType.APPLICATION_JSON })
    public Bookshelf getSessionBookshelf() throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            Bookshelf bookshelf = DataManager.getInstance().getBookshelfManager().getOrCreateBookshelf(session);
            return bookshelf;
        }
        throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns the session stored bookshelf, creating a new empty one if needed
     * 
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     * @throws ViewerConfigurationException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    @GET
    @Path("/session/mirador")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getSessionBookshelfForMirador()
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            Bookshelf bookshelf = DataManager.getInstance().getBookshelfManager().getOrCreateBookshelf(session);
            if (bookshelf != null) {
                return bookshelf.getMiradorJsonObject(servletRequest.getContextPath());
            }
            return "";
        }
        throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Adds an item with the given pi to the session stored bookshelf, creating a new bookshelf if needed
     * 
     * @param pi
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/add/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addToSessionBookshelf(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return addToSessionBookshelf(pi, null, null);
    }

    /**
     * Adds an item with the given pi, logid and page number to the session stored bookshelf, creating a new bookshelf if needed
     * 
     * @param pi
     * @param logId
     * @param pageString
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/add/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addToSessionBookshelf(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString)
            throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session == null) {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        try {
            BookshelfItem item = new BookshelfItem(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = DataManager.getInstance().getBookshelfManager().addToBookshelf(item, session);
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            String errorMessage = "Unable to create bookshelf item for pi = " + pi + ", page = " + pageString + " and logid = " + logId;
            logger.error(errorMessage);
            throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Deletes the bookshelf item with the given pi from the session store bookshelf This operation returns an object with the property "success:
     * false" if the operation failed (usually because the object wasn't in the bookshelf to begin with). Otherwise the return object contains
     * "success: true"
     * 
     * @param pi
     * @return an object containing the boolean property 'success', detailing wether the operation was successfull
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/delete/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteFromSessionBookshelf(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return deleteFromSessionBookshelf(pi, null, null);
    }

    /**
     * Deletes the bookshelf item with the given pi, logid and page number from the session store bookshelf This operation returns an object with the
     * property "success: false" if the operation failed (usually because the object wasn't in the bookshelf to begin with). Otherwise the return
     * object contains "success: true"
     * 
     * @param pi
     * @param logId
     * @param pageString
     * @return an object containing the boolean property 'success', detailing wether the operation was successfull
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/delete/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteFromSessionBookshelf(@PathParam("pi") String pi, @PathParam("logid") String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session == null) {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        try {
            BookshelfItem item = new BookshelfItem(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = DataManager.getInstance().getBookshelfManager().removeFromBookself(item, session);
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            String errorMessage = "Unable to delete bookshelf item with pi = " + pi + " and logid = " + logId;
            logger.error(errorMessage);
            throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Deletes the entry bookshelf from the session store. Always returns an object with the property "success: true", unless an error occurs in which
     * case an error status code and an error object is returned
     * 
     * @return
     * @throws RestApiException
     */
    @GET
    @Path("/session/delete")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteSessionBookshelf() throws RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            DataManager.getInstance().getBookshelfManager().deleteBookshelf(session);
            return new SuccessMessage(true);
        }
        throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns "true" if the object with the given IP is in the session store bookshelf, "false" otherwise
     * 
     * @param pi
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/contains/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Boolean isInSessionBookshelf(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return isInSessionBookshelf(pi, null, null);
    }

    /**
     * Returns "true" if the object with the given IP, logid and page number is in the session store bookshelf, "false" otherwise
     * 
     * @param pi
     * @param logId
     * @param pageString
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/contains/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Boolean isInSessionBookshelf(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString)
            throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session == null) {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        try {
            BookshelfItem item = new BookshelfItem(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = DataManager.getInstance().getBookshelfManager().isInBookshelf(item, session);
            return success;
        } catch (PresentationException e) {
            //no such document
            return false;
        } catch (IndexUnreachableException e) {
            String errorMessage = "Error looking for bookshelf item pi = " + pi + " and logid = " + logId;
            logger.error(errorMessage);
            throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Counts the items contained in the session store bookshelf and returns the number as plain integer If no session store bookshelf exists, 0 is
     * returned
     * 
     * @return
     * @throws RestApiException
     */
    @GET
    @Path("/session/count")
    @Produces({ MediaType.APPLICATION_JSON })
    public Integer countSessionBookshelfItems() throws RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            int count = DataManager.getInstance().getBookshelfManager().getBookshelf(session).map(bs -> bs.getItems().size()).orElse(0);
            return count;
        }
        throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns all Bookshelves owned by the current user
     * 
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/user/get/")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<Bookshelf> getAllUserBookshelfs() throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user != null) {
            return DataManager.getInstance().getDao().getBookshelves(user);
        }
        throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Returns all Bookshelves shared to the current user and not owned by him
     * 
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/shared/get/")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<Bookshelf> getAllSharedBookshelfs() throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        return DataManager.getInstance()
                .getDao()
                .getAllBookshelves()
                .stream()
                .filter(bs -> !user.equals(bs.getOwner()))
                .filter(bs -> isSharedTo(bs, user))
                .collect(Collectors.toList());
    }

    /**
     * Returns all public Bookshelves
     * 
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/public/get/")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<Bookshelf> getAllPublicBookshelfs() throws DAOException, IOException, RestApiException {
        return DataManager.getInstance().getDao().getPublicBookshelves();
    }

    /**
     * Returns the bookshelf with the given id, provided it is owned by the user or it is public or shared to him
     * 
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/user/get/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Bookshelf getUserBookshelfById(@PathParam("id") Long id) throws DAOException, IOException, RestApiException {
        Bookshelf bookshelf = DataManager.getInstance().getDao().getBookshelf(id);
        if (bookshelf.isPublic()) {
            logger.trace("Serving public bookshelf " + id);
            return bookshelf;
        }

        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        if (user.equals(bookshelf.getOwner())) {
            logger.trace("Serving bookshelf " + id + " owned by user " + user);
            return bookshelf;
        } else if (isSharedTo(bookshelf, user)) {
            logger.trace("Serving bookshelf " + id + " shared to user " + user);
            return bookshelf;
        } else {
            throw new RestApiException("User has no access to bookshelf " + id + " - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Sets the name of the Bookshelf with the given id to the given name - provided the user owns such a bookshelf; otherwise 204 is returned
     * 
     */
    @GET
    @Path("/user/get/{id}/set/name/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage setUserBookshelfName(@PathParam("id") Long id, @PathParam("name") String name)
            throws DAOException, IOException, RestApiException {
        Optional<Bookshelf> o = getAllUserBookshelfs().stream().filter(bs -> bs.getId().equals(id)).findFirst();
        if (!o.isPresent()) {
            throw new RestApiException("No bookshelf with id '" + id + "' found for current user", HttpServletResponse.SC_NO_CONTENT);
        }

        o.get().setName(name);
        DataManager.getInstance().getDao().updateBookshelf(o.get());
        return new SuccessMessage(true);
    }

    /**
     * Adds a new BookshelfItem with the given pi, logid and page number to the current users bookshelf with the given id Returns 203 if no matching
     * bookshelf was found or 400 if the BookshelfItem could not be created (wrong pi/logid/page)
     * 
     */
    @GET
    @Path("/user/get/{id}/add/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addItemToUserBookshelf(@PathParam("id") Long id, @PathParam("pi") String pi, @PathParam("logid") String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {
        Optional<Bookshelf> o = getAllUserBookshelfs().stream().filter(bs -> bs.getId().equals(id)).findFirst();
        if (!o.isPresent()) {
            throw new RestApiException("No bookshelf with id '" + id + "' found for current user", HttpServletResponse.SC_NO_CONTENT);
        }

        try {
            BookshelfItem item = new BookshelfItem(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = o.get().addItem(item);
            DataManager.getInstance().getDao().updateBookshelf(o.get());
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            throw new RestApiException("Failed to create bookshelf item with pi '" + pi + "', logid '" + logId + "' and page number '" + pageString
                    + "': " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Adds a new BookshelfItem with the given pi to the current users bookshelf with the given id Returns 203 if no matching bookshelf was found or
     * 400 if the BookshelfItem could not be created (wrong pi)
     * 
     */
    @GET
    @Path("/user/get/{id}/add/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addItemToUserBookshelf(@PathParam("id") Long id, @PathParam("pi") String pi)
            throws DAOException, IOException, RestApiException {
        return addItemToUserBookshelf(id, pi, null, null);
    }

    /**
     * Removes a BookshelfItem with the given pi, logid and page number from the current users bookshelf with the given id Returns 203 if no matching
     * bookshelf was found or 400 if the requested BookshelfItem is invalid (wrong pi/logid/page)
     * 
     */
    @GET
    @Path("/user/get/{id}/delete/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteFromUserBookshelf(@PathParam("id") Long id, @PathParam("pi") String pi, @PathParam("logid") String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {
        Optional<Bookshelf> o = getAllUserBookshelfs().stream().filter(bs -> bs.getId().equals(id)).findFirst();
        if (!o.isPresent()) {
            throw new RestApiException("No bookshelf with id '" + id + "' found for current user", HttpServletResponse.SC_NO_CONTENT);
        }

        try {
            BookshelfItem item = new BookshelfItem(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            boolean success = o.get().removeItem(item);
            DataManager.getInstance().getDao().updateBookshelf(o.get());
            return new SuccessMessage(success);
        } catch (IndexUnreachableException | PresentationException e) {
            throw new RestApiException("Failed to create bookshelf item with pi '" + pi + "', logid '" + logId + "' and page number '" + pageString
                    + "': " + e.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Removes a BookshelfItem with the given pi from the current users bookshelf with the given id Returns 203 if no matching bookshelf was found or
     * 400 if the requested BookshelfItem is invalid (wrong pi)
     * 
     * @throws RestApiException
     * @throws IOException
     * @throws DAOException
     * 
     */
    @GET
    @Path("/user/get/{id}/delete/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteFromUserBookshelf(@PathParam("id") Long id, @PathParam("pi") String pi)
            throws DAOException, IOException, RestApiException {
        return deleteFromUserBookshelf(id, pi, null, null);
    }

    /**
     * Adds a new Bookshelf with the given name to the current users bookshelves
     * 
     * @param pi
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/user/add/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addUserBookshelf(@PathParam("name") String name) throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
        if (userHasBookshelf(user, name)) {
            throw new RestApiException("Bookshelf '" + name + "' already exists for the current user", HttpServletResponse.SC_BAD_REQUEST);
        }

        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setName(name);
        bookshelf.setOwner(user);
        bookshelf.setPublic(false);
        boolean success = DataManager.getInstance().getDao().addBookshelf(bookshelf);
        return new SuccessMessage(success);
    }

    /**
     * Adds a new Bookshelf with the given name to the current users bookshelves
     * 
     * @param pi
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/user/add")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addUserBookshelf() throws DAOException, IOException, RestApiException {
        String name = SessionStoreBookshelfManager.generateNewBookshelfName(getAllUserBookshelfs());
        return addUserBookshelf(name);
    }

    /**
     * Adds the current session bookshelf to the current user bookshelves under a newly generated name
     * 
     * @param pi
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/user/addSessionBookshelf")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addUserBookshelfFromSession() throws DAOException, IOException, RestApiException {
        String name = SessionStoreBookshelfManager.generateNewBookshelfName(getAllUserBookshelfs());
        return addUserBookshelfFromSession(name);
    }

    /**
     * Adds the current session bookshelf to the current user bookshelves under the given name
     * 
     * @param pi
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/user/addSessionBookshelf/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addUserBookshelfFromSession(@PathParam("name") String bookshelfName) throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        Optional<Bookshelf> bookshelf = DataManager.getInstance().getBookshelfManager().getBookshelf(servletRequest.getSession());
        if (bookshelf.isPresent()) {
            bookshelf.get().setName(bookshelfName);
            boolean success = DataManager.getInstance().getDao().addBookshelf(bookshelf.get());
            return new SuccessMessage(success);
        }
        
        throw new RestApiException("No session bookshelf found", HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Deletes the current user's bookshelf with the given id. If no such bookshelf could be found a message with 'success:false' is returned,
     * otherwise one with 'success:true'
     * 
     * @param id The bookshelf id
     * @return an object containing the boolean property 'success', detailing wether the operation was successfull
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException if no current user was found
     */
    @GET
    @Path("/user/delete/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteUserBookshelf(@PathParam("id") Long id) throws DAOException, IOException, RestApiException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }

        Optional<Bookshelf> bookshelf = getBookshelf(user, id);
        if (bookshelf.isPresent()) {
            DataManager.getInstance().getDao().deleteBookshelf(bookshelf.get());
            return new SuccessMessage(true);
        }

        throw new RestApiException("No bookshelf with id '" + id + "' found for user " + user, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Returns the user bookshelf with the given ID.
     * 
     * @param id
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     * @throws ViewerConfigurationException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    @GET
    @Path("/user/mirador/{id}/")
    @Produces({ MediaType.APPLICATION_JSON })
    public String getUserBookshelfForMirador(@PathParam("id") Long id)
            throws DAOException, IOException, RestApiException, ViewerConfigurationException, IndexUnreachableException, PresentationException {
        User user = getUser();
        if (user == null) {
            throw new RestApiException("No user available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
        Optional<Bookshelf> bookshelf = getBookshelf(user, id);
        if (bookshelf.isPresent()) {
            return bookshelf.get().getMiradorJsonObject(servletRequest.getContextPath());
        }

        throw new RestApiException("No bookshelf with id '" + id + "' found for user " + user, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Returns the bookshelf containing the object with the given pi, logid and page number if is contained in any bookshelf of the current user
     * Otherwise an json object with the property "success:false" is returned
     * 
     * @param pi
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/user/contains/{pi}/{page}/{logid}")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<Bookshelf> getContainingUserBookshelves(@PathParam("pi") String pi, @PathParam("logid") String logId,
            @PathParam("page") String pageString) throws DAOException, IOException, RestApiException {
        List<Bookshelf> bookshelves = getAllUserBookshelfs();
        if (bookshelves == null) {
            return Collections.emptyList();
        }

        try {
            BookshelfItem item = new BookshelfItem(pi, "-".equals(logId) ? null : logId, getPageOrder(pageString), testing);
            List<Bookshelf> containingShelves = bookshelves.stream().filter(bs -> bs.getItems().contains(item)).collect(Collectors.toList());
            return containingShelves;
        } catch (IndexUnreachableException | PresentationException e) {
            throw new RestApiException("Error retrieving bookshelves: " + e.toString(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Returns the bookshelf containing the object with the given pi if is contained in any bookshelf of the current user Otherwise an json object
     * with the property "success:false" is returned
     * 
     * @param pi
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/user/contains/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<Bookshelf> getContainingUserBookshelves(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return getContainingUserBookshelves(pi, null, null);
    }

    /**
     * Counts the items contained in the current user's bookshelf with the given id and returns the number as plain integer If no session store
     * bookshelf exists, 0 is returned
     * 
     * @return
     * @throws RestApiException
     * @throws IOException
     * @throws DAOException
     */
    @GET
    @Path("/user/get/{id}/count")
    @Produces({ MediaType.APPLICATION_JSON })
    public Long countUserBookshelfItems(@PathParam("id") Long id) throws RestApiException, DAOException, IOException {
        return getUserBookshelfById(id).getItems().stream().count();
    }

    /**
     * @param pageString
     * @return
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
     * @param bookshelf
     * @param user
     * @return
     */
    private boolean isSharedTo(Bookshelf bookshelf, User user) {
        return bookshelf.getGroupShares().stream().anyMatch(group -> isInGroup(user, group));
    }

    /**
     * @param user
     * @param name
     * @return
     * @throws RestApiException
     * @throws IOException
     * @throws DAOException
     */
    private boolean userHasBookshelf(User user, String name) throws DAOException, IOException, RestApiException {
        return getAllUserBookshelfs().stream().anyMatch(bs -> bs.getName() != null && bs.getName().equals(name));
    }

    /**
     * @param user
     * @param id
     * @return
     * @throws DAOException
     */
    private static Optional<Bookshelf> getBookshelf(User user, Long id) throws DAOException {
        List<Bookshelf> bookshelves = DataManager.getInstance().getDao().getBookshelves(user);
        if (bookshelves != null) {
            return bookshelves.stream().filter(bs -> bs.getId().equals(id)).findFirst();
        }

        return Optional.empty();
    }

    /**
     * @param user
     * @param group
     * @return
     * @throws DAOException
     */
    public boolean isInGroup(User user, UserGroup group) {
        try {
            return group.getMembers().contains(user);
        } catch (DAOException e) {
            logger.error("Cannot determine user affiliation with group: " + e.toString());
            return false;
        }
    }

    //    @PUT
    //    @Path("/testPost")
    //    @Consumes(MediaType.APPLICATION_JSON)
    //    public Response test(Bookshelf bs) {
    //        System.out.println("Received data " + bs);
    //        return Response.ok().build();
    //    }

}
