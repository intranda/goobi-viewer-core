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
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * A single message key with all its available translations for admin backend editing.
 */
public class MessageEntry implements Comparable<MessageEntry> {

    public enum TranslationStatus {
        NONE,
        PARTIAL,
        FULL;
    }

    private String keyPrefix;
    private String keySuffix;
    private final List<MessageValue> values;
    private boolean newEntryMode = false;

    /**
     * Factory method that creates a <code>MessageEntry</code> instance with values initialized for all given locales.
     *
     * @param keyPrefix Message key prefix (optional)
     * @param keySuffix Message key suffix (or entire key if no prefix)
     * @param allLocales List of locales
     * @return new <code>MessageEntry</code>
     * @should create MessageEntry correctly
     */
    public static MessageEntry create(final String keyPrefix, String keySuffix, List<Locale> allLocales) {
        if (keySuffix == null) {
            throw new IllegalArgumentException("keySuffix may not be null");
        }
        if (allLocales == null) {
            throw new IllegalArgumentException("allLocales may not be null");
        }
        String useKeyPrefix = keyPrefix;
        if (useKeyPrefix == null) {
            useKeyPrefix = "";
        }

        String key = useKeyPrefix + keySuffix.trim();
        List<MessageValue> values = new ArrayList<>(allLocales.size());
        for (Locale locale : allLocales) {
            String translation = ViewerResourceBundle.getTranslation(key, locale, false, false, false, false);
            String globalTranslation = ViewerResourceBundle.getTranslation(key, locale, false, false, true, false);
            values.add(new MessageValue(locale.getLanguage(), translation, globalTranslation));
        }

        return new MessageEntry(useKeyPrefix, keySuffix, values);
    }

    /**
     *
     * @param key
     * @param values
     */
    public MessageEntry(String key, List<MessageValue> values) {
        this.keyPrefix = "";
        this.keySuffix = key;
        this.values = values;
    }

    /**
     * Constructor with a composite key.
     *
     * @param keyPrefix
     * @param keySuffix
     * @param values
     */
    public MessageEntry(String keyPrefix, String keySuffix, List<MessageValue> values) {
        this.keyPrefix = keyPrefix != null ? keyPrefix : "";
        this.keySuffix = keySuffix;
        this.values = values;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getKey() == null) ? 0 : getKey().hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MessageEntry other = (MessageEntry) obj;
        if (getKey() == null) {
            if (other.getKey() != null) {
                return false;
            }
        } else if (!getKey().equals(other.getKey())) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @should compare correctly
     */
    @Override
    public int compareTo(MessageEntry o) {
        return this.getKey().compareTo(o.getKey());
    }

    /**
     * Returns the translation status over all languages.
     *
     * @return appropriate {@link TranslationStatus}
     * @should return none status correctly
     * @should return partial status correctly
     * @should return full status correctly
     */
    public TranslationStatus getTranslationStatus() {
        int full = 0;
        int none = 0;
        for (MessageValue value : values) {
            switch (value.getTranslationStatus()) {
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

        if (none == values.size()) {
            return TranslationStatus.NONE;
        }
        if (full == values.size()) {
            return TranslationStatus.FULL;
        }

        return TranslationStatus.PARTIAL;
    }

    /**
     * Returns the translation status for the requested language.
     *
     * @param language Requested language
     * @return appropriate {@link TranslationStatus}
     * @should return correct status for language
     */
    public TranslationStatus getTranslationStatusForLanguage(String language) {
        if (language == null) {
            return TranslationStatus.NONE;
        }

        for (MessageValue value : getValues()) {
            if (language.equals(value.getLanguage())) {
                return value.getTranslationStatus();
            }
        }

        return TranslationStatus.NONE;
    }

    /**
     * @return the key
     * @should trim suffix
     */
    public String getKey() {
        return keyPrefix + (keySuffix != null ? keySuffix.trim() : "");
    }

    /**
     * @return the keyPrefix
     */
    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     * @param keyPrefix the keyPrefix to set
     */
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * @return the keySuffix
     */
    public String getKeySuffix() {
        return keySuffix;
    }

    /**
     * @param keySuffix the keySuffix to set
     */
    public void setKeySuffix(String keySuffix) {
        this.keySuffix = keySuffix;
    }

    /**
     *
     * @return true if keySuffix blank; false otherwise
     */
    public boolean isKeySuffixBlank() {
        return StringUtils.isBlank(keySuffix);
    }

    /**
     * @return the values
     */
    public List<MessageValue> getValues() {
        return values;
    }

    /**
     * @return the newEntryMode
     */
    public boolean isNewEntryMode() {
        return newEntryMode;
    }

    /**
     * @param newEntryMode the newEntryMode to set
     */
    public void setNewEntryMode(boolean newEntryMode) {
        this.newEntryMode = newEntryMode;
    }

}
