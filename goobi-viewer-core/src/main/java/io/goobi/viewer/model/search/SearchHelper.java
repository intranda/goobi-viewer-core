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
package io.goobi.viewer.model.search;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ExpandParams;
import org.apache.solr.common.params.GroupParams;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.SearchHit.HitType;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.termbrowsing.BrowseTerm;
import io.goobi.viewer.model.termbrowsing.BrowseTermComparator;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;
import io.goobi.viewer.model.translations.language.LocaleComparator;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;

/**
 * Search utility class. Static methods only.
 */
public final class SearchHelper {

    private static final Logger logger = LoggerFactory.getLogger(SearchHelper.class);

    // public static final String[] FULLTEXT_SEARCH_FIELDS = { LuceneConstants.FULLTEXT, LuceneConstants.IDDOC_OWNER,
    // LuceneConstants.IDDOC_IMAGEOWNER };

    /** Constant <code>PARAM_NAME_FILTER_QUERY_SUFFIX="filterQuerySuffix"</code> */
    public static final String PARAM_NAME_FILTER_QUERY_SUFFIX = "filterQuerySuffix";
    /** Constant <code>SEARCH_TERM_SPLIT_REGEX="[ ]|[,]|[-]"</code> */
    public static final String SEARCH_TERM_SPLIT_REGEX = "[ ]|[,]|[-]";
    /** Constant <code>PLACEHOLDER_HIGHLIGHTING_START="##HLS##"</code> */
    public static final String PLACEHOLDER_HIGHLIGHTING_START = "##ĦŁ$##";
    /** Constant <code>PLACEHOLDER_HIGHLIGHTING_END="##HLE##"</code> */
    public static final String PLACEHOLDER_HIGHLIGHTING_END = "##ĦŁȄ##";
    /** Constant <code>SEARCH_TYPE_REGULAR=0</code> */
    public static final int SEARCH_TYPE_REGULAR = 0;
    /** Constant <code>SEARCH_TYPE_ADVANCED=1</code> */
    public static final int SEARCH_TYPE_ADVANCED = 1;
    /** Constant <code>SEARCH_TYPE_TIMELINE=2</code> */
    public static final int SEARCH_TYPE_TIMELINE = 2;
    /** Constant <code>SEARCH_TYPE_CALENDAR=3</code> */
    public static final int SEARCH_TYPE_CALENDAR = 3;
    /** Constant <code>SEARCH_FILTER_ALL</code> */
    public static final SearchFilter SEARCH_FILTER_ALL = new SearchFilter("filter_ALL", "ALL");
    public static final String AGGREGATION_QUERY_PREFIX = "{!join from=PI_TOPSTRUCT to=PI}";
    /** Standard Solr query for all records and anchors. */
    public static final String ALL_RECORDS_QUERY = "+(ISWORK:true ISANCHOR:true)";
    /** Constant <code>DEFAULT_DOCSTRCT_WHITELIST_FILTER_QUERY="(ISWORK:true OR ISANCHOR:true) AND NOT("{trunked}</code> */
    public static final String DEFAULT_DOCSTRCT_WHITELIST_FILTER_QUERY = ALL_RECORDS_QUERY + " -IDDOC_PARENT:*";

    private static final Object lock = new Object();

    private static final Random random = new SecureRandom();

    /** Constant <code>patternNotBrackets</code> */
    public static Pattern patternNotBrackets = Pattern.compile("NOT\\([^()]*\\)");
    /** Constant <code>patternPhrase</code> */
    public static Pattern patternPhrase = Pattern.compile("[\\w]+:" + StringTools.REGEX_QUOTATION_MARKS);

    /** Filter subquery for collection listing (no volumes). */
    static volatile String collectionBlacklistFilterSuffix = null;

