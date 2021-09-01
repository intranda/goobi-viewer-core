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
package io.goobi.viewer.model.metadata;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;

public class MetadataElementTest {

    /**
     * @see MetadataElement#isSkip()
     * @verifies return true if metadata list empty
     */
    @Test
    public void isSkip_shouldReturnTrueIfMetadataListEmpty() throws Exception {
        MetadataElement me = new MetadataElement();
        Assert.assertTrue(me.isSkip());

    }

    /**
     * @see MetadataElement#isSkip()
     * @verifies return true if all metadata fields blank
     */
    @Test
    public void isSkip_shouldReturnTrueIfAllMetadataFieldsBlank() throws Exception {
        MetadataElement me = new MetadataElement();
        me.getMetadataList().add(new Metadata());
        Assert.assertTrue(me.getMetadataList().get(0).isBlank());
        Assert.assertFalse(me.getMetadataList().get(0).isHideIfOnlyMetadataField());
        Assert.assertTrue(me.isSkip());
    }

    /**
     * @see MetadataElement#isSkip()
     * @verifies return true if all metadata fields hidden
     */
    @Test
    public void isSkip_shouldReturnTrueIfAllMetadataFieldsHidden() throws Exception {
        MetadataElement me = new MetadataElement();
        {
            Metadata md = new Metadata().setHideIfOnlyMetadataField(true);
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("foo"));
            md.setParamValue(0, 0, Collections.singletonList("bar"), "foo", null, null, null, null);
            Assert.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
        }
        {
            Metadata md = new Metadata().setHideIfOnlyMetadataField(true);
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("label"));
            md.setParamValue(0, 0, Collections.singletonList("value"), "label", null, null, null, null);
            Assert.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
        }
        Assert.assertTrue(me.isSkip());
    }

    /**
     * @see MetadataElement#isSkip()
     * @verifies return false if non hidden metadata fields exist
     */
    @Test
    public void isSkip_shouldReturnFalseIfNonHiddenMetadataFieldsExist() throws Exception {
        MetadataElement me = new MetadataElement();
        {
            Metadata md = new Metadata();
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("foo"));
            md.setParamValue(0, 0, Collections.singletonList("bar"), "foo", null, null, null, null);
            Assert.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
        }
        {
            Metadata md = new Metadata().setHideIfOnlyMetadataField(true);
            md.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setKey("label"));
            md.setParamValue(0, 0, Collections.singletonList("value"), "label", null, null, null, null);
            Assert.assertFalse(md.isBlank());
            me.getMetadataList().add(md);
        }
        Assert.assertFalse(me.isSkip());
    }
}