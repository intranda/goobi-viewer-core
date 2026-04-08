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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RANGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RIS_FILE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_SECTIONS_RIS_TEXT;

import java.net.URISyntaxException;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.bindings.IIIFPresentationBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.resourcebuilders.IIIFPresentation2ResourceBuilder;
import io.goobi.viewer.api.rest.resourcebuilders.RisResourceBuilder;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author Florian Alpers
 */
@jakarta.ws.rs.Path(RECORDS_SECTIONS)
@ViewerRestServiceBinding
@CORSBinding
public class RecordSectionResource {

    private static final Logger logger = LogManager.getLogger(RecordSectionResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    private final String pi;
    private final String divId;

    public RecordSectionResource(@Context HttpServletRequest request,
            @Parameter(description = "Persistent identifier of the record",
                    schema = @Schema(pattern = "^[A-Za-z0-9][A-Za-z0-9_.-]*$")) @PathParam("pi") String pi,
            @Parameter(description = "Logical div ID of METS section",
                    schema = @Schema(pattern = "^[A-Za-z0-9_]+$")) @PathParam("divId") String divId) {
        // Reject PIs containing characters illegal in URI paths / Solr queries before any
        // Solr or file-system access occurs.  BadRequestException (HTTP 400) is an unchecked
        // WebApplicationException that Jersey maps to 400 before invoking the endpoint.
        if (!PIValidator.validatePi(pi)) {
            throw new BadRequestException("Invalid record identifier: " + pi);
        }
        // Enforce the divId pattern documented in the OpenAPI spec: alphanumeric and
        // underscores only. Values like "-3.349e+52" would cause Solr syntax errors.
        if (!divId.matches("[A-Za-z0-9_]+")) {
            throw new BadRequestException("Invalid section identifier: " + divId);
        }
        this.pi = pi;
        this.divId = divId;
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
        request.setAttribute(FilterTools.ATTRIBUTE_LOGID, divId);

    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_SECTIONS_RIS_FILE)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Download ris as file")
    @ApiResponse(responseCode = "200", description = "RIS citation for the section downloaded as plain text file")
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "404", description = "Section not found for the given identifiers")
    public String getRISAsFile()
            throws PresentationException, IndexUnreachableException, DAOException, ContentLibException {

        StructElement se = getStructElement(pi, divId);
        String fileName = se.getPi() + "_" + se.getLogid() + ".ris";
        servletResponse.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + fileName + "\"");
        return new RisResourceBuilder(servletRequest, servletResponse).getRIS(se);
    }

    /**
     * getRISAsText.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @jakarta.ws.rs.Path(RECORDS_SECTIONS_RIS_TEXT)
    @Produces({ MediaType.TEXT_PLAIN })
    @Operation(tags = { "records" }, summary = "Get ris as text")
    @ApiResponse(responseCode = "200", description = "RIS citation for the section as plain text")
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "404", description = "Section not found for the given identifiers")
    public String getRISAsText()
            throws PresentationException, IndexUnreachableException, ContentNotFoundException, DAOException {

        StructElement se = getStructElement(pi, divId);
        return new RisResourceBuilder(servletRequest, servletResponse).getRIS(se);
    }

    @GET
    @jakarta.ws.rs.Path(RECORDS_SECTIONS_RANGE)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "records", "iiif" }, summary = "Get IIIF 2.1.1 range for section")
    @ApiResponse(responseCode = "200", description = "IIIF 2.1.1 range for the requested section")
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "Section not found for the given identifiers")
    @IIIFPresentationBinding
    public IPresentationModelElement getRange() throws ContentNotFoundException, PresentationException, IndexUnreachableException, URISyntaxException,
            ViewerConfigurationException, DAOException {
        IIIFPresentation2ResourceBuilder builder = new IIIFPresentation2ResourceBuilder(urls, servletRequest);
        return builder.getRange(pi, divId);
    }

    /**
     * @param pi persistent identifier of the record
     * @param divId logical div ID of the METS section
     * @return {@link StructElement}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private static StructElement getStructElement(String pi, String divId)
            throws PresentationException, IndexUnreachableException, ContentNotFoundException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc("+PI_TOPSTRUCT:" + pi + " +DOCTYPE:DOCSTRCT +LOGID:" + divId, null);
        // Guard against NPE when no matching section is found in the index
        if (doc == null) {
            throw new ContentNotFoundException("No section found for PI: " + pi + ", divId: " + divId);
        }
        return new StructElement((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
    }

}
