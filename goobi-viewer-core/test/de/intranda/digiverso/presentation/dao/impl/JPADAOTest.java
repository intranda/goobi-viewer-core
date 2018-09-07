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
package de.intranda.digiverso.presentation.dao.impl;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.annotation.Comment;
import de.intranda.digiverso.presentation.model.bookshelf.Bookshelf;
import de.intranda.digiverso.presentation.model.bookshelf.BookshelfItem;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem.CMSContentItemType;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItemMetadata;
import de.intranda.digiverso.presentation.model.cms.CMSNavigationItem;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSPageLanguageVersion;
import de.intranda.digiverso.presentation.model.cms.CMSPageLanguageVersion.CMSPageStatus;
import de.intranda.digiverso.presentation.model.cms.CMSStaticPage;
import de.intranda.digiverso.presentation.model.cms.CMSTemplateManager;
import de.intranda.digiverso.presentation.model.download.DownloadJob;
import de.intranda.digiverso.presentation.model.download.DownloadJob.JobStatus;
import de.intranda.digiverso.presentation.model.download.EPUBDownloadJob;
import de.intranda.digiverso.presentation.model.download.PDFDownloadJob;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPageUpdate;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.security.License;
import de.intranda.digiverso.presentation.model.security.LicenseType;
import de.intranda.digiverso.presentation.model.security.Role;
import de.intranda.digiverso.presentation.model.security.user.IpRange;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.security.user.UserGroup;
import de.intranda.digiverso.presentation.model.security.user.UserRole;

/**
 * JPADAO test suite using H2 DB.
 */
