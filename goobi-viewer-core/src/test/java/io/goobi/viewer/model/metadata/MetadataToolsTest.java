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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.metadata.MetadataReplaceRule.MetadataReplaceRuleType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;

class MetadataToolsTest extends AbstractSolrEnabledTest {

    /**
     * @verifies apply char string and regex replace rules to transform the input value
     */
    @Test
    void applyReplaceRules_shouldApplyCharStringAndRegexReplaceRulesToTransformTheInputValue() throws Exception {
        List<MetadataReplaceRule> replaceRules = new ArrayList<>(3);
        replaceRules.add(new MetadataReplaceRule('<', "", MetadataReplaceRuleType.CHAR));
        replaceRules.add(new MetadataReplaceRule(">", "s", MetadataReplaceRuleType.STRING));
        replaceRules.add(new MetadataReplaceRule("[ ]*100[ ]*", "", MetadataReplaceRuleType.REGEX));
        Assertions.assertEquals("vase", MetadataTools.applyReplaceRules(" 100 v<a>e", replaceRules, null));
    }

    /**
     * @verifies apply conditional replace rule only when PI matches the condition
     */
    @Test
    void applyReplaceRules_shouldApplyConditionalReplaceRuleOnlyWhenPIMatchesTheCondition() throws Exception {
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
     * @verifies map GND type codes and named types to the corresponding MetadataGroupType
     */
    @Test
    void findMetadataGroupType_shouldMapGNDTypeCodesAndNamedTypesToTheCorrespondingMetadataGroupType() throws Exception {
        Assertions.assertEquals(MetadataGroupType.CORPORATION.name(), MetadataTools.findMetadataGroupType("kiz"));
        Assertions.assertEquals(MetadataGroupType.PERSON.name(), MetadataTools.findMetadataGroupType("piz"));
        Assertions.assertEquals(MetadataGroupType.SUBJECT.name(), MetadataTools.findMetadataGroupType("saa"));
        Assertions.assertEquals(MetadataGroupType.CONFERENCE.name(), MetadataTools.findMetadataGroupType("viz"));
        Assertions.assertEquals(MetadataGroupType.RECORD.name(), MetadataTools.findMetadataGroupType("wiz"));
        
        Assertions.assertEquals(MetadataGroupType.LOCATION.name(), MetadataTools.findMetadataGroupType("Place"));
        Assertions.assertEquals(MetadataGroupType.PERSON.name(), MetadataTools.findMetadataGroupType("Person"));
    }

    /**
     * @see MetadataTools#convertLanguageToIso2(String)
     * @verifies return original value if language not found
     */
    @Test
    void convertLanguageToIso2_shouldReturnOriginalValueIfLanguageNotFound() throws Exception {
        Assertions.assertEquals("###", MetadataTools.convertLanguageToIso2("###"));
    }

    /**
     * @see MetadataTools#getGroupedMetadata(String, String, List)
     * @verifies return grouped metadata docs correctly
     */
    @Test
    void getGroupedMetadata_shouldReturnGroupedMetadataDocsCorrectly() throws Exception {
        // Query grouped metadata docs for PI_KLEIUNIV using its IDDOC as owner
        SolrDocumentList docs = MetadataTools.getGroupedMetadata(iddocKleiuniv, null, null);
        Assertions.assertNotNull(docs);
        // The query should return only docs with DOCTYPE=METADATA and IDDOC_OWNER matching the owner IDDOC
        for (org.apache.solr.common.SolrDocument doc : docs) {
            Assertions.assertEquals("METADATA", doc.getFieldValue(SolrConstants.DOCTYPE));
            Assertions.assertEquals(iddocKleiuniv, String.valueOf(doc.getFieldValue(SolrConstants.IDDOC_OWNER)));
        }
    }

    /**
     * @see MetadataTools#getShapeMetadataByOwner(java.util.Collection)
     * @verifies map shape docs to their owners
     */
    @Test
    void getShapeMetadataByOwner_shouldMapShapeDocsToTheirOwners() throws Exception {
        SolrDocumentList anyShape = DataManager.getInstance()
                .getSearchIndex()
                .search("+" + SolrConstants.DOCTYPE + ":METADATA +" + SolrConstants.METADATATYPE + ':' + MetadataGroupType.SHAPE.name(), 1, null,
                        List.of(SolrConstants.IDDOC_OWNER));
        Assertions.assertFalse(anyShape.isEmpty(), "test index must contain at least one SHAPE document");
        String owner = String.valueOf(anyShape.get(0).getFirstValue(SolrConstants.IDDOC_OWNER));

        Map<String, List<SolrDocument>> result = MetadataTools.getShapeMetadataByOwner(List.of(owner, iddocKleiuniv));

        Assertions.assertFalse(result.get(owner).isEmpty());
        Assertions.assertEquals(MetadataTools.getGroupedMetadata(owner, " +" + SolrConstants.METADATATYPE + ':' + MetadataGroupType.SHAPE.name(), null)
                .size(), result.get(owner).size());
        for (SolrDocument doc : result.get(owner)) {
            Assertions.assertEquals(owner, String.valueOf(doc.getFirstValue(SolrConstants.IDDOC_OWNER)));
        }
    }

    /**
     * @see MetadataTools#getShapeMetadataByOwner(java.util.Collection)
     * @verifies map owners without shapes to empty list
     */
    @Test
    void getShapeMetadataByOwner_shouldMapOwnersWithoutShapesToEmptyList() throws Exception {
        Map<String, List<SolrDocument>> result = MetadataTools.getShapeMetadataByOwner(List.of("0"));
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.get("0").isEmpty());
    }

