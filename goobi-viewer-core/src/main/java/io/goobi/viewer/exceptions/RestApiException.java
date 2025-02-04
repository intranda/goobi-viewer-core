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
package io.goobi.viewer.exceptions;

import jakarta.ws.rs.core.Response.Status;

/**
 * <p>
 * RestApiException class.
 * </p>
 *
 * @author Florian Alpers
 */
public class RestApiException extends Exception {

    private final int statusCode;

    /**
     * <p>
     * Constructor for RestApiException.
     * </p>
     *
     * @param statusCode a int.
     */
    public RestApiException(int statusCode) {
        super();
        this.statusCode = statusCode;
    }

    /**
     * <p>
     * Constructor for RestApiException.
     * </p>
     *
     * @param message a {@link java.lang.String} object.
     * @param cause a {@link java.lang.Throwable} object.
     * @param statusCode a int.
     */
    public RestApiException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public RestApiException(String message, Throwable cause, Status status) {
        this(message, cause, status.getStatusCode());
    }

    /**
     * <p>
     * Constructor for RestApiException.
     * </p>
     *
     * @param message a {@link java.lang.String} object.
     * @param statusCode a int.
     */
    public RestApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public RestApiException(String message, Status status) {
        this(message, status.getStatusCode());
    }

    /**
     * <p>
     * Constructor for RestApiException.
     * </p>
     *
     * @param cause a {@link java.lang.Throwable} object.
     * @param statusCode a int.
     */
    public RestApiException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    public RestApiException(Throwable cause, Status status) {
        this(cause, status.getStatusCode());
    }

    /**
     * <p>
     * Getter for the field <code>statusCode</code>.
     * </p>
     *
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }

    public Status getStatus() {
        return Status.fromStatusCode(statusCode);
    }

}
