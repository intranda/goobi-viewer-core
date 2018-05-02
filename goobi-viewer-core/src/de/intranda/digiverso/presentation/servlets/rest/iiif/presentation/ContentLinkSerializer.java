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
package de.intranda.digiverso.presentation.servlets.rest.iiif.presentation;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import de.intranda.digiverso.presentation.model.iiif.presentation.CollectionExtent;
import de.intranda.digiverso.presentation.model.iiif.presentation.IPresentationModelElement;
import de.intranda.digiverso.presentation.model.iiif.presentation.Range;
import de.intranda.digiverso.presentation.servlets.rest.services.Service;

/**
 * @author Florian Alpers
 *
 */
public class ContentLinkSerializer extends JsonSerializer<List<IPresentationModelElement>> {

    private static final Logger logger = LoggerFactory.getLogger(ContentLinkSerializer.class);

    /* (non-Javadoc)
     * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(List<IPresentationModelElement> elements, JsonGenerator generator, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        if (elements != null && !elements.isEmpty()) {
                generator.writeStartArray();
                for (IPresentationModelElement element : elements) {
                    writeElement(element, generator, provider);
                }
                generator.writeEndArray();
            }

    }

    /**
     * @param element
     * @param generator
     * @throws IOException
     */
    public void writeElement(IPresentationModelElement element, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("@id", element.getId().toString());
        generator.writeStringField("@type", element.getType());
        if (element.getLabel() != null && !element.getLabel().isEmpty()) {
            generator.writeObjectField("label", element.getLabel());
        }
        if (element.getViewingHint() != null) {
            generator.writeObjectField("viewingHint", element.getViewingHint());
        }
        if (element.getThumbnail() != null) {
            generator.writeFieldName("thumbnail");
            new ImageContentLinkSerializer().serialize(element.getThumbnail(), generator, provider);
        }

        if (element.getService() != null) {
            generator.writeObjectField("service", element.getService(CollectionExtent.class));
        }
        
        if (element instanceof Range) {
            if(((Range) element).getStartCanvas() != null) {       
                generator.writeObjectField("startCanvas", ((Range) element).getStartCanvas().getId());
            }
        }


        generator.writeEndObject();
    }

}
