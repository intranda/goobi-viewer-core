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
 * IDAO interface.
 */
public interface IDAO {

    /**
     * tableExists.
     *
     * @param tableName a {@link java.lang.String} object.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     * @throws DAOException
     */
    boolean tableExists(String tableName) throws DAOException, SQLException;

    /**
     * columnsExists.
     *
     * @param tableName a {@link java.lang.String} object.
     * @param columnName a {@link java.lang.String} object.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    boolean columnsExists(String tableName, String columnName) throws DAOException, SQLException;

    // User

    /**
     * getAllUsers.
     *
     * @param refresh a boolean.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getAllUsers(boolean refresh) throws DAOException;

    /**
     * getUserCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserCount(Map<String, String> filters) throws DAOException;

    /**
     * getUsers.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getUsers(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    /**
     * 
     * @param propertyName
     * @param propertyValue
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
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUser(long id) throws DAOException;

    /**
     * getUserByEmail.
     *
     * @param email a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByEmail(String email) throws DAOException;

    /**
     * getUserByOpenId.
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByOpenId(String identifier) throws DAOException;

    /**
     * getUserByNickname.
     *
     * @param nickname a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByNickname(String nickname) throws DAOException;

    /**
     * addUser.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUser(User user) throws DAOException;

    /**
     * updateUser.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUser(User user) throws DAOException;

    /**
     * deleteUser.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUser(User user) throws DAOException;

    // UserGroup

    /**
     * getAllUserGroups.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getAllUserGroups() throws DAOException;

    /**
     * getUserGroupCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserGroupCount(Map<String, String> filters) throws DAOException;

    /**
     * getUserGroups.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getUserGroups(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getUserGroups.
     *
     * @param owner a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getUserGroups(User owner) throws DAOException;

    /**
     * getUserGroup.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public UserGroup getUserGroup(long id) throws DAOException;

    /**
     * getUserGroup.
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public UserGroup getUserGroup(String name) throws DAOException;

    /**
     * addUserGroup.
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUserGroup(UserGroup userGroup) throws DAOException;

    /**
     * updateUserGroup.
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUserGroup(UserGroup userGroup) throws DAOException;

    /**
     * deleteUserGroup.
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUserGroup(UserGroup userGroup) throws DAOException;

    // Bookmarks

    /**
     * getAllBookmarkLists.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getAllBookmarkLists() throws DAOException;

    /**
     * getPublicBookmarkLists.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getPublicBookmarkLists() throws DAOException;

    /**
     * getBookmarkLists.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getBookmarkLists(User user) throws DAOException;

    /**
     * Gets number of bookmark lists owned by the given user.
     *
     * @param user
     * @return number of owned bookmark lists
     * @throws DAOException
     */
    long getBookmarkListCount(User user) throws DAOException;

