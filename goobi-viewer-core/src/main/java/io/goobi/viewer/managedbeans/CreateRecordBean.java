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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PreDestroy;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.LicenseDescription;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.misc.DCRecordWriter;
import io.goobi.viewer.model.translations.language.Language;

/**
 * Bean for uploading Dublin Core records.
 *
 * @author Florian Alpers
 */
@Named
@ViewScoped
public class CreateRecordBean implements Serializable {

    private static final long serialVersionUID = -8052248087187114268L;
    private static final Logger logger = LogManager.getLogger(CreateRecordBean.class);

    private String title;
    private String description;
    private String language;
    private String date;
    private String creator;
    private String collection;
    private String accessCondition;
    private String license;

    private final Path tempImagesFolder;
    private final String uuid;

    /**
     * Constructor. Generates a random uuid for the record
     */
    public CreateRecordBean() {
        this.language = "";

        this.uuid = createUUID();

        this.tempImagesFolder = getTempImagesDirectory();
    }

    /**
     * Get the path to a folder named <uuid>_tif within the temp_media directory.
     *
     * @return a folder within the viewer temp_media directory
     */
    private Path getTempImagesDirectory() {
        return Paths.get(DataManager.getInstance().getConfiguration().getViewerHome())
                .resolve(DataManager.getInstance().getConfiguration().getTempMediaFolder())
                .resolve(uuid + "_tif");
    }

    
    public Path getTempImagesFolder() {
        return tempImagesFolder;
    }

    /**
     *
     * @return the generated UUID for the current record
     */
    public String getUuid() {
        return this.uuid;
    }

    
    public String getTitle() {
        return title;
    }

    
    public void setTitle(String title) {
        this.title = title;
    }

    
    public String getDescription() {
        return description;
    }

    
    public void setDescription(String description) {
        this.description = description;
    }

    
    public String getLanguage() {
        return language;
    }

    
    public void setLanguage(String language) {
        this.language = language;
    }

    
    public String getDate() {
        return date;
    }

    
    public void setDate(String date) {
        this.date = date;
    }

    
    public String getCreator() {
        return creator;
    }

    
    public void setCreator(String creator) {
        this.creator = creator;
    }

    
    public String getCollection() {
        return collection;
    }

    
    public void setCollection(String collection) {
        this.collection = collection;
    }

    
    public String getAccessCondition() {
        return accessCondition;
    }

    
    public void setAccessCondition(String accessCondition) {
        this.accessCondition = accessCondition;
    }

    /*

     */
    public String getLicense() {
        return license;
    }

    
    public void setLicense(String license) {
        this.license = license;
    }

    /**
     * Add any uploaded images to the record, move the images folder from temp_media to hotfolder,
     * and write the record as Dublin Core xml to the viewer hotfolder.
     *
     * @return the url of the create record page to allow creating a new record
     */
    public String saveRecord() {
        DCRecordWriter writer = generateDCRecord();

        try {
            if (Files.exists(tempImagesFolder)) {
                addFiles(writer, tempImagesFolder);
            }
            Path hotfolder = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder());
            Files.move(tempImagesFolder, hotfolder.resolve(tempImagesFolder.getFileName()));
            writer.write(hotfolder);
            Messages.info(ViewerResourceBundle.getTranslationWithParameters("admin__create_record__write_record__success", null, true,
                    writer.getMetadataValue("identifier")));
            return "pretty:adminCreateRecord";
        } catch (IOException e) {
            Messages.error(
                    ViewerResourceBundle.getTranslationWithParameters("admin__create_record__write_record__error", null, true, e.getMessage()));
            return "";
        }

    }

    /**
     * Write filenames of files within mediaFolder as "dc:relation" metadata to writer.
     *
     * @param writer DC record writer to receive file relation metadata
     * @param mediaFolder folder whose files are added as dc:relation entries
     * @throws IOException
     */
    private static void addFiles(DCRecordWriter writer, Path mediaFolder) throws IOException {
        try (Stream<Path> stream = Files.list(mediaFolder)) {
            stream.sorted().forEach(path -> {
                if (Files.isRegularFile(path)) {
                    writer.addDCMetadata("relation", path.getFileName().toString());
                }
            });
        }

    }

    /**
     * @return A list of possible languages to use for the record
     */
    public List<Language> getPossibleLanguages() {
        List<Language> languages = DataManager.getInstance().getLanguageHelper().getMajorLanguages();
        Locale locale = BeanUtils.getLocale();
        languages.sort((l1, l2) -> l1.getName(locale).compareTo(l2.getName(locale)));
        return languages;
    }

    /**
     * @return A list of possible licenses to use for the record
     */
    public List<LicenseDescription> getPossibleLicenses() {
        return DataManager.getInstance().getConfiguration().getLicenseDescriptions();
    }

    /**
     * Create a {@link DCRecordWriter} instance containing all metadata of the bean as dc metadata.
     *
     * @return the{@link DCRecordWriter}
     */
    protected DCRecordWriter generateDCRecord() {
        DCRecordWriter writer = new DCRecordWriter();
        writer.addDCMetadata("title", getTitle());
        writer.addDCMetadata("description", getDescription());
        writer.addDCMetadata("language", getLanguage());
        writer.addDCMetadata("creator", getCreator());
        writer.addDCMetadata("identifier", getUuid());
        writer.addDCMetadata("subject", getCollection());
        writer.addDCMetadata("date", getDate());
        writer.addDCMetadata("rights", getLicense());
        writer.addDCMetadata("rights", getAccessCondition());

        return writer;
    }

    /**
     * @return a UUID created by {@link UUID#randomUUID()}
     */
    private static String createUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Marks the record as not ready for indexing and delete all associated images.
     *
     * @return the url of the create record page
     */
    public String reset() {
        destroy();
        return "pretty:adminCreateRecord";
    }

    /**
     * Delete the {@link #tempImagesFolder} with all contained files if it still exists. Called when the user session ends
     */
    @PreDestroy
    public void destroy() {
        if (Files.exists(tempImagesFolder)) {
            try (Stream<Path> stream = Files.list(this.tempImagesFolder)) {
                List<Path> uploadedFiles = stream.collect(Collectors.toList());
                for (Path file : uploadedFiles) {
                    Files.delete(file);
                }
            } catch (IOException e) {
                logger.error("Error deleting uploaded files on bean finalization", e);
            } finally {
                try {
                    Files.delete(this.tempImagesFolder);
                } catch (IOException e) {
                    logger.error("Error deleting temp images folder on bean finalization", e);
                }
            }
        }
    }

}
