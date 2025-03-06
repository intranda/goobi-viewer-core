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
package io.goobi.viewer.controller.imaging;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.intranda.api.iiif.image.ImageInformation;
import de.intranda.api.iiif.image.ImageTile;
import de.intranda.api.iiif.image.v3.ImageInformation3;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.util.PathConverter;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.RestApiManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;

/**
 * Provides urls to download pdfs, images and image footer
 *
 * @author Florian Alpers
 */
public class ImageHandler {

    private static final Logger logger = LogManager.getLogger(ImageHandler.class);

    private final AbstractApiUrlManager urls;

    public ImageHandler() {
        this.urls = null;
    }

    /**
     * @param contentUrlManager
     */
    public ImageHandler(AbstractApiUrlManager contentUrlManager) {
        this.urls = contentUrlManager;
    }

    /**
     * Returns the image link for the given page and pageType. For external images, this links to the IIIF image information json+ls For external
     * images, this may either also be a IIIF image information or the image itself
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return a {@link java.lang.String} object.
     */
    public String getImageUrl(PhysicalElement page, PageType pageType) {
        if (page == null) {
            throw new IllegalArgumentException("Cannot get image url: PhysicalElement is null");
        }
        if (pageType == null) {
            throw new IllegalArgumentException("Cannot get image url: PageType is null");
        }

        String pi = page.getPi();
        String filepath = page.getFilepath();
        String filename = page.getFileName();

        return getImageUrl(pageType, pi, filepath, filename);

    }

    public String getImageUrl(PageType pageType, String pi, String filepath) {
        Path path = Paths.get(filepath);
        return getImageUrl(pageType, pi, filepath, path.getFileName().toString());
    }

