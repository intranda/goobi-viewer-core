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
package io.goobi.viewer.model.bookmark;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;

/**
 *
 * This class handles the bookmark list stored in the session store
 *
 * @author Florian Alpers
 */
public class SessionStoreBookmarkManager {

    private static final Logger logger = LogManager.getLogger(SessionStoreBookmarkManager.class);

    /** Constant <code>BOOKMARK_LIST_ATTRIBUTE_NAME="bookmarkList"</code> */
    public static final String BOOKMARK_LIST_ATTRIBUTE_NAME = "bookmarkList";

    /**
     * <p>
     * getBookmarkList.
     * </p>
     *
     * @return An optional containing the stored bookmark list if one exists
     * @throws java.lang.NullPointerException if the session is NULL
     * @param session a {@link jakarta.servlet.http.HttpSession} object.
     */
    public Optional<BookmarkList> getBookmarkList(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        try {
            Object object = session.getAttribute(BOOKMARK_LIST_ATTRIBUTE_NAME);
            if (object != null) {
                return Optional.of((BookmarkList) object);
            }
            return Optional.empty();
        } catch (ClassCastException e) {
            logger.error("Attribute stored in session under " + BOOKMARK_LIST_ATTRIBUTE_NAME + " is not of type BookmarkList");
            return Optional.empty();
        }
    }

    /**
     * Create a new BookmarkList and store it in the session store in the attribute "bookmarkList"
     *
     * @param session a {@link jakarta.servlet.http.HttpSession} object.
     * @throws java.lang.IllegalArgumentException if a bookmark list already exists
     * @throws java.lang.IllegalStateException if the bookmark list could not be stored in the session
     * @throws java.lang.NullPointerException if the session is NULL
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     */
    public BookmarkList createBookmarkList(HttpSession session) {

        if (session.getAttribute(BOOKMARK_LIST_ATTRIBUTE_NAME) == null) {
            BookmarkList bookmarkList = new BookmarkList();
            bookmarkList.setName("session");
            session.setAttribute(BOOKMARK_LIST_ATTRIBUTE_NAME, bookmarkList);
            return getBookmarkList(session).orElseThrow(() -> new IllegalStateException("Attribute stored but not available"));
        }
        return getBookmarkList(session).orElseThrow(() -> new IllegalArgumentException("Bookmark list can neither be retrieved nor created"));
    }

    /**
     * Gets the bookmark list stored in the session. If no bookmark list exists, a new one is created, stored and returned
     *
     * @param session a {@link jakarta.servlet.http.HttpSession} object.
     * @throws java.lang.NullPointerException if the session is NULL
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     */
    public synchronized BookmarkList getOrCreateBookmarkList(HttpSession session) {
        return getBookmarkList(session).orElseGet(() -> createBookmarkList(session));
    }

    /**
     * Adds the given item to the session bookmark list, creating a new bookmark list if required
     *
     * @param item a {@link io.goobi.viewer.model.bookmark.Bookmark} object.
     * @param session a {@link jakarta.servlet.http.HttpSession} object.
     * @return false if the item could not be added (usually because it already exists), true otherwise
     * @throws java.lang.NullPointerException if the session is NULL
     */
    public boolean addToBookmarkList(Bookmark item, HttpSession session) {
        return getOrCreateBookmarkList(session).addItem(item);
    }

    /**
     * Remove the given item (or one with identical pi, logId and order) from the session bookmark list if it exists If no session bookmark list
     * exists, it doesn't contain the item or the item could not be removed for some other reason, false is returned
     *
     * @param item a {@link io.goobi.viewer.model.bookmark.Bookmark} object.
     * @param session a {@link jakarta.servlet.http.HttpSession} object.
     * @throws java.lang.NullPointerException if the session is NULL
     * @return a boolean.
     */
    public boolean removeFromBookself(Bookmark item, HttpSession session) {
        Optional<BookmarkList> o = getBookmarkList(session);
        if (o.isPresent()) {
            return o.get().removeItem(item);
        }
        return false;
    }

