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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class UserTokenTest {

    /**
     * @see UserToken#isExpired()
     * @verifies return true when expiration date is in the past
     */
    @Test
    void isExpired_shouldReturnTrueWhenExpirationDateIsInThePast() {
        UserToken token = new UserToken();
        token.setExpirationDate(LocalDateTime.now().minusSeconds(1));
        assertTrue(token.isExpired());
    }

    /**
     * @see UserToken#isExpired()
     * @verifies return false when expiration date is in the future
     */
    @Test
    void isExpired_shouldReturnFalseWhenExpirationDateIsInTheFuture() {
        UserToken token = new UserToken();
        token.setExpirationDate(LocalDateTime.now().plusDays(1));
        assertFalse(token.isExpired());
    }

    /**
     * @see UserToken#isExpired()
     * @verifies return false when expiration date is null
     */
    @Test
    void isExpired_shouldReturnFalseWhenExpirationDateIsNull() {
        UserToken token = new UserToken();
        token.setExpirationDate(null);
        assertFalse(token.isExpired());
    }
}
