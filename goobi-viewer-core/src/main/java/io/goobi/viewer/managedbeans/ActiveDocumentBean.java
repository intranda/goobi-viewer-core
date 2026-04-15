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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jboss.weld.contexts.ContextNotActiveException;
import org.json.JSONObject;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.util.Faces;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import de.intranda.api.annotation.wa.TypedResource;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.controller.ProcessDataResolver;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.model.ViewAttributes;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.exceptions.IllegalUrlParameterException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordDeletedException;
import io.goobi.viewer.exceptions.RecordLimitExceededException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.annotation.comments.CommentGroup;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent.ContentType;
import io.goobi.viewer.model.job.download.DownloadOption;
import io.goobi.viewer.model.job.download.EpubDownloadJob;
import io.goobi.viewer.model.job.download.PdfDownloadJob;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.maps.GeoMapFeature;
import io.goobi.viewer.model.maps.ManualFeatureSet;
import io.goobi.viewer.model.maps.RecordGeoMap;
import io.goobi.viewer.model.maps.coordinates.CoordinateReaderProvider;
import io.goobi.viewer.controller.FileSizeCalculator;
import io.goobi.viewer.model.pdf.PdfSizeCalculator;
import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.statistics.usage.RequestType;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.toc.TOCElement;
import io.goobi.viewer.model.toc.export.pdf.TocWriter;
import io.goobi.viewer.model.toc.export.pdf.WriteTocException;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.Dataset;
import io.goobi.viewer.model.viewer.PageNavigation;
import io.goobi.viewer.model.viewer.PageOrientation;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import mil.nga.sf.geojson.Geometry;

/**
 * JSF session-scoped backing bean that opens the requested record and provides all data relevant
 * to it. Owns the {@link io.goobi.viewer.model.viewer.ViewManager} for the current record and
 * coordinates access to its structure elements, physical pages, TOC, and download jobs.
 *
 * <p><b>Lifecycle:</b> Created once per HTTP session; a new {@code ViewManager} is instantiated
 * each time a different record PI is requested. The bean is destroyed when the session expires.
 *
 * <p><b>Thread safety:</b> Explicitly synchronised. Multiple {@code synchronized(this)} and
 * {@code synchronized(lock)} blocks guard concurrent access to shared record state, since JSF
 * AJAX requests and background threads (PDF/EPUB generation, TOC building) may run concurrently
 * within the same session.
 */
@Named
@SessionScoped
public class ActiveDocumentBean implements Serializable {

    private static final long serialVersionUID = -8686943862186336894L;

    private static final Logger logger = LogManager.getLogger(ActiveDocumentBean.class);

    /**
     * Regex pattern 'imageToShow' matches if doublePageMode should be active.
     */
    private static final String DOUBLE_PAGE_PATTERN = "\\d+-\\d+";

    private static int imageContainerWidth = 600;

    private final transient Object lock = new Object();

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private CmsBean cmsBean;
    @Inject
    private SearchBean searchBean;
    @Inject
    private BookmarkBean bookmarkBean;
    @Inject
    private ImageDeliveryBean imageDelivery;
    @Inject
    private BreadcrumbBean breadcrumbBean;

    /** URL parameter 'action'. */
    private volatile String action = "";
    /** URL parameter 'imageToShow'. */
    private String imageToShow = "1";
    /** URL parameter 'logid'. */
    private volatile String logid = "";
    /** URL parameter 'tocCurrentPage'. */
    private volatile int tocCurrentPage = 1;

    // volatile ensures the safely-published ViewManager reference is immediately visible to all threads
    private volatile ViewManager viewManager;
    // volatile so that reads outside synchronized(this) in getRelativeUrlTags() see the value written
    // inside the synchronized update() block without needing the full monitor
    private volatile boolean anchor = false;
    private boolean volume = false;
    private volatile boolean group = false;
    protected String topDocumentIddoc = null;

    // TODO move to SearchBean
    private BrowseElement prevHit;
    private BrowseElement nextHit;

    /** This persists the last value given to setPersistentIdentifier() and is used for handling a RecordNotFoundException. */
    // volatile so that setRepresentativeImage() sees the latest value without holding any lock during Solr I/O
    private volatile String lastReceivedIdentifier;
    /** Available languages for this record. */
    private List<String> recordLanguages;
    /** Currently selected language for multilingual records. */
    private Language selectedRecordLanguage;

    private Boolean deleteRecordKeepTrace;

    private String clearCacheMode;

    // volatile so that the reference swap in getRecordGeoMap() (geoMaps = singletonMap(...))
    // is immediately visible to all threads without requiring the ADB monitor
    private volatile Map<String, RecordGeoMap> geoMaps = new HashMap<>();

    private int reloads = 0;

    private boolean downloadImageModalVisible = false;

    private String selectedDownloadOptionLabel;
    /* Previous docstruct URL cache. TODO Implement differently once other views beside full-screen are used. */
    private Map<String, String> prevDocstructUrlCache = new HashMap<>();
    /* Next docstruct URL cache. TODO Implement differently once other views beside full-screen are used. */
    private Map<String, String> nextDocstructUrlCache = new HashMap<>();

    @Inject
    @Push
    private PushContext tocUpdateChannel;

    // volatile for double-checked locking in getRecordDataset()
    private volatile Dataset recordDataset;
    private volatile PdfSizeCalculator pdfSizes;
    // Cached full-record PDF size estimate derived from Solr MDNUM_FILESIZE fields
    private volatile String cachedFullPdfSize = null;

    /**
     * Empty constructor.
     */
    public ActiveDocumentBean() {
        // the emptiness inside
    }

    /**
     * Required setter for ManagedProperty injection.
     *
     * @param navigationHelper the NavigationHelper instance to inject for testing
     */
    public void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    /**
     * Required setter for ManagedProperty injection.
     *
     * @param cmsBean the CmsBean instance to inject for testing
     */
    public void setCmsBean(CmsBean cmsBean) {
        this.cmsBean = cmsBean;
    }

    /**
     * Required setter for ManagedProperty injection.
     *
     * @param searchBean the SearchBean instance to inject for testing
     */
    public void setSearchBean(SearchBean searchBean) {
        this.searchBean = searchBean;
    }

    /**
     * Required setter for ManagedProperty injection.
     *
     * @param bookshelfBean the BookmarkBean instance to inject for testing
     */
    public void setBookshelfBean(BookmarkBean bookshelfBean) {
        this.bookmarkBean = bookshelfBean;
    }

    /**
     * Required setter for ManagedProperty injection.
     *
     * @param breadcrumbBean the BreadcrumbBean instance to inject for testing
     */
    public void setBreadcrumbBean(BreadcrumbBean breadcrumbBean) {
        this.breadcrumbBean = breadcrumbBean;
    }

    /**
     * Resets the bean state when a record is unloaded: discards the current {@link ViewManager},
     * clears navigation state (logid, action, prev/next hit, docstruct URL caches), and notifies
     * all registered modules.
     *
     * <p><b>Warning:</b> Although this method is fully {@code synchronized(this)}, calling it
     * while {@code update()} is running on another thread may still cause NPEs, because
     * {@code update()} holds the lock only in discrete blocks and not for its entire duration.
     *
     * @throws IndexUnreachableException if a module's cleanup requires Solr and Solr is unavailable
     * @should reset lastReceivedIdentifier
     */
    public void reset() throws IndexUnreachableException {
        synchronized (this) {
            logger.trace("reset (thread {})", Thread.currentThread().threadId());
            String pi = viewManager != null ? viewManager.getPi() : null;
            viewManager = null;
            topDocumentIddoc = null;
            logid = "";
            action = "";
            prevHit = null;
            nextHit = null;
            group = false;
            clearCacheMode = null;
            prevDocstructUrlCache.clear();
            nextDocstructUrlCache.clear();
            lastReceivedIdentifier = null;
            // Reset per-record caches so they are recalculated for the next record
            recordDataset = null;
            pdfSizes = null;
            cachedFullPdfSize = null;

            // Any cleanup modules need to do when a record is unloaded
            for (IModule module : DataManager.getInstance().getModules()) {
                try {
                    module.augmentResetRecord();
                } catch (ContextNotActiveException | IllegalStateException e) {
                    logger.warn("Session context not active while resetting module; skipping that cleanup: {}", e.toString());
                }
            }

            // Remove record lock for this record and session
            if (BeanUtils.getSession() != null) {
                DataManager.getInstance()
                        .getRecordLockManager()
                        .removeLockForPiAndSessionId(pi, BeanUtils.getSession().getId());
            }
        }
    }

