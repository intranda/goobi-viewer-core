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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.search.SearchHelper;

public class MetadataValueTest {

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies construct param correctly
     */
    @Test
    public void getComboValueShort_shouldConstructParamCorrectly() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamPrefixes().add("pre_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("val");
        value.getParamSuffixes().add("_suf");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value");
        value.getParamSuffixes().add("_suffix");
        Assertions.assertEquals("prefix_value_suffix", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies construct multivalued param correctly
     */
    @Test
    public void getComboValueShort_shouldConstructMultivaluedParamCorrectly() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamPrefixes().add("pre_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("val");
        value.getParamSuffixes().add("_suf");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value1");
        value.getParamValues().get(1).add("value2");
        value.getParamSuffixes().add("_suffix");
        Assertions.assertEquals("prefix_value1_suffixprefix_value2_suffix", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies return empty string if value index larger than number of values
     */
    @Test
    public void getComboValueShort_shouldReturnEmptyStringIfValueIndexLargerThanNumberOfValues() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value");
        value.getParamSuffixes().add("_suffix");
        Assertions.assertEquals("", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies return empty string if value is empty
     */
    @Test
    public void getComboValueShort_shouldReturnEmptyStringIfValueIsEmpty() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("");
        value.getParamSuffixes().add("_suffix");
        Assertions.assertEquals("", value.getComboValueShort(0));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies not add prefix if first param
     */
    @Test
    public void getComboValueShort_shouldNotAddPrefixIfFirstParam() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamPrefixes().add("prefix_");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value");
        value.getParamSuffixes().add("_suffix");
        Assertions.assertEquals("value_suffix", value.getComboValueShort(0));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies not add null suffix
     */
    @Test
    public void getComboValueShort_shouldNotAddNullSuffix() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamSuffixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value1");
        value.getParamSuffixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value2");
        Assertions.assertEquals("value2", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies not add empty prefix
     */
    @Test
    public void getComboValueShort_shouldNotAddEmptyPrefix() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamPrefixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value1");
        value.getParamPrefixes().add("");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value2");
        Assertions.assertEquals("value2", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies not add empty suffix
     */
    @Test
    public void getComboValueShort_shouldNotAddEmptySuffix() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamSuffixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value1");
        value.getParamSuffixes().add("");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("value2");
        Assertions.assertEquals("value2", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies add separator between values if no prefix used
     */
    @Test
    public void getComboValueShort_shouldAddSeparatorBetweenValuesIfNoPrefixUsed() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamSuffixes().add(null);
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("value1");
        value.getParamValues().get(0).add("value2");
        Assertions.assertEquals("value1, value2", value.getComboValueShort(0));
    }

    /**
     * @see MetadataValue#getComboValueShort(int)
     * @verifies use master value fragment correctly
     */
    @Test
    public void getComboValueShort_shouldUseMasterValueFragmentCorrectly() throws Exception {
        MetadataValue value = new MetadataValue("", "", "");
        value.getParamMasterValueFragments().add("foo {0} bar");
        value.getParamSuffixes().add("pre_");
        value.getParamSuffixes().add("_suf");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(0).add("vs");
        Assertions.assertEquals("foo vs bar", value.getComboValueShort(0));

        value.getParamMasterValueFragments().add("foo {0} bar");
        value.getParamSuffixes().add("pre_");
        value.getParamSuffixes().add("_suf");
        value.getParamValues().add(new ArrayList<>());
        value.getParamValues().get(1).add("minus");
        Assertions.assertEquals("foo minus bar", value.getComboValueShort(1));
    }

    /**
     * @see MetadataValue#applyHighlightingToParamValue(int,Set)
     * @verifies apply highlighting correctly
     */
    @Test
    public void applyHighlightingToParamValue_shouldApplyHighlightingCorrectly() throws Exception {
        MetadataValue mdValue = new MetadataValue("", "", "");
        List<String> values = Arrays.asList("foobar", "something");
        mdValue.getParamValues().add(values);
        mdValue.applyHighlightingToParamValue(0, Collections.singleton("foo"));
        Assertions.assertEquals("<span class=\"search-list--highlight\">foo</span>bar", mdValue.getParamValues().get(0).get(0));
    }

    /**
     * @see MetadataValue#isAllParamValuesBlank()
     * @verifies return true if all param values blank
     */
    @Test
    public void isAllParamValuesBlank_shouldReturnTrueIfAllParamValuesBlank() throws Exception {
        MetadataValue mdValue = new MetadataValue("", "", "");
        List<String> values = Arrays.asList("", "");
        mdValue.getParamValues().add(values);
        mdValue.getParamValues().add(values);

        Assertions.assertTrue(mdValue.isAllParamValuesBlank());
    }

    /**
     * @see MetadataValue#isAllParamValuesBlank()
     * @verifies return false if any param value not blank
     */
    @Test
    public void isAllParamValuesBlank_shouldReturnFalseIfAnyParamValueNotBlank() throws Exception {
        MetadataValue mdValue = new MetadataValue("", "", "");
        List<String> values = Arrays.asList("", "foo");
        mdValue.getParamValues().add(values);

        Assertions.assertFalse(mdValue.isAllParamValuesBlank());
    }
}
