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
package io.goobi.viewer.model.search;

/**
 * Configuration element for advanced search fields.
 */
public class AdvancedSearchFieldConfiguration {

    public static final int DEFAULT_THRESHOLD = 10;

    public static final String SELECT_TYPE_BADGES = "badges";
    public static final String SELECT_TYPE_DROPDOWN = "dropdown";

    private final String field;
    private String label;
    private boolean hierarchical;
    private boolean range;
    private boolean untokenizeForPhraseSearch;
    private boolean disabled;
    private boolean visible = false;
    private int displaySelectItemsThreshold = DEFAULT_THRESHOLD;
    private String selectType = SELECT_TYPE_DROPDOWN;

    /**
     *
     * @param field Solr field name or separate placeholder
     */
    public AdvancedSearchFieldConfiguration(String field) {
        this.field = field;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the label
     * @should return field if label null
     */
    public String getLabel() {
        if (label == null) {
            return field;
        }
        return label;
    }

    /**
     * @param label the label to set
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * @return the hierarchical
     */
    public boolean isHierarchical() {
        return hierarchical;
    }

    /**
     * @param hierarchical the hierarchical to set
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical;
        return this;
    }

    /**
     * @return the range
     */
    public boolean isRange() {
        return range;
    }

    /**
     * @param range the range to set
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setRange(boolean range) {
        this.range = range;
        return this;
    }

    /**
     * @return the untokenizeForPhraseSearch
     */
    public boolean isUntokenizeForPhraseSearch() {
        return untokenizeForPhraseSearch;
    }

    /**
     * @param untokenizeForPhraseSearch the untokenizeForPhraseSearch to set
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setUntokenizeForPhraseSearch(boolean untokenizeForPhraseSearch) {
        this.untokenizeForPhraseSearch = untokenizeForPhraseSearch;
        return this;
    }

    /**
     * @return the disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * @param disabled the disabled to set
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     * @return this
     */
    public AdvancedSearchFieldConfiguration setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * @return the displaySelectItemsThreshold
     */
    public int getDisplaySelectItemsThreshold() {
        return displaySelectItemsThreshold;
    }

    /**
     * @param displaySelectItemsThreshold the displaySelectItemsThreshold to set
     * @return this
     */
    public AdvancedSearchFieldConfiguration setDisplaySelectItemsThreshold(int displaySelectItemsThreshold) {
        this.displaySelectItemsThreshold = displaySelectItemsThreshold;
        return this;
    }

    /**
     * @return the selectType
     */
    public String getSelectType() {
        return selectType;
    }

    /**
     * @param selectType the selectType to set
     * @return this
     */
    public AdvancedSearchFieldConfiguration setSelectType(String selectType) {
        this.selectType = selectType;
        return this;
    }

}
