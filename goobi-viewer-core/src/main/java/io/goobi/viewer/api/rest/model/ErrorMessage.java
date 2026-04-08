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

import io.goobi.viewer.exceptions.RestApiException;

/**
 * REST API response model that carries an HTTP status code, a human-readable error description, and an optional stack trace.
 * Implements {@link IResponseMessage} and is serialized as JSON for error responses.
 *
 * @author Florian Alpers
 */
public class ErrorMessage implements IResponseMessage {

    private final int status;
    private final String message;
    private final String stackTrace;

    public ErrorMessage() {
        status = 0;
        message = null;
        stackTrace = null;
    }

    /**
     * Creates a new ErrorMessage instance.
     *
     * @param status HTTP status code of the error response
     * @param message human-readable error description
     */
    public ErrorMessage(int status, String message) {
        super();
        this.status = status;
        this.message = message;
        this.stackTrace = null;
    }

    /**
     * Creates a new ErrorMessage instance.
     *
     * @param status HTTP status code of the error response
     * @param message human-readable error description
     * @param stackTrace stack trace string for diagnostic purposes
     */
    public ErrorMessage(int status, String message, String stackTrace) {
        super();
        this.status = status;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    /**
     * Creates a new ErrorMessage instance.
     *
     * @param exception REST API exception carrying status code and message
     */
    public ErrorMessage(RestApiException exception) {
        this.status = exception.getStatusCode();
        this.message = exception.getMessage();
        // Stack traces must never be sent to clients; log server-side if needed instead.
        this.stackTrace = null;
    }

    /**
     * Getter for the field <code>status</code>.
     *
     * @return the HTTP status code of this error response
     */
    public int getStatus() {
        return status;
    }

    /**
     * Getter for the field <code>message</code>.
     *
     * @return the human-readable error description
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * Getter for the field <code>stackTrace</code>.
     *
     * @return the stack trace string for diagnostic purposes, or null if not set
     */
    public String getStackTrace() {
        return stackTrace;
    }

}
