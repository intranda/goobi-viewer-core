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
package io.goobi.viewer.controller;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author florian
 * @param <T>
 *
 */
public class RandomComparator<T> implements Comparator<T> {
    private final Map<T, Integer> map = new IdentityHashMap<>();
    private final SecureRandom random;

    public RandomComparator() {
        this(new SecureRandom());
    }

    public RandomComparator(SecureRandom random) {
        this.random = random;
    }

    @Override
    public int compare(T t1, T t2) {
        return Integer.compare(valueFor(t1), valueFor(t2));
    }

    private int valueFor(T t) {
        synchronized (map) {
            return map.computeIfAbsent(t, ignore -> random.nextInt());
        }
    }
}
