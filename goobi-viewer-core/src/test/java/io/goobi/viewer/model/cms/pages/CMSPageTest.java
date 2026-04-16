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
package io.goobi.viewer.model.cms.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSPageContentManager;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.pages.content.types.CMSShortTextContent;

class CMSPageTest extends AbstractDatabaseEnabledTest {

    Path componentTemplatesPath = Paths.get("src/test/resources/data/viewer/cms/component_templates");
    CMSTemplateManager templateManager;
    CMSPageContentManager contentManager;
    IDAO dao;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dao = DataManager.getInstance().getDao();
        templateManager = new CMSTemplateManager(componentTemplatesPath.toString(), null);
        contentManager = templateManager.getContentManager();// new CMSPageContentManager(componentTemplatesPath);
    }

    /**
     * @verifies persist page
     * @see CMSPage#addCMSPage
     */
    @Test
    void persistPage_shouldPersistPage() throws DAOException {

        CMSPage page = new CMSPage();
        page.getTitleTranslations().setValue("Titel", Locale.ENGLISH);
        assertEquals("Titel", page.getTitle(Locale.ENGLISH));

        assertTrue(dao.addCMSPage(page));

        CMSPage loaded = dao.getCMSPage(page.getId());

        assertEquals("Titel", loaded.getTitle(Locale.ENGLISH));

        CMSPage cloned = new CMSPage(loaded);

        assertEquals("Titel", cloned.getTitle(Locale.ENGLISH));
    }

    /**
     * @verifies persist page with content
     * @see CMSPage#addComponent
     */
    @Test
    void persistPage_shouldPersistPageWithContent() throws DAOException {

        CMSPage page = new CMSPage();
        page.getTitleTranslations().setValue("Titel", Locale.ENGLISH);

        CMSComponent textComponent = contentManager.getComponent("text").orElse(null);
        assertNotNull(textComponent);
        PersistentCMSComponent textComponentInPage = page.addComponent(textComponent);
        CMSShortTextContent textContent = (CMSShortTextContent) textComponentInPage.getContentItems().get(0);
        textContent.getText().setText("Entered Text", Locale.ENGLISH);

        assertTrue(dao.addCMSPage(page));
        assertTrue(page.removeComponent(page.getAsCMSComponent(textComponentInPage)));

        CMSPage loaded = dao.getCMSPage(page.getId());
        CMSPage cloned = new CMSPage(loaded);

        CMSShortTextContent clonedTextContent = (CMSShortTextContent) cloned.getPersistentComponents().get(0).getContentItems().get(0);
        assertEquals("Entered Text", clonedTextContent.getText().getText(Locale.ENGLISH));
    }

    /**
     * @verifies return false before initialisation
     * @see CMSPage#isComponentsLoaded
     */
    @Test
    void isComponentsLoaded_shouldReturnFalseBeforeInitialisation() {
        CMSPage page = new CMSPage();
        page.addComponent(contentManager.getComponent("text").orElseThrow());
        // Copy constructor creates page with persistentComponents but without cmsComponents
        CMSPage copy = new CMSPage(page);
        assertFalse(copy.isComponentsLoaded());
    }

    /**
     * @verifies return true after initialisation
     * @see CMSPage#isComponentsLoaded
     */
    @Test
    void isComponentsLoaded_shouldReturnTrueAfterInitialisation() {
        CMSPage page = new CMSPage();
        page.addComponent(contentManager.getComponent("text").orElseThrow());
        CMSPage copy = new CMSPage(page);
        copy.initialiseCMSComponents(templateManager);
        assertTrue(copy.isComponentsLoaded());
    }

    /**
     * @verifies return true even when template not found
     * @see CMSPage#isComponentsLoaded
     */
    @Test
    void isComponentsLoaded_shouldReturnTrueEvenWhenTemplateNotFound() {
        CMSPage page = new CMSPage();
        // Add a component with an unknown template filename
        PersistentCMSComponent unknown = new PersistentCMSComponent();
        unknown.setTemplateFilename("nonexistent_template");
        unknown.setOwningPage(page);
        page.getPersistentComponents().add(unknown);
        // initialiseCMSComponents cannot find the template — but flag must still be set
        page.initialiseCMSComponents(templateManager);
        assertTrue(page.isComponentsLoaded());
        // The missing component is simply skipped
        assertEquals(0, page.getComponents().size());
    }

    /**
     * @see CMSPage#exportAsXml()
     * @verifies create XML document with localized titles, categories, and text components
     */
    @Test
    void exportAsXml_shouldCreateXMLDocumentWithLocalizedTitlesCategoriesAndTextComponents() {
        CMSPage page = new CMSPage();
        page.getTitleTranslations().setValue("Title", Locale.ENGLISH);
        page.getTitleTranslations().setValue("Titel", Locale.GERMAN);
        page.addCategory(new CMSCategory("foo"));
        page.addCategory(new CMSCategory("bar"));

        CMSComponent textComponent = contentManager.getComponent("text").orElse(null);
        assertNotNull(textComponent);
        PersistentCMSComponent textComponentInPage = page.addComponent(textComponent);
        CMSShortTextContent textContent = (CMSShortTextContent) textComponentInPage.getContentItems().get(0);
        textContent.getText().setText("Entered Text", Locale.ENGLISH);

        Document doc = page.exportAsXml();
        assertNotNull(doc);
        assertEquals("cmsPage", doc.getRootElement().getName());
        List<Element> eleListTitle = doc.getRootElement().getChildren("title");
        assertNotNull(eleListTitle);
        assertEquals(2, eleListTitle.size());
        assertEquals("Title", eleListTitle.get(0).getText());
        assertEquals("en", eleListTitle.get(0).getAttributeValue("lang"));
        assertEquals("Titel", eleListTitle.get(1).getText());
        assertEquals("de", eleListTitle.get(1).getAttributeValue("lang"));

        assertNotNull(doc.getRootElement().getChildText("categories"));
        List<Element> eleListCategory = doc.getRootElement().getChild("categories").getChildren("category");
        assertNotNull(eleListCategory);
        assertEquals(2, eleListCategory.size());
        assertEquals("foo", eleListCategory.get(0).getText());
        assertEquals("bar", eleListCategory.get(1).getText());

        List<Element> eleListText = doc.getRootElement().getChildren("text");
        assertNotNull(eleListText);
        assertEquals(1, eleListText.size());
        assertEquals("en", eleListText.get(0).getAttributeValue("lang"));
        assertEquals("Entered Text", eleListText.get(0).getText());
    }

}
