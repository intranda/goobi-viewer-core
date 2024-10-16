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
package io.goobi.viewer.model.job.download;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

public final class DownloadJobTools {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(DownloadJobTools.class);

    /** Private constructor. */
    private DownloadJobTools() {
        //
    }

    /**
     * Delete all jobs and associated files for the given record identifier.
     * 
     * @param pi Record identifier
     * @return Number of removed jobs
     * @throws DAOException
     * @should delete all finished jobs for record
     */
    public static int removeJobsForRecord(String pi) throws DAOException {
        List<DownloadJob> jobs = DataManager.getInstance().getDao().getDownloadJobsForPi(pi);
        if (jobs.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (DownloadJob job : jobs) {
            switch (job.getStatus()) {
                case READY:
                case DELETED:
                case ERROR:
                    if (DataManager.getInstance().getDao().deleteDownloadJob(job)) {
                        job.deleteFile();
                        count++;
                    }
                    break;
                default:
                    break;
            }

        }

        return count;
    }

    /**
     * <p>
     * cleanupExpiredDownloads.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should delete expired jobs correctly
     * @should delete file correctly
     * @deprecated Only used in deprecated method {@link DownloadJob#checkDownload(String, String, String, String, String, long)}
     */
    @Deprecated(since = "24.10")
    public static int cleanupExpiredDownloads() throws DAOException {
        List<DownloadJob> jobs = DataManager.getInstance().getDao().getAllDownloadJobs();
        if (jobs.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (DownloadJob job : jobs) {
            if (job.isExpired()) {
                if (DataManager.getInstance().getDao().deleteDownloadJob(job)) {
                    job.deleteFile();
                    count++;
                }
            }
        }

        logger.info("Deleted {} expired download jobs.", count);
        return count;
    }

    /**
     * <p>
     * getDownloadFileStatic.
     * </p>
     *
     * @param identifier the identifier of the download
     * @param type either "pdf" or "epub"
     * @param extension a {@link java.lang.String} object.
     * @return The Download location file, ending with ".pdf" or ".epub" depending on type
     * @throws java.lang.IllegalArgumentException If the hash is null, empty or blank, or if the type is not "epub" or "pdf"
     */
    public static File getDownloadFileStatic(String identifier, String type, String extension) {
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("Cannot determine download path for empty identifier");
        }
        if (!(EPUBDownloadJob.LOCAL_TYPE.equals(type) || PDFDownloadJob.LOCAL_TYPE.equals(type))) {
            throw new IllegalArgumentException("Unknown download type: " + type);
        }
        File folder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(type));
        return new File(folder, identifier + extension);
    }
}
