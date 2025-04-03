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
package io.goobi.viewer.api.rest.v1.localization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.serialization.TranslationListSerializer;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.messages.MessagesTranslation;
import io.goobi.viewer.model.translations.Translation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * <p>
 * TranslationResource class.
 * </p>
 *
 * @author florian
 */
@Path(ApiUrls.LOCALIZATION)
@ViewerRestServiceBinding
public class TranslationResource {

    private static final Logger logger = LogManager.getLogger(TranslationResource.class);

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param inKeys a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.api.rest.v1.localization.TranslationResource.TranslationList} object.
     * @throws IllegalRequestException
     */
    @GET
    @Path(ApiUrls.LOCALIZATION_TRANSLATIONS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "localization" }, summary = "Get translations for message keys",
            description = "Pass a list of message keys to get translations for all configured languages")
    @ApiResponse(responseCode = "200", description = "Return translations for given keys")
    @ApiResponse(responseCode = "400", description = "No keys passed")
    public TranslationList getTranslations(
            @QueryParam("keys") @Parameter(description = "A comma separated list of message keys") final String inKeys)
            throws IllegalRequestException {
        String keys = StringTools.stripPatternBreakingChars(inKeys);

        Collection<String> keysCollection;
        if (StringUtils.isBlank(keys)) {
            throw new IllegalRequestException("Must provide query parameter 'keys'");
        }
        keysCollection = Arrays.asList(keys.split(","));

        List<Translation> translations = new ArrayList<>();
        for (String key : keysCollection) {
            translations.addAll(MessagesTranslation.getTranslations(key.trim()));
        }
        return new TranslationList(translations);
    }

    public static class TranslationList {
        private final List<Translation> translations;

        public TranslationList(List<Translation> translations) {
            this.translations = translations;
        }

        public TranslationList() {
            this.translations = new ArrayList<>();
        }

        /**
         * @return the translations
         */
        @JsonValue
        @JsonSerialize(using = TranslationListSerializer.class)
        public List<Translation> getTranslations() {
            return translations;
        }
    }

}
