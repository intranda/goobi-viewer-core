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
package io.goobi.viewer.model.iiif.search;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.iiif.search.IIIFSearchBuilder.AnnotationResultList;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * @author florian
 *
 */
public class IIIFAutocompleteBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(IIIFAutocompleteBuilder.class);

    private String query;
    private String motivation;
    
    
    private Map<Integer, Integer> countFulltextOccurances(String query, String pi)
            throws PresentationException, IndexUnreachableException {

        //replace search wildcards with word character regex and replace whitespaces with '|' to facilitate OR search
        String queryRegex = getQueryRegex(query);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:PAGE");
        queryBuilder.append(" +FULLTEXTAVAILABLE:true");
        queryBuilder.append(" +FULLTEXT:").append(query);

        AnnotationResultList results = new AnnotationResultList();

        StringPair sortField = new StringPair(SolrConstants.ORDER, "asc");
        //        QueryResponse response = DataManager.getInstance().getSearchIndex().search(queryBuilder.toString(), (page-1)*getHitsPerPage(), getHitsPerPage(), Collections.singletonList(sortField), null, FULLTEXTFIELDLIST);
        SolrDocumentList docList = DataManager.getInstance()
                .getSearchIndex()
                .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, Collections.singletonList(sortField), FULLTEXTFIELDLIST);
        for (SolrDocument doc : docList) {
            Path altoFile = getPath(pi, SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.FILENAME_ALTO));
            Path fulltextFile = getPath(pi, SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.FILENAME_FULLTEXT));
            Integer pageNo = SolrSearchIndex.getAsInt(doc.getFieldValue(SolrConstants.ORDER));
            if (altoFile != null && Files.exists(altoFile)) {
                results.add(getAnnotationsFromAlto(altoFile, pi, pageNo, queryRegex, results.numHits, firstIndex, numHits));
            } else if (fulltextFile != null && Files.exists(fulltextFile)) {
                results.add(getAnnotationsFromFulltext(fulltextFile, "utf-8", pi, pageNo, queryRegex, results.numHits, firstIndex, numHits));
            }
        }
        return results;
    }
}
