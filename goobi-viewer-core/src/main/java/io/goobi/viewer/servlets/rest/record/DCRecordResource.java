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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
import com.fasterxml.jackson.databind.ObjectWriter;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.CreateRecordBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

/**
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

    @POST
    @javax.ws.rs.Path("/{uuid}/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadMediaFiles(@PathParam("uuid") String uuid, @DefaultValue("true") @FormDataParam("enabled") boolean enabled, @FormDataParam("filename") String filename,
            @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail)
            throws DAOException {

        try {

            if (uploadedInputStream == null) {
                return Response.status(Status.NOT_ACCEPTABLE).entity("Upload stream is null").build();
            }

            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("No bean found containing record data").build();
            }

            Path targetDir = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()).resolve(uuid);
            if (!Files.isDirectory(targetDir)) {
                Files.createDirectory(targetDir);
            }
            if (!Files.isDirectory(targetDir)) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("No target directory for upload available").build();
            }
            Path targetFile = targetDir.resolve(filename);

            try {
                Files.copy(uploadedInputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

                if (Files.exists(targetFile) && Files.size(targetFile) > 0) {
                    return Response.status(Status.OK).entity("Successfully uploaded " + targetFile).build();
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
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();
            }
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unknown error: " + e.toString()).build();
        }
    }

    @GET
    @javax.ws.rs.Path("/{uiid}/upload")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUploadedFiles(@PathParam("uuid") String uuid) {

        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("No bean found containing record data").build();
            }

            List<Path> uploadedFiles = new ArrayList<>();
            Path targetDir = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()).resolve(uuid);
            if (Files.isDirectory(targetDir)) {
                try (Stream<Path> stream = Files.list(targetDir)) {
                    uploadedFiles = stream.collect(Collectors.toList());
                } catch (IOException e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error reading upload directory: " + e.toString()).build();
                }
            }
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
                String json = mapper.writeValueAsString(uploadedFiles);
                return Response.status(Status.OK).entity(json).build();
            } catch (JsonProcessingException e) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error creating json object: " + e.toString()).build();

            }
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unknown error: " + e.toString()).build();
        }
    }

    @DELETE
    @javax.ws.rs.Path("/{uuid}/upload")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUploadedFiles(@PathParam("uuid") String uuid) {

        try {
            CreateRecordBean bean = BeanUtils.getCreateRecordBean();
            if (bean == null) {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("No bean found containing record data").build();
            }

            Path targetDir = Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()).resolve(uuid);
            if (Files.isDirectory(targetDir)) {
                try (Stream<Path> stream = Files.list(targetDir)) {
                    List<Path> uploadedFiles = stream.collect(Collectors.toList());
                    for (Path file : uploadedFiles) {
                        Files.delete(file);
                    }
                } catch (IOException e) {
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error reading upload directory: " + e.toString()).build();
                }
            }
            return Response.status(Status.OK).build();
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unknown error: " + e.toString()).build();
        }
    }

}
