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

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.goobi.viewer.model.security.user.User;

/**
 * Simplified representation of a {@link User} object for json (de-)serialization.
 *
 * @author Florian Alpers
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
     * @param user the user to create the facade from
     * @param request current HTTP request for resolving avatar URL
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
     * @param user the user to create the facade from
     */
    public UserJsonFacade(User user) {
        this(user, null);
    }

    /**
     *
     * @param orig the instance to copy all fields from
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
     * @param userId database ID of the user
     * @param name display name of the user
     * @param avatar URL of the user's avatar image
     * @param score gamification score of the user
     * @param active whether the user account is active
     * @param suspended whether the user account is suspended
     * @param anonymous whether the user is an anonymous guest
     * @param superuser whether the user has superuser privileges
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
     * @param name display name for the anonymous user facade
     */
    public UserJsonFacade(String name) {
        this(null, name, null, 0, false, false, true, false);
    }

    /**

     */
    public Long getUserId() {
        return userId;
    }

    /**

     */
    public String getName() {
        return name;
    }

    /**

     */
    public String getAvatar() {
        return avatar;
    }

    /**

     */
    public long getScore() {
        return score;
    }

    /**

     */
    public boolean isActive() {
        return active;
    }

    /**

     */
    public boolean isSuspended() {
        return suspended;
    }

    /**

     */
    public boolean isAnonymous() {
        return anonymous;
    }

    /**

     */
    public boolean isSuperuser() {
        return superuser;
    }

    @Override
    public String toString() {
        return this.name + " ID:" + this.userId + " avatar:" + this.avatar;
    }
}
