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
package io.goobi.viewer.model.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.viewer.StructElement;

class MetadataParameterTest {

    @Test
    void testReadMetadata() throws IndexUnreachableException, PresentationException {
        MetadataParameter param = new MetadataParameter();
        param.setKey("TEST_FIELD");
        param.setType(MetadataParameterType.FIELD);
        MetadataParameter param2 = new MetadataParameter();
        param2.setKey("TEST_FIELD_2");
        param2.setType(MetadataParameterType.FIELD);
        param2.setPrefix(" ");
        
        StructElement ele = new StructElement();
        ele.setMetadataFields(Map.of("TEST_FIELD", Arrays.asList("foo", "boo"), "TEST_FIELD_2", Collections.singletonList("bar")));
        
        Metadata md = new Metadata("", "", Arrays.asList(param, param2));
        md.populate(ele, "1234", null, null);
        assertEquals("foo", md.getValues().get(0).getComboValueShort(0));
        assertEquals(" bar", md.getValues().get(0).getComboValueShort(1));
        assertEquals("boo", md.getValues().get(1).getComboValueShort(0));
        assertEquals("foo bar", md.getValues().get(0).getCombinedValue());
        assertEquals("foo bar, boo", md.getCombinedValue(", "));
    }

}
