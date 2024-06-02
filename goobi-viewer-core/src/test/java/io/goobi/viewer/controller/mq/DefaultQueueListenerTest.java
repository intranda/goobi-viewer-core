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
package io.goobi.viewer.controller.mq;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.mq.PdfMessageHandler;

class DefaultQueueListenerTest extends AbstractDatabaseEnabledTest {

    private static final String activeMqConfigPath = "src/test/resources/config_activemq.xml";

    IDAO dao;
    MessageQueueManager broker;
    Path schedulerDirectory;

    /**
     * <p>setUp.</p>
     *
     * @throws java.lang.Exception if any.
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.addViewerMessage(Mockito.any())).thenReturn(true);
        Mockito.when(dao.updateViewerMessage(Mockito.any())).thenReturn(true);

        PdfMessageHandler pdfHandler = Mockito.mock(PdfMessageHandler.class);
        Mockito.when(pdfHandler.call(Mockito.any(), Mockito.any())).thenReturn(MessageStatus.FINISH);
        ActiveMQConfig activeMQConfig = new ActiveMQConfig(Paths.get(activeMqConfigPath));
        MessageQueueManager tempBroker = new MessageQueueManager(activeMQConfig, this.dao, Map.of(TaskType.DOWNLOAD_PDF.name(), pdfHandler));
        broker = Mockito.spy(tempBroker);
        assertTrue(broker.initializeMessageServer("localhost", 1088, 0), "Failed to start message queue. See log for details");

        //delete messages from other tests
        List<ViewerMessage> messages = this.dao.getViewerMessages(0, 500, "", false, Collections.emptyMap());
        for (ViewerMessage viewerMessage : messages) {
            this.dao.deleteViewerMessage(viewerMessage);
        }

        schedulerDirectory = Paths.get(activeMQConfig.getSchedulerDirectory());
        if (Files.exists(schedulerDirectory)) {
            FileUtils.deleteDirectory(schedulerDirectory.toFile());
        }
    }

    /**
     * <p>tearDown.</p>
     *
     * @throws java.io.IOException if any.
     */
    @AfterEach
    public void tearDown() throws IOException {
        broker.closeMessageServer();
        if (schedulerDirectory != null && Files.exists(schedulerDirectory)) {
            FileUtils.deleteDirectory(schedulerDirectory.toFile());
        }
    }

    @Test
    void testStartQueues() throws MessageQueueException {

        ViewerMessage message = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        String messageId = broker.addToQueue(message);
        Mockito.verify(broker, Mockito.timeout(8000).times(1)).handle(Mockito.argThat(m -> m.getMessageId().equals(messageId)));
        //        Mockito.verify(broker, Mockito.timeout(8000).times(1)).initializeMessageServer(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
    }

}
