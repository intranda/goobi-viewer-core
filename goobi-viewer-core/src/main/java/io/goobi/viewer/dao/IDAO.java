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
package io.goobi.viewer.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.administration.MaintenanceMode;
import io.goobi.viewer.model.administration.legal.CookieBanner;
import io.goobi.viewer.model.administration.legal.Disclaimer;
import io.goobi.viewer.model.administration.legal.TermsOfUse;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.cms.CMSArchiveConfig;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.HighlightData;
import io.goobi.viewer.model.cms.collections.CMSCollection;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.recordnotes.CMSMultiRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSSingleRecordNote;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordPageStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.job.ITaskType;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.quartz.RecurringTaskTrigger;
import io.goobi.viewer.model.job.upload.UploadJob;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.security.ILicensee;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.tickets.AccessTicket;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;
import io.goobi.viewer.model.statistics.usage.DailySessionUsageStatistics;
import io.goobi.viewer.model.transkribus.TranskribusJob;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.themes.ThemeConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;

/**
 * Central data-access interface for the Goobi viewer, defining all persistence operations for the application's domain objects.
 * Covers users, user groups, roles, licenses, CMS pages and components, crowdsourcing campaigns, bookmarks, annotations, search history,
 * geo-maps, upload jobs, message-queue entries, usage statistics, and more.
 */
public interface IDAO {

    /**
     * tableExists.
     *
     * @param tableName name of the database table to check
     * @return true if the table exists; false otherwise
     * @throws java.sql.SQLException if any.
     * @throws DAOException
     */
    boolean tableExists(String tableName) throws DAOException, SQLException;

    /**
     * columnsExists.
     *
     * @param tableName name of the database table to check
     * @param columnName name of the column to check for existence
     * @return true if the column exists in the table; false otherwise
     * @throws java.sql.SQLException if any.
     */
    boolean columnsExists(String tableName, String columnName) throws DAOException, SQLException;

    // User

    /**
     * getAllUsers.
     *
     * @param refresh true to bypass cache and reload from database
     * @return list of all users in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getAllUsers(boolean refresh) throws DAOException;

    /**
     * getUserCount.
     *
     * @param filters map of field names to filter values
     * @return total number of users matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserCount(Map<String, String> filters) throws DAOException;

    /**
     * getUsers.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of users matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getUsers(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    /**
     * 
     * @param propertyName name of the user property to filter by
     * @param propertyValue value of the user property to filter by
     * @return List if users with matching property name/value pair
     * @throws DAOException
     */
    public List<User> getUsersByPropertyValue(String propertyName, String propertyValue) throws DAOException;

    /**
     * 
     * @return List<User> where User.superuser == true
     * @throws DAOException
     */
    public List<User> getAdminUsers() throws DAOException;

    /**
     * getUser.
     *
     * @param id database primary key of the user
     * @return the matching user, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUser(long id) throws DAOException;

    /**
     * getUserByEmail.
     *
     * @param email email address of the user to look up
     * @return the matching user, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByEmail(String email) throws DAOException;

    /**
     * getUserByOpenId.
     *
     * @param identifier OpenID identifier of the user to look up
     * @return the matching user, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByOpenId(String identifier) throws DAOException;

    /**
     * getUserByNickname.
     *
     * @param nickname display name of the user to look up
     * @return the matching user, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByNickname(String nickname) throws DAOException;

    /**
     * addUser.
     *
     * @param user user to persist
     * @return true if user was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUser(User user) throws DAOException;

    /**
     * updateUser.
     *
     * @param user user to update in the database
     * @return true if user was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUser(User user) throws DAOException;

    /**
     * deleteUser.
     *
     * @param user user to delete from the database
     * @return true if user was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUser(User user) throws DAOException;

    // UserGroup

    /**
     * getAllUserGroups.
     *
     * @return list of all user groups in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getAllUserGroups() throws DAOException;

    /**
     * getUserGroupCount.
     *
     * @param filters map of field names to filter values
     * @return total number of user groups matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserGroupCount(Map<String, String> filters) throws DAOException;

    /**
     * getUserGroups.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of user groups matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getUserGroups(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getUserGroups.
     *
     * @param owner user who owns the groups to retrieve
     * @return list of user groups owned by the given user
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getUserGroups(User owner) throws DAOException;

    /**
     * getUserGroup.
     *
     * @param id database primary key of the user group
     * @return the matching user group, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public UserGroup getUserGroup(long id) throws DAOException;

    /**
     * getUserGroup.
     *
     * @param name name of the user group to look up
     * @return the matching user group, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public UserGroup getUserGroup(String name) throws DAOException;

    /**
     * addUserGroup.
     *
     * @param userGroup user group to persist
     * @return true if user group was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUserGroup(UserGroup userGroup) throws DAOException;

    /**
     * updateUserGroup.
     *
     * @param userGroup user group to update in the database
     * @return true if user group was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUserGroup(UserGroup userGroup) throws DAOException;

    /**
     * deleteUserGroup.
     *
     * @param userGroup user group to delete from the database
     * @return true if user group was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUserGroup(UserGroup userGroup) throws DAOException;

    // Bookmarks

    /**
     * getAllBookmarkLists.
     *
     * @return list of all bookmark lists in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getAllBookmarkLists() throws DAOException;

    /**
     * getPublicBookmarkLists.
     *
     * @return list of all publicly visible bookmark lists
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getPublicBookmarkLists() throws DAOException;

    /**
     * getBookmarkLists.
     *
     * @param user owner of the bookmark lists to retrieve
     * @return list of bookmark lists owned by the given user
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getBookmarkLists(User user) throws DAOException;

    /**
     * Gets number of bookmark lists owned by the given user.
     *
     * @param user owner of the bookmark lists
     * @return number of owned bookmark lists
     * @throws DAOException
     */
    long getBookmarkListCount(User user) throws DAOException;

