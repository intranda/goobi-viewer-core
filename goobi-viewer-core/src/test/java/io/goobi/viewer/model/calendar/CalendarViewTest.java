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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;

class CalendarViewTest extends AbstractSolrEnabledTest {

    /**
     * @see CalendarView#isDisplay()
     * @verifies return true if number of items sufficient
     */
    @Test
    void isDisplay_shouldReturnTrueIfNumberOfItemsSufficient() throws Exception {
        // Verify that isDisplay returns true when the calendar has enough items
        CalendarView cv = new CalendarView("168714434_1805", "168714434", null, null);
        Assertions.assertTrue(cv.isDisplay());
    }

    /**
     * @see CalendarView#isDisplay()
     * @verifies return true if numer of items suffient
     */
    @Test
    void isDisplay_shouldReturnTrueIfNumerOfItemsSuffient() throws Exception {
        CalendarView cv = new CalendarView("168714434_1805", "168714434", null, null);
        Assertions.assertTrue(cv.isDisplay());
    }

    /**
     * @see CalendarView#getVolumeYears()
     * @verifies only return volume years that have YEARMONTHDAY field
     */
    @Test
    void getVolumeYears_shouldOnlyReturnVolumeYearsThatHaveYEARMONTHDAYField() throws Exception {
        CalendarView cv = new CalendarView("168714434_1805", "168714434", null, null);
        List<String> years = cv.getVolumeYears();
        Assertions.assertEquals(9, years.size());
    }

    /**
     * @see CalendarView#isDisplay()
     * @verifies return false when docstruct is not in calendar whitelist
     */
    @Test
    void isDisplay_shouldReturnFalseWhenDocstructIsNotInCalendarWhitelist() throws Exception {
        // The test config seeds the whitelist with Newspaper/Periodical only — a Podcast
        // anchor must therefore suppress the calendar view even if the year-count probe
        // would otherwise admit it, so that viewToc.xhtml falls through to the regular
        // issue-list TOC instead of rendering an empty calendar grid.
        CalendarView cv = new CalendarView("168714434_1805", "168714434", null, null, "Podcast");
        Assertions.assertFalse(cv.isDisplay());
    }

    /**
     * @see CalendarView#isDisplay()
     * @verifies return true when whitelisted docstruct provided (issue inheriting from anchor)
     */
    @Test
    void isDisplay_shouldReturnTrueWhenWhitelistedDocstructProvided() throws Exception {
        // ViewManager passes the anchor's docstruct rather than the issue/volume's own when
        // constructing CalendarView, so a newspaper issue (top struct = "NewspaperIssue") is
        // expected to receive its anchor's docstruct ("Newspaper") here. With that whitelisted
        // value the gate must not suppress the calendar that widget_calendar.xhtml renders.
        CalendarView cv = new CalendarView("168714434_1805", "168714434", null, null, "Newspaper");
        Assertions.assertTrue(cv.isDisplay());
    }

}
