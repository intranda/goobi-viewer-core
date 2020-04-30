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
package io.goobi.viewer.controller;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.comparators.ReverseComparator;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.solr.common.luke.FieldFlag;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.Tag;

/**
 * <p>
 * SolrSearchIndex class.
 * </p>
 */
public final class SolrSearchIndex {

    private static final Logger logger = LoggerFactory.getLogger(SolrSearchIndex.class);

    private static final int MIN_SCHEMA_VERSION = 20190924;
    private static final String SCHEMA_VERSION_PREFIX = "goobi_viewer-";
    private static final String MULTILANGUAGE_FIELD_REGEX = "(\\w+)_LANG_(\\w{2,3})";
    private static final int MULTILANGUAGE_FIELD_NAME_GROUP = 1;
    private static final int MULTILANGUAGE_FIELD_LANGUAGE_GROUP = 2;
    /** Constant <code>MAX_HITS=1000000</code> */
    public static final int MAX_HITS = 1000000;
    private static final int TIMEOUT_SO = 30000;
    private static final int TIMEOUT_CONNECTION = 30000;

    public boolean initialized = false;

    /** Application-scoped map containing already looked up data repository names of records. */
    Map<String, String> dataRepositoryNames = new HashMap<>();

    private SolrServer server;

    /**
     * <p>
     * Constructor for SolrSearchIndex.
     * </p>
     *
     * @param server a {@link org.apache.solr.client.solrj.SolrServer} object.
     */
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

    /**
     * <p>
     * getNewHttpSolrServer.
     * </p>
     *
     * @return a {@link org.apache.solr.client.solrj.impl.HttpSolrServer} object.
     */
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
     * <p>
     * testQuery.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     */
    public QueryResponse testQuery(String query) throws SolrServerException {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setStart(0);
        solrQuery.setRows(0);

        return server.query(solrQuery);
    }

