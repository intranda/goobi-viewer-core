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
package io.goobi.viewer.model.maps;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.eclipse.persistence.annotations.PrivateOwned;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.rest.serialization.TranslationListSerializer;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "cms_geomap")
public class GeoMap {
    
    private static final Logger logger = LoggerFactory.getLogger(GeoMap.class);
    
    /**
     * Placeholder User if the actual creator could not be determined
     */
    private static final User CREATOR_UNKNOWN = new User("Unknown");
    
    private static final String METADATA_TAG_TITLE = "Title";
    private static final String METADATA_TAG_DESCRIPTION = "Description";

    public static enum GeoMapType {
        SOLR_QUERY,
        MANUAL
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "geomap_id")
    private Long id;
    
    @Column(name = "creator_id")
    private Long creatorId;
    
    @Transient
    private User creator;
    
    /** Translated metadata. */
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    @JsonSerialize(using = TranslationListSerializer.class)
    private Set<MapTranslation> translations = new HashSet<>();
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false)
    @JsonIgnore
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated")
    @JsonIgnore
    private Date dateUpdated;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cms_geomap_features", joinColumns = @JoinColumn(name = "geomap_id"))
    @Column(name = "features", columnDefinition = "LONGTEXT")
    private List<String> features = new ArrayList<String>();

    @Column(name = "map_type")
    private GeoMapType type = null;
    
    @Column(name="initial_view")
    private String initialView = "{}";

    /**
     * Empty Constructor
     */
    public GeoMap() {
        // TODO Auto-generated constructor stub
    }
    
    /**
     * Clone constructor
     * 
     * @param blueprint
     */
    public GeoMap(GeoMap blueprint) {
        this.creatorId = blueprint.creatorId;
        this.creator = blueprint.creator;
        this.dateCreated = blueprint.dateCreated;
        this.dateUpdated = blueprint.dateUpdated;
        this.id = blueprint.id;
        this.translations = blueprint.translations;//.stream().filter(t -> !t.isEmpty()).map(t -> new MapTranslation(t)).collect(Collectors.toSet());
        this.type = blueprint.type;
        this.features = blueprint.features;
        this.initialView = blueprint.initialView;
        
    }
    
    
    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }
    
    /**
     * @return the creatorId
     */
    public Long getCreatorId() {
        return creatorId;
    }
    
    /**
     * @param creatorId the creatorId to set
     */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
        this.creator = null;
    }
    
    public User getCreator() {
        if(this.creator == null) {
            this.creator = CREATOR_UNKNOWN;
            try {
                this.creator = DataManager.getInstance().getDao().getUser(this.creatorId);
            } catch (DAOException e) {
                logger.error("Error getting creator ", e);
            }
        }
        return this.creator;
    }
    
    public void setCreator(User creator) {
        this.creator = creator;
        if(creator != null) {            
            this.creatorId = creator.getId();
        } else {
            this.creatorId = null;
        }
    }
    
    /**
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }
    
    /**
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }
    
    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
    
    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }
    
    public String getTitle() {
        MapTranslation title = getTitle(BeanUtils.getNavigationHelper().getLocale().getLanguage());
        if(title.isEmpty()) {
            title = getTitle(BeanUtils.getNavigationHelper().getDefaultLocale().getLanguage());
        }
        return title.getValue();
    }
    
    public String getDescription() {
        MapTranslation desc = getDescription(BeanUtils.getNavigationHelper().getLocale().getLanguage());
        if(desc.isEmpty()) {
            desc = getDescription(BeanUtils.getNavigationHelper().getDefaultLocale().getLanguage());
        }
        return desc.getValue();
    }

    
    public MapTranslation getTitle(String language) {
        MapTranslation title = translations.stream()
                .filter(t -> METADATA_TAG_TITLE.equals(t.getTag()))
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst().orElse(null);
        if(title == null) {
            title = new MapTranslation(language, METADATA_TAG_TITLE, this);
            translations.add(title);
        }
        return title;
    }
    
    public MapTranslation getDescription(String language) {
        MapTranslation title = translations.stream()
                .filter(t -> METADATA_TAG_DESCRIPTION.equals(t.getTag()))
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst().orElse(null);
        if(title == null) {
            title = new MapTranslation(language, METADATA_TAG_DESCRIPTION, this);
            translations.add(title);
        }
        return title;
    }
    
    /**
     * @return the type
     */
    public GeoMapType getType() {
        return type;
    }
    
    /**
     * @param type the type to set
     */
    public void setType(GeoMapType type) {
        this.type = type;
    }
    
    /**
     * @return the features
     */
    public List<String> getFeatures() {
        return features;
    }
    
    /**
     * @param features the features to set
     */
    public void setFeatures(List<String> features) {
        this.features = features;
    }
    
    public String getFeaturesAsString() {
        String string = this.features.stream().collect(Collectors.joining(","));
        string = "[" + string + "]";
        return string;
    }
    
    public void setFeaturesAsString(String features) {
        JSONArray array = new JSONArray(features);
        this.features = new ArrayList<>();
        for (Object object : array) {
            this.features.add(object.toString());
        }
    }
    
    public static void main(String[] args) {
        
        String string = "[{a: \"b,c\", b: \"sdfsdf\"}, {a: [\"a\",\"b\", \"c\"], d: 12, e: {a: 2}},{a: \"b\", d: { a: [1,2], b: \"dsfsdf\"}}]";
        
        JSONArray array = new JSONArray(string);
        for (Object object : array) {
            System.out.println(object);
        }
    }
    
    /**
     * @param initialView the initialView to set
     */
    public void setInitialView(String initialView) {
        this.initialView = initialView;
    }
    
    /**
     * @return the initialView
     */
    public String getInitialView() {
        return initialView;
    }
    
}
