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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import io.goobi.viewer.managedbeans.CreateRecordBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;

/**
 * Upload of resouces for DC record creation. Files uploaded here are directly written to a subfolder of the viewer hotfolder
 *
 * @author florian
 *
 */
@javax.ws.rs.Path(TEMP_MEDIA_FILES)
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
    @javax.ws.rs.Path(TEMP_MEDIA_FILES_FOLDER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadMediaFiles(@PathParam("folder") String foldername, @DefaultValue("true") @FormDataParam("enabled") boolean enabled,
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
            Path targetFile = targetDir.resolve(filename);

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
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error: " + e.toString())).build();
        }
    }

    /**
     * Get a filename list of all uploaded files in the media directory of the given folder.
     *
     * @param folder
     * @return a filename list of all uploaded files in the media folder
     */
    @GET
    @javax.ws.rs.Path(TEMP_MEDIA_FILES_FOLDER)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUploadedFiles(@PathParam("folder") String folder) {

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
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity(errorMessage("Error reading upload directory: " + e.toString()))
                            .build();
                }
            }
            try {
                String json = getAsJson(uploadedFiles);
                return Response.status(Status.OK).entity(json).build();
            } catch (JsonProcessingException e) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Error creating json object: " + e.toString())).build();
            }

        } catch (IOException e) {
            logger.error("Error retgrieving uploaded files: {}", e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error: " + e.toString())).build();
        }
    }

    /**
     * Delete all files uploaded for the given folder.
     *
     * @param folder
     * @return a 200 response if deletion was successful, otherwise 500
     */
    @DELETE
    @javax.ws.rs.Path(TEMP_MEDIA_FILES_FOLDER)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUploadedFiles(@PathParam("folder") String folder) {

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
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity(errorMessage("Error reading upload directory: " + e.toString()))
                            .build();
                }
            }
            return Response.status(Status.OK).build();
        } catch (IOException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error: " + e.toString())).build();
        }
    }

    public static String errorMessage(String string) {
        return message(string);
    }

    public static String message(String string) {
        return "{message: \"" + string + "\"}";
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
