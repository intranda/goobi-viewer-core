package io.goobi.viewer.controller.mq;

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
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.mq.PdfMessageHandler;

public class DefaultQueueListenerTest extends AbstractDatabaseEnabledTest {

private static final String activeMqConfigPath = "src/test/resources/config_activemq.xml";
    
    IDAO dao;
    MessageQueueManager broker;
    
    
    @Before
    public void setup() throws Exception {
        super.setUp();
        this.dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.addViewerMessage(Mockito.any())).thenReturn(true);
        Mockito.when(dao.updateViewerMessage(Mockito.any())).thenReturn(true);
        
        PdfMessageHandler pdfHandler = Mockito.mock(PdfMessageHandler.class);
        Mockito.when(pdfHandler.call(Mockito.any())).thenReturn(MessageStatus.FINISH);
        ActiveMQConfig activeMQConfig = new ActiveMQConfig(Paths.get(activeMqConfigPath));
        MessageQueueManager tempBroker = new MessageQueueManager(activeMQConfig, this.dao, Map.of(TaskType.DOWNLOAD_PDF.name(), pdfHandler));
        broker = Mockito.spy(tempBroker);
        assertTrue("Failed to start message queue. See log for details", broker.initializeMessageServer("localhost", 1088, 0));
        
        //delete messages from other tests
        List<ViewerMessage> messages = this.dao.getViewerMessages(0, 500, "", false, Collections.emptyMap());
        for (ViewerMessage viewerMessage : messages) {
            this.dao.deleteViewerMessage(viewerMessage);
        }
    }
    
    @After
    public void tearDown() {
        broker.closeMessageServer();
    }
    
    @Test
    public void testStartQueues() throws MessageQueueException {
        
        ViewerMessage message = new ViewerMessage(TaskType.DOWNLOAD_PDF.name());
        String messageId = broker.addToQueue(message);
        Mockito.verify(broker, Mockito.timeout(8000).times(1)).handle(Mockito.argThat( m -> m.getMessageId().equals(messageId)  ));
//        Mockito.verify(broker, Mockito.timeout(8000).times(1)).initializeMessageServer(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());
    }

}
