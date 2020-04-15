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
package io.goobi.viewer.model.search;

/**
 * Configuration element for advanced search fields.
 */
public class AdvancedSearchFieldConfiguration {

    private final String field;
    private final String label;
    private final boolean hierarchical;
    private final boolean untokenizeForPhraseSearch;
    private final boolean disabled;

    /**
     * 
     * @param field Solr field name or separate placeholder
     * @param label Optional lternative label
     * @param hierarchical
     * @param untokenizeForPhraseSearch
     * @param disabled
     */
    public AdvancedSearchFieldConfiguration(String field, String label, boolean hierarchical, boolean untokenizeForPhraseSearch, boolean disabled) {
        this.field = field;
        this.label = label;
        this.hierarchical = hierarchical;
        this.untokenizeForPhraseSearch = untokenizeForPhraseSearch;
        this.disabled = disabled;
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
     * @return the hierarchical
     */
    public boolean isHierarchical() {
        return hierarchical;
    }

    /**
     * @return the untokenizeForPhraseSearch
     */
    public boolean isUntokenizeForPhraseSearch() {
        return untokenizeForPhraseSearch;
    }

    /**
     * @return the disabled
     */
    public boolean isDisabled() {
        return disabled;
    }
}
