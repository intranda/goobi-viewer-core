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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * A Page in a resource list which contains a list of paged items and a reference to the next and previous page and the containing list
 * 
 * @author Florian Alpers
 *
 */
@JsonPropertyOrder({"@context", "id", "type", "prev", "next", "partOf", "orderedItems"})
@JsonInclude(Include.NON_EMPTY)
public class OrderedCollectionPage<T> {

    private final static String TYPE = "OrderedCollectionPage";
    
    private String[] context;
    private final URI id;
    private OrderedCollection<T> partOf;
    private OrderedCollectionPage<T> prev;
    private OrderedCollectionPage<T> next;
    private List<T> orderedItems = new ArrayList<>();
    
    /**
     * Constructs a collection page from the URI to the resource providing this object
     */
    public OrderedCollectionPage(URI id) {
        this.id = id;
    }

    /**
     * Reference to the containing collection
     * 
     * @return the containing collection
     */
    public OrderedCollection<T> getPartOf() {
        return partOf;
    }

    /**
     * Set the reference to the containing collection
     * 
     * @param partOf the containing collection
     */
    public void setPartOf(OrderedCollection<T> partOf) {
        this.partOf = partOf;
    }

    /**
     * Get the previous collection page
     * 
     * @return the previous collection page. May be null if no previous page exists
     */
    public OrderedCollectionPage<T> getPrev() {
        return prev;
    }

    /**
     * Set the previous collection page
     * 
     * @param prev the page to set
     */
    public void setPrev(OrderedCollectionPage<T> prev) {
        this.prev = prev;
    }

    /**
     * Get the succeeding collection page
     * 
     * @return the succeeding collection page. May be null if no succeeding page exists
     */
    public OrderedCollectionPage<T> getNext() {
        return next;
    }

    /**
     * Set the succeeding collection page
     * 
     * @param next the page to set
     */
    public void setNext(OrderedCollectionPage<T> next) {
        this.next = next;
    }

    /**
     * Get the list of items on this page
     * 
     * @return the items
     */
    public List<T> getOrderedItems() {
        return orderedItems;
    }

    /**
     * Set the list of items for this page
     * 
     * @param orderedItems the items to set
     */
    public void setOrderedItems(List<T> orderedItems) {
        this.orderedItems = orderedItems;
    }

    /**
     * @return the context
     */
    @JsonProperty("@context")    
    public String[] getContext() {
        return this.context;
    }
    
    /**
     * The JSON-LD context of the resource
     * 
     * @return The JSON-LD context of the resource
     */
    public void setContext(String[] context) {
        this.context = context;
    }

    /**
     * The type of this resource. Always "OrderedCollectionPage"
     * 
     * @return the type
     */
    public String getType() {
        return TYPE;
    }

    /**
     * @return the URI to the resource providing this object
     */
    public URI getId() {
        return id;
    }
    
    
}
