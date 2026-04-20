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
package io.goobi.viewer.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.model.cms.pages.CMSPage;

class AdvancedSearchOriginTest {

    // --- Record origin (PI constructor) ---

    /**
     * @verifies return pi label and docstrct passed to constructor
     * @see AdvancedSearchOrigin#AdvancedSearchOrigin(String, String, String)
     */
    @Test
    void piConstructor_shouldSetPiLabelAndDocstrct() {
        AdvancedSearchOrigin origin = new AdvancedSearchOrigin("PI_001", "Test Record", "Monograph");

        assertEquals("PI_001", origin.getPi());
        assertEquals("Test Record", origin.getLabel());
        assertEquals("Monograph", origin.getDocstrct());
        assertNull(origin.getCmsPageId());
    }

    /**
     * @verifies return true when pi is not null
     * @see AdvancedSearchOrigin#isRecordOrigin()
     */
    @Test
    void isRecordOrigin_shouldReturnTrueWhenPiIsSet() {
        AdvancedSearchOrigin origin = new AdvancedSearchOrigin("PI_001", "Test Record", "Monograph");

        assertTrue(origin.isRecordOrigin());
    }

    /**
     * @verifies return false when pi is not null
     * @see AdvancedSearchOrigin#isCmsPageOrigin()
     */
    @Test
    void isCmsPageOrigin_shouldReturnFalseWhenPiIsSet() {
        AdvancedSearchOrigin origin = new AdvancedSearchOrigin("PI_001", "Test Record", "Monograph");

        assertFalse(origin.isCmsPageOrigin());
    }

    // --- CMS page origin ---

    /**
     * @verifies return cms page id and title from given page
     * @see AdvancedSearchOrigin#AdvancedSearchOrigin(CMSPage)
     */
    @Test
    void cmsPageConstructor_shouldSetCmsPageIdAndLabel() {
        CMSPage page = new CMSPage();
        page.setId(42L);
        page.getTitleTranslations().setValue("CMS Title", Locale.ENGLISH);

        AdvancedSearchOrigin origin = new AdvancedSearchOrigin(page);

        assertEquals(42L, origin.getCmsPageId());
        assertEquals("CMS Title", origin.getLabel());
        assertNull(origin.getPi());
        assertNull(origin.getDocstrct());
    }

    /**
     * @verifies return false when cms page id is set
     * @see AdvancedSearchOrigin#isRecordOrigin()
     */
    @Test
    void isRecordOrigin_shouldReturnFalseWhenCmsPageIdIsSet() {
        CMSPage page = new CMSPage();
        page.setId(42L);

        AdvancedSearchOrigin origin = new AdvancedSearchOrigin(page);

        assertFalse(origin.isRecordOrigin());
    }

    /**
     * @verifies return true when cms page id is set
     * @see AdvancedSearchOrigin#isCmsPageOrigin()
     */
    @Test
    void isCmsPageOrigin_shouldReturnTrueWhenCmsPageIdIsSet() {
        CMSPage page = new CMSPage();
        page.setId(42L);

        AdvancedSearchOrigin origin = new AdvancedSearchOrigin(page);

        assertTrue(origin.isCmsPageOrigin());
    }

    // --- getOriginUrl ---

    /**
     * @verifies return toc url for record origin
     * @see AdvancedSearchOrigin#getOriginUrl()
     */
    @Test
    void getOriginUrl_shouldReturnTocUrlForRecordOrigin() {
        AdvancedSearchOrigin origin = new AdvancedSearchOrigin("PI_001", "Test Record", "Monograph");

        try (MockedStatic<PrettyUrlTools> mockTools = mockStatic(PrettyUrlTools.class)) {
            mockTools.when(() -> PrettyUrlTools.getAbsolutePageUrl("toc2", "PI_001", 1))
                    .thenReturn("/viewer/toc/PI_001/1/");

            assertEquals("/viewer/toc/PI_001/1/", origin.getOriginUrl());
        }
    }

    /**
     * @verifies return cms page url for cms page origin
     * @see AdvancedSearchOrigin#getOriginUrl()
     */
    @Test
    void getOriginUrl_shouldReturnCmsPageUrlForCmsPageOrigin() {
        CMSPage page = new CMSPage();
        page.setId(42L);
        AdvancedSearchOrigin origin = new AdvancedSearchOrigin(page);

        try (MockedStatic<PrettyUrlTools> mockTools = mockStatic(PrettyUrlTools.class)) {
            mockTools.when(() -> PrettyUrlTools.getAbsolutePageUrl("cmsOpenPage1", 42L))
                    .thenReturn("/viewer/cms/42/");

            assertEquals("/viewer/cms/42/", origin.getOriginUrl());
        }
    }

    /**
     * @verifies throw IllegalStateException when pi is null and no cms page id set
     * @see AdvancedSearchOrigin#getOriginUrl()
     */
    @Test
    void getOriginUrl_shouldThrowIllegalStateExceptionWhenNeitherPiNorCmsPageIdIsSet() {
        AdvancedSearchOrigin origin = new AdvancedSearchOrigin(null, "Test", "Monograph");

        assertThrows(IllegalStateException.class, origin::getOriginUrl);
    }
}
