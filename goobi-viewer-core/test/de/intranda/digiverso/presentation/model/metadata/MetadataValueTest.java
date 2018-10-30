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
package de.intranda.digiverso.presentation.model.metadata;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

public class MetadataValueTest {

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies construct param correctly
     */
    @Test
    public void getComboValueShort_shouldConstructParamCorrectly() throws Exception {
        MetadataValue value = new MetadataValue("");
        value.getParamPrefixes().add("pre_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("val");
        value.getParamSuffixes().add("_suf");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value");
        value.getParamSuffixes().add("_suffix");
        Assert.assertEquals("prefix_value_suffix", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies construct multivalued param correctly
     */
    @Test
    public void getComboValueShort_shouldConstructMultivaluedParamCorrectly() throws Exception {
        MetadataValue value = new MetadataValue("");
        value.getParamPrefixes().add("pre_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("val");
        value.getParamSuffixes().add("_suf");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value1");
        value.getParamValues().get(1).add("value2");
        value.getParamSuffixes().add("_suffix");
        Assert.assertEquals("prefix_value1_suffixprefix_value2_suffix", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies return empty string if value index larger than number of values
     */
    @Test
    public void getComboValueShort_shouldReturnEmptyStringIfValueIndexLargerThanNumberOfValues() throws Exception {
        MetadataValue value = new MetadataValue("");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value");
        value.getParamSuffixes().add("_suffix");
        Assert.assertEquals("", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies return empty string if value is empty
     */
    @Test
    public void getComboValueShort_shouldReturnEmptyStringIfValueIsEmpty() throws Exception {
        MetadataValue value = new MetadataValue("");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("");
        value.getParamSuffixes().add("_suffix");
        Assert.assertEquals("", value.getComboValueShort(0));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies not add prefix if first param
     */
    @Test
    public void getComboValueShort_shouldNotAddPrefixIfFirstParam() throws Exception {
        MetadataValue value = new MetadataValue("");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value");
        value.getParamSuffixes().add("_suffix");
        Assert.assertEquals("value_suffix", value.getComboValueShort(0));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies not add null prefix
     */
    @Test
    public void getComboValueShort_shouldNotAddNullPrefix() throws Exception {
        MetadataValue value = new MetadataValue("");
        value.getParamPrefixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value1");
        value.getParamPrefixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value2");
        Assert.assertEquals("value2", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies not add null suffix
     */
    @Test
    public void getComboValueShort_shouldNotAddNullSuffix() throws Exception {
        MetadataValue value = new MetadataValue("");
        value.getParamSuffixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value1");
        value.getParamSuffixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value2");
        Assert.assertEquals("value2", value.getComboValueShort(1));
    }
}