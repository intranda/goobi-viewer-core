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
package io.goobi.viewer.api.rest.v2.cms;

import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_BY_CATEGORY;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE_AUDIO;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE_HTML;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE_ICO;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE_PDF;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE_SVG;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_FILES_FILE_VIDEO;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_ITEM_BY_FILE;
import static io.goobi.viewer.api.rest.v2.ApiUrls.CMS_MEDIA_ITEM_BY_ID;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.util.CacheUtils;
import io.goobi.viewer.api.rest.bindings.AuthorizationBinding;
import io.goobi.viewer.api.rest.bindings.UserLoggedInBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.MediaDeliveryService;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RestApiException;
import io.goobi.viewer.managedbeans.CmsBean;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.media.CMSMediaItemMetadata;
import io.goobi.viewer.model.cms.media.CMSMediaLister;
import io.goobi.viewer.model.cms.media.MediaItem;
import io.goobi.viewer.model.security.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * <p>
 * CMSMediaResource class.
 * </p>
 *
 * @author Florian Alpers
 */
@javax.ws.rs.Path(CMS_MEDIA)
@ViewerRestServiceBinding
public class CMSMediaResource {

    private static final Logger logger = LogManager.getLogger(CMSMediaResource.class);

    private static final String FILE_NOT_FOUND_MESSAGE = "File {} not found in file system";

    @Context
    protected HttpServletRequest servletRequest;
    @Context
    protected HttpServletResponse servletResponse;
    @Context
    private IDAO dao;

