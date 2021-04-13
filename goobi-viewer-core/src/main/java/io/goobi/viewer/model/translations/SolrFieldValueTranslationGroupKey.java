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
package io.goobi.viewer.model.translations;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

public class SolrFieldValueTranslationGroupKey extends TranslationGroupKey {

    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(SolrFieldValueTranslationGroupKey.class);

    /**
     * Protected constructor.
     * 
     * @param value
     * @param regex
     */
    protected SolrFieldValueTranslationGroupKey(String value, boolean regex) {
        super(value, regex);
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
    protected void loadValues() {
        if (regex) {
            // TODO
        } else {
            try {
                QueryResponse qr = DataManager.getInstance()
                        .getSearchIndex()
                        .searchFacetsAndStatistics(key + "*:*", null, Collections.singletonList(key), 0, false);
                FacetField ff = qr.getFacetField(key);
                values = new ArrayList<>(ff.getValueCount());
                for (Count value : ff.getValues()) {
                    values.add(value.getName());
                }
                Collections.sort(values);
            } catch (PresentationException e) {
                logger.error(e.getMessage());
            } catch (IndexUnreachableException e) {
                logger.error(e.getMessage());
            }
        }
    }
}
