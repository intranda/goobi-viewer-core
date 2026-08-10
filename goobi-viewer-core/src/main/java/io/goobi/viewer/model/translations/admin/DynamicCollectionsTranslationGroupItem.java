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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.collections.DynamicCollection;
import io.goobi.viewer.model.cms.collections.DynamicCollectionTranslation;

/**
 * A {@link TranslationGroupItem} backed by the database-defined dynamic collections: one entry per collection, keyed by the collection identifier,
 * with the collection's labels as values. Edited values are persisted to the database via {@link TranslationGroup#saveSelectedEntry()}, not to the
 * messages files.
 */
public class DynamicCollectionsTranslationGroupItem extends TranslationGroupItem {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(DynamicCollectionsTranslationGroupItem.class);

    /**
     * Protected constructor.
     *
     * @param key group key; only used for display purposes, the entries are always all dynamic collections
     * @param regex unused for this item type
     */
    protected DynamicCollectionsTranslationGroupItem(String key, boolean regex) {
        super(key, regex);
    }

    /**
     * @see io.goobi.viewer.model.translations.admin.TranslationGroupItem#loadEntries()
     * @should load one entry per dynamic collection with the database labels as values
     */
    @Override
    protected void loadEntries() {
        List<DynamicCollection> collections;
        try {
            collections = DataManager.getInstance().getDao().getAllDynamicCollections();
        } catch (DAOException e) {
            logger.error("Error loading dynamic collections: {}", e.getMessage());
            entries = Collections.emptyList();
            return;
        }

        List<Locale> allLocales = ViewerResourceBundle.getAllLocales();
        List<MessageEntry> ret = new ArrayList<>(collections.size());
        for (DynamicCollection collection : collections) {
            collection.populateLabels();
            List<MessageValue> values = new ArrayList<>(allLocales.size());
            for (Locale locale : allLocales) {
                DynamicCollectionTranslation label = collection.getLabelAsTranslation(locale.getLanguage());
                values.add(new MessageValue(locale.getLanguage(), label != null ? label.getTranslationValue() : "", null));
            }
            ret.add(new MessageEntry("", collection.getIdentifier(), values));
        }
        ret.sort(Comparator.comparing(MessageEntry::getKey));
        entries = ret;
    }
}
