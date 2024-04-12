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

import java.io.PrintWriter;
import java.io.StringWriter;

import io.goobi.viewer.exceptions.RestApiException;

/**
 * <p>
 * ErrorMessage class.
 * </p>
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
     * <p>
     * Constructor for ErrorMessage.
     * </p>
     *
     * @param status a int.
     * @param message a {@link java.lang.String} object.
     */
    public ErrorMessage(int status, String message) {
        super();
        this.status = status;
        this.message = message;
        this.stackTrace = null;
    }

    /**
     * <p>
     * Constructor for ErrorMessage.
     * </p>
     *
     * @param status a int.
     * @param message a {@link java.lang.String} object.
     * @param stackTrace a {@link java.lang.String} object.
     */
    public ErrorMessage(int status, String message, String stackTrace) {
        super();
        this.status = status;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    /**
     * <p>
     * Constructor for ErrorMessage.
     * </p>
     *
     * @param exception a {@link io.goobi.viewer.exceptions.RestApiException} object.
     */
    public ErrorMessage(RestApiException exception) {
        this.status = exception.getStatusCode();
        this.message = exception.getMessage();
        this.stackTrace = getStackTrace(exception);
    }

    /**
     * <p>
     * Getter for the field <code>status</code>.
     * </p>
     *
     * @return the status
     */
    public int getStatus() {
        return status;
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

    /**
     * <p>
     * Getter for the field <code>stackTrace</code>.
     * </p>
     *
     * @return the stackTrace
     */
    public String getStackTrace() {
        return stackTrace;
    }

    private String getStackTrace(RestApiException exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        exception.printStackTrace(writer);
        String st = stringWriter.toString();
        return st;
    }
}
