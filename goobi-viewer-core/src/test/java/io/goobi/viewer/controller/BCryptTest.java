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

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;

public class BCryptTest extends AbstractTest {

    /**
     * @see BCrypt#checkpw(String,String)
     * @verifies return true if passwords match
     */
    @Test
    public void checkpw_shouldReturnTrueIfPasswordsMatch() throws Exception {
        Assert.assertTrue(new BCrypt().checkpw("foobar", "$2a$10$riYEc4vydN5ksUpw/c9e0uV643f4qRyeQ2u.NpXW1FOgI4JnIn5dy"));
    }

    /**
     * @see BCrypt#checkpw(String,String)
     * @verifies return false if passwords dont match
     */
    @Test
    public void checkpw_shouldReturnFalseIfPasswordsDontMatch() throws Exception {
        Assert.assertFalse(new BCrypt().checkpw("barfoo", "$2a$10$riYEc4vydN5ksUpw/c9e0uV643f4qRyeQ2u.NpXW1FOgI4JnIn5dy"));
    }
}
