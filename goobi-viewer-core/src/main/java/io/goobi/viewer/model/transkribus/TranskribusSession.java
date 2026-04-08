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
package io.goobi.viewer.model.transkribus;

import java.io.Serializable;

/**
 * Holds an authenticated Transkribus session token obtained after login.
 */
public class TranskribusSession implements Serializable {

    private static final long serialVersionUID = 3624185152470088153L;

    /** Internal system ID for the user account at Transkribus. */
    private final String userId;
    /** Login name (e-mail address). */
    private final String userName;
    /** Session ID, needed as an authentication token for subsequent API calls. */
    private final String sessionId;

    /**
     * Creates a new TranskribusSession instance.
     *
     * @param userId internal Transkribus user account ID.
     * @param userName login name (e-mail address) of the user.
     * @param sessionId authentication token for subsequent API calls.
     */
    public TranskribusSession(String userId, String userName, String sessionId) {
        this.userId = userId;
        this.userName = userName;
        this.sessionId = sessionId;
    }

    /**
     * Getter for the field <code>userId</code>.
     *

     */
    public String getUserId() {
        return userId;
    }

    /**
     * Getter for the field <code>userName</code>.
     *

     */
    public String getUserName() {
        return userName;
    }

    /**
     * Getter for the field <code>sessionId</code>.
     *

     */
    public String getSessionId() {
        return sessionId;
    }
}