    /**
     * getBookmarkList.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkList(long id) throws DAOException;

    /**
     * getBookmarkList.
     *
     * @param name a {@link java.lang.String} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkList(String name, User user) throws DAOException;

    /**
     * getBookmarkListByShareKey.
     *
     * @param shareKey a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkListByShareKey(String shareKey) throws DAOException;

    /**
     * addBookmarkList.
     *
     * @param bookmarkList a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addBookmarkList(BookmarkList bookmarkList) throws DAOException;

    /**
     * updateBookmarkList.
     *
     * @param bookmarkList a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateBookmarkList(BookmarkList bookmarkList) throws DAOException;

    /**
     * deleteBookmarkList.
     *
     * @param bookmarkList a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteBookmarkList(BookmarkList bookmarkList) throws DAOException;

    // Role

    /**
     * getAllRoles.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Role> getAllRoles() throws DAOException;

    /**
     * getRoleCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getRoleCount(Map<String, String> filters) throws DAOException;

    /**
     * getRoles.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Role> getRoles(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    /**
     * getRole.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.Role} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Role getRole(long id) throws DAOException;

    /**
     * getRole.
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.Role} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Role getRole(String name) throws DAOException;

    /**
     * addRole.
     *
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addRole(Role role) throws DAOException;

    /**
     * updateRole.
     *
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateRole(Role role) throws DAOException;

    /**
     * deleteRole.
     *
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteRole(Role role) throws DAOException;

    // UserRole

    /**
     * getAllUserRoles.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserRole> getAllUserRoles() throws DAOException;

    /**
     * getUserRoleCount.
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return Row count
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserRoleCount(UserGroup userGroup, User user, Role role) throws DAOException;

    /**
     * getUserRoles.
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserRole> getUserRoles(UserGroup userGroup, User user, Role role) throws DAOException;

    /**
     * addUserRole.
     *
     * @param userRole a {@link io.goobi.viewer.model.security.user.UserRole} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUserRole(UserRole userRole) throws DAOException;

    /**
     * updateUserRole.
     *
     * @param userRole a {@link io.goobi.viewer.model.security.user.UserRole} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUserRole(UserRole userRole) throws DAOException;

    /**
     * deleteUserRole.
     *
     * @param userRole a {@link io.goobi.viewer.model.security.user.UserRole} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUserRole(UserRole userRole) throws DAOException;

    // LicenseType

    /**
     * getAllLicenseTypes.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllLicenseTypes() throws DAOException;

    /**
     * getLicenseTypeCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getLicenseTypeCount(Map<String, String> filters) throws DAOException;

    /**
     * getCoreLicenseTypeCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCoreLicenseTypeCount(Map<String, String> filters) throws DAOException;

    /**
     * getRecordLicenseTypes.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getRecordLicenseTypes() throws DAOException;

    /**
     * getLicenseTypes.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getCoreLicenseTypes.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getCoreLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getLicenseType.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public LicenseType getLicenseType(long id) throws DAOException;

    /**
     * getLicenseType.
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public LicenseType getLicenseType(String name) throws DAOException;

    /**
     * Returns all license types that match the given name list.
     *
     * @param names
     * @return a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @throws DAOException in case of errors
     */
    public List<LicenseType> getLicenseTypes(List<String> names) throws DAOException;

    /**
     * 
     * @param licenseType
     * @return List of license types overriding given licenseType
     * @throws DAOException
     */
    public List<LicenseType> getOverridingLicenseType(LicenseType licenseType) throws DAOException;

    /**
     * addLicenseType.
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addLicenseType(LicenseType licenseType) throws DAOException;

    /**
     * updateLicenseType.
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateLicenseType(LicenseType licenseType) throws DAOException;

    /**
     * deleteLicenseType.
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteLicenseType(LicenseType licenseType) throws DAOException;

    // License

    /**
     * getAllLicenses.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<License> getAllLicenses() throws DAOException;

    /**
     * getLicense.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.License} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public License getLicense(Long id) throws DAOException;

    /**
     *
     * @param licenseType
     * @return List of {@link License}s of the given licenseType
     * @throws DAOException
     */
    public List<License> getLicenses(LicenseType licenseType) throws DAOException;

    /**
     *
     * @param licensee
     * @return List of {@link License}s for the given licensee
     * @throws DAOException
     */
    public List<License> getLicenses(ILicensee licensee) throws DAOException;

    /**
     * Returns the number of licenses that use the given license type.
     *
     * @param licenseType
     * @return Number of existing {@link License}s of the given licenseType
     * @throws DAOException
     */
    public long getLicenseCount(LicenseType licenseType) throws DAOException;

    /**
     * addLicenseType.
     *
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addLicense(License license) throws DAOException;

    /**
     * updateLicenseType.
     *
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateLicense(License license) throws DAOException;

    /**
     * deleteLicenseType.
     *
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteLicense(License license) throws DAOException;

    // AccessTicket

    /**
     * 
     * @param id
     * @return {@link AccessTicket} with the given id
     * @throws DAOException
     */
    public AccessTicket getTicket(Long id) throws DAOException;

