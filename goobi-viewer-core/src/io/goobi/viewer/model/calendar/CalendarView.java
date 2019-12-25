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
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package io.goobi.viewer.model.calendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CalendarBean;
import io.goobi.viewer.model.search.SearchHelper;

/**
 * Data for the calendar view of an individual record (e.g. newspaper).
 */
public class CalendarView {

    private static final Logger logger = LoggerFactory.getLogger(CalendarView.class);

    private final String pi;
    private final String anchorPi;
    private String year;
    /** Calendar representation of this record's child elements. */
    private List<CalendarItemMonth> calendarItems = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param pi Record identifier
     * @param anchorPi Anchor record identifier (must be same as pi if this is an anchor)
     * @param year Year of a volume; null, if this is an anchor!
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.PresentationException
     */
    public CalendarView(String pi, String anchorPi, String year) throws IndexUnreachableException, PresentationException {
        this.pi = pi;
        this.anchorPi = anchorPi;
        this.year = year;

        if (year != null) {
            populateCalendar();
        }
    }

    /**
     * Checks whether the conditions for displaying the calendar view have been met.
     *
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @return a boolean.
     */
    public boolean isDisplay() throws PresentationException, IndexUnreachableException {
        boolean empty = true;
        for (CalendarItemMonth item : calendarItems) {
            if (item.getHits() > 0) {
                empty = false;
                break;
            }
        }
        return !empty || !getVolumeYears().isEmpty();
    }

    /**
     * Populates the calendar with items.
     *
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     */
    public void populateCalendar() throws PresentationException, IndexUnreachableException {
        if (anchorPi != null && anchorPi.equals(pi)) {
            calendarItems = CalendarBean.populateMonthsWithDays(year, null, " AND " + SolrConstants.PI_ANCHOR + ":" + anchorPi);
        } else {
            calendarItems = CalendarBean.populateMonthsWithDays(year, null, " AND " + SolrConstants.PI_TOPSTRUCT + ":" + pi);
        }
        logger.trace("Calendar items: {}", calendarItems.size());
    }

    /**
     * <p>getVolumeYears.</p>
     *
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @should only return volume years that have YEARMONTHDAY field
     * @return a {@link java.util.List} object.
     */
    public List<String> getVolumeYears() throws PresentationException, IndexUnreachableException {
        if (anchorPi != null) {
            return SearchHelper.getFacetValues("+" + SolrConstants.PI_PARENT + ":" + anchorPi + " +" + SolrConstants._CALENDAR_YEAR + ":*",
                    SolrConstants._CALENDAR_YEAR, 1);
        }

        return Collections.emptyList();
    }

    /**
     * <p>Getter for the field <code>year</code>.</p>
     *
     * @return the year
     */
    public String getYear() {
        return year;
    }

    /**
     * <p>Setter for the field <code>year</code>.</p>
     *
     * @param year the year to set
     */
    public void setYear(String year) {
        this.year = year;
        try {
            populateCalendar();
        } catch (PresentationException | IndexUnreachableException e) {
            logger.debug("{} thrown here: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * <p>Getter for the field <code>calendarItems</code>.</p>
     *
     * @return the calendarItems
     */
    public List<CalendarItemMonth> getCalendarItems() {
        return calendarItems;
    }

}
