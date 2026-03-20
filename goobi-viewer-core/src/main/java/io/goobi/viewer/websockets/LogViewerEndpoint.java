package io.goobi.viewer.websockets;

import java.io.EOFException;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.log.LogFile;
import io.goobi.viewer.model.log.LogViewerManager;
import io.goobi.viewer.model.security.user.User;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint for live log file streaming.
 * URL: /admin/logviewer.socket?logfile={name}
 * Access: superusers only (checked in onOpen).
 */
@ServerEndpoint(value = "/admin/logviewer.socket", configurator = GetHttpSessionConfigurator.class)
public class LogViewerEndpoint {

    private static final Logger logger = LogManager.getLogger(LogViewerEndpoint.class);

    // Singleton — one manager shared across all endpoint instances
    private static final LogViewerManager MANAGER = new LogViewerManager();

    private LogFile logFile;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // Auth: superusers only
        HttpSession httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        User user = BeanUtils.getUserFromSession(httpSession);
        if (user == null || !user.isSuperuser()) {
            closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized");
            return;
        }

        // Logfile name passed as query parameter: ?logfile=viewer
        var params = session.getRequestParameterMap().get("logfile");
        String logfileName = (params != null && !params.isEmpty()) ? params.get(0) : null;
        var optLogFile = LogFile.fromName(logfileName);
        if (optLogFile.isEmpty()) {
            closeSession(session, CloseReason.CloseCodes.CANNOT_ACCEPT, "Unknown log file: " + logfileName);
            return;
        }

        this.logFile = optLogFile.get();
        MANAGER.registerSession(this.logFile, session);
        logger.debug("WebSocket opened for log file: {}", logFile.getName());
    }

    @OnClose
    public void onClose(Session session) {
        if (logFile != null) {
            MANAGER.unregisterSession(logFile, session);
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        if (!(t instanceof EOFException)) {
            logger.warn("LogViewerEndpoint error: {}", t.getMessage());
        }
        if (logFile != null) {
            MANAGER.unregisterSession(logFile, session);
        }
    }

    private static void closeSession(Session session, CloseReason.CloseCode code, String reason) {
        try {
            session.close(new CloseReason(code, reason));
        } catch (IOException e) {
            // Stack trace intentionally omitted - best-effort close, session may already be gone
            logger.debug("Could not close WebSocket session: {}", e.getMessage());
        }
    }

    /** Package-private for tests. */
    static LogViewerManager getManager() {
        return MANAGER;
    }
}
