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

import java.util.ArrayList;
import java.util.List;

/**
 * The computed from-to register of a card catalog: per drawer a list of position ranges labeled
 * with the ordering titles of their first and last card. Only available while the catalog's total
 * card count does not exceed the configured register threshold.
 */
public class CardCatalogRegister {

    private boolean available;
    private List<RegisterDrawer> drawers = new ArrayList<>();

    public static class RegisterDrawer {

        private String id;
        private String label;
        private int entryCount;
        private List<Range> ranges = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public void setEntryCount(int entryCount) {
            this.entryCount = entryCount;
        }

        public List<Range> getRanges() {
            return ranges;
        }
    }

    public static class Range {

        private int from;
        private int to;
        private String fromLabel;
        private String toLabel;

        public Range() {
        }

        public Range(int from, int to, String fromLabel, String toLabel) {
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<RegisterDrawer> getDrawers() {
        return drawers;
    }
}
