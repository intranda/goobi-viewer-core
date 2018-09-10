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
package de.intranda.digiverso.presentation.model.search;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.GroupParams;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.controller.language.LocaleComparator;
import de.intranda.digiverso.presentation.exceptions.AccessDeniedException;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.search.SearchHit.HitType;
import de.intranda.digiverso.presentation.model.security.AccessConditionUtils;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.security.LicenseType;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.viewer.BrowseDcElement;
import de.intranda.digiverso.presentation.model.viewer.BrowseTerm;
import de.intranda.digiverso.presentation.model.viewer.BrowsingMenuFieldConfig;
import de.intranda.digiverso.presentation.model.viewer.StringPair;

/**
 * Search utility class. Static methods only.
 */
public final class SearchHelper {

    private static final Random random = new Random(System.currentTimeMillis());

    private static final Logger logger = LoggerFactory.getLogger(SearchHelper.class);

    // public static final String[] FULLTEXT_SEARCH_FIELDS = { LuceneConstants.FULLTEXT, LuceneConstants.IDDOC_OWNER,
    // LuceneConstants.IDDOC_IMAGEOWNER };

    public static final String PARAM_NAME_FILTER_QUERY_SUFFIX = "filterQuerySuffix";
    public static final String SEARCH_TERM_SPLIT_REGEX = "[ ]|[,]|[-]";
    public static final String PLACEHOLDER_HIGHLIGHTING_START = "##HLS##";
    public static final String PLACEHOLDER_HIGHLIGHTING_END = "##HLE##";
    public static final int SEARCH_TYPE_REGULAR = 0;
    public static final int SEARCH_TYPE_ADVANCED = 1;
    public static final int SEARCH_TYPE_TIMELINE = 2;
    public static final int SEARCH_TYPE_CALENDAR = 3;
    public static final SearchFilter SEARCH_FILTER_ALL = new SearchFilter("filter_ALL", "ALL");

    private static final Object lock = new Object();

    public static Pattern patternNotBrackets = Pattern.compile("NOT\\([^()]*\\)");
    public static Pattern patternPhrase = Pattern.compile("[\\w]+:" + Helper.REGEX_QUOTATION_MARKS);
    public static String collectionSplitRegex = new StringBuilder("[").append(BrowseDcElement.split).append(']').toString();

    static volatile String docstrctWhitelistFilterSuffix = null;
    static volatile String collectionBlacklistFilterSuffix = null;

