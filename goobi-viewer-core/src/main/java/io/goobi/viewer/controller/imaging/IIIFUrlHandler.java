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

import static io.goobi.viewer.api.rest.v1.ApiUrls.EXTERNAL_IMAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.EXTERNAL_IMAGES_IIIF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_IMAGE_IIIF;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.api.iiif.IIIFUrlResolver;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;

/**
 * <p>
 * IIIFUrlHandler class.
 * </p>
 *
 * @author Florian Alpers
 */
public class IIIFUrlHandler {
    /**
     *
     */
    private static final Logger logger = LogManager.getLogger(IIIFUrlHandler.class);

    private final AbstractApiUrlManager urls;

    /**
     * 
     * @param urls
     */
    public IIIFUrlHandler(AbstractApiUrlManager urls) {
        this.urls = urls;
    }

    public IIIFUrlHandler(URI apiUrl) {
        this.urls = new ApiUrls(apiUrl.toString());
    }

    public IIIFUrlHandler() {
        this.urls = null;
    }

    /**
     * 
     * @param fileUrl
     * @param docStructIdentifier
     * @param region
     * @param size
     * @param rotation
     * @param quality
     * @param format
     * @return Generated URL
     */
    public String getIIIFImageUrl(String fileUrl, String docStructIdentifier, String region, String size, String rotation, String quality,
            String format) {
        String apiUrl = this.urls == null ? DataManager.getInstance().getConfiguration().getIIIFApiUrl() : this.urls.getApiUrl() + "/";
        return getIIIFImageUrl(apiUrl, fileUrl, docStructIdentifier, region, size, rotation,
                quality, format);
    }

