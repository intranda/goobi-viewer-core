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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
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
import jakarta.ws.rs.BadRequestException;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

/**
 * @author florian
 *
 *         Used to call ContentServer with external image resource URLs
 */
@Path(EXTERNAL_IMAGES)
@ContentServerBinding
@AccessConditionBinding
@CORSBinding
public class ExternalImageResource extends ImageResource {

    private static final Logger logger = LogManager.getLogger(ExternalImageResource.class);

    /**
     * Validates the decoded image URL before passing it to the parent constructor.
     *
     * <p>Must be static so it can be called within the super() argument expression.
     * Throws BadRequestException (HTTP 400) for non-ASCII or bare-percent filenames.
     *
     * @param imageUrl the decoded image URL to validate
     * @return the unchanged imageUrl if valid
     */
    // Package-private for testing
    static String validateImageUrl(String imageUrl) {
        // Reject non-ASCII filenames: the schema pattern ^[ !-$&-~]+$ requires printable ASCII only,
        // excluding '%' (0x25) which signals invalid double-encoding and causes URI parsing errors.
        if (imageUrl != null && !imageUrl.matches("[ !-$&-~]+")) {
            throw new BadRequestException("Invalid filename: must contain only printable ASCII characters");
        }
        // Reject filenames with bare '%' (result of double-encoding like %2B%254 → +%4).
        // A bare '%' in the decoded filename causes URI.create() to throw IllegalArgumentException → HTTP 500.
        if (imageUrl != null && imageUrl.contains("%")) {
            throw new BadRequestException("Invalid filename: contains a literal '%' character");
        }
        return imageUrl;
    }

    /**
     * @param context JAX-RS container request context
     * @param request current HTTP servlet request
     * @param response current HTTP servlet response
     * @param urls configured API URL manager
     * @param imageUrl URL-encoded filename/URL of the external image
     * @param cacheManager content server cache manager
     */
    public ExternalImageResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context ApiUrls urls, @Parameter(description = "URL of the image",
                    // Pattern excludes '%' (0x25) to reject double-encoded filenames that would
                    // cause URI parsing errors; valid range is space-'$' (0x20-0x24) + '&'-'~' (0x26-0x7E)
                    schema = @Schema(pattern = "^[ !-$&-~]+$")) @PathParam("filename") String imageUrl,
            @Context ContentServerCacheManager cacheManager) {
        // Validate imageUrl BEFORE passing it to the parent constructor, because the parent tries to
        // build a URI from it, which throws IllegalArgumentException (→ HTTP 500) on bare '%' signs
        // or non-ASCII characters. validateImageUrl() converts those cases to HTTP 400 instead.
        super(context, request, response, "", validateImageUrl(imageUrl), cacheManager);
        request.setAttribute(FilterTools.ATTRIBUTE_FILENAME, imageUrl);
        request.setAttribute(AccessConditionRequestFilter.REQUIRED_PRIVILEGE, IPrivilegeHolder.PRIV_VIEW_IMAGES);
        // Use the URL-decoded request path for indexOf so that chars like '>' (received as '%3E')
        // are compared in their decoded form against the decoded imageUrl.  request.getRequestURI()
        // returns the raw (percent-encoded) path, which would fail to match when the filename
        // contains characters that browsers percent-encode but we have already decoded.
        String rawRequestUrl = request.getRequestURI();
        String requestUrl;
        try {
            // Use URI.getPath() for proper RFC 3986 path decoding instead of URLDecoder,
            // which treats '+' as a space (form-encoding semantics). In URL paths '+' is
            // a literal plus sign, so URLDecoder causes indexOf mismatches for filenames
            // containing '+' (e.g. %2B-encoded). URI.getPath() decodes only %XX sequences.
            requestUrl = new java.net.URI(rawRequestUrl).getPath();
        } catch (java.net.URISyntaxException | IllegalArgumentException e) {
            // Malformed URI — reject with 400
            throw new BadRequestException("Invalid request URI: " + e.getMessage());
        }
        String baseImageUrl = EXTERNAL_IMAGES.replace("{filename}", imageUrl);
        int baseStartIndex = requestUrl.indexOf(baseImageUrl);
        // Safety check: if the decoded path still does not contain the decoded filename, the
        // request URI is inconsistent (should not happen after URL-decoding).
        if (baseStartIndex < 0) {
            throw new BadRequestException("Invalid filename: cannot locate filename in request URI");
        }
        int baseEndIndex = baseStartIndex + baseImageUrl.length();
        String imageRequestPath = requestUrl.substring(baseEndIndex);

        List<String> parts = Arrays.stream(imageRequestPath.split("/")).filter(StringUtils::isNotBlank).toList();
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

    @Override
    @GET
    @Path(RECORDS_FILES_IMAGE_PDF)
    @Produces("application/pdf")
    @ContentServerPdfBinding
    @Operation(tags = { "records" }, summary = "Returns the image for the given filename as PDF")
    @ApiResponse(responseCode = "200", description = "PDF document")
    @ApiResponse(responseCode = "400", description = "Invalid filename (e.g. non-ASCII characters)")
    // 403 is returned as application/json when access to the resource is restricted
    @ApiResponse(responseCode = "403", description = "Access denied due to access conditions")
    @ApiResponse(responseCode = "404", description = "Image or record not found")
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
    @ApiResponse(responseCode = "200", description = "IIIF image information object")
    @ApiResponse(responseCode = "400", description = "Invalid filename (non-ASCII or malformed URI)")
    @ApiResponse(responseCode = "403", description = "Access denied due to access conditions")
    // The external image server may return an HTML error page that the proxy passes through,
    // hence text/html is a possible content type for 404 responses.
    @ApiResponse(responseCode = "404", description = "External image not found (may be returned as text/html by the upstream server)")
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
        } catch (IllegalArgumentException e) {
            // The directory (decoded filename) contains characters that are invalid in a URI
            // (e.g. a bare '%' from double-encoded percent-sign input like %2B%254 → +%4).
            // Convert to IllegalRequestException so the framework returns HTTP 400 instead of 500.
            throw new IllegalRequestException("Invalid filename: cannot build resource URI — " + e.getMessage());
        }
    }

}
