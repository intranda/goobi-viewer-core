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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IDDOCNotFoundException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordDeletedException;
import io.goobi.viewer.exceptions.RecordLimitExceededException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.media.CMSMediaMultiHolder;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.types.CMSRecordListContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSSearchContent;
import io.goobi.viewer.model.search.HitListView;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.model.viewer.collections.Sorting;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

/**
 * CMS functions.
 */
@Named
@SessionScoped
public class CmsBean implements Serializable {

    
    public static final String CMSPAGES_FILTER = "title_menuTitle_categories-name";

    private static final long serialVersionUID = -2021732230593473827L;

    private static final Logger logger = LogManager.getLogger(CmsBean.class);

    @Inject
    private transient NavigationHelper navigationHelper;
    @Inject
    private transient SearchBean searchBean;
    @Inject
    private transient UserBean userBean;
    @Inject
    private transient CmsMediaBean cmsMediaBean;
    @Inject
    private transient CMSTemplateManager templateManager;

    private TableDataProvider<CMSPage> lazyModelPages;
    /** The page currently open for viewing. */
    private CMSPage currentPage;
    private Locale selectedMediaLocale;
    private CMSMediaItem selectedMediaItem;
    private List<CMSStaticPage> staticPages = null;
    private String currentWorkPi = "";
    private Optional<CMSMediaHolder> selectedMediaHolder = Optional.empty();
    private HashMap<Long, Boolean> editablePages = new HashMap<>();
    private List<String> solrSortFields = null;
    private List<String> solrGroupFields = null;
    private String test = "";

    private List<String> luceneFields = null;

    private List<CMSNavigationItem> navigationMenuItems = null;

    /** Persists checked menu item access permission for the duration of the user session. */
    //    private final Map<Long, Boolean> menuItemAccessPermissionMap = new HashMap<>();

    public CmsBean() {

    }

    public CmsBean(CMSTemplateManager templateManager, NavigationHelper navigationHelper) {
        this.templateManager = templateManager;
        this.navigationHelper = navigationHelper;
    }

