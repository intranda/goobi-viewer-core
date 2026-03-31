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

import static io.goobi.viewer.api.rest.v1.ApiUrls.TEMP_MEDIA_FILES_FILE_IMAGE;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ImageResource;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.bindings.AdminLoggedInBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.managedbeans.CreateRecordBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@jakarta.ws.rs.Path(TEMP_MEDIA_FILES_FILE_IMAGE)
@CORSBinding
@AdminLoggedInBinding
public class TempMediaImageResource extends ImageResource {

    private static final Logger logger = LogManager.getLogger(TempMediaImageResource.class);

    public TempMediaImageResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Parameter(description = "Temp folder name") @PathParam("folder") String folder,
            @Parameter(description = "Filename of the image") @PathParam("filename") String filename,
            @Context ContentServerCacheManager cacheManager) {
        super(context, request, response, "", getMediaFileUrl(folder, filename).toString(), cacheManager);
        AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null);
        request.setAttribute("filename", this.imageURI.toString());
        String requestUrl = request.getRequestURI();
        String baseImageUrl = urls != null ? urls.path(ApiUrls.TEMP_MEDIA_FILES, ApiUrls.TEMP_MEDIA_FILES_FILE).params(folder, filename).build() : "";
        String imageRequestPath = requestUrl.replace(baseImageUrl, "");
        this.resourceURI = URI.create(baseImageUrl);

        List<String> parts = Arrays.stream(imageRequestPath.split("/")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if (parts.size() == 4) {
            //image request
            request.setAttribute("iiif-info", false);
            request.setAttribute("iiif-region", parts.get(0));
            request.setAttribute("iiif-size", parts.get(1));
            request.setAttribute("iiif-rotation", parts.get(2));
            request.setAttribute("iiif-format", parts.get(3));
        } else {
            //image info request
            request.setAttribute("iiif-info", true);
        }
    }

    /**
     * 
     * @param foldername
     * @param filename
     * @return {@link URI}
     */
    private static URI getMediaFileUrl(String foldername, String filename) {
        Path folder =
                Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                        DataManager.getInstance().getConfiguration().getTempMediaFolder(),
                        Paths.get(foldername).getFileName().toString());
        Path file = folder.resolve(Paths.get(filename).getFileName());
        return PathConverter.toURI(file);
    }

    @Override
    public void createResourceURI(HttpServletRequest request, String directory, String filename) throws IllegalRequestException {
        //don't do anyhting. The resource url has already been set in constructor
    }

    @Override
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_JSONLD })
    @ContentServerImageInfoBinding
    @Operation(tags = { "iiif" },
            summary = "IIIF image identifier for the CMS image file of the given filename. Returns a IIIF 2.1.1 image information object")
    @ApiResponse(responseCode = "200", description = "IIIF 2.1.1 image information object")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not authorized (admin login required)")
    @ApiResponse(responseCode = "404", description = "Temporary image not found or expired")
    public Response redirectToCanonicalImageInfo() throws ContentLibException {
        return super.redirectToCanonicalImageInfo();
    }

    /**
     * Delete the file with the given filename in the temp media folder for the given uuid.
     *
     * @param folder
     * @param filename
     * @return A 200 "OK" answer if deletion was successfull, 406 if the file was not found and 500 if there was an error
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete a temporary image file from the given folder", tags = { "media" })
    @ApiResponse(responseCode = "200", description = "File deleted successfully")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not authorized (admin login required)")
    @ApiResponse(responseCode = "406", description = "File not found")
    @ApiResponse(responseCode = "500", description = "Internal error deleting file")
    public Response deleteUploadedFile(
            @Parameter(description = "Temp folder name") @PathParam("folder") String folder,
            @Parameter(description = "Filename to delete") @PathParam("filename") String filename) {
        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity(TempMediaFileResource.errorMessage("No bean found containing record data"))
                        .build();
            }
            Path file = TempMediaFileResource.getTargetDir(folder).resolve(StringTools.cleanUserGeneratedData(filename));
            if (Files.exists(file)) {
                try {
                    Files.delete(file);
                    return Response.status(Status.OK).build();
                } catch (IOException e) {
                    logger.error("Error deleting uploaded file {} in folder {}", filename, folder, e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity(TempMediaFileResource.errorMessage("Error deleting uploaded file"))
                            .build();
                }
            }
            return Response.status(Status.NOT_ACCEPTABLE).entity(TempMediaFileResource.errorMessage("File doesn't exist")).build();
        } catch (IOException e) {
            logger.error("Error deleting uploaded file {} in folder {}", filename, folder, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(TempMediaFileResource.errorMessage("Unknown error")).build();
        }
    }

}
