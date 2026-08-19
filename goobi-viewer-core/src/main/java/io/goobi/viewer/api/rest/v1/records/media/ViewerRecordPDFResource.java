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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.goobi.presentation.contentServlet.controller.GetMetsPageCountAction;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibPdfException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.servlet.model.MetsPdfRequest;
import de.unigoettingen.sub.commons.contentlib.servlet.model.PdfInformation;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfInfoBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.bindings.RecordFileDownloadBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.model.job.download.PdfDownloadJob;
import io.goobi.viewer.model.viewer.Dataset;
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
 * REST resource for serving PDF representations of entire digitized records with access control.
 *
 * @author Florian Alpers
 */
@jakarta.ws.rs.Path(ApiUrls.RECORDS_RECORD)
@ContentServerBinding
public class ViewerRecordPDFResource {

    private static final Logger logger = LogManager.getLogger(ViewerRecordPDFResource.class);

    /** Maximum time to wait for a PDF that is currently being created by another thread/process before giving up on this request. */
    private static final int MAX_WAIT_FOR_LOCK_MILLIS = 90_000;

    private final String pi;
    private final boolean usePdfSource;

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ContentServerCacheManager cacheManager;

    public ViewerRecordPDFResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context AbstractApiUrlManager urls,
            @Parameter(description = "Persistent identifier of the record",
                    schema = @Schema(pattern = "^[A-Za-z0-9][A-Za-z0-9_.-]*$")) @PathParam("pi") String pi,
            @Parameter(description = "allow using single page files from pdf folder to render pdf") @QueryParam("usePdfSource") Boolean usePdfSource,
            @Context ContentServerCacheManager cacheManager) throws ContentLibException {
        this.pi = requireValidPi(pi);
        this.cacheManager = cacheManager;
        this.request = request;
        this.response = response;
        this.usePdfSource = Optional.ofNullable(usePdfSource).orElse(ContentServerConfiguration.getInstance().getUsePdf());
        request.setAttribute("pi", pi);
    }

    @GET
    @jakarta.ws.rs.Path(ApiUrls.RECORDS_PDF)
    @Produces("application/pdf")
    @ContentServerPdfBinding
    @RecordFileDownloadBinding
    @Operation(tags = { "records" }, summary = "Get PDF for entire record")
    @ApiResponse(responseCode = "200", description = "PDF file", content = @Content(mediaType = "application/pdf"))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "403", description = "Access to this record is restricted")
    @ApiResponse(responseCode = "404", description = "Record not found")
    @ApiResponse(responseCode = "500", description = "PDF generation error")
    @ApiResponse(responseCode = "503", description = "PDF is still being created by another request, retry later")
    public StreamingOutput getPdf()
            throws ContentLibException, PresentationException, IOException, IndexUnreachableException, RecordNotFoundException {
        ViewerMessage message = new ViewerMessage(PdfDownloadJob.TYPE);
        message.getProperties().put("pi", this.pi);
        message.getProperties().put("usePdfSource", Boolean.toString(this.usePdfSource));
        PdfDownloadJob job = new PdfDownloadJob(message);

        int waitedMillis = 0;
        while (!Files.exists(job.getPath())) {
            if (job.isLocked()) {
                //pdf is currently being created by someone else
                if (waitedMillis >= MAX_WAIT_FOR_LOCK_MILLIS) {
                    throw new WebApplicationException(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .header("Retry-After", "30")
                            .entity("PDF for '" + pi + "' is still being created, please retry later")
                            .build());
                }
                try {
                    Thread.sleep(1000);
                    waitedMillis += 1000;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new WebApplicationException(Response.status(Response.Status.SERVICE_UNAVAILABLE)
                            .header("Retry-After", "30")
                            .entity("PDF creation for '" + pi + "' was interrupted, please retry later")
                            .build());
                }
            } else {
                // No-op if another thread/process wins the race to acquire the lock in the meantime (see PdfDownloadJob#create)
                job.create();
            }
        }

        response.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION,
                NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + StringTools.sanitizeFilenameToAscii(job.getDownloadFilename()) + "\"");
        response.setContentLengthLong(Files.size(job.getPath()));

        return out -> {
            try (InputStream in = Files.newInputStream(job.getPath())) {
                IOUtils.copy(in, out);
            }
        };

    }

    @GET
    @jakarta.ws.rs.Path(ApiUrls.RECORDS_PDF_INFO)
    @Produces({ MediaType.APPLICATION_JSON })
    @ContentServerPdfInfoBinding
    @Operation(tags = { "records" }, summary = "Get information about PDF for entire record")
    @ApiResponse(responseCode = "200", description = "PDF information object",
            content = @Content(mediaType = MediaType.APPLICATION_JSON))
    @ApiResponse(responseCode = "400", description = "Invalid record identifier")
    @ApiResponse(responseCode = "404", description = "Record not found")
    @ApiResponse(responseCode = "500", description = "Error reading PDF information")
    public PdfInformation getInfoAsJson() throws ContentLibException {
        // ContentLib's MetsPdfResource.extractBaseURIs() appends File.separator ("\") to the METS
        // folder path before calling URI.create(), which fails on Windows with "Illegal character
        // in path". Use Solr MDNUM_FILESIZE fields instead — same approach as ActiveDocumentBean.getPdfSize().
        PdfInformation info = new PdfInformation();
        info.setTitle(pi);
        try {
            String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " +" + SolrConstants.DOCTYPE + ":PAGE";
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

    @GET
    @jakarta.ws.rs.Path(ApiUrls.RECORDS_EPUB_INFO)
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
            String cleanedPi = StringTools.cleanUserGeneratedData(this.pi);
            Dataset work = DataFileTools.getDataset(cleanedPi);
            MetsPdfRequest pdfRequest = PdfDownloadJob.createPdfRequest(work, Optional.empty(), false, cleanedPi);
            PdfInformation info = new GetMetsPageCountAction(ContentServerCacheManager.getInstance()).getEpubInfo(pdfRequest);

            return info;
        } catch (ContentLibPdfException | URISyntaxException | PresentationException | IndexUnreachableException | RecordNotFoundException
                | IOException e) {
            throw new ContentNotFoundException("Record not found: " + this.pi, e);
        }
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
