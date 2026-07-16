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
package io.goobi.viewer.websockets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.managedbeans.storage.ApplicationBean;
import io.goobi.viewer.model.resources.download.ExternalResourceUrlService;
import jakarta.websocket.RemoteEndpoint.Basic;
import jakarta.websocket.Session;

class DownloadTaskEndpointTest extends AbstractTest {

    private Session wsSession;
    private Basic remote;
    private ApplicationBean storageBean;
    private MessageQueueManager queueManager;
    private ExternalResourceUrlService urlService;
    private DownloadTaskEndpoint endpoint;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        wsSession = Mockito.mock(Session.class);
        remote = Mockito.mock(Basic.class);
        Mockito.when(wsSession.getBasicRemote()).thenReturn(remote);

        queueManager = Mockito.mock(MessageQueueManager.class);
        storageBean = Mockito.mock(ApplicationBean.class);
        Mockito.when(storageBean.getMessageBroker()).thenReturn(queueManager);

        urlService = Mockito.mock(ExternalResourceUrlService.class);

        endpoint = new DownloadTaskEndpoint();
        endpoint.setSession(wsSession);
        endpoint.setStorageBean(storageBean);
        endpoint.setUrlService(urlService);
    }

    /**
     * @verifies send error response for unknown url
     */
    @Test
    void onMessage_shouldSendErrorResponseForUnknownUrl() throws Exception {
        Mockito.when(urlService.getAllowedUrls("PPN123")).thenReturn(Collections.emptyMap());

        endpoint.onMessage("{\"action\":\"startdownload\",\"pi\":\"PPN123\",\"url\":\"https://evil.example.com/malware\"}");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(remote).sendText(captor.capture());
        assertTrue(captor.getValue().contains("\"status\":\"error\""));
    }

    /**
     * @verifies use server side url template instead of client provided value
     */
    @Test
    void onMessage_shouldUseServerSideUrlTemplateInsteadOfClientProvidedValue() throws Exception {
        String allowedUrl = "https://trusted.example.com/resource/PPN123";
        String serverTemplate = "https://trusted.example.com/resource/{PI}";

        Mockito.when(urlService.getAllowedUrls("PPN123")).thenReturn(Map.of(allowedUrl, serverTemplate));
        Mockito.when(queueManager.addToQueue(Mockito.any())).thenReturn("queue-id-1");

        // Client sends a manipulated urlTemplate — server must ignore it
        endpoint.onMessage("{\"action\":\"startdownload\",\"pi\":\"PPN123\",\"url\":\"" + allowedUrl
                + "\",\"urlTemplate\":\"https://evil.example.com/injected/{PI}\"}");

        ArgumentCaptor<ViewerMessage> mqCaptor = ArgumentCaptor.forClass(ViewerMessage.class);
        Mockito.verify(queueManager).addToQueue(mqCaptor.capture());
        assertEquals(serverTemplate, mqCaptor.getValue().getProperties().get("urlTemplate"));
    }

}
