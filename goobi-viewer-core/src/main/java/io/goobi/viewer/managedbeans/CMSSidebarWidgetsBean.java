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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementAutomatic;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementCustom;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.cms.widgets.type.DefaultWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.jsf.DynamicContent;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.DynamicContentType;
import io.goobi.viewer.model.maps.GeoMap;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.html.HtmlPanelGroup;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * JSF backing bean that manages sidebar widget configuration for CMS pages.
 */
@Named("cmsSidebarWidgetsBean")
@RequestScoped
public class CMSSidebarWidgetsBean implements Serializable {

    private static final String SIDEBAR_ELEMENT_ID_PREFIX = "sidebar_widget_";

    private static final String SIDEBAR_COMPONENT_ATTRIBUTE_GEOMAP = "geoMap";

    private static final String SIDEBAR_COMPONENT_ATTRIBUTE_CMS_PAGE = "cmsPage";

    private static final String SIDEBAR_COMPONENT_ATTRIBUTE_WIDGET = "widget";

    private static final String SIDEBAR_COMPONENT_ATTRIBUTE_SIDEBAR_ELEMENT = "sidebarElement";

    private transient HtmlPanelGroup sidebarGroup = new HtmlPanelGroup();;

    private static final long serialVersionUID = -6039330925483238481L;

    private static final Logger logger = LogManager.getLogger(CMSSidebarWidgetsBean.class);

    public static final int MAX_DESCRIPTION_LENGTH = Integer.MAX_VALUE;

    @Inject
    private CmsBean cmsBean;

    /**
     * getAllWidgets.
     *
     * @return a {@link java.util.List} object
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<WidgetDisplayElement> getAllWidgets() throws DAOException {
        return getAllWidgets(false);
    }

    /**
     * getAllWidgets.
     *
     * @param queryAdditionalInformation if true, queries embedding pages for each widget
     * @return a {@link java.util.List} object
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<WidgetDisplayElement> getAllWidgets(boolean queryAdditionalInformation) throws DAOException {

        List<WidgetDisplayElement> widgets = new ArrayList<>();

        for (DefaultWidgetType widgetType : DefaultWidgetType.values()) {
            WidgetDisplayElement widget = new WidgetDisplayElement(
                    ViewerResourceBundle.getTranslations(widgetType.getLabel(), true),
                    new SimpleMetadataValue(),
                    Collections.emptyList(),
                    WidgetGenerationType.DEFAULT,
                    widgetType);
            widgets.add(widget);
        }

        for (AutomaticWidgetType widgetType : AutomaticWidgetType.values()) {
            switch (widgetType) {
                case WIDGET_CMSGEOMAP:
                    for (GeoMap geoMap : DataManager.getInstance().getDao().getAllGeoMaps()) {
                        WidgetDisplayElement widget = new WidgetDisplayElement(
                                geoMap.getTitles(),
                                geoMap.getDescriptions(),
                                getEmbeddingPages(geoMap),
                                WidgetGenerationType.AUTOMATIC,
                                widgetType, geoMap.getId(), null);
                        widgets.add(widget);
                    }
                    break;
                default:
                    break;
            }
        }

        List<CustomSidebarWidget> customWidgets = DataManager.getInstance().getDao().getAllCustomWidgets();
        for (CustomSidebarWidget widget : customWidgets) {
            WidgetDisplayElement element = new WidgetDisplayElement(
                    widget.getTitle(),
                    widget.getShortDescription(MAX_DESCRIPTION_LENGTH),
                    queryAdditionalInformation ? getEmbeddingPages(widget) : Collections.emptyList(),
                    WidgetGenerationType.CUSTOM,
                    widget.getType(), widget.getId(), CustomWidgetType.WIDGET_FIELDFACETS.equals(widget.getType()) ? null : widget);
            widgets.add(element);
        }

        return widgets;

    }

    private static List<CMSPage> getEmbeddingPages(GeoMap geoMap) {
        try {
            return DataManager.getInstance().getDao().getPagesUsingMapInSidebar(geoMap);
        } catch (DAOException e) {
            logger.error("Error querying embedding pages ", e);
            return Collections.emptyList();
        }
    }

    public static List<CMSPage> getEmbeddingPages(CustomSidebarWidget widget) {
        try {
            return DataManager.getInstance().getDao().getPagesUsingWidget(widget);
        } catch (DAOException e) {
            logger.error("Error querying embedding pages ", e);
            return Collections.emptyList();
        }
    }

    /**
     * deleteWidget.
     *
     * @param id database ID of the custom widget to delete
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteWidget(Long id) throws DAOException {
        DataManager.getInstance().getDao().deleteCustomWidget(id);
    }

    /**
     * Getter for the field <code>sidebarGroup</code>.
     *
     * @param elements ordered list of sidebar elements to render
     * @param page CMS page owning the sidebar elements
     * @return a {@link jakarta.faces.component.html.HtmlPanelGroup} object
     */
    public HtmlPanelGroup getSidebarGroup(List<CMSSidebarElement> elements, CMSPage page) {
        if (elements != null && !elements.isEmpty()) {
            sidebarGroup = new HtmlPanelGroup();
            elements.sort((e1, e2) -> Integer.compare(e1.getOrder(), e2.getOrder()));
            for (CMSSidebarElement element : elements) {
                loadWidgetComponent(element, page, sidebarGroup);
            }
        }
        return sidebarGroup;
    }

    /**
     * Getter for the field <code>sidebarGroup</code>.
     *
     * @return a {@link jakarta.faces.component.html.HtmlPanelGroup} object
     */
    public HtmlPanelGroup getSidebarGroup() {
        return Optional.ofNullable(cmsBean).map(CmsBean::getCurrentPage).map(page -> {
            List<CMSSidebarElement> elements = Optional.of(page).map(CMSPage::getSidebarElements).orElse(Collections.emptyList());
            return getSidebarGroup(new ArrayList<>(elements), page);
        }).orElseGet(() -> getSidebarGroup(Collections.emptyList(), null));
    }

    /**
     * Setter for the field <code>sidebarGroup</code>.
     *
     * @param sidebarGroup panel group to set as the sidebar container
     */
    public void setSidebarGroup(HtmlPanelGroup sidebarGroup) {
        this.sidebarGroup = sidebarGroup;
    }

    private static void loadWidgetComponent(CMSSidebarElement component, CMSPage page, HtmlPanelGroup parent) {
        DynamicContentBuilder builder = new DynamicContentBuilder();
        DynamicContent content = new DynamicContent(DynamicContentType.WIDGET, component.getContentType().getFilename());
        content.setId(SIDEBAR_ELEMENT_ID_PREFIX + component.getId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SIDEBAR_COMPONENT_ATTRIBUTE_CMS_PAGE, page);
        attributes.put(SIDEBAR_COMPONENT_ATTRIBUTE_SIDEBAR_ELEMENT, component);
        if (component instanceof CMSSidebarElementCustom c) {
            attributes.put(SIDEBAR_COMPONENT_ATTRIBUTE_WIDGET, c.getWidget());
        } else if (component instanceof CMSSidebarElementAutomatic c) {
            attributes.put(SIDEBAR_COMPONENT_ATTRIBUTE_GEOMAP, c.getMap());
        }
        content.setAttributes(attributes);
        UIComponent widgetComponent = builder.build(content, parent);
        if (widgetComponent == null) {
            logger.error("Error loading widget: {}", component);
        }
    }

}
