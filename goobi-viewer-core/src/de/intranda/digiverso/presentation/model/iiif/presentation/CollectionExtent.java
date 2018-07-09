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
package de.intranda.digiverso.presentation.model.iiif.presentation;

import java.net.URI;
import java.net.URISyntaxException;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.servlets.rest.services.Service;

/**
 * @author Florian Alpers
 *
 */
public class CollectionExtent implements Service {

    private static final String CONTEXTPATH = "api/collections/extent/context.json";

    private int children;
    private int containedWorks;
    
    /**
     * 
     */
    public CollectionExtent() {
       this.children = 0;
       this.containedWorks = 0;
    }
    
    /**
     * 
     * @param children     The number of direct subcollections of this collection
     * @param containedWorks    The number of works within this collection (including subcollections)
     */
    public CollectionExtent(int items, int extent) {
        this.children = items;
        this.containedWorks = extent;
     }
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.servlets.rest.services.Service#getContext()
     */
    @Override
    public URI getContext() throws URISyntaxException, ViewerConfigurationException {
        return new URI(DataManager.getInstance().getConfiguration().getRestApiUrl() + CONTEXTPATH);
    }
    
    /**
     * @return The number of direct subcollections of this collection
     */
    public int getChildren() {
        return children;
    }
    
    /**
     * @return The number of works within this collection (including subcollections)
     */
    public int getContainedWorks() {
        return containedWorks;
    }
    
    /**
     * @param items the items to set
     */
    public void setChildren(int items) {
        this.children = items;
    }
    
    /**
     * @param extent the extent to set
     */
    public void setCcontainedWorks(int extent) {
        this.containedWorks = extent;
    }

}