    /**
     * getBookmarkList.
     *
     * @param id database primary key of the bookmark list
     * @return the matching bookmark list, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkList(long id) throws DAOException;

    /**
     * getBookmarkList.
     *
     * @param name name of the bookmark list to look up
     * @param user owner of the bookmark list
     * @return the matching bookmark list, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkList(String name, User user) throws DAOException;

    /**
     * getBookmarkListByShareKey.
     *
     * @param shareKey share key identifying the bookmark list
     * @return the matching bookmark list, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkListByShareKey(String shareKey) throws DAOException;

    /**
     * addBookmarkList.
     *
     * @param bookmarkList bookmark list to persist
     * @return true if bookmark list was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addBookmarkList(BookmarkList bookmarkList) throws DAOException;

    /**
     * updateBookmarkList.
     *
     * @param bookmarkList bookmark list to update in the database
     * @return true if bookmark list was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateBookmarkList(BookmarkList bookmarkList) throws DAOException;

    /**
     * deleteBookmarkList.
     *
     * @param bookmarkList bookmark list to delete from the database
     * @return true if bookmark list was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteBookmarkList(BookmarkList bookmarkList) throws DAOException;

    // Role

    /**
     * getAllRoles.
     *
     * @return list of all roles in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Role> getAllRoles() throws DAOException;

    /**
     * getRoleCount.
     *
     * @param filters map of field names to filter values
     * @return total number of roles matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getRoleCount(Map<String, String> filters) throws DAOException;

    /**
     * getRoles.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of roles matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Role> getRoles(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    /**
     * getRole.
     *
     * @param id database primary key of the role
     * @return the matching role, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Role getRole(long id) throws DAOException;

    /**
     * getRole.
     *
     * @param name name of the role to look up
     * @return the matching role, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Role getRole(String name) throws DAOException;

    /**
     * addRole.
     *
     * @param role role to persist
     * @return true if role was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addRole(Role role) throws DAOException;

    /**
     * updateRole.
     *
     * @param role role to update in the database
     * @return true if role was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateRole(Role role) throws DAOException;

    /**
     * deleteRole.
     *
     * @param role role to delete from the database
     * @return true if role was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteRole(Role role) throws DAOException;

    // UserRole

    /**
     * getAllUserRoles.
     *
     * @return list of all user role assignments in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserRole> getAllUserRoles() throws DAOException;

    /**
     * getUserRoleCount.
     *
     * @param userGroup user group to filter by, or null to ignore
     * @param user user to filter by, or null to ignore
     * @param role role to filter by, or null to ignore
     * @return Row count
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserRoleCount(UserGroup userGroup, User user, Role role) throws DAOException;

    /**
     * getUserRoles.
     *
     * @param userGroup user group to filter by, or null to ignore
     * @param user user to filter by, or null to ignore
     * @param role role to filter by, or null to ignore
     * @return list of user role assignments matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserRole> getUserRoles(UserGroup userGroup, User user, Role role) throws DAOException;

    /**
     * addUserRole.
     *
     * @param userRole user role assignment to persist
     * @return true if user role was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUserRole(UserRole userRole) throws DAOException;

    /**
     * updateUserRole.
     *
     * @param userRole user role assignment to update in the database
     * @return true if user role was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUserRole(UserRole userRole) throws DAOException;

    /**
     * deleteUserRole.
     *
     * @param userRole user role assignment to delete from the database
     * @return true if user role was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUserRole(UserRole userRole) throws DAOException;

    // LicenseType

    /**
     * getAllLicenseTypes.
     *
     * @return list of all license types in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllLicenseTypes() throws DAOException;

    /**
     * getLicenseTypeCount.
     *
     * @param filters map of field names to filter values
     * @return total number of license types matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getLicenseTypeCount(Map<String, String> filters) throws DAOException;

    /**
     * getCoreLicenseTypeCount.
     *
     * @param filters map of field names to filter values
     * @return total number of core license types matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCoreLicenseTypeCount(Map<String, String> filters) throws DAOException;

    /**
     * getRecordLicenseTypes.
     *
     * @return list of license types that apply at the record level
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getRecordLicenseTypes() throws DAOException;

    /**
     * getLicenseTypes.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of license types matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getCoreLicenseTypes.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of core license types matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getCoreLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getLicenseType.
     *
     * @param id database primary key of the license type
     * @return the matching license type, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public LicenseType getLicenseType(long id) throws DAOException;

    /**
     * getLicenseType.
     *
     * @param name name of the license type to look up
     * @return the matching license type, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public LicenseType getLicenseType(String name) throws DAOException;

    /**
     * Returns all license types that match the given name list.
     *
     * @param names list of license type names to look up
     * @return list of license types whose names are contained in the given list
     * @throws DAOException in case of errors
     */
    public List<LicenseType> getLicenseTypes(List<String> names) throws DAOException;

    /**
     * 
     * @param licenseType license type whose overriding types to retrieve
     * @return List of license types overriding given licenseType
     * @throws DAOException
     */
    public List<LicenseType> getOverridingLicenseType(LicenseType licenseType) throws DAOException;

    /**
     * addLicenseType.
     *
     * @param licenseType license type to persist
     * @return true if license type was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addLicenseType(LicenseType licenseType) throws DAOException;

    /**
     * updateLicenseType.
     *
     * @param licenseType license type to update in the database
     * @return true if license type was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateLicenseType(LicenseType licenseType) throws DAOException;

    /**
     * deleteLicenseType.
     *
     * @param licenseType license type to delete from the database
     * @return true if license type was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteLicenseType(LicenseType licenseType) throws DAOException;

    // License

    /**
     * getAllLicenses.
     *
     * @return list of all licenses in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<License> getAllLicenses() throws DAOException;

    /**
     * getLicense.
     *
     * @param id database primary key of the license
     * @return the matching license, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public License getLicense(Long id) throws DAOException;

    /**
     *
     * @param licenseType license type to filter by
     * @return List of {@link License}s of the given licenseType
     * @throws DAOException
     */
    public List<License> getLicenses(LicenseType licenseType) throws DAOException;

    /**
     *
     * @param licensee licensee (user, group, or IP range) to filter by
     * @return List of {@link License}s for the given licensee
     * @throws DAOException
     */
    public List<License> getLicenses(ILicensee licensee) throws DAOException;

    /**
     * Returns the number of licenses that use the given license type.
     *
     * @param licenseType license type to count licenses for
     * @return Number of existing {@link License}s of the given licenseType
     * @throws DAOException
     */
    public long getLicenseCount(LicenseType licenseType) throws DAOException;

    /**
     * addLicenseType.
     *
     * @param license license to persist
     * @return true if license was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addLicense(License license) throws DAOException;

    /**
     * updateLicenseType.
     *
     * @param license license to update in the database
     * @return true if license was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateLicense(License license) throws DAOException;

    /**
     * deleteLicenseType.
     *
     * @param license license to delete from the database
     * @return true if license was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteLicense(License license) throws DAOException;

    // AccessTicket

    /**
     * 
     * @param id database id of the access ticket
     * @return {@link AccessTicket} with the given id
     * @throws DAOException
     */
    public AccessTicket getTicket(Long id) throws DAOException;

    /**
     * 
     * @param passwordHash password hash of the access ticket
     * @return {@link AccessTicket} with the given passwordHash
     * @throws DAOException
     */
    public AccessTicket getTicketByPasswordHash(String passwordHash) throws DAOException;

