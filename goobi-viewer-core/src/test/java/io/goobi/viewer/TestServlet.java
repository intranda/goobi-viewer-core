package io.goobi.viewer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;

public class TestServlet {

    private final ClientAndServer mockServer;
    private final MockServerClient serverClient;

    public TestServlet(String url, int port) {
        mockServer = ClientAndServer.startClientAndServer(port);
        serverClient = new MockServerClient(url, port);

    }
    
    public MockServerClient getServerClient() {
        return serverClient;
    }

    public void shutdown() throws Exception {
        serverClient.stop();
        mockServer.stop();
        Path logFile = Paths.get("mockserver.log");
        if (Files.isRegularFile(logFile)) {
            Files.delete(logFile);
        }
    }
}
