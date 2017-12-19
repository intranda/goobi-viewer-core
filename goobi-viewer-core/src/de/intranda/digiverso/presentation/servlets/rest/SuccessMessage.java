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
package de.intranda.digiverso.presentation.servlets.rest;

/**
 * @author Florian Alpers
 *
 */
public class SuccessMessage {

    private final boolean success;
    private final String message;
    /**
     * @param success
     * @param message
     */
    public SuccessMessage(boolean success, String message) {
        super();
        this.success = success;
        this.message = message;
    }
    public SuccessMessage(boolean success) {
        super();
        this.success = success;
        this.message = "";
    }
    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }
    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Two success messages are equal, of their success properties are equal
     */
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj.getClass().equals(this.getClass())) {
            return this.success == ((SuccessMessage)obj).success;
        } else {
            return false;
        }
    }
    
}
