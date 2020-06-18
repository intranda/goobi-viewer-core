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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.ISelector;
import de.intranda.api.annotation.oa.OpenAnnotation;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.FragmentSelector;
import de.intranda.api.annotation.wa.SpecificResource;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.TypedResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.user.User;

/**
 * An Annotation class to store annotation in a database
 *
 * @author florian
 */
@Entity
@Table(name = "annotations")
public class PersistentAnnotation {

    private static final Logger logger = LoggerFactory.getLogger(PersistentAnnotation.class);

    private static final String URI_ID_TEMPLATE = DataManager.getInstance().getConfiguration().getRestApiUrl() + "annotations/{id}";
    private static final String URI_ID_REGEX = ".*/annotations/(\\d+)/?$";
    private static final String TARGET_REGEX = ".*/iiif/manifests/(.+?)/(?:canvas|manifest)?(?:/(\\d+))?/?$";

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

    /**
     * This is the id of the {@link User} who created the annotation. If it is null, either the annotation wasn't created by a logged in user, or this
     * information is withheld for privacy reasons
     */
    @Column(name = "creator_id")
    private Long creatorId;

    /**
     * This is the id of the {@link User} who reviewed the annotation. May be null if this annotation wasn't reviewed (yet)
     */
    @Column(name = "reviewer_id")
    private Long reviewerId;

    /**
     * This is the id of the {@link Question} this annotation was created with. If it is null, the annotation was generated outside the campaign
     * framework
     */
    @Column(name = "generator_id")
    private Long generatorId;

    /**
     * JSON representation of the annotation body as String
     */
    @Column(name = "body", columnDefinition = "LONGTEXT")
    private String body;

    /**
     * JSON representation of the annotation target as String
     */
    @Column(name = "target", columnDefinition = "LONGTEXT")
    private String target;

    @Column(name = "target_pi")
    private String targetPI;

    @Column(name = "target_page")
    private Integer targetPageOrder;

    /**
     * empty constructor
     */
    public PersistentAnnotation() {
    }

