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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.bookmark.BookmarkTools;
import io.goobi.viewer.model.bookmark.SessionStoreBookmarkManager;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.viewer.ViewManager;

@Named
@SessionScoped
public class BookmarkBean implements Serializable {

    private static final long serialVersionUID = -2656584301309913161L;

    private static final Logger logger = LoggerFactory.getLogger(BookmarkBean.class);

    @Inject
    private UserBean userBean;
    @Inject
    private NavigationHelper navigationHelper;

    /** Currently selected bookmark list. */
    private BookmarkList currentBookmarkList = null;
    /** Flag indicating that currentBookmarkList was opened via a share link and is publicly visible. */
    private String currentBookmarkListSharedKey = null;

    private Bookmark currentBookmark;

    private UserGroup currentUserGroup;

    /**
     * An email-address which a user may enter to receive the session store bookmark list as mail
     */
    private String sessionBookmarkListEmail = "";
    private static String KEY_BOOKMARK_LIST_EMAIL_SUBJECT = "bookmarkList_session_mail_header";
    private static String KEY_BOOKMARK_LIST_EMAIL_BODY = "bookmarkList_session_mail_body";
    private static String KEY_BOOKMARK_LIST_EMAIL_ITEM = "bookmarkList_session_mail_list";
    private static String KEY_BOOKMARK_LIST_EMAIL_EMPTY_LIST = "bookmarkList_session_mail_emptylist";
    private static String KEY_BOOKMARK_LIST_EMAIL_ERROR = "bookmarkList_session_mail_error";
    private static String KEY_BOOKMARK_LIST_EMAIL_SUCCESS = "bookmarkList_session_mail_success";

    /** Empty Constructor. */
    public BookmarkBean() {
        // the emptiness inside
    }

    @PostConstruct
    public void init() {
        resetCurrentBookmarkListAction();
    }

    /**
     * Resets the current bookmark list and returns to the overview of own bookmark lists.
     *
     * @return
     */
    public String cancelEditCurrentBookmarkListAction() {
        resetCurrentBookmarkListAction();
        return "pretty:userMenuBookmarkLists";
    }

    /**
     * Updates the currently selected bookmark list if it already in the user's list of bookmark lists, adds it to the list otherwise. Saves
     * DataManager in both cases.
     *
     * @throws DAOException
     */
    public String saveCurrentBookmarkListAction() {
        if (saveBookmarkListAction(currentBookmarkList)) {
            resetCurrentBookmarkListAction();
            return "pretty:userMenuBookmarkLists";
        }

        return "pretty:userMenuEditBookmarkList";
    }

    /**
     * Updates the given BookmarkList if it already in the user's list of bookmark lists, adds it to the list otherwise. Saves DataManager in both
     * cases.
     * 
     * @param bookmarkList
     * @return
     */
    static boolean saveBookmarkListAction(BookmarkList bookmarkList) {
        UserBean userBean = BeanUtils.getUserBean();
        if (bookmarkList == null || userBean == null || userBean.getUser() == null || StringUtils.isEmpty(bookmarkList.getName())) {
            return false;
        }

        logger.debug("saveBookmarkListAction: {}, ID: {}", bookmarkList.getName(), bookmarkList.getId());

        if (bookmarkList.getId() == null) {
            // New bookmark list
            if (bookmarkList.getOwner() == null) {
                logger.trace("Owner not yet set");
                bookmarkList.setOwner(userBean.getUser());
            }

            try {
                if (DataManager.getInstance().getDao().addBookmarkList(bookmarkList)) {
                    String msg = Helper.getTranslation("bookmarkList_createBookmarkListSuccess", null);
                    Messages.info(msg.replace("{0}", bookmarkList.getName()));
                    logger.debug("Bookmark list '{}' for user {} added.", bookmarkList.getName(), userBean.getUser().getId());
                    return true;
                }
            } catch (DAOException e) {
                logger.error("Could not save bookmark list: {}", e.getMessage());
            }
            String msg = Helper.getTranslation("bookmarkList_createBookmarkListFailure", null);
            Messages.error(msg.replace("{0}", bookmarkList.getName()));
        } else {
            // Update bookmark list in the DB
            try {
                if (DataManager.getInstance().getDao().updateBookmarkList(bookmarkList)) {
                    logger.debug("Bookmark list '{}' for user {} updated.", bookmarkList.getName(), userBean.getUser().getId());
                    String msg = Helper.getTranslation("bookmarkList_updateBookmarkListSuccess", null);
                    Messages.info(msg.replace("{0}", bookmarkList.getName()));
                    return true;
                }
            } catch (DAOException e) {
                logger.error("Could not update bookmark list: {}", e.getMessage());
            }
            String msg = Helper.getTranslation("bookmarkList_updateBookmarkListFailure", null);
            Messages.error(msg.replace("{0}", bookmarkList.getName()));
        }

        return false;
    }

