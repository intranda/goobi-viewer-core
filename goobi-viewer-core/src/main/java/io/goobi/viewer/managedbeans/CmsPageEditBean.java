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
import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.IndexerTools;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.faces.utils.SelectItemBuilder;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.cms.SelectableNavigationItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageEditState;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.CMSContentItem;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;

/**
 * JSF backing bean for creating and editing CMS pages including content and metadata.
 */
@Named
@ViewScoped
public class CmsPageEditBean implements Serializable {

    private static final long serialVersionUID = 7163586584773468296L;
    private static final Logger logger = LogManager.getLogger(CmsPageEditBean.class);

    @Inject
    private transient IDAO dao;
    @Inject
    private transient CMSTemplateManager templateManager;
    @Inject
    private transient UserBean userBean;
    @Inject
    private transient CmsBean cmsBean;
    @Inject
    private transient CmsNavigationBean navigationBean;
    @Inject
    private transient CMSSidebarWidgetsBean widgetsBean;
    @Inject
    private transient CollectionViewBean collectionViewBean;
    @Inject
    private transient FacesContext facesContext;

    private CMSPage selectedPage = null;
    private boolean editMode = false;
    private CMSPageEditState pageEditState = CMSPageEditState.CONTENT;
    private String selectedComponent = "";

    private Map<WidgetDisplayElement, Boolean> sidebarWidgets;

    private boolean saveAsTemplate = false;
    private String templateName = "";
    private boolean templateLockComponents = false;

    /**
     * setup.
     */
    @PostConstruct
    public void setup() {
        try {
            long pageId = Long
                    .parseLong(facesContext.getExternalContext().getRequestParameterMap().getOrDefault("selectedPageId", "-1"));
            long templateId =
                    Long.parseLong(facesContext.getExternalContext().getRequestParameterMap().getOrDefault("templateId", "-1"));
            String title =
                    facesContext.getExternalContext().getRequestParameterMap().getOrDefault("title", "");
            String relatedPi =
                    facesContext.getExternalContext().getRequestParameterMap().getOrDefault("relatedPi", "");

            if (pageId > 0) {
                CMSPage page = this.dao.getCMSPage(pageId);
                this.setSelectedPage(page);
                this.editMode = true;
            } else if (templateId > -1) {
                this.editMode = false;
                this.setNewSelectedPage(templateId);
            } else {
                this.editMode = false;
                this.setNewSelectedPage();
            }
            if (!this.editMode && StringUtils.isNotBlank(title)) {
                this.selectedPage.getTitleTranslations().setValue(title, IPolyglott.getDefaultLocale());
            }
            if (!this.editMode && StringUtils.isNotBlank(relatedPi)) {
                this.selectedPage.setRelatedPI(relatedPi);
            }
        } catch (NullPointerException | NumberFormatException e) {
            this.editMode = false;
            this.setNewSelectedPage();
        } catch (DAOException e) {
            logger.error("Error retrieving cms page template from dao: {}", e.toString());
            this.editMode = false;
            this.setNewSelectedPage();
        }
        try {
            setUserRestrictedValues(selectedPage, userBean.getUser());
        } catch (PresentationException | DAOException e1) {
            logger.error("Error setting user specific subtheme and categories", e1);
        }
        try {
            this.sidebarWidgets = widgetsBean.getAllWidgets().stream().collect(Collectors.toMap(Function.identity(), w -> Boolean.FALSE));
        } catch (DAOException e) {
            this.sidebarWidgets = Collections.emptyMap();
        }
    }

    /**
     * savePageAndForwardToEdit.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
      * @should save page
      * @should save as template
      * @should save page no admin
     */
    public void savePageAndForwardToEdit() throws DAOException {
        this.saveSelectedPage();
        if (this.selectedPage.getId() != null) {
            String url = PrettyUrlTools.getAbsolutePageUrl("adminCmsEditPage", this.selectedPage.getId());
            try {
                facesContext.getExternalContext().redirect(url);
            } catch (IOException | NullPointerException e) {
                logger.error("Error redirecting to database url {}: {}", url, e.toString());
            }
        }
    }

