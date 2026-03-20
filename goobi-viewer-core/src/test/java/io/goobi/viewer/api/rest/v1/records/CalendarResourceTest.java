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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CALENDAR;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CALENDAR_YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

class CalendarResourceTest extends AbstractRestApiTest {

    private static final String PI = "168714434_1823";
    private static final int YEAR = 1823;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @see CalendarResource#getCalendarEntries(int)
     * @verifies return all issues for given pi and year
     */
    @Test
    void getCalendarEntries_shouldReturnAllIssuesForGivenPiAndYear() {
        String url = urls.path(RECORDS_CALENDAR, RECORDS_CALENDAR_YEAR).params(PI, YEAR).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return response entity");
            String entity = response.readEntity(String.class);
            assertNotNull(entity);

            JSONArray entries = new JSONArray(entity);
            assertEquals(106, entries.length(), "Should return 106 issues for year 1823");

            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                assertTrue(entry.has("date"), "Entry " + i + " should have 'date'");
                assertTrue(entry.has("label"), "Entry " + i + " should have 'label'");
                assertTrue(entry.has("url"), "Entry " + i + " should have 'url'");

                String date = entry.getString("date");
                assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"), "Date should be in ISO format: " + date);
                assertTrue(date.startsWith("1823-"), "Date should be in year 1823: " + date);

                String entryUrl = entry.getString("url");
                assertTrue(entryUrl.startsWith("/image/"), "URL should start with /image/: " + entryUrl);
                assertTrue(entryUrl.endsWith("/"), "URL should end with /: " + entryUrl);
            }
        }
    }

    /**
     * @see CalendarResource#getCalendarEntries(int)
     * @verifies return 404 if pi not found
     */
    @Test
    void getCalendarEntries_shouldReturn404IfPiNotFound() {
        String url = urls.path(RECORDS_CALENDAR, RECORDS_CALENDAR_YEAR).params("NONEXISTENT_PI", YEAR).build();
        try (Response response = target(url)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(404, response.getStatus(), "Should return status 404 for unknown PI");
        }
    }

    /**
     * @see CalendarResource#convertYearMonthDayToIsoDate(String)
     * @verifies convert valid yearmonthday to iso date
     */
    @Test
    void convertYearMonthDayToIsoDate_shouldConvertValidYearmonthdayToIsoDate() {
        assertEquals("1893-03-01", CalendarResource.convertYearMonthDayToIsoDate("18930301"));
        assertEquals("2024-12-25", CalendarResource.convertYearMonthDayToIsoDate("20241225"));
        assertEquals("1823-01-04", CalendarResource.convertYearMonthDayToIsoDate("18230104"));
    }

    /**
     * @see CalendarResource#convertYearMonthDayToIsoDate(String)
     * @verifies return original value if parsing fails
     */
    @Test
    void convertYearMonthDayToIsoDate_shouldReturnOriginalValueIfParsingFails() {
        assertEquals("invalid", CalendarResource.convertYearMonthDayToIsoDate("invalid"));
        assertEquals("12345", CalendarResource.convertYearMonthDayToIsoDate("12345"));
        assertNull(CalendarResource.convertYearMonthDayToIsoDate(null));
    }
}