    /**
     * getActiveTicketCount.
     *
     * @param filters Selected filters
     * @return Number of found rows
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getActiveTicketCount(Map<String, String> filters) throws DAOException;

    /**
     * getActiveRecordAccessTickets.
     *
     * @param first First row index
     * @param pageSize Number of rows
     * @param sortField field to sort by
     * @param descending true if descending order requested; false otherwise
     * @param filters Selected filters
     * @return list of active access tickets matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<AccessTicket> getActiveTickets(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * 
     * @return {@link AccessTicket}s with the requested status
     * @throws DAOException
     */
    public List<AccessTicket> getTicketRequests() throws DAOException;

    /**
     * addTicket.
     *
     * @param ticket access ticket to persist
     * @return true if ticket was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addTicket(AccessTicket ticket) throws DAOException;

    /**
     * updateTicket.
     *
     * @param ticket access ticket to update in the database
     * @return true if ticket was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateTicket(AccessTicket ticket) throws DAOException;

    /**
     * deleteTicket.
     *
     * @param ticket access ticket to delete from the database
     * @return true if ticket was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteTicket(AccessTicket ticket) throws DAOException;

    // IpRange

    /**
     * getAllIpRanges.
     *
     * @return list of all IP ranges in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<IpRange> getAllIpRanges() throws DAOException;

    /**
     * getIpRangeCount.
     *
     * @param filters map of field names to filter values
     * @return total number of IP ranges matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getIpRangeCount(Map<String, String> filters) throws DAOException;

    /**
     * getIpRanges.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of IP ranges matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<IpRange> getIpRanges(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    /**
     * getIpRange.
     *
     * @param id database primary key of the IP range
     * @return the matching IP range, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public IpRange getIpRange(long id) throws DAOException;

    /**
     * getIpRange.
     *
     * @param name name of the IP range to look up
     * @return the matching IP range, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public IpRange getIpRange(String name) throws DAOException;

    /**
     * addIpRange.
     *
     * @param ipRange IP range to persist
     * @return true if IP range was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addIpRange(IpRange ipRange) throws DAOException;

    /**
     * updateIpRange.
     *
     * @param ipRange IP range to update in the database
     * @return true if IP range was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateIpRange(IpRange ipRange) throws DAOException;

    /**
     * deleteIpRange.
     *
     * @param ipRange IP range to delete from the database
     * @return true if IP range was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteIpRange(IpRange ipRange) throws DAOException;

    // CommentGroup

    /**
     *
     * @return All existing {@link CommentGroup}s
     * @throws DAOException
     */
    public List<CommentGroup> getAllCommentGroups() throws DAOException;

    /**
     *
     * @return {@link CommentGroup}
     * @throws DAOException
     */
    public CommentGroup getCommentGroupUnfiltered() throws DAOException;

    /**
     * getCommentGroup.
     *
     * @param id database primary key of the comment group
     * @return the matching comment group, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CommentGroup getCommentGroup(long id) throws DAOException;

    /**
     * addCommentGroup.
     *
     * @param commentGroup comment group to persist
     * @return true if comment group was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCommentGroup(CommentGroup commentGroup) throws DAOException;

    /**
     * updateCommentGroup.
     *
     * @param commentGroup comment group to update in the database
     * @return true if comment group was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCommentGroup(CommentGroup commentGroup) throws DAOException;

    /**
     * deleteCommentGroup.
     *
     * @param commentGroup comment group to delete from the database
     * @return true if comment group was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCommentGroup(CommentGroup commentGroup) throws DAOException;

    // Comment

    /**
     * getAllComments.
     *
     * @return list of all comments in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getAllComments() throws DAOException;

    /**
     * getCommentCount.
     *
     * @param filters map of field names to filter values
     * @param owner user who owns the comments, or null for all users
     * @param targetPIs set of persistent identifiers to restrict results to
     * @return Number of rows that match the criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCommentCount(Map<String, String> filters, User owner, Set<String> targetPIs) throws DAOException;

    /**
     * getComments.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @param targetPIs set of persistent identifiers to restrict results to
     * @return list of comments matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getComments(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters,
            Set<String> targetPIs) throws DAOException;

    /**
     * Gets Comments created by a specific user.
     *
     * @param user the creator/owner of the comment
     * @param maxResults maximum number of results to return
     * @param sortField class field to sort results by
     * @param descending set to "true" to sort descending
     * @return A list of at most maxResults comments.
     * @throws DAOException
     */
    List<Comment> getCommentsOfUser(User user, int maxResults, String sortField, boolean descending) throws DAOException;

    /**
     * getCommentsForPage.
     *
     * @param pi persistent identifier of the record
     * @param page page order number within the record
     * @return list of comments for the given record page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getCommentsForPage(String pi, int page) throws DAOException;

    /**
     * getCommentsForWork.
     *
     * @param pi persistent identifier of the record
     * @return list of all comments for the given record
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getCommentsForWork(String pi) throws DAOException;

    /**
     * countCommentsForWork.
     *
     * @param pi persistent identifier of the record
     * @return a long
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    long countCommentsForWork(String pi) throws DAOException;

    /**
     * getComment.
     *
     * @param id database primary key of the comment
     * @return the matching comment, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Comment getComment(long id) throws DAOException;

    /**
     * addComment.
     *
     * @param comment comment to persist
     * @return true if comment was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addComment(Comment comment) throws DAOException;

    /**
     * updateComment.
     *
     * @param comment comment to update in the database
     * @return true if comment was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateComment(Comment comment) throws DAOException;

    /**
     * deleteComment.
     *
     * @param comment comment to delete from the database
     * @return true if comment was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteComment(Comment comment) throws DAOException;

    /**
     *
     * @param pi Record identifier
     * @param owner Comment creator
     * @return Number of affected rows
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int deleteComments(String pi, User owner) throws DAOException;

    /**
     * Changes ownership of all comments from <code>fromUser</code> to <code>toUser</code>.
     *
     * @param fromUser user whose comments are to be reassigned
     * @param toUser user to reassign the comments to
     * @return Number of updated {@link Comment}s
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int changeCommentsOwner(User fromUser, User toUser) throws DAOException;

    // Search

    /**
     * getAllSearches.
     *
     * @return list of all saved searches in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getAllSearches() throws DAOException;

    /**
     * getSearchCount.
     *
     * @param owner user who owns the searches, or null for all users
     * @param filters map of field names to filter values
     * @return total number of saved searches matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getSearchCount(User owner, Map<String, String> filters) throws DAOException;

    /**
     * getSearches.
     *
     * @param owner user who owns the searches, or null for all users
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of saved searches matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getSearches(User owner, int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getSearches.
     *
     * @param owner user whose saved searches to retrieve
     * @return list of saved searches owned by the given user
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getSearches(User owner) throws DAOException;

    /**
     * getSearch.
     *
     * @param id database primary key of the search
     * @return the matching saved search, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Search getSearch(long id) throws DAOException;

    /**
     * addSearch.
     *
     * @param search saved search to persist
     * @return true if search was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addSearch(Search search) throws DAOException;

    /**
     * updateSearch.
     *
     * @param search saved search to update in the database
     * @return true if search was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateSearch(Search search) throws DAOException;

    /**
     * deleteSearch.
     *
     * @param search saved search to delete from the database
     * @return true if search was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteSearch(Search search) throws DAOException;

    // UploadJob

    /**
     * 
     * @param status {@link JobStatus}
     * @return List of {@link UploadJob}s with given status
     * @throws DAOException
     */
    public List<UploadJob> getUploadJobsWithStatus(JobStatus status) throws DAOException;

