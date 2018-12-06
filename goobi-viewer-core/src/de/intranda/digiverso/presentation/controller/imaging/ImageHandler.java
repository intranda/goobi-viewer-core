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
package de.intranda.digiverso.presentation.controller.imaging;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.intranda.digiverso.presentation.servlets.rest.content.ContentResource;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;

/**
 * Provides urls to download pdfs, images and image footer
 * 
 * @author Florian Alpers
 *
 */
public class ImageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ImageHandler.class);

    /**
     * Returns the image link for the given page and pageType. For external images, this links to the IIIF image information json+ls For external
     * images, this may either also be a IIIF image information or the image itself
     * 
     * @param page
     * @return
     */
    public String getImageUrl(PhysicalElement page, PageType pageType) {
        if (page == null) {
            throw new IllegalArgumentException("Cannot get image url: PhysicalElement is null");
        }
        if (pageType == null) {
            throw new IllegalArgumentException("Cannot get image url: PageType is null");
        }

        String pageName;
        switch (pageType) {
            case viewFullscreen:
                pageName = "fullscreen/image";
                break;
            case editContent:
            case editOcr:
            case editHistory:
                pageName = "crowdsourcing/image";
                break;
            default:
                pageName = "image";

        }

        try {
            if (isRestrictedUrl(page.getFilepath())) {
                StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getRestApiUrl());
                sb.append(pageName).append("/-/").append(BeanUtils.escapeCriticalUrlChracters(page.getFilepath(), true)).append("/info.json");
                return sb.toString();
            } else if (isExternalUrl(page.getFilepath())) {
                return page.getFilepath();
            } else {
                StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getRestApiUrl());
                sb.append(pageName).append("/").append(page.getPi()).append("/").append(page.getFileName()).append("/info.json");
                return sb.toString();
            }
        } catch (ViewerConfigurationException e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    /**
     * Returns the image link for the given page. For external images, this links to the IIIF image information json+ls For external images, this may
     * either also be a IIIF image information or the image itself
     * 
     * @param page
     * @return
     */
    public String getImageUrl(PhysicalElement page) {
        return getImageUrl(page, PageType.viewImage);
    }

    /**
     * 
     * @param page
     * @param dataRepository
     * @return The image information for the image file of the given page
     * @throws IllegalPathSyntaxException
     * @throws ContentLibException
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public ImageInformation getImageInformation(PhysicalElement page, String dataRepository)
            throws IllegalPathSyntaxException, ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException {
        String path = page.getFilepath();
        String url = null;
        if (isExternalUrl(path)) {
            url = path;
        } else {
            StringBuilder sbUrl = new StringBuilder();
            if (dataRepository != null) {
                if (!Paths.get(dataRepository).isAbsolute()) {
                    sbUrl.append(DataManager.getInstance().getConfiguration().getDataRepositoriesHome());
                }
                sbUrl.append(dataRepository).append('/').append(DataManager.getInstance().getConfiguration().getMediaFolder()).append('/');
            }
            sbUrl.append(page.getPi()).append('/').append(page.getFilepath());
            url = Paths.get(sbUrl.toString()).toUri().toString();
        }
        
        logger.trace(url);
        return getImageInformation(url);
    }

    /**
     * @param url
     * @return
     * @throws IllegalPathSyntaxException
     * @throws URISyntaxException
     * @throws ContentLibException
     */
    public ImageInformation getImageInformation(String url) throws IllegalPathSyntaxException, URISyntaxException, ContentLibException {
        if (url.endsWith("info.json")) {
            url = url.replace("info.json", "full/max/0/default.jpg");
        }
        PageSource imageSource = new PageSource(0, url, Collections.emptyMap());
        ImageInformation info = new ImageInformation(imageSource.getImageUri(), new URI(""));
        return info;
    }

    /**
     * @param path
     * @return true exactly if the given path starts with {@code http://} or {@code https://}
     */
    protected static boolean isExternalUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    /**
     * 
     * @param displayableTypesOnly if true, the method only returns true for images that can be directly displayed in a browser (jpg and png)
     * @return true if the url ends with an image file suffix
     */
    protected static boolean isImageUrl(String url, boolean displayableTypesOnly) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String extension = FilenameUtils.getExtension(url.toLowerCase());
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
                return true;
            case "tif":
            case "tiff":
            case "jp2":
                return !displayableTypesOnly;
            default:
                return false;
        }
    }

    /**
     * @return true if the path is an external url which has restricted access and must therefore be delivered via the contenetServer
     */
    public static boolean isRestrictedUrl(String path) {
        return DataManager.getInstance().getConfiguration().getRestrictedImageUrls().stream().anyMatch(
                regex -> Pattern.compile(regex).matcher(path).matches());
    }

    protected static ImageType getImageType(ImageInformation info) {
        String id = info.getId();
        ImageFileFormat iff = ImageFileFormat.getImageFileFormatFromFileExtension(id);
        if (iff != null) {
            return new ImageType(iff);
        }
        return null;
    }

}
