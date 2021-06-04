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
package io.goobi.viewer.model.search;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author florian
 *
 */
public class GeoCoordinateFeatureTest {

    @Test
    public void testParseSearchString() {
        double[][] points = new double[][] {{1.1, 1.2}, {2.1, 2.2}, {3.1, 3.2}, {4.1, 4.2}, {1.1, 1.2}};
        String referencePointsString = "1.1 1.2, 2.1 2.2, 3.1 3.2, 4.1 4.2, 1.1 1.2";
        String referenceQuery = "IsWithin(POLYGON(("+referencePointsString+")))";
        System.out.println("query = " + referenceQuery);
        GeoCoordinateFeature feature = new GeoCoordinateFeature(points);
        String query = feature.getSearchString();
        assertEquals(referenceQuery, query);
    }
    
    @Test
    public void testParsePoints() {
        double[][] referencePoints = new double[][] {{1.1, 1.2}, {2.1, 2.2}, {3.1, 3.2}, {4.1, 4.2}, {1.1, 1.2}};
        String pointsString = "1.1 1.2, 2.1 2.2, 3.1 3.2, 4.1 4.2, 1.1 1.2";
        String query = "WKT_COORDS:\"Intersects(POLYGON(("+pointsString+")))";
        System.out.println("query = " + query);
        double[][] points = GeoCoordinateFeature.getGeoSearchPoints(query);
        assertArrayEquals(referencePoints, points);
    }

}
