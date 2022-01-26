package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.AjaxResponseException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.FacetFieldSidebarWidget;
import io.goobi.viewer.model.cms.widgets.HtmlSidebarWidget;
import io.goobi.viewer.model.cms.widgets.PageListSidebarWidget;
import io.goobi.viewer.model.cms.widgets.RssFeedSidebarWidget;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.jsf.DynamicContent;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.DynamicContentType;
import io.goobi.viewer.model.viewer.PageType;

@Named
@ViewScoped
public class CustomWidgetEditBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(CustomWidgetEditBean.class);

    
    private static final long serialVersionUID = 4892069370268036814L;
    private CustomSidebarWidget widget = null;
    private String returnUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.adminCmsSidebarWidgets.getName();
    private HtmlPanelGroup previewGroup = null;
    
    public CustomSidebarWidget getWidget() {
        return widget;
    }
    
    public void setWidget(CustomSidebarWidget widget) {
        this.widget = widget;
    }
    
    public Long getWidgetId() {
        return Optional.ofNullable(widget).map(CustomSidebarWidget::getId).orElse(null);
    }
    
    public void setWidgetId(Long id) throws DAOException {
        if(id != null) {
            setWidget(DataManager.getInstance().getDao().getCustomWidget(id));
        }
    }
    
    public CustomWidgetType getWidgetType() {
        if(widget == null) {
            return null;
        } else {
            return widget.getType();
        }
    }
    
    public List<CustomWidgetType> getWidgetTypes() {
        return Arrays.asList(CustomWidgetType.values());
    }
    
    public String getReturnUrl() {
        return returnUrl;
    }
    
    public void save() throws AjaxResponseException {
        try {
        if(widget != null) {
            if(widget.getId() != null) {                
                DataManager.getInstance().getDao().updateCustomWidget(widget);
            } else {
                DataManager.getInstance().getDao().addCustomWidget(widget);
            }
        }
        } catch(DAOException e) {
            throw new AjaxResponseException(e.toString());
        }
    }
    
    public void createWidget(CustomWidgetType type) {
        switch(type) {
            case WIDGET_CMSPAGES:
                this.widget = new PageListSidebarWidget();
                break;
            case WIDGET_FIELDFACETS:
                this.widget = new FacetFieldSidebarWidget();
                break;
            case WIDGET_HTML:
                this.widget = new HtmlSidebarWidget();
                break;
            case WIDGET_RSSFEED:
                this.widget = new RssFeedSidebarWidget();
                break;
        }
    }
    
    public HtmlPanelGroup getPreviewGroup() {
        if(widget != null) {
            previewGroup = new HtmlPanelGroup();
            loadWidgetComponent(widget, previewGroup);
        }
        return previewGroup;
    }
    
    private void loadWidgetComponent(CustomSidebarWidget component, HtmlPanelGroup parent) {
        DynamicContentBuilder builder = new DynamicContentBuilder();
        DynamicContent content = new DynamicContent(DynamicContentType.WIDGET, component.getType().getFilename());
        content.setId("sidebar_widget_" + component.getId());
        content.setAttributes(Map.of("widget", component));
        UIComponent widgetComponent = builder.build(content, parent);
        if(widgetComponent == null) {
            logger.error("Error loading widget " + component);
        }
    }

    public void setPreviewGroup(HtmlPanelGroup previewGroup) {
        this.previewGroup = previewGroup;
    }
}
