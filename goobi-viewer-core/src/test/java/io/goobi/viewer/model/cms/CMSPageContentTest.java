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
package io.goobi.viewer.model.cms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.types.CMSHtmlText;

public class CMSPageContentTest extends AbstractDatabaseEnabledTest {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        String webContentPath = new File("src/main/resources/META-INF/resources/resources/cms/components/frontend/component/template").getAbsolutePath();
        CMSTemplateManager.getInstance(webContentPath, null);
    }
    
    @Test
    public void testPersistContentItems() throws DAOException {
        
        
        IDAO dao = DataManager.getInstance().getDao();
        CMSPage page = new CMSPage();
        page.setTemplateId("page_template_id");
        assertTrue(dao.addCMSPage(page));
        CMSPage copy = dao.getCMSPage(page.getId());
        assertNotNull(copy);
        
        CMSComponent component = CMSTemplateManager.getInstance().getContentManager().getComponent("htmltext").orElse(null);
        assertNotNull(component);
        page.addComponent(component);
        assertEquals(1, page.getCmsComponents().size());
        CMSComponent c2 = page.getAsCMSComponent(page.getCmsComponents().get(0));
        assertEquals(CMSHtmlText.class, c2.getFirstContentItem().getContent().getClass());
        
        assertTrue(dao.updateCMSPage(page));
        copy = dao.getCMSPage(page.getId());
        assertEquals(1, copy.getCmsComponents().size());
        CMSComponent cc = copy.getAsCMSComponent(page.getCmsComponents().get(0));
        assertEquals(CMSHtmlText.class, cc.getFirstContentItem().getContent().getClass());
    }
    
}