    /**
     * Do not call from ActiveDocumentBean.update()!
     *
     * @return the current ViewManager, triggering an update if not yet initialized
     */
    public ViewManager getViewManager() {
        if (viewManager == null) {
            try {
                try {
                    update();
                } catch (IDDOCNotFoundException e) {
                    reload(lastReceivedIdentifier);
                }
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
            } catch (RecordNotFoundException | RecordDeletedException | RecordLimitExceededException e) {
                if (e.getMessage() != null && !"null".equals(e.getMessage()) && !"???".equals(e.getMessage())) {
                    logger.warn("{}: {}", e.getClass().getName(), e.getMessage());
                }
            } catch (IndexUnreachableException | DAOException | ViewerConfigurationException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return viewManager;
    }

    /**
     * reload.
     *
     * @param pi persistent identifier of the record to reload
     * @return output of open()
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.RecordNotFoundException
     * @throws io.goobi.viewer.exceptions.RecordDeletedException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException
     * @throws io.goobi.viewer.exceptions.RecordLimitExceededException
     */
    public String reload(String pi) throws PresentationException, RecordNotFoundException, RecordDeletedException, IndexUnreachableException,
            DAOException, ViewerConfigurationException, RecordLimitExceededException {
        logger.trace("reload({})", pi);
        reloads++;
        reset();
        if (reloads > 3) {
            throw new RecordNotFoundException(pi);
        }
        setPersistentIdentifier(pi);
        //        setImageToShow(1);
        return open();
    }

    /**
     * Loads the record with the IDDOC set in <code>currentElementIddoc</code>.
     *
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.RecordNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.RecordDeletedException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IDDOCNotFoundException
     * @throws io.goobi.viewer.exceptions.RecordLimitExceededException
     * @throws java.lang.NumberFormatException
     * @should initialize ViewManager with matching PI and struct elements after update
     * @should create new ViewManager instance when LOGID changes between updates
     * @should not override topDocumentIddoc if LOGID has changed
     * @should throw RecordNotFoundException if listing not allowed by default
     * @should load records that have been released via moving wall
     * @should set toc on view manager after update
     */
    public void update() throws PresentationException, IndexUnreachableException, RecordNotFoundException, RecordDeletedException, DAOException,
            ViewerConfigurationException, IDDOCNotFoundException, NumberFormatException, RecordLimitExceededException {
        // Tracks the ViewManager that needs a TOC built after the synchronized block.
        // Building the TOC (which fires N Solr queries) outside the monitor prevents
        // the entire session thread pool from blocking on one slow document load.
        ViewManager tocTarget = null;

        synchronized (this) {
            if (topDocumentIddoc == null) {
                try {
                    if (StringUtils.isNotEmpty(lastReceivedIdentifier)) {
                        throw new RecordNotFoundException(lastReceivedIdentifier);
                    }
                    throw new RecordNotFoundException("???");
                } finally {
                    lastReceivedIdentifier = null;
                }
            }
            logger.debug("update(): (IDDOC {} ; page {} ; thread {})", topDocumentIddoc, imageToShow, Thread.currentThread().threadId());
            prevHit = null;
            nextHit = null;
            boolean doublePageMode = isDoublePageUrl();
            // Do these steps only if a new document has been loaded
            boolean mayChangeHitIndex = false;
            boolean vmNull = viewManager == null;
            boolean vmTopNull = !vmNull && viewManager.getTopStructElement() == null;
            boolean iddocMismatch = !vmNull && !vmTopNull && !viewManager.getTopStructElementIddoc().equals(topDocumentIddoc);
            if (vmNull || vmTopNull || iddocMismatch) {
                anchor = false;
                volume = false;
                group = false;

                // Change current hit index only if loading a new record
                if (searchBean != null && searchBean.getCurrentSearch() != null) {
                    searchBean.increaseCurrentHitIndex();
                    mayChangeHitIndex = true;
                }

                StructElement topStructElement = new StructElement(topDocumentIddoc);

                // Exit here if record is not found or has been deleted
                if (!topStructElement.isExists()) {
                    logger.info("IDDOC for the current record '{}' ({}) no longer seems to exist, attempting to retrieve an updated IDDOC...",
                            topStructElement.getPi(), topDocumentIddoc);
                    topDocumentIddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(topStructElement.getPi());
                    if (topDocumentIddoc == null) {
                        logger.warn("New IDDOC for the current record '{}' could not be found. Perhaps this record has been deleted?",
                                topStructElement.getPi());
                        reset();
                        try {
                            throw new RecordNotFoundException(lastReceivedIdentifier);
                        } finally {
                            lastReceivedIdentifier = null;
                        }
                    }
                } else if (topStructElement.isDeleted()) {
                    logger.debug("Record '{}' is deleted and only available as a trace document.", topStructElement.getPi());
                    reset();
                    throw new RecordDeletedException(topStructElement.getPi());
                }

                // Do not open records who may not be listed for the current user
                List<String> requiredAccessConditions = topStructElement.getMetadataValues(SolrConstants.ACCESSCONDITION);
                boolean accessTicketRequired = false;
                if (requiredAccessConditions != null && !requiredAccessConditions.isEmpty()) {
                    AccessPermission access =
                            AccessConditionUtils.checkAccessPermission(new HashSet<>(requiredAccessConditions), IPrivilegeHolder.PRIV_LIST,
                                    new StringBuilder().append('+').append(SolrConstants.PI).append(':').append(topStructElement.getPi()).toString(),
                                    (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
                    if (!access.isGranted()) {
                        logger.debug("User may not open {}", topStructElement.getPi());
                        try {
                            throw new RecordNotFoundException(lastReceivedIdentifier);
                        } finally {
                            lastReceivedIdentifier = null;
                        }
                    }
                    // If license type is configured to redirect to a URL, redirect here
                    if (access.isRedirect() && StringUtils.isNotEmpty(access.getRedirectUrl())) {
                        logger.debug("Redirecting to {}", access.getRedirectUrl());
                        try {
                            // reset the bean's state. Otherwise this code will be called again when the redirected page uses
                            // activeDocumentBean at all (may the the case in widgets)
                            reset();
                            FacesContext.getCurrentInstance().getExternalContext().redirect(access.getRedirectUrl());
                            return;
                        } catch (IOException e) {
                            logger.error(e.getMessage());
                            return;
                        }
                    }
                    // Access token required
                    if (access.isAccessTicketRequired()) {
                        logger.trace("Access ticket required");
                        accessTicketRequired = true;
                    }
                }

                PageType pageType = Optional.ofNullable(this.navigationHelper).map(NavigationHelper::getCurrentPageType).orElse(PageType.other);
                String mimeType = topStructElement.getMetadataValue(SolrConstants.MIMETYPE);
                PageNavigation pageNavigation = this.calculateCurrentPageNavigation(pageType);
                boolean showThumbnailGallery =
                        DataManager.getInstance().getConfiguration().showImageThumbnailGallery(new ViewAttributes(viewManager, pageType));
                boolean useEagerLoader = PageNavigation.SEQUENCE.equals(pageNavigation) || showThumbnailGallery;
                // Build in a local variable; publish via single volatile write after full init
                // so that concurrent readers never observe a half-initialized ViewManager.
                ViewManager newViewManager = new ViewManager(topStructElement,
                        AbstractPageLoader.create(topStructElement, true, useEagerLoader),
                        topDocumentIddoc,
                        logid, topStructElement.getMetadataValue(SolrConstants.MIMETYPE), imageDelivery);
                newViewManager.setPageNavigation(pageNavigation);
                newViewManager.setRecordAccessTicketRequired(accessTicketRequired);
                // Publish the new ViewManager before TOC generation so concurrent readers
                // are unblocked. TOC is set after the synchronized block via tocTarget.
                viewManager = newViewManager;
                tocTarget = newViewManager;

                HttpSession session = BeanUtils.getSession();
                // Release all locks for this session except the current record
                if (session != null) {
                    DataManager.getInstance()
                            .getRecordLockManager()
                            .removeLocksForSessionId(session.getId(), Collections.singletonList(viewManager.getPi()));
                }
                String limit = viewManager.getTopStructElement().getMetadataValue(SolrConstants.ACCESSCONDITION_CONCURRENTUSE);
                // Lock limited view records, if limit exists and record has a license type that has this feature enabled
                if (limit != null && AccessConditionUtils.isConcurrentViewsLimitEnabledForAnyAccessCondition(
                        viewManager.getTopStructElement().getMetadataValues(SolrConstants.ACCESSCONDITION))) {
                    if (session != null) {
                        DataManager.getInstance()
                                .getRecordLockManager()
                                .lockRecord(viewManager.getPi(), session.getId(), Integer.valueOf(limit));
                    } else {
                        logger.debug("No session found, unable to lock limited view record {}", topStructElement.getPi());
                        try {
                            throw new RecordLimitExceededException(lastReceivedIdentifier + ":" + limit);
                        } finally {
                            lastReceivedIdentifier = null;
                        }
                    }
                }
            }

            // If LOGID is set, update the current element
            if (StringUtils.isNotEmpty(logid) && viewManager != null && !logid.equals(viewManager.getLogId())) {
                // TODO set new values instead of re-creating ViewManager, perhaps
                logger.debug("Find doc by LOGID: {}", logid);
                String query = new StringBuilder("+")
                        .append(SolrConstants.LOGID)
                        .append(":\"")
                        .append(logid)
                        .append("\" +")
                        .append(SolrConstants.PI_TOPSTRUCT)
                        .append(":")
                        .append(viewManager.getPi())
                        .append(" +")
                        .append(SolrConstants.DOCTYPE)
                        .append(':')
                        .append(DocType.DOCSTRCT.name())
                        .toString();
                SolrDocumentList docList = DataManager.getInstance()
                        .getSearchIndex()
                        .search(query, 1, null, Collections.singletonList(SolrConstants.IDDOC));
                String subElementIddoc = null;
                // TODO check whether creating a new ViewManager can be avoided here
                if (!docList.isEmpty()) {
                    subElementIddoc = (String) docList.get(0).getFieldValue(SolrConstants.IDDOC);
                    // Re-initialize ViewManager with the new current element.
                    // Build in local variable; publish via single volatile write after full init.
                    PageOrientation firstPageOrientation = viewManager.getFirstPageOrientation();
                    PageNavigation pageNavigation = viewManager.getPageNavigation();
                    ViewManager newLogidViewManager = new ViewManager(viewManager.getTopStructElement(), viewManager.getPageLoader(),
                            subElementIddoc, logid, viewManager.getMimeType(), imageDelivery);
                    newLogidViewManager.setPageNavigation(pageNavigation);
                    newLogidViewManager.setFirstPageOrientation(firstPageOrientation);
                    // Publish before TOC generation; tocTarget tracks what needs a TOC.
                    viewManager = newLogidViewManager;
                    tocTarget = newLogidViewManager;
                } else {
                    // Include PI so the warning identifies the affected record
                    logger.warn("{} not found for LOGID '{}' in record '{}'.", SolrConstants.IDDOC, logid, viewManager.getPi());
                }
            }

            if (viewManager != null && viewManager.getCurrentStructElement() != null) {
                viewManager.setDoublePageMode(doublePageMode);
                StructElement structElement = viewManager.getCurrentStructElement();
                if (!structElement.isExists()) {
                    logger.trace("StructElement {} is not marked as existing. Record will be reloaded", structElement.getLuceneId());
                    try {
                        throw new IDDOCNotFoundException(lastReceivedIdentifier + " - " + structElement.getLuceneId());
                    } finally {
                        lastReceivedIdentifier = null;
                    }
                }
                if (structElement.isAnchor()) {
                    anchor = true;
                }
                if (structElement.isVolume()) {
                    volume = true;
                }
                if (structElement.isGroup()) {
                    group = true;
                }

                viewManager.setCurrentImageOrderString(imageToShow);
                viewManager.updateDropdownSelected();

                // Search hit navigation
                if (searchBean != null && searchBean.getCurrentSearch() != null) {
                    if (searchBean.getCurrentHitIndex() < 0) {
                        // Determine the index of this element in the search result list.
                        // Must be done after re-initializing ViewManager so that the PI is correct!
                        searchBean.findCurrentHitIndex(getPersistentIdentifier(), viewManager.getCurrentImageOrder(), true);
                    } else if (mayChangeHitIndex) {
                        // Modify the current hit index
                        searchBean.increaseCurrentHitIndex();
                    } else if (searchBean.getHitIndexOperand() != 0) {
                        // Reset hit index operand (should only be necessary if the URL was called twice, but the current hit has not changed
                        // logger.trace("Hit index modifier operand is {}, resetting...", searchBean.getHitIndexOperand()); //NOSONAR Debug
                        searchBean.setHitIndexOperand(0);
                    }
                }
            } else {
                logger.debug("ViewManager is null or ViewManager.currentDocument is null.");
                try {
                    throw new RecordNotFoundException(lastReceivedIdentifier);
                } finally {
                    lastReceivedIdentifier = null;
                }
            }

            // Metadata language versions
            recordLanguages = viewManager.getTopStructElement().getMetadataValues(SolrConstants.LANGUAGE);
            // If the record has metadata language versions, pre-select the current locale as the record language
            //            if (StringUtils.isBlank(selectedRecordLanguage) && !recordLanguages.isEmpty()) {
            if (selectedRecordLanguage == null && navigationHelper != null) {
                selectedRecordLanguage = DataManager.getInstance().getLanguageHelper().getLanguage(navigationHelper.getLocaleString());
            }

            // Prepare a new bookshelf item
            if (bookmarkBean != null) {
                bookmarkBean.prepareItemForBookmarkList();
                if (bookmarkBean.getCurrentBookmark() == null || !viewManager.getPi().equals(bookmarkBean.getCurrentBookmark().getPi())) {
                    bookmarkBean.prepareItemForBookmarkList();
                }
            }
        } // end synchronized(this)

        // Build the TOC outside the monitor. createTOC() reads this.viewManager (volatile),
        // which already points to tocTarget at this point. If another thread concurrently
        // replaces viewManager, the setToc() write lands on the now-stale tocTarget which
        // nobody reads anymore — a benign race.
        if (tocTarget != null) {
            tocTarget.setToc(createTOC());
        }
    }

    /**
     *
     * @return true if the 'imageToShow' part of the url matches {@link #DOUBLE_PAGE_PATTERN}, i.e. if the url suggests that double page mode is
     *         expected
     */
    private boolean isDoublePageUrl() {
        return (StringUtils.isBlank(imageToShow) && getViewManager().isDoublePageMode())
                || (StringUtils.isNotBlank(imageToShow) && imageToShow.matches(DOUBLE_PAGE_PATTERN));
    }

    /**
     * @return created {@link TOC}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    private TOC createTOC() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        TOC toc = new TOC();
        // Single volatile read to avoid TOCTOU race: reset() on another thread may set
        // this.viewManager to null between a null-check and the subsequent field accesses.
        // synchronized(toc) was removed — toc is a local variable, no other thread can
        // acquire its monitor, so it provided no thread safety.
        ViewManager vm = this.viewManager;
        if (vm != null) {
            toc.generate(vm.getTopStructElement(), vm.isListAllVolumesInTOC(), vm.getMimeType(), tocCurrentPage);
            // The TOC object will correct values that are too high, so update the local value, if necessary
            if (toc.getCurrentPage() != this.tocCurrentPage) {
                this.tocCurrentPage = toc.getCurrentPage();
            }
        }
        return toc;
    }

    /**
     * Pretty-URL entry point.
     *
     * @return an empty string after initializing the record view for the current URL
     * @throws io.goobi.viewer.exceptions.RecordNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.RecordDeletedException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.RecordLimitExceededException
     */
    public String open()
            throws RecordNotFoundException, RecordDeletedException, IndexUnreachableException, DAOException, ViewerConfigurationException,
            RecordLimitExceededException {
        // update() handles its own synchronization. Removing the outer synchronized
        // block here is essential so that the post-lock TOC generation in update()
        // actually runs without a monitor, allowing concurrent session threads to
        // proceed through setPersistentIdentifier() and update() Phase 1 while the
        // TOC is being built.
        logger.trace("open()");
        try {
            update();
            // Capture the published ViewManager once via volatile read; all subsequent
            // operations in this method use this local reference so they are consistent
            // even if another thread concurrently replaces viewManager.
            ViewManager vm = this.viewManager;
            if (navigationHelper == null || vm == null) {
                return "";
            }

            //update usage statistics
            DataManager.getInstance()
                    .getUsageStatisticsRecorder()
                    .recordRequest(RequestType.RECORD_VIEW, vm.getPi(), BeanUtils.getRequest());

            IMetadataValue name = vm.getTopStructElement().getMultiLanguageDisplayLabel();
            HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            URL url = PrettyContext.getCurrentInstance(request).getRequestURL();
            List<String> languages = new ArrayList<>(name.getLanguages()); //temporary variable to avoid ConcurrentModificationException
            Map<String, String> truncatedNames = new HashMap<>();
            for (String language : languages) {
                String translation = name.getValue(language).orElse(vm.getPi());
                if (translation != null && translation.length() > DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()) {
                    translation =
                            new StringBuilder(translation.substring(0, DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()))
                                    .append("...")
                                    .toString();
                    truncatedNames.put(language, translation);
                }
            }
            // Replace translation outside of the loop
            if (!truncatedNames.isEmpty()) {
                for (Entry<String, String> entry : truncatedNames.entrySet()) {
                    name.setValue(entry.getValue(), entry.getKey());
                }
            }
            // Fallback using the identifier as the label
            if (name.isEmpty()) {
                name.setValue(vm.getPi());
            }
            logger.trace("topdocument label: {} ", name.getValue());
            if (!PrettyContext.getCurrentInstance(request).getRequestURL().toURL().contains("/crowd")) {
                breadcrumbBean.addRecordBreadcrumbs(vm, name, url);
            }
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage(), e);
            Messages.error(e.getMessage());
        } catch (IDDOCNotFoundException e) {
            try {
                return reload(lastReceivedIdentifier);
            } catch (PresentationException e1) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage(), e);
            }
        }

        reloads = 0;
        return "";
    }

    /**
     * openFulltext.
     *
     * @return the view name for the fulltext view after opening the record
     * @throws io.goobi.viewer.exceptions.RecordNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.RecordDeletedException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.RecordLimitExceededException
     * @throws java.lang.NumberFormatException
     */
    public String openFulltext()
            throws RecordNotFoundException, RecordDeletedException, IndexUnreachableException, DAOException, ViewerConfigurationException,
            NumberFormatException, RecordLimitExceededException {
        open();
        return "viewFulltext";
    }

    /**
     * Getter for the field <code>prevHit</code>.
     *
     * @return the previous search hit browse element, or null if there is none
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public BrowseElement getPrevHit() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (prevHit == null && searchBean != null) {
            prevHit = searchBean.getPreviousElement();
        }

        return prevHit;
    }

    /**
     * Getter for the field <code>nextHit</code>.
     *
     * @return the next search hit browse element, or null if there is none
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public BrowseElement getNextHit() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (nextHit == null && searchBean != null) {
            nextHit = searchBean.getNextElement();
        }

        return nextHit;
    }

    /**
     * getCurrentElement.
     *
     * @return the {@link io.goobi.viewer.model.viewer.StructElement} for the currently displayed structural unit, or null if no record is loaded
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public StructElement getCurrentElement() throws IndexUnreachableException {
        if (viewManager != null) {
            return viewManager.getCurrentStructElement();
        }

        return null;
    }

    /**
     * 
     * @throws NumberFormatException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws IDDOCNotFoundException
     * @throws RecordNotFoundException
     * @throws RecordDeletedException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @throws RecordLimitExceededException
     * @throws IllegalUrlParameterException
     */
    public void setCurrentImageOrderPerScript()
            throws NumberFormatException, IndexUnreachableException, PresentationException, IDDOCNotFoundException, RecordNotFoundException,
            RecordDeletedException, DAOException, ViewerConfigurationException, RecordLimitExceededException, IllegalUrlParameterException {
        String order = Faces.getRequestParameter("order");
        setImageToShow(order);
        update();
        Optional.ofNullable(this.viewManager).ifPresent(viewManager -> {
            JSONObject json = new JSONObject();
            json.put("iddoc", viewManager.getCurrentStructElementIddoc());
            json.put("page", viewManager.getCurrentImageOrder());
            this.tocUpdateChannel.send(json.toString());
        });
    }

    /**
     * Setter for the field <code>imageToShow</code>.
     *
     * @param imageToShow Single page number (1) or range (2-3)
     * @throws IllegalUrlParameterException
     */
    public void setImageToShow(String imageToShow) throws IllegalUrlParameterException {
        synchronized (lock) {
            if (StringUtils.isNotEmpty(imageToShow) && imageToShow.matches("^\\d+(-\\d+)?$")) {
                this.imageToShow = imageToShow;
            } else {
                //                logger.warn("The passed image number '{}' contains illegal characters, setting to '1'...", imageToShow);
                //                this.imageToShow = "1";
                throw new IllegalUrlParameterException("Illegal page number(s): " + imageToShow);
            }
            if (viewManager != null) {
                viewManager.setDropdownSelected(String.valueOf(this.imageToShow));
            }
            // Reset LOGID (the LOGID setter is called later by PrettyFaces, so if a value is passed, it will still be set)
            try {
                setLogid("");
            } catch (IllegalUrlParameterException e) {
                //cannot be thrown here since "" is always valid
            }
            logger.trace("imageToShow: {}", this.imageToShow);
        }
    }

    /**
     * Sets imageToShow to the representative page found in the search index, or "1" if none found.
     *
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws IllegalUrlParameterException
     * @should use default image "1" when no identifier is set
     * @should use default image "1" when identifier is the dash sentinel
     * @should use default image when no identifier set
     * @should use default image when identifier is dash sentinel
     */
    public void setRepresentativeImage()
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException, IllegalUrlParameterException {
        logger.trace("setRepresentativeImage"); //NOSONAR Debug
        // Capture the volatile field once so we can do Solr I/O outside the lock and later verify
        // consistency: if a concurrent request replaced lastReceivedIdentifier in the meantime
        // (e.g. two tabs opened simultaneously), we discard this result rather than overwriting the
        // correct value that the other thread will set.
        String identifier = this.lastReceivedIdentifier;
        String image = "1";
        if (StringUtils.isNotEmpty(identifier) && !"-".equals(identifier)) {
            SolrDocument doc = DataManager.getInstance()
                    .getSearchIndex()
                    .getFirstDoc(SolrConstants.PI + ":" + identifier, Collections.singletonList(SolrConstants.THUMBPAGENO));
            if (doc != null && doc.getFieldValue(SolrConstants.THUMBPAGENO) != null) {
                image = String.valueOf(doc.getFieldValue(SolrConstants.THUMBPAGENO));
                logger.trace("{} found: {}", SolrConstants.THUMBPAGENO, image);
            } else {
                logger.trace("{}  not found, using {}", SolrConstants.THUMBPAGENO, image);
            }
        }
        synchronized (lock) {
            // Consistency check: bail out if a concurrent navigation replaced the identifier while
            // we were doing Solr I/O. The other thread will set the correct value for its record.
            if (!Objects.equals(identifier, this.lastReceivedIdentifier)) {
                return;
            }
            boolean isDoublePageNavigation = Optional.ofNullable(this.viewManager)
                    .map(ViewManager::getPageNavigation)
                    .map(pageNavigation -> PageNavigation.DOUBLE == pageNavigation)
                    .orElse(DataManager.getInstance()
                            .getConfiguration()
                            .isDoublePageNavigationDefault(new ViewAttributes(this.viewManager, this.navigationHelper.getCurrentPageType())));
            if (isDoublePageNavigation) {
                image = String.format("%s-%s", image, image);
            }
            setImageToShow(image);
        }
    }

    /**
     * Getter for the field <code>imageToShow</code>.
     *
     * @return single page number (e.g. "1") or range (e.g. "2-3") of the image(s) currently displayed
     */
    public String getImageToShow() {
        synchronized (lock) {
            return imageToShow;
        }
    }

    /**
     * Setter for the field <code>logid</code>.
     *
     * @param logid structural element LOGID to navigate to, or "-" / empty for the top-level document
     * @throws io.goobi.viewer.exceptions.PresentationException
     */
    public void setLogid(String logid) throws IllegalUrlParameterException {
        synchronized (this) {
            if ("-".equals(logid) || StringUtils.isEmpty(logid)) {
                this.logid = "";
            } else if (StringUtils.isNotBlank(logid) && logid.matches("[\\w-]+")) {
                this.logid = SolrTools.escapeSpecialCharacters(logid);
            } else {
                // Illegal logId in URL — surface as a user-facing error without the exception class prefix
                throw new IllegalUrlParameterException(
                        "The passed logId " + SolrTools.escapeSpecialCharacters(logid) + " contains illegal characters");
            }
        }
    }

    /**
     * Getter for the field <code>logid</code>.
     *
     * @return the LOGID of the current structural element, or "-" if at top-level document
     */
    public String getLogid() {
        // logid is volatile; String is immutable — no lock needed for a read
        if (StringUtils.isEmpty(logid)) {
            return "-";
        }

        return logid;
    }

    /**
     * isAnchor.
     *
     * @return true if the current record is an anchor document (e.g. a multi-volume work); false otherwise
     */
    public boolean isAnchor() {
        return anchor;
    }

    /**
     * Setter for the field <code>anchor</code>.
     *
     * @param anchor true if the current record is an anchor document
     */
    public void setAnchor(boolean anchor) {
        this.anchor = anchor;
    }

    /**
     * isVolume.
     *
     * @return true if the current record is a volume (child of a multi-volume work), false otherwise
     */
    public boolean isVolume() {
        return volume;
    }

    /**
     * isGroup.
     *
     * @return true if the current record is a group document, false otherwise
     */
    public boolean isGroup() {
        return group;
    }

    /**
     * Getter for the field <code>action</code>.
     *
     * @return the navigation action string (e.g. "nextHit", "prevHit"), or null if none set
     */
    public String getAction() {
        // action is volatile; no lock needed for a single-field read
        return action;
    }

    /**
     * Setter for the field <code>action</code>.
     *
     * @param action navigation action string (e.g. "nextHit", "prevHit") to execute
     */
    public void setAction(String action) {
        synchronized (this) {
            logger.trace("setAction: {}", action);
            this.action = action;
            if (searchBean != null && action != null) {
                switch (action) {
                    case "nextHit":
                        searchBean.setHitIndexOperand(1);
                        break;
                    case "prevHit":
                        searchBean.setHitIndexOperand(-1);
                        break;
                    default:
                        // do nothing
                        break;

                }
            }
        }
    }

    /**
     * getPIFromFieldValue.
     *
     * @param value field value to search for
     * @param field Solr field name to query
     * @return the persistent identifier of the first record matching the field/value query, or empty string if none found
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPIFromFieldValue(String value, String field) throws PresentationException, IndexUnreachableException {
        String query = "{field}:\"{value}\"".replace("{field}", field).replace("{value}", value);
        SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, List.of(SolrConstants.PI));
        return Optional.ofNullable(doc).map(d -> d.getFirstValue(SolrConstants.PI)).map(Object::toString).orElse("");
    }

    /**
     * setPersistentIdentifier.
     *
     * @param persistentIdentifier persistent identifier of the record to load
     * @should resolve topDocumentIddoc from Solr when PI is set
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.RecordNotFoundException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should preserve last received identifier when not found
     */
    public void setPersistentIdentifier(String persistentIdentifier)
            throws PresentationException, RecordNotFoundException, IndexUnreachableException {
        logger.trace("setPersistentIdentifier: {}", StringTools.stripPatternBreakingChars(persistentIdentifier));
        if (!PIValidator.validatePi(persistentIdentifier)) {
            logger.warn("Invalid identifier '{}'.", persistentIdentifier);
            synchronized (this) {
                reset();
            }
            return;
        }

        // Perform the Solr IDDOC lookup outside the monitor so the lock is not held
        // during I/O. The volatile read of viewManager is safe here; the subsequent
        // write to topDocumentIddoc is protected by the synchronized block below.
        String id = null;
        ViewManager vm = viewManager;
        if (!"-".equals(persistentIdentifier) && (vm == null || !persistentIdentifier.equals(vm.getPi()))) {
            id = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(persistentIdentifier);
            if (id == null) {
                logger.warn("No IDDOC for identifier '{}' found.", persistentIdentifier);
            }
        }

        synchronized (this) {
            lastReceivedIdentifier = persistentIdentifier;
            // Re-check with the lock held in case viewManager was concurrently replaced
            if (!"-".equals(persistentIdentifier) && (viewManager == null || !persistentIdentifier.equals(viewManager.getPi()))) {
                if (id != null) {
                    if (!id.equals(topDocumentIddoc)) {
                        topDocumentIddoc = id;
                        logger.trace("IDDOC found for {}: {}", persistentIdentifier, id);
                    }
                } else {
                    reset();
                    // Restore the identifier after reset so that update() can include it in the RecordNotFoundException message
                    lastReceivedIdentifier = persistentIdentifier;
                }
            }
        }
    }

    /**
     * Returns the PI of the currently loaded record. Only call this method after the update() method has re-initialized ViewManager, otherwise the
     * previous PI may be returned!
     *
     * @return the persistent identifier of the currently loaded record, or "-" if no record is loaded
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should be thread safe after record load
     */
    public String getPersistentIdentifier() throws IndexUnreachableException {
        // Capture volatile reference once; ViewManager.getPi() is immutable after construction
        ViewManager vm = viewManager;
        if (vm != null) {
            return vm.getPi();
        }
        return "-";
    }

    // navigation in work

    /**
     * Returns the navigation URL for the given page type and number.
     *
     * @param pageType view page type name, e.g. "viewObject"
     * @param pageOrderRange Single page number or range
     * @return the absolute URL to the specified page type and order for the current record
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should return correct page in single page mode
     * @should return correct range in double page mode if currently showing one page
     * @should return correct range in double page mode if currently showing two pages
     * @should return correct range in double page mode if current page double image
     */
    public String getPageUrl(final String pageType, String pageOrderRange) throws IndexUnreachableException {
        StringBuilder sbUrl = new StringBuilder();
        String localPageType = pageType;
        if (StringUtils.isBlank(localPageType)) {
            if (navigationHelper != null) {
                localPageType = navigationHelper.getCurrentView();
                if (localPageType == null) {
                    localPageType = PageType.viewObject.name();
                }
            }
            if (StringUtils.isBlank(localPageType)) {
                localPageType = PageType.viewObject.name();
            }
            // logger.trace("current view: {}", localPageType); //NOSONAR Debug
        }

        int[] pages = StringTools.getIntegerRange(pageOrderRange);
        int page = pages[0];
        int page2 = pages[1];

        // Capture both viewManager and pageLoader once to avoid TOCTOU with concurrent reset():
        // viewManager is volatile, so each direct field read can yield null after another thread calls reset().
        ViewManager vm = this.viewManager;
        var loader = vm != null ? vm.getPageLoader() : null;
        if (loader != null) {
            page = Math.max(page, loader.getFirstPageOrder());
            page = Math.min(page, loader.getLastPageOrder());
            if (page2 != Integer.MAX_VALUE) {
                page2 = Math.max(page2, loader.getFirstPageOrder());
                page2 = Math.min(page2, loader.getLastPageOrder());
            }
        }

        String range = page + (page2 != Integer.MAX_VALUE ? "-" + page2 : "");
        // logger.trace("final range: {}", range); //NOSONAR Debug
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                .append('/')
                .append(PageType.getByName(localPageType).getName())
                .append('/')
                .append(getPersistentIdentifier())
                .append('/')
                .append(range)
                .append('/');

        return sbUrl.toString();
    }

    /**
     * getPageUrl.
     *
     * @param pageOrderRange Single page number or range
     * @return the absolute URL to the current view page type for the given page order range
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPageUrl(String pageOrderRange) throws IndexUnreachableException {
        return getPageUrl(null, pageOrderRange);
    }

    /**
     * getPageUrl.
     *
     * @return the absolute URL to the current page in the preferred or currently active view
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPageUrl() throws IndexUnreachableException {
        String pageType = navigationHelper.getPreferredView();
        if (StringUtils.isBlank(pageType)) {
            pageType = navigationHelper.getCurrentView();
        }

        return getPageUrlByType(pageType);
    }

    /**
     * getPageUrl.
     *
     * @param pageType view page type name, e.g. "viewObject"
     * @return the absolute URL for the given view page type of the currently loaded record
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPageUrlByType(String pageType) throws IndexUnreachableException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                .append('/')
                .append(PageType.getByName(pageType).getName())
                .append('/')
                .append(getPersistentIdentifier())
                .append('/');

        return sbUrl.toString();
    }

    /**
     * getFirstPageUrl.
     *
     * @return the absolute URL to the first page of the current record, or null if no record is loaded
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getFirstPageUrl() throws IndexUnreachableException {
        if (viewManager != null && viewManager.getPageLoader() != null) {
            int image = viewManager.getPageLoader().getFirstPageOrder();
            if (viewManager.isDoublePageMode()) {
                return getPageUrl(image + "-" + image);
            }

            return getPageUrl(Integer.toString(image));
        }

        return null;
    }

    /**
     * getLastPageUrl.
     *
     * @return the absolute URL to the last page of the current record, or null if no record is loaded
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getLastPageUrl() throws IndexUnreachableException {
        if (viewManager != null && viewManager.getPageLoader() != null) {
            int image = viewManager.getPageLoader().getLastPageOrder();
            if (viewManager.isDoublePageMode()) {
                return getPageUrl(image + "-" + image);
            }

            return getPageUrl(Integer.toString(image));
        }

        return null;
    }

    /**
     * getNextPageUrl.
     *
     * @param step number of pages to advance (positive or negative)
     * @return the absolute URL to the page that is the given number of steps from the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should not throw NPE when viewManager is null
     * @should not throw NPE when current page is null
     */
    public String getPageUrlRelativeToCurrentPage(int step) throws IndexUnreachableException {
        // logger.trace("getPageUrl: {}", step); //NOSONAR Debug
        // Single volatile read to avoid TOCTOU race: reset() on another thread may set
        // this.viewManager to null between the null-check and any subsequent field read.
        final ViewManager vm = this.viewManager;
        if (vm == null) {
            return getPageUrl(imageToShow);
        }

        if (!vm.isDoublePageMode()) {
            int number = vm.getCurrentImageOrder() + step;
            return getPageUrl(String.valueOf(number));
        }

        int number;

        // Capture getCurrentPage() once to avoid a secondary TOCTOU: concurrent reset() can
        // null out the pageLoader between the null-check and the isDoubleImage() call.
        PhysicalElement currentPage = vm.getCurrentPage();
        if (currentPage != null && currentPage.isDoubleImage()) {
            // logger.trace("{} is double page", currentPage.getOrder()); //NOSONAR Debug
            if (step < 0) {
                number = vm.getCurrentImageOrder() + 2 * step;
            } else {
                number = vm.getCurrentImageOrder() + step;
            }
            return getPageUrl(number + "-" + (number + 1));
        }

        // Use current left/right page as a point of reference, if available (opposite when in right-to-left navigation)
        Optional<PhysicalElement> currentLeftPage =
                vm.getTopStructElement().isRtl() ? vm.getCurrentRightPage() : vm.getCurrentLeftPage();
        Optional<PhysicalElement> currentRightPage =
                vm.getTopStructElement().isRtl() ? vm.getCurrentLeftPage() : vm.getCurrentRightPage();

        // Only go back one step unit at first
        if (currentLeftPage.isPresent()) {
            // logger.trace("{} is left page", currentLeftPage.get().getOrder()); //NOSONAR Debug
            number = currentLeftPage.get().getOrder() + step;
        } else if (currentRightPage.isPresent()) {
            // If only the right page is present, it's probably the first page - do not add step at this point
            // logger.trace("{} is right page", currentRightPage.get().getOrder()); //NOSONAR Debug
            number = currentRightPage.get().getOrder();
        } else {
            number = vm.getCurrentImageOrder() + step;
        }

        // Target image candidate contains two pages
        Optional<PhysicalElement> nextPage = vm.getPage(number);
        if (nextPage.isPresent() && nextPage.get().isDoubleImage()) {
            return getPageUrl(String.valueOf(number) + "-" + number);
        }
        // If the immediate neighbor is not a double image, add another step
        number += step;

        nextPage = vm.getPage(number);
        if (nextPage.isPresent() && nextPage.get().isDoubleImage()) {
            return getPageUrl(String.valueOf(number) + "-" + number);
        }

        // logger.trace("step: {}", step); //NOSONAR Debug
        // logger.trace("Number: {}", number); //NOSONAR Debug

        return getPageUrl(number + "-" + (number + 1));
    }

    /**
     * getPageUrl.
     *
     * @param order physical page order number
     * @return Page URL for the given page number
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     */
    public String getPageUrl(int order) throws IndexUnreachableException {
        return getPageUrl(Integer.toString(order));
    }

    /**
     * getPreviousPageUrl.
     *
     * @param step number of pages to go back
     * @return the absolute URL to the page that is the given number of steps before the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPreviousPageUrl(int step) throws IndexUnreachableException {
        return getPageUrlRelativeToCurrentPage(step * -1);
    }

    /**
     * getNextPageUrl.
     *
     * @param step number of pages to advance forward
     * @return the absolute URL to the page that is the given number of steps after the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getNextPageUrl(int step) throws IndexUnreachableException {
        return getPageUrlRelativeToCurrentPage(step);
    }

    /**
     * getPreviousPageUrl.
     *
     * @return the absolute URL to the page one step before the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getPreviousPageUrl() throws IndexUnreachableException {
        return getPreviousPageUrl(1);
    }

    /**
     * getNextPageUrl.
     *
     * @return the absolute URL to the page one step after the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getNextPageUrl() throws IndexUnreachableException {
        return getNextPageUrl(1);
    }

    /**
     * getPreviousDocstructUrl.
     *
     * @return URL to the previous docstruct
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws io.goobi.viewer.exceptions.PresentationException
     */
    public String getPreviousDocstructUrl() throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        // logger.trace("getPreviousDocstructUrl"); //NOSONAR Debug
        if (viewManager == null) {
            return null;
        }
        List<String> docstructTypes =
                DataManager.getInstance().getConfiguration().getDocstructNavigationTypes(viewManager.getTopStructElement().getDocStructType(), true);
        if (docstructTypes.isEmpty()) {
            return null;
        }

        String currentDocstructIddoc = String.valueOf(viewManager.getCurrentStructElementIddoc());
        // Determine docstruct URL and cache it
        if (prevDocstructUrlCache.get(currentDocstructIddoc) == null) {
            int currentElementIndex = getToc().findTocElementIndexByIddoc(currentDocstructIddoc);
            if (currentElementIndex == -1) {
                logger.warn("Current IDDOC not found in TOC: {}", viewManager.getCurrentStructElement().getLuceneId());
                return null;
            }

            boolean found = false;
            for (int i = currentElementIndex - 1; i >= 0; --i) {
                TOCElement tocElement = viewManager.getToc().getTocElements().get(i);
                String docstructType = tocElement.getMetadataValue(SolrConstants.DOCSTRCT);
                if (docstructType != null && docstructTypes.contains(docstructType) && StringUtils.isNotBlank(tocElement.getPageNo())) {
                    logger.trace("Found previous {}: {}", docstructType, tocElement.getLogId());
                    // Add LOGID to the URL because ViewManager.currentStructElementIddoc (IDDOC_OWNER) can be incorrect in the index sometimes,
                    // resulting in the URL pointing at the current element
                    prevDocstructUrlCache.put(currentDocstructIddoc,
                            "/" + viewManager.getPi() + "/" + Integer.valueOf(tocElement.getPageNo()) + "/" + tocElement.getLogId() + "/");
                    found = true;
                    break;
                }
            }
            if (!found) {
                prevDocstructUrlCache.put(currentDocstructIddoc, "");
            }
        }

        if (StringUtils.isNotEmpty(prevDocstructUrlCache.get(currentDocstructIddoc))) {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + navigationHelper.getCurrentPageType().getName()
                    + prevDocstructUrlCache.get(currentDocstructIddoc);
        }

        return "";
    }

