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
package de.intranda.digiverso.presentation.model.iiif.presentation;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florian Alpers
 *
 */
public class ListOrObjectSerializer extends JsonSerializer<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ListOrObjectSerializer.class);
    
    /* (non-Javadoc)
     * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void serialize(Object object, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
       
        if(object instanceof Collection) {
            
            if(((Collection)object).size() > 1) {
                
                generator.writeStartArray();
                
                ((Collection)object).forEach(element -> {
                    try {
                        generator.writeObject(element);
                    } catch (IOException e) {
                        logger.error("Error writing object " + element + " to json array");
                    }
                });
                
                generator.writeEndArray();
                
            } else if(((Collection)object).size() == 1) {
                Object element = ((Collection)object).iterator().next();
                generator.writeObject(element);
            }
            
        } else {
            generator.writeObject(object);
        }

    }

}
