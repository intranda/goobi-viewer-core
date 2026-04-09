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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains additional information of collections searched by {@link SearchHelper#findAllCollectionsFromField}.
 *
 * @author Florian Alpers
 */
public class CollectionResult implements Serializable {

    private static final long serialVersionUID = -5015011669151323658L;

    private final String name;
    private Set<String> facetValues = new HashSet<>();
    /**
     * A counter for all records within the collections or its descendants.
     */
    private Long recordCount = 0L;
    /**
     * A counter for direct child collections.
     */
    private Long childCount = 0L;

    public CollectionResult(String name) {
        this.name = name;
    }

    /**
     * @param name collection name (Solr field value)
     * @param recordCount number of records in this collection
     */
    public CollectionResult(String name, long recordCount) {
        this.name = name;
        this.recordCount = recordCount;
    }

    
    public Set<String> getFacetValues() {
        return facetValues;
    }

    
    public void setFacetValues(Set<String> facetValues) {
        this.facetValues = facetValues;
    }

    
    public Long getCount() {
        return recordCount;
    }

    
    public void setCount(Long count) {
        this.recordCount = count;
    }

    
    public String getName() {
        return name;
    }

    public void incrementCount(long l) {
        this.recordCount += l;
    }

    public Long getChildCount() {
        return this.childCount;
    }

    public void setChildCount(Long count) {
        this.childCount = count;
    }

    public void incrementChildCount(long l) {
        this.childCount += l;
    }

    /**
     * @param fieldValues collection of facet values to add to this result
     */
    public void addFacetValues(Collection<Object> fieldValues) {
        if (fieldValues != null) {
            for (Object object : fieldValues) {
                String value = object.toString();
                facetValues.add(value);
            }
        }

    }
}
