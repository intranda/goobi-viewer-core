package io.goobi.viewer.model.log;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.websocket.Session;

/**
 * Application-scoped singleton that manages log file tailers and WebSocket sessions.
 * One Tailer thread per active LogFile, shared across all sessions watching that file.
 */
public class LogViewerManager {

    private static final Logger logger = LogManager.getLogger(LogViewerManager.class);
    private static final long TAILER_DELAY_MS = 500;

    private final ConcurrentHashMap<LogFile, Set<Session>> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LogFile, Tailer> activeTailers = new ConcurrentHashMap<>();

    public void registerSession(LogFile logFile, Session session) {
        activeSessions.computeIfAbsent(logFile, k -> ConcurrentHashMap.newKeySet()).add(session);
        // computeIfAbsent does not store null — if startTailer returns null (unconfigured path),
        // no entry is created and the next registerSession call will attempt again (harmless).
        activeTailers.computeIfAbsent(logFile, k -> startTailer(logFile));
    }

    public void unregisterSession(LogFile logFile, Session session) {
        Set<Session> sessions = activeSessions.get(logFile);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) {
            activeSessions.remove(logFile);
            Tailer tailer = activeTailers.remove(logFile);
            if (tailer != null) tailer.stop();
        }
    }

    public boolean hasActiveSessions(LogFile logFile) {
        Set<Session> sessions = activeSessions.get(logFile);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Broadcasts a raw log line to all active WebSocket sessions for the given log file.
     * Each session is wrapped in its own try/catch — a failing session does not block others.
     * Dead sessions are removed from the registry.
     */
    public void broadcast(LogFile logFile, String rawLine) {
        Set<Session> sessions = activeSessions.get(logFile);
        if (sessions == null || sessions.isEmpty()) return;

        String json = LogLineParser.parse(rawLine).stream()
            .findFirst()
            .map(LogLine::toJson)
            .orElseGet(() -> new LogLine("", "", "", rawLine).toJson());

        Set<Session> dead = ConcurrentHashMap.newKeySet();
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendText(json);
            } catch (IOException e) {
                // Stack trace intentionally omitted - dead session, expected on disconnect
                logger.debug("Removing dead WebSocket session for log file: {}", logFile.getName());
                dead.add(session);
            }
        }
        sessions.removeAll(dead);
    }

    private Tailer startTailer(LogFile logFile) {
        Optional<Path> path = logFile.getPath();
        if (path.isEmpty()) {
            logger.warn("No path configured for log file: {}", logFile.getName());
            return null; // ConcurrentHashMap.computeIfAbsent ignores null — next call retries
        }

        // Anonymous listener captures logFile in its closure — avoids needing a Tailer→LogFile map.
        TailerListener listener = new TailerListener() {
            @Override public void init(Tailer tailer) {}
            @Override public void fileNotFound() {
                logger.debug("Log file not found: {}", logFile.getName());
            }
            @Override public void fileRotated() {
                logger.debug("Log file rotated, continuing: {}", logFile.getName());
            }
            @Override public void handle(String line) {
                broadcast(logFile, line);
            }
            @Override public void handle(Exception ex) {
                logger.error("Tailer error for {}: {}", logFile.getName(), ex.getMessage(), ex);
            }
        };

        return Tailer.builder()
            .setFile(path.get().toFile())
            .setTailerListener(listener)
            .setDelayDuration(Duration.ofMillis(TAILER_DELAY_MS))
            .setReOpen(true)
            .setStartThread(true)
            .get();
    }
}
