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
package io.goobi.viewer.model.security.recordlock;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RecordLockManager {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(RecordLockManager.class);

    /** Currently viewed records */
    private final Map<String, Set<RecordLock>> loadedRecordMap = new ConcurrentHashMap<>();
    /**
     * Cache for record access conditions. Null value means the record is not yet cached, while empty list means the record has no access conditions.
     */
    private final Map<String, List<String>> recordAccessConditionsCache = new ConcurrentHashMap<>();
    /**
     * Cache for record access limits. Null value means the record is not yet cached, while empty list means the record has no limits.
     */
    private final Map<String, List<String>> recordLimitsCache = new ConcurrentHashMap<>();

    /**
     *
     * @param pi Record identifier
     * @param sessionId HTTP session ID
     * @param limit Optional number of concurrent views for the record
     * @throws IllegalArgumentException if the given pi is null
     * @should add record lock to map correctly
     * @should do nothing if limit null
     * @should do nothing if session id already in list
     * @return a {@link LockRecordResult}, indicating that either a lock has been set, the lock limit was exceeded or that no action was necessary
     */
    public synchronized LockRecordResult lockRecord(String pi, String sessionId, Integer limit) {
        logger.trace("lockRecord: {}", pi);
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }
        if (sessionId == null) {
            logger.warn("No sessionId given");
            return LockRecordResult.NO_ACTION;
        }
        // Record has unlimited views
        if (limit == null) {
            return LockRecordResult.NO_ACTION;
        }
        Set<RecordLock> recordLocks = loadedRecordMap.computeIfAbsent(pi, k -> new HashSet<>(limit));
        RecordLock newLock = new RecordLock(pi, sessionId);
        logger.trace("{} is currently locked {} times", pi, recordLocks.size());
        if (recordLocks.contains(newLock)) {
            return LockRecordResult.NO_ACTION;
        } else {
            if (recordLocks.size() == limit) {
                return LockRecordResult.LIMIT_EXCEEDED;
            } else {
                recordLocks.add(newLock);
            }
        }
        logger.trace("Added lock: {}", newLock);
        return LockRecordResult.RECORD_LOCKED;
    }

    /**
     *
     * @param sessionId HTTP session ID
     * @param skipPiList Optional list of identifiers to skip
     * @return number of records if session id removed successfully
     * @should return number of records if session id removed successfully
     * @should skip pi in list
     */
    public synchronized int removeLocksForSessionId(String sessionId, List<String> skipPiList) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId may not be null");
        }

        int count = 0;
        for (String pi : loadedRecordMap.keySet()) {
            if (skipPiList != null && skipPiList.contains(pi)) {
                continue;
            }
            if (removeLockForPiAndSessionId(pi, sessionId)) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * @param pi
     * @param sessionId
     * @return true if lock removed successfully; false otherwise
     */
    public synchronized boolean removeLockForPiAndSessionId(String pi, String sessionId) {
        if (pi == null || sessionId == null) {
            return false;
        }
        Set<RecordLock> recordLocks = loadedRecordMap.get(pi);
        if (recordLocks == null) {
            return false;
        }

        boolean ret = false;
        RecordLock lock = new RecordLock(pi, sessionId);
        if (recordLocks.contains(lock)) {
            recordLocks.remove(lock);
            ret = true;
            logger.trace("Removed record lock: {}", lock);
        }

        return ret;
    }

    /**
     * Removes all record locks that are older that <code>maxAge</code> milliseconds. Can be used to periodically clean up locks that might have been
     * missed by the web socket mechanism.
     *
     * @param maxAge
     * @return Number of removed locks
     * @should remove locks older than maxAge
     */
    public synchronized int removeOldLocks(long maxAge) {
        if (loadedRecordMap.isEmpty()) {
            return 0;
        }

        long now = System.currentTimeMillis();
        int count = 0;
        Set<String> emptyPIs = new HashSet<>(loadedRecordMap.size());
        for (Entry<String, Set<RecordLock>> entry : loadedRecordMap.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            Set<RecordLock> toRemove = new HashSet<>();
            for (RecordLock lock : entry.getValue()) {
                if (now - lock.getTimeCreated() > maxAge) {
                    toRemove.add(lock);
                }
            }
            if (!toRemove.isEmpty() && entry.getValue().removeAll(toRemove)) {
                count += toRemove.size();
            }
            if (entry.getValue().isEmpty()) {
                emptyPIs.add(entry.getKey());
            }
        }

        // Remove empty entries
        if (!emptyPIs.isEmpty()) {
            for (String pi : emptyPIs) {
                loadedRecordMap.remove(pi);
            }
        }

        return count;
    }

    /**
     * @return the loadedRecordMap
     */
    Map<String, Set<RecordLock>> getLoadedRecordMap() {
        return loadedRecordMap;
    }

    /**
     *
     * @param pi
     */
    public void emptyCacheForRecord(String pi) {
        if (pi == null) {
            return;
        }

        recordAccessConditionsCache.remove(pi);
        recordLimitsCache.remove(pi);
    }

    /**
     * @return the recordAccessConditionsCache
     */
    public Map<String, List<String>> getRecordAccessConditionsCache() {
        return recordAccessConditionsCache;
    }

    /**
     * @return the recordLimitsCache
     */
    public Map<String, List<String>> getRecordLimitsCache() {
        return recordLimitsCache;
    }
}
