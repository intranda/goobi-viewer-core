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
package io.goobi.viewer.model.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.user.User;

public class AccessConditionUtilsTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if required access conditions empty
     */
    @Test
    public void checkAccessPermission_shouldReturnTrueIfRequiredAccessConditionsEmpty() throws Exception {
        Assert.assertTrue(AccessConditionUtils.checkAccessPermission(new ArrayList<LicenseType>(), new HashSet<String>(),
                IPrivilegeHolder.PRIV_VIEW_IMAGES, null, null, null));
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if ip range allows access
     */
    @Test
    public void checkAccessPermission_shouldReturnTrueIfIpRangeAllowsAccess() throws Exception {
        Assert.assertTrue(AccessConditionUtils.checkAccessPermission(DataManager.getInstance().getDao().getAllLicenseTypes(),
                new HashSet<>(Collections.singletonList("license type 3 name")), IPrivilegeHolder.PRIV_LIST, null, "127.0.0.1", null));
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if required access conditions contain only open access
     */
    @Test
    public void checkAccessPermission_shouldReturnTrueIfRequiredAccessConditionsContainOnlyOpenAccess() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>();
        LicenseType lt = new LicenseType();
        licenseTypes.add(lt);
        lt.setName("type1");
        lt = new LicenseType();
        licenseTypes.add(lt);
        lt.setName("type2");

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add(SolrConstants.OPEN_ACCESS_VALUE);
        Assert.assertTrue(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_VIEW_IMAGES, null,
                null, null));

        recordAccessConditions.add("type1");
        recordAccessConditions.add("type2");
        Assert.assertFalse(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_VIEW_IMAGES, null,
                null, null));
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if all license types allow privilege by default
     */
    @Test
    public void checkAccessPermission_shouldReturnTrueIfAllLicenseTypesAllowPrivilegeByDefault() throws Exception {
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

        Assert.assertTrue(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_VIEW_IMAGES, null,
                null, null));
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return false if not all license types allow privilege by default
     */
    @Test
    public void checkAccessPermission_shouldReturnFalseIfNotAllLicenseTypesAllowPrivilegeByDefault() throws Exception {
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

        Assert.assertFalse(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_VIEW_IMAGES, null,
                null, null));
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies return true if ip range allows access to all conditions
     */
    @Test
    public void checkAccessPermission_shouldReturnTrueIfIpRangeAllowsAccessToAllConditions() throws Exception {
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
        Assert.assertTrue(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
                "127.0.0.1", null));

        // localhost always gets access now
        //        recordAccessConditions.add("license type 1 name");
        //        Assert.assertFalse(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
        //                "127.0.0.1", null));
    }

    /**
     * @see SearchHelper#checkAccessPermission(List,Set,String,User,String,String)
     * @verifies not return true if no ip range matches
     */
    @Test
    public void checkAccessPermission_shouldNotReturnTrueIfNoIpRangeMatches() throws Exception {
        List<LicenseType> licenseTypes = new ArrayList<>();
        {
            // 'license type 1 name' doesn't allow anything by default
            LicenseType lt = new LicenseType();
            lt.setName("license type 1 name");
            licenseTypes.add(lt);
        }

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("license type 1 name");
        Assert.assertFalse(AccessConditionUtils.checkAccessPermission(licenseTypes, recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
                "11.22.33.44", null));
    }

    /**
     * @see SearchHelper#getRelevantLicenseTypesOnly(List,Set,String)
     * @verifies remove license types whose names do not match access conditions
     */
    @Test
    public void getRelevantLicenseTypesOnly_shouldRemoveLicenseTypesWhoseNamesDoNotMatchAccessConditions() throws Exception {
        List<LicenseType> allLicenseTypes = new ArrayList<>();

        LicenseType lt = new LicenseType();
        allLicenseTypes.add(lt);
        lt.setName("type1");

        lt = new LicenseType();
        allLicenseTypes.add(lt);
        lt.setName("type2");

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("type2");

        Map<String, List<LicenseType>> ret = AccessConditionUtils.getRelevantLicenseTypesOnly(allLicenseTypes, recordAccessConditions,
                "+" + SolrConstants.PI_TOPSTRUCT + ":PPN517154005", Collections.singletonMap("", Boolean.FALSE));
        Assert.assertNotNull(ret);
        Assert.assertNotNull(ret.get(""));
        Assert.assertEquals(1, ret.get("").size());
        Assert.assertEquals("type2", ret.get("").get(0).getName());
    }

    /**
     * @see SearchHelper#getRelevantLicenseTypesOnly(List,Set,String)
     * @verifies remove license types whose condition query excludes the given pi
     */
    @Test
    public void getRelevantLicenseTypesOnly_shouldRemoveLicenseTypesWhoseConditionQueryExcludesTheGivenPi() throws Exception {
        List<LicenseType> allLicenseTypes = new ArrayList<>();

        LicenseType lt = new LicenseType();
        allLicenseTypes.add(lt);
        lt.setName("type1");
        lt.setConditions("+" + SolrConstants.PI_TOPSTRUCT + ":unknownidentifier");

        lt = new LicenseType();
        allLicenseTypes.add(lt);
        lt.setName("type2");
        lt.setConditions("+" + SolrConstants.PI_TOPSTRUCT + ":PPN517154005");

        lt = new LicenseType();
        allLicenseTypes.add(lt);
        lt.setName("type3");

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("type1");
        recordAccessConditions.add("type2");
        recordAccessConditions.add("type3");

        Map<String, List<LicenseType>> ret = AccessConditionUtils.getRelevantLicenseTypesOnly(allLicenseTypes, recordAccessConditions,
                "+" + SolrConstants.PI_TOPSTRUCT + ":PPN517154005", Collections.singletonMap("", Boolean.FALSE));
        Assert.assertNotNull(ret);
        Assert.assertNotNull(ret.get(""));
        Assert.assertEquals(2, ret.get("").size());
        Assert.assertEquals("type2", ret.get("").get(0).getName());
        Assert.assertEquals("type3", ret.get("").get(1).getName());
    }

    /**
     * @see AccessConditionUtils#getRelevantLicenseTypesOnly(List,Set,String,Map)
     * @verifies not remove moving wall license types to open access if condition query excludes given pi
     */
    @Test
    public void getRelevantLicenseTypesOnly_shouldNotRemoveWallLicenseTypesToOpenAccessIfConditionQueryExcludesGivenPi() throws Exception {
        List<LicenseType> allLicenseTypes = new ArrayList<>();

        LicenseType lt = new LicenseType();
        allLicenseTypes.add(lt);
        lt.setName("type1");
        lt.setMovingWall(true);
        lt.setConditions("+" + SolrConstants.PI_TOPSTRUCT + ":unknownidentifier");

        Set<String> recordAccessConditions = new HashSet<>();
        recordAccessConditions.add("type1");

        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":PPN517154005";
        Map<String, List<LicenseType>> ret = AccessConditionUtils.getRelevantLicenseTypesOnly(allLicenseTypes, recordAccessConditions,
                query, Collections.singletonMap("", Boolean.FALSE));
        Assert.assertNotNull(ret);
        Assert.assertNotNull(ret.get(""));
        Assert.assertEquals(1, ret.get("").size());
        Assert.assertTrue(ret.get("").get(0).isRestrictionsExpired(query));
        Assert.assertEquals("type1", ret.get("").get(0).getName());
    }

    /**
     * @see SearchHelper#generateAccessCheckQuery(String,String)
     * @verifies use correct field name for AV files
     */
    @Test
    public void generateAccessCheckQuery_shouldUseCorrectFieldNameForAVFiles() throws Exception {
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.tif");
            Assert.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"00000001.tif\"", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.webm");
            Assert.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.mp4");
            Assert.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.mp3");
            Assert.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001.ogg");
            Assert.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001.*", result);
        }
    }

    /**
     * @see SearchHelper#generateAccessCheckQuery(String,String)
     * @verifies use correct file name for text files
     */
    @Test
    public void generateAccessCheckQuery_shouldUseCorrectFileNameForTextFiles() throws Exception {
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "alto/PPN123456789/00000001.txt");
            Assert.assertEquals(
                    "+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME_FULLTEXT
                            + ":\"alto/PPN123456789/00000001.txt\" FILENAME_PLAIN:\"00000001.txt\")",
                    result);
        }
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "alto/PPN123456789/00000001.xml");
            Assert.assertEquals(
                    "+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +(" + SolrConstants.FILENAME_ALTO
                            + ":\"alto/PPN123456789/00000001.xml\" FILENAME_XML:\"00000001.xml\")",
                    result);
        }
    }

    /**
     * @see AccessConditionUtils#generateAccessCheckQuery(String,String)
     * @verifies escape file name for wildcard search correctly
     */
    @Test
    public void generateAccessCheckQuery_shouldEscapeFileNameForWildcardSearchCorrectly() throws Exception {
        {
            String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "00000001 (1)");
            Assert.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":00000001\\ \\(1\\).*", result);
        }
    }

    /**
     * @see AccessConditionUtils#generateAccessCheckQuery(String,String)
     * @verifies work correctly with urls
     */
    @Test
    public void generateAccessCheckQuery_shouldWorkCorrectlyWithUrls() throws Exception {
        String result = AccessConditionUtils.generateAccessCheckQuery("PPN123456789", "file:///opt/digiverso/viewer/cms_media/bild4.png");
        Assert.assertEquals("+" + SolrConstants.PI_TOPSTRUCT + ":PPN123456789 +" + SolrConstants.FILENAME + ":\"bild4.png\"", result);
    }

    /**
     * @see AccessConditionUtils#getPdfDownloadQuotaForRecord(String)
     * @verifies throw RecordNotFoundException if record not found
     */
    @Test(expected = RecordNotFoundException.class)
    public void getPdfDownloadQuotaForRecord_shouldThrowRecordNotFoundExceptionIfRecordNotFound() throws Exception {
        AccessConditionUtils.getPdfDownloadQuotaForRecord("notfound");
    }

    /**
     * @see AccessConditionUtils#getPdfDownloadQuotaForRecord(String)
     * @verifies return 100 if record has no quota value
     */
    @Test
    public void getPdfDownloadQuotaForRecord_shouldReturn100IfRecordHasNoQuotaValue() throws Exception {
        Assert.assertEquals(100, AccessConditionUtils.getPdfDownloadQuotaForRecord("51419376X"));
    }

    /**
     * @see AccessConditionUtils#getPdfDownloadQuotaForRecord(String)
     * @verifies return 100 if record open access
     */
    @Test
    public void getPdfDownloadQuotaForRecord_shouldReturn100IfRecordOpenAccess() throws Exception {
        Assert.assertEquals(100, AccessConditionUtils.getPdfDownloadQuotaForRecord("PPN517154005"));
    }

    /**
     * @see AccessConditionUtils#isConcurrentViewsLimitEnabledForAnyAccessCondition(List)
     * @verifies return false if access conditions null or empty
     */
    @Test
    public void isConcurrentViewsLimitEnabledForAnyAccessCondition_shouldReturnFalseIfAccessConditionsNullOrEmpty() throws Exception {
        Assert.assertFalse(AccessConditionUtils.isConcurrentViewsLimitEnabledForAnyAccessCondition(null));
        Assert.assertFalse(AccessConditionUtils.isConcurrentViewsLimitEnabledForAnyAccessCondition(Collections.emptyList()));
    }

    /**
     * @see AccessConditionUtils#isConcurrentViewsLimitEnabledForAnyAccessCondition(List)
     * @verifies return true if any license type has limit enabled
     */
    @Test
    public void isConcurrentViewsLimitEnabledForAnyAccessCondition_shouldReturnTrueIfAnyLicenseTypeHasLimitEnabled() throws Exception {
        String[] licenseTypes = new String[] { "license type 1 name", "license type 4 name" };
        Assert.assertTrue(AccessConditionUtils.isConcurrentViewsLimitEnabledForAnyAccessCondition(Arrays.asList(licenseTypes)));
    }
}