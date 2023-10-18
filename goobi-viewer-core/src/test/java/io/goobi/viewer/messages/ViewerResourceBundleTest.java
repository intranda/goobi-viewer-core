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

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringTools;

public class ViewerResourceBundleTest extends AbstractTest {

    @Test
    public void testGetDefaultLocale() {
        Assert.assertEquals(Locale.ENGLISH, ViewerResourceBundle.getDefaultLocale());
    }

    @Test
    public void testGetAllLocales() {
        List<Locale> locales = ViewerResourceBundle.getAllLocales();
        Assert.assertEquals(2, locales.size());
    }

    /**
     * @see ViewerResourceBundle#replaceParameters(String,String[])
     * @verifies return null if msg is null
     */
    @Test
    public void replaceParameters_shouldReturnNullIfMsgIsNull() throws Exception {
        Assert.assertNull(ViewerResourceBundle.replaceParameters(null, false, "one", "two", "three"));
    }

    /**
     * @see ViewerResourceBundle#replaceParameters(String,String[])
     * @verifies replace parameters correctly
     */
    @Test
    public void replaceParameters_shouldReplaceParametersCorrectly() throws Exception {
        Assert.assertEquals("one two three", ViewerResourceBundle.replaceParameters("{0} {1} {2}", false, "one", "two", "three"));
    }

    /**
     * @see ViewerResourceBundle#replaceParameters(String,boolean,String[])
     * @verifies remove remaining placeholders correctly
     */
    @Test
    public void replaceParameters_shouldRemoveRemainingPlaceholdersCorrectly() throws Exception {
        Assert.assertEquals("one two three {3}", ViewerResourceBundle.replaceParameters("{0} {1} {2} {3}", false, "one", "two", "three"));
        Assert.assertEquals("one two three", ViewerResourceBundle.replaceParameters("{0} {1} {2} {3}", true, "one", "two", "three"));
    }

    /**
     * @see ViewerResourceBundle#getAllLocales()
     * @verifies return English if no other locales found
     */
    @Test
    public void getAllLocales_shouldReturnEnglishForUnknownLanguages() throws Exception {

        String germanTranslation = ViewerResourceBundle.getTranslation("MD_AUTHOR", Locale.GERMAN);
        String englishTranslation = ViewerResourceBundle.getTranslation("MD_AUTHOR", Locale.ENGLISH);
        String frenchtranslation = ViewerResourceBundle.getTranslation("MD_AUTHOR", Locale.FRENCH);
        Assert.assertEquals(englishTranslation, frenchtranslation);
        Assert.assertNotEquals(englishTranslation, germanTranslation);
    }

    @Test
    public void testGetLocalesFromFile() throws FileNotFoundException, IOException, JDOMException {
        Path configPath = Paths.get("src/test/resources/localConfig/faces-config.xml");
        assumeTrue(Files.isRegularFile(configPath));
        List<Locale> locales = ViewerResourceBundle.getLocalesFromFile(configPath);
        assertEquals(6, locales.size());
        assertEquals(Locale.ENGLISH, locales.get(1));
        assertEquals(Locale.FRENCH, locales.get(3));
    }

    @Test
    public void testGetTranslation() {
        String autor = ViewerResourceBundle.getTranslation("MD_AUTHOR", Locale.GERMAN);
        Assert.assertEquals("Autor", autor);
    }

    @Test
    public void testGetTranslations() {
        IMetadataValue translations = ViewerResourceBundle.getTranslations("MD_AUTHOR");
        Assert.assertTrue(translations instanceof MultiLanguageMetadataValue);
        Assert.assertEquals("Author", translations.getValue("en").orElse(""));
        Assert.assertEquals("Autor", translations.getValue("de").orElse(""));
    }

    /**
     * @see ViewerResourceBundle#createLocalMessageFiles(List)
     * @verifies create files correctly
     */
    @Test
    public void createLocalMessageFiles_shouldCreateFilesCorrectly() throws Exception {
        List<Locale> locales = Arrays.asList(new Locale[] { Locale.ENGLISH, Locale.GERMAN });
        Assert.assertEquals(2, locales.size());

        // Create config folder
        Path configFolder = Paths.get(DataManager.getInstance().getConfiguration().getConfigLocalPath());
        try {
            if (!Files.exists(configFolder)) {
                Files.createDirectories(configFolder);
            }
            Assert.assertTrue(Files.isDirectory(configFolder));

            // Verify files do not exist yet
            for (Locale locale : locales) {
                Path path =
                        Paths.get(DataManager.getInstance().getConfiguration().getConfigLocalPath(),
                                "messages_" + locale.getLanguage() + ".properties");
                Assert.assertFalse(Files.exists(path));
            }

            ViewerResourceBundle.createLocalMessageFiles();
            // Verify files have been created
            for (Locale locale : locales) {
                Path path =
                        Paths.get(DataManager.getInstance().getConfiguration().getConfigLocalPath(),
                                "messages_" + locale.getLanguage() + ".properties");
                Assert.assertTrue(Files.isRegularFile(path));
            }
        } finally {
            if (Files.exists(configFolder)) {
                FileUtils.deleteDirectory(configFolder.toFile());
            }
        }
    }

    /**
     * @see ViewerResourceBundle#getFallbackLocale()
     * @verifies return locale for configured fallback language
     */
    @Test
    public void getFallbackLocale_shouldReturnLocaleForConfiguredFallbackLanguage() throws Exception {
        Assert.assertEquals(Locale.GERMAN, ViewerResourceBundle.getFallbackLocale());
    }

    /**
     * @see ViewerResourceBundle#getFallbackLocale()
     * @verifies return English if no fallback language configured
     */
    @Test
    public void getFallbackLocale_shouldReturnEnglishIfNoFallbackLanguageConfigured() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("viewer.fallbackDefaultLanguage", null);
        Assert.assertEquals(Locale.ENGLISH, ViewerResourceBundle.getFallbackLocale());
    }

    /**
     * @see ViewerResourceBundle#updateLocalMessageKey(String,String,String)
     * @verifies preserve spaces
     */
    @Test
    public void updateLocalMessageKey_shouldPreserveSpaces() throws Exception {
        DataManager.getInstance().getConfiguration().overrideValue("configFolder", "target/temp");

        File tempDir = new File("target/temp");
        Assert.assertTrue(tempDir.isDirectory() || tempDir.mkdirs());

        Assert.assertTrue(ViewerResourceBundle.updateLocalMessageKey("foo", "foo, bar", "en"));

        File file = new File(tempDir, "messages_en.properties");
        Assert.assertTrue(file.isFile());
        String fileContents = FileTools.getStringFromFile(file, StringTools.DEFAULT_ENCODING);
        Assert.assertEquals("foo = foo, bar", fileContents);

    }
}
