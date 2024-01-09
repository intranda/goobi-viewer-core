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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;
import io.goobi.viewer.solr.SolrConstants;

public class SolrFieldValueTranslationGroupItemTest extends AbstractSolrEnabledTest {

    /**
     * @see SolrFieldValueTranslationGroupItem#loadEntries()
     * @verifies load hierarchical entries correctly
     */
    @Test
    public void loadEntries_shouldLoadHierarchicalEntriesCorrectly() throws Exception {
        TranslationGroupItem item = TranslationGroupItem.create(TranslationGroupType.SOLR_FIELD_VALUES, SolrConstants.DC, false);
        item.loadEntries();
        Assertions.assertFalse(item.getEntries().isEmpty());
        Map<String, Integer> keys = new HashMap<>();
        for (MessageEntry entry : item.getEntries()) {
            Integer count = keys.get(entry.getKey());
            if (count == null) {
                keys.put(entry.getKey(), 1);
            } else {
                keys.put(entry.getKey(), count + 1);
            }
        }
        Assertions.assertEquals(Integer.valueOf(1), keys.get("dcimage")); // Parent keys should only be added once
        Assertions.assertEquals(Integer.valueOf(1), keys.get("dcimage.many"));
        Assertions.assertEquals(Integer.valueOf(1), keys.get("dcimage.png"));
    }
}
