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
package de.intranda.digiverso.presentation.model.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Florian Alpers
 *
 */
public class CollectionLabeledLink extends CompoundLabeledLink {

    /**
     * 
     */
    private static final long serialVersionUID = -318199786884811710L;
    private final CollectionView collection;

    /**
     * @param name
     * @param url
     * @param weight
     */
    public CollectionLabeledLink(String name, String url, CollectionView collection, int weight) {
        super(name, url, weight);
        this.collection = collection;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.viewer.CompoundLabeledLink#getSubLinks()
     */
    @Override
    public List<LabeledLink> getSubLinks() {
        List<LabeledLink> links = collection.getAncestors(collection.getTopVisibleElement(), true)
        .stream()
        .map(element -> new LabeledLink(element.getName(), collection.getCollectionUrl(element), 0))
        .collect(Collectors.toList());
       links.add(0, this);
       return  links;
    }

}
