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
package io.goobi.viewer.model.viewer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Enum containing supported mime types.
 */
public enum BaseMimeType {

    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    APPLICATION("application"),
    SANDBOXED_HTML("text"),
    MODEL("model"),
    UNKNOWN("");

    /** Constant <code>logger</code> */
    private static final Logger logger = LogManager.getLogger(BaseMimeType.class);

    private final String name;

    /**
     * Constructor.
     *
     * @param name
     * @should split full mime type names correctly
     */
    private BaseMimeType(String name) {
        this.name = name;
    }

    /**
     * <p>
     * getByName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.BaseMimeType} object.
     * @should find mime type by short name correctly
     * @should find mime type by full name correctly
     */
    public static BaseMimeType getByName(final String name) {
        if (name == null) {
            return UNKNOWN;
        }

        String useName = name;
        if (useName.contains("/")) {
            useName = useName.substring(0, useName.indexOf("/"));
        }
        // "object" deprecated
        if ("object".equals(useName)) {
            return MODEL;
        }
        for (BaseMimeType o : BaseMimeType.values()) {
            if (o.getName().equalsIgnoreCase(useName)) {
                return o;
            }
        }

        return UNKNOWN;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>
     * isImageOrPdfDownloadAllowed.
     * </p>
     *
     * @return true if image or PDF download is allowed for this mime type; false otherwise
     */
    public boolean isImageOrPdfDownloadAllowed() {
        switch (this) {
            case AUDIO, VIDEO, MODEL:
                return false;
            default:
                return true;
        }
    }

    /**
     * 
     * @return true for all types directly visible (or audible) in the viewer object view.
     */
    public boolean isMediaType() {
        switch (this) {
            case AUDIO, IMAGE, VIDEO, SANDBOXED_HTML, MODEL:
                return true;
            default:
                return false;
        }
    }

    /**
     * <p>
     * isImageOrPdfDownloadAllowed.
     * </p>
     *
     * @return true if image or PDF download is allowed for the given mime type name; false otherwise
     * @param mimeTypeName a {@link java.lang.String} object.
     */
    public static boolean isImageOrPdfDownloadAllowed(String mimeTypeName) {
        BaseMimeType mimeType = BaseMimeType.getByName(mimeTypeName);
        if (BaseMimeType.UNKNOWN.equals(mimeType)) {
            return false;
        }

        return mimeType.isImageOrPdfDownloadAllowed();
    }
}
