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
package io.goobi.viewer.model.maps.features;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.maps.GlobalFeatureDataProvider;
import io.goobi.viewer.model.maps.SolrSearchScope;
import io.goobi.viewer.solr.SolrSearchIndex;

public abstract class AbstractFeatureDataProvider implements IFeatureDataProvider {

    private final SolrSearchIndex searchIndex;
    private final List<String> requiredFields;

    public AbstractFeatureDataProvider(SolrSearchIndex searchIndex, List<String> requiredFields) {
        this.searchIndex = searchIndex;
        this.requiredFields = requiredFields;
    }

    List<MetadataDocument> search(int maxResults, String finalQuery, Map<String, String> paramMap)
            throws PresentationException, IndexUnreachableException {
        QueryResponse response =
                searchIndex
                        .search(finalQuery, 0, maxResults, Collections.emptyList(), Collections.emptyList(), getFieldList(),
                                Collections.emptyList(), paramMap);
        return response.getResults().stream().map(doc -> getMetadataDocument(response, doc)).toList();
    }

    List<String> getFieldList() {
        List<String> fieldList = (requiredFields == null || requiredFields.isEmpty()) ? Collections.emptyList()
                : ListUtils.union(this.requiredFields, IFeatureDataProvider.REQUIRED_FIELDS);
        return fieldList;
    }

    public SolrSearchIndex getSearchIndex() {
        return searchIndex;
    }

    public List<String> getRequiredFields() {
        return requiredFields;
    }

    public static IFeatureDataProvider getDataProvider(SolrSearchScope scope, List<String> requiredFields) {
        switch (scope) {
            case RECORDS:
                return new RecordDataProvider(DataManager.getInstance().getSearchIndex(), requiredFields, false);
            case DOCSTRUCTS:
                return new DocStructDataProvider(DataManager.getInstance().getSearchIndex(), requiredFields);
            case METADATA:
                return new MetadataDataProvider(DataManager.getInstance().getSearchIndex(), requiredFields);
            case RELATIONSHIPS:
                return new RelationshipDataProvider(DataManager.getInstance().getSearchIndex(), requiredFields);
            case ALL:
            default:
                return new GlobalFeatureDataProvider(DataManager.getInstance().getSearchIndex(), requiredFields);
        }
    }

    protected abstract MetadataDocument getMetadataDocument(QueryResponse response, SolrDocument doc);

    @Override
    public abstract List<MetadataDocument> getResults(String query, int maxResults) throws PresentationException, IndexUnreachableException;

}
