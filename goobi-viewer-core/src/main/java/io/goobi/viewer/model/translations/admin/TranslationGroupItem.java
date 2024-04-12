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
import java.util.List;
import java.util.Locale;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.translations.admin.MessageEntry.TranslationStatus;
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
     * @return Created {@link TranslationGroupItem}
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
            case LOCAL_STRINGS:
                return new LocalMessagesTranslationGroupItem(key, regex);
            default:
                return new CoreMessagesTranslationGroupItem(key, regex);
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

    /**
     * Returns the translation status over all existing entries.
     *
     * @return <code>TranslationStatu</code>; FULL if all entries are FULL; NONE if all entries are NONE; PARTIAL otherwise
     * @throws PresentationException 
     * @throws IndexUnreachableException 
     */
    public TranslationStatus getTranslationStatus() throws IndexUnreachableException, PresentationException {
        int full = 0;
        int none = 0;
        for (MessageEntry entry : getEntries()) {
            switch (entry.getTranslationStatus()) {
                case NONE:
                    none++;
                    break;
                case FULL:
                    full++;
                    break;
                default:
                    break;
            }
        }

        if (none == entries.size()) {
            return TranslationStatus.NONE;
        }
        if (full == entries.size()) {
            return TranslationStatus.FULL;
        }

        return TranslationStatus.PARTIAL;
    }

    /**
     * Returns the translation status for the requested language over all existing entries.
     *
     * @param language Requested language
     * @return <code>TranslationStatu</code>; FULL if all entries are FULL; NONE if all entries are NONE; PARTIAL otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws Exception
     */
    public TranslationStatus getTranslationStatusLanguage(String language) throws IndexUnreachableException, PresentationException {
        int full = 0;
        int none = 0;
        for (MessageEntry entry : getEntries()) {
            switch (entry.getTranslationStatusForLanguage(language)) {
                case NONE:
                    none++;
                    break;
                case FULL:
                    full++;
                    break;
                default:
                    break;
            }
        }

        if (none == entries.size()) {
            return TranslationStatus.NONE;
        }
        if (full == entries.size()) {
            return TranslationStatus.FULL;
        }

        return TranslationStatus.PARTIAL;
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
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws Exception
     */
    public List<MessageEntry> getEntries() throws IndexUnreachableException, PresentationException {
        if (entries == null) {
            loadEntries();
        }

        return entries;
    }

    /**
     * Populates the message key map by first loading the appropriate keys and then calling <code>createMessageKeyStatusMap</code>. Each subclass will
     * have its specific data source, so each subclass must implement this method.
     */
    protected abstract void loadEntries() throws IndexUnreachableException, PresentationException;

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
        List<Locale> allLocales = ViewerResourceBundle.getAllLocales();
        for (String k : keys) {
            entries.add(MessageEntry.create(null, k, allLocales));
        }
    }
}
