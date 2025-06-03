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
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.quartz.RecurringTaskTrigger;
import io.goobi.viewer.model.job.upload.UploadJob;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.security.DownloadTicket;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.clients.ClientApplication;
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
import net.sf.saxon.trans.LicenseException;

/**
 * <p>
 * IDAO interface.
 * </p>
 */
public interface IDAO {

    /**
     * <p>
     * tableExists.
     * </p>
     *
     * @param tableName a {@link java.lang.String} object.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     * @throws DAOException
     */
    boolean tableExists(String tableName) throws DAOException, SQLException;

    /**
     * <p>
     * columnsExists.
     * </p>
     *
     * @param tableName a {@link java.lang.String} object.
     * @param columnName a {@link java.lang.String} object.
     * @return a boolean.
     * @throws java.sql.SQLException if any.
     */
    boolean columnsExists(String tableName, String columnName) throws DAOException, SQLException;

    // User

    /**
     * <p>
     * getAllUsers.
     * </p>
     *
     * @param refresh a boolean.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getAllUsers(boolean refresh) throws DAOException;

    /**
     * <p>
     * getUserCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getUsers.
     * </p>
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
     * <p>
     * getUser.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUser(long id) throws DAOException;

    /**
     * <p>
     * getUserByEmail.
     * </p>
     *
     * @param email a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByEmail(String email) throws DAOException;

    /**
     * <p>
     * getUserByOpenId.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByOpenId(String identifier) throws DAOException;

    /**
     * <p>
     * getUserByNickname.
     * </p>
     *
     * @param nickname a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getUserByNickname(String nickname) throws DAOException;

    /**
     * <p>
     * addUser.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUser(User user) throws DAOException;

    /**
     * <p>
     * updateUser.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUser(User user) throws DAOException;

    /**
     * <p>
     * deleteUser.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUser(User user) throws DAOException;

    // UserGroup

    /**
     * <p>
     * getAllUserGroups.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getAllUserGroups() throws DAOException;

    /**
     * <p>
     * getUserGroupCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserGroupCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getUserGroups.
     * </p>
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
     * <p>
     * getUserGroups.
     * </p>
     *
     * @param owner a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getUserGroups(User owner) throws DAOException;

    /**
     * <p>
     * getUserGroup.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public UserGroup getUserGroup(long id) throws DAOException;

    /**
     * <p>
     * getUserGroup.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public UserGroup getUserGroup(String name) throws DAOException;

    /**
     * <p>
     * addUserGroup.
     * </p>
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUserGroup(UserGroup userGroup) throws DAOException;

    /**
     * <p>
     * updateUserGroup.
     * </p>
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUserGroup(UserGroup userGroup) throws DAOException;

    /**
     * <p>
     * deleteUserGroup.
     * </p>
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUserGroup(UserGroup userGroup) throws DAOException;

    // Bookmarks

    /**
     * <p>
     * getAllBookmarkLists.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getAllBookmarkLists() throws DAOException;

    /**
     * <p>
     * getPublicBookmarkLists.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getPublicBookmarkLists() throws DAOException;

    /**
     * <p>
     * getBookmarkLists.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<BookmarkList> getBookmarkLists(User user) throws DAOException;

    /**
     * Get number of bookmark lists owned by the given user
     *
     * @param user
     * @return number of owned bookmark lists
     * @throws DAOException
     */
    long getBookmarkListCount(User user) throws DAOException;

