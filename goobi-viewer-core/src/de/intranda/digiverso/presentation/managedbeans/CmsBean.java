/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.dao.IDAO;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataProvider;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataProvider.SortOrder;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataSource;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem.CMSContentItemType;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.CMSNavigationItem;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSPageLanguageVersion;
import de.intranda.digiverso.presentation.model.cms.CMSPageLanguageVersion.CMSPageStatus;
import de.intranda.digiverso.presentation.model.cms.CMSPageTemplate;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarManager;
import de.intranda.digiverso.presentation.model.cms.CMSStaticPage;
import de.intranda.digiverso.presentation.model.cms.CMSTemplateManager;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.search.SearchHit;
import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.PageType;

/**
 * CMS functions.
 */
@ManagedBean
@SessionScoped
public class CmsBean {

    private static final Logger logger = LoggerFactory.getLogger(CmsBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @ManagedProperty("#{navigationHelper}")
    private NavigationHelper navigationHelper;
    @ManagedProperty("#{searchBean}")
    private SearchBean searchBean;

    private TableDataProvider<CMSPage> lazyModelPages;
    /** The page open for editing */
    private CMSPage selectedPage;
    /** The page currently open for viewing */
    private CMSPage currentPage;
    private Locale selectedLocale;
    private Locale selectedMediaLocale;
    private CMSMediaItem selectedMediaItem;
    private String selectedClassification;
    private CMSSidebarElement selectedSidebarElement;
    private boolean displaySidebarEditor = false;
    private List<CMSPage> createdPages;
    private int nestedPagesCount = 0;
    private boolean editMode = false;
    private Map<String, CollectionView> collections = new HashMap<>();
    private List<CMSStaticPage> staticPages = null;

    @PostConstruct
    public void init() {
        createdPages = null;
        if (lazyModelPages == null) {
            lazyModelPages = new TableDataProvider<>(new TableDataSource<CMSPage>() {

                @Override
                public List<CMSPage> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    List<CMSPage> list;
                    try {
                        if (sortField == null) {
                            sortField = "id";
                        }
                        return loadCreatedPages(first, first + pageSize);
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords() {
                    try {
                        return DataManager.getInstance().getDao().getCMSPageCount(lazyModelPages.getFiltersAsMap());
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                        return 0;
                    }
                }
            });
            lazyModelPages.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        }
        selectedLocale = getDefaultLocale();
    }

    /**
     * Required setter for ManagedProperty injection
     * 
     * @param navigationHelper navigationHelper searchBean to set
     */
    public void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
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
     * @return
     */
    public static List<Locale> getAllLocales() {
        List<Locale> list = new LinkedList<>();
        list.add(getDefaultLocaleStatic());
        Iterator<Locale> iter = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();
        while (iter.hasNext()) {
            Locale locale = iter.next();
            if (!list.contains(locale)) {
                list.add(locale);
            }
        }
        return list;
    }

    public Locale getDefaultLocale() {
        return getDefaultLocaleStatic();
    }

    public static Locale getDefaultLocaleStatic() {
        Locale defaultLocale = null;
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getApplication() != null) {
            defaultLocale = FacesContext.getCurrentInstance().getApplication().getDefaultLocale();
        }
        if (defaultLocale == null) {
            defaultLocale = Locale.ENGLISH;
        }
        return defaultLocale;
    }

    public static Locale getCurrentLocale() {
        if (FacesContext.getCurrentInstance() != null && FacesContext.getCurrentInstance().getViewRoot() != null) {
            return FacesContext.getCurrentInstance().getViewRoot().getLocale();
        }

        return Locale.GERMAN;
    }

    public List<CMSPageLanguageVersion.CMSPageStatus> getLanguageStatusValues() {
        return Arrays.asList(CMSPageLanguageVersion.CMSPageStatus.values());
    }

    public void loadTemplates() {
        CMSTemplateManager.getInstance().updateTemplates();
    }

    public List<CMSPageTemplate> getTemplates() {
        return new ArrayList<>(CMSTemplateManager.getInstance().getTemplates());
    }

    public List<CMSPage> loadCreatedPages() throws DAOException {
        logger.debug("Loading created cms-pages from database");
        createdPages = DataManager.getInstance().getDao().getAllCMSPages();
        Iterator<CMSPage> pages = createdPages.iterator();

        while (pages.hasNext()) {
            CMSPage page = pages.next();
            CMSPageTemplate template = CMSTemplateManager.getInstance().getTemplate(page.getTemplateId());
            if (template == null) {
                //remove pages with no template files
                pages.remove();
            } else {
                //check if this pages is used as static page
                for (CMSStaticPage staticPage : getStaticPages()) {
                    if (staticPage.getCmsPage() != null && staticPage.getCmsPage().getId().equals(page.getId())) {
                        staticPage.setCmsPage(page);
                    }
                }
            }
        }
        return createdPages;
    }

    public List<CMSPage> loadCreatedPages(int from, int to) throws DAOException {
        logger.debug("Loading created cms-pages from database");
        createdPages = DataManager.getInstance().getDao().getCMSPages(from, to - from, null, false, null);
        Iterator<CMSPage> pages = createdPages.iterator();

        while (pages.hasNext()) {
            CMSPage page = pages.next();
            CMSPageTemplate template = CMSTemplateManager.getInstance().getTemplate(page.getTemplateId());

            if (!isPageValid(page, template)) {
                pages.remove();
            } else {
                //check if this pages is used as static page
                for (CMSStaticPage staticPage : getStaticPages()) {
                    if (staticPage.getCmsPage() != null && staticPage.getCmsPage().getId().equals(page.getId())) {
                        staticPage.setCmsPage(page);
                    }
                }
            }
        }
        return createdPages;
    }

    /**
     * @param page
     * @param template
     * @return
     */
    private boolean isPageValid(CMSPage page, CMSPageTemplate template) {
        boolean pageValid = true;
        if (template == null) {
            //remove pages with no template files
            pageValid = false;
        } else {
            //remove page with content items that don't match the template's content items
            for (CMSContentItem templateItem : template.getContentItems()) {
                if (page.getContentItem(templateItem.getItemId()) == null) {
                    logger.warn("Found template item that doesn't exists in page");
                    pageValid = false;
                }
            }
        }
        return pageValid;
    }

    public List<CMSPage> getCreatedPages() throws DAOException {
        if (createdPages == null) {
            return loadCreatedPages();
        }
        return createdPages;
    }

    public List<CMSPage> getDisplayedPages() throws DAOException {
        return lazyModelPages.getPaginatorList();
    }

    public List<CMSPage> getCreatedPages(int from, int to) throws DAOException {
        if (createdPages == null) {
            return loadCreatedPages();
        }
        return createdPages.subList(from, to);
    }

    public TableDataProvider<CMSPage> getLazyModelPages() {
        return lazyModelPages;
    }

    public CMSPage createNewPage(CMSPageTemplate template) {
        List<Locale> locales = getAllLocales();
        CMSPage page = template.createNewPage(locales);
        // page.setId(System.currentTimeMillis());
        page.setDateCreated(new Date());
        return page;
    }

    /**
     * Current page URL getter for PrettyFaces. Page must be either published or the current user must be an admin.
     *
     * @return
     */
    public String getCurrentPageUrl() {
        logger.trace("getCurrentPageUrl");
        if (currentPage != null && (currentPage.isPublished() || (getUserBean() != null && getUserBean().getUser() != null && getUserBean().getUser()
                .isSuperuser()))) {
            String url = getTemplateUrl(currentPage.getTemplateId(), false);
            return url;
        }
        return "pretty:index";
    }

    // public String getPageUrl(CMSPage page) {
    // try {
    // return new
    // StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/cms/")
    // .append(page.getId()).append('/').toString();
    // } catch (NullPointerException e) {
    // return "pretty:index";
    // }
    // }

    /**
     * Returns the URL to the CMS template of the given page. This URL will only resolve if the page has been published or the current user is
     * superuser.
     *
     * @param page
     * @return
     */
    public String getPageUrl(Long pageId) {
        try {
            CMSPage page = getPage(pageId);
            return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/").append(page.getRelativeUrlPath(true)).toString();
        } catch (NullPointerException e) {
            return "pretty:index";
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            return "pretty:index";
        }
    }

    /**
     * Returns the preview URL to the CMS template of the given page. This URL call will save the current page before opening it.
     *
     * @param page
     * @param pretty
     * @return
     */
    public String getPagePreviewUrl(CMSPage page, boolean pretty) {
        if (pretty) {
            try {
                return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/cmspreview/").append(page.getId()).append(
                        '/').toString();
            } catch (NullPointerException e) {
                return "pretty:index";
            }
        }
        return getTemplateUrl(page.getTemplateId(), false);
    }

