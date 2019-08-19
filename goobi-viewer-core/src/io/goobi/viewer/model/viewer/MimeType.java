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
package io.goobi.viewer.model.viewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enum containing supported mime types.
 */
public enum MimeType {

    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    APPLICATION("application"),
    SANDBOXED_HTML("text"),
    OBJECT("object");

    private static final Logger logger = LoggerFactory.getLogger(MimeType.class);

    private final String name;

    /**
     * Constructor.
     * 
     * @should split full mime type names correctly
     */
    private MimeType(String name) {
        this.name = name;
    }

    /**
     * 
     * @param name
     * @return
     * @should find mime type by short name correctly
     * @should find mime type by full name correctly
     */
    public static MimeType getByName(String name) {
        if (name == null) {
            return null;
        }
        for (MimeType o : MimeType.values()) {
            if (o.getName().equals(name)) {
                return o;
            }
        }
        return null;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return true if image or PDF download is allowed for this mime type; false otherwise
     */
    public boolean isImageOrPdfDownloadAllowed() {
        switch (this) {
            case AUDIO:
            case VIDEO:
            case OBJECT:
                return false;
            default:
                return true;
        }
    }

    /**
     * 
     * @return true if image or PDF download is allowed for the given mime type name; false otherwise
     */
    public static boolean isImageOrPdfDownloadAllowed(String mimeTypeName) {
        MimeType mimeType = MimeType.getByName(mimeTypeName);
        if (mimeType == null) {
            return false;
        }

        return mimeType.isImageOrPdfDownloadAllowed();
    }
}
