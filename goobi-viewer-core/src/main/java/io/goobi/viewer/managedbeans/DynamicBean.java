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
import io.goobi.viewer.model.maps.GeoMap;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class DynamicBean implements Serializable {

    private final static Logger logger = LoggerFactory.getLogger(DynamicBean.class);
    
    private FacesContext context = FacesContext.getCurrentInstance();
    private Application application = context.getApplication();
    private FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
    
    private HtmlPanelGroup formGroup = null;

    public void setFormGroup(HtmlPanelGroup group) {
        this.formGroup = group;
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
     * @throws DAOException
     * 
     */
    private void loadFormGroup() throws DAOException {

        logger.debug("Load form group");


        Resource componentResource = context.getApplication().getResourceHandler().createResource("geoMap.xhtml", "components");
        UIComponent composite = application.createComponent(context, componentResource);
        composite.setId("geoMapComponent"); // Mandatory for the case composite is part of UIForm! Otherwise JSF can't find inputs.

        // This basically creates <composite:implementation>.
        UIComponent implementation = application.createComponent(UIPanel.COMPONENT_TYPE);
        implementation.setRendererType("javax.faces.Group");
        composite.getFacets().put(UIComponent.COMPOSITE_FACET_NAME, implementation); 

        this.formGroup = new HtmlPanelGroup();
        this.formGroup.getChildren().add(composite);
        this.formGroup.pushComponentToEL(context, composite); // This makes #{cc} available.
        try {
            faceletContext.includeFacelet(implementation, componentResource.getURL());
        } catch (IOException e) {
            throw new FacesException(e);
        } finally {
            this.formGroup.popComponentFromEL(context);
        }
        
        GeoMap map = DataManager.getInstance().getDao().getAllGeoMaps().get(0);
        composite.getAttributes().put("geoMap", map);
//        HtmlOutputText out = new HtmlOutputText();
//        out.setValue("HALLO WELT");
//        this.formGroup.getChildren().add(out);

    }

}
