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

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.model.maps.IArea;
import io.goobi.viewer.model.maps.Location;
import io.goobi.viewer.model.viewer.StringPair;

public class SearchTest extends AbstractTest {

    /**
     * @see Search#getAllSortFields()
     * @verifies return all fields
     */
    @Test
    public void getAllSortFields_shouldReturnAllFields() throws Exception {
        List<String> staticFields = DataManager.getInstance().getConfiguration().getStaticSortFields();
        Assert.assertEquals(1, staticFields.size());

        Search search = new Search();
        search.setSortString("SORT_FOO;SORT_BAR");
        Assert.assertEquals(2, search.getSortFields().size());

        List<StringPair> result = search.getAllSortFields();
        Assert.assertTrue(result.containsAll(search.getSortFields()));
        Assert.assertTrue(result.contains(new StringPair(staticFields.get(0).substring(1), "desc")));
    }

    @Test
    public void testParseGeoCoordsPoint() {
        String fieldValue ="[13.587443500000063 54.3766782, 13.568806999999993 54.364621, 13.57175570000004 54.38059639999999, 13.576777300000003 54.38823009999999, 13.632939999999962 54.35865]";
        List<IArea> locs = GeoCoordinateConverter.getLocations(fieldValue);
        assertEquals(5, locs.size());
        Location location = new Location(locs.get(0), "Label", URI.create("#"));
        //System.out.println(location.getGeoJson());
    }

    @Test
    public void testParseGeoCoordsPolygon() {
        String fieldValue ="[POLYGON((28.88222222222222 41.13361111111111, 29.06888888888889 41.13361111111111, 29.06888888888889 40.974444444444444, 28.88222222222222 40.974444444444444, 28.88222222222222 41.13361111111111)), "
                + "POLYGON((18.15 44.96666666666667, 30.033333333333335 44.96666666666667, 30.033333333333335 39.333333333333336, 18.15 39.333333333333336, 18.15 44.96666666666667))]";
        List<IArea> locs = GeoCoordinateConverter.getLocations(fieldValue);
        assertEquals(2, locs.size());
        Location location = new Location(locs.get(0), "Label", URI.create("#"));
        //System.out.println(location.getGeoJson());
    }
}
