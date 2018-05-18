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
import java.io.FileNotFoundException;
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
import java.util.Optional;

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
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.controller.TranskribusUtils;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.ImageDeliveryBean;
import de.intranda.digiverso.presentation.managedbeans.SearchBean;
import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.calendar.CalendarView;
import de.intranda.digiverso.presentation.model.metadata.MetadataTools;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusJob;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusSession;
import de.intranda.digiverso.presentation.model.viewer.pageloader.IPageLoader;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;

/**
 * Holds information about the currently open record (structure, pages, etc.). Used to reduced the size of ActiveDocumentBean.
 */
public class ViewManager implements Serializable {

    private static final long serialVersionUID = -7776362205876306849L;

    private static final Logger logger = LoggerFactory.getLogger(ViewManager.class);

    private ImageDeliveryBean imageDelivery;

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
    private final List<SelectItem> dropdownPages = new ArrayList<>();
    private final List<SelectItem> dropdownFulltext = new ArrayList<>();
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
    private PageOrientation firstPageOrientation = PageOrientation.right;
    private boolean doublePageMode = false;
    private int firstPdfPage;
    private int lastPdfPage;
    private CalendarView calendarView;
    private Boolean belowFulltextThreshold = null;

    /**
     * 
     * @param topDocument
     * @param pageLoader
     * @param currentDocumentIddoc
     * @param logId
     * @param mainMimeType
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public ViewManager(StructElement topDocument, IPageLoader pageLoader, long currentDocumentIddoc, String logId, String mainMimeType,
            ImageDeliveryBean imageDelivery) throws IndexUnreachableException, PresentationException {
        this.imageDelivery = imageDelivery;
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
                pageLoader.generateSelectItems(dropdownPages, dropdownFulltext, BeanUtils.getServletPathWithHostAsUrlFromJsfContext(),
                        isBelowFulltextThreshold());
            }
        }
        this.mainMimeType = mainMimeType;
        logger.trace("mainMimeType: {}", mainMimeType);

    }

    public CalendarView createCalendarView() throws IndexUnreachableException, PresentationException {
        // Init calendar view
        String anchorPi = anchorDocument != null ? anchorDocument.getPi() : (topDocument.isAnchor() ? pi : null);
        return new CalendarView(pi, anchorPi, topDocument.isAnchor() ? null : topDocument.getMetadataValue(SolrConstants._CALENDAR_YEAR));

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
        return getCurrentImageInfo(BeanUtils.getNavigationHelper().getCurrentPagerType());
    }

    public String getCurrentImageInfo(PageType pageType) throws IndexUnreachableException, DAOException {
        StringBuilder urlBuilder = new StringBuilder();
        if (isDoublePageMode()) {
            urlBuilder.append("[");
            String imageInfoLeft = getCurrentLeftPage().map(page -> getImageInfo(page, pageType)).orElse(null);
            String imageInfoRight = getCurrentRightPage().map(page -> getImageInfo(page, pageType)).orElse(null);
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
            urlBuilder.append(getImageInfo(getCurrentPage(), pageType));
        }
        return urlBuilder.toString();
    }

    /**
     * @param currentPage
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    private Optional<PhysicalElement> getCurrentLeftPage() throws IndexUnreachableException, DAOException {
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
    private Optional<PhysicalElement> getCurrentRightPage() throws IndexUnreachableException, DAOException {
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
        return imageDelivery.getImages().getImageUrl(page);
    }

    private String getImageInfo(PhysicalElement page, PageType pageType) {
        return imageDelivery.getImages().getImageUrl(page, pageType);
    }

    public String getCurrentImageInfoFullscreen() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }
        String url = getImageInfo(currentPage, PageType.viewFullscreen);
        return url;
    }

    public String getCurrentImageInfoCrowd() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }
        String url = getImageInfo(currentPage, PageType.editOcr);
        return url;
    }

    public String getWatermarkUrl() throws IndexUnreachableException, DAOException, ConfigurationException {
        return getWatermarkUrl("viewImage");
    }

    public String getWatermarkUrl(String pageType) throws IndexUnreachableException, DAOException, ConfigurationException {
        return imageDelivery.getFooter()
                .getWatermarkUrl(Optional.ofNullable(getCurrentPage()), Optional.ofNullable(getTopDocument()),
                        Optional.ofNullable(PageType.getByName(pageType)))
                .orElse("");

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

    public String getCurrentImageUrl() throws ConfigurationException, IndexUnreachableException, DAOException {
        return getCurrentImageUrl(PageType.viewImage);
    }

    /**
     * @return the iiif url to the image in a configured size
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ConfigurationException
     */
    public String getCurrentImageUrl(PageType view) throws IndexUnreachableException, DAOException, ConfigurationException {

        int size = DataManager.getInstance()
                .getConfiguration()
                .getImageViewZoomScales(view, getCurrentPage().getImageType())
                .stream()
                .mapToInt(string -> Integer.parseInt(string))
                .max()
                .orElse(800);
        return getCurrentImageUrl(view, size);
    }

