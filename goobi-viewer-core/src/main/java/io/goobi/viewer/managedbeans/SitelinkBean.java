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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchAggregationType;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * SitelinkBean.
 */
@Named
@SessionScoped
public class SitelinkBean implements Serializable {

    private static final long serialVersionUID = -3131868167344465016L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(SitelinkBean.class);

    private String value;
    // volatile + single-write publication: SitelinkBean is @SessionScoped, so concurrent requests
    // within one session (e.g. a crawler fanning out /sitelinks/{value}/ URLs over a single cookie)
    // share this instance. Building the list locally in searchAction() and assigning the field once
    // via a volatile write ensures readers (sitelinks.xhtml's <c:forEach items="#{sitelinkBean.hits}">)
    // never observe a list that another thread is still appending to — which previously caused
    // ConcurrentModificationException from Mojarra's ForEachHandler during RENDER_RESPONSE.
    private volatile List<StringPair> hits;

    /**
     * getAvailableValues.
     *
     * @return List of facet values for the configured field and query
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getAvailableValues() throws PresentationException, IndexUnreachableException {
        return SolrTools.getAvailableValuesForField(DataManager.getInstance().getConfiguration().getSitelinksField(),
                DataManager.getInstance().getConfiguration().getSitelinksFilterQuery());
    }

    /**
     * searchAction.
     *
     * @return Target page
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String searchAction() throws IndexUnreachableException, PresentationException, DAOException {
        String field = DataManager.getInstance().getConfiguration().getSitelinksField();
        String filterQuery = DataManager.getInstance().getConfiguration().getSitelinksFilterQuery();

        if (value == null) {
            hits = null;
            return "";
        }

        String[] fields = { SolrConstants.PI, SolrConstants.PI_PARENT, SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.DOCSTRCT,
                SolrConstants.MIMETYPE, SolrConstants.CURRENTNO };
        // PI is included so buildAnchorDocMap can key the returned documents by identifier
        String[] anchorFields = { SolrConstants.PI, SolrConstants.LABEL, SolrConstants.TITLE };
        String query = SearchHelper.buildFinalQuery(field + ":\"" + value + '"' + (filterQuery != null ? " AND " + filterQuery : ""), false,
                SearchAggregationType.NO_AGGREGATION);
        logger.trace("q: {}", query);

        SolrDocumentList docList = DataManager.getInstance().getSearchIndex().search(query, Arrays.asList(fields));
        if (docList != null && !docList.isEmpty()) {
            // Build into a local list and publish once at the end. Do NOT assign `hits = new ArrayList<>(...)`
            // up front and then mutate it in the loop — concurrent renders in the same session would then
            // iterate a half-built list and throw ConcurrentModificationException (see field comment above).
            List<StringPair> localHits = new ArrayList<>(docList.size());
            // Batch-fetch all anchor labels in a single query instead of one query per result.
            // Previously, each result with a PI_PARENT triggered an individual Solr lookup,
            // causing N+1 queries for year views with many volumes (e.g. newspapers).
            Map<String, SolrDocument> anchorDocsByPi = buildAnchorDocMap(docList, anchorFields);
            for (SolrDocument doc : docList) {
                StringBuilder sbLabel = new StringBuilder();
                String anchorPi = (String) doc.getFieldValue(SolrConstants.PI_PARENT);
                if (anchorPi != null) {
                    SolrDocument anchorDoc = anchorDocsByPi.get(anchorPi);
                    if (anchorDoc != null) {
                        sbLabel.append(anchorDoc.getFieldValue(SolrConstants.LABEL));
                    }
                }

                String pi = (String) doc.getFieldValue(SolrConstants.PI);
                String label = (String) doc.getFieldValue(SolrConstants.LABEL);
                if (label == null) {
                    label = (String) doc.getFieldValue(SolrConstants.CURRENTNO);
                    if (label == null) {
                        label = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);

                        if (label == null) {
                            label = pi;
                        }
                    }
                }
                if (sbLabel.length() > 0) {
                    sbLabel.append(": ");
                }
                sbLabel.append(label);
                //                PageType pageType = PageType.determinePageType(docStructType, null, false, hasImages, false, false);
                PageType pageType = PageType.viewMetadata;
                localHits.add(new StringPair(sbLabel.toString(), pageType.getName() + "/" + pi + "/1/"));
            }
            // Single volatile write — once published, this list is never mutated again, so any
            // <c:forEach> iteration started before or after this point is safe.
            hits = localHits;
        }

        // logger.trace("done"); //NOSONAR Debug
        return "";
    }

    /**
     * resetAction.
     *
     * @return an empty string after resetting the current sitelink value and hits
     */
    public String resetAction() {
        value = null;
        hits = null;

        return "";
    }

    /**
     * Getter for the field <code>value</code>.
     *
     * @return the field value used to filter or look up sitelinks
     */
    public String getValue() {
        return value;
    }

    /**
     * Setter for the field <code>value</code>.
     *
     * @param value the field value used to filter or look up sitelinks
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Getter for the field <code>hits</code>.
     *
     * @return list of sitelink result pairs (label and URL)
     */
    public List<StringPair> getHits() {
        return hits;
    }

    /**
     * Collects all unique PI_PARENT values from {@code docList} and fetches the corresponding
     * anchor documents in a single batched Solr query ({@code PI:(val1 OR val2 OR ...)}).
     * <p>
     * This replaces the previous N+1 pattern where each result with a PI_PARENT triggered
     * an individual Solr lookup. For a year view with 300 newspaper volumes, this reduces
     * the anchor-label lookups from 300 queries to one.
     * </p>
     *
     * @param docList result documents from the main sitelinks query
     * @param anchorFields Solr fields to retrieve for each anchor document
     * @return map of PI → SolrDocument for all found anchors; empty map if none exist
     * @throws PresentationException if Solr returns an error response
     * @throws IndexUnreachableException if Solr cannot be reached
     */
    private Map<String, SolrDocument> buildAnchorDocMap(SolrDocumentList docList, String[] anchorFields)
            throws PresentationException, IndexUnreachableException {
        Set<String> anchorPis = new LinkedHashSet<>();
        for (SolrDocument doc : docList) {
            String anchorPi = (String) doc.getFieldValue(SolrConstants.PI_PARENT);
            if (anchorPi != null) {
                anchorPis.add(anchorPi);
            }
        }
        if (anchorPis.isEmpty()) {
            return Collections.emptyMap();
        }

        // PI:(val1 OR val2 OR ...) — a single round-trip for all anchors regardless of count
        String batchQuery = SolrConstants.PI + ":(" + String.join(" OR ", anchorPis) + ")";
        SolrDocumentList anchorDocList = DataManager.getInstance().getSearchIndex()
                .search(batchQuery, Arrays.asList(anchorFields));
        if (anchorDocList == null || anchorDocList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, SolrDocument> map = new HashMap<>(anchorDocList.size());
        for (SolrDocument anchorDoc : anchorDocList) {
            String pi = (String) anchorDoc.getFieldValue(SolrConstants.PI);
            if (pi != null) {
                map.put(pi, anchorDoc);
            }
        }
        return map;
    }
}
