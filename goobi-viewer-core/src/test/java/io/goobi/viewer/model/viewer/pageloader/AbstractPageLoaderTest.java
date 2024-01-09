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
package io.goobi.viewer.model.viewer.pageloader;

import java.util.Locale;

import javax.faces.model.SelectItem;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.viewer.StructElement;

class AbstractPageLoaderTest extends AbstractTest {

    /**
     * @see AbstractPageLoader#buildPageLabelTemplate(String, Locale)
     * @verifies replace numpages currectly
     */
    @Test
    void buildPageLabelTemplate_shouldReplaceNumpagesCurrectly() throws Exception {
        StructElement se = new StructElement();
        EagerPageLoader loader = new EagerPageLoader(se);
        Assertions.assertEquals("foo 0 bar", loader.buildPageLabelTemplate("foo {numpages} bar", null));
    }

    /**
     * @see AbstractPageLoader#buildPageLabelTemplate(String, Locale)
     * @verifies replace message keys correctly
     */
    @Test
    void buildPageLabelTemplate_shouldReplaceMessageKeysCorrectly() throws Exception {
        StructElement se = new StructElement();
        EagerPageLoader loader = new EagerPageLoader(se);
        Assertions.assertEquals("1 of 10", loader.buildPageLabelTemplate("1 {msg.of} 10", null));
    }

    /**
     * @see AbstractPageLoader#buildPageSelectItem(String,int,String,Integer,String)
     * @verifies construct single page item correctly
     */
    @Test
    void buildPageSelectItem_shouldConstructSinglePageItemCorrectly() throws Exception {
        SelectItem si = AbstractPageLoader.buildPageSelectItem("{order}: {orderlabel}", 1, "one", null, null);
        Assertions.assertNotNull(si);
        Assertions.assertEquals("1: one", si.getLabel());
        Assertions.assertEquals("1", si.getValue());
    }

    /**
     * @see AbstractPageLoader#buildPageSelectItem(String,int,String,Integer,String)
     * @verifies construct double page item correctly
     */
    @Test
    void buildPageSelectItem_shouldConstructDoublePageItemCorrectly() throws Exception {
        SelectItem si = AbstractPageLoader.buildPageSelectItem("{order}: {orderlabel}", 1, "one", 2, "two");
        Assertions.assertNotNull(si);
        Assertions.assertEquals("1-2: one - two", si.getLabel());
        Assertions.assertEquals("1-2", si.getValue());
    }
}
