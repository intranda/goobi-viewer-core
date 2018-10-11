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
package de.intranda.digiverso.presentation.model.security.authentication.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author Florian Alpers
 *
 */
@JsonInclude(Include.NON_EMPTY)
public class VuAuthenticationResponse {

    private User user;
    private Expired expired;
    private Blocks blocks;
    private Request request;
    
    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }
    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }
    /**
     * @return the expired
     */
    public Expired getExpired() {
        return expired;
    }
    /**
     * @param expired the expired to set
     */
    public void setExpired(Expired expired) {
        this.expired = expired;
    }
    /**
     * @return the blocks
     */
    public Blocks getBlocks() {
        return blocks;
    }
    /**
     * @param blocks the blocks to set
     */
    public void setBlocks(Blocks blocks) {
        this.blocks = blocks;
    }
    /**
     * @return the request
     */
    public Request getRequest() {
        return request;
    }
    /**
     * @param request the request to set
     */
    public void setRequest(Request request) {
        this.request = request;
    }
    
    @JsonInclude(Include.NON_EMPTY)
    public static class User {
        
        private Boolean isValid;
        private Boolean exists;
        private String group;
        
        @JsonDeserialize(using=BooleanDeserializer.class)
        public void setIsValid(Boolean valid) {
            this.isValid = valid;
        }
        @JsonDeserialize(using=BooleanDeserializer.class)
        public void setExists(Boolean exists) {
            this.exists = exists;
        }
        @JsonSerialize(using=BooleanSerializer.class)
        public Boolean getIsValid() {
            return this.isValid;
        }
        @JsonSerialize(using=BooleanSerializer.class)
        public Boolean getExists() {
            return this.exists;
        }
        public void setGroup(String group) {
            this.group = group;
        }
        public String getGroup() {
            return group;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "valid: " + getIsValid() + ", exists: " + getExists() + ", group: " + getGroup();
        }
    }
    
    @JsonInclude(Include.NON_EMPTY)
    public static class Expired {
        private Boolean isExpired;
        private Date date;
        
        @JsonDeserialize(using=BooleanDeserializer.class)
        public void setIsExpired(Boolean expired) {
            this.isExpired = expired;
        }
        @JsonSerialize(using=BooleanSerializer.class)
        public Boolean getIsExpired() {
            return this.isExpired;
        }
        public Date getDate() {
            return date;
        }
        public void setDate(Date date) {
            this.date = date;
        }
    }
    
    @JsonInclude(Include.NON_EMPTY)
    public static class Date {
        private String timestamp;
        private String formatted;

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
        public String getTimestamp() {
            return timestamp;
        }
        public void setFormatted(String formatted) {
            this.formatted = formatted;
        }
        public String getFormatted() {
            return formatted;
        }
    }
    
    @JsonInclude(Include.NON_EMPTY)
    public static class Blocks {
        private Boolean isBlocked;
        private List<Reason> reasons = new ArrayList<>();
        
        @JsonSerialize(using=BooleanSerializer.class)
        public Boolean getIsBlocked() {
            return isBlocked;
        }
        @JsonDeserialize(using=BooleanDeserializer.class)
        public void setIsBlocked(Boolean blocked) {
            this.isBlocked = blocked;
        }
        public void setReasons(List<Reason> reasons) {
            this.reasons = reasons;
        }
        public List<Reason> getReasons() {
            return reasons;
        }
    }
    
    @JsonInclude(Include.NON_EMPTY)
    public static class Reason {
        private String code;
        private String note;
        
        public String getCode() {
            return code;
        }
        public void setCode(String code) {
            this.code = code;
        }
        public String getNote() {
            return note;
        }
        public void setNote(String note) {
            this.note = note;
        }
    }
    
    @JsonInclude(Include.NON_EMPTY)
    public static class Request {
        private Boolean hasError;
        private String errorMsg;
        
        @JsonSerialize(using=BooleanSerializer.class)
        public Boolean getHasError() {
            return hasError;
        }
        @JsonDeserialize(using=BooleanDeserializer.class)
        public void setHasError(Boolean hasError) {
            this.hasError = hasError;
        }
        public String getErrorMsg() {
            return errorMsg;
        }
        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getUser().toString() + "\n\tExpired: " + Boolean.TRUE.equals(getExpired().getIsExpired()) + "\n\tBlocked: " + Boolean.TRUE.equals(getBlocks().getIsBlocked());
    }
}
