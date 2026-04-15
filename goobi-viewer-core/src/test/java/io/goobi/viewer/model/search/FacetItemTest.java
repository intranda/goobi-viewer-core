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
package io.goobi.viewer.model.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.solr.SolrConstants;

class FacetItemTest extends AbstractTest {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see FacetItem#FacetItem(String)
     * @verifies split field and value correctly
     */
    @Test
    void FacetItem_shouldSplitFieldAndValueCorrectly() {
        IFacetItem item = new FacetItem("FIELD:value:1:2:3", false);
        Assertions.assertEquals("FIELD", item.getField());
        Assertions.assertEquals("value:1:2:3", item.getValue());
    }

    /**
     * @see FacetItem#FacetItem(String,boolean)
     * @verifies split field and value range correctly
     */
    @Test
    void FacetItem_shouldSplitFieldAndValueRangeCorrectly() {
        IFacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assertions.assertEquals("FIELD", item.getField());
        Assertions.assertEquals("foo", item.getValue());
        Assertions.assertEquals("bar", item.getValue2());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies return unchanged f i e l d value link for a non hierarchical facet item
     */
    @Test
    void getQueryEscapedLink_shouldReturnUnchangedFIELDValueLinkForANonHierarchicalFacetItem() {
        IFacetItem item = new FacetItem("FIELD:value", false);
        Assertions.assertEquals("FIELD:value", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies escape values containing whitespaces
     */
    @Test
    void getQueryEscapedLink_shouldEscapeValuesContainingWhitespaces() {
        IFacetItem item = new FacetItem("FIELD:foo bar", false);
        Assertions.assertEquals("FIELD:\"foo\\ bar\"", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies return OR query with wildcard suffix for a hierarchical facet item
     */
    @Test
    void getQueryEscapedLink_shouldReturnORQueryWithWildcardSuffixForAHierarchicalFacetItem() {
        IFacetItem item = new FacetItem("FIELD:value", true);
        Assertions.assertEquals("(FIELD:value OR FIELD:value.*)", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies return unchanged range query link for a range facet item
     */
    @Test
    void getQueryEscapedLink_shouldReturnUnchangedRangeQueryLinkForARangeFacetItem() {
        IFacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assertions.assertEquals("FIELD:[foo TO bar]", item.getQueryEscapedLink());
    }

    //    /**
    //     * @see FacetItem#getQueryEscapedLink()
    //     * @verifies construct polygon link correctly
    //     */
    //    @Test
    //    void getQueryEscapedLink_shouldConstructPolygonLinkCorrectly() {
    //        FacetItem item = new FacetItem("WKT_COORDS:0 0, 0 90, 90 90, 90 0, 0 0", false);
    //        Assertions.assertEquals("WKT_:\"IsWithin(POLYGON((0 0, 0 90, 90 90, 90 0, 0 0))) distErrPct=0\"", item.getQueryEscapedLink());
    //    }

    /**
     * @see FacetItem#generateFacetItems(String,Map,boolean,boolean,boolean)
     * @verifies sort generated facet items alphabetically in ascending and descending order
     */
    @Test
    void generateFacetItems_shouldSortGeneratedFacetItemsAlphabeticallyInAscendingAndDescendingOrder() {
        Map<String, Long> values = new TreeMap<>();
        values.put("Monograph", 1L);
        values.put("Article", 5L);
        values.put("Volume", 3L);
        {
            // asc
            List<IFacetItem> items = FacetItem.generateFacetItems(SolrConstants.DOCSTRCT, values, true, false, false);
            Assertions.assertEquals(3, items.size());
            Assertions.assertEquals("Article", items.get(0).getLabel());
            Assertions.assertEquals("Monograph", items.get(1).getLabel());
            Assertions.assertEquals("Volume", items.get(2).getLabel());
        }
        {
            // desc
            List<IFacetItem> items = FacetItem.generateFacetItems(SolrConstants.DOCSTRCT, values, true, true, false);
            Assertions.assertEquals(3, items.size());
            Assertions.assertEquals("Article", items.get(2).getLabel());
            Assertions.assertEquals("Monograph", items.get(1).getLabel());
            Assertions.assertEquals("Volume", items.get(0).getLabel());
        }
    }

    /**
     * @see FacetItem#getFullValue()
     * @verifies combine value and value2 with dash separator for a range facet
     */
    @Test
    void getFullValue_shouldCombineValueAndValue2WithDashSeparatorForARangeFacet() {
        IFacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assertions.assertEquals("foo", item.getValue());
        Assertions.assertEquals("bar", item.getValue2());
        Assertions.assertEquals("foo - bar", item.getFullValue());
    }

    /**
     * @see FacetItem#getEscapedValue(String)
     * @verifies escape value correctly
     */
    @Test
    void getEscapedValue_shouldEscapeValueCorrectly() {
        Assertions.assertEquals("\\(foo\\)", FacetItem.getEscapedValue("(foo)"));
    }

    /**
     * @see FacetItem#getEscapedValue(String)
     * @verifies add quotation marks if value contains space
     */
    @Test
    void getEscapedValue_shouldAddQuotationMarksIfValueContainsSpace() {
        Assertions.assertEquals("\"foo\\ bar\"", FacetItem.getEscapedValue("foo bar"));
    }

    /**
     * @see FacetItem#getEscapedValue(String)
     * @verifies preserve leading and trailing quotation marks
     */
    @Test
    void getEscapedValue_shouldPreserveLeadingAndTrailingQuotationMarks() {
        Assertions.assertEquals("\"IsWithin\\(foobar\\)\\ disErrPct=0\"", FacetItem.getEscapedValue("\"IsWithin(foobar) disErrPct=0\""));
    }

    /**
     * @see FacetItem#getEscapedValue(String)
     * @verifies preserve wildcard
     */
    @Test
    void getEscapedValue_shouldPreserveWildcard() {
        Assertions.assertEquals("A*", FacetItem.getEscapedValue("A*"));
    }

    /**
     * @see FacetItem#getEscapedValue(String)
     * @verifies not throw for wildcard only value
     */
    @Test
    void getEscapedValue_shouldNotThrowForWildcardOnlyValue() {
        Assertions.assertEquals("*", FacetItem.getEscapedValue("*"));
        Assertions.assertEquals("**", FacetItem.getEscapedValue("**"));
    }

    /**
     * @verifies set label from separate field if configured and found
     */
    @Test
    void generateFilterLinkList_shouldSetLabelFromSeparateFieldIfConfiguredAndFound() {
        Map<String, String> labelMap = new HashMap<>(1);
        List<IFacetItem> facetItems =
                FacetItem.generateFilterLinkList(null, "MD_CREATOR",
                        FacetSorting.getSortingMap(Collections.singletonMap("Groos, Karl", 1L), "alphabetical"), false, -1, labelMap);
        Assertions.assertEquals(1, facetItems.size());
        Assertions.assertEquals("Karl", facetItems.get(0).getLabel());
    }

    /**
     * @verifies prefer existing items
     */
    @Test
    void generateFilterLinkList_shouldPreferExistingItems() {
        // Regular
        FacetItem existing1 = new FacetItem("MD_FOO:bar", false);
        List<IFacetItem> facetItems1 =
                FacetItem.generateFilterLinkList(Collections.singletonList(existing1), "MD_FOO",
                        FacetSorting.getSortingMap(Collections.singletonMap("bar", 1L), "alphabetical"),
                        false, -1, null);
        Assertions.assertEquals(1, facetItems1.size());
        Assertions.assertEquals("MD_FOO:bar", facetItems1.get(0).getLink());

        // With groupToLength=1
        FacetItem existing2 = new FacetItem("MD_FOO:B*", false);
        List<IFacetItem> facetItems2 =
                FacetItem.generateFilterLinkList(Collections.singletonList(existing2), "MD_FOO",
                        FacetSorting.getSortingMap(Collections.singletonMap("bar", 1L), "alphabetical"),
                        false, 1, null);
        Assertions.assertEquals(1, facetItems2.size());
        Assertions.assertEquals("MD_FOO:B*", facetItems2.get(0).getLink());
    }

    /**
     * @verifies group facet values by starting character and sum their counts
     */
    @Test
    void generateFilterLinkList_shouldGroupFacetValuesByStartingCharacterAndSumTheirCounts() {
        Map<String, String> labelMap = new HashMap<>(1);
        FacetSorting.SortingMap<String, Long> valueMap = FacetSorting.getSortingMap("MD_PERSON", "alphabetical", Locale.GERMAN);
        valueMap.put("Cooper, Alice", 1L);
        valueMap.put("Campbell, Wayne", 1L);
        valueMap.put("Algar, Garth", 1L);
        List<IFacetItem> facetItems = FacetItem.generateFilterLinkList(null, "MD_CREATOR", valueMap, false, 1, labelMap);
        Assertions.assertEquals(2, facetItems.size());
        Assertions.assertEquals("A", facetItems.get(0).getLabel());
        Assertions.assertEquals(1L, facetItems.get(0).getCount());
        Assertions.assertEquals("C", facetItems.get(1).getLabel());
        Assertions.assertEquals(2L, facetItems.get(1).getCount());
    }

    /**
     * @see FacetItem#parseLink(String)
     * @verifies set label to value if label empty
     */
    @Test
    void parseLink_shouldSetLabelToValueIfLabelEmpty() {
        FacetItem item = new FacetItem(false);
        Assertions.assertNull(item.getLabel());
        item.setLink("foo:bar");
        item.parseLink();
        Assertions.assertEquals("bar", item.getLabel());
    }

    /**
     * @see FacetItem#parseLink()
     * @verifies removed wildcard from label
     */
    @Test
    void parseLink_shouldRemovedWildcardFromLabel() {
        FacetItem item = new FacetItem(false);
        Assertions.assertNull(item.getLabel());
        item.setLink("foo:b*");
        item.parseLink();
        Assertions.assertEquals("b", item.getLabel());
    }

    /**
     * @verifies group facet values by first character and sum counts including existing items
     */
    @Test
    void generateFilterLinkList_shouldGroupFacetValuesByFirstCharacterAndSumCountsIncludingExistingItems() {
        List<IFacetItem> existingItems = new ArrayList<>(2);
        existingItems.add(new FacetItem("MD_CREATOR:Groos, Karl", false).setCount(1));
        existingItems.add(new FacetItem("MD_CREATOR:Cooper, Alice", false).setCount(1));

        Map<String, String> labelMap = new HashMap<>(1);
        FacetSorting.SortingMap<String, Long> valueMap = FacetSorting.getSortingMap("MD_CREATOR", "alphabetical", Locale.GERMAN);
        valueMap.put("Cooper, Alice", 1L);
        valueMap.put("Campbell, Wayne", 1L);
        valueMap.put("Algar, Garth", 1L);
        List<IFacetItem> facetItems = FacetItem.generateFilterLinkList(existingItems, "MD_CREATOR", valueMap, false, 1, labelMap);
        Assertions.assertEquals(3, facetItems.size());
        Assertions.assertEquals("A", facetItems.get(0).getLabel());
        Assertions.assertEquals(1L, facetItems.get(0).getCount());
        Assertions.assertEquals("C", facetItems.get(1).getLabel());
        Assertions.assertEquals(3L, facetItems.get(1).getCount());
        Assertions.assertEquals("G", facetItems.get(2).getLabel());
        Assertions.assertEquals(1L, facetItems.get(2).getCount());
    }

    /**
     * @verifies merge existing and new items into filter link list
     */
    @Test
    void generateFilterLinkList_shouldMergeExistingAndNewItemsIntoFilterLinkList() {
        List<IFacetItem> existingItems = new ArrayList<>(2);
        existingItems.add(new FacetItem("MD_CREATOR:Groos, Karl", false).setCount(1));
        existingItems.add(new FacetItem("MD_CREATOR:Doe, John", false).setCount(1));

        FacetSorting.SortingMap<String, Long> newValueMap = FacetSorting.getSortingMap("MD_CREATOR", "alphabetical", Locale.GERMAN);
        newValueMap.put("Montana, Tony", 1L);
        newValueMap.put("Groos, Karl", 1L);
        List<IFacetItem> facetItems =
                FacetItem.generateFilterLinkList(existingItems, "MD_CREATOR", newValueMap, false, -1, null);
        Assertions.assertEquals(3, facetItems.size());
        Assertions.assertEquals("Doe, John", facetItems.get(0).getValue());
        Assertions.assertEquals("Groos, Karl", facetItems.get(1).getValue());
        Assertions.assertEquals(2, facetItems.get(1).getCount());
        Assertions.assertEquals("Montana, Tony", facetItems.get(2).getValue());
    }
}
