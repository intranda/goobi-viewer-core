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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.RecordLimitExceededException;

class RecordLockManagerTest extends AbstractTest {

    /**
     * @see RecordLockManager#lockRecord(String,String,Integer)
     * @verifies add record lock to map correctly
     */
    @Test
    void lockRecord_shouldAddRecordLockToMapCorrectly() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 1);
        Set<RecordLock> locks = DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123");
        Assertions.assertNotNull(locks);
        Assertions.assertEquals(1, locks.size());
        RecordLock lock = locks.iterator().next();
        Assertions.assertEquals("PPN123", lock.getPi());
        Assertions.assertEquals("SID123", lock.getSessionId());
    }

    /**
     * @see RecordLockManager#lockRecord(String,String,Integer)
     * @verifies do nothing if limit null
     */
    @Test
    void lockRecord_shouldDoNothingIfLimitNull() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", null);
        Assertions.assertNull(DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123"));
    }

    /**
     * @see RecordLockManager#lockRecord(String,String,Integer)
     * @verifies do nothing if session id already in list
     */
    @Test
    void lockRecord_shouldDoNothingIfSessionIdAlreadyInList() throws Exception {
        {
            DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 2);
            Set<RecordLock> locks = DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123");
            Assertions.assertNotNull(locks);
            Assertions.assertEquals(1, locks.size());
        }
        {
            DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 2);
            Set<RecordLock> locks = DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123");
            Assertions.assertNotNull(locks);
            Assertions.assertEquals(1, locks.size());
        }
    }

    /**
     * @see RecordLockManager#lockRecord(String,String,Integer)
     * @verifies throw RecordLimitExceededException if limit exceeded
     */
    @Test
    void lockRecord_shouldThrowRecordLimitExceededExceptionIfLimitExceeded() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 1);
        Assertions.assertThrows(RecordLimitExceededException.class,
                () -> DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID789", 1));
    }

    /**
     * @see RecordLockManager#removeLocksForSessionId(String)
     * @verifies return number of records if session id removed successfully
     */
    @Test
    void removeLocksForSessionId_shouldReturnNumberOfRecordsIfSessionIdRemovedSuccessfully() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 2);
        Assertions.assertEquals(1, DataManager.getInstance().getRecordLockManager().removeLocksForSessionId("SID123", null));
    }

    /**
     * @see RecordLockManager#removeLocksForSessionId(String,List)
     * @verifies skip pi in list
     */
    @Test
    void removeLocksForSessionId_shouldSkipPiInList() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 2);
        Assertions.assertEquals(0,
                DataManager.getInstance().getRecordLockManager().removeLocksForSessionId("SID123", Collections.singletonList("PPN123")));
    }

    /**
     * @see RecordLockManager#removeOldLocks(long)
     * @verifies remove locks older than maxAge
     */
    @Test
    void removeOldLocks_shouldRemoveLocksOlderThanMaxAge() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 1);
        Set<RecordLock> locks = DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123");
        Assertions.assertNotNull(locks);
        Assertions.assertEquals(1, locks.size());

        Thread.sleep(10);
        DataManager.getInstance().getRecordLockManager().removeOldLocks(5);
        Assertions.assertNull(DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123"));
    }
}