    public String getCurrentImageUrl(int size) throws IndexUnreachableException, DAOException {
        return getCurrentImageUrl(PageType.viewImage, size);
    }

    /**
     * @param view
     * @param size
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    private String getCurrentImageUrl(PageType view, int size) throws IndexUnreachableException, DAOException {
        ImageFileFormat format = ImageFileFormat.JPG;
        if (ImageFileFormat.PNG.equals(getCurrentPage().getImageType().getFormat())) {
            format = ImageFileFormat.PNG;
        }
        return imageDelivery.getThumbs().getThumbnailUrl(getCurrentPage(), size, size);
        //        return new IIIFUrlHandler().getIIIFImageUrl(DataManager.getInstance().getConfiguration().getIiifUrl() + "image/" + pi + "/" + getCurrentPage().getFileName(), RegionRequest.FULL,
        //                new Scale.ScaleToWidth(size), Rotation.NONE, Colortype.DEFAULT, format);
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
        SearchBean searchBean = BeanUtils.getSearchBean();
        if (searchBean != null && (searchBean.getCurrentSearchFilterString() == null
                || searchBean.getCurrentSearchFilterString().equals(SearchHelper.SEARCH_FILTER_ALL.getLabel())
                || searchBean.getCurrentSearchFilterString().equals("filter_" + SolrConstants.FULLTEXT))) {
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

    public String getRepresentativeImageUrl() throws IndexUnreachableException, PresentationException, DAOException, ConfigurationException {

        if (getRepresentativePage() != null) {
            Dimension imageSize = new Dimension(representativePage.getImageWidth(), representativePage.getImageHeight());
            return imageDelivery.getThumbs().getThumbnailUrl(representativePage);
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
            UserBean ub = BeanUtils.getUserBean();
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
                filesOnly = childIsFilesOnly
                        || (isHasPages() && firstPage != null && firstPage.getMimeType().equals(PhysicalElement.MIME_TYPE_APPLICATION));
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
                if (page != null) {
                    ret.add(page);
                }
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
        return getPage(currentImageOrder).orElse(null);
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
    public Optional<PhysicalElement> getPage(int order) throws IndexUnreachableException, DAOException {
        if (pageLoader != null && pageLoader.getPage(order) != null) {
            // logger.debug("page " + order + ": " + pageLoader.getPage(order).getFileName());
            return Optional.ofNullable(pageLoader.getPage(order));
        }

        return Optional.empty();
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

    public boolean isMultiPageRecord() throws IndexUnreachableException {
        return getImagesCount() > 1;
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
     * @return the dropdownPages
     */
    public List<SelectItem> getDropdownPages() {
        return dropdownPages;
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
        if (topDocument != null && StructElementStub.SOURCE_DOC_FORMAT_METS.equals(topDocument.getSourceDocFormat()) && isHasPages()) {
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
     * Returns the pdf download link for the current document
     * 
     * @return {@link String}
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getPdfDownloadLink() throws IndexUnreachableException, PresentationException {
        return imageDelivery.getPdf().getPdfUrl(getTopDocument(), "");
    }

    /**
     * Returns the pdf download link for the current page
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getPdfPageDownloadLink() throws IndexUnreachableException, DAOException {
        return imageDelivery.getPdf().getPdfUrl(getTopDocument(), getCurrentPage());
    }

    /**
     * Returns the pdf download link for a pdf of all pages from this.firstPdfPage to this.lastPdfPage (inclusively)
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

        //        StringBuilder sb = new StringBuilder(DataManager.getInstance().getConfiguration().getContentServerWrapperUrl()).append("?action=pdf&images=");
        List<PhysicalElement> pages = new ArrayList<>();
        for (int i = firstPdfPage; i <= lastPdfPage; ++i) {
            PhysicalElement page = pageLoader.getPage(i);
            pages.add(page);
            //            sb.append(getPi()).append('/').append(page.getFileName()).append('$');
        }
        PhysicalElement[] pageArr = new PhysicalElement[pages.size()];
        return imageDelivery.getPdf().getPdfUrl(getActiveDocument(), pages.toArray(pageArr));
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
                accessPermissionPdf =
                        AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_DOWNLOAD_PDF, request);
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
                if (StringUtils.isNotEmpty(query) && DataManager.getInstance().getSearchIndex().getHitCount(
                        new StringBuilder(SolrConstants.PI).append(':').append(pi).append(" AND (").append(query).append(')').toString()) == 0) {
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
        if (currentDocument != null && opacUrl == null) {
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
     * @return PURL for the current page
     * @throws IndexUnreachableException
     * @should generate purl via urn correctly
     * @should generate purl without urn correctly
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
                // Prefer configured target page type for the docstruct type
                PageType pageType = null;
                if (topDocument != null) {
                    pageType = PageType.getPageTypeForDocStructType(topDocument.getDocStructType());
                }
                if (pageType == null) {
                    if (isHasPages()) {
                        pageType = PageType.viewImage;
                    } else {
                        pageType = PageType.viewMetadata;
                    }
                }
                url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
                url.append('/').append(pageType.getName()).append('/').append(getPi()).append('/').append(currentImageOrder).append('/');
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
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public boolean isBelowFulltextThreshold() throws PresentationException, IndexUnreachableException {
        if (belowFulltextThreshold == null) {

            long pagesWithFulltext = DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                            .append(':')
                            .append(pi)
                            .append(" +")
                            .append(SolrConstants.DOCTYPE)
                            .append(":PAGE")
                            .append(" +")
                            .append(SolrConstants.FULLTEXTAVAILABLE)
                            .append(":true")
                            .toString());
            int threshold = DataManager.getInstance().getConfiguration().getFulltextPercentageWarningThreshold();
            double percentage = pagesWithFulltext * 100.0 / pageLoader.getNumPages();
            logger.trace("{}% of pages have full-text", percentage);
            if (percentage < threshold) {
                belowFulltextThreshold = true;
            } else {
                belowFulltextThreshold = false;
            }
        }

        return belowFulltextThreshold;
    }

    /**
     * Default fulltext getter (with HTML escaping).
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String getFulltext() throws IndexUnreachableException, DAOException {
        return getFulltext(true, null);
    }

    /**
     * Returns the full-text for the current page, stripped of any included JavaScript.
     * 
     * @param escapeHtml If true HTML tags will be escaped to prevent pseudo-HTML from breaking the text.
     * @param language
     * @return Full-text for the current page.
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws IOException
     * @throws FileNotFoundException
     */
    public String getFulltext(boolean escapeHtml, String language) throws IndexUnreachableException, DAOException {
        String currentFulltext = null;

        // Current page fulltext
        PhysicalElement currentImg = getCurrentPage();
        if (currentImg != null && StringUtils.isNotEmpty(currentImg.getFullText())) {
            currentFulltext = Helper.stripJS(currentImg.getFullText());
            if (currentFulltext.length() < currentImg.getFullText().length()) {
                logger.warn("JavaScript found and removed from full-text in {}, page {}", pi, currentImg.getOrder());
            }
            if (escapeHtml) {
                currentFulltext = Helper.escapeHtmlChars(currentImg.getFullText());
            }

        }

        // logger.trace(currentFulltext);
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
            if (DataManager.getInstance().getConfiguration().isOriginalContentDownload() && AccessConditionUtils.checkContentFileAccessPermission(pi,
                    (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest())) {

                File sourceFileDir;
                if (StringUtils.isNotEmpty(topDocument.getDataRepository())) {
                    String dataRepositoriesHome = DataManager.getInstance().getConfiguration().getDataRepositoriesHome();
                    StringBuilder sbFilePath = new StringBuilder();
                    if (StringUtils.isNotEmpty(dataRepositoriesHome)) {
                        sbFilePath.append(dataRepositoriesHome).append(File.separator);
                    }
                    sbFilePath.append(topDocument.getDataRepository())
                            .append(File.separator)
                            .append(DataManager.getInstance().getConfiguration().getOrigContentFolder())
                            .append(File.separator)
                            .append(getPi());
                    sourceFileDir = new File(sbFilePath.toString());
                } else {
                    sourceFileDir = new File(new StringBuilder().append(DataManager.getInstance().getConfiguration().getViewerHome())
                            .append(DataManager.getInstance().getConfiguration().getOrigContentFolder())
                            .append(File.separator)
                            .append(getPi())
                            .toString());
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
            String dataRepositoriesHome = DataManager.getInstance().getConfiguration().getDataRepositoriesHome();
            StringBuilder sbFilePath = new StringBuilder();
            if (StringUtils.isNotEmpty(dataRepositoriesHome)) {
                sbFilePath.append(dataRepositoriesHome).append(File.separator);
            }
            sbFilePath.append(topDocument.getDataRepository())
                    .append(File.separator)
                    .append(DataManager.getInstance().getConfiguration().getOrigContentFolder())
                    .append(File.separator)
                    .append(getPi());
            sourceFileDir = new File(sbFilePath.toString());
        } else {
            sourceFileDir = new File(new StringBuilder(DataManager.getInstance().getConfiguration().getViewerHome())
                    .append(DataManager.getInstance().getConfiguration().getOrigContentFolder())
                    .append(File.separator)
                    .append(getPi())
                    .toString());

        }
        if (sourceFileDir.isDirectory()) {
            try {
                File[] sourcesArray = sourceFileDir.listFiles();
                if (sourcesArray != null && sourcesArray.length > 0) {
                    List<File> sourcesList = Arrays.asList(sourcesArray);
                    Collections.sort(sourcesList, filenameComparator);
                    for (File file : sourcesList) {
                        if (file.isFile()) {
                            String url = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/file?pi=" + getPi() + "&file="
                                    + URLEncoder.encode(file.getName(), Helper.DEFAULT_ENCODING);
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
            String dataRepositoriesHome = DataManager.getInstance().getConfiguration().getDataRepositoriesHome();
            StringBuilder sbFilePath = new StringBuilder();
            if (StringUtils.isNotEmpty(dataRepositoriesHome)) {
                sbFilePath.append(dataRepositoriesHome).append(File.separator);
            }
            sbFilePath.append(topDocument.getDataRepository())
                    .append(File.separator)
                    .append(DataManager.getInstance().getConfiguration().getOrigContentFolder())
                    .append(File.separator)
                    .append(getPi())
                    .append(File.separator)
                    .append(page);
            sourceFileDir = new File(sbFilePath.toString());
        } else {
            sourceFileDir = new File(new StringBuilder(DataManager.getInstance().getConfiguration().getViewerHome())
                    .append(DataManager.getInstance().getConfiguration().getOrigContentFolder())
                    .append(File.separator)
                    .append(getPi())
                    .append(File.separator)
                    .append(page)
                    .toString());

        }
        if (sourceFileDir.isDirectory()) {
            try {
                for (File file : sourceFileDir.listFiles()) {
                    if (file.isFile()) {
                        String url = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/file?pi=")
                                .append(getPi())
                                .append("&page=")
                                .append(page)
                                .append("&file=")
                                .append(URLEncoder.encode(file.getName(), Helper.DEFAULT_ENCODING))
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
            logger.trace("Creating new currentDocument from IDDOC {}, old currentDocumentIddoc: {}", currentDocumentIddoc,
                    currentDocument.getLuceneId());
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

    @Deprecated
    public String getHtmlHeadDCMetadata() {
        return getDublinCoreMetaTags();
    }

    /**
     * Generates DC meta tags for the head of a HTML page.
     * 
     * @return String with tags
     */
    public String getDublinCoreMetaTags() {
        return MetadataTools.generateDublinCoreMetaTags(this.topDocument);
    }

    /**
     * 
     * @return String with tags
     */
    public String getHighwirePressMetaTags() {
        try {
            return MetadataTools.generateHighwirePressMetaTags(this.topDocument, isFilesOnly() ? getAllPages() : null);
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
            return "";
        } catch (ConfigurationException e) {
            logger.error(e.getMessage(), e);
            return "";
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
            return "";
        }
    }

    public boolean isHasVersionHistory() throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField())
                && StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getNextVersionIdentifierField())) {
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

            String versionLabelField = DataManager.getInstance().getConfiguration().getVersionLabelField();

            {
                String nextVersionIdentifierField = DataManager.getInstance().getConfiguration().getNextVersionIdentifierField();
                if (StringUtils.isNotEmpty(nextVersionIdentifierField)) {
                    List<String> next = new ArrayList<>();
                    String identifier = topDocument.getMetadataValue(nextVersionIdentifierField);
                    while (identifier != null) {
                        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + identifier, null);
                        if (doc != null) {
                            JSONObject jsonObj = new JSONObject();
                            String versionLabel =
                                    versionLabelField != null ? SolrSearchIndex.getSingleFieldStringValue(doc, versionLabelField) : null;
                            if (StringUtils.isNotEmpty(versionLabel)) {
                                jsonObj.put("label", versionLabel);
                            }
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
                String versionLabel = versionLabelField != null ? topDocument.getMetadataValue(versionLabelField) : null;
                if (versionLabel != null) {
                    jsonObj.put("label", versionLabel);
                }
                jsonObj.put("id", getPi());
                jsonObj.put("year", topDocument.getMetadataValue("MD_YEARPUBLISH"));
                jsonObj.put("order", "0"); // "0" identifies the currently loaded version
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
                            String versionLabel =
                                    versionLabelField != null ? SolrSearchIndex.getSingleFieldStringValue(doc, versionLabelField) : null;
                            if (StringUtils.isNotEmpty(versionLabel)) {
                                jsonObj.put("label", versionLabel);
                            }
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
                        } else {
                            //Identifier has no matching document. break while-loop
                            break;
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
                contextObject =
                        currentDocument.generateContextObject(BeanUtils.getNavigationHelper().getCurrentUrl(), currentDocument.getTopStruct());
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

        return DataManager.getInstance().getConfiguration().useTiles();
    }

    public boolean useTilesFullscreen() throws IndexUnreachableException, DAOException, ConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return false;
        }

        return DataManager.getInstance().getConfiguration().useTilesFullscreen();
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
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public CalendarView getCalendarView() throws IndexUnreachableException, PresentationException {
        if (calendarView == null) {
            calendarView = createCalendarView();
        }
        return calendarView;
    }

    /**
     * @return the firstPageOrientation
     */
    public PageOrientation getFirstPageOrientation() {
        return firstPageOrientation;
    }

    /**
     * @param firstPageOrientation the firstPageOrientation to set
     */
    public void setFirstPageOrientation(PageOrientation firstPageOrientation) {
        this.firstPageOrientation = firstPageOrientation;
    }

    /**
     * @return 1 if we are in double page mode and the current page is the right page. 0 otherwise
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    public int getCurrentPageSourceIndex() throws IndexUnreachableException, DAOException {
        if (isDoublePageMode()) {
            PhysicalElement currentRightPage = getCurrentRightPage().orElse(null);
            if (currentRightPage != null) {
                return currentRightPage.equals(getCurrentPage()) ? 1 : 0;
            }
        }

        return 0;
    }

    public String getTopDocumentTitle() {
        return getDocumentTitle(this.topDocument);
    }

    public String getDocumentTitle(StructElement document) {
        StringBuilder sb = new StringBuilder();
        if (document != null) {
            switch (document.docStructType) {
                case "Comment":
                    sb.append("\"").append(document.getMetadataValue(SolrConstants.TITLE)).append("\"");
                    if (StringUtils.isNotBlank(document.getMetadataValue("MD_AUTHOR"))) {
                        sb.append(" von ").append(document.getMetadataValue("MD_AUTHOR"));
                    }
                    if (StringUtils.isNotBlank(document.getMetadataValue("MD_YEARPUBLISH"))) {
                        sb.append(" (").append(document.getMetadataValue("MD_YEARPUBLISH")).append(")");
                    }
                    break;
                case "FormationHistory":
                    sb.append("\"").append(document.getMetadataValue(SolrConstants.TITLE)).append("\"");
                    //TODO: Add Einsatzland z.b.: (Deutschland)
                    if (StringUtils.isNotBlank(document.getMetadataValue("MD_AUTHOR"))) {
                        sb.append(" von ").append(document.getMetadataValue("MD_AUTHOR"));
                    }
                    if (StringUtils.isNotBlank(document.getMetadataValue("MD_YEARPUBLISH"))) {
                        sb.append(" (").append(document.getMetadataValue("MD_YEARPUBLISH")).append(")");
                    }
                    break;
                case "Source":
                default:
                    sb.append(document.getDisplayLabel());
            }
        }
        return sb.toString();
    }
}
