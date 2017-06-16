/**
 * This file is part of the Goobi Viewer - a content presentation and management application for digitized objects.
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

import java.util.ArrayList;
import java.util.List;

import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;

public class TileGrid {

    private final List<Tile> items = new ArrayList<Tile>();

    public TileGrid(List<ImageGalleryTile> items, String language) {
        for (ImageGalleryTile mediaItem : items) {
            this.items.add(new Tile(mediaItem.getName(language) != null ? mediaItem.getName(language) : "", mediaItem.getIconURI() != null ? mediaItem
                    .getIconURI().toString() : "", mediaItem.getDescription(language) != null ? mediaItem.getDescription(language) : "", mediaItem
                            .getLinkURI() != null ? mediaItem.getLinkURI().toString() : "", mediaItem.isImportant(), mediaItem.getSize() != null
                                    ? mediaItem.getSize() : CMSMediaItem.DisplaySize.DEFAULT, mediaItem.getTags(), mediaItem.isCollection()
                                            ? mediaItem.getCollectionName() : null, mediaItem.getDisplayOrder()));
        }
    }

    public TileGrid() {
    }

    public List<Tile> getItems() {
        return this.items;
    }

    public void addItem(Tile item) {
        this.items.add(item);
    }

    public void removeItem(Tile item) {
        this.items.remove(item);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getItems().toString();
    }
}
