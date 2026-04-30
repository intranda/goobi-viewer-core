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
package io.goobi.viewer.model.viewer.record.relatedgroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Resolves the cards displayed by the related-groups widget/section.
 *
 * <p>For an anchor child the current record's sibling volumes (other works under the same anchor)
 * are returned. Otherwise the records referenced through the {@code GROUPID_*} memberships of the
 * current record are returned. When both apply, the queries are merged in a single Solr OR query.
 *
 * <p>Results are sorted by the configured sort field and capped at the configured maxResults.
 *
 * <p>Failure policy is best-effort: an invalid sort field falls back to an unsorted retry,
 * individual cards that fail to build are skipped with a warn log, and the overall method
 * never throws into the view layer.
 */
public class RelatedGroupsResolver {

    private static final Logger logger = LogManager.getLogger(RelatedGroupsResolver.class);

    /** Solr field used as a generic series-grouping ID; not (yet) declared as a SolrConstant. */
    private static final String GROUPID_SERIES = "GROUPID_SERIES";

    private final ImageDeliveryBean imageDelivery;

    public RelatedGroupsResolver(ImageDeliveryBean imageDelivery) {
        this.imageDelivery = imageDelivery;
    }

    /**
     * Executes the resolver. Idempotent and safe to call outside any session lock.
     *
     * @param vm the active record's {@link ViewManager}; must not be null
     * @return list of cards (possibly empty), sorted and capped according to configuration
     * @throws PresentationException if the underlying Solr query fails irrecoverably
     * @throws IndexUnreachableException if the Solr index is not reachable
     */
    public List<GroupMemberDetail> resolve(ViewManager vm) throws PresentationException, IndexUnreachableException {
        if (vm == null) {
            return Collections.emptyList();
        }
        StructElement topStruct = vm.getTopStructElement();
        if (topStruct == null) {
            return Collections.emptyList();
        }

        Configuration config = DataManager.getInstance().getConfiguration();
        int maxResults = config.getSidebarWidgetRelatedGroupsMaxResults();
        if (maxResults <= 0) {
            logger.warn("Related-groups: maxResults ({}) is <= 0 - section will be empty", maxResults);
            return Collections.emptyList();
        }

        String titleField = StringUtils.defaultIfBlank(config.getSidebarWidgetRelatedGroupsTitleField(), SolrConstants.TITLE);
        String subtitleField = StringUtils.defaultIfBlank(config.getSidebarWidgetRelatedGroupsSubtitleField(), SolrConstants.PERSON_ONEFIELD);
        String sortField = config.getSidebarWidgetRelatedGroupsSortField();
        String sortOrder = config.getSidebarWidgetRelatedGroupsSortOrder();
        List<StringPair> sortFields = StringUtils.isNotBlank(sortField)
                ? Collections.singletonList(new StringPair(sortField, StringUtils.defaultIfBlank(sortOrder, "desc")))
                : null;

        List<String> fields = queryFields(titleField, subtitleField);

        List<String> groupPis = collectGroupMembershipPis(topStruct);
        String anchorPi = vm.getAnchorPi();
        boolean hasAnchor = topStruct.isAnchorChild() && StringUtils.isNotEmpty(anchorPi);
        if (!hasAnchor && groupPis.isEmpty()) {
            return Collections.emptyList();
        }

        String query = buildQuery(hasAnchor, anchorPi, topStruct.getPi(), groupPis);
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }

        SolrDocumentList docs = searchWithSortFallback(query, maxResults, sortFields, fields);
        if (docs == null || docs.isEmpty()) {
            return Collections.emptyList();
        }

