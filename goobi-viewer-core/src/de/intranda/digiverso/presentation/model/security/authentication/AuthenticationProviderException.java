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
package de.intranda.digiverso.presentation.model.security.authentication;

/**
 * An exception indicating that an error occured when trying to authenticate a user.
 * This should only be used if an internal error occured or the request could not be processed.
 * Ordinary login refusal should not be handled with exceptions
 * 
 * @author Florian Alpers
 *
 */
public class AuthenticationProviderException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 2454421697805524935L;

    /**
     * 
     */
    public AuthenticationProviderException() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     * @param arg3
     */
    public AuthenticationProviderException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
        super(arg0, arg1, arg2, arg3);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     * @param arg1
     */
    public AuthenticationProviderException(String arg0, Throwable arg1) {
        super(arg0, arg1);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public AuthenticationProviderException(String arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param arg0
     */
    public AuthenticationProviderException(Throwable arg0) {
        super(arg0);
        // TODO Auto-generated constructor stub
    }
    
    

}
