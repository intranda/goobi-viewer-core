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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.IOException;
import java.io.Serializable;
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
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.controller.imaging.ThumbnailHandler;
import de.intranda.digiverso.presentation.dao.IDAO;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.tabledata.PersistentTableDataProvider;
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
import de.intranda.digiverso.presentation.model.cms.PageValidityStatus;
import de.intranda.digiverso.presentation.model.cms.SelectableNavigationItem;
import de.intranda.digiverso.presentation.model.cms.itemfunctionality.SearchFunctionality;
import de.intranda.digiverso.presentation.model.glossary.Glossary;
import de.intranda.digiverso.presentation.model.glossary.GlossaryManager;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.search.SearchHit;
import de.intranda.digiverso.presentation.model.urlresolution.ViewHistory;
import de.intranda.digiverso.presentation.model.urlresolution.ViewerPath;
import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.PageType;

/**
 * CMS functions.
 */
@Named
@SessionScoped
public class CmsBean implements Serializable {

    private static final long serialVersionUID = -2021732230593473827L;

    private static final Logger logger = LoggerFactory.getLogger(CmsBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private CmsNavigationBean cmsNavigationBean;
    @Inject
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
    private int nestedPagesCount = 0;
    private boolean editMode = false;
    private Map<String, CollectionView> collections = new HashMap<>();
    private List<CMSStaticPage> staticPages = null;

    @PostConstruct
    public void init() {
        if (lazyModelPages == null) {
            lazyModelPages = new TableDataProvider<>(new TableDataSource<CMSPage>() {

                private Optional<Long> numCreatedPages = Optional.empty();

                @Override
                public List<CMSPage> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    try {
                        if (StringUtils.isBlank(sortField)) {
                            sortField = "id";
                        }
                        List<CMSPage> pages =
                                DataManager.getInstance().getDao().getCMSPages(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                        pages.forEach(page -> {
                            PageValidityStatus validityStatus = isPageValid(page);
                            page.setValidityStatus(validityStatus);
                            if (validityStatus.isValid()) {
                                page.getSidebarElements().forEach(element -> element.deSerialize());
                            }
                        });
                        return pages;
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (!numCreatedPages.isPresent()) {
                        try {
                            numCreatedPages = Optional.ofNullable(DataManager.getInstance().getDao().getCMSPageCount(filters));
                        } catch (DAOException e) {
                            logger.error("Unable to retrieve total number of cms pages", e);
                        }
                    }
                    return numCreatedPages.orElse(0l);
                }

                @Override
                public void resetTotalNumberOfRecords() {
                    numCreatedPages = Optional.empty();
                }
            });
            lazyModelPages.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
            lazyModelPages.addFilter("CMSPageLanguageVersion", "title_menuTitle");
            lazyModelPages.addFilter("classifications", "classification");
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

    /**
     * @deprecated This method only remains to avoid pages failing to load which still call this method
     */
    @Deprecated
    public void forwardToCMSPage() {
        //do nothing
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
        logger.trace("loadTemplates");
        CMSTemplateManager.getInstance().updateTemplates(CMSTemplateManager.getInstance().getCoreFolderPath(),
                CMSTemplateManager.getInstance().getThemeFolderPath());
    }

    public List<CMSPageTemplate> getTemplates() {
        try {
            List<CMSPageTemplate> list = CMSTemplateManager.getInstance()
                    .getTemplates()
                    .stream()
                    .sorted((t1, t2) -> t1.getTemplateFileName().compareTo(t2.getTemplateFileName()))
                    .collect(Collectors.toList());
            return list;
        } catch (IllegalStateException e) {
            logger.warn("Error loading templates", e);
            return Collections.emptyList();
        }
    }

    /**
     * @param page
     * @param template
     * @return
     */
    private static PageValidityStatus isPageValid(CMSPage page) {
        if (page.getTemplate() == null) {
            //remove pages with no template files
            return PageValidityStatus.INVALID_NO_TEMPLATE;
        }
        //remove page with content items that don't match the template's content items
        for (CMSContentItem templateItem : page.getTemplate().getContentItems()) {
            if (!page.hasContentItem(templateItem.getItemId())) {
                page.addContentItem(new CMSContentItem(templateItem, null));
                //                    logger.warn("Found template item that doesn't exists in page");
                //                    pageValid = false;
            }
        }
        return PageValidityStatus.VALID;
    }

    public List<CMSPage> getDisplayedPages() {
        return lazyModelPages.getPaginatorList();
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
        if (currentPage != null && (currentPage.isPublished()
                || (getUserBean() != null && getUserBean().getUser() != null && getUserBean().getUser().isSuperuser()))) {
            String url = getTemplateUrl(currentPage.getTemplateId(), false);
            return url;
        }
        return "pretty:index";
    }

    /**
     * Returns the URL to the CMS template of the given page. This URL will only resolve if the page has been published or the current user is
     * superuser.
     *
     * @param page
     * @return
     */
    public String getPageUrl(Long pageId) {
        return getPageUrl(pageId, true);
    }

    public String getPageUrl(Long pageId, boolean pretty) {
        try {
            CMSPage page = getPage(pageId);
            return getUrl(page, pretty);
        } catch (DAOException e) {
            logger.error(e.toString(), e);
            return "pretty:index";
        }
    }

    public String getUrl(CMSPage page) {
        return getUrl(page, true);
    }

    public String getUrl(CMSPage page, boolean pretty) {
        try {
            return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/")
                    .append(page.getRelativeUrlPath(pretty))
                    .toString();
        } catch (NullPointerException e) {
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
                return new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext()).append("/cmspreview/")
                        .append(page.getId())
                        .append('/')
                        .toString();
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
    private CMSPage findPage(String id) throws DAOException {
        CMSPage page = null;
        if (id != null) {
            try {
                logger.trace("Get cmsPage from database with pageId = " + id);
                page = getCMSPage(Long.valueOf(id));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse page number: {}", e.getMessage());
            }
            if (page != null) {
                logger.trace("Found cmsPage " + page.getTitle());
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
        int size = item.getElementsPerPage();
        int offset = item.getListOffset();
        List<CMSPage> nestedPages = new ArrayList<>();
        int counter = 0;
        List<CMSPage> cmsPages = getAllCMSPages();
        for (String  classification : item.getPageClassification()) {            
            if (!StringUtils.isEmpty(classification)) {
                for (CMSPage cmsPage : cmsPages) {
                    if (cmsPage.isPublished() && cmsPage.getClassifications().contains(classification)) {
                        counter++;
                        if (counter > offset && counter <= size + offset) {
                            nestedPages.add(cmsPage);
                        }
                    }
                }
            }
        }
        setNestedPagesCount((int) Math.ceil(counter / (double) size));
        return nestedPages;
    }

    /**
     * @return
     * @throws DAOException
     */
    public List<CMSPage> getAllCMSPages() throws DAOException {
        List<CMSPage> pages =  DataManager.getInstance().getDao().getAllCMSPages();
        pages.forEach(page -> {
            PageValidityStatus validityStatus = isPageValid(page);
            page.setValidityStatus(validityStatus);
            if (validityStatus.isValid()) {
                page.getSidebarElements().forEach(element -> element.deSerialize());
            }
        });
        return pages;
    }

    /**
     * @param pageId
     * @return
     * @throws DAOException
     */
    public CMSPage getCMSPage(Long pageId) throws DAOException {
          Optional<CMSPage> page = Optional.ofNullable(DataManager.getInstance().getDao().getCMSPage(pageId));
//        Optional<CMSPage> page = getAllCMSPages().stream().filter(p -> p.getId().equals(pageId)).findFirst();
        if (page.isPresent()) {
            PageValidityStatus validityStatus = isPageValid(page.get());
            page.get().setValidityStatus(validityStatus);
            if (validityStatus.isValid()) {
                page.get().getSidebarElements().forEach(element -> element.deSerialize());
            }
            return page.get();
        }
        return null;
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
            logger.trace("save sidebar elements");
            selectedPage.saveSidebarElements();
            logger.trace("validate page");
            validatePage(selectedPage, getDefaultLocale().getLanguage());
            logger.trace("reset item data");
            selectedPage.resetItemData();
            // Save
            boolean success = false;
            selectedPage.setDateUpdated(new Date());
            logger.trace("update dao");
            if (selectedPage.getId() != null) {
                success = DataManager.getInstance().getDao().updateCMSPage(selectedPage);
            } else {
                success = DataManager.getInstance().getDao().addCMSPage(selectedPage);
            }
            if (success) {
                Messages.info("cms_pageSaveSuccess");
                logger.trace("reload cms page");
//                selectedPage = getCMSPage(selectedPage.getId());
                setSelectedPage(selectedPage);
//                DataManager.getInstance().getDao().updateCMSPage(selectedPage);
                logger.trace("update pages");
                lazyModelPages.update();
            } else {
                Messages.error("cms_pageSaveFailure");
            }
            logger.trace("reset collections");
            resetCollectionsForPage(selectedPage.getId().toString());
            if (cmsNavigationBean != null) {
                logger.trace("add navigation item");
                cmsNavigationBean.getItemManager().addAvailableItem(new SelectableNavigationItem(this.selectedPage));
            }
        }
        logger.trace("Done saving page");
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
    
    public void invalidate() {
        collections = new HashMap<>();
    }

    public static boolean validateSidebarElement(CMSSidebarElement element) {
        if (element != null && !element.isValid()) {
            String msg = Helper.getTranslation("cms_validationWarningHtmlInvalid", null);
            Messages.error(msg.replace("{0}", element.getType()).replace("{1}", CMSSidebarManager.getInstance().getDisallowedHtmlTagsForDisplay()));
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
            if (StringUtils.isBlank(languageVersion.getTitle())) {
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
                            if (item.getPageClassification().length == 0) {
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
            String mainTheme = DataManager.getInstance().getConfiguration().getTheme();
            String currentTheme = BeanUtils.getNavigationHelper().getThemeOrSubtheme();
            List<CMSNavigationItem> items = DataManager.getInstance().getDao().getAllTopCMSNavigationItems().stream().filter(item -> (StringUtils.isBlank(item.getAssociatedTheme()) && mainTheme.equalsIgnoreCase(currentTheme)) || currentTheme.equalsIgnoreCase( item.getAssociatedTheme())).collect(Collectors.toList());
            if(items.isEmpty()) {
                items = DataManager.getInstance().getDao().getAllTopCMSNavigationItems().stream().filter(item -> StringUtils.isBlank(item.getAssociatedTheme()) || item.getAssociatedTheme().equalsIgnoreCase(mainTheme)).collect(Collectors.toList());
            }
            return items;
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
                lazyModelPages.update();
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
                lazyModelPages.update();
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

        if (pageId != null) {
            return getCMSPage(pageId);
        }
        return null;
    }

    public CMSPage getSelectedPage() {
        return selectedPage;
    }

    public void setSelectedPage(CMSPage currentPage) throws DAOException {
        if(currentPage != null) {            
            if (currentPage.getId() != null) {
                this.selectedPage = DataManager.getInstance().getDao().getCMSPageForEditing(currentPage.getId());
            } else {
                this.selectedPage = currentPage;
            }
            this.selectedPage.getSidebarElements().forEach(element -> element.deSerialize());
            logger.debug("Selected page " + currentPage);
        } else {
            this.selectedPage = null;
        }

    }

    public CMSPage getCurrentPage() {
        if (currentPage == null) {
            return new CMSPage();
        }
        return currentPage;
    }

    public void setCurrentPage(CMSPage currentPage) {
        this.currentPage = currentPage;
        if (currentPage != null) {
            this.currentPage.setListPage(1);
            navigationHelper.setCmsPage(true);
            logger.trace("Set current cms page to " + this.currentPage.getTitle());
        }
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
     * @throws ViewerConfigurationException
     */
    public String cmsContextAction() throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return cmsContextAction(true);
    }

    /**
     * Action method called when a CMS page is opened. The exact action depends on the page and content item type.
     * 
     * @param resetSearch If true, the search parameters in SearchBean will be reset
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public String cmsContextAction(boolean resetSearch)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("cmsContextAction: {}", resetSearch);
        if (currentPage != null) {
            List<CMSContentItem> contentItems = currentPage.getGlobalContentItems();
            for (CMSContentItem item : contentItems) {
                if (item != null && CMSContentItemType.SOLRQUERY.equals(item.getType())) {
                    if (resetSearch && searchBean != null) {
                        searchBean.resetSearchAction();
                    }
                    return searchAction(item);
                } else if (item != null && CMSContentItemType.SEARCH.equals(item.getType())) {
                    //                    setSearchType();
                    if (resetSearch && searchBean != null) {
                        searchBean.resetSearchAction();
                    }
                    return searchAction(item);
                } else if (item != null && CMSContentItemType.COLLECTION.equals(item.getType())) {
                    getCollection(item.getItemId(), currentPage).reset(true);
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
                return search.getHits();
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
     * @throws ViewerConfigurationException
     */
    public String searchAction(CMSContentItem item)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("searchAction");
        if (searchBean == null) {
            logger.error("Cannot search: SearchBean is null");
            return "";
        }
        if (item != null && CMSContentItemType.SEARCH.equals(item.getType())) {
            ((SearchFunctionality) item.getFunctionality()).search();
        } else if (item != null && StringUtils.isNotBlank(item.getSolrQuery())) {
            searchBean.resetSearchResults();
            searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
            searchBean.setHitsPerPage(item.getElementsPerPage());
            searchBean.setExactSearchStringResetGui(item.getSolrQuery());
//            searchBean.setCurrentPage(item.getListPage());
            if (item.getSolrSortFields() != null) {
                searchBean.setSortString(item.getSolrSortFields());
            }
            return searchBean.search();
        }
        if (item == null) {
            logger.error("Cannot search: item is null");
            searchBean.resetSearchResults();
            return "";
        }
        if (StringUtils.isBlank(item.getSolrQuery())) {
            searchBean.resetSearchResults();
            return "";
        }
        return "";
        //        searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
        //        searchBean.setHitsPerPage(item.getElementsPerPage());
        //        searchBean.setExactSearchStringResetGui(item.getSolrQuery());
        //        searchBean.setCurrentPage(item.getListPage());
        //        if (item.getSolrSortFields() != null) {
        //            searchBean.setSortString(item.getSolrSortFields());
        //        }
        //        //            searchBean.getFacets().setCurrentFacetString();
        //        //            searchBean.getFacets().setCurrentCollection();
        //        return searchBean.search();
    }

    public boolean hasSearchResults() {
        return searchBean != null && searchBean.getCurrentSearch() != null && searchBean.getCurrentSearch().getHitsCount() > 0;
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
        logger.trace("removeFacetAction: {}", facetQuery);
        CMSPage currentPage = getCurrentPage();
        if (currentPage != null) {
            SearchFunctionality search = currentPage.getSearch();
            if (search != null) {
                search.setFacetString("-");
            }
        }
        if (searchBean != null) {
            searchBean.removeFacetAction(facetQuery);
        }

        return "pretty:cmsOpenPageWithSearch2";
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
        if (searchBean != null && searchBean.getCurrentSearch() != null) {
            return searchBean.getCurrentSearch().getLastPage(searchBean.getHitsPerPage());
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

    /**
     * 
     * @param id
     * @param page
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public CollectionView getCollection(String id, CMSPage page) throws PresentationException, IndexUnreachableException {
        String myId = page.getId() + "_" + id;
        CollectionView collection = collections.get(myId);
        if (collection == null) {
            CMSContentItem contentItem = page.getContentItem(id);
            if (contentItem != null) {
                collection = contentItem.initializeCollection();
                collections.put(myId, collection);
            }
        }
        return collection;
    }

    public CollectionView getCollection(CMSPage page) throws PresentationException, IndexUnreachableException {
        Optional<CMSContentItem> collectionItem =
                page.getGlobalContentItems().stream().filter(item -> CMSContentItemType.COLLECTION.equals(item.getType())).findFirst();
        if (collectionItem.isPresent()) {
            return getCollection(collectionItem.get().getItemId(), page);
        }

        return null;
    }

    public static List<String> getLuceneFields() {
        return getLuceneFields(false, false);
    }

    public static List<String> getLuceneFields(boolean includeUntokenized) {
        return getLuceneFields(includeUntokenized, false);
    }

    /**
     * 
     * @param includeUntokenized
     * @param excludeTokenizedMetadataFields
     * @return
     */
    public static List<String> getLuceneFields(boolean includeUntokenized, boolean excludeTokenizedMetadataFields) {
        List<String> constants;
        try {
            constants = DataManager.getInstance().getSearchIndex().getAllFieldNames();
            Iterator<String> iterator = constants.iterator();
            while (iterator.hasNext()) {
                String name = iterator.next();
                if (name.startsWith("_") || name.startsWith("FACET_") || name.startsWith("NORM_")
                        || (!includeUntokenized && name.endsWith(SolrConstants._UNTOKENIZED))
                        || (excludeTokenizedMetadataFields && name.startsWith("MD_") && !name.endsWith(SolrConstants._UNTOKENIZED))) {
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
    @SuppressWarnings("deprecation")
    private List<CMSStaticPage> createStaticPageList() throws DAOException {
        List<CMSStaticPage> staticPages = DataManager.getInstance().getDao().getAllStaticPages();

//        if (staticPages == null || staticPages.isEmpty()) {
//            //resore from old schema
//            staticPages = getAllCMSPages().stream()
//                    .filter(cmsPage -> StringUtils.isNotBlank(cmsPage.getStaticPageName()))
//                    .map(cmsPage -> new CMSStaticPage(cmsPage))
//                    .distinct()
//                    .collect(Collectors.toList());
//        }
        List<PageType> pageTypesForCMS = PageType.getTypesHandledByCms();
        for (PageType pageType : pageTypesForCMS) {
            CMSStaticPage newPage = new CMSStaticPage(pageType.name());
            if (!staticPages.contains(newPage)) {
                staticPages.add(newPage);
            }
        }

        return staticPages;
    }

    /**
     * @return A list of all cmsPages except the given one
     * @throws DAOException
     */
    public List<CMSPage> getAvailableParentPages(CMSPage page) throws DAOException {
        Locale currentLocale = BeanUtils.getLocale();
        return getAllCMSPages().stream()
                .filter(p -> !p.equals(page))
                .sorted((p1, p2) -> p1.getTitle(currentLocale).toLowerCase().compareTo(p2.getTitle(currentLocale).toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * @return A list of all cmsPages not yet registered to a static page
     * @throws DAOException
     */
    public List<CMSPage> getAvailableCmsPages(CMSStaticPage page) throws DAOException {
        List<CMSPage> allPages = getAllCMSPages().stream()
                .filter(cmsPage -> isPageValid(cmsPage).equals(PageValidityStatus.VALID))
                .filter(cmsPage -> cmsPage.isPublished())
                .collect(Collectors.toList());

        for (CMSStaticPage staticPage : getStaticPages()) {
            if (!staticPage.equals(page) && staticPage.isHasCmsPage()) {
                allPages.remove(staticPage.getCmsPageOptional());
            }
        }
        return allPages;
    }

    /**
     * @return a list of all valid cms pages which contain a "search" item
     * @throws DAOException
     */
    public List<CMSPage> getCMSPagesWithSearch() throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getAllCMSPages()
                .stream()
                .filter(cmsPage -> isPageValid(cmsPage).equals(PageValidityStatus.VALID))
                .filter(cmsPage -> cmsPage.getGlobalContentItems().stream().anyMatch(item -> CMSContentItemType.SEARCH.equals(item.getType())))
                .collect(Collectors.toList());
    }

    /**
     * Save static page status for all cms pages
     * 
     * @throws DAOException
     */
    public void saveStaticPages() throws DAOException {
        for (CMSStaticPage page : getStaticPages()) {
            try {
                if (page.getId() != null) {
                    DataManager.getInstance().getDao().updateStaticPage(page);
                } else {
                    DataManager.getInstance().getDao().addStaticPage(page);
                }
            } catch (DAOException e) {
                Messages.error("cms_errorSavingStaticPages");
                return;
            }
        }
        Messages.info("cms_staticPagesSaved");
    }

    /**
     * 
     * @return all cmsPages which are valid and have a menu title
     * @throws DAOException
     */
    public List<CMSPage> getValidCMSPages() throws DAOException {
        return getAllCMSPages().stream()
                .filter(page -> isPageValid(page).equals(PageValidityStatus.VALID) && StringUtils.isNotBlank(page.getMenuTitle()))
                .filter(page -> page.isPublished())
                .collect(Collectors.toList());
    }

    /**
     * @return
     */
    protected FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    public List<String> getSubThemeDiscriminatorValues() throws PresentationException, IndexUnreachableException {
        String subThemeDiscriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
        if (StringUtils.isNotBlank(subThemeDiscriminatorField)) {
            subThemeDiscriminatorField = subThemeDiscriminatorField + "_UNTOKENIZED";
            List<String> values = SearchHelper.getFacetValues(subThemeDiscriminatorField + ":*", subThemeDiscriminatorField, 0);
            return values;
        }
        return Collections.emptyList();
    }

    /**
     * Sets the searchType in SearchBean to the type assciated with the current static view (e.g. if the current cms page replaces the static page
     * 'advancedSearch' the search type is set to 'advanced') For the normal search pages this is done in the pretty mapping which isn't used if
     * redirecting to cms page
     * 
     * @param currentPath
     */
    public void setSearchType() {
        logger.trace("setSearchType");
        Optional<ViewerPath> currentPath = ViewHistory.getCurrentView(BeanUtils.getRequest());
        if (currentPath.isPresent()) {
            SearchBean searchBean = BeanUtils.getSearchBean();
            if (searchBean != null) {
                if (PageType.advancedSearch.equals(currentPath.get().getPageType())) {
                    searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_ADVANCED);
                } else if (PageType.calendarsearch.equals(currentPath.get().getPageType())) {
                    searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_CALENDAR);
                } else if (PageType.search.equals(currentPath.get().getPageType())) {
                    searchBean.setActiveSearchType(SearchHelper.SEARCH_TYPE_REGULAR);
                }
            }
        }
    }

    /**
     * 
     * @param pi
     * @return
     * @throws DAOException
     */
    public boolean isHasRelatedPages(String pi) throws DAOException {
        if (StringUtils.isEmpty(pi)) {
            return false;
        }
        List<CMSPage> relatedPages = getRelatedPages(pi);
        return relatedPages != null && !relatedPages.isEmpty();
    }

    /**
     * 
     * @param pi
     * @return
     * @throws DAOException
     */
    public List<CMSPage> getRelatedPages(String pi) throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getAllCMSPages()
                .stream()
                .filter(page -> pi.equals(page.getRelatedPI()))
                .filter(page -> page.isPublished())
                .collect(Collectors.toList());
    }

    /**
     * 
     * @param pi
     * @param classification
     * @return
     * @throws DAOException
     */
    public List<CMSPage> getRelatedPages(String pi, String classification) throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getAllCMSPages()
                .stream()
                .filter(page -> pi.equals(page.getRelatedPI()))
                .filter(page -> page.getClassifications().contains(classification))
                .filter(page -> page.isPublished())
                .collect(Collectors.toList());
    }

    public List<Glossary> getGlossaries() {
        try {
            return new GlossaryManager().getGlossaries();
        } catch (IOException e) {
            logger.error("Error loading glossary files", e);
            return Collections.emptyList();
        }
    }

    public String getRepresentativeImageForQuery(CMSPage page) throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        int width = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
        int height = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
        return getRepresentativeImageForQuery(page, width, height);
    }

    public String getRepresentativeImageForQuery(CMSContentItem item)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        int width = DataManager.getInstance().getConfiguration().getThumbnailsWidth();
        int height = DataManager.getInstance().getConfiguration().getThumbnailsHeight();
        return getRepresentativeImageForQuery(item, width, height);
    }

    public String getRepresentativeImageForQuery(CMSPage page, int width, int height)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        CMSContentItem contentItem =
                page.getGlobalContentItems().stream().filter(item -> CMSContentItemType.SOLRQUERY.equals(item.getType())).findAny().orElseThrow(
                        () -> new IllegalStateException("The page does not contain content items of type '" + CMSContentItemType.SOLRQUERY + "'"));
        return getRepresentativeImageForQuery(contentItem, width, height);
    }

    public String getRepresentativeImageForQuery(CMSContentItem item, int width, int height)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        if (StringUtils.isBlank(item.getSolrQuery())) {
            throw new IllegalStateException("Item " + item + " does not define a solr query");
        }
        SolrDocument doc =
                DataManager.getInstance().getSearchIndex().getFirstDoc(item.getSolrQuery(), Arrays.asList(ThumbnailHandler.REQUIRED_SOLR_FIELDS));
        if (doc != null) {
            return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(doc, width, height);
        }
        throw new PresentationException("No document matching query '" + item.getSolrQuery() + "' found");
    }
    
    public List<String> getPossibleSortFields() throws PresentationException, IndexUnreachableException, SolrServerException, IOException {
        List<String> sortFields = DataManager.getInstance().getSearchIndex().getAllSortFieldNames();
//        sortFields = sortFields.stream().flatMap(field -> Arrays.asList(field + " asc", field + " desc").stream()).collect(Collectors.toList());
        return sortFields;
    }

}
