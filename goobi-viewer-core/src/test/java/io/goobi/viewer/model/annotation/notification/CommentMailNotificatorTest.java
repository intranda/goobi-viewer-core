package io.goobi.viewer.model.annotation.notification;

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("<a href=\"https://example.com/viewer/object/PPN123/3/\">https://example.com/viewer/object/PPN123/3/</a><br/><br/>",
                CommentMailNotificator.buildRecordUrlElement("https://example.com/viewer", comment));
        // Trailing slash in base URL
        Assert.assertEquals("<a href=\"https://example.com/viewer/object/PPN123/3/\">https://example.com/viewer/object/PPN123/3/</a><br/><br/>",
                CommentMailNotificator.buildRecordUrlElement("https://example.com/viewer/", comment));
    }
}