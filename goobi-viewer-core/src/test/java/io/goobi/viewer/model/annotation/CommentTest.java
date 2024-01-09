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
package io.goobi.viewer.model.annotation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.annotation.comments.CommentManager;
import io.goobi.viewer.model.security.user.User;

public class CommentTest {
    /**
     * @see Comment#Comment(String,int,User,String,Comment)
     * @verifies construct object correctly
     */
    @Test
    public void Comment_shouldConstructObjectCorrectly() throws Exception {
        User owner = new User();
        Comment comment = new Comment("PPN123", 1, owner, "comment text", null, null);
        Assertions.assertEquals("PPN123", comment.getTargetPI());
        Assertions.assertEquals(owner, comment.getCreator());
        Assertions.assertEquals("comment text", comment.getText());
        //        Assertions.assertNull(comment.getParent());
    }

    /**
     * @see Comment#mayEdit(User)
     * @verifies return true if use id equals owner id
     */
    @Test
    public void mayEdit_shouldReturnTrueIfUseIdEqualsOwnerId() throws Exception {
        User owner = new User();
        owner.setId(1L);
        Comment comment = new Comment("PPN123", 1, owner, "comment text", null, null);
        Assertions.assertTrue(comment.mayEdit(owner));
    }

    /**
     * @see Comment#mayEdit(User)
     * @verifies return false if owner id is null
     */
    @Test
    public void mayEdit_shouldReturnFalseIfOwnerIdIsNull() throws Exception {
        User owner = new User(); // no ID set
        Comment comment = new Comment("PPN123", 1, owner, "comment text", null, null);
        Assertions.assertFalse(comment.mayEdit(owner));

    }

    /**
     * @see Comment#mayEdit(User)
     * @verifies return false if user is null
     */
    @Test
    public void mayEdit_shouldReturnFalseIfUserIsNull() throws Exception {
        User owner = new User();
        Comment comment = new Comment("PPN123", 1, owner, "comment text", null, null);
        Assertions.assertFalse(comment.mayEdit(null));
    }

    /**
     * @see Comment#checkAndCleanScripts()
     * @verifies remove scripts correctly
     */
    @Test
    public void checkAndCleanScripts_shouldRemoveScriptsCorrectly() throws Exception {
        User owner = new User();
        owner.setId(1L);
        Comment comment =
                new Comment("PPN123", 1, owner, "foo <script type=\\\"javascript\\\">\\nfunction f {\\n alert();\\n}\\n</script> bar", null, null);
        String cleanedText = CommentManager.checkAndCleanScripts(comment.getText(), null, null, null);
        Assertions.assertEquals("foo  bar", cleanedText);
    }
}
