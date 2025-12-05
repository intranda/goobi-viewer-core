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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.goobi.presentation.contentServlet.controller.GetMetsPdfAction;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.servlet.model.MetsPdfRequest;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.managedbeans.DownloadBean;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.job.JobStatus;
import io.goobi.viewer.model.viewer.Dataset;
import jakarta.mail.MessagingException;

public class PdfGenerator {

    public static final String TYPE = "pdf";

    private final String pi;
    private final String logId;
    private final String configVariant;
    private final boolean usePdfSource;
    private final Path path;
    private final long timeToLive = DownloadBean.getTimeToLive();
    private final LocalDateTime lastRequested = LocalDateTime.now();

    public PdfGenerator(ViewerMessage pdfMessage) {
        this.pi = pdfMessage.getProperties().get("pi");
        this.logId = pdfMessage.getProperties().get("logId");
        this.configVariant = pdfMessage.getProperties().get("configVariant");
        this.usePdfSource = Boolean.parseBoolean(pdfMessage.getProperties().getOrDefault("usePdfSource", "false"));
        String path = pdfMessage.getProperties().get("path");
        this.path = StringUtils.isNotBlank(path) ? Path.of(path) : generatePath();
    }

    public PdfGenerator(String pi, String logId, String configVariant, boolean usePdfSource) {
        this.pi = pi;
        this.logId = logId;
        this.configVariant = configVariant;
        this.usePdfSource = usePdfSource;
        this.path = generatePath();
    }

    public PdfGenerator(Path path) {
        this.pi = "";
        this.logId = "";
        this.configVariant = "2";
        this.usePdfSource = false;
        this.path = path;
    }

    public String getPi() {
        return pi;
    }

    public String getLogId() {
        return logId;
    }

    public String getConfigVariant() {
        return configVariant;
    }

    public boolean isUsePdfSource() {
        return usePdfSource;
    }

    public Path generatePath() {
        return Path.of(DataManager.getInstance().getConfiguration().getDownloadFolder(TYPE)).resolve(generateFilename());
    }

    public Path getPath() {
        return path;
    }

    public String getFilename() {
        return getPath().getFileName().toString();
    }

    private String generateFilename() {

        StringBuilder sb = new StringBuilder(pi);
        if (StringUtils.isNotBlank(logId)) {
            sb.append("_").append(logId);
        }

        if (this.isUsePdfSource()) {
            sb.append("_usePdfSources");
        } else {
            sb.append("_ignorePdfSources");
        }

        if (StringUtils.isNotBlank(this.configVariant)) {
            sb.append("_").append(this.configVariant);
        } else {
            sb.append("_default");
        }
        sb.append(".pdf");

        return sb.toString();
    }

    public void createPdf()
            throws PresentationException, IndexUnreachableException, RecordNotFoundException, IOException, ContentLibException, URISyntaxException {
        String cleanedPi = StringTools.cleanUserGeneratedData(pi);
        Dataset work = DataFileTools.getDataset(cleanedPi);
        createPdf(work);
    }

    public void createPdf(Dataset work)
            throws IOException, ContentLibException, URISyntaxException {
        createLock();
        try (FileOutputStream fos = new FileOutputStream(getPath().toFile())) {
            MetsPdfRequest request = createPdfRequest(work,
                    Optional.ofNullable(logId).filter(StringUtils::isNotBlank).filter(div -> !"-".equals(div)), usePdfSource, configVariant);
            GetMetsPdfAction action = new GetMetsPdfAction(ContentServerCacheManager.getInstance());
            action.writePdf(request, ContentServerConfiguration.getInstance(), fos, p -> {
            });
        } catch (IOException | ContentLibException | URISyntaxException e) {
            Files.deleteIfExists(getPath());
            throw e;
        } finally {
            releaseLock();
        }
    }

    public boolean createLock() throws IOException {
        try {
            Path lockFile = getPath().getParent().resolve(FilenameUtils.getBaseName(getFilename()) + ".creating.lock");
            Files.createFile(lockFile);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        }
    }

    public boolean releaseLock() throws IOException {
        Path lockFile = getPath().getParent().resolve(FilenameUtils.getBaseName(getFilename()) + ".creating.lock");
        return Files.deleteIfExists(lockFile);
    }

    public boolean isLocked() throws IOException {
        Path lockFile = getPath().getParent().resolve(FilenameUtils.getBaseName(getFilename()) + ".creating.lock");
        return Files.exists(lockFile);
    }

    public static MetsPdfRequest createPdfRequest(Dataset work, Optional<String> divId, boolean usePdfSource, String configVariant)
            throws URISyntaxException {
        Map<String, String> params = new HashMap<>();
        params.put("metsFile", work.getMetadataFilePath().toString());
        params.put("imageSource", work.getMediaFolderPath().getParent().toUri().toString());
        divId.ifPresent(id -> params.put("divID", id));

        if (usePdfSource && work.getPdfFolderPath() != null) {
            params.put("pdfSource", work.getPdfFolderPath().getParent().toUri().toString());
        }
        if (work.getAltoFolderPath() != null) {
            params.put("altoSource", work.getAltoFolderPath().getParent().toUri().toString());
        }
        if (StringUtils.isNotBlank(configVariant)) {
            params.put("configVariant", configVariant);
        }
        params.put("metsFileGroup", "PRESENTATION");
        params.put("goobiMetsFile", "false");
        MetsPdfRequest request = new MetsPdfRequest(params);
        return request;
    }

    /**
     * <p>
     * notifyObservers.
     * </p>
     * 
     * @param email
     * @param status a {@link io.goobi.viewer.model.job.JobStatus} object.
     * @param messageId Id of the MQ message to link to
     * @param message a {@link java.lang.String} object.
     * @return a boolean.
     * @throws java.io.UnsupportedEncodingException if any.
     * @throws jakarta.mail.MessagingException if any.
     */
    public boolean notifyObserver(String email, JobStatus status, String messageId, String message)
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
                    body = body.replace("{1}", DataManager.getInstance().getConfiguration().getDownloadUrl() + messageId + "/");
                    body = body.replace("{4}", TYPE.toUpperCase());
                    LocalDateTime exirationDate = lastRequested;
                    exirationDate = exirationDate.plus(this.timeToLive, ChronoUnit.MILLIS);
                    body = body.replace("{2}", DateTools.format(exirationDate, DateTools.FORMATTERISO8601DATE, false));
                    body = body.replace("{3}", DateTools.format(exirationDate, DateTools.FORMATTERISO8601DATE, false));
                }
                break;
            case ERROR:
                subject = ViewerResourceBundle.getTranslation("downloadErrorSubject", null);
                body = ViewerResourceBundle.getTranslation("downloadErrorBody", null);
                if (body != null) {
                    body = body.replace("{0}", pi);
                    body = body.replace("{1}", DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress());
                    body = body.replace("{2}", TYPE.toUpperCase());
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

    /**
     * <p>
     * getTimeToLive.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTimeToLive() {
        Duration d = Duration.ofMillis(this.timeToLive);
        return String.format("%dd %d:%02d:%02d", d.toDays(), d.toHours() % 24, d.toMinutes() % 60, d.getSeconds() % 60);
    }

    /**
     * <p>
     * isExpired.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isExpired() {
        if (lastRequested == null) {
            return false;
        }

        return System.currentTimeMillis() > DateTools.getMillisFromLocalDateTime(lastRequested, false) + this.timeToLive;
    }
}