    /**
     * 
     * @param creatorId User id of the creator
     * @return {@link UploadJob}s belonging to user with given ID
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UploadJob> getUploadJobsForCreatorId(Long creatorId) throws DAOException;

    /**
     * addDownloadJob.
     *
     * @param uploadJob upload job to persist
     * @return true if upload job was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUploadJob(UploadJob uploadJob) throws DAOException;

    /**
     * updateDownloadJob.
     *
     * @param uploadJob upload job to update in the database
     * @return true if upload job was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUploadJob(UploadJob uploadJob) throws DAOException;

    /**
     * deleteDownloadJob.
     *
     * @param uploadJob upload job to delete from the database
     * @return true if upload job was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUploadJob(UploadJob uploadJob) throws DAOException;

    // CMS

    /**
     * getAllCMSPages.
     *
     * @return list of all CMS pages in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getAllCMSPages() throws DAOException;

    /**
     * getCmsPageForStaticPage.
     *
     * @param pageName static page name identifying the CMS page
     * @return the CMS page associated with the given static page name, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCmsPageForStaticPage(String pageName) throws DAOException;

    /**
     * getCMSPageCount.
     *
     * @param filters map of field names to filter values
     * @param allowedTemplates list of template IDs the user is allowed to see
     * @param allowedSubthemes list of subtheme identifiers the user is allowed to see
     * @param allowedCategories list of category names the user is allowed to see
     * @return total number of CMS pages matching the given filters and permissions
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCMSPageCount(Map<String, String> filters, List<Long> allowedTemplates, List<String> allowedSubthemes,
            List<String> allowedCategories) throws DAOException;

    /**
     * 
     * @param propertyName name of the CMS page property to filter by
     * @param propertyValue value of the CMS page property to filter by
     * @return long
     * @throws DAOException if a database error occurs
     */
    public long getCMSPageCountByPropertyValue(String propertyName, String propertyValue) throws DAOException;

    /**
     * 
     * @param propertyName name of the CMS page property to filter by
     * @param propertyValue value of the CMS page property to filter by
     * @return List<CMSPage>
     * @throws DAOException if a database error occurs
     */
    public List<CMSPage> getCMSPagesByPropertyValue(String propertyName, String propertyValue) throws DAOException;

    /**
     * getCMSPages.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @param allowedTemplates list of template IDs the user is allowed to see
     * @param allowedSubthemes list of subtheme identifiers the user is allowed to see
     * @param allowedCategories list of category names the user is allowed to see
     * @return list of CMS pages matching the given filters and permissions
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters,
            List<Long> allowedTemplates, List<String> allowedSubthemes, List<String> allowedCategories) throws DAOException;

    /**
     * getCMSPagesByCategory.
     *
     * @param category category to filter CMS pages by
     * @return list of CMS pages that have the given category assigned
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesByCategory(CMSCategory category) throws DAOException;

    /**
     * getCMSPagesForRecord.
     *
     * @param pi persistent identifier of the record
     * @param category category to filter CMS pages by, or null for all categories
     * @return list of CMS pages associated with the given record
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesForRecord(String pi, CMSCategory category) throws DAOException;

    /**
     * getCMSPagesWithRelatedPi.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param fromDate lower bound of the date range filter, or null
     * @param toDate upper bound of the date range filter, or null
     * @return list of CMS pages that have a related persistent identifier within the given date range
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesWithRelatedPi(int first, int pageSize, LocalDateTime fromDate, LocalDateTime toDate)
            throws DAOException;

    /**
     * isCMSPagesForRecordHaveUpdates.
     *
     * @param pi persistent identifier of the record
     * @param category category to filter CMS pages by, or null for all categories
     * @param fromDate lower bound of the date range filter, or null
     * @param toDate upper bound of the date range filter, or null
     * @return true if any CMS pages for the record have been updated within the given date range; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isCMSPagesForRecordHaveUpdates(String pi, CMSCategory category, LocalDateTime fromDate, LocalDateTime toDate) throws DAOException;

    /**
     * getCMSPageWithRelatedPiCount.
     *
     * @param fromDate lower bound of the date range filter, or null
     * @param toDate upper bound of the date range filter, or null
     * @return total number of CMS pages with a related persistent identifier within the given date range
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCMSPageWithRelatedPiCount(LocalDateTime fromDate, LocalDateTime toDate) throws DAOException;

    /**
     *
     * @param pi Record identifier
     * @return {@link CMSPage}
     * @throws DAOException
     */
    public CMSPage getCMSPageDefaultViewForRecord(String pi) throws DAOException;

    /**
     * 
     * @return List<String>
     * @throws DAOException
     */
    public List<String> getCMSPageAccessConditions() throws DAOException;

    /**
     * getCMSPage.
     *
     * @param id database primary key of the CMS page
     * @return the matching CMS page, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCMSPage(long id) throws DAOException;

    /**
     * addCMSPage.
     *
     * @param page CMS page to persist
     * @return true if CMS page was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSPage(CMSPage page) throws DAOException;

    /**
     * updateCMSPage.
     *
     * @param page CMS page to update in the database
     * @return true if CMS page was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSPage(CMSPage page) throws DAOException;

    /**
     * deleteCMSPage.
     *
     * @param page CMS page to delete from the database
     * @return true if CMS page was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSPage(CMSPage page) throws DAOException;

    public List<CMSPageTemplate> getAllCMSPageTemplates() throws DAOException;

    public CMSPageTemplate getCMSPageTemplate(Long id) throws DAOException;

    public boolean addCMSPageTemplate(CMSPageTemplate template) throws DAOException;

    public boolean updateCMSPageTemplate(CMSPageTemplate template) throws DAOException;

    public boolean removeCMSPageTemplate(CMSPageTemplate template) throws DAOException;

    /**
     * getAllCMSMediaItems.
     *
     * @return list of all CMS media items in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException;

    /**
     * getAllCMSCollectionItems.
     *
     * @return list of all CMS media items that represent collections
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSMediaItem> getAllCMSCollectionItems() throws DAOException;

    /**
     * getCMSMediaItem.
     *
     * @param id database primary key of the CMS media item
     * @return the matching CMS media item, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSMediaItem getCMSMediaItem(long id) throws DAOException;

    /**
     * getCMSMediaItemByFilename.
     *
     * @param string filename of the CMS media item to look up
     * @return the matching CMS media item, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    CMSMediaItem getCMSMediaItemByFilename(String string) throws DAOException;

    /**
     * addCMSMediaItem.
     *
     * @param item CMS media item to persist
     * @return true if CMS media item was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * updateCMSMediaItem.
     *
     * @param item CMS media item to update in the database
     * @return true if CMS media item was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * deleteCMSMediaItem.
     *
     * @param item CMS media item to delete from the database
     * @return true if CMS media item was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * Gets a list of all {@link CMSMediaItem}s which contain the given category.
     *
     * @param category category to filter media items by
     * @return all containing cmsPages
     * @throws DAOException
     */
    List<CMSMediaItem> getCMSMediaItemsByCategory(CMSCategory category) throws DAOException;

