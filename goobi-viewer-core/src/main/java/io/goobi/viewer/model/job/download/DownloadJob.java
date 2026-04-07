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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.viewer.Dataset;
import jakarta.mail.MessagingException;

public abstract class DownloadJob {

    public static final String FILE_EXTENSION_CREATING_LOCK = ".creating.lock";
    private final String pi;

    public DownloadJob(String pi) {
        this.pi = pi;
    }

    public DownloadJob(ViewerMessage message) {
        this.pi = message.getProperties().get("pi");
    }

    public abstract String getFilename();

    public abstract Path getPath();

    public abstract String getType();

    public String getPi() {
        return pi;
    }

    /**
     * Creates path to a temporary file to which the data is written. Only after completion is the file moved to #{@link DownloadJob#getPath()}
     * 
     * @return a path
     */
    protected Path getTempPath() {
        return getPath().getParent().resolve(getPath().getFileName().toString() + ".tmp");
    }

    public void create()
            throws PresentationException, IOException, IndexUnreachableException, RecordNotFoundException, ContentLibException {
        String cleanedPi = StringTools.cleanUserGeneratedData(getPi());
        Dataset work = DataFileTools.getDataset(cleanedPi);
        create(work);
    }

    public abstract void create(Dataset work) throws IOException, PresentationException, ContentLibException;

    public boolean createLock() throws IOException {
        try {
            Path lockFile = getPath().getParent().resolve(FilenameUtils.getBaseName(getFilename()) + FILE_EXTENSION_CREATING_LOCK);
            Files.createFile(lockFile);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        }
    }

    public boolean releaseLock() throws IOException {
        Path lockFile = getPath().getParent().resolve(FilenameUtils.getBaseName(getFilename()) + FILE_EXTENSION_CREATING_LOCK);
        return Files.deleteIfExists(lockFile);
    }

    public boolean isLocked() throws IOException {
        Path lockFile = getPath().getParent().resolve(FilenameUtils.getBaseName(getFilename()) + FILE_EXTENSION_CREATING_LOCK);
        return Files.exists(lockFile);
    }

    /**
     * notifyObservers.
     *
     * @param email
     * @param status a {@link io.goobi.viewer.model.job.JobStatus} object.
     * @param downloadUri the URI under which the download is made available
     * @return a boolean.
     * @throws java.io.UnsupportedEncodingException if any.
     * @throws jakarta.mail.MessagingException if any.
     */
    public boolean notifyObserver(String email, JobStatus status, URI downloadUri)
            throws UnsupportedEncodingException, MessagingException {

        if (StringUtils.isBlank(email)) {
            return false;
        }

        String subject = "Unknown status";
        String body = "";
        switch (status) {
            case READY:
                subject = ViewerResourceBundle.getTranslation("downloadReadySubject", null);
                body = ViewerResourceBundle.getTranslation("downloadReadyBody", null);
                if (body != null) {
                    body = body.replace("{0}", pi);
                    body = body.replace("{1}", downloadUri.toString());
                    body = body.replace("{4}", getType().toUpperCase());
                    try {
                        body = body.replace("{2}", DateTools.format(getExirationTime(), DateTools.FORMATTERISO8601DATE, false));
                        body = body.replace("{3}", DateTools.format(getExirationTime(), DateTools.FORMATTERISO8601DATE, false));
                    } catch (IOException e) {
                        //cannot replace expiration date since file time could not be accessed
                        body = body.replace("{2}", "?");
                        body = body.replace("{3}", "?");
                    }
                }
                break;
            case ERROR:
                subject = ViewerResourceBundle.getTranslation("downloadErrorSubject", null);
                body = ViewerResourceBundle.getTranslation("downloadErrorBody", null);
                if (body != null) {
                    body = body.replace("{0}", pi);
                    body = body.replace("{1}", DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress());
                    body = body.replace("{2}", getType().toUpperCase());
                }
                break;
            default:
                break;
        }
        if (subject != null) {
            subject = subject.replace("{0}", pi);
        }

        return NetTools.postMail(List.of(email), null, null, subject, body);
    }

    public LocalDateTime getExirationTime() throws IOException {
        if (Files.exists(getPath())) {
            FileTime lastAccessed = FileTools.getDateModified(getPath());
            Instant expirationTime = lastAccessed.toInstant().plus(DataManager.getInstance().getConfiguration().getDownloadPdfTimeToLive());
            return LocalDateTime.ofInstant(expirationTime, ZoneId.systemDefault());
        } else {
            throw new FileNotFoundException();
        }
    }

    /**
     * getTimeToLive.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTimeToLive() {
        Duration d = DataManager.getInstance().getConfiguration().getDownloadPdfTimeToLive();
        return String.format("%dd %d:%02d:%02d", d.toDays(), d.toHours() % 24, d.toMinutes() % 60, d.getSeconds() % 60);
    }

    /**
     * isExpired.
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isExpired() {

        try {
            return System.currentTimeMillis() > DateTools.getMillisFromLocalDateTime(getExirationTime(), false);
        } catch (IOException e) {
            return true;
        }
    }

    public static DownloadJob from(ViewerMessage message) throws IllegalArgumentException {

        String taskName = message.getTaskName();
        TaskType taskType = TaskType.valueOf(taskName);
        switch (taskType) {
            case TaskType.DOWNLOAD_PDF:
                return new PdfDownloadJob(message);
            case TaskType.DOWNLOAD_EPUB:
                return new EpubDownloadJob(message);
            default:
                throw new IllegalArgumentException(taskName + " is not known download task type");
        }
    }

    public abstract String getMimeType();

}
