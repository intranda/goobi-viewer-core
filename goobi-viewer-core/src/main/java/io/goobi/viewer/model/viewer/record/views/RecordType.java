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
package io.goobi.viewer.model.viewer.record.views;

import io.goobi.viewer.model.viewer.ViewManager;

/**
 * An enum of different types of records pertaining to the display-conditions of page elements
 */
public enum RecordType {

    RECORD,
    ANCHOR,
    GROUP;

    public static RecordType fromViewManager(ViewManager vm) {
        if (vm.getTopStructElement().isAnchor()) {
            return ANCHOR;
        } else if (vm.getTopStructElement().isGroup()) {
            return GROUP;
        } else {
            return RECORD;
        }
    }

}
