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
package io.goobi.viewer.model.maps;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author florian
 *
 */
public class GeoMapMarkerTest {

    private GeoMapMarker testObject = new GeoMapMarker("test");
    private String testString = "{\"name\":\"test\",\"icon\":\"\",\"markerColor\":\"blue\",\"highlightColor\":\"cyan\",\"shape\":\"circle\",\"extraClasses\":\"\",\"prefix\":\"fa\",\"iconColor\":\"white\",\"iconRotate\":0,\"number\":\"\",\"highlightIcon\":\"\",\"useDefault\":false,\"svg\":false,\"shadow\":true,\"type\":\"ExtraMarkers\",\"className\":\"\"}";


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testToJSONString() throws JsonProcessingException {
        String s = testObject.toJSONString();
        Assert.assertEquals(testString, s);
    }

    @Test
    public void testDeserialize() throws JsonMappingException, JsonProcessingException {
        GeoMapMarker m = GeoMapMarker.fromJSONString(testString);
        Assert.assertEquals(testObject.getName(), m.getName());
        Assert.assertEquals(testObject.getIconRotate(), m.getIconRotate());
        Assert.assertEquals(testObject.getShape(), m.getShape());
        Assert.assertEquals(testObject.isSvg(), m.isSvg());
    }

}
