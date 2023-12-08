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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.faces.utils.SelectItemBuilder;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPageEditState;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;

@Named("cmsPageTemplateEditBean")
@ViewScoped
public class CMSPageTemplateEditBean implements Serializable {

    private static final long serialVersionUID = 1399752926754065793L;
    private static final Logger logger = LogManager.getLogger(CMSPageTemplateEditBean.class);

    @Inject
    private transient IDAO dao;
    @Inject
    private transient CMSTemplateManager templateManager;
    @Inject
    private transient UserBean userBean;
    @Inject
    private transient CMSSidebarWidgetsBean widgetsBean;
    @Inject
    private transient FacesContext facesContext;

    private CMSPageTemplate selectedTemplate = null;
    private transient Map<WidgetDisplayElement, Boolean> sidebarWidgets;
    private boolean editMode = false;
    private CMSPageEditState pageEditState = CMSPageEditState.CONTENT;
    private String selectedComponent = "";

    @PostConstruct
    public void setup() {
        try {
            long templateId = Long.parseLong(facesContext.getExternalContext().getRequestParameterMap().get("templateId"));
            CMSPageTemplate template = this.dao.getCMSPageTemplate(templateId);
            if (template != null) {
                this.setSelectedTemplate(template);
                this.editMode = true;
            } else {
                this.editMode = false;
                this.setNewSelectedTemplate();
            }
        } catch (NullPointerException | NumberFormatException e) {
            this.editMode = false;
            this.setNewSelectedTemplate();
        } catch (DAOException e) {
            logger.error("Error retrieving cms page template from dao: {}", e.toString());
            this.editMode = false;
            this.setNewSelectedTemplate();
        }
        try {
            this.sidebarWidgets = widgetsBean.getAllWidgets().stream().collect(Collectors.toMap(Function.identity(), w -> Boolean.FALSE));
        } catch (DAOException e) {
            this.sidebarWidgets = Collections.emptyMap();
        }

    }

    public Map<WidgetDisplayElement, Boolean> getSidebarWidgets() {
        return sidebarWidgets;
    }

    public void setSidebarWidgets(Map<WidgetDisplayElement, Boolean> sidebarWidgets) {
        this.sidebarWidgets = sidebarWidgets;
    }

