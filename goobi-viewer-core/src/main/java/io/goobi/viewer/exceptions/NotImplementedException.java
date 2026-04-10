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

/**
 * Thrown to indicate that a method or operation is not yet implemented.
 *
 * @author Florian Alpers
 */
public class NotImplementedException extends Exception {

    /**
     * Creates a new NotImplementedException instance.
     */
    public NotImplementedException() {
        super();
    }

    /**
     * Creates a new NotImplementedException instance.
     *
     * @param arg0 human-readable error message
     * @param arg1 underlying cause of this exception
     * @param arg2 whether suppression is enabled
     * @param arg3 whether the stack trace is writable
     */
    public NotImplementedException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }

    /**
     * Creates a new NotImplementedException instance.
     *
     * @param arg0 human-readable error message
     * @param arg1 underlying cause of this exception
     */
    public NotImplementedException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * Creates a new NotImplementedException instance.
     *
     * @param arg0 human-readable error message
     */
    public NotImplementedException(String arg0) {
        super(arg0);
    }

    /**
     * Creates a new NotImplementedException instance.
     *
     * @param arg0 underlying cause of this exception
     */
    public NotImplementedException(Throwable arg0) {
        super(arg0);
    }

}
