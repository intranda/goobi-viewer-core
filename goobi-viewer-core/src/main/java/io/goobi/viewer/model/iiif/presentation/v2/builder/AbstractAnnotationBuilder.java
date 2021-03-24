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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StringPair;

/**
 * @author florian
 *
 */
public class AbstractAnnotationBuilder {
    
    private final static Logger logger  = LoggerFactory.getLogger(AbstractAnnotationBuilder.class);

    public static final String[] UGC_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI_TOPSTRUCT, SolrConstants.ORDER, SolrConstants.UGCTYPE,
            SolrConstants.MD_TEXT, SolrConstants.UGCCOORDS, SolrConstants.MD_BODY, SolrConstants.UGCTERMS, SolrConstants.ACCESSCONDITION };

    
    private final AbstractBuilder restBuilder;
    
    public AbstractAnnotationBuilder(AbstractApiUrlManager urls) {
        this.restBuilder = new AbstractBuilder(urls) {
        };
    }
    
    public String getAnnotationQuery() {
        String query = "+" + SolrConstants.DOCTYPE + ":" + DocType.UGC.name();
        return query;
    }
    
    public String getAnnotationQuery(String pi) {
        String query = "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " " + getAnnotationQuery();
        return query;
    }

    public String getAnnotationQuery(String pi, int pageNo) {
        String query = "+" + SolrConstants.ORDER + ":" + pageNo + " " + getAnnotationQuery(pi);
        return query;
    }
    
    public String getAnnotationQuery(long iddoc) {
        return "+" + SolrConstants.IDDOC + ":" + iddoc;
    }

    
    public List<SolrDocument> getAnnotationDocuments(String query, HttpServletRequest request) throws PresentationException, IndexUnreachableException {
        return getAnnotationDocuments(query, getDefaultSortFields(), request);
    }
    
    /**
     * Return sort fields to sort results ascending after topstruct PI, then page order, then IDDOC;
     * 
     * @return sort fields for PI_TOPSTRUCT, ORDER and IDDOC
     */
    private List<StringPair> getDefaultSortFields() {
        StringPair sortField1 = new StringPair(SolrConstants.PI_TOPSTRUCT, "asc");
        StringPair sortField2 = new StringPair(SolrConstants.ORDER, "asc");
        StringPair sortField3 = new StringPair(SolrConstants.IDDOC, "asc");
        return Arrays.asList(sortField1, sortField2, sortField3);
    }

    public List<SolrDocument> getAnnotationDocuments(String query, List<StringPair> sortFields,  HttpServletRequest request) throws PresentationException, IndexUnreachableException {
        return getAnnotationDocuments(query, 0, SolrSearchIndex.MAX_HITS, sortFields, request);
    }
        
    public List<SolrDocument> getAnnotationDocuments(String query, int first, int rows, List<StringPair> sortFields, HttpServletRequest request) throws PresentationException, IndexUnreachableException {
        if(sortFields ==  null) {
            sortFields = getDefaultSortFields();
        }
        SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(query, first, rows, sortFields, null, Arrays.asList(UGC_SOLR_FIELDS)).getResults();
        if (hits.isEmpty()) {
            return Collections.emptyList();
        } else {
            return hits.stream().filter(hit -> isAccessGranted(hit, query, request)).collect(Collectors.toList());
        }
    }
    
    public Optional<SolrDocument> getAnnotationDocument(long id, HttpServletRequest request) throws PresentationException, IndexUnreachableException {
        SolrDocument hit = DataManager.getInstance().getSearchIndex().getFirstDoc(getAnnotationQuery(id), Arrays.asList(UGC_SOLR_FIELDS));
        return Optional.ofNullable(hit);
    }

    
    private boolean isAccessGranted(SolrDocument doc, String query, HttpServletRequest request) {
        
        String accessCondition = SolrSearchIndex.getSingleFieldStringValue(doc, SolrConstants.ACCESSCONDITION);
        if(StringUtils.isBlank(accessCondition) || "OPENACCESS".equalsIgnoreCase(accessCondition)) {
            return true;
        } else if(request != null){
            try {
                return AccessConditionUtils.checkAccessPermission(Collections.singleton(accessCondition),
                        IPrivilegeHolder.PRIV_VIEW_UGC, query, request);
            } catch (IndexUnreachableException | PresentationException | DAOException e) {
                logger.error("Failed to check access to annotation", e);
                return false;
            }
        } else {
            return false;
        }
    }
    
    public long getAnnotationCount(HttpServletRequest request) throws PresentationException, IndexUnreachableException {
        SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(getAnnotationQuery(), SolrSearchIndex.MAX_HITS, null, Arrays.asList(SolrConstants.ACCESSCONDITION));
        if (hits.isEmpty()) {
            return 0;
        } else {
            return hits.stream().filter(hit -> isAccessGranted(hit, getAnnotationQuery(), request)).count();
        }
    }
    
    protected AbstractBuilder getRestBuilder() {
        return restBuilder;
    }
}
