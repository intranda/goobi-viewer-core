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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Eager;

import io.goobi.viewer.managedbeans.AdminConfigEditorBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Application-scoped CDI bean that periodically removes expired config-editor edit leases. Removing a lease also
 * deletes its owner-tagged swap file, which {@link SwapFileWatcher} turns into a lock-status push to clients.
 */
@Eager
@ApplicationScoped
public class FileLockReaper {

    private static final Logger logger = LogManager.getLogger(FileLockReaper.class);

    /** Interval between reaper runs, in seconds. Must be well below the lease TTL so orphaned locks clear promptly. */
    static final long REAPER_INTERVAL_SECONDS = 10L;

    private ScheduledExecutorService scheduler;

    /**
     * Starts the scheduled reaper.
     *
     * @should start and stop without throwing
     */
    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FileLockReaper");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::reap, REAPER_INTERVAL_SECONDS, REAPER_INTERVAL_SECONDS, TimeUnit.SECONDS);
        logger.debug("FileLockReaper started ({}s interval)", REAPER_INTERVAL_SECONDS);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /**
     * Removes expired leases. Package-private for testing.
     *
     * @should not throw when invoked
     */
    void reap() {
        try {
            AdminConfigEditorBean.removeExpiredLocks();
        } catch (RuntimeException e) {
            // Catching RuntimeException at the scheduler boundary on purpose: an uncaught exception from a
            // scheduleWithFixedDelay task silently cancels all future runs, which would disable lock reaping.
            logger.warn("FileLockReaper run failed: {}", e.getMessage());
        }
    }
}
