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
package io.goobi.viewer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VersionTest {

    final static String MANIFEST = "Manifest-Version: 1.0\r\n" + "Public-Version: 20.01\r\n" + "Implementation-Version: 6d948ec\r\n"
            + "Built-By: root\r\n" + "version: 4.3.0-SNAPSHOT\r\n" + "Created-By: Apache Maven\r\n" + "Build-Jdk: 1.8.0_232\r\n"
            + "ApplicationName: goobi-viewer-core\r\n" + "Implementation-Build-Date: 2020-01-15 18:30";

    /**
     * @see Version#getInfo(String,String)
     * @verifies extract fields correctly
     */
    @Test
    void getInfo_shouldExtractFieldsCorrectly() throws Exception {
        Assertions.assertEquals("20.01", Version.getInfo("Public-Version", MANIFEST));
        Assertions.assertEquals("Apache Maven", Version.getInfo("Created-By", MANIFEST));
        Assertions.assertEquals("2020-01-15 18:30", Version.getInfo("Implementation-Build-Date", MANIFEST));
    }
}
