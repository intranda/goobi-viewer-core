
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
package io.goobi.viewer.controller;

import org.junit.Assert;
import org.junit.Test;

public class NetToolsTest {

    /**
     * @see NetTools#parseMultipleIpAddresses(String)
     * @verifies filter multiple addresses correctly
     */
    @Test
    public void parseMultipleIpAddresses_shouldFilterMultipleAddressesCorrectly() throws Exception {
        Assert.assertEquals("3.3.3.3", NetTools.parseMultipleIpAddresses("1.1.1.1, 2.2.2.2, 3.3.3.3"));
    }

    /**
     * @see NetTools#scrambleEmailAddress(String)
     * @verifies modify string correctly
     */
    @Test
    public void scrambleEmailAddress_shouldModifyStringCorrectly() throws Exception {
        Assert.assertEquals("foo*****com", NetTools.scrambleEmailAddress("foo@bar.com"));
    }

    /**
     * @see NetTools#scrambleIpAddress(String)
     * @verifies modify string correctly
     */
    @Test
    public void scrambleIpAddress_shouldModifyStringCorrectly() throws Exception {
        Assert.assertEquals("192.168.X.X", NetTools.scrambleIpAddress("192.168.0.1"));
    }
}