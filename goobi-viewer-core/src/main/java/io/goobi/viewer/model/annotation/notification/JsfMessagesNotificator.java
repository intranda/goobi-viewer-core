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
package io.goobi.viewer.model.annotation.notification;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.messages.Messages;

/**
 * @author florian
 *
 */
public class JsfMessagesNotificator implements ChangeNotificator {

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyCreation(de.intranda.api.annotation.wa.WebAnnotation)
     */
    @Override
    public void notifyCreation(WebAnnotation annotation) {
        Messages.info(null, "Successfully created comment '{}'", annotation.getBody().toString());
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyEdit(de.intranda.api.annotation.wa.WebAnnotation, de.intranda.api.annotation.wa.WebAnnotation)
     */
    @Override
    public void notifyEdit(WebAnnotation oldAnnotation, WebAnnotation newAnnotation) {
        Messages.info(null, "Successfully changed comment '{}' to '{}'", oldAnnotation.getBody().toString(), newAnnotation.getBody().toString());

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyDeletion(de.intranda.api.annotation.wa.WebAnnotation)
     */
    @Override
    public void notifyDeletion(WebAnnotation annotation) {
        Messages.info(null, "Successfully deleted comment '{}'", annotation.getBody().toString());
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.notification.ChangeNotificator#notifyError(java.lang.Exception)
     */
    @Override
    public void notifyError(Exception exception) {
        Messages.error("Error changing notification: " + exception.getMessage().toString());
    }

}
