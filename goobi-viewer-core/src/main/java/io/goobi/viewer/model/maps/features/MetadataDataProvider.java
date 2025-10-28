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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;

public class MetadataDataProvider extends AbstractFeatureDataProvider {

    private static final int MAX_METATA_PER_RECORD = 1000;

    public MetadataDataProvider(SolrSearchIndex searchIndex, List<String> requiredFields) {
        super(searchIndex, requiredFields);
    }

    public List<MetadataDocument> getResults(String query, int maxResults) throws PresentationException, IndexUnreachableException {

        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }

        String metadataQuery = "+(%s) +DOCTYPE:METADATA".formatted(query);
        String filterQuery = SearchHelper.buildFinalQuery("*:*", false, SearchAggregationType.NO_AGGREGATION);
        String topDocQuery = SearchHelper.AGGREGATION_QUERY_PREFIX + metadataQuery;

        Map<String, String> paramMap = SearchHelper.getExpandQueryParams(metadataQuery, MAX_METATA_PER_RECORD);
        paramMap.put("fq", filterQuery);

        return search(maxResults, topDocQuery, paramMap);

    }

    protected MetadataDocument getMetadataDocument(QueryResponse response, SolrDocument topDocument) {
        String pi = topDocument.getFirstValue(SolrConstants.PI_TOPSTRUCT).toString();

        List<SolrDocument> metadataDocs = Collections.emptyList();
        if (response.getExpandedResults() != null && response.getExpandedResults().get(pi) != null) {
            metadataDocs = response.getExpandedResults()
                    .get(pi)
                    .stream()
                    .toList();
        }
        return MetadataDocument.fromSolrDocs(topDocument, Collections.emptyList(), metadataDocs);
    }

}
