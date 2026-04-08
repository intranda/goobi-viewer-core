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
package io.goobi.viewer.model.viewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.oa.Motivation;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.api.rest.filters.FilterTools;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileSizeCalculator;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.controller.model.ViewAttributes;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessDeniedInfoConfig;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IAccessDeniedThumbnailOutput;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.toc.TocMaker;
import io.goobi.viewer.model.viewer.StructElement.ShapeMetadata;
import io.goobi.viewer.model.viewer.record.views.FileType;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Physical element (page) containing an image, video or audio.
 */
public class PhysicalElement implements Comparable<PhysicalElement>, IAccessDeniedThumbnailOutput, Serializable {

    public enum CoordsFormat {
        UNCHECKED,
        NONE,
        ALTO
    }

    private static final long serialVersionUID = -6744820937107786721L;

    private static final Logger logger = LogManager.getLogger(PhysicalElement.class);

    /** Constant <code>WATERMARK_TEXT_TYPE_URN="URN"</code>. */
    public static final String WATERMARK_TEXT_TYPE_URN = "URN";
    /** Constant <code>WATERMARK_TEXT_TYPE_PURL="PURL"</code>. */
    public static final String WATERMARK_TEXT_TYPE_PURL = "PURL";
    /** Constant <code>WATERMARK_TEXT_TYPE_SOLR="SOLR:"</code>. */
    public static final String WATERMARK_TEXT_TYPE_SOLR = "SOLR:";
    /** Constant <code>defaultVideoWidth=320</code>. */
    private static final int DEFAULT_VIDEO_WIDTH = 320;
    /** Constant <code>defaultVideoHeight=240</code>. */
    private static final int DEFAULT_VIDEO_HEIGHT = 240;

    private List<String> watermarkTextConfiguration;

    private final transient Object lock = new Object();

    /** Persistent identifier. */
    private final String pi;
    /** Physical ID from the METS file. */
    private final String physId;
    private final String filePath;
    private String filePathTiff;
    private String filePathJpeg;
    private String fileName;
    private String fileIdRoot;
    private long fileSize = 0;
    /** Physical page number of this element in the list of all pages (this value is always 1 below the ORDER value in the METS file). */
    private final int order;
    /** Logical page number (label) of this element. */
    private final String orderLabel;
    /** URN granular. */
    private final String urn;
    private String purlPart;
    /** Media mime type. */
    private String mimeType = "image";
    /** Actual image/video width (if available). */
    private int width = 0;
    /** Actual image/video height (if available). */
    private int height = 0;
    /** Whether or not this page has image data. */
    private boolean hasImage = false;
    /** Whether or not this page contains an image that spans two pages. */
    private boolean doubleImage = false;
    /** If this page comes after an uneven number of double image pages, this should be set to true. */
    private boolean flipRectoVerso = false;
    /** Whether or not full-text is available for this page. */
    private boolean fulltextAvailable = false;

    private Boolean fulltextAccessPermission;
    /** Map containing AccessPermission access check results and custom access denied info. */
    private Map<String, AccessPermission> accessPermissionMap = new HashMap<>();
    /** True if a download ticket is required before files may be downloaded. Value is set during the access permission check. */
    private Boolean bornDigitalDownloadTicketRequired = null; // TODO reset when logging in/out or persist in session
    /** File name of the full-text document in the file system. */
    private String fulltextFileName;
    /** File name of the ALTO document in the file system. */
    private String altoFileName;
    /** Plain full-text. */
    private String fullText;
    /** XML document containing the ALTO document for this page. Saved into a variable so it doesn't have to be expensively loaded multiple times. */
    private String altoText;
    /** ALTO file charset (determined when loading). */
    private String altoCharset;
    /** Format of the loaded word coordinates XML document. */
    private CoordsFormat wordCoordsFormat = CoordsFormat.UNCHECKED;
    /** Data repository name for the record to which this page belongs. */
    private final String dataRepository;

    private Map<String, String> fileNames = new HashMap<>();
    private Set<String> accessConditions = new HashSet<>();
    /** List of <code>StructElement</code>s contained on this page. */
    private List<StructElement> containedStructElements;
    /** Content type of loaded fulltext. */
    private String textContentType = null;

    private final List<Metadata> metadata = new ArrayList<>();

    /**
     * Creates a new PhysicalElement instance.
     *
     * @param physId Physical element ID
     * @param filePath Path to the file
     * @param order Page number (numerical)
     * @param orderLabel Page number (label)
     * @param urn Page URN
     * @param purlPart persistent URL path segment for this page
     * @param pi Record identifier
     * @param mimeType Page mime type
     * @param dataRepository Record date repository
     */
    PhysicalElement(String physId, String filePath, int order, String orderLabel, String urn, String purlPart, String pi, String mimeType,
            String dataRepository) {
        this.physId = physId;
        this.filePath = filePath;
        this.order = order;
        this.orderLabel = orderLabel;
        this.urn = urn;
        this.purlPart = purlPart;
        this.pi = pi;

        if (StringUtils.isNotEmpty(mimeType)) {
            this.mimeType = mimeType;
        }

        this.dataRepository = dataRepository;
        this.fileName = determineFileName(filePath);

        if (watermarkTextConfiguration == null) {
            watermarkTextConfiguration = DataManager.getInstance().getConfiguration().isWatermarkTextConfigurationEnabled()
                    ? DataManager.getInstance().getConfiguration().getWatermarkTextConfiguration() : Collections.emptyList();
        }
    }

