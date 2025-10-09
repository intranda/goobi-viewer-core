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
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CalendarBean;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Data for the calendar view of an individual record (e.g. newspaper).
 */
public class CalendarView implements Serializable {

    private static final long serialVersionUID = -8118596197858508038L;

    private static final Logger logger = LogManager.getLogger(CalendarView.class);

    private final String pi;
    private final String anchorPi;
    private final String anchorField;
    private String year;
    /** Calendar representation of this record's child elements. */
    private List<CalendarItemMonth> calendarItems = new ArrayList<>();
    private List<String> volumeYears = null;

    /**
     * Constructor.
     *
     * @param pi Record identifier
     * @param anchorPi Anchor record identifier (must be same as pi if this is an anchor)
     * @param anchorField
     * @param year Year of a volume; null, if this is an anchor!
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public CalendarView(String pi, String anchorPi, String anchorField, String year) throws IndexUnreachableException, PresentationException {
        this.pi = pi;
        this.anchorPi = anchorPi;
        this.anchorField = anchorField != null ? anchorField : SolrConstants.PI_ANCHOR;
        this.year = year;

        if (year != null) {
            populateCalendar();
        }
    }

    /**
     * Checks whether the conditions for displaying the calendar view have been met.
     *
     * @return true if more than one selectable year is available or more than one item for the currently selected year; false otherwise
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should return true if number of items sufficient
     */
    public boolean isDisplay() throws PresentationException, IndexUnreachableException {
        int hits = 0;
        for (CalendarItemMonth item : calendarItems) {
            if (item.getHits() > 0) {
                hits += item.getHits();
                if (hits > 1) {
                    break;
                }
            }
        }

        return hits > 1 || getVolumeYears().size() > 1;
    }

    /**
     * Populates the calendar with items.
     *
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void populateCalendar() throws PresentationException, IndexUnreachableException {
        if (anchorPi != null && anchorPi.equals(pi)) {
            calendarItems = CalendarBean.populateMonthsWithDays(year, null, " +" + anchorField + ":\"" + anchorPi + "\"");
        } else {
            calendarItems = CalendarBean.populateMonthsWithDays(year, null, " +" + SolrConstants.PI_TOPSTRUCT + ":\"" + pi + "\"");
        }
        logger.trace("Calendar items: {}", calendarItems.size());
    }

    /**
     * <p>
     * getVolumeYears.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should only return volume years that have YEARMONTHDAY field
     */
    public List<String> getVolumeYears() throws PresentationException, IndexUnreachableException {
        if (volumeYears == null) {
            if (anchorPi == null) {
                volumeYears = Collections.emptyList();
            } else {
                String query = "+" + anchorField + ":\"" + anchorPi + "\" +" + SolrConstants.CALENDAR_DAY + ":*";
                logger.trace("Volume years query: {}", query);
                volumeYears = SearchHelper.getFacetValues(query, SolrConstants.CALENDAR_YEAR, 1);
            }
        }

        return volumeYears;
    }

    /**
     * <p>
     * Getter for the field <code>year</code>.
     * </p>
     *
     * @return the year
     */
    public String getYear() {
        return year;
    }

    /**
     * <p>
     * Setter for the field <code>year</code>.
     * </p>
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
     * <p>
     * Getter for the field <code>calendarItems</code>.
     * </p>
     *
     * @return the calendarItems
     */
    public List<CalendarItemMonth> getCalendarItems() {
        return calendarItems;
    }

}
