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
package io.goobi.viewer.model.translations.admin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;

class TranslationGroupItemTest {

    /**
     * @see TranslationGroupItem#create(TranslationGroupType, String, boolean)
     * @verifies create correct class instance by type
     */
    @Test
    void create_shouldCreateCorrectClassInstanceByType() throws Exception {
        // Verify that the factory method returns the correct subclass for each TranslationGroupType
        Assertions.assertInstanceOf(SolrFieldNameTranslationGroupItem.class,
                TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_NAMES, "key", false));
        Assertions.assertInstanceOf(SolrFieldValueTranslationGroupItem.class,
                TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_VALUES, "key", false));
        Assertions.assertInstanceOf(LocalMessagesTranslationGroupItem.class,
                TranslationGroupItem.create(TranslationGroupType.LOCAL_STRINGS, "key", false));
        Assertions.assertInstanceOf(CoreMessagesTranslationGroupItem.class,
                TranslationGroupItem.create(TranslationGroupType.CORE_STRINGS, "key", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> TranslationGroupItem.create(null, "key", false));
    }
}
