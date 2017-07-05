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
package de.intranda.digiverso.presentation.model.search;

import java.io.Serializable;

public class CalendarItemDay extends AbstractCalendarItem implements Serializable {

    private static final long serialVersionUID = 6482477938806267855L;

    private final String url;

    private String dayOfWeek;

    private String info;

    public CalendarItemDay(String name, int value, int hits, String url) {
        super(name, value, hits);
        this.url = url;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the dayOfWeek
     */
    public String getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * @param dayOfWeek the dayOfWeek to set
     */
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /**
     * @return the info
     */
    public String getInfo() {
        return info;
    }

    /**
     * @param info the info to set
     */
    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "CalendarItemDay [day=" + name + ", hits=" + hits + "]";
    }
}
