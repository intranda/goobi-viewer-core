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
package de.intranda.digiverso.presentation.model.cms.tilegrid;

import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents a tile within a tile grid of images that link to some URL.
 */
public interface ImageGalleryTile {

    public DisplaySize getSize();

    public Priority getPriority();

    public String getName(String language);

    public URI getIconURI();

    public URI getLinkURI(HttpServletRequest request);

    public String getDescription(String language);

    public boolean isImportant();

    public List<String> getTags();

    public boolean isCollection();

    public String getCollectionName();

    public int getDisplayOrder();

    public static enum Priority {
        IMPORTANT,
        DEFAULT;
    }

    public static enum DisplaySize {
        DEFAULT("display_size_standard", ""),
        THIRD("display_size_third", "grid-item--col-4"),
        HALF("display_size_half", "grid-item--col-6"),
        THREEQUARTER("display_size_threequarter", "grid-item--col-9"),
        FULL("display_size_full", "grid-item--col-12");
        //TODO

        private String className;
        private String label;

        private DisplaySize(String label, String className) {
            this.className = className;
            this.label = label;
        }

        @JsonValue
        public String getClassName() {
            return className;
        };

        public String getLabel() {
            return label;
        }

    }
}
