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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class ConfigurationBeanTest extends AbstractTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(ConfigurationBeanTest.class);

    private ConfigurationBean bean;

    @BeforeEach
    public void setUp() throws Exception {
        bean = new ConfigurationBean();
    }

    /**
     * @see ConfigurationBean#isDisplaySearchRssLinks()
     * @verifies return correct value
     */
    @Test
    void isDisplaySearchRssLinks_shouldReturnCorrectValue() throws Exception {
        Assertions.assertFalse(bean.isDisplaySearchRssLinks());
    }

    @Test
    void testPageBrowseConfiguration() throws Exception {
        Assertions.assertTrue(bean.isPagePdfEnabled());
        Assertions.assertFalse(bean.isPageBrowseStep1Visible());
        Assertions.assertTrue(bean.isPageBrowseStep2Visible());
        Assertions.assertTrue(bean.isPageBrowseStep2Visible());
        Assertions.assertEquals(0, bean.getPageBrowseStep1());
        Assertions.assertEquals(5, bean.getPageBrowseStep2());
        Assertions.assertEquals(10, bean.getPageBrowseStep3());
    }

    /**
     * @see ConfigurationBean#isSidebarPageLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarPageLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assertions.assertEquals(false, bean.isSidebarPageLinkVisible());
    }

    /**
     * @see ConfigurationBean#isSidebarCalendarLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarCalendarLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assertions.assertEquals(false, bean.isSidebarCalendarLinkVisible());
    }

    /**
     * @see ConfigurationBean#isSidebarThumbsLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarThumbsLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assertions.assertEquals(false, bean.isSidebarThumbsLinkVisible());
    }

    /**
     * @see ConfigurationBean#isSidebarMetadataLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarMetadataLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assertions.assertEquals(false, bean.isSidebarMetadataLinkVisible());
    }

    /**
     * @see ConfigurationBean#isSidebarFulltextLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarFulltextLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assertions.assertEquals(false, bean.isSidebarFulltextLinkVisible());
    }

    /**
     * @see ConfigurationBean#getSearchResultGroupNames()
     * @verifies return all values
     */
    @Test
    void getSearchResultGroupNames_shouldReturnAllValues() throws Exception {
        Assertions.assertEquals(3, bean.getSearchResultGroupNames().size());
    }

    /**
     * @see ConfigurationBean#isSidebarFulltextLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isFacetFieldTypeBoolean_shouldReturnCorrectValue() throws Exception {
        Assertions.assertTrue(bean.isFacetFieldTypeBoolean("BOOL_HASIMAGES"));
        Assertions.assertFalse(bean.isFacetFieldTypeBoolean("MD_CREATOR"));
    }
}
