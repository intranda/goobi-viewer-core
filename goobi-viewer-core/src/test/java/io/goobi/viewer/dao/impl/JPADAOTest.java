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
package io.goobi.viewer.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.administration.MaintenanceMode;
import io.goobi.viewer.model.administration.legal.TermsOfUse;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.bookmark.Bookmark;
import io.goobi.viewer.model.bookmark.BookmarkList;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.CMSSlider.SourceType;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.media.CMSMediaItemMetadata;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.recordnotes.CMSMultiRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSSingleRecordNote;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.StatisticMode;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignLogMessage;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.crowdsourcing.questions.QuestionType;
import io.goobi.viewer.model.crowdsourcing.questions.TargetSelector;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.download.DownloadJob;
import io.goobi.viewer.model.job.download.EPUBDownloadJob;
import io.goobi.viewer.model.job.download.PDFDownloadJob;
import io.goobi.viewer.model.job.quartz.RecurringTaskTrigger;
import io.goobi.viewer.model.job.upload.UploadJob;
import io.goobi.viewer.model.log.LogMessage;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.security.DownloadTicket;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.security.user.UserRole;
import io.goobi.viewer.model.security.user.icon.UserAvatarOption;

/**
 * JPADAO test suite using H2 DB.
 */
class JPADAOTest extends AbstractDatabaseEnabledTest {

    public static final int NUM_LICENSE_TYPES = 6;

