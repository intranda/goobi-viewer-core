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
 * A from-to range of the computed card catalog register: a contiguous block of card positions
 * within one drawer, labeled with the ordering titles of its first and last card.
 */
public class CardCatalogRegisterRange {

    private final int from;
    private final int to;
    private final String fromLabel;
    private final String toLabel;

    /**
     * @param from 1-based position of the first card of this range within its drawer
     * @param to 1-based position of the last card of this range within its drawer
     * @param fromLabel ordering title at (or nearest after) the first position
     * @param toLabel ordering title at (or nearest before) the last position
     */
    public CardCatalogRegisterRange(int from, int to, String fromLabel, String toLabel) {
        this.from = from;
        this.to = to;
        this.fromLabel = fromLabel;
        this.toLabel = toLabel;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public String getFromLabel() {
        return fromLabel;
    }

    public String getToLabel() {
        return toLabel;
    }
}
