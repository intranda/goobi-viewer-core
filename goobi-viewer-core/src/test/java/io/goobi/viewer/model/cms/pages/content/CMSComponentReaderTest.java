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
package io.goobi.viewer.model.cms.pages.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jdom2.JDOMException;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.cms.pages.content.types.CMSShortTextContent;

class CMSComponentReaderTest {

    /**
     * @see CMSComponentReader#read(Path)
     * @verifies skip content item without className
     */
    @Test
    void read_shouldSkipContentItemWithoutClassName() throws IOException, JDOMException {
        Path file = Paths.get("src/test/resources/data/viewer/cms/component_templates_invalid/noclassname.xml");
        CMSComponent component = new CMSComponentReader().read(file);
        assertNotNull(component);
        // The single content item has no <className>, so it must be skipped instead of producing a content item.
        assertEquals(0, component.getContentItems().size());
    }

    /**
     * @see CMSComponentReader#read(Path)
     * @verifies construct content item from className
     */
    @Test
    void read_shouldConstructContentItemFromClassName() throws IOException, JDOMException {
        Path file = Paths.get("src/test/resources/data/viewer/cms/component_templates/text.xml");
        CMSComponent component = new CMSComponentReader().read(file);
        assertNotNull(component);
        assertEquals(1, component.getContentItems().size());
        assertEquals(CMSShortTextContent.class, component.getContentItems().get(0).getContent().getClass());
    }

}
