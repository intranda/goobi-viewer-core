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
package de.intranda.digiverso.presentation.model.iiif;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A collection of items, divided into pages
 * 
 * @author Florian Alpers
 *
 */
@JsonPropertyOrder({"@context", "id", "type", "totalItems", "first", "last"})
@JsonInclude(Include.NON_EMPTY)
public class OrderedCollection<T> {

    private final static String TYPE = "OrderedCollection";
    
    private String[] context;
    private final URI id;
    private long totalItems;
    private OrderedCollectionPage<T> first;
    private OrderedCollectionPage<T> last;
    
    /**
     * Constructs a collection from the URI to the resource providing this object
     */
    public OrderedCollection(URI id) {
       this.id = id;
    }
    
    /**
     * @return the URI to the resource providing this object
     */
    public URI getId() {
        return id;
    }
    
    /**
     * Get the total number of items contained in the collection
     * 
     * @return the total number of items contained in the collection
     */
    public long getTotalItems() {
        return totalItems;
    }
    /**
     * Set the total number of items contained in the collection
     * 
     * @param totalItems the totalItems to set
     */
    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }
    /**
     * Returns a reference to the first page of this collection
     * 
     * @return a reference to the first page of this collection
     */
    public OrderedCollectionPage<T> getFirst() {
        return first;
    }
    /**
     *Sets the first page of this collection
     * 
     * @param first the first page to set
     */
    public void setFirst(OrderedCollectionPage<T> first) {
        this.first = first;
    }
    
    /**
     * Returns a reference to the last page of this collection
     * 
     * @return a reference to the last page of this collection
     */
    public OrderedCollectionPage<T> getLast() {
        return last;
    }
    /**
     *Sets the last page of this collection
     * 
     * @param last the last page to set
     */
    public void setLast(OrderedCollectionPage<T> last) {
        this.last = last;
    }
    /**
     * The JSON-LD context of the resource
     * 
     * @return The JSON-LD context of the resource
     */
    @JsonProperty("@context")    
    public String[] getContext() {
        return this.context;
    }
    
    /**
     * Set the JSON-LD context of the resource
     * 
     * @param context the context to set
     */
    public void setContext(String[] context) {
        this.context = context;
    }
    
    /**
     * The type of this resource. Always "OrderedCollection"
     * 
     * @return the type
     */
    public String getType() {
        return TYPE;
    }
    
    
}
