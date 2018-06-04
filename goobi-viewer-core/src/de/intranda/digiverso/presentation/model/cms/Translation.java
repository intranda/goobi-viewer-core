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

/**
 * A persistence object holding a translated String value
 * 
 * @author Florian Alpers
 *
 */
@Entity
@Table(name = "translations")
public class Translation {
    
    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "translation_id")
    private Long id;

    /** Reference to the owning {@link PersistentEntity}. */
    @ManyToOne
    @JoinColumn(name = "translation_owner_id")
    private CMSCollection owner;
    
    /** An additional optional field used to identify the purpose or categorization of a translation.
     * Usefull if an object has more than one relationship with Translation entities and needs to
     * distinguish them in some way
     * **/
    @Column(name="tag", nullable=true, columnDefinition = "LONGTEXT")
    private String tag;
    
    @Column(name = "language")
    private String language;
    
    @Column(name = "value")
    private String value;
    
    public Translation() {
        
    }
    
    public Translation(String language, String value) {
        this.language = language;
        this.value = value;
    }
    
    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
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
    
    /**
     * @return the tag
     */
    public String getTag() {
        return tag;
    }
    
    /**
     * @param tag the tag to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    /**
     * @return the owner
     */
    public CMSCollection getOwner() {
        return owner;
    }
    
    /**
     * @param owner the owner to set
     */
    public void setOwner(CMSCollection owner) {
        this.owner = owner;
    }

    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return value;
    }

}
