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
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.ImageDeliveryBean;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.ViewManager;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Resolves the cards displayed by the recommendations section ("Das könnte Sie auch interessieren").
 *
 * <p>Cascading logic:
 * <ol>
 * <li>If the current record carries values in the configured identifier source field
 * (e.g. {@code IdentifierRelatedWork}), the works matching those values in the configured
 * target field are returned.</li>
 * <li>Otherwise a content-similarity fallback returns works sharing any value in the configured
 * soll fields (default {@code MD_TOPIC}).</li>
 * <li>If {@code fillRandom} is enabled and fewer than maxResults were found, the remaining slots
 * are filled with random works from the same collection (DC).</li>
 * </ol>
 *
 * <p>Results are sorted by the configured sort field and capped at maxResults. The method is
 * best-effort and self-contained, mirroring {@link RelatedGroupsResolver}.
 */
public class RecommendationsResolver {

    private static final Logger logger = LogManager.getLogger(RecommendationsResolver.class);

    private static final int FILL_FETCH_MULTIPLIER = 4;
    private static final int FILL_FETCH_CAP = 100;

    private final ImageDeliveryBean imageDelivery;
    private final Random random;

    public RecommendationsResolver(ImageDeliveryBean imageDelivery) {
        this(imageDelivery, new Random());
    }

    /** Package-visible constructor allowing a seeded Random for deterministic tests. */
    RecommendationsResolver(ImageDeliveryBean imageDelivery, Random random) {
        this.imageDelivery = imageDelivery;
        this.random = random;
    }

    /**
     * Executes the resolver. Idempotent and safe to call outside any session lock.
     *
     * @param vm the active record's {@link ViewManager}; may be null
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
        String currentPi = topStruct.getPi();
        if (StringUtils.isBlank(currentPi)) {
            return Collections.emptyList();
        }

        Configuration config = DataManager.getInstance().getConfiguration();
        int maxResults = config.getSidebarWidgetRecommendationsMaxResults();
        if (maxResults <= 0) {
            return Collections.emptyList();
        }

        String titleField = StringUtils.defaultIfBlank(config.getSidebarWidgetRecommendationsTitleField(), SolrConstants.TITLE);
        String subtitleField = StringUtils.defaultIfBlank(config.getSidebarWidgetRecommendationsSubtitleField(), SolrConstants.PERSON_ONEFIELD);
        String sortField = config.getSidebarWidgetRecommendationsSortField();
        String sortOrder = config.getSidebarWidgetRecommendationsSortOrder();
        List<StringPair> sortFields = StringUtils.isNotBlank(sortField)
                ? Collections.singletonList(new StringPair(sortField, StringUtils.defaultIfBlank(sortOrder, "desc")))
                : null;
        List<String> fields = queryFields(titleField, subtitleField);

        // Prio 1: explicit related works via the identifier source field
        List<GroupMemberDetail> results = new ArrayList<>();
        List<String> identifiers = topStruct.getMetadataValues(config.getSidebarWidgetRecommendationsIdentifierSourceField());
        if (identifiers != null && !identifiers.isEmpty()) {
            String targetField = StringUtils.defaultIfBlank(config.getSidebarWidgetRecommendationsIdentifierTargetField(), SolrConstants.PI);
            String query = buildIdentifierQuery(targetField, identifiers, currentPi);
            results = loadCards(query, maxResults, sortFields, fields, titleField, subtitleField);
        }

        // Prio 2: content similarity via configured soll fields
        if (results.isEmpty()) {
            String query = buildSollFieldQuery(config.getSidebarWidgetRecommendationsSollFields(), topStruct, currentPi);
            results = loadCards(query, maxResults, sortFields, fields, titleField, subtitleField);
        }

        // Optional: fill remaining slots with random works from the same collection
        if (config.isSidebarWidgetRecommendationsFillRandom() && results.size() < maxResults) {
            fillFromCollection(results, topStruct, currentPi, maxResults, sortFields, fields, titleField, subtitleField);
        }

        return results.size() <= maxResults ? results : new ArrayList<>(results.subList(0, maxResults));
    }

    /** Query for works whose target field matches any of the current record's identifier values. */
    private static String buildIdentifierQuery(String targetField, List<String> identifiers, String currentPi) {
        List<String> escaped = new ArrayList<>(identifiers.size());
        for (String id : identifiers) {
            if (StringUtils.isNotBlank(id)) {
                escaped.add(ClientUtils.escapeQueryChars(id));
            }
        }
        if (escaped.isEmpty()) {
            return null;
        }
        return targetField + ":(" + String.join(" OR ", escaped) + ")"
                + " AND " + SolrConstants.ISWORK + ":true"
                + " AND -" + SolrConstants.PI + ":" + ClientUtils.escapeQueryChars(currentPi);
    }

