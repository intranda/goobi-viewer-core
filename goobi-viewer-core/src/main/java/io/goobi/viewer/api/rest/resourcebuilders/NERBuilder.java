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
package io.goobi.viewer.api.rest.resourcebuilders;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.api.rest.model.ner.DocumentReference;
import io.goobi.viewer.api.rest.model.ner.ElementReference;
import io.goobi.viewer.api.rest.model.ner.MultiPageReference;
import io.goobi.viewer.api.rest.model.ner.NERTag;
import io.goobi.viewer.api.rest.model.ner.PageReference;
import io.goobi.viewer.api.rest.model.ner.TagCount;
import io.goobi.viewer.api.rest.model.ner.TagGroup;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author florian
 *
 */
public class NERBuilder {

    private static final Logger logger = LogManager.getLogger(NERBuilder.class);

    public DocumentReference getNERTags(String pi, String type, Integer start, Integer end, int rangeSize, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException {
        StringBuilder query = new StringBuilder();
        query.append(SolrConstants.PI_TOPSTRUCT).append(':').append(pi);

        if (start != null && end != null) {
            query.append(SolrConstants.SOLR_QUERY_AND).append(SolrConstants.ORDER).append(":[").append(start).append(" TO ").append(end).append("]");
        } else if (start != null) {
            query.append(SolrConstants.SOLR_QUERY_AND).append(SolrConstants.ORDER).append(":[").append(start).append(" TO *]");
        } else if (end != null) {
            query.append(SolrConstants.SOLR_QUERY_AND).append(SolrConstants.ORDER).append(":[* TO ").append(end).append("]");
        } else {
            query.append(SolrConstants.SOLR_QUERY_AND).append(SolrConstants.DOCTYPE).append(":PAGE");
        }

        return getNERTagsByQuery(request, query.toString(), type, rangeSize);
    }

    /**
     *
     * @param request
     * @param query must return a set of PAGE documents within a single topStruct
     * @param typeString
     * @param rangeSize
     * @return {@link DocumentReference}
     * @throws PresentationException if there is an error parsing the alto documents or if the search doesn't result in PAGE documents from a single
     *             topStruct
     * @throws IndexUnreachableException if the index cannot be reached
     * @throws ViewerConfigurationException
     */
    private DocumentReference getNERTagsByQuery(HttpServletRequest request, String query, String typeString, int rangeSize)
            throws PresentationException, IndexUnreachableException {
        final List<String> fieldList = Arrays.asList(SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.IDDOC,
                SolrConstants.IDDOC_TOPSTRUCT, SolrConstants.ORDER, SolrConstants.FILENAME_ALTO);
        SolrDocumentList solrDocuments = DataManager.getInstance().getSearchIndex().search(query, fieldList);
        Collections.sort(solrDocuments, docOrderComparator);

        if (solrDocuments != null && !solrDocuments.isEmpty()) {
            String topStructPi = null;
            if (solrDocuments.get(0).containsKey(SolrConstants.PI_TOPSTRUCT)) {
                topStructPi = SolrTools.getAsString(solrDocuments.get(0).getFieldValue(SolrConstants.PI_TOPSTRUCT));
            }
            DocumentReference doc = new DocumentReference(topStructPi);
            NERTag.Type type = NERTag.Type.getByLabel(typeString);
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

                    try {
                        if (!altoFileName.contains("/")) {
                            altoFileName = topStructPi + "/" + altoFileName;
                        }
                        if (AccessConditionUtils.checkAccess(request, "text", topStructPi, altoFileName, false).isGranted()) {

                            String altoString = "";
                            String charset = null;
                            try {
                                StringPair alto = DataFileTools.loadAlto(altoFileName);
                                altoString = alto.getOne();
                                charset = alto.getTwo();
                            } catch (FileNotFoundException e) {
                                continue;
                            }
                            Integer pageOrder = getPageOrder(solrDoc);
                            List<TagCount> tags = ALTOTools.getNERTags(altoString, charset, type);
                            for (TagCount tagCount : tags) {
                                for (ElementReference reference : tagCount.getReferences()) {
                                    reference.setPage(pageOrder);
                                }
                            }
                            // TODO add URI to reference
                            range.addTags(tags);
                        }
                    } catch (DAOException e) {
                        logger.error(e.toString());
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

    private static Integer getPageOrder(SolrDocument solrDoc) {
        return (Integer) solrDoc.getFieldValue(SolrConstants.ORDER);
    }

    private static TagGroup createPageReference(List<SolrDocument> solrDocs) {
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
        if (firstPage != null && firstPage.equals(lastPage)) {
            group = new PageReference(firstPage);
        } else {
            group = new MultiPageReference(firstPage, lastPage);
        }
        return group;
    }

    /**
     * @param solrDoc
     * @return Page order from given solrDoc
     */
    private static Integer getPageOrderFromSolrDoc(SolrDocument solrDoc) {
        if (solrDoc != null && solrDoc.containsKey(SolrConstants.ORDER)) {
            String orderString = SolrTools.getAsString(solrDoc.getFieldValue(SolrConstants.ORDER));
            try {
                return Integer.parseInt(orderString);
            } catch (NumberFormatException e) {
                //
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
