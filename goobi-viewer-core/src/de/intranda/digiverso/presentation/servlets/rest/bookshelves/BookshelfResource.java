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
import org.apache.solr.common.SolrDocument;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.model.bookshelf.Bookshelf;
import de.intranda.digiverso.presentation.model.bookshelf.BookshelfItem;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

/**
 * @author Florian Alpers
 *
 */
@Path("/bookshelves")
@ViewerRestServiceBinding
public class BookshelfResource {
    
    private static final Logger logger = LoggerFactory.getLogger(BookshelfResource.class);
    
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    
    /**
     * Retrieves a bookshelf with name @bookshelfName from the currently logged in user if one could be found.
     * If no bookshelf of this name is found for the current user, 404 is returned
     * 
     * 
     * @param bookshelfName
     * @return
     * @throws DAOException 
     * @throws IOException 
     */
    @GET
    @Path("/get/{bookshelfId}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Bookshelf getBookshelf(@PathParam("bookshelfId") Long id) throws DAOException, IOException {
        
        User user = getUserFromSession(servletRequest.getSession());
        
        if(user != null) {
            Bookshelf shelf = DataManager.getInstance().getDao().getBookshelf(id);
            if(shelf != null) {                
                if(shelf.getOwner().equals(user)) {
                    return shelf;
                }
            }
        }
        servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Did not find bookshelf " + id + " for user " + user);
        return null; 
    }
    
    /**
     * Adds the object determined by the passed parameters to the session store bookshelf
     * returns a json object with the single property "stored" containing a boolean which is true exactly if the object is in the session 
     * bookshelf after the operation
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
    public JSONObject addToSessionStore(@PathParam("pi") String pi, @PathParam("logid") String logId, @PathParam("page") String pageString) throws IndexUnreachableException, PresentationException {
        
        if("-".equals(pi)) {
            pi = "";
        }
        if("-".equals(logId)) {
            logId = "";
        }
        if("-".equals(pageString)) {
            pageString = "";
        }
        Integer order = getPageOrder(pageString);
        
        boolean success = false;

        BookshelfItem item = new BookshelfItem(pi, logId, order);
        Bookshelf sessionStoreBookshelf = getSessionStoreBookshelf(servletRequest.getSession());
        
        Map<String, Object> map = new HashMap<>();
        map.put("stored", success);
        JSONObject result = new JSONObject(map);
        return result;
    }

    /**
     * @param session
     * @return
     */
    private Bookshelf getSessionStoreBookshelf(HttpSession session) {
        return DataManager.getInstance().getBookshelfManager().getOrCreateBookshelf(session);
    }

    /**
     * @param pageString
     * @return
     */
    public Integer getPageOrder(String pageString) {
        Integer order = null;
        if(StringUtils.isNotBlank(pageString) &&  pageString.matches("\\d+")) {
            try {
                order = Integer.parseInt(pageString);
            } catch(NumberFormatException e) {
                //not in integer range
            }
        }
        return order;
    }


    /**
     * Determines the current User using the UserBean instance stored in the session store.
     * If no session is available, no UserBean could be found or no user is logged in, NULL is returned
     * 
     * @param session
     * @return
     */
    private User getUserFromSession(HttpSession session) {
        if(session == null) {
            logger.debug("Unable to get user: No session available");
            return null;
        } else {
            UserBean userBean = (UserBean)session.getAttribute("userBean");
            if(userBean == null) {
                logger.debug("Unable to get user: No UserBean found in session store.");
                return null;
            } else {
                User user = userBean.getUser();
                if(user == null) {
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
