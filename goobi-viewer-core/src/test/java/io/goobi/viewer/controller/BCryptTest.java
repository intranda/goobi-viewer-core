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
package io.goobi.viewer.controller;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class BCryptTest extends AbstractTest {

    /**
     * @see BCrypt#hashpw(String,String)
     * @verifies hash password correctly
     */
    @Test
    void hashpw_shouldHashPasswordCorrectly() throws Exception {
        String salt = BCrypt.gensalt();
        Set<String> used = new HashSet<>();

        // ASCII
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 80; ++i) {
            sb.append("A");
            String hash = BCrypt.hashpw(sb.toString(), salt);
            if (i < 73) {
                Assertions.assertFalse(used.contains(hash), "Collision at password length " + i);
            } else {
                Assertions.assertTrue(used.contains(hash));
            }
            used.add(hash);
        }
        // Umlauts
        sb = new StringBuilder();
        for (int i = 1; i <= 40; ++i) {
            sb.append("Ä");
            String hash = BCrypt.hashpw(sb.toString(), salt);
            if (i < 37) {
                Assertions.assertFalse(used.contains(hash), "Collision at password length " + i);
            } else {
                Assertions.assertTrue(used.contains(hash));
            }
            used.add(hash);
        }
        // Symbols
        sb = new StringBuilder();
        for (int i = 1; i <= 30; ++i) {
            sb.append("♠");
            String hash = BCrypt.hashpw(sb.toString(), salt);
            if (i < 25) {
                Assertions.assertFalse(used.contains(hash), "Collision at password length " + i);
            } else {
                Assertions.assertTrue(used.contains(hash));
            }
            used.add(hash);
        }
    }
}
