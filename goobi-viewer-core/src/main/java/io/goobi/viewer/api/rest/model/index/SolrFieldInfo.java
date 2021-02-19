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
package io.goobi.viewer.api.rest.model.index;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Solr field information as JSON.
 */
@Schema(name = "SolrFieldInfo", description = "Solr field information", requiredProperties = { "field" })
public class SolrFieldInfo {

    @Schema(description = "Main Solr field", example = "MD_TITLE, MDNUM_COUNT, DOCSTRCT, ACCESSCONDITION, PI, YEAR")
    private final String field;

    @Schema(description = "Field variant for sorting", example = "SORT_TITLE, SORTNUM_YEAR")
    @JsonInclude(Include.NON_ABSENT)
    private String sortField = null;

    @Schema(description = "Field variant for faceting", example = "FACET_TITLE, FACET_YEAR")
    @JsonInclude(Include.NON_ABSENT)
    private String facetField = null;

    @Schema(description = "Boolean field variant, indicates whether values for the main field exist in a given Solr document",
            example = "BOOL_TITLE")
    @JsonInclude(Include.NON_ABSENT)
    private String boolField = null;

    @Schema(description = "Indicates whether this field is configured as 'indexed' (value is searchable) in the Solr schema")
    private boolean indexed = true;

    @Schema(description = "Indicates whether this field is configured as 'stored' (value is readable) in the Solr schema")
    private boolean stored = true;

    @Schema(description = "Field name translation for available languages", example = "en: Title, de: Titel")
    @JsonInclude(Include.NON_NULL)
    private Map<String, String> translations = null;

    /**
     * Constructor.
     * 
     * @param field Main field name
     */
    public SolrFieldInfo(String field) {
        this.field = field;
    }

    /**
     * @return the field
     */
    public String getField() {
        return field;
    }

    /**
     * @return the sortField
     */
    public String getSortField() {
        return sortField;
    }

    /**
     * @param sortField the sortField to set
     */
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    /**
     * @return the facetField
     */
    public String getFacetField() {
        return facetField;
    }

    /**
     * @param facetField the facetField to set
     */
    public void setFacetField(String facetField) {
        this.facetField = facetField;
    }

    /**
     * @return the boolField
     */
    public String getBoolField() {
        return boolField;
    }

    /**
     * @param boolField the boolField to set
     */
    public void setBoolField(String boolField) {
        this.boolField = boolField;
    }

    /**
     * @return the indexed
     */
    public boolean isIndexed() {
        return indexed;
    }

    /**
     * @param indexed the indexed to set
     */
    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    /**
     * @return the stored
     */
    public boolean isStored() {
        return stored;
    }

    /**
     * @param stored the stored to set
     */
    public void setStored(boolean stored) {
        this.stored = stored;
    }

    /**
     * @return the translations
     */
    public Map<String, String> getTranslations() {
        if (translations == null) {
            translations = new HashMap<>();
        }

        return translations;
    }
}
