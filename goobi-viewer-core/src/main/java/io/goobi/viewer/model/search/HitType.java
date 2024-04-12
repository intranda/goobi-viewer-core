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
package io.goobi.viewer.model.search;

import java.util.Locale;

import io.goobi.viewer.messages.ViewerResourceBundle;

public enum HitType {
    ACCESSDENIED,
    DOCSTRCT,
    PAGE,
    METADATA, // grouped metadata
    UGC, // user-generated content
    LOCATION, //metadata type location
    SHAPE, //metadata type shape
    SUBJECT, //metadata type subject
    PERSON, // UGC/metadata person
    CORPORATION, // UGC/meadata corporation
    ADDRESS, // UGC address
    COMMENT, // UGC comment
    EVENT, // LIDO event
    GROUP, // convolute/series
    CMS; // CMS page type for search hits

    /**
     * 
     * @param name
     * @return {@link HitType} matching given name; null if none found
     * @should return all known types correctly
     * @should return null if name unknown
     */
    public static HitType getByName(String name) {
        if (name != null) {
            if ("OVERVIEWPAGE".equals(name)) {
                return HitType.CMS;
            }
            for (HitType type : HitType.values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
        }

        return null;
    }

    public String getLabel(Locale locale) {
        return ViewerResourceBundle.getTranslation(new StringBuilder("doctype_").append(name()).toString(), locale);
    }
}
