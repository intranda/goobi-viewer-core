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
package io.goobi.viewer.model.administration.legal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;

class TermsOfUseTest extends AbstractTest {

    /**
     * @see TermsOfUse#TermsOfUse(TermsOfUse)
     * @verifies clone original correctly
     */
    @Test
    void TermsOfUse_shouldProduceFeedCorrectly() {
        TermsOfUse orig = new TermsOfUse();
        orig.id = 123L;
        orig.setActive(true);
        orig.setTitle("en", "title");
        orig.setDescription("en", "desc");

        TermsOfUse copy = new TermsOfUse(orig);
        Assertions.assertEquals(123L, copy.getId());
        Assertions.assertTrue(copy.isActive());
        Assertions.assertEquals("title", copy.getTitleIfExists("en").get());
        Assertions.assertEquals("desc", copy.getDescriptionIfExists("en").get());
    }

    /**
     * @see TermsOfUse#getForLanguage(Stream<TermsOfUseTranslation>,String)
     * @verifies throw IllegalArgumentException if language blank
     */
    @Test
    void getForLanguage_shouldThrowIllegalArgumentExceptionIfLanguageBlank() {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> TermsOfUse.getForLanguage(null, " "));
        assertEquals("Must provide non-empty language parameter to filter translations for language", e.getMessage());
    }

    /**
     * @see TermsOfUse#TermsOfUse(TermsOfUse)
     * @verifies clear the list
     */
    @Test
    void cleanTranslations_shouldClearTheList() {
        TermsOfUse orig = new TermsOfUse();
        orig.id = 123L;
        orig.setActive(true);
        orig.setTitle("en", "title");
        orig.setTitle("de", " ");
        orig.setDescription("en", "desc");
        orig.setDescription("de", "");
        Assertions.assertEquals(4, orig.getTranslations().size());

        orig.cleanTranslations();
        Assertions.assertEquals(2, orig.getTranslations().size());
    }
}
