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

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_ALTO_ZIP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_ALTO;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_PLAINTEXT;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_TEI;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PLAINTEXT_ZIP;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_TEI_LANG;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.jdom2.JDOMException;
import org.json.JSONObject;
import org.omnifaces.util.Faces;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.intranda.api.iiif.image.ImageInformation;
import de.undercouch.citeproc.CSL;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.GetPdfAction;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.servlet.model.SinglePdfRequest;
import de.unigoettingen.sub.commons.util.MimeType;
import de.unigoettingen.sub.commons.util.MimeType.UnknownMimeTypeException;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.ProcessDataResolver;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.config.filter.IFilterConfiguration;
import io.goobi.viewer.controller.sorting.AlphanumCollatorComparator;
import io.goobi.viewer.exceptions.ArchiveException;
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
import io.goobi.viewer.model.archives.ArchiveEntry;
import io.goobi.viewer.model.archives.ArchiveResource;
import io.goobi.viewer.model.calendar.CalendarView;
import io.goobi.viewer.model.citation.Citation;
import io.goobi.viewer.model.citation.CitationLink;
import io.goobi.viewer.model.citation.CitationLink.CitationLinkLevel;
import io.goobi.viewer.model.citation.CitationList;
import io.goobi.viewer.model.citation.CitationProcessorWrapper;
import io.goobi.viewer.model.citation.CitationTools;
import io.goobi.viewer.model.files.external.ExternalFilesDownloader;
import io.goobi.viewer.model.job.download.DownloadOption;
import io.goobi.viewer.model.metadata.ComplexMetadata;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataTools;
import io.goobi.viewer.model.metadata.MetadataValue;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.CopyrightIndicatorLicense;
import io.goobi.viewer.model.security.CopyrightIndicatorStatus;
import io.goobi.viewer.model.security.CopyrightIndicatorStatus.Status;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.transkribus.TranskribusJob;
import io.goobi.viewer.model.transkribus.TranskribusSession;
import io.goobi.viewer.model.transkribus.TranskribusUtils;
import io.goobi.viewer.model.variables.VariableReplacer;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import io.goobi.viewer.model.viewer.pageloader.SelectPageItem;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.model.SelectItem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Holds information about the currently open record (structure, pages, etc.). Used to reduced the size of ActiveDocumentBean.
 */
public class ViewManager implements Serializable {

    private static final long serialVersionUID = -7776362205876306849L;

    private static final Logger logger = LogManager.getLogger(ViewManager.class);

    private static final String STYLE_CLASS_WORD_SEPARATOR = "_";
    private static final int MAX_STYLECLASS_LENGTH = 100;

    private ImageDeliveryBean imageDeliveryBean;

    /** IDDOC of the top level document. */
    private final String topStructElementIddoc;
    /** IDDOC of the current level document. The initial top level document values eventually gets overridden with the image owner element's IDDOC. */
    private String currentStructElementIddoc;
    /** LOGID of the current level document. */
    private String logId;

    /** Document of the anchor element, if applicable. */
    private StructElement anchorStructElement;

    /** Top level document. */
    private StructElement topStructElement;

    /** Currently selected document. */
    private StructElement currentStructElement;

    private IPageLoader pageLoader;
    private PhysicalElement representativePage;

    /** Table of contents object. */
    private TOC toc;

    private int rotate = 0;
    private int zoomSlider;
    private int currentImageOrder = -1;
    private final List<SelectPageItem> dropdownPages = new ArrayList<>();
    private final List<SelectPageItem> dropdownFulltext = new ArrayList<>();
    private String dropdownSelected = "";
    private int currentThumbnailPage = 1;
    private String pi;
    private Boolean accessPermissionPdf = null;
    private Boolean allowUserComments = null;
    /** True if an access ticket is required before anything in this record may be viewed.. Value is set during the access permission check. */
    private boolean recordAccessTicketRequired = false;
    private List<StructElementStub> docHierarchy = null;
    private String mimeType = null;
    private Boolean filesOnly = null;
    private String opacUrl = null;
    private String contextObject = null;
    private List<String> versionHistory = null;
    private PageOrientation firstPageOrientation = PageOrientation.RIGHT;
    private int firstPdfPage;
    private int lastPdfPage;
    private CalendarView calendarView;
    private Long pagesWithFulltext = null;
    private Long pagesWithAlto = null;
    private Boolean workHasTEIFiles = null;
    private Boolean metadataViewOnly = null;
    private String citationStyle = null;
    private CitationProcessorWrapper citationProcessorWrapper;
    private ArchiveResource archiveResource = null;
    private Pair<Optional<String>, Optional<String>> archiveTreeNeighbours = Pair.of(Optional.empty(), Optional.empty());
    private List<CopyrightIndicatorStatus> copyrightIndicatorStatuses = null;
    private CopyrightIndicatorLicense copyrightIndicatorLicense = null;
    private Map<CitationLinkLevel, CitationList> citationLinks = new HashMap<>();
    private List<String> externalResourceUrls = null;
    private List<PhysicalResource> downloadResources = null;

    private PageNavigation pageNavigation = PageNavigation.SINGLE;

    /**
     * <p>
     * Constructor for ViewManager.
     * </p>
     *
     * @param topDocument a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param pageLoader a {@link io.goobi.viewer.model.viewer.pageloader.IPageLoader} object.
     * @param currentDocumentIddoc a long.
     * @param logId a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @param imageDeliveryBean a {@link io.goobi.viewer.managedbeans.ImageDeliveryBean} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public ViewManager(StructElement topDocument, IPageLoader pageLoader, String currentDocumentIddoc, String logId, String mimeType,
            ImageDeliveryBean imageDeliveryBean) throws IndexUnreachableException, PresentationException {
        this.imageDeliveryBean = imageDeliveryBean;
        this.topStructElement = topDocument;
        this.topStructElementIddoc = topDocument.getLuceneId();
        logger.trace("New ViewManager: {} / {} / {}", topDocument.getLuceneId(), currentDocumentIddoc, logId);
        this.pageLoader = pageLoader;
        this.currentStructElementIddoc = currentDocumentIddoc;
        this.logId = logId;
        if (topStructElementIddoc.equals(currentDocumentIddoc)) {
            currentStructElement = topDocument;
        } else {
            currentStructElement = new StructElement(currentDocumentIddoc);
        }
        // Set the anchor StructElement for extracting metadata later
        if (topDocument.isAnchorChild()) {
            anchorStructElement = topDocument.getParent();
        }

        currentThumbnailPage = 1;
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
        this.mimeType = mimeType;
        logger.trace("mimeType: {}", mimeType);

        this.pageNavigation = getDefaultPageNavigation(null);

        // Linked archive node
        try {
            String archiveId = getArchiveEntryIdentifier();
            if (StringUtils.isNotBlank(archiveId)) {
                DataManager.getInstance().getArchiveManager().updateArchiveList();
                this.archiveResource = DataManager.getInstance().getArchiveManager().loadArchiveForEntry(archiveId);
                this.archiveTreeNeighbours = DataManager.getInstance().getArchiveManager().findIndexedNeighbours(archiveId);
            }
        } catch (ArchiveException e) {
            logger.error("Error creating archive link for {}: {}", this.pi, e.getMessage());
        }
    }

    protected PageNavigation getDefaultPageNavigation(PageType pageType) {
        try {
            if (DataManager.getInstance().getConfiguration().isSequencePageNavigationEnabled(pageType, this.mimeType)) {
                return PageNavigation.SEQUENCE;
            } else if (DataManager.getInstance().getConfiguration().isDoublePageNavigationDefault(pageType, this.mimeType)) {
                return PageNavigation.DOUBLE;
            } else {
                return PageNavigation.SINGLE;
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Error reading default page navigation from config", e);
            return PageNavigation.SINGLE;
        }
    }

    public Pair<Optional<String>, Optional<String>> getArchiveTreeNeighbours() {
        return archiveTreeNeighbours;
    }

    public List<ArchiveEntry> getArchiveHierarchyForIdentifier(String identifier) {
        if (this.archiveResource != null) {
            return DataManager.getInstance().getArchiveManager().getArchiveHierarchyForIdentifier(this.archiveResource, identifier);
        }
        logger.trace("No archive resource found for {}", identifier);
        return Collections.emptyList();
    }

    public String getArchiveUrlForIdentifier(String identifier) {
        String url = DataManager.getInstance().getArchiveManager().getArchiveUrl(this.archiveResource, identifier);
        return url.replaceAll("\\s", "+");
    }

    private void setDoublePageModeForDropDown(boolean doublePages) {
        this.dropdownFulltext.forEach(i -> i.setDoublePageMode(doublePages));
        this.dropdownPages.forEach(i -> i.setDoublePageMode(doublePages));

    }

    public String getPageUrl(SelectItem item) {
        if (isDoublePageMode()) {
            return item.getValue().toString() + item.getValue().toString();
        }
        return item.getValue().toString();
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
        String anchorPi = null;
        if (anchorStructElement != null) {
            anchorPi = anchorStructElement.getPi();
        } else if (topStructElement.isAnchor() || topStructElement.isGroup()) {
            anchorPi = pi;
        } else if (topStructElement.isGroupMember()) {
            anchorPi = topStructElement.getGroupMemberships().get(topStructElement.getGroupIdField());
        }
        String anchorField = topStructElement.isVolume() ? SolrConstants.PI_ANCHOR : topStructElement.getGroupIdField();
        return new CalendarView(pi, anchorPi, anchorField,
                topStructElement.isAnchor() ? null : topStructElement.getMetadataValue(SolrConstants.CALENDAR_YEAR));

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
        return imageDeliveryBean.getImages().getImageUrl(null, pi, representative.getFileName());
    }

    public Map<Integer, String> getImageInfos(PageType pageType) throws IndexUnreachableException, DAOException {
        Map<Integer, String> infos = new LinkedHashMap<>();

        switch (getPageNavigation()) {
            case SINGLE:
                infos.put(getCurrentImageOrder(), getImageInfo(getCurrentPage(), pageType));
                break;
            case DOUBLE:
                getCurrentLeftPage().filter(p -> !p.isDoubleImage()).ifPresent(p -> infos.put(p.getOrder(), getImageInfo(p, pageType)));
                getCurrentRightPage().filter(p -> !p.isDoubleImage() || infos.isEmpty())
                        .ifPresent(p -> infos.put(p.getOrder(), getImageInfo(p, pageType)));
                break;
            case SEQUENCE:
                for (PhysicalElement page : this.getAllPages()) {
                    infos.put(page.getOrder(), getImageInfo(page, pageType));
                }
                break;
            default:
                break;
        }

        return infos;
    }

    public String getImageInfosAsJson(PageType pageType) throws IndexUnreachableException, DAOException {
        return JSONObject.wrap(getImageInfos(pageType)).toString();
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
        if (isDoublePageMode() && !getCurrentPage().isDoubleImage()) {
            Optional<PhysicalElement> leftPage = getCurrentLeftPage();
            Optional<PhysicalElement> rightPage = getCurrentRightPage();
            logger.trace("left page: {}", leftPage.isPresent() ? leftPage.get().getOrder() : "-");
            logger.trace("right page: {}", rightPage.isPresent() ? rightPage.get().getOrder() : "-");
            urlBuilder.append("[");
            String imageInfoLeft =
                    (leftPage.isPresent() && leftPage.get().isDoubleImage()) ? null : leftPage.map(page -> getImageInfo(page, pageType)).orElse(null);
            String imageInfoRight =
                    (rightPage.isPresent() && (rightPage.get().isDoubleImage() || rightPage.get().equals(leftPage.orElse(null)))) ? null
                            : rightPage.map(page -> getImageInfo(page, pageType)).orElse(null);
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
     * @return Optional<PhysicalElement>
     */
    public Optional<PhysicalElement> getCurrentLeftPage() {
        boolean actualPageOrderEven = this.currentImageOrder % 2 == 0;
        PageOrientation actualPageOrientation = actualPageOrderEven ? getFirstPageOrientation().opposite() : getFirstPageOrientation();
        if (topStructElement != null && topStructElement.isRtl()) {
            actualPageOrientation = actualPageOrientation.opposite();
        }
        if (actualPageOrientation.equals(PageOrientation.LEFT)) {
            return getPage(this.currentImageOrder);
        } else if (topStructElement != null && topStructElement.isRtl()) {
            return getPage(this.currentImageOrder + 1);
        } else {
            return getPage(this.currentImageOrder - 1);
        }

    }

