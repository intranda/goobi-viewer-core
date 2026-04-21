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
}
