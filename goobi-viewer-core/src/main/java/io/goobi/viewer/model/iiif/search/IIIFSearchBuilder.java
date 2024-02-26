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
package io.goobi.viewer.model.iiif.search;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.iiif.search.AutoSuggestResult;
import de.intranda.api.iiif.search.SearchHit;
import de.intranda.api.iiif.search.SearchResult;
import de.intranda.api.iiif.search.SearchResultLayer;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.iiif.presentation.v2.builder.OpenAnnotationBuilder;
import io.goobi.viewer.model.iiif.search.model.AnnotationResultList;
import io.goobi.viewer.model.iiif.search.model.SearchTermList;
import io.goobi.viewer.model.iiif.search.parser.AbstractSearchParser;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Creates a IIIF Search API v1.0 response as {@link de.intranda.api.iiif.search.SearchResult}
 *
 * @author florian
 */
public class IIIFSearchBuilder {

    private static final String MOTIVATION_NON_PAINTING = "non-painting";

    private static final Logger logger = LogManager.getLogger(IIIFSearchBuilder.class);

    private static final List<String> FULLTEXTFIELDLIST =
            Arrays.asList(SolrConstants.FILENAME_ALTO, SolrConstants.FILENAME_FULLTEXT, SolrConstants.ORDER);

    private final String query;
    private final String pi;
    private SearchResultConverter converter;
    private List<String> motivation = new ArrayList<>();
    private String user = null;
    private String date = null;
    private String min = null;
    private int page = 1;
    private int hitsPerPage = 20;
    private AbstractApiUrlManager urls;
    private final HttpServletRequest request;

    /**
     * Initializes the builder with all required parameters
     *
     * @param urls
     * @param query the query string
     * @param pi the pi of the manifest to search
     * @param request
     */
    public IIIFSearchBuilder(AbstractApiUrlManager urls, final String query, String pi, HttpServletRequest request) {
        this.query = query != null ? query.replace("+", " ") : query;
        this.pi = pi;
        this.urls = urls;
        this.converter = new SearchResultConverter(urls, pi, 0);
        this.request = request;
    }

