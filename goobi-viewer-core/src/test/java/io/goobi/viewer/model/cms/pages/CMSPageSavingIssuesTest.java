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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSPageContentManager;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.pages.content.types.CMSShortTextContent;

public class CMSPageSavingIssuesTest extends AbstractDatabaseEnabledTest {

    IDAO dao;
    Path componentTemplatesPath = Paths.get("src/test/resources/data/viewer/cms/component_templates");
    CMSTemplateManager templateManager;
    CMSPageContentManager contentManager;
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dao = DataManager.getInstance().getDao();
        templateManager = new CMSTemplateManager(componentTemplatesPath.toString(), null);
        contentManager = templateManager.getContentManager();
    }
    
    @Test
    void test() throws DAOException {
        CMSPage page = new CMSPage();
        
        page.initialiseCMSComponents(templateManager);
        PersistentCMSComponent component1 = page.addComponent("text", templateManager);
        component1.getTranslatableContentItems().get(0).getText().setValue("text1");
        component1.setOrder(1);
        PersistentCMSComponent component2 = page.addComponent("text", templateManager);
        component2.getTranslatableContentItems().get(0).getText().setValue("text2");
        component2.setOrder(2);
        
        assertEquals(2, page.getPersistentComponents().size());
        assertEquals(2, page.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).count());
        assertEquals("text1text2", page.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).map(c -> (((CMSShortTextContent)c).getText().getText())).sorted().collect(Collectors.joining()));

        assertTrue(dao.addCMSPage(page));
        
        CMSPage page2 = new CMSPage(dao.getCMSPage(page.getId()));
        page2.initialiseCMSComponents(templateManager);
        assertEquals(2, page2.getPersistentComponents().size());
        assertEquals(2, page2.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).count());
        assertEquals("text1text2", page2.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).map(c -> (((CMSShortTextContent)c).getText().getText())).sorted().collect(Collectors.joining()));
        
        CMSComponent deletedComponent = page2.getComponents().get(0);
        page2.removeComponent(deletedComponent);
        dao.deleteCMSComponent(deletedComponent.getPersistentComponent());
        assertEquals(1, page2.getPersistentComponents().size());
        assertEquals(1, page2.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).count());
        assertEquals("text2", page2.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).map(c -> (((CMSShortTextContent)c).getText().getText())).collect(Collectors.joining()));
        
        assertTrue(dao.updateCMSPage(page2));
        
        CMSPage page3 = new CMSPage(dao.getCMSPage(page2.getId()));
        page3.initialiseCMSComponents(templateManager);
        assertEquals(1, page3.getPersistentComponents().size());
        assertEquals(1, page3.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).count());
        assertEquals("text2", page3.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).map(c -> (((CMSShortTextContent)c).getText().getText())).collect(Collectors.joining()));
        
        
    }
    
    @Test
    void testNoContent() throws DAOException {
        CMSPage page = new CMSPage();
        
        page.initialiseCMSComponents(templateManager);
        PersistentCMSComponent component1 = page.addComponent("static", templateManager);
        component1.setOrder(1);
        PersistentCMSComponent component2 = page.addComponent("static", templateManager);
        component2.setOrder(2);
        
        assertEquals(2, page.getPersistentComponents().size());
        assertEquals(0, page.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).count());

        assertTrue(dao.addCMSPage(page));
        
        CMSPage page2 = new CMSPage(dao.getCMSPage(page.getId()));
        assertEquals(2, page2.getPersistentComponents().size());
        assertEquals(0, page2.getPersistentComponents().stream().flatMap(p -> p.getContentItems().stream()).count());
        assertEquals(2, dao.getNativeQueryResults("SELECT * FROM cms_components WHERE owning_page_id=" + page2.getId()).size());

        page2.initialiseCMSComponents(templateManager);
        CMSComponent deletedComponent = page2.getComponents().get(0);
        page2.removeComponent(deletedComponent);
        assertEquals(1, page2.getPersistentComponents().size());
        
        assertTrue(dao.updateCMSPage(page2));
        
        CMSPage page3 = new CMSPage(dao.getCMSPage(page2.getId()));
        assertEquals(1, page3.getPersistentComponents().size());
        assertEquals(1, dao.getNativeQueryResults("SELECT * FROM cms_components WHERE owning_page_id=" + page3.getId()).size());
        
        
    }
    
    @Test
    void testComponent() throws DAOException {
        
        CMSShortTextContent text = new CMSShortTextContent();
        text.getText().setText("text");
        PersistentCMSComponent component1 = new PersistentCMSComponent(templateManager.getComponent("text").orElse(null), Collections.singletonList(text));
        assertEquals("text", component1.getTranslatableContentItems().get(0).getText().getText());
        
        dao.addCMSComponent(component1);
        assertEquals(1, dao.getNativeQueryResults("SELECT * FROM cms_components WHERE component_id=" + component1.getId()).size());
        assertEquals(1, dao.getNativeQueryResults("SELECT * FROM cms_content WHERE owning_component_id=" + component1.getId()).size());

        PersistentCMSComponent component2 = new PersistentCMSComponent(dao.getCMSComponent(component1.getId()));
        assertTrue(dao.deleteCMSComponent(component2));
        
        assertEquals(0, dao.getNativeQueryResults("SELECT * FROM cms_components WHERE component_id=" + component1.getId()).size());
        assertEquals(0, dao.getNativeQueryResults("SELECT * FROM cms_content WHERE owning_component_id=" + component1.getId()).size());

        
    }
    
    

}
