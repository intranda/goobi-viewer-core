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
package io.goobi.viewer.managedbeans;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.ConfigurationTest;

public class ConfigurationBeanTest extends AbstractTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(ConfigurationTest.class);

    private ConfigurationBean bean;

    @Before
    public void setUp() throws Exception {
        bean = new ConfigurationBean();
    }

    /**
     * @see ConfigurationBean#isDisplaySearchRssLinks()
     * @verifies return correct value
     */
    @Test
    public void isDisplaySearchRssLinks_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(bean.isDisplaySearchRssLinks());
    }

    @Test
    public void testPageBrowseConfiguration() throws Exception {
        Assert.assertTrue(bean.isPagePdfEnabled());
        Assert.assertFalse(bean.isPageBrowseStep1Visible());
        Assert.assertTrue(bean.isPageBrowseStep2Visible());
        Assert.assertTrue(bean.isPageBrowseStep2Visible());
        Assert.assertEquals(0, bean.getPageBrowseStep1());
        Assert.assertEquals(5, bean.getPageBrowseStep2());
        Assert.assertEquals(10, bean.getPageBrowseStep3());
    }

    /**
     * @see ConfigurationBean#isSidebarPageLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarPageLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, bean.isSidebarPageLinkVisible());
    }

    /**
     * @see ConfigurationBean#isSidebarCalendarLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarCalendarLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, bean.isSidebarCalendarLinkVisible());
    }

    /**
     * @see ConfigurationBean#isSidebarThumbsLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarThumbsLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, bean.isSidebarThumbsLinkVisible());
    }

    /**
     * @see ConfigurationBean#isSidebarMetadataLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarMetadataLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, bean.isSidebarMetadataLinkVisible());
    }

    /**
     * @see ConfigurationBean#isSidebarFulltextLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarFulltextLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, bean.isSidebarFulltextLinkVisible());
    }

    /**
     * @see ConfigurationBean#getSearchResultGroupNames()
     * @verifies return all values
     */
    @Test
    public void getSearchResultGroupNames_shouldReturnAllValues() throws Exception {
        Assert.assertEquals(3, bean.getSearchResultGroupNames().size());
    }
}
