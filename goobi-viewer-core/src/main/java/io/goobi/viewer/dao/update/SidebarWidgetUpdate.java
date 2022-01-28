package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.FacetFieldSidebarWidget;
import io.goobi.viewer.model.cms.widgets.HtmlSidebarWidget;
import io.goobi.viewer.model.cms.widgets.PageListSidebarWidget;
import io.goobi.viewer.model.cms.widgets.RssFeedSidebarWidget;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementAutomatic;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementCustom;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementDefault;
import io.goobi.viewer.model.cms.widgets.type.AutomaticWidgetType;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.cms.widgets.type.DefaultWidgetType;
import io.goobi.viewer.model.cms.widgets.type.WidgetContentType;
import io.goobi.viewer.model.cms.widgets.type.WidgetGenerationType;
import io.goobi.viewer.model.maps.GeoMap;

public class SidebarWidgetUpdate implements IModelUpdate {

    private static final Logger logger = LoggerFactory.getLogger(SidebarWidgetUpdate.class);

    @Override
    public boolean update(IDAO dao) throws DAOException, SQLException {
        List<String> tables = dao.createNativeQuery("show tables").getResultList();
        if(tables.contains("cms_sidebar_elements")) {               
            migrateWidgetTables(dao);
            return true;
        } else {
            return false;
        }

    }

    private void migrateWidgetTables(IDAO dao) throws DAOException {
        List<Object[]> info = dao.createNativeQuery("desc cms_sidebar_elements").getResultList();

        List<Object[]> legacyWidgets = dao.createNativeQuery("SELECT * FROM cms_sidebar_elements").getResultList();

        List<String> columnNames = info.stream().map(o -> (String) o[0]).collect(Collectors.toList());

        for (Object[] legacyWidget : legacyWidgets) {
            Map<String, Object> columns = IntStream.range(0, columnNames.size())
                    .boxed()
                    .filter(i -> legacyWidget[i] != null)
                    .collect(Collectors.toMap(i -> columnNames.get(i), i -> legacyWidget[i]));

            Long cms_sidebar_element_id = Optional.ofNullable(columns.get("cms_sidebar_element_id")).map(o -> (Long) o).orElse(null);
            String widget_type = Optional.ofNullable(columns.get("widget_type")).map(o -> (String) o).orElse(null);
            String css_class = Optional.ofNullable(columns.get("css_class")).map(o -> (String) o).orElse(null);
            Long geomap__id = Optional.ofNullable(columns.get("geomap__id")).map(o -> (Long) o).orElse(null);
            String inner_html = Optional.ofNullable(columns.get("inner_html")).map(o -> (String) o).orElse(null);
            String linked_pages = Optional.ofNullable(columns.get("linked_pages")).map(o -> (String) o).orElse(null);
            Integer sort_order = Optional.ofNullable(columns.get("sort_order")).map(o -> (Integer) o).orElse(null);
            String type = Optional.ofNullable(columns.get("type")).map(o -> (String) o).orElse(null);
            String value = Optional.ofNullable(columns.get("value")).map(o -> (String) o).orElse(null);
            String widget_mode = Optional.ofNullable(columns.get("widget_mode")).map(o -> (String) o).orElse(null);
            String widget_title = Optional.ofNullable(columns.get("widget_title")).map(o -> (String) o).orElse(null);
            Long owner_page_id = Optional.ofNullable(columns.get("owner_page_id")).map(o -> (Long) o).orElse(null);
            String additional_query = Optional.ofNullable(columns.get("additional_query")).map(o -> (String) o).orElse(null);
            Boolean descending_order = Optional.ofNullable(columns.get("descending_order")).map(o -> (Boolean) o).orElse(null);
            Integer result_display_limit = Optional.ofNullable(columns.get("result_display_limit")).map(o -> (Integer) o).orElse(null);
            String search_field = Optional.ofNullable(columns.get("search_field")).map(o -> (String) o).orElse(null);

            WidgetContentType contentType = parseContentType(type);

            if (contentType != null) {
                CMSPage ownerPage = dao.getCMSPage(owner_page_id);
                if (ownerPage != null) {
                    WidgetGenerationType generationType = WidgetContentType.getGenerationType(contentType);
                    CMSSidebarElement element = null;
                    switch (generationType) {
                        case DEFAULT:
                            element = new CMSSidebarElementDefault(contentType, ownerPage);
                            break;
                        case AUTOMATIC:
                            GeoMap map = dao.getGeoMap(geomap__id);
                            if (map != null) {
                                element = new CMSSidebarElementAutomatic(map, ownerPage);
                            }
                            break;
                        case CUSTOM:
                            if(CustomWidgetType.WIDGET_HTML.equals(contentType)) {
                                widget_title = type;
                            }
                            CustomSidebarWidget widget = createCustomWidget(inner_html, linked_pages, widget_title, additional_query,
                                    result_display_limit, search_field, contentType, widget_mode, css_class);
                            if(widget != null) {
                                dao.addCustomWidget(widget);
                                logger.error("CREATED NEW SIDEBAR WIDGET OF TYPE '{}' FOR USE IN CMS PAGE '{}'", contentType, ownerPage);
                                element = new CMSSidebarElementCustom(widget, ownerPage);
                            }
                    }
                    if (element != null) {
                        element.setOrder(sort_order);
                        ownerPage.addSidebarElement(element);
                        dao.updateCMSPage(ownerPage);
                    }
                }
            }
        }

        dao.startTransaction();
        dao.createNativeQuery("DROP TABLE cms_sidebar_elements").executeUpdate();
        dao.commitTransaction();
    }

