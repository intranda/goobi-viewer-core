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
package io.goobi.viewer.model.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.model.maps.IArea;
import io.goobi.viewer.model.maps.Location;
import io.goobi.viewer.model.viewer.StringPair;

class SearchTest extends AbstractDatabaseAndSolrEnabledTest {

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @see Search#getAllSortFields()
     * @verifies return all fields
     */
    @Test
    void getAllSortFields_shouldReturnAllFields() {
        List<String> staticFields = DataManager.getInstance().getConfiguration().getStaticSortFields();
        Assertions.assertEquals(1, staticFields.size());

        Search search = new Search();
        search.setSortString("SORT_FOO;SORT_BAR");
        Assertions.assertEquals(2, search.getSortFields().size());

        List<StringPair> result = search.getAllSortFields();
        Assertions.assertTrue(result.containsAll(search.getSortFields()));
        Assertions.assertTrue(result.contains(new StringPair(staticFields.get(0).substring(1), "desc")));
    }

    @Test
    void testParseGeoCoordsPoint() {
        String fieldValue =
                "[13.587443500000063 54.3766782, 13.568806999999993 54.364621, 13.57175570000004 54.38059639999999, 13.576777300000003 54.38823009999999, 13.632939999999962 54.35865]";
        List<IArea> locs = GeoCoordinateConverter.getLocations(fieldValue);
        assertEquals(5, locs.size());
        Location location = new Location(locs.get(0), "Label", URI.create("#"));
    }

    @Test
    void testParseGeoCoordsPolygon() {
        String fieldValue =
                "[POLYGON((28.88222222222222 41.13361111111111, 29.06888888888889 41.13361111111111, 29.06888888888889 40.974444444444444, 28.88222222222222 40.974444444444444, 28.88222222222222 41.13361111111111)), "
                        + "POLYGON((18.15 44.96666666666667, 30.033333333333335 44.96666666666667, 30.033333333333335 39.333333333333336, 18.15 39.333333333333336, 18.15 44.96666666666667))]";
        List<IArea> locs = GeoCoordinateConverter.getLocations(fieldValue);
        assertEquals(2, locs.size());
        Location location = new Location(locs.get(0), "Label", URI.create("#"));
    }

    /**
     * Regression test for the merge of populateRanges() and populateUnfilteredFacets() into a
     * single Solr call. Verifies that range facet min/max values for the YEAR field are populated
     * after Search.execute() runs against the test index.
     *
     * @see Search#execute(SearchFacets, java.util.Map, int, Locale)
     * @verifies populate YEAR range facet values
     */
    @Test
    void execute_shouldPopulateYearRangeFacets() throws Exception {
        Search search = new Search();
        // Empty query resolves to ALL_RECORDS_QUERY, matching the full test index
        search.setQuery("");
        SearchFacets facets = new SearchFacets();

        search.execute(facets, null, 10, Locale.ENGLISH);

        // YEAR is configured as a range facet in config_viewer.test.xml; the test index must
        // contain at least one record with a YEAR value within the configured min/max window.
        Assertions.assertFalse(facets.getValueRange("YEAR").isEmpty(),
                "YEAR range facet should be populated after execute()");
    }

    /**
     * Regression test for the merge of populateRanges() and populateUnfilteredFacets() into a
     * single Solr call. Verifies that fields with alwaysApplyToUnfilteredHits=true are populated
     * in the available-facets map after Search.execute() runs.
     *
     * @see Search#execute(SearchFacets, java.util.Map, int, Locale)
     * @verifies populate DC unfiltered facet
     */
    @Test
    void execute_shouldPopulateUnfilteredFacetsForAlwaysApplyFields() throws Exception {
        Search search = new Search();
        // Empty query resolves to ALL_RECORDS_QUERY, matching the full test index
        search.setQuery("");
        SearchFacets facets = new SearchFacets();

        search.execute(facets, null, 10, Locale.ENGLISH);

        // DC is configured with alwaysApplyToUnfilteredHits="true" in config_viewer.test.xml;
        // every record in the test index has a DC value, so it must appear in availableFacets.
        Assertions.assertTrue(facets.getAvailableFacets().containsKey("DC"),
                "DC should appear in available facets after execute()");
    }
}
