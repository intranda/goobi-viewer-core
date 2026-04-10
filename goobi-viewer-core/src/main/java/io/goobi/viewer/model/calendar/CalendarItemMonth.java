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
import java.util.List;

/**
 * Calendar browsing item representing a single month.
 */
public class CalendarItemMonth extends AbstractCalendarItem implements Serializable {

    private static final long serialVersionUID = -4930440571977358495L;

    private List<CalendarItemWeek> weeksOfMonth;

    private List<CalendarItemDay> daysOfMonth;

    /**
     * Creates a new CalendarItemMonth instance.
     *
     * @param name display name of the month (e.g. "January").
     * @param value numeric month value (1-12).
     * @param hits number of records for this month.
     */
    public CalendarItemMonth(String name, int value, int hits) {
        super(name, value, hits);
    }

    /**
     * Getter for the field <code>daysOfMonth</code>.
     *
     * @return list of calendar day items for this month.
     */
    public List<CalendarItemDay> getDaysOfMonth() {
        return daysOfMonth;
    }

    /**
     * Setter for the field <code>daysOfMonth</code>.
     *
     * @param daysOfMonth list of calendar day items to set.
     */
    public void setDaysOfMonth(List<CalendarItemDay> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    /**
     * Getter for the field <code>weeksOfMonth</code>.
     *
     * @return list of calendar week items for this month.
     */
    public List<CalendarItemWeek> getWeeksOfMonth() {
        return weeksOfMonth;
    }

    /**
     * Setter for the field <code>weeksOfMonth</code>.
     *
     * @param weeksOfMonth list of calendar week items to set.
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
