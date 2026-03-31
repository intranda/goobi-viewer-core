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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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

    @Test
    void downloadFileAction_unknownTaskType_throwsDownloadException() throws Exception {
        // A ViewerMessage with an unrecognised task type causes DownloadJob.from() to
        // throw IllegalArgumentException, which must be re-raised as DownloadException.
        ViewerMessage msg = new ViewerMessage("NOT_A_DOWNLOAD_TASK");
        msg.getProperties().put("pi", "PPN123");

        DownloadBean bean = createBeanWithMessage(msg);

        assertThrows(DownloadException.class, bean::downloadFileAction);
    }

    @Test
    void downloadFileAction_fileNotFound_throwsDownloadException() throws Exception {
        // A valid PDF download job whose target file does not exist on disk must
        // raise DownloadException rather than letting Files.size() throw IOException.
        ViewerMessage msg = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        msg.getProperties().put("pi", "PPN123");
        msg.getProperties().put("path", "/nonexistent/path/to/download.pdf");

        DownloadBean bean = createBeanWithMessage(msg);

        assertThrows(DownloadException.class, bean::downloadFileAction);
    }
}
