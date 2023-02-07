package io.goobi.viewer.controller.mq;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class ActiveMQConfigTest {

    private static final String activeMqConfigPath = "src/test/resources/config_activemq.xml";

    ActiveMQConfig config;
    
    @Before
    public void setup() throws IOException {
        this.config = new ActiveMQConfig(Paths.get(activeMqConfigPath));
    }
    
    @Test
    public void testReadURI() {
        assertEquals("tcp://0.0.0.0:61618", config.getConnectorURI());
    }
    
    @Test
    public void testReadUserName() {
        assertEquals("testadmin", config.getUsernameAdmin());
    }
    
    @Test
    public void testReadPassword() {
        assertEquals("test", config.getPasswordAdmin());
    }

}