    /**
     * getNextDocstructUrl.
     *
     * @return URL to the next docstruct
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws io.goobi.viewer.exceptions.PresentationException
     */
    public String getNextDocstructUrl() throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        // logger.trace("getNextDocstructUrl"); //NOSONAR Debug
        if (viewManager == null) {
            return "";
        }
        List<String> docstructTypes =
                DataManager.getInstance().getConfiguration().getDocstructNavigationTypes(viewManager.getTopStructElement().getDocStructType(), true);
        if (docstructTypes.isEmpty()) {
            return null;
        }

        String currentDocstructIddoc = String.valueOf(viewManager.getCurrentStructElementIddoc());
        // Determine docstruct URL and cache it
        if (nextDocstructUrlCache.get(currentDocstructIddoc) == null) {
            int currentElementIndex = getToc().findTocElementIndexByIddoc(currentDocstructIddoc);
            logger.trace("currentIndexElement: {}", currentElementIndex);
            if (currentElementIndex == -1) {
                return null;
            }

            boolean found = false;
            for (int i = currentElementIndex + 1; i < viewManager.getToc().getTocElements().size(); ++i) {
                TOCElement tocElement = viewManager.getToc().getTocElements().get(i);
                String docstructType = tocElement.getMetadataValue(SolrConstants.DOCSTRCT);
                if (docstructType != null && docstructTypes.contains(docstructType)) {
                    logger.trace("Found next {}: {}", docstructType, tocElement.getLogId());
                    // Add LOGID to the URL because ViewManager.currentStructElementIddoc (IDDOC_OWNER) can be incorrect in the index sometimes,
                    // resulting in the URL pointing at the current element
                    nextDocstructUrlCache.put(currentDocstructIddoc,
                            "/" + viewManager.getPi() + "/" + Integer.valueOf(tocElement.getPageNo()) + "/" + tocElement.getLogId() + "/");
                    found = true;
                    break;
                }
            }
            if (!found) {
                nextDocstructUrlCache.put(currentDocstructIddoc, "");
            }
        }

