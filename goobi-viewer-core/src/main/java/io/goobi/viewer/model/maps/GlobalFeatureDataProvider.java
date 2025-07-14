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
package io.goobi.viewer.model.maps;

import java.util.ArrayList;
import java.util.List;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.maps.features.DocStructDataProvider;
import io.goobi.viewer.model.maps.features.IFeatureDataProvider;
import io.goobi.viewer.model.maps.features.MetadataDataProvider;
import io.goobi.viewer.model.maps.features.MetadataDocument;
import io.goobi.viewer.model.maps.features.RecordDataProvider;
import io.goobi.viewer.solr.SolrSearchIndex;

public class GlobalFeatureDataProvider implements IFeatureDataProvider {

    private final DocStructDataProvider docStructProvider;
    private final RecordDataProvider recordDataProvider;
    private final MetadataDataProvider metadataDataProvider;

    public GlobalFeatureDataProvider(SolrSearchIndex searchIndex, List<String> requiredFields) {
        this.docStructProvider = new DocStructDataProvider(searchIndex, requiredFields);
        this.recordDataProvider = new RecordDataProvider(searchIndex, requiredFields, false);
        this.metadataDataProvider = new MetadataDataProvider(searchIndex, requiredFields);
    }

    @Override
    public List<MetadataDocument> getResults(String query, int maxResults) throws PresentationException, IndexUnreachableException {
        List<MetadataDocument> recordResults = this.recordDataProvider.getResults(query, maxResults);
        List<MetadataDocument> docStructResults = this.docStructProvider.getResults(query, maxResults);
        List<MetadataDocument> metadataResults = this.metadataDataProvider.getResults(query, maxResults);
        List<MetadataDocument> results = new ArrayList<>();
        results.addAll(recordResults);
        results.addAll(docStructResults);
        results.addAll(metadataResults);
        return results;
    }

}
