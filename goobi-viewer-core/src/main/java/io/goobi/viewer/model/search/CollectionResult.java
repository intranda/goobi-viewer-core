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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains additional information of collections searched by {@link SearchHelper#findAllCollectionsFromField}
 * 
 * @author florian
 *
 */
public class CollectionResult {

    private final String name;
    private Set<String> facetValues = new HashSet<>();
    private Long count = 0l; 
    

    public CollectionResult(String name) {
        this.name = name;
    }


    /**
     * @param string
     * @param l
     */
    public CollectionResult(String name, long l) {
        this.name = name;
        this.count = l;
    }


    /**
     * @return the groupingValues
     */
    public Set<String> getFacetValues() {
        return facetValues;
    }


    /**
     * @param groupingValues the groupingValues to set
     */
    public void setFacetValues(Set<String> facetValues) {
        this.facetValues = facetValues;
    }


    /**
     * @return the count
     */
    public Long getCount() {
        return count;
    }


    /**
     * @param count the count to set
     */
    public void setCount(Long count) {
        this.count = count;
    }


    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    public void incrementCount(long l) {
        this.count += l;
    }


    /**
     * @param fieldValues
     */
    public void addFacetValues(Collection<Object> fieldValues) {
        if(fieldValues != null) {            
            for (Object object : fieldValues) {
                String value = object.toString();
                facetValues.add(value);
            }
        }
        
    }
}
