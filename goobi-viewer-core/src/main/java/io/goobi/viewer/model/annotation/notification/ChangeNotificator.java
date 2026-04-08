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
package io.goobi.viewer.model.annotation.notification;

import java.util.Locale;

import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author Florian Alpers
 */
public interface ChangeNotificator {

    /**
     *
     * @param annotation the newly created annotation to notify about
     * @param locale locale used to format the notification message
     * @param viewerRootUrl base URL of the viewer application
     */
    public void notifyCreation(PersistentAnnotation annotation, Locale locale, String viewerRootUrl);

    /**
     *
     * @param oldAnnotation the annotation state before the edit
     * @param newAnnotation the annotation state after the edit
     * @param locale locale used to format the notification message
     * @param viewerRootUrl base URL of the viewer application
     */
    public void notifyEdit(PersistentAnnotation oldAnnotation, PersistentAnnotation newAnnotation, Locale locale, String viewerRootUrl);

    /**
     *
     * @param annotation the deleted annotation to notify about
     * @param locale locale used to format the notification message
     */
    public void notifyDeletion(PersistentAnnotation annotation, Locale locale);

    /**
     *
     * @param exception the exception that caused the error
     * @param locale locale used to format the notification message
     */
    public void notifyError(Exception exception, Locale locale);
}