    /**
     * @return Optional<PhysicalElement>
     */
    public Optional<PhysicalElement> getCurrentRightPage() {
        boolean actualPageOrderEven = this.currentImageOrder % 2 == 0;
        PageOrientation actualPageOrientation = actualPageOrderEven ? getFirstPageOrientation().opposite() : getFirstPageOrientation();
        if (topStructElement != null && topStructElement.isRtl()) {
            actualPageOrientation = actualPageOrientation.opposite();
        }
        if (actualPageOrientation.equals(PageOrientation.RIGHT)) {
            return getPage(this.currentImageOrder);
        } else if (topStructElement != null && topStructElement.isRtl()) {
            return getPage(this.currentImageOrder - 1);
        } else {
            return getPage(this.currentImageOrder + 1);
        }

    }

    /**
     *
     * @param page
     * @param pageType
     * @return Image URL
     * @throws ViewerConfigurationException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws URISyntaxException
     * @throws ContentLibException
     * @throws JsonProcessingException
     */
    private String getImageInfo(PhysicalElement page, PageType pageType) {
        try {
            ImageInformation info = imageDeliveryBean.getImages().getImageInformation(page, pageType);
            if (info.getWidth() * info.getHeight() == 0) {
                return UriBuilder.fromUri(info.getId()).path("info.json").build().toString();
            }
            return JsonTools.getAsJson(info);
        } catch (ContentLibException | ViewerConfigurationException | URISyntaxException | JsonProcessingException | IndexUnreachableException
                | DAOException e) {
            logger.warn("Error creating image information for {}: {}", page, e.toString());
            return imageDeliveryBean.getImages().getImageUrl(page, pageType);
        }
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
        return getImageInfo(currentPage, PageType.viewFullscreen);
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
        return getImageInfo(currentPage, PageType.editOcr);
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
                .getWatermarkUrl(Optional.ofNullable(getCurrentPage()), Optional.ofNullable(getTopStructElement()),
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
        return imageDeliveryBean.getObjects3D().getObjectUrl(pi, getCurrentPage().getFirstFileName());
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
                .getImageViewZoomScales(view,
                        Optional.ofNullable(getCurrentPage())
                                .map(page -> page.getImageType())
                                .map(type -> type.getFormat().getMimeType())
                                .orElse(null))
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
    @Deprecated(since = "24.10")
    public String getCurrentMasterImageUrl() throws IndexUnreachableException, DAOException {
        return getMasterImageUrl(Scale.MAX, getCurrentPage());
    }

    /**
     * <p>
     * getCurrentMasterImageUrl.
     * </p>
     *
     * @param scale a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @param page
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getMasterImageUrl(Scale scale, PhysicalElement page) throws IndexUnreachableException, DAOException {

        PageType pageType = Optional.ofNullable(BeanUtils.getNavigationHelper()).map(NavigationHelper::getCurrentPageType).orElse(null);
        if (pageType == null) {
            pageType = PageType.viewObject;
        }
        StringBuilder sb = new StringBuilder(imageDeliveryBean.getThumbs().getFullImageUrl(page, scale, "MASTER"));
        logger.trace("Master image URL: {}", sb);
        try {
            if (DataManager.getInstance()
                    .getConfiguration()
                    .getFooterHeight(pageType, Optional.ofNullable(page).map(PhysicalElement::getMimeType).orElse(null)) > 0) {
                sb.append("?ignoreWatermark=false");
                sb.append(imageDeliveryBean.getFooter().getWatermarkTextIfExists(page).map(text -> {
                    try {
                        return "&watermarkText=" + URLEncoder.encode(text, StringTools.DEFAULT_ENCODING);
                    } catch (UnsupportedEncodingException e) {
                        logger.error(e.getMessage());
                        return "&watermarkText=" + text;
                    }
                }).orElse(""));
                sb.append(imageDeliveryBean.getFooter().getFooterIdIfExists(getTopStructElement()).map(id -> "&watermarkId=" + id).orElse(""));
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Unable to read watermark config, ignore watermark", e);
        }
        return sb.toString();
    }

    /**
     * <p>
     * getCurrentThumbnailUrlForDownload.
     * </p>
     *
     * @param scale a {@link de.unigoettingen.sub.commons.contentlib.imagelib.transform.Scale} object.
     * @param page
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String getThumbnailUrlForDownload(Scale scale, PhysicalElement page) throws IndexUnreachableException, DAOException {

        PageType pageType = Optional.ofNullable(BeanUtils.getNavigationHelper()).map(NavigationHelper::getCurrentPageType).orElse(null);
        if (pageType == null) {
            pageType = PageType.viewObject;
        }

        StringBuilder sb = new StringBuilder(imageDeliveryBean.getThumbs().getThumbnailUrl(page, scale));
        try {
            if (DataManager.getInstance()
                    .getConfiguration()
                    .getFooterHeight(pageType, Optional.ofNullable(page).map(PhysicalElement::getMimeType).orElse(null)) > 0) {
                sb.append("?ignoreWatermark=false");
                sb.append(imageDeliveryBean.getFooter()
                        .getWatermarkTextIfExists(page)
                        .map(text -> {
                            try {
                                return "&watermarkText=" + URLEncoder.encode(text, StringTools.DEFAULT_ENCODING);
                            } catch (UnsupportedEncodingException e) {
                                logger.error(e.getMessage());
                                return "&watermarkText=" + text;
                            }
                        })
                        .orElse(""));
                sb.append(imageDeliveryBean.getFooter().getFooterIdIfExists(getTopStructElement()).map(id -> "&watermarkId=" + id).orElse(""));
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Unable to read watermark config, ignore watermark", e);
        }
        return sb.toString();
    }

    /**
     * @param view
     * @param size
     * @return Image URL
     */
    private String getCurrentImageUrl(PageType view, int size) {
        StringBuilder sb = new StringBuilder(imageDeliveryBean.getThumbs().getThumbnailUrl(getCurrentPage(), size, size));
        try {
            if (DataManager.getInstance().getConfiguration().getFooterHeight(view, getCurrentPage().getImageType().getFormat().getMimeType()) > 0) {
                sb.append("?ignoreWatermark=false");
                sb.append(imageDeliveryBean.getFooter().getWatermarkTextIfExists(getCurrentPage()).map(text -> {
                    try {
                        return "&watermarkText=" + URLEncoder.encode(text, StringTools.DEFAULT_ENCODING);
                    } catch (UnsupportedEncodingException e) {
                        logger.error(e.getMessage());
                        return "&watermarkText=" + text;
                    }
                }).orElse(""));
                sb.append(imageDeliveryBean.getFooter().getFooterIdIfExists(getTopStructElement()).map(id -> "&watermarkId=" + id).orElse(""));
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Unable to read watermark config, ignore watermark", e);
        }
        return sb.toString();
    }

    /**
     * <p>
     * getPageDownloadUrl.
     * </p>
     *
     * @param option
     * @param page
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws ViewerConfigurationException
     */
    public String getPageDownloadUrl(final DownloadOption option, PhysicalElement page)
            throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("getPageDownloadUrl: {}", option);
        DownloadOption useOption = option;
        if (useOption == null || !useOption.isValid()) {
            useOption = getDownloadOptionsForPage(page).stream()
                    .findFirst()
                    .orElse(null);
            if (useOption == null) {
                return "";
            }
        }
        Scale scale;
        if (DownloadOption.MAX == useOption.getBoxSizeInPixel()) {
            scale = Scale.MAX;
        } else if (useOption.getBoxSizeInPixel() == DownloadOption.NONE) {
            throw new IllegalArgumentException("Invalid box size: " + useOption.getBoxSizeInPixel());
        } else {
            scale = new Scale.ScaleToBox(useOption.getBoxSizeInPixel());
        }
        switch (useOption.getFormat().toLowerCase()) {
            case "jpg":
            case "jpeg":
                return getThumbnailUrlForDownload(scale, page);
            default:
                // If master image URL is an empty string, check the indexed mime type (i.e. "image/tif" instead of "image/tiff")!
                return getMasterImageUrl(scale, page);
        }
    }

    /**
     * 
     * @param configuredOptions
     * @param origImageSize
     * @param configuredMaxSize
     * @param imageFilename
     * @return List<DownloadOption>
     */
    public static List<DownloadOption> getDownloadOptionsForImage(
            List<DownloadOption> configuredOptions,
            Dimension origImageSize,
            Dimension configuredMaxSize,
            String imageFilename) {

        List<DownloadOption> options = new ArrayList<>();

        int maxWidth;
        int maxHeight;
        Dimension maxSize;
        if (origImageSize != null && origImageSize.height * origImageSize.width > 0) {
            maxWidth = configuredMaxSize.width > 0 ? Math.min(origImageSize.width, configuredMaxSize.width) : origImageSize.width;
            maxHeight = configuredMaxSize.height > 0 ? Math.min(origImageSize.height, configuredMaxSize.height) : origImageSize.height;
            maxSize = new Dimension(maxWidth, maxHeight);
        } else {
            maxWidth = configuredMaxSize.width;
            maxHeight = configuredMaxSize.height;
            maxSize = configuredMaxSize;
        }

        for (DownloadOption option : configuredOptions) {
            try {
                Dimension dim = option.getBoxSizeInPixel();
                if (dim == DownloadOption.MAX) {
                    Scale scale = new Scale.ScaleToBox(maxSize);
                    if (origImageSize != null && (origImageSize.height > maxHeight || origImageSize.width > maxWidth)) {
                        Dimension size = scale.scale(origImageSize);
                        options.add(new DownloadOption(option.getLabel(), getImageFormat(option.getFormat(), imageFilename), size));
                    } else {
                        options.add(new DownloadOption(option.getLabel(), getImageFormat(option.getFormat(), imageFilename), DownloadOption.MAX));
                    }
                } else if (dim.width * dim.height == 0 || (maxWidth > 0 && maxWidth < dim.width) || (maxHeight > 0 && maxHeight < dim.height)) {
                    // nothing
                    continue; //NOSONAR Checkstyle tweak
                } else if (origImageSize == null || origImageSize.height * origImageSize.width == 0) {
                    options.add(new DownloadOption(option.getLabel(), getImageFormat(option.getFormat(), imageFilename), option.getBoxSizeInPixel()));
                } else {
                    Scale scale = new Scale.ScaleToBox(option.getBoxSizeInPixel());
                    Dimension size = scale.scale(origImageSize);
                    options.add(new DownloadOption(option.getLabel(), getImageFormat(option.getFormat(), imageFilename), size));
                }
            } catch (IllegalRequestException e) {
                //attempting scale beyond original size. Ignore
            }
        }
        return options;
    }

    /**
     * 
     * @param page
     * @return List<DownloadOption>
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public List<DownloadOption> getDownloadOptionsForPage(PhysicalElement page)
            throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (page != null && page.isHasImage()) {
            List<DownloadOption> configuredOptions = DataManager.getInstance().getConfiguration().getSidebarWidgetDownloadsPageDownloadOptions();
            String imageFilename = page.getFileName();
            Dimension maxSize = new Dimension(
                    page.isAccessPermissionImageZoom() ? DataManager.getInstance().getConfiguration().getViewerMaxImageWidth()
                            : DataManager.getInstance().getConfiguration().getUnzoomedImageAccessMaxWidth(),
                    DataManager.getInstance().getConfiguration().getViewerMaxImageHeight());
            if (this.imageDeliveryBean.getIiif().isIIIFUrl(page.getFileName())) {
                return getDownloadOptionsForImage(configuredOptions, null, maxSize, imageFilename);
            }
            Dimension imageSize = new Dimension(page.getImageWidth(), page.getImageHeight());
            return getDownloadOptionsForImage(configuredOptions, imageSize, maxSize, imageFilename);
        }

        return Collections.emptyList();
    }

    /**
     * return the current image format if argument is 'MASTER', or the argument itself otherwise
     *
     * @param format
     * @param imageFilename
     * @return Image format
     */
    public static String getImageFormat(String format, String imageFilename) {
        if (format != null && format.equalsIgnoreCase("master")) {
            return Optional.ofNullable(imageFilename)
                    .map(ImageFileFormat::getImageFileFormatFromFileExtension)
                    .map(ImageFileFormat::name)
                    .orElse(format);
        }

        return format;
    }

    /**
     * <p>
     * getMasterImageUrlForDownload.
     * </p>
     *
     * @param boxSizeInPixel
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @Deprecated(since = "24.10")
    public String getMasterImageUrlForDownload(String boxSizeInPixel) throws IndexUnreachableException, DAOException {
        if (boxSizeInPixel == null) {
            throw new IllegalArgumentException("boxSizeInPixel may not be null");
        }

        Scale scale;
        if (boxSizeInPixel.equalsIgnoreCase(Scale.MAX_SIZE) || boxSizeInPixel.equalsIgnoreCase(Scale.FULL_SIZE)) {
            scale = Scale.MAX;
        } else if (boxSizeInPixel.matches("\\d{1,9}")) {
            scale = new Scale.ScaleToBox(Integer.valueOf(boxSizeInPixel), Integer.valueOf(boxSizeInPixel));
        } else {
            throw new IllegalArgumentException("Not a valid size parameter: " + boxSizeInPixel);
        }

        return getMasterImageUrl(scale, getCurrentPage());
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
        if (coordStrings != null && !coordStrings.isEmpty()) {
            for (String string : coordStrings) {
                coords.add(Arrays.asList(string.split(",")));
            }
        }

        return coords;
    }

    /**
     * 
     * @param currentImg
     * @return List<String>
     * @throws ViewerConfigurationException
     */
    private List<String> getSearchResultCoords(PhysicalElement currentImg) throws ViewerConfigurationException {
        if (currentImg == null) {
            return Collections.emptyList();
        }
        List<String> coords = null;
        SearchBean searchBean = BeanUtils.getSearchBean();
        if (searchBean != null && (searchBean.getCurrentSearchFilterString() == null
                || searchBean.getCurrentSearchFilterString().equals(SearchHelper.SEARCH_FILTER_ALL.getLabel())
                || searchBean.getCurrentSearchFilterString().equals("filter_" + SolrConstants.FULLTEXT))) {
            logger.trace("Adding word coords to page {}: {}", currentImg.getOrder(), searchBean.getSearchTerms());
            int proximitySearchDistance = searchBean.getProximitySearchDistance();
            coords = currentImg.getWordCoords(searchBean.getSearchTerms().get(SolrConstants.FULLTEXT), proximitySearchDistance, rotate);
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
     * @return URL to the representative image as {@link String}
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public String getRepresentativeImageUrl(int width, int height) throws IndexUnreachableException, PresentationException, DAOException {
        if (getRepresentativePage() == null) {
            return null;
        }

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
        return topStructElement.getMetadataFields().containsKey(SolrConstants.URN)
                || topStructElement.getFirstPageFieldValue(SolrConstants.IMAGEURN) != null;
    }

    /**
     * <p>
     * isHasVolumes.
     * </p>
     *
     * @return true if this is an anchor record and has indexed volumes; false otherwise
     */
    public boolean isHasVolumes() {
        if (!topStructElement.isAnchor()) {
            return false;
        }

        return topStructElement.getNumVolumes() > 0;
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
     * @should return true if mime type application
     */
    public boolean isFilesOnly() throws IndexUnreachableException, DAOException {
        // TODO check all files for mime type?
        if (filesOnly == null) {
            BaseMimeType baseMimeType = BaseMimeType.getByName(mimeType);
            if (BaseMimeType.APPLICATION.equals(baseMimeType)) {
                filesOnly = true;
            } else {
                boolean childIsFilesOnly = isChildFilesOnly();
                PhysicalElement firstPage = pageLoader.getPage(pageLoader.getFirstPageOrder());
                filesOnly =
                        childIsFilesOnly || (isHasPages() && firstPage != null && firstPage.getMimeType().equals(BaseMimeType.APPLICATION.getName()));
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
     * @deprecated replc
     */
    @Deprecated(since = "25.02")
    public boolean isBornDigital() throws IndexUnreachableException, DAOException {
        return isHasPages() && isFilesOnly();
    }

    public boolean isHasExternalResources() throws IndexUnreachableException {
        return Optional.ofNullable(getExternalResourceUrls()).map(list -> !list.isEmpty()).orElse(false);
    }

    public boolean isHasDownloadResources() throws PresentationException, IndexUnreachableException {
        return Optional.ofNullable(getDownloadResources()).map(list -> !list.isEmpty()).orElse(false);
    }

    public List<PhysicalResource> getDownloadResources() throws PresentationException, IndexUnreachableException {
        if (this.downloadResources == null) {
            this.downloadResources = loadDownloadResources();
        }
        return this.downloadResources;
    }

    private List<PhysicalResource> loadDownloadResources() throws PresentationException, IndexUnreachableException {
        String query = "+PI_TOPSTRUCT:%s +DOCTYPE:DOWNLOAD_RESOURCE".formatted(pi);
        List<SolrDocument> docs = DataManager.getInstance().getSearchIndex().getDocs(query, null);
        if (docs != null) {
            return docs.stream().map(PhysicalResource::create).filter(Objects::nonNull).toList();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     *
     * @return true if current record is anchor or group and its first child volume is of application mime type; false otherwise
     * @throws IndexUnreachableException
     */
    private boolean isChildFilesOnly() throws IndexUnreachableException {
        boolean childIsFilesOnly = false;
        if (currentStructElement != null && (currentStructElement.isAnchor() || currentStructElement.isGroup())) {
            try {
                String localMimeType = currentStructElement.getFirstVolumeFieldValue(SolrConstants.MIMETYPE);
                if (BaseMimeType.APPLICATION.getName().equals(localMimeType)) {
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
    public PhysicalElement getCurrentPage() {
        return getPage(currentImageOrder).orElse(null);
    }

    /**
     * @param step
     * @return {@link PhysicalElement}
     * @throws IndexUnreachableException
     */
    public PhysicalElement getNextPrevPage(int step) throws IndexUnreachableException {
        int index = currentImageOrder + step;
        if (index <= 0 || index >= pageLoader.getNumPages()) {
            return null;
        }
        return getPage(index).orElse(null);
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
    public Optional<PhysicalElement> getPage(int order) {
        try {
            if (pageLoader != null && pageLoader.getPage(order) != null) {
                return Optional.ofNullable(pageLoader.getPage(order));
            }
        } catch (IndexUnreachableException e) {
            logger.error("Error getting current page {}", e.toString());
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
            String thumbnailName = topStructElement.getMetadataValue(SolrConstants.THUMBNAIL);
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
     * Getter for the paginator or the direct page number input field
     *
     * @return currentImageNo
     */
    public int getCurrentImageOrderForPaginator() {
        return getCurrentImageOrder();
    }

    /**
     * Setter for the direct page number input field
     *
     * @param currentImageOrder a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws IDDOCNotFoundException
     */
    public void setCurrentImageOrderForPaginator(int currentImageOrder)
            throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        logger.trace("setCurrentImmageNoForPaginator({})", currentImageOrder);
        setCurrentImageOrder(currentImageOrder);
    }

    /**
     * <p>
     * currentImageOrder.
     * </p>
     *
     * @return the currentImageOrder
     */
    public int getCurrentImageOrder() {
        return currentImageOrder;
    }

    public void setCurrentImageOrderPerScript()
            throws NumberFormatException, IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        String order = Faces.getRequestParameter("order");
        if (StringUtils.isNotBlank(order) && order.matches("\\d+")) {
            setCurrentImageOrder(Integer.parseInt(order));
        } else {
            throw new PresentationException("Order parameter invalid: " + order);
        }
    }

    /**
     * <p>
     * currentPageOrder.
     * </p>
     *
     * @param currentImageOrder the currentImageOrder to set
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws RecordNotFoundException
     * @throws PresentationException
     * @throws IDDOCNotFoundException
     */
    public void setCurrentImageOrder(final int currentImageOrder) throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        logger.trace("setCurrentImageNo: {}", currentImageOrder);
        if (pageLoader == null) {
            return;
        }
        int useOrder = currentImageOrder;
        if (useOrder < pageLoader.getFirstPageOrder()) {
            useOrder = pageLoader.getFirstPageOrder();
        } else if (useOrder >= pageLoader.getLastPageOrder()) {
            useOrder = pageLoader.getLastPageOrder();
        }
        this.currentImageOrder = useOrder;

        if (StringUtils.isEmpty(logId)) {
            String iddoc = pageLoader.getOwnerIddocForPage(useOrder);
            // Set the currentDocumentIddoc to the IDDOC of the image owner document, but only if no specific document LOGID has been requested
            if (iddoc != null) {
                currentStructElementIddoc = iddoc;
                logger.trace("currentDocumentIddoc: {} ({})", currentStructElementIddoc, pi);
            } else if (isHasPages()) {
                logger.warn("currentDocumentIddoc not found for '{}', page {}", pi, useOrder);
                throw new IDDOCNotFoundException("currentElementIddoc not found for '" + pi + "', page " + useOrder);
            }
        } else {
            // If a specific LOGID has been requested, look up its IDDOC
            logger.trace("Selecting currentElementIddoc by LOGID: {} ({})", logId, pi);
            String iddoc = DataManager.getInstance().getSearchIndex().getIddocByLogid(getPi(), logId);
            if (iddoc != null) {
                currentStructElementIddoc = iddoc;
            } else {
                logger.trace("currentElementIddoc not found for '{}', LOGID: {}", pi, logId);
            }
            // Reset LOGID so that the same TOC element doesn't stay highlighted when flipping pages
            logId = null;
        }
        if (currentStructElement == null || !Objects.equals(currentStructElement.getLuceneId(), currentStructElementIddoc)) {
            setCurrentStructElement(new StructElement(currentStructElementIddoc));
        }
    }

    protected void setPageNavigation(PageNavigation navigation) {
        this.pageNavigation = navigation;
    }

    public void updatePageNavigation(PageType pageType) {
        this.pageNavigation = calculateCurrentPageNavigation(pageType);
    }

    protected PageNavigation calculateCurrentPageNavigation(PageType pageType) {
        try {
            PageNavigation defaultPageNavigation = getDefaultPageNavigation(pageType);
            if (this.pageNavigation == defaultPageNavigation) {
                return this.pageNavigation;
            } else if (defaultPageNavigation == PageNavigation.SEQUENCE) {
                return defaultPageNavigation;
            } else if (this.pageNavigation == PageNavigation.SINGLE) {
                return this.pageNavigation;
            } else if (this.pageNavigation == PageNavigation.DOUBLE
                    && DataManager.getInstance().getConfiguration().isDoublePageNavigationEnabled(pageType, this.mimeType)) {
                return this.pageNavigation;
            } else {
                return defaultPageNavigation;
            }

        } catch (ViewerConfigurationException | NullPointerException | IllegalArgumentException e) {
            logger.error("Failed to set view mode: {}", e.toString());
            return PageNavigation.SINGLE;
        }
    }

    /**
     * Main method for setting the current page(s) in this ViewManager. If the given String cannot be parsed to an integer the current image order is
     * set to 1
     *
     * @param currentImageOrderString A string containing a single page number or a range of two pages
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws IDDOCNotFoundException
     */
    public void setCurrentImageOrderString(String currentImageOrderString)
            throws IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        int newImageOrder = 1;
        if (currentImageOrderString != null && currentImageOrderString.contains("-")) {
            String[] orderSplit = currentImageOrderString.split("-");
            newImageOrder = StringTools.parseInt(orderSplit[0]).orElse(1);
        } else {
            newImageOrder = StringTools.parseInt(currentImageOrderString).orElse(1);
        }

        setCurrentImageOrder(newImageOrder);
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
            setCurrentImageOrder(currentImageOrder);
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
            setCurrentImageOrder(currentImageOrder);
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
        setCurrentImageOrder(pageLoader.getFirstPageOrder());
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
        setCurrentImageOrder(pageLoader.getLastPageOrder());
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
     *
     * @return Last page number
     */
    public int getLastPageOrder() {
        if (pageLoader == null) {
            return -1;
        }
        return pageLoader.getLastPageOrder();
    }

    /**
     *
     * @return First page number
     */
    public int getFirstPageOrder() {
        if (pageLoader == null) {
            return -1;
        }
        return pageLoader.getFirstPageOrder();
    }

    /**
     * <p>
     * Getter for the field <code>dropdownPages</code>.
     * </p>
     *
     * @return the dropdownPages
     */
    public List<SelectPageItem> getDropdownPages() {
        return dropdownPages;
    }

    public List<String> getDropdownPagesAsJson() {
        return getDropdownPages().stream().map(item -> {
            JSONObject obj = new JSONObject();
            obj.put("label", item.getLabel());
            obj.put("description", item.getDescription());
            obj.put("value", item.getValue());
            return obj.toString();
        }).toList();
    }

    /**
     * <p>
     * Getter for the field <code>dropdownFulltext</code>.
     * </p>
     *
     * @return the dropdownPages
     */
    public List<SelectPageItem> getDropdownFulltext() {
        return dropdownFulltext;
    }

    public List<String> getDropdownFulltextAsJson() {
        return getDropdownFulltext().stream().map(item -> {
            JSONObject obj = new JSONObject();
            obj.put("label", item.getLabel());
            obj.put("description", item.getDescription());
            obj.put("value", item.getValue());
            return obj.toString();
        }).toList();
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
     * Fist thumbnail index +not+ on the current page anymore
     * 
     * @param thumbnailsPerPage
     * @return Index of the last thumbnail on the current page
     */
    private int getLastDisplayedThumbnailIndex(int thumbnailsPerPage) {
        return getFirstDisplayedThumbnailIndex(thumbnailsPerPage) + thumbnailsPerPage;
    }

    /**
     * @param thumbnailsPerPage
     * @return Index of the first thumbnail on the current page
     */
    private int getFirstDisplayedThumbnailIndex(int thumbnailsPerPage) {
        int i = pageLoader.getFirstPageOrder();
        if (currentThumbnailPage > 1) {
            i = (currentThumbnailPage - 1) * thumbnailsPerPage + pageLoader.getFirstPageOrder();
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
        if (PageNavigation.DOUBLE.equals(getPageNavigation())) {
            setDropdownSelected(String.valueOf(currentImageOrder) + "-" + currentImageOrder);
        } else {
            setDropdownSelected(String.valueOf(currentImageOrder));
        }
    }

    public PageNavigation getPageNavigation() {
        return pageNavigation;
    }

    /**
     * <p>
     * dropdownAction.
     * </p>
     *
     * @param event {@link jakarta.faces.event.ValueChangeEvent}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws java.lang.NumberFormatException if any.
     * @throws IDDOCNotFoundException
     */
    public void dropdownAction(ValueChangeEvent event)
            throws NumberFormatException, IndexUnreachableException, PresentationException, IDDOCNotFoundException {
        setCurrentImageOrder(Integer.valueOf((String) event.getNewValue()) - 1);
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
        if (pageLoader == null) {
            return "0";
        }

        double im = pageLoader.getNumPages();
        double thumb = DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage();
        int answer = (int) Math.floor(im / thumb);
        if (im % thumb != 0 || answer == 0) {
            answer++;
        }

        return String.valueOf(answer);
    }

    /**
     * <p>
     * getLinkForDFGViewer.
     * </p>
     *
     * @return DFG Viewer link
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should construct default url correctly
     * @should construct url from custom field correctly
     */
    public String getLinkForDFGViewer() throws IndexUnreachableException {
        if (topStructElement != null && SolrConstants.SOURCEDOCFORMAT_METS.equals(topStructElement.getSourceDocFormat()) && isHasPages()) {
            String metsUrl = getSourceFileResolverUrl();

            String urlField = DataManager.getInstance().getConfiguration().getDfgViewerSourcefileField();
            if (StringUtils.isNotBlank(urlField)) {
                // If there's a configured metadata field containing the DfG Viewer record URL, embed the custom URL instead
                String url = topStructElement.getMetadataValue(DataManager.getInstance().getConfiguration().getDfgViewerSourcefileField());
                if (StringUtils.isNotBlank(url)) {
                    logger.trace("Found DfG Viewer URL: {}", url);
                    metsUrl = url;
                }
            }

            try {
                return new StringBuilder()
                        .append(DataManager.getInstance().getConfiguration().getDfgViewerUrl())
                        .append(URLEncoder.encode(metsUrl, "utf-8"))
                        .append("&set[image]=")
                        .append(currentImageOrder)
                        .toString();
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
     * @return METS resolver link
     * @deprecated Use ViewManager.getSourceFileResolverUrl()
     */
    @Deprecated(since = "24.08")
    public String getMetsResolverUrl() {
        return getSourceFileResolverUrl();
    }

    /**
     * <p>
     * getLidoResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @deprecated Use ViewManager.getSourceFileResolverUrl()
     */
    @Deprecated(since = "24.08")
    public String getLidoResolverUrl() {
        return getSourceFileResolverUrl();
    }

    /**
     * <p>
     * getDenkxwebResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @deprecated Use ViewManager.getSourceFileResolverUrl()
     */
    @Deprecated(since = "24.08")
    public String getDenkxwebResolverUrl() {
        return getSourceFileResolverUrl();
    }

    /**
     * <p>
     * getDublinCoreResolverUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @deprecated Use ViewManager.getSourceFileResolverUrl()
     */
    @Deprecated(since = "24.08")
    public String getDublinCoreResolverUrl() {
        return getSourceFileResolverUrl();
    }

    /**
     * 
     * @return Source file resolver URL for the current record identifier
     */
    public String getSourceFileResolverUrl() {
        try {
            String url = DataManager.getInstance().getConfiguration().getSourceFileUrl();
            if (StringUtils.isNotEmpty(url)) {
                return url + getPi();
            }
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/sourcefile?id=" + getPi();
        } catch (NullPointerException | IllegalStateException | IndexUnreachableException e) {
            logger.error("Could not get source file resolver URL for {}.", topStructElementIddoc);
            Messages.error("errGetCurrUrl");
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/sourcefile?id=" + 0;
    }

    /**
     * <p>
     * getAnchorMetsResolverUrl.
     * </p>
     *
     * @return METS resolver URL for the anchor; null if no parent PI found (must be null, otherwise an empty link will be displayed).
     */
    public String getAnchorMetsResolverUrl() {
        if (anchorStructElement != null) {
            String parentPi = anchorStructElement.getMetadataValue(SolrConstants.PI);
            String url = DataManager.getInstance().getConfiguration().getSourceFileUrl();
            if (StringUtils.isNotEmpty(url)) {
                return url + parentPi;
            }
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
        String localPi = getPi();
        if (localPi != null) {
            return DataManager.getInstance()
                    .getRestApiManager()
                    .getContentApiManager()
                    .map(urls -> urls.path(RECORDS_RECORD, RECORDS_ALTO_ZIP).params(localPi).build())
                    .orElse("");
        }

        return "";
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
        String localPi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_RECORD, RECORDS_PLAINTEXT_ZIP).params(localPi).build())
                .orElse("");
    }

    /**
     * Return the url to a REST service delivering a TEI document containing the text of all pages
     *
     * @return the TEI REST url
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getTeiUrlForAllPages() throws ViewerConfigurationException, IndexUnreachableException {
        return getTeiUrlForAllPages(BeanUtils.getLocale().getLanguage());
    }

    /**
     * 
     * @param language
     * @return TEI URL for given language
     * @throws IndexUnreachableException
     */
    public String getTeiUrlForAllPages(String language) throws IndexUnreachableException {
        String localPi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_RECORD, RECORDS_TEI_LANG)
                        .params(localPi, language)
                        .build())
                .orElse("");
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
        String plaintextFilename = null;
        try {
            plaintextFilename = FileTools.getFilenameFromPathString(getCurrentPage().getFulltextFileName());
        } catch (FileNotFoundException e) {
            logger.trace("FULLTEXT not found: {}", e.getMessage());
        }
        String altoFilename = null;
        try {
            altoFilename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        } catch (FileNotFoundException e) {
            logger.trace("ALTO not found: {}", e.getMessage());
        }
        String filenameToUse = StringUtils.isNotBlank(plaintextFilename) ? plaintextFilename : altoFilename;
        if (StringUtils.isBlank(filenameToUse)) {
            return "";
        }

        String localPi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_FILES, RECORDS_FILES_TEI)
                        .params(localPi, filenameToUse)
                        .build())
                .orElse("");
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
        String filename;
        try {
            filename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        } catch (FileNotFoundException | NullPointerException e) {
            return "";
        }
        String localPi = getPi();
        if (StringUtils.isNoneBlank(pi, filename)) {
            return DataManager.getInstance()
                    .getRestApiManager()
                    .getContentApiManager()
                    .map(urls -> urls.path(RECORDS_FILES, RECORDS_FILES_ALTO)
                            .params(localPi, filename)
                            .build())
                    .orElse("");
        }

        return "";
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
        String plaintextFilename = null;
        try {
            plaintextFilename = FileTools.getFilenameFromPathString(getCurrentPage().getFulltextFileName());
        } catch (FileNotFoundException e) {
            logger.trace("FULLTEXT not found: {}", e.getMessage());
        }
        String altoFilename = null;
        try {
            altoFilename = FileTools.getFilenameFromPathString(getCurrentPage().getAltoFileName());
        } catch (FileNotFoundException e) {
            logger.trace("ALTO not found: {}", e.getMessage());
        }
        String filenameToUse = StringUtils.isNotBlank(plaintextFilename) ? plaintextFilename : altoFilename;
        if (StringUtils.isBlank(filenameToUse)) {
            return "";
        }

        String localPi = getPi();
        return DataManager.getInstance()
                .getRestApiManager()
                .getContentApiManager()
                .map(urls -> urls.path(RECORDS_FILES, RECORDS_FILES_PLAINTEXT)
                        .params(localPi, filenameToUse)
                        .build())
                .orElse("");
    }

    /**
     * Returns an external download URL, if once exists in MD2_DOWNLOAD_URL.
     * 
     * @return url if exists; null otherwise
     * @should return correct value
     */
    public String getExternalDownloadUrl() {
        return topStructElement != null ? topStructElement.getMetadataValue(SolrConstants.DOWNLOAD_URL_EXTERNAL) : null;
    }

    /**
     * Returns the pdf download link for the current document
     *
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws URISyntaxException
     */
    public String getPdfDownloadLink() throws IndexUnreachableException, PresentationException, ViewerConfigurationException, URISyntaxException {
        return getPdfDownloadLink(null);
    }

    /**
     * Returns the pdf download link for the current document, allowing to attach a number of query parameters to it
     *
     * @param queryParams
     * @return {@link java.lang.String}
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws URISyntaxException
     */
    public String getPdfDownloadLink(List<List<String>> queryParams)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException, URISyntaxException {
        String uriString = imageDeliveryBean.getPdf().getPdfUrl(getTopStructElement(), "");
        uriString = NetTools.addQueryParameters(uriString, queryParams);
        return uriString;
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
        return imageDeliveryBean.getPdf().getPdfUrl(getTopStructElement(), currentPage);
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
        StructElement currentStruct = getCurrentStructElement();
        return imageDeliveryBean.getPdf().getPdfUrl(currentStruct, currentStruct.getLabel());

    }

    /**
     * Returns the pdf download link for the current struct element, allowing to add a number of query parameters to it
     * 
     * @param queryParams
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public String getPdfStructDownloadLink(List<List<String>> queryParams)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException, URISyntaxException {
        StructElement currentStruct = getCurrentStructElement();
        String uriString = imageDeliveryBean.getPdf().getPdfUrl(currentStruct, currentStruct.getLabel());
        uriString = NetTools.addQueryParameters(uriString, queryParams);
        return uriString;
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

        List<PhysicalElement> pages = new ArrayList<>();
        for (int i = firstPdfPage; i <= lastPdfPage; ++i) {
            PhysicalElement page = pageLoader.getPage(i);
            pages.add(page);
        }
        PhysicalElement[] pageArr = new PhysicalElement[pages.size()];
        return imageDeliveryBean.getPdf().getPdfUrl(getTopStructElement(), pages.toArray(pageArr));
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
            if (topStructElement == null || !topStructElement.isWork() || !isHasPages()) {
                return false;
            }
            if (!BaseMimeType.isImageOrPdfDownloadAllowed(topStructElement.getMetadataValue(SolrConstants.MIMETYPE))) {
                return false;
            }
        } catch (IndexUnreachableException e) {
            logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            return false;
        }
        // Only allow PDF downloads for records coming from METS files
        // TODO Allow METS_MARC once supported
        if (!SolrConstants.SOURCEDOCFORMAT_METS.equals(topStructElement.getSourceDocFormat())) {
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

    public boolean isAccessPermissionExternalResources() throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getExternalContext() != null) {
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            AccessPermission access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null,
                    IPrivilegeHolder.PRIV_DOWNLOAD_BORN_DIGITAL_FILES, request);
            return access.isGranted();
        }
        logger.trace("FacesContext not found");
        return false;
    }

    /**
     *
     * @param privilege Privilege name to check
     * @return true if current user has the privilege for this record; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isAccessPermission(String privilege) throws IndexUnreachableException, DAOException {
        // logger.trace("isAccessPermission: {}", privilege); //NOSONAR Debug
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        try {
            return AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, privilege, request).isGranted();
        } catch (RecordNotFoundException e) {
            return false;
        }
    }

    /**
     * Reset the permissions for writing user comments. They will be evaluated again on the next call to {@link #isAllowUserComments()}
     */
    public void resetAllowUserComments() {
        this.allowUserComments = null;
    }

    /**
     * @return the allowUserComments
     */
    public Boolean isAllowUserComments() {
        return allowUserComments;
    }

    /**
     * @param allowUserComments the allowUserComments to set
     */
    public void setAllowUserComments(Boolean allowUserComments) {
        this.allowUserComments = allowUserComments;
    }

    /**
     * 
     * @return true if a download ticket requirement is present and not yet satisfied; false otherwise
     */
    public boolean isRecordAccessTicketRequired() {
        // logger.trace("isRecordAccessTicketRequired: {}", recordAccessTicketRequired); //NOSONAR Debug

        // If license requires a download ticket, check agent session for loaded ticket
        if (Boolean.TRUE.equals(recordAccessTicketRequired) && FacesContext.getCurrentInstance() != null
                && FacesContext.getCurrentInstance().getExternalContext() != null) {
            boolean hasTicket = AccessConditionUtils.isHasDownloadTicket(pi, BeanUtils.getSession());
            logger.trace("User has ticket: {}", hasTicket); //NOSONAR Debug
            return !hasTicket;
        }

        return recordAccessTicketRequired;
    }

    /**
     * @param recordAccessTicketRequired the recordAccessTicketRequired to set
     */
    public void setRecordAccessTicketRequired(Boolean recordAccessTicketRequired) {
        this.recordAccessTicketRequired = recordAccessTicketRequired;
    }

    /**
     * <p>
     * isDisplayMetadataPdfLink.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayMetadataPdfLink() {
        return topStructElement != null && topStructElement.isWork() && DataManager.getInstance().getConfiguration().isMetadataPdfEnabled()
                && isAccessPermissionPdf();
    }

    /**
     * Convenience method that checks whether only the metadata view link is displayed for this record (i.e. criteria for all other links are not
     * met).
     *
     * @return true if loaded record only contains metadata and doesn't support over views; false otherwise
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public boolean isMetadataViewOnly() throws IndexUnreachableException, DAOException, PresentationException {
        if (metadataViewOnly == null) {
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
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetObjectViewLinkVisible() && isHasPages() && !isFilesOnly();
    }

    /**
     *
     * @return true if calendar view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public boolean isDisplayCalendarViewLink() throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetCalendarViewLinkVisible() && calendarView != null
                && calendarView.isDisplay();
    }

    /**
     *
     * @return true if TOC view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isDisplayTocViewLink() throws IndexUnreachableException, DAOException {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetTocViewLinkVisible() && !isFilesOnly() && topStructElement != null
                && !topStructElement.isLidoRecord() && toc != null
                && toc.isHasChildren();
    }

    /**
     *
     * @return true if thumbnail view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isDisplayThumbnailViewLink() throws IndexUnreachableException, DAOException {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetThumbsViewLinkVisible()
                && pageLoader != null && pageLoader.getNumPages() > 1 && !isFilesOnly();
    }

    /**
     *
     * @return true if metadata view link may be displayed; false otherwise
     */
    public boolean isDisplayMetadataViewLink() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetMetadataViewLinkVisible() && topStructElement != null
                && !topStructElement.isGroup();
    }

    /**
     *
     * @return true if full-text view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public boolean isDisplayFulltextViewLink() throws IndexUnreachableException, DAOException, PresentationException {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetFulltextLinkVisible() && topStructElement != null
                && ((topStructElement.isFulltextAvailable()
                        && !isFilesOnly()
                        && getCurrentPage() != null
                        && getCurrentPage().isFulltextAccessPermission()) || isRecordHasTEIFiles());
        // TODO tweak conditions as necessary
    }

    /**
     *
     * @return true if external full-text link may be displayed; false otherwise
     */
    public boolean isDisplayExternalFulltextLink() {
        return topStructElement != null
                && topStructElement.getMetadataValue("MD_LOCATION_URL_EXTERNALFULLTEXT") != null && getCurrentPage() != null
                && getCurrentPage().isFulltextAccessPermission();
    }

    /**
     *
     * @return true if NER view link may be displayed; false otherwise
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public boolean isDisplayNerViewLink() throws IndexUnreachableException, PresentationException {
        return topStructElement != null && topStructElement.isNerAvailable();
    }

    /**
     *
     * @return true if NER view link may be displayed; false otherwise
     */
    public boolean isDisplayExternalResolverLink() {
        return topStructElement != null
                && topStructElement.getMetadataValue("MD_LOCATION_URL_EXTERNALRESOLVER") != null;
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
        if (currentStructElement != null && opacUrl == null) {
            try {
                StructElement topStruct = currentStructElement.getTopStruct();
                if (topStruct != null) {
                    opacUrl = topStruct.getMetadataValue(SolrConstants.OPACURL);
                }
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
            }
        }

        return opacUrl;
    }

    /**
     * Returns the main title of the current volume's anchor, if available.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAnchorTitle() {
        if (anchorStructElement != null) {
            return anchorStructElement.getMetadataValue(SolrConstants.TITLE);
        }

        return null;
    }

    /**
     * Returns the main title of the current volume.
     *
     * @return The volume's main title.
     */
    public String getVolumeTitle() {
        if (topStructElement != null) {
            return topStructElement.getMetadataValue(SolrConstants.TITLE);
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
     * @param threshold
     * @return true if percentage of pages with full-text is below given threshold; false otherwise
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should return true if there are no pages
     */
    boolean isBelowFulltextThreshold(double threshold) throws PresentationException, IndexUnreachableException {
        if (pageLoader.getNumPages() == 0) {
            return true;
        }
        if (pagesWithFulltext == null) {
            pagesWithFulltext = getPageCountWithFulltext();
        }
        double percentage = pagesWithFulltext * 100.0 / pageLoader.getNumPages();
        // logger.trace("{}% of pages have full-text", percentage); //NOSONAR Debug

        return percentage < threshold;
    }

    public long getPageCountWithFulltext() throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance()
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

        boolean access;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                    BeanUtils.getRequest()).isGranted();
        } catch (RecordNotFoundException e) {
            return false;
        }

        return access && (!isBelowFulltextThreshold(0.0001) || isAltoAvailableForWork());
    }

    /**
     * 
     * @return true if record full-text is generated from TEI documents; false otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isFulltextFromTEI() throws IndexUnreachableException, PresentationException {
        return isRecordHasTEIFiles();
    }

    /**
     *
     * @return true if any of this record's pages has an image and user has access rights; false otherwise
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws RecordNotFoundException
     */
    public boolean isRecordHasImages() throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (topStructElement == null || !topStructElement.isHasImages()) {
            return false;
        }

        return AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_IMAGES,
                BeanUtils.getRequest()).isGranted();
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

        boolean access;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                    BeanUtils.getRequest()).isGranted();
            return access && (!isBelowFulltextThreshold(0.0001) || isRecordHasTEIFiles());
        } catch (RecordNotFoundException e) {
            return false;
        }
    }

    /**
     * 
     * @return true if there are any TEI files associated directly with the top document
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isRecordHasTEIFiles() throws IndexUnreachableException, PresentationException {
        if (workHasTEIFiles == null) {
            SolrDocument doc = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc(new StringBuilder("+").append(SolrConstants.PI)
                            .append(':')
                            .append(pi)
                            .append(" +")
                            .append(SolrConstants.DOCTYPE)
                            .append(":")
                            .append(SolrConstants.DOCSTRCT)
                            .toString(), Arrays.asList(SolrConstants.FILENAME_TEI, SolrConstants.FILENAME_TEI + SolrConstants.MIDFIX_LANG + "*"));
            if (doc != null) {
                workHasTEIFiles = !doc.getFieldNames().isEmpty();
            } else {
                workHasTEIFiles = false;
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
        boolean access;
        try {
            access = AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(getPi(), null, IPrivilegeHolder.PRIV_VIEW_FULLTEXT,
                    BeanUtils.getRequest()).isGranted();
        } catch (RecordNotFoundException e) {
            return false;
        }
        if (!access) {
            return false;
        }
        if (pagesWithAlto == null) {

            pagesWithAlto = getPageCountWithAlto();
            logger.trace("{} of pages have full-text", pagesWithAlto);
        }
        int threshold = 1; // TODO ???

        return pagesWithAlto >= threshold;
    }

    /**
     * 
     * @param localFilesOnly
     * @return Map with mime type and file names for each
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public Map<String, List<String>> getFilenamesByMimeType(boolean localFilesOnly) throws IndexUnreachableException, PresentationException {
        List<SolrDocument> pageDocs = DataManager.getInstance()
                .getSearchIndex()
                .getDocs(new StringBuilder("+").append(SolrConstants.PI_TOPSTRUCT)
                        .append(':')
                        .append(pi)
                        .append(" +")
                        .append(SolrConstants.DOCTYPE)
                        .append(":PAGE")
                        .append(" +")
                        .append(SolrConstants.FILENAME)
                        .append(":*")
                        .toString(), List.of(SolrConstants.FILENAME));
        return Optional.ofNullable(pageDocs)
                .orElse(Collections.emptyList())
                .stream()
                .map(doc -> doc.getFieldValue(SolrConstants.FILENAME))
                .map(Object::toString)
                .filter(path -> !localFilesOnly || !path.matches("(?i)^https?:.*"))
                .collect(Collectors.toMap(
                        this::getMimeTypeViaFileName,
                        List::of,
                        (set1, set2) -> new ArrayList<>(CollectionUtils.union(set1, set2))));
    }

    /**
     * 
     * @param filename
     * @return {@link String}
     */
    public String getMimeTypeViaFileName(String filename) {
        try {
            return MimeType.getMimeTypeFromExtension(filename);
        } catch (UnknownMimeTypeException e) {
            return "unknown";
        }
    }

    public Long getPageCountWithAlto() throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance()
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
    }

    /**
     * Default fulltext getter (with HTML escaping).
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @deprecated Use <code>PhysicalElement.getFullText()</code>
     */
    @Deprecated(since = "24.10")
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
     * @deprecated Use <code>PhysicalElement.getFullText()</code>
     */
    @Deprecated(since = "24.10")
    public String getFulltext(boolean escapeHtml, String language) throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        String currentFulltext = null;

        // Current page fulltext

        if (isDoublePageMode()) {
            // Double page view
            StringBuilder sb = new StringBuilder();
            Optional<PhysicalElement> leftPage = getCurrentLeftPage();
            if (leftPage.isPresent() && StringUtils.isNotEmpty(leftPage.get().getFullText())) {
                sb.append(leftPage.get().getFullText());
            }
            Optional<PhysicalElement> rightPage = getCurrentRightPage();
            if (rightPage.isPresent() && StringUtils.isNotEmpty(rightPage.get().getFullText())) {
                if (sb.length() > 0) {
                    sb.append("<hr />");
                }
                sb.append(rightPage.get().getFullText());
            }
            currentFulltext = sb.toString();
        } else {
            // Single page view
            PhysicalElement currentPage = getCurrentPage();
            if (currentPage == null || StringUtils.isEmpty(currentPage.getFullText())) {
                return currentFulltext;
            }
            currentFulltext = currentPage.getFullText();
        }

        if (escapeHtml) {
            currentFulltext = StringTools.escapeHtmlChars(currentFulltext);
        }

        // logger.trace(currentFulltext); //NOSONAR Debug
        return currentFulltext;
    }

    /**
     *
     *
     * @return the probable mimeType of the fulltext of the current page. Loads the fulltext of that page if necessary
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public String getFulltextMimeType() throws ViewerConfigurationException {
        return getFulltextMimeType(null);
    }

    /**
     * 
     * @param language
     * @return TEI mime type if TEI files are indexed; mime type from the loaded full-text of the current page otherwise
     * @throws ViewerConfigurationException
     */
    public String getFulltextMimeType(String language) throws ViewerConfigurationException {
        logger.trace("getFulltextMimeType: {}", language);
        if (topStructElement != null && topStructElement.isHasTeiForLanguage(language)) {
            return StringConstants.MIMETYPE_TEI;
        }
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
     * List all files in {@link Configuration#getOrigContentFolder()} for which accecss is granted and which are not hidden per config
     *
     * @return the list of downloadable filenames. If no downloadable resources exists, an empty list is returned
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws IOException
     */
    private List<String> listDownloadableContent() throws PresentationException, IndexUnreachableException, DAOException, IOException {
        List<String> downloadFilenames = Collections.emptyList();
        VariableReplacer vr = new VariableReplacer(this);
        Path sourceFileDir = DataFileTools.getDataFolder(pi, DataManager.getInstance().getConfiguration().getOrigContentFolder());
        if (Files.exists(sourceFileDir) && AccessConditionUtils.checkContentFileAccessPermission(pi,
                (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).isGranted()) {
            List<IFilterConfiguration> displayFilters = DataManager.getInstance().getConfiguration().getAdditionalFilesDisplayFilters();
            try (Stream<Path> files = Files.list(sourceFileDir)) {
                Stream<String> filenames = files.map(path -> path.getFileName().toString());
                if (!displayFilters.isEmpty()) {
                    filenames = filenames.filter(filename -> displayFilters.stream().allMatch(filter -> filter.passes(filename, vr)));
                }
                downloadFilenames = filenames.toList();
            }
        }

        return downloadFilenames;
    }

    /**
     * Returns true if original content download has been enabled in the configuration and there are files in the original content folder for this
     * record.
     *
     * @return a boolean.
     */
    public boolean isDisplayContentDownloadMenu() {
        try {
            return !listDownloadableContent().isEmpty();
        } catch (PresentationException | IndexUnreachableException | DAOException | IOException e) {
            logger.warn("Error listing downloadable content: {}", e.getMessage());
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
     * @throws IOException
     */
    public List<LabeledLink> getContentDownloadLinksForWork() throws IOException, PresentationException, IndexUnreachableException, DAOException {
        AlphanumCollatorComparator comparator = new AlphanumCollatorComparator(null);
        return listDownloadableContent().stream()
                .sorted(comparator)
                .map(this::getLinkToDownloadFile)
                .filter(link -> link != LabeledLink.EMPTY)
                .toList();

    }

    protected LabeledLink getLinkToDownloadFile(String filename) {
        try {
            String localPi = getPi();
            String filenameEncoded = PathConverter.toURI(filename).toString();

            return DataManager.getInstance()
                    .getRestApiManager()
                    .getContentApiManager()
                    .map(urls -> urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_SOURCE).params(localPi, filenameEncoded).build())
                    .map(url -> new LabeledLink(filename, url, 0))
                    .orElse(LabeledLink.EMPTY);
        } catch (IndexUnreachableException | URISyntaxException e) {
            logger.error("Failed to create download link to {}", filename, e);
            return LabeledLink.EMPTY;
        }
    }

    /**
     * <p>
     * Getter for the field <code>topStructElementIddoc</code>.
     * </p>
     *
     * @return the topStructElementIddoc
     */
    public String getTopStructElementIddoc() {
        return topStructElementIddoc;
    }

    public String getAnchorDocumentIddoc() {
        if (this.anchorStructElement != null) {
            return anchorStructElement.getLuceneId();
        }

        return null;
    }

    /**
     * Returns <code>topDocument</code>. If the IDDOC of <code>topDocument</code> is different from <code>topDocumentIddoc</code>,
     * <code>topDocument</code> is reloaded.
     *
     * @return the currentDocument
     * @throws IndexUnreachableException
     */
    private StructElement loadTopStructElement() throws IndexUnreachableException {
        if (topStructElement == null || !Objects.equals(topStructElement.getLuceneId(), topStructElementIddoc)) {
            topStructElement = new StructElement(topStructElementIddoc, null);
        }
        return topStructElement;
    }

    /**
     * <p>
     * Getter for the field <code>topStructElement</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.StructElement} object.
     */
    public StructElement getTopStructElement() {
        try {
            return loadTopStructElement();
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * <p>
     * setTopStructElement.
     * </p>
     *
     * @param topStructElement the topStructElement to set
     */
    public void setTopStructElement(StructElement topStructElement) {
        this.topStructElement = topStructElement;
    }

    /**
     * <p>
     * Getter for the field <code>currentStructElementIddoc</code>.
     * </p>
     *
     * @return the currentStructElementIddoc
     */
    public String getCurrentStructElementIddoc() {
        return currentStructElementIddoc;
    }

    /**
     * <p>
     * Setter for the field <code>currentStructElementIddoc</code>.
     * </p>
     *
     * @param currentStructElementIddoc the currentStructElementIddoc to set
     */
    public void setCurrentStructElementtIddoc(String currentStructElementIddoc) {
        this.currentStructElementIddoc = currentStructElementIddoc;
    }

    /**
     * <p>
     * Getter for the field <code>currentStructElement</code>.
     * </p>
     *
     * @return the currentStructElement
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getCurrentStructElement() throws IndexUnreachableException {
        if (currentStructElement == null || !Objects.equals(currentStructElement.getLuceneId(), currentStructElementIddoc)) {
            logger.trace("Creating new currentDocument from IDDOC {}, old currentDocumentIddoc: {}", currentStructElementIddoc,
                    currentStructElementIddoc);
            currentStructElement = new StructElement(currentStructElementIddoc);
        }
        return currentStructElement;
    }

    /**
     * <p>
     * Setter for the field <code>currentStructElement</code>.
     * </p>
     *
     * @param currentStructElement the currentStructElement to set
     */
    public void setCurrentStructElement(StructElement currentStructElement) {
        this.currentStructElement = currentStructElement;
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
            docHierarchy = new LinkedList<>();

            StructElement curDoc = getCurrentStructElement();
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
     * Generates DC meta tags for the head of a HTML page.
     *
     * @return String with tags
     */
    public String getDublinCoreMetaTags() {
        return MetadataTools.generateDublinCoreMetaTags(this.topStructElement);
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
            return MetadataTools.generateHighwirePressMetaTags(this.topStructElement, isFilesOnly() ? getAllPages() : null);
        } catch (IndexUnreachableException | ViewerConfigurationException | DAOException | PresentationException e) {
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
        logger.trace("getVersionHistory");
        if (versionHistory == null) {
            versionHistory = new ArrayList<>();

            String versionLabelField = DataManager.getInstance().getConfiguration().getVersionLabelField();

            String nextVersionIdentifierField = DataManager.getInstance().getConfiguration().getNextVersionIdentifierField();
            if (StringUtils.isNotEmpty(nextVersionIdentifierField)) {
                List<String> next = new ArrayList<>();
                String identifier = topStructElement.getMetadataValue(nextVersionIdentifierField);
                while (identifier != null) {
                    SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + identifier, null);
                    if (doc != null) {
                        JSONObject jsonObj = new JSONObject();
                        String versionLabel =
                                versionLabelField != null ? SolrTools.getSingleFieldStringValue(doc, versionLabelField) : null;
                        if (StringUtils.isNotEmpty(versionLabel)) {
                            jsonObj.put("label", versionLabel);
                        }
                        jsonObj.put("id", identifier);
                        if (doc.getFieldValues(SolrConstants.MD_YEARPUBLISH) != null) {
                            jsonObj.put("year", doc.getFieldValues(SolrConstants.MD_YEARPUBLISH).iterator().next());
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

            // This version
            JSONObject jsonObj = new JSONObject();
            String versionLabel = versionLabelField != null ? topStructElement.getMetadataValue(versionLabelField) : null;
            if (versionLabel != null) {
                jsonObj.put("label", versionLabel);
            }
            jsonObj.put("id", getPi());
            jsonObj.put("year", topStructElement.getMetadataValue(SolrConstants.MD_YEARPUBLISH));
            jsonObj.put("order", "0"); // "0" identifies the currently loaded version
            versionHistory.add(jsonObj.toString());

            String prevVersionIdentifierField = DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField();
            if (StringUtils.isNotEmpty(prevVersionIdentifierField)) {
                List<String> previous = new ArrayList<>();
                String identifier = topStructElement.getMetadataValue(prevVersionIdentifierField);
                while (identifier != null) {
                    SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + identifier, null);
                    if (doc != null) {
                        jsonObj = new JSONObject();
                        versionLabel =
                                versionLabelField != null ? SolrTools.getSingleFieldStringValue(doc, versionLabelField) : null;
                        if (StringUtils.isNotEmpty(versionLabel)) {
                            jsonObj.put("label", versionLabel);
                        }
                        jsonObj.put("id", identifier);
                        if (doc.getFieldValues(SolrConstants.MD_YEARPUBLISH) != null) {
                            jsonObj.put("year", doc.getFieldValues(SolrConstants.MD_YEARPUBLISH).iterator().next());
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

        //		logger.trace("Version history size: {}", versionHistory.size()); //NOSONAR Debug
        return versionHistory;
    }

    /**
     * Returns the ContextObject value for a COinS element (generated using metadata from <code>currentDocument</code>).
     *
     * @return a {@link java.lang.String} object.
     */
    public String getContextObject() {
        if (currentStructElement != null && contextObject == null) {
            try {
                contextObject =
                        currentStructElement.generateContextObject(BeanUtils.getNavigationHelper().getCurrentUrl(),
                                currentStructElement.getTopStruct());
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
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
            Messages.error(StringConstants.MSG_TRANSKRIBUS_RECORDIGESTERROR);
            return "";
        }

        TranskribusSession session = ub.getUser().getTranskribusSession();
        if (session == null && login) {
            ub.transkribusLoginAction();
            session = ub.getUser().getTranskribusSession();
        }
        if (session == null) {
            Messages.error(StringConstants.MSG_TRANSKRIBUS_RECORDIGESTERROR);
            return "";
        }
        try {
            NavigationHelper nh = BeanUtils.getNavigationHelper();
            String resolverUrlRoot = nh != null ? nh.getApplicationUrl() : "http://viewer.goobi.io/" + "metsresolver?id=";
            TranskribusJob job = TranskribusUtils.ingestRecord(DataManager.getInstance().getConfiguration().getTranskribusRestApiUrl(), session, pi,
                    resolverUrlRoot);
            if (job == null) {
                Messages.error(StringConstants.MSG_TRANSKRIBUS_RECORDIGESTERROR);
                return "";
            }
            Messages.info("transkribus_recordIngestSuccess");
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
            Messages.error(StringConstants.MSG_TRANSKRIBUS_RECORDIGESTERROR);
        } catch (DAOException e) {
            logger.debug("DAOException thrown here");
            logger.error(e.getMessage(), e);
            Messages.error(StringConstants.MSG_TRANSKRIBUS_RECORDIGESTERROR);
        } catch (HTTPException e) {
            if (e.getCode() == 401) {
                ub.getUser().setTranskribusSession(null);
                Messages.error("transkribus_sessionExpired");
            } else {
                logger.error(e.getMessage(), e);
                Messages.error(StringConstants.MSG_TRANSKRIBUS_RECORDIGESTERROR);
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
            pi = getCurrentStructElement().getMetadataValue(SolrConstants.PI_TOPSTRUCT);
        }

        return pi;
    }

    /**
     * If the current record is a volume, returns the PI of the anchor record.
     *
     * @return anchor PI if record is volume; null otherwise.
     */
    public String getAnchorPi() {
        if (anchorStructElement != null) {
            return anchorStructElement.getMetadataValue(SolrConstants.PI);
        }

        return null;
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
        setPageNavigation(doublePageMode ? PageNavigation.DOUBLE : PageNavigation.SINGLE);
        this.setDoublePageModeForDropDown(doublePageMode);
    }

    /**
     * <p>
     * isDoublePageMode.
     * </p>
     *
     * @return the doublePageMode
     */
    public boolean isDoublePageMode() {
        return PageNavigation.DOUBLE.equals(getPageNavigation());
    }

    public boolean isSequenceMode() {
        return PageNavigation.SEQUENCE.equals(getPageNavigation());
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
        if (StringUtils.isNotBlank(firstPdfPage) && firstPdfPage.matches(StringConstants.POSITIVE_INTEGER)) {
            this.firstPdfPage = Integer.valueOf(firstPdfPage);
        }
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
        if (lastPdfPage != null && lastPdfPage.matches(StringConstants.POSITIVE_INTEGER)) {
            this.lastPdfPage = Integer.valueOf(lastPdfPage);
        }
    }

    public void generatePageRangePdf() {
        logger.debug("Generating pdf of {} from pages {} to {}", this.pi, this.firstPdfPage, this.lastPdfPage);
        String filename = String.format("%s_%s_%s.pdf", this.pi, this.firstPdfPage, this.lastPdfPage);
        try (PipedInputStream in = new PipedInputStream(); OutputStream out = new PipedOutputStream(in);
                ExecutorService executor = Executors.newFixedThreadPool(1)) {

            Map<String, String> params = new HashMap<>();
            params.put("imageSource", DataFileTools.getMediaFolder(this.pi).toAbsolutePath().toString());
            params.put("pdfSource", DataFileTools.getPdfFolder(this.pi).toAbsolutePath().toString());
            params.put("altoSource", DataFileTools.getAltoFolder(this.pi).toAbsolutePath().toString());
            Optional.ofNullable(this.firstPdfPage)
                    .flatMap(this::getPage)
                    .map(PhysicalElement::getFileName)
                    .ifPresent(first -> params.put("first", first));
            Optional.ofNullable(this.lastPdfPage)
                    .flatMap(this::getPage)
                    .map(PhysicalElement::getFileName)
                    .ifPresent(last -> params.put("last", last));

            SinglePdfRequest request = new SinglePdfRequest(params);
            executor.submit(() -> {
                try {
                    new GetPdfAction().writePdf(request, ContentServerConfiguration.getInstance(), out);
                } catch (URISyntaxException | ContentLibException | IOException e) {
                    logger.error("Error creating page range pdf", e);
                }
            });
            Faces.sendFile(in, filename, true);
        } catch (PresentationException | IOException | URISyntaxException | IndexUnreachableException e) {
            logger.error("Error creating page range pdf", e);
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
        if (getCurrentPage() != null && getCurrentPage().isFlipRectoVerso()) {
            logger.trace("page {} is flipped", getCurrentPage().getOrder());
            return firstPageOrientation.opposite();
        }
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
        return getDocumentTitle(this.topStructElement);
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
            case "FormationHistory":
                sb.append("\"").append(document.getMetadataValue(SolrConstants.TITLE)).append("\"");
                //TODO: Add Einsatzland z.b.: (Deutschland)
                if (StringUtils.isNotBlank(document.getMetadataValue("MD_AUTHOR"))) {
                    sb.append(" von ").append(document.getMetadataValue("MD_AUTHOR"));
                }
                if (StringUtils.isNotBlank(document.getMetadataValue(SolrConstants.MD_YEARPUBLISH))) {
                    sb.append(" (").append(document.getMetadataValue(SolrConstants.MD_YEARPUBLISH)).append(")");
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
     * getCiteLinkWork.
     * </p>
     *
     * @return A persistent link to the current work
     *
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @should return correct url
     */
    public String getCiteLinkWork() throws IndexUnreachableException, DAOException, PresentationException {
        if (topStructElement == null) {
            return "";
        }

        // Prefer custom PURL in MD_PURL, if available
        String customPURL = topStructElement.getMetadataValue("MD_PURL");
        if (StringUtils.isNotEmpty(customPURL)) {
            return customPURL;
        }

        // Build URL
        StringBuilder url = new StringBuilder();
        boolean anchorOrGroup = topStructElement.isAnchor() || topStructElement.isGroup();
        PageType pageType = PageType.determinePageType(topStructElement.getDocStructType(), null, anchorOrGroup, isHasPages(), false);
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

    /**
     * <p>
     * isDisplayCiteLinkWork.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayCiteLinkWork() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetCitationCitationLinks() && topStructElement != null;
    }

    /**
     *
     * @return Citation URL for the current structure element
     * @throws IndexUnreachableException
     * @should return correct url
     */
    public String getCiteLinkDocstruct() throws IndexUnreachableException {
        if (currentStructElement == null) {
            return "";
        }

        // Prefer custom PURL in MD_PURL, if available
        String customPURL = currentStructElement.getMetadataValue("MD_PURL");
        if (StringUtils.isNotEmpty(customPURL)) {
            return customPURL;
        }

        // Build URL
        StringBuilder url = new StringBuilder();
        boolean anchorOrGroup = currentStructElement.isAnchor() || currentStructElement.isGroup();
        PageType pageType = PageType.determinePageType(currentStructElement.getDocStructType(), null, anchorOrGroup, isHasPages(), false);
        if (pageType == null) {
            if (isHasPages()) {
                pageType = PageType.viewObject;
            } else {
                pageType = PageType.viewMetadata;
            }
        }
        url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        url.append('/').append(pageType.getName()).append('/').append(getPi()).append('/');
        if (currentStructElement.getImageNumber() > 0) {
            // First page of the docstruct
            url.append(currentStructElement.getImageNumber()).append('/');
        } else {
            // Current page
            url.append(getCurrentImageOrder()).append('/');
        }
        if (StringUtils.isNotEmpty(currentStructElement.getLogid())) {
            url.append(currentStructElement.getLogid()).append('/');
        }

        return url.toString();
    }

    /**
     * <p>
     * isDisplayCiteLinkDocstruct.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayCiteLinkDocstruct() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetCitationCitationLinks() && currentStructElement != null
                && !Objects.equals(currentStructElement.getLuceneId(), topStructElement.getLuceneId());
    }

    /**
     * <p>
     * getCiteLinkPage.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return correct url
     */
    public String getCiteLinkPage() throws IndexUnreachableException, DAOException {
        PhysicalElement currentPage = getCurrentPage();
        if (currentPage == null) {
            return "";
        }

        // Build URL
        StringBuilder url = new StringBuilder();
        PageType pageType = null;
        if (topStructElement != null) {
            boolean anchorOrGroup = topStructElement.isAnchor() || topStructElement.isGroup();
            pageType = PageType.determinePageType(topStructElement.getDocStructType(), null, anchorOrGroup, isHasPages(), false);
        }
        if (pageType == null) {
            pageType = PageType.viewObject;
        }
        url.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        url.append('/').append(pageType.getName()).append('/').append(getPi()).append('/').append(currentPage.getOrder()).append('/');

        return url.toString();
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
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetCitationCitationLinks() && getCurrentPage() != null;
    }

    /**
     *
     * @return HTML-formatted citation text
     * @throws IOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getCitationStringHtml() throws IOException, IndexUnreachableException, PresentationException {
        return getCitationString("html");
    }

    /**
     *
     * @return Plain citation text
     * @throws IOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getCitationStringPlain() throws IOException, IndexUnreachableException, PresentationException {
        return getCitationString("text");
    }

    /**
     *
     * @param outputFormat Output format (html or text)
     * @return Generated citation string for the selected style
     * @throws IOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return apa html citation correctly
     * @should return apa html plaintext correctly
     */
    String getCitationString(String outputFormat) throws IOException, IndexUnreachableException, PresentationException {
        if (StringUtils.isEmpty(citationStyle)) {
            List<String> availableStyles = DataManager.getInstance().getConfiguration().getSidebarWidgetCitationCitationRecommendationStyles();
            if (availableStyles.isEmpty()) {
                return "";
            }
            citationStyle = availableStyles.get(0);
        }

        if (citationProcessorWrapper == null) {
            citationProcessorWrapper = new CitationProcessorWrapper();
        }
        CSL processor = citationProcessorWrapper.getCitationProcessor(citationStyle);
        Metadata md = DataManager.getInstance().getConfiguration().getSidebarWidgetCitationCitationRecommendationSource();
        md.populate(topStructElement, String.valueOf(topStructElement.getLuceneId()), null, BeanUtils.getLocale());
        for (MetadataValue val : md.getValues()) {
            if (!val.getCitationValues().isEmpty()) {
                Citation citation = new Citation(pi, processor, citationProcessorWrapper.getCitationItemDataProvider(),
                        CitationTools.getCSLTypeForDocstrct(topStructElement.getDocStructType(), topStructElement.getDocStructType()),
                        val.getCitationValues());
                try {
                    return citation.getCitationString(outputFormat);
                } catch (DateTimeException e) {
                    logger.error("Citeproc encountered exception parsing date: {}", e.toString());
                    if ("html".equalsIgnoreCase(outputFormat)) {
                        return "<span style=\"color: red;\">Citation engine encountered exception parsing date: <span style=\"font-weight: bold;\">"
                                + e.getLocalizedMessage() + "</span></span>";
                    }
                    return "Citation engine encountered exception parsing date:" + e.getLocalizedMessage();
                }
            }
        }

        return "";
    }

    /**
     * @return the citationStyle
     */
    public String getCitationStyle() {
        return citationStyle;
    }

    /**
     * @param citationStyle the citationStyle to set
     */
    public void setCitationStyle(String citationStyle) {
        this.citationStyle = citationStyle;
    }

    /**
     * @return the citationProcessorWrapper
     */
    public CitationProcessorWrapper getCitationProcessorWrapper() {
        return citationProcessorWrapper;
    }

    /**
     * @param citationProcessorWrapper the citationProcessorWrapper to set
     */
    public void setCitationProcessorWrapper(CitationProcessorWrapper citationProcessorWrapper) {
        this.citationProcessorWrapper = citationProcessorWrapper;
    }

    /**
     * @param levelName
     * @return List of configured citation links for the given levelName, populated with values
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public List<CitationLink> getSidebarWidgetUsageCitationLinksForLevel(String levelName) throws PresentationException, IndexUnreachableException {
        // logger.trace("getSidebarWidgetUsageCitationLinksForLevel: {}", levelName); //NOSONAR Debug
        CitationLinkLevel level = CitationLinkLevel.getByName(levelName);
        if (level == null) {
            logger.warn("Unknown citation link level: {}", levelName);
            return Collections.emptyList();
        }

        // Populate values
        if (this.citationLinks.get(level) == null || !this.citationLinks.get(level).isCurrent(this)) {
            this.citationLinks.put(level, new CitationList(CitationTools
                    .generateCitationLinksForLevel(DataManager.getInstance().getConfiguration().getSidebarWidgetCitationCitationLinks(), level, this),
                    level,
                    this));
        }

        return this.citationLinks.get(level).getList();
    }

    /**
     *
     * @return Value of EAD_NODE_ID in the loaded record
     */
    public String getArchiveEntryIdentifier() {
        if (topStructElement == null) {
            return null;
        }

        // logger.trace("getArchiveEntryIdentifier: {}", topStructElement.getMetadataValue(SolrConstants.EAD_NODE_ID)); //NOSONAR Debug
        return topStructElement.getMetadataValue(SolrConstants.EAD_NODE_ID);
    }

    /**
     * Creates an instance of ViewManager loaded with the record with the given identifier.
     *
     * @param pi Record identifier
     * @param loadPages
     * @return Created {@link ViewManager}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws RecordNotFoundException
     */
    public static ViewManager createViewManager(String pi, boolean loadPages)
            throws PresentationException, IndexUnreachableException, DAOException, RecordNotFoundException {
        if (pi == null) {
            throw new IllegalArgumentException("pi may not be null");
        }

        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(SolrConstants.PI + ":" + pi, null);
        if (doc == null) {
            throw new RecordNotFoundException(pi);
        }

        String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC);
        StructElement topDocument = new StructElement(iddoc, doc);
        return new ViewManager(topDocument, AbstractPageLoader.create(topDocument, loadPages), iddoc, null, null, null);
    }

    /**
     * Returns an integer list such that
     * <ul>
     * <li>the 'pageOrder' is contained in the list</li>
     * <li>the list contains (2*range)+1 consecutive numbers, or all page numbers of the current record if it is less than that</li>
     * <li>the first number is not less than the first image order</li>
     * <li>the last number is not larger than the last image order</li>
     * <li>the 'pageOrder' is as far in the middle of the list as possible without violating any of the other points</li></li> Used int
     * thumbnailPaginator.xhtml to calculate the pages to display.
     * 
     * @param pageOrder The current page number around which to center the numbers
     * @param range The number of numbers to include above and below the current page number, if possible
     * @param fillToSize if true, always return a list of exactly 2*range+1 elements, no matter the total number of pages in the current record
     * @return an integer list
     * @throws IndexUnreachableException If the page numbers could not be read from SOLR
     * @throws IllegalArgumentException If the pageOrder is not within the range of page numbers of the current record or if range is less than zero
     */
    public List<Integer> getPageRangeAroundPage(int pageOrder, int range, boolean fillToSize) throws IndexUnreachableException {

        if (pageOrder < pageLoader.getFirstPageOrder() || pageOrder > pageLoader.getLastPageOrder()) {
            throw new IllegalArgumentException(
                    "the given pageOrder must be within the range of page numbers of the current record. The given pageOrder is " + pageOrder);
        } else if (range < 0) {
            throw new IllegalArgumentException("the given range must not be less than zero. It is " + range);
        }

        int firstPage = pageOrder;
        int lastPage = pageOrder;
        int numPages = 2 * range + 1;
        while (lastPage - firstPage + 1 < numPages) {
            boolean changed = false;
            if (firstPage > pageLoader.getFirstPageOrder()) {
                firstPage--;
                changed = true;
            }
            if (lastPage < pageLoader.getLastPageOrder()) {
                lastPage++;
                changed = true;
            }
            if (!changed) {
                break;
            }
        }
        if (fillToSize) {
            while (lastPage - firstPage + 1 < numPages) {
                lastPage++;
            }
        }
        return IntStream.range(firstPage, lastPage + 1).boxed().toList();
    }

    /**
     * 
     * @return The most restrictive status name of the configured statuses
     * @should return locked status if locked most restrictive status found
     * @should return partial status if partial most restrictive status found
     * @should return open status if no restrictive statuses found
     */
    public String getCopyrightIndicatorStatusName() {
        Status ret = Status.OPEN;
        for (CopyrightIndicatorStatus status : getCopyrightIndicatorStatuses()) {
            switch (status.getStatus()) {
                case LOCKED:
                    return Status.LOCKED.name();
                case PARTIAL:
                    ret = Status.PARTIAL;
                    break;
                default:
                    break;
            }
        }

        return ret.name();
    }

    /**
     * 
     * @return copyrightIndicatorStatuses
     * @should return correct statuses
     * @should return locked status if no statuses found
     */
    public List<CopyrightIndicatorStatus> getCopyrightIndicatorStatuses() {
        // logger.trace("getCopyrightIndicatorStatuses");
        if (copyrightIndicatorStatuses == null) {
            copyrightIndicatorStatuses = new ArrayList<>();
            String field = DataManager.getInstance().getConfiguration().getCopyrightIndicatorStatusField();
            StringBuilder sbUnconfiguredAccessConditions = new StringBuilder();
            if (StringUtils.isNotEmpty(field)) {
                List<String> values = topStructElement.getMetadataValues(field);
                if (!values.isEmpty()) {
                    for (String value : values) {
                        CopyrightIndicatorStatus status = DataManager.getInstance().getConfiguration().getCopyrightIndicatorStatusForValue(value);
                        if (status != null) {
                            copyrightIndicatorStatuses.add(status);
                        } else {
                            if (sbUnconfiguredAccessConditions.length() > 0) {
                                sbUnconfiguredAccessConditions.append(", ");
                            }
                            sbUnconfiguredAccessConditions.append(value);
                        }
                    }
                }
            }
            // Default
            if (copyrightIndicatorStatuses.isEmpty()) {
                // If no statuses are configured for existing values, set to locked and add all values to the description
                copyrightIndicatorStatuses.add(new CopyrightIndicatorStatus(Status.LOCKED, sbUnconfiguredAccessConditions.toString()));
            }
        }

        return copyrightIndicatorStatuses;
    }

    /**
     * @return the copyrightIndicatorLicense
     * @should return correct license
     * @should return default license if no licenses found
     */
    public CopyrightIndicatorLicense getCopyrightIndicatorLicense() {
        if (copyrightIndicatorLicense == null) {
            String field = DataManager.getInstance().getConfiguration().getCopyrightIndicatorLicenseField();
            if (StringUtils.isNotEmpty(field)) {
                String value = topStructElement.getMetadataValue(field);
                if (StringUtils.isNotEmpty(value)) {
                    copyrightIndicatorLicense = DataManager.getInstance().getConfiguration().getCopyrightIndicatorLicenseForValue(value);
                }
            }
            // Default
            if (copyrightIndicatorLicense == null) {
                copyrightIndicatorLicense = new CopyrightIndicatorLicense("", Collections.emptyList());
            }
        }

        return copyrightIndicatorLicense;
    }

    public boolean hasPrerenderedPagePdfs() {
        try {
            Path pdfFolder = new ProcessDataResolver().getDataFolders(pi, "pdf").get("pdf");
            return pdfFolder != null && Files.exists(pdfFolder) && !FileTools.isFolderEmpty(pdfFolder);
        } catch (IndexUnreachableException | PresentationException | IOException e) {
            logger.error("Error checking pdf resource folder for pi {}: {}", pi, e.toString());
            return false;
        }

    }

    public List<PhysicalElement> getPagesForMediaType(String type) throws PresentationException, IndexUnreachableException {
        List<ComplexMetadata> mds = getTopStructElement().getMetadataDocuments().getMetadata("MD_MEDIA_INFO");
        return mds.stream()
                .filter(md -> type.equalsIgnoreCase(md.getFirstValue("MD_SUBJECT", null)))
                .map(md -> md.getFirstValue("MD_MEDIA_INFO", null))
                .map(path -> Paths.get(path).getFileName().toString())
                .map(filename -> this.getPageLoader().findPageForFilename(filename))
                .filter(p -> p != null)
                .toList();
    }

    public List<String> getExternalResourceUrls() throws IndexUnreachableException {
        if (this.externalResourceUrls == null) {
            this.externalResourceUrls = loadExternalResourceUrls();
        }
        return this.externalResourceUrls;
    }

    private List<String> loadExternalResourceUrls() throws IndexUnreachableException {
        List<String> urlTemplates = DataManager.getInstance().getConfiguration().getExternalResourceUrlTemplates();
        VariableReplacer vr = new VariableReplacer(this);
        return urlTemplates.stream()
                .flatMap(templ -> vr.replace(templ).stream())
                .filter(ExternalFilesDownloader::resourceExists)
                .toList();
    }

    public StructElement getAnchorStructElement() {
        return anchorStructElement;
    }

    public String getCssClass() throws IndexUnreachableException {
        VariableReplacer vr =
                new VariableReplacer(this);
        String template = DataManager.getInstance().getConfiguration().getRecordViewStyleClass();
        String value = vr.replace(template).stream().findAny().orElse("");
        value = StringTools.convertToSingleWord(value, MAX_STYLECLASS_LENGTH, STYLE_CLASS_WORD_SEPARATOR).toLowerCase();

        return value;
    }

    public boolean isDoublePageNavigationEnabled(PageType pageType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .isDoublePageNavigationEnabled(pageType, Optional.ofNullable(getCurrentPage()).map(PhysicalElement::getMimeType).orElse(null));
    }

    public boolean showImageThumbnailGallery(PageType pageType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .showImageThumbnailGallery(pageType, Optional.ofNullable(getCurrentPage()).map(PhysicalElement::getMimeType).orElse(null));

    }
}