    /**
     * getAllTopCMSNavigationItems.
     *
     * @return list of all top-level CMS navigation items
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSNavigationItem> getAllTopCMSNavigationItems() throws DAOException;

    /**
     * getCMSNavigationItem.
     *
     * @param id database primary key of the navigation item
     * @return the matching CMS navigation item, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSNavigationItem getCMSNavigationItem(long id) throws DAOException;

    /**
     * addCMSNavigationItem.
     *
     * @param item CMS navigation item to persist
     * @return true if navigation item was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * updateCMSNavigationItem.
     *
     * @param item CMS navigation item to update in the database
     * @return true if navigation item was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * deleteCMSNavigationItem.
     *
     * @param item CMS navigation item to delete from the database
     * @return true if navigation item was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * getRelatedNavItem.
     *
     * @param page CMS page whose related navigation items to retrieve
     * @return list of CMS navigation items linking to the given page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSNavigationItem> getRelatedNavItem(CMSPage page) throws DAOException;

    /**
     * getAllStaticPages.
     *
     * @return list of all CMS static page mappings in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSStaticPage> getAllStaticPages() throws DAOException;

    /**
     * addStaticPage.
     *
     * @param page static page to persist
     * @return true if page added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * updateStaticPage.
     *
     * @param page static page to update in the database
     * @return true if page updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * deleteStaticPage.
     *
     * @param page static page to delete from the database
     * @return true if page deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * getStaticPageForCMSPage.
     *
     * @param page CMS page whose static page mappings to retrieve
     * @return list of static page mappings associated with the given CMS page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSStaticPage> getStaticPageForCMSPage(CMSPage page) throws DAOException;

    /**
     * getStaticPageForTypeType.
     *
     * @param pageType viewer page type to look up the static page mapping for
     * @return the CMS static page mapped to the given page type, or empty if none exists
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException;

    // CMS archive configurations

    /**
     * getCMSArchiveConfigs.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of column filter values
     * @return List<CMSArchiveConfig>
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSArchiveConfig> getCMSArchiveConfigs(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getCMSArchiveConfigCount.
     *
     * @param filters Selected filters
     * @return Number of found rows
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCMSArchiveConfigCount(Map<String, String> filters) throws DAOException;

    /**
     * 
     * @param pi Archive record identifier
     * @return Optional<CMSArchiveConfig> for the given pi; null if none found
     * @throws DAOException
     */
    public Optional<CMSArchiveConfig> getCmsArchiveConfigForArchive(String pi) throws DAOException;

    /**
     * saveCMSArchiveConfig.
     *
     * @param config CMS archive configuration to persist or update
     * @return true if archive config was saved successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean saveCMSArchiveConfig(CMSArchiveConfig config) throws DAOException;

    /**
     * deleteCMSArchiveConfig.
     *
     * @param config CMS archive configuration to delete from the database
     * @return true if archive config was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSArchiveConfig(CMSArchiveConfig config) throws DAOException;

    /**
     * getAllCategories.
     *
     * @return list of all CMS categories in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCategory> getAllCategories() throws DAOException;

    /**
     *
     * @param category category to count CMS pages for
     * @return Number of existing CMS pages having the given category
     * @throws DAOException
     */
    public long getCountPagesUsingCategory(CMSCategory category) throws DAOException;

    /**
     *
     * @param category category to count CMS media items for
     * @return Number of existing CMS media items having the given category
     * @throws DAOException
     */
    public long getCountMediaItemsUsingCategory(CMSCategory category) throws DAOException;

    /**
     * addCategory.
     *
     * @param category CMS category to persist
     * @return true if category added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCategory(CMSCategory category) throws DAOException;

    /**
     * updateCategory.
     *
     * @param category CMS category to update in the database
     * @return true if category updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCategory(CMSCategory category) throws DAOException;

    /**
     * deleteCategory.
     *
     * @param category CMS category to delete from the database
     * @return true if category deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCategory(CMSCategory category) throws DAOException;

    /**
     * getCategoryByName.
     *
     * @param name name of the category to look up
     * @return the matching CMS category, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCategory getCategoryByName(String name) throws DAOException;

    /**
     * getCategory.
     *
     * @param id database primary key of the category
     * @return the matching CMS category, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCategory getCategory(Long id) throws DAOException;

    // Transkribus

    /**
     * getAllTranskribusJobs.
     *
     * @return list of all Transkribus jobs in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<TranskribusJob> getAllTranskribusJobs() throws DAOException;

    /**
     * getTranskribusJobs.
     *
     * @param pi persistent identifier of the record, or null to ignore
     * @param transkribusUserId Transkribus user ID to filter by, or null to ignore
     * @param status job status to filter by, or null to ignore
     * @return list of Transkribus jobs matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<TranskribusJob> getTranskribusJobs(String pi, String transkribusUserId, JobStatus status) throws DAOException;

    /**
     * addTranskribusJob.
     *
     * @param job Transkribus job to persist
     * @return true if job added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addTranskribusJob(TranskribusJob job) throws DAOException;

    /**
     * updateTranskribusJob.
     *
     * @param job Transkribus job to update in the database
     * @return true if job updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateTranskribusJob(TranskribusJob job) throws DAOException;

    /**
     * deleteTranskribusJob.
     *
     * @param job Transkribus job to delete from the database
     * @return true if job deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteTranskribusJob(TranskribusJob job) throws DAOException;

    // Crowdsourcing campaigns

    /**
     * getAllCampaigns.
     *
     * @return list of all crowdsourcing campaigns in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Campaign> getAllCampaigns() throws DAOException;

    /**
     * getCampaignCount.
     *
     * @param filters map of field names to filter values
     * @return total number of crowdsourcing campaigns matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCampaignCount(Map<String, String> filters) throws DAOException;

    /**
     * getCampaign.
     *
     * @param id database primary key of the campaign
     * @return the matching campaign, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Campaign getCampaign(Long id) throws DAOException;

    /**
     * getQuestion.
     *
     * @param id database primary key of the question
     * @return the matching campaign question, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Question getQuestion(Long id) throws DAOException;

    /**
     * getCampaigns.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of crowdsourcing campaigns matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Campaign> getCampaigns(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getCampaignStatisticsForRecord.
     *
     * @param pi persistent identifier of the record
     * @param status crowdsourcing status to filter by, or null for any status
     * @return list of campaign record statistics for the given record
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CampaignRecordStatistic> getCampaignStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException;

    /**
     * getCampaignPageStatisticsForRecord.
     *
     * @param pi persistent identifier of the record
     * @param status crowdsourcing status to filter by, or null for any status
     * @return list of campaign page statistics for the given record
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    List<CampaignRecordPageStatistic> getCampaignPageStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException;

    /**
     * addCampaign.
     *
     * @param campaign crowdsourcing campaign to persist
     * @return true if campaign added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCampaign(Campaign campaign) throws DAOException;

    /**
     * updateCampaign.
     *
     * @param campaign crowdsourcing campaign to update in the database
     * @return true if campaign updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCampaign(Campaign campaign) throws DAOException;

    /**
     * deleteCampaign.
     *
     * @param campaign crowdsourcing campaign to delete from the database
     * @return true if campaign deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCampaign(Campaign campaign) throws DAOException;

    /**
     * Deletes given user from the lists of annotators and reviewers an all campaign statistics.
     *
     * @param user user to remove from campaign statistics
     * @return Number of affected campaigns
     * @throws DAOException
     */
    public int deleteCampaignStatisticsForUser(User user) throws DAOException;

