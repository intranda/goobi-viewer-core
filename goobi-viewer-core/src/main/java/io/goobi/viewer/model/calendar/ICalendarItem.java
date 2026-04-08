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

/**
 * ICalendarItem interface.
 */
public interface ICalendarItem extends Comparable<ICalendarItem> {

    /**
     * getName.
     *
     * @return the display name of this calendar item
     */
    public String getName();

    /**
     * getValue.
     *
     * @return a int.
     */
    public int getValue();

    /**
     * getHits.
     *
     * @return a int.
     */
    public int getHits();

    /**
     * setHits.
     *
     * @param hits number of search hits for this item
     */
    public void setHits(int hits);

    /**
     * isSelected.
     *
     * @return true if this calendar item is currently selected, false otherwise
     */
    public boolean isSelected();

    /**
     * setSelected.
     *
     * @param selected true to mark this item as selected
     */
    public void setSelected(boolean selected);

    @Override
    public default int compareTo(ICalendarItem other) {
        return Integer.compare(getValue(), other.getValue());
    }

}
