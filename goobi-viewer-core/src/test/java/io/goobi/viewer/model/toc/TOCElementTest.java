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
     * @see TOCElement#getUrl()
     * @verifies return URL containing page type, PI, page number and logId for fullscreen view
     */
    @Test
    void getUrl_shouldReturnURLContainingPageTypePIPageNumberAndLogIdForFullscreenView() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, false, false, true,
                "image", null, null);
        Assertions.assertEquals('/' + PageType.viewFullscreen.getName() + "/PPN123/1/LOG_0001/", tef.getUrl(PageType.viewFullscreen.getName()));
    }

    /**
     * @see TOCElement#getUrl()
     * @verifies return URL containing page type PI page number and log id for fullscreen view with alternate setup
     */
    @Test
    void getUrl_shouldReturnURLContainingPageTypePIPageNumberAndLogIdForFullscreenViewWithAlternateSetup() throws Exception {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", "first", "123", "LOG_0001", 0, "PPN123", null, false, false, true,
                "image", null, null);
        Assertions.assertEquals('/' + PageType.viewFullscreen.getName() + "/PPN123/1/LOG_0001/", tef.getUrl(PageType.viewFullscreen.getName()));
    }

    /**
     * Test that getUrl(String) returns a correctly constructed fullscreen URL
     * when called with the viewFullscreen page type on an image element.
     *
     * @see TOCElement#getUrl(String)
     * @verifies construct full screen url correctly
     */
    @Test
    void getUrl_shouldConstructFullScreenUrlCorrectly() throws Exception {
        // Create a non-anchor element with image mime type so pageType becomes viewObject,
        // which enables the fullscreen branch in getUrl(String)
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "5", "five", "456", "LOG_0003", 1, "PPN999", null, false, false, true,
                "image", null, null);
        String url = tef.getUrl(PageType.viewFullscreen.getName());
        // URL must contain the fullscreen page type, the PI, the page number and the logId
        Assertions.assertTrue(url.contains(PageType.viewFullscreen.getName()), "URL should contain fullscreen page type");
        Assertions.assertTrue(url.contains("PPN999"), "URL should contain PI");
        Assertions.assertTrue(url.contains("/5/"), "URL should contain page number");
        Assertions.assertTrue(url.contains("LOG_0003"), "URL should contain logId");
    }

    /**
     * @see TOCElement#getUrl()
     * @verifies return URL containing page type, PI, page number and logId for reading mode view
     */
    @Test
    void getUrl_shouldReturnURLContainingPageTypePIPageNumberAndLogIdForReadingModeView() throws Exception {
        // Create a non-anchor element with image content so that its default URL uses the image/object page type
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "3", "three", "789", "LOG_0002", 1, "PPN456", null, false, false, true,
                "image", null, null);
        String url = tef.getUrl();
        // The no-arg getUrl() returns urlPrefix + urlSuffix built in the constructor; verify all components are present
        Assertions.assertTrue(url.contains("PPN456"), "URL should contain PI");
        Assertions.assertTrue(url.contains("/3/"), "URL should contain page number");
        Assertions.assertTrue(url.contains("LOG_0002"), "URL should contain logId");
    }

    /**
     * @see TOCElement#getUrl(String)
     * @verifies construct reading mode url correctly
     */
    @Test
    void getUrl_shouldConstructReadingModeUrlCorrectly() throws Exception {
        // The viewImage branch in getUrl(String) builds a URL using PageType.viewImage,
        // which represents the standard image reading mode
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "7", "seven", "321", "LOG_0005", 1, "PPN789", null, false, false, true,
                "image", null, null);
        String url = tef.getUrl(PageType.viewImage.getName());
        // Verify the URL contains the viewImage page type name plus all identifiers
        Assertions.assertTrue(url.contains("/" + PageType.viewImage.getName() + "/"), "URL should contain viewImage page type");
        Assertions.assertTrue(url.contains("PPN789"), "URL should contain PI");
        Assertions.assertTrue(url.contains("/7/"), "URL should contain page number");
        Assertions.assertTrue(url.contains("LOG_0005"), "URL should contain logId");
    }

    /**
     * @see TOCElement#equals(Object)
     * @verifies return true for same object reference
     */
    @Test
    void equals_shouldReturnTrueForSameObject() {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        Assertions.assertEquals(tef, tef);
    }

    /**
     * @see TOCElement#equals(Object)
     * @verifies return false for null
     */
    @Test
    void equals_shouldReturnFalseForNull() {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        Assertions.assertNotEquals(null, tef);
    }

    /**
     * @see TOCElement#equals(Object)
     * @verifies return false for different class
     */
    @Test
    void equals_shouldReturnFalseForDifferentClass() {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        Assertions.assertNotEquals("some string", tef);
    }

    /**
     * @see TOCElement#equals(Object)
     * @verifies return true for elements with same logId pageNo and topStructPi
     */
    @Test
    void equals_shouldReturnTrueForSameLogIdPageNoAndTopStructPi() {
        TOCElement tef1 = new TOCElement(new SimpleMetadataValue("Label A"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        TOCElement tef2 = new TOCElement(new SimpleMetadataValue("Label B"), "1", null, "456", "LOG_0001", 3, "PPN123", null, true, true, true, "image", null, null);
        Assertions.assertEquals(tef1, tef2);
    }

    /**
     * @see TOCElement#equals(Object)
     * @verifies return false for different logId
     */
    @Test
    void equals_shouldReturnFalseForDifferentLogId() {
        TOCElement tef1 = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        TOCElement tef2 = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0002", 0, "PPN123", null, false, false, false, null, null, null);
        Assertions.assertNotEquals(tef1, tef2);
    }

    /**
     * @see TOCElement#equals(Object)
     * @verifies return false for different topStructPi
     */
    @Test
    void equals_shouldReturnFalseForDifferentTopStructPi() {
        TOCElement tef1 = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        TOCElement tef2 = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN456", null, false, false, false, null, null, null);
        Assertions.assertNotEquals(tef1, tef2);
    }

    /**
     * @see TOCElement#hashCode()
     * @verifies be equal for equal elements
     */
    @Test
    void hashCode_shouldBeEqualForEqualElements() {
        TOCElement tef1 = new TOCElement(new SimpleMetadataValue("Label A"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        TOCElement tef2 = new TOCElement(new SimpleMetadataValue("Label B"), "1", null, "456", "LOG_0001", 5, "PPN123", null, true, true, true, "image", null, null);
        Assertions.assertEquals(tef1.hashCode(), tef2.hashCode());
    }

    /**
     * @see TOCElement#hashCode()
     * @verifies differ for unequal elements
     */
    @Test
    void hashCode_shouldDifferForUnequalElements() {
        TOCElement tef1 = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        TOCElement tef2 = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0002", 0, "PPN456", null, false, false, false, null, null, null);
        Assertions.assertNotEquals(tef1.hashCode(), tef2.hashCode());
    }

    /**
     * @see TOCElement#hashCode()
     * @verifies differ for elements with same logId but different topStructPi
     *
     * Safety net for replacing ArrayList.contains() with HashSet in addTocElementsRecusively:
     * a HashSet<TOCElement> must not conflate elements that share logId but belong to different
     * top-level records, so hashCode() must differ when topStructPi differs.
     */
    @Test
    void hashCode_shouldDifferForElementsWithSameLogIdButDifferentTopStructPi() {
        TOCElement tef1 = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        TOCElement tef2 = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "456", "LOG_0001", 0, "PPN456", null, false, false, false, null, null, null);
        // Elements are not equal (different topStructPi)
        Assertions.assertNotEquals(tef1, tef2);
        // Therefore their hashCodes must differ so they occupy separate buckets in a HashSet
        Assertions.assertNotEquals(tef1.hashCode(), tef2.hashCode());
    }

    /**
     * @see TOCElement#isEmpty()
     * @verifies return true for blank label
     */
    @Test
    void isEmpty_shouldReturnTrueForBlankLabel() {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("   "), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        Assertions.assertTrue(tef.isEmpty());
    }

    /**
     * @see TOCElement#isEmpty()
     * @verifies return false for non-blank label
     */
    @Test
    void isEmpty_shouldReturnFalseForNonBlankLabel() {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Some Title"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        Assertions.assertFalse(tef.isEmpty());
    }

    /**
     * @see TOCElement#isAccessPermissionPdf()
     * @verifies return true when set in constructor
     */
    @Test
    void isAccessPermissionPdf_shouldReturnTrueWhenSetInConstructor() {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, true, false, false, null, null, null);
        Assertions.assertTrue(tef.isAccessPermissionPdf());
    }

    /**
     * @see TOCElement#isAccessPermissionPdf()
     * @verifies return false when not set in constructor
     */
    @Test
    void isAccessPermissionPdf_shouldReturnFalseWhenNotSetInConstructor() {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        Assertions.assertFalse(tef.isAccessPermissionPdf());
    }

    /**
     * @see TOCElement#getMetadataValue(String)
     * @verifies return null for unknown key
     */
    @Test
    void getMetadataValue_shouldReturnNullForUnknownKey() {
        TOCElement tef = new TOCElement(new SimpleMetadataValue("Label"), "1", null, "123", "LOG_0001", 0, "PPN123", null, false, false, false, null, null, null);
        Assertions.assertNull(tef.getMetadataValue("UNKNOWN_FIELD"));
    }
}
