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
package io.goobi.viewer.model.job.mq;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetPdfAction;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.servlet.model.SinglePdfRequest;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.ProcessDataResolver;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.TaskType;

public class PrerenderPdfMessageHandler implements MessageHandler<MessageStatus> {

    private static final Logger logger = LogManager.getLogger(PrerenderPdfMessageHandler.class);
    private static final String PDF = "pdf";
    private static final String MEDIA = "media";
    private static final String ALTO = "alto";

    private final ProcessDataResolver processDataResolver;
    private final ContentServerConfiguration contentServerConfiguration;

    public PrerenderPdfMessageHandler() {
        this.processDataResolver = new ProcessDataResolver();
        this.contentServerConfiguration = ContentServerConfiguration.getInstance();
    }

    public PrerenderPdfMessageHandler(ProcessDataResolver processDataResolver, ContentServerConfiguration contentServerConfiguration) {
        this.processDataResolver = processDataResolver;
        this.contentServerConfiguration = contentServerConfiguration;
    }

    @Override
    public MessageStatus call(ViewerMessage ticket, MessageQueueManager queueManager) {

        String pi = ticket.getProperties().get("pi");
        String configVariant = ticket.getProperties().get("variant");
        boolean force = Boolean.parseBoolean(ticket.getProperties().get("force"));

        if (StringUtils.isNotBlank(pi)) {
            logger.trace("Starting task to prerender pdf files for PI {}, using config {}; force = {}", pi, this.contentServerConfiguration, force);
            try {
                if (!createPdfFiles(pi, configVariant, force)) {
                    return MessageStatus.ERROR;
                }
            } catch (IndexUnreachableException | PresentationException e) {
                logger.error("Failed to get data folders for PI {}. Reason: {}", pi, e.toString());
                return MessageStatus.ERROR;
            }
        }
        return MessageStatus.FINISH;
    }

    private boolean createPdfFiles(String pi, String configVariant, boolean force) throws PresentationException, IndexUnreachableException {
        Map<String, Path> dataFolders = processDataResolver.getDataFolders(pi, MEDIA, PDF, ALTO);
        Path imageFolder = dataFolders.get(MEDIA);
        Path pdfFolder = dataFolders.get(PDF);
        Path altoFolder = dataFolders.get(ALTO);
        if (imageFolder != null && pdfFolder != null && Files.exists(imageFolder)) {
            List<Path> imageFiles = FileTools.listFiles(imageFolder, FileTools.IMAGE_NAME_FILTER);
            if (imageFiles.isEmpty()) {
                logger.trace("No images in {}. Abandoning task", imageFolder);
            } else {
                return createPdfFiles(configVariant, pdfFolder, altoFolder, imageFiles, force);
            }
        }
        return true;
    }

    private boolean createPdfFiles(String configVariant, Path pdfFolder, Path altoFolder, List<Path> imageFiles, boolean force) {
        if (!Files.exists(pdfFolder)) {
            try {
                Files.createDirectories(pdfFolder);
            } catch (IOException e) {
                logger.error("Cannot create pdf directory: {}", e.toString());
                return false;
            }
        }
        for (Path imagePath : imageFiles) {
            try {
                createPdfFile(imagePath, pdfFolder, altoFolder, configVariant, force);
            } catch (PresentationException e) {
                logger.error("Error creating pdf for {}. Abandoning task", imagePath);
                return false;
            }
        }
        return true;
    }

    private boolean createPdfFile(Path imagePath, Path pdfFolder, Path altoFolder, String configVariant, boolean force) throws PresentationException {
        Map<String, String> params = Map.of(
                "config", configVariant,
                "ignoreCache", "true",
                "altoSource", Optional.ofNullable(altoFolder).map(f -> PathConverter.toURI(f.toAbsolutePath()).toString()).orElse(""),
                "imageSource", PathConverter.toURI(imagePath.getParent().toAbsolutePath()).toString());
        Path pdfPath = pdfFolder.resolve(FileTools.replaceExtension(imagePath.getFileName(), "pdf"));
        if (force || !Files.exists(pdfPath) || FileTools.isYoungerThan(imagePath, pdfPath)) {
            try (OutputStream out = Files.newOutputStream(pdfPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                SinglePdfRequest request = new SinglePdfRequest(imagePath.getFileName().toString(), params);
                new GetPdfAction().writePdf(request, this.contentServerConfiguration, out);
            } catch (ContentLibException | IOException | URISyntaxException e) {
                throw new PresentationException("Failed to create pdf file {} from {}. Reason: {}", pdfPath, imagePath, e.toString());
            }
            return true;
        } else {
            logger.trace("No pdf created at {}, it already exists", pdfPath);
            return false;
        }
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.PRERENDER_PDF.name();
    }

}
