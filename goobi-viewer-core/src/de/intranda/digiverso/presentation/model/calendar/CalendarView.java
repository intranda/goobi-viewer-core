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
package de.intranda.digiverso.presentation.model.calendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.CalendarBean;
import de.intranda.digiverso.presentation.model.search.SearchHelper;

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
     * @throws IndexUnreachableException
     * @throws PresentationException
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
     * @return
     */
    public boolean isDisplay() {
        boolean empty = true;
        for (CalendarItemMonth item : calendarItems) {
            if (item.getHits() > 0) {
                empty = false;
                break;
            }
        }
        return pi.equals(anchorPi) || !empty;
    }

    /**
     * Populates the calendar with items.
     * 
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void populateCalendar() throws PresentationException, IndexUnreachableException {
        if (anchorPi != null) {
            calendarItems = CalendarBean.populateMonthsWithDays(year, null, " AND " + SolrConstants.PI_ANCHOR + ":" + anchorPi);
        } else {
            calendarItems = CalendarBean.populateMonthsWithDays(year, null, " AND " + SolrConstants.PI_TOPSTRUCT + ":" + pi);
        }
        logger.trace("Calendar items: {}", calendarItems.size());
    }

    /**
     * 
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<String> getVolumeYears() throws PresentationException, IndexUnreachableException {
        if (anchorPi != null) {
            return SearchHelper.getFacetValues(SolrConstants.PI_PARENT + ":" + anchorPi, SolrConstants._CALENDAR_YEAR, 1);
        }

        return Collections.emptyList();
    }

    /**
     * @return the year
     */
    public String getYear() {
        return year;
    }

    /**
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
     * @return the calendarItems
     */
    public List<CalendarItemMonth> getCalendarItems() {
        return calendarItems;
    }

}
