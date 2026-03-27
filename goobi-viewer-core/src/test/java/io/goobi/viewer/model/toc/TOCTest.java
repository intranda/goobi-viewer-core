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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.StringConstants;

class TOCTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see TOC#getNumPages()
     * @verifies return ceiling division of total TOC size by page size
     */
    @Test
    void getNumPages_shouldReturnCeilingDivisionOfTotalTOCSizeByPageSize() {
        TOC toc = new TOC();
        toc.setTotalTocSize(70);
        Assertions.assertEquals(7, toc.getNumPages());
        toc.setTotalTocSize(77);
        Assertions.assertEquals(8, toc.getNumPages());
    }

    /**
     * @verifies return correct label
     */
    @Test
    void getLabel_shouldReturnCorrectLabel() {
        TOC toc = new TOC();
        toc.setTocElementMap(new HashMap<>());
        toc.getTocElementMap().put(StringConstants.DEFAULT_NAME, new ArrayList<>(3));
        toc.getTocElementMap()
                .get(StringConstants.DEFAULT_NAME)
                .add(new TOCElement(new SimpleMetadataValue("one"), "0", null, "1", "LOG_0000", 0, "PPN_anchor", null, false, true, false, null,
                        "periodical", null));
        toc.getTocElementMap()
                .get(StringConstants.DEFAULT_NAME)
                .add(new TOCElement(new SimpleMetadataValue("two"), "1", null, "2", "LOG_0001", 1, "PPN_volume", null, false, false, true, null,
                        "periodical_volume", null));
        toc.getTocElementMap()
                .get(StringConstants.DEFAULT_NAME)
                .add(new TOCElement(new SimpleMetadataValue("three"), "1", null, "3", "LOG_0002", 2, "PPN_volume", null, false, false, true, null,
                        "article", null));

        Assertions.assertEquals("one", toc.getLabel("PPN_anchor"));
        Assertions.assertEquals("two", toc.getLabel("PPN_volume"));
    }

    /**
     * @see TOC#setCurrentPage(int)
     * @verifies set value to 1 if given value too low
     */
    @Test
    void setCurrentPage_shouldSetValueTo1IfGivenValueTooLow() {
        TOC toc = new TOC();
        toc.setCurrentPage(0);
        Assertions.assertEquals(1, toc.getCurrentPage());
    }

    /**
     * @see TOC#setCurrentPage(int)
     * @verifies set value to last page number if given value too high
     */
    @Test
    void setCurrentPage_shouldSetValueToLastPageNumberIfGivenValueTooHigh() {
        TOC toc = new TOC();
        toc.setTotalTocSize(70);
        Assertions.assertEquals(7, toc.getNumPages());
        // Valid value
        toc.setCurrentPage(4);
        Assertions.assertEquals(4, toc.getCurrentPage());
        // Invalid value
        toc.setCurrentPage(40);
        Assertions.assertEquals(7, toc.getCurrentPage());
    }

    /**
     * @see TOC#getGroupNames()
     * @verifies return empty list if map is null
     */
    @Test
    void getGroupNames_shouldReturnEmptyListIfMapIsNull() {
        TOC toc = new TOC();
        List<String> names = toc.getGroupNames();
        assertNotNull(names);
        assertTrue(names.isEmpty());
    }

    /**
     * @see TOC#getGroupNames()
     * @verifies return group names from map
     */
    @Test
    void getGroupNames_shouldReturnGroupNamesFromMap() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        map.put(StringConstants.DEFAULT_NAME, new ArrayList<>());
        map.put("group2", new ArrayList<>());
        toc.setTocElementMap(map);
        List<String> names = toc.getGroupNames();
        assertEquals(2, names.size());
        assertTrue(names.contains(StringConstants.DEFAULT_NAME));
        assertTrue(names.contains("group2"));
    }

    /**
     * @see TOC#getViewForGroup(String)
     * @verifies return null if map is null
     */
    @Test
    void getViewForGroup_shouldReturnNullIfMapIsNull() {
        TOC toc = new TOC();
        assertNull(toc.getViewForGroup(StringConstants.DEFAULT_NAME));
    }

    /**
     * @see TOC#getViewForGroup(String)
     * @verifies return elements for group
     */
    @Test
    void getViewForGroup_shouldReturnElementsForGroup() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        List<TOCElement> elements = new ArrayList<>();
        elements.add(new TOCElement(new SimpleMetadataValue("elem"), "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null));
        map.put(StringConstants.DEFAULT_NAME, elements);
        toc.setTocElementMap(map);
        List<TOCElement> result = toc.getViewForGroup(StringConstants.DEFAULT_NAME);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * @see TOC#getFlatView()
     * @verifies return default group elements
     */
    @Test
    void getFlatView_shouldReturnDefaultGroupElements() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        List<TOCElement> elements = new ArrayList<>();
        elements.add(new TOCElement(new SimpleMetadataValue("elem"), "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null));
        map.put(StringConstants.DEFAULT_NAME, elements);
        toc.setTocElementMap(map);
        List<TOCElement> result = toc.getFlatView();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * @see TOC#getTocElements()
     * @verifies return empty list if map is null
     */
    @Test
    void getTocElements_shouldReturnEmptyListIfMapIsNull() {
        TOC toc = new TOC();
        List<TOCElement> result = toc.getTocElements();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * @see TOC#findTocElementIndexByIddoc(String)
     * @verifies return minus one for null or empty iddoc
     */
    @Test
    void findTocElementIndexByIddoc_shouldReturnMinusOneForNullOrEmptyIddoc() {
        TOC toc = new TOC();
        toc.setTocElementMap(new HashMap<>());
        toc.getTocElementMap().put(StringConstants.DEFAULT_NAME, new ArrayList<>());
        assertEquals(-1, toc.findTocElementIndexByIddoc(null));
        assertEquals(-1, toc.findTocElementIndexByIddoc(""));
    }

    /**
     * @see TOC#findTocElementIndexByIddoc(String)
     * @verifies return minus one if not found
     */
    @Test
    void findTocElementIndexByIddoc_shouldReturnMinusOneIfNotFound() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        List<TOCElement> elements = new ArrayList<>();
        elements.add(new TOCElement(new SimpleMetadataValue("e1"), "1", null, "111", "LOG_0001", 0, null, null, false, false, false, null, null, null));
        map.put(StringConstants.DEFAULT_NAME, elements);
        toc.setTocElementMap(map);
        assertEquals(-1, toc.findTocElementIndexByIddoc("999"));
    }

    /**
     * @see TOC#findTocElementIndexByIddoc(String)
     * @verifies return correct index
     */
    @Test
    void findTocElementIndexByIddoc_shouldReturnCorrectIndex() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        List<TOCElement> elements = new ArrayList<>();
        elements.add(new TOCElement(new SimpleMetadataValue("e1"), "1", null, "111", "LOG_0001", 0, null, null, false, false, false, null, null, null));
        elements.add(new TOCElement(new SimpleMetadataValue("e2"), "1", null, "222", "LOG_0002", 1, null, null, false, false, false, null, null, null));
        elements.add(new TOCElement(new SimpleMetadataValue("e3"), "1", null, "333", "LOG_0003", 2, null, null, false, false, false, null, null, null));
        map.put(StringConstants.DEFAULT_NAME, elements);
        toc.setTocElementMap(map);
        assertEquals(0, toc.findTocElementIndexByIddoc("111"));
        assertEquals(1, toc.findTocElementIndexByIddoc("222"));
        assertEquals(2, toc.findTocElementIndexByIddoc("333"));
    }

    /**
     * @see TOC#isHasChildren()
     * @verifies return false if map is null
     */
    @Test
    void isHasChildren_shouldReturnFalseIfMapIsNull() {
        TOC toc = new TOC();
        assertFalse(toc.isHasChildren());
    }

    /**
     * @see TOC#isHasChildren()
     * @verifies return false for single element without children
     */
    @Test
    void isHasChildren_shouldReturnFalseForSingleElementWithoutChildren() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        TOCElement root = new TOCElement(new SimpleMetadataValue("root"), "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        // hasChild defaults to false
        map.put(StringConstants.DEFAULT_NAME, Collections.singletonList(root));
        toc.setTocElementMap(map);
        assertFalse(toc.isHasChildren());
    }

    /**
     * @see TOC#isHasChildren()
     * @verifies return true for multiple elements
     */
    @Test
    void isHasChildren_shouldReturnTrueForMultipleElements() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        TOCElement root = new TOCElement(new SimpleMetadataValue("root"), "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        TOCElement child = new TOCElement(new SimpleMetadataValue("child"), "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, child));
        toc.setTocElementMap(map);
        assertTrue(toc.isHasChildren());
    }

    /**
     * @see TOC#isHasChildren()
     * @verifies return true for single element with children flag set
     */
    @Test
    void isHasChildren_shouldReturnTrueForSingleElementWithChildrenFlagSet() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        TOCElement root = new TOCElement(new SimpleMetadataValue("root"), "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        root.setHasChild(true);
        map.put(StringConstants.DEFAULT_NAME, Collections.singletonList(root));
        toc.setTocElementMap(map);
        assertTrue(toc.isHasChildren());
    }

    /**
     * @see TOC#expandAll()
     * @verifies make all elements visible and expand parents
     */
    @Test
    void expandAll_shouldMakeAllElementsVisibleAndExpandParents() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        TOCElement root = new TOCElement(new SimpleMetadataValue("root"), "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        root.setHasChild(true);
        root.setExpanded(false);
        TOCElement child = new TOCElement(new SimpleMetadataValue("child"), "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        child.setVisible(false);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, child));
        toc.setTocElementMap(map);

        toc.expandAll();

        assertTrue(root.isVisible());
        assertTrue(root.isExpanded()); // has children -> gets expanded
        assertTrue(child.isVisible());
        assertFalse(child.isExpanded()); // no children -> stays collapsed
    }

    /**
     * @see TOC#collapseAll()
     * @verifies keep root visible and hide non-root elements
     */
    @Test
    void collapseAll_shouldKeepRootVisibleAndHideNonRootElements() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        TOCElement root = new TOCElement(new SimpleMetadataValue("root"), "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        root.setExpanded(true);
        TOCElement child = new TOCElement(new SimpleMetadataValue("child"), "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        child.setVisible(true);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, child));
        toc.setTocElementMap(map);

        toc.collapseAll();

        assertTrue(root.isVisible()); // level 0 stays visible
        assertFalse(root.isExpanded()); // but gets collapsed
        assertFalse(child.isVisible()); // level 1 becomes invisible
    }

    /**
     * @see TOC#buildTree(String, int, int, int, String)
     * @verifies throw IllegalArgumentException if group is null
     */
    @Test
    void buildTree_shouldThrowIllegalArgumentExceptionIfGroupIsNull() {
        TOC toc = new TOC();
        assertThrows(IllegalArgumentException.class, () -> toc.buildTree(null, 1, 5, 0, null));
    }

    /**
     * @see TOC#buildTree(String, int, int, int, String)
     * @verifies collapse children when sibling count exceeds threshold
     */
    @Test
    void buildTree_shouldCollapseChildrenWhenSiblingCountExceedsThreshold() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        TOCElement root = new TOCElement(null, "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        // 6 siblings at level 1, threshold is 5 -> should trigger collapse
        TOCElement c1 = new TOCElement(null, "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        TOCElement c2 = new TOCElement(null, "1", null, "3", "LOG_0003", 1, null, null, false, false, false, null, null, null);
        TOCElement c3 = new TOCElement(null, "1", null, "4", "LOG_0004", 1, null, null, false, false, false, null, null, null);
        TOCElement c4 = new TOCElement(null, "1", null, "5", "LOG_0005", 1, null, null, false, false, false, null, null, null);
        TOCElement c5 = new TOCElement(null, "1", null, "6", "LOG_0006", 1, null, null, false, false, false, null, null, null);
        TOCElement c6 = new TOCElement(null, "1", null, "7", "LOG_0007", 1, null, null, false, false, false, null, null, null);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, c1, c2, c3, c4, c5, c6));
        toc.setTocElementMap(map);

        toc.buildTree(StringConstants.DEFAULT_NAME, 1, 5, 0, null);

        assertTrue(root.isVisible());
        assertFalse(root.isExpanded()); // parent collapsed by length threshold
        assertFalse(c1.isVisible());
        assertFalse(c2.isVisible());
        assertFalse(c6.isVisible());
    }

    /**
     * @see TOC#buildTree(String, int, int, int, String)
     * @verifies not collapse children when sibling count is below threshold
     */
    @Test
    void buildTree_shouldNotCollapseChildrenWhenBelowThreshold() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        TOCElement root = new TOCElement(null, "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        // 3 siblings at level 1, threshold is 5 -> no collapse
        TOCElement c1 = new TOCElement(null, "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        TOCElement c2 = new TOCElement(null, "1", null, "3", "LOG_0003", 1, null, null, false, false, false, null, null, null);
        TOCElement c3 = new TOCElement(null, "1", null, "4", "LOG_0004", 1, null, null, false, false, false, null, null, null);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, c1, c2, c3));
        toc.setTocElementMap(map);

        toc.buildTree(StringConstants.DEFAULT_NAME, 1, 5, 0, null);

        assertTrue(root.isVisible());
        assertTrue(root.isExpanded()); // not collapsed, level 1 is within visibleLevel
        assertTrue(c1.isVisible());
        assertTrue(c2.isVisible());
        assertTrue(c3.isVisible());
    }

    /**
     * @see TOC#getLabel(String)
     * @verifies return null for empty pi
     */
    @Test
    void getLabel_shouldReturnNullForEmptyPi() {
        TOC toc = new TOC();
        assertNull(toc.getLabel(null));
        assertNull(toc.getLabel(""));
    }

    /**
     * @see TOC#getLabel(String)
     * @verifies return null if pi not found
     */
    @Test
    void getLabel_shouldReturnNullIfPiNotFound() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        map.put(StringConstants.DEFAULT_NAME, Collections.singletonList(
                new TOCElement(new SimpleMetadataValue("title"), "1", null, "1", "LOG_0001", 0, "PPN001", null, false, false, false, null, null, null)));
        toc.setTocElementMap(map);
        assertNull(toc.getLabel("UNKNOWN_PI"));
    }

    /**
     * @see TOC#getActiveElement()
     * @verifies expand direct children when tocVisible is set
     */
    @Test
    void getActiveElement_shouldExpandChildrenWhenTocVisibleIsSet() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        // root at index 0, child at index 1 — expandTree(0) will make level-1 children visible
        TOCElement root = new TOCElement(null, "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        root.setHasChild(true);
        TOCElement child = new TOCElement(null, "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        child.setVisible(false);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, child));
        toc.setTocElementMap(map);

        toc.setChildVisible(0); // triggers expandTree(0) on next getActiveElement call
        TOCElement active = toc.getActiveElement();

        assertSame(root, active); // getActiveElement returns the clicked element
        assertTrue(root.isExpanded()); // getActiveElement sets expanded=true on the element
        assertTrue(child.isVisible()); // expandTree made the child visible
        assertEquals(-1, toc.getTocVisible()); // flag is reset after processing
    }

    /**
     * @see TOC#getActiveElement()
     * @verifies collapse all descendants when tocInvisible is set
     */
    @Test
    void getActiveElement_shouldCollapseChildrenWhenTocInvisibleIsSet() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        // root at index 0, child at index 1 — collapseTree(0) will hide deeper elements
        TOCElement root = new TOCElement(null, "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        root.setExpanded(true);
        TOCElement child = new TOCElement(null, "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        child.setVisible(true);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, child));
        toc.setTocElementMap(map);

        toc.setChildInvisible(0); // triggers collapseTree(0) on next getActiveElement call
        TOCElement active = toc.getActiveElement();

        assertSame(root, active); // getActiveElement returns the clicked element
        assertFalse(root.isExpanded()); // getActiveElement sets expanded=false on the element
        assertFalse(child.isVisible()); // collapseTree hid all descendants
        assertEquals(-1, toc.getTocInvisible()); // flag is reset after processing
    }

    /**
     * @see TOC#buildTree(String, int, int, int, String)
     * @verifies assign correct parent ids to elements
     */
    @Test
    void buildTree_shouldAssignCorrectParentIds() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        // Linear chain: root → child → grandchild
        TOCElement root = new TOCElement(null, "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        TOCElement child = new TOCElement(null, "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        TOCElement grandchild = new TOCElement(null, "1", null, "3", "LOG_0003", 2, null, null, false, false, false, null, null, null);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, child, grandchild));
        toc.setTocElementMap(map);

        toc.buildTree(StringConstants.DEFAULT_NAME, 1, 10, 0, null);

        // IDs are assigned based on list position
        assertEquals(0, root.getID());
        assertEquals(1, child.getID());
        assertEquals(2, grandchild.getID());
        // parentIds reflect the tree hierarchy
        assertEquals(-1, root.getParentId()); // root has no parent (initial value)
        assertEquals(0, child.getParentId()); // child's parent is root at index 0
        assertEquals(1, grandchild.getParentId()); // grandchild's parent is child at index 1
    }

    /**
     * @see TOC#buildTree(String, int, int, int, String)
     * @verifies not collapse elements below lowestLevelToCollapse even when threshold exceeded
     */
    @Test
    void buildTree_shouldNotCollapseElementsBelowLowestLevelToCollapse() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        // 3 children at level 1 exceed threshold of 2 — but lowestLevelToCollapse=2
        // protects level-1 elements (1 < 2) from being collapsed by length
        TOCElement root = new TOCElement(null, "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        TOCElement c1 = new TOCElement(null, "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        TOCElement c2 = new TOCElement(null, "1", null, "3", "LOG_0003", 1, null, null, false, false, false, null, null, null);
        TOCElement c3 = new TOCElement(null, "1", null, "4", "LOG_0004", 1, null, null, false, false, false, null, null, null);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, c1, c2, c3));
        toc.setTocElementMap(map);

        toc.buildTree(StringConstants.DEFAULT_NAME, 1, 2, 2, null);

        assertTrue(root.isExpanded()); // root stays expanded — no length-based collapse occurred
        assertTrue(c1.isVisible());
        assertTrue(c2.isVisible());
        assertTrue(c3.isVisible());
    }

    /**
     * @see TOC#buildTree(String, int, int, int, String)
     * @verifies not collapse any elements when threshold is zero or negative
     */
    @Test
    void buildTree_shouldNotCollapseWhenThresholdIsZero() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        // 6 children would normally exceed any threshold, but threshold=0 disables collapsing
        TOCElement root = new TOCElement(null, "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        TOCElement c1 = new TOCElement(null, "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        TOCElement c2 = new TOCElement(null, "1", null, "3", "LOG_0003", 1, null, null, false, false, false, null, null, null);
        TOCElement c3 = new TOCElement(null, "1", null, "4", "LOG_0004", 1, null, null, false, false, false, null, null, null);
        TOCElement c4 = new TOCElement(null, "1", null, "5", "LOG_0005", 1, null, null, false, false, false, null, null, null);
        TOCElement c5 = new TOCElement(null, "1", null, "6", "LOG_0006", 1, null, null, false, false, false, null, null, null);
        TOCElement c6 = new TOCElement(null, "1", null, "7", "LOG_0007", 1, null, null, false, false, false, null, null, null);
        map.put(StringConstants.DEFAULT_NAME, Arrays.asList(root, c1, c2, c3, c4, c5, c6));
        toc.setTocElementMap(map);

        toc.buildTree(StringConstants.DEFAULT_NAME, 1, 0, 0, null); // collapseThreshold=0 → no length-based collapse

        assertTrue(root.isExpanded());
        assertTrue(c1.isVisible());
        assertTrue(c2.isVisible());
        assertTrue(c3.isVisible());
        assertTrue(c4.isVisible());
        assertTrue(c5.isVisible());
        assertTrue(c6.isVisible());
    }

    /**
     * @see TOC#buildTree(String, int, int, int, String)
     * @verifies expand ancestors of target element
     */
    @Test
    void buildTree_shouldExpandAncestorsOfTargetElement() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> tocElementMap = new HashMap<>();
        TOCElement top = new TOCElement(null, "1", null, "1", "LOG_0001", 0, null, null, false, false, false, null, null, null);
        TOCElement child = new TOCElement(null, "1", null, "2", "LOG_0002", 1, null, null, false, false, false, null, null, null);
        TOCElement grandchild = new TOCElement(null, "1", null, "3", "LOG_0003", 2, null, null, false, false, false, null, null, null);
        TOCElement otherChild = new TOCElement(null, "2", null, "4", "LOG_0004", 1, null, null, false, false, false, null, null, null);
        TOCElement otherGrandchild = new TOCElement(null, "2", null, "5", "LOG_0005", 2, null, null, false, false, false, null, null, null);

        List<TOCElement> list = Arrays.asList(top, child, grandchild, otherChild, otherGrandchild);
        tocElementMap.put(StringConstants.DEFAULT_NAME, list);

        toc.setTocElementMap(tocElementMap);

        toc.buildTree(StringConstants.DEFAULT_NAME, 1, 5, 0, null);
        assertTrue(top.isVisible());
        assertTrue(top.isExpanded());
        assertTrue(child.isVisible());
        assertFalse(child.isExpanded());
        assertFalse(grandchild.isVisible());
        assertTrue(otherChild.isVisible());
        assertFalse(otherChild.isExpanded());
        assertFalse(otherGrandchild.isVisible());

        toc.buildTree(StringConstants.DEFAULT_NAME, 1, 5, 0, grandchild.getIddoc());
        assertTrue(top.isVisible());
        assertTrue(top.isExpanded());
        assertTrue(child.isVisible());
        assertTrue(child.isExpanded());
        assertTrue(grandchild.isVisible());
        assertFalse(grandchild.isExpanded());
        assertTrue(otherChild.isVisible());
        assertFalse(otherChild.isExpanded());
        assertFalse(otherGrandchild.isVisible());
    }

    /**
     * @verifies not throw NPE when group not in map
     * @see TOC#buildTree(String, int, int, int, String)
     */
    @Test
    void buildTree_shouldNotThrowNPEWhenGroupNotInMap() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        map.put(StringConstants.DEFAULT_NAME, new ArrayList<>());
        toc.setTocElementMap(map);

        // "unknownGroup" is not a key in the map — must not throw NPE
        Assertions.assertDoesNotThrow(() -> toc.buildTree("unknownGroup", 1, 5, 0, null));
        // Result should be null since the group doesn't exist
        Assertions.assertNull(toc.getViewForGroup("unknownGroup"));
    }

    /**
     * Test that buildTree does not throw an NPE when the requested group name
     * is not present in tocElementMap.
     *
     * @see TOC#buildTree(String, int, int, int, String)
     * @verifies not throw NPE when group not in tocElementMap
     */
    @Test
    void buildTree_shouldNotThrowNPEWhenGroupNotInTocElementMap() {
        TOC toc = new TOC();
        Map<String, List<TOCElement>> map = new HashMap<>();
        // Only add the default group — the "missingGroup" key is intentionally absent
        map.put(StringConstants.DEFAULT_NAME, new ArrayList<>());
        toc.setTocElementMap(map);

        // Calling buildTree with a group that does not exist in the map must not throw NPE
        Assertions.assertDoesNotThrow(() -> toc.buildTree("missingGroup", 1, 5, 0, null));
    }

    /**
     * @verifies not throw NPE when ViewManager is null
     */
    @Test
    void getTreeViewForGroup_shouldNotThrowNPEWhenViewManagerIsNull() {
        // Regression test: BeanUtils.getActiveDocumentBean() may return null (or its
        // ViewManager may be null) in concurrent scenarios; must not throw NPE.
        TOC toc = new TOC();
        Map<String, List<TOCElement>> tocElementMap = new HashMap<>();
        TOCElement element = new TOCElement(new SimpleMetadataValue("root"), "1", null, "1", "LOG_0001", 0, "PI", null, false, true, false, null,
                "monograph", null);
        tocElementMap.put(StringConstants.DEFAULT_NAME, List.of(element));
        toc.setTocElementMap(tocElementMap);

        // In test context BeanUtils.getActiveDocumentBean() returns null — this must
        // not cause a NullPointerException after the fix.
        Assertions.assertDoesNotThrow(() -> toc.getTreeViewForGroup(StringConstants.DEFAULT_NAME));
    }
}
