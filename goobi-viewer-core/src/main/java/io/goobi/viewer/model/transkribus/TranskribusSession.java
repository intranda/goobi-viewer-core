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
package io.goobi.viewer.model.transkribus;

/**
 * <p>TranskribusSession class.</p>
 */
public class TranskribusSession {

    /** Internal system ID for the user account at Transkribus. */
    private final String userId;
    /** Login name (e-mail address). */
    private final String userName;
    /** Session ID, needed as an authentication token for subsequent API calls. */
    private final String sessionId;

    /**
     * <p>Constructor for TranskribusSession.</p>
     *
     * @param userId a {@link java.lang.String} object.
     * @param userName a {@link java.lang.String} object.
     * @param sessionId a {@link java.lang.String} object.
     */
    public TranskribusSession(String userId, String userName, String sessionId) {
        this.userId = userId;
        this.userName = userName;
        this.sessionId = sessionId;
    }

    /**
     * <p>Getter for the field <code>userId</code>.</p>
     *
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * <p>Getter for the field <code>userName</code>.</p>
     *
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * <p>Getter for the field <code>sessionId</code>.</p>
     *
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }
}
