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
package io.goobi.viewer.model.download;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;

public class DownloadJobToolsTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see DownloadJobTools#removeJobsForRecord(String)
     * @verifies delete all finished jobs for record
     */
    @Test
    public void removeJobsForRecord_shouldDeleteAllFinishedJobsForRecord() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("epub.downloadFolder", "target");

        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());

        DownloadJob job = DataManager.getInstance().getDao().getDownloadJob(2);
        Assert.assertNotNull(job);
        Path path = DownloadJobTools.getDownloadFileStatic(job.getIdentifier(), job.getType(), job.getFileExtension()).toPath();
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        Assert.assertTrue(Files.isRegularFile(path));

        Assert.assertEquals(1, DownloadJobTools.removeJobsForRecord("PI_1"));
        Assert.assertFalse(Files.exists(path));
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllDownloadJobs().size());
        Assert.assertNull(DataManager.getInstance().getDao().getDownloadJob(2));
    }

    /**
     * @see DownloadJobTools#cleanupExpiredDownloads()
     * @verifies delete expired jobs correctly
     */
    @Test
    public void cleanupExpiredDownloads_shouldDeleteExpiredJobsCorrectly() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());

        DownloadJob job = new PDFDownloadJob("PI_3", null, LocalDateTime.now(), 3000000);
        Assert.assertTrue(DataManager.getInstance().getDao().addDownloadJob(job));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllDownloadJobs().size());
        Long id = job.getId();
        Assert.assertNotNull(id);

        // The two jobs in the static DB should be removed while the new one should remain
        Assert.assertEquals(2, DownloadJobTools.cleanupExpiredDownloads());
        Assert.assertEquals(1, DataManager.getInstance().getDao().getAllDownloadJobs().size());
        Assert.assertNotNull(DataManager.getInstance().getDao().getDownloadJob(id));
    }

    /**
     * @see DownloadJobTools#cleanupExpiredDownloads()
     * @verifies delete file correctly
     */
    @Test
    public void cleanupExpiredDownloads_shouldDeleteFileCorrectly() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("pdf.downloadFolder", "target");

        Assert.assertEquals(2, DataManager.getInstance().getDao().getAllDownloadJobs().size());

        DownloadJob job = new PDFDownloadJob("PI_3", null, LocalDateTime.now().minusDays(2), 86400000);
        Assert.assertTrue(job.isExpired());
        Assert.assertTrue(DataManager.getInstance().getDao().addDownloadJob(job));
        Assert.assertEquals(3, DataManager.getInstance().getDao().getAllDownloadJobs().size());
        Long id = job.getId();
        Assert.assertNotNull(id);

        Path path = DownloadJobTools.getDownloadFileStatic(job.getIdentifier(), job.getType(), job.getFileExtension()).toPath();
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        Assert.assertTrue(Files.isRegularFile(path));

        Assert.assertEquals(3, DownloadJobTools.cleanupExpiredDownloads());
        Assert.assertFalse(Files.exists(path));
    }
}
