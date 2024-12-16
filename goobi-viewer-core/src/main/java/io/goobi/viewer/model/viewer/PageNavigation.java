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

import org.apache.commons.lang3.StringUtils;

/**
 * Describes different ways in which the sequence of images within a record is presented
 */
public enum PageNavigation {

    /**
     * Display a single image at a time, navigating to other images means loading a new page for that image
     */
    SINGLE,
    /**
     * Display two neighbouring images within the same page, navigation the different pairs of images means means loading a new page
     */
    DOUBLE,
    /**
     * Display all images of a record within the same page. Keeps track of the current image to update the page information accordingly
     */
    SEQUENCE;

    public static PageNavigation fromString(String string) {
        if (StringUtils.isNotBlank(string)) {
            return valueOf(string.toUpperCase());
        } else {
            return null;
        }
    }

}