    /**
     * Replaced <code>fromUser</code> with <code>toUser</code> in the lists of annotators and reviewers an all campaign statistics.
     *
     * @param fromUser user to be replaced in campaign statistics
     * @param toUser user to replace fromUser with
     * @return Number of updated rows
     * @throws DAOException
     */
    public int changeCampaignStatisticContributors(User fromUser, User toUser) throws DAOException;

    // Misc

    /**
     *
     * @return true if accessible; false otherwise
     */
    public boolean checkAvailability();

    /**
     * shutdown.
     */
    public void shutdown();

    /**
     * getPagesWithComments.
     *
     * @param pi persistent identifier of the record
     * @return list of page order numbers that have at least one comment in the given record
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Integer> getPagesWithComments(String pi) throws DAOException;

    /**
     * getCMSCollections.
     *
     * @param solrField Solr field name identifying the collection type
     * @return list of CMS collections for the given Solr field
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCollection> getCMSCollections(String solrField) throws DAOException;

    /**
     * addCMSCollection.
     *
     * @param collection CMS collection to persist
     * @return true if CMS collection was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * updateCMSCollection.
     *
     * @param collection CMS collection to update in the database
     * @return true if CMS collection was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * deleteCMSCollection.
     *
     * @param collection CMS collection to delete from the database
     * @return true if CMS collection was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * getCMSCollection.
     *
     * @param solrField Solr field name identifying the collection type
     * @param solrFieldValue Solr field value identifying the collection
     * @return the matching CMS collection, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException;

    /**
     * Annotations *.
     *
     * @param id database primary key of the annotation
     * @return the matching crowdsourcing annotation, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CrowdsourcingAnnotation getAnnotation(Long id) throws DAOException;

    /**
     * getAnnotationsForCampaign.
     *
     * @param campaign campaign whose annotations to retrieve
     * @return list of crowdsourcing annotations for the given campaign
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaign(Campaign campaign) throws DAOException;

    /**
     * getAnnotationsForWork.
     *
     * @param pi persistent identifier of the record
     * @return list of crowdsourcing annotations for the given record
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForWork(String pi) throws DAOException;

    /**
     * @param pi persistent identifier of the record
     * @return Number of existing annotations for the given pi
     * @throws DAOException
     */
    long getAnnotationCountForWork(String pi) throws DAOException;

    /**
     * getAnnotationsForCampaignAndWork.
     *
     * @param campaign campaign whose annotations to retrieve
     * @param pi persistent identifier of the record
     * @return list of crowdsourcing annotations for the given campaign and record
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaignAndWork(Campaign campaign, String pi) throws DAOException;

    /**
     * getAnnotationsForTarget.
     *
     * @param pi persistent identifier of the record
     * @param page page order number within the record, or null for all pages
     * @return list of crowdsourcing annotations targeting the given record page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForTarget(String pi, Integer page) throws DAOException;

    public List<CrowdsourcingAnnotation> getAnnotationsForTarget(String pi, Integer page, String motivation) throws DAOException;

    /**
     *
     * @param userId database id of the annotation creator
     * @param maxResults maximum number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @return List of {@link CrowdsourcingAnnotation}s for the given userId
     * @throws DAOException
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForUserId(Long userId, Integer maxResults, String sortField, boolean descending)
            throws DAOException;

    /**
     * getAnnotations.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of crowdsourcing annotations matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotations(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getAnnotationCount.
     *
     * @param filters map of field names to filter values
     * @return total number of crowdsourcing annotations matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getAnnotationCount(Map<String, String> filters) throws DAOException;

    /**
     * getAnnotationCountForTarget.
     *
     * @param pi persistent identifier of the record
     * @param page page order number within the record, or null for all pages
     * @return total number of crowdsourcing annotations for the given record page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    long getAnnotationCountForTarget(String pi, Integer page) throws DAOException;

    /**
     * getAnnotationsForCampaignAndTarget.
     *
     * @param campaign campaign whose annotations to retrieve
     * @param pi persistent identifier of the record
     * @param page page order number within the record, or null for all pages
     * @return list of crowdsourcing annotations for the given campaign and record page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaignAndTarget(Campaign campaign, String pi, Integer page) throws DAOException;

    /**
     * addAnnotation.
     *
     * @param annotation crowdsourcing annotation to persist
     * @return true if annotation was added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * updateAnnotation.
     *
     * @param annotation crowdsourcing annotation to update in the database
     * @return true if annotation was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * deleteAnnotation.
     *
     * @param annotation crowdsourcing annotation to delete from the database
     * @return true if annotation was deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * Gets the {@link GeoMap} of the given mapId.
     *
     * @param mapId database id of the geo map
     * @return The GeoMap of the given id or else null
     * @throws DAOException if a database error occurs
     */
    public GeoMap getGeoMap(Long mapId) throws DAOException;

    /**
     * Gets all {@link GeoMap}s in database.
     *
     * @return A list of all stored GeoMaps
     * @throws DAOException
     */
    public List<GeoMap> getAllGeoMaps() throws DAOException;

    /**
     * Adds the given map to the database if no map of the same id already exists.
     *
     * @param map geo map to add
     * @return true if successful
     * @throws DAOException
     */
    public boolean addGeoMap(GeoMap map) throws DAOException;

