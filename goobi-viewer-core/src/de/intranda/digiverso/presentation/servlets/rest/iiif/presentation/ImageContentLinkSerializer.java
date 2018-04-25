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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import de.intranda.digiverso.presentation.model.iiif.presentation.content.ImageContent;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;

/**
 * @author Florian Alpers
 *
 */
public class ImageContentLinkSerializer extends JsonSerializer<ImageContent>{

    /* (non-Javadoc)
     * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(ImageContent element, JsonGenerator generator, SerializerProvider provicer) throws IOException, JsonProcessingException {
        
        if(element.getService() == null) {
//            generator.writeString(element.getId().toString());
            generator.writeStartObject();
            generator.writeStringField("@id", element.getId().toString());
            generator.writeStringField("@type", "dcTypes:Image");
            generator.writeEndObject();
        } else {
            generator.writeStartObject();
            generator.writeStringField("@id", element.getId().toString());
            generator.writeStringField("@type", "dcTypes:Image");
            generator.writeObjectFieldStart("service");
            generator.writeStringField("@context", ImageInformation.JSON_CONTEXT);
            generator.writeStringField("@id", element.getService().getId());
            generator.writeStringField("profile", ImageInformation.IIIF_COMPLIANCE_LEVEL.getUri());
            generator.writeEndObject();
            generator.writeEndObject();
        }
        
        
    }

}
