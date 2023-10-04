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
package io.goobi.viewer.model.search;

import java.beans.Transient;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Sample query : "{!join from=MD_IDENTIFIER to=MD_PROCESSID} +LABEL:MD_RELATIONSHIP_EVENT +PI_TOPSTRUCT:848f183d-ea46-48d7-a8f1-b9fab4da5c02"
 */
@Named
@ViewScoped
public class RelatedDocumentSearch implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String DOCUMENT_IDENTIFIER = "MD_PROCESSID";
    private static final String RELATIONSHIP_ID_REFERENCE = "MD_IDENTIFIER";
    private static final String SEARCH_RELATIONSHIP_FORMAT = "+LABEL:%s +PI_TOPSTRUCT:%s";
    private static final String SEARCH_STRING_FORMAT = "+" + DOCUMENT_IDENTIFIER + ":(%s)";
    private static final String SEARCH_STRING_IDENTIFIER_SEPARATOR = " ";

    private final transient SolrSearchIndex searchIndex;
    private final SearchBean searchBean;
    
    public RelatedDocumentSearch(SolrSearchIndex searchIndex, SearchBean searchBean) {
        this.searchIndex = searchIndex;
        this.searchBean = searchBean;
    }
    
    public RelatedDocumentSearch() {
        this(DataManager.getInstance().getSearchIndex(), BeanUtils.getSearchBean());
    }

    public String search(String pi, String relationshipField) throws PresentationException, IndexUnreachableException {
        List<String> ids = getRelatedProcessIdentifier(pi, relationshipField);
        String searchString = String.format(SEARCH_STRING_FORMAT, ids.stream().collect(Collectors.joining(SEARCH_STRING_IDENTIFIER_SEPARATOR)));
        return PrettyUrlTools.getAbsolutePageUrl("newSearch5", "-", searchString, 1, searchBean.getSortString(), "-");
        
//        searchBean.setExactSearchString(searchString);
//        return "pretty:newSearch5";
    }
    
    private List<String> getRelatedProcessIdentifier(String pi, String relationshipField) throws PresentationException, IndexUnreachableException {
        
        return this.searchIndex.search(String.format(SEARCH_RELATIONSHIP_FORMAT, relationshipField, pi), List.of(RELATIONSHIP_ID_REFERENCE))
        .stream().map(doc -> SolrTools.getSingleFieldStringValue(doc, RELATIONSHIP_ID_REFERENCE)).collect(Collectors.toList());
        
    }
    
     
    
}
