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
package io.goobi.viewer.api.rest.v2.records;

import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_SECTIONS;
import static io.goobi.viewer.api.rest.v2.ApiUrls.RECORDS_SECTIONS_RANGE;

import java.net.URISyntaxException;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.iiif.presentation.v3.Range3;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.iiif.presentation.v3.builder.RangeBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(RECORDS_SECTIONS)
@ViewerRestServiceBinding
@CORSBinding
public class RecordSectionsResource {

    private static final Logger logger = LogManager.getLogger(RecordSectionsResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    private final String pi;
    private final String divId;

    public RecordSectionsResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Identifier of the section") @PathParam("divId") String divId) {
        this.pi = pi;
        this.divId = divId;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
        request.setAttribute(FilterTools.ATTRIBUTE_LOGID, divId);
    }

    @GET
    @javax.ws.rs.Path(RECORDS_SECTIONS_RANGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "IIIF 3.0 range of the section")
    @IIIFPresentationBinding
    public Range3 getRange()
            throws IndexUnreachableException, URISyntaxException, ViewerConfigurationException,
            DAOException, IllegalPathSyntaxException, ContentLibException {
        try {
            return new RangeBuilder(urls).build(pi, divId);
        } catch (PresentationException e) {
            throw new WebApplicationException(e);
        }
    }

}
