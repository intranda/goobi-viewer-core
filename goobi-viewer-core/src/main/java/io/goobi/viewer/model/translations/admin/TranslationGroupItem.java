/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.translations.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;

/**
 * A single <key> element as part of a translation group.
 */
public abstract class TranslationGroupItem {

    /** A single message key or a regular expression for multiple message keys. */
    protected final String key;
    /** If true, <code>key</code> will contain a regular expression. */
    protected final boolean regex;
    protected List<MessageEntry> entries;

    /**
     * Factory method.
     * 
     * @param type
     * @param key
     * @param regex
     * @return
     * @should create correct class instance by type
     */
    public static TranslationGroupItem create(TranslationGroupType type, String key, boolean regex) {
        if (type == null) {
            throw new IllegalArgumentException("type may not be null");
        }

        switch (type) {
            case SOLR_FIELD_NAMES:
                return new SolrFieldNameTranslationGroupItem(key, regex);
            case SOLR_FIELD_VALUES:
                return new SolrFieldValueTranslationGroupItem(key, regex);
            default:
                return new MessagesTranslationGroupItem(key, regex);
        }
    }

    /**
     * Protected constructor.
     * 
     * @param key
     * @param regex
     */
    protected TranslationGroupItem(String key, boolean regex) {
        this.key = key;
        this.regex = regex;
    }

    public boolean isTranslated() {
        return false; // TODO
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the regex
     */
    public boolean isRegex() {
        return regex;
    }

    /**
     * @return the messageKeys
     */
    public List<MessageEntry> getEntries() {
        if (entries == null) {
            loadEntries();
        }

        return entries;
    }

    /**
     * Populates the message key map by first loading the appropriate keys and then calling <code>createMessageKeyStatusMap</code>. Each subclass will
     * have its specific data source, so each subclass must implement this method.
     */
    protected abstract void loadEntries();

    /**
     * Checks the translation status for each of the given keys and populates <code>messageKeys</code> accordingly.
     * 
     * @param keys
     */
    protected void createMessageKeyStatusMap(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            entries = Collections.emptyList();
            return;
        }

        entries = new ArrayList<>(keys.size());
        for (String k : keys) {
            List<Locale> allLocales = ViewerResourceBundle.getAllLocales();
            List<MessageValue> values = new ArrayList<>(allLocales.size());
            for (Locale locale : allLocales) {
                String translation = ViewerResourceBundle.getTranslation(k, locale, false, false);
                if (translation.equals(k)) {
                    values.add(new MessageValue(locale.getLanguage(), null));
                } else {
                    values.add(new MessageValue(locale.getLanguage(), translation));
                }
            }
            entries.add(new MessageEntry(k, values));
        }
    }
}
