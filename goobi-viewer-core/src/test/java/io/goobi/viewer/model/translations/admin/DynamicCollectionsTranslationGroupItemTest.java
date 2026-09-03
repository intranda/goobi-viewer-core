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

import java.util.List;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.cms.collections.DynamicCollection;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;
import io.goobi.viewer.solr.SolrConstants;

class DynamicCollectionsTranslationGroupItemTest extends AbstractDatabaseEnabledTest {

    /**
     * @verifies load one entry per dynamic collection with the database labels as values
     */
    @Test
    void loadEntries_shouldLoadDatabaseLabels() throws Exception {
        DynamicCollection collection = new DynamicCollection("dyncol_group_test");
        collection.setSolrQuery("*:*");
        // Explicit language list: populateLabels() without arguments needs a NavigationHelper, which does not exist in unit tests
        collection.populateLabels(List.of("de", "en"));
        collection.getLabelAsTranslation("de").setTranslationValue("Testsammlung");
        collection.pruneEmptyTranslations();
        DataManager.getInstance().getDao().addDynamicCollection(collection);
        try {
            TranslationGroupItem item = TranslationGroupItem.create(TranslationGroupType.DYNAMIC_COLLECTIONS, SolrConstants.DC_DYNAMIC, false);
            MessageEntry entry = item.getEntries().stream().filter(e -> "dyncol_group_test".equals(e.getKey())).findFirst().orElse(null);
            Assertions.assertNotNull(entry);
            MessageValue de = entry.getValues().stream().filter(v -> "de".equals(v.getLanguage())).findFirst().orElse(null);
            Assertions.assertNotNull(de);
            Assertions.assertEquals("Testsammlung", de.getValue());
        } finally {
            DataManager.getInstance().getDao().deleteDynamicCollection(collection);
        }
    }

    /**
     * @verifies persist edited label values to the database instead of the messages files
     */
    @Test
    void saveSelectedEntry_shouldPersistLabelsToDatabase() throws Exception {
        DynamicCollection collection = new DynamicCollection("dyncol_group_save_test");
        collection.setSolrQuery("*:*");
        DataManager.getInstance().getDao().addDynamicCollection(collection);
        try {
            TranslationGroup group = TranslationGroup.create(99, TranslationGroupType.DYNAMIC_COLLECTIONS, "admin__dynamic_collections", null, 1);
            group.getItems().add(TranslationGroupItem.create(TranslationGroupType.DYNAMIC_COLLECTIONS, SolrConstants.DC_DYNAMIC, false));
            Assertions.assertTrue(group.findEntryByMessageKey("dyncol_group_save_test"));
            MessageEntry entry = group.getSelectedEntry();
            entry.getValues().stream().filter(v -> "en".equals(v.getLanguage())).findFirst().orElseThrow().setValue("Test collection");
            group.saveSelectedEntry();

            DynamicCollection persisted = DataManager.getInstance().getDao().getDynamicCollection("dyncol_group_save_test");
            Assertions.assertNotNull(persisted);
            Assertions.assertNotNull(persisted.getLabelAsTranslation("en"), "Saving must create the label row for a language that had none");
            Assertions.assertEquals("Test collection", persisted.getLabelAsTranslation("en").getTranslationValue());
        } finally {
            DataManager.getInstance()
                    .getDao()
                    .deleteDynamicCollection(DataManager.getInstance().getDao().getDynamicCollection("dyncol_group_save_test"));
        }
    }
}
