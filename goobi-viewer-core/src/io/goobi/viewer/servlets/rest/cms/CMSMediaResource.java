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
package io.goobi.viewer.servlets.rest.cms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.serializer.MetadataSerializer;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.ConversionTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.CmsBean;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSMediaItemMetadata;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

/**
 * @author Florian Alpers
 *
 */
@javax.ws.rs.Path("/cms/media")
@ViewerRestServiceBinding
public class CMSMediaResource {

    private static final Logger logger = LoggerFactory.getLogger(CMSContentResource.class);
    @Context
    protected HttpServletRequest servletRequest;
    @Context
    protected HttpServletResponse servletResponse;

    @GET
    @javax.ws.rs.Path("/get/{tag}")
    @Produces({ MediaType.APPLICATION_JSON })
    public MediaList getMediaByTag(@PathParam("tag") String tag) throws DAOException {

        List<CMSMediaItem> items = DataManager.getInstance()
                .getDao()
                .getAllCMSMediaItems()
                .stream()
                .filter(item -> item.getCategories().stream().anyMatch(c -> c.getName().equalsIgnoreCase(tag)))
                .collect(Collectors.toList());
        return new MediaList(items);
    }

    @GET
    @javax.ws.rs.Path("/get")
    @Produces({ MediaType.APPLICATION_JSON })
    public MediaList getAllMedia() throws DAOException {

        List<CMSMediaItem> items = DataManager.getInstance().getDao().getAllCMSMediaItems();
        return new MediaList(items);
    }
    
    /**
     * 
     * @param id
     * @return File contents as HTML
     * @throws ContentNotFoundException
     * @throws DAOException
     */
    @GET
    @javax.ws.rs.Path("/get/{id}.pdf")
    @Produces("application/pdf")
    @CORSBinding
    public static StreamingOutput getPDFMediaItemContent(@PathParam("id") Long id, @Context HttpServletResponse response) throws ContentNotFoundException, DAOException {
        
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(id);
        if(item != null && item.getContentType().equals(CMSMediaItem.CONTENT_TYPE_PDF)) {
            Path path = item.getFilePath();
            if(Files.exists(path)) {
                return new StreamingOutput() {
                    
                    @Override
                    public void write(OutputStream out) throws IOException, WebApplicationException {
                        try(InputStream in = Files.newInputStream(path)) {
                            IOUtils.copy(in, out);
                        }
                    }
                };
            } else {
                throw new ContentNotFoundException("File " + path + " not found in file system");
            }
        } else {
            throw new ContentNotFoundException("No pdf item with id " + id + " found");
        }
        
    }

