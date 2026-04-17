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
 * <p>Allows O(1) permission lookups during IIIF manifest generation, replacing the
 * per-page Solr queries triggered when
 * {@link io.goobi.viewer.model.viewer.PhysicalElement} access-permission methods fall back to
 * individual Solr calls in a REST context (where no {@code FacesContext} is available and
 * the session-based permission cache is bypassed).
 */
public final class PagePermissions {

    /**
     * Sentinel instance representing the absence of pre-fetched permissions.
     * When {@link #isEmpty()} returns {@code true}, callers fall back to per-page checks.
     */
    public static final PagePermissions EMPTY = new PagePermissions(
            Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());

    private final Map<Integer, AccessPermission> imagePermissions;
    private final Map<Integer, AccessPermission> fulltextPermissions;
    private final Map<Integer, AccessPermission> pdfPermissions;

    /**
     * @param imagePermissions map of page order → image (VIEW_IMAGES) access permission
     * @param fulltextPermissions map of page order → fulltext (VIEW_FULLTEXT) access permission
     * @param pdfPermissions map of page order → page-PDF (DOWNLOAD_PAGE_PDF) access permission
     */
    public PagePermissions(Map<Integer, AccessPermission> imagePermissions,
            Map<Integer, AccessPermission> fulltextPermissions,
            Map<Integer, AccessPermission> pdfPermissions) {
        this.imagePermissions = imagePermissions;
        this.fulltextPermissions = fulltextPermissions;
        this.pdfPermissions = pdfPermissions;
    }

    /**
     * @return {@code true} if this instance carries no pre-fetched data;
     *         callers must fall back to per-page {@link io.goobi.viewer.model.viewer.PhysicalElement} checks
     * @should sentinel is empty
     */
    public boolean isEmpty() {
        return imagePermissions.isEmpty() && fulltextPermissions.isEmpty() && pdfPermissions.isEmpty();
    }

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
}
