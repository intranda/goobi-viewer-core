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

import io.goobi.viewer.AbstractSolrEnabledTest;

class PIInSolrValidatorTest extends AbstractSolrEnabledTest {

    /**
     * @see PIInSolrValidator#validatePi(String)
     * @verifies true if pi exists
     */
    @Test
    void validatePi_shouldReturnTrueIfPiExists() throws Exception {
        Assertions.assertTrue(PIInSolrValidator.validatePi(PI_KLEIUNIV));
    }

    /**
     * @see PIInSolrValidator#validatePi(String)
     * @verifies return false if pi does not exist
     */
    @Test
    void validatePi_shouldReturnFalseIfPiDoesNotExist() throws Exception {
        Assertions.assertFalse(PIInSolrValidator.validatePi("NOWAY"));
    }

    /**
     * @see PIInSolrValidator#validatePi(String)
     * @verifies return false if pi contains illegal characters
     */
    @Test
    void validatePi_shouldReturnFalseIfPiContainsIllegalCharacters() throws Exception {
        Assertions.assertFalse(PIInSolrValidator.validatePi("PPN!"));
        Assertions.assertFalse(PIInSolrValidator.validatePi("PPN?"));
        Assertions.assertFalse(PIInSolrValidator.validatePi("PPN/"));
        Assertions.assertFalse(PIInSolrValidator.validatePi("PPN\\"));
        Assertions.assertFalse(PIInSolrValidator.validatePi("PPN:"));
        Assertions.assertFalse(PIInSolrValidator.validatePi("Août 2025"));
    }
}