    /**
     * 
     * @param id
     * @return File contents as HTML
     * @throws ContentNotFoundException
     * @throws DAOException
     */
    @GET
    @javax.ws.rs.Path("/get/item/{id}")
    @Produces({ MediaType.TEXT_HTML })
    public static String getMediaItemContent(@PathParam("id") Long id) throws ContentNotFoundException, DAOException {
        CMSMediaItem item = DataManager.getInstance().getDao().getCMSMediaItem(id);
        if (item == null) {
            throw new ContentNotFoundException("Resource not found");
        }
        String extension = FilenameUtils.getExtension(item.getFileName()).toLowerCase();
        StringBuilder sbUri = new StringBuilder();
        sbUri.append(DataManager.getInstance().getConfiguration().getViewerHome())
                .append(DataManager.getInstance().getConfiguration().getCmsMediaFolder())
                .append('/')
                .append(item.getFileName());
        java.nio.file.Path filePath = Paths.get(sbUri.toString());
        if (Files.isRegularFile(filePath)) {
            switch (item.getContentType()) {
                case CMSMediaItem.CONTENT_TYPE_HTML:
                case CMSMediaItem.CONTENT_TYPE_XML:
                    try {
                        String encoding = "windows-1252";
                        String ret = FileTools.getStringFromFile(filePath.toFile(), encoding, Helper.DEFAULT_ENCODING);
                        return StringTools.renameIncompatibleCSSClasses(ret);
                    } catch (FileNotFoundException e) {
                        logger.debug(e.getMessage());
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                    break;
                case CMSMediaItem.CONTENT_TYPE_DOCX:
                    try {
                        String ret = ConversionTools.convertDocxToHtml(filePath);
                        return StringTools.renameIncompatibleCSSClasses(ret);
                    } catch (FileNotFoundException e) {
                        logger.debug(e.getMessage());
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                case CMSMediaItem.CONTENT_TYPE_RTF:
                    try {
                        String ret = ConversionTools.convertFileToHtml(filePath);
                        return StringTools.renameIncompatibleCSSClasses(ret);
                    } catch (FileNotFoundException e) {
                        logger.debug(e.getMessage());
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                    break;
            }
        }
        throw new ContentNotFoundException("Resource not found");
    }
    
    /**
     * May receive a file from a multipart form and saves the file in the cms media folder
     * @return an ACCEPTED response if the upload was successful, a FORBIDDEN response if no user is registered in the html session or the user does not have rights to upload media,
     *  or a CONFLICT response if a file of the same name already exists in the cms media foler
     */
    @POST
    @javax.ws.rs.Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadMediaFiles(
            @DefaultValue("true") @FormDataParam("enabled") boolean enabled,
            @FormDataParam("filename") String filename,
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) throws DAOException {
        
        if (uploadedInputStream == null) {
            return Response.status(Status.NOT_ACCEPTABLE).entity("Upload stream is null").build();
        }
        
        
        Optional<User> user = getUser();
        if(!user.isPresent()) {
            return Response.status(Status.NOT_ACCEPTABLE).entity("No user session found").build();
        } else if(!user.get().isCmsAdmin()) {
            return Response.status(Status.FORBIDDEN).entity("User has no permission to upload media files").build();
        } else {

            Path cmsMediaFolder = Paths.get(DataManager.getInstance().getConfiguration().getViewerHome(),DataManager.getInstance().getConfiguration().getCmsMediaFolder());
            Path mediaFile = cmsMediaFolder.resolve(filename);
            try {           
                Optional<CMSCategory> requiredCategory = getRequiredCategoryForUser(user.get());

                
                if(!Files.exists(cmsMediaFolder)) {
                    Files.createDirectory(cmsMediaFolder);
                }
                Files.copy(uploadedInputStream, mediaFile);
                
                if(Files.exists(mediaFile) && Files.size(mediaFile) > 0) {
                    logger.debug("Successfully downloaded file {}", mediaFile);
                    //upload successful. TODO: check file integrity?
                    CMSMediaItem item = createMediaItem(mediaFile);
                    requiredCategory.ifPresent(cat -> item.addCategory(cat));                   
                    
                    
                    DataManager.getInstance().getDao().addCMSMediaItem(item);
                    MediaItem jsonItem = new MediaItem(item);
                    return Response.status(Status.ACCEPTED).entity(jsonItem).build();
                } else {
                    String message = Messages.translate("admin__media_upload_error", servletRequest.getLocale(), mediaFile.getFileName().toString());
                    if(Files.exists(mediaFile)) {
                        Files.delete(mediaFile);
                    }
                    return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();
                    
                }
            } catch (AccessDeniedException e) {
                return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
            } catch(FileAlreadyExistsException e) {
                String message = Messages.translate("admin__media_upload_error_exists", servletRequest.getLocale(), mediaFile.getFileName().toString());
                return Response.status(Status.CONFLICT).entity(message).build();
            } catch(IOException | DAOException e) {
                logger.error("Error uploading media file", e);
                String message = Messages.translate("admin__media_upload_error", servletRequest.getLocale(), mediaFile.getFileName().toString(), e.getMessage());
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(message).build();
            }
        }
    }
    
    /**
     * Return an Optional containing a {@link CMSCategory} for which the user has access rights if the user in a CmsAdmin but has limited category rights
     * If the user has unlimited category rights, return an empty optional
     * 
     * @param user
     * @return
     * @throws DAOException 
     * @throws AccessDeniedException    if the user is not allowed to use any categories whatsoever
     */
    private Optional<CMSCategory> getRequiredCategoryForUser(User user) throws DAOException, AccessDeniedException {

        if(!user.hasPrivilegeForAllCategories()) {
            List<CMSCategory> allowedCategories = user.getAllowedCategories(DataManager.getInstance().getDao().getAllCategories());
            if(!allowedCategories.isEmpty()) {
                return Optional.of(allowedCategories.get(0));
            } else {
                throw new AccessDeniedException("The user " + user + " has no rights to any categories and may therefore not upload any media files");
            }
        }
        return Optional.empty();
    }

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
     * @param session
     * @return
     */
    private Optional<User> getUser() {
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

    public class MediaList {

        private final List<MediaItem> mediaItems;

        public MediaList(List<CMSMediaItem> items) {
            this.mediaItems = items.stream().map(MediaItem::new).collect(Collectors.toList());
        }

        /**
         * @return the mediaItems
         */
        public List<MediaItem> getMediaItems() {
            return mediaItems;
        };

    }

    public class MediaItem {

        @JsonSerialize(using = MetadataSerializer.class)
        private final IMetadataValue label;
        @JsonSerialize(using = MetadataSerializer.class)
        private final IMetadataValue description;
        private final String link;
        private final ImageContent image;
        private final List<String> tags;

        public MediaItem(CMSMediaItem source) {
            this.label = source.getTranslationsForName();
            this.description = source.getTranslationsForDescription();
            this.image = new ImageContent(source.getIconURI());
            if(IIIFUrlResolver.isIIIFImageUrl(source.getIconURI().toString())) {   
                URI imageInfoURI = URI.create(IIIFUrlResolver.getIIIFImageBaseUrl(source.getIconURI().toString()));
                this.image.setService(new ImageInformation(imageInfoURI.toString()));
            }
            this.link = Optional.ofNullable(source.getLinkURI(servletRequest)).map(URI::toString).orElse("#");
            this.tags = source.getCategories().stream().map(CMSCategory::getName).collect(Collectors.toList());
        }

        /**
         * @return the label
         */
        public IMetadataValue getLabel() {
            return label;
        }

        /**
         * @return the description
         */
        public IMetadataValue getDescription() {
            return description;
        }

        /**
         * @return the link
         */
        public String getLink() {
            return link;
        }

        /**
         * @return the image
         */
        public ImageContent getImage() {
            return image;
        }

        /**
         * @return the tags
         */
        public List<String> getTags() {
            return tags;
        }

    }
}
