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
package io.goobi.viewer.model.statistics.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * @author florian
 *
 */
class DailySessionUsageStatisticsTest extends AbstractDatabaseEnabledTest {

    IDAO dao;
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dao = DataManager.getInstance().getDao();
        List<DailySessionUsageStatistics> stats = dao.getUsageStatistics(LocalDate.of(0, Month.JANUARY, 1), LocalDate.of(3000, Month.JANUARY, 1));
        stats.forEach(stat -> {
            try {
                dao.deleteUsageStatistics(stat.getId());
            } catch (DAOException e) {
                fail(e.toString());
            }
        });

    }
    
    /**
     * @verifies persist and load usage statistics correctly
     * @see IDAO#addUsageStatistics(DailySessionUsageStatistics)
     */
    @Test
    void addUsageStatistics_shouldPersistAndLoadCorrectly() throws DAOException {
        
        
        LocalDate date = LocalDate.now();
        
        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
        
        dao.addUsageStatistics(stats);
        
        DailySessionUsageStatistics loaded = dao.getUsageStatistics(date);
        assertNotNull(loaded);
        assertEquals(stats.getId(), loaded.getId());
        assertEquals(stats.getViewerInstance(), loaded.getViewerInstance());
        
        dao.deleteUsageStatistics(stats.getId());
    }
    
    /**
     * @verifies persist and update session entries correctly
     */
    @Test
    void addUsageStatistics_shouldPersistAndUpdateSessionEntriesCorrectly() throws DAOException {
        
        LocalDate date = LocalDate.now();
        RequestType type = RequestType.RECORD_VIEW;
        
        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
        
        SessionUsageStatistics session1 = new SessionUsageStatistics("ABCD", "Ubuntu Firefox", "168.178.192.2");
        session1.setRecordRequectCount(type, "PI_01", 7);
        session1.setRecordRequectCount(type, "PI_02", 3);
        stats.addSession(session1);
        
        SessionUsageStatistics session2 = new SessionUsageStatistics("EFGH", "Ubuntu Chrome", "168.178.192.3");
        session2.setRecordRequectCount(type, "PI_01", 2);
        session2.setRecordRequectCount(type, "PI_03", 4);
        stats.addSession(session2);

        dao.addUsageStatistics(stats);
        
        DailySessionUsageStatistics loaded = dao.getUsageStatistics(date);
        assertNotNull(loaded);
        assertNotNull(loaded.getSession("ABCD"));
        assertEquals(7, loaded.getSession("ABCD").getRecordRequestCount(type, "PI_01"));
        assertNotNull(loaded.getSession("EFGH"));
        assertEquals(4, loaded.getSession("EFGH").getRecordRequestCount(type, "PI_03"));
        
        DailySessionUsageStatistics stats3 = dao.getUsageStatistics(date);
        stats3.getSession("EFGH").incrementRequestCount(type, "PI_03");
        dao.updateUsageStatistics(stats3);
        DailySessionUsageStatistics stats4 = dao.getUsageStatistics(date);
        assertEquals(5, stats4.getSession("EFGH").getRecordRequestCount(type, "PI_03"));
        
        
        dao.deleteUsageStatistics(stats.getId());
    }
    
    /**
     * @verifies delete usage statistics successfully
     */
    @Test
    void deleteUsageStatistics_shouldDeleteUsageStatisticsSuccessfully() throws DAOException {
        
        LocalDate date = LocalDate.now();
        RequestType type = RequestType.RECORD_VIEW;

        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
        
        SessionUsageStatistics session1 = new SessionUsageStatistics("ABCD", "Ubuntu Firefox", "168.178.192.2");
        session1.setRecordRequectCount(type, "PI_01", 7);
        session1.setRecordRequectCount(type, "PI_02", 3);
        stats.addSession(session1);

        dao.addUsageStatistics(stats);
        
        DailySessionUsageStatistics loaded = dao.getUsageStatistics(date);
        assertNotNull(loaded);
        assertNotNull(loaded.getSession("ABCD"));
        assertEquals(7, loaded.getSession("ABCD").getRecordRequestCount(type, "PI_01"));
        
        dao.deleteUsageStatistics(loaded.getId());
        loaded = dao.getUsageStatistics(date);
        assertNull(loaded);
        
        dao.deleteUsageStatistics(stats.getId());
        
    }
    
    /**
     * @verifies return correct total and unique request counts per record
     * @see DailySessionUsageStatistics#getTotalRequestCount(RequestType, String)
     */
    @Test
    void getTotalRequestCount_shouldReturnCorrectTotalAndUniqueRequestCountsPerRecord() {
        
        LocalDate date = LocalDate.now();
        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
        RequestType type = RequestType.RECORD_VIEW;

        SessionUsageStatistics session1 = new SessionUsageStatistics("ABCD", "Ubuntu Firefox", "168.178.192.2");
        session1.setRecordRequectCount(type, "PI_01", 7);
        session1.setRecordRequectCount(type, "PI_02", 3);
        stats.addSession(session1);
        
        SessionUsageStatistics session2 = new SessionUsageStatistics("EFGH", "Ubuntu Chrome", "168.178.192.3");
        session2.setRecordRequectCount(type, "PI_01", 2);
        session2.setRecordRequectCount(type, "PI_03", 4);
        stats.addSession(session2);

        assertEquals(9l, stats.getTotalRequestCount(type, "PI_01"));
        assertEquals(2l, stats.getUniqueRequestCount(type, "PI_01"));
        assertEquals(3l, stats.getTotalRequestCount(type, "PI_02"));
        assertEquals(1l, stats.getUniqueRequestCount(type, "PI_02"));
        assertEquals(4l, stats.getTotalRequestCount(type, "PI_03"));
        assertEquals(1l, stats.getUniqueRequestCount(type, "PI_03"));
    }
    
    /**
     * @see DailySessionUsageStatistics#getSession(String)
     * @verifies persistence with entries
     */
    @Test
    void getSession_shouldPersistenceWithEntries() throws DAOException {
        // Verify that getSession returns the correct session after persistence round-trip
        LocalDate date = LocalDate.now();
        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");

        SessionUsageStatistics session = new SessionUsageStatistics("SESSION1", "TestAgent", "127.0.0.1");
        session.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_01", 5);
        stats.addSession(session);

        dao.addUsageStatistics(stats);

        DailySessionUsageStatistics loaded = dao.getUsageStatistics(date);
        assertNotNull(loaded);
        assertNotNull(loaded.getSession("SESSION1"));
        assertEquals(5, loaded.getSession("SESSION1").getRecordRequestCount(RequestType.RECORD_VIEW, "PI_01"));

        dao.deleteUsageStatistics(stats.getId());
    }

    /**
     * @see DailySessionUsageStatistics#getSession(String)
     * @verifies persistence delete
     */
    @Test
    void getSession_shouldPersistenceDelete() throws DAOException {
        // Verify that after deleting persisted statistics, getSession returns null on reload
        LocalDate date = LocalDate.now();
        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");

        SessionUsageStatistics session = new SessionUsageStatistics("SESS_DEL", "TestAgent", "127.0.0.1");
        session.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_01", 3);
        stats.addSession(session);

        dao.addUsageStatistics(stats);
        assertNotNull(dao.getUsageStatistics(date));

        dao.deleteUsageStatistics(stats.getId());
        assertNull(dao.getUsageStatistics(date));
    }

    /**
     * @see DailySessionUsageStatistics#addSession(SessionUsageStatistics)
     * @verifies get date range
     */
    @Test
    void addSession_shouldGetDateRange() throws DAOException {
        // Verify that sessions added to stats for different dates are retrievable by date range
        LocalDate date1 = LocalDate.of(2023, Month.MARCH, 1);
        LocalDate date2 = LocalDate.of(2023, Month.MARCH, 15);

        DailySessionUsageStatistics stats1 = new DailySessionUsageStatistics(date1, "viewer-test");
        stats1.addSession(new SessionUsageStatistics("S1", "Agent1", "10.0.0.1"));
        dao.addUsageStatistics(stats1);

        DailySessionUsageStatistics stats2 = new DailySessionUsageStatistics(date2, "viewer-test");
        stats2.addSession(new SessionUsageStatistics("S2", "Agent2", "10.0.0.2"));
        dao.addUsageStatistics(stats2);

        List<DailySessionUsageStatistics> range = dao.getUsageStatistics(
                LocalDate.of(2023, Month.MARCH, 1), LocalDate.of(2023, Month.MARCH, 31));
        assertEquals(2, range.size());

        dao.deleteUsageStatistics(stats1.getId());
        dao.deleteUsageStatistics(stats2.getId());
    }

    /**
     * @see DailySessionUsageStatistics#getTotalRequestCount(RequestType, String)
     * @verifies test counting
     */
    @Test
    void getTotalRequestCount_shouldTestCounting() {
        // Verify that getTotalRequestCount sums request counts across multiple sessions
        LocalDate date = LocalDate.now();
        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");

        SessionUsageStatistics session1 = new SessionUsageStatistics("A", "Firefox", "10.0.0.1");
        session1.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_X", 10);
        stats.addSession(session1);

        SessionUsageStatistics session2 = new SessionUsageStatistics("B", "Chrome", "10.0.0.2");
        session2.setRecordRequectCount(RequestType.RECORD_VIEW, "PI_X", 20);
        stats.addSession(session2);

        assertEquals(30L, stats.getTotalRequestCount(RequestType.RECORD_VIEW, "PI_X"));
        assertEquals(0L, stats.getTotalRequestCount(RequestType.FILE_DOWNLOAD, "PI_X"));
    }

    /**
     * @verifies return statistics filtered by date range
     */
    @Test
    void getUsageStatistics_shouldReturnStatisticsFilteredByDateRange() throws DAOException {

        RequestType type = RequestType.RECORD_VIEW;
        
        
        {
            LocalDate date = LocalDate.of(2022, Month.JUNE, 30);
            DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
            
            SessionUsageStatistics session1 = new SessionUsageStatistics("ABCD", "Ubuntu Firefox", "168.178.192.2");
            session1.setRecordRequectCount(type, "PI_01", 7);
            session1.setRecordRequectCount(type, "PI_02", 3);
            stats.addSession(session1);
            
            SessionUsageStatistics session2 = new SessionUsageStatistics("EFGH", "Ubuntu Chrome", "168.178.192.3");
            session2.setRecordRequectCount(type, "PI_01", 2);
            session2.setRecordRequectCount(type, "PI_03", 4);
            stats.addSession(session2);
    
            dao.addUsageStatistics(stats);
        }
        {
            LocalDate date = LocalDate.of(2022, Month.JULY, 1);
            DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
            
            SessionUsageStatistics session1 = new SessionUsageStatistics("ABCD", "Ubuntu Firefox", "168.178.192.2");
            session1.setRecordRequectCount(type, "PI_01", 7);
            session1.setRecordRequectCount(type, "PI_02", 3);
            stats.addSession(session1);
            
            SessionUsageStatistics session2 = new SessionUsageStatistics("EFGH", "Ubuntu Chrome", "168.178.192.3");
            session2.setRecordRequectCount(type, "PI_01", 2);
            session2.setRecordRequectCount(type, "PI_03", 4);
            stats.addSession(session2);
    
            dao.addUsageStatistics(stats);
        }
        {
            LocalDate date = LocalDate.of(2022, Month.JULY, 4);
            DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
            
            SessionUsageStatistics session1 = new SessionUsageStatistics("ABCD", "Ubuntu Firefox", "168.178.192.2");
            session1.setRecordRequectCount(type, "PI_01", 7);
            session1.setRecordRequectCount(type, "PI_02", 3);
            stats.addSession(session1);
            
            SessionUsageStatistics session2 = new SessionUsageStatistics("EFGH", "Ubuntu Chrome", "168.178.192.3");
            session2.setRecordRequectCount(type, "PI_01", 2);
            session2.setRecordRequectCount(type, "PI_03", 4);
            stats.addSession(session2);
    
            dao.addUsageStatistics(stats);
        }
        
        List<DailySessionUsageStatistics> statsJuly = dao.getUsageStatistics(LocalDate.of(2022, Month.JULY, 1), LocalDate.of(2022, Month.AUGUST, 1));
        assertEquals(2, statsJuly.size());
        
        List<DailySessionUsageStatistics> statsJune = dao.getUsageStatistics(LocalDate.of(2022, Month.JUNE, 30), LocalDate.of(2022, Month.JUNE, 30));
        assertEquals(1, statsJune.size());
    }


}
