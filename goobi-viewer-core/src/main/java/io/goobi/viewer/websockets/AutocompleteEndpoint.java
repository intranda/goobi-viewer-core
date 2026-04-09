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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.core.Context;

/**
 * WebSocket server endpoint that handles real-time search autocomplete requests by querying
 * the Solr index and returning JSON-formatted suggestion lists to the connected client.
 */
@ServerEndpoint(value = "/search/autocomplete.socket", configurator = GetHttpSessionConfigurator.class)
public class AutocompleteEndpoint {

    private static final Logger logger = LogManager.getLogger(AutocompleteEndpoint.class);

    private static final int MIN_TERM_LENGTH = 3;

    private Session session;
    private HttpSession httpSession;

    @Context
    private SearchBean searchBean;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        if (searchBean == null) {
            searchBean = BeanUtils.findInstanceInSessionAttributes(httpSession, SearchBean.class).orElse(null);
        }
    }

    @OnMessage
    public synchronized void onMessage(String messageString) {

        logger.debug("receive message {}", messageString);
        if (searchBean == null) {
            String message = "Search context not found in session. Autosuggest not available";
            sendError(message);
        } else {

            String term = getTerm(messageString);
            if (term != null && term.length() >= MIN_TERM_LENGTH) {
                try {
                    List<String> suggestions = searchBean.autocomplete(term);
                    sendMessage(suggestions.stream().map(this::cleanSuggestion).distinct().toList());
                } catch (IndexUnreachableException e) {
                    logger.error("Error getting autocomplete suggestions for {}: {}", term, e.toString());
                }
            }
        }
    }

    private String cleanSuggestion(String suggestion) {
        return suggestion.replaceAll("^\\W++", "")
                .replaceAll("\\W++$", ""); //NOSONAR – false positive: anchored regex used only to trim non-word chars
    }

    private static String getTerm(String messageString) {
        JSONObject message = new JSONObject(messageString);
        if (message.has("term")) {
            return message.getString("term");
        }
        return "";
    }

    private synchronized void sendError(String error) {
        logger.debug("send error message {}", error);
        try {
            JSONObject object = new JSONObject();
            object.put("error", true);
            object.put("message", error);
            session.getBasicRemote().sendText(object.toString());
        } catch (IOException e) {
            logger.error("Experienced Exception while sending text {}", error, e);
        }
    }

    private synchronized void sendMessage(List<String> suggestions) {
        logger.debug("send message {}", StringUtils.join(suggestions));
        try {
            session.getBasicRemote().sendText(JsonTools.getAsJson(suggestions));
        } catch (IOException e) {
            logger.error("Experienced Exception while sending text {}", suggestions, e);
        }
    }

}