    /**
     * determineFileName.
     *
     * @param filePath full path or URL to extract the filename from
     * @should cut off everything but the file name for normal file paths
     * @should leave external urls intact
     * @return a {@link java.lang.String} object.
     */
    protected static String determineFileName(String filePath) {
        String ret = filePath;
        if (StringUtils.isNotBlank(ret) && !(ret.startsWith("http://") || ret.startsWith("https://"))) {
            File file = new File(ret);
            ret = file.getName();
        }

        return ret;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + order;
        result = prime * result + ((physId == null) ? 0 : physId.hashCode());
        result = prime * result + ((pi == null) ? 0 : pi.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PhysicalElement other = (PhysicalElement) obj;
        if (order != other.order) {
            return false;
        }
        if (physId == null) {
            if (other.physId != null) {
                return false;
            }
        } else if (!physId.equals(other.physId)) {
            return false;
        }
        if (pi == null) {
            if (other.pi != null) {
                return false;
            }
        } else if (!pi.equals(other.pi)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(PhysicalElement o) {
        if (o.getOrder() > getOrder()) {
            return -1;
        } else if (o.getOrder() < getOrder()) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * getUrl.
     *
     * @return the url to the media content of the page, for example the
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getUrl() throws IndexUnreachableException, ViewerConfigurationException {
        MimeType mediaType = new MimeType(this.mimeType);

        if (mediaType.isAllowsImageView()) {
            return getImageUrl();
        } else if (mediaType.isAudio() || mediaType.isVideo()) {
            String format = getFileNames().keySet().stream().findFirst().orElse("");
            return getMediaUrl(format);
        } else if (mediaType.isSandboxedHtml()) {
            return getSandboxedUrl();
        } else {
            logger.error("Page {} of record '{}' has unsupported mime-type: {}", orderLabel, pi, this.mimeType);
            return "";
        }
    }

    public Map<FileType, String> getFileTypes() {
        Collection<String> filenames = new HashSet<>(getFileNames().values());
        filenames.add(getFileName());
        return FileType.sortByFileType(filenames);
    }

    public String getFileForType(String type) {
        return getFileTypes().get(FileType.valueOf(type.toUpperCase()));
    }

    /**
     * getSandboxedUrl.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSandboxedUrl() {
        logger.trace(fileNames);
        if (fileNames.get("html-sandboxed") != null) {
            return fileNames.get("html-sandboxed");
        }
        return filePath;
    }

    /**
     * getWatermarkText.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getWatermarkText() {
        if (watermarkTextConfiguration == null || watermarkTextConfiguration.isEmpty()) {
            return "";
        }

        StringBuilder urlBuilder = new StringBuilder();
        for (String text : watermarkTextConfiguration) {
            if (Strings.CI.startsWith(text, WATERMARK_TEXT_TYPE_SOLR)) {
                String field = text.substring(WATERMARK_TEXT_TYPE_SOLR.length());
                try {
                    SolrDocumentList res = DataManager.getInstance()
                            .getSearchIndex()
                            .search(new StringBuilder(SolrConstants.PI).append(":").append(pi).toString(), SolrSearchIndex.MAX_HITS, null,
                                    Collections.singletonList(field));
                    if (res != null && !res.isEmpty() && res.get(0).getFirstValue(field) != null) {
                        // logger.debug("{}:{}", field, res.get(0).getFirstValue(field)); //NOSONAR Debug
                        urlBuilder.append((String) res.get(0).getFirstValue(field));
                        break;
                    }
                } catch (PresentationException e) {
                    logger.debug("PresentationException thrown here: {}", e.getMessage());
                } catch (IndexUnreachableException e) {
                    logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());

                }
            } else if (Strings.CI.equals(text, WATERMARK_TEXT_TYPE_URN)) {
                if (StringUtils.isNotEmpty(urn)) {
                    urlBuilder.append(urn);
                    break;
                }
            } else if (Strings.CI.equals(text, WATERMARK_TEXT_TYPE_PURL)) {
                urlBuilder.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                        .append("/")
                        .append(PageType.viewImage.getName())
                        .append("/")
                        .append(pi)
                        .append("/")
                        .append(order)
                        .append("/");
                break;
            } else {
                urlBuilder.append(text);
                break;
            }
        }
        return urlBuilder.toString();
    }

    /**
     * getThumbnailUrl.
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getThumbnailUrl() throws ViewerConfigurationException {
        int thumbWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
        int thumbHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();

        return getThumbnailUrl(thumbWidth, thumbHeight);
    }

    /**
     * getThumbnailUrl.
     *
     * @param width desired thumbnail width in pixels
     * @param height desired thumbnail height in pixels
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(int width, int height) {
        ThumbnailHandler thumbHandler = BeanUtils.getImageDeliveryBean().getThumbs();
        return thumbHandler.getThumbnailUrl(this, width, height);
    }

    public String getAccessDeniedDescriptionTextForImage(Locale locale) throws IndexUnreachableException, DAOException {
        logger.trace("getAccessDeniedDescriptionTextForImage: locale: {}, page: {}", locale, order);
        return getAccessDeniedDescriptionText(IPrivilegeHolder.PRIV_VIEW_IMAGES, locale);
    }

    public String getAccessDeniedDescriptionTextForVideo(Locale locale) throws IndexUnreachableException, DAOException {
        logger.trace("getAccessDeniedDescriptionTextForAudio: locale: {}, page: {}", locale, order);
        return getAccessDeniedDescriptionText(IPrivilegeHolder.PRIV_VIEW_VIDEO, locale);
    }

    public String getAccessDeniedDescriptionTextForAudio(Locale locale) throws IndexUnreachableException, DAOException {
        logger.trace("getAccessDeniedDescriptionTextForAudio: locale: {}, page: {}", locale, order);
        return getAccessDeniedDescriptionText(IPrivilegeHolder.PRIV_VIEW_AUDIO, locale);
    }

    public String getAccessDeniedDescriptionTextFor3D(Locale locale) throws IndexUnreachableException, DAOException {
        logger.trace("getAccessDeniedDescriptionTextFor3D: locale: {}, page: {}", locale, order);
        return getAccessDeniedDescriptionTextForImage(locale);
    }

    /**
     *
     * @param privilegeName access privilege name to look up
     * @param locale locale for the description text
     * @return Description text if found; otherwise null
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    private String getAccessDeniedDescriptionText(String privilegeName, Locale locale) throws IndexUnreachableException, DAOException {
        AccessPermission accessPermission = getAccessPermission(privilegeName);
        if (accessPermission != null && accessPermission.getAccessDeniedPlaceholderInfo() != null) {
            AccessDeniedInfoConfig placeholderInfo = accessPermission.getAccessDeniedPlaceholderInfo().get(locale.getLanguage());
            if (placeholderInfo != null && StringUtils.isNotEmpty(placeholderInfo.getDescription())) {
                logger.trace("returning custom description text for {}: {}", privilegeName, placeholderInfo.getDescription());
                return StringTools.stripJS(placeholderInfo.getDescription());
            }
        }

        return null;
    }

    public String getAccessDeniedImageUrl(Locale locale) throws IndexUnreachableException, DAOException {
        logger.trace("getAccessDeniedImageUrl: locale: {}, page: {}", locale, order);
        return getAccessDeniedUrl(getAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES), locale);
    }

    @Override
    public String getAccessDeniedThumbnailUrl(Locale locale) throws IndexUnreachableException, DAOException {
        // logger.trace("getAccessDeniedThumbnailUrl: locale: {}, page: {}", locale, order); //NOSONAR Debug
        return getAccessDeniedUrl(getAccessPermission(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS), locale);
    }

    /**
     *
     * @param accessPermission Access permission holding placeholder info
     * @param locale Locale for selecting the image URL
     * @return Access denied image url; null if none found
     */
    static String getAccessDeniedUrl(AccessPermission accessPermission, Locale locale) {
        if (accessPermission != null && accessPermission.getAccessDeniedPlaceholderInfo() != null) {
            AccessDeniedInfoConfig placeholderInfo = accessPermission.getAccessDeniedPlaceholderInfo().get(locale.getLanguage());
            if (placeholderInfo != null && StringUtils.isNotEmpty(placeholderInfo.getImageUri())) {
                logger.trace("returning custom image: {}", placeholderInfo.getImageUri());
                return placeholderInfo.getImageUri();
            }
        }

        return null;
    }

    /**
     * @param privilegeName Access privilege name to check

     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public AccessPermission getAccessPermission(String privilegeName) throws IndexUnreachableException, DAOException {
        return getAccessPermission(privilegeName, null);
    }

    /**
     * @param privilegeName Access privilege name to check
     * @param user The User requesting access. If null, it is fetched from the jsfContext if one exists

     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public AccessPermission getAccessPermission(String privilegeName, User user) throws IndexUnreachableException, DAOException {
        if (accessPermissionMap.get(privilegeName) == null) {
            AccessPermission accessPermission = AccessConditionUtils.getAccessPermission(pi, fileName, privilegeName, user);
            if (accessPermission != null) {
                accessPermissionMap.put(privilegeName, accessPermission);
            }
        }

        return accessPermissionMap.getOrDefault(privilegeName, AccessPermission.denied());
    }

    /**
     * getId.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getId() {
        logger.debug("getPhysId");
        return physId;
    }

    /**
     * 
     * @return filePath if mime type is image; alternative file path otherwise
     * @should return filePath if mime type image
     * @should return tiff if available
     * @should return jpeg if availabel
     */
    public String getImageFilepath() {

        if (!getMediaType().isAllowsImageView()) {
            if (filePathTiff != null) {
                return filePathTiff;
            }
            if (filePathJpeg != null) {
                return filePathJpeg;
            }
        }

        return getFilepath();
    }

    public MimeType getMediaType() {
        return new MimeType(this.mimeType);
    }

    /**
     * getFilepath.
     *
     * @return {@link java.lang.String} Path zu Image Datei.
     */
    public String getFilepath() {
        return filePath;
    }

    
    public String getFilePathTiff() {
        return filePathTiff;
    }

    /**
     * @param filePathTiff the TIFF file path for this page
     * @return this
     */
    public PhysicalElement setFilePathTiff(String filePathTiff) {
        this.filePathTiff = filePathTiff;
        return this;
    }

    
    public String getFilePathJpeg() {
        return filePathJpeg;
    }

    /**
     * @param filePathJpeg the JPEG file path for this page
     * @return this
     */
    public PhysicalElement setFilePathJpeg(String filePathJpeg) {
        this.filePathJpeg = filePathJpeg;
        return this;
    }

    /**
     * Getter for the field <code>order</code>.
     *
     * @return a int.
     */
    public int getOrder() {
        return order;
    }

    /**
     * Getter for the field <code>orderLabel</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getOrderLabel() {
        return orderLabel;
    }

    /**
     * Getter for the field <code>urn</code>.
     *

     */
    public String getUrn() {
        return urn;
    }

    /**
     * Setter for the field <code>purlPart</code>.
     *
     * @param purlPart the persistent URL fragment for this page
     */
    public void setPurlPart(String purlPart) {
        this.purlPart = purlPart;
    }

    /**
     * Getter for the field <code>purlPart</code>.
     *

     */
    public String getPurlPart() {
        return purlPart;
    }

    /**
     * For images, this returns the full mime-type as image/X, with X being the format which should be used for image display. This is png for
     * png-images and jpeg for all other types.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayMimeType() {
        String fullMimetype = getFullMimeType(getMimeType(), fileName);
        if (fullMimetype.matches("(?i)image/png")) {
            return fullMimetype;
        }
        return "image/jpeg";
    }

    /**
     * getFullMimeType.
     *
     * @param mimeType partial or full MIME type string
     * @param fileName file name used to determine image format
     * @return a {@link java.lang.String} object.
     * @should return mimeType if already full mime type
     * @should return mimeType if not image
     * @should return png image mime type from file name
     * @should return jpeg if not png
     */
    public static String getFullMimeType(String mimeType, String fileName) {
        if (mimeType == null) {
            return "";
        }

        // Already full mime type
        if (mimeType.contains("/")) {
            return mimeType;
        }

        MimeType mediaType = new MimeType(mimeType);

        if (mediaType.isImage()) {
            ImageFileFormat fileFormat = ImageFileFormat.getImageFileFormatFromFileExtension(fileName);
            if (ImageFileFormat.PNG.equals(fileFormat)) {
                return fileFormat.getMimeType();
            }
            return ImageFileFormat.JPG.getMimeType();
        }

        return mimeType;
    }

    /**
     * Getter for the field <code>mimeType</code>.
     *

     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Setter for the field <code>mimeType</code>.
     *
     * @param mimeType the MIME type of the primary media file for this page
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Setter for the field <code>width</code>.
     *
     * @param width the image width in pixels
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Setter for the field <code>height</code>.
     *
     * @param height the image height in pixels
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Getter for the field <code>fileIdRoot</code>.
     *

     */
    public String getFileIdRoot() {
        return fileIdRoot;
    }

    /**
     * Setter for the field <code>fileIdRoot</code>.
     *
     * @param fileIdRoot the root identifier used to build file IDs for this page
     */
    public void setFileIdRoot(String fileIdRoot) {
        this.fileIdRoot = fileIdRoot;
    }

    /**
     *
     * @return true if page has an indexed image file name and user has access permission; false otherwise
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public boolean isDisplayImage() throws IndexUnreachableException, DAOException {
        if (!hasImage) {
            return false;
        }
        String filename = getFileName();
        if (StringUtils.isBlank(filename)) {
            return false;
        }

        HttpServletRequest request = BeanUtils.getRequest();
        return AccessConditionUtils
                .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, getPi(), filename,
                        IPrivilegeHolder.PRIV_VIEW_IMAGES, NetTools.getIpAddress(request))
                .isGranted();
    }

    
    public boolean isHasImage() {
        return hasImage;
    }

    
    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    
    public boolean isDoubleImage() {
        // logger.trace("isDoubleImage: {}", doubleImage); //NOSONAR Debug
        return doubleImage;
    }

    
    public void setDoubleImage(boolean doubleImage) {
        this.doubleImage = doubleImage;
    }

    
    public boolean isFlipRectoVerso() {
        return flipRectoVerso;
    }

    
    public void setFlipRectoVerso(boolean flipRectoVerso) {
        this.flipRectoVerso = flipRectoVerso;
    }

    /**
     * isFulltextAvailableForPage.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isDisplayFulltext() throws IndexUnreachableException, DAOException {
        if (!fulltextAvailable) {
            return false;
        }
        String filename = null;
        try {
            filename = FileTools.getFilenameFromPathString(getFulltextFileName());
        } catch (FileNotFoundException e) {
            //
        }
        if (StringUtils.isBlank(filename)) {
            try {
                filename = FileTools.getFilenameFromPathString(getAltoFileName());
            } catch (FileNotFoundException e) {
                //
            }
        }
        if (StringUtils.isBlank(filename)) {
            return false;
        }

        HttpServletRequest request = BeanUtils.getRequest();
        return AccessConditionUtils
                .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, getPi(), filename,
                        IPrivilegeHolder.PRIV_VIEW_FULLTEXT, NetTools.getIpAddress(request))
                .isGranted();
    }

    /**
     * isFulltextAvailable.
     *

     */
    public boolean isFulltextAvailable() {
        return fulltextAvailable;
    }

    /**

     * @throws ViewerConfigurationException
     * @should return true if access allowed for this page
     * @should return false if access denied for this page
     */
    public Boolean isFulltextAccessPermission() {
        if (fulltextAccessPermission == null) {
            fulltextAccessPermission = false;
            try {
                fulltextAccessPermission = AccessConditionUtils
                        .checkAccessPermissionByIdentifierAndPageOrder(this, IPrivilegeHolder.PRIV_VIEW_FULLTEXT, BeanUtils.getRequest())
                        .isGranted();
            } catch (IndexUnreachableException | DAOException e) {
                logger.error(String.format("Cannot check fulltext access for pi %s and pageNo %d: %s", pi, order, e.toString()));
            }
        }

        return fulltextAccessPermission;
    }

    /**
     * Setter for the field <code>fulltextAvailable</code>.
     *
     * @param fulltextAvailable true if fulltext content is available for this page
     */
    public void setFulltextAvailable(boolean fulltextAvailable) {
        this.fulltextAvailable = fulltextAvailable;
    }

    /**
     * isAltoAvailableForPage.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAltoAvailable() {
        String filename = null;
        try {
            filename = FileTools.getFilenameFromPathString(getAltoFileName());
            if (StringUtils.isBlank(filename)) {
                return false;
            }

            HttpServletRequest request = BeanUtils.getRequest();
            return AccessConditionUtils
                    .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, getPi(), filename,
                            IPrivilegeHolder.PRIV_VIEW_FULLTEXT, NetTools.getIpAddress(request))
                    .isGranted();
        } catch (FileNotFoundException | IndexUnreachableException | DAOException e) {
            return false;
        }
    }

    /**
     * isTeiAvailableForPage.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isTeiAvailable() throws IndexUnreachableException, DAOException {
        return isDisplayFulltext();
    }

    /**
     * Getter for the field <code>fulltextFileName</code>.
     *

     */
    public String getFulltextFileName() {
        return fulltextFileName;
    }

    /**
     * Setter for the field <code>fulltextFileName</code>.
     *
     * @param fulltextFileName the file name of the plain-text fulltext file for this page
     */
    public void setFulltextFileName(String fulltextFileName) {
        this.fulltextFileName = fulltextFileName;
    }

    /**
     * Getter for the field <code>altoFileName</code>.
     *

     */
    public String getAltoFileName() {
        return altoFileName;
    }

    /**
     * Setter for the field <code>altoFileName</code>.
     *
     * @param altoFileName the file name of the ALTO XML file for this page
     */
    public void setAltoFileName(String altoFileName) {
        this.altoFileName = altoFileName;
    }

    /**
     * Getter for the field <code>fullText</code>.
     *

     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getFullText() throws ViewerConfigurationException {
        if (altoText == null && wordCoordsFormat == CoordsFormat.UNCHECKED) {
            // Load XML document
            try {
                StringPair alto = loadAlto();
                if (StringUtils.isNotEmpty(alto.getOne())) {
                    altoText = alto.getOne();
                }
                altoCharset = alto.getTwo();
                fulltextAccessPermission = true;
            } catch (AccessDeniedException e) {
                fulltextAccessPermission = false;
                fullText = ViewerResourceBundle.getTranslation(e.getMessage(), null);
            } catch (JDOMException | IOException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (StringUtils.isNotEmpty(altoText)) {
            wordCoordsFormat = CoordsFormat.ALTO;
            String text = ALTOTools.getFulltext(altoText, altoCharset, false);
            if (StringUtils.isNotEmpty(text)) {
                String cleanText = StringTools.stripJS(text);
                if (cleanText.length() < text.length()) {
                    text = cleanText;
                    logger.warn("JavaScript found and removed from full-text in {}, page {}", pi, getOrder());
                }
            }
            return text;
        }
        wordCoordsFormat = CoordsFormat.NONE;
        if (fullText == null) {
            try {
                fullText = loadFullText();
                if (StringUtils.isNotEmpty(fullText)) {
                    String cleanText = StringTools.stripJS(fullText);
                    if (cleanText.length() < fullText.length()) {
                        fullText = cleanText;
                        logger.warn("JavaScript found and removed from full-text in {}, page {}", pi, getOrder());
                    }
                }
                fulltextAccessPermission = true;
            } catch (AccessDeniedException e) {
                fulltextAccessPermission = false;
                fullText = ViewerResourceBundle.getTranslation(e.getMessage(), null);
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            } catch (IOException | IndexUnreachableException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return fullText;
    }

    /**
     *
     * @return The probable mimeType of the fulltext. If the fulltext is not yet loaded, it is loaded first
     * @throws ViewerConfigurationException
     */
    public String getFulltextMimeType() throws ViewerConfigurationException {
        if (textContentType == null) {
            getFullText();
        }
        return textContentType;
    }

    /**
     * Setter for the field <code>fullText</code>.
     *
     * @param fullText the plain-text fulltext content for this page
     */
    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    /**
     * Loads full-text data for this page via the REST service, if not yet loaded.
     *
     * @return true if fulltext loaded successfully false otherwise
     * @throws AccessDeniedException
     * @throws IOException
     * @throws IndexUnreachableException
     * @should load full-text correctly if not yet loaded
     * @should return null if already loaded
     */
    String loadFullText() throws AccessDeniedException, IOException, IndexUnreachableException {
        if (Boolean.FALSE.equals(isFulltextAccessPermission())) {
            throw new AccessDeniedException(String.format("Fulltext access denied for pi %s and pageNo %d", pi, order));
        }
        if (fulltextFileName == null) {
            return null;
        }

        logger.trace("Loading full-text for page {}", fulltextFileName);
        try {
            return DataFileTools.loadFulltext(null, fulltextFileName, false);
        } catch (FileNotFoundException e) {
            // Include PI and fulltext filename to help diagnose missing fulltext files
            logger.error("{} (pi={}, fulltextFileName={})", e.getMessage(), pi, fulltextFileName);
            return "";
        }
    }

    /**
     * getWordCoords.
     *
     * @param searchTerms terms whose word coordinates to retrieve
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getWordCoords(Set<String> searchTerms) throws ViewerConfigurationException {
        return getWordCoords(searchTerms, 0, 0);
    }

    /**
     * Returns word coordinates for words that start with any of the given search terms.
     *
     * @param searchTerms terms whose word coordinates to retrieve
     * @param proximitySearchDistance Maximum word distance for proximity search
     * @param rotation image rotation in degrees
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @should load XML document if none yet set
     */
    public List<String> getWordCoords(Set<String> searchTerms, int proximitySearchDistance, int rotation) throws ViewerConfigurationException {
        if (searchTerms == null || searchTerms.isEmpty()) {
            return Collections.emptyList();
        }
        logger.trace("loadWordCoords: {}", searchTerms);

        if (altoText == null && wordCoordsFormat == CoordsFormat.UNCHECKED) {
            // Load XML document
            try {
                loadAlto();
                fulltextAccessPermission = true;
            } catch (AccessDeniedException e) {
                fulltextAccessPermission = false;
                fullText = ViewerResourceBundle.getTranslation(e.getMessage(), null);
            } catch (JDOMException | IOException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (altoText != null) {
            return ALTOTools.getWordCoords(altoText, altoCharset, searchTerms, proximitySearchDistance, rotation);
        }
        wordCoordsFormat = CoordsFormat.NONE;

        return Collections.emptyList();
    }

    /**
     * Loads ALTO data for this page via the REST service, if not yet loaded.
     *
     * @return StringPair(ALTO,charset)
     * @should load and set alto correctly
     * @should set wordCoordsFormat correctly
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     * @throws org.jdom2.JDOMException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public StringPair loadAlto()
            throws AccessDeniedException, JDOMException, IOException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("loadAlto: {}", altoFileName);
        if (Boolean.FALSE.equals(isFulltextAccessPermission())) {
            throw new AccessDeniedException(String.format("Fulltext access denied for pi %s and pageNo %d", pi, order));
        }
        if (altoFileName == null) {
            return new StringPair("", null);
        }

        try {
            StringPair alto = DataFileTools.loadAlto(altoFileName);
            //Text from alto is always plain text
            textContentType = StringConstants.MIMETYPE_TEXT_PLAIN;
            if (alto != null) {
                altoText = alto.getOne();
                altoCharset = alto.getTwo();
                wordCoordsFormat = CoordsFormat.ALTO;
            }
            return alto;
        } catch (FileNotFoundException | PresentationException e) {
            // Include PI and ALTO filename to help diagnose missing ALTO files
            logger.error("{} (pi={}, altoFileName={})", e.getMessage(), pi, altoFileName);
        }

        return new StringPair("", null);
    }

    /**
     * Getter for the field <code>fileNames</code>.
     *

     */
    public Map<String, String> getFileNames() {
        return fileNames;
    }

    /**
     * Setter for the field <code>fileNames</code>.
     *
     * @param fileNames map of media format keys to file name values for this page
     */
    public void setFileNames(Map<String, String> fileNames) {
        this.fileNames = fileNames;
    }

    /**
     * Returns The first matching media filename for this page.
     *
     * @return The first matching media filename for this page
     */
    public String getFirstFileName() {
        String format = getFileNames().keySet().stream().findFirst().orElse("");
        return getFileNameForFormat(format);
    }

    /**
     * Returns the fileName alone, if {@link io.goobi.viewer.model.viewer.PhysicalElement#getFilepath()} is a local file, or the entire filePath
     * otherwise.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFileName() {
        if (StringUtils.isEmpty(fileName)) {
            determineFileName(filePath);
        }
        return fileName;
    }

    /**
     * getFileNameBase.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFileNameBase() {
        return FilenameUtils.getBaseName(fileName);
    }

    /**
     * getFileNameExtension.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFileNameExtension() {
        return FilenameUtils.getExtension(fileName);
    }

    /**
     * getFileNameForFormat.
     *
     * @param format media format key to look up (e.g. "ogg", "mp4")
     * @return a {@link java.lang.String} object.
     */
    public String getFileNameForFormat(String format) {
        if (fileNames.get(format) != null) {
            return fileNames.get(format);
        }

        return fileName;
    }

    /**
     * getImageToPdfUrl.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getImageToPdfUrl() throws IndexUnreachableException {
        return BeanUtils.getImageDeliveryBean().getPdf().getPdfUrl(BeanUtils.getActiveDocumentBean().getCurrentElement(), this);
    }

    /**
     * Returns a "RESTful" URL for a media (audio or video) file in the given format.
     *
     * @param format media format key (e.g. "ogg", "mp4", "mp3")
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getMediaUrl(String format) throws IndexUnreachableException {

        String url;
        try {
            url = BeanUtils.getImageDeliveryBean().getMedia().getMediaUrl(getMediaType().getType(), format, pi, getFileNameForFormat(format));
        } catch (IllegalRequestException e) {
            throw new IllegalStateException("media type must be either audio or video, but is " + getMediaType().getType());
        }

        logger.trace("currentMediaUrl: {}", url);
        return url;
    }

    /**
     * Returns a list of media formats available for this element.
     * 
     * @param type audio or video
     * @return List of supported formats for the given type
     */
    public List<String> getMediaFormats(String type) {
        logger.trace("getMediaFormats: {}", type);
        if (type == null) {
            return Collections.emptyList();
        }

        List<String> ret = new ArrayList<>();
        switch (type.toLowerCase()) {
            case "audio":
                if (fileNames.get("ogg") != null) {
                    ret.add("ogg");
                }
                if (fileNames.get("mpeg") != null || fileNames.get("mp3") != null) {
                    ret.add("mpeg");
                    ret.add("mp3");
                }
                break;
            case "video":
                if (fileNames.get("webm") != null) {
                    ret.add("webm");
                }
                if (fileNames.get("mp4") != null) {
                    ret.add("mp4");
                }
                if (fileNames.get("ogg") != null) {
                    ret.add("ogg");
                }
                break;
            default:
                logger.warn("Unsupported type: {}", type);
        }

        return ret;
    }

    /**
     * getVideoWidth.
     *
     * @return a int.
     */
    public int getVideoWidth() {
        if (width > 0) {
            return width;
        }

        return DEFAULT_VIDEO_WIDTH;
    }

    /**
     * getVideoHeight.
     *
     * @return a int.
     */
    public int getVideoHeight() {
        if (height > 0) {
            return height;
        }

        return DEFAULT_VIDEO_HEIGHT;
    }

    /**
     * Returns the actual image width, if available. Otherwise the default width.
     *
     * @return a int.
     */
    public int getImageWidth() {
        return width;
    }

    /**
     * Returns the actual image height, if available. Otherwise the default height.
     *
     * @return a int.
     */
    public int getImageHeight() {
        return height;
    }

    /**
     * getPhysicalImageHeight.
     *
     * @return a int.
     */
    public int getPhysicalImageHeight() {
        return getImageHeight();
    }

    /**
     * Returns the zoom factor for this image depending on its actual size.
     *
     * @return a int.
     */
    public int getImageZoomFactor() {
        return getImageWidth() / 100;
    }

    /**
     * getImageUrl.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getImageUrl() {
        return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(this, Scale.MAX);
    }

    /**
     * getImageUrl.
     *
     * @param size desired image width in pixels
     * @return a {@link java.lang.String} object.
     */
    public String getImageUrl(int size) {
        Scale scale = new Scale.ScaleToWidth(size);
        return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(this, scale);
    }

    /**
     * Returns the bare width as read from the index (0 if none available).
     *
     * @return a int.
     */
    public int getMixWidth() {
        return getImageWidth();
    }

    /**
     * getPhysicalImageWidth.
     *
     * @return a int.
     */
    public int getPhysicalImageWidth() {
        return getImageWidth();
    }

    /**
     * Getter for the field <code>pi</code>.
     *

     */
    public String getPi() {
        return pi;
    }

    /**
     * Getter for the field <code>accessConditions</code>.
     *

     */
    public Set<String> getAccessConditions() {
        return accessConditions;
    }

    /**
     * Setter for the field <code>accessConditions</code>.
     *
     * @param accessConditions set of access condition identifiers restricting this page
     */
    public void setAccessConditions(Set<String> accessConditions) {
        this.accessConditions = accessConditions;
    }

    /**
     * getPageLinkLabel.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPageLinkLabel() {
        MimeType type = getMediaType();

        if (type.isAllowsImageView()) {
            return "viewImage";
        } else if (type.isVideo()) {
            return "viewVideo";
        } else if (type.isAudio()) {
            return "viewAudio";
        } else if (type.isSandboxedHtml()) {
            return "viewSandboxedHtml";
        } else {
            return "viewImage";
        }

    }

    /**
     * Checks if the media type is displayable as a 3d object and access is granted for viewing it.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermission3DObject() throws IndexUnreachableException, DAOException {
        logger.trace("AccessPermission3DObject");
        // Prevent access if mime type incompatible
        if (!getMediaType().is3DModel()) {
            return false;
        }

        if (getFilepath().startsWith("http")) {
            //External urls are always free to use
            return true;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            return AccessConditionUtils
                    .checkAccessPermissionForImage(request != null ? request.getSession() : null, pi, fileName, NetTools.getIpAddress(request))
                    .isGranted();
        } else {
            logger.trace("FacesContext not found");
        }

        return false;
    }

    /**
     * Checks if the media type is displayable as an image and access is granted for viewing an image.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermissionImage() throws IndexUnreachableException, DAOException {
        return isAccessPermissionObject();
    }

    /**
     * Remnant from when image access had to be checked for each tile. Still used for OpenSeaDragon, so it just redirects to the access permission
     * check.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermissionObject() throws IndexUnreachableException, DAOException {
        // Prevent access if mime type incompatible
        if (!getMediaType().isAllowsImageView()) {
            return false;
        }

        HttpServletRequest request = null;
        if (getFilepath().startsWith("http")) {
            //External urls are always free to use
            return true;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        }

        return getAccessPermission(IPrivilegeHolder.PRIV_VIEW_IMAGES).isGranted() && FilterTools.checkForConcurrentViewLimit(pi, request);
    }

    /**
     * Checks if the user has the privilege {@link io.goobi.viewer.model.security.IPrivilegeHolder#PRIV_ZOOM_IMAGES} If the check fails and
     * {@link Configuration#getUnzoomedImageAccessMaxWidth()} is greater than 0, false is returned.
     *
     * @return true exactly if the user is allowed to zoom images. false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isAccessPermissionImageZoom() throws IndexUnreachableException, DAOException {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            return AccessConditionUtils
                    .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, pi, fileName,
                            IPrivilegeHolder.PRIV_ZOOM_IMAGES, NetTools.getIpAddress(request))
                    .isGranted();
        }
        logger.trace("FacesContext not found");
        return false;

    }

    /**
     *
     * @return true if user has access permission; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isAccessPermissionImageDownload() throws IndexUnreachableException, DAOException {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            return AccessConditionUtils
                    .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, pi, fileName,
                            IPrivilegeHolder.PRIV_DOWNLOAD_IMAGES, NetTools.getIpAddress(request))
                    .isGranted();
        }
        logger.trace("FacesContext not found");

        return false;
    }

    /**
     * isAccessPermissionPdf.
     *
     * @return true if PDF download is allowed for this page; false otherwise
     */
    public boolean isAccessPermissionPdf() {
        if (!DataManager.getInstance().getConfiguration().isPagePdfEnabled()) {
            return false;
        }
        // Prevent access if mime type incompatible
        if (!getMediaType().isAllowsImageView()) {
            return false;
        }

        HttpServletRequest request = null;
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        }
        try {
            return AccessConditionUtils.checkAccessPermissionForPagePdf(request, this).isGranted();
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return false;
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
            return false;
        }
    }

    /**
     * isAccessPermissionBornDigital.
     *
     * @return true if access is allowed for born digital files; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermissionBornDigital() throws IndexUnreachableException, DAOException {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            AccessPermission access =
                    AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, pi,
                            fileName,
                            IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES, NetTools.getIpAddress(request));
            // logger.trace("Born digital access for page {} is granted: {}", order, access.isGranted()); //NOSONAR Debug
            bornDigitalDownloadTicketRequired = access.isDownloadTicketRequired();
            // logger.trace("Ticket required for page {}: {}", order, access.isTicketRequired()); //NOSONAR Debug
            return access.isGranted();
        }
        logger.trace("FacesContext not found");

        bornDigitalDownloadTicketRequired = false; // maybe set to true?
        return false;
    }

    /**
     * 
     * @return true if a download ticket requirement is present and not yet satisfied; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isBornDigitalDownloadTicketRequired() throws IndexUnreachableException, DAOException {
        isAccessPermissionBornDigital();
        // logger.trace("isBornDigitalDownloadTicketRequired: {}", bornDigitalDownloadTicketRequired); //NOSONAR Debug

        // If license requires a download ticket, check agent session for loaded ticket
        if (Boolean.TRUE.equals(bornDigitalDownloadTicketRequired) && FacesContext.getCurrentInstance() != null
                && FacesContext.getCurrentInstance().getExternalContext() != null) {
            boolean hasTicket = AccessConditionUtils.isHasDownloadTicket(pi, BeanUtils.getSession());
            // logger.trace("User has download ticket: {}", hasTicket); //NOSONAR Debug
            return !hasTicket;
        }

        return bornDigitalDownloadTicketRequired;
    }

    /**
     *
     * @return true if user has access permission; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isAccessPermissionFulltext() throws IndexUnreachableException, DAOException {
        HttpServletRequest request = null;
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        } else {
            logger.trace("FacesContext not found");
        }

        return AccessConditionUtils
                .checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request != null ? request.getSession() : null, pi, fileName,
                        IPrivilegeHolder.PRIV_VIEW_FULLTEXT, NetTools.getIpAddress(request))
                .isGranted();
    }

    /**
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermissionVideo() throws IndexUnreachableException, DAOException {
        HttpServletRequest request = null;
        if (getFilepath().startsWith("http")) {
            //External urls are always free to use
            return true;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        }

        return getAccessPermission(IPrivilegeHolder.PRIV_VIEW_VIDEO).isGranted() && FilterTools.checkForConcurrentViewLimit(pi, request);
    }

    /**
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermissionAudio() throws IndexUnreachableException, DAOException {
        HttpServletRequest request = null;
        if (getFilepath().startsWith("http")) {
            //External urls are always free to use
            return true;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        }

        return getAccessPermission(IPrivilegeHolder.PRIV_VIEW_AUDIO).isGranted() && FilterTools.checkForConcurrentViewLimit(pi, request);
    }

    /**
     * getFooterHeight.
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight() throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .getFooterHeight(new ViewAttributes(this, PageType.getByName(PageType.viewImage.name())));
    }

    /**
     * getFooterHeight.
     *
     * @param pageType name of the page type for footer configuration lookup
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight(String pageType) throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(new ViewAttributes(this, PageType.getByName(pageType)));
    }

    /**
     * getComments.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CrowdsourcingAnnotation> getComments() throws DAOException {
        List<CrowdsourcingAnnotation> comments =
                DataManager.getInstance().getDao().getAnnotationsForTarget(this.pi, this.order, Motivation.COMMENTING);
        Collections.sort(comments, (c1, c2) -> c1.getDateCreated().compareTo(c2.getDateCreated()));
        return comments;
    }

    /**
     * deleteCommentAction.
     *
     * @param comment the comment to delete
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteCommentAction(Comment comment) throws DAOException {
        logger.trace("deleteCommentAction");
        if (DataManager.getInstance().getDao().deleteComment(comment)) {
            Messages.info("commentDeleteSuccess");
        } else {
            Messages.error("commentDeleteFailure");
        }
    }

    /**
     * Return true if this image has its own width/height measurements, and does not rely on default width/height.
     *
     * @return a boolean.
     */
    public boolean hasIndividualSize() {
        return (width > 0 && height > 0);
    }

    /**
     * Getter for the field <code>altoText</code>.
     *

     */
    public String getAltoText() {
        return altoText;
    }

    /**
     *
     * @param load If true, ALTO will be loaded if altoText is null
     * @return ALTO document for this page
     * @throws ViewerConfigurationException
     */
    public String getAltoText(boolean load) throws ViewerConfigurationException {
        if (altoText == null && fulltextAccessPermission == null && load) {
            try {
                loadAlto();
            } catch (AccessDeniedException e) {
                fulltextAccessPermission = false;
            } catch (JDOMException | IOException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return altoText;
    }

    
    public String getAltoCharset() {
        return altoCharset;
    }

    /**
     * Getter for the field <code>wordCoordsFormat</code>.
     *

     */
    public CoordsFormat getWordCoordsFormat() {
        return wordCoordsFormat;
    }

    /**
     * Getter for the field <code>dataRepository</code>.
     *

     */
    public String getDataRepository() {
        return dataRepository;
    }

    /**
     * Getter for the field <code>fileSize</code>.
     *

     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Setter for the field <code>fileSize</code>.
     *
     * @param fileSize the size of the primary media file in bytes
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * getFileSizeAsString.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFileSizeAsString() {
        return FileSizeCalculator.formatSize(this.fileSize);
    }

    /**
     * getImageType.
     *
     * @return a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     */
    public ImageType getImageType() {
        ImageType imageType = new ImageType(false);
        imageType.setFormat(ImageFileFormat.getImageFileFormatFromFileExtension(fileName));
        return imageType;
    }

    /**
     * Gets the filename but with its extension replaced by the given extension. If the extension is an empty String, the filename without any
     * extension is returned If the extension is null, {@link io.goobi.viewer.model.viewer.PhysicalElement#getFileName()} is returned.
     *
     * @param extension replacement extension, without leading dot; null returns original filename
     * @return a {@link java.lang.String} object.
     */
    public String getFileName(String extension) {
        if (extension == null) {
            return getFileName();
        }

        String baseName = FilenameUtils.removeExtension(getFileName());
        return baseName + (StringUtils.isNotBlank(extension) ? "." + extension : "");
    }

    /**
     * isDisplayPagePdfLink.
     *
     * @return true if page pdf link is allowed in configuration and no access conditions prevent PDF download; false otherwise
     */
    public boolean isDisplayPagePdfLink() {
        logger.trace("isDisplayPagePdfLink");
        return DataManager.getInstance().getConfiguration().isPagePdfEnabled() && isAccessPermissionPdf();
    }

    public List<Float> getImageHeightRationThresholds() {
        float lowerThreshold = DataManager.getInstance().getConfiguration().getLimitImageHeightLowerRatioThreshold();
        float upperThreshold = DataManager.getInstance().getConfiguration().getLimitImageHeightUpperRatioThreshold();
        return List.of(lowerThreshold, upperThreshold);
    }

    /**
     * Lists of struct elements that start on this page. For example, if a page contains multiple elements that only cover a certain area of the page
     * (using coordinates), this method can be used to get all shape coordinates for these elemets for visualization.
     *
     * @return List of <code>StructElement</code>s
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<StructElement> getContainedStructElements() throws PresentationException, IndexUnreachableException {
        if (containedStructElements == null) {
            String query = '+' + SolrConstants.PI_TOPSTRUCT + ':' + pi + " +" + SolrConstants.THUMBPAGENO + ':' + order;
            SolrDocumentList docstructDocs = DataManager.getInstance().getSearchIndex().search(query);
            if (docstructDocs.isEmpty()) {
                containedStructElements = Collections.emptyList();
            } else {
                containedStructElements = new ArrayList<>(docstructDocs.size());
                for (SolrDocument doc : docstructDocs) {
                    StructElement ele = new StructElement((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
                    IMetadataValue value = TocMaker.buildTocElementLabel(doc);
                    String label = value.getValue(BeanUtils.getLocale()).orElse(value.getValue().orElse(""));
                    if (StringUtils.isNotBlank(label)) {
                        ele.setLabel(label);
                    }
                    containedStructElements.add(ele);
                }
            }
        }

        return containedStructElements;
    }

    /**
     *
     * @return {@link String}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws JsonProcessingException
     */
    public String getContainedStructElementsAsJson() throws PresentationException, IndexUnreachableException, JsonProcessingException {
        synchronized (lock) {
            List<StructElement> elements = getContainedStructElements();

            ObjectMapper mapper = new ObjectMapper();
            List<ShapeMetadata> shapes = elements.stream()
                    .filter(ele -> ele.getShapeMetadata() != null && !ele.getShapeMetadata().isEmpty())
                    .flatMap(ele -> ele.getShapeMetadata().stream())
                    .toList();
            return mapper.writeValueAsString(shapes);
        }
    }

    
    public List<Metadata> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return String.format("%s %s (%s)", getPi(), getOrder(), getOrderLabel());
    }

    public String getFileMimeType() {

        return Optional.ofNullable(getFileName())
                .map(ImageFileFormat::getImageFileFormatFromFileExtension)
                .map(ImageFileFormat::getMimeType)
                .orElse("");
    }
}
