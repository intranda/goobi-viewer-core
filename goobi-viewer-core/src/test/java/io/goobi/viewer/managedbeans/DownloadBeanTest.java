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
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DownloadException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;

class DownloadBeanTest {

    /**
     * Creates a DownloadBean with the private {@code message} field set via reflection.
     * BeanUtils.getUserBean() is mocked for the duration of construction to avoid NPE
     * from the email field initializer.
     */
    private DownloadBean createBeanWithMessage(ViewerMessage message) throws Exception {
        DownloadBean bean;
        UserBean mockUserBean = mock(UserBean.class);
        when(mockUserBean.getEmail()).thenReturn("");

        try (MockedStatic<BeanUtils> beanUtils = mockStatic(BeanUtils.class)) {
            beanUtils.when(BeanUtils::getUserBean).thenReturn(mockUserBean);
            bean = new DownloadBean();
        }

        Field messageField = DownloadBean.class.getDeclaredField("message");
        messageField.setAccessible(true);
        messageField.set(bean, message);
        return bean;
    }

    /**
     * @verifies throw download exception when unknown task type
     * @see DownloadBean#downloadFileAction
     */
    @Test
    void downloadFileAction_shouldThrowDownloadExceptionWhenUnknownTaskType() throws Exception {
        // A ViewerMessage with an unrecognised task type causes DownloadJob.from() to
        // throw IllegalArgumentException, which must be re-raised as DownloadException.
        ViewerMessage msg = new ViewerMessage("NOT_A_DOWNLOAD_TASK");
        msg.getProperties().put("pi", "PPN123");

        DownloadBean bean = createBeanWithMessage(msg);

        assertThrows(DownloadException.class, bean::downloadFileAction);
    }

    /**
     * @verifies throw download exception when file not found
     * @see DownloadBean#downloadFileAction
     */
    @Test
    void downloadFileAction_shouldThrowDownloadExceptionWhenFileNotFound() throws Exception {
        // A valid PDF download job whose target file does not exist on disk must
        // raise DownloadException rather than letting Files.size() throw IOException.
        ViewerMessage msg = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        msg.getProperties().put("pi", "PPN123");
        msg.getProperties().put("path", "/nonexistent/path/to/download.pdf");

        DownloadBean bean = createBeanWithMessage(msg);

        assertThrows(DownloadException.class, bean::downloadFileAction);
    }

    /**
     * @verifies throw download exception when message status is error
     * @see DownloadBean#openDownloadAction()
     */
    @Test
    void openDownloadAction_shouldThrowDownloadExceptionWhenMessageStatusIsError() throws Exception {
        // The PDF generation worker stores the underlying failure cause in
        // properties['message'] and sets MessageStatus.ERROR. openDownloadAction()
        // must surface this as DownloadException so MyExceptionHandler can route it
        // through the standard error.xhtml flow. The thrown message must be sanitised
        // (no absolute filesystem paths leaked) and include the PI for context.
        ViewerMessage msg = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        msg.setMessageStatus(MessageStatus.ERROR);
        msg.getProperties().put("pi", "PPN123");
        msg.getProperties().put("message", "Error creating PDF: Failed to write page 8 to pdf: "
                + "neither file:///opt/digiverso/viewer/data/1/media/X/img.tif nor "
                + "file:///opt/digiverso/viewer/data/1/pdf/X/img.pdf could be resolved");
        DownloadBean bean = createBeanWithMessage(msg);

        DownloadException ex = assertThrows(DownloadException.class, bean::openDownloadAction);
        assertFalse(ex.getMessage().contains("/opt/digiverso"),
                "DownloadException must not leak filesystem paths; was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("PPN123"),
                "DownloadException must include PI for context; was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("img.tif") && ex.getMessage().contains("img.pdf"),
                "DownloadException should preserve filenames; was: " + ex.getMessage());
    }

    /**
     * @verifies throw download exception with fallback message when properties message is blank
     * @see DownloadBean#openDownloadAction()
     */
    @Test
    void openDownloadAction_shouldThrowDownloadExceptionWithFallbackMessageWhenPropertiesMessageIsBlank() throws Exception {
        // Defensive: if a worker sets MessageStatus.ERROR but forgets to populate
        // properties['message'] (or stores an empty string), openDownloadAction must still
        // raise a DownloadException so the user is routed to error.xhtml rather than
        // landing on a viewDownload page that silently shows nothing.
        ViewerMessage msg = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        msg.setMessageStatus(MessageStatus.ERROR);
        msg.getProperties().put("pi", "PPN123");
        // properties['message'] intentionally not set

        DownloadBean bean = createBeanWithMessage(msg);

        DownloadException ex = assertThrows(DownloadException.class, bean::openDownloadAction);
        assertEquals("PDF generation failed", ex.getMessage());
    }

