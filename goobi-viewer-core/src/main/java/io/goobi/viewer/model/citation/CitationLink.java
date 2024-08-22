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
package io.goobi.viewer.model.citation;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.ViewManager;

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
         * @return {@link CitationLinkType}; null if no matching type found
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
         * @return {@link CitationLinkLevel}; null if no matching level found
         */
        public static CitationLinkLevel getByName(String name) {
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

    public enum CitationLinkAction {
        CLIPBOARD,
        OPEN,
        DOWNLOAD;

        /**
         *
         * @param name
         * @return {@link CitationLinkLevel}; null if no matching level found
         */
        public static CitationLinkAction getByName(String name) {
            if (name == null) {
                return null;
            }

            for (CitationLinkAction acion : CitationLinkAction.values()) {
                if (acion.name().equals(name.toUpperCase())) {
                    return acion;
                }
            }

            return null;
        }
    }

    private static final Logger logger = LogManager.getLogger(CitationLink.class);

    private final CitationLinkType type;
    private final CitationLinkLevel level;
    private final CitationLinkAction action;
    private final String label;
    private String field;
    private String value;
    private String pattern;
    private boolean topstructValueFallback = false;

    /**
     *
     * @param type
     * @param level
     * @param label
     * 
     */
    public CitationLink(String type, String level, String action, String label) {
        this.type = CitationLinkType.getByName(type);
        if (this.type == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        this.level = CitationLinkLevel.getByName(level);
        if (this.level == null) {
            throw new IllegalArgumentException("Unknown level: " + level);
        }
        this.action = CitationLinkAction.getByName(StringUtils.isBlank(action) ? "clipboard" : action);
        if (this.action == null) {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
        this.label = label;
    }

    public CitationLinkAction getAction() {
        return action;
    }

    /**
     * 
     * @param viewManager
     * @return Appropriate URL
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @should construct internal record url correctly
     * @should construct internal docstruct url correctly
     * @should construct internal image url correctly
     * @should construct external url correctly
     */
    public String getUrl(ViewManager viewManager) throws PresentationException, IndexUnreachableException, DAOException {
        // logger.trace("getUrl: {}/{}", level, field); //NOSONAR Debug
        if (viewManager == null) {
            return null;
        }

        if (CitationLinkType.INTERNAL.equals(type)) {
            switch (level) {
                case RECORD:
                    return viewManager.getCiteLinkWork();
                case DOCSTRUCT:
                    return viewManager.getCiteLinkDocstruct();
                case IMAGE:
                    return viewManager.getCiteLinkPage();
                default:
                    break;
            }
        }

        return getValue();
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
     * 
     * @return the value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
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
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     * @return this
     */
    public CitationLink setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    /**
     * @return the topstructValueFallback
     */
    public boolean isTopstructValueFallback() {
        return topstructValueFallback;
    }

    /**
     * @param topstructValueFallback the topstructValueFallback to set
     * @return this
     */
    public CitationLink setTopstructValueFallback(boolean topstructValueFallback) {
        this.topstructValueFallback = topstructValueFallback;
        return this;
    }

    public boolean isEmpty() {
        switch (type) {
            case URL:
                return StringUtils.isBlank(value);
            case INTERNAL:
                return false;
            default:
                return true;
        }
    }

}
