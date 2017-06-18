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
package de.intranda.digiverso.presentation.faces.validators;

import org.junit.Assert;
import org.junit.Test;

public class PIValidatorTest {

    /**
     * @see PIValidator#validatePi(String)
     * @verifies return false if pi contains illegal characters
     */
    @Test
    public void validatePi_shouldReturnFalseIfPiContainsIllegalCharacters() throws Exception {
        Assert.assertFalse(PIValidator.validatePi("PPN!"));
        Assert.assertFalse(PIValidator.validatePi("PPN?"));
        Assert.assertFalse(PIValidator.validatePi("PPN/"));
        Assert.assertFalse(PIValidator.validatePi("PPN\\"));
        Assert.assertFalse(PIValidator.validatePi("PPN:"));
    }

    /**
     * @see PIValidator#validatePi(String)
     * @verifies return false if pi empty, blank or null
     */
    @Test
    public void validatePi_shouldReturnFalseIfPiEmptyBlankOrNull() throws Exception {
        Assert.assertFalse(PIValidator.validatePi(null));
        Assert.assertFalse(PIValidator.validatePi(""));
        Assert.assertFalse(PIValidator.validatePi("    "));
    }

    /**
     * @see PIValidator#validatePi(String)
     * @verifies return true if pi good
     */
    @Test
    public void validatePi_shouldReturnTrueIfPiGood() throws Exception {
        Assert.assertTrue(PIValidator.validatePi("PPN123456789"));
    }
}