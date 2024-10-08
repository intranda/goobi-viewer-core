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
package io.goobi.viewer.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.comparators.ReverseComparator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.request.json.HeatmapFacetMap;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.json.HeatmapJsonFacet;
import org.apache.solr.client.solrj.response.json.NestableJsonFacet;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.luke.FieldFlag;
import org.json.JSONArray;
import org.json.JSONObject;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.Tag;
import io.goobi.viewer.model.viewer.pageloader.AbstractPageLoader;
import io.goobi.viewer.model.viewer.pageloader.IPageLoader;
import io.goobi.viewer.solr.SolrConstants.DocType;

/**
 * <p>
 * SolrSearchIndex class.
 * </p>
 */
public class SolrSearchIndex {

    private static final METHOD DEFAULT_QUERY_METHOD = METHOD.POST;

    private static final Logger logger = LogManager.getLogger(SolrSearchIndex.class);

    /** Constant <code>MAX_HITS=1000000</code>. */
    public static final int MAX_HITS = 1000000;
    private static final int TIMEOUT_SO = 30000;
    private static final int TIMEOUT_CONNECTION = 30000;

    private long lastPing = 0;

    /** Application-scoped map containing already looked up data repository names of records. */
    private Map<String, String> dataRepositoryNames = new HashMap<>();

    private SolrClient client;

    private List<String> solrFields = null;
    /**
     * Usually boolean fields should not be part of the solr field list. In case one needs them, they are listed here
     */
    private List<String> booleanSolrFields = null;

    /**
     * <p>
     * Constructor for SolrSearchIndex.
     * </p>
     *
     * @param client a {@link org.apache.solr.client.solrj.SolrClient} object.
     */
    public SolrSearchIndex(SolrClient client) {
        if (client == null) {
            this.client = getNewSolrClient();
        } else {
            this.client = client;
        }
    }

