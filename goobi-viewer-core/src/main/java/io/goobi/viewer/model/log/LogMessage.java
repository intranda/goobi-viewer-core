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
package io.goobi.viewer.model.log;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.goobi.viewer.api.rest.model.UserJsonFacade;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.user.User;

/**
 * @author florian
 *
 */
@MappedSuperclass
public class LogMessage implements Serializable, Comparable<LogMessage> {

    private static final Logger logger = LogManager.getLogger(LogMessage.class);
    private static final long serialVersionUID = -7884550362212875027L;
    /**
     * Constant for creator meaning that no creator was set
     */
    private static final UserJsonFacade UNASSIGNED = new UserJsonFacade("unknown");
    /**
     * Constant for creator meaning the creator could not be found in the database
     */
    private static final UserJsonFacade ANONYMOUS = new UserJsonFacade("anonymous");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "creator_id", nullable = true)
    private Long creatorId;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    @Column(name = "message", nullable = true, columnDefinition = "LONGTEXT")
    private String message;

    @Transient
    private UserJsonFacade creator = UNASSIGNED;

    /**
     * 
     * @param message
     * @param creatorId
     * @param request
     */
    public LogMessage(String message, Long creatorId, HttpServletRequest request) {
        this.message = message;
        this.creatorId = creatorId;
        this.dateCreated = LocalDateTime.now();
        this.loadCreator(request);
    }

    /**
     * 
     * @param message
     * @param creatorId
     * @param dateCreated
     * @param request
     */
    public LogMessage(String message, Long creatorId, LocalDateTime dateCreated, HttpServletRequest request) {
        this(message, creatorId, request);
        this.dateCreated = dateCreated;
    }

    /**
     * 
     * @param source
     * @param request
     */
    public LogMessage(LogMessage source, HttpServletRequest request) {
        this.message = source.message;
        this.creatorId = source.creatorId;
        this.dateCreated = source.dateCreated;
        this.creator = source.creator;
        if (creator == UNASSIGNED) {
            this.loadCreator(request);
        }
        if (this.creatorId == null && this.creator.getUserId() != null) {
            this.creatorId = this.creator.getUserId();
        }
        this.id = source.id;
    }

    /**
     * 
     * @param source
     */
    public LogMessage(LogMessage source) {
        this(source, null);
    }

    public LogMessage() {
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
    @JsonIgnore
    public Long getCreatorId() {
        return creatorId;
    }

    /**
     * @return the dateCreated
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the creator
     */
    public UserJsonFacade getCreator() {
        return creator;
    }

    /**
     * Set the value of {@link #creator} from the value of {@link #creatorId}. If creatorId is null or an exception occurs while retrieving the user
     * data, the creator is set to {@link #UNASSIGNED}. If no creator could be found by the given id, the creator is set to {@link #ANONYMOUS} A
     * {@link HttpServletRequest request} may be passed to create an absolute URL for the creator avatar.
     * 
     * @param request
     */
    private void loadCreator(HttpServletRequest request) {
        if (this.creatorId == null) {
            this.creator = UNASSIGNED;
        } else {
            try {
                User user = DataManager.getInstance().getDao().getUser(this.creatorId);
                if (user == null) {
                    this.creator = ANONYMOUS;
                } else {
                    this.creator = new UserJsonFacade(user, request);
                }
            } catch (DAOException e) {
                logger.error("Error loading user with id {}", this.creatorId, e);
                this.creator = UNASSIGNED;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(LogMessage o) {
        return this.dateCreated.compareTo(o.dateCreated);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.message + " (" + (this.creator == null ? ("ID:" + this.creatorId) : this.creator.getName()) + " - " + this.dateCreated;
    }

}
