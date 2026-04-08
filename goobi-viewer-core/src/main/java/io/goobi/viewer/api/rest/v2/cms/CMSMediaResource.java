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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;

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
import de.unigoettingen.sub.commons.cache.CacheUtils;
import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
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
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * REST resource for accessing CMS media files in the v2 API with category and type filtering.
 *
 * @author Florian Alpers
 */
@jakarta.ws.rs.Path(CMS_MEDIA)
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
     * getMediaByTag.
     *
     * @param tags a {@link java.lang.String} object.
     * @param maxItems maximum number of items to return
     * @param prioritySlots number of high-priority items guaranteed in result
     * @param random if true, return items in random order
     * @return a MediaList containing the CMS media items matching the given category tags
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(
            tags = { "media" },
            summary = "Get a list of CMS-Media Items of one or more categories")
    @jakarta.ws.rs.Path(CMS_MEDIA_BY_CATEGORY)
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
     * getMediaByTag.
     *
     * @param tags comma-separated list of category tags to filter by
     * @param maxItems maximum number of items to return
     * @param prioritySlots number of high-priority items guaranteed in result
     * @param random if true, return items in random order
     * @return a MediaList containing all CMS media items optionally filtered by the given tags
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
                    description = "Comma separated list of tags. Only media items with any of these tags will be included") @QueryParam("tags") 
            String tags,
            @Parameter(description = "Maximum number of items to return") @QueryParam("max")
            Integer maxItems,
            @Parameter(
                    description = "Number of media items marks as 'important' that must be included in the result") @QueryParam("prioritySlots") 
            Integer prioritySlots,
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
    @jakarta.ws.rs.Path(CMS_MEDIA_ITEM_BY_ID)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(tags = { "media" }, summary = "Get CMS media item metadata by its database ID")
    @ApiResponse(responseCode = "200", description = "CMS media item metadata")
    @ApiResponse(responseCode = "404", description = "CMS media item not found")
    public MediaItem getMediaItem(@PathParam("id") Long id) throws DAOException {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(id);
        return new MediaItem(item, servletRequest);
    }

    /**
     * getPDFMediaItemContent.
     *
     * @param filename name of the PDF file to serve
     * @param response a {@link jakarta.servlet.http.HttpServletResponse} object.
     * @return File contents as HTML
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE_PDF)
    @Produces("application/pdf")
    @CORSBinding
    @Operation(tags = { "media" }, summary = "Get CMS media file content as PDF")
    @ApiResponse(responseCode = "200", description = "PDF file content")
    @ApiResponse(responseCode = "404", description = "PDF media file not found")
    public static StreamingOutput getPDFMediaItemContent(@PathParam("filename") String filename, @Context HttpServletResponse response)
            throws ContentNotFoundException {
        String decFilename = StringTools.cleanUserGeneratedData(StringTools.decodeUrl(filename));
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
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE_SVG)
    @Produces("image/svg+xml")
    @CORSBinding
    @Operation(tags = { "media" }, summary = "Get CMS media file content as SVG")
    @ApiResponse(responseCode = "200", description = "SVG file content")
    @ApiResponse(responseCode = "404", description = "SVG media file not found")
    public static StreamingOutput getSvgContent(@PathParam("filename") String filename, @Context HttpServletResponse response)
            throws ContentNotFoundException {
        String decFilename = StringTools.cleanUserGeneratedData(StringTools.decodeUrl(filename));
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
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE_ICO)
    @Produces("image/x-icon")
    @CORSBinding
    @Operation(tags = { "media" }, summary = "Get CMS media file content as ICO")
    @ApiResponse(responseCode = "200", description = "ICO file content")
    @ApiResponse(responseCode = "404", description = "ICO media file not found")
    public static StreamingOutput getIcoContent(@PathParam("filename") String filename, @Context HttpServletResponse response)
            throws ContentNotFoundException {
        String decFilename = StringTools.cleanUserGeneratedData(StringTools.decodeUrl(filename));
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
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE_VIDEO)
    @Operation(tags = { "media" }, summary = "Stream CMS media file as video")
    @ApiResponse(responseCode = "200", description = "Video stream")
    @ApiResponse(responseCode = "404", description = "Video media file not found")
    public String serveVideoContent(@PathParam("filename") String filename)
            throws PresentationException, WebApplicationException {
        Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        Path file = cmsMediaFolder.resolve(StringTools.cleanUserGeneratedData(StringTools.decodeUrl(filename)));
        return serveMediaContent("video", file);
    }

    @GET
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE_AUDIO)
    @Operation(tags = { "media" }, summary = "Stream CMS media file as audio")
    @ApiResponse(responseCode = "200", description = "Audio stream")
    @ApiResponse(responseCode = "404", description = "Audio media file not found")
    public String serveAudioContent(@PathParam("filename") String filename)
            throws PresentationException, WebApplicationException {
        Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        Path file = cmsMediaFolder.resolve(StringTools.cleanUserGeneratedData(StringTools.decodeUrl(filename)));
        return serveMediaContent("audio", file);
    }

    /**
     * getMediaItemContent.
     *
     * @param filename name of the HTML file to serve
     * @return File contents as HTML
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE_HTML)
    @Produces({ MediaType.TEXT_HTML })
    @Operation(tags = { "media" }, summary = "Get CMS media file content as HTML")
    @ApiResponse(responseCode = "200", description = "HTML file content")
    @ApiResponse(responseCode = "404", description = "HTML media file not found")
    public static String getMediaItemContent(@PathParam("filename") String filename) throws ContentNotFoundException {

        String decFilename = StringTools.cleanUserGeneratedData(StringTools.decodeUrl(filename));
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
     * Return the media item for the given filename. If no matching media item exists, return a not-found status code
     *
     * @param filename URL-encoded filename to look up in the database
     * @return an HTTP response containing the CMS media item metadata as JSON, or a 404 response if no item was found for the given filename
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @jakarta.ws.rs.Path(CMS_MEDIA_ITEM_BY_FILE)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = { "media" }, summary = "Get CMS media item metadata by filename")
    @ApiResponse(responseCode = "200", description = "CMS media item metadata")
    @ApiResponse(responseCode = "404", description = "No CMS media item found for the given filename")
    public Response validateUploadMediaFiles(@PathParam("filename") String filename) throws DAOException {

        CMSMediaItem item =
                DataManager.getInstance().getDao().getCMSMediaItemByFilename(StringTools.cleanUserGeneratedData(StringTools.decodeUrl(filename)));
        if (item != null) {
            MediaItem jsonItem = new MediaItem(item, servletRequest);
            return Response.status(Status.OK).entity(jsonItem).build();
        }
        return Response.status(Status.NOT_FOUND).entity("{}").build();
    }

    /**
     * List all uploaded media files.
     * 
     * @return All CMS media files
     * @throws PresentationException
     */
    @GET
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES)
    @Produces(MediaType.APPLICATION_JSON)
    @UserLoggedInBinding
    @Operation(tags = { "media" }, summary = "List all uploaded CMS media files (requires login)")
    @ApiResponse(responseCode = "200", description = "List of filenames in the CMS media folder")
    @ApiResponse(responseCode = "401", description = "Not logged in")
    public List<String> getAllFiles() throws PresentationException {
        Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                DataManager.getInstance().getConfiguration().getCmsMediaFolder());
        try (Stream<Path> files = Files.list(cmsMediaFolder)) {
            return files.filter(Files::isRegularFile).map(Path::getFileName).map(Path::toString).collect(Collectors.toList());
        } catch (IOException e) {
            throw new PresentationException("Failed to read uploaded files: " + e.toString());
        }
    }

    // Deleting CMS media files via REST is intentionally not supported — always returns 400
    @Hidden
    @DELETE
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES)
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationBinding
    @Operation(hidden = true)
    public void deleteAllFiles() throws IllegalRequestException {
        throw new IllegalRequestException("Deleting cms media files is not supported via REST");
    }

    // Deleting a single CMS media file via REST is intentionally not supported — always returns 400
    @Hidden
    @DELETE
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE)
    @Produces(MediaType.APPLICATION_JSON)
    @AuthorizationBinding
    @Operation(hidden = true)
    public void deleteFile() throws IllegalRequestException {
        throw new IllegalRequestException("Deleting cms media files is not supported via REST");
    }

    /**
     * Fallback GET handler for filenames that do NOT match any of the specific extension patterns
     * handled by {@link CMSMediaImageResource3} and the other typed handlers.
     *
     * <p>Returns 400 so that schemathesis receives the correct error code instead of 405 Method Not
     * Allowed (which JAX-RS would return if no GET handler existed for this path).
     *
     * @param filename the requested filename
     * @return never returns normally; always throws {@link IllegalRequestException}
     */
    @io.swagger.v3.oas.annotations.Hidden
    @GET
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES_FILE)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(hidden = true)
    public Response getInvalidFilename(
            @PathParam("filename") String filename) throws IllegalRequestException {
        throw new IllegalRequestException("Invalid filename: must have a supported file extension"
                + " (jpg, png, tif, gif, jp2, pdf, svg, ico, mp4, webm, mp3, ogg, html)");
    }

    /**
     * May receive a file from a multipart form and saves the file in the cms media folder.
     *
     * @param enabled whether the uploaded media item should be enabled; defaults to true
     * @param filename target filename for the uploaded file in the CMS media folder
     * @param uploadedInputStream byte stream of the uploaded file content
     * @param fileDetail multipart content-disposition metadata for the uploaded file
     * @return an ACCEPTED response if the upload was successful, a FORBIDDEN response if no user is registered in the html session or the user does
     *         not have rights to upload media, or a CONFLICT response if a file of the same name already exists in the cms media foler
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @POST
    @jakarta.ws.rs.Path(CMS_MEDIA_FILES)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @UserLoggedInBinding
    @Operation(tags = { "media" }, summary = "Upload a new CMS media file (requires login)")
    @ApiResponse(responseCode = "200", description = "File successfully uploaded; returns the CMS media item metadata")
    @ApiResponse(responseCode = "401", description = "Not logged in")
    @ApiResponse(responseCode = "403", description = "User does not have rights to upload media")
    @ApiResponse(responseCode = "406", description = "Upload stream is null")
    @ApiResponse(responseCode = "500", description = "Upload failed due to an internal error")
    public Response uploadMediaFiles(@DefaultValue("true") @FormDataParam("enabled") boolean enabled, @FormDataParam("filename") String filename,
            @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail)
            throws DAOException {

        try {
            if (uploadedInputStream == null) {
                throw new RestApiException("Upload stream is null", Status.NOT_ACCEPTABLE);
            }

            Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),
                    DataManager.getInstance().getConfiguration().getCmsMediaFolder());
            Path mediaFile = cmsMediaFolder.resolve(StringTools.cleanUserGeneratedData(StringTools.decodeUrl(filename)));
            return writeMediaFile(uploadedInputStream, cmsMediaFolder, mediaFile);
        } catch (RestApiException e) {
            return Response.status(e.getStatus()).entity(e.getMessage()).build();
        }
    }

    /**
     *
     * @param uploadedInputStream the input stream of the uploaded file data
     * @param cmsMediaFolder path to the CMS media storage directory
     * @param mediaFile resolved target path for the uploaded file
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
     * @param item the CMS media item to evict from cache
     */
    public static void removeFromImageCache(CMSMediaItem item) {
        String identifier =
                DataManager.getInstance().getConfiguration().getCmsMediaFolder() + "_" + item.getFileName().replace(".", "-").replaceAll("\\s", "");
        CacheUtils cacheUtils = new CacheUtils(ContentServerCacheManager.getInstance());
        cacheUtils.deleteFromCache(identifier, true, true);
    }

    /**
     * Return an Optional containing a {@link CMSCategory} for which the user has access rights if the user in a CmsAdmin but has limited category
     * rights If the user has unlimited category rights, return an empty optional.
     *
     * @param user the logged-in user to check category rights for
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
     * createMediaItem.
     *
     * @param filePath path to the newly uploaded file on disk
     * @return a new CMSMediaItem initialized with the filename and empty metadata for all configured locales
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

        
        public List<MediaItem> getMediaItems() {
            return mediaItems;
        }
    }

    /**
     *
     * @param type media type prefix, e.g. "video" or "audio"
     * @param file path to the media file to stream
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
