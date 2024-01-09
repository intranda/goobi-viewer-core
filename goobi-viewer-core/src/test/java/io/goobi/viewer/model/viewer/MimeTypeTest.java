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
package io.goobi.viewer.model.viewer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MimeTypeTest {

    /**
     * @see BaseMimeType#getByName(String)
     * @verifies find mime type by short name correctly
     */
    @Test
    void getByName_shouldFindMimeTypeByShortNameCorrectly() throws Exception {
        Assertions.assertEquals(BaseMimeType.APPLICATION, BaseMimeType.getByName("application"));
        Assertions.assertEquals(BaseMimeType.AUDIO, BaseMimeType.getByName("audio"));
        Assertions.assertEquals(BaseMimeType.IMAGE, BaseMimeType.getByName("image"));
        Assertions.assertEquals(BaseMimeType.SANDBOXED_HTML, BaseMimeType.getByName("text"));
    }

    /**
     * @see BaseMimeType#getByName(String)
     * @verifies find mime type by full name correctly
     */
    @Test
    void getByName_shouldFindMimeTypeByFullNameCorrectly() throws Exception {
        Assertions.assertEquals(BaseMimeType.APPLICATION, BaseMimeType.getByName("application/pdf"));
        Assertions.assertEquals(BaseMimeType.AUDIO, BaseMimeType.getByName("audio/mpeg3"));
        Assertions.assertEquals(BaseMimeType.IMAGE, BaseMimeType.getByName("image/jpeg"));
        Assertions.assertEquals(BaseMimeType.SANDBOXED_HTML, BaseMimeType.getByName("text/sandboxed-html"));
    }
}