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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ReportErrorsErrorHandler implements ErrorHandler {

    private List<XMLError> errors;

    public ReportErrorsErrorHandler() {
        errors = new ArrayList<>();
    }

    /**
     * 
     * @param e
     * @param severity
     */
    private void addError(SAXParseException e, String severity) {
        errors.add(new XMLError(e.getLineNumber(), e.getColumnNumber(), severity, e.getMessage()));
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        addError(exception, "ERROR");
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        addError(exception, "FATAL");
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        addError(exception, "WARNING");
    }

    /**
     * @return the errors
     */
    public List<XMLError> getErrors() {
        return errors;
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(List<XMLError> errors) {
        this.errors = errors;
    }
}