    /**
     * Updates the given {@link GeoMap} in the database.
     *
     * @param map geo map to update
     * @return true if successful
     * @throws DAOException
     */
    public boolean updateGeoMap(GeoMap map) throws DAOException;

    /**
     * Deletes the given {@link GeoMap} from the database.
     *
     * @param map geo map to delete
     * @return true if successful
     * @throws DAOException
     */
    public boolean deleteGeoMap(GeoMap map) throws DAOException;

    /**
     * Returns a list of CMS-pages embedding the given map.
     *
     * @param map geo map to find embedding pages for
     * @return List of {@link CMSPage}s that use given map
     * @throws DAOException
     */
    public List<CMSPage> getPagesUsingMap(GeoMap map) throws DAOException;

    /**
     * Returns a list of CMS-pages embedding the given map in a sidebar widget.
     *
     * @param map geo map to find embedding sidebar pages for
     * @return List of {@link CMSPage}s that use given map in sidebar
     * @throws DAOException
     */
    public List<CMSPage> getPagesUsingMapInSidebar(GeoMap map) throws DAOException;

    /**
     * @param subtheme subtheme identifier to filter CMS pages by
     * @return List of {@link CMSPage}s that use given subtheme
     * @throws DAOException
     */
    List<CMSPage> getCMSPagesForSubtheme(String subtheme) throws DAOException;