    /**
     * 
     * @param passwordHash
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
     * @param sortField a {@link java.lang.String} object.
     * @param descending true if descending order requested; false otherwise
     * @param filters Selected filters
     * @return a {@link java.util.List} object.
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
     * @param ticket a {@link AccessTicket} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addTicket(AccessTicket ticket) throws DAOException;

    /**
     * updateTicket.
     *
     * @param ticket a {@link AccessTicket} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateTicket(AccessTicket ticket) throws DAOException;

    /**
     * deleteTicket.
     *
     * @param ticket a {@link AccessTicket} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteTicket(AccessTicket ticket) throws DAOException;

    // IpRange

    /**
     * getAllIpRanges.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<IpRange> getAllIpRanges() throws DAOException;

    /**
     * getIpRangeCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getIpRangeCount(Map<String, String> filters) throws DAOException;

    /**
     * getIpRanges.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<IpRange> getIpRanges(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    /**
     * getIpRange.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public IpRange getIpRange(long id) throws DAOException;

    /**
     * getIpRange.
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public IpRange getIpRange(String name) throws DAOException;

    /**
     * addIpRange.
     *
     * @param ipRange a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addIpRange(IpRange ipRange) throws DAOException;

    /**
     * updateIpRange.
     *
     * @param ipRange a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateIpRange(IpRange ipRange) throws DAOException;

    /**
     * deleteIpRange.
     *
     * @param ipRange a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @return a boolean.
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
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CommentGroup getCommentGroup(long id) throws DAOException;

    /**
     * addCommentGroup.
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCommentGroup(CommentGroup commentGroup) throws DAOException;

    /**
     * updateCommentGroup.
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCommentGroup(CommentGroup commentGroup) throws DAOException;

    /**
     * deleteCommentGroup.
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCommentGroup(CommentGroup commentGroup) throws DAOException;

    // Comment

    /**
     * getAllComments.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getAllComments() throws DAOException;

    /**
     * getCommentCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @param owner
     * @param targetPIs
     * @return Number of rows that match the criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCommentCount(Map<String, String> filters, User owner, Set<String> targetPIs) throws DAOException;

    /**
     * getComments.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @param targetPIs
     * @return a {@link java.util.List} object.
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
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getCommentsForPage(String pi, int page) throws DAOException;

    /**
     * getCommentsForWork.
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getCommentsForWork(String pi) throws DAOException;

    /**
     * countCommentsForWork.
     *
     * @param pi a {@link java.lang.String} object.
     * @return a long
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    long countCommentsForWork(String pi) throws DAOException;

    /**
     * getComment.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Comment getComment(long id) throws DAOException;

    /**
     * addComment.
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addComment(Comment comment) throws DAOException;

    /**
     * updateComment.
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateComment(Comment comment) throws DAOException;

    /**
     * deleteComment.
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @return a boolean.
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
     * @param fromUser
     * @param toUser
     * @return Number of updated {@link Comment}s
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int changeCommentsOwner(User fromUser, User toUser) throws DAOException;

    // Search

    /**
     * getAllSearches.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getAllSearches() throws DAOException;

    /**
     * getSearchCount.
     *
     * @param owner a {@link io.goobi.viewer.model.security.user.User} object.
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getSearchCount(User owner, Map<String, String> filters) throws DAOException;

    /**
     * getSearches.
     *
     * @param owner a {@link io.goobi.viewer.model.security.user.User} object.
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getSearches(User owner, int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getSearches.
     *
     * @param owner a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getSearches(User owner) throws DAOException;

    /**
     * getSearch.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.search.Search} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Search getSearch(long id) throws DAOException;

    /**
     * addSearch.
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addSearch(Search search) throws DAOException;

    /**
     * updateSearch.
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateSearch(Search search) throws DAOException;

    /**
     * deleteSearch.
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a boolean.
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
     * @param uploadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUploadJob(UploadJob uploadJob) throws DAOException;

    /**
     * updateDownloadJob.
     *
     * @param uploadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUploadJob(UploadJob uploadJob) throws DAOException;

    /**
     * deleteDownloadJob.
     *
     * @param uploadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUploadJob(UploadJob uploadJob) throws DAOException;

    // CMS

    /**
     * getAllCMSPages.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getAllCMSPages() throws DAOException;

    /**
     * getCmsPageForStaticPage.
     *
     * @param pageName a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCmsPageForStaticPage(String pageName) throws DAOException;

    /**
     * getCMSPageCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @param allowedTemplates a {@link java.util.List} object.
     * @param allowedSubthemes a {@link java.util.List} object.
     * @param allowedCategories a {@link java.util.List} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCMSPageCount(Map<String, String> filters, List<Long> allowedTemplates, List<String> allowedSubthemes,
            List<String> allowedCategories) throws DAOException;

    /**
     * 
     * @param propertyName
     * @param propertyValue
     * @return long
     */
    public long getCMSPageCountByPropertyValue(String propertyName, String propertyValue) throws DAOException;

