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
package io.goobi.viewer.model.annotation.comments;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.security.user.User;

class CommentLegacyTest extends AbstractTest {

    /**
     * Verifies that the parameterized constructor correctly assigns all fields.
     *
     * @see CommentLegacy#CommentLegacy(String, int, User, String, CommentLegacy)
     * @verifies construct object correctly
     */
    @Test
    void CommentLegacy_shouldConstructObjectCorrectly() throws Exception {
        User owner = new User();
        String pi = "PPN12345";
        int page = 3;
        String text = "This is a test comment";

        CommentLegacy comment = new CommentLegacy(pi, page, owner, text, null);

        Assertions.assertEquals(pi, comment.getPi());
        Assertions.assertEquals(Integer.valueOf(page), comment.getPage());
        Assertions.assertEquals(owner, comment.getOwner());
        Assertions.assertEquals(text, comment.getText());
    }
}
