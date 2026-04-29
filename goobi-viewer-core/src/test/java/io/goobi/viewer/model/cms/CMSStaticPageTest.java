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
package io.goobi.viewer.model.cms;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.cms.CMSStaticPage;

class CMSStaticPageTest {

    private CMSStaticPage page = new CMSStaticPage("test");

    @BeforeEach
    public void setUp() {

    }

    /**
     * @verifies return test for given input
     * @see CMSStaticPage#getPageName()
     */
    @Test
    void getPageName_shouldReturnTestForGivenInput() {
        Assertions.assertEquals("test", page.getPageName());
    }

    /**
     * @verifies return false for given input
     * @see CMSStaticPage#isLanguageComplete(Locale)
     */
    @Test
    void isLanguageComplete_shouldReturnFalseForGivenInput() {
        Assertions.assertFalse(page.isLanguageComplete(Locale.GERMANY));
    }

    /**
     * @verifies has cms page
     * @see CMSStaticPage#isHasCmsPage
     */
    @Test
    void isHasCmsPage_shouldHasCmsPage() {
        Assertions.assertFalse(page.isHasCmsPage());
    }

}
