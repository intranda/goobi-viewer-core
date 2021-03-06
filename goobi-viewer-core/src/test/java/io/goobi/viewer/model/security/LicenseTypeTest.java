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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florian Alpers
 *
 */
public class LicenseTypeTest {

    private static final String CONDITION_QUERY_1 = "(DOCTYPE:Monograph AND isWork:true) -DC:privatecollection";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetProcessedConditions() {
        LicenseType type = new LicenseType();

        type.setConditions(CONDITION_QUERY_1);
        Assert.assertTrue("processed conditions are " + type.getProcessedConditions(), type.getProcessedConditions().equals(CONDITION_QUERY_1));
    }


    /**
     * @see LicenseType#getAvailablePrivileges(Set)
     * @verifies only return priv view ugc if ugc type
     */
    @Test
    public void getAvailablePrivileges_shouldOnlyReturnPrivViewUgcIfUgcType() throws Exception {
        LicenseType type = new LicenseType();
        type.ugcType = true;
        List<String> result = type.getAvailablePrivileges(Collections.emptySet());
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(IPrivilegeHolder.PRIV_VIEW_UGC, result.get(0));
    }
    @Test
    public void getAvailablePrivilegesHandleNonEmptyArgument() throws Exception {
        LicenseType type = new LicenseType();
        type.ugcType = true;
        Set<String> privileges = new HashSet<>(Arrays.asList(IPrivilegeHolder.PRIV_VIEW_UGC));
        List<String> result = type.getAvailablePrivileges(privileges);
        Assert.assertEquals(0, result.size());
    }


}