    /**
     * <p>
     * getMediaByTag.
     * </p>
     *
     * @param tags a {@link java.lang.String} object.
     * @param maxItems
     * @param prioritySlots
     * @param random
     * @return a {@link io.goobi.viewer.servlets.rest.cms.CMSMediaResource.MediaList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "media" },
            summary = "Get a list of CMS-Media Items of one or more categories")
    @javax.ws.rs.Path(CMS_MEDIA_BY_CATEGORY)
    public MediaList getMediaOfCategories(
            @Parameter(description = "tag specifying the category the delivered media items must be associated with."
                    + " Multiple categories can be listed using '...' as separator") @PathParam("tags") String tags,
            @Parameter(description = "Maximum number of items to return") @QueryParam("max") Integer maxItems,
            @Parameter(description = "Number of media items marks as 'important' that must be included"
                    + " in the result") @QueryParam("prioritySlots") Integer prioritySlots,
            @Parameter(description = "Set to 'true' to return random items for each call."
                    + " Otherwise the items will be ordererd by their upload date") @QueryParam("random") Boolean random)
            throws DAOException {
        List<String> tagList = new ArrayList<>();
        if (StringUtils.isNotBlank(tags)) {
            tagList.addAll(Arrays.stream(StringUtils.split(tags, "...")).map(String::toLowerCase).collect(Collectors.toList()));
            List<CMSMediaItem> items = new CMSMediaLister(dao).getMediaItems(tagList, maxItems, prioritySlots, Boolean.TRUE.equals(random));
            return new MediaList(items);
        }
        return new MediaList(Collections.emptyList());
    }

    /**
     * <p>
     * getMediaByTag.
     * </p>
     *
     * @param tags
     * @param maxItems
     * @param prioritySlots
     * @param random
     * @return a {@link io.goobi.viewer.servlets.rest.cms.CMSMediaResource.MediaList} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "media" },
            summary = "Get a list of CMS-Media Items")
    @AuthorizationBinding
    public MediaList getAllMedia(
            @Parameter(
                    description = "Comma separated list of tags. Only media items with any of these tags will be included")
            @QueryParam("tags") String tags,
            @Parameter(description = "Maximum number of items to return") @QueryParam("max") Integer maxItems,
            @Parameter(
                    description = "Number of media items marks as 'important' that must be included in the result")
            @QueryParam("prioritySlots") Integer prioritySlots,
            @Parameter(
                    description = "Set to 'true' to return random items for each call. Otherwise the items will be ordererd by their upload date")
            @QueryParam("random") Boolean random)
            throws DAOException {
        List<String> tagList = new ArrayList<>();
        if (StringUtils.isNotBlank(tags)) {
            tagList.addAll(Arrays.stream(StringUtils.split(tags, ",")).map(String::toLowerCase).collect(Collectors.toList()));
        }
        List<CMSMediaItem> items = new CMSMediaLister(dao).getMediaItems(tagList, maxItems, prioritySlots, Boolean.TRUE.equals(random));
        return new MediaList(items);
    }

    @GET
    @javax.ws.rs.Path(CMS_MEDIA_ITEM_BY_ID)
    @Produces({ MediaType.APPLICATION_JSON })
    public MediaItem getMediaItem(@PathParam("id") Long id) throws DAOException {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(id);
        return new MediaItem(item, servletRequest);
    }

    /**
     * <p>
     * getPDFMediaItemContent.
     * </p>
     *
     * @param filename
     * @param response a {@link javax.servlet.http.HttpServletResponse} object.
     * @return File contents as HTML
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @javax.ws.rs.Path(CMS_MEDIA_FILES_FILE_PDF)
    @Produces("application/pdf")
    @CORSBinding
    public static StreamingOutput getPDFMediaItemContent(@PathParam("filename") String filename, @Context HttpServletResponse response)
            throws ContentNotFoundException {
        String decFilename = StringTools.decodeUrl(filename);
        Path path = Paths.get(
                DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder(),
                decFilename);
        if (Files.exists(path)) {
            return new StreamingOutput() {

                @Override
                public void write(OutputStream out) throws IOException, WebApplicationException {
                    try (InputStream in = Files.newInputStream(path)) {
                        IOUtils.copy(in, out);
                    }
                }
            };
        }
        throw new ContentNotFoundException(FILE_NOT_FOUND_MESSAGE.replace("{}", path.toString()));
    }

    @GET
    @javax.ws.rs.Path(CMS_MEDIA_FILES_FILE_SVG)
    @Produces("image/svg+xml")
    @CORSBinding
    public static StreamingOutput getSvgContent(@PathParam("filename") String filename, @Context HttpServletResponse response)
            throws ContentNotFoundException {
        String decFilename = StringTools.decodeUrl(filename);
        Path path = Paths.get(
                DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder(),
                decFilename);
        if (Files.exists(path)) {
            return new StreamingOutput() {

                @Override
                public void write(OutputStream out) throws IOException, WebApplicationException {
                    try (InputStream in = Files.newInputStream(path)) {
                        IOUtils.copy(in, out);
                    }
                }
            };
        }
        throw new ContentNotFoundException(FILE_NOT_FOUND_MESSAGE.replace("{}", path.toString()));
    }

    @GET
    @javax.ws.rs.Path(CMS_MEDIA_FILES_FILE_ICO)
    @Produces("image/x-icon")
    @CORSBinding
    public static StreamingOutput getIcoContent(@PathParam("filename") String filename, @Context HttpServletResponse response)
            throws ContentNotFoundException {
        String decFilename = StringTools.decodeUrl(filename);
        Path path = Paths.get(
                DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder(),
                decFilename);
        if (Files.exists(path)) {
            return new StreamingOutput() {

                @Override
                public void write(OutputStream out) throws IOException, WebApplicationException {
                    try (InputStream in = Files.newInputStream(path)) {
                        IOUtils.copy(in, out);
                    }
                }
            };
        }
        throw new ContentNotFoundException(FILE_NOT_FOUND_MESSAGE.replace("{}", path.toString()));
    }

    @GET
    @javax.ws.rs.Path(CMS_MEDIA_FILES_FILE_VIDEO)
    public String serveVideoContent(@PathParam("filename") String filename)
            throws PresentationException, WebApplicationException {
        Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        Path file = cmsMediaFolder.resolve(StringTools.decodeUrl(filename));
        return serveMediaContent("video", file);
    }

    @GET
    @javax.ws.rs.Path(CMS_MEDIA_FILES_FILE_AUDIO)
    public String serveAudioContent(@PathParam("filename") String filename)
            throws PresentationException, WebApplicationException {
        Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        Path file = cmsMediaFolder.resolve(StringTools.decodeUrl(filename));
        return serveMediaContent("audio", file);
    }

    /**
     * <p>
     * getMediaItemContent.
     * </p>
     *
     * @param filename
     * @return File contents as HTML
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @javax.ws.rs.Path(CMS_MEDIA_FILES_FILE_HTML)
    @Produces({ MediaType.TEXT_HTML })
    public static String getMediaItemContent(@PathParam("filename") String filename) throws ContentNotFoundException {

        String decFilename = StringTools.decodeUrl(filename);
        decFilename = Paths.get(decFilename).getFileName().toString(); // Make sure filename doesn't inject a path traversal
        Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        Path path = Paths.get(cmsMediaFolder.toAbsolutePath().toString(), decFilename);

        try {
            if (Files.isRegularFile(path)) {
                String encoding = "windows-1252";
                String ret = FileTools.getStringFromFile(path.toFile(), encoding, StringTools.DEFAULT_ENCODING);
                return StringTools.renameIncompatibleCSSClasses(ret);

            }
        } catch (FileNotFoundException e) {
            logger.debug(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        throw new ContentNotFoundException(StringConstants.EXCEPTION_RESOURCE_NOT_FOUND);
    }

    /**
     * <p>
     * Return the media item for the given filename. If no matching media item exists, return a not-found status code
     * </p>
     *
     * @param filename a {@link java.lang.String} object.
     * @return a {@link javax.ws.rs.core.Response} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @javax.ws.rs.Path(CMS_MEDIA_ITEM_BY_FILE)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateUploadMediaFiles(@PathParam("filename") String filename) throws DAOException {

        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItemByFilename(filename);
        if (item != null) {
            MediaItem jsonItem = new MediaItem(item, servletRequest);
            return Response.status(Status.OK).entity(jsonItem).build();
        }
        return Response.status(Status.NOT_FOUND).entity("{}").build();
    }

    /**
     * List all uploaded media files
     * 
     * @return All CMS media files
     * @throws PresentationException
     *
     */
    @GET
    @javax.ws.rs.Path(CMS_MEDIA_FILES)
    @Produces(MediaType.APPLICATION_JSON)
    @UserLoggedInBinding
    public List<String> getAllFiles() throws PresentationException {
        Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        try (Stream<Path> files = Files.list(cmsMediaFolder)) {
            return files.filter(Files::isRegularFile).map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
        } catch (IOException e) {
            throw new PresentationException("Failed to read uploaded files: " + e.toString());
        }
    }