    /**
     * creates a new PersistentAnnotation from a WebAnnotation
     *
     * @param source a {@link de.intranda.api.annotation.wa.WebAnnotation} object.
     */
    public PersistentAnnotation(WebAnnotation source) {
        this.dateCreated = source.getCreated();
        this.dateModified = source.getModified();
        this.motivation = source.getMotivation();
        this.id = source.getId() != null ? getId(source.getId()) : null;
        this.creatorId = null;
        this.generatorId = null;
        try {
            if (source.getCreator() != null && source.getCreator().getId() != null) {
                Long userId = User.getId(source.getCreator().getId());
                if (userId != null) {
                    this.creatorId = userId;
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Error getting creator of " + source, e);
        }
        try {
            if (source.getGenerator() != null && source.getGenerator().getId() != null) {
                Long questionId = Question.getQuestionId(source.getGenerator().getId());
                Long campaignId = Question.getCampaignId(source.getGenerator().getId());
                this.generatorId = questionId;
            }
        } catch (NumberFormatException e) {
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
     * Get the PI of the annotation target from its URI id
     *
     * @param uri a {@link java.net.URI} object.
     * @return a {@link java.lang.String} object.
     */
    public static String parsePI(URI uri) {
        Matcher matcher = Pattern.compile(TARGET_REGEX).matcher(uri.toString());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extract the page order from a canvas url. If the url points to a manifest, return null
     *
     * @param uri a {@link java.net.URI} object.
     * @return a {@link java.lang.Integer} object.
     */
    public static Integer parsePageOrder(URI uri) {
        Matcher matcher = Pattern.compile(TARGET_REGEX).matcher(uri.toString());
        if (matcher.find()) {
            String pageNo = matcher.group(2);
            if (StringUtils.isNotBlank(pageNo)) {
                return Integer.parseInt(pageNo);
            }
            return null;
        }
        return null;
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @param idAsURI a {@link java.net.URI} object.
     * @return a {@link java.lang.Long} object.
     */
    public static Long getId(URI idAsURI) {
        Matcher matcher = Pattern.compile(URI_ID_REGEX).matcher(idAsURI.toString());
        if (matcher.find()) {
            String idString = matcher.group(1);
            return Long.parseLong(idString);
        }
        return null;
    }

    /**
     * <p>
     * getIdAsURI.
     * </p>
     *
     * @return a {@link java.net.URI} object.
     */
    public URI getIdAsURI() {
        return URI.create(URI_ID_TEMPLATE.replace("{id}", this.getId().toString()));
    }

    /**
     * <p>
     * getIdAsURI.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @return a {@link java.net.URI} object.
     */
    public static URI getIdAsURI(String id) {
        return URI.create(URI_ID_TEMPLATE.replace("{id}", id));
    }

    /**
     * <p>
     * Getter for the field <code>dateCreated</code>.
     * </p>
     *
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * <p>
     * Setter for the field <code>dateCreated</code>.
     * </p>
     *
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * <p>
     * Getter for the field <code>dateModified</code>.
     * </p>
     *
     * @return the dateModified
     */
    public Date getDateModified() {
        return dateModified;
    }

    /**
     * <p>
     * Setter for the field <code>dateModified</code>.
     * </p>
     *
     * @param dateModified the dateModified to set
     */
    public void setDateModified(Date dateModified) {
        this.dateModified = dateModified;
    }

    /**
     * <p>
     * getCreator.
     * </p>
     *
     * @return the creator
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getCreator() throws DAOException {
        if (getCreatorId() != null) {
            return DataManager.getInstance().getDao().getUser(getCreatorId());
        }
        return null;
    }

    /**
     * <p>
     * setCreator.
     * </p>
     *
     * @param creator the creator to set
     */
    public void setCreator(User creator) {
        this.creatorId = creator.getId();
    }

    /**
     * <p>
     * getGenerator.
     * </p>
     *
     * @return the generator
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Question getGenerator() throws DAOException {
        if (getGeneratorId() != null) {
            return DataManager.getInstance().getDao().getQuestion(getGeneratorId());
        }
        return null;
    }

    /**
     * <p>
     * setGenerator.
     * </p>
     *
     * @param generator the generator to set
     */
    public void setGenerator(Question generator) {
        this.generatorId = generator.getId();
    }

    /**
     * <p>
     * Getter for the field <code>creatorId</code>.
     * </p>
     *
     * @return the creatorId
     */
    public Long getCreatorId() {
        return creatorId;
    }

    /**
     * <p>
     * Setter for the field <code>creatorId</code>.
     * </p>
     *
     * @param creatorId the creatorId to set
     */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    /**
     * <p>
     * Getter for the field <code>reviewerId</code>.
     * </p>
     *
     * @return the reviewerId
     */
    public Long getReviewerId() {
        return reviewerId;
    }

    /**
     * <p>
     * Setter for the field <code>reviewerId</code>.
     * </p>
     *
     * @param reviewerId the reviewerId to set
     */
    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    /**
     * <p>
     * Getter for the field <code>generatorId</code>.
     * </p>
     *
     * @return the generatorId
     */
    public Long getGeneratorId() {
        return generatorId;
    }

    /**
     * <p>
     * Setter for the field <code>generatorId</code>.
     * </p>
     *
     * @param generatorId the generatorId to set
     */
    public void setGeneratorId(Long generatorId) {
        this.generatorId = generatorId;
    }

    /**
     * <p>
     * Getter for the field <code>body</code>.
     * </p>
     *
     * @return the body
     */
    public String getBody() {
        return body;
    }

    /**
     * <p>
     * Setter for the field <code>body</code>.
     * </p>
     *
     * @param body the body to set
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * <p>
     * Getter for the field <code>motivation</code>.
     * </p>
     *
     * @return the motivation
     */
    public String getMotivation() {
        return motivation;
    }

    /**
     * <p>
     * Setter for the field <code>motivation</code>.
     * </p>
     *
     * @param motivation the motivation to set
     */
    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    /**
     * Get the
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getBodyAsResource() throws JsonParseException, JsonMappingException, IOException {
        if (this.body != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            IResource resource = mapper.readValue(this.body, TextualResource.class);
            return resource;
        }
        return null;
    }

    /**
     * <p>
     * getBodyAsOAResource.
     * </p>
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getBodyAsOAResource() throws JsonParseException, JsonMappingException, IOException {
        TextualResource resource = (TextualResource) getBodyAsResource();
        if (resource != null) {
            IResource oaResource = new de.intranda.api.annotation.oa.TextualResource(resource.getText());
            return oaResource;
        }
        return null;
    }

    /**
     * <p>
     * Getter for the field <code>target</code>.
     * </p>
     *
     * @return the target
     */
    public String getTarget() {
        return target;
    }

    /**
     * <p>
     * Getter for the field <code>targetPI</code>.
     * </p>
     *
     * @return the targetPI
     */
    public String getTargetPI() {
        return targetPI;
    }

    /**
     * <p>
     * Getter for the field <code>targetPageOrder</code>.
     * </p>
     *
     * @return the targetPageOrder
     */
    public Integer getTargetPageOrder() {
        return targetPageOrder;
    }

    /**
     * <p>
     * Setter for the field <code>targetPI</code>.
     * </p>
     *
     * @param targetPI the targetPI to set
     */
    public void setTargetPI(String targetPI) {
        this.targetPI = targetPI;
    }

    /**
     * <p>
     * Setter for the field <code>targetPageOrder</code>.
     * </p>
     *
     * @param targetPageOrder the targetPageOrder to set
     */
    public void setTargetPageOrder(Integer targetPageOrder) {
        this.targetPageOrder = targetPageOrder;
    }

    /**
     * <p>
     * Setter for the field <code>target</code>.
     * </p>
     *
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Get the annotation target as an WebAnnotation {@link de.intranda.api.annotation.IResource} java object
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getTargetAsResource() throws JsonParseException, JsonMappingException, IOException {
        if (this.target != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            IResource resource;
            if (this.target.contains("SpecificResource")) {
                resource = mapper.readValue(this.target, SpecificResource.class);
            } else {
                resource = mapper.readValue(this.target, TypedResource.class);
            }
            return resource;
        }
        return null;
    }

    /**
     * Get the annotation target as an OpenAnnotation {@link de.intranda.api.annotation.IResource} java object
     *
     * @return a {@link de.intranda.api.annotation.IResource} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public IResource getTargetAsOAResource() throws JsonParseException, JsonMappingException, IOException {
        IResource resource = getTargetAsResource();
        if (resource != null) {
            if (resource instanceof SpecificResource && ((SpecificResource) resource).getSelector() instanceof FragmentSelector) {
                FragmentSelector selector = (FragmentSelector) ((SpecificResource) resource).getSelector();
                ISelector oaSelector = new de.intranda.api.annotation.oa.FragmentSelector(selector.getFragment());
                IResource oaResource = new de.intranda.api.annotation.oa.SpecificResource(resource.getId(), oaSelector);
                return oaResource;
            }
            return resource;
        }
        return null;

    }

    /**
     * Get the annotation as an {@link de.intranda.api.annotation.wa.WebAnnotation} java object
     *
     * @return a {@link de.intranda.api.annotation.wa.WebAnnotation} object.
     */
    public WebAnnotation getAsAnnotation() {
        WebAnnotation annotation = new WebAnnotation(getIdAsURI());
        annotation.setCreated(this.dateCreated);
        annotation.setModified(this.dateModified);
        try {
            if (getCreator() != null) {
                annotation.setCreator(new Agent(getCreator().getIdAsURI(), AgentType.PERSON, getCreator().getDisplayName()));
            }
            if (getGenerator() != null) {
                annotation.setGenerator(new Agent(getGenerator().getIdAsURI(), AgentType.SOFTWARE, getGenerator().getOwner().getTitle()));
            }
        } catch (DAOException e) {
            logger.error("unable to set creator and generator for annotation", e);
        }
        try {
            annotation.setBody(this.getBodyAsResource());
            annotation.setTarget(this.getTargetAsResource());
        } catch (IOException e) {
            logger.error("unable to parse body or target for annotation", e);

        }
        annotation.setMotivation(this.getMotivation());

        return annotation;
    }

    /**
     * Get the annotation as an {@link de.intranda.api.annotation.oa.OpenAnnotation} java object
     *
     * @return a {@link de.intranda.api.annotation.oa.OpenAnnotation} object.
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     */
    public OpenAnnotation getAsOpenAnnotation() {
        OpenAnnotation annotation = new OpenAnnotation(getIdAsURI());
        try {
            annotation.setBody(this.getBodyAsOAResource());
            annotation.setTarget(this.getTargetAsOAResource());
            annotation.setMotivation(this.getMotivation());
        } catch (IOException e) {
            logger.error("unable to parse body or target for annotation", e);
        }
        
        return annotation;
    }

    /**
     * Deletes exported JSON annotations from a related record's data folder. Should be called when deleting this annotation.
     *
     * @return Number of deleted files
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int deleteExportedTextFiles() throws ViewerConfigurationException {
        if (DataManager.getInstance().getConfiguration().getAnnotationFolder() == null) {
            throw new ViewerConfigurationException("annotationFolder is not configured");
        }

        int count = 0;
        try {
            Set<Path> filesToDelete = new HashSet<>();
            Path annotationFolder = DataFileTools.getDataFolder(targetPI, DataManager.getInstance().getConfiguration().getAnnotationFolder());
            logger.trace("Annotation folder path: {}", annotationFolder.toAbsolutePath().toString());
            if (!Files.isDirectory(annotationFolder)) {
                logger.trace("Annotation folder not found - nothing to delete");
                return 0;
            }

            {
                Path file = Paths.get(annotationFolder.toAbsolutePath().toString(), targetPI + "_" + id + ".json");
                if (Files.isRegularFile(file)) {
                    filesToDelete.add(file);
                }
            }
            if (!filesToDelete.isEmpty()) {
                for (Path file : filesToDelete) {
                    try {
                        Files.delete(file);
                        count++;
                        logger.info("Annotation file deleted: {}", file.getFileName().toString());
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                }
            }

            // Delete folder if empty
            try {
                if (FileTools.isFolderEmpty(annotationFolder)) {
                    Files.delete(annotationFolder);
                    logger.info("Empty annotation folder deleted: {}", annotationFolder.toAbsolutePath());
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        } catch (PresentationException e) {
            logger.error(e.getMessage(), e);
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
        }

        return count;
    }

    /**
     * <p>
     * getContentString.
     * </p>
     *
     * @return Just the string value of the body document
     * @throws com.fasterxml.jackson.core.JsonParseException if any.
     * @throws com.fasterxml.jackson.databind.JsonMappingException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getContentString() throws JsonParseException, JsonMappingException, IOException, DAOException {
        // Value
        WebAnnotation wa = getAsAnnotation();
        if (wa.getBody() instanceof TextualResource) {
            return ((TextualResource) wa.getBody()).getText();
        }

        return body;
    }

    /**
     * <p>
     * getTargetLink.
     * </p>
     *
     * @return URL string to the record view
     */
    public String getTargetLink() {
        String ret = "/" + targetPI + "/";
        if (targetPageOrder != null) {
            ret += targetPageOrder + "/";
        } else {
            ret += "1/";
        }

        return ret;
    }
}
