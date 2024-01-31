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

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.FacesContext;

/**
 * <p>
 * Messages class.
 * </p>
 */
public class Messages {

    /**
     * Private constructor.
     */
    private Messages() {
        //
    }

    /**
     * <p>
     * error.
     * </p>
     *
     * @param targetId a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param messageParams a {@link java.lang.String} object.
     */
    public static void error(String targetId, String message, String... messageParams) {
        showMessage(targetId, message, FacesMessage.SEVERITY_ERROR, messageParams);
    }

    /**
     * <p>
     * error.
     * </p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public static void error(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_ERROR);
    }

    /**
     * <p>
     * info.
     * </p>
     *
     * @param targetId a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param messageParams a {@link java.lang.String} object.
     */
    public static void info(String targetId, String message, String... messageParams) {
        showMessage(targetId, message, FacesMessage.SEVERITY_INFO, messageParams);
    }

    /**
     * <p>
     * info.
     * </p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public static void info(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_INFO);
    }

    /**
     * <p>
     * warn.
     * </p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public static void warn(String message) {
        showMessage(null, message, FacesMessage.SEVERITY_WARN);
    }

    /**
     * <p>
     * warn.
     * </p>
     *
     * @param targetId a {@link java.lang.String} object.
     * @param message a {@link java.lang.String} object.
     * @param messageParams a {@link java.lang.String} object.
     */
    public static void warn(String targetId, String message, String... messageParams) {
        showMessage(targetId, message, FacesMessage.SEVERITY_WARN, messageParams);
    }

    /**
     * <p>
     * clear.
     * </p>
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
     * <p>
     * translate.
     * </p>
     *
     * @param inMessage a {@link java.lang.String} object.
     * @param messageParams a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
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
