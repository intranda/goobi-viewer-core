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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.presentation.contentServlet.controller.GetMetsPdfAction;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.servlet.model.MetsPdfRequest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.Dataset;

public class PdfDownloadJob extends DownloadJob {

    private static final Logger logger = LogManager.getLogger(PdfDownloadJob.class);

    public static final String TYPE = "pdf";

    private final String logId;
    private final String configVariant;
    private final boolean usePdfSource;
    private final Path path;

    public PdfDownloadJob(ViewerMessage pdfMessage) {
        super(pdfMessage);
        this.logId = pdfMessage.getProperties().get("logId");
        this.configVariant = pdfMessage.getProperties().get("configVariant");
        this.usePdfSource = Boolean.parseBoolean(pdfMessage.getProperties().getOrDefault("usePdfSource", "false"));
        String path = pdfMessage.getProperties().get("path");
        this.path = StringUtils.isNotBlank(path) ? Path.of(path) : generatePath();
    }

    public PdfDownloadJob(String pi, String logId, String configVariant, boolean usePdfSource) {
        super(pi);
        this.logId = logId;
        this.configVariant = configVariant;
        this.usePdfSource = usePdfSource;
        this.path = generatePath();
    }

    public PdfDownloadJob(Path path) {
        super("");
        this.logId = "";
        this.configVariant = "2";
        this.usePdfSource = false;
        this.path = path;
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

    public Path getPath() {
        return path;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getFilename() {
        return getPath().getFileName().toString();
    }

    public void create(Dataset work)
            throws IOException, ContentLibException, PresentationException {
        createLock();
        Files.deleteIfExists(getTempPath());
        try (FileOutputStream fos = new FileOutputStream(getTempPath().toFile())) {
            MetsPdfRequest request = createPdfRequest(work,
                    Optional.ofNullable(logId).filter(StringUtils::isNotBlank).filter(div -> !"-".equals(div)), usePdfSource, configVariant);
            GetMetsPdfAction action = new GetMetsPdfAction(ContentServerCacheManager.getInstance());
            action.writePdf(request, ContentServerConfiguration.getInstance(), fos, p -> {
            });
            if (Files.isRegularFile(getTempPath())) {
                Files.move(getTempPath(), getPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new PresentationException("Generated pdf file " + getTempPath() + " not found");
            }
        } catch (IOException e) {
            Files.deleteIfExists(getPath());
            throw e;
        } catch (URISyntaxException e) {
            Files.deleteIfExists(getPath());
            throw new PresentationException(e.toString(), e);
        } finally {
            Files.deleteIfExists(getTempPath());
            releaseLock();
        }
    }

    private Path generatePath() {
        return Path.of(DataManager.getInstance().getConfiguration().getDownloadFolder(TYPE)).resolve(generateFilename());
    }

    private String generateFilename() {

        StringBuilder sb = new StringBuilder(getPi());
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

    private static MetsPdfRequest createPdfRequest(Dataset work, Optional<String> divId, boolean usePdfSource, String configVariant)
            throws URISyntaxException {

        boolean usePdfFiles = usePdfSource;
        Path pdfFolder = work.getPdfFolderPath();
        //If media folder contains PDFs, make sure the the contentServer may use them and set the pdf folder to the media folder
        if (containsPdfs(work.getMediaFolderPath())) {
            usePdfFiles = true;
            pdfFolder = work.getMediaFolderPath();
        }

        Map<String, String> params = new HashMap<>();
        params.put("metsFile", work.getMetadataFilePath().toString());
        params.put("imageSource", work.getMediaFolderPath().getParent().toUri().toString());
        divId.ifPresent(id -> params.put("divID", id));

        if (usePdfFiles && Files.exists(pdfFolder)) {
            params.put("pdfSource", pdfFolder.getParent().toUri().toString());
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

    static boolean containsPdfs(Path folder) {
        if (folder == null || !Files.exists(folder)) {
            return false;
        }
        try {
            return Files.list(folder).anyMatch(f -> f.getFileName().toString().matches("(?i).*\\.pdf"));
        } catch (IOException e) {
            logger.error("Error parsing folder {} to look for existing pdf files ", folder);
            return false;
        }
    }

    public static int removeFilesForRecord(String pi) {
        Path pdfFolder = Path.of(DataManager.getInstance().getConfiguration().getDownloadFolder(TYPE));

        List<Path> pdfFiles = getDownloadPdfFiles(pdfFolder);

        int filesRemoved = 0;
        for (Path pdfFile : pdfFiles) {
            if (pdfFile.startsWith(pi + "_")) {
                PdfDownloadJob job = new PdfDownloadJob(pdfFile);
                try {
                    if (!job.isLocked()) {
                        Files.deleteIfExists(pdfFile);
                        ++filesRemoved;
                    }
                } catch (IOException e) {
                    //continue
                }
            }
        }
        return filesRemoved;
    }

    private static List<Path> getDownloadPdfFiles(Path targetFolder) {
        try {
            if (Files.isDirectory(targetFolder)) {
                try (Stream<Path> paths = Files.list(targetFolder)) {
                    return paths
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                            .toList();
                }
            }
        } catch (IOException e) {
            //ignore. No pdfs to download
        }
        return Collections.emptyList();
    }

    @Override
    public String getMimeType() {
        return "application/pdf";
    }

}
