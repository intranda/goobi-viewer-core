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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.controller.AlphabetIterator;

/**
 * @author Florian Alpers
 *
 */
class AlphabetIteratorTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    void test() {
        AlphabetIterator abc = new AlphabetIterator();

        String lastValue = "";
        int index = 0;
        while (abc.hasNext()) {
            index++;
            lastValue = abc.next();
            switch (index) {
                case 1:
                    Assertions.assertEquals("a", lastValue);
                    break;
                case 2:
                    Assertions.assertEquals("b", lastValue);
                    break;
                case 26:
                    Assertions.assertEquals("z", lastValue);
                    break;
            }
        }
        Assertions.assertEquals("z", lastValue);
        Assertions.assertEquals(26, index);
    }

}
