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
package io.goobi.viewer.model.xml;

public class XMLError {
    private final int line;
    private final int column;
    private final String severity;
    private final String message;

    /**
     * 
     * @param line line number where the error occurred
     * @param column column number where the error occurred
     * @param severity severity level of the error (e.g. error, warning)
     * @param message human-readable error message
     */
    public XMLError(int line, int column, String severity, String message) {
        this.line = line;
        this.column = column;
        this.severity = severity;
        this.message = message;
    }

    
    public int getLine() {
        return line;
    }

    
    public int getColumn() {
        return column;
    }

    
    public String getSeverity() {
        return severity;
    }

    
    public String getMessage() {
        return message;
    }
}
