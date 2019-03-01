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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.dao.IDAO;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItemMetadata;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.Category;
import de.intranda.digiverso.presentation.model.viewer.BrowseDcElement;

@Named
@SessionScoped
public class CmsMediaBean implements Serializable {

    private static final long serialVersionUID = 1156829371382069634L;

    private static final Logger logger = LoggerFactory.getLogger(CmsMediaBean.class);

    private String selectedTag;
    //    private List<CMSMediaItem> mediaItems;

    /**
     * @param mediaFile2
     * @param contentType
     * @return false if the content type is html or xml and the file contains the string "<script" (case insensitive)
     * @throws IOException
     */
    private static boolean validate(File file, String contentType) throws IOException {
        if (CMSMediaItem.CONTENT_TYPE_HTML.equals(contentType) || CMSMediaItem.CONTENT_TYPE_XML.equals(contentType)) {
            String content = FileUtils.readFileToString(file);
            return !content.toLowerCase().contains("<script");
        }

        return true;
    }

    /**
     * @param contentType
     * @param fileName
     * @return true if supported; false otherwise
     * @should return true for tiff
     * @should return true for jpeg
     * @should return true for jpeg 2000
     * @should return true for png
     * @should return true for docx
     */
    private static boolean isValidMediaType(String contentType, String fileName) {
        logger.trace("isValidMediaType: {} - {}", contentType, fileName);
        switch (contentType) {
            case "image/tiff":
            case "image/jpeg":
            case "image/jp2":
            case "image/png":
            case CMSMediaItem.CONTENT_TYPE_DOC: // RTF 
            case CMSMediaItem.CONTENT_TYPE_DOCX:
            case CMSMediaItem.CONTENT_TYPE_HTML:
            case CMSMediaItem.CONTENT_TYPE_RTF:
            case CMSMediaItem.CONTENT_TYPE_RTF2:
            case CMSMediaItem.CONTENT_TYPE_RTF3:
            case CMSMediaItem.CONTENT_TYPE_RTF4:
            case CMSMediaItem.CONTENT_TYPE_XML:
                return true;
            default:
                logger.warn("Unsupported media type: {}", contentType);
                return false;
        }
    }

    public CMSMediaItem createMediaItem() {
        CMSMediaItem item = new CMSMediaItem();
        for (Locale locale : CmsBean.getAllLocales()) {
            CMSMediaItemMetadata metadata = new CMSMediaItemMetadata();
            metadata.setLanguage(locale.getLanguage());
            item.addMetadata(metadata);
        }
        return item;
    }

