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
package io.goobi.viewer.model.toc.export.pdf;

/**
 * WriteTocException class.
 */
public class WriteTocException extends Exception {

    private static final long serialVersionUID = -4454192342277070479L;

    /**
     * Creates a new WriteTocException instance.
     */
    public WriteTocException() {
        super();
    }

    /**
     * Creates a new WriteTocException instance.
     *
     * @param message human-readable error description
     * @param cause underlying exception that triggered this error
     * @param enableSuppression whether suppression is enabled
     * @param writableStackTrace whether the stack trace is writable
     */
    public WriteTocException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Creates a new WriteTocException instance.
     *
     * @param message human-readable error description
     * @param cause underlying exception that triggered this error
     */
    public WriteTocException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new WriteTocException instance.
     *
     * @param message human-readable error description
     */
    public WriteTocException(String message) {
        super(message);
    }

    /**
     * Creates a new WriteTocException instance.
     *
     * @param cause underlying exception that triggered this error
     */
    public WriteTocException(Throwable cause) {
        super(cause);
    }
}
