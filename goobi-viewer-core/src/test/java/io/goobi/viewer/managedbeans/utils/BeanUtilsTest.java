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
package io.goobi.viewer.managedbeans.utils;

import org.junit.Assert;
import org.junit.Test;

public class BeanUtilsTest {

    /**
     * @see BeanUtils#escapeCriticalUrlChracters(String)
     * @verifies replace characters correctly
     */
    @Test
    public void escapeCriticalUrlChracters_shouldReplaceCharactersCorrectly() throws Exception {
        Assert.assertEquals("U002BAU002FU005CU007CU003FZ", BeanUtils.escapeCriticalUrlChracters("+A/\\|?Z"));
        Assert.assertEquals("U007C", BeanUtils.escapeCriticalUrlChracters("%7C"));
    }

    /**
     * @see BeanUtils#unescapeCriticalUrlChracters(String)
     * @verifies replace characters correctly
     */
    @Test
    public void unescapeCriticalUrlChracters_shouldReplaceCharactersCorrectly() throws Exception {
        Assert.assertEquals("+A/\\|?Z", BeanUtils.unescapeCriticalUrlChracters("U002BAU002FU005CU007CU003FZ"));
    }
}