        if (StringUtils.isNotEmpty(nextDocstructUrlCache.get(currentDocstructIddoc))) {
            return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + navigationHelper.getCurrentPageType().getName()
                    + nextDocstructUrlCache.get(currentDocstructIddoc);
        }

        return "";
    }

    /**
     * getImageUrl.
     *
     * @return the absolute URL to the image view for the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getImageUrl() throws IndexUnreachableException {
        return getPageUrl(PageType.viewImage.getName(), imageToShow);
    }

    /**
     * getFullscreenImageUrl.
     *
     * @return the absolute URL to the fullscreen image view for the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should not throw NPE when getCurrentPage returns null in double page mode
     * @should not throw NPE when current page is null
     */
    public String getFullscreenImageUrl() throws IndexUnreachableException {
        // Capture vm once so that a concurrent reset() cannot null the volatile field mid-method.
        // Also capture getCurrentPage() once to avoid TOCTOU between the null check and isDoubleImage().
        ViewManager vm = this.viewManager;
        if (vm != null && vm.isDoublePageMode()) {
            PhysicalElement currentPage = vm.getCurrentPage();
            if (currentPage != null && !currentPage.isDoubleImage()) {
                Optional<PhysicalElement> currentLeftPage = vm.getCurrentLeftPage();
                Optional<PhysicalElement> currentRightPage = vm.getCurrentRightPage();
                if (currentLeftPage.isPresent() && currentRightPage.isPresent()) {
                    return getPageUrl(PageType.viewFullscreen.getName(), currentLeftPage.get().getOrder() + "-" + currentRightPage.get().getOrder());
                }
            }
        }

        return getPageUrl(PageType.viewFullscreen.getName(), imageToShow);
    }

    /**
     * getFulltextUrl.
     *
     * @return the absolute URL to the fulltext view for the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getFulltextUrl() throws IndexUnreachableException {
        return getPageUrl(PageType.viewFulltext.getName(), imageToShow);
    }

    /**
     * getMetadataUrl.
     *
     * @return the absolute URL to the metadata view for the current page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getMetadataUrl() throws IndexUnreachableException {
        return getPageUrl(PageType.viewMetadata.getName(), imageToShow);
    }

    /**
     * getTopDocument.
     *
     * @return the top-level StructElement of the current record, or null if no record is loaded
     */
    public StructElement getTopDocument() {
        if (viewManager != null) {
            return viewManager.getTopStructElement();
        }

        return null;
    }

    /**
     * setChildrenVisible.
     *
     * @param element TOC element whose children to make visible
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void setChildrenVisible(TOCElement element)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (getToc() != null) {
            synchronized (getToc()) {
                getToc().setChildVisible(element.getID());
                getToc().getActiveElement();
            }
        }
    }

    /**
     * setChildrenInvisible.
     *
     * @param element TOC element whose children to hide
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void setChildrenInvisible(TOCElement element)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (getToc() != null) {
            synchronized (getToc()) {
                getToc().setChildInvisible(element.getID());
                getToc().getActiveElement();
            }
        }
    }

    /**
     * Recalculates the visibility of TOC elements and jumps to the active element after a +/- button has been pressed.
     *
     * @return the anchor fragment URL of the active TOC element, or null if none is active
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String calculateSidebarToc()
            throws IOException, PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (getToc() != null) {
            TOCElement activeTocElement = getToc().getActiveElement();
            if (activeTocElement != null) {
                String result = new StringBuilder("#").append(activeTocElement.getLogId()).toString();
                FacesContext.getCurrentInstance().getExternalContext().redirect(result);
                return result;
            }
        }

        return null;
    }

    /**
     * Getter for the field <code>toc</code>.
     *
     * @return the {@link io.goobi.viewer.model.toc.TOC} for the currently loaded record, or null if no record is loaded
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @should return null when viewManager is null
     * @should rebuild and cache toc when view manager toc is null
     */
    public TOC getToc() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        ViewManager vm = viewManager;
        if (vm == null) {
            return null;
        }
        // Fast path: TOC already built — single volatile read, no lock needed.
        TOC existing = vm.getToc();
        if (existing != null) {
            return existing;
        }
        // Slow path: build TOC *outside* the ViewManager monitor to eliminate the B1 pattern.
        // Previously, holding the VM lock during createTOC() (which performs Solr I/O via
        // CountDownLatch) caused BLOCKED threads in production whenever two requests for the
        // same record arrived concurrently. Multiple threads may now race through here and each
        // build a TOC; only the first to acquire the lock will publish its result — the extra
        // work is bounded (at most one build per concurrent caller) and acceptable.
        TOC fresh = createTOC();
        synchronized (vm) {
            if (vm.getToc() == null) {
                vm.setToc(fresh);
            }
            return vm.getToc();
        }
    }

    /**
     * Getter for the field <code>tocCurrentPage</code>.
     *
     * @return a int.
     */
    public String getTocCurrentPage() {
        // tocCurrentPage is volatile; no lock needed for a single-field read
        return Integer.toString(tocCurrentPage);
    }

    /**
     * Setter for the field <code>tocCurrentPage</code>.
     *
     * @param tocCurrentPage desired TOC pagination page number
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws io.goobi.viewer.exceptions.IllegalUrlParameterException if tocCurrentPage is not a valid integer or range
     * @should throw IllegalUrlParameterException for non-numeric value
     */
    public void setTocCurrentPage(String tocCurrentPage)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException, IllegalUrlParameterException {
        synchronized (this) {
            // Guard against non-numeric values (e.g. when a record PI like "15849354_1940" is
            // incorrectly injected into this parameter by a mismatched PrettyFaces URL pattern).
            // Wrap NumberFormatException as IllegalUrlParameterException so MyExceptionHandler
            // treats it as an invalid URL (WARN) rather than an application error (ERROR).
            int[] pages;
            try {
                pages = StringTools.getIntegerRange(tocCurrentPage);
            } catch (NumberFormatException e) {
                throw new IllegalUrlParameterException("Illegal TOC page value: " + tocCurrentPage);
            }
            this.tocCurrentPage = pages[0];
            if (this.tocCurrentPage < 1) {
                this.tocCurrentPage = 1;
            }
            // Do not call getToc() here - the setter is usually called before update(),
            // so the required information for proper TOC creation is not yet available
            if (viewManager != null && viewManager.getToc() != null) {
                int currentCurrentPage = viewManager.getToc().getCurrentPage();
                viewManager.getToc().setCurrentPage(this.tocCurrentPage);
                // The TOC object will correct values that are too high, so update the local value, if necessary
                if (viewManager.getToc().getCurrentPage() != this.tocCurrentPage) {
                    this.tocCurrentPage = viewManager.getToc().getCurrentPage();
                }
                // Create a new TOC if pagination is enabled and the paginator page has changed
                if (currentCurrentPage != this.tocCurrentPage
                        && DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage() > 0) {
                    viewManager.getToc()
                            .generate(viewManager.getTopStructElement(), viewManager.isListAllVolumesInTOC(), viewManager.getMimeType(),
                                    this.tocCurrentPage);
                }
            }
        }
    }

    /**
     * getTitleBarLabel.
     *
     * @param locale locale used to select the label language
     * @return the title bar label for the current page in the given locale
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getTitleBarLabel(Locale locale)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        return getTitleBarLabel(locale.getLanguage());
    }

    /**
     * getTitleBarLabel.
     *
     * @return the title bar label for the current page in the active user locale
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getTitleBarLabel() throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        Locale locale = BeanUtils.getLocale();
        if (locale != null && StringUtils.isNotEmpty(locale.getLanguage())) {
            return getTitleBarLabel(locale.getLanguage());
        }

        return getTitleBarLabel(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
    }

    /**
     * getTitleBarLabel.
     *
     * @param language ISO 639 language code for label selection
     * @return the title bar label for the current page in the given language, or null if no label could be determined
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getTitleBarLabel(String language)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        if (navigationHelper == null) {
            return null;
        }
        // Capture once to avoid a race where another thread calls reset() between the null-check
        // and the subsequent direct field accesses (which would cause a NullPointerException).
        ViewManager vm = getViewManager();
        if (navigationHelper.getCurrentPage() != null && PageType.getByName(navigationHelper.getCurrentPage()) != null
                && PageType.getByName(navigationHelper.getCurrentPage()).isDocumentPage() && vm != null) {
            // Prefer the label of the current TOC element
            TOC toc = getToc();
            if (toc != null && toc.getTocElements() != null && !toc.getTocElements().isEmpty()) {
                String label = null;
                String labelTemplate = StringConstants.DEFAULT_NAME;
                if (vm.getTopStructElement() != null) {
                    labelTemplate = vm.getTopStructElement().getDocStructType();
                }
                if (DataManager.getInstance().getConfiguration().isDisplayAnchorLabelInTitleBar(labelTemplate)
                        && StringUtils.isNotBlank(vm.getAnchorPi())) {
                    String prefix = DataManager.getInstance().getConfiguration().getAnchorLabelInTitleBarPrefix(labelTemplate);
                    String suffix = DataManager.getInstance().getConfiguration().getAnchorLabelInTitleBarSuffix(labelTemplate);
                    prefix = ViewerResourceBundle.getTranslation(prefix, Locale.forLanguageTag(language)).replace("_SPACE_", " ");
                    suffix = ViewerResourceBundle.getTranslation(suffix, Locale.forLanguageTag(language)).replace("_SPACE_", " ");
                    label = prefix + toc.getLabel(vm.getAnchorPi(), language) + suffix + toc.getLabel(vm.getPi(), language);
                } else {
                    label = toc.getLabel(vm.getPi(), language);
                }
                if (label != null) {
                    return label;
                }
            }
            String label = vm.getTopStructElement().getLabel(selectedRecordLanguage.getIsoCodeOld());
            if (StringUtils.isNotEmpty(label)) {
                return label;
            }
        } else if (cmsBean != null && navigationHelper.isCmsPage()) {
            CMSPage cmsPage = cmsBean.getCurrentPage();
            if (cmsPage != null) {
                String cmsPageName = StringUtils.isNotBlank(cmsPage.getMenuTitle()) ? cmsPage.getMenuTitle() : cmsPage.getTitle();
                if (StringUtils.isNotBlank(cmsPageName)) {
                    return cmsPageName;
                }
            }
        }

        if (navigationHelper.getCurrentPageType() != null) {
            PageType pageType = navigationHelper.getCurrentPageType();
            if (PageType.other.equals(pageType)) {
                String pageLabel = navigationHelper.getCurrentPage();
                if (StringUtils.isNotBlank(pageLabel)) {
                    return Messages.translate(pageLabel, Locale.forLanguageTag(language));
                }
            }
            return Messages.translate(pageType.getLabel(), Locale.forLanguageTag(language));
        }

        return null;
    }

    /**
     * Title bar label value escaped for JavaScript.
     *
     * @return the title bar label with JavaScript special characters escaped, or null if no label is available
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getLabelForJS() throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        String label = getTitleBarLabel();
        if (label != null) {
            return StringEscapeUtils.escapeEcmaScript(label);
        }

        return null;
    }

    public String getPdfSize() {
        return getPdfSize(null);
    }

    public String getPdfSize(String logId) {
        // pdfSizes and cachedFullPdfSize are volatile; benign double-init race acceptable for display
        if (StringUtils.isNotBlank(logId)) {
            // Section-level PDF size: delegate to PdfSizeCalculator which reads the METS
            // file to resolve which pages belong to the given logical section (logId).
            // This path is only triggered by user interaction (TOC clicks), not on page load.
            try {
                if (this.pdfSizes == null) {
                    this.pdfSizes = new PdfSizeCalculator(getRecordDataset());
                }
                return this.pdfSizes.getPdfSize(logId);
            } catch (PresentationException | IndexUnreachableException | IOException | RecordNotFoundException | NullPointerException e) {
                logger.error("Error getting pdf file sizes for logId '{}': {}", logId, e.toString());
                return "";
            }
        }

        // Full-record PDF size: sum MDNUM_FILESIZE from Solr — avoids the content server's
        // GetMetsPageCountAction.getPdfInfo() which does filesystem I/O for every page
        // (22+ seconds for 7000-page records due to per-page NFS stat calls).
        if (cachedFullPdfSize != null) {
            return cachedFullPdfSize;
        }
        String currentPi = null;
        try {
            currentPi = viewManager != null ? viewManager.getPi() : null;
        } catch (IndexUnreachableException e) {
            logger.warn("Could not resolve PI for pdf size calculation: {}", e.toString());
        }
        if (StringUtils.isBlank(currentPi)) {
            return "";
        }
        try {
            String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + currentPi + " +" + SolrConstants.DOCTYPE + ":PAGE";
            List<SolrDocument> pageDocs = DataManager.getInstance().getSearchIndex()
                    .getDocs(query, List.of(SolrConstants.MDNUM_FILESIZE));
            long totalBytes = 0;
            if (pageDocs != null) {
                for (SolrDocument doc : pageDocs) {
                    Object size = doc.getFieldValue(SolrConstants.MDNUM_FILESIZE);
                    if (size instanceof Number) {
                        totalBytes += ((Number) size).longValue();
                    }
                }
            }
            cachedFullPdfSize = totalBytes > 0 ? FileSizeCalculator.formatSize(totalBytes) : "";
            return cachedFullPdfSize;
        } catch (IndexUnreachableException | PresentationException e) {
            logger.error("Error calculating pdf size from Solr for PI '{}': {}", currentPi, e.toString());
            return "";
        }
    }

    public Path getMetsFilePath() throws IndexUnreachableException {

        String dataRepository = getViewManager().getTopStructElement().getDataRepository();
        String filePath =
                DataFileTools.getSourceFilePath(getPersistentIdentifier() + ".xml", dataRepository, SolrConstants.SOURCEDOCFORMAT_METS);
        return Path.of(filePath);
    }

    /**
     * Getter for the field <code>imageContainerWidth</code>.
     *
     * @return a int.
     */
    public int getImageContainerWidth() {
        return imageContainerWidth;
    }

    /**
     * getNumberOfImages.
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getNumberOfImages() throws IndexUnreachableException {
        if (viewManager != null) {
            return viewManager.getImagesCount();
        }

        return 0;
    }

    /**
     * Getter for the field <code>topDocumentIddoc</code>.
     *
     * @return Not this.topDocumentIddoc but ViewManager.topDocumentIddoc
     */
    public String getTopDocumentIddoc() {
        if (viewManager != null) {
            return viewManager.getTopStructElementIddoc();
        }
        return null;
    }

    /**
     * Indicates whether a record is currently properly loaded in this bean. Use to determine whether to display components.
     *
     * @return true if a record is currently loaded in this bean, false otherwise
     */
    public boolean isRecordLoaded() {
        return viewManager != null;
    }

    /**
     * Checks if there is an anchor in this docStruct's hierarchy.
     *
     * @return true if the current record has an anchor (i.e. is a child of a multi-volume work), false otherwise
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean hasAnchor() throws IndexUnreachableException {
        return getTopDocument().isAnchorChild();
    }

    /**
     * Exports the currently loaded for re-indexing.
     *
     * @return an empty string after triggering the re-indexing of the current record
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.RecordNotFoundException if any.
     */
    public String reIndexRecordAction() throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (viewManager != null) {
            if (IndexerTools.reIndexRecord(viewManager.getPi())) {
                Messages.info("reIndexRecordSuccess");
            } else {
                Messages.error("reIndexRecordFailure");
            }
        }

        return "";
    }

    /**
     * deleteRecordAction.
     *
     * @param keepTraceDocument If true, a .delete file will be created; otherwise a .purge file
     * @return outcome
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String deleteRecordAction(boolean keepTraceDocument) throws IOException, IndexUnreachableException {
        try {
            if (viewManager == null) {
                return "";
            }

            if (IndexerTools.deleteRecord(viewManager.getPi(), keepTraceDocument,
                    Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()))) {
                Messages.info("deleteRecord_success");
                return "pretty:index";
            }
            Messages.error("deleteRecord_failure");
        } finally {
            deleteRecordKeepTrace = null;
        }

        return "";
    }

    /**
     * clearCacheAction.
     *
     * @return empty string
     * @throws java.io.IOException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     */
    public String clearCacheAction() throws IOException, IndexUnreachableException {
        logger.trace("clearCacheAction: {}", clearCacheMode);
        if (clearCacheMode == null || viewManager == null) {
            return "";
        }

        String url = NetTools.buildClearCacheUrl(clearCacheMode, viewManager.getPi(), navigationHelper.getApplicationUrl(),
                DataManager.getInstance().getConfiguration().getWebApiToken());
        try {
            try {
                NetTools.getWebContentDELETE(url, null, null, null, null);
                Messages.info("cache_clear__success");
            } catch (IOException e) {
                logger.error(e.getMessage());
                Messages.error("cache_clear__failure");
            }
        } finally {
            clearCacheMode = null;
        }

        return "";
    }

    /**
     * getCurrentThumbnailPage.
     *
     * @return a int.
     */
    public int getCurrentThumbnailPage() {
        // Capture volatile reference once; ViewManager is fully initialized before publication
        ViewManager vm = viewManager;
        return vm != null ? vm.getCurrentThumbnailPage() : 1;
    }

    /**
     * setCurrentThumbnailPage.
     *
     * @param currentThumbnailPage thumbnail grid page number to set
     */
    public void setCurrentThumbnailPage(int currentThumbnailPage) {
        synchronized (this) {
            if (viewManager != null) {
                viewManager.setCurrentThumbnailPage(currentThumbnailPage);
            }
        }
    }

    /**
     * isHasLanguages.
     *
     * @return true if the current record is available in multiple languages, false otherwise
     */
    public boolean isHasLanguages() {
        return recordLanguages != null && !recordLanguages.isEmpty();
    }

    /**
     * Getter for the field <code>lastReceivedIdentifier</code>.
     *
     * @return the persistent identifier of the last successfully loaded record
     */
    public String getLastReceivedIdentifier() {
        return lastReceivedIdentifier;
    }

    /**
     * Setter for the field <code>lastReceivedIdentifier</code>.
     *
     * @param lastReceivedIdentifier the persistent identifier of the last successfully loaded record
     */
    public void setLastReceivedIdentifier(String lastReceivedIdentifier) {
        this.lastReceivedIdentifier = lastReceivedIdentifier;
    }

    /**
     * Getter for the field <code>recordLanguages</code>.
     *
     * @return list of ISO 639-1 language codes available for the current record
     */
    public List<String> getRecordLanguages() {
        return recordLanguages;
    }

    /**
     * Setter for the field <code>recordLanguages</code>.
     *
     * @param recordLanguages list of ISO 639-1 language codes available for the current record
     */
    public void setRecordLanguages(List<String> recordLanguages) {
        this.recordLanguages = recordLanguages;
    }

    /**
     * Getter for the field <code>selectedRecordLanguage</code>.
     *
     * @return the 639_1 code for selectedRecordLanguage
     */
    public String getSelectedRecordLanguage() {
        return Optional.ofNullable(selectedRecordLanguage).map(Language::getIsoCodeOld).orElse(navigationHelper.getLocale().getLanguage());
    }

    /**
     * Setter for the field <code>selectedRecordLanguage</code>.
     *
     * @param selectedRecordLanguageCode ISO 639-1 language code of the language to select for this record
     */
    public void setSelectedRecordLanguage(String selectedRecordLanguageCode) {
        logger.trace("setSelectedRecordLanguage: {}", selectedRecordLanguageCode);
        if (selectedRecordLanguageCode != null) {
            this.selectedRecordLanguage = DataManager.getInstance().getLanguageHelper().getLanguage(selectedRecordLanguageCode);
        }
        if (this.selectedRecordLanguage == null) {
            this.selectedRecordLanguage =
                    DataManager.getInstance().getLanguageHelper().getLanguage(ViewerResourceBundle.getDefaultLocale().getLanguage());
            if (selectedRecordLanguage == null) {
                this.selectedRecordLanguage = DataManager.getInstance().getLanguageHelper().getLanguage("en");
            }
        }

        MetadataBean mdb = BeanUtils.getMetadataBean();
        if (mdb != null && this.selectedRecordLanguage != null) {
            mdb.setSelectedRecordLanguage(this.selectedRecordLanguage.getIsoCodeOld());
        }
    }

    /**
     * Getter for the field <code>selectedRecordLanguage</code>.
     *
     * @return the 639_2B code for selectedRecordLanguage
     */
    public String getSelectedRecordLanguage3() {
        return Optional.ofNullable(selectedRecordLanguage).map(Language::getIsoCode).orElse(navigationHelper.getLocale().getLanguage());
    }

    /**
     * Setter to match getSelectedRecordLanguage3() for URL patterns.
     *
     * @param selectedRecordLanguageCode ISO 639-2/B language code to set
     */
    public void setSelectedRecordLanguage3(String selectedRecordLanguageCode) {
        setSelectedRecordLanguage(selectedRecordLanguageCode);
    }

    /**
     * isAccessPermissionEpub.
     *
     * @return true if the current user has permission to download an EPUB of the current record, false otherwise
     */
    public boolean isAccessPermissionEpub() {
        // Capture volatile reference once; access permission is lazily cached in ViewManager
        // and not affected by the post-publication mutations in update()
        ViewManager vm = viewManager;
        try {
            if ((navigationHelper != null && !isEnabled(EpubDownloadJob.TYPE, navigationHelper.getCurrentPage())) || vm == null
                    || !ocrFolderExists(vm.getPi())) {
                return false;
            }
        } catch (IndexUnreachableException e) {
            logger.error("Error checking EPUB resources: {}", e.getMessage());
            return false;
        }

        // TODO EPUB privilege type
        return vm.isAccessPermissionPdf();
    }

    private boolean ocrFolderExists(String pi) {
        try {
            Path altoFolder = getRecordDataset().getAltoFolderPath();
            return altoFolder != null && Files.isDirectory(altoFolder);
        } catch (RecordNotFoundException e) {
            // viewManager was reset concurrently before the dataset could be loaded; not an error
            logger.debug("Record not available when checking ALTO folder for {}: {}", pi, e.getMessage());
            return false;
        } catch (PresentationException | IndexUnreachableException | IOException e) {
            logger.error("Error finding alto folder for {}", pi, e);
            return false;
        }
    }

    /**
     * isAccessPermissionPdf.
     *
     * @return true if the current user has permission to download a PDF of the current record, false otherwise
     */
    public boolean isAccessPermissionPdf() {
        // Capture volatile reference once; access permission is lazily cached in ViewManager
        // and not affected by the post-publication mutations in update()
        ViewManager vm = viewManager;
        if ((navigationHelper != null && !isEnabled(PdfDownloadJob.TYPE, navigationHelper.getCurrentPage())) || vm == null) {
            return false;
        }

        return vm.isAccessPermissionPdf();
    }

    /**
     * @param downloadType download job type identifier (e.g. PDF or EPUB)
     * @param pageTypeName name of the current page type
     * @return true if download of the given type is enabled for the given page type; false otherwise
     */
    private static boolean isEnabled(String downloadType, String pageTypeName) {
        if (downloadType.equals(EpubDownloadJob.TYPE) && !DataManager.getInstance().getConfiguration().isGeneratePdfInMessageQueue()) {
            return false;
        }
        PageType pageType = PageType.getByName(pageTypeName);
        boolean pdf = PdfDownloadJob.TYPE.equals(downloadType);
        if (pageType != null) {
            switch (pageType) {
                case viewToc:
                    return pdf ? DataManager.getInstance().getConfiguration().isTocPdfEnabled()
                            : DataManager.getInstance().getConfiguration().isTocEpubEnabled();
                case viewMetadata:
                    return pdf ? DataManager.getInstance().getConfiguration().isMetadataPdfEnabled()
                            : DataManager.getInstance().getConfiguration().isMetadataEpubEnabled();
                default:
                    return pdf ? DataManager.getInstance().getConfiguration().isTitlePdfEnabled()
                            : DataManager.getInstance().getConfiguration().isTitleEpubEnabled();
            }
        }

        logger.warn("Unknown page type: {}", pageTypeName);
        return false;
    }

    /**
     * downloadTOCAction.
     *
     * @throws java.io.IOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void downloadTOCAction() throws IOException, PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        try {

            String fileNameRaw = getToc().getTocElements().get(0).getLabel();
            String fileName = fileNameRaw + ".pdf";

            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();
            // Some JSF component library or some Filter might have set some headers in the buffer beforehand.
            // We want to get rid of them, else it may collide.
            ec.responseReset();
            ec.setResponseContentType("application/pdf");
            ec.setResponseHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.HTTP_HEADER_VALUE_ATTACHMENT_FILENAME + fileName + "\"");
            OutputStream os = ec.getResponseOutputStream();
            TocWriter writer = new TocWriter("", fileNameRaw);
            writer.createPdfDocument(os, getToc().getTocElements());
            // Important! Otherwise JSF will attempt to render the response which obviously
            // will fail since it's already written with a file and closed.
            fc.responseComplete();
        } catch (IndexOutOfBoundsException e) {
            logger.error("No toc to generate");
        } catch (WriteTocException e) {
            logger.error("Error writing toc: {}", e.getMessage(), e);
        }
    }

    /**
     * getRelatedItems.
     *
     * <p>TODO Is this still in use?
     *
     * @param identifierField Index field containing related item identifiers
     * @return List of related items as SearchHit objects.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<SearchHit> getRelatedItems(String identifierField)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("getRelatedItems: {}", identifierField);
        if (identifierField == null) {
            return Collections.emptyList();
        }
        if (viewManager == null) {
            return Collections.emptyList();
        }
        String query = getRelatedItemsQueryString(identifierField);
        if (query == null) {
            return Collections.emptyList();
        }

        List<SearchHit> ret = SearchHelper.searchWithAggregation(query, 0, SolrSearchIndex.MAX_HITS, null, null, null, null, null, null, null,
                navigationHelper.getLocale(), false, 0);

        logger.trace("{} related items found", ret.size());
        return ret;
    }

    /**
     * Returns a query string containing all values of the given identifier field.
     *
     * @param identifierField Index field containing related item identifiers
     * @return Query string of the pattern "PI:(a OR b OR c)"
     * @should construct query correctly
     */
    public String getRelatedItemsQueryString(String identifierField) {
        logger.trace("getRelatedItemsQueryString: {}", identifierField);
        List<String> relatedItemIdentifiers = viewManager.getTopStructElement().getMetadataValues(identifierField);
        if (relatedItemIdentifiers.isEmpty()) {
            return null;
        }

        StringBuilder sbQuery = new StringBuilder(SolrConstants.PI).append(":(");
        int initLength = sbQuery.length();
        for (String identifier : relatedItemIdentifiers) {
            if (sbQuery.length() > initLength) {
                sbQuery.append(" OR ");
            }
            sbQuery.append(identifier);
        }
        sbQuery.append(')');

        return sbQuery.toString();
    }

    /**
     * Returns a string that contains previous and/or next url <link> elements.
     *
     * @return string containing previous and/or next url <link> elements
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @should return empty string if no record loaded
     * @should return empty string if navigationHelper null
     * @should return non blank link tags when record is loaded
     * @should generate canonical without page number for first page
     * @should generate canonical with page number for non first page
     */
    public String getRelativeUrlTags() throws IndexUnreachableException, DAOException, PresentationException {
        // Single volatile read to avoid TOCTOU race with reset()/update() on concurrent threads.
        // Using a local reference ensures consistency for all subsequent field accesses even if
        // another thread calls reset() and sets this.viewManager to null mid-execution.
        ViewManager vm = this.viewManager;
        if (vm == null || navigationHelper == null) {
            return "";
        }
        if (logger.isTraceEnabled()) {
            logger.trace("current view: {}", navigationHelper.getCurrentView());
        }

        String linkCanonical = "\n<link rel=\"canonical\" href=\"";
        String linkAlternate = "\n<link rel=\"alternate\" href=\"";
        String linkEnd = "\" />";

        PageType currentPageType = PageType.getByName(navigationHelper.getCurrentView());
        PageType defaultPageTypeForRecord =
                PageType.determinePageType(vm.getTopStructElement().getDocStructType(), vm.getMimeType(),
                        isAnchor() || isGroup(), vm.isHasPages(), false);

        StringBuilder sb = new StringBuilder();

        // Add resolver links if current view matches resolved view for this record
        if (defaultPageTypeForRecord != null && defaultPageTypeForRecord.equals(currentPageType)) {
            if (vm.getCurrentPage() != null) {
                // URN resolver URL (alternate)
                if (StringUtils.isNotEmpty(vm.getCurrentPage().getUrn())) {
                    String urnResolverUrl = DataManager.getInstance().getConfiguration().getUrnResolverUrl() + vm.getCurrentPage().getUrn();
                    sb.append(linkAlternate).append(urnResolverUrl).append(linkEnd);
                }
                // PI resolver URL (alternate): getRepresentativePage() may trigger a single lazy Solr
                // load on the first call; subsequent calls return the cached PhysicalElement. A benign
                // double-init race between two threads is acceptable (idempotent, same result).
                if (vm.getCurrentPage().equals(vm.getRepresentativePage())) {
                    String piResolverUrl = navigationHelper.getApplicationUrl() + "piresolver?id=" + vm.getPi();
                    sb.append(linkAlternate).append(piResolverUrl).append(linkEnd);
                }
            }
        }
        if (currentPageType != null && StringUtils.isNotEmpty(currentPageType.getName())) {
            logger.trace("page type: {}", currentPageType);
            // logger.trace("current url: {}", navigationHelper.getCurrentUrl()); //NOSONAR Debug

            int page = vm.getCurrentImageOrder();
            String urlRoot = navigationHelper.getApplicationUrl() + currentPageType.getName() + "/" + vm.getPi() + "/";
            String urlRootExplicit = navigationHelper.getApplicationUrl() + "!" + currentPageType.getName() + "/" + vm.getPi() + "/";
            switch (currentPageType) {
                case viewFullscreen:
                case viewImage:
                case viewMetadata:
                case viewObject:
                    if (page == 1) {
                        // Page 1: URL without page canonical
                        sb.append(linkCanonical).append(urlRoot).append(linkEnd);
                        sb.append(linkAlternate).append(urlRoot).append(page).append('/').append(linkEnd);
                        sb.append(linkAlternate).append(urlRootExplicit).append(page).append('/').append(linkEnd);
                    } else {
                        // Page 2+: URL with page canonical
                        sb.append(linkCanonical).append(urlRoot).append(page).append('/').append("\" />");
                        sb.append(linkAlternate).append(urlRootExplicit).append(page).append('/').append(linkEnd);
                    }
                    if (StringUtils.isNotEmpty(getLogid())) {
                        sb.append(linkAlternate).append(urlRoot).append(page).append('/').append(getLogid()).append('/').append(linkEnd);
                        sb.append(linkAlternate).append(urlRootExplicit).append(page).append('/').append(getLogid()).append('/').append(linkEnd);
                    }
                    break;
                case viewFulltext:
                    if (page == 1) {
                        sb.append(linkCanonical).append(urlRoot).append(linkEnd);
                        sb.append(linkAlternate).append(urlRoot).append(page).append('/').append(linkEnd);
                        sb.append(linkAlternate).append(urlRootExplicit).append(page).append('/').append(linkEnd);
                    } else {
                        sb.append(linkCanonical).append(urlRoot).append(page).append('/').append(linkEnd);
                        sb.append(linkAlternate).append(urlRootExplicit).append(page).append('/').append(linkEnd);
                    }
                    if (StringUtils.isNotEmpty(getSelectedRecordLanguage3())) {
                        sb.append(linkAlternate)
                                .append(urlRoot)
                                .append(page)
                                .append('/')
                                .append(getSelectedRecordLanguage3())
                                .append('/')
                                .append(linkEnd);
                        sb.append(linkAlternate)
                                .append(urlRootExplicit)
                                .append(page)
                                .append('/')
                                .append(getSelectedRecordLanguage3())
                                .append('/')
                                .append(linkEnd);
                    }
                    break;
                case viewThumbs:
                    sb.append(linkCanonical).append(urlRoot).append(getCurrentThumbnailPage()).append('/').append(linkEnd);
                    break;
                case viewToc:
                    sb.append(linkCanonical).append(urlRoot).append(tocCurrentPage).append('/').append(linkEnd);
                    if (StringUtils.isNotEmpty(getLogid())) {
                        sb.append(linkAlternate).append(urlRoot).append(tocCurrentPage).append('/').append(getLogid()).append('/').append(linkEnd);
                        sb.append(linkAlternate)
                                .append(urlRootExplicit)
                                .append(tocCurrentPage)
                                .append('/')
                                .append(getLogid())
                                .append('/')
                                .append(linkEnd);
                    }
                    break;
                default:
                    break;
            }
        }

        // Skip prev/next links for non-paginated views
        if (PageType.viewMetadata.equals(currentPageType) || PageType.viewToc.equals(currentPageType)) {
            return sb.toString();
        }

        // Add next/prev links
        String currentUrl = getPageUrl(imageToShow);
        String prevUrl = getPreviousPageUrl();
        String nextUrl = getNextPageUrl();
        if (StringUtils.isNotEmpty(nextUrl) && !nextUrl.equals(currentUrl)) {
            sb.append("\n<link rel=\"next\" href=\"").append(nextUrl).append(linkEnd);
        }
        if (StringUtils.isNotEmpty(prevUrl) && !prevUrl.equals(currentUrl)) {
            sb.append("\n<link rel=\"prev\" href=\"").append(prevUrl).append(linkEnd);
        }

        return sb.toString();
    }

    /**
     * resets the access rights for user comments and pdf download stored in {@link io.goobi.viewer.model.viewer.ViewManager}. After reset, the access
     * rights will be evaluated again on being called
     */
    public void resetAccess() {
        if (getViewManager() != null) {
            getViewManager().resetAccessPermissionPdf();
            getViewManager().resetAllowUserComments();
        }
    }

    /**
     * Getter for the field <code>deleteRecordKeepTrace</code>.
     *
     * @return true to keep a deletion trace entry when deleting the record; false to remove completely; null if not yet set
     */
    public Boolean getDeleteRecordKeepTrace() {
        return deleteRecordKeepTrace;
    }

    /**
     * Setter for the field <code>deleteRecordKeepTrace</code>.
     *
     * @param deleteRecordKeepTrace true to keep a deletion trace entry when deleting the record; false to remove completely
     */
    public void setDeleteRecordKeepTrace(Boolean deleteRecordKeepTrace) {
        this.deleteRecordKeepTrace = deleteRecordKeepTrace;
    }

    /**
     * Getter for the field <code>clearCacheMode</code>.
     *
     * @return cache clearing mode string indicating which caches to clear, or null if not set
     */
    public String getClearCacheMode() {
        return clearCacheMode;
    }

    /**
     * Setter for the field <code>clearCacheMode</code>.
     *
     * @param clearCacheMode cache clearing mode string indicating which caches to clear
     */
    public void setClearCacheMode(String clearCacheMode) {
        logger.trace("setClearCacheMode: {}", clearCacheMode);
        this.clearCacheMode = clearCacheMode;
    }

    /**
     * Get a CMSSidebarElement with a map containing all GeoMarkers for the current PI. The widget is stored in the bean, but refreshed each time the
     * PI changes
     *
     * @return {@link io.goobi.viewer.model.maps.GeoMap}
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @should return GeoMap for loaded record
     * @should return non null geo map when no record loaded
     */
    public GeoMap getGeoMap() throws DAOException, IndexUnreachableException {
        // No synchronization needed: getRecordGeoMap() reads/writes the volatile geoMaps
        // reference; a benign double-init race is acceptable (idempotent construction).
        return getRecordGeoMap().getGeoMap();
    }

    /**
     * getRecordGeoMap.
     *
     * @return {@link io.goobi.viewer.model.maps.RecordGeoMap}
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @should cache result for same PI
     */
    public RecordGeoMap getRecordGeoMap() throws DAOException, IndexUnreachableException {
        // getPersistentIdentifier() and getTopDocument() each capture viewManager independently.
        // A reset() between the two reads is tolerated: worst case we cache an empty RecordGeoMap
        // or skip caching entirely — no invariant is violated (see class-level thread-safety note).
        // A concurrent double-init race (two threads both see null) is benign: RecordGeoMap
        // construction is pure (configuration only, no external state), the last writer wins,
        // and the unused instance becomes GC-eligible.
        RecordGeoMap map = this.geoMaps.get(getPersistentIdentifier());
        if (map == null) {
            StructElement topDocument = getTopDocument();
            if (topDocument == null) {
                return new RecordGeoMap();
            }
            map = new RecordGeoMap(topDocument);
            this.geoMaps = Collections.singletonMap(getPersistentIdentifier(), map);
        }
        return map;
    }

    /**
     * generateGeoMap.
     *
     * @param pi persistent identifier of the record to map
     * @return {@link io.goobi.viewer.model.maps.GeoMap}
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public GeoMap generateGeoMap(String pi) throws PresentationException, DAOException {
        try {
            if ("-".equals(pi)) {
                return null;
            }

            GeoMap map = new GeoMap();
            map.setId(Long.MAX_VALUE);
            map.setShowPopover(true);

            ManualFeatureSet featureSet = new ManualFeatureSet();
            featureSet.setMarker("default");
            map.addFeatureSet(featureSet);

            String mainDocQuery = String.format("PI:%s", pi);
            List<String> mainDocFields = PrettyUrlTools.getSolrFieldsToDeterminePageType();
            SolrDocument mainDoc = DataManager.getInstance().getSearchIndex().getFirstDoc(mainDocQuery, mainDocFields);
            PageType pageType = PrettyUrlTools.getPreferredPageType(mainDoc);

            boolean addMetadataFeatures = DataManager.getInstance().getConfiguration().includeCoordinateFieldsFromMetadataDocs();
            String docTypeFilter = " +" + SolrConstants.DOCTYPE + ":DOCSTRCT";
            if (addMetadataFeatures) {
                docTypeFilter = " +(" + SolrConstants.DOCTYPE + ":DOCSTRCT " + SolrConstants.DOCTYPE + ":METADATA)";
            }

            String subDocQuery = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + docTypeFilter;
            List<String> coordinateFields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();
            List<String> subDocFields = new ArrayList<>();
            subDocFields.add(SolrConstants.LABEL);
            subDocFields.add(SolrConstants.PI_TOPSTRUCT);
            subDocFields.add(SolrConstants.THUMBPAGENO);
            subDocFields.add(SolrConstants.LOGID);
            subDocFields.add(SolrConstants.ISWORK);
            subDocFields.add(SolrConstants.DOCTYPE);
            subDocFields.add("MD_VALUE");
            subDocFields.addAll(coordinateFields);

            Collection<GeoMapFeature> features = new ArrayList<>();
            CoordinateReaderProvider coordinateReaderProvider = new CoordinateReaderProvider();
            List<DisplayUserGeneratedContent> annos = DataManager.getInstance()
                    .getDao()
                    .getAnnotationsForWork(pi)
                    .stream()
                    .filter(a -> PublicationStatus.PUBLISHED.equals(a.getPublicationStatus()))
                    .filter(a -> StringUtils.isNotBlank(a.getBody()))
                    .map(DisplayUserGeneratedContent::new)
                    .filter(a -> ContentType.GEOLOCATION.equals(a.getType()))
                    .filter(a -> ContentBean.isAccessible(a, BeanUtils.getRequest()))
                    .toList();
            for (DisplayUserGeneratedContent anno : annos) {
                if (anno.getAnnotationBody() instanceof TypedResource tr) {
                    Geometry geometry = coordinateReaderProvider.getReader(tr.asJson()).read(tr.asJson());
                    GeoMapFeature feature = new GeoMapFeature(geometry);
                    features.add(feature);
                }
            }

            SolrDocumentList subDocs = DataManager.getInstance().getSearchIndex().getDocs(subDocQuery, subDocFields);
            if (subDocs != null) {
                for (SolrDocument solrDocument : subDocs) {
                    List<GeoMapFeature> docFeatures = new ArrayList<>();
                    for (String coordinateField : coordinateFields) {
                        String docType = solrDocument.getFieldValue(SolrConstants.DOCTYPE).toString();
                        String labelField = "METADATA".equals(docType) ? "MD_VALUE" : SolrConstants.LABEL;
                        docFeatures.addAll(new GeoCoordinateConverter().getGeojsonPoints(solrDocument, null, coordinateField, labelField));
                    }
                    if (!solrDocument.containsKey(SolrConstants.ISWORK) && solrDocument.getFieldValue(SolrConstants.DOCTYPE).equals("DOCSTRCT")) {
                        docFeatures.forEach(f -> f.setLink(PrettyUrlTools.getRecordUrl(solrDocument, pageType)));
                    } else {
                        docFeatures.forEach(f -> f.setLink(null));
                    }
                    docFeatures.forEach(f -> f.setDocumentId((String) solrDocument.getFieldValue(SolrConstants.LOGID)));
                    features.addAll(docFeatures);
                }
            }
            //remove dubplicates
            features = features.stream().distinct().collect(Collectors.toList());
            if (!features.isEmpty()) {
                featureSet.setFeatures(features.stream().map(f -> f.getJsonObject().toString()).collect(Collectors.toList()));
            }
            return map;
        } catch (IndexUnreachableException e) {
            logger.error("Unable to load geomap", e);
            return null;
        }
    }

    /**
     * toggleDownloadImageModal.
     */
    public void toggleDownloadImageModal() {
        downloadImageModalVisible = !downloadImageModalVisible;
    }

    /**
     * isDownloadImageModalVisible.
     *
     * @return true if the download image modal dialog is currently visible; false otherwise
     */
    public boolean isDownloadImageModalVisible() {
        return downloadImageModalVisible;
    }

    /**
     * getSelectedDownloadOption.
     *
     * @return Selected {@link io.goobi.viewer.model.job.download.DownloadOption}
     */
    public DownloadOption getSelectedDownloadOption() {
        if (selectedDownloadOptionLabel == null) {
            return null;
        }

        return DownloadOption.getByLabel(selectedDownloadOptionLabel);
    }

    public DownloadOption getSelectedDownloadOptionOrDefault() throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        return Optional.ofNullable(getSelectedDownloadOption())
                .orElse(getViewManager().getDownloadOptionsForPage(getViewManager().getCurrentPage())
                        .stream()
                        .findFirst()
                        .orElse(new DownloadOption()));
    }

    /**
     * Getter for the field <code>selectedDownloadOptionLabel</code>.
     *
     * @return the label of the download option currently selected by the user, or null if none selected
     */
    public String getSelectedDownloadOptionLabel() {
        return selectedDownloadOptionLabel;
    }

    /**
     * Setter for the field <code>selectedDownloadOptionLabel</code>.
     *
     * @param selectedDownloadOptionLabel label of the download option selected by the user
     */
    public void setSelectedDownloadOptionLabel(String selectedDownloadOptionLabel) {
        logger.trace("setSelectedDownloadOption: {}", selectedDownloadOptionLabel != null ? selectedDownloadOptionLabel : null);
        this.selectedDownloadOptionLabel = selectedDownloadOptionLabel;
    }

    /**
     * setDownloadOptionLabelFromRequestParameter.
     */
    public void setDownloadOptionLabelFromRequestParameter() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        String value = params.get("optionvalue");
        if (StringUtils.isNotBlank(value)) {
            setSelectedDownloadOptionLabel(value);
        }

    }

    /**
     * Augments the setter <code>ViewManager.setDoublePageMode(boolean)</code> with URL modifications to reflect the mode.
     *
     * @param doublePageMode The doublePageMode to set
     * @return empty string
     * @should set imageToShow if value changes
     */
    public String setDoublePageModeAction(boolean doublePageMode) {
        if (viewManager == null) {
            return "";
        }
        try {
            // Adapt URL page range when switching between single and double page modes
            if (viewManager.isDoublePageMode() != doublePageMode) {
                if (doublePageMode && !viewManager.getCurrentPage().isDoubleImage()) {
                    Optional<PhysicalElement> currentLeftPage = viewManager.getCurrentLeftPage();
                    Optional<PhysicalElement> currentRightPage = viewManager.getCurrentRightPage();
                    if (currentLeftPage.isPresent() && currentRightPage.isPresent()) {
                        imageToShow = currentLeftPage.get().getOrder() + "-" + currentRightPage.get().getOrder();
                    } else if (currentLeftPage.isPresent()) {
                        imageToShow = currentLeftPage.get().getOrder() + "-" + currentLeftPage.get().getOrder();
                    } else if (currentRightPage.isPresent()) {
                        imageToShow = currentRightPage.get().getOrder() + "-" + currentRightPage.get().getOrder();
                    }
                } else if (doublePageMode) {
                    imageToShow = String.valueOf(viewManager.getCurrentPage().getOrder() + "-" + viewManager.getCurrentPage().getOrder());
                } else {
                    imageToShow = String.valueOf(viewManager.getCurrentPage().getOrder());
                }
            }
        } finally {
            viewManager.setDoublePageMode(doublePageMode);
        }

        // When not using PrettyContext, the updated URL will always be a click behind
        if (PrettyContext.getCurrentInstance() != null && PrettyContext.getCurrentInstance().getCurrentMapping() != null) {
            return StringConstants.PREFIX_PRETTY + PrettyContext.getCurrentInstance().getCurrentMapping().getId();
        }

        return "";
    }

    /**
     * Indicates whether user comments are allowed for the current record based on several criteria.
     *
     * @return true if user comments are allowed for the current record, false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException
     * @should return false when no record is loaded
     * @should return false when comments are disabled globally
     * @should not throw NPE if allowUserComments is reset concurrently
     * @should return false when no record loaded
     * @should return false when comments disabled globally
     * @should not throw NPE if allow user comments reset concurrently
     */
    public boolean isAllowUserComments() throws DAOException {
        // Single volatile read to avoid TOCTOU race with reset()/update() on concurrent threads.
        // DAO and Solr I/O now run without holding the ADB monitor.
        ViewManager vm = this.viewManager;
        if (vm == null) {
            return false;
        }

        CommentGroup commentGroupAll = DataManager.getInstance().getDao().getCommentGroupUnfiltered();
        if (commentGroupAll == null) {
            logger.warn("Comment view for all comments not found in the DB, please insert.");
            return false;
        }
        if (!commentGroupAll.isEnabled()) {
            logger.trace("User comments disabled globally.");
            vm.setAllowUserComments(false);
            return false;
        }

        if (vm.isAllowUserComments() == null) {
            // A concurrent double-init race (two threads both see null) is benign: same Solr
            // query, same result, idempotent write to volatile Boolean in ViewManager.
            try {
                if (StringUtils.isNotEmpty(commentGroupAll.getSolrQuery()) && DataManager.getInstance()
                        .getSearchIndex()
                        .getHitCount(new StringBuilder("+").append(SolrConstants.PI)
                                .append(':')
                                .append(vm.getPi())
                                .append(" +(")
                                .append(commentGroupAll.getSolrQuery())
                                .append(')')
                                .toString()) == 0) {
                    vm.setAllowUserComments(false);
                    logger.trace("User comments are not allowed for this record.");
                } else {
                    vm.setAllowUserComments(true);
                }
            } catch (IndexUnreachableException e) {
                logger.debug("IndexUnreachableException thrown here: {}", e.getMessage());
                return false;
            } catch (PresentationException e) {
                logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
                return false;
            }
        }

        // Null-safe unboxing: resetAllowUserComments() on a concurrent thread can set the volatile
        // Boolean back to null between our set above and this read. Boolean.TRUE.equals() treats
        // null as false — the correct conservative default (next call will re-evaluate).
        return Boolean.TRUE.equals(vm.isAllowUserComments());
    }

    /**
     * Checks if the current page should initialize a WebSocket.
     *
     * @return true if a document is loaded and it contains the field {@link io.goobi.viewer.solr.SolrConstants#ACCESSCONDITION_CONCURRENTUSE}
     */
    public boolean isRequiresWebSocket() {
        if (viewManager != null && viewManager.getTopStructElement() != null && viewManager.getTopStructElement().getMetadataFields() != null) {
            return viewManager.getTopStructElement().getMetadataFields().containsKey(SolrConstants.ACCESSCONDITION_CONCURRENTUSE);
        }

        return false;
    }

    /**
     * getGeomapFilters.
     *
     * @return a list of Solr field names used as geo map filter options, formatted as quoted strings
     */
    public List<String> getGeomapFilters() {
        return List.of("MD_METADATATYPE", "MD_GENRE").stream().map(s -> "'" + s + "'").collect(Collectors.toList());
    }

    public void updatePageNavigation(PageType pageType) {
        if (this.viewManager != null) {
            this.viewManager.setPageNavigation(calculateCurrentPageNavigation(pageType));
        }
    }

    protected PageNavigation calculateCurrentPageNavigation(PageType pageType) {
        try {
            ViewAttributes viewAttributes = new ViewAttributes(viewManager, pageType);
            PageNavigation defaultPageNavigation = getDefaultPageNavigation(viewAttributes);
            PageNavigation currentPageNavigation =
                    Optional.ofNullable(this.viewManager).map(ViewManager::getPageNavigation).orElse(PageNavigation.SINGLE);
            if (currentPageNavigation == defaultPageNavigation) {
                return currentPageNavigation;
            } else if (defaultPageNavigation == PageNavigation.SEQUENCE) {
                return defaultPageNavigation;
            } else if (currentPageNavigation == PageNavigation.SINGLE) {
                return currentPageNavigation;
            } else if (currentPageNavigation == PageNavigation.DOUBLE
                    && DataManager.getInstance().getConfiguration().isDoublePageNavigationEnabled(viewAttributes)) {
                return currentPageNavigation;
            } else {
                return defaultPageNavigation;
            }

        } catch (ViewerConfigurationException | NullPointerException | IllegalArgumentException e) {
            logger.error("Failed to set view mode: {}", e.toString());
            return PageNavigation.SINGLE;
        }
    }

    protected PageNavigation getDefaultPageNavigation(ViewAttributes viewAttributes) {
        try {
            if (DataManager.getInstance().getConfiguration().isSequencePageNavigationEnabled(viewAttributes)) {
                return PageNavigation.SEQUENCE;
            } else if (DataManager.getInstance().getConfiguration().isDoublePageNavigationDefault(viewAttributes)) {
                return PageNavigation.DOUBLE;
            } else {
                return PageNavigation.SINGLE;
            }
        } catch (ViewerConfigurationException e) {
            logger.error("Error reading default page navigation from config", e);
            return PageNavigation.SINGLE;
        }
    }

    private Dataset getRecordDataset() throws PresentationException, IndexUnreachableException, RecordNotFoundException, IOException {
        // Double-checked locking: volatile field guarantees safe publication without holding the
        // ADB monitor during Solr I/O. PI validation prevents returning a stale dataset if
        // reset() fires between the cache-miss check and the Solr call.
        Dataset cached = this.recordDataset;
        ViewManager vm = this.viewManager;
        if (vm == null) {
            throw new RecordNotFoundException("No active record");
        }
        String currentPi = StringTools.cleanUserGeneratedData(vm.getPi());
        if (cached != null && currentPi.equals(cached.getPi())) {
            return cached;
        }
        // Solr I/O outside any lock — benign double-init race is acceptable (idempotent query)
        Dataset fresh = new ProcessDataResolver().getDataset(currentPi);
        synchronized (this) {
            // Only publish if the PI hasn't changed (i.e. reset() hasn't loaded a different record)
            if (this.recordDataset == null || !currentPi.equals(this.recordDataset.getPi())) {
                this.recordDataset = fresh;
            }
        }
        return this.recordDataset;
    }

}