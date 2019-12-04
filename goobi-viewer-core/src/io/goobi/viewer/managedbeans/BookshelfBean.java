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
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
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
import io.goobi.viewer.model.bookshelf.Bookshelf;
import io.goobi.viewer.model.bookshelf.BookshelfItem;
import io.goobi.viewer.model.bookshelf.SessionStoreBookshelfManager;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.viewer.ViewManager;

@Named
@SessionScoped
public class BookshelfBean implements Serializable {

    private static final long serialVersionUID = -2656584301309913161L;

    private static final Logger logger = LoggerFactory.getLogger(BookshelfBean.class);

    @Inject
    private UserBean userBean;
    @Inject
    private NavigationHelper navigationHelper;

    /** Currently selected bookshelf. */
    private Bookshelf currentBookshelf = null;
    private BookshelfItem currentBookshelfItem;
    private UserGroup currentUserGroup;

    /**
     * An email-address which a user may enter to receive the session store bookshelf as mail
     */
    private String sessionBookshelfEmail = "";
    private static String KEY_BOOKSHELF_EMAIL_SUBJECT = "bookshelf_session_mail_header";
    private static String KEY_BOOKSHELF_EMAIL_BODY = "bookshelf_session_mail_body";
    private static String KEY_BOOKSHELF_EMAIL_ITEM = "bookshelf_session_mail_list";
    private static String KEY_BOOKSHELF_EMAIL_EMPTY_LIST = "bookshelf_session_mail_emptylist";
    private static String KEY_BOOKSHELF_EMAIL_ERROR = "bookshelf_session_mail_error";
    private static String KEY_BOOKSHELF_EMAIL_SUCCESS = "bookshelf_session_mail_success";

    /** Empty Constructor. */
    public BookshelfBean() {
        // the emptiness inside
    }

    @PostConstruct
    public void init() {
        resetCurrentBookshelfAction();
    }

    public void changeCurrentOwnBookshelf(ActionEvent e) {
        // logger.info(this.currentOwnBookshelf.getName());
    }

    /**
     * Resets the current bookshelf and returns to the overview of own bookshelves.
     *
     * @return
     */
    public String cancelEditCurrentBookshelfAction() {
        resetCurrentBookshelfAction();
        return "pretty:userMenuMyBookshelves";
    }

    /**
     * Updates the currently selected Bookshelf if it already in the user's bookshelf list, adds it to the list otherwise. Saves DataManager in both
     * cases.
     *
     * @throws DAOException
     */
    public String saveCurrentBookshelfAction() {
        if (saveBookshelfAction(currentBookshelf)) {
            resetCurrentBookshelfAction();
            return "pretty:userMenuMyBookshelves";
        }

        return "pretty:userMenuSingleBookshelf";
    }

    /**
     * Updates the given Bookshelf if it already in the user's bookshelf list, adds it to the list otherwise. Saves DataManager in both cases.
     * 
     * @param bookshelf
     * @return
     */
    static boolean saveBookshelfAction(Bookshelf bookshelf) {
        UserBean userBean = BeanUtils.getUserBean();
        if (bookshelf == null || userBean == null || userBean.getUser() == null || StringUtils.isEmpty(bookshelf.getName())) {
            return false;
        }

        logger.debug("saveBookshelfAction: {}, ID: {}", bookshelf.getName(), bookshelf.getId());

        if (bookshelf.getId() == null) {
            // New bookshelf
            if (bookshelf.getOwner() == null) {
                logger.trace("Owner not yet set");
                bookshelf.setOwner(userBean.getUser());
            }

            try {
                if (DataManager.getInstance().getDao().addBookshelf(bookshelf)) {
                    String msg = Helper.getTranslation("bookshelf_createBookshelfSuccess", null);
                    Messages.info(msg.replace("{0}", bookshelf.getName()));
                    logger.debug("Bookshelf '{}' for user {} added.", bookshelf.getName(), userBean.getUser().getId());
                    return true;
                }
            } catch (DAOException e) {
                logger.error("Could not save bookshelf: {}", e.getMessage());
            }
            String msg = Helper.getTranslation("bookshelf_createBookshelfFailure", null);
            Messages.error(msg.replace("{0}", bookshelf.getName()));
        } else {
            // Update bookshelf in the DB
            try {
                if (DataManager.getInstance().getDao().updateBookshelf(bookshelf)) {
                    logger.debug("Bookshelf '{}' for user {} updated.", bookshelf.getName(), userBean.getUser().getId());
                    String msg = Helper.getTranslation("bookshelf_updateBookshelfSuccess", null);
                    Messages.info(msg.replace("{0}", bookshelf.getName()));
                    return true;
                }
            } catch (DAOException e) {
                logger.error("Could not update bookshelf: {}", e.getMessage());
            }
            String msg = Helper.getTranslation("bookshelf_updateBookshelfFailure", null);
            Messages.error(msg.replace("{0}", bookshelf.getName()));
        }

        return false;
    }

