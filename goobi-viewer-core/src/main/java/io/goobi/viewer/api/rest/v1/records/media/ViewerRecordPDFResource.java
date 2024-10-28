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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.PdfInformation;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfInfoBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.MetsPdfResource;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.NetTools;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

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
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) throws ContentLibException {
        super(context, request, response, "pdf", pi + ".xml");
        this.filename = pi + ".pdf";
        request.setAttribute("pi", pi);
    }

    @Override
    @GET
    @Path(ApiUrls.RECORDS_PDF)
    @Produces("application/pdf")
    @ContentServerPdfBinding
    @Operation(tags = { "records" }, summary = "Get PDF for entire record")
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
    public PdfInformation getInfoAsJson() throws ContentLibException {
        return super.getInfoAsJson();
    }

    @GET
    @Path(ApiUrls.RECORDS_EPUB_INFO)
    @Produces({ MediaType.APPLICATION_JSON })
    @ContentServerPdfInfoBinding
    @Operation(tags = { "records" }, summary = "Get information about epub for entire record")
    public PdfInformation getEpubInfoAsJson() throws ContentLibException {
        return super.getInfo("epub");
    }

}