    /**
     * init.
     */
    @PostConstruct
    public void init() {
        if (lazyModelPages == null) {
            lazyModelPages = new TableDataProvider<>(new TableDataSource<CMSPage>() {

                private Optional<Long> numCreatedPages = Optional.empty();
                private List<String> allowedSubthemes = null;
                private List<String> allowedCategories = null;
                private List<Long> allowedTemplates = null;
                private boolean initialized = false;

                @Override
                public List<CMSPage> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder,
                        Map<String, String> filters) {
                    try {
                        initialize();
                        String useSortField = sortField;
                        SortOrder useSortOrder = sortOrder;
                        if (StringUtils.isBlank(useSortField)) {
                            useSortField = "id";
                        }

                        return DataManager.getInstance()
                                .getDao()
                                .getCMSPages(first, pageSize, useSortField, useSortOrder.asBoolean(), filters, allowedTemplates, allowedSubthemes,
                                        allowedCategories);
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (!numCreatedPages.isPresent()) {
                        try {
                            initialize();
                            numCreatedPages = Optional.ofNullable(DataManager.getInstance()
                                    .getDao()
                                    .getCMSPageCount(filters, allowedTemplates, allowedSubthemes, allowedCategories));
                        } catch (DAOException e) {
                            logger.error("Unable to retrieve total number of cms pages", e);
                        }
                    }
                    return numCreatedPages.orElse(0L);
                }

                private void initialize() throws DAOException {
                    if (initialized) {
                        return;
                    }
                    try {
                        if (StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField())
                                && !userBean.getUser().hasPrivilegeForAllSubthemeDiscriminatorValues()) {
                            allowedSubthemes = getAllowedSubthemeDiscriminatorValues(userBean.getUser());
                        }
                        if (!userBean.getUser().hasPriviledgeForAllTemplates()) {
                            allowedTemplates =
                                    getAllowedTemplates(userBean.getUser()).stream().map(CMSPageTemplate::getId).collect(Collectors.toList());
                        }
                        if (!userBean.getUser().hasPrivilegeForAllCategories()) {
                            allowedCategories = getAllowedCategories(userBean.getUser()).stream()
                                    .map(CMSCategory::getId)
                                    .map(l -> l
                                            .toString())
                                    .collect(Collectors.toList());
                        }
                        initialized = true;
                    } catch (PresentationException | IndexUnreachableException e) {
                        throw new DAOException("Error getting user rights from dao: " + e.toString());
                    } catch (NullPointerException e) {
                        throw new DAOException("No user or userBean available to determine user rights");
                    }
                }

                @Override
                public void resetTotalNumberOfRecords() {
                    numCreatedPages = Optional.empty();
                }
            });
            lazyModelPages.setEntriesPerPage(AdminBean.DEFAULT_ROWS_PER_PAGE);
            lazyModelPages.getFilter(CMSPAGES_FILTER);
            //            lazyModelPages.addFilter("CMSCategory", "name");
        }
    }

    /**
     * Returns the most recently edited CMS pages from the lazy model, which should include all restrictions placed upon the current user.
     *
     * @param number Number of requested rows
     * @return List<CMSPage>
     */
    public List<CMSPage> getMostRecentlyEditedPages(int number) {
        try {
            lazyModelPages.setSortField("dateUpdated");
            lazyModelPages.setSortOrder(SortOrder.DESCENDING);
            lazyModelPages.setEntriesPerPage(number);
            lazyModelPages.update();

            return lazyModelPages.getPaginatorList();
        } finally {
            lazyModelPages.setSortField("id");
            lazyModelPages.setSortOrder(SortOrder.ASCENDING);
            lazyModelPages.setEntriesPerPage(AdminBean.DEFAULT_ROWS_PER_PAGE);
            lazyModelPages.update();
        }
    }

    /**
     * Required setter for ManagedProperty injection.
     *
     * @param navigationHelper navigationHelper searchBean to set
     */
    public void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
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
     * getAllLocales.
     *
     * @return a list of all locales supported by the JSF application, with the default locale first
     */
    public static List<Locale> getAllLocales() {
        List<Locale> list = new LinkedList<>();
        list.add(ViewerResourceBundle.getDefaultLocale());
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
            Iterator<Locale> iter = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
            while (iter.hasNext()) {
                Locale locale = iter.next();
                if (!list.contains(locale)) {
                    list.add(locale);
                }
            }
        }
        return list;
    }

    /**
     * getDefaultLocale.
     *
     * @return the default application locale
     */
    public Locale getDefaultLocale() {
        return ViewerResourceBundle.getDefaultLocale();
    }

    /**
     * getCurrentLocale.
     *
     * @return the currently selected locale from the JSF view root, or the default locale if unavailable
     */
    public static Locale getCurrentLocale() {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
            return FacesContext.getCurrentInstance().getViewRoot().getLocale();
        }

        return ViewerResourceBundle.getDefaultLocale();
    }

    /**
     * loadTemplates.
     */
    public void loadTemplates() {
        logger.trace("loadTemplates");
        templateManager.reloadContentManager();
    }

    /**
     * getTemplates.
     *
     * @return all existing templates
     * @throws DAOException
     */
    public List<CMSPageTemplate> getTemplates() throws DAOException {
        try {
            return DataManager.getInstance()
                    .getDao()
                    .getAllCMSPageTemplates()
                    .stream()
                    .sorted((t1, t2) -> t1.getTitle().compareTo(t2.getTitle()))
                    .collect(Collectors.toList());
        } catch (IllegalStateException e) {
            logger.warn("Error loading templates", e);
            return Collections.emptyList();
        }
    }

    /**
     *
     * @param enabled Published/enabled status to filter by
     * @return All templates with the given enabled status
     * @throws DAOException
     */
    public List<CMSPageTemplate> getTemplates(boolean enabled) throws DAOException {
        List<CMSPageTemplate> all = getTemplates();
        if (all.isEmpty()) {
            return all;
        }

        List<CMSPageTemplate> ret = new ArrayList<>(all.size());
        for (CMSPageTemplate template : getTemplates()) {
            if (template.isPublished() == enabled) {
                ret.add(template);
            }
        }

        return ret;
    }

    /**
     * Returns a filtered page template list for the given user, unless the user is a superuser. Other CMS admins get a list matching the template ID
     * list attached to their CMS license.
     *
     * @param user user whose allowed templates are filtered
     * @return List of CMS templates whose IDs are among allowed template IDs
     * @throws DAOException
     */
    public List<CMSPageTemplate> getAllowedTemplates(User user) throws DAOException {
        logger.trace("getAllowedTemplates");
        if (user == null) {
            return Collections.emptyList();
        }
        return user.getAllowedTemplates(getTemplates())
                .stream()
                .map(CMSPageTemplate::new)
                .collect(Collectors.toList());
    }

    /**
     * Persists the enabled/disabled status of all CMS tempaltes in the DB.
     *
     * @return Navigation outcome
     * @throws DAOException
     */
    public String saveTemplatesAction() throws DAOException {
        for (CMSPageTemplate template : getTemplates()) {
            DataManager.getInstance().getDao().updateCMSPageTemplate(template);
        }
        return "pretty:adminCmsSelectTemplate";
    }

    /**
     * getDisplayedPages.
     *
     * @return a list of CMS pages for the current page in the paginated CMS page list
     */
    public List<CMSPage> getDisplayedPages() {
        return lazyModelPages.getPaginatorList();
    }

    /**
     * Getter for the field <code>lazyModelPages</code>.
     *
     * @return the TableDataProvider for the CMS page list
     */
    public TableDataProvider<CMSPage> getLazyModelPages() {
        return lazyModelPages;
    }

    /**
     * Returns the URL to the CMS template of the given page. This URL will only resolve if the page has been published or the current user is CMS
     * admin.
     *
     * @param pageId database ID of the CMS page
     * @return the absolute URL to the CMS page with the given database ID
     */
    public String getPageUrl(Long pageId) {
        return getPageUrl(pageId, true);
    }

    /**
     * getPageUrl.
     *
     * @param pageId database ID of the CMS page
     * @param pretty true to generate pretty URL, false for plain servlet URL
     * @return the absolute URL to the CMS page with the given database ID, in pretty or plain format
     */
    public String getPageUrl(Long pageId, boolean pretty) {
        try {
            CMSPage page = getPage(pageId);
            return getUrl(page, pretty);
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            return "pretty:index";
        }
    }

    /**
     * getUrl.
     *
     * @param page CMS page whose URL is resolved
     * @return the absolute pretty URL to the given CMS page
     */
    public String getUrl(CMSPage page) {
        return getUrl(page, true);
    }

    /**
     * getUrl.
     *
     * @param page CMS page whose URL is resolved
     * @param pretty true to generate pretty URL, false for plain servlet URL
     * @return the absolute URL to the given CMS page, in pretty or plain format
     */
    public String getUrl(CMSPage page, boolean pretty) {
        try {
            String host = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
            String prettyPath = page.getRelativeUrlPath(pretty);
            return new StringBuilder(host).append("/")
                    .append(prettyPath)
                    .toString();
        } catch (NullPointerException e) {
            return "pretty:index";
        }
    }

    /**
     * @param id String representation of the CMS page's database ID
     * @return CMS page matching id; null if none found
     * @throws DAOException
     */
    CMSPage findPage(String id) throws DAOException {
        CMSPage page = null;
        if (id != null) {
            try {
                logger.trace("Get cmsPage from database with pageId = {}", id);
                page = getCMSPage(Long.valueOf(id));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse page number: {}", e.getMessage());
            }
            if (page != null) {
                logger.trace("Found cmsPage {}", page.getTitle());
                // DataManager.getInstance().getDao().updateCMSPage(page);
            }
        }
        return page;
    }

    /**
     * getAllCMSPages.
     *
     * @return a list of all CMS pages in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getAllCMSPages() throws DAOException {
        return DataManager.getInstance().getDao().getAllCMSPages();
    }

    /**
     * getCMSPage.
     *
     * @param pageId database ID of the CMS page
     * @return the CMS page with the given ID, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getCMSPage(Long pageId) throws DAOException {
        Optional<CMSPage> page = Optional.ofNullable(DataManager.getInstance().getDao().getCMSPage(pageId));
        return page.orElse(null);
    }

    /**
     * isRelatedWorkLoaded.
     *
     * @return true if an {@link io.goobi.viewer.managedbeans.ActiveDocumentBean} is registered and the the
     *         {@link io.goobi.viewer.model.cms.pages.CMSPage#getRelatedPI()} of {@link #getCurrentPage()} is loaded
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isRelatedWorkLoaded() throws IndexUnreachableException {
        if (getCurrentPage() != null && StringUtils.isNotBlank(getCurrentPage().getRelatedPI())) {
            String loadedIdentifier = this.getCurrentWorkPi();
            String pagePi = getCurrentPage().getRelatedPI();
            return StringUtils.isNoneBlank(loadedIdentifier, pagePi) && loadedIdentifier.equals(pagePi);
        }
        return false;
    }

    /**
     * Returns true if the current request is a CMS page in a work context, i.e. if the URL-injected
     * {@code currentWorkPi} matches the {@code relatedPI} of the current CMS page.
     * <p>
     * This method is safe to call from JSTL {@code c:if} at view-build time because it
     * does not throw checked exceptions. It avoids the session-stale-PI problem of checking
     * {@code currentWorkPi} alone by also verifying the current page's {@code relatedPI}.
     *
     * @return true if {@code currentWorkPi} is non-blank and equals the current page's relatedPI
     * @should return false when current work pi is blank
     * @should return true when current work pi matches related PI
     * @should return false when current work pi does not match related PI
     */
    public boolean isCmsWorkPageContext() {
        // currentWorkPi is set by OCPSoft Rewrite URL injection (/page/{PI}/{pageId}/) before the
        // JSF lifecycle, so it is reliable at JSTL view-build time. Checking relatedPI in addition
        // prevents session-stale values from activating the work sidebar on unrelated CMS pages.
        return StringUtils.isNotBlank(currentWorkPi)
                && currentWorkPi.equals(getCurrentPage().getRelatedPI());
    }

    /**
     * Returns the sidebar view name for the work associated with the current CMS page.
     * The returned value matches the view names used in {@code sidebar.views.view} configuration
     * (e.g. "object", "metadata", "toc"), not the Java enum names.
     * Falls back to {@link NavigationHelper#getCurrentView()} if no related work is loaded.
     *
     * @return the sidebar view name for the work's default page (e.g. "object", "metadata", "toc"),
     *         or the current navigation view as fallback
     * @throws IndexUnreachableException if the Solr index is unreachable when checking if the related work is loaded
     * @should return current navigation view when no related work is loaded
     * @should return current navigation view when no related work loaded
     */
    public String getRelatedWorkDefaultView() throws IndexUnreachableException {
        if (!isRelatedWorkLoaded()) {
            return navigationHelper != null ? navigationHelper.getCurrentView() : PageType.viewMetadata.getRawName();
        }
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb == null || adb.getViewManager() == null) {
            return navigationHelper != null ? navigationHelper.getCurrentView() : PageType.viewMetadata.getRawName();
        }
        ViewManager vm = adb.getViewManager();
        // Guard against null when Solr is temporarily unavailable and topStructElement cannot be loaded
        StructElement topStruct = vm.getTopStructElement();
        if (topStruct == null) {
            return navigationHelper != null ? navigationHelper.getCurrentView() : PageType.viewMetadata.getRawName();
        }
        return determineWorkDefaultView(
                topStruct.getDocStructType(),
                vm.getMimeType(),
                adb.isAnchor() || adb.isGroup(),
                vm.isHasPages());
    }

    /**
     * Determines the sidebar view name for a work based on its properties.
     * Uses {@link PageType#getRawName()} to return the raw enum path ("object", "metadata", "toc")
     * that matches the keys in the {@code sidebar.views.view} configuration, independent of any
     * URL-path remapping configured in {@code config_viewer.xml}.
     * Package-private to allow unit testing without a JSF context.
     *
     * @param docStructType the logical document structure type (e.g. "monograph")
     * @param mimeType the primary MIME type of the work (e.g. "image/jpeg")
     * @param anchorOrGroup true if the work is an anchor or group record
     * @param hasPages true if the work has image pages
     * @return the sidebar view name of the determined {@link PageType}
     * @should return object when work has images
     * @should return metadata when work has no images
     * @should return toc for anchor work
     */
    static String determineWorkDefaultView(String docStructType, String mimeType, boolean anchorOrGroup, boolean hasPages) {
        PageType pageType = PageType.determinePageType(docStructType, mimeType, anchorOrGroup, hasPages, false);
        // viewImage is the legacy alias for the image viewer; normalise to viewObject since only "object"
        // has a sidebar config entry. Both page types display the same image viewer.
        if (pageType == PageType.viewImage) {
            pageType = PageType.viewObject;
        }
        // getRawName() returns the hardcoded enum path ("object", "metadata", "toc"), independent of any
        // URL-path remapping in config_viewer.xml. The sidebar config uses these raw names.
        return pageType.getRawName();
    }

    /**
     * Returns the sidebar view name appropriate for the current CMS page context, or null if no override is needed.
     * Called from {@code record_sidebar.xhtml} to determine which sidebar widgets to render without going through
     * JSF composite attributes (which cause ContextualCompositeValueExpression infinite recursion in ui:repeat).
     * When null is returned, the caller falls back to {@code navigationHelper.currentView}.
     *
     * @return sidebar view name such as "object", "metadata", "toc", or null if not on a CMS page with a loaded work
     * @should return null when no current page
     * @should return null when page has no related work
     */
    public String getEffectiveSidebarView() {
        if (currentPage == null) {
            return null;
        }
        try {
            if (isRelatedWorkLoaded()) {
                return getRelatedWorkDefaultView();
            }
        } catch (IndexUnreachableException e) {
            logger.warn("Could not determine related work default view for CMS sidebar: {}", e.getMessage());
        }
        return null;
    }

    /**
     * isLinkedFromNavBar.
     *
     * @param page CMS page to check for navigation bar links
     * @return true if the given CMS page is referenced by at least one navigation bar item, false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isLinkedFromNavBar(CMSPage page) throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        if (dao != null && page != null) {
            List<CMSNavigationItem> relatedItems = dao.getRelatedNavItem(page);
            return relatedItems != null && !relatedItems.isEmpty();
        }
        return false;
    }

    /**
     *
     * @return the {@link #getCurrentPage()} if one is set, or an empty Optional if the current page is not a cmsPage (i.e. if
     *         {@link NavigationHelper#isCmsPage()} == false)
     */
    public Optional<CMSPage> getCurrentCmsPageIfLoaded() {
        return Optional.ofNullable(currentPage)
                .filter(page -> BeanUtils.getNavigationHelper().isCmsPage());
    }

    /**
     * getPage.
     *
     * @param page CMS page to return, or null to fall back to current page
     * @return the given page, or the current page if null is passed
     */
    public CMSPage getPage(CMSPage page) {
        return page == null ? currentPage : page;
    }

    /**
     * getPage.
     *
     * @param pageId database ID of the CMS page to look up
     * @return the CMS page with the given ID, or null if the ID is null
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSPage getPage(Long pageId) throws DAOException {

        if (pageId != null) {
            return getCMSPage(pageId);
        }
        return null;
    }

    /**
     * Getter for the field <code>currentPage</code>.
     *
     * @return the currently active CMS page, or an empty CMSPage if none is set
     * @should page
     */
    public CMSPage getCurrentPage() {
        if (currentPage == null) {
            return new CMSPage();
        }
        return currentPage;
    }

    /**
     * Setter for the field <code>currentPage</code>.
     *
     * @param currentPage CMS page to set as the currently active page
     */
    public void setCurrentPage(CMSPage currentPage) {
        if (currentPage != null) {
            CMSPage page = new CMSPage(currentPage);
            page.initialiseCMSComponents(templateManager);
            page.setListPage(1);
            this.currentPage = page;
            navigationHelper.setCmsPage(true);
            logger.trace("Set current cms page to {}", this.currentPage.getTitle());
        } else {
            this.currentPage = null;
        }
    }

    /**
     * For unit tests.
     * 
     * @param currentPage CMS page to set without further initialization
     */
    void setCurrentPageNoInit(CMSPage currentPage) {
        this.currentPage = new CMSPage(currentPage);
    }

    /**
     * getCurrentPageId.
     *
     * @return the database ID of the currently active CMS page as a string, or "0" if none is set
     */
    public String getCurrentPageId() {
        if (currentPage != null) {
            return String.valueOf(currentPage.getId());
        }

        return "0";
    }

    /**
     * Page ID setter for PrettyFaces.
     *
     * @param id string representation of the CMS page database ID
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    public void setCurrentPageId(String id) throws DAOException, ContentNotFoundException {
        logger.trace("setCurrentPageId: {}", id);
        CMSPage page = findPage(id);
        setCurrentPage(page);
    }

    /**
     * checkRelatedWork.
     *
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    public void checkRelatedWork() throws ContentNotFoundException {
        CMSPage page = getCurrentPage();
        //if we have both a cmsPage and a currentWorkPi set, they must be the same
        //the currentWorkPi is set via pretty mapping
        if (page != null && StringUtils.isNotBlank(getCurrentWorkPi()) && !getCurrentWorkPi().equals(page.getRelatedPI())) {
            throw new ContentNotFoundException("There is no CMS page with id " + page.getId() + " related to PI " + getCurrentWorkPi());
        }
    }

    /**
     * getAllCategories.
     *
     * @return the return value of {@link io.goobi.viewer.dao.IDAO#getAllCategories()}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCategory> getAllCategories() throws DAOException {
        return DataManager.getInstance().getDao().getAllCategories();
    }

    /**
     * Getter for the field <code>selectedMediaItem</code>.
     *
     * @return the currently selected CMS media item
     */
    public CMSMediaItem getSelectedMediaItem() {
        return selectedMediaItem;
    }

    /**
     * Setter for the field <code>selectedMediaItem</code>.
     *
     * @param selectedMediaItem media item to mark as selected
     */
    public void setSelectedMediaItem(CMSMediaItem selectedMediaItem) {
        // logger.trace("Set media item to {}", selectedMediaItem.getFileName()); //NOSONAR Debug
        this.selectedMediaItem = selectedMediaItem;
    }

    /**
     * Getter for the field <code>selectedMediaLocale</code>.
     *
     * @return the locale currently selected for the media item, defaulting to the application default locale
     */
    public Locale getSelectedMediaLocale() {
        if (selectedMediaLocale == null) {
            selectedMediaLocale = IPolyglott.getDefaultLocale();
        }
        return selectedMediaLocale;
    }

    /**
     * Setter for the field <code>selectedMediaLocale</code>.
     *
     * @param selectedMediaLocale locale used for the selected media item
     */
    public void setSelectedMediaLocale(Locale selectedMediaLocale) {
        this.selectedMediaLocale = selectedMediaLocale;
    }

    /**
     * Action method called when a CMS page is opened. The exact action depends on the page and content item type.
     *
     * @return the navigation outcome after processing the CMS page context action
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @throws RecordDeletedException
     * @throws RecordNotFoundException
     * @throws IllegalRequestException
     */
    public String cmsContextAction()
            throws IndexUnreachableException, DAOException, ViewerConfigurationException, RecordNotFoundException, RecordDeletedException {
        return cmsContextAction(true);
    }

    /**
     * Action method called when a CMS page is opened. The exact action depends on the page and content item type.
     *
     * @param resetSearch If true, the search parameters in SearchBean will be reset
     * @return the navigation outcome after processing the CMS page context action
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @throws RecordDeletedException
     * @throws RecordNotFoundException
     * @throws IllegalRequestException
     */
    public String cmsContextAction(boolean resetSearch)
            throws IndexUnreachableException, DAOException, ViewerConfigurationException, RecordNotFoundException, RecordDeletedException {
        logger.trace("cmsContextAction: {}", resetSearch);
        if (currentPage == null) {
            return "";
        }

        // If the page is related to a record, load that record
        if (StringUtils.isNotEmpty(currentPage.getRelatedPI())) {
            ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
            if (adb != null && !currentPage.getRelatedPI().equals(adb.getPersistentIdentifier())) {
                logger.trace("Loading related record: {}", currentPage.getRelatedPI());
                try {
                    adb.setPersistentIdentifier(currentPage.getRelatedPI());
                    adb.update();
                } catch (RecordNotFoundException | RecordDeletedException | PresentationException | RecordLimitExceededException e) {
                    logger.warn(e.getMessage());
                } catch (IDDOCNotFoundException e) {
                    try {
                        adb.reload(currentPage.getRelatedPI());
                    } catch (PresentationException | RecordLimitExceededException e1) {
                        logger.warn(e.getMessage());
                    }
                }
            }
        }

        //Execute all page loading reoutines of content items.
        //If one returns a redirect url, return that, otherwise an empty string
        return currentPage.getComponents()
                .stream()
                .flatMap(comp -> comp.getContentItems().stream())
                .map(content -> {
                    try {
                        return content.getContent().handlePageLoad(resetSearch, content.getOwningComponent());
                    } catch (PresentationException e) {
                        // Log as warning without stack trace; include request URL and exception message for diagnostics
                        String requestUrl = "unknown";
                        FacesContext fc = FacesContext.getCurrentInstance();
                        if (fc != null && fc.getExternalContext().getRequest() instanceof HttpServletRequest httpRequest) {
                            requestUrl = httpRequest.getRequestURL().toString();
                        }
                        logger.warn("Error handling page load for page {} in content {}: {} (Request URL: {})",
                                content.getContent().getOwningPage().getId(), content.getItemId(), e.getMessage(), requestUrl);
                        return "";
                    }
                })
                .filter(StringUtils::isNotBlank)
                .findAny()
                .orElse("");

    }

    /**
     * getQueryResults.
     *
     * @return a list of search hit objects from the current search, or an empty list if no search is active
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<SearchHit> getQueryResults() throws IndexUnreachableException, PresentationException, DAOException {
        if (searchBean != null) {
            Search search = searchBean.getCurrentSearch();
            if (search != null) {
                return search.getHits();
            }
        }

        return Collections.emptyList();
    }

    /**
     *
     * @param hits List of search hits to group
     * @param groupingField Solr field name to group hits by
     * @return List&lt;Entry&lt;String, List&lt;SearchHit&gt;&gt;&gt;
     * @should return collection with 3 elements
     */
    public List<Entry<String, List<SearchHit>>> getGroupedQueryResults(List<SearchHit> hits, String groupingField) {

        Map<String, List<SearchHit>> hitMap = new HashMap<>();
        for (SearchHit searchHit : hits) {
            List<String> groupingValues = getMetadataValues(searchHit, groupingField);
            if (groupingValues.isEmpty()) {
                List<SearchHit> valueHits = hitMap.get("");
                if (valueHits == null) {
                    valueHits = new ArrayList<>();
                    hitMap.put("", valueHits);
                }
                valueHits.add(searchHit);
            } else {
                for (String value : groupingValues) {
                    List<SearchHit> valueHits = hitMap.computeIfAbsent(value, k -> new ArrayList<>());
                    valueHits.add(searchHit);
                }
            }
        }
        List<Entry<String, List<SearchHit>>> entryList = new ArrayList<>(hitMap.entrySet());
        entryList.sort((e1, e2) -> e1.getKey().compareTo(e2.getKey()));
        return entryList;
    }

    /**
     *
     * @param hit Search hit whose Solr document is queried
     * @param solrField Solr field name to retrieve values for
     * @return Metadata values for the given Solr field from the given search hit
     */
    private static List<String> getMetadataValues(SearchHit hit, String solrField) {

        SolrDocument doc = hit.getSolrDoc();
        if (doc != null) {
            Collection<Object> values = doc.getFieldValues(solrField);
            if (values != null) {
                return values.stream().map(SolrTools::getAsString).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    /**
     * hasSearchResults.
     *
     * @return true if the current search returned at least one hit, false otherwise
     */
    public boolean hasSearchResults() {
        return searchBean != null && searchBean.getCurrentSearch() != null && searchBean.getCurrentSearch().getHitsCount() > 0;
    }

    /**
     * Calculates the number of pages needed for the paginator. The value is taken directly from
     * {@link io.goobi.viewer.model.search.Search#getLastPage(int)}
     *
     * @return The number of pages to display in the paginator
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public long getQueryResultCount() throws PresentationException, IndexUnreachableException {
        if (searchBean != null && searchBean.getCurrentSearch() != null) {
            return searchBean.getCurrentSearch().getLastPage(searchBean.getHitsPerPage());
        }
        return 0;
    }

    /**
     * getFieldNames.
     *
     * @param solrDoc Solr document whose field names are retrieved
     * @return a list of all field names present in the given Solr document
     */
    public List<String> getFieldNames(SolrDocument solrDoc) {
        if (solrDoc != null) {
            return new ArrayList<>(solrDoc.getFieldNames());
        }
        return Collections.emptyList();
    }

    /**
     * getLuceneFields.
     *
     * @return a list of all Solr field names excluding internal, facet, and norm fields
     * @should return true for given input
     */
    public List<String> getLuceneFields() {
        return getLuceneFields(false, false);
    }

    /**
     * getLuceneFields.
     *
     * @param includeUntokenized true to include fields ending in _UNTOKENIZED
     * @return a list of Solr field names optionally including untokenized variants
     */
    public List<String> getLuceneFields(boolean includeUntokenized) {
        return getLuceneFields(includeUntokenized, false);
    }

    /**
     * getLuceneFields.
     *
     * @param includeUntokenized true to include fields ending in _UNTOKENIZED
     * @param excludeTokenizedMetadataFields true to exclude tokenized MD_ fields
     * @return a list of Solr field names filtered according to the given options, sorted alphabetically
     */
    public List<String> getLuceneFields(boolean includeUntokenized, boolean excludeTokenizedMetadataFields) {
        try {
            if (this.luceneFields == null) {
                this.luceneFields = DataManager.getInstance().getSearchIndex().getAllFieldNames();
            }

            Stream<String> filteredLuceneFields = this.luceneFields.stream()
                    .filter(name -> !(name.startsWith("_") || name.startsWith("FACET_") || name.startsWith("NORM_")));
            if (!includeUntokenized) {
                filteredLuceneFields = filteredLuceneFields.filter(name -> !name.endsWith(SolrConstants.SUFFIX_UNTOKENIZED));
            }
            if (excludeTokenizedMetadataFields) {
                filteredLuceneFields =
                        filteredLuceneFields.filter(name -> !(name.startsWith("MD_") && !name.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)));
            }
            filteredLuceneFields = filteredLuceneFields.sorted();
            return filteredLuceneFields.collect(Collectors.toList());

        } catch (IndexUnreachableException e) {
            logger.error("Error retrieving solr fields", e);
            return Collections.singletonList("");
        }

    }

    /**
     * Getter for the field <code>staticPages</code>.
     *
     * @return a list of all CMS static pages mapping viewer page types to CMS pages
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return non empty collection for given input
     */
    public List<CMSStaticPage> getStaticPages() throws DAOException {
        if (this.staticPages == null) {
            this.staticPages = createStaticPageList();
        }
        return this.staticPages;
    }

    /**
     * getStaticPage.
     *
     * @param pageName name of the static page type to look up
     * @return the CMSStaticPage matching the given name, or null if not found
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public CMSStaticPage getStaticPage(String pageName) throws DAOException {
        List<CMSStaticPage> pages = getStaticPages();
        for (CMSStaticPage page : pages) {
            if (page.getPageName().equals(pageName)) {
                return page;
            }
        }
        return null;
    }

    /**
     *
     * @param page CMS page to check for a static page mapping
     * @return true if the given CMS page is mapped to any static page; otherwise
     * @throws DAOException
     */
    public boolean isMappedToStaticPage(CMSPage page) throws DAOException {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }

        for (CMSStaticPage staticPage : getStaticPages()) {
            if (page.equals(staticPage.getCmsPage())) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return List of static pages in the order specified in the PageType enum
     * @throws DAOException
     * @should return pages in specified order
     */
    static List<CMSStaticPage> createStaticPageList() throws DAOException {
        List<CMSStaticPage> staticPages = DataManager.getInstance().getDao().getAllStaticPages();

        List<PageType> pageTypesForCMS = PageType.getTypesHandledByCms();

        List<CMSStaticPage> ret = new ArrayList<>(pageTypesForCMS.size());
        for (PageType pageType : pageTypesForCMS) {
            CMSStaticPage newPage = new CMSStaticPage(pageType.getName());
            if (staticPages.contains(newPage)) {
                ret.add(staticPages.get(staticPages.indexOf(newPage)));
            } else {
                ret.add(newPage);
            }
        }

        return ret;
    }

    /**
     * getAvailableParentPages.
     *
     * @param page CMS page to exclude from the result list
     * @return A list of all cmsPages except the given one
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getAvailableParentPages(CMSPage page) throws DAOException {
        Locale currentLocale = BeanUtils.getLocale();
        return getAllCMSPages().stream()
                .filter(p -> !p.equals(page))
                .sorted((p1, p2) -> p1.getTitle(currentLocale).toLowerCase().compareTo(p2.getTitle(currentLocale).toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * getAvailableCmsPages.
     *
     * @param page static page whose existing mapping is excluded from filtering
     * @return A list of all cmsPages not yet registered to a static page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return 2 for given input
     */
    public List<CMSPage> getAvailableCmsPages(CMSStaticPage page) throws DAOException {
        List<CMSPage> allPages = getAllCMSPages().stream()
                .filter(CMSPage::isPublished)
                .collect(Collectors.toList());

        for (CMSStaticPage staticPage : getStaticPages()) {
            Optional<CMSPage> cmsPage = staticPage.getCmsPageOptional();
            if (!staticPage.equals(page) && staticPage.isHasCmsPage() && cmsPage.isPresent()) {
                allPages.remove(cmsPage.get());
            }
        }
        return allPages;
    }

    /**
     * getCMSPagesWithSearch.
     *
     * @return a list of all valid cms pages which contain a "search" item
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getCMSPagesWithSearch() throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getAllCMSPages()
                .stream()
                .filter(cmsPage -> cmsPage.getPersistentComponents()
                        .stream()
                        .flatMap(c -> c.getContentItems().stream())
                        .anyMatch(CMSSearchContent.class::isInstance))
                .collect(Collectors.toList());
    }

    /**
     * Saves static page status for all cms pages.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveStaticPages() throws DAOException {
        for (CMSStaticPage page : getStaticPages()) {
            try {
                //delete static pages with no mapped cms page to remove deprecated pages
                if (page.getId() != null && !page.isHasCmsPage()) {
                    DataManager.getInstance().getDao().deleteStaticPage(page);
                } else if (page.getId() != null) {
                    DataManager.getInstance().getDao().updateStaticPage(page);
                } else if (page.isHasCmsPage()) {
                    DataManager.getInstance().getDao().addStaticPage(page);
                }
            } catch (DAOException e) {
                Messages.error("cms_errorSavingStaticPages");
                return;
            }
        }
        this.staticPages = null;
        Messages.info("cms_staticPagesSaved");
    }

    public boolean isLinkedToStaticPage(CMSPage page) throws DAOException {
        for (CMSStaticPage staticPage : getStaticPages()) {
            if (staticPage.getCmsPageId().map(id -> id.equals(page.getId())).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * getValidCMSPages.
     *
     * @return all cmsPages which are valid and have a menu title
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getValidCMSPages() throws DAOException {
        return getAllCMSPages().stream()
                .filter(page -> StringUtils.isNotBlank(page.getMenuTitle()))
                .filter(CMSPage::isPublished)
                .collect(Collectors.toList());
    }

    /**
     * getFacesContext.
     *
     * @return the current FacesContext instance
     */
    protected FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    /**
     * getSubthemeDiscriminatorValues.
     *
     * @return a list of all values found in the configured subtheme discriminator Solr field
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public List<String> getSubthemeDiscriminatorValues() throws PresentationException {
        String subThemeDiscriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
        if (StringUtils.isNotBlank(subThemeDiscriminatorField)) {
            try {
                return SearchHelper.getFacetValues(subThemeDiscriminatorField + ":*", subThemeDiscriminatorField, 0);
            } catch (IndexUnreachableException e) {
                logger.error(e.getMessage());
                Messages.error("errIndexUnreachableMsg");
            }
        }

        return Collections.emptyList();
    }

    /**
     * Returns a filtered subtheme discriminator value list for the given user, unless the user is a superuser. Other CMS admins get a list matching
     * values list attached to their CMS license.
     *
     * @param user user whose allowed subtheme values are filtered
     * @return List of CMS templates whose IDs are among allowed template IDs
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getAllowedSubthemeDiscriminatorValues(User user) throws PresentationException, IndexUnreachableException {
        if (user == null) {
            return Collections.emptyList();
        }

        return user.getAllowedSubthemeDiscriminatorValues(getSubthemeDiscriminatorValues());
    }

    /**
     * isSubthemeRequired.
     *
     * @param user user to check for subtheme restrictions
     * @return true if user is limited to a subset of all available subtheme discriminator values; false otherwise
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean isSubthemeRequired(User user) throws PresentationException, IndexUnreachableException {
        return user != null && !user.hasPrivilegeForAllSubthemeDiscriminatorValues();
    }

    /**
     * Returns a filtered category list for the given user, unless the user is a superuser. Other CMS admins get a list matching values list attached
     * to their CMS license.
     *
     * @param user user whose allowed categories are filtered
     * @return a list of CMS categories the given user is permitted to access
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCategory> getAllowedCategories(User user) throws DAOException {
        if (user == null) {
            return Collections.emptyList();
        }

        return user.getAllowedCategories(getAllCategories());
    }

    /**
     * Sets the searchType in SearchBean to the type assciated with the current static view (e.g. if the current cms page replaces the static page
     * 'advancedSearch' the search type is set to 'advanced') For the normal search pages this is done in the pretty mapping which isn't used if
     * redirecting to cms page
     */
    public void setSearchType() {
        logger.trace("setSearchType");
        Optional<ViewerPath> currentPath = ViewHistory.getCurrentView(BeanUtils.getRequest());
        if (currentPath.isPresent() && searchBean != null) {
            if (PageType.advancedSearch.equals(currentPath.get().getPageType())) {
                searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_ADVANCED);
            } else if (PageType.searchCalendar.equals(currentPath.get().getPageType())) {
                searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_CALENDAR);
            } else if (PageType.search.equals(currentPath.get().getPageType())) {
                searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
            }
        }
    }

    /**
     * isHasRelatedPages.
     *
     * @param pi persistent identifier of the record to check
     * @return true if there are CMS pages related to the given persistent identifier, false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isHasRelatedPages(String pi) throws DAOException {
        if (StringUtils.isEmpty(pi)) {
            return false;
        }
        List<CMSPage> relatedPages = getRelatedPages(pi);
        return relatedPages != null && !relatedPages.isEmpty();
    }

    /**
     * getRelatedPages.
     *
     * @param pi persistent identifier of the record to look up
     * @return a list of CMS pages associated with the given record identifier
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getRelatedPages(String pi) throws DAOException {
        List<CMSPage> relatedPages = DataManager.getInstance().getDao().getCMSPagesForRecord(pi, null);
        return relatedPages.stream()
                //                .filter(page -> page.isPublished())
                .collect(Collectors.toList());
    }

    /**
     * getRelatedPages.
     *
     * @param pi persistent identifier of the record to look up
     * @param category category to filter CMS pages by
     * @return a list of published CMS pages associated with the given record identifier and category
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getRelatedPages(String pi, CMSCategory category) throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getCMSPagesForRecord(pi, category)
                .stream()
                //                .filter(page -> pi.equals(page.getRelatedPI()))
                //                .filter(page -> page.getClassifications().contains(classification))
                .filter(CMSPage::isPublished)
                .collect(Collectors.toList());
    }

    /**
     * getRepresentativeImageForQuery.
     *
     * @param page CMS page whose record list query selects the representative image
     * @return the thumbnail URL of the first record matching the CMS page's Solr query, at the configured size
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRepresentativeImageForQuery(CMSPage page) throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        int width = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
        int height = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
        return getRepresentativeImageForQuery(page, width, height);
    }

    /**
     * getRepresentativeImageForQuery.
     *
     * @param item record list content item whose Solr query selects the representative image
     * @return the thumbnail URL of the first record matching the content item's Solr query, at the configured size
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRepresentativeImageForQuery(CMSRecordListContent item)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        int width = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
        int height = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
        return getRepresentativeImageForQuery(item, width, height);
    }

    /**
     * getRepresentativeImageForQuery.
     *
     * @param page CMS page whose record list query selects the representative image
     * @param width desired thumbnail width in pixels
     * @param height desired thumbnail height in pixels
     * @return the thumbnail URL of the first record matching the CMS page's Solr query at the specified dimensions
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRepresentativeImageForQuery(CMSPage page, int width, int height)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        CMSRecordListContent contentItem = page.getPersistentComponents()
                .stream()
                .flatMap(c -> c.getContentItems().stream())
                .filter(CMSRecordListContent.class::isInstance)
                .map(CMSRecordListContent.class::cast)
                .findAny()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "The page does not contain content items of type '" + CMSRecordListContent.class.getSimpleName() + "'"));
        return getRepresentativeImageForQuery(contentItem, width, height);
    }

    /**
     * getRepresentativeImageForQuery.
     *
     * @param item record list content item whose Solr query selects the representative image
     * @param width desired thumbnail width in pixels
     * @param height desired thumbnail height in pixels
     * @return the thumbnail URL of the first record matching the content item's Solr query at the specified dimensions
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRepresentativeImageForQuery(CMSRecordListContent item, int width, int height)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        if (StringUtils.isBlank(item.getSolrQuery())) {
            throw new IllegalStateException("Item " + item + " does not define a solr query");
        }
        SolrDocument doc =
                DataManager.getInstance().getSearchIndex().getFirstDoc(item.getSolrQuery(), new ArrayList<>(ThumbnailHandler.REQUIRED_SOLR_FIELDS));
        if (doc != null) {
            return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(doc, width, height);
        }
        throw new PresentationException("No document matching query '" + item.getSolrQuery() + "' found");
    }

    /**
     * getPossibleSortFields.
     *
     * @return a list of all available sort field names including relevance, random, and all SORT_ fields from the Solr schema
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     * @throws java.io.IOException if any.
     * @should add relevance and random values at beginning
     */
    public List<String> getPossibleSortFields() throws SolrServerException, IOException {
        if (this.solrSortFields == null) {
            this.solrSortFields = new ArrayList<>();
            this.solrSortFields.add(SolrConstants.SORT_RELEVANCE);
            this.solrSortFields.add(SolrConstants.SORT_RANDOM);
            this.solrSortFields.add(SolrConstants.CURRENTNOSORT);
            this.solrSortFields.add(SolrConstants.DATECREATED);
            this.solrSortFields.add(SolrConstants.DATEUPDATED);
            this.solrSortFields.addAll(DataManager.getInstance().getSearchIndex().getAllSortFieldNames());
        }
        return this.solrSortFields;
    }

    /**
     * getPossibleGroupFields.
     *
     * @return a list of all Solr field names suitable for grouping search results, sorted alphabetically
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     * @throws java.io.IOException if any.
     * @throws DAOException
     */
    public List<String> getPossibleGroupFields() throws IndexUnreachableException {

        if (this.solrGroupFields == null) {
            this.solrGroupFields = Stream.concat(
                    DataManager.getInstance()
                            .getSearchIndex()
                            .getAllFieldNames()
                            .stream(),
                    DataManager.getInstance()
                            .getSearchIndex()
                            .getAllBooleanFieldNames()
                            .stream())
                    .filter(field -> !field.startsWith("SORT_") && !field.startsWith("FACET_") && !field.endsWith("_UNTOKENIZED")
                            && !field.matches(".*_LANG_\\w{2,3}"))
                    .collect(Collectors.toList());
            Collections.sort(solrGroupFields);
        }
        return this.solrGroupFields;

    }

    /**
     * 
     * @return Configured search result group names; empty list if disabled
     */
    public List<String> getPossibleResultGroupNames() {
        List<SearchResultGroup> groups = DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled()
                ? DataManager.getInstance().getConfiguration().getSearchResultGroups() : Collections.emptyList();

        List<String> ret = new ArrayList<>(groups.size());
        for (SearchResultGroup group : groups) {
            ret.add(group.getName());
        }

        return ret;
    }

    /**
     * Returns metadataList types from the configuration where the type value stars with "cms_".
     * 
     * @return List of configured metadata lists where the type starts with "cms_"
     */
    public List<String> getCmsMetadataListTypes() {
        return DataManager.getInstance().getConfiguration().getMetadataListTypes("cms_");
    }

    /**
     * getCssClass.
     *
     * @return The {@link io.goobi.viewer.model.cms.pages.CMSPage#getWrapperElementClass()} of the current CMSPage. If no current page is set or
     *         {@link io.goobi.viewer.managedbeans.NavigationHelper#isCmsPage()} returns false, an empty String is returned
     */
    public String getCssClass() {
        if (BeanUtils.getNavigationHelper().isCmsPage() && getCurrentPage() != null) {
            return getCurrentPage().getWrapperElementClass();
        }

        return "";
    }

    /**
     * getLastEditedTimestamp.
     *
     * @param pageId database ID of the CMS page
     * @return the last-modified timestamp in milliseconds for the given CMS page, or null if unavailable
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public Long getLastEditedTimestamp(long pageId) throws DAOException {
        CMSPage page = getCMSPage(pageId);
        if (page == null || page.getDateUpdated() == null) {
            return null;
        }

        return DateTools.getMillisFromLocalDateTime(page.getDateUpdated(), false);
    }

    /**
     * Getter for the field <code>currentWorkPi</code>.
     *
     * @return the persistent identifier of the record currently associated with the CMS page
     */
    public String getCurrentWorkPi() {
        return currentWorkPi;
    }

    /**
     * Setter for the field <code>currentWorkPi</code>.
     *
     * @param currentWorkPi persistent identifier of the record currently associated with the CMS page
     */
    public void setCurrentWorkPi(String currentWorkPi) {
        this.currentWorkPi = currentWorkPi == null ? "" : currentWorkPi;
    }

    /**
     * resetCurrentWorkPi.
     */
    public void resetCurrentWorkPi() {
        this.currentWorkPi = "";
    }

    /**
     * Set a {@link io.goobi.viewer.model.cms.media.CMSMediaHolder} in the {@link io.goobi.viewer.managedbeans.CmsMediaBean} which may receive a
     * {@link io.goobi.viewer.model.cms.media.CMSMediaItem} selected in the selectMedia dialog.
     *
     * @param item media holder that will receive the selected media item
     */
    public void setSelectedMediaHolder(CMSMediaHolder item) {
        this.selectedMediaHolder = Optional.ofNullable(item);
        this.selectedMediaHolder.ifPresent(contentItem -> {
            String filter = contentItem.getMediaFilter();
            if (StringUtils.isBlank(filter)) {
                filter = "(" + CmsMediaBean.getImageFilter() + ")|(" + CmsMediaBean.getVideoFilter() + ")";
            }
            cmsMediaBean.setFilenameFilter(filter);
            if (contentItem.hasMediaItem()) {
                CategorizableTranslatedSelectable<CMSMediaItem> wrapper = contentItem.getMediaItemWrapper();
                try {
                    List<CMSCategory> categories =
                            BeanUtils.getUserBean().getUser().getAllowedCategories(DataManager.getInstance().getDao().getAllCategories());
                    wrapper.setCategories(contentItem.getMediaItem().wrapCategories(categories));
                } catch (DAOException e) {
                    logger.error("Unable to determine allowed categories for media holder", e);
                }
                cmsMediaBean.setSelectedMediaItem(wrapper);
            } else {
                cmsMediaBean.setSelectedMediaItem(null);
            }
        });
    }

    public CMSMediaHolder getSelectedMediaHolder() {
        return selectedMediaHolder.orElse(null);
    }

    /**
     * Set the given (wrapped) {@link io.goobi.viewer.model.cms.media.CMSMediaItem} to Media holder set by {@link #setSelectedMediaHolder}.
     *
     * @param mediaItem wrapped media item to place into the selected holder
     */
    public void fillSelectedMediaHolder(CategorizableTranslatedSelectable<CMSMediaItem> mediaItem) {
        fillSelectedMediaHolder(mediaItem, false);
    }

    /**
     * Set the given (wrapped) {@link io.goobi.viewer.model.cms.media.CMSMediaItem} to Media holder set by {@link #setSelectedMediaHolder}
     * Additionally save the given media item if the parameter saveMedia is set to true.
     *
     * @param mediaItem wrapped media item to place into the selected holder
     * @param saveMedia true to persist the media item to the database
     */
    public void fillSelectedMediaHolder(CategorizableTranslatedSelectable<CMSMediaItem> mediaItem, boolean saveMedia) {
        fillSelectedMediaHolder(mediaItem, 0, saveMedia);
    }

    /**
     * 
     * @param mediaItem Media item to place into the selected media holder
     * @param index Media item index for instances of CMSMediaMultiHolder
     * @param saveMedia If true, persist the media item to the database
     */
    public void fillSelectedMediaHolder(CategorizableTranslatedSelectable<CMSMediaItem> mediaItem, int index, boolean saveMedia) {
        logger.trace("fillSelectedMediaHolder, index {}", index);
        this.selectedMediaHolder.ifPresent(holder -> {
            if (mediaItem != null) {
                if (holder instanceof CMSMediaMultiHolder mediaMultiItem) {
                    mediaMultiItem.setMediaItem(index, mediaItem.getValue());
                } else {
                    holder.setMediaItem(mediaItem.getValue());
                }
                if (saveMedia) {
                    try {
                        cmsMediaBean.saveMedia(mediaItem.getValue(), mediaItem.getCategories());
                    } catch (DAOException e) {
                        logger.error("Failed to save media item: {}", e.toString());
                    }
                }
            } else {
                holder.setMediaItem(null);
            }
        });
        this.selectedMediaHolder = Optional.empty();
        cmsMediaBean.setSelectedMediaItem(null);
    }

    /**
     * hasSelectedMediaHolder.
     *
     * @return true if a mediaHolder is present
     */
    public boolean hasSelectedMediaHolder() {
        return this.selectedMediaHolder.isPresent();
    }

    /**
     * mayEdit.
     *
     * @param page CMS page to check edit permissions for
     * @return true if the currently logged-in user has privileges to edit the given CMS page, false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean mayEdit(CMSPage page) throws DAOException, PresentationException, IndexUnreachableException {

        if (userBean.getUser() != null && page != null) {
            synchronized (editablePages) {
                Boolean mayEdit = editablePages.get(page.getId());
                if (mayEdit == null) {
                    mayEdit = hasPrivilegesToEdit(userBean.getUser(), page);
                    editablePages.put(page.getId(), mayEdit);
                }
                return mayEdit;
            }
        }

        return false;
    }

    /**
     * @param user User whose privileges are checked
     * @param page CMS page to check edit access for
     * @return true if given use may edit given page; false otherwise
     * @throws DAOException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private boolean hasPrivilegesToEdit(User user, CMSPage page) throws DAOException, PresentationException, IndexUnreachableException {
        if (user == null || !user.isCmsAdmin()) {
            return false;
        } else if (user.isSuperuser()) {
            return true;
        } else {
            if (!user.hasPriviledgeForAllTemplates() && user.hasPrivilegesForTemplate(page.getTemplate().orElse(null))) {
                return false;
            }
            if (!user.hasPrivilegeForAllCategories() && ListUtils.intersection(getAllowedCategories(user), page.getCategories()).isEmpty()) {
                return false;
            }
            return user.hasPrivilegeForAllSubthemeDiscriminatorValues()
                    || getAllowedSubthemeDiscriminatorValues(user).contains(page.getSubTheme());
        }
    }

    /**
     * For CMS pages with {@link CMSPage#getWrapperElementClass()} return 'body_' followed by the wrapperElementClass. Otherwise return an empty
     * String
     *
     * @return Configured wrapper element class of the current page; otherwise empty string
     */
    public String getCmsBodyClass() {
        if (navigationHelper.isCmsPage() && getCurrentPage() != null && StringUtils.isNotBlank(getCurrentPage().getWrapperElementClass())) {
            return "body_" + getCurrentPage().getWrapperElementClass();
        }

        return "";
    }

    /**
     * Getter for jsf.
     *
     * @return CMSPAGES_FILTER
     */
    public String getCmsPagesFilter() {
        return CMSPAGES_FILTER;
    }

    /**
     * getNavigationMenuItems.
     *
     * @return a list of top-level CMS navigation items visible to the current user for the active theme
     */
    public List<CMSNavigationItem> getNavigationMenuItems() {
        // logger.trace("getNavigationMenuItems");
        if (navigationMenuItems == null) {
            try {
                String mainTheme = DataManager.getInstance().getConfiguration().getTheme();
                String currentTheme = getCurrentCmsPageIfLoaded()
                        .map(CMSPage::getSubTheme)
                        .orElse(BeanUtils.getNavigationHelper().getThemeOrSubtheme());
                navigationMenuItems = DataManager.getInstance()
                        .getDao()
                        .getAllTopCMSNavigationItems()
                        .stream()
                        .filter(item -> item.checkAccess(BeanUtils.getRequest()))
                        .filter(item -> (StringUtils.isBlank(item.getAssociatedTheme()) && mainTheme.equalsIgnoreCase(currentTheme))
                                || currentTheme.equalsIgnoreCase(item.getAssociatedTheme()))
                        .toList();
                if (navigationMenuItems.isEmpty()) {
                    navigationMenuItems = DataManager.getInstance()
                            .getDao()
                            .getAllTopCMSNavigationItems()
                            .stream()
                            .filter(item -> item.checkAccess(BeanUtils.getRequest()))
                            .filter(item -> (StringUtils.isBlank(item.getAssociatedTheme())
                                    || item.getAssociatedTheme().equalsIgnoreCase(mainTheme)))
                            .toList();
                }
                logger.info("returning {} items", navigationMenuItems.size());
            } catch (DAOException e) {
                navigationMenuItems = Collections.emptyList();
            }
        }

        return navigationMenuItems;
    }

    /**
     * Resets visible navigation menu items (e.g. when logging in/out).
     */
    public void resetNavigationMenuItems() {
        this.navigationMenuItems = null;
    }

    public Collection<Sorting> getSortingModes() {
        return Arrays.asList(Sorting.values());
    }

    public String getCurrentPageUrl() {
        if (this.currentPage != null) {
            return "cmsPage.xhtml";
        }
        return "";
    }

    public CMSTemplateManager getTemplateManager() {
        return templateManager;
    }

    public Collection<HitListView> getPossibleHitListViews() {
        return EnumSet.allOf(HitListView.class);
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public boolean isUserHasAccess(CMSPage page) {
        if (page != null) {
            // logger.trace("isUserHasAccess: {}", page.getId()); //NOSONAR Debug
            try {
                return AccessConditionUtils.checkAccessPermissionForCmsPage(BeanUtils.getRequest(), page).isGranted();
            } catch (IndexUnreachableException | DAOException | PresentationException e) {
                logger.error(e.getMessage());
            }
        }

        return false;
    }
}