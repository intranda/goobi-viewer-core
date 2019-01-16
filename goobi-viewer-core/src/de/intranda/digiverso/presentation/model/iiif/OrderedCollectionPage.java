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
    private OrderedCollection partOf;
    private OrderedCollectionPage<T> prev;
    private OrderedCollectionPage<T> next;
    private List<T> orderedItems = new ArrayList<>();
    
    public OrderedCollectionPage(URI id) {
        this.id = id;
    }

    /**
     * @return the partOf
     */
    public OrderedCollection getPartOf() {
        return partOf;
    }

    /**
     * @param partOf the partOf to set
     */
    public void setPartOf(OrderedCollection partOf) {
        this.partOf = partOf;
    }

    /**
     * @return the prev
     */
    public OrderedCollectionPage getPrev() {
        return prev;
    }

    /**
     * @param prev the prev to set
     */
    public void setPrev(OrderedCollectionPage prev) {
        this.prev = prev;
    }

    /**
     * @return the next
     */
    public OrderedCollectionPage getNext() {
        return next;
    }

    /**
     * @param next the next to set
     */
    public void setNext(OrderedCollectionPage next) {
        this.next = next;
    }

    /**
     * @return the orderedItems
     */
    public List<T> getOrderedItems() {
        return orderedItems;
    }

    /**
     * @param orderedItems the orderedItems to set
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
     * @param context the context to set
     */
    public void setContext(String[] context) {
        this.context = context;
    }

    /**
     * @return the type
     */
    public String getType() {
        return TYPE;
    }

    /**
     * @return the id
     */
    public URI getId() {
        return id;
    }
    
    
}
