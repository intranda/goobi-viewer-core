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

import java.util.List;

import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;

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
     * @param title
     * @param name
     * @param caption
     * @param id
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
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the alt
     */
    public String getAlt() {
        return title;
    }

    /**
     * @return the caption
     */
    public String getCaption() {
        return caption;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @return the important
     */
    public boolean isImportant() {
        return important;
    }

    /**
     * @return the size
     */
    public CMSMediaItem.DisplaySize getSize() {
        return size;
    }

    /**
     * @return the displayOrder
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

}
