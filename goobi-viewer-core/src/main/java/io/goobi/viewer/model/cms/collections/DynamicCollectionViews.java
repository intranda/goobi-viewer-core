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
package io.goobi.viewer.model.cms.collections;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.faces.validators.SolrQueryValidator;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.viewer.collections.CollectionView;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Factory for building a {@link CollectionView} whose entries are the database-defined {@link DynamicCollection}s. Used by every rendering path that
 * supports the {@link SolrConstants#DC_DYNAMIC} pseudo field (server-side collection listings and the IIIF collection REST API backing the
 * client-side accordion widget), so that all paths produce the same flat set of entries with the same labels, thumbnails, links and hit counts.
 */
public final class DynamicCollectionViews {

    private static final Logger logger = LogManager.getLogger(DynamicCollectionViews.class);

    private DynamicCollectionViews() {
        //
    }

    /**
     * Builds a flat {@link CollectionView} of all dynamic collections, populated and with each entry's {@link DynamicCollection} attached as its
     * browse-element info (so label, description, thumbnail and link resolve from the DB).
     *
     * @param subtheme subtheme discriminator value to keep links within, may be empty
     * @param filterQuery additional filter query (component filter + subtheme) AND-combined with each collection's query for the hit count, may be
     *            blank
     * @return the populated collection view
     * @throws IndexUnreachableException if any.
     * @throws IllegalRequestException if any.
     */
    public static CollectionView build(String subtheme, String filterQuery) throws IndexUnreachableException, IllegalRequestException {
        CollectionView collection = new CollectionView(SolrConstants.DC_DYNAMIC, () -> buildMap(filterQuery));
        collection.setSubtheme(StringUtils.trimToEmpty(subtheme));
        // Dynamic collections are a flat set, not a Solr hierarchy
        collection.setIgnoreHierarchy(true);
        // An empty string marks "no base collection" the same way the regular collection views do; leaving it null makes callers that compare the
        // base element name against a blank configuration value repopulate the view on every access
        collection.setBaseElementName("");
        // Populating attaches the DB collection info (label, description, thumbnail, link) to each entry
        collection.populateCollectionList();
        return collection;
    }

    /**
     * Builds the collection map for the dynamic-collection listing: one entry per {@link DynamicCollection} with a non-blank query, keyed by its
     * identifier, with the record count of its stored query AND-combined with the given filter query.
     *
     * @param filterQuery additional filter query to AND-combine with each collection's query, may be blank
     * @return map of identifier to {@link CollectionResult}
     */
    public static Map<String, CollectionResult> buildMap(String filterQuery) {
        Map<String, CollectionResult> map = new HashMap<>();
        try {
            for (DynamicCollection dynamicCollection : DataManager.getInstance().getDao().getAllDynamicCollections()) {
                if (StringUtils.isBlank(dynamicCollection.getSolrQuery())) {
                    continue;
                }
                long count = getHitCount(dynamicCollection.getSolrQuery(), filterQuery);
                map.put(dynamicCollection.getIdentifier(), new CollectionResult(dynamicCollection.getIdentifier(), count));
            }
        } catch (DAOException e) {
            logger.error("Error loading dynamic collections: {}", e.getMessage());
        }
        return map;
    }

    /**
     * @param solrQuery the dynamic collection's stored Solr query
     * @param filterQuery additional filter query to AND-combine, may be blank
     * @return the number of records matching the combined query, or 0 on error
     */
    private static long getHitCount(String solrQuery, String filterQuery) {
        String query = StringUtils.isNotBlank(filterQuery) ? "(" + solrQuery + ") AND (" + filterQuery + ")" : solrQuery;
        try {
            return SolrQueryValidator.getHitCount(query);
        } catch (SolrServerException | IOException e) {
            logger.error("Error counting hits for dynamic collection query '{}': {}", query, e.getMessage());
            return 0;
        }
    }
}
