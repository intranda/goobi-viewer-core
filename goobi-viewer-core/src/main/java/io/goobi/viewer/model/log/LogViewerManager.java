package io.goobi.viewer.model.log;

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
     * All entries from a single flush cycle are sent as one JSON array per session so that
     * only a single sendText() call is made per session — the WebSocket async remote endpoint
     * does not allow concurrent sends (TEXT_FULL_WRITING IllegalStateException).
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

        sessions.removeIf(session -> !session.isOpen());
        for (Session session : sessions) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(payload);
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
