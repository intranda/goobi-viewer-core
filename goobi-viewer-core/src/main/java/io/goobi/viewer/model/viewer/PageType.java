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
package io.goobi.viewer.model.viewer;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
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
    //    viewOverview("overview"),
    viewFullscreen("fullscreen"),
    viewObject("object"),
    viewCalendar("calendar"),
    search("search", PageTypeHandling.cms),
    searchlist("searchlist"),
    advancedSearch("searchadvanced", PageTypeHandling.cms),
    calendarsearch("searchcalendar"),
    term("term"),
    browse("browse", PageTypeHandling.cms),
    expandCollection("expandCollection"),
    firstWorkInCollection("rest/redirect/toFirstWork"),
    sitelinks("sitelinks"),
    //admin
    admin("admin"),
    adminAllUsers("admin/users"),
    adminUser("admin/users"),
    adminAllUserGroups("admin/groups"),
    adminUserGroup("admin/groups"),
    adminIpRanges("admin/ipranges"),
    adminIpRange("admin/ipranges"),
    adminAllLicenseTypes("admin/licenses"),
    adminLicenseType("admin/license"),
    adminRoles("admin/roles"),
    adminUserComments("admin/comments"),
    adminCreateRecord("admin/record/new"),
    //admin/cms
    adminCms("admin/cms"),
    adminCmsOverview("admin/cms/pages"),
    adminCmsSelectTemplate("admin/cms/pages/new"),
    adminCmsCreatePage("admin/cms/pages/create"),
    adminCmsCategories("admin/cms/categories"),
    adminCmsStaticPages("admin/cms/pages/mapping"),
    adminCmsMedia("admin/cms/media"),
    adminCmsMenuItems("admin/cms/menus"),
    adminCmsCollections("admin/cms/collections"),
    adminCmsEditCollection("admin/cms/collections/edit"),
    adminCmsGeoMaps("admin/cms/maps"),
    adminCmsGeoMapEdit("admin/cms/maps/edit"),
    cmsPageOfWork("page"),
    cmsPage("cms"),
    //admin/crowdsourcing
    adminCrowdsourcingAnnotations("admin/crowdsourcing/annotations"),
    adminCrowdsourcingCampaigns("admin/crowdsourcing/campaigns"),
    adminUserActivity("admin/user/activity/"),
    //crowdsourcing/annotation
    crowsourcingCampaigns("campaigns", PageTypeHandling.cms),
    crowsourcingAnnotation("campaigns/.../annotate"),
    crowsourcingReview("campaigns/.../review"),
    // TODO remove
    editContent("crowd/editContent"),
    editOcr("crowd/editOcr"),
    editHistory("crowd/editHistory"),
    index("index", PageTypeHandling.cms),
    bookmarks("bookmarks", PageTypeHandling.cms),
    user("user"),
    privacy("privacy", PageTypeHandling.cms),
    feedback("feedback", PageTypeHandling.cms),
    other(""); //unknown page type name in Navigationhelper. Probably a cms-page

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(PageType.class);

    private final String name;
    private final PageTypeHandling handling;

    private PageType(String name) {
        this.name = name;
        this.handling = PageTypeHandling.none;
    }

    private PageType(String name, PageTypeHandling handling) {
        this.name = name;
        this.handling = handling;
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

    /**
     * <p>
     * isHandledWithCms.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHandledWithCms() {
        return this.handling.equals(PageTypeHandling.cms);
    }

    /**
     * <p>
     * isCmsPage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isCmsPage() {
        switch (this) {
            case editContent:
            case editHistory:
            case editOcr:
            case viewCalendar:
            case viewFullscreen:
            case viewFulltext:
            case viewImage:
            case viewMetadata:
                //            case viewOverview:
            case viewThumbs:
            case viewToc:
                return true;
            default:
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
        switch (this) {
            case editContent:
            case editHistory:
            case editOcr:
            case viewCalendar:
            case viewFullscreen:
            case viewFulltext:
            case viewImage:
            case viewMetadata:
                //            case viewOverview:
            case viewThumbs:
            case viewToc:
            case viewObject:
                return true;
            default:
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
     * @should return correct type for raw names
     * @should return correct type for mapped names
     * @should return correct type for enum names
     * @return a {@link io.goobi.viewer.model.viewer.PageType} object.
     */
    public static PageType getByName(String name) {
        if (name == null) {
            return null;
        }
        for (PageType p : PageType.values()) {
            if (p.getName().equalsIgnoreCase(name) || p.name.equalsIgnoreCase(name) || p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        //look for configured names
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
        return name;
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

        return name;
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
        // First choice: Use preferred target page type for this docstruct type, if configured
        String preferredPageTypeName = DataManager.getInstance().getConfiguration().getDocstructTargetPageType(docStructType);
        PageType preferredPageType = PageType.getByName(preferredPageTypeName);
        if (StringUtils.isNotEmpty(preferredPageTypeName) && preferredPageType == null) {
            logger.error("docstructTargetPageType configured for '{}' does not exist: {}", docStructType, preferredPageTypeName);
        }
        // Second choice: Use target page type configured as _DEFAULT, if available
        String defaultPageTypeName = DataManager.getInstance().getConfiguration().getDocstructTargetPageType("_DEFAULT");
        PageType defaultPageType = PageType.getByName(defaultPageTypeName);
        if (StringUtils.isNotEmpty(defaultPageTypeName) && defaultPageType == null) {
            logger.error("docstructTargetPageType configured for '_DEFAULT' does not exist: {}", docStructType, defaultPageTypeName);
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
    public static PageType determinePageType(String docStructType, String mimeType, boolean anchorOrGroup, boolean hasImages,
            boolean pageResolverUrl) {
        // Determine preferred target for the docstruct
        //         logger.trace("determinePageType: docstrct: {} / mime type: {} / anchor: {} / images: {} / resolver: {}", docStructType, mimeType,
        //                anchorOrGroup, hasImages, pageResolverUrl);
        PageType configuredPageType = PageType.getPageTypeForDocStructType(docStructType);
        if (configuredPageType != null && !pageResolverUrl) {
            return configuredPageType;
        }
        if ("application".equals(mimeType)) {
            return PageType.viewMetadata;
        }
        if (anchorOrGroup) {
            return PageType.viewToc;
        }
        if (hasImages) {
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
        pagePath = pagePath.replaceAll("^\\/|\\/$", "");
        return pagePath.equalsIgnoreCase(this.name()) || pagePath.equalsIgnoreCase(this.name) || pagePath.equalsIgnoreCase(getName());
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
        return ViewerPathBuilder.startsWith(pagePath, this.name()) || ViewerPathBuilder.startsWith(pagePath, this.name)
                || ViewerPathBuilder.startsWith(pagePath, getName());
    }

    /**
     * <p>
     * isRestricted.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isRestricted() {
        switch (this) {
            case admin:
            case adminAllLicenseTypes:
            case adminAllUserGroups:
            case adminAllUsers:
            case adminCms:
            case adminCmsCategories:
            case adminCmsCollections:
            case adminCmsCreatePage:
            case adminCmsEditCollection:
            case adminCmsMedia:
            case adminCmsMenuItems:
            case adminCmsOverview:
            case adminCmsSelectTemplate:
            case adminCmsStaticPages:
	    case adminCreateRecord:
            case adminCrowdsourcingAnnotations:
            case adminCrowdsourcingCampaigns:
            case adminIpRange:
            case adminIpRanges:
            case adminLicenseType:
            case adminRoles:
            case adminUser:
            case adminUserActivity:
            case adminUserComments:
            case adminUserGroup:
            case editContent:
            case editHistory:
            case editOcr:
            case bookmarks:
            case user:
                return true;
            default:
                return false;
        }
    }
}
