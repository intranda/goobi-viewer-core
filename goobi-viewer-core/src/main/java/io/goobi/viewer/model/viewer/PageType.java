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
package io.goobi.viewer.model.viewer;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.urlresolution.ViewerPathBuilder;

/**
 * <p>
 * PageType class.
 * </p>
 */
public enum PageType {

    viewImage("image"),
    viewToc("toc"),
    viewThumbs("thumbs"),
    viewMetadata("metadata"),
    viewFulltext("fulltext"),
    viewFullscreen("fullscreen"),
    viewObject("object"),
    viewCalendar("calendar"),
    searchlist("searchlist", "search"),
    searchCalendar("searchcalendar", "searchCalendar"),
    searchGeoMap("searchgeomap", "title__search_geomap"),
    term("term", "searchTermList"),
    expandCollection("expandCollection"),
    firstWorkInCollection("rest/redirect/toFirstWork"),
    sitelinks("sitelinks"),
    archives("archives"),
    archive("archive"),
    timematrix("timematrix"),
    //user
    user("user"),
    userSearches("user/searches", "label__user_searches"),
    userContentUpload("user/upload", "user__upload_content"),
    //admin
    admin("admin"),
    adminDashboard("admin/", "admin__dashboard"),
    adminUsers("admin/users", "admin__users", adminDashboard),
    adminUser("admin/users", "admin__user", adminUsers),
    adminUserNew("admin/users/new", "admin__user_create_new", adminUsers),
    adminUserEdit("admin/users/edit", "admin__user_edit", adminUsers),
    adminUserGroups("admin/groups", "admin__groups", adminDashboard),
    adminUserGroupEdit("admin/groups/edit", "admin__group_edit", adminUserGroups),
    adminUserGroupNew("admin/groups/new", "admin__group_create_new", adminUserGroups),
    adminIpRanges("admin/ipranges", "admin__ip_ranges", adminDashboard),
    adminIpRange("admin/ipranges", "admin__ip_range_edit", adminIpRanges),
    adminIpRangeNew("admin/ipranges/new", "admin__ip_range_new", adminIpRanges),
    adminLicenseTypes("admin/licenses", "admin__licenses", adminDashboard),
    adminLicenseType("admin/license", "admin__license_edit", adminLicenseTypes),
    adminRights("admin/rights", "admin__rights", adminDashboard),
    adminRight("admin/rights", "admin__right", adminRights),
    adminRightsNew("admin/rights/new", "admin__right", adminRights),
    adminUserComments("admin/comments", "userComments", adminDashboard),
    adminUserCommentGroups("admin/comments", "admin__comment_groups_title", adminDashboard),
    adminUserCommentGroupAll("admin/comments/all", "admin__comment_groups_all_comments_title", adminUserCommentGroups),
    adminUserCommentGroupNew("admin/comments/new", "admin__comment_group_create", adminUserCommentGroups),
    adminUserCommentGroupEdit("admin/comments/edit", "admin__comment_group_edit", adminUserCommentGroups),
    adminUserTerms("admin/userterms", "admin__terms_of_use__title", adminDashboard),
    adminCreateRecord("admin/record/new", "admin__create_record__title", adminDashboard),
    adminThemes("admin/themes", "admin__themes__title", adminDashboard),
    adminClients("admin/clients", "admin__clients", adminDashboard),
    adminClientsEdit("admin/clients/edit", "admin__clients__edit__title", adminClients),
    adminConfigEditor("admin/config", "admin__config_editor__title", adminDashboard),
    adminMessageQueue("admin/tasks", "admin__tasks__title", adminDashboard),
    adminDeveloper("admin/developer", "admin__developer__title", adminDashboard),

