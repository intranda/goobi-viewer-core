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

class PasswordValidatorTest {

    /**
     * @see PasswordValidator#validatePassword(String)
     * @verifies return true if password good
     */
    @Test
    void validatePassword_shouldReturnTrueIfPasswordGood() throws Exception {
        Assertions.assertTrue(PasswordValidator.validatePassword("12345678")); // 8 chars
        Assertions.assertTrue(PasswordValidator.validatePassword("ÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄ")); // 72B (UTF-8)
        Assertions.assertTrue(PasswordValidator.validatePassword("very long secure password, very hard to guess or brüte förce")); // 62B (UTF-8)
    }

    /**
     * @see PasswordValidator#validatePassword(String)
     * @verifies return false if password empty
     */
    @Test
    void validatePassword_shouldReturnTrueIfPasswordEmpty() throws Exception {
        Assertions.assertTrue(PasswordValidator.validatePassword(""));
    }

    /**
     * @see PasswordValidator#validatePassword(String)
     * @verifies return false if password blank
     */
    @Test
    void validatePassword_shouldReturnFalseIfPasswordBlank() throws Exception {
        Assertions.assertFalse(PasswordValidator.validatePassword("   "));
    }

    /**
     * @see PasswordValidator#validatePassword(String)
     * @verifies return false if password too short
     */
    @Test
    void validatePassword_shouldReturnFalseIfPasswordTooShort() throws Exception {
        Assertions.assertFalse(PasswordValidator.validatePassword("1234567"));
    }

    /**
     * @see PasswordValidator#validatePassword(String)
     * @verifies return false if password too long
     */
    @Test
    void validatePassword_shouldReturnFalseIfPasswordTooLong() throws Exception {
        Assertions.assertFalse(PasswordValidator.validatePassword("eagle fury ominous booger respected quantum table greedy river what baggage stood")); // 81B (UTF-8)
        Assertions.assertFalse(PasswordValidator.validatePassword("ÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄÄ")); // 74B (UTF-8)
        Assertions.assertFalse(PasswordValidator.validatePassword("♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠♠")); // 75B (UTF-8)
    }
}