    /**
     * 
     * @param propertyName
     * @param propertyValue
     * @return List<CMSPage>
     */
    public List<CMSPage> getCMSPagesByPropertyValue(String propertyName, String propertyValue) throws DAOException;

    /**
     * getCMSPages.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @param allowedTemplates a {@link java.util.List} object.
     * @param allowedSubthemes a {@link java.util.List} object.
     * @param allowedCategories a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters,
            List<Long> allowedTemplates, List<String> allowedSubthemes, List<String> allowedCategories) throws DAOException;

    /**
     * getCMSPagesByCategory.
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesByCategory(CMSCategory category) throws DAOException;

    /**
     * getCMSPagesForRecord.
     *
     * @param pi a {@link java.lang.String} object.
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesForRecord(String pi, CMSCategory category) throws DAOException;

    /**
     * getCMSPagesWithRelatedPi.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param fromDate a {@link java.time.LocalDateTime} object.
     * @param toDate a {@link java.time.LocalDateTime} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesWithRelatedPi(int first, int pageSize, LocalDateTime fromDate, LocalDateTime toDate)
            throws DAOException;

    /**
     * isCMSPagesForRecordHaveUpdates.
     *
     * @param pi a {@link java.lang.String} object.
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @param fromDate a {@link java.time.LocalDateTime} object.
     * @param toDate a {@link java.time.LocalDateTime} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isCMSPagesForRecordHaveUpdates(String pi, CMSCategory category, LocalDateTime fromDate, LocalDateTime toDate) throws DAOException;

    /**
     * getCMSPageWithRelatedPiCount.
     *
     * @param fromDate a {@link java.time.LocalDateTime} object.
     * @param toDate a {@link java.time.LocalDateTime} object.
     * @return a long.
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
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCMSPage(long id) throws DAOException;

    /**
     * addCMSPage.
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSPage(CMSPage page) throws DAOException;

    /**
     * updateCMSPage.
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSPage(CMSPage page) throws DAOException;

    /**
     * deleteCMSPage.
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a boolean.
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
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException;

    /**
     * getAllCMSCollectionItems.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSMediaItem> getAllCMSCollectionItems() throws DAOException;

    /**
     * getCMSMediaItem.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSMediaItem getCMSMediaItem(long id) throws DAOException;

    /**
     * getCMSMediaItemByFilename.
     *
     * @param string a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    CMSMediaItem getCMSMediaItemByFilename(String string) throws DAOException;

    /**
     * addCMSMediaItem.
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * updateCMSMediaItem.
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * deleteCMSMediaItem.
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * Gets a list of all {@link CMSMediaItem}s which contain the given category.
     *
     * @param category
     * @return all containing cmsPages
     * @throws DAOException
     */
    List<CMSMediaItem> getCMSMediaItemsByCategory(CMSCategory category) throws DAOException;

