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
package de.intranda.digiverso.presentation.model.cms;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

/**
 * A class representing persistent configurations for a collection.
 * A collections is identified by a SOLR-field name and a label. The most common SOLR-field is "DC" and label is the internal 
 * name of the collection to edit.
 * This class allows setting a representative image, names in multiple languages and a uri linking to a collection page.
 * 
 * @author Florian Alpers
 *
 */
@Entity
@Table(name = "cms_collections")
public class CMSCollection implements Comparable<CMSCollection>{
    
    @Column(name = "solr_field")
    private final String solrField;
    
    @Column(name = "solr_value")
    private final String solrFieldValue;
    
    
    /**
     * Default constructor, creating a Collection from the identifying fields {@link CMSCollection#solrField} and {@link CMSCollection#solrFieldValue}
     * 
     * @param solrField         The name of the SOLR field holding the values for the collection
     * @param solrFieldValue    The value of the solrField identifying this collection
     * 
     * @throws IllegalArgumentException     If either argument returns true for {@link StringUtils#isBlank(CharSequence)}
     */
    public CMSCollection(String solrField, String solrFieldValue) {
        if(StringUtils.isBlank(solrField) || StringUtils.isBlank(solrFieldValue)) {
            throw new IllegalArgumentException("The constructor paramters of CMSCollections may not be null, empty or blank");
        }
        this.solrField = solrField;
        this.solrFieldValue = solrFieldValue;
    }
    
    
    
    /**
     * @return the solrField. Guaranteed to hold a non-blank value
     */
    public String getSolrField() {
        return solrField;
    }
    
    /**
     * @return the solrFieldValue. Guaranteed to hold a non-blank value
     */
    public String getSolrFieldValue() {
        return solrFieldValue;
    }

    /**
     * Compares collection by the alphabatical sorting of their {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public int compareTo(CMSCollection other) {
        return getSolrFieldValue().compareTo(other.getSolrFieldValue());
    }
    
    /**
     * Returns the hashCode of {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public int hashCode() {
        return getSolrFieldValue().hashCode();
    }
    
    /**
     * A {@link CMSCollection} is equal to any other object if that is also a CMSCollection and returns the same
     * values for {@link CMSCollection#getSolrField()} and {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass().equals(this.getClass())) {
            return getSolrField().equals(((CMSCollection) obj).getSolrField()) &&
                    getSolrFieldValue().equals(((CMSCollection) obj).getSolrFieldValue());
        } else {
            return false;
        }
    }
    

}
