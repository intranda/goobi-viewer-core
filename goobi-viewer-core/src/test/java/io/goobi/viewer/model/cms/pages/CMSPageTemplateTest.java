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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.cms.legacy.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.jsf.JsfComponent;

public class CMSPageTemplateTest {

    Path themeComponentFile = Paths.get("src/test/resources/data/viewer/cms/theme_templates/custom_template_01_home.xml");

    
    @Test
    public void testLoadLegacyThemeTemplate() {
      CMSPageTemplate template = CMSPageTemplate.loadFromXML(themeComponentFile);
      CMSComponent component = template.createCMSComponent();
      JsfComponent jsf = component.getJsfComponent();
      assertEquals("themes/mnha/cms/templates/views", jsf.getLibrary());
      assertEquals("custom_template_01_home.xhtml", jsf.getFilename());
    }

}
