/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
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

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.LicenseDescription;
import io.goobi.viewer.controller.language.Language;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.misc.DCRecordWriter;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class CreateRecordBean implements Serializable {

    private static final long serialVersionUID = -8052248087187114268L;
    private static final Logger logger = LoggerFactory.getLogger(CreateRecordBean.class);

    private String title;
    private String description;
    private Language language;
    private String date;
    private String creator;
    private String collection;
    private String accessCondition;
    private String license;

    private final Path tempImagesFolder;
    private final String uuid;

    public CreateRecordBean() {
        String languageCode = BeanUtils.getNavigationHelper().getLocale().getLanguage();
        this.language = DataManager.getInstance().getLanguageHelper().getLanguage(languageCode);

        this.uuid = createUUID();

        this.tempImagesFolder = createTempImagesDirectory();
    }

    /**
     * 
     */
    public Path createTempImagesDirectory() {
        try {
            return Files.createTempDirectory(this.uuid);
        } catch (IOException e) {
            Messages.error(ViewerResourceBundle.getTranslationWithParameters("admin__create_record__write_record__error", null,
                    "Failed to create images temp directory: " + e.getMessage()));
            return null;
        }
    }

    /**
     * @return the tempImagesFolder
     */
    public Path getTempImagesFolder() {
        return tempImagesFolder;
    }

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
    public Language getLanguage() {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(Language language) {
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

    public String saveRecord() {
        DCRecordWriter writer = generateDCRecord();
        Path hotfolder = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder());
        Path mediaFolder = hotfolder.resolve(getUuid());
        try {
            if (Files.exists(mediaFolder)) {
                addFiles(writer, mediaFolder);
            }
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

    public List<Language> getPossibleLanguages() {
        List<Language> languages = DataManager.getInstance().getLanguageHelper().getMajorLanguages();
        Locale locale = BeanUtils.getLocale();
        languages.sort((l1, l2) -> l1.getName(locale).compareTo(l2.getName(locale)));
        return languages;
    }

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
        writer.addDCMetadata("language", getLanguage().getIsoCode());
        writer.addDCMetadata("creator", getCreator());
        writer.addDCMetadata("identifier", getUuid());
        writer.addDCMetadata("subject", getCollection());
        writer.addDCMetadata("date", getDate().toString());
        writer.addDCMetadata("rights", getLicense());
        writer.addDCMetadata("rights", getAccessCondition());

        return writer;
    }

    /**
     * TODO: implement
     * 
     * @return
     */
    private String createUUID() {
        return UUID.randomUUID().toString();
    }

    public static void main(String[] args) throws InterruptedException {
        String name = "Mein Titel";

        String source1 = DCRecordWriter.namespaceDC.getURI() + name;
        String id1 = UUID.nameUUIDFromBytes(source1.getBytes()).toString();

        Thread.sleep(632);

        String source2 = DCRecordWriter.namespaceDC.getURI() + name;
        String id2 = UUID.nameUUIDFromBytes(source2.getBytes()).toString();

        System.out.println(id1);
        System.out.println(id2);

    }

    /**
     * Delete the {@link #tempImagesFolder} with all contained files if it still exists
     */
    @Override
    protected void finalize() throws Throwable {
        if (this.tempImagesFolder != null && Files.exists(tempImagesFolder)) {
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