    /**
     * getAllTopCMSNavigationItems.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSNavigationItem> getAllTopCMSNavigationItems() throws DAOException;

    /**
     * getCMSNavigationItem.
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSNavigationItem getCMSNavigationItem(long id) throws DAOException;

    /**
     * addCMSNavigationItem.
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * updateCMSNavigationItem.
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * deleteCMSNavigationItem.
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * getRelatedNavItem.
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSNavigationItem> getRelatedNavItem(CMSPage page) throws DAOException;

    /**
     * getAllStaticPages.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSStaticPage> getAllStaticPages() throws DAOException;

    /**
     * addStaticPage.
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSStaticPage} object.
     * @return true if page added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * updateStaticPage.
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSStaticPage} object.
     * @return true if page updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * deleteStaticPage.
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSStaticPage} object.
     * @return true if page deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * getStaticPageForCMSPage.
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSStaticPage> getStaticPageForCMSPage(CMSPage page) throws DAOException;

    /**
     * getStaticPageForTypeType.
     *
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException;

    // CMS archive configurations

    /**
     * getCMSArchiveConfigs.
     *
     * @param first
     * @param pageSize
     * @param sortField
     * @param descending
     * @param filters
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
     * @param config a {@link io.goobi.viewer.model.cms.CMSArchiveConfig} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean saveCMSArchiveConfig(CMSArchiveConfig config) throws DAOException;

    /**
     * deleteCMSArchiveConfig.
     *
     * @param config a {@link io.goobi.viewer.model.cms.CMSArchiveConfig} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSArchiveConfig(CMSArchiveConfig config) throws DAOException;

    /**
     * getAllCategories.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCategory> getAllCategories() throws DAOException;

    /**
     *
     * @param category
     * @return Number of existing CMS pages having the given category
     * @throws DAOException
     */
    public long getCountPagesUsingCategory(CMSCategory category) throws DAOException;

    /**
     *
     * @param category
     * @return Number of existing CMS media items having the given category
     * @throws DAOException
     */
    public long getCountMediaItemsUsingCategory(CMSCategory category) throws DAOException;

    /**
     * addCategory.
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return true if category added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCategory(CMSCategory category) throws DAOException;

    /**
     * updateCategory.
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return true if category updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCategory(CMSCategory category) throws DAOException;

    /**
     * deleteCategory.
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return true if category deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCategory(CMSCategory category) throws DAOException;

    /**
     * getCategoryByName.
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCategory getCategoryByName(String name) throws DAOException;

    /**
     * getCategory.
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCategory getCategory(Long id) throws DAOException;

    // Transkribus

    /**
     * getAllTranskribusJobs.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<TranskribusJob> getAllTranskribusJobs() throws DAOException;

    /**
     * getTranskribusJobs.
     *
     * @param pi a {@link java.lang.String} object.
     * @param transkribusUserId a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.job.JobStatus} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<TranskribusJob> getTranskribusJobs(String pi, String transkribusUserId, JobStatus status) throws DAOException;

    /**
     * addTranskribusJob.
     *
     * @param job a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @return true if job added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addTranskribusJob(TranskribusJob job) throws DAOException;

    /**
     * updateTranskribusJob.
     *
     * @param job a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @return true if job updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateTranskribusJob(TranskribusJob job) throws DAOException;

    /**
     * deleteTranskribusJob.
     *
     * @param job a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @return true if job deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteTranskribusJob(TranskribusJob job) throws DAOException;

    // Crowdsourcing campaigns

    /**
     * getAllCampaigns.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Campaign> getAllCampaigns() throws DAOException;

    /**
     * getCampaignCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCampaignCount(Map<String, String> filters) throws DAOException;

    /**
     * getCampaign.
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Campaign getCampaign(Long id) throws DAOException;

    /**
     * getQuestion.
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.crowdsourcing.questions.Question} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Question getQuestion(Long id) throws DAOException;

    /**
     * getCampaigns.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Campaign> getCampaigns(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getCampaignStatisticsForRecord.
     *
     * @param pi a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CampaignRecordStatistic> getCampaignStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException;

    /**
     * getCampaignPageStatisticsForRecord.
     *
     * @param pi a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    List<CampaignRecordPageStatistic> getCampaignPageStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException;

    /**
     * addCampaign.
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return true if campaign added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCampaign(Campaign campaign) throws DAOException;

    /**
     * updateCampaign.
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return true if campaign updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCampaign(Campaign campaign) throws DAOException;

    /**
     * deleteCampaign.
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return true if campaign deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCampaign(Campaign campaign) throws DAOException;

    /**
     * Deletes given user from the lists of annotators and reviewers an all campaign statistics.
     *
     * @param user
     * @return Number of affected campaigns
     * @throws DAOException
     */
    public int deleteCampaignStatisticsForUser(User user) throws DAOException;

