package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.HtmlSidebarWidget;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.cms.widgets.type.DefaultWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.maps.GeoMap;

@Named("cmsSidebarWidgetsBean")
@SessionScoped
public class CMSSidebarWidgetsBean implements Serializable {

    private static final long serialVersionUID = -6039330925483238481L;
    
    private static final Logger logger = LoggerFactory.getLogger(CMSSidebarWidgetsBean.class);

    public List<WidgetDisplayElement> getAllWidgets() throws DAOException {
        
        List<WidgetDisplayElement> widgets = new ArrayList<WidgetDisplayElement>();
        
        for (DefaultWidgetType widgetType : DefaultWidgetType.values()) {
            WidgetDisplayElement widget = new WidgetDisplayElement(
                    ViewerResourceBundle.getTranslations(widgetType.getLabel(), true), 
                    ViewerResourceBundle.getTranslations(widgetType.getDescription(), true), 
                    getEmbeddingPages(widgetType), 
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
                                widgetType);
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


    
}

