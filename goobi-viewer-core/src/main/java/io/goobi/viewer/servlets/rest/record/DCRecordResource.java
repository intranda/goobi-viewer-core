/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.servlets.rest.record;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.CreateRecordBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.servlets.rest.MediaItem;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

/**
 * Upload of resouces for DC record creation. Files uploaded here are directly written to a subfolder of the viewer hotfolder
 * 
 * @author florian
 *
 */
@javax.ws.rs.Path("/record/dc")
@ViewerRestServiceBinding
public class DCRecordResource {

    private static final Logger logger = LoggerFactory.getLogger(DCRecordResource.class);
    @Context
    protected HttpServletRequest servletRequest;
    @Context
    protected HttpServletResponse servletResponse;

    /**
     * Upload a file to the hotfolder
     * 
     * @param uuid
     * @param enabled
     * @param filename
     * @param uploadedInputStream
     * @param fileDetail
     * @return a json response with a result message
     * @throws DAOException
     */
    @POST
    @javax.ws.rs.Path("/{uuid}/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadMediaFiles(@PathParam("uuid") String uuid, @DefaultValue("true") @FormDataParam("enabled") boolean enabled,
            @FormDataParam("filename") String filename,
            @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail)
            throws DAOException {

        try {

            if (uploadedInputStream == null) {
                return Response.status(Status.NOT_ACCEPTABLE).entity(errorMessage("Upload stream is null")).build();
            }

            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("No bean found containing record data")).build();
            }

            Path targetDir = getTargetDir(uuid);
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
                } else {
                    throw new IOException("Uploaded file doesn't exist or is empty");
                }
            } catch (IOException e) {
                String message = Messages.translate("admin__media_upload_error", servletRequest.getLocale(), targetFile.getFileName().toString());
                try {
                    if (Files.exists(targetFile)) {
                        Files.delete(targetFile);
                    }
                } catch (IOException e1) {
                    logger.error("Error deleting failed upload file " + targetFile, e1);
                }
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage(message)).build();
            }
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error: " + e.toString())).build();
        }
    }

    /**
     * Return a json object with the filename of the uploaded image denoted by filename if it exists. Otherwise an empty json object
     * 
     * @param uuid
     * @param filename
     * @return a json object with the filename of the uploaded image denoted by filename if it exists. Otherwise an empty json object
     */
    @GET
    @javax.ws.rs.Path("/{uuid}/upload/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUploadedFiles(@PathParam("uuid") String uuid, @PathParam("filename") String filename) {
        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("No bean found containing record data")).build();
            }

            Path file = getTargetDir(uuid).resolve(filename);
            if (Files.exists(file)) {
                URI uri = getIiifUri(file);
                List<URI> uploadedFiles = Collections.singletonList(uri);
                String jsonItem = getAsJson(uploadedFiles);
                return Response.status(Status.OK).entity(jsonItem).build();
            } else {
                return Response.status(Status.OK).entity("{}").build();
            }
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error: " + e.toString())).build();
        }
    }

    /**
     * @param file
     * @return
     */
    private URI getIiifUri(Path file) {

        String uri = BeanUtils.getImageDeliveryBean().getThumbs().getSquareThumbnailUrl(file);
        return URI.create(uri);
    }

    private String getAsJson(List list) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        String json = mapper.writeValueAsString(list);
        return json;
    }

    /**
     * Get a filename list of all uploaded files in the media directory of the given uuid
     * 
     * @param uuid
     * @return a filename list of all uploaded files in the media directory of the given uuid
     */
    @GET
    @javax.ws.rs.Path("/{uuid}/upload")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUploadedFiles(@PathParam("uuid") String uuid) {

        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("No bean found containing record data")).build();
            }

            List<URI> uploadedFiles = new ArrayList<>();
            Path targetDir = getTargetDir(uuid);
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

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error: " + e.toString())).build();
        }
    }

    /**
     * Delete all files uploaded for the given uuid
     * 
     * @param uuid
     * @return a 200 response if deletion was successfull, otherwise 500
     */
    @DELETE
    @javax.ws.rs.Path("/{uuid}/upload")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUploadedFiles(@PathParam("uuid") String uuid) {

        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("No bean found containing record data").build();
            }

            Path targetDir = getTargetDir(uuid);
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
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error: " + e.toString())).build();
        }
    }

    /**
     * Delete the file with the given filename in the temp media folder for the given uuid
     * 
     * @param uuid
     * @param filename
     * @return  A 200 "OK" answer if deletion was successfull, 406 if the file was not found and 500 if there was an error
     */
    @DELETE
    @javax.ws.rs.Path("/{uuid}/upload/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUploadedFile(@PathParam("uuid") String uuid, @PathParam("filename") String filename) {
        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("No bean found containing record data")).build();
            }

            Path file = getTargetDir(uuid).resolve(filename);
            if (Files.exists(file)) {
                try {
                    Files.delete(file);
                    return Response.status(Status.OK).build();
                } catch (IOException e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity(errorMessage("Error reading upload directory: " + e.toString()))
                            .build();
                }
            } else {
                return Response.status(Status.NOT_ACCEPTABLE).entity(errorMessage("File doesn't exist")).build();
            }
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(errorMessage("Unknown error: " + e.toString())).build();
        }
    }

    private String errorMessage(String string) {
        return message(string);
    }

    private String message(String string) {
        return "{message: \"" + string + "\"}";
    }

    /**
     * Get the appropriate media subfolder for the given uuid in the viewer hotfolder
     * 
     * @param uuid
     * @return the folder for upload
     * @throws IOException
     */
    private Path getTargetDir(String uuid) throws IOException {
        Path targetDir = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome())
                .resolve(DataManager.getInstance().getConfiguration().getTempMediaFolder())
                .resolve(uuid + "_tif");
        return targetDir;
    }

}