    /**
     * Deletes currentBookmarkList.
     *
     * @throws DAOException
     */
    public String deleteCurrentBookmarkListAction() {
        logger.debug("deleteCurrentBookmarkListAction: {}", currentBookmarkList.getId());
        try {
            UserBean userBean = BeanUtils.getUserBean();
            if (userBean != null && userBean.getUser() != null && DataManager.getInstance().getDao().deleteBookmarkList(currentBookmarkList)) {
                logger.debug("BookmarkList '" + currentBookmarkList.getName() + "' deleted.");
                String msg = Helper.getTranslation("bookmarkList_deleteSuccess", null);
                Messages.info(msg.replace("{0}", currentBookmarkList.getName()));
                resetCurrentBookmarkListAction();
                return "pretty:userMenuBookmarkLists";
            }
        } catch (DAOException e) {
            logger.error("Could not delete bookmark list: {}", e.getMessage());
        }
        String msg = Helper.getTranslation("bookmarkList_deleteFailure", null);
        Messages.error(msg.replace("{0}", currentBookmarkList.getName()));

        return "pretty:userMenuEditBookmarkList";
    }

    /**
     * Shares currentBookmarkList with currentUserGroup.
     */
    public void shareCurrentBookmarkListAction() {
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getUser() != null && currentBookmarkList.addGroupShare(currentUserGroup)) {
            Messages.info("bookmarkList_shareWin");
            logger.debug("BookmarkList '" + currentBookmarkList.getName() + "' shared with user group '" + currentUserGroup.getName() + "'.");
            return;
        }
        Messages.error("bookmarkList_shareFail");
    }

    /**
     * Removes currentUserGroup from the shares list of currentBookmarkList.
     */
    public void unshareCurrentBookmarkListAction() {
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getUser() != null && currentBookmarkList.removeGroupShare(currentUserGroup)) {
            Messages.info("bookmarkList_unshareWin");
            logger.debug("BookmarkList '" + currentBookmarkList.getName() + "' unshared with user group '" + currentUserGroup.getName() + "'.");
            return;
        }
        Messages.error("bookmarkList_unshareFail");
    }

    /**
     * Sets currentBookmarkList to a new object.
     */
    public void resetCurrentBookmarkListAction() {
        logger.trace("resetCurrentBookmarkListAction");
        currentBookmarkListSharedKey = null;
        currentBookmarkList = new BookmarkList();
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null) {
            currentBookmarkList.setOwner(userBean.getUser());
        }
        currentBookmark = new Bookmark();
    }

    public void prepareItemForBookmarkList() throws IndexUnreachableException {
        logger.trace("prepareItemForBookmarkList");
        ActiveDocumentBean activeDocumentBean = BeanUtils.getActiveDocumentBean();
        if (activeDocumentBean != null) {
            ViewManager viewManager = activeDocumentBean.getViewManager();
            this.currentBookmark = new Bookmark(viewManager.getPi(), viewManager.getVolumeTitle(), viewManager.getVolumeTitle());
        }
    }

    /**
     * Updates the currently selected Bookmark if it is already part of the current BookmarkList, otherwise adds a new Bookmark. Saves DataManager in
     * both cases.
     *
     * @return
     * @throws DAOException
     */
    public String saveCurrentBookmarkAction() {
        logger.trace("name: {}", currentBookmark.getName());
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getUser() != null && currentBookmarkList != null && StringUtils.isNotEmpty(currentBookmark.getName())) {
            logger.trace("saving bookmark to bookmark list");
            try {
                if (currentBookmarkList.getItems().contains(currentBookmark)) {
                    // TODO Do not throw error if item already in bookmark list. Instead, offer to edit or remove.
                    DataManager.getInstance().getDao().updateBookmarkList(currentBookmarkList);
                    String msg = Helper.getTranslation("bookmarkList_addToBookmarkListFailureAlreadyContains", null);
                    Messages.error(msg.replace("{0}", currentBookmarkList.getName()));
                    return "";
                } else if (currentBookmarkList.addItem(currentBookmark)
                        && DataManager.getInstance().getDao().updateBookmarkList(currentBookmarkList)) {
                    String msg = Helper.getTranslation("bookmarkList_addToBookmarkListSuccess", null);
                    Messages.info(msg.replace("{0}", currentBookmarkList.getName()));
                    logger.debug("Bookmark '{}' added, ID: {}", currentBookmark.getName(), currentBookmark.getId());
                    return "";
                }
            } catch (DAOException e) {
                logger.error("Could not save bookmark: {}", e.getMessage());
            }
        }

        String msg = Helper.getTranslation("bookmarkList_addToBookmarkListFailure", null);
        Messages.error(msg.replace("{0}", currentBookmarkList.getName()));
        return "";
    }

    /**
     * Removes the currently selected Bookmark from the currently selected BookmarkList.
     *
     * @throws DAOException
     */
    public void deleteCurrentItemAction(Bookmark bookmark) {
        UserBean userBean = BeanUtils.getUserBean();
        if (bookmark != null && userBean != null && userBean.getUser() != null && currentBookmarkList != null) {
            try {
                if (currentBookmarkList.removeItem(bookmark) && DataManager.getInstance().getDao().updateBookmarkList(currentBookmarkList)) {
                    String msg = Helper.getTranslation("bookmarkList_removeBookmarkSuccess", null);
                    Messages.info(msg.replace("{0}", bookmark.getPi()));
                    logger.debug("Bookmark '" + bookmark.getName() + "' deleted.");
                    return;
                }
            } catch (DAOException e) {
                logger.error("Could not delete bookmark: {}", e.getMessage());
            }
            String msg = Helper.getTranslation("bookmarkList_removeBookmarkFailure", null);
            Messages.error(msg.replace("{0}", bookmark.getPi()));
        } else if (bookmark == null) {
            logger.error("Bookmark to delete is not defined");
        }
    }

    /**
     * Returns the names all existing user groups (minus the ones currentBookmarkList is already shared with). TODO Filter some user groups, if
     * required (e.g. admins)
     *
     * @return
     * @throws DAOException
     * @should not return any used group names
     * @should not modify global user group list
     * @should return empty list if no remaining user group names
     */
    public List<String> getRemainingUserGroupNames() throws DAOException {
        List<String> ret = new ArrayList<>();

        List<UserGroup> allGroups = new ArrayList<>();
        allGroups.addAll(DataManager.getInstance().getDao().getAllUserGroups());
        allGroups.removeAll(currentBookmarkList.getGroupShares());
        for (UserGroup ug : allGroups) {
            ret.add(ug.getName());
        }

        return ret;
    }

    /**
     * Returns a list of all existing bookmark list that are marked public.
     *
     * @return
     * @throws DAOException
     */
    public List<BookmarkList> getPublicBookmarkLists() throws DAOException {
        return DataManager.getInstance().getDao().getPublicBookmarkLists();
    }

    /**
     * 
     * @param user
     * @return
     * @throws DAOException
     */
    public static List<BookmarkList> getBookmarkListsSharedWithUser(User user) throws DAOException {
        return BookmarkTools.getBookmarkListsSharedWithUser(user);
    }

    /**
     * Returns a list of all existing bookmark lists owned by current user
     *
     * @return
     * @throws DAOException
     */

    public List<BookmarkList> getBookmarkLists() throws DAOException {
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null) {
            return getBookmarkListsForUser(userBean.getUser());
        }
        return Collections.emptyList();
    }

    public List<BookmarkList> getBookmarkListsForUser(User user) throws DAOException {
        return DataManager.getInstance().getDao().getBookmarkLists(user);
    }

    public int getNumBookmarkListsForUser(User user) throws DAOException {
        List<BookmarkList> bookmarkLists = getBookmarkListsForUser(user);
        if (bookmarkLists != null) {
            return bookmarkLists.size();
        }

        return 0;
    }

    public void selectBookmarkListAction(ValueChangeEvent event) throws DAOException {
        logger.debug("bookmark list selected: {}", event.getNewValue());
        currentBookmarkList = DataManager.getInstance().getDao().getBookmarkList(String.valueOf(event.getNewValue()), userBean.getUser());
    }

    public boolean isNewBookmarkList() {
        return currentBookmarkList.getId() == null;
    }

    public String createNewBookmarkListAction() {
        resetCurrentBookmarkListAction();
        return "pretty:userMenuEditBookmarkList";
    }

    /**
     * @param event {@link ValueChangeEvent}
     * @throws DAOException
     */
    public void userGroupSelectedAction(ValueChangeEvent event) throws DAOException {
        currentUserGroup = DataManager.getInstance().getDao().getUserGroup(String.valueOf(event.getNewValue()));
    }

    public void validateName(FacesContext context, UIComponent toValidate, Object value) throws ValidatorException, DAOException {
        String name = (String) value;
        name = name.trim();

        if (StringUtils.isEmpty(name)) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, Helper.getTranslation("bookmark listNameFailure", null), null);
            throw new ValidatorException(message);
        }

        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        // Do not allow duplicate names
        if (isNewBookmarkList()) {
            for (BookmarkList bookmarkList : getBookmarkListsForUser(userBean.getUser())) {
                if (bookmarkList.getName().equals(name) && bookmarkList.getOwner().equals(userBean.getUser())) {
                    ((UIInput) toValidate).setValid(false);
                    logger.debug("BookmarkList '" + currentBookmarkList.getName() + "' for user '" + userBean.getEmail()
                            + "' could not be added. A bookmark list with this name for this use may already exist.");
                    String msg = Helper.getTranslation("bookmarkList_createBookmarkListNameExists", null);
                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, Helper.getTranslation(msg.replace("{0}", name), null), null);
                    throw new ValidatorException(message);
                }
            }
        }
    }

    public boolean isCurrentBookmarkListMine() {
        UserBean userBean = BeanUtils.getUserBean();
        return currentBookmarkList != null && (isNewBookmarkList() || currentBookmarkList.getOwner().equals(userBean.getUser()));
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * @return the currentBookmarkList
     */
    public BookmarkList getCurrentBookmarkList() {
        return currentBookmarkList;
    }

    /**
     * @return true if currentBookmarkListSharedKey matches the shared key value of currentBookmarkList; false otherwise;
     */
    public boolean isCurrentBookmarkListShared() {
        return currentBookmarkListSharedKey != null && currentBookmarkList != null
                && currentBookmarkListSharedKey.equals(currentBookmarkList.getShareKey());
    }

    public List<String> getCurrentBookmarkListNames() throws DAOException {
        UserBean userBean = BeanUtils.getUserBean();
        List<BookmarkList> bookmarkLists = getBookmarkListsForUser(userBean.getUser());
        if (bookmarkLists == null || bookmarkLists.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> nameList = new ArrayList<>(bookmarkLists.size());
        for (BookmarkList bookmarkList : bookmarkLists) {
            nameList.add(bookmarkList.getName());
        }
        return nameList;
    }

    /**
     * @param currentBookmarkList the currentBookmarkList to set
     */
    public void setCurrentBookmarkList(BookmarkList currentBookmarkList) {
        this.currentBookmarkList = currentBookmarkList;
    }

    /**
     * 
     * @return Identifier of currentBookmarkList; null if none loaded
     */
    public String getCurrentBookmarkListId() {
        if (getCurrentBookmarkList() != null && getCurrentBookmarkList().getId() != null) {
            return getCurrentBookmarkList().getId().toString();
        }
        return null;
    }

    /**
     * @param bookmarkList
     * @throws DAOException
     */
    public void setCurrentBookmarkListId(String bookmarkListId) throws PresentationException, DAOException {
        if (bookmarkListId == null) {
            return;
        }

        currentBookmarkListSharedKey = null;

        try {
            Long id = Long.parseLong(bookmarkListId);
            BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(id);
            if (bl == null || !bl.isMayView(userBean.getUser())) {
                throw new PresentationException("No bookmark list found with id " + bookmarkListId);
            }
            setCurrentBookmarkList(bl);
        } catch (NumberFormatException e) {
            throw new PresentationException(bookmarkListId + " is not viable bookmark list id");
        }
    }

    public String viewBookmarkListAction(BookmarkList bookmarkList) {
        if (bookmarkList != null) {
            logger.debug("bookmark list to open: {}, belongs to: {}", bookmarkList.getId(), bookmarkList.getOwner().getId());
            currentBookmarkList = bookmarkList;
        }

        return "pretty:userMenuViewBookmarkList1";
    }

    public String editBookmarkListAction(BookmarkList bookmarkList) {
        if (bookmarkList != null) {
            logger.debug("bookmark list to edit: {}, belongs to: {}", bookmarkList.getId(), bookmarkList.getOwner().getId());
            currentBookmarkList = bookmarkList;
        }

        return "pretty:userMenuEditBookmarkList1";
    }

    /**
     * @return the currentBookmark
     */
    public Bookmark getCurrentBookmark() {
        return currentBookmark;
    }

    /**
     * @param currentBookmark the currentBookmark to set
     */
    public void setCurrentBookmark(Bookmark currentBookmark) {
        this.currentBookmark = currentBookmark;
    }

    /**
     * @return the currentUserGroup
     */
    public UserGroup getCurrentUserGroup() {
        return currentUserGroup;
    }

    /**
     * @param currentUserGroup the currentUserGroup to set
     */
    public void setCurrentUserGroup(UserGroup currentUserGroup) {
        this.currentUserGroup = currentUserGroup;
    }

    /**
     * @param sessionBookmarkListEmail the sessionBookmarkListEmail to set
     */
    public void setSessionBookmarkListEmail(String sessionBookmarkListEmail) {
        this.sessionBookmarkListEmail = sessionBookmarkListEmail;
    }

    /**
     * @return the sessionBookmarkListEmail
     */
    public String getSessionBookmarkListEmail() {
        return sessionBookmarkListEmail;
    }

    /**
     * 
     * @param bookmarkList
     * @return Absolute share URLto the given bookmark list
     */
    public String getShareLink(BookmarkList bookmarkList) {
        if (bookmarkList == null) {
            return "";
        }

        if (bookmarkList.getShareKey() == null) {
            bookmarkList.generateShareKey();
            saveBookmarkListAction(bookmarkList);
        }

        return navigationHelper.getApplicationUrl() + "bookmarks/key/" + bookmarkList.getShareKey() + "/";
    }

    public String getShareKey() {
        if (currentBookmarkList != null) {
            return currentBookmarkList.getShareKey();
        }

        return "-";
    }

    /**
     * 
     * @param key
     * @throws DAOException
     */
    public void setShareKey(String key) throws DAOException {
        if (key == null) {
            return;
        }

        currentBookmarkList = DataManager.getInstance().getDao().getBookmarkListByShareKey(key);
        // Set currentBookmarkListSharedKey to enable public sharing of this list
        if (currentBookmarkList != null) {
            currentBookmarkListSharedKey = currentBookmarkList.getShareKey();
        }
    }

    public void sendSessionBookmarkListAsMail() {
        if (StringUtils.isBlank(getSessionBookmarkListEmail())) {
            return;
        }

        DataManager.getInstance().getBookmarkManager().getBookmarkList(BeanUtils.getRequest().getSession(false)).ifPresent(bookmarkList -> {
            String body = SessionStoreBookmarkManager.generateBookmarkListInfo(Helper.getTranslation(KEY_BOOKMARK_LIST_EMAIL_BODY, null),
                    Helper.getTranslation(KEY_BOOKMARK_LIST_EMAIL_ITEM, null), Helper.getTranslation(KEY_BOOKMARK_LIST_EMAIL_EMPTY_LIST, null),
                    bookmarkList);
            String subject = Helper.getTranslation(KEY_BOOKMARK_LIST_EMAIL_SUBJECT, null);
            try {
                Helper.postMail(Collections.singletonList(getSessionBookmarkListEmail()), subject, body);
                Messages.info(Helper.getTranslation(KEY_BOOKMARK_LIST_EMAIL_SUCCESS, null));
            } catch (UnsupportedEncodingException | MessagingException e) {
                logger.error(e.getMessage(), e);
                Messages.error(Helper.getTranslation(KEY_BOOKMARK_LIST_EMAIL_ERROR, null)
                        .replace("{0}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
            }
        });
    }

    /**
     * 
     * @return Size of items in the session bookmark list
     */
    public int countSessionBookmarkListItems() {
        return DataManager.getInstance()
                .getBookmarkManager()
                .getBookmarkList(BeanUtils.getRequest().getSession(false))
                .map(bookmarkList -> bookmarkList.getItems().size())
                .orElse(0);
    }
}