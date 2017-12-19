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
package de.intranda.digiverso.presentation.model.calendar;

import java.io.Serializable;
import java.util.List;

import de.intranda.digiverso.presentation.controller.Helper;

public class CalendarItemMonth extends AbstractCalendarItem implements Serializable {

    private static final long serialVersionUID = -4930440571977358495L;

    private List<CalendarItemWeek> weeksOfMonth;

    private List<CalendarItemDay> daysOfMonth;

    public CalendarItemMonth(String name, int value, int hits) {
        super(name, value, hits);
    }

    public List<CalendarItemDay> getDaysOfMonth() {
        return daysOfMonth;
    }

    public void setDaysOfMonth(List<CalendarItemDay> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    public List<CalendarItemWeek> getWeeksOfMonth() {
        return weeksOfMonth;
    }

    public void setWeeksOfMonth(List<CalendarItemWeek> weeksOfMonth) {
        this.weeksOfMonth = weeksOfMonth;
    }

    @Override
    public String toString() {
        return "CalendarItemMonth [month=" + name + ", hits=" + hits + "]";
    }

}
