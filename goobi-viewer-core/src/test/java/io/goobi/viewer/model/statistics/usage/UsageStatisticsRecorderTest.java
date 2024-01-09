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

import java.time.LocalDate;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
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
public class UsageStatisticsRecorderTest extends AbstractDatabaseEnabledTest {
    
    volatile IDAO dao;
    
    @BeforeEach
    void before() throws Exception  {
        super.setUp();
        dao = DataManager.getInstance().getDao();
    }


    @Test
    void test_recordRequests() throws DAOException, InterruptedException {


        String pi1 = "PI_1";
        String pi2 = "PI_2";
        String pi3 = "PI_3";

        String session1 = "12345";
        String session2 = "67890";

        UsageStatisticsRecorder recorder = new UsageStatisticsRecorder(dao, DataManager.getInstance().getConfiguration(),  "viewer.goobi.io");
        Random random = new Random();

        Thread thread1 = new Thread(() -> {
            try {
                for (int i = 0; i < 25; i++) {
                    wait(random);
                    recorder.recordRequest(RequestType.RECORD_VIEW, pi1, session1, "", "");
                }
                recorder.recordRequest(RequestType.FILE_DOWNLOAD, pi1, session1, "", "");
                for (int i = 0; i < 15; i++) {
                    wait(random);
                    recorder.recordRequest(RequestType.RECORD_VIEW, pi2, session1, "", "");
                }
                recorder.recordRequest(RequestType.FILE_DOWNLOAD, pi2, session1, "", "");
                recorder.recordRequest(RequestType.FILE_DOWNLOAD, pi2, session1, "", "");
            } catch (InterruptedException e) {

            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    wait(random);
                    recorder.recordRequest(RequestType.RECORD_VIEW, pi3, session2, "", "");
                }
                recorder.recordRequest(RequestType.FILE_DOWNLOAD, pi3, session2, "", "");
                recorder.recordRequest(RequestType.FILE_DOWNLOAD, pi3, session2, "", "");
                for (int i = 0; i < 30; i++) {
                    wait(random);
                    recorder.recordRequest(RequestType.RECORD_VIEW, pi1, session2, "", "");
                }
                recorder.recordRequest(RequestType.FILE_DOWNLOAD, pi1, session2, "", "");
            } catch (InterruptedException e) {

            }
        });
        
        LocalDate date = LocalDate.now();
        thread1.start();
        thread2.start();
        
        thread1.join();
        thread2.join();
        DailySessionUsageStatistics stats = dao.getUsageStatistics(date);
        
        assertEquals(55l, stats.getTotalRequestCount(RequestType.RECORD_VIEW, pi1));
    }

    private void wait(Random random) throws InterruptedException {
        Thread.sleep(random.nextInt(100));
    }

}
