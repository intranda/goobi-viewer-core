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
package de.intranda.digiverso.presentation.model.user;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseEnabledTest;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.security.user.IpRange;

public class IpRangeTest extends AbstractDatabaseEnabledTest {

    /**
    * @see IpRange#canSatisfyAllAccessConditions(Set,String,String)
    * @verifies return true if condition is open access
    */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfConditionIsOpenAccess() throws Exception {
        IpRange ipRange = new IpRange();
        Assert.assertTrue(ipRange.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList(SolrConstants.OPEN_ACCESS_VALUE)),
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
        Assert.assertTrue(ipRange.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList("license type 3 name")),
                IPrivilegeHolder.PRIV_LIST, "PPN123"));
    }

    /**
    * @see IpRange#canSatisfyAllAccessConditions(Set,String,String)
    * @verifies return false if ip range has no license
    */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnFalseIfIpRangeHasNoLicense() throws Exception {
        IpRange ipRange = DataManager.getInstance().getDao().getIpRange(1);
        Assert.assertNotNull(ipRange);
        Assert.assertFalse(ipRange.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList("license type 2 name")),
                IPrivilegeHolder.PRIV_LIST, "PPN123"));
    }

    /**
    * @see IpRange#canSatisfyAllAccessConditions(Set,String,String)
    * @verifies return true if condition list empty
    */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfConditionListEmpty() throws Exception {
        IpRange ipRange = new IpRange();
        Assert.assertTrue(ipRange.canSatisfyAllAccessConditions(new HashSet<String>(0),
                "restricted", "PPN123"));
    }
}