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
package io.goobi.viewer.managedbeans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataElement;

class MetadataBeanTest extends AbstractTest {

    /**
     * @see MetadataBean#etMetadataElementsAsList(MetadataElement...)
     * @verifies return empty list given null
     */
    @Test
    void getMetadataElementsAsList_shouldReturnEmptyListGivenNull() {
        MetadataBean bean = new MetadataBean();
        List<MetadataElement> list = bean.getMetadataElementsAsList((MetadataElement[]) null);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.isEmpty());
    }

    /**
     * @see MetadataBean#etMetadataElementsAsList(MetadataElement...)
     * @verifies return given elements as list
     */
    @Test
    void getMetadataElementsAsList_shouldReturnGivenElementsAsList() {
        MetadataBean bean = new MetadataBean();
        List<MetadataElement> list = bean.getMetadataElementsAsList(new MetadataElement(), new MetadataElement(), new MetadataElement());
        Assertions.assertNotNull(list);
        Assertions.assertEquals(3, list.size());
    }

    /**
     * @see MetadataBean#getBottomMetadataElementAsList(int)
     * @verifies return empty list if bottom element missing
     */
    @Test
    void getBottomMetadataElementAsList_shouldReturnEmptyListIfBottomElementMissing() {
        MetadataBean bean = new MetadataBean();
        List<MetadataElement> list = bean.getBottomMetadataElementAsList(0);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.isEmpty());
    }

    /**
     * @see MetadataBean#getBottomMetadataElementAsList(int)
     * @verifies return empty list if bottom element contains no sidebar metadata
     */
    @Test
    void getBottomMetadataElementAsList_shouldReturnEmptyListIfBottomElementContainsNoSidebarMetadata() {
        MetadataBean bean = new MetadataBean();
        bean.getMetadataElementMap().put(0, new ArrayList<>());
        Assertions.assertNotNull(bean.getMetadataElementList());
        bean.getMetadataElementList().add(new MetadataElement());
        Assertions.assertEquals(1, bean.getMetadataElementList().size());
        Assertions.assertFalse(bean.getMetadataElementList().get(0).isHasSidebarMetadata());
        List<MetadataElement> list = bean.getBottomMetadataElementAsList(0);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.isEmpty());
    }

    /**
     * @see MetadataBean#getBottomMetadataElementAsList(int)
     * @verifies return bottom if it contains sidebar metadata
     */
    @Test
    void getBottomMetadataElementAsList_shouldReturnBottomElementIfItContainsSidebarMetadata() {
        MetadataBean bean = new MetadataBean();
        bean.getMetadataElementMap().put(0, new ArrayList<>());
        Assertions.assertNotNull(bean.getMetadataElementList());
        MetadataElement me = new MetadataElement();
        me.setSidebarMetadataList(Collections.singletonList(new Metadata()));
        bean.getMetadataElementList().add(me);
        Assertions.assertEquals(1, bean.getMetadataElementList().size());
        Assertions.assertTrue(bean.getMetadataElementList().get(0).isHasSidebarMetadata());
        List<MetadataElement> list = bean.getBottomMetadataElementAsList(0);
        Assertions.assertNotNull(list);
        Assertions.assertFalse(list.isEmpty());
    }
}