    /**
     * @verifies return message id when message status is finish
     * @see DownloadBean#openDownloadAction()
     */
    @Test
    void openDownloadAction_shouldReturnMessageIdWhenMessageStatusIsFinish() throws Exception {
        // Positive control: a successfully completed PDF job must continue to return
        // its messageId so PrettyFaces can render viewDownload.xhtml normally.
        ViewerMessage msg = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        msg.setMessageStatus(MessageStatus.FINISH);
        msg.setMessageId("abc-123");
        msg.getProperties().put("pi", "PPN123");

        DownloadBean bean = createBeanWithMessage(msg);

        assertEquals("abc-123", bean.openDownloadAction());
    }

    /**
     * @verifies strip file uri paths to filename only
     * @see DownloadBean#sanitizeDownloadErrorMessage(String, String)
     */
    @Test
    void sanitizeDownloadErrorMessage_shouldStripFileUriPathsToFilenameOnly() {
        // file:/// URIs in worker messages must be reduced to the bare filename so
        // server-side directory layout is not exposed to the user.
        String raw = "Failed to read file:///opt/digiverso/viewer/data/1/media/PPN/img.tif";
        assertEquals("Failed to read img.tif", DownloadBean.sanitizeDownloadErrorMessage(raw, null));
    }

    /**
     * @verifies strip absolute filesystem paths to filename only
     * @see DownloadBean#sanitizeDownloadErrorMessage(String, String)
     */
    @Test
    void sanitizeDownloadErrorMessage_shouldStripAbsoluteFilesystemPathsToFilenameOnly() {
        // Absolute Unix paths (without file:/// scheme) must also be stripped so any
        // worker that logs raw java.nio.file.Path-style output is covered.
        String raw = "I/O error reading /opt/digiverso/viewer/data/1/media/PPN/img.tif";
        assertEquals("I/O error reading img.tif", DownloadBean.sanitizeDownloadErrorMessage(raw, null));
    }

    /**
     * @verifies rewrite contentlib image not found pattern into friendly form with pi
     * @see DownloadBean#sanitizeDownloadErrorMessage(String, String)
     */
    @Test
    void sanitizeDownloadErrorMessage_shouldRewriteContentlibImageNotFoundPatternIntoFriendlyFormWithPi() {
        // The contentlib's "Failed to write page X to pdf: neither A nor B could be resolved"
        // is the most common PDF generation failure; rewrite it into a user-friendly form
        // including the record PI.
        String raw = "Error creating PDF: Failed to write page 8 to pdf: neither "
                + "file:///opt/digiverso/viewer/data/1/media/X/EPN_770720048_0008.tif nor "
                + "file:///opt/digiverso/viewer/data/1/pdf/X/EPN_770720048_0008.pdf could be resolved";
        assertEquals("Failed to generate PDF: Unable to find image EPN_770720048_0008.tif "
                + "or PDF EPN_770720048_0008.pdf for PI 514587393 in the file system.",
                DownloadBean.sanitizeDownloadErrorMessage(raw, "514587393"));
    }

    /**
     * @verifies append pi when not already present and pattern does not match
     * @see DownloadBean#sanitizeDownloadErrorMessage(String, String)
     */
    @Test
    void sanitizeDownloadErrorMessage_shouldAppendPiWhenNotAlreadyPresentAndPatternDoesNotMatch() {
        // For messages that don't match the friendly-rewrite pattern, append the PI in
        // parentheses so the support staff can correlate the user-facing error with the
        // worker log entry. Skip the appendage when the message already mentions the PI.
        String raw = "Out of memory while generating PDF";
        assertEquals("Out of memory while generating PDF (PI: PPN999)",
                DownloadBean.sanitizeDownloadErrorMessage(raw, "PPN999"));
    }

    /**
     * @verifies return null when raw is blank
     * @see DownloadBean#sanitizeDownloadErrorMessage(String, String)
     */
    @Test
    void sanitizeDownloadErrorMessage_shouldReturnNullWhenRawIsBlank() {
        // Null/blank input is the worker-forgot-to-populate-properties['message'] case;
        // the caller is expected to substitute its own fallback (e.g. "PDF generation failed").
        assertNull(DownloadBean.sanitizeDownloadErrorMessage(null, "PPN999"));
        assertNull(DownloadBean.sanitizeDownloadErrorMessage("", "PPN999"));
        assertNull(DownloadBean.sanitizeDownloadErrorMessage("   ", "PPN999"));
    }
}
