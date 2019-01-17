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
package de.intranda.digiverso.presentation.model.iiif.discovery;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;

/**
 * A JSON-serializable object to represent an "activity" as described in https://iiif.io/api/discovery/0.1/#activities
 * 
 * @author Florian Alpers
 *
 */
public class Activity {

    protected static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    
    private ActivityType type;
    private IPresentationModelElement object;
    private Date endTime;
    /**
     * The type of this Activity. One of UPDATE, CREATE and DELETE
     * 
     * @return the type
     */
    public ActivityType getType() {
        return type;
    }
    /**
     * Sets the type of this Activity. One of UPDATE, CREATE and DELETE
     * 
     * @param type the type to set
     */
    public void setType(ActivityType type) {
        this.type = type;
    }
    /**
     * Get the document changed by this Activity in the form of a IIIF manifest
     * 
     * @return the object
     */
    public IPresentationModelElement getObject() {
        return object;
    }
    /**
     * Set the document changed by this Activity in the form of a IIIF manifest
     * 
     * @param object the object to set
     */
    public void setObject(IPresentationModelElement object) {
        this.object = object;
    }
    /**
     * The update time
     * 
     * @return the update time
     */
    @JsonFormat(pattern = DATETIME_FORMAT)
    public Date getEndTime() {
        return endTime;
    }
    /**
     * Set the update time
     * 
     * @param endTime the updat time
     */
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    
    
}
