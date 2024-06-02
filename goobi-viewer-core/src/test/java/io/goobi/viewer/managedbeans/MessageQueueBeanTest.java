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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.ActiveMQConfig;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataFilter;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.model.job.TaskType;

class MessageQueueBeanTest extends AbstractDatabaseEnabledTest {

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
        this.dao = DataManager.getInstance().getDao();
        //necessary to connect to mq in MessageQueueBean#init
        ActiveMQConfig activeMQConfig = new ActiveMQConfig(Paths.get(activeMqConfigPath));
        MessageQueueManager tempBroker = new MessageQueueManager(activeMQConfig, this.dao, Collections.emptyMap());
        broker = Mockito.spy(tempBroker);
        assertTrue(broker.initializeMessageServer("localhost", 1088, 0), "Failed to start message queue. See log for details");
        //delete messages from other tests
        List<ViewerMessage> messages = this.dao.getViewerMessages(0, 10000, "", false, Collections.emptyMap());
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
    void testSearchFinishedTasks() throws DAOException {
        ViewerMessage task1 = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        task1.setMessageId("ID:florian-test:1.0.0");
        task1.setMessageStatus(MessageStatus.FINISH);
        task1.getProperties().put("pi", "PPN12345");
        task1.getProperties().put("divId", "LOG_0003");

        ViewerMessage task2 = new ViewerMessage(TaskType.UPDATE_DATA_REPOSITORY_NAMES.name());
        task2.setMessageId("ID:florian-test:2.0.0");
        task2.setMessageStatus(MessageStatus.ERROR);
        task2.getProperties().put("pi", "PPN67890");

        dao.addViewerMessage(task1);
        dao.addViewerMessage(task2);

        MessageQueueBean bean = new MessageQueueBean(broker);
        bean.init();
        TableDataProvider<ViewerMessage> data = bean.getLazyModelViewerHistory();
        assertNotNull(data);
        assertEquals(2, data.getPaginatorList().size());

        TableDataFilter filter = data.getFilters().get(0);
        filter.setValue(TaskType.DOWNLOAD_PDF.name());
        assertEquals(1, data.getPaginatorList().size());

        filter.setValue(MessageStatus.ERROR.name());
        assertEquals(1, data.getPaginatorList().size());

        filter.setValue("PPN67890");
        assertEquals(1, data.getPaginatorList().size());

        filter.setValue("ID:florian-test:2.0.0");
        assertEquals(1, data.getPaginatorList().size());
    }
}
