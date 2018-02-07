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

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.servlets.rest.bookshelves.BookshelfResource;

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
        if(session == null) {
            return Optional.empty();
        }
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
            return getBookshelf(session).orElseThrow(() -> new IllegalArgumentException("Bookshelf can neither be retrieved nor created"));
        }
    }
    
    /**
     * Gets the bookshelf stored in the session. If no bookshelf exists, a new one is created, stored and returned
     * 
     * @param session
     * @return
     * @throws NullPointerException     if the session is NULL
     */
    public synchronized Bookshelf getOrCreateBookshelf(HttpSession session) {
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
    
    /**
     * Assigns the curent session bookshelf (if any) to the given user and saves the bookshelf to the database
     * The bookshelf gets a newly generated name provided by {@link #generateNewBookshelfName(List)}
     * 
     * @param user
     * @param request 
     * @throws DAOException
     */
    public void addSessionBookshelfToUser(User user, HttpServletRequest request) throws DAOException {
        if(request != null) {
            Optional<Bookshelf> oBookshelf =getBookshelf(request.getSession());
            if(oBookshelf.isPresent() && !oBookshelf.get().getItems().isEmpty()) {
                oBookshelf.get().setOwner(user);
                oBookshelf.get().setPublic(false);
                List<Bookshelf> userBookshelves = DataManager.getInstance().getDao().getBookshelves(user);
                oBookshelf.get().setName(generateNewBookshelfName(userBookshelves));
                DataManager.getInstance().getDao().addBookshelf(oBookshelf.get());
            }
        }
    }
    
    
    /**
     * Returns a String of the pattern "List {n}" 
     * where {n} is the lowest positive integer such that no bookshelf named "List {n}" exists in the given list
     * 
     * @param allUserBookshelfs
     * @return
     */
    public static String generateNewBookshelfName(List<Bookshelf> bookshelves) {
        
        String bookshelfNameTemplate = "List {num}";
        String bookshelfNamePlaceholder = "{num}";
        String bookshelfNameRegex = "List \\d+";
        String bookshelfNameBase = "List ";

        if(bookshelves == null || bookshelves.isEmpty()) {
            return bookshelfNameTemplate.replace(bookshelfNamePlaceholder, "1");
        }
        
        
        Integer counter = bookshelves.stream()
        .map(bs -> bs.getName())
        .filter(name -> name != null && name.matches(bookshelfNameRegex))
        .map(name -> name.replace(bookshelfNameBase, ""))
        .map(num -> Integer.parseInt(num))
        .sorted((n1, n2) -> Integer.compare(n2, n1))
        .findFirst().orElse(0);
        
        counter++;
        
        return bookshelfNameTemplate.replace(bookshelfNamePlaceholder, counter.toString());
    }
    
     /**
     * This embedds the bookshelf items within a text, to be used in autogenerated mails describing the bookshelf
     * The parameter {@code text} contains the complete text with a placeholder {0} which is replaced by the text 
     * generated from the bookshelf items. {@code itemText} is the text for each bookshelf item with placeholder {0}
     * which is replaced by the link to the bookmarked item, and {1} which is replaced by the title of that item.
     * The parameter {@code bookshelf} is the bookshelf containing the items to be inserted
     * 
     * @param text
     * @param itemText
     * @return
     */
    public static String generateBookshelfInfo(String text, String itemText, String emptyListText, Bookshelf bookshelf) {
        StringBuilder itemList = new StringBuilder();
        for (BookshelfItem item : bookshelf.getItems()) {
            String currentItemText = itemText.replace("{0}", item.getUrl()).replace("{1}", item.getName());
            itemList.append(currentItemText);
        }
        if(itemList.toString().isEmpty()) {
            itemList.append(emptyListText);
        }
        return text.replace("{0}", itemList.toString());
        
    }

}
