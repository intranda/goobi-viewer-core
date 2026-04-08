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
package io.goobi.viewer.model.viewer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.goobi.viewer.model.viewer.collections.CollectionView;

/**
 * A labeled link targeting a collection browse page, carrying the collection name alongside label and URL.
 *
 * @author Florian Alpers
 */
public class CollectionLabeledLink extends CompoundLabeledLink {

    /**
     */
    private static final long serialVersionUID = -318199786884811710L;
    private final CollectionView collection;

    /**
     * Creates a new CollectionLabeledLink instance.
     *
     * @param name display label of the collection link
     * @param url target URL of the collection link
     * @param collection collection view providing sub-links and field information
     * @param weight sort weight of this link
     */
    public CollectionLabeledLink(String name, String url, CollectionView collection, int weight) {
        super(name, url, Optional.ofNullable(collection).map(CollectionView::getField).orElse(null), weight);
        this.collection = collection;
    }

    /** {@inheritDoc} */
    @Override
    public List<LabeledLink> getSubLinks() {
        if (collection == null) {
            return Collections.emptyList();
        }

        List<LabeledLink> links = collection.getAncestors(collection.getBaseElementName(), true)
                .stream()
                .map(element -> new LabeledLink(element.getName(), collection.getCollectionUrl(element), 0))
                .collect(Collectors.toList());
        links.add(0, this);
        return links;
    }

}
