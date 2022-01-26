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
package io.goobi.viewer.model.jsf;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.view.facelets.FaceletContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.maps.GeoMap;

/**
 * @author florian
 *
 */
public class DynamicContentBuilder {

    private final static Logger logger = LoggerFactory.getLogger(DynamicContentBuilder.class);

    private FacesContext context = FacesContext.getCurrentInstance();
    private Application application = context.getApplication();
    private FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);

    public DynamicContentBuilder() {

    }

    public UIComponent build(DynamicContent content, UIComponent parent) {
        UIComponent composite = null;
        switch (content.getType()) {
            case GEOMAP:
                String id = (String)content.getAttributes().get("geoMapId");
                try {
                    if (id != null && id.matches("\\d+")) {
                        GeoMap map = DataManager.getInstance().getDao().getGeoMap(Long.parseLong(id));
                        if (map != null) {
                            composite = loadCompositeComponent(parent, content.getComponentFilename(), "components");
                            composite.getAttributes().put("geoMap", map);
                            //if query param linkTarget is given, open links from map in that target,
                            //otherwise open them in a new tab
                            String linkTarget = (String)content.getAttributes().get("linkTarget");
                            if(StringUtils.isNotBlank(linkTarget)) {
                                composite.getAttributes().put("linkTarget", linkTarget);
                            } else {
                                composite.getAttributes().put("linkTarget", "_blank");
                            }
                        } else {
                            logger.error("Cannot build GeoMap content. No map found with id = " + id);
                        }
                    } else {
                        logger.error("Cannot build GeoMap content. Need map id as first attribute");
                    }
                } catch (NumberFormatException | DAOException e) {
                    logger.error("Error retrieving content from DAO", e);
                }
                break;
            case SLIDER:
                Long sliderId = (Long)content.getAttributes().get("sliderId");
                try {
                    if (sliderId != null) {
                        CMSSlider slider = DataManager.getInstance().getDao().getSlider(sliderId);
                        if (slider != null) {
                            composite = loadCompositeComponent(parent, content.getComponentFilename() , "components");
                            composite.getAttributes().put("slider", slider);
                            String linkTarget = (String)content.getAttributes().get("linkTarget");
                            if(StringUtils.isNotBlank(linkTarget)) {
                                composite.getAttributes().put("linkTarget", linkTarget);
                            } else {
                                composite.getAttributes().put("linkTarget", "_blank");
                            }
                        } else {
                            logger.error("Cannot build content. No item found with id = " + sliderId);
                        }
                    } else {
                        logger.error("Cannot build content. Need item id as first attribute");
                    }
                } catch (NumberFormatException | DAOException e) {
                    logger.error("Error retrieving content from DAO", e);
                }
                break;
            case WIDGET:
                composite = loadCompositeComponent(parent, content.getComponentFilename(), "components/widgets");
                if(content.getAttributes().containsKey("widget")) {                    
                    CustomSidebarWidget widget = (CustomSidebarWidget) content.getAttributes().get("widget");
                    composite.getAttributes().put("widget", widget);
                }
                
        }
        if (composite != null) {
            composite.setId(content.getId());
        }
        return composite;
    }

    /**
     * @param string2
     * @param string
     * @return
     */
    private UIComponent loadCompositeComponent(UIComponent parent, String name, String library) {
        Resource componentResource = context.getApplication().getResourceHandler().createResource(name, library);
        UIComponent composite = application.createComponent(context, componentResource);

        // This basically creates <composite:implementation>.
        UIComponent implementation = application.createComponent(UIPanel.COMPONENT_TYPE);
        implementation.setRendererType("javax.faces.Group");
        composite.getFacets().put(UIComponent.COMPOSITE_FACET_NAME, implementation);
        parent.getChildren().add(composite);
        parent.pushComponentToEL(context, composite); // This makes #{cc} available.
        try {
            faceletContext.includeFacelet(implementation, componentResource.getURL());
        } catch (IOException e) {
            throw new FacesException(e);
        } finally {
            parent.popComponentFromEL(context);
        }
        return composite;
    }
    
    private UIComponent createTag(String name, Map<String, String> attributes) {
        UIComponent component = new UIComponentBase() {
            
            @Override
            public void encodeBegin(FacesContext context) throws IOException {
                ResponseWriter out = context.getResponseWriter();
                out.append("\n\n");
                out.startElement(name, null);
                for (Entry<String, String> entry : attributes.entrySet()) {
                    out.writeAttribute(entry.getKey(), entry.getValue(), null);
                }
            }

            @Override
            public void encodeEnd(FacesContext context) throws IOException {
                ResponseWriter out = context.getResponseWriter();
                out.endElement(name);
            }
            
            @Override
            public String getFamily() {
                return null;
            }
        };
        return component;
    }
    
    /**
     * @param content
     * @param headGroup
     * @return
     */
    public Optional<UIComponent> buildHead(DynamicContent content, HtmlPanelGroup parent) {
        UIComponent component = null;
        switch (content.getType()) {
            case GEOMAP:
                String id = (String)content.getAttributes().get("geoMapId");
                try {
                    if (id != null && id.matches("\\d+")) {
                        GeoMap map = DataManager.getInstance().getDao().getGeoMap(Long.parseLong(id));
                        if (map != null) {
                            
                            Map<String, String> attributes = new LinkedHashMap<>();
                            attributes.put("rel", "alternate");
                            attributes.put("type", "application/json+oembed");
                            attributes.put("href", map.getOEmbedURI().toString());
                            attributes.put("title", "Goobi viewer oEmbed Profile");
                            component = createTag("link", attributes);
                            parent.getChildren().add(component);
                            
                        } else {
                            logger.error("Cannot build GeoMap content. No map found with id = " + id);
                        }
                    } else {
                        logger.error("Cannot build GeoMap content. Need map id as first attribute");
                    }
                } catch (NumberFormatException | DAOException e) {
                    logger.error("Error retrieving content from DAO", e);
                }
                break;
        }
        return Optional.ofNullable(component);
    }
    
    public DynamicContent createContent(String id, DynamicContentType type, Map<String, Object> attributes ) {
            DynamicContent content = new DynamicContent(type, getFilenameForType(type));
            content.setId(id);
            content.setAttributes(attributes);
            return content;
    }

    public static String getFilenameForType(DynamicContentType type) {
        switch(type) {
            case GEOMAP:
                return "geoMap.xhtml";
            case SLIDER:
                return "cmsSlider.xhtml";
            default:
                return "";
        }
    }
}
