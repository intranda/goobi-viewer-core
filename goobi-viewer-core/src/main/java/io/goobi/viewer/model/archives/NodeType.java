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
package io.goobi.viewer.model.archives;

import java.util.stream.Stream;

import org.jboss.weld.exceptions.IllegalArgumentException;

/**
 * @author florian
 *
 */
public enum NodeType {
    FILE("fa fa-file-text-o"),
    FOLDER("fa fa-folder-open-o"),
    IMAGE("fa fa-file-image-o"),
    AUDIO("fa fa-file-audio-o"),
    VIDEO("fa fa-file-video-o"),
    OTHER("fa fa-file-o"),
    COLLECTION("fa fa-folder-open-o"),
    CLASS("fa fa-files-o");
    
    private final String iconClass;
    
    private NodeType(String iconClass) {
        this.iconClass = iconClass;
    }
    
    public String getIconClass() {
        return this.iconClass;
    }
    
    public static NodeType getNodeType(String type) {
        return Stream.of(NodeType.values())
        .filter(nodeType -> nodeType.name().equalsIgnoreCase(type))
        .findAny()
        .orElse(OTHER);
    }
}
