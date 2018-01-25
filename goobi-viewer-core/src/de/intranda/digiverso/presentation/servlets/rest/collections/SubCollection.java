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
package de.intranda.digiverso.presentation.servlets.rest.collections;

import java.net.MalformedURLException;
import java.util.Locale;

import de.intranda.digiverso.presentation.model.viewer.CollectionView;
import de.intranda.digiverso.presentation.model.viewer.HierarchicalBrowseDcElement;

/**
 * Part of the IIIF presentation api
 * 
 * Represents a collection that is embedded within another collection in the json+ld response
 * 
 * @author Florian Alpers
 *
 */
public class SubCollection extends Collection {

    /**
     * @param collectionView
     * @param locale
     * @param collectionField
     * @param facetField
     * @throws MalformedURLException
     */
    public SubCollection(CollectionView collectionView, Locale locale, String baseUrl, HierarchicalBrowseDcElement topElement, String collectionField, String facetField, String contextPath) throws MalformedURLException {
        super(collectionView, locale, baseUrl, topElement, collectionField, facetField, contextPath);
    }

}
