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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.inject.Named;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.util.CacheUtils;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.EmailValidator;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSPageTemplate;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;
import io.goobi.viewer.model.security.user.UserTools;

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
    private String emailConfirmation = "";
    private boolean deleteUserContributions =
            EmailValidator.validateEmailAddress(DataManager.getInstance().getConfiguration().getAnonymousUserEmailAddress()) ? false : true;

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
        lazyModelComments.setFilters("text_owner-nickName_owner-email");
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
        if (currentUser.getNickName() != null) {
            currentUser.setNickName(currentUser.getNickName().trim());
        }
        User nicknameOwner = DataManager.getInstance().getDao().getUserByNickname(currentUser.getNickName()); // This basically resets all changes
        if (nicknameOwner != null && nicknameOwner.getId() != currentUser.getId()) {
            Messages.error(ViewerResourceBundle.getTranslation("user_nicknameTaken", null).replace("{0}", currentUser.getNickName().trim()));
            currentUser = copy;
            currentUser.setNickName(copy.getCopy().getNickName());
            return "";
        }
        currentUser = copy;
        if (getCurrentUser().getId() != null) {
            // Existing user
            if (StringUtils.isNotEmpty(passwordOne) || StringUtils.isNotEmpty(passwordTwo)) {
                if (!passwordOne.equals(passwordTwo)) {
                    Messages.error("user_passwordMismatch");
                    return "";
                }
                currentUser.setNewPassword(passwordOne);
            }
            if (DataManager.getInstance().getDao().updateUser(getCurrentUser())) {
                Messages.info("user_saveSuccess");
            } else {
                Messages.error("errSave");
                return "";
            }
        } else {
            // New user
            if (DataManager.getInstance().getDao().getUserByEmail(currentUser.getEmail()) != null) {
                // Do not allow the same email address being used for multiple users
                Messages.error("newUserExist");
                logger.debug("User account already exists for '" + currentUser.getEmail() + "'.");
                return "";
            }
            if (StringUtils.isEmpty(passwordOne) || StringUtils.isEmpty(passwordTwo)) {
                Messages.error("newUserPasswordOneRequired");
                return "";
            } else if (!passwordOne.equals(passwordTwo)) {
                Messages.error("user_passwordMismatch");
                return "";
            } else {
                getCurrentUser().setNewPassword(passwordOne);

            }
            if (DataManager.getInstance().getDao().addUser(getCurrentUser())) {
                Messages.info("newUserCreated");
            } else {
                Messages.info("errSave");
                return "";
            }
        }
        setCurrentUser(null);

        return "pretty:adminUsers";
    }

    /**
     * <p>
     * deleteUserAction.
     * </p>
     *
     * @param user User to be deleted
     * @param deleteContributions If true, all content created by this user will also be deleted
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
        } else if (EmailValidator.validateEmailAddress(DataManager.getInstance().getConfiguration().getAnonymousUserEmailAddress())) {
            // Move all public content to an anonymous user
            if (!UserTools.anonymizeUserPublicContributions(user)) {
                Messages.error("deleteFailure");
                return "";
            }
        } else {
            logger.error("Anonymous user e-mail address not configured.");
            Messages.error("deleteFailure");
            return "";
        }

        // Finally, delete user (and any user-created data that's not publicly visible)
        if (UserTools.deleteUser(user)) {
            Messages.info("deletedSuccessfully");
            return "pretty:adminUsers";
        }

        Messages.error("deleteFailure");
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
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveUserGroupAction() throws DAOException {
        if (currentUserGroup == null) {
            return "pretty:adminGroups";
        }

        if (getCurrentUserGroup().getId() != null) {
            if (DataManager.getInstance().getDao().updateUserGroup(getCurrentUserGroup())) {
                Messages.info("updatedSuccessfully");
            } else {
                Messages.info("errSave");
                return "pretty:adminGroupEdit";
            }
        } else {
            if (DataManager.getInstance().getDao().addUserGroup(getCurrentUserGroup())) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info("errSave");
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
        // If this the user group is not yet persisted, add it to DB first
        if (currentUserRole.getUserGroup() != null && currentUserRole.getUserGroup().getId() == null) {
            if (!DataManager.getInstance().getDao().addUserGroup(currentUserRole.getUserGroup())) {
                Messages.info("errSave");
                return;
            }
        }
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
     * @return Two SelectItemGroups for core and regular license types
     * @throws DAOException
     * @should group license types in select item groups correctly
     */
    public List<SelectItem> getGroupedLicenseTypeSelectItems() throws DAOException {
        List<LicenseType> licenseTypes = getAllLicenseTypes();
        if (licenseTypes.isEmpty()) {
            return Collections.emptyList();
        }

        List<LicenseType> list1 = new ArrayList<>();
        List<LicenseType> list2 = new ArrayList<>();
        for (LicenseType licenseType : licenseTypes) {
            if (licenseType.isCore()) {
                list1.add(licenseType);
            } else {
                list2.add(licenseType);
            }
        }

        List<SelectItem> ret = new ArrayList<>(licenseTypes.size());
        {
            SelectItemGroup group1 = new SelectItemGroup(ViewerResourceBundle.getTranslation("admin__license_function", null));
            SelectItem[] array1 = new SelectItem[list1.size()];
            for (int i = 0; i < array1.length; ++i) {
                array1[i] = new SelectItem(list1.get(i), ViewerResourceBundle.getTranslation(list1.get(i).getName(), null));
            }
            group1.setSelectItems(array1);
            ret.add(group1);
        }
        {
            SelectItemGroup group2 = new SelectItemGroup(ViewerResourceBundle.getTranslation("admin__license", null));
            SelectItem[] array2 = new SelectItem[list2.size()];
            for (int i = 0; i < array2.length; ++i) {
                array2[i] = new SelectItem(list2.get(i), ViewerResourceBundle.getTranslation(list2.get(i).getName(), null));
            }
            group2.setSelectItems(array2);
            ret.add(group2);
        }

        return ret;
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
     * getAllCoreLicenseTypes.
     * </p>
     *
     * @return all license types in the database where core=true
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllCoreLicenseTypes() throws DAOException {
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
     * Returns all existing non-core license types minus <code>currentLicenseType</code>. Used for overriding license type selection.
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
    public String saveCurrentLicenseTypeAction() throws DAOException {
        if (currentLicenseType == null) {
            Messages.error("errSave");
            return "licenseTypes";
        }

        // Adopt changes made to the privileges
        if (!currentLicenseType.getPrivileges().equals(currentLicenseType.getPrivilegesCopy())) {
            logger.trace("Saving changes to privileges");
            currentLicenseType.setPrivileges(new HashSet<>(currentLicenseType.getPrivilegesCopy()));
        }

        if (currentLicenseType.getId() != null) {
            if (DataManager.getInstance().getDao().updateLicenseType(currentLicenseType)) {
                logger.trace("License type '{}' updated successfully", currentLicenseType.getName());
                Messages.info("updatedSuccessfully");
            } else {
                Messages.error("errSave");
                return "pretty:adminLicenseEdit";
            }
        } else {
            if (DataManager.getInstance().getDao().addLicenseType(currentLicenseType)) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.error("errSave");
                return "pretty:adminLicenseNew";
            }
        }

        return "pretty:adminLicenses";
    }

    /**
     * <p>
     * deleteLicenseTypeAction.
     * </p>
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteLicenseTypeAction(LicenseType licenseType) throws DAOException {
        if (licenseType == null) {
            return "";
        }

        if (DataManager.getInstance().getDao().deleteLicenseType(licenseType)) {
            Messages.info("deletedSuccessfully");

        } else {
            Messages.error("deleteFailure");
        }

        return licenseType.isCore() ? "pretty:adminRoles" : "pretty:adminLicenseTypes";
    }

    /**
     * <p>
     * newCurrentLicenseTypeAction.
     * </p>
     */
    public void newCurrentLicenseTypeAction(String name) {
        logger.trace("newCurrentLicenseTypeAction({})", name);
        currentLicenseType = new LicenseType(name);
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

    // License

    public List<License> getAllLicenses() throws DAOException {
        return DataManager.getInstance().getDao().getAllLicenses();
    }

    /**
     * 
     * @param licenseType
     * @return true if at least one license uses the given license type; false otherwise
     * @throws DAOException
     */
    public boolean isLicenseTypeInUse(LicenseType licenseType) throws DAOException {
        if (licenseType == null) {
            return false;
        }

        return DataManager.getInstance().getDao().getLicenseCount(licenseType) > 0;
    }

    /**
     * 
     * @param licenseType
     * @return
     * @throws DAOException 
     */
    public List<License> getLicenses(LicenseType licenseType) throws DAOException {
        return DataManager.getInstance().getDao().getLicenses(licenseType);
    }

    // IpRange

    /**
     * 
     * @return all IpRanges from the database
     * @throws DAOException
     */
    public List<IpRange> getAllIpRanges() throws DAOException {
        return DataManager.getInstance().getDao().getAllIpRanges();
    }

    /**
     * <p>
     * saveIpRangeAction.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveIpRangeAction() throws DAOException {
        // String name = getCurrentIpRange().getName();
        if (getCurrentIpRange().getId() != null) {
            if (DataManager.getInstance().getDao().updateIpRange(getCurrentIpRange())) {
                Messages.info("updatedSuccessfully");
            } else {
                Messages.info("errSave");
                return "pretty:adminIpRangeEdit";
            }
        } else {
            if (DataManager.getInstance().getDao().addIpRange(getCurrentIpRange())) {
                Messages.info("addedSuccessfully");
            } else {
                Messages.info("errSave");
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
     * Creates <code>currentLicense</code> to a new instance.
     * </p>
     */
    public void newCurrentLicenseAction() {
        logger.trace("newCurrentLicenseAction");
        setCurrentLicense(new License());
    }

    /**
     * Adds the current License to the licensee (User, UserGroup or IpRange). It is imperative that the licensee object is refreshed after updating so
     * that a new license object is an ID attached. Otherwise the list of licenses will throw an NPE!
     *
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String saveCurrentLicenseAction() throws DAOException, IndexUnreachableException, PresentationException {
        logger.trace("saveCurrentLicenseAction");
        if (currentLicense == null) {
            throw new IllegalArgumentException("license may not be null");
        }

        // Sync changes made to the privileges
        if (!currentLicense.getPrivileges().equals(currentLicense.getPrivilegesCopy())) {
            logger.trace("Saving changes to privileges");
            currentLicense.setPrivileges(new HashSet<>(currentLicense.getPrivilegesCopy()));
        }
        // Sync changes made to allowed subthemes
        if (currentLicense.getSelectableSubthemes() != null && !currentLicense.getSelectableSubthemes().isEmpty()) {
            currentLicense.getSubthemeDiscriminatorValues().clear();
            for (Selectable<String> selectable : currentLicense.getSelectableSubthemes()) {
                if (selectable.isSelected()) {
                    currentLicense.getSubthemeDiscriminatorValues().add(selectable.getValue());
                }
            }
        }
        // Sync changes made to allowed categories
        if (currentLicense.getSelectableCategories() != null && !currentLicense.getSelectableCategories().isEmpty()) {
            currentLicense.getAllowedCategories().clear();
            for (Selectable<CMSCategory> selectable : currentLicense.getSelectableCategories()) {
                if (selectable.isSelected()) {
                    currentLicense.getAllowedCategories().add(selectable.getValue());
                }
            }
        }
        // Sync changes made to allowed templates
        if (currentLicense.getSelectableTemplates() != null && !currentLicense.getSelectableTemplates().isEmpty()) {
            currentLicense.getAllowedCmsTemplates().clear();
            for (Selectable<CMSPageTemplate> selectable : currentLicense.getSelectableTemplates()) {
                if (selectable.isSelected()) {
                    currentLicense.getAllowedCmsTemplates().add(selectable.getValue().getId());
                }
            }
        }

        boolean error = false;
        if (currentLicense.getUser() != null) {
            // User
            currentLicense.getUser().addLicense(currentLicense);
            if (DataManager.getInstance().getDao().updateUser(currentLicense.getUser())) {
                Messages.info("license_licenseSaveSuccess");
            } else {
                Messages.error("license_licenseSaveFailure");
                error = true;
            }
        } else if (currentLicense.getUserGroup() != null) {
            // UserGroup
            currentLicense.getUserGroup().addLicense(currentLicense);
            if (DataManager.getInstance().getDao().updateUserGroup(currentLicense.getUserGroup())) {
                Messages.info("license_licenseSaveSuccess");
            } else {
                Messages.error("license_licenseSaveFailure");
                error = true;
            }
        } else if (currentLicense.getIpRange() != null) {
            // IpRange
            logger.trace("ip range id:{} ", currentLicense.getIpRange().getId());
            currentLicense.getIpRange().addLicense(currentLicense);
            if (DataManager.getInstance().getDao().updateIpRange(currentLicense.getIpRange())) {
                Messages.info("license_licenseSaveSuccess");
            } else {
                Messages.error("license_licenseSaveFailure");
                error = true;
            }
        } else {
            logger.trace("nothing");
            Messages.error("license_licenseSaveFailure");
            error = true;
        }

        if (error) {
            if (currentLicense.getId() != null) {
                return "pretty:adminRightsEdit";
            }
            return "pretty:adminRightsNew";
        }

        return "pretty:adminRights";
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
        logger.debug("removing license: {}", license.getLicenseType().getName());
        if (license.getUser() != null) {
            license.getUser().removeLicense(license);
            success = DataManager.getInstance().getDao().updateUser(license.getUser());
        } else if (license.getUserGroup() != null) {
            license.getUserGroup().removeLicense(license);
            success = DataManager.getInstance().getDao().updateUserGroup(license.getUserGroup());
        } else if (license.getIpRange() != null) {
            license.getIpRange().removeLicense(license);
            success = DataManager.getInstance().getDao().updateIpRange(license.getIpRange());
        }

        if (success) {
            Messages.info("license_deleteSuccess");
        } else {
            Messages.error("license_deleteFailure");
        }

        return "pretty:adminRights";
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
     * Returns the user ID of <code>currentUser/code>.
     * 
     * return <code>currentUser.id</code> if loaded and has ID; null if not
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
     * @param id
     * @throws DAOException
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
     * return <code>currentUserGroup.id</code> if loaded and has ID; null if not
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
     * @param id
     * @throws DAOException
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
            logger.trace("setCurrentLicenseType: {}", currentLicenseType.getName());
            // Prepare privileges working copy (but only if the same license type is not already set)
            if (!currentLicenseType.equals(this.currentLicenseType)) {
                currentLicenseType.setPrivilegesCopy(new HashSet<>(currentLicenseType.getPrivileges()));
            }
        }
        this.currentLicenseType = currentLicenseType;
    }

    /**
     * Returns the user ID of <code>currentLicenseType/code>.
     * 
     * return <code>currentLicenseType.id</code> if loaded and has ID; null if not
     */
    public Long getCurrentLicenseTypeId() {
        if (currentLicenseType != null && currentLicenseType.getId() != null) {
            return currentLicenseType.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentUserGroup</code> by loading it from the DB via the given ID.
     * 
     * @param id
     * @throws DAOException
     */
    public void setCurrentLicenseTypeId(Long id) throws DAOException {
        setCurrentLicenseType(DataManager.getInstance().getDao().getLicenseType(id));
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
        if (currentLicense != null) {
            logger.trace("setCurrentLicense: {}", currentLicense.toString());
            // Prepare privileges working copy (but only if the same license is not already set)
            currentLicense.resetTempData();
            if (!currentLicense.equals(this.currentLicense)) {
                currentLicense.setPrivilegesCopy(new HashSet<>(currentLicense.getPrivileges()));
            }
        }
        this.currentLicense = currentLicense;
    }

    /**
     * Returns the user ID of <code>currentLicense/code>.
     * 
     * return <code>currentLicense.id</code> if loaded and has ID; null if not
     */
    public Long getCurrentLicenseId() {
        if (currentLicense != null && currentLicense.getId() != null) {
            return currentLicense.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentLicense/code> by loading it from the DB via the given ID.
     * 
     * @param id
     * @throws DAOException
     */
    public void setCurrentLicenseId(Long id) throws DAOException {
        if (ObjectUtils.notEqual(getCurrentLicenseId(), id)) {
            setCurrentLicense(DataManager.getInstance().getDao().getLicense(id));
        }
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
     * return <code>currentIpRange.id</code> if loaded and has ID; null if not
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
     * @param id
     * @throws DAOException
     */
    public void setCurrentIpRangeId(Long id) throws DAOException {
        this.currentIpRange = DataManager.getInstance().getDao().getIpRange(id);
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
     * @return the emailConfirmation
     */
    public String getEmailConfirmation() {
        return emailConfirmation;
    }

    /**
     * @param emailConfirmation the emailConfirmation to set
     */
    public void setEmailConfirmation(String emailConfirmation) {
        this.emailConfirmation = emailConfirmation;
    }

    /**
     * @return the deleteUserContributions
     */
    public boolean isDeleteUserContributions() {
        return deleteUserContributions;
    }

    /**
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
            String metsFilePath = DataFileTools.getSourceFilePath(pi + ".xml", dataRepository, SolrConstants._METS);
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
     * Queries Solr for a list of all values of the set ACCESSCONDITION
     *
     * @return A list of all indexed ACCESSCONDITIONs
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public List<String> getPossibleAccessConditions() throws IndexUnreachableException, PresentationException {
        List<String> accessConditions = SearchHelper.getFacetValues(
                "+" + SolrConstants.ACCESSCONDITION + ":[* TO *] -" + SolrConstants.ACCESSCONDITION + ":" + SolrConstants.OPEN_ACCESS_VALUE,
                SolrConstants.ACCESSCONDITION, 1);
        Collections.sort(accessConditions);
        return accessConditions;
    }

    /**
     * 
     * @return List of access condition values that have no corresponding license type in the database
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public List<String> getNotConfiguredAccessConditions() throws IndexUnreachableException, PresentationException, DAOException {
        List<String> accessConditions = getPossibleAccessConditions();
        if (accessConditions.isEmpty()) {
            return Collections.emptyList();
        }

        List<LicenseType> licenseTypes = getAllLicenseTypes();
        if (licenseTypes.isEmpty()) {
            return accessConditions;
        }

        List<String> ret = new ArrayList<>();
        for (String accessCondition : accessConditions) {
            boolean found = false;
            for (LicenseType licenseType : licenseTypes) {
                if (licenseType.getName().equals(accessCondition)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ret.add(accessCondition);
            }
        }

        return ret;

    }

    /**
     * 
     * @param accessCondition
     * @return Number of records containing the given access condition value
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public long getNumRecordsWithAccessCondition(String accessCondition) throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance()
                .getSearchIndex()
                .getHitCount(SolrSearchIndex.getQueryForAccessCondition(accessCondition, false));
    }

    /**
     * 
     * @param accessCondition
     * @return
     */
    public String getUrlQueryForAccessCondition(String accessCondition) {
        String query = SolrSearchIndex.getQueryForAccessCondition(accessCondition, true);
        try {
            return URLEncoder.encode(query, StringTools.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return query;
        }
    }

    public void triggerMessage(String message) {
        logger.debug("Show message " + message);
        Messages.info(ViewerResourceBundle.getTranslation(message, null));
    }

    /**
     * 
     * @param privilege
     * @return
     */
    public String getMessageKeyForPrivilege(String privilege) {
        return "license_priv_" + privilege.toLowerCase();
    }
}
