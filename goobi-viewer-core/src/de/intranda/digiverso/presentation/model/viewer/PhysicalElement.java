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
package de.intranda.digiverso.presentation.model.viewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.ALTOTools;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.FileTools;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.controller.imaging.IIIFUrlHandler;
import de.intranda.digiverso.presentation.controller.imaging.ImageHandler;
import de.intranda.digiverso.presentation.controller.imaging.PdfHandler;
import de.intranda.digiverso.presentation.controller.imaging.ThumbnailHandler;
import de.intranda.digiverso.presentation.exceptions.AccessDeniedException;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.ConfigurationBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.annotation.Comment;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType.Colortype;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.RegionRequest;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Rotation;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetImageDimensionAction;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

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

    public static final String WATERMARK_TEXT_TYPE_URN = "URN";
    public static final String WATERMARK_TEXT_TYPE_PURL = "PURL";
    public static final String WATERMARK_TEXT_TYPE_SOLR = "SOLR:";
    public static final String MIME_TYPE_IMAGE = "image";
    public static final String MIME_TYPE_VIDEO = "video";
    public static final String MIME_TYPE_AUDIO = "audio";
    public static final String MIME_TYPE_APPLICATION = "application";
    public static final String MIME_TYPE_SANDBOXED_HTML = "text";

    private static List<String> watermarkTextConfiguration;
    public static int defaultVideoWidth = 320;
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
    private String mimeType = MIME_TYPE_IMAGE;
    /** Actual image/video width (if available). */
    private int width = 0;
    /** Actual image/video height (if available). */
    private int height = 0;
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
    /** set to false if the image cannot be read **/
    private Boolean imageAvailable = null;

    /**
     * 
     * @param physId Physical element ID
     * @param filePath Path to the file
     * @param order Page number (numerical)
     * @param orderLabel Page number (label)
     * @param urn Page URN
     * @param purlPart
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
     * 
     * @param filePath
     * @return
     * @should cut off everything but the file name for normal file paths
     * @should leave external urls intact
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
     * 
     * @return the url to the media content of the page, for example the
     * @throws IndexUnreachableException
     * @throws ConfigurationException 
     */
    public String getUrl() throws IndexUnreachableException, ConfigurationException {

        switch (mimeType) {
            case MIME_TYPE_IMAGE:
                return getImageUrl();
            case MIME_TYPE_VIDEO:
            case MIME_TYPE_AUDIO: {
                
                String format = getFileNames().keySet().stream().findFirst().orElse("");
                return getMediaUrl(format);
            }
            case MIME_TYPE_APPLICATION:
                if (StringUtils.isEmpty(fileName)) {
                    fileName = determineFileName(filePath);
                }
                String localFilename = fileName;

                PdfHandler pdfHandler = BeanUtils.getImageDeliveryBean().getPdf();
                return pdfHandler.getPdfUrl(pi, localFilename);

            case MIME_TYPE_SANDBOXED_HTML:
                return getSandboxedUrl();
            default:
                logger.error("Page {} of record '{}' has unknown mime-type: {}", orderLabel, pi, mimeType);
        }

        return "";
    }
    
    public String getSandboxedUrl() {
        logger.trace(fileNames.toString());
        if (fileNames.get("html-sandboxed") != null) {
            return fileNames.get("html-sandboxed");
        }
        return filePath;
    }

    public String getWatermarkText() {
        if (watermarkTextConfiguration != null && !watermarkTextConfiguration.isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder();
            for (String text : watermarkTextConfiguration) {
                if (StringUtils.startsWithIgnoreCase(text, WATERMARK_TEXT_TYPE_SOLR)) {
                    String field = text.substring(WATERMARK_TEXT_TYPE_SOLR.length());
                    try {
                        SolrDocumentList res = DataManager.getInstance().getSearchIndex().search(
                                new StringBuilder(SolrConstants.PI).append(":").append(pi).toString(), SolrSearchIndex.MAX_HITS, null,
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

        return "";
    }

    /**
     *
     * @return {@link String}
     */
    public String getThumbnailUrl() {
        int thumbWidth = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
        int thumbHeight = DataManager.getInstance().getConfiguration().getThumbnailsHeight();

        return getThumbnailUrl(thumbWidth, thumbHeight);
    }

    public String getThumbnailUrl(int width, int height) {
        ThumbnailHandler thumbHandler = BeanUtils.getImageDeliveryBean().getThumb();
        return thumbHandler.getThumbnailUrl(this, width, height);
    }

    public String getId() {
        logger.debug("getPhysId");
        return physId;
    }

    /**
     * @return {@link String} Path zu Image Datei.
     */
    public String getFilepath() {
        return filePath;
    }

    public int getOrder() {
        return order;
    }

    public String getOrderLabel() {
        return orderLabel;
    }

    /**
     * @return the urn
     */
    public String getUrn() {
        return urn;
    }

    /**
     * @param purlPart the purlPart to set
     */
    public void setPurlPart(String purlPart) {
        this.purlPart = purlPart;
    }

    /**
     * @return the purlPart
     */
    public String getPurlPart() {
        return purlPart;
    }

    /**
     * For images, this returns the full mime-type as image/X, with X being the format which should be used for image display. This is png for
     * png-images and jpeg for all other types.
     * 
     * 
     * @return
     */
    public String getDisplayMimeType() {
        return getFullMimeType(getMimeType(), fileName);
    }

    /**
     * 
     * @param baseType
     * @param fileName
     * @return
     */
    public static String getFullMimeType(String baseType, String fileName) {
        if (baseType.equals(MIME_TYPE_IMAGE)) {
            //            return baseType + "/jpeg";
            ImageFileFormat fileFormat = ImageFileFormat.getImageFileFormatFromFileExtension(fileName);
            if (ImageFileFormat.PNG.equals(fileFormat)) {
                return fileFormat.getMimeType();
            }
            return ImageFileFormat.JPG.getMimeType();
        }

        return baseType;
    }

    @Deprecated
    public String getFullMimeType() {
        return getDisplayMimeType();
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    public String getFileName() {
        if (StringUtils.isEmpty(fileName)) {
            determineFileName(filePath);
        }
        return fileName;
    }

    public String getFileNameBase() {
        return FilenameUtils.getBaseName(fileName);
    }

    public String getFileNameExtension() {
        return FilenameUtils.getExtension(fileName);
    }

    /**
     * @return the fileIdRoot
     */
    public String getFileIdRoot() {
        return fileIdRoot;
    }

    /**
     * @param fileIdRoot the fileIdRoot to set
     */
    public void setFileIdRoot(String fileIdRoot) {
        this.fileIdRoot = fileIdRoot;
    }

    /**
     * @return the fulltextAvailable
     */
    public boolean isFulltextAvailable() {
        return fulltextAvailable;
    }

    /**
     * @param fulltextAvailable the fulltextAvailable to set
     */
    public void setFulltextAvailable(boolean fulltextAvailable) {
        this.fulltextAvailable = fulltextAvailable;
    }

    /**
     * @return the fulltextFileName
     */
    public String getFulltextFileName() {
        return fulltextFileName;
    }

    /**
     * @param fulltextFileName the fulltextFileName to set
     */
    public void setFulltextFileName(String fulltextFileName) {
        this.fulltextFileName = fulltextFileName;
    }

    /**
     * @return the altoFileName
     */
    public String getAltoFileName() {
        return altoFileName;
    }

    /**
     * @param altoFileName the altoFileName to set
     */
    public void setAltoFileName(String altoFileName) {
        this.altoFileName = altoFileName;
    }

    /**
     * @return the fullText
     */
    public String getFullText() {
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
            String text = ALTOTools.getFullText(altoText);
            return text;
        }
        wordCoordsFormat = CoordsFormat.NONE;
        if (fullText == null) {
            try {
                fullText = loadFullText();
            } catch (AccessDeniedException e) {
                fullText = ViewerResourceBundle.getTranslation(e.getMessage(), null);
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            } catch (IOException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return fullText;
    }

    /**
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
     * @should load full-text correctly if not yet loaded
     * @should return null if already loaded
     */
    String loadFullText() throws AccessDeniedException, FileNotFoundException, IOException, IndexUnreachableException, DAOException {
        if (fullText == null && fulltextFileName != null) {
            if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndFilePathWithSessionMap(BeanUtils.getRequest(), fulltextFileName,
                    IPrivilegeHolder.PRIV_VIEW_FULLTEXT)) {
                logger.debug("Access denied for full-text file {}", fulltextFileName);
                throw new AccessDeniedException("fulltextAccessDenied");
            }
            logger.trace("Loading full-text for page {}", fulltextFileName);
            String url = Helper.buildFullTextUrl(dataRepository, fulltextFileName);
            try {
                return Helper.getWebContentGET(url);
            } catch (HTTPException e) {
                logger.error("Could not retrieve file from {}", url);
                logger.error(e.getMessage());
            }
        }

        return null;
    }

    public List<String> getWordCoords(Set<String> searchTerms) {
        return getWordCoords(searchTerms, 0);
    }

    /**
     * Returns word coordinates for words that start with any of the given search terms.
     *
     * @param searchTerms
     * @return
     * @throws IOException
     * @throws JDOMException
     * @should load XML document if none yet set
     */
    public List<String> getWordCoords(Set<String> searchTerms, int rotation) {
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
            Document altoDoc;
            try {
                altoDoc = FileTools.getDocumentFromString(altoText, "UTF-8");
                return ALTOTools.getWordCoords(altoDoc, searchTerms, rotation, getImageFooterHeight());
            } catch (JDOMException | IOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            wordCoordsFormat = CoordsFormat.NONE;
        }

        return Collections.emptyList();
    }

    /**
     * Loads ALTO data for this page via the REST service, if not yet loaded.
     * 
     * @return true if ALTO successfully loaded; false otherwise
     * @throws AccessDeniedException
     * @throws IOException
     * @throws JDOMException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @should load and set alto correctly
     * @should set wordCoordsFormat correctly
     */
    public String loadAlto() throws AccessDeniedException, JDOMException, IOException, IndexUnreachableException, DAOException {
        logger.trace("loadAlto: {}", altoFileName);
        if (altoText == null && altoFileName != null) {
            if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndFilePathWithSessionMap(BeanUtils.getRequest(), altoFileName,
                    IPrivilegeHolder.PRIV_VIEW_FULLTEXT)) {
                logger.debug("Access denied for ALTO file {}", altoFileName);
                throw new AccessDeniedException("fulltextAccessDenied");
            }
            String url = Helper.buildFullTextUrl(dataRepository, altoFileName);
            logger.trace("URL: {}", url);
            try {
                altoText = Helper.getWebContentGET(url);
                if (altoText != null) {
                    wordCoordsFormat = CoordsFormat.ALTO;
                }
                return altoText;
            } catch (HTTPException e) {
                logger.error("Could not retrieve file from {}", url);
                logger.error(e.getMessage());
            }
        }

        return null;

    }

    /**
     * @return the fileNames
     */
    public Map<String, String> getFileNames() {
        return fileNames;
    }

    /**
     * @param fileNames the fileNames to set
     */
    public void setFileNames(Map<String, String> fileNames) {
        this.fileNames = fileNames;
    }

    public String getFileNameForFormat(String format) {
        if (fileNames.get(format) != null) {
            return fileNames.get(format);
        }

        return fileName;
    }

    public String getImageToPdfUrl() {
        
        return BeanUtils.getImageDeliveryBean().getPdf().getPdfUrl(BeanUtils.getImageDeliveryBean().getCurrentDocumentIfExists().orElse(null), this);
        
//        StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl());
//        sb.append("?action=pdf")
//                .append("&images=")
//                .append(pi)
//                .append("/")
//                .append(fileName)
//                .append("&metsFile=")
//                .append(pi)
//                .append(".xml")
//                .append("&targetFileName=")
//                .append(pi)
//                .append("_")
//                .append(order)
//                .append(".pdf");
//        return sb.toString();
        //        return DataManager.getInstance().getConfiguration().getContentServerWrapperUrl() + "?action=pdf&images=" + pi + "/" + fileName
        //                + "&targetFileName=" + pi + "_" + order + ".pdf";
    }

    /**
     * Returns a "RESTful" URL for a media (audio or video) file in the given format.
     *
     * @param format
     * @return
     * @throws IndexUnreachableException
     */
    public String getMediaUrl(String format) throws IndexUnreachableException, ConfigurationException {
        
        String url = BeanUtils.getImageDeliveryBean().getMedia().getMediaUrl(mimeType + "/" + format, pi, getFileNameForFormat(format));

        logger.trace("currentMediaUrl: {}", url.toString());
        return url.toString();
    }

    public int getVideoWidth() {
        if (width > 0) {
            return width;
        }

        return defaultVideoWidth;
    }

    public int getVideoHeight() {
        if (height > 0) {
            return height;
        }

        return defaultVideoHeight;
    }

    /**
     * Returns the actual image width, if available. Otherwise the default width.
     *
     * @return
     */
    public int getImageWidth() {
        return width;
    }

    /**
     * Returns the actual image height, if available. Otherwise the default height.
     *
     * @return
     */
    public int getImageHeight() {
        return height;
    }

    /**
     *
     * @return
     */
    public int getPhysicalImageHeight() {
        return height;
    }

    /**
     * Returns the actual image width, if available. Otherwise the default width multiplied by 3. Used for zoom.
     *
     * @param rotation Rotation angle.
     * @return
     */
    public int getImageMaxWidth(int rotation) {
        if (rotation == 0 || rotation == 180) {
            if (width > 0) {
                return width;
            }
            return DataManager.getInstance().getConfiguration().getDefaultImageWidth() * 3;
        }
        if (height > 0) {
            return height;
        }

        return DataManager.getInstance().getConfiguration().getDefaultImageHeight() * 3;
    }

    /**
     * Returns the actual image height, if available. Otherwise the default height multiplied by 3. Used for zoom.
     *
     * @param rotation Rotation angle.
     * @return
     */
    public int getImageMaxHeight(int rotation) {
        if (rotation == 0 || rotation == 180) {
            if (height > 0) {
                return height;
            }
            return DataManager.getInstance().getConfiguration().getDefaultImageHeight() * 3;
        }
        if (width > 0) {
            return width;
        }
        return DataManager.getInstance().getConfiguration().getDefaultImageWidth() * 3;
    }

    /**
     * Return the initial width at which to display the image. Should not be larger than the default width, but may be smaller. Used for initial
     * display.
     *
     * @param rotation Rotation angle.
     * @return
     */
    public int getImageDefaultWidth(int rotation) {
        if (rotation == 0 || rotation == 180) {
            return DataManager.getInstance().getConfiguration().getDefaultImageWidth();
        }
        return DataManager.getInstance().getConfiguration().getDefaultImageHeight();
    }

    /**
     * Return the initial height at which to display the image. Should not be larger than the default height, but may be smaller. Used for initial
     * display.
     *
     * @param rotation Rotation angle.
     * @return
     */
    public int getImageDefaultHeight(int rotation) {
        if (rotation == 0 || rotation == 180) {
            return DataManager.getInstance().getConfiguration().getDefaultImageHeight();
        }
        return DataManager.getInstance().getConfiguration().getDefaultImageWidth();
    }

    /**
     * Return the initial width at which to display the image in full screen mode. Should not be larger than defaultImageFullscreenWidth, but may be
     * smaller.
     *
     * @return
     */
    public int getImageDefaultFullscreenWidth() {
        return DataManager.getInstance().getConfiguration().getDefaultImageFullscreenWidth();
    }

    /**
     * Return the initial height at which to display the image in full screen mode. Should not be larger than defaultImageFullscreenHeight, but may be
     * smaller.
     *
     * @return
     */
    public int getImageDefaultFullscreenHeight() {
        return DataManager.getInstance().getConfiguration().getDefaultImageFullscreenHeight();
    }

    /**
     * Return the zoom factor for this image depending on its actual size.
     *
     * @return
     */
    public int getImageZoomFactor() {
        return getImageWidth() / 100;
    }

    public String getImageUrl() {
        ImageFileFormat format = ImageFileFormat.JPG;
        if (ImageFileFormat.PNG.equals(getImageType().getFormat())) {
            format = ImageFileFormat.PNG;
        }
        return new IIIFUrlHandler().getIIIFImageUrl(DataManager.getInstance().getConfiguration().getIiifUrl(), RegionRequest.FULL, Scale.MAX,
                Rotation.NONE, Colortype.DEFAULT, format);
    }

    public String getImageUrl(int size) {
        ImageFileFormat format = ImageFileFormat.JPG;
        if (ImageFileFormat.PNG.equals(getImageType().getFormat())) {
            format = ImageFileFormat.PNG;
        }
        return new IIIFUrlHandler().getIIIFImageUrl(DataManager.getInstance().getConfiguration().getIiifUrl(), RegionRequest.FULL,
                new Scale.ScaleToWidth(size), Rotation.NONE, Colortype.DEFAULT, format);
    }

    /**
     * Return the bare width as read from the index (0 if none available).
     *
     * @return
     */
    public int getMixWidth() {
        return width;
    }

    /**
     *
     * @return
     */
    public int getPhysicalImageWidth() {
        return width;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @return the accessConditions
     */
    public Set<String> getAccessConditions() {
        return accessConditions;
    }

    /**
     * @param accessConditions the accessConditions to set
     */
    public void setAccessConditions(Set<String> accessConditions) {
        this.accessConditions = accessConditions;
    }

    public String getPageLinkLabel() {
        switch (mimeType) {
            case MIME_TYPE_IMAGE:
                return "viewImage";
            case MIME_TYPE_VIDEO:
                return "viewVideo";
            case MIME_TYPE_AUDIO:
                return "viewAudio";
            case MIME_TYPE_SANDBOXED_HTML:
                return "viewSandboxedHtml";
        }

        return "viewImage";
    }

    /**
     * Remnant from when image access had to be checked for each tile. Still used for OpenSeaDragon, so it just redirects to the access permission
     * check.
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isAccessForJs() throws IndexUnreachableException, DAOException {
        logger.trace("isAccessForJs");
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
    
    public int getFooterHeight() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.getByName(PageType.viewImage.name()), getImageType());
    }
    
    public int getFooterHeight(String pageType) throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.getByName(pageType), getImageType());
    }

    public int getImageFooterHeight() {
        return getImageFooterHeight(0);
    }

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
     * @return the currentComment
     */
    public Comment getCurrentComment() {
        return currentComment;
    }

    /**
     * @param currentComment the currentComment to set
     */
    public void setCurrentComment(Comment currentComment) {
        this.currentComment = currentComment;
        logger.debug("currentComment: " + currentComment.getText());
    }

    public void resetCurrentComment() {
        currentComment = new Comment(this.pi, this.order, null, "", null);
    }

    public List<Comment> getComments() throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage(pi, order, true);
        Collections.sort(comments);
        Collections.reverse(comments);
        return comments;
    }

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
     * @param comment
     * @throws DAOException
     */
    public void saveCommentAction(Comment comment) throws DAOException {
        logger.trace("saveCommentAction");
        Locale defaultLocale = null;
        if (FacesContext.getCurrentInstance() != null) {
            defaultLocale = FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
        }
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
     * @return
     */
    public boolean hasIndividualSize() {
        return (width > 0 && height > 0);
    }

    /**
     * @return the altoText
     */
    public String getAltoText() {
        return altoText;
    }

    /**
     * @return the wordCoordsFormat
     */
    public CoordsFormat getWordCoordsFormat() {
        return wordCoordsFormat;
    }

    /**
     * @return the fileSize
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize the fileSize to set
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

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
     * @return
     */
    public ImageType getImageType() {
        ImageType imageType = new ImageType(false);
        imageType.setFormat(ImageFileFormat.getImageFileFormatFromFileExtension(fileName));
        return imageType;
    }

}
