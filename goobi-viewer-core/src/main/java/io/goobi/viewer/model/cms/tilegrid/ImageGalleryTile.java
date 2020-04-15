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
package io.goobi.viewer.model.cms.tilegrid;

import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonValue;

import io.goobi.viewer.model.cms.CMSCategory;

/**
 * Represents a tile within a tile grid of images that link to some URL.
 */
public interface ImageGalleryTile {

    /**
     * <p>
     * getPriority.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.tilegrid.ImageGalleryTile.Priority} object.
     */
    public Priority getPriority();

    /**
     * <p>
     * getName.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getName(String language);

    /**
     * <p>
     * getIconURI.
     * </p>
     *
     * @return a {@link java.net.URI} object.
     */
    public URI getIconURI();

    /**
     * <p>
     * getIconURI.
     * </p>
     *
     * @param width a int.
     * @param height a int.
     * @return a {@link java.net.URI} object.
     */
    public URI getIconURI(int width, int height);

    /**
     * <p>
     * getLinkURI.
     * </p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link java.net.URI} object.
     */
    public URI getLinkURI(HttpServletRequest request);

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDescription(String language);

    /**
     * <p>
     * isImportant.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isImportant();

    /**
     * <p>
     * getCategories.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<CMSCategory> getCategories();

    /**
     * <p>
     * getDisplayOrder.
     * </p>
     *
     * @return a int.
     */
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
