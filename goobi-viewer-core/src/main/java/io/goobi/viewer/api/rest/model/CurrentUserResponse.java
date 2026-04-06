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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Response object for GET /api/v1/users/current.
 * Contains the client IP address and, if the user is logged in, user information.
 * The {@code user} field is omitted from JSON output when null (i.e. not logged in).
 */
@JsonInclude(Include.NON_NULL)
public class CurrentUserResponse {

    private final String ip;
    private final UserJsonFacade user;

    public CurrentUserResponse(String ip, UserJsonFacade user) {
        this.ip = ip;
        this.user = user;
    }

    public String getIp() {
        return ip;
    }

    public UserJsonFacade getUser() {
        return user;
    }
}
