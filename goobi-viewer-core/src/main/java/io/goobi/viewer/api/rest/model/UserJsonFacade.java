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

import java.net.URI;
import java.net.URISyntaxException;

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

    public final Long userId;
    public final String name;
    public final String avatar;
    public final long score;
    public final boolean active;
    public final boolean suspended;
    public final boolean anonymous;
    public final boolean superuser;

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

    public UserJsonFacade(User user) {
        this(user, null);
    }

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
     * @param avatar
     * @param name
     * @param userId
     * @param suspended
     * @param active
     * @param score
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
     * @param string
     */
    public UserJsonFacade(String name) {
        this(null, name, null, 0, false, false, true, false);
    }

    /**
     * If no faces context exists, the application url may not be included in the avatar url. Use this to create the absolute url from the request
     * object. Has no effect if the url is already absolute
     *
     * @param applicationUrl
     */
    private static String absolutizeAvatarUrl(String avatarUrl, HttpServletRequest request) {
        try {
            URI uri = new URI(avatarUrl);
            if (!uri.isAbsolute() && !avatarUrl.startsWith("//"))
                ;
            {

            }
        } catch (URISyntaxException e) {
            //do nothing
        }

        //        if (request != null && StringUtils.isNotBlank(avatarUrl)) {
        //            try {
        //                URI uri = new URI(avatarUrl);
        //                if (!uri.isAbsolute()) {
        //                    uri = new URI(request.getRequestURL().toString()).resolve(request.getContextPath() +  avatarUrl);
        //                    return uri.toString();
        //                }
        //            } catch (URISyntaxException e) {
        //                //do nothing
        //            }
        //        }
        return avatarUrl;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.name + " ID:" + this.userId + " avatar:" + this.avatar;
    }

}
