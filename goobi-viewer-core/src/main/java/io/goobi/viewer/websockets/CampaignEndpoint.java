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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.StatisticMode;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordPageStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;

/**
 * Endpoint that maps HTTP session IDs to connected web sockets.
 */
@ServerEndpoint(value = "/crowdsourcing/campaign.socket", configurator = GetHttpSessionConfigurator.class)
public class CampaignEndpoint {

    private static final Logger logger = LogManager.getLogger(CampaignEndpoint.class);

    private static Map<String, PageLock> pageLocks = new ConcurrentHashMap<>();

    private static class PageLock {

        public PageLock(Session session, long campaignId, String recordIdentifier, int pageNumber) {
            this.session = session;
            this.campaignId = campaignId;
            this.recordIdentifier = recordIdentifier;
            this.pageNumber = pageNumber;
        }

        private final Session session;
        private final long campaignId;
        private final String recordIdentifier;
        private final int pageNumber;

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (campaignId ^ (campaignId >>> 32));
            result = prime * result + pageNumber;
            result = prime * result + ((recordIdentifier == null) ? 0 : recordIdentifier.hashCode());
            return result;
        }

        /**
         * @return true if both locks have the same campaignId, recordIdentifier and pageNumber. Session is disregarded for comparison
         */
        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj.getClass().equals(this.getClass())) {
                PageLock other = (PageLock) obj;
                return this.campaignId == other.campaignId
                        && this.recordIdentifier.equals(other.recordIdentifier)
                        && this.pageNumber == other.pageNumber;
            }

            return false;
        }
    }

    private String httpSessionId;
    private Session session;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        logger.trace("onOpen: {}", session.getId());
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        this.httpSessionId = httpSession == null ? null : httpSession.getId();
        this.session = session;
    }

    @OnMessage
    public void onMessage(String message) throws IOException, DAOException {
        logger.trace("onMessage from {}: {}", session.getId(), message);
        if (httpSessionId != null) {
            setPageLock(httpSessionId, session, message);
        }
    }

    private void setPageLock(String httpSessionId, Session session, String message) throws IOException, DAOException {
        synchronized (pageLocks) {
            JSONObject json = new JSONObject(message);
            String pi = json.getString("record");
            int pageNo = json.getInt("page");
            long campaignId = json.getLong("campaign");

            if (getLockedPages(httpSessionId).contains(pageNo)) {
                sendPageLocks(campaignId, pi);
            } else {
                PageLock lock = new PageLock(session, campaignId, pi, pageNo);
                pageLocks.put(httpSessionId, lock);
                broadcast(lock);
            }
        }

    }

    private static PageLock removePageLock(String sessionId) {
        synchronized (pageLocks) {
            PageLock lock = pageLocks.remove(sessionId);
            broadcast(lock);
            return lock;
        }
    }

    @OnClose
    public void onClose(Session session) {
        logger.trace("onClose {}", session.getId());
        if (httpSessionId != null) {
            removePageLock(httpSessionId);
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        logger.warn(t.getMessage());
    }

    private void sendPageLocks(Long campaignId, String recordIdentifier) throws IOException, DAOException {
        synchronized (pageLocks) {
            this.session.getBasicRemote().sendText(getLockedPagesAsJson(httpSessionId, campaignId, recordIdentifier));
        }
    }

    private static void sendWarning(PageLock lock, String text) throws IOException {
        JSONObject warning = new JSONObject();
        warning.put("status", "warning");
        warning.put("message", text);
        lock.session.getBasicRemote().sendText(warning.toString());
    }

    /**
     * Send a message about all locked pages to all sessions which have the same campaignId and recordIdentifier
     *
     * @param sessionLock The lock of the broadcasting session
     * @throws IOException
     * @throws EncodeException
     */
    private static void broadcast(PageLock sessionLock) {

        synchronized (pageLocks) {
            if (sessionLock != null) {
                pageLocks.entrySet().forEach(entry -> {
                    synchronized (entry) { //NOSONAR       entry is in fact part of the property pageLocks and may be used for synchronization
                        String httpSessionId = entry.getKey();
                        PageLock lock = entry.getValue();
                        if (sessionLock.campaignId == lock.campaignId
                                && sessionLock.recordIdentifier.equals(lock.recordIdentifier)) {
                            try {
                                lock.session.getBasicRemote().sendText(getLockedPagesAsJson(httpSessionId, lock.campaignId, lock.recordIdentifier));
                            } catch (IOException | DAOException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * Get all locked pages with the given campaignId and recordIdentifier which are not(!) locked by the given httpSessionId
     *
     * @param httpSessionId
     * @param campaignId
     * @param recordIdentifier
     * @return JSON containing pages locked by other sessions
     * @throws DAOException
     */
    private static String getLockedPagesAsJson(String httpSessionId, long campaignId, String recordIdentifier) throws DAOException {
        JSONObject json = new JSONObject();

        //first add finished and inReview pages
        Map<Integer, String> statusMap = getPageStatus(campaignId, recordIdentifier);
        statusMap.entrySet().forEach(entry -> {
            json.put(Integer.toString(entry.getKey()), entry.getValue());
        });

        pageLocks.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(httpSessionId))
                .filter(entry -> entry.getValue().campaignId == campaignId)
                .filter(entry -> entry.getValue().recordIdentifier.equals(recordIdentifier))
                .map(Entry::getValue)
                .forEach(lock -> json.put(Integer.toString(lock.pageNumber), "LOCKED"));
        return json.toString();
    }

    /**
     * @param campaignId
     * @param recordIdentifier
     * @return Map<Integer, String>
     * @throws DAOException
     */
    private static Map<Integer, String> getPageStatus(long campaignId, String recordIdentifier) throws DAOException {
        Map<Integer, String> map = new HashMap<>();
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        if (StatisticMode.PAGE.equals(campaign.getStatisticMode()) && campaign.getStatistics().get(recordIdentifier) != null) {
            CampaignRecordStatistic statistics = campaign.getStatistics().get(recordIdentifier);
            for (String key : statistics.getPageStatistics().keySet()) {
                CampaignRecordPageStatistic pageStatistic = statistics.getPageStatistics().get(key);
                if (pageStatistic.getPage() != null) {
                    map.put(pageStatistic.getPage(), pageStatistic.getStatus().name());
                }
            }
            logger.debug("pageStatusMap set");
        }
        return map;
    }

    private static List<Integer> getLockedPages(String httpSessionId) {
        return pageLocks.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(httpSessionId))
                .map(Entry::getValue)
                .map(lock -> lock.pageNumber)
                .collect(Collectors.toList());
    }

    /**
     * Remove a registered crowdsourcing page lock after session end and notify the assiciated websocket session that the session has ended
     *
     * @param sessionId
     * @throws DAOException
     * @throws IOException
     */
    public static void removeSessionLock(String sessionId) throws IOException {
        PageLock lock = removePageLock(sessionId);
        sendWarning(lock, "notify__crowdsourcing_session_timed_out");
    }

    /**
     * Checks if the given http session id has a registered lock
     *
     * @param sessionId
     * @return true if given sesisonId has a registered lock; false otherwise
     */
    public static boolean hasLock(String sessionId) {
        return pageLocks.containsKey(sessionId);
    }

}
