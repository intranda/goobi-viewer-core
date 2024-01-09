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
package io.goobi.viewer.model.security.user;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.security.License;

public class LicenseTest {

    /**
     * @see License#isValid()
     * @verifies return correct value
     */
    @Test
    void isValid_shouldReturnCorrectValue() throws Exception {
        License lic = new License();
        Assertions.assertTrue(lic.isValid());
        {
            // Start date before now: true
            lic.setStart(LocalDateTime.of(1970, 1, 1, 0, 0));
            Assertions.assertTrue(lic.isValid());
        }
        {
            // End date before now: false
            lic.setEnd(LocalDateTime.of(2000, 1, 1, 0, 0));
            Assertions.assertFalse(lic.isValid());
        }
        {
            // End date after now: true
            lic.setEnd(LocalDateTime.of(2270, 1, 1, 0, 0));
            Assertions.assertTrue(lic.isValid());
        }
        {
            // Start date after now: false
            lic.setStart(LocalDateTime.of(2269, 1, 1, 0, 0));
            Assertions.assertFalse(lic.isValid());
        }
    }
}