    /**
     * Adds the current page to the database, if it doesn't exist or updates it otherwise.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should save page
     * @should save as template
     * @should save page no admin
     */
    public void saveSelectedPage() throws DAOException {
        logger.trace("saveSelectedPage");
        if (userBean == null || !userBean.getUser().isCmsAdmin() || selectedPage == null) {
            // Only authorized CMS admins may save
            return;
        }

        setSidebarElementOrder(selectedPage);
        selectedPage.writeSelectableCategories();
        // Save
        boolean success = false;
        selectedPage.setDateUpdated(LocalDateTime.now());

        logger.trace("update dao");
        if (selectedPage.getId() != null) {
            success = this.dao.updateCMSPage(selectedPage);
        } else {
            success = this.dao.addCMSPage(selectedPage);
        }

        if (saveAsTemplate) {
            success = saveTemplate(selectedPage, templateName, templateLockComponents);
            if (success) {
                saveAsTemplate = false;
                this.templateLockComponents = false;
                this.templateName = "";
            }
        }

        if (success) {
            Messages.info("cms_pageSaveSuccess");
            logger.trace("reload cms page");
            logger.trace("update pages");
            cmsBean.getLazyModelPages().update();

            // Add CMS page metadata to search index
            if (selectedPage.isSearchable() && selectedPage.isPublished()) {
                if (StringUtils.isNotEmpty(selectedPage.getRelatedPI())) {
                    // Re-index related record text as part of the record
                    try {
                        IndexerTools.reIndexRecord(selectedPage.getRelatedPI());
                        Messages.info("admin_recordReExported");
                    } catch (RecordNotFoundException e) {
                        logger.error(e.getMessage());
                    }
                } else {
                    // Index CMS page metadata and texts as standalone docs
                    IndexerTools.triggerReIndexCMSPage(selectedPage, null);
                }
            }

            // Delete CMS page metadata from index if page is not published
            if (!selectedPage.isPublished() || !selectedPage.isSearchable()) {
                deletePageMetadataFromIndex(selectedPage, selectedPage.getId());
            }

        } else {
            Messages.error("cms_pageSaveFailure");
        }
        logger.trace("reset collections");
        this.collectionViewBean.removeCollectionsForPage(selectedPage);
        if (navigationBean != null) {
            logger.trace("add navigation item");
            navigationBean.getItemManager().addAvailableItem(new SelectableNavigationItem(this.selectedPage));
        }
        logger.trace("Done saving page");
    }

    private boolean saveTemplate(CMSPage page, String name, boolean lockComponents) throws DAOException {
        CMSPageTemplate template = new CMSPageTemplate(page);
        TranslatedText title = new TranslatedText(IPolyglott.getLocalesStatic());
        title.setText(name, IPolyglott.getDefaultLocale());
        template.setTitleTranslations(title);
        template.setLockComponents(lockComponents);
        template.setPublished(true);
        return this.dao.addCMSPageTemplate(template);
    }

    /**
     * Action method for deleting selectedPage from the database.
     *
     * @return Return view
     * @throws io.goobi.viewer.exceptions.DAOException if any.
      * @should delete page for given input
     */
    public String deleteSelectedPage() throws DAOException {
        if (deletePage(selectedPage)) {
            selectedPage = null;
        }

        return "cmsOverview";
    }

