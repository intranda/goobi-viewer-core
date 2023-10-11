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
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
 * @author florian
 *
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
     * Get the path to a folder named <uuid>_tif within the temp_media directory
     *
     * @return a folder within the viewer temp_media directory
     */
    private Path getTempImagesDirectory() {
        Path targetDir = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome())
                .resolve(DataManager.getInstance().getConfiguration().getTempMediaFolder())
                .resolve(uuid + "_tif");
        return targetDir;
    }

    /**
     * @return the tempImagesFolder
     */
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

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * @return the creator
     */
    public String getCreator() {
        return creator;
    }

    /**
     * @param creator the creator to set
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @param collection the collection to set
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * @return the accessCondition
     */
    public String getAccessCondition() {
        return accessCondition;
    }

    /**
     * @param accessCondition the accessCondition to set
     */
    public void setAccessCondition(String accessCondition) {
        this.accessCondition = accessCondition;
    }

    /*
     * @return the license
     */
    public String getLicense() {
        return license;
    }

    /**
     * @param license the license to set
     */
    public void setLicense(String license) {
        this.license = license;
    }

    /**
     * Add any uploaded images to the record, move the images folder frm temp_media to hotfolder and write the record as Dublin Core xml to the viewer
     * hotfolder
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
            Messages.info(ViewerResourceBundle.getTranslationWithParameters("admin__create_record__write_record__success", null,
                    writer.getMetadataValue("identifier")));
            return "pretty:adminCreateRecord";
        } catch (IOException e) {
            Messages.error(ViewerResourceBundle.getTranslationWithParameters("admin__create_record__write_record__error", null, e.getMessage()));
            return "";
        }

    }

    /**
     * Write filenames of files within mediaFolder as "dc:relation" metadata to writer
     *
     * @param writer
     * @param mediaFolder
     * @throws IOException
     */
    private void addFiles(DCRecordWriter writer, Path mediaFolder) throws IOException {
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
     * Create a {@link DCRecordWriter} instance containing all metadata of the bean as dc metadata
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
        writer.addDCMetadata("date", getDate().toString());
        writer.addDCMetadata("rights", getLicense());
        writer.addDCMetadata("rights", getAccessCondition());

        return writer;
    }

    /**
     * @return a UUID created by {@link UUID#randomUUID()}
     */
    private String createUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Mark the record as not ready for indexing and delete all associated images
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
