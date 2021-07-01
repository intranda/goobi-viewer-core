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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.model.translations.admin.MessageEntry.TranslationStatus;

/**
 * Language + translation pair.
 */
public class MessageValue {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(MessageValue.class);

    private final String language;
    private final String globalValue;
    private String value;
    private String loadedValue;

    /**
     * 
     * @param language Language code
     * @param value
     * @param globalValue
     */
    public MessageValue(String language, String value, String globalValue) {
        this.language = language;
        this.value = value;
        this.loadedValue = value;
        this.globalValue = globalValue;
    }

    /**
     * 
     * @return true if <code>value</code> has been changed so that it no longer matches <code>loadedValue</code>; false otherwise
     */
    public boolean isDirty() {
        return value != null && !value.equals(loadedValue);
    }

    public void resetDirtyStatus() {
        loadedValue = value;
    }

    /**
     * @return true if <code>globalValue</code> is set but doesn't equal <code>value</code>; false otherwise
     */
    public boolean isDisplayGlobalValue() {
        return globalValue != null && !globalValue.equals(value);
    }

    /**
     * 
     * @return true if value is null or empty or contains 'zzz'; false otherwise
     * @should return true if status none of partial
     * @should return false if status full
     */
    public boolean isDisplayHighlight() {
        TranslationStatus status = getTranslationStatus();
        return TranslationStatus.NONE.equals(status) || TranslationStatus.PARTIAL.equals(status);
    }

    /**
     * 
     * @return Translation status of this value
     * @should return none status correctly
     * @should return partial status correctly
     * @should return full status correctly
     */
    public TranslationStatus getTranslationStatus() {
        if (StringUtils.isBlank(value)) {
            return TranslationStatus.NONE;
        }
        if (value.contains(" zzz")) {
            return TranslationStatus.PARTIAL;
        }

        return TranslationStatus.FULL;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return the globalValue
     */
    public String getGlobalValue() {
        return globalValue;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the loadedValue
     */
    public String getLoadedValue() {
        return loadedValue;
    }
}
