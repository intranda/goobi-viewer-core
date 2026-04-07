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

/**
 * CalendarItemDay class.
 */
public class CalendarItemDay extends AbstractCalendarItem implements Serializable {

    private static final long serialVersionUID = 6482477938806267855L;

    private String query;

    private String singleResultUrl;

    private String dayOfWeek;

    /**
     * Creates a new CalendarItemDay instance.
     *
     * @param name a {@link java.lang.String} object.
     * @param value a int.
     * @param hits a int.
     */
    public CalendarItemDay(String name, int value, int hits) {
        super(name, value, hits);
    }

    /**
     * Getter for the field <code>query</code>.
     *
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Setter for the field <code>query</code>.
     *
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Getter for the field <code>singleResultUrl</code>.
     *
     * @return the singleResultUrl
     */
    public String getSingleResultUrl() {
        return singleResultUrl;
    }

    /**
     * Setter for the field <code>singleResultUrl</code>.
     *
     * @param singleResultUrl the singleResultUrl to set
     */
    public void setSingleResultUrl(String singleResultUrl) {
        this.singleResultUrl = singleResultUrl;
    }

    /**
     * Getter for the field <code>dayOfWeek</code>.
     *
     * @return the dayOfWeek
     */
    public String getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * Setter for the field <code>dayOfWeek</code>.
     *
     * @param dayOfWeek the dayOfWeek to set
     */
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "CalendarItemDay [day=" + name + ", hits=" + hits + "]";
    }
}
