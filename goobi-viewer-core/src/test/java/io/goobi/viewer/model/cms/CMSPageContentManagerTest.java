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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSComponentAttribute;
import io.goobi.viewer.model.cms.pages.content.CMSContentItem;
import io.goobi.viewer.model.cms.pages.content.CMSPageContentManager;

public class CMSPageContentManagerTest {

    @Test
    public void testReadFromTemplateFiles() throws IOException {
        Path path = Paths.get("src/test/resources/data/viewer/cms/component_templates");
        
        CMSPageContentManager manager = new CMSPageContentManager(path);
        List<CMSComponent> components = manager.getComponents();
        assertEquals(5, components.size());
        CMSComponent htmlComponent = components.stream().filter(c -> c.getLabel().equals("cms_component__htmltext__label")).findAny().orElse(null);
        assertNotNull(htmlComponent);
        assertEquals("cms/components/frontend/component", htmlComponent.getJsfComponent().getLibrary());
        assertEquals("htmltext", htmlComponent.getJsfComponent().getName());
        assertEquals("cms_component__htmltext__desc", htmlComponent.getDescription());
        assertEquals("cms/components/frontend/component/icon/htmltext.png", htmlComponent.getIconPath());
        
        CMSComponent imageComponent = components.stream().filter(c -> c.getLabel().equals("cms_component__image__label")).findAny().orElse(null);
        assertNotNull(imageComponent);
        assertEquals(2, imageComponent.getAttributes().size());
        
        CMSComponentAttribute widthAttribute = imageComponent.getAttribute("width");
        assertNotNull(widthAttribute);
        assertEquals("cms__component_attribute__width", widthAttribute.getLabel());
        assertEquals(4, widthAttribute.getOptions().size());
        assertEquals("100", widthAttribute.getValue());
    }

}
