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
package io.goobi.viewer.model.translations.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

public class SolrFieldValueTranslationGroupItem extends TranslationGroupItem {

    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(SolrFieldValueTranslationGroupItem.class);

    /**
     * Protected constructor.
     * 
     * @param key
     * @param regex
     */
    protected SolrFieldValueTranslationGroupItem(String key, boolean regex) {
        super(key, regex);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.TranslationGroupKey#isTranslated()
     */
    @Override
    public boolean isTranslated() {
        return false; // TODO
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.TranslationGroupKey#loadValues()
     */
    @Override
    protected void loadMessageKeys() {
        try {
            QueryResponse qr = DataManager.getInstance()
                    .getSearchIndex()
                    .searchFacetsAndStatistics("*:*", null, Collections.singletonList(key), 0, false);
            FacetField ff = qr.getFacetField(key);
            List<String> keys = new ArrayList<>(ff.getValueCount());
            for (Count value : ff.getValues()) {
                keys.add(value.getName());
            }
            Collections.sort(keys);
            createMessageKeyStatusMap(keys);
        } catch (PresentationException e) {
            logger.error(e.getMessage());
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage());
        }
    }
}
