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
package io.goobi.viewer.api.rest.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;

import de.intranda.api.serializer.WebAnnotationMetadataValueSerializer;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;

/**
 * Implementation of {@link WebAnnotationMetadataValueSerializer} which always writes the value as json-object, never as string. This was, language
 * info is always preserved
 *
 * @author florian
 *
 */
public class TranslatedTextSerializer extends WebAnnotationMetadataValueSerializer {

    @Override
    public void serialize(IMetadataValue element, JsonGenerator generator, SerializerProvider provicer) throws IOException, JsonProcessingException {
        super.serialize(element, generator, provicer);
    }

    /**
     * Always assume all translations are unique to write full translation info.
     */
    @Override
    protected boolean allTranslationsEqual(MultiLanguageMetadataValue element) {
        return false;
    }
}
