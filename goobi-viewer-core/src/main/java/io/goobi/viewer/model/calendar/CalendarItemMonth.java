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
import java.util.List;

import io.goobi.viewer.controller.Helper;

/**
 * <p>
 * CalendarItemMonth class.
 * </p>
 */
public class CalendarItemMonth extends AbstractCalendarItem implements Serializable {

    private static final long serialVersionUID = -4930440571977358495L;

    private List<CalendarItemWeek> weeksOfMonth;

    private List<CalendarItemDay> daysOfMonth;

    /**
     * <p>
     * Constructor for CalendarItemMonth.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a int.
     * @param hits a int.
     */
    public CalendarItemMonth(String name, int value, int hits) {
        super(name, value, hits);
    }

    /**
     * <p>
     * Getter for the field <code>daysOfMonth</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CalendarItemDay> getDaysOfMonth() {
        return daysOfMonth;
    }

    /**
     * <p>
     * Setter for the field <code>daysOfMonth</code>.
     * </p>
     *
     * @param daysOfMonth a {@link java.util.List} object.
     */
    public void setDaysOfMonth(List<CalendarItemDay> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    /**
     * <p>
     * Getter for the field <code>weeksOfMonth</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CalendarItemWeek> getWeeksOfMonth() {
        return weeksOfMonth;
    }

    /**
     * <p>
     * Setter for the field <code>weeksOfMonth</code>.
     * </p>
     *
     * @param weeksOfMonth a {@link java.util.List} object.
     */
    public void setWeeksOfMonth(List<CalendarItemWeek> weeksOfMonth) {
        this.weeksOfMonth = weeksOfMonth;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "CalendarItemMonth [month=" + name + ", hits=" + hits + "]";
    }

}