    /**
     * Main Solr search method.
     *
     * @param query {@link java.lang.String}
     * @param first {@link java.lang.Integer}
     * @param rows {@link java.lang.Integer}
     * @param sortFields a {@link java.util.List} object.
     * @param facetFields a {@link java.util.List} object.
     * @param facetSort a {@link java.lang.String} object.
     * @param fieldList If not null, only the fields in the list will be returned.
     * @param filterQueries a {@link java.util.List} object.
     * @param params Additional query parameters.
     * @return {@link org.apache.solr.client.solrj.response.QueryResponse}
     * @should return correct results
     * @should return correct number of rows
     * @should sort results correctly
     * @should facet results correctly
     * @should filter fields correctly
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
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
                logger.trace("adding filter query: {}", fq);
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
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery(), e);
                throw new PresentationException("Bad query.");
            }
            logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            logger.error(e.toString(), e);
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
     * <p>
     * search.
     * </p>
     *
     * @param query {@link java.lang.String}
     * @param first {@link java.lang.Integer}
     * @param rows {@link java.lang.Integer}
     * @param sortFields a {@link java.util.List} object.
     * @param facetFields a {@link java.util.List} object.
     * @param fieldList If not null, only the fields in the list will be returned.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @return {@link org.apache.solr.client.solrj.response.QueryResponse}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public QueryResponse search(String query, int first, int rows, List<StringPair> sortFields, List<String> facetFields, List<String> fieldList,
            List<String> filterQueries, Map<String, String> params) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, first, rows, sortFields, facetFields, null, fieldList, filterQueries, params);
    }

    /**
     * <p>
     * search.
     * </p>
     *
     * @param query {@link java.lang.String}
     * @param first {@link java.lang.Integer}
     * @param rows {@link java.lang.Integer}
     * @param sortFields a {@link java.util.List} object.
     * @param facetFields a {@link java.util.List} object.
     * @param fieldList If not null, only the fields in the list will be returned.
     * @return {@link org.apache.solr.client.solrj.response.QueryResponse}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public QueryResponse search(String query, int first, int rows, List<StringPair> sortFields, List<String> facetFields, List<String> fieldList)
            throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, first, rows, sortFields, facetFields, fieldList, null, null);
    }

    /**
     * <p>
     * search.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @param rows a int.
     * @param sortFields a {@link java.util.List} object.
     * @param fieldList If not null, only the fields in the list will be returned.
     * @return a {@link org.apache.solr.common.SolrDocumentList} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocumentList search(String query, int rows, List<StringPair> sortFields, List<String> fieldList)
            throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, 0, rows, sortFields, null, fieldList).getResults();
    }

    /**
     * Diese Methode führt eine Suche im Lucene durch.
     *
     * @param query a {@link java.lang.String} object.
     * @return {@link Hits}
     * @param fieldList a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocumentList search(String query, List<String> fieldList) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, 0, MAX_HITS, null, null, fieldList).getResults();
    }

    /**
     * Diese Methode führt eine Suche im Lucene durch.
     *
     * @param query a {@link java.lang.String} object.
     * @return {@link Hits}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocumentList search(String query) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, 0, MAX_HITS, null, null, null).getResults();
    }

    /**
     * <p>
     * count.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public long count(String query) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query);
        return search(query, 0, 0, null, null, null).getResults().getNumFound();
    }

    /**
     * Retrieves the first document found by the given query
     *
     * @param query a {@link java.lang.String} object.
     * @param fieldList a {@link java.util.List} object.
     * @return The first hit returned by the query
     * @should return correct doc
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocument getFirstDoc(String query, List<String> fieldList) throws PresentationException, IndexUnreachableException {
        return getFirstDoc(query, fieldList, null);
    }

    /**
     *
     * Retrieves the first document found by the given query
     *
     * @param query The query to search
     * @param fieldList The fields retrieved
     * @param sortFields Sorting - the first volume according to this sorting is returned
     * @return The first hit returned by the query
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocument getFirstDoc(String query, List<String> fieldList, List<StringPair> sortFields)
            throws PresentationException, IndexUnreachableException {
        logger.trace("getFirstDoc: {}", query);
        SolrDocumentList hits = search(query, 0, 1, sortFields, null, fieldList).getResults();
        if (hits.getNumFound() > 0) {
            return hits.get(0);
        }

        return null;
    }

    /**
     * Returns all SolrDocuments matching the given query. If no documents were found, null is returned
     *
     * @param query a {@link java.lang.String} object.
     * @param fieldList a {@link java.util.List} object.
     * @should return SolrDocumentList containing all hits, or null if no hits are found
     * @return a {@link org.apache.solr.common.SolrDocumentList} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocumentList getDocs(String query, List<String> fieldList) throws PresentationException, IndexUnreachableException {
        logger.trace("getDocs: {}", query);
        SolrDocumentList hits = search(query, fieldList);
        if (hits.getNumFound() > 0) {
            return hits;
        }

        return null;
    }

    /**
     * <p>
     * getDocumentByIddoc.
     * </p>
     *
     * @param iddoc a {@link java.lang.String} object.
     * @should return correct doc
     * @return a {@link org.apache.solr.common.SolrDocument} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
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
     * <p>
     * getDocumentByPI.
     * </p>
     *
     * @should return correct doc
     * @param pi a {@link java.lang.String} object.
     * @return a {@link org.apache.solr.common.SolrDocument} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public SolrDocument getDocumentByPI(String pi) throws IndexUnreachableException, PresentationException {
        // logger.trace("getDocumentByIddoc: {}", iddoc);
        SolrDocument ret = null;
        SolrDocumentList hits = search(new StringBuilder(SolrConstants.PI).append(':').append(pi).toString(), 0, 1, null, null, null).getResults();
        if (hits != null && hits.size() > 0) {
            ret = hits.get(0);
        }

        return ret;
    }

    /**
     * Returns a list of Tags created from the terms for the given field name. This method uses the slower doc search instead of term search, but can
     * be filtered with a query.
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param querySuffix a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<Tag> generateFilteredTagCloud(String fieldName, String querySuffix) throws IndexUnreachableException {
        List<Tag> tags = new ArrayList<>();

        String query = new StringBuilder(fieldName).append(":*").append(querySuffix).toString();
        logger.trace("generateFilteredTagCloud query: {}", query);
        // Pattern p = Pattern.compile("\\w+");
        Pattern p = Pattern.compile(StringTools.REGEX_WORDS);
        Set<String> stopWords = DataManager.getInstance().getConfiguration().getStopwords();

        SolrQuery solrQuery = new SolrQuery(query);
        try {
            List<String> termlist = new ArrayList<>();
            Map<String, Long> frequencyMap = new HashMap<>();

            solrQuery.setRows(DataManager.getInstance().getConfiguration().getTagCloudSampleSize(fieldName));
            solrQuery.addField(fieldName);
            QueryResponse resp = server.query(solrQuery);
            logger.trace("query done");
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
            Collections.sort(termlist, new ReverseComparator<>(new TermWeightComparator(frequencyMap)));
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
     * @param identifier a {@link java.lang.String} object.
     * @should retrieve correct IDDOC
     * @return a long.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
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
     * <p>
     * getIdentifierFromIddoc.
     * </p>
     *
     * @should retrieve correct identifier
     * @param iddoc a long.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
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
     * @param pi a {@link java.lang.String} object.
     * @param pageNo a int.
     * @should retrieve correct IDDOC
     * @return a long.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
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
     * @param pi a {@link java.lang.String} object.
     * @param logId a {@link java.lang.String} object.
     * @should retrieve correct IDDOC
     * @return a long.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
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
     * <p>
     * getSingleFieldValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
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
     * <p>
     * getSingleFieldStringValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @should return value as string correctly
     * @should not return null as string if value is null
     * @return a {@link java.lang.String} object.
     */
    public static String getSingleFieldStringValue(SolrDocument doc, String field) {
        Object val = getSingleFieldValue(doc, field);
        return val != null ? String.valueOf(val) : null;
    }

