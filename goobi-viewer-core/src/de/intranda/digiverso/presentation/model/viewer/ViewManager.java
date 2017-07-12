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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.jdom2.JDOMException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.AlphanumCollatorComparator;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.TranskribusUtils;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.calendar.CalendarView;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusJob;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusSession;
import de.intranda.digiverso.presentation.model.user.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.user.User;
import de.intranda.digiverso.presentation.model.viewer.pageloader.IPageLoader;

/**
 * Holds information about the currently open record (structure, pages, etc.). Used to reduced the size of ActiveDocumentBean.
 */
public class ViewManager implements Serializable {

    private static final long serialVersionUID = -7776362205876306849L;

    private static final Logger logger = LoggerFactory.getLogger(ViewManager.class);

    private SearchBean searchBean;

    /** IDDOC of the top level document. */
    private long topDocumentIddoc;
    /** IDDOC of the current level document. The initial top level document values eventually gets overridden with the image owner element's IDDOC. */
    private long currentDocumentIddoc;
    /** LOGID of the current level document. */
    private String logId;

    /** Document of the anchor element, if applicable. */
    private StructElement anchorDocument;

    /** Top level document. */
    private StructElement topDocument;

    /** Currently selected document. */
    private StructElement currentDocument;

    private IPageLoader pageLoader;
    private PhysicalElement representativePage;

    private int rotate = 0;
    private int zoomSlider;
    private int currentImageOrder = -1;
    private List<SelectItem> dropdownPages = new ArrayList<>();
    private List<SelectItem> dropdownFulltext = new ArrayList<>();
    private String dropdownSelected = "";
    private int currentThumbnailPage = 1;
    private String pi;
    private Boolean accessPermissionPdf = null;
    private Boolean allowUserComments = null;
    private String persistentUrl = null;
    private boolean displayImage = false;
    private List<StructElementStub> docHierarchy = null;
    private String mainMimeType = null;
    private Boolean filesOnly = null;
    private String opacUrl = null;
    private String contextObject = null;
    private List<String> versionHistory = null;
    private PageOrientation firstPageOrientation = PageOrientation.left;
    private boolean doublePageMode = false;
    private int firstPdfPage;
    private int lastPdfPage;
    private final CalendarView calendarView;

