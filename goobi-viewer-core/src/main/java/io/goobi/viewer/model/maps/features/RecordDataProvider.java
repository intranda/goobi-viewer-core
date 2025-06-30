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

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

public class RecordDataProvider extends AbstractFeatureDataProvider {

    private final boolean includeMetadataGroups;

    public RecordDataProvider(SolrSearchIndex searchIndex, List<String> requiredFields, boolean includeMetadataGroups) {
        super(searchIndex, requiredFields);
        this.includeMetadataGroups = includeMetadataGroups;
    }

    public List<MetadataDocument> getResults(String query, int maxResults) throws PresentationException, IndexUnreachableException {

        String docsQuery = "+(%s) +(ISWORK:true ISANCHOR:true)".formatted(query);
        String finalQuery = SearchHelper.buildFinalQuery(docsQuery, false, SearchAggregationType.NO_AGGREGATION);

        String expandQuery = "+DOCTYPE:METADATA";
        Map<String, String> paramMap = this.includeMetadataGroups ? SearchHelper.getExpandQueryParams(expandQuery, null) : Collections.emptyMap();

        return search(maxResults, finalQuery, paramMap);
    }

    protected MetadataDocument getMetadataDocument(QueryResponse response, SolrDocument mainDoc) {
        String pi = mainDoc.getFirstValue(SolrConstants.PI).toString();
        if (response.getExpandedResults() != null && response.getExpandedResults().get(pi) != null) {
            List<SolrDocument> mdDocs = response.getExpandedResults()
                    .get(pi)
                    .stream()
                    .filter(doc -> doc.getFirstValue(SolrConstants.DOCTYPE).equals(SolrConstants.DocType.METADATA.name()))
                    .toList();
            List<SolrDocument> childDocs = response.getExpandedResults()
                    .get(pi)
                    .stream()
                    .filter(doc -> doc.getFirstValue(SolrConstants.DOCTYPE).equals(SolrConstants.DocType.DOCSTRCT.name()))
                    .toList();
            return MetadataDocument.fromSolrDocs(mainDoc, childDocs, mdDocs);
        } else {
            return MetadataDocument.fromSolrDocs(mainDoc, Collections.emptyList(), Collections.emptyList());
        }
    }

}