    // admin/translations
    adminTranslations("admin/translations", "admin__translations", adminDashboard),
    adminTranslationsEdit("admin/translations/new", "admin__translations__add_new_entry", adminTranslations),
    //admin/cms
    adminCms("admin/cms", "admin__cms", adminDashboard),
    adminCmsOverview("admin/cms/pages", "cms_menu_pages", adminDashboard),
    adminCmsSelectTemplate("admin/cms/pages/templates", "admin__cms__select_template", adminCmsOverview),
    adminCmsNewPage("admin/cms/pages/new", "cms_createPage", adminCmsSelectTemplate),
    adminCmsEditPage("admin/cms/pages/edit", "cms_editPage", adminCmsOverview),
    adminCmsTemplatesNew("admin/cms/pages/templates/new/", "cms__create_template_title", adminCmsSelectTemplate),
    adminCmsTemplatesEdit("/admin/cms/pages/templates/edit/", "cms__edit_template_title", adminCmsSelectTemplate),
    adminCmsSidebarWidgets("admin/cms/widgets", "admin__widgets", adminDashboard),
    adminCmsWidgetsAdd("admin/cms/widgets/new", "cms__add_widget__title", adminCmsSidebarWidgets),
    adminCmsWidgetsEdit("admin/cms/widgets/edit", "cms__edit_widget__title", adminCmsSidebarWidgets),
    adminCmsCategories("admin/cms/categories", "admin__categories", adminDashboard),
    adminCmsNewCategory("admin/cms/categories/new", "admin__category_new", adminCmsCategories),
    adminCmsEditCategory("admin/cms/categories/edit", "admin__category_edit", adminCmsCategories),
    adminCmsStaticPages("admin/cms/pages/mapping", "cms_staticPages", adminDashboard),
    adminCmsMedia("admin/cms/media", "cms_overviewMedia", adminDashboard),
    adminCmsMenuItems("admin/cms/menus", "cms_menu_heading", adminDashboard),
    adminCmsCollections("admin/cms/collections", "admin__cms_collections", adminDashboard),
    adminCmsEditCollection("admin/cms/collections/edit", "cms_collection_edit", adminCmsCollections),
    adminCmsGeoMaps("admin/cms/maps", "cms__geomaps__title", adminDashboard),
    adminCmsGeoMapEdit("admin/cms/maps/edit", "cms__geomap_edit__title", adminCmsGeoMaps),
    adminCmsGeoMapNew("admin/cms/maps/new", "cms__geomap_new__title", adminCmsGeoMaps),
    adminCmsRecordNotes("admin/cms/recordnotes", "cms__record_notes__title_plural", adminDashboard),
    adminCmsRecordNotesNew("admin/cms/recordnotes/new", "cms__record_notes__add_note", adminCmsRecordNotes),
    adminCmsRecordNotesEdit("admin/cms/recordnotes/edit", "cms__record_notes_edit__title", adminCmsRecordNotes),
    adminCmsHighlights("admin/cms/highlights", "admin__highlights__title", adminDashboard),
    adminCmsHighlightsNew("admin/cms/highlights/new", "admin__highlights__new_title", adminCmsHighlights),
    adminCmsHighlightsEdit("admin/cms/highlights/edit", "admin__highlights__edit_title", adminCmsHighlights),
    adminCmsSliders("admin/cms/slider", "cms__sliders__title", adminDashboard),
    adminCmsSlidersNew("admin/cms/slider/new", "cms__add_slider__title", adminCmsSliders),
    adminCmsSlidersEdit("admin/cms/slider/edit", "cms__edit_slider__title", adminCmsSliders),
    adminCookieBanner("admin/legal/cookies", "label__cookie_banner", adminDashboard),
    adminDisclaimer("admin/legal/disclaimer", "label__disclaimer", adminDashboard),
    cmsPageOfWork("page"),
    cmsPage("cms"),
    //admin/crowdsourcing
    adminCrowdsourcingAnnotations("admin/crowdsourcing/annotations", "admin__crowdsourcing_annotations", adminDashboard),
    adminCrowdsourcingCampaigns("admin/crowdsourcing/campaigns", "admin__crowdsourcing_campaigns", adminDashboard),
    adminCrowdsourcingCampaignsNew("admin/crowdsourcing/campaigns/new", "admin__crowdsourcing_campaign_new", adminCrowdsourcingCampaigns),
    adminCrowdsourcingCampaignsEdit("admin/crowdsourcing/campaigns/edit", "admin__crowdsourcing_campaign_edit", adminCrowdsourcingCampaigns),
    adminUserActivity("admin/user/activity/"),
    annotations("annotations"),
    editContent("crowd/editContent"),
    editOcr("crowd/editOcr"),
    editHistory("crowd/editHistory"),

