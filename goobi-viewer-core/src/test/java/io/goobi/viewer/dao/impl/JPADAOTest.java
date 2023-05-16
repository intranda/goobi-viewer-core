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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
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
public class JPADAOTest extends AbstractDatabaseEnabledTest {

    public static final int NUM_LICENSE_TYPES = 6;

    String pi = "PI_TEST";
    String title = "TITLE_TEST";
    String germanNote = "Bemerkung";
    String englishNote = "Note";
    String changed = "CHANGED";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        File webContent = new File("WebContent/").getAbsoluteFile();
        String webContentPath = webContent.toURI().toString();
    }

    // Users

    @Test
    public void getAllUsersTest() throws DAOException {
        List<User> users = DataManager.getInstance().getDao().getAllUsers(false);
        Assert.assertEquals(3, users.size());
    }

    @Test
    public void getUserByIdTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        Assert.assertEquals(2, user.getOpenIdAccounts().size());
        Assert.assertEquals(LocalDateTime.of(2012, 3, 3, 11, 22, 33), user.getLastLogin());
    }

    @Test
    public void getUserByEmailTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByEmail("1@UsErS.oRg");
        Assert.assertNotNull(user);
    }

    @Test
    public void getUserByOpenIdTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByOpenId("user_1_claimed_identifier_2");
        Assert.assertNotNull(user);
    }

    @Test
    public void getUserByNicknameTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByNickname("admin");
        Assert.assertNotNull(user);
    }

    @Test
    public void addUserTest() throws DAOException {
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
        Assert.assertNotNull(user2);
        Assert.assertEquals(user.getPasswordHash(), user2.getPasswordHash());
        Assert.assertEquals(user.getActivationKey(), user2.getActivationKey());
        Assert.assertEquals(user.getFirstName(), user2.getFirstName());
        Assert.assertEquals(user.getLastName(), user2.getLastName());
        Assert.assertEquals(user.getNickName(), user2.getNickName());
        Assert.assertEquals(user.getComments(), user2.getComments());
        Assert.assertEquals(user.getAvatarType(), user2.getAvatarType());
        Assert.assertEquals(user.getLastLogin(), now);
        Assert.assertEquals(user.isActive(), user2.isActive());
        Assert.assertEquals(user.isSuspended(), user2.isSuspended());
        Assert.assertEquals(user.isSuperuser(), user2.isSuperuser());
    }

    @Test
    public void updateUserTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUsers(false).size());
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
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

        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUsers(false).size());

        User user2 = DataManager.getInstance().getDao().getUser(user.getId());
        Assert.assertNotNull(user2);
        Assert.assertEquals(user.getId(), user2.getId());
        Assert.assertEquals(user.getEmail(), user2.getEmail());
        Assert.assertEquals(user.getFirstName(), user2.getFirstName());
        Assert.assertEquals(user.getLastName(), user2.getLastName());
        Assert.assertEquals(user.getNickName(), user2.getNickName());
        Assert.assertEquals(user.getComments(), user2.getComments());
        Assert.assertEquals(user.getAvatarType(), user2.getAvatarType());
        Assert.assertEquals(user.getLastLogin(), user2.getLastLogin());
        Assert.assertEquals(user.isActive(), user2.isActive());
        Assert.assertEquals(user.isSuspended(), user2.isSuspended());
        Assert.assertEquals(user.isSuperuser(), user2.isSuperuser());
        Assert.assertEquals(user.isAgreedToTermsOfUse(), user2.isAgreedToTermsOfUse());
    }

    @Test
    public void deleteUserTest() throws DAOException {
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

        Assert.assertNotNull(DataManager.getInstance().getDao().getUser(user.getId()));
        Assert.assertTrue(DataManager.getInstance().getDao().deleteUser(user));
        Assert.assertNull(DataManager.getInstance().getDao().getUserByEmail("deleteme@b.com"));
    }

    @Test
    public void userLicenseTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        Assert.assertEquals(1, user.getLicenses().size());
        Assert.assertEquals(1, user.getLicenses().get(0).getPrivileges().size());
        for (String priv : user.getLicenses().get(0).getPrivileges()) {
            Assert.assertEquals(IPrivilegeHolder.PRIV_LIST, priv);
            break;
        }

        // Saving the licensee should not create any extra licenses
        {
            Assert.assertTrue(DataManager.getInstance().getDao().updateUser(user));
            User user2 = DataManager.getInstance().getDao().getUser(user.getId());
            Assert.assertNotNull(user2);
            Assert.assertEquals(1, user2.getLicenses().size());
        }

        // Adding a new license should update the attached object with ID
        {
            License license = new License();
            license.setLicenseType(user.getLicenses().get(0).getLicenseType());
            user.addLicense(license);
            Assert.assertTrue(DataManager.getInstance().getDao().updateUser(user));
            User user2 = DataManager.getInstance().getDao().getUser(user.getId());
            Assert.assertNotNull(user2);
            Assert.assertEquals(2, user2.getLicenses().size());
            Assert.assertNotNull(user2.getLicenses().get(0).getId());
            Assert.assertNotNull(user2.getLicenses().get(1).getId());
        }

        // Orphaned licenses should be deleted properly
        user.removeLicense(user.getLicenses().get(0));
        Assert.assertTrue(DataManager.getInstance().getDao().updateUser(user));
        User user3 = DataManager.getInstance().getDao().getUser(user.getId());
        Assert.assertNotNull(user3);
        Assert.assertEquals(1, user3.getLicenses().size());

    }

    // User groups

    @Test
    public void getAllUserGroupsTest() throws DAOException {
        List<UserGroup> userGroups = DataManager.getInstance().getDao().getAllUserGroups();
        Assert.assertEquals(3, userGroups.size());
    }

    @Test
    public void getAllUserGroupsForOwnerTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        Assert.assertEquals(3, DataManager.getInstance().getDao().getUserGroups(user).size());
    }

    @Test
    public void getUserGroupByIdTest() throws DAOException {
        UserGroup ug = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(ug);
        Assert.assertNotNull(ug.getOwner());
        Assert.assertEquals(Long.valueOf(1), ug.getOwner().getId());
        Assert.assertEquals("user group 1 name", ug.getName());
        Assert.assertEquals("user group 1 desc", ug.getDescription());
        Assert.assertTrue(ug.isActive());
    }

    @Test
    public void getUserGroupByNameTest() throws DAOException {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup("user group 1 name");
        Assert.assertNotNull(userGroup);
        Assert.assertNotNull(userGroup.getOwner());
        Assert.assertEquals(Long.valueOf(1), userGroup.getOwner().getId());
        Assert.assertEquals("user group 1 name", userGroup.getName());
        Assert.assertEquals("user group 1 desc", userGroup.getDescription());
        Assert.assertTrue(userGroup.isActive());
    }

    @Test
    public void addUserGroupTest() throws DAOException {
        User owner = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(owner);

        UserGroup userGroup = new UserGroup();
        userGroup.setOwner(owner);
        userGroup.setName("added user group name");
        userGroup.setDescription("added user group desc");
        userGroup.setActive(false);

        Assert.assertTrue(DataManager.getInstance().getDao().addUserGroup(userGroup));
        Assert.assertNotNull(userGroup.getId());

        UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
        Assert.assertNotNull(userGroup2);
        Assert.assertNotNull(userGroup2.getOwner());
        Assert.assertEquals(userGroup.getOwner().getId(), userGroup2.getOwner().getId());
        Assert.assertEquals(userGroup.getName(), userGroup2.getName());
        Assert.assertEquals(userGroup.getDescription(), userGroup2.getDescription());
        Assert.assertEquals(userGroup.isActive(), userGroup2.isActive());
    }

    @Test
    public void updateUserGroupTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        Assert.assertEquals("user group 1 name", userGroup.getName());

        userGroup.setName("user group 1 new name");
        Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());

        UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
        Assert.assertNotNull(userGroup2);
        Assert.assertEquals(userGroup.getId(), userGroup2.getId());
        Assert.assertEquals("user group 1 new name", userGroup2.getName());

    }

    @Test
    public void deleteUserGroupWithMembersTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        Assert.assertFalse(DataManager.getInstance().getDao().deleteUserGroup(userGroup));
        Assert.assertNotNull(DataManager.getInstance().getDao().getUserGroup(2));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());
    }

    @Test
    public void deleteUserGroupWithoutMembersTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(2);
        Assert.assertNotNull(userGroup);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteUserGroup(userGroup));
        Assert.assertNull(DataManager.getInstance().getDao().getUserGroup(2));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUserGroups().size());
    }

    // UserRoles (group memberships)

    @Test
    public void getAllUserRolesTest() throws DAOException {
        List<UserRole> userRoles = DataManager.getInstance().getDao().getAllUserRoles();
        Assert.assertEquals(3, userRoles.size());
    }

    /**
     * @see JPADAO#getUserRoleCount(UserGroup,User,Role)
     * @verifies return correct count
     */
    @Test
    public void getUserRoleCount_shouldReturnCorrectCount() throws Exception {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        Assert.assertEquals(1, DataManager.getInstance().getDao().getUserRoleCount(userGroup, null, null));

        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        Assert.assertEquals(1, DataManager.getInstance().getDao().getUserRoleCount(userGroup, user, null));

        Role role = DataManager.getInstance().getDao().getRole(1);
        Assert.assertNotNull(role);
        Assert.assertEquals(1, DataManager.getInstance().getDao().getUserRoleCount(userGroup, user, role));
    }

    @Test
    public void getUserGroupMembershipsByUserGroupTest() throws DAOException {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        List<UserRole> memberships = DataManager.getInstance().getDao().getUserRoles(userGroup, null, null);
        Assert.assertNotNull(memberships);
        Assert.assertEquals(1, memberships.size());
    }

    @Test
    public void getUserGroupMembershipsByUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        List<UserRole> memberships = DataManager.getInstance().getDao().getUserRoles(null, user, null);
        Assert.assertNotNull(memberships);
        Assert.assertEquals(1, memberships.size());
    }

    @Test
    public void getUserGroupMembershipsByRoleTest() throws DAOException {
        Role role = DataManager.getInstance().getDao().getRole(1);
        Assert.assertNotNull(role);
        List<UserRole> memberships = DataManager.getInstance().getDao().getUserRoles(null, null, role);
        Assert.assertNotNull(memberships);
        Assert.assertEquals(3, memberships.size());
    }

    @Test
    public void userGroupLicenseTest() throws DAOException {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        Assert.assertEquals(2, userGroup.getLicenses().size());
        Assert.assertEquals(1, userGroup.getLicenses().get(0).getPrivileges().size());
        for (String priv : userGroup.getLicenses().get(0).getPrivileges()) {
            Assert.assertEquals("license 2 priv 1", priv);
            break;
        }

        // Saving the licensee should not create any extra licenses
        {
            Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
            UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
            Assert.assertNotNull(userGroup2);
            Assert.assertEquals(2, userGroup2.getLicenses().size());
        }

        // Adding a new license should update the attached object with ID
        {
            License license = new License();
            license.setLicenseType(userGroup.getLicenses().get(0).getLicenseType());
            userGroup.addLicense(license);
            Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
            UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
            Assert.assertNotNull(userGroup2);
            Assert.assertEquals(3, userGroup2.getLicenses().size());
            Assert.assertNotNull(userGroup2.getLicenses().get(0).getId());
            Assert.assertNotNull(userGroup2.getLicenses().get(1).getId());
        }

        // Orphaned licenses should be deleted properly
        {
            userGroup.removeLicense(userGroup.getLicenses().get(0));
            Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
            UserGroup userGroup3 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
            Assert.assertNotNull(userGroup3);
            Assert.assertEquals(2, userGroup3.getLicenses().size());
        }
    }

    @Test
    public void addUserRoleTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(2);
        Assert.assertNotNull(userGroup);
        Role role = DataManager.getInstance().getDao().getRole(2);
        Assert.assertNotNull(role);

        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserRoles().size());
        Assert.assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, user, role).size());
        Assert.assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, user, null).size());
        Assert.assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, null, null).size());

        UserRole userRole = new UserRole(userGroup, user, role);
        Assert.assertTrue(DataManager.getInstance().getDao().addUserRole(userRole));
        Assert.assertNotNull(userRole.getId());

        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllUserRoles().size());
        Assert.assertEquals(1, DataManager.getInstance().getDao().getUserRoles(userGroup, user, role).size());
        Assert.assertEquals(1, DataManager.getInstance().getDao().getUserRoles(userGroup, user, null).size());
        Assert.assertEquals(1, DataManager.getInstance().getDao().getUserRoles(userGroup, null, null).size());

        UserRole userRole2 = DataManager.getInstance().getDao().getUserRoles(userGroup, user, role).get(0);
        Assert.assertNotNull(userRole2);
        Assert.assertEquals(userRole.getUserGroup(), userRole2.getUserGroup());
        Assert.assertEquals(userRole.getUser(), userRole2.getUser());
        Assert.assertEquals(userRole.getRole(), userRole2.getRole());
    }

    @Test
    public void updateUserRoleTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserRoles().size());
        UserRole userRole = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        Assert.assertNotNull(userRole);

        Role role1 = DataManager.getInstance().getDao().getRole(1);
        Assert.assertNotNull(role1);
        Role role2 = DataManager.getInstance().getDao().getRole(2);
        Assert.assertNotNull(role2);
        Assert.assertEquals(role1, userRole.getRole());
        userRole.setRole(role2);
        Assert.assertTrue(DataManager.getInstance().getDao().updateUserRole(userRole));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserRoles().size());

        UserRole userRole2 = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        Assert.assertNotNull(userRole2);
        Assert.assertEquals(userRole.getRole(), userRole2.getRole());
    }

    @Test
    public void deleteUserRoleTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllUserRoles().size());
        UserRole userRole = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteUserRole(userRole));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUserRoles().size());
    }

    // IP ranges

    @Test
    public void getAllIpRangesTest() throws DAOException {
        List<IpRange> ipRanges = DataManager.getInstance().getDao().getAllIpRanges();
        Assert.assertEquals(2, ipRanges.size());
    }

    @Test
    public void getIpRangeByIdTest() throws DAOException {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        Assert.assertNotNull(ipRange);
        Assert.assertEquals(Long.valueOf(1), ipRange.getId());
        Assert.assertEquals("localhost", ipRange.getName());
        Assert.assertEquals("1.2.3.4/24", ipRange.getSubnetMask());
        Assert.assertEquals("ip range 1 desc", ipRange.getDescription());
    }

    @Test
    public void getIpRangeByNameTest() throws DAOException {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange("localhost");
        Assert.assertNotNull(ipRange);
        Assert.assertEquals(Long.valueOf(1), ipRange.getId());
        Assert.assertEquals("localhost", ipRange.getName());
        Assert.assertEquals("1.2.3.4/24", ipRange.getSubnetMask());
        Assert.assertEquals("ip range 1 desc", ipRange.getDescription());
    }

    @Test
    public void addIpRangeTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllIpRanges().size());
        IpRange ipRange = new IpRange();
        ipRange.setName("ip range to add name");
        ipRange.setDescription("ip range to add desc");
        ipRange.setSubnetMask("0.0.0.0./0");
        Assert.assertTrue(DataManager.getInstance().getDao().addIpRange(ipRange));
        Assert.assertNotNull(ipRange.getId());
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllIpRanges().size());

        IpRange ipRange2 = DataManager.getInstance().getDao().getIpRange(ipRange.getId());
        Assert.assertNotNull(ipRange2);
        Assert.assertEquals(ipRange.getName(), ipRange2.getName());
        Assert.assertEquals(ipRange.getDescription(), ipRange2.getDescription());
        Assert.assertEquals(ipRange.getSubnetMask(), ipRange2.getSubnetMask());
    }

    @Test
    public void updateIpRangeTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllIpRanges().size());
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        Assert.assertNotNull(ipRange);

        ipRange.setName("ip range 1 new name");
        ipRange.setDescription("ip range 1 new desc");
        ipRange.setSubnetMask("0.0.0.0./0");

        Assert.assertTrue(DataManager.getInstance().getDao().updateIpRange(ipRange));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllIpRanges().size());

        IpRange ipRange2 = DataManager.getInstance().getDao().getIpRange(ipRange.getId());
        Assert.assertNotNull(ipRange2);
        Assert.assertEquals(ipRange.getId(), ipRange2.getId());
        Assert.assertEquals(ipRange.getName(), ipRange.getName());
        Assert.assertEquals(ipRange.getDescription(), ipRange2.getDescription());
        Assert.assertEquals(ipRange.getSubnetMask(), ipRange2.getSubnetMask());
    }

    @Test
    public void deleteIpRangeTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllIpRanges().size());
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        Assert.assertNotNull(ipRange);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteIpRange(ipRange));
        Assert.assertNull(DataManager.getInstance().getDao().getIpRange(1));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllIpRanges().size());
    }

    @Test
    public void ipRangeLicenseTest() throws DAOException {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        Assert.assertNotNull(ipRange);
        Assert.assertEquals(1, ipRange.getLicenses().size());
        Assert.assertEquals(1, ipRange.getLicenses().get(0).getPrivileges().size());
        for (String priv : ipRange.getLicenses().get(0).getPrivileges()) {
            Assert.assertEquals(IPrivilegeHolder.PRIV_LIST, priv);
            break;
        }

        // Saving the licensee should not create any extra licenses
        Assert.assertTrue(DataManager.getInstance().getDao().updateIpRange(ipRange));
        IpRange ipRange2 = DataManager.getInstance().getDao().getIpRange(ipRange.getId());
        Assert.assertNotNull(ipRange2);
        Assert.assertEquals(1, ipRange2.getLicenses().size());

        // Orphaned licenses should be deleted properly
        ipRange.removeLicense(ipRange.getLicenses().get(0));
        Assert.assertTrue(DataManager.getInstance().getDao().updateIpRange(ipRange));
        IpRange ipRange3 = DataManager.getInstance().getDao().getIpRange(ipRange.getId());
        Assert.assertNotNull(ipRange3);
        Assert.assertEquals(0, ipRange3.getLicenses().size());
    }

    // CommentGroups

    /**
     * @see JPADAO#getAllCommentGroups()
     * @verifies return all rows
     */
    @Test
    public void getAllCommentGroups_shouldReturnAllRows() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllCommentGroups().size());
    }

    /**
     * @see JPADAO#getCommentGroupUnfiltered()
     * @verifies return correct row
     */
    @Test
    public void getCommentGroupUnfiltered_shouldReturnCorrectRow() throws Exception {
        CommentGroup commentGroup = DataManager.getInstance().getDao().getCommentGroupUnfiltered();
        Assert.assertNotNull(commentGroup);
        Assert.assertEquals(Long.valueOf(1), commentGroup.getId());
    }

    // Comments

    @Test
    public void getAllCommentsTest() throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getAllComments();
        Assert.assertEquals(4, comments.size());
    }

    /**
     * @see JPADAO#getCommentCount(Map,User,Set)
     * @verifies return correct count
     */
    @Test
    public void getCommentCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(4L, DataManager.getInstance().getDao().getCommentCount(null, null, null));
    }

    /**
     * @see JPADAO#getCommentCount(Map,User,Set)
     * @verifies filter correctly
     */
    @Test
    public void getCommentCount_shouldFilterCorrectly() throws Exception {
        Map<String, String> filters = new HashMap<>();
        filters.put("targetPageOrder", "1");
        Assert.assertEquals(3L, DataManager.getInstance().getDao().getCommentCount(filters, null, null));
    }

    /**
     * @see JPADAO#getCommentCount(Map,User,Set)
     * @verifies filter for users correctly
     */
    @Test
    public void getCommentCount_shouldFilterForUsersCorrectly() throws Exception {
        {
            User owner = DataManager.getInstance().getDao().getUser(1l);
            Assert.assertEquals(3L, DataManager.getInstance().getDao().getCommentCount(null, owner, null));
        }
        {
            User owner = DataManager.getInstance().getDao().getUser(2l);
            Assert.assertEquals(1L, DataManager.getInstance().getDao().getCommentCount(null, owner, null));
        }
    }

    /**
     * @see JPADAO#getCommentCount(Map,User,Set)
     * @verifies apply target pi filter correctly
     */
    @Test
    public void getCommentCount_shouldApplyTargetPiFilterCorrectly() throws Exception {
        // TODO update test dataset to include comments with different target_pi
        Assert.assertEquals(4L, DataManager.getInstance().getDao().getCommentCount(null, null, Collections.singleton("PI_1")));
    }

    @Test
    public void getCommentByIdTest() throws DAOException {
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        Assert.assertNotNull(comment);
        Assert.assertEquals(Long.valueOf(1), comment.getId());
        Assert.assertEquals("PI_1", comment.getTargetPI());
        Assert.assertEquals(Integer.valueOf(1), comment.getTargetPageOrder());
        Assert.assertNotNull(comment.getCreator());
        Assert.assertEquals(Long.valueOf(1), comment.getCreator().getId());
        Assert.assertEquals("comment 1 text", comment.getText());
        Assert.assertNotNull(comment.getDateCreated());
        Assert.assertNull(comment.getDateModified());
        //        Assert.assertNull(comment.getParent());
        //        Assert.assertEquals(1, comment.getChildren().size());

        Comment comment2 = DataManager.getInstance().getDao().getComment(2);
        //        Assert.assertEquals(comment, comment2.getParent());
        //        Assert.assertEquals(1, comment2.getChildren().size());
    }

    @Test
    public void getCommentsForPageTest() throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage("PI_1", 1);
        Assert.assertEquals(3, comments.size());
    }

    @Test
    public void addCommentTest() throws DAOException {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = new Comment();
        comment.setTargetPI("PI_2");
        comment.setTargetPageOrder(1);
        comment.setText("new comment text");
        comment.setCreator(DataManager.getInstance().getDao().getUser(1));
        Assert.assertTrue(DataManager.getInstance().getDao().addComment(comment));
        Assert.assertNotNull(comment.getId());
        Assert.assertEquals(5, DataManager.getInstance().getDao().getAllComments().size());

        Comment comment2 = DataManager.getInstance().getDao().getComment(comment.getId());
        Assert.assertNotNull(comment2);
        Assert.assertEquals(comment.getTargetPI(), comment2.getTargetPI());
        Assert.assertEquals(comment.getTargetPageOrder(), comment2.getTargetPageOrder());
        Assert.assertEquals(comment.getText(), comment2.getText());
        Assert.assertEquals(comment.getCreator(), comment2.getCreator());
        Assert.assertNotNull(comment2.getDateCreated());
        Assert.assertNull(comment2.getDateModified());
    }

    @Test
    public void updateCommentTest() throws DAOException {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        Assert.assertNotNull(comment);

        comment.setText("new comment 1 text");
        LocalDateTime now = LocalDateTime.now();
        comment.setDateModified(now);

        Assert.assertTrue(DataManager.getInstance().getDao().updateComment(comment));
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());

        Comment comment2 = DataManager.getInstance().getDao().getComment(comment.getId());
        Assert.assertNotNull(comment2);
        Assert.assertEquals(comment.getTargetPI(), comment2.getTargetPI());
        Assert.assertEquals(comment.getTargetPageOrder(), comment2.getTargetPageOrder());
        Assert.assertEquals(comment.getText(), comment2.getText());
        Assert.assertEquals(now, comment2.getDateModified());
        Assert.assertEquals(comment.getCreator(), comment2.getCreator());
    }

    @Test
    public void deleteCommentTest() throws DAOException {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        Assert.assertNotNull(comment);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteComment(comment));
        Assert.assertNull(DataManager.getInstance().getDao().getComment(1));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#deleteComments(String,User)
     * @verifies delete comments for pi correctly
     */
    @Test
    public void deleteComments_shouldDeleteCommentsForPiCorrectly() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        Assert.assertEquals(4, DataManager.getInstance().getDao().deleteComments("PI_1", null));
        Assert.assertEquals(0, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#deleteComments(String,User)
     * @verifies delete comments for user correctly
     */
    @Test
    public void deleteComments_shouldDeleteCommentsForUserCorrectly() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        Assert.assertEquals(3, DataManager.getInstance().getDao().deleteComments(null, user));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#deleteComments(String,User)
     * @verifies delete comments for pi and user correctly
     */
    @Test
    public void deleteComments_shouldDeleteCommentsForPiAndUserCorrectly() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        Assert.assertEquals(3, DataManager.getInstance().getDao().deleteComments("PI_1", user));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#deleteComments(String,User)
     * @verifies not delete anything if both pi and creator are null
     */
    @Test
    public void deleteComments_shouldNotDeleteAnythingIfBothPiAndCreatorAreNull() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Assert.assertEquals(0, DataManager.getInstance().getDao().deleteComments(null, null));
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
    }

    /**
     * @see JPADAO#changeCommentsOwner(User,User)
     * @verifies update rows correctly
     */
    @Test
    public void changeCommentsOwner_shouldUpdateRowsCorrectly() throws Exception {
        User oldOwner = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(oldOwner);
        User newOwner = DataManager.getInstance().getDao().getUser(3);
        Assert.assertNotNull(newOwner);

        Assert.assertEquals(3, DataManager.getInstance().getDao().changeCommentsOwner(oldOwner, newOwner));

        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork("PI_1");
        Assert.assertEquals(4, comments.size());
        Assert.assertEquals(newOwner, comments.get(0).getCreator());
        Assert.assertNotEquals(newOwner, comments.get(1).getCreator());
        Assert.assertEquals(newOwner, comments.get(2).getCreator());
        Assert.assertEquals(newOwner, comments.get(3).getCreator());
    }

    // Search

    @Test
    public void getAllSearchesTest() throws DAOException {
        List<Search> list = DataManager.getInstance().getDao().getAllSearches();
        Assert.assertEquals(3, list.size());
    }

    @Test
    public void getSearchByIdTest() throws DAOException {
        Search o = DataManager.getInstance().getDao().getSearch(1);
        Assert.assertNotNull(o);
        Assert.assertEquals(Long.valueOf(1), o.getId());
        Assert.assertNotNull(o.getOwner());
        Assert.assertEquals(Long.valueOf(1), o.getOwner().getId());
        Assert.assertEquals("query 1", o.getQuery());
        Assert.assertEquals(1, o.getPage());
        Assert.assertEquals("sort 1", o.getSortString());
        Assert.assertEquals("filter 1", o.getFacetString());
        Assert.assertTrue(o.isNewHitsNotification());
        Assert.assertNotNull(o.getDateUpdated());
    }

    @Test
    public void getSearchesForUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        List<Search> list = DataManager.getInstance().getDao().getSearches(user);
        Assert.assertEquals(2, list.size());
    }

    @Test
    public void addSearchTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllSearches().size());
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
        Assert.assertTrue(DataManager.getInstance().getDao().addSearch(o));
        Assert.assertNotNull(o.getId());
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllSearches().size());

        Search o2 = DataManager.getInstance().getDao().getSearch(o.getId());
        Assert.assertNotNull(o2);
        Assert.assertEquals(o.getOwner(), o2.getOwner());
        Assert.assertEquals(o.getName(), o2.getName());
        Assert.assertEquals(o.getQuery(), o2.getQuery());
        Assert.assertEquals(o.getPage(), o2.getPage());
        Assert.assertEquals(o.getSortString(), o2.getSortString());
        Assert.assertEquals(o.getFacetString(), o2.getFacetString());
        Assert.assertEquals(o.isNewHitsNotification(), o2.isNewHitsNotification());
        Assert.assertEquals(now, o2.getDateUpdated());
    }

    @Test
    public void updateSearchTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllSearches().size());
        Search o = DataManager.getInstance().getDao().getSearch(1);
        Assert.assertNotNull(o);

        o.setName("new name");
        LocalDateTime now = LocalDateTime.now();
        o.setDateUpdated(now);

        Assert.assertTrue(DataManager.getInstance().getDao().updateSearch(o));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllSearches().size());

        Search o2 = DataManager.getInstance().getDao().getSearch(o.getId());
        Assert.assertNotNull(o2);
        Assert.assertEquals(o.getName(), o2.getName());
        Assert.assertEquals(o.getOwner(), o2.getOwner());
        Assert.assertEquals(now, o2.getDateUpdated());
    }

    @Test
    public void deleteSearchTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllSearches().size());
        Search o = DataManager.getInstance().getDao().getSearch(1);
        Assert.assertNotNull(o);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteSearch(o));
        Assert.assertNull(DataManager.getInstance().getDao().getSearch(1));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllSearches().size());
    }

    // License types

    @Test
    public void getAllLicenseTypesTest() throws DAOException {
        Assert.assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
    }

    /**
     * @see JPADAO#getRecordLicenseTypes()
     * @verifies only return non open access license types
     */
    @Test
    public void getRecordTypes_shouldOnlyReturnNonOpenAccessLicenseTypes() throws Exception {
        List<LicenseType> licenseTypes = DataManager.getInstance().getDao().getRecordLicenseTypes();
        Assert.assertEquals(5, licenseTypes.size());
        Assert.assertEquals(Long.valueOf(1), licenseTypes.get(0).getId());
        Assert.assertEquals(Long.valueOf(6), licenseTypes.get(4).getId());
    }

    @Test
    public void getLicenseTypeByIdTest() throws DAOException {
        {
            LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
            Assert.assertNotNull(licenseType);
            Assert.assertEquals(Long.valueOf(1), licenseType.getId());
            Assert.assertEquals("license type 1 name", licenseType.getName());
            Assert.assertEquals("license type 1 desc", licenseType.getDescription());
            Assert.assertEquals(false, licenseType.isOpenAccess());
            Assert.assertEquals(1, licenseType.getPrivileges().size());
            Assert.assertEquals(1, licenseType.getOverriddenLicenseTypes().size());
        }
        {
            LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(4);
            Assert.assertNotNull(licenseType);
            Assert.assertEquals(Long.valueOf(4), licenseType.getId());
            Assert.assertEquals(1, licenseType.getOverriddenLicenseTypes().size());
        }
    }

    @Test
    public void getLicenseTypeByNameTest() throws DAOException {
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType("license type 2 name");
        Assert.assertNotNull(licenseType);
        Assert.assertEquals(Long.valueOf(2), licenseType.getId());
        Assert.assertEquals("license type 2 name", licenseType.getName());
        Assert.assertEquals("license type 2 (unused)", licenseType.getDescription());
        Assert.assertEquals(true, licenseType.isOpenAccess());
        Assert.assertEquals(1, licenseType.getPrivileges().size());
    }

    /**
     * @see JPADAO#getLicenseTypes(List)
     * @verifies return all matching rows
     */
    @Test
    public void getLicenseTypes_shouldReturnAllMatchingRows() throws Exception {
        String[] names = new String[] { "license type 1 name", "license type 2 name" };
        List<LicenseType> result = DataManager.getInstance().getDao().getLicenseTypes(Arrays.asList(names));
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("license type 1 name", result.get(0).getName());
        Assert.assertEquals("license type 2 name", result.get(1).getName());
    }

    /**
     * @see JPADAO#getOverridingLicenseType(LicenseType)
     * @verifies return all matching rows
     */
    @Test
    public void getOverridingLicenseType_shouldReturnAllMatchingRows() throws Exception {
        LicenseType lt = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(lt);
        List<LicenseType> overriding = DataManager.getInstance().getDao().getOverridingLicenseType(lt);
        Assert.assertEquals(1, overriding.size());
        Assert.assertEquals(Long.valueOf(4), overriding.get(0).getId());
    }

    @Test
    public void addLicenseTypeTest() throws DAOException {
        Assert.assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = new LicenseType();
        licenseType.setName("license type to add name");
        licenseType.setDescription("license type to add desc");
        licenseType.getPrivileges().add("license type to add priv 1");
        Assert.assertTrue(DataManager.getInstance().getDao().addLicenseType(licenseType));
        Assert.assertNotNull(licenseType.getId());
        Assert.assertEquals(NUM_LICENSE_TYPES + 1, DataManager.getInstance().getDao().getAllLicenseTypes().size());

        LicenseType licenseType2 = DataManager.getInstance().getDao().getLicenseType(licenseType.getId());
        Assert.assertNotNull(licenseType2);
        Assert.assertEquals(licenseType.getName(), licenseType2.getName());
        Assert.assertEquals(licenseType.getDescription(), licenseType2.getDescription());
        Assert.assertEquals(1, licenseType2.getPrivileges().size());
        Assert.assertTrue(licenseType2.getPrivileges().contains("license type to add priv 1"));
    }

    @Test
    public void updateLicenseTypeTest() throws DAOException {
        Assert.assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(licenseType);
        Assert.assertEquals(1, licenseType.getPrivileges().size());

        licenseType.setName("license type 1 new name");
        licenseType.setDescription("license type 1 new desc");
        licenseType.getPrivileges().add("license type 1 priv 2");
        Assert.assertTrue(DataManager.getInstance().getDao().updateLicenseType(licenseType));
        Assert.assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());

        LicenseType licenseType2 = DataManager.getInstance().getDao().getLicenseType(licenseType.getId());
        Assert.assertNotNull(licenseType2);
        Assert.assertEquals(licenseType.getId(), licenseType2.getId());
        Assert.assertEquals(licenseType.getName(), licenseType2.getName());
        Assert.assertEquals(licenseType.getDescription(), licenseType2.getDescription());
        Assert.assertEquals(2, licenseType.getPrivileges().size());
    }

    @Test
    public void deleteUsedLicenseTypeTest() throws DAOException {
        // Deleting license types in use should fail
        Assert.assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(licenseType);
        Assert.assertFalse(DataManager.getInstance().getDao().deleteLicenseType(licenseType));
        Assert.assertNotNull(DataManager.getInstance().getDao().getLicenseType(1));
        Assert.assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
    }

    @Test
    public void deleteUnusedLicenseTypeTest() throws DAOException {
        Assert.assertEquals(NUM_LICENSE_TYPES, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(2);
        Assert.assertNotNull(licenseType);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteLicenseType(licenseType));
        Assert.assertNull(DataManager.getInstance().getDao().getLicenseType(2));
        Assert.assertEquals(NUM_LICENSE_TYPES - 1, DataManager.getInstance().getDao().getAllLicenseTypes().size());
    }

    // Roles

    @Test
    public void getAllRolesTest() throws DAOException {
        List<Role> roles = DataManager.getInstance().getDao().getAllRoles();
        Assert.assertEquals(2, roles.size());
    }

    @Test
    public void getRoleByIdTest() throws DAOException {
        Role role = DataManager.getInstance().getDao().getRole(1);
        Assert.assertNotNull(role);
        Assert.assertEquals(Long.valueOf(1), role.getId());
        Assert.assertEquals("role 1 name", role.getName());
        Assert.assertEquals("role 1 desc", role.getDescription());
        Assert.assertEquals(1, role.getPrivileges().size());
    }

    @Test
    public void getRoleByNameTest() throws DAOException {
        Role role = DataManager.getInstance().getDao().getRole("role 1 name");
        Assert.assertNotNull(role);
        Assert.assertEquals(Long.valueOf(1), role.getId());
        Assert.assertEquals("role 1 name", role.getName());
        Assert.assertEquals("role 1 desc", role.getDescription());
        Assert.assertEquals(1, role.getPrivileges().size());
    }

    @Test
    public void addRoleTest() throws DAOException {
        Role role = new Role();
        role.setName("role to add name");
        role.setDescription("role to add desc");
        role.getPrivileges().add("role to add priv 1");
        Assert.assertTrue(DataManager.getInstance().getDao().addRole(role));
        Assert.assertNotNull(role.getId());

        Role role2 = DataManager.getInstance().getDao().getRole(role.getId());
        Assert.assertNotNull(role2);
        Assert.assertEquals(role.getName(), role2.getName());
        Assert.assertEquals(role.getDescription(), role2.getDescription());
        Assert.assertEquals(1, role2.getPrivileges().size());
        Assert.assertTrue(role2.getPrivileges().contains("role to add priv 1"));
    }

    @Test
    public void updateRoleTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllRoles().size());
        Role role = DataManager.getInstance().getDao().getRole(1);
        Assert.assertNotNull(role);
        Assert.assertEquals("role 1 name", role.getName());
        Assert.assertEquals("role 1 desc", role.getDescription());
        Assert.assertEquals(1, role.getPrivileges().size());

        role.setName("role 1 new name");
        role.setDescription("role 1 new desc");
        role.getPrivileges().add("role 1 priv 2");
        Assert.assertTrue(DataManager.getInstance().getDao().updateRole(role));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllRoles().size());

        Role role2 = DataManager.getInstance().getDao().getRole(role.getId());
        Assert.assertNotNull(role2);
        Assert.assertEquals(role.getId(), role2.getId());
        Assert.assertEquals(role.getName(), role2.getName());
        Assert.assertEquals(role.getDescription(), role2.getDescription());
        Assert.assertEquals(2, role.getPrivileges().size());
    }

    @Test
    public void duplicateRolePrivilegeTest() throws DAOException {
        Role role = DataManager.getInstance().getDao().getRole(1);
        Assert.assertNotNull(role);
        Assert.assertEquals(1, role.getPrivileges().size());

        role.getPrivileges().add("role 1 priv 1");
        Assert.assertTrue(DataManager.getInstance().getDao().updateRole(role));

        Role role2 = DataManager.getInstance().getDao().getRole(role.getId());
        Assert.assertNotNull(role2);
        Assert.assertEquals(1, role2.getPrivileges().size());
    }

    @Test
    public void deleteRoleTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllRoles().size());
        Role role = DataManager.getInstance().getDao().getRole(2);
        Assert.assertNotNull(role);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteRole(role));
        Assert.assertNull(DataManager.getInstance().getDao().getRole(2));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllRoles().size());
    }

    // Bookmarks

    @Test
    public void getAllBookmarkListsTest() throws DAOException {
        List<BookmarkList> result = DataManager.getInstance().getDao().getAllBookmarkLists();
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void getPublicBookmarkListsTest() throws DAOException {
        List<BookmarkList> result = DataManager.getInstance().getDao().getPublicBookmarkLists();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(2), result.get(0).getId());
    }

    @Test
    public void getAllBookmarkListsForUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        List<BookmarkList> boomarkLists = DataManager.getInstance().getDao().getBookmarkLists(user);
        Assert.assertEquals(2, boomarkLists.size());
        Assert.assertEquals(Long.valueOf(3), boomarkLists.get(0).getId());
        Assert.assertEquals(user, boomarkLists.get(0).getOwner());
    }

    @Test
    public void countAllBookmarkListsForUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        List<BookmarkList> boomarkLists = DataManager.getInstance().getDao().getBookmarkLists(user);
        long count = DataManager.getInstance().getDao().getBookmarkListCount(user);
        Assert.assertEquals(boomarkLists.size(), count);
    }

    @Test
    public void getBookmarkListByIdTest() throws DAOException {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(1);
        Assert.assertNotNull(bl);
        Assert.assertEquals(Long.valueOf(1), bl.getId());
        Assert.assertNotNull(bl.getOwner());
        Assert.assertEquals(Long.valueOf(1), bl.getOwner().getId());
        Assert.assertEquals("bookmark list 1 name", bl.getName());
        Assert.assertEquals("bookmark list 1 desc", bl.getDescription());
        Assert.assertEquals(2, bl.getItems().size());

    }

    /**
     * @see JPADAO#getBookmarkList(String,User)
     * @verifies return correct row for name
     */
    @Test
    public void getBookmarkList_shouldReturnCorrectRowForName() throws Exception {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList("bookmark list 1 name", null);
        Assert.assertNotNull(bl);
        Assert.assertEquals(Long.valueOf(1), bl.getId());
        Assert.assertNotNull(bl.getOwner());
        Assert.assertEquals(Long.valueOf(1), bl.getOwner().getId());
        Assert.assertEquals("bookmark list 1 name", bl.getName());
        Assert.assertEquals("bookmark list 1 desc", bl.getDescription());
        Assert.assertEquals(2, bl.getItems().size());
    }

    /**
     * @see JPADAO#getBookmarkList(String,User)
     * @verifies return correct row for name and user
     */
    @Test
    public void getBookmarkList_shouldReturnCorrectRowForNameAndUser() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);

        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList("bookmark list 1 name", user);
        Assert.assertNotNull(bl);
        Assert.assertEquals(Long.valueOf(1), bl.getId());
        Assert.assertNotNull(bl.getOwner());
        Assert.assertEquals(Long.valueOf(1), bl.getOwner().getId());
        Assert.assertEquals("bookmark list 1 name", bl.getName());
        Assert.assertEquals("bookmark list 1 desc", bl.getDescription());
        Assert.assertEquals(2, bl.getItems().size());
    }

    /**
     * @see JPADAO#getBookmarkList(String,User)
     * @verifies return null if no result found
     */
    @Test
    public void getBookmarkList_shouldReturnNullIfNoResultFound() throws Exception {
        Assert.assertNull(DataManager.getInstance().getDao().getBookmarkList("notfound", null));
    }

    /**
     * @see JPADAO#getBookmarkListByShareKey(String)
     * @verifies return correct row
     */
    @Test
    public void getBookmarkListByShareKey_shouldReturnCorrectRow() throws Exception {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkListByShareKey("c548e2ea6915acbfa17c3dc6f453f5b1");
        Assert.assertNotNull(bl);
        Assert.assertEquals(Long.valueOf(1), bl.getId());
    }

    /**
     * @see JPADAO#getBookmarkListByShareKey(String)
     * @verifies return null if no result found
     */
    @Test
    public void getBookmarkListByShareKey_shouldReturnNullIfNoResultFound() throws Exception {
        Assert.assertNull(DataManager.getInstance().getDao().getBookmarkListByShareKey("123"));
    }

    /**
     * @see JPADAO#getBookmarkListByShareKey(String)
     * @verifies throw IllegalArgumentException if shareKey empty
     */
    @Test(expected = IllegalArgumentException.class)
    public void getBookmarkListByShareKey_shouldThrowIllegalArgumentExceptionIfShareKeyEmpty() throws Exception {
        DataManager.getInstance().getDao().getBookmarkListByShareKey("");
    }

    @Test
    public void addBookmarkListTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);

        BookmarkList bl = new BookmarkList();
        bl.setName("add bookmark list test");
        bl.setOwner(user);
        bl.setDescription("add bookmark list test desc");
        Bookmark item = new Bookmark("PPNTEST", "add bookmark 1 main title", "add bookmark 1 name");
        item.setDescription("add bookmark 1 desc");
        bl.addItem(item);
        Assert.assertTrue(DataManager.getInstance().getDao().addBookmarkList(bl));

        BookmarkList bl2 = DataManager.getInstance().getDao().getBookmarkList("add bookmark list test", user);
        Assert.assertNotNull(bl2);
        Assert.assertNotNull(bl2.getId());
        Assert.assertEquals(user, bl2.getOwner());
        Assert.assertEquals(bl.getName(), bl2.getName());
        Assert.assertEquals(bl.getDescription(), bl2.getDescription());
        Assert.assertEquals(1, bl2.getItems().size());
        Bookmark item2 = bl2.getItems().get(0);
        Assert.assertEquals(bl2, item2.getBookmarkList());
        Assert.assertEquals("PPNTEST", item2.getPi());
        //        Assert.assertEquals("add bookmark 1 main title", item2.getMainTitle());
        Assert.assertEquals("add bookmark 1 name", item2.getName());
        Assert.assertEquals("add bookmark 1 desc", item2.getDescription());
    }

    @Test
    public void updateBookmarkListTest() throws DAOException {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(1);
        Assert.assertNotNull(bl);
        Assert.assertEquals(2, bl.getItems().size());

        int numBookmarkLists = DataManager.getInstance().getDao().getAllBookmarkLists().size();

        Bookmark item = new Bookmark("PPNTEST", "addBookmarkTest item main title", "addBookmarkTest item name");
        item.setDescription("addBookmarkTest item desc");
        bl.addItem(item);
        bl.setName("bookmark list 1 new name");
        bl.setDescription("bookmark list 1 new desc");
        Assert.assertTrue(DataManager.getInstance().getDao().updateBookmarkList(bl));
        //        Assert.assertNotNull(item.getId());

        int numBookmarkLists2 = DataManager.getInstance().getDao().getAllBookmarkLists().size();
        Assert.assertEquals(numBookmarkLists, numBookmarkLists2);

        BookmarkList bl2 = DataManager.getInstance().getDao().getBookmarkList(bl.getId());
        Assert.assertNotNull(bl2);
        Assert.assertEquals(bl.getId(), bl2.getId());
        Assert.assertEquals(bl.getName(), bl2.getName());
        Assert.assertEquals(bl.getDescription(), bl2.getDescription());
        Assert.assertEquals(3, bl2.getItems().size());
    }

    @Test
    public void deleteBookmarkListTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllBookmarkLists().size());
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(1);
        Assert.assertNotNull(bl);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteBookmarkList(bl));
        Assert.assertNull(DataManager.getInstance().getDao().getBookmarkList(1));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllBookmarkLists().size());
    }

    @Test
    public void removeBookMarkTest() throws DAOException {
        BookmarkList bl = DataManager.getInstance().getDao().getBookmarkList(1);
        Assert.assertNotNull(bl);
        Assert.assertEquals(2, bl.getItems().size());
        bl.removeItem(bl.getItems().get(0));
        Assert.assertTrue(DataManager.getInstance().getDao().updateBookmarkList(bl));

        BookmarkList bl2 = DataManager.getInstance().getDao().getBookmarkList(1);
        Assert.assertNotNull(bl2);
        Assert.assertEquals(1, bl2.getItems().size());
    }

    /**
     * @see JPADAO#getComments(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getComments_shouldSortResultsCorrectly() throws Exception {
        List<Comment> ret = DataManager.getInstance().getDao().getComments(0, 2, "body", true, null, null);
        Assert.assertEquals(2, ret.size());
        Assert.assertEquals(Long.valueOf(4), ret.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), ret.get(1).getId());
    }

    /**
     * @see JPADAO#getComments(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getComments_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("targetPI", "pi_1");
        filterMap.put("body", "ment 2");
        List<Comment> ret = DataManager.getInstance().getDao().getComments(0, 2, null, true, filterMap, null);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("comment 2 text", ret.get(0).getText());
    }

    /**
     * @see JPADAO#getComments(int,int,String,boolean,Map,Set)
     * @verifies apply target pi filter correctly
     */
    @Test
    public void getComments_shouldApplyTargetPiFilterCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        // TODO update test dataset to include comments with different target_pi
        List<Comment> ret = DataManager.getInstance().getDao().getComments(0, 10, null, true, filterMap, Collections.singleton("PI_1"));
        Assert.assertEquals(4, ret.size());
    }

    /**
     * @see JPADAO#getUserGroups(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getUserGroups_shouldSortResultsCorrectly() throws Exception {
        List<UserGroup> userGroups = DataManager.getInstance().getDao().getUserGroups(0, 2, "name", true, null);
        Assert.assertEquals(2, userGroups.size());
        Assert.assertEquals(Long.valueOf(3), userGroups.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), userGroups.get(1).getId());
    }

    /**
     * @see JPADAO#getUserGroups(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getUserGroups_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("description", "no members");
        filterMap.put("name", "user group 2 name");
        List<UserGroup> ret = DataManager.getInstance().getDao().getUserGroups(0, 2, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("user group 2 name", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getUsers(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getUsers_shouldSortResultsCorrectly() throws Exception {
        List<User> users = DataManager.getInstance().getDao().getUsers(0, 2, "score", true, null);
        Assert.assertEquals(2, users.size());
        Assert.assertEquals(Long.valueOf(2), users.get(0).getId());
        Assert.assertEquals(Long.valueOf(1), users.get(1).getId());
    }

    /**
     * @see JPADAO#getUsers(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getUsers_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("email", "1@users.org");
        List<User> ret = DataManager.getInstance().getDao().getUsers(0, 2, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("1@users.org", ret.get(0).getEmail());
    }

    /**
     * @see JPADAO#getUsersByPropertyValue(String,String)
     * @verifies return correct rows
     */
    @Test
    public void getUsersByPropertyValue_shouldReturnCorrectRows() throws Exception {
        List<User> result = DataManager.getInstance().getDao().getUsersByPropertyValue("foo", "bar");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Long.valueOf(2), result.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), result.get(1).getId());
    }

    /**
     * @see JPADAO#getSearches(User,int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getSearches_shouldSortResultsCorrectly() throws Exception {
        List<Search> ret = DataManager.getInstance().getDao().getSearches(null, 0, 10, "name", true, null);
        Assert.assertEquals(3, ret.size());
        Assert.assertEquals(Long.valueOf(3), ret.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), ret.get(1).getId());
        Assert.assertEquals(Long.valueOf(1), ret.get(2).getId());
    }

    /**
     * @see JPADAO#getSearchCount(User,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getSearchCount_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("newHitsNotification", "true");
        Assert.assertEquals(2, DataManager.getInstance().getDao().getSearchCount(null, filterMap));
    }

    /**
     * @see JPADAO#getSearches(User,int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getSearches_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("query", "y");
        filterMap.put("name", "search 1");
        List<Search> ret = DataManager.getInstance().getDao().getSearches(DataManager.getInstance().getDao().getUser(1), 0, 2, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("search 1", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getRoles(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getRoles_shouldSortResultsCorrectly() throws Exception {
        List<Role> ret = DataManager.getInstance().getDao().getRoles(0, 2, "name", true, null);
        Assert.assertEquals(2, ret.size());
        Assert.assertEquals(Long.valueOf(2), ret.get(0).getId());
        Assert.assertEquals(Long.valueOf(1), ret.get(1).getId());
    }

    /**
     * @see JPADAO#getRoles(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getRoles_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("name", "role 2 name");
        filterMap.put("description", "unused");
        List<Role> ret = DataManager.getInstance().getDao().getRoles(0, 2, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("role 2 name", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getLicenseTypes(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getLicenseTypes_shouldSortResultsCorrectly() throws Exception {
        // asc
        List<LicenseType> ret = DataManager.getInstance().getDao().getLicenseTypes(0, 2, "name", true, null);
        Assert.assertEquals(2, ret.size());
        Assert.assertEquals(Long.valueOf(6), ret.get(0).getId());
        Assert.assertEquals(Long.valueOf(4), ret.get(1).getId());
        // desc
        ret = DataManager.getInstance().getDao().getLicenseTypes(0, 2, "name", false, null);
        Assert.assertEquals(2, ret.size());
        Assert.assertEquals(Long.valueOf(1), ret.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), ret.get(1).getId());
    }

    /**
     * @see JPADAO#getLicenseTypes(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getLicenseTypes_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("name", "license type 2 name");
        filterMap.put("description", "unused");
        List<LicenseType> ret = DataManager.getInstance().getDao().getLicenseTypes(0, 2, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("license type 2 name", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getCoreLicenseTypes(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getCoreLicenseTypes_shouldSortResultsCorrectly() throws Exception {
        // TODO add more core types to test DB

        // asc
        List<LicenseType> ret = DataManager.getInstance().getDao().getCoreLicenseTypes(0, 10, "name", true, null);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals(Long.valueOf(5), ret.get(0).getId());
        // desc
        ret = DataManager.getInstance().getDao().getCoreLicenseTypes(0, 10, "name", false, null);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals(Long.valueOf(5), ret.get(0).getId());
    }

    /**
     * @see JPADAO#getCoreLicenseTypes(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getCoreLicenseTypes_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("name", "cms");
        filterMap.put("description", "cms");
        List<LicenseType> ret = DataManager.getInstance().getDao().getCoreLicenseTypes(0, 10, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("CMS", ret.get(0).getName());

        filterMap.put("description", "notfound");
        Assert.assertEquals(0, DataManager.getInstance().getDao().getCoreLicenseTypes(0, 10, null, true, filterMap).size());
    }

    /**
     * @see JPADAO#getLicenses(LicenseType)
     * @verifies return correct values
     */
    @Test
    public void getLicenses_shouldReturnCorrectValues() throws Exception {
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(licenseType);
        List<License> result = DataManager.getInstance().getDao().getLicenses(licenseType);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Long.valueOf(1), result.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), result.get(1).getId());
    }

    /**
     * @see JPADAO#getLicenseCount(LicenseType)
     * @verifies return correct value
     */
    @Test
    public void getLicenseCount_shouldReturnCorrectValue() throws Exception {
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(licenseType);
        Assert.assertEquals(2, DataManager.getInstance().getDao().getLicenseCount(licenseType));
    }

    /**
     * @see JPADAO#getIpRanges(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getIpRanges_shouldSortResultsCorrectly() throws Exception {
        List<IpRange> ret = DataManager.getInstance().getDao().getIpRanges(0, 2, "name", true, null);
        Assert.assertEquals(2, ret.size());
        Assert.assertEquals(Long.valueOf(2), ret.get(0).getId());
        Assert.assertEquals(Long.valueOf(1), ret.get(1).getId());
    }

    /**
     * @see JPADAO#getIpRanges(int,int,String,boolean,Map)
     * @verifies filter results correctly
     */
    @Test
    public void getIpRanges_shouldFilterResultsCorrectly() throws Exception {
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("name", "localhost");
        filterMap.put("description", "2 desc");
        List<IpRange> ret = DataManager.getInstance().getDao().getIpRanges(0, 2, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("localhost2", ret.get(0).getName());
    }

    /**
     * @see JPADAO#getAllCMSMediaItems()
     * @verifies return all items
     */
    @Test
    public void getAllCMSMediaItems_shouldReturnAllItems() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
    }

    /**
     * @see JPADAO#getCMSMediaItem(long)
     * @verifies return correct item
     */
    @Test
    public void getCMSMediaItem_shouldReturnCorrectItem() throws Exception {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(1);
        Assert.assertNotNull(item);
        Assert.assertEquals(Long.valueOf(1), item.getId());
        Assert.assertEquals("image1.jpg", item.getFileName());
        Assert.assertEquals(2, item.getMetadata().size());
        Assert.assertEquals("de", item.getMetadata().get(0).getLanguage());
        Assert.assertEquals("Bild 1", item.getMetadata().get(0).getName());
        Assert.assertEquals("Beschreibung 1", item.getMetadata().get(0).getDescription());
        Assert.assertEquals("en", item.getMetadata().get(1).getLanguage());
        Assert.assertEquals("Image 1", item.getMetadata().get(1).getName());
        Assert.assertEquals("Description 1", item.getMetadata().get(1).getDescription());
    }

    /**
     * @see JPADAO#addCMSMediaItem(CMSMediaItem)
     * @verifies add item correctly
     */
    @Test
    public void addCMSMediaItem_shouldAddItemCorrectly() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        CMSMediaItem item = new CMSMediaItem();
        item.setFileName("image5.jpg");
        CMSMediaItemMetadata md = new CMSMediaItemMetadata();
        md.setLanguage("eu");
        md.setName("Ongi etorriak");
        md.setDescription("bla");
        item.getMetadata().add(md);
        Assert.assertTrue(DataManager.getInstance().getDao().addCMSMediaItem(item));

        Assert.assertEquals(5, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        Assert.assertNotNull(item.getId());

        CMSMediaItem item2 = DataManager.getInstance().getDao().getCMSMediaItem(item.getId());
        Assert.assertNotNull(item2);
        Assert.assertEquals(item, item2);
        Assert.assertEquals(item.getFileName(), item2.getFileName());
        Assert.assertEquals(1, item2.getMetadata().size());
        Assert.assertEquals(md, item2.getMetadata().get(0));
        Assert.assertEquals(md.getLanguage(), item2.getMetadata().get(0).getLanguage());
        Assert.assertEquals(md.getName(), item2.getMetadata().get(0).getName());
        Assert.assertEquals(md.getDescription(), item2.getMetadata().get(0).getDescription());
    }

    /**
     * @see JPADAO#updateCMSMediaItem(CMSMediaItem)
     * @verifies update item correctly
     */
    @Test
    public void updateCMSMediaItem_shouldUpdateItemCorrectly() throws Exception {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(1);
        Assert.assertNotNull(item);
        Assert.assertEquals(2, item.getMetadata().size());
        item.setFileName("image_new.jpg");
        item.getMetadata().remove(item.getMetadata().get(0));
        Assert.assertTrue(DataManager.getInstance().getDao().updateCMSMediaItem(item));

        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        CMSMediaItem item2 = DataManager.getInstance().getDao().getCMSMediaItem(1);
        Assert.assertNotNull(item2);
        Assert.assertEquals(item, item2);
        Assert.assertEquals(item.getFileName(), item2.getFileName());
        Assert.assertEquals(1, item2.getMetadata().size());
    }

    /**
     * @see JPADAO#deleteCMSMediaItem(CMSMediaItem)
     * @verifies delete item correctly
     */
    @Test
    public void deleteCMSMediaItem_shouldDeleteItemCorrectly() throws Exception {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(2);
        Assert.assertNotNull(item);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteCMSMediaItem(item));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        Assert.assertNull(DataManager.getInstance().getDao().getCMSMediaItem(2));
    }

    /**
     * @see JPADAO#deleteCMSMediaItem(CMSMediaItem)
     * @verifies not delete referenced items
     */
    @Test
    public void deleteCMSMediaItem_shouldNotDeleteReferencedItems() throws Exception {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(1);
        Assert.assertNotNull(item);
        Assert.assertFalse(DataManager.getInstance().getDao().deleteCMSMediaItem(item));
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllCMSMediaItems().size());
        Assert.assertNotNull(DataManager.getInstance().getDao().getCMSMediaItem(1));
    }

    /**
     * @see JPADAO#getAllCMSPages()
     * @verifies return all pages
     */
    @Test
    public void getAllCMSPages_shouldReturnAllPages() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllCMSPages().size());
    }

    /**
     * @see JPADAO#getCMSPageCount(Map)
     * @verifies return correct count
     */
    @Test
    public void getCMSPageCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(3L, DataManager.getInstance().getDao().getCMSPageCount(null, null, null, null));
    }

    /**
     * @see JPADAO#getCMSPagesByClassification(String)
     * @verifies return all pages with given classification
     */
    @Test
    public void getCMSPagesByClassification_shouldReturnAllPagesWithGivenClassification() throws Exception {
        CMSCategory news = DataManager.getInstance().getDao().getCategoryByName("news");
        Assert.assertEquals(2, DataManager.getInstance().getDao().getCMSPagesByCategory(news).size());
    }

    /**
     * @see JPADAO#getCMSPagesForRecord(String)
     * @verifies return all pages with the given related pi
     */
    @Test
    public void getCMSPagesForRecord_shouldReturnAllPagesWithTheGivenRelatedPi() throws Exception {
        CMSCategory c = DataManager.getInstance().getDao().getCategoryByName(CMSPage.CLASSIFICATION_OVERVIEWPAGE);
        Assert.assertEquals(1, DataManager.getInstance().getDao().getCMSPagesForRecord("PI_1", c).size());
    }

    /**
     * @see JPADAO#getCMSPagesWithRelatedPi(int,int,Date,Date)
     * @verifies return correct rows
     */
    @Test
    public void getCMSPagesWithRelatedPi_shouldReturnCorrectRows() throws Exception {
        Assert.assertEquals(1,
                DataManager.getInstance()
                        .getDao()
                        .getCMSPagesWithRelatedPi(0, 100, LocalDateTime.of(2015, 1, 1, 0, 0), LocalDateTime.of(2015, 12, 31, 0, 0))
                        .size());
        // Wrong date range
        Assert.assertEquals(0,
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
    public void isCMSPagesForRecordHaveUpdates_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance()
                .getDao()
                .isCMSPagesForRecordHaveUpdates("PI_1", null, LocalDateTime.of(2015, 1, 1, 0, 0), LocalDateTime.of(2015, 12, 31, 0, 0)));
        Assert.assertFalse(DataManager.getInstance()
                .getDao()
                .isCMSPagesForRecordHaveUpdates("PI_1", null, LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.of(2016, 12, 31, 0, 0)));
        Assert.assertFalse(DataManager.getInstance().getDao().isCMSPagesForRecordHaveUpdates("PI_2", null, null, null));
    }

    /**
     * @see JPADAO#getCMSPageWithRelatedPiCount(Date,Date)
     * @verifies return correct count
     */
    @Test
    public void getCMSPageWithRelatedPiCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(1,
                DataManager.getInstance()
                        .getDao()
                        .getCMSPageWithRelatedPiCount(LocalDateTime.of(2015, 1, 1, 0, 0), LocalDateTime.of(2015, 12, 31, 0, 0)));
    }

    /**
     * @see JPADAO#getCMSPageDefaultViewForRecord(String)
     * @verifies return correct result
     */
    @Test
    public void getCMSPageDefaultViewForRecord_shouldReturnCorrectResult() throws Exception {
        CMSPage page = DataManager.getInstance().getDao().getCMSPageDefaultViewForRecord("PI_1");
        Assert.assertNotNull(page);
        Assert.assertEquals(Long.valueOf(3), page.getId());
    }

    /**
     * @see JPADAO#deleteCMSPage(CMSPage)
     * @verifies delete page correctly
     */
    @Test
    public void deleteCMSPage_shouldDeletePageCorrectly() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllCMSPages().size());
        CMSPage page = DataManager.getInstance().getDao().getCMSPage(1);
        Assert.assertNotNull(page);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteCMSPage(page));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllCMSPages().size());
        Assert.assertNull(DataManager.getInstance().getDao().getCMSPage(1));
    }

    /**
     * @see JPADAO#getAllTopCMSNavigationItems()
     * @verifies return all top items
     */
    @Test
    public void getAllTopCMSNavigationItems_shouldReturnAllTopItems() throws Exception {
        List<CMSNavigationItem> items = DataManager.getInstance().getDao().getAllTopCMSNavigationItems();
        Assert.assertEquals(2, items.size());
        Collections.sort(items);
        Assert.assertEquals(Long.valueOf(1), items.get(0).getId());
        Assert.assertEquals(Long.valueOf(4), items.get(1).getId());
    }

    /**
     * @see JPADAO#getCMSNavigationItem(long)
     * @verifies return correct item and child items
     */
    @Test
    public void getCMSNavigationItem_shouldReturnCorrectItemAndChildItems() throws Exception {
        CMSNavigationItem item = DataManager.getInstance().getDao().getCMSNavigationItem(1);
        Assert.assertNotNull(item);
        Assert.assertEquals(Long.valueOf(1), item.getId());
        Assert.assertEquals("item 1", item.getItemLabel());
        Assert.assertEquals("url 1", item.getPageUrl());
        Assert.assertEquals(Integer.valueOf(1), item.getOrder());
        Assert.assertNull(item.getParentItem());
        Assert.assertEquals(2, item.getChildItems().size());
        Collections.sort(item.getChildItems());
        Assert.assertEquals(Long.valueOf(2), item.getChildItems().get(0).getId());
        Assert.assertEquals(Long.valueOf(3), item.getChildItems().get(1).getId());
        Assert.assertEquals(Integer.valueOf(1), item.getChildItems().get(0).getOrder());
        Assert.assertEquals(Integer.valueOf(2), item.getChildItems().get(1).getOrder());
    }

    /**
     * @see JPADAO#addCMSNavigationItem(CMSNavigationItem)
     * @verifies add item and child items correctly
     */
    @Test
    public void addCMSNavigationItem_shouldAddItemAndChildItemsCorrectly() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        CMSNavigationItem item = new CMSNavigationItem();
        item.setItemLabel("new item");
        item.setPageUrl("url");
        item.setOrder(3);
        CMSNavigationItem childItem = new CMSNavigationItem();
        childItem.setItemLabel("child item");
        childItem.setPageUrl("child url");
        childItem.setParentItem(item); // this also adds the child item to the parent
        childItem.setOrder(1);

        Assert.assertTrue(DataManager.getInstance().getDao().addCMSNavigationItem(item));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        Assert.assertNotNull(item.getId());
        Assert.assertNotNull(childItem.getId());

        CMSNavigationItem item2 = DataManager.getInstance().getDao().getCMSNavigationItem(item.getId());
        Assert.assertNotNull(item2);
        Assert.assertEquals(item.getId(), item2.getId());
        Assert.assertEquals(item.getChildItems().size(), item2.getChildItems().size());
        Assert.assertEquals(childItem.getId(), item2.getChildItems().get(0).getId());
    }

    /**
     * @see JPADAO#updateCMSNavigationItem(CMSNavigationItem)
     * @verifies update item and child items correctly
     */
    @Test
    public void updateCMSNavigationItem_shouldUpdateItemAndChildItemsCorrectly() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        CMSNavigationItem item = DataManager.getInstance().getDao().getCMSNavigationItem(1);
        Assert.assertNotNull(item);
        item.setPageUrl("new url");
        item.getChildItems().get(0).setPageUrl("new child url");

        Assert.assertTrue(DataManager.getInstance().getDao().updateCMSNavigationItem(item));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());

        CMSNavigationItem item2 = DataManager.getInstance().getDao().getCMSNavigationItem(item.getId());
        Assert.assertNotNull(item2);
        Assert.assertEquals(item.getChildItems().size(), item2.getChildItems().size());
        Assert.assertEquals(item.getPageUrl(), item2.getPageUrl());
        Assert.assertEquals(item.getChildItems().get(0).getPageUrl(), item2.getChildItems().get(0).getPageUrl());
    }

    /**
     * @see JPADAO#deleteCMSNavigationItem(CMSNavigationItem)
     * @verifies delete item and child items correctly
     */
    @Test
    public void deleteCMSNavigationItem_shouldDeleteItemAndChildItemsCorrectly() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        CMSNavigationItem item = DataManager.getInstance().getDao().getCMSNavigationItem(1);
        Assert.assertNotNull(item);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteCMSNavigationItem(item));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllTopCMSNavigationItems().size());
        Assert.assertNull(DataManager.getInstance().getDao().getCMSNavigationItem(1));
        Assert.assertNull(DataManager.getInstance().getDao().getCMSNavigationItem(2));
        Assert.assertNull(DataManager.getInstance().getDao().getCMSNavigationItem(3));
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
    //    public void updateUserGroup_shouldSetIdOnNewLicense() throws Exception {
    //        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
    //        Assert.assertNotNull(userGroup);
    //        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
    //        Assert.assertNotNull(licenseType);
    //        License license = new License();
    //        license.setDescription("xxx");
    //        license.setLicenseType(licenseType);
    //        userGroup.addLicense(license);
    //        Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
    //        boolean licenseFound = false;
    //        for (License l : userGroup.getLicenses()) {
    //            if ("xxx".equals(l.getDescription())) {
    //                licenseFound = true;
    //                Assert.assertNotNull(l.getId());
    //            }
    //        }
    //        Assert.assertTrue(licenseFound);
    //    }

    /**
     * @see JPADAO#getUserCount()
     * @verifies return correct count
     */
    @Test
    public void getUserCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(3L, DataManager.getInstance().getDao().getUserCount(null));
    }

    /**
     * Should return 2 results: user1 by its name and user 2 by its user group
     *
     * @see JPADAO#getUserCount(Map)
     * @verifies filter correctly
     */
    @Test
    public void getUserCount_shouldFilterCorrectly() throws Exception {
        Map<String, String> filters = new HashMap<>();
        filters.put("filter", "1");
        Assert.assertEquals(2L, DataManager.getInstance().getDao().getUserCount(filters));
    }

    /**
     * @see JPADAO#getIpRangeCount()
     * @verifies return correct count
     */
    @Test
    public void getIpRangeCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(2L, DataManager.getInstance().getDao().getIpRangeCount(null));
    }

    /**
     * @see JPADAO#getLicenseTypeCount()
     * @verifies return correct count
     */
    @Test
    public void getLicenseTypeCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(NUM_LICENSE_TYPES - 1, DataManager.getInstance().getDao().getLicenseTypeCount(null));
    }

    /**
     * @see JPADAO#getRoleCount()
     * @verifies return correct count
     */
    @Test
    public void getRoleCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(2L, DataManager.getInstance().getDao().getRoleCount(null));
    }

    /**
     * @see JPADAO#getUserGroupCount()
     * @verifies return correct count
     */
    @Test
    public void getUserGroupCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(3L, DataManager.getInstance().getDao().getUserGroupCount(null));
    }

    /**
     * @see JPADAO#getAllDownloadJobs()
     * @verifies return all objects
     */
    @Test
    public void getAllDownloadJobs_shouldReturnAllObjects() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());
    }

    /**
     * @see JPADAO#getDownloadJob(long)
     * @verifies return correct object
     */
    @Test
    public void getDownloadJob_shouldReturnCorrectObject() throws Exception {
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(1);
            Assert.assertNotNull(job);
            Assert.assertEquals(PDFDownloadJob.class, job.getClass());
            Assert.assertEquals(Long.valueOf(1), job.getId());
            Assert.assertEquals("PI_1", job.getPi());
            Assert.assertNull(job.getLogId());
            Assert.assertEquals(DownloadJob.generateDownloadJobId(job.getType(), job.getPi(), job.getLogId()), job.getIdentifier());
            Assert.assertEquals(3600, job.getTtl());
            Assert.assertEquals(JobStatus.WAITING, job.getStatus());
            Assert.assertEquals(1, job.getObservers().size());
            Assert.assertEquals("viewer@intranda.com", job.getObservers().get(0));
        }
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(2);
            Assert.assertNotNull(job);
            Assert.assertEquals(EPUBDownloadJob.class, job.getClass());
            Assert.assertEquals(DownloadJob.generateDownloadJobId(job.getType(), job.getPi(), job.getLogId()), job.getIdentifier());
        }
    }

    /**
     * @see JPADAO#getDownloadJobByIdentifier(String)
     * @verifies return correct object
     */
    @Test
    public void getDownloadJobByIdentifier_shouldReturnCorrectObject() throws Exception {
        DownloadJob job =
                DataManager.getInstance().getDao().getDownloadJobByIdentifier("187277c96410b2358a36e2eb6c8ad76f8610a022d2cd95b180b94a76a1cb118a");
        Assert.assertNotNull(job);
        Assert.assertEquals(PDFDownloadJob.class, job.getClass());
        Assert.assertEquals(Long.valueOf(1), job.getId());
        Assert.assertEquals("PI_1", job.getPi());
        Assert.assertNull(job.getLogId());
        Assert.assertEquals(PDFDownloadJob.generateDownloadJobId(job.getType(), job.getPi(), job.getLogId()), job.getIdentifier());
        Assert.assertEquals(3600, job.getTtl());
        Assert.assertEquals(JobStatus.WAITING, job.getStatus());
        Assert.assertEquals(1, job.getObservers().size());
        Assert.assertEquals("viewer@intranda.com", job.getObservers().get(0));
    }

    /**
     * @see JPADAO#getDownloadJobByMetadata(String,String,String)
     * @verifies return correct object
     */
    @Test
    public void getDownloadJobByMetadata_shouldReturnCorrectObject() throws Exception {
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByMetadata(PDFDownloadJob.LOCAL_TYPE, "PI_1", null);
            Assert.assertNotNull(job);
            Assert.assertEquals(PDFDownloadJob.class, job.getClass());
            Assert.assertEquals(Long.valueOf(1), job.getId());
        }
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByMetadata(EPUBDownloadJob.LOCAL_TYPE, "PI_1", "LOG_0001");
            Assert.assertNotNull(job);
            Assert.assertEquals(EPUBDownloadJob.class, job.getClass());
            Assert.assertEquals(Long.valueOf(2), job.getId());
        }
    }

    /**
     * @see JPADAO#addDownloadJob(DownloadJob)
     * @verifies add object correctly
     */
    @Test
    public void addDownloadJob_shouldAddObjectCorrectly() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        PDFDownloadJob job = new PDFDownloadJob("PI_2", "LOG_0002", now, 3600);
        job.generateDownloadIdentifier();
        job.setStatus(JobStatus.WAITING);
        Assert.assertNotNull(job.getIdentifier());
        job.getObservers().add("a@b.com");

        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());
        Assert.assertTrue(DataManager.getInstance().getDao().addDownloadJob(job));
        Assert.assertNotNull(job.getId());
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllDownloadJobs().size());

        DownloadJob job2 = DataManager.getInstance().getDao().getDownloadJob(job.getId());
        Assert.assertNotNull(job2);
        Assert.assertEquals(job.getId(), job2.getId());
        Assert.assertEquals(job.getPi(), job2.getPi());
        Assert.assertEquals(job.getLogId(), job2.getLogId());
        Assert.assertEquals(job.getIdentifier(), job2.getIdentifier());
        Assert.assertEquals(now, job2.getLastRequested());
        Assert.assertEquals(job.getTtl(), job2.getTtl());
        Assert.assertEquals(job.getStatus(), job2.getStatus());
    }

    /**
     * @see JPADAO#updateDownloadJob(DownloadJob)
     * @verifies update object correctly
     */
    @Test
    public void updateDownloadJob_shouldUpdateObjectCorrectly() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());

        DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(1);
        Assert.assertNotNull(job);
        LocalDateTime now = LocalDateTime.now();
        job.setLastRequested(now);
        job.setStatus(JobStatus.READY);
        job.getObservers().add("newobserver@example.com");

        Assert.assertTrue(DataManager.getInstance().getDao().updateDownloadJob(job));
        Assert.assertEquals("Too many observers after updateDownloadJob", 2, job.getObservers().size());
        List<DownloadJob> allJobs = DataManager.getInstance().getDao().getAllDownloadJobs();
        Assert.assertEquals(2, allJobs.size());
        Assert.assertEquals("Too many observers after getAllDownloadJobs", 2, job.getObservers().size());

        DownloadJob job2 = DataManager.getInstance().getDao().getDownloadJob(job.getId());
        Assert.assertNotNull(job2);
        Assert.assertEquals(job.getId(), job2.getId());
        Assert.assertEquals(now, job2.getLastRequested());
        Assert.assertEquals(JobStatus.READY, job2.getStatus());
        Assert.assertEquals(2, job2.getObservers().size());
        Assert.assertEquals("newobserver@example.com", job2.getObservers().get(1));
    }

    /**
     * @see JPADAO#deleteDownloadJob(DownloadJob)
     * @verifies delete object correctly
     */
    @Test
    public void deleteDownloadJob_shouldDeleteObjectCorrectly() throws Exception {
        DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(1);
        Assert.assertNotNull(job);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteDownloadJob(job));
        Assert.assertNull(DataManager.getInstance().getDao().getDownloadJob(1));
    }

    /**
     * @see JPADAO#getUploadJobsForCreatorId(Long)
     * @verifies return rows in correct order
     */
    @Test
    public void getUploadJobsForCreatorId_shouldReturnRowsInCorrectOrder() throws Exception {
        List<UploadJob> result = DataManager.getInstance().getDao().getUploadJobsForCreatorId(1L);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Long.valueOf(2), result.get(0).getId());
        Assert.assertEquals(Long.valueOf(1), result.get(1).getId());
    }

    /**
     * @see JPADAO#getUploadJobsWithStatus(JobStatus)
     * @verifies return rows with given status
     */
    @Test
    public void getUploadJobsWithStatus_shouldReturnRowsWithGivenStatus() throws Exception {
        List<UploadJob> result = DataManager.getInstance().getDao().getUploadJobsWithStatus(JobStatus.WAITING);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(1), result.get(0).getId());
    }

    /**
     * @see JPADAO#getUploadJobsForCreatorId(Long)
     * @verifies return empty list if creatorId null
     */
    @Test
    public void getUploadJobsForCreatorId_shouldReturnEmptyListIfCreatorIdNull() throws Exception {
        List<UploadJob> result = DataManager.getInstance().getDao().getUploadJobsForCreatorId(null);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void getCMSPagesCount_shouldReturnCorrectCount() throws Exception {
        long numPages = DataManager.getInstance().getDao().getCMSPageCount(Collections.emptyMap(), null, null, null);
        Assert.assertEquals(3, numPages);
    }

    @Test
    public void getStaticPageForCMSPage_shouldReturnCorrectResult() throws Exception {
        List<CMSPage> cmsPages = DataManager.getInstance().getDao().getAllCMSPages();
        for (CMSPage page : cmsPages) {
            Optional<CMSStaticPage> staticPage = DataManager.getInstance().getDao().getStaticPageForCMSPage(page).stream().findFirst();
            if (page.getId().equals(1l)) {
                Assert.assertTrue(staticPage.isPresent());
                Assert.assertTrue(staticPage.get().getPageName().equals("index"));
                Assert.assertTrue(staticPage.get().getCmsPage().equals(page));
            } else {
                Assert.assertFalse(staticPage.isPresent());
            }
        }
    }

    @Test
    public void testCreateCMSPageFilter_createValidQueryWithAllParams() throws AccessDeniedException {

        List<String> categories = Arrays.asList(new String[] { "c1", "c2", "c3" });
        List<String> subThemes = Arrays.asList(new String[] { "s1" });
        List<Long> templates = Arrays.asList(new Long[] { 1l, 2l });

        Map<String, Object> params = new HashMap<>();

        String query = JPADAO.createCMSPageFilter(params, "p", templates, subThemes, categories);

        String shouldQuery = "(:tpl1 = p.templateId OR :tpl2 = p.templateId) AND (:thm1 = p.subThemeDiscriminatorValue) AND "
                + "(:cat1 IN (SELECT c.id FROM p.categories c) OR :cat2 IN (SELECT c.id FROM p.categories c) OR :cat3 IN (SELECT c.id FROM p.categories c))";
        Assert.assertEquals(shouldQuery, query);

        Assert.assertEquals("c1", params.get("cat1"));
        Assert.assertEquals("c2", params.get("cat2"));
        Assert.assertEquals("c3", params.get("cat3"));
        Assert.assertEquals("s1", params.get("thm1"));
        Assert.assertEquals(1l, params.get("tpl1"));
        Assert.assertEquals(2l, params.get("tpl2"));

    }

    @Test
    public void testCreateCMSPageFilter_createValidQueryWithTwoParams() throws AccessDeniedException {

        List<String> categories = Arrays.asList(new String[] { "c1", "c2", "c3" });
        List<String> subThemes = Arrays.asList(new String[] { "s1" });

        Map<String, Object> params = new HashMap<>();

        String query = JPADAO.createCMSPageFilter(params, "p", null, subThemes, categories);

        String shouldQuery = "(:thm1 = p.subThemeDiscriminatorValue) AND "
                + "(:cat1 IN (SELECT c.id FROM p.categories c) OR :cat2 IN (SELECT c.id FROM p.categories c) OR :cat3 IN (SELECT c.id FROM p.categories c))";
        Assert.assertEquals(shouldQuery, query);

        Assert.assertEquals("c1", params.get("cat1"));
        Assert.assertEquals("c2", params.get("cat2"));
        Assert.assertEquals("c3", params.get("cat3"));
        Assert.assertEquals("s1", params.get("thm1"));
    }

    @Test
    public void testCreateCMSPageFilter_createValidQueryWithOneParam() throws AccessDeniedException {

        List<Long> templates = Arrays.asList(new Long[] { 1l, 2l });

        Map<String, Object> params = new HashMap<>();

        String query = JPADAO.createCMSPageFilter(params, "p", templates, null, null);

        String shouldQuery = "(:tpl1 = p.templateId OR :tpl2 = p.templateId)";
        Assert.assertEquals(shouldQuery, query);

        Assert.assertEquals(1l, params.get("tpl1"));
        Assert.assertEquals(2l, params.get("tpl2"));
    }

    /**
     * @see JPADAO#getAllCampaigns()
     * @verifies return all campaigns
     */
    @Test
    public void getAllCampaigns_shouldReturnAllCampaigns() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllCampaigns().size());
    }

    /**
     * @see JPADAO#getCampaigns(int,int,String,boolean,Map)
     * @verifies filter campaigns correctly
     */
    @Test
    public void getCampaigns_shouldFilterCampaignsCorrectly() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getCampaigns(0, 10, null, false, null).size());
        Assert.assertEquals(2,
                DataManager.getInstance().getDao().getCampaigns(0, 10, null, false, Collections.singletonMap("visibility", "PUBLIC")).size());
    }

    /**
     * @see JPADAO#getCampaignCount(Map)
     * @verifies count correctly
     */
    @Test
    public void getCampaignCount_shouldCountCorrectly() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getCampaignCount(null));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getCampaignCount(Collections.singletonMap("visibility", "PUBLIC")));
    }

    /**
     * @see JPADAO#getCampaign(Long)
     * @verifies return correct campaign
     */
    @Test
    public void getCampaign_shouldReturnCorrectCampaign() throws Exception {
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            Assert.assertNotNull(campaign);
            campaign.setSelectedLocale(Locale.ENGLISH);
            Assert.assertEquals(Long.valueOf(1), campaign.getId());
            Assert.assertEquals(CampaignVisibility.PUBLIC, campaign.getVisibility());
            Assert.assertEquals(StatisticMode.RECORD, campaign.getStatisticMode());
            Assert.assertEquals("+DC:varia", campaign.getSolrQuery());
            Assert.assertEquals("English title", campaign.getTitle());

            Assert.assertEquals(2, campaign.getQuestions().size());
            Assert.assertEquals("English text", campaign.getQuestions().get(0).getText().getText(Locale.ENGLISH));

            Assert.assertEquals(4, campaign.getStatistics().size());
            Assert.assertNotNull(campaign.getStatistics().get("PI_1"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_2"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_3"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_4"));
        }
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(3L);
            Assert.assertNotNull(campaign);
            campaign.setSelectedLocale(Locale.ENGLISH);
            Assert.assertEquals(Long.valueOf(3), campaign.getId());
            Assert.assertEquals(CampaignVisibility.PUBLIC, campaign.getVisibility());
            Assert.assertEquals("+PI:PI_5", campaign.getSolrQuery());

            Assert.assertEquals(1, campaign.getStatistics().size());
            CampaignRecordStatistic recordStatistic = campaign.getStatistics().get("PI_5");
            Assert.assertNotNull(recordStatistic);
            Assert.assertEquals(StatisticMode.PAGE, campaign.getStatisticMode());
            Assert.assertEquals(2, recordStatistic.getPageStatistics().size());
            Assert.assertEquals(Integer.valueOf(1), recordStatistic.getPageStatistics().get("PI_5_1").getPage());
            Assert.assertEquals("PI_5", recordStatistic.getPageStatistics().get("PI_5_1").getPi());
        }
    }

    @Test
    public void testLoadCampaignWithLogMessage() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        Assert.assertNotNull(campaign);
        Assert.assertEquals(1, campaign.getLogMessages().size());

        CampaignLogMessage message = campaign.getLogMessages().get(0);
        Assert.assertEquals("Eine Nachricht im Log", message.getMessage());
        Assert.assertEquals(Long.valueOf(1), message.getCreatorId());
        Assert.assertEquals("PI_1", message.getPi());
        Assert.assertEquals(campaign, message.getCampaign());
    }

    @Test
    public void testCampaignUpdate() throws DAOException {
        Campaign campaign = new Campaign();
        campaign.setTitle("Test titel");
        campaign.setId(2L);
        campaign.setSolrQuery("*:*");
        campaign.setDateCreated(LocalDateTime.now());

        Campaign campaign2 = DataManager.getInstance().getDao().getCampaign(2L);

        Assert.assertTrue(DataManager.getInstance().getDao().updateCampaign(campaign));
        campaign = DataManager.getInstance().getDao().getCampaign(2L);
        Assert.assertEquals("Test titel", campaign.getTitle());
    }

    @Test
    public void testUpdateCampaignWithLogMessage() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(2L);
        Assert.assertNotNull(campaign);

        LogMessage message = new LogMessage("Test", 1l, LocalDateTime.now(), null);
        campaign.addLogMessage(message, "PI_10");
        Assert.assertEquals("Test", campaign.getLogMessages().get(0).getMessage());

        DataManager.getInstance().getDao().updateCampaign(campaign);
        campaign = DataManager.getInstance().getDao().getCampaign(2L);
        Assert.assertEquals("Test", campaign.getLogMessages().get(0).getMessage());

    }

    /**
     * @see JPADAO#getQuestion(Long)
     * @verifies return correct row
     */
    @Test
    public void getQuestion_shouldReturnCorrectRow() throws Exception {
        Question q = DataManager.getInstance().getDao().getQuestion(1L);
        Assert.assertNotNull(q);
        Assert.assertEquals(Long.valueOf(1), q.getId());
        Assert.assertEquals(Long.valueOf(1), q.getOwner().getId());
        Assert.assertEquals("English text", q.getText().getText(Locale.ENGLISH));
        Assert.assertEquals(QuestionType.PLAINTEXT, q.getQuestionType());
        Assert.assertEquals(TargetSelector.RECTANGLE, q.getTargetSelector());
        Assert.assertEquals(0, q.getTargetFrequency());
    }

    /**
     * @see JPADAO#getCampaignStatisticsForRecord(String,CrowdsourcingStatus)
     * @verifies return correct rows
     */
    @Test
    public void getCampaignStatisticsForRecord_shouldReturnCorrectRows() throws Exception {
        Assert.assertEquals(1, DataManager.getInstance().getDao().getCampaignStatisticsForRecord("PI_1", CrowdsourcingStatus.FINISHED).size());
    }

    /**
     * @see JPADAO#getAllAnnotations(String,boolean)
     * @verifies sort correctly
     */
    @Test
    public void getAllAnnotations_shouldSortCorrectly() throws Exception {
        {
            List<CrowdsourcingAnnotation> result = DataManager.getInstance().getDao().getAllAnnotations("id", false);
            Assert.assertEquals(5, result.size());
            Assert.assertEquals(Long.valueOf(1), result.get(0).getId());
            Assert.assertEquals(Long.valueOf(2), result.get(1).getId());
            Assert.assertEquals(Long.valueOf(3), result.get(2).getId());
            Assert.assertEquals(Long.valueOf(4), result.get(3).getId());
            Assert.assertEquals(Long.valueOf(5), result.get(4).getId());
        }
        {
            List<CrowdsourcingAnnotation> result = DataManager.getInstance().getDao().getAllAnnotations("id", true);
            Assert.assertEquals(5, result.size());
            Assert.assertEquals(Long.valueOf(5), result.get(0).getId());
            Assert.assertEquals(Long.valueOf(4), result.get(1).getId());
            Assert.assertEquals(Long.valueOf(3), result.get(2).getId());
            Assert.assertEquals(Long.valueOf(2), result.get(3).getId());
            Assert.assertEquals(Long.valueOf(1), result.get(4).getId());
        }
    }

    /**
     * @see JPADAO#getAllAnnotations(String,boolean)
     * @verifies throw IllegalArgumentException if sortField unknown
     */
    @Test(expected = IllegalArgumentException.class)
    public void getAllAnnotations_shouldThrowIllegalArgumentExceptionIfSortFieldUnknown() throws Exception {
        DataManager.getInstance().getDao().getAllAnnotations("foo", false);
    }

    /**
     * @see JPADAO#getAnnotation(Long)
     * @verifies return correct row
     */
    @Test
    public void getAnnotation_shouldReturnCorrectRow() throws Exception {
        CrowdsourcingAnnotation annotation = DataManager.getInstance().getDao().getAnnotation(1L);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(Long.valueOf(1), annotation.getId());
    }

    /**
     * @see JPADAO#getAnnotationsForCampaign(Campaign)
     * @verifies return correct rows
     */
    @Test
    public void getAnnotationsForCampaign_shouldReturnCorrectRows() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        Assert.assertNotNull(campaign);

        List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaign(campaign);
        Assert.assertEquals(3, annotations.size());
        Assert.assertEquals(Long.valueOf(1), annotations.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), annotations.get(1).getId());
        Assert.assertEquals(Long.valueOf(3), annotations.get(2).getId());
    }

    /**
     * @see JPADAO#getAnnotationsForWork(String)
     * @verifies return correct rows
     */
    @Test
    public void getAnnotationsForWork_shouldReturnCorrectRows() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        Assert.assertNotNull(campaign);

        List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForWork("PI_1");
        Assert.assertEquals(3, annotations.size());
        Assert.assertEquals(Long.valueOf(1), annotations.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), annotations.get(1).getId());
        Assert.assertEquals(Long.valueOf(4), annotations.get(2).getId());
    }

    /**
     * @see JPADAO#getAnnotationCountForTarget(String,Integer)
     * @verifies return correct count
     */
    @Test
    public void getAnnotationCountForTarget_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAnnotationCountForTarget("PI_1", 1));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAnnotationCountForTarget("PI_1", null));
    }

    /**
     * @see JPADAO#getAnnotationsForTarget(String,Integer)
     * @verifies return correct rows
     */
    @Test
    public void getAnnotationsForTarget_shouldReturnCorrectRows() throws Exception {
        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForTarget("PI_1", 1);
            Assert.assertEquals(1, annotations.size());
            Assert.assertEquals(Long.valueOf(1), annotations.get(0).getId());
        }
        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForTarget("PI_1", null);
            Assert.assertEquals(2, annotations.size());
            Assert.assertEquals(Long.valueOf(3), annotations.get(0).getId());
            Assert.assertEquals(Long.valueOf(4), annotations.get(1).getId());
        }
    }

    /**
     * @see JPADAO#getAnnotationsForCampaignAndTarget(Campaign,String,Integer)
     * @verifies return correct rows
     */
    @Test
    public void getAnnotationsForCampaignAndTarget_shouldReturnCorrectRows() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        Assert.assertNotNull(campaign);

        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, "PI_1", 1);
            Assert.assertEquals(1, annotations.size());
            Assert.assertEquals(Long.valueOf(1), annotations.get(0).getId());
        }
        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, "PI_1", null);
            Assert.assertEquals(1, annotations.size());
            Assert.assertEquals(Long.valueOf(3), annotations.get(0).getId());
        }
        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, "PI_2", 6);
            Assert.assertEquals(1, annotations.size());
            Assert.assertEquals(Long.valueOf(2), annotations.get(0).getId());
        }
    }

    /**
     * @see JPADAO#getAnnotationsForCampaignAndWork(Campaign,String)
     * @verifies return correct rows
     */
    @Test
    public void getAnnotationsForCampaignAndWork_shouldReturnCorrectRows() throws Exception {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
        Assert.assertNotNull(campaign);

        {
            List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, "PI_1");
            Assert.assertEquals(2, annotations.size());
            Assert.assertEquals(Long.valueOf(1), annotations.get(0).getId());
            Assert.assertEquals(Long.valueOf(3), annotations.get(1).getId());
        }
    }

    /**
     * @see JPADAO#getAnnotationsForUserId(Long)
     * @verifies return correct rows
     */
    @Test
    public void getAnnotationsForUserId_shouldReturnCorrectRows() throws Exception {
        List<CrowdsourcingAnnotation> result = DataManager.getInstance().getDao().getAnnotationsForUserId(1L, null, "id", true);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Long.valueOf(2), result.get(0).getId());
        Assert.assertEquals(Long.valueOf(1), result.get(1).getId());
    }

    /**
     * @see JPADAO#getAnnotationsForUserId(Long,Integer,String,boolean)
     * @verifies throw IllegalArgumentException if sortField unknown
     */
    @Test(expected = IllegalArgumentException.class)
    public void getAnnotationsForUserId_shouldThrowIllegalArgumentExceptionIfSortFieldUnknown() throws Exception {
        DataManager.getInstance().getDao().getAnnotationsForUserId(1L, null, "foo", false);
    }

    /**
     * @see JPADAO#getAnnotationCount(Map)
     * @verifies return correct count
     */
    @Test
    public void getAnnotationCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getDao().getAnnotationCount(null));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAnnotationCount(Collections.singletonMap("targetPI", "PI_1")));
    }

    /**
     * @see JPADAO#getAnnotations(int,int,String,boolean,Map)
     * @verifies return correct rows
     */
    @Test
    public void getAnnotations_shouldReturnCorrectRows() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getDao().getAnnotations(0, 10, null, false, null).size());
        Assert.assertEquals(2,
                DataManager.getInstance().getDao().getAnnotations(0, 10, null, false, Collections.singletonMap("targetPI", "PI_2")).size());
    }

    /**
     * @see JPADAO#getAnnotations(int,int,String,boolean,Map)
     * @verifies filter by campaign name correctly
     */
    @Test
    public void getAnnotations_shouldFilterByCampaignNameCorrectly() throws Exception {
        List<CrowdsourcingAnnotation> result =
                DataManager.getInstance().getDao().getAnnotations(0, 10, null, false, Collections.singletonMap("campaign", "english"));
        Assert.assertEquals(3, result.size());
    }

    @Test
    public void testGetAllGeoMaps() throws DAOException {
        List<GeoMap> maps = DataManager.getInstance().getDao().getAllGeoMaps();
        Assert.assertEquals(2, maps.size());
    }

    @Test
    public void testGetPagesUsingMap() throws DAOException {

        GeoMap map1 = DataManager.getInstance().getDao().getGeoMap(1l);
        GeoMap map2 = DataManager.getInstance().getDao().getGeoMap(2l);

        List<CMSPage> embedMap1 = DataManager.getInstance().getDao().getPagesUsingMap(map1);
        List<CMSPage> embedMap2 = DataManager.getInstance().getDao().getPagesUsingMap(map2);

        Assert.assertEquals(2, embedMap1.size());
        Assert.assertEquals(0, embedMap2.size());

    }

    @Test
    public void testUpdateTranslations() throws Exception {
        String newVal = "Kartenbeschreibung 2";
        GeoMap map1 = DataManager.getInstance().getDao().getGeoMap(1l);
        Assert.assertEquals("Kartenbeschreibung 1", map1.getDescription("de").getValue());
        map1.getDescription("de").setValue(newVal);
        Assert.assertEquals(newVal, map1.getDescription("de").getValue());

        DataManager.getInstance().getDao().updateGeoMap(map1);
        map1 = DataManager.getInstance().getDao().getAllGeoMaps().stream().filter(map -> map.getId() == 1l).findAny().orElse(null);
        Assert.assertEquals(newVal, map1.getDescription("de").getValue());
    }

    /**
     * @see JPADAO#deleteCampaignStatisticsForUser(User)
     * @verifies remove user from creators and reviewers lists correctly
     */
    @Test
    public void deleteCampaignStatisticsForUser_shouldRemoveUserFromCreatorsAndReviewersListsCorrectly() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            Assert.assertNotNull(campaign);
            Assert.assertNotNull(campaign.getStatistics().get("PI_1"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_2"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_3"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_4"));
            Assert.assertTrue(campaign.getStatistics().get("PI_1").getAnnotators().contains(user));
            Assert.assertTrue(campaign.getStatistics().get("PI_2").getAnnotators().contains(user));
            Assert.assertTrue(campaign.getStatistics().get("PI_3").getReviewers().contains(user));
            Assert.assertTrue(campaign.getStatistics().get("PI_4").getReviewers().contains(user));
        }

        int rows = DataManager.getInstance().getDao().deleteCampaignStatisticsForUser(user);
        Assert.assertEquals(6, rows);
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            Assert.assertNotNull(campaign);
            Assert.assertNotNull(campaign.getStatistics().get("PI_1"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_2"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_3"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_4"));
            // User should no longer be among the reviewers
            Assert.assertFalse(campaign.getStatistics().get("PI_1").getAnnotators().contains(user));
            Assert.assertFalse(campaign.getStatistics().get("PI_2").getAnnotators().contains(user));
            Assert.assertFalse(campaign.getStatistics().get("PI_3").getReviewers().contains(user));
            Assert.assertFalse(campaign.getStatistics().get("PI_4").getReviewers().contains(user));
        }
    }

    /**
     * @see JPADAO#changeCampaignStatisticContributors(User,User)
     * @verifies replace user in creators and reviewers lists correctly
     */
    @Test
    public void changeCampaignStatisticContributors_shouldReplaceUserInCreatorsAndReviewersListsCorrectly() throws Exception {
        User fromUser = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(fromUser);
        User toUser = DataManager.getInstance().getDao().getUser(3);
        Assert.assertNotNull(toUser);
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            Assert.assertNotNull(campaign);
            Assert.assertNotNull(campaign.getStatistics().get("PI_1"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_2"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_3"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_4"));
            Assert.assertTrue(campaign.getStatistics().get("PI_1").getAnnotators().contains(fromUser));
            Assert.assertTrue(campaign.getStatistics().get("PI_2").getAnnotators().contains(fromUser));
            Assert.assertTrue(campaign.getStatistics().get("PI_3").getReviewers().contains(fromUser));
            Assert.assertTrue(campaign.getStatistics().get("PI_4").getReviewers().contains(fromUser));
            Assert.assertFalse(campaign.getStatistics().get("PI_1").getAnnotators().contains(toUser));
            Assert.assertFalse(campaign.getStatistics().get("PI_2").getAnnotators().contains(toUser));
            Assert.assertFalse(campaign.getStatistics().get("PI_3").getReviewers().contains(toUser));
            Assert.assertFalse(campaign.getStatistics().get("PI_4").getReviewers().contains(toUser));
        }

        int rows = DataManager.getInstance().getDao().changeCampaignStatisticContributors(fromUser, toUser);
        Assert.assertEquals(6, rows);
        {
            Campaign campaign = DataManager.getInstance().getDao().getCampaign(1L);
            Assert.assertNotNull(campaign);
            Assert.assertNotNull(campaign.getStatistics().get("PI_1"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_2"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_3"));
            Assert.assertNotNull(campaign.getStatistics().get("PI_4"));
            // Only toUser should be among the reviewers
            Assert.assertFalse(campaign.getStatistics().get("PI_1").getAnnotators().contains(fromUser));
            Assert.assertFalse(campaign.getStatistics().get("PI_2").getAnnotators().contains(fromUser));
            Assert.assertFalse(campaign.getStatistics().get("PI_3").getReviewers().contains(fromUser));
            Assert.assertFalse(campaign.getStatistics().get("PI_4").getReviewers().contains(fromUser));
            Assert.assertTrue(campaign.getStatistics().get("PI_1").getAnnotators().contains(toUser));
            Assert.assertTrue(campaign.getStatistics().get("PI_2").getAnnotators().contains(toUser));
            Assert.assertTrue(campaign.getStatistics().get("PI_3").getReviewers().contains(toUser));
            Assert.assertTrue(campaign.getStatistics().get("PI_4").getReviewers().contains(toUser));
        }
    }

    /**
     * @see JPADAO#getUserByNickname(String)
     * @verifies return null if nickname empty
     */
    @Test
    public void getUserByNickname_shouldReturnNullIfNicknameEmpty() throws Exception {
        Assert.assertNull(DataManager.getInstance().getDao().getUserByNickname(""));
    }

    /**
     * @see JPADAO#getCountPagesUsingCategory(CMSCategory)
     * @verifies return correct value
     */
    @Test
    public void getCountPagesUsingCategory_shouldReturnCorrectValue() throws Exception {
        CMSCategory cat = DataManager.getInstance().getDao().getCategory(4L);
        Assert.assertNotNull(cat);
        Assert.assertEquals(2, DataManager.getInstance().getDao().getCountPagesUsingCategory(cat));
    }

    /**
     * @see JPADAO#getCountMediaItemsUsingCategory(CMSCategory)
     * @verifies return correct value
     */
    @Test
    public void getCountMediaItemsUsingCategory_shouldReturnCorrectValue() throws Exception {
        CMSCategory cat = DataManager.getInstance().getDao().getCategory(1L);
        Assert.assertNotNull(cat);
        Assert.assertEquals(3, DataManager.getInstance().getDao().getCountMediaItemsUsingCategory(cat));
    }

    /**
     * @see JPADAO#createFilterQuery(String,Map,Map)
     * @verifies build multikey filter query correctly
     */
    @Test
    public void createFilterQuery_shouldBuildMultikeyFilterQueryCorrectly() throws Exception {
        Map<String, String> filters = Collections.singletonMap("a-a_b-b_c-c_d-d", "bar");
        Map<String, String> params = new HashMap<>();

        Assert.assertEquals(
                "STATIC:query AND ( ( UPPER(a.a.a) LIKE :aabbccdd OR UPPER(a.b.b) LIKE :aabbccdd OR UPPER(a.c.c) LIKE :aabbccdd OR UPPER(a.d.d) LIKE :aabbccdd ) ",
                JPADAO.createFilterQuery("STATIC:query", filters, params));
    }

    @Test
    public void createFilterQuery_twoJoinedTables() throws Exception {
        Map<String, String> filters = Collections.singletonMap("b-B_c-C", "bar");
        Map<String, Object> params = new HashMap<>();

        String expectedFilterString = " LEFT JOIN a.b b LEFT JOIN a.c c WHERE (UPPER(b.B) LIKE :bBcC OR UPPER(c.C) LIKE :bBcC)";
        String filterString = JPADAO.createFilterQuery2("", filters, params);

        Assert.assertEquals(expectedFilterString, filterString);
        Assert.assertTrue(params.get("bBcC").equals("%BAR%"));
    }

    @Test
    public void createFilterQuery_joinedTableAndField() throws Exception {
        Map<String, String> filters = Collections.singletonMap("B_c-C", "bar");
        Map<String, Object> params = new HashMap<>();

        String expectedFilterString = " LEFT JOIN a.c b WHERE (UPPER(a.B) LIKE :BcC OR UPPER(b.C) LIKE :BcC)";
        String filterString = JPADAO.createFilterQuery2("", filters, params);

        Assert.assertEquals(expectedFilterString, filterString);
        Assert.assertTrue(params.get("BcC").equals("%BAR%"));
    }

    @Test
    public void testGetTermsOfUse() throws DAOException {
        TermsOfUse tou = DataManager.getInstance().getDao().getTermsOfUse();
        Assert.assertNotNull(tou);
    }

    @Test
    public void testSaveTermsOfUse() throws DAOException {
        TermsOfUse tou = new TermsOfUse();
        tou.setActive(true);
        DataManager.getInstance().getDao().saveTermsOfUse(tou);

        tou = DataManager.getInstance().getDao().getTermsOfUse();
        Assert.assertTrue(tou.isActive());
        tou.setTitle("en", "English Title");
        tou.setTitle("de", "German Title");
        tou.setDescription("en", "English description");
        tou.setDescription("de", "German description");

        DataManager.getInstance().getDao().saveTermsOfUse(tou);
        tou = DataManager.getInstance().getDao().getTermsOfUse();
        Assert.assertEquals("English Title", tou.getTitle("en").getValue());
        Assert.assertEquals("German Title", tou.getTitle("de").getValue());
        Assert.assertEquals("German description", tou.getDescription("de").getValue());
        Assert.assertEquals("English description", tou.getDescription("en").getValue());

    }

    @Test
    public void testResetUserAgreementsToTermsOfUse() throws DAOException {

        //initially noone has agreed
        List<User> users = DataManager.getInstance().getDao().getAllUsers(true);
        Assert.assertTrue(users.stream().allMatch(u -> !u.isAgreedToTermsOfUse()));

        //now all agree
        for (User user : users) {
            user.setAgreedToTermsOfUse(true);
            DataManager.getInstance().getDao().updateUser(user);
        }

        //now all should have agreed
        users = DataManager.getInstance().getDao().getAllUsers(true);
        Assert.assertTrue(users.stream().allMatch(u -> u.isAgreedToTermsOfUse()));

        //reset agreements
        DataManager.getInstance().getDao().resetUserAgreementsToTermsOfUse();

        //now noone has agreed again
        users = DataManager.getInstance().getDao().getAllUsers(true);
        Assert.assertTrue(users.stream().allMatch(u -> !u.isAgreedToTermsOfUse()));
    }

    /**
     * @see JPADAO#createCampaignsFilterQuery(String,Map,Map)
     * @verifies create query correctly
     */
    @Test
    public void createCampaignsFilterQuery_shouldCreateQueryCorrectly() throws Exception {
        Map<String, String> filters = new HashMap<>(1);
        filters.put("groupOwner", "1");
        Map<String, Object> params = new HashMap<>(1);
        Assert.assertEquals(
                " prefix WHERE a.userGroup.owner IN (SELECT g.owner FROM UserGroup g WHERE g.owner.id=:groupOwner)",
                JPADAO.createCampaignsFilterQuery("prefix", filters, params));
        Assert.assertEquals(1, params.size());
        Assert.assertEquals(1L, params.get("groupOwner"));
    }

    /**
     * @see JPADAO#createAnnotationsFilterQuery(String,Map,Map)
     * @verifies create query correctly
     */
    @Test
    public void createAnnotationsFilterQuery_shouldCreateQueryCorrectly() throws Exception {
        {
            // creator/reviewer and campaign name
            Map<String, String> filters = new HashMap<>(2);
            filters.put("creatorId_reviewerId", "1");
            filters.put("campaign", "geo");
            Map<String, Object> params = new HashMap<>(2);
            Assert.assertEquals(
                    " prefix WHERE (a.creatorId=:creatorIdreviewerId OR a.reviewerId=:creatorIdreviewerId) AND (a.generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN (SELECT t.owner FROM CampaignTranslation t WHERE t.tag='title' AND UPPER(t.value) LIKE :campaign)))",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
            Assert.assertEquals(2, params.size());
            Assert.assertEquals("%GEO%", params.get("campaign"));
            Assert.assertEquals(1L, params.get("creatorIdreviewerId"));
        }
        {
            // just creator/reviewer
            Map<String, String> filters = new HashMap<>(1);
            filters.put("creatorId_reviewerId", "1");
            Map<String, Object> params = new HashMap<>(1);
            Assert.assertEquals(" prefix WHERE (a.creatorId=:creatorIdreviewerId OR a.reviewerId=:creatorIdreviewerId)",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
        }
        {
            // just campaign name
            Map<String, String> filters = new HashMap<>(1);
            filters.put("campaign", "geo");
            Map<String, Object> params = new HashMap<>(1);
            Assert.assertEquals(
                    " prefix WHERE (a.generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN (SELECT t.owner FROM CampaignTranslation t WHERE t.tag='title' AND UPPER(t.value) LIKE :campaign)))",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
        }
        {
            // just campaign ID
            Map<String, String> filters = new HashMap<>(2);
            filters.put("generatorId", "1");
            Map<String, Object> params = new HashMap<>(2);
            Assert.assertEquals(
                    " prefix WHERE (a.generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN (SELECT c FROM Campaign c WHERE c.id=:generatorId)))",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
            Assert.assertEquals(1, params.size());
            Assert.assertEquals(1L, params.get("generatorId"));
        }
        {
            // campaign ID and record identifier
            Map<String, String> filters = new HashMap<>(2);
            filters.put("generatorId", "1");
            filters.put("targetPI_body", "ppn123");
            Map<String, Object> params = new HashMap<>(2);
            Assert.assertEquals(
                    " prefix WHERE (a.generatorId IN (SELECT q.id FROM Question q WHERE q.owner IN (SELECT c FROM Campaign c WHERE c.id=:generatorId))) AND (UPPER(a.targetPI) LIKE :targetPIbody OR UPPER(a.body) LIKE :targetPIbody)",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
            Assert.assertEquals(2, params.size());
            Assert.assertEquals(1L, params.get("generatorId"));
            Assert.assertEquals("%PPN123%", params.get("targetPIbody"));
        }

    }

    @Test
    public void testGenerateNullMotivationFilterQuery() {
        {
            // campaign ID and record identifier
            Map<String, String> filters = new HashMap<>(2);
            filters.put("motivation", "NULL");
            Map<String, Object> params = new HashMap<>(2);
            Assert.assertEquals(
                    " prefix WHERE (a.motivation IS NULL)",
                    JPADAO.createAnnotationsFilterQuery("prefix", filters, params));
            Assert.assertEquals(0, params.size());
        }
    }

    @Test
    public void testGetAllRecordNotes() throws DAOException {
        List<CMSRecordNote> notes = DataManager.getInstance().getDao().getAllRecordNotes();
        assertEquals(5, notes.size());

    }

    @Test
    public void testGetCMSRecordNotesPaginated() throws DAOException {
        List<CMSRecordNote> notesP1 = DataManager.getInstance().getDao().getRecordNotes(0, 2, null, false, null);
        assertEquals(2, notesP1.size());
        List<CMSRecordNote> notesP2 = DataManager.getInstance().getDao().getRecordNotes(4, 2, null, false, null);
        assertEquals(1, notesP2.size());
    }

    @Test
    public void testGetCMSRecordNote() throws DAOException {
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
    public void testAddCMSRecordNote() throws DAOException {

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
    public void testAddCMSMultiRecordNote() throws DAOException {

        CMSMultiRecordNote note = new CMSMultiRecordNote();
        note.setQuery("DC:dc3d");

        assertTrue(DataManager.getInstance().getDao().addRecordNote(note));
        assertNotNull(note.getId());
        CMSMultiRecordNote pNote = (CMSMultiRecordNote) DataManager.getInstance().getDao().getRecordNote(note.getId());
        assertNotNull(pNote);
        assertEquals("DC:dc3d", pNote.getQuery());
    }

    @Test
    public void testUpdateRecordNote() throws DAOException {

        CMSRecordNote note = DataManager.getInstance().getDao().getRecordNote(2l);
        note.getNoteTitle().setText(changed, Locale.GERMAN);
        note.getNoteText().setText(changed, Locale.GERMAN);

        DataManager.getInstance().getDao().updateRecordNote(note);

        CMSRecordNote pNote = DataManager.getInstance().getDao().getRecordNote(2l);
        assertEquals(changed, note.getNoteTitle().getText(Locale.GERMAN));
        assertEquals("Notes 2", note.getNoteTitle().getText(Locale.ENGLISH));
        assertEquals(changed, note.getNoteText().getText(Locale.GERMAN));
        assertFalse(note.getNoteText().getValue(Locale.ENGLISH).isPresent());
    }

    @Test
    public void testDeleteRecordNote() throws DAOException {
        assertEquals(5, DataManager.getInstance().getDao().getAllRecordNotes().size());
        CMSRecordNote note = DataManager.getInstance().getDao().getRecordNote(2l);
        DataManager.getInstance().getDao().deleteRecordNote(note);
        assertEquals(4, DataManager.getInstance().getDao().getAllRecordNotes().size());
        List<CMSRecordNote> remainingNotes = DataManager.getInstance().getDao().getAllRecordNotes();
        assertNull(DataManager.getInstance().getDao().getRecordNote(2l));

    }

    @Test
    public void testGetRecordNotesForPi() throws DAOException {
        assertEquals(2, DataManager.getInstance().getDao().getRecordNotesForPi("PI1", false).size());
        assertEquals(1, DataManager.getInstance().getDao().getRecordNotesForPi("PI1", true).size());
        assertEquals(0, DataManager.getInstance().getDao().getRecordNotesForPi("PI5", false).size());
    }

    @Test
    public void testGetAllMultiRecordNotes() throws DAOException {
        assertEquals(1, DataManager.getInstance().getDao().getAllMultiRecordNotes(true).size());
        assertEquals(2, DataManager.getInstance().getDao().getAllMultiRecordNotes(false).size());
    }

    @Test
    public void testGetAllSliders() throws DAOException {
        List<CMSSlider> sliders = DataManager.getInstance().getDao().getAllSliders();
        assertEquals(3, sliders.size());
        assertTrue(sliders.stream().anyMatch(sl -> sl.getName().equals("Query Slider")));
        assertTrue(sliders.stream().anyMatch(sl -> sl.getDescription().equals("Slider from collections")));
    }

    @Test
    public void testGetSlider() throws DAOException {
        CMSSlider slider = DataManager.getInstance().getDao().getSlider(1l);
        assertNotNull(slider);
    }

    @Test
    public void testAddSlider() throws DAOException {
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
    public void testUpdateSlider() throws DAOException {
        String name = "Test Slider";
        CMSSlider slider = DataManager.getInstance().getDao().getSlider(1l);
        assertNotEquals(name, slider.getName());

        slider.setName(name);
        assertNotEquals(name, DataManager.getInstance().getDao().getSlider(1l).getName());

        DataManager.getInstance().getDao().updateSlider(slider);
        assertEquals(name, DataManager.getInstance().getDao().getSlider(1l).getName());
    }

    @Test
    public void testDeleteSlider() throws DAOException {
        String name = "Test Slider";
        CMSSlider slider = new CMSSlider(SourceType.COLLECTIONS);
        slider.setName(name);
        assertTrue(DataManager.getInstance().getDao().addSlider(slider));
        assertNotNull(DataManager.getInstance().getDao().getSlider(slider.getId()));

        assertTrue(DataManager.getInstance().getDao().deleteSlider(slider));
        assertNull(DataManager.getInstance().getDao().getSlider(slider.getId()));

    }

    @Test
    public void testGetEmbeddingCmsPage() throws DAOException {
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
    public void getCommentsOfUser_shouldSortCorrectly() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
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
    public void testSynchronization() throws DAOException {

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
    public void getDownloadTicketCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getActiveDownloadTicketCount(Collections.emptyMap()));
        Assert.assertEquals(2,
                DataManager.getInstance().getDao().getActiveDownloadTicketCount(Collections.singletonMap("pi_email", "user1@example.com")));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getActiveDownloadTicketCount(Collections.singletonMap("pi_email", "PPN456")));
    }

    /**
     * @see JPADAO#getActiveDownloadTickets(int,int,String,boolean,Map)
     * @verifies filter rows correctly
     */
    @Test
    public void getActiveDownloadTickets_shouldFilterRowsCorrectly() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getActiveDownloadTickets(0, 10, null, false, null).size());
        Assert.assertEquals(0,
                DataManager.getInstance()
                        .getDao()
                        .getActiveDownloadTickets(0, 10, null, false, Collections.singletonMap("pi_email", "user2@example.com"))
                        .size());
        Assert.assertEquals(1,
                DataManager.getInstance()
                        .getDao()
                        .getActiveDownloadTickets(0, 10, null, false, Collections.singletonMap("pi_email", "PPN456"))
                        .size());
    }

    @Test
    public void test_deleteViewerMessagesBeforeDate() throws DAOException {
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
    public void getDownloadTicketRequests_shouldReturnTicketsThatHaveNeverBeenActivated() throws Exception {
        List<DownloadTicket> result = DataManager.getInstance().getDao().getDownloadTicketRequests();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(3), result.get(0).getId());
    }

    @Test
    public void test_persistRecurringTaskTrigger() throws Exception {
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
        assertEquals(triggered, updated.getLastTimeTriggered());

        dao.deleteRecurringTaskTrigger(trigger.getId());
        assertTrue(dao.getRecurringTaskTriggers().isEmpty());

    }

}