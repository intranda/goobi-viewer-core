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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compact positional index of one card catalog archive: the ordered list of its drawers with card
 * counts, built from a handful of small Solr queries instead of materializing the full archive
 * tree. Card lists are loaded lazily per drawer.
 */
public class CardCatalogIndex {

    private final String pi;
    private final String label;
    private final long dateIndexed;
    private final List<CardCatalogDrawer> drawers;
    private final Map<String, CardCatalogDrawer> drawersById;
    private final int totalEntries;
    /** Timestamp of the last staleness check against Solr, used to throttle DATEINDEXED lookups. */
    private final AtomicLong lastStalenessCheck = new AtomicLong(System.currentTimeMillis());

    /**
     * @param pi PI of the archive resource
     * @param label display label of the archive
     * @param dateIndexed latest DATEINDEXED value of the archive's root document
     * @param drawers drawers in catalog order
     */
    public CardCatalogIndex(String pi, String label, long dateIndexed, List<CardCatalogDrawer> drawers) {
        this.pi = pi;
        this.label = label;
        this.dateIndexed = dateIndexed;
        this.drawers = Collections.unmodifiableList(drawers);
        this.drawersById = drawers.stream().collect(Collectors.toMap(CardCatalogDrawer::getId, Function.identity(), (a, b) -> a));
        this.totalEntries = drawers.stream().mapToInt(CardCatalogDrawer::getEntryCount).sum();
    }

    public String getPi() {
        return pi;
    }

    public String getLabel() {
        return label;
    }

    public long getDateIndexed() {
        return dateIndexed;
    }

    public List<CardCatalogDrawer> getDrawers() {
        return drawers;
    }

    public Optional<CardCatalogDrawer> getDrawer(String drawerId) {
        return Optional.ofNullable(drawersById.get(drawerId));
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    /**
     * @param intervalMillis minimum interval between staleness checks
     * @return true if the last staleness check is older than the given interval
     */
    public boolean isStalenessCheckDue(long intervalMillis) {
        return System.currentTimeMillis() - lastStalenessCheck.get() > intervalMillis;
    }

    public void touchStalenessCheck() {
        lastStalenessCheck.set(System.currentTimeMillis());
    }
}
