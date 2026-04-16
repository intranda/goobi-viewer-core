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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.model.viewer.PageType;

class TOCElementTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see TOCElement#TOCElement(String,String,String,String,String,int,String,String,boolean,boolean,String,String)
     * @verifies add log id to url
     */
    @Test
    void TOCElement_shouldAddLogIdToUrl() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, true, false, true,
                "image", null, null);
        Assertions.assertEquals("LOG_0001", tef.getLogId());
        Assertions.assertTrue(tef.getUrl().endsWith("/LOG_0001/"));
        Assertions.assertTrue(tef.getFullscreenUrl().endsWith("/LOG_0001/"));
    }

    /**
     * @see TOCElement#TOCElement(String,String,String,String,String,int,String,String,boolean,boolean,String,String)
     * @verifies set correct view url for given doc struct type
     */
    @Test
    void TOCElement_shouldSetCorrectViewUrlForGivenDocStructType() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, true, false, true,
                "image", "Catalogue", null);
        Assertions.assertEquals("LOG_0001", tef.getLogId());
        Assertions.assertTrue(tef.getUrl().contains("/" + PageType.viewToc.getName() + "/"));

    }

    /**
     * @verifies return URL containing page type PI page number and log id for fullscreen view
     */
    @Test
    void getUrl_shouldReturnURLContainingPageTypePIPageNumberAndLogIdForFullscreenView() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, false, false, true,
                "image", null, null);
        Assertions.assertEquals('/' + PageType.viewFullscreen.getName() + "/PPN123/1/LOG_0001/", tef.getUrl(PageType.viewFullscreen.getName()));
    }

    /**
     * @verifies return URL containing page type PI page number and log id for fullscreen view with alternate setup
     */
    @Test
    void getUrl_shouldReturnURLContainingPageTypePIPageNumberAndLogIdForFullscreenViewWithAlternateSetup() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, false, false, true,
                "image", null, null);
        Assertions.assertEquals('/' + PageType.viewFullscreen.getName() + "/PPN123/1/LOG_0001/", tef.getUrl(PageType.viewFullscreen.getName()));
    }
}
