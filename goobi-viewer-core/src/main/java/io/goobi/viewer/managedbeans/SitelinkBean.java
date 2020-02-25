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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * SitelinkBean
 */
@Named
@SessionScoped
public class SitelinkBean implements Serializable {

    private static final long serialVersionUID = -3131868167344465016L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(SitelinkBean.class);

    private String value;
    //    private List<SearchHit> hits;
    private List<StringPair> hits;

    /**
     * <p>
     * getAvailableValues.
     * </p>
     *
     * @return List of facet values for the configured field and query
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getAvailableValues() throws PresentationException, IndexUnreachableException {
        return getAvailableValuesForField(DataManager.getInstance().getConfiguration().getSitelinksField(),
                DataManager.getInstance().getConfiguration().getSitelinksFilterQuery());
    }

    /**
     * <p>
     * getAvailableValuesForField.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @param filterQuery a {@link java.lang.String} object.
     * @return List of facet values for the given field and query
     * @should return all existing values for the given field
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<String> getAvailableValuesForField(String field, String filterQuery) throws PresentationException, IndexUnreachableException {
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (filterQuery == null) {
            throw new IllegalArgumentException("filterQuery may not be null");
        }

        filterQuery = SearchHelper.buildFinalQuery(filterQuery, false);
        QueryResponse qr =
                DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(filterQuery, Collections.singletonList(field), 1, false);
        if (qr != null) {
            FacetField facet = qr.getFacetField(field);
            if (facet != null) {
                List<String> ret = new ArrayList<>(facet.getValueCount());
                for (Count count : facet.getValues()) {
                    // Skip values starting with u0001
                    if (count.getName().charAt(0) != 0x01) {
                        ret.add(count.getName());
                    }
                }
                return ret;
            }
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * searchAction.
     * </p>
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
        String[] anchorFields = { SolrConstants.LABEL, SolrConstants.TITLE };
        String query = SearchHelper.buildFinalQuery(field + ":" + value + (filterQuery != null ? " AND " + filterQuery : ""), false);
        logger.trace("q: {}", query);
        //        hits = SearchHelper.searchWithAggregation(query, 0, SolrSearchIndex.MAX_HITS, null, null, null, null, null, null, null);
        //        hits = SearchHelper.searchWithFulltext(query, 9, SolrSearchIndex.MAX_HITS, null, null, null, null, null, null, null,
        //                (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());

        SolrDocumentList docList = DataManager.getInstance().getSearchIndex().search(query, Arrays.asList(fields));
        if (docList != null && !docList.isEmpty()) {
            hits = new ArrayList<>(docList.size());
            Map<String, String> anchorTitle = new HashMap<>();
            for (SolrDocument doc : docList) {
                StringBuilder sbLabel = new StringBuilder();
                String anchorPi = (String) doc.getFieldValue(SolrConstants.PI_PARENT);
                if (anchorPi != null) {
                    SolrDocumentList anchorDocList =
                            DataManager.getInstance().getSearchIndex().search(SolrConstants.PI + ":" + anchorPi, Arrays.asList(anchorFields));
                    if (!anchorDocList.isEmpty()) {
                        SolrDocument anchorDoc = anchorDocList.get(0);
                        sbLabel.append(anchorDoc.getFieldValue(SolrConstants.LABEL));
                    }
                }

                String pi = (String) doc.getFieldValue(SolrConstants.PI);
                String label = (String) doc.getFieldValue(SolrConstants.LABEL);
                if (label == null) {
                    label = (String) doc.getFieldValue(SolrConstants.CURRENTNO);
                    if (label == null) {
                        label = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.TITLE);

                        if (label == null) {
                            label = pi;
                        }
                    }
                }
                if (sbLabel.length() > 0) {
                    sbLabel.append(": ");
                }
                sbLabel.append(label);
                String docStructType = (String) doc.getFieldValue(SolrConstants.DOCSTRCT);
                String mimeType = (String) doc.getFieldValue(SolrConstants.MIMETYPE);
                boolean hasImages = "image".equals(mimeType);
                //                PageType pageType = PageType.determinePageType(docStructType, null, false, hasImages, false, false);
                PageType pageType = PageType.viewMetadata;
                hits.add(new StringPair(sbLabel.toString(), pageType.getName() + "/" + pi + "/1/"));
            }
        }

        // logger.trace("done");
        return "";
    }

    /**
     * <p>
     * resetAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String resetAction() {
        value = null;
        hits = null;

        return "";
    }

    /**
     * <p>
     * Getter for the field <code>value</code>.
     * </p>
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * <p>
     * Setter for the field <code>value</code>.
     * </p>
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * <p>
     * Getter for the field <code>hits</code>.
     * </p>
     *
     * @return the hits
     */
    public List<StringPair> getHits() {
        return hits;
    }
}
