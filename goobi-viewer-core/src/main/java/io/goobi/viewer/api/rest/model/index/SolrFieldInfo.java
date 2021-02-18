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

/**
 * Solr field information as JSON.
 */
public class SolrFieldInfo {

    private final String field;

    private String sortField = "";

    private String facetField = "";

    private String boolField = "";

    private boolean indexed = true;

    private boolean stored = true;

    private final Map<String, String> translations = new HashMap<>();

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
        return translations;
    }
}
