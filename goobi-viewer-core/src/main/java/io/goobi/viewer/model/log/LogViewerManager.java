package io.goobi.viewer.model.log;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.websocket.Session;

/**
 * Application-scoped singleton that manages log file tailers and WebSocket sessions.
 * One Tailer thread per active LogFile, shared across all sessions watching that file.
 * Lines are buffered until the next log entry header arrives, so multi-line entries
 * (stacktraces) are sent as a single complete message.
 */
public class LogViewerManager {

    private static final Logger logger = LogManager.getLogger(LogViewerManager.class);
    private static final long TAILER_DELAY_MS = 500;
    private static final long FLUSH_TIMEOUT_MS = 500;

    private final ConcurrentHashMap<LogFile, Set<Session>> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LogFile, Tailer> activeTailers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "logviewer-flush");
        t.setDaemon(true);
        return t;
    });

    public void registerSession(LogFile logFile, Session session) {
        activeSessions.computeIfAbsent(logFile, k -> ConcurrentHashMap.newKeySet()).add(session);
        activeTailers.computeIfAbsent(logFile, k -> startTailer(logFile));
    }

    public void unregisterSession(LogFile logFile, Session session) {
        activeSessions.computeIfPresent(logFile, (k, sessions) -> {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                Tailer tailer = activeTailers.remove(logFile);
                if (tailer != null) {
                    tailer.stop();
                }
                return null;
            }
            return sessions;
        });
    }

    public boolean hasActiveSessions(LogFile logFile) {
        Set<Session> sessions = activeSessions.get(logFile);
        return sessions != null && !sessions.isEmpty();
    }

    public void shutdown() {
        activeTailers.values().forEach(Tailer::stop);
        activeTailers.clear();
        activeSessions.clear();
        scheduler.shutdownNow();
    }

    /**
     * Broadcasts parsed log entries to all active WebSocket sessions for the given log file.
     * All entries from a single flush cycle are sent as one JSON array per session, and the
     * synchronous basic remote is used so that consecutive sends to the same session cannot
     * overlap. The async remote returns before the write completes, which caused
     * {@link IllegalStateException} (TEXT_FULL_WRITING) when several entries were flushed in
     * quick succession.
     *
     * @param logFile the log file whose sessions should receive the update
     * @param rawBlock the raw, unparsed log text of one flush cycle
     * @should send exactly one message per open session and skip closed sessions
     * @should send multiple entries of one flush as a single JSON array message
     * @should not send and not throw when raw block is empty or null
     * @should use synchronous basic remote and never use async remote across consecutive broadcasts
     * @should keep delivering to remaining sessions when one session send fails
     */
    void broadcastParsed(LogFile logFile, String rawBlock) {
        Set<Session> sessions = activeSessions.get(logFile);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        List<LogLine> entries = LogLineParser.parse(rawBlock);
        if (entries.isEmpty()) {
            return;
        }

        // Build one JSON array payload for all entries in this flush cycle
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(entries.get(i).toJson());
        }
        json.append("]");
        String payload = json.toString();

        // Prune sessions that are already closed before sending.
        sessions.removeIf(session -> !session.isOpen());
        for (Session session : sessions) {
            // Re-check isOpen() to guard the prune->send race: a session may close on a
            // container thread between removeIf() above and the send below.
            if (session.isOpen()) {
                try {
                    // Use the synchronous basic remote: sendText() blocks until the message is
                    // fully written, so back-to-back sends to the same session cannot overlap.
                    // The async remote returned before the write completed, which caused
                    // IllegalStateException: TEXT_FULL_WRITING when multiple log entries were
                    // flushed in quick succession (e.g. several entries within one Tailer batch).
                    session.getBasicRemote().sendText(payload);
                } catch (IOException e) {
                    // A broken session must not abort delivery to the remaining sessions; drop it.
                    logger.debug("Failed to send log update to session {}, removing it: {}",
                        session.getId(), e.getMessage());
                    sessions.remove(session);
                }
            }
        }
    }

    private Tailer startTailer(LogFile logFile) {
        Optional<Path> path = logFile.getPath();
        if (path.isEmpty()) {
            logger.warn("No path configured for log file: {}", logFile.getName());
            return null;
        }

        TailerListener listener = new TailerListener() {
            private final StringBuilder pending = new StringBuilder();
            private ScheduledFuture<?> flushTask;

            @Override
            public void init(Tailer tailer) {
                // no-op
            }

            @Override
            public void fileNotFound() {
                logger.debug("Log file not found: {}", logFile.getName());
            }
            @Override public void fileRotated() {
                logger.debug("Log file rotated, continuing: {}", logFile.getName());
                flush();
            }

            @Override
            public synchronized void handle(String line) {
                if (LogLineParser.isHeaderLine(line)) {
                    flush();
                    pending.append(line);
                } else {
                    if (pending.length() > 0) {
                        pending.append("\n");
                    }
                    pending.append(line);
                }
                resetFlushTimer();
            }

            @Override
            public void handle(Exception ex) {
                logger.error("Tailer error for {}", logFile.getName(), ex);
            }

            private synchronized void flush() {
                if (pending.length() == 0) {
                    return;
                }
                String raw = pending.toString();
                pending.setLength(0);
                cancelFlushTimer();
                broadcastParsed(logFile, raw);
            }

            private void resetFlushTimer() {
                cancelFlushTimer();
                flushTask = scheduler.schedule(this::flush, FLUSH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            }

            private void cancelFlushTimer() {
                if (flushTask != null) {
                    flushTask.cancel(false);
                    flushTask = null;
                }
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