    // The order of page types handled by CMS here determines the listing order of static pages
    index("index", "home", PageTypeHandling.cms),
    search("search", PageTypeHandling.cms),
    advancedSearch("searchadvanced"),
    browse("browse", PageTypeHandling.cms),
    privacy("privacy", PageTypeHandling.cms),
    imprint("imprint", PageTypeHandling.cms),
    feedback("feedback", PageTypeHandling.cms),
    crowsourcingCampaigns("campaigns", PageTypeHandling.cms),
    bookmarks("bookmarks", PageTypeHandling.cms),
    crowsourcingAnnotation("campaigns/.../annotate"),
    crowsourcingReview("campaigns/.../review"),

    other(""); //unknown page type name in Navigationhelper. Probably a cms-page

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(PageType.class);

    public final String path;
    private final String label;
    private final PageTypeHandling handling;
    private final PageType parent;

    private PageType(String name) {
        this.path = name;
        this.label = name;
        this.handling = PageTypeHandling.none;
        this.parent = null;
    }

    private PageType(String name, PageTypeHandling handling) {
        this.path = name;
        this.label = name;
        this.handling = handling;
        this.parent = null;
    }

    private PageType(String path, String label) {
        this.path = path;
        this.label = label;
        this.handling = PageTypeHandling.none;
        this.parent = null;
    }

    private PageType(String path, String label, PageType parent) {
        this.path = path;
        this.label = label;
        this.handling = PageTypeHandling.none;
        this.parent = parent;
    }

    private PageType(String path, String label, PageTypeHandling handling) {
        this.path = path;
        this.label = label;
        this.handling = handling;
        this.parent = null;
    }

    /**
     * <p>
     * Getter for the field <code>handling</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.viewer.PageType.PageTypeHandling} object.
     */
    public PageTypeHandling getHandling() {
        return this.handling;
    }

    public PageType getParent() {
        return parent;
    }

