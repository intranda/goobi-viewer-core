package io.goobi.viewer.controller.mq;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.mq.PdfMessageHandler;

public class DefaultQueueListenerTest extends AbstractDatabaseEnabledTest {

private static final String activeMqConfigPath = "src/test/resources/config_activemq.xml";
    
    IDAO dao;
    StartQueueBrokerListener messageQueueEnvironment;
    MessageBroker broker;
    
    
    @Before
    public void setup() throws Exception {
        super.setUp();
        this.dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.addViewerMessage(Mockito.any())).thenReturn(true);
        Mockito.when(dao.updateViewerMessage(Mockito.any())).thenReturn(true);
        
        PdfMessageHandler pdfHandler = Mockito.mock(PdfMessageHandler.class);
        Mockito.when(pdfHandler.call(Mockito.any())).thenReturn(MessageStatus.FINISH);
        MessageBroker tempBroker = new MessageBroker(this.dao, Map.of(TaskType.DOWNLOAD_PDF.name(), pdfHandler));
        broker = Mockito.spy(tempBroker);
        messageQueueEnvironment = new StartQueueBrokerListener(broker);
        messageQueueEnvironment.initializeMessageServer(activeMqConfigPath, "goobi", "goobi");
        
        //delete messages from other tests
        List<ViewerMessage> messages = this.dao.getViewerMessages(0, 500, "", false, Collections.emptyMap());
        for (ViewerMessage viewerMessage : messages) {
            this.dao.deleteViewerMessage(viewerMessage);
        }
    }
    
    @Test
    public void testStartQueues() {
        
        ViewerMessage message = MessageGenerator.generateSimpleMessage(TaskType.DOWNLOAD_PDF.name());
        String messageId = broker.addToQueue(message);
        Mockito.verify(broker, Mockito.timeout(20000).times(1)).handle(Mockito.argThat( m -> m.getMessageId().equals(messageId)  ));
    }

}