    /**
     * <p>
     * getSingleFieldIntegerValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.Integer} object.
     */
    public static Integer getSingleFieldIntegerValue(SolrDocument doc, String field) {
        Object val = getSingleFieldValue(doc, field);
        return getAsInt(val);
    }

    /**
     * <p>
     * getSingleFieldBooleanValue.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.Boolean} object.
     */
    public static Boolean getSingleFieldBooleanValue(SolrDocument doc, String field) {
        Object val = getSingleFieldValue(doc, field);
        if (val == null) {
            return null;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.valueOf((String) val);
        } else {
            return null;
        }
    }

    /**
     * Returns a list with all (string) values for the given field name in the given SolrDocument.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param fieldName a {@link java.lang.String} object.
     * @should return all values for the given field
     * @return a {@link java.util.List} object.
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
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @should return all fields in the given doc except page urns
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<String>> getFieldValueMap(SolrDocument doc) {
        Map<String, List<String>> ret = new HashMap<>();

        for (String fieldName : doc.getFieldNames()) {
            switch (fieldName) {
                case SolrConstants.IMAGEURN_OAI:
                case "PAGEURNS":
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
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @should return all fields in the given doc except page urns
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, List<IMetadataValue>> getMultiLanguageFieldValueMap(SolrDocument doc) {
        Map<String, List<IMetadataValue>> ret = new HashMap<>();

        for (String fieldName : doc.getFieldNames()) {
            switch (fieldName) {
                case SolrConstants.IMAGEURN_OAI:
                case "PAGEURNS":
                    break;
                default:
                    if (isLanguageCodedField(fieldName)) {
                        break;
                    }
                    Map<String, List<String>> mdValues = getMetadataValuesForLanguage(doc, fieldName);
                    List<IMetadataValue> values = getMultiLanguageMetadata(mdValues);
                    ret.put(fieldName, values);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getMultiLanguageMetadata.
     * </p>
     *
     * @param mdValues a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
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
        }
        return "";
    }

    /**
     * @param fieldName
     * @return
     */
    private static String getBaseFieldName(String fieldName) {
        if (isLanguageCodedField(fieldName)) {
            return Pattern.compile(MULTILANGUAGE_FIELD_REGEX).matcher(fieldName).group(MULTILANGUAGE_FIELD_NAME_GROUP);
        }
        return fieldName;
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
     * @param query a {@link java.lang.String} object.
     * @return a long.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public long getHitCount(String query) throws IndexUnreachableException, PresentationException {
        SolrDocumentList result = search(query, 0, null, Collections.singletonList(SolrConstants.IDDOC));
        return result.getNumFound();
    }

    /**
     * Retrieves the repository name for the record with the given PI and persists it in a map. This method is package private to discourage clients
     * from constructing data file paths manually instead of using Helper methods.
     * 
     * @param pi
     * @return Data repository name for the record with the given identifier; null if not in a repository
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return value from map if available
     */
    String findDataRepositoryName(String pi) throws PresentationException, IndexUnreachableException {
        if (!dataRepositoryNames.containsKey(pi)) {
            String dataRepositoryName = findDataRepository(pi);
            updateDataRepositoryNames(pi, dataRepositoryName);
        }

        return dataRepositoryNames.get(pi);
    }

    /**
     * 
     * @param pi
     * @param dataRepositoryName
     * @should update value correctly
     */
    public void updateDataRepositoryNames(String pi, String dataRepositoryName) {
        dataRepositoryNames.put(pi, dataRepositoryName);
    }

    /**
     * Retrieves the repository name for the record with the given PI from the index.
     * 
     * @param pi
     * @return Data repository name for the record with the given identifier; null if not in a repository
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private String findDataRepository(String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("findDataRepository: {}", pi);
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
            logger.error(e.getMessage(), e);
            throw new PresentationException(e.getMessage() + ": " + solrQuery.getQuery());
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
            NetTools.getWebContentGET(
                    DataManager.getInstance().getConfiguration().getSolrUrl() + "/admin/file/?contentType=text/xml;charset=utf-8&file=schema.xml");
            String responseBody = NetTools.getWebContentGET(
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

    /**
     * <p>
     * checkSolrSchemaName.
     * </p>
     *
     * @return an array of {@link java.lang.String} objects.
     */
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
     * @param facetMinCount a int.
     * @param getFieldStatistics If true, field statistics will be generated for every facet field.
     * @should generate facets correctly
     * @should generate field statistics for every facet field if requested
     * @should not return any docs
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public QueryResponse searchFacetsAndStatistics(String query, List<String> facetFields, int facetMinCount, boolean getFieldStatistics)
            throws PresentationException, IndexUnreachableException {
        return searchFacetsAndStatistics(query, facetFields, facetMinCount, null, getFieldStatistics);
    }

    /**
     * Returns facets for the given facet field list. No actual docs are returned since they aren't necessary.
     *
     * @param query The query to use.
     * @param facetFields List of facet fields.
     * @param facetMinCount a int.
     * @param facetPrefix The facet field value must start with these characters. Ignored if null or blank
     * @param getFieldStatistics If true, field statistics will be generated for every facet field.
     * @should generate facets correctly
     * @should generate field statistics for every facet field if requested
     * @should not return any docs
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public QueryResponse searchFacetsAndStatistics(String query, List<String> facetFields, int facetMinCount, String facetPrefix,
            boolean getFieldStatistics) throws PresentationException, IndexUnreachableException {
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
        if (StringUtils.isNotBlank(facetPrefix)) {
            solrQuery.setFacetPrefix(facetPrefix);
        }
        solrQuery.setFacetLimit(-1); // no limit
        try {
            QueryResponse resp = server.query(solrQuery);
            return resp;
        } catch (SolrServerException e) {
            if (e.getMessage().startsWith("Server refused connection")) {
                logger.warn("Solr offline; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            } else if (e.getMessage().startsWith("IOException occured when talking to server") || e.getMessage().contains("Timeout")) {
                logger.warn("Solr communication timeout; Query: {}", solrQuery.getQuery());
                throw new IndexUnreachableException(e.getMessage());
            }
            logger.error("Could not execute query: {}", solrQuery.getQuery());
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
     * @param solrSortFields a {@link java.lang.String} object.
     * @param splitFieldsBy String by which the individual field configurations are split
     * @param splitNameOrderBy String by which the field name and sorting order are split
     * @should split fields correctly
     * @should split single field correctly
     * @should throw IllegalArgumentException if solrSortFields is null
     * @should throw IllegalArgumentException if splitFieldsBy is null
     * @should throw IllegalArgumentException if splitNameOrderBy is null
     * @return a {@link java.util.List} object.
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
     * @param fieldValue a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
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

    /**
     * <p>
     * getAsInt.
     * </p>
     *
     * @param fieldValue a {@link java.lang.Object} object.
     * @return a {@link java.lang.Integer} object.
     */
    public static Integer getAsInt(Object fieldValue) {
        if (fieldValue == null) {
            return null;
        }
        if (fieldValue instanceof Integer) {
            return (Integer) fieldValue;
        }
        try {
            return Integer.parseInt(fieldValue.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * <p>
     * isQuerySyntaxError.
     * </p>
     *
     * @param e a {@link java.lang.Exception} object.
     * @return a boolean.
     */
    public static boolean isQuerySyntaxError(Exception e) {
        return e.getMessage() != null && (e.getMessage().startsWith("org.apache.solr.search.SyntaxError")
                || e.getMessage().startsWith("Invalid Number") || e.getMessage().startsWith("undefined field"));
    }

    /**
     * <p>
     * getAllFieldNames.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     * @throws java.io.IOException if any.
     */
    public List<String> getAllFieldNames() throws SolrServerException, IOException {
        LukeRequest lukeRequest = new LukeRequest();
        lukeRequest.setNumTerms(0);
        LukeResponse lukeResponse = lukeRequest.process(server);
        Map<String, FieldInfo> fieldInfoMap = lukeResponse.getFieldInfo();

        List<String> list = new ArrayList<>();
        for (String name : fieldInfoMap.keySet()) {
            FieldInfo info = fieldInfoMap.get(name);
            if (info != null && info.getType() != null && (info.getType().toLowerCase().contains("string")
                    || info.getType().toLowerCase().contains("text") || info.getType().toLowerCase().contains("tlong"))) {
                list.add(name);
            }
        }

        return list;
    }

    /**
     * <p>
     * getAllSortFieldNames.
     * </p>
     *
     * @return a list of all SOLR fields starting with "SORT_".
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     * @throws java.io.IOException if any.
     */
    public List<String> getAllSortFieldNames() throws SolrServerException, IOException {
        LukeRequest lukeRequest = new LukeRequest();
        lukeRequest.setNumTerms(0);
        LukeResponse lukeResponse = lukeRequest.process(server);
        Map<String, FieldInfo> fieldInfoMap = lukeResponse.getFieldInfo();

        List<String> list = new ArrayList<>();
        for (String name : fieldInfoMap.keySet()) {
            if (name.startsWith("SORT_")) {
                list.add(name);
            }
        }

        return list;
    }

    /**
     * 
     * @return A list of all SOLR fields without the multivalues flag
     * @throws SolrServerException
     * @throws IOException
     */
    public List<String> getAllGroupFieldNames() throws SolrServerException, IOException {
        LukeRequest lukeRequest = new LukeRequest();
        lukeRequest.setNumTerms(0);
        LukeResponse lukeResponse = lukeRequest.process(server);
        Map<String, FieldInfo> fieldInfoMap = lukeResponse.getFieldInfo();

        List<String> keys = new ArrayList<>(fieldInfoMap.keySet());
        Collections.sort(keys);
        List<String> list = new ArrayList<>();
        for (String name : keys) {
            FieldInfo info = fieldInfoMap.get(name);
            EnumSet<FieldFlag> flags = FieldInfo.parseFlags(info.getSchema());
            if (!flags.contains(FieldFlag.MULTI_VALUED)) {
                if (info.getDocs() > 0
                        && (flags.contains(FieldFlag.DOC_VALUES) || name.equals(SolrConstants.DOCSTRCT) || name.equals(SolrConstants.PI_ANCHOR))) {
                    list.add(name);
                }
            }
        }

        return list;
    }

    /**
     * <p>
     * getMetadataValuesForLanguage.
     * </p>
     *
     * @param doc The document containing the metadata
     * @param key the metadata key without the '_LANG_...' suffix
     * @return A map with keys for each language and lists of all found metadata values for this language. Metadata that match the given key but have
     *         no language information are listed as language {@code _DEFAULT}
     */
    public static Map<String, List<String>> getMetadataValuesForLanguage(SolrDocument doc, String key) {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key may not be null");
        }

        List<String> fieldNames =
                doc.getFieldNames().stream().filter(field -> field.equals(key) || field.startsWith(key + "_LANG_")).collect(Collectors.toList());
        Map<String, List<String>> map = new HashMap<>(fieldNames.size());
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

        return map;
    }

    /**
     * <p>
     * getMetadataValuesForLanguage.
     * </p>
     *
     * @param doc The document containing the metadata
     * @param key the metadata key without the '_LANG_...' suffix
     * @return A map with keys for each language and lists of all found metadata values for this language. Metadata that match the given key but have
     *         no language information are listed as language {@code _DEFAULT}
     */
    public static Map<String, List<String>> getMetadataValuesForLanguage(StructElement doc, String key) {
        Map<String, List<String>> map = new HashMap<>();
        if (doc != null) {
            List<String> fieldNames = doc.getMetadataFields()
                    .keySet()
                    .stream()
                    .filter(field -> field.equals(key) || field.startsWith(key + "_LANG_"))
                    .collect(Collectors.toList());
            for (String languageField : fieldNames) {
                String locale = null;
                if (languageField.matches(key + "_LANG_\\w{2,3}")) {
                    locale = languageField.substring(languageField.lastIndexOf("_LANG_") + 6).toLowerCase();
                } else {
                    locale = MultiLanguageMetadataValue.DEFAULT_LANGUAGE;
                }
                Collection<String> languageValues = doc.getMetadataValues(languageField);
                if (languageValues != null) {
                    List<String> values = languageValues.stream().map(value -> String.valueOf(value)).collect(Collectors.toList());
                    map.put(locale, values);
                }
            }
        }
        return map;
    }

    /**
     * <p>
     * getDisplayUserGeneratedContentsForPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param page a int.
     * @return contents for the given page
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<DisplayUserGeneratedContent> getDisplayUserGeneratedContentsForPage(String pi, int page)
            throws PresentationException, IndexUnreachableException {
        String query = new StringBuilder().append(SolrConstants.PI_TOPSTRUCT)
                .append(":")
                .append(pi)
                .append(" AND ")
                .append(SolrConstants.ORDER)
                .append(":")
                .append(page)
                .append(" AND ")
                .append(SolrConstants.DOCTYPE)
                .append(":")
                .append(DocType.UGC.name())
                .toString();

        SolrDocumentList hits = search(query);
        if (hits.isEmpty()) {
            return Collections.emptyList();
        }

        List<DisplayUserGeneratedContent> ret = new ArrayList<>(hits.size());
        for (SolrDocument doc : hits) {
            DisplayUserGeneratedContent ugc = DisplayUserGeneratedContent.buildFromSolrDoc(doc);
            if (ugc != null) {
                ret.add(ugc);
                logger.trace("Loaded UGC: {}", ugc.getLabel());
            }
        }

        return ret;
    }

    /**
     * Catches the filename of the page with the given order under the given ip
     *
     * @param pi The topstruct pi
     * @param order The page order (1-based
     * @return An optíonal containing the filename of the page with the given order under the given ip. Or an empty optional if no matching page was
     *         found.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public Optional<String> getFilename(String pi, int order) throws PresentationException, IndexUnreachableException {
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append(SolrConstants.DOCTYPE)
                .append(":")
                .append("PAGE")
                .append(" AND ")
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(":")
                .append(pi)
                .append(" AND ")
                .append(SolrConstants.ORDER)
                .append(":")
                .append(order);

        SolrDocumentList hits = search(sbQuery.toString(), Collections.singletonList(SolrConstants.FILENAME));
        if (hits.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable((String) (hits.get(0).getFirstValue(SolrConstants.FILENAME)));
    }

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @return a {@link java.util.Optional} object.
     */
    public static Optional<IMetadataValue> getTranslations(String fieldName, SolrDocument doc) {
        Map<String, List<String>> translations = SolrSearchIndex.getMetadataValuesForLanguage(doc, fieldName);
        if (translations.size() > 1) {
            return Optional.of(new MultiLanguageMetadataValue(translations));
        } else if (translations.size() == 1) {
            return Optional.of(ViewerResourceBundle.getTranslations(translations.values().iterator().next().stream().findFirst().orElse("")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param combiner a {@link java.util.function.BinaryOperator} object.
     * @return a {@link java.util.Optional} object.
     */
    public static Optional<IMetadataValue> getTranslations(String fieldName, SolrDocument doc, BinaryOperator<String> combiner) {
        Map<String, List<String>> translations = SolrSearchIndex.getMetadataValuesForLanguage(doc, fieldName);
        if (translations.size() > 1) {
            return Optional.of(new MultiLanguageMetadataValue(translations, combiner));
        } else if (translations.size() == 1) {
            return Optional.of(ViewerResourceBundle.getTranslations(translations.values().iterator().next().stream().findFirst().orElse("")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * getTranslations.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @param doc a {@link io.goobi.viewer.model.viewer.StructElement} object.
     * @param combiner a {@link java.util.function.BinaryOperator} object.
     * @return a {@link java.util.Optional} object.
     */
    public static Optional<IMetadataValue> getTranslations(String fieldName, StructElement doc, BinaryOperator<String> combiner) {
        Map<String, List<String>> translations = SolrSearchIndex.getMetadataValuesForLanguage(doc, fieldName);
        if (translations.size() > 1) {
            return Optional.of(new MultiLanguageMetadataValue(translations, combiner));
        } else if (!translations.isEmpty()) {
            return Optional.ofNullable(ViewerResourceBundle
                    .getTranslations(translations.values().iterator().next().stream().reduce((s1, s2) -> combiner.apply(s1, s2)).orElse("")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * isHasImages.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @should return correct value for page docs
     * @should return correct value for docsctrct docs
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static boolean isHasImages(SolrDocument doc) throws IndexUnreachableException {
        StructElement structElement = new StructElement(0, doc);
        String fileExtension = "";

        String filename = structElement.getMetadataValue(SolrConstants.FILENAME);
        if (StringUtils.isEmpty(filename)) {
            filename = structElement.getMetadataValue(SolrConstants.THUMBNAIL);
        }
        if (filename != null) {
            fileExtension = FilenameUtils.getExtension(filename).toLowerCase();
        }

        return fileExtension != null && fileExtension.toLowerCase().matches("(tiff?|jpe?g|png|jp2|gif)");
    }

    /**
     * 
     * @param conditions
     * @return
     */
    public static String getProcessedConditions(String conditions) {
        if (conditions == null) {
            return null;
        }

        if (conditions.contains("NOW/YEAR") && !conditions.contains("DATE_")) {
            // Hack for getting the current year as a number for non-date Solr fields
            conditions = conditions.replace("NOW/YEAR", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }

        return conditions.trim();
    }
}
