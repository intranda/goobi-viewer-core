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

public class FeatureQueryGenerator {

    private final SolrSearchIndex searchIndex;

    public FeatureQueryGenerator(SolrSearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    public List<MetadataDocument> getResults(String query, int maxResults) throws PresentationException, IndexUnreachableException {

        String finalQuery = SearchHelper.buildFinalQuery(query, false, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);

        Map<String, String> paramMap = SearchHelper.getExpandQueryParams(query);

        QueryResponse response =
                searchIndex
                        .search(finalQuery, 0, maxResults, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                Collections.emptyList(), paramMap);
        var expanded = response.getExpandedResults();
        int expandedKeys = expanded.keySet().size();
        long expandedResults = expanded.values().stream().flatMap(solrDocs -> solrDocs.stream()).count();
        return response.getResults().stream().map(doc -> FeatureQueryGenerator.getMetadataDocument(response, doc)).toList();

    }

    private static MetadataDocument getMetadataDocument(QueryResponse response, SolrDocument mainDoc) {
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