    /**
     * <p>
     * getBookmarkList.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkList(long id) throws DAOException;

    /**
     * <p>
     * getBookmarkList.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkList(String name, User user) throws DAOException;

    /**
     * <p>
     * getBookmarkListByShareKey.
     * </p>
     *
     * @param shareKey a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public BookmarkList getBookmarkListByShareKey(String shareKey) throws DAOException;

    /**
     * <p>
     * addBookmarkList.
     * </p>
     *
     * @param bookmarkList a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addBookmarkList(BookmarkList bookmarkList) throws DAOException;

    /**
     * <p>
     * updateBookmarkList.
     * </p>
     *
     * @param bookmarkList a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateBookmarkList(BookmarkList bookmarkList) throws DAOException;

    /**
     * <p>
     * deleteBookmarkList.
     * </p>
     *
     * @param bookmarkList a {@link io.goobi.viewer.model.bookmark.BookmarkList} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteBookmarkList(BookmarkList bookmarkList) throws DAOException;

    // Role

    /**
     * <p>
     * getAllRoles.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Role> getAllRoles() throws DAOException;

    /**
     * <p>
     * getRoleCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getRoleCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getRoles.
     * </p>
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
     * <p>
     * getRole.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.Role} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Role getRole(long id) throws DAOException;

    /**
     * <p>
     * getRole.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.Role} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Role getRole(String name) throws DAOException;

    /**
     * <p>
     * addRole.
     * </p>
     *
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addRole(Role role) throws DAOException;

    /**
     * <p>
     * updateRole.
     * </p>
     *
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateRole(Role role) throws DAOException;

    /**
     * <p>
     * deleteRole.
     * </p>
     *
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteRole(Role role) throws DAOException;

    // UserRole

    /**
     * <p>
     * getAllUserRoles.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserRole> getAllUserRoles() throws DAOException;

    /**
     * <p>
     * getUserRoleCount.
     * </p>
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return Row count
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getUserRoleCount(UserGroup userGroup, User user, Role role) throws DAOException;

    /**
     * <p>
     * getUserRoles.
     * </p>
     *
     * @param userGroup a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param role a {@link io.goobi.viewer.model.security.Role} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserRole> getUserRoles(UserGroup userGroup, User user, Role role) throws DAOException;

    /**
     * <p>
     * addUserRole.
     * </p>
     *
     * @param userRole a {@link io.goobi.viewer.model.security.user.UserRole} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUserRole(UserRole userRole) throws DAOException;

    /**
     * <p>
     * updateUserRole.
     * </p>
     *
     * @param userRole a {@link io.goobi.viewer.model.security.user.UserRole} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUserRole(UserRole userRole) throws DAOException;

    /**
     * <p>
     * deleteUserRole.
     * </p>
     *
     * @param userRole a {@link io.goobi.viewer.model.security.user.UserRole} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUserRole(UserRole userRole) throws DAOException;

    // LicenseType

    /**
     * <p>
     * getAllLicenseTypes.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllLicenseTypes() throws DAOException;

    /**
     * <p>
     * getLicenseTypeCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getLicenseTypeCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getCoreLicenseTypeCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCoreLicenseTypeCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getRecordLicenseTypes.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getRecordLicenseTypes() throws DAOException;

    /**
     * <p>
     * getLicenseTypes.
     * </p>
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
     * <p>
     * getCoreLicenseTypes.
     * </p>
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
     * <p>
     * getLicenseType.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public LicenseType getLicenseType(long id) throws DAOException;

    /**
     * <p>
     * getLicenseType.
     * </p>
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
     * <p>
     * addLicenseType.
     * </p>
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addLicenseType(LicenseType licenseType) throws DAOException;

    /**
     * <p>
     * updateLicenseType.
     * </p>
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateLicenseType(LicenseType licenseType) throws DAOException;

    /**
     * <p>
     * deleteLicenseType.
     * </p>
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteLicenseType(LicenseType licenseType) throws DAOException;

    // License

    /**
     * <p>
     * getAllLicenses.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<License> getAllLicenses() throws DAOException;

    /**
     * <p>
     * getLicense.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.License} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public License getLicense(Long id) throws DAOException;

    /**
     *
     * @param licenseType
     * @return List of {@link LicenseException}s of the given licenseType
     * @throws DAOException
     */
    public List<License> getLicenses(LicenseType licenseType) throws DAOException;

    /**
     * Returns the number of licenses that use the given license type.
     *
     * @param licenseType
     * @return Number of existing {@link License}s of the given licenseType
     * @throws DAOException
     */
    public long getLicenseCount(LicenseType licenseType) throws DAOException;

    // DownloadTicket

    /**
     * 
     * @param id
     * @return {@link DownloadTicket} with the given id
     * @throws DAOException
     */
    public DownloadTicket getDownloadTicket(Long id) throws DAOException;

    /**
     * 
     * @param passwordHash
     * @return {@link DownloadTicket} with the given passwordHash
     * @throws DAOException
     */
    public DownloadTicket getDownloadTicketByPasswordHash(String passwordHash) throws DAOException;

    /**
     * <p>
     * getActiveDownloadTicketCount.
     * </p>
     *
     * @param filters Selected filters
     * @return Number of found rows
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getActiveDownloadTicketCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getActiveDownloadTickets.
     * </p>
     *
     * @param first First row index
     * @param pageSize Number of rows
     * @param sortField a {@link java.lang.String} object.
     * @param descending true if descending order requested; false otherwise
     * @param filters Selected filters
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<DownloadTicket> getActiveDownloadTickets(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    /**
     * 
     * @return {@link DownloadTicket}s with the requested status
     * @throws DAOException
     */
    public List<DownloadTicket> getDownloadTicketRequests() throws DAOException;

