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
package io.goobi.viewer.managedbeans;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

import de.unigoettingen.sub.commons.cache.CacheUtils;
import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.job.download.DownloadJobTools;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.authentication.AuthenticationProviderException;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;
import io.goobi.viewer.model.security.user.UserTools;
import io.goobi.viewer.model.translations.admin.MessageEntry;
import io.goobi.viewer.model.translations.admin.TranslationGroup;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;
import io.goobi.viewer.model.translations.admin.TranslationGroupItem;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;

/**
 * Administration backend functions.
 */
@Named
@SessionScoped
public class AdminBean implements Serializable {

    private static final long serialVersionUID = -8334669036711935331L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(AdminBean.class);

    static final int DEFAULT_ROWS_PER_PAGE = 50;

    private static final Object TRANSLATION_LOCK = new Object();

    private static String translationGroupsEditorSession = null;

    @Inject
    private UserBean userBean;

    @Inject
    @Push
    private PushContext hotfolderFileCount;

    private TableDataProvider<User> lazyModelUsers;

    private User currentUser = null;
    private UserGroup currentUserGroup = null;
    private Role currentRole = null;
    /** List of UserRoles to persist or delete */
    private Map<UserRole, String> dirtyUserRoles = new HashMap<>();
    private UserRole currentUserRole = null;
    private IpRange currentIpRange = null;
    private TranslationGroup currentTranslationGroup = null;

    /** Current password for password change */
    private String currentPassword = null;
    /** New password */
    private String passwordOne = "";
    /** New password confirmation */
    private String passwordTwo = "";
    private String emailConfirmation = "";
    private boolean deleteUserContributions = false;

    private Role memberRole;

    private transient Part uploadedAvatarFile;

    private CacheUtils cacheUtils = new CacheUtils(ContentServerCacheManager.getInstance());

    /**
     * <p>
     * Constructor for AdminBean.
     * </p>
     */
    public AdminBean() {
        // the emptiness inside
    }

