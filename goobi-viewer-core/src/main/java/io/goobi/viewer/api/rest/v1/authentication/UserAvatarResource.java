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
package io.goobi.viewer.api.rest.v1.authentication;

import static io.goobi.viewer.api.rest.v1.ApiUrls.USERS_USER_AVATAR_IMAGE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ImageResource;
import de.unigoettingen.sub.commons.util.CacheUtils;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.security.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @author florian
 *
 */
@javax.ws.rs.Path(USERS_USER_AVATAR_IMAGE)
@CORSBinding
public class UserAvatarResource extends ImageResource {

    private static final Logger logger = LogManager.getLogger(UserAvatarResource.class);
    @Context
    protected HttpServletRequest servletRequest;
    @Context
    protected HttpServletResponse servletResponse;

    private static final String FILENAME_TEMPLATE = "user_{id}";

    public UserAvatarResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Parameter(description = "User id") @PathParam("userId") Long userId) throws WebApplicationException, ViewerConfigurationException {
        super(context, request, response, "", getMediaFileUrl(userId).toString());
        AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(null);
        if (urls == null) {
            throw new ViewerConfigurationException("Could not initioalize API manager, check configuration.");
        }
        request.setAttribute("filename", this.imageURI.toString());
        String requestUrl = request.getRequestURI();
        String baseImageUrl = urls.path(ApiUrls.USERS_USER_AVATAR_IMAGE).params(userId).build();
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
     * @param userId
     * @return {@link URI}
     * @throws WebApplicationException
     */
    public static URI getMediaFileUrl(Long userId) throws WebApplicationException {
        try {
            return getUserAvatarFile(userId).map(PathConverter::toURI)
                    .orElseThrow(() -> new ContentNotFoundException("No avatar file found for user " + userId));
        } catch (ContentLibException | IOException e) {
            throw new WebApplicationException(e);
        }
    }

    public static Path getUserAvatarFolder() {
        return Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getUserAvatarFolder());
    }

    /**
     * @param userId
     * @return Optional<Path>
     * @throws IOException
     */
    public static Optional<Path> getUserAvatarFile(Long userId) throws IOException {
        Path folder = getUserAvatarFolder();
        if (!Files.isDirectory(folder)) {
            Files.createDirectories(folder);
        }
        String baseFilename = FILENAME_TEMPLATE.replace("{id}", userId.toString());
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> FilenameUtils.getBaseName(file.getFileName().toString()).equals(baseFilename)).findAny();
        }
    }

    public static String getAvatarFileSuffix(Long userId) throws IOException {
        return getUserAvatarFile(userId).map(Path::getFileName).map(Path::toString).map(FilenameUtils::getExtension).orElse("jpg");
    }

    @Override
    public void createResourceURI(HttpServletRequest request, String directory, String filename) throws IllegalRequestException {
        //don't do anyhting. The resource url has already been set in constructor
    }

    @Override
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MEDIA_TYPE_APPLICATION_JSONLD })
    @ContentServerImageInfoBinding
    @Operation(tags = { "users" }, summary = "IIIF image identifier for an uploaded user avatar image. Returns a IIIF 2.1.1 image information object")
    @ApiResponse(responseCode = "404", description = "No image for the given user was uploaded")
    public Response redirectToCanonicalImageInfo() throws ContentLibException {
        return super.redirectToCanonicalImageInfo();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadAvatarFile(@DefaultValue("true") @FormDataParam("enabled") boolean enabled,
            @FormDataParam("filename") String uploadFilename,
            @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail) {

        if (uploadedInputStream == null) {
            return Response.status(Status.NOT_ACCEPTABLE).entity("Upload stream is null").build();
        }
        Optional<User> user = getUser();
        if (!user.isPresent()) {
            return Response.status(Status.NOT_ACCEPTABLE).entity("No user session found").build();
        }

        Path mediaFolder = getUserAvatarFolder();
        Path mediaFile = getAvatarFilePath(uploadFilename, user.get().getId());
        try {

            if (!Files.exists(mediaFolder)) {
                Files.createDirectories(mediaFolder);
            }

            Files.copy(uploadedInputStream, mediaFile, StandardCopyOption.REPLACE_EXISTING);

            if (Files.exists(mediaFile) && Files.size(mediaFile) > 0) {
                logger.debug("Successfully downloaded file {}", mediaFile);
                //upload successful. TODO: check file integrity?
                removeFromImageCache(mediaFile);
                return Response.status(Status.OK).build();
            }
            String message = Messages.translate("admin__media_upload_error", servletRequest.getLocale(), mediaFile.getFileName().toString());
            if (Files.exists(mediaFile)) {
                Files.delete(mediaFile);
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();
        } catch (FileAlreadyExistsException e) {
            String message =
                    Messages.translate("admin__media_upload_error_exists", servletRequest.getLocale(), mediaFile.getFileName().toString());
            return Response.status(Status.CONFLICT).entity(message).build();
        } catch (IOException e) {
            logger.error("Error uploading media file", e);
            String message = Messages.translate("admin__media_upload_error", servletRequest.getLocale(), mediaFile.getFileName().toString(),
                    e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();
        }
    }

    public static Path getAvatarFilePath(String uploadFilename, Long userId) {
        ImageFileFormat fileFormat = ImageFileFormat.getImageFileFormatFromFileExtension(uploadFilename);
        if (fileFormat != null) {
            String filename = FILENAME_TEMPLATE.replace("{id}", userId.toString()) + "." + fileFormat.getFileExtension();
            return getUserAvatarFolder().resolve(filename);            
        } else {
            String filename = FILENAME_TEMPLATE.replace("{id}", userId.toString()) + "." + FilenameUtils.getExtension(uploadFilename);
            return getUserAvatarFolder().resolve(filename);      
        }

    }

    /**
     * Determines the current User using the UserBean instance stored in the session store. If no session is available, no UserBean could be found or
     * no user is logged in, NULL is returned
     *
     * @return Optional<User>
     */
    private static Optional<User> getUser() {
        UserBean userBean = BeanUtils.getUserBean();
        if (userBean == null) {
            logger.trace("Unable to get user: No UserBean found in session store.");
            return Optional.empty();
        }
        User user = userBean.getUser();
        if (user == null) {
            logger.trace("Unable to get user: No user found in session store UserBean instance");
            return Optional.empty();
        }
        // logger.trace("Found user {}", user);
        return Optional.of(user);
    }

    public static void removeFromImageCache(Path file) {
        String identifier = file.getParent().getFileName().toString() + "_" + file.getFileName().toString().replace(".", "-");
        CacheUtils.deleteFromCache(identifier, true, true);
    }

}
