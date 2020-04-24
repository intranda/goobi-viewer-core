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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.jdom2.JDOMException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import io.goobi.viewer.controller.AlphanumCollatorComparator;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.TranskribusUtils;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.calendar.CalendarView;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.transkribus.TranskribusJob;
import io.goobi.viewer.model.transkribus.TranskribusSession;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import io.goobi.viewer.model.viewer.pageloader.LeanPageLoader;

/**
 * Holds information about the currently open record (structure, pages, etc.). Used to reduced the size of ActiveDocumentBean.
 */
public class ViewManager implements Serializable {

    private static final long serialVersionUID = -7776362205876306849L;

    private static final Logger logger = LoggerFactory.getLogger(ViewManager.class);

    private ImageDeliveryBean imageDeliveryBean;

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

    /** Table of contents object. */
    private TOC toc;

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
    private Long pagesWithFulltext = null;
    private Long pagesWithAlto = null;
    private Boolean workHasTEIFiles = null;
    private Boolean metadataViewOnly = null;

    /**
     * <p>
     * Constructor for ViewManager.
     * </p>
     *
     * @param topDocument a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pageLoader a {@link io.goobi.viewer.model.viewer.pageloader.IPageLoader} object.
     * @param currentDocumentIddoc a long.
     * @param logId a {@link java.lang.String} object.
     * @param mainMimeType a {@link java.lang.String} object.
     * @param imageDeliveryBean a {@link io.goobi.viewer.managedbeans.ImageDeliveryBean} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    public ViewManager(StructElement topDocument, IPageLoader pageLoader, long currentDocumentIddoc, String logId, String mainMimeType,
            ImageDeliveryBean imageDeliveryBean) throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        this.imageDeliveryBean = imageDeliveryBean;
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
                        isBelowFulltextThreshold(), BeanUtils.getLocale());
            }
        }
        this.mainMimeType = mainMimeType;
        logger.trace("mainMimeType: {}", mainMimeType);
    }

    /**
     * <p>
     * createCalendarView.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.calendar.CalendarView} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public CalendarView createCalendarView() throws IndexUnreachableException, PresentationException {
        // Init calendar view
        String anchorPi = anchorDocument != null ? anchorDocument.getPi() : (topDocument.isAnchor() ? pi : null);
        return new CalendarView(pi, anchorPi, topDocument.isAnchor() ? null : topDocument.getMetadataValue(SolrConstants._CALENDAR_YEAR));

    }

    /**
     * <p>
     * getRepresentativeImageInfo.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRepresentativeImageInfo() throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        PhysicalElement representative = getRepresentativePage();
        if (representative == null) {
            return "";
        }

        StringBuilder urlBuilder = new StringBuilder(DataManager.getInstance().getConfiguration().getIIIFApiUrl());
        urlBuilder.append("image/").append(pi).append('/').append(representative.getFileName()).append("/info.json");
        return urlBuilder.toString();
    }

    /**
     * <p>
     * getCurrentImageInfo.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageInfo() throws IndexUnreachableException, DAOException {
        if (getCurrentPage() != null && getCurrentPage().getMimeType().startsWith("image")) {
            return getCurrentImageInfo(BeanUtils.getNavigationHelper().getCurrentPageType());
        }

        return "{}";
    }

    /**
     * <p>
     * getCurrentImageInfo.
     * </p>
     *
     * @param pageType a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
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
        if (topDocument != null && topDocument.isRtl()) {
            actualPageOrientation = actualPageOrientation.opposite();
        }
        if (actualPageOrientation.equals(PageOrientation.left)) {
            return getPage(this.currentImageOrder);
        } else if (topDocument != null && topDocument.isRtl()) {
            return getPage(this.currentImageOrder + 1);
        } else {
            return getPage(this.currentImageOrder - 1);
        }

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
        if (topDocument != null && topDocument.isRtl()) {
            actualPageOrientation = actualPageOrientation.opposite();
        }
        if (actualPageOrientation.equals(PageOrientation.right)) {
            return getPage(this.currentImageOrder);
        } else if (topDocument != null && topDocument.isRtl()) {
            return getPage(this.currentImageOrder - 1);
        } else {
            return getPage(this.currentImageOrder + 1);
        }

    }

    private String getImageInfo(PhysicalElement page, PageType pageType) {
        return imageDeliveryBean.getImages().getImageUrl(page, pageType);
    }

    /**
     * <p>
     * getCurrentImageInfoFullscreen.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageInfoFullscreen() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }
        String url = getImageInfo(currentPage, PageType.viewFullscreen);
        return url;
    }

    /**
     * <p>
     * getCurrentImageInfoCrowd.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageInfoCrowd() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }
        String url = getImageInfo(currentPage, PageType.editOcr);
        return url;
    }

    /**
     * <p>
     * getWatermarkUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getWatermarkUrl() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        return getWatermarkUrl("viewImage");
    }

    /**
     * <p>
     * getWatermarkUrl.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getWatermarkUrl(String pageType) throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        return imageDeliveryBean.getFooter()
                .getWatermarkUrl(Optional.ofNullable(getCurrentPage()), Optional.ofNullable(getTopDocument()),
                        Optional.ofNullable(PageType.getByName(pageType)))
                .orElse("");

    }

    /**
     * <p>
     * getCurrentImageUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageUrl() throws ViewerConfigurationException, IndexUnreachableException, DAOException {
        return getCurrentImageUrl(PageType.viewObject);
    }

    /**
     * <p>
     * getCurrentObjectUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentObjectUrl() throws IndexUnreachableException, DAOException {
        return imageDeliveryBean.getObjects3D().getObjectUrl(pi, getCurrentPage().getFilename());
    }

    /**
     * <p>
     * getCurrentImageUrl.
     * </p>
     *
     * @return the iiif url to the image in a configured size
     * @param view a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getCurrentImageUrl(PageType view) throws IndexUnreachableException, DAOException, ViewerConfigurationException {

        int size = DataManager.getInstance()
                .getConfiguration()
                .getImageViewZoomScales(view, Optional.ofNullable(getCurrentPage()).map(page -> page.getImageType()).orElse(null))
                .stream()
                .map(string -> "max".equalsIgnoreCase(string) ? 0 : Integer.parseInt(string))
                .sorted((s1, s2) -> s1 == 0 ? -1 : (s2 == 0 ? 1 : Integer.compare(s2, s1)))
                .findFirst()
                .orElse(800);
        return getCurrentImageUrl(view, size);
    }

    /**
     * <p>
     * getCurrentImageUrl.
     * </p>
     *
     * @param size a int.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageUrl(int size) throws IndexUnreachableException, DAOException {
        return getCurrentImageUrl(PageType.viewImage, size);
    }

    /**
     * <p>
     * getCurrentMasterImageUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentMasterImageUrl() throws IndexUnreachableException, DAOException {
        return getCurrentMasterImageUrl(Scale.MAX);
    }

    /**
     * <p>
     * getCurrentMasterImageUrl.
     * </p>
     *
     * @param scale a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentMasterImageUrl(Scale scale) throws IndexUnreachableException, DAOException {

        PageType pageType = BeanUtils.getNavigationHelper().getCurrentPageType();
        if (pageType == null) {
            pageType = PageType.viewObject;
        }
        StringBuilder sb = new StringBuilder(imageDeliveryBean.getThumbs().getFullImageUrl(getCurrentPage(), scale));
        try {
            if (DataManager.getInstance().getConfiguration().getFooterHeight(pageType, getCurrentPage().getImageType()) > 0) {
                sb.append("?ignoreWatermark=false");
                sb.append(imageDeliveryBean.getFooter().getWatermarkTextIfExists(getCurrentPage()).map(text -> "&watermarkText=" + text).orElse(""));
                sb.append(imageDeliveryBean.getFooter().getFooterIdIfExists(getTopDocument()).map(id -> "&watermarkId=" + id).orElse(""));
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Unable to read watermark config, ignore watermark", e);
        }
        return sb.toString();
    }

    /**
     * @param view
     * @param size
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     */
    private String getCurrentImageUrl(PageType view, int size) throws IndexUnreachableException, DAOException {
        StringBuilder sb = new StringBuilder(imageDeliveryBean.getThumbs().getThumbnailUrl(getCurrentPage(), size, size));
        try {
            if (DataManager.getInstance().getConfiguration().getFooterHeight(view, getCurrentPage().getImageType()) > 0) {
                sb.append("?ignoreWatermark=false");
                sb.append(imageDeliveryBean.getFooter().getWatermarkTextIfExists(getCurrentPage()).map(text -> "&watermarkText=" + text).orElse(""));
                sb.append(imageDeliveryBean.getFooter().getFooterIdIfExists(getTopDocument()).map(id -> "&watermarkId=" + id).orElse(""));
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Unable to read watermark config, ignore watermark", e);
        }
        return sb.toString();
    }

    /**
     * <p>
     * getJpegUrlForDownload.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getJpegUrlForDownload() throws IndexUnreachableException, DAOException {

        Scale scale;

        String maxSize = DataManager.getInstance().getConfiguration().getWidgetUsageMaxJpegSize();
        if (maxSize.equalsIgnoreCase(Scale.MAX_SIZE) || maxSize.equalsIgnoreCase(Scale.FULL_SIZE)) {
            scale = Scale.MAX;
        } else if (maxSize.matches("\\d{1,9}")) {
            scale = new Scale.ScaleToBox(Integer.parseInt(maxSize), Integer.parseInt(maxSize));
        } else {
            throw new IllegalArgumentException("Not a valid size paramter in config: " + maxSize);
        }

        return imageDeliveryBean.getThumbs().getThumbnailUrl(getCurrentPage(), scale);
    }

    /**
     * <p>
     * getMasterImageUrlForDownload.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getMasterImageUrlForDownload() throws IndexUnreachableException, DAOException {

        Scale scale;

        String maxSize = DataManager.getInstance().getConfiguration().getWidgetUsageMaxMasterImageSize();
        if (maxSize.equalsIgnoreCase(Scale.MAX_SIZE) || maxSize.equalsIgnoreCase(Scale.FULL_SIZE)) {
            scale = Scale.MAX;
        } else if (maxSize.matches("\\d{1,9}")) {
            scale = new Scale.ScaleToBox(Integer.parseInt(maxSize), Integer.parseInt(maxSize));
        } else {
            throw new IllegalArgumentException("Not a valid size paramter in config: " + maxSize);
        }

        return getCurrentMasterImageUrl(scale);

    }

    /**
     * <p>
     * getCurrentSearchResultCoords.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<List<String>> getCurrentSearchResultCoords() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        List<List<String>> coords = new ArrayList<>();
        List<String> coordStrings = getSearchResultCoords(getCurrentPage());
        if (coordStrings != null) {
            for (String string : coordStrings) {
                coords.add(Arrays.asList(string.split(",")));
            }
        }
        return coords;
    }

    private List<String> getSearchResultCoords(PhysicalElement currentImg) throws ViewerConfigurationException {
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

    /**
     * <p>
     * getRepresentativeWidth.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getRepresentativeWidth() throws PresentationException, IndexUnreachableException, DAOException {
        if (getRepresentativePage() != null) {
            return getRepresentativePage().getImageWidth();
        }
        return 0;
    }

    /**
     * <p>
     * getRepresentativeHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getRepresentativeHeight() throws PresentationException, IndexUnreachableException, DAOException {
        if (getRepresentativePage() != null) {
            return getRepresentativePage().getImageHeight();
        }
        return 0;
    }

    /**
     * <p>
     * getCurrentWidth.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
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

    /**
     * <p>
     * getCurrentHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
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

    /**
     * <p>
     * getRepresentativeImageUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getRepresentativeImageUrl() throws IndexUnreachableException, PresentationException, DAOException {
        return getRepresentativeImageUrl(representativePage.getImageWidth(), representativePage.getImageHeight());
    }

    /**
     * 
     * @param width
     * @param height
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public String getRepresentativeImageUrl(int width, int height) throws IndexUnreachableException, PresentationException, DAOException {
        if (getRepresentativePage() == null) {
            return null;
        }

        //      Dimension imageSize = new Dimension(representativePage.getImageWidth(), representativePage.getImageHeight());
        return imageDeliveryBean.getThumbs().getThumbnailUrl(representativePage, width, height);
    }

    /**
     * <p>
     * scaleToWidth.
     * </p>
     *
     * @param imageSize a {@link java.awt.Dimension} object.
     * @param scaledWidth a int.
     * @return a {@link java.awt.Dimension} object.
     */
    public static Dimension scaleToWidth(Dimension imageSize, int scaledWidth) {
        double scale = scaledWidth / imageSize.getWidth();
        int scaledHeight = (int) (imageSize.getHeight() * scale);
        return new Dimension(scaledWidth, scaledHeight);
    }

    /**
     * <p>
     * scaleToHeight.
     * </p>
     *
     * @param imageSize a {@link java.awt.Dimension} object.
     * @param scaledHeight a int.
     * @return a {@link java.awt.Dimension} object.
     */
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
     * <p>
     * rotateLeft.
     * </p>
     *
     * @should rotate correctly
     * @return a {@link java.lang.String} object.
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
     * <p>
     * rotateRight.
     * </p>
     *
     * @should rotate correctly
     * @return a {@link java.lang.String} object.
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
     * <p>
     * resetImage.
     * </p>
     *
     * @should reset rotation
     * @return a {@link java.lang.String} object.
     */
    public String resetImage() {
        this.rotate = 0;
        logger.trace("resetImage: {}", rotate);

        return null;
    }

    /**
     * <p>
     * isHasUrns.
     * </p>
     *
     * @return true if this record contains URN or IMAGEURN fields; false otherwise
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isHasUrns() throws PresentationException, IndexUnreachableException {
        return topDocument.getMetadataFields().containsKey(SolrConstants.URN) || topDocument.getFirstPageFieldValue(SolrConstants.IMAGEURN) != null;
    }

    /**
     * <p>
     * isHasVolumes.
     * </p>
     *
     * @return true if this is an anchor record and has indexed volumes; false otherwise
     */
    public boolean isHasVolumes() {
        if (!topDocument.isAnchor()) {
            return false;
        }

        return topDocument.getNumVolumes() > 0;
    }

    /**
     * <p>
     * isHasPages.
     * </p>
     *
     * @return true if record contains pages; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isHasPages() throws IndexUnreachableException {
        return pageLoader != null && pageLoader.getNumPages() > 0;
    }

    /**
     * <p>
     * isFilesOnly.
     * </p>
     *
     * @return true if record or first child or first page have an application mime type; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isFilesOnly() throws IndexUnreachableException, DAOException {
        // TODO check all files for mime type?
        if (filesOnly == null) {
            if (MimeType.APPLICATION.getName().equals(mainMimeType)) {
                filesOnly = true;
            } else {
                boolean childIsFilesOnly = isChildFilesOnly();
                PhysicalElement firstPage = pageLoader.getPage(pageLoader.getFirstPageOrder());
                filesOnly = childIsFilesOnly || (isHasPages() && firstPage != null && firstPage.getMimeType().equals(MimeType.APPLICATION.getName()));
            }

        }

        return filesOnly;
    }

    /**
     * Convenience method for identifying born digital material records.
     *
     * @return true if record is born digital material (no scanned images); false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isBornDigital() throws IndexUnreachableException, DAOException {
        return isHasPages() && isFilesOnly();
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     */
    private boolean isChildFilesOnly() throws IndexUnreachableException {
        boolean childIsFilesOnly = false;
        if (currentDocument != null && (currentDocument.isAnchor() || currentDocument.isGroup())) {
            try {
                String mimeType = currentDocument.getFirstVolumeFieldValue(SolrConstants.MIMETYPE);
                if (MimeType.APPLICATION.getName().equals(mimeType)) {
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
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isListAllVolumesInTOC() throws IndexUnreachableException, DAOException {
        return DataManager.getInstance().getConfiguration().isTocListSiblingRecords() || isFilesOnly();
    }

    /**
     * Returns all pages in their correct order. Used for e-publications.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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
     * <p>
     * getCurrentPage.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getCurrentPage() throws IndexUnreachableException, DAOException {
        return getPage(currentImageOrder).orElse(null);
    }

    /**
     * Returns the page with the given order number from the page loader, if exists.
     *
     * @param order a int.
     * @return requested page if exists; null otherwise.
     * @should return correct page
     * @should return null if order less than zero
     * @should return null if order larger than number of pages
     * @should return null if pageLoader is null
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Optional<PhysicalElement> getPage(int order) throws IndexUnreachableException, DAOException {
        if (pageLoader != null && pageLoader.getPage(order) != null) {
            // logger.debug("page " + order + ": " + pageLoader.getPage(order).getFileName());
            return Optional.ofNullable(pageLoader.getPage(order));
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Getter for the field <code>representativePage</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getRepresentativePage() throws PresentationException, IndexUnreachableException, DAOException {
        if (representativePage == null) {
            String thumbnailName = topDocument.getMetadataValue(SolrConstants.THUMBNAIL);
            if (pageLoader != null) {
                if (thumbnailName != null) {
                    representativePage = pageLoader.getPageForFileName(thumbnailName);
                }
                if (representativePage == null) {
                    representativePage = pageLoader.getPage(pageLoader.getFirstPageOrder());
                }
            }
        }

        return representativePage;
    }

    /**
     * <p>
     * getFirstPage.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getFirstPage() throws IndexUnreachableException, DAOException {
        return pageLoader.getPage(pageLoader.getFirstPageOrder());
    }

    /**
     * <p>
     * getCurrentImageNo.
     * </p>
     *
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
     * @param currentImageNo a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public void setCurrentImageNoForPaginator(int currentImageNo) throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        logger.trace("setCurrentImageNoForPaginator({})", currentImageNo);
        setCurrentImageNo(currentImageNo);
    }

    /**
     * <p>
     * setCurrentImageNo.
     * </p>
     *
     * @param currentImageNo the currentImageNo to set
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws RecordNotFoundException
     * @throws PresentationException
     * @throws IDDOCNotFoundException
     */
    public void setCurrentImageNo(int currentImageNo) throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        logger.trace("setCurrentImageNo: {}", currentImageNo);
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
            if (iddoc != null && iddoc > -1) {
                currentDocumentIddoc = iddoc;
                logger.trace("currentDocumentIddoc: {} ({})", currentDocumentIddoc, pi);
            } else if (isHasPages()) {
                logger.warn("currentDocumentIddoc not found for '{}', page {}", pi, currentImageNo);
                throw new IDDOCNotFoundException("currentElementIddoc not found for '" + pi + "', page " + currentImageNo);
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
        if (currentDocument == null || currentDocument.getLuceneId() != currentDocumentIddoc) {
            setCurrentDocument(new StructElement(currentDocumentIddoc));
        }
    }

    /**
     * Returns the ORDERLABEL value for the current page.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCurrentImageLabel() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            return currentPage.getOrderLabel().trim();
        }

        return null;
    }

    /**
     * <p>
     * nextImage.
     * </p>
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public String nextImage() throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        //        logger.debug("currentImageNo: {}", currentImageOrder);
        if (currentImageOrder < pageLoader.getLastPageOrder()) {
            setCurrentImageNo(currentImageOrder);
        }
        updateDropdownSelected();
        return null;
    }

    /**
     * <p>
     * prevImage.
     * </p>
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public String prevImage() throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        if (currentImageOrder > 0) {
            setCurrentImageNo(currentImageOrder);
        }
        updateDropdownSelected();
        return "";
    }

    /**
     * <p>
     * firstImage.
     * </p>
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public String firstImage() throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        setCurrentImageNo(pageLoader.getFirstPageOrder());
        updateDropdownSelected();
        return null;
    }

    /**
     * <p>
     * lastImage.
     * </p>
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public String lastImage() throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        setCurrentImageNo(pageLoader.getLastPageOrder());
        updateDropdownSelected();
        return null;
    }

    /**
     * <p>
     * isMultiPageRecord.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isMultiPageRecord() throws IndexUnreachableException {
        return getImagesCount() > 1;
    }

    /**
     * <p>
     * getImagesCount.
     * </p>
     *
     * @return {@link java.lang.Integer}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getImagesCount() throws IndexUnreachableException {
        if (pageLoader == null) {
            return -1;
        }
        return pageLoader.getNumPages();
    }

    /**
     * <p>
     * Getter for the field <code>dropdownPages</code>.
     * </p>
     *
     * @return the dropdownPages
     */
    public List<SelectItem> getDropdownPages() {
        return dropdownPages;
    }

    /**
     * <p>
     * Getter for the field <code>dropdownFulltext</code>.
     * </p>
     *
     * @return the dropdownPages
     */
    public List<SelectItem> getDropdownFulltext() {
        return dropdownFulltext;
    }

    /**
     * <p>
     * Setter for the field <code>dropdownSelected</code>.
     * </p>
     *
     * @param dropdownSelected the dropdownSelected to set
     */
    public void setDropdownSelected(String dropdownSelected) {
        this.dropdownSelected = dropdownSelected;
        //        logger.debug("dropdownSelected: " + dropdownSelected);
    }

    /**
     * <p>
     * Getter for the field <code>dropdownSelected</code>.
     * </p>
     *
     * @return the dropdownSelected
     */
    public String getDropdownSelected() {
        return dropdownSelected;
    }

    /**
     *
     * Returns the PhysicalElements for the current thumbnail page using the configured number of thumbnails per page;
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<PhysicalElement> getImagesSection() throws IndexUnreachableException, DAOException {
        return getImagesSection(DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage());
    }

    /**
     * Returns the PhysicalElements for the current thumbnail page.
     *
     * @param thumbnailsPerPage Length of the thumbnail list per page.
     * @return PhysicalElements for the current thumbnail page.
     * @should return correct PhysicalElements for a thumbnail page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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

    /**
     * <p>
     * getFirstDisplayedThumbnailIndex.
     * </p>
     *
     * @return a int.
     */
    public int getFirstDisplayedThumbnailIndex() {
        return getFirstDisplayedThumbnailIndex(DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage());
    }

    /**
     * <p>
     * Getter for the field <code>currentThumbnailPage</code>.
     * </p>
     *
     * @return a int.
     */
    public int getCurrentThumbnailPage() {
        return currentThumbnailPage;
    }

    /**
     * <p>
     * Setter for the field <code>currentThumbnailPage</code>.
     * </p>
     *
     * @param currentThumbnailPage a int.
     */
    public void setCurrentThumbnailPage(int currentThumbnailPage) {
        this.currentThumbnailPage = currentThumbnailPage;
    }

    /**
     * <p>
     * nextThumbnailSection.
     * </p>
     */
    public void nextThumbnailSection() {
        ++currentThumbnailPage;
    }

    /**
     * <p>
     * previousThumbnailSection.
     * </p>
     */
    public void previousThumbnailSection() {
        --currentThumbnailPage;
    }

    /**
     * <p>
     * hasPreviousThumbnailSection.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasPreviousThumbnailSection() {
        int currentFirstThumbnailIndex = getFirstDisplayedThumbnailIndex();
        int previousLastThumbnailIndex = currentFirstThumbnailIndex - DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
        return previousLastThumbnailIndex >= pageLoader.getFirstPageOrder();
    }

    /**
     * <p>
     * hasNextThumbnailSection.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasNextThumbnailSection() {
        int currentFirstThumbnailIndex = getFirstDisplayedThumbnailIndex();
        int previousLastThumbnailIndex = currentFirstThumbnailIndex + DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
        return previousLastThumbnailIndex <= pageLoader.getLastPageOrder();
    }

    /**
     * <p>
     * updateDropdownSelected.
     * </p>
     */
    public void updateDropdownSelected() {
        setDropdownSelected(String.valueOf(currentImageOrder));
    }

    /**
     * <p>
     * dropdownAction.
     * </p>
     *
     * @param event {@link javax.faces.event.ValueChangeEvent}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws java.lang.NumberFormatException if any.
     * @throws IDDOCNotFoundException
     */
    public void dropdownAction(ValueChangeEvent event)
            throws NumberFormatException, IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        setCurrentImageNo(Integer.valueOf((String) event.getNewValue()) - 1);
    }

    /**
     * <p>
     * getImagesSizeThumbnail.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
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
     * <p>
     * getLinkForDFGViewer.
     * </p>
     *
     * @return DFG Viewer link
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getLinkForDFGViewer() throws IndexUnreachableException {
        if (topDocument != null && SolrConstants._METS.equals(topDocument.getSourceDocFormat()) && isHasPages()) {
            try {
                StringBuilder sbPath = new StringBuilder();
                sbPath.append(DataManager.getInstance().getConfiguration().getDfgViewerUrl());
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
     * <p>
     * getMetsResolverUrl.
     * </p>
     *
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

    /**
     * <p>
     * getLidoResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
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
     * <p>
     * getDenkxwebResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDenkxwebResolverUrl() {
        try {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/denkxwebresolver?id=" + getPi();
        } catch (Exception e) {
            logger.error("Could not get DenkXweb resolver URL for {}.", topDocumentIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/denkxwebresolver?id=" + 0;
    }

    /**
     * <p>
     * getDublinCoreResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDublinCoreResolverUrl() {
        try {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/dublincoreresolver?id=" + getPi();
        } catch (Exception e) {
            logger.error("Could not get DublinCore resolver URL for {}.", topDocumentIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/dublincoreresolver?id=" + 0;
    }

    /**
     * <p>
     * getAnchorMetsResolverUrl.
     * </p>
     *
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
     * Return the url to a REST service delivering all alto files of a work as zip
     *
     * @return the url to a REST service delivering all alto files of a work as zip
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getAltoUrlForAllPages() throws ViewerConfigurationException, PresentationException, IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getRestApiUrl() + "content/alto/" + getPi();
    }

    /**
     * Return the url to a REST service delivering all plain text of a work as zip
     *
     * @return the url to a REST service delivering all plain text of a work as zip
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getFulltextUrlForAllPages() throws ViewerConfigurationException, PresentationException, IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getRestApiUrl() + "content/fulltext/" + getPi();
    }

    /**
     * Return the url to a REST service delivering a TEI document containing the text of all pages
     *
     * @return the TEI REST url
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getTeiUrlForAllPages() throws ViewerConfigurationException, IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getRestApiUrl() + "content/tei/" + getPi() + "/" + BeanUtils.getLocale().getLanguage();
    }

    /**
     * Return the url to a REST service delivering the fulltext of the current page as TEI
     *
     * @return the TEI REST url
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getTeiUrl() throws ViewerConfigurationException, IndexUnreachableException, DAOException {
        String filename = FileTools.getFilenameFromPathString(getCurrentPage().getFulltextFileName());
        if (StringUtils.isBlank(filename)) {
            filename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        }
        return DataManager.getInstance().getConfiguration().getRestApiUrl() + "content/tei/" + getPi() + "/" + filename + "/"
                + BeanUtils.getLocale().getLanguage();

    }

    /**
     * Return the url to a REST service delivering the alto file of the given page as xml
     *
     * @return the url to a REST service delivering the alto file of the given page as xml
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getAltoUrl() throws ViewerConfigurationException, PresentationException, IndexUnreachableException, DAOException {
        String filename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        return DataManager.getInstance().getConfiguration().getRestApiUrl() + "content/alto/" + getPi() + "/" + filename;
    }

    /**
     * Return the url to a REST service delivering the fulltext as plain text of the given page
     *
     * @return the url to a REST service delivering the fulltext as plain text of the given page
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getFulltextUrl() throws ViewerConfigurationException, PresentationException, IndexUnreachableException, DAOException {
        String filename = FileTools.getFilenameFromPathString(getCurrentPage().getFulltextFileName());
        if (StringUtils.isBlank(filename)) {
            filename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        }
        return DataManager.getInstance().getConfiguration().getRestApiUrl() + "content/fulltext/" + getPi() + "/" + filename;
    }

    /**
     * Returns the pdf download link for the current document
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getPdfDownloadLink() throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        return imageDeliveryBean.getPdf().getPdfUrl(getTopDocument(), "");
    }

    /**
     * Returns the pdf download link for the current page
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getPdfPageDownloadLink() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return null;
        }
        return imageDeliveryBean.getPdf().getPdfUrl(getTopDocument(), currentPage);
    }

    /**
     * Returns the pdf download link for the current struct element
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public String getPdfStructDownloadLink() throws IndexUnreachableException, DAOException, ViewerConfigurationException, PresentationException {
        StructElement currentStruct = getCurrentDocument();
        return imageDeliveryBean.getPdf().getPdfUrl(currentStruct, currentStruct.getLabel());

    }

    /**
     * Returns the pdf download link for a pdf of all pages from this.firstPdfPage to this.lastPdfPage (inclusively)
     *
     * @should construct url correctly
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getPdfPartDownloadLink() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
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
        return imageDeliveryBean.getPdf().getPdfUrl(getTopDocument(), pages.toArray(pageArr));
    }

    /**
     * <p>
     * isPdfPartDownloadLinkEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPdfPartDownloadLinkEnabled() {
        return firstPdfPage <= lastPdfPage;
    }

    /**
     * Reset the pdf access permissions. They will be evaluated again on the next call to {@link #isAccessPermissionPdf()}
     */
    public void resetAccessPermissionPdf() {
        this.accessPermissionPdf = null;
    }

    /**
     * <p>
     * isAccessPermissionPdf.
     * </p>
     *
     * @return true if record/structure PDF download is allowed; false otherwise
     */
    public boolean isAccessPermissionPdf() {
        try {
            if (topDocument == null || !topDocument.isWork() || !isHasPages()) {
                return false;
            }
            if (!MimeType.isImageOrPdfDownloadAllowed(topDocument.getMetadataValue(SolrConstants.MIMETYPE))) {
                return false;
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return false;
        }
        // Only allow PDF downloads for records coming from METS files
        if (!SolrConstants._METS.equals(topDocument.getSourceDocFormat())) {
            return false;
        }

        if (accessPermissionPdf == null) {
            try {
                accessPermissionPdf = isAccessPermission(IPrivilegeHolder.PRIV_DOWNLOAD_PDF);
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
     * 
     * @param privilege Privilege name to check
     * @return true if current user has the privilege for this record; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isAccessPermission(String privilege) throws IndexUnreachableException, DAOException {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, privilege, request);
    }

    /**
     * Reset the permissions for writing user comments. They will be evaluated again on the next call to {@link #isAllowUserComments()}
     */
    public void resetAllowUserComments() {
        this.allowUserComments = null;
    }

    /**
     * Indicates whether user comments are allowed for the current record based on several criteria.
     *
     * @return a boolean.
     */
    public boolean isAllowUserComments() {
        if (!DataManager.getInstance().getConfiguration().isUserCommentsEnabled()) {
            return false;
        }

        if (allowUserComments == null) {
            String query = DataManager.getInstance().getConfiguration().getUserCommentsConditionalQuery();
            try {
                if (StringUtils.isNotEmpty(query) && DataManager.getInstance()
                        .getSearchIndex()
                        .getHitCount(new StringBuilder(SolrConstants.PI).append(':')
                                .append(pi)
                                .append(" AND (")
                                .append(query)
                                .append(')')
                                .toString()) == 0) {
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

    /**
     * <p>
     * isDisplayTitleBarPdfLink.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTitleBarPdfLink() {
        return DataManager.getInstance().getConfiguration().isTitlePdfEnabled() && isAccessPermissionPdf();
    }

    /**
     * <p>
     * isDisplayMetadataPdfLink.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayMetadataPdfLink() {
        return topDocument != null && topDocument.isWork() && DataManager.getInstance().getConfiguration().isMetadataPdfEnabled()
                && isAccessPermissionPdf();
    }

    /**
     * <p>
     * isDisplayPagePdfLink.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @Deprecated
    public boolean isDisplayPagePdfLink() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            return currentPage.isDisplayPagePdfLink();
        }

        return false;
    }

    /**
     * Convenience method that checks whether only the metadata view link is displayed for this record (i.e. criteria for all other links are not
     * met).
     * 
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public boolean isMetadataViewOnly() throws IndexUnreachableException, DAOException, PresentationException {
        if (metadataViewOnly == null) {
            // Check whether this mode is enabled first to avoid all the other checks
            if (!DataManager.getInstance().getConfiguration().isShowRecordLabelIfNoOtherViews()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }

            // Display object view criteria
            if (isDisplayObjectViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayCalendarViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayTocViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayThumbnailViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayFulltextViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayExternalFulltextLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayNerViewLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }
            if (isDisplayExternalResolverLink()) {
                metadataViewOnly = false;
                return metadataViewOnly;
            }

            metadataViewOnly = true;
        }

        return metadataViewOnly;
    }

    /**
     * 
     * @return true if object view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isDisplayObjectViewLink() throws IndexUnreachableException, DAOException {
        return DataManager.getInstance().getConfiguration().isSidebarPageLinkVisible() && isHasPages() && !isFilesOnly();
    }

    /**
     * 
     * @return true if calendar view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayCalendarViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarCalendarLinkVisible() && calendarView != null && calendarView.isDisplay();
    }

    /**
     * 
     * @return true if TOC view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayTocViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarTocVisible() && !isFilesOnly() && topDocument != null
                && !topDocument.isLidoRecord() && toc != null
                && (toc.isHasChildren() || DataManager.getInstance().getConfiguration().isDisplayEmptyTocInSidebar());
    }

    /**
     * 
     * @return true if thumbnail view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayThumbnailViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarThumbsLinkVisible()
                && pageLoader != null && pageLoader.getNumPages() > 1 && !isFilesOnly();
    }

    /**
     * 
     * @return true if metadata view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayMetadataViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarMetadataLinkVisible() && topDocument != null && !topDocument.isGroup();
    }

    /**
     * 
     * @return true if full-text view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayFulltextViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarFulltextLinkVisible() && topDocument != null && topDocument.isFulltextAvailable()
                && !isFilesOnly();
    }

    /**
     * 
     * @return true if external full-text link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayExternalFulltextLink() throws IndexUnreachableException, DAOException, PresentationException {
        return topDocument != null
                && topDocument.getMetadataValue("MD_LOCATION_URL_EXTERNALFULLTEXT") != null;
    }

    /**
     * 
     * @return true if NER view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayNerViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return topDocument != null && topDocument.isNerAvailable();
    }

    /**
     * 
     * @return true if NER view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayExternalResolverLink() throws IndexUnreachableException, DAOException, PresentationException {
        return topDocument != null
                && topDocument.getMetadataValue("MD_LOCATION_URL_EXTERNALRESOLVER") != null;
    }

    /**
     * <p>
     * getOaiMarcUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getOaiMarcUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getMarcUrl() + getPi();
    }

    /**
     * <p>
     * getOaiDcUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getOaiDcUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getDcUrl() + getPi();
    }

    /**
     * <p>
     * getOaiEseUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getOaiEseUrl() throws IndexUnreachableException {
        return DataManager.getInstance().getConfiguration().getEseUrl() + getPi();
    }

    /**
     * <p>
     * Getter for the field <code>opacUrl</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * Getter for the field <code>persistentUrl</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
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

        if (persistentUrl == null) {
            persistentUrl = getPersistentUrl(urn);
        }
        return persistentUrl;
    }

    /**
     * Returns the PURL for the current page (either via the URN resolver or a pretty URL)
     *
     * @return PURL for the current page
     * @should generate purl via urn correctly
     * @should generate purl without urn correctly
     * @param urn a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPersistentUrl(String urn) throws IndexUnreachableException {
        String persistentUrl = "";
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
                boolean anchorOrGroup = topDocument.isAnchor() || topDocument.isGroup();
                pageType = PageType.determinePageType(topDocument.getDocStructType(), null, anchorOrGroup, isHasPages(), false);
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

        return persistentUrl;
    }

    /**
     * Returns the main title of the current volume's anchor, if available.
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * isBelowFulltextThreshold.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isBelowFulltextThreshold() throws PresentationException, IndexUnreachableException {
        int threshold = DataManager.getInstance().getConfiguration().getFulltextPercentageWarningThreshold();
        return isBelowFulltextThreshold(threshold);
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should return true if there are no pages
     */
    boolean isBelowFulltextThreshold(double threshold) throws PresentationException, IndexUnreachableException {
        if (pageLoader.getNumPages() == 0) {
            return true;
        }
        if (pagesWithFulltext == null) {
            pagesWithFulltext = DataManager.getInstance()
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
        }
        double percentage = pagesWithFulltext * 100.0 / pageLoader.getNumPages();
        logger.trace("{}% of pages have full-text", percentage);
        if (percentage < threshold) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * isFulltextAvailableForWork.
     * </p>
     *
     * @return true if record has full-text and user has access rights; false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public boolean isFulltextAvailableForWork() throws IndexUnreachableException, DAOException, PresentationException {
        if (isBornDigital()) {
            return false;
        }

        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                BeanUtils.getRequest());
        return access && (!isBelowFulltextThreshold(0.0001) || isAltoAvailableForWork());
    }

    /**
     * 
     * @return true if any of this record's pages has an image and user has access rights; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isRecordHasImages() throws IndexUnreachableException, DAOException {
        if (topDocument == null || !topDocument.isHasImages()) {
            return false;
        }

        return AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_IMAGES,
                BeanUtils.getRequest());
    }

    /**
     * <p>
     * isTeiAvailableForWork.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public boolean isTeiAvailableForWork() throws IndexUnreachableException, DAOException, PresentationException {
        if (isBornDigital()) {
            return false;
        }

        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                BeanUtils.getRequest());
        return access && (!isBelowFulltextThreshold(0.0001) || isAltoAvailableForWork() || isWorkHasTEIFiles());
    }

    /**
     * @return true if there are any TEI files associated directly with the top document
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private boolean isWorkHasTEIFiles() throws IndexUnreachableException, PresentationException {
        if (workHasTEIFiles == null) {
            long teiDocs = DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                            .append(':')
                            .append(pi)
                            .append(" + ")
                            .append(SolrConstants.DOCTYPE)
                            .append(":")
                            .append(SolrConstants.DOCSTRCT)
                            .append(" +")
                            .append(SolrConstants.FILENAME_TEI)
                            .append(":*")
                            .toString());
            int threshold = 1;
            logger.trace("{} of pages have tei", teiDocs);
            if (teiDocs < threshold) {
                workHasTEIFiles = false;
            } else {
                workHasTEIFiles = true;
            }
        }

        return workHasTEIFiles;
    }

    /**
     * @return the toc
     */
    public TOC getToc() {
        return toc;
    }

    /**
     * @param toc the toc to set
     */
    public void setToc(TOC toc) {
        this.toc = toc;
    }

    /**
     * <p>
     * isAltoAvailableForWork.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isAltoAvailableForWork() throws IndexUnreachableException, PresentationException, DAOException {
        boolean access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                BeanUtils.getRequest());
        if (!access) {
            return false;
        }
        if (pagesWithAlto == null) {

            pagesWithAlto = DataManager.getInstance()
                    .getSearchIndex()
                    .getHitCount(new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                            .append(':')
                            .append(pi)
                            .append(" +")
                            .append(SolrConstants.DOCTYPE)
                            .append(":PAGE")
                            .append(" +")
                            .append(SolrConstants.FILENAME_ALTO)
                            .append(":*")
                            .toString());
            logger.trace("{} of pages have full-text", pagesWithAlto);
        }
        int threshold = 1;
        if (pagesWithAlto < threshold) {
            return false;
        }

        return true;
    }

    /**
     * Default fulltext getter (with HTML escaping).
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getFulltext() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        return getFulltext(true, null);
    }

    /**
     * Returns the full-text for the current page, stripped of any included JavaScript.
     *
     * @param escapeHtml If true HTML tags will be escaped to prevent pseudo-HTML from breaking the text.
     * @param language a {@link java.lang.String} object.
     * @return Full-text for the current page.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getFulltext(boolean escapeHtml, String language) throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        String currentFulltext = null;

        // Current page fulltext
        PhysicalElement currentImg = getCurrentPage();
        if (currentImg == null || StringUtils.isEmpty(currentImg.getFullText())) {
            return currentFulltext;
        }

        currentFulltext = StringTools.stripJS(currentImg.getFullText());
        if (currentFulltext.length() < currentImg.getFullText().length()) {
            logger.warn("JavaScript found and removed from full-text in {}, page {}", pi, currentImg.getOrder());
        }
        if (escapeHtml) {
            currentFulltext = StringTools.escapeHtmlChars(currentImg.getFullText());
        }

        // logger.trace(currentFulltext);
        return currentFulltext;
    }

    /**
     * 
     * 
     * @return the probable mimeType of the fulltext of the current page. Loads the fulltext of that page if neccessary
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public String getFulltextMimeType() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        PhysicalElement currentImg = getCurrentPage();
        if (currentImg != null) {
            return currentImg.getFulltextMimeType();
        }

        return null;
    }

    /**
     * <p>
     * getCurrentRotate.
     * </p>
     *
     * @return a int.
     */
    public int getCurrentRotate() {
        return rotate;
    }

    /**
     * <p>
     * getCurrentFooterHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getCurrentFooterHeight() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage != null) {
            return currentPage.getImageFooterHeight(rotate);
        }
        return 0;
    }

    /**
     * <p>
     * Setter for the field <code>zoomSlider</code>.
     * </p>
     *
     * @param zoomSlider a int.
     */
    public void setZoomSlider(int zoomSlider) {
        this.zoomSlider = zoomSlider;
    }

    /**
     * <p>
     * Getter for the field <code>zoomSlider</code>.
     * </p>
     *
     * @return a int.
     */
    public int getZoomSlider() {
        return this.zoomSlider;
    }

    /**
     * Returns true if original content download has been enabled in the configuration and there are files in the original content folder for this
     * record.
     *
     * @return a boolean.
     */
    public boolean isDisplayContentDownloadMenu() {
        if (!DataManager.getInstance().getConfiguration().isOriginalContentDownload()) {
            return false;
        }

        try {
            Path sourceFileDir = Helper.getDataFolder(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder());
            if (!Files.isDirectory(sourceFileDir)) {
                return false;
            }

            List<Path> files = Arrays.asList(sourceFileDir.toFile().listFiles()).stream().map(File::toPath).collect(Collectors.toList());
            if (!files.isEmpty()) {
                return AccessConditionUtils
                        .checkContentFileAccessPermission(pi,
                                (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest(), files)
                        .containsValue(Boolean.TRUE);
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
        } catch (DAOException e) {
            logger.debug("DAOException thrown here: {}", e.getMessage());
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Returns a list of original content file download links (name+url) for the current document.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public List<LabeledLink> getContentDownloadLinksForWork() throws IndexUnreachableException, DAOException, PresentationException {
        Path sourceFileDir = Helper.getDataFolder(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder());
        if (!Files.isDirectory(sourceFileDir)) {
            return Collections.emptyList();
        }

        List<LabeledLink> ret = new ArrayList<>();
        try {
            File[] sourcesArray = sourceFileDir.toFile().listFiles();
            if (sourcesArray != null && sourcesArray.length > 0) {
                List<File> sourcesList = Arrays.asList(sourcesArray);
                Collections.sort(sourcesList, filenameComparator);
                Map<String, Boolean> fileAccess = AccessConditionUtils.checkContentFileAccessPermission(getPi(), BeanUtils.getRequest(),
                        sourcesList.stream().map(file -> file.toPath()).collect(Collectors.toList()));
                for (File file : sourcesList) {
                    if (file.isFile()) {
                        Boolean access = fileAccess.get(file.toPath().toString());
                        if (access != null && access) {
                            String url = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/file?pi=" + getPi() + "&file="
                                    + URLEncoder.encode(file.getName(), Helper.DEFAULT_ENCODING);
                            ret.add(new LabeledLink(file.getName(), url, 0));
                        }
                        ;
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
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
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public List<LabeledLink> getContentDownloadLinksForPage() throws IndexUnreachableException, DAOException, PresentationException {
        List<LabeledLink> ret = new ArrayList<>();

        String page = String.valueOf(currentImageOrder);
        Path sourceFileDir = Paths
                .get(Helper.getDataFolder(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder()).toAbsolutePath().toString(), page);
        if (!Files.isDirectory(sourceFileDir)) {
            return Collections.emptyList();
        }

        try {
            List<File> files = Arrays.asList(sourceFileDir.toFile().listFiles());
            Map<String, Boolean> fileAccessMap = AccessConditionUtils.checkContentFileAccessPermission(getPi(), BeanUtils.getRequest(),
                    files.stream().map(File::toPath).collect(Collectors.toList()));
            for (File file : files) {
                if (file.isFile()) {
                    Boolean access = fileAccessMap.get(file.toPath().toString());
                    if (access != null && access) {
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
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>topDocumentIddoc</code>.
     * </p>
     *
     * @return the topDocumentIddoc
     */
    public long getTopDocumentIddoc() {
        return topDocumentIddoc;
    }

    /**
     * <p>
     * Setter for the field <code>topDocumentIddoc</code>.
     * </p>
     *
     * @param topDocumentIddoc the topDocumentIddoc to set
     */
    public void setTopDocumentIddoc(long topDocumentIddoc) {
        this.topDocumentIddoc = topDocumentIddoc;
    }

    /**
     * <p>
     * Getter for the field <code>topDocument</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public StructElement getTopDocument() {
        try {
            return loadTopDocument();
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Returns <code>topDocument</code>. If the IDDOC of <code>topDocument</code> is different from <code>topDocumentIddoc</code>,
     * <code>topDocument</code> is reloaded.
     *
     * @return the currentDocument
     * @throws IndexUnreachableException
     */
    private StructElement loadTopDocument() throws IndexUnreachableException {
        if (topDocument == null || topDocument.getLuceneId() != topDocumentIddoc) {
            topDocument = new StructElement(topDocumentIddoc, null);
        }
        return topDocument;
    }

    /**
     * <p>
     * setActiveDocument.
     * </p>
     *
     * @param currentDocument the currentDocument to set
     */
    public void setActiveDocument(StructElement currentDocument) {
        this.topDocument = currentDocument;
    }

    /**
     * <p>
     * Getter for the field <code>currentDocumentIddoc</code>.
     * </p>
     *
     * @return the currentDocumentIddoc
     */
    public long getCurrentDocumentIddoc() {
        return currentDocumentIddoc;
    }

    /**
     * <p>
     * Setter for the field <code>currentDocumentIddoc</code>.
     * </p>
     *
     * @param currentDocumentIddoc the currentDocumentIddoc to set
     */
    public void setCurrentDocumentIddoc(long currentDocumentIddoc) {
        this.currentDocumentIddoc = currentDocumentIddoc;
    }

    /**
     * <p>
     * Getter for the field <code>currentDocument</code>.
     * </p>
     *
     * @return the currentDocument
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getCurrentDocument() throws IndexUnreachableException {
        if (currentDocument == null || currentDocument.getLuceneId() != currentDocumentIddoc) {
            logger.trace("Creating new currentDocument from IDDOC {}, old currentDocumentIddoc: {}", currentDocumentIddoc,
                    currentDocument.getLuceneId());
            currentDocument = new StructElement(currentDocumentIddoc);
        }
        return currentDocument;
    }

    /**
     * <p>
     * getCurrentDocumentHierarchy.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
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
     * <p>
     * Setter for the field <code>currentDocument</code>.
     * </p>
     *
     * @param currentDocument the currentDocument to set
     */
    public void setCurrentDocument(StructElement currentDocument) {
        this.currentDocument = currentDocument;
    }

    /**
     * <p>
     * Getter for the field <code>logId</code>.
     * </p>
     *
     * @return the logId
     */
    public String getLogId() {
        return logId;
    }

    /**
     * <p>
     * Setter for the field <code>logId</code>.
     * </p>
     *
     * @param logId the logId to set
     */
    public void setLogId(String logId) {
        this.logId = logId;
        // Reset the hieararchy list so that a new one is created
        docHierarchy = null;
    }

    /**
     * <p>
     * Getter for the field <code>pageLoader</code>.
     * </p>
     *
     * @return the pageLoader
     */
    public IPageLoader getPageLoader() {
        return pageLoader;

    }

    /**
     * <p>
     * getHtmlHeadDCMetadata.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
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
     * <p>
     * getHighwirePressMetaTags.
     * </p>
     *
     * @return String with tags
     */
    public String getHighwirePressMetaTags() {
        try {
            return MetadataTools.generateHighwirePressMetaTags(this.topDocument, isFilesOnly() ? getAllPages() : null);
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
            return "";
        } catch (ViewerConfigurationException e) {
            logger.error(e.getMessage(), e);
            return "";
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
            return "";
        } catch (PresentationException e) {
            logger.error(e.getMessage(), e);
            return "";
        }
    }

    /**
     * <p>
     * isHasVersionHistory.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isHasVersionHistory() throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField())
                && StringUtils.isEmpty(DataManager.getInstance().getConfiguration().getNextVersionIdentifierField())) {
            return false;
        }

        return getVersionHistory().size() > 1;
    }

    /**
     * <p>
     * Getter for the field <code>versionHistory</code>.
     * </p>
     *
     * @should create create history correctly
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
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
                            next.add(jsonObj.toString());
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
                versionHistory.add(jsonObj.toString());
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
                            previous.add(jsonObj.toString());
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
     * @return a {@link java.lang.String} object.
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
     * <p>
     * addToTranskribusAction.
     * </p>
     *
     * @param login If true, the user will first be logged into their Transkribus account in the UserBean.
     * @return a {@link java.lang.String} object.
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
            NavigationHelper nh = BeanUtils.getNavigationHelper();
            String resolverUrlRoot = nh != null ? nh.getApplicationUrl() : "http://viewer.goobi.io/" + "metsresolver?id=";
            TranskribusJob job = TranskribusUtils.ingestRecord(DataManager.getInstance().getConfiguration().getTranskribusRestApiUrl(), session, pi,
                    resolverUrlRoot);
            if (job == null) {
                Messages.error("transkribus_recordInjestError");
                return "";
            }
            Messages.info("transkribus_recordIngestSuccess");
        } catch (IOException | JDOMException e) {
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
     * <p>
     * isRecordAddedToTranskribus.
     * </p>
     *
     * @param session a {@link io.goobi.viewer.model.transkribus.TranskribusSession} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isRecordAddedToTranskribus(TranskribusSession session) throws DAOException {
        if (session == null) {
            return false;
        }
        List<TranskribusJob> jobs = DataManager.getInstance().getDao().getTranskribusJobs(pi, session.getUserId(), null);

        return jobs != null && !jobs.isEmpty();
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return false;
        }

        return DataManager.getInstance().getConfiguration().useTiles();
    }

    /**
     * <p>
     * useTilesFullscreen.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesFullscreen() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return false;
        }

        return DataManager.getInstance().getConfiguration().useTilesFullscreen();
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
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
     * <p>
     * Getter for the field <code>mainMimeType</code>.
     * </p>
     *
     * @return the mainMimeType
     */
    public String getMainMimeType() {
        return mainMimeType;
    }

    /**
     * <p>
     * togglePageOrientation.
     * </p>
     */
    public void togglePageOrientation() {
        this.firstPageOrientation = this.firstPageOrientation.opposite();
    }

    /**
     * <p>
     * Setter for the field <code>doublePageMode</code>.
     * </p>
     *
     * @param doublePageMode the doublePageMode to set
     */
    public void setDoublePageMode(boolean doublePageMode) {
        this.doublePageMode = doublePageMode;
    }

    /**
     * <p>
     * isDoublePageMode.
     * </p>
     *
     * @return the doublePageMode
     */
    public boolean isDoublePageMode() {
        return doublePageMode;
    }

    /**
     * <p>
     * Getter for the field <code>firstPdfPage</code>.
     * </p>
     *
     * @return the firstPdfPage
     */
    public String getFirstPdfPage() {
        return String.valueOf(firstPdfPage);
    }

    /**
     * <p>
     * Setter for the field <code>firstPdfPage</code>.
     * </p>
     *
     * @param firstPdfPage the firstPdfPage to set
     */
    public void setFirstPdfPage(String firstPdfPage) {
        this.firstPdfPage = Integer.valueOf(firstPdfPage);
    }

    /**
     * <p>
     * Getter for the field <code>lastPdfPage</code>.
     * </p>
     *
     * @return the lastPdfPage
     */
    public String getLastPdfPage() {
        return String.valueOf(lastPdfPage);
    }

    /**
     * <p>
     * Setter for the field <code>lastPdfPage</code>.
     * </p>
     *
     * @param lastPdfPage the lastPdfPage to set
     */
    public void setLastPdfPage(String lastPdfPage) {
        logger.trace("setLastPdfPage: {}", lastPdfPage);
        if (lastPdfPage != null) {
            this.lastPdfPage = Integer.valueOf(lastPdfPage);
        }
    }

    /**
     * <p>
     * Getter for the field <code>calendarView</code>.
     * </p>
     *
     * @return the calendarView
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public CalendarView getCalendarView() throws IndexUnreachableException, PresentationException {
        if (calendarView == null) {
            calendarView = createCalendarView();
        }
        return calendarView;
    }

    /**
     * <p>
     * Getter for the field <code>firstPageOrientation</code>.
     * </p>
     *
     * @return the firstPageOrientation
     */
    public PageOrientation getFirstPageOrientation() {
        return firstPageOrientation;
    }

    /**
     * <p>
     * Setter for the field <code>firstPageOrientation</code>.
     * </p>
     *
     * @param firstPageOrientation the firstPageOrientation to set
     */
    public void setFirstPageOrientation(PageOrientation firstPageOrientation) {
        this.firstPageOrientation = firstPageOrientation;
    }

    /**
     * <p>
     * getCurrentPageSourceIndex.
     * </p>
     *
     * @return 1 if we are in double page mode and the current page is the right page. 0 otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public int getCurrentPageSourceIndex() throws IndexUnreachableException, DAOException {
        if (!isDoublePageMode()) {
            return 0;
        }

        PhysicalElement currentRightPage = getCurrentRightPage().orElse(null);
        if (currentRightPage != null) {
            return currentRightPage.equals(getCurrentPage()) ? 1 : 0;
        }

        return 0;
    }

    /**
     * <p>
     * getTopDocumentTitle.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTopDocumentTitle() {
        return getDocumentTitle(this.topDocument);
    }

    /**
     * <p>
     * getDocumentTitle.
     * </p>
     *
     * @param document a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDocumentTitle(StructElement document) {
        if (document == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
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

        return sb.toString();
    }

    /**
     * <p>
     * Setter for the field <code>pageLoader</code>.
     * </p>
     *
     * @param loader a {@link io.goobi.viewer.model.viewer.pageloader.IPageLoader} object.
     */
    public void setPageLoader(IPageLoader loader) {
        this.pageLoader = loader;

    }

    /**
     * <p>
     * getUsageWidgetAccessCondition.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.metadata.Metadata} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public Metadata getUsageWidgetAccessCondition() throws IndexUnreachableException, PresentationException {
        Metadata md = DataManager.getInstance().getConfiguration().getWidgetUsageLicenceTextMetadata();
        md.populate(getTopDocument(), BeanUtils.getLocale());
        return md;
    }

    /**
     * <p>
     * getCiteLinkWork.
     * </p>
     *
     * @return A persistent link to the current work
     *
     *         TODO: additional urn-resolving logic
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public String getCiteLinkWork() throws IndexUnreachableException, DAOException, PresentationException {
        if (topDocument == null) {
            return "";
        }

        String customPURL = topDocument.getMetadataValue("MD_PURL");
        if (StringUtils.isNotEmpty(customPURL)) {
            return customPURL;
        } else if (StringUtils.isNotBlank(topDocument.getMetadataValue(SolrConstants.URN))) {
            String urn = topDocument.getMetadataValue(SolrConstants.URN);
            return getPersistentUrl(urn);
        } else {
            StringBuilder url = new StringBuilder();
            boolean anchorOrGroup = topDocument.isAnchor() || topDocument.isGroup();
            PageType pageType = PageType.determinePageType(topDocument.getDocStructType(), null, anchorOrGroup, isHasPages(), false);
            if (pageType == null) {
                if (isHasPages()) {
                    pageType = PageType.viewObject;
                } else {
                    pageType = PageType.viewMetadata;
                }
            }
            url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
            url.append('/').append(pageType.getName()).append('/').append(getPi()).append('/');
            if (getRepresentativePage() != null) {
                url.append(getRepresentativePage().getOrder()).append('/');
            }
            return url.toString();
        }
    }

    /**
     * <p>
     * isDisplayCiteLinkWork.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayCiteLinkWork() {
        return topDocument != null;
    }

    /**
     * <p>
     * getCiteLinkPage.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getCiteLinkPage() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }

        String urn = currentPage.getUrn();
        return getPersistentUrl(urn);
    }

    /**
     * <p>
     * isDisplayCiteLinkPage.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isDisplayCiteLinkPage() throws IndexUnreachableException, DAOException {
        return getCurrentPage() != null;
    }

    /**
     * Creates an instance of ViewManager loaded with the record with the given identifier.
     * 
     * @param pi Record identifier
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     */
    public static ViewManager createViewManager(String pi)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + pi, null);
        if (doc == null) {
            return null;
        }

        long iddoc = Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC));
        StructElement topDocument = new StructElement(iddoc, doc);
        ViewManager ret = new ViewManager(topDocument, new LeanPageLoader(topDocument, topDocument.getNumPages()), iddoc, null, null, null);

        return ret;
    }
}
