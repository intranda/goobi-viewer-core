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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSPageContentManager;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;

@Named
@ViewScoped
public class CMSPageTemplateEditBean implements Serializable {


    private static final long serialVersionUID = 1399752926754065793L;
    
    private CMSPageTemplate selectedTemplate = null;
    private Map<WidgetDisplayElement, Boolean> sidebarWidgets;
    private final IDAO dao;
    private final CMSPageContentManager contentManager;
    
    
    @Inject
    public CMSPageTemplateEditBean(CMSSidebarWidgetsBean widgetsBean) throws DAOException {
            this.sidebarWidgets = widgetsBean.getAllWidgets().stream().collect(Collectors.toMap(Function.identity(), w -> Boolean.FALSE));
            this.dao = DataManager.getInstance().getDao();
            this.contentManager = CMSTemplateManager.getInstance().getContentManager();
    }

    public Map<WidgetDisplayElement, Boolean> getSidebarWidgets() {
        return sidebarWidgets;
    }

    public void setSidebarWidgets(Map<WidgetDisplayElement, Boolean> sidebarWidgets) {
        this.sidebarWidgets = sidebarWidgets;
    }

    public List<WidgetDisplayElement> getSelectedWidgets() {
        return this.sidebarWidgets.entrySet().stream().filter(e -> e.getValue()).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public void resetSelectedWidgets() {
        this.sidebarWidgets.entrySet().forEach(e -> e.setValue(false));
    }

    public List<WidgetDisplayElement> getAndResetSelectedWidgets() {
        List<WidgetDisplayElement> selected = getSelectedWidgets();
        resetSelectedWidgets();
        return selected;
    }

    public List<CMSComponent> getAvailableComponents() {
        return this.contentManager.getComponents();
    }
    
    public void setSelectedTemplate(CMSPageTemplate selectedTemplate) {
        this.selectedTemplate = new CMSPageTemplate(selectedTemplate);
    }
    
    public CMSPageTemplate getSelectedTemplate() {
        return selectedTemplate;
    }
    
    public void setSelectedTemplateId(Long id) throws DAOException {
        CMSPageTemplate template = this.dao.getCMSPageTemplate(id);
        if(template == null) {
            throw new DAOException("No cms page template with id " + id + " found in DAO");
        } else {
            this.setSelectedTemplate(template);
        }
    }
    
    public void setNewSelectedTemplate() {
        this.selectedTemplate = new CMSPageTemplate();
    }
}
