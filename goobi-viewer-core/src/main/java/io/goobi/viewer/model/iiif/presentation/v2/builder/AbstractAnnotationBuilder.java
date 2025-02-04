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
package io.goobi.viewer.model.iiif.presentation.v2.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.annotation.IAnnotation;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * @author florian
 *
 */
public class AbstractAnnotationBuilder {

    private static final Logger logger = LogManager.getLogger(AbstractAnnotationBuilder.class);

    protected static final String[] UGC_SOLR_FIELDS = { SolrConstants.IDDOC, SolrConstants.PI_TOPSTRUCT, SolrConstants.ORDER, SolrConstants.UGCTYPE,
            SolrConstants.MD_TEXT, SolrConstants.UGCCOORDS, SolrConstants.MD_BODY, SolrConstants.UGCTERMS, SolrConstants.ACCESSCONDITION,
            SolrConstants.MD_ANNOTATION_ID };

    private final AbstractBuilder restBuilder;

    public AbstractAnnotationBuilder(AbstractApiUrlManager urls) {
        this.restBuilder = new AbstractBuilder(urls) {
            //
        };
    }

    public String getAnnotationQuery() {
        return "+" + SolrConstants.DOCTYPE + ":" + DocType.UGC.name();
    }

    public String getAnnotationQuery(String pi) {
        return "+" + SolrConstants.PI_TOPSTRUCT + ":" + pi + " " + getAnnotationQuery();
    }

    public String getAnnotationQuery(String pi, int pageNo) {
        return "+" + SolrConstants.ORDER + ":" + pageNo + " " + getAnnotationQuery(pi);
    }

    /**
     * Search for both UGC docs with given IDDOC and with MD_ANNOTATION_ID = "annotation_<id>". Searching for IDDOC is only included for backwards
     * compatibility purposes. The correct identifier is MD_ANNOTATION_ID since it reflects the original sql identifier
     *
     * @param id
     * @return Generated query
     */
    public String getAnnotationQuery(long id) {
        return "+(" + SolrConstants.MD_ANNOTATION_ID + ":annotation_" + id + " " + SolrConstants.IDDOC + ":" + id + ")";
    }

    /**
     * 
     * @param query
     * @param request
     * @return List<SolrDocument>
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<SolrDocument> getAnnotationDocuments(String query, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException {
        return getAnnotationDocuments(query, getDefaultSortFields(), request);
    }

    /**
     * Return sort fields to sort results ascending after topstruct PI, then page order, then IDDOC;
     *
     * @return sort fields for PI_TOPSTRUCT, ORDER and IDDOC
     */
    private static List<StringPair> getDefaultSortFields() {
        StringPair sortField1 = new StringPair(SolrConstants.PI_TOPSTRUCT, "asc");
        StringPair sortField2 = new StringPair(SolrConstants.ORDER, "asc");
        StringPair sortField3 = new StringPair(SolrConstants.IDDOC, "asc");
        return Arrays.asList(sortField1, sortField2, sortField3);
    }

    /**
     * 
     * @param query
     * @param sortFields
     * @param request
     * @return List<SolrDocument>
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<SolrDocument> getAnnotationDocuments(String query, List<StringPair> sortFields, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException {
        return getAnnotationDocuments(query, 0, SolrSearchIndex.MAX_HITS, sortFields, request);
    }

    /**
     * 
     * @param query
     * @param first
     * @param rows
     * @param sortFields
     * @param request
     * @return List<SolrDocument>
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public List<SolrDocument> getAnnotationDocuments(String query, int first, int rows, final List<StringPair> sortFields, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException {
        // logger.trace("getAnnotationDocuments: {}", query); //NOSONAR Debug
        SolrDocumentList hits =
                DataManager.getInstance()
                        .getSearchIndex()
                        .search(query, first, rows, sortFields != null ? sortFields : getDefaultSortFields(), null, Arrays.asList(UGC_SOLR_FIELDS))
                        .getResults();
        if (hits.isEmpty()) {
            return Collections.emptyList();
        }
        return hits.stream().filter(hit -> isAccessGranted(hit, query, request)).collect(Collectors.toList());
    }

    /**
     * 
     * @param id
     * @param request
     * @return Optional<SolrDocument>
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public Optional<SolrDocument> getAnnotationDocument(long id, HttpServletRequest request) throws PresentationException, IndexUnreachableException {
        SolrDocument hit = DataManager.getInstance().getSearchIndex().getFirstDoc(getAnnotationQuery(id), Arrays.asList(UGC_SOLR_FIELDS));
        return Optional.ofNullable(hit);
    }

    /**
     * 
     * @param doc
     * @param query
     * @param request
     * @return true if user session is allowed access; false otherwise
     */
    private static boolean isAccessGranted(SolrDocument doc, String query, HttpServletRequest request) {
        String accessCondition = SolrTools.getSingleFieldStringValue(doc, SolrConstants.ACCESSCONDITION);
        if (StringUtils.isBlank(accessCondition) || "OPENACCESS".equalsIgnoreCase(accessCondition)) {
            return true;
        } else if (request != null) {
            try {
                return AccessConditionUtils.checkAccessPermission(Collections.singleton(accessCondition),
                        IPrivilegeHolder.PRIV_VIEW_UGC, query, request).isGranted();
            } catch (IndexUnreachableException | PresentationException | DAOException e) {
                logger.error("Failed to check access to annotation", e);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 
     * @param request
     * @return Annotation count
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public long getAnnotationCount(HttpServletRequest request) throws PresentationException, IndexUnreachableException {
        SolrDocumentList hits = DataManager.getInstance()
                .getSearchIndex()
                .search(getAnnotationQuery(), SolrSearchIndex.MAX_HITS, null, Arrays.asList(SolrConstants.ACCESSCONDITION));
        if (hits.isEmpty()) {
            return 0;
        }
        return hits.stream().filter(hit -> isAccessGranted(hit, getAnnotationQuery(), request)).count();
    }

    protected AbstractBuilder getRestBuilder() {
        return restBuilder;
    }

    /**
     * 
     * @param idString
     * @return Optional<IAnnotation>
     */
    public Optional<IAnnotation> getAnnotation(String idString) {
        int underscoreIndex = idString.lastIndexOf("_");
        Long id = Long.parseLong(idString.substring(underscoreIndex > -1 ? underscoreIndex + 1 : 0));

        try {
            CrowdsourcingAnnotation pa = DataManager.getInstance().getDao().getAnnotation(id);
            return Optional.ofNullable(pa).map(a -> new AnnotationConverter().getAsWebAnnotation(a));
        } catch (DAOException e) {
            logger.error("Error getting annotation {} from DAO: {}", idString, e.toString());
            return Optional.empty();
        }
    }
}
