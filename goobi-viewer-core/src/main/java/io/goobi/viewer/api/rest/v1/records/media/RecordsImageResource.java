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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_IMAGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_IMAGE_IIIF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_IMAGE_INFO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerResource;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.solr.SolrConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@Path(RECORDS_RECORD)
public class RecordsImageResource {

    private static final Logger logger = LogManager.getLogger(RecordsImageResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final String pi;

    /**
     * @param pi
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public RecordsImageResource(
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi) {
        this.pi = pi;

    }

    @GET
    @Path(RECORDS_IMAGE)
    @Produces({ MediaType.APPLICATION_JSON, ContentServerResource.MEDIA_TYPE_APPLICATION_JSONLD })
    @Operation(
            summary = "IIIF image identifier for the representative image of the process given by the identifier."
                    + " Returns a IIIF 2.1.1 image information object",
            tags = { "iiif", "records" })
    @ApiResponse(responseCode = "200", description = "Get the IIIF image information object as json")
    @ApiResponse(responseCode = "404", description = "Either the record or the file for the representative image doesn't exist")
    @ApiResponse(responseCode = "500", description = "Internal error reading image or querying index")
    public Response getImageBase() throws URISyntaxException {
        String forwardUrl = new ApiUrls(ApiUrls.API).path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_IMAGE_INFO).params(pi).build();
        return Response.seeOther(PathConverter.toURI(servletRequest.getContextPath() + forwardUrl))
                .header("Content-Type", servletResponse.getContentType())
                .build();
    }

    @GET
    @Path(RECORDS_IMAGE_INFO)
    @Produces({ MediaType.APPLICATION_JSON, ContentServerResource.MEDIA_TYPE_APPLICATION_JSONLD })
    public String getImageInfo() throws PresentationException, IndexUnreachableException, ServletException, IOException, ContentNotFoundException {
        String filename = getRepresentativeFilename(pi);
        String forwardUrl = new ApiUrls(ApiUrls.API).path(ApiUrls.RECORDS_FILES_IMAGE, ApiUrls.RECORDS_FILES_IMAGE_INFO).params(pi, filename).build();
        RequestDispatcher dispatcher = servletRequest.getRequestDispatcher(forwardUrl);
        dispatcher.forward(servletRequest, servletResponse);
        return "";
    }

    @GET
    @Path(RECORDS_IMAGE_IIIF)
    @Produces({ "image/jpg", "image/png", "image/tif" })
    public String getImage(
            @PathParam("region") String region, @PathParam("size") String size,
            @PathParam("rotation") String rotation, @PathParam("quality") String quality,
            @PathParam("format") String format)
            throws PresentationException, IndexUnreachableException, ServletException, IOException, ContentNotFoundException {
        String filename = getRepresentativeFilename(pi);
        String forwardUrl = new ApiUrls(ApiUrls.API).path(ApiUrls.RECORDS_FILES_IMAGE, ApiUrls.RECORDS_FILES_IMAGE_IIIF)
                .params(pi, filename, region, size, rotation, quality, format)
                .build();
        RequestDispatcher dispatcher = servletRequest.getRequestDispatcher(forwardUrl);
        dispatcher.forward(servletRequest, servletResponse);
        return "";
    }

    /**
     * @param pi
     * @return Representative image file name for the given record pi
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ContentNotFoundException
     */
    private static String getRepresentativeFilename(String pi) throws PresentationException, IndexUnreachableException, ContentNotFoundException {
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(pi);
        if (doc == null) {
            throw new ContentNotFoundException("Not record found with identifier " + pi);
        } else if (doc.containsKey(SolrConstants.THUMBNAIL)) {
            return (String) doc.getFieldValue(SolrConstants.THUMBNAIL);
        } else {
            SolrDocumentList docs = DataManager.getInstance()
                    .getSearchIndex()
                    .search("+PI_TOPSTRUCT:" + pi + " +DOCTYPE:PAGE +ORDER:1", Collections.singletonList(SolrConstants.FILENAME));
            if (docs != null && !docs.isEmpty()) {
                SolrDocument firstPage = docs.get(0);
                return (String) firstPage.getFieldValue(SolrConstants.FILENAME);
            }
        }
        
        return "";
    }

}
