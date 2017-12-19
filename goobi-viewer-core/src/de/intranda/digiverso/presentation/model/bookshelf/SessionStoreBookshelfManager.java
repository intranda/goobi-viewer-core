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
package de.intranda.digiverso.presentation.model.bookshelf;

import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class handles the bookshelf stored in the session store
 * 
 * @author Florian Alpers
 *
 */
public class SessionStoreBookshelfManager {
   
    private static final Logger logger = LoggerFactory.getLogger(SessionStoreBookshelfManager.class);
    
    public static final String BOOKSHELF_ATTRIBUTE_NAME = "bookshelf";
    
    /**
     * 
     * @return An optional containing the stored bookshelf if one exists
     * 
     * @throws NullPointerException     if the session is NULL
     */
    public Optional<Bookshelf> getBookshelf(HttpSession session) {
        
        try {            
            Object object = session.getAttribute(BOOKSHELF_ATTRIBUTE_NAME);
            if(object != null) {                
                return Optional.of((Bookshelf)object);
            } else {
                return Optional.empty();
            }
        } catch(ClassCastException e) {
            logger.error("Attribute stored in session under " + BOOKSHELF_ATTRIBUTE_NAME + " is not of type Bookshelf");
            return Optional.empty();
        }
    }
    
    /**
     * Create a new Bookshelf and store it in the session store in the attribute "bookshelf"
     * 
     * @param session
     * @return
     * 
     * @throws IllegalArgumentException   if a bookshelf already exists
     * @throws IllegalStateException      if the bookshelf could not be stored in the session
     * @throws NullPointerException     if the session is NULL
     */
    public Bookshelf createBookshelf(HttpSession session) {
        
        if(session.getAttribute(BOOKSHELF_ATTRIBUTE_NAME) == null) {            
            Bookshelf bookshelf = new Bookshelf();
            session.setAttribute(BOOKSHELF_ATTRIBUTE_NAME, bookshelf);
            return getBookshelf(session).orElseThrow(() -> new IllegalStateException("Attribute stored but not available"));
        } else {
            throw new IllegalArgumentException("Bookshelf already exists. Cannot create a new one");
        }
    }
    
    /**
     * Gets the bookshelf stored in the session. If no bookshelf exists, a new one is created, stored and returned
     * 
     * @param session
     * @return
     * @throws NullPointerException     if the session is NULL
     */
    public Bookshelf getOrCreateBookshelf(HttpSession session) {
        return getBookshelf(session).orElseGet(() -> createBookshelf(session));
    }
    
    /**
     * Adds the given item to the session bookshelf, creating a new bookshelf if required
     * 
     * @param item
     * @param session
     * @return  false if the item could not be added (usually because it already exists), true otherwise
     * @throws NullPointerException     if the session is NULL
     */
    public boolean addToBookshelf(BookshelfItem item, HttpSession session) {
        return getOrCreateBookshelf(session).addItem(item);
    }
    
    /**
     * Remove the given item (or one with identical pi, logId and order) from the session bookshelf if it exists
     * If no session bookshelf exists, it doesn't contain the item or the item could not be removed for some other reason, false is returned
     * 
     * @param item
     * @param session
     * @return
     * @throws NullPointerException     if the session is NULL
     */
    public boolean removeFromBookself(BookshelfItem item, HttpSession session) {
        Optional<Bookshelf> o = getBookshelf(session);
        if(o.isPresent()) {
            return o.get().removeItem(item);
        } else {
            return false;
        }
    }
    
    /**
     * 
     * 
     * @param session
     * 
     * @throws NullPointerException     if the session is NULL
     */
    public void deleteBookshelf(HttpSession session) {
        session.removeAttribute(BOOKSHELF_ATTRIBUTE_NAME);
    }
    
    public boolean isInBookshelf(BookshelfItem item, HttpSession session) {
        Optional<Bookshelf> o = getBookshelf(session);
        if(o.isPresent()) {
            return o.get().getItems().contains(item);
        } else {
            return false;
        }
    }

}
