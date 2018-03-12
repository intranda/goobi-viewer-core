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
package de.intranda.digiverso.presentation.controller;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.comparators.ReverseComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue;
import de.intranda.digiverso.presentation.model.toc.metadata.MultiLanguageMetadataValue;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.Tag;

public final class SolrSearchIndex {

    private static final Logger logger = LoggerFactory.getLogger(SolrSearchIndex.class);

    private static final int MIN_SCHEMA_VERSION = 20170710;
    private static final String SCHEMA_VERSION_PREFIX = "goobi_viewer-";
    private static final String MULTILANGUAGE_FIELD_REGEX = "(\\w+)_LANG_(\\w{2,3})";
    private static final int MULTILANGUAGE_FIELD_NAME_GROUP = 1;
    private static final int MULTILANGUAGE_FIELD_LANGUAGE_GROUP = 2;
    public static final int MAX_HITS = 1000000;
    private static final int TIMEOUT_SO = 30000;
    private static final int TIMEOUT_CONNECTION = 30000;

    public boolean initialized = false;

    private SolrServer server;

    public SolrSearchIndex(SolrServer server) {
        if (server == null) {
            this.server = getNewHttpSolrServer();
        } else {
            this.server = server;
        }
    }

    /**
     * Checks whether the server's configured URL matches that in the config file. If not, a new server instance is created.
     */
    public void checkReloadNeeded() {
        if (server != null && server instanceof HttpSolrServer) {
            HttpSolrServer httpSolrServer = (HttpSolrServer) server;
            if (!DataManager.getInstance().getConfiguration().getSolrUrl().equals(httpSolrServer.getBaseURL())) {
                logger.info("Solr URL has changed, re-initializing SolrHelper...");
                httpSolrServer.shutdown();
                server = getNewHttpSolrServer();
            }
        }
    }

    public static HttpSolrServer getNewHttpSolrServer() {
        HttpSolrServer server = new HttpSolrServer(DataManager.getInstance().getConfiguration().getSolrUrl());
        server.setSoTimeout(TIMEOUT_SO); // socket read timeout
        server.setConnectionTimeout(TIMEOUT_CONNECTION);
        server.setDefaultMaxConnectionsPerHost(100);
        server.setMaxTotalConnections(100);
        server.setFollowRedirects(false); // defaults to false
        server.setAllowCompression(true);
        server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
        // server.setParser(new XMLResponseParser()); // binary parser is used by default
        server.setRequestWriter(new BinaryRequestWriter());

        return server;
    }

