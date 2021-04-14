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
package io.goobi.viewer.model.translations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Translation group configuration item.
 */
public class TranslationGroup {

    public enum TranslationGroupType {
        SOLR_FIELD_NAMES,
        SOLR_FIELD_VALUES,
        CORE_STRINGS;

        /**
         * 
         * @param name
         * @return
         */
        public static TranslationGroupType getByName(String name) {
            if (name == null) {
                return null;
            }

            for (TranslationGroupType type : TranslationGroupType.values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }

            return null;
        }
    }

    private final int id;
    private final TranslationGroupType type;
    private final String name;
    private final String description;
    private final List<TranslationGroupItem> items;

    private Integer translatedKeyCount = null;

    /**
     * Factory method.
     * 
     * @param id unique ID number
     * @param type
     * @param name
     * @param description
     * @param numKeys
     * @return
     */
    public static TranslationGroup create(int id, TranslationGroupType type, String name, String description, int numKeys) {
        return new TranslationGroup(id, type, name, description, numKeys);
    }

    /**
     * Private constructor.
     * 
     * @param id
     * @param type
     * @param name
     * @param description
     * @param numKeys
     */
    private TranslationGroup(int id, TranslationGroupType type, String name, String description, int numKeys) {
        if (numKeys < 0) {
            throw new IllegalArgumentException("numKeys may not be negative");
        }
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.items = new ArrayList<>(numKeys);
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the type
     */
    public TranslationGroupType getType() {
        return type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the keys
     */
    public List<TranslationGroupItem> getItems() {
        return items;
    }

    /**
     * 
     * @return Number of unique message keys across all groups
     * @should return correct count
     */
    public int getMessageKeyCount() {
        Set<String> returnSet = new HashSet<>();
        for (TranslationGroupItem item : items) {
            for (String key : item.getMessageKeys()) {
                returnSet.add(key);
            }
        }

        return returnSet.size();
    }

    /**
     * 
     * @return
     */
    public Integer getTranslatedKeyCount() {
        if (translatedKeyCount == null) {
            translatedKeyCount = 0; // TODO
            for (TranslationGroupItem key : items) {
                if (key.isTranslated()) {
                    translatedKeyCount++;
                }
            }
        }
        return translatedKeyCount;
    }

    /**
     * 
     * @return Unique message keys across all groups
     */
    public List<String> getAllKeys() {
        Set<String> returnSet = new HashSet<>();
        for (TranslationGroupItem item : items) {
            for (String key : item.getMessageKeys()) {
                returnSet.add(key);
            }
        }

        List<String> ret = new ArrayList<>(returnSet);
        Collections.sort(ret);
        return ret;
    }
}
