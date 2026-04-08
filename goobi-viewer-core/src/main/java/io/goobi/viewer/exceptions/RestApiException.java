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
 * Thrown when an error occurs during REST API request processing, carrying an associated HTTP status code.
 *
 * @author Florian Alpers
 */
public class RestApiException extends Exception {

    private final int statusCode;

    /**
     * Creates a new RestApiException instance.
     *
     * @param statusCode HTTP status code of the failed API response
     */
    public RestApiException(int statusCode) {
        super();
        this.statusCode = statusCode;
    }

    /**
     * Creates a new RestApiException instance.
     *
     * @param message human-readable error description
     * @param cause underlying exception that triggered this error
     * @param statusCode HTTP status code of the failed API response
     */
    public RestApiException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public RestApiException(String message, Throwable cause, Status status) {
        this(message, cause, status.getStatusCode());
    }

    /**
     * Creates a new RestApiException instance.
     *
     * @param message human-readable error description
     * @param statusCode HTTP status code of the failed API response
     */
    public RestApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public RestApiException(String message, Status status) {
        this(message, status.getStatusCode());
    }

    /**
     * Creates a new RestApiException instance.
     *
     * @param cause underlying exception that triggered this error
     * @param statusCode HTTP status code of the failed API response
     */
    public RestApiException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    public RestApiException(Throwable cause, Status status) {
        this(cause, status.getStatusCode());
    }

    /**
     * Getter for the field <code>statusCode</code>.
     *
     * @return the HTTP status code associated with this REST API exception
     */
    public int getStatusCode() {
        return statusCode;
    }

    public Status getStatus() {
        return Status.fromStatusCode(statusCode);
    }

}
