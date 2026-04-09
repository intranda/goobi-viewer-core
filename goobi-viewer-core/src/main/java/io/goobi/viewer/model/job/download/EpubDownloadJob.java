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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;

import de.intranda.digiverso.convert.EpubConverter;
import de.intranda.digiverso.convert.messages.MessagesHelper;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.viewer.Dataset;

/**
 * Download job that generates an EPUB file for a given digitized record using the intranda EPUB converter library.
 */
public class EpubDownloadJob extends DownloadJob {

    public static final String TYPE = "epub";

    private final Path path;

    public EpubDownloadJob(ViewerMessage epubMessage) {
        super(epubMessage);
        String path = epubMessage.getProperties().get("path");
        this.path = StringUtils.isNotBlank(path) ? Path.of(path) : generatePath();
    }

    public EpubDownloadJob(String pi) {
        super(pi);
        this.path = generatePath();
    }

    public EpubDownloadJob(Path path) {
        super("");
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public String getFilename() {
        return getPath().getFileName().toString();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void create(Dataset work) throws IOException, PresentationException {
        createLock();
        Path workDir = Path.of(DataManager.getInstance().getConfiguration().getTempFolder()).resolve("epub").resolve(getPi());
        Files.createDirectories(workDir);

        try (FileOutputStream fos = new FileOutputStream(getTempPath().toFile())) {
            EpubConverter converter = new EpubConverter();
            // Provide viewer's local messages directory to the epub library so it can look up translations.
            // MessagesHelper falls back to classpath bundle names that don't exist in this app if not configured.
            File messagesDir = new File(DataManager.getInstance().getConfiguration().getConfigLocalPath());
            MessagesHelper.getINSTANCE().setLocalBundle(messagesDir);
            Path xmlFolder = work.getAltoFolderPath();
            Path imageFolder = work.getMediaFolderPath();
            Path metsFile = work.getMetadataFilePath();
            boolean rightToLeft = false;
            Locale locale = IPolyglott.getDefaultLocale();

            if (Files.exists(xmlFolder)) {
                converter.writeEpubToOutputStream(workDir.toFile(), fos, xmlFolder.toFile(), imageFolder.toFile(), metsFile.toFile(), null, true,
                        true,
                        imageFolder != null && Files.exists(imageFolder), rightToLeft,
                        locale);
            }
            if (Files.isRegularFile(getTempPath())) {
                Files.move(getTempPath(), getPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new PresentationException("Generated epub file " + getTempPath() + " not found");
            }
        } catch (IOException e) {
            Files.deleteIfExists(getPath());
            throw e;
        } catch (JDOMException | XMLStreamException e) {
            Files.deleteIfExists(getPath());
            throw new PresentationException(e.toString(), e);
        } finally {
            if (Files.isDirectory(workDir)) {
                FileUtils.deleteDirectory(workDir.toFile());
            }
            Files.deleteIfExists(getTempPath());
            releaseLock();
        }

    }

    private Path generatePath() {
        return Path.of(DataManager.getInstance().getConfiguration().getDownloadFolder(TYPE)).resolve(generateFilename());
    }

    private String generateFilename() {

        StringBuilder sb = new StringBuilder(getPi());
        sb.append(".epub");

        return sb.toString();
    }

    @Override
    public String getMimeType() {
        return "application/epub+zip";
    }
}
