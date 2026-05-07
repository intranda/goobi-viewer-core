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
package io.goobi.viewer.model.toc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;

class TocMakerTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see TocMaker#getSolrFieldsToFetch(String)
     * @verifies return both static and configured fields
     */
    @Test
    void getSolrFieldsToFetch_shouldReturnBothStaticAndConfiguredFields() {
        List<?> fields = TocMaker.getSolrFieldsToFetch("_DEFAULT");
        Assertions.assertNotNull(fields);
        // 17 REQUIRED_FIELDS + 1 base param (MD_CREATOR; LABEL is deduped) + 2 params × 2 LANG_ variants (EN, DE) + 3 ancestor/GROUPID_* fields (#27788)
        Assertions.assertEquals(25, fields.size());
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies include anchor element full volume tree and sibling volume top elements in TOC
     */
    @Test
    void generateToc_shouldIncludeAnchorElementFullVolumeTreeAndSiblingVolumeTopElementsInTOC() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, true, "image/tiff", 1, -1);
        Assertions.assertNotNull(tocElements);
        Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assertions.assertEquals(111, tocElements.get(StringConstants.DEFAULT_NAME).size()); // 1 anchor + 104 elements of volume 306653648_1891 + 6 sibling volume top elements
        // Anchor first
        Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 104; ++i) {
            TOCElement element = tocElements.get(StringConstants.DEFAULT_NAME).get(i);
            Assertions.assertEquals("306653648_1891", element.getTopStructPi());
        }
        // Sibling volumes (just topstruct)
        Assertions.assertEquals("306653648_1892", tocElements.get(StringConstants.DEFAULT_NAME).get(105).getTopStructPi());
        Assertions.assertEquals("306653648_1893", tocElements.get(StringConstants.DEFAULT_NAME).get(106).getTopStructPi());
        Assertions.assertEquals("306653648_1894", tocElements.get(StringConstants.DEFAULT_NAME).get(107).getTopStructPi());
        Assertions.assertEquals("306653648_1897", tocElements.get(StringConstants.DEFAULT_NAME).get(108).getTopStructPi());
        Assertions.assertEquals("306653648_1898", tocElements.get(StringConstants.DEFAULT_NAME).get(109).getTopStructPi());
        Assertions.assertEquals("306653648_1899", tocElements.get(StringConstants.DEFAULT_NAME).get(110).getTopStructPi());
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies return anchor element followed by all volume elements when siblings excluded
     */
    @Test
    void generateToc_shouldReturnAnchorElementFollowedByAllVolumeElementsWhenSiblingsExcluded() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, false, "image/tiff", 1, -1);
        Assertions.assertNotNull(tocElements);
        Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assertions.assertEquals(105, tocElements.get(StringConstants.DEFAULT_NAME).size()); // 1 anchor + 104 elements of volume ZDB026544598_0001
        // Anchor first
        Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        // First volume (complete tree)
        for (int i = 1; i <= 104; ++i) {
            TOCElement element = tocElements.get(StringConstants.DEFAULT_NAME).get(i);
            Assertions.assertEquals("306653648_1891", element.getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies return anchor plus all child volumes when generating anchor TOC
     */
    @Test
    void generateToc_shouldReturnAnchorPlusAllChildVolumesWhenGeneratingAnchorTOC() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        TOC toc = new TOC();
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, "image/tiff", 1, -1);
        Assertions.assertNotNull(tocElements);
        Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
        Assertions.assertEquals(8, tocElements.get(StringConstants.DEFAULT_NAME).size());
        Assertions.assertEquals(7, toc.getTotalTocSize()); // 7 volumes
        Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
        for (int i = 1; i < tocElements.get(StringConstants.DEFAULT_NAME).size(); ++i) {
            Assertions.assertTrue(tocElements.get(StringConstants.DEFAULT_NAME).get(i).getTopStructPi().startsWith("306653648_189"));
        }
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies return different volume subsets per page when anchor TOC is paginated
     */
    @Test
    void generateToc_shouldReturnDifferentVolumeSubsetsPerPageWhenAnchorTOCIsPaginated() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        TOC toc = new TOC();
        {
            // Page 1
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, "image/tiff", 1, 3);
            Assertions.assertNotNull(tocElements);
            Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assertions.assertEquals(4, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assertions.assertEquals(7, toc.getTotalTocSize());
            Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assertions.assertEquals("306653648_1891", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
            Assertions.assertEquals("306653648_1892", tocElements.get(StringConstants.DEFAULT_NAME).get(2).getTopStructPi());
            Assertions.assertEquals("306653648_1893", tocElements.get(StringConstants.DEFAULT_NAME).get(3).getTopStructPi());
        }
        {
            // Page 2
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, "image/tiff", 2, 3);
            Assertions.assertNotNull(tocElements);
            Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assertions.assertEquals(4, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assertions.assertEquals(7, toc.getTotalTocSize());
            Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assertions.assertEquals("306653648_1894", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
            Assertions.assertEquals("306653648_1897", tocElements.get(StringConstants.DEFAULT_NAME).get(2).getTopStructPi());
            Assertions.assertEquals("306653648_1898", tocElements.get(StringConstants.DEFAULT_NAME).get(3).getTopStructPi());
        }
        {
            // Page 3
            Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(toc, structElement, true, "image/tiff", 3, 3);
            Assertions.assertNotNull(tocElements);
            Assertions.assertNotNull(tocElements.get(StringConstants.DEFAULT_NAME));
            Assertions.assertEquals(2, tocElements.get(StringConstants.DEFAULT_NAME).size());
            Assertions.assertEquals(7, toc.getTotalTocSize());
            Assertions.assertEquals("306653648", tocElements.get(StringConstants.DEFAULT_NAME).get(0).getTopStructPi());
            Assertions.assertEquals("306653648_1899", tocElements.get(StringConstants.DEFAULT_NAME).get(1).getTopStructPi());
        }
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies throw IllegalArgumentException if structElement is null
     */
    @Test
    void generateToc_shouldThrowIllegalArgumentExceptionIfStructElementIsNull() {
        TOC toc = new TOC();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> TocMaker.generateToc(toc, null, true, "image/tiff", 1, -1));
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies throw IllegalArgumentException if toc is null
     */
    @Test
    void generateToc_shouldThrowIllegalArgumentExceptionIfTocIsNull() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);
        StructElement se = new StructElement(iddoc);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> TocMaker.generateToc(null, se, true, "image/tiff", 1, -1));
    }

    /**
     * @see TocMaker#buildLabel(SolrDocument, String)
     * @verifies build configured label correctly
     */
    @Test
    void buildLabel_shouldBuildConfiguredLabelCorrectly() {
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.LABEL, "label");
            doc.setField("MD_CREATOR", "creator");
            String label = TocMaker.buildLabel(doc, null).getValue().orElse("");
            Assertions.assertEquals("label / creator", label);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.CURRENTNO, "1");
            doc.setField("MD_TITLE", "title");
            Assertions.assertEquals("Number 1: title", TocMaker.buildLabel(doc, "PeriodicalVolume").getValue().orElse(""));
        }
    }

    /**
     * @see TocMaker#buildLabel(SolrDocument, String)
     * @verifies fill remaining parameters correctly if docstruct fallback used
     */
    @Test
    void buildLabel_shouldFillRemainingParametersCorrectlyIfDocstructFallbackUsed() {

        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.CURRENTNO, "1");
        doc.setField(SolrConstants.DOCSTRCT, "PeriodicalVolume");
        IMetadataValue value = TocMaker.buildLabel(doc, "PeriodicalVolume");
        String label = value.getValue(Locale.ENGLISH).orElse("");
        Assertions.assertEquals("Number 1: Periodical volume", label);
    }

    /**
     * Test that parseVolumeLabelConfig extracts all brace-delimited field names
     * from the label configuration string.
     *
     * @see TocMaker#parseVolumeLabelConfig(String)
     * @verifies parse all field names correctly
     */
    @Test
    void parseVolumeLabelConfig_shouldParseAllFieldNamesCorrectly() {
        // Configuration string with three placeholder fields in braces
        List<String> fields = TocMaker.parseVolumeLabelConfig("Number {CURRENTNO}: {MD_TITLE} ({YEAR})");
        Assertions.assertNotNull(fields);
        Assertions.assertEquals(3, fields.size());
        Assertions.assertEquals("CURRENTNO", fields.get(0));
        Assertions.assertEquals("MD_TITLE", fields.get(1));
        Assertions.assertEquals("YEAR", fields.get(2));
    }

    /**
     * @see TocMaker#createOrderedGroupDocMap(List, List, String)
     * @verifies create correctly sorted map
     */
    @Test
    void createOrderedGroupDocMap_shouldCreateCorrectlySortedMap() {
        String pi = "PPN123";
        List<SolrDocument> groupMemberDocs = new ArrayList<>(5);
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(1));
            doc.setField("GROUPID_SERIES", pi);
            doc.setField("GROUPORDER_SERIES", 5);
            groupMemberDocs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(2));
            doc.setField("GROUPID_SERIES_2", pi);
            doc.setField("GROUPORDER_SERIES_2", 4);
            groupMemberDocs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(3));
            doc.setField("GROUPID_SERIES_3", pi);
            doc.setField("GROUPORDER_SERIES_3", 3);
            groupMemberDocs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(4));
            doc.setField("GROUPID_SERIES_2", pi);
            doc.setField("GROUPORDER_SERIES_2", 2);
            groupMemberDocs.add(doc);
        }
        {
            SolrDocument doc = new SolrDocument();
            doc.setField(SolrConstants.IDDOC, String.valueOf(5));
            doc.setField("GROUPID_SERIES", pi);
            doc.setField("GROUPORDER_SERIES", 1);
            groupMemberDocs.add(doc);
        }
        Map<Integer, SolrDocument> result = TocMaker.createOrderedGroupDocMap(groupMemberDocs,
                Arrays.asList(new String[] { "GROUPID_SERIES", "GROUPID_SERIES_2", "GROUPID_SERIES_3" }), pi);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.size());
        Assertions.assertEquals("5", result.get(1).getFieldValue(SolrConstants.IDDOC));
        Assertions.assertEquals("4", result.get(2).getFieldValue(SolrConstants.IDDOC));
        Assertions.assertEquals("3", result.get(3).getFieldValue(SolrConstants.IDDOC));
        Assertions.assertEquals("2", result.get(4).getFieldValue(SolrConstants.IDDOC));
        Assertions.assertEquals("1", result.get(5).getFieldValue(SolrConstants.IDDOC));
    }

    /**
     * @see TocMaker#generateToc(TOC,StructElement,boolean,String,int,int)
     * @verifies prefer the ancestor-containing tree over a standalone tree
     *
     * Safety net for optimizing the tree-selection logic in buildToc:
     * buildToc builds one TOC tree per configured ancestor field and returns the LARGEST.
     * For a volume that belongs to an anchor, the tree with the ancestor (N+1 elements)
     * must win over a hypothetical standalone tree (N elements).
     * Any optimization that changes "return largest" to "return first non-empty" must
     * not break this: the ancestor must always appear at index 0.
     */
    @Test
    void generateToc_shouldPreferTreeWithAncestorOverStandaloneTree() throws Exception {
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, false, "image/tiff", 1, -1);
        Assertions.assertNotNull(tocElements);
        List<TOCElement> elements = tocElements.get(StringConstants.DEFAULT_NAME);
        Assertions.assertNotNull(elements);
        // The tree with the ancestor (anchor) contains more elements than the volume alone.
        // buildToc must select this larger tree, placing the anchor at index 0.
        Assertions.assertTrue(elements.size() > 104, "Tree with ancestor must be larger than the volume's own struct tree");
        Assertions.assertEquals("306653648", elements.get(0).getTopStructPi(),
                "Anchor must be at index 0 — the ancestor-containing tree was selected as the largest");
        // Volume elements immediately follow the anchor
        Assertions.assertEquals("306653648_1891", elements.get(1).getTopStructPi());
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies render unique PIs that match the existing anchor structure invariant
     */
    @Test
    void generateToc_shouldRenderUniquePisThatMatchTheExistingAnchorStructureInvariant() throws Exception {
        // Anchor 306653648 has 7 volumes (1891/1892/1893/1894/1897/1898/1899). Opening volume 306653648_1891
        // with addAllSiblings=true renders a TOC containing the volume itself plus the 6 sibling top-elements,
        // per existing TocMakerTest.generateToc_shouldIncludeAnchorElementFullVolumeTreeAndSiblingVolumeTopElementsInTOC.
        // The total list has 111 elements (1 anchor + 104 volume struct elements + 6 sibling volume top elements),
        // referencing 8 unique top-struct PIs (anchor + main volume + 6 siblings).
        String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(iddoc);
        StructElement structElement = new StructElement(iddoc);
        Map<String, List<TOCElement>> tocElements = TocMaker.generateToc(new TOC(), structElement, true, "image/tiff", 1, -1);

        Set<String> uniquePis = new HashSet<>();
        for (TOCElement element : tocElements.get(StringConstants.DEFAULT_NAME)) {
            if (element.getTopStructPi() != null) uniquePis.add(element.getTopStructPi());
        }
        Assertions.assertFalse(uniquePis.isEmpty(), "Rendered TOC has no top-struct PIs at all");
        Assertions.assertEquals(8, uniquePis.size(), "Rendered TOC should reference exactly 8 unique PIs");
    }

    /**
     * @see TocMaker#isCalendarEligibleParent(SolrDocument)
     * @verifies return false when doc is null
     */
    @Test
    void isCalendarEligibleParent_shouldReturnFalseWhenDocIsNull() throws Exception {
        Assertions.assertFalse(TocMaker.isCalendarEligibleParent(null));
    }

    /**
     * @see TocMaker#isCalendarEligibleParent(SolrDocument)
     * @verifies return false when doc is neither anchor nor group
     */
    @Test
    void isCalendarEligibleParent_shouldReturnFalseWhenDocIsNeitherAnchorNorGroup() throws Exception {
        // Plain DOCSTRCT doc (not anchor, not group) — early-out before any Solr access.
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.DOCTYPE, "DOCSTRCT");
        doc.setField(SolrConstants.DOCSTRCT, "Newspaper");
        doc.setField(SolrConstants.PI, "test_pi");
        Assertions.assertFalse(TocMaker.isCalendarEligibleParent(doc));
    }

    /**
     * @see TocMaker#isCalendarEligibleParent(SolrDocument)
     * @verifies return false when docstruct is not in the whitelist
     */
    @Test
    void isCalendarEligibleParent_shouldReturnFalseWhenDocstructIsNotInTheWhitelist() throws Exception {
        // Test config whitelist is [Newspaper, Periodical]. An anchor with a different docstruct
        // must NOT trigger the sibling-skip — protects multi-volume monographs etc. from accidental
        // TOC suppression. Early-out before the multi-year facet query, so no Solr access required.
        Assertions.assertFalse(DataManager.getInstance().getConfiguration().getCalendarDocStructTypes().isEmpty(),
                "Test config has unexpectedly an empty calendar docstruct whitelist; this test needs entries");
        SolrDocument doc = new SolrDocument();
        doc.setField(SolrConstants.ISANCHOR, Boolean.TRUE);
        doc.setField(SolrConstants.DOCTYPE, "DOCSTRCT");
        doc.setField(SolrConstants.DOCSTRCT, "MultiVolumeWork");
        doc.setField(SolrConstants.PI, "test_pi");
        Assertions.assertFalse(TocMaker.isCalendarEligibleParent(doc));
    }
}
