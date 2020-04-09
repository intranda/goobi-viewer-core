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
package io.goobi.viewer.model.viewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.imaging.IIIFUrlHandler;
import io.goobi.viewer.controller.imaging.PdfHandler;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ConfigurationBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.Comment;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.StructElement.ShapeMetadata;

/**
 * Physical element (page) containing an image, video or audio.
 */
public class PhysicalElement implements Comparable<PhysicalElement>, Serializable {

    public enum CoordsFormat {
        UNCHECKED,
        NONE,
        ALTO
    }

    private static final long serialVersionUID = -6744820937107786721L;

    private static final Logger logger = LoggerFactory.getLogger(PhysicalElement.class);

    /** Constant <code>WATERMARK_TEXT_TYPE_URN="URN"</code> */
    public static final String WATERMARK_TEXT_TYPE_URN = "URN";
    /** Constant <code>WATERMARK_TEXT_TYPE_PURL="PURL"</code> */
    public static final String WATERMARK_TEXT_TYPE_PURL = "PURL";
    /** Constant <code>WATERMARK_TEXT_TYPE_SOLR="SOLR:"</code> */
    public static final String WATERMARK_TEXT_TYPE_SOLR = "SOLR:";

    private static List<String> watermarkTextConfiguration;
    /** Constant <code>defaultVideoWidth=320</code> */
    public static int defaultVideoWidth = 320;
    /** Constant <code>defaultVideoHeight=240</code> */
    public static int defaultVideoHeight = 240;

    /** Persistent identifier. */
    private final String pi;
    /** Physical ID from the METS file. */
    private final String physId;
    private final String filePath;
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
    private String mimeType = MimeType.IMAGE.getName();
    /** Actual image/video width (if available). */
    private int width = 0;
    /** Actual image/video height (if available). */
    private int height = 0;
    /** Whether or not this page has image data. */
    private boolean hasImage = false;
    /** Whether or not full-text is available for this page. */
    private boolean fulltextAvailable = false;
    /** File name of the full-text document in the file system. */
    private String fulltextFileName;
    /** File name of the ALTO document in the file system. */
    private String altoFileName;
    /** Plain full-text. */
    private String fullText;
    /** XML document containing the ALTO document for this page. Saved into a variable so it doesn't have to be expensively loaded multiple times. */
    private String altoText;
    /** Format of the loaded word coordinates XML document. */
    private CoordsFormat wordCoordsFormat = CoordsFormat.UNCHECKED;
    /** Data repository name for the record to which this page belongs. */
    private final String dataRepository;

    private Map<String, String> fileNames = new HashMap<>();
    private Set<String> accessConditions = new HashSet<>();
    /** Image footer height. */
    private Integer imageFooterHeight;
    private Integer imageFooterHeightRotated;
    /** Comment currently being created/edited. */
    private Comment currentComment;
    /** Textual content of the previously created page comment. Workaround for duplicate posts via browser refresh. */
    private String previousCommentText;
    /** List of <code>StructElement</code>s contained on this page. */
    private List<StructElement> containedStructElements;

    /**
     * <p>
     * Constructor for PhysicalElement.
     * </p>
     *
     * @param physId Physical element ID
     * @param filePath Path to the file
     * @param order Page number (numerical)
     * @param orderLabel Page number (label)
     * @param urn Page URN
     * @param purlPart a {@link java.lang.String} object.
     * @param pi Record identifier
     * @param mimeType Page mime type
     * @param dataRepository Record date repository
     */
    public PhysicalElement(String physId, String filePath, int order, String orderLabel, String urn, String purlPart, String pi, String mimeType,
            String dataRepository) {

        super();
        this.physId = physId;
        this.filePath = filePath;
        this.order = order;
        this.orderLabel = orderLabel;
        this.urn = urn;
        this.purlPart = purlPart;
        this.pi = pi;
        this.currentComment = new Comment(this.pi, this.order, null, "", null);

        if (StringUtils.isNotEmpty(mimeType)) {
            this.mimeType = mimeType;
        }

        this.dataRepository = dataRepository;
        this.fileName = determineFileName(filePath);

        if (watermarkTextConfiguration == null) {
            watermarkTextConfiguration = DataManager.getInstance().getConfiguration().getWatermarkTextConfiguration();
        }

    }