    /**
     * <p>
     * deleteBookmarkList.
     * </p>
     *
     * @param session a {@link jakarta.servlet.http.HttpSession} object.
     * @throws java.lang.NullPointerException if the session is NULL
     */
    public void deleteBookmarkList(HttpSession session) {
        session.removeAttribute(BOOKMARK_LIST_ATTRIBUTE_NAME);
    }

    /**
     * <p>
     * isInBookmarkList.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.bookmark.Bookmark} object.
     * @param session a {@link jakarta.servlet.http.HttpSession} object.
     * @return a boolean.
     */
    public boolean isInBookmarkList(Bookmark item, HttpSession session) {
        Optional<BookmarkList> o = getBookmarkList(session);
        if (o.isPresent()) {
            return o.get().getItems().contains(item);
        }
        return false;
    }

    /**
     * Assigns the current session bookmark list (if any) to the given user and saves the bookmark list to the database The bookmark list gets a newly
     * generated name provided by {@link #generateNewBookmarkListName(List)}
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addSessionBookmarkListToUser(User user, HttpServletRequest request) throws DAOException {
        if (request == null) {
            return;
        }
        Optional<BookmarkList> oBookmarkList = getBookmarkList(request.getSession());
        if (oBookmarkList.isPresent() && !oBookmarkList.get().getItems().isEmpty()) {
            try {
                oBookmarkList.get().setOwner(user);
                oBookmarkList.get().setIsPublic(false);
                List<BookmarkList> userBookmarkLists = DataManager.getInstance().getDao().getBookmarkLists(user);
                oBookmarkList.get().setName(generateNewBookmarkListName(userBookmarkLists));
                DataManager.getInstance().getDao().addBookmarkList(oBookmarkList.get());
            } catch (PersistenceException | DAOException e) {
                logger.error("Error saving session bookmark list", e);
            }

        }
    }

    /**
     * Returns a String of the pattern "List {n}" where {n} is the lowest positive integer such that no bookmark list named "List {n}" exists in the
     * given list
     *
     * @param bookmarkLists a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    public static String generateNewBookmarkListName(List<BookmarkList> bookmarkLists) {
        String nameTemplate = "List {num}";
        String namePlaceholder = "{num}";
        String nameRegex = "List \\d+";
        String nameBase = "List ";

        if (bookmarkLists == null || bookmarkLists.isEmpty()) {
            return nameTemplate.replace(namePlaceholder, "1");
        }

        Integer counter = bookmarkLists.stream()
                .map(bs -> bs.getName())
                .filter(name -> name != null && name.matches(nameRegex))
                .map(name -> name.replace(nameBase, ""))
                .map(num -> Integer.parseInt(num))
                .sorted((n1, n2) -> Integer.compare(n2, n1))
                .findFirst()
                .orElse(0);

        counter++;

        return nameTemplate.replace(namePlaceholder, counter.toString());
    }

    /**
     * This embedds the bookmark list items within a text, to be used in autogenerated mails describing the bookmark list The parameter {@code text}
     * contains the complete text with a placeholder {0} which is replaced by the text generated from the bookmarks. {@code itemText} is the text for
     * each bookmark with placeholder {0} which is replaced by the link to the bookmarked item, and {1} which is replaced by the title of that item.
     * The parameter {@code bookmarkList} is the bookmark list containing the items to be inserted
     *
     * @param text a {@link java.lang.String} object.
     * @param itemText a {@link java.lang.String} object.
     * @param emptyListText a {@link java.lang.String} object.
     * @param bookmarkList a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @return a {@link java.lang.String} object.
     */
    public static String generateBookmarkListInfo(String text, String itemText, String emptyListText, BookmarkList bookmarkList) {
        StringBuilder itemList = new StringBuilder();
        for (Bookmark item : bookmarkList.getItems()) {
            try {
                String path = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
                String url = path + item.getUrl();
                String currentItemText = itemText.replace("{0}", url).replace("{1}", item.getName());
                itemList.append(currentItemText);
            } catch (PresentationException e) {
                logger.error(e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.error(e.getMessage(), e);
            }

        }
        if (itemList.toString().isEmpty()) {
            itemList.append(emptyListText);
        }
        return text.replace("{0}", itemList.toString());

    }

}
