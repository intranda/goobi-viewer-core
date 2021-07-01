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
package io.goobi.viewer.model.security.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.solr.SolrConstants;

public class IpRangeTest extends AbstractDatabaseEnabledTest {

    /**
     * @see IpRange#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if condition is open access
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfConditionIsOpenAccess() throws Exception {
        IpRange ipRange = new IpRange();
        Assert.assertTrue(ipRange.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList(SolrConstants.OPEN_ACCESS_VALUE)), null,
                IPrivilegeHolder.PRIV_LIST, "PPN123"));
    }

    /**
     * @see IpRange#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if ip range has license
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfIpRangeHasLicense() throws Exception {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        Assert.assertNotNull(ipRange);
        List<String> licences = Arrays.asList(new String[] { "license type 3 name", "restriction on access" });
        Assert.assertTrue(ipRange.canSatisfyAllAccessConditions(new HashSet<>(licences), null, IPrivilegeHolder.PRIV_LIST, "PPN123"));
    }

    /**
     * @see IpRange#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return false if ip range has no license
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnFalseIfIpRangeHasNoLicense() throws Exception {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        Assert.assertNotNull(ipRange);
        Assert.assertFalse(ipRange.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList("license type 2 name")), null,
                IPrivilegeHolder.PRIV_LIST, "PPN123"));
    }

    /**
     * @see IpRange#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if condition list empty
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfConditionListEmpty() throws Exception {
        IpRange ipRange = new IpRange();
        Assert.assertTrue(ipRange.canSatisfyAllAccessConditions(new HashSet<String>(0), null, "restricted", "PPN123"));
    }
}