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

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrTools;

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

    private static final Logger logger = LogManager.getLogger(CitationLink.class);

    private final CitationLinkType type;
    private final CitationLinkLevel level;
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

        return getValue(viewManager);
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
     * @param viewManager
     * @return the value
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should return correct value for record type
     * @should return correct value for docstruct type
     * @should return correct value for image type
     * @should fall back to topstruct value correctly
     */
    public String getValue(ViewManager viewManager) throws IndexUnreachableException, PresentationException {
        if (!CitationLinkType.URL.equals(type) || viewManager == null) {
            return null;
        }

        if (StringUtils.isEmpty(this.value)) {
            // logger.trace("Loading value: {}/{}", level, field); //NOSONAR Debug
            String query = null;
            switch (level) {
                case RECORD:
                    query = SolrConstants.PI + ":" + viewManager.getPi();
                    break;
                case DOCSTRUCT:
                    query = "+" + SolrConstants.IDDOC + ":" + viewManager.getCurrentStructElement().getLuceneId();
                    break;
                case IMAGE:
                    query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + viewManager.getPi() + " +" + SolrConstants.ORDER + ":"
                            + viewManager.getCurrentImageOrder() + " +" + SolrConstants.DOCTYPE
                            + ":" + DocType.PAGE.name();
                    break;
                default:
                    break;
            }

            SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(field));
            if (doc == null) {
                return null;
            }

            if (doc.get(field) != null) {
                this.value = SolrTools.getAsString(doc.get(field));
            } else if (topstructValueFallback && !CitationLinkLevel.RECORD.equals(level)) {
                query = SolrConstants.PI + ":" + viewManager.getPi();
                doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, Collections.singletonList(field));
                if (doc != null && doc.get(field) != null) {
                    this.value = SolrTools.getAsString(doc.get(field));
                }
            }

            if (StringUtils.isNotEmpty(pattern) && this.value != null) {
                this.value = pattern.replace("{value}", this.value)
                        .replace("{page}", String.valueOf(viewManager.getCurrentImageOrder()));
            }
        }

        return this.value;
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

}
