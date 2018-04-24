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
import java.net.URISyntaxException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import de.intranda.digiverso.presentation.servlets.rest.content.CommentAnnotation;
import de.intranda.digiverso.presentation.servlets.rest.content.IAnnotation;

/**
 * @author Florian Alpers
 *
 */
public class IIIFAnnotationSerializer extends JsonSerializer<Object> {

    /* (non-Javadoc)
     * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(Object object, JsonGenerator generator, SerializerProvider provide) throws IOException, JsonProcessingException {

        if (object instanceof List) {
            generator.writeStartArray();
            for (Object o : (List) object) {
                if (o instanceof IAnnotation) {
                    writeAnnotation(generator, (IAnnotation) o);
                }
            }
            generator.writeEndArray();
        } else if (object instanceof IAnnotation) {
            writeAnnotation(generator, (IAnnotation) object);
        }

    }

    /**
     * @param o
     * @throws @throws IOException
     */
    private void writeAnnotation(JsonGenerator generator, IAnnotation annotation) throws IOException {
       
        if(annotation instanceof CommentAnnotation) {   
            CommentAnnotation comment = (CommentAnnotation)annotation;
            generator.writeStartObject();
            try {
                generator.writeStringField("@id", comment.getId().toString());
                generator.writeStringField("@type", "oa:Annotation");
                generator.writeStringField("motivation", "oa:commenting");
                generator.writeStringField("on", comment.getTarget().toString());
                generator.writeObjectFieldStart("resource");
                generator.writeStringField("@type", "cnt:ContentAsText");
                generator.writeStringField("format", "text/plain");
                generator.writeStringField("chars", comment.getBody().getText());
                generator.writeEndObject();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            } finally {
                generator.writeEndObject();
            }
        } else {            
            generator.writeObject(annotation);
        }
        

    }

}