    /**
     * <p>
     * init.
     * </p>
     */
    @PostConstruct
    public void init() {
        try {
            memberRole = DataManager.getInstance().getDao().getRole("member");
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        lazyModelUsers = new TableDataProvider<>(new TableDataSource<User>() {

            @Override
            public List<User> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder, Map<String, String> filters) {
                logger.trace("getEntries<User>, {}-{}", first, first + pageSize);
                try {
                    String useSortField = sortField;
                    SortOrder useSortOrder = sortOrder;
                    if (StringUtils.isBlank(useSortField)) {
                        useSortField = "id";
                    }
                    return DataManager.getInstance().getDao().getUsers(first, pageSize, useSortField, useSortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                }
                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                try {
                    return DataManager.getInstance().getDao().getUserCount(filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage(), e);
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
                // 
            }
        });
        lazyModelUsers.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelUsers.getFilter("firstName_lastName_nickName_email");
    }
    //
    // User

    /**
     * Returns all users in the DB. Needed for getting a list of users (e.g for adding user group members).
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getAllUsers() throws DAOException {
        List<User> users = DataManager.getInstance().getDao().getAllUsers(true);
        Collections.sort(users);
        return users;
    }

    /**
     * <p>
     * getAllUsersExcept.
     * </p>
     *
     * @param usersToExclude a {@link java.util.Set} object.
     * @should return all users except given
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getAllUsersExcept(Set<User> usersToExclude) throws DAOException {
        List<User> ret = getAllUsers();
        if (usersToExclude != null && !usersToExclude.isEmpty()) {
            ret.removeAll(usersToExclude);
        }

        return ret;
    }

    /**
     * <p>
     * saveCurrentUserAction.
     * </p>
     *
     * @return a {@link java.lang.String} object
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @deprecated Seems to be unused
     */
    @Deprecated(since = "24.12")
    public String saveCurrentUserAction() throws DAOException {
        if (this.saveUser(getCurrentUser(), true)) {
            return "pretty:adminUsers";
        }

        return "";
    }

    /**
     * <p>
     * saveUserAction.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object
     * @param forceCheckCurrentPassword If true, even if an admin is changing their own password
     * @param returnPage a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveUserAction(User user, boolean forceCheckCurrentPassword, String returnPage) throws DAOException {
        if (this.saveUser(user, forceCheckCurrentPassword)) {
            return returnPage;
        }
        return "";
    }

    /**
     * <p>
     * resetUserAction.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object
     * @param returnPage a {@link java.lang.String} object
     * @return a {@link java.lang.String} object
     */
    public String resetUserAction(User user, String returnPage) {
        user.backupFields();
        return returnPage;
    }

    /**
     * <p>
     * Saves the given use. Attention: Used by regular users editing their own profile as well.
     * </p>
     *
     * @param user User to save
     * @param forceCheckCurrentPassword If true, even if an admin is changing their own password
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean saveUser(User user, boolean forceCheckCurrentPassword) throws DAOException {

        //first check if current user has the right to edit the given user
        User activeUser = BeanUtils.getUserBean().getUser();
        if (user == null || activeUser == null || (!activeUser.isSuperuser() && !activeUser.getId().equals(user.getId()))) {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
            return false;
        }

        // Do not allow the same nickname being used for multiple users
        if (user.getNickName() != null) {
            user.setNickName(user.getNickName().trim());
        }
        if (UserTools.isEmailInUse(user.getEmail(), user.getId())) {
            Messages.error("email", ViewerResourceBundle.getTranslation("user_emailTaken", null).replace("{0}", user.getEmail().trim()));
            return false;
        }
        if (UserTools.isNicknameInUse(user.getNickName(), user.getId())) {
            Messages.error("displayName", ViewerResourceBundle.getTranslation("user_nicknameTaken", null).replace("{0}", user.getNickName().trim()));
            return false;
        }
        if (user.getId() != null) {
            // Existing user
            if (StringUtils.isNotEmpty(passwordOne) || StringUtils.isNotEmpty(passwordTwo)) {
                // Check current password entry if user not an admin or forceCheckCurrentPassword==true
                if ((forceCheckCurrentPassword || (!activeUser.isSuperuser() && activeUser.getId().equals(user.getId())))
                        && (currentPassword == null || !new BCrypt().checkpw(currentPassword, user.getPasswordHash()))) {
                    Messages.error("user_currentPasswordWrong");
                    return false;
                }
                if (!passwordOne.equals(passwordTwo)) {
                    Messages.error("user_passwordMismatch");
                    return false;
                }
                user.setNewPassword(passwordOne);
            }
            if (DataManager.getInstance().getDao().updateUser(user)) {
                Messages.info("user_saveSuccess");
            } else {
                Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
                return false;
            }
        } else {
            // New user
            if (DataManager.getInstance().getDao().getUserByEmail(user.getEmail()) != null) {
                // Do not allow the same email address being used for multiple users
                Messages.error("newUserExist");
                logger.debug("User account already exists for '{}'.", user.getEmail());
                return false;
            }
            if (StringUtils.isEmpty(passwordOne) || StringUtils.isEmpty(passwordTwo)) {
                Messages.error("newUserPasswordOneRequired");
                return false;
            } else if (!passwordOne.equals(passwordTwo)) {
                Messages.error("user_passwordMismatch");
                return false;
            } else {
                user.setNewPassword(passwordOne);

            }
            if (DataManager.getInstance().getDao().addUser(user)) {
                Messages.info("newUserCreated");
                currentPassword = null;
                passwordOne = "";
                passwordTwo = "";
            } else {
                Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
                return false;
            }
        }

        //update changes to current user in userBean
        if (activeUser.getId().equals(user.getId())) {
            User newUser = DataManager.getInstance().getDao().getUser(activeUser.getId());
            newUser.backupFields();
            BeanUtils.getUserBean().setUser(newUser);
        }

        return true;
    }

    /**
     * <p>
     * Deletes the given User and optionally their contributions. This method is user for admin-induced deletion of other users as well as
     * self-deletion by a user.
     * </p>
     *
     * @param user User to be deleted
     * @param deleteContributions If true, all content created by this user will also be deleted
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should delete all user public content correctly
     * @should anonymize all user public content correctly
     */
    public String deleteUserAction(User user, boolean deleteContributions) throws DAOException {
        if (user == null) {
            return "";
        }
        if (StringUtils.isBlank(emailConfirmation) || !emailConfirmation.equals(user.getEmail())) {
            Messages.error("admin__error_email_mismatch");
            return "";
        }

        // Prevent deletion if user owns user groups
        if (!user.getUserGroupOwnerships().isEmpty()) {
            Messages.error("admin__error_delete_user_group_ownerships");
            return "";
        }

        logger.debug("Deleting user: {} (delete contributions: {})", user.getDisplayName(), deleteContributions);
        if (deleteContributions) {
            // Delete all public content created by this user
            UserTools.deleteUserPublicContributions(user);
        } else if (!UserTools.anonymizeUserPublicContributions(user)) {
            // Move all public content to an anonymous user
            Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
            return "";
        }

        // Finally, delete user (and any user-created data that's not publicly visible)
        if (UserTools.deleteUser(user)) {
            // If user is deleting themselves, log them out; do not redirect to admin page
            if (userBean != null && user.equals(userBean.getUser())) {
                logger.trace("User self-deletion: {}", user.getId());
                try {
                    userBean.logout();
                } catch (AuthenticationProviderException e) {
                    logger.error(e.getMessage());
                }
                return "pretty:index";
            }

            Messages.info(StringConstants.MSG_ADMIN_DELETED_SUCCESSFULLY);
            return "pretty:adminUsers";
        }

        Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
        return "";
    }

    /**
     * <p>
     * resetCurrentUserAction.
     * </p>
     */
    public void resetCurrentUserAction() {
        currentUser = new User();
        emailConfirmation = "";
        deleteUserContributions = false;
    }

    /**
     * Returns all user groups in the DB. Needed for getting a list of users (e.g for adding user group members).
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getAllUserGroups() throws DAOException {
        logger.trace("getAllUserGroups");
        return DataManager.getInstance().getDao().getAllUserGroups();
    }

    /**
     * Persists changes in <code>currentUserGroup</code>.
     *
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveUserGroupAction() throws DAOException {
        if (currentUserGroup == null) {
            return "pretty:adminGroups";
        }

        // Apply persistence changes to memberships
        updateUserRoles();
        currentUserGroup.setMemberships(null);

        if (getCurrentUserGroup().getId() != null) {
            if (DataManager.getInstance().getDao().updateUserGroup(getCurrentUserGroup())) {
                Messages.info(StringConstants.MSG_ADMIN_UPDATED_SUCCESSFULLY);
            } else {
                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
                return "pretty:adminGroupEdit";
            }
        } else {
            if (DataManager.getInstance().getDao().addUserGroup(getCurrentUserGroup())) {
                Messages.info(StringConstants.MSG_ADMIN_ADDED_SUCCESSFULLY);
            } else {
                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
                return "pretty:adminGroupNew";
            }
        }
        setCurrentUserGroup(null);

        return "pretty:adminGroups";
    }

    /**
     * <p>
     * deleteUserGroupAction.
     * </p>
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteUserGroupAction(UserGroup userGroup) throws DAOException {
        if (DataManager.getInstance().getDao().deleteUserGroup(userGroup)) {
            Messages.info(StringConstants.MSG_ADMIN_DELETED_SUCCESSFULLY);
        } else {
            Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
        }
    }

    /**
     * <p>
     * resetCurrentUserGroupAction.
     * </p>
     */
    public void resetCurrentUserGroupAction() {
        currentUserGroup = new UserGroup();
    }

    // Role

    /**
     * Returns a list of all existing roles. Required for admin tab components.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Role> getAllRoles() throws DAOException {
        return DataManager.getInstance().getDao().getAllRoles();
    }

    /**
     * <p>
     * saveRoleAction.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveRoleAction() throws DAOException {
        if (getCurrentRole().getId() != null) {
            if (DataManager.getInstance().getDao().updateRole(getCurrentRole())) {
                Messages.info(StringConstants.MSG_ADMIN_UPDATED_SUCCESSFULLY);
            } else {
                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
            }
        } else {
            if (DataManager.getInstance().getDao().addRole(getCurrentRole())) {
                Messages.info(StringConstants.MSG_ADMIN_ADDED_SUCCESSFULLY);
            } else {
                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
            }
        }
        setCurrentRole(null);
    }

    /**
     * <p>
     * deleteRoleAction.
     * </p>
     *
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteRoleAction(Role role) throws DAOException {
        if (DataManager.getInstance().getDao().deleteRole(role)) {
            Messages.info(StringConstants.MSG_ADMIN_DELETED_SUCCESSFULLY);
        } else {
            Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
        }
    }

    /**
     * <p>
     * resetCurrentRoleAction.
     * </p>
     */
    public void resetCurrentRoleAction() {
        currentRole = new Role();
    }

    // UserRole

    /**
     * <p>
     * resetCurrentUserRoleAction.
     * </p>
     */
    public void resetCurrentUserRoleAction() {
        currentUserRole = new UserRole(getCurrentUserGroup(), null, memberRole);
    }

    /**
     * <p>
     * resetDirtyUserRolesAction.
     * </p>
     */
    public void resetDirtyUserRolesAction() {
        dirtyUserRoles.clear();
        // Reset working list in the user group object
        if (currentUserGroup != null) {
            currentUserGroup.setMemberships(null);
        }
    }

    /**
     * Adds currentUserRole to the map of UserRoles to be processed, marked as to save.
     *
     * @throws io.goobi.viewer.exceptions.DAOException
     * @should add user if not yet in group
     */
    public void addUserRoleAction() throws DAOException {
        logger.trace("addUserRoleAction: {}", currentUserRole);
        if (currentUserRole == null) {
            logger.trace("currentUserRole not set");
            Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
            return;
        }
        if (currentUserRole.getUser() == null) {
            logger.trace("currentUserRole: User not set");
            Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
            return;
        }

        if (currentUserGroup != null && !currentUserGroup.getMemberships().contains(currentUserRole)) {
            logger.trace("adding user");
            currentUserGroup.getMemberships().add(currentUserRole);
            dirtyUserRoles.put(currentUserRole, "save");
        }
        resetCurrentUserRoleAction();
    }

    /**
     * <p>
     * Adds currentUserRole to the map of UserRoles to be processed, marked as to delete.
     * </p>
     *
     * @param userRole a {@link io.goobi.viewer.model.security.user.UserRole} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteUserRoleAction(UserRole userRole) throws DAOException {
        logger.trace("deleteUserRoleAction: {}", userRole);
        if (currentUserGroup != null && currentUserGroup.getMemberships().contains(userRole)) {
            currentUserGroup.getMemberships().remove(userRole);
            dirtyUserRoles.put(userRole, "delete");
        }
    }

    /**
     * <p>
     * saveUserRoleAction.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should persist UserRole correctly
     */
    public void updateUserRoles() throws DAOException {
        logger.trace("updateUserRoles: {}", dirtyUserRoles.size());
        if (dirtyUserRoles.isEmpty()) {
            return;
        }

        try {
            //the userRoles don't match the keys of dirtyUserRoles after saving (dirtyUserRoles.get(userRole) returns null for the second entry),
            //so dirty status for each user role is matched by the user behind the userGroup
            Map<User, String> dirtyUsers = dirtyUserRoles.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getUser(), e -> e.getValue()));
            for (Entry<User, String> entry : dirtyUsers.entrySet()) {
                String dirty = entry.getValue();
                UserRole userRole = dirtyUserRoles.keySet().stream().filter(r -> r.getUser().equals(entry.getKey())).findFirst().orElse(null);
                if (userRole == null) {
                    logger.warn("userRole not found");
                    return;
                }

                switch (dirty) {
                    case "save":
                        logger.trace("Saving UserRole: {}", userRole);
                        // If this the user group is not yet persisted, add it to DB first
                        if (userRole.getUserGroup() != null && userRole.getUserGroup().getId() == null) {
                            logger.trace("adding new user group: {}", userRole.getUserGroup());
                            if (!DataManager.getInstance().getDao().addUserGroup(userRole.getUserGroup())) {
                                logger.error("Could not save UserRole: {}", userRole);
                                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
                                continue;
                            }
                        }
                        if (userRole.getId() != null) {
                            // existing
                            if (DataManager.getInstance().getDao().updateUserRole(userRole)) {
                                Messages.info("userGroup_membershipUpdateSuccess");
                            } else {
                                Messages.error("userGroup_membershipUpdateFailure");
                            }
                        } else {
                            // new
                            if (DataManager.getInstance().getDao().addUserRole(userRole)) {
                                Messages.info("userGroup_memberAddSuccess");
                            } else {
                                Messages.error("userGroup_memberAddFailure");
                            }
                        }
                        break;
                    case "delete":
                        logger.trace("Deleting UserRole: {}", userRole);
                        if (userRole.getId() != null) {
                            if (DataManager.getInstance().getDao().deleteUserRole(userRole)) {
                                Messages.info(StringConstants.MSG_ADMIN_DELETED_SUCCESSFULLY);
                            } else {
                                Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
                            }
                        }
                        break;
                    default:
                        logger.warn("Unknown action: {}", dirtyUserRoles.get(userRole));
                }

            }
        } finally {
            resetDirtyUserRolesAction();
        }
    }