    /** Query for works sharing any value of the configured soll fields with the current record. */
    private static String buildSollFieldQuery(List<String> sollFields, StructElement topStruct, String currentPi) {
        if (sollFields == null || sollFields.isEmpty()) {
            return null;
        }
        List<String> clauses = new ArrayList<>();
        for (String field : sollFields) {
            if (StringUtils.isBlank(field)) {
                continue;
            }
            List<String> values = topStruct.getMetadataValues(field);
            if (values == null) {
                continue;
            }
            for (String value : values) {
                if (StringUtils.isNotBlank(value)) {
                    clauses.add(field + ":\"" + ClientUtils.escapeQueryChars(value) + "\"");
                }
            }
        }
        if (clauses.isEmpty()) {
            return null;
        }
        return "(" + String.join(" OR ", clauses) + ")"
                + " AND " + SolrConstants.ISWORK + ":true"
                + " AND -" + SolrConstants.PI + ":" + ClientUtils.escapeQueryChars(currentPi);
    }

    /** Fills free slots with random works from the same collection, excluding already-shown PIs. */
    private void fillFromCollection(List<GroupMemberDetail> results, StructElement topStruct, String currentPi,
            int maxResults, List<StringPair> sortFields, List<String> fields, String titleField, String subtitleField)
            throws PresentationException, IndexUnreachableException {
        List<String> collections = topStruct.getMetadataValues(SolrConstants.DC);
        if (collections == null || collections.isEmpty()) {
            return;
        }
        List<String> excludePis = new ArrayList<>();
        excludePis.add(currentPi);
        for (GroupMemberDetail detail : results) {
            excludePis.add(detail.getPi());
        }
        String query = buildCollectionQuery(collections, excludePis);
        if (StringUtils.isBlank(query)) {
            return;
        }
        int fetchSize = Math.min(maxResults * FILL_FETCH_MULTIPLIER, FILL_FETCH_CAP);
        List<GroupMemberDetail> candidates = loadCards(query, fetchSize, sortFields, fields, titleField, subtitleField);
        Collections.shuffle(candidates, random);
        int needed = maxResults - results.size();
        for (GroupMemberDetail candidate : candidates) {
            if (needed <= 0) {
                break;
            }
            results.add(candidate);
            needed--;
        }
    }

    private static String buildCollectionQuery(List<String> collections, List<String> excludePis) {
        List<String> escapedCollections = new ArrayList<>(collections.size());
        for (String collection : collections) {
            if (StringUtils.isNotBlank(collection)) {
                escapedCollections.add("\"" + ClientUtils.escapeQueryChars(collection) + "\"");
            }
        }
        if (escapedCollections.isEmpty()) {
            return null;
        }
        List<String> escapedPis = new ArrayList<>(excludePis.size());
        for (String pi : excludePis) {
            if (StringUtils.isNotBlank(pi)) {
                escapedPis.add(ClientUtils.escapeQueryChars(pi));
            }
        }
        StringBuilder query = new StringBuilder()
                .append(SolrConstants.DC).append(":(").append(String.join(" OR ", escapedCollections)).append(')')
                .append(" AND ").append(SolrConstants.ISWORK).append(":true");
        if (!escapedPis.isEmpty()) {
            query.append(" AND -").append(SolrConstants.PI).append(":(").append(String.join(" OR ", escapedPis)).append(')');
        }
        return query.toString();
    }

    /** Runs the query and builds cards; returns a mutable list (possibly empty). */
    private List<GroupMemberDetail> loadCards(String query, int rows, List<StringPair> sortFields, List<String> fields,
            String titleField, String subtitleField) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(query) || rows <= 0) {
            return new ArrayList<>();
        }
        SolrDocumentList docs = searchWithSortFallback(query, rows, sortFields, fields);
        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }
        List<GroupMemberDetail> cards = new ArrayList<>(docs.size());
        for (SolrDocument doc : docs) {
            GroupMemberDetail detail = buildCard(doc, titleField, subtitleField);
            if (detail != null) {
                cards.add(detail);
            }
        }
        return cards;
    }

    /** Solr fl list covering card display fields and what the ThumbnailHandler reads internally. */
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

    /** Runs the Solr search; on a sort-related Solr error, retries unsorted (best-effort). */
    private SolrDocumentList searchWithSortFallback(String query, int rows, List<StringPair> sortFields, List<String> fields)
            throws PresentationException, IndexUnreachableException {
        try {
            return DataManager.getInstance().getSearchIndex().search(query, rows, sortFields, fields);
        } catch (PresentationException e) {
            if (sortFields == null) {
                throw e;
            }
            logger.warn("Recommendations sort on '{}' failed, retrying unsorted: {}", sortFields, e.getMessage());
            return DataManager.getInstance().getSearchIndex().search(query, rows, null, fields);
        }
    }

    /** Builds a single card; returns null if the doc has no PI or any RuntimeException occurs. */
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
        } catch (NullPointerException | IllegalArgumentException | IllegalStateException | ClassCastException e) {
            logger.warn("Skipping recommendation card due to error: {}", e.toString());
            return null;
        }
    }

    /** Resolves the thumbnail via the primary handler; returns null on failure. */
    private String resolveThumbnailUrl(SolrDocument doc, String pi) {
        try {
            String url = imageDelivery.getThumbs().getThumbnailUrl(doc);
            if (StringUtils.isNotBlank(url)) {
                return url;
            }
        } catch (ViewerConfigurationException | NullPointerException | IllegalArgumentException | IllegalStateException e) {
            logger.debug("Thumbnail URL failed for {}: {}", pi, e.getMessage());
        }
        return null;
    }
}
