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

import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;

/**
 * A {@link TranslationGroupItem} whose keys are Solr field names, resolved either as a literal
 * field name or as a regular expression matched against all known Solr fields.
 */
public class SolrFieldNameTranslationGroupItem extends TranslationGroupItem {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SolrFieldNameTranslationGroupItem.class);

    /**
     * Protected constructor.
     *
     * @param key the Solr field name or pattern
     * @param regex whether the key is a regular expression
     */
    protected SolrFieldNameTranslationGroupItem(String key, boolean regex) {
        super(key, regex);
    }

    @Override
    protected void loadEntries() throws IndexUnreachableException {
        List<String> keys;
        if (regex) {
            keys = StringTools.filterStringsViaRegex(DataManager.getInstance().getSearchIndex().getAllFieldNames(), key);
        } else {
            keys = Collections.singletonList(key);
        }
        createMessageKeyStatusMap(keys);
    }
}
