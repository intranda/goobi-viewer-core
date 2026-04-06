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
package io.goobi.viewer.api.rest.v1.records.media;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibPdfException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.PdfInformation;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfInfoBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.MetsPdfResource;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.faces.validators.PIValidator;
import jakarta.ws.rs.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * @author florian
 *
 */
@Path(ApiUrls.RECORDS_SECTIONS)
@ContentServerBinding
public class ViewerSectionPDFResource extends MetsPdfResource {

    private final String divId;
    private String filename;

    /**
     * @param context
     * @param request
     * @param response
     * @param urls
     * @param pi
     * @param divId
     * @param cacheManager
     * @throws ContentLibException
     */
    public ViewerSectionPDFResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context AbstractApiUrlManager urls,
            @Parameter(description = "Persistent identifier of the record", schema = @Schema(pattern = "^[A-Za-z0-9][A-Za-z0-9_.-]*$")) @PathParam("pi") String pi,
            @Parameter(description = "Logical div ID of METS section",
                    schema = @Schema(pattern = "^[A-Za-z0-9_]+$")) @PathParam("divId") String divId,
            @Context ContentServerCacheManager cacheManager) throws ContentLibException {
        // Validate PI before passing to MetsPdfResource which builds a file:// URI from the
        // value; illegal URI characters would cause a ContentLibException (HTTP 500).
        super(context, request, response, "pdf", requireValidPi(pi) + ".xml", cacheManager);
        this.divId = divId;
        this.filename = pi + "_" + divId + ".pdf";
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
        request.setAttribute(FilterTools.ATTRIBUTE_LOGID, divId);
    }

    @Override
    @GET
    @Path(ApiUrls.RECORDS_SECTIONS_PDF)
    @Produces("application/pdf")
    @ContentServerPdfBinding
    @Operation(tags = { "records" }, summary = "Get PDF for section of record")
    @ApiResponse(responseCode = "200", description = "PDF file for the requested section",
            content = @Content(mediaType = "application/pdf"))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or section")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "Record or section not found")
    @ApiResponse(responseCode = "500", description = "PDF generation error")
    public StreamingOutput getPdf() throws ContentLibException {
        response.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename + "\"");
        return super.getPdf(divId);
    }

    @GET
    @Path(ApiUrls.RECORDS_SECTIONS_PDF_INFO)
    @Produces({ MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_JSONLD })
    @ContentServerPdfInfoBinding
    @Override
    @Operation(tags = { "records" }, summary = "Get information about PDF for section of record")
    @ApiResponse(responseCode = "200", description = "PDF information object for the requested section",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or section")
    @ApiResponse(responseCode = "404", description = "Record or section not found")
    @ApiResponse(responseCode = "500", description = "Error reading PDF information")
    public PdfInformation getInfoAsJson() throws ContentLibException {
        // ContentLib wraps a missing METS file as ContentLibPdfException (not ContentNotFoundException),
        // which ContentExceptionMapper would map to HTTP 500. Rethrow as 404 instead.
        try {
            return super.getInfoAsJson(divId);
        } catch (ContentLibPdfException e) {
            throw new ContentNotFoundException("Record or section not found: " + filename, e);
        }
    }

    /**
     * Validates the PI and returns it unchanged. Throws {@link BadRequestException} (HTTP 400)
     * if the PI contains characters that are illegal in java.net.URI paths or Solr queries.
     * Declared static so it can be invoked inside the super() constructor call.
     */
    static String requireValidPi(String pi) {
        if (!PIValidator.validatePi(pi)) {
            throw new BadRequestException("Invalid record identifier: " + pi);
        }
        return pi;
    }

}
