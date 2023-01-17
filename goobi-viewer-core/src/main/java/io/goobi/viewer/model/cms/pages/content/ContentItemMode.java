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
package io.goobi.viewer.model.cms.pages.content;

import org.apache.commons.lang3.StringUtils;

/**
 * Optional property of a ContentItem within a template Allows additional control about which fields are shown in the cms-backend for this item. The
 * value is set in the template xml like <content> <item type="expanded">...</item> </content> If no value is set, "simple" is assumed
 *
 * @author Florian Alpers
 */
public enum ContentItemMode {
    simple,
    expanded,
    paginated;

    /**
     * <p>
     * get.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.pages.content.ContentItemMode} object.
     */
    public static ContentItemMode get(String name) {
        if (StringUtils.isBlank(name)) {
            return simple;
        }
        try {
            return ContentItemMode.valueOf(name.toLowerCase());
        } catch (IllegalArgumentException e) {
            return simple;
        }
    }
}
