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
package de.intranda.digiverso.presentation.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.annotation.Comment;
import de.intranda.digiverso.presentation.model.bookshelf.Bookshelf;
import de.intranda.digiverso.presentation.model.cms.CMSCollection;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.CMSNavigationItem;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement;
import de.intranda.digiverso.presentation.model.cms.CMSStaticPage;
import de.intranda.digiverso.presentation.model.download.DownloadJob;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPageUpdate;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.security.LicenseType;
import de.intranda.digiverso.presentation.model.security.Role;
import de.intranda.digiverso.presentation.model.security.user.IpRange;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.security.user.UserGroup;
import de.intranda.digiverso.presentation.model.security.user.UserRole;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusJob;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusJob.JobStatus;
import de.intranda.digiverso.presentation.model.viewer.PageType;

public interface IDAO {

    // User

    public List<User> getAllUsers(boolean refresh) throws DAOException;

    public long getUserCount(Map<String, String> filters) throws DAOException;

    public List<User> getUsers(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    public User getUser(long id) throws DAOException;

    public User getUserByEmail(String email) throws DAOException;

    public User getUserByOpenId(String identifier) throws DAOException;

    public User getUserByNickname(String nickname) throws DAOException;

    public boolean addUser(User user) throws DAOException;

    public boolean updateUser(User user) throws DAOException;

    public boolean deleteUser(User user) throws DAOException;

    // UserGroup

    public List<UserGroup> getAllUserGroups() throws DAOException;

    public long getUserGroupCount(Map<String, String> filters) throws DAOException;

    public List<UserGroup> getUserGroups(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    public List<UserGroup> getUserGroups(User owner) throws DAOException;

    public UserGroup getUserGroup(long id) throws DAOException;

    public UserGroup getUserGroup(String name) throws DAOException;

    public boolean addUserGroup(UserGroup userGroup) throws DAOException;

    public boolean updateUserGroup(UserGroup userGroup) throws DAOException;

    public boolean deleteUserGroup(UserGroup userGroup) throws DAOException;

    // Bookshelf

    public List<Bookshelf> getAllBookshelves() throws DAOException;

    public List<Bookshelf> getPublicBookshelves() throws DAOException;

    public List<Bookshelf> getBookshelves(User user) throws DAOException;

    public Bookshelf getBookshelf(long id) throws DAOException;

    public Bookshelf getBookshelf(String name) throws DAOException;

    public boolean addBookshelf(Bookshelf bookshelf) throws DAOException;

    public boolean updateBookshelf(Bookshelf bookshelf) throws DAOException;

    public boolean deleteBookshelf(Bookshelf bookshelf) throws DAOException;

    // Role

    public List<Role> getAllRoles() throws DAOException;

    public long getRoleCount(Map<String, String> filters) throws DAOException;

    public List<Role> getRoles(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    public Role getRole(long id) throws DAOException;

    public Role getRole(String name) throws DAOException;

    public boolean addRole(Role role) throws DAOException;

    public boolean updateRole(Role role) throws DAOException;

    public boolean deleteRole(Role role) throws DAOException;

    // UserRole

    public List<UserRole> getAllUserRoles() throws DAOException;

    public List<UserRole> getUserRoles(UserGroup userGroup, User user, Role role) throws DAOException;

    public boolean addUserRole(UserRole userRole) throws DAOException;

    public boolean updateUserRole(UserRole userRole) throws DAOException;

    public boolean deleteUserRole(UserRole userRole) throws DAOException;

    // LicenseType

    public List<LicenseType> getAllLicenseTypes() throws DAOException;

    public long getLicenseTypeCount(Map<String, String> filters) throws DAOException;

    public List<LicenseType> getNonOpenAccessLicenseTypes() throws DAOException;

    public List<LicenseType> getLicenseTypes(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    public LicenseType getLicenseType(long id) throws DAOException;

    public LicenseType getLicenseType(String name) throws DAOException;

    public boolean addLicenseType(LicenseType licenseType) throws DAOException;

    public boolean updateLicenseType(LicenseType licenseType) throws DAOException;

    public boolean deleteLicenseType(LicenseType licenseType) throws DAOException;

    // IpRange

    public List<IpRange> getAllIpRanges() throws DAOException;

    public long getIpRangeCount(Map<String, String> filters) throws DAOException;

    public List<IpRange> getIpRanges(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    public IpRange getIpRange(long id) throws DAOException;

    public IpRange getIpRange(String name) throws DAOException;

    public boolean addIpRange(IpRange ipRange) throws DAOException;

    public boolean updateIpRange(IpRange ipRange) throws DAOException;

    public boolean deleteIpRange(IpRange ipRange) throws DAOException;

    // Comment

    public List<Comment> getAllComments() throws DAOException;

    public long getCommentCount(Map<String, String> filters) throws DAOException;

    public List<Comment> getComments(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    public List<Comment> getCommentsForPage(String pi, int page, boolean topLevelOnly) throws DAOException;

    public Comment getComment(long id) throws DAOException;

    public boolean addComment(Comment comment) throws DAOException;

    public boolean updateComment(Comment comment) throws DAOException;

    public boolean deleteComment(Comment comment) throws DAOException;

    // Search

    public List<Search> getAllSearches() throws DAOException;

    public long getSearchCount(User owner, Map<String, String> filters) throws DAOException;

    public List<Search> getSearches(User owner, int first, int pageSize, String sortField, boolean descending, Map<String, String> filters)
            throws DAOException;

    public List<Search> getSearches(User owner) throws DAOException;

    public Search getSearch(long id) throws DAOException;

    public boolean addSearch(Search search) throws DAOException;

    public boolean updateSearch(Search search) throws DAOException;

    public boolean deleteSearch(Search search) throws DAOException;

    // Overview page

    public long getOverviewPageCount(Date fromDate, Date toDate) throws DAOException;

    public List<OverviewPage> getOverviewPages(int first, int pageSize, Date fromDate, Date toDate) throws DAOException;

    public OverviewPage getOverviewPage(long id) throws DAOException;

    public OverviewPage getOverviewPageForRecord(String pi, Date fromDate, Date toDate) throws DAOException;

    public boolean addOverviewPage(OverviewPage overviewPage) throws DAOException;

    public boolean updateOverviewPage(OverviewPage overviewPage) throws DAOException;

    public boolean deleteOverviewPage(OverviewPage overviewPage) throws DAOException;

    // Overview page updates

    public List<OverviewPageUpdate> getOverviewPageUpdatesForRecord(String pi) throws DAOException;

    public boolean isOverviewPageHasUpdates(String pi, Date fromDate, Date toDate) throws DAOException;

    public OverviewPageUpdate getOverviewPageUpdate(long id) throws DAOException;

    public boolean addOverviewPageUpdate(OverviewPageUpdate update) throws DAOException;

    public boolean deleteOverviewPageUpdate(OverviewPageUpdate update) throws DAOException;

    // Download jobs

    public List<DownloadJob> getAllDownloadJobs() throws DAOException;

    public DownloadJob getDownloadJob(long id) throws DAOException;

    public DownloadJob getDownloadJobByIdentifier(String identifier) throws DAOException;

    public DownloadJob getDownloadJobByMetadata(String type, String pi, String logId) throws DAOException;

    public boolean addDownloadJob(DownloadJob downloadJob) throws DAOException;

    public boolean updateDownloadJob(DownloadJob downloadJob) throws DAOException;

    public boolean deleteDownloadJob(DownloadJob downloadJob) throws DAOException;

    // CMS

    public List<CMSPage> getAllCMSPages() throws DAOException;

    public CMSPage getCmsPageForStaticPage(String pageName) throws DAOException;

    public long getCMSPageCount(Map<String, String> filters) throws DAOException;

    public List<CMSPage> getCMSPages(int first, int pageSize, String sortField, boolean descending, Map<String, String> filters) throws DAOException;

    public List<CMSPage> getCMSPagesByClassification(String pageClassification) throws DAOException;

    public CMSPage getCMSPage(long id) throws DAOException;

    long getCMSPagesCount(Map<String, String> filters) throws DAOException;

    public boolean addCMSPage(CMSPage page) throws DAOException;

    public boolean updateCMSPage(CMSPage page) throws DAOException;

    public boolean deleteCMSPage(CMSPage page) throws DAOException;

    public CMSSidebarElement getCMSSidebarElement(long id) throws DAOException;

    public List<CMSMediaItem> getAllCMSMediaItems() throws DAOException;

    public List<CMSMediaItem> getAllCMSCollectionItems() throws DAOException;

    public CMSMediaItem getCMSMediaItem(long id) throws DAOException;

    public boolean addCMSMediaItem(CMSMediaItem item) throws DAOException;

    public boolean updateCMSMediaItem(CMSMediaItem item) throws DAOException;

    public boolean deleteCMSMediaItem(CMSMediaItem item) throws DAOException;

    public List<CMSPage> getMediaOwners(CMSMediaItem item) throws DAOException;

    public List<CMSNavigationItem> getAllTopCMSNavigationItems() throws DAOException;

    public CMSNavigationItem getCMSNavigationItem(long id) throws DAOException;

    public boolean addCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    public boolean updateCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    public boolean deleteCMSNavigationItem(CMSNavigationItem item) throws DAOException;

    public List<CMSNavigationItem> getRelatedNavItem(CMSPage page) throws DAOException;

    public List<String> getMatchingTags(String inputString) throws DAOException;

    public List<String> getAllTags() throws DAOException;

    public List<CMSStaticPage> getAllStaticPages() throws DAOException;

    public void addStaticPage(CMSStaticPage page) throws DAOException;

    public void updateStaticPage(CMSStaticPage page) throws DAOException;

    public boolean deleteStaticPage(CMSStaticPage page) throws DAOException;

    public List<CMSStaticPage> getStaticPageForCMSPage(CMSPage page) throws DAOException;

    public Optional<CMSStaticPage> getStaticPageForTypeType(PageType pageType) throws DAOException;

    @Deprecated
    public default void detach(Object object) throws DAOException {
        //noop
    }

    // Transkribus

    public List<TranskribusJob> getAllTranskribusJobs() throws DAOException;

    public List<TranskribusJob> getTranskribusJobs(String pi, String transkribusUserId, JobStatus status) throws DAOException;

    public boolean addTranskribusJob(TranskribusJob job) throws DAOException;

    public boolean updateTranskribusJob(TranskribusJob job) throws DAOException;

    public boolean deleteTranskribusJob(TranskribusJob job) throws DAOException;

    // Misc

    public void shutdown();

    /**
     * @param id
     * @return
     * @throws DAOException
     */
    CMSPage getCMSPageForEditing(long id) throws DAOException;

    /**
     * @param pi
     * @return
     * @throws DAOException 
     */
    List<Integer> getPagesWithComments(String pi) throws DAOException;

    /**
     * @param solrField
     * @return 
     * @throws DAOException 
     */
    public List<CMSCollection> getCMSCollections(String solrField) throws DAOException;
    public boolean addCMSCollection(CMSCollection collection) throws DAOException;
    public boolean updateCMSCollection(CMSCollection collection) throws DAOException;
    public boolean deleteCMSCollection(CMSCollection collection) throws DAOException;
    public CMSCollection getCMSCollection(String solrField, String solrFieldValue) throws DAOException;
    /** Update the given collection from the database */
    void refreshCMSCollection(CMSCollection collection) throws DAOException;


}
