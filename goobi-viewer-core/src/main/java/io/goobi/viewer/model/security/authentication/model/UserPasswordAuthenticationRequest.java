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
package io.goobi.viewer.model.security.authentication.model;

/**
 * Request DTO carrying a username and password for local authentication.
 *
 * @author Florian Alpers
 */
public class UserPasswordAuthenticationRequest {

    protected String username;
    protected String password;

    /**
     * Creates a new XServiceAuthenticationRequest instance.
     */
    public UserPasswordAuthenticationRequest() {
    }

    /**
     * Creates a new XServiceAuthenticationRequest instance.
     *
     * @param username login name of the user
     * @param password plaintext password for authentication
     */
    public UserPasswordAuthenticationRequest(String username, String password) {
        super();
        this.username = username;
        this.password = password;
    }

    /**
     * Getter for the field <code>username</code>.
     *

     */
    public String getUsername() {
        return username;
    }

    /**
     * Getter for the field <code>password</code>.
     *

     */
    public String getPassword() {
        return password;
    }

    /**
     * Setter for the field <code>username</code>.
     *
     * @param username login name of the user to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Setter for the field <code>password</code>.
     *
     * @param password plaintext password for authentication to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "username: " + username + ", password: " + password;
    }

}