    /**
     * Main search method for flat search.
     *
     * @param query {@link java.lang.String} Solr search query. Merges full-text and metadata hits into their corresponding docstructs.
     * @param first {@link java.lang.Integer} von
     * @param rows {@link java.lang.Integer} bis
     * @param sortFields a {@link java.util.List} object.
     * @param resultFields a {@link java.util.List} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param exportFields a {@link java.util.List} object.
     * @param locale a {@link java.util.Locale} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return List of <code>StructElement</code>s containing the search hits.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static List<SearchHit> searchWithFulltext(String query, int first, int rows, List<StringPair> sortFields, List<String> resultFields,
            List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms, List<String> exportFields, Locale locale,
            HttpServletRequest request) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return searchWithFulltext(query, first, rows, sortFields, resultFields, filterQueries, params, searchTerms, exportFields, locale, request,
                false);
    }

    /**
     * Main search method for flat search.
     *
     * @param query {@link java.lang.String} Solr search query. Merges full-text and metadata hits into their corresponding docstructs.
     * @param first {@link java.lang.Integer} von
     * @param rows {@link java.lang.Integer} bis
     * @param sortFields a {@link java.util.List} object.
     * @param resultFields a {@link java.util.List} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param exportFields a {@link java.util.List} object.
     * @param locale a {@link java.util.Locale} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return List of <code>StructElement</code>s containing the search hits.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static List<SearchHit> searchWithFulltext(String query, int first, int rows, List<StringPair> sortFields, List<String> resultFields,
            List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms, List<String> exportFields, Locale locale,
            HttpServletRequest request, boolean keepSolrDoc)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        Map<String, SolrDocument> ownerDocs = new HashMap<>();
        QueryResponse resp =
                DataManager.getInstance().getSearchIndex().search(query, first, rows, sortFields, null, resultFields, filterQueries, params);
        if (resp.getResults() == null) {
            return Collections.emptyList();
        }
        if (params != null) {
            logger.trace("params: {}", params.toString());
        }
        Set<String> ignoreFields = new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataIgnoreFields());
        Set<String> translateFields = new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataTranslateFields());
        logger.trace("hits found: {}; results returned: {}", resp.getResults().getNumFound(), resp.getResults().size());
        List<SearchHit> ret = new ArrayList<>(resp.getResults().size());
        int count = 0;
        ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
        for (SolrDocument doc : resp.getResults()) {
            logger.trace("result iddoc: {}", doc.getFieldValue(SolrConstants.IDDOC));
            String fulltext = null;
            SolrDocument ownerDoc = null;
            if (doc.containsKey(SolrConstants.IDDOC_OWNER)) {
                // This is a page, event or metadata. Look up the doc that contains the image owner docstruct.
                String ownerIddoc = (String) doc.getFieldValue(SolrConstants.IDDOC_OWNER);
                ownerDoc = ownerDocs.get(ownerIddoc);
                if (ownerDoc == null) {
                    ownerDoc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc(ownerIddoc);
                    if (ownerDoc != null) {
                        ownerDocs.put(ownerIddoc, ownerDoc);
                    }
                }

                // Load full-text
                try {
                    String altoFilename = (String) doc.getFirstValue(SolrConstants.FILENAME_ALTO);
                    String plaintextFilename = (String) doc.getFirstValue(SolrConstants.FILENAME_FULLTEXT);
                    String pi = (String) doc.getFirstValue(SolrConstants.PI_TOPSTRUCT);
                    if (StringUtils.isNotBlank(plaintextFilename)) {
                        boolean access = AccessConditionUtils.checkAccess(BeanUtils.getRequest(), "text", pi, plaintextFilename, false);
                        if (access) {
                            fulltext = DataFileTools.loadFulltext(null, plaintextFilename, false, request);
                        } else {
                            fulltext = ViewerResourceBundle.getTranslation("fulltextAccessDenied", null);
                        }
                    } else if (StringUtils.isNotBlank(altoFilename)) {
                        boolean access = AccessConditionUtils.checkAccess(BeanUtils.getRequest(), "text", pi, altoFilename, false);
                        if (access) {
                            fulltext = DataFileTools.loadFulltext(altoFilename, null, false, request);
                        } else {
                            fulltext = ViewerResourceBundle.getTranslation("fulltextAccessDenied", null);
                        }
                    }
                } catch (FileNotFoundException e) {
                    logger.error(e.getMessage());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                // Add docstruct documents to the owner doc map, just in case
                ownerDocs.put((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
            }

            SearchHit hit =
                    SearchHit.createSearchHit(doc, ownerDoc, null, locale, fulltext, searchTerms, exportFields, sortFields,
                            ignoreFields, translateFields, null, thumbs);
            if (keepSolrDoc) {
                hit.setSolrDoc(doc);
            }
            ret.add(hit);
            count++;
            logger.trace("added hit {}", count);
        }

        return ret;
    }

    /**
     * Main search method for aggregated search.
     *
     * @param query {@link java.lang.String} Solr search query. Merges full-text and metadata hits into their corresponding docstructs.
     * @param first {@link java.lang.Integer} von
     * @param rows {@link java.lang.Integer} bis
     * @param sortFields a {@link java.util.List} object.
     * @param resultFields a {@link java.util.List} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param exportFields a {@link java.util.List} object.
     * @param locale a {@link java.util.Locale} object.
     * @return List of <code>StructElement</code>s containing the search hits.
     * @should return all hits
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static List<SearchHit> searchWithAggregation(String query, int first, int rows, List<StringPair> sortFields,
            List<String> resultFields, List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms,
            List<String> exportFields, Locale locale)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return searchWithAggregation(query, first, rows, sortFields, resultFields, filterQueries, params, searchTerms, exportFields, locale, false);
    }

    /**
     * Main search method for aggregated search.
     *
     * @param query {@link java.lang.String} Solr search query. Merges full-text and metadata hits into their corresponding docstructs.
     * @param first {@link java.lang.Integer} von
     * @param rows {@link java.lang.Integer} bis
     * @param sortFields a {@link java.util.List} object.
     * @param resultFields a {@link java.util.List} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param exportFields a {@link java.util.List} object.
     * @param locale a {@link java.util.Locale} object.
     * @return List of <code>StructElement</code>s containing the search hits.
     * @should return all hits
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static List<SearchHit> searchWithAggregation(String query, int first, int rows, List<StringPair> sortFields,
            List<String> resultFields, List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms,
            List<String> exportFields, Locale locale, boolean keepSolrDoc)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("searchWithAggregation: {}", query);
        QueryResponse resp =
                DataManager.getInstance().getSearchIndex().search(query, first, rows, sortFields, null, resultFields, filterQueries, params);
        if (resp.getResults() == null) {
            return new ArrayList<>();
        }
        Set<String> ignoreFields = new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataIgnoreFields());
        Set<String> translateFields = new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataTranslateFields());
        logger.trace("hits found: {}; results returned: {}", resp.getResults().getNumFound(), resp.getResults().size());
        List<SearchHit> ret = new ArrayList<>(resp.getResults().size());
        ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
        for (SolrDocument doc : resp.getResults()) {
            // logger.trace("result iddoc: {}", doc.getFieldValue(SolrConstants.IDDOC));
            Map<String, SolrDocumentList> childDocs = resp.getExpandedResults();

            // Create main hit
            // logger.trace("Creating search hit from {}", doc);
            SearchHit hit =
                    SearchHit.createSearchHit(doc, null, null, locale, null, searchTerms, exportFields, sortFields, ignoreFields,
                            translateFields, null, thumbs);
            if (keepSolrDoc) {
                hit.setSolrDoc(doc);
            }
            ret.add(hit);
            hit.addCMSPageChildren();
            hit.addFulltextChild(doc, locale != null ? locale.getLanguage() : null);
            // logger.trace("Added search hit {}", hit.getBrowseElement().getLabel());
            // Collect Solr docs of child hits 
            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            if (pi != null && childDocs != null && childDocs.containsKey(pi)) {
                logger.trace("{} child hits found for {}", childDocs.get(pi).size(), pi);
                hit.setChildDocs(childDocs.get(pi));
                for (SolrDocument childDoc : childDocs.get(pi)) {
                    String docType = (String) childDoc.getFieldValue(SolrConstants.DOCTYPE);
                    String ownerId = (String) childDoc.getFieldValue(SolrConstants.IDDOC_OWNER);
                    String topStructId = (String) doc.getFieldValue(SolrConstants.IDDOC);
                    if (DocType.METADATA.name().equals(docType)) {
                        // Hack: count metadata hits as docstruct for now (because both are labeled "Metadata")
                        docType = DocType.DOCSTRCT.name();
                    }
                    // if this is a metadata/docStruct hit directly in the top document, don't add to hit count
                    // It will simply be added to the metadata list of the main hit
                    //                    if (!(DocType.DOCSTRCT.name().equals(docType) && ownerId != null && ownerId.equals(topStructId))) {
                    HitType hitType = HitType.getByName(docType);
                    int count = hit.getHitTypeCounts().get(hitType) != null ? hit.getHitTypeCounts().get(hitType) : 0;
                    hit.getHitTypeCounts().put(hitType, count + 1);
                    //                    }
                }
            }
        }
        logger.trace("Return {} search hits", ret.size());
        return ret;
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param addStaticQuerySuffix a boolean.
     * @param addCollectionBlacklistSuffix a boolean.
     * @param addDiscriminatorValueSuffix a boolean.
     * @should add static suffix
     * @should not add static suffix if not requested
     * @should add collection blacklist suffix
     * @should add discriminator value suffix
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static String getAllSuffixes(HttpServletRequest request, boolean addStaticQuerySuffix,
            boolean addCollectionBlacklistSuffix) throws IndexUnreachableException {
        StringBuilder sbSuffix = new StringBuilder("");
        if (addStaticQuerySuffix && StringUtils.isNotBlank(DataManager.getInstance().getConfiguration().getStaticQuerySuffix())) {
            String staticSuffix = DataManager.getInstance().getConfiguration().getStaticQuerySuffix();
            if (staticSuffix.charAt(0) != ' ') {
                sbSuffix.append(' ');
            }
            sbSuffix.append(staticSuffix);
        }
        if (addCollectionBlacklistSuffix) {
            sbSuffix.append(getCollectionBlacklistFilterSuffix(SolrConstants.DC));
        }
        String filterQuerySuffix = getFilterQuerySuffix(request);
        // logger.trace("filterQuerySuffix: {}", filterQuerySuffix);
        if (filterQuerySuffix != null) {
            sbSuffix.append(filterQuerySuffix);
        }

        return sbSuffix.toString();
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @param addDiscriminatorValueSuffix a boolean.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static String getAllSuffixes() throws IndexUnreachableException {
        return getAllSuffixes(BeanUtils.getRequest(), true, true);
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @param addDiscriminatorValueSuffix a boolean.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static String getAllSuffixesExceptCollectionBlacklist()
            throws IndexUnreachableException {
        return getAllSuffixes(BeanUtils.getRequest(), true, false);
    }

    /**
     * Returns the <code>BrowseElement</code> constructed from the search hit at <code>index</code> from the search hit list for the given
     * <code>query</code>.
     *
     * @param query a {@link java.lang.String} object.
     * @param index a int.
     * @param sortFields a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param locale a {@link java.util.Locale} object.
     * @param aggregateHits a boolean.
     * @should return correct hit for non-aggregated search
     * @should return correct hit for aggregated search
     * @param filterQueries a {@link java.util.List} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link io.goobi.viewer.model.search.BrowseElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static BrowseElement getBrowseElement(String query, int index, List<StringPair> sortFields, List<String> filterQueries,
            Map<String, String> params, Map<String, Set<String>> searchTerms, Locale locale, boolean aggregateHits, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        String finalQuery = prepareQuery(query);
        finalQuery = buildFinalQuery(finalQuery, aggregateHits);
        logger.trace("getBrowseElement final query: {}", finalQuery);
        List<SearchHit> hits = aggregateHits
                ? SearchHelper.searchWithAggregation(finalQuery, index, 1, sortFields, null, filterQueries, params, searchTerms, null, locale)
                : SearchHelper.searchWithFulltext(finalQuery, index, 1, sortFields, null, filterQueries, params, searchTerms, null, locale, request);
        if (!hits.isEmpty()) {
            return hits.get(0).getBrowseElement();
        }

        return null;
    }

    /**
     * <p>
     * getFirstRecordMetadataWithFieldValue.
     * </p>
     *
     * @param luceneField a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     * @param filterForWhitelist a boolean.
     * @param filterForBlacklist a boolean.
     * @param separatorString a {@link java.lang.String} object.
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static StringPair getFirstRecordURL(String luceneField, String value, boolean filterForWhitelist,
            boolean filterForBlacklist, String separatorString, Locale locale)
            throws IndexUnreachableException, PresentationException {
        if (luceneField == null || value == null) {
            return null;
        }

        StringBuilder sbQuery = new StringBuilder();
        if (filterForWhitelist) {
            if (sbQuery.length() > 0) {
                sbQuery.append(" AND ");
            }
            sbQuery.append('(').append(getDocstrctWhitelistFilterQuery()).append(')');
        }
        sbQuery.append(SearchHelper.getAllSuffixesExceptCollectionBlacklist());
        sbQuery.append(" AND (")
                .append(luceneField)
                .append(":")
                .append(value)
                .append(" OR ")
                .append(luceneField)
                .append(":")
                .append(value + separatorString + "*)");
        if (filterForBlacklist) {
            sbQuery.append(getCollectionBlacklistFilterSuffix(luceneField));
        }

        List<String> fields =
                Arrays.asList(SolrConstants.PI, SolrConstants.MIMETYPE, SolrConstants.DOCSTRCT, SolrConstants.THUMBNAIL, SolrConstants.ISANCHOR,
                        SolrConstants.ISWORK, SolrConstants.LOGID);
        // logger.trace("query: {}", sbQuery.toString());
        QueryResponse resp = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 0, 1, null, null, fields);
        // logger.trace("query done");

        if (resp.getResults().size() == 0) {
            return null;
        }

        String splittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(luceneField);
        try {
            SolrDocument doc = resp.getResults().get(0);
            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            Collection<Object> accessConditions = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
            if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_LIST,
                    BeanUtils.getRequest())) {
                // TODO check whether users with permissions still skip over such records
                logger.trace("Record '{}' does not allow listing, skipping...", pi);
                throw new RecordNotFoundException(pi);
            }

            boolean anchorOrGroup = SolrTools.isAnchor(doc) || SolrTools.isGroup(doc);
            PageType pageType =
                    PageType.determinePageType((String) doc.get(SolrConstants.DOCSTRCT), (String) doc.get(SolrConstants.MIMETYPE), anchorOrGroup,
                            doc.containsKey(SolrConstants.THUMBNAIL), false);

            //            String url = DataManager.getInstance()
            //                    .getUrlBuilder()
            //                    .buildPageUrl(pi, 1, (String) doc.getFieldValue(SolrConstants.LOGID), pageType);
            //            logger.trace(url);
            return new StringPair(pi, pageType.name());
        } catch (Throwable e) {
            logger.error("Failed to retrieve record", e);
        }

        return null;
    }

    /**
     * Returns a Map with hierarchical values from the given field and their respective record counts. Results are filtered by AccessConditions
     * available for current HttpRequest
     *
     * @param luceneField the SOLR field over which to build the collections (typically "DC")
     * @param facetField a SOLR field which values should be recorded for each collection. Values are written into
     *            {@link CollectionResult#getFacetValues()}. Used for grouping service of IIIF collections
     * @param filterQuery An addition solr-query to filer collections by.
     * @param filterForWhitelist a boolean.
     * @param filterForBlacklist a boolean.
     * @param splittingChar Character used for separating collection hierarchy levels within a collection name (typically ".")
     * @should find all collections
     * @return a {@link java.util.Map} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static Map<String, CollectionResult> findAllCollectionsFromField(String luceneField, String facetField, String filterQuery,
            boolean filterForWhitelist, boolean filterForBlacklist, String splittingChar) throws IndexUnreachableException {
        logger.debug("findAllCollectionsFromField: {}", luceneField);
        Map<String, CollectionResult> ret = new HashMap<>();
        if (StringUtils.isBlank(splittingChar)) {
            throw new IllegalArgumentException("Splitting char may not be empty. Check configuration for collection field " + luceneField);
        }
        try {
            StringBuilder sbQuery = new StringBuilder();
            if (StringUtils.isNotBlank(filterQuery)) {
                sbQuery.append(filterQuery);
            }
            if (filterForWhitelist) {
                if (sbQuery.length() > 0) {
                    sbQuery.append(" AND ");
                }
                sbQuery.append("+(").append(getDocstrctWhitelistFilterQuery()).append(')');
            }
            sbQuery.append(SearchHelper.getAllSuffixesExceptCollectionBlacklist());
            if (filterForBlacklist) {
                sbQuery.append(getCollectionBlacklistFilterSuffix(luceneField));
            }

            // Iterate over record hits instead of using facets to determine the size of the parent collections
            {
                logger.trace("query: {}", sbQuery.toString());

                QueryResponse response = DataManager.getInstance()
                        .getSearchIndex()
                        .searchFacetsAndStatistics(sbQuery.toString(), null, Collections.singletonList(luceneField), 1, false);
                FacetField facetResults = response.getFacetField(luceneField);

                for (Count count : facetResults.getValues()) {
                    String dc = count.getName();
                    // Skip inverted values
                    if (StringTools.checkValueEmptyOrInverted(dc)) {
                        continue;
                    }

                    CollectionResult result = ret.get(dc);
                    if (result == null) {
                        result = new CollectionResult(dc);
                        ret.put(dc, result);
                    }
                    result.incrementCount(count.getCount());

                    if (dc.contains(splittingChar)) {
                        String parent = dc;
                        while (parent.lastIndexOf(splittingChar) != -1) {
                            parent = parent.substring(0, parent.lastIndexOf(splittingChar));
                            CollectionResult parentCollection = ret.get(parent);
                            if (parentCollection == null) {
                                parentCollection = new CollectionResult(parent);
                                ret.put(parent, parentCollection);
                            }
                            parentCollection.incrementCount(count.getCount());
                        }
                    }
                }

            }

            //Add facet (grouping) field values
            if (StringUtils.isNotBlank(facetField)) {
                for (String collectionName : ret.keySet()) {
                    //query all results from above filtered for this collection and subcollections
                    String collectionFilterQuery = "+($1:$2 $1:$2.*)".replace("$1", luceneField).replace("$2", collectionName);
                    String query = sbQuery.toString() + " " + collectionFilterQuery;

                    QueryResponse response = DataManager.getInstance()
                            .getSearchIndex()
                            .searchFacetsAndStatistics(query, null, Collections.singletonList(facetField), 1, false);
                    FacetField facetResults = response.getFacetField(facetField);

                    CollectionResult collectionResult = ret.get(collectionName);
                    collectionResult.setFacetValues(facetResults.getValues()
                            .stream()
                            .map(Count::getName)
                            .filter(v -> !v.startsWith("#1;") && !v.startsWith("\\u0001") && !v.startsWith("\u0001"))
                            .collect(Collectors.toSet()));
                }
            }

        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }

        logger.debug("{} collections found", ret.size());
        return ret;
    }

    /**
     * Matches given collection name against the given collection blacklist. Also matches wildcards and child collections.
     *
     * @param dc a {@link java.lang.String} object.
     * @param blacklist a {@link java.util.Set} object.
     * @param splittingChar a {@link java.lang.String} object.
     * @should match simple collections correctly
     * @should match subcollections correctly
     * @should throw IllegalArgumentException if dc is null
     * @should throw IllegalArgumentException if blacklist is null
     * @return a boolean.
     */
    protected static boolean checkCollectionInBlacklist(String dc, Set<String> blacklist, String splittingChar) {
        if (dc == null) {
            throw new IllegalArgumentException("dc may not be null");
        }
        if (blacklist == null) {
            throw new IllegalArgumentException("blacklist may not be null");
        }
        if (splittingChar == null) {
            throw new IllegalArgumentException("splittingChar may not be null");
        }

        String collectionSplitRegex = new StringBuilder("[").append(splittingChar).append(']').toString();
        String dcSplit[] = dc.split(collectionSplitRegex);
        // boolean blacklisted = false;
        StringBuilder sbDc = new StringBuilder();
        for (String element : dcSplit) {
            if (sbDc.length() > 0) {
                sbDc.append(splittingChar);
            }
            sbDc.append(element);
            String current = sbDc.toString();
            if (blacklist.contains(current)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * searchCalendar.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @param facetFields a {@link java.util.List} object.
     * @param facetMinCount a int.
     * @param getFieldStatistics a boolean.
     * @return a {@link org.apache.solr.client.solrj.response.QueryResponse} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static QueryResponse searchCalendar(String query, List<String> facetFields, int facetMinCount, boolean getFieldStatistics)
            throws PresentationException, IndexUnreachableException {
        logger.trace("searchCalendar: {}", query);
        StringBuilder sbQuery =
                new StringBuilder(query).append(getAllSuffixes());
        return DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(sbQuery.toString(), null, facetFields, facetMinCount, getFieldStatistics);
    }

    /**
     * <p>
     * getMinMaxYears.
     * </p>
     *
     * @param subQuery a {@link java.lang.String} object.
     * @return an array of {@link int} objects.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static int[] getMinMaxYears(String subQuery) throws PresentationException, IndexUnreachableException {
        int[] ret = { -1, -1 };

        String searchString = String.format("+%s:*", SolrConstants._CALENDAR_YEAR);
        if (StringUtils.isNotBlank(subQuery)) {
            searchString += " " + subQuery;
        }

        // logger.debug("searchString: {}", searchString);
        QueryResponse resp = searchCalendar(searchString, Collections.singletonList(SolrConstants._CALENDAR_YEAR), 0, true);

        FieldStatsInfo info = resp.getFieldStatsInfo().get(SolrConstants._CALENDAR_YEAR);
        Object min = info.getMin();
        if (min instanceof Long || min instanceof Integer) {
            ret[0] = (int) min;
        } else if (min instanceof Double) {
            ret[0] = ((Double) min).intValue();
        }
        Object max = info.getMax();
        if (max instanceof Long || max instanceof Integer) {
            ret[1] = (int) max;
        } else if (max instanceof Double) {
            ret[1] = ((Double) max).intValue();
        }

        logger.trace("Min year: {}, max year: {}", ret[0], ret[1]);
        return ret;
    }

    /**
     * search method for auto suggestion
     *
     * <li>First search in field "DEFAULT" and analyze values tokenized, check with startsWith</li>
     * <li>Then search in field "TITLE" and check with contains</li>
     *
     * @param suggest the search string
     * @param currentFacets a {@link java.util.List} object.
     * @should return autosuggestions correctly
     * @should filter by collection correctly
     * @should filter by facet correctly
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static List<String> searchAutosuggestion(final String suggest, List<IFacetItem> currentFacets) throws IndexUnreachableException {
        if (suggest.contains(" ")) {
            return Collections.emptyList();
        }

        List<String> ret = new ArrayList<>();
        try {
            String suggestLower = suggest.toLowerCase();
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append("+").append(SolrConstants.DEFAULT).append(':').append(ClientUtils.escapeQueryChars(suggestLower)).append('*');
            if (currentFacets != null && !currentFacets.isEmpty()) {
                for (IFacetItem facetItem : currentFacets) {
                    if (sbQuery.length() > 0) {
                        sbQuery.append(" AND ");
                    }
                    sbQuery.append(facetItem.getQueryEscapedLink());
                    logger.trace("Added  facet: {}", facetItem.getQueryEscapedLink());
                }
            }
            sbQuery.append(getAllSuffixes());
            logger.debug("Autocomplete query: {}", sbQuery.toString());
            
            QueryResponse response = DataManager.getInstance()
                    .getSearchIndex().searchFacetsAndStatistics(sbQuery.toString(), null, Collections.singletonList(SolrConstants.DEFAULT), 1, null, false);
            FacetField facetField = response.getFacetFields().get(0);
            
            ret = facetField.getValues().stream()
            .filter( count -> count.getName().toLowerCase().startsWith(suggestLower))
            .sorted( (c1,c2) -> Long.compare(c2.getCount(), c1.getCount()) )
            .map(Count::getName)
            .distinct()
            .collect(Collectors.toList());
            
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
        }

        return ret;
    }

    /**
     * @return Filter query for record listing
     */
    static String getDocstrctWhitelistFilterQuery() {
        return DataManager.getInstance().getConfiguration().getDocstrctWhitelistFilterQuery();
    }

    /**
     * Returns a Solr query suffix that filters out collections defined in the collection blacklist. This suffix is only generated once per
     * application lifecycle.
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getCollectionBlacklistFilterSuffix(String field) {
        String suffix = collectionBlacklistFilterSuffix;
        if (suffix == null) {
            synchronized (lock) {
                suffix = collectionBlacklistFilterSuffix;
                if (suffix == null) {
                    suffix = generateCollectionBlacklistFilterSuffix(field);
                    collectionBlacklistFilterSuffix = suffix;
                }
            }
        }

        return suffix;
    }

    /**
     * <p>
     * generateCollectionBlacklistFilterSuffix.
     * </p>
     *
     * @param field a {@link java.lang.String} object.
     * @should construct suffix correctly
     * @return a {@link java.lang.String} object.
     */
    protected static String generateCollectionBlacklistFilterSuffix(String field) {
        logger.trace("Generating blacklist suffix for field '{}'...", field);
        StringBuilder sbQuery = new StringBuilder();
        List<String> list = DataManager.getInstance().getConfiguration().getCollectionBlacklist(field);
        if (list != null && !list.isEmpty()) {
            // sbQuery.append(" AND NOT (");
            for (String s : list) {
                if (StringUtils.isNotBlank(s)) {
                    sbQuery.append(" -").append(field).append(':').append(s.trim());
                }
            }
            // sbQuery.delete(sbQuery.length() - 4, sbQuery.length());
            // sbQuery.append(')');
        }

        return sbQuery.toString();
    }

    /**
     * <p>
     * getDiscriminatorFieldFilterSuffix.
     * </p>
     *
     * @param discriminatorField a {@link java.lang.String} object.
     * @should construct subquery correctly
     * @should return empty string if discriminator value is empty or hyphen
     * @param nh a {@link io.goobi.viewer.managedbeans.NavigationHelper} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static String getDiscriminatorFieldFilterSuffix(NavigationHelper nh, String discriminatorField) throws IndexUnreachableException {
        // logger.trace("nh null? {}", nh == null);
        logger.trace("discriminatorField: {}", discriminatorField);
        if (StringUtils.isNotEmpty(discriminatorField) && nh != null) {
            String discriminatorValue = nh.getSubThemeDiscriminatorValue();
            logger.trace("discriminatorValue: {}", discriminatorValue);
            if (StringUtils.isNotEmpty(discriminatorValue) && !"-".equals(discriminatorValue)) {
                StringBuilder sbSuffix = new StringBuilder();
                sbSuffix.append(" +").append(discriminatorField).append(':').append(discriminatorValue);
                logger.trace("Discriminator field suffix: {}", sbSuffix.toString());
                return sbSuffix.toString();
            }
        }

        return "";
    }

    /**
     * Updates the calling agent's session with a personalized filter sub-query.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static void updateFilterQuerySuffix(HttpServletRequest request) throws IndexUnreachableException, PresentationException, DAOException {
        String filterQuerySuffix = getPersonalFilterQuerySuffix((User) request.getSession().getAttribute("user"), NetTools.getIpAddress(request));
        logger.trace("New filter query suffix: {}", filterQuerySuffix);
        request.getSession().setAttribute(PARAM_NAME_FILTER_QUERY_SUFFIX, filterQuerySuffix);
    }

    /**
     * Constructs a personal search query filter suffix for the given user and IP address.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param ipAddress a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should construct suffix correctly
     * @should construct suffix correctly if user has license privilege
     * @should construct suffix correctly if user has overriding license privilege
     * @should construct suffix correctly if ip range has license privilege
     * @should construct suffix correctly if moving wall license
     */
    public static String getPersonalFilterQuerySuffix(User user, String ipAddress)
            throws IndexUnreachableException, PresentationException, DAOException {
        // No restrictions for admins
        if (user != null && user.isSuperuser()) {
            return "";
        }
        // No restrictions for localhost, if so configured
        if (NetTools.isIpAddressLocalhost(ipAddress)
                && DataManager.getInstance().getConfiguration().isFullAccessForLocalhost()) {
            return "";
        }

        StringBuilder query = new StringBuilder();
        query.append(" +(").append(SolrConstants.ACCESSCONDITION).append(":\"").append(SolrConstants.OPEN_ACCESS_VALUE).append('"');

        Set<String> usedLicenseTypes = new HashSet<>();
        for (LicenseType licenseType : DataManager.getInstance().getDao().getRecordLicenseTypes()) {
            if (usedLicenseTypes.contains(licenseType.getName())) {
                continue;
            }

            // Moving wall license type, use negated filter query
            if (licenseType.isMovingWall() && StringUtils.isNotBlank(licenseType.getProcessedConditions())) {
                logger.trace("License type '{}' is a moving wall", licenseType.getName());
                query.append(licenseType.getFilterQueryPart(true));
                // Do not continue with the next license type here because the user may have full access to the moving wall license, in which case it should also be added with a non-negated filter query
            }

            // License type contains listing privilege
            if (licenseType.isOpenAccess() || licenseType.getPrivileges().contains(IPrivilegeHolder.PRIV_LIST)) {
                query.append(licenseType.getFilterQueryPart(false));
                usedLicenseTypes.add(licenseType.getName());
                continue;
            }

            if (AccessConditionUtils.checkAccessPermission(Collections.singletonList(licenseType),
                    new HashSet<>(Collections.singletonList(licenseType.getName())), IPrivilegeHolder.PRIV_LIST, user, ipAddress, null)) {
                // If the user has an explicit permission to list a certain license type, ignore all other license types
                logger.trace("User has listing privilege for license type '{}'.", licenseType.getName());
                query.append(licenseType.getFilterQueryPart(false));
                usedLicenseTypes.add(licenseType.getName());
            } else if (!licenseType.getOverridingLicenseTypes().isEmpty()) {
                // If there are overriding license types for which the user has listing permission, ignore the current license type
                for (LicenseType overridingLicenseType : licenseType.getOverridingLicenseTypes()) {
                    if (usedLicenseTypes.contains(overridingLicenseType.getName())) {
                        continue;
                    }
                    if (AccessConditionUtils.checkAccessPermission(Collections.singletonList(overridingLicenseType),
                            new HashSet<>(Collections.singletonList(overridingLicenseType.getName())), IPrivilegeHolder.PRIV_LIST, user, ipAddress,
                            null)) {
                        query.append(overridingLicenseType.getFilterQueryPart(false));
                        usedLicenseTypes.add(overridingLicenseType.getName());
                        logger.trace("User has listing privilege for license type '{}', overriding the restriction of license type '{}'.",
                                overridingLicenseType.getName(), licenseType.getName());
                        break;
                    }
                }
            }

        }
        query.append(')');

        return query.toString();
    }

    /**
     * TODO This method might be quite expensive.
     *
     * @param searchTerms a {@link java.util.Set} object.
     * @param fulltext a {@link java.lang.String} object.
     * @param targetFragmentLength Desired (approximate) length of the text fragment.
     * @param firstMatchOnly If true, only the fragment for the first match will be returned
     * @param addFragmentIfNoMatches If true, a fragment will be added even if no term was matched
     * @should not add prefix and suffix to text
     * @should truncate string to 200 chars if no terms are given
     * @should truncate string to 200 chars if no term has been found
     * @should make terms bold if found in text
     * @should remove unclosed HTML tags
     * @should return multiple match fragments correctly
     * @should replace line breaks with spaces
     * @should add fragment if no term was matched only if so requested
     * @should highlight multi word terms while removing stopwords
     * @return a {@link java.util.List} object.
     */
    public static List<String> truncateFulltext(Set<String> searchTerms, String fulltext, int targetFragmentLength, boolean firstMatchOnly,
            boolean addFragmentIfNoMatches) {
        // logger.trace("truncateFulltext");
        if (fulltext == null) {
            throw new IllegalArgumentException("fulltext may not be null");
        }

        // Remove HTML breaks
        fulltext = Jsoup.parse(fulltext).text();
        List<String> ret = new ArrayList<>();
        String fulltextFragment = "";
        if (searchTerms != null && !searchTerms.isEmpty()) {
            for (String searchTerm : searchTerms) {
                if (searchTerm.length() == 0) {
                    continue;
                }
                searchTerm = SearchHelper.removeTruncation(searchTerm);
                //                logger.trace("term: {}", searchTerm);
                // Stopwords do not get pre-filtered out when doing a phrase search
                if (searchTerm.contains(" ")) {
                    for (String stopword : DataManager.getInstance().getConfiguration().getStopwords()) {
                        if (searchTerm.startsWith(stopword + " ") || searchTerm.endsWith(" " + stopword)) {
                            logger.trace("filtered out stopword '{}' from term '{}'", stopword, searchTerm);
                            searchTerm = searchTerm.replace(stopword, "").trim();
                        }
                    }
                }
                if (searchTerm.length() > 1 && searchTerm.endsWith("*") || searchTerm.endsWith("?")) {
                    searchTerm = searchTerm.substring(0, searchTerm.length() - 1);
                }
                if (searchTerm.length() > 1 && searchTerm.charAt(0) == '*' || searchTerm.charAt(0) == '?') {
                    searchTerm = searchTerm.substring(1);
                }
                if (searchTerm.contains("*") || searchTerm.contains("?")) {
                    fulltextFragment += " ";
                    break;
                }
                Matcher m = Pattern.compile(searchTerm.toLowerCase()).matcher(fulltext.toLowerCase());
                int lastIndex = -1;
                while (m.find()) {
                    // Skip match if it follows right after the last match
                    if (lastIndex != -1 && m.start() <= lastIndex + searchTerm.length()) {
                        continue;
                    }
                    int indexOfTerm = m.start();
                    lastIndex = m.start();

                    // fulltextFragment = getTextFragmentFromLine(fulltext, searchTerm, indexOfTerm, targetFragmentLength);
                    fulltextFragment = getTextFragmentRandomized(fulltext, searchTerm, indexOfTerm, targetFragmentLength);
                    // fulltextFragment = getTextFragmentStatic(fulltext, targetFragmentLength, fulltextFragment, searchTerm,
                    // indexOfTerm);

                    indexOfTerm = fulltextFragment.toLowerCase().indexOf(searchTerm.toLowerCase());
                    int indexOfSpace = fulltextFragment.indexOf(' ');
                    if (indexOfTerm > indexOfSpace && indexOfSpace >= 0) {
                        fulltextFragment = fulltextFragment.substring(indexOfSpace, fulltextFragment.length());
                    }

                    indexOfTerm = fulltextFragment.toLowerCase().indexOf(searchTerm.toLowerCase());

                    if (indexOfTerm < fulltextFragment.lastIndexOf(' ')) {
                        fulltextFragment = fulltextFragment.substring(0, fulltextFragment.lastIndexOf(' '));
                    }

                    indexOfTerm = fulltextFragment.toLowerCase().indexOf(searchTerm.toLowerCase());
                    if (indexOfTerm >= 0) {
                        fulltextFragment = applyHighlightingToPhrase(fulltextFragment, searchTerm);
                        fulltextFragment = replaceHighlightingPlaceholders(fulltextFragment);
                    }
                    if (StringUtils.isNotBlank(fulltextFragment)) {
                        // Check for unclosed HTML tags
                        int lastIndexOfLT = fulltextFragment.lastIndexOf('<');
                        int lastIndexOfGT = fulltextFragment.lastIndexOf('>');
                        if (lastIndexOfLT != -1 && lastIndexOfLT > lastIndexOfGT) {
                            fulltextFragment = fulltextFragment.substring(0, lastIndexOfLT).trim();
                        }
                        // fulltextFragment = fulltextFragment.replaceAll("[\\t\\n\\r]+", " ");
                        // fulltextFragment = fulltextFragment.replace("<br>", " ");
                        ret.add(fulltextFragment);
                    }
                    if (firstMatchOnly) {
                        break;
                    }
                }
            }

            // If no search term has been found (i.e. when searching for a phrase), make sure no empty string gets delivered
            if (addFragmentIfNoMatches && StringUtils.isEmpty(fulltextFragment)) {
                if (fulltext.length() > 200) {
                    fulltextFragment = fulltext.substring(0, 200);
                } else {
                    fulltextFragment = fulltext;
                }
                // fulltextFragment = fulltextFragment.replaceAll("[\\t\\n\\r]+", " ");
                fulltextFragment = fulltextFragment.replace("<br>", " ");
                ret.add(fulltextFragment);
            }
        } else {
            if (fulltext.length() > 200) {
                fulltextFragment = fulltext.substring(0, 200);
            } else {
                fulltextFragment = fulltext;
            }
            if (StringUtils.isNotBlank(fulltextFragment)) {
                // Check for unclosed HTML tags
                int lastIndexOfLT = fulltextFragment.lastIndexOf('<');
                int lastIndexOfGT = fulltextFragment.lastIndexOf('>');
                if (lastIndexOfLT != -1 && lastIndexOfLT > lastIndexOfGT) {
                    fulltextFragment = fulltextFragment.substring(0, lastIndexOfLT).trim();
                }
                // fulltextFragment = fulltextFragment.replaceAll("[\\t\\n\\r]+", " ");
                fulltextFragment = fulltextFragment.replace("<br>", " ");
                ret.add(fulltextFragment);
            }
        }

        return ret;
    }

    /**
     * Adds highlighting markup for all given terms to the phrase.
     *
     * @param phrase a {@link java.lang.String} object.
     * @param terms a {@link java.util.Set} object.
     * @should apply highlighting for all terms
     * @should skip single character terms
     * @return a {@link java.lang.String} object.
     */
    public static String applyHighlightingToPhrase(String phrase, Set<String> terms) {
        if (phrase == null) {
            throw new IllegalArgumentException("phrase may not be null");
        }
        if (terms == null) {
            throw new IllegalArgumentException("terms may not be null");
        }

        String highlightedValue = phrase;
        for (String term : terms) {
            // Highlighting single-character terms can take a long time, so skip them
            if (term.length() < 2) {
                continue;
            }
            term = SearchHelper.removeTruncation(term);
            String normalizedPhrase = normalizeString(phrase);
            String normalizedTerm = normalizeString(term);
            if (StringUtils.contains(normalizedPhrase, normalizedTerm)) {
                highlightedValue = SearchHelper.applyHighlightingToPhrase(highlightedValue, term);
                // logger.trace("highlighted value: {}", highlightedValue);
            }
        }

        return highlightedValue;
    }

    /**
     * Recursively adds highlighting markup to all occurrences of the given term in the given phrase.
     * 
     * @param phrase
     * @param term
     * @return
     * @should apply highlighting to all occurrences of term
     * @should ignore special characters
     * @should skip single character terms
     */
    static String applyHighlightingToPhrase(String phrase, String term) {
        if (phrase == null) {
            throw new IllegalArgumentException("phrase may not be null");
        }
        if (term == null) {
            throw new IllegalArgumentException("term may not be null");
        }

        // Highlighting single-character terms can take a long time, so skip them
        if (term.length() < 2) {
            return phrase;
        }

        StringBuilder sb = new StringBuilder();
        String normalizedPhrase = normalizeString(phrase);
        String normalizedTerm = normalizeString(term);
        int startIndex = normalizedPhrase.indexOf(normalizedTerm);
        if (startIndex == -1) {
            return phrase;
        }
        int endIndex = startIndex + term.length();
        String before = phrase.substring(0, startIndex);
        String highlightedTerm = applyHighlightingToTerm(phrase.substring(startIndex, endIndex));
        // logger.trace("highlighted term: {}", highlightedTerm);
        String after = phrase.substring(endIndex);

        return sb.append(applyHighlightingToPhrase(before, term)).append(highlightedTerm).append(applyHighlightingToPhrase(after, term)).toString();
    }

    /**
     * Remove any diacritic characters and replace any non.letter and non-digit characters with space
     * 
     * @param string
     * @return
     * @should preserve digits
     * @should preserve latin chars
     * @should preserve hebrew chars
     */
    static String normalizeString(String string) {
        if (string == null) {
            return null;
        }
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        string = string.toLowerCase().replaceAll("\\p{M}", "").replaceAll("[^\\p{L}0-9#]", " ");
        string = Normalizer.normalize(string, Normalizer.Form.NFC);
        return string;
    }

    /**
     * 
     * @param term
     * @param substitute
     * @return
     * @should add span correctly
     */
    static String applyHighlightingToTerm(String term) {
        return new StringBuilder(PLACEHOLDER_HIGHLIGHTING_START).append(term).append(PLACEHOLDER_HIGHLIGHTING_END).toString();
    }

    /**
     * <p>
     * replaceHighlightingPlaceholdersForHyperlinks.
     * </p>
     *
     * @param phrase a {@link java.lang.String} object.
     * @should replace placeholders with bold tags
     * @return a {@link java.lang.String} object.
     */
    public static String replaceHighlightingPlaceholdersForHyperlinks(String phrase) {
        return phrase.replace(PLACEHOLDER_HIGHLIGHTING_START, "<span style=\"color:blue\">").replace(PLACEHOLDER_HIGHLIGHTING_END, "</span>");
    }

    /**
     * <p>
     * replaceHighlightingPlaceholders.
     * </p>
     *
     * @param phrase a {@link java.lang.String} object.
     * @should replace placeholders with html tags
     * @return a {@link java.lang.String} object.
     */
    public static String replaceHighlightingPlaceholders(String phrase) {
        return phrase.replace(PLACEHOLDER_HIGHLIGHTING_START, "<span class=\"search-list--highlight\">")
                .replace(PLACEHOLDER_HIGHLIGHTING_END, "</span>");
    }

    /**
     * <p>
     * removeHighlightingTags.
     * </p>
     *
     * @param phrase a {@link java.lang.String} object.
     * @return Given phrase without the highlighting html tags
     * @should remove html tags
     */
    public static String removeHighlightingTags(String phrase) {
        return phrase.replace("<span class=\"search-list--highlight\">", "").replace("</span>", "");
    }

    /**
     * @param fulltext
     * @param targetFragmentLength
     * @param fulltextFragment
     * @param searchTerm
     * @param indexOfTerm
     * @return
     */
    @SuppressWarnings("unused")
    private static String getTextFragmentStatic(String fulltext, int targetFragmentLength, String fulltextFragment, String searchTerm,
            int indexOfTerm) {
        if (fulltextFragment.length() == 0) {
            int start = 0;
            int end = fulltext.toLowerCase().length();
            int halfLength = targetFragmentLength / 2;
            // Use the position first found search term to determine the displayed fulltext fragment
            if (indexOfTerm > halfLength) {
                start = indexOfTerm - halfLength;
            }
            if (end - (indexOfTerm + searchTerm.length()) > halfLength) {
                end = indexOfTerm + searchTerm.length() + halfLength;
            }
            fulltextFragment = fulltext.substring(start, end);
        }
        return fulltextFragment;
    }

    /**
     * @param fulltext
     * @param searchTerm
     * @param indexOfTerm
     * @param halfLength
     * @return
     */
    private static String getTextFragmentRandomized(String fulltext, String searchTerm, int indexOfTerm, int fragmentLength) {

        int minDistanceFromEdge = 10;

        int fragmentStartIndexFloor = Math.max(0, indexOfTerm + minDistanceFromEdge - (fragmentLength - searchTerm.length()));
        int fragmentStartIndexCeil = Math.max(0, indexOfTerm - minDistanceFromEdge);

        int fragmentStartIndex = fragmentStartIndexFloor + random.nextInt(Math.max(1, fragmentStartIndexCeil - fragmentStartIndexFloor));
        int fragmentEndIndex = Math.min(fulltext.length(), fragmentStartIndex + fragmentLength);

        return fulltext.substring(fragmentStartIndex, fragmentEndIndex);
    }

    /**
     * @param fulltext
     * @param searchTerm
     * @param indexOfTerm
     * @param halfLength
     * @return
     */
    @SuppressWarnings("unused")
    private static String getTextFragmentFromLine(String fulltext, String searchTerm, int indexOfTerm, int fragmentLength) {
        String fulltextFragment;
        String stringBefore = fulltext.substring(0, indexOfTerm);
        String stringAfter = fulltext.substring(indexOfTerm + searchTerm.length());
        int halfLength = fragmentLength / 2;

        int lineStartIndex = Math.max(0, Math.max(indexOfTerm - halfLength, stringBefore.lastIndexOf(System.lineSeparator())));
        int lineEndIndex = Math.min(fulltext.length(),
                Math.min(indexOfTerm + halfLength, indexOfTerm + searchTerm.length() + stringAfter.indexOf(System.lineSeparator())));

        fulltextFragment = fulltext.substring(lineStartIndex, lineEndIndex);
        return fulltextFragment;
    }

    /**
     * Returns a list of values for a given facet field and the given query.
     *
     * @param query a {@link java.lang.String} object.
     * @param facetFieldName a {@link java.lang.String} object.
     * @param facetMinCount a int.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static List<String> getFacetValues(String query, String facetFieldName, int facetMinCount)
            throws PresentationException, IndexUnreachableException {
        return getFacetValues(query, facetifyField(facetFieldName), null, facetMinCount);
    }

    /**
     * Returns a list of values for a given facet field and the given query.
     *
     * @param query a {@link java.lang.String} object.
     * @param facetFieldName a {@link java.lang.String} object.
     * @param facetMinCount a int.
     * @param facetPrefix The facet field value must start with these characters. Ignored if null or blank
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static List<String> getFacetValues(String query, String facetFieldName, String facetPrefix, int facetMinCount)
            throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query may not be null or empty");
        }
        if (StringUtils.isEmpty(facetFieldName)) {
            throw new IllegalArgumentException("facetFieldName may not be null or empty");
        }

        QueryResponse resp = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(query, null, Collections.singletonList(facetFieldName), facetMinCount, facetPrefix, false);
        FacetField facetField = resp.getFacetField(facetFieldName);
        List<String> ret = new ArrayList<>(facetField.getValueCount());
        for (Count count : facetField.getValues()) {
            if (StringUtils.isNotEmpty(count.getName()) && count.getCount() >= facetMinCount) {
                if (count.getName().startsWith("")) {
                    continue;
                }
                ret.add(count.getName());
            }
        }

        return ret;
    }

    /**
     * 
     * @param bmfc
     * @param startsWith
     * @param filterQuery
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static int getFilteredTermsCount(BrowsingMenuFieldConfig bmfc, String startsWith, String filterQuery)
            throws PresentationException, IndexUnreachableException {
        if (bmfc == null) {
            throw new IllegalArgumentException("bmfc may not be null");
        }

        logger.trace("getFilteredTermsCount: {}", bmfc.getField());
        List<StringPair> sortFields =
                StringUtils.isEmpty(bmfc.getSortField()) ? null : Collections.singletonList(new StringPair(bmfc.getSortField(), "asc"));
        QueryResponse resp = getFilteredTermsFromIndex(bmfc, startsWith, filterQuery, sortFields, 0, 0);
        logger.trace("getFilteredTermsCount hits: {}", resp.getResults().getNumFound());

        if (bmfc.getField() == null) {
            return 0;
        }

        int ret = 0;
        String facetField = SearchHelper.facetifyField(bmfc.getField());
        for (Count count : resp.getFacetField(facetField).getValues()) {
            if (count.getCount() == 0) {
                continue;
            }
            if (StringUtils.isNotEmpty(startsWith) && !StringUtils.startsWithIgnoreCase(count.getName(), startsWith.toLowerCase())) {
                continue;
            }
            ret++;

        }
        logger.debug("getFilteredTermsCount result: {}", ret);
        return ret;
    }

    /**
     * Returns a list of index terms for the given field name. This method uses the slower doc search instead of term search, but can be filtered with
     * a query.
     *
     * @param bmfc a {@link io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig} object.
     * @param startsWith a {@link java.lang.String} object.
     * @param filterQuery a {@link java.lang.String} object.
     * @param comparator a {@link java.util.Comparator} object.
     * @param aggregateHits a boolean.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should be thread safe when counting terms
     */
    public static List<BrowseTerm> getFilteredTerms(BrowsingMenuFieldConfig bmfc, String startsWith, String filterQuery, int start, int rows,
            Comparator<BrowseTerm> comparator, boolean aggregateHits) throws PresentationException, IndexUnreachableException {
        if (bmfc == null) {
            throw new IllegalArgumentException("bmfc may not be null");
        }

        logger.trace("getFilteredTerms: {}", bmfc.getField());
        List<BrowseTerm> ret = new ArrayList<>();
        ConcurrentMap<String, BrowseTerm> terms = new ConcurrentHashMap<>();

        // If only browsing top level documents, use faceting for faster performance
        if (bmfc.isRecordsAndAnchorsOnly()) {
            rows = 0;
        }

        try {
            List<StringPair> sortFields =
                    StringUtils.isEmpty(bmfc.getSortField()) ? null : Collections.singletonList(new StringPair(bmfc.getSortField(), "asc"));
            QueryResponse resp = getFilteredTermsFromIndex(bmfc, startsWith, filterQuery, sortFields, start, rows);
            // logger.debug("getFilteredTerms hits: {}", resp.getResults().getNumFound());
            if ("0-9".equals(startsWith)) {
                // TODO Is this still necessary?
                // Numerical filtering
                Pattern p = Pattern.compile("[\\d]");
                // Use hits (if sorting field is provided)
                for (SolrDocument doc : resp.getResults()) {
                    Collection<Object> termList = doc.getFieldValues(bmfc.getField());
                    String sortTerm = (String) doc.getFieldValue(bmfc.getSortField());
                    Set<String> usedTermsInCurrentDoc = new HashSet<>();
                    for (Object o : termList) {
                        String term = String.valueOf(o);
                        // Only add to hit count if the same string is not in the same doc
                        if (usedTermsInCurrentDoc.contains(term)) {
                            continue;
                        }
                        String termStart = term;
                        if (termStart.length() > 1) {
                            termStart = term.substring(0, 1);
                        }
                        String compareTerm = termStart;
                        if (StringUtils.isNotEmpty(sortTerm)) {
                            compareTerm = sortTerm;
                        }
                        Matcher m = p.matcher(compareTerm);
                        if (m.find()) {
                            BrowseTerm browseTerm = terms.get(term);
                            if (browseTerm == null) {
                                browseTerm = new BrowseTerm(term, sortTerm, bmfc.isTranslate() ? ViewerResourceBundle.getTranslations(term) : null);
                                terms.put(term, browseTerm);
                            }
                            sortTerm = null; // only use the sort term for the first term
                            browseTerm.addToHitCount(1);
                            usedTermsInCurrentDoc.add(term);
                        }
                    }
                }
            } else {
                String facetField = SearchHelper.facetifyField(bmfc.getField());
                if (resp.getResults().isEmpty() && resp.getFacetField(facetField) != null) {
                    // If only browsing records and anchors, use faceting
                    logger.trace("using faceting: {}", facetField);
                    for (Count count : resp.getFacetField(facetField).getValues()) {
                        terms.put(count.getName(),
                                new BrowseTerm(count.getName(), null,
                                        bmfc.isTranslate() ? ViewerResourceBundle.getTranslations(count.getName()) : null)
                                                .setHitCount(count.getCount()));
                    }
                } else {
                    // Without filtering or using alphabetical filtering
                    // Parallel processing of hits (if sorting field is provided), requires compiler level 1.8
                    //                ((List<SolrDocument>) resp.getResults()).parallelStream()
                    //                        .forEach(doc -> processSolrResult(doc, bmfc, startsWith, terms, aggregateHits));

                    // Sequential processing (doesn't break the sorting done by Solr)
                    for (SolrDocument doc : resp.getResults()) {
                        processSolrResult(doc, bmfc, startsWith, terms, aggregateHits);
                    }
                }
            }
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            throw new PresentationException(e.getMessage());
        }

        if (!terms.isEmpty()) {
            ret = new ArrayList<>(terms.values());
            if (comparator != null) {
                Collections.sort(ret, comparator);
            }
        }

        logger.debug("getFilteredTerms end: {} terms found.", ret.size());
        return ret;
    }

    /**
     * 
     * @param bmfc
     * @param startsWith
     * @param filterQuery
     * @param sortFields
     * @param start
     * @param rows
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should contain facets for the main field
     */
    static QueryResponse getFilteredTermsFromIndex(BrowsingMenuFieldConfig bmfc, String startsWith, String filterQuery, List<StringPair> sortFields,
            int start, int rows)
            throws PresentationException, IndexUnreachableException {
        List<String> fields = new ArrayList<>(3);
        fields.add(SolrConstants.PI_TOPSTRUCT);
        fields.add(bmfc.getField());

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append('+');
        // Only search via the sorting field if not doing a wildcard search
        if (StringUtils.isNotEmpty(bmfc.getSortField())) {
            sbQuery.append(bmfc.getSortField());
            fields.add(bmfc.getSortField());
        } else {
            sbQuery.append(bmfc.getField());
        }
        sbQuery.append(":[* TO *] ");
        if (bmfc.isRecordsAndAnchorsOnly()) {
            sbQuery.append(ALL_RECORDS_QUERY);
        }

        List<String> filterQueries = new ArrayList<>();
        if (StringUtils.isNotEmpty(filterQuery)) {
            filterQueries.add(filterQuery);
        }

        // Add configured filter queries
        if (!bmfc.getFilterQueries().isEmpty()) {
            filterQueries.addAll(bmfc.getFilterQueries());
        }

        // logger.trace("getFilteredTermsFromIndex startsWith: {}", startsWith);
        String query = buildFinalQuery(sbQuery.toString(), false);
        logger.trace("getFilteredTermsFromIndex query: {}", query);
        if (logger.isTraceEnabled()) {
            for (String fq : filterQueries) {
                logger.trace("getFilteredTermsFromIndex filter query: {}", fq);
            }
        }

        String facetField = SearchHelper.facetifyField(bmfc.getField());
        List<String> facetFields = new ArrayList<>();
        facetFields.add(facetField);

        Map<String, String> params = new HashMap<>();
        if (DataManager.getInstance().getConfiguration().isGroupDuplicateHits()) {
            params.put(GroupParams.GROUP, "true");
            params.put(GroupParams.GROUP_MAIN, "true");
            params.put(GroupParams.GROUP_FIELD, SolrConstants.GROUPFIELD);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("row count: {}", DataManager.getInstance().getSearchIndex().getHitCount(query, filterQueries));
        }

        // Faceting (no rows requested or expected row count too high)
        if (rows == 0 || DataManager.getInstance().getSearchIndex().getHitCount(query, filterQueries) > DataManager.getInstance()
                .getConfiguration()
                .getBrowsingMenuIndexSizeThreshold()) {
            return DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(query, filterQueries, facetFields, 1, startsWith, false);
        }

        // Docs (required for correct mapping of sorting vs displayed term names, but may time out if doc count is too high)
        return DataManager.getInstance().getSearchIndex().search(query, start, rows, sortFields, facetFields, fields, filterQueries, params);
    }

    /**
     * Extracts terms from the given Solr document and adds them to the terms map, if applicable. Can be executed in parallel, provided
     * <code>terms</code> and <code>usedTerms</code> are synchronized.
     *
     * @param doc
     * @param bmfc
     * @param startsWith
     * @param terms Map of terms collected so far.
     * @param aggregateHits
     */
    private static void processSolrResult(SolrDocument doc, BrowsingMenuFieldConfig bmfc, String startsWith,
            ConcurrentMap<String, BrowseTerm> terms, boolean aggregateHits) {
        // logger.trace("processSolrResult thread {}", Thread.currentThread().getId());
        Collection<Object> termList = doc.getFieldValues(bmfc.getField());
        if (termList == null) {
            return;
        }

        String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        String sortTerm = (String) doc.getFieldValue(bmfc.getSortField());
        Set<String> usedTermsInCurrentDoc = new HashSet<>();
        int count = -1;
        for (Object o : termList) {
            count++;
            String term = String.valueOf(o);
            if (StringUtils.isEmpty(term)) {
                continue;
            }

            // Only add to hit count if the same string is not in the same doc
            if (usedTermsInCurrentDoc.contains(term)) {
                continue;
            }

            String compareTerm = term;
            if (StringUtils.isNotEmpty(sortTerm) && count == 0) {
                compareTerm = sortTerm;
            }
            if (StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars())) {
                // Exclude leading characters from filters explicitly configured to be ignored
                compareTerm = BrowseTermComparator.normalizeString(compareTerm,
                        DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars()).trim();
            }
            if (StringUtils.isNotEmpty(startsWith) && !"-".equals(startsWith) && !StringUtils.startsWithIgnoreCase(compareTerm, startsWith)) {
                continue;
            }

            BrowseTerm browseTerm = terms.get(term);
            if (browseTerm == null) {
                synchronized (lock) {
                    // Another thread may have added this term by now
                    if (!terms.containsKey(term)) {
                        // logger.trace("Adding term: {}, compareTerm: {}, sortTerm: {}, translate: {}", term, compareTerm, sortTerm, bmfc.isTranslate());
                        terms.put(term, new BrowseTerm(term, sortTerm, bmfc.isTranslate() ? ViewerResourceBundle.getTranslations(term) : null));
                    }
                }
                browseTerm = terms.get(term);
            }

            sortTerm = null; // only use the sort term for the first term

            // If using aggregated search, do not count instances of records that already have been counted
            if (aggregateHits && browseTerm.getPiList().contains(pi)) {
                continue;
            }

            browseTerm.addToHitCount(1);
            browseTerm.getPiList().add(pi);
            usedTermsInCurrentDoc.add(term);
        }
    }

    /**
     * Parses the given Solr query for field values and returns them as a set of strings.
     *
     * @param query a {@link java.lang.String} object.
     * @param discriminatorValue a {@link java.lang.String} object.
     * @should extract all values from query except from NOT blocks
     * @should handle multiple phrases in query correctly
     * @should skip discriminator value
     * @should not remove truncation
     * @should throw IllegalArgumentException if query is null
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, Set<String>> extractSearchTermsFromQuery(String query, String discriminatorValue) {
        if (query == null) {
            throw new IllegalArgumentException("query may not be null");
        }
        Map<String, Set<String>> ret = new HashMap<>();

        Set<String> stopwords = DataManager.getInstance().getConfiguration().getStopwords();
        // Do not extract a currently set discriminator value
        if (StringUtils.isNotEmpty(discriminatorValue)) {
            stopwords.add(discriminatorValue);
        }
        // Remove all NOT(*) parts
        Matcher mNot = patternNotBrackets.matcher(query);
        while (mNot.find()) {
            query = query.replace(query.substring(mNot.start(), mNot.end()), "");
        }

        // Remove parentheses, ANDs and ORs
        query = query.replace("(", "").replace(")", "").replace(" AND ", " ").replace(" OR ", " ");

        // Extract phrases and add them directly
        {
            // Use a copy of the query because the original query gets shortened after every match, causing an IOOBE eventually
            String queryCopy = query;
            Matcher mPhrases = patternPhrase.matcher(queryCopy);
            while (mPhrases.find()) {
                String phrase = queryCopy.substring(mPhrases.start(), mPhrases.end());
                String[] phraseSplit = phrase.split(":");
                String field = phraseSplit[0];
                if (SolrConstants.SUPERDEFAULT.equals(field)) {
                    field = SolrConstants.DEFAULT;
                } else if (SolrConstants.SUPERFULLTEXT.equals(field)) {
                    field = SolrConstants.FULLTEXT;
                } else if (SolrConstants.SUPERUGCTERMS.equals(field)) {
                    field = SolrConstants.UGCTERMS;
                } else if (field.endsWith(SolrConstants._UNTOKENIZED)) {
                    field = field.substring(0, field.length() - SolrConstants._UNTOKENIZED.length());
                }
                String phraseWoQuot = phraseSplit[1].replace("\"", "");
                if (phraseWoQuot.length() > 0 && !stopwords.contains(phraseWoQuot)) {
                    if (ret.get(field) == null) {
                        ret.put(field, new HashSet<String>());
                    }
                    logger.trace("phraseWoQuot: {}", phraseWoQuot);
                    ret.get(field).add(phraseWoQuot);
                }
                query = query.replace(phrase, "");
            }
        }

        // Split into FIELD:value pairs
        // query = query.replace("-", " ");
        String[] querySplit = query.split(SEARCH_TERM_SPLIT_REGEX);
        String currentField = null;
        for (String s : querySplit) {
            s = s.trim();
            // logger.trace("term: {}", s);
            // Extract the value part
            if (s.contains(":") && !s.startsWith(":")) {
                int split = s.indexOf(':');
                String field = s.substring(0, split);
                String value = s.length() > split ? s.substring(split + 1) : null;
                if (StringUtils.isNotBlank(value)) {
                    currentField = field;
                    if (SolrConstants.SUPERDEFAULT.equals(currentField)) {
                        currentField = SolrConstants.DEFAULT;
                    } else if (SolrConstants.SUPERFULLTEXT.equals(currentField)) {
                        currentField = SolrConstants.FULLTEXT;
                    } else if (SolrConstants.SUPERUGCTERMS.equals(currentField)) {
                        currentField = SolrConstants.UGCTERMS;
                    }
                    if (currentField.endsWith(SolrConstants._UNTOKENIZED)) {
                        currentField = currentField.substring(0, currentField.length() - SolrConstants._UNTOKENIZED.length());
                    }
                    // Remove quotation marks from phrases
                    // logger.trace("field: {}", field);
                    // logger.trace("value: {}", value);
                    if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                        value = value.replace("\"", "");
                    }
                    if (value.length() > 0 && !stopwords.contains(value)) {
                        if (ret.get(currentField) == null) {
                            ret.put(currentField, new HashSet<String>());
                        }
                        ret.get(currentField).add(value);
                    }
                }
            } else if (s.length() > 0 && !stopwords.contains(s)) {
                // single values w/o a field
                if (currentField == null) {
                    currentField = SolrConstants.DEFAULT;
                } else if (currentField.endsWith(SolrConstants._UNTOKENIZED)) {
                    currentField = currentField.substring(0, currentField.length() - SolrConstants._UNTOKENIZED.length());
                }
                if (ret.get(currentField) == null) {
                    ret.put(currentField, new HashSet<String>());
                }
                ret.get(currentField).add(s);
            }
        }

        return ret;
    }

    public static String removeTruncation(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        // Remove left truncation
        if (value.charAt(0) == '*' && value.length() > 1) {
            value = value.substring(1);
        }
        // Remove right truncation
        if (value.charAt(value.length() - 1) == '*' && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }

    /**
     * <p>
     * generateQueryParams.
     * </p>
     *
     * @return a {@link java.util.Map} object.
     * @should return empty map if search hit aggregation on
     */
    public static Map<String, String> generateQueryParams() {
        Map<String, String> params = new HashMap<>();
        if (DataManager.getInstance().getConfiguration().isAggregateHits()) {
            return params;
        }
        if (DataManager.getInstance().getConfiguration().isGroupDuplicateHits()) {
            // Add grouping by GROUPFIELD (to avoid duplicates among metadata search hits)
            params.put(GroupParams.GROUP, "true");
            params.put(GroupParams.GROUP_MAIN, "true");
            params.put(GroupParams.GROUP_FIELD, SolrConstants.GROUPFIELD);
        }
        if (DataManager.getInstance().getConfiguration().isBoostTopLevelDocstructs()) {
            // Add a boost query to promote anchors and works to the top of the list (Extended Dismax query parser is
            // required for this)
            params.put("defType", "edismax");
            params.put("bq", "ISANCHOR:true^10 OR ISWORK:true^5");
        }

        return params;
    }

    /**
     * <p>
     * facetifyList.
     * </p>
     *
     * @param sourceList a {@link java.util.List} object.
     * @should facetify correctly
     * @return a {@link java.util.List} object.
     */
    public static List<String> facetifyList(List<String> sourceList) {
        if (sourceList == null) {
            return null;
        }

        List<String> ret = new ArrayList<>(sourceList.size());
        for (String s : sourceList) {
            String fieldName = facetifyField(s);
            if (fieldName != null) {
                ret.add(fieldName);
            }
        }
        return ret;
    }

    /**
     * <p>
     * facetifyField.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @should facetify correctly
     * @should leave bool fields unaltered
     * @should leave year month day fields unaltered
     */
    public static String facetifyField(String fieldName) {
        if (fieldName != null && (fieldName.startsWith("BOOL_") || fieldName.equals(SolrConstants._CALENDAR_YEAR)
                || fieldName.equals(SolrConstants._CALENDAR_MONTH) || fieldName.equals(SolrConstants._CALENDAR_DAY))) {
            return fieldName;
        }
        return adaptField(fieldName, "FACET_");
    }

    /**
     * <p>
     * sortifyField.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @should sortify correctly
     */
    public static String sortifyField(String fieldName) {
        return adaptField(fieldName, "SORT_");
    }

    /**
     * <p>
     * boolifyField.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String boolifyField(String fieldName) {
        return adaptField(fieldName, "BOOL_");
    }

    /**
     * 
     * @param fieldName
     * @return
     */
    public static String normalizeField(String fieldName) {
        return adaptField(fieldName, null);
    }

    /**
     * 
     * @param fieldName
     * @param prefix
     * @return modified field name
     * @should apply prefix correctly
     * @should not apply prefix to regular fields if empty
     * @should not apply facet prefix to calendar fields
     * @should remove untokenized correctly
     */
    static String adaptField(String fieldName, String prefix) {
        if (fieldName == null) {
            return null;
        }
        if (prefix == null) {
            prefix = "";
        }

        switch (fieldName) {
            case SolrConstants.DC:
            case SolrConstants.DOCSTRCT:
            case SolrConstants.DOCSTRCT_SUB:
            case SolrConstants.DOCSTRCT_TOP:
                return prefix + fieldName;
            case SolrConstants._CALENDAR_YEAR:
            case SolrConstants._CALENDAR_MONTH:
            case SolrConstants._CALENDAR_DAY:
                if ("SORT_".equals(prefix)) {
                    return "SORTNUM_" + fieldName;
                }
            default:
                if (StringUtils.isNotEmpty(prefix)) {
                    if (fieldName.startsWith("MD_")) {
                        fieldName = fieldName.replace("MD_", prefix);
                    } else if (fieldName.startsWith("MD2_")) {
                        fieldName = fieldName.replace("MD2_", prefix);
                    } else if (fieldName.startsWith("MDNUM_")) {
                        if ("SORT_".equals(prefix)) {
                            fieldName = fieldName.replace("MDNUM_", "SORTNUM_");
                        } else {
                            fieldName = fieldName.replace("MDNUM_", prefix);
                        }
                    } else if (fieldName.startsWith("NE_")) {
                        fieldName = fieldName.replace("NE_", prefix);
                    } else if (fieldName.startsWith("BOOL_")) {
                        fieldName = fieldName.replace("BOOL_", prefix);
                    } else if (fieldName.startsWith("SORT_")) {
                        fieldName = fieldName.replace("SORT_", prefix);
                    }
                }
                fieldName = fieldName.replace(SolrConstants._UNTOKENIZED, "");
                return fieldName;
        }
    }

    /**
     * <p>
     * defacetifyField.
     * </p>
     *
     * @param fieldName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @should defacetify correctly
     */
    public static String defacetifyField(String fieldName) {
        if (fieldName == null) {
            return null;
        }

        switch (fieldName) {
            case SolrConstants.FACET_DC:
            case "FACET_" + SolrConstants.DOCSTRCT:
            case "FACET_" + SolrConstants.DOCSTRCT_SUB:
            case "FACET_" + SolrConstants.DOCSTRCT_TOP:
            case "FACET_" + SolrConstants._CALENDAR_YEAR:
            case "FACET_" + SolrConstants._CALENDAR_MONTH:
            case "FACET_" + SolrConstants._CALENDAR_DAY:
                return fieldName.substring(6);
            default:
                if (fieldName.startsWith("FACET_")) {
                    return fieldName.replace("FACET_", "MD_");
                }
                return fieldName;
        }
    }

    /**
     * Creates a Solr expand query string out of lists of fields and terms.
     *
     * @param fields a {@link java.util.List} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param phraseSearch If true, quotation marks are added to terms
     * @should generate query correctly
     * @should return empty string if no fields match
     * @should skip reserved fields
     * @should escape reserved characters
     * @should not escape asterisks
     * @should not escape truncation
     * @should add quotation marks if phraseSearch is true
     * @return a {@link java.lang.String} object.
     */
    public static String generateExpandQuery(List<String> fields, Map<String, Set<String>> searchTerms, boolean phraseSearch) {
        logger.trace("generateExpandQuery");
        StringBuilder sbOuter = new StringBuilder();
        if (!searchTerms.isEmpty()) {
            logger.trace("fields: {}", fields.toString());
            logger.trace("searchTerms: {}", searchTerms.toString());
            boolean moreThanOne = false;
            for (String field : fields) {
                // Skip fields that exist in all child docs (e.g. PI_TOPSTRUCT) so that searches within a record don't return
                // every single doc
                switch (field) {
                    case SolrConstants.PI_TOPSTRUCT:
                    case SolrConstants.PI_ANCHOR:
                    case SolrConstants.DC:
                    case SolrConstants.DOCSTRCT:
                        continue;
                    default:
                        if (field.startsWith(SolrConstants.GROUPID_)) {
                            continue;
                        }
                }
                Set<String> terms = searchTerms.get(field);
                if (terms == null || terms.isEmpty()) {
                    continue;
                }
                if (sbOuter.length() == 0) {
                    sbOuter.append(" +(");
                }
                if (moreThanOne) {
                    sbOuter.append(" OR ");
                }
                StringBuilder sbInner = new StringBuilder();
                boolean multipleTerms = false;
                for (String term : terms) {
                    if (sbInner.length() > 0) {
                        sbInner.append(" OR ");
                        multipleTerms = true;
                    }
                    if (!"*".equals(term)) {
                        term = ClientUtils.escapeQueryChars(term);
                        term = term.replace("\\*", "*");
                        if (phraseSearch) {
                            term = "\"" + term + "\"";
                        }
                    }
                    sbInner.append(term);
                }
                sbOuter.append(field).append(":");
                if (multipleTerms) {
                    sbOuter.append('(');
                }
                sbOuter.append(sbInner.toString());
                if (multipleTerms) {
                    sbOuter.append(')');
                }
                moreThanOne = true;
            }
            if (sbOuter.length() > 0) {
                sbOuter.append(')');
            }
        }

        return sbOuter.toString();
    }

    /**
     * Creates a Solr expand query string out of advanced search query item groups.
     *
     * @param groups a {@link java.util.List} object.
     * @param advancedSearchGroupOperator a int.
     * @should generate query correctly
     * @should skip reserved fields
     * @return a {@link java.lang.String} object.
     */
    public static String generateAdvancedExpandQuery(List<SearchQueryGroup> groups, int advancedSearchGroupOperator) {
        logger.trace("generateAdvancedExpandQuery");
        if (groups == null || groups.isEmpty()) {
            return "";
        }
        StringBuilder sbOuter = new StringBuilder();

        for (SearchQueryGroup group : groups) {
            StringBuilder sbGroup = new StringBuilder();

            // Identify any fields that only exist in page or UGC docs and enable the page search mode
            boolean orMode = false;
            for (SearchQueryItem item : group.getQueryItems()) {
                if (item.getField() == null) {
                    continue;
                }
                switch (item.getField()) {
                    case SolrConstants.FULLTEXT:
                    case SolrConstants.UGCTERMS:
                    case SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS:
                        orMode = true;
                        break;
                }
            }

            for (SearchQueryItem item : group.getQueryItems()) {
                if (item.getField() == null) {
                    continue;
                }
                logger.trace("item field: " + item.getField());
                // Skip fields that exist in all child docs (e.g. PI_TOPSTRUCT) so that searches within a record don't
                // return every single doc
                switch (item.getField()) {
                    case SolrConstants.PI_TOPSTRUCT:
                    case SolrConstants.PI_ANCHOR:
                    case SolrConstants.DC:
                    case SolrConstants.DOCSTRCT:
                    case SolrConstants.BOOKMARKS:
                        continue;
                    default:
                        if (item.getField().startsWith(SolrConstants.GROUPID_)) {
                            continue;
                        }
                }
                String itemQuery = item.generateQuery(new HashSet<String>(), false);
                if (StringUtils.isNotEmpty(itemQuery)) {
                    if (sbGroup.length() > 0) {
                        if (orMode) {
                            // When also searching in page document fields, the operator must be 'OR'
                            sbGroup.append(" OR ");
                        } else {
                            sbGroup.append(' ').append(group.getOperator().name()).append(' ');
                        }
                    }
                    sbGroup.append(itemQuery);
                }
            }
            if (sbGroup.length() > 0) {
                if (sbOuter.length() > 0) {
                    switch (advancedSearchGroupOperator) {
                        case 0:
                            sbOuter.append(" AND ");
                            break;
                        case 1:
                            sbOuter.append(" OR ");
                            break;
                        default:
                            sbOuter.append(" OR ");
                            break;
                    }
                }
                sbOuter.append('(').append(sbGroup).append(')');
            }
        }
        if (sbOuter.length() > 0) {
            return " +(" + sbOuter.toString() + ')';
        }

        return "";
    }

    /**
     * <p>
     * getExpandQueryFieldList.
     * </p>
     *
     * @param searchType a int.
     * @param searchFilter a {@link io.goobi.viewer.model.search.SearchFilter} object.
     * @param queryGroups a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getExpandQueryFieldList(int searchType, SearchFilter searchFilter, List<SearchQueryGroup> queryGroups) {
        List<String> ret = new ArrayList<>();
        // logger.trace("searchType: {}", searchType);
        switch (searchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                if (queryGroups != null && !queryGroups.isEmpty()) {
                    for (SearchQueryGroup group : queryGroups) {
                        for (SearchQueryItem item : group.getQueryItems()) {
                            if (SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS.equals(item.getField())) {
                                if (!ret.contains(SolrConstants.DEFAULT)) {
                                    ret.add(SolrConstants.DEFAULT);
                                }
                                if (!ret.contains(SolrConstants.FULLTEXT)) {
                                    ret.add(SolrConstants.FULLTEXT);
                                }
                                if (!ret.contains(SolrConstants.NORMDATATERMS)) {
                                    ret.add(SolrConstants.NORMDATATERMS);
                                }
                                if (!ret.contains(SolrConstants.UGCTERMS)) {
                                    ret.add(SolrConstants.UGCTERMS);
                                }
                                if (!ret.contains(SolrConstants.CMS_TEXT_ALL)) {
                                    ret.add(SolrConstants.CMS_TEXT_ALL);
                                }
                            } else if (SolrConstants.DEFAULT.equals(item.getField())
                                    || SolrConstants.SUPERDEFAULT.equals(item.getField()) && !ret.contains(SolrConstants.DEFAULT)) {
                                ret.add(SolrConstants.DEFAULT);
                            } else if (SolrConstants.FULLTEXT.equals(item.getField())
                                    || SolrConstants.SUPERFULLTEXT.equals(item.getField()) && !ret.contains(SolrConstants.FULLTEXT)) {
                                ret.add(SolrConstants.FULLTEXT);
                            } else if (SolrConstants.UGCTERMS.equals(item.getField())
                                    || SolrConstants.SUPERUGCTERMS.equals(item.getField()) && !ret.contains(SolrConstants.UGCTERMS)) {
                                ret.add(SolrConstants.UGCTERMS);
                            } else if (SolrConstants.CMS_TEXT_ALL.equals(item.getField()) && !ret.contains(SolrConstants.CMS_TEXT_ALL)) {
                                ret.add(SolrConstants.CMS_TEXT_ALL);
                            } else if (!ret.contains(item.getField())) {
                                ret.add(item.getField());
                            }
                        }
                    }
                }
                break;
            case SearchHelper.SEARCH_TYPE_TIMELINE:
                ret.add(SolrConstants.DEFAULT);
                // TODO
                break;
            case SearchHelper.SEARCH_TYPE_CALENDAR:
                // ret.add(SolrConstants.DEFAULT);
                ret.add(SolrConstants._CALENDAR_DAY);
                ret.add(SolrConstants._CALENDAR_MONTH);
                ret.add(SolrConstants._CALENDAR_YEAR);
                break;
            default:
                if (searchFilter == null || searchFilter.equals(SEARCH_FILTER_ALL)) {
                    // No filters defined or ALL: use DEFAULT + FULLTEXT + UGCTERMS
                    ret.add(SolrConstants.DEFAULT);
                    ret.add(SolrConstants.FULLTEXT);
                    ret.add(SolrConstants.NORMDATATERMS);
                    ret.add(SolrConstants.UGCTERMS);
                    ret.add(SolrConstants.CMS_TEXT_ALL);
                    ret.add(SolrConstants._CALENDAR_DAY);
                } else {
                    ret.add(searchFilter.getField());
                }
                break;
        }

        return ret;
    }

    /**
     * <p>
     * prepareQuery.
     * </p>
     *
     * @param query a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String prepareQuery(String query) {
        StringBuilder sbQuery = new StringBuilder();
        if (StringUtils.isNotEmpty(query)) {
            sbQuery.append('(').append(query).append(')');
        } else {
            // Collection browsing (no search query)
            String docstructWhitelistFilterQuery = getDocstrctWhitelistFilterQuery();
            if (StringUtils.isNotEmpty(docstructWhitelistFilterQuery)) {
                sbQuery.append(docstructWhitelistFilterQuery);
            } else {
                sbQuery.append(ALL_RECORDS_QUERY);
            }

        }

        return sbQuery.toString();
    }

    /**
     * Puts non-empty queries into parentheses and replaces empty queries with a top level record-only query (for collection listing).
     *
     * @param query a {@link java.lang.String} object.
     * @param docstructWhitelistFilterQuery a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @should prepare non-empty queries correctly
     * @should prepare empty queries correctly
     */
    public static String prepareQuery(String query, String docstructWhitelistFilterQuery) {
        StringBuilder sbQuery = new StringBuilder();
        if (StringUtils.isNotEmpty(query)) {
            sbQuery.append('(').append(query).append(')');
        } else {
            // Collection browsing (no search query)
            if (StringUtils.isNotEmpty(docstructWhitelistFilterQuery)) {
                sbQuery.append(docstructWhitelistFilterQuery);
            } else {
                sbQuery.append(SearchHelper.ALL_RECORDS_QUERY);
            }

        }

        return sbQuery.toString();
    }

    /**
     * Constructs the complete query using the raw query and adding all available suffixes.
     *
     * @param rawQuery a {@link java.lang.String} object.
     * @param aggregateHits a boolean.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * 
     */
    public static String buildFinalQuery(String rawQuery, boolean aggregateHits) throws IndexUnreachableException {
        return buildFinalQuery(rawQuery, aggregateHits, null);
    }

    /**
     * Constructs the complete query using the raw query and adding all available suffixes.
     *
     * @param rawQuery a {@link java.lang.String} object.
     * @param aggregateHits a boolean.
     * @param nh
     * @param request
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should add join statement if aggregateHits true
     * @should not add join statement if aggregateHits false
     * @should remove existing join statement
     */
    public static String buildFinalQuery(String rawQuery, boolean aggregateHits, HttpServletRequest request) throws IndexUnreachableException {
        if (rawQuery == null) {
            throw new IllegalArgumentException("rawQuery may not be null");
        }

        logger.trace("rawQuery: {}", rawQuery);
        StringBuilder sbQuery = new StringBuilder();
        if (rawQuery.contains(AGGREGATION_QUERY_PREFIX)) {
            rawQuery = rawQuery.replace(AGGREGATION_QUERY_PREFIX, "");
        }
        if (aggregateHits) {
            sbQuery.append(AGGREGATION_QUERY_PREFIX);
            // https://wiki.apache.org/solr/FieldCollapsing
            // https://wiki.apache.org/solr/Join
        }
        sbQuery.append("+(").append(rawQuery).append(")");
        String suffixes = getAllSuffixes(request, true,
                true);

        if (StringUtils.isNotBlank(suffixes)) {
            sbQuery.append(suffixes);
        }
        return sbQuery.toString();
    }

    /**
     * @param request
     * @return Filter query suffix string from the HTTP session
     */
    static String getFilterQuerySuffix(HttpServletRequest request) {
        if (request == null) {
            request = BeanUtils.getRequest();
        }
        if (request == null) {
            return "";
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        String ret = (String) session.getAttribute(PARAM_NAME_FILTER_QUERY_SUFFIX);
        // If not suffix generated yet, initiate update
        if (ret == null) {
            try {
                updateFilterQuerySuffix(request);
                ret = (String) session.getAttribute(PARAM_NAME_FILTER_QUERY_SUFFIX);
            } catch (IndexUnreachableException e) {
                logger.error(e.getMessage(), e);
            } catch (PresentationException e) {
                logger.error(e.getMessage());
            } catch (DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        return ret;
    }

    /**
     * <p>
     * exportSearchAsExcel.
     * </p>
     *
     * @param finalQuery Complete query with suffixes.
     * @param exportQuery Query constructed from the user's input, without any secret suffixes.
     * @param sortFields a {@link java.util.List} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @should create excel workbook correctly
     * @param searchTerms a {@link java.util.Map} object.
     * @param locale a {@link java.util.Locale} object.
     * @param aggregateHits a boolean.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @return a {@link org.apache.poi.xssf.streaming.SXSSFWorkbook} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static SXSSFWorkbook exportSearchAsExcel(String finalQuery, String exportQuery, List<StringPair> sortFields, List<String> filterQueries,
            Map<String, String> params, Map<String, Set<String>> searchTerms, Locale locale, boolean aggregateHits, HttpServletRequest request)
            throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        SXSSFWorkbook wb = new SXSSFWorkbook(25);
        SXSSFSheet currentSheet = wb.createSheet("intranda_viewer_search");

        CellStyle styleBold = wb.createCellStyle();
        Font font2 = wb.createFont();
        font2.setFontHeightInPoints((short) 10);
        font2.setBold(true);
        styleBold.setFont(font2);

        int currentRowIndex = 0;
        int currentCellIndex = 0;

        // Query row
        {
            SXSSFRow row = currentSheet.createRow(currentRowIndex++);
            SXSSFCell cell = row.createCell(currentCellIndex++);
            cell.setCellStyle(styleBold);
            cell.setCellValue(new XSSFRichTextString("Query:"));
            cell = row.createCell(currentCellIndex++);
            cell.setCellValue(new XSSFRichTextString(exportQuery));
            currentCellIndex = 0;
        }

        // Title row
        SXSSFRow row = currentSheet.createRow(currentRowIndex++);
        for (String field : DataManager.getInstance().getConfiguration().getSearchExcelExportFields()) {
            SXSSFCell cell = row.createCell(currentCellIndex++);
            cell.setCellStyle(styleBold);
            cell.setCellValue(new XSSFRichTextString(ViewerResourceBundle.getTranslation(field, locale)));
        }

        List<String> exportFields = DataManager.getInstance().getConfiguration().getSearchExcelExportFields();
        long totalHits = DataManager.getInstance().getSearchIndex().getHitCount(finalQuery, filterQueries);
        int batchSize = 100;
        int totalBatches = (int) Math.ceil((double) totalHits / batchSize);
        for (int i = 0; i < totalBatches; ++i) {
            int first = i * batchSize;
            int max = first + batchSize - 1;
            if (max > totalHits) {
                max = (int) (totalHits - 1);
                batchSize = (int) (totalHits - first);
            }
            logger.trace("Fetching search hits {}-{} out of {}", first, max, totalHits);
            List<SearchHit> batch;
            if (aggregateHits) {
                batch = searchWithAggregation(finalQuery, first, batchSize, sortFields, null, filterQueries, params, searchTerms, exportFields,
                        locale);
            } else {
                batch = searchWithFulltext(finalQuery, first, batchSize, sortFields, null, filterQueries, params, searchTerms, exportFields, locale,
                        request);
            }
            for (SearchHit hit : batch) {
                // Create row
                currentCellIndex = 0;
                row = currentSheet.createRow(currentRowIndex++);
                for (String field : exportFields) {
                    SXSSFCell cell = row.createCell(currentCellIndex++);
                    String value = hit.getExportMetadata().get(field);
                    cell.setCellValue(new XSSFRichTextString(value != null ? value : ""));
                }
            }
        }

        return wb;
    }

    /**
     * <p>
     * getAllFacetFields.
     * </p>
     *
     * @param hierarchicalFacetFields a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public static List<String> getAllFacetFields(List<String> hierarchicalFacetFields) {
        List<String> facetFields = DataManager.getInstance().getConfiguration().getDrillDownFields();
        Optional<String> geoFacetField = Optional.ofNullable(DataManager.getInstance().getConfiguration().getGeoDrillDownField());
        List<String> allFacetFields = new ArrayList<>(hierarchicalFacetFields.size() + facetFields.size() + (geoFacetField.isPresent() ? 1 : 0));
        allFacetFields.addAll(hierarchicalFacetFields);
        allFacetFields.addAll(facetFields);
        geoFacetField.ifPresent(field -> allFacetFields.add(field));
        return SearchHelper.facetifyList(allFacetFields);
    }

    /**
     * <p>
     * parseSortString.
     * </p>
     *
     * @param sortString a {@link java.lang.String} object.
     * @param navigationHelper a {@link io.goobi.viewer.managedbeans.NavigationHelper} object.
     * @should parse string correctly
     * @return a {@link java.util.List} object.
     */
    public static List<StringPair> parseSortString(String sortString, NavigationHelper navigationHelper) {
        List<StringPair> ret = new ArrayList<>();
        if (StringUtils.isNotEmpty(sortString)) {
            String[] sortStringSplit = sortString.split(";");
            if (sortStringSplit.length > 0) {
                for (String field : sortStringSplit) {
                    ret.add(new StringPair(field.replace("!", ""), field.charAt(0) == '!' ? "desc" : "asc"));
                    logger.trace("Added sort field: {}", field);
                    // add translated sort fields
                    if (navigationHelper != null && field.startsWith("SORT_")) {
                        Iterable<Locale> locales = () -> navigationHelper.getSupportedLocales();
                        StreamSupport.stream(locales.spliterator(), false)
                                .sorted(new LocaleComparator(BeanUtils.getLocale()))
                                .map(locale -> field + SolrConstants._LANG_ + locale.getLanguage().toUpperCase())
                                .peek(language -> logger.trace("Adding sort field: {}", language))
                                .forEach(language -> ret.add(new StringPair(language.replace("!", ""), language.charAt(0) == '!' ? "desc" : "asc")));
                    }
                }
            }
        }

        return ret;
    }

    /**
     * @param params
     * @param useExpandQuery
     */
    public static Map<String, String> getExpandQueryParams(String expandQuery) {
        Map<String, String> params = new HashMap<>();
        params.put(ExpandParams.EXPAND, "true");
        params.put(ExpandParams.EXPAND_Q, expandQuery);
        params.put(ExpandParams.EXPAND_FIELD, SolrConstants.PI_TOPSTRUCT);
        params.put(ExpandParams.EXPAND_ROWS, String.valueOf(SolrSearchIndex.MAX_HITS));
        params.put(ExpandParams.EXPAND_SORT, SolrConstants.ORDER + " asc");
        params.put(ExpandParams.EXPAND_FQ, ""); // The main filter query may not apply to the expand query to produce child hits
        return params;
    }

    /**
     * Removes illegal characters from an individual search term. Do not use on whole queries!
     *
     * @param s The term to clean up.
     * @return Cleaned up term.
     * @should remove illegal chars correctly
     * @should preserve truncation
     * @should preserve negation
     */
    public static String cleanUpSearchTerm(String s) {
        if (StringUtils.isNotEmpty(s)) {
            boolean addNegation = false;
            boolean addLeftTruncation = false;
            boolean addRightTruncation = false;
            if (s.charAt(0) == '-') {
                addNegation = true;
                s = s.substring(1);
            } else if (s.charAt(0) == '*') {
                addLeftTruncation = true;
            }
            if (s.endsWith("*")) {
                addRightTruncation = true;
            }
            s = s.replace("*", "");
            // s = s.replace(".", "");
            s = s.replace("(", "");
            s = s.replace(")", "");
            if (addNegation) {
                s = '-' + s;
            } else if (addLeftTruncation) {
                s = '*' + s;
            }
            if (addRightTruncation) {
                s += '*';
            }
        }

        return s;
    }

    /**
     * 
     * @param accessCondition
     * @param escapeAccessCondition
     * @return
     * @should build escaped query correctly
     * @should build not escaped query correctly
     */
    public static String getQueryForAccessCondition(String accessCondition, boolean escapeAccessCondition) {
        if (escapeAccessCondition) {
            accessCondition = BeanUtils.escapeCriticalUrlChracters(accessCondition);
        }
        return AGGREGATION_QUERY_PREFIX + "+(ISWORK:true ISANCHOR:true DOCTYPE:UGC)" + " +" + SolrConstants.ACCESSCONDITION + ":\"" + accessCondition
                + "\"";
    }

}