    /**
     * <p>
     * determineFileName.
     * </p>
     *
     * @param filePath a {@link java.lang.String} object.
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

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
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

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
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
     * <p>
     * getUrl.
     * </p>
     *
     * @return the url to the media content of the page, for example the
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getUrl() throws IndexUnreachableException, ViewerConfigurationException {
        MimeType mimeType = MimeType.getByName(this.mimeType);
        if (mimeType == null) {
            logger.error("Page {} of record '{}' has unknown mime-type: {}", orderLabel, pi, this.mimeType);
            return "";
        }
        switch (mimeType) {
            case IMAGE:
                return getImageUrl();
            case VIDEO:
            case AUDIO: {

                String format = getFileNames().keySet().stream().findFirst().orElse("");
                return getMediaUrl(format);
            }
            case APPLICATION:
                if (StringUtils.isEmpty(fileName)) {
                    fileName = determineFileName(filePath);
                }
                String localFilename = fileName;

                PdfHandler pdfHandler = BeanUtils.getImageDeliveryBean().getPdf();
                return pdfHandler.getPdfUrl(pi, localFilename);
            case SANDBOXED_HTML:
                return getSandboxedUrl();
            default:
                logger.error("Page {} of record '{}' has unsupported mime-type: {}", orderLabel, pi, mimeType);
                return "";
        }
    }

    /**
     * <p>
     * getSandboxedUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSandboxedUrl() {
        logger.trace(fileNames.toString());
        if (fileNames.get("html-sandboxed") != null) {
            return fileNames.get("html-sandboxed");
        }
        return filePath;
    }

    /**
     * <p>
     * getWatermarkText.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getWatermarkText() {
        if (watermarkTextConfiguration == null || watermarkTextConfiguration.isEmpty()) {
            return "";
        }

        StringBuilder urlBuilder = new StringBuilder();
        for (String text : watermarkTextConfiguration) {
            if (StringUtils.startsWithIgnoreCase(text, WATERMARK_TEXT_TYPE_SOLR)) {
                String field = text.substring(WATERMARK_TEXT_TYPE_SOLR.length());
                try {
                    SolrDocumentList res = DataManager.getInstance()
                            .getSearchIndex()
                            .search(new StringBuilder(SolrConstants.PI).append(":").append(pi).toString(), SolrSearchIndex.MAX_HITS, null,
                                    Collections.singletonList(field));
                    if (res != null && !res.isEmpty() && res.get(0).getFirstValue(field) != null) {
                        // logger.debug(field + ":" + res.get(0).getFirstValue(field));
                        urlBuilder.append((String) res.get(0).getFirstValue(field));
                        break;
                    }
                } catch (PresentationException e) {
                    logger.debug("PresentationException thrown here: " + e.getMessage());
                } catch (IndexUnreachableException e) {
                    logger.debug("IndexUnreachableException thrown here: " + e.getMessage());

                }
            } else if (StringUtils.equalsIgnoreCase(text, WATERMARK_TEXT_TYPE_URN)) {
                if (StringUtils.isNotEmpty(urn)) {
                    urlBuilder.append(urn);
                    break;
                }
            } else if (StringUtils.equalsIgnoreCase(text, WATERMARK_TEXT_TYPE_PURL)) {
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
     * <p>
     * getThumbnailUrl.
     * </p>
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
     * <p>
     * getThumbnailUrl.
     * </p>
     *
     * @param width a int.
     * @param height a int.
     * @return a {@link java.lang.String} object.
     */
    public String getThumbnailUrl(int width, int height) {
        ThumbnailHandler thumbHandler = BeanUtils.getImageDeliveryBean().getThumbs();
        return thumbHandler.getThumbnailUrl(this, width, height);
    }

    /**
     * <p>
     * getId.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getId() {
        logger.debug("getPhysId");
        return physId;
    }

    /**
     * <p>
     * getFilepath.
     * </p>
     *
     * @return {@link java.lang.String} Path zu Image Datei.
     */
    public String getFilepath() {
        return filePath;
    }

    /**
     * <p>
     * Getter for the field <code>order</code>.
     * </p>
     *
     * @return a int.
     */
    public int getOrder() {
        return order;
    }

    /**
     * <p>
     * Getter for the field <code>orderLabel</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getOrderLabel() {
        return orderLabel;
    }

    /**
     * <p>
     * Getter for the field <code>urn</code>.
     * </p>
     *
     * @return the urn
     */
    public String getUrn() {
        return urn;
    }

    /**
     * <p>
     * Setter for the field <code>purlPart</code>.
     * </p>
     *
     * @param purlPart the purlPart to set
     */
    public void setPurlPart(String purlPart) {
        this.purlPart = purlPart;
    }

