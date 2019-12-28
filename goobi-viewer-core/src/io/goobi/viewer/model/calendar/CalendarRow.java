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
package io.goobi.viewer.model.calendar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>CalendarRow class.</p>
 */
public class CalendarRow implements Serializable {

    private static final long serialVersionUID = 1669202746505522856L;

    private List<ICalendarItem> itemList = new ArrayList<>();

    private boolean selected = false;

    /**
     * <p>Getter for the field <code>itemList</code>.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<ICalendarItem> getItemList() {
        return itemList;
    }

    /**
     * <p>Setter for the field <code>itemList</code>.</p>
     *
     * @param itemList a {@link java.util.List} object.
     */
    public void setItemList(List<ICalendarItem> itemList) {
        this.itemList = itemList;
    }

    /**
     * <p>addItem.</p>
     *
     * @param item a {@link io.goobi.viewer.model.calendar.ICalendarItem} object.
     */
    public void addItem(ICalendarItem item) {
        itemList.add(item);
    }

    /**
     * <p>isSelected.</p>
     *
     * @return a boolean.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * <p>Setter for the field <code>selected</code>.</p>
     *
     * @param selected a boolean.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
