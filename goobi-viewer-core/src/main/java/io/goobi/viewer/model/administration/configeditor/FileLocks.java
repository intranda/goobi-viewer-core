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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages per-session exclusive edit leases for configuration files. Each lease carries an expiry; it must be
 * renewed (heartbeat) before it expires, otherwise it is treated as orphaned and reaped. Prevents concurrent
 * modification by different HTTP sessions.
 */
public class FileLocks {

    private static final Logger logger = LogManager.getLogger(FileLocks.class);

    /** Lease duration in milliseconds after which an un-renewed lock is considered orphaned. */
    static final long LOCK_TTL_MILLIS = 60_000L;

    /** A single edit lease: the holding session and the epoch-millis time at which it expires. */
    private record LockEntry(String sessionId, long expiresAtMillis) {
    }

    private final Map<Path, LockEntry> locks = new ConcurrentHashMap<>();

    /** Time source in epoch millis; injectable so expiry can be tested deterministically. */
    private final LongSupplier clock;

    /** Production constructor using the system clock. */
    public FileLocks() {
        this(System::currentTimeMillis);
    }

    /** Test constructor with an injectable clock. */
    FileLocks(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Acquires or refreshes the lease for the given session if the file is free or already held by this session.
     *
     * @param file the file path to lock
     * @param sessionId the HTTP session identifier acquiring the lock
     * @return true if file locked successfully; false otherwise
     */
    public synchronized boolean lockFile(Path file, String sessionId) {
        if (!isFileLockedByOthers(file, sessionId)) {
            locks.put(file, new LockEntry(sessionId, clock.getAsLong() + LOCK_TTL_MILLIS));
            logger.trace("File locked by {}: {}", sessionId, file.toAbsolutePath());
            return true;
        }
        return false;
    }

    /**
     * Extends the lease expiry, but only if the lease is currently held by the given session (heartbeat).
     *
     * @param file the file path whose lease should be renewed
     * @param sessionId the HTTP session identifier that must hold the lease
     * @return true if the lease was renewed; false if held by another session or not present
     * @should extend expiry for own lock
     * @should return false for other session
     * @should return false when not locked
     */
    public synchronized boolean renewLock(Path file, String sessionId) {
        LockEntry entry = locks.get(file);
        if (entry != null && entry.sessionId().equals(sessionId)) {
            locks.put(file, new LockEntry(sessionId, clock.getAsLong() + LOCK_TTL_MILLIS));
            return true;
        }
        return false;
    }

    /**
     * Releases the lease for the given file if it is not held by another session.
     *
     * @param file the file path to unlock
     * @param sessionId the HTTP session identifier releasing the lock
     * @return true if file unlocked successfully; false otherwise
     */
    public synchronized boolean unlockFile(Path file, String sessionId) {
        if (!isFileLockedByOthers(file, sessionId) && locks.remove(file) != null) {
            logger.trace("File lock released: {}", file.toAbsolutePath());
            return true;
        }
        return false;
    }

    /**
     * @param file path to the file to check
     * @param sessionId current HTTP session ID to compare against the lock holder
     * @return true if a non-expired lease is held by a different session; false otherwise
     * @should return true if file locked by different session id
     * @should return false if file locked by own session id
     * @should return false if file not locked
     * @should return false when lock expired
     */
    public synchronized boolean isFileLockedByOthers(Path file, String sessionId) {
        LockEntry entry = locks.get(file);
        return entry != null && !entry.sessionId().equals(sessionId) && clock.getAsLong() < entry.expiresAtMillis();
    }

    /**
     * @param file path to the file to check
     * @return true if any (non-expired) lease currently exists for the file. Used by the reaper to re-check, after
     *         removing expired leases, whether a fresh lease was re-acquired before deleting the swap file.
     * @should return true for non-expired lock
     * @should return false when lock expired
     * @should return false when not locked
     */
    public synchronized boolean isLocked(Path file) {
        LockEntry entry = locks.get(file);
        return entry != null && clock.getAsLong() < entry.expiresAtMillis();
    }

    /**
     * Removes all leases whose expiry time has passed.
     *
     * @return map of expired paths to the session id that held them (so callers can clean up owner-tagged artefacts)
     * @should remove and return only expired locks with their session ids
     */
    public synchronized Map<Path, String> removeExpiredLocks() {
        long now = clock.getAsLong();
        Map<Path, String> expired = new HashMap<>();
        for (Entry<Path, LockEntry> e : locks.entrySet()) {
            if (now >= e.getValue().expiresAtMillis()) {
                expired.put(e.getKey(), e.getValue().sessionId());
            }
        }
        for (Path p : expired.keySet()) {
            locks.remove(p);
            logger.debug("Removed expired edit lock for {}", p.toAbsolutePath());
        }
        return expired;
    }

    /**
     * Returns all file paths locked by the given session id.
     *
     * @param sessionId a {@link java.lang.String} object
     * @return set of paths locked by the session
     */
    public synchronized Set<Path> getLockedPathsForSessionId(String sessionId) {
        Set<Path> result = new HashSet<>();
        for (Entry<Path, LockEntry> entry : locks.entrySet()) {
            if (entry.getValue().sessionId().equals(sessionId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * @param sessionId the HTTP session identifier whose locks should be released
     */
    public synchronized void clearLocksForSessionId(String sessionId) {
        for (Path path : getLockedPathsForSessionId(sessionId)) {
            locks.remove(path);
            logger.debug("Released edit lock for {}", path.toAbsolutePath());
        }
    }

    /**
     * Atomically collects and removes all locks for the given session.
     *
     * @param sessionId a {@link java.lang.String} object
     * @return set of paths that were locked by the session and have now been released
     */
    public synchronized Set<Path> getAndClearLocksForSessionId(String sessionId) {
        Set<Path> result = getLockedPathsForSessionId(sessionId);
        for (Path path : result) {
            locks.remove(path);
            logger.debug("Released edit lock for {}", path.toAbsolutePath());
        }
        return result;
    }
}
