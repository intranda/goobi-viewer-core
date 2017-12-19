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

import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.model.security.License;

public class LicenseTest {

    /**
     * @see License#isValid()
     * @verifies return correct value
     */
    @Test
    public void isValid_shouldReturnCorrectValue() throws Exception {
        License lic = new License();
        Assert.assertTrue(lic.isValid());
        {
            // Start date before now: true
            Calendar cal = Calendar.getInstance();
            cal.set(1970, 01, 01);
            lic.setStart(cal.getTime());
            Assert.assertTrue(lic.isValid());
        }
        {
            // End date before now: false
            Calendar cal = Calendar.getInstance();
            cal.set(2000, 01, 01);
            lic.setEnd(cal.getTime());
            Assert.assertFalse(lic.isValid());
        }
        {
            // End date after now: true
            Calendar cal = Calendar.getInstance();
            cal.set(2270, 01, 01);
            lic.setEnd(cal.getTime());
            Assert.assertTrue(lic.isValid());
        }
        {
            // Start date after now: false
            Calendar cal = Calendar.getInstance();
            cal.set(2269, 01, 01);
            lic.setStart(cal.getTime());
            Assert.assertFalse(lic.isValid());
        }
    }
}