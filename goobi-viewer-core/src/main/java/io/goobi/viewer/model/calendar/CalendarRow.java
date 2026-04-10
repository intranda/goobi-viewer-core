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
package io.goobi.viewer.model.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a row of calendar items (e.g. a week row within a month view) for display in the calendar widget.
 */
public class CalendarRow implements Serializable {

    private static final long serialVersionUID = 1669202746505522856L;

    private List<ICalendarItem> itemList = new ArrayList<>();

    private boolean selected = false;

    /**
     * Getter for the field <code>itemList</code>.
     *
     * @return a list of calendar items contained in this row
     */
    public List<ICalendarItem> getItemList() {
        return itemList;
    }

    /**
     * Setter for the field <code>itemList</code>.
     *
     * @param itemList calendar items to set for this row
     */
    public void setItemList(List<ICalendarItem> itemList) {
        this.itemList = itemList;
    }

    /**
     * addItem.
     *
     * @param item calendar item to append to this row
     */
    public void addItem(ICalendarItem item) {
        itemList.add(item);
    }

    /**
     * isSelected.
     *
     * @return true if this calendar row is currently selected, false otherwise
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Setter for the field <code>selected</code>.
     *
     * @param selected true to mark this row as selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
