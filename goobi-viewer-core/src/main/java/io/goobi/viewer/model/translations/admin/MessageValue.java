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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Language + translation pair.
 */
public class MessageValue {
    
    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(MessageValue.class);

    private final String language;
    private String loadedValue;
    private String value;

    /**
     * 
     * @return
     */
    public boolean isDirty() {
        return value != null && !value.equals(loadedValue);
    }

    public void resetDirtyStatus() {
        loadedValue = value;
    }

    /**
     * 
     * @param language Language code
     * @param value
     */
    public MessageValue(String language, String value) {
        this.language = language;
        this.value = value;
        this.loadedValue = value;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
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
        logger.trace("setValue: {}", value);
        this.value = value;
    }

    /**
     * @return the loadedValue
     */
    public String getLoadedValue() {
        return loadedValue;
    }
}
