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

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;

/**
 * @author Florian Alpers
 *
 */
public class CompoundLabeledLink extends LabeledLink {
    
    private static final long serialVersionUID = 2336154265426936610L;

    protected final String field;
    protected final List<String> hierarchy;

    /**
     * @param name
     * @param url
     * @param weight
     */
    public CompoundLabeledLink(String name, String url, String field, int weight) {
        super(name, url, weight);
        this.field = field;
        this.hierarchy = Collections.emptyList();
    }

    /**
     * 
     * @param name
     * @param url
     * @param field
     * @param hierarchy
     * @param weight
     */
    public CompoundLabeledLink(String name, String url, String field, List<String> hierarchy, int weight) {
        super(name, url, weight);
        this.field = field;
        this.hierarchy = hierarchy;
    }

    /**
     * 
     * @return List of labeled links, one for each hierarchy level
     */
    public List<LabeledLink> getSubLinks() {
        List<LabeledLink> links = new ArrayList<>(hierarchy.size());
        List<HierarchicalBrowseDcElement> collectionElements = new ArrayList<>(hierarchy.size());
        try {
            for (String col : hierarchy) {
                HierarchicalBrowseDcElement collectionElement = new HierarchicalBrowseDcElement(col, 1, field, field);
                collectionElement.setInfo(new SimpleBrowseElementInfo(col, null, null));
                collectionElements.add(collectionElement);
            }
            CollectionView.associateWithCMSCollections(collectionElements, field);
            int subLinkWeight = weight;
            // Add collection hierarchy links with the same weight
            for (HierarchicalBrowseDcElement collectionElement : collectionElements) {
                links.add(new LabeledLink(collectionElement.getName(), CollectionView.getCollectionUrl(collectionElement, field), subLinkWeight));
            }
        } catch (PresentationException e) {
        } catch (DAOException e) {
        }

        return links;
    }

}
