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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * CalendarItemCentury class.
 * </p>
 */
public class CalendarItemCentury extends AbstractCalendarItem implements Serializable {

    private static final long serialVersionUID = -3123380483405975449L;

    private final Map<Integer, CalendarItemYear> years = new HashMap<>();

    /**
     * <p>
     * Constructor for CalendarItemYear.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param value a int.
     * @param hits a int.
     */
    public CalendarItemCentury(String name, int value, int hits) {
        super(name, value, hits);
    }

    public CalendarItemCentury(Integer century) {
        this(String.valueOf(century), century, 0);
    }

    /**
     * <p>
     * Getter for the field <code>years</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CalendarItemYear> getYears() {
        return years.values().stream().sorted().toList();
    }

    public CalendarItemYear getYear(Integer year) {
        return this.years.get(year);
    }

    /**
     * Add the given hits count to the given year within the century, creating a new item in {@link #getYears()} if necessary. The hits count is both
     * added to the year item and the century itself, so there is no need to additionally call {@link #addHits(Integer)} on the century
     * 
     * @param year the year of the hits
     * @param hits the hit count for the given year
     */
    public void addYearHits(Integer year, Integer hits) {
        this.years.computeIfAbsent(year, y -> new CalendarItemYear(String.valueOf(year), year, 0)).addHits(hits);
        this.addHits(hits);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "CalendarItemCentury [century=" + name + ", hits=" + hits + "]";
    }

}
