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
package io.goobi.viewer.servlets.rest.serialization;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.goobi.viewer.model.misc.Translation;

/**
 * <p>
 * TranslationListSerializer class.
 * </p>
 *
 * @author florian
 */
public class TranslationListSerializer extends JsonSerializer<Collection<Translation>> {

    /* (non-Javadoc)
     * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
     */
    /** {@inheritDoc} */
    @Override
    public void serialize(Collection<Translation> translations, JsonGenerator gen, SerializerProvider serializers) throws IOException {

        Map<String, List<Translation>> groupedTranslations = groupByLabel(translations);

        if (!groupedTranslations.isEmpty()) {
            gen.writeStartObject();
            for (String tag : groupedTranslations.keySet()) {
                gen.writeObjectFieldStart(tag);
                for (Translation translation : groupedTranslations.get(tag)) {
                    gen.writeArrayFieldStart(translation.getLanguage());
                    gen.writeString(translation.getValue());
                    gen.writeEndArray();
                }
                gen.writeEndObject();
            }
            gen.writeEndObject();
        } else {
            gen.writeNull();
        }

    }

    private Map<String, List<Translation>> groupByLabel(Collection<Translation> translations) {
        Map<String, List<Translation>> map = new HashMap<>();
        for (Translation translation : translations) {
            String tag = translation.getTag();
            List<Translation> tagList;
            if (!map.containsKey(tag)) {
                tagList = new ArrayList<>();
                map.put(tag, tagList);
            } else {
                tagList = map.get(tag);
            }
            tagList.add(translation);
        }
        return map;
    }

}
