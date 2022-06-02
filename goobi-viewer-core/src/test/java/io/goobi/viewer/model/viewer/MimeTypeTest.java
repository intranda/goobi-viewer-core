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

import org.junit.Assert;
import org.junit.Test;

public class MimeTypeTest {

    /**
     * @see BaseMimeType#getByName(String)
     * @verifies find mime type by short name correctly
     */
    @Test
    public void getByName_shouldFindMimeTypeByShortNameCorrectly() throws Exception {
        Assert.assertEquals(BaseMimeType.APPLICATION, BaseMimeType.getByName("application"));
        Assert.assertEquals(BaseMimeType.AUDIO, BaseMimeType.getByName("audio"));
        Assert.assertEquals(BaseMimeType.IMAGE, BaseMimeType.getByName("image"));
        Assert.assertEquals(BaseMimeType.SANDBOXED_HTML, BaseMimeType.getByName("text"));
    }

    /**
     * @see BaseMimeType#getByName(String)
     * @verifies find mime type by full name correctly
     */
    @Test
    public void getByName_shouldFindMimeTypeByFullNameCorrectly() throws Exception {
        Assert.assertEquals(BaseMimeType.APPLICATION, BaseMimeType.getByName("application/pdf"));
        Assert.assertEquals(BaseMimeType.AUDIO, BaseMimeType.getByName("audio/mpeg3"));
        Assert.assertEquals(BaseMimeType.IMAGE, BaseMimeType.getByName("image/jpeg"));
        Assert.assertEquals(BaseMimeType.SANDBOXED_HTML, BaseMimeType.getByName("text/sandboxed-html"));
    }
}