    /**
     * <p>
     * Getter for the field <code>purlPart</code>.
     * </p>
     *
     * @return the purlPart
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
        return getFullMimeType(getMimeType(), fileName);
    }

    /**
     * <p>
     * getFullMimeType.
     * </p>
     *
     * @param baseType a {@link java.lang.String} object.
     * @param fileName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getFullMimeType(String baseType, String fileName) {
        if (baseType.equals(MimeType.IMAGE.getName())) {
            //            return baseType + "/jpeg";
            ImageFileFormat fileFormat = ImageFileFormat.getImageFileFormatFromFileExtension(fileName);
            if (ImageFileFormat.PNG.equals(fileFormat)) {
                return fileFormat.getMimeType();
            }
            return ImageFileFormat.JPG.getMimeType();
        }

        return baseType;
    }

    /**
     * <p>
     * getFullMimeType.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Deprecated
    public String getFullMimeType() {
        return getDisplayMimeType();
    }

    /**
     * <p>
     * Getter for the field <code>mimeType</code>.
     * </p>
     *
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * <p>
     * Setter for the field <code>mimeType</code>.
     * </p>
     *
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * <p>
     * Setter for the field <code>width</code>.
     * </p>
     *
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * <p>
     * Setter for the field <code>height</code>.
     * </p>
     *
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Returns the filename alone, if {@link io.goobi.viewer.model.viewer.PhysicalElement#getFilePath()} is a local file, or the entire filepath
     * otherwise
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
     * <p>
     * getFileNameBase.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFileNameBase() {
        return FilenameUtils.getBaseName(fileName);
    }

    /**
     * <p>
     * getFileNameExtension.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFileNameExtension() {
        return FilenameUtils.getExtension(fileName);
    }

    /**
     * <p>
     * Getter for the field <code>fileIdRoot</code>.
     * </p>
     *
     * @return the fileIdRoot
     */
    public String getFileIdRoot() {
        return fileIdRoot;
    }

    /**
     * <p>
     * Setter for the field <code>fileIdRoot</code>.
     * </p>
     *
     * @param fileIdRoot the fileIdRoot to set
     */
    public void setFileIdRoot(String fileIdRoot) {
        this.fileIdRoot = fileIdRoot;
    }

    /**
     * 
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public boolean isDisplayImage() throws IndexUnreachableException, DAOException {
        if (!hasImage) {
            return false;
        }
        String filename = FileTools.getFilenameFromPathString(getFileName());
        if (StringUtils.isBlank(filename)) {
            return false;
        }

        return AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(BeanUtils.getRequest(), getPi(), filename,
                IPrivilegeHolder.PRIV_VIEW_IMAGES);
    }

    /**
     * @return the hasImage
     */
    public boolean isHasImage() {
        return hasImage;
    }

    /**
     * @param hasImage the hasImage to set
     */
    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    /**
     * <p>
     * isFulltextAvailableForPage.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isDisplayFulltext() throws IndexUnreachableException, DAOException {
        if (!fulltextAvailable) {
            return false;
        }
        String filename = FileTools.getFilenameFromPathString(getFulltextFileName());
        if (StringUtils.isBlank(filename)) {
            filename = FileTools.getFilenameFromPathString(getAltoFileName());
        }
        if (StringUtils.isBlank(filename)) {
            return false;
        }

        return AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(BeanUtils.getRequest(), getPi(), filename,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
    }

    /**
     * <p>
     * isFulltextAvailable.
     * </p>
     *
     * @return the fulltextAvailable
     */
    public boolean isFulltextAvailable() {
        return fulltextAvailable;
    }

    /**
     * <p>
     * Setter for the field <code>fulltextAvailable</code>.
     * </p>
     *
     * @param fulltextAvailable the fulltextAvailable to set
     */
    public void setFulltextAvailable(boolean fulltextAvailable) {
        this.fulltextAvailable = fulltextAvailable;
    }

    /**
     * <p>
     * isAltoAvailableForPage.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAltoAvailable() throws IndexUnreachableException, DAOException {
        String filename = FileTools.getFilenameFromPathString(getAltoFileName());
        if (StringUtils.isBlank(filename)) {
            return false;
        }

        return AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(BeanUtils.getRequest(), getPi(), filename,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
    }
    
    /**
     * <p>
     * isTeiAvailableForPage.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isTeiAvailable() throws IndexUnreachableException, DAOException {
        return isDisplayFulltext();
    }


    /**
     * <p>
     * Getter for the field <code>fulltextFileName</code>.
     * </p>
     *
     * @return the fulltextFileName
     */
    public String getFulltextFileName() {
        return fulltextFileName;
    }

