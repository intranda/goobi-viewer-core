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
package io.goobi.viewer.model.security.authentication.model;

/**
 * <p>
 * XServiceAuthenticationRequest class.
 * </p>
 *
 */
public class BibliothecaAuthenticationRequest extends UserPasswordAuthenticationRequest {

    /**
     * <p>
     * Constructor for BibliothecaAuthenticationRequest.
     * </p>
     *
     * @param username a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     */
    public BibliothecaAuthenticationRequest(String username, String password) {
        super(normalizeUsername(username), password);
    }

    /**
     * 
     * @param username
     * @return 11-digit representation of the given user name.
     * @should normalize value correctly
     */
    static String normalizeUsername(String username) {
        if (username == null || username.length() == 11) {
            return username;
        }

        if (username.length() < 11) {
            StringBuilder sb = new StringBuilder(11);
            for (int i = username.length(); i < 11; ++i) {
                sb.append('0');
            }
            sb.append(username);
            return sb.toString();
        }

        return username;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "username: " + username + ", password: " + password;
    }

}
