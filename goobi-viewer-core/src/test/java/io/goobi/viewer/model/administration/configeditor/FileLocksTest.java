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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FileLocksTest {

    /**
     * @see FileLocks#isFileLockedByOthers(Path, String)
     * @verifies return true if file locked by different session id
     */
    @Test
    void isFileLockedByOthers_shouldReturnTrueIfFileLockedByDifferentSessionId() throws Exception {
        // Lock a file with one session, then check from a different session
        FileLocks fileLocks = new FileLocks();
        Path file = Path.of("/tmp/test-config.xml");
        fileLocks.lockFile(file, "session-A");
        Assertions.assertTrue(fileLocks.isFileLockedByOthers(file, "session-B"));
    }

    /**
     * @see FileLocks#isFileLockedByOthers(Path, String)
     * @verifies return false if file locked by own session id
     */
    @Test
    void isFileLockedByOthers_shouldReturnFalseIfFileLockedByOwnSessionId() throws Exception {
        // Lock a file with a session, then check from the same session
        FileLocks fileLocks = new FileLocks();
        Path file = Path.of("/tmp/test-config.xml");
        fileLocks.lockFile(file, "session-A");
        Assertions.assertFalse(fileLocks.isFileLockedByOthers(file, "session-A"));
    }

    /**
     * @see FileLocks#isFileLockedByOthers(Path, String)
     * @verifies return false if file not locked
     */
    @Test
    void isFileLockedByOthers_shouldReturnFalseIfFileNotLocked() throws Exception {
        // Check a file that was never locked
        FileLocks fileLocks = new FileLocks();
        Path file = Path.of("/tmp/test-config.xml");
        Assertions.assertFalse(fileLocks.isFileLockedByOthers(file, "session-A"));
    }

    /** @see FileLocks#renewLock(Path, String) @verifies extend expiry for own lock */
    @Test
    void renewLock_shouldExtendExpiryForOwnLock() {
        AtomicLong now = new AtomicLong(0L);
        FileLocks fileLocks = new FileLocks(now::get);
        Path file = Path.of("/tmp/test-config.xml");
        fileLocks.lockFile(file, "session-A");
        now.set(FileLocks.LOCK_TTL_MILLIS - 1);
        Assertions.assertTrue(fileLocks.renewLock(file, "session-A"));
        now.set(FileLocks.LOCK_TTL_MILLIS + 1);
        Assertions.assertTrue(fileLocks.isFileLockedByOthers(file, "session-B"),
                "renewed lock must still block other sessions past the original TTL");
    }

    /** @see FileLocks#renewLock(Path, String) @verifies return false for other session */
    @Test
    void renewLock_shouldReturnFalseForOtherSession() {
        FileLocks fileLocks = new FileLocks(() -> 0L);
        Path file = Path.of("/tmp/test-config.xml");
        fileLocks.lockFile(file, "session-A");
        Assertions.assertFalse(fileLocks.renewLock(file, "session-B"));
    }

    /** @see FileLocks#renewLock(Path, String) @verifies return false when not locked */
    @Test
    void renewLock_shouldReturnFalseWhenNotLocked() {
        FileLocks fileLocks = new FileLocks(() -> 0L);
        Assertions.assertFalse(fileLocks.renewLock(Path.of("/tmp/test-config.xml"), "session-A"));
    }

    /** @see FileLocks#isFileLockedByOthers(Path, String) @verifies return false when lock expired */
    @Test
    void isFileLockedByOthers_shouldReturnFalseWhenLockExpired() {
        AtomicLong now = new AtomicLong(0L);
        FileLocks fileLocks = new FileLocks(now::get);
        Path file = Path.of("/tmp/test-config.xml");
        fileLocks.lockFile(file, "session-A");
        now.set(FileLocks.LOCK_TTL_MILLIS);
        Assertions.assertFalse(fileLocks.isFileLockedByOthers(file, "session-B"));
    }

    /** @see FileLocks#removeExpiredLocks() @verifies remove and return only expired locks with their session ids */
    @Test
    void removeExpiredLocks_shouldRemoveAndReturnOnlyExpiredLocks() {
        AtomicLong now = new AtomicLong(0L);
        FileLocks fileLocks = new FileLocks(now::get);
        Path oldFile = Path.of("/tmp/old.xml");
        fileLocks.lockFile(oldFile, "session-A");
        now.set(FileLocks.LOCK_TTL_MILLIS - 10);
        Path freshFile = Path.of("/tmp/fresh.xml");
        fileLocks.lockFile(freshFile, "session-B");
        now.set(FileLocks.LOCK_TTL_MILLIS);
        Map<Path, String> removed = fileLocks.removeExpiredLocks();
        Assertions.assertEquals(Map.of(oldFile, "session-A"), removed);
        Assertions.assertTrue(fileLocks.renewLock(freshFile, "session-B"), "fresh lock must survive the reap");
    }

    /** @see FileLocks#isLocked(Path) @verifies return true for non-expired lock */
    @Test
    void isLocked_shouldReturnTrueForNonExpiredLock() {
        FileLocks fileLocks = new FileLocks(() -> 0L);
        Path file = Path.of("/tmp/test-config.xml");
        fileLocks.lockFile(file, "session-A");
        Assertions.assertTrue(fileLocks.isLocked(file));
    }

    /** @see FileLocks#isLocked(Path) @verifies return false when lock expired */
    @Test
    void isLocked_shouldReturnFalseWhenLockExpired() {
        AtomicLong now = new AtomicLong(0L);
        FileLocks fileLocks = new FileLocks(now::get);
        Path file = Path.of("/tmp/test-config.xml");
        fileLocks.lockFile(file, "session-A");
        now.set(FileLocks.LOCK_TTL_MILLIS);
        Assertions.assertFalse(fileLocks.isLocked(file));
    }

    /** @see FileLocks#isLocked(Path) @verifies return false when not locked */
    @Test
    void isLocked_shouldReturnFalseWhenNotLocked() {
        FileLocks fileLocks = new FileLocks(() -> 0L);
        Assertions.assertFalse(fileLocks.isLocked(Path.of("/tmp/test-config.xml")));
    }
}
