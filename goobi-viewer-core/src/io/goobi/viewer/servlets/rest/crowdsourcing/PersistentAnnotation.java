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
package io.goobi.viewer.servlets.rest.crowdsourcing;

import java.io.IOException;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.TypedResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.user.User;

/**
 * An Annotation class to store annotation in a database
 * 
 * @author florian
 *
 */
@Entity
@Table(name = "annotations")
public class PersistentAnnotation{

    private static final Logger logger = LoggerFactory.getLogger(PersistentAnnotation.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annotation_id")
    private Long id;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created")
    private Date dateCreated;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_modified")
    private Date dateModified;
    
    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;
    
    @ManyToOne
    @JoinColumn(name = "generator_id")
    private Campaign generator;
    
    @Column(name = "body", columnDefinition="JSON_OBJECT")
    private String body;
    
    @Column(name = "target", columnDefinition="JSON_OBJECT")
    private String target;
       
    
    public PersistentAnnotation(WebAnnotation source) {
        this.dateCreated = source.getCreated();
        this.dateModified = source.getModified();
        try {
            this.creator = DataManager.getInstance().getDao().getUser(Long.parseLong(source.getCreator().getId().toString()));
        } catch (NumberFormatException | DAOException e) {
            logger.error("Error getting creator of " + source, e);
        }
        try {
            this.generator = DataManager.getInstance().getDao().getCampaign(Long.parseLong(source.getGenerator().getId().toString()));
        } catch (NumberFormatException | DAOException e) {
            logger.error("Error getting generator of " + source, e);
        }
        
        ObjectMapper mapper = new ObjectMapper();        
        try {
            this.body = mapper.writeValueAsString(source.getBody());
        } catch (JsonProcessingException e) {
            logger.error("Error writing body " + source.getBody() + " to string ", e);
        }
        try {
            this.target = mapper.writeValueAsString(source.getTarget());
        } catch (JsonProcessingException e) {
            logger.error("Error writing body " + source.getBody() + " to string ", e);
        }
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
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }


    /**
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }


    /**
     * @return the dateModified
     */
    public Date getDateModified() {
        return dateModified;
    }


    /**
     * @param dateModified the dateModified to set
     */
    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }


    /**
     * @return the creator
     */
    public User getCreator() {
        return creator;
    }


    /**
     * @param creator the creator to set
     */
    public void setCreator(User creator) {
        this.creator = creator;
    }


    /**
     * @return the generator
     */
    public Campaign getGenerator() {
        return generator;
    }


    /**
     * @param generator the generator to set
     */
    public void setGenerator(Campaign generator) {
        this.generator = generator;
    }


    /**
     * @return the body
     */
    public String getBody() {
        return body;
    }


    /**
     * @param body the body to set
     */
    public void setBody(String body) {
        this.body = body;
    }
    
    public IResource getBodyAsResource() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        IResource resource = mapper.readValue(this.body, TextualResource.class);
        return resource;
    }


    /**
     * @return the target
     */
    public String getTarget() {
        return target;
    }


    /**
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

    public IResource getTargetAsResource() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        IResource resource; 
        if(this.target.contains("SpecificResource")) {
            resource = mapper.readValue(this.target, SpecificResource.class);     
        } else {
            resource = mapper.readValue(this.target, TypedResource.class);   
        }
        return resource;
    }

    

    
    
}
