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
package io.goobi.viewer.model.archives;

import java.io.Serializable;
import java.util.Map;

/**
 * @author florian
 *
 */
public class NodeType implements Serializable {

    private static final long serialVersionUID = 3716038748824985126L;

    private static final Map<String, String> LEGACY_ICON_MAP = Map.ofEntries(
            Map.entry("fa fa-folder-open-o", "folder-open"),
            Map.entry("fa fa-folder", "folder"),
            Map.entry("fa fa-files-o", "folder"),
            Map.entry("fa fa-file-text-o", "file"),
            Map.entry("fa fa-file-image-o", "photo"),
            Map.entry("fa fa-file-audio-o", "music"),
            Map.entry("fa fa-file-video-o", "video"),
            Map.entry("fa fa-file-o", "file"));
    private static final String DEFAULT_ICON = "folder-open";

    private final String name;
    private final String icon;

    public NodeType(String name, String icon) {
        this.name = name;
        this.icon = sanitizeIcon(icon);
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return this.icon;
    }

    /**
     * @deprecated use {@link #getIcon()}.
     * @return icon name defined for this node type
     */
    @Deprecated
    public String getIconClass() {
        return this.icon;
    }

    private static String sanitizeIcon(String icon) {
        if (icon == null) {
            return DEFAULT_ICON;
        }

        String trimmed = icon.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_ICON;
        }

        String mapped = LEGACY_ICON_MAP.get(trimmed);
        if (mapped != null) {
            return mapped;
        }

        if (trimmed.contains(" ")) {
            return DEFAULT_ICON;
        }

        return trimmed;
    }
}