    /**
     * @param id
     * @return
     * @throws DAOException
     */
    private static CMSPage findPage(String id) throws DAOException {
        CMSPage page = null;
        if (id != null) {
            try {
                logger.trace("Get cmsPage from database with pageId = " + id);
                page = DataManager.getInstance().getDao().getCMSPage(Long.valueOf(id));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse page number: {}", e.getMessage());
            }
            if (page != null) {
                logger.trace("Found cmsPage " + page.getMenuTitle());
                // DataManager.getInstance().getDao().updateCMSPage(page);
            }
        }
        return page;
    }

    /**
     * @param templateId
     * @return
     */
    private static String getTemplateUrl(String templateId, boolean redirect) {
        logger.trace("Getting url for template " + templateId);
        String templateUrl = CMSTemplateManager.getInstance().getTemplateViewUrl(templateId);
        logger.trace("Found template url " + templateUrl);
        if (redirect) {
            logger.trace("Redirecting to url " + templateUrl);
            FacesContext.getCurrentInstance().getExternalContext().getFlash().setRedirect(true);
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(templateUrl);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return templateUrl;
    }

    public String getIconUrlByTemplateId(String templateId) {
        String iconUrl = CMSTemplateManager.getInstance().getTemplateIconUrl(templateId);
        return iconUrl;
    }

    public List<CMSPage> getNestedPages(CMSContentItem item) throws DAOException {
        String classification = item.getPageClassification();
        int size = item.getElementsPerPage();
        int offset = item.getListOffset();
        List<CMSPage> nestedPages = new ArrayList<>();
        int counter = 0;
        if (!StringUtils.isEmpty(classification)) {
            for (CMSPage cmsPage : getCreatedPages()) {
                if (cmsPage.isPublished() && cmsPage.getClassifications().contains(classification)) {
                    counter++;
                    if (counter > offset && counter <= size + offset) {
                        nestedPages.add(cmsPage);
                    }
                }
            }
        }
        setNestedPagesCount((int) Math.ceil(counter / (double) size));
        return nestedPages;
    }

    public List<CMSSidebarElement> getSidebarElements(boolean isCMSPage) {
        if (isCMSPage && getCurrentPage() != null && !getCurrentPage().isUseDefaultSidebar()) {
            return getCurrentPage().getSidebarElements();
        }
        return CMSSidebarManager.getDefaultSidebarElements();
    }

    /**
     * Adds the current page to the database, if it doesn't exist or updates it otherwise
     *
     * @throws DAOException
     *
     */
    public void saveSelectedPage() throws DAOException {
        logger.trace("saveSelectedPage");
        if (getUserBean() == null || getUserBean().getUser() == null || !getUserBean().getUser().isSuperuser()) {
            // Only superusers may save
            return;
        }
        // resetImageDisplay();
        if (selectedPage != null) {
            // Validate
            selectedPage.saveSidebarElements();
            validatePage(selectedPage, getDefaultLocale().getLanguage());
            selectedPage.resetItemData();
            // // Delete navbar items for this page
            // if (!selectedPage.isPublished()) {
            // for (CMSNavigationItem item : getNavigationMenuItems()) {
            // if (selectedPage.equals(item.getCmsPage())) {
            // if
            // (DataManager.getInstance().getDao().deleteCMSNavigationItem(item))
            // {
            // logger.info("Navigation item {} for page {} has been removed.",
            // item.getId(),
            // selectedPage.getId());
            // Messages.info("cms_pageSaveNavItemRemoved");
            // }
            // }
            // }
            // }

            // Save
            boolean success = false;
            selectedPage.setDateUpdated(new Date());
            if (selectedPage.getId() != null) {
                success = DataManager.getInstance().getDao().updateCMSPage(selectedPage);
            } else {
                success = DataManager.getInstance().getDao().addCMSPage(selectedPage);
            }
            if (success) {
                Messages.info("cms_pageSaveSuccess");
                selectedPage = DataManager.getInstance().getDao().getCMSPage(selectedPage.getId());
                loadCreatedPages();
            } else {
                Messages.error("cms_pageSaveFailure");
            }
            resetCollectionsForPage(selectedPage.getId().toString());
        }
    }

    /**
     * @param id
     */
    private void resetCollectionsForPage(String pageId) {
        List<String> collectionKeys = new ArrayList<>(collections.keySet());
        for (String id : collectionKeys) {
            if (id.startsWith(pageId + "_")) {
                collections.remove(id);
            }
        }

    }

    public static boolean validateSidebarElement(CMSSidebarElement element) {
        if (element != null && !element.isValid()) {
            String msg = Helper.getTranslation("cms_validationWarningHtmlInvalid", null);
            Messages.error(msg.replace("{0}", element.getType()).replace("{1}", CMSSidebarManager.getInstance().getAllowedHtmlTagsForDisplay()));
            return false;
        }
        return true;
    }

    /**
     *
     * @param page
     * @param defaultLanguage
     */
    protected static void validatePage(CMSPage page, String defaultLanguage) {

        if (!page.isUseDefaultSidebar()) {
            for (CMSSidebarElement element : page.getSidebarElements()) {
                if (!validateSidebarElement(element)) {
                    page.setPublished(false);
                }
            }
        }

        for (CMSPageLanguageVersion languageVersion : page.getLanguageVersions()) {
            boolean languageIncomplete = false;
            if (StringUtils.isBlank(languageVersion.getTitle()) || StringUtils.isBlank(languageVersion.getMenuTitle())) {
                // Messages.warn("cmsValidationErrorTitle");
                languageIncomplete = true;
            }
            for (CMSContentItem item : languageVersion.getContentItems()) {
                if (item.isMandatory()) {
                    switch (item.getType()) {
                        case TEXT:
                        case HTML:
                            if (StringUtils.isBlank(item.getHtmlFragment())) {
                                // Messages.warn("cmsValidationErrorHtml");
                                languageIncomplete = true;
                            }
                            break;
                        case MEDIA:
                            if (item.getMediaItem() == null) {
                                // Messages.warn("cmsValidationErrorMedia");
                                languageIncomplete = true;
                            }
                            break;
                        case SOLRQUERY:
                            if (StringUtils.isBlank(item.getHtmlFragment())) {
                                // Messages.warn("cmsValidationErrorSolr");
                                languageIncomplete = true;
                            }
                            break;
                        case PAGELIST:
                            if (StringUtils.isBlank(item.getPageClassification())) {
                                languageIncomplete = true;
                            }
                            break;
                        default:
                            logger.warn("Validation for page type {} is not yet implemented.", item.getType());
                            break;
                    }
                }
            }
            if (languageIncomplete) {
                // Set each incomplete language version to WIP if it's set to
                // FINISHED
                if (CMSPageStatus.FINISHED.equals(languageVersion.getStatus())) {
                    languageVersion.setStatus(CMSPageStatus.WIP);
                    String msg = Helper.getTranslation("cms_validationWarningLanguageVersionIncomplete", null);
                    Messages.error(msg.replace("{0}", languageVersion.getLanguage()));
                }
                // Remove the finished flag on the page if the default language
                // page is incomplete
                if (defaultLanguage.equals(languageVersion.getLanguage()) && page.isPublished()) {
                    page.setPublished(false);
                    String msg = Helper.getTranslation("cms_validationWarningPageIncomplete", null);
                    Messages.error(msg.replace("{0}", languageVersion.getLanguage()));
                }
            } else if (defaultLanguage.equals(languageVersion.getLanguage()) && page.isPublished()) {
                // Set default language to FINISHED because it cannot be done
                // manually
                languageVersion.setStatus(CMSPageStatus.FINISHED);
            }
        }
    }

    /**
     * Same as saveCurrentPage, but also set published=true for currentPage
     *
     * @throws DAOException
     *
     */
    public void publishSelectedPage() throws DAOException {
        if (getSelectedPage() != null) {
            synchronized (selectedPage) {
                getSelectedPage().setPublished(true);
                saveSelectedPage();
            }
        }
    }

    public boolean isLinkedFromNavBar(CMSPage page) throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        if (dao != null && page != null) {
            List<CMSNavigationItem> relatedItems = dao.getRelatedNavItem(page);
            return relatedItems != null && !relatedItems.isEmpty();
        }
        return false;
    }

