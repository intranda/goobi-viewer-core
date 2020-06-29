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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.model.ner.DocumentReference;
import io.goobi.viewer.api.rest.model.ner.ElementReference;
import io.goobi.viewer.api.rest.model.ner.MultiPageReference;
import io.goobi.viewer.api.rest.model.ner.NERTag;
import io.goobi.viewer.api.rest.model.ner.PageReference;
import io.goobi.viewer.api.rest.model.ner.TagCount;
import io.goobi.viewer.api.rest.model.ner.TagGroup;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;

/**
 * @author florian
 *
 */
public class NERBuilder {

    private static final Logger logger = LoggerFactory.getLogger(NERBuilder.class);
    
    private final AbstractApiUrlManager urls;
    
    public NERBuilder() {
        this.urls = new ApiUrls();
    }
    
    public NERBuilder(AbstractApiUrlManager urls) {
        this.urls = urls;
    }
    
    public DocumentReference getNERTags(String pi, String type, Integer start, Integer end, int rangeSize)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        StringBuilder query = new StringBuilder();
        query.append(SolrConstants.PI_TOPSTRUCT).append(':').append(pi);

        if (start != null && end != null) {
            query.append(" AND ").append(SolrConstants.ORDER).append(":[").append(start).append(" TO ").append(end).append("]");
        } else if (start != null) {
            query.append(" AND ").append(SolrConstants.ORDER).append(":[").append(start).append(" TO *]");
        } else if (end != null) {
            query.append(" AND ").append(SolrConstants.ORDER).append(":[* TO ").append(end).append("]");
        } else {
            query.append(" AND ").append(SolrConstants.DOCTYPE).append(":PAGE");
        }

        return getNERTagsByQuery(query.toString(), type, rangeSize);
    }
    
    /**
     * 
     * @param query must return a set of PAGE documents within a single topStruct
     * @param typeString
     * @param rangeSize
     * @return
     * @throws PresentationException if there is an error parsing the alto documents or if the search doesn't result in PAGE documents from a single
     *             topStruct
     * @throws IndexUnreachableException if the index cannot be reached
     * @throws ViewerConfigurationException
     */
    private DocumentReference getNERTagsByQuery(String query, String typeString, int rangeSize)
            throws PresentationException, IndexUnreachableException, ViewerConfigurationException {
        final List<String> fieldList = Arrays.asList(new String[] { SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.IDDOC,
                SolrConstants.IDDOC_TOPSTRUCT, SolrConstants.ORDER, SolrConstants.FILENAME_ALTO });
        SolrDocumentList solrDocuments = DataManager.getInstance().getSearchIndex().search(query, fieldList);
        Collections.sort(solrDocuments, docOrderComparator);

        NERTag.Type type = NERTag.Type.getType(typeString);

        if (solrDocuments != null && !solrDocuments.isEmpty()) {
            String topStructPi = null;
            if (solrDocuments.get(0).containsKey(SolrConstants.PI_TOPSTRUCT)) {
                topStructPi = SolrSearchIndex.getAsString(solrDocuments.get(0).getFieldValue(SolrConstants.PI_TOPSTRUCT));
            }
            DocumentReference doc = new DocumentReference(topStructPi);

            for (int index = 0; index < solrDocuments.size(); index += rangeSize) {
                List<SolrDocument> rangeList = solrDocuments.subList(index, Math.min(index + rangeSize, solrDocuments.size()));
                TagGroup range = createPageReference(rangeList);
                for (SolrDocument solrDoc : rangeList) {
                    String altoFileName = (String) solrDoc.getFieldValue(SolrConstants.FILENAME_ALTO);
                    if (altoFileName == null) {
                        logger.error("{}, page {} has no {} value.", topStructPi, solrDoc.getFieldValue(SolrConstants.ORDER),
                                SolrConstants.FILENAME_ALTO);
                        continue;
                    }
                    //TODO: Load directly from file if on same server?
                    // Load ALTO via the REST service
                    String url = DataFileTools.buildFullTextUrl(altoFileName);
//                    String filename = Paths.get(altoFileName).getFileName().toString();
//                    String url = urls.path(ApiUrls.RECORDS_FILES, ApiUrls.RECORDS_FILES_ALTO).params(topStructPi, filename).build();
                    try {
                        String altoString = NetTools.getWebContentGET(url);
                        Integer pageOrder = getPageOrder(solrDoc);
                        List<TagCount> tags = ALTOTools.getNERTags(altoString, type);
                        for (TagCount tagCount : tags) {
                            for (ElementReference reference : tagCount.getReferences()) {
                                reference.setPage(pageOrder);
                            }
                        }
                        range.addTags(tags);
                    } catch (FileNotFoundException e) {
                        logger.error(e.getMessage());
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    } catch (HTTPException e) {
                        logger.error(e.getMessage());
                    }
                }
                Collections.sort(range.getTags());
                Collections.reverse(range.getTags());
                doc.addPageRange(range);
            }
            return doc;
        }
        return new DocumentReference();
    }
    
    private Integer getPageOrder(SolrDocument solrDoc) {
        Integer order = (Integer) solrDoc.getFieldValue(SolrConstants.ORDER);
        return order;
    }

    private TagGroup createPageReference(List<SolrDocument> solrDocs) {
        Integer firstPage = null;
        Integer lastPage = null;
        for (SolrDocument solrDocument : solrDocs) {
            Integer order = getPageOrderFromSolrDoc(solrDocument);
            if (firstPage == null || (order != null && order < firstPage)) {
                firstPage = order;
            }
            if (lastPage == null || (order != null && order > lastPage)) {
                lastPage = order;
            }
        }
        TagGroup group;
        if (firstPage == lastPage) {
            group = new PageReference(firstPage);
        } else {
            group = new MultiPageReference(firstPage, lastPage);
        }
        return group;
    }

    /**
     * @param solrDocument
     * @return
     */
    private Integer getPageOrderFromSolrDoc(SolrDocument solrDoc) {
        if (solrDoc != null && solrDoc.containsKey(SolrConstants.ORDER)) {
            String orderString = SolrSearchIndex.getAsString(solrDoc.getFieldValue(SolrConstants.ORDER));
            try {
                //                Integer.parseInt(orderString);
                return Integer.parseInt(orderString);
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    private Comparator<SolrDocument> docOrderComparator = new Comparator<SolrDocument>() {

        @Override
        public int compare(SolrDocument doc1, SolrDocument doc2) {
            Integer order1 = getPageOrderFromSolrDoc(doc1);
            Integer order2 = getPageOrderFromSolrDoc(doc2);
            if (order1 != null && order2 != null) {
                return order1.compareTo(order2);
            } else if (order1 != null) {
                return 1;
            } else if (order2 != null) {
                return -1;
            } else {
                return 1;
            }
        }
    };
    
}
