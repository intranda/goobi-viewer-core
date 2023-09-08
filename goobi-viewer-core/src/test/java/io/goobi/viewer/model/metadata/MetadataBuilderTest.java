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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;

class MetadataBuilderTest {

    @Test
    void test() {
        MultiLanguageMetadataValue value = new MultiLanguageMetadataValue(Map.of("en", "Gallery", "de", "Gallerie"));
        Map<String, List<IMetadataValue>> metadata = Map.of(
                "MD_ROLE", List.of(value));

        MetadataBuilder builder = new MetadataBuilder(metadata);

        MetadataParameter param = new MetadataParameter();
        param.setKey("MD_ROLE");
        param.setPrefix(" (");
        param.setSuffix(")");
        param.setType(MetadataParameterType.FIELD);
        Metadata config = new Metadata("MD_ROLE", "{MD_ROLE}", List.of(param));
        IMetadataValue translation = builder.build(config);

        assertEquals(" (Gallery)", translation.getValue("en").orElse(""));
        assertEquals("Gallery", value.getValue("en").orElse(""));
    }
}
