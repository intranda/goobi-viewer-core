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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.util.CacheUtils;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;

/**
 * Administration backend functions.
 */
@Named
@SessionScoped
public class AdminBean implements Serializable {

    private static final long serialVersionUID = -8334669036711935331L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(AdminBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    private TableDataProvider<User> lazyModelUsers;
    private TableDataProvider<UserGroup> lazyModelUserGroups;
    private TableDataProvider<LicenseType> lazyModelLicenseTypes;
    private TableDataProvider<LicenseType> lazyModelCoreLicenseTypes;
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
     *
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
        lazyModelUsers.setFilters("firstName_lastName_nickName_email");

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

        // License types
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

        // Core license types
        lazyModelCoreLicenseTypes = new TableDataProvider<>(new TableDataSource<LicenseType>() {

            @Override
            public List<LicenseType> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                if (StringUtils.isEmpty(sortField)) {
                    sortField = "name";
                }
                try {
                    return DataManager.getInstance().getDao().getCoreLicenseTypes(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                try {
                    return DataManager.getInstance().getDao().getCoreLicenseTypeCount(filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage(), e);
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
            }
        });
        lazyModelCoreLicenseTypes.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelCoreLicenseTypes.setFilters("name");

        // IP ranges
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
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getAllUsers() throws DAOException {
        return DataManager.getInstance().getDao().getAllUsers(true);
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
     * saveUserAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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
     * <p>
     * deleteUserAction.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteUserAction(User user) throws DAOException {
        logger.debug("Deleting user: " + user.getDisplayName());
        if (DataManager.getInstance().getDao().deleteUser(user)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }
    }

    /**
     * <p>
     * resetCurrentUserAction.
     * </p>
     */
    public void resetCurrentUserAction() {
        currentUser = new User();
    }

    /**
     * Persists changes in <code>currentUserGroup</code>.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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
     * <p>
     * deleteUserGroupAction.
     * </p>
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteUserGroupAction(UserGroup userGroup) throws DAOException {
        if (DataManager.getInstance().getDao().deleteUserGroup(userGroup)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
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
     * <p>
     * deleteRoleAction.
     * </p>
     *
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteRoleAction(Role role) throws DAOException {
        if (DataManager.getInstance().getDao().deleteRole(role)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
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
        currentUserRole = new UserRole(getCurrentUserGroup(), null, null);
    }

    /**
     * <p>
     * saveUserRoleAction.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveUserRoleAction() throws DAOException {
        if (currentUserRole == null) {
            logger.trace("currentUserRole not set");
            return;
        }

        logger.trace("saveUserRoleAction: {}, {}, {}", currentUserRole.getUserGroup(), currentUserRole.getUser(), currentUserRole);
        if (getCurrentUserRole().getId() != null) {
            // existing
            if (DataManager.getInstance().getDao().updateUserRole(getCurrentUserRole())) {
                Messages.info("userGroup_membershipUpdateSuccess");
            } else {
                Messages.error("userGroup_membershipUpdateFailure");
            }
        } else {
            // new
            if (DataManager.getInstance().getDao().addUserRole(currentUserRole)) {
                Messages.info("userGroup_memberAddSuccess");
            } else {
                Messages.error("userGroup_memberAddFailure");
            }
        }
        setCurrentUserRole(null);
    }

    /**
     * <p>
     * deleteUserRoleAction.
     * </p>
     *
     * @param userRole a {@link io.goobi.viewer.model.security.user.UserRole} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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
     *
     * @return all license types in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllLicenseTypes() throws DAOException {
        return DataManager.getInstance().getDao().getAllLicenseTypes();
    }

    /**
     * 
     * @param core
     * @return all license types in the database where this.core=core
     * @throws DAOException
     */
    private List<LicenseType> getFilteredLicenseTypes(boolean core) throws DAOException {
        List<LicenseType> all = getAllLicenseTypes();
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        List<LicenseType> ret = new ArrayList<>(all.size());
        for (LicenseType lt : all) {
            if (lt.isCore() == core) {
                ret.add(lt);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getAllRoleLicenseTypes.
     * </p>
     *
     * @return all license types in the database where core=true
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllRoleLicenseTypes() throws DAOException {
        return getFilteredLicenseTypes(true);
    }

    /**
     * <p>
     * getAllRecordLicenseTypes.
     * </p>
     *
     * @return all license types in the database where core=false
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllRecordLicenseTypes() throws DAOException {
        return getFilteredLicenseTypes(false);
    }

    /**
     * Returns all existing non-core license types minus this one. Required for admin tabs.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getOtherLicenseTypes() throws DAOException {
        List<LicenseType> all = DataManager.getInstance().getDao().getAllLicenseTypes();
        if (all.isEmpty() || all.get(0).equals(this.currentLicenseType)) {
            return Collections.emptyList();
        }

        List<LicenseType> ret = new ArrayList<>(all.size() - 1);
        for (LicenseType licenseType : all) {
            if (licenseType.equals(this.currentLicenseType) || licenseType.isCore()) {
                continue;
            }
            ret.add(licenseType);
        }

        return ret;
    }

    /**
     * <p>
     * saveLicenseTypeAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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
     * <p>
     * deleteLicenseTypeAction.
     * </p>
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteLicenseTypeAction(LicenseType licenseType) throws DAOException {
        if (DataManager.getInstance().getDao().deleteLicenseType(licenseType)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
        }
    }

    /**
     * <p>
     * resetCurrentLicenseTypeAction.
     * </p>
     */
    public void resetCurrentLicenseTypeAction() {
        logger.trace("resetCurrentLicenseTypeAction");
        currentLicenseType = new LicenseType();
    }

    /**
     * <p>
     * resetCurrentRoleLicenseAction.
     * </p>
     */
    public void resetCurrentRoleLicenseAction() {
        currentLicenseType = new LicenseType();
        currentLicenseType.setCore(true);
    }

    // IpRange

    /**
     * <p>
     * saveIpRangeAction.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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
     * <p>
     * deleteIpRangeAction.
     * </p>
     *
     * @param ipRange a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteIpRangeAction(IpRange ipRange) throws DAOException {
        if (DataManager.getInstance().getDao().deleteIpRange(ipRange)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error("deleteFailure");
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

    /**
     * <p>
     * resetCurrentLicenseAction.
     * </p>
     */
    public void resetCurrentLicenseAction() {
        logger.trace("resetCurrentLicenseAction");
        setCurrentLicense(null);
    }

    /**
     * <p>
     * resetCurrentLicenseForUserAction.
     * </p>
     */
    public void resetCurrentLicenseForUserAction() {
        logger.trace("resetCurrentLicenseForUserAction");
        currentLicense = new License();
        currentLicense.setUser(getCurrentUser());
    }

    /**
     * <p>
     * resetCurrentLicenseForUserGroupAction.
     * </p>
     */
    public void resetCurrentLicenseForUserGroupAction() {
        logger.trace("resetCurrentLicenseForUserGroupAction");
        currentLicense = new License();
        currentLicense.setUserGroup(getCurrentUserGroup());
    }

    /**
     * <p>
     * resetCurrentLicenseForIpRangeAction.
     * </p>
     */
    public void resetCurrentLicenseForIpRangeAction() {
        logger.trace("resetCurrentLicenseForIpRangeAction");
        currentLicense = new License();
        currentLicense.setIpRange(getCurrentIpRange());
    }

    /**
     * <p>
     * saveCurrentLicenseAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
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
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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

    /**
     * <p>
     * deleteLicenseAction.
     * </p>
     *
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
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

    /**
     * <p>
     * saveCommentAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.Comment} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
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

    /**
     * <p>
     * deleteCommentAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.Comment} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteCommentAction(Comment comment) throws DAOException {
        if (DataManager.getInstance().getDao().deleteComment(comment)) {
            Messages.info("commentDeleteSuccess");
        } else {
            Messages.error("commentDeleteFailure");
        }

        return "";
    }

    /**
     * <p>
     * resetCurrentCommentAction.
     * </p>
     */
    public void resetCurrentCommentAction() {
        currentComment = null;
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
     * Getter for the field <code>currentLicenseType</code>.
     * </p>
     *
     * @return the currentLicenseType
     */
    public LicenseType getCurrentLicenseType() {
        return currentLicenseType;
    }

    /**
     * <p>
     * Setter for the field <code>currentLicenseType</code>.
     * </p>
     *
     * @param currentLicenseType the currentLicenseType to set
     */
    public void setCurrentLicenseType(LicenseType currentLicenseType) {
        if (currentLicenseType != null) {
            logger.debug("setCurrentLicenseType: " + currentLicenseType.getName());
        }
        this.currentLicenseType = currentLicenseType;
    }

    /**
     * <p>
     * Getter for the field <code>currentLicense</code>.
     * </p>
     *
     * @return the currentLicense
     */
    public License getCurrentLicense() {
        return currentLicense;
    }

    /**
     * <p>
     * Setter for the field <code>currentLicense</code>.
     * </p>
     *
     * @param currentLicense the currentLicense to set
     */
    public void setCurrentLicense(License currentLicense) {
        logger.trace("setCurrentLicense: {}", currentLicense);
        this.currentLicense = currentLicense;
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
     * <p>
     * Getter for the field <code>currentComment</code>.
     * </p>
     *
     * @return the currentComment
     */
    public Comment getCurrentComment() {
        return currentComment;
    }

    /**
     * <p>
     * Setter for the field <code>currentComment</code>.
     * </p>
     *
     * @param currentComment the currentComment to set
     */
    public void setCurrentComment(Comment currentComment) {
        this.currentComment = currentComment;
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
     * Getter for the field <code>lazyModelUserGroups</code>.
     * </p>
     *
     * @return the lazyModelUserGroups
     */
    public TableDataProvider<UserGroup> getLazyModelUserGroups() {
        return lazyModelUserGroups;
    }

    /**
     * <p>
     * getPageUserGroups.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<UserGroup> getPageUserGroups() {
        return lazyModelUserGroups.getPaginatorList();
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelLicenseTypes</code>.
     * </p>
     *
     * @return the lazyModelLicenseTypes
     */
    public TableDataProvider<LicenseType> getLazyModelLicenseTypes() {
        return lazyModelLicenseTypes;
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelCoreLicenseTypes</code>.
     * </p>
     *
     * @return the lazyModelCoreLicenseTypes
     */
    public TableDataProvider<LicenseType> getLazyModelCoreLicenseTypes() {
        return lazyModelCoreLicenseTypes;
    }

    /**
     * <p>
     * getPageLicenseTypes.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<LicenseType> getPageLicenseTypes() {
        return lazyModelLicenseTypes.getPaginatorList();
    }

    /**
     * <p>
     * getPageCoreLicenseTypes.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<LicenseType> getPageCoreLicenseTypes() {
        return lazyModelCoreLicenseTypes.getPaginatorList();
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelIpRanges</code>.
     * </p>
     *
     * @return the lazyModelIpRanges
     */
    public TableDataProvider<IpRange> getLazyModelIpRanges() {
        return lazyModelIpRanges;
    }

    /**
     * <p>
     * getPageIpRanges.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<IpRange> getPageIpRanges() {
        return lazyModelIpRanges.getPaginatorList();
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelComments</code>.
     * </p>
     *
     * @return the lazyModelComments
     */
    public TableDataProvider<Comment> getLazyModelComments() {
        return lazyModelComments;
    }

    /**
     * <p>
     * getPageComments.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<Comment> getPageComments() {
        return lazyModelComments.getPaginatorList();
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
     * deleteFromCache.
     * </p>
     *
     * @param identifiers a {@link java.util.List} object.
     * @param fromContentCache a boolean.
     * @param fromThumbnailCache a boolean.
     * @return a int.
     */
    public int deleteFromCache(List<String> identifiers, boolean fromContentCache, boolean fromThumbnailCache) {
        return CacheUtils.deleteFromCache(identifiers, fromContentCache, fromThumbnailCache);
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
     */
    public int deleteFromCache(List<String> identifiers, boolean fromContentCache, boolean fromThumbnailCache, boolean fromPdfCache) {
        return CacheUtils.deleteFromCache(identifiers, fromContentCache, fromThumbnailCache, fromPdfCache);
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
            String metsFilePath = Helper.getSourceFilePath(pi + ".xml", dataRepository, SolrConstants._METS);
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

                XmlTools.writeXmlFile(doc, DataManager.getInstance().getConfiguration().getHotfolder() + File.separator + pi + ".xml");

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
     * Querys solr for a list of all values of the set ACCESSCONDITION
     *
     * @return A list of all indexed ACCESSCONDITIONs
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public List<String> getPossibleAccessConditions() throws IndexUnreachableException, PresentationException {

        List<String> accessConditions = SearchHelper.getFacetValues(SolrConstants.ACCESSCONDITION + ":[* TO *]", SolrConstants.ACCESSCONDITION, 0);
        Collections.sort(accessConditions);
        return accessConditions;
    }
    
    public void triggerMessage(String message) {
        logger.debug("Show message " + message);
        Messages.info(Helper.getTranslation(message, null));

    }
}
