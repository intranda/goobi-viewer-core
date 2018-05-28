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
package de.intranda.digiverso.presentation.model.urlresolution;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florian Alpers
 *
 */
public class ViewerPathBuilderTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testStartsWith() {
        String url1 = "a";
        String url2 = "a/b";
        String url3 = "a/b/c";
        String url4 = "f";
        String url5 = "b/a";
        String url6 = "a/bc";
        
        URI uri = URI.create("a/b/cdef");
        
        Assert.assertTrue(ViewerPathBuilder.startsWith(uri, url1));
        Assert.assertTrue(ViewerPathBuilder.startsWith(uri, url2));
        Assert.assertFalse(ViewerPathBuilder.startsWith(uri, url3));
        Assert.assertFalse(ViewerPathBuilder.startsWith(uri, url4));
        Assert.assertFalse(ViewerPathBuilder.startsWith(uri, url5));
        Assert.assertFalse(ViewerPathBuilder.startsWith(uri, url6));
        
    }

}