    /**
     * <p>
     * addDownloadTicket.
     * </p>
     *
     * @param downloadTicket a {@link io.goobi.viewer.model.security.DownloadTicket} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addDownloadTicket(DownloadTicket downloadTicket) throws DAOException;

    /**
     * <p>
     * updateDownloadTicket.
     * </p>
     *
     * @param downloadTicket a {@link io.goobi.viewer.model.security.DownloadTicket} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateDownloadTicket(DownloadTicket downloadTicket) throws DAOException;

    /**
     * <p>
     * deleteDownloadTicket.
     * </p>
     *
     * @param downloadTicket a {@link io.goobi.viewer.model.security.DownloadTicket} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteDownloadTicket(DownloadTicket downloadTicket) throws DAOException;

    // IpRange

    /**
     * <p>
     * getAllIpRanges.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<IpRange> getAllIpRanges() throws DAOException;

    /**
     * <p>
     * getIpRangeCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getIpRangeCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getIpRanges.
     * </p>
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
     * <p>
     * getIpRange.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public IpRange getIpRange(long id) throws DAOException;

    /**
     * <p>
     * getIpRange.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public IpRange getIpRange(String name) throws DAOException;

    /**
     * <p>
     * addIpRange.
     * </p>
     *
     * @param ipRange a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addIpRange(IpRange ipRange) throws DAOException;

    /**
     * <p>
     * updateIpRange.
     * </p>
     *
     * @param ipRange a {@link io.goobi.viewer.model.security.user.IpRange} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateIpRange(IpRange ipRange) throws DAOException;

    /**
     * <p>
     * deleteIpRange.
     * </p>
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
     * <p>
     * getCommentGroup.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CommentGroup getCommentGroup(long id) throws DAOException;

    /**
     * <p>
     * addCommentGroup.
     * </p>
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCommentGroup(CommentGroup commentGroup) throws DAOException;

    /**
     * <p>
     * updateCommentGroup.
     * </p>
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCommentGroup(CommentGroup commentGroup) throws DAOException;

    /**
     * <p>
     * deleteCommentGroup.
     * </p>
     *
     * @param commentGroup a {@link io.goobi.viewer.model.annotation.comments.CommentGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCommentGroup(CommentGroup commentGroup) throws DAOException;

    // Comment

    /**
     * <p>
     * getAllComments.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getAllComments() throws DAOException;

    /**
     * <p>
     * getCommentCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @param owner
     * @param targetPIs
     * @return Number of rows that match the criteria
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCommentCount(Map<String, String> filters, User owner, Set<String> targetPIs) throws DAOException;

    /**
     * <p>
     * getComments.
     * </p>
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
     * Get Comments created by a specific user
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
     * <p>
     * getCommentsForPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getCommentsForPage(String pi, int page) throws DAOException;

    /**
     * <p>
     * getCommentsForWork.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getCommentsForWork(String pi) throws DAOException;

    /**
     * <p>
     * getComment.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Comment getComment(long id) throws DAOException;

    /**
     * <p>
     * addComment.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addComment(Comment comment) throws DAOException;

    /**
     * <p>
     * updateComment.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.comments.Comment} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateComment(Comment comment) throws DAOException;

    /**
     * <p>
     * deleteComment.
     * </p>
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
     * <p>
     * getAllSearches.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getAllSearches() throws DAOException;

    /**
     * <p>
     * getSearchCount.
     * </p>
     *
     * @param owner a {@link io.goobi.viewer.model.security.user.User} object.
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getSearchCount(User owner, Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getSearches.
     * </p>
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
     * <p>
     * getSearches.
     * </p>
     *
     * @param owner a {@link io.goobi.viewer.model.security.user.User} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getSearches(User owner) throws DAOException;

    /**
     * <p>
     * getSearch.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.search.Search} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Search getSearch(long id) throws DAOException;

    /**
     * <p>
     * addSearch.
     * </p>
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addSearch(Search search) throws DAOException;

    /**
     * <p>
     * updateSearch.
     * </p>
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateSearch(Search search) throws DAOException;

    /**
     * <p>
     * deleteSearch.
     * </p>
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteSearch(Search search) throws DAOException;

    // Download jobs

    /**
     * <p>
     * getAllDownloadJobs.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<DownloadJob> getAllDownloadJobs() throws DAOException;

    /**
     * <p>
     * getDownloadJobsForPi.
     * </p>
     *
     * @param pi Record identifier
     * @return List of {@link DownloadJob}s for given record identfier
     * @throws DAOException
     */
    public List<DownloadJob> getDownloadJobsForPi(String pi) throws DAOException;

