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
package io.goobi.viewer.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.Query;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSCollection;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSPageTemplate;
import io.goobi.viewer.model.cms.CMSPageTemplateEnabled;
import io.goobi.viewer.model.cms.CMSSidebarElement;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.download.DownloadJob;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;
import io.goobi.viewer.model.transkribus.TranskribusJob;
import io.goobi.viewer.model.transkribus.TranskribusJob.JobStatus;
import io.goobi.viewer.model.viewer.PageType;

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
     */
    boolean tableExists(String tableName) throws SQLException;

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
    boolean columnsExists(String tableName, String columnName) throws SQLException;

    /**
     * <p>
     * startTransaction.
     * </p>
     */
    void startTransaction();

    /**
     * <p>
     * commitTransaction.
     * </p>
     */
    void commitTransaction();

    /**
     * <p>
     * createNativeQuery.
     * </p>
     *
     * @param string a {@link java.lang.String} object.
     * @return a {@link javax.persistence.Query} object.
     */
    Query createNativeQuery(String string);

    /**
     * <p>
     * createQuery.
     * </p>
     *
     * @param string a {@link java.lang.String} object.
     * @return a {@link javax.persistence.Query} object.
     */
    Query createQuery(String string);

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
     * getNonOpenAccessLicenseTypes.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getNonOpenAccessLicenseTypes() throws DAOException;

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
     * @return a {@link io.goobi.viewer.model.security.getLicense} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public License getLicense(Long id) throws DAOException;

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
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCommentCount(Map<String, String> filters) throws DAOException;

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
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getComments(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    /**
     * <p>
     * getCommentsForPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @param topLevelOnly a boolean.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getCommentsForPage(String pi, int page, boolean topLevelOnly) throws DAOException;

    /**
     * <p>
     * getCommentsForWork.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param topLevelOnly a boolean.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getCommentsForWork(String pi, boolean topLevelOnly) throws DAOException;

    /**
     * <p>
     * getComment.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.annotation.Comment} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Comment getComment(long id) throws DAOException;

    /**
     * <p>
     * addComment.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.Comment} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addComment(Comment comment) throws DAOException;

    /**
     * <p>
     * updateComment.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.Comment} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateComment(Comment comment) throws DAOException;

    /**
     * <p>
     * deleteComment.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.Comment} object.
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
     * getDownloadJob.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.download.DownloadJob} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public DownloadJob getDownloadJob(long id) throws DAOException;

    /**
     * <p>
     * getDownloadJobByIdentifier.
     * </p>
     *
     * @param identifier a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.download.DownloadJob} object.
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
     * @return a {@link io.goobi.viewer.model.download.DownloadJob} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public DownloadJob getDownloadJobByMetadata(String type, String pi, String logId) throws DAOException;

    /**
     * <p>
     * addDownloadJob.
     * </p>
     *
     * @param downloadJob a {@link io.goobi.viewer.model.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addDownloadJob(DownloadJob downloadJob) throws DAOException;

    /**
     * <p>
     * updateDownloadJob.
     * </p>
     *
     * @param downloadJob a {@link io.goobi.viewer.model.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateDownloadJob(DownloadJob downloadJob) throws DAOException;

    /**
     * <p>
     * deleteDownloadJob.
     * </p>
     *
     * @param downloadJob a {@link io.goobi.viewer.model.download.DownloadJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteDownloadJob(DownloadJob downloadJob) throws DAOException;

    // CMS

    public CMSPageTemplateEnabled getCMSPageTemplateEnabled(String templateId) throws DAOException;

    public boolean addCMSPageTemplateEnabled(CMSPageTemplateEnabled o) throws DAOException;

    public boolean updateCMSPageTemplateEnabled(CMSPageTemplateEnabled o) throws DAOException;

    public int saveCMSPageTemplateEnabledStatuses(List<CMSPageTemplate> templates) throws DAOException;

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
     * @return a {@link io.goobi.viewer.model.cms.CMSPage} object.
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
    public long getCMSPageCount(Map<String, String> filters, List<String> allowedTemplates, List<String> allowedSubthemes,
            List<String> allowedCategories) throws DAOException;

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
            List<String> allowedTemplates, List<String> allowedSubthemes, List<String> allowedCategories) throws DAOException;

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
     * @param fromDate a {@link java.util.Date} object.
     * @param toDate a {@link java.util.Date} object.
     * @param templateIds Optional list of template IDs for filtering.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesWithRelatedPi(int first, int pageSize, Date fromDate, Date toDate, List<String> templateIds) throws DAOException;

    /**
     * <p>
     * isCMSPagesForRecordHaveUpdates.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @param fromDate a {@link java.util.Date} object.
     * @param toDate a {@link java.util.Date} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isCMSPagesForRecordHaveUpdates(String pi, CMSCategory category, Date fromDate, Date toDate) throws DAOException;

    /**
     * <p>
     * getCMSPageWithRelatedPiCount.
     * </p>
     *
     * @param fromDate a {@link java.util.Date} object.
     * @param toDate a {@link java.util.Date} object.
     * @param templateIds Optional list of template IDs for filtering.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCMSPageWithRelatedPiCount(Date fromDate, Date toDate, List<String> templateIds) throws DAOException;

    /**
     * <p>
     * getCMSPage.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.CMSPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCMSPage(long id) throws DAOException;

    /**
     * <p>
     * addCMSPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSPage} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSPage(CMSPage page) throws DAOException;

    /**
     * <p>
     * updateCMSPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSPage} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSPage(CMSPage page) throws DAOException;

    /**
     * <p>
     * deleteCMSPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSPage} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSPage(CMSPage page) throws DAOException;

    /**
     * <p>
     * getCMSSidebarElement.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.CMSSidebarElement} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSSidebarElement getCMSSidebarElement(long id) throws DAOException;

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
     * @return a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSMediaItem getCMSMediaItem(long id) throws DAOException;

    /**
     * <p>
     * getCMSMediaItemByFilename.
     * </p>
     *
     * @param string a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    CMSMediaItem getCMSMediaItemByFilename(String string) throws DAOException;

    /**
     * <p>
     * addCMSMediaItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * <p>
     * updateCMSMediaItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * <p>
     * deleteCMSMediaItem.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteCMSMediaItem(CMSMediaItem item) throws DAOException;

    /**
     * <p>
     * getMediaOwners.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.CMSMediaItem} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getMediaOwners(CMSMediaItem item) throws DAOException;

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
     * @param page a {@link io.goobi.viewer.model.cms.CMSPage} object.
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
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * <p>
     * updateStaticPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSStaticPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void updateStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * <p>
     * deleteStaticPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSStaticPage} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteStaticPage(CMSStaticPage page) throws DAOException;

    /**
     * <p>
     * getStaticPageForCMSPage.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.cms.CMSPage} object.
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
     * @return
     * @throws DAOException
     */
    public long getCountPagesUsingCategory(CMSCategory category) throws DAOException;

    /**
     * 
     * @param category
     * @return
     * @throws DAOException
     */
    public long getCountMediaItemsUsingCategory(CMSCategory category) throws DAOException;

    /**
     * <p>
     * addCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addCategory(CMSCategory category) throws DAOException;

    /**
     * <p>
     * updateCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void updateCategory(CMSCategory category) throws DAOException;

    /**
     * <p>
     * deleteCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @return a boolean.
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
     * @param status a {@link io.goobi.viewer.model.transkribus.TranskribusJob.JobStatus} object.
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
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addTranskribusJob(TranskribusJob job) throws DAOException;

    /**
     * <p>
     * updateTranskribusJob.
     * </p>
     *
     * @param job a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateTranskribusJob(TranskribusJob job) throws DAOException;

    /**
     * <p>
     * deleteTranskribusJob.
     * </p>
     *
     * @param job a {@link io.goobi.viewer.model.transkribus.TranskribusJob} object.
     * @return a boolean.
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
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CampaignRecordStatistic> getCampaignStatisticsForRecord(String pi, CampaignRecordStatus status) throws DAOException;

    /**
     * <p>
     * addCampaign.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCampaign(Campaign campaign) throws DAOException;

    /**
     * <p>
     * updateCampaign.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCampaign(Campaign campaign) throws DAOException;

    /**
     * <p>
     * deleteCampaign.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a boolean.
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

    // Misc

    /**
     * <p>
     * shutdown.
     * </p>
     */
    public void shutdown();

    /**
     * <p>
     * getCMSPageForEditing.
     * </p>
     *
     * @param id a long.
     * @return a {@link io.goobi.viewer.model.cms.CMSPage} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCMSPageForEditing(long id) throws DAOException;

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
     * @param collection a {@link io.goobi.viewer.model.cms.CMSCollection} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * <p>
     * updateCMSCollection.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.cms.CMSCollection} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException;

    /**
     * <p>
     * deleteCMSCollection.
     * </p>
     *
     * @param collection a {@link io.goobi.viewer.model.cms.CMSCollection} object.
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
     * @return a {@link io.goobi.viewer.model.cms.CMSCollection} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException;

    /**
     * Annotations *
     *
     * @param id a {@link java.lang.Long} object.
     * @return a {@link io.goobi.viewer.model.annotation.PersistentAnnotation} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PersistentAnnotation getAnnotation(Long id) throws DAOException;

    /**
     * <p>
     * getAnnotationsForCampaign.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<PersistentAnnotation> getAnnotationsForCampaign(Campaign campaign) throws DAOException;

    /**
     * <p>
     * getAnnotationsForWork.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<PersistentAnnotation> getAnnotationsForWork(String pi) throws DAOException;

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
    public List<PersistentAnnotation> getAnnotationsForCampaignAndWork(Campaign campaign, String pi) throws DAOException;

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
    public List<PersistentAnnotation> getAnnotationsForTarget(String pi, Integer page) throws DAOException;

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
    public List<PersistentAnnotation> getAnnotations(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
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
     * getCampaignContributorCount.
     * </p>
     *
     * @param questionIds a {@link java.util.List} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @Deprecated
    public long getCampaignContributorCount(List<Long> questionIds) throws DAOException;

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
    public List<PersistentAnnotation> getAnnotationsForCampaignAndTarget(Campaign campaign, String pi, Integer page) throws DAOException;

    /**
     * <p>
     * addAnnotation.
     * </p>
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.PersistentAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean addAnnotation(PersistentAnnotation annotation) throws DAOException;

    /**
     * <p>
     * updateAnnotation.
     * </p>
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.PersistentAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean updateAnnotation(PersistentAnnotation annotation) throws DAOException;

    /**
     * <p>
     * deleteAnnotation.
     * </p>
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.PersistentAnnotation} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteAnnotation(PersistentAnnotation annotation) throws DAOException;

    /**
     * Update the given collection from the database
     *
     * @param collection a {@link io.goobi.viewer.model.cms.CMSCollection} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    void refreshCMSCollection(CMSCollection collection) throws DAOException;

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
     * @return true if successfull
     * @throws DAOException
     */
    public boolean addGeoMap(GeoMap map) throws DAOException;

    /**
     * Update the given {@link GeoMap} in the database
     * 
     * @param map
     * @return true if successfull
     * @throws DAOException
     */
    public boolean updateGeoMap(GeoMap map) throws DAOException;

    /**
     * Delete the given {@link GeoMap} from the database
     * 
     * @param map
     * @return true if successfull
     * @throws DAOException
     */
    public boolean deleteGeoMap(GeoMap map) throws DAOException;

    /**
     * Return a list of CMS-pages embedding the given map
     * 
     * @param map
     * @return
     * @throws DAOException
     */
    public List<CMSPage> getPagesUsingMap(GeoMap map) throws DAOException;
}