    /**
     * Replaced <code>fromUser</code> with <code>toUser</code> in the lists of annotators and reviewers an all campaign statistics.
     *
     * @param fromUser
     * @param toUser
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
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Integer> getPagesWithComments(String pi) throws DAOException;

    /**
     * getCMSCollections.
     *
     * @param solrField a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCollection> getCMSCollections(String solrField) throws DAOException;

    /**
     * addCMSCollection.
     *
     * @param collection a {@link io.goobi.viewer.model.cms.collections.CMSCollection} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * updateCMSCollection.
     *
     * @param collection a {@link io.goobi.viewer.model.cms.collections.CMSCollection} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * deleteCMSCollection.
     *
     * @param collection a {@link io.goobi.viewer.model.cms.collections.CMSCollection} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * getCMSCollection.
     *
     * @param solrField a {@link java.lang.String} object.
     * @param solrFieldValue a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.collections.CMSCollection} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException;

    /**
     * Annotations *.
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CrowdsourcingAnnotation getAnnotation(Long id) throws DAOException;

    /**
     * getAnnotationsForCampaign.
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaign(Campaign campaign) throws DAOException;

    /**
     * getAnnotationsForWork.
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForWork(String pi) throws DAOException;

    /**
     * @param pi
     * @return Number of existing annotations for the given pi
     * @throws DAOException
     */
    long getAnnotationCountForWork(String pi) throws DAOException;

    /**
     * getAnnotationsForCampaignAndWork.
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaignAndWork(Campaign campaign, String pi) throws DAOException;

    /**
     * getAnnotationsForTarget.
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a {@link java.lang.Integer} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForTarget(String pi, Integer page) throws DAOException;

    public List<CrowdsourcingAnnotation> getAnnotationsForTarget(String pi, Integer page, String motivation) throws DAOException;

    /**
     *
     * @param userId
     * @param maxResults
     * @param sortField
     * @param descending
     * @return List of {@link CrowdsourcingAnnotation}s for the given userId
     * @throws DAOException
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForUserId(Long userId, Integer maxResults, String sortField, boolean descending)
            throws DAOException;

    /**
     * getAnnotations.
     *
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotations(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getAnnotationCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getAnnotationCount(Map<String, String> filters) throws DAOException;

    /**
     * getAnnotationCountForTarget.
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a {@link java.lang.Integer} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    long getAnnotationCountForTarget(String pi, Integer page) throws DAOException;

    /**
     * getAnnotationsForCampaignAndTarget.
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @param pi a {@link java.lang.String} object.
     * @param page a {@link java.lang.Integer} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaignAndTarget(Campaign campaign, String pi, Integer page) throws DAOException;

    /**
     * addAnnotation.
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * updateAnnotation.
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * deleteAnnotation.
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * Gets the {@link GeoMap} of the given mapId.
     *
     * @param mapId
     * @return The GeoMap of the given id or else null
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
     * @param map
     * @return true if successful
     * @throws DAOException
     */
    public boolean addGeoMap(GeoMap map) throws DAOException;

