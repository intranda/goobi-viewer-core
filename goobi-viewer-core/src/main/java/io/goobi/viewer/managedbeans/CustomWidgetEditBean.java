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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
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

    private static final Logger logger = LogManager.getLogger(CustomWidgetEditBean.class);

    private static final long serialVersionUID = 4892069370268036814L;
    private CustomSidebarWidget widget = null;
    private String returnUrl = BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/" + PageType.adminCmsSidebarWidgets.getName();
    private transient HtmlPanelGroup previewGroup = null;
    private Map<CMSPage, Boolean> cmsPageMap;

    @Inject
    public CustomWidgetEditBean(CmsBean cmsBean) {
        try {
            this.cmsPageMap = cmsBean.getAllCMSPages()
                    .stream()
                    .filter(CMSPage::isPublished)
                    .filter(p -> StringUtils.isNotBlank(p.getMenuTitle()))
                    .collect(Collectors.toMap(Function.identity(), p -> Boolean.FALSE));
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
    }

    public CustomSidebarWidget getWidget() {
        return widget;
    }

    public void setWidget(CustomSidebarWidget widget) {
        this.widget = widget;
        if (this.widget != null && CustomWidgetType.WIDGET_CMSPAGES.equals(this.widget.getType())) {
            fillCmsPageMap(((PageListSidebarWidget) this.widget).getPageIds());
        }
    }

    private void fillCmsPageMap(List<Long> pageIds) {
        this.cmsPageMap.keySet().forEach(page -> {
            if (pageIds.contains(page.getId())) {
                this.cmsPageMap.put(page, Boolean.TRUE);
            } else {
                this.cmsPageMap.put(page, Boolean.FALSE);
            }
        });

    }

    public Long getWidgetId() {
        return Optional.ofNullable(widget).map(CustomSidebarWidget::getId).orElse(null);
    }

    public void setWidgetId(Long id) throws DAOException {
        if (id != null) {
            setWidget(DataManager.getInstance().getDao().getCustomWidget(id));
        }
    }

    public CustomWidgetType getWidgetType() {
        if (widget == null) {
            return null;
        }
        return widget.getType();
    }

    public List<CustomWidgetType> getWidgetTypes() {
        return Arrays.asList(CustomWidgetType.values());
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void save() {
        try {
            if (widget != null) {
                if (CustomWidgetType.WIDGET_CMSPAGES.equals(this.widget.getType())) {
                    List<Long> pageIds = this.cmsPageMap.entrySet()
                            .stream()
                            .filter(Entry::getValue)
                            .map(Entry::getKey)
                            .map(CMSPage::getId)
                            .collect(Collectors.toList());
                    ((PageListSidebarWidget) this.widget).setPageIds(pageIds);
                }
                if (widget.getId() != null) {
                    if (!DataManager.getInstance().getDao().updateCustomWidget(widget)) {
                        throw new DAOException("Updating database failed");
                    }
                } else {
                    if (!DataManager.getInstance().getDao().addCustomWidget(widget)) {
                        throw new DAOException("Updating database failed");
                    }
                }
                Messages.info(ViewerResourceBundle.getTranslation("cms__edit_widget__save_widget__success", null));
            }
        } catch (DAOException e) {
            Messages.error(
                    ViewerResourceBundle.getTranslationWithParameters("cms__edit_widget__save_widget__error", BeanUtils.getLocale(), true,
                            e.getMessage()));
        }
    }

    public void createWidget(CustomWidgetType type) {
        switch (type) {
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
            default:
                logger.warn("Type not yet supported: {}", type);
                break;
        }
    }

    public HtmlPanelGroup getPreviewGroup() {
        if (widget != null) {
            previewGroup = new HtmlPanelGroup();
            loadWidgetComponent(widget, previewGroup);
        }
        return previewGroup;
    }

    private static boolean loadWidgetComponent(CustomSidebarWidget component, HtmlPanelGroup parent) {
        DynamicContentBuilder builder = new DynamicContentBuilder();
        DynamicContent content = new DynamicContent(DynamicContentType.WIDGET, component.getType().getFilename());
        content.setId("sidebar_widget_" + component.getId());
        content.setAttributes(Map.of("widget", component));
        UIComponent widgetComponent = builder.build(content, parent);
        if (widgetComponent == null) {
            logger.error("Error loading widget {}", component);
        }
        return widgetComponent != null;
    }

    public void setPreviewGroup(HtmlPanelGroup previewGroup) {
        this.previewGroup = previewGroup;
    }

    public Map<CMSPage, Boolean> getCmsPageMap() {
        return cmsPageMap;
    }

    public void setCmsPageMap(Map<CMSPage, Boolean> cmsPageMap) {
        this.cmsPageMap = cmsPageMap;
    }

    public void refresh() {
        //NOOP
    }
}
