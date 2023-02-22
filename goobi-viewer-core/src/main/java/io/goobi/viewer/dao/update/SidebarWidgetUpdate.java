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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
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
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 *
 * This class migrates migrates data from the deprcated table cms_sidebar_elements to the new table cms_page_sidebar_elements, which backs
 * {@link CMSSidebarElement}. For user configurable widgets it also creates an entry in 'custom_sidebar_widgets', which backs
 * {@link CustomSidebarWidget}. The table cms_sidebar_elements is eventually dropped. The updae is only performed if the table cms_sidebar_elements
 * still exists in the database
 *
 * @author florian
 *
 */
public class SidebarWidgetUpdate implements IModelUpdate {

    private static final Logger logger = LogManager.getLogger(SidebarWidgetUpdate.class);

    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        if (dao.tableExists("cms_sidebar_elements")) {
            migrateWidgetTables(dao);
            return true;
        }
        return false;

    }

    @SuppressWarnings("unchecked")
    private static void migrateWidgetTables(IDAO dao) throws DAOException {
        List<Object[]> info = dao.getNativeQueryResults("SHOW COLUMNS FROM cms_sidebar_elements");

        List<Object[]> legacyWidgets = dao.getNativeQueryResults("SELECT * FROM cms_sidebar_elements");

        List<String> columnNames = info.stream().map(o -> (String) o[0]).collect(Collectors.toList());

        for (Object[] legacyWidget : legacyWidgets) {
            Map<String, Object> columns = IntStream.range(0, columnNames.size())
                    .boxed()
                    .filter(i -> legacyWidget[i] != null)
                    .collect(Collectors.toMap(columnNames::get, i -> legacyWidget[i]));

            Long cmsSidebaElementId = Optional.ofNullable(columns.get("cms_sidebar_element_id")).map(Long.class::cast).orElse(null);
            String widgetType = Optional.ofNullable(columns.get("widget_type")).map(String.class::cast).orElse(null);
            String cssClass = Optional.ofNullable(columns.get("css_class")).map(String.class::cast).orElse(null);
            Long geomapId = Optional.ofNullable(columns.get("geomap__id")).map(Long.class::cast).orElse(null);
            String innerHtml = Optional.ofNullable(columns.get("inner_html")).map(String.class::cast).orElse(null);
            String linkedPages = Optional.ofNullable(columns.get("linked_pages")).map(String.class::cast).orElse(null);
            Integer sortOrder = Optional.ofNullable(columns.get("sort_order")).map(Integer.class::cast).orElse(null);
            String type = Optional.ofNullable(columns.get("type")).map(String.class::cast).orElse(null);
            String value = Optional.ofNullable(columns.get("value")).map(String.class::cast).orElse(null);
            String widgetMode = Optional.ofNullable(columns.get("widget_mode")).map(String.class::cast).orElse(null);
            String widgetTitle = Optional.ofNullable(columns.get("widget_title")).map(String.class::cast).orElse(null);
            Long ownerPageId = Optional.ofNullable(columns.get("owner_page_id")).map(Long.class::cast).orElse(null);
            String additionalQuery = Optional.ofNullable(columns.get("additional_query")).map(String.class::cast).orElse(null);
            Boolean descendingOrder = Optional.ofNullable(columns.get("descending_order")).map(Boolean.class::cast).orElse(null);
            Integer resultDisplayLimit = Optional.ofNullable(columns.get("result_display_limit")).map(Integer.class::cast).orElse(null);
            String searchField = Optional.ofNullable(columns.get("search_field")).map(String.class::cast).orElse(null);

            WidgetContentType contentType = parseContentType(type);

            if (contentType != null) {
                CMSPage ownerPage = dao.getCMSPage(ownerPageId);
                if (ownerPage != null) {
                    WidgetGenerationType generationType = WidgetContentType.getGenerationType(contentType);
                    CMSSidebarElement element = null;
                    switch (generationType) {
                        case DEFAULT:
                            element = new CMSSidebarElementDefault(contentType, ownerPage);
                            break;
                        case AUTOMATIC:
                            GeoMap map = dao.getGeoMap(geomapId);
                            if (map != null) {
                                element = new CMSSidebarElementAutomatic(map, ownerPage);
                            }
                            break;
                        case CUSTOM:
                            if (CustomWidgetType.WIDGET_HTML.equals(contentType)) {
                                widgetTitle = type;
                            }
                            CustomSidebarWidget widget = createCustomWidget(innerHtml, linkedPages, widgetTitle, additionalQuery,
                                    resultDisplayLimit, searchField, contentType, widgetMode, cssClass);
                            if (widget != null) {
                                dao.addCustomWidget(widget);
                                logger.error("CREATED NEW SIDEBAR WIDGET OF TYPE '{}' FOR USE IN CMS PAGE '{}'", contentType, ownerPage);
                                element = new CMSSidebarElementCustom(widget, ownerPage);
                            }
                    }
                    if (element != null) {
                        element.setOrder(sortOrder);
                        ownerPage.addSidebarElement(element);
                        dao.updateCMSPage(ownerPage);
                    }
                }
            }
        }

        dao.executeUpdate("DROP TABLE cms_sidebar_elements");
    }

    /**
     * 
     * @param innerHtml
     * @param linkedPages
     * @param widgetTitle
     * @param additionalQuery
     * @param resultDisplayLimit
     * @param searchField
     * @param contentType
     * @param widgetMode
     * @param cssClass
     * @return
     */
    private static CustomSidebarWidget createCustomWidget(String innerHtml, String linkedPages, String widgetTitle, String additionalQuery,
            Integer resultDisplayLimit, String searchField, WidgetContentType contentType, String widgetMode, String cssClass) {
        CustomWidgetType customType = (CustomWidgetType) contentType;
        CustomSidebarWidget widget = new CustomSidebarWidget();
        switch (customType) {
            case WIDGET_HTML:
                if (StringUtils.isNotBlank(innerHtml)) {
                    HtmlSidebarWidget htmlWidget = new HtmlSidebarWidget();
                    setTitle(widgetTitle, htmlWidget);
                    htmlWidget.getHtmlText().mapEach(oldValue -> innerHtml);
                    widget = htmlWidget;
                } else {
                    return null;
                }
                break;
            case WIDGET_CMSPAGES:
                PageListSidebarWidget pageWidget = new PageListSidebarWidget();
                setTitle(widgetTitle, pageWidget);
                pageWidget.setPageIds(parseIds(linkedPages, "\\s*;\\s*"));
                widget = pageWidget;
                break;
            case WIDGET_RSSFEED:
                RssFeedSidebarWidget rssWidget = new RssFeedSidebarWidget();
                setTitle(StringUtils.isBlank(widgetTitle) ? "lastImports" : widgetTitle, rssWidget);
                if (StringUtils.isNotBlank(widgetTitle)) {
                    rssWidget.getTitle().setValue(widgetTitle, IPolyglott.getDefaultLocale());
                } else {
                    rssWidget.setTitle(new TranslatedText(ViewerResourceBundle.getTranslations("lastImports")));
                }
                rssWidget.setFilterQuery(additionalQuery);
                widget = rssWidget;
                break;
            case WIDGET_FIELDFACETS:
                FacetFieldSidebarWidget facetWidget = new FacetFieldSidebarWidget();
                facetWidget.setFacetField(searchField);
                facetWidget.setNumEntries(resultDisplayLimit);
                facetWidget.setFilterQuery(additionalQuery);
                widget = facetWidget;
                break;
        }

        widget.setCollapsed("FOLDOUT".equals(widgetMode));
        widget.setStyleClass(cssClass);

        return widget;
    }

    /**
     * 
     * @param widgetTitle
     * @param htmlWidget
     */
    private static void setTitle(String widgetTitle, CustomSidebarWidget htmlWidget) {
        TranslatedText translatedTitle = new TranslatedText(ViewerResourceBundle.getTranslations(widgetTitle, false));
        if (translatedTitle.isEmpty()) {
            htmlWidget.getTitle().setValue(widgetTitle, IPolyglott.getDefaultLocale());
        } else {
            htmlWidget.setTitle(translatedTitle);
        }
    }

    /**
     * 
     * @param type
     * @return
     */
    private static WidgetContentType parseContentType(String type) {
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

    /**
     * 
     * @param string
     * @param separatorPattern
     * @return
     */
    private static List<Long> parseIds(String string, String separatorPattern) {
        return Arrays.stream(string.split(separatorPattern)).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
    }

}
