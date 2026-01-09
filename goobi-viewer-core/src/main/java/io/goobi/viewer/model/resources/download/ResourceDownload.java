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

package io.goobi.viewer.model.resources.download;

import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.model.job.TaskType;

/**
 * Manages record resources that are made available to users for download, like pdf files or other media not displayed directly in the viewer
 */
public final class ResourceDownload {

    private static final String ID_WORD_SEPARATOR = "_";
    private static final int MAX_ID_PART_LENGTH = 100;

    private ResourceDownload() {
    }

    /**
     * Create an identifier for a resource accessible through an external url
     * 
     * @param pi persistent identifier of the work the resource belongs to
     * @param url download url of the resource
     * @return an id
     */
    public static String getExternalResourceId(String pi, String url) {
        String urlId = StringTools.convertToSingleWord(url, MAX_ID_PART_LENGTH, ID_WORD_SEPARATOR);

        return "%s_%s_%s".formatted(TaskType.DOWNLOAD_EXTERNAL_RESOURCE.name(), pi,
                urlId);
    }
}
