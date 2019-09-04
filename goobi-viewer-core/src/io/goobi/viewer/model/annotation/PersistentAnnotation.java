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
package io.goobi.viewer.model.annotation;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.Motivation;
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

    private static final String URI_ID_TEMPLATE = DataManager.getInstance().getConfiguration().getRestApiUrl() + "annotations/{id}";
    private static final String URI_ID_REGEX = ".*/annotations/(\\d+)/?$";
    private static final String TARGET_REGEX = ".*/iiif/manifests/(\\w+)/(?:canvas|manifest)?(?:/(\\d+))?/?$";


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
    
    @Column(name = "motivation")
    private String motivation;
    
    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;
    
    @ManyToOne
    @JoinColumn(name = "generator_id")
    private Campaign generator;
    
    @Column(name = "body", columnDefinition="LONGTEXT")
    private String body;
    
    @Column(name = "target", columnDefinition="LONGTEXT")
    private String target;
    
    @Column(name = "target_pi")
    private String targetPI;
    
    @Column(name = "target_page")
    private Integer targetPageOrder;

    public PersistentAnnotation() {
    }
    
    public PersistentAnnotation(WebAnnotation source) {
        this.dateCreated = source.getCreated();
        this.dateModified = source.getModified();
        this.motivation = source.getMotivation();
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
        this.targetPI = parsePI(source.getTarget().getId());
        this.targetPageOrder = parsePageOrder(source.getTarget().getId());

    }


    /**
     * @param id2
     * @return
     */
    public static String parsePI(URI uri) {
        Matcher matcher = Pattern.compile(TARGET_REGEX).matcher(uri.toString());
        if(matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
    
    public static Integer parsePageOrder(URI uri) {
        Matcher matcher = Pattern.compile(TARGET_REGEX).matcher(uri.toString());
        if(matcher.find()) {
            return Integer.parseInt(matcher.group(2));
        } else {
            return null;
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

    public static Long getId(URI idAsURI) {
        Matcher matcher = Pattern.compile(URI_ID_REGEX).matcher(idAsURI.toString());
        if(matcher.find()) {
            String idString = matcher.group(1);
            return Long.parseLong(idString);
        } else {
            return null;
        }
    }
    
    public URI getIdAsURI() {
        return URI.create(URI_ID_TEMPLATE.replace("{id}", this.getId().toString()));
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
    
    /**
     * @return the motivation
     */
    public String getMotivation() {
        return motivation;
    }
    
    /**
     * @param motivation the motivation to set
     */
    public void setMotivation(String motivation) {
        this.motivation = motivation;
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
     * @return the targetPI
     */
    public String getTargetPI() {
        return targetPI;
    }
    
    /**
     * @return the targetPageOrder
     */
    public Integer getTargetPageOrder() {
        return targetPageOrder;
    }
    
    /**
     * @param targetPI the targetPI to set
     */
    public void setTargetPI(String targetPI) {
        this.targetPI = targetPI;
    }
    
    /**
     * @param targetPageOrder the targetPageOrder to set
     */
    public void setTargetPageOrder(Integer targetPageOrder) {
        this.targetPageOrder = targetPageOrder;
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

    public WebAnnotation getAsAnnotation() throws JsonParseException, JsonMappingException, IOException {
        WebAnnotation annotation = new WebAnnotation(getIdAsURI());
        annotation.setCreated(this.dateCreated);
        annotation.setModified(this.dateModified);
        if(getCreator() != null) {            
            annotation.setCreator(new Agent(getCreator().getIdAsURI(), AgentType.PERSON, getCreator().getDisplayName()));
        }
        if(getGenerator() != null) {            
            annotation.setGenerator(new Agent(getGenerator().getIdAsURI(), AgentType.SOFTWARE, getGenerator().getTitle()));
        }
        annotation.setBody(this.getBodyAsResource());
        annotation.setTarget(this.getTargetAsResource());
        annotation.setMotivation(this.getMotivation());
        
        return annotation;   
    }

    
    
}
