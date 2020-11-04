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
package io.goobi.viewer.model.log;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Entity
public class LogMessage implements Serializable, Comparable<LogMessage> {

    private static final Logger logger = LoggerFactory.getLogger(LogMessage.class);
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
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created")
    private Date dateCreated;
    
    @Column(name = "message", nullable = true, columnDefinition = "LONGTEXT")
    private String message;
    
    @Transient
    private UserJsonFacade creator = UNASSIGNED;
    
    
    public LogMessage(String message, Long creatorId, HttpServletRequest request) {
        this.message = message;
        this.creatorId = creatorId;
        this.dateCreated = new Date();
        this.loadCreator(request);
    }
    
    public LogMessage(String message, Long creatorId, Date dateCreated, HttpServletRequest request) {
        this(message, creatorId, request);
        this.dateCreated = dateCreated;
    }
    
    public LogMessage(LogMessage source) {
        this.message = source.message;
        this.creatorId = source.creatorId;
        this.dateCreated = source.dateCreated;
        this.creator = source.creator;
        this.id = source.id;
    }

    public LogMessage() {
    }

    /**
     * @return the id
     */
    @JsonIgnore
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
    public Date getDateCreated() {
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
     * Set the value of {@link #creator} from the value of {@link #creatorId}. If creatorId is null or an exception occurs while retrieving
     * the user data, the creator is set to {@link #UNASSIGNED}. If no creator could be found by the given id, the creator is set 
     * to {@link #ANONYMOUS}
     */
    private void loadCreator(HttpServletRequest request) {
        if(this.creatorId == null) {
            this.creator = UNASSIGNED;
        } else {            
            try {
                User user = DataManager.getInstance().getDao().getUser(this.creatorId);
                this.creator = new UserJsonFacade(user, request);
                if(this.creator == null) {
                    this.creator = ANONYMOUS;
                }
            } catch (DAOException e) {
                logger.error("Error loading user with id " + this.creatorId, e);
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
        return this.message + " (" + (this.creator == null ? ("ID:" + this.creatorId) : this.creator.name)  + " - " + this.dateCreated;
    }
    
    
}
