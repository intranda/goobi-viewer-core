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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.metadata.MetadataReplaceRule.MetadataReplaceRuleType;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;

class MetadataToolsTest extends AbstractSolrEnabledTest {

    /**
     * @see MetadataTools#applyReplaceRules(String,Map)
     * @verifies apply rules correctly
     */
    @Test
    void applyReplaceRules_shouldApplyRulesCorrectly() throws Exception {
        List<MetadataReplaceRule> replaceRules = new ArrayList<>(3);
        replaceRules.add(new MetadataReplaceRule('<', "", MetadataReplaceRuleType.CHAR));
        replaceRules.add(new MetadataReplaceRule(">", "s", MetadataReplaceRuleType.STRING));
        replaceRules.add(new MetadataReplaceRule("[ ]*100[ ]*", "", MetadataReplaceRuleType.REGEX));
        Assertions.assertEquals("vase", MetadataTools.applyReplaceRules(" 100 v<a>e", replaceRules, null));
    }

    /**
     * @see MetadataTools#applyReplaceRules(String,List,String)
     * @verifies apply conditional rules correctly
     */
    @Test
    void applyReplaceRules_shouldApplyConditionalRulesCorrectly() throws Exception {
        List<MetadataReplaceRule> replaceRules = Collections.singletonList(
                new MetadataReplaceRule("remove me", "", SolrConstants.PI_TOPSTRUCT + ":PPN517154005", MetadataReplaceRuleType.STRING));
        Assertions.assertEquals(SolrConstants.PI_TOPSTRUCT + ":PPN517154005", replaceRules.get(0).getConditions());
        // Condition match
        Assertions.assertEquals(" please", MetadataTools.applyReplaceRules("remove me please", replaceRules, "PPN517154005"));
        // No condition match
        Assertions.assertEquals("remove me please", MetadataTools.applyReplaceRules("remove me please", replaceRules, "PPN123"));
        // Ignore conditions if no PI was given
        Assertions.assertEquals(" please", MetadataTools.applyReplaceRules("remove me please", replaceRules, null));
    }

    /**
     * @see MetadataTools#findMetadataGroupType(String)
     * @verifies map values correctly
     */
    @Test
    void findMetadataGroupType_shouldMapValuesCorrectly() throws Exception {
        Assertions.assertEquals(MetadataGroupType.CORPORATION.name(), MetadataTools.findMetadataGroupType("kiz"));
        Assertions.assertEquals(MetadataGroupType.PERSON.name(), MetadataTools.findMetadataGroupType("piz"));
        Assertions.assertEquals(MetadataGroupType.SUBJECT.name(), MetadataTools.findMetadataGroupType("saa"));
        Assertions.assertEquals(MetadataGroupType.CONFERENCE.name(), MetadataTools.findMetadataGroupType("viz"));
        Assertions.assertEquals(MetadataGroupType.RECORD.name(), MetadataTools.findMetadataGroupType("wiz"));
    }

    /**
     * @see MetadataTools#convertLanguageToIso2(String)
     * @verifies return original value if language not found
     */
    @Test
    void convertLanguageToIso2_shouldReturnOriginalValueIfLanguageNotFound() throws Exception {
        Assertions.assertEquals("###", MetadataTools.convertLanguageToIso2("###"));
    }
}
