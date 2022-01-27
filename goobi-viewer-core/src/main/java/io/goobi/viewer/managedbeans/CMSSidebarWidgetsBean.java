package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementAutomatic;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementCustom;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.cms.widgets.type.DefaultWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.jsf.DynamicContent;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.DynamicContentType;
import io.goobi.viewer.model.maps.GeoMap;

@Named("cmsSidebarWidgetsBean")
@RequestScoped
public class CMSSidebarWidgetsBean implements Serializable {

    private HtmlPanelGroup sidebarGroup = null;
    
    private static final long serialVersionUID = -6039330925483238481L;
    
    private static final Logger logger = LoggerFactory.getLogger(CMSSidebarWidgetsBean.class);

    @Inject
    CmsBean cmsBean;
    
    public List<WidgetDisplayElement> getAllWidgets() throws DAOException {
        return getAllWidgets(false);
    }
        
    public List<WidgetDisplayElement> getAllWidgets(boolean queryAdditionalInformation) throws DAOException {
        
        List<WidgetDisplayElement> widgets = new ArrayList<WidgetDisplayElement>();
        
        for (DefaultWidgetType widgetType : DefaultWidgetType.values()) {
            WidgetDisplayElement widget = new WidgetDisplayElement(
                    ViewerResourceBundle.getTranslations(widgetType.getLabel(), true), 
                    ViewerResourceBundle.getTranslations(widgetType.getDescription(), true), 
                    queryAdditionalInformation ? getEmbeddingPages(widgetType): Collections.emptyList(), 
                    WidgetGenerationType.DEFAULT,
                    widgetType);
            widgets.add(widget);
        }
        
        for (AutomaticWidgetType widgetType : AutomaticWidgetType.values()) {
            switch(widgetType) {
                case WIDGET_CMSGEOMAP:
                    for (GeoMap geoMap : DataManager.getInstance().getDao().getAllGeoMaps()) {                        
                        WidgetDisplayElement widget = new WidgetDisplayElement(
                                geoMap.getTitles(), 
                                geoMap.getDescriptions(), 
                                getEmbeddingPages(widgetType, geoMap), 
                                WidgetGenerationType.AUTOMATIC,
                                widgetType, geoMap.getId());
                        widgets.add(widget);
                    }
            }
        }
        
        List<CustomSidebarWidget> customWidgets = DataManager.getInstance().getDao().getAllCustomWidgets();
        for (CustomSidebarWidget widget : customWidgets) {
            WidgetDisplayElement element = new WidgetDisplayElement(
                    widget.getTitle(), 
                    ViewerResourceBundle.getTranslations(widget.getType().getDescription(), true), 
                    getEmbeddingPages(widget),
                    WidgetGenerationType.CUSTOM,
                    widget.getType(), widget.getId());
            widgets.add(element);
        }
                
        return widgets;
        
    }
    
    private List<CMSPage> getEmbeddingPages(CustomSidebarWidget widget) {
        // TODO Auto-generated method stub
        return null;
    }

    private List<CMSPage> getEmbeddingPages(AutomaticWidgetType widgetType, GeoMap geoMap) {
        List<CMSPage> pages = new ArrayList<>();
        return pages;
    }

    private List<CMSPage> getEmbeddingPages(DefaultWidgetType widgetType) {
        List<CMSPage> pages = new ArrayList<>();
        return pages;
    }
    
    public void deleteWidget(Long id) throws DAOException {
        DataManager.getInstance().getDao().deleteCustomWidget(id);
    }

    public HtmlPanelGroup getSidebarGroup(List<CMSSidebarElement> elements) {
        if(elements != null && !elements.isEmpty()) {
            sidebarGroup = new HtmlPanelGroup();
            elements.sort((e1,e2) -> Integer.compare(e1.getOrder(), e2.getOrder()));
            for (CMSSidebarElement element : elements) {                
                loadWidgetComponent(element, sidebarGroup);
            }
        }
        return sidebarGroup;
    }
    
    public HtmlPanelGroup getSidebarGroup() {
        List<CMSSidebarElement> elements = Optional.ofNullable(cmsBean).map(CmsBean::getCurrentPage).map(CMSPage::getSidebarElements).orElse(Collections.emptyList());
        return getSidebarGroup(elements);
    }
    
    public void setSidebarGroup(HtmlPanelGroup sidebarGroup) {
        this.sidebarGroup = sidebarGroup;
    }
    
    private void loadWidgetComponent(CMSSidebarElement component, HtmlPanelGroup parent) {
        DynamicContentBuilder builder = new DynamicContentBuilder();
        DynamicContent content = new DynamicContent(DynamicContentType.WIDGET, component.getContentType().getFilename());
        content.setId("sidebar_widget_" + component.getId());
        if(component instanceof CMSSidebarElementCustom) {            
            content.setAttributes(Map.of("widget", ((CMSSidebarElementCustom) component).getWidget()));
        } else if(component instanceof CMSSidebarElementAutomatic) {
            content.setAttributes(Map.of("geoMap", ((CMSSidebarElementAutomatic) component).getMap()));
        }
        UIComponent widgetComponent = builder.build(content, parent);
        if(widgetComponent == null) {
            logger.error("Error loading widget " + component);
        }
    }
    
}

