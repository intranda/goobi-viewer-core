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
package de.intranda.digiverso.presentation.model.toc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.model.metadata.multilanguage.SimpleMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.PageType;

public class TOCElementTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

    /**
     * @see TOCElement#TOCElement(String,String,String,String,String,int,String,String,boolean,boolean,String,String)
     * @verifies add logId to url
     */
    @Test
    public void TOCElement_shouldAddLogIdToUrl() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, true, false, true,
                "image", null, null);
        Assert.assertEquals("LOG_0001", tef.getLogId());
        Assert.assertTrue(tef.getUrl().endsWith("/LOG_0001/"));
        Assert.assertTrue(tef.getFullscreenUrl().endsWith("/LOG_0001/"));
    }

    /**
     * @see TOCElement#TOCElement(String,String,String,String,String,int,String,String,boolean,boolean,String,String)
     * @verifies set correct view url for given docStructType
     */
    @Test
    public void TOCElement_shouldSetCorrectViewUrlForGivenDocStructType() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, true, false, true,
                "image", "Catalogue", null);
        Assert.assertEquals("LOG_0001", tef.getLogId());
        Assert.assertTrue(tef.getUrl().contains("/" + PageType.viewToc.getName() + "/"));

    }

    /**
     * @see TOCElement#getUrl(String)
     * @verifies construct full screen url correctly
     */
    @Test
    public void getUrl_shouldConstructFullScreenUrlCorrectly() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, false, false, true,
                "image", null, null);
        Assert.assertEquals('/' + PageType.viewReadingMode.getName() + "/PPN123/1/LOG_0001/", tef.getUrl(PageType.viewReadingMode.getName()));
    }

    /**
     * @see TOCElement#getUrl(String)
     * @verifies construct reading mode url correctly
     */
    @Test
    public void getUrl_shouldConstructReadingModeUrlCorrectly() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, false, false, true,
                "image", null, null);
        Assert.assertEquals('/' + PageType.viewFullscreen.getName() + "/PPN123/1/LOG_0001/", tef.getUrl(PageType.viewFullscreen.getName()));
    }
}