    /**
     * Returns a link to the actual image of the given page, delivered via IIIF api using the given parameters
     *
     * @param apiUrl
     * @param inFileUrl a {@link java.lang.String} object.
     * @param docStructIdentifier
     * @param region a {@link java.lang.String} object.
     * @param size a {@link java.lang.String} object.
     * @param rotation a {@link java.lang.String} object.
     * @param quality a {@link java.lang.String} object.
     * @param format a {@link java.lang.String} object.
     * @return Generated URL
     */
    public String getIIIFImageUrl(String apiUrl, final String inFileUrl, final String docStructIdentifier, String region, String size,
            String rotation, String quality, String format) {
        String fileUrl = inFileUrl;
        try {
            if (PathConverter.isInternalUrl(fileUrl) || ImageHandler.isRestrictedUrl(fileUrl)) {
                try {
                    URI uri = PathConverter.toURI(fileUrl);
                    if (StringUtils.isBlank(uri.getScheme())) {
                        uri = new URI("file", fileUrl, null);
                    }
                    fileUrl = uri.toString();
                } catch (URISyntaxException e) {
                    logger.warn("file url {} is not a valid url: {}", fileUrl, e.getMessage());
                }
                if (urls != null) {
                    fileUrl = StringTools.escapeCriticalUrlChracters(fileUrl, false);
                    return urls.path(EXTERNAL_IMAGES, EXTERNAL_IMAGES_IIIF).params(fileUrl, region, size, rotation, quality, format).toString();
                }
                StringBuilder sb = new StringBuilder(apiUrl);
                sb.append("image/-/").append(StringTools.escapeCriticalUrlChracters(fileUrl, false));
                return IIIFUrlResolver.getIIIFImageUrl(sb.toString(), region, size, rotation, quality, format);
            } else if (ImageHandler.isExternalUrl(fileUrl)) {
                fileUrl = fileUrl.replace(" ", "+"); // Hotfix for URIs that contain spaces in the image file name
                if (IIIFUrlResolver.isIIIFImageUrl(fileUrl)) {
                    return IIIFUrlResolver.getModifiedIIIFFUrl(fileUrl, region, size, rotation, quality, format);
                } else if (ImageHandler.isImageUrl(fileUrl, false)) {
                    if (urls != null) {
                        fileUrl = StringTools.escapeCriticalUrlChracters(fileUrl, false);
                        return urls.path(EXTERNAL_IMAGES, EXTERNAL_IMAGES_IIIF).params(fileUrl, region, size, rotation, quality, format).toString();
                    }
                    StringBuilder sb = new StringBuilder(apiUrl);
                    if (!apiUrl.endsWith("/")) {
                        sb.append("/");
                    }
                    sb.append("image/-/").append(StringTools.escapeCriticalUrlChracters(fileUrl, false)).append("/");
                    sb.append(region).append("/");
                    sb.append(size).append("/");
                    sb.append(rotation).append("/");
                    sb.append("default.").append(format);
                    //                  thumbCompression.ifPresent(compr -> sb.append("?compression=").append(thumbCompression));
                    return sb.toString();
                } else {
                    //assume its a iiif id
                    if (fileUrl.endsWith("info.json")) {
                        fileUrl = fileUrl.substring(0, fileUrl.length() - 9);
                    }
                    StringBuilder sb = new StringBuilder(fileUrl);
                    sb.append(region).append("/");
                    sb.append(size).append("/");
                    sb.append(rotation).append("/");
                    sb.append("default.").append(format);
                    return sb.toString();
                }
            } else {
                String usedocStructIdentifier = docStructIdentifier;
                if (urls != null) {
                    //In the case of multivolume thumbnails in first-volume mode, a path consisting of both pi and filename is given.
                    //parse that path to get correct url for thumbnail
                    //TODO: find a more robust solution
                    Path filePath = Paths.get(fileUrl);
                    if (filePath.getNameCount() == 2) {
                        usedocStructIdentifier = filePath.getName(0).toString();
                        fileUrl = filePath.getName(1).toString();
                    }
                    return urls.path(RECORDS_FILES_IMAGE, RECORDS_FILES_IMAGE_IIIF)
                            .params(URLEncoder.encode(usedocStructIdentifier, StringTools.DEFAULT_ENCODING),
                                    URLEncoder.encode(fileUrl, StringTools.DEFAULT_ENCODING), region, size, rotation,
                                    "default", format)
                            .build();
                }
                //if the fileUrl contains a "/", then the part before that is the actual docStructIdentifier
                int separatorIndex = fileUrl.indexOf("/");
                if (separatorIndex > 0) {
                    usedocStructIdentifier = fileUrl.substring(0, separatorIndex);
                    fileUrl = fileUrl.substring(separatorIndex + 1);
                }

                StringBuilder sb = new StringBuilder(apiUrl);
                sb.append("image/{pi}/{filename}"
                        .replace("{pi}", URLEncoder.encode(usedocStructIdentifier, StringTools.DEFAULT_ENCODING))
                        .replace("{filename}",
                                URLEncoder.encode(fileUrl, StringTools.DEFAULT_ENCODING)))
                        .append("/");
                sb.append(region).append("/");
                sb.append(size).append("/");
                sb.append(rotation).append("/");
                sb.append("default.").append(format);
                //                thumbCompression.ifPresent(compr -> sb.append("?compression=").append(thumbCompression));
                return sb.toString();
            }
        } catch (URISyntaxException e) {
            logger.error("Not a valid URL: {} ({})", fileUrl, e.getMessage());
            return "";
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage());
            return "";
        }
    }

    /**
     * Appends image request parameter paths to the given baseUrl
     *
     * @param baseUrl a {@link java.lang.String} object.
     * @param region a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest} object.
     * @param size a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @param rotation a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation} object.
     * @param quality a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype} object.
     * @param format a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat} object.
     * @return a {@link java.lang.String} object.
     */
    public String getIIIFImageUrl(final String baseUrl, RegionRequest region, Scale size, Rotation rotation, Colortype quality,
            ImageFileFormat format) {
        StringBuilder url = new StringBuilder(StringTools.appendTrailingSlash(baseUrl));
        url.append(region).append("/");
        url.append(size).append("/");
        url.append(Math.round(rotation.getRotation())).append("/");
        url.append(quality.getLabel()).append(".");
        url.append(format.getFileExtension());

        return url.toString();
    }

    /**
     * Replaces the image request parameters in an IIIF URL with the given ones
     *
     * @param url a {@link java.lang.String} object.
     * @should replace dimensions correctly
     * @should do nothing if not iiif url
     * @param region a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest} object.
     * @param size a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @param rotation a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation} object.
     * @param quality a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype} object.
     * @param format a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat} object.
     * @return a {@link java.lang.String} object.
     */
    public String getModifiedIIIFFUrl(String url, RegionRequest region, Scale size, Rotation rotation, Colortype quality, ImageFileFormat format) {
        return IIIFUrlResolver.getModifiedIIIFFUrl(url, region == null ? null : region.toString(), size == null ? null : size.toString(),
                rotation == null ? null : rotation.toString(), quality == null ? null : quality.toString(),
                format == null ? null : format.getFileExtension());
    }

    /**
     * @return the urls
     */
    public AbstractApiUrlManager getUrlManager() {
        return urls;
    }

    public boolean isIIIFUrl(String url) {
        try {
            URI uri = PathConverter.toURI(url);
            return uri.getScheme() != null && uri.getScheme().matches("https?") && uri.getPath() != null && uri.getPath().endsWith("/info.json");
        } catch (URISyntaxException e) {
            logger.error("No valid url: " + url);
            return false;
        }
    }

}