    private CustomSidebarWidget createCustomWidget(String inner_html, String linked_pages, String widget_title, String additional_query,
            Integer result_display_limit, String search_field, WidgetContentType contentType, String widget_mode, String css_class) {
        CustomWidgetType customType = (CustomWidgetType) contentType;
        CustomSidebarWidget widget = new CustomSidebarWidget();
        
        switch (customType) {
            case WIDGET_HTML:
                if(StringUtils.isNotBlank(inner_html)) {
                    HtmlSidebarWidget htmlWidget = new HtmlSidebarWidget();
                    widget.getTitle().mapEach(oldValue -> widget_title);
                    htmlWidget.getHtmlText().mapEach(oldValue -> inner_html);
                    widget = htmlWidget;
                } else {
                    return null;
                }
                break;
            case WIDGET_CMSPAGES:
                PageListSidebarWidget pageWidget = new PageListSidebarWidget();
                widget.getTitle().mapEach(oldValue -> widget_title);
                pageWidget.setPageIds(parseIds(linked_pages, "\\s*;\\s*"));
                widget = pageWidget;
                break;
            case WIDGET_RSSFEED:
                RssFeedSidebarWidget rssWidget = new RssFeedSidebarWidget();
                widget.getTitle().mapEach(oldValue -> widget_title);
                rssWidget.setFilterQuery(additional_query);
                widget = rssWidget;
                break;
            case WIDGET_FIELDFACETS:
                FacetFieldSidebarWidget facetWidget = new FacetFieldSidebarWidget();
                facetWidget.setFacetField(search_field);
                facetWidget.setNumEntries(result_display_limit);
                facetWidget.setFilterQuery(additional_query);
                widget = facetWidget;
                break;
        }
        
        widget.setCollapsed("FOLDOUT".equals(widget_mode));
        widget.setStyleClass(css_class);
        
        return widget;
    }

    private WidgetContentType parseContentType(String type) {
        WidgetContentType contentType = null;
        if (StringUtils.isNotBlank(type)) {
            switch (type) {
                case "widgetSearchDrillDown":
                    contentType = DefaultWidgetType.WIDGET_FACETTING;
                    break;
                case "widgetSearchField":
                    contentType = DefaultWidgetType.WIDGET_SEARCH;
                    break;
                case "widgetBrowsing":
                    contentType = DefaultWidgetType.WIDGET_BROWSING;
                    break;
                case "widgetWorkCount":
                    contentType = DefaultWidgetType.WIDGET_WORKCOUNT;
                    break;
                case "widgetGeoMap":
                    contentType = AutomaticWidgetType.WIDGET_CMSGEOMAP;
                    break;
                case "widgetFieldFacets":
                    contentType = CustomWidgetType.WIDGET_FIELDFACETS;
                    break;
                case "widgetRssFeed":
                    contentType = CustomWidgetType.WIDGET_RSSFEED;
                    break;
                case "widgetCmsPageLinks":
                    contentType = CustomWidgetType.WIDGET_CMSPAGES;
                    break;
                default:
                    contentType = CustomWidgetType.WIDGET_HTML;
            }
        }
        return contentType;
    }

    private List<Long> parseIds(String string, String separatorPattern) {
        return Arrays.stream(string.split(separatorPattern)).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
    }

}
