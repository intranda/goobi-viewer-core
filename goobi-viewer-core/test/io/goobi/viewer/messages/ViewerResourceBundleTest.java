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
package io.goobi.viewer.messages;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.messages.ViewerResourceBundle;

public class ViewerResourceBundleTest extends AbstractTest {

    /**
     * @see ViewerResourceBundle#replaceParameters(String,String[])
     * @verifies return null if msg is null
     */
    @Test
    public void replaceParameters_shouldReturnNullIfMsgIsNull() throws Exception {
        Assert.assertNull(ViewerResourceBundle.replaceParameters(null, "one", "two", "three"));
    }

    /**
     * @see ViewerResourceBundle#replaceParameters(String,String[])
     * @verifies replace parameters correctly
     */
    @Test
    public void replaceParameters_shouldReplaceParametersCorrectly() throws Exception {
        Assert.assertEquals("one two three", ViewerResourceBundle.replaceParameters("{0} {1} {2}", "one", "two", "three"));
    }
}