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
package io.goobi.viewer.controller.model;

import java.util.LinkedHashMap;
import java.util.function.Function;

/**
 * A thread-safe, size-bounded {@link java.util.LinkedHashMap} that evicts the eldest entry
 * when the map exceeds its configured maximum capacity. Suitable as a simple in-memory LRU
 * cache for moderate data volumes.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class CachingMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1882556825980074260L;
    private final long maxSize;

    public CachingMap(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public synchronized V get(Object key) {
        return super.get(key);
    }

    @Override
    public synchronized V getOrDefault(Object key, V defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    public synchronized V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public synchronized V computeIfAbsent(K arg0, Function<? super K, ? extends V> arg1) {
        return super.computeIfAbsent(arg0, arg1);
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > this.maxSize;
    }
}
