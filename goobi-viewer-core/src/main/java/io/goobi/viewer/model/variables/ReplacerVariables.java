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
package io.goobi.viewer.model.variables;

public final class ReplacerVariables {

    public static final String NAMESPACE_CONFIG = "config";
    public static final String NAMESPACE_ANCHOR = "anchor";
    public static final String NAMESPACE_RECORD = "record";
    public static final String NAMESPACE_STRUCT = "struct";
    public static final String NAMESPACE_PAGE = "page";

    public static final String THEME_PATH = "theme-path";
    public static final String BASE_PATH = "base-path";
    public static final String SOLR_URL = "solr-url";
    public static final String REST_API_URL = "rest-api-url";
    public static final String CONFIG_FOLDER_PATH = "config-folder-path";
    public static final String MIME_TYPE = "mimeType";
    public static final String BASE_MIME_TYPE = "baseMimeType";
    public static final String ORDER = "order";
    public static final String ORDER_LABEL = "orderLabel";
    public static final String FILENAME = "filename";
    public static final String FILENAME_BASE = "baseFilename";
    public static final String VIEWER_URL = "viewer-url";

    /**
     * should not be instantiated
     */
    private ReplacerVariables() {
    };
}
