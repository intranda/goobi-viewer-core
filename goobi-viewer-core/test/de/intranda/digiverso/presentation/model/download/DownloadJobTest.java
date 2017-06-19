/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.model.download;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DownloadException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;

public class DownloadJobTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see DownloadJob#cleanupExpiredDownloads()
     * @verifies delete expired jobs correctly
     */
    @Test
    public void cleanupExpiredDownloads_shouldDeleteExpiredJobsCorrectly() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());

        DownloadJob job = new PDFDownloadJob("PI 3", null, new Date(), 3000000);
        Assert.assertTrue(DataManager.getInstance().getDao().addDownloadJob(job));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllDownloadJobs().size());
        Long id = job.getId();
        Assert.assertNotNull(id);

        // The two jobs in the static DB should be removed while the new one should remain
        Assert.assertEquals(2, DownloadJob.cleanupExpiredDownloads());
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllDownloadJobs().size());
        Assert.assertNotNull(DataManager.getInstance().getDao().getDownloadJob(id));
    }

    /**
     * @see DownloadJob#isExpired()
     * @verifies return correct value
     */
    @Test
    public void isExpired_shouldReturnCorrectValue() throws Exception {
        DownloadJob job = new PDFDownloadJob("PI 3", null, new Date(), 0);
        Thread.sleep(5);
        Assert.assertTrue(job.isExpired());
        job.setTtl(30000);
        Assert.assertFalse(job.isExpired());
    }

    /**
     * @see DownloadJob#generateDownloadJobId(String[])
     * @verifies generate same id from same criteria
     */
    @Test
    public void generateDownloadJobId_shouldGenerateSameIdFromSameCriteria() throws Exception {
        String hash = "78acb5991aaf0fee0329b673e985ce82";
        String crit1 = "PPN123456789";
        String crit2 = "LOG_0000";
        Assert.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
        Assert.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
        Assert.assertEquals(hash, DownloadJob.generateDownloadJobId(crit1, crit2));
    }

    //    @Test
    public void testSendJobToTaskManager() throws PresentationException, IndexUnreachableException {
        boolean triggered = false;
        try {
            PDFDownloadJob.triggerCreation("18979459_1830", "LOG_0003", "6c685d274f44f6e3ab8ef3f1c640bd01");
            triggered = true;
        } catch (DownloadException e) {

        }
        Assert.assertTrue("TaskManager job not triggered", triggered);
    }

    //    @Test
    public void testTaskManagerQueue() throws InterruptedException, PresentationException, IndexUnreachableException {
        List<String> logs = new ArrayList<>();
        logs.add("");
        logs.add("LOG_0004");
        logs.add("LOG_0005");
        logs.add("LOG_0006");
        logs.add("LOG_0007");
        logs.add("LOG_0008");
        String pi = "18979459_1830";
        for (String logId : logs) {
            PDFDownloadJob.triggerCreation(pi, logId, DownloadJob.generateDownloadJobId(pi, logId));
        }
        Thread.sleep(500);
        for (String logId : logs) {
            String id = DownloadJob.generateDownloadJobId(pi, logId);
            int jobsUntil = PDFDownloadJob.getPDFJobsInQueue(id);
            assertEquals(4, jobsUntil, 3);
        }
    }

    //    @Test
    //    public void testCreateEpub() throws PresentationException, IndexUnreachableException {
    //        String tempPath = "resources/test/data/viewer/download_epub";
    //        try {
    //            String msg = EPUBDownloadJob.triggerCreation("PPN648829383", "testIdentifier", "resources/test/data/viewer/download_epub");
    //            Assert.assertNull("EPUB not created", msg);
    //        } finally {
    //            File tempFolder = new File(tempPath);
    //            if (tempFolder.isDirectory()) {
    //                //                try {
    //                //                    FileUtils.deleteDirectory(tempFolder);
    //                //                } catch (IOException e) {
    //                //                }
    //            }
    //        }
    //    }
}
