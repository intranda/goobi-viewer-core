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
package de.intranda.digiverso.presentation.model.search;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrConstants;

public class FacetItemTest {

    @Before
    public void setUp() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see FacetItem#FacetItem(String)
     * @verifies split field and value correctly
     */
    @Test
    public void FacetItem_shouldSplitFieldAndValueCorrectly() throws Exception {
        FacetItem item = new FacetItem("FIELD:value:1:2:3", false);
        Assert.assertEquals("FIELD", item.getField());
        Assert.assertEquals("value:1:2:3", item.getValue());
    }

    /**
     * @see FacetItem#FacetItem(String,boolean)
     * @verifies split field and value range correctly
     */
    @Test
    public void FacetItem_shouldSplitFieldAndValueRangeCorrectly() throws Exception {
        FacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assert.assertEquals("FIELD", item.getField());
        Assert.assertEquals("foo", item.getValue());
        Assert.assertEquals("bar", item.getValue2());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies construct link correctly
     */
    @Test
    public void getQueryEscapedLink_shouldConstructLinkCorrectly() throws Exception {
        FacetItem item = new FacetItem("FIELD:value", false);
        Assert.assertEquals("FIELD:value", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies escape values containing whitespaces
     */
    @Test
    public void getQueryEscapedLink_shouldEscapeValuesContainingWhitespaces() throws Exception {
        FacetItem item = new FacetItem("FIELD:foo bar", false);
        Assert.assertEquals("FIELD:\"foo\\ bar\"", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies construct hierarchical link correctly
     */
    @Test
    public void getQueryEscapedLink_shouldConstructHierarchicalLinkCorrectly() throws Exception {
        FacetItem item = new FacetItem("FIELD:value", true);
        Assert.assertEquals("(FIELD:value OR FIELD:value.*)", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#getQueryEscapedLink()
     * @verifies construct range link correctly
     */
    @Test
    public void getQueryEscapedLink_shouldConstructRangeLinkCorrectly() throws Exception {
        FacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assert.assertEquals("FIELD:[foo TO bar]", item.getQueryEscapedLink());
    }

    /**
     * @see FacetItem#generateFacetItems(String,Map,boolean,boolean,boolean)
     * @verifies sort items correctly
     */
    @Test
    public void generateFacetItems_shouldSortItemsCorrectly() throws Exception {
        Map<String, Long> values = new TreeMap<>();
        values.put("Monograph", 1L);
        values.put("Article", 5L);
        values.put("Volume", 3L);
        {
            // asc
            List<FacetItem> items = FacetItem.generateFacetItems(SolrConstants.DOCSTRCT, values, true, false, false, null);
            Assert.assertEquals(3, items.size());
            Assert.assertEquals("Article", items.get(0).getLabel());
            Assert.assertEquals("Monograph", items.get(1).getLabel());
            Assert.assertEquals("Volume", items.get(2).getLabel());
        }
        {
            // desc
            List<FacetItem> items = FacetItem.generateFacetItems(SolrConstants.DOCSTRCT, values, true, true, false, null);
            Assert.assertEquals(3, items.size());
            Assert.assertEquals("Article", items.get(2).getLabel());
            Assert.assertEquals("Monograph", items.get(1).getLabel());
            Assert.assertEquals("Volume", items.get(0).getLabel());
        }
    }

    /**
     * @see FacetItem#getFullValue()
     * @verifies build full value correctly
     */
    @Test
    public void getFullValue_shouldBuildFullValueCorrectly() throws Exception {
        FacetItem item = new FacetItem("FIELD:[foo TO bar]", false);
        Assert.assertEquals("foo", item.getValue());
        Assert.assertEquals("bar", item.getValue2());
        Assert.assertEquals("foo - bar", item.getFullValue());
    }
}