    /**
     * Deletes given CMS page from the database.
     *
     * @param page Page to delete
     * @return true if deletion was successful; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should delete page for given input
     */
    public boolean deletePage(CMSPage page) throws DAOException {
        if (this.dao == null || page == null || page.getId() == null) {
            return false;
        }

        logger.info("Deleting CMS page: {}", page);

        if (!page.isComponentsLoaded()) {
            page.initialiseCMSComponents(this.templateManager);
        }

        List<CMSComponent> components = new ArrayList<>(page.getComponents());
        for (CMSComponent component : components) {
            PersistentCMSComponent persistentComponent = component.getPersistentComponent();
            List<CMSContentItem> contentItems = new ArrayList<>(component.getContentItems());
            for (CMSContentItem contentItem : contentItems) {
                CMSContent content = contentItem.getContent();
                component.removeContentItem(contentItem);
                dao.deleteCMSContent(content);
            }
            page.removeComponent(component);
            dao.deleteCMSComponent(persistentComponent);
        }
        Long pageId = page.getId(); // This is gone after deleting
        if (this.dao.deleteCMSPage(page)) {
            // Delete files matching content item IDs of the deleted page and re-index record
            try {
                if (page.deleteExportedTextFiles() > 0) {
                    try {
                        IndexerTools.reIndexRecord(page.getRelatedPI());
                        logger.debug("Re-indexing record: {}", page.getRelatedPI());
                    } catch (RecordNotFoundException e) {
                        logger.error(e.getMessage());
                    }
                }
            } catch (ViewerConfigurationException e) {
                logger.error(e.getMessage());
                Messages.error(e.getMessage());
            }
            // Delete page metadata from the index
            deletePageMetadataFromIndex(page, pageId);
            cmsBean.getLazyModelPages().update();
            Messages.info("cms_deletePage_success");
            return true;
        }
        Messages.error("cms_deletePage_failure");

        return false;
    }

