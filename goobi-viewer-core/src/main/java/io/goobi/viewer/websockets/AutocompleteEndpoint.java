package io.goobi.viewer.websockets;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

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
            try {
                sendError(message);
            } catch (JsonProcessingException e) {
                logger.error("Error sending error message {}: {}", message, e.toString());

            }
        } else {

            String term = getTerm(messageString);
            if (term != null && term.length() >= MIN_TERM_LENGTH) {
                try {
                    List<String> suggestions = searchBean.autocomplete(term);
                    sendMessage(suggestions.stream().map(this::cleanSuggestion).distinct().toList());
                } catch (IndexUnreachableException | JsonProcessingException e) {
                    logger.error("Error getting autocomplete suggestions for {}: {}", term, e.toString());
                }
            }
        }
    }

    private String cleanSuggestion(String suggestion) {
        return suggestion.replaceAll("^\\W+|\\W+$", "");
    }

    private String getTerm(String messageString) {
        JSONObject message = new JSONObject(messageString);
        if (message.has("term")) {
            return message.getString("term");
        } else {
            return "";
        }
    }

    private synchronized void sendError(String error) throws JsonProcessingException {
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

    private synchronized void sendMessage(List<String> suggestions) throws JsonProcessingException {
        logger.debug("send message {}", StringUtils.join(suggestions));
        try {
            session.getBasicRemote().sendText(JsonTools.getAsJson(suggestions));
        } catch (IOException e) {
            logger.error("Experienced Exception while sending text {}", suggestions, e);
        }
    }

}
