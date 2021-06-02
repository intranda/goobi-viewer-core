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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
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
        Assert.assertTrue(result.contains(new StringPair(staticFields.get(0), "asc")));
    }
    
    @Test
    public void testParseGeoCoords() {
        String fieldValue ="[13.587443500000063 54.3766782, 13.568806999999993 54.364621, 13.57175570000004 54.38059639999999, 13.576777300000003 54.38823009999999, 13.632939999999962 54.35865]";
        List<double[]> locs = Search.getLocations(fieldValue);
        assertEquals(5, locs.size());
    }
}