    /**
     * Main Solr search method.
     *
     * @param query {@link String}
     * @param first {@link Integer}
     * @param rows {@link Integer}
     * @param sortFields
     * @param facetFields
     * @param facetSort
     * @param fieldList If not null, only the fields in the list will be returned.
     * @param filterQueries
     * @param params Additional query parameters.
     * @return {@link QueryResponse}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return correct results
     * @should return correct number of rows
     * @should sort results correctly
     * @should facet results correctly
     * @should filter fields correctly
     */
    public QueryResponse search(String query, int first, int rows, List<StringPair> sortFields, List<String> facetFields, String facetSort,
            List<String> fieldList, List<String> filterQueries, Map<String, String> params) throws PresentationException, IndexUnreachableException {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setStart(first);
        solrQuery.setRows(rows);

        if (sortFields != null && !sortFields.isEmpty()) {
            for (int i = 0; i < sortFields.size(); ++i) {
                StringPair sortField = sortFields.get(i);
                if (StringUtils.isNotEmpty(sortField.getOne())) {
                    solrQuery.addSort(sortField.getOne(), "desc".equals(sortField.getTwo()) ? ORDER.desc : ORDER.asc);
                    // logger.trace("Added sorting field: {}", sortField);
                }
            }
        }
        if (facetFields != null && !facetFields.isEmpty()) {
            for (String facetField : facetFields) {
                // logger.trace("adding facet field: {}", sortField);
                if (StringUtils.isNotEmpty(facetField)) {
                    solrQuery.addFacetField(facetField);
                    // TODO only do this once, perhaps?
                    if (StringUtils.isNotEmpty(facetSort)) {
                        solrQuery.setFacetSort(facetSort);
                    }
                }
            }
            solrQuery.setFacetMinCount(1);
            solrQuery.setFacetLimit(-1); // no limit
        }
        if (fieldList != null && !fieldList.isEmpty()) {
            for (String field : fieldList) {
                // logger.trace("adding result field: " + field);
                if (StringUtils.isNotEmpty(field)) {
                    solrQuery.addField(field);
                }
            }
        }
        if (filterQueries != null && !filterQueries.isEmpty()) {
            for (String fq : filterQueries) {
                solrQuery.addFilterQuery(fq);
            }
        }
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                solrQuery.set(key, params.get(key));
                // logger.trace("&{}={}", key, params.get(key));
            }
        }

        try {
            // logger.trace("Solr query URL: {}", solrQuery.getQuery());
            // logger.debug("range: {} - {}", first, first + rows);
            // logger.debug("facetFields: " + facetFields);
            // logger.debug("fieldList: " + fieldList);
            QueryResponse resp = server.query(solrQuery);
            // logger.debug("found: " + resp.getResults().getNumFound());
            // logger.debug("fetched: {}", resp.getResults().size());

            return resp;
        } catch (SolrServerException e) {
            if (e.getMessage().startsWith("Server refused connection")) {
                logger.warn("Solr offline; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            } else if (e.getMessage().startsWith("IOException occured when talking to server") || e.getMessage().contains("Timeout")) {
                logger.warn("Solr communication timeout; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            }
            logger.error("Bad query: {}", solrQuery.getQuery());
            logger.error(e.getMessage(), e);
            throw new PresentationException(e.getMessage());
        } catch (RemoteSolrException e) {
            if (isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
                throw new PresentationException("Bad query.");
            }
            logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            throw new IndexUnreachableException(e.getMessage());
        }

        // SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class=\"highlight\">", "</span>");
        //
        // Highlighter highlighter = new Highlighter(formatter, queryScorer);
        // Fragmenter fragmenter = new SimpleFragmenter(200);
        // highlighter.setTextFragmenter(fragmenter);
        //
        // TokenStream tokenStream = new StandardAnalyzer().tokenStream(ConstantsRetrieval.FIELD_DOC_CONTENT, new StringReader(text));
        //
        // formattedText = highlighter.getBestFragments(tokenStream, text, 5, "...");
    }

    /**
     *
     * @param query {@link String}
     * @param first {@link Integer}
     * @param rows {@link Integer}
     * @param sortFields
     * @param facetFields
     * @param fieldList If not null, only the fields in the list will be returned.
     * @param filterQueries
     * @param params
     * @return {@link QueryResponse}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public QueryResponse search(String query, int first, int rows, List<StringPair> sortFields, List<String> facetFields, List<String> fieldList,
            List<String> filterQueries, Map<String, String> params) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, first, rows, sortFields, facetFields, null, fieldList, filterQueries, params);
    }

    /**
     *
     * @param query {@link String}
     * @param first {@link Integer}
     * @param rows {@link Integer}
     * @param sortFields
     * @param facetFields
     * @param fieldList If not null, only the fields in the list will be returned.
     * @return {@link QueryResponse}
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public QueryResponse search(String query, int first, int rows, List<StringPair> sortFields, List<String> facetFields, List<String> fieldList)
            throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, first, rows, sortFields, facetFields, fieldList, null, null);
    }

    /**
     *
     * @param query
     * @param rows
     * @param sortFields
     * @param fieldList If not null, only the fields in the list will be returned.
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public SolrDocumentList search(String query, int rows, List<StringPair> sortFields, List<String> fieldList)
            throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, 0, rows, sortFields, null, fieldList).getResults();
    }

    /**
     * Diese Methode führt eine Suche im Lucene durch.
     *
     * @param query
     * @return {@link Hits}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws SolrServerException
     *
     */
    public SolrDocumentList search(String query, List<String> fieldList) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, 0, MAX_HITS, null, null, fieldList).getResults();
    }

    /**
     * Diese Methode führt eine Suche im Lucene durch.
     *
     * @param query
     * @return {@link Hits}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws SolrServerException
     *
     */
    public SolrDocumentList search(String query) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, 0, MAX_HITS, null, null, null).getResults();
    }

    public long count(String query) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, 0, 0, null, null, null).getResults().getNumFound();
    }

    /**
     *
     * @param query
     * @param fieldList
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return correct doc
     */
    public SolrDocument getFirstDoc(String query, List<String> fieldList) throws PresentationException, IndexUnreachableException {
        logger.trace("getFirstDoc: {}", query);
        SolrDocumentList hits = search(query, 0, 1, null, null, fieldList).getResults();
        if (hits.getNumFound() > 0) {
            return hits.get(0);
        }

        return null;
    }

    /**
     *
     * @param iddoc
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should return correct doc
     */
    public SolrDocument getDocumentByIddoc(String iddoc) throws IndexUnreachableException, PresentationException {
        // logger.trace("getDocumentByIddoc: {}", iddoc);
        SolrDocument ret = null;
        SolrDocumentList hits =
                search(new StringBuilder(SolrConstants.IDDOC).append(':').append(iddoc).toString(), 0, 1, null, null, null).getResults();
        if (hits != null && hits.size() > 0) {
            ret = hits.get(0);
        }

        return ret;
    }

    /**
     * Returns a list of Tags created from the terms for the given field name. This method uses the slower doc search instead of term search, but can
     * be filtered with a query.
     *
     * @param fieldName
     * @param querySuffix
     * @return
     * @throws IndexUnreachableException
     */
    @SuppressWarnings("unchecked")
    public List<Tag> generateFilteredTagCloud(String fieldName, String querySuffix) throws IndexUnreachableException {
        List<Tag> tags = new ArrayList<>();

        String query = new StringBuilder(fieldName).append(":*").append(querySuffix).toString();
        logger.debug("generateFilteredTagCloud query: {}", query);
        // Pattern p = Pattern.compile("\\w+");
        Pattern p = Pattern.compile(Helper.REGEX_WORDS);
        Set<String> stopWords = DataManager.getInstance().getConfiguration().getStopwords();

        SolrQuery solrQuery = new SolrQuery(query);
        try {
            List<String> termlist = new ArrayList<>();
            Map<String, Long> frequencyMap = new HashMap<>();

            solrQuery.setRows(DataManager.getInstance().getConfiguration().getTagCloudSampleSize(fieldName));
            solrQuery.addField(fieldName);
            QueryResponse resp = server.query(solrQuery);
            logger.debug("query done");
            for (SolrDocument doc : resp.getResults()) {
                Collection<Object> values = doc.getFieldValues(fieldName);
                for (Object o : values) {
                    String terms = String.valueOf(o).toLowerCase();
                    String[] termsSplit = terms.split(" ");
                    for (String term : termsSplit) {
                        Matcher m = p.matcher(term);
                        if (m.find()) {
                            term = term.substring(m.start(), m.end());
                            if (term.length() > 2 && term.charAt(0) != 1 && !stopWords.contains(term)) {
                                if (!frequencyMap.containsKey(term)) {
                                    frequencyMap.put(term, 0L);
                                    termlist.add(term);
                                } else {
                                    frequencyMap.put(term, frequencyMap.get(term) + 1);
                                }
                            }
                        }
                    }
                }
            }

            // Cutoff
            float topTermCutoff = 0.02F;
            Collections.sort(termlist, new ReverseComparator(new TermWeightComparator(frequencyMap)));
            float topFreq = -1.0F;
            for (String term : termlist) {
                if (topFreq < 0.0F) {
                    // first term, capture the value
                    topFreq = frequencyMap.get(term); // count of the terms
                    tags.add(new Tag(frequencyMap.get(term), term, fieldName));
                } else {
                    // not the first term, compute the ratio and discard if below
                    // topTermCutoff score
                    float ratio = (float) frequencyMap.get(term) / topFreq;
                    if (ratio >= topTermCutoff) {
                        tags.add(new Tag(frequencyMap.get(term), term, fieldName));
                    } else {
                        break;
                    }
                }
            }
            logger.trace("done");
        } catch (SolrServerException e) {
            if (e.getMessage().startsWith("Server refused connection")) {
                logger.warn("Solr offline; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            } else if (e.getMessage().startsWith("IOException occured when talking to server") || e.getMessage().contains("Timeout")) {
                logger.warn("Solr communication timeout; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            }
            logger.error("Bad query: {}", solrQuery.getQuery());
            logger.error(e.getMessage());
        } catch (RemoteSolrException e) {
            if (isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
            } else {
                logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            }
        }

        return tags;
    }

    /**
     * Returns the value of the IDDOC field for the document with the given PI (or 0 if none found).
     *
     * @param identifier
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should retrieve correct IDDOC
     */
    public long getIddocFromIdentifier(String identifier) throws PresentationException, IndexUnreachableException {
        // logger.trace("getIddocFromIdentifier: {}", identifier);
        SolrDocumentList docs = search(new StringBuilder(SolrConstants.PI).append(':').append(identifier).toString(), 1, null,
                Collections.singletonList(SolrConstants.IDDOC));
        if (!docs.isEmpty()) {
            return Long.valueOf((String) docs.get(0).getFieldValue(SolrConstants.IDDOC));
        }
        return 0;
    }

    /**
     *
     * @param identifier
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should retrieve correct identifier
     */
    public String getIdentifierFromIddoc(long iddoc) throws PresentationException, IndexUnreachableException {
        logger.trace("getIdentifierFromIddoc: {}", iddoc);
        SolrQuery solrQuery = new SolrQuery(new StringBuilder(SolrConstants.IDDOC).append(":").append(iddoc).toString());
        solrQuery.setRows(1);
        try {
            QueryResponse resp = server.query(solrQuery);
            if (resp.getResults().getNumFound() > 0) {
                return (String) resp.getResults().get(0).getFieldValue(SolrConstants.PI);
            }
            return null;
        } catch (SolrServerException e) {
            if (e.getMessage().startsWith("Server refused connection")) {
                logger.warn("Solr offline; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            } else if (e.getMessage().startsWith("IOException occured when talking to server") || e.getMessage().contains("Timeout")) {
                logger.warn("Solr communication timeout; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            }
            logger.error("Bad query: {}", solrQuery.getQuery());
            logger.error(e.getMessage());
            throw new PresentationException(e.getMessage());
        } catch (RemoteSolrException e) {
            if (isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
                throw new PresentationException("Bad query.");
            }
            logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            throw new PresentationException("Search index unavailable.");
        }
    }

    /**
     * Returns the IDDOC of the logical document to which the given page belongs.
     *
     * @param pi
     * @param pageNo
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should retrieve correct IDDOC
     */
    public long getImageOwnerIddoc(String pi, int pageNo) throws IndexUnreachableException, PresentationException {
        logger.trace("getImageOwnerIddoc: {}:{}", pi, pageNo);
        String query = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(":")
                .append(pi)
                .append(" AND ")
                .append(SolrConstants.ORDER)
                .append(":")
                .append(pageNo)
                .append(" AND ")
                .append(SolrConstants.DOCTYPE)
                .append(":")
                .append(DocType.PAGE.name())
                .toString();
        logger.trace("query: {}", query);
        String luceneOwner = SolrConstants.IDDOC_OWNER;
        SolrDocument pageDoc = getFirstDoc(query, Collections.singletonList(luceneOwner));
        //            if (pageDoc == null) {
        //                query = query.replace(" AND DOCTYPE:PAGE", " AND FILENAME:*");
        //                luceneOwner = "IDDOC_IMAGEOWNER";
        //                pageDoc = getFirstDoc(query, Collections.singletonList(luceneOwner));
        //            }
        if (pageDoc != null) {
            String iddoc = (String) pageDoc.getFieldValue(luceneOwner);
            return Long.valueOf(iddoc);
        }

        return -1;
    }

    /**
     * Returns the IDDOC of the logical document to which the given LOGID belongs.
     *
     * @param pi
     * @param logId
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should retrieve correct IDDOC
     */
    public long getIddocByLogid(String pi, String logId) throws IndexUnreachableException, PresentationException {
        logger.trace("getIddocByLogid: {}:{}", pi, logId);
        String query = new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(":")
                .append(pi)
                .append(" AND ")
                .append(SolrConstants.LOGID)
                .append(":")
                .append(logId)
                .toString();
        SolrDocument doc = getFirstDoc(query, Collections.singletonList(SolrConstants.IDDOC));
        if (doc != null) {
            String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC);
            return Long.valueOf(iddoc);
        }

        return -1;
    }

    /**
     *
     * @param doc
     * @param field
     * @return
     */
    public static Object getSingleFieldValue(SolrDocument doc, String field) {
        //        if(field.equals("MD_TITLE")) {            
        //            field = field + "_LANG_DE";
        //        }
        Collection<Object> valueList = doc.getFieldValues(field);
        if (valueList != null && !valueList.isEmpty()) {
            return valueList.iterator().next();
        }

        return null;
    }

    /**
     *
     * @param doc
     * @param field
     * @return
     */
    public static String getSingleFieldStringValue(SolrDocument doc, String field) {
        return (String) getSingleFieldValue(doc, field);
    }

    /**
     * Returns a list with all (string) values for the given field name in the given SolrDocument.
     *
     * @param doc
     * @param fieldName
     * @return
     * @should return all values for the given field
     */
    public static List<String> getMetadataValues(SolrDocument doc, String fieldName) {
        if (doc != null) {
            Collection<Object> values = doc.getFieldValues(fieldName);
            if (values != null) {
                List<String> ret = new ArrayList<>(values.size());
                for (Object value : values) {
                    if (value instanceof String) {
                        ret.add((String) value);
                    } else {
                        ret.add(String.valueOf(value));
                    }
                }
                return ret;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Converts the given SolrDocument to a value map. IMAGEURN_OAI and PAGEURNS are not returned because they have no relevance in this application
     * and can get quite large.
     *
     * @param doc
     * @return
     * @should return all fields in the given doc except page urns
     */
    public static Map<String, List<String>> getFieldValueMap(SolrDocument doc) {
        Map<String, List<String>> ret = new HashMap<>();

        for (String fieldName : doc.getFieldNames()) {
            switch (fieldName) {
                case SolrConstants.IMAGEURN_OAI:
                    // case SolrConstants.ALTO:
                case "WORDCOORDS":
                case "PAGEURNS":
                case "ABBYYXML":
                    break;
                default:
                    List<String> values = getMetadataValues(doc, fieldName);
                    ret.put(fieldName, values);
                    break;
            }
        }

        return ret;
    }

    /**
     * Converts the given SolrDocument to a value map. IMAGEURN_OAI and PAGEURNS are not returned because they have no relevance in this application
     * and can get quite large.
     *
     * @param doc
     * @return
     * @should return all fields in the given doc except page urns
     */
    public static Map<String, List<IMetadataValue>> getMultiLanguageFieldValueMap(SolrDocument doc) {
        Map<String, List<IMetadataValue>> ret = new HashMap<>();

        for (String fieldName : doc.getFieldNames()) {
            switch (fieldName) {
                case SolrConstants.IMAGEURN_OAI:
                    // case SolrConstants.ALTO:
                case "WORDCOORDS":
                case "PAGEURNS":
                case "ABBYYXML":
                    break;
                default:
                    if (isLanguageCodedField(fieldName)) {
                        break;
                    } else {
                        Map<String, List<String>> mdValues = getMetadataValuesForLanguage(doc, fieldName);
                        List<IMetadataValue> values = getMultiLanguageMetadata(mdValues);
                        ret.put(fieldName, values);
                    }
            }
        }

        return ret;
    }

    /**
     * @param mdValues
     * @return
     */
    public static List<IMetadataValue> getMultiLanguageMetadata(Map<String, List<String>> mdValues) {
        List<IMetadataValue> values = new ArrayList<>();
        int numValues = mdValues.values().stream().mapToInt(list -> list.size()).max().orElse(0);
        for (int i = 0; i < numValues; i++) {
            MultiLanguageMetadataValue value = new MultiLanguageMetadataValue();
            for (String language : mdValues.keySet()) {
                List<String> stringValues = mdValues.get(language);
                if (i < stringValues.size()) {
                    value.setValue(stringValues.get(i), language);
                }
            }
            values.add(value);
        }
        return values;
    }

    /**
     * @param fieldName
     * @return
     */
    private static String getLanguage(String fieldName) {
        if (isLanguageCodedField(fieldName)) {
            return Pattern.compile(MULTILANGUAGE_FIELD_REGEX).matcher(fieldName).group(MULTILANGUAGE_FIELD_LANGUAGE_GROUP);
        } else {
            return "";
        }
    }

    /**
     * @param fieldName
     * @return
     */
    private static String getBaseFieldName(String fieldName) {
        if (isLanguageCodedField(fieldName)) {
            return Pattern.compile(MULTILANGUAGE_FIELD_REGEX).matcher(fieldName).group(MULTILANGUAGE_FIELD_NAME_GROUP);
        } else {
            return fieldName;
        }
    }

    /**
     * @param fieldName
     * @return
     */
    private static boolean isLanguageCodedField(String fieldName) {
        return StringUtils.isNotBlank(fieldName) && fieldName.matches(MULTILANGUAGE_FIELD_REGEX);
    }

    /**
     * Returns the number of hits for the given query without actually returning any documents.
     *
     * @param query
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public long getHitCount(String query) throws IndexUnreachableException, PresentationException {
        SolrDocumentList result = search(query, 0, null, Collections.singletonList(SolrConstants.IDDOC));
        return result.getNumFound();
    }

    /**
     *
     * @param pi
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String findDataRepository(String pi) throws PresentationException, IndexUnreachableException {
        logger.trace("findDataRepository: {}", pi);
        if (StringUtils.isEmpty(pi)) {
            throw new IllegalArgumentException("pi may not be null or empty");
        }
        SolrQuery solrQuery = new SolrQuery(new StringBuilder(SolrConstants.PI).append(":").append(pi).toString());
        solrQuery.setRows(1);
        solrQuery.setFields(SolrConstants.DATAREPOSITORY);

        try {
            QueryResponse resp = server.query(solrQuery);
            if (!resp.getResults().isEmpty()) {
                return (String) resp.getResults().get(0).getFieldValue(SolrConstants.DATAREPOSITORY);
            }
        } catch (SolrServerException e) {
            if (e.getMessage().startsWith("Server refused connection")) {
                logger.warn("Solr offline; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            } else if (e.getMessage().startsWith("IOException occured when talking to server") || e.getMessage().contains("Timeout")) {
                logger.warn("Solr communication timeout; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            }
            throw new PresentationException(e.getMessage());
        } catch (RemoteSolrException e) {
            if (isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
                throw new PresentationException("Bad query.");
            }
            logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            throw new IndexUnreachableException(e.getMessage());
        }

        return null;
    }

    private static Document getSolrSchemaDocument() {
        StringReader sr = null;
        try {
            Helper.getWebContentGET(
                    DataManager.getInstance().getConfiguration().getSolrUrl() + "/admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml");
            String responseBody = Helper.getWebContentGET(
                    DataManager.getInstance().getConfiguration().getSolrUrl() + "/admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml");
            sr = new StringReader(responseBody);
            return new SAXBuilder().build(sr);
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (JDOMException e) {
            logger.error(e.getMessage(), e);
        } catch (HTTPException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (sr != null) {
                sr.close();
            }
        }

        return null;
    }

    public static String[] checkSolrSchemaName() {
        String[] ret = { "200", "" };
        Document doc = getSolrSchemaDocument();
        if (doc != null) {
            Element eleRoot = doc.getRootElement();
            if (eleRoot != null) {
                String schemaName = eleRoot.getAttributeValue("name");
                if (StringUtils.isNotEmpty(schemaName)) {
                    try {
                        if (schemaName.length() > SCHEMA_VERSION_PREFIX.length()
                                && Integer.parseInt(schemaName.substring(SCHEMA_VERSION_PREFIX.length())) >= MIN_SCHEMA_VERSION) {
                            String msg = "Solr schema is up to date: " + SCHEMA_VERSION_PREFIX + MIN_SCHEMA_VERSION;
                            logger.info(msg);
                            ret[0] = "200";
                            ret[1] = msg;
                            return ret;
                        }
                    } catch (NumberFormatException e) {
                        logger.error("Schema version must contain a number.");
                    }
                    String msg = "Solr schema is not up to date; required: " + SCHEMA_VERSION_PREFIX + MIN_SCHEMA_VERSION + ", found: " + schemaName;
                    logger.error(msg);
                    ret[0] = "417";
                    ret[1] = msg;
                }
            }
        } else {
            String msg = "Could not read the Solr schema name.";
            logger.error(msg);
            ret[0] = "500";
            ret[1] = msg;
        }

        return ret;
    }

    private class TermWeightComparator implements Comparator<String> {

        private Map<String, Long> weightMap;

        public TermWeightComparator(Map<String, Long> weightMap) {
            this.weightMap = weightMap;
        }

        @Override
        public int compare(String term1, String term2) {
            if (!weightMap.containsKey(term1) || !weightMap.containsKey(term2)) {
                return 0;
            }

            return Long.compare(weightMap.get(term1), weightMap.get(term2));
        }
    }

    /**
     * Returns facets for the given facet field list. No actual docs are returned since they aren't necessary.
     *
     * @param query The query to use.
     * @param facetFields List of facet fields.
     * @param facetMinCount
     * @param getFieldStatistics If true, field statistics will be generated for every facet field.
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should generate facets correctly
     * @should generate field statistics for every facet field if requested
     * @should not return any docs
     */
    public QueryResponse searchFacetsAndStatistics(String query, List<String> facetFields, int facetMinCount, boolean getFieldStatistics)
            throws PresentationException, IndexUnreachableException {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setStart(0);
        solrQuery.setRows(0);

        for (String field : facetFields) {
            solrQuery.addField(field);
            solrQuery.addFacetField(field);
            solrQuery.setFacetSort(field);
            if (getFieldStatistics) {
                solrQuery.setGetFieldStatistics(field);
            }
        }
        solrQuery.setFacetMinCount(facetMinCount);
        solrQuery.setFacetLimit(-1); // no limit
        try {
            QueryResponse resp = server.query(solrQuery);
            //            for (SolrDocument doc : resp.getResults()) {
            //                logger.debug(doc.getFieldNames().toString());
            //            }
            return resp;
        } catch (SolrServerException e) {
            if (e.getMessage().startsWith("Server refused connection")) {
                logger.warn("Solr offline; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            } else if (e.getMessage().startsWith("IOException occured when talking to server") || e.getMessage().contains("Timeout")) {
                logger.warn("Solr communication timeout; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            }
            logger.error("Bad query: {}", solrQuery.getQuery());
            logger.error(e.getMessage());
            throw new PresentationException(e.getMessage());
        } catch (RemoteSolrException e) {
            if (isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
                throw new PresentationException("Bad query.");
            }
            logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            throw new IndexUnreachableException(e.getMessage());
        }
    }

    /**
     * Returns the comma-separated sorting fields in <code>solrSortFields</code> as a List<StringPair>.
     *
     * @param solrSortFields
     * @param splitFieldsBy String by which the individual field configurations are split
     * @param splitNameOrderBy String by which the field name and sorting order are split
     * @return
     * @should split fields correctly
     * @should split single field correctly
     * @should throw IllegalArgumentException if solrSortFields is null
     * @should throw IllegalArgumentException if splitFieldsBy is null
     * @should throw IllegalArgumentException if splitNameOrderBy is null
     */
    public static List<StringPair> getSolrSortFieldsAsList(String solrSortFields, String splitFieldsBy, String splitNameOrderBy) {
        if (solrSortFields == null) {
            throw new IllegalArgumentException("solrSortFields may not be null");
        }
        if (splitFieldsBy == null) {
            throw new IllegalArgumentException("splitFieldsBy may not be null");
        }
        if (splitNameOrderBy == null) {
            throw new IllegalArgumentException("splitNameOrderBy may not be null");
        }

        if (StringUtils.isNotEmpty(solrSortFields)) {
            String[] solrSortFieldsSplit = solrSortFields.split(splitFieldsBy);
            List<StringPair> ret = new ArrayList<>(solrSortFieldsSplit.length);
            for (String fieldConfig : solrSortFieldsSplit) {
                if (StringUtils.isNotBlank(fieldConfig)) {
                    String[] fieldConfigSplit = fieldConfig.split(splitNameOrderBy);
                    switch (fieldConfigSplit.length) {
                        case 1:
                            ret.add(new StringPair(fieldConfigSplit[0].trim(), "asc"));
                            break;
                        case 2:
                            ret.add(new StringPair(fieldConfigSplit[0].trim(), fieldConfigSplit[1].trim()));
                            break;
                        default:
                            logger.warn("Cannot parse sorting field configuration: {}", fieldConfig);
                    }

                }
            }
            return ret;
        }

        return Collections.emptyList();
    }

    /**
     * Parses a Solr-Field value in order to return it as String
     * 
     * @param fieldValue
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String getAsString(Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        if (fieldValue instanceof String) {
            return (String) fieldValue;
        } else if (fieldValue instanceof List) {
            StringBuilder sb = new StringBuilder();
            List<Object> list = (List<Object>) fieldValue;
            for (Object object : list) {
                sb.append("\n").append(getAsString(object));
            }
            return sb.toString().trim();
        } else {
            return fieldValue.toString();
        }
    }

    @SuppressWarnings("unchecked")
    public static Integer getAsInt(Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        if (fieldValue instanceof Integer) {
            return (Integer) fieldValue;
        } else {
            try {
                return Integer.parseInt(fieldValue.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     *
     * @param e
     * @return
     */
    private static boolean isQuerySyntaxError(Exception e) {
        return e.getMessage() != null && (e.getMessage().startsWith("org.apache.solr.search.SyntaxError")
                || e.getMessage().startsWith("Invalid Number") || e.getMessage().startsWith("undefined field"));
    }

    /**
     * @return
     * @throws IOException
     * @throws SolrServerException
     */
    public List<String> getAllFieldNames() throws SolrServerException, IOException {
        LukeRequest lukeRequest = new LukeRequest();
        lukeRequest.setNumTerms(0);
        LukeResponse lukeResponse = lukeRequest.process(server);
        Map<String, FieldInfo> fieldInfoMap = lukeResponse.getFieldInfo();

        List<String> list = new ArrayList<>();
        for (String name : fieldInfoMap.keySet()) {
            FieldInfo info = fieldInfoMap.get(name);
            if (info != null && info.getType() != null && info.getType().toLowerCase().contains("string")
                    || info.getType().toLowerCase().contains("text") || info.getType().toLowerCase().contains("tlong")) {
                list.add(name);
            }
        }

        return list;
    }

    /**
     * @param doc The document containing the metadata
     * @param key the metadata key without the '_LANG_...' suffix
     * @return A map with keys for each language and lists of all found metadata values for this language. Metadata that match the given key but have
     *         no language information are listed as language {@code _DEFAULT}
     */
    public static Map<String, List<String>> getMetadataValuesForLanguage(SolrDocument doc, String key) {
        Map<String, List<String>> map = new HashMap<>();
        if (doc != null) {
            List<String> fieldNames = doc.getFieldNames().stream().filter(field -> field.startsWith(key)).collect(Collectors.toList());
            for (String languageField : fieldNames) {
                String locale = null;
                if (languageField.startsWith(key + "_LANG_")) {
                    locale = languageField.substring(languageField.lastIndexOf("_LANG_") + 6).toLowerCase();
                } else {
                    locale = MultiLanguageMetadataValue.DEFAULT_LANGUAGE;
                }
                Collection<Object> languageValues = doc.getFieldValues(languageField);
                if (languageValues != null) {
                    List<String> values = languageValues.stream().map(value -> String.valueOf(value)).collect(Collectors.toList());
                    map.put(locale, values);
                }
            }
        }
        return map;
    }
}
