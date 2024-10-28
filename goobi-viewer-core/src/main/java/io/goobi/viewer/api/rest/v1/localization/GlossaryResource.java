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

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.glossary.Glossary;
import io.goobi.viewer.model.glossary.GlossaryManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * <p>
 * GlossaryResource class.
 * </p>
 *
 * @author Florian Alpers
 */
@Path(ApiUrls.LOCALIZATION)
@ViewerRestServiceBinding
public class GlossaryResource {

    private static final Logger logger = LogManager.getLogger(GlossaryResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    /**
     * <p>
     * listVocabularies.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws java.io.IOException if any.
     */
    @GET
    @Path(ApiUrls.LOCALIZATION_VOCABS)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "localization" }, summary = "Get a list of all glossaries")
    public List<Glossary> listVocabularies() throws IOException {
        return new GlossaryManager().getGlossaries();
    }

    /**
     * <p>
     * getVocabulary.
     * </p>
     *
     * @param filename a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path(ApiUrls.LOCALIZATION_VOCABS_FILE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "localization" }, summary = "Get glossary from a glossary file")
    @ApiResponse(responseCode = "404", description = "Not vocabulary found with that filename")
    public String getVocabulary(@PathParam("filename") @Parameter(description = "Glossary filename") String filename)
            throws IOException, ContentNotFoundException {
        return new GlossaryManager().getGlossaryAsJson(StringTools.cleanUserGeneratedData(filename));

    }

}
