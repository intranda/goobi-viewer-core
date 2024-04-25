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
package io.goobi.viewer.controller.config.filter;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * How the filter condition of a {@link EntityFilter} is applied. {@link #SHOW} means the filter passes objects meeting its condition, {@link #HIDE}
 * means it blocks objects meeting its condition
 */
public enum FilterAction {
    /**
     * Block objects meeting a filter's condition
     */
    HIDE,
    /**
     * Pass objects meeting a filter's condition
     */
    SHOW;

    public static Optional<FilterAction> getAction(String value) {
        if (StringUtils.isNotBlank(value)) {
            return Optional.ofNullable(valueOf(value.toUpperCase()));
        } else {
            return Optional.empty();
        }
    }
}
