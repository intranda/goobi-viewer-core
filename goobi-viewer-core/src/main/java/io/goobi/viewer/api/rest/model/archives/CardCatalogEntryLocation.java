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
package io.goobi.viewer.api.rest.model.archives;

/**
 * Location of a single card within a card catalog: its drawer and its 1-based position therein.
 * Used to resolve deep links that address a card by its EAD node ID.
 */
public class CardCatalogEntryLocation {

    private String entryId;
    private String drawerId;
    private int position;

    public CardCatalogEntryLocation() {
    }

    public CardCatalogEntryLocation(String entryId, String drawerId, int position) {
        this.entryId = entryId;
        this.drawerId = drawerId;
        this.position = position;
    }

    public String getEntryId() {
        return entryId;
    }

    public String getDrawerId() {
        return drawerId;
    }

    public int getPosition() {
        return position;
    }
}