    /**
     * Checks whether the server's configured URL matches that in the config file. If not, a new server instance is created.
     */
    public void checkReloadNeeded() {
        if (!(client instanceof Http2SolrClient || client instanceof HttpSolrClient)) {
            return;
        }

        String baseUrl = client instanceof Http2SolrClient http2Client ? http2Client.getBaseURL() : ((HttpSolrClient) client).getBaseURL();
        if (!DataManager.getInstance().getConfiguration().getSolrUrl().equals(baseUrl)) {
            // Re-init Solr client if the configured Solr URL has been changed
            logger.info("Solr URL has changed, re-initializing Solr client...");
            synchronized (this) {
                solrFields = null; // Reset available Solr field name list
                try {
                    client.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
                client = getNewSolrClient();
            }
        } else if (lastPing == 0 || System.currentTimeMillis() - lastPing > 60000) {
            // Check whether the HTTP connection pool of the Solr client has been shut down and re-init
            try {
                client.ping();
            } catch (Exception e) {
                logger.warn("HTTP client was closed, re-initializing Solr client...");
                synchronized (this) {
                    try {
                        client.close();
                    } catch (IOException e1) {
                        logger.error(e1.getMessage());
                    }
                    client = getNewSolrClient();
                }
            }
            lastPing = System.currentTimeMillis();
        }
    }

    /**
     * 
     * @return New {@link SolrClient}
     */
    public static SolrClient getNewSolrClient() {
        if (DataManager.getInstance().getConfiguration().isSolrUseHttp2()) {
            return getNewHttp2SolrClient();
        }

        logger.trace("Using HTTP1 compatiblity mode.");
        return getNewHttpSolrClient();
    }

    /**
     * <p>
     * getNewHttpSolrClient.
     * </p>
     *
     * @return a {@link org.apache.solr.client.solrj.impl.HttpSolrServer} object.
     * @deprecated Use getNewHttp2SolrClient(), if Solr 9 is available
     */
    @Deprecated(since = "24.01")
    static HttpSolrClient getNewHttpSolrClient() {
        HttpSolrClient client = new HttpSolrClient.Builder()
                .withBaseSolrUrl(DataManager.getInstance().getConfiguration().getSolrUrl())
                .withSocketTimeout(TIMEOUT_SO)
                .withConnectionTimeout(TIMEOUT_CONNECTION)
                .allowCompression(DataManager.getInstance().getConfiguration().isSolrCompressionEnabled())
                .build();
        //        server.setDefaultMaxConnectionsPerHost(100);
        //        server.setMaxTotalConnections(100);
        client.setFollowRedirects(false); // defaults to false
        //        server.setMaxRetries(1); // defaults to 0. > 1 not recommended.
        client.setRequestWriter(new BinaryRequestWriter());
        // Backwards compatibility mode for Solr 4 servers
        if (DataManager.getInstance().getConfiguration().isSolrBackwardsCompatible()) {
            client.setParser(new XMLResponseParser());
        }

        return client;
    }

    /**
     * <p>
     * getNewHttp2SolrClient.
     * </p>
     *
     * @return a {@link org.apache.solr.client.solrj.impl.HttpSolrServer} object.
     */
    static Http2SolrClient getNewHttp2SolrClient() {
        return new Http2SolrClient.Builder(DataManager.getInstance().getConfiguration().getSolrUrl())
                .withIdleTimeout(TIMEOUT_SO, TimeUnit.MILLISECONDS)
                .withConnectionTimeout(TIMEOUT_CONNECTION, TimeUnit.MILLISECONDS)
                .withFollowRedirects(false)
                .withRequestWriter(new BinaryRequestWriter())
                // .allowCompression(DataManager.getInstance().getConfiguration().isSolrCompressionEnabled())
                .build();
    }

    /**
     *
     * @return true if test query executes without errors; false otherwise
     * @should return true if solr online
     * @should return false if solr offline
     */
    public boolean isSolrIndexOnline() {
        try {
            testQuery(SolrConstants.PI + ":*");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * <p>
     * testQuery.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     * @throws IOException
     */
    public QueryResponse testQuery(String query) throws SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setStart(0);
        solrQuery.setRows(0);

        return client.query(solrQuery);
    }

    /**
     * Main Solr search method.
     *
     * @param query {@link java.lang.String}
     * @param first {@link java.lang.Integer}
     * @param rows {@link java.lang.Integer}
     * @param sortFields Optional field/order pairs for sorting
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
        return search(query, first, rows, sortFields, facetFields, facetSort, fieldList, filterQueries, params, DEFAULT_QUERY_METHOD);
    }

    public QueryResponse search(String query, int first, int rows, List<StringPair> sortFields, List<String> facetFields, String facetSort,
            List<String> fieldList, List<String> filterQueries, Map<String, String> params, METHOD queryMethod)
            throws PresentationException, IndexUnreachableException {
        SolrQuery solrQuery = new SolrQuery(SolrTools.cleanUpQuery(query)).setStart(first).setRows(rows);
        if (sortFields != null && !sortFields.isEmpty()) {
            for (int i = 0; i < sortFields.size(); ++i) {
                StringPair sortField = sortFields.get(i);
                if (StringUtils.isNotEmpty(sortField.getOne())) {
                    if (SolrConstants.SORT_RELEVANCE.equals(sortField.getOne())) {
                        // If RELEVANCE is used, just add nothing
                        continue;
                    } else if (SolrConstants.SORT_RANDOM.equals(sortField.getOne())) {
                        // If RANDOM is used, generate a randomized sort field
                        sortField.setOne(SolrTools.generateRandomSortField());
                    }
                    solrQuery.addSort(sortField.getOne(), "desc".equals(sortField.getTwo()) ? ORDER.desc : ORDER.asc);
                    // logger.trace("sort field: {} {}", sortField.getOne(), sortField.getTwo()); //NOSONAR Debug
                }
            }
        }
        if (facetFields != null && !facetFields.isEmpty()) {
            for (String facetField : facetFields) {
                if (StringUtils.isNotEmpty(facetField)) {
                    // logger.trace("facet field: {}", facetField); //NOSONAR Debug
                    solrQuery.addFacetField(facetField);
                    // TODO only do this once, perhaps?
                    if (StringUtils.isNotEmpty(facetSort)) {
                        solrQuery.setFacetSort(facetSort);
                    }
                }
            }
            solrQuery.setFacetMinCount(1).setFacetLimit(-1); // no limit
        }
        if (fieldList != null && !fieldList.isEmpty()) {
            for (String field : fieldList) {
                if (StringUtils.isNotEmpty(field)) {
                    solrQuery.addField(field);
                }
            }
        }
        if (filterQueries != null && !filterQueries.isEmpty()) {
            for (String fq : filterQueries) {
                String cleanedQuery = SolrTools.cleanUpQuery(fq);
                solrQuery.addFilterQuery(cleanedQuery);
                // logger.trace("adding filter query: {}", fq) //NOSONAR Debug
            }
        }
        if (params != null && !params.isEmpty()) {
            for (Entry<String, String> entry : params.entrySet()) {
                solrQuery.set(entry.getKey(), entry.getValue());
                // logger.trace("&{}={}", key, params.get(key)); //NOSONAR Debug
            }
        }

        try {
            //             logger.trace("Solr query : {}", solrQuery.getQuery()); //NOSONAR Debug
            //             logger.debug("range: {} - {}", first, first + rows); //NOSONAR Debug
            //             logger.debug("facetFields: {}", facetFields); //NOSONAR Debug
            //             logger.debug("fieldList: {}", fieldList); //NOSONAR Debug
            QueryResponse resp = client.query(solrQuery, queryMethod);
            //             logger.debug("found: {}", resp.getResults().getNumFound()); //NOSONAR Debug
            //             logger.debug("fetched: {}", resp.getResults().size()); //NOSONAR Debug

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
            if (SolrTools.isQuerySyntaxError(e)) {
                throw new PresentationException("Bad query: " + e.getMessage());
            }
            logger.error("{} (this usually means Solr is returning 403); Query: {}", SolrTools.extractExceptionMessageHtmlTitle(e.getMessage()),
                    solrQuery.getQuery());
            throw new IndexUnreachableException(e.getMessage());
        } catch (IOException e) {
            throw new IndexUnreachableException(e.getMessage());
        }
    }

    /**
     * <p>
     * search.
     * </p>
     *
     * @param query {@link java.lang.String}
     * @param first {@link java.lang.Integer}
     * @param rows {@link java.lang.Integer}
     * @param sortFields Optional field/order pairs for sorting
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
        // logger.trace("search: {}", query); //NOSONAR Debug
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
     * @param sortFields Optional field/order pairs for sorting
     * @param facetFields a {@link java.util.List} object.
     * @param fieldList If not null, only the fields in the list will be returned.
     * @return {@link org.apache.solr.client.solrj.response.QueryResponse}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public QueryResponse search(String query, int first, int rows, List<StringPair> sortFields, List<String> facetFields, List<String> fieldList)
            throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query); //NOSONAR Debug
        return search(query, first, rows, sortFields, facetFields, fieldList, null, null);
    }

    /**
     * <p>
     * search.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @param rows a int.
     * @param sortFields Optional field/order pairs for sorting
     * @param fieldList If not null, only the fields in the list will be returned.
     * @return a {@link org.apache.solr.common.SolrDocumentList} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocumentList search(String query, int rows, List<StringPair> sortFields, List<String> fieldList)
            throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query); //NOSONAR Debug
        return search(query, 0, rows, sortFields, null, fieldList).getResults();
    }

    /**
     * 
     *
     * @param query a {@link java.lang.String} object.
     * @param fieldList a {@link java.util.List} object.
     * @return {@link SolrDocumentList}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocumentList search(String query, List<String> fieldList) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query); //NOSONAR Debug
        return search(query, 0, MAX_HITS, null, null, fieldList).getResults();
    }

    /**
     * Diese Methode führt eine Suche im Lucene durch.
     *
     * @param query a {@link java.lang.String} object.
     * @return {@link SolrDocumentList}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public SolrDocumentList search(String query) throws PresentationException, IndexUnreachableException {
        //        logger.trace("search: {}", query); //NOSONAR Debug
        return search(query, 0, MAX_HITS, null, null, null).getResults();
    }

    /**
     * Retrieves the first document found by the given query.
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
     * Retrieves the first document found by the given query.
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
        // logger.trace("getFirstDoc: {}", query); //NOSONAR Debug
        SolrDocumentList hits = search(SolrTools.cleanUpQuery(query), 0, 1, sortFields, null, fieldList).getResults();
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
        // logger.trace("getDocs: {}", query); //NOSONAR Debug
        SolrDocumentList hits = search(SolrTools.cleanUpQuery(query), fieldList);
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

        SolrDocument ret = null;
        SolrDocumentList hits =
                search(new StringBuilder(SolrConstants.IDDOC).append(':').append(SolrTools.cleanUpQuery(iddoc)).toString(), 0, 1, null, null, null)
                        .getResults();
        if (hits != null && !hits.isEmpty()) {
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
        // logger.trace("getDocumentByIddoc: {}", iddoc); //NOSONAR Debug
        SolrDocument ret = null;
        SolrDocumentList hits =
                search(new StringBuilder(SolrConstants.PI).append(':').append(SolrTools.cleanUpQuery(pi)).toString(), 0, 1, null, null, null)
                        .getResults();
        if (hits != null && !hits.isEmpty()) {
            ret = hits.get(0);
        }

        return ret;
    }

    public SolrDocument getDocumentByPIAndLogId(String pi, String divId) throws IndexUnreachableException, PresentationException {
        SolrDocument ret = null;
        if (StringUtils.isNoneBlank(pi, divId)) {
            // logger.trace("getDocumentByIddoc: {}", iddoc); //NOSONAR Debug
            String query = SolrConstants.PI_TOPSTRUCT + ":" + pi + SolrConstants.SOLR_QUERY_AND + SolrConstants.LOGID + ":" + divId;
            SolrDocumentList hits = search(query, 0, 1, null, null, null).getResults();
            if (hits != null && !hits.isEmpty()) {
                ret = hits.get(0);
            }
        } else if (StringUtils.isNotBlank(pi)) {
            ret = getDocumentByPI(pi);
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

        String query = SolrTools.cleanUpQuery(new StringBuilder(fieldName).append(":*").append(querySuffix).toString());
        logger.trace("generateFilteredTagCloud query: {}", query);
        Pattern p = Pattern.compile(StringTools.REGEX_WORDS);
        Set<String> stopWords = DataManager.getInstance().getConfiguration().getStopwords();

        List<String> termlist = new ArrayList<>();
        Map<String, Long> frequencyMap = new HashMap<>();
        SolrQuery solrQuery =
                new SolrQuery(query).setRows(DataManager.getInstance().getConfiguration().getTagCloudSampleSize(fieldName)).addField(fieldName);
        try {
            QueryResponse resp = client.query(solrQuery);
            logger.trace("query done");
            for (SolrDocument doc : resp.getResults()) {
                Collection<Object> values = doc.getFieldValues(fieldName);
                if (values == null) {
                    continue;
                }
                for (Object o : values) {
                    String terms = String.valueOf(o).toLowerCase();
                    String[] termsSplit = terms.split(" ");
                    for (String term : termsSplit) {
                        Matcher m = p.matcher(term);
                        if (m.find()) {
                            String t = term.substring(m.start(), m.end());
                            if (t.length() > 2 && t.charAt(0) != 1 && !stopWords.contains(t)) {
                                if (!frequencyMap.containsKey(t)) {
                                    frequencyMap.put(t, 0L);
                                    termlist.add(t);
                                } else {
                                    frequencyMap.put(t, frequencyMap.get(t) + 1);
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
            if (SolrTools.isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
            } else {
                logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            }
        } catch (IOException e) {
            throw new IndexUnreachableException(e.getMessage());
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
        // logger.trace("getIddocFromIdentifier: {}", identifier); //NOSONAR Debug
        SolrDocumentList docs = search(new StringBuilder(SolrConstants.PI).append(':').append(SolrTools.cleanUpQuery(identifier)).toString(), 1, null,
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
            QueryResponse resp = client.query(solrQuery);
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
            if (SolrTools.isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
                throw new PresentationException("Bad query.");
            }
            logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            throw new PresentationException("Search index unavailable.");
        } catch (IOException e) {
            throw new IndexUnreachableException(e.getMessage());
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
        String query = SolrTools.cleanUpQuery(new StringBuilder(SolrConstants.PI_TOPSTRUCT).append(":")
                .append(pi)
                .append(SolrConstants.SOLR_QUERY_AND)
                .append(SolrConstants.ORDER)
                .append(":")
                .append(pageNo)
                .append(SolrConstants.SOLR_QUERY_AND)
                .append(SolrConstants.DOCTYPE)
                .append(":")
                .append(DocType.PAGE.name())
                .toString());
        logger.trace("query: {}", query);
        String luceneOwner = SolrConstants.IDDOC_OWNER;
        SolrDocument pageDoc = getFirstDoc(query, Collections.singletonList(luceneOwner));
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
     * @return a long.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @should retrieve correct IDDOC
     */
    public long getIddocByLogid(String pi, String logId) throws IndexUnreachableException, PresentationException {
        logger.trace("getIddocByLogid: {}:{}", pi, logId);
        String query = SolrTools.cleanUpQuery(new StringBuilder("+")
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(":")
                .append(pi)
                .append(" +")
                .append(SolrConstants.LOGID)
                .append(":")
                .append(logId)
                .append(" +")
                .append(SolrConstants.DOCTYPE)
                .append(":")
                .append(DocType.DOCSTRCT.name())
                .toString());
        SolrDocument doc = getFirstDoc(query, Collections.singletonList(SolrConstants.IDDOC));
        if (doc != null) {
            String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC);
            return Long.valueOf(iddoc);
        }

        return -1;
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
     * Returns the number of hits for the given query and filter queries without actually returning any documents.
     *
     * @param query
     * @param filterQueries
     * @return Number of hits for the given queries
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public long getHitCount(String query, List<String> filterQueries) throws IndexUnreachableException, PresentationException {
        QueryResponse qr = search(query, 0, 0, null, null, null, filterQueries, null);
        return qr.getResults().getNumFound();
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
    public String findDataRepositoryName(String pi) throws PresentationException, IndexUnreachableException {
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
        // logger.trace("findDataRepository: {}", pi); //NOSONAR Debug
        if (StringUtils.isEmpty(pi)) {
            throw new IllegalArgumentException("pi may not be null or empty");
        }
        SolrQuery solrQuery = new SolrQuery(new StringBuilder(SolrConstants.PI).append(":").append(SolrTools.cleanUpQuery(pi)).toString());
        solrQuery.setRows(1);
        solrQuery.setFields(SolrConstants.DATAREPOSITORY);

        try {
            QueryResponse resp = client.query(solrQuery);
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
            if (SolrTools.isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
                throw new PresentationException("Bad query.");
            }
            logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), solrQuery.getQuery());
            throw new IndexUnreachableException(e.getMessage());
        } catch (IOException e) {
            throw new IndexUnreachableException(e.getMessage());
        }

        return null;
    }

    /**
     * Returns facets for the given facet field list. No actual docs are returned since they aren't necessary.
     *
     * @param query The query to use.
     * @param filterQueries Optional filter queries
     * @param facetFields List of facet fields.
     * @param facetMinCount a int.
     * @param getFieldStatistics If true, field statistics will be generated for every facet field.
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should generate facets correctly
     * @should generate field statistics for every facet field if requested
     * @should not return any docs
     */
    public QueryResponse searchFacetsAndStatistics(String query, List<String> filterQueries, List<String> facetFields, int facetMinCount,
            boolean getFieldStatistics) throws PresentationException, IndexUnreachableException {
        // logger.trace("searchFacetsAndStatistics: {}", query); //NOSONAR Debug
        return searchFacetsAndStatistics(query, filterQueries, facetFields, facetMinCount, null, null, getFieldStatistics);
    }

    /**
     * Returns facets for the given facet field list. No actual docs are returned since they aren't necessary.
     *
     * @param query The query to use.
     * @param filterQueries Optional filter queries
     * @param facetFields List of facet fields.
     * @param facetMinCount a int.
     * @param params
     * @param getFieldStatistics If true, field statistics will be generated for every facet field.
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public QueryResponse searchFacetsAndStatistics(String query, List<String> filterQueries, List<String> facetFields, int facetMinCount,
            Map<String, String> params, boolean getFieldStatistics)
            throws PresentationException, IndexUnreachableException {
        // logger.trace("searchFacetsAndStatistics: {}", query); //NOSONAR Debug
        return searchFacetsAndStatistics(query, filterQueries, facetFields, facetMinCount, null, params, getFieldStatistics);
    }

    /**
     * Returns facets for the given facet field list. No actual docs are returned since they aren't necessary.
     *
     * @param query The query to use.
     * @param filterQueries Optional filter queries
     * @param facetFields List of facet fields.
     * @param facetMinCount a int.
     * @param facetPrefix The facet field value must start with these characters. Ignored if null or blank
     * @param params
     * @param getFieldStatistics If true, field statistics will be generated for every facet field.
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should generate facets correctly
     * @should generate field statistics for every facet field if requested
     * @should not return any docs
     */
    public QueryResponse searchFacetsAndStatistics(String query, List<String> filterQueries, List<String> facetFields, int facetMinCount,
            String facetPrefix, Map<String, String> params, boolean getFieldStatistics) throws PresentationException, IndexUnreachableException {
        // logger.trace("searchFacetsAndStatistics: {}", query); //NOSONAR Debug
        SolrQuery solrQuery = new SolrQuery(SolrTools.cleanUpQuery(query));
        solrQuery.setStart(0);
        solrQuery.setRows(0);

        if (filterQueries != null && !filterQueries.isEmpty()) {
            for (String fq : filterQueries) {
                solrQuery.addFilterQuery(fq);
            }
        }

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

        if (params != null && !params.isEmpty()) {
            for (Entry<String, String> entry : params.entrySet()) {
                solrQuery.set(entry.getKey(), entry.getValue());
                // logger.trace("&{}={}", key, params.get(key)); //NOSONAR Debug
            }
        }

        try {
            return client.query(solrQuery);
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
            if (SolrTools.isQuerySyntaxError(e)) {
                logger.error("{}; Query: {}", e.getMessage(), solrQuery.getQuery());
                throw new PresentationException("Bad query.");
            }
            logger.error("{} (this usually means Solr is returning an error); Query: {}", SolrTools.extractExceptionMessageHtmlTitle(e.getMessage()),
                    solrQuery.getQuery());
            throw new IndexUnreachableException(e.getMessage());
        } catch (IOException e) {
            throw new IndexUnreachableException(e.getMessage());
        }
    }

    /**
     * <p>
     * getAllFieldNames.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws DAOException
     * @throws org.apache.solr.client.solrj.SolrServerException if any.
     * @throws java.io.IOException if any.
     */
    public List<String> getAllFieldNames() throws IndexUnreachableException {
        try {
            if (this.solrFields == null) {
                loadSolrFields();
            }
        } catch (IllegalStateException | SolrServerException | RemoteSolrException | IOException e) {
            throw new IndexUnreachableException("Failed to load SOLR field names: " + e.toString());
        }
        return this.solrFields;
    }

    public List<String> getAllBooleanFieldNames() throws IndexUnreachableException {
        try {
            if (this.booleanSolrFields == null) {
                loadSolrFields();
            }
        } catch (IllegalStateException | SolrServerException | RemoteSolrException | IOException e) {
            throw new IndexUnreachableException("Failed to load SOLR field names: " + e.toString());
        }
        return this.booleanSolrFields;
    }

    public void loadSolrFields() throws SolrServerException, IOException {
        LukeRequest lukeRequest = new LukeRequest();
        lukeRequest.setNumTerms(0);
        LukeResponse lukeResponse = lukeRequest.process(client);
        Map<String, FieldInfo> fieldInfoMap = lukeResponse.getFieldInfo();

        List<String> list = new ArrayList<>();
        List<String> boolList = new ArrayList<>();
        for (Entry<String, FieldInfo> entry : fieldInfoMap.entrySet()) {
            FieldInfo info = entry.getValue();
            if (info != null && info.getType() != null && (info.getType().toLowerCase().contains("string")
                    || info.getType().toLowerCase().contains("text") || info.getType().toLowerCase().contains("tlong"))) {
                list.add(entry.getKey());
            } else if (info != null && info.getType().toLowerCase().contains("bool")) {
                boolList.add(entry.getKey());
            }
        }
        this.solrFields = list;
        this.booleanSolrFields = boolList;
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
        LukeResponse lukeResponse = lukeRequest.process(client);
        Map<String, FieldInfo> fieldInfoMap = lukeResponse.getFieldInfo();

        List<String> list = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (String name : fieldInfoMap.keySet()) {
            String n = name;
            if ((n.startsWith(SolrConstants.PREFIX_SORT) || n.startsWith("SORTNUM_") || n.equals(SolrConstants.DATECREATED))) {
                if (n.contains(SolrConstants.MIDFIX_LANG)) {
                    n = n.replaceAll(SolrConstants.MIDFIX_LANG + ".*", SolrConstants.MIDFIX_LANG + "{}");
                }
                if (!added.contains(n)) {
                    list.add(n);
                    added.add(n);
                    // logger.trace("added sort field: {}", n); //NOSONAR Debug
                }
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
        LukeResponse lukeResponse = lukeRequest.process(client);
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
                .append(SolrConstants.SOLR_QUERY_AND)
                .append(SolrConstants.ORDER)
                .append(":")
                .append(page)
                .append(SolrConstants.SOLR_QUERY_AND)
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
     * <p>
     * getDisplayUserGeneratedContentsForPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @return contents for the given page
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<DisplayUserGeneratedContent> getDisplayUserGeneratedContentsForRecord(String pi)
            throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(pi)) {
            logger.warn("Cannot fetch user generated content, no PI value given.");
            return Collections.emptyList();
        }

        String query = new StringBuilder().append(SolrConstants.PI_TOPSTRUCT)
                .append(":")
                .append(pi)
                .append(SolrConstants.SOLR_QUERY_AND)
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
        Collections.sort(ret, (c1, c2) -> Integer.compare(
                c1.getPage() == null ? 0 : c1.getPage(),
                c2.getPage() == null ? 0 : c2.getPage()));

        return ret;
    }

    /**
     * Catches the filename of the page with the given order under the given ip.
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
                .append(DocType.PAGE.name())
                .append(SolrConstants.SOLR_QUERY_AND)
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(":")
                .append(pi)
                .append(SolrConstants.SOLR_QUERY_AND)
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
     * Catches the filename of the page with the given basename under the given ip. Used in case a filename is requested without the file extension
     *
     * @param pi The topstruct pi
     * @param basename The filename of the image without the extension (everything before the last dot)
     * @return An optíonal containing the filename of the page with the given order under the given ip. Or an empty optional if no matching page was
     *         found.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public Optional<String> getFilename(String pi, String basename) throws PresentationException, IndexUnreachableException {
        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append(SolrConstants.DOCTYPE)
                .append(":")
                .append(DocType.PAGE.name())
                .append(SolrConstants.SOLR_QUERY_AND)
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(":")
                .append(pi)
                .append(SolrConstants.SOLR_QUERY_AND)
                .append(SolrConstants.FILENAME)
                .append(":")
                .append(basename)
                .append(".*");

        SolrDocumentList hits = search(sbQuery.toString(), Collections.singletonList(SolrConstants.FILENAME));
        if (hits.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable((String) (hits.get(0).getFirstValue(SolrConstants.FILENAME)));
    }

    /**
     *
     * @param field
     * @param labelField
     * @param values
     * @return Map
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return correct values
     */
    public Map<String, String> getLabelValuesForFacetField(String field, String labelField, Set<String> values)
            throws PresentationException, IndexUnreachableException {
        if (field == null || labelField == null || values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> ret = new HashMap<>();
        Set<String> used = new HashSet<>();
        String[] fields = new String[] { "MD_VALUE", labelField };
        String queryRoot = new StringBuilder().append("+").append(SolrConstants.LABEL).append(":").append(field).append(" +(").toString();
        double pages = Math.ceil(values.size() / 10d);
        List<String> valuesList = new ArrayList<>(values);
        for (int i = 0; i < pages; ++i) {
            StringBuilder sbValueQuery = new StringBuilder();
            int start = i * 10;
            for (int j = start; j < start + 10; ++j) {
                if (j >= valuesList.size()) {
                    break;
                }
                sbValueQuery.append("MD_VALUE:\"").append(valuesList.get(j)).append('"');
            }
            sbValueQuery.append(')');

            // logger.trace("label query: {}{}", queryRoot, sbValueQuery.toString()); //NOSONAR Debug

            SolrDocumentList result = search(SolrTools.cleanUpQuery(queryRoot + sbValueQuery.toString()), Arrays.asList(fields));
            for (SolrDocument doc : result) {
                String value = SolrTools.getSingleFieldStringValue(doc, "MD_VALUE");
                String label = String.valueOf(doc.getFirstValue(labelField));
                if (used.contains(value + label)) {
                    continue;
                }
                if (StringUtils.isNotEmpty(value) || StringUtils.isNotEmpty(labelField)) {
                    String key = field + ":" + value;
                    if (ret.get(key) != null) {
                        // Add all found labels for each value to the string
                        ret.put(key, ret.get(key) + " / " + label);
                        // TODO truncate at certain length?
                    } else {
                        ret.put(key, label);
                    }
                    used.add(value + label);
                }
            }

        }

        return ret;
    }

    /**
     *
     * @return Base URL of the active Solr server
     */
    public String getSolrServerUrl() {
        if (client instanceof Http2SolrClient) {
            return ((Http2SolrClient) client).getBaseURL();
        }

        return null;
    }

    /**
     *
     * @return true if ping successful; false otherwise
     */
    public boolean pingSolrIndex() {
        if (client != null) {
            try {
                SolrPingResponse ping = client.ping();
                return ping.getStatus() < 400;
            } catch (SolrException | SolrServerException | IOException e) {
                logger.trace("Ping to solr failed: {}", SolrTools.extractExceptionMessageHtmlTitle(e.getMessage()));
                return false;
            }
        }

        return false;
    }

    /**
     *
     *
     * @param solrField
     * @param wktRegion
     * @param query
     * @param filterQuery
     * @param gridLevel
     * @return String
     * @throws IndexUnreachableException
     */
    public String getHeatMap(String solrField, String wktRegion, String query, String filterQuery, Integer gridLevel)
            throws IndexUnreachableException {

        HeatmapFacetMap facetMap = new HeatmapFacetMap(solrField)
                .setHeatmapFormat(HeatmapFacetMap.HeatmapFormat.INTS2D)
                .setRegionQuery(wktRegion);
        if (gridLevel != null) {
            facetMap.setGridLevel(gridLevel);
        }

        final JsonQueryRequest request = new JsonQueryRequest()
                .setQuery(SolrTools.cleanUpQuery(query))
                .withFilter(SolrTools.cleanUpQuery(filterQuery))
                .setLimit(0)
                .withFacet("heatmapFacet", facetMap);

        try {
            QueryResponse response = request.process(client);
            final NestableJsonFacet topLevelFacet = response.getJsonFacetingResponse();
            final HeatmapJsonFacet heatmap = topLevelFacet.getHeatmapFacetByName("heatmapFacet");
            if (heatmap != null) {
                return getAsJson(heatmap);
            }
            return "{}";
        } catch (SolrServerException | IOException e) {
            throw new IndexUnreachableException("Error getting facet heatmap: " + e.toString());
        }
    }

    /**
     * @param heatmap
     * @return JSON string representation of given heatmap
     */
    private static String getAsJson(HeatmapJsonFacet heatmap) {
        JSONObject json = new JSONObject();
        json.put("gridLevel", heatmap.getGridLevel());
        json.put("columns", heatmap.getNumColumns());
        json.put("rows", heatmap.getNumRows());
        json.put("minX", heatmap.getMinX());
        json.put("maxX", heatmap.getMaxX());
        json.put("minY", heatmap.getMinY());
        json.put("maxY", heatmap.getMaxY());
        JSONArray rows = new JSONArray();
        List<List<Integer>> grid = heatmap.getCountGrid();
        int count = 0;
        if (grid != null) {
            for (int row = 0; row < heatmap.getNumRows(); row++) {
                List<Integer> gridRow = grid.get(row);
                if (gridRow == null) {
                    rows.put(JSONObject.NULL);
                } else {
                    JSONArray column = new JSONArray();
                    count += gridRow.stream().mapToInt(Integer::intValue).sum();
                    column.putAll(gridRow);
                    rows.put(column);
                }
            }
        }
        json.put("count", count);
        json.put("counts_ints2D", rows);

        return json.toString();
    }

    /**
     * <p>
     * getPage.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     * @param order a int.
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getPage(String pi, int order) throws IndexUnreachableException, PresentationException, DAOException {
        SolrDocument doc = getDocumentByPI(pi);
        if (doc != null) {
            StructElement struct = new StructElement(Long.parseLong(doc.getFirstValue(SolrConstants.IDDOC).toString()), doc);
            IPageLoader pageLoader = AbstractPageLoader.create(struct, List.of(order));
            return pageLoader.getPage(order);
        }
        return null;
    }

    /**
     * <p>
     * getPage.
     * </p>
     *
     * @param struct
     * @param order a int.
     * @return a {@link io.goobi.viewer.model.viewer.PhysicalElement} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public PhysicalElement getPage(StructElement struct, int order) throws IndexUnreachableException, PresentationException, DAOException {
        IPageLoader pageLoader = AbstractPageLoader.create(struct, List.of(order));
        return pageLoader.getPage(order);
    }

    /**
     * @return the dataRepositoryNames
     */
    public Map<String, String> getDataRepositoryNames() {
        return dataRepositoryNames;
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

}