    /**
     * Updates the given {@link GeoMap} in the database.
     *
     * @param map
     * @return true if successful
     * @throws DAOException
     */
    public boolean updateGeoMap(GeoMap map) throws DAOException;

    /**
     * Deletes the given {@link GeoMap} from the database.
     *
     * @param map
     * @return true if successful
     * @throws DAOException
     */
    public boolean deleteGeoMap(GeoMap map) throws DAOException;

    /**
     * Returns a list of CMS-pages embedding the given map.
     *
     * @param map
     * @return List of {@link CMSPage}s that use given map
     * @throws DAOException
     */
    public List<CMSPage> getPagesUsingMap(GeoMap map) throws DAOException;

    /**
     * Returns a list of CMS-pages embedding the given map in a sidebar widget.
     *
     * @param map
     * @return List of {@link CMSPage}s that use given map in sidebar
     * @throws DAOException
     */
    public List<CMSPage> getPagesUsingMapInSidebar(GeoMap map) throws DAOException;

    /**
     * @param subtheme
     * @return List of {@link CMSPage}s that use given subtheme
     * @throws DAOException
     */
    List<CMSPage> getCMSPagesForSubtheme(String subtheme) throws DAOException;

    /**
     * Gets a paginated list of {@link CMSRecordNote}s.
     *
     * @param first
     * @param pageSize
     * @param sortField
     * @param descending
     * @param filters
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
     * @param displayedNotesOnly
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
     * @param id
     * @return {@link CMSSingleRecordNote} with the given id
     */
    public CMSRecordNote getRecordNote(Long id) throws DAOException;

    /**
     * Persist a new {@link CMSRecordNote}.
     *
     * @param note
     * @return true if note added successfully; false otherwise
     */
    public boolean addRecordNote(CMSRecordNote note) throws DAOException;

    /**
     * Updates an existing {@link CMSRecordNote}
     *
     * @param note
     * @return true if note updated successfully; false otherwise
     */
    public boolean updateRecordNote(CMSRecordNote note) throws DAOException;

    /**
     * Deletes an existing {@link CMSRecordNote}
     *
     * @param note
     * @return true if note deleted successfully; false otherwise
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
     * @param first
     * @param pageSize
     * @param sortField
     * @param descending
     * @param filterString
     * @param filterParams
     * @return List of {@link CrowdsourcingAnnotation}s matching given filters
     * @throws DAOException
     */
    public List<CrowdsourcingAnnotation> getAnnotations(int first, int pageSize, String sortField, boolean descending, String filterString,
            Map<String, Object> filterParams) throws DAOException;

    /**
     * @param commenting
     * @return List of {@link CrowdsourcingAnnotation}s matching given commenting
     * @throws DAOException
     */
    public List<CrowdsourcingAnnotation> getAllAnnotationsByMotivation(String commenting) throws DAOException;

    /**
     * @param sortField
     * @param sortDescending
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
     * @param first a int.
     * @param pageSize a int.
     * @param sortField a {@link java.lang.String} object.
     * @param descending a boolean.
     * @param filters a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<ViewerMessage> getViewerMessages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * getViewerMessageCount.
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
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
     * @return a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public MaintenanceMode getMaintenanceMode() throws DAOException;

    /**
     * updateMaintenanceMode.
     *
     * @param maintenanceMode a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateMaintenanceMode(MaintenanceMode maintenanceMode) throws DAOException;

    /**
     * Get the EntityManagerFactory created when initializing the class. Can be used to explicitly create new EntityManagers.
     *
     * @return the EntityManagerFactory
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
