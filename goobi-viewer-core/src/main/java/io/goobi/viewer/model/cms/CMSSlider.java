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
package io.goobi.viewer.model.cms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import io.goobi.viewer.dao.converter.StringListConverter;

/**
 * Class to persist sliders ("slideshows") created in CMS backend
 * @author florian
 *
 */
@Entity
@Table(name = "cms_sliders")
public class CMSSlider implements Serializable {

    private static final long serialVersionUID = -3029283417613875012L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slider_id")
    private Long id;
    @Column(name="source_type", nullable=false)
    private SourceType sourceType;
    @Column(name = "name", columnDefinition = "LONGTEXT")
    private String name;
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;
    @Column(name = "query", columnDefinition = "LONGTEXT")
    private String solrQuery;
    @Column(name = "categories", columnDefinition = "LONGTEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> categories  = new ArrayList<>();
    @Column(name = "collections", columnDefinition = "LONGTEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> collections  = new ArrayList<>();
    @Column(name="style")
    private String style = "base";
    /**
     * Copy constructor
     */
    public CMSSlider(CMSSlider o) {
        this.id = o.id;
        this.sourceType = o.sourceType;
        this.name = o.name;
        this.description = o.description;
        this.solrQuery = o.solrQuery;
        this.categories = new ArrayList<>(o.categories);
        this.collections = new ArrayList<>(o.collections);
        this.style = o.style;
    }
    
    /**
     * persistence constructor
     */
    public CMSSlider() {
        
    }
    
    /**
     * Default constructor. Provides the source type which should be treated as final
     * 
     * @param type
     */
    public CMSSlider(SourceType type) {
        this.sourceType = type;
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
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the solrQuery
     */
    public String getSolrQuery() {
        return solrQuery;
    }
    /**
     * @param solrQuery the solrQuery to set
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }
    /**
     * @return the categories
     */
    public List<String> getCategories() {
        return categories;
    }
    /**
     * @param categories the categories to set
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
    /**
     * @return the collections
     */
    public List<String> getCollections() {
        return collections;
    }
    /**
     * @param collections the collections to set
     */
    public void setCollections(List<String> collections) {
        this.collections = collections;
    }
    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }
    
    /**
     * @return the style
     */
    public String getStyle() {
        return style;
    }
    
    /**
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }
    
    /**
     * @return the sourceType
     */
    public SourceType getSourceType() {
        return sourceType;
    }
    
    public String getSourceUrl() {
        //TODO: return rest url, probably something like "/api/v1/slider/1/source.json"
        return null;
    }
    
    public static enum SourceType {
        RECORDS("cms__slider_type__records"), //has solrQuery
        COLLECTIONS("cms__slider_type__collections"), //has collections
        PAGES("cms__slider_type__pages"); //has categories
    
        private final String label;
        
        private SourceType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

}
