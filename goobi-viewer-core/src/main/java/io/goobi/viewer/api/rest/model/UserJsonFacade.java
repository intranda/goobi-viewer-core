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
package io.goobi.viewer.api.rest.model;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.goobi.viewer.model.security.user.User;

/**
 * Simplified representation of a {@link User} object for json (de-)serialization
 *
 * @author florian
 *
 */
@JsonInclude(Include.NON_NULL)
public class UserJsonFacade {

    private static final int AVATAR_IMAGE_SIZE = 150;

    private final Long userId;
    private final String name;
    private final String avatar;
    private final long score;
    private final boolean active;
    private final boolean suspended;
    private final boolean anonymous;
    private final boolean superuser;

    /**
     * 
     * @param user
     * @param request
     */
    public UserJsonFacade(User user, HttpServletRequest request) {
        this.name = user.getDisplayName();
        this.userId = user.getId();
        this.active = user.isActive();
        this.suspended = user.isSuspended();
        this.score = user.getScore();
        this.anonymous = user.isAnonymous();
        this.superuser = user.isSuperuser();
        this.avatar = user.getAvatarUrl(AVATAR_IMAGE_SIZE, request);
    }

    /**
     * 
     * @param user
     */
    public UserJsonFacade(User user) {
        this(user, null);
    }

    /**
     * 
     * @param orig
     */
    public UserJsonFacade(UserJsonFacade orig) {
        this.avatar = orig.avatar;
        this.name = orig.name;
        this.userId = orig.userId;
        this.active = orig.active;
        this.suspended = orig.suspended;
        this.score = orig.score;
        this.anonymous = orig.anonymous;
        this.superuser = orig.superuser;
    }

    /**
     * @param userId
     * @param name
     * @param avatar
     * @param score
     * @param active
     * @param suspended
     * @param anonymous
     * @param superuser
     */
    @JsonCreator
    public UserJsonFacade(
            @JsonProperty("userId") Long userId,
            @JsonProperty("name") String name,
            @JsonProperty("avatar") String avatar,
            @JsonProperty("score") long score,
            @JsonProperty("active") boolean active,
            @JsonProperty("suspended") boolean suspended,
            @JsonProperty("anonymous") boolean anonymous,
            @JsonProperty("superuser") boolean superuser) {
        this.avatar = avatar;
        this.name = name;
        this.userId = userId;
        this.suspended = suspended;
        this.active = active;
        this.score = score;
        this.anonymous = anonymous;
        this.superuser = superuser;
    }

    /**
     * @param name
     */
    public UserJsonFacade(String name) {
        this(null, name, null, 0, false, false, true, false);
    }

    /**
     * @return the userId
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the avatar
     */
    public String getAvatar() {
        return avatar;
    }

    /**
     * @return the score
     */
    public long getScore() {
        return score;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @return the suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * @return the anonymous
     */
    public boolean isAnonymous() {
        return anonymous;
    }

    /**
     * @return the superuser
     */
    public boolean isSuperuser() {
        return superuser;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name + " ID:" + this.userId + " avatar:" + this.avatar;
    }
}
