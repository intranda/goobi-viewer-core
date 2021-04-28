/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.websockets;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint that maps HTTP session IDs to connected web sockets.
 */
@ServerEndpoint(value = "/crowdsourcing/campaign.socket", configurator = GetHttpSessionConfigurator.class)
public class CampaignEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(CampaignEndpoint.class);

    private static Map<String, PageLock> pageLocks = new ConcurrentHashMap<>();

    private static class PageLock {

        public PageLock(Session session, long campaignId, String recordIdentifier, int pageIndex) {
            this.session = session;
            this.campaignId = campaignId;
            this.recordIdentifier = recordIdentifier;
            this.pageIndex = pageIndex;
        }

        public final Session session;
        public final long campaignId;
        public final String recordIdentifier;
        public final int pageIndex;

        /**
         * @return true if both locks have the same campaignId, recordIdentifier and pageIndex. Session is disregarded for comparison
         */
        @Override
        public boolean equals(Object obj) {
            if (obj.getClass().equals(this.getClass())) {
                PageLock other = (PageLock) obj;
                return this.campaignId == other.campaignId &&
                        this.recordIdentifier.equals(other.recordIdentifier) &&
                        this.pageIndex == other.pageIndex;
            } else {
                return false;
            }
        }
    }

    private String httpSessionId;
    private Session session;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        this.httpSessionId = httpSession == null ? null : httpSession.getId();
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) {
        // logger.trace("onMessage from {}: {}", session.getId(), message);
        if (httpSessionId != null) {
            setPageLock(httpSessionId, session, message);
        }
    }

    private void setPageLock(String httpSessionId, Session session, String message) {
        JSONObject json = new JSONObject(message);
        String pi = json.getString("record");
        int index = json.getInt("index");
        long campaignId = json.getLong("campaign");
        PageLock lock = new PageLock(session, campaignId, pi, index);
        pageLocks.put(httpSessionId, lock);
        broadcast();
    }

    private void removePageLock(String sessionId)  {
        pageLocks.remove(sessionId);
        broadcast();
    }

    @OnClose
    public void onClose(Session session) {
        if (httpSessionId != null) {
            removePageLock(httpSessionId);
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.warn(t.getMessage());
    }

    /**
     * Send a message about all locked pages to all session except the current one
     * which have the same campaignId and recordIdentifier
     * 
     * @throws IOException
     * @throws EncodeException
     */
    private void broadcast() {

        if(httpSessionId != null) {
        PageLock sessionLock = pageLocks.get(httpSessionId);
        if(sessionLock != null) {
        pageLocks.entrySet().forEach(entry -> {
            synchronized (entry) {
                String httpSessionId = entry.getKey();
                PageLock lock = entry.getValue();
                if (!httpSessionId.equals(this.httpSessionId) &&
                        sessionLock.campaignId == lock.campaignId &&
                        sessionLock.recordIdentifier.equals(lock.recordIdentifier)) {
                    try {
                        lock.session.getBasicRemote().sendText(getLockedPages(httpSessionId, lock.campaignId, lock.recordIdentifier));
                    } catch (IOException  e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        }
        }
    }

    private String getLockedPages(String httpSessionId, long campaignId, String recordIdentifier) {
        JSONObject json = new JSONObject();
        pageLocks.entrySet().stream()
        .filter(entry -> !entry.getKey().equals(httpSessionId))
        .map(Entry::getValue)
        .forEach(lock -> json.put(Integer.toString(lock.pageIndex), "LOCKED"));
        return json.toString();
    }

    public static void main(String[] args) {
        PageLock p1 = new PageLock(null, 1, "1234", 2);
        PageLock p2 = new PageLock(null, 1, "1234", 2);
        PageLock p3 = new PageLock(null, 1, "1234", 5);

        System.out.println(p1.equals(p2));
        System.out.println(p1.equals(p3));

    }
}
