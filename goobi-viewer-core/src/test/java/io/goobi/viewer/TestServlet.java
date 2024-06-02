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
package io.goobi.viewer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;

/**
 * <p>TestServlet class.</p>
 *
 */
public class TestServlet {

    private final ClientAndServer mockServer;
    private final MockServerClient serverClient;

    /**
     * <p>Constructor for TestServlet.</p>
     *
     * @param url a {@link java.lang.String} object
     * @param port a int
     */
    public TestServlet(String url, int port) {
        mockServer = ClientAndServer.startClientAndServer(port);
        serverClient = new MockServerClient(url, port);

    }

    /**
     * <p>Getter for the field <code>serverClient</code>.</p>
     *
     * @return a {@link org.mockserver.client.MockServerClient} object
     */
    public MockServerClient getServerClient() {
        return serverClient;
    }

    /**
     * <p>shutdown.</p>
     *
     * @throws java.lang.Exception if any.
     */
    public void shutdown() throws Exception {
        serverClient.stop();
        mockServer.stop();
        Path logFile = Paths.get("mockserver.log");
        if (Files.isRegularFile(logFile)) {
            Files.delete(logFile);
        }
    }
}
