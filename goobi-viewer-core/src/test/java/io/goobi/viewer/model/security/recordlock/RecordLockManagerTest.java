package io.goobi.viewer.model.security.recordlock;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.RecordLimitExceededException;

public class RecordLockManagerTest extends AbstractTest {

    /**
     * @see RecordLockManager#lockRecord(String,String,Integer)
     * @verifies add record lock to map correctly
     */
    @Test
    public void lockRecord_shouldAddRecordLockToMapCorrectly() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 1);
        Set<RecordLock> locks = DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123");
        Assert.assertNotNull(locks);
        Assert.assertEquals(1, locks.size());
        RecordLock lock = locks.iterator().next();
        Assert.assertEquals("PPN123", lock.getPi());
        Assert.assertEquals("SID123", lock.getSessionId());
    }

    /**
     * @see RecordLockManager#lockRecord(String,String,Integer)
     * @verifies do nothing if limit null
     */
    @Test
    public void lockRecord_shouldDoNothingIfLimitNull() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", null);
        Assert.assertNull(DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123"));
    }

    /**
     * @see RecordLockManager#lockRecord(String,String,Integer)
     * @verifies do nothing if session id already in list
     */
    @Test
    public void lockRecord_shouldDoNothingIfSessionIdAlreadyInList() throws Exception {
        {
            DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 2);
            Set<RecordLock> locks = DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123");
            Assert.assertNotNull(locks);
            Assert.assertEquals(1, locks.size());
        }
        {
            DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 2);
            Set<RecordLock> locks = DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123");
            Assert.assertNotNull(locks);
            Assert.assertEquals(1, locks.size());
        }
    }

    /**
     * @see RecordLockManager#lockRecord(String,String,Integer)
     * @verifies throw RecordLimitExceededException if limit exceeded
     */
    @Test(expected = RecordLimitExceededException.class)
    public void lockRecord_shouldThrowRecordLimitExceededExceptionIfLimitExceeded() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 1);
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID789", 1);
    }

    /**
     * @see RecordLockManager#removeLocksForSessionId(String)
     * @verifies return number of records if session id removed successfully
     */
    @Test
    public void removeLocksForSessionId_shouldReturnNumberOfRecordsIfSessionIdRemovedSuccessfully() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 2);
        Assert.assertEquals(1, DataManager.getInstance().getRecordLockManager().removeLocksForSessionId("SID123", null));
    }

    /**
     * @see RecordLockManager#removeLocksForSessionId(String,List)
     * @verifies skip pi in list
     */
    @Test
    public void removeLocksForSessionId_shouldSkipPiInList() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 2);
        Assert.assertEquals(0,
                DataManager.getInstance().getRecordLockManager().removeLocksForSessionId("SID123", Collections.singletonList("PPN123")));
    }

    /**
     * @see RecordLockManager#removeOldLocks(long)
     * @verifies remove locks older than maxAge
     */
    @Test
    public void removeOldLocks_shouldRemoveLocksOlderThanMaxAge() throws Exception {
        DataManager.getInstance().getRecordLockManager().lockRecord("PPN123", "SID123", 1);
        Set<RecordLock> locks = DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123");
        Assert.assertNotNull(locks);
        Assert.assertEquals(1, locks.size());

        Thread.sleep(10);
        DataManager.getInstance().getRecordLockManager().removeOldLocks(5);
        Assert.assertNull(DataManager.getInstance().getRecordLockManager().getLoadedRecordMap().get("PPN123"));
    }
}