    /**
     * Deletes currentBookshelf.
     *
     * @throws DAOException
     */
    public String deleteCurrentBookshelfAction() {
        logger.debug("deleteCurrentBookshelfAction: {}", currentBookshelf.getId());
        try {
            UserBean userBean = BeanUtils.getUserBean();
            if (userBean != null && userBean.getUser() != null && DataManager.getInstance().getDao().deleteBookshelf(currentBookshelf)) {
                logger.debug("Bookshelf '" + currentBookshelf.getName() + "' deleted.");
                String msg = Helper.getTranslation("bookshelf_deleteSuccess", null);
                Messages.info(msg.replace("{0}", currentBookshelf.getName()));
                resetCurrentBookshelfAction();
                return "pretty:userMenuMyBookshelves";
            }
        } catch (DAOException e) {
            logger.error("Could not delete bookshelf: {}", e.getMessage());
        }
        String msg = Helper.getTranslation("bookshelf_deleteFailure", null);
        Messages.error(msg.replace("{0}", currentBookshelf.getName()));

        return "pretty:userMenuSingleBookshelf";
    }

    /**
     * Shares currentBookshelf with currentUserGroup.
     */
    public void shareCurrentBookshelfAction() {
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getUser() != null && currentBookshelf.addGroupShare(currentUserGroup)) {
            Messages.info("bookshelf_shareWin");
            logger.debug("Bookshelf '" + currentBookshelf.getName() + "' shared with user group '" + currentUserGroup.getName() + "'.");
            return;
        }
        Messages.error("bookshelf_shareFail");
    }

    /**
     * Removes currentUserGroup from the shares list of currentBookshelf.
     */
    public void unshareCurrentBookshelfAction() {
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getUser() != null && currentBookshelf.removeGroupShare(currentUserGroup)) {
            Messages.info("bookshelf_unshareWin");
            logger.debug("Bookshelf '" + currentBookshelf.getName() + "' unshared with user group '" + currentUserGroup.getName() + "'.");
            return;
        }
        Messages.error("bookshelf_unshareFail");
    }

    /**
     * Sets currentBookshelf to a new object.
     */
    public void resetCurrentBookshelfAction() {
        logger.trace("resetCurrentBookshelfAction");
        currentBookshelf = new Bookshelf();
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null) {
            currentBookshelf.setOwner(userBean.getUser());
        }
        currentBookshelfItem = new BookshelfItem();
    }

    public void prepareItemForBookshelf() throws IndexUnreachableException {
        logger.trace("prepareItemForBookshelf");
        ActiveDocumentBean activeDocumentBean = BeanUtils.getActiveDocumentBean();
        if (activeDocumentBean != null) {
            ViewManager viewManager = activeDocumentBean.getViewManager();
            this.currentBookshelfItem = new BookshelfItem(viewManager.getPi(), viewManager.getVolumeTitle(), viewManager.getVolumeTitle());
        }
    }

    /**
     * Updates the currently selected BookshelfItem if it is already part of the current Bookshelf, otherwise adds a new BookshelfItem. Saves
     * DataManager in both cases.
     *
     * @return
     * @throws DAOException
     */
    public String saveCurrentItemAction() {
        logger.trace("name: {}", currentBookshelfItem.getName());
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getUser() != null && currentBookshelf != null && StringUtils.isNotEmpty(currentBookshelfItem.getName())) {
            logger.trace("saving item to bookshelf");
            try {
                if (currentBookshelf.getItems().contains(currentBookshelfItem)) {
                    // TODO Do not throw error if item already in bookshelf. Instead, offer to edit or remove.
                    DataManager.getInstance().getDao().updateBookshelf(currentBookshelf);
                    String msg = Helper.getTranslation("bookshelf_addToBookshelfFailureAlreadyContains", null);
                    Messages.error(msg.replace("{0}", currentBookshelf.getName()));
                    //                String msg = Helper.getTranslation("bookshelf_addToBookshelfSuccess", null);
                    //                Messages.info(msg.replace("{0}", currentBookshelf.getName()));
                    //                logger.debug("Bookshelf item '" + currentBookshelfItem.getName() + "' added.");
                    return "";
                } else if (currentBookshelf.addItem(currentBookshelfItem) && DataManager.getInstance().getDao().updateBookshelf(currentBookshelf)) {
                    String msg = Helper.getTranslation("bookshelf_addToBookshelfSuccess", null);
                    Messages.info(msg.replace("{0}", currentBookshelf.getName()));
                    logger.debug("Bookshelf item '{}' added, ID: {}", currentBookshelfItem.getName(), currentBookshelfItem.getId());
                    return "";
                }
            } catch (DAOException e) {
                logger.error("Could not save bookshelf item: {}", e.getMessage());
            }
        }

        String msg = Helper.getTranslation("bookshelf_addToBookshelfFailure", null);
        Messages.error(msg.replace("{0}", currentBookshelf.getName()));
        return "";
    }

    /**
     * Removes the currently selected BookshelfItem from the currently selected Bookshelf.
     *
     * @throws DAOException
     */
    public void deleteCurrentItemAction(BookshelfItem bookshelfItem) {
        UserBean userBean = BeanUtils.getUserBean();
        if (bookshelfItem != null && userBean != null && userBean.getUser() != null && currentBookshelf != null) {
            try {
                if (currentBookshelf.removeItem(bookshelfItem) && DataManager.getInstance().getDao().updateBookshelf(currentBookshelf)) {
                    String msg = Helper.getTranslation("bookshelf_removeBookshelfItemSuccess", null);
                    Messages.info(msg.replace("{0}", bookshelfItem.getPi()));
                    logger.debug("Bookshelf item '" + bookshelfItem.getName() + "' deleted.");
                    return;
                }
            } catch (DAOException e) {
                logger.error("Could not delete bookshelf item: {}", e.getMessage());
            }
            String msg = Helper.getTranslation("bookshelf_removeBookshelfItemFailure", null);
            Messages.error(msg.replace("{0}", bookshelfItem.getPi()));
        } else if (bookshelfItem == null) {
            logger.error("BookshelfItem to delete is not defined");
        }
    }

    /**
     * Returns the names all existing user groups (minus the ones currentBookshelf is already shared with). TODO Filter some user groups, if required
     * (e.g. admins)
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
        allGroups.removeAll(currentBookshelf.getGroupShares());
        for (UserGroup ug : allGroups) {
            ret.add(ug.getName());
        }

        return ret;
    }

    /**
     * Returns a list of all bookshelves shared with this user by other users through any group of which he is a member or the owner.
     *
     * @return
     * @throws DAOException
     */
    public List<Bookshelf> getBookshelvesSharedByOthers() throws DAOException {
        List<Bookshelf> ret = new ArrayList<>();

        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null && userBean.getUser() != null) {
            // Own user groups
            for (UserGroup userGroup : userBean.getUser().getUserGroupOwnerships()) {
                for (Bookshelf bs : userGroup.getSharedBookshelves()) {
                    if (!ret.contains(bs) && !bs.getOwner().equals(userBean.getUser())) {
                        ret.add(bs);
                    }
                }
            }

            // Group memberships
            for (UserGroup userGroup : userBean.getUser().getUserGroupsWithMembership()) {
                for (Bookshelf bs : userGroup.getSharedBookshelves()) {
                    if (!ret.contains(bs) && !bs.getOwner().equals(userBean.getUser())) {
                        ret.add(bs);
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Returns a list of all existing bookshelves that are marked public.
     *
     * @return
     * @throws DAOException
     */
    public List<Bookshelf> getPublicBookshelves() throws DAOException {
        return DataManager.getInstance().getDao().getPublicBookshelves();
    }

    /**
     * 
     * @param user
     * @return
     * @throws DAOException
     */
    public List<Bookshelf> getBookshelvesSharedWithUsers(User user) throws DAOException {
        if (user == null) {
            return Collections.emptyList();
        }
        List<UserGroup> userGroups = user.getAllUserGroups();
        if (userGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<Bookshelf> allBookshelves = DataManager.getInstance().getDao().getAllBookshelves();
        if (allBookshelves.isEmpty()) {
            return Collections.emptyList();
        }

        List<Bookshelf> ret = new ArrayList<>();
        for (Bookshelf bs : allBookshelves) {
            if (bs.getOwner().equals(user)) {
                continue;
            }
            // TODO
        }

        return ret;
    }

    /**
     * Returns a list of all existing bookshelves owned by current user
     *
     * @return
     * @throws DAOException
     */

    public List<Bookshelf> getBookshelves() throws DAOException {
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean != null) {
            return getBookshelvesForUser(userBean.getUser());
        }
        return Collections.emptyList();
    }

    public List<Bookshelf> getBookshelvesForUser(User user) throws DAOException {
        return DataManager.getInstance().getDao().getBookshelves(user);
    }

    public int getNumBookshelvesForUser(User user) throws DAOException {
        List<Bookshelf> bookshelves = getBookshelvesForUser(user);
        if (bookshelves != null) {
            return bookshelves.size();
        }

        return 0;
    }

    public void bookshelfSelectAction(ValueChangeEvent event) throws DAOException {
        logger.debug("bookshelf selected: {}", event.getNewValue());
        currentBookshelf = DataManager.getInstance().getDao().getBookshelf(String.valueOf(event.getNewValue()), userBean.getUser());
    }

    public boolean isNewBookshelf() {
        return currentBookshelf.getId() == null;
    }

    public String createNewBookshelfAction() {
        resetCurrentBookshelfAction();
        return "pretty:userMenuEditBookshelf";
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
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, Helper.getTranslation("bookshelfNameFailure", null), null);
            throw new ValidatorException(message);
        }

        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        // Do not allow duplicate names
        if (isNewBookshelf()) {
            for (Bookshelf bookshelf : getBookshelvesForUser(userBean.getUser())) {
                if (bookshelf.getName().equals(name) && bookshelf.getOwner().equals(userBean.getUser())) {
                    ((UIInput) toValidate).setValid(false);
                    logger.debug("Bookshelf '" + currentBookshelf.getName() + "' for user '" + userBean.getEmail()
                            + "' could not be added. A bookshelf with this name for this use may already exist.");
                    String msg = Helper.getTranslation("bookshelf_createBookshelfNameExists", null);
                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, Helper.getTranslation(msg.replace("{0}", name), null), null);
                    throw new ValidatorException(message);
                }
            }
        }
    }

    public boolean isCurrentBookshelfMine() {
        UserBean userBean = BeanUtils.getUserBean();
        return currentBookshelf != null && (isNewBookshelf() || currentBookshelf.getOwner().equals(userBean.getUser()));
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * @return the currentBookshelf
     */
    public Bookshelf getCurrentBookshelf() {
        return currentBookshelf;
    }

    public List<String> getCurrentBookshelfNames() throws DAOException {
        UserBean userBean = BeanUtils.getUserBean();
        List<Bookshelf> bookshelflist = getBookshelvesForUser(userBean.getUser());
        if (bookshelflist == null || bookshelflist.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> nameList = new ArrayList<>(bookshelflist.size());
        for (Bookshelf bookshelf : bookshelflist) {
            nameList.add(bookshelf.getName());
        }
        return nameList;
    }

    /**
     * @param currentBookshelf the currentBookshelf to set
     */
    public void setCurrentBookshelf(Bookshelf currentOwnBookshelf) {
        // logger.debug("currentOwnBookshelf set to "+currentOwnBookshelf.getName());
        this.currentBookshelf = currentOwnBookshelf;
    }

    /**
     * @param currentBookshelf the currentBookshelf to set
     * @throws DAOException
     */
    public void setCurrentBookshelfId(String bookshelfId) throws PresentationException, DAOException {
        if (bookshelfId != null) {
            try {
                Long id = Long.parseLong(bookshelfId);
                Optional<Bookshelf> o = getBookshelves().stream().filter(bookshelf -> id.equals(bookshelf.getId())).findFirst();
                if (o.isPresent()) {
                    setCurrentBookshelf(o.get());
                } else {
                    throw new PresentationException("No bookshelf found with id " + bookshelfId + " of current user");
                }
            } catch (NumberFormatException e) {
                throw new PresentationException(bookshelfId + " is not viable bookshelf id");
            }
        }
    }

    public String viewBookshelfAction(Bookshelf bookshelf) {
        if (bookshelf != null) {
            logger.debug("bookshelf to open: {}, belongs to: {}", bookshelf.getId(), bookshelf.getOwner().getId());
            currentBookshelf = bookshelf;
        }

        return "pretty:userMenuViewBookshelf";
    }

    public String editBookshelfAction(Bookshelf bookshelf) {
        if (bookshelf != null) {
            logger.debug("bookshelf to edit: {}, belongs to: {}", bookshelf.getId(), bookshelf.getOwner().getId());
            currentBookshelf = bookshelf;
        }

        return "pretty:userMenuEditBookshelf";
    }

    /**
     * @return the currentBookshelfItem
     */
    public BookshelfItem getCurrentBookshelfItem() {
        return currentBookshelfItem;
    }

    /**
     * @param currentBookshelfItem the currentBookshelfItem to set
     */
    public void setCurrentBookshelfItem(BookshelfItem currentBookshelfItem) {
        this.currentBookshelfItem = currentBookshelfItem;
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
     * @param sessionBookshelfEmail the sessionBookshelfEmail to set
     */
    public void setSessionBookshelfEmail(String sessionBookshelfEmail) {
        this.sessionBookshelfEmail = sessionBookshelfEmail;
    }

    /**
     * @return the sessionBookshelfEmail
     */
    public String getSessionBookshelfEmail() {
        return sessionBookshelfEmail;
    }

    /**
     * 
     * @param bookshelf
     * @return Absolute share URLto the given bookshelf
     */
    public String getShareLink(Bookshelf bookshelf) {
        if (bookshelf == null) {
            return "";
        }
        logger.trace("getShareLink: {}", bookshelf.getId());

        if (bookshelf.getShareKey() == null) {
            bookshelf.generateShareKey();
            saveBookshelfAction(bookshelf);
        }

        return navigationHelper.getApplicationUrl() + "bookshelf/key/" + bookshelf.getShareKey() + "/";
    }

    public String getShareKey() {
        if (currentBookshelf != null) {
            return currentBookshelf.getShareKey();
        }

        return "-";
    }

    public void setShareKey(String key) throws DAOException {
        if (key == null) {
            return;
        }

        currentBookshelf = DataManager.getInstance().getDao().getBookshelfByShareKey(key);
    }

    public void sendSessionBookshelfAsMail() {
        if (StringUtils.isNotBlank(getSessionBookshelfEmail())) {
            DataManager.getInstance().getBookshelfManager().getBookshelf(BeanUtils.getRequest().getSession(false)).ifPresent(bookshelf -> {
                String body = SessionStoreBookshelfManager.generateBookshelfInfo(Helper.getTranslation(KEY_BOOKSHELF_EMAIL_BODY, null),
                        Helper.getTranslation(KEY_BOOKSHELF_EMAIL_ITEM, null), Helper.getTranslation(KEY_BOOKSHELF_EMAIL_EMPTY_LIST, null),
                        bookshelf);
                String subject = Helper.getTranslation(KEY_BOOKSHELF_EMAIL_SUBJECT, null);
                try {
                    Helper.postMail(Collections.singletonList(getSessionBookshelfEmail()), subject, body);
                    Messages.info(Helper.getTranslation(KEY_BOOKSHELF_EMAIL_SUCCESS, null));
                } catch (UnsupportedEncodingException | MessagingException e) {
                    logger.error(e.getMessage(), e);
                    Messages.error(Helper.getTranslation(KEY_BOOKSHELF_EMAIL_ERROR, null)
                            .replace("{0}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
                }
            });
        }
    }

    /**
     * 
     * @return Size of items in the session bookshelf
     */
    public int countSessionBookshelfItems() {
        return DataManager.getInstance()
                .getBookshelfManager()
                .getBookshelf(BeanUtils.getRequest().getSession(false))
                .map(bookshelf -> bookshelf.getItems().size())
                .orElse(0);
    }
}