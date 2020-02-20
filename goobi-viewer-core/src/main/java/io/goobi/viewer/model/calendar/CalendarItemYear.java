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

/**
 * <p>
 * CalendarItemYear class.
 * </p>
 */
public class CalendarItemYear extends AbstractCalendarItem implements Serializable {

    private static final long serialVersionUID = -3123380483405975449L;

    private List<CalendarItemMonth> monthsOfYear;

    /**
     * <p>
     * Constructor for CalendarItemYear.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a int.
     * @param hits a int.
     */
    public CalendarItemYear(String name, int value, int hits) {
        super(name, value, hits);
    }

    /**
     * <p>
     * Getter for the field <code>monthsOfYear</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CalendarItemMonth> getMonthsOfYear() {
        return monthsOfYear;
    }

    /**
     * <p>
     * Setter for the field <code>monthsOfYear</code>.
     * </p>
     *
     * @param monthsOfYear a {@link java.util.List} object.
     */
    public void setMonthsOfYear(List<CalendarItemMonth> monthsOfYear) {
        this.monthsOfYear = monthsOfYear;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "CalendarItemYear [year=" + name + ", hits=" + hits + "]";
    }
}
