/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.intranda.digiverso.presentation.servlets.rest.iiif.presentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Florian Alpers
 *
 */
public class PropertyList<T> implements Collection<T>{

    private final List<T> items;
    
    public PropertyList(List<T> items) {
        this.items = items;
    }
    
    public PropertyList() {
        this.items = new ArrayList<T>();
    }
    
    @JsonValue
    @JsonInclude(Include.NON_ABSENT)
    public Object getValue() {
        if(this.items.isEmpty()) {
            return null;
        } else if(this.items.size() == 1) {
            return this.items.get(0);
        } else {
            return this.items;
        }
    }
    
    public List<T> getItems() {
        return items;
    }
    
    public void addItem(T item) {
        this.items.add(item);
    }
    
    public void removeItem(T item) {
        this.items.remove(item);
    }
    
    public void clear() {
        this.items.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#add(java.lang.Object)
     */
    @Override
    public boolean add(T e) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends T> c) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#contains(java.lang.Object)
     */
    @Override
    public boolean contains(Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#containsAll(java.util.Collection)
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.util.Collection#iterator()
     */
    @Override
    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object o) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#removeAll(java.util.Collection)
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#retainAll(java.util.Collection)
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#size()
     */
    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#toArray()
     */
    @Override
    public Object[] toArray() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#toArray(java.lang.Object[])
     */
    @Override
    public <T> T[] toArray(T[] a) {
        // TODO Auto-generated method stub
        return null;
    }

}