    /**
     * <p>
     * Setter for the field <code>fulltextFileName</code>.
     * </p>
     *
     * @param fulltextFileName the fulltextFileName to set
     */
    public void setFulltextFileName(String fulltextFileName) {
        this.fulltextFileName = fulltextFileName;
    }

    /**
     * <p>
     * Getter for the field <code>altoFileName</code>.
     * </p>
     *
     * @return the altoFileName
     */
    public String getAltoFileName() {
        return altoFileName;
    }

    /**
     * <p>
     * Setter for the field <code>altoFileName</code>.
     * </p>
     *
     * @param altoFileName the altoFileName to set
     */
    public void setAltoFileName(String altoFileName) {
        this.altoFileName = altoFileName;
    }

    /**
     * <p>
     * Getter for the field <code>fullText</code>.
     * </p>
     *
     * @return the fullText
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getFullText() throws ViewerConfigurationException {
        if (altoText == null && wordCoordsFormat == CoordsFormat.UNCHECKED) {
            // Load XML document
            try {
                altoText = loadAlto();
            } catch (AccessDeniedException e) {
                fullText = ViewerResourceBundle.getTranslation(e.getMessage(), null);
            } catch (JDOMException | IOException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (StringUtils.isNotEmpty(altoText)) {
            wordCoordsFormat = CoordsFormat.ALTO;
            String text = ALTOTools.getFullText(altoText, false, null);
            return text;
        }
        wordCoordsFormat = CoordsFormat.NONE;
        if (fullText == null) {
            try {
                fullText = loadFullText();
            } catch (AccessDeniedException e) {
                fullText = e.getMessage();
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            } catch (IOException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (fullText != null && fullText.length() < 30) {
            return ViewerResourceBundle.getTranslation(fullText, null);
        }

        return fullText;
    }

    /**
     * <p>
     * Setter for the field <code>fullText</code>.
     * </p>
     *
     * @param fullText the fullText to set
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
     * @throws FileNotFoundException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws ConfigurationException
     * @should load full-text correctly if not yet loaded
     * @should return null if already loaded
     */
    String loadFullText()
            throws AccessDeniedException, FileNotFoundException, IOException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (fulltextFileName == null) {
            return null;
        }

