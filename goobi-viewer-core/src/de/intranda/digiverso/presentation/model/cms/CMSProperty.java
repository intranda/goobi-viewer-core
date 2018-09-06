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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.logging.log4j.core.pattern.NotANumber;

/**
 * @author Florian Alpers
 *
 */
@Entity
@Table(name = "cms_properties")
public class CMSProperty {
    
    public static final String KEY_EMPTY= "EMPTY";
    
    /**
     * Boolean specifying that a page list should separate child pages by tag and prepend each group with a header
     */
    public static final String KEY_DISPLAY_CHILD_TAGS_AS_HEADERS = "DISPLAY_CHILD_TAGS_AS_HEADERS";
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "property_key", nullable = true)
    private String key = KEY_EMPTY;
    
    @Column(name = "property_value", nullable = true,  columnDefinition = "LONGTEXT")
    private String value;
    
//    /** Reference to the owning {@link PersistentEntity}. */
//    @ManyToOne
//    @JoinColumn(name = "property_owner_id")
//    private CMSPage owner;
    
    /**
     * 
     */
    public CMSProperty() {
    }
    
    /**
     * @param key2
     */
    public CMSProperty(String key) {
       this.key = key;
    }

    /**
     * Creates a clone of the given property
     * @param property
     */
    public CMSProperty(CMSProperty original) {
        if(original.id != null) {            
            this.id = new Long(original.id);
        }
        this.key = original.getKey();
        this.value = original.getValue();
        
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }
    
    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }
    
    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }
    
    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return key + " - " + value;
    }

    public boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }
    
    public Long getLongValue() {
        try {
            return Long.parseLong(value);
        } catch(NullPointerException | NumberFormatException e) {
            return null;
        }
    }
    
    public Double getDoubleValue() {
        try {
            return Double.parseDouble(value);
        } catch(NullPointerException | NumberFormatException e) {
            return null;
        }
    }
}
