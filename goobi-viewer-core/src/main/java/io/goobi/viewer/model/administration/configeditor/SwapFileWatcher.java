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
package io.goobi.viewer.model.administration.configeditor;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Eager;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

import io.goobi.viewer.controller.DataManager;

/**
 * Application-scoped CDI bean that monitors directories for vim swap file changes
 * and pushes live lock-status updates via OmniFaces WebSocket push channel {@code adminLockStatus}.
 */
@Eager
@ApplicationScoped
public class SwapFileWatcher {

    private static final Logger logger = LogManager.getLogger(SwapFileWatcher.class);

    static final String PUSH_MESSAGE = "lockStatusChanged";

    @Inject
    @Push
    private PushContext adminLockStatus;

    private WatchService watchService;
    private ExecutorService executor;

    /** CDI no-arg constructor. */
    public SwapFileWatcher() {
    }

    /** Test constructor with injected PushContext. */
    SwapFileWatcher(PushContext pushContext) {
        this.adminLockStatus = pushContext;
    }

    @PostConstruct
    public void init() {
        try {
            String configLocalPath = DataManager.getInstance().getConfiguration().getConfigLocalPath();
            startWatching(Path.of(configLocalPath));
        } catch (IOException | InvalidPathException | IllegalStateException e) {
            logger.warn("SwapFileWatcher could not start: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        stopWatching();
    }

    /**
     * Start watching the given directory for swap file changes.
     * Safe to call multiple times for different directories.
     * Package-private for testing.
     *
     * @param directory the directory to watch
     * @throws IOException if the watch service cannot be registered
     */
    synchronized void startWatching(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            logger.warn("SwapFileWatcher: not a directory, skipping: {}", directory);
            return;
        }
        if (watchService == null) {
            watchService = FileSystems.getDefault().newWatchService();
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "SwapFileWatcher");
                t.setDaemon(true);
                return t;
            });
            executor.submit(this::watchLoop);
        }
        directory.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE);
        logger.debug("SwapFileWatcher: watching {}", directory);
    }

    /**
     * Stop the watcher and shut down the executor thread.
     * Package-private for testing.
     */
    synchronized void stopWatching() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn("SwapFileWatcher: error closing WatchService: {}", e.getMessage());
            }
            watchService = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void watchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (ClosedWatchServiceException e) {
                break; // Normal shutdown via @PreDestroy / stopWatching()
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                Path changed = (Path) event.context();
                if (isSwpFile(changed)) {
                    logger.trace("SwapFileWatcher: .swp change: {}", changed);
                    try {
                        adminLockStatus.send(PUSH_MESSAGE);
                    } catch (IllegalStateException | NullPointerException e) {
                        logger.trace("SwapFileWatcher: push failed (no active sessions?): {}", e.getMessage());
                    }
                }
            }

            if (!key.reset()) {
                logger.warn("SwapFileWatcher: WatchKey invalid, directory may have been deleted");
            }
        }
    }

    private static boolean isSwpFile(Path path) {
        String name = path.getFileName().toString();
        return name.startsWith(".") && name.endsWith(".swp");
    }
}
