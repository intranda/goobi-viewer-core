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
package de.intranda.digiverso.presentation.servlets.rest.services;

/**
 * @author Florian Alpers
 *
 */
public class CollectionExtentDefinition {

    private static final Context DCTERMS = new Context("dcTerms",  "http://purl.org/dc/terms/");
    private static final Context ACTIVITYSTREAMS = new Context("as",  "http://www.w3.org/ns/activitystreams#");
    
    private static final Definition ITEMS = new Definition(ACTIVITYSTREAMS, "totalItems");
    private static final Definition EXTENT = new Definition(DCTERMS, "extent");
    
    public Context getDcterms() {
        return DCTERMS;
    }
    
    public Context getAs() {
        return ACTIVITYSTREAMS;
    }
    
    public Definition getItems() {
        return ITEMS;
    }
    
    public Definition getExtent() {
        return EXTENT;
    }

}