    @DELETE
    @javax.ws.rs.Path(CMS_MEDIA_FILES)
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationBinding
    public void deleteAllFiles() throws IllegalRequestException {
        throw new IllegalRequestException("Deleting cms media files is not supported via REST");
    }

    @DELETE
    @javax.ws.rs.Path(CMS_MEDIA_FILES_FILE)
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationBinding
    public void deleteFile() throws IllegalRequestException {
        throw new IllegalRequestException("Deleting cms media files is not supported via REST");
    }

    /**
     * May receive a file from a multipart form and saves the file in the cms media folder
     *
     * @return an ACCEPTED response if the upload was successful, a FORBIDDEN response if no user is registered in the html session or the user does
     *         not have rights to upload media, or a CONFLICT response if a file of the same name already exists in the cms media foler
     * @param enabled a boolean.
     * @param filename a {@link java.lang.String} object.
     * @param uploadedInputStream a {@link java.io.InputStream} object.
     * @param fileDetail a {@link org.glassfish.jersey.media.multipart.FormDataContentDisposition} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @POST
    @javax.ws.rs.Path(CMS_MEDIA_FILES)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @UserLoggedInBinding
    public Response uploadMediaFiles(@DefaultValue("true") @FormDataParam("enabled") boolean enabled, @FormDataParam("filename") String filename,
            @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail)
            throws DAOException {

        try {
            if (uploadedInputStream == null) {
                throw new RestApiException("Upload stream is null", Status.NOT_ACCEPTABLE);
            }

            Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                    DataManager.getInstance().getConfiguration().getCmsMediaFolder());
            Path mediaFile = cmsMediaFolder.resolve(filename);
            return writeMediaFile(uploadedInputStream, cmsMediaFolder, mediaFile);
        } catch (RestApiException e) {
            return Response.status(e.getStatus()).entity(e.getMessage()).build();
        }
    }

    /**
     * 
     * @param uploadedInputStream
     * @param cmsMediaFolder
     * @param mediaFile
     * @return {@link Response}
     * @throws RestApiException
     */
    private Response writeMediaFile(InputStream uploadedInputStream, Path cmsMediaFolder, Path mediaFile) throws RestApiException {
        try {
            Optional<CMSCategory> requiredCategory = getRequiredCategoryForUser(getUser().orElse(null));
            if (!Files.exists(cmsMediaFolder)) {
                Files.createDirectory(cmsMediaFolder);
            }

            CMSMediaItem item = null;
            if (Files.exists(mediaFile)) {
                //re-uploading existing file. Replace file in existing MediaItem
                item = DataManager.getInstance().getDao().getCMSMediaItemByFilename(mediaFile.getFileName().toString());
                if (item != null) {
                    logger.error("Found existing media file without mediaItem entry in database. Deleting file");
                }
            }
            Files.copy(uploadedInputStream, mediaFile, StandardCopyOption.REPLACE_EXISTING);

            if (Files.exists(mediaFile) && Files.size(mediaFile) > 0) {
                logger.debug("Successfully downloaded file {}", mediaFile);
                if (item == null) {
                    item = createMediaItem(mediaFile);
                    requiredCategory.ifPresent(item::addCategory);
                    DataManager.getInstance().getDao().addCMSMediaItem(item);
                } else {
                    item.setFileName(mediaFile.getFileName().toString());
                    DataManager.getInstance().getDao().updateCMSMediaItem(item);
                    removeFromImageCache(item);
                }
                MediaItem jsonItem = new MediaItem(item, servletRequest);
                return Response.status(Status.OK).entity(jsonItem).build();
            }
            String message = Messages.translate("admin__media_upload_error", servletRequest.getLocale(), mediaFile.getFileName().toString());
            if (Files.exists(mediaFile)) {
                Files.delete(mediaFile);
            }
            throw new RestApiException(message, Status.INTERNAL_SERVER_ERROR);
        } catch (AccessDeniedException e) {
            throw new RestApiException(e.getMessage(), Status.FORBIDDEN);
        } catch (FileAlreadyExistsException e) {
            String message =
                    Messages.translate("admin__media_upload_error_exists", servletRequest.getLocale(), mediaFile.getFileName().toString());
            throw new RestApiException(message, Status.CONFLICT);
        } catch (IOException | DAOException e) {
            logger.error("Error uploading media file", e);
            String message = Messages.translate("admin__media_upload_error", servletRequest.getLocale(), mediaFile.getFileName().toString(),
                    e.getMessage());
            throw new RestApiException(message, Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * @param item
     */
    public static void removeFromImageCache(CMSMediaItem item) {
        String identifier =
                DataManager.getInstance().getConfiguration().getCmsMediaFolder() + "_" + item.getFileName().replace(".", "-").replaceAll("\\s", "");
        CacheUtils.deleteFromCache(identifier, true, true);
    }

    /**
     * Return an Optional containing a {@link CMSCategory} for which the user has access rights if the user in a CmsAdmin but has limited category
     * rights If the user has unlimited category rights, return an empty optional
     *
     * @param user
     * @return Optional<CMSCategory>
     * @throws DAOException
     * @throws AccessDeniedException if the user is not allowed to use any categories whatsoever
     */
    private static Optional<CMSCategory> getRequiredCategoryForUser(User user) throws DAOException, AccessDeniedException {

        if (user != null && !user.hasPrivilegeForAllCategories()) {
            List<CMSCategory> allowedCategories = user.getAllowedCategories(DataManager.getInstance().getDao().getAllCategories());
            if (!allowedCategories.isEmpty()) {
                return Optional.of(allowedCategories.get(0));
            }
            throw new AccessDeniedException("The user " + user + " has no rights to any categories and may therefore not upload any media files");
        }
        return Optional.empty();
    }

    /**
     * <p>
     * createMediaItem.
     * </p>
     *
     * @param filePath a {@link java.nio.file.Path} object.
     * @return a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     */
    public CMSMediaItem createMediaItem(Path filePath) {
        CMSMediaItem item = new CMSMediaItem();
        item.setFileName(filePath.getFileName().toString());
        for (Locale locale : CmsBean.getAllLocales()) {
            CMSMediaItemMetadata metadata = new CMSMediaItemMetadata();
            metadata.setLanguage(locale.getLanguage());
            item.addMetadata(metadata);
        }
        return item;
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
        return Optional.of(user);
    }

    public class MediaList {

        private final List<MediaItem> mediaItems;

        public MediaList(List<CMSMediaItem> items) {
            this.mediaItems = items.stream().map(item -> new MediaItem(item, servletRequest)).collect(Collectors.toList());
        }

        /**
         * @return the mediaItems
         */
        public List<MediaItem> getMediaItems() {
            return mediaItems;
        }
    }

    /**
     * 
     * @param type
     * @param file
     * @return {@link String}
     * @throws PresentationException
     * @throws WebApplicationException
     */
    private String serveMediaContent(String type, Path file) throws PresentationException, WebApplicationException {
        String mimeType = type + "/" + FilenameUtils.getExtension(file.getFileName().toString());

        if (Files.isRegularFile(file)) {
            logger.debug("Video file: {} ({} bytes)", file.toAbsolutePath(), file.toFile().length());
            try {
                new MediaDeliveryService().processRequest(servletRequest, servletResponse, file.toAbsolutePath().toString(), mimeType);
            } catch (IOException e) {
                throw new PresentationException("Error accessing media resource", e);
            }
        } else {
            logger.error("File '{}' not found.", file.toAbsolutePath());
            try {
                servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        }
        return "";
    }

}
