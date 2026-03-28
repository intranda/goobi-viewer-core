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
package io.goobi.viewer.model.jsf;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.CMSSlider;
import io.goobi.viewer.model.maps.GeoMap;
import jakarta.el.ELException;
import jakarta.faces.FacesException;
import jakarta.faces.application.Application;
import jakarta.faces.application.Resource;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UIPanel;
import jakarta.faces.component.html.HtmlPanelGroup;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.view.facelets.FaceletContext;
import jakarta.faces.view.facelets.FaceletException;

/**
 * @author florian
 *
 */
public class DynamicContentBuilder {

    private static final Logger logger = LogManager.getLogger(DynamicContentBuilder.class);

    private FacesContext context = FacesContext.getCurrentInstance();
    private Application application = context.getApplication();
    private FaceletContext faceletContext = (FaceletContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);

    public UIComponent build(JsfComponent jsfComponent, UIComponent parent, Map<String, Object> attributes) throws PresentationException {
        try {
            UIComponent composite = loadCompositeComponent(parent, jsfComponent.getFilename(), jsfComponent.getLibrary(), attributes);
            return composite;
        } catch (FaceletException e) {
            throw new PresentationException(
                    "error building jsf custom component from file " + jsfComponent.toString() + ".\nCause: " + e.getMessage());
        } catch (NullPointerException | IllegalArgumentException | FacesException e) {
            throw new PresentationException("error building jsf custom component from file " + jsfComponent.toString()
                    + ": " + e.toString()
                    + ". Please check if the file exists and is a valid jsf composite component");
        }
    }

    /**
     * 
     * @param content
     * @param parent
     * @return {@link UIComponent}
     */
    public UIComponent build(DynamicContent content, UIComponent parent) {
        UIComponent composite = null;
        switch (content.getType()) {
            case GEOMAP:
                String id = (String) content.getAttributes().get("geoMapId");
                try {
                    if (id != null && id.matches("\\d+")) {
                        GeoMap map = DataManager.getInstance().getDao().getGeoMap(Long.parseLong(id));
                        if (map != null) {

                            Map<String, Object> attributes = new HashMap<>();
                            attributes.put("geoMap", map);
                            //if query param linkTarget is given, open links from map in that target,
                            //otherwise open them in a new tab
                            String linkTarget = (String) content.getAttributes().get("linkTarget");
                            if (StringUtils.isNotBlank(linkTarget)) {
                                attributes.put("linkTarget", linkTarget);
                            } else {
                                attributes.put("linkTarget", "_blank");
                            }
                            attributes.put("popoverOnHover", map.shouldOpenPopoversOnHover());

                            composite = loadCompositeComponent(parent, content.getComponentFilename(), "components", attributes);
                        } else {
                            logger.error("Cannot build GeoMap content. No map found with id = {}", id);
                        }
                    } else {
                        logger.error("Cannot build GeoMap content. Need map id as first attribute");
                    }
                } catch (NumberFormatException | DAOException e) {
                    logger.error("Error retrieving content from DAO", e);
                }
                break;
            case SLIDER:
                String sliderId = (String) content.getAttributes().get("sliderId");
                try {
                    if (sliderId != null) {
                        Map<String, Object> attributes = new HashMap<>();
                        for (Entry<String, Object> entry : content.getAttributes().entrySet()) {
                            if (StringUtils.isNotBlank(entry.getKey()) && entry.getValue() != null && !"sliderId".equals(entry.getKey())) {
                                attributes.put(entry.getKey(), entry.getValue());
                            } else if ("sliderId".equals(entry.getKey())) {
                                CMSSlider slider = DataManager.getInstance().getDao().getSlider(Long.parseLong(sliderId));
                                if (slider != null) {
                                    attributes.put("slider", slider);
                                }
                            }
                            String linkTarget = (String) composite.getAttributes().get("linkTarget");
                            if (StringUtils.isBlank(linkTarget)) {
                                attributes.put("linkTarget", "_blank");
                            }
                        }
                        composite = loadCompositeComponent(parent, content.getComponentFilename(), "components", attributes);
                    } else {
                        logger.error("Cannot build content. Need item id as first attribute");
                    }
                } catch (NumberFormatException | DAOException e) {
                    logger.error("Error retrieving content from DAO", e);
                }
                break;
            case WIDGET:
                Map<String, Object> attributes = new HashMap<>();
                for (String attribute : content.getAttributes().keySet()) {
                    attributes.put(attribute, content.getAttributes().get(attribute));
                }
                composite = loadCompositeComponent(parent, content.getComponentFilename(), "components/widgets", attributes);
                break;
            default:
                break;
        }
        if (composite != null) {
            composite.setId(content.getId());
        }
        return composite;
    }

