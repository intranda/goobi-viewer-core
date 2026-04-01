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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import io.goobi.viewer.api.rest.bindings.RecordFileDownloadBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.faces.validators.PIValidator;
import jakarta.ws.rs.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
@Path(ApiUrls.RECORDS_RECORD)
@ContentServerBinding
public class ViewerRecordPDFResource extends MetsPdfResource {

    private static final Logger logger = LogManager.getLogger(ViewerRecordPDFResource.class);

    private String filename;

    public ViewerRecordPDFResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context AbstractApiUrlManager urls,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Context ContentServerCacheManager cacheManager) throws ContentLibException {
        // Validate PI before passing it to MetsPdfResource, which builds a file:// URI from
        // the value and throws ContentLibException (HTTP 500) on illegal URI characters.
        // requireValidPi() must be called here inside super() because Java requires the
        // super-constructor call to be the first statement.
        super(context, request, response, "pdf", requireValidPi(pi) + ".xml", cacheManager);
        this.filename = pi + ".pdf";
        request.setAttribute("pi", pi);
    }

    @Override
    @GET
    @Path(ApiUrls.RECORDS_PDF)
    @Produces("application/pdf")
    @ContentServerPdfBinding
    @RecordFileDownloadBinding
    @Operation(tags = { "records" }, summary = "Get PDF for entire record")
    @ApiResponse(responseCode = "200", description = "PDF file", content = @Content(mediaType = "application/pdf"))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "500", description = "PDF generation error")
    public StreamingOutput getPdf() throws ContentLibException {
        logger.trace("getPdf: {}", filename);
        response.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename + "\"");
        return super.getPdf();
    }

    @Override
    @GET
    @Path(ApiUrls.RECORDS_PDF_INFO)
    @Produces({ MediaType.APPLICATION_JSON })
    @ContentServerPdfInfoBinding
    @Operation(tags = { "records" }, summary = "Get information about PDF for entire record")
    @ApiResponse(responseCode = "200", description = "PDF information object",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "404", description = "Record not found")
    @ApiResponse(responseCode = "500", description = "Error reading PDF information")
    public PdfInformation getInfoAsJson() throws ContentLibException {
        // ContentLib wraps a missing METS file as ContentLibPdfException (not ContentNotFoundException),
        // which ContentExceptionMapper would map to HTTP 500. Rethrow as 404 instead.
        try {
            return super.getInfoAsJson();
        } catch (ContentLibPdfException e) {
            throw new ContentNotFoundException("Record not found: " + filename, e);
        }
    }

    @GET
    @Path(ApiUrls.RECORDS_EPUB_INFO)
    @Produces({ MediaType.APPLICATION_JSON })
    @ContentServerPdfInfoBinding
    @Operation(tags = { "records" }, summary = "Get information about epub for entire record")
    @ApiResponse(responseCode = "200", description = "ePub information object",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "404", description = "Record not found")
    @ApiResponse(responseCode = "500", description = "Error reading ePub information")
    public PdfInformation getEpubInfoAsJson() throws ContentLibException {
        // Same as getInfoAsJson(): rethrow ContentLibPdfException (missing METS) as 404.
        try {
            return super.getInfo("epub");
        } catch (ContentLibPdfException e) {
            throw new ContentNotFoundException("Record not found: " + filename, e);
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
