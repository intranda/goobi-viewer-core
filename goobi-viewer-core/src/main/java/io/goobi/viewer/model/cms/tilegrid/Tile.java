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

import java.util.List;

import io.goobi.viewer.model.cms.CMSMediaItem;

/**
 * Java representation of JSON object to send to viewer GUI.
 */
public class Tile {

    private final String title, name, caption, url;
    private final boolean important;
    private final CMSMediaItem.DisplaySize size;
    private final List<String> tags;
    private final String collection;
    private final int displayOrder;

    /**
     * <p>Constructor for Tile.</p>
     *
     * @param title a {@link java.lang.String} object.
     * @param caption a {@link java.lang.String} object.
     * @param imageUrl a {@link java.lang.String} object.
     * @param linkUrl a {@link java.lang.String} object.
     * @param important a boolean.
     * @param size a CMSMediaItem.DisplaySize object.
     * @param tags a {@link java.util.List} object.
     * @param collection a {@link java.lang.String} object.
     * @param displayOrder a int.
     */
    public Tile(String title, String imageUrl, String caption, String linkUrl, boolean important, CMSMediaItem.DisplaySize size, List<String> tags,
            String collection, int displayOrder) {
        super();
        this.title = title;
        this.name = imageUrl;
        this.caption = caption;
        this.url = linkUrl;
        this.important = important;
        this.tags = tags;
        this.collection = collection;
        this.size = size;
        this.displayOrder = displayOrder;
    }

    /**
     * <p>Getter for the field <code>title</code>.</p>
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * <p>getAlt.</p>
     *
     * @return the alt
     */
    public String getAlt() {
        return title;
    }

    /**
     * <p>Getter for the field <code>caption</code>.</p>
     *
     * @return the caption
     */
    public String getCaption() {
        return caption;
    }

    /**
     * <p>Getter for the field <code>url</code>.</p>
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * <p>Getter for the field <code>tags</code>.</p>
     *
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * <p>Getter for the field <code>collection</code>.</p>
     *
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * <p>isImportant.</p>
     *
     * @return the important
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * <p>Getter for the field <code>size</code>.</p>
     *
     * @return the size
     */
    public CMSMediaItem.DisplaySize getSize() {
        return size;
    }

    /**
     * <p>Getter for the field <code>displayOrder</code>.</p>
     *
     * @return the displayOrder
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

}