    /**
     * <p>
     * getDownloadJob.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public DownloadJob getDownloadJob(long id) throws DAOException;

    /**
     * <p>
     * getDownloadJobByIdentifier.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public DownloadJob getDownloadJobByIdentifier(String identifier) throws DAOException;

    /**
     * <p>
     * getDownloadJobByMetadata.
     * </p>
     *
     * @param type a {@link java.lang.String} object.
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public DownloadJob getDownloadJobByMetadata(String type, String pi, String logId) throws DAOException;

    /**
     * <p>
     * addDownloadJob.
     * </p>
     *
     * @param downloadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addDownloadJob(DownloadJob downloadJob) throws DAOException;

    /**
     * <p>
     * updateDownloadJob.
     * </p>
     *
     * @param downloadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateDownloadJob(DownloadJob downloadJob) throws DAOException;

    /**
     * <p>
     * deleteDownloadJob.
     * </p>
     *
     * @param downloadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteDownloadJob(DownloadJob downloadJob) throws DAOException;

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
     * <p>
     * addDownloadJob.
     * </p>
     *
     * @param uploadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addUploadJob(UploadJob uploadJob) throws DAOException;

    /**
     * <p>
     * updateDownloadJob.
     * </p>
     *
     * @param uploadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateUploadJob(UploadJob uploadJob) throws DAOException;

    /**
     * <p>
     * deleteDownloadJob.
     * </p>
     *
     * @param uploadJob a {@link io.goobi.viewer.model.job.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteUploadJob(UploadJob uploadJob) throws DAOException;

    // CMS
    /**
     * <p>
     * getAllCMSPages.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getAllCMSPages() throws DAOException;

    /**
     * <p>
     * getCmsPageForStaticPage.
     * </p>
     *
     * @param pageName a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCmsPageForStaticPage(String pageName) throws DAOException;

    /**
     * <p>
     * getCMSPageCount.
     * </p>
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
     * <p>
     * getCMSPages.
     * </p>
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
     * <p>
     * getCMSPagesByCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesByCategory(CMSCategory category) throws DAOException;

    /**
     * <p>
     * getCMSPagesForRecord.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesForRecord(String pi, CMSCategory category) throws DAOException;

    /**
     * <p>
     * getCMSPagesWithRelatedPi.
     * </p>
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
     * <p>
     * isCMSPagesForRecordHaveUpdates.
     * </p>
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
     * <p>
     * getCMSPageWithRelatedPiCount.
     * </p>
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
     * <p>
     * getCMSPage.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCMSPage(long id) throws DAOException;

    /**
     * <p>
     * addCMSPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSPage(CMSPage page) throws DAOException;

    /**
     * <p>
     * updateCMSPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSPage(CMSPage page) throws DAOException;

    /**
     * <p>
     * deleteCMSPage.
     * </p>
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
     * <p>
     * getAllCMSMediaItems.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException;

    /**
     * <p>
     * getAllCMSCollectionItems.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSMediaItem> getAllCMSCollectionItems() throws DAOException;

    /**
     * <p>
     * getCMSMediaItem.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSMediaItem getCMSMediaItem(long id) throws DAOException;

    /**
     * <p>
     * getCMSMediaItemByFilename.
     * </p>
     *
     * @param string a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    CMSMediaItem getCMSMediaItemByFilename(String string) throws DAOException;

    /**
     * <p>
     * addCMSMediaItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * <p>
     * updateCMSMediaItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * <p>
     * deleteCMSMediaItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * Get a list of all {@link CMSMediaItem}s which contain the given category
     *
     * @param category
     * @return all containing cmsPages
     * @throws DAOException
     */
    List<CMSMediaItem> getCMSMediaItemsByCategory(CMSCategory category) throws DAOException;

