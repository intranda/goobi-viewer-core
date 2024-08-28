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
 * <p>
 * ICalendarItem interface.
 * </p>
 */
public interface ICalendarItem extends Comparable<ICalendarItem> {

    /**
     * <p>
     * getName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName();

    /**
     * <p>
     * getValue.
     * </p>
     *
     * @return a int.
     */
    public int getValue();

    /**
     * <p>
     * getHits.
     * </p>
     *
     * @return a int.
     */
    public int getHits();

    /**
     * <p>
     * setHits.
     * </p>
     *
     * @param hits a int.
     */
    public void setHits(int hits);

    /**
     * <p>
     * isSelected.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSelected();

    /**
     * <p>
     * setSelected.
     * </p>
     *
     * @param selected a boolean.
     */
    public void setSelected(boolean selected);

    @Override
    public default int compareTo(ICalendarItem other) {
        return Integer.compare(getValue(), other.getValue());
    }

}
