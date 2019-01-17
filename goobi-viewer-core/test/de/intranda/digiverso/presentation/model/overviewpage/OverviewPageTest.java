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
package de.intranda.digiverso.presentation.model.overviewpage;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Locale;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.AbstractDatabaseAndSolrEnabledTest;
import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.XmlTools;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.viewer.StructElement;
import de.unigoettingen.sub.commons.util.file.FileUtils;

public class OverviewPageTest extends AbstractDatabaseAndSolrEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see OverviewPage#loadOverviewPage(StructElement,Locale)
     * @verifies load overview page correctly
     */
    @Test
    public void loadOverviewPage_shouldLoadOverviewPageCorrectly() throws Exception {
        StructElement structElement = new StructElement();
        structElement.setPi("PI 1");
        OverviewPage op = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op);
        Assert.assertEquals("PI 1", op.getPi());
        Assert.assertEquals("Hello, Overview", op.getDescription());
        Assert.assertEquals("pub", op.getPublicationText());
    }

    /**
     * @see OverviewPage#getPublicationNumPages()
     * @verifies return correct number
     */
    @Test
    public void getPublicationNumPages_shouldReturnCorrectNumber() throws Exception {
        OverviewPage op = new OverviewPage();
        op.setPublicationText("a" + OverviewPage.PAGE_BREAK_REGEX + "b" + OverviewPage.PAGE_BREAK_REGEX + "c");
        Assert.assertEquals(3, op.getPublicationTextNumPages());
    }

    /**
     * @see OverviewPage#copyFields(OverviewPage)
     * @verifies copy fields correctly
     */
    @Test
    public void copyFields_shouldCopyFieldsCorrectly() throws Exception {
        OverviewPage page1 = new OverviewPage();
        page1.setId(111L);
        page1.setConfigXml("XML");
        page1.setPublicationText("text");
        page1.setDateUpdated(new Date());

        OverviewPage page2 = new OverviewPage();
        page2.copyFields(page1);
        Assert.assertEquals(page1.getId(), page2.getId());
        Assert.assertEquals(page1.getPi(), page2.getPi());
        Assert.assertEquals(page1.getConfigXml(), page2.getConfigXml());
        Assert.assertEquals(page1.getPublicationText(), page2.getPublicationText());
        Assert.assertEquals(page1.getDateUpdated(), page2.getDateUpdated());
    }

    /**
     * @see OverviewPage#copyFields(OverviewPage)
     * @verifies throw IllegalArgumentException if sourceOverviewPage is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void copyFields_shouldThrowIllegalArgumentExceptionIfSourceOverviewPageIsNull() throws Exception {
        OverviewPage page = new OverviewPage();
        page.copyFields(null);
    }

    /**
     * @see OverviewPage#getExportFormat()
     * @verifies create export xml correctly
     */
    @Test
    public void getExportFormat_shouldCreateExportXmlCorrectly() throws Exception {
        OverviewPage page = DataManager.getInstance().getDao().getOverviewPage(1);
        Assert.assertNotNull(page);
        String exportString = page.getExportFormat();
        Assert.assertNotNull(exportString);
        try (StringReader sr = new StringReader(exportString)) {
            Document exportDoc = new SAXBuilder().build(sr);
            Assert.assertNotNull(exportDoc);
            Assert.assertNotNull(exportDoc.getRootElement());
            Assert.assertEquals("PI 1", exportDoc.getRootElement().getChildText("pi", null));
            Assert.assertEquals("pub", exportDoc.getRootElement().getChildText("publicationText", null));
        }
    }

    /**
     * @see OverviewPage#saveAction(User)
     * @verifies update metadata list correctly
     */
    @Test
    public void saveAction_shouldUpdateMetadataListCorrectly() throws Exception {
        StructElement structElement = new StructElement();
        structElement.setPi("PI 1");
        OverviewPage op = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op);

        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        op.saveAction(user, false);
    }

    /**
     * @see OverviewPage#saveAction(User)
     * @verifies update timestamp
     */
    @Test
    public void saveAction_shouldUpdateTimestamp() throws Exception {
        StructElement structElement = new StructElement();
        structElement.setPi("PI 1");
        OverviewPage op = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op);
        Date oldDate = op.getDateUpdated();

        op.setDescription("must be dirty to save");
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        op.saveAction(user, false);
        Assert.assertTrue(oldDate.getTime() < op.getDateUpdated().getTime());
    }

    /**
     * @see OverviewPage#saveAction(User)
     * @verifies update description correctly
     */
    @Test
    public void saveAction_shouldUpdateDescriptionCorrectly() throws Exception {
        StructElement structElement = new StructElement();
        structElement.setPi("PI 1");
        OverviewPage op = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op);
        Assert.assertEquals("Hello, Overview", op.getDescription());

        op.setDescription("Goodbye, Overview");
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        op.saveAction(user, false);

        Document config = XmlTools.getDocumentFromString(op.getConfigXml(), "utf-8");
        Assert.assertNotNull(config);
        Assert.assertEquals("Goodbye, Overview", config.getRootElement().getChildText("description"));
    }

    /**
     * @see OverviewPage#saveAction(User)
     * @verifies update publication text correctly
     */
    @Test
    public void saveAction_shouldUpdatePublicationTextCorrectly() throws Exception {
        StructElement structElement = new StructElement();
        structElement.setPi("PI 1");
        OverviewPage op = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op);
        Assert.assertEquals("Hello, Overview", op.getDescription());

        op.setDescription("Goodbye, Overview");
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        op.saveAction(user, false);

        Document config = XmlTools.getDocumentFromString(op.getConfigXml(), "utf-8");
        Assert.assertNotNull(config);
        Assert.assertEquals("Goodbye, Overview", config.getRootElement().getChildText("description"));
    }

    /**
     * @see OverviewPage#saveAction(User)
     * @verifies write to DB correctly
     */
    @Test
    public void saveAction_shouldWriteToDBCorrectly() throws Exception {
        StructElement structElement = new StructElement();
        structElement.setPi("PI 1");
        OverviewPage op = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op);
        Assert.assertEquals("Hello, Overview", op.getDescription());

        op.setDescription("Goodbye, Overview");
        op.setPublicationText("more literature");
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        op.saveAction(user, false);

        OverviewPage op2 = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op2);
        Assert.assertEquals(op.getDescription(), op2.getDescription());
        Assert.assertEquals(op.getPublicationText(), op2.getPublicationText());
    }

    /**
     * @see OverviewPage#saveAction(User)
     * @verifies add history entry
     */
    @Test
    public void saveAction_shouldAddHistoryEntry() throws Exception {
        StructElement structElement = new StructElement();
        structElement.setPi("PI 1");
        OverviewPage op = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op);
        Assert.assertEquals(3, op.getHistory().size());

        op.setDescription("Goodbye, Overview");
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        op.saveAction(user, false);

        Assert.assertEquals(4, op.getHistory().size());
        OverviewPageUpdate update = op.getHistory().get(0);
        Assert.assertEquals(op.getPi(), update.getPi());
        Assert.assertEquals(op.getDateUpdated(), update.getDateUpdated());
        Assert.assertEquals(user, update.getUpdatedBy());
        Assert.assertFalse(update.isMetadataChanged());
        Assert.assertTrue(update.isDescriptionChanged());
        Assert.assertFalse(update.isPublicationTextChanged());
    }

    /**
     * @see OverviewPage#saveAction(User)
     * @verifies reset edit modes
     */
    @Test
    public void saveAction_shouldResetEditModes() throws Exception {
        StructElement structElement = new StructElement();
        structElement.setPi("PI 1");
        OverviewPage op = OverviewPage.loadOverviewPage(structElement, null);
        Assert.assertNotNull(op);

        op.getMetadata().add(new Metadata("MD_NEW", "", "val"));
        op.setDescription("Goodbye, Overview");
        op.setPublicationText("new text");
        User user = DataManager.getInstance().getDao().getUser(1);
        Assert.assertNotNull(user);
        op.saveAction(user, false);

        Assert.assertFalse(op.isMetadataDirty());
        Assert.assertFalse(op.isDescriptionDirty());
        Assert.assertFalse(op.isEditPublicationMode());
    }

    /**
     * @see OverviewPage#exportTextData(File,String)
     * @verifies write files correctly
     */
    @Test
    public void exportTextData_shouldWriteFilesCorrectly() throws Exception {
        OverviewPage overviewPage = DataManager.getInstance().getDao().getOverviewPageForRecord("PI 1", null, null);
        Assert.assertNotNull(overviewPage);

        Path hotfolder = Paths.get("build/hotfolder");
        try {
            if (!Files.exists(hotfolder)) {
                Files.createDirectory(hotfolder);
            }
            Assert.assertTrue(Files.isDirectory(hotfolder));

            String namingScheme = "PI 1";
            overviewPage.exportTextData(hotfolder.toAbsolutePath().toString(), namingScheme);
            Path descriptionFile = Paths.get(hotfolder.toAbsolutePath().toString(), namingScheme + "_overview", "description.xml");
            Assert.assertTrue(Files.isRegularFile(descriptionFile));
            Path publicationTextFile = Paths.get(hotfolder.toAbsolutePath().toString(), namingScheme + "_overview", "publicationtext.xml");
            Assert.assertTrue(Files.isRegularFile(publicationTextFile));
        } finally {
            FileUtils.deleteDir(hotfolder.toFile());
        }
    }

}