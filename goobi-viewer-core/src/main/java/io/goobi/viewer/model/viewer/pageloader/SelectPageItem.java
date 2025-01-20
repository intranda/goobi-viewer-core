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
package io.goobi.viewer.model.viewer.pageloader;

import jakarta.faces.model.SelectItem;

public class SelectPageItem extends SelectItem {

    private static final long serialVersionUID = -7384015705477411687L;

    private boolean doublePageMode = false;

    /**
     *
     */
    public SelectPageItem() {
        super();
    }

    public void setDoublePageMode(boolean doublePageMode) {
        this.doublePageMode = doublePageMode;
    }

    public boolean isDoublePageMode() {
        return doublePageMode;
    }

    @Override
    public Object getValue() {
        String value = super.getValue().toString();
        if (doublePageMode) {
            return value + "-" + value;
        }

        return value;
    }

}
