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
package io.goobi.viewer.model.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.servlet.http.HttpSession;

class AccessConditionUtilsTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if required access conditions empty
     */
    @Test
    void checkAccessPermission_shouldReturnTrueIfRequiredAccessConditionsEmpty() throws Exception {
        Assertions.assertTrue(AccessConditionUtils.checkAccessPermission(new ArrayList<LicenseType>(), new HashSet<String>(),
                IPrivilegeHolder.PRIV_VIEW_IMAGES, null, null, Optional.empty(), null).isGranted());
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if ip range allows access
     */
    @Test
    void checkAccessPermission_shouldReturnTrueIfIpRangeAllowsAccess() throws Exception {
        Assertions.assertTrue(AccessConditionUtils.checkAccessPermission(DataManager.getInstance().getDao().getAllLicenseTypes(),
                new HashSet<>(Collections.singletonList("license type 3 name")), IPrivilegeHolder.PRIV_LIST, null, "127.0.0.1", Optional.empty(),
                null).isGranted());
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if required access conditions contain only open access
     */
    @Test
    void checkAccessPermission_shouldReturnTrueIfRequiredAccessConditionsContainOnlyOpenAccess() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>();
        LicenseType lt = new LicenseType();
        licenseTypes.add(lt);
        lt.setName("type1");
        lt = new LicenseType();
        licenseTypes.add(lt);
        lt.setName("type2");

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add(SolrConstants.OPEN_ACCESS_VALUE);
        Assertions
                .assertTrue(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_VIEW_IMAGES, null,
                        null, Optional.empty(), null).isGranted());

        recordAccessConditions.add("type1");
        recordAccessConditions.add("type2");
        Assertions
                .assertFalse(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_VIEW_IMAGES, null,
                        null, Optional.empty(), null).isGranted());
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if all license types allow privilege by default
     */
    @Test
    void checkAccessPermission_shouldReturnTrueIfAllLicenseTypesAllowPrivilegeByDefault() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>();
        LicenseType lt = new LicenseType();
        licenseTypes.add(lt);
        lt.setName("type1");
        lt.getPrivileges().add(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        lt = new LicenseType();
        licenseTypes.add(lt);
        lt.setName("type2");
        lt.getPrivileges().add(IPrivilegeHolder.PRIV_VIEW_IMAGES);

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("type1");
        recordAccessConditions.add("condition2");

        Assertions
                .assertTrue(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_VIEW_IMAGES, null,
                        null, Optional.empty(), null).isGranted());
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return false if not all license types allow privilege by default
     */
    @Test
    void checkAccessPermission_shouldReturnFalseIfNotAllLicenseTypesAllowPrivilegeByDefault() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>();
        LicenseType lt = new LicenseType();
        licenseTypes.add(lt);
        lt.setName("type1");
        lt.getPrivileges().add(IPrivilegeHolder.PRIV_VIEW_IMAGES);
        lt = new LicenseType();
        licenseTypes.add(lt);
        lt.setName("type2");

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("type1");
        recordAccessConditions.add("type2");

        Assertions
                .assertFalse(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_VIEW_IMAGES, null,
                        null, Optional.empty(), null).isGranted());
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if ip range allows access to all conditions
     */
    @Test
    void checkAccessPermission_shouldReturnTrueIfIpRangeAllowsAccessToAllConditions() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>();
        {
            // 'license type 1 name' doesn't allow anything by default
            LicenseType lt = new LicenseType();
            lt.setName("license type 1 name");
            licenseTypes.add(lt);
        }
        {
            // IP range has an explicit license for PRIV_LIST for 'license type 3 name'
            LicenseType lt = new LicenseType();
            lt.setName("license type 3 name");
            licenseTypes.add(lt);
        }

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("license type 3 name");
        Assertions.assertTrue(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
                "127.0.0.1", Optional.empty(), null).isGranted());

        // localhost always gets access now
        //        recordAccessConditions.add("license type 1 name");
        //        Assertions.assertFalse(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
        //                "127.0.0.1", null));
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies not return true if no ip range matches
     */
    @Test
    void checkAccessPermission_shouldNotReturnTrueIfNoIpRangeMatches() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>();
        {
            // 'license type 1 name' doesn't allow anything by default
            LicenseType lt = new LicenseType();
            lt.setName("license type 1 name");
            licenseTypes.add(lt);
        }

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("license type 1 name");
        Assertions.assertFalse(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
                "11.22.33.44", Optional.empty(), null).isGranted());
    }

    /**
     * @see SearchHelper#getRelevantLicenseTypesOnly(List,Set,String)
     * @verifies remove license types whose names do not match access conditions
     */
    @Test
    void getRelevantLicenseTypesOnly_shouldRemoveLicenseTypesWhoseNamesDoNotMatchAccessConditions() throws Exception {
        List<LicenseType> allLicenseTypes = new ArrayList<>();

        LicenseType lt = new LicenseType();
        allLicenseTypes.add(lt);
        lt.setName("type1");

        lt = new LicenseType();
        allLicenseTypes.add(lt);
        lt.setName("type2");

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("type2");

        List<LicenseType> result = AccessConditionUtils.getRelevantLicenseTypesOnly(allLicenseTypes, recordAccessConditions,
                "+" + SolrConstants.PI_TOPSTRUCT + ":PPN517154005");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("type2", result.get(0).getName());
    }

    /**
     * @see AccessConditionUtils#getRelevantLicenseTypesOnly(List,Set,String,Map)
     * @verifies not remove moving wall license types to open access if condition query excludes given pi
     */
    @Test
    void getRelevantLicenseTypesOnly_shouldNotRemoveMovingWallLicenseTypesToOpenAccessIfConditionQueryExcludesGivenPi() throws Exception {
        LicenseType lt = new LicenseType();
        lt.setName("type1");
        lt.setMovingWall(true);

        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":PPN517154005";
        List<LicenseType> result =
                AccessConditionUtils.getRelevantLicenseTypesOnly(Collections.singletonList(lt), Collections.singleton("type1"), query);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.get(0).isRestrictionsExpired(query));
        Assertions.assertEquals("type1", result.get(0).getName());
    }

    /**
     * @see SearchHelper#generateAccessCheckQuery(String,String)
     * @verifies use correct field name for AV files
     */
    @Test
    void generateAccessCheckQuery_shouldUseCorrectFieldNameForAVFiles() throws Exception {
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.tif");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME + ":\"00000001.tif\" "
                    + SolrConstants.FILENAME + "_TIFF:\"00000001.tif\")", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.webm");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.mp4");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.mp3");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.ogg");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
    }

    /**
     * @see SearchHelper#generateAccessCheckQuery(String,String)
     * @verifies use correct file name for text files
     */
    @Test
    void generateAccessCheckQuery_shouldUseCorrectFileNameForTextFiles() throws Exception {
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "alto/PPN123456789/00000001.txt");
            Assertions.assertEquals(
                    "+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME_FULLTEXT
                            + ":\"alto/PPN123456789/00000001.txt\" " + SolrConstants.FILENAME_FULLTEXT_SHORT + ":\"00000001.txt\")",
                    result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "alto/PPN123456789/00000001.xml");
            Assertions.assertEquals(
                    "+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME_ALTO
                            + ":\"alto/PPN123456789/00000001.xml\" " + SolrConstants.FILENAME_ALTO_SHORT + ":\"00000001.xml\")",
                    result);
        }
    }

    /**
     * @see AccessConditionUtils#generateAccessCheckQuery(String,String)
     * @verifies use correct file name for pdf files
     */
    @Test
    void generateAccessCheckQuery_shouldUseCorrectFileNameForPdfFiles() {
        String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "12345.pdf");
        Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"12345.pdf\"", result);
    }

    /**
     * @see AccessConditionUtils#generateAccessCheckQuery(String,String)
     * @verifies adapt basic alto file name
     */
    @Test
    void generateAccessCheckQuery_shouldAdaptBasicAltoFileName() throws Exception {
        String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.xml");
        Assertions.assertEquals(
                "+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME_ALTO
                        + ":*/PPN123456789/00000001.xml " + SolrConstants.FILENAME_ALTO_SHORT + ":\"00000001.xml\")",
                result);
    }

    /**
     * @see AccessConditionUtils#generateAccessCheckQuery(String,String)
     * @verifies escape file name for wildcard search correctly
     */
    @Test
    void generateAccessCheckQuery_shouldEscapeFileNameForWildcardSearchCorrectly() throws Exception {
        String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001 (1)");
        Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001\\ \\(1\\).*", result);
    }

    @Test
    void generateAccessCheckQuery_shouldUseFullNameForImageFormats() throws Exception {
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.tif");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME + ":\"00000001.tif\" "
                    + SolrConstants.FILENAME + "_TIFF:\"00000001.tif\")", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.jpeg");
            Assertions.assertEquals(
                    "+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME + ":\"00000001.jpeg\" " + SolrConstants.FILENAME
                            + "_JPEG:\"00000001.jpeg\")",
                    result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.png");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"00000001.png\"", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.jp2");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"00000001.jp2\"", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.jpg");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME + ":\"00000001.jpg\" "
                    + SolrConstants.FILENAME + "_JPEG:\"00000001.jpg\")", result);
        }
    }

    @Test
    void generateAccessCheckQuery_shouldUseFullNameFor3dObjectFormats() throws Exception {
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.gltf");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"00000001.gltf\"", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.glb");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"00000001.glb\"", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.obj");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"00000001.obj\"", result);
        }
    }

    @Test
    void generateAccessCheckQuery_shouldUseBaseNameForFormatlessFiles() throws Exception {
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "SM.1.6.001");
            Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":SM.1.6.001.*", result);
        }
    }

    /**
     * @see AccessConditionUtils#generateAccessCheckQuery(String,String)
     * @verifies work correctly with urls
     */
    @Test
    void generateAccessCheckQuery_shouldWorkCorrectlyWithUrls() throws Exception {
        String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "file:///opt/digiverso/viewer/cms_media/bild4.png");
        Assertions.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"bild4.png\"", result);
    }

    /**
     * @see AccessConditionUtils#getPdfDownloadQuotaForRecord(String)
     * @verifies throw RecordNotFoundException if record not found
     */
    @Test
    void getPdfDownloadQuotaForRecord_shouldThrowRecordNotFoundExceptionIfRecordNotFound() throws Exception {
        Assertions.assertThrows(RecordNotFoundException.class, () -> AccessConditionUtils.getPdfDownloadQuotaForRecord("notfound"));
    }

    /**
     * @see AccessConditionUtils#getPdfDownloadQuotaForRecord(String)
     * @verifies return 100 if record has no quota value
     */
    @Test
    void getPdfDownloadQuotaForRecord_shouldReturn100IfRecordHasNoQuotaValue() throws Exception {
        Assertions.assertEquals(100, AccessConditionUtils.getPdfDownloadQuotaForRecord("34115495_1940"));
    }

    /**
     * @see AccessConditionUtils#getPdfDownloadQuotaForRecord(String)
     * @verifies return 100 if record open access
     */
    @Test
    void getPdfDownloadQuotaForRecord_shouldReturn100IfRecordOpenAccess() throws Exception {
        Assertions.assertEquals(100, AccessConditionUtils.getPdfDownloadQuotaForRecord("PPN517154005"));
    }

    /**
     * @see AccessConditionUtils#isConcurrentViewsLimitEnabledForAnyAccessCondition(List)
     * @verifies return false if access conditions null or empty
     */
    @Test
    void isConcurrentViewsLimitEnabledForAnyAccessCondition_shouldReturnFalseIfAccessConditionsNullOrEmpty() throws Exception {
        Assertions.assertFalse(AccessConditionUtils.isConcurrentViewsLimitEnabledForAnyAccessCondition(null));
        Assertions.assertFalse(AccessConditionUtils.isConcurrentViewsLimitEnabledForAnyAccessCondition(Collections.emptyList()));
    }

    /**
     * @see AccessConditionUtils#isConcurrentViewsLimitEnabledForAnyAccessCondition(List)
     * @verifies return true if any license type has limit enabled
     */
    @Test
    void isConcurrentViewsLimitEnabledForAnyAccessCondition_shouldReturnTrueIfAnyLicenseTypeHasLimitEnabled() throws Exception {
        String[] licenseTypes = new String[] { "license type 1 name", "license type 4 name" };
        Assertions.assertTrue(AccessConditionUtils.isConcurrentViewsLimitEnabledForAnyAccessCondition(Arrays.asList(licenseTypes)));
    }

    @Test
    void test_getApplyingLicenses_byIp() throws DAOException {

        LicenseType licenseType = new LicenseType();

        IpRange ipRangeMatch = new IpRange();
        ipRangeMatch.setSubnetMask("192.168.0.10/32");

        IpRange ipRangeNoMatch = new IpRange();
        ipRangeNoMatch.setSubnetMask("172.168.0.11/32");

        License license = new License();
        license.setLicenseType(licenseType);
        license.setIpRange(ipRangeMatch);

        IDAO dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getLicenses(licenseType)).thenReturn(Arrays.asList(license));
        Mockito.when(dao.getAllIpRanges()).thenReturn(Arrays.asList(ipRangeMatch, ipRangeNoMatch));

        List<License> licenses = AccessConditionUtils.getApplyingLicenses(Optional.empty(), "192.168.0.10", licenseType, dao);
        assertFalse(licenses.isEmpty());
        assertEquals(license, licenses.get(0));

        license.setIpRange(ipRangeNoMatch);
        licenses = AccessConditionUtils.getApplyingLicenses(Optional.empty(), "192.168.0.10", licenseType, dao);
        assertTrue(licenses.isEmpty());
    }

    /**
     * @see AccessConditionUtils#addSessionPermission(String, Object, HttpSession)
     */
    @Test
    void testAddSessionPermission_invalidatedSession() {
        jakarta.servlet.http.HttpSession session = Mockito.mock(jakarta.servlet.http.HttpSession.class);
        // Simulate a session that has already been invalidated
        Mockito.doThrow(new IllegalStateException("setAttribute: Session [null] has already been invalidated"))
                .when(session).setAttribute(Mockito.anyString(), Mockito.any());

        // Must not throw; must return false gracefully
        assertFalse(AccessConditionUtils.addSessionPermission("PRIV_TEST", "value", session));
    }

    /**
     * @see AccessConditionUtils#addSessionPermission(String, Object, HttpSession)
     */
    @Test
    void testAddSessionPermission_nullSession() {
        assertFalse(AccessConditionUtils.addSessionPermission("PRIV_TEST", "value", null));
    }

    /**
     * @see AccessConditionUtils#retrieveUserFromContext(HttpSession)
     * @verifies return null for null session
     */
    @Test
    void retrieveUserFromContext_shouldReturnNullForNullSession() {
        Assertions.assertNull(AccessConditionUtils.retrieveUserFromContext(null));
    }

    /**
     * @see AccessConditionUtils#retrieveUserFromContext(HttpSession)
     * @verifies return user from standard userBean session attribute
     */
    @Test
    void retrieveUserFromContext_shouldReturnUserFromDirectSessionAttribute() {
        User user = new User();
        UserBean userBean = new UserBean();
        userBean.setUser(user);

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(session.getAttribute("userBean")).thenReturn(userBean);

        Assertions.assertEquals(user, AccessConditionUtils.retrieveUserFromContext(session));
    }

    /**
     * @see AccessConditionUtils#retrieveUserFromContext(HttpSession)
     * @verifies return null without session scan when CDI returns userBean with null user
     *
     * When CDI is active and returns a UserBean with null user (anonymous visitor), the method must
     * return null directly without falling through to the expensive O(N) session scan.
     */
    @Test
    void retrieveUserFromContext_shouldReturnNullWithoutSessionScanWhenCdiReturnsNullUser() {
        UserBean userBean = new UserBean(); // user field is null — anonymous visitor

        HttpSession session = Mockito.mock(HttpSession.class);

        try (MockedStatic<BeanUtils> mockedBeanUtils = Mockito.mockStatic(BeanUtils.class)) {
            mockedBeanUtils.when(BeanUtils::getUserBean).thenReturn(userBean);

            User result = AccessConditionUtils.retrieveUserFromContext(session);

            Assertions.assertNull(result);
            // The session must not be scanned — getAttributeNames() is the entry point of the scan
            Mockito.verify(session, Mockito.never()).getAttributeNames();
        }
    }

    /**
     * @see AccessConditionUtils#retrieveUserFromContext(HttpSession)
     * @verifies find user via session attribute scan when stored under non-standard key
     *
     * In CDI/Weld environments, session-scoped beans may be stored under a generated key rather than
     * the EL name "userBean". This test verifies that the fallback scan (findInstanceInSessionAttributes)
     * correctly locates the UserBean in that case.
     */
    @Test
    void retrieveUserFromContext_shouldReturnUserViaSessionScanWhenStoredUnderNonStandardKey() {
        User user = new User();
        UserBean userBean = new UserBean();
        userBean.setUser(user);

        HttpSession session = Mockito.mock(HttpSession.class);
        // Standard key returns nothing — simulates CDI storing the bean under a generated key
        Mockito.when(session.getAttribute("userBean")).thenReturn(null);
        Mockito.when(session.getAttributeNames()).thenReturn(Collections.enumeration(List.of("weld_generated_key")));
        Mockito.when(session.getAttribute("weld_generated_key")).thenReturn(userBean);

        Assertions.assertEquals(user, AccessConditionUtils.retrieveUserFromContext(session));
    }

    // --- fetchAccessibleFileNames ---

    /**
     * @see AccessConditionUtils#fetchAccessibleFileNames(String,String,String,HttpServletRequest)
     * @verifies return empty list for blank pi
     */
    @Test
    void fetchAccessibleFileNames_shouldReturnEmptyListForBlankPi() throws Exception {
        Assertions.assertTrue(
                AccessConditionUtils.fetchAccessibleFileNames("", SolrConstants.FILENAME_ALTO,
                        IPrivilegeHolder.PRIV_VIEW_FULLTEXT, null).isEmpty());
        Assertions.assertTrue(
                AccessConditionUtils.fetchAccessibleFileNames("   ", SolrConstants.FILENAME_ALTO,
                        IPrivilegeHolder.PRIV_VIEW_FULLTEXT, null).isEmpty());
    }

    /**
     * @see AccessConditionUtils#fetchAccessibleFileNames(String,String,String,HttpServletRequest)
     * @verifies return empty list for null pi
     */
    @Test
    void fetchAccessibleFileNames_shouldReturnEmptyListForNullPi() throws Exception {
        Assertions.assertTrue(
                AccessConditionUtils.fetchAccessibleFileNames(null, SolrConstants.FILENAME_ALTO,
                        IPrivilegeHolder.PRIV_VIEW_FULLTEXT, null).isEmpty());
    }

    /**
     * @see AccessConditionUtils#fetchAccessibleFileNames(String,String,String,HttpServletRequest)
     * @verifies return filenames for open access record
     */
    @Test
    void fetchAccessibleFileNames_shouldReturnFilenamesForOpenAccessRecord() throws Exception {
        List<String> result = AccessConditionUtils.fetchAccessibleFileNames(
                "306653648_1892", SolrConstants.FILENAME_ALTO,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT, null);
        Assertions.assertFalse(result.isEmpty());
    }

    /**
     * @see AccessConditionUtils#fetchAccessibleFileNames(String,String,String,HttpServletRequest)
     * @verifies return empty list for record with no files indexed
     */
    @Test
    void fetchAccessibleFileNames_shouldReturnEmptyListForRecordWithNoFilesIndexed() throws Exception {
        // PPN517154005 (PI_KLEIUNIV) is indexed without FILENAME_ALTO, so result must be empty
        List<String> result = AccessConditionUtils.fetchAccessibleFileNames(
                PI_KLEIUNIV, SolrConstants.FILENAME_ALTO,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT, null);
        Assertions.assertTrue(result.isEmpty());
    }

    /**
     * @see AccessConditionUtils#fetchAccessibleFileNames(String,String,String,HttpServletRequest)
     * @verifies return empty list for restricted record when anonymous
     */
    @Test
    void fetchAccessibleFileNames_shouldReturnEmptyListForRestrictedRecordAnonymous() throws Exception {
        // Confirm the test Solr has FILENAME_ALTO docs for this PI; skip if data is missing
        // to distinguish "no documents found" from "access denied"
        SolrDocumentList docs = DataManager.getInstance().getSearchIndex()
                .search("+PI_TOPSTRUCT:34115495_1940 +DOCTYPE:PAGE +FILENAME_ALTO:[* TO *]",
                        10, null, Arrays.asList("FILENAME_ALTO", "ACCESSCONDITION"));
        org.junit.jupiter.api.Assumptions.assumeTrue(docs != null && !docs.isEmpty(),
                "Skipping: 34115495_1940 has no FILENAME_ALTO pages in the test Solr");

        // 34115495_1940 has access condition "fulltext_locked" — anonymous access must be denied
        List<String> result = AccessConditionUtils.fetchAccessibleFileNames(
                "34115495_1940", SolrConstants.FILENAME_ALTO,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT, null);
        Assertions.assertTrue(result.isEmpty());
    }

    /**
     * @see AccessConditionUtils#fetchAccessibleFileNames(String,String,String,HttpServletRequest)
     * @verifies return bare filenames not full paths
     */
    @Test
    void fetchAccessibleFileNames_shouldReturnBareFilenamesNotFullPaths() throws Exception {
        List<String> result = AccessConditionUtils.fetchAccessibleFileNames(
                "306653648_1892", SolrConstants.FILENAME_ALTO,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT, null);
        Assertions.assertFalse(result.isEmpty());
        // Results must be ordered by Solr page ORDER — first file corresponds to page 1
        Assertions.assertEquals("00000001.xml", result.get(0),
                "First result should be page-1 file (ORDER=1)");
        for (String filename : result) {
            Assertions.assertFalse(filename.contains("/"),
                    "Expected bare filename without path separator, got: " + filename);
        }
    }

    /**
     * @see AccessConditionUtils#fetchAccessibleFileNames(String,String,String,HttpServletRequest)
     * @verifies work for fulltext field
     */
    @Test
    void fetchAccessibleFileNames_shouldWorkForFulltextField() throws Exception {
        // khi_escidoc_7101 is OPENACCESS with FILENAME_FULLTEXT entries in the test Solr
        List<String> result = AccessConditionUtils.fetchAccessibleFileNames(
                "khi_escidoc_7101", SolrConstants.FILENAME_FULLTEXT,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT, null);
        Assertions.assertFalse(result.isEmpty());
    }

    // --- fetchPagePermissions ---

    /**
     * @see AccessConditionUtils#fetchPagePermissions(String, jakarta.servlet.http.HttpServletRequest)
     * @verifies return EMPTY for blank pi
     */
    @Test
    void fetchPagePermissions_shouldReturnEmptyForBlankPi() {
        PagePermissions result = AccessConditionUtils.fetchPagePermissions("", null);
        assertTrue(result.isEmpty());
    }

    /**
     * @see AccessConditionUtils#fetchPagePermissions(String, jakarta.servlet.http.HttpServletRequest)
     * @verifies return EMPTY for null pi
     */
    @Test
    void fetchPagePermissions_shouldReturnEmptyForNullPi() {
        PagePermissions result = AccessConditionUtils.fetchPagePermissions(null, null);
        assertTrue(result.isEmpty());
    }

    /**
     * @see AccessConditionUtils#fetchPagePermissions(String, jakarta.servlet.http.HttpServletRequest)
     * @verifies return granted permissions for open access record
     */
    @Test
    void fetchPagePermissions_shouldReturnGrantedPermissionsForOpenAccessRecord() {
        // PPN517154005 is an OPENACCESS record in the test Solr index; page order 1 always exists
        PagePermissions result = AccessConditionUtils.fetchPagePermissions(PI_KLEIUNIV, null);
        assertFalse(result.isEmpty(), "Expected non-empty PagePermissions for open-access record " + PI_KLEIUNIV);
        assertTrue(result.isImageGranted(1),
                "Expected image access granted for page 1 of open-access record");
        assertTrue(result.isFulltextGranted(1),
                "Expected fulltext access granted for page 1 of open-access record");
        assertTrue(result.isPdfGranted(1),
                "Expected PDF access granted for page 1 of open-access record");
    }

    /**
     * @see AccessConditionUtils#fetchPagePermissions(String, jakarta.servlet.http.HttpServletRequest)
     * @verifies populate all six privilege maps for open access record
     */
    @Test
    void fetchPagePermissions_shouldPopulateAllSixPrivilegeMapsForOpenAccessRecord() throws Exception {
        PagePermissions result = AccessConditionUtils.fetchPagePermissions(PI_KLEIUNIV, null);
        assertFalse(result.isEmpty());
        assertTrue(result.isImageGranted(1));
        assertTrue(result.isThumbnailGranted(1));
        assertTrue(result.isZoomGranted(1));
        assertTrue(result.isDownloadGranted(1));
        assertTrue(result.isFulltextGranted(1));
        assertTrue(result.isPdfGranted(1));
    }

    /**
     * @see AccessConditionUtils#removePrivAttributesForPi(HttpSession, String)
     * @verifies remove only PRIV_ attributes of the given pi
     */
    @Test
    void removePrivAttributesForPi_shouldRemoveOnlyPrivAttributesOfTheGivenPi() {
        // Stub HttpSession that tracks attribute state in a local map; we mutate the map via the
        // setAttribute/removeAttribute mocks and enumerate it through getAttributeNames.
        java.util.Map<String, Object> store = new java.util.concurrent.ConcurrentHashMap<>();
        HttpSession session = mock(HttpSession.class);
        when(session.getAttributeNames())
                .thenAnswer(inv -> Collections.enumeration(new java.util.ArrayList<>(store.keySet())));
        doAnswer(inv -> {
            store.remove(inv.getArgument(0));
            return null;
        }).when(session).removeAttribute(anyString());

        // PRIV_ entries for OLD_PI — must all be removed. Covers all three key schemes.
        store.put("PRIV_VIEW_IMAGES_OLD_PI_page1.png", new java.util.HashMap<>());               // fileName scheme
        store.put("PRIV_DOWNLOAD_PDF_OLD_PI_page1.pdf", new java.util.HashMap<>());              // fileName scheme
        store.put("PRIV_VIEW_FULLTEXT_OLD_PI", new java.util.HashMap<>());                       // no fileName (suffix match)
        store.put("PRIV_DOWNLOAD_ORIGINAL_CONTENT_OLD_PI", new Object());                        // no fileName (suffix match)
        // PRIV_ entries for KEEP_PI — must remain.
        store.put("PRIV_VIEW_IMAGES_KEEP_PI_page1.png", new java.util.HashMap<>());
        store.put("PRIV_VIEW_FULLTEXT_KEEP_PI_page1.txt", new java.util.HashMap<>());
        store.put("PRIV_VIEW_FULLTEXT_KEEP_PI", new java.util.HashMap<>());
        // non-PRIV attributes must stay untouched.
        store.put("currentPi", "OLD_PI");
        store.put("user", new Object());

        AccessConditionUtils.removePrivAttributesForPi(session, "OLD_PI");

        assertFalse(store.containsKey("PRIV_VIEW_IMAGES_OLD_PI_page1.png"));
        assertFalse(store.containsKey("PRIV_DOWNLOAD_PDF_OLD_PI_page1.pdf"));
        assertFalse(store.containsKey("PRIV_VIEW_FULLTEXT_OLD_PI"));
        assertFalse(store.containsKey("PRIV_DOWNLOAD_ORIGINAL_CONTENT_OLD_PI"));
        assertTrue(store.containsKey("PRIV_VIEW_IMAGES_KEEP_PI_page1.png"));
        assertTrue(store.containsKey("PRIV_VIEW_FULLTEXT_KEEP_PI_page1.txt"));
        assertTrue(store.containsKey("PRIV_VIEW_FULLTEXT_KEEP_PI"));
        assertTrue(store.containsKey("currentPi"));
        assertTrue(store.containsKey("user"));
    }
}