    // IpRange

    /**
     * <p>
     * getAllIpRanges.
     * </p>
     *
     * @return all IpRanges from the database
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public List<IpRange> getAllIpRanges() throws DAOException {
        return DataManager.getInstance().getDao().getAllIpRanges();
    }

    /**
     * <p>
     * saveIpRangeAction.
     * </p>
     *
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveIpRangeAction() throws DAOException {
        if (getCurrentIpRange().getId() != null) {
            if (DataManager.getInstance().getDao().updateIpRange(getCurrentIpRange())) {
                Messages.info(StringConstants.MSG_ADMIN_UPDATED_SUCCESSFULLY);
            } else {
                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
                return "pretty:adminIpRangeEdit";
            }
        } else {
            if (DataManager.getInstance().getDao().addIpRange(getCurrentIpRange())) {
                Messages.info(StringConstants.MSG_ADMIN_ADDED_SUCCESSFULLY);
            } else {
                Messages.info(StringConstants.MSG_ADMIN_SAVE_ERROR);
                return "pretty:adminIpRangeNew";
            }
        }
        setCurrentIpRange(null);

        return "pretty:adminIpRanges";
    }

    /**
     * <p>
     * deleteIpRangeAction.
     * </p>
     *
     * @param ipRange a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteIpRangeAction(IpRange ipRange) throws DAOException {
        if (DataManager.getInstance().getDao().deleteIpRange(ipRange)) {
            Messages.info(StringConstants.MSG_ADMIN_DELETED_SUCCESSFULLY);
        } else {
            Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
        }
    }

    /**
     * <p>
     * resetCurrentIpRangeAction.
     * </p>
     */
    public void resetCurrentIpRangeAction() {
        currentIpRange = new IpRange();
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * <p>
     * Getter for the field <code>currentUser</code>.
     * </p>
     *
     * @return the currentUser
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * <p>
     * Setter for the field <code>currentUser</code>.
     * </p>
     *
     * @param currentUser the currentUser to set
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Returns the user ID of <code>currentUser/code>.
     *
     * @return <code>currentUser.id</code> if loaded and has ID; null if not
     */
    public Long getCurrentUserId() {
        if (currentUser != null && currentUser.getId() != null) {
            return currentUser.getId();
        }

        return null;
    }

    /**
     * Sets the current user by loading them from the DB via the given user ID.
     *
     * @param id a {@link java.lang.Long} object
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public void setCurrentUserId(Long id) throws DAOException {
        this.currentUser = DataManager.getInstance().getDao().getUser(id);
    }

    /**
     * <p>
     * Getter for the field <code>currentUserGroup</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     */
    public UserGroup getCurrentUserGroup() {
        return this.currentUserGroup;
    }

    /**
     * <p>
     * Setter for the field <code>currentUserGroup</code>.
     * </p>
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     */
    public void setCurrentUserGroup(UserGroup userGroup) {
        this.currentUserGroup = userGroup;
    }

    /**
     * Returns the user ID of <code>currentUserGroup/code>.
     *
     * @return <code>currentUserGroup.id</code> if loaded and has ID; null if not
     */
    public Long getCurrentUserGroupId() {
        if (currentUserGroup != null && currentUserGroup.getId() != null) {
            return currentUserGroup.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentUserGroup/code> by loading it from the DB via the given ID.
     *
     * @param id a {@link java.lang.Long} object
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public void setCurrentUserGroupId(Long id) throws DAOException {
        this.currentUserGroup = DataManager.getInstance().getDao().getUserGroup(id);
    }

    /**
     * <p>
     * Getter for the field <code>currentRole</code>.
     * </p>
     *
     * @return the currentRole
     */
    public Role getCurrentRole() {
        return currentRole;
    }

    /**
     * <p>
     * Setter for the field <code>currentRole</code>.
     * </p>
     *
     * @param currentRole the currentRole to set
     */
    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole;
    }

    /**
     * Getter for unit tests.
     * 
     * @return the dirtyUserRoles
     */
    Map<UserRole, String> getDirtyUserRoles() {
        return dirtyUserRoles;
    }

    /**
     * <p>
     * Getter for the field <code>currentUserRole</code>.
     * </p>
     *
     * @return the currentUserRole
     */
    public UserRole getCurrentUserRole() {
        return currentUserRole;
    }

    /**
     * <p>
     * Setter for the field <code>currentUserRole</code>.
     * </p>
     *
     * @param currentUserRole the currentUserRole to set
     */
    public void setCurrentUserRole(UserRole currentUserRole) {
        this.currentUserRole = currentUserRole;
    }

    /**
     * <p>
     * Getter for the field <code>currentIpRange</code>.
     * </p>
     *
     * @return the currentIpRange
     */
    public IpRange getCurrentIpRange() {
        return currentIpRange;
    }

    /**
     * <p>
     * Setter for the field <code>currentIpRange</code>.
     * </p>
     *
     * @param currentIpRange the currentIpRange to set
     */
    public void setCurrentIpRange(IpRange currentIpRange) {
        this.currentIpRange = currentIpRange;
    }

    /**
     * Returns the user ID of <code>currentIpRange/code>.
     *
     * @return <code>currentIpRange.id</code> if loaded and has ID; null if not
     */
    public Long getCurrentIpRangeId() {
        if (currentIpRange != null && currentIpRange.getId() != null) {
            return currentIpRange.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentIpRange/code> by loading it from the DB via the given ID.
     *
     * @param id a {@link java.lang.Long} object
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public void setCurrentIpRangeId(Long id) throws DAOException {
        this.currentIpRange = DataManager.getInstance().getDao().getIpRange(id);
    }

    // Lazy models

    /**
     * <p>
     * Getter for the field <code>lazyModelUsers</code>.
     * </p>
     *
     * @return the lazyModelUsers
     */
    public TableDataProvider<User> getLazyModelUsers() {
        return lazyModelUsers;
    }

    /**
     * <p>
     * getPageUsers.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<User> getPageUsers() {
        return lazyModelUsers.getPaginatorList();
    }

    /**
     * <p>
     * Getter for the field <code>currentPassword</code>.
     * </p>
     *
     * @return the currentPassword
     */
    public String getCurrentPassword() {
        return currentPassword;
    }

    /**
     * <p>
     * Setter for the field <code>currentPassword</code>.
     * </p>
     *
     * @param currentPassword the currentPassword to set
     */
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    /**
     * <p>
     * Getter for the field <code>passwordOne</code>.
     * </p>
     *
     * @return the passwordOne
     */
    public String getPasswordOne() {
        return passwordOne;
    }

    /**
     * <p>
     * Setter for the field <code>passwordOne</code>.
     * </p>
     *
     * @param passwordOne the passwordOne to set
     */
    public void setPasswordOne(String passwordOne) {
        this.passwordOne = passwordOne;
    }

    /**
     * <p>
     * Getter for the field <code>passwordTwo</code>.
     * </p>
     *
     * @return the passwordTwo
     */
    public String getPasswordTwo() {
        return passwordTwo;
    }

    /**
     * <p>
     * Setter for the field <code>passwordTwo</code>.
     * </p>
     *
     * @param passwordTwo the passwordTwo to set
     */
    public void setPasswordTwo(String passwordTwo) {
        this.passwordTwo = passwordTwo;
    }

    /**
     * <p>
     * Getter for the field <code>emailConfirmation</code>.
     * </p>
     *
     * @return the emailConfirmation
     */
    public String getEmailConfirmation() {
        return emailConfirmation;
    }

    /**
     * <p>
     * Setter for the field <code>emailConfirmation</code>.
     * </p>
     *
     * @param emailConfirmation the emailConfirmation to set
     */
    public void setEmailConfirmation(String emailConfirmation) {
        this.emailConfirmation = emailConfirmation;
    }

    /**
     * <p>
     * isDeleteUserContributions.
     * </p>
     *
     * @return the deleteUserContributions
     */
    public boolean isDeleteUserContributions() {
        return deleteUserContributions;
    }

    /**
     * <p>
     * Setter for the field <code>deleteUserContributions</code>.
     * </p>
     *
     * @param deleteUserContributions the deleteUserContributions to set
     */
    public void setDeleteUserContributions(boolean deleteUserContributions) {
        logger.trace("setDeleteUserContributions: {}", deleteUserContributions);
        this.deleteUserContributions = deleteUserContributions;
    }

    /**
     * <p>
     * deleteFromCache.
     * </p>
     *
     * @param identifiers a {@link java.util.List} object.
     * @param fromContentCache a boolean.
     * @param fromThumbnailCache a boolean.
     * @return a int.
     */
    public int deleteFromCache(List<String> identifiers, boolean fromContentCache, boolean fromThumbnailCache) {
        return cacheUtils.deleteFromCache(identifiers, fromContentCache, fromThumbnailCache);
    }

    /**
     * <p>
     * deleteFromCache.
     * </p>
     *
     * @param identifiers a {@link java.util.List} object.
     * @param fromContentCache a boolean.
     * @param fromThumbnailCache a boolean.
     * @param fromPdfCache a boolean.
     * @return a int.
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public int deleteFromCache(List<String> identifiers, boolean fromContentCache, boolean fromThumbnailCache, boolean fromPdfCache)
            throws DAOException {
        // Delete download jobs/files
        if (fromPdfCache) {
            for (String identifier : identifiers) {
                DownloadJobTools.removeJobsForRecord(identifier);
            }
        }
        return cacheUtils.deleteFromCache(identifiers, fromContentCache, fromThumbnailCache, fromPdfCache);
    }

    /**
     * <p>
     * setRepresantativeImageAction.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param dataRepository a {@link java.lang.String} object.
     * @param fileIdRoot a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String setRepresantativeImageAction(String pi, String dataRepository, String fileIdRoot) {
        setRepresantativeImageStatic(pi, dataRepository, fileIdRoot);

        return "";
    }

    /**
     * Opens the METS file for the given identifier and sets the attribute USE='banner' to all file elements that match the given file ID root. Any
     * USE='banner' attributes that do not match the file ID root are removed. Solr schema version "intranda_viewer-20130117" or newer required.
     *
     * @param pi a {@link java.lang.String} object.
     * @param dataRepository a {@link java.lang.String} object.
     * @param fileIdRoot a {@link java.lang.String} object.
     */
    public static void setRepresantativeImageStatic(String pi, String dataRepository, String fileIdRoot) {
        logger.debug("setRepresantativeImageStatic");
        if (StringUtils.isEmpty(pi)) {
            return;
        }

        Namespace nsMets = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
        try {
            String metsFilePath = DataFileTools.getSourceFilePath(pi + ".xml", dataRepository, SolrConstants.SOURCEDOCFORMAT_METS);
            Document doc = XmlTools.readXmlFile(metsFilePath);
            if (doc == null || doc.getRootElement() == null) {
                logger.error("Invalid METS file: {}", metsFilePath);
                return;
            }

            List<Element> eleFileList =
                    XmlTools.evaluateToElements("mets:fileSec/mets:fileGrp/mets:file", doc.getRootElement(), Collections.singletonList(nsMets));
            if (eleFileList != null) {
                for (Element eleFile : eleFileList) {
                    String id = eleFile.getAttributeValue("ID");
                    if (StringUtils.isNotEmpty(id)) {
                        if (!id.startsWith(fileIdRoot)) {
                            // If an mets:file element that belongs to a different image already has the USE='banner' attribute, remove it
                            Attribute attrUse = eleFile.getAttribute("USE");
                            if (attrUse != null) {
                                eleFile.removeAttribute(attrUse);
                                logger.debug("Atribute 'USE' removed from '{}' file ID: {}", pi, eleFile.getAttributeValue("ID"));
                            }
                        } else {
                            Attribute attrUse = eleFile.getAttribute("USE");
                            if (attrUse == null) {
                                // Add the USE='banner' attribute
                                eleFile.setAttribute("USE", "banner");
                                eleFile.removeAttribute(attrUse);
                                logger.debug("Atribute 'USE=\"banner\"' set in '{}' file ID: {}", pi, eleFile.getAttributeValue("ID"));
                            } else {
                                // If the correct image already has a USE attribute, make sure its value is 'banner'
                                attrUse.setValue("banner");
                                logger.debug("Atribute 'USE' already exists in '{}' file ID: {}", pi, eleFile.getAttributeValue("ID"));
                            }
                        }
                    } else {
                        logger.warn("METS document for '{}' contains no file ID in some file group.", pi);
                    }
                }
                // Write altered file into hotfolder

                XmlTools.writeXmlFile(doc, DataManager.getInstance().getConfiguration().getHotfolder() + File.separator + pi + ".xml");

                Messages.info("admin_recordReExported");
            } else {
                logger.warn("METS document for '{}' contains no mets:file elements for file ID root: {}", pi, fileIdRoot);
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * <p>
     * toggleSuspendUserAction.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String toggleSuspendUserAction(User user) throws DAOException {
        if (user == null) {
            throw new IllegalArgumentException("user may not be null");
        }
        user.setSuspended(!user.isSuspended());
        if (DataManager.getInstance().getDao().updateUser(user)) {
            Messages.info(user.isSuspended() ? "user_accountSuspended" : "user_accountUnsuspended");
        }

        return "";
    }

    /**
     * <p>
     * triggerMessage.
     * </p>
     *
     * @param message a {@link java.lang.String} object
     */
    public void triggerMessage(String message) {
        logger.debug("Show message: {}", message);
        Messages.info(ViewerResourceBundle.getTranslation(message, null));
    }

    /**
     * <p>
     * isDisplayTranslationsDashboardWidget.
     * </p>
     *
     * @return true if at least one group is not fully translated; false otherwise
     */
    public boolean isDisplayTranslationsDashboardWidget() {
        for (TranslationGroup group : getConfiguredTranslationGroups()) {
            if (!group.isFullyTranslated()) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * getConfiguredTranslationGroupsCount.
     * </p>
     *
     * @return Number of configured translation grouns
     */
    public long getConfiguredTranslationGroupsCount() {
        return DataManager.getInstance()
                .getConfiguration()
                .getTranslationGroups()
                .stream()
                .filter(g -> !g.isLoadError())
                .filter(g -> g.getEntryCount() - g.getFullyTranslatedEntryCount() > 0)
                .count();
    }

    /**
     * <p>
     * getConfiguredTranslationGroups.
     * </p>
     *
     * @return All configured <code>TranslationGroup</code>s
     */
    public List<TranslationGroup> getConfiguredTranslationGroups() {
        synchronized (TRANSLATION_LOCK) {
            List<TranslationGroup> ret = DataManager.getInstance().getConfiguration().getTranslationGroups();
            logger.trace("groups: {}", ret.size());
            setTranslationGroupsEditorSession(BeanUtils.getSession().getId());
            logger.trace("Locked translation for: {}", translationGroupsEditorSession);
            return ret;
        }
    }

    /**
     * <p>
     * getTranslationGroupsForSolrField.
     * </p>
     *
     * @param field Index field that the translation groups should have as a key
     * @return List of TranslationGroups; null if not found
     * @should return correct groups
     */
    public List<TranslationGroup> getTranslationGroupsForSolrField(String field) {
        return getTranslationGroupsForSolrFieldStatic(field);
    }

    /**
     * <p>
     * getTranslationGroupForFieldAndKey.
     * </p>
     *
     * @param field Solr field
     * @param key Message key
     * @return First <code>TranslationGroup</code> that contains the requested field+key; null if none found
     */
    public TranslationGroup getTranslationGroupForFieldAndKey(String field, String key) {
        for (TranslationGroup group : getTranslationGroupsForSolrFieldStatic(field)) {
            for (MessageEntry entry : group.getAllEntries()) {
                if (entry.getKey().equals(key) || entry.getKey().startsWith(key + ".")) {
                    return group;
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * getTranslationGroupsForSolrFieldStatic.
     * </p>
     *
     * @param field Index field that the translation groups should have as a key
     * @return List of TranslationGroups; null if not found
     */
    public static List<TranslationGroup> getTranslationGroupsForSolrFieldStatic(String field) {
        if (StringUtils.isEmpty(field)) {
            return Collections.emptyList();
        }

        synchronized (TRANSLATION_LOCK) {
            List<TranslationGroup> ret = new ArrayList<>();
            for (TranslationGroup group : DataManager.getInstance().getConfiguration().getTranslationGroups()) {
                if (group.getType().equals(TranslationGroupType.SOLR_FIELD_VALUES)) {
                    for (TranslationGroupItem item : group.getItems()) {
                        if (field.equals(item.getKey())) {
                            ret.add(group);
                        }
                    }
                }
            }
            return ret;
        }
    }

    /**
     * Saves <code>currentTranslationGroup</code> if it has a selected entry. Resets group to null afterwards.
     */
    public void saveAndResetCurrentTranslationGroup() {
        if (currentTranslationGroup != null) {
            currentTranslationGroup.saveSelectedEntry();
            currentTranslationGroup = null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>currentTranslationGroup</code>.
     * </p>
     *
     * @return the currentTranslationGroup
     */
    public TranslationGroup getCurrentTranslationGroup() {
        synchronized (TRANSLATION_LOCK) {
            if (translationGroupsEditorSession != null && !translationGroupsEditorSession.equals(BeanUtils.getSession().getId())) {
                logger.trace("Translation locked");
                Messages.error("Translation already in use");
                return null;
            }

            return currentTranslationGroup;
        }
    }

    /**
     * <p>
     * Setter for the field <code>currentTranslationGroup</code>.
     * </p>
     *
     * @param currentTranslationGroup the currentTranslationGroup to set
     */
    public void setCurrentTranslationGroup(TranslationGroup currentTranslationGroup) {
        this.currentTranslationGroup = currentTranslationGroup;
    }

    /**
     * <p>
     * isNewMessageEntryModeAllowed.
     * </p>
     *
     * @return true if at least one LOCAL_STRINGS type group is found in config; false otherwise
     */
    public boolean isNewMessageEntryModeAllowed() {
        for (TranslationGroup group : DataManager.getInstance().getConfiguration().getTranslationGroups()) {
            if (group.getType().equals(TranslationGroupType.LOCAL_STRINGS) && !group.getItems().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Triggers a mode for adding new message keys to the first LOCAL_STRINGS type group found.
     */
    public void triggerNewMessageEntryMode() {
        List<TranslationGroup> groups = DataManager.getInstance().getConfiguration().getTranslationGroups();
        for (TranslationGroup group : groups) {
            if (group.getType().equals(TranslationGroupType.LOCAL_STRINGS) && !group.getItems().isEmpty()) {
                setCurrentTranslationGroup(group);
                MessageEntry entry =
                        MessageEntry.create(group.getItems().get(0).getKey().replace(".*", ""), "", ViewerResourceBundle.getAllLocales());
                entry.setNewEntryMode(true);
                //                group.getAllEntries().add(entry);
                group.setSelectedEntry(entry);
                return;
            }
        }
    }

    /**
     * Saves currently selected message entry in the current translation group and returns to the translations overview page.
     *
     * @return Target page
     */
    public String saveSelectedMessageEntryAction() {
        if (currentTranslationGroup == null || currentTranslationGroup.getSelectedEntry() == null) {
            return "";
        }

        currentTranslationGroup.saveSelectedEntry();
        return "pretty:adminTranslations";
    }

    /**
     * Reset selected message entry and returns to the translations overview page.
     *
     * @return Target page
     */
    public String cancelSelectedMessageEntryAction() {
        // Reset selected entry so it doesn't get saved
        if (currentTranslationGroup != null) {
            currentTranslationGroup.resetSelectedEntry();
        }

        return "pretty:adminTranslations";
    }

    /**
     * <p>
     * getCurrentTranslationGroupId.
     * </p>
     *
     * @return Index of currentTranslationGroup in the list of configured groups
     */
    public int getCurrentTranslationGroupId() {
        synchronized (TRANSLATION_LOCK) {
            if (currentTranslationGroup != null) {
                return DataManager.getInstance().getConfiguration().getTranslationGroups().indexOf(currentTranslationGroup);
            }

            return 0;
        }
    }

    /**
     * <p>
     * setCurrentTranslationGroupId.
     * </p>
     *
     * @param id Looks up and loads <code>currentTranslationGroup</code> that matches the given id
     */
    public void setCurrentTranslationGroupId(int id) {
        List<TranslationGroup> groups = DataManager.getInstance().getConfiguration().getTranslationGroups();
        if (id >= 0 && groups.size() > id) {
            TranslationGroup group = groups.get(id);
            if (!group.equals(currentTranslationGroup)) {
                currentTranslationGroup = groups.get(id);
            }
        } else {
            logger.error("Translation group ID not found: {}", id);
        }
    }

    /**
     * <p>
     * getCurrentTranslationMessageKey.
     * </p>
     *
     * @return Key of the currently selected entry; otherwise "-"
     */
    public String getCurrentTranslationMessageKey() {
        if (currentTranslationGroup != null && currentTranslationGroup.getSelectedEntry() != null) {
            return currentTranslationGroup.getSelectedEntry().getKey();
        }

        return "-";
    }

    /**
     * If <code>currentTranslationGroup</code> is set, looks up the message entry for the given key and pre-selects it.
     *
     * @param key Message key to select
     */
    public void setCurrentTranslationMessageKey(String key) {
        if (currentTranslationGroup != null) {
            currentTranslationGroup.findEntryByMessageKey(key);
        }
    }

    /**
     * <p>
     * isTranslationLocked.
     * </p>
     *
     * @return true if translations are locked by a different user; false otherwise
     */
    public boolean isTranslationLocked() {
        return translationGroupsEditorSession != null && !translationGroupsEditorSession.equals(BeanUtils.getSession().getId());
    }

    /**
     * <p>
     * lockTranslation.
     * </p>
     */
    public void lockTranslation() {
        if (translationGroupsEditorSession == null) {
            setTranslationGroupsEditorSession(BeanUtils.getSession().getId());
            logger.trace("Translation locked");
        }
    }

    /**
     * <p>
     * Getter for the field <code>translationGroupsEditorSession</code>.
     * </p>
     *
     * @return the translationGroupsEditorSession
     */
    public static String getTranslationGroupsEditorSession() {
        return translationGroupsEditorSession;
    }

    /**
     * <p>
     * Setter for the field <code>translationGroupsEditorSession</code>.
     * </p>
     *
     * @param translationGroupsEditorSession the translationGroupsEditorSession to set
     */
    public static void setTranslationGroupsEditorSession(String translationGroupsEditorSession) {
        logger.trace("setTranslationGroupsEditorSession: {}", translationGroupsEditorSession);
        AdminBean.translationGroupsEditorSession = translationGroupsEditorSession;
    }

    /**
     * <p>
     * updateHotfolderFileCount.
     * </p>
     */
    public void updateHotfolderFileCount() {
        hotfolderFileCount.send("update");
    }

    /**
     * <p>
     * Getter for the field <code>hotfolderFileCount</code>.
     * </p>
     *
     * @return Number of queued records in hotfolder
     */
    public int getHotfolderFileCount() {
        return DataManager.getInstance().getHotfolderFileCount();
    }

    /**
     * <p>
     * isHasAccessPermissingForTranslationFiles.
     * </p>
     *
     * @return {@link io.goobi.viewer.model.translations.admin.TranslationGroup#isHasFileAccess()}
     */
    public boolean isHasAccessPermissingForTranslationFiles() {
        return TranslationGroup.isHasFileAccess();
    }

    /**
     * <p>
     * Setter for the field <code>uploadedAvatarFile</code>.
     * </p>
     *
     * @param uploadedAvatarFile the uploadedAvatarFile to set
     */
    public void setUploadedAvatarFile(Part uploadedAvatarFile) {
        this.uploadedAvatarFile = uploadedAvatarFile;
    }

    /**
     * <p>
     * Getter for the field <code>uploadedAvatarFile</code>.
     * </p>
     *
     * @return the uploadedAvatarFile
     */
    public Part getUploadedAvatarFile() {
        return uploadedAvatarFile;
    }
}
