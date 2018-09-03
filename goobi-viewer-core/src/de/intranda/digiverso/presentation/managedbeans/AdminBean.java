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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataProvider;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataProvider.SortOrder;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataSource;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.annotation.Comment;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.security.License;
import de.intranda.digiverso.presentation.model.security.LicenseType;
import de.intranda.digiverso.presentation.model.security.Role;
import de.intranda.digiverso.presentation.model.security.user.IpRange;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.security.user.UserGroup;
import de.intranda.digiverso.presentation.model.security.user.UserRole;
import de.unigoettingen.sub.commons.util.CacheUtils;

/**
 * Administration backend functions.
 */
@SuppressWarnings("deprecation")
@Named
@SessionScoped
public class AdminBean implements Serializable {

    private static final long serialVersionUID = -8334669036711935331L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(AdminBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    private TableDataProvider<User> lazyModelUsers;
    private TableDataProvider<UserGroup> lazyModelUserGroups;
    //    private TableDataProvider<Role> lazyModelRoles;
    private TableDataProvider<LicenseType> lazyModelLicenseTypes;
    private TableDataProvider<IpRange> lazyModelIpRanges;
    private TableDataProvider<Comment> lazyModelComments;

    private User currentUser = null;
    private UserGroup currentUserGroup = null;
    private Role currentRole = null;
    private UserRole currentUserRole = null;
    private LicenseType currentLicenseType = null;
    private License currentLicense = null;
    private IpRange currentIpRange = null;
    private Comment currentComment = null;

    private String passwordOne = "";
    private String passwordTwo = "";

    public AdminBean() {
        // the emptiness inside
    }

    /**
     * @should sort lazyModelComments by dateUpdated desc by default
     */
    @PostConstruct
    public void init() {
        lazyModelUsers = new TableDataProvider<>(new TableDataSource<User>() {

            @Override
            public List<User> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                logger.trace("getEntries<User>");
                try {
                    if (StringUtils.isEmpty(sortField)) {
                        sortField = "id";
                    }
                    return DataManager.getInstance().getDao().getUsers(first, pageSize, sortField, sortOrder.asBoolean(), filters);
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
            }
        });
        lazyModelUsers.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelUsers.setFilters("firstName_lastName_nickName_email", "score");

        lazyModelUserGroups = new TableDataProvider<>(new TableDataSource<UserGroup>() {

            @Override
            public List<UserGroup> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                if (StringUtils.isEmpty(sortField)) {
                    sortField = "name";
                }
                try {
                    return DataManager.getInstance().getDao().getUserGroups(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                try {
                    return DataManager.getInstance().getDao().getUserGroupCount(filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage(), e);
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
            }
        });
        lazyModelUserGroups.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelUserGroups.setFilters("name");

        //        lazyModelRoles = new TableDataProvider<>(new TableDataSource<Role>() {
        //
        //            @Override
        //            public List<Role> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
        //                List<Role> roleList = new ArrayList<>();
        //                if (sortField == null) {
        //                    sortField = "id";
        //                }
        //                try {
        //                    roleList = DataManager.getInstance().getDao().getRoles(first, pageSize, sortField, sortOrder.asBoolean(), filters);
        //                } catch (DAOException e) {
        //                    logger.error(e.getMessage());
        //                }
        //
        //                return roleList;
        //            }
        //
        //            @Override
        //            public long getTotalNumberOfRecords() {
        //                try {
        //                    return DataManager.getInstance().getDao().getRoleCount(lazyModelRoles.getFiltersAsMap());
        //                } catch (DAOException e) {
        //                    logger.error(e.getMessage(), e);
        //                    return 0;
        //                }
        //            }
        //        });
        //        lazyModelRoles.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        //        lazyModelRoles.setFilters("name");

        lazyModelLicenseTypes = new TableDataProvider<>(new TableDataSource<LicenseType>() {

            @Override
            public List<LicenseType> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                if (StringUtils.isEmpty(sortField)) {
                    sortField = "name";
                }
                try {
                    return DataManager.getInstance().getDao().getLicenseTypes(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                try {
                    return DataManager.getInstance().getDao().getLicenseTypeCount(filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage(), e);
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
            }
        });
        lazyModelLicenseTypes.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelLicenseTypes.setFilters("name");

        lazyModelIpRanges = new TableDataProvider<>(new TableDataSource<IpRange>() {

            @Override
            public List<IpRange> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                if (StringUtils.isEmpty(sortField)) {
                    sortField = "name";
                }
                try {
                    return DataManager.getInstance().getDao().getIpRanges(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                try {
                    return DataManager.getInstance().getDao().getIpRangeCount(filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage(), e);
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
            }
        });
        lazyModelIpRanges.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelIpRanges.setFilters("name", "subnetMask", "description");

        lazyModelComments = new TableDataProvider<>(new TableDataSource<Comment>() {

            @Override
            public List<Comment> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                try {
                    if (StringUtils.isEmpty(sortField)) {
                        sortField = "dateCreated";
                        sortOrder = SortOrder.DESCENDING;
                    }
                    return DataManager.getInstance().getDao().getComments(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                }
                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                try {
                    return DataManager.getInstance().getDao().getCommentCount(filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage(), e);
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
            }
        });
        lazyModelComments.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelComments.setFilters("pi", "text");
    }

    // User

    /**
     * Returns all users in the DB. Needed for getting a list of users (e.g for adding user group members).
     */
    public List<User> getAllUsers() throws DAOException {
        return DataManager.getInstance().getDao().getAllUsers(true);
    }

    /**
     * @throws DAOException
     *
     */
    public String saveUserAction() throws DAOException {
        // Retrieving a new user from the DB overrides the current object and resets the field, so save a copy
        User copy = currentUser.clone();
        // Copy of the copy contains the previous nickname, in case the chosen one is already taken
        copy.setCopy(currentUser.getCopy().clone());
        // Do not allow the same nickname being used for multiple users
        User nicknameOwner = DataManager.getInstance().getDao().getUserByNickname(currentUser.getNickName()); // This basically resets all changes
        if (nicknameOwner != null && nicknameOwner.getId() != currentUser.getId()) {
            Messages.error(Helper.getTranslation("user_nicknameTaken", null).replace("{0}", currentUser.getNickName().trim()));
            currentUser = copy;
            currentUser.setNickName(copy.getCopy().getNickName());
            return "adminUser";
        }
        currentUser = copy;
        if (getCurrentUser().getId() != null) {
            // Existing user
            if (StringUtils.isNotEmpty(passwordOne) || StringUtils.isNotEmpty(passwordTwo)) {
                if (!passwordOne.equals(passwordTwo)) {
                    Messages.error("user_passwordMismatch");
                    return "adminUser";
                }
                currentUser.setNewPassword(passwordOne);
            }
            if (DataManager.getInstance().getDao().updateUser(getCurrentUser())) {
                Messages.info("user_saveSuccess");
            } else {
                Messages.error("errSave");
                return "adminUser";
            }
        } else {
            // New user
            if (DataManager.getInstance().getDao().getUserByEmail(currentUser.getEmail()) != null) {
                // Do not allow the same email address being used for multiple users
                Messages.error("newUserExist");
                logger.debug("User account already exists for '" + currentUser.getEmail() + "'.");
                return "adminUser";
            }
            if (StringUtils.isEmpty(passwordOne) || StringUtils.isEmpty(passwordTwo)) {
                Messages.error("newUserPasswordOneRequired");
                return "adminUser";
            } else if (!passwordOne.equals(passwordTwo)) {
                Messages.error("user_passwordMismatch");
                return "adminUser";
            } else {
                getCurrentUser().setNewPassword(passwordOne);

            }
            if (DataManager.getInstance().getDao().addUser(getCurrentUser())) {
                Messages.info("newUserCreated");
            } else {
                Messages.info("errSave");
                return "adminUser";
            }
        }
        setCurrentUser(null);
        
        return "adminAllUsers";
    }

    /**
     * @throws DAOException
     *
     */
    public void deleteUserAction(User user) throws DAOException {
        logger.debug("Deleting user: " + user.getDisplayName());
        if (DataManager.getInstance().getDao().deleteUser(user)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }
    }

    public void resetCurrentUserAction() {
        currentUser = new User();
    }

    /**
     * Persists changes in <code>currentUserGroup</code>.
     *
     * @throws DAOException
     */
    public void saveUserGroupAction() throws DAOException {
        if (currentUserGroup != null) {
            if (getCurrentUserGroup().getId() != null) {
                if (DataManager.getInstance().getDao().updateUserGroup(getCurrentUserGroup())) {
                    Messages.info("updatedSuccessfully");
                } else {
                    Messages.info("errSave");
                }
            } else {
                if (DataManager.getInstance().getDao().addUserGroup(getCurrentUserGroup())) {
                    Messages.info("addedSuccessfully");
                } else {
                    Messages.info("errSave");
                }
            }
        }
        setCurrentUserGroup(null);
    }

    /**
     * @throws DAOException
     *
     */
    public void deleteUserGroupAction(UserGroup userGroup) throws DAOException {
        if (DataManager.getInstance().getDao().deleteUserGroup(userGroup)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }
    }

    public void resetCurrentUserGroupAction() {
        currentUserGroup = new UserGroup();
    }

    // Role

    /**
     * Returns a list of all existing roles. Required for admin tab components.
     *
     * @return
     * @throws DAOException
     */
    public List<Role> getAllRoles() throws DAOException {
        return DataManager.getInstance().getDao().getAllRoles();
    }

    /**
     * @throws DAOException
     *
     */
    public void saveRoleAction() throws DAOException {
        // String name = getCurrentRole().getName();
        if (getCurrentRole().getId() != null) {
            if (DataManager.getInstance().getDao().updateRole(getCurrentRole())) {
                Messages.info("updatedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        } else {
            if (DataManager.getInstance().getDao().addRole(getCurrentRole())) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        }
        setCurrentRole(null);
    }

    /**
     * @throws DAOException
     *
     */
    public void deleteRoleAction(Role role) throws DAOException {
        if (DataManager.getInstance().getDao().deleteRole(role)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }
    }

    public void resetCurrentRoleAction() {
        currentRole = new Role();
    }

    // UserRole

    public void resetCurrentUserRoleAction() {
        currentUserRole = new UserRole(getCurrentUserGroup(), null, null);
    }

    /**
     * @throws DAOException
     *
     */
    public void saveUserRoleAction() throws DAOException {
        logger.debug(getCurrentUserRole().getUserGroup() + ", " + getCurrentUserRole().getUser() + ", " + getCurrentUserRole().getRole());
        if (getCurrentUserRole().getId() != null) {
            // existing
            if (DataManager.getInstance().getDao().updateUserRole(getCurrentUserRole())) {
                Messages.info("userGroup_membershipUpdateSuccess");
            } else {
                Messages.error("userGroup_membershipUpdateFailure");
            }
        } else {
            // new
            if (DataManager.getInstance().getDao().addUserRole(getCurrentUserRole())) {
                Messages.info("userGroup_memberAddSuccess");
            } else {
                Messages.error("userGroup_memberAddFailure");
            }
        }
        setCurrentUserRole(null);
    }

    /**
     * @throws DAOException
     *
     */
    public void deleteUserRoleAction(UserRole userRole) throws DAOException {
        if (DataManager.getInstance().getDao().deleteUserRole(userRole)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }
        setCurrentUserRole(null);
    }

    // LicenseType

    /**
     * Returns all existing license types. Required for admin tabs.
     */
    public List<LicenseType> getAllLicenseTypes() throws DAOException {
        return DataManager.getInstance().getDao().getAllLicenseTypes();
    }

    /**
     * @throws DAOException
     *
     */
    public String saveLicenseTypeAction() throws DAOException {
        // String name = getCurrentLicenseType().getName();
        if (getCurrentLicenseType().getId() != null) {
            if (DataManager.getInstance().getDao().updateLicenseType(getCurrentLicenseType())) {
                Messages.info("updatedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        } else {
            if (DataManager.getInstance().getDao().addLicenseType(getCurrentLicenseType())) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        }
        setCurrentLicenseType(null);

        return "licenseTypes";
    }

    /**
     * @throws DAOException
     *
     */
    public void deleteLicenseTypeAction(LicenseType licenseType) throws DAOException {
        if (DataManager.getInstance().getDao().deleteLicenseType(licenseType)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }
    }

    public void resetCurrentLicenseTypeAction() {
        logger.debug("resetCurrentLicenseTypeAction");
        currentLicenseType = new LicenseType();
    }

    // IpRange

    /**
     * @throws DAOException
     *
     */
    public void saveIpRangeAction() throws DAOException {
        // String name = getCurrentIpRange().getName();
        if (getCurrentIpRange().getId() != null) {
            if (DataManager.getInstance().getDao().updateIpRange(getCurrentIpRange())) {
                Messages.info("updatedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        } else {
            if (DataManager.getInstance().getDao().addIpRange(getCurrentIpRange())) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        }
        setCurrentIpRange(null);
    }

    /**
     * @throws DAOException
     *
     */
    public void deleteIpRangeAction(IpRange ipRange) throws DAOException {
        if (DataManager.getInstance().getDao().deleteIpRange(ipRange)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }
    }

    public void resetCurrentIpRangeAction() {
        currentIpRange = new IpRange();
    }

    public void resetCurrentLicenseAction() {
        logger.trace("resetCurrentLicenseAction");
        setCurrentLicense(null);
    }

    public void resetCurrentLicenseForUserAction() {
        logger.trace("resetCurrentLicenseForUserAction");
        currentLicense = new License();
        currentLicense.setUser(getCurrentUser());
    }

    public void resetCurrentLicenseForUserGroupAction() {
        logger.trace("resetCurrentLicenseForUserGroupAction");
        currentLicense = new License();
        currentLicense.setUserGroup(getCurrentUserGroup());
    }

    public void resetCurrentLicenseForIpRangeAction() {
        logger.trace("resetCurrentLicenseForIpRangeAction");
        currentLicense = new License();
        currentLicense.setIpRange(getCurrentIpRange());
    }

    public String saveCurrentLicenseAction() throws DAOException {
        logger.trace("saveCurrentLicenseAction");
        String ret = saveLicenseAction(currentLicense);
        resetCurrentLicenseAction();
        return ret;
    }

    /**
     * Adds the current License to the licensee (User, UserGroup or IpRange). It is imperative that the licensee object is refreshed after updating so
     * that a new license object is an ID attached. Otherwise the list of licenses will throw an NPE!
     *
     * @throws DAOException
     */
    public String saveLicenseAction(License license) throws DAOException {
        logger.trace("saveLicenseAction");
        if (license == null) {
            throw new IllegalArgumentException("license may not be null");
        }
        if (license.getUser() != null) {
            // User
            license.getUser().addLicense(license);
            if (DataManager.getInstance().getDao().updateUser(license.getUser())) {
                Messages.info("license_licenseSaveSuccess");
            } else {
                Messages.error("license_licenseSaveFailure");
            }
        } else if (license.getUserGroup() != null) {
            // UserGroup
            license.getUserGroup().addLicense(license);
            if (DataManager.getInstance().getDao().updateUserGroup(license.getUserGroup())) {
                Messages.info("license_licenseSaveSuccess");
            } else {
                Messages.error("license_licenseSaveFailure");
            }
        } else if (license.getIpRange() != null) {
            // IpRange
            logger.trace("ip range id:{} ", license.getIpRange().getId());
            license.getIpRange().addLicense(license);
            if (DataManager.getInstance().getDao().updateIpRange(license.getIpRange())) {
                Messages.info("license_licenseSaveSuccess");
            } else {
                Messages.error("license_licenseSaveFailure");
            }
        } else {
            logger.trace("nothing");
            Messages.error("license_licenseSaveFailure");
        }

        return "";
    }

    public String deleteLicenseAction(License license) throws DAOException {
        if (license == null) {
            throw new IllegalArgumentException("license may not be null");
        }
        boolean success = false;
        String ret = "adminUser";
        logger.debug("removing license: " + license.getLicenseType().getName());
        if (license.getUser() != null) {
            license.getUser().removeLicense(license);
            success = DataManager.getInstance().getDao().updateUser(license.getUser());
        } else if (license.getUserGroup() != null) {
            license.getUserGroup().removeLicense(license);
            success = DataManager.getInstance().getDao().updateUserGroup(license.getUserGroup());
            ret = "adminUserGroup";
        } else if (license.getIpRange() != null) {
            license.getIpRange().removeLicense(license);
            success = DataManager.getInstance().getDao().updateIpRange(license.getIpRange());
            ret = "adminIpRange";
        }

        if (success) {
            Messages.info("license_deleteSuccess");
        } else {
            Messages.error("license_deleteFailure");
        }
        setCurrentLicense(null);

        return ret;
    }

    // Comments

    public void saveCommentAction(Comment comment) throws DAOException {
        logger.trace("saveCommentAction");
        if (comment.getId() != null) {
            // Set updated timestamp
            comment.setDateUpdated(new Date());
            logger.trace(comment.getText());
            if (DataManager.getInstance().getDao().updateComment(comment)) {
                Messages.info("updatedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        } else {
            if (DataManager.getInstance().getDao().addComment(comment)) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info("errSave");
            }
        }
        resetCurrentCommentAction();
    }

    public String deleteCommentAction(Comment comment) throws DAOException {
        if (DataManager.getInstance().getDao().deleteComment(comment)) {
            Messages.info("commentDeleteSuccess");
        } else {
            Messages.error("commentDeleteFailure");
        }

        return "";
    }

    public void resetCurrentCommentAction() {
        currentComment = null;
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * @return the currentUser
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * @param currentUser the currentUser to set
     */
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public UserGroup getCurrentUserGroup() {
        return this.currentUserGroup;
    }

    public void setCurrentUserGroup(UserGroup userGroup) {
        this.currentUserGroup = userGroup;
    }

    /**
     * @return the currentRole
     */
    public Role getCurrentRole() {
        return currentRole;
    }

    /**
     * @param currentRole the currentRole to set
     */
    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole;
    }

    /**
     * @return the currentUserRole
     */
    public UserRole getCurrentUserRole() {
        return currentUserRole;
    }

    /**
     * @param currentUserRole the currentUserRole to set
     */
    public void setCurrentUserRole(UserRole currentUserRole) {
        this.currentUserRole = currentUserRole;
    }

    /**
     * @return the currentLicenseType
     */
    public LicenseType getCurrentLicenseType() {
        return currentLicenseType;
    }

    /**
     * @param currentLicenseType the currentLicenseType to set
     */
    public void setCurrentLicenseType(LicenseType currentLicenseType) {
        if (currentLicenseType != null) {
            logger.debug("setCurrentLicenseType: " + currentLicenseType.getName());
        }
        this.currentLicenseType = currentLicenseType;
    }

    /**
     * @return the currentLicense
     */
    public License getCurrentLicense() {
        return currentLicense;
    }

    /**
     * @param currentLicense the currentLicense to set
     */
    public void setCurrentLicense(License currentLicense) {
        logger.trace("setCurrentLicense: {}", currentLicense);
        this.currentLicense = currentLicense;
    }

    /**
     * @return the currentIpRange
     */
    public IpRange getCurrentIpRange() {
        return currentIpRange;
    }

    /**
     * @param currentIpRange the currentIpRange to set
     */
    public void setCurrentIpRange(IpRange currentIpRange) {
        this.currentIpRange = currentIpRange;
    }

    /**
     * @return the currentComment
     */
    public Comment getCurrentComment() {
        return currentComment;
    }

    /**
     * @param currentComment the currentComment to set
     */
    public void setCurrentComment(Comment currentComment) {
        this.currentComment = currentComment;
    }

    // Lazy models

    /**
     * @return the lazyModelUsers
     */
    public TableDataProvider<User> getLazyModelUsers() {
        return lazyModelUsers;
    }

    public List<User> getPageUsers() throws DAOException {
        return lazyModelUsers.getPaginatorList();
    }

    /**
     * @return the lazyModelUserGroups
     */
    public TableDataProvider<UserGroup> getLazyModelUserGroups() {
        return lazyModelUserGroups;
    }

    public List<UserGroup> getPageUserGroups() throws DAOException {
        return lazyModelUserGroups.getPaginatorList();
    }

    /**
     * @return the lazyModelLicenseTypes
     */
    public TableDataProvider<LicenseType> getLazyModelLicenseTypes() {
        return lazyModelLicenseTypes;
    }

    public List<LicenseType> getPageLicenseTypes() throws DAOException {
        return lazyModelLicenseTypes.getPaginatorList();
    }

    /**
     * @return the lazyModelIpRanges
     */
    public TableDataProvider<IpRange> getLazyModelIpRanges() {
        return lazyModelIpRanges;
    }

    public List<IpRange> getPageIpRanges() throws DAOException {
        return lazyModelIpRanges.getPaginatorList();
    }

    /**
     * @return the lazyModelComments
     */
    public TableDataProvider<Comment> getLazyModelComments() {
        return lazyModelComments;
    }

    public List<Comment> getPageComments() throws DAOException {
        return lazyModelComments.getPaginatorList();
    }

    /**
     * @return the passwordOne
     */
    public String getPasswordOne() {
        return passwordOne;
    }

    /**
     * @param passwordOne the passwordOne to set
     */
    public void setPasswordOne(String passwordOne) {
        this.passwordOne = passwordOne;
    }

    /**
     * @return the passwordTwo
     */
    public String getPasswordTwo() {
        return passwordTwo;
    }

    /**
     * @param passwordTwo the passwordTwo to set
     */
    public void setPasswordTwo(String passwordTwo) {
        this.passwordTwo = passwordTwo;
    }

    public int deleteFromCache(List<String> identifiers, boolean fromContentCache, boolean fromThumbnailCache) {
        return CacheUtils.deleteFromCache(identifiers, fromContentCache, fromThumbnailCache);
    }

    public int deleteFromCache(List<String> identifiers, boolean fromContentCache, boolean fromThumbnailCache, boolean fromPdfCache) {
        return CacheUtils.deleteFromCache(identifiers, fromContentCache, fromThumbnailCache, fromPdfCache);
    }

    public String setRepresantativeImageAction(String pi, String dataRepository, String fileIdRoot) {
        setRepresantativeImageStatic(pi, dataRepository, fileIdRoot);

        return "";
    }

    /**
     * Opens the METS file for the given identifier and sets the attribute USE='banner' to all file elements that match the given file ID root. Any
     * USE='banner' attributes that do not match the file ID root are removed. Solr schema version "intranda_viewer-20130117" or newer required.
     *
     * @param pi
     * @param dataRepository
     * @param fileIdRoot
     */
    @SuppressWarnings("unchecked")
    public static void setRepresantativeImageStatic(String pi, String dataRepository, String fileIdRoot) {
        logger.debug("setRepresantativeImageStatic");
        if (StringUtils.isNotEmpty(pi)) {
            Namespace nsMets = Namespace.getNamespace("mets", "http://www.loc.gov/METS/");
            try {
                StringBuilder sbFilePath = new StringBuilder();
                if (StringUtils.isNotEmpty(dataRepository)) {
                    String dataRepositoriesHome = DataManager.getInstance().getConfiguration().getDataRepositoriesHome();
                    if (StringUtils.isNotEmpty(dataRepositoriesHome)) {
                        sbFilePath.append(dataRepositoriesHome).append(File.separator);
                    }
                    sbFilePath.append(dataRepository).append(File.separator).append(DataManager.getInstance().getConfiguration()
                            .getIndexedMetsFolder()).append(File.separator).append(pi).append(".xml");
                } else {
                    // Backwards compatibility with old indexes
                    sbFilePath.append(DataManager.getInstance().getConfiguration().getViewerHome()).append(DataManager.getInstance()
                            .getConfiguration().getIndexedMetsFolder()).append(File.separator).append(pi).append(".xml");
                }
                Document doc = FileTools.readXmlFile(sbFilePath.toString());

                XPath xp = XPath.newInstance("mets:mets/mets:fileSec/mets:fileGrp/mets:file");
                xp.addNamespace(nsMets);
                List<Element> eleFileList = (List<Element>) xp.selectNodes(doc);
                if (eleFileList != null) {
                    for (Element eleFile : eleFileList) {
                        String id = eleFile.getAttributeValue("ID");
                        if (StringUtils.isNotEmpty(id)) {
                            if (!id.startsWith(fileIdRoot)) {
                                // If an mets:file element that belongs to a different image already has the USE='banner' attribute, remove it
                                Attribute attrUse = eleFile.getAttribute("USE");
                                if (attrUse != null) {
                                    eleFile.removeAttribute(attrUse);
                                    logger.debug("Atribute 'USE' removed from '" + pi + "' file ID: " + eleFile.getAttributeValue("ID"));
                                }
                            } else {
                                Attribute attrUse = eleFile.getAttribute("USE");
                                if (attrUse == null) {
                                    // Add the USE='banner' attribute
                                    eleFile.setAttribute("USE", "banner");
                                    eleFile.removeAttribute(attrUse);
                                    logger.debug("Atribute 'USE=\"banner\"' set in '" + pi + "' file ID: " + eleFile.getAttributeValue("ID"));
                                } else {
                                    // If the correct image already has a USE attribute, make sure its value is 'banner'
                                    attrUse.setValue("banner");
                                    logger.debug("Atribute 'USE' already exists in '" + pi + "' file ID: " + eleFile.getAttributeValue("ID"));
                                }
                            }
                        } else {
                            logger.warn("METS document for '" + pi + "' contains no file ID in some file group.");
                        }
                    }
                    // Write altered file into hotfolder

                    FileTools.writeXmlFile(doc, DataManager.getInstance().getConfiguration().getHotfolder() + File.separator + pi + ".xml");

                    Messages.info("admin_recordReExported");
                } else {
                    logger.warn("METS document for '" + pi + "' contains no mets:file elements for file ID root: " + fileIdRoot);
                }
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            } catch (JDOMException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     *
     * @param user
     * @return
     * @throws DAOException
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
     * Querys solr for a list of all values of the set ACCESSCONDITION
     * 
     * @return A list of all indexed ACCESSCONDITIONs
     * @throws IndexUnreachableException
     * @throws PresentationException 
     */
    public List<String> getPossibleAccessConditions() throws IndexUnreachableException, PresentationException {
        
        List<String> accessConditions = SearchHelper.getFacetValues(SolrConstants.ACCESSCONDITION + ":[* TO *]", SolrConstants.ACCESSCONDITION, 0);
        Collections.sort(accessConditions);
        return accessConditions;
    }

}