public class JPADAOTest extends AbstractDatabaseEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        File webContent = new File("WebContent/").getAbsoluteFile();
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
        CMSTemplateManager.getInstance(webContent.toURI().toString(), null);
    }

    // Users

    @Test
    public void getAllUsersTest() throws DAOException {
        List<User> users = DataManager.getInstance().getDao().getAllUsers(false);
        Assert.assertEquals(2, users.size());
    }

    @Test
    public void getUserByIdTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        Assert.assertEquals(2, user.getOpenIdAccounts().size());
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
        User user = DataManager.getInstance().getDao().getUserByNickname("nick 1");
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
        user.setUseGravatar(true);
        Date now = new Date();
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
        Assert.assertEquals(user.isUseGravatar(), user2.isUseGravatar());
        Assert.assertEquals(user.getLastLogin(), now);
        Assert.assertEquals(user.isActive(), user2.isActive());
        Assert.assertEquals(user.isSuspended(), user2.isSuspended());
        Assert.assertEquals(user.isSuperuser(), user2.isSuperuser());
    }

    @Test
    public void updateUserTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUsers(false).size());
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        user.setEmail("b@b.com");
        user.setPasswordHash("EFEFEF");
        user.setFirstName("first");
        user.setLastName("last");
        user.setNickName("unbanned_admin");
        user.setComments("no");
        user.setUseGravatar(true);
        Date now = new Date();
        user.setLastLogin(now);
        user.setActive(false);
        user.setSuspended(true);
        user.setSuperuser(false);
        DataManager.getInstance().getDao().updateUser(user);

        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUsers(false).size());

        User user2 = DataManager.getInstance().getDao().getUser(user.getId());
        Assert.assertNotNull(user2);
        Assert.assertEquals(user.getId(), user2.getId());
        Assert.assertEquals(user.getEmail(), user2.getEmail());
        Assert.assertEquals(user.getFirstName(), user2.getFirstName());
        Assert.assertEquals(user.getLastName(), user2.getLastName());
        Assert.assertEquals(user.getNickName(), user2.getNickName());
        Assert.assertEquals(user.getComments(), user2.getComments());
        Assert.assertEquals(user.isUseGravatar(), user2.isUseGravatar());
        Assert.assertEquals(user.getLastLogin(), user2.getLastLogin());
        Assert.assertEquals(user.isActive(), user2.isActive());
        Assert.assertEquals(user.isSuspended(), user2.isSuspended());
        Assert.assertEquals(user.isSuperuser(), user2.isSuperuser());
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
        Assert.assertEquals(2, userGroups.size());
    }

    @Test
    public void getAllUserGroupsForOwnerTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        Assert.assertEquals(2, DataManager.getInstance().getDao().getUserGroups(user).size());
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
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        Assert.assertEquals("user group 1 name", userGroup.getName());

        userGroup.setName("user group 1 new name");
        Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUserGroups().size());

        UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
        Assert.assertNotNull(userGroup2);
        Assert.assertEquals(userGroup.getId(), userGroup2.getId());
        Assert.assertEquals("user group 1 new name", userGroup2.getName());

    }

    @Test
    public void deleteUserGroupWithMembersTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        Assert.assertFalse(DataManager.getInstance().getDao().deleteUserGroup(userGroup));
        Assert.assertNotNull(DataManager.getInstance().getDao().getUserGroup(2));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUserGroups().size());
    }

    @Test
    public void deleteUserGroupWithoutMembersTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUserGroups().size());
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(2);
        Assert.assertNotNull(userGroup);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteUserGroup(userGroup));
        Assert.assertNull(DataManager.getInstance().getDao().getUserGroup(2));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllUserGroups().size());
    }

    // UserRoles (group memberships)

    @Test
    public void getAllUserRolesTest() throws DAOException {
        List<UserRole> userRoles = DataManager.getInstance().getDao().getAllUserRoles();
        Assert.assertEquals(1, userRoles.size());
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
        Assert.assertEquals(1, memberships.size());
    }

    @Test
    public void userGroupLicenseTest() throws DAOException {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        Assert.assertEquals(1, userGroup.getLicenses().size());
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
            Assert.assertEquals(1, userGroup2.getLicenses().size());
        }

        // Adding a new license should update the attached object with ID
        {
            License license = new License();
            license.setLicenseType(userGroup.getLicenses().get(0).getLicenseType());
            userGroup.addLicense(license);
            Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
            UserGroup userGroup2 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
            Assert.assertNotNull(userGroup2);
            Assert.assertEquals(2, userGroup2.getLicenses().size());
            Assert.assertNotNull(userGroup2.getLicenses().get(0).getId());
            Assert.assertNotNull(userGroup2.getLicenses().get(1).getId());
        }

        // Orphaned licenses should be deleted properly
        {
            userGroup.removeLicense(userGroup.getLicenses().get(0));
            Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
            UserGroup userGroup3 = DataManager.getInstance().getDao().getUserGroup(userGroup.getId());
            Assert.assertNotNull(userGroup3);
            Assert.assertEquals(1, userGroup3.getLicenses().size());
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

        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllUserRoles().size());
        Assert.assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, user, role).size());
        Assert.assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, user, null).size());
        Assert.assertEquals(0, DataManager.getInstance().getDao().getUserRoles(userGroup, null, null).size());

        UserRole userRole = new UserRole(userGroup, user, role);
        Assert.assertTrue(DataManager.getInstance().getDao().addUserRole(userRole));
        Assert.assertNotNull(userRole.getId());

        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllUserRoles().size());
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
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllUserRoles().size());
        UserRole userRole = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        Assert.assertNotNull(userRole);

        Role role1 = DataManager.getInstance().getDao().getRole(1);
        Assert.assertNotNull(role1);
        Role role2 = DataManager.getInstance().getDao().getRole(2);
        Assert.assertNotNull(role2);
        Assert.assertEquals(role1, userRole.getRole());
        userRole.setRole(role2);
        Assert.assertTrue(DataManager.getInstance().getDao().updateUserRole(userRole));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllUserRoles().size());

        UserRole userRole2 = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        Assert.assertNotNull(userRole2);
        Assert.assertEquals(userRole.getRole(), userRole2.getRole());
    }

    @Test
    public void deleteUserRoleTest() throws DAOException {
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllUserRoles().size());
        UserRole userRole = DataManager.getInstance().getDao().getAllUserRoles().get(0);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteUserRole(userRole));
        Assert.assertEquals(0, DataManager.getInstance().getDao().getAllUserRoles().size());
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
        Assert.assertEquals("127.0.0.1/24", ipRange.getSubnetMask());
        Assert.assertEquals("ip range 1 desc", ipRange.getDescription());
    }

    @Test
    public void getIpRangeByNameTest() throws DAOException {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange("localhost");
        Assert.assertNotNull(ipRange);
        Assert.assertEquals(Long.valueOf(1), ipRange.getId());
        Assert.assertEquals("localhost", ipRange.getName());
        Assert.assertEquals("127.0.0.1/24", ipRange.getSubnetMask());
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

    // Annotations

    /*
     * @Test public void getAllAnnotationsTest() { List<AnnotationElement> annotations =
     * DataManager.getInstance().getDao().getAllAnnotations(); Assert.assertEquals(3, annotations.size()); }
     * 
     * @Test public void getAnnotationByIdTest() { AnnotationElement annotation =
     * DataManager.getInstance().getDao().getAnnotation(1); Assert.assertNotNull(annotation);
     * Assert.assertEquals(Long.valueOf(1), annotation.getId()); Assert.assertEquals("PI 1", annotation.getPi());
     * Assert.assertEquals(1, annotation.getPage()); Assert.assertNotNull(annotation.getOwner());
     * Assert.assertEquals(Long.valueOf(1), annotation.getOwner().getId());
     * Assert.assertNotNull(annotation.getDateCreated()); Assert.assertNull(annotation.getDateUpdated());
     * Assert.assertEquals(AnnotationStatus.PUBLIC, annotation.getStatus()); Assert.assertEquals("annotation 1 text",
     * annotation.getText()); }
     * 
     * @Test public void getAnnotationsForPageTest() { List<AnnotationElement> annotations =
     * DataManager.getInstance().getDao().getAnnotationsForPage("PI 1", 1); Assert.assertEquals(2, annotations.size()); }
     * 
     * @Test public void addAnnotationTest() { Assert.assertEquals(3,
     * DataManager.getInstance().getDao().getAllAnnotations().size()); AnnotationElement annotation = new
     * AnnotationElement(); annotation.setPi("PI 2"); annotation.setPage(1); annotation.setText("new annotation text");
     * annotation.setOwner(DataManager.getInstance().getDao().getUser(1)); annotation.setStatus(AnnotationStatus.PRIVATE);
     * try { Assert.assertTrue(DataManager.getInstance().getDao().addAnnotation(annotation)); } catch (PresentationException
     * e) { e.printStackTrace(); } Assert.assertNotNull(annotation.getId()); Assert.assertEquals(4,
     * DataManager.getInstance().getDao().getAllAnnotations().size());
     * 
     * AnnotationElement annotation2 = DataManager.getInstance().getDao().getAnnotation(annotation.getId());
     * Assert.assertNotNull(annotation2); Assert.assertEquals(annotation.getPi(), annotation2.getPi());
     * Assert.assertEquals(annotation.getPage(), annotation2.getPage()); Assert.assertEquals(annotation.getText(),
     * annotation2.getText()); Assert.assertEquals(AnnotationStatus.PRIVATE, annotation2.getStatus());
     * Assert.assertEquals(annotation.getOwner(), annotation2.getOwner());
     * Assert.assertNotNull(annotation2.getDateCreated()); Assert.assertNull(annotation.getDateUpdated()); }
     * 
     * @Test public void updateAnnotationTest() { Assert.assertEquals(3,
     * DataManager.getInstance().getDao().getAllAnnotations().size()); AnnotationElement annotation =
     * DataManager.getInstance().getDao().getAnnotation(1); Assert.assertNotNull(annotation);
     * 
     * annotation.setText("new annotation 1 text"); annotation.setStatus(AnnotationStatus.PRIVATE);
     * annotation.setDateUpdated(new Date());
     * 
     * Assert.assertTrue(DataManager.getInstance().getDao().updateAnnotation(annotation)); Assert.assertEquals(3,
     * DataManager.getInstance().getDao().getAllAnnotations().size());
     * 
     * AnnotationElement annotation2 = DataManager.getInstance().getDao().getAnnotation(annotation.getId());
     * Assert.assertNotNull(annotation2); Assert.assertEquals(annotation.getPi(), annotation2.getPi());
     * Assert.assertEquals(annotation.getPage(), annotation2.getPage()); Assert.assertEquals(annotation.getText(),
     * annotation2.getText()); Assert.assertEquals(AnnotationStatus.PRIVATE, annotation2.getStatus());
     * Assert.assertEquals(annotation.getOwner(), annotation2.getOwner()); Assert.assertNotNull(annotation.getDateCreated());
     * Assert.assertNotNull(annotation.getDateUpdated()); }
     * 
     * @Test public void deleteAnnotationTest() { Assert.assertEquals(3,
     * DataManager.getInstance().getDao().getAllAnnotations().size()); AnnotationElement annotation =
     * DataManager.getInstance().getDao().getAnnotation(1); Assert.assertNotNull(annotation);
     * Assert.assertTrue(DataManager.getInstance().getDao().deleteAnnotation(annotation));
     * Assert.assertNull(DataManager.getInstance().getDao().getAnnotation(1)); Assert.assertEquals(2,
     * DataManager.getInstance().getDao().getAllAnnotations().size()); }
     */

    // Comments

    @Test
    public void getAllCommentsTest() throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getAllComments();
        Assert.assertEquals(4, comments.size());
    }

    /**
     * @see JPADAO#getCommentCount(Map)
     * @verifies return correct count
     */
    @Test
    public void getCommentCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(4L, DataManager.getInstance().getDao().getCommentCount(null));
    }

    /**
     * @see JPADAO#getCommentCount(Map)
     * @verifies filter correctly
     */
    @Test
    public void getCommentCount_shouldFilterCorrectly() throws Exception {
        Map<String, String> filters = new HashMap<>();
        filters.put("page", "1");
        Assert.assertEquals(3L, DataManager.getInstance().getDao().getCommentCount(filters));
    }

    @Test
    public void getCommentByIdTest() throws DAOException {
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        Assert.assertNotNull(comment);
        Assert.assertEquals(Long.valueOf(1), comment.getId());
        Assert.assertEquals("PI 1", comment.getPi());
        Assert.assertEquals(1, comment.getPage());
        Assert.assertNotNull(comment.getOwner());
        Assert.assertEquals(Long.valueOf(1), comment.getOwner().getId());
        Assert.assertEquals("comment 1 text", comment.getText());
        Assert.assertNotNull(comment.getDateCreated());
        Assert.assertNull(comment.getDateUpdated());
        Assert.assertNull(comment.getParent());
        Assert.assertEquals(1, comment.getChildren().size());

        Comment comment2 = DataManager.getInstance().getDao().getComment(2);
        Assert.assertEquals(comment, comment2.getParent());
        Assert.assertEquals(1, comment2.getChildren().size());
    }

    @Test
    public void getCommentsForPageTest() throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage("PI 1", 1, false);
        Assert.assertEquals(3, comments.size());
        comments = DataManager.getInstance().getDao().getCommentsForPage("PI 1", 1, true);
        Assert.assertEquals(1, comments.size());
    }

    @Test
    public void addCommentTest() throws DAOException {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = new Comment();
        comment.setPi("PI 2");
        comment.setPage(1);
        comment.setText("new comment text");
        comment.setOwner(DataManager.getInstance().getDao().getUser(1));
        Assert.assertTrue(DataManager.getInstance().getDao().addComment(comment));
        Assert.assertNotNull(comment.getId());
        Assert.assertEquals(5, DataManager.getInstance().getDao().getAllComments().size());

        Comment comment2 = DataManager.getInstance().getDao().getComment(comment.getId());
        Assert.assertNotNull(comment2);
        Assert.assertEquals(comment.getPi(), comment2.getPi());
        Assert.assertEquals(comment.getPage(), comment2.getPage());
        Assert.assertEquals(comment.getText(), comment2.getText());
        Assert.assertEquals(comment.getOwner(), comment2.getOwner());
        Assert.assertNotNull(comment2.getDateCreated());
        Assert.assertNull(comment2.getDateUpdated());
    }

    @Test
    public void updateCommentTest() throws DAOException {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        Assert.assertNotNull(comment);

        comment.setText("new comment 1 text");
        Date now = new Date();
        comment.setDateUpdated(now);

        Assert.assertTrue(DataManager.getInstance().getDao().updateComment(comment));
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());

        Comment comment2 = DataManager.getInstance().getDao().getComment(comment.getId());
        Assert.assertNotNull(comment2);
        Assert.assertEquals(comment.getPi(), comment2.getPi());
        Assert.assertEquals(comment.getPage(), comment2.getPage());
        Assert.assertEquals(comment.getText(), comment2.getText());
        Assert.assertEquals(comment.getOwner(), comment2.getOwner());
        Assert.assertEquals(now, comment2.getDateUpdated());
    }

    @Test
    public void deleteCommentTest() throws DAOException {
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllComments().size());
        Comment comment = DataManager.getInstance().getDao().getComment(1);
        Assert.assertNotNull(comment);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteComment(comment));
        Assert.assertNull(DataManager.getInstance().getDao().getComment(1));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllComments().size()); // Should cascade to child
                                                                                            // comments
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
        Search o = new Search(SearchHelper.SEARCH_TYPE_REGULAR, SearchHelper.SEARCH_FILTER_ALL);
        o.setOwner(DataManager.getInstance().getDao().getUser(1));
        o.setName("new search");
        o.setQuery("PI:*");
        o.setPage(1);
        o.setSortString("SORT_FIELD");
        o.setFacetString("DOCSTRCT:Other;;DC:newcol");
        o.setNewHitsNotification(true);
        Date now = new Date();
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
        Date now = new Date();
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
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllLicenseTypes().size());
    }

    /**
     * @see JPADAO#getOpenAccessLicenseTypes()
     * @verifies only return non open access license types
     */
    @Test
    public void getOpenAccessLicenseTypes_shouldOnlyReturnNonOpenAccessLicenseTypes() throws Exception {
        List<LicenseType> licenseTypes = DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes();
        Assert.assertEquals(2, licenseTypes.size());
        Assert.assertEquals(Long.valueOf(1), licenseTypes.get(0).getId());
        Assert.assertEquals(Long.valueOf(3), licenseTypes.get(1).getId());
    }

    @Test
    public void getLicenseTypeByIdTest() throws DAOException {
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(licenseType);
        Assert.assertEquals(Long.valueOf(1), licenseType.getId());
        Assert.assertEquals("license type 1 name", licenseType.getName());
        Assert.assertEquals("license type 1 desc", licenseType.getDescription());
        Assert.assertEquals("YEAR:[* TO 3000]", licenseType.getConditions());
        Assert.assertEquals(false, licenseType.isOpenAccess());
        Assert.assertEquals(1, licenseType.getPrivileges().size());
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

    @Test
    public void addLicenseTypeTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = new LicenseType();
        licenseType.setName("license type to add name");
        licenseType.setDescription("license type to add desc");
        licenseType.getPrivileges().add("license type to add priv 1");
        Assert.assertTrue(DataManager.getInstance().getDao().addLicenseType(licenseType));
        Assert.assertNotNull(licenseType.getId());
        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllLicenseTypes().size());

        LicenseType licenseType2 = DataManager.getInstance().getDao().getLicenseType(licenseType.getId());
        Assert.assertNotNull(licenseType2);
        Assert.assertEquals(licenseType.getName(), licenseType2.getName());
        Assert.assertEquals(licenseType.getDescription(), licenseType2.getDescription());
        Assert.assertEquals(1, licenseType2.getPrivileges().size());
        Assert.assertTrue(licenseType2.getPrivileges().contains("license type to add priv 1"));
    }

    @Test
    public void updateLicenseTypeTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(licenseType);
        Assert.assertEquals(1, licenseType.getPrivileges().size());

        licenseType.setName("license type 1 new name");
        licenseType.setDescription("license type 1 new desc");
        licenseType.getPrivileges().add("license type 1 priv 2");
        Assert.assertTrue(DataManager.getInstance().getDao().updateLicenseType(licenseType));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllLicenseTypes().size());

        LicenseType licenseType2 = DataManager.getInstance().getDao().getLicenseType(licenseType.getId());
        Assert.assertNotNull(licenseType2);
        Assert.assertEquals(licenseType.getId(), licenseType2.getId());
        Assert.assertEquals(licenseType.getName(), licenseType2.getName());
        Assert.assertEquals(licenseType.getDescription(), licenseType2.getDescription());
        Assert.assertEquals(2, licenseType.getPrivileges().size());
    }

    @Test
    public void deleteUsedLicenseTypeTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(licenseType);
        Assert.assertFalse(DataManager.getInstance().getDao().deleteLicenseType(licenseType));
        Assert.assertNotNull(DataManager.getInstance().getDao().getLicenseType(1));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllLicenseTypes().size());
    }

    @Test
    public void deleteUnusedLicenseTypeTest() throws DAOException {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllLicenseTypes().size());
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(2);
        Assert.assertNotNull(licenseType);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteLicenseType(licenseType));
        Assert.assertNull(DataManager.getInstance().getDao().getLicenseType(2));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllLicenseTypes().size());
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

    // Bookshelves

    @Test
    public void getAllBookshelvesTest() throws DAOException {
        List<Bookshelf> bookshelves = DataManager.getInstance().getDao().getAllBookshelves();
        Assert.assertEquals(2, bookshelves.size());
    }

    @Test
    public void getPublicBookshelvesTest() throws DAOException {
        List<Bookshelf> bookshelves = DataManager.getInstance().getDao().getPublicBookshelves();
        Assert.assertEquals(1, bookshelves.size());
        Assert.assertEquals(Long.valueOf(2), bookshelves.get(0).getId());
    }

    @Test
    public void getAllBookshelvesForUserTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        List<Bookshelf> bookshelves = DataManager.getInstance().getDao().getBookshelves(user);
        Assert.assertEquals(1, bookshelves.size());
        Assert.assertEquals(user, bookshelves.get(0).getOwner());
    }

    @Test
    public void getBookshelfByIdTest() throws DAOException {
        Bookshelf bookshelf = DataManager.getInstance().getDao().getBookshelf(1);
        Assert.assertNotNull(bookshelf);
        Assert.assertEquals(Long.valueOf(1), bookshelf.getId());
        Assert.assertNotNull(bookshelf.getOwner());
        Assert.assertEquals(Long.valueOf(1), bookshelf.getOwner().getId());
        Assert.assertEquals("bookshelf 1 name", bookshelf.getName());
        Assert.assertEquals("bookshelf 1 desc", bookshelf.getDescription());
        // for (BookshelfItem item : bookshelf.getItems()) {
        // System.out.println(item.getName());
        // }
        Assert.assertEquals(2, bookshelf.getItems().size());

    }

    @Test
    public void getBookshelfByNameTest() throws DAOException {
        Bookshelf bookshelf = DataManager.getInstance().getDao().getBookshelf("bookshelf 1 name");
        Assert.assertNotNull(bookshelf);
        Assert.assertEquals(Long.valueOf(1), bookshelf.getId());
        Assert.assertNotNull(bookshelf.getOwner());
        Assert.assertEquals(Long.valueOf(1), bookshelf.getOwner().getId());
        Assert.assertEquals("bookshelf 1 name", bookshelf.getName());
        Assert.assertEquals("bookshelf 1 desc", bookshelf.getDescription());
        Assert.assertEquals(2, bookshelf.getItems().size());
    }

    @Test
    public void addBookshelfTest() throws DAOException {
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);

        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setName("add bookshelf test");
        bookshelf.setOwner(user);
        bookshelf.setDescription("add bookshelf test desc");
        BookshelfItem item = new BookshelfItem("PPNTEST", "add bookshelf test item 1 main title", "add bookshelf test item 1 name");
        item.setDescription("add bookshelf test item 1 desc");
        bookshelf.addItem(item);
        Assert.assertTrue(DataManager.getInstance().getDao().addBookshelf(bookshelf));

        Bookshelf bookshelf2 = DataManager.getInstance().getDao().getBookshelf("add bookshelf test");
        Assert.assertNotNull(bookshelf2);
        Assert.assertNotNull(bookshelf2.getId());
        Assert.assertEquals(user, bookshelf2.getOwner());
        Assert.assertEquals(bookshelf.getName(), bookshelf2.getName());
        Assert.assertEquals(bookshelf.getDescription(), bookshelf2.getDescription());
        Assert.assertEquals(1, bookshelf2.getItems().size());
        BookshelfItem item2 = bookshelf2.getItems().get(0);
        Assert.assertEquals(bookshelf2, item2.getBookshelf());
        Assert.assertEquals("PPNTEST", item2.getPi());
        //        Assert.assertEquals("add bookshelf test item 1 main title", item2.getMainTitle());
        Assert.assertEquals("add bookshelf test item 1 name", item2.getName());
        Assert.assertEquals("add bookshelf test item 1 desc", item2.getDescription());
    }

    @Test
    public void updateBookshelfTest() throws DAOException {
        Bookshelf bookshelf = DataManager.getInstance().getDao().getBookshelf(1);
        Assert.assertNotNull(bookshelf);
        Assert.assertEquals(2, bookshelf.getItems().size());

        int numBookshelves = DataManager.getInstance().getDao().getAllBookshelves().size();

        BookshelfItem item = new BookshelfItem("PPNTEST", "addBookshelfItemTest item main title", "addBookshelfItemTest item name");
        item.setDescription("addBookshelfItemTest item desc");
        bookshelf.addItem(item);
        bookshelf.setName("bookshelf 1 new name");
        bookshelf.setDescription("bookshelf 1 new desc");
        Assert.assertTrue(DataManager.getInstance().getDao().updateBookshelf(bookshelf));
        //        Assert.assertNotNull(item.getId());

        int numBookshelves2 = DataManager.getInstance().getDao().getAllBookshelves().size();
        Assert.assertEquals(numBookshelves, numBookshelves2);

        Bookshelf bookshelf2 = DataManager.getInstance().getDao().getBookshelf(bookshelf.getId());
        Assert.assertNotNull(bookshelf2);
        Assert.assertEquals(bookshelf.getId(), bookshelf2.getId());
        Assert.assertEquals(bookshelf.getName(), bookshelf2.getName());
        Assert.assertEquals(bookshelf.getDescription(), bookshelf2.getDescription());
        Assert.assertEquals(3, bookshelf2.getItems().size());
    }

    @Test
    public void deleteBookshelfTest() throws DAOException {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllBookshelves().size());
        Bookshelf bookshelf = DataManager.getInstance().getDao().getBookshelf(1);
        Assert.assertNotNull(bookshelf);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteBookshelf(bookshelf));
        Assert.assertNull(DataManager.getInstance().getDao().getBookshelf(1));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllBookshelves().size());
    }

    @Test
    public void removeBookshelfItemTest() throws DAOException {
        Bookshelf bookshelf = DataManager.getInstance().getDao().getBookshelf(1);
        Assert.assertNotNull(bookshelf);
        Assert.assertEquals(2, bookshelf.getItems().size());
        bookshelf.removeItem(bookshelf.getItems().get(0));
        Assert.assertTrue(DataManager.getInstance().getDao().updateBookshelf(bookshelf));

        Bookshelf bookshelf2 = DataManager.getInstance().getDao().getBookshelf(1);
        Assert.assertNotNull(bookshelf2);
        Assert.assertEquals(1, bookshelf2.getItems().size());
    }

    /**
     * @see JPADAO#getComments(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getComments_shouldSortResultsCorrectly() throws Exception {
        List<Comment> ret = DataManager.getInstance().getDao().getComments(0, 2, "text", true, null);
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
        filterMap.put("pi", "pi 1");
        filterMap.put("text", "ment 2");
        List<Comment> ret = DataManager.getInstance().getDao().getComments(0, 2, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("comment 2 text", ret.get(0).getText());
    }

    /**
     * @see JPADAO#getUserGroups(int,int,String,boolean,Map)
     * @verifies sort results correctly
     */
    @Test
    public void getUserGroups_shouldSortResultsCorrectly() throws Exception {
        List<UserGroup> userGroups = DataManager.getInstance().getDao().getUserGroups(0, 2, "name", true, null);
        Assert.assertEquals(2, userGroups.size());
        Assert.assertEquals(Long.valueOf(2), userGroups.get(0).getId());
        Assert.assertEquals(Long.valueOf(1), userGroups.get(1).getId());
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
        filterMap.put("comments", "comments");
        List<User> ret = DataManager.getInstance().getDao().getUsers(0, 2, null, true, filterMap);
        Assert.assertEquals(1, ret.size());
        Assert.assertEquals("1@users.org", ret.get(0).getEmail());
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
        List<LicenseType> ret = DataManager.getInstance().getDao().getLicenseTypes(0, 2, "name", true, null);
        Assert.assertEquals(2, ret.size());
        Assert.assertEquals(Long.valueOf(3), ret.get(0).getId());
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
        Assert.assertEquals(3L, DataManager.getInstance().getDao().getCMSPageCount(null));
    }

    /**
     * @see JPADAO#getCMSPagesByClassification(String)
     * @verifies return all pages with given classification
     */
    @Test
    public void getCMSPagesByClassification_shouldReturnAllPagesWithGivenClassification() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getCMSPagesByClassification("news").size());
    }

    /**
     * @see JPADAO#getCMSPage(long)
     * @verifies return correct page
     */
    @Test
    public void getCMSPage_shouldReturnCorrectPage() throws Exception {
        CMSPage page = DataManager.getInstance().getDao().getCMSPage(1);
        Assert.assertNotNull(page);
        Assert.assertEquals(Long.valueOf(1), page.getId());
        Assert.assertEquals("template_simple", page.getTemplateId());
        Assert.assertNotNull(page.getDateCreated());

        Assert.assertEquals(2, page.getLanguageVersions().size());
        Assert.assertEquals("de", page.getLanguageVersions().get(0).getLanguage());
        Assert.assertEquals(CMSPageStatus.FINISHED, page.getLanguageVersions().get(0).getStatus());
        Assert.assertEquals("Titel 1", page.getLanguageVersions().get(0).getTitle());
        Assert.assertEquals("Menütitel 1", page.getLanguageVersions().get(0).getMenuTitle());
        boolean lv1found = false;
        for (CMSPageLanguageVersion lv : page.getLanguageVersions()) {
            if (lv.getId() != null && lv.getId() == 1) {
                lv1found = true;
                //				Assert.assertEquals(3, page.getLanguageVersions().get(1).getContentItems().size());
            }
        }
        Assert.assertTrue(lv1found);
        Assert.assertEquals("C1", page.getLanguageVersions().get(0).getContentItems().get(0).getItemId());
        Assert.assertEquals(CMSContentItemType.HTML, page.getLanguageVersions().get(0).getContentItems().get(0).getType());
    }

    /**
     * @see JPADAO#addCMSPage(CMSPage)
     * @verifies add page correctly
     */
    @Test
    public void addCMSPage_shouldAddPageCorrectly() throws Exception {
        CMSPage page = new CMSPage();
        page.setTemplateId("template_id");
        page.setDateCreated(new Date());
        page.setPublished(true);
        page.setUseDefaultSidebar(false);
        page.getClassifications().add("class");

        CMSPageLanguageVersion version = new CMSPageLanguageVersion();
        version.setLanguage("en");
        version.setOwnerPage(page);
        version.setTitle("title");
        version.setMenuTitle("menutitle");
        version.setStatus(CMSPageStatus.REVIEW_PENDING);
        page.getLanguageVersions().add(version);

        CMSContentItem item = new CMSContentItem();
        item.setOwnerPageLanguageVersion(version);
        item.setItemId("I1");
        item.setType(CMSContentItemType.SOLRQUERY);
        item.setMandatory(true);
        item.setElementsPerPage(3);
        item.setSolrQuery("PI:PPN517154005");
        item.setSolrSortFields("SORT_TITLE,DATECREATED");
        version.getContentItems().add(item);

        // TODO add sidebar elements

        Assert.assertTrue(DataManager.getInstance().getDao().addCMSPage(page));
        Assert.assertNotNull(page.getId());

        Assert.assertEquals(4, DataManager.getInstance().getDao().getAllCMSPages().size());
        CMSPage page2 = DataManager.getInstance().getDao().getCMSPage(page.getId());
        Assert.assertNotNull(page2);
        Assert.assertEquals(page.getTemplateId(), page2.getTemplateId());
        Assert.assertEquals(page.getDateCreated(), page2.getDateCreated());
        Assert.assertEquals(1, page2.getClassifications().size());
        Assert.assertEquals("class", page2.getClassifications().get(0));
        Assert.assertEquals(1, page.getLanguageVersions().size());
        Assert.assertEquals(version.getLanguage(), page.getLanguageVersions().get(0).getLanguage());
        Assert.assertEquals(version.getTitle(), page.getLanguageVersions().get(0).getTitle());
        Assert.assertEquals(version.getMenuTitle(), page.getLanguageVersions().get(0).getMenuTitle());
        Assert.assertEquals(version.getStatus(), page.getLanguageVersions().get(0).getStatus());
        Assert.assertEquals(1, page.getLanguageVersions().get(0).getContentItems().size());
        Assert.assertEquals(item.getItemId(), page.getLanguageVersions().get(0).getContentItems().get(0).getItemId());
        Assert.assertEquals(item.getType(), page.getLanguageVersions().get(0).getContentItems().get(0).getType());
        Assert.assertEquals(item.isMandatory(), page.getLanguageVersions().get(0).getContentItems().get(0).isMandatory());
        Assert.assertEquals(item.getElementsPerPage(), page.getLanguageVersions().get(0).getContentItems().get(0).getElementsPerPage());
        Assert.assertEquals(item.getSolrQuery(), page.getLanguageVersions().get(0).getContentItems().get(0).getSolrQuery());
        Assert.assertEquals(item.getSolrSortFields(), page.getLanguageVersions().get(0).getContentItems().get(0).getSolrSortFields());
    }

    /**
     * @see JPADAO#updateCMSPage(CMSPage)
     * @verifies update page correctly
     */
    @Test
    public void updateCMSPage_shouldUpdatePageCorrectly() throws Exception {
        CMSPage page = DataManager.getInstance().getDao().getCMSPage(1);
        Assert.assertNotNull(page);
        page.getLanguageVersion("de").setTitle("Deutscher Titel");
        page.getLanguageVersion("en").setTitle("English title");
        page.getLanguageVersion("fr").setTitle("Titre français");
        //        page.getLanguageVersions().remove(0);
        page.getClassifications().add("new class");
        Date now = new Date();
        page.setDateUpdated(now);
        Assert.assertTrue(DataManager.getInstance().getDao().updateCMSPage(page));

        CMSPage page2 = DataManager.getInstance().getDao().getCMSPage(1);
        Assert.assertNotNull(page2);
        Assert.assertEquals(page.getDateUpdated(), page2.getDateUpdated());
        Assert.assertEquals("Deutscher Titel", page2.getLanguageVersion("de").getTitle());
        Assert.assertEquals("English title", page2.getLanguageVersion("en").getTitle());
        Assert.assertEquals("Titre français", page2.getLanguageVersion("fr").getTitle());
        Assert.assertEquals(3, page2.getClassifications().size());
        Assert.assertEquals(now, page2.getDateUpdated());

        page.getLanguageVersion("fr").setTitle("");
        page.removeClassification("new class");
        Assert.assertTrue(DataManager.getInstance().getDao().updateCMSPage(page));
        Assert.assertEquals("", page.getLanguageVersion("fr").getTitle());
        Assert.assertEquals(2, page.getClassifications().size(), 0);
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

    /**
     * @see JPADAO#updateUserGroup(UserGroup)
     * @verifies set id on new license
     */
    @Test
    public void updateUserGroup_shouldSetIdOnNewLicense() throws Exception {
        UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(1);
        Assert.assertNotNull(userGroup);
        LicenseType licenseType = DataManager.getInstance().getDao().getLicenseType(1);
        Assert.assertNotNull(licenseType);
        License license = new License();
        license.setDescription("xxx");
        license.setLicenseType(licenseType);
        userGroup.addLicense(license);
        Assert.assertTrue(DataManager.getInstance().getDao().updateUserGroup(userGroup));
        boolean licenseFound = false;
        for (License l : userGroup.getLicenses()) {
            if ("xxx".equals(l.getDescription())) {
                licenseFound = true;
                Assert.assertNotNull(l.getId());
            }
        }
        Assert.assertTrue(licenseFound);
    }

    /**
     * @see JPADAO#getOverviewPage(long)
     * @verifies load overview page correctly
     */
    @Test
    public void getOverviewPage_shouldLoadOverviewPageCorrectly() throws Exception {
        OverviewPage op = DataManager.getInstance().getDao().getOverviewPage(1);
        Assert.assertNotNull(op);
        Assert.assertEquals(Long.valueOf(1), op.getId());
        Assert.assertEquals("PI 1", op.getPi());
        Assert.assertEquals("pub", op.getPublicationText());
        Assert.assertNotNull(op.getConfigXml());
    }

    /**
     * @see JPADAO#getOverviewPageForRecord(String)
     * @verifies load overview page correctly
     */
    @Test
    public void getOverviewPageForRecord_shouldLoadOverviewPageCorrectly() throws Exception {
        OverviewPage op = DataManager.getInstance().getDao().getOverviewPageForRecord("PI 1", null, null);
        Assert.assertNotNull(op);
        Assert.assertEquals(Long.valueOf(1), op.getId());
        Assert.assertEquals("PI 1", op.getPi());
        Assert.assertNotNull(op.getConfigXml());
    }

    /**
     * @see JPADAO#getOverviewPageForRecord(String,Date,Date)
     * @verifies filter by date range correctly
     */
    @Test
    public void getOverviewPageForRecord_shouldFilterByDateRangeCorrectly() throws Exception {
        Date fromDate = DateTools.parseDateFromString("2015-06-21");
        Date toDate = DateTools.parseDateFromString("2015-06-23");
        OverviewPage op = DataManager.getInstance().getDao().getOverviewPageForRecord("PI 1", fromDate, toDate);
        Assert.assertNotNull(op);
        Assert.assertEquals(Long.valueOf(1), op.getId());

        fromDate = DateTools.parseDateFromString("2015-06-22");
        toDate = DateTools.parseDateFromString("2015-06-22");
        op = DataManager.getInstance().getDao().getOverviewPageForRecord("PI 1", fromDate, toDate);
        Assert.assertNotNull(op);
        Assert.assertEquals(Long.valueOf(1), op.getId());

        fromDate = DateTools.parseDateFromString("2016-01-01");
        toDate = DateTools.parseDateFromString("2016-05-04");
        op = DataManager.getInstance().getDao().getOverviewPageForRecord("PI 1", fromDate, toDate);
        Assert.assertNull(op);
    }

    /**
     * @see JPADAO#addOverviewPage(OverviewPage)
     * @verifies add overview page correctly
     */
    @Test
    public void addOverviewPage_shouldAddOverviewPageCorrectly() throws Exception {
        OverviewPage op = new OverviewPage();
        op.setPi("PI 2");
        op.setConfigXml("<xml />");
        op.setPublicationText("tl;dr");
        Date now = new Date();
        op.setDateUpdated(now);

        Assert.assertTrue(DataManager.getInstance().getDao().addOverviewPage(op));
        OverviewPage op2 = DataManager.getInstance().getDao().getOverviewPage(op.getId());
        Assert.assertNotNull(op2);
        Assert.assertEquals(op.getId(), op2.getId());
        Assert.assertEquals(op.getPi(), op2.getPi());
        Assert.assertEquals(op.getConfigXml(), op2.getConfigXml());
        Assert.assertEquals(op.getPublicationText(), op2.getPublicationText());
        Assert.assertEquals(now, op2.getDateUpdated());
    }

    /**
     * @see JPADAO#deleteOverviewPage(OverviewPage)
     * @verifies delete overview page correctly
     */
    @Test
    public void deleteOverviewPage_shouldDeleteOverviewPageCorrectly() throws Exception {
        OverviewPage op = DataManager.getInstance().getDao().getOverviewPage(1);
        Assert.assertNotNull(op);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteOverviewPage(op));
        Assert.assertNull(DataManager.getInstance().getDao().getOverviewPage(1));
    }

    /**
     * @see JPADAO#updateOverviewPage(OverviewPage)
     * @verifies update overview page correctly
     */
    @Test
    public void updateOverviewPage_shouldUpdateOverviewPageCorrectly() throws Exception {
        OverviewPage op = DataManager.getInstance().getDao().getOverviewPage(1);
        Assert.assertNotNull(op);
        op.setConfigXml("new value");
        op.setPublicationText("new text");

        Assert.assertTrue(DataManager.getInstance().getDao().updateOverviewPage(op));
        OverviewPage op2 = DataManager.getInstance().getDao().getOverviewPage(op.getId());
        Assert.assertNotNull(op2);
        Assert.assertEquals(op.getId(), op2.getId());
        Assert.assertEquals(op.getPi(), op2.getPi());
        Assert.assertEquals(op.getConfigXml(), op2.getConfigXml());
        Assert.assertEquals(op.getPublicationText(), op2.getPublicationText());
    }

    /**
     * @see JPADAO#getOverviewPages(int,int,Date,Date)
     * @verifies return all overview pages
     */
    @Test
    public void getOverviewPages_shouldReturnAllOverviewPages() throws Exception {
        {
            List<OverviewPage> overviewPages = DataManager.getInstance().getDao().getOverviewPages(0, 10, null, null);
            Assert.assertNotNull(overviewPages);
            Assert.assertEquals(3, overviewPages.size());
            Assert.assertEquals("PI 1", overviewPages.get(0).getPi());
        }
        {
            Date fromDate = DateTools.parseDateFromString("2015-06-21");
            Date toDate = DateTools.parseDateFromString("2015-06-23");
            List<OverviewPage> overviewPages = DataManager.getInstance().getDao().getOverviewPages(0, 10, fromDate, null);
            Assert.assertNotNull(overviewPages);
            Assert.assertEquals(1, overviewPages.size());
            Assert.assertEquals("PI 1", overviewPages.get(0).getPi());
        }
        {
            Date fromDate = DateTools.parseDateFromString("2016-06-21");
            Date toDate = DateTools.parseDateFromString("2016-06-23");
            List<OverviewPage> overviewPages = DataManager.getInstance().getDao().getOverviewPages(0, 10, fromDate, null);
            Assert.assertNotNull(overviewPages);
            Assert.assertEquals(0, overviewPages.size());
        }
    }

    /**
     * @see JPADAO#getOverviewPages(int,int,Date,Date)
     * @verifies paginate results correctly
     */
    @Test
    public void getOverviewPages_shouldPaginateResultsCorrectly() throws Exception {
        {
            List<OverviewPage> overviewPages = DataManager.getInstance().getDao().getOverviewPages(0, 1, null, null);
            Assert.assertNotNull(overviewPages);
            Assert.assertEquals(1, overviewPages.size());
            Assert.assertEquals("PI 1", overviewPages.get(0).getPi());
        }
        {
            List<OverviewPage> overviewPages = DataManager.getInstance().getDao().getOverviewPages(1, 1, null, null);
            Assert.assertNotNull(overviewPages);
            Assert.assertEquals(1, overviewPages.size());
            Assert.assertEquals("PI 2", overviewPages.get(0).getPi());
        }
    }

    /**
     * @see JPADAO#getOverviewPageUpdatesForRecord(String)
     * @verifies return all updates for record
     */
    @Test
    public void getOverviewPageUpdatesForRecord_shouldReturnAllUpdatesForRecord() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getOverviewPageUpdatesForRecord("PI 1").size());
    }

    /**
     * @see JPADAO#getOverviewPageUpdatesForRecord(String)
     * @verifies sort updates by date descending
     */
    @Test
    public void getOverviewPageUpdatesForRecord_shouldSortUpdatesByDateDescending() throws Exception {
        List<OverviewPageUpdate> updates = DataManager.getInstance().getDao().getOverviewPageUpdatesForRecord("PI 1");
        Assert.assertEquals(3, DataManager.getInstance().getDao().getOverviewPageUpdatesForRecord("PI 1").size());
        Assert.assertEquals(Long.valueOf(3), updates.get(0).getId());
        Assert.assertEquals(Long.valueOf(2), updates.get(1).getId());
        Assert.assertEquals(Long.valueOf(1), updates.get(2).getId());
    }

    /**
     * @see JPADAO#isOverviewPageHasUpdates(String,Date,Date)
     * @verifies return status correctly
     */
    @Test
    public void isOverviewPageHasUpdates_shouldReturnStatusCorrectly() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getDao().isOverviewPageHasUpdates("PI 1", null, null));

        Date fromDate = DateTools.parseDateFromString("2015-06-21");
        Date toDate = DateTools.parseDateFromString("2015-06-23");
        Assert.assertTrue(DataManager.getInstance().getDao().isOverviewPageHasUpdates("PI 1", fromDate, toDate));

        fromDate = DateTools.parseDateFromString("2016-05-13");
        toDate = DateTools.parseDateFromString("2016-05-13");
        Assert.assertFalse(DataManager.getInstance().getDao().isOverviewPageHasUpdates("PI 1", fromDate, toDate));
    }

    /**
     * @see JPADAO#getOverviewPageUpdate(long)
     * @verifies load object correctly
     */
    @Test
    public void getOverviewPageUpdate_shouldLoadObjectCorrectly() throws Exception {
        OverviewPageUpdate update = DataManager.getInstance().getDao().getOverviewPageUpdate(3);
        Assert.assertNotNull(update);
        Assert.assertEquals(Long.valueOf(3), update.getId());
        Assert.assertEquals("PI 1", update.getPi());
        Assert.assertNotNull(update.getDateUpdated());
        Assert.assertNotNull(update.getUpdatedBy());
        Assert.assertEquals(Long.valueOf(1), update.getUpdatedBy().getId());
        Assert.assertEquals("<xml />", update.getConfig());
        Assert.assertFalse(update.isMetadataChanged());
        Assert.assertTrue(update.isDescriptionChanged());
        Assert.assertFalse(update.isPublicationTextChanged());
    }

    /**
     * @see JPADAO#addOverviewPageUpdate(OverviewPageUpdate)
     * @verifies add update correctly
     */
    @Test
    public void addOverviewPageUpdate_shouldAddUpdateCorrectly() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getOverviewPageUpdatesForRecord("PI 1").size());
        OverviewPageUpdate o = new OverviewPageUpdate();
        o.setPi("PI 1");
        Date now = new Date();
        o.setDateUpdated(now);
        o.setUpdatedBy(DataManager.getInstance().getDao().getUser(1));
        o.setConfig("config");
        o.setMetadataChanged(true);
        o.setDescriptionChanged(true);
        o.setPublicationTextChanged(true);
        Assert.assertTrue(DataManager.getInstance().getDao().addOverviewPageUpdate(o));
        Assert.assertNotNull(o.getId());
        Assert.assertEquals(4, DataManager.getInstance().getDao().getOverviewPageUpdatesForRecord("PI 1").size());

        OverviewPageUpdate o2 = DataManager.getInstance().getDao().getOverviewPageUpdate(o.getId());
        Assert.assertNotNull(o2);
        Assert.assertEquals(o.getPi(), o2.getPi());
        Assert.assertEquals(now, o2.getDateUpdated());
        Assert.assertEquals(o.getUpdatedBy(), o2.getUpdatedBy());
        Assert.assertEquals(o.getConfig(), o2.getConfig());
        Assert.assertEquals(o.isMetadataChanged(), o2.isMetadataChanged());
        Assert.assertEquals(o.isDescriptionChanged(), o2.isDescriptionChanged());
        Assert.assertEquals(o.isPublicationTextChanged(), o2.isPublicationTextChanged());
    }

    /**
     * @see JPADAO#deleteOverviewPageUpdate(OverviewPageUpdate)
     * @verifies delete update correctly
     */
    @Test
    public void deleteOverviewPageUpdate_shouldDeleteUpdateCorrectly() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getDao().getOverviewPageUpdatesForRecord("PI 1").size());
        OverviewPageUpdate o = DataManager.getInstance().getDao().getOverviewPageUpdate(1);
        Assert.assertNotNull(o);
        Assert.assertTrue(DataManager.getInstance().getDao().deleteOverviewPageUpdate(o));
        Assert.assertNull(DataManager.getInstance().getDao().getOverviewPageUpdate(1));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getOverviewPageUpdatesForRecord("PI 1").size());
    }

    /**
     * @see JPADAO#getUserCount()
     * @verifies return correct count
     */
    @Test
    public void getUserCount_shouldReturnCorrectCount() throws Exception {
        Assert.assertEquals(2L, DataManager.getInstance().getDao().getUserCount(null));
    }

    /**
     * @see JPADAO#getUserCount(Map)
     * @verifies filter correctly
     */
    @Test
    public void getUserCount_shouldFilterCorrectly() throws Exception {
        Map<String, String> filters = new HashMap<>();
        filters.put("firstName", "1");
        Assert.assertEquals(1L, DataManager.getInstance().getDao().getUserCount(filters));
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
        Assert.assertEquals(3L, DataManager.getInstance().getDao().getLicenseTypeCount(null));
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
        Assert.assertEquals(2L, DataManager.getInstance().getDao().getUserGroupCount(null));
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
            Assert.assertEquals("PI 1", job.getPi());
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
        DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByIdentifier("70eef211dc8a5889578f58d88eb50b8e");
        Assert.assertNotNull(job);
        Assert.assertEquals(PDFDownloadJob.class, job.getClass());
        Assert.assertEquals(Long.valueOf(1), job.getId());
        Assert.assertEquals("PI 1", job.getPi());
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
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByMetadata(PDFDownloadJob.TYPE, "PI 1", null);
            Assert.assertNotNull(job);
            Assert.assertEquals(PDFDownloadJob.class, job.getClass());
            Assert.assertEquals(Long.valueOf(1), job.getId());
        }
        {
            DownloadJob job = DataManager.getInstance().getDao().getDownloadJobByMetadata(EPUBDownloadJob.TYPE, "PI 1", "LOG_0001");
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
        Date now = new Date();
        PDFDownloadJob job = new PDFDownloadJob("PI 2", "LOG_0002", now, 3600);
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
        Date now = new Date();
        job.setLastRequested(now);
        job.setStatus(JobStatus.READY);
        job.getObservers().add("newobserver@example.com");

        Assert.assertTrue(DataManager.getInstance().getDao().updateDownloadJob(job));
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());

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

    @Test
    public void getCMSPagesCount_shouldReturnCorrectCount() throws Exception {
        long numPages = DataManager.getInstance().getDao().getCMSPageCount(Collections.emptyMap());
        Assert.assertEquals(3, numPages);
    }

    @Test
    public void getStaticPageForCMSPage_shouldReturnCorrectResult() throws Exception {
        List<CMSPage> cmsPages = DataManager.getInstance().getDao().getAllCMSPages();
        for (CMSPage page : cmsPages) {
            Optional<CMSStaticPage> staticPage = DataManager.getInstance().getDao().getStaticPageForCMSPage(page);
            if (page.getId().equals(1l)) {
                Assert.assertTrue(staticPage.isPresent());
                Assert.assertTrue(staticPage.get().getPageName().equals("index"));
                Assert.assertTrue(staticPage.get().getCmsPage().equals(page));
            } else {
                Assert.assertFalse(staticPage.isPresent());
            }
        }
    }
}
