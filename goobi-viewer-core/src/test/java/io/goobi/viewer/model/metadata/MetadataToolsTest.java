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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.controller.SolrConstants.MetadataGroupType;

public class MetadataToolsTest {

    /**
     * @see MetadataTools#applyReplaceRules(String,Map)
     * @verifies apply rules correctly
     */
    @Test
    public void applyReplaceRules_shouldApplyRulesCorrectly() throws Exception {
        Map<Object, String> replaceRules = new HashMap<>();
        replaceRules.put('<', "");
        replaceRules.put(">", "s");
        replaceRules.put("REGEX:[ ]*100[ ]*", "");
        Assert.assertEquals("vase", MetadataTools.applyReplaceRules(" 100 v<a>e", replaceRules));
    }

    /**
     * @see MetadataTools#findMetadataGroupType(String)
     * @verifies map values correctly
     */
    @Test
    public void findMetadataGroupType_shouldMapValuesCorrectly() throws Exception {
        Assert.assertEquals(MetadataGroupType.CORPORATION.name(), MetadataTools.findMetadataGroupType("kiz"));
        Assert.assertEquals(MetadataGroupType.PERSON.name(), MetadataTools.findMetadataGroupType("piz"));
        Assert.assertEquals(MetadataGroupType.SUBJECT.name(), MetadataTools.findMetadataGroupType("saa"));
        Assert.assertEquals(MetadataGroupType.CONFERENCE.name(), MetadataTools.findMetadataGroupType("viz"));
        Assert.assertEquals(MetadataGroupType.RECORD.name(), MetadataTools.findMetadataGroupType("wiz"));
    }

    /**
     * @see MetadataTools#convertLanguageToIso2(String)
     * @verifies return original value if language not found
     */
    @Test
    public void convertLanguageToIso2_shouldReturnOriginalValueIfLanguageNotFound() throws Exception {
        Assert.assertEquals("###", MetadataTools.convertLanguageToIso2("###"));
    }
}