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

/**
 * SuccessMessage class.
 *
 * @author Florian Alpers
 */
public class SuccessMessage implements IResponseMessage {

    private final boolean success;
    private final String message;

    /**
     * Creates a new SuccessMessage instance.
     *
     * @param success true if the operation succeeded
     * @param message human-readable result description
     */
    public SuccessMessage(boolean success, String message) {
        super();
        this.success = success;
        this.message = message;
    }

    /**
     * Creates a new SuccessMessage instance.
     *
     * @param success true if the operation succeeded
     */
    public SuccessMessage(boolean success) {
        super();
        this.success = success;
        this.message = "";
    }

    /**
     * isSuccess.
     *
     * @return true if the operation succeeded, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Getter for the field <code>message</code>.
     *
     * @return the human-readable result description
     */
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (success ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SuccessMessage other = (SuccessMessage) obj;

        return success == other.success;
    }

}
