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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.language.Language;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordDeletedException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.faces.validators.PIValidator;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.download.DownloadJob;
import io.goobi.viewer.model.download.EPUBDownloadJob;
import io.goobi.viewer.model.download.PDFDownloadJob;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.search.BrowseElement;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.toc.TOC;
import io.goobi.viewer.model.toc.TOCElement;
import io.goobi.viewer.model.toc.export.pdf.TocWriter;
import io.goobi.viewer.model.toc.export.pdf.WriteTocException;
import io.goobi.viewer.model.viewer.LabeledLink;
import io.goobi.viewer.model.viewer.PageOrientation;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.model.viewer.pageloader.EagerPageLoader;
import io.goobi.viewer.model.viewer.pageloader.LeanPageLoader;
import io.goobi.viewer.modules.IModule;

/**
 * This bean opens the requested record and provides all data relevant to this record.
 */
@Named
@SessionScoped
public class ActiveDocumentBean implements Serializable {

    private static final long serialVersionUID = -8686943862186336894L;

    private static final Logger logger = LoggerFactory.getLogger(ActiveDocumentBean.class);

    private static int imageContainerWidth = 600;

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private CmsBean cmsBean;
    @Inject
    private SearchBean searchBean;
    @Inject
    private BookshelfBean bookshelfBean;
    @Inject
    private ImageDeliveryBean imageDelivery;

    /** URL parameter 'action'. */
    private String action = "";
    /** URL parameter 'imageToShow'. */
    private int imageToShow = 1;
    /** URL parameter 'logid'. */
    private String logid = "";
    /** URL parameter 'tocCurrentPage'. */
    private int tocCurrentPage = 1;

    private ViewManager viewManager;
    private boolean anchor = false;
    private boolean volume = false;
    private boolean group = false;
    protected long topDocumentIddoc = 0;

    /** Table of contents object. */
    private TOC toc;

    /** Metadata displayed in title.xhtml */
    private List<Metadata> titleBarMetadata = new ArrayList<>();

    // TODO move to SearchBean
    private BrowseElement prevHit;
    private BrowseElement nextHit;

    /** This persists the last value given to setPersistentIdentifier() and is used for handling a RecordNotFoundException. */
    private String lastReceivedIdentifier;
    /** Available languages for this record. */
    private List<String> recordLanguages;
    /** Currently selected language for multilingual records. */
    private String selectedRecordLanguage;

    private Boolean deleteRecordKeepTrace;

    /** Empty constructor. */
    public ActiveDocumentBean() {
        // the emptiness inside
    }

    /**
     * Required setter for ManagedProperty injection
     * 
     * @param navigationHelper the navigationHelper to set
     */
    public void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    /**
     * Required setter for ManagedProperty injection
     * 
     * @param cmsBean the cmsBean to set
     */
    public void setCmsBean(CmsBean cmsBean) {
        this.cmsBean = cmsBean;
    }

    /**
     * Required setter for ManagedProperty injection
     * 
     * @param searchBean the searchBean to set
     */
    public void setSearchBean(SearchBean searchBean) {
        this.searchBean = searchBean;
    }

    /**
     * Required setter for ManagedProperty injection
     * 
     * @param bookshelfBean the bookshelfBean to set
     */
    public void setBookshelfBean(BookshelfBean bookshelfBean) {
        this.bookshelfBean = bookshelfBean;
    }

    /**
     * TODO This can cause NPEs if called while update() is running.
     */
    public void reset() {
        synchronized (this) {
            logger.trace("reset (thread {})", Thread.currentThread().getId());
            viewManager = null;
            topDocumentIddoc = 0;
            toc = null;
            titleBarMetadata.clear();
            logid = "";
            action = "";
            prevHit = null;
            nextHit = null;
            group = false;

            // Any cleanup modules need to do when a record is unloaded
            for (IModule module : DataManager.getInstance().getModules()) {
                module.augmentResetRecord();
            }
        }
    }

    /**
     * Do not call from ActiveDocumentBean.update()!
     *
     * @return
     */
    public ViewManager getViewManager() {
        if (viewManager == null) {
            try {
                update();
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            } catch (RecordNotFoundException | RecordDeletedException | IndexUnreachableException | DAOException | ViewerConfigurationException e) {
            }
        }

        return viewManager;
    }

