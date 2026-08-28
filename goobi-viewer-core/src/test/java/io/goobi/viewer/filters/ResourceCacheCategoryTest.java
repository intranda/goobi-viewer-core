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
package io.goobi.viewer.filters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResourceCacheCategoryTest {

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies classify asset directories as static
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "/resources/css/dist/viewer.min.css",
            "/resources/javascript/dist/viewerJS.min.js",
            "/resources/images/placeholder.png",
            "/resources/icons/outline/arrow.svg",
            "/resources/fonts/DejaVuSans-webfont.woff2",
            "/resources/themes/reference/css/theme.min.css",
            "/resources/themes/reference/images/logo.png" })
    void classify_shouldClassifyAssetDirectoriesAsStatic(String path) {
        Assertions.assertEquals(ResourceCacheCategory.STATIC, ResourceCacheCategory.classify(path));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies classify view templates below resources as account or dynamic
     */
    @Test
    void classify_shouldClassifyViewTemplatesBelowResourcesAsAccountOrDynamic() {
        Assertions.assertEquals(ResourceCacheCategory.ACCOUNT, ResourceCacheCategory.classify("/resources/admin/views/adminUsers.xhtml"));
        Assertions.assertEquals(ResourceCacheCategory.ACCOUNT,
                ResourceCacheCategory.classify("/resources/crowdsourcing/campaignItemEdit.xhtml"));
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/resources/cms/editCmsPage.xhtml"));
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/resources/includes/metadata.xhtml"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies never classify markup inside the asset whitelist as static
     */
    @Test
    void classify_shouldNeverClassifyMarkupInsideTheAssetWhitelistAsStatic() {
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/resources/css/inline.xhtml"));
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/resources/javascript/widget.html"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies classify jsf resources as skip
     */
    @Test
    void classify_shouldClassifyJsfResourcesAsSkip() {
        Assertions.assertEquals(ResourceCacheCategory.SKIP, ResourceCacheCategory.classify("/jakarta.faces.resource/viewer.css.xhtml"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies classify api paths as api regardless of file extension
     */
    @Test
    void classify_shouldClassifyApiPathsAsApiRegardlessOfFileExtension() {
        Assertions.assertEquals(ResourceCacheCategory.API,
                ResourceCacheCategory.classify("/api/v1/records/PPN123/files/images/0001.jpg/full/max/0/default.jpg"));
        Assertions.assertEquals(ResourceCacheCategory.API, ResourceCacheCategory.classify("/api"));
        Assertions.assertEquals(ResourceCacheCategory.API, ResourceCacheCategory.classify("/rest/something"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies not classify lookalike paths as api
     */
    @Test
    void classify_shouldNotClassifyLookalikePathsAsApi() {
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/apidocs/index.xhtml"));
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/api-status.xhtml"));
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/restaurant/menu"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies classify account bound prefixes as account
     */
    @ParameterizedTest
    @ValueSource(strings = { "/admin/users/", "/user/dashboard/", "/campaigns/1/", "/bookmarks/" })
    void classify_shouldClassifyAccountBoundPrefixesAsAccount(String path) {
        Assertions.assertEquals(ResourceCacheCategory.ACCOUNT, ResourceCacheCategory.classify(path));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies ignore path parameters and tolerate percent encoded characters
     */
    @Test
    void classify_shouldIgnorePathParametersAndToleratePercentEncodedCharacters() {
        Assertions.assertEquals(ResourceCacheCategory.STATIC,
                ResourceCacheCategory.classify("/resources/css/theme.min.css;jsessionid=ABC123"));
        Assertions.assertEquals(ResourceCacheCategory.STATIC, ResourceCacheCategory.classify("/resources/images/a%20b.png"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies treat null and empty path as dynamic
     */
    @Test
    void classify_shouldTreatNullAndEmptyPathAsDynamic() {
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify(null));
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify(""));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies classify ordinary viewer pages as dynamic
     */
    @Test
    void classify_shouldClassifyOrdinaryViewerPagesAsDynamic() {
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/object/PPN123/1/"));
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/search/-/foo/1/-/-/"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies not throw on malformed percent encoded characters
     */
    @Test
    void classify_shouldNotThrowOnMalformedPercentEncodedCharacters() {
        Assertions.assertEquals(ResourceCacheCategory.STATIC, ResourceCacheCategory.classify("/resources/css/bad%.css"));
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/resources/%zz/x.css"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies treat plus signs as literal characters
     */
    @Test
    void classify_shouldTreatPlusSignsAsLiteralCharacters() {
        Assertions.assertEquals(ResourceCacheCategory.STATIC, ResourceCacheCategory.classify("/resources/images/a+b.png"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies never classify paths containing a parent directory segment as static
     */
    @Test
    void classify_shouldNeverClassifyPathsContainingAParentDirectorySegmentAsStatic() {
        Assertions.assertNotEquals(ResourceCacheCategory.STATIC,
                ResourceCacheCategory.classify("/resources/css/../../admin/private.png"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies treat markup extensions case insensitively
     */
    @Test
    void classify_shouldTreatMarkupExtensionsCaseInsensitively() {
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/resources/css/inline.XHTML"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies not classify lookalike paths as account
     */
    @Test
    void classify_shouldNotClassifyLookalikePathsAsAccount() {
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/administration/"));
    }

    /**
     * @see ResourceCacheCategory#classify(String)
     * @verifies classify incomplete theme paths as dynamic
     */
    @Test
    void classify_shouldClassifyIncompleteThemePathsAsDynamic() {
        Assertions.assertEquals(ResourceCacheCategory.DYNAMIC, ResourceCacheCategory.classify("/resources/themes/reference"));
    }
}
