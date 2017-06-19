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
package de.intranda.digiverso.presentation.model.transkribus;

public class TranskribusSession {

    /** Internal system ID for the user account at Transkribus. */
    private final String userId;
    /** Login name (e-mail address). */
    private final String userName;
    /** Session ID, needed as an authentication token for subsequent API calls. */
    private final String sessionId;

    public TranskribusSession(String userId, String userName, String sessionId) {
        this.userId = userId;
        this.userName = userName;
        this.sessionId = sessionId;
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }
}