    /**
     * Loads the record with the IDDOC set in <code>currentElementIddoc</code>.
     *
     * @throws PresentationException
     * @throws RecordNotFoundException
     * @throws RecordDeletedException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @should create ViewManager correctly
     * @should update ViewManager correctly if LOGID has changed
     * @should not override topDocumentIddoc if LOGID has changed
     */
    public void update() throws PresentationException, IndexUnreachableException, RecordNotFoundException, RecordDeletedException, DAOException,
            ViewerConfigurationException {
        synchronized (this) {
            if (topDocumentIddoc == 0) {
                throw new RecordNotFoundException(lastReceivedIdentifier);
            }
            logger.debug("update(): (IDDOC {} ; page {} ; thread {})", topDocumentIddoc, imageToShow, Thread.currentThread().getId());
            prevHit = null;
            nextHit = null;
            boolean doublePageMode = false;
            titleBarMetadata.clear();

            if (viewManager != null && viewManager.getCurrentDocument() != null) {
                doublePageMode = viewManager.isDoublePageMode();
            }

            // Do these steps only if a new document has been loaded
            boolean mayChangeHitIndex = false;
            if (viewManager == null || viewManager.getTopDocument() == null || viewManager.getTopDocumentIddoc() != topDocumentIddoc) {
                toc = null;
                anchor = false;
                volume = false;
                group = false;

                // Change current hit index only if loading a new record
                if (searchBean != null && searchBean.getCurrentSearch() != null) {
                    searchBean.increaseCurrentHitIndex();
                    mayChangeHitIndex = true;
                }

                StructElement topDocument = new StructElement(topDocumentIddoc);

                // Exit here if record is not found or has been deleted
                if (!topDocument.isExists()) {
                    logger.info("IDDOC for the current record '{}' ({}) no longer seems to exist, attempting to retrieve an updated IDDOC...",
                            topDocument.getPi(), topDocumentIddoc);
                    topDocumentIddoc = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(topDocument.getPi());
                    if (topDocumentIddoc == 0) {
                        logger.warn("New IDDOC for the current record '{}' could not be found. Perhaps this record has been deleted?",
                                viewManager.getPi());
                        reset();
                        throw new RecordNotFoundException(lastReceivedIdentifier);
                    }
                } else if (topDocument.isDeleted()) {
                    logger.debug("Record '{}' is deleted and only available as a trace document.", topDocument.getPi());
                    reset();
                    throw new RecordDeletedException(topDocument.getPi());
                }

                // Do not open records who may not be listed for the current user
                List<String> requiredAccessConditions = topDocument.getMetadataValues(SolrConstants.ACCESSCONDITION);
                if (requiredAccessConditions != null && !requiredAccessConditions.isEmpty()) {
                    boolean access = AccessConditionUtils.checkAccessPermission(new HashSet<>(requiredAccessConditions), IPrivilegeHolder.PRIV_LIST,
                            new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(':').append(topDocument.getPi()).toString(),
                            (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
                    if (!access) {
                        logger.debug("User may not open {}", topDocument.getPi());
                        throw new RecordNotFoundException(lastReceivedIdentifier);
                    }

                }

                int numPages = topDocument.getNumPages();
                if (numPages < DataManager.getInstance().getConfiguration().getPageLoaderThreshold()) {
                    viewManager = new ViewManager(topDocument, new EagerPageLoader(topDocument), topDocumentIddoc, logid,
                            topDocument.getMetadataValue(SolrConstants.MIMETYPE), imageDelivery);
                } else {
                    logger.debug("Record has {} pages, using a lean page loader to limit memory usage.", numPages);
                    viewManager = new ViewManager(topDocument, new LeanPageLoader(topDocument, numPages), topDocumentIddoc, logid,
                            topDocument.getMetadataValue(SolrConstants.MIMETYPE), imageDelivery);
                }

                toc = createTOC();
            }

            // If LOGID is set, update the current element
            if (StringUtils.isNotEmpty(logid) && viewManager != null && !logid.equals(viewManager.getLogId())) {
                // TODO set new values instead of re-creating ViewManager, perhaps
                logger.debug("Find doc by LOGID: {}", logid);
                new StructElement(topDocumentIddoc);
                StringBuilder sbQuery = new StringBuilder();
                sbQuery.append(SolrConstants.LOGID)
                        .append(':')
                        .append(logid)
                        .append(" AND ")
                        .append(SolrConstants.PI_TOPSTRUCT)
                        .append(':')
                        .append(viewManager.getPi());
                SolrDocumentList docList = DataManager.getInstance()
                        .getSearchIndex()
                        .search(sbQuery.toString(), 1, null, Collections.singletonList(SolrConstants.IDDOC));
                long subElementIddoc = 0;
                if (!docList.isEmpty()) {
                    subElementIddoc = Long.valueOf((String) docList.get(0).getFieldValue(SolrConstants.IDDOC));
                    // Re-initialize ViewManager with the new current element
                    PageOrientation firstPageOrientation = viewManager.getFirstPageOrientation();
                    viewManager = new ViewManager(viewManager.getTopDocument(), viewManager.getPageLoader(), subElementIddoc, logid,
                            viewManager.getMainMimeType(), imageDelivery);
                    viewManager.setFirstPageOrientation(firstPageOrientation);
                } else {
                    logger.warn("{} not found for LOGID '{}'.", SolrConstants.IDDOC, logid);
                }
            }

            if (viewManager != null && viewManager.getCurrentDocument() != null) {
                viewManager.setDoublePageMode(doublePageMode);
                StructElement structElement = viewManager.getCurrentDocument();
                if (!structElement.isExists()) {
                    logger.trace("StructElement {} is not marked as existing.", structElement.getLuceneId());
                    throw new RecordNotFoundException(lastReceivedIdentifier);
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

                // Populate title bar metadata
                StructElement topSe = viewManager.getCurrentDocument().getTopStruct();
                // logger.debug("topSe: " + topSe.getId());
                for (Metadata md : DataManager.getInstance().getConfiguration().getTitleBarMetadata()) {
                    md.populate(topSe, BeanUtils.getLocale());
                    if (!md.isBlank()) {
                        titleBarMetadata.add(md);
                    }
                }

                // When not aggregating hits, a new page will also be a new search hit in the list
                if (imageToShow != viewManager.getCurrentImageNo() && !DataManager.getInstance().getConfiguration().isAggregateHits()) {
                    mayChangeHitIndex = true;
                }
                viewManager.setCurrentImageNo(imageToShow);
                viewManager.updateDropdownSelected();

                // Search hit navigation
                if (searchBean != null && searchBean.getCurrentSearch() != null) {
                    if (searchBean.getCurrentHitIndex() < 0) {
                        // Determine the index of this element in the search result list. Must be done after re-initializing ViewManager so that the PI is correct!
                        searchBean.findCurrentHitIndex(getPersistentIdentifier(), imageToShow,
                                DataManager.getInstance().getConfiguration().isAggregateHits());
                    } else if (mayChangeHitIndex) {
                        // Modify the current hit index
                        searchBean.increaseCurrentHitIndex();
                    } else if (searchBean.getHitIndexOperand() != 0) {
                        // Reset hit index operand (should only be necessary if the URL was called twice, but the current hit has not changed
                        // logger.trace("Hit index modifier operand is {}, resetting...", searchBean.getHitIndexOperand());
                        searchBean.setHitIndexOperand(0);
                    }
                }
            } else {
                logger.debug("ViewManager is null or ViewManager.currentDocument is null.");
                throw new RecordNotFoundException(lastReceivedIdentifier);
            }

            // Metadata language versions
            recordLanguages = viewManager.getTopDocument().getMetadataValues(SolrConstants.LANGUAGE);
            // If the record has metadata language versions, pre-select the current locale as the record language
            //            if (StringUtils.isBlank(selectedRecordLanguage) && !recordLanguages.isEmpty()) {
            if (StringUtils.isBlank(selectedRecordLanguage) && navigationHelper != null) {
                selectedRecordLanguage = navigationHelper.getLocaleString();
            }

            // Prepare a new bookshelf item
            if (bookshelfBean != null) {
                bookshelfBean.prepareItemForBookshelf();
                if (bookshelfBean.getCurrentBookshelfItem() == null || !viewManager.getPi().equals(bookshelfBean.getCurrentBookshelfItem().getPi())) {
                    bookshelfBean.prepareItemForBookshelf();
                }
            }
        }
    }

    /**
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    private TOC createTOC() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        TOC toc = new TOC();
        synchronized (toc) {
            if (viewManager != null) {
                toc.generate(viewManager.getTopDocument(), viewManager.isListAllVolumesInTOC(), viewManager.getMainMimeType(), tocCurrentPage);
                // The TOC object will correct values that are too high, so update the local value, if necessary
                if (toc.getCurrentPage() != this.tocCurrentPage) {
                    this.tocCurrentPage = toc.getCurrentPage();
                }
            }
        }
        return toc;
    }

    /**
     * Pretty-URL entry point.
     *
     * @return
     * @throws RecordNotFoundException
     * @throws RecordDeletedException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public String open()
            throws RecordNotFoundException, RecordDeletedException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        synchronized (this) {
            logger.trace("open()");
            try {
                update();
                if (navigationHelper == null || viewManager == null) {
                    return "";
                }

                IMetadataValue name = viewManager.getTopDocument().getMultiLanguageDisplayLabel();
                HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
                URL url = PrettyContext.getCurrentInstance(request).getRequestURL();

                for (String language : name.getLanguages()) {
                    String translation = name.getValue(language).orElse(getPersistentIdentifier());
                    if (translation != null && translation.length() > DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()) {
                        translation =
                                new StringBuilder(translation.substring(0, DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()))
                                        .append("...")
                                        .toString();
                        name.setValue(translation, language);
                    }
                }
                // Fallback using the identifier as the label
                if (name.isEmpty()) {
                    name.setValue(getPersistentIdentifier());
                }
                logger.trace("topdocument label: {} ", name.getValue());
                if (!PrettyContext.getCurrentInstance(request).getRequestURL().toURL().contains("/crowd")) {

                    // Add collection hierarchy to breadcrumbs, if the record only belongs to one collection
                    String collectionHierarchyField = DataManager.getInstance().getConfiguration().getCollectionHierarchyField();
                    if (collectionHierarchyField != null) {
                        List<String> collections = viewManager.getTopDocument().getMetadataValues(collectionHierarchyField);
                        if (collections.size() == 1) {
                            navigationHelper.addCollectionHierarchyToBreadcrumb(collections.get(0), collectionHierarchyField,
                                    DataManager.getInstance().getConfiguration().getCollectionSplittingChar(collectionHierarchyField));
                        }
                    }
                    int weight = NavigationHelper.WEIGHT_OPEN_DOCUMENT;
                    IMetadataValue anchorName = null;
                    if (isVolume() && viewManager.getAnchorPi() != null) {
                        logger.trace("anchor breadcrumb");
                        // Anchor breadcrumb
                        StructElement anchorDocument = viewManager.getTopDocument().getParent();
                        anchorName = anchorDocument.getMultiLanguageDisplayLabel();
                        for (String language : anchorName.getLanguages()) {
                            String translation = anchorName.getValue(language).orElse("");
                            if (translation != null && translation.length() > DataManager.getInstance().getConfiguration().getBreadcrumbsClipping()) {
                                translation = new StringBuilder(
                                        translation.substring(0, DataManager.getInstance().getConfiguration().getBreadcrumbsClipping())).append("...")
                                                .toString();
                                anchorName.setValue(translation, language);
                            }
                        }
                        PageType pageType = PageType.determinePageType(anchorDocument.getDocStructType(), null, true, false, false);
                        String anchorUrl = '/' + DataManager.getInstance().getUrlBuilder().buildPageUrl(anchorDocument.getPi(), 1, null, pageType);
                        navigationHelper.updateBreadcrumbs(
                                new LabeledLink(anchorName, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + anchorUrl, weight++));
                    }
                    // If volume name is the same as anchor name, add the volume number, otherwise the volume breadcrumb will be rejected as a duplicate
                    if (anchorName != null && anchorName.getValue().equals(name.getValue())) {
                        StringBuilder sb = new StringBuilder(name.getValue().get());
                        sb.append(" (");
                        if (viewManager.getTopDocument().getMetadataValue(SolrConstants.CURRENTNO) != null) {
                            sb.append(viewManager.getTopDocument().getMetadataValue(SolrConstants.CURRENTNO));
                        } else if (viewManager.getTopDocument().getMetadataValue(SolrConstants.CURRENTNOSORT) != null) {
                            sb.append(viewManager.getTopDocument().getMetadataValue(SolrConstants.CURRENTNOSORT));
                        }
                        sb.append(')');
                        name.setValue(sb.toString());
                    }
                    // Volume/monograph breadcrumb
                    navigationHelper
                            .updateBreadcrumbs(new LabeledLink(name, BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + url.toURL(), weight));
                }
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage(), e);
                Messages.error(e.getMessage());
            }

            return "";
        }
    }

    public String openFulltext()
            throws RecordNotFoundException, RecordDeletedException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        open();
        return "viewFulltext";
    }

    public BrowseElement getPrevHit() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (prevHit == null && searchBean != null) {
            prevHit = searchBean.getPreviousElement();
        }

        return prevHit;
    }

    public BrowseElement getNextHit() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (nextHit == null && searchBean != null) {
            nextHit = searchBean.getNextElement();
        }

        return nextHit;
    }

    /*********************************** Getter and Setter ***************************************/

    public long getActiveDocumentIddoc() {
        if (viewManager != null) {
            return viewManager.getTopDocumentIddoc();
        }

        return 0;
    }

    /**
     * @return the currentElement
     * @throws IndexUnreachableException
     */
    public StructElement getCurrentElement() throws IndexUnreachableException {
        if (viewManager != null) {
            return viewManager.getCurrentDocument();
        }

        return null;
    }

    /**
     * @param imageToShow the imageToShow to set
     */
    public void setImageToShow(int imageToShow) {
        synchronized (this) {
            this.imageToShow = imageToShow;
            if (viewManager != null) {
                viewManager.setDropdownSelected(String.valueOf(imageToShow));
            }
            // Reset LOGID (the LOGID setter is called later by PrettyFaces, so if a value is passed, it will still be set)
            setLogid("");
            logger.trace("imageToShow: {}", this.imageToShow);
        }
    }

    /**
     * @return the imageToShow
     */
    public int getImageToShow() {
        return imageToShow;
    }

    /**
     * @return the titleBarMetadata
     */
    public List<Metadata> getTitleBarMetadata() {
        return Metadata.filterMetadataByLanguage(titleBarMetadata, selectedRecordLanguage);
    }

    /**
     * @param logid the logid to set
     */
    public void setLogid(String logid) {
        synchronized (this) {
            if ("-".equals(logid)) {
                this.logid = "";
            } else {
                this.logid = logid;
            }
        }
    }

    /**
     * @return the logid
     */
    public String getLogid() {
        if (StringUtils.isEmpty(logid)) {
            return "-";
        }

        return logid;
    }

    /**
     * @return the anchor
     */
    public boolean isAnchor() {
        return anchor;
    }

    /**
     * @param anchor the anchor to set
     */
    public void setAnchor(boolean anchor) {
        this.anchor = anchor;
    }

    public boolean isVolume() {
        return volume;
    }

    /**
     * @param anchor the anchor to set
     */
    public boolean isGroup() {
        return group;
    }

    /**
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * @param action the action to set
     */
    public void setAction(String action) {
        synchronized (this) {
            logger.trace("setAction: " + action);
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
     * @param persistentIdentifier
     * @throws PresentationException
     * @throws RecordNotFoundException
     * @throws IndexUnreachableException
     * @should determine currentElementIddoc correctly
     */
    public void setPersistentIdentifier(String persistentIdentifier)
            throws PresentationException, RecordNotFoundException, IndexUnreachableException {
        synchronized (this) {
            logger.trace("setPersistentIdentifier: {}", persistentIdentifier);
            lastReceivedIdentifier = persistentIdentifier;
            if (!PIValidator.validatePi(persistentIdentifier)) {
                logger.warn("Invalid identifier '{}'.", persistentIdentifier);
                reset();
                return;
                // throw new RecordNotFoundException("Illegal identifier: " + persistentIdentifier);
            }
            if (!"-".equals(persistentIdentifier) && (viewManager == null || !persistentIdentifier.equals(viewManager.getPi()))) {
                long id = DataManager.getInstance().getSearchIndex().getIddocFromIdentifier(persistentIdentifier);
                if (id > 0) {
                    if (topDocumentIddoc != id) {
                        topDocumentIddoc = id;
                        logger.trace("IDDOC found for {}: {}", persistentIdentifier, id);
                    }
                } else {
                    logger.warn("No IDDOC for identifier '{}' found.", persistentIdentifier);
                    reset();
                    return;
                    // throw new RecordNotFoundException(new StringBuilder(persistentIdentifier).toString());
                }
            }
        }
    }

    /**
     * Returns the PI of the currently loaded record. Only call this method after the update() method has re-initialized ViewManager, otherwise the
     * previous PI may be returned!
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getPersistentIdentifier() throws IndexUnreachableException {
        if (viewManager != null) {
            return viewManager.getPi();
        }
        return "-";
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getThumbPart() throws IndexUnreachableException {
        if (viewManager != null) {
            return new StringBuilder("/").append(getPersistentIdentifier())
                    .append('/')
                    .append(viewManager.getCurrentThumbnailPage())
                    .append('/')
                    .toString();
        }

        return "";
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getLogPart() throws IndexUnreachableException {
        return new StringBuilder("/").append(getPersistentIdentifier())
                .append('/')
                .append(imageToShow)
                .append('/')
                .append(getLogid())
                .append('/')
                .toString();
    }

    // navigation in work

    /**
     * Returns the navigation URL for the given page type and number.
     *
     * @param pageType
     * @param page
     * @return
     * @throws IndexUnreachableException
     * @should construct url correctly
     */
    public String getPageUrl(String pageType, int page) throws IndexUnreachableException {
        StringBuilder sbUrl = new StringBuilder();
        //        if (StringUtils.isBlank(pageType)) {
        //            pageType = navigationHelper.getPreferredView();
        //            logger.trace("preferred view: {}", pageType);
        //        }
        if (StringUtils.isBlank(pageType)) {
            pageType = navigationHelper.getCurrentView();
            if (pageType == null) {
                pageType = PageType.viewObject.name();
            }
            logger.trace("current view: {}", pageType);
        }

        if (viewManager != null) {
            page = Math.max(page, viewManager.getPageLoader().getFirstPageOrder());
            page = Math.min(page, viewManager.getPageLoader().getLastPageOrder());
        }
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                .append('/')
                .append(PageType.getByName(pageType).getName())
                .append('/')
                .append(getPersistentIdentifier())
                .append('/')
                .append(page)
                .append('/');

        return sbUrl.toString();
    }

    public String getPageUrl(int page) throws IndexUnreachableException {
        return getPageUrl(null, page);
    }

    public String getPageUrl() throws IndexUnreachableException {
        String pageType = null;
        if (StringUtils.isBlank(pageType)) {
            pageType = navigationHelper.getPreferredView();
        }
        if (StringUtils.isBlank(pageType)) {
            pageType = navigationHelper.getCurrentView();
        }
        return getPageUrl(pageType);
    }

    /**
     * @param pageType
     * @return
     * @throws IndexUnreachableException
     */
    public String getPageUrl(String pageType) throws IndexUnreachableException {
        StringBuilder sbUrl = new StringBuilder();
        sbUrl.append(BeanUtils.getServletPathWithHostAsUrlFromJsfContext())
                .append('/')
                .append(PageType.getByName(pageType).getName())
                .append('/')
                .append(getPersistentIdentifier())
                .append('/');

        return sbUrl.toString();
    }

    public String getFirstPageUrl() throws IndexUnreachableException {
        if (viewManager != null) {
            return getPageUrl(viewManager.getPageLoader().getFirstPageOrder());
        }

        return null;
    }

    public String getLastPageUrl() throws IndexUnreachableException {
        if (viewManager != null) {
            return getPageUrl(viewManager.getPageLoader().getLastPageOrder());
        }

        return null;
    }

    public String getPreviousPageUrl(int step) throws IndexUnreachableException {
        if (viewManager != null && viewManager.isDoublePageMode()) {
            step *= 2;
        }
        int number = imageToShow - step;
        return getPageUrl(number);
    }

    public String getNextPageUrl(int step) throws IndexUnreachableException {
        if (viewManager != null && viewManager.isDoublePageMode()) {
            step *= 2;
        }
        int number = imageToShow + step;
        return getPageUrl(number);
    }

    public String getPreviousPageUrl() throws IndexUnreachableException {
        return getPreviousPageUrl(1);
    }

    public String getNextPageUrl() throws IndexUnreachableException {
        return getNextPageUrl(1);
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getImageUrl() throws IndexUnreachableException {
        return getPageUrl(PageType.viewImage.getName(), imageToShow);
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getFullscreenImageUrl() throws IndexUnreachableException {
        return getPageUrl(PageType.viewFullscreen.getName(), imageToShow);
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     * @deprecated renamed to fullscreen
     */
    public String getReadingModeUrl() throws IndexUnreachableException {
        return getFullscreenImageUrl();
    }

    /**
     *
     * @return
     * @throws IndexUnreachableException
     */
    public String getFulltextUrl() throws IndexUnreachableException {
        return getPageUrl(PageType.viewFulltext.getName(), imageToShow);
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     */
    public String getMetadataUrl() throws IndexUnreachableException {
        return getPageUrl(PageType.viewMetadata.getName(), imageToShow);
    }

    public StructElement getTopDocument() {
        if (viewManager != null) {
            return viewManager.getTopDocument();
        }

        return null;
    }

    public void setChildrenVisible(TOCElement element)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (getToc() != null) {
            synchronized (toc) {
                getToc().setChildVisible(element.getID());
                getToc().getActiveElement();
            }
        }
    }

    public void setChildrenInvisible(TOCElement element)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (getToc() != null) {
            synchronized (toc) {
                getToc().setChildInvisible(element.getID());
                getToc().getActiveElement();
            }
        }
    }

    /**
     * Recalculates the visibility of TOC elements and jumps to the active element after a +/- button has been pressed.
     *
     * @throws IOException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
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
     * @return the toc
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public TOC getToc() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (toc == null) {
            toc = createTOC();
        }
        return toc;
    }

    public int getTocCurrentPage() {
        return tocCurrentPage;
    }

    /**
     * 
     * @param tocCurrentPage
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @should set toc page to last page if value too high
     */
    public void setTocCurrentPage(int tocCurrentPage)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        synchronized (this) {
            this.tocCurrentPage = tocCurrentPage;
            if (this.tocCurrentPage < 1) {
                this.tocCurrentPage = 1;
            }
            // Do not call getToc() here - the setter is usually called before update(), so the required information for proper TOC creation is not yet available
            if (toc != null) {
                int currentCurrentPage = toc.getCurrentPage();
                toc.setCurrentPage(this.tocCurrentPage);
                // The TOC object will correct values that are too high, so update the local value, if necessary
                if (toc.getCurrentPage() != this.tocCurrentPage) {
                    this.tocCurrentPage = toc.getCurrentPage();
                }
                // Create a new TOC if pagination is enabled and the paginator page has changed
                if (currentCurrentPage != this.tocCurrentPage && DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage() > 0
                        && viewManager != null) {
                    toc.generate(viewManager.getTopDocument(), viewManager.isListAllVolumesInTOC(), viewManager.getMainMimeType(),
                            this.tocCurrentPage);
                }
            }
        }
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws PresentationException
     */
    public String getTitleBarLabel(Locale locale)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        return getTitleBarLabel(locale.getLanguage());
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws PresentationException
     */
    public String getTitleBarLabel() throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        Locale locale = BeanUtils.getLocale();
        if (locale != null) {
            return getTitleBarLabel(locale.getLanguage());
        }

        return getTitleBarLabel(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
    }

    /**
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws PresentationException
     */
    public String getTitleBarLabel(String language)
            throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        PageType pageType = PageType.getByName(navigationHelper.getCurrentPage());
        TOC toc = getToc();
        //        if (pageType != null && pageType.isDocumentPage() && viewManager != null && viewManager.getTopDocument() != null) {
        //            String label = viewManager.getTopDocument()
        //                    .getLabel(selectedRecordLanguage);
        //            if (StringUtils.isNotEmpty(label)) {
        //                return label;
        //            }
        //        }
        if (pageType != null && pageType.isDocumentPage() && viewManager != null) {
            // Prefer the label of the current TOC element
            if (toc != null && toc.getTocElements() != null && !toc.getTocElements().isEmpty()) {
                String label = toc.getLabel(viewManager.getPi(), language);
                if (label != null) {
                    return label;
                }
            }
            String label = viewManager.getTopDocument().getLabel(selectedRecordLanguage);
            if (StringUtils.isNotEmpty(label)) {
                return label;
            }
        } else if (cmsBean != null) {
            CMSPage cmsPage = cmsBean.getCurrentPage();
            if (cmsPage != null) {
                String cmsPageName = StringUtils.isNotBlank(cmsPage.getMenuTitle()) ? cmsPage.getMenuTitle() : cmsPage.getTitle();
                if (StringUtils.isNotBlank(cmsPageName)) {
                    return cmsPageName;
                }
            }
        }

        return null;
    }

    /**
     * Title bar label value escaped for JavaScript.
     * 
     * @return
     * @throws IndexUnreachableException
     * @throws ViewerConfigurationException
     * @throws DAOException
     * @throws PresentationException
     */
    public String getLabelForJS() throws IndexUnreachableException, PresentationException, DAOException, ViewerConfigurationException {
        String label = getTitleBarLabel();
        if (label != null) {
            return StringEscapeUtils.escapeJavaScript(label);
        }

        return null;
    }

    public int getImageContainerWidth() {
        return imageContainerWidth;
    }

    public int getNumberOfImages() throws IndexUnreachableException {
        if (viewManager != null) {
            return viewManager.getImagesCount();
        }

        return 0;
    }

    /**
     * @return Not this.topDocumentIddoc but ViewManager.topDocumentIddoc
     */
    public long getTopDocumentIddoc() {
        if (viewManager != null) {
            return viewManager.getTopDocumentIddoc();
        }
        return 0;
    }

    /**
     * Indicates whether a record is currently properly loaded in this bean. Use to determine whether to display components.
     *
     * @return
     */
    public boolean isRecordLoaded() {
        return viewManager != null;
    }

    /**
     * Checks if there is an anchor in this docStruct's hierarchy
     *
     * @return
     * @throws IndexUnreachableException
     */
    public boolean hasAnchor() throws IndexUnreachableException {
        return getTopDocument().isAnchorChild();
    }

    /**
     * Exports the currently loaded for re-indexing.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws RecordNotFoundException
     */
    public String reIndexRecordAction() throws IndexUnreachableException, DAOException, RecordNotFoundException {
        if (viewManager != null) {
            if (Helper.reIndexRecord(viewManager.getPi())) {
                Messages.info("reIndexRecordSuccess");
            } else {
                Messages.error("reIndexRecordFailure");
            }
        }

        return "";
    }

    /**
     * 
     * @param keepTraceDocument If true, a .delete file will be created; otherwise a .purge file
     * @return outcome
     * @throws IOException
     * @throws IndexUnreachableException
     */
    public String deleteRecordAction(boolean keepTraceDocument) throws IOException, IndexUnreachableException {
        try {
            if (viewManager == null) {
                return "";
            }

            //            if (viewManager.isHasVolumes()) {
            //                Messages.error("deleteRecord_failure_volumes_present");
            //                return "";
            //            }

            if (Helper.deleteRecord(viewManager.getPi(), keepTraceDocument, Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()))) {
                Messages.info("deleteRecord_success");
            } else {
                Messages.error("deleteRecord_failure");
            }
        } finally {
            deleteRecordKeepTrace = null;
        }

        return "";
    }

    public int getCurrentThumbnailPage() {
        return viewManager != null ? viewManager.getCurrentThumbnailPage() : 1;
    }

    public void setCurrentThumbnailPage(int currentThumbnailPage) {
        synchronized (this) {
            if (viewManager != null) {
                viewManager.setCurrentThumbnailPage(currentThumbnailPage);
            }
        }
    }

    /**
     * 
     * @return
     */
    public boolean isHasLanguages() {
        return recordLanguages != null && !recordLanguages.isEmpty();
    }

    /**
     * @return the recordLanguages
     */
    public List<String> getRecordLanguages() {
        return recordLanguages;
    }

    /**
     * @param recordLanguages the recordLanguages to set
     */
    public void setRecordLanguages(List<String> recordLanguages) {
        this.recordLanguages = recordLanguages;
    }

    /**
     * @return the selectedRecordLanguage
     */
    public String getSelectedRecordLanguage() {
        return selectedRecordLanguage;
    }

    /**
     * @param selectedRecordLanguage the selectedRecordLanguage to set
     */
    public void setSelectedRecordLanguage(String selectedRecordLanguage) {
        logger.trace("setSelectedRecordLanguage: {}", selectedRecordLanguage);
        if (selectedRecordLanguage != null && selectedRecordLanguage.length() == 3) {
            // Map ISO-3 codes to their ISO-2 variant
            Language language = DataManager.getInstance().getLanguageHelper().getLanguage(selectedRecordLanguage);
            if (language != null) {
                logger.trace("Mapped language found: {}", language.getIsoCodeOld());
                this.selectedRecordLanguage = language.getIsoCodeOld();
            } else {
                logger.warn("Language not found for code: {}", selectedRecordLanguage);
                this.selectedRecordLanguage = selectedRecordLanguage;
            }
        } else {
            this.selectedRecordLanguage = selectedRecordLanguage;
        }
        MetadataBean mdb = BeanUtils.getMetadataBean();
        if(mdb != null) {
            mdb.setSelectedRecordLanguage(this.selectedRecordLanguage);
        }
    }

    public boolean isAccessPermissionEpub() {
        synchronized (this) {
            try {
                if ((navigationHelper != null && !isEnabled(EPUBDownloadJob.TYPE, navigationHelper.getCurrentPage())) || viewManager == null
                        || !DownloadJob.ocrFolderExists(viewManager.getPi())) {
                    return false;
                }
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error("Error checking PDF resources: {}", e.getMessage());
                return false;
            }

            // TODO EPUB privilege type
            return viewManager.isAccessPermissionPdf();
        }
    }

    public boolean isAccessPermissionPdf() {
        synchronized (this) {
            if ((navigationHelper != null && !isEnabled(PDFDownloadJob.TYPE, navigationHelper.getCurrentPage())) || viewManager == null) {
                return false;
            }

            return viewManager.isAccessPermissionPdf();
        }
    }

    /**
     * @param currentPage
     * @return
     */
    private static boolean isEnabled(String downloadType, String pageTypeName) {
        if (downloadType.equals(EPUBDownloadJob.TYPE) && !DataManager.getInstance().getConfiguration().isGeneratePdfInTaskManager()) {
            return false;
        }
        PageType pageType = PageType.getByName(pageTypeName);
        boolean pdf = PDFDownloadJob.TYPE.equals(downloadType);
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

    public void downloadTOCAction() throws IOException, PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        try {

            String fileNameRaw = getToc().getTocElements().get(0).getLabel();
            String fileName = fileNameRaw + ".pdf";

            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();
            ec.responseReset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
            ec.setResponseContentType("application/pdf");
            ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            OutputStream os = ec.getResponseOutputStream();
            TocWriter writer = new TocWriter("", fileNameRaw);
            writer.createDocument(os, getToc().getTocElements());
            fc.responseComplete(); // Important! Otherwise JSF will attempt to render the response which obviously will fail since it's already written with a file and closed.
        } catch (IndexOutOfBoundsException e) {
            logger.error("No toc to generate");
        } catch (WriteTocException e) {
            logger.error("Error writing toc: " + e.getMessage(), e);
        }
    }

    /**
     * 
     * @param identifierField Index field containing related item identifiers
     * @return List of related items as SearchHit objects.
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     */
    public List<SearchHit> getRelatedItems(String identifierField)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("getRelatedItems: {}", identifierField);
        if (identifierField == null) {
            return null;
        }
        if (viewManager == null) {
            return null;
        }
        String query = getRelatedItemsQueryString(identifierField);
        if (query == null) {
            return null;
        }

        List<String> relatedItemIdentifiers = viewManager.getTopDocument().getMetadataValues(identifierField);
        List<SearchHit> ret = SearchHelper.searchWithFulltext(query, 0, SolrSearchIndex.MAX_HITS, null, null, null, null, null, null,
                navigationHelper.getLocale(), BeanUtils.getRequest());

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
        List<String> relatedItemIdentifiers = viewManager.getTopDocument().getMetadataValues(identifierField);
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
     * Returns a string that contains previous and/or next url <link> elements
     *
     * @return string containing previous and/or next url <link> elements
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     */
    public String getRelativeUrlTags() throws IndexUnreachableException, DAOException, PresentationException {
        if (!isRecordLoaded() || navigationHelper == null) {
            return "";
        }
        if (logger.isTraceEnabled()) {
            logger.trace("current view: {}", navigationHelper.getCurrentView());
        }

        StringBuilder sb = new StringBuilder();

        // Add canonical links
        if (viewManager.getCurrentPage() != null) {
            if (StringUtils.isNotEmpty(viewManager.getCurrentPage().getUrn())) {
                String urnResolverUrl = DataManager.getInstance().getConfiguration().getUrnResolverUrl() + viewManager.getCurrentPage().getUrn();
                sb.append("\n<link rel=\"canonical\" href=\"").append(urnResolverUrl).append("\" />");
            }
            if (viewManager.getCurrentPage().equals(viewManager.getRepresentativePage())) {
                String piResolverUrl = navigationHelper.getApplicationUrl() + "piresolver?id=" + viewManager.getPi();
                sb.append("\n<link rel=\"canonical\" href=\"").append(piResolverUrl).append("\" />");
            }
        }
        PageType currentPageType = PageType.getByName(navigationHelper.getCurrentView());
        if (currentPageType != null && StringUtils.isNotEmpty(currentPageType.name())) {
            // logger.trace("page type: {}", currentPageType.getName());
            // logger.trace("current url: {}", navigationHelper.getCurrentUrl());
            String currentUrl = navigationHelper.getCurrentUrl();
            if (currentUrl.contains("!" + currentPageType.getName())) {
                // Preferred view - add regular view URL
                sb.append("\n<link rel=\"canonical\" href=\"")
                        .append(currentUrl.replace("!" + currentPageType.getName(), currentPageType.getName()))
                        .append("\" />");
            } else if (currentUrl.contains(currentPageType.getName())) {
                // Regular view - add preferred view URL
                sb.append("\n<link rel=\"canonical\" href=\"")
                        .append(currentUrl.replace(currentPageType.getName(), "!" + currentPageType.getName()))
                        .append("\" />");
            }
        }

        // Skip prev/next links for non-paginated views
        if (PageType.viewMetadata.equals(currentPageType) || PageType.viewToc.equals(currentPageType)) {
            return "";
        }

        // Add next/prev links
        String currentUrl = getPageUrl(imageToShow);
        String prevUrl = getPreviousPageUrl();
        String nextUrl = getNextPageUrl();
        if (StringUtils.isNotEmpty(nextUrl) && !nextUrl.equals(currentUrl)) {
            sb.append("\n<link rel=\"next\" href=\"").append(nextUrl).append("\" />");
        }
        if (StringUtils.isNotEmpty(prevUrl) && !prevUrl.equals(currentUrl)) {
            sb.append("\n<link rel=\"prev\" href=\"").append(prevUrl).append("\" />");
        }

        return sb.toString();
    }

    /**
     * resets the access rights for user comments and pdf download stored in {@link ViewManager}. After reset, the access rights will be evaluated
     * again on being called
     */
    public void resetAccess() {
        if (getViewManager() != null) {
            getViewManager().resetAccessPermissionPdf();
            getViewManager().resetAllowUserComments();
        }
    }

    /**
     * @return the deleteRecordKeepTrace
     */
    public Boolean getDeleteRecordKeepTrace() {
        return deleteRecordKeepTrace;
    }

    /**
     * @param deleteRecordKeepTrace the deleteRecordKeepTrace to set
     */
    public void setDeleteRecordKeepTrace(Boolean deleteRecordKeepTrace) {
        this.deleteRecordKeepTrace = deleteRecordKeepTrace;
    }

}
