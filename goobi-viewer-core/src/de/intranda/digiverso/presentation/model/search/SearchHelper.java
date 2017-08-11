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
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.search.SearchHit.HitType;
import de.intranda.digiverso.presentation.model.user.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.user.IpRange;
import de.intranda.digiverso.presentation.model.user.LicenseType;
import de.intranda.digiverso.presentation.model.user.User;
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
     * @param params
     * @param searchTerms
     * @param exportFields
     * @param locale
     * @return List of <code>StructElement</code>s containing the search hits.
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static List<SearchHit> searchWithFulltext(String query, int first, int rows, List<StringPair> sortFields, List<String> resultFields,
            Map<String, String> params, Map<String, Set<String>> searchTerms, List<String> exportFields, Locale locale) throws PresentationException,
            IndexUnreachableException, DAOException {
        Map<String, SolrDocument> ownerDocs = new HashMap<>();
        QueryResponse resp = DataManager.getInstance().getSearchIndex().search(query, first, rows, sortFields, null, resultFields, params);
        if (resp.getResults() == null) {
            return Collections.emptyList();
        }
        if (params != null) {
            logger.trace("params: {}", params.toString());
        }
        logger.trace("hits found: {}; results returned: {}", resp.getResults().getNumFound(), resp.getResults().size());
        List<SearchHit> ret = new ArrayList<>(resp.getResults().size());
        for (SolrDocument doc : resp.getResults()) {
            // logger.trace("result iddoc: {}", doc.getFieldValue(LuceneConstants.IDDOC));
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

                fulltext = (String) doc.getFirstValue("MD_FULLTEXT");
                if (fulltext == null) {
                    fulltext = (String) doc.getFieldValue(SolrConstants.FULLTEXT);
                }
            } else {
                // Add docstruct documents to the owner doc map, just in case
                ownerDocs.put((String) doc.getFieldValue(SolrConstants.IDDOC), doc);
            }

            SearchHit hit = SearchHit.createSearchHit(doc, ownerDoc, locale, fulltext, searchTerms, exportFields, true);
            ret.add(hit);
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
     * @param params
     * @param searchTerms
     * @param exportFields
     * @param locale
     * @return List of <code>StructElement</code>s containing the search hits.
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should return all hits
     */
    public static List<SearchHit> searchWithAggregation(String query, int first, int rows, List<StringPair> sortFields, List<String> resultFields,
            Map<String, String> params, Map<String, Set<String>> searchTerms, List<String> exportFields, Locale locale) throws PresentationException,
            IndexUnreachableException, DAOException {
        QueryResponse resp = DataManager.getInstance().getSearchIndex().search(query, first, rows, sortFields, null, resultFields, params);
        if (resp.getResults() == null) {
            return new ArrayList<>();
        }
        logger.trace("hits found: {}; results returned: {}", resp.getResults().getNumFound(), resp.getResults().size());
        List<SearchHit> ret = new ArrayList<>(resp.getResults().size());
        for (SolrDocument doc : resp.getResults()) {
            // logger.trace("result iddoc: {}", doc.getFieldValue(LuceneConstants.IDDOC));
            Map<String, SolrDocumentList> childDocs = resp.getExpandedResults();

            // Create main hit
            SearchHit hit = SearchHit.createSearchHit(doc, null, locale, null, searchTerms, exportFields, true);
            ret.add(hit);
            hit.addOverviewPageChild();

            // Collect Solr docs of child hits 
            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            if (pi != null && childDocs != null && childDocs.containsKey(pi)) {
                logger.trace("{} child hits found for {}", childDocs.get(pi).size(), pi);
                hit.setChildDocs(childDocs.get(pi));
                for (SolrDocument childDoc : childDocs.get(pi)) {
                    HitType hitType = HitType.getByName((String) childDoc.getFieldValue(SolrConstants.DOCTYPE));
                    int count = hit.getHitTypeCounts().get(hitType) != null ? hit.getHitTypeCounts().get(hitType) : 0;
                    hit.getHitTypeCounts().put(hitType, count + 1);
                }
            }
        }

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
            sbSuffix.append(getDiscriminatorFieldFilterSuffix(BeanUtils.getNavigationHelper(), DataManager.getInstance().getConfiguration()
                    .getSubthemeDiscriminatorField()));
        }
        String filterQuerySuffix = getFilterQuerySuffix(request);
        logger.debug("filterQuerySuffix: " + filterQuerySuffix);
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
     * @should return correct hit for non-aggregated search
     * @should return correct hit for aggregated search
     */
    public static BrowseElement getBrowseElement(String query, int index, List<StringPair> sortFields, Map<String, String> params,
            Map<String, Set<String>> searchTerms, Locale locale, boolean aggregateHits) throws PresentationException, IndexUnreachableException,
            DAOException {
        // logger.debug("getBrowseElement(): " + query);
        String finalQuery = buildFinalQuery(query, aggregateHits);
        List<SearchHit> hits = aggregateHits ? SearchHelper.searchWithAggregation(finalQuery, index, 1, sortFields, null, params, searchTerms, null,
                locale) : SearchHelper.searchWithFulltext(finalQuery, index, 1, sortFields, null, params, searchTerms, null, locale);
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
            boolean filterForWhitelist, boolean filterForBlacklist, String separatorString, Locale locale) throws IndexUnreachableException,
            PresentationException {
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
        sbQuery.append(" AND (").append(luceneField).append(":").append(value).append(" OR ").append(luceneField).append(":").append(value
                + separatorString + "*)");
        List<String> blacklist = new ArrayList<>();
        if (filterForBlacklist) {
            String blacklistMode = DataManager.getInstance().getConfiguration().getCollectionBlacklistMode(luceneField);
            switch (blacklistMode) {
                case "all":
                    blacklist = new ArrayList<>();
                    sbQuery.append(getCollectionBlacklistFilterSuffix(luceneField));
                    break;
                case "dcList":
                    blacklist = DataManager.getInstance().getConfiguration().getCollectionBlacklist(luceneField);
                    break;
                default:
                    blacklist = new ArrayList<>();
                    break;
            }
        }

        logger.debug("query: {}", sbQuery.toString());
        QueryResponse resp = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 0, SolrSearchIndex.MAX_HITS, null, null, null,
                null);
        logger.trace("query done");

        if (resp.getResults().size() > 0) {
            try {
                for (SolrDocument doc : resp.getResults()) {
                    Collection<Object> fieldList = doc.getFieldValues(luceneField);
                    if (fieldList != null) {
                        for (Object o : fieldList) {
                            String dc = (String) o;
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
            List<String> blacklist = new ArrayList<>();
            if (filterForBlacklist) {
                String blacklistMode = DataManager.getInstance().getConfiguration().getCollectionBlacklistMode(luceneField);
                switch (blacklistMode) {
                    case "all":
                        blacklist = new ArrayList<>();
                        sbQuery.append(getCollectionBlacklistFilterSuffix(luceneField));
                        break;
                    case "dcList":
                        blacklist = DataManager.getInstance().getConfiguration().getCollectionBlacklist(luceneField);
                        break;
                    default:
                        blacklist = new ArrayList<>();
                        break;
                }
            }

            logger.debug("query: {}", sbQuery.toString());
            // String query = "(ISWORK:true OR ISANCHOR:true) AND (DOCSTRCT:Monograph OR DOCSTRCT:monograph OR
            // DOCSTRCT:MultiVolumeWork OR DOCSTRCT:Periodical OR DOCSTRCT:VolumeRun OR DOCSTRCT:Deed OR DOCSTRCT:Picture OR
            // DOCSTRCT:Sequence OR DOCSTRCT:Seal OR DOCSTRCT:Map OR DOCSTRCT:SingleRecord OR DOCSTRCT:library_object OR
            // DOCSTRCT:museum_object OR DOCSTRCT:Record OR DOCSTRCT:SingleLetter OR DOCSTRCT:Video OR DOCSTRCT:Audio OR
            // DOCSTRCT:MusicSupplies OR DOCSTRCT:manuscript OR DOCSTRCT:Incunable OR DOCSTRCT:Drawing OR DOCSTRCT:Painting
            // OR DOCSTRCT:Newspaper OR DOCSTRCT:Botanik OR DOCSTRCT:Illustration OR DOCSTRCT:pathologisches_Präparat OR
            // DOCSTRCT:PathologicalSpecimen OR DOCSTRCT:Plastisches_Objekt OR DOCSTRCT:Abzug OR DOCSTRCT:Ansichtskarte OR
            // DOCSTRCT:Deckfarbe OR DOCSTRCT:Faltprospekt OR DOCSTRCT:Formstein OR DOCSTRCT:Kreidezeichnung OR
            // DOCSTRCT:Münze OR DOCSTRCT:Coin OR DOCSTRCT:Painting OR DOCSTRCT:Plastik OR DOCSTRCT:Portalpfosten OR
            // DOCSTRCT:Puppe OR DOCSTRCT:Relief OR DOCSTRCT:Reliefbild OR DOCSTRCT:Tisch OR
            // DOCSTRCT:Wendeltreppe_\\(Modell\\) OR DOCSTRCT:Zeichnung) AND NOT ((ACCESSCONDITION:\"test\")) AND NOT
            // (DC:mmmsammlungen.500unigreifswald.030anatomischesammlungen* OR
            // DC:mmmsammlungen.500unigreifswald.050botanischesammlungen* OR
            // DC:mmmsammlungen.500unigreifswald.080dalmansammlung OR
            // DC:mmmsammlungen.500unigreifswald.200vorgeschichtlichesammlung OR
            // DC:mmmsammlungen.500unigreifswald.250zoologischesammlung OR
            // DC:institutfrpathologiederernstmoritzarndtuniversittgreifswald OR
            // DC:mmmsammlungen500unigreifswald030anatomischesammlungen200vergleichendanatomischesammlung)";
            QueryResponse resp = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 0, SolrSearchIndex.MAX_HITS, null, Collections
                    .singletonList(facetField), Collections.singletonList(luceneField), null);
            logger.trace("query done");

            // Construct splitting regex
            // String splitRegex = BrowseDcElement.split;
            // if (BrowseDcElement.split.equals(".")) {
            // splitRegex = "[.]";
            // }

            // Fill the map from the facet (faster, but unfortunately, precise parent collection size cannot be determined
            // this way)
            // if (resp.getFacetField(LuceneConstants.DC) != null && resp.getFacetField(LuceneConstants.DC).getValues() !=
            // null) {
            // for (Count count : resp.getFacetField(LuceneConstants.DC).getValues()) {
            // Long recordCount = ret.get(count.getName());
            // if (recordCount == null) {
            // recordCount = 0L;
            // }
            // ret.put(count.getName(), recordCount + count.getCount());
            //
            // // Add count to parent collections
            // String[] nameSplit = count.getName().split(splitRegex);
            // if (nameSplit.length > 1) {
            // for (int i = 0; i < nameSplit.length; ++i) {
            // StringBuilder sbParentName = new StringBuilder();
            // for (int j = 0; j <= i; ++j) {
            // if (sbParentName.length() > 0) {
            // sbParentName.append(BrowseDcElement.split);
            // }
            // sbParentName.append(nameSplit[j]);
            // }
            // String parentName = sbParentName.toString();
            // if (parentName.length() < count.getName().length()) {
            // Long parentRecordCount = ret.get(parentName);
            // if (parentRecordCount == null) {
            // parentRecordCount = 0L;
            // }
            // ret.put(parentName, parentRecordCount + count.getCount());
            // if(nameSplit[0].equals("belletristik"))
            // logger.debug(count.getName() + " parent '" + parentName + "' count is now " + ret.get(parentName));
            // }
            // }
            // }
            // }
            // }

            // Iterate over record hits instead of using facets to determine the size of the parent collections
            for (SolrDocument doc : resp.getResults()) {
                Set<String> dcDoneForThisRecord = new HashSet<>();
                Collection<Object> fieldList = doc.getFieldValues(luceneField);
                if (fieldList != null) {
                    for (Object o : fieldList) {
                        String dc = (String) o;
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
    protected static boolean checkCollectionInBlacklist(String dc, List<String> blacklist) {
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
     * @param currentCollections Optional collections to which all suggestions have to belong.
     * @param currentFacets
     * @throws IndexUnreachableException
     * @should return autosuggestions correctly
     * @should filter by collection correctly
     * @should filter by facet correctly
     */
    public static List<String> searchAutosuggestion(String suggest, List<FacetItem> currentCollections, List<FacetItem> currentFacets)
            throws IndexUnreachableException {
        List<String> ret = new ArrayList<>();

        if (!suggest.contains(" ")) {
            try {
                suggest = suggest.toLowerCase();
                StringBuilder sbQuery = new StringBuilder();
                sbQuery.append(SolrConstants.DEFAULT).append(':').append(ClientUtils.escapeQueryChars(suggest)).append('*');
                if (currentCollections != null && !currentCollections.isEmpty()) {
                    for (FacetItem facetItem : currentCollections) {
                        if (sbQuery.length() > 0) {
                            sbQuery.append(" AND ");
                        }
                        sbQuery.append(facetItem.getQueryEscapedLink());
                        logger.trace("Added collection facet: {}", facetItem.getQueryEscapedLink());
                    }
                }
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
                SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 100, null, Collections.singletonList(
                        SolrConstants.DEFAULT));
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
                    suffix = generateDocstrctWhitelistFilterSuffix();
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
     * @return
     * @should construct suffix correctly
     */
    protected static String generateDocstrctWhitelistFilterSuffix() {
        logger.debug("Generating docstruct whitelist suffix...");
        StringBuilder sbQuery = new StringBuilder();
        List<String> list = DataManager.getInstance().getConfiguration().getDocStructWhiteList();
        if (!list.isEmpty()) {
            sbQuery.append(" AND (");
            for (String s : list) {
                if (StringUtils.isNotBlank(s)) {
                    sbQuery.append(SolrConstants.DOCSTRCT).append(':').append(ClientUtils.escapeQueryChars(s.trim())).append(" OR ");
                }
            }
            sbQuery.delete(sbQuery.length() - 4, sbQuery.length());
            sbQuery.append(')');
        }
        return sbQuery.toString();
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
    public static String getPersonalFilterQuerySuffix(User user, String ipAddress) throws IndexUnreachableException, PresentationException,
            DAOException {
        StringBuilder query = new StringBuilder();

        for (LicenseType licenseType : DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes()) {
            // Consider only license types that do not allow listing by default and are not static licenses
            if (!licenseType.isStaticLicenseType() && !licenseType.getPrivileges().contains(IPrivilegeHolder.PRIV_LIST)) {
                if (checkAccessPermission(Collections.singletonList(licenseType), new HashSet<>(Collections.singletonList(licenseType.getName())),
                        IPrivilegeHolder.PRIV_LIST, user, ipAddress, null)) {
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
     * 
     * @param fileName
     * @param identifier
     * @return
     * @should use correct field name for AV files
     * @should use correct file name for text files
     */
    static String[] generateAccessCheckQuery(String identifier, String fileName) {
        String[] ret = new String[2];

        StringBuilder sbQuery = new StringBuilder();
        String useFileField = SolrConstants.FILENAME;
        String useFileName = fileName;
        // Different media types have the file name in different fields
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        switch (extension) {
            case "webm":
                useFileField = SolrConstants.FILENAME_WEBM;
                break;
            case "mp4":
                useFileField = SolrConstants.FILENAME_MP4;
                break;
            case "mp3":
                // if the mime type in METS is not audio/mpeg3 but something else, access will be false
                useFileField = SolrConstants.FILENAME_MPEG3;
                break;
            case "ogg":
            case "ogv":
                useFileField = SolrConstants.FILENAME_OGG;
                break;
            case "txt":
            case "xml":
                useFileName = fileName.replace(extension, "*");
                break;
            default:
                break;
        }
        sbQuery.append(SolrConstants.PI_TOPSTRUCT).append(':').append(identifier).append(" AND ").append(useFileField).append(':');
        if (useFileName.endsWith(".*")) {
            sbQuery.append(useFileName);
        } else {
            sbQuery.append("\"").append(useFileName).append("\"");
        }

        // logger.trace(sbQuery.toString());
        ret[0] = sbQuery.toString();
        ret[1] = useFileField;

        return ret;
    }

    /**
     * Checks whether the client may access an image (by PI + file name).
     *
     * @param identifier Work identifier (PI).
     * @param fileName Image file name. For all files of a record, use "*".
     * @param request Calling HttpServiceRequest.
     * @return true if access is granted; false otherwise.
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    protected static Map<String, Boolean> checkAccessPermissionByIdentifierAndFileName(String identifier, String fileName, String privilegeName,
            HttpServletRequest request) throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndFileName({}, {}, {}, {})", identifier, fileName, privilegeName,
        // request.getAttributeNames().toString());
        if (StringUtils.isNotEmpty(identifier)) {
            String[] query = generateAccessCheckQuery(identifier, fileName);
            try {
                // Collect access conditions required by the page
                Map<String, Set<String>> requiredAccessConditions = new HashMap<>();
                SolrDocumentList results = DataManager.getInstance().getSearchIndex().search(query[0], "*".equals(fileName) ? SolrSearchIndex.MAX_HITS
                        : 1, null, Arrays.asList(new String[] { query[1], SolrConstants.ACCESSCONDITION }));
                if (results != null) {
                    for (SolrDocument doc : results) {
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            Set<String> pageAccessConditions = new HashSet<>();
                            for (Object accessCondition : fieldsAccessConddition) {
                                pageAccessConditions.add(accessCondition.toString());
                                // logger.debug(accessCondition.toString());
                            }
                            requiredAccessConditions.put(fileName, pageAccessConditions);
                        }
                    }
                }

                User user = BeanUtils.getUserFromRequest(request);
                Map<String, Boolean> ret = new HashMap<>(requiredAccessConditions.size());
                for (String pageFileName : requiredAccessConditions.keySet()) {
                    Set<String> pageAccessConditions = requiredAccessConditions.get(pageFileName);
                    boolean access = checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), pageAccessConditions,
                            privilegeName, user, Helper.getIpAddress(request), query[0]);
                    ret.put(pageFileName, access);
                }
                return ret;
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return new HashMap<>(0);

    }

    /**
     * Checks whether the current users has the given access permissions to the element with the given identifier and LOGID.
     *
     * @param identifier The PI to check.
     * @param logId The LOGID to check (optional).
     * @param privilegeName Particular privilege for which to check the permission.
     * @param request
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionByIdentifierAndLogId(String identifier, String logId, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        // logger.trace("checkAccessPermissionByIdentifierAndLogId({}, {}, {}, {})", identifier, logId, privilegeName,
        // request.getAttributeNames());
        if (StringUtils.isNotEmpty(identifier)) {
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append(SolrConstants.PI_TOPSTRUCT).append(':').append(identifier);
            if (StringUtils.isNotEmpty(logId)) {
                sbQuery.append(" AND ").append(SolrConstants.LOGID).append(':').append(logId);
            }
            // Only query docstruct docs because metadata/event docs may not contain values defined in the license type
            // filter query
            sbQuery.append(" AND ").append(SolrConstants.DOCTYPE).append(':').append(DocType.DOCSTRCT.name());
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                SolrDocumentList results = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1, null, Arrays.asList(new String[] {
                        SolrConstants.ACCESSCONDITION }));
                if (results != null) {
                    for (SolrDocument doc : results) {
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            for (Object accessCondition : fieldsAccessConddition) {
                                requiredAccessConditions.add((String) accessCondition);
                                // logger.debug(accessCondition.toString());
                            }
                        }
                    }
                }

                User user = BeanUtils.getUserFromRequest(request);
                return checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), requiredAccessConditions,
                        privilegeName, user, Helper.getIpAddress(request), sbQuery.toString());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return false;
    }

    public static boolean checkContentFileAccessPermission(String identifier, HttpServletRequest request) throws IndexUnreachableException,
            DAOException {
        // logger.trace("checkContentFileAccessPermission({})", identifier);
        if (StringUtils.isNotEmpty(identifier)) {
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append(SolrConstants.PI).append(':').append(identifier);
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                SolrDocumentList results = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1, null, Arrays.asList(new String[] {
                        SolrConstants.ACCESSCONDITION }));
                if (results != null) {
                    for (SolrDocument doc : results) {
                        Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                        if (fieldsAccessConddition != null) {
                            for (Object accessCondition : fieldsAccessConddition) {
                                requiredAccessConditions.add((String) accessCondition);
                                // logger.debug(accessCondition.toString());
                            }
                        }
                    }
                }

                User user = BeanUtils.getUserFromRequest(request);
                return checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), requiredAccessConditions,
                        IPrivilegeHolder.PRIV_DOWNLOAD_ORIGINAL_CONTENT, user, Helper.getIpAddress(request), sbQuery.toString());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     * Checks whether the client may access an image (by image URN).
     *
     * @param imageUrn Image URN.
     * @param request Calling HttpServiceRequest.
     * @return true if access is granted; false otherwise.
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionByImageUrn(String imageUrn, String privilegeName, HttpServletRequest request)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByImageUrn({}, {}, {}, {})", imageUrn, privilegeName, request.getAttributeNames());
        if (StringUtils.isNotEmpty(imageUrn)) {
            StringBuilder sbQuery = new StringBuilder();
            sbQuery.append(SolrConstants.IMAGEURN).append(':').append(imageUrn.replace(":", "\\:"));
            try {
                Set<String> requiredAccessConditions = new HashSet<>();
                SolrDocumentList hits = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 1, null, Arrays.asList(new String[] {
                        SolrConstants.ACCESSCONDITION, SolrConstants.PI_TOPSTRUCT }));
                for (SolrDocument doc : hits) {
                    Collection<Object> fieldsAccessConddition = doc.getFieldValues(SolrConstants.ACCESSCONDITION);
                    if (fieldsAccessConddition != null) {
                        for (Object accessCondition : fieldsAccessConddition) {
                            requiredAccessConditions.add((String) accessCondition);
                            // logger.debug((String) accessCondition);
                        }
                    }
                }

                User user = BeanUtils.getUserFromRequest(request);
                return checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), requiredAccessConditions,
                        privilegeName, user, Helper.getIpAddress(request), sbQuery.toString());
            } catch (PresentationException e) {
                logger.debug("PresentationException thrown here: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     *
     * @param requiredAccessConditions
     * @param privilegeName
     * @param pi
     * @param request
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public static boolean checkAccessPermission(Set<String> requiredAccessConditions, String privilegeName, String query, HttpServletRequest request)
            throws IndexUnreachableException, PresentationException, DAOException {
        User user = BeanUtils.getUserFromRequest(request);
        return checkAccessPermission(DataManager.getInstance().getDao().getNonOpenAccessLicenseTypes(), requiredAccessConditions, privilegeName, user,
                Helper.getIpAddress(request), query);
    }

    /**
     * Checks access permission for the given image and puts the permission status into the corresponding session map.
     *
     * @param request
     * @param pi
     * @param contentFileName
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionForImage(HttpServletRequest request, String pi, String contentFileName)
            throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionForImage: {}/{}", pi, contentFileName);
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_IMAGES);
    }

    /**
     * Checks access permission for the given thumbnail and puts the permission status into the corresponding session map.
     *
     * @param request
     * @param pi
     * @param contentFileName
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public static boolean checkAccessPermissionForThumbnail(HttpServletRequest request, String pi, String contentFileName)
            throws IndexUnreachableException, DAOException {
        return checkAccessPermissionByIdentifierAndFileNameWithSessionMap(request, pi, contentFileName, IPrivilegeHolder.PRIV_VIEW_THUMBNAILS);
    }

    /**
     * Checks access permission of the given privilege type for the given image and puts the permission status into the corresponding session map.
     *
     * @param request
     * @param pi
     * @param contentFileName
     * @param privilegeType
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    public static boolean checkAccessPermissionByIdentifierAndFileNameWithSessionMap(HttpServletRequest request, String pi, String contentFileName,
            String privilegeType) throws IndexUnreachableException, DAOException {
        logger.trace("checkAccessPermissionByIdentifierAndFileNameWithSessionMap");
        if (privilegeType == null) {
            throw new IllegalArgumentException("privilegeType may not be null");
        }
        boolean access = false;
        // logger.debug("session id: " + request.getSession().getId());
        // Session persistent permission check: Servlet-local method.
        String attributeName = IPrivilegeHolder._PRIV_PREFIX + privilegeType;
        logger.trace("Checking session attribute: {}", attributeName);
        Map<String, Boolean> permissions = (Map<String, Boolean>) request.getSession().getAttribute(attributeName);
        if (permissions == null) {
            permissions = new HashMap<>();
            logger.trace("Session attribute not found, creating new");
        }
        // logger.debug("Permissions found, " + permissions.size() + " items.");
        // new pi -> create an new empty map in the session
        if (!pi.equals(request.getSession().getAttribute("currentPi"))) {
            // logger.trace("new PI: {}", pi);
            request.getSession().setAttribute("currentPi", pi);
            request.getSession().removeAttribute(attributeName);
            permissions = new HashMap<>();
            logger.trace("PI has changed, permissions map reset.");
        }

        String key = new StringBuilder(pi).append('_').append(contentFileName).toString();
        // pi already checked -> look in the session
        // logger.debug("permissions key: " + key + ": " + permissions.get(key));
        if (permissions.containsKey(key)) {
            access = permissions.get(key);
            logger.trace("Access ({}) previously checked and is {} for '{}/{}' (Session ID {})", privilegeType, access, pi, contentFileName, request
                    .getSession().getId());
        } else {
            // TODO check for all images and save to map
            Map<String, Boolean> accessMap = SearchHelper.checkAccessPermissionByIdentifierAndFileName(pi, contentFileName, privilegeType, request);
            for (String pageFileName : accessMap.keySet()) {
                String newKey = new StringBuilder(pi).append('_').append(pageFileName).toString();
                boolean pageAccess = accessMap.get(pageFileName);
                permissions.put(newKey, pageAccess);
            }
            access = permissions.get(key) != null ? permissions.get(key) : false;
            // logger.debug("Access ({}) not yet checked for '{}/{}', access is {}", privilegeType, pi, contentFileName,
            // access);
            request.getSession().setAttribute(attributeName, permissions);
        }

        return access;
    }

    /**
     *
     * @param allLicenseTypes
     * @param requiredAccessConditions Set of access condition names to satisfy (one suffices).
     * @param privilegeName The particular privilege to check.
     * @param user Logged in user.
     * @param query Solr query describing the resource in question.
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @should return true if required access conditions empty
     * @should return true if required access conditions contain only open access
     * @should return true if all license types allow privilege by default
     * @should return false if not all license types allow privilege by default
     * @should return true if ip range allows access
     * @should not return true if no ip range matches
     * 
     *         TODO user license checks
     */
    static boolean checkAccessPermission(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions, String privilegeName, User user,
            String remoteAddress, String query) throws IndexUnreachableException, PresentationException, DAOException {
        // logger.trace("checkAccessPermission({},{},{})", allLicenseTypes, requiredAccessConditions, privilegeName);
        // If OPENACCESS is the only condition, allow immediately
        if (requiredAccessConditions.isEmpty()) {
            logger.debug("No required access conditions given, access granted.");
            return true;
        }
        if (requiredAccessConditions.size() == 1 && (requiredAccessConditions.contains(SolrConstants.OPEN_ACCESS_VALUE) || requiredAccessConditions
                .contains(SolrConstants.OPEN_ACCESS_VALUE.toLowerCase()))) {
            return true;
        }
        // If no license types are configured or no privilege name is given, allow immediately
        if (allLicenseTypes == null || !StringUtils.isNotEmpty(privilegeName)) {
            logger.trace("No license types or no privilege name given.");
            return true;
        }

        List<LicenseType> relevantLicenseTypes = getRelevantLicenseTypesOnly(allLicenseTypes, requiredAccessConditions, query);
        requiredAccessConditions = new HashSet<>(relevantLicenseTypes.size());
        if (relevantLicenseTypes.isEmpty()) {
            logger.trace("No relevant license types.");
            return true;
        }

        // If all relevant license types allow the requested privilege by default, allow access
        {
            boolean licenseTypeAllowsPriv = true;
            // Check whether *all* relevant license types allow the requested privilege by default
            for (LicenseType licenseType : relevantLicenseTypes) {
                requiredAccessConditions.add(licenseType.getName());
                if (!licenseType.getPrivileges().contains(privilegeName)) {
                    // logger.debug("LicenseType '" + licenseType.getName() + "' does not allow the action '" + privilegeName
                    // + "' by default.");
                    licenseTypeAllowsPriv = false;
                }
            }
            if (licenseTypeAllowsPriv) {
                logger.trace("Privilege '{}' is allowed by default in all license types.", privilegeName);
                return true;
            }
        }

        // Check IP range
        if (StringUtils.isNotEmpty(remoteAddress)) {
            // Check whether the requested privilege is allowed to this IP range (for all access conditions)
            Map<String, Boolean> permissionMap = new HashMap<>(requiredAccessConditions.size());
            for (IpRange ipRange : DataManager.getInstance().getDao().getAllIpRanges()) {
                // logger.debug("ip range: " + ipRange.getSubnetMask());
                if (ipRange.matchIp(remoteAddress) && ipRange.canSatisfyAllAccessConditions(requiredAccessConditions, privilegeName, null)) {
                    logger.debug("Access granted to {} via IP range {}", remoteAddress, ipRange.getName());
                    return true;
                }
            }
        }

        // If not within an allowed IP range, check the current user's satisfied access conditions
        if (user != null && user.canSatisfyAllAccessConditions(requiredAccessConditions, privilegeName, null)) {
            return true;
        }

        // logger.trace("not allowed");
        return false;
    }

    /**
     * Filters the given list of license types my removing those that have Solr query conditions that do not match the given identifier.
     *
     * @param allLicenseTypes
     * @param requiredAccessConditions
     * @param query
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @should remove license types whose names do not match access conditions
     * @should remove license types whose condition query excludes the given pi
     */
    static List<LicenseType> getRelevantLicenseTypesOnly(List<LicenseType> allLicenseTypes, Set<String> requiredAccessConditions, String query)
            throws IndexUnreachableException, PresentationException {
        if (requiredAccessConditions == null || requiredAccessConditions.isEmpty()) {
            return Collections.emptyList();
        }

        logger.trace("getRelevantLicenseTypesOnly: {} | {}", query, requiredAccessConditions);
        List<LicenseType> ret = new ArrayList<>(allLicenseTypes.size());
        for (LicenseType licenseType : allLicenseTypes) {
            // logger.trace(licenseType.getName());
            if (!requiredAccessConditions.contains(licenseType.getName())) {
                continue;
            }
            // Check whether the license type contains conditions that exclude the given record, in that case disregard this
            // license type
            if (StringUtils.isNotEmpty(licenseType.getConditions()) && StringUtils.isNotEmpty(query)) {
                String conditions = licenseType.getProcessedConditions();
                // logger.trace("License conditions: {}", conditions);
                StringBuilder sbQuery = new StringBuilder(query);
                if (conditions.charAt(0) == '-') {
                    // do not wrap the conditions in parentheses if it starts with a negation, otherwise it won't work
                    sbQuery.append(" AND ").append(conditions);
                } else {
                    sbQuery.append(" AND (").append(conditions).append(')');
                }
                // logger.trace("License relevance query: {}", sbQuery.toString());
                if (DataManager.getInstance().getSearchIndex().getHitCount(sbQuery.toString()) == 0) {
                    // logger.trace("LicenseType '{}' does not apply to resource described by '{}' due to configured the
                    // license subquery.", licenseType
                    // .getName(), query);
                    continue;
                }
                logger.trace("LicenseType '{}' applies to resource described by '{}' due to configured license subquery.", licenseType.getName(),
                        query);
            }
            ret.add(licenseType);
        }

        return ret;
    }

    /**
     * TODO This method might be quite expensive.
     *
     * @param searchTerms
     * @param fulltext
     * @param targetFragmentLength Desired (approximate) length of the text fragment.
     * @return
     * @should not add prefix and suffix to text
     * @should truncate string to 200 chars if no terms are given
     * @should truncate string to 200 chars if no term has been found
     * @should make terms bold if found in text
     * @should remove unclosed HTML tags
     */
    public static String truncateFulltext(Set<String> searchTerms, String fulltext, int targetFragmentLength) {
        if (fulltext == null) {
            throw new IllegalArgumentException("fulltext may not be null");
        }
        StringBuilder sbFulltextFragment = new StringBuilder();

        String fulltextFragment = "";

        if (searchTerms != null && !searchTerms.isEmpty()) {
            for (String searchTerm : searchTerms) {
                if (searchTerm.length() == 0) {
                    continue;
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
                int indexOfTerm = fulltext.toLowerCase().indexOf(searchTerm.toLowerCase());
                if (indexOfTerm >= 0) {

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
                }
            }
            // If no search term has been found (i.e. when searching for a phrase), make sure no empty string gets delivered
            if (StringUtils.isEmpty(fulltextFragment)) {
                if (fulltext.length() > 200) {
                    fulltextFragment = fulltext.substring(0, 200);
                } else {
                    fulltextFragment = fulltext;
                }
            }
        } else {
            if (fulltext.length() > 200) {
                fulltextFragment = fulltext.substring(0, 200);
            } else {
                fulltextFragment = fulltext;
            }
        }

        if (StringUtils.isNotBlank(fulltextFragment)) {
            // Check for unclosed HTML tags
            int lastIndexOfLT = fulltextFragment.lastIndexOf('<');
            int lastIndexOfGT = fulltextFragment.lastIndexOf('>');
            if (lastIndexOfLT != -1 && lastIndexOfLT > lastIndexOfGT) {
                fulltextFragment = fulltextFragment.substring(0, lastIndexOfLT).trim();
            }
            // Add prefix + suffix
            // sbFulltextFragment.append("[...] ").append(fulltextFragment).append(" [...]");
            sbFulltextFragment.append(fulltextFragment);
        }

        return sbFulltextFragment.toString();
    }

    /**
     * Adds highlighting markup for all given terms to the phrase.
     * 
     * @param phrase
     * @param terms
     * @return
     * @should apply highlighting for all terms
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
            if (StringUtils.containsIgnoreCase(phrase, term)) {
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
     */
    static String applyHighlightingToPhrase(String phrase, String term) {
        if (phrase == null) {
            throw new IllegalArgumentException("phrase may not be null");
        }
        if (term == null) {
            throw new IllegalArgumentException("term may not be null");
        }

        StringBuilder sb = new StringBuilder();
        String normalizedPhrase = phrase.toLowerCase().replaceAll("[^a-zA-Z0-9#]", " ");
        String normalizedTerm = term.toLowerCase().replaceAll("[^a-zA-Z0-9#]", " ");
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
     * @should replace placeholders with html tags
     */
    public static String replaceHighlightingPlaceholders(String phrase) {
        return phrase.replace(PLACEHOLDER_HIGHLIGHTING_START, "<span class=\"search-list--highlight\">").replace(PLACEHOLDER_HIGHLIGHTING_END,
                "</span>");
    }

    /**
     * @param fulltext
     * @param targetFragmentLength
     * @param fulltextFragment
     * @param searchTerm
     * @param indexOfTerm
     * @return
     */
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
    private static String getTextFragmentFromLine(String fulltext, String searchTerm, int indexOfTerm, int fragmentLength) {
        String fulltextFragment;
        String stringBefore = fulltext.substring(0, indexOfTerm);
        String stringAfter = fulltext.substring(indexOfTerm + searchTerm.length());
        int halfLength = fragmentLength / 2;

        int lineStartIndex = Math.max(0, Math.max(indexOfTerm - halfLength, stringBefore.lastIndexOf(System.lineSeparator())));
        int lineEndIndex = Math.min(fulltext.length(), Math.min(indexOfTerm + halfLength, indexOfTerm + searchTerm.length() + stringAfter.indexOf(
                System.lineSeparator())));

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
    public static List<String> getFacetValues(String query, String facetFieldName, int facetMinCount) throws PresentationException,
            IndexUnreachableException {
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query may not be null or empty");
        }
        if (StringUtils.isEmpty(facetFieldName)) {
            throw new IllegalArgumentException("facetFieldName may not be null or empty");
        }

        QueryResponse resp = DataManager.getInstance().getSearchIndex().searchFacetsAndStatistics(query, Collections.singletonList(facetFieldName),
                facetMinCount, false);
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
     * @param comparator
     * @param aggregateHits
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static List<BrowseTerm> getFilteredTerms(BrowsingMenuFieldConfig bmfc, String startsWith, Comparator<BrowseTerm> comparator,
            boolean aggregateHits) throws PresentationException, IndexUnreachableException {
        List<BrowseTerm> ret = new ArrayList<>();
        Map<BrowseTerm, Boolean> terms = new ConcurrentHashMap<>();
        Map<String, BrowseTerm> usedTerms = new ConcurrentHashMap<>();

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append(bmfc.getField()).append(':');
        if (StringUtils.isEmpty(startsWith)) {
            sbQuery.append("[* TO *]");
        } else {
            sbQuery.append(ClientUtils.escapeQueryChars(startsWith)).append('*');
        }
        if (!bmfc.getDocstructFilters().isEmpty()) {
            sbQuery.append(" AND (");
            for (String docstruct : bmfc.getDocstructFilters()) {
                sbQuery.append(SolrConstants.DOCSTRCT).append(':').append(docstruct).append(" OR ");
            }
            sbQuery.delete(sbQuery.length() - 4, sbQuery.length());
            sbQuery.append(')');
        }
        if (bmfc.isRecordsAndAnchorsOnly()) {
            sbQuery.append(" AND (").append(SolrConstants.ISWORK).append(":true OR ").append(SolrConstants.ISANCHOR).append(":true)");
        }

        String query = buildFinalQuery(sbQuery.toString(), false);
        logger.debug("getFilteredTerms query: {}", query);
        // int rows = 0;
        int rows = SolrSearchIndex.MAX_HITS;
        List<String> fieldList = new ArrayList<>();
        fieldList.add(bmfc.getField());
        fieldList.add(SolrConstants.PI_TOPSTRUCT);
        List<String> facetFields = new ArrayList<>();
        facetFields.add(bmfc.getField());

        try {
            Map<String, String> params = new HashMap<>();
            if (DataManager.getInstance().getConfiguration().isGroupDuplicateHits()) {
                params.put(GroupParams.GROUP, "true");
                params.put(GroupParams.GROUP_MAIN, "true");
                params.put(GroupParams.GROUP_FIELD, SolrConstants.GROUPFIELD);
            }
            QueryResponse resp = DataManager.getInstance().getSearchIndex().search(query, 0, rows, null, facetFields, null, fieldList, params);
            // QueryResponse resp = DataManager.getInstance().getSolrHelper().searchFacetsAndStatistics(sbQuery.toString(), facetFields, false);
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
                ((List<SolrDocument>) resp.getResults()).parallelStream().forEach(doc -> processSolrResult(doc, bmfc.getField(), bmfc.getSortField(),
                        startsWith, terms, usedTerms, aggregateHits));

                // Sequential processing
                //                for (SolrDocument doc : resp.getResults()) {
                //                    processSolrResult(doc, bmfc.getField(), bmfc.getSortField(), startsWith, terms, usedTerms);
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
            // Extract the value part
            if (s.contains(":")) {
                String[] pairSplit = s.split(":");
                if (pairSplit.length > 1) {
                    currentField = pairSplit[0];
                    if (SolrConstants.SUPERDEFAULT.equals(currentField)) {
                        currentField = SolrConstants.DEFAULT;
                    } else if (SolrConstants.SUPERFULLTEXT.equals(currentField)) {
                        currentField = SolrConstants.FULLTEXT;
                    }
                    // Remove quotation marks from phrases
                    if (pairSplit[1].charAt(0) == '"' && pairSplit[1].charAt(pairSplit[1].length() - 1) == '"') {
                        pairSplit[1] = pairSplit[1].replace("\"", "");
                    }
                    if (pairSplit[1].length() > 0 && !stopwords.contains(pairSplit[1])) {
                        if (ret.get(currentField) == null) {
                            ret.put(currentField, new HashSet<String>());
                        }
                        ret.get(currentField).add(pairSplit[1]);
                    }
                }
            } else if (s.length() > 0 && !stopwords.contains(s)) {
                // single values w/o a field
                if (currentField == null) {
                    currentField = SolrConstants.DEFAULT;
                }
                if (ret.get(currentField) == null) {
                    ret.put(currentField, new HashSet<String>());
                }
                ret.get(currentField).add(s);
            }
        }

        return ret;
    }

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

    @Deprecated
    public static List<String> generateFacetFields() {
        List<String> facetFields = new ArrayList<>();
        if (DataManager.getInstance().getConfiguration().isGroupDuplicateHits()) {
            facetFields.add(SolrConstants.GROUPFIELD);
        }
        // if (DataManager.getInstance().getConfiguration().isCollectionDrilldownEnabled()) {
        // facetFields.add(LuceneConstants.FACET_DC);
        // }
        for (String field : DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields()) {
            if (!facetFields.contains(field)) {
                facetFields.add(field);
            }
        }
        for (String field : DataManager.getInstance().getConfiguration().getDrillDownFields()) {
            if (SolrConstants.DC.equals(field) && !facetFields.contains(SolrConstants.FACET_DC)) {
                facetFields.add(SolrConstants.FACET_DC);
            } else if (!facetFields.contains(field)) {
                facetFields.add(field);
            }
        }

        return facetFields;
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
                switch (s) {
                    case SolrConstants.DC:
                        ret.add(SolrConstants.FACET_DC);
                        break;
                    default:
                        if (s.startsWith("MD_")) {
                            s = s.replace("MD_", "FACET_");
                        }
                        s = s.replace(SolrConstants._UNTOKENIZED, "");
                        ret.add(s);
                        break;
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
     * @return
     * @should generate query correctly
     * @should return empty string if no fields match
     * @should skip PI_TOPSTRUCT
     */
    public static String generateExpandQuery(List<String> fields, Map<String, Set<String>> searchTerms) {
        StringBuilder sbOuter = new StringBuilder();
        // sbOuter.append("NOT(").append(LuceneConstants.ISWORK).append(":true OR
        // ").append(LuceneConstants.ISANCHOR).append(":true)");
        if (!searchTerms.isEmpty()) {
            logger.trace("fields: {}", fields.toString());
            logger.trace("searchTerms: {}", searchTerms.toString());
            boolean moreThanOne = false;
            for (String field : fields) {
                // Skip fields that exist in all child docs (e.g. PI_TOPSTRUCT) so that searches within a record don't return
                // every single doc}
                switch (field) {
                    case SolrConstants.PI_TOPSTRUCT:
                    case SolrConstants.DC:
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
                for (String term : terms) {
                    if (sbInner.length() > 0) {
                        sbInner.append(" OR ");
                    }
                    sbInner.append(term);
                }
                sbOuter.append(field).append(":(").append(sbInner.toString()).append(')');
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
     * @return
     * @should generate query correctly
     * @should skip PI_TOPSTRUCT
     */
    public static String generateAdvancedExpandQuery(List<SearchQueryGroup> groups) {
        StringBuilder sbOuter = new StringBuilder();
        // sbOuter.append("NOT(").append(LuceneConstants.ISWORK).append(":true OR
        // ").append(LuceneConstants.ISANCHOR).append(":true)");
        if (!groups.isEmpty()) {
            for (SearchQueryGroup group : groups) {
                StringBuilder sbGroup = new StringBuilder();
                for (SearchQueryItem item : group.getQueryItems()) {
                    // Skip fields that exist in all child docs (e.g. PI_TOPSTRUCT) so that searches within a record don't
                    // return every single doc}
                    if (item.getField() == null) {
                        continue;
                    }
                    switch (item.getField()) {
                        case SolrConstants.PI_TOPSTRUCT:
                        case SolrConstants.DC:
                            continue;
                    }
                    String itemQuery = item.generateQuery(new HashSet<String>(), false);
                    if (StringUtils.isNotEmpty(itemQuery)) {
                        if (sbGroup.length() > 0) {
                            sbGroup.append(" OR ");
                        }
                        sbGroup.append(itemQuery);
                    }
                }
                if (sbGroup.length() > 0) {
                    if (sbOuter.length() > 0) {
                        sbOuter.append(" OR ");
                    }
                    sbOuter.append('(').append(sbGroup).append(')');
                }
            }
        }
        if (sbOuter.length() > 0) {
            return " +(" + sbOuter.toString() + ')';
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
                            } else if (SolrConstants.DEFAULT.equals(item.getField()) || SolrConstants.SUPERDEFAULT.equals(item.getField()) && !ret
                                    .contains(SolrConstants.DEFAULT)) {
                                ret.add(SolrConstants.DEFAULT);
                            } else if (SolrConstants.FULLTEXT.equals(item.getField()) || SolrConstants.SUPERFULLTEXT.equals(item.getField()) && !ret
                                    .contains(SolrConstants.FULLTEXT)) {
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
     * @param params
     * @return
     * @throws IndexUnreachableException
     * @throws DAOException
     * @throws PresentationException
     * @should create excel workbook correctly
     */
    public static SXSSFWorkbook exportSearchAsExcel(String query, String exportQuery, List<StringPair> sortFields, Map<String, String> params,
            Map<String, Set<String>> searchTerms, Locale locale, boolean aggregateHits) throws IndexUnreachableException, DAOException,
            PresentationException {
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
                batch = searchWithAggregation(query, first, batchSize, sortFields, null, params, searchTerms, exportFields, locale);
            } else {
                batch = searchWithFulltext(query, first, batchSize, sortFields, null, params, searchTerms, exportFields, locale);
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
}
