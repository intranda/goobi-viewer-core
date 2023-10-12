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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CmsCollectionsBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.search.SearchHelper;

/**
 * Manages contentAssist requests by returning lists of suggested values from partial input
 *
 * @author florian
 *
 */
public class ContentAssistResourceBuilder {

    /**
     * <p>
     * getCollections.
     * </p>
     *
     * @param solrField a {@link java.lang.String} object.
     * @param inputString a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws IllegalRequestException if the solrField doesn't exist in the index
     * @throws IndexUnreachableException If an error occured communicating with the SOLR index
     */

    public List<String> getCollections(String solrField, String inputString)
            throws IllegalRequestException, IndexUnreachableException {
        if ("-".equals(inputString)) {
            inputString = "";
        }
        String query = "DOCTYPE:DOCSTRCT AND (ISANCHOR:true OR ISWORK:true)";
        try {
            List<String> facets = SearchHelper.getFacetValues(query, solrField, inputString, 0, null);
            List<String> collections = new ArrayList<>();
            CmsCollectionsBean bean = BeanUtils.getCMSCollectionsBean();
            if (bean != null) {
                collections.addAll(bean.getCollections().stream().map(collection -> collection.getSolrFieldValue()).collect(Collectors.toList()));
            }
            String splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(solrField);
            List<String> list = facets.stream()
                    .flatMap(facet -> getHierarchy("", facet, splittingChar).stream())
                    .distinct()
                    .filter(facet -> !collections.contains(facet))
                    .sorted()
                    .sorted((f1, f2) -> Integer.compare(f1.split(splittingChar).length, f2.split(splittingChar).length))
                    .collect(Collectors.toList());

            return list;
        } catch (PresentationException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("bad query")) {
                throw new IllegalRequestException("Not a valid SOLR field: " + solrField);
            } else {
                throw new IndexUnreachableException("Internal error in index:" + e.toString());
            }
        }

    }

    /**
     *
     * @param prefix
     * @param facet
     * @param splittingChar
     * @return
     */
    private List<String> getHierarchy(String prefix, String facet, String splittingChar) {
        if (splittingChar == null || !facet.contains(splittingChar)) {
            ArrayList<String> list = new ArrayList<>();
            list.add(prefix + facet);
            return list;
        }

        int firstSeparator = facet.indexOf(splittingChar);
        String parent = facet.substring(0, firstSeparator);
        List<String> children =
                getHierarchy(prefix + parent + splittingChar, facet.substring(firstSeparator + splittingChar.length()), splittingChar);
        children.add(prefix + parent);
        return children;
    }
}