    /**
     * 
     * @param page CMS page whose index entry is to be deleted
     * @param pageId Numeric ID of the CMS page
     */
    static void deletePageMetadataFromIndex(CMSPage page, Long pageId) {
        if (page == null) {
            throw new IllegalArgumentException("page may not be null");
        }
        if (pageId == null) {
            throw new IllegalArgumentException("pageId may not be null");
        }

        try {
            String pi = "CMS" + pageId;
            if (DataManager.getInstance().getSearchIndex().getHitCount(SolrConstants.PI + ":\"" + pi + '"') > 0) {
                IndexerTools.deleteRecord("CMS" + page.getId(), false,
                        Paths.get(DataManager.getInstance().getConfiguration().getHotfolder()));
                logger.debug("Page contents will be deleted from index: {}", pi);
            } else {
                logger.trace("Page not in index, no deletion necessary: {}", pi);
            }
        } catch (IOException | IndexUnreachableException | PresentationException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Setter for the field <code>selectedPage</code>.
     *
     * @param currentPage CMS page to select; null clears the selection
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void setSelectedPage(CMSPage currentPage) throws DAOException {
        if (currentPage != null) {
            if (currentPage.getId() != null) {
                //get page from DAO
                this.selectedPage = new CMSPage(this.dao.getCMSPage(currentPage.getId()));
            } else {
                this.selectedPage = currentPage;
            }
            this.selectedPage.initialiseCMSComponents(templateManager);
            logger.debug("Selected page: {}", currentPage);
        } else {
            this.selectedPage = null;
        }

    }

    /**
     * getSelectedPageId.
     *
     * @return ID of selectedPage
     */
    public String getSelectedPageId() {
        if (selectedPage == null) {
            return null;
        }

        return String.valueOf(selectedPage.getId());
    }

    /**
     * setSelectedPageId.
     *
     * @param id string representation of the CMS page database ID
     * @throws io.goobi.viewer.exceptions.DAOException
     */
    public void setSelectedPageId(String id) throws DAOException {
        logger.trace("setSelectedPageId: {}", id);
        CMSPage page = cmsBean.findPage(id);
        setSelectedPage(page);
    }

    /**
     * Getter for the field <code>selectedPage</code>.
     *
     * @return the CMS page currently selected for editing
     * @should new page
     * @should new page from template
     * @should new page from template with title and pi
     * @should edit page
     * @should save page
     */
    public CMSPage getSelectedPage() {
        return selectedPage;
    }

    /**
     * Create a new CMSPage based on the given template. title and relatedPI are set on the page if given Opens the view to create/edit the cmsPage
     *
     * @param title The title to be used for the current locale, optional
     * @param relatedPI The PI of a related work, optional
     * @return the absolute URL to the new CMS page creation view with optional title and PI query parameters
     */
    public String createAndOpenNewPage(String title, String relatedPI) {

        String createPageUrl = PrettyUrlTools.getAbsolutePageUrl("adminCmsNewPage");
        URI uri = UriBuilder.fromUri(createPageUrl).queryParam("title", title).queryParam("relatedPi", relatedPI).build();
        return uri.toString();

        //        CMSPage page = new CMSPage();
        //        page.getTitleTranslations().setValue(title, IPolyglott.getDefaultLocale());
        //        page.setRelatedPI(relatedPI);
        //        setUserRestrictedValues(page, userBean.getUser());
        //        setSelectedPage(page);
        //        return "pretty:adminCmsNewPage";
    }

    private static void setSidebarElementOrder(CMSPage page) {
        for (int i = 0; i < page.getSidebarElements().size(); i++) {
            page.getSidebarElements().get(i).setOrder(i);
        }
    }

    /**
     * Getter for the field <code>sidebarWidgets</code>.
     *
     * @return a map of sidebar widget display elements to their selection state
     */
    public Map<WidgetDisplayElement, Boolean> getSidebarWidgets() {
        return sidebarWidgets;
    }

    /**
     * Setter for the field <code>sidebarWidgets</code>.
     *
     * @param sidebarWidgets map of widgets to their selection state
     */
    public void setSidebarWidgets(Map<WidgetDisplayElement, Boolean> sidebarWidgets) {
        this.sidebarWidgets = sidebarWidgets;
    }

    /**
     * getSelectedWidgets.
     *
     * @return a list of sidebar widget display elements that are currently selected for the CMS page
     */
    public List<WidgetDisplayElement> getSelectedWidgets() {
        return this.sidebarWidgets.entrySet().stream().filter(Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    /**
     * resetSelectedWidgets.
     */
    public void resetSelectedWidgets() {
        this.sidebarWidgets.entrySet().forEach(e -> e.setValue(false));
    }

    /**
     * getAndResetSelectedWidgets.
     *
     * @return a list of the currently selected sidebar widgets, after which all selections are cleared
     */
    public List<WidgetDisplayElement> getAndResetSelectedWidgets() {
        List<WidgetDisplayElement> selected = getSelectedWidgets();
        resetSelectedWidgets();
        return selected;
    }

    /**
     * Getter for the field <code>selectedComponent</code>.
     *
     * @return the template filename of the selected component to add
     */
    public String getSelectedComponent() {
        return selectedComponent;
    }

    /**
     * Setter for the field <code>selectedComponent</code>.
     *
     * @param selectedComponent template filename of the component to add
     */
    public void setSelectedComponent(String selectedComponent) {
        this.selectedComponent = selectedComponent;
    }

    /**
     * getAvailableComponents.
     *
     * @param page CMS page used to filter out incompatible paged components
     * @return a list of select items representing CMS components available for the given page, grouped by type
     */
    public List<SelectItem> getAvailableComponents(CMSPage page) {
        Stream<CMSComponent> stream = templateManager.getContentManager().getComponents().stream();
        boolean hidePagedComponents = page != null && page.isContainsPagedComponents();
        Locale locale = BeanUtils.getLocale();
        List<CMSComponent> components = stream
                .sorted((c1, c2) -> Strings.CS.compare(ViewerResourceBundle.getTranslation(c1.getLabel(), locale),
                        ViewerResourceBundle.getTranslation(c2.getLabel(), locale)))
                .collect(Collectors.toList());
        Map<String, List<CMSComponent>> sortedMap = SelectItemBuilder.getAsSortedMap(components,
                component -> component.getTypes(), label -> ViewerResourceBundle.getTranslation("label__cms_component_type__" + label, locale));
        return SelectItemBuilder.getAsGroupedSelectItems(sortedMap, CMSComponent::getTemplateFilename,
                c -> ViewerResourceBundle.getTranslation(c.getLabel(), locale),
                c -> ViewerResourceBundle.getTranslation(c.getDescription(), locale),
                c -> (hidePagedComponents && c.isPaged()));
    }

    /**
     * Get the list of metadata fields which may be displayed. This is the main metadata list
     *
     * @return the main metadata list
     */
    public List<String> getAvailableMetadataFields() {
        return DataManager.getInstance()
                .getConfiguration()
                .getMainMetadataForTemplate(0, null)
                .stream()
                .map(Metadata::getLabel)
                .map(md -> md.replaceAll("_LANG_.*", ""))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * mayRemoveCategoryFromPage.
     *
     * @param cat category whose removal eligibility is being checked
     * @return false only if the user has limited privileges for categories and only one category is set for the selected page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean mayRemoveCategoryFromPage(CMSCategory cat) throws DAOException {
        if (this.selectedPage != null) {
            return userBean.getUser().hasPrivilegeForAllCategories()
                    || this.selectedPage.getSelectableCategories().stream().anyMatch(Selectable::isSelected);
        }

        return true;
    }

    /**
     * isEditMode.
     *
     * @return true if an existing CMS page is being edited (as opposed to creating a new one), false otherwise
     */
    public boolean isEditMode() {
        return editMode;
    }

    /**
     * Setter for the field <code>editMode</code>.
     *
     * @param editMode true when editing an existing page; false for new pages
     */
    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    /**
     * setNewSelectedPage.
     */
    public void setNewSelectedPage() {
        this.selectedPage = new CMSPage();
    }

    /**
     * setNewSelectedPage.
     *
     * @param templateId database ID of the page template to base the new page on
     */
    public void setNewSelectedPage(Long templateId) {
        CMSPageTemplate template = loadTemplate(templateId);
        if (template == null) {
            this.selectedPage = new CMSPage();
        } else {
            this.selectedPage = new CMSPage(template);
            this.selectedPage.initialiseCMSComponents(templateManager);
        }
    }

    private CMSPageTemplate loadTemplate(Long templateId) {
        if (templateId != null) {
            try {
                CMSPageTemplate template = this.dao.getCMSPageTemplate(templateId);
                if (template != null) {
                    template.initialiseCMSComponents(templateManager);
                }
                return template;
            } catch (DAOException e) {
                logger.error("Error loading cms page template with id {}: {}", templateId, e.toString());
            }
        }
        return null;
    }

    /**
     * Getter for the field <code>pageEditState</code>.
     *
     * @return the current state of the CMS page edit UI
     */
    public CMSPageEditState getPageEditState() {
        return pageEditState;
    }

    /**
     * Setter for the field <code>pageEditState</code>.
     *
     * @param pageEditState active section of the page edit UI
     */
    public void setPageEditState(CMSPageEditState pageEditState) {
        this.pageEditState = pageEditState;
    }

    /**
     * deleteComponent.
     *
     * @param component CMS component to remove from the selected page
     * @return true if the component was successfully removed from the page, false otherwise
     * @should return true for given input
     */
    public boolean deleteComponent(CMSComponent component) {
        return this.selectedPage.removeComponent(component);
    }

    /**
     * addComponent.
     * @should return true for given input
     */
    public void addComponent() {
        if (addComponent(getSelectedPage(), getSelectedComponent())) {
            setSelectedComponent(null);
        }
    }

    private boolean addComponent(CMSPage page, String componentFilename) {
        if (page != null) {
            if (StringUtils.isNotBlank(componentFilename)) {
                try {
                    page.addComponent(componentFilename, templateManager);
                    return true;
                } catch (IllegalArgumentException e) {
                    logger.error("Cannot add component: No component found for filename {}.", componentFilename);
                    Messages.error(null, "cms__create_page__error_unknown_component_name", componentFilename);
                }
            } else {
                logger.error("Cannot add component: No component filename given");
                Messages.error("cms__create_page__error_no_component_name_given");
            }
        } else {
            logger.error("Cannot add component: No page given");
        }
        return false;
    }

    /**
     * Fills all properties of the page with values for which the user has privileges - but only if the user has restricted privileges for that
     * property.
     *
     * @param page CMS page whose restricted fields are to be set
     * @param user User whose privilege constraints are applied
     * @throws PresentationException
     * @throws DAOException
     */
    private void setUserRestrictedValues(CMSPage page, User user) throws PresentationException, DAOException {
        if (!user.hasPrivilegeForAllSubthemeDiscriminatorValues()) {
            List<String> allowedSubThemeDiscriminatorValues = user.getAllowedSubthemeDiscriminatorValues(cmsBean.getSubthemeDiscriminatorValues());
            if (StringUtils.isBlank(page.getSubTheme()) && !allowedSubThemeDiscriminatorValues.isEmpty()) {
                page.setSubTheme(allowedSubThemeDiscriminatorValues.get(0));
            } else {
                logger.error("User has no access to any subtheme discriminator values and can therefore not create a page");
                //do something??
            }
        }
        if (!user.hasPrivilegeForAllCategories()) {
            List<CMSCategory> allowedCategories = user.getAllowedCategories(cmsBean.getAllCategories());
            if (page.getCategories().isEmpty() && !allowedCategories.isEmpty()) {
                // Use a defensive copy instead of a raw subList view: User.getAllowedCategories()
                // can return the original allCategories list directly (for superusers/full-access),
                // so subList(0,1) would be a live view of a shared list. If that list is modified
                // by a concurrent request, JSF's ListDataModel.isRowAvailable() throws
                // ConcurrentModificationException during rendering.
                page.setCategories(new ArrayList<>(allowedCategories.subList(0, 1)));
            }
        }

    }

    /**
     * Setter for the field <code>saveAsTemplate</code>.
     *
     * @param saveAsTemplate true to save the current page as a template after saving
     */
    public void setSaveAsTemplate(boolean saveAsTemplate) {
        this.saveAsTemplate = saveAsTemplate;
    }

    /**
     * isSaveAsTemplate.
     *
     * @return true if the current page should be saved as a template, false otherwise
     */
    public boolean isSaveAsTemplate() {
        return saveAsTemplate;
    }

    /**
     * Setter for the field <code>templateName</code>.
     *
     * @param templateName display name for the template to be created
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    /**
     * Getter for the field <code>templateName</code>.
     *
     * @return the template name, falling back to the selected page title if blank
     */
    public String getTemplateName() {
        if (StringUtils.isBlank(this.templateName)) {
            return getSelectedPage().getTitle(IPolyglott.getDefaultLocale());
        }
        return templateName;
    }

    /**
     * isTemplateLockComponents.
     *
     * @return true if components are locked when the page template is applied, false otherwise
     */
    public boolean isTemplateLockComponents() {
        return templateLockComponents;
    }

    /**
     * Setter for the field <code>templateLockComponents</code>.
     *
     * @param templateLockComponents true to prevent component editing in pages based on this template
     */
    public void setTemplateLockComponents(boolean templateLockComponents) {
        this.templateLockComponents = templateLockComponents;
    }

    /**
     * Getter for unit tests.
     * 

     */
    IDAO getDao() {
        return dao;
    }

    /**
     * Setter for unit tests.
     * 

     */
    void setDao(IDAO dao) {
        this.dao = dao;
    }

    /**
     * Setter for unit tests.
     * 

     */
    void setTemplateManager(CMSTemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    /**
     * Setter for unit tests.
     * 

     */
    void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /**
     * Setter for unit tests.
     * 

     */
    void setWidgetsBean(CMSSidebarWidgetsBean widgetsBean) {
        this.widgetsBean = widgetsBean;
    }

    /**
     * Getter for unit tests.
     * 

     */
    CollectionViewBean getCollectionViewBean() {
        return collectionViewBean;
    }

    /**
     * Setter for unit tests.
     * 

     */
    void setCollectionViewBean(CollectionViewBean collectionViewBean) {
        this.collectionViewBean = collectionViewBean;
    }

    /**
     * Setter for unit tests.
     *

     */
    void setCmsBean(CmsBean cmsBean) {
        this.cmsBean = cmsBean;
    }

    /**
     * Setter for unit tests.
     *

     */
    void setFacesContext(FacesContext facesContext) {
        this.facesContext = facesContext;
    }
}
