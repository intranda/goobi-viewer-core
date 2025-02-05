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
package io.goobi.viewer.model.annotation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.AbstractAnnotation;
import de.intranda.api.annotation.ITypedResource;
import de.intranda.api.annotation.oa.TextualResource;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Transient;

/**
 * @author florian
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class PersistentAnnotation {
    private static final Logger logger = LogManager.getLogger(PersistentAnnotation.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annotation_id")
    private Long id;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "date_modified")
    private LocalDateTime dateModified;

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

    @Column(name = "access_condition", nullable = true)
    private String accessCondition;

    @Column(name = "publication_status")
    @Enumerated(EnumType.STRING)
    private PublicationStatus publicationStatus = PublicationStatus.CREATING;

    @Transient
    private User creator = null;

    /**
     * empty constructor
     */
    protected PersistentAnnotation() {
        this.dateCreated = LocalDateTime.now();
    }

    /**
     * 
     * @param source
     */
    protected PersistentAnnotation(PersistentAnnotation source) {
        this.id = source.id;
        this.accessCondition = source.accessCondition;
        this.body = source.body;
        this.creatorId = source.creatorId;
        this.dateCreated = source.dateCreated;
        this.dateModified = source.dateModified;
        this.generatorId = source.getGeneratorId();
        this.motivation = source.motivation;
        this.reviewerId = source.reviewerId;
        this.target = source.target;
        this.targetPI = source.targetPI;
        this.targetPageOrder = source.targetPageOrder;
        this.publicationStatus = source.publicationStatus;
    }

    /**
     * creates a new PersistentAnnotation from a WebAnnotation
     *
     * @param source a {@link de.intranda.api.annotation.wa.WebAnnotation} object.
     * @param id
     * @param targetPI
     * @param targetPage
     */
    protected PersistentAnnotation(AbstractAnnotation source, Long id, String targetPI, Integer targetPage) {
        this.dateCreated = source.getCreated();
        this.dateModified = source.getModified();
        this.motivation = source.getMotivation();
        this.id = id;
        this.creatorId = null;
        this.generatorId = null;
        this.accessCondition = source.getRights();
        try {
            if (source.getCreator() != null && source.getCreator().getId() != null) {
                Long userId = User.getId(source.getCreator().getId());
                if (userId != null) {
                    this.creatorId = userId;
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Error getting creator of {}", source, e);
        }
        try {
            if (source.getGenerator() != null && source.getGenerator().getId() != null) {
                Long questionId = Question.getQuestionId(source.getGenerator().getId());
                Long campaignId = Question.getCampaignId(source.getGenerator().getId());
                this.generatorId = questionId;
            }
        } catch (NumberFormatException e) {
            logger.error("Error getting generator of {}", source, e);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            this.body = mapper.writeValueAsString(source.getBody());
        } catch (JsonProcessingException e) {
            logger.error("Error writing body {} to string.", source.getBody(), e);
        }
        try {
            this.target = mapper.writeValueAsString(source.getTarget());
        } catch (JsonProcessingException e) {
            logger.error("Error writing body {} to string.", source.getBody(), e);
        }
        this.targetPI = targetPI;
        this.targetPageOrder = targetPage;

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
     * Getter for the field <code>dateCreated</code>.
     * </p>
     *
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * <p>
     * Setter for the field <code>dateCreated</code>.
     * </p>
     *
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * <p>
     * Getter for the field <code>dateModified</code>.
     * </p>
     *
     * @return the dateModified
     */
    public LocalDateTime getDateModified() {
        return dateModified;
    }

    /**
     * <p>
     * Setter for the field <code>dateModified</code>.
     * </p>
     *
     * @param dateModified the dateModified to set
     */
    public void setDateModified(LocalDateTime dateModified) {
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
        if (this.creator == null && this.creatorId != null) {
            this.creator = DataManager.getInstance().getDao().getUser(creatorId);
        }
        return this.creator;
    }

    public Optional<User> getCreatorIfPresent() {
        try {
            return Optional.ofNullable(getCreator());
        } catch (DAOException e) {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * setCreator.
     * </p>
     *
     * @param creator the creator to set
     */
    public void setCreator(User creator) {
        this.creator = creator;
        if (creator != null) {
            this.creatorId = creator.getId();
        }
    }

    /**
     * <p>
     * getReviewer.
     * </p>
     *
     * @return the reviewer
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User getReviewer() throws DAOException {
        if (reviewerId != null) {
            return DataManager.getInstance().getDao().getUser(reviewerId);
        }
        return null;
    }

    /**
     * <p>
     * setReviewer.
     * </p>
     *
     * @param reviewer the reviewer to set
     */
    public void setReviewer(User reviewer) {
        if (reviewer != null) {
            this.reviewerId = reviewer.getId();
        }
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
        this.generatorId = Optional.ofNullable(generator).map(Question::getId).orElse(null);
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
            logger.debug("Annotation folder path: {}", annotationFolder.toAbsolutePath().toString());
            if (!Files.isDirectory(annotationFolder)) {
                logger.debug("Annotation folder not found - nothing to delete");
                return 0;
            }

            Path file = Paths.get(annotationFolder.toAbsolutePath().toString(), targetPI + "_" + id + ".json");
            if (Files.isRegularFile(file)) {
                filesToDelete.add(file);
            }

            if (!filesToDelete.isEmpty()) {
                for (Path p : filesToDelete) {
                    try {
                        Files.delete(p);
                        count++;
                        logger.info("Annotation file deleted: {}", p.getFileName().toString());
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
        } catch (IndexUnreachableException | PresentationException e) {
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
    public String getContentString() {

        if (StringUtils.isNotBlank(body)) {
            try {
                ITypedResource resource = new ObjectMapper().readValue(this.body, ITypedResource.class);
                if (resource instanceof TextualResource) {
                    return ((TextualResource) resource).getText();
                } else if (resource instanceof de.intranda.api.annotation.wa.TextualResource) {
                    return ((de.intranda.api.annotation.wa.TextualResource) resource).getText();
                }

            } catch (JsonProcessingException e) {
                logger.trace("Error reading body as json value:'{}'. Error message is '{}'", body, e.toString());
                return body;
            }
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

    /**
     * @return the accessCondition
     */
    public String getAccessCondition() {
        return accessCondition;
    }

    /**
     * @param accessCondition the accessCondition to set
     */
    public void setAccessCondition(String accessCondition) {
        this.accessCondition = accessCondition;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Crowdsourcing Annotation");
        sb.append("\n\t").append("Body:").append(getBody());
        sb.append("\n\t").append("Target:").append(getTarget());
        sb.append("\n\t").append("GeneratorId:").append(getGeneratorId());
        sb.append("\n\t").append("CreatorId:").append(getCreatorId());
        sb.append("\n\t").append("ReviewerId:").append(getReviewerId());

        return sb.toString();

    }

    public String getDisplayDate(LocalDateTime date) {
        return DateTools.format(date, DateTools.FORMATTERDEDATETIME, false);
    }

    /**
     * Checks whether the user with the given ID is allowed to edit this comment (i.e. the annotation belongs to this (proper) user.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return true if allowed; false otherwise
     * @should return true if use id equals owner id
     * @should return false if owner id is null
     * @should return false if user is null
     */
    public boolean mayEdit(User user) {
        return this.creatorId != null && user != null && this.creatorId.equals(user.getId());
    }

    /**
     * @return the publicationStatus
     */
    public PublicationStatus getPublicationStatus() {
        return publicationStatus;
    }

    /**
     * @param publicationStatus the publicationStatus to set
     */
    public void setPublicationStatus(PublicationStatus publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return 0;
        }
        
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            PersistentAnnotation other = (PersistentAnnotation) obj;
            return Objects.equals(this.body, other.body)
                    && Objects.equals(this.creatorId, other.creatorId)
                    && Objects.equals(this.generatorId, other.generatorId)
                    && Objects.equals(this.motivation, other.motivation)
                    && Objects.equals(this.accessCondition, other.accessCondition)
                    && Objects.equals(this.dateModified, other.dateModified)
                    && Objects.equals(this.target, other.target)
                    && Objects.equals(this.targetPageOrder, other.targetPageOrder)
                    && Objects.equals(this.targetPI, other.targetPI);
        }
        
        return false;
    }
}
