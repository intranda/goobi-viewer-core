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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_3D;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_3D_AUXILIARY_FILE_1;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_3D_AUXILIARY_FILE_1_ALT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_3D_AUXILIARY_FILE_2;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_3D_AUXILIARY_FILE_2_ALT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_3D_INFO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.bindings.AccessConditionBinding;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.object.ObjectInfo;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * <p>
 * ObjectResource class.
 * </p>
 *
 * @author Florian Alpers
 */

@javax.ws.rs.Path(RECORDS_FILES_3D)
@AccessConditionBinding
@CORSBinding
public class ObjectResource {

    private static final Logger logger = LogManager.getLogger(ObjectResource.class);

    private final String pi;
    private final String filename;

    /**
     * @param context
     * @param request
     * @param response
     * @param urls
     * @param pi
     * @param filename
     */
    public ObjectResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context AbstractApiUrlManager urls,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Filename of the image") @PathParam("filename") String filename) {
        request.setAttribute("pi", pi);
        request.setAttribute("filename", filename);
        this.pi = pi;
        this.filename = filename;
    }

    /**
     * <p>
     * getInfo.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a {@link io.goobi.viewer.model.viewer.object.ObjectInfo} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_FILES_3D_INFO)
    @Produces({ MediaType.APPLICATION_JSON })
    public ObjectInfo getInfo(@Context HttpServletRequest request, @Context HttpServletResponse response)
            throws PresentationException, IndexUnreachableException {

        String objectURI = request.getRequestURL().toString().replaceAll("/(info.json)?$", "");
        //        String baseURI = objectURI.replace(filename, "");
        String baseFilename = FilenameUtils.getBaseName(filename);
        Path mediaDirectory = DataFileTools.getMediaFolder(pi);

        try {
            List<URI> resourceURIs = getResources(mediaDirectory.toString(), baseFilename, objectURI);
            ObjectInfo info = new ObjectInfo(objectURI);
            info.setResources(resourceURIs);

            //calculate sizes
            Path modelFile = mediaDirectory.resolve(filename);
            try {
                long modelSize = Files.size(modelFile);
                info.setSize(info.getUri(), modelSize);
            } catch (IOException e) {
                logger.error("Error determining file size of {}: {}", modelFile, e.toString());
            }

            return info;
        } catch (IOException | URISyntaxException e) {
            throw new PresentationException(e.getMessage(), e);
        }

    }

    /**
     * <p>
     * getObject.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return a {@link javax.ws.rs.core.StreamingOutput} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @GET
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public StreamingOutput getObject(@Context HttpServletRequest request, @Context HttpServletResponse response)
            throws IOException, PresentationException, IndexUnreachableException {

        Path mediaDirectory = DataFileTools.getMediaFolder(pi);
        java.nio.file.Path objectPath = mediaDirectory.resolve(filename);
        if (!objectPath.toFile().isFile()) {
            //try subfolders
            DirectoryStream.Filter<? super java.nio.file.Path> filter = new DirectoryStream.Filter<java.nio.file.Path>() {

                @Override
                public boolean accept(java.nio.file.Path entry) throws IOException {
                    return entry.endsWith(FilenameUtils.getBaseName(filename));
                }

            };

            try (DirectoryStream<java.nio.file.Path> folders = Files.newDirectoryStream(mediaDirectory, filter)) {
                for (java.nio.file.Path folder : folders) {
                    java.nio.file.Path filePath = folder.resolve(filename);
                    if (Files.isRegularFile(filePath)) {
                        return new ObjectStreamingOutput(filePath);
                    }
                }
            }

            throw new FileNotFoundException("File " + objectPath + " not found in file system");
        }

        return new ObjectStreamingOutput(objectPath);
    }

    /**
     * <p>
     * getObjectResource.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param pi a {@link java.lang.String} object.
     * @param subfolder a {@link java.lang.String} object.
     * @param auxfilename a {@link java.lang.String} object.
     * @return a {@link javax.ws.rs.core.StreamingOutput} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_FILES_3D_AUXILIARY_FILE_1)
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public StreamingOutput getObjectResource(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("pi") String pi,
            @PathParam("subfolder") String subfolder, @PathParam("auxfilename") final String auxfilename)
            throws IOException, PresentationException, IndexUnreachableException {

        Path mediaDirectory = DataFileTools.getMediaFolder(pi);
        java.nio.file.Path objectPath = mediaDirectory.resolve(subfolder).resolve(auxfilename);
        if (!objectPath.toFile().isFile()) {
            throw new FileNotFoundException("File " + objectPath + " not found in file system");
        }

        return new ObjectStreamingOutput(objectPath);
    }

    /**
     * <p>
     * getObjectResource2.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param pi a {@link java.lang.String} object.
     * @param subfolder a {@link java.lang.String} object.
     * @param auxfilename a {@link java.lang.String} object.
     * @return a {@link javax.ws.rs.core.StreamingOutput} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_FILES_3D_AUXILIARY_FILE_1_ALT)
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public StreamingOutput getObjectResource2(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("pi") String pi,
            @PathParam("subfolder") String subfolder, @PathParam("auxfilename") final String auxfilename)
            throws IOException, PresentationException, IndexUnreachableException {
        return getObjectResource(request, response, pi, subfolder, auxfilename);
    }

    /**
     * <p>
     * getObjectResource.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param pi a {@link java.lang.String} object.
     * @param subfolder1 a {@link java.lang.String} object.
     * @param subfolder2 a {@link java.lang.String} object.
     * @param auxfilename a {@link java.lang.String} object.
     * @return a {@link javax.ws.rs.core.StreamingOutput} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_FILES_3D_AUXILIARY_FILE_2)
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public StreamingOutput getObjectResource(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("pi") String pi,
            @PathParam("subfolder") String subfolder1, @PathParam("subsubfolder") String subfolder2, @PathParam("auxfilename") String auxfilename)
            throws IOException, PresentationException, IndexUnreachableException {

        Path mediaDirectory = DataFileTools.getMediaFolder(pi);
        java.nio.file.Path objectPath = mediaDirectory.resolve(subfolder1).resolve(subfolder2).resolve(auxfilename);
        if (!objectPath.toFile().isFile()) {
            throw new FileNotFoundException("File " + objectPath + " not found in file system");
        }

        return new ObjectStreamingOutput(objectPath);
    }

    /**
     * <p>
     * getObjectResource2.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @param pi a {@link java.lang.String} object.
     * @param subfolder1 a {@link java.lang.String} object.
     * @param subfolder2 a {@link java.lang.String} object.
     * @param auxfilename a {@link java.lang.String} object.
     * @return a {@link javax.ws.rs.core.StreamingOutput} object.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    @GET
    @javax.ws.rs.Path(RECORDS_FILES_3D_AUXILIARY_FILE_2_ALT)
    @Produces({ MediaType.APPLICATION_OCTET_STREAM })
    public StreamingOutput getObjectResource2(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("pi") String pi,
            @PathParam("subfolder") String subfolder1, @PathParam("subsubfolder") String subfolder2, @PathParam("auxfilename") String auxfilename)
            throws IOException, PresentationException, IndexUnreachableException {
        return getObjectResource(request, response, pi, subfolder1, subfolder2, auxfilename);
    }

    public static class ObjectStreamingOutput implements StreamingOutput {

        private java.nio.file.Path filePath;

        public ObjectStreamingOutput(java.nio.file.Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
            try {
                try (InputStream inputStream = new java.io.FileInputStream(this.filePath.toString())) {
                    IOUtils.copy(inputStream, output);
                }
            } catch (FileNotFoundException | SecurityException e) {
                throw new WebApplicationException(e);
            }

        }
    }

    /**
     * @param baseFolder
     * @param baseFilename
     * @param baseURI
     * @return List<URI>
     * @throws IOException
     * @throws URISyntaxException
     */
    private static List<URI> getResources(String baseFolder, String baseFilename, String baseURI) throws IOException, URISyntaxException {
        List<URI> resourceURIs = new ArrayList<>();

        java.nio.file.Path mtlFilePath = Paths.get(baseFolder, baseFilename + ".mtl");
        if (mtlFilePath.toFile().isFile()) {
            resourceURIs.add(new URI(baseURI + "/" + Paths.get(baseFolder).relativize(mtlFilePath)));
        }

        java.nio.file.Path resourceFolderPath = Paths.get(baseFolder, baseFilename);
        if (resourceFolderPath.toFile().isDirectory()) {
            try (DirectoryStream<java.nio.file.Path> directoryStream = Files.newDirectoryStream(resourceFolderPath)) {
                for (java.nio.file.Path path : directoryStream) {
                    java.nio.file.Path relPath = resourceFolderPath.getParent().relativize(path);
                    resourceURIs.add(new URI(baseURI + "/" + relPath.toString().replace(File.separator, "/")));
                }
            }
        }
        Collections.sort(resourceURIs);
        return resourceURIs;
    }
}
