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
import java.util.List;
import java.util.Optional;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.jsf.DynamicContent;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.DynamicContentType;
import io.goobi.viewer.model.maps.GeoMap;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class DynamicBean implements Serializable {

    private static final long serialVersionUID = -6628922677497179970L;

    private final static Logger logger = LoggerFactory.getLogger(DynamicBean.class);
    
    private List<DynamicContent> components = new ArrayList<>();
    private HtmlPanelGroup formGroup = null;
    private HtmlPanelGroup headGroup = null;

    public void setFormGroup(HtmlPanelGroup group) {
        this.formGroup = group;
    }
    
    /**
     * @param headGroup the headGroup to set
     */
    public void setHeadGroup(HtmlPanelGroup headGroup) {
        this.headGroup = headGroup;
    }

    /**
     * @return the formGroup
     * @throws DAOException
     */
    public HtmlPanelGroup getFormGroup() throws DAOException {
        if (formGroup == null) {
            loadFormGroup();
        }
        return formGroup;
    }
    
    /**
     * @return the formGroup
     * @throws DAOException
     */
    public HtmlPanelGroup getHeadGroup() throws DAOException {
        if (headGroup == null) {
            loadHeadGroup();
        }
        return headGroup;
    }

    /**
     * 
     */
    private void loadHeadGroup() {
        this.headGroup = new HtmlPanelGroup();

        DynamicContentBuilder builder = new DynamicContentBuilder();
        for (DynamicContent content : components) {
             builder.buildHead(content, this.headGroup);
        }
    }

    /**
     * @throws DAOException
     * 
     */
    private void loadFormGroup() throws DAOException {

        this.formGroup = new HtmlPanelGroup();

        DynamicContentBuilder builder = new DynamicContentBuilder();
        for (DynamicContent content : components) {
            UIComponent component = builder.build(content, this.formGroup);
            logger.trace("Added dynamic content " + component);
        }
    }
    
    public void addComponent(String id, String type, String...attributes ) {
        DynamicContentType contentType = DynamicContentType.valueOf(type.toUpperCase());
        try {            
            DynamicContent content = new DynamicContent(contentType);
            content.setId(id);
            content.setAttributes(attributes);
            this.components.add(content);
        } catch(IllegalArgumentException e) {
            logger.error("Cannot resolve dynamic content type " + type);
        }
    }



}
