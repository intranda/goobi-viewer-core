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
    public static final String SELECT_TYPE_CHECKBOXES = "checkboxes";
    public static final String SELECT_TYPE_DROPDOWN = "dropdown";

    private final String field;
    private String label;
    private boolean hierarchical;
    private boolean range;
    private boolean datepicker;
    private boolean untokenizeForPhraseSearch;
    private boolean disabled;
    private boolean visible = false;
    private boolean allowMultipleItems = false;
    private int displaySelectItemsThreshold = DEFAULT_THRESHOLD;
    private String selectType = SELECT_TYPE_DROPDOWN;
    private String replaceRegex;
    private String replaceWith;
    private String preselectValue;

    /**
     *
     * @param field Solr field name or separate placeholder
     */
    public AdvancedSearchFieldConfiguration(String field) {
        this.field = field;
    }

    
    public String getField() {
        return field;
    }

    /**

     * @should return field if label null
     */
    public String getLabel() {
        if (label == null) {
            return field;
        }
        return label;
    }

    /**
     * @param label the display label for this search field configuration
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setLabel(String label) {
        this.label = label;
        return this;
    }

    
    public boolean isHierarchical() {
        return hierarchical;
    }

    /**
     * @param hierarchical true if the field should be displayed hierarchically; false otherwise
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical;
        return this;
    }

    
    public boolean isRange() {
        return range;
    }

    /**
     * @param range true if the field should be displayed as a range input; false otherwise
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setRange(boolean range) {
        this.range = range;
        return this;
    }

    
    public boolean isDatepicker() {
        return datepicker;
    }

    /**
     * @param datepicker true if the field should use a datepicker UI control; false otherwise
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setDatepicker(boolean datepicker) {
        this.datepicker = datepicker;
        return this;
    }

    
    public boolean isUntokenizeForPhraseSearch() {
        return untokenizeForPhraseSearch;
    }

    /**
     * @param untokenizeForPhraseSearch true if the field value should be untokenized when used in phrase search queries; false otherwise
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setUntokenizeForPhraseSearch(boolean untokenizeForPhraseSearch) {
        this.untokenizeForPhraseSearch = untokenizeForPhraseSearch;
        return this;
    }

    
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * @param disabled true if this search field configuration should be disabled; false otherwise
     * @return this object
     */
    public AdvancedSearchFieldConfiguration setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    
    public boolean isVisible() {
        return visible;
    }

    
    public boolean isAllowMultipleItems() {
        return allowMultipleItems;
    }

    /**
     * @param allowMultipleItems true if multiple search items may be added for this field; false otherwise
     * @return this
     */
    public AdvancedSearchFieldConfiguration setAllowMultipleItems(boolean allowMultipleItems) {
        this.allowMultipleItems = allowMultipleItems;
        return this;
    }

    /**
     * @param visible true if this field configuration should be visible to the user; false otherwise
     * @return this
     */
    public AdvancedSearchFieldConfiguration setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    
    public int getDisplaySelectItemsThreshold() {
        return displaySelectItemsThreshold;
    }

    /**
     * @param displaySelectItemsThreshold maximum number of available values below which a select widget is displayed instead of a text input
     * @return this
     */
    public AdvancedSearchFieldConfiguration setDisplaySelectItemsThreshold(int displaySelectItemsThreshold) {
        this.displaySelectItemsThreshold = displaySelectItemsThreshold;
        return this;
    }

    
    public String getSelectType() {
        return selectType;
    }

    /**
     * @param selectType the type of select widget to use for this field (e.g. dropdown, checkboxes)
     * @return this
     */
    public AdvancedSearchFieldConfiguration setSelectType(String selectType) {
        this.selectType = selectType;
        return this;
    }

    
    public String getReplaceRegex() {
        return replaceRegex;
    }

    /**
     * @param replaceRegex the regular expression pattern applied to field values before display
     * @return this
     */
    public AdvancedSearchFieldConfiguration setReplaceRegex(String replaceRegex) {
        this.replaceRegex = replaceRegex;
        return this;
    }

    
    public String getReplaceWith() {
        return replaceWith;
    }

    /**
     * @param replaceWith the replacement string substituted for matches of the replaceRegex pattern
     * @return this
     */
    public AdvancedSearchFieldConfiguration setReplaceWith(String replaceWith) {
        this.replaceWith = replaceWith;
        return this;
    }

    
    public String getPreselectValue() {
        return preselectValue;
    }

    /**
     * @param preselectValue the value that should be pre-selected in the search field by default
     * @return this
     */
    public AdvancedSearchFieldConfiguration setPreselectValue(String preselectValue) {
        this.preselectValue = preselectValue;
        return this;
    }
}
