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
 * Calendar browsing item representing a single week.
 */
public class CalendarItemWeek extends AbstractCalendarItem implements Serializable {
    private static final long serialVersionUID = -6938153715941936763L;

    private List<CalendarItemDay> daysOfWeek = new ArrayList<>();

    /**
     * Creates a new CalendarItemWeek instance.
     *
     * @param name display label for the week
     * @param value numeric week-of-year value
     * @param hits number of records found for this week
     */
    public CalendarItemWeek(String name, int value, int hits) {
        super(name, value, hits);
    }

    /**
     * Getter for the field <code>daysOfWeek</code>.
     *
     * @return a {@link java.util.List} object.
     */
    public List<CalendarItemDay> getDaysOfWeek() {
        return daysOfWeek;
    }

    /**
     * Setter for the field <code>daysOfWeek</code>.
     *
     * @param daysOfWeek list of daily calendar items for this week
     */
    public void setDaysOfWeek(List<CalendarItemDay> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    /**
     * addDay.
     *
     * @param day daily calendar item to append to this week
     */
    public void addDay(CalendarItemDay day) {
        daysOfWeek.add(day);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "CalendarItemWeek [Week=" + name + ", hits=" + hits + "]";
    }
}
