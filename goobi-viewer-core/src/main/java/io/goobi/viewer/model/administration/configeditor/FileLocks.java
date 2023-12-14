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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class FileLocks {

    private static final Logger logger = LogManager.getLogger(FileLocks.class);

    private final Map<Path, String> locks = new ConcurrentHashMap<>();

    /**
     * 
     * @param file
     * @param sessionId
     * @return true if file locked successfully; false otherwise
     */
    public synchronized boolean lockFile(Path file, String sessionId) {
        if (!isFileLockedByOthers(file, sessionId)) {
            locks.put(file, sessionId);
            logger.trace("File locked by {}: {}", sessionId, file.toAbsolutePath());
            return true;
        }

        return false;
    }

    /**
     * 
     * @param file
     * @param sessionId
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
     * 
     * @param file
     * @param sessionId
     * @return true if file locked by different session; false otherwise
     * @should return true if file locked by different session id
     * @should return false if file locked by own session id
     * @should return false if file not locked
     */
    public synchronized boolean isFileLockedByOthers(Path file, String sessionId) {
        return locks.containsKey(file) && !locks.get(file).equals(sessionId);
    }

    /**
     * 
     * @param sessionId
     */
    public void clearLocksForSessionId(String sessionId) {
        Set<Path> toClear = new HashSet<>();
        for (Entry<Path, String> entry : locks.entrySet()) {
            if (entry.getValue().equals(sessionId)) {
                toClear.add(entry.getKey());
            }
        }
        if (!toClear.isEmpty()) {
            for (Path path : toClear) {
                locks.remove(path);
                logger.debug("Released edit lock for {}", path.toAbsolutePath());
            }
        }
    }
}
