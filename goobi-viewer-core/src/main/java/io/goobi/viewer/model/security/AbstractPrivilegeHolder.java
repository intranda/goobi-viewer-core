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

public abstract class AbstractPrivilegeHolder implements IPrivilegeHolder {

    protected AbstractPrivilegeHolder() {
    }

    /** Constant array containing all constants for record privileges. */
    protected static final String[] PRIVS_RECORD =
            { PRIV_LIST, PRIV_VIEW_THUMBNAILS, PRIV_VIEW_IMAGES, PRIV_VIEW_VIDEO, PRIV_VIEW_AUDIO, PRIV_VIEW_FULLTEXT, PRIV_VIEW_METADATA,
                    PRIV_ZOOM_IMAGES, PRIV_DOWNLOAD_IMAGES, PRIV_DOWNLOAD_ORIGINAL_CONTENT, PRIV_DOWNLOAD_PAGE_PDF, PRIV_DOWNLOAD_PDF,
                    PRIV_DOWNLOAD_METADATA, PRIV_GENERATE_IIIF_MANIFEST, PRIV_VIEW_UGC, PRIV_DOWNLOAD_BORN_DIGITAL_FILES, PRIV_ARCHIVE_DISPLAY_NODE };

    /** Constant array containing all constants for CMS privileges. */
    protected static final String[] PRIVS_CMS =
            { PRIV_CMS_PAGES, PRIV_CMS_MENU, PRIV_CMS_STATIC_PAGES,
                    PRIV_CMS_COLLECTIONS,
                    PRIV_CMS_CATEGORIES };
}
