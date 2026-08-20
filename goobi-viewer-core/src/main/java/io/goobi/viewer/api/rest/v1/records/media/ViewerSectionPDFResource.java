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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.servlet.model.PdfInformation;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfInfoBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.model.job.download.PdfDownloadJob;
import io.goobi.viewer.solr.SolrConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * @author Florian Alpers
 */
@Path(ApiUrls.RECORDS_SECTIONS)
@ContentServerBinding
public class ViewerSectionPDFResource {

    private static final Logger logger = LogManager.getLogger(ViewerSectionPDFResource.class);

    private final String pi;
    private final String divId;

    private final boolean usePdfSource;
    private String filename;

    private final HttpServletResponse response;

    /**
     * @param context JAX-RS container request context
     * @param request incoming HTTP servlet request
     * @param response outgoing HTTP servlet response
     * @param urls API URL manager for building resource URIs
     * @param pi persistent identifier of the record
     * @param divId logical div ID of the METS section
     * @param cacheManager content server cache manager
     * @throws ContentLibException
     */
    public ViewerSectionPDFResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context AbstractApiUrlManager urls,
            @Parameter(description = "Persistent identifier of the record",
                    schema = @Schema(pattern = "^[A-Za-z0-9][A-Za-z0-9_.-]*$")) @PathParam("pi") String pi,
            @Parameter(description = "Logical div ID of METS section",
                    schema = @Schema(pattern = "^[A-Za-z0-9_]+$")) @PathParam("divId") String divId,
            @Parameter(description = "allow using single page files from pdf folder to render pdf") @QueryParam("usePdfSource") Boolean usePdfSource)
            throws ContentLibException {
        // Validate PI before passing to MetsPdfResource which builds a file:// URI from the
        // value; illegal URI characters would cause a ContentLibException (HTTP 500).
        this.pi = pi;
        this.divId = divId;
        this.response = response;
        this.usePdfSource = Optional.ofNullable(usePdfSource).orElse(ContentServerConfiguration.getInstance().getUsePdf());
        String custom = StringTools.formatPdfDownloadFilename(
                DataManager.getInstance().getConfiguration().getDownloadFilenamePattern(), pi, divId);
        this.filename = custom != null ? custom : pi + "_" + divId + ".pdf";
        request.setAttribute(FilterTools.ATTRIBUTE_PI, pi);
        request.setAttribute(FilterTools.ATTRIBUTE_LOGID, divId);
    }

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
    public StreamingOutput getPdf() {
        ViewerMessage message = new ViewerMessage(PdfDownloadJob.TYPE);
        message.getProperties().put("pi", this.pi);
        message.getProperties().put("logId", this.divId);
        message.getProperties().put("usePdfSource", Boolean.toString(this.usePdfSource));
        PdfDownloadJob job = new PdfDownloadJob(message);

        response.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION,
                NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + StringTools.sanitizeFilenameToAscii(job.getDownloadFilename()) + "\"");

        return out -> {
            try {
                createPdf(job, out);
                response.setContentLengthLong(Files.size(job.getPath()));
            } catch (RecordNotFoundException e) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
            } catch (PresentationException | IndexUnreachableException | ContentLibException | IOException | URISyntaxException e) {
                throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
            }

        };

    }

    private void createPdf(PdfDownloadJob job, OutputStream out)
            throws IOException, ContentLibException, PresentationException, IndexUnreachableException, RecordNotFoundException, URISyntaxException {
        job.create(out);
        try (InputStream in = Files.newInputStream(job.getPath())) {
            IOUtils.copy(in, out);
        }
    }

    @GET
    @Path(ApiUrls.RECORDS_SECTIONS_PDF_INFO)
    @Produces({ MediaType.APPLICATION_JSON })
    @ContentServerPdfInfoBinding
    @Operation(tags = { "records" }, summary = "Get information about PDF for section of record")
    @ApiResponse(responseCode = "200", description = "PDF information object for the requested section",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier or section")
    @ApiResponse(responseCode = "404", description = "Record or section not found")
    @ApiResponse(responseCode = "500", description = "Error reading PDF information")
    public PdfInformation getInfoAsJson() throws ContentLibException {
        PdfInformation info = new PdfInformation();
        info.setTitle(pi);
        try {

            String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.LOGID + ":" + this.divId + " +" + SolrConstants.DOCTYPE
                    + ":PAGE";
            List<SolrDocument> pageDocs = DataManager.getInstance()
                    .getSearchIndex()
                    .getDocs(query, List.of(SolrConstants.MDNUM_FILESIZE));
            long totalBytes = 0;
            if (pageDocs != null) {
                for (SolrDocument doc : pageDocs) {
                    Object size = doc.getFieldValue(SolrConstants.MDNUM_FILESIZE);
                    if (size instanceof Number n) {
                        totalBytes += n.longValue();
                    }
                }
            }
            info.setSize(totalBytes);
        } catch (IndexUnreachableException | PresentationException e) {
            logger.warn("Could not get PDF size from Solr for PI '{}': {}", pi, e.toString());
        }
        return info;
    }

    /**
     * Validates the PI and returns it unchanged. Throws {@link BadRequestException} (HTTP 400) if the PI contains characters that are illegal in
     * java.net.URI paths or Solr queries.
     *
     * <p>
     * Declared static so it can be invoked inside the super() constructor call.
     *
     * @param pi persistent identifier to validate
     * @return the unchanged pi if valid
     */
    static String requireValidPi(String pi) {
        if (!PIValidator.validatePi(pi)) {
            throw new BadRequestException("Invalid record identifier: " + pi);
        }
        return pi;
    }

}
