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
package io.goobi.viewer.model.cms.media;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Comparator that sorts as many items marked as high priority to the beginning of the list as are given in the constructor The remaining items will
 * be sorted randomly if the random parameter is true or else by the {@link CMSMediaItem#compareTo(CMSMediaItem)}
 *
 * @author florian
 *
 */
public class PriorityComparator implements Comparator<CMSMediaItem> {

    private final int prioritySlots;
    private final boolean random;
    private final Random randomizer = new SecureRandom();
    private final Map<CMSMediaItem, Integer> map = new IdentityHashMap<>();
    private final List<CMSMediaItem> priorityList = new ArrayList<>();

    public PriorityComparator(Integer prioritySlots, boolean random) {
        this.prioritySlots = prioritySlots == null ? 0 : prioritySlots;
        this.random = random;
    }

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(CMSMediaItem a, CMSMediaItem b) {
        maybeAddToPriorityList(a);
        maybeAddToPriorityList(b);
        if (priorityList.contains(a) && !priorityList.contains(b)) {
            return -1;
        } else if (priorityList.contains(b) && !priorityList.contains(a)) {
            return 1;
        } else if (a.getDisplayOrder() != 0 && b.getDisplayOrder() != 0) {
            return Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
        } else if (a.getDisplayOrder() != 0) {
            return -1;
        } else if (b.getDisplayOrder() != 0) {
            return 1;
        } else if (random) {
            return Integer.compare(valueFor(a), valueFor(b));
        } else {
            return a.compareTo(b);
        }
    }

    /**
     * 
     * @param a
     * @return an int
     */
    private int valueFor(CMSMediaItem a) {
        synchronized (map) {
            return map.computeIfAbsent(a, ignore -> randomizer.nextInt());
        }
    }

    /**
     * @param teim
     */
    private void maybeAddToPriorityList(CMSMediaItem item) {
        if (item.isImportant() && priorityList.size() < prioritySlots && !priorityList.contains(item)) {
            priorityList.add(item);
        }
    }

}