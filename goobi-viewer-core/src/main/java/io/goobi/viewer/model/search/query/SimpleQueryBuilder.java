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
package io.goobi.viewer.model.search.query;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.model.search.SearchFilter;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.solr.SolrConstants;

/**
 * Builds a simple search String for a {@link SearchBean}
 */
public final class SimpleQueryBuilder {

    private static final Logger logger = LogManager.getLogger(SimpleQueryBuilder.class);

    public static final String URL_ENCODING = "UTF8";

    private SearchFilter searchFilter;
    private boolean fuzzySearchEnabled;
    private Map<String, Set<String>> searchTerms;

    private SimpleQueryBuilder() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public QueryResult build(String rawInput) {
        String normalized = normalizeInput(rawInput);
        if (normalized.isEmpty()) {
            return emptyResult();
        }

        String display = StringTools.stripJS(normalized).trim();
        String normalizedQuery = normalizeOperators(normalized);

        if ("*".equals(normalizedQuery)) {
            return new QueryResult(display,
                    SearchHelper.prepareQuery(""), this.searchTerms,
                    0);
        }

        if (normalizedQuery.contains("\"")) {
            return buildPhraseQuery(display, normalizedQuery);
        }
        return buildTermQuery(display, normalizedQuery);
    }

    private QueryResult buildPhraseQuery(String display, final String query) {
        int proximity =
                SearchHelper.extractProximitySearchDistanceFromQuery(query);

        String q = query;
        if (proximity > 0) {
            q = SearchHelper.removeProximitySearchToken(q);
        }

        StringBuilder sb = new StringBuilder();
        extractPhrases(q).forEach(
                phrase -> appendPhrase(sb, phrase, proximity));

        return new QueryResult(
                display,
                trimTrailingOperator(sb.toString()), this.searchTerms,
                proximity);
    }

    private static List<String> extractPhrases(String query) {
        if (query == null || query.isEmpty()) {
            return List.of();
        }

        String[] parts = query.split("\"");
        List<String> phrases = new ArrayList<>(parts.length);

        for (String part : parts) {
            String phrase = part.replace("\"", "").trim();
            if (!phrase.isEmpty()) {
                phrases.add(phrase);
            }
        }
        return phrases;
    }

    private static String trimTrailingOperator(String query) {
        if (query.endsWith(SolrConstants.SOLR_QUERY_OR)) {
            return query.substring(0, query.length() - 4);
        }
        if (query.endsWith(SolrConstants.SOLR_QUERY_AND)) {
            return query.substring(0, query.length() - 5);
        }
        return query;
    }

    private QueryResult buildTermQuery(String display, String query) {
        List<String> preparedTerms = prepareTerms(query);
        String innerQuery = SearchHelper.buildTermQuery(preparedTerms);

        if (innerQuery.isEmpty()) {
            return emptyResult();
        }

        String outerQuery = buildOuterQuery(innerQuery);
        return new QueryResult(display, outerQuery, this.searchTerms, 0);
    }

    private void appendPhrase(StringBuilder sb, String phrase, int proximity) {

        if (isAllFields()) {
            appendAllFieldPhrase(sb, phrase, proximity);
        } else {
            appendFilteredPhrase(sb, phrase, proximity);
        }
        sb.append(SolrConstants.SOLR_QUERY_AND);
    }

    private List<String> prepareTerms(String query) {
        String[] rawTerms = query.replace(" &&", "")
                .split(SearchHelper.SEARCH_TERM_SPLIT_REGEX);

        List<String> prepared = new ArrayList<>();
        Iterator<String> it = Arrays.asList(rawTerms).iterator();

        while (it.hasNext()) {
            String term = cleanTerm(it.next());
            if (term.isEmpty()) {
                continue;
            }

            if ("||".equals(term) && it.hasNext() && !prepared.isEmpty()) {
                String prev = prepared.remove(prepared.size() - 1);
                String next = cleanTerm(it.next());
                prepared.add(prev + " OR " + next);
                updateSearchTerms(next);
            } else {
                prepared.add(term);
                updateSearchTerms(term);
            }
        }
        return prepared;
    }

