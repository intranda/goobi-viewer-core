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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSMediaItem;

/**
 * <p>TileGrid class.</p>
 */
public class TileGrid {

	private final List<String> tags = new ArrayList<>();
	private final List<Tile> items = new ArrayList<Tile>();

    /**
     * <p>Constructor for TileGrid.</p>
     *
     * @param items a {@link java.util.List} object.
     * @param tags a {@link java.util.Set} object.
     * @param language a {@link java.lang.String} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     */
    public TileGrid(List<ImageGalleryTile> items, Set<String> tags,String language, HttpServletRequest request) {
        this.tags.addAll(tags);
        for (ImageGalleryTile mediaItem : items) {
            this.items.add(new Tile(mediaItem.getName(language) != null ? mediaItem.getName(language) : "", mediaItem.getIconURI() != null ? mediaItem
                    .getIconURI(0,0).toString() : "", mediaItem.getDescription(language) != null ? mediaItem.getDescription(language) : "", mediaItem
                            .getLinkURI(request) != null ? mediaItem.getLinkURI(request).toString() : "", mediaItem.isImportant(), CMSMediaItem.DisplaySize.DEFAULT, mediaItem.getCategories().stream().map(CMSCategory::getName).collect(Collectors.toList()), null, mediaItem.getDisplayOrder()));
        }
    }

	/**
	 * <p>Constructor for TileGrid.</p>
	 */
	public TileGrid() {
	}

	/**
	 * <p>Getter for the field <code>items</code>.</p>
	 *
	 * @return a {@link java.util.List} object.
	 */
	public List<Tile> getItems() {
		return this.items;
	}

	/**
	 * <p>addItem.</p>
	 *
	 * @param item a {@link io.goobi.viewer.model.cms.tilegrid.Tile} object.
	 */
	public void addItem(Tile item) {
		this.items.add(item);
	}

	/**
	 * <p>removeItem.</p>
	 *
	 * @param item a {@link io.goobi.viewer.model.cms.tilegrid.Tile} object.
	 */
	public void removeItem(Tile item) {
		this.items.remove(item);
	}

	/**
	 * <p>Getter for the field <code>tags</code>.</p>
	 *
	 * @return the tags
	 */
	public List<String> getTags() {
		return tags;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	/** {@inheritDoc} */
	@Override
	public String toString() {
		return getItems().toString();
	}
}
