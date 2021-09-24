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
package io.goobi.viewer.model.annotation;

/**
 * @author florian
 *
 */
public enum PublicationStatus {
    CREATING, //The annotation has been created but is not ready for review or publication yet
    REVIEW, //The annotation needs to be approved of before being published
    PUBLISHED, //The annotation may be viewed by anyone meeting the access conditions
    PRIVATE, //The annotation is hidden from anyone except the creator
    SUSPENDED, //The annotation is temporarily disabled
    DELETED, //The annotation should remain disabled
}
