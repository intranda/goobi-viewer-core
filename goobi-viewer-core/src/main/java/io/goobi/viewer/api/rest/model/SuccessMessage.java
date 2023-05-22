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
 * <p>
 * SuccessMessage class.
 * </p>
 *
 * @author Florian Alpers
 */
public class SuccessMessage implements IResponseMessage {

    private final boolean success;
    private final String message;

    /**
     * <p>
     * Constructor for SuccessMessage.
     * </p>
     *
     * @param success a boolean.
     * @param message a {@link java.lang.String} object.
     */
    public SuccessMessage(boolean success, String message) {
        super();
        this.success = success;
        this.message = message;
    }

    /**
     * <p>
     * Constructor for SuccessMessage.
     * </p>
     *
     * @param success a boolean.
     */
    public SuccessMessage(boolean success) {
        super();
        this.success = success;
        this.message = "";
    }

    /**
     * <p>
     * isSuccess.
     * </p>
     *
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * <p>
     * Getter for the field <code>message</code>.
     * </p>
     *
     * @return the message
     */
    @Override
    public String getMessage() {
        return message;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (success ? 1231 : 1237);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
