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
package io.goobi.viewer.faces.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class RelatedPIValidatorTest extends AbstractTest {

    /**
     * @see RelatedPIValidator#validatePi(String, io.goobi.viewer.model.security.user.User)
     * @verifies return false if pi empty, blank or null
     */
    @Test
    void validatePi_shouldReturnFalseIfPiEmptyBlankOrNull() throws Exception {
        // Empty, blank and null PI values are treated as optional (no error), so the method returns null
        Assertions.assertNull(RelatedPIValidator.validatePi(null, null));
        Assertions.assertNull(RelatedPIValidator.validatePi("", null));
    }

    /**
     * @see RelatedPIValidator#validatePi(String, io.goobi.viewer.model.security.user.User)
     * @verifies return false if user is null
     */
    @Test
    void validatePi_shouldReturnFalseIfUserIsNull() throws Exception {
        // Non-empty PI with null user should return an error key (user must be a CMS admin)
        Assertions.assertEquals("cms_page_related_pi_forbidden", RelatedPIValidator.validatePi("PPN123", null));
    }
}
