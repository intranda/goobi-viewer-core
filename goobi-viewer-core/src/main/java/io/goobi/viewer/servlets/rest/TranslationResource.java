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
package io.goobi.viewer.servlets.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.goobi.viewer.messages.MessagesTranslation;
import io.goobi.viewer.model.misc.Translation;
import io.goobi.viewer.servlets.rest.bookmarks.BookmarkResource;
import io.goobi.viewer.servlets.rest.serialization.TranslationListSerializer;

/**
 * <p>TranslationResource class.</p>
 *
 * @author florian
 */
@Path("/messages")
@ViewerRestServiceBinding
public class TranslationResource {
    
    private static final Logger logger = LoggerFactory.getLogger(TranslationResource.class);

    /**
     * <p>getTranslations.</p>
     *
     * @param keys a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.servlets.rest.TranslationResource.TranslationList} object.
     */
    @GET
    @Path("/translate/{keys}")
    @Produces({ MediaType.APPLICATION_JSON })
    public TranslationList getTranslations(@PathParam("keys") String keys) {
        logger.trace("getTranslations: {}", keys);

        String[] keyArray = keys.split("\\$");
        List<Translation> translations = new ArrayList<>();
        for (String key : keyArray) {
            translations.addAll(MessagesTranslation.getTranslations(key));
        }
        return new TranslationList(translations);
    }
    
    public static class TranslationList {
        private final List<Translation> translations;
        
        public TranslationList(List<Translation> translations) {
            this.translations = translations;
        }
        
        /**
         * @return the translations
         */
        @JsonValue
        @JsonSerialize(using=TranslationListSerializer.class)
        public List<Translation> getTranslations() {
            return translations;
        }
    }
    
}
