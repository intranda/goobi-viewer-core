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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.v1.ApiUrls;

/**
 * @author florian
 *
 */
class AbstractBuilderTest extends AbstractTest {

    AbstractBuilder builder;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        builder = new AbstractBuilder(new ApiUrls("http://localhost:8080/viewer/rest")) {
        };
    }

    @Test
    void testGetEventFields() {
        Map<String, List<String>> events = builder.getEventFields();
        Assertions.assertNotNull(events);
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals(2, events.get("").size());
        Assertions.assertEquals(2, events.get("Provenienz").size());
        Assertions.assertEquals(1, events.get("Expression Creation").size());
        Assertions.assertEquals("MD_EVENTARTIST", events.get("Expression Creation").iterator().next());

    }

    @Test
    void testMetadataContained() {
        List<String> fieldNames = List.of("MD_TEST", "MD_BLA*");
        
        Assertions.assertTrue(builder.contained("MD_TEST", fieldNames));
        Assertions.assertTrue(builder.contained("MD_TEST_LANG_DE", fieldNames));
        Assertions.assertFalse(builder.contained("MD_TEST_2", fieldNames));
        
        Assertions.assertTrue(builder.contained("MD_BLA", fieldNames));
        Assertions.assertTrue(builder.contained("MD_BLA_LANG_EN", fieldNames));
        Assertions.assertTrue(builder.contained("MD_BLA_2", fieldNames));

    }
    
}