    /**
     * @see MetadataTools#getShapeMetadataByOwner(java.util.Collection)
     * @verifies return empty map for empty input
     */
    @Test
    void getShapeMetadataByOwner_shouldReturnEmptyMapForEmptyInput() throws Exception {
        Assertions.assertTrue(MetadataTools.getShapeMetadataByOwner(List.of()).isEmpty());
    }

    /**
     * @see MetadataTools#generateRIS(String, Map, Long)
     * @verifies ignore stored NUMVOLUMES field
     */
    @Test
    void generateRIS_shouldIgnoreStoredNUMVOLUMESField() throws Exception {
        Map<String, List<String>> fields = new HashMap<>();
        fields.put(SolrConstants.TITLE, Collections.singletonList("Some title"));
        fields.put(SolrConstants.NUMVOLUMES, Collections.singletonList("99"));

        String ris = MetadataTools.generateRIS("periodical", fields, null);
        Assertions.assertNotNull(ris);
        Assertions.assertTrue(ris.contains("TI  - Some title\r\n"));
        Assertions.assertFalse(ris.contains("NV  - "));
    }

    /**
     * @see MetadataTools#generateRIS(String, Map, Long)
     * @verifies add NV entry for given volume count
     */
    @Test
    void generateRIS_shouldAddNVEntryForGivenVolumeCount() throws Exception {
        Map<String, List<String>> fields = new HashMap<>();
        fields.put(SolrConstants.NUMVOLUMES, Collections.singletonList("99"));

        String ris = MetadataTools.generateRIS("periodical", fields, 5L);
        Assertions.assertNotNull(ris);
        Assertions.assertTrue(ris.endsWith("NV  - 5\r\nER  - \r\n"));
        Assertions.assertFalse(ris.contains("NV  - 99"));
    }

    /**
     * @see MetadataTools#generateRIS(StructElement)
     * @verifies use live volume count for anchors
     */
    @Test
    void generateRIS_shouldUseLiveVolumeCountForAnchors() throws Exception {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":306653648", null);
        Assertions.assertNotNull(doc);
        StructElement anchor = new StructElement((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
        Assertions.assertTrue(anchor.isAnchor());

        long liveCount = DataManager.getInstance()
                .getSearchIndex()
                .getHitCount(SolrConstants.PI_PARENT + ":\"306653648\" AND " + SolrConstants.ISWORK + ":true");
        Assertions.assertTrue(liveCount > 0, "test index anchor must have at least one volume");

        String ris = MetadataTools.generateRIS(anchor);
        Assertions.assertNotNull(ris);
        Assertions.assertTrue(ris.contains("NV  - " + liveCount + "\r\n"));
    }
}
