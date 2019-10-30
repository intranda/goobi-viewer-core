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
package io.goobi.viewer.model.iiif.search;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.iiif.search.SearchResult;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * @author florian
 *
 */
public class IIIFAutocompleteBuilder extends IIIFSearchBuilder{
    
    /**
     * @param requestURI
     * @param query
     * @param pi
     */
    public IIIFAutocompleteBuilder(URI requestURI, String query, String pi) {
        super(requestURI, query, pi);
    }

    private static final Logger logger = LoggerFactory.getLogger(IIIFAutocompleteBuilder.class);

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.iiif.search.IIIFSearchBuilder#build()
     */
    @Override
    public SearchResult build() throws PresentationException, IndexUnreachableException {
        SearchResult searchResult = super.build();
        
    }

}
