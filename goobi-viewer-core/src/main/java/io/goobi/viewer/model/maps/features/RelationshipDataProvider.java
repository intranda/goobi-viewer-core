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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.ComplexMetadataContainer;
import io.goobi.viewer.model.metadata.RelationshipMetadataContainer;
import io.goobi.viewer.solr.SolrSearchIndex;

public class RelationshipDataProvider extends MetadataDataProvider {

    private static final Logger logger = LogManager.getLogger(RelationshipDataProvider.class);

    public RelationshipDataProvider(SolrSearchIndex searchIndex, List<String> requiredFields) {
        super(searchIndex, requiredFields);
    }

    protected MetadataDocument getMetadataDocument(QueryResponse response, SolrDocument topDocument) {
        MetadataDocument doc = super.getMetadataDocument(response, topDocument);
        ComplexMetadataContainer mdGroups = doc.getMetadataGroups();
        try {
            RelationshipMetadataContainer relations =
                    RelationshipMetadataContainer.loadRelationships(mdGroups, getFieldList(), this.getSearchIndex());
            return new MetadataDocument(doc.getPi(), doc.getIddoc(), doc.getMainDocMetadata(), relations, doc.getChildDocuments());
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Error loading related documents for {}. Reason: {}", doc.getPi(), e.toString());
            return doc;
        }
    }

}
