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
package io.goobi.viewer.messages;

import java.util.Locale;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.FacesMessage.Severity;
import jakarta.faces.context.FacesContext;

/**
 * Utility class for looking up localised message strings from the viewer's resource bundles.
 */
public final class Messages {

    /**
     * Private constructor.
     */
    private Messages() {
        //
    }

    /**
     * error.
     *
     * @param targetId JSF component ID to attach the message to
     * @param message message key or text to display
     * @param messageParams placeholder values substituted into the message
     */
    public static void error(String targetId, String message, String... messageParams) {
        showMessage(targetId, message, FacesMessage.SEVERITY_ERROR, messageParams);
    }

    /**
     * error.
     *
     * @param message message key or text to display
     */
    public static void error(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_ERROR);
    }

    /**
     * info.
     *
     * @param targetId JSF component ID to attach the message to
     * @param message message key or text to display
     * @param messageParams placeholder values substituted into the message
     */
    public static void info(String targetId, String message, String... messageParams) {
        showMessage(targetId, message, FacesMessage.SEVERITY_INFO, messageParams);
    }

    /**
     * info.
     *
     * @param message message key or text to display
     */
    public static void info(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_INFO);
    }

    /**
     * warn.
     *
     * @param message message key or text to display
     */
    public static void warn(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_WARN);
    }

    /**
     * warn.
     *
     * @param targetId JSF component ID to attach the message to
     * @param message message key or text to display
     * @param messageParams placeholder values substituted into the message
     */
    public static void warn(String targetId, String message, String... messageParams) {
        showMessage(targetId, message, FacesMessage.SEVERITY_WARN, messageParams);
    }

    /**
     * clear.
     */
    public static void clear() {
        FacesContext fc = FacesContext.getCurrentInstance();
        fc.getMessageList().clear();
    }

    private static void showMessage(String targetId, String inMessage, Severity inSeverity, String... messageParams) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null) {
            return;
        }
        String translatedMessage = translate(inMessage, null, messageParams);
        FacesMessage fm = new FacesMessage(translatedMessage);
        fm.setSeverity(inSeverity);
        // remove duplicate
        fm.setDetail("");
        fc.addMessage(targetId, fm);
    }

    /**
     * translate.
     *
     * @param inMessage message key to look up in the resource bundle
     * @param locale locale used for translation lookup
     * @param messageParams placeholder values substituted into the translated message
     * @return translated and interpolated message string
     */
    public static String translate(String inMessage, Locale locale, String... messageParams) {
        String translatedMessage = ViewerResourceBundle.getTranslation(inMessage, locale);
        for (int i = 0; i < messageParams.length; i++) {
            //two replacements to handle both placeholders with and without numbers
            translatedMessage = translatedMessage.replace("{" + i + "}", messageParams[i]);
            translatedMessage = translatedMessage.replaceFirst("\\{\\}", messageParams[i]);
        }
        return translatedMessage;
    }

}
