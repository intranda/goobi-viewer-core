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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.model.viewer.collections.HierarchicalBrowseDcElement;
import io.goobi.viewer.model.viewer.collections.SimpleBrowseElementInfo;

/**
 * <p>
 * CompoundLabeledLink class.
 * </p>
 *
 * @author Florian Alpers
 */
public class CompoundLabeledLink extends LabeledLink {

    private static final long serialVersionUID = 2336154265426936610L;

    private static final Logger logger = LogManager.getLogger(CompoundLabeledLink.class);

    protected final String field;
    protected final List<String> hierarchy;

    /**
     * <p>
     * Constructor for CompoundLabeledLink.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param weight a int.
     * @param field a {@link java.lang.String} object.
     */
    public CompoundLabeledLink(String name, String url, String field, int weight) {
        super(name, url, weight);
        this.field = field;
        this.hierarchy = Collections.emptyList();
    }

    /**
     * <p>
     * Constructor for CompoundLabeledLink.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     * @param url a {@link java.lang.String} object.
     * @param field a {@link java.lang.String} object.
     * @param hierarchy a {@link java.util.List} object.
     * @param weight a int.
     */
    public CompoundLabeledLink(String name, String url, String field, List<String> hierarchy, int weight) {
        super(name, url, weight);
        this.field = field;
        this.hierarchy = hierarchy;
    }

    /**
     * <p>
     * getSubLinks.
     * </p>
     *
     * @return List of labeled links, one for each hierarchy level
     */
    public List<LabeledLink> getSubLinks() {
        logger.trace("getSubLinks");
        List<LabeledLink> links = new ArrayList<>(hierarchy.size());
        List<HierarchicalBrowseDcElement> collectionElements = new ArrayList<>(hierarchy.size());
        String splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(field);
        Map<String, String> sortFields = DataManager.getInstance().getConfiguration().getCollectionDefaultSortFields(field);
        int displayNumberOfVolumesLevel = DataManager.getInstance().getConfiguration().getCollectionDisplayNumberOfVolumesLevel(field);
        try {
            for (String col : hierarchy) {
                String sortField = CollectionView.getCollectionDefaultSortField(col, sortFields);
                HierarchicalBrowseDcElement collectionElement =
                        new HierarchicalBrowseDcElement(col, 1, field, sortField, splittingChar, displayNumberOfVolumesLevel);
                collectionElement.setInfo(new SimpleBrowseElementInfo(col, null, null));
                // Actual collection size is expensive to determine at this point, so instead pretend it's larger than one
                // so that no redirection to the first volume takes place for all collections
                collectionElement.addToNumber(1);
                collectionElements.add(collectionElement);
            }
            CollectionView.associateWithCMSCollections(collectionElements, field);
            int subLinkWeight = weight;
            // Add collection hierarchy links with the same weight
            for (HierarchicalBrowseDcElement collectionElement : collectionElements) {
                links.add(new LabeledLink(collectionElement.getLabel(), CollectionView.getCollectionUrl(collectionElement, field, null),
                        subLinkWeight));
            }
        } catch (DAOException | PresentationException e) {
            logger.error(e.getMessage());
        }
        
        return links;
    }

}
