/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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

import org.junit.Assert;
import org.junit.Test;

public class FacetItemTest {

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
}