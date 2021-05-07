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
package io.goobi.viewer.dao.update;

import static org.junit.Assert.assertEquals;

import java.util.regex.Pattern;

import org.junit.Test;

import io.goobi.viewer.controller.StringTools;

/**
 * @author florian
 *
 */
public class CMSMediaUpdateTest {

    byte[] bytes = new byte[] {-84, -19, 0, 5, 115, 114, 0, 12, 106, 97, 118, 97, 46, 110, 101, 116, 46, 85, 82, 73, -84, 1, 120, 46, 67, -98, 73, -85, 3, 0, 1, 76, 0, 6, 115, 116, 114, 105, 110, 103, 116, 0, 18, 76, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114, 105, 110, 103, 59, 120, 112, 116, 0, 36, 104, 116, 116, 112, 58, 47, 47, 100, 105, 103, 105, 116, 97, 108, 46, 122, 108, 98, 46, 100, 101, 47, 118, 105, 101, 119, 101, 114, 47, 99, 109, 115, 47, 56, 57, 47, 120};
    String expected = "http://digital.zlb.de/viewer/cms/89/";
    
    @Test
    public void testReadBlob() {
        assertEquals(expected, new CMSMediaUpdate().parseUrl(bytes));
        
    }

}
