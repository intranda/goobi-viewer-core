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

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.monitoring.timer.Time;
import de.intranda.monitoring.timer.TimeAnalysis;
import de.intranda.monitoring.timer.TimeAnalysisItem;
import de.intranda.monitoring.timer.Timer;
import de.intranda.monitoring.timer.TimerOutput;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.Helper;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordDeletedException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSContentItem.CMSContentItemType;
import io.goobi.viewer.model.cms.CMSMediaHolder;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSNavigationItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.CMSPageLanguageVersion;
import io.goobi.viewer.model.cms.CMSPageLanguageVersion.CMSPageStatus;
import io.goobi.viewer.model.cms.CMSPageTemplate;
import io.goobi.viewer.model.cms.CMSSidebarElement;
import io.goobi.viewer.model.cms.CMSSidebarManager;
import io.goobi.viewer.model.cms.CMSStaticPage;
import io.goobi.viewer.model.cms.CMSTemplateManager;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.cms.PageValidityStatus;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.cms.SelectableNavigationItem;
import io.goobi.viewer.model.cms.itemfunctionality.SearchFunctionality;
import io.goobi.viewer.model.glossary.Glossary;
import io.goobi.viewer.model.glossary.GlossaryManager;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchHit;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.viewer.CollectionView;
import io.goobi.viewer.model.viewer.PageType;

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
    @Inject
    private UserBean userBean;
    @Inject
    private CmsMediaBean cmsMediaBean;

    private TableDataProvider<CMSPage> lazyModelPages;
    /** The page open for editing */
    private CMSPage selectedPage;
    /** The page currently open for viewing */
    private CMSPage currentPage;
    private Locale selectedLocale;
    private Locale selectedMediaLocale;
    private CMSMediaItem selectedMediaItem;
    private CMSSidebarElement selectedSidebarElement;
    private boolean displaySidebarEditor = false;
    private int nestedPagesCount = 0;
    private boolean editMode = false;
    private Map<String, CollectionView> collections = new HashMap<>();
    private List<CMSStaticPage> staticPages = null;
    private String currentWorkPi = "";
    private Optional<CMSMediaHolder> selectedMediaHolder = Optional.empty();
    private HashMap<Long, Boolean> editablePages = new HashMap<>();
    private List<String> solrSortFields = null;

    @PostConstruct
    public void init() {
        if (lazyModelPages == null) {
            lazyModelPages = new TableDataProvider<>(new TableDataSource<CMSPage>() {

                private Optional<Long> numCreatedPages = Optional.empty();
                private List<String> allowedSubthemes = null;
                private List<String> allowedCategories = null;
                private List<String> allowedTemplates = null;
                private boolean initialized = false;

                @Override
                public List<CMSPage> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    try {
                        initialize();
                        if (StringUtils.isBlank(sortField)) {
                            sortField = "id";
                        }

                        List<CMSPage> pages = DataManager.getInstance()
                                .getDao()
                                .getCMSPages(first, pageSize, sortField, sortOrder.asBoolean(), filters, allowedTemplates, allowedSubthemes,
                                        allowedCategories);
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
                                    .map(l -> l.toString())
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
     * @deprecated This method only remains to avoid pages failing to load which still call this method
     */
    @Deprecated
    public void forwardToCMSPage() {
        //do nothing
    }

    public Locale getDefaultLocale() {
        return ViewerResourceBundle.getDefaultLocale();
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
        CMSTemplateManager.getInstance()
                .updateTemplates(CMSTemplateManager.getInstance().getCoreFolderPath(), CMSTemplateManager.getInstance().getThemeFolderPath());
    }

    /**
     * 
     * @return all existing templates
     */
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
     * Returns a filtered page template list for the given user, unless the user is a superuser. Other CMS admins get a list matching the template ID
     * list attached to ther CMS license.
     * 
     * @param user
     * @return List of CMS templates whose IDs are among allowed template IDs
     */
    public List<CMSPageTemplate> getAllowedTemplates(User user) {
        logger.trace("getAllowedTemplates");
        if (user == null) {
            return Collections.emptyList();
        }

        return user.getAllowedTemplates(getTemplates());
    }

    /**
     * @param page
     * @param template
     * @return
     */
    private static PageValidityStatus isPageValid(CMSPage page) {
        CMSPageTemplate template = page.getTemplate();
        if (template == null) {
            //remove pages with no template files
            return PageValidityStatus.INVALID_NO_TEMPLATE;
        }
        //check if all page content items exist in template
        List<CMSContentItem> allPageItems =
                page.getLanguageVersions().stream().flatMap(lang -> lang.getContentItems().stream()).distinct().collect(Collectors.toList());
        for (CMSContentItem pageItem : allPageItems) {
            if (template.getContentItem(pageItem.getItemId()) == null) {
                //if not, remove them
                page.removeContentItem(pageItem.getItemId());
            }
        }
        //check if all template content items exist in page and add missing items
        for (CMSContentItem templateItem : template.getContentItems()) {
            // completely new item
            if (!page.hasContentItem(templateItem.getItemId())) {
                page.addContentItem(templateItem);
                logger.info("Added new template item '{}' all languages.", templateItem.getItemLabel());
            }
            // new language version
            for (CMSPageLanguageVersion language : page.getLanguageVersions()) {
                language.addContentItemFromTemplateItem(templateItem);
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

    public CMSPage createNewPage(CMSPageTemplate template) throws PresentationException, IndexUnreachableException, DAOException {
        List<Locale> locales = getAllLocales();
        CMSPage page = template.createNewPage(locales);
        setUserRestrictedValues(page, userBean.getUser());
        // page.setId(System.currentTimeMillis());
        page.setDateCreated(new Date());
        return page;
    }

    /**
     * Create a new CMSPage based on the given template. title and relatedPI are set on the page if given Opens the view to create/edit the cmsPage
     * 
     * @param templateId The id of the template to base the page on
     * @param title The title to be used for the current locale, optional
     * @param relatedPI The PI of a related work, optional
     */
    public String createAndOpenNewPage(String templateId, String title, String relatedPI) {
        CMSPageTemplate template = CMSTemplateManager.getInstance().getTemplate(templateId);
        if (template != null) {
            try {
                CMSPage page = createNewPage(template);
                if (StringUtils.isNotBlank(title)) {
                    page.getLanguageVersion(getCurrentLocale()).setTitle(title);
                }
                if (StringUtils.isNotBlank(relatedPI)) {
                    page.setRelatedPI(relatedPI);
                }

                setSelectedPage(page);

                return "pretty:adminCmsCreatePage";

            } catch (PresentationException | IndexUnreachableException | DAOException e) {
                logger.error("Error creating new page", e);
            }

        } else {
            logger.error("No template found with id {}. Cannot create new page", templateId);
        }
        return "";
    }

    /**
     * Fills all properties of the page with values for which the user has privileges - but only if the user has restricted privileges for that
     * property
     * 
     * @param page
     * @param user
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    private void setUserRestrictedValues(CMSPage page, User user) throws PresentationException, IndexUnreachableException, DAOException {
        if (!user.hasPrivilegeForAllSubthemeDiscriminatorValues()) {
            List<String> allowedSubThemeDiscriminatorValues = user.getAllowedSubthemeDiscriminatorValues(getSubthemeDiscriminatorValues());
            if (StringUtils.isBlank(page.getSubThemeDiscriminatorValue()) && allowedSubThemeDiscriminatorValues.size() > 0) {
                page.setSubThemeDiscriminatorValue(allowedSubThemeDiscriminatorValues.get(0));
            } else {
                logger.error("User has no access to any subtheme discriminator values and can therefore not create a page");
                //do something??			
            }
        }
        if (!user.hasPrivilegeForAllCategories()) {
            List<CMSCategory> allowedCategories = user.getAllowedCategories(getAllCategories());
            if (page.getCategories().isEmpty() && allowedCategories.size() > 0) {
                page.setCategories(allowedCategories.subList(0, 1));
            }
            for (CMSContentItem contentItem : page.getGlobalContentItems()) {
                if (contentItem.getCategories().isEmpty() && allowedCategories.size() > 0) {
                    contentItem.setCategories(allowedCategories.subList(0, 1));
                }
            }
        }

    }

    /**
     * Current page URL getter for PrettyFaces. Page must be either published or the current user must be an admin.
     *
     * @return
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getCurrentPageUrl() {
        logger.trace("getCurrentPageUrl");
        if (currentPage != null
                && (currentPage.isPublished() || (userBean != null && userBean.getUser() != null && userBean.getUser().isCmsAdmin()))) {
            String url = getTemplateUrl(currentPage.getTemplateId(), false);
            return url;
        }
        return "pretty:index";
    }

    /**
     * Returns the URL to the CMS template of the given page. This URL will only resolve if the page has been published or the current user is CMS
     * admin.
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
        for (CMSCategory category : item.getCategories()) {
            for (CMSPage cmsPage : cmsPages) {
                if (cmsPage.isPublished() && cmsPage.getCategories().contains(category)) {
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

    /**
     * @return
     * @throws DAOException
     */
    public List<CMSPage> getAllCMSPages() throws DAOException {
        List<CMSPage> pages = DataManager.getInstance().getDao().getAllCMSPages();
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

    public CMSSidebarElement getSidebarElement(String type) {
        return getSidebarElements(true).stream().filter(widget -> widget.getType().equalsIgnoreCase(type)).findFirst().orElse(null);
    }

    /**
     * @return true if an {@link ActiveDocumentBean} is registered and the the {@link CMSPage#getRelatedPI()} of {@link #getCurrentPage()} is loaded
     */
    public boolean isRelatedWorkLoaded() throws IndexUnreachableException {
        if (getCurrentPage() != null && StringUtils.isNotBlank(getCurrentPage().getRelatedPI())) {
            ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
            if (adb != null && StringUtils.isNotBlank(adb.getPersistentIdentifier())
                    && adb.getPersistentIdentifier().equals(getCurrentPage().getRelatedPI())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the current page to the database, if it doesn't exist or updates it otherwise
     *
     * @throws DAOException
     *
     */
    @SuppressWarnings("unused")
    public void saveSelectedPage() throws DAOException {
        logger.trace("saveSelectedPage");
        if (userBean == null || userBean == null || !userBean.getUser().isCmsAdmin()) {
            // Only authorized CMS admins may save
            return;
        }
        // resetImageDisplay();
        if (selectedPage != null) {
            // Validate
            logger.trace("save sidebar elements");
            selectedPage.saveSidebarElements();
            logger.trace("validate page");
            if (!validatePage(selectedPage, getDefaultLocale().getLanguage())) {
                logger.warn("Cannot save invalid page");
                return;
            }
            logger.trace("reset item data");
            selectedPage.resetItemData();
            writeCategoriesToPage();

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

                // Re-index related record
                if (StringUtils.isNotEmpty(selectedPage.getRelatedPI())) {
                    try {
                        Helper.reIndexRecord(selectedPage.getRelatedPI());
                        Messages.info("admin_recordReExported");
                    } catch (RecordNotFoundException e) {
                        logger.error(e.getMessage());
                    }
                }
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
     * This is kind of a hack to avoid a ConcurrentModificationException when persisting page after changing categories. The exception is probably
     * caused by the categories taken from License object, since it only occurs if the categories are actually taken from Licenses due to limited
     * rights of the user
     */
    private void writeCategoriesToPage() {
        selectedPage.writeSelectableCategories();
        selectedPage.getGlobalContentItems().forEach(item -> item.writeSelectableCategories());
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
    protected static boolean validatePage(CMSPage page, String defaultLanguage) {

        if (!page.isUseDefaultSidebar()) {
            for (CMSSidebarElement element : page.getSidebarElements()) {
                if (!validateSidebarElement(element)) {
                    page.setPublished(false);
                    return false;
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
                            if (item.getCategories().size() == 0) {
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
        return true;
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
            List<CMSNavigationItem> items = DataManager.getInstance()
                    .getDao()
                    .getAllTopCMSNavigationItems()
                    .stream()
                    .filter(item -> (StringUtils.isBlank(item.getAssociatedTheme()) && mainTheme.equalsIgnoreCase(currentTheme))
                            || currentTheme.equalsIgnoreCase(item.getAssociatedTheme()))
                    .collect(Collectors.toList());
            if (items.isEmpty()) {
                items = DataManager.getInstance()
                        .getDao()
                        .getAllTopCMSNavigationItems()
                        .stream()
                        .filter(item -> StringUtils.isBlank(item.getAssociatedTheme()) || item.getAssociatedTheme().equalsIgnoreCase(mainTheme))
                        .collect(Collectors.toList());
            }
            return items;
        } catch (DAOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Action method for deleting selectedPage from the database.
     * 
     * @return Return view
     * @throws DAOException
     */
    public String deleteSelectedPage() throws DAOException {
        deletePage(selectedPage);
        return "cmsOverview";
    }

    /**
     * Deletes given CMS page from the database.
     * 
     * @param page Page to delete
     * @throws DAOException
     */
    public void deletePage(CMSPage page) throws DAOException {
        if (DataManager.getInstance().getDao() != null && page != null && page.getId() != null) {
            logger.info("Deleting CMS page: {}", selectedPage);
            if (DataManager.getInstance().getDao().deleteCMSPage(page)) {
                // Delete files matching content item IDs of the deleted page and re-index record
                try {
                    if (page.deleteExportedTextFiles() > 0) {
                        try {
                            Helper.reIndexRecord(page.getRelatedPI());
                            logger.debug("Re-indexing record: {}", page.getRelatedPI());
                        } catch (RecordNotFoundException e) {
                            logger.error(e.getMessage());
                        }
                    }
                } catch (ViewerConfigurationException e) {
                    logger.error(e.getMessage());
                    Messages.error(e.getMessage());
                }
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
        if (currentPage != null) {
            if (currentPage.getId() != null) {
                this.selectedPage = DataManager.getInstance().getDao().getCMSPageForEditing(currentPage.getId());
            } else {
                this.selectedPage = currentPage;
            }
            PageValidityStatus validityStatus = isPageValid(this.selectedPage);
            this.selectedPage.setValidityStatus(validityStatus);
            if (validityStatus.isValid()) {
                this.selectedPage.getSidebarElements().forEach(element -> element.deSerialize());
            }
            this.selectedPage.createMissingLanguageVersions(getAllLocales());
            logger.debug("Selected page: {}", currentPage);

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
     * @throws ContentNotFoundException
     */
    public void setCurrentPageId(String id) throws DAOException, ContentNotFoundException {
        logger.trace("setCurrentPageId: {}", id);
        CMSPage page = findPage(id);
        setCurrentPage(page);
    }

    public void checkRelatedWork() throws ContentNotFoundException {
        CMSPage page = getCurrentPage();
        //if we have both a cmsPage and a currentWorkPi set, they must be the same
        //the currentWorkPi is set via pretty mapping
        if (page != null && StringUtils.isNotBlank(getCurrentWorkPi()) && !getCurrentWorkPi().equals(page.getRelatedPI())) {
            throw new ContentNotFoundException("There is no CMS page with id " + page.getId() + " related to PI " + getCurrentWorkPi());
        }
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

    public boolean isDisplaySidebarEditor() {
        return displaySidebarEditor;
    }

    public void setDisplaySidebarEditor(boolean displaySidebarEditor) {
        this.displaySidebarEditor = displaySidebarEditor;
    }

    /**
     * Create a list of {@link Selectable} containing all {@link CMSCategory CMSCategories} which the current user may access and select those which
     * are included in the {@link #getSelectedPage()}
     * 
     * @return the list of selectable categories which may be applied to the selected page
     * @deprecated moved categories logic to {@link CMSPage}
     */
    public List<Selectable<CMSCategory>> getCategoriesToSelect() throws DAOException {
        User user = null;
        if (userBean != null) {
            user = userBean.getUser();
        }
        if (user == null) {
            return Collections.emptyList();
        }
        List<CMSCategory> categories = new ArrayList<>(user.getAllowedCategories(DataManager.getInstance().getDao().getAllCategories()));
        categories.sort((c1, c2) -> c1.getId().compareTo(c2.getId()));
        List<Selectable<CMSCategory>> selectables = new ArrayList<>();
        if (this.selectedPage != null) {
            for (CMSCategory category : categories) {
                boolean used = this.selectedPage.getCategories().contains(category);
                Selectable<CMSCategory> selectable = new Selectable<>(category, used);
                selectables.add(selectable);
            }
        }
        return selectables;
    }

    /**
     * @return false only if the user has limited privileges for categories and only one category is set for the selected page
     * @throws DAOException
     */
    public boolean mayRemoveCategoryFromPage(CMSCategory cat) throws DAOException {
        if (this.selectedPage != null) {
            return userBean.getUser().hasPrivilegeForAllCategories()
                    || this.selectedPage.getSelectableCategories().stream().anyMatch(c -> c.isSelected());
        }

        return true;
    }

    /**
     * @return the return value of {@link IDAO#getAllCategories()}
     */
    public List<CMSCategory> getAllCategories() throws DAOException {
        return DataManager.getInstance().getDao().getAllCategories();
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
                        searchBean.setActiveSearchType(item.getSearchType());
                    }
                    if (StringUtils.isNotBlank(searchBean.getExactSearchString().replace("-", ""))) {
                        searchBean.setShowReducedSearchOptions(true);
                        return searchAction(item);
                    } else if (item.isDisplayEmptySearchResults()) {
                        String searchString = StringUtils.isNotBlank(item.getSolrQuery().replace("-", "")) ? item.getSolrQuery() : "";
                        searchBean.setExactSearchString(searchString);
                        searchBean.setShowReducedSearchOptions(false);
                        return searchAction(item);
                    } else {
                        searchBean.setShowReducedSearchOptions(false);
                    }
                } else if (item != null && CMSContentItemType.COLLECTION.equals(item.getType())) {
                    getCollection(item.getItemId(), currentPage).reset(true);
                }
            }

            // If the page is related to a record, load that record
            if (StringUtils.isNotEmpty(currentPage.getRelatedPI())) {
                ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
                if (adb != null && !currentPage.getRelatedPI().equals(adb.getPersistentIdentifier())) {
                    logger.trace("Loading related record: {}", currentPage.getRelatedPI());
                    try {
                        adb.setPersistentIdentifier(currentPage.getRelatedPI());
                        adb.update();
                    } catch (RecordNotFoundException e) {
                        logger.warn(e.getMessage());
                    } catch (RecordDeletedException e) {
                        logger.warn(e.getMessage());
                    }
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
            if (StringUtils.isNotBlank(item.getSolrSortFields())) {
                searchBean.setSortString(item.getSolrSortFields());
            }
            return searchBean.search();
        } else if (item == null) {
            logger.error("Cannot search: item is null");
            searchBean.resetSearchResults();
            return "";
        }
        return "";
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
     * Calculates the number of pages needed for the paginator. The value is taken directly from {@link Search#getLastPage(int)}
     * 
     * @return The number of pages to display in the paginator
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public long getQueryResultCount() throws PresentationException, IndexUnreachableException {
        if (searchBean != null && searchBean.getCurrentSearch() != null) {
            return searchBean.getCurrentSearch().getLastPage(searchBean.getHitsPerPage());
        }
        return 0;
    }

    /**
     * Calcs number of paginator pages for the query result.
     * 
     * @param item
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @deprecated use {@link #getQueryResultCount()} instead
     */
    @Deprecated
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
     * Get the {@link CollectionView} of the given content item in the given page. If the view hasn't been initialized yet, do so and add it to the
     * Bean's CollectionView map
     * 
     * @param id The ContentItemId of the ContentItem to look for
     * @param page The page containing the collection ContentItem
     * @return The CollectionView or null if no matching ContentItem was found
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

    /**
     * get a list of all {@link CollectionView}s with the given solr field which are already loaded via {@link #getCollection(CMSPage)} or
     * {@link #getCollection(String, CMSPage)
     * 
     * @param field The solr field the colleciton is based on
     * @return
     */
    public List<CollectionView> getCollections(String field) {
        return collections.values().stream().filter(collection -> field.equals(collection.getField())).collect(Collectors.toList());
    }

    /**
     * Get the first available {@link CollectionView} from any {@link CMSContentItem} of the given {@link CMSPage page}. The CollectionView is added
     * to the Bean's internal collection map
     * 
     * @param page The CMSPage to provide the collection
     * @return The CollectionView or null if none was found
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
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
    private static List<CMSStaticPage> createStaticPageList() throws DAOException {
        List<CMSStaticPage> staticPages = DataManager.getInstance().getDao().getAllStaticPages();

        List<PageType> pageTypesForCMS = PageType.getTypesHandledByCms();
        for (PageType pageType : pageTypesForCMS) {
            CMSStaticPage newPage = new CMSStaticPage(pageType.getName());
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
                allPages.remove(staticPage.getCmsPageOptional().get());
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

    public List<String> getSubthemeDiscriminatorValues() throws PresentationException, IndexUnreachableException {
        String subThemeDiscriminatorField = DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField();
        if (StringUtils.isNotBlank(subThemeDiscriminatorField)) {
            subThemeDiscriminatorField = subThemeDiscriminatorField + "_UNTOKENIZED";
            List<String> values = SearchHelper.getFacetValues(subThemeDiscriminatorField + ":*", subThemeDiscriminatorField, 0);
            return values;
        }
        return Collections.emptyList();
    }

    /**
     * Returns a filtered subtheme discriminator value list for the given user, unless the user is a superuser. Other CMS admins get a list matching
     * values list attached to their CMS license.
     * 
     * @param user
     * @return List of CMS templates whose IDs are among allowed template IDs
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<String> getAllowedSubthemeDiscriminatorValues(User user) throws PresentationException, IndexUnreachableException {
        if (user == null) {
            return Collections.emptyList();
        }

        return user.getAllowedSubthemeDiscriminatorValues(getSubthemeDiscriminatorValues());
    }

    /**
     * 
     * @param user
     * @return true if user is limited to a subset of all available subtheme discriminator values; false otherwise
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public boolean isSubthemeRequired(User user) throws PresentationException, IndexUnreachableException {
        return user != null && !user.hasPrivilegeForAllSubthemeDiscriminatorValues();
    }

    /**
     * Returns a filtered category list for the given user, unless the user is a superuser. Other CMS admins get a list matching values list attached
     * to their CMS license.
     * 
     * @param user
     * @return
     * @throws DAOException
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
        List<CMSPage> relatedPages = DataManager.getInstance().getDao().getCMSPagesForRecord(pi, null);
        return relatedPages.stream()
                //                .filter(page -> page.isPublished())
                .collect(Collectors.toList());
    }

    /**
     * 
     * @param pi
     * @param classification
     * @return
     * @throws DAOException
     */
    public List<CMSPage> getRelatedPages(String pi, CMSCategory category) throws DAOException {
        return DataManager.getInstance()
                .getDao()
                .getCMSPagesForRecord(pi, category)
                .stream()
                //                .filter(page -> pi.equals(page.getRelatedPI()))
                //                .filter(page -> page.getClassifications().contains(classification))
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
        CMSContentItem contentItem = page.getGlobalContentItems()
                .stream()
                .filter(item -> CMSContentItemType.SOLRQUERY.equals(item.getType()))
                .findAny()
                .orElseThrow(
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

    public List<String> getPossibleSortFields() throws SolrServerException, IOException {
        if(this.solrSortFields == null) {            
            this.solrSortFields = DataManager.getInstance().getSearchIndex().getAllSortFieldNames();
        }
        return this.solrSortFields;
    }

    /**
     * 
     * @return The {@link CMSPage#getWrapperElementClass()} of the current CMSPage. If no current page is set or {@link NavigationHelper#isCmsPage()}
     *         returns false, an empty String is returned
     */
    public String getCssClass() {
        if (BeanUtils.getNavigationHelper().isCmsPage() && getCurrentPage() != null) {
            return getCurrentPage().getWrapperElementClass();
        }

        return "";
    }

    public Long getLastEditedTimestamp(long pageId) throws DAOException {
        return Optional.ofNullable(getCMSPage(pageId)).map(CMSPage::getDateUpdated).map(Date::getTime).orElse(null);
    }

    /**
     * @return the currentWorkPi
     */
    public String getCurrentWorkPi() {
        return currentWorkPi;
    }

    /**
     * @param currentWorkPi the currentWorkPi to set
     */
    public void setCurrentWorkPi(String currentWorkPi) {
        this.currentWorkPi = currentWorkPi == null ? "" : currentWorkPi;
    }

    public void resetCurrentWorkPi() {
        this.currentWorkPi = "";
    }

    /**
     * Set a {@link CMSMediaHolder} in the {@link CmsMediaBean} which may receive a {@link CMSMediaItem} selected in the selectMedia dialog
     * 
     * @param selectedMediaHolder the selectedMediaHolder to set
     */
    public void setSelectedMediaHolder(CMSMediaHolder item) {
        this.selectedMediaHolder = Optional.ofNullable(item);
        this.selectedMediaHolder.ifPresent(contentItem -> {
            String filter = contentItem.getMediaFilter();
            if (StringUtils.isBlank(filter)) {
                filter = CmsMediaBean.getImageFilter();
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

    /**
     * Set the given (wrapped) {@link CMSMediaItem} to Media holder set by {@link #setSelectedMediaHolder}
     */
    public void fillSelectedMediaHolder(CategorizableTranslatedSelectable<CMSMediaItem> mediaItem) {
        fillSelectedMediaHolder(mediaItem, false);
    }

    /**
     * Set the given (wrapped) {@link CMSMediaItem} to Media holder set by {@link #setSelectedMediaHolder} Additionally save the given media item if
     * the parameter saveMedia is set to true
     */
    public void fillSelectedMediaHolder(CategorizableTranslatedSelectable<CMSMediaItem> mediaItem, boolean saveMedia) {
        this.selectedMediaHolder.ifPresent(item -> {
            if (mediaItem != null) {
                item.setMediaItem(mediaItem.getValue());
                if (saveMedia) {
                    try {
                        cmsMediaBean.saveMedia(mediaItem.getValue(), mediaItem.getCategories());
                    } catch (DAOException e) {
                        logger.error("Failed to save media item: {}", e.toString());
                    }
                }
            } else {
                item.setMediaItem(null);
            }
        });
        this.selectedMediaHolder = Optional.empty();
        cmsMediaBean.setSelectedMediaItem(null);
    }

    /**
     * @return true if a mediaHolder is present
     */
    public boolean hasSelectedMediaHolder() {
        return this.selectedMediaHolder.isPresent();
    }

    public boolean mayEdit(CMSPage page) throws DAOException, PresentationException, IndexUnreachableException {

        if (userBean.getUser() != null) {
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
     * @param user
     * @param page
     * @return
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
            if (!user.hasPriviledgeForAllTemplates() && user.hasPrivilegesForTemplate(page.getTemplateId())) {
                return false;
            }
            if (!user.hasPrivilegeForAllCategories() && ListUtils.intersection(getAllowedCategories(user), page.getCategories()).isEmpty()) {
                return false;
            }
            if (!user.hasPrivilegeForAllSubthemeDiscriminatorValues()
                    && !getAllowedSubthemeDiscriminatorValues(user).contains(page.getSubThemeDiscriminatorValue())) {
                return false;
            }
            return true;
        }
    }

    public String editPage(CMSPage page) throws DAOException, PresentationException, IndexUnreachableException {
        if (mayEdit(page)) {
            setSelectedPage(page);
            return "pretty:adminCmsCreatePage";
        }

        return "";
    }

}