    /**
     * Main search method for flat search.
     *
     * @param query {@link String} Solr search query. Merges full-text and metadata hits into their corresponding docstructs.
     * @param first {@link Integer} von
     * @param rows {@link Integer} bis
     * @param sortFields
     * @param resultFields
     * @param filterQueries
     * @param params
     * @param searchTerms
     * @param exportFields
     * @param locale
     * @param request
     * @return List of <code>StructElement</code>s containing the search hits.
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     */
    public static List<SearchHit> searchWithFulltext(String query, int first, int rows, List<StringPair> sortFields, List<String> resultFields,
            List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms, List<String> exportFields, Locale locale,
            HttpServletRequest request) throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
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
                    fulltext = Helper.loadFulltext((String) doc.getFirstValue(SolrConstants.DATAREPOSITORY),
                            (String) doc.getFirstValue(SolrConstants.FILENAME_ALTO), (String) doc.getFirstValue(SolrConstants.FILENAME_FULLTEXT),
                            request);
                } catch (AccessDeniedException e) {
                    fulltext = ViewerResourceBundle.getTranslation(e.getMessage(), null);
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
                    SearchHit.createSearchHit(doc, ownerDoc, locale, fulltext, searchTerms, exportFields, true, ignoreFields, translateFields, null);
            ret.add(hit);
            count++;
            logger.trace("added hit {}", count);
        }

        return ret;
    }

    /**
     * Main search method for aggregated search.
     *
     * @param query {@link String} Solr search query. Merges full-text and metadata hits into their corresponding docstructs.
     * @param first {@link Integer} von
     * @param rows {@link Integer} bis
     * @param sortFields
     * @param resultFields
     * @param filterQueries
     * @param params
     * @param searchTerms
     * @param exportFields
     * @param locale
     * @return List of <code>StructElement</code>s containing the search hits.
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @should return all hits
     */
    public static List<SearchHit> searchWithAggregation(String query, int first, int rows, List<StringPair> sortFields, List<String> resultFields,
            List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms, List<String> exportFields, Locale locale)
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
        for (SolrDocument doc : resp.getResults()) {
            // logger.trace("result iddoc: {}", doc.getFieldValue(SolrConstants.IDDOC));
            Map<String, SolrDocumentList> childDocs = resp.getExpandedResults();

            // Create main hit
            // logger.trace("Creating search hit from {}", doc);
            SearchHit hit = SearchHit.createSearchHit(doc, null, locale, null, searchTerms, exportFields, true, ignoreFields, translateFields, null);
            ret.add(hit);
            hit.addOverviewPageChild();
            hit.addFulltextChild(doc, locale != null ? locale.getLanguage() : null);
            logger.trace("Added search hit {}", hit.getBrowseElement().getLabel());
            // Collect Solr docs of child hits 
            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            if (pi != null && childDocs != null && childDocs.containsKey(pi)) {
                logger.trace("{} child hits found for {}", childDocs.get(pi).size(), pi);
                hit.setChildDocs(childDocs.get(pi));
                for (SolrDocument childDoc : childDocs.get(pi)) {
                    // childDoc.remove(SolrConstants.ALTO); // remove ALTO texts to avoid OOM
                    String docType = (String) childDoc.getFieldValue(SolrConstants.DOCTYPE);
                    if (DocType.METADATA.name().equals(docType)) {
                        // Hack: count metadata hits as docstruct for now (because both are labeled "Metadata")
                        docType = DocType.DOCSTRCT.name();
                    }
                    HitType hitType = HitType.getByName(docType);
                    int count = hit.getHitTypeCounts().get(hitType) != null ? hit.getHitTypeCounts().get(hitType) : 0;
                    hit.getHitTypeCounts().put(hitType, count + 1);
                }
            }
        }
        logger.trace("Return {} search hits", ret.size());
        return ret;
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @param addCollectionBlacklistSuffix
     * @param addDiscriminatorValueSuffix
     * @return
     * @throws IndexUnreachableException
     * @should add static suffix
     * @should add collection blacklist suffix
     * @should add discriminator value suffix
     */
    public static String getAllSuffixes(HttpServletRequest request, boolean addCollectionBlacklistSuffix, boolean addDiscriminatorValueSuffix)
            throws IndexUnreachableException {
        StringBuilder sbSuffix = new StringBuilder();
        String staticSuffix = DataManager.getInstance().getConfiguration().getStaticQuerySuffix();
        if (StringUtils.isNotBlank(staticSuffix)) {
            if (staticSuffix.charAt(0) != ' ') {
                sbSuffix.append(' ');
            }
            sbSuffix.append(staticSuffix);
        }
        if (addCollectionBlacklistSuffix) {
            String blacklistMode = DataManager.getInstance().getConfiguration().getCollectionBlacklistMode(SolrConstants.DC);
            if ("all".equals(blacklistMode)) {
                sbSuffix.append(getCollectionBlacklistFilterSuffix(SolrConstants.DC));
            }
        }
        if (addDiscriminatorValueSuffix) {
            sbSuffix.append(getDiscriminatorFieldFilterSuffix(BeanUtils.getNavigationHelper(),
                    DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField()));
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
     * @param addDiscriminatorValueSuffix
     * @return
     * @throws IndexUnreachableException
     */
    public static String getAllSuffixes(boolean addDiscriminatorValueSuffix) throws IndexUnreachableException {
        return getAllSuffixes(null, true, addDiscriminatorValueSuffix);
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @param addDiscriminatorValueSuffix
     * @return
     * @throws IndexUnreachableException
     */
    public static String getAllSuffixesExceptCollectionBlacklist(boolean addDiscriminatorValueSuffix) throws IndexUnreachableException {
        return getAllSuffixes(null, false, addDiscriminatorValueSuffix);
    }

    /**
     * Returns the <code>BrowseElement</code> constructed from the search hit at <code>index</code> from the search hit list for the given
     * <code>query</code>.
     *
     * @param query
     * @param index
     * @param sortFields
     * @param params
     * @param searchTerms
     * @param locale
     * @param aggregateHits
     * @param filterQuerySuffix
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws ViewerConfigurationException
     * @should return correct hit for non-aggregated search
     * @should return correct hit for aggregated search
     */
    public static BrowseElement getBrowseElement(String query, int index, List<StringPair> sortFields, List<String> filterQueries,
            Map<String, String> params, Map<String, Set<String>> searchTerms, Locale locale, boolean aggregateHits, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        String finalQuery = prepareQuery(query, getDocstrctWhitelistFilterSuffix());
        finalQuery = buildFinalQuery(finalQuery, aggregateHits);
        logger.debug("getBrowseElement final query: {}", finalQuery);
        List<SearchHit> hits = aggregateHits
                ? SearchHelper.searchWithAggregation(finalQuery, index, 1, sortFields, null, filterQueries, params, searchTerms, null, locale)
                : SearchHelper.searchWithFulltext(finalQuery, index, 1, sortFields, null, filterQueries, params, searchTerms, null, locale, request);
        if (!hits.isEmpty()) {
            return hits.get(0).getBrowseElement();
        }

        return null;
    }

    /**
     * 
     * @param luceneField
     * @param value
     * @param filterForWorks
     * @param filterForAnchors
     * @param filterForWhitelist
     * @param filterForBlacklist
     * @param separatorString
     * @param locale
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public static String getFirstWorkUrlWithFieldValue(String luceneField, String value, boolean filterForWorks, boolean filterForAnchors,
            boolean filterForWhitelist, boolean filterForBlacklist, String separatorString, Locale locale)
            throws IndexUnreachableException, PresentationException {
        StringBuilder sbQuery = new StringBuilder();
        if (filterForWorks || filterForAnchors) {
            sbQuery.append("(");
        }
        if (filterForWorks) {
            sbQuery.append(SolrConstants.ISWORK).append(":true");
        }
        if (filterForWorks && filterForAnchors) {
            sbQuery.append(" OR ");
        }
        if (filterForAnchors) {
            sbQuery.append(SolrConstants.ISANCHOR).append(":true");
        }
        if (filterForWorks || filterForAnchors) {
            sbQuery.append(")");
        }
        if (filterForWhitelist) {
            sbQuery.append(getDocstrctWhitelistFilterSuffix());
        }
        sbQuery.append(SearchHelper.getAllSuffixesExceptCollectionBlacklist(true));
        sbQuery.append(" AND (").append(luceneField).append(":").append(value).append(" OR ").append(luceneField).append(":").append(
                value + separatorString + "*)");
        Set<String> blacklist = new HashSet<>();
        if (filterForBlacklist) {
            String blacklistMode = DataManager.getInstance().getConfiguration().getCollectionBlacklistMode(luceneField);
            switch (blacklistMode) {
                case "all":
                    blacklist = new HashSet<>();
                    sbQuery.append(getCollectionBlacklistFilterSuffix(luceneField));
                    break;
                case "dcList":
                    blacklist = new HashSet<>(DataManager.getInstance().getConfiguration().getCollectionBlacklist(luceneField));
                    break;
                default:
                    blacklist = new HashSet<>();
                    break;
            }
        }

        logger.debug("query: {}", sbQuery.toString());
        QueryResponse resp = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 0, SolrSearchIndex.MAX_HITS, null, null, null);
        logger.trace("query done");

        if (resp.getResults().size() > 0) {
            try {
                for (SolrDocument doc : resp.getResults()) {
                    Collection<Object> fieldList = doc.getFieldValues(luceneField);
                    if (fieldList != null) {
                        for (Object o : fieldList) {
                            String dc = SolrSearchIndex.getAsString(o);
                            if (!blacklist.isEmpty() && checkCollectionInBlacklist(dc, blacklist)) {
                                continue;
                            }
                            String pi = (String) doc.getFieldValue(SolrConstants.PI);
                            String url = "/ppnresolver?id=" + pi;
                            return url;
                            // StructElement struct = new StructElement(luceneId, doc);
                            // BrowseElement ele = new BrowseElement(struct, false, null, locale);
                            // return ele.getUrl();
                        }
                    }
                }
            } catch (Throwable e) {
                logger.error("Failed to retrieve work", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Returns a Map with hierarchical values from the given field and their respective record counts.
     *
     * @param luceneField
     * @param facetField
     * @param filterForWhitelist
     * @param filterForBlacklist
     * @param filterForWorks
     * @param filterForAnchors
     * @return
     * @throws IndexUnreachableException
     * @should find all collections
     */
    public static Map<String, Long> findAllCollectionsFromField(String luceneField, String facetField, boolean filterForWhitelist,
            boolean filterForBlacklist, boolean filterForWorks, boolean filterForAnchors) throws IndexUnreachableException {
        logger.trace("findAllCollectionsFromField: {}", luceneField);
        Map<String, Long> ret = new HashMap<>();
        try {
            StringBuilder sbQuery = new StringBuilder();
            if (filterForWorks || filterForAnchors) {
                sbQuery.append("(");
            }
            if (filterForWorks) {
                sbQuery.append(SolrConstants.ISWORK).append(":true");
            }
            if (filterForWorks && filterForAnchors) {
                sbQuery.append(" OR ");
            }
            if (filterForAnchors) {
                sbQuery.append(SolrConstants.ISANCHOR).append(":true");
            }
            if (filterForWorks || filterForAnchors) {
                sbQuery.append(")");
            }
            if (filterForWhitelist) {
                sbQuery.append(getDocstrctWhitelistFilterSuffix());
            }
            sbQuery.append(SearchHelper.getAllSuffixesExceptCollectionBlacklist(true));
            Set<String> blacklist = new HashSet<>();
            if (filterForBlacklist) {
                String blacklistMode = DataManager.getInstance().getConfiguration().getCollectionBlacklistMode(luceneField);
                switch (blacklistMode) {
                    case "all":
                        blacklist = new HashSet<>();
                        sbQuery.append(getCollectionBlacklistFilterSuffix(luceneField));
                        break;
                    case "dcList":
                        blacklist = new HashSet<>(DataManager.getInstance().getConfiguration().getCollectionBlacklist(luceneField));
                        break;
                    default:
                        blacklist = new HashSet<>();
                        break;
                }
            }

            // Fill the map from the facet (faster, but unfortunately, precise parent collection size cannot be determined
            // this way)
            {
                //              logger.debug("query: {}", sbQuery.toString());
                // QueryResponse resp = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 0, 0, null,
                // Collections.singletonList(
                //                    facetField), null, null, null);
                //              logger.trace("query done");
                //                if (resp.getFacetField(facetField) != null && resp.getFacetField(facetField).getValues() != null) {
                //                    for (Count count : resp.getFacetField(facetField).getValues()) {
                // if (count.getName() == null || (!blacklist.isEmpty() && checkCollectionInBlacklist(count.getName(),
                // blacklist))) {
                //                            continue;
                //                        }
                //                        Long recordCount = ret.get(count.getName());
                //                        if (recordCount == null) {
                //                            recordCount = 0L;
                //                        }
                //                        ret.put(count.getName(), recordCount + count.getCount());
                //
                //                        // Add count to parent collections
                //                        if (count.getName().contains(BrowseDcElement.split)) {
                //                            String parent = count.getName();
                //                            while (parent.lastIndexOf(BrowseDcElement.split) != -1) {
                //                                parent = parent.substring(0, parent.lastIndexOf(BrowseDcElement.split));
                //                                Long parentRecordCount = ret.get(parent);
                //                                if (parentRecordCount == null) {
                //                                    parentRecordCount = 0L;
                //                                }
                //                                ret.put(parent, parentRecordCount + count.getCount());
                //                            }
                //                        }
                //                    }
                //                }
            }

            // Iterate over record hits instead of using facets to determine the size of the parent collections
            {
                logger.debug("query: {}", sbQuery.toString());
                // No faceting needed when fetching field names manually (faceting adds to the total execution time)
                SolrDocumentList results =
                        DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), Collections.singletonList(luceneField));
                logger.trace("query done");
                for (SolrDocument doc : results) {
                    Set<String> dcDoneForThisRecord = new HashSet<>();
                    Collection<Object> fieldList = doc.getFieldValues(luceneField);
                    if (fieldList != null) {
                        for (Object o : fieldList) {
                            String dc = SolrSearchIndex.getAsString(o);
                            //                            String dc = (String) o;
                            if (!blacklist.isEmpty() && checkCollectionInBlacklist(dc, blacklist)) {
                                continue;
                            }
                            {
                                Long count = ret.get(dc);
                                if (count == null) {
                                    count = 0L;
                                }
                                count++;
                                ret.put(dc, count);
                                dcDoneForThisRecord.add(dc);
                            }

                            if (dc.contains(BrowseDcElement.split)) {
                                String parent = dc;
                                while (parent.lastIndexOf(BrowseDcElement.split) != -1) {
                                    parent = parent.substring(0, parent.lastIndexOf(BrowseDcElement.split));
                                    if (!dcDoneForThisRecord.contains(parent)) {
                                        Long count = ret.get(parent);
                                        if (count == null) {
                                            count = 0L;
                                        }
                                        count++;
                                        ret.put(parent, count);
                                        dcDoneForThisRecord.add(parent);
                                    }
                                }
                            }
                        }
                    }
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
     * @param dc
     * @param blacklist
     * @return
     * @should match simple collections correctly
     * @should match subcollections correctly
     * @should throw IllegalArgumentException if dc is null
     * @should throw IllegalArgumentException if blacklist is null
     */
    protected static boolean checkCollectionInBlacklist(String dc, Set<String> blacklist) {
        if (dc == null) {
            throw new IllegalArgumentException("dc may not be null");
        }
        if (blacklist == null) {
            throw new IllegalArgumentException("blacklist may not be null");
        }

        String dcSplit[] = dc.split(collectionSplitRegex);
        // boolean blacklisted = false;
        StringBuilder sbDc = new StringBuilder();
        for (String element : dcSplit) {
            if (sbDc.length() > 0) {
                sbDc.append(BrowseDcElement.split);
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
     *
     * @param query
     * @param facetFields
     * @param facetMinCount
     * @param getFieldStatistics
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static QueryResponse searchCalendar(String query, List<String> facetFields, int facetMinCount, boolean getFieldStatistics)
            throws PresentationException, IndexUnreachableException {
        logger.trace("searchCalendar: {}", query);
        StringBuilder sbQuery = new StringBuilder(query).append(getAllSuffixes(true));
        return DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(sbQuery.toString(), facetFields, facetMinCount,
                getFieldStatistics);
    }

    public static int[] getMinMaxYears(String subQuery) throws PresentationException, IndexUnreachableException {
        int[] ret = { -1, -1 };

        StringBuilder sbSearchString = new StringBuilder();
        sbSearchString.append(SolrConstants._CALENDAR_YEAR).append(":*");
        if (StringUtils.isNotEmpty(subQuery)) {
            sbSearchString.append(subQuery);
        }
        // logger.debug("searchString: {}", searchString);
        QueryResponse resp = searchCalendar(sbSearchString.toString(), Collections.singletonList(SolrConstants._CALENDAR_YEAR), 0, true);

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
     * @param currentFacets
     * @throws IndexUnreachableException
     * @should return autosuggestions correctly
     * @should filter by collection correctly
     * @should filter by facet correctly
     */
    public static List<String> searchAutosuggestion(String suggest, List<FacetItem> currentFacets) throws IndexUnreachableException {
        List<String> ret = new ArrayList<>();

        if (!suggest.contains(" ")) {
            try {
                suggest = suggest.toLowerCase();
                StringBuilder sbQuery = new StringBuilder();
                sbQuery.append(SolrConstants.DEFAULT).append(':').append(ClientUtils.escapeQueryChars(suggest)).append('*');
                if (currentFacets != null && !currentFacets.isEmpty()) {
                    for (FacetItem facetItem : currentFacets) {
                        if (sbQuery.length() > 0) {
                            sbQuery.append(" AND ");
                        }
                        sbQuery.append(facetItem.getQueryEscapedLink());
                        logger.trace("Added  facet: {}", facetItem.getQueryEscapedLink());
                    }
                }
                sbQuery.append(getAllSuffixes(true));
                logger.debug("Autocomplete query: {}", sbQuery.toString());
                SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 100, null,
                        Collections.singletonList(SolrConstants.DEFAULT));
                for (SolrDocument doc : hits) {
                    String defaultValue = (String) doc.getFieldValue(SolrConstants.DEFAULT);
                    if (StringUtils.isNotEmpty(defaultValue)) {
                        String[] bla = defaultValue.split(" ");
                        for (String s : bla) {
                            String st = s.trim();
                            st = st.toLowerCase();
                            if (!" ".equals(st) && st.startsWith(suggest)) {
                                while (!StringUtils.isAlphanumeric(st.substring(st.length() - 1))) {
                                    st = st.substring(0, st.length() - 1);
                                }
                                if (!ret.contains(st)) {
                                    ret.add(st);
                                }
                            }
                        }
                    }
                }
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return ret;
    }

    /**
     * Builds a Solr query suffix that filters the results by the docstrct whitelist. It should be sufficient to build this string once per
     * application lifetime, since updating the whitelist would also require a tomcat restart.
     *
     * @return
     * @should construct suffix correctly
     */
    public static String getDocstrctWhitelistFilterSuffix() {
        String suffix = docstrctWhitelistFilterSuffix;
        if (suffix == null) {
            synchronized (lock) {
                suffix = docstrctWhitelistFilterSuffix;
                if (suffix == null) {
                    suffix = generateDocstrctWhitelistFilterSuffix(DataManager.getInstance().getConfiguration().getDocStructWhiteList());
                    docstrctWhitelistFilterSuffix = suffix;
                }
            }
        }

        return suffix;
    }

    /**
     * Returns a Solr query suffix that filters out collections defined in the collection blacklist. This suffix is only generated once per
     * application lifecycle.
     *
     * @param fiel
     * @return
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
     *
     * @param docstructList
     * @return Solr query suffix for docstruct filtering
     * @should construct suffix correctly
     * @should return empty string if only docstruct is asterisk
     */
    protected static String generateDocstrctWhitelistFilterSuffix(List<String> docstructList) {
        if (docstructList == null) {
            throw new IllegalArgumentException("docstructList may not be null");
        }
        logger.debug("Generating docstruct whitelist suffix...");
        if (!docstructList.isEmpty()) {
            if (docstructList.size() == 1 && "*".equals(docstructList.get(0))) {
                return "";
            }
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append(" AND (");
            for (String s : docstructList) {
                if (StringUtils.isNotBlank(s)) {
                    String escapedS = s.trim();
                    sbQuery.append(SolrConstants.DOCSTRCT).append(':').append(escapedS).append(" OR ");
                }
            }
            sbQuery.delete(sbQuery.length() - 4, sbQuery.length());
            sbQuery.append(')');
            return sbQuery.toString();
        }

        return "";
    }

    /**
     *
     * @param field
     * @return
     * @should construct suffix correctly
     */
    protected static String generateCollectionBlacklistFilterSuffix(String field) {
        logger.debug("Generating blacklist suffix for field '{}'...", field);
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
     *
     * @param nagivationHelper
     * @param discriminatorField
     * @return
     * @throws IndexUnreachableException
     * @should construct subquery correctly
     * @should return empty string if discriminator value is empty or hyphen
     */
    public static String getDiscriminatorFieldFilterSuffix(NavigationHelper nh, String discriminatorField) throws IndexUnreachableException {
        // logger.trace("nh null? {}", nh == null);
        logger.trace("discriminatorField: {}", discriminatorField);
        if (StringUtils.isNotEmpty(discriminatorField) && nh != null) {
            String discriminatorValue = nh.getSubThemeDiscriminatorValue();
            logger.trace("discriminatorValue: {}", discriminatorValue);
            if (StringUtils.isNotEmpty(discriminatorValue) && !"-".equals(discriminatorValue)) {
                StringBuilder sbSuffix = new StringBuilder();
                sbSuffix.append(" AND ").append(discriminatorField).append(':').append(discriminatorValue);
                logger.trace("Discriminator field suffix: {}", sbSuffix.toString());
                return sbSuffix.toString();
            }
        }

        return "";
    }

    /**
     * Updates the calling agent's session with a personalized filter sub-query.
     *
     * @param request
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public static void updateFilterQuerySuffix(HttpServletRequest request) throws IndexUnreachableException, PresentationException, DAOException {
        String filterQuerySuffix = getPersonalFilterQuerySuffix((User) request.getSession().getAttribute("user"), Helper.getIpAddress(request));
        logger.trace("New filter query suffix: {}", filterQuerySuffix);
        request.getSession().setAttribute(PARAM_NAME_FILTER_QUERY_SUFFIX, filterQuerySuffix);
    }

    /**
     * Constructs a personal search query filter suffix for the given user and IP address.
     *
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @should construct suffix correctly
     * @should construct suffix correctly if user has license privilege
     * @should construct suffix correctly if ip range has license privilege
     */
    public static String getPersonalFilterQuerySuffix(User user, String ipAddress)
            throws IndexUnreachableException, PresentationException, DAOException {
        StringBuilder query = new StringBuilder();

        for (LicenseType licenseType : DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes()) {
            // Consider only license types that do not allow listing by default and are not static licenses
            if (!licenseType.isStaticLicenseType() && !licenseType.getPrivileges().contains(IPrivilegeHolder.PRIV_LIST)) {
                if (AccessConditionUtils.checkAccessPermission(Collections.singletonList(licenseType),
                        new HashSet<>(Collections.singletonList(licenseType.getName())), IPrivilegeHolder.PRIV_LIST, user, ipAddress, null)) {
                    // If the use has an explicit priv to list a certain license type, ignore all other license types
                    logger.trace("User has listing privilege for license type '{}'.", licenseType.getName());
                    query = new StringBuilder();
                    continue;
                }
                if (licenseType.getConditions() != null) {
                    String processedConditions = licenseType.getProcessedConditions();
                    // Do not append empty subquery
                    if (StringUtils.isNotBlank(processedConditions)) {
                        query.append(" -(").append(SolrConstants.ACCESSCONDITION).append(":\"").append(licenseType.getName()).append('"');
                        query.append(" AND ").append(processedConditions).append(')');
                    } else {
                        query.append(" -").append(SolrConstants.ACCESSCONDITION).append(":\"").append(licenseType.getName()).append('"');
                    }
                } else {
                    query.append(" -").append(SolrConstants.ACCESSCONDITION).append(":\"").append(licenseType.getName()).append('"');
                }
            }
        }

        return query.toString();
    }

    /**
     * TODO This method might be quite expensive.
     *
     * @param searchTerms
     * @param fulltext
     * @param targetFragmentLength Desired (approximate) length of the text fragment.
     * @param firstMatchOnly If true, only the fragment for the first match will be returned
     * @param addFragmentIfNoMatches If true, a fragment will be added even if no term was matched
     * @return
     * @should not add prefix and suffix to text
     * @should truncate string to 200 chars if no terms are given
     * @should truncate string to 200 chars if no term has been found
     * @should make terms bold if found in text
     * @should remove unclosed HTML tags
     * @should return multiple match fragments correctly
     * @should replace line breaks with spaces
     * @should add fragment if no term was matched only if so requested
     * @should highlight multi word terms while removing stopwords
     * 
     */
    public static List<String> truncateFulltext(Set<String> searchTerms, String fulltext, int targetFragmentLength, boolean firstMatchOnly,
            boolean addFragmentIfNoMatches) {
        logger.trace("truncateFulltext");
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
                logger.trace("term: {}", searchTerm);
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
     * @param phrase
     * @param terms
     * @return
     * @should apply highlighting for all terms
     * @should skip single character terms
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
            String normalizedPhrase = normalizeString(phrase);
            String normalizedTerm = normalizeString(term);
            if (StringUtils.contains(normalizedPhrase, normalizedTerm)) {
                highlightedValue = SearchHelper.applyHighlightingToPhrase(highlightedValue, term);
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
        String after = phrase.substring(endIndex);

        return sb.append(applyHighlightingToPhrase(before, term)).append(highlightedTerm).append(applyHighlightingToPhrase(after, term)).toString();
    }

    /**
     * 
     * @param string
     * @return
     */
    static String normalizeString(String string) {
        if (string == null) {
            return null;
        }

        return string.toLowerCase().replaceAll("[^a-zA-Z0-9#]", " ");
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
     * 
     * @param phrase
     * @return
     * @should replace placeholders with bold tags
     */
    public static String replaceHighlightingPlaceholdersForHyperlinks(String phrase) {
        return phrase.replace(PLACEHOLDER_HIGHLIGHTING_START, "<span style=\"color:blue\">").replace(PLACEHOLDER_HIGHLIGHTING_END, "</span>");
    }

    /**
     * 
     * @param phrase
     * @return
     * @should replace placeholders with html tags
     */
    public static String replaceHighlightingPlaceholders(String phrase) {
        return phrase.replace(PLACEHOLDER_HIGHLIGHTING_START, "<span class=\"search-list--highlight\">").replace(PLACEHOLDER_HIGHLIGHTING_END,
                "</span>");
    }

    /**
     * 
     * @param phrase
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
     * @param query
     * @param facetFieldName
     * @param facetMinCount
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static List<String> getFacetValues(String query, String facetFieldName, int facetMinCount)
            throws PresentationException, IndexUnreachableException {
        return getFacetValues(query, facetifyField(facetFieldName), null, facetMinCount);
    }

    /**
     * Returns a list of values for a given facet field and the given query.
     *
     * @param query
     * @param facetFieldName
     * @param facetMinCount
     * @param facetPrefix The facet field value must start with these characters. Ignored if null or blank
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static List<String> getFacetValues(String query, String facetFieldName, String facetPrefix, int facetMinCount)
            throws PresentationException, IndexUnreachableException {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query may not be null or empty");
        }
        if (StringUtils.isEmpty(facetFieldName)) {
            throw new IllegalArgumentException("facetFieldName may not be null or empty");
        }

        QueryResponse resp = DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(query, Collections.singletonList(facetFieldName),
                facetMinCount, facetPrefix, false);
        FacetField facetField = resp.getFacetField(facetFieldName);
        List<String> ret = new ArrayList<>(facetField.getValueCount());
        for (Count count : facetField.getValues()) {
            if (StringUtils.isNotEmpty(count.getName()) && count.getCount() >= facetMinCount) {
                ret.add(count.getName());
            }
        }

        return ret;
    }

    /**
     * Returns a list of index terms for the given field name. This method uses the slower doc search instead of term search, but can be filtered with
     * a query.
     *
     * @param bmfc
     * @param startsWith
     * @param filterQuery
     * @param comparator
     * @param aggregateHits
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static List<BrowseTerm> getFilteredTerms(BrowsingMenuFieldConfig bmfc, String startsWith, String filterQuery,
            Comparator<BrowseTerm> comparator, boolean aggregateHits) throws PresentationException, IndexUnreachableException {
        List<BrowseTerm> ret = new ArrayList<>();
        Map<BrowseTerm, Boolean> terms = new ConcurrentHashMap<>();
        Map<String, BrowseTerm> usedTerms = new ConcurrentHashMap<>();

        StringBuilder sbQuery = new StringBuilder();
        // Only search via the sorting field if not doing a wildcard search
        if (StringUtils.isNotEmpty(bmfc.getSortField())) {
            sbQuery.append(bmfc.getSortField());
        } else {
            sbQuery.append(bmfc.getField());
        }
        sbQuery.append(":[* TO *]");

        List<String> filterQueries = new ArrayList<>();
        if (StringUtils.isNotEmpty(filterQuery)) {
            filterQueries.add(filterQuery);
        }
        if (!bmfc.getDocstructFilters().isEmpty()) {
            //            sbQuery.append(" AND (");
            StringBuilder sbDocstructFilter = new StringBuilder();
            for (String docstruct : bmfc.getDocstructFilters()) {
                sbDocstructFilter.append(SolrConstants.DOCSTRCT).append(':').append(docstruct).append(" OR ");
//                sbDocstructFilter.append(docstruct).append(" OR ");
            }
            //            sbQuery.delete(sbQuery.length() - 4, sbQuery.length());
            //            sbQuery.append(')');
            if(sbDocstructFilter.length() > 4) {                
                sbDocstructFilter.delete(sbDocstructFilter.length() - 4, sbDocstructFilter.length());
            }
            filterQueries.add(sbDocstructFilter.toString());
        }
        if (bmfc.isRecordsAndAnchorsOnly()) {
            filterQueries.add(
                    new StringBuilder().append(SolrConstants.ISWORK).append(":true OR ").append(SolrConstants.ISANCHOR).append(":true").toString());

        }

        String query = buildFinalQuery(sbQuery.toString(), false);
        if (logger.isDebugEnabled()) {
            logger.debug("getFilteredTerms query: {}", query);
            for (String fq : filterQueries) {
                logger.trace("getFilteredTerms filter query: {}", fq);
            }
        }

        int rows = SolrSearchIndex.MAX_HITS;
        List<String> facetFields = new ArrayList<>();
        facetFields.add(bmfc.getField());

        try {
            Map<String, String> params = new HashMap<>();
            if (DataManager.getInstance().getConfiguration().isGroupDuplicateHits()) {
                params.put(GroupParams.GROUP, "true");
                params.put(GroupParams.GROUP_MAIN, "true");
                params.put(GroupParams.GROUP_FIELD, SolrConstants.GROUPFIELD);
            }
            List<StringPair> sortFields =
                    StringUtils.isEmpty(bmfc.getSortField()) ? null : Collections.singletonList(new StringPair(bmfc.getSortField(), "asc"));
            QueryResponse resp =
                    DataManager.getInstance().getSearchIndex().search(query, 0, rows, sortFields, facetFields, null, filterQueries, params);
            // QueryResponse resp = DataManager.getInstance().getSolrHelper().searchFacetsAndStatistics(sbQuery.toString(),
            // facetFields, false);
            logger.debug("getFilteredTerms hits: {}", resp.getResults().getNumFound());
            if ("0-9".equals(startsWith)) {
                // Numerical filtering
                Pattern p = Pattern.compile("[\\d]");
                // Use hits (if sorting field is provided)
                for (SolrDocument doc : resp.getResults()) {
                    Collection<Object> termList = doc.getFieldValues(bmfc.getField());
                    String sortTerm = (String) doc.getFieldValue(bmfc.getSortField());
                    Set<String> usedTermsInCurrentDoc = new HashSet<>();
                    for (Object o : termList) {
                        String term = String.valueOf(o);
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
                            if (!usedTerms.containsKey(term)) {
                                BrowseTerm browseTerm = new BrowseTerm(term, sortTerm);
                                terms.put(browseTerm, true);
                                usedTerms.put(term, browseTerm);
                                usedTermsInCurrentDoc.add(term);
                                sortTerm = null; // only use the sort term for the first term
                            } else if (!usedTermsInCurrentDoc.contains(term)) {
                                // Only add to hit count if the same string is not in the same doc
                                usedTerms.get(term).addToHitCount(1);
                                usedTermsInCurrentDoc.add(term);
                            }
                        }
                    }
                }
            } else {
                // Without filtering or using alphabetical filtering
                // Parallel processing of hits (if sorting field is provided), requires compiler level 1.8
                ((List<SolrDocument>) resp.getResults()).parallelStream()
                        .forEach(doc -> processSolrResult(doc, bmfc.getField(), bmfc.getSortField(), startsWith, terms, usedTerms, aggregateHits));

                // Sequential processing (doesn't break the sorting done by Solr)
                //                for (SolrDocument doc : resp.getResults()) {
                //                    processSolrResult(doc, bmfc.getField(), bmfc.getSortField(), startsWith, terms, usedTerms, aggregateHits);
                //                }
            }
        } catch (PresentationException e) {
            logger.debug("PresentationException thrown here: {}", e.getMessage());
            throw new PresentationException(e.getMessage());
        } catch (RemoteSolrException e) {
            logger.error("{} (this usually means Solr is returning 403); Query: {}", e.getMessage(), sbQuery.toString());
            throw new PresentationException("Search index unavailable.");
        }

        if (!terms.isEmpty()) {
            ret = new ArrayList<>(terms.keySet());
            if (comparator != null) {
                Collections.sort(ret, comparator);
            }
        }

        logger.debug("getFilteredTerms end: {} terms found.", ret.size());
        return ret;
    }

    /**
     * Extracts terms from the given Solr document and adds them to the terms map, if applicable. Can be executed in parallel, provided
     * <code>terms</code> and <code>usedTerms</code> are synchronized.
     *
     * @param doc
     * @param field
     * @param sortField
     * @param startsWith
     * @param terms Set of terms collected so far.
     * @param usedTerms Terms that are already in the terms map.
     * @param aggregateHits
     */
    private static void processSolrResult(SolrDocument doc, String field, String sortField, String startsWith, Map<BrowseTerm, Boolean> terms,
            Map<String, BrowseTerm> usedTerms, boolean aggregateHits) {
        // logger.trace("processSolrResult thread {}", Thread.currentThread().getId());
        Collection<Object> termList = doc.getFieldValues(field);
        if (termList == null) {
            return;
        }
        String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        String sortTerm = (String) doc.getFieldValue(sortField);
        Set<String> usedTermsInCurrentDoc = new HashSet<>();
        for (Object o : termList) {
            String term = String.valueOf(o);
            if (StringUtils.isEmpty(term)) {
                continue;
            }
            String compareTerm = term;
            if (StringUtils.isNotEmpty(sortTerm)) {
                compareTerm = sortTerm;
            }
            //            if (logger.isTraceEnabled() && StringUtils.startsWithIgnoreCase(compareTerm, startsWith)) {
            //                logger.trace("compareTerm '{}' starts with '{}'", compareTerm, startsWith);
            //            }
            if (StringUtils.isEmpty(startsWith) || "-".equals(startsWith) || StringUtils.startsWithIgnoreCase(compareTerm, startsWith)) {
                if (!usedTerms.containsKey(term)) {
                    BrowseTerm browseTerm = new BrowseTerm(term, sortTerm);
                    // logger.trace("Adding term: {}, compareTerm: {}, sortTerm: {}", term, compareTerm, sortTerm);
                    terms.put(browseTerm, true);
                    usedTerms.put(term, browseTerm);
                    usedTermsInCurrentDoc.add(term);
                    browseTerm.getPiList().add(pi);
                } else if (!usedTermsInCurrentDoc.contains(term)) {
                    // Only add to hit count if the same string is not in the same doc
                    BrowseTerm browseTerm = usedTerms.get(term);
                    // If using aggregated search, do not count instances of records that already have been counted
                    if (aggregateHits && browseTerm.getPiList().contains(pi)) {
                        continue;
                    }
                    browseTerm.addToHitCount(1);
                    usedTermsInCurrentDoc.add(term);
                    browseTerm.getPiList().add(pi);
                }
            }
            sortTerm = null; // only use the sort term for the first term
        }
    }

    /**
     * Parses the given Solr query for field values and returns them as a set of strings.
     *
     * @param query
     * @param discriminatorValue
     * @return
     * @should extract all values from query except from NOT blocks
     * @should handle multiple phrases in query correctly
     * @should skip discriminator value
     * @should remove truncation
     * @should throw IllegalArgumentException if query is null
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
            // Use a copy of the query because the original query gets shortened after every match, causing an IOOBE
            // eventually
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
                    // Remove left truncation
                    if (value.charAt(0) == '*' && value.length() > 1) {
                        value = value.substring(1);
                    }
                    // Remove right truncation
                    if (value.charAt(value.length() - 1) == '*' && value.length() > 1) {
                        value = value.substring(0, value.length() - 1);
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

    /**
     * 
     * @return
     */
    public static Map<String, String> generateQueryParams() {
        Map<String, String> params = new HashMap<>();
        if (DataManager.getInstance().getConfiguration().isGroupDuplicateHits() && !DataManager.getInstance().getConfiguration().isAggregateHits()) {
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
     * 
     * @param sourceList
     * @return
     * @should facetify correctly
     */
    public static List<String> facetifyList(List<String> sourceList) {
        if (sourceList != null) {
            List<String> ret = new ArrayList<>(sourceList.size());
            for (String s : sourceList) {
                String fieldName = facetifyField(s);
                if (fieldName != null) {
                    ret.add(fieldName);
                }
            }
            return ret;
        }
        return null;
    }

    /**
     * 
     * @param fieldName
     * @return
     * @should facetify correctly
     */
    public static String facetifyField(String fieldName) {
        if (fieldName != null) {
            switch (fieldName) {
                case SolrConstants.DC:
                    return SolrConstants.FACET_DC;

                case SolrConstants.DOCSTRCT:
                    return "FACET_DOCSTRCT";
                default:
                    if (fieldName.startsWith("MD_")) {
                        fieldName = fieldName.replace("MD_", "FACET_");
                    }
                    fieldName = fieldName.replace(SolrConstants._UNTOKENIZED, "");
                    return fieldName;
            }
        }
        return null;
    }

    /**
     * 
     * @param fieldName
     * @return
     * @should defacetify correctly
     */
    public static String defacetifyField(String fieldName) {
        if (fieldName != null) {
            switch (fieldName) {
                case SolrConstants.FACET_DC:
                    return SolrConstants.DC;
                case "FACET_DOCSTRCT":
                    return SolrConstants.DOCSTRCT;
                default:
                    if (fieldName.startsWith("FACET_")) {
                        return fieldName.replace("FACET_", "MD_");
                    }
                    return fieldName;
            }
        }
        return null;
    }

    /**
     * Creates a Solr expand query string out of lists of fields and terms.
     * 
     * @param fields
     * @param searchTerms
     * @param phraseSearch If true, quotation marks are added to terms
     * @return
     * @should generate query correctly
     * @should return empty string if no fields match
     * @should skip reserved fields
     * @should escape reserved characters
     * @should not escape asterisks
     * @should add quotation marks if phraseSearch is true
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
                    case SolrConstants.DC:
                    case SolrConstants.DOCSTRCT:
                        continue;
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
     * @param groups
     * @param advancedSearchGroupOperator
     * @return
     * @should generate query correctly
     * @should skip reserved fields
     */
    public static String generateAdvancedExpandQuery(List<SearchQueryGroup> groups, int advancedSearchGroupOperator) {
        logger.trace("generateAdvancedExpandQuery");
        StringBuilder sbOuter = new StringBuilder();

        if (groups != null && !groups.isEmpty()) {
            for (SearchQueryGroup group : groups) {
                StringBuilder sbGroup = new StringBuilder();

                // Identify any fields that only exist in page docs and enable the page search mode
                boolean searchInFulltext = false;
                for (SearchQueryItem item : group.getQueryItems()) {
                    if (item.getField() == null) {
                        continue;
                    }
                    switch (item.getField()) {
                        case SolrConstants.FULLTEXT:
                        case SearchQueryItem.ADVANCED_SEARCH_ALL_FIELDS:
                            searchInFulltext = true;
                            break;
                    }
                }

                for (SearchQueryItem item : group.getQueryItems()) {
                    if (item.getField() == null) {
                        continue;
                    }
                    // Skip fields that exist in all child docs (e.g. PI_TOPSTRUCT) so that searches within a record don't
                    // return every single doc
                    switch (item.getField()) {
                        case SolrConstants.PI_TOPSTRUCT:
                        case SolrConstants.DC:
                        case SolrConstants.DOCSTRCT:
                            continue;
                    }
                    String itemQuery = item.generateQuery(new HashSet<String>(), false);
                    if (StringUtils.isNotEmpty(itemQuery)) {
                        if (sbGroup.length() > 0) {
                            if (searchInFulltext) {
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
        }
        if (sbOuter.length() > 0) {
            return " +(" + sbOuter.toString() + ')';
            //            return sbOuter.toString();
        }

        return "";
    }

    /**
     * 
     * @param searchType
     * @param searchFilter
     * @param queryGroups
     * @return
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
                                if (!ret.contains(SolrConstants.OVERVIEWPAGE_DESCRIPTION)) {
                                    ret.add(SolrConstants.OVERVIEWPAGE_DESCRIPTION);
                                }
                                if (!ret.contains(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT)) {
                                    ret.add(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT);
                                }
                            } else if (SolrConstants.DEFAULT.equals(item.getField())
                                    || SolrConstants.SUPERDEFAULT.equals(item.getField()) && !ret.contains(SolrConstants.DEFAULT)) {
                                ret.add(SolrConstants.DEFAULT);
                            } else if (SolrConstants.FULLTEXT.equals(item.getField())
                                    || SolrConstants.SUPERFULLTEXT.equals(item.getField()) && !ret.contains(SolrConstants.FULLTEXT)) {
                                ret.add(SolrConstants.FULLTEXT);
                            } else if (SolrConstants.OVERVIEWPAGE.equals(item.getField())) {
                                if (!ret.contains(SolrConstants.OVERVIEWPAGE_DESCRIPTION)) {
                                    ret.add(SolrConstants.OVERVIEWPAGE_DESCRIPTION);
                                }
                                if (!ret.contains(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT)) {
                                    ret.add(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT);
                                }
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
                    ret.add(SolrConstants.OVERVIEWPAGE_DESCRIPTION);
                    ret.add(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT);
                    ret.add(SolrConstants._CALENDAR_DAY);
                } else {
                    ret.add(searchFilter.getField());
                }
                break;
        }

        return ret;
    }

    /**
     * Puts non-empty queries into parentheses and replaces empty queries with a top level record-only query (for collection listing).
     * 
     * @param query
     * @query
     * @return
     * @should prepare non-empty queries correctly
     * @should prepare empty queries correctly
     */
    public static String prepareQuery(String query, String docstructWhitelistFilterSuffix) {
        StringBuilder sbQuery = new StringBuilder();
        if (StringUtils.isNotEmpty(query)) {
            sbQuery.append('(').append(query).append(')');
        } else {
            // Collection browsing (no search query)
            sbQuery.append('(').append(SolrConstants.ISWORK).append(":true OR ").append(SolrConstants.ISANCHOR).append(":true)");
            if (docstructWhitelistFilterSuffix != null) {
                sbQuery.append(docstructWhitelistFilterSuffix);
            }

        }

        return sbQuery.toString();
    }

    /**
     * Constructs the complete query using the raw query and adding all available suffixes.
     * 
     * @param rawQuery
     * @param aggregateHits
     * @return
     * @throws IndexUnreachableException
     * @should add join statement if aggregateHits true
     */
    public static String buildFinalQuery(String rawQuery, boolean aggregateHits) throws IndexUnreachableException {
        StringBuilder sbQuery = new StringBuilder();
        if (aggregateHits) {
            sbQuery.append("{!join from=PI_TOPSTRUCT to=PI}");
            // https://wiki.apache.org/solr/FieldCollapsing
            // https://wiki.apache.org/solr/Join
        }
        sbQuery.append('(').append(rawQuery).append(')').append(getAllSuffixes(true));
        return sbQuery.toString();
    }

    /**
     * @param request
     * @return
     */
    static String getFilterQuerySuffix(HttpServletRequest request) {
        if (request == null) {
            request = BeanUtils.getRequest();
        }
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        return (String) session.getAttribute(PARAM_NAME_FILTER_QUERY_SUFFIX);
    }

    /**
     * 
     * @param query Complete query with suffixes.
     * @param exportQuery Query constructed from the user's input, without any secret suffixes.
     * @param sortFields
     * @param resultFields
     * @param filterQueries
     * @param params
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @throws ViewerConfigurationException
     * @should create excel workbook correctly
     */
    public static SXSSFWorkbook exportSearchAsExcel(String query, String exportQuery, List<StringPair> sortFields, List<String> filterQueries,
            Map<String, String> params, Map<String, Set<String>> searchTerms, Locale locale, boolean aggregateHits, HttpServletRequest request)
            throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        SXSSFWorkbook wb = new SXSSFWorkbook(25);
        List<SXSSFSheet> sheets = new ArrayList<>();
        int currentSheetIndex = 0;
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
            cell.setCellValue(new XSSFRichTextString(Helper.getTranslation(field, locale)));
        }

        List<String> exportFields = DataManager.getInstance().getConfiguration().getSearchExcelExportFields();
        long totalHits = DataManager.getInstance().getSearchIndex().getHitCount(query);
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
                batch = searchWithAggregation(query, first, batchSize, sortFields, null, filterQueries, params, searchTerms, exportFields, locale);
            } else {
                batch = searchWithFulltext(query, first, batchSize, sortFields, null, filterQueries, params, searchTerms, exportFields, locale,
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
     * @param hierarchicalFacetFields
     * @return
     */
    public static List<String> getAllFacetFields(List<String> hierarchicalFacetFields) {
        List<String> facetFields = DataManager.getInstance().getConfiguration().getDrillDownFields();
        List<String> allFacetFields = new ArrayList<>(hierarchicalFacetFields.size() + facetFields.size());
        allFacetFields.addAll(hierarchicalFacetFields);
        allFacetFields.addAll(facetFields);
        allFacetFields = SearchHelper.facetifyList(allFacetFields);
        return allFacetFields;
    }

    /**
     * 
     * @param sortString
     * @param navigationHelper
     * @return
     * @should parse string correctly
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
}