    /**
     * <p>
     * isHandledWithCms.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHandledWithCms() {
        return PageTypeHandling.cms.equals(this.handling);
    }

    /**
     * <p>
     * isCmsPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isCmsPage() {
        try {
            switch (this) {
                case editContent:
                case editHistory:
                case editOcr:
                case viewCalendar:
                case viewFullscreen:
                case viewFulltext:
                case viewImage:
                case viewMetadata:
                case viewThumbs:
                case viewToc:
                    return true;
                default:
                    return false;
            }
        } catch (NoClassDefFoundError e) {
            //Gets thrown under some conditions for some reason. For now just ignore
            return false;
        }
    }

    /**
     * <p>
     * isDocumentPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDocumentPage() {
        try {
            switch (this) {
                case editContent:
                case editHistory:
                case editOcr:
                case viewCalendar:
                case viewFullscreen:
                case viewFulltext:
                case viewImage:
                case viewMetadata:
                case viewThumbs:
                case viewToc:
                case viewObject:
                    return true;
                default:
                    return false;
            }
        } catch (NoClassDefFoundError e) {
            //Gets thrown under some conditions for some reason. For now just ignore
            return false;
        }
    }

    /**
     * <p>
     * getTypesHandledByCms.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public static List<PageType> getTypesHandledByCms() {
        Set<PageType> all = EnumSet.allOf(PageType.class);
        List<PageType> cmsPages = new ArrayList<>();
        for (PageType pageType : all) {
            if (pageType.isHandledWithCms()) {
                cmsPages.add(pageType);
            }
        }
        return cmsPages;
    }

    /**
     * <p>
     * getByName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     * @should return correct type for raw names
     * @should return correct type for mapped names
     * @should return correct type for enum names
     * @should return correct type if name starts with metadata
     */
    public static PageType getByName(String name) {
        if (name == null) {
            return null;
        }
        for (PageType p : PageType.values()) {
            if (p.getName().equalsIgnoreCase(name) || p.path.equalsIgnoreCase(name) || p.label.equalsIgnoreCase(name)
                    || p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        // Set type viewMetadata is page name starts with "metadata"
        if (name.startsWith(PageType.viewMetadata.getName())) {
            return PageType.viewMetadata;
        }
        // look for configured names
        for (PageType p : PageType.values()) {
            String configName = DataManager.getInstance().getConfiguration().getPageType(p);
            if (configName != null && configName.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return PageType.other;
    }

    /**
     * <p>
     * getRawName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRawName() {
        return path;
    }

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     *
     * @return Mapped alternative name, if available; raw name otherwise
     */
    public String getName() {
        String configName = DataManager.getInstance().getConfiguration().getPageType(this);
        if (configName != null) {
            return configName;
        }

        return path;
    }

    public String getLabel() {
        return label;
    }

    public static enum PageTypeHandling {
        none,
        cms;
    }

    /**
     * <p>
     * getPageTypeForDocStructType.
     * </p>
     *
     * @param docStructType a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public static PageType getPageTypeForDocStructType(String docStructType) {
        // First choice: Use preferred target page type for this publication type, if configured
        String preferredPageTypeName = DataManager.getInstance().getConfiguration().getRecordTargetPageType(docStructType);
        PageType preferredPageType = PageType.getByName(preferredPageTypeName);
        if (StringUtils.isNotEmpty(preferredPageTypeName) && preferredPageType == null) {
            logger.error("docstructTargetPageType configured for '{}' does not exist: {}", docStructType, preferredPageTypeName);
        }
        // Second choice: Use target page type configured as _DEFAULT, if available
        String defaultPageTypeName = DataManager.getInstance().getConfiguration().getRecordTargetPageType(StringConstants.DEFAULT_NAME);
        PageType defaultPageType = PageType.getByName(defaultPageTypeName);
        if (StringUtils.isNotEmpty(defaultPageTypeName) && defaultPageType == null) {
            logger.error("docstructTargetPageType configured for '_DEFAULT' does not exist: {}", docStructType);
        }

        if (preferredPageType != null) {
            // logger.trace("Found preferred page type: {}", preferredPageType.getName());
            return preferredPageType;
        } else if (defaultPageType != null) {
            // logger.trace("Found default page type: {}", defaultPageType.getName());
            return defaultPageType;
        }

        return null;
    }

    /**
     * <p>
     * determinePageType.
     * </p>
     *
     * @param docStructType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @param anchorOrGroup a boolean.
     * @param hasImages a boolean.
     * @param pageResolverUrl If this page type is for a page resolver url, ignore certain preferences
     * @should return configured page type correctly
     * @should return metadata page type for application mime type
     * @should return toc page type for anchors
     * @should return image page type correctly
     * @should return medatata page type if nothing else matches
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public static PageType determinePageType(String docStructType, String mimeType, Boolean anchorOrGroup, Boolean hasImages,
            boolean pageResolverUrl) {
        // Determine preferred target for the docstruct
        //         logger.trace("determinePageType: docstrct: {} / mime type: {} / anchor: {} / images: {} / resolver: {}", docStructType, mimeType,
        //                anchorOrGroup, hasImages, pageResolverUrl);
        PageType configuredPageType = PageType.getPageTypeForDocStructType(docStructType);
        if (configuredPageType != null && !pageResolverUrl) {
            return configuredPageType;
        }

        if (BaseMimeType.APPLICATION.equals(BaseMimeType.getByName(mimeType))) {
            return PageType.viewMetadata;
        }
        if (Boolean.TRUE.equals(anchorOrGroup)) {
            return PageType.viewToc;
        }
        if (Boolean.TRUE.equals(hasImages)) {
            return PageType.viewObject;
        }

        return PageType.viewMetadata;
    }

    /**
     * <p>
     * matches.
     * </p>
     *
     * @param pagePath a {@link java.lang.String} object.
     * @return true if the given path equals either the intrinsic or configured name of this pageType Leading and trailing slashes are ignored.
     *         PageType other is never matched
     */
    public boolean matches(String pagePath) {
        if (StringUtils.isBlank(pagePath)) {
            return false;
        }
        pagePath = pagePath.replaceAll("(^\\/)|(\\/$)", "");
        return pagePath.equalsIgnoreCase(this.name()) || pagePath.equalsIgnoreCase(this.path) || pagePath.equalsIgnoreCase(getName());
    }

    /**
     * <p>
     * matches.
     * </p>
     *
     * @param pagePath a {@link java.net.URI} object.
     * @return true if the given path starts with either the intrinsic or configured name of this pageType Leading and trailing slashes are ignored.
     *         PageType other is never matched
     */
    public boolean matches(URI pagePath) {
        if (pagePath == null || StringUtils.isBlank(pagePath.toString())) {
            return false;
        }
        return ViewerPathBuilder.startsWith(pagePath, this.name()) || ViewerPathBuilder.startsWith(pagePath, this.path)
                || ViewerPathBuilder.startsWith(pagePath, getName());
    }

    public boolean isAdminBackendPage() {
        return this.name().toLowerCase().startsWith("admin");
    }
}