    /**
     * Gets a paginated list of {@link CMSRecordNote}s.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of column filter values
     * @return List of {@link CMSPage}s that match the given filters
     * @throws DAOException
     */
    public List<CMSRecordNote> getRecordNotes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * Gets all {@link CMSRecordNote}s for the given pi.
     *
     * @param pi The pi of the record.
     * @param displayedNotesOnly set to true to only return notes with {@link io.goobi.viewer.model.cms.recordnotes.CMSRecordNote#isDisplayNote()} set
     *            to true
     * @return List of {@link CMSSingleRecordNote}s for the given pi
     * @throws DAOException
     */
    public List<CMSSingleRecordNote> getRecordNotesForPi(String pi, boolean displayedNotesOnly) throws DAOException;

    /**
     * Get all {@link CMSMultiRecordNote}s. To find notes relating to record, all notes must be check for matching query
     *
     * @param displayedNotesOnly if true, only return notes marked as displayed
     * @return List of all existing {@link CMSSingleRecordNote}s, optionaly filtered by displayed only
     * @throws DAOException
     */
    public List<CMSMultiRecordNote> getAllMultiRecordNotes(boolean displayedNotesOnly) throws DAOException;

    /**
     * Gets all persisted {@link CMSRecordNote}s.
     *
     * @return List of all existing {@link CMSSingleRecordNote}s
     * @throws DAOException
     */
    public List<CMSRecordNote> getAllRecordNotes() throws DAOException;

    /**
     * Gets a {@link CMSRecordNote} by its id property.
     *
     * @param id database id of the record note
     * @return {@link CMSSingleRecordNote} with the given id
     * @throws DAOException if a database error occurs
     */
    public CMSRecordNote getRecordNote(Long id) throws DAOException;

    /**
     * Persist a new {@link CMSRecordNote}.
     *
     * @param note record note to add
     * @return true if note added successfully; false otherwise
     * @throws DAOException if a database error occurs
     */
    public boolean addRecordNote(CMSRecordNote note) throws DAOException;

    /**
     * Updates an existing {@link CMSRecordNote}.
     *
     * @param note record note to update
     * @return true if note updated successfully; false otherwise
     * @throws DAOException if a database error occurs
     */
    public boolean updateRecordNote(CMSRecordNote note) throws DAOException;

    /**
     * Deletes an existing {@link CMSRecordNote}.
     *
     * @param note record note to delete
     * @return true if note deleted successfully; false otherwise
     * @throws DAOException if a database error occurs
     */
    public boolean deleteRecordNote(CMSRecordNote note) throws DAOException;

    public boolean saveTermsOfUse(TermsOfUse tou) throws DAOException;

    public TermsOfUse getTermsOfUse() throws DAOException;

    public boolean resetUserAgreementsToTermsOfUse() throws DAOException;

    public List<CMSSlider> getAllSliders() throws DAOException;

    public CMSSlider getSlider(Long id) throws DAOException;

    public boolean addSlider(CMSSlider slider) throws DAOException;

    public boolean updateSlider(CMSSlider slider) throws DAOException;

    public boolean deleteSlider(CMSSlider slider) throws DAOException;

    List<CMSPage> getPagesUsingSlider(CMSSlider slider) throws DAOException;

    public List<ThemeConfiguration> getConfiguredThemes() throws DAOException;

    public ThemeConfiguration getTheme(String name) throws DAOException;

    public boolean addTheme(ThemeConfiguration theme) throws DAOException;

    public boolean updateTheme(ThemeConfiguration theme) throws DAOException;

    public boolean deleteTheme(ThemeConfiguration theme) throws DAOException;

    /**
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filterString JPQL WHERE clause fragment for additional filtering
     * @param filterParams named parameters for the filterString clause
     * @return List of {@link CrowdsourcingAnnotation}s matching given filters
     * @throws DAOException
     */
    public List<CrowdsourcingAnnotation> getAnnotations(int first, int pageSize, String sortField, boolean descending, String filterString,
            Map<String, Object> filterParams) throws DAOException;

    /**
     * @param commenting annotation motivation value to filter by
     * @return List of {@link CrowdsourcingAnnotation}s matching given commenting
     * @throws DAOException
     */
    public List<CrowdsourcingAnnotation> getAllAnnotationsByMotivation(String commenting) throws DAOException;

    /**
     * @param sortField field to sort by
     * @param sortDescending true for descending sort order
     * @return List of all existing {@link CrowdsourcingAnnotation}s, optionally sorted by given sortField
     * @throws DAOException
     */
    public List<CrowdsourcingAnnotation> getAllAnnotations(String sortField, boolean sortDescending) throws DAOException;

    /**
     * @return Number of all existing {@link CrowdsourcingAnnotation}s
     * @throws DAOException
     */
    public long getTotalAnnotationCount() throws DAOException;

    public List<CustomSidebarWidget> getAllCustomWidgets() throws DAOException;

    public CustomSidebarWidget getCustomWidget(Long id) throws DAOException;

    public boolean addCustomWidget(CustomSidebarWidget widget) throws DAOException;

    public boolean updateCustomWidget(CustomSidebarWidget widget) throws DAOException;

    public boolean deleteCustomWidget(Long id) throws DAOException;

    public List<CMSPage> getPagesUsingWidget(CustomSidebarWidget widget) throws DAOException;

    public CookieBanner getCookieBanner() throws DAOException;

    public boolean saveCookieBanner(CookieBanner banner) throws DAOException;

    /**
     * Get the single stored {@link Disclaimer}. May return null if no disclaimer has been persisted yet
     * 
     * @return the disclaimer or null
     * @throws DAOException
     */
    public Disclaimer getDisclaimer() throws DAOException;

    public boolean saveDisclaimer(Disclaimer disclaimer) throws DAOException;

    public Long getNumRecordsWithComments(User user) throws DAOException;

    @SuppressWarnings("rawtypes")
    public List getNativeQueryResults(String query) throws DAOException;

    public int executeUpdate(String string) throws DAOException;

    public List<ClientApplication> getAllClientApplications() throws DAOException;

    public ClientApplication getClientApplication(long id) throws DAOException;

    public boolean saveClientApplication(ClientApplication client) throws DAOException;

    public boolean deleteClientApplication(long id) throws DAOException;

    public ClientApplication getClientApplicationByClientId(String clientId) throws DAOException;

    public List<DailySessionUsageStatistics> getAllUsageStatistics() throws DAOException;

    public DailySessionUsageStatistics getUsageStatistics(LocalDate date) throws DAOException;

    public List<DailySessionUsageStatistics> getUsageStatistics(LocalDate start, LocalDate end) throws DAOException;

    public boolean addUsageStatistics(DailySessionUsageStatistics statistics) throws DAOException;

    public boolean updateUsageStatistics(DailySessionUsageStatistics statistics) throws DAOException;

    public boolean deleteUsageStatistics(long id) throws DAOException;

    public boolean deleteCMSComponent(PersistentCMSComponent persistentCMSComponent) throws DAOException;

    public boolean deleteCMSContent(CMSContent content) throws DAOException;

    public boolean addCMSComponent(PersistentCMSComponent persistentCMSComponent) throws DAOException;

    public boolean updatedCMSComponent(PersistentCMSComponent persistentCMSComponent) throws DAOException;

    public PersistentCMSComponent getCMSComponent(Long id) throws DAOException;

    public boolean deleteViewerMessage(ViewerMessage message) throws DAOException;

    public boolean addViewerMessage(ViewerMessage message) throws DAOException;

    public boolean updateViewerMessage(ViewerMessage message) throws DAOException;

    public ViewerMessage getViewerMessage(Long id) throws DAOException;

    public ViewerMessage getViewerMessageByMessageID(String id) throws DAOException;

    /**
     * getViewerMessages.
     *
     * @param first index of first result (pagination)
     * @param pageSize max number of results to return
     * @param sortField field to sort by
     * @param descending true for descending sort order
     * @param filters map of field names to filter values
     * @return list of viewer messages matching the given criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<ViewerMessage> getViewerMessages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getViewerMessageCount.
     *
     * @param filters map of field names to filter values
     * @return total number of viewer messages matching the given filters
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getViewerMessageCount(Map<String, String> filters) throws DAOException;

    public List<RecurringTaskTrigger> getRecurringTaskTriggers() throws DAOException;

    public RecurringTaskTrigger getRecurringTaskTrigger(Long id) throws DAOException;

    public RecurringTaskTrigger getRecurringTaskTriggerForTask(ITaskType task) throws DAOException;

    public boolean addRecurringTaskTrigger(RecurringTaskTrigger trigger) throws DAOException;

    public boolean updateRecurringTaskTrigger(RecurringTaskTrigger trigger) throws DAOException;

    public boolean deleteRecurringTaskTrigger(Long id) throws DAOException;

    public int deleteViewerMessagesBefore(LocalDateTime date) throws DAOException;

    public boolean addHighlight(HighlightData object) throws DAOException;

    public boolean updateHighlight(HighlightData object) throws DAOException;

    public boolean deleteHighlight(Long id) throws DAOException;

    public HighlightData getHighlight(Long id) throws DAOException;

    public List<HighlightData> getAllHighlights() throws DAOException;

    public List<HighlightData> getHighlights(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    public List<HighlightData> getHighlightsForDate(LocalDateTime date) throws DAOException;

    public List<HighlightData> getPastHighlightsForDate(int first, int pageSize, String sortField, boolean descending,
            Map<String, String> filters, LocalDateTime date) throws DAOException;

    public List<HighlightData> getFutureHighlightsForDate(int first, int pageSize, String sortField, boolean descending,
            Map<String, String> filters, LocalDateTime date) throws DAOException;

    // Maintenance mode

    /**
     * Returns the only existing instance of MaintenanceMode.
     *
     * @return the singleton MaintenanceMode entity, or null if not yet persisted
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public MaintenanceMode getMaintenanceMode() throws DAOException;

    /**
     * updateMaintenanceMode.
     *
     * @param maintenanceMode maintenance mode entity to update in the database
     * @return true if maintenance mode was updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateMaintenanceMode(MaintenanceMode maintenanceMode) throws DAOException;

    /**
     * Get the EntityManagerFactory created when initializing the class. Can be used to explicitly create new EntityManagers.
     *

     */
    EntityManagerFactory getFactory();

    /**
     * Get an EntityManager for a query or transaction. Must always be followed by {@link #close(EntityManager) close(EntityManager) Method} after the
     * query/transaction
     *
     * @return a new EntityManager
     */
    EntityManager getEntityManager();

    /**
     * Either close the given EntityManager or do some other post query/transaction handling for the given EntityManager. Must be called after each
     * query/transaction.
     *
     * @param em EntityManager
     * @throws DAOException
     */
    void close(EntityManager em) throws DAOException;

    /**
     * Call {@link EntityManager#getTransaction() getTransaction()} on the given EntityManager and then {@link EntityTransaction#begin() begin()} on
     * the transaction.
     * 
     * @param em EntityManager
     * @return the transaction gotten from the entity manager
     */
    EntityTransaction startTransaction(EntityManager em);

    /**
     * Call {@link EntityTransaction#commit()} on the given transaction.
     *
     * @param et EntityTransaction
     * @throws PersistenceException
     */
    void commitTransaction(EntityTransaction et) throws PersistenceException;

    /**
     * Call {@link EntityTransaction#commit()} on the current transaction of the given EntityManager.
     *
     * @param em EntityManager
     * @throws PersistenceException
     */
    void commitTransaction(EntityManager em) throws PersistenceException;

    /**
     * Handling of exceptions occurred during {@link #commitTransaction(EntityTransaction)}. Usually calls {@link EntityTransaction#rollback()}
     *
     * @param et EntityTransaction
     * @throws PersistenceException
     */
    void handleException(EntityTransaction et) throws PersistenceException;

    /**
     * Handling of exceptions occurred during {@link #commitTransaction(EntityManager)} Usually calls {@link EntityTransaction#rollback()} on the
     * current transaction of the given EntityManager.
     *
     * @param em EntityManager
     * @throws PersistenceException
     */
    void handleException(EntityManager em);

}
