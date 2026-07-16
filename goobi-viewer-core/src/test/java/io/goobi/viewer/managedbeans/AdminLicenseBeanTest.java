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
package io.goobi.viewer.managedbeans;

import java.util.List;

import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.LicenseType;

class AdminLicenseBeanTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see AdminLicenseBean#getGroupedLicenseTypeSelectItems()
     * @verifies return two select item groups with 1 core and 5 non core license types
     */
    @Test
    void getGroupedLicenseTypeSelectItems_shouldReturnTwoSelectItemGroupsWith1CoreAnd5NonCoreLicenseTypes() throws Exception {
        AdminLicenseBean bean = new AdminLicenseBean();
        bean.init();

        List<SelectItem> items = bean.getGroupedLicenseTypeSelectItems();
        Assertions.assertEquals(2, items.size());
        Assertions.assertEquals(1, ((SelectItemGroup) items.get(0)).getSelectItems().length);
        Assertions.assertEquals(5, ((SelectItemGroup) items.get(1)).getSelectItems().length);
    }

    /**
     * @see AdminLicenseBean#getNumRecordsWithAccessCondition(String)
     * @verifies count aggregated records rather than raw documents
     */
    @Test
    void getNumRecordsWithAccessCondition_shouldCountAggregatedRecordsRatherThanRawDocuments() throws Exception {
        AdminLicenseBean bean = new AdminLicenseBean();
        // Access condition "1907" is carried by several Solr documents that belong to fewer top-level records.
        // The count must reflect records (as the search link does), not the raw number of matching documents.
        long recordCount = bean.getNumRecordsWithAccessCondition("1907");
        long rawDocumentCount = DataManager.getInstance()
                .getSearchIndex()
                .getHitCount(SearchHelper.getQueryForAccessCondition("1907", false));
        Assertions.assertTrue(recordCount >= 1, "Expected at least one record, but was " + recordCount);
        Assertions.assertTrue(recordCount < rawDocumentCount,
                "Expected aggregated record count (" + recordCount + ") to be smaller than raw document count (" + rawDocumentCount + ")");
    }

    /**
     * @see AdminLicenseBean#getNumRecordsWithAccessCondition(String)
     * @verifies still count records whose access condition is only on a grouped metadata field
     */
    @Test
    void getNumRecordsWithAccessCondition_shouldStillCountRecordsWithMetadataOnlyAccessCondition() throws Exception {
        AdminLicenseBean bean = new AdminLicenseBean();
        // "RESTRICTED" is carried by grouped metadata (and event) documents; the aggregation join must still resolve
        // these to their top-level record via PI_TOPSTRUCT, otherwise metadata-only records would drop out (see #26795).
        Assertions.assertTrue(bean.getNumRecordsWithAccessCondition("RESTRICTED") >= 1);
    }

    /**
     * @see AdminLicenseBean#createsOverrideCycle(LicenseType, List)
     * @verifies detect reciprocal override cycle
     */
    @Test
    void createsOverrideCycle_shouldDetectReciprocalCycle() {
        LicenseType a = new LicenseType();
        a.setName("A");
        LicenseType b = new LicenseType();
        b.setName("B");
        a.getOverriddenLicenseTypes().add(b);
        b.getOverriddenLicenseTypes().add(a);
        Assertions.assertTrue(AdminLicenseBean.createsOverrideCycle(a, Arrays.asList(a, b)));
    }

    /**
     * @see AdminLicenseBean#createsOverrideCycle(LicenseType, List)
     * @verifies detect self override cycle
     */
    @Test
    void createsOverrideCycle_shouldDetectSelfCycle() {
        LicenseType a = new LicenseType();
        a.setName("A");
        a.getOverriddenLicenseTypes().add(a);
        Assertions.assertTrue(AdminLicenseBean.createsOverrideCycle(a, Arrays.asList(a)));
    }

    /**
     * @see AdminLicenseBean#createsOverrideCycle(LicenseType, List)
     * @verifies return false for acyclic overrides
     */
    @Test
    void createsOverrideCycle_shouldReturnFalseForAcyclicOverrides() {
        LicenseType a = new LicenseType();
        a.setName("A");
        LicenseType b = new LicenseType();
        b.setName("B");
        a.getOverriddenLicenseTypes().add(b);
        Assertions.assertFalse(AdminLicenseBean.createsOverrideCycle(a, Arrays.asList(a, b)));
    }
}
