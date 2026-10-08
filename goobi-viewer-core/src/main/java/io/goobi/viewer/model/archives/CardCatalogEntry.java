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

/**
 * A single card of a card catalog drawer: one EAD file-level node with its ordering title and the
 * associated record that carries the card scan. Instances are immutable; the position of a card is
 * given by its position in the drawer's Solr ordering.
 */
public class CardCatalogEntry {

    private final String id;
    private final String label;
    private final String recordPi;
    private final boolean hasImage;

    /**
     * @param id EAD node ID; may be null if the source EAD node has none
     * @param label ordering title of the card; may be null
     * @param recordPi PI of the associated viewer record; null if no record is associated
     * @param hasImage true if the associated record has an image
     */
    public CardCatalogEntry(String id, String label, String recordPi, boolean hasImage) {
        this.id = id;
        this.label = label;
        this.recordPi = recordPi;
        this.hasImage = hasImage;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getRecordPi() {
        return recordPi;
    }

    public boolean isHasImage() {
        return hasImage;
    }
}
