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
package io.goobi.viewer.model.translations.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

public class SolrFieldValueTranslationGroupItem extends TranslationGroupItem {

    /** Logger for this class */
    private static final Logger logger = LogManager.getLogger(SolrFieldValueTranslationGroupItem.class);

    /**
     * Protected constructor.
     *
     * @param key
     * @param regex
     */
    protected SolrFieldValueTranslationGroupItem(String key, boolean regex) {
        super(key, regex);
    }

    /**
     * @see io.goobi.viewer.model.translations.TranslationGroupKey#loadValues()
     * @should load hierarchical entries correctly
     */
    @Override
    protected void loadEntries() throws PresentationException, IndexUnreachableException {
        // logger.trace("loadEntries");
        QueryResponse qr = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics("*:*", null, Collections.singletonList(key), 1, false);
        FacetField ff = qr.getFacetField(key);
        List<String> keys = new ArrayList<>(ff.getValueCount());
        if (!ff.getValues().isEmpty()) {
            Set<String> addedValues = new HashSet<>();
            String splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(key);
            boolean useSplittingChar = StringUtils.isNotEmpty(splittingChar);
            for (Count count : ff.getValues()) {
                if (useSplittingChar && count.getName().contains(splittingChar)) {
                    // For collection names, etc., add an entry for every level
                    String[] valueNameSplit = count.getName().split("[" + splittingChar + "]");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < valueNameSplit.length; ++i) {
                        if (i > 0) {
                            sb.append(splittingChar);
                        }
                        sb.append(valueNameSplit[i]);
                        if (!addedValues.contains(sb.toString())) {
                            keys.add(sb.toString());
                            addedValues.add(sb.toString());
                        }
                    }
                } else {
                    // Regular values
                    if (!StringTools.checkValueEmptyOrInverted(count.getName())) {
                        keys.add(count.getName());
                    }
                }
            }
        }
        Collections.sort(keys);
        createMessageKeyStatusMap(keys);
    }
}
