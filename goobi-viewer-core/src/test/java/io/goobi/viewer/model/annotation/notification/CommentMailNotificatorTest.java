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
package io.goobi.viewer.model.annotation.notification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;

public class CommentMailNotificatorTest extends AbstractTest {

    /**
     * @see CommentMailNotificator#buildRecordUrlElement(String,PersistentAnnotation)
     * @verifies build element correctly
     */
    @Test
    public void buildRecordUrlElement_shouldBuildElementCorrectly() throws Exception {
        Comment comment = new Comment();
        comment.setTargetPI("PPN123");
        comment.setTargetPageOrder(3);
        // No trailing slash in base URL
        Assertions.assertEquals("<a href=\"https://example.com/viewer/object/PPN123/3/\">https://example.com/viewer/object/PPN123/3/</a><br/><br/>",
                CommentMailNotificator.buildRecordUrlElement("https://example.com/viewer", comment));
        // Trailing slash in base URL
        Assertions.assertEquals("<a href=\"https://example.com/viewer/object/PPN123/3/\">https://example.com/viewer/object/PPN123/3/</a><br/><br/>",
                CommentMailNotificator.buildRecordUrlElement("https://example.com/viewer/", comment));
    }
}