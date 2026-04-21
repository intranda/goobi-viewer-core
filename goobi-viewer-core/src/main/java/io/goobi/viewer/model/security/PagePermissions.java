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
package io.goobi.viewer.model.security;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable container for pre-fetched per-page access permissions for a single record.
 *
 * <p>Holds decisions for the six privilege types evaluated by
 * {@link AccessConditionUtils#fetchPagePermissions(String, jakarta.servlet.http.HttpServletRequest)}:
 * {@code VIEW_IMAGES}, {@code VIEW_THUMBNAILS}, {@code ZOOM_IMAGES}, {@code DOWNLOAD_IMAGES},
 * {@code VIEW_FULLTEXT}, {@code DOWNLOAD_PAGE_PDF}. This covers every per-page
 * {@code isAccessPermission*} method on {@link io.goobi.viewer.model.viewer.PhysicalElement}
 * so that seeding covers the full viewer render path.
 */
public final class PagePermissions {

    public static final PagePermissions EMPTY = new PagePermissions(
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(),
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());

    private final Map<Integer, AccessPermission> imagePermissions;
    private final Map<Integer, AccessPermission> thumbnailPermissions;
    private final Map<Integer, AccessPermission> zoomPermissions;
    private final Map<Integer, AccessPermission> downloadPermissions;
    private final Map<Integer, AccessPermission> fulltextPermissions;
    private final Map<Integer, AccessPermission> pdfPermissions;

    /**
     * @param imagePermissions map of page order → {@code VIEW_IMAGES} access permission
     * @param thumbnailPermissions map of page order → {@code VIEW_THUMBNAILS} access permission
     * @param zoomPermissions map of page order → {@code ZOOM_IMAGES} access permission
     * @param downloadPermissions map of page order → {@code DOWNLOAD_IMAGES} access permission
     * @param fulltextPermissions map of page order → {@code VIEW_FULLTEXT} access permission
     * @param pdfPermissions map of page order → {@code DOWNLOAD_PAGE_PDF} access permission
     */
    public PagePermissions(Map<Integer, AccessPermission> imagePermissions,
            Map<Integer, AccessPermission> thumbnailPermissions,
            Map<Integer, AccessPermission> zoomPermissions,
            Map<Integer, AccessPermission> downloadPermissions,
            Map<Integer, AccessPermission> fulltextPermissions,
            Map<Integer, AccessPermission> pdfPermissions) {
        this.imagePermissions = imagePermissions;
        this.thumbnailPermissions = thumbnailPermissions;
        this.zoomPermissions = zoomPermissions;
        this.downloadPermissions = downloadPermissions;
        this.fulltextPermissions = fulltextPermissions;
        this.pdfPermissions = pdfPermissions;
    }

    /**
     * @return {@code true} if this instance carries no pre-fetched data;
     *         callers must fall back to per-page {@link io.goobi.viewer.model.viewer.PhysicalElement} checks
     * @should sentinel is empty
     */
    public boolean isEmpty() {
        return imagePermissions.isEmpty() && thumbnailPermissions.isEmpty()
                && zoomPermissions.isEmpty() && downloadPermissions.isEmpty()
                && fulltextPermissions.isEmpty() && pdfPermissions.isEmpty();
    }

    // --- "granted?" convenience accessors used by IIIF builders ---

    /**
     * @param order physical page order number (Solr ORDER field)
     * @return {@code true} if image access is granted for the given order;
     *         {@code false} for unknown orders (fail-safe default)
     * @should return true for granted order
     * @should return false for denied order
     * @should return false for unknown order
     */
    public boolean isImageGranted(int order) {
        return imagePermissions.getOrDefault(order, AccessPermission.denied()).isGranted();
    }

    /**
     * @param order physical page order number (Solr ORDER field)
     * @return {@code true} if thumbnail access is granted for the given order;
     *         {@code false} for unknown orders (fail-safe default)
     * @should return true for granted order
     * @should return false for unknown order
     */
    public boolean isThumbnailGranted(int order) {
        return thumbnailPermissions.getOrDefault(order, AccessPermission.denied()).isGranted();
    }

    /**
     * @param order physical page order number (Solr ORDER field)
     * @return {@code true} if zoom access is granted for the given order;
     *         {@code false} for unknown orders (fail-safe default)
     * @should return true for granted order
     * @should return false for unknown order
     */
    public boolean isZoomGranted(int order) {
        return zoomPermissions.getOrDefault(order, AccessPermission.denied()).isGranted();
    }

    /**
     * @param order physical page order number (Solr ORDER field)
     * @return {@code true} if download access is granted for the given order;
     *         {@code false} for unknown orders (fail-safe default)
     * @should return true for granted order
     * @should return false for unknown order
     */
    public boolean isDownloadGranted(int order) {
        return downloadPermissions.getOrDefault(order, AccessPermission.denied()).isGranted();
    }

    /**
     * @param order physical page order number (Solr ORDER field)
     * @return {@code true} if fulltext access is granted for the given order;
     *         {@code false} for unknown orders (fail-safe default)
     * @should return true for granted order
     */
    public boolean isFulltextGranted(int order) {
        return fulltextPermissions.getOrDefault(order, AccessPermission.denied()).isGranted();
    }

    /**
     * @param order physical page order number (Solr ORDER field)
     * @return {@code true} if page-PDF download is granted for the given order;
     *         {@code false} for unknown orders (fail-safe default)
     * @should return false for denied order
     * @should return false for unknown order
     */
    public boolean isPdfGranted(int order) {
        return pdfPermissions.getOrDefault(order, AccessPermission.denied()).isGranted();
    }

    // --- raw permission accessors used by PhysicalElement seeding ---
    // Unlike the "is*Granted" variants these preserve denied-placeholder info.

    /**
     * @param order physical page order number
     * @return the stored image permission, or null if not prefetched
     * @should return permission for known order
     * @should return null for unknown order
     */
    public AccessPermission getImagePermission(int order) {
        return imagePermissions.get(order);
    }

    /**
     * @param order physical page order number
     * @return the stored thumbnail permission, or null if not prefetched
     * @should return null for unknown order
     */
    public AccessPermission getThumbnailPermission(int order) {
        return thumbnailPermissions.get(order);
    }

    /**
     * @param order physical page order number
     * @return the stored zoom permission, or null if not prefetched
     * @should return null for unknown order
     */
    public AccessPermission getZoomPermission(int order) {
        return zoomPermissions.get(order);
    }

    /**
     * @param order physical page order number
     * @return the stored download permission, or null if not prefetched
     * @should return null for unknown order
     */
    public AccessPermission getDownloadPermission(int order) {
        return downloadPermissions.get(order);
    }

    /**
     * @param order physical page order number
     * @return the stored fulltext permission, or null if not prefetched
     * @should return null for unknown order
     */
    public AccessPermission getFulltextPermission(int order) {
        return fulltextPermissions.get(order);
    }

    /**
     * @param order physical page order number
     * @return the stored PDF permission, or null if not prefetched
     * @should return null for unknown order
     */
    public AccessPermission getPdfPermission(int order) {
        return pdfPermissions.get(order);
    }
}
