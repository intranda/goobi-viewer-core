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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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
