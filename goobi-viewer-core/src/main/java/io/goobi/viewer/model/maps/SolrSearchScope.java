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
package io.goobi.viewer.model.maps;

public enum SolrSearchScope {

    ALL("label__geomap_search_scope__alls", "desc__geomap_search_scope__all"),
    RECORDS("label__geomap_search_scope__records", "desc__geomap_search_scope__records"),
    METADATA("label__geomap_saerch_scope__metadata", "desc__geomap_saerch_scope__metadata"),
    DOCSTRUCTS("label__geomap_saerch_scope__docStructs", "desc__geomap_saerch_scope__docStructs");

    private final String label;
    private final String description;

    private SolrSearchScope(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSearchInTopDocuments() {
        return this == ALL || this == RECORDS;
    }

    public boolean isSearchInStructureDocuments() {
        return this == ALL || this == DOCSTRUCTS;
    }

    public boolean isSearchInMetadata() {
        return this == ALL || this == METADATA;
    }

    String getQuery() {
        switch (this) {
            case RECORDS:
                return "+(ISWORK:true ISANCHOR:true)";
            case DOCSTRUCTS:
                return "+DOCTYPE:DOCSTRCT -(ISWORK:true ISANCHOR:true)";
            case METADATA:
                return "+DOCTYPE:METADATA";
            case ALL:
            default:
                return "";

        }
    }

}