    /**
     * @param pageType
     * @param pi
     * @param filepath
     * @param filename
     * @return Generated URL
     */
    public String getImageUrl(PageType pageType, String pi, String filepath, String filename) {
        String escPi = StringTools.encodeUrl(pi);
        String escFilename = StringTools.encodeUrl(filename);
        if (isRestrictedUrl(filepath)) {
            String escFilepath = StringTools.escapeCriticalUrlChracters(filepath, true);
            if (this.urls != null) {
                ApiPath path = this.urls.path(ApiUrls.EXTERNAL_IMAGES, "/info.json").params(escFilepath);
                if (pageType != null) {
                    path = path.query("pageType", pageType.name());
                }
                return path.build();
            }
            StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getIIIFApiUrl());
            sb.append("image/-/").append(escFilepath).append("/info.json");
            return sb.toString();
        } else if (isExternalUrl(filepath)) {
            return filepath;
        } else if (urls != null) {
            ApiPath path = this.urls.path(ApiUrls.RECORDS_FILES_IMAGE, ApiUrls.RECORDS_FILES_IMAGE_INFO).params(escPi, escFilename);
            if (pageType != null) {
                path = path.query("pageType", pageType.name());
            }
            return path.build();
        } else {
            StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getIIIFApiUrl());
            sb.append("image").append("/").append(escPi).append("/").append(escFilename).append("/info.json");
            return sb.toString();
        }
    }

    /**
     * Returns the image link for the given page. For external images, this links to the IIIF image information json+ls For external images, this may
     * either also be a IIIF image information or the image itself
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String getImageUrl(PhysicalElement page) {
        return getImageUrl(page, PageType.viewImage);
    }

    /**
     * <p>
     * getImageInformation.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @param pageType
     * @return The image information for the image file of the given page
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws ViewerConfigurationException
     */
    public ImageInformation getImageInformation(PhysicalElement page, PageType pageType)
            throws ContentLibException, ViewerConfigurationException, URISyntaxException {

        URI fileUri = new URI(getIIIFBaseUrl(page.getFilepath()));
        int width = page.getImageWidth(); //0 if width is not known
        int height = page.getImageHeight(); //0 if height is not known
        //        if (fileUri.getScheme() != null && fileUri.getScheme().matches("^http.*")) {
        //            return new ImageInformation(fileUri);
        //        }
        URI apiUri;
        if (fileUri.getScheme() != null && fileUri.getScheme().matches("^http.*")) {
            if (width * height == 0) {
                return new ImageInformation(fileUri); //no internal size information. return external url
            } else {
                apiUri = fileUri; //use external url as if for internal imageInformation
            }
        } else {
            apiUri = urls.path(ApiUrls.RECORDS_FILES_IMAGE).params(page.getPi(), PathConverter.getPath(fileUri).getFileName().toString()).buildURI(); //create interl imageInformation uri
        }

        Map<Integer, List<Integer>> tileSizes = DataManager.getInstance().getConfiguration().getTileSizes(pageType, page.getMimeType());
        List<Integer> sizes = DataManager.getInstance()
                .getConfiguration()
                .getImageViewZoomScales(pageType, page.getMimeType())
                .stream()
                .filter(s -> s.matches("\\d{1,9}"))
                .map(Integer::parseInt)
                .toList();

        ImageInformation info = (Version.v2 == RestApiManager.getVersionToUseForIIIF()) ? new ImageInformation(apiUri)
                : new ImageInformation3(apiUri);
        info.setWidth(width);
        info.setHeight(height);
        info.setTiles(tileSizes.entrySet()
                .stream()
                .sorted((e1, e2) -> Integer.compare(e1.getKey(), e2.getKey()))
                .map(entry -> new ImageTile(entry.getKey(), entry.getKey(), entry.getValue()))
                .toList());
        info.setSizesFromDimensions(
                new ArrayList<>(sizes.stream()
                        .sorted()
                        .map(scale -> new Dimension(scale, (int) (scale * height / (double) width)))
                        .toList()));

        return info;
    }

    /**
     * Get the IIIF base url (resource id) from an iiif info.json url or a iiif image url. If the given url does not match a iiif image resource
     * pattern, return the unchanged url
     * 
     * @param url
     * @return the base url/id of the given iiif image resource.
     */
    public static String getIIIFBaseUrl(String url) {
        return IIIFUrlResolver.getIIIFImageBaseUrl(url).replaceAll("\\/info\\.json\\/?", "");
    }

    /**
     * <p>
     * getImageInformation.
     * </p>
     *
     * @param page a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @return The image information for the image file of the given page
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public ImageInformation getImageInformation(PhysicalElement page)
            throws ContentLibException, URISyntaxException, PresentationException, IndexUnreachableException {
        String path = page.getFilepath();
        String url = null;
        if (isExternalUrl(path)) {
            url = path;
        } else {
            url = DataFileTools.getDataFilePath(page.getPi(), DataManager.getInstance().getConfiguration().getMediaFolder(), null, page.getFilepath())
                    .toUri()
                    .toString();
        }

        logger.trace(url);
        return getImageInformation(url);
    }

    /**
     * <p>
     * getImageInformation.
     * </p>
     *
     * @param url a {@link java.lang.String} object.
     * @return a {@link de.intranda.api.iiif.image.ImageInformation} object.
     * @throws java.net.URISyntaxException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException if any.
     */
    public ImageInformation getImageInformation(final String url) throws URISyntaxException, ContentLibException {
        PageSource imageSource = new PageSource(0, url.replace("info.json", "full/max/0/default.jpg"), Collections.emptyMap());
        try (ImageManager manager = new ImageManager(imageSource.getImageUri())) {
            return manager.getImageInformation(new URI(""));
        } catch (FileNotFoundException e) {
            throw new ContentLibException("Cannot resolve url " + url.replace("info.json", "full/max/0/default.jpg") + " to existing resource");
        }
    }

    /**
     * <p>
     * isExternalUrl.
     * </p>
     *
     * @param path a {@link java.lang.String} object.
     * @return true exactly if the given path starts with {@code http://} or {@code https://}
     */
    public static boolean isExternalUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    /**
     * <p>
     * isImageUrl.
     * </p>
     *
     * @param displayableTypesOnly if true, the method only returns true for images that can be directly displayed in a browser (jpg and png)
     * @return true if the url ends with an image file suffix
     * @param url a {@link java.lang.String} object.
     */
    protected static boolean isImageUrl(String url, boolean displayableTypesOnly) {
        String extension = FilenameUtils.getExtension(StringTools.removeTrailingSlashes(url).toLowerCase());
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
     * <p>
     * isRestrictedUrl.
     * </p>
     *
     * @return true if the path is an external url which has restricted access and must therefore be delivered via the contenetServer
     * @param path a {@link java.lang.String} object.
     */
    public static boolean isRestrictedUrl(String path) {
        return DataManager.getInstance()
                .getConfiguration()
                .getRestrictedImageUrls()
                .stream()
                .anyMatch(regex -> Pattern.compile(regex).matcher(path).matches());
    }

}
