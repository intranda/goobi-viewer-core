package io.goobi.viewer.managedbeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.ActiveMQConfig;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.StartQueueBrokerListener;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataFilter;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.model.job.TaskType;

public class MessageQueueBeanTest extends AbstractDatabaseEnabledTest {

    private static final String activeMqConfigPath = "src/test/resources/config_activemq.xml";
    
    IDAO dao;
    StartQueueBrokerListener messageQueueEnvironment;
    MessageQueueManager broker;
    
    @Before
    public void setup() throws Exception {
        super.setUp();
        this.dao = DataManager.getInstance().getDao();
        //necessary to connect to mq in MessageQueueBean#init
        ActiveMQConfig activeMQConfig = new ActiveMQConfig(Paths.get(activeMqConfigPath));
        MessageQueueManager tempBroker = new MessageQueueManager(activeMQConfig, this.dao, Collections.emptyMap());
        broker = Mockito.spy(tempBroker);
        assertTrue("Failed to start message queue. See log for details", broker.initializeMessageServer("localhost", 1088, 0));
        //delete messages from other tests
        List<ViewerMessage> messages = this.dao.getViewerMessages(0, 10000, "", false, Collections.emptyMap());
        for (ViewerMessage viewerMessage : messages) {
            this.dao.deleteViewerMessage(viewerMessage);
        }
    }
    
    @After
    public void tearDown() {
        broker.closeMessageServer();
    }
    
    
    @Test
    public void testSearchFinishedTasks() throws DAOException {
        
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
