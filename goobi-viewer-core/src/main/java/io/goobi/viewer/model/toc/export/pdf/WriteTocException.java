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
package io.goobi.viewer.model.toc.export.pdf;

/**
 * <p>WriteTocException class.</p>
 */
public class WriteTocException extends Exception {

    private static final long serialVersionUID = -4454192342277070479L;

    /**
     * <p>Constructor for WriteTocException.</p>
     */
    public WriteTocException() {
        super();
    }

    /**
     * <p>Constructor for WriteTocException.</p>
     *
     * @param message a {@link java.lang.String} object.
     * @param cause a {@link java.lang.Throwable} object.
     * @param enableSuppression a boolean.
     * @param writableStackTrace a boolean.
     */
    public WriteTocException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * <p>Constructor for WriteTocException.</p>
     *
     * @param message a {@link java.lang.String} object.
     * @param cause a {@link java.lang.Throwable} object.
     */
    public WriteTocException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * <p>Constructor for WriteTocException.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public WriteTocException(String message) {
        super(message);
    }

    /**
     * <p>Constructor for WriteTocException.</p>
     *
     * @param cause a {@link java.lang.Throwable} object.
     */
    public WriteTocException(Throwable cause) {
        super(cause);
    }
}
