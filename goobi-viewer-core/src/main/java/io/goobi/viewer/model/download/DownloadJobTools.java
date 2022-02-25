package io.goobi.viewer.model.download;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

public class DownloadJobTools {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(DownloadJobTools.class);

    /**
     * Delete all jobs and associated files for the given record identifier.
     * 
     * @param pi Record identifier
     * @return
     * @throws DAOException
     * @should delete all jobs for record
     */
    public static int removeJobsForRecord(String pi) throws DAOException {
        List<DownloadJob> jobs = DataManager.getInstance().getDao().getDownloadJobsForPi(pi);
        if (jobs.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (DownloadJob job : jobs) {
            if (DataManager.getInstance().getDao().deleteDownloadJob(job)) {
                job.deleteFile();
                count++;
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
     */
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
    protected static File getDownloadFileStatic(String identifier, String type, String extension) {
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("Cannot determine download path for empty identifier");
        }
        if (!(EPUBDownloadJob.TYPE.equals(type) || PDFDownloadJob.TYPE.equals(type))) {
            throw new IllegalArgumentException("Unknown download type: " + type);
        }
        File folder = new File(DataManager.getInstance().getConfiguration().getDownloadFolder(type));
        return new File(folder, identifier + extension);
    }
}
