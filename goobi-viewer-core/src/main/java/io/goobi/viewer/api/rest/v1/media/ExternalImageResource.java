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
package io.goobi.viewer.api.rest.v1.media;

import static io.goobi.viewer.api.rest.v1.ApiUrls.EXTERNAL_IMAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE_PDF;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Region;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ImageResource;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.api.rest.filters.AccessConditionRequestFilter;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 *         Used to call ContentServer with external image resource URLs
 *
 */
@Path(EXTERNAL_IMAGES)
@ContentServerBinding
@AccessConditionBinding
@CORSBinding
public class ExternalImageResource extends ImageResource {

    private static final Logger logger = LogManager.getLogger(ExternalImageResource.class);

    /**
     * @param context
     * @param request
     * @param response
     * @param urls
     * @param imageUrl
     */
    public ExternalImageResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context ApiUrls urls,
            @Parameter(description = "URL of the image") @PathParam("filename") String imageUrl) {
        super(context, request, response, "", imageUrl);
        request.setAttribute(FilterTools.ATTRIBUTE_FILENAME, imageUrl);
        request.setAttribute(AccessConditionRequestFilter.REQUIRED_PRIVILEGE, IPrivilegeHolder.PRIV_VIEW_IMAGES);
        String requestUrl = request.getRequestURI();
        String baseImageUrl = EXTERNAL_IMAGES.replace("{filename}", imageUrl);
        int baseStartIndex = requestUrl.indexOf(baseImageUrl);
        int baseEndIndex = baseStartIndex + baseImageUrl.length();
        String imageRequestPath = requestUrl.substring(baseEndIndex);

        List<String> parts = Arrays.stream(imageRequestPath.split("/")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if (parts.size() == 4) {
            //image request
            String region = parts.get(0);
            String size = parts.get(1);
            Optional<Integer> scaleWidth = getRequestedWidth(size);
            request.setAttribute("iiif-info", false);
            request.setAttribute("iiif-region", region);
            request.setAttribute("iiif-size", size);
            request.setAttribute("iiif-rotation", parts.get(2));
            request.setAttribute("iiif-format", parts.get(3));
            int maxUnzoomedImageWidth = DataManager.getInstance().getConfiguration().getUnzoomedImageAccessMaxWidth();
            if (maxUnzoomedImageWidth > 0
                    && (!(Region.FULL_IMAGE.equals(region) || Region.SQUARE_IMAGE.equals(region))
                            || scaleWidth.orElse(Integer.MAX_VALUE) > maxUnzoomedImageWidth)) {
                request.setAttribute(AccessConditionRequestFilter.REQUIRED_PRIVILEGE,
                        new String[] { IPrivilegeHolder.PRIV_VIEW_IMAGES, IPrivilegeHolder.PRIV_ZOOM_IMAGES });
            }
        } else {
            //image info request
            request.setAttribute("iiif-info", true);
        }
    }

    @GET
    @Path(RECORDS_FILES_IMAGE_PDF)
    @Produces("application/pdf")
    @ContentServerPdfBinding
    @Operation(tags = { "records" }, summary = "Returns the image for the given filename as PDF")
    public StreamingOutput getPdf() throws ContentLibException {
        String pi = request.getAttribute("pi").toString();
        String filename = request.getAttribute("filename").toString();
        logger.trace("getPdf: {}/{}", pi, filename);
        filename = FilenameUtils.getBaseName(filename);
        filename = pi + "_" + filename + ".pdf";
        response.addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + filename + "\"");

        if (context.getProperty("param:metsSource") != null) {
            try {
                String metsSource = context.getProperty("param:metsSource").toString();
                String metsPath = PathConverter.getPath(PathConverter.toURI(metsSource)).resolve(pi + ".xml").toUri().toString();
                context.setProperty("param:metsFile", metsPath);
            } catch (URISyntaxException e) {
                logger.error("Failed to convert metsSource {} to METS URI", context.getProperty("metsSource"));
            }

        }

        return super.getPdf();
    }

    @Override
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_JSONLD })
    @ContentServerImageInfoBinding
    @Operation(tags = { "records", "iiif" }, summary = "IIIF image identifier for the given filename. Returns a IIIF 2.1.1 image information object")
    public Response redirectToCanonicalImageInfo() throws ContentLibException {
        return super.redirectToCanonicalImageInfo();
    }

    @Override
    public void createResourceURI(HttpServletRequest request, String directory, String filename) throws IllegalRequestException {
        super.createResourceURI(request, directory, filename);
        try {
            String toReplace = URLEncoder.encode("{pi}", "UTF-8");
            this.resourceURI = URI.create(this.resourceURI.toString().replace(toReplace, directory));
        } catch (UnsupportedEncodingException e) {
            //
        }
    }

}