    public ViewManager(StructElement topDocument, IPageLoader pageLoader, long currentDocumentIddoc, String logId, String mainMimeType)
            throws IndexUnreachableException {
        this.topDocument = topDocument;
        this.topDocumentIddoc = topDocument.getLuceneId();
        logger.trace("New ViewManager: {} / {} / {}", topDocument.getLuceneId(), currentDocumentIddoc, logId);
        this.pageLoader = pageLoader;
        this.currentDocumentIddoc = currentDocumentIddoc;
        this.logId = logId;
        if (topDocumentIddoc == currentDocumentIddoc) {
            currentDocument = topDocument;
        } else {
            currentDocument = new StructElement(currentDocumentIddoc);
        }
        // Set the anchor StructElement for extracting metadata later
        if (topDocument.isAnchorChild()) {
            anchorDocument = topDocument.getParent();
        }
        currentThumbnailPage = 1;
        //        annotationManager = new AnnotationManager(topDocument);
        pi = topDocument.getPi();
        if (!topDocument.isAnchor()) {
            // Generate drop-down page selector elements
            dropdownPages.clear();
            dropdownFulltext.clear();
            if (pageLoader != null) {
                pageLoader.generateSelectItems(dropdownPages, dropdownFulltext, BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            }
        }
        this.mainMimeType = mainMimeType;
        logger.trace("mainMimeType: {}", mainMimeType);

        calendarView = new CalendarView(pi, topDocument.isAnchor(), topDocument.isAnchor() ? null : topDocument.getMetadataValue(
                SolrConstants._CALENDAR_YEAR));
        if (topDocument.getMetadataValue(SolrConstants._CALENDAR_YEAR) != null) {
            try {
                calendarView.populateCalendar();
            } catch (PresentationException e) {
                logger.debug(e.getMessage());
            }
        }
    }

    public String getRepresentativeImageInfo() throws IndexUnreachableException, DAOException, PresentationException {
        PhysicalElement representative = getRepresentativePage();
        if (representative == null) {
            return "";
        }

        StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
        urlBuilder.append("image/").append(pi).append('/').append(representative.getFileName()).append("/info.json");
        return urlBuilder.toString();
    }

    public String getCurrentImageInfo() throws IndexUnreachableException, DAOException {
        return getCurrentImageInfo(BeanUtils.getNavigationHelper().getCurrentPage());
    }

    public String getCurrentImageInfo(String pageType) throws IndexUnreachableException, DAOException {
        StringBuilder urlBuilder = new StringBuilder();
        if (isDoublePageMode()) {
            urlBuilder.append("[");
            String imageInfoLeft = getImageInfo(getCurrentLeftPage());
            String imageInfoRight = getImageInfo(getCurrentRightPage());
            if (StringUtils.isNotBlank(imageInfoLeft)) {
                urlBuilder.append("\"").append(imageInfoLeft).append("\"");
            }
            if (StringUtils.isNotBlank(imageInfoLeft) && StringUtils.isNotBlank(imageInfoRight)) {
                urlBuilder.append(", ");
            }
            if (StringUtils.isNotBlank(imageInfoRight)) {
                urlBuilder.append("\"").append(imageInfoRight).append("\"");
            }
            urlBuilder.append("]");
        } else {
            urlBuilder.append(getImageInfo(getCurrentPage()));
        }
        return urlBuilder.toString();
    }

    /**
     * @param currentPage
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    private PhysicalElement getCurrentLeftPage() throws IndexUnreachableException, DAOException {
        boolean actualPageOrderEven = this.currentImageOrder % 2 == 0;
        PageOrientation actualPageOrientation = actualPageOrderEven ? firstPageOrientation.opposite() : firstPageOrientation;
        if (actualPageOrientation.equals(PageOrientation.left)) {
            return getPage(this.currentImageOrder);
        }

        return getPage(this.currentImageOrder - 1);
    }

    /**
     * @param currentPage
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    private PhysicalElement getCurrentRightPage() throws IndexUnreachableException, DAOException {
        boolean actualPageOrderEven = this.currentImageOrder % 2 == 0;
        PageOrientation actualPageOrientation = actualPageOrderEven ? firstPageOrientation.opposite() : firstPageOrientation;
        if (actualPageOrientation.equals(PageOrientation.right)) {
            return getPage(this.currentImageOrder);
        }

        return getPage(this.currentImageOrder + 1);
    }

    /**
     * @param page
     * @return
     */
    private String getImageInfo(PhysicalElement page) {
        StringBuilder urlBuilder = new StringBuilder();
        if (page != null) {
            if (page.isExternalUrl()) {
                String url = page.getFilepath();
                urlBuilder.append(url);
            } else {
                urlBuilder.append(DataManager.getInstance().getConfiguration().getIiifUrl());
                urlBuilder.append("image/").append(pi).append('/').append(page.getFileName()).append("/info.json");
            }
        }
        return urlBuilder.toString();
    }

    public String getCurrentImageInfoFullscreen() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }
        if (currentPage.isExternalUrl()) {
            return getCurrentImageInfo();
        }
        StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
        urlBuilder.append("fullscreen/image/").append(pi).append('/').append(currentPage.getFileName()).append("/info.json");
        return urlBuilder.toString();
    }

    public String getCurrentImageInfoCrowd() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }
        if (currentPage.isExternalUrl()) {
            return getCurrentImageInfo();
        }
        StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
        urlBuilder.append("crowdsourcing/image/").append(pi).append('/').append(currentPage.getFileName()).append("/info.json");
        return urlBuilder.toString();
    }

    public String getCurrentImageUrl(int width, int height, float rotation) throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }

        StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
        urlBuilder.append("image/").append(pi).append('/').append(currentPage.getFileName()).append("/full/!").append(width).append(",").append(
                height).append('/').append(rotation).append("/default.jpg");
        return urlBuilder.toString();
    }

    public String getWatermarkUrl() throws IndexUnreachableException, DAOException {
        StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getIiifUrl());
        String format = DataManager.getInstance().getConfiguration().getWatermarkFormat();
        if (getCurrentPage() != null) {
            return urlBuilder.append("footer/full/!{width},{height}/0/default." + format + "?watermarkId=" + getFooterId() + getCurrentPage()
                    .getWatermarkText()).toString();
        }

        return urlBuilder.append("footer/full/!{width},{height}/0/default." + format + "?watermarkId=" + getFooterId()).toString();
    }

    public String getCurrentPreviewUrl() throws IndexUnreachableException, DAOException, ConfigurationException {
        int width = DataManager.getInstance().getConfiguration().getPreviewWidth();
        int previewHeightPercentage = DataManager.getInstance().getConfiguration().getPreviewHeightPercentage();
        PhysicalElement currentImg = getCurrentPage();
        if (currentImg == null) {
            return "";
        }
        String imageUrl = currentImg.getUrl(width, width * previewHeightPercentage, 0, false, false, null, null);
        String previewUrl = new StringBuilder(imageUrl).append("&ignoreWatermark=true&roi=0,0,100,").append(previewHeightPercentage).toString();
        return previewUrl;
    }

    public String getCurrentThumbnailUrl() throws IndexUnreachableException, DAOException {
        int width = DataManager.getInstance().getConfiguration().getPreviewThumbnailWidth();
        int height = DataManager.getInstance().getConfiguration().getPreviewThumbnailHeight();
        PhysicalElement currentImg = getCurrentPage();
        if (currentImg == null) {
            return "";
        }
        return currentImg.getThumbnailUrl(width, height);
    }

    public String getNeighbourPreviewUrl(int step) throws IndexUnreachableException, DAOException, ConfigurationException {
        PhysicalElement page = getPage(currentImageOrder + step);
        if (page != null) {
            int width = DataManager.getInstance().getConfiguration().getPreviewWidth();
            int previewHeightPercentage = DataManager.getInstance().getConfiguration().getPreviewHeightPercentage();
            String imageUrl = page.getUrl(width, width * previewHeightPercentage, 0, false, false, null, null);
            return imageUrl + "&ignoreWatermark=true" + "&roi=0,0,100," + previewHeightPercentage;
        }

        return "";
    }

    public String getNeighbourThumbnailUrl(int step) throws IndexUnreachableException, DAOException {
        PhysicalElement page = getPage(currentImageOrder + step);
        if (page != null) {
            int width = DataManager.getInstance().getConfiguration().getPreviewThumbnailWidth();
            int height = DataManager.getInstance().getConfiguration().getPreviewThumbnailHeight();
            return page.getThumbnailUrl(width, height);
        }

        return "";
    }

    public String getCurrentImageUrl() throws IndexUnreachableException, DAOException, ConfigurationException {
        return getCurrentImageUrl(false, false);
    }

    public String getCurrentImageUrlLarge() throws IndexUnreachableException, DAOException, ConfigurationException {
        return getCurrentImageUrl(false, true);

    }

    public String getCurrentImageUrlFullscreen() throws IndexUnreachableException, DAOException, ConfigurationException {
        return getCurrentImageUrl(true, false);
    }

    public String getCurrentImageUrlFullscreenLarge() throws IndexUnreachableException, DAOException, ConfigurationException {
        return getCurrentImageUrl(true, true);
    }

    public String getCurrentImageUrl(boolean fullscreen, boolean enlarged) throws IndexUnreachableException, DAOException, ConfigurationException {
        logger.trace("getCurrentImageUrl");
        PhysicalElement currentImg = getCurrentPage();
        if (currentImageOrder != -1 && currentImg != null) {
            List<String> coords = getSearchResultCoords(currentImg);

            Dimension imageSize = new Dimension(currentImg.getImageWidth(), currentImg.getImageHeight());
            if (imageSize.height * imageSize.width == 0) {
                return ""; //no image available
            }
            if (fullscreen) {
                if (enlarged) {
                    // width=defaultwidth, so the image doesn't have a unique width
                    logger.debug("Creating larger image");
                    int height = currentImg.getImageDefaultFullscreenHeight() * DataManager.getInstance().getConfiguration().getFullscreenZoomScale();
                    imageSize = scaleToHeight(imageSize, height);
                } else {
                    int height = currentImg.getImageDefaultFullscreenHeight();
                    imageSize = scaleToHeight(imageSize, height);
                }
            } else {
                if (enlarged) {
                    logger.trace("Creating larger image");
                    int width = currentImg.getImageDefaultWidth(0) * DataManager.getInstance().getConfiguration().getImageViewZoomScale();
                    imageSize = scaleToWidth(imageSize, width);
                } else {
                    int width = currentImg.getImageDefaultWidth(0);
                    imageSize = scaleToWidth(imageSize, width);
                }
            }

            String footerId = getFooterId();

            String url = currentImg.getUrl(imageSize.width, imageSize.height, getCurrentRotate(), true, fullscreen, coords, footerId);
            logger.debug("Calling content server url: {}", url);
            return url;
        }
        return null;
    }

    public List<ImageLevel> getRepresentativeImageUrls() throws IndexUnreachableException, PresentationException, DAOException,
            ConfigurationException {
        PhysicalElement representative = getRepresentativePage();
        List<ImageLevel> imageUrls = new ArrayList<>();
        if (representative != null) {

            //            List<String> coords = getSearchResultCoords(currentImage);
            String footerId = getFooterId();
            int imageWidth = representative.getImageWidth();
            int imageHeight = representative.getImageHeight();
            Dimension origSize = new Dimension(imageWidth, imageHeight);

            List<String> imageScales = DataManager.getInstance().getConfiguration().getImageViewZoomScales();
            String scale = imageScales.get(0);
            if (scale != null) {
                try {
                    Dimension size = calculateImageSize(origSize, scale);
                    if (getCurrentRotate() % 180 == 90) {
                        size = new Dimension(size.height, size.width);
                    }
                    String url = representative.getUrl(size.width, size.height, 0, true, false, null, footerId);
                    imageUrls.add(new ImageLevel(url, size));
                } catch (NumberFormatException e) {
                    logger.error("Cannot parse {} to positive number value", scale);
                }
            }
        }
        return imageUrls;
    }

    public List<ImageLevel> getCurrentImageUrls() throws IndexUnreachableException, DAOException, ConfigurationException {
        int rotation = 0;//gtCurrentRotate();
        PhysicalElement currentImage = getCurrentPage();
        List<ImageLevel> imageUrls = new ArrayList<>();
        if (currentImageOrder != -1 && currentImage != null) {

            getFooterId();

            int imageWidth = currentImage.getImageWidth();
            int imageHeight = currentImage.getImageHeight();

            Dimension origSize = new Dimension(imageWidth, imageHeight);

            List<String> imageScales = DataManager.getInstance().getConfiguration().getImageViewZoomScales();
            for (String scale : imageScales) {
                if (scale != null) {
                    try {
                        Dimension size = calculateImageSize(origSize, scale);
                        if (rotation % 180 == 90) {
                            size = new Dimension(size.height, size.width);
                        }
                        // String url = currentImage.getUrl(size.width, size.height, getCurrentRotate(), true, false, null, footerId);
                        String url = getCurrentImageUrl(size.width, size.height, rotation);
                        imageUrls.add(new ImageLevel(url, size));
                    } catch (NumberFormatException e) {
                        logger.error("Cannot parse {} to positive number value", scale);
                    }
                }
            }
        }
        return imageUrls;
    }

    private static Dimension calculateImageSize(Dimension origSize, String scale) throws NumberFormatException {
        Dimension size = new Dimension(0, 0);
        if (scale.matches("[\\.\\d]+%")) { //percentage value
            float factor = Float.parseFloat(scale.replace("%", "")) / 100f;
            if (factor > 0) {
                size.width = (int) (origSize.width * factor);
                size.height = (int) (origSize.height * factor);
            } else {
                throw new NumberFormatException("Cannot scale image to zero");
            }
        } else if (scale.matches("\\d*x\\d")) {
            int xIndex = scale.indexOf("x");
            String xString = scale.substring(0, xIndex);
            String yString = scale.substring(xIndex + 1);
            if (!xString.trim().isEmpty()) {
                size = scaleToWidth(origSize, Integer.parseInt(xString));
            }
            if (!yString.trim().isEmpty()) {
                size = scaleToHeight(origSize, Integer.parseInt(yString));
            }
        } else if (scale.matches("\\d+")) {
            size = scaleToWidth(origSize, Integer.parseInt(scale));
        } else {
            throw new NumberFormatException("Cannot parse" + scale + " to image scale");
        }
        if (size.width > origSize.width) {
            return origSize;
        }
        return size;
    }

    private String getFooterId() {
        String footerIdField = DataManager.getInstance().getConfiguration().getWatermarkIdField();
        String footerId = null;
        if (footerIdField != null) {
            footerId = topDocument.getMetadataValue(footerIdField);
        }
        return footerId;
    }

    public List<List<String>> getCurrentSearchResultCoords() throws IndexUnreachableException, DAOException {
        List<List<String>> coords = new ArrayList<>();
        List<String> coordStrings = getSearchResultCoords(getCurrentPage());
        if (coordStrings != null) {
            for (String string : coordStrings) {
                coords.add(Arrays.asList(string.split(",")));
            }
        }
        return coords;
    }

    private List<String> getSearchResultCoords(PhysicalElement currentImg) {
        if (currentImg == null) {
            return null;
        }
        List<String> coords = null;
        if (searchBean == null) {
            searchBean = (SearchBean) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("searchBean");
        }
        if (searchBean != null && (searchBean.getCurrentSearchFilterString() == null || searchBean.getCurrentSearchFilterString().equals(
                SearchHelper.SEARCH_FILTER_ALL.getLabel()) || searchBean.getCurrentSearchFilterString().equals("filter_" + SolrConstants.FULLTEXT))) {
            logger.trace("Adding word coords to page {}: {}", currentImg.getOrder(), searchBean.getSearchTerms().toString());
            coords = currentImg.getWordCoords(searchBean.getSearchTerms().get(SolrConstants.FULLTEXT), rotate);
        }
        return coords;
    }

    public int getRepresentativeWidth() throws PresentationException, IndexUnreachableException, DAOException {
        if (getRepresentativePage() != null) {
            return getRepresentativePage().getImageWidth();
        }
        return 0;
    }

    public int getRepresentativeHeight() throws PresentationException, IndexUnreachableException, DAOException {
        if (getRepresentativePage() != null) {
            return getRepresentativePage().getImageHeight();
        }
        return 0;
    }

    public int getCurrentWidth() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            if (rotate % 180 == 90) {
                return currentPage.getImageHeight();
            }
            return currentPage.getImageWidth();
        }
        return 0;
    }

    public int getCurrentHeight() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            if (rotate % 180 == 90) {
                return currentPage.getImageWidth();
            }
            return currentPage.getImageHeight();
        }
        return 0;
    }

    @Deprecated
    public String getCurrentDimensionsUrl() throws IndexUnreachableException, DAOException {
        PhysicalElement currentImg = getCurrentPage();
        if (currentImageOrder != -1 && currentImg != null) {
            String url = currentImg.getDimensionsUrl();
            logger.trace("Url for retrieving image dimensions: {}", url);
            return url;
        }
        return "";
    }

    @Deprecated
    public String getRepresentativePageDimensionsUrl() {
        if (representativePage != null) {
            String url = representativePage.getDimensionsUrl();
            logger.trace("Url for retrieving image dimensions: {}", url);
            return url;
        }
        return "";
    }

    @Deprecated
    public String getRepresentativeImageUrlForOpenLayers() throws IndexUnreachableException, PresentationException, DAOException,
            ConfigurationException {
        return getRepresentativeImageUrl();

    }

    public String getRepresentativeImageUrl() throws IndexUnreachableException, PresentationException, DAOException, ConfigurationException {
        if (getRepresentativePage() != null) {
            Dimension imageSize = new Dimension(representativePage.getImageWidth(), representativePage.getImageHeight());
            int width = representativePage.getImageDefaultWidth(0);
            if (representativePage.getMixWidth() > 0 && width > representativePage.getMixWidth()) {
                width = representativePage.getMixWidth();
            }
            imageSize = scaleToWidth(imageSize, width);
            return representativePage.getUrl(imageSize.width, imageSize.height, 0, true, false, null);
        }
        return null;

    }

    public static Dimension scaleToWidth(Dimension imageSize, int scaledWidth) {
        double scale = scaledWidth / imageSize.getWidth();
        int scaledHeight = (int) (imageSize.getHeight() * scale);
        return new Dimension(scaledWidth, scaledHeight);
    }

    public static Dimension scaleToHeight(Dimension imageSize, int scaledHeight) {
        double scale = scaledHeight / imageSize.getHeight();
        int scaledWidth = (int) (imageSize.getWidth() * scale);
        return new Dimension(scaledWidth, scaledHeight);
    }

    /**
     * Retrieves the current User from the session, if exists.
     *
     * @return The current User; null of not logged in.
     */
    public User getCurrentUser() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        if (request != null) {
            UserBean ub = (UserBean) request.getSession().getAttribute("userBean");
            if (ub != null && ub.getUser() != null) {
                return ub.getUser();
            }
        }
        return null;
    }

    /**
     *
     * @return
     * @should rotate correctly
     */
    public String rotateLeft() {
        rotate -= 90;
        if (rotate < 0) {
            rotate = 360 + rotate;
        }
        if (rotate == -360) {
            rotate = 0;
        }
        logger.trace("rotateLeft: {}", rotate);

        return null;
    }

    /**
     *
     * @return
     * @should rotate correctly
     */
    public String rotateRight() {
        rotate += 90;
        if (rotate == 360) {
            rotate = 0;
        }
        logger.trace("rotateRight: {}", rotate);

        return null;
    }

    /**
     *
     * @return
     * @should reset rotation
     */
    public String resetImage() {
        this.rotate = 0;
        logger.trace("resetImage: {}", rotate);

        return null;
    }

    public boolean isHasPages() throws IndexUnreachableException {
        return pageLoader != null && pageLoader.getNumPages() > 0;
    }

    public boolean isFilesOnly() throws IndexUnreachableException, DAOException {
        // TODO check all files for mime type?
        if (filesOnly == null) {
            if (PhysicalElement.MIME_TYPE_APPLICATION.equals(mainMimeType)) {
                filesOnly = true;
            } else {
                boolean childIsFilesOnly = isChildFilesOnly();
                PhysicalElement firstPage = pageLoader.getPage(pageLoader.getFirstPageOrder());
                filesOnly = childIsFilesOnly || (isHasPages() && firstPage != null && firstPage.getMimeType().equals(
                        PhysicalElement.MIME_TYPE_APPLICATION));
            }

        }

        return filesOnly;
    }

    private boolean isChildFilesOnly() throws IndexUnreachableException {
        boolean childIsFilesOnly = false;
        if (currentDocument != null && (currentDocument.isAnchor() || currentDocument.isGroup())) {
            try {
                String mimeType = currentDocument.getFirstVolumeFieldValue(SolrConstants.MIMETYPE);
                if (PhysicalElement.MIME_TYPE_APPLICATION.equals(mimeType)) {
                    childIsFilesOnly = true;
                }
            } catch (PresentationException e) {
                logger.warn(e.toString());
            }
        }
        return childIsFilesOnly;
    }

    /**
     * Defines the criteria whether to list all remaining volumes in the TOC if the current record is a volume.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isListAllVolumesInTOC() throws IndexUnreachableException, DAOException {
        return DataManager.getInstance().getConfiguration().isTocListSiblingRecords() || isFilesOnly();
    }

    /**
     * Returns all pages in their correct order. Used for e-publications.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public List<PhysicalElement> getAllPages() throws IndexUnreachableException, DAOException {
        List<PhysicalElement> ret = new ArrayList<>();
        if (pageLoader != null) {
            for (int i = pageLoader.getFirstPageOrder(); i <= pageLoader.getLastPageOrder(); ++i) {
                PhysicalElement page = pageLoader.getPage(i);
                ret.add(page);
            }
        }

        return ret;
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public PhysicalElement getCurrentPage() throws IndexUnreachableException, DAOException {
        return getPage(currentImageOrder);
    }

    /**
     * Returns the page with the given order number from the page loader, if exists.
     *
     * @param order
     * @return requested page if exists; null otherwise.
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should return correct page
     * @should return null if order less than zero
     * @should return null if order larger than number of pages
     * @should return null if pageLoader is null
     */
    public PhysicalElement getPage(int order) throws IndexUnreachableException, DAOException {
        if (pageLoader != null && pageLoader.getPage(order) != null) {
            // logger.debug("page " + order + ": " + pageLoader.getPage(order).getFileName());
            return pageLoader.getPage(order);
        }

        return null;
    }

    public PhysicalElement getRepresentativePage() throws PresentationException, IndexUnreachableException, DAOException {
        if (representativePage == null) {
            String thumbnailName = topDocument.getMetadataValue(SolrConstants.THUMBNAIL);
            if (pageLoader != null) {
                if (thumbnailName != null) {
                    representativePage = pageLoader.getPageForFileName(thumbnailName);
                }
                if (representativePage == null) {
                    representativePage = pageLoader.getPage(0);
                }
            }
        }

        return representativePage;
    }

    public PhysicalElement getFirstPage() throws IndexUnreachableException, DAOException {
        return pageLoader.getPage(pageLoader.getFirstPageOrder());
    }

    /**
     * @return the currentImageNo
     */
    public int getCurrentImageNo() {
        return currentImageOrder;
    }

    /**
     * Getter for the paginator or the direct page number input field
     *
     * @return currentImageNo
     */
    public int getCurrentImageNoForPaginator() {
        return getCurrentImageNo();
    }

    /**
     * Setter for the direct page number input field
     *
     * @param currentImageNo
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void setCurrentImageNoForPaginator(int currentImageNo) throws IndexUnreachableException, PresentationException {
        setCurrentImageNo(currentImageNo);
    }

    /**
     * @param currentImageNo the currentImageNo to set
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public void setCurrentImageNo(int currentImageNo) throws IndexUnreachableException, PresentationException {
        logger.debug("setCurrentImageNo: {}", currentImageNo);
        if (pageLoader != null) {
            if (currentImageNo < pageLoader.getFirstPageOrder()) {
                currentImageNo = pageLoader.getFirstPageOrder();
            } else if (currentImageNo >= pageLoader.getLastPageOrder()) {
                currentImageNo = pageLoader.getLastPageOrder();
            }
        }
        this.currentImageOrder = currentImageNo;
        persistentUrl = null;

        if (StringUtils.isEmpty(logId)) {
            Long iddoc = pageLoader.getOwnerIddocForPage(currentImageNo);
            // Set the currentDocumentIddoc to the IDDOC of the image owner document, but only if no specific document LOGID has been requested
            if (iddoc != null && iddoc > -1 && iddoc != currentDocumentIddoc) {
                currentDocumentIddoc = iddoc;
                logger.trace("currentDocumentIddoc: {} ({})", currentDocumentIddoc, pi);
            } else {
                logger.warn("currentDocumentIddoc not found for '{}', page {}", pi, currentImageNo);
            }
        } else {
            // If a specific LOGID has been requested, look up its IDDOC
            logger.trace("Selecting currentElementIddoc by LOGID: {} ({})", logId, pi);
            long iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(getPi(), logId);
            if (iddoc > -1) {
                currentDocumentIddoc = iddoc;
            } else {
                logger.trace("currentElementIddoc not found for '{}', LOGID: {}", pi, logId);
            }
            // Reset LOGID so that the same TOC element doesn't stay highlighted when flipping pages
            logId = null;
        }
    }

    /**
     * Returns the ORDERLABEL value for the current page.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getCurrentImageLabel() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            return currentPage.getOrderLabel().trim();
        }

        return null;
    }

    /**
     * 
     * @return {@link String}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String nextImage() throws IndexUnreachableException, PresentationException {
        //        logger.debug("currentImageNo: {}", currentImageOrder);
        if (currentImageOrder < pageLoader.getLastPageOrder()) {
            setCurrentImageNo(currentImageOrder);
        }
        updateDropdownSelected();
        return null;
    }

    /**
     *
     * @return {@link String}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String prevImage() throws IndexUnreachableException, PresentationException {
        if (currentImageOrder > 0) {
            setCurrentImageNo(currentImageOrder);
        }
        updateDropdownSelected();
        return "";
    }

    /**
     *
     * @return {@link String}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String firstImage() throws IndexUnreachableException, PresentationException {
        setCurrentImageNo(pageLoader.getFirstPageOrder());
        updateDropdownSelected();
        return null;
    }

    /**
     *
     * @return {@link String}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String lastImage() throws IndexUnreachableException, PresentationException {
        setCurrentImageNo(pageLoader.getLastPageOrder());
        updateDropdownSelected();
        return null;
    }

    /**
     *
     * @return {@link Integer}
     * @throws IndexUnreachableException
     */
    public int getImagesCount() throws IndexUnreachableException {
        if (pageLoader == null) {
            return -1;
        }
        return pageLoader.getNumPages();
    }

    /**
     * @param dropdownPages the dropdownPages to set
     */
    public void setDropdownPages(List<SelectItem> dropdownPages) {
        this.dropdownPages = dropdownPages;
    }

    /**
     * @return the dropdownPages
     */
    public List<SelectItem> getDropdownPages() {
        return dropdownPages;
    }

    public void setDropdownFulltext(List<SelectItem> dropdownFulltext) {
        this.dropdownFulltext = dropdownFulltext;
    }

    /**
     * @return the dropdownPages
     */
    public List<SelectItem> getDropdownFulltext() {
        return dropdownFulltext;
    }

    /**
     * @param dropdownSelected the dropdownSelected to set
     */
    public void setDropdownSelected(String dropdownSelected) {
        this.dropdownSelected = dropdownSelected;
        //        logger.debug("dropdownSelected: " + dropdownSelected);
    }

    /**
     * @return the dropdownSelected
     */
    public String getDropdownSelected() {
        return dropdownSelected;
    }

    /**
     *
     * Returns the PhysicalElements for the current thumbnail page using the configured number of thumbnails per page;
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public List<PhysicalElement> getImagesSection() throws IndexUnreachableException, DAOException {
        return getImagesSection(DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage());
    }

    /**
     * Returns the PhysicalElements for the current thumbnail page.
     *
     * @param thumbnailsPerPage Length of the thumbnail list per page.
     * @return PhysicalElements for the current thumbnail page.
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should return correct PhysicalElements for a thumbnail page
     */
    protected List<PhysicalElement> getImagesSection(int thumbnailsPerPage) throws IndexUnreachableException, DAOException {
        List<PhysicalElement> imagesSection = new ArrayList<>();

        if (pageLoader != null) {
            int i = getFirstDisplayedThumbnailIndex(thumbnailsPerPage);
            int end = getLastDisplayedThumbnailIndex(thumbnailsPerPage);
            //        logger.debug(i + " - " + end);
            for (; i < end; i++) {
                if (i > pageLoader.getLastPageOrder()) {
                    break;
                }
                if (pageLoader.getPage(i) != null) {
                    imagesSection.add(pageLoader.getPage(i));
                }
            }
        }

        return imagesSection;
    }

    /**
     * @param thumbnailsPerPage
     * @param i
     * @return
     */
    private int getLastDisplayedThumbnailIndex(int thumbnailsPerPage) {
        int end = getFirstDisplayedThumbnailIndex(thumbnailsPerPage) + thumbnailsPerPage;
        return end;
    }

    /**
     * @param thumbnailsPerPage
     * @return
     */
    private int getFirstDisplayedThumbnailIndex(int thumbnailsPerPage) {
        int i = pageLoader.getFirstPageOrder();
        if (currentThumbnailPage > 1) {
            i = (currentThumbnailPage - 1) * thumbnailsPerPage + 1;
        }
        return i;
    }

    public int getFirstDisplayedThumbnailIndex() {
        return getFirstDisplayedThumbnailIndex(DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage());
    }

    public int getCurrentThumbnailPage() {
        return currentThumbnailPage;
    }

    public void setCurrentThumbnailPage(int currentThumbnailPage) {
        this.currentThumbnailPage = currentThumbnailPage;
    }

    public void nextThumbnailSection() {
        ++currentThumbnailPage;
    }

    public void previousThumbnailSection() {
        --currentThumbnailPage;
    }

    public boolean hasPreviousThumbnailSection() {
        int currentFirstThumbnailIndex = getFirstDisplayedThumbnailIndex();
        int previousLastThumbnailIndex = currentFirstThumbnailIndex - DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
        return previousLastThumbnailIndex >= pageLoader.getFirstPageOrder();
    }

    public boolean hasNextThumbnailSection() {
        int currentFirstThumbnailIndex = getFirstDisplayedThumbnailIndex();
        int previousLastThumbnailIndex = currentFirstThumbnailIndex + DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
        return previousLastThumbnailIndex <= pageLoader.getLastPageOrder();
    }

    public void updateDropdownSelected() {
        setDropdownSelected(String.valueOf(currentImageOrder));
    }

    /**
     * @param event {@link ValueChangeEvent}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws NumberFormatException
     */
    public void dropdownAction(ValueChangeEvent event) throws NumberFormatException, IndexUnreachableException, PresentationException {
        setCurrentImageNo(Integer.valueOf((String) event.getNewValue()) - 1);
    }

    /**
     * @return
     * @throws IndexUnreachableException
     */
    public String getImagesSizeThumbnail() throws IndexUnreachableException {
        if (pageLoader != null) {
            Double im = (double) pageLoader.getNumPages();

            Double thumb = (double) DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
            int answer = new Double(Math.floor(im / thumb)).intValue();
            if (im % thumb != 0 || answer == 0) {
                answer++;
            }
            return String.valueOf(answer);
        }

        return "0";
    }

    /**
     * @return DFG Viewer link
     * @throws IndexUnreachableException
     */
    public String getLinkForDFGViewer() throws IndexUnreachableException {
        if (DataManager.getInstance().getConfiguration().isSidebarDfgLinkVisible() && topDocument != null && StructElementStub.SOURCE_DOC_FORMAT_METS
                .equals(topDocument.getSourceDocFormat()) && isHasPages()) {
            try {
                StringBuilder sbPath = new StringBuilder();
                sbPath.append(DataManager.getInstance().getConfiguration().getViewerDfgViewerUrl());
                sbPath.append(URLEncoder.encode(getMetsResolverUrl(), "utf-8"));
                sbPath.append("&set[image]=").append(currentImageOrder);
                return sbPath.toString();
            } catch (UnsupportedEncodingException e) {
                logger.error("error while encoding url", e);
                return null;
            }
        }

        return null;
    }

    /**
     * @return METS resolver link for the DFG Viewer
     */
    public String getMetsResolverUrl() {
        try {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/metsresolver?id=" + getPi();
        } catch (Exception e) {
            logger.error("Could not get METS resolver URL for {}.", topDocumentIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/metsresolver?id=" + 0;
    }

    public String getLidoResolverUrl() {
        try {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/lidoresolver?id=" + getPi();
        } catch (Exception e) {
            logger.error("Could not get LIDO resolver URL for {}.", topDocumentIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/lidoresolver?id=" + 0;
    }

    /**
     * @return METS resolver URL for the anchor; null if no parent PI found (must be null, otherwise an empty link will be displayed).
     */
    public String getAnchorMetsResolverUrl() {
        if (anchorDocument != null) {
            String parentPi = anchorDocument.getMetadataValue(SolrConstants.PI);
            return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/metsresolver?id=").append(parentPi).toString();
        }

        return null;
    }

    /**
     * @return {@link String}
     * @throws IndexUnreachableException
     */
    public String getPdfDownloadLink() throws IndexUnreachableException {
        // TODO
        return new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=pdf&metsFile=").append(
                getPi()).append(".xml").append("&targetFileName=").append(getPi()).append(".pdf").toString();
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should construct url correctly
     */
    public String getPdfPartDownloadLink() throws IndexUnreachableException, DAOException {
        logger.trace("getPdfPartDownloadLink: {}-{}", firstPdfPage, lastPdfPage);
        if (firstPdfPage > pageLoader.getLastPageOrder()) {
            firstPdfPage = pageLoader.getLastPageOrder();
        }
        if (lastPdfPage > pageLoader.getLastPageOrder()) {
            lastPdfPage = pageLoader.getLastPageOrder();
        }
        if (firstPdfPage < 1) {
            firstPdfPage = 1;
        }
        if (lastPdfPage < firstPdfPage) {
            lastPdfPage = firstPdfPage;
        }

        StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=pdf&images=");
        for (int i = firstPdfPage; i <= lastPdfPage; ++i) {
            PhysicalElement page = pageLoader.getPage(i);
            sb.append(getPi()).append('/').append(page.getFileName()).append('$');
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("&targetFileName=").append(getPi()).append('_').append(firstPdfPage).append('-').append(lastPdfPage).append(".pdf");

        return sb.toString();
    }

    public boolean isPdfPartDownloadLinkEnabled() {
        return firstPdfPage <= lastPdfPage;
    }

    public boolean isAccessPermissionPdf() {
        try {
            if (topDocument == null || !topDocument.isWork() || !isHasPages()) {
                return false;
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return false;
        }
        // Only allow PDF downloads for records coming from METS files
        if (!StructElementStub.SOURCE_DOC_FORMAT_METS.equals(topDocument.getSourceDocFormat())) {
            return false;
        }
        if (accessPermissionPdf == null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            try {
                accessPermissionPdf = SearchHelper.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_DOWNLOAD_PDF,
                        request);
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                return false;
            } catch (DAOException e) {
                logger.debug("DAOException thrown here: {}", e.getMessage());
                return false;
            }
        }

        return accessPermissionPdf;
    }

    /**
     * Indicates whether user comments are allowed for the current record based on several criteria.
     *
     * @return
     */
    public boolean isAllowUserComments() {
        if (!DataManager.getInstance().getConfiguration().isUserCommentsEnabled()) {
            return false;
        }

        if (allowUserComments == null) {
            String query = DataManager.getInstance().getConfiguration().getUserCommentsConditionalQuery();
            try {
                if (StringUtils.isNotEmpty(query) && DataManager.getInstance().getSearchIndex().getHitCount(new StringBuilder(SolrConstants.PI)
                        .append(':').append(pi).append(" AND (").append(query).append(')').toString()) == 0) {
                    allowUserComments = false;
                    logger.trace("User comments are not allowed for this record.");
                } else {
                    allowUserComments = true;
                }
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                return false;
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
                return false;
            }
        }

        return allowUserComments;
    }

    public boolean isDisplayTitleBarPdfLink() {
        return DataManager.getInstance().getConfiguration().isTitlePdfEnabled() && isAccessPermissionPdf();
    }

    public boolean isDisplayMetadataPdfLink() {
        return topDocument != null && topDocument.isWork() && DataManager.getInstance().getConfiguration().isMetadataPdfEnabled()
                && isAccessPermissionPdf();
    }

    public boolean isDisplayPagePdfLink() {
        return DataManager.getInstance().getConfiguration().isPagePdfEnabled() && isAccessPermissionPdf();
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getOaiMarcUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getMarcUrl() + getPi();
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getOaiDcUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getDcUrl() + getPi();
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getOaiEseUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getEseUrl() + getPi();
    }

    /**
     *
     * @return
     */
    public String getOpacUrl() {
        if (currentDocument != null && DataManager.getInstance().getConfiguration().isSidebarOpacLinkVisible() && opacUrl == null) {
            try {
                StructElement topStruct = currentDocument.getTopStruct();
                if (topStruct != null) {
                    opacUrl = topStruct.getMetadataValue(SolrConstants.OPACURL);
                }
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
        }

        return opacUrl;
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getPersistentUrl() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (topDocument != null) {
            String customPURL = topDocument.getMetadataValue("MD_PURL");
            if (StringUtils.isNotEmpty(customPURL)) {
                return customPURL;
            }
        }
        String urn = currentPage != null ? currentPage.getUrn() : null;
        if (urn == null && currentDocument != null) {
            urn = currentDocument.getMetadataValue(SolrConstants.URN);
        }

        return getPersistentUrl(urn);
    }

    /**
     * Returns the PURL for the current page (either via the URN resolver or a pretty URL)
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getPersistentUrl(String urn) throws IndexUnreachableException {
        if (persistentUrl == null) {
            StringBuilder url = new StringBuilder();
            if (StringUtils.isNotEmpty(urn) && !urn.equalsIgnoreCase("NULL")) {
                // URN-based PURL
                if (urn.startsWith("http:") || urn.startsWith("https:")) {
                    // URN is full URL
                    persistentUrl = urn;
                } else {
                    // Just the URN
                    url.append(DataManager.getInstance().getConfiguration().getUrnResolverUrl()).append(urn);
                    persistentUrl = url.toString();
                }
            } else {
                if (isHasPages()) {
                    url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
                    url.append('/').append(PageType.viewImage.getName()).append('/').append(getPi()).append('/').append(currentImageOrder).append(
                            '/');
                } else {
                    url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
                    url.append('/').append(PageType.viewMetadata.getName()).append('/').append(getPi()).append('/').append(currentImageOrder).append(
                            '/');
                }
                persistentUrl = url.toString();
            }
            logger.trace("PURL: {}", persistentUrl);
        }

        return persistentUrl;
    }

    /**
     * Returns the main title of the current volume's anchor, if available.
     *
     * @return
     */
    public String getAnchorTitle() {
        if (anchorDocument != null) {
            return anchorDocument.getMetadataValue(SolrConstants.TITLE);
        }

        return null;
    }

    /**
     * Returns the main title of the current volume.
     *
     * @return The volume's main title.
     */
    public String getVolumeTitle() {
        if (topDocument != null) {
            return topDocument.getMetadataValue(SolrConstants.TITLE);
        }
        return null;
    }

    /**
     * Default fulltext getter (with HTML escaping).
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getFulltext() throws IndexUnreachableException, DAOException {
        return getFulltext(true);
    }

    /**
     *
     * @param escapeHtml If true HTML tags will be escaped to prevent pseudo-HTML from breaking the text.
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getFulltext(boolean escapeHtml) throws IndexUnreachableException, DAOException {
        // Use existing string, if not null (this method gets called 8 times per
        // page load!)
        String currentFulltext = null;

        // logger.debug("getFulltext() START");
        PhysicalElement currentImg = getCurrentPage();
        if (currentImg != null && StringUtils.isNotEmpty(currentImg.getFullText())) {
            // Check permissions first
            boolean access = SearchHelper.checkAccessPermissionByIdentifierAndFileNameWithSessionMap((HttpServletRequest) FacesContext
                    .getCurrentInstance().getExternalContext().getRequest(), getPi(), currentImg.getFileName(), IPrivilegeHolder.PRIV_VIEW_FULLTEXT);
            if (access) {
                currentFulltext = escapeHtml ? Helper.escapeHtmlChars(currentImg.getFullText()) : currentImg.getFullText();
            } else {
                currentFulltext = "ACCESS DENIED";
            }
        }
        // logger.debug("getFulltext() END");

        return currentFulltext;
    }

    public int getCurrentRotate() {
        return rotate;
    }

    public int getCurrentFooterHeight() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            return currentPage.getImageFooterHeight(rotate);
        }
        return 0;
    }

    public void setZoomSlider(int zoomSlider) {
        this.zoomSlider = zoomSlider;
    }

    public int getZoomSlider() {
        return this.zoomSlider;
    }

    /**
     * Returns true if original content download has been enabled in the configuration and there are files in the original content folder for this
     * record.
     *
     * @return
     */
    public boolean isDisplayContentDownloadMenu() {
        try {
            if (DataManager.getInstance().getConfiguration().isOriginalContentDownload() && SearchHelper.checkContentFileAccessPermission(pi,
                    (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())) {

                File sourceFileDir;
                if (StringUtils.isNotEmpty(topDocument.getDataRepository())) {
                    sourceFileDir = new File(DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + File.separator + topDocument
                            .getDataRepository() + File.separator + DataManager.getInstance().getConfiguration().getOrigContentFolder()
                            + File.separator + getPi());
                } else {
                    sourceFileDir = new File(DataManager.getInstance().getConfiguration().getViewerHome() + DataManager.getInstance()
                            .getConfiguration().getOrigContentFolder() + File.separator + getPi());
                }
                if (sourceFileDir.isDirectory() && sourceFileDir.listFiles().length > 0) {
                    return true;
                }
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Returns a list of original content file download links (name+url) for the current document.
     *
     * @return
     * @throws IndexUnreachableException
     */
    public List<LabeledLink> getContentDownloadLinksForWork() throws IndexUnreachableException {
        List<LabeledLink> ret = new ArrayList<>();

        File sourceFileDir;
        if (StringUtils.isNotEmpty(topDocument.getDataRepository())) {
            sourceFileDir = new File(new StringBuilder(DataManager.getInstance().getConfiguration().getDataRepositoriesHome()).append(File.separator)
                    .append(topDocument.getDataRepository()).append(File.separator).append(DataManager.getInstance().getConfiguration()
                            .getOrigContentFolder()).append(File.separator + getPi()).toString());
        } else {
            sourceFileDir = new File(new StringBuilder(DataManager.getInstance().getConfiguration().getViewerHome()).append(DataManager.getInstance()
                    .getConfiguration().getOrigContentFolder()).append(File.separator).append(getPi()).toString());

        }
        if (sourceFileDir.isDirectory()) {
            try {
                File[] sourcesArray = sourceFileDir.listFiles();
                if (sourcesArray != null && sourcesArray.length > 0) {
                    List<File> sourcesList = Arrays.asList(sourcesArray);
                    Collections.sort(sourcesList, filenameComparator);
                    for (File file : sourcesList) {
                        if (file.isFile()) {
                            String url = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/file?pi=" + getPi() + "&file=" + URLEncoder.encode(
                                    file.getName(), Helper.DEFAULT_ENCODING);
                            ret.add(new LabeledLink(file.getName(), url, 0));
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return ret;
    }

    Comparator<File> filenameComparator = new Comparator<File>() {
        AlphanumCollatorComparator comparator = new AlphanumCollatorComparator(null);

        @Override
        public int compare(File f1, File f2) {
            return comparator.compare(f1.getName(), f2.getName());
            //            return f1.getName().compareTo(f2.getName());
        }

    };

    /**
     * Returns a list of original content file download links (name+url) for the current page. CURRENTLY NOT SUPPORTED
     *
     * @return
     * @throws IndexUnreachableException
     */
    public List<LabeledLink> getContentDownloadLinksForPage() throws IndexUnreachableException {
        List<LabeledLink> ret = new ArrayList<>();

        String page = String.valueOf(currentImageOrder);
        File sourceFileDir;
        if (StringUtils.isNotEmpty(topDocument.getDataRepository())) {
            sourceFileDir = new File(DataManager.getInstance().getConfiguration().getDataRepositoriesHome() + File.separator + topDocument
                    .getDataRepository() + File.separator + DataManager.getInstance().getConfiguration().getOrigContentFolder() + File.separator
                    + getPi() + File.separator + page);
        } else {
            sourceFileDir = new File(DataManager.getInstance().getConfiguration().getViewerHome() + DataManager.getInstance().getConfiguration()
                    .getOrigContentFolder() + File.separator + getPi() + File.separator + page);

        }
        if (sourceFileDir.isDirectory()) {
            try {
                for (File file : sourceFileDir.listFiles()) {
                    if (file.isFile()) {
                        String url = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/file?pi=").append(getPi())
                                .append("&page=").append(page).append("&file=").append(URLEncoder.encode(file.getName(), Helper.DEFAULT_ENCODING))
                                .toString();
                        ret.add(new LabeledLink(file.getName(), url, 0));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return ret;
    }

    /**
     * @return the topDocumentIddoc
     */
    public long getTopDocumentIddoc() {
        return topDocumentIddoc;
    }

    /**
     * @param topDocumentIddoc the topDocumentIddoc to set
     */
    public void setTopDocumentIddoc(long topDocumentIddoc) {
        this.topDocumentIddoc = topDocumentIddoc;
    }

    /**
     * Returns <code>topDocument</code>. If the IDDOC of <code>topDocument</code> is different from <code>topDocumentIddoc</code>,
     * <code>topDocument</code> is reloaded.
     *
     * @return the currentDocument
     * @throws IndexUnreachableException
     */
    public StructElement getActiveDocument() throws IndexUnreachableException {
        if (topDocument == null || topDocument.getLuceneId() != topDocumentIddoc) {
            topDocument = new StructElement(topDocumentIddoc, null);
        }

        return topDocument;
    }

    /**
     * @param currentDocument the currentDocument to set
     */
    public void setActiveDocument(StructElement currentDocument) {
        this.topDocument = currentDocument;
    }

    /**
     * @return the currentDocumentIddoc
     */
    public long getCurrentDocumentIddoc() {
        return currentDocumentIddoc;
    }

    /**
     * @param currentDocumentIddoc the currentDocumentIddoc to set
     */
    public void setCurrentDocumentIddoc(long currentDocumentIddoc) {
        this.currentDocumentIddoc = currentDocumentIddoc;
    }

    /**
     * @return the currentDocument
     * @throws IndexUnreachableException
     */
    public StructElement getCurrentDocument() throws IndexUnreachableException {
        if (currentDocument == null || currentDocument.getLuceneId() != currentDocumentIddoc) {
            logger.trace("Creating new currentDocument from IDDOC {}, old currentDocumentIddoc: {}", currentDocumentIddoc, currentDocument
                    .getLuceneId());
            currentDocument = new StructElement(currentDocumentIddoc);
        }
        return currentDocument;
    }

    public StructElement getTopDocument() {
        return topDocument;
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public List<StructElementStub> getCurrentDocumentHierarchy() throws IndexUnreachableException {
        if (docHierarchy == null) {
            //            PageType pageType = PageType.viewImage;
            docHierarchy = new LinkedList<>();

            StructElement curDoc = getCurrentDocument();
            while (curDoc != null) {
                docHierarchy.add(curDoc.createStub());
                curDoc = curDoc.getParent();
            }
            Collections.reverse(docHierarchy);
        }

        logger.trace("docHierarchy size: {}", docHierarchy.size());
        if (!DataManager.getInstance().getConfiguration().getIncludeAnchorInTitleBreadcrumbs() && !docHierarchy.isEmpty()) {
            return docHierarchy.subList(1, docHierarchy.size());
        }
        return docHierarchy;
    }

    /**
     * @param currentDocument the currentDocument to set
     */
    public void setCurrentDocument(StructElement currentDocument) {
        this.currentDocument = currentDocument;
    }

    /**
     * @return the logId
     */
    public String getLogId() {
        return logId;
    }

    /**
     * @param logId the logId to set
     */
    public void setLogId(String logId) {
        this.logId = logId;
        // Reset the hieararchy list so that a new one is created
        docHierarchy = null;
    }

    /**
     * @return the pageLoader
     */
    public IPageLoader getPageLoader() {
        return pageLoader;

    }

    /**
     * generates DC meta Tags for the head of a HTML page
     */

    public String getHtmlHeadDCMetadata() {
        StringBuilder result = new StringBuilder(100);

        String title = "-";
        String creators = "-";
        String publisher = "-";
        String yearpublish = "-";
        String placepublish = "-";
        String date = null;
        String identifier = null;
        String rights = null;

        if (this.topDocument.getMetadataValue("MD_TITLE") != null) {
            title = topDocument.getMetadataValues("MD_TITLE").iterator().next();
            result.append("\r<meta name=\"DC.title\" content=\"").append(title).append("\">");
        }

        if (this.topDocument.getMetadataValue("MD_CREATOR") != null) {
            for (Object fieldValue : topDocument.getMetadataValues("MD_CREATOR")) {
                String value = (String) fieldValue;
                if (StringUtils.isEmpty(creators)) {
                    creators = value;
                } else {
                    creators = new StringBuilder(creators).append(", ").append(value).toString();
                }
            }
            result.append("\r<meta name=\"DC.creator\" content=\"").append(creators).append("\">");
        }

        if (this.topDocument.getMetadataValue("MD_PUBLISHER") != null) {
            publisher = topDocument.getMetadataValue("MD_PUBLISHER");
            result.append("\r<meta name=\"DC.publisher\" content=\"").append(publisher).append("\">");
        }

        if (this.topDocument.getMetadataValue("MD_YEARPUBLISH") != null) {
            date = topDocument.getMetadataValue("MD_YEARPUBLISH");
            result.append("\r<meta name=\"DC.date\" content=\"").append(date).append("\">");
        }

        if (this.topDocument.getMetadataValue(SolrConstants.URN) != null) {
            identifier = this.topDocument.getMetadataValue(SolrConstants.URN);
            result.append("\r<meta name=\"DC.identifier\" content=\"").append(identifier).append("\">");
        }

        String sourceString = new StringBuilder(creators).append(": ").append(title).append(", ").append(placepublish).append(": ").append(publisher)
                .append(' ').append(yearpublish).append('.').toString();

        result.append("\r<meta name=\"DC.source\" content=\"").append(sourceString).append("\">");

        if (topDocument.getMetadataValue(SolrConstants.ACCESSCONDITION) != null) {
            rights = this.topDocument.getMetadataValue(SolrConstants.ACCESSCONDITION);
            if (!SolrConstants.OPEN_ACCESS_VALUE.equals(rights)) {
                result.append("\r<meta name=\"DC.rights\" content=\"").append(rights).append("\">");
            }
        }

        return result.toString();
    }

    public boolean isHasVersionHistory() throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField()) && StringUtils.isEmpty(DataManager
                .getInstance().getConfiguration().getNextVersionIdentifierField())) {
            return false;
        }

        return getVersionHistory().size() > 1;
    }

    /**
     * 
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should create create history correctly
     */
    @SuppressWarnings("unchecked")
    public List<String> getVersionHistory() throws PresentationException, IndexUnreachableException {
        if (versionHistory == null) {
            versionHistory = new ArrayList<>();

            {
                String nextVersionIdentifierField = DataManager.getInstance().getConfiguration().getNextVersionIdentifierField();
                if (StringUtils.isNotEmpty(nextVersionIdentifierField)) {
                    List<String> next = new ArrayList<>();
                    String identifier = topDocument.getMetadataValue(nextVersionIdentifierField);
                    while (identifier != null) {
                        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + identifier, null);
                        if (doc != null) {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("id", identifier);
                            if (doc.getFieldValues("MD_YEARPUBLISH") != null) {
                                jsonObj.put("year", doc.getFieldValues("MD_YEARPUBLISH").iterator().next());
                            }
                            jsonObj.put("order", "1"); // "1" means this is a
                                                       // succeeding version
                            next.add(jsonObj.toJSONString());
                            identifier = null;
                            if (doc.getFieldValues(nextVersionIdentifierField) != null) {
                                identifier = (String) doc.getFieldValues(nextVersionIdentifierField).iterator().next();
                            }
                        }
                    }
                    Collections.reverse(next);
                    versionHistory.addAll(next);
                }
            }

            {
                // This version
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("id", getPi());
                jsonObj.put("year", topDocument.getMetadataValue("MD_YEARPUBLISH"));
                jsonObj.put("order", "0"); // "0" identifies the currently
                                           // loaded version
                versionHistory.add(jsonObj.toJSONString());
            }

            {
                String prevVersionIdentifierField = DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField();
                if (StringUtils.isNotEmpty(prevVersionIdentifierField)) {
                    List<String> previous = new ArrayList<>();
                    String identifier = topDocument.getMetadataValue(prevVersionIdentifierField);
                    while (identifier != null) {
                        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + identifier, null);
                        if (doc != null) {
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("id", identifier);
                            if (doc.getFieldValues("MD_YEARPUBLISH") != null) {
                                jsonObj.put("year", doc.getFieldValues("MD_YEARPUBLISH").iterator().next());
                            }
                            jsonObj.put("order", "-1"); // "-1" means this is a
                                                        // preceding version
                            previous.add(jsonObj.toJSONString());
                            identifier = null;
                            if (doc.getFieldValues(prevVersionIdentifierField) != null) {
                                identifier = (String) doc.getFieldValues(prevVersionIdentifierField).iterator().next();
                            }
                        }
                    }
                    versionHistory.addAll(previous);
                }
            }
            // Collections.reverse(versionHistory);
        }

        //		logger.trace("Version history size: {}", versionHistory.size());
        return versionHistory;
    }

    /**
     * Returns the ContextObject value for a COinS element (generated using metadata from <code>currentDocument</code>).
     *
     * @return
     */
    public String getContextObject() {
        if (currentDocument != null && contextObject == null) {
            try {
                contextObject = currentDocument.generateContextObject(BeanUtils.getNavigationHelper().getCurrentUrl(), currentDocument
                        .getTopStruct());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
        }

        return contextObject;
    }

    /**
     * 
     * @param login If true, the user will first be logged into their Transkribus account in the UserBean.
     * @return
     */
    public String addToTranskribusAction(boolean login) {
        logger.trace("addToTranskribusAction");
        UserBean ub = BeanUtils.getUserBean();
        if (ub == null) {
            logger.error("Could not retrieve UserBean");
            Messages.error("transkribus_recordInjestError");
            return "";
        }

        TranskribusSession session = ub.getUser().getTranskribusSession();
        if (session == null && login) {
            ub.transkribusLoginAction();
            session = ub.getUser().getTranskribusSession();
        }
        if (session == null) {
            Messages.error("transkribus_recordInjestError");
            return "";
        }
        try {
            String resolverUrlRoot = "http://viewer-demo01.intranda.com/viewer/metsresolver?id="; // TODO
            TranskribusJob job = TranskribusUtils.ingestRecord(DataManager.getInstance().getConfiguration().getTranskribusRestApiUrl(), session, pi,
                    resolverUrlRoot);
            if (job == null) {
                Messages.error("transkribus_recordInjestError");
                return "";
            }
            Messages.info("transkribus_recordIngestSuccess");
        } catch (IOException | JDOMException | ParseException e) {
            logger.error(e.getMessage(), e);
            Messages.error("transkribus_recordInjestError");
        } catch (DAOException e) {
            logger.debug("DAOException thrown here");
            logger.error(e.getMessage(), e);
            Messages.error("transkribus_recordInjestError");
        } catch (HTTPException e) {
            if (e.getCode() == 401) {
                ub.getUser().setTranskribusSession(null);
                Messages.error("transkribus_sessionExpired");
            } else {
                logger.error(e.getMessage(), e);
                Messages.error("transkribus_recordInjestError");
            }
        }

        return "";
    }

    /**
     * 
     * @param session
     * @return
     * @throws DAOException
     */
    public boolean isRecordAddedToTranskribus(TranskribusSession session) throws DAOException {
        if (session == null) {
            return false;
        }
        List<TranskribusJob> jobs = DataManager.getInstance().getDao().getTranskribusJobs(pi, session.getUserId(), null);
        return jobs != null && !jobs.isEmpty();
    }

    public boolean useTiles() throws IndexUnreachableException, DAOException, ConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return false;
        }
        return DataManager.getInstance().getConfiguration().useTiles() && currentPage.isTilesExist();
    }

    public boolean useTilesFullscreen() throws IndexUnreachableException, DAOException, ConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return false;
        }

        return DataManager.getInstance().getConfiguration().useTilesFullscreen() && currentPage.isTilesExist();
    }

    /**
     * @return the displayImage
     */
    public boolean isDisplayImage() {
        return displayImage;
    }

    /**
     * @param displayImage the displayImage to set
     */
    public void setDisplayImage(boolean displayImage) {
        this.displayImage = displayImage;
    }

    public String getTitleBarLabel() {
        if (topDocument != null && StringUtils.isNotEmpty(topDocument.getLabel())) {
            return topDocument.getLabel();
        }

        return null;
    }

    /**
     * @return the pi
     * @throws IndexUnreachableException
     */
    public String getPi() throws IndexUnreachableException {
        if (StringUtils.isEmpty(pi)) {
            pi = getCurrentDocument().getMetadataValue(SolrConstants.PI_TOPSTRUCT);
        }

        return pi;
    }

    /**
     * If the current record is a volume, returns the PI of the anchor record.
     *
     * @return anchor PI if record is volume; null otherwise.
     */
    public String getAnchorPi() {
        if (anchorDocument != null) {
            return anchorDocument.getMetadataValue(SolrConstants.PI);
        }

        return null;
    }

    /**
     * @return the mainMimeType
     */
    public String getMainMimeType() {
        return mainMimeType;
    }

    public void togglePageOrientation() {
        this.firstPageOrientation = this.firstPageOrientation.opposite();
    }

    /**
     * @param doublePageMode the doublePageMode to set
     */
    public void setDoublePageMode(boolean doublePageMode) {
        this.doublePageMode = doublePageMode;
    }

    /**
     * @return the doublePageMode
     */
    public boolean isDoublePageMode() {
        return doublePageMode;
    }

    /**
     * @return the firstPdfPage
     */
    public String getFirstPdfPage() {
        return String.valueOf(firstPdfPage);
    }

    /**
     * @param firstPdfPage the firstPdfPage to set
     */
    public void setFirstPdfPage(String firstPdfPage) {
        this.firstPdfPage = Integer.valueOf(firstPdfPage);
    }

    /**
     * @return the lastPdfPage
     */
    public String getLastPdfPage() {
        return String.valueOf(lastPdfPage);
    }

    /**
     * @param lastPdfPage the lastPdfPage to set
     */
    public void setLastPdfPage(String lastPdfPage) {
        logger.trace("setLastPdfPage: {}", lastPdfPage);
        if (lastPdfPage != null) {
            this.lastPdfPage = Integer.valueOf(lastPdfPage);
        }
    }

    /**
     * @return the calendarView
     */
    public CalendarView getCalendarView() {
        return calendarView;
    }
}
