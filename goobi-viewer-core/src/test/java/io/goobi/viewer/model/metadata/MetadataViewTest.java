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
package io.goobi.viewer.model.metadata;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.viewer.StructElement;

public class MetadataViewTest {

    /**
     * @see MetadataView#isVisible(StructElement)
     * @verifies return true if condition null or empty
     */
    @Test
    public void isVisible_shouldReturnTrueIfConditionNullOrEmpty() throws Exception {
        MetadataView view = new MetadataView();
        Assertions.assertTrue(view.isVisible(new StructElement()));
    }

    /**
     * @see MetadataView#isVisible(StructElement)
     * @verifies return false if struct element null
     */
    @Test
    public void isVisible_shouldReturnFalseIfStructElementNull() throws Exception {
        MetadataView view = new MetadataView().setCondition("foo:bar");
        Assertions.assertFalse(view.isVisible(null));
    }

    /**
     * @see MetadataView#isVisible(StructElement)
     * @verifies return true if field value pair found
     */
    @Test
    public void isVisible_shouldReturnTrueIfFieldValuePairFound() throws Exception {
        MetadataView view = new MetadataView().setCondition("foo:bar");
        StructElement se = new StructElement();
        se.getMetadataFields().put("foo", Collections.singletonList("bar"));
        Assertions.assertTrue(view.isVisible(se));
    }

    /**
     * @see MetadataView#isVisible(StructElement)
     * @verifies return false if field value pair not found
     */
    @Test
    public void isVisible_shouldReturnFalseIfFieldValuePairNotFound() throws Exception {
        MetadataView view = new MetadataView().setCondition("foo:bar");
        StructElement se = new StructElement();
        se.getMetadataFields().put("foo", Collections.singletonList("other"));
        Assertions.assertFalse(view.isVisible(se));
    }

    /**
     * @see MetadataView#isVisible(StructElement)
     * @verifies return true if field name found
     */
    @Test
    public void isVisible_shouldReturnTrueIfFieldNameFound() throws Exception {
        MetadataView view = new MetadataView().setCondition("foo");
        StructElement se = new StructElement();
        se.getMetadataFields().put("foo", Collections.singletonList("bar"));
        Assertions.assertTrue(view.isVisible(se));
    }

    /**
     * @see MetadataView#isVisible(StructElement)
     * @verifies return false if field name not found
     */
    @Test
    public void isVisible_shouldReturnFalseIfFieldNameNotFound() throws Exception {
        MetadataView view = new MetadataView().setCondition("foo");
        StructElement se = new StructElement();
        se.getMetadataFields().put("bar", Collections.singletonList("other"));
        Assertions.assertFalse(view.isVisible(se));
    }
}