    public List<CMSPage> getMediaOwnerPages(CMSMediaItem item) throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        List<CMSPage> owners = new ArrayList<>();
        if (dao != null) {
            owners = dao.getMediaOwners(item);
        }
        return owners;
    }

    public void deleteMedia(CMSMediaItem item) throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        if (dao != null) {
            if (!dao.deleteCMSMediaItem(item)) {
                logger.error("Failed to delete media item");
            } else if (item.getFileName() != null) {
                try {
                    Path mediaFile = item.getFilePath();
                    Files.delete(mediaFile);
                    if (Files.exists(mediaFile)) {
                        throw new IOException("Cannot delete file " + mediaFile.toAbsolutePath());
                    }
                } catch (IOException e) {
                    logger.error("Failed to delete media file: " + e.getMessage());
                }
            }
        }
    }

    public static List<CMSMediaItem> getAllMedia() throws DAOException {
        return DataManager.getInstance().getDao().getAllCMSMediaItems();
    }

    public List<CMSMediaItem> getMediaItems(Category category, String filenameFilter) throws DAOException {
        Stream<CMSMediaItem> items = getAllMedia().stream();
        if (category != null) {
            items = items.filter(item -> item.getCategories().contains(category));
        }
        if (StringUtils.isNotBlank(filenameFilter)) {
            items = items.filter(item -> item.getFileName().matches(filenameFilter));
        }
        List<CMSMediaItem> list = items.sorted().collect(Collectors.toList());
        list.forEach(item -> {
        	System.out.println("Item " + item.toString() + " Name = " + item.getName());
        });
        return list;
    }

    public static String getMediaUrl(CMSMediaItem item) throws NumberFormatException, ViewerConfigurationException {
        return getMediaUrl(item, null, null);
    }

    /**
     * @param item
     * @param width
     * @param height
     * @return
     * @throws ViewerConfigurationException
     */
    public static String getMediaUrl(CMSMediaItem item, String width, String height) throws ViewerConfigurationException {
        if (item == null || StringUtils.isEmpty(item.getFileName())) {
            return "";
        }

        if (item.isHasExportableText()) {
            StringBuilder sbUri = new StringBuilder();
            sbUri.append(DataManager.getInstance().getConfiguration().getRestApiUrl()).append("cms/media/get/item/").append(item.getId());
            return sbUri.toString();
        }
        
        String url = BeanUtils.getImageDeliveryBean()
                .getThumbs()
                .getThumbnailUrl(Optional.ofNullable(item), StringUtils.isNotBlank(width) ? Integer.parseInt(width) : 0,
                        StringUtils.isNotBlank(height) ? Integer.parseInt(height) : 0);
        
        return url;
    }

    /**
     * 
     * @param item
     * @return
     * @throws ViewerConfigurationException
     */
    public static String getMediaFileAsString(CMSMediaItem item) throws ViewerConfigurationException {
        if (item == null || StringUtils.isEmpty(item.getFileName())) {
            return "";
        }

        StringBuilder sbUri = new StringBuilder();
        sbUri.append(DataManager.getInstance().getConfiguration().getRestApiUrl()).append("cms/media/get/item/").append(item.getId());
        try {
            String ret = Helper.getWebContentGET(sbUri.toString());
            return ret;
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (HTTPException e) {
            logger.error(e.getMessage(), e);
        }

        return "";
    }

    public static String getMediaPreviewUrl(CMSMediaItem item) throws NumberFormatException, ViewerConfigurationException {
        if (item != null && item.getFileName() != null) {
            return getMediaUrl(item, null, "160");
        }
        return "";
    }

    public boolean isImage(CMSMediaItem item) {
        return item.getFileName().matches(getImageFilter());
    }

    public boolean isText(CMSMediaItem item) {
        return !item.getFileName().matches(getImageFilter());
    }


    public void saveMedia(CMSMediaItem media) throws DAOException {
        if (media != null && media.getId() == null) {
            // currentMediaItem.setFileName(mediaFile.getName());
            //            currentMediaItem.processMediaFile(mediaFile);
            DataManager.getInstance().getDao().addCMSMediaItem(media);
            //            setCurrentMediaItem(null);
        } else if (media != null && media.getId() != null) {
        	media.processMediaFile(media.getFilePath());
            DataManager.getInstance().getDao().updateCMSMediaItem(media);
            //            setCurrentMediaItem(null);
        }
    }

    public static String getFileName(Part filePart) {
        if (filePart != null) {
            // String basename = filePart.getName();
            // if (basename.startsWith(".")) {
            // basename = basename.substring(1);
            // }
            // if (basename.contains("/")) {
            // basename = basename.substring(basename.lastIndexOf("/") + 1);
            // }
            // if (basename.contains("\\")) {
            // basename = basename.substring(basename.lastIndexOf("\\") + 1);
            // }
            //
            // return basename;
            String header = filePart.getHeader("content-disposition");
            for (String headerPart : header.split(";")) {
                if (headerPart.trim().startsWith("filename")) {
                    return headerPart.substring(headerPart.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return null;
    }


    /**
     * @param selectedTag the selectedTag to set
     */
    public void setSelectedTag(String selectedTag) {
        this.selectedTag = selectedTag;
    }

    /**
     * @return the selectedTag
     */
    public String getSelectedTag() {
        return selectedTag;
    }

    /**
     * @return
     * @throws DAOException
     */
    public List<Category> getAllMediaCategories() throws DAOException {
        return DataManager.getInstance().getDao().getAllCategories();
    }

    public Collection<CMSMediaItem.DisplaySize> getMediaItemDisplaySizes() {
        Set<CMSMediaItem.DisplaySize> sizes = EnumSet.allOf(CMSMediaItem.DisplaySize.class);
        return sizes;
    }

    /**
     * 
     * @return a regex matching only filenames ending with one of the supported image format suffixes
     */
    public String getImageFilter() {
        return "(?i).*\\.(png|jpe?g|gif|tiff?|jp2)";
    }

}
