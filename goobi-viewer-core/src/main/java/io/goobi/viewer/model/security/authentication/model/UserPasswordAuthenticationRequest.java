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
 * <p>UserPasswordAuthenticationRequest class.</p>
 *
 * @author Florian Alpers
 */
public class UserPasswordAuthenticationRequest {

    protected String username;
    protected String password;
    
    /**
     * <p>Constructor for XServiceAuthenticationRequest.</p>
     */
    public UserPasswordAuthenticationRequest() {
    }
    
    /**
     * <p>Constructor for XServiceAuthenticationRequest.</p>
     *
     * @param username a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     */
    public UserPasswordAuthenticationRequest(String username, String password) {
        super();
        this.username = username;
        this.password = password;
    }
    /**
     * <p>Getter for the field <code>username</code>.</p>
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    /**
     * <p>Getter for the field <code>password</code>.</p>
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * <p>Setter for the field <code>username</code>.</p>
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * <p>Setter for the field <code>password</code>.</p>
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
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
