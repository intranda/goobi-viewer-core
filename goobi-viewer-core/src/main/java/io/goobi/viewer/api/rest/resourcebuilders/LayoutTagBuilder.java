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
import io.goobi.viewer.api.rest.model.ner.PageReference;
import io.goobi.viewer.api.rest.model.ner.TagCount;
import io.goobi.viewer.api.rest.model.ner.TagGroup;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds a per-record listing of ALTO {@code <LayoutTag>} labels, grouped into page ranges — the layout-tag analogue
 * of {@link NERBuilder}. Reuses the NER REST model ({@link DocumentReference} / {@link TagGroup} / {@link TagCount} /
 * {@link ElementReference}); layout tags carry a placeholder {@code MISC} type and no authority URI (see
 * {@link ALTOTools#getLayoutTags(String, String, String)}). The per-page ALTO is loaded and parsed on demand (no
 * occurrence data is held in Solr), exactly like the NER listing.
 */
public class LayoutTagBuilder {

    private static final Logger logger = LogManager.getLogger(LayoutTagBuilder.class);

    /**
     * @param pi record identifier (topstruct PI)
     * @param label if non-null, only this exact layout-tag LABEL is listed; null lists all layout tags
     * @param start first page order (inclusive), or null
     * @param end last page order (inclusive), or null
     * @param rangeSize number of pages per result group
     * @param request HTTP servlet request (for per-page access control)
     * @return {@link DocumentReference}
     * @throws PresentationException if the ALTO documents cannot be parsed or the query is not a single topstruct
     * @throws IndexUnreachableException if the index cannot be reached
     */
    public DocumentReference getLayoutTags(String pi, String label, Integer start, Integer end, int rangeSize, HttpServletRequest request)
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

        return getLayoutTagsByQuery(request, query.toString(), label, rangeSize);
    }

    private DocumentReference getLayoutTagsByQuery(HttpServletRequest request, String query, String label, int rangeSize)
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
            for (int index = 0; index < solrDocuments.size(); index += rangeSize) {
                List<SolrDocument> rangeList = solrDocuments.subList(index, Math.min(index + rangeSize, solrDocuments.size()));
                TagGroup range = createPageReference(rangeList);
                for (SolrDocument solrDoc : rangeList) {
                    String altoFileName = (String) solrDoc.getFieldValue(SolrConstants.FILENAME_ALTO);
                    if (altoFileName == null) {
                        logger.warn("{}, page {} ({}={}) has no {} value.", topStructPi, solrDoc.getFieldValue(SolrConstants.ORDER),
                                SolrConstants.FILENAME, solrDoc.getFieldValue(SolrConstants.FILENAME), SolrConstants.FILENAME_ALTO);
                        continue;
                    }

                    try {
                        if (!altoFileName.contains("/")) {
                            altoFileName = topStructPi + "/" + altoFileName;
                        }
                        if (AccessConditionUtils.checkAccess(request.getSession(), "text", topStructPi, altoFileName,
                                NetTools.getIpAddress(request), false).isGranted()) {

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
                            List<TagCount> tags = ALTOTools.getLayoutTags(altoString, charset, label);
                            for (TagCount tagCount : tags) {
                                for (ElementReference reference : tagCount.getReferences()) {
                                    reference.setPage(pageOrder);
                                }
                            }
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