    public List<CMSNavigationItem> getNavigationMenuItems() {
        try {
            return DataManager.getInstance().getDao().getAllTopCMSNavigationItems();
        } catch (DAOException e) {
            return Collections.emptyList();
        }
    }

    public String deleteSelectedPage() throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        if (dao != null && selectedPage != null && selectedPage.getId() != null) {
            logger.debug("deleting page " + selectedPage);
            if (dao.deleteCMSPage(selectedPage)) {
                selectedPage = null;
                loadCreatedPages();
                Messages.info("cms_deletePage_success");
            } else {
                logger.error("Failed to delete page");
                Messages.error("cms_deletePage_failure");
            }
        }

        return "cmsOverview";
    }

    public void deletePage(CMSPage page) throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        if (dao != null && page != null && page.getId() != null) {
            if (dao.deleteCMSPage(page)) {
                loadCreatedPages();
                Messages.info("cms_deletePage_success");
            } else {
                logger.error("Failed to delete page");
                Messages.error("cms_deletePage_failure");
            }
        }
        selectedPage = null;
    }

    public CMSPage getPage(CMSPage page) {
        return page == null ? currentPage : page;
    }
    
    public CMSPage getPage(Long pageId) throws DAOException {
        for (CMSPage cmsPage : getCreatedPages()) {
            if(cmsPage.getId().equals(pageId)) {
                return cmsPage;
            }
        }
        return null;
    }

    public CMSPage getSelectedPage() {
        return selectedPage;
    }

    public void setSelectedPage(CMSPage currentPage) {
        this.selectedPage = currentPage;
        logger.debug("Selected page " + currentPage);
        // resetImageDisplay();
    }

    public CMSPage getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(CMSPage currentPage) {
        this.currentPage = currentPage;
        if (currentPage != null) {
            this.currentPage.setListPage(1);
            logger.trace("Set current cms page to " + this.currentPage.getMenuTitle());
        }
        // if
        // (DataManager.getInstance().getDao().getCMSPage(currentPage.getId())
        // != null) {
        // this.currentPage =
        // DataManager.getInstance().getDao().getCMSPage(currentPage.getId());
        // }
        // }
    }

    public void updatePage() {
        if (getSelectedPage() != null) {
            logger.trace("Setting current page to {}", getSelectedPage().getTitle());
            setCurrentPage(getSelectedPage());
        }
    }

    public String getCurrentPageId() {
        if (currentPage != null) {
            return String.valueOf(currentPage.getId());
        }

        return "0";
    }

    /**
     * Page ID setter for PrettyFaces.
     *
     * @param id
     * @throws DAOException
     */
    public void setCurrentPageId(String id) throws DAOException {
        logger.trace("setCurrentPageId: {}", id);
        setCurrentPage(findPage(id));
    }

    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    public void setSelectedLocale(Locale selectedLocale) {
        if (this.selectedLocale == null) {
            // currently in sidebar view: save before leaving
            selectedPage.saveSidebarElements();
        }
        this.selectedLocale = selectedLocale;

    }

    public List<String> getClassifications() {
        List<String> ret = new ArrayList<>();
        ret.add("");
        ret.addAll(DataManager.getInstance().getConfiguration().getCmsClassifications());

        return ret;
    }

    public boolean isDisplaySidebarEditor() {
        return displaySidebarEditor;
    }

    public void setDisplaySidebarEditor(boolean displaySidebarEditor) {
        this.displaySidebarEditor = displaySidebarEditor;
    }

    public String getSelectedClassification() {
        return selectedClassification;
    }

    public void setSelectedClassification(String selectedClassification) {
        this.selectedClassification = selectedClassification;
    }

    public CMSMediaItem getSelectedMediaItem() {
        return selectedMediaItem;
    }

    public void setSelectedMediaItem(CMSMediaItem selectedMediaItem) {
        // logger.trace("Set media item to " + selectedMediaItem.getFileName());
        this.selectedMediaItem = selectedMediaItem;
    }

    public Locale getSelectedMediaLocale() {
        if (selectedMediaLocale == null) {
            selectedMediaLocale = getSelectedLocale();
        }
        return selectedMediaLocale;
    }

    public void setSelectedMediaLocale(Locale selectedMediaLocale) {
        this.selectedMediaLocale = selectedMediaLocale;
    }

    /**
     * Action method called when a CMS page is opened. The exact action depends on the page and content item type.
     * 
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String cmsContextAction() throws PresentationException, IndexUnreachableException, DAOException {
        if (currentPage != null) {
            List<CMSContentItem> contentItems = currentPage.getGlobalContentItems();
            for (CMSContentItem item : contentItems) {
                if (item != null && CMSContentItemType.SOLRQUERY.equals(item.getType())) {
                    return searchAction(item);
                }
            }
        }

        return "";
    }

    @Deprecated
    public List<SearchHit> getQueryResults(CMSContentItem item) throws IndexUnreachableException, PresentationException, DAOException {
        return getQueryResults();
    }

    /**
     *
     * @param item
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public List<SearchHit> getQueryResults() throws IndexUnreachableException, PresentationException, DAOException {
        if (searchBean != null) {
            Search search = searchBean.getCurrentSearch();
            if (search != null) {
                return searchBean.getCurrentSearch().getHits();
            }
        }

        return Collections.emptyList();
    }

    /**
     * Uses SearchBean to execute a search.
     * 
     * @param item
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public String searchAction(CMSContentItem item) throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("searchAction");
        if (searchBean != null && item != null && item.getSolrQuery() != null) {
            searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
            searchBean.setHitsPerPage(item.getElementsPerPage());
            searchBean.setExactSearchStringResetGui(item.getSolrQuery());
            searchBean.setCurrentPage(item.getListPage());
            if (item.getSolrSortFields() != null) {
                searchBean.setSortString(item.getSolrSortFields());
            }
            searchBean.newSearch();
        } else {
            logger.error("cannot search, SearchBean null: {}, item null: {}", searchBean == null, item == null);
        }

        return "";
    }

    /**
     * 
     * @param facetQuery
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String removeHierarchicalFacetAction(String facetQuery) throws PresentationException, IndexUnreachableException, DAOException {
        if (searchBean != null) {
            searchBean.removeHierarchicalFacetAction(facetQuery);
        }

        return cmsContextAction();
    }

    /**
     * 
     * @param facetQuery
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String removeFacetAction(String facetQuery) throws PresentationException, IndexUnreachableException, DAOException {
        if (searchBean != null) {
            searchBean.removeFacetAction(facetQuery);
        }

        return cmsContextAction();
    }

    /**
     * Calcs number of paginator pages for the query result.
     * 
     * @param item
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public long getQueryResultCount(CMSContentItem item) throws PresentationException, IndexUnreachableException {
        if (item == null) {
            return 0;
        }
        //		String query = item.getSolrQuery();
        if (searchBean != null) {
            return searchBean.getLastPage();
            //			QueryResponse resp = DataManager.getInstance().getSearchIndex().search(query, 0, 0, null, getFacetFields(), null);
            //			if (resp != null) {
            //				long hitsCount = resp.getResults().getNumFound();
            //				return (long) Math.ceil(hitsCount / (double) item.getElementsPerPage());
            //			}
        }

        return 0;
    }

    public List<String> getFieldNames(SolrDocument solrDoc) {
        if (solrDoc != null) {
            return new ArrayList<>(solrDoc.getFieldNames());
        }
        return Collections.emptyList();
    }

    public int getNestedPagesCount() {
        return nestedPagesCount;
    }

    public void setNestedPagesCount(int nestedPages) {
        this.nestedPagesCount = nestedPages;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public CMSSidebarElement getSelectedSidebarElement() {
        if (selectedSidebarElement == null) {
            createSidebarElement();
        }
        return selectedSidebarElement;
    }

    public void setSelectedSidebarElement(CMSSidebarElement selectedSidebarElement) {
        this.selectedSidebarElement = selectedSidebarElement;
    }

    public void createSidebarElement() {
        this.selectedSidebarElement = new CMSSidebarElement();
        this.selectedSidebarElement.setType("");
        this.selectedSidebarElement.setHtml("");
        // if(getSelectedPage() != null) {
        // getSelectedPage().saveSidebarElements();
        // }
    }

    public void saveSidebarElement() {
        getSelectedPage().saveSidebarElements();
        if (this.selectedSidebarElement == null || this.selectedPage == null) {
            logger.error("Cannot write sidebar-element " + this.selectedSidebarElement + " to page " + this.selectedPage);
        } else {
            validateSidebarElement(this.selectedSidebarElement);
            this.selectedPage.addSidebarElement(this.selectedSidebarElement);
        }
    }

    public void resetImageDisplay() {
        // logger.trace("reset Image display");
        if (getSelectedPage() != null) {
            getSelectedPage().resetEditorItemVisibility();
        }
    }

    /**
     * TODO Is this necessary?
     * 
     * @return
     */
    public UserBean getUserBean() {
        return BeanUtils.getUserBean();
    }

    public CollectionView getCollection(String id, CMSPage page) throws PresentationException, IndexUnreachableException {
        String myId = page.getId() + "_" + id;
        CollectionView collection = collections.get(myId);
        if (collection == null) {
            collection = page.getContentItem(id).initializeCollection();
            collections.put(myId, collection);
        }
        return collection;
    }

    public static List<String> getLuceneFields() {
        return getLuceneFields(false);
    }

    public static List<String> getLuceneFields(boolean includeUntokenized) {
        List<String> constants;
        try {
            constants = DataManager.getInstance().getSearchIndex().getAllFieldNames();
            Iterator<String> iterator = constants.iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                if (name.startsWith("_") || name.startsWith("FACET_") || name.startsWith("NORM_") || (!includeUntokenized && name.endsWith(
                        "_UNTOKENIZED"))) {
                    iterator.remove();
                }
            }
            Collections.sort(constants);
            return constants;
        } catch (SolrServerException | IOException e) {
            logger.error("Error retrieving solr fields", e);
            return Collections.singletonList("");
        }

    }

    /**
     * @return
     * @throws DAOException
     */
    public List<CMSStaticPage> getStaticPages() throws DAOException {
        if (this.staticPages == null) {
            this.staticPages = createStaticPageList();
        }
        return this.staticPages;
    }

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
     * @return
     * @throws DAOException
     */
    private static List<CMSStaticPage> createStaticPageList() throws DAOException {
        List<CMSStaticPage> pages = new ArrayList<>();
        for (PageType pageType : PageType.getTypesHandledByCms()) {
            CMSPage cmsPage = DataManager.getInstance().getDao().getCmsPageForStaticPage(pageType.getName());
            CMSStaticPage page = new CMSStaticPage(pageType.getName());
            if (cmsPage != null) {
                page.setCmsPage(cmsPage);
                page.setUseCmsPage(true);
            }
            pages.add(page);
        }
        return pages;
    }

    /**
     * @return A list of all cmsPages not yet registered to a static page
     * @throws DAOException
     */
    public List<CMSPage> getAvailableCmsPages(CMSStaticPage page) throws DAOException {
        List<CMSPage> allPages = new ArrayList<>();
        for (CMSPage cmsPage : getCreatedPages()) {
            if (cmsPage.isPublished()) {
                allPages.add(cmsPage);
            }
        }
        for (CMSStaticPage staticPage : getStaticPages()) {
            if (!staticPage.equals(page) && staticPage.isHasCmsPage()) {
                allPages.remove(staticPage.getCmsPage());
            }
        }
        return allPages;
    }

    public void saveCMSPages() throws DAOException {
        for (CMSPage cmsPage : getCreatedPages()) {
            if (!DataManager.getInstance().getDao().updateCMSPage(cmsPage)) {
                Messages.error("cms_errorSavingStaticPages");
                return;
            }
        }
        Messages.info("cms_staticPagesSaved");
    }

    public void forwardToCMSPage() throws IOException, DAOException {
        String pageName = navigationHelper.getCurrentPage();
        CMSStaticPage page = getStaticPageForPageType(PageType.getByName(pageName));
        if (page != null && page.isHasCmsPage() && page.getCmsPage().isPublished()) {
            forwardToCMSPage(page.getCmsPage());
        }
    }

    /**
     * @param page
     * @throws IOException
     */
    public void forwardToCMSPage(CMSPage page) throws IOException {
        setCurrentPage(page);
        String path = CMSTemplateManager.getInstance().getTemplateViewUrl(page.getTemplate());
        if (StringUtils.isNotBlank(path)) {
            logger.debug("Forwarding to " + path);
            FacesContext context = getFacesContext();
            context.getExternalContext().dispatch(path);
            context.responseComplete();
        }
    }

    /**
     * @return
     */
    protected FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    /**
     * @param pageType
     * @return The CMSStaticPage for the given pageType if it exists, i.e. if the pageType has PageTypeHandling.cms
     * @throws DAOException
     */
    private CMSStaticPage getStaticPageForPageType(PageType pageType) throws DAOException {
        for (CMSStaticPage staticPage : getStaticPages()) {
            if (staticPage.getPageName().equals(pageType.getName())) {
                return staticPage;
            }
        }
        return null;
    }
}
