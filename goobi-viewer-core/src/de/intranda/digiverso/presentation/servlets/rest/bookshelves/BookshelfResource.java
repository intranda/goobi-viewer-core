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
package de.intranda.digiverso.presentation.servlets.rest.bookshelves;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.RestApiException;
import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.model.bookshelf.Bookshelf;
import de.intranda.digiverso.presentation.model.bookshelf.BookshelfItem;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.servlets.rest.SuccessMessage;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

/**
 * @author Florian Alpers
 *
 */
@Path("/bookshelves")
@ViewerRestServiceBinding
public class BookshelfResource {

    private static final Logger logger = LoggerFactory.getLogger(BookshelfResource.class);
    private final boolean testing;
    
    @Context
    private HttpServletRequest servletRequest;

    public BookshelfResource() {
        this.testing = false;
    }
    
    /**
     * For testing
     * @param request
     */
    protected BookshelfResource(HttpServletRequest request) {
        this.servletRequest = request;
        this.testing = true;
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
    public Bookshelf getBookshelf() throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            Bookshelf bookshelf = DataManager.getInstance().getBookshelfManager().getOrCreateBookshelf(session);
            return bookshelf;
        } else {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
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
    public SuccessMessage addToBookshelf(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return addToBookshelf(pi, null, null);
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
    @Path("/session/add/{pi}/{logid}/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addToBookshelf(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString)
            throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            try {
                BookshelfItem item = new BookshelfItem(pi, logId, getPageOrder(pageString), testing);
                boolean success = DataManager.getInstance().getBookshelfManager().addToBookshelf(item, session);
                return new SuccessMessage(success);
            } catch (IndexUnreachableException | PresentationException e) {
                String errorMessage = "Unable to create bookshelf item for pi = " + pi + " and logid = " + logId;
                logger.error(errorMessage);
                //                servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to create bookshelf item for pi = " + pi + " and logid = " + logId);
                throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
            //            servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "No session available - request refused");
            //            return null;
        }
    }

    /**
     * Deletes the bookshelf item with the given pi from the session store bookshelf
     * This operation returns an object with the property "success: false" if the operation failed (usually because the
     * object wasn't in the bookshelf to begin with). Otherwise the return opject contains "success: true"
     * 
     * @param pi
     * @return  an object containing the boolean property 'success', detailing wether the operation was successfull
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/delete/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteFromBookshelf(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return deleteFromBookshelf(pi, null, null);
    }

    /**
     * Deletes the bookshelf item with the given pi, logid and page number from the session store bookshelf
     * This operation returns an object with the property "success: false" if the operation failed (usually because the
     * object wasn't in the bookshelf to begin with). Otherwise the return opject contains "success: true"
     * 
     * @param pi
     * @param logId
     * @param pageString
     * @return  an object containing the boolean property 'success', detailing wether the operation was successfull
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException
     */
    @GET
    @Path("/session/delete/{pi}/{logid}/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteFromBookshelf(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString)
            throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {

            try {
                BookshelfItem item = new BookshelfItem(pi, logId, getPageOrder(pageString), testing);
                boolean success = DataManager.getInstance().getBookshelfManager().removeFromBookself(item, session);
                return new SuccessMessage(success);
            } catch (IndexUnreachableException | PresentationException e) {
                String errorMessage = "Unable to delete bookshelf item with pi = " + pi + " and logid = " + logId;
                logger.error(errorMessage);
                throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Deletes the entiry bookshelf from the session store. Always returns an object with the 
     * property "success: true", unless an error occurs in which case an error status code and an error object is returned
     * 
     * @return
     * @throws RestApiException
     */
    @GET
    @Path("/session/delete")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage deleteBookshelf() throws RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            DataManager.getInstance().getBookshelfManager().deleteBookshelf(session);
            return new SuccessMessage(true);
        } else {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
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
    public Boolean isInBookshelf(@PathParam("pi") String pi) throws DAOException, IOException, RestApiException {
        return isInBookshelf(pi, null, null);
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
    @Path("/session/contains/{pi}/{logid}/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Boolean isInBookshelf(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString)
            throws DAOException, IOException, RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {

            try {
                BookshelfItem item = new BookshelfItem(pi, logId, getPageOrder(pageString), testing);
                boolean success = DataManager.getInstance().getBookshelfManager().isInBookshelf(item, session);
                return success;
            } catch(PresentationException e) {
                //no such document
                return false;
            } catch (IndexUnreachableException e) {
                String errorMessage = "Error looking for bookshelf item pi = " + pi + " and logid = " + logId;
                logger.error(errorMessage);
                throw new RestApiException(errorMessage, e, HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
    }
    
    /**
     * Counts the items contained in the session store bookshelf and returns the number as plain integer
     * If no session store bookshelf exists, 0 is returned
     * 
     * @return
     * @throws RestApiException
     */
    @GET
    @Path("/session/count")
    @Produces({ MediaType.APPLICATION_JSON })
    public Integer countItems() throws RestApiException {
        HttpSession session = servletRequest.getSession();
        if (session != null) {
            int count = DataManager.getInstance().getBookshelfManager().getBookshelf(session).map(bs -> bs.getItems().size()).orElse(0);
            return count;
        } else {
            throw new RestApiException("No session available - request refused", HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Retrieves a bookshelf with name @bookshelfName from the currently logged in user if one could be found. If no bookshelf of this name is found
     * for the current user, 404 is returned
     * 
     * 
     * @param bookshelfName
     * @return
     * @throws DAOException
     * @throws IOException
     * @throws RestApiException 
     */
    @GET
    @Path("/get/{bookshelfId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Bookshelf getBookshelf(@PathParam("bookshelfId") Long id) throws DAOException, IOException, RestApiException {

        User user = getUserFromSession(servletRequest.getSession());

        if (user != null) {
            Bookshelf shelf = DataManager.getInstance().getDao().getBookshelf(id);
            if (shelf != null) {
                if (shelf.getOwner().equals(user)) {
                    return shelf;
                }
            }
        }
        throw new RestApiException("Did not find bookshelf " + id + " for user " + user, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Adds the object determined by the passed parameters to the session store bookshelf returns a json object with the single property "stored"
     * containing a boolean which is true exactly if the object is in the session bookshelf after the operation
     * 
     * @param pi
     * @param logid
     * @param page
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    @GET
    @Path("/add/{pi}/{logid}/{page}")
    @Produces({ MediaType.APPLICATION_JSON })
    public SuccessMessage addToSessionStore(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString)
            throws IndexUnreachableException, PresentationException {

        if ("-".equals(pi)) {
            pi = "";
        }
        if ("-".equals(logId)) {
            logId = "";
        }
        if ("-".equals(pageString)) {
            pageString = "";
        }
        Integer order = getPageOrder(pageString);

        boolean success = false;

        BookshelfItem item = new BookshelfItem(pi, logId, order, testing);

        return new SuccessMessage(success);
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
    private User getUserFromSession(HttpSession session) {
        if (session == null) {
            logger.debug("Unable to get user: No session available");
            return null;
        } else {
            UserBean userBean = (UserBean) session.getAttribute("userBean");
            if (userBean == null) {
                logger.debug("Unable to get user: No UserBean found in session store.");
                return null;
            } else {
                User user = userBean.getUser();
                if (user == null) {
                    logger.debug("Unable to get user: No user found in session store UserBean instance");
                    return null;
                } else {
                    logger.debug("Found user " + user);
                    return user;
                }
            }
        }
    }

}
