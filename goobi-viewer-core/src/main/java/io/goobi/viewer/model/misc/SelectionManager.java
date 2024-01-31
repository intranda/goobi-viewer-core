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
package io.goobi.viewer.model.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @param <T>
 * @author florian
 *
 */
public class SelectionManager<T> implements Map<T, Boolean> {

    private final Map<T, Boolean> selectionMap;

    private boolean selectAll = false;

    /**
     * @param allEntries
     */
    public SelectionManager(List<T> allEntries) {
        this.selectionMap = allEntries.stream().collect(Collectors.toMap(t -> t, t -> false));
    }

    public SelectionManager() {
        this.selectionMap = new HashMap<>();
    }

    /**
     * @return the selectAll
     */
    public boolean isSelectAll() {
        return selectAll;
    }

    /**
     * @param selectAll the selectAll to set
     */
    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public Boolean select(T item) {
        return setSelected(item, Boolean.TRUE);
    }

    public Boolean deselect(T item) {
        return setSelected(item, Boolean.FALSE);
    }

    public Boolean get(Object item) {
        if (isSelectAll()) {
            return Boolean.TRUE;
        }
        return Optional.ofNullable(selectionMap.get(item)).orElse(Boolean.FALSE);
    }

    @SuppressWarnings("unchecked")
    public Boolean put(Object item, Boolean selected) {
        try {
            return setSelected((T) item, selected);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Key is wong type");
        }
    }

    public Boolean isSelected(T item) {
        return get(item);
    }

    public Boolean setSelected(T item, boolean selected) {
        if (!selected) {
            setSelectAll(false);
        }
        return selectionMap.put(item, selected);
    }

    /**
     * @return List<T>
     */
    public List<T> getAllSelected() {
        if (isSelectAll()) {
            return new ArrayList<>(selectionMap.keySet());
        }
        
        return selectionMap.entrySet().stream().filter(e -> Boolean.TRUE.equals(e.getValue())).map(Entry::getKey).collect(Collectors.toList());
    }

    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    @Override
    public void clear() {
        this.setSelectAll(false);
        selectionMap.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return selectionMap.containsKey(key);
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue(Object value) {
        return selectionMap.containsValue(value);
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<Entry<T, Boolean>> entrySet() {
        return selectionMap.entrySet();
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return selectionMap.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<T> keySet() {
        return selectionMap.keySet();
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends T, ? extends Boolean> m) {
        if (m.containsValue(Boolean.FALSE)) {
            setSelectAll(false);
        }
        selectionMap.putAll(m);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public Boolean remove(Object key) {
        return selectionMap.remove(key);
    }

    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    @Override
    public int size() {
        return selectionMap.size();
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    @Override
    public Collection<Boolean> values() {
        return selectionMap.values();
    }

}
