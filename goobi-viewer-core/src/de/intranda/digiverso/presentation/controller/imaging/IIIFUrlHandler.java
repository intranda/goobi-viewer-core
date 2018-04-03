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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;

/**
 * @author Florian Alpers
 *
 */
public class IIIFUrlHandler {

    /**
     * Regex to match any calls to images via iiif (not image information!). This includes the following capturing groups:
     * <ul>
     * <li>1: region</li>
     * <li>2: size</li>
     * <li>3: rotation</li>
     * <li>4: quality</li>
     * <li>5: output format</li>
     * </ul>
     */
    public static final String IIIF_IMAGE_REGEX =
            "https?:\\/\\/.*\\/((?:pct:)?(?:\\d+,\\d+,\\d+,\\d+)|full|square)\\/((?:pct:\\d{1,2})|!?(?:(?:\\d+)?,(?:\\d+)?)|full|max)\\/(!?-?\\d{1,3})\\/(default|bitonal|gray|color).(jpg|png|tif|jp2|pdf)(?:\\?.*)";
    public static final int IIIF_IMAGE_REGEX_REGION_GROUP = 1;
    public static final int IIIF_IMAGE_REGEX_SIZE_GROUP = 2;
    public static final int IIIF_IMAGE_REGEX_ROTATION_GROUP = 3;
    public static final int IIIF_IMAGE_REGEX_QUALITY_GROUP = 4;
    public static final int IIIF_IMAGE_REGEX_FORMAT_GROUP = 5;
    public static final String IIIF_IMAGE_REQUEST_TEMPLATE = "{region}/{size}/{rotation}/{quality}.{format}";

    /**
     * Returns a link to the actual image of the given page, delivered via IIIF api using the given parameters
     * 
     * @param page
     * @param region
     * @param size
     * @param rotation
     * @param thumbCompression
     * @return
     */
    public String getIIIFImageUrl(String fileUrl, String docStructIdentifier, String region, String size, String rotation, String quality, String format, int thumbCompression) {
        if(ImageHandler.isInternalUrl(fileUrl) || ImageHandler.isRestrictedUrl(fileUrl)) {
            StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
            sb.append("image/-/").append(BeanUtils.escapeCriticalUrlChracters(fileUrl)).append("/");
            return getIIIFImageUrl(sb.toString(), region, size, rotation, quality, format);
        } else if (ImageHandler.isExternalUrl(fileUrl)) {
            if (isIIIFImageUrl(fileUrl)) {
                return getModifiedIIIFFUrl(fileUrl, region, size, rotation, quality, format);
            } else if (ImageHandler.isImageUrl(fileUrl, false)) {
                StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
                sb.append("image/-/").append(BeanUtils.escapeCriticalUrlChracters(fileUrl)).append("/");
                sb.append(region).append("/");
                sb.append(size).append("/");
                sb.append(rotation).append("/");
                sb.append("default.").append(format);
                sb.append("?compression=").append(thumbCompression);
                return sb.toString();
            } else {
                //assume its a iiif id
                if(fileUrl.endsWith("info.json")) {
                    fileUrl = fileUrl.substring(0, fileUrl.length()-9);
                }
                StringBuilder sb = new StringBuilder(fileUrl);
                sb.append(region).append("/");
                sb.append(size).append("/");
                sb.append(rotation).append("/");
                sb.append("default.").append(format);
                return sb.toString();
            }
        } else {
            StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
            sb.append("image/").append(docStructIdentifier).append("/").append(fileUrl).append("/");
            sb.append(region).append("/");
            sb.append(size).append("/");
            sb.append(rotation).append("/");
            sb.append("default.").append(format);
            sb.append("?compression=").append(thumbCompression);
            return sb.toString();
        }
    }
    
    /**
     * Appends image request parameter paths to the given baseUrl
     * 
     * @param baseUrl
     * @return
     */
    public String getIIIFImageUrl(String baseUrl, String region, String size, String rotation, String quality, String format) {
        if (StringUtils.isNotBlank(baseUrl) && !baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        StringBuilder url = new StringBuilder(baseUrl);
        url.append(region).append("/");
        url.append(size).append("/");
        url.append(rotation).append("/");
        url.append(quality).append(".");
        url.append(format).append("/");
        
        return url.toString();
    }

    /**
     * Appends image request parameter paths to the given baseUrl
     * 
     * @param baseUrl
     * @return
     */
    public String getIIIFImageUrl(String baseUrl, RegionRequest region, Scale size, Rotation rotation, Colortype quality, ImageFileFormat format) {
        if (StringUtils.isNotBlank(baseUrl) && !baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        StringBuilder url = new StringBuilder(baseUrl);
        url.append(region).append("/");
        url.append(size).append("/");
        url.append(Math.round(rotation.getRotation())).append("/");
        url.append(quality.getLabel()).append(".");
        url.append(format.getFileExtension()).append("/");
        
        return url.toString();
    }

    /**
     * Replaces the image request parameters in an IIIF URL with the given ones
     * 
     * @param url
     * @param width
     * @param height
     * @return
     * @should replace dimensions correctly
     * @should do nothing if not iiif url
     */
    public String getModifiedIIIFFUrl(String url, String region, String size, String rotation, String quality, String format) {

        Matcher matcher = Pattern.compile(IIIF_IMAGE_REGEX).matcher(url);
        if (matcher.matches()) {
            url = replaceGroup(url, region, matcher, IIIF_IMAGE_REGEX_REGION_GROUP);
            url = replaceGroup(url, size, matcher, IIIF_IMAGE_REGEX_SIZE_GROUP);
            url = replaceGroup(url, rotation, matcher, IIIF_IMAGE_REGEX_ROTATION_GROUP);
            url = replaceGroup(url, quality, matcher, IIIF_IMAGE_REGEX_QUALITY_GROUP);
            url = replaceGroup(url, format, matcher, IIIF_IMAGE_REGEX_FORMAT_GROUP);

        }
        return url;
    }

    /**
     * Replaces the image request parameters in an IIIF URL with the given ones
     * 
     * @param url
     * @param width
     * @param height
     * @return
     * @should replace dimensions correctly
     * @should do nothing if not iiif url
     */
    public String getModifiedIIIFFUrl(String url, RegionRequest region, Scale size, Rotation rotation, Colortype quality, ImageFileFormat format) {
        return getModifiedIIIFFUrl(url, region == null ? null : region.toString(), size == null ? null : size.toString(), rotation == null ? null : rotation.toString(), quality == null ? null : quality.toString(), format == null ? null : format.getFileExtension());
    }

    /**
     * Replaces a capturing group of a group already matched by the matcher with the String {@code replacement}
     * 
     * @param url
     * @param group
     * @return
     */
    private String replaceGroup(String text, String replacement, Matcher matcher, int group) {
        if(replacement != null) {            
            int start = matcher.start(group);
            int end = matcher.end(group);
            if (start > -1 && end > -1) {
                return text.substring(0, start) + replacement + text.substring(end);
            }
        }
        return text;
    }

    /**
     * 
     * @param url
     * @return true if the given url conforms to a IIIF image request pattern (that is, an actual image is requested, not just image information)
     */
    public static boolean isIIIFImageUrl(String url) {
        return url.matches(IIIF_IMAGE_REGEX);
    }

}
