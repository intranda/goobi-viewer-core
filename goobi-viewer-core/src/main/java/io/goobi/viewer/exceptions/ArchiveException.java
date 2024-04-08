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

public abstract class ArchiveException extends PresentationException {

    private static final long serialVersionUID = -6999584810651228875L;

    /**
     * 
     * @param message
     * @param resourceName
     * @param resourceLocation
     * @param e
     */
    protected ArchiveException(String message, String resourceName, String resourceLocation, Throwable e) {
        super(createMessage(message, resourceName, resourceLocation), e);
    }

    /**
     * 
     * @param message
     * @param resourceName
     * @param resourceLocation
     */
    protected ArchiveException(String message, String resourceName, String resourceLocation) {
        super(createMessage(message, resourceName, resourceLocation));
    }

    /**
     * 
     * @param message
     * @param replacements
     * @return Updated message
     */
    protected static String createMessage(String message, String... replacements) {
        int i = 0;
        String msg = message;
        while (msg.contains("{}") && i < replacements.length) {
            msg = msg.replaceFirst("\\{\\}", replacements[i]);
            i++;
        }
        return msg;
    }
}