        logger.trace("Loading full-text for page {}", fulltextFileName);
        String url = Helper.buildFullTextUrl(fulltextFileName);
        try {
            return Helper.getWebContentGET(url);
        } catch (HTTPException e) {
            logger.error("Could not retrieve file from {}", url);
            logger.error(e.getMessage());
            if (e.getCode() == 403) {
                logger.debug("Access denied for full-text file {}", fulltextFileName);
                throw new AccessDeniedException("fulltextAccessDenied");
            }
            return null;
        }
    }

    /**
     * <p>
     * getWordCoords.
     * </p>
     *
     * @param searchTerms a {@link java.util.Set} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getWordCoords(Set<String> searchTerms) throws ViewerConfigurationException {
        return getWordCoords(searchTerms, 0);
    }

    /**
     * Returns word coordinates for words that start with any of the given search terms.
     *
     * @param searchTerms a {@link java.util.Set} object.
     * @should load XML document if none yet set
     * @param rotation a int.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getWordCoords(Set<String> searchTerms, int rotation) throws ViewerConfigurationException {
        if (searchTerms == null || searchTerms.isEmpty()) {
            return Collections.emptyList();
        }
        logger.trace("loadWordCoords: {}", searchTerms.toString());

        if (altoText == null && wordCoordsFormat == CoordsFormat.UNCHECKED) {
            // Load XML document
            try {
                loadAlto();
            } catch (AccessDeniedException e) {
                fullText = ViewerResourceBundle.getTranslation(e.getMessage(), null);
            } catch (JDOMException | IOException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (altoText != null) {
            return ALTOTools.getWordCoords(altoText, searchTerms, rotation);
        }
        wordCoordsFormat = CoordsFormat.NONE;

        return Collections.emptyList();
    }

    /**
     * Loads ALTO data for this page via the REST service, if not yet loaded.
     *
     * @return true if ALTO successfully loaded; false otherwise
     * @should load and set alto correctly
     * @should set wordCoordsFormat correctly
     * @throws io.goobi.viewer.exceptions.AccessDeniedException if any.
     * @throws org.jdom2.JDOMException if any.
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String loadAlto()
            throws AccessDeniedException, JDOMException, IOException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("loadAlto: {}", altoFileName);
        if (altoFileName == null) {
            return null;
        }

        if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndFilePathWithSessionMap(BeanUtils.getRequest(), altoFileName,
                IPrivilegeHolder.PRIV_VIEW_FULLTEXT)) {
            logger.debug("Access denied for ALTO file {}", altoFileName);
            throw new AccessDeniedException("fulltextAccessDenied");
        }
        String url = Helper.buildFullTextUrl(altoFileName);
        logger.trace("ALTO URL: {}", url);
        try {
            altoText = Helper.getWebContentGET(url);
            if (altoText != null) {
                wordCoordsFormat = CoordsFormat.ALTO;
            }
            return altoText;
        } catch (HTTPException e) {
            logger.error("Could not retrieve file from {}", url);
            logger.error(e.getMessage());
            return null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>fileNames</code>.
     * </p>
     *
     * @return the fileNames
     */
    public Map<String, String> getFileNames() {
        return fileNames;
    }

    /**
     * <p>
     * Setter for the field <code>fileNames</code>.
     * </p>
     *
     * @param fileNames the fileNames to set
     */
    public void setFileNames(Map<String, String> fileNames) {
        this.fileNames = fileNames;
    }

    /**
     * <p>
     * getFileNameForFormat.
     * </p>
     *
     * @param format a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getFileNameForFormat(String format) {
        if (fileNames.get(format) != null) {
            return fileNames.get(format);
        }

        return fileName;
    }

    /**
     * Returns The first matching media filename for this page
     *
     * @return The first matching media filename for this page
     */
    public String getFilename() {
        String format = getFileNames().keySet().stream().findFirst().orElse("");
        return getFileNameForFormat(format);
    }

    /**
     * <p>
     * getImageToPdfUrl.
     * </p>
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
     * @param format a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getMediaUrl(String format) throws IndexUnreachableException {

        String url = BeanUtils.getImageDeliveryBean().getMedia().getMediaUrl(mimeType + "/" + format, pi, getFileNameForFormat(format));

        logger.trace("currentMediaUrl: {}", url.toString());
        return url.toString();
    }

    /**
     * <p>
     * getVideoWidth.
     * </p>
     *
     * @return a int.
     */
    public int getVideoWidth() {
        if (width > 0) {
            return width;
        }

        return defaultVideoWidth;
    }

    /**
     * <p>
     * getVideoHeight.
     * </p>
     *
     * @return a int.
     */
    public int getVideoHeight() {
        if (height > 0) {
            return height;
        }

        return defaultVideoHeight;
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
     * <p>
     * getPhysicalImageHeight.
     * </p>
     *
     * @return a int.
     */
    public int getPhysicalImageHeight() {
        return height;
    }

    /**
     * Return the zoom factor for this image depending on its actual size.
     *
     * @return a int.
     */
    public int getImageZoomFactor() {
        return getImageWidth() / 100;
    }

    /**
     * <p>
     * getImageUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getImageUrl() {
        ImageFileFormat format = ImageFileFormat.JPG;
        if (ImageFileFormat.PNG.equals(getImageType().getFormat())) {
            format = ImageFileFormat.PNG;
        }
        return new IIIFUrlHandler().getIIIFImageUrl(
                DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "image/" + pi + "/" + getFileName(), RegionRequest.FULL, Scale.MAX,
                Rotation.NONE, Colortype.DEFAULT, format);
    }

    /**
     * <p>
     * getImageUrl.
     * </p>
     *
     * @param size a int.
     * @return a {@link java.lang.String} object.
     */
    public String getImageUrl(int size) {
        ImageFileFormat format = ImageFileFormat.JPG;
        if (ImageFileFormat.PNG.equals(getImageType().getFormat())) {
            format = ImageFileFormat.PNG;
        }
        return new IIIFUrlHandler().getIIIFImageUrl(
                DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "image/" + pi + "/" + getFileName(), RegionRequest.FULL,
                new Scale.ScaleToWidth(size), Rotation.NONE, Colortype.DEFAULT, format);
    }

    /**
     * Return the bare width as read from the index (0 if none available).
     *
     * @return a int.
     */
    public int getMixWidth() {
        return width;
    }

    /**
     * <p>
     * getPhysicalImageWidth.
     * </p>
     *
     * @return a int.
     */
    public int getPhysicalImageWidth() {
        return width;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>
     * Getter for the field <code>accessConditions</code>.
     * </p>
     *
     * @return the accessConditions
     */
    public Set<String> getAccessConditions() {
        return accessConditions;
    }

    /**
     * <p>
     * Setter for the field <code>accessConditions</code>.
     * </p>
     *
     * @param accessConditions the accessConditions to set
     */
    public void setAccessConditions(Set<String> accessConditions) {
        this.accessConditions = accessConditions;
    }

    /**
     * <p>
     * getPageLinkLabel.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPageLinkLabel() {
        MimeType mimeType = MimeType.getByName(this.mimeType);
        if (mimeType == null) {
            return "viewImage";
        }

        switch (mimeType) {
            case IMAGE:
                return "viewImage";
            case VIDEO:
                return "viewVideo";
            case AUDIO:
                return "viewAudio";
            case SANDBOXED_HTML:
                return "viewSandboxedHtml";
            default:
                return "viewImage";
        }
    }

    /**
     * Checks if the media type is displayable as a 3d object and access is granted for viewing it
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermission3DObject() throws IndexUnreachableException, DAOException {
        logger.trace("AccessPermission3DObject");
        // Prevent access if mime type incompatible
        if (!MimeType.OBJECT.equals(MimeType.getByName(mimeType))) {
            return false;
        }

        if (getFilepath().startsWith("http")) {
            //External urls are always free to use
            return true;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            return AccessConditionUtils.checkAccessPermissionForImage(request, pi, fileName);
        } else {
            logger.trace("FacesContext not found");
        }

        return false;
    }

    /**
     * Checks if the media type is displayable as an image and access is granted for viewing an image
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermissionImage() throws IndexUnreachableException, DAOException {
        // logger.trace("AccessPermissionImage");
        // Prevent access if mime type incompatible
        if (!MimeType.isImageOrPdfDownloadAllowed(mimeType)) {
            return false;
        }

        if (getFilepath().startsWith("http")) {
            //External urls are always free to use
            return true;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            return AccessConditionUtils.checkAccessPermissionForImage(request, pi, fileName);
        } else {
            logger.trace("FacesContext not found");
        }

        return false;
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
        if (!MimeType.isImageOrPdfDownloadAllowed(mimeType)) {
            return false;
        }

        if (getFilepath().startsWith("http")) {
            //External urls are always free to use
            return true;
        } else if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            return AccessConditionUtils.checkAccessPermissionForImage(request, pi, fileName);
        } else {
            logger.trace("FacesContext not found");
        }

        return false;
    }

    /**
     * <p>
     * isAccessPermissionPdf.
     * </p>
     *
     * @return true if PDF download is allowed for this page; false otherwise
     */
    public boolean isAccessPermissionPdf() {
        if (!DataManager.getInstance().getConfiguration().isPagePdfEnabled()) {
            return false;
        }
        // Prevent access if mime type incompatible
        if (!MimeType.isImageOrPdfDownloadAllowed(mimeType)) {
            return false;
        }

        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        try {
            boolean accessPermissionPdf = AccessConditionUtils.checkAccessPermissionForPagePdf(request, this);
            // logger.trace("accessPermissionPdf for {}: {}", pi, accessPermissionPdf);
            return accessPermissionPdf;
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return false;
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
            return false;
        }
    }

    /**
     * <p>
     * isAccessPermissionBornDigital.
     * </p>
     *
     * @return true if access is allowed for born digital files; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAccessPermissionBornDigital() throws IndexUnreachableException, DAOException {
        return isAccessPermissionObject();
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isAccessPermissionFulltext() throws IndexUnreachableException, DAOException {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            return AccessConditionUtils.checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, fileName,
                    IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
        }
        logger.trace("FacesContext not found");

        return false;
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.getByName(PageType.viewImage.name()), getImageType());
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight(String pageType) throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.getByName(pageType), getImageType());
    }

    /**
     * <p>
     * Getter for the field <code>imageFooterHeight</code>.
     * </p>
     *
     * @return a int.
     */
    public int getImageFooterHeight() {
        return getImageFooterHeight(0);
    }

    /**
     * <p>
     * Getter for the field <code>imageFooterHeight</code>.
     * </p>
     *
     * @param rotation a int.
     * @return a int.
     */
    public int getImageFooterHeight(int rotation) {
        if (imageFooterHeight == null) {
            try {
                List<String> watermark = DataManager.getInstance().getConfiguration().getWatermarkTextConfiguration();
                if (watermark != null && !watermark.isEmpty() && ContentServerConfiguration.getInstance().getWatermarkUse()) {
                    int watermarkScale = ContentServerConfiguration.getInstance().getWatermarkPercent();
                    int imageHeight = this.getImageHeight();
                    int imageWidth = this.getImageWidth();
                    if (watermarkScale > 0) {
                        imageFooterHeight = (int) (imageHeight * watermarkScale / 100.0);
                        imageFooterHeightRotated = (int) (imageWidth * watermarkScale / 100.0);
                    } else if (ContentServerConfiguration.getInstance().getScaleWatermark()) {
                        double relHeight = new ConfigurationBean().getRelativeImageFooterHeight();
                        imageFooterHeight = (int) (imageHeight * relHeight);
                        imageFooterHeightRotated = (int) (imageWidth * relHeight);
                    } else {
                        imageFooterHeight = new ConfigurationBean().getImageFooterHeight();
                        imageFooterHeightRotated = imageFooterHeight;
                    }
                }
            } catch (Exception e) {
                imageFooterHeight = 0;
                imageFooterHeightRotated = 0;
            }
            if (imageFooterHeight == null) {
                imageFooterHeight = 0;
            }
            if (imageFooterHeightRotated == null) {
                imageFooterHeightRotated = 0;
            }
        }
        return rotation % 180 == 90 ? imageFooterHeightRotated : imageFooterHeight;
    }

    /**
     * <p>
     * Getter for the field <code>currentComment</code>.
     * </p>
     *
     * @return the currentComment
     */
    public Comment getCurrentComment() {
        return currentComment;
    }

    /**
     * <p>
     * Setter for the field <code>currentComment</code>.
     * </p>
     *
     * @param currentComment the currentComment to set
     */
    public void setCurrentComment(Comment currentComment) {
        this.currentComment = currentComment;
        logger.debug("currentComment: " + currentComment.getText());
    }

    /**
     * <p>
     * resetCurrentComment.
     * </p>
     */
    public void resetCurrentComment() {
        currentComment = new Comment(this.pi, this.order, null, "", null);
    }

    /**
     * <p>
     * getComments.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Comment> getComments() throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage(pi, order, true);
        Collections.sort(comments);
        //        Collections.reverse(comments);
        return comments;
    }

    /**
     * <p>
     * createNewCommentAction.
     * </p>
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void createNewCommentAction(User user) throws DAOException {
        if (user != null) {
            if (previousCommentText == null || !previousCommentText.equals(currentComment.getText())) {
                currentComment.setOwner(user);
                saveCommentAction(currentComment);
                previousCommentText = currentComment.getText();
                resetCurrentComment();
            } else {
                logger.trace("Comment not saved because the textual content is the same as the previous commennt: '{}'", previousCommentText);
            }
        }
    }

    /**
     * <p>
     * updateCommentAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.Comment} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void updateCommentAction(Comment comment) throws DAOException {
        // Set updated timestamp
        comment.setDateUpdated(new Date());
        saveCommentAction(comment);
        resetCurrentComment();
    }

    /**
     * Saves the given <code>Comment</code> and sends out notification emails. The language of the email is the default JSF locale for the current
     * theme.
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.Comment} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveCommentAction(Comment comment) throws DAOException {
        logger.trace("saveCommentAction");
        Locale defaultLocale = null;
        if (FacesContext.getCurrentInstance() != null) {
            defaultLocale = FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
        }
        // Check for any JS added to the comment text and remove it
        comment.checkAndCleanScripts();
        if (comment.getId() == null) {
            if (DataManager.getInstance().getDao().addComment(comment)) {
                Comment.sendEmailNotifications(comment, null, defaultLocale);
                Messages.info("commentSaveSuccess");
            } else {
                Messages.error("commentSaveFailure");
            }
        } else {
            if (DataManager.getInstance().getDao().updateComment(comment)) {
                Comment.sendEmailNotifications(comment, comment.getOldText(), defaultLocale);
                Messages.info("commentSaveSuccess");
            } else {
                Messages.error("commentSaveFailure");
            }
        }
    }

    /**
     * <p>
     * deleteCommentAction.
     * </p>
     *
     * @param comment a {@link io.goobi.viewer.model.annotation.Comment} object.
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
     * return true if this image has its own width/height measurements, and does not rely on default width/height
     *
     * @return a boolean.
     */
    public boolean hasIndividualSize() {
        return (width > 0 && height > 0);
    }

    /**
     * <p>
     * Getter for the field <code>altoText</code>.
     * </p>
     *
     * @return the altoText
     */
    public String getAltoText() {
        return altoText;
    }

    /**
     * <p>
     * Getter for the field <code>wordCoordsFormat</code>.
     * </p>
     *
     * @return the wordCoordsFormat
     */
    public CoordsFormat getWordCoordsFormat() {
        return wordCoordsFormat;
    }

    /**
     * <p>
     * Getter for the field <code>dataRepository</code>.
     * </p>
     *
     * @return the dataRepository
     */
    public String getDataRepository() {
        return dataRepository;
    }

    /**
     * <p>
     * Getter for the field <code>fileSize</code>.
     * </p>
     *
     * @return the fileSize
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * <p>
     * Setter for the field <code>fileSize</code>.
     * </p>
     *
     * @param fileSize the fileSize to set
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * <p>
     * getFileSizeAsString.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFileSizeAsString() {
        if (fileSize > 1024 * 1024) {
            return (fileSize / 1024 / 1024) + " MB";
        }

        else if (fileSize > 1024) {
            return (fileSize / 1024) + " KB";
        } else {
            return fileSize + " Byte";
        }
    }

    /**
     * <p>
     * getImageType.
     * </p>
     *
     * @return a {@link de.unigoettingen.sub.commons.contentlib.imagelib.ImageType} object.
     */
    public ImageType getImageType() {
        ImageType imageType = new ImageType(false);
        imageType.setFormat(ImageFileFormat.getImageFileFormatFromFileExtension(fileName));
        return imageType;
    }

    /**
     * Gets the filename but with its extension replaced by the given extension If the extension is an empty String, the filename without any
     * extension is returned If the extension is null, {@link io.goobi.viewer.model.viewer.PhysicalElement#getFileName()} is returned
     *
     * @param extension a {@link java.lang.String} object.
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
     * <p>
     * isDisplayPagePdfLink.
     * </p>
     *
     * @return true if page pdf link is allowed in configuration and no access conditions prevent PDF download; false otherwise
     */
    public boolean isDisplayPagePdfLink() {
        logger.trace("isDisplayPagePdfLink");
        return DataManager.getInstance().getConfiguration().isPagePdfEnabled() && isAccessPermissionPdf();
    }

    /**
     * <p>
     * isAdaptImageViewHeight.
     * </p>
     *
     * @return false if {@link Configuration#isLimitImageHeight} returns true and the image side ratio (width/height) is below the lower or above the
     *         upper threshold Otherwise return true
     */
    public boolean isAdaptImageViewHeight() {
        float ratio = getImageWidth() / (float) getImageHeight();
        //if dimensions cannot be determined (usually widht, height == 0), then return true
        if (Float.isNaN(ratio) || Float.isInfinite(ratio)) {
            return true;
        }
        float lowerThreshold = DataManager.getInstance().getConfiguration().getLimitImageHeightLowerRatioThreshold();
        float upperThreshold = DataManager.getInstance().getConfiguration().getLimitImageHeightUpperRatioThreshold();

        if (DataManager.getInstance().getConfiguration().isLimitImageHeight()) {
            return ratio > lowerThreshold && ratio < upperThreshold;
        }

        return true;
    }

    /**
     * List of struct elements that start on this page. For example, if a page contains multiple elements that only cover a certain area of the page
     * (using coordinates), this method can be used to get all shape coordinates for these elemets for visualization.
     * 
     * @return List of <code>/StructElement<code>s
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
                    containedStructElements.add(new StructElement(Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC)), doc));
                }
            }
        }

        return containedStructElements;
    }

    public String getContainedStructElementsAsJson() throws PresentationException, IndexUnreachableException, JsonProcessingException {
        List<StructElement> elements = getContainedStructElements();
        elements.forEach(element -> {

        });

        ObjectMapper mapper = new ObjectMapper();
        List<ShapeMetadata> shapes = elements.stream()
                .filter(ele -> ele.getShapeMetadata() != null && !ele.getShapeMetadata().isEmpty())
                .flatMap(ele -> ele.getShapeMetadata().stream())
                .collect(Collectors.toList());
        String json = mapper.writeValueAsString(shapes);
        return json;
    }
}
