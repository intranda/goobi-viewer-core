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
package de.intranda.digiverso.presentation.model.cms;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.model.cms.CMSContentItem.CMSContentItemType;

public class CMSPageTemplateTest {

    /**
     * @see CMSPageTemplate#loadFromXML(File)
     * @verifies load template correctly
     */
    @Test
    public void loadFromXML_shouldLoadTemplateCorrectly() throws Exception {
        File file = new File("resources/test/data/cms_template.xml");
        Assert.assertTrue(file.isFile());
        CMSPageTemplate template = CMSPageTemplate.loadFromXML(file.toPath());
        Assert.assertNotNull(template);
        Assert.assertEquals("File path: " + file.getAbsolutePath(), "test_template", template.getId());
        Assert.assertEquals("File path: " + file.getAbsolutePath(), "2015-01-14", template.getVersion());
        Assert.assertEquals("File path: " + file.getAbsolutePath(), "Test template", template.getName());
        Assert.assertEquals("File path: " + file.getAbsolutePath(), "Test description", template.getDescription());
        Assert.assertEquals("icon.png", template.getIconFileName());
        Assert.assertEquals("File path: " + file.getAbsolutePath(), "test_template_html.xhtml", template.getHtmlFileName());
        Assert.assertEquals("File path: " + file.getAbsolutePath(), 4, template.getContentItems().size());
        CMSContentItem item = template.getContentItems().get(0);
        // TODO make this consistent so that changes to sorting don't break the test
        Assert.assertEquals("imag01", item.getItemId());
        Assert.assertEquals(CMSContentItemType.MEDIA, item.getType());
        Assert.assertFalse(item.isMandatory());
    }

    /**
     * @see CMSPageTemplate#loadFromXML(File)
     * @verifies throw IllegalArgumentException if file is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void loadFromXML_shouldThrowIllegalArgumentExceptionIfFileIsNull() throws Exception {
        CMSPageTemplate.loadFromXML(null);
    }
}