    /**
     * <p>
     * getAllTopCMSNavigationItems.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSNavigationItem> getAllTopCMSNavigationItems() throws DAOException;

    /**
     * <p>
     * getCMSNavigationItem.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSNavigationItem getCMSNavigationItem(long id) throws DAOException;

    /**
     * <p>
     * addCMSNavigationItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * <p>
     * updateCMSNavigationItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * <p>
     * deleteCMSNavigationItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSNavigationItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    /**
     * <p>
     * getRelatedNavItem.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSNavigationItem> getRelatedNavItem(CMSPage page) throws DAOException;

    /**
     * <p>
     * getAllStaticPages.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSStaticPage> getAllStaticPages() throws DAOException;

    /**
     * <p>
     * addStaticPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSStaticPage} object.
     * @return true if page added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * <p>
     * updateStaticPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSStaticPage} object.
     * @return true if page updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * <p>
     * deleteStaticPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSStaticPage} object.
     * @return true if page deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * <p>
     * getStaticPageForCMSPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.pages.CMSPage} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSStaticPage> getStaticPageForCMSPage(CMSPage page) throws DAOException;

    /**
     * <p>
     * getStaticPageForTypeType.
     * </p>
     *
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return a {@link java.util.Optional} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException;

    /**
     * <p>
     * getAllCategories.
     * </p>
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
     * <p>
     * addCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return true if category added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCategory(CMSCategory category) throws DAOException;

    /**
     * <p>
     * updateCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return true if category updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCategory(CMSCategory category) throws DAOException;

    /**
     * <p>
     * deleteCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return true if category deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCategory(CMSCategory category) throws DAOException;

    /**
     * <p>
     * getCategoryByName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCategory getCategoryByName(String name) throws DAOException;

    /**
     * <p>
     * getCategory.
     * </p>
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCategory getCategory(Long id) throws DAOException;

    // Transkribus

    /**
     * <p>
     * getAllTranskribusJobs.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<TranskribusJob> getAllTranskribusJobs() throws DAOException;

    /**
     * <p>
     * getTranskribusJobs.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param transkribusUserId a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.job.JobStatus} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<TranskribusJob> getTranskribusJobs(String pi, String transkribusUserId, JobStatus status) throws DAOException;

    /**
     * <p>
     * addTranskribusJob.
     * </p>
     *
     * @param job a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @return true if job added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addTranskribusJob(TranskribusJob job) throws DAOException;

    /**
     * <p>
     * updateTranskribusJob.
     * </p>
     *
     * @param job a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @return true if job updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateTranskribusJob(TranskribusJob job) throws DAOException;

    /**
     * <p>
     * deleteTranskribusJob.
     * </p>
     *
     * @param job a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @return true if job deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteTranskribusJob(TranskribusJob job) throws DAOException;

    // Crowdsourcing campaigns

    /**
     * <p>
     * getAllCampaigns.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Campaign> getAllCampaigns() throws DAOException;

    /**
     * <p>
     * getCampaignCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCampaignCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getCampaign.
     * </p>
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Campaign getCampaign(Long id) throws DAOException;

    /**
     * <p>
     * getQuestion.
     * </p>
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.crowdsourcing.questions.Question} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Question getQuestion(Long id) throws DAOException;

    /**
     * <p>
     * getCampaigns.
     * </p>
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
     * <p>
     * getCampaignStatisticsForRecord.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CampaignRecordStatistic> getCampaignStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException;

    /**
     * <p>
     * getCampaignPageStatisticsForRecord.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    List<CampaignRecordPageStatistic> getCampaignPageStatisticsForRecord(String pi, CrowdsourcingStatus status) throws DAOException;

    /**
     * <p>
     * addCampaign.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return true if campaign added successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCampaign(Campaign campaign) throws DAOException;

    /**
     * <p>
     * updateCampaign.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return true if campaign updated successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCampaign(Campaign campaign) throws DAOException;

    /**
     * <p>
     * deleteCampaign.
     * </p>
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
     * <p>
     * shutdown.
     * </p>
     */
    public void shutdown();