    private String cleanTerm(String raw) {
        if (raw == null) {
            return "";
        }

        String term = SearchHelper.cleanUpSearchTerm(raw.trim());
        term = term.replace("\\*", "*"); // unescape falsely escaped truncation

        if (term.isEmpty()) {
            return "";
        }

        if (DataManager.getInstance()
                .getConfiguration()
                .getStopwords()
                .contains(term)) {
            return "";
        }

        if (fuzzySearchEnabled) {
            String[] wildcards = SearchHelper.getWildcardsTokens(term);
            term = SearchHelper.addFuzzySearchToken(
                    wildcards[1],
                    wildcards[0],
                    wildcards[2]);
        }

        logger.trace("term: {}", term);
        return term;
    }

    private String buildOuterQuery(String innerQuery) {
        StringBuilder sbOuter = new StringBuilder();

        if (isAllFields()) {
            sbOuter.append(SolrConstants.SUPERDEFAULT).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.SUPERFULLTEXT).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.SUPERUGCTERMS).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.SUPERSEARCHTERMS_ARCHIVE).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.DEFAULT).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.FULLTEXT).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.NORMDATATERMS).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.UGCTERMS).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.SEARCHTERMS_ARCHIVE).append(":(").append(innerQuery);
            sbOuter.append(") ").append(SolrConstants.CMS_TEXT_ALL).append(":(").append(innerQuery).append(')');
        } else {
            appendFilteredOuterQuery(sbOuter, innerQuery);
        }
        return sbOuter.toString();
    }

    private static String normalizeInput(String input) {
        if (input == null || "-".equals(input)) {
            return "";
        }
        try {
            return URLDecoder.decode(input, URL_ENCODING);
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return input;
        }
    }

    private static String normalizeOperators(String input) {
        return input.replace(SolrConstants.SOLR_QUERY_OR, " || ")
                .replace(SolrConstants.SOLR_QUERY_AND, " && ")
                .toLowerCase();
    }

    private boolean isAllFields() {
        return searchFilter == null
                || searchFilter.equals(SearchHelper.SEARCH_FILTER_ALL);
    }

    private static void appendAllFieldPhrase(StringBuilder sb,
            String phrase,
            int proximityDistance) {

        appendPhraseField(sb, SolrConstants.SUPERDEFAULT, phrase, proximityDistance);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.SUPERFULLTEXT, phrase, proximityDistance);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.SUPERUGCTERMS, phrase, 0);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.SUPERSEARCHTERMS_ARCHIVE, phrase, 0);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.DEFAULT, phrase, proximityDistance);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.FULLTEXT, phrase, proximityDistance);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.NORMDATATERMS, phrase, 0);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.UGCTERMS, phrase, 0);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.SEARCHTERMS_ARCHIVE, phrase, 0);
        sb.append(SolrConstants.SOLR_QUERY_OR);

        appendPhraseField(sb, SolrConstants.CMS_TEXT_ALL, phrase, 0);
    }

    private static void appendPhraseField(StringBuilder sb,
            String field,
            String phrase,
            int proximityDistance) {

        sb.append(field)
                .append(":(\"")
                .append(phrase)
                .append('"');

        if (proximityDistance > 0) {
            sb.append('~').append(proximityDistance);
        }

        sb.append(')');
    }

    private void appendFilteredPhrase(StringBuilder sb,
            String phrase,
            int proximityDistance) {

        String field = searchFilter.getField();

        switch (field) {

            case SolrConstants.DEFAULT:
                appendPhraseField(sb, SolrConstants.SUPERDEFAULT, phrase, 0);
                sb.append(SolrConstants.SOLR_QUERY_OR);
                appendPhraseField(sb, SolrConstants.DEFAULT, phrase, 0);
                break;

            case SolrConstants.FULLTEXT:
                appendPhraseField(sb, SolrConstants.SUPERFULLTEXT, phrase, proximityDistance);
                sb.append(SolrConstants.SOLR_QUERY_OR);
                appendPhraseField(sb, SolrConstants.FULLTEXT, phrase, proximityDistance);
                break;

            case SolrConstants.UGCTERMS:
                appendPhraseField(sb, SolrConstants.SUPERUGCTERMS, phrase, 0);
                sb.append(SolrConstants.SOLR_QUERY_OR);
                appendPhraseField(sb, SolrConstants.UGCTERMS, phrase, 0);
                break;

            case SolrConstants.SEARCHTERMS_ARCHIVE:
                appendPhraseField(sb, SolrConstants.SUPERSEARCHTERMS_ARCHIVE, phrase, 0);
                sb.append(SolrConstants.SOLR_QUERY_OR);
                appendPhraseField(sb, SolrConstants.SEARCHTERMS_ARCHIVE, phrase, 0);
                break;

            default:
                appendPhraseField(sb, field, phrase, 0);
                break;
        }
    }

    private void appendFilteredOuterQuery(StringBuilder sb, String innerQuery) {

        String field = searchFilter.getField();

        switch (field) {

            case SolrConstants.DEFAULT:
                sb.append(SolrConstants.SUPERDEFAULT)
                        .append(":(")
                        .append(innerQuery)
                        .append(')')
                        .append(SolrConstants.SOLR_QUERY_OR);
                sb.append(SolrConstants.DEFAULT)
                        .append(":(")
                        .append(innerQuery)
                        .append(')');
                break;

            case SolrConstants.FULLTEXT:
                sb.append(SolrConstants.SUPERFULLTEXT)
                        .append(":(")
                        .append(innerQuery)
                        .append(')')
                        .append(SolrConstants.SOLR_QUERY_OR);
                sb.append(SolrConstants.FULLTEXT)
                        .append(":(")
                        .append(innerQuery)
                        .append(')');
                break;

            case SolrConstants.UGCTERMS:
                sb.append(SolrConstants.SUPERUGCTERMS)
                        .append(":(")
                        .append(innerQuery)
                        .append(')')
                        .append(SolrConstants.SOLR_QUERY_OR);
                sb.append(SolrConstants.UGCTERMS)
                        .append(":(")
                        .append(innerQuery)
                        .append(')');
                break;

            case SolrConstants.SEARCHTERMS_ARCHIVE:
                sb.append(SolrConstants.SUPERSEARCHTERMS_ARCHIVE)
                        .append(":(")
                        .append(innerQuery)
                        .append(')')
                        .append(SolrConstants.SOLR_QUERY_OR);
                sb.append(SolrConstants.SEARCHTERMS_ARCHIVE)
                        .append(":(")
                        .append(innerQuery)
                        .append(')');
                break;

            default:
                sb.append(field)
                        .append(":(")
                        .append(innerQuery)
                        .append(')');
                break;
        }
    }

    private void updateSearchTerms(String unescapedTerm) {
        if (unescapedTerm == null || unescapedTerm.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Set<String>> entry : searchTerms.entrySet()) {
            entry.getValue().add(unescapedTerm);
        }
    }

    private QueryResult emptyResult() {
        return new QueryResult("", "", this.searchTerms, 0);
    }

    public static final class Builder {

        private final SimpleQueryBuilder instance = new SimpleQueryBuilder();

        public Builder withSearchFilter(SearchFilter filter) {
            instance.searchFilter = filter;
            return this;
        }

        public Builder withFuzzySearchEnabled(boolean enabled) {
            instance.fuzzySearchEnabled = enabled;
            return this;
        }

        public Builder withSearchTerms(Map<String, Set<String>> searchTerms) {
            instance.searchTerms = searchTerms;
            return this;
        }

        public SimpleQueryBuilder build() {
            return instance;
        }
    }

}
