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
package io.goobi.viewer.model.citation;

/**
 * 
 */
public class CitationLink {

    public enum CitationLinkType {
        URL,
        INTERNAL;

        /**
         * 
         * @param name
         * @return
         */
        protected static CitationLinkType getByName(String name) {
            if (name == null) {
                return null;
            }

            for (CitationLinkType type : CitationLinkType.values()) {
                if (type.name().equals(name.toUpperCase())) {
                    return type;
                }
            }

            return null;
        }

    }

    public enum CitationLinkLevel {
        RECORD,
        DOCSTRUCT,
        IMAGE;

        /**
         * 
         * @param name
         * @return
         */
        protected static CitationLinkLevel getByName(String name) {
            if (name == null) {
                return null;
            }

            for (CitationLinkLevel level : CitationLinkLevel.values()) {
                if (level.name().equals(name.toUpperCase())) {
                    return level;
                }
            }

            return null;
        }
    }

    private final CitationLinkType type;
    private final CitationLinkLevel level;
    private final String label;
    private String field;
    private String prefix;
    private String suffix;

    /**
     * 
     * @param type
     * @param level
     * @param label
     */
    public CitationLink(String type, String level, String label) {
        this.type = CitationLinkType.getByName(type);
        if (this.type == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        this.level = CitationLinkLevel.getByName(level);
        if (this.level == null) {
            throw new IllegalArgumentException("Unknown level: " + level);
        }
        this.label = label;
    }

    /**
     * @return the type
     */
    public CitationLinkType getType() {
        return type;
    }

    /**
     * @return the level
     */
    public CitationLinkLevel getLevel() {
        return level;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @param field the field to set
     * @return this
     */
    public CitationLink setField(String field) {
        this.field = field;
        return this;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param prefix the prefix to set
     * @return this
     */
    public CitationLink setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * @return the suffix
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * @param suffix the suffix to set
     * @return this
     */
    public CitationLink setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }
}
