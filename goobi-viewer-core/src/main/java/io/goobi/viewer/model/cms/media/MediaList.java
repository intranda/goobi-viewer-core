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
package io.goobi.viewer.model.cms.media;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Contains a list of {@link MediaItem MediaItems} built from {@link CMSMediaItem CMSMediaItems}
 */
public class MediaList {

    private final List<MediaItem> mediaItems;

    public MediaList(List<CMSMediaItem> items, HttpServletRequest servletRequest) {
        this.mediaItems = items.stream().map(item -> new MediaItem(item, servletRequest)).collect(Collectors.toList());
    }

    /**
     * @return the mediaItems
     */
    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }
}