        List<GroupMemberDetail> results = new ArrayList<>(docs.size());
        for (SolrDocument doc : docs) {
            GroupMemberDetail detail = buildCard(doc, titleField, subtitleField);
            if (detail != null) {
                results.add(detail);
            }
        }
        return results;
    }

    /** Solr fl list covering both card display fields and what the ThumbnailHandler reads internally. */
    private static List<String> queryFields(String titleField, String subtitleField) {
        List<String> fields = new ArrayList<>(List.of(
                SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.IDDOC,
                SolrConstants.LABEL, SolrConstants.TITLE, SolrConstants.MD_YEARPUBLISH,
                SolrConstants.THUMBNAIL, SolrConstants.MIMETYPE, SolrConstants.DOCSTRCT,
                SolrConstants.DATAREPOSITORY, SolrConstants.ISANCHOR, SolrConstants.ISWORK, SolrConstants.FILENAME));
        if (StringUtils.isNotBlank(titleField) && !fields.contains(titleField)) {
            fields.add(titleField);
        }
        if (StringUtils.isNotBlank(subtitleField) && !fields.contains(subtitleField)) {
            fields.add(subtitleField);
        }
        return fields;
    }

    /** De-duplicated GROUPID_* PIs from the given struct, defensively handling null maps. */
    private static List<String> collectGroupMembershipPis(StructElement topStruct) {
        List<String> groupPis = new ArrayList<>();
        Map<String, String> memberships = topStruct.getGroupMemberships();
        if (memberships == null || memberships.isEmpty()) {
            return groupPis;
        }
        for (String pi : memberships.values()) {
            if (StringUtils.isNotBlank(pi) && !groupPis.contains(pi)) {
                groupPis.add(pi);
            }
        }
        return groupPis;
    }

    /** Combined OR-query for the anchor-sibling branch and/or the GROUPID-membership branch. */
    private static String buildQuery(boolean hasAnchor, String anchorPi, String currentPi, List<String> groupPis) {
        List<String> clauses = new ArrayList<>();
        if (hasAnchor && StringUtils.isNotBlank(currentPi)) {
            clauses.add("(" + SolrConstants.PI_ANCHOR + ":" + ClientUtils.escapeQueryChars(anchorPi)
                    + " AND " + SolrConstants.ISWORK + ":true"
                    + " AND -" + SolrConstants.PI + ":" + ClientUtils.escapeQueryChars(currentPi) + ")");
        }
        if (!groupPis.isEmpty()) {
            List<String> escapedPis = new ArrayList<>(groupPis.size());
            for (String pi : groupPis) {
                escapedPis.add(ClientUtils.escapeQueryChars(pi));
            }
            clauses.add(SolrConstants.PI + ":(" + String.join(" OR ", escapedPis) + ")");
        }
        return String.join(" OR ", clauses);
    }

    /** Runs the Solr search; on a sort-related Solr error, retries unsorted (best-effort). */
    private SolrDocumentList searchWithSortFallback(String query, int rows, List<StringPair> sortFields, List<String> fields)
            throws PresentationException, IndexUnreachableException {
        try {
            return DataManager.getInstance().getSearchIndex().search(query, rows, sortFields, fields);
        } catch (PresentationException e) {
            if (sortFields == null) {
                throw e;
            }
            logger.warn("Related-groups sort on '{}' failed, retrying unsorted: {}", sortFields, e.getMessage());
            return DataManager.getInstance().getSearchIndex().search(query, rows, null, fields);
        }
    }

    /** Builds a single card; returns null if the doc is missing a PI (cannot link) or any RuntimeException occurs. */
    private GroupMemberDetail buildCard(SolrDocument doc, String titleField, String subtitleField) {
        try {
            String pi = SolrTools.getSingleFieldStringValue(doc, SolrConstants.PI);
            if (StringUtils.isBlank(pi)) {
                return null;
            }
            String title = SolrTools.getSingleFieldStringValue(doc, titleField);
            if (StringUtils.isBlank(title)) {
                title = SolrTools.getSingleFieldStringValue(doc, SolrConstants.LABEL);
            }
            if (StringUtils.isBlank(title)) {
                title = SolrTools.getSingleFieldStringValue(doc, SolrConstants.TITLE);
            }
            String subtitle = SolrTools.getSingleFieldStringValue(doc, subtitleField);
            String year = SolrTools.getSingleFieldStringValue(doc, SolrConstants.MD_YEARPUBLISH);
            String thumbnailUrl = resolveThumbnailUrl(doc, pi);
            return new GroupMemberDetail(pi, title, subtitle, year, thumbnailUrl);
        } catch (RuntimeException e) {
            logger.warn("Skipping related-groups card due to error: {}", e.toString());
            return null;
        }
    }

    /** Tries the primary thumbnail handler, then a series/anchor fallback if no URL was returned. */
    private String resolveThumbnailUrl(SolrDocument doc, String pi) {
        try {
            String url = imageDelivery.getThumbs().getThumbnailUrl(doc);
            if (StringUtils.isNotBlank(url)) {
                return url;
            }
        } catch (ViewerConfigurationException | RuntimeException e) {
            logger.debug("Primary thumbnail URL failed for {}: {}", pi, e.getMessage());
        }
        if (StringUtils.isNotBlank(pi)) {
            return findFallbackThumbnailUrl(pi);
        }
        return null;
    }

    /**
     * Resolves a thumbnail for a series/anchor record without its own THUMBNAIL field
     * by picking the first indexed child/member that has one.
     */
    private String findFallbackThumbnailUrl(String pi) {
        String escapedPi = ClientUtils.escapeQueryChars(pi);
        String fallbackQuery = "(" + SolrConstants.PI_ANCHOR + ":" + escapedPi
                + " OR " + GROUPID_SERIES + ":" + escapedPi + ")"
                + " AND " + SolrConstants.THUMBNAIL + ":*";
        try {
            SolrDocumentList fallbackDocs = DataManager.getInstance().getSearchIndex()
                    .search(fallbackQuery, 1, null,
                            List.of(SolrConstants.PI, SolrConstants.PI_TOPSTRUCT, SolrConstants.IDDOC, SolrConstants.THUMBNAIL,
                                    SolrConstants.MIMETYPE, SolrConstants.DOCSTRCT, SolrConstants.DATAREPOSITORY, SolrConstants.ISANCHOR,
                                    SolrConstants.ISWORK));
            if (fallbackDocs != null && !fallbackDocs.isEmpty()) {
                try {
                    return imageDelivery.getThumbs().getThumbnailUrl(fallbackDocs.get(0));
                } catch (RuntimeException re) {
                    logger.warn("Fallback thumbnail URL generation failed for {}: {}", pi, re.getMessage());
                }
            }
        } catch (PresentationException | IndexUnreachableException | ViewerConfigurationException e) {
            logger.warn("Fallback thumbnail lookup failed for {}: {}", pi, e.getMessage());
        }
        return null;
    }
}
