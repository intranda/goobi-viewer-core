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
package io.goobi.viewer.messages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;

class ViewerResourceBundleTest extends AbstractTest {

    /**
     * @verifies return expected value for given input
     * @see ViewerResourceBundle#getDefaultLocale()
     */
    @Test
    void getDefaultLocale_shouldReturnExpectedValueForGivenInput() {
        Assertions.assertEquals(Locale.ENGLISH, ViewerResourceBundle.getDefaultLocale());
    }

    /**
     * @verifies return collection with 2 elements
     * @see ViewerResourceBundle#getAllLocales()
     */
    @Test
    void getAllLocales_shouldReturnCollectionWith2Elements() {
        List<Locale> locales = ViewerResourceBundle.getAllLocales();
        Assertions.assertEquals(2, locales.size());
    }

    /**
     * @see ViewerResourceBundle#replaceParameters(String,String[])
     * @verifies return null if msg is null
     */
    @Test
    void replaceParameters_shouldReturnNullIfMsgIsNull() {
        Assertions.assertNull(ViewerResourceBundle.replaceParameters(null, false, "one", "two", "three"));
    }

    /**
     * @see ViewerResourceBundle#replaceParameters(String,String[])
     * @verifies replace parameters correctly
     */
    @Test
    void replaceParameters_shouldReplaceParametersCorrectly() {
        Assertions.assertEquals("one two three", ViewerResourceBundle.replaceParameters("{0} {1} {2}", false, "one", "two", "three"));
    }

    /**
     * @see ViewerResourceBundle#replaceParameters(String,boolean,String[])
     * @verifies remove remaining placeholders correctly
     */
    @Test
    void replaceParameters_shouldRemoveRemainingPlaceholdersCorrectly() {
        Assertions.assertEquals("one two three {3}", ViewerResourceBundle.replaceParameters("{0} {1} {2} {3}", false, "one", "two", "three"));
        Assertions.assertEquals("one two three", ViewerResourceBundle.replaceParameters("{0} {1} {2} {3}", true, "one", "two", "three"));
    }

    /**
     * @see ViewerResourceBundle#getTranslation(String, Locale)
     * @verifies return english for unknown languages
     */
    @Test
    void getTranslation_shouldReturnEnglishForUnknownLanguages() {

        String germanTranslation = ViewerResourceBundle.getTranslation("MD_AUTHOR", Locale.GERMAN);
        String englishTranslation = ViewerResourceBundle.getTranslation("MD_AUTHOR", Locale.ENGLISH);
        String frenchtranslation = ViewerResourceBundle.getTranslation("MD_AUTHOR", Locale.FRENCH);
        Assertions.assertEquals(englishTranslation, frenchtranslation);
        Assertions.assertNotEquals(englishTranslation, germanTranslation);
    }

    /**
     * @verifies return collection with 6 elements
     */
    @Test
    void getLocalesFromFile_shouldReturnCollectionWith6Elements() throws IOException, JDOMException {
        Path configPath = Paths.get("src/test/resources/localConfig/faces-config.xml");
        Assertions.assertTrue(Files.isRegularFile(configPath));
        List<Locale> locales = ViewerResourceBundle.getLocalesFromFile(configPath);
        assertEquals(6, locales.size());
        assertEquals(Locale.ENGLISH, locales.get(1));
        assertEquals(Locale.FRENCH, locales.get(3));
    }

    /**
     * @verifies return Autor for given input
     * @see ViewerResourceBundle#getTranslation(final String, Locale)
     */
    @Test
    void getTranslation_shouldReturnAutorForGivenInput() {
        String autor = ViewerResourceBundle.getTranslation("MD_AUTHOR", Locale.GERMAN);
        Assertions.assertEquals("Autor", autor);
    }

    /**
     * @verifies return true for given input
     * @see ViewerResourceBundle#getTranslations(String)
     */
    @Test
    void getTranslations_shouldReturnTrueForGivenInput() {
        IMetadataValue translations = ViewerResourceBundle.getTranslations("MD_AUTHOR");
        Assertions.assertTrue(translations instanceof MultiLanguageMetadataValue);
        Assertions.assertEquals("Author", translations.getValue("en").orElse(""));
        Assertions.assertEquals("Autor", translations.getValue("de").orElse(""));
    }

    /**
     * @see ViewerResourceBundle#createLocalMessageFiles()
     * @verifies create locale-specific message properties files in the config folder
     */
    @Test
    void createLocalMessageFiles_shouldCreateLocaleSpecificMessagePropertiesFilesInTheConfigFolder() throws Exception {
        List<Locale> locales = Arrays.asList(new Locale[] { Locale.ENGLISH, Locale.GERMAN });
        Assertions.assertEquals(2, locales.size());

        String origConfigLocalPath = DataManager.getInstance().getConfiguration().getConfigLocalPath();
        DataManager.getInstance().getConfiguration().overrideValue("configFolder", "target/config_temp/");
        Assertions.assertEquals("target/config_temp/", DataManager.getInstance().getConfiguration().getConfigLocalPath());

        // Create config folder
        Path configFolder = Paths.get(DataManager.getInstance().getConfiguration().getConfigLocalPath());
        try {
            if (!Files.exists(configFolder)) {
                Files.createDirectories(configFolder);
            }
            Assertions.assertTrue(Files.isDirectory(configFolder));

            // Verify files do not exist yet
            for (Locale locale : locales) {
                Path path =
                        Paths.get(configFolder.toAbsolutePath().toString(), "messages_" + locale.getLanguage() + ".properties");
                Assertions.assertFalse(Files.exists(path));
            }

            ViewerResourceBundle.createLocalMessageFiles();
            // Verify files have been created
            for (Locale locale : locales) {
                Path path =
                        Paths.get(configFolder.toAbsolutePath().toString(), "messages_" + locale.getLanguage() + ".properties");
                Assertions.assertTrue(Files.isRegularFile(path));
            }
        } finally {
            if (Files.exists(configFolder)) {
                FileUtils.deleteDirectory(configFolder.toFile());
            }
            DataManager.getInstance().getConfiguration().overrideValue("configFolder", origConfigLocalPath);
        }
    }

    /**
     * @see ViewerResourceBundle#getFallbackLocale()
     * @verifies return locale for configured fallback language
     */
    @Test
    void getFallbackLocale_shouldReturnLocaleForConfiguredFallbackLanguage() {
        Assertions.assertEquals(Locale.GERMAN, ViewerResourceBundle.getFallbackLocale());
    }

    /**
     * @verifies return English if no fallback language configured
     */
    @Test
    void getFallbackLocale_shouldReturnEnglishIfNoFallbackLanguageConfigured() {
        DataManager.getInstance().getConfiguration().overrideValue("viewer.fallbackDefaultLanguage", null);
        Assertions.assertEquals(Locale.ENGLISH, ViewerResourceBundle.getFallbackLocale());
    }

    /**
     * @verifies preserve spaces
     */
    @Test
    void updateLocalMessageKey_shouldPreserveSpaces() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("configFolder", "target/temp");

        File tempDir = new File("target/temp");
        Assertions.assertTrue(tempDir.isDirectory() || tempDir.mkdirs());

        Assertions.assertTrue(ViewerResourceBundle.updateLocalMessageKey("foo", "foo, bar", "en"));

        File file = new File(tempDir, "messages_en.properties");
        Assertions.assertTrue(file.isFile());
        String fileContents = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assertions.assertEquals("foo = foo, bar", fileContents);

    }
}
