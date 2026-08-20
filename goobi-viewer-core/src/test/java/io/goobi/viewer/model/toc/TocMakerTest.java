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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

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
        Assertions.assertEquals(26, fields.size());
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
     * Test that parseVolumeLabelConfig extracts all brace-delimited field names from the label configuration string.
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
     *           Safety net for optimizing the tree-selection logic in buildToc: buildToc builds one TOC tree per configured ancestor field and
     *           returns the LARGEST. For a volume that belongs to an anchor, the tree with the ancestor (N+1 elements) must win over a hypothetical
     *           standalone tree (N elements). Any optimization that changes "return largest" to "return first non-empty" must not break this: the
     *           ancestor must always appear at index 0.
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
            if (element.getTopStructPi() != null)
                uniquePis.add(element.getTopStructPi());
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

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies include loaded volume and its structure when anchor is calendar eligible
     */
    @Test
    void generateToc_shouldIncludeLoadedVolumeAndItsStructureWhenAnchorIsCalendarEligible() throws Exception {
        TOC toc = buildTocWithCalendarEligibleAnchor(false);
        List<TOCElement> elements = toc.getTocElements();
        // The anchor heads the TOC, the loaded volume and its 104 structure elements follow it - the same
        // result the non-calendar build produces with siblings excluded, i.e. the optimization changes the
        // query but not the TOC. While the calendar branch ended the whole walk at the anchor, the volume
        // was absent and only the anchor remained.
        Assertions.assertEquals("306653648", elements.get(0).getTopStructPi());
        long volumeElements = elements.stream().filter(element -> "306653648_1891".equals(element.getTopStructPi())).count();
        Assertions.assertEquals(104, volumeElements, "Loaded volume and its structure elements are missing from the TOC");
        Assertions.assertEquals(105, elements.size(), "TOC should consist of the anchor plus the loaded volume tree");
        // The lookup ActiveDocumentBean.getTitleBarLabel() performs for the loaded record. It returned null
        // while the volume was missing, which sent the title bar into its StructElement fallback - and that
        // one hands out the raw, untranslated DOCSTRCT.
        Assertions.assertNotNull(toc.getLabel("306653648_1891"), "TOC must provide a label for the loaded record");
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies still list sibling volumes of calendar eligible anchor when siblings requested
     */
    @Test
    void generateToc_shouldStillListSiblingVolumesOfCalendarEligibleAnchorWhenSiblingsRequested() throws Exception {
        // With listSiblingRecords enabled the sibling volumes are requested output: they are the only way to
        // reach another volume from a volume page, because the calendar TOC view only renders for a loaded
        // anchor or group. So narrowing the query down to the navigation path must not happen here, and the
        // result has to stay identical to the non-calendar build (1 anchor + 104 volume elements + 6 siblings).
        List<TOCElement> elements = buildTocWithCalendarEligibleAnchor(true).getTocElements();
        List<String> siblingPis = elements.stream()
                .map(TOCElement::getTopStructPi)
                .filter(pi -> pi != null && pi.startsWith("306653648_") && !"306653648_1891".equals(pi))
                .distinct()
                .sorted()
                .toList();
        Assertions.assertEquals(List.of("306653648_1892", "306653648_1893", "306653648_1894", "306653648_1897", "306653648_1898", "306653648_1899"),
                siblingPis, "Requested sibling volumes must survive the calendar optimization");
        Assertions.assertEquals(111, elements.size());
    }

    /**
     * @see TocMaker#generateToc(TOC, StructElement, boolean, String, int, int)
     * @verifies restrict sibling query to navigation path when anchor is calendar eligible
     */
    @Test
    void generateToc_shouldRestrictSiblingQueryToNavigationPathWhenAnchorIsCalendarEligible() throws Exception {
        // Performance guard. The TOC contents alone cannot show this: a build that enumerates all 23.705
        // issues of a newspaper and then refrains from adding them produces the same element list as one
        // that never asks for them. So watch the queries themselves and require the anchor's child query to
        // name the volume on the navigation path, which is what bounds the result set.
        // Note this observes one specific search() overload - if the production code moves to another one,
        // the first assertion below fails rather than passing silently.
        String volumeIddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
        Assertions.assertNotNull(volumeIddoc);
        SolrSearchIndex originalIndex = DataManager.getInstance().getSearchIndex();
        List<String> queries = Collections.synchronizedList(new ArrayList<>());
        SolrSearchIndex indexSpy = Mockito.spy(originalIndex);
        Mockito.doAnswer(invocation -> {
            queries.add(invocation.getArgument(0));
            return invocation.callRealMethod();
        }).when(indexSpy).search(Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.any());
        DataManager.getInstance().injectSearchIndex(indexSpy);
        try {
            buildTocWithCalendarEligibleAnchor(false);
        } finally {
            DataManager.getInstance().injectSearchIndex(originalIndex);
        }

        List<String> anchorChildQueries = queries.stream().filter(query -> query.contains(SolrConstants.PI_PARENT + ":\"306653648\"")).toList();
        Assertions.assertFalse(anchorChildQueries.isEmpty(), "Expected the anchor's child query to run at all; observed queries: " + queries);
        for (String query : anchorChildQueries) {
            Assertions.assertTrue(query.contains("+" + SolrConstants.IDDOC + ":(") && query.contains(volumeIddoc),
                    "The child query of a calendar-eligible anchor must be restricted to the navigation path, otherwise it returns every sibling: "
                            + query);
        }
    }

    /**
     * @see TocMaker#buildMainChainRestriction(List, String)
     * @verifies return clause for every chain iddoc except the parent
     */
    @Test
    void buildMainChainRestriction_shouldReturnClauseForEveryChainIddocExceptTheParent() {
        // Deeper hierarchies put more than one IDDOC below the calendar-eligible node, and a chain that
        // accumulated several ancestor fields may hold unrelated IDDOCs; all of them are OR-joined here and
        // sorted out by the ancestor field clause of the query this is appended to. Null entries are skipped.
        Assertions.assertEquals(" +IDDOC:(\"1\" OR \"3\")", TocMaker.buildMainChainRestriction(Arrays.asList("1", null, "2", "3"), "2"));
        Assertions.assertEquals(" +IDDOC:(\"1\")", TocMaker.buildMainChainRestriction(Arrays.asList("1", "2"), "2"));
    }

    /**
     * @see TocMaker#buildMainChainRestriction(List, String)
     * @verifies return null if chain holds no other iddoc
     */
    @Test
    void buildMainChainRestriction_shouldReturnNullIfChainHoldsNoOtherIddoc() {
        // Returning null is what tells the caller there is nothing on the path below this document, so the
        // query can be skipped altogether instead of being restricted to an empty set
        Assertions.assertNull(TocMaker.buildMainChainRestriction(Arrays.asList("2"), "2"));
        Assertions.assertNull(TocMaker.buildMainChainRestriction(Arrays.asList((String) null), "2"));
    }

    /**
     * @see TocMaker#buildMainChainRestriction(List, String)
     * @verifies return null if chain is null or empty
     */
    @Test
    void buildMainChainRestriction_shouldReturnNullIfChainIsNullOrEmpty() {
        Assertions.assertNull(TocMaker.buildMainChainRestriction(null, "2"));
        Assertions.assertNull(TocMaker.buildMainChainRestriction(new ArrayList<>(), "2"));
    }

    /**
     * Generates the TOC of volume 306653648_1891 with its anchor forced to be calendar-eligible.
     *
     * <p>
     * Two config values are overridden to reproduce a production newspaper setup on the test data:
     *
     * <ul>
     * <li>The test index stores the anchor's docstruct as lower case "newspaper" while the test config whitelists "Newspaper", so
     * {@link TocMaker#isCalendarEligibleParent(SolrDocument)} would never fire here. The precondition assertion below guarantees the calendar branch
     * really is the code path under test rather than the regular sibling build.</li>
     * <li>The ancestor identifier fields are reduced to PI_PARENT, which is what the reference config ships. buildToc builds one tree per ancestor
     * field and returns the largest one; with the test config's additional MD_OTHERANCESTOR and GROUPID_1 fields, their ancestor lists come out empty
     * and the resulting trees are rooted in the volume itself, which would mask an incomplete anchor tree.</li>
     * </ul>
     *
     * <p>
     * Generation goes through {@link TOC#generate(StructElement, boolean, String, int)} rather than calling {@link TocMaker} directly, so the tests
     * exercise the same entry point ActiveDocumentBean uses.
     *
     * @param addAllSiblings value for the listSiblingRecords behaviour under test
     * @return the generated TOC
     * @throws Exception on Solr, config or permission failures
     */
    private static TOC buildTocWithCalendarEligibleAnchor(boolean addAllSiblings) throws Exception {
        Configuration originalConfig = DataManager.getInstance().getConfiguration();
        Configuration configSpy = Mockito.spy(originalConfig);
        Mockito.doReturn(List.of("newspaper")).when(configSpy).getCalendarDocStructTypes();
        // Fresh mutable list per call because buildToc prepends PI_PARENT to the returned list when absent
        Mockito.doAnswer(invocation -> new ArrayList<>(List.of(SolrConstants.PI_PARENT))).when(configSpy).getAncestorIdentifierFields();
        DataManager.getInstance().injectConfiguration(configSpy);
        try {
            SolrDocument anchorDoc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":306653648", null);
            Assertions.assertNotNull(anchorDoc);
            Assertions.assertTrue(TocMaker.isCalendarEligibleParent(anchorDoc),
                    "Test precondition: anchor 306653648 must be calendar-eligible for this test to exercise the calendar branch");

            String iddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier("306653648_1891");
            Assertions.assertNotNull(iddoc);
            TOC toc = new TOC();
            toc.generate(new StructElement(iddoc), addAllSiblings, "image/tiff", 1);
            Assertions.assertFalse(toc.getTocElements().isEmpty());
            return toc;
        } finally {
            DataManager.getInstance().injectConfiguration(originalConfig);
        }
    }
}
