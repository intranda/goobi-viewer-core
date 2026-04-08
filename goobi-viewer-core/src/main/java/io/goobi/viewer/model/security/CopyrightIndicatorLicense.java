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
package io.goobi.viewer.model.security;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a license entry for the copyright indicator widget, consisting of a human-readable description and a list of icon identifiers.
 */
public class CopyrightIndicatorLicense implements Serializable {

    private static final long serialVersionUID = -4163068583648929435L;

    private final String description;
    private final List<String> icons;

    /**
     * 
     * @param description human-readable license description
     * @param icons list of icon identifiers for this license
     */
    public CopyrightIndicatorLicense(String description, List<String> icons) {
        this.description = description;
        this.icons = icons;
    }

    /**

     */
    public String getDescription() {
        return description;
    }

    /**

     */
    public List<String> getIcons() {
        return icons;
    }

}