    /**
     * @param parent
     * @param name
     * @param library
     * @return {@link UIComponent}
     */
    private UIComponent loadCompositeComponent(UIComponent parent, String name, String library, Map<String, Object> attributes)
            throws FacesException {
        Resource componentResource = context.getApplication().getResourceHandler().createResource(name, library);
        if (componentResource == null) {
            return null;
        }
        UIComponent composite = application.createComponent(context, componentResource);
        composite.setId(parent.getId() + "_" + FilenameUtils.getBaseName(name));
        // This basically creates <composite:implementation>.
        UIComponent implementation = application.createComponent(UIPanel.COMPONENT_TYPE);
        implementation.setRendererType("jakarta.faces.Group");
        composite.getFacets().put(UIComponent.COMPOSITE_FACET_NAME, implementation);
        parent.getChildren().add(composite);

        // Snapshot the EL component stack top *before* our push so the finally block
        // can drain the stack back to exactly this state. When includeFacelet is
        // interrupted by an exception, Mojarra may leave extra pushComponentToEL
        // entries on the stack that its own cleanup code never popped. If those stale
        // entries remain, #{cc} resolves to a broken composite for every subsequent
        // widget load in the same request, eventually causing the
        // ContextualCompositeValueExpression → AttributesMap infinite loop (SOE).
        UIComponent elStackTopBeforePush = UIComponent.getCurrentComponent(context);
        parent.pushComponentToEL(context, composite); // This makes #{cc} available.

        if (composite != null && attributes != null) {
            for (Entry<String, Object> entry : attributes.entrySet()) {
                composite.getAttributes().put(entry.getKey(), entry.getValue());
            }
        }

        try {
            faceletContext.includeFacelet(implementation, componentResource.getURL());
        } catch (IOException | NullPointerException e) {
            parent.getChildren().remove(composite);
            throw new FacesException(e);
        } catch (FacesException | ELException e) {
            // FaceletException (a FacesException subclass) is thrown when Facelets tag
            // processing fails for non-EL reasons (e.g. Solr unreachable wrapped in a
            // PresentationException propagating through a composite's tag handler).
            // ELException covers failures that occur directly during EL evaluation.
            // In both cases the composite is partially initialised and must be removed;
            // returning it would cause Mojarra's CompositeComponentAttributesELResolver
            // to enter an infinite loop (StackOverflowError) during the render phase.
            logger.error("Error rendering composite", e);
            parent.getChildren().remove(composite);
            return null;
        } finally {
            // Restore the EL component stack to its pre-push state regardless of
            // whether includeFacelet succeeded or was interrupted. On the normal
            // success path this removes exactly the one entry we pushed; on failure
            // it also drains any additional entries leaked by the aborted processing.
            restoreELComponentStack(elStackTopBeforePush);
        }
        return composite;
    }

    /**
     * Drains the JSF EL component stack until the component at the top equals
     * {@code targetTop}. Called from the finally block of
     * {@link #loadCompositeComponent} to recover from an interrupted
     * {@link FaceletContext#includeFacelet} call that may have left stale
     * composite component references on the stack.
     *
     * @param targetTop the component that should be at the top after draining;
     *                  {@code null} means the stack should be empty
     */
    private void restoreELComponentStack(UIComponent targetTop) {
        UIComponent current;
        int safetyLimit = 1000;
        while ((current = UIComponent.getCurrentComponent(context)) != targetTop && safetyLimit-- > 0) {
            if (current == null) {
                break;
            }
            current.popComponentFromEL(context);
        }
    }

    /**
     * 
     * @param name
     * @param attributes
     * @return {@link UIComponent}
     */
    public UIComponent createTag(String name, Map<String, String> attributes) {
        return new UIComponentBase() {

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
    }

    /**
     * @param content
     * @param parent
     * @return Optional<UIComponent>
     */
    public Optional<UIComponent> buildHead(DynamicContent content, HtmlPanelGroup parent) {
        UIComponent component = null;
        switch (content.getType()) {
            case GEOMAP:
                String id = (String) content.getAttributes().get("geoMapId");
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
                            logger.error("Cannot build GeoMap content. No map found with id = {}", id);
                        }
                    } else {
                        logger.error("Cannot build GeoMap content. Need map id as first attribute");
                    }
                } catch (NumberFormatException | DAOException e) {
                    logger.error("Error retrieving content from DAO", e);
                }
                break;
            default:
                break;
        }

        return Optional.ofNullable(component);
    }

    /**
     * 
     * @param id
     * @param type
     * @param attributes
     * @return {@link DynamicContent}
     */
    public DynamicContent createContent(String id, DynamicContentType type, Map<String, Object> attributes) {
        DynamicContent content = new DynamicContent(type, getFilenameForType(type));
        content.setId(id);
        content.setAttributes(attributes);
        return content;
    }

    /**
     * 
     * @param type
     * @return File name
     */
    public static String getFilenameForType(DynamicContentType type) {
        switch (type) {
            case GEOMAP:
                return "geoMap.xhtml";
            case SLIDER:
                return "cmsSlider.xhtml";
            default:
                return "";
        }
    }
}