    /**
     * <p>
     * getPagesWithComments.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Integer> getPagesWithComments(String pi) throws DAOException;

    /**
     * <p>
     * getCMSCollections.
     * </p>
     *
     * @param solrField a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCollection> getCMSCollections(String solrField) throws DAOException;

    /**
     * <p>
     * addCMSCollection.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.cms.collections.CMSCollection} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * <p>
     * updateCMSCollection.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.cms.collections.CMSCollection} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * <p>
     * deleteCMSCollection.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.cms.collections.CMSCollection} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * <p>
     * getCMSCollection.
     * </p>
     *
     * @param solrField a {@link java.lang.String} object.
     * @param solrFieldValue a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.collections.CMSCollection} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException;

    /**
     * Annotations *
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CrowdsourcingAnnotation getAnnotation(Long id) throws DAOException;

    /**
     * <p>
     * getAnnotationsForCampaign.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaign(Campaign campaign) throws DAOException;

    /**
     * <p>
     * getAnnotationsForWork.
     * </p>
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
     * <p>
     * getAnnotationsForCampaignAndWork.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaignAndWork(Campaign campaign, String pi) throws DAOException;

    /**
     * <p>
     * getAnnotationsForTarget.
     * </p>
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
     * <p>
     * getAnnotations.
     * </p>
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
     * <p>
     * getAnnotationCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getAnnotationCount(Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getAnnotationCountForTarget.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a {@link java.lang.Integer} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    long getAnnotationCountForTarget(String pi, Integer page) throws DAOException;

    /**
     * <p>
     * getAnnotationsForCampaignAndTarget.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @param pi a {@link java.lang.String} object.
     * @param page a {@link java.lang.Integer} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getAnnotationsForCampaignAndTarget(Campaign campaign, String pi, Integer page) throws DAOException;

    /**
     * <p>
     * addAnnotation.
     * </p>
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * <p>
     * updateAnnotation.
     * </p>
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * <p>
     * deleteAnnotation.
     * </p>
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteAnnotation(CrowdsourcingAnnotation annotation) throws DAOException;

    /**
     * Get the {@link GeoMap} of the given mapId
     *
     * @param mapId
     * @return The GeoMap of the given id or else null
     */
    public GeoMap getGeoMap(Long mapId) throws DAOException;

    /**
     * Get all {@link GeoMap}s in database
     *
     * @return A list of all stored GeoMaps
     * @throws DAOException
     */
    public List<GeoMap> getAllGeoMaps() throws DAOException;

    /**
     * Add the given map to the database if no map of the same id already exists
     *
     * @param map
     * @return true if successful
     * @throws DAOException
     */
    public boolean addGeoMap(GeoMap map) throws DAOException;

    /**
     * Update the given {@link GeoMap} in the database
     *
     * @param map
     * @return true if successful
     * @throws DAOException
     */
    public boolean updateGeoMap(GeoMap map) throws DAOException;

    /**
     * Delete the given {@link GeoMap} from the database
     *
     * @param map
     * @return true if successful
     * @throws DAOException
     */
    public boolean deleteGeoMap(GeoMap map) throws DAOException;

    /**
     * Return a list of CMS-pages embedding the given map
     *
     * @param map
     * @return List of {@link CMSPage}s that use given map
     * @throws DAOException
     */
    public List<CMSPage> getPagesUsingMap(GeoMap map) throws DAOException;

    /**
     * Return a list of CMS-pages embedding the given map in a sidebar widget
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
     * Get a paginated list of {@link CMSRecordNote}s
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
     * Get all {@link CMSRecordNote}s for the given pi
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
     * Get all persisted {@link CMSRecordNote}s
     *
     * @return List of all existing {@link CMSSingleRecordNote}s
     * @throws DAOException
     */
    public List<CMSRecordNote> getAllRecordNotes() throws DAOException;

    /**
     * Get a {@link CMSRecordNote} by its id property
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
     * <p>
     * getViewerMessages.
     * </p>
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
     * <p>
     * getViewerMessageCount.
     * </p>
     *
     * @param filters a {@link java.util.Map} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getViewerMessageCount(Map<String, String> filters) throws DAOException;

    public List<RecurringTaskTrigger> getRecurringTaskTriggers() throws DAOException;

    public RecurringTaskTrigger getRecurringTaskTrigger(Long id) throws DAOException;

    public RecurringTaskTrigger getRecurringTaskTriggerForTask(TaskType task) throws DAOException;

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
     * <p>
     * updateMaintenanceMode.
     * </p>
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
     * the transaction
     * 
     * @param em EntityManager
     * @return the transaction gotten from the entity manager
     */
    EntityTransaction startTransaction(EntityManager em);

    /**
     * Call {@link EntityTransaction#commit()} on the given transaction
     *
     * @param et EntityTransaction
     * @throws PersistenceException
     */
    void commitTransaction(EntityTransaction et) throws PersistenceException;

    /**
     * Call {@link EntityTransaction#commit()} on the current transaction of the given EntityManager
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
     * current transaction of the given EntityManager
     *
     * @param em EntityManager
     * @throws PersistenceException
     */
    void handleException(EntityManager em);

}
