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
package de.intranda.digiverso.presentation.messages;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;

import de.intranda.digiverso.presentation.controller.Helper;

public class Messages {

    public static void error(String targetId, String message) {
        showMessage(targetId, message, FacesMessage.SEVERITY_ERROR);
    }

    public static void error(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_ERROR);
    }

    public static void info(String targetId, String message) {
        showMessage(targetId, message, FacesMessage.SEVERITY_INFO);
    }

    public static void info(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_INFO);
    }

    public static void warn(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_WARN);
    }

    public static void warn(String targetId, String message) {
        showMessage(targetId, message, FacesMessage.SEVERITY_WARN);
    }

    public static void clear() {
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.getMessageList().clear();
    }

    private static void showMessage(String targetId, String inMessage, Severity inSeverity) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return;
        }
        String translatedMessage = Helper.getTranslation(inMessage, null);
        FacesMessage fm = new FacesMessage(translatedMessage);
        fm.setSeverity(inSeverity);
        // remove duplicate
        fm.setDetail("");
        fc.addMessage(targetId, fm);
    }

}
