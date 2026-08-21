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

import java.util.List;

/**
 * A drawer of a card catalog: an EAD structure node whose children are the individual cards.
 * The card list and the computed register ranges are loaded lazily and cached on the drawer;
 * both are populated by {@link CardCatalogManager} under synchronization on this instance.
 */
public class CardCatalogDrawer {

    private final String id;
    private final String iddoc;
    private final String label;
    private final int entryCount;

    private volatile List<CardCatalogEntry> entries = null;
    private volatile List<CardCatalogRegisterRange> registerRanges = null;

    /**
     * @param id EAD node ID of the drawer node
     * @param iddoc Solr IDDOC of the drawer node, used to query its children
     * @param label display label of the drawer (letter group)
     * @param entryCount number of cards in this drawer
     */
    public CardCatalogDrawer(String id, String iddoc, String label, int entryCount) {
        this.id = id;
        this.iddoc = iddoc;
        this.label = label;
        this.entryCount = entryCount;
    }

    public String getId() {
        return id;
    }

    public String getIddoc() {
        return iddoc;
    }

    public String getLabel() {
        return label;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public List<CardCatalogEntry> getEntries() {
        return entries;
    }

    void setEntries(List<CardCatalogEntry> entries) {
        this.entries = entries;
    }

    public List<CardCatalogRegisterRange> getRegisterRanges() {
        return registerRanges;
    }

    void setRegisterRanges(List<CardCatalogRegisterRange> registerRanges) {
        this.registerRanges = registerRanges;
    }
}
