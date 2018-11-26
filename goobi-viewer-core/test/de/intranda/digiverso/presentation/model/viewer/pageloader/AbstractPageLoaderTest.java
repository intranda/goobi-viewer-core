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
package de.intranda.digiverso.presentation.model.viewer.pageloader;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.model.viewer.StructElement;

public class AbstractPageLoaderTest {
    
    /**
     * @see AbstractPageLoader#buildPageLabelTemplate(String, Locale)
     * @verifies replace numpages currectly
     */
    @Test
    public void buildPageLabelTemplate_shouldReplaceNumpagesCurrectly() throws Exception {
        StructElement se = new StructElement();
        EagerPageLoader loader = new EagerPageLoader(se);
        Assert.assertEquals("foo 0 bar", loader.buildPageLabelTemplate("foo {numpages} bar", null));
    }

    /**
     * @see AbstractPageLoader#buildPageLabelTemplate(String, Locale)
     * @verifies replace message keys correctly
     */
    @Test
    public void buildPageLabelTemplate_shouldReplaceMessageKeysCorrectly() throws Exception {
        StructElement se = new StructElement();
        EagerPageLoader loader = new EagerPageLoader(se);
        Assert.assertEquals("1 of 10", loader.buildPageLabelTemplate("1 {msg.of} 10", null));
    }
}