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

import static io.goobi.viewer.api.rest.v1.ApiUrls.TEMP_MEDIA_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.TEMP_MEDIA_FILES_FILE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.TEMP_MEDIA_FILES_FOLDER;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.goobi.viewer.api.rest.bindings.AdminLoggedInBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.managedbeans.CreateRecordBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;

/**
 * Upload of resouces for DC record creation. Files uploaded here are directly written to a subfolder of the viewer hotfolder
 *
 * @author florian
 *
 */
@Hidden
@jakarta.ws.rs.Path(TEMP_MEDIA_FILES)
@ViewerRestServiceBinding
@AdminLoggedInBinding
public class TempMediaFileResource {

    private static final Logger logger = LogManager.getLogger(TempMediaFileResource.class);
    @Context
    protected HttpServletRequest servletRequest;
    @Context
    protected HttpServletResponse servletResponse;
    @Inject
    private ApiUrls urls;

    /**
     * Upload a file to the hotfolder.
     *
     * @param foldername
     * @param enabled
     * @param filename
     * @param uploadedInputStream
     * @param fileDetail
     * @return a json response with a result message
     */
    @POST
    @jakarta.ws.rs.Path(TEMP_MEDIA_FILES_FOLDER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Upload a media file to the temporary hotfolder for DC record creation", tags = { "media" })
    @ApiResponse(responseCode = "200", description = "File uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid filename or missing upload stream")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not authorized (admin login required)")
    @ApiResponse(responseCode = "500", description = "Internal error during file upload")
    public Response uploadMediaFiles(
            @Parameter(description = "Target folder name") @PathParam("folder") String foldername,
            @DefaultValue("true") @FormDataParam("enabled") boolean enabled,
            @FormDataParam("filename") String filename, @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {

        try {

            if (uploadedInputStream == null) {
                return Response.status(Status.NOT_ACCEPTABLE).entity(errorMessage("Upload stream is null")).build();
            }

            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("No bean found containing record data")).build();
            }

            Path targetDir = getTargetDir(foldername);
            if (!Files.isDirectory(targetDir)) {
                Files.createDirectories(targetDir);
            }
            if (!Files.isDirectory(targetDir)) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("No target directory for upload available")).build();
            }
            // Sanitize filename to prevent path traversal attacks (e.g. "../../etc/passwd")
            final String sanitizedFilename;
            try {
                sanitizedFilename = FileTools.sanitizeFileName(filename);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity(errorMessage("Invalid filename")).build();
            }
            Path targetFile = targetDir.resolve(sanitizedFilename);

            try {
                Files.copy(uploadedInputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

                if (Files.exists(targetFile) && Files.size(targetFile) > 0) {
                    return Response.status(Status.OK).entity(message("Successfully uploaded " + targetFile)).build();
                }
                throw new IOException("Uploaded file doesn't exist or is empty");
            } catch (IOException e) {
                String message = Messages.translate("admin__media_upload_error", servletRequest.getLocale(), targetFile.getFileName().toString());
                try {
                    if (Files.exists(targetFile)) {
                        Files.delete(targetFile);
                    }
                } catch (IOException e1) {
                    logger.error("Error deleting failed upload file {}", targetFile, e1);
                }
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage(message)).build();
            }
        } catch (IOException e) {
            logger.error("Error uploading file to folder {}", foldername, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error")).build();
        }
    }

    /**
     * Get a filename list of all uploaded files in the media directory of the given folder.
     *
     * @param folder
     * @return a filename list of all uploaded files in the media folder
     */
    @GET
    @jakarta.ws.rs.Path(TEMP_MEDIA_FILES_FOLDER)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all uploaded temporary media files in the given folder", tags = { "media" })
    @ApiResponse(responseCode = "200", description = "List of uploaded file URIs")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not authorized (admin login required)")
    @ApiResponse(responseCode = "500", description = "Internal error reading upload directory")
    public Response getUploadedFiles(@Parameter(description = "Target folder name") @PathParam("folder") String folder) {

        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("No bean found containing record data")).build();
            }

            List<URI> uploadedFiles = new ArrayList<>();
            Path targetDir = getTargetDir(folder);
            if (Files.isDirectory(targetDir)) {
                try (Stream<Path> stream = Files.list(targetDir)) {
                    uploadedFiles =
                            stream.map(this::getIiifUri).sorted((i1, i2) -> i1.toString().compareTo(i2.toString())).collect(Collectors.toList());
                } catch (IOException e) {
                    logger.error("Error reading upload directory {}", targetDir, e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity(errorMessage("Error reading upload directory"))
                            .build();
                }
            }
            try {
                String json = getAsJson(uploadedFiles);
                return Response.status(Status.OK).entity(json).build();
            } catch (JsonProcessingException e) {
                logger.error("Error serializing uploaded files list", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Error creating json object")).build();
            }

        } catch (IOException e) {
            logger.error("Error retrieving uploaded files: {}", e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error")).build();
        }
    }

    /**
     * Delete all files uploaded for the given folder.
     *
     * @param folder
     * @return a 200 response if deletion was successful, otherwise 500
     */
    @DELETE
    @jakarta.ws.rs.Path(TEMP_MEDIA_FILES_FOLDER)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete all uploaded temporary media files in the given folder", tags = { "media" })
    @ApiResponse(responseCode = "200", description = "All files deleted successfully")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not authorized (admin login required)")
    @ApiResponse(responseCode = "500", description = "Internal error deleting files")
    public Response deleteUploadedFiles(@Parameter(description = "Target folder name") @PathParam("folder") String folder) {

        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("No bean found containing record data").build();
            }

            Path targetDir = getTargetDir(folder);
            if (Files.isDirectory(targetDir)) {
                try (Stream<Path> stream = Files.list(targetDir)) {
                    List<Path> uploadedFiles = stream.collect(Collectors.toList());
                    for (Path file : uploadedFiles) {
                        Files.delete(file);
                    }
                } catch (IOException e) {
                    logger.error("Error deleting files in upload directory {}", targetDir, e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity(errorMessage("Error deleting upload directory contents"))
                            .build();
                }
            }
            return Response.status(Status.OK).build();
        } catch (IOException e) {
            logger.error("Error deleting uploaded files for folder {}", folder, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error")).build();
        }
    }

    public static String errorMessage(String string) {
        return message(string);
    }

    public static String message(String string) {
        try {
            return new ObjectMapper().writeValueAsString(Map.of("message", string));
        } catch (JsonProcessingException e) {
            return "{\"message\":\"internal error\"}";
        }
    }

    /**
     * Get the appropriate media subfolder for foldername in the viewer hotfolder.
     *
     * @param foldername
     * @return the folder for upload
     * @throws IOException
     */
    public static Path getTargetDir(String foldername) throws IOException {
        return Paths.get(DataManager.getInstance().getConfiguration().getViewerHome())
                .resolve(DataManager.getInstance().getConfiguration().getTempMediaFolder())
                .resolve(Paths.get(foldername).getFileName());
    }

    /**
     * @param file
     * @return {@link URI}
     */
    private URI getIiifUri(Path file) {
        String filename = file.getFileName().toString();
        String folder = file.getParent().getFileName().toString();
        String uri = urls.path(TEMP_MEDIA_FILES, TEMP_MEDIA_FILES_FILE).params(folder, filename).build();
        String iiifUri = BeanUtils.getImageDeliveryBean().getThumbs().getSquareThumbnailUrl(URI.create(uri));
        return URI.create(iiifUri);
    }

    private static String getAsJson(List list) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper.writeValueAsString(list);
    }
}