    String pi = "PI_TEST";
    String title = "TITLE_TEST";
    String germanNote = "Bemerkung";
    String englishNote = "Note";
    String changed = "CHANGED";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // File webContent = new File("WebContent/").getAbsoluteFile();
        // String webContentPath = webContent.toURI().toString();
    }

    // Users

    @Test
    void getAllUsersTest() throws DAOException {
        List<User> users = DataManager.getInstance().getDao().getAllUsers(false);
        assertEquals(3, users.size());
    }

    @Test
    void getUserByIdTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        assertEquals(2, user.getOpenIdAccounts().size());
        assertEquals(LocalDateTime.of(2012, 3, 3, 11, 22, 33), user.getLastLogin());
    }

    @Test
    void getUserByEmailTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByEmail("1@UsErS.oRg");
        assertNotNull(user);
    }

    @Test
    void getUserByOpenIdTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByOpenId("user_1_claimed_identifier_2");
        assertNotNull(user);
    }

    @Test
    void getUserByNicknameTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByNickname("admin");
        assertNotNull(user);
    }

    @Test
    void addUserTest() throws DAOException {
        User user = new User();
        user.setEmail("a@b.com");
        user.setPasswordHash("EEEEEE");
        user.setPasswordHash("FFFFFF");
        user.setFirstName("first");
        user.setLastName("last");
        user.setNickName("banned_admin");
        user.setComments("no");
        user.setAvatarType(UserAvatarOption.GRAVATAR);
        LocalDateTime now = LocalDateTime.now();
        user.setLastLogin(now);
        user.setActive(false);
        user.setSuperuser(true);

        DataManager.getInstance().getDao().addUser(user);

        User user2 = DataManager.getInstance().getDao().getUser(user.getId());
        assertNotNull(user2);
        assertEquals(user.getPasswordHash(), user2.getPasswordHash());
        assertEquals(user.getActivationKey(), user2.getActivationKey());
        assertEquals(user.getFirstName(), user2.getFirstName());
        assertEquals(user.getLastName(), user2.getLastName());
        assertEquals(user.getNickName(), user2.getNickName());
        assertEquals(user.getComments(), user2.getComments());
        assertEquals(user.getAvatarType(), user2.getAvatarType());
        assertEquals(user.getLastLogin(), now);
        assertEquals(user.isActive(), user2.isActive());
        assertEquals(user.isSuspended(), user2.isSuspended());
        assertEquals(user.isSuperuser(), user2.isSuperuser());
    }

    @Test
    void updateUserTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllUsers(false).size());
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        user.setEmail("b@b.com");
        user.setPasswordHash("EFEFEF");
        user.setFirstName("first");
        user.setLastName("last");
        user.setNickName("unbanned_admin");
        user.setComments("no");
        user.setAvatarType(UserAvatarOption.GRAVATAR);
        user.setLastLogin(LocalDateTime.now());
        user.setActive(false);
        user.setSuspended(true);
        user.setSuperuser(false);
        user.setAgreedToTermsOfUse(true);
        DataManager.getInstance().getDao().updateUser(user);

        assertEquals(3, DataManager.getInstance().getDao().getAllUsers(false).size());

        User user2 = DataManager.getInstance().getDao().getUser(user.getId());
        assertNotNull(user2);
        assertEquals(user.getId(), user2.getId());
        assertEquals(user.getEmail(), user2.getEmail());
        assertEquals(user.getFirstName(), user2.getFirstName());
        assertEquals(user.getLastName(), user2.getLastName());
        assertEquals(user.getNickName(), user2.getNickName());
        assertEquals(user.getComments(), user2.getComments());
        assertEquals(user.getAvatarType(), user2.getAvatarType());
        // Compare date strings instead of LocalDateTime due to differences in millisecond precision between JVMs
        assertEquals(DateTools.FORMATTERISO8601DATETIME.format(user.getLastLogin()),
                DateTools.FORMATTERISO8601DATETIME.format(user2.getLastLogin()));
        assertEquals(user.isActive(), user2.isActive());
        assertEquals(user.isSuspended(), user2.isSuspended());
        assertEquals(user.isSuperuser(), user2.isSuperuser());
        assertEquals(user.isAgreedToTermsOfUse(), user2.isAgreedToTermsOfUse());
    }

    @Test
    void deleteUserTest() throws DAOException {
        User user = new User();
        user.setEmail("deleteme@b.com");
        user.setPasswordHash("DDDDDD");
        user.setFirstName("first");
        user.setLastName("last");
        user.setNickName("banned_admin");
        user.setComments("no");
        user.setActive(false);
        user.setSuperuser(true);

        DataManager.getInstance().getDao().addUser(user);

        assertNotNull(DataManager.getInstance().getDao().getUser(user.getId()));
        assertTrue(DataManager.getInstance().getDao().deleteUser(user));
        assertNull(DataManager.getInstance().getDao().getUserByEmail("deleteme@b.com"));
    }

    @Test
    void userLicenseTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(2);
        assertNotNull(user);
        assertEquals(1, user.getLicenses().size());
        assertEquals(1, user.getLicenses().get(0).getPrivileges().size());
        for (String priv : user.getLicenses().get(0).getPrivileges()) {
            assertEquals(IPrivilegeHolder.PRIV_LIST, priv);
        }

        // Saving the licensee should not create any extra licenses
        {
            assertTrue(DataManager.getInstance().getDao().updateUser(user));
            User user2 = DataManager.getInstance().getDao().getUser(user.getId());
            assertNotNull(user2);
            assertEquals(1, user2.getLicenses().size());
        }

        // Adding a new license should update the attached object with ID
        {
            License license = new License();
            license.setLicenseType(user.getLicenses().get(0).getLicenseType());
            user.addLicense(license);
            assertTrue(DataManager.getInstance().getDao().updateUser(user));
            User user2 = DataManager.getInstance().getDao().getUser(user.getId());
            assertNotNull(user2);
            assertEquals(2, user2.getLicenses().size());
            assertNotNull(user2.getLicenses().get(0).getId());
            assertNotNull(user2.getLicenses().get(1).getId());
        }

        // Orphaned licenses should be deleted properly
        user.removeLicense(user.getLicenses().get(0));
        assertTrue(DataManager.getInstance().getDao().updateUser(user));
        User user3 = DataManager.getInstance().getDao().getUser(user.getId());
        assertNotNull(user3);
        assertEquals(1, user3.getLicenses().size());

    }

    // User groups

    @Test
    void getAllUserGroupsTest() throws DAOException {
        List<UserGroup> userGroups = DataManager.getInstance().getDao().getAllUserGroups();
        assertEquals(3, userGroups.size());
    }

    @Test
    void getAllUserGroupsForOwnerTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        assertEquals(3, DataManager.getInstance().getDao().getUserGroups(user).size());
    }

    @Test
    void getUserGroupByIdTest() throws DAOException {
        UserGroup ug = DataManager.getInstance().getDao().getUserGroup(1);
        assertNotNull(ug);
        assertNotNull(ug.getOwner());
        assertEquals(Long.valueOf(1), ug.getOwner().getId());
        assertEquals("user group 1 name", ug.getName());
        assertEquals("user group 1 desc", ug.getDescription());
        assertTrue(ug.isActive());
    }

    @Test
    void getUserGroupByNameTest() throws DAOException {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup("user group 1 name");
        assertNotNull(userGroup);
        assertNotNull(userGroup.getOwner());
        assertEquals(Long.valueOf(1), userGroup.getOwner().getId());
        assertEquals("user group 1 name", userGroup.getName());
        assertEquals("user group 1 desc", userGroup.getDescription());
        assertTrue(userGroup.isActive());
    }

    @Test
    void addUserGroupTest() throws DAOException {
        User owner = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(owner);

        UserGroup userGroup = new UserGroup();
        userGroup.setOwner(owner);
        userGroup.setName("added user group name");
        userGroup.setDescription("added user group desc");
        userGroup.setActive(false);

        assertTrue(DataManager.getInstance().getDao().addUserGroup(userGroup));
        assertNotNull(userGroup.getId());

        UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
        assertNotNull(userGroup2);
        assertNotNull(userGroup2.getOwner());
        assertEquals(userGroup.getOwner().getId(), userGroup2.getOwner().getId());
        assertEquals(userGroup.getName(), userGroup2.getName());
        assertEquals(userGroup.getDescription(), userGroup2.getDescription());
        assertEquals(userGroup.isActive(), userGroup2.isActive());
    }

    @Test
    void updateUserGroupTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        assertNotNull(userGroup);
        assertEquals("user group 1 name", userGroup.getName());

        userGroup.setName("user group 1 new name");
        assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
        assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());

        UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
        assertNotNull(userGroup2);
        assertEquals(userGroup.getId(), userGroup2.getId());
        assertEquals("user group 1 new name", userGroup2.getName());

    }

    @Test
    void deleteUserGroupWithMembersTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        assertNotNull(userGroup);
        assertFalse(DataManager.getInstance().getDao().deleteUserGroup(userGroup));
        assertNotNull(DataManager.getInstance().getDao().getUserGroup(2));
        assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());
    }

    @Test
    void deleteUserGroupWithoutMembersTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(2);
        assertNotNull(userGroup);
        assertTrue(DataManager.getInstance().getDao().deleteUserGroup(userGroup));
        assertNull(DataManager.getInstance().getDao().getUserGroup(2));
        assertEquals(2, DataManager.getInstance().getDao().getAllUserGroups().size());
    }

    // UserRoles (group memberships)

    @Test
    void getAllUserRolesTest() throws DAOException {
        List<UserRole> userRoles = DataManager.getInstance().getDao().getAllUserRoles();
        assertEquals(3, userRoles.size());
    }

    /**
     * @see JPADAO#getUserRoleCount(UserGroup,User,Role)
     * @verifies return correct count
     */
    @Test
    void getUserRoleCount_shouldReturnCorrectCount() throws Exception {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        assertNotNull(userGroup);
        assertEquals(1, DataManager.getInstance().getDao().getUserRoleCount(userGroup, null, null));

        User user = DataManager.getInstance().getDao().getUser(2);
        assertNotNull(user);
        assertEquals(1, DataManager.getInstance().getDao().getUserRoleCount(userGroup, user, null));

        Role role = DataManager.getInstance().getDao().getRole(1);
        assertNotNull(role);
        assertEquals(1, DataManager.getInstance().getDao().getUserRoleCount(userGroup, user, role));
    }

    @Test
    void getUserGroupMembershipsByUserGroupTest() throws DAOException {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        assertNotNull(userGroup);
        List<UserRole> memberships = DataManager.getInstance().getDao().getUserRoles(userGroup, null, null);
        assertNotNull(memberships);
        assertEquals(1, memberships.size());
    }

    @Test
    void getUserGroupMembershipsByUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(2);
        assertNotNull(user);
        List<UserRole> memberships = DataManager.getInstance().getDao().getUserRoles(null, user, null);
        assertNotNull(memberships);
        assertEquals(1, memberships.size());
    }

    @Test
    void getUserGroupMembershipsByRoleTest() throws DAOException {
        Role role = DataManager.getInstance().getDao().getRole(1);
        assertNotNull(role);
        List<UserRole> memberships = DataManager.getInstance().getDao().getUserRoles(null, null, role);
        assertNotNull(memberships);
        assertEquals(3, memberships.size());
    }

    @Test
    void userGroupLicenseTest() throws DAOException {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        assertNotNull(userGroup);
        assertEquals(2, userGroup.getLicenses().size());
        assertEquals(1, userGroup.getLicenses().get(0).getPrivileges().size());
        for (String priv : userGroup.getLicenses().get(0).getPrivileges()) {
            assertEquals("license 2 priv 1", priv);
        }

        // Saving the licensee should not create any extra licenses
        {
            assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
            UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
            assertNotNull(userGroup2);
            assertEquals(2, userGroup2.getLicenses().size());
        }

        // Adding a new license should update the attached object with ID
        {
            License license = new License();
            license.setLicenseType(userGroup.getLicenses().get(0).getLicenseType());
            userGroup.addLicense(license);
            assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
            UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
            assertNotNull(userGroup2);
            assertEquals(3, userGroup2.getLicenses().size());
            assertNotNull(userGroup2.getLicenses().get(0).getId());
            assertNotNull(userGroup2.getLicenses().get(1).getId());
        }

        // Orphaned licenses should be deleted properly
        {
            userGroup.removeLicense(userGroup.getLicenses().get(0));
            assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
            UserGroup userGroup3 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
            assertNotNull(userGroup3);
            assertEquals(2, userGroup3.getLicenses().size());
        }
    }

    @Test
    void addUserRoleTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(2);
        assertNotNull(user);
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(2);
        assertNotNull(userGroup);
        Role role = DataManager.getInstance().getDao().getRole(2);
        assertNotNull(role);

        assertEquals(3, DataManager.getInstance().getDao().getAllUserRoles().size());
        assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, user, role).size());
        assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, user, null).size());
        assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, null, null).size());

        UserRole userRole = new UserRole(userGroup, user, role);
        assertTrue(DataManager.getInstance().getDao().addUserRole(userRole));
        assertNotNull(userRole.getId());

        assertEquals(4, DataManager.getInstance().getDao().getAllUserRoles().size());
        assertEquals(1, DataManager.getInstance().getDao().getUserRoles(userGroup, user, role).size());
        assertEquals(1, DataManager.getInstance().getDao().getUserRoles(userGroup, user, null).size());
        assertEquals(1, DataManager.getInstance().getDao().getUserRoles(userGroup, null, null).size());

        UserRole userRole2 = DataManager.getInstance().getDao().getUserRoles(userGroup, user, role).get(0);
        assertNotNull(userRole2);
        assertEquals(userRole.getUserGroup(), userRole2.getUserGroup());
        assertEquals(userRole.getUser(), userRole2.getUser());
        assertEquals(userRole.getRole(), userRole2.getRole());
    }

    @Test
    void updateUserRoleTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllUserRoles().size());
        UserRole userRole = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        assertNotNull(userRole);

        Role role1 = DataManager.getInstance().getDao().getRole(1);
        assertNotNull(role1);
        Role role2 = DataManager.getInstance().getDao().getRole(2);
        assertNotNull(role2);
        assertEquals(role1, userRole.getRole());
        userRole.setRole(role2);
        assertTrue(DataManager.getInstance().getDao().updateUserRole(userRole));
        assertEquals(3, DataManager.getInstance().getDao().getAllUserRoles().size());

        UserRole userRole2 = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        assertNotNull(userRole2);
        assertEquals(userRole.getRole(), userRole2.getRole());
    }

    @Test
    void deleteUserRoleTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllUserRoles().size());
        UserRole userRole = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        assertTrue(DataManager.getInstance().getDao().deleteUserRole(userRole));
        assertEquals(2, DataManager.getInstance().getDao().getAllUserRoles().size());
    }

    // IP ranges

    @Test
    void getAllIpRangesTest() throws DAOException {
        List<IpRange> ipRanges = DataManager.getInstance().getDao().getAllIpRanges();
        assertEquals(2, ipRanges.size());
    }

    @Test
    void getIpRangeByIdTest() throws DAOException {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        assertNotNull(ipRange);
        assertEquals(Long.valueOf(1), ipRange.getId());
        assertEquals("localhost", ipRange.getName());
        assertEquals("1.2.3.4/24", ipRange.getSubnetMask());
        assertEquals("ip range 1 desc", ipRange.getDescription());
    }

    @Test
    void getIpRangeByNameTest() throws DAOException {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange("localhost");
        assertNotNull(ipRange);
        assertEquals(Long.valueOf(1), ipRange.getId());
        assertEquals("localhost", ipRange.getName());
        assertEquals("1.2.3.4/24", ipRange.getSubnetMask());
        assertEquals("ip range 1 desc", ipRange.getDescription());
    }

    @Test
    void addIpRangeTest() throws DAOException {
        assertEquals(2, DataManager.getInstance().getDao().getAllIpRanges().size());
        IpRange ipRange = new IpRange();
        ipRange.setName("ip range to add name");
        ipRange.setDescription("ip range to add desc");
        ipRange.setSubnetMask("0.0.0.0./0");
        assertTrue(DataManager.getInstance().getDao().addIpRange(ipRange));
        assertNotNull(ipRange.getId());
        assertEquals(3, DataManager.getInstance().getDao().getAllIpRanges().size());

        IpRange ipRange2 = DataManager.getInstance().getDao().getIpRange(ipRange.getId());
        assertNotNull(ipRange2);
        assertEquals(ipRange.getName(), ipRange2.getName());
        assertEquals(ipRange.getDescription(), ipRange2.getDescription());
        assertEquals(ipRange.getSubnetMask(), ipRange2.getSubnetMask());
    }

    @Test
    void updateIpRangeTest() throws DAOException {
        assertEquals(2, DataManager.getInstance().getDao().getAllIpRanges().size());
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        assertNotNull(ipRange);

        ipRange.setName("ip range 1 new name");
        ipRange.setDescription("ip range 1 new desc");
        ipRange.setSubnetMask("0.0.0.0./0");

        assertTrue(DataManager.getInstance().getDao().updateIpRange(ipRange));
        assertEquals(2, DataManager.getInstance().getDao().getAllIpRanges().size());

        IpRange ipRange2 = DataManager.getInstance().getDao().getIpRange(ipRange.getId());
        assertNotNull(ipRange2);
        assertEquals(ipRange.getId(), ipRange2.getId());
        assertEquals(ipRange.getName(), ipRange.getName());
        assertEquals(ipRange.getDescription(), ipRange2.getDescription());
        assertEquals(ipRange.getSubnetMask(), ipRange2.getSubnetMask());
    }

    @Test
    void deleteIpRangeTest() throws DAOException {
        assertEquals(2, DataManager.getInstance().getDao().getAllIpRanges().size());
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        assertNotNull(ipRange);
        assertTrue(DataManager.getInstance().getDao().deleteIpRange(ipRange));
        assertNull(DataManager.getInstance().getDao().getIpRange(1));
        assertEquals(1, DataManager.getInstance().getDao().getAllIpRanges().size());
    }

    @Test
    void ipRangeLicenseTest() throws DAOException {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        assertNotNull(ipRange);
        assertEquals(1, ipRange.getLicenses().size());
        assertEquals(1, ipRange.getLicenses().get(0).getPrivileges().size());
        for (String priv : ipRange.getLicenses().get(0).getPrivileges()) {
            assertEquals(IPrivilegeHolder.PRIV_LIST, priv);
        }

        // Saving the licensee should not create any extra licenses
        assertTrue(DataManager.getInstance().getDao().updateIpRange(ipRange));
        IpRange ipRange2 = DataManager.getInstance().getDao().getIpRange(ipRange.getId());
        assertNotNull(ipRange2);
        assertEquals(1, ipRange2.getLicenses().size());

        // Orphaned licenses should be deleted properly
        ipRange.removeLicense(ipRange.getLicenses().get(0));
        assertTrue(DataManager.getInstance().getDao().updateIpRange(ipRange));
        IpRange ipRange3 = DataManager.getInstance().getDao().getIpRange(ipRange.getId());
        assertNotNull(ipRange3);
        assertEquals(0, ipRange3.getLicenses().size());
    }

    // CommentGroups

    /**
     * @see JPADAO#getAllCommentGroups()
     * @verifies return all rows
     */
    @Test
    void getAllCommentGroups_shouldReturnAllRows() throws Exception {
        assertEquals(2, DataManager.getInstance().getDao().getAllCommentGroups().size());
    }

    /**
     * @see JPADAO#getCommentGroupUnfiltered()
     * @verifies return correct row
     */
    @Test
    void getCommentGroupUnfiltered_shouldReturnCorrectRow() throws Exception {
        CommentGroup commentGroup = DataManager.getInstance().getDao().getCommentGroupUnfiltered();
        assertNotNull(commentGroup);
        assertEquals(Long.valueOf(1), commentGroup.getId());
    }

    // Comments

    @Test
    void getAllCommentsTest() throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getAllComments();
        assertEquals(4, comments.size());
    }

    /**
     * @see JPADAO#getCommentCount(Map,User,Set)
     * @verifies return correct count
     */
    @Test
    void getCommentCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(4L, DataManager.getInstance().getDao().getCommentCount(null, null, null));
    }

    /**
     * @see JPADAO#getCommentCount(Map,User,Set)
     * @verifies filter correctly
     */
    @Test
    void getCommentCount_shouldFilterCorrectly() throws Exception {
        Map<String, String> filters = new HashMap<>();
        filters.put("targetPageOrder", "1");
        assertEquals(3L, DataManager.getInstance().getDao().getCommentCount(filters, null, null));
    }

    /**
     * @see JPADAO#getCommentCount(Map,User,Set)
     * @verifies filter for users correctly
     */
    @Test
    void getCommentCount_shouldFilterForUsersCorrectly() throws Exception {
        {
            User owner = DataManager.getInstance().getDao().getUser(1l);
            assertEquals(3L, DataManager.getInstance().getDao().getCommentCount(null, owner, null));
        }
        {
            User owner = DataManager.getInstance().getDao().getUser(2l);
            assertEquals(1L, DataManager.getInstance().getDao().getCommentCount(null, owner, null));
        }
    }

    /**
     * @see JPADAO#getCommentCount(Map,User,Set)
     * @verifies apply target pi filter correctly
     */
    @Test
    void getCommentCount_shouldApplyTargetPiFilterCorrectly() throws Exception {
        // TODO update test dataset to include comments with different target_pi
        assertEquals(4L, DataManager.getInstance().getDao().getCommentCount(null, null, Collections.singleton("PI_1")));
    }

    @Test
    void getCommentByIdTest() throws DAOException {
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        assertNotNull(comment);
        assertEquals(Long.valueOf(1), comment.getId());
        assertEquals("PI_1", comment.getTargetPI());
        assertEquals(Integer.valueOf(1), comment.getTargetPageOrder());
        assertNotNull(comment.getCreator());
        assertEquals(Long.valueOf(1), comment.getCreator().getId());
        assertEquals("comment 1 text", comment.getText());
        assertNotNull(comment.getDateCreated());
        assertNull(comment.getDateModified());
        //        assertNull(comment.getParent());
        //        assertEquals(1, comment.getChildren().size());

        //        Comment comment2 = DataManager.getInstance().getDao().getComment(2);
        //        assertEquals(comment, comment2.getParent());
        //        assertEquals(1, comment2.getChildren().size());
    }

    @Test
    void getCommentsForPageTest() throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage("PI_1", 1);
        assertEquals(3, comments.size());
    }

    @Test
    void addCommentTest() throws DAOException {
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = new Comment();
        comment.setTargetPI("PI_2");
        comment.setTargetPageOrder(1);
        comment.setText("new comment text");
        comment.setCreator(DataManager.getInstance().getDao().getUser(1));
        assertTrue(DataManager.getInstance().getDao().addComment(comment));
        assertNotNull(comment.getId());
        assertEquals(5, DataManager.getInstance().getDao().getAllComments().size());

        Comment comment2 = DataManager.getInstance().getDao().getComment(comment.getId());
        assertNotNull(comment2);
        assertEquals(comment.getTargetPI(), comment2.getTargetPI());
        assertEquals(comment.getTargetPageOrder(), comment2.getTargetPageOrder());
        assertEquals(comment.getText(), comment2.getText());
        assertEquals(comment.getCreator(), comment2.getCreator());
        assertNotNull(comment2.getDateCreated());
        assertNull(comment2.getDateModified());
    }

    @Test
    void updateCommentTest() throws DAOException {
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        assertNotNull(comment);

        comment.setText("new comment 1 text");
        LocalDateTime now = LocalDateTime.now();
        comment.setDateModified(now);

        assertTrue(DataManager.getInstance().getDao().updateComment(comment));
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());

        Comment comment2 = DataManager.getInstance().getDao().getComment(comment.getId());
        assertNotNull(comment2);
        assertEquals(comment.getTargetPI(), comment2.getTargetPI());
        assertEquals(comment.getTargetPageOrder(), comment2.getTargetPageOrder());
        assertEquals(comment.getText(), comment2.getText());
        // Compare date strings instead of LocalDateTime due to differences in milisecond precision between JVMs
        assertEquals(DateTools.FORMATTERISO8601DATETIMEMS.format(now),
                DateTools.FORMATTERISO8601DATETIMEMS.format(comment2.getDateModified()));
        assertEquals(comment.getCreator(), comment2.getCreator());
    }

    @Test
    void deleteCommentTest() throws DAOException {
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        assertNotNull(comment);
        assertTrue(DataManager.getInstance().getDao().deleteComment(comment));
        assertNull(DataManager.getInstance().getDao().getComment(1));
        assertEquals(3, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#deleteComments(String,User)
     * @verifies delete comments for pi correctly
     */
    @Test
    void deleteComments_shouldDeleteCommentsForPiCorrectly() throws Exception {
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        assertEquals(4, DataManager.getInstance().getDao().deleteComments("PI_1", null));
        assertEquals(0, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#deleteComments(String,User)
     * @verifies delete comments for user correctly
     */
    @Test
    void deleteComments_shouldDeleteCommentsForUserCorrectly() throws Exception {
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        assertEquals(3, DataManager.getInstance().getDao().deleteComments(null, user));
        assertEquals(1, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#deleteComments(String,User)
     * @verifies delete comments for pi and user correctly
     */
    @Test
    void deleteComments_shouldDeleteCommentsForPiAndUserCorrectly() throws Exception {
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        assertEquals(3, DataManager.getInstance().getDao().deleteComments("PI_1", user));
        assertEquals(1, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#deleteComments(String,User)
     * @verifies not delete anything if both pi and creator are null
     */
    @Test
    void deleteComments_shouldNotDeleteAnythingIfBothPiAndCreatorAreNull() throws Exception {
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        assertEquals(0, DataManager.getInstance().getDao().deleteComments(null, null));
        assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#changeCommentsOwner(User,User)
     * @verifies update rows correctly
     */
    @Test
    void changeCommentsOwner_shouldUpdateRowsCorrectly() throws Exception {
        User oldOwner = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(oldOwner);
        User newOwner = DataManager.getInstance().getDao().getUser(3);
        assertNotNull(newOwner);

        assertEquals(3, DataManager.getInstance().getDao().changeCommentsOwner(oldOwner, newOwner));

        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork("PI_1");
        assertEquals(4, comments.size());
        assertEquals(newOwner, comments.get(0).getCreator());
        assertNotEquals(newOwner, comments.get(1).getCreator());
        assertEquals(newOwner, comments.get(2).getCreator());
        assertEquals(newOwner, comments.get(3).getCreator());
    }

    // Search

    @Test
    void getAllSearchesTest() throws DAOException {
        List<Search> list = DataManager.getInstance().getDao().getAllSearches();
        assertEquals(3, list.size());
    }

    @Test
    void getSearchByIdTest() throws DAOException {
        Search o = DataManager.getInstance().getDao().getSearch(1);
        assertNotNull(o);
        assertEquals(Long.valueOf(1), o.getId());
        assertNotNull(o.getOwner());
        assertEquals(Long.valueOf(1), o.getOwner().getId());
        assertEquals("query 1", o.getQuery());
        assertEquals(1, o.getPage());
        assertEquals("sort 1", o.getSortString());
        assertEquals("filter 1", o.getFacetString());
        assertTrue(o.isNewHitsNotification());
        assertNotNull(o.getDateUpdated());
    }

    @Test
    void getSearchesForUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        List<Search> list = DataManager.getInstance().getDao().getSearches(user);
        assertEquals(2, list.size());
    }

    @Test
    void addSearchTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllSearches().size());
        Search o = new Search(SearchHelper.SEARCH_TYPE_REGULAR, SearchHelper.SEARCH_FILTER_ALL,
                Collections.singletonList(SearchResultGroup.createDefaultGroup()));
        o.setOwner(DataManager.getInstance().getDao().getUser(1));
        o.setName("new search");
        o.setQuery("PI:*");
        o.setPage(1);
        o.setSortString("SORT_FIELD");
        o.setFacetString("DOCSTRCT:Other;;DC:newcol");
        o.setNewHitsNotification(true);
        LocalDateTime now = LocalDateTime.now();
        o.setDateUpdated(now);
        assertTrue(DataManager.getInstance().getDao().addSearch(o));
        assertNotNull(o.getId());
        assertEquals(4, DataManager.getInstance().getDao().getAllSearches().size());

        Search o2 = DataManager.getInstance().getDao().getSearch(o.getId());
        assertNotNull(o2);
        assertEquals(o.getOwner(), o2.getOwner());
        assertEquals(o.getName(), o2.getName());
        assertEquals(o.getQuery(), o2.getQuery());
        assertEquals(o.getPage(), o2.getPage());
        assertEquals(o.getSortString(), o2.getSortString());
        assertEquals(o.getFacetString(), o2.getFacetString());
        assertEquals(o.isNewHitsNotification(), o2.isNewHitsNotification());
        // Compare date strings instead of LocalDateTime due to differences in milisecond precision between JVMs
        assertEquals(DateTools.FORMATTERISO8601DATETIMEMS.format(now), DateTools.FORMATTERISO8601DATETIMEMS.format(o2.getDateUpdated()));
    }

    @Test
    void updateSearchTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllSearches().size());
        Search o = DataManager.getInstance().getDao().getSearch(1);
        assertNotNull(o);

        o.setName("new name");
        LocalDateTime now = LocalDateTime.now();
        o.setDateUpdated(now);

        assertTrue(DataManager.getInstance().getDao().updateSearch(o));
        assertEquals(3, DataManager.getInstance().getDao().getAllSearches().size());

        Search o2 = DataManager.getInstance().getDao().getSearch(o.getId());
        assertNotNull(o2);
        assertEquals(o.getName(), o2.getName());
        assertEquals(o.getOwner(), o2.getOwner());
        // Compare date strings instead of LocalDateTime due to differences in milisecond precision between JVMs
        assertEquals(DateTools.FORMATTERISO8601DATETIMEMS.format(now), DateTools.FORMATTERISO8601DATETIMEMS.format(o2.getDateUpdated()));
    }

    @Test
    void deleteSearchTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllSearches().size());
        Search o = DataManager.getInstance().getDao().getSearch(1);
        assertNotNull(o);
        assertTrue(DataManager.getInstance().getDao().deleteSearch(o));
        assertNull(DataManager.getInstance().getDao().getSearch(1));
        assertEquals(2, DataManager.getInstance().getDao().getAllSearches().size());
    }

    // License types

    @Test
    void getAllLicenseTypesTest() throws DAOException {
        assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
    }

    /**
     * @see JPADAO#getRecordLicenseTypes()
     * @verifies only return non open access license types
     */
    @Test
    void getRecordTypes_shouldOnlyReturnNonOpenAccessLicenseTypes() throws Exception {
        List<LicenseType> licenseTypes = DataManager.getInstance().getDao().getRecordLicenseTypes();
        assertEquals(5, licenseTypes.size());
        assertEquals(Long.valueOf(1), licenseTypes.get(0).getId());
        assertEquals(Long.valueOf(6), licenseTypes.get(4).getId());
    }

    @Test
    void getLicenseTypeByIdTest() throws DAOException {
        {
            LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
            assertNotNull(licenseType);
            assertEquals(Long.valueOf(1), licenseType.getId());
            assertEquals("license type 1 name", licenseType.getName());
            assertEquals("license type 1 desc", licenseType.getDescription());
            assertEquals(false, licenseType.isOpenAccess());
            assertEquals(1, licenseType.getPrivileges().size());
            assertEquals(1, licenseType.getOverriddenLicenseTypes().size());
        }
        {
            LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(4);
            assertNotNull(licenseType);
            assertEquals(Long.valueOf(4), licenseType.getId());
            assertEquals(1, licenseType.getOverriddenLicenseTypes().size());
        }
    }

    @Test
    void getLicenseTypeByNameTest() throws DAOException {
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType("license type 2 name");
        assertNotNull(licenseType);
        assertEquals(Long.valueOf(2), licenseType.getId());
        assertEquals("license type 2 name", licenseType.getName());
        assertEquals("license type 2 (unused)", licenseType.getDescription());
        assertEquals(true, licenseType.isOpenAccess());
        assertEquals(1, licenseType.getPrivileges().size());
    }

    /**
     * @see JPADAO#getLicenseTypes(List)
     * @verifies return all matching rows
     */
    @Test
    void getLicenseTypes_shouldReturnAllMatchingRows() throws Exception {
        String[] names = new String[] { "license type 1 name", "license type 2 name" };
        List<LicenseType> result = DataManager.getInstance().getDao().getLicenseTypes(Arrays.asList(names));
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("license type 1 name", result.get(0).getName());
        assertEquals("license type 2 name", result.get(1).getName());
    }

    /**
     * @see JPADAO#getOverridingLicenseType(LicenseType)
     * @verifies return all matching rows
     */
    @Test
    void getOverridingLicenseType_shouldReturnAllMatchingRows() throws Exception {
        LicenseType lt = DataManager.getInstance().getDao().getLicenseType(1);
        assertNotNull(lt);
        List<LicenseType> overriding = DataManager.getInstance().getDao().getOverridingLicenseType(lt);
        assertEquals(1, overriding.size());
        assertEquals(Long.valueOf(4), overriding.get(0).getId());
    }

    @Test
    void addLicenseTypeTest() throws DAOException {
        assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = new LicenseType();
        licenseType.setName("license type to add name");
        licenseType.setDescription("license type to add desc");
        licenseType.getPrivileges().add("license type to add priv 1");
        assertTrue(DataManager.getInstance().getDao().addLicenseType(licenseType));
        assertNotNull(licenseType.getId());
        assertEquals(NUM_LICENSE_TYPES + 1, DataManager.getInstance().getDao().getAllLicenseTypes().size());

        LicenseType licenseType2 = DataManager.getInstance().getDao().getLicenseType(licenseType.getId());
        assertNotNull(licenseType2);
        assertEquals(licenseType.getName(), licenseType2.getName());
        assertEquals(licenseType.getDescription(), licenseType2.getDescription());
        assertEquals(1, licenseType2.getPrivileges().size());
        assertTrue(licenseType2.getPrivileges().contains("license type to add priv 1"));
    }

    @Test
    void updateLicenseTypeTest() throws DAOException {
        assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        assertNotNull(licenseType);
        assertEquals(1, licenseType.getPrivileges().size());

        licenseType.setName("license type 1 new name");
        licenseType.setDescription("license type 1 new desc");
        licenseType.getPrivileges().add("license type 1 priv 2");
        assertTrue(DataManager.getInstance().getDao().updateLicenseType(licenseType));
        assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());

        LicenseType licenseType2 = DataManager.getInstance().getDao().getLicenseType(licenseType.getId());
        assertNotNull(licenseType2);
        assertEquals(licenseType.getId(), licenseType2.getId());
        assertEquals(licenseType.getName(), licenseType2.getName());
        assertEquals(licenseType.getDescription(), licenseType2.getDescription());
        assertEquals(2, licenseType.getPrivileges().size());
    }

    @Test
    void deleteUsedLicenseTypeTest() throws DAOException {
        // Deleting license types in use should fail
        assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        assertNotNull(licenseType);
        assertFalse(DataManager.getInstance().getDao().deleteLicenseType(licenseType));
        assertNotNull(DataManager.getInstance().getDao().getLicenseType(1));
        assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
    }

    @Test
    void deleteUnusedLicenseTypeTest() throws DAOException {
        assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(2);
        assertNotNull(licenseType);
        assertTrue(DataManager.getInstance().getDao().deleteLicenseType(licenseType));
        assertNull(DataManager.getInstance().getDao().getLicenseType(2));
        assertEquals(NUM_LICENSE_TYPES - 1, DataManager.getInstance().getDao().getAllLicenseTypes().size());
    }

    // Roles

    @Test
    void getAllRolesTest() throws DAOException {
        List<Role> roles = DataManager.getInstance().getDao().getAllRoles();
        assertEquals(2, roles.size());
    }

    @Test
    void getRoleByIdTest() throws DAOException {
        Role role = DataManager.getInstance().getDao().getRole(1);
        assertNotNull(role);
        assertEquals(Long.valueOf(1), role.getId());
        assertEquals("role 1 name", role.getName());
        assertEquals("role 1 desc", role.getDescription());
        assertEquals(1, role.getPrivileges().size());
    }

    @Test
    void getRoleByNameTest() throws DAOException {
        Role role = DataManager.getInstance().getDao().getRole("role 1 name");
        assertNotNull(role);
        assertEquals(Long.valueOf(1), role.getId());
        assertEquals("role 1 name", role.getName());
        assertEquals("role 1 desc", role.getDescription());
        assertEquals(1, role.getPrivileges().size());
    }

    @Test
    void addRoleTest() throws DAOException {
        Role role = new Role();
        role.setName("role to add name");
        role.setDescription("role to add desc");
        role.getPrivileges().add("role to add priv 1");
        assertTrue(DataManager.getInstance().getDao().addRole(role));
        assertNotNull(role.getId());

        Role role2 = DataManager.getInstance().getDao().getRole(role.getId());
        assertNotNull(role2);
        assertEquals(role.getName(), role2.getName());
        assertEquals(role.getDescription(), role2.getDescription());
        assertEquals(1, role2.getPrivileges().size());
        assertTrue(role2.getPrivileges().contains("role to add priv 1"));
    }

    @Test
    void updateRoleTest() throws DAOException {
        assertEquals(2, DataManager.getInstance().getDao().getAllRoles().size());
        Role role = DataManager.getInstance().getDao().getRole(1);
        assertNotNull(role);
        assertEquals("role 1 name", role.getName());
        assertEquals("role 1 desc", role.getDescription());
        assertEquals(1, role.getPrivileges().size());

        role.setName("role 1 new name");
        role.setDescription("role 1 new desc");
        role.getPrivileges().add("role 1 priv 2");
        assertTrue(DataManager.getInstance().getDao().updateRole(role));
        assertEquals(2, DataManager.getInstance().getDao().getAllRoles().size());

        Role role2 = DataManager.getInstance().getDao().getRole(role.getId());
        assertNotNull(role2);
        assertEquals(role.getId(), role2.getId());
        assertEquals(role.getName(), role2.getName());
        assertEquals(role.getDescription(), role2.getDescription());
        assertEquals(2, role.getPrivileges().size());
    }

    @Test
    void duplicateRolePrivilegeTest() throws DAOException {
        Role role = DataManager.getInstance().getDao().getRole(1);
        assertNotNull(role);
        assertEquals(1, role.getPrivileges().size());

        role.getPrivileges().add("role 1 priv 1");
        assertTrue(DataManager.getInstance().getDao().updateRole(role));

        Role role2 = DataManager.getInstance().getDao().getRole(role.getId());
        assertNotNull(role2);
        assertEquals(1, role2.getPrivileges().size());
    }

    @Test
    void deleteRoleTest() throws DAOException {
        assertEquals(2, DataManager.getInstance().getDao().getAllRoles().size());
        Role role = DataManager.getInstance().getDao().getRole(2);
        assertNotNull(role);
        assertTrue(DataManager.getInstance().getDao().deleteRole(role));
        assertNull(DataManager.getInstance().getDao().getRole(2));
        assertEquals(1, DataManager.getInstance().getDao().getAllRoles().size());
    }

    // Bookmarks

    @Test
    void getAllBookmarkListsTest() throws DAOException {
        List<BookmarkList> result = DataManager.getInstance().getDao().getAllBookmarkLists();
        assertEquals(3, result.size());
    }

    @Test
    void getPublicBookmarkListsTest() throws DAOException {
        List<BookmarkList> result = DataManager.getInstance().getDao().getPublicBookmarkLists();
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(2), result.get(0).getId());
    }

    @Test
    void getAllBookmarkListsForUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        List<BookmarkList> boomarkLists = DataManager.getInstance().getDao().getBookmarkLists(user);
        assertEquals(2, boomarkLists.size());
        assertEquals(Long.valueOf(3), boomarkLists.get(0).getId());
        assertEquals(user, boomarkLists.get(0).getOwner());
    }

    @Test
    void countAllBookmarkListsForUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        List<BookmarkList> boomarkLists = DataManager.getInstance().getDao().getBookmarkLists(user);
        long count = DataManager.getInstance().getDao().getBookmarkListCount(user);
        assertEquals(boomarkLists.size(), count);
    }

    @Test
    void getBookmarkListByIdTest() throws DAOException {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(1);
        assertNotNull(bl);
        assertEquals(Long.valueOf(1), bl.getId());
        assertNotNull(bl.getOwner());
        assertEquals(Long.valueOf(1), bl.getOwner().getId());
        assertEquals("bookmark list 1 name", bl.getName());
        assertEquals("bookmark list 1 desc", bl.getDescription());
        assertEquals(2, bl.getItems().size());

    }

    /**
     * @see JPADAO#getBookmarkList(String,User)
     * @verifies return correct row for name
     */
    @Test
    void getBookmarkList_shouldReturnCorrectRowForName() throws Exception {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList("bookmark list 1 name", null);
        assertNotNull(bl);
        assertEquals(Long.valueOf(1), bl.getId());
        assertNotNull(bl.getOwner());
        assertEquals(Long.valueOf(1), bl.getOwner().getId());
        assertEquals("bookmark list 1 name", bl.getName());
        assertEquals("bookmark list 1 desc", bl.getDescription());
        assertEquals(2, bl.getItems().size());
    }

    /**
     * @see JPADAO#getBookmarkList(String,User)
     * @verifies return correct row for name and user
     */
    @Test
    void getBookmarkList_shouldReturnCorrectRowForNameAndUser() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);

        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList("bookmark list 1 name", user);
        assertNotNull(bl);
        assertEquals(Long.valueOf(1), bl.getId());
        assertNotNull(bl.getOwner());
        assertEquals(Long.valueOf(1), bl.getOwner().getId());
        assertEquals("bookmark list 1 name", bl.getName());
        assertEquals("bookmark list 1 desc", bl.getDescription());
        assertEquals(2, bl.getItems().size());
    }

    /**
     * @see JPADAO#getBookmarkList(String,User)
     * @verifies return null if no result found
     */
    @Test
    void getBookmarkList_shouldReturnNullIfNoResultFound() throws Exception {
        assertNull(DataManager.getInstance().getDao().getBookmarkList("notfound", null));
    }

    /**
     * @see JPADAO#getBookmarkListByShareKey(String)
     * @verifies return correct row
     */
    @Test
    void getBookmarkListByShareKey_shouldReturnCorrectRow() throws Exception {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkListByShareKey("c548e2ea6915acbfa17c3dc6f453f5b1");
        assertNotNull(bl);
        assertEquals(Long.valueOf(1), bl.getId());
    }

    /**
     * @see JPADAO#getBookmarkListByShareKey(String)
     * @verifies return null if no result found
     */
    @Test
    void getBookmarkListByShareKey_shouldReturnNullIfNoResultFound() throws Exception {
        assertNull(DataManager.getInstance().getDao().getBookmarkListByShareKey("123"));
    }

    /**
     * @throws DAOException
     * @see JPADAO#getBookmarkListByShareKey(String)
     * @verifies throw IllegalArgumentException if shareKey empty
     */
    @Test
    void getBookmarkListByShareKey_shouldThrowIllegalArgumentExceptionIfShareKeyEmpty() throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        Assertions.assertThrows(IllegalArgumentException.class, () -> dao.getBookmarkListByShareKey(""));
    }

    @Test
    void addBookmarkListTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);

        BookmarkList bl = new BookmarkList();
        bl.setName("add bookmark list test");
        bl.setOwner(user);
        bl.setDescription("add bookmark list test desc");
        Bookmark item = new Bookmark("PPNTEST", "add bookmark 1 main title", "add bookmark 1 name");
        item.setDescription("add bookmark 1 desc");
        bl.addItem(item);
        assertTrue(DataManager.getInstance().getDao().addBookmarkList(bl));

        BookmarkList bl2 = DataManager.getInstance().getDao().getBookmarkList("add bookmark list test", user);
        assertNotNull(bl2);
        assertNotNull(bl2.getId());
        assertEquals(user, bl2.getOwner());
        assertEquals(bl.getName(), bl2.getName());
        assertEquals(bl.getDescription(), bl2.getDescription());
        assertEquals(1, bl2.getItems().size());
        Bookmark item2 = bl2.getItems().get(0);
        assertEquals(bl2, item2.getBookmarkList());
        assertEquals("PPNTEST", item2.getPi());
        //        assertEquals("add bookmark 1 main title", item2.getMainTitle());
        assertEquals("add bookmark 1 name", item2.getName());
        assertEquals("add bookmark 1 desc", item2.getDescription());
    }

    @Test
    void updateBookmarkListTest() throws DAOException {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(1);
        assertNotNull(bl);
        assertEquals(2, bl.getItems().size());

        int numBookmarkLists = DataManager.getInstance().getDao().getAllBookmarkLists().size();

        Bookmark item = new Bookmark("PPNTEST", "addBookmarkTest item main title", "addBookmarkTest item name");
        item.setDescription("addBookmarkTest item desc");
        bl.addItem(item);
        bl.setName("bookmark list 1 new name");
        bl.setDescription("bookmark list 1 new desc");
        assertTrue(DataManager.getInstance().getDao().updateBookmarkList(bl));
        //        assertNotNull(item.getId());

        int numBookmarkLists2 = DataManager.getInstance().getDao().getAllBookmarkLists().size();
        assertEquals(numBookmarkLists, numBookmarkLists2);

        BookmarkList bl2 = DataManager.getInstance().getDao().getBookmarkList(bl.getId());
        assertNotNull(bl2);
        assertEquals(bl.getId(), bl2.getId());
        assertEquals(bl.getName(), bl2.getName());
        assertEquals(bl.getDescription(), bl2.getDescription());
        assertEquals(3, bl2.getItems().size());
    }

    @Test
    void deleteBookmarkListTest() throws DAOException {
        assertEquals(3, DataManager.getInstance().getDao().getAllBookmarkLists().size());
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(1);
        assertNotNull(bl);
        assertTrue(DataManager.getInstance().getDao().deleteBookmarkList(bl));
        assertNull(DataManager.getInstance().getDao().getBookmarkList(1));
        assertEquals(2, DataManager.getInstance().getDao().getAllBookmarkLists().size());
    }

    @Test
    void removeBookMarkTest() throws DAOException {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(1);
        assertNotNull(bl);
        assertEquals(2, bl.getItems().size());
        bl.removeItem(bl.getItems().get(0));
        assertTrue(DataManager.getInstance().getDao().updateBookmarkList(bl));

        BookmarkList bl2 = DataManager.getInstance().getDao().getBookmarkList(1);
        assertNotNull(bl2);
        assertEquals(1, bl2.getItems().size());
    }

    /**
     * @see JPADAO#getComments(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    void getComments_shouldSortResultsCorrectly() throws Exception {
        List<Comment> ret = DataManager.getInstance().getDao().getComments(0, 2, "body", true, null, null);
        assertEquals(2, ret.size());
        assertEquals(Long.valueOf(4), ret.get(0).getId());
        assertEquals(Long.valueOf(3), ret.get(1).getId());
    }

    /**
     * @see JPADAO#getComments(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    void getComments_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("targetPI", "pi_1");
        filterMap.put("body", "ment 2");
        List<Comment> ret = DataManager.getInstance().getDao().getComments(0, 2, null, true, filterMap, null);
        assertEquals(1, ret.size());
        assertEquals("comment 2 text", ret.get(0).getText());
    }

    /**
     * @see JPADAO#getComments(int,int,String,boolean,Map,Set)
     * @verifies apply target pi filter correctly
     */
    @Test
    void getComments_shouldApplyTargetPiFilterCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        // TODO update test dataset to include comments with different target_pi
        List<Comment> ret = DataManager.getInstance().getDao().getComments(0, 10, null, true, filterMap, Collections.singleton("PI_1"));
        assertEquals(4, ret.size());
    }

    /**
     * @see JPADAO#getUserGroups(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    void getUserGroups_shouldSortResultsCorrectly() throws Exception {
        List<UserGroup> userGroups = DataManager.getInstance().getDao().getUserGroups(0, 2, "name", true, null);
        assertEquals(2, userGroups.size());
        assertEquals(Long.valueOf(3), userGroups.get(0).getId());
        assertEquals(Long.valueOf(2), userGroups.get(1).getId());
    }

    /**
     * @see JPADAO#getUserGroups(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    void getUserGroups_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("description", "no members");
        filterMap.put("name", "user group 2 name");
        List<UserGroup> ret = DataManager.getInstance().getDao().getUserGroups(0, 2, null, true, filterMap);
        assertEquals(1, ret.size());
        assertEquals("user group 2 name", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getUsers(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    void getUsers_shouldSortResultsCorrectly() throws Exception {
        List<User> users = DataManager.getInstance().getDao().getUsers(0, 2, "score", true, null);
        assertEquals(2, users.size());
        assertEquals(Long.valueOf(2), users.get(0).getId());
        assertEquals(Long.valueOf(1), users.get(1).getId());
    }

    /**
     * @see JPADAO#getUsers(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    void getUsers_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("email", "1@users.org");
        List<User> ret = DataManager.getInstance().getDao().getUsers(0, 2, null, true, filterMap);
        assertEquals(1, ret.size());
        assertEquals("1@users.org", ret.get(0).getEmail());
    }

    /**
     * @see JPADAO#getUsersByPropertyValue(String,String)
     * @verifies return correct rows
     */
    @Test
    void getUsersByPropertyValue_shouldReturnCorrectRows() throws Exception {
        List<User> result = DataManager.getInstance().getDao().getUsersByPropertyValue("foo", "bar");
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(2), result.get(0).getId());
        assertEquals(Long.valueOf(3), result.get(1).getId());
    }
    
    /**
     * @see JPADAO#getAdminUsers()
     * @verifies return correct rows
     */
    @Test
    void getAdminUsers_shouldReturnCorrectRows() throws Exception {
        List<User> result = DataManager.getInstance().getDao().getAdminUsers();
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(1), result.get(0).getId());
    }

    /**
     * @see JPADAO#getSearches(User,int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    void getSearches_shouldSortResultsCorrectly() throws Exception {
        List<Search> ret = DataManager.getInstance().getDao().getSearches(null, 0, 10, "name", true, null);
        assertEquals(3, ret.size());
        assertEquals(Long.valueOf(3), ret.get(0).getId());
        assertEquals(Long.valueOf(2), ret.get(1).getId());
        assertEquals(Long.valueOf(1), ret.get(2).getId());
    }

    /**
     * @see JPADAO#getSearchCount(User,Map)
     * @verifies filter results correctly
     */
    @Test
    void getSearchCount_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("newHitsNotification", "true");
        assertEquals(2, DataManager.getInstance().getDao().getSearchCount(null, filterMap));
    }

    /**
     * @see JPADAO#getSearches(User,int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    void getSearches_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("query", "y");
        filterMap.put("name", "search 1");
        List<Search> ret = DataManager.getInstance().getDao().getSearches(DataManager.getInstance().getDao().getUser(1), 0, 2, null, true, filterMap);
        assertEquals(1, ret.size());
        assertEquals("search 1", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getRoles(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    void getRoles_shouldSortResultsCorrectly() throws Exception {
        List<Role> ret = DataManager.getInstance().getDao().getRoles(0, 2, "name", true, null);
        assertEquals(2, ret.size());
        assertEquals(Long.valueOf(2), ret.get(0).getId());
        assertEquals(Long.valueOf(1), ret.get(1).getId());
    }

    /**
     * @see JPADAO#getRoles(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    void getRoles_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("name", "role 2 name");
        filterMap.put("description", "unused");
        List<Role> ret = DataManager.getInstance().getDao().getRoles(0, 2, null, true, filterMap);
        assertEquals(1, ret.size());
        assertEquals("role 2 name", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getLicenseTypes(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    void getLicenseTypes_shouldSortResultsCorrectly() throws Exception {
        // asc
        List<LicenseType> ret = DataManager.getInstance().getDao().getLicenseTypes(0, 2, "name", true, null);
        assertEquals(2, ret.size());
        assertEquals(Long.valueOf(6), ret.get(0).getId());
        assertEquals(Long.valueOf(4), ret.get(1).getId());
        // desc
        ret = DataManager.getInstance().getDao().getLicenseTypes(0, 2, "name", false, null);
        assertEquals(2, ret.size());
        assertEquals(Long.valueOf(1), ret.get(0).getId());
        assertEquals(Long.valueOf(2), ret.get(1).getId());
    }

    /**
     * @see JPADAO#getLicenseTypes(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    void getLicenseTypes_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("name", "license type 2 name");
        filterMap.put("description", "unused");
        List<LicenseType> ret = DataManager.getInstance().getDao().getLicenseTypes(0, 2, null, true, filterMap);
        assertEquals(1, ret.size());
        assertEquals("license type 2 name", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getCoreLicenseTypes(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    void getCoreLicenseTypes_shouldSortResultsCorrectly() throws Exception {
        // TODO add more core types to test DB

        // asc
        List<LicenseType> ret = DataManager.getInstance().getDao().getCoreLicenseTypes(0, 10, "name", true, null);
        assertEquals(1, ret.size());
        assertEquals(Long.valueOf(5), ret.get(0).getId());
        // desc
        ret = DataManager.getInstance().getDao().getCoreLicenseTypes(0, 10, "name", false, null);
        assertEquals(1, ret.size());
        assertEquals(Long.valueOf(5), ret.get(0).getId());
    }

    /**
     * @see JPADAO#getCoreLicenseTypes(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    void getCoreLicenseTypes_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("name", "cms");
        filterMap.put("description", "cms");
        List<LicenseType> ret = DataManager.getInstance().getDao().getCoreLicenseTypes(0, 10, null, true, filterMap);
        assertEquals(1, ret.size());
        assertEquals("CMS", ret.get(0).getName());

        filterMap.put("description", "notfound");
        assertEquals(0, DataManager.getInstance().getDao().getCoreLicenseTypes(0, 10, null, true, filterMap).size());
    }

    /**
     * @see JPADAO#getLicenses(LicenseType)
     * @verifies return correct values
     */
    @Test
    void getLicenses_shouldReturnCorrectValues() throws Exception {
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        assertNotNull(licenseType);
        List<License> result = DataManager.getInstance().getDao().getLicenses(licenseType);
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(1), result.get(0).getId());
        assertEquals(Long.valueOf(2), result.get(1).getId());
    }

    /**
     * @see JPADAO#getLicenseCount(LicenseType)
     * @verifies return correct value
     */
    @Test
    void getLicenseCount_shouldReturnCorrectValue() throws Exception {
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        assertNotNull(licenseType);
        assertEquals(2, DataManager.getInstance().getDao().getLicenseCount(licenseType));
    }

    /**
     * @see JPADAO#getIpRanges(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    void getIpRanges_shouldSortResultsCorrectly() throws Exception {
        List<IpRange> ret = DataManager.getInstance().getDao().getIpRanges(0, 2, "name", true, null);
        assertEquals(2, ret.size());
        assertEquals(Long.valueOf(2), ret.get(0).getId());
        assertEquals(Long.valueOf(1), ret.get(1).getId());
    }

    /**
     * @see JPADAO#getIpRanges(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    void getIpRanges_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("name", "localhost");
        filterMap.put("description", "2 desc");
        List<IpRange> ret = DataManager.getInstance().getDao().getIpRanges(0, 2, null, true, filterMap);
        assertEquals(1, ret.size());
        assertEquals("localhost2", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getAllCMSMediaItems()
     * @verifies return all items
     */
    @Test
    void getAllCMSMediaItems_shouldReturnAllItems() throws Exception {
        assertEquals(4, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
    }

    /**
     * @see JPADAO#getCMSMediaItem(long)
     * @verifies return correct item
     */
    @Test
    void getCMSMediaItem_shouldReturnCorrectItem() throws Exception {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(1);
        assertNotNull(item);
        assertEquals(Long.valueOf(1), item.getId());
        assertEquals("image1.jpg", item.getFileName());
        assertEquals(2, item.getMetadata().size());
        assertEquals("de", item.getMetadata().get(0).getLanguage());
        assertEquals("Bild 1", item.getMetadata().get(0).getName());
        assertEquals("Beschreibung 1", item.getMetadata().get(0).getDescription());
        assertEquals("en", item.getMetadata().get(1).getLanguage());
        assertEquals("Image 1", item.getMetadata().get(1).getName());
        assertEquals("Description 1", item.getMetadata().get(1).getDescription());
    }

    /**
     * @see JPADAO#addCMSMediaItem(CMSMediaItem)
     * @verifies add item correctly
     */
    @Test
    void addCMSMediaItem_shouldAddItemCorrectly() throws Exception {
        assertEquals(4, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        CMSMediaItem item = new CMSMediaItem();
        item.setFileName("image5.jpg");
        CMSMediaItemMetadata md = new CMSMediaItemMetadata();
        md.setLanguage("eu");
        md.setName("Ongi etorriak");
        md.setDescription("bla");
        item.getMetadata().add(md);
        assertTrue(DataManager.getInstance().getDao().addCMSMediaItem(item));

        assertEquals(5, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        assertNotNull(item.getId());

        CMSMediaItem item2 = DataManager.getInstance().getDao().getCMSMediaItem(item.getId());
        assertNotNull(item2);
        assertEquals(item, item2);
        assertEquals(item.getFileName(), item2.getFileName());
        assertEquals(1, item2.getMetadata().size());
        assertEquals(md, item2.getMetadata().get(0));
        assertEquals(md.getLanguage(), item2.getMetadata().get(0).getLanguage());
        assertEquals(md.getName(), item2.getMetadata().get(0).getName());
        assertEquals(md.getDescription(), item2.getMetadata().get(0).getDescription());
    }

    /**
     * @see JPADAO#updateCMSMediaItem(CMSMediaItem)
     * @verifies update item correctly
     */
    @Test
    void updateCMSMediaItem_shouldUpdateItemCorrectly() throws Exception {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(1);
        assertNotNull(item);
        assertEquals(2, item.getMetadata().size());
        item.setFileName("image_new.jpg");
        item.getMetadata().remove(item.getMetadata().get(0));
        assertTrue(DataManager.getInstance().getDao().updateCMSMediaItem(item));

        assertEquals(4, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        CMSMediaItem item2 = DataManager.getInstance().getDao().getCMSMediaItem(1);
        assertNotNull(item2);
        assertEquals(item, item2);
        assertEquals(item.getFileName(), item2.getFileName());
        assertEquals(1, item2.getMetadata().size());
    }

    /**
     * @see JPADAO#deleteCMSMediaItem(CMSMediaItem)
     * @verifies delete item correctly
     */
    @Test
    void deleteCMSMediaItem_shouldDeleteItemCorrectly() throws Exception {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(2);
        assertNotNull(item);
        assertTrue(DataManager.getInstance().getDao().deleteCMSMediaItem(item));
        assertEquals(3, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        assertNull(DataManager.getInstance().getDao().getCMSMediaItem(2));
    }

    /**
     * @see JPADAO#deleteCMSMediaItem(CMSMediaItem)
     * @verifies not delete referenced items
     */
    @Test
    void deleteCMSMediaItem_shouldNotDeleteReferencedItems() throws Exception {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(1);
        assertNotNull(item);
        assertFalse(DataManager.getInstance().getDao().deleteCMSMediaItem(item));
        assertEquals(4, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        assertNotNull(DataManager.getInstance().getDao().getCMSMediaItem(1));
    }

    /**
     * @see JPADAO#getAllCMSPages()
     * @verifies return all pages
     */
    @Test
    void getAllCMSPages_shouldReturnAllPages() throws Exception {
        assertEquals(3, DataManager.getInstance().getDao().getAllCMSPages().size());
    }

    /**
     * @see JPADAO#getCMSPageCount(Map)
     * @verifies return correct count
     */
    @Test
    void getCMSPageCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(3L, DataManager.getInstance().getDao().getCMSPageCount(null, null, null, null));
    }

    /**
     * @see JPADAO#getCMSPagesByClassification(String)
     * @verifies return all pages with given classification
     */
    @Test
    void getCMSPagesByClassification_shouldReturnAllPagesWithGivenClassification() throws Exception {
        CMSCategory news = DataManager.getInstance().getDao().getCategoryByName("news");
        assertEquals(2, DataManager.getInstance().getDao().getCMSPagesByCategory(news).size());
    }

    /**
     * @see JPADAO#getCMSPagesForRecord(String)
     * @verifies return all pages with the given related pi
     */
    @Test
    void getCMSPagesForRecord_shouldReturnAllPagesWithTheGivenRelatedPi() throws Exception {
        CMSCategory c = DataManager.getInstance().getDao().getCategoryByName(CMSPage.CLASSIFICATION_OVERVIEWPAGE);
        assertEquals(1, DataManager.getInstance().getDao().getCMSPagesForRecord("PI_1", c).size());
    }

    /**
     * @see JPADAO#getCMSPagesWithRelatedPi(int,int,Date,Date)
     * @verifies return correct rows
     */
    @Test
    void getCMSPagesWithRelatedPi_shouldReturnCorrectRows() throws Exception {
        assertEquals(1,
                DataManager.getInstance()
                        .getDao()
                        .getCMSPagesWithRelatedPi(0, 100, LocalDateTime.of(2015, 1, 1, 0, 0), LocalDateTime.of(2015, 12, 31, 0, 0))
                        .size());
        // Wrong date range
        assertEquals(0,
                DataManager.getInstance()
                        .getDao()
                        .getCMSPagesWithRelatedPi(0, 100, LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.of(2016, 12, 31, 0, 0))
                        .size());
    }

    /**
     * @see JPADAO#isCMSPagesForRecordHaveUpdates(String,CMSCategory,Date,Date)
     * @verifies return correct value
     */
    @Test
    void isCMSPagesForRecordHaveUpdates_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance()
                .getDao()
                .isCMSPagesForRecordHaveUpdates("PI_1", null, LocalDateTime.of(2015, 1, 1, 0, 0), LocalDateTime.of(2015, 12, 31, 0, 0)));
        assertFalse(DataManager.getInstance()
                .getDao()
                .isCMSPagesForRecordHaveUpdates("PI_1", null, LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.of(2016, 12, 31, 0, 0)));
        assertFalse(DataManager.getInstance().getDao().isCMSPagesForRecordHaveUpdates("PI_2", null, null, null));
    }

    /**
     * @see JPADAO#getCMSPageWithRelatedPiCount(Date,Date)
     * @verifies return correct count
     */
    @Test
    void getCMSPageWithRelatedPiCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(1,
                DataManager.getInstance()
                        .getDao()
                        .getCMSPageWithRelatedPiCount(LocalDateTime.of(2015, 1, 1, 0, 0), LocalDateTime.of(2015, 12, 31, 0, 0)));
    }

    /**
     * @see JPADAO#getCMSPageDefaultViewForRecord(String)
     * @verifies return correct result
     */
    @Test
    void getCMSPageDefaultViewForRecord_shouldReturnCorrectResult() throws Exception {
        CMSPage page = DataManager.getInstance().getDao().getCMSPageDefaultViewForRecord("PI_1");
        assertNotNull(page);
        assertEquals(Long.valueOf(3), page.getId());
    }

    /**
     * @see JPADAO#deleteCMSPage(CMSPage)
     * @verifies delete page correctly
     */
    @Test
    void deleteCMSPage_shouldDeletePageCorrectly() throws Exception {
        assertEquals(3, DataManager.getInstance().getDao().getAllCMSPages().size());
        CMSPage page = DataManager.getInstance().getDao().getCMSPage(1);
        assertNotNull(page);
        assertTrue(DataManager.getInstance().getDao().deleteCMSPage(page));
        assertEquals(2, DataManager.getInstance().getDao().getAllCMSPages().size());
        assertNull(DataManager.getInstance().getDao().getCMSPage(1));
    }

    /**
     * @see JPADAO#getAllTopCMSNavigationItems()
     * @verifies return all top items
     */
    @Test
    void getAllTopCMSNavigationItems_shouldReturnAllTopItems() throws Exception {
        List<CMSNavigationItem> items = DataManager.getInstance().getDao().getAllTopCMSNavigationItems();
        assertEquals(2, items.size());
        Collections.sort(items);
        assertEquals(Long.valueOf(1), items.get(0).getId());
        assertEquals(Long.valueOf(4), items.get(1).getId());
    }

    /**
     * @see JPADAO#getCMSNavigationItem(long)
     * @verifies return correct item and child items
     */
    @Test
    void getCMSNavigationItem_shouldReturnCorrectItemAndChildItems() throws Exception {
        CMSNavigationItem item = DataManager.getInstance().getDao().getCMSNavigationItem(1);
        assertNotNull(item);
        assertEquals(Long.valueOf(1), item.getId());
        assertEquals("item 1", item.getItemLabel());
        assertEquals("url 1", item.getPageUrl());
        assertEquals(Integer.valueOf(1), item.getOrder());
        assertNull(item.getParentItem());
        assertEquals(2, item.getChildItems().size());
        Collections.sort(item.getChildItems());
        assertEquals(Long.valueOf(2), item.getChildItems().get(0).getId());
        assertEquals(Long.valueOf(3), item.getChildItems().get(1).getId());
        assertEquals(Integer.valueOf(1), item.getChildItems().get(0).getOrder());
        assertEquals(Integer.valueOf(2), item.getChildItems().get(1).getOrder());
    }

    /**
     * @see JPADAO#addCMSNavigationItem(CMSNavigationItem)
     * @verifies add item and child items correctly
     */
    @Test
    void addCMSNavigationItem_shouldAddItemAndChildItemsCorrectly() throws Exception {
        assertEquals(2, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        CMSNavigationItem item = new CMSNavigationItem();
        item.setItemLabel("new item");
        item.setPageUrl("url");
        item.setOrder(3);
        CMSNavigationItem childItem = new CMSNavigationItem();
        childItem.setItemLabel("child item");
        childItem.setPageUrl("child url");
        childItem.setParentItem(item); // this also adds the child item to the parent
        childItem.setOrder(1);

        assertTrue(DataManager.getInstance().getDao().addCMSNavigationItem(item));
        assertEquals(3, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        assertNotNull(item.getId());
        assertNotNull(childItem.getId());

        CMSNavigationItem item2 = DataManager.getInstance().getDao().getCMSNavigationItem(item.getId());
        assertNotNull(item2);
        assertEquals(item.getId(), item2.getId());
        assertEquals(item.getChildItems().size(), item2.getChildItems().size());
        assertEquals(childItem.getId(), item2.getChildItems().get(0).getId());
    }

    /**
     * @see JPADAO#updateCMSNavigationItem(CMSNavigationItem)
     * @verifies update item and child items correctly
     */
    @Test
    void updateCMSNavigationItem_shouldUpdateItemAndChildItemsCorrectly() throws Exception {
        assertEquals(2, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        CMSNavigationItem item = DataManager.getInstance().getDao().getCMSNavigationItem(1);
        assertNotNull(item);
        item.setPageUrl("new url");
        item.getChildItems().get(0).setPageUrl("new child url");

        assertTrue(DataManager.getInstance().getDao().updateCMSNavigationItem(item));
        assertEquals(2, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());

        CMSNavigationItem item2 = DataManager.getInstance().getDao().getCMSNavigationItem(item.getId());
        assertNotNull(item2);
        assertEquals(item.getChildItems().size(), item2.getChildItems().size());
        assertEquals(item.getPageUrl(), item2.getPageUrl());
        assertEquals(item.getChildItems().get(0).getPageUrl(), item2.getChildItems().get(0).getPageUrl());
    }

    /**
     * @see JPADAO#deleteCMSNavigationItem(CMSNavigationItem)
     * @verifies delete item and child items correctly
     */
    @Test
    void deleteCMSNavigationItem_shouldDeleteItemAndChildItemsCorrectly() throws Exception {
        assertEquals(2, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        CMSNavigationItem item = DataManager.getInstance().getDao().getCMSNavigationItem(1);
        assertNotNull(item);
        assertTrue(DataManager.getInstance().getDao().deleteCMSNavigationItem(item));
        assertEquals(1, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        assertNull(DataManager.getInstance().getDao().getCMSNavigationItem(1));
        assertNull(DataManager.getInstance().getDao().getCMSNavigationItem(2));
        assertNull(DataManager.getInstance().getDao().getCMSNavigationItem(3));
    }

    /*
     * Unused since the tested behaviour (licence entity ids being bein immediately visible in owning user group)
     * Is no longer supported because of transaction-scoped EntityManagers; and the purpose of this behaviour is unclear
     */
    //
    //    /**
    //     * @see JPADAO#updateUserGroup(UserGroup)
    //     * @verifies set id on new license
    //     */
    //    @Test
    //    void updateUserGroup_shouldSetIdOnNewLicense() throws Exception {
    //        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
    //        assertNotNull(userGroup);
    //        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
    //        assertNotNull(licenseType);
    //        License license = new License();
    //        license.setDescription("xxx");
    //        license.setLicenseType(licenseType);
    //        userGroup.addLicense(license);
    //        assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
    //        boolean licenseFound = false;
    //        for (License l : userGroup.getLicenses()) {
    //            if ("xxx".equals(l.getDescription())) {
    //                licenseFound = true;
    //                assertNotNull(l.getId());
    //            }
    //        }
    //        assertTrue(licenseFound);
    //    }

    /**
     * @see JPADAO#getUserCount()
     * @verifies return correct count
     */
    @Test
    void getUserCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(3L, DataManager.getInstance().getDao().getUserCount(null));
    }

    /**
     * Should return 2 results: user1 by its name and user 2 by its user group
     *
     * @see JPADAO#getUserCount(Map)
     * @verifies filter correctly
     */
    @Test
    void getUserCount_shouldFilterCorrectly() throws Exception {
        Map<String, String> filters = new HashMap<>();
        filters.put("filter", "1");
        assertEquals(2L, DataManager.getInstance().getDao().getUserCount(filters));
    }

    /**
     * @see JPADAO#getIpRangeCount()
     * @verifies return correct count
     */
    @Test
    void getIpRangeCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(2L, DataManager.getInstance().getDao().getIpRangeCount(null));
    }

    /**
     * @see JPADAO#getLicenseTypeCount()
     * @verifies return correct count
     */
    @Test
    void getLicenseTypeCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(NUM_LICENSE_TYPES - 1, DataManager.getInstance().getDao().getLicenseTypeCount(null));
    }

    /**
     * @see JPADAO#getRoleCount()
     * @verifies return correct count
     */
    @Test
    void getRoleCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(2L, DataManager.getInstance().getDao().getRoleCount(null));
    }

    /**
     * @see JPADAO#getUserGroupCount()
     * @verifies return correct count
     */
    @Test
    void getUserGroupCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(3L, DataManager.getInstance().getDao().getUserGroupCount(null));
    }

    /**
     * @see JPADAO#getAllDownloadJobs()
     * @verifies return all objects
     */
    @Test
    void getAllDownloadJobs_shouldReturnAllObjects() throws Exception {
        assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());
    }

    /**
     * @see JPADAO#getDownloadJob(long)
     * @verifies return correct object
     */
    @Test
    void getDownloadJob_shouldReturnCorrectObject() throws Exception {
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(1);
            assertNotNull(job);
            assertEquals(PDFDownloadJob.class, job.getClass());
            assertEquals(Long.valueOf(1), job.getId());
            assertEquals("PI_1", job.getPi());
            assertNull(job.getLogId());
            assertEquals(DownloadJob.generateDownloadJobId(job.getType(), job.getPi(), job.getLogId()), job.getIdentifier());
            assertEquals(3600, job.getTtl());
            assertEquals(JobStatus.WAITING, job.getStatus());
            assertEquals(1, job.getObservers().size());
            assertEquals("viewer@intranda.com", job.getObservers().get(0));
        }
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(2);
            assertNotNull(job);
            assertEquals(EPUBDownloadJob.class, job.getClass());
            assertEquals(DownloadJob.generateDownloadJobId(job.getType(), job.getPi(), job.getLogId()), job.getIdentifier());
        }
    }

    /**
     * @see JPADAO#getDownloadJobByIdentifier(String)
     * @verifies return correct object
     */
    @Test
    void getDownloadJobByIdentifier_shouldReturnCorrectObject() throws Exception {
        DownloadJob job =
                DataManager.getInstance().getDao().getDownloadJobByIdentifier("187277c96410b2358a36e2eb6c8ad76f8610a022d2cd95b180b94a76a1cb118a");
        assertNotNull(job);
        assertEquals(PDFDownloadJob.class, job.getClass());
        assertEquals(Long.valueOf(1), job.getId());
        assertEquals("PI_1", job.getPi());
        assertNull(job.getLogId());
        assertEquals(PDFDownloadJob.generateDownloadJobId(job.getType(), job.getPi(), job.getLogId()), job.getIdentifier());
        assertEquals(3600, job.getTtl());
        assertEquals(JobStatus.WAITING, job.getStatus());
        assertEquals(1, job.getObservers().size());
        assertEquals("viewer@intranda.com", job.getObservers().get(0));
    }

    /**
     * @see JPADAO#getDownloadJobByMetadata(String,String,String)
     * @verifies return correct object
     */
    @Test
    void getDownloadJobByMetadata_shouldReturnCorrectObject() throws Exception {
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByMetadata(PDFDownloadJob.LOCAL_TYPE, "PI_1", null);
            assertNotNull(job);
            assertEquals(PDFDownloadJob.class, job.getClass());
            assertEquals(Long.valueOf(1), job.getId());
        }
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByMetadata(EPUBDownloadJob.LOCAL_TYPE, "PI_1", "LOG_0001");
            assertNotNull(job);
            assertEquals(EPUBDownloadJob.class, job.getClass());
            assertEquals(Long.valueOf(2), job.getId());
        }
    }

    /**
     * @see JPADAO#addDownloadJob(DownloadJob)
     * @verifies add object correctly
     */
    @Test
    void addDownloadJob_shouldAddObjectCorrectly() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        PDFDownloadJob job = new PDFDownloadJob("PI_2", "LOG_0002", now, 3600);
        job.generateDownloadIdentifier();
        job.setStatus(JobStatus.WAITING);
        assertNotNull(job.getIdentifier());
        job.getObservers().add("a@b.com");

        assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());
        assertTrue(DataManager.getInstance().getDao().addDownloadJob(job));
        assertNotNull(job.getId());
        assertEquals(3, DataManager.getInstance().getDao().getAllDownloadJobs().size());

        DownloadJob job2 = DataManager.getInstance().getDao().getDownloadJob(job.getId());
        assertNotNull(job2);
        assertEquals(job.getId(), job2.getId());
        assertEquals(job.getPi(), job2.getPi());
        assertEquals(job.getLogId(), job2.getLogId());
        assertEquals(job.getIdentifier(), job2.getIdentifier());
        // Compare date strings instead of LocalDateTime due to differences in milisecond precision between JVMs
        assertEquals(DateTools.FORMATTERISO8601DATETIMEMS.format(now),
                DateTools.FORMATTERISO8601DATETIMEMS.format(job2.getLastRequested()));
        assertEquals(job.getTtl(), job2.getTtl());
        assertEquals(job.getStatus(), job2.getStatus());
    }

    /**
     * @see JPADAO#updateDownloadJob(DownloadJob)
     * @verifies update object correctly
     */
    @Test
    void updateDownloadJob_shouldUpdateObjectCorrectly() throws Exception {
        assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());

        DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(1);
        assertNotNull(job);
        LocalDateTime now = LocalDateTime.now();
        job.setLastRequested(now);
        job.setStatus(JobStatus.READY);
        job.getObservers().add("newobserver@example.com");

        assertTrue(DataManager.getInstance().getDao().updateDownloadJob(job));
        assertEquals(2, job.getObservers().size(), "Too many observers after updateDownloadJob");
        List<DownloadJob> allJobs = DataManager.getInstance().getDao().getAllDownloadJobs();
        assertEquals(2, allJobs.size());
        assertEquals(2, job.getObservers().size(), "Too many observers after getAllDownloadJobs");

        DownloadJob job2 = DataManager.getInstance().getDao().getDownloadJob(job.getId());
        assertNotNull(job2);
        assertEquals(job.getId(), job2.getId());
        // Compare date strings instead of LocalDateTime due to differences in milisecond precision between JVMs
        assertEquals(DateTools.FORMATTERISO8601DATETIMEMS.format(now),
                DateTools.FORMATTERISO8601DATETIMEMS.format(job2.getLastRequested()));
        assertEquals(JobStatus.READY, job2.getStatus());
        assertEquals(2, job2.getObservers().size());
        assertEquals("newobserver@example.com", job2.getObservers().get(1));
    }

    /**
     * @see JPADAO#deleteDownloadJob(DownloadJob)
     * @verifies delete object correctly
     */
    @Test
    void deleteDownloadJob_shouldDeleteObjectCorrectly() throws Exception {
        DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(1);
        assertNotNull(job);
        assertTrue(DataManager.getInstance().getDao().deleteDownloadJob(job));
        assertNull(DataManager.getInstance().getDao().getDownloadJob(1));
    }

    /**
     * @see JPADAO#getUploadJobsForCreatorId(Long)
     * @verifies return rows in correct order
     */
    @Test
    void getUploadJobsForCreatorId_shouldReturnRowsInCorrectOrder() throws Exception {
        List<UploadJob> result = DataManager.getInstance().getDao().getUploadJobsForCreatorId(1L);
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(2), result.get(0).getId());
        assertEquals(Long.valueOf(1), result.get(1).getId());
    }

    /**
     * @see JPADAO#getUploadJobsWithStatus(JobStatus)
     * @verifies return rows with given status
     */
    @Test
    void getUploadJobsWithStatus_shouldReturnRowsWithGivenStatus() throws Exception {
        List<UploadJob> result = DataManager.getInstance().getDao().getUploadJobsWithStatus(JobStatus.WAITING);
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(1), result.get(0).getId());
    }

    /**
     * @see JPADAO#getUploadJobsForCreatorId(Long)
     * @verifies return empty list if creatorId null
     */
    @Test
    void getUploadJobsForCreatorId_shouldReturnEmptyListIfCreatorIdNull() throws Exception {
        List<UploadJob> result = DataManager.getInstance().getDao().getUploadJobsForCreatorId(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCMSPagesCount_shouldReturnCorrectCount() throws Exception {
        long numPages = DataManager.getInstance().getDao().getCMSPageCount(Collections.emptyMap(), null, null, null);
        assertEquals(3, numPages);
    }

    @Test
    void getStaticPageForCMSPage_shouldReturnCorrectResult() throws Exception {
        List<CMSPage> cmsPages = DataManager.getInstance().getDao().getAllCMSPages();
        for (CMSPage page : cmsPages) {
            Optional<CMSStaticPage> staticPage = DataManager.getInstance().getDao().getStaticPageForCMSPage(page).stream().findFirst();
            if (page.getId().equals(1l)) {
                assertTrue(staticPage.isPresent());
                assertEquals("index", staticPage.get().getPageName());
                assertEquals(page, staticPage.get().getCmsPage());
            } else {
                assertFalse(staticPage.isPresent());
            }
        }
    }

    @Test
    void testCreateCMSPageFilter_createValidQueryWithAllParams() throws AccessDeniedException {

        List<String> categories = Arrays.asList(new String[] { "c1", "c2", "c3" });
        List<String> subThemes = Arrays.asList(new String[] { "s1" });
        List<Long> templates = Arrays.asList(new Long[] { 1l, 2l });

        Map<String, Object> params = new HashMap<>();

        String query = JPADAO.createCMSPageFilter(params, "p", templates, subThemes, categories);

        String shouldQuery = "(:tpl1 = p.templateId OR :tpl2 = p.templateId) AND (:thm1 = p.subThemeDiscriminatorValue) AND "
                + "(:cat1 IN (SELECT c.id FROM p.categories c) OR :cat2 IN (SELECT c.id FROM p.categories c) OR :cat3 IN (SELECT c.id FROM p.categories c))";
        assertEquals(shouldQuery, query);

        assertEquals("c1", params.get("cat1"));
        assertEquals("c2", params.get("cat2"));
        assertEquals("c3", params.get("cat3"));
        assertEquals("s1", params.get("thm1"));
        assertEquals(1l, params.get("tpl1"));
        assertEquals(2l, params.get("tpl2"));

    }

    @Test
    void testCreateCMSPageFilter_createValidQueryWithTwoParams() throws AccessDeniedException {

        List<String> categories = Arrays.asList(new String[] { "c1", "c2", "c3" });
        List<String> subThemes = Arrays.asList(new String[] { "s1" });

        Map<String, Object> params = new HashMap<>();

        String query = JPADAO.createCMSPageFilter(params, "p", null, subThemes, categories);

        String shouldQuery = "(:thm1 = p.subThemeDiscriminatorValue) AND "
                + "(:cat1 IN (SELECT c.id FROM p.categories c) OR :cat2 IN (SELECT c.id FROM p.categories c) OR :cat3 IN (SELECT c.id FROM p.categories c))";
        assertEquals(shouldQuery, query);

        assertEquals("c1", params.get("cat1"));
        assertEquals("c2", params.get("cat2"));
        assertEquals("c3", params.get("cat3"));
        assertEquals("s1", params.get("thm1"));
    }

    @Test
    void testCreateCMSPageFilter_createValidQueryWithOneParam() throws AccessDeniedException {

        List<Long> templates = Arrays.asList(new Long[] { 1l, 2l });

        Map<String, Object> params = new HashMap<>();

        String query = JPADAO.createCMSPageFilter(params, "p", templates, null, null);

        String shouldQuery = "(:tpl1 = p.templateId OR :tpl2 = p.templateId)";
        assertEquals(shouldQuery, query);

        assertEquals(1l, params.get("tpl1"));
        assertEquals(2l, params.get("tpl2"));
    }

    /**
     * @see JPADAO#getAllCampaigns()
     * @verifies return all campaigns
     */
    @Test
    void getAllCampaigns_shouldReturnAllCampaigns() throws Exception {
        assertEquals(3, DataManager.getInstance().getDao().getAllCampaigns().size());
    }

    /**
     * @see JPADAO#getCampaigns(int,int,String,boolean,Map)
     * @verifies filter campaigns correctly
     */
    @Test
    void getCampaigns_shouldFilterCampaignsCorrectly() throws Exception {
        assertEquals(3, DataManager.getInstance().getDao().getCampaigns(0, 10, null, false, null).size());
        assertEquals(2,
                DataManager.getInstance().getDao().getCampaigns(0, 10, null, false, Collections.singletonMap("visibility", "PUBLIC")).size());
    }

    /**
     * @see JPADAO#getCampaignCount(Map)
     * @verifies count correctly
     */
    @Test
    void getCampaignCount_shouldCountCorrectly() throws Exception {
        assertEquals(3, DataManager.getInstance().getDao().getCampaignCount(null));
        assertEquals(2, DataManager.getInstance().getDao().getCampaignCount(Collections.singletonMap("visibility", "PUBLIC")));
    }

    /**
     * @see JPADAO#getCampaign(Long)
     * @verifies return correct campaign
     */
    @Test
    void getCampaign_shouldReturnCorrectCampaign() throws Exception {
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            assertNotNull(campaign);
            campaign.setSelectedLocale(Locale.ENGLISH);
            assertEquals(Long.valueOf(1), campaign.getId());
            assertEquals(CampaignVisibility.PUBLIC, campaign.getVisibility());
            assertEquals(StatisticMode.RECORD, campaign.getStatisticMode());
            assertEquals("+DC:varia", campaign.getSolrQuery());
            assertEquals("English title", campaign.getTitle());

            assertEquals(2, campaign.getQuestions().size());
            assertEquals("English text", campaign.getQuestions().get(0).getText().getText(Locale.ENGLISH));

            assertEquals(4, campaign.getStatistics().size());
            assertNotNull(campaign.getStatistics().get("PI_1"));
            assertNotNull(campaign.getStatistics().get("PI_2"));
            assertNotNull(campaign.getStatistics().get("PI_3"));
            assertNotNull(campaign.getStatistics().get("PI_4"));
        }
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(3L);
            assertNotNull(campaign);
            campaign.setSelectedLocale(Locale.ENGLISH);
            assertEquals(Long.valueOf(3), campaign.getId());
            assertEquals(CampaignVisibility.PUBLIC, campaign.getVisibility());
            assertEquals("+PI:PI_5", campaign.getSolrQuery());

            assertEquals(1, campaign.getStatistics().size());
            CampaignRecordStatistic recordStatistic = campaign.getStatistics().get("PI_5");
            assertNotNull(recordStatistic);
            assertEquals(StatisticMode.PAGE, campaign.getStatisticMode());
            assertEquals(2, recordStatistic.getPageStatistics().size());
            assertEquals(Integer.valueOf(1), recordStatistic.getPageStatistics().get("PI_5_1").getPage());
            assertEquals("PI_5", recordStatistic.getPageStatistics().get("PI_5_1").getPi());
        }
    }

    @Test
    void testLoadCampaignWithLogMessage() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        assertNotNull(campaign);
        assertEquals(1, campaign.getLogMessages().size());

        CampaignLogMessage message = campaign.getLogMessages().get(0);
        assertEquals("Eine Nachricht im Log", message.getMessage());
        assertEquals(Long.valueOf(1), message.getCreatorId());
        assertEquals("PI_1", message.getPi());
        assertEquals(campaign, message.getCampaign());
    }

    @Test
    void testCampaignUpdate() throws DAOException {
        Campaign campaign = new Campaign();
        campaign.setTitle("Test titel");
        campaign.setId(2L);
        campaign.setSolrQuery("*:*");
        campaign.setDateCreated(LocalDateTime.now());

        assertTrue(DataManager.getInstance().getDao().updateCampaign(campaign));
        campaign = DataManager.getInstance().getDao().getCampaign(2L);
        assertEquals("Test titel", campaign.getTitle());
    }

    @Test
    void testUpdateCampaignWithLogMessage() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(2L);
        assertNotNull(campaign);

        LogMessage message = new LogMessage("Test", 1l, LocalDateTime.now(), null);
        campaign.addLogMessage(message, "PI_10");
        assertEquals("Test", campaign.getLogMessages().get(0).getMessage());

        DataManager.getInstance().getDao().updateCampaign(campaign);
        campaign = DataManager.getInstance().getDao().getCampaign(2L);
        assertEquals("Test", campaign.getLogMessages().get(0).getMessage());

    }

    /**
     * @see JPADAO#getQuestion(Long)
     * @verifies return correct row
     */
    @Test
    void getQuestion_shouldReturnCorrectRow() throws Exception {
        Question q = DataManager.getInstance().getDao().getQuestion(1L);
        assertNotNull(q);
        assertEquals(Long.valueOf(1), q.getId());
        assertEquals(Long.valueOf(1), q.getOwner().getId());
        assertEquals("English text", q.getText().getText(Locale.ENGLISH));
        assertEquals(QuestionType.PLAINTEXT, q.getQuestionType());
        assertEquals(TargetSelector.RECTANGLE, q.getTargetSelector());
        assertEquals(0, q.getTargetFrequency());
    }

    /**
     * @see JPADAO#getCampaignStatisticsForRecord(String,CrowdsourcingStatus)
     * @verifies return correct rows
     */
    @Test
    void getCampaignStatisticsForRecord_shouldReturnCorrectRows() throws Exception {
        assertEquals(1, DataManager.getInstance().getDao().getCampaignStatisticsForRecord("PI_1", CrowdsourcingStatus.FINISHED).size());
    }

    /**
     * @see JPADAO#getAllAnnotations(String,boolean)
     * @verifies sort correctly
     */
    @Test
    void getAllAnnotations_shouldSortCorrectly() throws Exception {
        {
            List<CrowdsourcingAnnotation> result = DataManager.getInstance().getDao().getAllAnnotations("id", false);
            assertEquals(5, result.size());
            assertEquals(Long.valueOf(1), result.get(0).getId());
            assertEquals(Long.valueOf(2), result.get(1).getId());
            assertEquals(Long.valueOf(3), result.get(2).getId());
            assertEquals(Long.valueOf(4), result.get(3).getId());
            assertEquals(Long.valueOf(5), result.get(4).getId());
        }
        {
            List<CrowdsourcingAnnotation> result = DataManager.getInstance().getDao().getAllAnnotations("id", true);
            assertEquals(5, result.size());
            assertEquals(Long.valueOf(5), result.get(0).getId());
            assertEquals(Long.valueOf(4), result.get(1).getId());
            assertEquals(Long.valueOf(3), result.get(2).getId());
            assertEquals(Long.valueOf(2), result.get(3).getId());
            assertEquals(Long.valueOf(1), result.get(4).getId());
        }
    }

    /**
     * @see JPADAO#getAllAnnotations(String,boolean)
     * @verifies throw IllegalArgumentException if sortField unknown
     */
    @Test
    void getAllAnnotations_shouldThrowIllegalArgumentExceptionIfSortFieldUnknown() throws Exception {
        IDAO dao = DataManager.getInstance().getDao();
        Assertions.assertThrows(IllegalArgumentException.class, () -> dao.getAllAnnotations("foo", false));
    }

    /**
     * @see JPADAO#getAnnotation(Long)
     * @verifies return correct row
     */
    @Test
    void getAnnotation_shouldReturnCorrectRow() throws Exception {
        CrowdsourcingAnnotation annotation = DataManager.getInstance().getDao().getAnnotation(1L);
        assertNotNull(annotation);
        assertEquals(Long.valueOf(1), annotation.getId());
    }

    /**
     * @see JPADAO#getAnnotationsForCampaign(Campaign)
     * @verifies return correct rows
     */
    @Test
    void getAnnotationsForCampaign_shouldReturnCorrectRows() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        assertNotNull(campaign);

        List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaign(campaign);
        assertEquals(3, annotations.size());
        assertEquals(Long.valueOf(1), annotations.get(0).getId());
        assertEquals(Long.valueOf(2), annotations.get(1).getId());
        assertEquals(Long.valueOf(3), annotations.get(2).getId());
    }

    /**
     * @see JPADAO#getAnnotationsForWork(String)
     * @verifies return correct rows
     */
    @Test
    void getAnnotationsForWork_shouldReturnCorrectRows() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        assertNotNull(campaign);

        List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForWork("PI_1");
        assertEquals(3, annotations.size());
        assertEquals(Long.valueOf(1), annotations.get(0).getId());
        assertEquals(Long.valueOf(3), annotations.get(1).getId());
        assertEquals(Long.valueOf(4), annotations.get(2).getId());
    }

    /**
     * @see JPADAO#getAnnotationCountForTarget(String,Integer)
     * @verifies return correct count
     */
    @Test
    void getAnnotationCountForTarget_shouldReturnCorrectCount() throws Exception {
        assertEquals(1, DataManager.getInstance().getDao().getAnnotationCountForTarget("PI_1", 1));
        assertEquals(2, DataManager.getInstance().getDao().getAnnotationCountForTarget("PI_1", null));
    }

    /**
     * @see JPADAO#getAnnotationsForTarget(String,Integer)
     * @verifies return correct rows
     */
    @Test
    void getAnnotationsForTarget_shouldReturnCorrectRows() throws Exception {
        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForTarget("PI_1", 1);
            assertEquals(1, annotations.size());
            assertEquals(Long.valueOf(1), annotations.get(0).getId());
        }
        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForTarget("PI_1", null);
            assertEquals(2, annotations.size());
            assertEquals(Long.valueOf(3), annotations.get(0).getId());
            assertEquals(Long.valueOf(4), annotations.get(1).getId());
        }
    }

    /**
     * @see JPADAO#getAnnotationsForCampaignAndTarget(Campaign,String,Integer)
     * @verifies return correct rows
     */
    @Test
    void getAnnotationsForCampaignAndTarget_shouldReturnCorrectRows() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        assertNotNull(campaign);

        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, "PI_1", 1);
            assertEquals(1, annotations.size());
            assertEquals(Long.valueOf(1), annotations.get(0).getId());
        }
        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, "PI_1", null);
            assertEquals(1, annotations.size());
            assertEquals(Long.valueOf(3), annotations.get(0).getId());
        }
        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, "PI_2", 6);
            assertEquals(1, annotations.size());
            assertEquals(Long.valueOf(2), annotations.get(0).getId());
        }
    }

    /**
     * @see JPADAO#getAnnotationsForCampaignAndWork(Campaign,String)
     * @verifies return correct rows
     */
    @Test
    void getAnnotationsForCampaignAndWork_shouldReturnCorrectRows() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        assertNotNull(campaign);

        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, "PI_1");
            assertEquals(2, annotations.size());
            assertEquals(Long.valueOf(1), annotations.get(0).getId());
            assertEquals(Long.valueOf(3), annotations.get(1).getId());
        }
    }

    /**
     * @see JPADAO#getAnnotationsForUserId(Long)
     * @verifies return correct rows
     */
    @Test
    void getAnnotationsForUserId_shouldReturnCorrectRows() throws Exception {
        List<CrowdsourcingAnnotation> result = DataManager.getInstance().getDao().getAnnotationsForUserId(1L, null, "id", true);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(Long.valueOf(2), result.get(0).getId());
        assertEquals(Long.valueOf(1), result.get(1).getId());
    }

    /**
     * @see JPADAO#getAnnotationsForUserId(Long,Integer,String,boolean)
     * @verifies throw IllegalArgumentException if sortField unknown
     */
    @Test
    void getAnnotationsForUserId_shouldThrowIllegalArgumentExceptionIfSortFieldUnknown() throws Exception {
        IDAO dao = DataManager.getInstance().getDao();
        Assertions.assertThrows(IllegalArgumentException.class, () -> dao.getAnnotationsForUserId(1L, null, "foo", false));
    }

    /**
     * @see JPADAO#getAnnotationCount(Map)
     * @verifies return correct count
     */
    @Test
    void getAnnotationCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(5, DataManager.getInstance().getDao().getAnnotationCount(null));
        assertEquals(3, DataManager.getInstance().getDao().getAnnotationCount(Collections.singletonMap("targetPI", "PI_1")));
    }

    /**
     * @see JPADAO#getAnnotations(int,int,String,boolean,Map)
     * @verifies return correct rows
     */
    @Test
    void getAnnotations_shouldReturnCorrectRows() throws Exception {
        assertEquals(5, DataManager.getInstance().getDao().getAnnotations(0, 10, null, false, null).size());
        assertEquals(2,
                DataManager.getInstance().getDao().getAnnotations(0, 10, null, false, Collections.singletonMap("targetPI", "PI_2")).size());
    }

    /**
     * @see JPADAO#getAnnotations(int,int,String,boolean,Map)
     * @verifies filter by campaign name correctly
     */
    @Test
    void getAnnotations_shouldFilterByCampaignNameCorrectly() throws Exception {
        List<CrowdsourcingAnnotation> result =
                DataManager.getInstance().getDao().getAnnotations(0, 10, null, false, Collections.singletonMap("campaign", "english"));
        assertEquals(3, result.size());
    }

    @Test
    void testGetAllGeoMaps() throws DAOException {
        List<GeoMap> maps = DataManager.getInstance().getDao().getAllGeoMaps();
        assertEquals(2, maps.size());
    }

    @Test
    void testGetPagesUsingMap() throws DAOException {

        GeoMap map1 = DataManager.getInstance().getDao().getGeoMap(1l);
        GeoMap map2 = DataManager.getInstance().getDao().getGeoMap(2l);

        List<CMSPage> embedMap1 = DataManager.getInstance().getDao().getPagesUsingMap(map1);
        List<CMSPage> embedMap2 = DataManager.getInstance().getDao().getPagesUsingMap(map2);

        assertEquals(2, embedMap1.size());
        assertEquals(0, embedMap2.size());

    }

    @Test
    void testUpdateTranslations() throws Exception {
        String newVal = "Kartenbeschreibung 2";
        GeoMap map1 = DataManager.getInstance().getDao().getGeoMap(1l);
        assertEquals("Kartenbeschreibung 1", map1.getDescription("de").getTranslationValue());
        map1.getDescription("de").setTranslationValue(newVal);
        assertEquals(newVal, map1.getDescription("de").getTranslationValue());

        DataManager.getInstance().getDao().updateGeoMap(map1);
        map1 = DataManager.getInstance().getDao().getAllGeoMaps().stream().filter(map -> map.getId() == 1l).findAny().orElse(null);
        assertEquals(newVal, map1.getDescription("de").getTranslationValue());
    }

    /**
     * @see JPADAO#deleteCampaignStatisticsForUser(User)
     * @verifies remove user from creators and reviewers lists correctly
     */
    @Test
    void deleteCampaignStatisticsForUser_shouldRemoveUserFromCreatorsAndReviewersListsCorrectly() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            assertNotNull(campaign);
            assertNotNull(campaign.getStatistics().get("PI_1"));
            assertNotNull(campaign.getStatistics().get("PI_2"));
            assertNotNull(campaign.getStatistics().get("PI_3"));
            assertNotNull(campaign.getStatistics().get("PI_4"));
            assertTrue(campaign.getStatistics().get("PI_1").getAnnotators().contains(user));
            assertTrue(campaign.getStatistics().get("PI_2").getAnnotators().contains(user));
            assertTrue(campaign.getStatistics().get("PI_3").getReviewers().contains(user));
            assertTrue(campaign.getStatistics().get("PI_4").getReviewers().contains(user));
        }

        int rows = DataManager.getInstance().getDao().deleteCampaignStatisticsForUser(user);
        assertEquals(6, rows);
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            assertNotNull(campaign);
            assertNotNull(campaign.getStatistics().get("PI_1"));
            assertNotNull(campaign.getStatistics().get("PI_2"));
            assertNotNull(campaign.getStatistics().get("PI_3"));
            assertNotNull(campaign.getStatistics().get("PI_4"));
            // User should no longer be among the reviewers
            assertFalse(campaign.getStatistics().get("PI_1").getAnnotators().contains(user));
            assertFalse(campaign.getStatistics().get("PI_2").getAnnotators().contains(user));
            assertFalse(campaign.getStatistics().get("PI_3").getReviewers().contains(user));
            assertFalse(campaign.getStatistics().get("PI_4").getReviewers().contains(user));
        }
    }

    /**
     * @see JPADAO#changeCampaignStatisticContributors(User,User)
     * @verifies replace user in creators and reviewers lists correctly
     */
    @Test
    void changeCampaignStatisticContributors_shouldReplaceUserInCreatorsAndReviewersListsCorrectly() throws Exception {
        User fromUser = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(fromUser);
        User toUser = DataManager.getInstance().getDao().getUser(3);
        assertNotNull(toUser);
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            assertNotNull(campaign);
            assertNotNull(campaign.getStatistics().get("PI_1"));
            assertNotNull(campaign.getStatistics().get("PI_2"));
            assertNotNull(campaign.getStatistics().get("PI_3"));
            assertNotNull(campaign.getStatistics().get("PI_4"));
            assertTrue(campaign.getStatistics().get("PI_1").getAnnotators().contains(fromUser));
            assertTrue(campaign.getStatistics().get("PI_2").getAnnotators().contains(fromUser));
            assertTrue(campaign.getStatistics().get("PI_3").getReviewers().contains(fromUser));
            assertTrue(campaign.getStatistics().get("PI_4").getReviewers().contains(fromUser));
            assertFalse(campaign.getStatistics().get("PI_1").getAnnotators().contains(toUser));
            assertFalse(campaign.getStatistics().get("PI_2").getAnnotators().contains(toUser));
            assertFalse(campaign.getStatistics().get("PI_3").getReviewers().contains(toUser));
            assertFalse(campaign.getStatistics().get("PI_4").getReviewers().contains(toUser));
        }

        int rows = DataManager.getInstance().getDao().changeCampaignStatisticContributors(fromUser, toUser);
        assertEquals(6, rows);
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            assertNotNull(campaign);
            assertNotNull(campaign.getStatistics().get("PI_1"));
            assertNotNull(campaign.getStatistics().get("PI_2"));
            assertNotNull(campaign.getStatistics().get("PI_3"));
            assertNotNull(campaign.getStatistics().get("PI_4"));
            // Only toUser should be among the reviewers
            assertFalse(campaign.getStatistics().get("PI_1").getAnnotators().contains(fromUser));
            assertFalse(campaign.getStatistics().get("PI_2").getAnnotators().contains(fromUser));
            assertFalse(campaign.getStatistics().get("PI_3").getReviewers().contains(fromUser));
            assertFalse(campaign.getStatistics().get("PI_4").getReviewers().contains(fromUser));
            assertTrue(campaign.getStatistics().get("PI_1").getAnnotators().contains(toUser));
            assertTrue(campaign.getStatistics().get("PI_2").getAnnotators().contains(toUser));
            assertTrue(campaign.getStatistics().get("PI_3").getReviewers().contains(toUser));
            assertTrue(campaign.getStatistics().get("PI_4").getReviewers().contains(toUser));
        }
    }

    /**
     * @see JPADAO#getUserByNickname(String)
     * @verifies return null if nickname empty
     */
    @Test
    void getUserByNickname_shouldReturnNullIfNicknameEmpty() throws Exception {
        assertNull(DataManager.getInstance().getDao().getUserByNickname(""));
    }

    /**
     * @see JPADAO#getCountPagesUsingCategory(CMSCategory)
     * @verifies return correct value
     */
    @Test
    void getCountPagesUsingCategory_shouldReturnCorrectValue() throws Exception {
        CMSCategory cat = DataManager.getInstance().getDao().getCategory(4L);
        assertNotNull(cat);
        assertEquals(2, DataManager.getInstance().getDao().getCountPagesUsingCategory(cat));
    }

    /**
     * @see JPADAO#getCountMediaItemsUsingCategory(CMSCategory)
     * @verifies return correct value
     */
    @Test
    void getCountMediaItemsUsingCategory_shouldReturnCorrectValue() throws Exception {
        CMSCategory cat = DataManager.getInstance().getDao().getCategory(1L);
        assertNotNull(cat);
        assertEquals(3, DataManager.getInstance().getDao().getCountMediaItemsUsingCategory(cat));
    }

    /**
     * @see JPADAO#createFilterQuery(String,Map,Map)
     * @verifies build multikey filter query correctly
     */
    @Test
    void createFilterQuery_shouldBuildMultikeyFilterQueryCorrectly() {
        Map<String, String> filters = Collections.singletonMap("a-a_b-b_c-c_d-d", "bar");
        Map<String, String> params = new HashMap<>();

        assertEquals(
                "STATIC:query AND ( ( UPPER(a.a.a) LIKE :aabbccdd OR UPPER(a.b.b) LIKE :aabbccdd OR UPPER(a.c.c) LIKE :aabbccdd OR UPPER(a.d.d) LIKE :aabbccdd ) ",
                JPADAO.createFilterQuery("STATIC:query", filters, params));
    }

    @Test
    void createFilterQuery_twoJoinedTables() {
        Map<String, String> filters = Collections.singletonMap("b-B_c-C", "bar");
        Map<String, Object> params = new HashMap<>();

        String expectedFilterString = " LEFT JOIN a.b b LEFT JOIN a.c c WHERE (UPPER(b.B) LIKE :bBcC OR UPPER(c.C) LIKE :bBcC)";
        String filterString = JPADAO.createFilterQuery2("", filters, params);

        assertEquals(expectedFilterString, filterString);
        assertEquals("%BAR%", params.get("bBcC"));
    }

    @Test
    void createFilterQuery_joinedTableAndField() {
        Map<String, String> filters = Collections.singletonMap("B_c-C", "bar");
        Map<String, Object> params = new HashMap<>();

        String expectedFilterString = " LEFT JOIN a.c b WHERE (UPPER(a.B) LIKE :BcC OR UPPER(b.C) LIKE :BcC)";
        String filterString = JPADAO.createFilterQuery2("", filters, params);

        assertEquals(expectedFilterString, filterString);
        assertEquals("%BAR%", params.get("BcC"));
    }

    @Test
    void testGetTermsOfUse() throws DAOException {
        TermsOfUse tou = DataManager.getInstance().getDao().getTermsOfUse();
        assertNotNull(tou);
    }

    @Test
    void testSaveTermsOfUse() throws DAOException {
        TermsOfUse tou = new TermsOfUse();
        tou.setActive(true);
        DataManager.getInstance().getDao().saveTermsOfUse(tou);

        tou = DataManager.getInstance().getDao().getTermsOfUse();
        assertTrue(tou.isActive());
        tou.setTitle("en", "English Title");
        tou.setTitle("de", "German Title");
        tou.setDescription("en", "English description");
        tou.setDescription("de", "German description");

        DataManager.getInstance().getDao().saveTermsOfUse(tou);
        tou = DataManager.getInstance().getDao().getTermsOfUse();
        assertEquals("English Title", tou.getTitle("en").getTranslationValue());
        assertEquals("German Title", tou.getTitle("de").getTranslationValue());
        assertEquals("German description", tou.getDescription("de").getTranslationValue());
        assertEquals("English description", tou.getDescription("en").getTranslationValue());

    }

    @Test
    void testResetUserAgreementsToTermsOfUse() throws DAOException {

        //initially noone has agreed
        List<User> users = DataManager.getInstance().getDao().getAllUsers(true);
        assertTrue(users.stream().allMatch(u -> !u.isAgreedToTermsOfUse()));

        //now all agree
        for (User user : users) {
            user.setAgreedToTermsOfUse(true);
            DataManager.getInstance().getDao().updateUser(user);
        }

        //now all should have agreed
        users = DataManager.getInstance().getDao().getAllUsers(true);
        assertTrue(users.stream().allMatch(User::isAgreedToTermsOfUse));

        //reset agreements
        DataManager.getInstance().getDao().resetUserAgreementsToTermsOfUse();

        //now noone has agreed again
        users = DataManager.getInstance().getDao().getAllUsers(true);
        assertTrue(users.stream().allMatch(u -> !u.isAgreedToTermsOfUse()));
    }

    /**
     * @see JPADAO#createCampaignsFilterQuery(String,Map,Map)
     * @verifies create query correctly
     */
    @Test
    void createCampaignsFilterQuery_shouldCreateQueryCorrectly() {
        Map<String, String> filters = new HashMap<>(1);
        filters.put("groupOwner", "1");
        Map<String, Object> params = new HashMap<>(1);
        assertEquals(
                " prefix WHERE a.userGroup.owner IN (SELECT g.owner FROM UserGroup g WHERE g.owner.id=:groupOwner)",
                JPADAO.createCampaignsFilterQuery("prefix", filters, params));
        assertEquals(1, params.size());
        assertEquals(1L, params.get("groupOwner"));
    }

    /**
     * @see JPADAO#createAnnotationsFilterQuery(String,Map,Map)
     * @verifies create query correctly
     */
    @Test
    void createAnnotationsFilterQuery_shouldCreateQueryCorrectly() {
        {
            // creator/reviewer and campaign name
            Map<String, String> filters = new HashMap<>(2);
            filters.put("creatorId_reviewerId", "1");
            filters.put("campaign", "geo");
            Map<String, Object> params = new HashMap<>(2);
            assertEquals(
                    " prefix WHERE (a.creatorId=:creatorIdreviewerId OR a.reviewerId=:creatorIdreviewerId) AND (a.generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN"
                            + " (SELECT t.owner FROM CampaignTranslation t WHERE t.tag='title' AND UPPER(t.translationValue) LIKE :campaign)))",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
            assertEquals(2, params.size());
            assertEquals("%GEO%", params.get("campaign"));
            assertEquals(1L, params.get("creatorIdreviewerId"));
        }
        {
            // just creator/reviewer
            Map<String, String> filters = new HashMap<>(1);
            filters.put("creatorId_reviewerId", "1");
            Map<String, Object> params = new HashMap<>(1);
            assertEquals(" prefix WHERE (a.creatorId=:creatorIdreviewerId OR a.reviewerId=:creatorIdreviewerId)",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
        }
        {
            // just campaign name
            Map<String, String> filters = new HashMap<>(1);
            filters.put("campaign", "geo");
            Map<String, Object> params = new HashMap<>(1);
            assertEquals(
                    " prefix WHERE (a.generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN (SELECT t.owner FROM CampaignTranslation t WHERE t.tag='title' AND UPPER(t.translationValue) LIKE :campaign)))",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
        }
        {
            // just campaign ID
            Map<String, String> filters = new HashMap<>(2);
            filters.put("generatorId", "1");
            Map<String, Object> params = new HashMap<>(2);
            assertEquals(
                    " prefix WHERE (a.generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN (SELECT c FROM Campaign c WHERE c.id=:generatorId)))",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
            assertEquals(1, params.size());
            assertEquals(1L, params.get("generatorId"));
        }
        {
            // campaign ID and record identifier
            Map<String, String> filters = new HashMap<>(2);
            filters.put("generatorId", "1");
            filters.put("targetPI_body", "ppn123");
            Map<String, Object> params = new HashMap<>(2);
            assertEquals(
                    " prefix WHERE (a.generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN (SELECT c FROM Campaign c WHERE c.id=:generatorId)))"
                            + " AND (UPPER(a.targetPI) LIKE :targetPIbody OR UPPER(a.body) LIKE :targetPIbody)",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
            assertEquals(2, params.size());
            assertEquals(1L, params.get("generatorId"));
            assertEquals("%PPN123%", params.get("targetPIbody"));
        }

    }

    @Test
    void testGenerateNullMotivationFilterQuery() {
        {
            // campaign ID and record identifier
            Map<String, String> filters = new HashMap<>(2);
            filters.put("motivation", "NULL");
            Map<String, Object> params = new HashMap<>(2);
            assertEquals(
                    " prefix WHERE (a.motivation IS NULL)",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
            assertEquals(0, params.size());
        }
    }

    @Test
    void testGetAllRecordNotes() throws DAOException {
        List<CMSRecordNote> notes = DataManager.getInstance().getDao().getAllRecordNotes();
        assertEquals(5, notes.size());

    }

    @Test
    void testGetCMSRecordNotesPaginated() throws DAOException {
        List<CMSRecordNote> notesP1 = DataManager.getInstance().getDao().getRecordNotes(0, 2, null, false, null);
        assertEquals(2, notesP1.size());
        List<CMSRecordNote> notesP2 = DataManager.getInstance().getDao().getRecordNotes(4, 2, null, false, null);
        assertEquals(1, notesP2.size());
    }

    @Test
    void testGetCMSRecordNote() throws DAOException {
        CMSSingleRecordNote note = (CMSSingleRecordNote) DataManager.getInstance().getDao().getRecordNote(1l);
        assertNotNull(note);
        assertEquals("PI1", note.getRecordPi());
        assertEquals("Titel 1", note.getRecordTitle().getText());
        assertEquals("Notes 1", note.getNoteTitle().getText(Locale.ENGLISH));
        assertEquals("Bemerkungen 1", note.getNoteTitle().getText(Locale.GERMAN));
        assertEquals("<p>First paragraph</p>", note.getNoteText().getText(Locale.ENGLISH));
        assertEquals("<p>Erster Paragraph</p>", note.getNoteText().getText(Locale.GERMAN));
    }

    @Test
    void testAddCMSRecordNote() throws DAOException {

        CMSSingleRecordNote note = new CMSSingleRecordNote();
        note.getRecordTitle().setSelectedLocale(Locale.GERMAN);
        note.setRecordPi(pi);
        note.getRecordTitle().setText(title);

        assertTrue(DataManager.getInstance().getDao().addRecordNote(note));
        assertNotNull(note.getId());
        CMSSingleRecordNote pNote = (CMSSingleRecordNote) DataManager.getInstance().getDao().getRecordNote(note.getId());
        assertNotNull(pNote);
        pNote.getRecordTitle().setSelectedLocale(Locale.GERMAN);
        assertEquals(title, pNote.getRecordTitle().getText());
        assertEquals(title, pNote.getRecordTitle().getText(Locale.GERMAN));
    }

    @Test
    void testAddCMSMultiRecordNote() throws DAOException {

        CMSMultiRecordNote note = new CMSMultiRecordNote();
        note.setQuery("DC:dc3d");

        assertTrue(DataManager.getInstance().getDao().addRecordNote(note));
        assertNotNull(note.getId());
        CMSMultiRecordNote pNote = (CMSMultiRecordNote) DataManager.getInstance().getDao().getRecordNote(note.getId());
        assertNotNull(pNote);
        assertEquals("DC:dc3d", pNote.getQuery());
    }

    @Test
    void testUpdateRecordNote() throws DAOException {

        CMSRecordNote note = DataManager.getInstance().getDao().getRecordNote(2l);
        note.getNoteTitle().setText(changed, Locale.GERMAN);
        note.getNoteText().setText(changed, Locale.GERMAN);

        DataManager.getInstance().getDao().updateRecordNote(note);

        assertEquals(changed, note.getNoteTitle().getText(Locale.GERMAN));
        assertEquals("Notes 2", note.getNoteTitle().getText(Locale.ENGLISH));
        assertEquals(changed, note.getNoteText().getText(Locale.GERMAN));
        assertFalse(note.getNoteText().getValue(Locale.ENGLISH).isPresent());
    }

    @Test
    void testDeleteRecordNote() throws DAOException {
        assertEquals(5, DataManager.getInstance().getDao().getAllRecordNotes().size());
        CMSRecordNote note = DataManager.getInstance().getDao().getRecordNote(2l);
        DataManager.getInstance().getDao().deleteRecordNote(note);
        assertEquals(4, DataManager.getInstance().getDao().getAllRecordNotes().size());
        assertNull(DataManager.getInstance().getDao().getRecordNote(2l));

    }

    @Test
    void testGetRecordNotesForPi() throws DAOException {
        assertEquals(2, DataManager.getInstance().getDao().getRecordNotesForPi("PI1", false).size());
        assertEquals(1, DataManager.getInstance().getDao().getRecordNotesForPi("PI1", true).size());
        assertEquals(0, DataManager.getInstance().getDao().getRecordNotesForPi("PI5", false).size());
    }

    @Test
    void testGetAllMultiRecordNotes() throws DAOException {
        assertEquals(1, DataManager.getInstance().getDao().getAllMultiRecordNotes(true).size());
        assertEquals(2, DataManager.getInstance().getDao().getAllMultiRecordNotes(false).size());
    }

    @Test
    void testGetAllSliders() throws DAOException {
        List<CMSSlider> sliders = DataManager.getInstance().getDao().getAllSliders();
        assertEquals(3, sliders.size());
        assertTrue(sliders.stream().anyMatch(sl -> sl.getName().equals("Query Slider")));
        assertTrue(sliders.stream().anyMatch(sl -> sl.getDescription().equals("Slider from collections")));
    }

    @Test
    void testGetSlider() throws DAOException {
        CMSSlider slider = DataManager.getInstance().getDao().getSlider(1l);
        assertNotNull(slider);
    }

    @Test
    void testAddSlider() throws DAOException {
        String name = "Test Slider";
        CMSSlider slider = new CMSSlider(SourceType.COLLECTIONS);
        slider.setName(name);
        assertTrue(DataManager.getInstance().getDao().addSlider(slider));

        assertNotNull(slider.getId());
        CMSSlider loadedSlider = DataManager.getInstance().getDao().getSlider(slider.getId());
        assertNotNull(loadedSlider);
        assertNotSame(loadedSlider, slider);
        assertEquals(loadedSlider.getId(), slider.getId());
        assertEquals(name, loadedSlider.getName());
    }

    @Test
    void testUpdateSlider() throws DAOException {
        String name = "Test Slider";
        CMSSlider slider = DataManager.getInstance().getDao().getSlider(1l);
        assertNotEquals(name, slider.getName());

        slider.setName(name);
        assertNotEquals(name, DataManager.getInstance().getDao().getSlider(1l).getName());

        DataManager.getInstance().getDao().updateSlider(slider);
        assertEquals(name, DataManager.getInstance().getDao().getSlider(1l).getName());
    }

    @Test
    void testDeleteSlider() throws DAOException {
        String name = "Test Slider";
        CMSSlider slider = new CMSSlider(SourceType.COLLECTIONS);
        slider.setName(name);
        assertTrue(DataManager.getInstance().getDao().addSlider(slider));
        assertNotNull(DataManager.getInstance().getDao().getSlider(slider.getId()));

        assertTrue(DataManager.getInstance().getDao().deleteSlider(slider));
        assertNull(DataManager.getInstance().getDao().getSlider(slider.getId()));

    }

    @Test
    void testGetEmbeddingCmsPage() throws DAOException {
        CMSSlider slider = DataManager.getInstance().getDao().getSlider(1l);
        List<CMSPage> pages = DataManager.getInstance().getDao().getPagesUsingSlider(slider);
        assertEquals(1, pages.size());
        assertEquals(Long.valueOf(3l), pages.iterator().next().getId());
    }

    /**
     * @see JPADAO#getCommentsOfUser(User,int,String,boolean)
     * @verifies sort correctly
     */
    @Test
    void getCommentsOfUser_shouldSortCorrectly() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        assertNotNull(user);
        {
            List<Comment> comments = DataManager.getInstance().getDao().getCommentsOfUser(user, 2, "dateCreated", true);
            assertEquals(2, comments.size());
            assertEquals(Long.valueOf(3), comments.get(0).getId());
            assertEquals(Long.valueOf(4), comments.get(1).getId());
        }
        {
            List<Comment> comments = DataManager.getInstance().getDao().getCommentsOfUser(user, 2, "dateModified", true);
            assertEquals(2, comments.size());
            assertEquals(Long.valueOf(4), comments.get(0).getId());
        }
    }

    @Test
    void testSynchronization() throws DAOException {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        IDAO dao = DataManager.getInstance().getDao();

        Comment comment = new Comment();
        comment.setBody("Init");
        dao.addComment(comment);
        assertNotNull(comment.getId());
        assertEquals("Init", dao.getComment(comment.getId()).getBody());

        FutureTask<Boolean> updateResult = new FutureTask<Boolean>(() -> {
            try {
                updateComment(dao, comment.getId(), "Changed", 200);
            } catch (InterruptedException | DAOException e) {
                fail("Updating interrupted");
            }
        }, true);

        FutureTask<Boolean> updateResultFast = new FutureTask<Boolean>(() -> {
            try {
                updateComment(dao, comment.getId(), "ChangedAgain", 0);
            } catch (InterruptedException | DAOException e) {
                fail("Updating interrupted");
            }
        }, true);

        try {
            executor.execute(updateResult);
            assertEquals("Init", dao.getComment(comment.getId()).getBody());
            //            executor.execute(updateResultFast);
            //            updateResultFast.get(100, TimeUnit.MILLISECONDS);
            //            assertEquals("ChangedAgain", dao.getComment(comment.getId()).getBody());
            updateResult.get(300, TimeUnit.MILLISECONDS);
            assertEquals("Changed", dao.getComment(comment.getId()).getBody());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            fail("Updating or retrieving interrupted");
        }

    }

    private static void updateComment(IDAO dao, long id, String content, long duration) throws InterruptedException, DAOException {
        Thread.sleep(duration);
        dao.executeUpdate("UPDATE annotations_comments SET body='" + content + "' WHERE annotation_id=" + id);
    }

    /**
     * @see JPADAO#getDownloadTicketCount(Map)
     * @verifies return correct count
     */
    @Test
    void getDownloadTicketCount_shouldReturnCorrectCount() throws Exception {
        assertEquals(2, DataManager.getInstance().getDao().getActiveDownloadTicketCount(Collections.emptyMap()));
        assertEquals(2,
                DataManager.getInstance().getDao().getActiveDownloadTicketCount(Collections.singletonMap("pi_email", "user1@example.com")));
        assertEquals(1, DataManager.getInstance().getDao().getActiveDownloadTicketCount(Collections.singletonMap("pi_email", "PPN456")));
    }

    /**
     * @see JPADAO#getActiveDownloadTickets(int,int,String,boolean,Map)
     * @verifies filter rows correctly
     */
    @Test
    void getActiveDownloadTickets_shouldFilterRowsCorrectly() throws Exception {
        assertEquals(2, DataManager.getInstance().getDao().getActiveDownloadTickets(0, 10, null, false, null).size());
        assertEquals(0,
                DataManager.getInstance()
                        .getDao()
                        .getActiveDownloadTickets(0, 10, null, false, Collections.singletonMap("pi_email", "user2@example.com"))
                        .size());
        assertEquals(1,
                DataManager.getInstance()
                        .getDao()
                        .getActiveDownloadTickets(0, 10, null, false, Collections.singletonMap("pi_email", "PPN456"))
                        .size());
    }

    @Test
    void test_deleteViewerMessagesBeforeDate() throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();

        ViewerMessage task1 = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        task1.setMessageId("ID:florian-test:0.0.0");
        task1.setMessageStatus(MessageStatus.FINISH);
        task1.getProperties().put("pi", "PPN12345");
        task1.getProperties().put("divId", "LOG_0001");
        task1.setLastUpdateTime(LocalDateTime.of(2022, 12, 20, 23, 30));

        ViewerMessage task2 = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        task2.setMessageId("ID:florian-test:1.0.0");
        task2.setMessageStatus(MessageStatus.FINISH);
        task2.getProperties().put("pi", "PPN12345");
        task2.getProperties().put("divId", "LOG_0003");
        task2.setLastUpdateTime(LocalDateTime.of(2023, 1, 3, 11, 30));

        ViewerMessage task3 = new ViewerMessage(TaskType.UPDATE_DATA_REPOSITORY_NAMES.name());
        task3.setMessageId("ID:florian-test:2.0.0");
        task3.setMessageStatus(MessageStatus.ERROR);
        task3.getProperties().put("pi", "PPN67890");
        task3.setLastUpdateTime(LocalDateTime.of(2023, 1, 3, 10, 30));

        ViewerMessage task4 = new ViewerMessage(TaskType.UPDATE_DATA_REPOSITORY_NAMES.name());
        task4.setMessageId("ID:florian-test:3.0.0");
        task4.setMessageStatus(MessageStatus.FINISH);
        task4.getProperties().put("pi", "PPN2244");
        task4.setLastUpdateTime(LocalDateTime.of(2023, 2, 1, 11, 30));

        dao.addViewerMessage(task1);
        dao.addViewerMessage(task2);
        dao.addViewerMessage(task3);
        dao.addViewerMessage(task4);

        assertEquals(4, dao.getViewerMessageCount(Collections.emptyMap()));
        int deleted = dao.deleteViewerMessagesBefore(LocalDateTime.of(2023, 1, 3, 11, 00));
        assertEquals(2, deleted);
        assertEquals(null, dao.getViewerMessageByMessageID("ID:florian-test:0.0.0"));
        assertEquals(2, dao.getViewerMessageCount(Collections.emptyMap()));
    }

    /**
     * @see JPADAO#getDownloadTicketRequests()
     * @verifies return tickets that have never been activated
     */
    @Test
    void getDownloadTicketRequests_shouldReturnTicketsThatHaveNeverBeenActivated() throws Exception {
        List<DownloadTicket> result = DataManager.getInstance().getDao().getDownloadTicketRequests();
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(3), result.get(0).getId());
    }

    @Test
    void test_persistRecurringTaskTrigger() throws Exception {
        IDAO dao = DataManager.getInstance().getDao();

        RecurringTaskTrigger trigger = new RecurringTaskTrigger(TaskType.DOWNLOAD_PDF, "0 0 0 * * ?");

        dao.addRecurringTaskTrigger(trigger);
        assertEquals(1, dao.getRecurringTaskTriggers().size());
        RecurringTaskTrigger loaded = dao.getRecurringTaskTrigger(trigger.getId());
        assertNotNull(loaded);
        assertEquals(TaskType.DOWNLOAD_PDF.name(), loaded.getTaskType());
        assertEquals("0 0 0 * * ?", loaded.getScheduleExpression());

        LocalDateTime triggered = LocalDateTime.now();
        loaded.setLastTimeTriggered(triggered);
        dao.updateRecurringTaskTrigger(loaded);

        RecurringTaskTrigger updated = dao.getRecurringTaskTriggerForTask(TaskType.DOWNLOAD_PDF);
        assertNotNull(updated);
        assertEquals(TaskType.DOWNLOAD_PDF.name(), updated.getTaskType());
        assertEquals("0 0 0 * * ?", updated.getScheduleExpression());
        // Compare date strings instead of LocalDateTime due to differences in milisecond precision between JVMs
        assertEquals(DateTools.FORMATTERISO8601DATETIMEMS.format(triggered),
                DateTools.FORMATTERISO8601DATETIMEMS.format(updated.getLastTimeTriggered()));

        dao.deleteRecurringTaskTrigger(trigger.getId());
        assertTrue(dao.getRecurringTaskTriggers().isEmpty());

    }

    /**
     * @see JPADAO#getMaintenanceMode()
     * @verifies return correct object
     */
    @Test
    void getMaintenanceMode_shouldReturnCorrectObject() throws Exception {
        MaintenanceMode mm = DataManager.getInstance().getDao().getMaintenanceMode();
        assertNotNull(mm);
        assertEquals(1, mm.getId());
        assertTrue(mm.isEnabled());
        assertEquals(2, mm.getTranslations().size());
        assertEquals("Maintenance mode EN", mm.getText("en"));
        assertEquals("Maintenance mode DE", mm.getText("de"));
    }
    
    /**
     * @see JPADAO#updateMaintenanceMode(MaintenanceMode)
     * @verifies update object correctly
     */
    @Test
    void updateMaintenanceMode_shouldUpdateObjectCorrectly() throws Exception {
        MaintenanceMode mm = DataManager.getInstance().getDao().getMaintenanceMode();
        assertNotNull(mm);
        mm.setEnabled(false);
        mm.setText("Maintenance mode EN NEW", "en");
        mm.setText("Maintenance mode JP", "jp");
        DataManager.getInstance().getDao().updateMaintenanceMode(mm);
        
        MaintenanceMode mm2 = DataManager.getInstance().getDao().getMaintenanceMode();
        assertEquals(mm, mm2);
        assertFalse(mm2.isEnabled());
        assertEquals(3, mm2.getTranslations().size());
        assertEquals("Maintenance mode EN NEW", mm.getText("en"));
    }
}