    public List<WidgetDisplayElement> getSelectedWidgets() {
        return this.sidebarWidgets.entrySet().stream().filter(Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public void resetSelectedWidgets() {
        this.sidebarWidgets.entrySet().forEach(e -> e.setValue(false));
    }

    public List<WidgetDisplayElement> getAndResetSelectedWidgets() {
        List<WidgetDisplayElement> selected = getSelectedWidgets();
        resetSelectedWidgets();
        return selected;
    }

    public List<SelectItem> getAvailableComponents(CMSPageTemplate template) {
        Stream<CMSComponent> stream = this.templateManager.getContentManager().getComponents().stream();
        boolean disablePagedComponents = template != null && template.isContainsPagedComponents();
        Locale locale = BeanUtils.getLocale();
        List<CMSComponent> components = stream
                .sorted((c1, c2) -> StringUtils.compare(ViewerResourceBundle.getTranslation(c1.getLabel(), locale),
                        ViewerResourceBundle.getTranslation(c2.getLabel(), locale)))
                .collect(Collectors.toList());
        // TODO StringIndexOutOfBoundsException is thrown here if component label is empty
        Map<String, List<CMSComponent>> sortedMap = SelectItemBuilder.getAsAlphabeticallySortedMap(components,
                component -> ViewerResourceBundle.getTranslation(component.getLabel(), locale));
        return SelectItemBuilder.getAsGroupedSelectItems(sortedMap, CMSComponent::getTemplateFilename,
                c -> ViewerResourceBundle.getTranslation(c.getLabel(), locale), c -> ViewerResourceBundle.getTranslation(c.getDescription(), locale),
                c -> (disablePagedComponents && c.isPaged()));

    }

    public void setSelectedTemplate(CMSPageTemplate selectedTemplate) {
        this.selectedTemplate = new CMSPageTemplate(selectedTemplate);
        this.selectedTemplate.initialiseCMSComponents(templateManager);
        this.editMode = true;
    }

    public CMSPageTemplate getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplateId(Long id) throws DAOException {
        CMSPageTemplate template = this.dao.getCMSPageTemplate(id);
        if (template == null) {
            throw new DAOException("No cms page template with id " + id + " found in DAO");
        }
        this.setSelectedTemplate(template);
    }

    public void setNewSelectedTemplate() {
        this.selectedTemplate = new CMSPageTemplate();
        this.editMode = false;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setPageEditState(CMSPageEditState pageEditState) {
        this.pageEditState = pageEditState;
    }

    public CMSPageEditState getPageEditState() {
        return pageEditState;
    }

    public String getSelectedComponent() {
        return selectedComponent;
    }

    public void setSelectedComponent(String selectedComponent) {
        this.selectedComponent = selectedComponent;
    }

    public void addComponent() {
        if (addComponent(getSelectedTemplate(), getSelectedComponent())) {
            setSelectedComponent(null);
        }
    }

    private boolean addComponent(CMSPageTemplate page, String componentFilename) {
        if (page != null) {
            if (StringUtils.isNotBlank(componentFilename)) {
                try {
                    page.addComponent(componentFilename, templateManager);
                    return true;
                } catch (IllegalArgumentException e) {
                    logger.error("Cannot add component: No component found for filename {}.", componentFilename);
                    Messages.error(null, "admin__cms__create_page__error_unknown_component_name", componentFilename);
                }
            } else {
                logger.error("Cannot add component: No component filename given");
                Messages.error("admin__cms__create_page__error_no_component_name_given");
            }
        } else {
            logger.error("Cannot add component: No page given");
        }
        return false;
    }

    public void saveTemplateAndForwardToEdit() throws DAOException {
        this.saveSelectedTemplate();
        if (this.selectedTemplate.getId() != null) {
            String url = PrettyUrlTools.getAbsolutePageUrl("adminCmsEditPageTemplate", this.selectedTemplate.getId());
            try {
                facesContext.getExternalContext().redirect(url);
            } catch (IOException | NullPointerException e) {
                logger.error("Error redirecting to database url {}: {}", url, e.toString());
            }
        }
    }

    public void saveSelectedTemplate() throws DAOException {
        logger.trace("saveSelectedPage");
        if (userBean == null || !userBean.getUser().isCmsAdmin() || selectedTemplate == null) {
            // Only authorized CMS admins may save
            return;
        }

        setSidebarElementOrder(selectedTemplate);
        selectedTemplate.writeSelectableCategories();
        // Save
        boolean success = false;
        selectedTemplate.setDateUpdated(LocalDateTime.now());

        logger.trace("update dao");
        if (selectedTemplate.getId() != null) {
            success = this.dao.updateCMSPageTemplate(selectedTemplate);
        } else {
            success = this.dao.addCMSPageTemplate(selectedTemplate);
        }
        if (success) {
            Messages.info("cms_pageSaveSuccess");
            logger.trace("reload cms page");
            logger.trace("update pages");
        } else {
            Messages.error("cms_pageSaveFailure");
        }
        logger.trace("Done saving page template");
    }

    private static void setSidebarElementOrder(CMSPageTemplate page) {
        for (int i = 0; i < page.getSidebarElements().size(); i++) {
            page.getSidebarElements().get(i).setOrder(i);
        }
    }

    public String deleteSelectedTemplate() throws DAOException {
        if (deleteTemplate(this.selectedTemplate)) {
            this.selectedTemplate = null;
            return "pretty:adminCmsSelectTemplate";
        }
        return "";
    }

    /**
     * Deletes given CMS page from the database.
     *
     * @param template
     * @retur true if template deleted successfully; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean deleteTemplate(CMSPageTemplate template) throws DAOException {
        if (dao != null && template != null && template.getId() != null) {
            logger.info("Deleting CMS page: {}", template);
            if (dao.removeCMSPageTemplate(template)) {
                Messages.info("cms_deletePageTemplate_success");
                return true;
            }
            logger.error("Failed to delete page");
            Messages.error("cms_deletePageTemplate_failure");
            return false;
        }

        return false;
    }

    /**
     * @return the dao
     */
    public IDAO getDao() {
        return dao;
    }

    /**
     * @param dao the dao to set
     */
    public void setDao(IDAO dao) {
        this.dao = dao;
    }

    /**
     * @param templateManager the templateManager to set
     */
    public void setTemplateManager(CMSTemplateManager templateManager) {
        this.templateManager = templateManager;
    }

    /**
     * @param userBean the userBean to set
     */
    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /**
     * @param widgetsBean the widgetsBean to set
     */
    public void setWidgetsBean(CMSSidebarWidgetsBean widgetsBean) {
        this.widgetsBean = widgetsBean;
    }

    /**
     * @param facesContext the facesContext to set
     */
    public void setFacesContext(FacesContext facesContext) {
        this.facesContext = facesContext;
    }

}