    /**
     * <p>
     * Getter for the field <code>query</code>.
     * </p>
     *
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * <p>
     * Getter for the field <code>pi</code>.
     * </p>
     *
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * <p>
     * Setter for the field <code>motivation</code>.
     * </p>
     *
     * @param motivation the motivation to set
     * @return a {@link io.goobi.viewer.model.iiif.search.IIIFSearchBuilder} object.
     */
    public IIIFSearchBuilder setMotivation(final String motivation) {
        if (StringUtils.isNotBlank(motivation)) {
            this.motivation = Arrays.asList(StringUtils.split(motivation.replace("+", " "), " "));
        }
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>motivation</code>.
     * </p>
     *
     * @return the motivation
     */
    public List<String> getMotivation() {
        return motivation;
    }

    public String getMotivationAsString() {
        if (this.motivation.isEmpty()) {
            return "";
        }
        return StringUtils.join(this.motivation, "+");
    }

    /**
     * <p>
     * Setter for the field <code>user</code>.
     * </p>
     *
     * @param user the user to set
     * @return a {@link io.goobi.viewer.model.iiif.search.IIIFSearchBuilder} object.
     */
    public IIIFSearchBuilder setUser(String user) {
        this.user = user;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>user</code>.
     * </p>
     *
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * <p>
     * Setter for the field <code>date</code>.
     * </p>
     *
     * @param date the date to set
     * @return a {@link io.goobi.viewer.model.iiif.search.IIIFSearchBuilder} object.
     */
    public IIIFSearchBuilder setDate(String date) {
        this.date = date;
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>date</code>.
     * </p>
     *
     * @return the date
     */
    public String getDate() {
        return date;
    }

    /**
     * <p>
     * Getter for the field <code>min</code>.
     * </p>
     *
     * @return the min
     */
    public String getMin() {
        return min;
    }

    /**
     * <p>
     * Setter for the field <code>min</code>.
     * </p>
     *
     * @param min the min to set
     * @return a {@link io.goobi.viewer.model.iiif.search.IIIFSearchBuilder} object.
     */
    public IIIFSearchBuilder setMin(String min) {
        this.min = min;
        return this;
    }

    /**
     * <p>
     * Setter for the field <code>page</code>.
     * </p>
     *
     * @param page the page to set
     * @return a {@link io.goobi.viewer.model.iiif.search.IIIFSearchBuilder} object.
     */
    public IIIFSearchBuilder setPage(Integer page) {
        if (page != null) {
            this.page = page;
        }
        return this;
    }

    /**
     * <p>
     * Getter for the field <code>page</code>.
     * </p>
     *
     * @return the page
     */
    public int getPage() {
        return page;
    }

    /**
     * <p>
     * Getter for the field <code>hitsPerPage</code>.
     * </p>
     *
     * @return the hitsPerPage
     */
    public int getHitsPerPage() {
        return hitsPerPage;
    }

    /**
     * <p>
     * Setter for the field <code>hitsPerPage</code>.
     * </p>
     *
     * @param hitsPerPage the hitsPerPage to set
     * @return a {@link io.goobi.viewer.model.iiif.search.IIIFSearchBuilder} object.
     */
    public IIIFSearchBuilder setHitsPerPage(int hitsPerPage) {
        this.hitsPerPage = hitsPerPage;
        return this;
    }

    /**
     * @return a list of all passed paramters that are ignored
     */
    private List<String> getIgnoredParameterList() {
        List<String> ignored = new ArrayList<>();
        if (StringUtils.isNotBlank(getUser())) {
            ignored.add("user");
        }
        if (StringUtils.isNotBlank(getDate())) {
            ignored.add("date");
        }
        if (StringUtils.isNotBlank(getMin())) {
            ignored.add("min");
        }
        return ignored;
    }

    /**
     * Creates a {@link de.intranda.api.iiif.search.SearchResult} containing annotations matching {@link #getQuery()} within {@link #getPi()}. The
     * answer may contain more than {@link #getHitsPerPage()} hits if more than one motivation is searched, but no more than {@link #getHitsPerPage()}
     * hits per motivation.
     *
     * @return the search result
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SearchResult build() throws PresentationException, IndexUnreachableException {

        AnnotationResultList resultList = new AnnotationResultList();

        long mostHits = 0;
        long total = 0;
        if (StringUtils.isNotBlank(query)) {
            if (motivation.isEmpty() || motivation.contains(Motivation.PAINTING)) {
                AnnotationResultList fulltextAnnotations = searchFulltext(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(fulltextAnnotations);
                mostHits = Math.max(mostHits, fulltextAnnotations.getNumHits());
                total += fulltextAnnotations.getNumHits();
            }
            if (motivation.isEmpty() || motivation.contains(MOTIVATION_NON_PAINTING) || motivation.contains(Motivation.DESCRIBING)) {
                AnnotationResultList annotations = searchAnnotations(query, pi, getFirstHitIndex(getPage()), getHitsPerPage(), request);
                resultList.add(annotations);
                mostHits = Math.max(mostHits, annotations.getNumHits());
                total += annotations.getNumHits();

            }
            if (motivation.isEmpty() || motivation.contains(MOTIVATION_NON_PAINTING) || motivation.contains(Motivation.DESCRIBING)) {
                AnnotationResultList metadata = searchMetadata(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(metadata);
                mostHits = Math.max(mostHits, metadata.getNumHits());
                total += metadata.getNumHits();

            }
            if (motivation.isEmpty() || motivation.contains(MOTIVATION_NON_PAINTING) || motivation.contains(Motivation.DESCRIBING)) {
                AnnotationResultList annotations = searchComments(query, pi, getFirstHitIndex(getPage()), getHitsPerPage());
                resultList.add(annotations);
                mostHits = Math.max(mostHits, annotations.getNumHits());
                total += annotations.getNumHits();

            }
        }

        int lastPageNo = 1 + (int) mostHits / getHitsPerPage();

        SearchResult searchResult = new SearchResult(getURI(getPage()));
        searchResult.setResources(resultList.getAnnotations());
        searchResult.setHits(resultList.getHits());
        searchResult.setStartIndex(getFirstHitIndex(getPage()));

        if (getPage() > 1) {
            searchResult.setPrev(getURI(getPage() - 1));
        }
        if (getPage() < lastPageNo) {
            searchResult.setNext(getURI(getPage() + 1));
        }
        SearchResultLayer layer = new SearchResultLayer();
        layer.setTotal(total);
        layer.setIgnored(getIgnoredParameterList());
        layer.setFirst(getURI(1));
        layer.setLast(getURI(lastPageNo));
        searchResult.setWithin(layer);

        return searchResult;
    }

    /**
     * Creates a {@link de.intranda.api.iiif.search.AutoSuggestResult} containing searchTerms matching {@link #getQuery()} within {@link #getPi()}.
     *
     * @return The searchTerm list
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public AutoSuggestResult buildAutoSuggest() throws PresentationException, IndexUnreachableException {

        SearchTermList terms = new SearchTermList();
        if (StringUtils.isNotBlank(query)) {
            if (motivation.isEmpty() || motivation.contains("painting")) {
                //add terms from fulltext?
            }
            if (motivation.isEmpty() || motivation.contains(MOTIVATION_NON_PAINTING) || motivation.contains("describing")) {
                terms.addAll(autoSuggestAnnotations(query, getPi(), request));
            }
            if (motivation.isEmpty() || motivation.contains(MOTIVATION_NON_PAINTING) || motivation.contains("describing")) {
                terms.addAll(autoSuggestMetadata(query, getPi()));
            }
            if (motivation.isEmpty() || motivation.contains(MOTIVATION_NON_PAINTING) || motivation.contains("commenting")) {
                terms.addAll(autoSuggestComments(query, getPi()));
            }
        }

        AutoSuggestResult result = new AutoSuggestResult(converter.getPresentationBuilder().getAutoSuggestURI(getPi(), getQuery(), getMotivation()));
        result.setIgnored(getIgnoredParameterList());
        result.setTerms(terms);
        return result;
    }

    private AnnotationResultList searchComments(String query, String pi, int firstHitIndex, int hitsPerPage) {

        AnnotationResultList results = new AnnotationResultList();
        String queryRegex = AbstractSearchParser.getQueryRegex(query);

        try {
            List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork(pi);
            comments = comments.stream()
                    .filter(c -> c.getContentString().matches(AbstractSearchParser.getContainedWordRegex(queryRegex)))
                    .toList();
            if (firstHitIndex < comments.size()) {
                comments = comments.subList(firstHitIndex, Math.min(firstHitIndex + hitsPerPage, comments.size()));
                for (Comment comment : comments) {
                    results.add(converter.convertCommentToHit(queryRegex, pi, comment));
                }
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    /**
     * 
     * @param query
     * @param pi Record identifier
     * @return {@link SearchTermList}
     */
    private SearchTermList autoSuggestComments(String query, String pi) {

        SearchTermList terms = new SearchTermList();
        String queryRegex = AbstractSearchParser.getAutoSuggestRegex(query);

        try {
            List<Comment> comments = DataManager.getInstance().getDao().getCommentsForWork(pi);
            comments = comments.stream()
                    .filter(c -> c.getContentString().matches(AbstractSearchParser.getContainedWordRegex(queryRegex)))
                    .toList();
            for (Comment comment : comments) {
                terms.addAll(converter.getSearchTerms(queryRegex, comment.getContentString(), getMotivation()));
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    /**
     * 
     * @param query
     * @param pi Record identifier
     * @param firstHitIndex
     * @param hitsPerPage
     * @return {@link AnnotationResultList}
     */
    private AnnotationResultList searchMetadata(String query, String pi, int firstHitIndex, int hitsPerPage) {

        AnnotationResultList results = new AnnotationResultList();
        List<String> searchFields = getSearchFields();
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();

        if (searchFields.isEmpty()) {
            return results;
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:DOCSTRCT");
        queryBuilder.append(" +( ");
        for (String field : searchFields) {
            if (StringUtils.isNotBlank(field)) {
                queryBuilder.append(field).append(":").append(query).append(" ");
            }
        }
        queryBuilder.append(")");

        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getDocStructSortFields(),
                            converter.getPresentationBuilder().getSolrFieldList());
            long hitIndex = 0;
            for (SolrDocument doc : docList) {
                Map<String, List<String>> fieldNames = SolrTools.getFieldValueMap(doc);
                for (String fieldName : fieldNames.keySet()) {
                    if (fieldNameMatches(fieldName, displayFields)) {
                        hitIndex++;
                        String fieldValue = fieldNames.get(fieldName).stream().collect(Collectors.joining(" "));
                        String containesWordRegex = AbstractSearchParser.getContainedWordRegex(AbstractSearchParser.getQueryRegex(query));
                        if (fieldValue.matches(containesWordRegex) && hitIndex >= firstHitIndex && hitIndex < firstHitIndex + hitsPerPage) {
                            SearchHit hit = converter.convertMetadataToHit(AbstractSearchParser.getQueryRegex(query), fieldName, doc);
                            results.add(hit);
                        }
                    }
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    /**
     * 
     * @param query
     * @param pi Record identifier
     * @return SearchTermList
     */
    private SearchTermList autoSuggestMetadata(String query, String pi) {

        SearchTermList terms = new SearchTermList();
        List<String> searchFields = getSearchFields();
        List<String> displayFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();

        if (searchFields.isEmpty()) {
            return terms;
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:DOCSTRCT");
        queryBuilder.append(" +( ");
        for (String field : searchFields) {
            queryBuilder.append(field).append(":").append(query).append("* ");
        }
        queryBuilder.append(")");

        try {
            SolrDocumentList docList = DataManager.getInstance()
                    .getSearchIndex()
                    .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getDocStructSortFields(),
                            converter.getPresentationBuilder().getSolrFieldList());
            String escapedQuery = Pattern.quote(query);
            for (SolrDocument doc : docList) {
                Map<String, List<String>> fieldNames = SolrTools.getFieldValueMap(doc);
                for (String fieldName : fieldNames.keySet()) {
                    if (fieldNameMatches(fieldName, displayFields)) {
                        String fieldValue = fieldNames.get(fieldName).stream().collect(Collectors.joining(" "));
                        if (fieldValue.matches(AbstractSearchParser.getContainedWordRegex(AbstractSearchParser.getAutoSuggestRegex(escapedQuery)))) {
                            terms.addAll(
                                    converter.getSearchTerms(AbstractSearchParser.getAutoSuggestRegex(escapedQuery), fieldValue, getMotivation()));
                        }
                    }
                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    /**
     * @param query Solr query
     * @param pi Record identifier
     * @param firstHitIndex
     * @param hitsPerPage
     * @param request {@link HttpServletRequest}
     * @return {@link AnnotationResultList}
     */
    private AnnotationResultList searchAnnotations(String query, String pi, int firstHitIndex, int hitsPerPage, HttpServletRequest request) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +UGCTERMS:").append(query);

        AnnotationResultList results = new AnnotationResultList();
        try {
            List<SolrDocument> docList = new OpenAnnotationBuilder(urls).getAnnotationDocuments(query, request);
            if (firstHitIndex < docList.size()) {
                List<SolrDocument> filteredDocList = docList.subList(firstHitIndex, Math.min(firstHitIndex + hitsPerPage, docList.size()));
                for (SolrDocument doc : filteredDocList) {
                    results.add(converter.convertUGCToHit(AbstractSearchParser.getQueryRegex(query), doc));

                }
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return results;
    }

    /**
     * 
     * @param query Solr query
     * @param pi Record identifier
     * @param request {@link HttpServletRequest}
     * @return {@link SearchTermList}
     */
    private SearchTermList autoSuggestAnnotations(String query, String pi, HttpServletRequest request) {

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:UGC");
        queryBuilder.append(" +UGCTERMS:").append(query).append("*");

        SearchTermList terms = new SearchTermList();
        try {
            List<SolrDocument> docList = new OpenAnnotationBuilder(urls).getAnnotationDocuments(query, request);
            for (SolrDocument doc : docList) {
                terms.addAll(converter.getSearchTerms(AbstractSearchParser.getAutoSuggestRegex(query), doc,
                        Collections.singletonList(SolrConstants.UGCTERMS), getMotivation()));
            }
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error(e.toString(), e);
        }
        return terms;
    }

    /**
     * 
     * @param query Solr query
     * @param pi Record identifier
     * @param firstIndex Result offset
     * @param numHits Number of results to return
     * @return {@link AnnotationResultList}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private AnnotationResultList searchFulltext(String query, String pi, int firstIndex, int numHits)
            throws PresentationException, IndexUnreachableException {

        //replace search wildcards with word character regex and replace whitespaces with '|' to facilitate OR search
        String queryRegex = AbstractSearchParser.getQueryRegex(query);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(" +PI_TOPSTRUCT:").append(pi);
        queryBuilder.append(" +DOCTYPE:PAGE");
        queryBuilder.append(" +FULLTEXTAVAILABLE:true");
        queryBuilder.append(" +FULLTEXT:").append(query);

        AnnotationResultList results = new AnnotationResultList();

        SolrDocumentList docList = DataManager.getInstance()
                .getSearchIndex()
                .search(queryBuilder.toString(), SolrSearchIndex.MAX_HITS, getPageSortFields(), FULLTEXTFIELDLIST);
        for (SolrDocument doc : docList) {
            Path altoFile = getPath(pi, SolrTools.getSingleFieldStringValue(doc, SolrConstants.FILENAME_ALTO));
            Path fulltextFile = getPath(pi, SolrTools.getSingleFieldStringValue(doc, SolrConstants.FILENAME_FULLTEXT));
            Integer pageNo = SolrTools.getAsInt(doc.getFieldValue(SolrConstants.ORDER));
            converter.setPageNo(pageNo);
            try {
                if (altoFile != null && Files.exists(altoFile)) {
                    results.add(converter.getAnnotationsFromAlto(altoFile, queryRegex));
                } else if (fulltextFile != null && Files.exists(fulltextFile)) {
                    String text = new String(Files.readAllBytes(fulltextFile), StandardCharsets.UTF_8.name());
                    results.add(converter.getAnnotationsFromFulltext(text, pi, pageNo, queryRegex, results.getNumHits(), firstIndex, numHits));
                }
            } catch (IOException | JDOMException e) {
                logger.error("Error reading {}", fulltextFile, e);
            }
        }
        return results;
    }

    /**
     * Test if the given fieldName is included in the configuredFields or matches any of the contained wildcard fieldNames.
     * 
     * @param fieldName
     * @param configuredFields
     * @return a boolean
     */
    private static boolean fieldNameMatches(String fieldName, List<String> configuredFields) {
        for (String configuredField : configuredFields) {
            if (configuredField.contains("*") && !fieldName.endsWith("_UNTOKENIZED")) {
                String fieldRegex = AbstractSearchParser.getQueryRegex(configuredField);
                if (fieldName.matches(fieldRegex)) {
                    return true;
                }
            } else if (fieldName.equalsIgnoreCase(configuredField)) {
                return true;
            }
        }
        return false;
    }

    private static List<StringPair> getPageSortFields() {
        StringPair sortField = new StringPair(SolrConstants.ORDER, "asc");
        return Collections.singletonList(sortField);
    }

    private static List<StringPair> getDocStructSortFields() {
        StringPair sortField1 = new StringPair(SolrConstants.THUMBPAGENO, "asc");
        StringPair sortField2 = new StringPair(SolrConstants.ISANCHOR, "asc");
        StringPair sortField3 = new StringPair(SolrConstants.ISWORK, "asc");
        List<StringPair> pairs = new ArrayList<>();
        pairs.add(sortField1);
        pairs.add(sortField2);
        pairs.add(sortField3);
        return pairs;
    }

    private static List<String> getSearchFields() {
        List<String> configuredFields = DataManager.getInstance().getConfiguration().getIIIFMetadataFields();
        if (configuredFields.stream().anyMatch(field -> field.contains("*"))) {
            configuredFields = configuredFields.stream().filter(field -> !field.contains("*")).collect(Collectors.toList());
            configuredFields.add(SolrConstants.DEFAULT);
        }
        return configuredFields;
    }

    private int getFirstHitIndex(int pageNo) {
        return (pageNo - 1) * getHitsPerPage();
    }

    /**
     *
     * @param pi Record identifier
     * @param relativeFilePath File path relative to the data repositories root
     * @return Absolute path to the file
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private static Path getPath(String pi, String relativeFilePath) throws PresentationException, IndexUnreachableException {
        if (StringUtils.isBlank(relativeFilePath)) {
            return null;
        }

        return DataFileTools.getDataFilePath(pi, relativeFilePath);
    }

    /**
     * @param page
     * @return {@link URI}
     */
    private URI getURI(Integer page) {
        ApiPath path = urls.path(ApiUrls.RECORDS_RECORD, ApiUrls.RECORDS_MANIFEST_SEARCH).params(this.pi);
        if (StringUtils.isNotBlank(getQuery())) {
            path = path.query("q", getQuery());
        }
        if (StringUtils.isNotBlank(getMotivationAsString())) {
            path = path.query("motivation", getMotivationAsString());
        }
        if (StringUtils.isNotBlank(getDate())) {
            path = path.query("date", getDate());
        }
        if (StringUtils.isNotBlank(getUser())) {
            path = path.query("user", getUser());
        }
        if (page != null) {
            path = path.query("page", page);
        }
        return URI.create(path.build());
    }
}
