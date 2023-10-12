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
 * Representation of the Littera authentication response which is delivered as xml. It only contains the single information if a login attempt
 * succeeded or not
 *
 * @author florian
 */
public class LitteraAuthenticationResponse {

    private boolean authenticationSuccessful;

    /**
     * <p>
     * Constructor for LitteraAuthenticationResponse.
     * </p>
     */
    public LitteraAuthenticationResponse() {
    }

    /**
     * <p>
     * Constructor for LitteraAuthenticationResponse.
     * </p>
     *
     * @param success a boolean.
     */
    public LitteraAuthenticationResponse(boolean success) {
        this.authenticationSuccessful = success;
    }

    /**
     * <p>
     * isAuthenticationSuccessful.
     * </p>
     *
     * @return the authenticationSuccessful
     */
    public boolean isAuthenticationSuccessful() {
        return authenticationSuccessful;
    }

    /**
     * <p>
     * Setter for the field <code>authenticationSuccessful</code>.
     * </p>
     *
     * @param authenticationSuccessful the authenticationSuccessful to set
     */
    public void setAuthenticationSuccessful(boolean authenticationSuccessful) {
        this.authenticationSuccessful = authenticationSuccessful;
    }
}
