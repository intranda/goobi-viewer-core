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
package io.goobi.viewer.model.search;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.Collator;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ExpandParams;
import org.jsoup.Jsoup;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DamerauLevenshtein;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.controller.sorting.AlphanumCollatorComparator;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.export.ExportFieldConfiguration;
import io.goobi.viewer.model.search.SearchQueryItem.SearchItemOperator;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.termbrowsing.BrowseTerm;
import io.goobi.viewer.model.termbrowsing.BrowseTermComparator;
import io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig;
import io.goobi.viewer.model.translations.language.LocaleComparator;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.servlets.IdentifierResolver;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrSearchIndex;
import io.goobi.viewer.solr.SolrTools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Search utility class. Static methods only.
 */
public final class SearchHelper {

    private static final Logger logger = LogManager.getLogger(SearchHelper.class);

    /** Constant <code>PARAM_NAME_FILTER_QUERY_SUFFIX="filterQuerySuffix"</code> */
    public static final String PARAM_NAME_FILTER_QUERY_SUFFIX = "filterQuerySuffix";
    /** Constant <code>SEARCH_TERM_SPLIT_REGEX</code> */
    public static final String SEARCH_QUERY_SPLIT_REGEX = "[ ,・]";
    /** Regex for splitting search values (including colons). */
    public static final String SEARCH_TERM_SPLIT_REGEX = "[ ,・:]";
    /** Constant <code>PLACEHOLDER_HIGHLIGHTING_START="##HLS##"</code> */
    public static final String PLACEHOLDER_HIGHLIGHTING_START = "##ĦŁ$##";
    /** Constant <code>PLACEHOLDER_HIGHLIGHTING_END="##HLE##"</code> */
    public static final String PLACEHOLDER_HIGHLIGHTING_END = "##ĦŁȄ##";
    /** Constant <code>SEARCH_TYPE_REGULAR=0</code> */
    public static final int SEARCH_TYPE_REGULAR = 0;
    /** Constant <code>SEARCH_TYPE_ADVANCED=1</code> */
    public static final int SEARCH_TYPE_ADVANCED = 1;
    /** Constant <code>SEARCH_TYPE_CALENDAR=3</code> */
    public static final int SEARCH_TYPE_CALENDAR = 3;
    /** Constant <code>SEARCH_TYPE_TERMS=4</code> */
    public static final int SEARCH_TYPE_TERMS = 4;
    /** Constant <code>SEARCH_FILTER_ALL</code> */
    public static final SearchFilter SEARCH_FILTER_ALL = new SearchFilter("filter_ALL", "ALL", false);
    public static final String SEARCH_FILTER_ALL_FIELD = "ALL";
    public static final String TITLE_TERMS = "_TITLE_TERMS";
    public static final String AGGREGATION_QUERY_PREFIX = "{!join from=PI_TOPSTRUCT to=PI}";
    public static final String BOOSTING_QUERY_TEMPLATE = "(+" + SolrConstants.PI + ":* +" + SolrConstants.TITLE + ":({0}))^20.0";
    public static final String EMBEDDED_QUERY_TEMPLATE = "_query_:\"{0}\"";
    /** Standard Solr query for all records and anchors. */
    public static final String ALL_RECORDS_QUERY = "+(ISWORK:true ISANCHOR:true)";
    /** Constant <code>DEFAULT_DOCSTRCT_WHITELIST_FILTER_QUERY="(ISWORK:true OR ISANCHOR:true) AND NOT("{trunked}</code> */
    public static final String DEFAULT_DOCSTRCT_WHITELIST_FILTER_QUERY = ALL_RECORDS_QUERY + " -IDDOC_PARENT:*";
    /**
     * Constant <code>FUZZY_SEARCH_TERM_TEMPLATE_WITH_BOOST="String prefix, String suffix"</code>. {t} is the actual search term, {d} the maximal edit
     * distance to search. {p} and {s} are prefix and suffix to be applied to the search term
     */
    public static final String FUZZY_SEARCH_TERM_TEMPLATE_WITH_BOOST = "{p}{t}{s} {t}~{d}";
    /**
     * Constant <code>FUZZY_SEARCH_TERM_TEMPLATE="String prefix, String suffix"</code>. {t} is the actual search term, {d} the maximal edit distance
     * to search.
     */
    public static final String FUZZY_SEARCH_TERM_TEMPLATE = "{t}~{d}";

    private static final Object LOCK = new Object();

    private static final Random RANDOM = new SecureRandom();

    /** Regex pattern for negations in brackets */
    private static final Pattern PATTERN_NOT_BRACKETS = Pattern.compile("NOT\\([^()]*\\)");
    /** Regex pattern for negations not followed by brackets */
    private static final Pattern PATTERN_NOT = Pattern.compile("NOT [a-zA-Z_]+:[a-zA-Z0-9\\*]+");
    /** Constant <code>REGEX_QUOTATION_MARKS="\"[^()]*?\""</code>. */
    public static final String REGEX_QUOTATION_MARKS = "@?+\"[^()\"]*\"@?+";
    /** Constant <code>PATTERN_FIELD_PHRASE</code> */
    private static final Pattern PATTERN_FIELD_PHRASE = Pattern.compile("[\\w]++:" + REGEX_QUOTATION_MARKS); //NOSONAR Checked and fixed potential CB
    /** Constant <code>PATTERN_PHRASE</code> */
    private static final Pattern PATTERN_PHRASE = Pattern.compile("^" + REGEX_QUOTATION_MARKS + "(~\\d+)?$");
    /** Constant <code>PATTERN_PROXIMITY_SEARCH_TOKEN</code> */
    private static final Pattern PATTERN_PROXIMITY_SEARCH_TOKEN = Pattern.compile("(?<=\")~(\\d+)");
    /** Constant <code>PATTERN_YEAR_RANGE</code> */
    private static final Pattern PATTERN_YEAR_RANGE = Pattern.compile("\\[\\d+ TO \\d+\\]");
    /** Constant <code>PATTERN_HYPHEN_LINK</code> */
    private static final Pattern PATTERN_HYPHEN_LINK = Pattern.compile("(<a (?:(?!<\\/a>).)*<\\/a>)");

    //No danger of catastrophic backtracking, because the ':' separator is not matched by \w
    private static final Pattern PATTERN_FACET_STRING = Pattern.compile("(\\w+:\\w+);;"); //NOSONAR

    // Regex patterns for parsing advanced query items from query

    public static final Pattern PATTERN_ALL_ITEMS = Pattern.compile(
            "[+-]*\\((\\w+:\\\"[\\wäáàâöóòôüúùûëéèêßñ ]+\\\" *)+\\)|[+-]*\\(((\\w+:\\([\\wäáàâöóòôüúùûëéèêßñ ]+\\)) *)++\\)"
                    + "|[+-]*\\((\\w+:\\(\\[[\\wäáàâöóòôüúùûëéèêßñ]+ TO [\\wäáàâöóòôüúùûëéèêßñ]+\\]\\) *+)\\)");

    //No danger of catastrophic backtracking: the repetitions are separated by other characters and the overal repetition is possessive ('++')
    private static final Pattern PATTERN_REGULAR_ITEMS = Pattern.compile("([+-]*)\\(((\\w+:\\([\\wäáàâöóòôüúùûëéèêßñ ]+\\)) *)++\\)"); //NOSONAR
    //No danger of catastrophic backtracking: separator (':') between the repetition
    private static final Pattern PATTERN_REGULAR_PAIRS = Pattern.compile("(\\w+:\\([\\wäáàâöóòôüúùûëéèêßñ ()]+\\))"); //NOSONAR
    //No danger of catastrophic backtracking: inner repetition has a separator and outer repetition is possessive
    private static final Pattern PATTERN_PHRASE_ITEMS = Pattern.compile("([+-]*)\\((\\w+:\\\"[\\wäáàâöóòôüúùûëéèêßñ ]+\\\" *)++\\)"); //NOSONAR
    //No danger of catastrophic backtracking: separator (':\"') between the repetition
    private static final Pattern PATTERN_PHRASE_PAIRS = Pattern.compile("(\\w+:\"[\\wäáàâöóòôüúùûëéèêßñ ]+\")"); //NOSONAR
    //No danger of catastrophic backtracking: inner repetition has a separator and outer repetition is possessive
    private static final Pattern PATTERN_RANGE_ITEMS =
            Pattern.compile("([+-]*)\\((\\w+:\\(\\[[\\wäáàâöóòôüúùûëéèêßñ]+ TO [\\wäáàâöóòôüúùûëéèêßñ]+\\]\\) *+)\\)"); //NOSONAR
    //No danger of catastrophic backtracking: use possessive repetition
    private static final Pattern PATTERN_RANGE_PAIRS =
            Pattern.compile("(\\w++:\\(\\[[\\wäáàâöóòôüúùûëéèêßñ]++ TO [\\wäáàâöóòôüúùûëéèêßñ]++\\]\\))"); //NOSONAR

    /**
     *
     */
    private SearchHelper() {
        //
    }

    /**
     * Main search method for flat search.
     *
     * @param query {@link java.lang.String} Solr search query. Merges full-text and metadata hits into their corresponding docstructs.
     * @param first {@link java.lang.Integer} First row index
     * @param rows {@link java.lang.Integer} Number of rows to return
     * @param sortFields a {@link java.util.List} object.
     * @param resultFields a {@link java.util.List} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param exportFields a {@link java.util.List} object.
     * @param locale a {@link java.util.Locale} object.
     * @param proximitySearchDistance
     * @return List of <code>StructElement</code>s containing the search hits.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static List<SearchHit> searchWithFulltext(String query, int first, int rows, List<StringPair> sortFields, List<String> resultFields,
            List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms, List<String> exportFields, Locale locale,
            int proximitySearchDistance)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        return searchWithFulltext(query, first, rows, sortFields, resultFields, filterQueries, params, searchTerms, exportFields, locale, false,
                proximitySearchDistance);
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
     * @param keepSolrDoc
     * @param proximitySearchDistance
     * @return List of <code>StructElement</code>s containing the search hits.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static List<SearchHit> searchWithFulltext(String query, int first, int rows, List<StringPair> sortFields, List<String> resultFields,
            List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms, List<String> exportFields, Locale locale,
            boolean keepSolrDoc, int proximitySearchDistance) throws PresentationException, IndexUnreachableException, DAOException {
        Map<String, SolrDocument> ownerDocs = new HashMap<>();
        QueryResponse resp =
                DataManager.getInstance().getSearchIndex().search(query, first, rows, sortFields, null, resultFields, filterQueries, params);
        if (resp.getResults() == null) {
            return Collections.emptyList();
        }
        if (params != null) {
            logger.trace("params: {}", params);
        }
        logger.trace("hits found: {}; results returned: {}", resp.getResults().getNumFound(), resp.getResults().size());

        List<SearchHit> ret = new ArrayList<>(resp.getResults().size());
        int count = first;
        ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();
        SearchHitFactory factory = new SearchHitFactory(searchTerms, sortFields, exportFields, proximitySearchDistance, thumbs, locale);
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
                    HttpServletRequest request = BeanUtils.getRequest();
                    if (StringUtils.isNotBlank(plaintextFilename)) {
                        boolean access = AccessConditionUtils
                                .checkAccess(request.getSession(), "text", pi, plaintextFilename, NetTools.getIpAddress(request), false)
                                .isGranted();
                        if (access) {
                            fulltext = DataFileTools.loadFulltext(null, plaintextFilename, false);
                        } else {
                            fulltext = ViewerResourceBundle.getTranslation("fulltextAccessDenied", null);
                        }
                    } else if (StringUtils.isNotBlank(altoFilename)) {
                        boolean access =
                                AccessConditionUtils
                                        .checkAccess(request.getSession(), "text", pi, altoFilename, NetTools.getIpAddress(request), false)
                                        .isGranted();
                        if (access) {
                            fulltext = DataFileTools.loadFulltext(altoFilename, null, false);
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

            SearchHit hit = factory.createSearchHit(doc, ownerDoc, fulltext, null);
            if (keepSolrDoc) {
                hit.setSolrDoc(doc);
            }
            hit.setHitNumber(++count);
            ret.add(hit);
        }

        return ret;
    }

    /**
     * Main search method for aggregated search.
     *
     * @param query {@link java.lang.String} Solr search query. Merges full-text and metadata hits into their corresponding docstructs.
     * @param first {@link java.lang.Integer} First hit index
     * @param rows {@link java.lang.Integer} Number of hits to return
     * @param sortFields a {@link java.util.List} object.
     * @param resultFields a {@link java.util.List} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param exportFields a {@link java.util.List} object.
     * @param additionalMetadataListType Optional addtional metadata list type, to be used on alternative search hit views, etc.
     * @param locale a {@link java.util.Locale} object.
     * @param keepSolrDoc
     * @param proximitySearchDistance
     * @return List of <code>StructElement</code>s containing the search hits.
     * @should return all hits
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static List<SearchHit> searchWithAggregation(String query, int first, int rows, List<StringPair> sortFields,
            List<String> resultFields, List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms,
            List<String> exportFields, String additionalMetadataListType, Locale locale, boolean keepSolrDoc, int proximitySearchDistance)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (query != null) {
            String s = query.replaceAll("[\n\r]", "_");
            logger.trace("searchWithAggregation: {}", s);
        }
        logger.trace("hitsPerPage: {}", rows);
        QueryResponse resp =
                DataManager.getInstance().getSearchIndex().search(query, first, rows, sortFields, null, resultFields, filterQueries, params);
        if (resp.getResults() == null) {
            return new ArrayList<>();
        }
        logger.trace("hits found: {}; results returned: {}", resp.getResults().getNumFound(), resp.getResults().size());
        List<SearchHit> ret = new ArrayList<>(resp.getResults().size());
        ThumbnailHandler thumbs = BeanUtils.getImageDeliveryBean().getThumbs();

        SearchHitFactory factory = new SearchHitFactory(searchTerms, sortFields, exportFields, proximitySearchDistance, thumbs, locale);
        if (StringUtils.isNotBlank(additionalMetadataListType)) {
            factory.setAdditionalMetadataListType(additionalMetadataListType);
        }

        int count = first;
        Map<String, SolrDocumentList> childDocsMap = resp.getExpandedResults();
        for (SolrDocument doc : resp.getResults()) {
            // logger.trace("result iddoc: {}", doc.getFieldValue(SolrConstants.IDDOC)); //NOSONAR Debug

            // Create main hit
            // logger.trace("Creating search hit from {}", doc); //NOSONAR Debug
            SearchHit hit = factory.createSearchHit(doc, null, null, null);
            if (keepSolrDoc) {
                hit.setSolrDoc(doc);
            }
            ret.add(hit);
            int populatedChildHits = hit.addCMSPageChildren();
            populatedChildHits += hit.addFulltextChild(doc, locale != null ? locale.getLanguage() : null);
            hit.setHitsPreloaded(populatedChildHits);
            // logger.trace("Added search hit {}", hit.getBrowseElement().getLabel()); //NOSONAR Debug
            // Collect Solr docs of child hits
            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            String iddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
            if (pi != null && childDocsMap != null) {

                SolrDocumentList childDocs = childDocsMap.getOrDefault(pi, new SolrDocumentList());
                logger.trace("{} child hits found for {}", childDocs.size(), pi);
                childDocs = filterChildDocs(childDocs, iddoc, searchTerms, factory);
                logger.trace("{} child hits left after filtering.", childDocs.size());
                hit.setChildDocs(childDocs);

                // Check whether user may see full-text, before adding them to count
                boolean fulltextAccessGranted = false; // Initial value should be false to avoid counting page docs that don't have text
                if (Boolean.TRUE.equals(SolrTools.getAsBoolean(doc.getFieldValue(SolrConstants.FULLTEXTAVAILABLE)))) {
                    fulltextAccessGranted =
                            AccessConditionUtils.isPrivilegeGrantedForDoc(doc, IPrivilegeHolder.PRIV_VIEW_FULLTEXT, BeanUtils.getRequest());
                    logger.trace("Full-text access checked for {} and is: {}", pi, fulltextAccessGranted);
                }

                for (SolrDocument childDoc : childDocs) {
                    // if this is a metadata/docStruct hit directly in the top document, don't add to hit count
                    // It will simply be added to the metadata list of the main hit
                    HitType hitType = getHitType(childDoc);
                    if (hitType != null) {
                        switch (hitType) {
                            case METADATA:
                                break;
                            case PAGE:
                                if (fulltextAccessGranted) {
                                    int hitTypeCount = hit.getHitTypeCounts().get(hitType) != null ? hit.getHitTypeCounts().get(hitType) : 0;
                                    hit.getHitTypeCounts().put(hitType, hitTypeCount + 1);
                                }
                                break;
                            default:
                                int hitTypeCount = hit.getHitTypeCounts().get(hitType) != null ? hit.getHitTypeCounts().get(hitType) : 0;
                                hit.getHitTypeCounts().put(hitType, hitTypeCount + 1);
                                break;
                        }
                    }
                }
            }
            hit.setHitNumber(++count);
        }
        logger.trace("Return {} search hits", ret.size());
        return ret;
    }

    /**
     * 
     * @param docs
     * @param mainIdDoc
     * @param searchTerms
     * @param factory
     * @return {@link SolrDocumentList}
     */
    private static SolrDocumentList filterChildDocs(SolrDocumentList docs, String mainIdDoc, Map<String, Set<String>> searchTerms,
            SearchHitFactory factory) {
        SolrDocumentList filteredList = new SolrDocumentList();
        Map<String, SolrDocument> ownerDocs = new HashMap<>();
        for (SolrDocument doc : docs) {
            HitType hitType = getHitType(doc);
            String ownerIDDoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC_OWNER);
            String iddoc = SolrTools.getSingleFieldStringValue(doc, SolrConstants.IDDOC);
            if (hitType == HitType.METADATA && !Objects.equals(mainIdDoc, ownerIDDoc)) {
                //ignore metadata docs not in the main doc
                continue;
            }
            if (hitType == HitType.PAGE) {
                filteredList.add(doc);
            } else if (containsSearchTerms(doc, searchTerms, factory)) {
                filteredList.add(doc);
                if (hitType == HitType.DOCSTRCT) {
                    ownerDocs.remove(iddoc); //remove from owner map because it is already in result list
                } else if (!Objects.equals(mainIdDoc, ownerIDDoc)) {
                    if (ownerDocs.containsKey(ownerIDDoc)) {
                        SolrDocument ownerDoc = ownerDocs.get(ownerIDDoc);
                        if (ownerDoc != null) {
                            ownerDocs.remove(ownerIDDoc);
                            filteredList.add(ownerDoc);
                        }
                    } else {
                        ownerDocs.put(ownerIDDoc, null); //put an empty entry to mark that the owner doc needs to be added to result list
                    }
                }
            } else if (hitType == HitType.DOCSTRCT) {
                if (ownerDocs.containsKey(iddoc)) {
                    ownerDocs.remove(iddoc);
                    filteredList.add(doc);
                } else {
                    ownerDocs.put(iddoc, doc);
                }
            }

        }
        filteredList.setNumFound(filteredList.size());
        return filteredList;
    }

    private static boolean containsSearchTerms(SolrDocument doc, Map<String, Set<String>> searchTerms, SearchHitFactory factory) {
        return !factory
                .findAdditionalMetadataFieldsContainingSearchTerms(SolrTools.getFieldValueMap(doc), searchTerms, Collections.emptySet(), "", "")
                .isEmpty();
    }

    /**
     * Return the {@link HitType} matching the {@link io.goobi.viewer.solr.SolrConstants#DOCTYPE} of the given document. In case the document is of
     * type 'UGC', return the type matching {@link io.goobi.viewer.solr.SolrConstants#UGCTYPE} instead
     *
     * @param doc
     * @return {@link HitType} for doc
     */
    public static HitType getHitType(SolrDocument doc) {
        // logger.trace("getHitType: {}", doc.getFieldValue(SolrConstants.IDDOC)); //NOSONAR Debug
        String docType = (String) doc.getFieldValue(SolrConstants.DOCTYPE);
        HitType hitType = HitType.getByName(docType);
        if (DocType.UGC.name().equals(docType)) {
            // For user-generated content hits use the metadata type for the hit type
            String ugcType = (String) doc.getFieldValue(SolrConstants.UGCTYPE);
            // logger.trace("ugcType: {}", ugcType); //NOSONAR Debug
            if (StringUtils.isNotEmpty(ugcType)) {
                // hitType = HitType.getByName(ugcType); //NOSONAR Debug
                logger.trace("hit type found: {}", hitType);
            }
        }
        return hitType;
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @return a {@link java.lang.String} object.
     */
    public static String getAllSuffixes() {
        return getAllSuffixes(BeanUtils.getRequest(), true, true);
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @param addStaticQuerySuffix a boolean.
     * @param addCollectionBlacklistSuffix a boolean.
     * @return Generated Solr query suffix
     */
    public static String getAllSuffixes(HttpServletRequest request, boolean addStaticQuerySuffix, boolean addCollectionBlacklistSuffix) {
        return getAllSuffixes(request, false, addStaticQuerySuffix, addCollectionBlacklistSuffix, IPrivilegeHolder.PRIV_LIST);
    }

    /**
     * 
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @param addStaticQuerySuffix a boolean.
     * @param addCollectionBlacklistSuffix a boolean.
     * @param privilege Privilege to check (Connector checks a different privilege)
     * @return Generated Solr query suffix
     */
    public static String getAllSuffixes(HttpServletRequest request, boolean addStaticQuerySuffix, boolean addCollectionBlacklistSuffix,
            String privilege) {
        return getAllSuffixes(request, false, addStaticQuerySuffix, addCollectionBlacklistSuffix, privilege);
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @param addArchiveFilterSuffix a boolean.
     * @param addCollectionBlacklistSuffix a boolean.
     * @param privilege Privilege to check (Connector checks a different privilege)
     * @param addStaticQuerySuffix if true, add the configured static query suffix to the query
     * @return Generated Solr query suffix
     * @should add archive filter suffix
     * @should add static suffix
     * @should not add static suffix if not requested
     * @should add collection blacklist suffix
     * @should add discriminator value suffix
     */
    public static String getAllSuffixes(HttpServletRequest request, boolean addArchiveFilterSuffix, boolean addStaticQuerySuffix,
            boolean addCollectionBlacklistSuffix, String privilege) {
        StringBuilder sbSuffix = new StringBuilder("");
        if (addArchiveFilterSuffix) {
            sbSuffix.append(" -").append(SolrConstants.DOCTYPE).append(':').append(DocType.ARCHIVE.toString());
        }
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
        String filterQuerySuffix = getFilterQuerySuffix(request, privilege);
        // logger.trace("filterQuerySuffix: {}", filterQuerySuffix); //NOSONAR Debug
        if (filterQuerySuffix != null) {
            sbSuffix.append(filterQuerySuffix);
        }

        return sbSuffix.toString();
    }

    /**
     * Returns all suffixes relevant to search filtering.
     *
     * @return a {@link java.lang.String} object.
     */
    public static String getAllSuffixesExceptCollectionBlacklist() {
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
     * @param proximitySearchDistance
     * @should return correct hit for non-aggregated search
     * @should return correct hit for aggregated search
     * @param filterQueries a {@link java.util.List} object.
     * @return a {@link io.goobi.viewer.model.search.BrowseElement} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static BrowseElement getBrowseElement(String query, int index, List<StringPair> sortFields, List<String> filterQueries,
            Map<String, String> params, Map<String, Set<String>> searchTerms, Locale locale, int proximitySearchDistance)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        String finalQuery = prepareQuery(query);
        // String termQuery = SearchHelper.buildTermQuery(searchTerms.get(SearchHelper.TITLE_TERMS));
        finalQuery = buildFinalQuery(finalQuery, true, SearchAggregationType.AGGREGATE_TO_TOPSTRUCT);
        logger.trace("getBrowseElement final query: {}", finalQuery);
        List<SearchHit> hits =
                SearchHelper.searchWithAggregation(finalQuery, index, 1, sortFields, null, filterQueries, params, searchTerms, null,
                        Configuration.METADATA_LIST_TYPE_SEARCH_HIT, locale, false,
                        proximitySearchDistance);
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
     * @param splittingChar a {@link java.lang.String} object.
     * @return StringPair containing the PI and the target page type of the first record.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public static StringPair getFirstRecordPiAndPageType(String luceneField, String value, boolean filterForWhitelist,
            boolean filterForBlacklist, final String splittingChar) throws IndexUnreachableException, PresentationException {
        // logger.trace("getFirstRecordPiAndPageType: {}:{}", luceneField, value); //NOSONAR Debug
        if (luceneField == null || value == null) {
            return null;
        }

        String useSplittingChar = splittingChar;
        if (StringUtils.isEmpty(useSplittingChar)) {
            useSplittingChar = DataManager.getInstance().getConfiguration().getCollectionSplittingChar(luceneField);
        }

        StringBuilder sbQuery = new StringBuilder(SolrConstants.PI).append(":*");
        if (filterForWhitelist) {
            if (sbQuery.length() > 0) {
                sbQuery.append(SolrConstants.SOLR_QUERY_AND);
            }
            sbQuery.append('(').append(getDocstrctWhitelistFilterQuery()).append(')');
        }
        sbQuery.append(SearchHelper.getAllSuffixesExceptCollectionBlacklist());
        sbQuery.append(" AND (")
                .append(luceneField)
                .append(":")
                .append(value)
                .append(SolrConstants.SOLR_QUERY_OR)
                .append(luceneField)
                .append(":")
                .append(value + useSplittingChar + "*)");
        if (filterForBlacklist) {
            sbQuery.append(getCollectionBlacklistFilterSuffix(luceneField));
        }

        List<String> fields =
                Arrays.asList(SolrConstants.PI, SolrConstants.MIMETYPE, SolrConstants.DOCSTRCT, SolrConstants.THUMBNAIL, SolrConstants.ISANCHOR,
                        SolrConstants.ISWORK, SolrConstants.LOGID);
        logger.trace("first record query: {}", sbQuery);
        QueryResponse resp = DataManager.getInstance().getSearchIndex().search(sbQuery.toString(), 0, 1, null, null, fields);

        if (resp.getResults().isEmpty()) {
            return null;
        }

        try {
            SolrDocument doc = resp.getResults().get(0);
            IdentifierResolver.constructUrl(doc, false);
            String pi = (String) doc.getFieldValue(SolrConstants.PI);
            if (!AccessConditionUtils.checkAccessPermissionByIdentifierAndLogId(pi, null, IPrivilegeHolder.PRIV_LIST,
                    BeanUtils.getRequest()).isGranted()) {
                // TODO check whether users with permissions still skip over such records
                logger.trace("Record '{}' does not allow listing, skipping...", pi);
                throw new RecordNotFoundException(pi);
            }

            boolean anchorOrGroup = SolrTools.isAnchor(doc) || SolrTools.isGroup(doc);
            PageType pageType =
                    PageType.determinePageType((String) doc.get(SolrConstants.DOCSTRCT), (String) doc.get(SolrConstants.MIMETYPE), anchorOrGroup,
                            doc.containsKey(SolrConstants.THUMBNAIL), false);
            return new StringPair(pi, pageType.name());
        } catch (RecordNotFoundException e) {
            //
        } catch (DAOException e) {
            logger.error("Failed to retrieve record", e);
        }

        return null;
    }

    /**
     * Returns a Map with hierarchical values from the given field and their respective record counts. Results are filtered by AccessConditions
     * available for current HttpRequest
     *
     * @param luceneField the SOLR field over which to build the collections (typically "DC")
     * @param groupingField
     * @param filterQuery An addition solr-query to filer collections by.
     * @param filterForWhitelist a boolean.
     * @param filterForBlacklist a boolean.
     * @param splittingChar Character used for separating collection hierarchy levels within a collection name (typically ".")
     * @return a {@link java.util.Map} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should find all collections
     */
    public static Map<String, CollectionResult> findAllCollectionsFromField(String luceneField, String groupingField, String filterQuery,
            boolean filterForWhitelist, boolean filterForBlacklist, String splittingChar) throws IndexUnreachableException {
        logger.debug("findAllCollectionsFromField: {}", luceneField);
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
                    sbQuery.append(SolrConstants.SOLR_QUERY_AND);
                }
                sbQuery.append("+(").append(getDocstrctWhitelistFilterQuery()).append(')');
            }
            sbQuery.append(SearchHelper.getAllSuffixesExceptCollectionBlacklist());
            if (filterForBlacklist) {
                sbQuery.append(getCollectionBlacklistFilterSuffix(luceneField));
            }
            logger.trace("Collection query: {}", sbQuery);

            // Iterate over record hits instead of using facets to determine the size of the parent collections

            FacetField facetResults = null;
            FacetField groupResults = null;
            List<String> facetFields = new ArrayList<>();
            facetFields.add(luceneField);
            if (StringUtils.isNotBlank(groupingField)) {
                facetFields.add(groupingField);
            }
            QueryResponse response = DataManager.getInstance()
                    .getSearchIndex()
                    .searchFacetsAndStatistics(sbQuery.toString(), null, facetFields, 1, false);
            facetResults = response.getFacetField(luceneField);
            groupResults = response.getFacetField(groupingField);

            Map<String, CollectionResult> ret = createCollectionResults(facetResults, splittingChar);

            addGrouping(ret, luceneField, groupResults, sbQuery.toString());

            logger.debug("{} collections found", ret.size());
            return ret;

        } catch (PresentationException e) {
            logger.debug(e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     *
     * @param facetResults
     * @param splittingChar
     * @return Map<String, CollectionResult>
     */
    private static Map<String, CollectionResult> createCollectionResults(FacetField facetResults, String splittingChar) {
        Map<String, CollectionResult> ret = new HashMap<>();

        Set<String> counted = new HashSet<>();
        for (Count count : facetResults.getValues()) {
            String dc = count.getName();
            // Skip inverted values
            if (StringTools.checkValueEmptyOrInverted(dc)) {
                continue;
            }

            CollectionResult result = ret.computeIfAbsent(dc, k -> new CollectionResult(dc));
            result.incrementCount(count.getCount());

            if (dc.contains(splittingChar) && !counted.contains(dc)) {
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

        return ret;
    }

    /**
     *
     * @param ret
     * @param luceneField
     * @param groupResults
     * @param filterQuery
     */
    private static void addGrouping(Map<String, CollectionResult> ret, String luceneField, FacetField groupResults, String filterQuery) {
        if (groupResults != null) {
            String groupField = groupResults.getName();
            groupResults.getValues()
                    .parallelStream()
                    .map(Count::getName)
                    .forEach(name -> {
                        try {
                            String query = filterQuery + " " + String.format("+%s:%s", groupField, name);
                            QueryResponse response = DataManager.getInstance()
                                    .getSearchIndex()
                                    .searchFacetsAndStatistics(query, null, Collections.singletonList(luceneField), 1, false);
                            FacetField facetField = response.getFacetField(luceneField);
                            for (Count count : facetField.getValues()) {
                                String collectionName = count.getName();
                                if (collectionName.startsWith("#1;") || collectionName.startsWith("\\u0001")
                                        || collectionName.startsWith("\u0001")) {
                                    continue;
                                }
                                getAllParentCollections(collectionName, ".", true).stream()
                                        .forEach(col -> {
                                            CollectionResult collectionResult = ret.get(col);
                                            collectionResult.addFacetValues(Collections.singletonList(name));
                                        });
                            }
                        } catch (PresentationException | IndexUnreachableException e) {
                            logger.error("Error applying grouping to collection: {}", e.toString());
                        }
                    });
        }
    }

    /**
     * @param collectionName
     * @param splittingChar
     * @param includeSelf
     * @return Collection<String>
     */
    private static Collection<String> getAllParentCollections(final String collectionName, String splittingChar, boolean includeSelf) {
        List<String> collections = new ArrayList<>();
        if (includeSelf) {
            collections.add(collectionName);
        }
        String colName = collectionName;
        while (StringUtils.isNotBlank(colName)) {
            int dotIndex = colName.lastIndexOf(splittingChar);
            if (dotIndex > -1) {
                colName = colName.substring(0, dotIndex);
                collections.add(colName);
            } else {
                break;
            }
        }
        Collections.reverse(collections);
        return collections;
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
        String[] dcSplit = dc.split(collectionSplitRegex);
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
     * @return int[]
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public static int[] getMinMaxYears(String subQuery) throws PresentationException, IndexUnreachableException {
        int[] ret = { -1, -1 };

        String searchString = String.format("+%s:*", SolrConstants.CALENDAR_YEAR);
        if (StringUtils.isNotBlank(subQuery)) {
            searchString += " " + subQuery;
        }

        // logger.debug("searchString: {}", searchString);
        QueryResponse resp = searchCalendar(searchString, Collections.singletonList(SolrConstants.CALENDAR_YEAR), 0, true);

        FieldStatsInfo info = resp.getFieldStatsInfo().get(SolrConstants.CALENDAR_YEAR);
        Object min = info.getMin();
        if (min instanceof Long || min instanceof Integer) {
            ret[0] = (int) min;
        } else if (min instanceof Double d) {
            ret[0] = d.intValue();
        }
        Object max = info.getMax();
        if (max instanceof Long || max instanceof Integer) {
            ret[1] = (int) max;
        } else if (max instanceof Double d) {
            ret[1] = d.intValue();
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
                        sbQuery.append(SolrConstants.SOLR_QUERY_AND);
                    }
                    sbQuery.append(facetItem.getQueryEscapedLink());
                    logger.trace("Added  facet: {}", facetItem.getQueryEscapedLink());
                }
            }
            sbQuery.append(getAllSuffixes());
            logger.debug("Autocomplete query: {}", sbQuery);

            QueryResponse response = DataManager.getInstance()
                    .getSearchIndex()
                    .searchFacetsAndStatistics(sbQuery.toString(), null,
                            Collections.singletonList(SolrConstants.PREFIX_FACET + SolrConstants.DEFAULT), 1, null, false);
            FacetField facetField = response.getFacetFields().get(0);

            logger.trace(facetField.getValues().size());
            ret = facetField.getValues()
                    .stream()
                    .filter(count -> count.getName().toLowerCase().startsWith(suggestLower))
                    .sorted((c1, c2) -> Long.compare(c2.getCount(), c1.getCount()))
                    .map(Count::getName)
                    .distinct()
                    .toList();
        } catch (PresentationException e) {
            logger.debug(StringConstants.LOG_PRESENTATION_EXCEPTION_THROWN_HERE, e.getMessage());
        }

        logger.trace("Autocomplete size: {}", ret.size());
        return ret;
    }

    /**
     * @return Filter query for record listing
     */
    static String getDocstrctWhitelistFilterQuery() {
        return DataManager.getInstance().getConfiguration().getDocstrctWhitelistFilterQuery();
    }

    /**
     * Returns a Solr query suffix that filters out collections defined in the collection blacklist.
     *
     * @param field a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getCollectionBlacklistFilterSuffix(String field) {
        return generateCollectionBlacklistFilterSuffix(field);
    }

    /**
     * Generates a Solr query suffix that filters out values for the given field that are configured as blacklisted. This isn't an expensive method,
     * so the suffix is generated anew upon every call and not persisted.
     *
     * @param field a {@link java.lang.String} object.
     * @should construct suffix correctly
     * @return a {@link java.lang.String} object.
     */
    protected static String generateCollectionBlacklistFilterSuffix(String field) {
        // logger.trace("Generating blacklist suffix for field '{}'...", field); //NOSONAR Debug
        StringBuilder sbQuery = new StringBuilder();
        List<String> list = DataManager.getInstance().getConfiguration().getCollectionBlacklist(field);
        if (list != null && !list.isEmpty()) {
            for (String s : list) {
                if (StringUtils.isNotBlank(s)) {
                    sbQuery.append(" -").append(field).append(':').append(s.trim());
                }
            }
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
        // logger.trace("nh null? {}", nh == null); //NOSONAR Debug
        logger.trace("discriminatorField: {}", discriminatorField);
        if (StringUtils.isNotEmpty(discriminatorField) && nh != null) {
            String discriminatorValue = nh.getSubThemeDiscriminatorValue();
            logger.trace("discriminatorValue: {}", discriminatorValue);
            if (StringUtils.isNotEmpty(discriminatorValue) && !"-".equals(discriminatorValue)) {
                StringBuilder sbSuffix = new StringBuilder();
                sbSuffix.append(" +").append(discriminatorField).append(':').append(discriminatorValue);
                logger.trace("Discriminator field suffix: {}", sbSuffix);
                return sbSuffix.toString();
            }
        }

        return "";
    }

    /**
     * Updates the calling agent's session with a personalized filter sub-query.
     *
     * @param request a {@link jakarta.servlet.http.HttpServletRequest} object.
     * @param privilege Privilege to check (Connector checks a different privilege)
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public static void updateFilterQuerySuffix(HttpServletRequest request, String privilege)
            throws IndexUnreachableException, PresentationException, DAOException {
        String filterQuerySuffix =
                getPersonalFilterQuerySuffix(DataManager.getInstance().getDao().getRecordLicenseTypes(),
                        (User) Optional.ofNullable(request)
                                .map(HttpServletRequest::getSession)
                                .map(session -> session.getAttribute("user"))
                                .orElse(null),
                        NetTools.getIpAddress(request),
                        ClientApplicationManager.getClientFromRequest(request), privilege);
        logger.trace("New filter query suffix: {}", filterQuerySuffix);
        if (request != null) {
            request.getSession().setAttribute(PARAM_NAME_FILTER_QUERY_SUFFIX, filterQuerySuffix);
        } else {
            logger.warn("No HttpServletRequest found, cannot set filter query.");
        }
    }

    /**
     * Constructs a personal search query filter suffix for the given user and IP address.
     *
     * @param licenseTypes
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param ipAddress a {@link java.lang.String} object.
     * @param client
     * @param privilege Privilege to check (Connector checks a different privilege)
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should construct suffix correctly
     * @should construct suffix correctly if user has license privilege
     * @should construct suffix correctly if user has overriding license privilege
     * @should construct suffix correctly if ip range has license privilege
     * @should construct suffix correctly if moving wall license
     * @should construct suffix correctly for alternate privilege
     * @should limit to open access if licenseTypes empty
     */
    public static String getPersonalFilterQuerySuffix(List<LicenseType> licenseTypes, User user, String ipAddress, Optional<ClientApplication> client,
            String privilege) throws IndexUnreachableException, PresentationException, DAOException {
        logger.trace("getPersonalFilterQuerySuffix: {}", ipAddress);
        if (privilege == null) {
            throw new IllegalArgumentException("privilege may not be null");
        }

        // No restrictions for admins
        if (user != null && user.isSuperuser()) {
            return "";
        }

        // No restrictions for localhost, if so configured
        if (NetTools.isIpAddressLocalhost(ipAddress) && DataManager.getInstance().getConfiguration().isFullAccessForLocalhost()) {
            return "";
        }

        StringBuilder query = new StringBuilder();
        query.append(" +(").append(SolrConstants.ACCESSCONDITION).append(":\"").append(SolrConstants.OPEN_ACCESS_VALUE).append('"');

        // No configured LicenseTypes - OPENACCESS only
        if (licenseTypes == null || licenseTypes.isEmpty()) {
            query.append(')');
            return query.toString();
        }

        Set<String> usedLicenseTypes = new HashSet<>();
        for (LicenseType licenseType : licenseTypes) {
            if (usedLicenseTypes.contains(licenseType.getName())) {
                continue;
            }

            // Moving wall license type, use negated filter query
            if (licenseType.isMovingWall()) {
                logger.trace("License type '{}' is a moving wall", licenseType.getName());
                query.append(licenseType.getMovingWallFilterQueryPart());
                // Do not continue; with the next license type here because the user may have full access to the moving wall license,
                // in which case it should also be added without a date restriction
            }

            // Open access, license type privileges and explicit privileges
            if (licenseType.isOpenAccess()
                    || licenseType.getPrivileges().contains(privilege) || AccessConditionUtils
                            .checkAccessPermission(Collections.singletonList(licenseType),
                                    new HashSet<>(Collections.singletonList(licenseType.getName())), privilege, user, ipAddress,
                                    client, null)
                            .isGranted()) {
                logger.trace("User has listing privilege for license type '{}'.", licenseType.getName());
                query.append(licenseType.getFilterQueryPart());
                usedLicenseTypes.add(licenseType.getName());

                // If the current license type overrides other license types, add permissions for those as well
                for (LicenseType overridingLicenseType : licenseType.getOverriddenLicenseTypes()) {
                    if (!usedLicenseTypes.contains(overridingLicenseType.getName())) {
                        query.append(overridingLicenseType.getFilterQueryPart());
                        usedLicenseTypes.add(overridingLicenseType.getName());
                        logger.trace("User has additional listing privilege for license type '{}' due to '{}' overriding it.",
                                overridingLicenseType.getName(), licenseType.getName());
                    }
                }
            }

        }
        query.append(')');

        return query.toString();
    }

    /**
     *
     * @return Solr query for the moving wall date range
     */
    public static String getMovingWallQuery() {
        return SolrConstants.DATE_PUBLICRELEASEDATE + ":[* TO NOW/DATE]";
    }

    /**
     * TODO This method might be quite expensive.
     *
     * @param searchTerms a {@link java.util.Set} object.
     * @param inFulltext a {@link java.lang.String} object.
     * @param targetFragmentLength Desired (approximate) length of the text fragment.
     * @param firstMatchOnly If true, only the fragment for the first match will be returned
     * @param addFragmentIfNoMatches If true, a fragment will be added even if no term was matched
     * @param proximitySearchDistance
     * @return a {@link java.util.List} object.
     * @should not add prefix and suffix to text
     * @should truncate string to 200 chars if no terms are given
     * @should truncate string to 200 chars if no term has been found
     * @should make terms bold if found in text
     * @should remove unclosed HTML tags
     * @should return multiple match fragments correctly
     * @should replace line breaks with spaces
     * @should add fragment if no term was matched only if so requested
     * @should highlight multi word terms while removing stopwords
     */
    public static List<String> truncateFulltext(Set<String> searchTerms, final String inFulltext, int targetFragmentLength, boolean firstMatchOnly,
            boolean addFragmentIfNoMatches, int proximitySearchDistance) {
        // logger.trace("truncateFulltext"); //NOSONAR Debug
        if (inFulltext == null) {
            throw new IllegalArgumentException("fulltext may not be null");
        }
        // Remove HTML breaks
        String fulltext = Jsoup.parse(inFulltext).text();
        List<String> ret = new ArrayList<>();
        if (searchTerms != null && !searchTerms.isEmpty()) {
            for (final String term : searchTerms) {
                if (term.isEmpty()) {
                    continue;
                }
                String searchTerm = SearchHelper.removeTruncation(term);
                searchTerm = StringTools.removeQuotations(searchTerm);
                // logger.trace("term: {}", searchTerm); //NOSONAR Debug
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
                    break;
                }
                if (FuzzySearchTerm.isFuzzyTerm(searchTerm)) {
                    // Fuzzy search
                    FuzzySearchTerm fuzzySearchTerm = new FuzzySearchTerm(searchTerm);
                    Matcher m = Pattern.compile(FuzzySearchTerm.WORD_PATTERN).matcher(fulltext.toLowerCase());
                    int lastIndex = -1;
                    while (m.find()) {
                        String word = m.group();
                        if (fuzzySearchTerm.matches(word)) {
                            if (lastIndex != -1 && m.start() <= lastIndex + searchTerm.length()) {
                                continue;
                            }
                            lastIndex = createFulltextFragment(m, fulltext, word, targetFragmentLength, ret);
                            if (firstMatchOnly) {
                                break;
                            }
                        }
                    }
                } else if (proximitySearchDistance > 0 && searchTerm.contains(" ")) {
                    // Proximity search
                    String regex = buildProximitySearchRegexPattern(searchTerm, proximitySearchDistance);
                    if (regex != null) {
                        Matcher m = Pattern.compile(regex).matcher(fulltext.toLowerCase());
                        // logger.trace(fulltext.toLowerCase()); //NOSONAR Debug
                        int lastIndex = -1;
                        while (m.find()) {
                            // Skip match if it follows right after the last match
                            if (lastIndex != -1 && m.start() <= lastIndex + searchTerm.length()) {
                                continue;
                            }
                            String fragment = fulltext.substring(m.start(), m.end());
                            logger.trace("fragment: {}", fragment);
                            lastIndex = createFulltextFragment(m, fulltext, fragment, targetFragmentLength, ret);
                            if (firstMatchOnly) {
                                break;
                            }
                        }
                    }
                } else {
                    Matcher m = Pattern.compile(searchTerm.toLowerCase()).matcher(fulltext.toLowerCase());
                    int lastIndex = -1;
                    while (m.find()) {
                        // Skip match if it follows right after the last match
                        if (lastIndex != -1 && m.start() <= lastIndex + searchTerm.length()) {
                            continue;
                        }
                        lastIndex = createFulltextFragment(m, fulltext, searchTerm, targetFragmentLength, ret);
                        if (firstMatchOnly) {
                            break;
                        }
                    }
                }
            }

            // If no search term has been found (i.e. when searching for a phrase), make sure no empty string gets delivered
            if (addFragmentIfNoMatches && ret.isEmpty()) {
                String fulltextFragment;
                if (fulltext.length() > 200) {
                    fulltextFragment = fulltext.substring(0, 200);
                } else {
                    fulltextFragment = fulltext;
                }
                fulltextFragment = fulltextFragment.replace("<br>", " ");
                ret.add(fulltextFragment);
            }
        } else {
            String fulltextFragment;
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
                fulltextFragment = fulltextFragment.replace("<br>", " ");
                ret.add(fulltextFragment);
            }
        }

        return ret;
    }

    /**
     * Builds regex for proximity search full-text snippets.
     *
     * @param searchTerm Search term containing multiple words
     * @param proximitySearchDistance Maximum distance between word
     * @return Regex pattern for given term and distance
     * @should build regex correctly
     */
    static String buildProximitySearchRegexPattern(String searchTerm, int proximitySearchDistance) {
        if (StringUtils.isEmpty(searchTerm)) {
            return null;
        }
        String[] searchTermSplit = searchTerm.toLowerCase().split(" ");
        StringBuilder sbPattern = new StringBuilder("\\b(?:");
        for (int i = 0; i < searchTermSplit.length; ++i) {
            if (i > 0) {
                sbPattern.append("\\W+(?:\\p{L}+\\W+){0,").append(proximitySearchDistance).append("}?");
            }
            for (int j = 0; j < searchTermSplit[i].length(); ++j) {
                // Allow space within term (remnant of line breaks)
                if (j > 0) {
                    sbPattern.append("(| )");
                }
                sbPattern.append(searchTermSplit[i].charAt(j));

            }
        }
        sbPattern.append('|');

        // Reverser order
        for (int i = searchTermSplit.length - 1; i >= 0; --i) {
            if (i < searchTermSplit.length - 1) {
                sbPattern.append("\\W+(?:\\p{L}+\\W+){0,").append(proximitySearchDistance).append("}?");
            }
            for (int j = 0; j < searchTermSplit[i].length(); ++j) {
                if (j > 0) {
                    sbPattern.append("(| )");
                }
                sbPattern.append(searchTermSplit[i].charAt(j));

            }
        }
        sbPattern.append(")\\b");

        return sbPattern.toString();
    }

    /**
     *
     * @param m
     * @param fulltext
     * @param searchTerm
     * @param targetFragmentLength
     * @param ret
     * @return Last index of text fragment
     */
    private static int createFulltextFragment(Matcher m, String fulltext, String searchTerm, int targetFragmentLength, List<String> ret) {
        int indexOfTerm = m.start();
        int lastIndex = m.start();

        // fulltextFragment = getTextFragmentFromLine(fulltext, searchTerm, indexOfTerm, targetFragmentLength);
        String fragment = getTextFragmentRandomized(fulltext, searchTerm, indexOfTerm, targetFragmentLength);
        // fulltextFragment = getTextFragmentStatic(fulltext, targetFragmentLength, fulltextFragment, searchTerm,
        // indexOfTerm);

        indexOfTerm = fragment.toLowerCase().indexOf(searchTerm.toLowerCase());
        int indexOfSpace = fragment.indexOf(' ');
        if (indexOfTerm > indexOfSpace && indexOfSpace >= 0) {
            fragment = fragment.substring(indexOfSpace, fragment.length());
        }

        indexOfTerm = fragment.toLowerCase().indexOf(searchTerm.toLowerCase());

        if (indexOfTerm < fragment.lastIndexOf(' ')) {
            fragment = fragment.substring(0, fragment.lastIndexOf(' '));
        }

        indexOfTerm = fragment.toLowerCase().indexOf(searchTerm.toLowerCase());
        if (indexOfTerm >= 0) {
            fragment = applyHighlightingToPhrase(fragment, searchTerm);
            fragment = replaceHighlightingPlaceholders(fragment);
        }
        if (StringUtils.isNotBlank(fragment)) {
            // Check for unclosed HTML tags
            int lastIndexOfLT = fragment.lastIndexOf('<');
            int lastIndexOfGT = fragment.lastIndexOf('>');
            if (lastIndexOfLT != -1 && lastIndexOfLT > lastIndexOfGT) {
                fragment = fragment.substring(0, lastIndexOfLT).trim();
            }
            // fulltextFragment = fulltextFragment.replaceAll("[\\t\\n\\r]+", " ");
            // fulltextFragment = fulltextFragment.replace("<br>", " ");
            ret.add(fragment);
        }
        return lastIndex;
    }

    /**
     * Adds highlighting markup for all given terms to the phrase.
     *
     * @param phrase a {@link java.lang.String} object.
     * @param terms a {@link java.util.Set} object.
     * @return a {@link java.lang.String} object.
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
        for (final String t : terms) {
            // Remove quotation
            String term = StringTools.removeQuotations(t);
            //remove fuzzy search suffix
            FuzzySearchTerm fuzzyTerm = new FuzzySearchTerm(term);
            term = fuzzyTerm.getTerm();
            // Highlighting single-character terms can take a long time, so skip them
            if (term.length() < 2) { //NOSONAR Debug
                continue;
            }
            term = SearchHelper.removeTruncation(term);
            String normalizedPhrase = normalizeString(phrase);
            String normalizedTerm = normalizeString(term);
            if (contains(normalizedPhrase, normalizedTerm, fuzzyTerm.getMaxDistance())) {
                highlightedValue = SearchHelper.applyHighlightingToPhrase(highlightedValue, term);
                // logger.trace("highlighted value: {}", highlightedValue);  //NOSONAR Debug
            }
        }

        return highlightedValue;
    }

    /**
     * if maxDistance &lt;= 0, or either phrase or term is blank, simply return {@link StringUtils#contains(phrase, term)}. Otherwise check if the
     * phrase contains a word which has a Damerau-Levenshtein distance of at most maxDistance to the term
     *
     * @param phrase
     * @param term
     * @param maxDistance
     * @return true if phrase contains term within maxDistance; false otherwise
     */
    public static boolean contains(String phrase, String term, int maxDistance) {
        if (maxDistance > 0 && StringUtils.isNoneBlank(phrase, term)) {
            Matcher matcher = Pattern.compile("[\\w-]+").matcher(phrase);
            while (matcher.find()) {
                String word = matcher.group();
                if (Math.abs(word.length() - term.length()) <= maxDistance) {
                    int distance = new DamerauLevenshtein(word, term).getSimilarity();
                    if (distance <= maxDistance) {
                        return true;
                    }
                }
            }
            return false;
        }

        return Strings.CS.contains(phrase, term);
    }

    /**
     * Recursively adds highlighting markup to all occurrences of the given term in the given phrase.
     *
     * @param phrase
     * @param term
     * @return phrase with highlighting markup around term
     * @should apply highlighting to all occurrences of term
     * @should ignore special characters
     * @should skip single character terms
     * @should not add highlighting to hyperlink urls
     */
    static String applyHighlightingToPhrase(String phrase, String term) {
        if (phrase == null) {
            throw new IllegalArgumentException("phrase may not be null");
        }
        if (term == null) {
            throw new IllegalArgumentException("term may not be null");
        }

        // Highlighting single-character terms can take a long time, so skip them
        if (term.length() < 2 || phrase.length() < 2) {
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
        // logger.trace("highlighted term: {}", highlightedTerm); //NOSONAR Debug
        String after = phrase.substring(endIndex);

        return sb.append(applyHighlightingToPhrase(before, term)).append(highlightedTerm).append(applyHighlightingToPhrase(after, term)).toString();
    }

    /**
     * Remove any diacritic characters and replace any non.letter and non-digit characters with space
     *
     * @param string
     * @return Normalized string
     * @should preserve digits
     * @should preserve latin chars
     * @should preserve hebrew chars
     * @should remove hyperlink html elements including terms
     */
    static String normalizeString(final String string) {
        if (string == null) {
            return null;
        }

        String ret = string;

        // Replace entire hyperink elements with spaces
        Matcher m = PATTERN_HYPHEN_LINK.matcher(ret);
        while (m.find()) {
            StringBuilder sb = new StringBuilder();
            sb.append(ret.substring(0, m.start()));
            for (int i = m.start(); i < m.end(); ++i) {
                sb.append(' ');
            }
            sb.append(ret.substring(m.end()));
            ret = sb.toString();
        }

        ret = Normalizer.normalize(ret, Normalizer.Form.NFD);

        // string = string.replaceAll(patternHyperlink.pattern(), " ");
        ret = ret.toLowerCase().replaceAll("\\p{M}", "").replaceAll("[^\\p{L}0-9#]", " ");
        ret = Normalizer.normalize(ret, Normalizer.Form.NFC);

        return ret;
    }

    /**
     *
     * @param term
     * @return term with highlighting markup
     * @should add span correctly
     */
    static String applyHighlightingToTerm(String term) {
        return new StringBuilder(PLACEHOLDER_HIGHLIGHTING_START).append(term).append(PLACEHOLDER_HIGHLIGHTING_END).toString();
    }

    /**
     * <p>
     * replaceHighlightingPlaceholders.
     * </p>
     *
     * @param phrase a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @should replace placeholders with html tags
     */
    public static String replaceHighlightingPlaceholders(String phrase) {
        return phrase.replace(PLACEHOLDER_HIGHLIGHTING_START, "<mark class=\"search-list--highlight\">")
                .replace(PLACEHOLDER_HIGHLIGHTING_END, "</mark>");
    }

    /**
     *
     * @param phrase
     * @return phrase without highlighting placeholders
     * @should replace placeholders with empty strings
     */
    public static String removeHighlightingPlaceholders(String phrase) {
        return phrase.replace(PLACEHOLDER_HIGHLIGHTING_START, "").replace(PLACEHOLDER_HIGHLIGHTING_END, "");
    }

    /**
     * @param fulltext
     * @param targetFragmentLength
     * @param fulltextFragment
     * @param searchTerm
     * @param indexOfTerm
     * @return Text fragment
     */
    @SuppressWarnings("unused")
    private static String getTextFragmentStatic(String fulltext, int targetFragmentLength, final String fulltextFragment, String searchTerm,
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
            return fulltext.substring(start, end);
        }
        return fulltextFragment;
    }

    /**
     * @param fulltext
     * @param searchTerm
     * @param indexOfTerm
     * @param fragmentLength
     * @return Text fragment
     */
    private static String getTextFragmentRandomized(String fulltext, String searchTerm, int indexOfTerm, int fragmentLength) {

        int minDistanceFromEdge = 10;

        int fragmentStartIndexFloor = Math.max(0, indexOfTerm + minDistanceFromEdge - (fragmentLength - searchTerm.length()));
        int fragmentStartIndexCeil = Math.max(0, indexOfTerm - minDistanceFromEdge);

        int fragmentStartIndex = fragmentStartIndexFloor + RANDOM.nextInt(Math.max(1, fragmentStartIndexCeil - fragmentStartIndexFloor));
        int fragmentEndIndex = Math.min(fulltext.length(), fragmentStartIndex + fragmentLength);

        return fulltext.substring(fragmentStartIndex, fragmentEndIndex);
    }

    /**
     * @param fulltext
     * @param searchTerm
     * @param indexOfTerm
     * @param fragmentLength
     * @return Text fragment
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
        // logger.trace("getFacetValues: {} / {}", query, facetFieldName);
        return getFacetValues(query, facetifyField(facetFieldName), null, facetMinCount, null);
    }

    /**
     * Returns a list of values for a given facet field and the given query.
     *
     * @param query a {@link java.lang.String} object.
     * @param facetFieldName a {@link java.lang.String} object.
     * @param facetPrefix The facet field value must start with these characters. Ignored if null or blank
     * @param facetMinCount a int.
     * @param params
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should return correct values via json response
     */
    public static List<String> getFacetValues(String query, final String facetFieldName, String facetPrefix, int facetMinCount,
            Map<String, String> params) throws PresentationException, IndexUnreachableException {
        // logger.trace("getFacetValues: {}", query); //NOSONAR Debug
        if (StringUtils.isEmpty(query)) {
            throw new IllegalArgumentException("query may not be null or empty");
        }
        if (StringUtils.isEmpty(facetFieldName)) {
            throw new IllegalArgumentException("facetFieldName may not be null or empty");
        }

        String useFacetFieldName = facetFieldName;
        boolean json = false;
        List<String> facetFieldNames = new ArrayList<>(1);
        if (useFacetFieldName.startsWith("json:")) {
            json = true;
            useFacetFieldName = useFacetFieldName.substring(5);
        } else {
            facetFieldNames.add(useFacetFieldName);
        }
        try {
            QueryResponse resp = DataManager.getInstance()
                    .getSearchIndex()
                    .searchFacetsAndStatistics(query, null, facetFieldNames, facetMinCount, facetPrefix, params, false);
            FacetField facetField = resp.getFacetField(useFacetFieldName);
            if (json && resp.getJsonFacetingResponse() != null && resp.getJsonFacetingResponse().getStatValue(useFacetFieldName) != null) {
                return Collections.singletonList(String.valueOf(resp.getJsonFacetingResponse().getStatValue(useFacetFieldName)));
            }

            if (facetField == null) {
                return Collections.emptyList();
            }

            List<String> ret = new ArrayList<>(facetField.getValueCount());
            for (Count count : facetField.getValues()) {
                if (StringUtils.isNotEmpty(count.getName()) && count.getCount() >= facetMinCount) {
                    if (count.getName().startsWith("\\u0001")) {
                        continue;
                    }
                    ret.add(count.getName());
                }
            }
            return ret;
        } catch (SolrException e) {
            throw new IndexUnreachableException("Error communication with solr: " + e.toString());
        }
    }

    /**
     *
     * @param bmfc
     * @param startsWith
     * @param filterQuery
     * @param language
     * @return Number of found terms
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static int getFilteredTermsCount(BrowsingMenuFieldConfig bmfc, String startsWith, String filterQuery, String language)
            throws PresentationException, IndexUnreachableException {
        if (bmfc == null) {
            throw new IllegalArgumentException("bmfc may not be null");
        }

        String mainField = bmfc.getFieldForLanguage(language);
        if (logger.isTraceEnabled()) {
            logger.trace("getFilteredTermsCount: {} ({})", mainField, startsWith);
        }
        List<StringPair> sortFields =
                StringUtils.isEmpty(bmfc.getSortField()) ? null : Collections.singletonList(new StringPair(bmfc.getSortField(), "asc"));
        QueryResponse resp = getFilteredTermsFromIndex(bmfc, startsWith, filterQuery, sortFields, 0, 0, language);
        logger.trace("getFilteredTermsCount hits: {}", resp.getResults().getNumFound());

        if (bmfc.getField() == null) {
            return 0;
        }

        int ret = 0;
        String facetField =
                StringUtils.isNotEmpty(bmfc.getSortField()) ? SearchHelper.facetifyField(bmfc.getSortField())
                        : SearchHelper.facetifyField(mainField);
        if (resp.getFacetField(facetField) != null) {
            for (Count count : resp.getFacetField(facetField).getValues()) {
                if (count.getCount() == 0
                        || (StringUtils.isNotEmpty(startsWith) && !Strings.CI.startsWith(count.getName(), startsWith.toLowerCase()))) {
                    continue;
                }
                ret++;
            }
        }
        logger.debug("getFilteredTermsCount result: {}", ret);
        return ret;
    }

    /**
     * Generates starting character filters for term browsing, using facets.
     * 
     * @param bmfc
     * @param filterQuery
     * @param locale
     * @return List<String>
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public static List<String> collectAvailableTermFilters(BrowsingMenuFieldConfig bmfc, String filterQuery, Locale locale)
            throws PresentationException, IndexUnreachableException {
        StringBuilder sbQuery = new StringBuilder("+");
        String facetMainField = SearchHelper.facetifyField(bmfc.getFieldForLanguage(locale.getLanguage()));
        if (StringUtils.isNotEmpty(bmfc.getSortField())) {
            // TODO language-specific sort field
            sbQuery.append(bmfc.getSortField());
        } else {
            sbQuery.append(bmfc.getFieldForLanguage(locale.getLanguage()));
        }
        sbQuery.append(":[* TO *] ");
        if (bmfc.isRecordsAndAnchorsOnly()) {
            sbQuery.append(ALL_RECORDS_QUERY);
        }

        // Add passed filter queries
        List<String> filterQueries = new ArrayList<>();
        if (StringUtils.isNotEmpty(filterQuery)) {
            filterQueries.add(filterQuery);
        }

        // Add configured filter queries
        if (!bmfc.getFilterQueries().isEmpty()) {
            filterQueries.addAll(bmfc.getFilterQueries());
        }

        String query = buildFinalQuery(sbQuery.toString(), false, SearchAggregationType.NO_AGGREGATION);
        String facetSortField = null;
        if (StringUtils.isNotEmpty(bmfc.getSortField())) {
            facetSortField = SearchHelper.facetifyField(bmfc.getSortField());
        }
        List<String> facetFields = Collections.singletonList(facetMainField);
        if (StringUtils.isNotEmpty(facetSortField)) {
            // Add facetified sort field to facet fields (SORT_ fields don't work for faceting)
            facetFields = new ArrayList<>(facetFields);
            facetFields.add(facetSortField);
        }

        QueryResponse qr = DataManager.getInstance()
                .getSearchIndex()
                .searchFacetsAndStatistics(query, filterQueries, facetFields, 1, null, null, false);

        String useField = null;
        if (qr.getFacetField(facetSortField) != null) {
            // Prefer facets from sort field
            useField = facetSortField;
        } else if (qr.getFacetField(facetMainField) != null) {
            // main field fallback
            useField = facetMainField;
        }

        Set<String> alreadyAdded = new HashSet<>();
        List<String> ret = new ArrayList<>();
        if (useField != null) {
            boolean ignoreLeadingChars =
                    StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars());
            for (Count count : qr.getFacetField(useField).getValues()) {
                String firstChar;
                if (ignoreLeadingChars) {
                    // Exclude leading characters from filters explicitly configured to be ignored
                    firstChar = BrowseTermComparator.normalizeString(count.getName(),
                            DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars())
                            .trim()
                            .substring(0, 1)
                            .toUpperCase();
                } else {
                    firstChar = count.getName().substring(0, 1).toUpperCase();
                }
                if (!alreadyAdded.contains(firstChar)) {
                    ret.add(firstChar);
                    alreadyAdded.add(firstChar);
                }
            }
            Collections.sort(ret, new AlphanumCollatorComparator(Collator.getInstance(locale)));
        }

        return ret;
    }

    /**
     * Returns a list of index terms for the given field name. This method uses the slower doc search instead of term search, but can be filtered with
     * a query.
     *
     * @param bmfc a {@link io.goobi.viewer.model.termbrowsing.BrowsingMenuFieldConfig} object.
     * @param startsWith a {@link java.lang.String} object.
     * @param filterQuery a {@link java.lang.String} object.
     * @param start
     * @param rows
     * @param comparator a {@link java.util.Comparator} object.
     * @param language Language for language-specific fields
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @should be thread safe when counting terms
     */
    public static List<BrowseTerm> getFilteredTerms(BrowsingMenuFieldConfig bmfc, String startsWith, String filterQuery, int start, final int rows,
            Comparator<BrowseTerm> comparator, String language) throws PresentationException, IndexUnreachableException {
        if (bmfc == null) {
            throw new IllegalArgumentException("bmfc may not be null");
        }

        String mainField = bmfc.getFieldForLanguage(language);
        if (logger.isTraceEnabled()) {
            logger.trace("getFilteredTerms: {} ({})", mainField, startsWith);
        }
        List<BrowseTerm> ret = new ArrayList<>();
        ConcurrentMap<String, BrowseTerm> terms = new ConcurrentHashMap<>();

        // If only browsing top level documents, use faceting for faster performance
        int returnRows = rows;
        if (bmfc.isRecordsAndAnchorsOnly()) {
            returnRows = 0;
        }

        List<StringPair> sortFields =
                StringUtils.isEmpty(bmfc.getSortField()) ? null : Collections.singletonList(new StringPair(bmfc.getSortField(), "asc"));
        QueryResponse resp = getFilteredTermsFromIndex(bmfc, startsWith, filterQuery, sortFields, start, returnRows, language);
        logger.debug("getFilteredTerms hits: {}", resp.getResults().getNumFound());
        String facetMainField = SearchHelper.facetifyField(mainField);
        String facetSortField = null;
        if (StringUtils.isNotEmpty(bmfc.getSortField())) {
            facetSortField = SearchHelper.facetifyField(bmfc.getSortField());
        }
        if (resp.getResults().isEmpty()) {
            // If only browsing records and anchors, use faceting
            String useField = null;
            if (resp.getFacetField(facetSortField) != null) {
                // Prefer facets from sort field
                useField = facetSortField;
            } else if (resp.getFacetField(facetMainField) != null) {
                // main field fallback
                useField = facetMainField;
            }
            if (useField != null) {
                for (Count count : resp.getFacetField(useField).getValues()) {
                    if (StringUtils.isNotEmpty(startsWith) && !"-".equals(startsWith)
                            && !Strings.CI.startsWith(count.getName(), startsWith)) {
                        // logger.trace("Skipping term: {}, compareTerm: {}, sortTerm: {}, translate: {}", //NOSONAR Debug
                        // term, compareTerm, sortTerm, bmfc.isTranslate());
                        continue;
                    }
                    terms.put(count.getName(),
                            new BrowseTerm(count.getName(), null,
                                    bmfc.isTranslate() ? ViewerResourceBundle.getTranslations(count.getName()) : null)
                                            .setHitCount(count.getCount()));
                }
            }
        } else {
            // Without filtering or using alphabetical filtering
            // Parallel processing of hits (if sorting field is provided), requires compiler level 1.8
            //                ((List<SolrDocument>) resp.getResults()).parallelStream()
            //                        .forEach(doc -> processSolrResult(doc, bmfc, startsWith, terms, true, language));

            // Sequential processing (doesn't break the sorting done by Solr)
            for (SolrDocument doc : resp.getResults()) {
                processSolrResult(doc, bmfc, startsWith, terms, true, language);
            }
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
     * @param language
     * @return {@link QueryResponse}
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should contain facets for the main field
     * @should contain facets for the sort field
     */
    static QueryResponse getFilteredTermsFromIndex(BrowsingMenuFieldConfig bmfc, String startsWith, String filterQuery, List<StringPair> sortFields,
            int start, int rows, String language) throws PresentationException, IndexUnreachableException {
        String mainField = bmfc.getFieldForLanguage(language);
        if (logger.isTraceEnabled()) {
            logger.trace("getFilteredTermsFromIndex: {} ({})", mainField, startsWith);
        }

        List<String> fields = new ArrayList<>(3);
        fields.add(SolrConstants.PI_TOPSTRUCT);
        fields.add(mainField);

        StringBuilder sbQuery = new StringBuilder();
        sbQuery.append('+');
        // TODO language-specific sort field
        if (StringUtils.isNotEmpty(bmfc.getSortField())) {
            sbQuery.append(bmfc.getSortField());
            fields.add(bmfc.getSortField());
            fields.add(SearchHelper.facetifyField(bmfc.getSortField()));
            fields.add(SearchHelper.facetifyField(mainField));
        } else {
            sbQuery.append(mainField);
        }
        sbQuery.append(":[* TO *] ");
        if (bmfc.isRecordsAndAnchorsOnly()) {
            sbQuery.append(ALL_RECORDS_QUERY);
        }

        List<String> filterQueries = new ArrayList<>();
        if (StringUtils.isNotEmpty(filterQuery)) {
            filterQueries.add(filterQuery);
        }

        // If no separate sorting field is configured, add startsWith to the filter queries
        if (StringUtils.isEmpty(bmfc.getSortField()) && StringUtils.isNotEmpty(startsWith)) {
            filterQueries.add(mainField + ":" + startsWith + "*");
        }

        // Add configured filter queries
        if (!bmfc.getFilterQueries().isEmpty()) {
            filterQueries.addAll(bmfc.getFilterQueries());
        }

        // logger.trace("getFilteredTermsFromIndex startsWith: {}", startsWith); //NOSONAR Debug
        String query = buildFinalQuery(sbQuery.toString(), false, SearchAggregationType.NO_AGGREGATION);
        logger.trace("getFilteredTermsFromIndex query: {}", query);
        if (logger.isTraceEnabled()) {
            for (String fq : filterQueries) {
                logger.trace("getFilteredTermsFromIndex filter query: {}", fq);
            }
        }

        List<String> facetFields = Collections.singletonList(SearchHelper.facetifyField(mainField));
        if (StringUtils.isNotEmpty(bmfc.getSortField())) {
            // Add facetified sort field to facet fields (SORT_ fields don't work for faceting)
            facetFields = new ArrayList<>(facetFields);
            facetFields.add(SearchHelper.facetifyField(bmfc.getSortField()));
        }
        Map<String, String> params = new HashMap<>();
        long hitCount = DataManager.getInstance().getSearchIndex().getHitCount(query, filterQueries);
        logger.trace("row count: {}", hitCount);

        // Faceting (no rows requested or expected row count too high)
        if ((rows == 0 || hitCount > DataManager.getInstance().getConfiguration().getBrowsingMenuIndexSizeThreshold())
                && StringUtils.isEmpty(bmfc.getSortField())) {
            logger.trace("Using facets");
            // params.put("facet.sort", "index");
            // params.put("facet.offset", String.valueOf(start));
            // if (rows > 0) {
            //     params.put("facet.limit", String.valueOf(rows));
            // }
            if (StringUtils.isNotEmpty(startsWith)) {
                params.put("facet.prefix", startsWith);
            }
            return DataManager.getInstance()
                    .getSearchIndex()
                    .searchFacetsAndStatistics(query, filterQueries, facetFields, 1, startsWith, params, false);
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
     * @param language
     */
    private static void processSolrResult(SolrDocument doc, BrowsingMenuFieldConfig bmfc, String startsWith,
            ConcurrentMap<String, BrowseTerm> terms, boolean aggregateHits, String language) {
        // logger.trace("processSolrResult thread {}", Thread.currentThread().getId()); //NOSONAR Debug
        List<String> termList = SolrTools.getMetadataValues(doc, bmfc.getFieldForLanguage(language));
        if (termList.isEmpty()) {
            return;
        }

        String pi = (String) doc.getFieldValue(SolrConstants.PI_TOPSTRUCT);
        Set<String> usedTermsInCurrentDoc = new HashSet<>();
        int count = -1;
        //        Collections.sort(termList);
        for (String term : termList) {
            count++;
            if (StringUtils.isEmpty(term)) {
                continue;
            }

            // Only add to hit count if the same string is not in the same doc
            if (usedTermsInCurrentDoc.contains(term)) {
                continue;
            }

            // Sort field value should only represent the first term instance
            String sortTerm = count == 0 ? SolrTools.getSingleFieldStringValue(doc, bmfc.getSortField()) : null;
            String compareTerm = term;
            if (StringUtils.isNotEmpty(sortTerm)) {
                compareTerm = sortTerm;
            } else if (bmfc.getSortField() != null) {
                // Only the first term will have a matching sort field, so attempt a fallback to a facetified variant
                List<String> facetifiedSortTermValues = SolrTools.getMetadataValues(doc, SearchHelper.facetifyField(bmfc.getSortField()));
                String bestMatch = null;
                // Look for exact match
                for (String val : facetifiedSortTermValues) {
                    if (Strings.CI.equals(val, term)) {
                        bestMatch = val;
                        break;
                    }
                }
                // Look for most similar value
                if (StringUtils.isEmpty(bestMatch)) {
                    bestMatch = StringTools.findBestMatch(term, facetifiedSortTermValues, language);
                }
                if (StringUtils.isNotEmpty(bestMatch)) {
                    compareTerm = bestMatch;
                    sortTerm = bestMatch;
                }
            }
            if (StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars())) {
                // Exclude leading characters from filters explicitly configured to be ignored
                compareTerm = BrowseTermComparator.normalizeString(compareTerm,
                        DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars()).trim();
            }
            if (StringUtils.isNotEmpty(startsWith) && !"-".equals(startsWith) && !Strings.CI.startsWith(compareTerm, startsWith)) {
                // logger.trace("Skipping term: {}, compareTerm: {}, sortTerm: {}, translate: {}", //NOSONAR Debug
                // term, compareTerm, sortTerm, bmfc.isTranslate()); //NOSONAR Debug
                continue;
            }

            BrowseTerm browseTerm = terms.get(term);
            if (browseTerm == null) {
                synchronized (LOCK) {
                    // Another thread may have added this term by now
                    if (!terms.containsKey(term)) {
                        // logger.trace("Adding term: {}, compareTerm: {}, sortTerm: {}, translate: {}", //NOSONAR Debug
                        // term, compareTerm, sortTerm, bmfc.isTranslate()); //NOSONAR Debug
                        terms.put(term, new BrowseTerm(term, sortTerm, bmfc.isTranslate() ? ViewerResourceBundle.getTranslations(term) : null));
                    }
                }
                browseTerm = terms.get(term);
            }

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
     * @return a {@link java.util.Map} object.
     * @should extract all values from query except from NOT blocks
     * @should handle multiple phrases in query correctly
     * @should skip discriminator value
     * @should not remove truncation
     * @should throw IllegalArgumentException if query is null
     * @should add title terms field
     * @should remove proximity search tokens
     * @should remove fuzzy search tokens
     * @should remove range values
     * @should remove operators from field names
     */
    public static Map<String, Set<String>> extractSearchTermsFromQuery(final String query, String discriminatorValue) {
        logger.trace("extractSearchTermsFromQuery:{}", query);
        if (query == null) {
            throw new IllegalArgumentException("query may not be null");
        }

        String q = query;
        Set<String> stopwords = DataManager.getInstance().getConfiguration().getStopwords();
        // Do not extract a currently set discriminator value
        if (StringUtils.isNotEmpty(discriminatorValue)) {
            stopwords.add(discriminatorValue);
        }
        // Remove all NOT(*) parts
        Matcher mNot = PATTERN_NOT_BRACKETS.matcher(q);
        while (mNot.find()) {
            q = q.replace(q.substring(mNot.start(), mNot.end()), "");
        }
        mNot = PATTERN_NOT.matcher(q);
        while (mNot.find()) {
            q = q.replace(q.substring(mNot.start(), mNot.end()), "");
        }

        // Remove parentheses, ANDs and ORs
        q = q.replace("(", "@").replace(")", "@").replace(SolrConstants.SOLR_QUERY_AND, " ").replace(SolrConstants.SOLR_QUERY_OR, " ");

        Map<String, Set<String>> ret = new HashMap<>();
        ret.put(TITLE_TERMS, new HashSet<>());

        // Drop proximity search tokens
        q = q.replaceAll(PATTERN_PROXIMITY_SEARCH_TOKEN.pattern(), "");

        // Drop year ranges
        q = q.replaceAll(PATTERN_YEAR_RANGE.pattern(), "");

        // Use a copy of the query because the original query gets shortened after every match, causing an IOOBE eventually
        String queryCopy = q;

        // Extract phrases and add them directly
        logger.trace("checking for phrases in: {}", queryCopy);
        Matcher mPhrases = PATTERN_FIELD_PHRASE.matcher(queryCopy);
        while (mPhrases.find()) {
            String phrase = queryCopy.substring(mPhrases.start(), mPhrases.end());
            String[] phraseSplit = phrase.split(":", 2);
            String field = phraseSplit[0];
            switch (field) {
                case SolrConstants.SUPERDEFAULT:
                    field = SolrConstants.DEFAULT;
                    break;
                case SolrConstants.SUPERFULLTEXT:
                    field = SolrConstants.FULLTEXT;
                    break;
                case SolrConstants.SUPERUGCTERMS:
                    field = SolrConstants.UGCTERMS;
                    break;
                case SolrConstants.SUPERSEARCHTERMS_ARCHIVE:
                    field = SolrConstants.SEARCHTERMS_ARCHIVE;
                    break;
                default:
                    if (field.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
                        field = field.substring(0, field.length() - SolrConstants.SUFFIX_UNTOKENIZED.length());
                    }
                    break;
            }

            String phraseWithoutQuotation = phraseSplit[1].replace("@", "").replace("\"", "");
            if (!phraseWithoutQuotation.isEmpty() && !stopwords.contains(phraseWithoutQuotation)) {
                if (ret.get(field) == null) {
                    ret.put(field, new HashSet<>());
                }
                // logger.trace("term: {}:{}", field, phraseWithoutQuotation); //NOSONAR Debug
                // TODO Check why quotes were removed here, they're needed for the expand query
                ret.get(field).add("\"" + phraseWithoutQuotation + "\"");
            }
            q = q.replace(phrase, "");
            ret.get(TITLE_TERMS).add("\"" + phraseWithoutQuotation + "\"");
        }

        // Split into FIELD:value pairs
        String[] querySplit = q.split(SEARCH_QUERY_SPLIT_REGEX);
        String currentField = null;
        for (final String queryPart : querySplit) {
            String s = queryPart.replace("@", " ").trim();
            // logger.error("term: {}", s); //NOSONAR Debug
            // Extract the value part
            if (s.contains(":") && !s.startsWith(":")) {
                int split = s.indexOf(':');
                String field = s.substring(0, split);
                String value = s.length() > split ? s.substring(split + 1) : null;
                if (StringUtils.isNotBlank(value)) {
                    if (value.trim().equals("+")) {
                        continue;
                    }
                    currentField = field;
                    value = value.trim();

                    // Remove operators before field name
                    if (currentField.charAt(0) == '+' || currentField.charAt(0) == '-') {
                        currentField = currentField.substring(1).trim();
                    }

                    switch (currentField) {
                        case SolrConstants.SUPERDEFAULT:
                            currentField = SolrConstants.DEFAULT;
                            break;
                        case SolrConstants.SUPERFULLTEXT:
                            currentField = SolrConstants.FULLTEXT;
                            break;
                        case SolrConstants.SUPERUGCTERMS:
                            currentField = SolrConstants.UGCTERMS;
                            break;
                        case SolrConstants.SUPERSEARCHTERMS_ARCHIVE:
                            currentField = SolrConstants.SEARCHTERMS_ARCHIVE;
                            break;
                        default:
                            break;
                    }
                    if (currentField.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
                        currentField = currentField.substring(0, currentField.length() - SolrConstants.SUFFIX_UNTOKENIZED.length());
                    }
                    // Remove quotation marks from phrases
                    if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                        value = value.replace("\"", "");
                    }
                    // Skip values in stopwords and duplicates for fuzzy search
                    if (!value.isEmpty() && !stopwords.contains(value) && !value.matches(".*~[1-2]$")) {
                        if (ret.get(currentField) == null) {
                            ret.put(currentField, new HashSet<>());
                        }
                        ret.get(currentField).add(value);
                        switch (currentField) {
                            // Do not add values to title terms for certain fields (expand as necessary)
                            case SolrConstants.ACCESSCONDITION:
                            case SolrConstants.DC:
                            case SolrConstants.DOCSTRCT:
                            case SolrConstants.DOCTYPE:
                            case SolrConstants.IDDOC:
                                break;
                            default:
                                if (!"true".equals(value.trim())) {
                                    ret.get(TITLE_TERMS).add("(" + value + ")");
                                }
                                break;
                        }
                    }
                }
            } else if (s.length() > 0 && !stopwords.contains(s)) {
                // single values w/o a field

                // Skip duplicates for fuzzy search
                if (s.trim().equals("+") || s.matches(".*~[1-2]$")) {
                    continue;
                }
                if (currentField == null) {
                    currentField = SolrConstants.DEFAULT;
                } else if (currentField.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
                    currentField = currentField.substring(0, currentField.length() - SolrConstants.SUFFIX_UNTOKENIZED.length());
                }
                if (ret.get(currentField) == null) {
                    ret.put(currentField, new HashSet<>());
                }
                ret.get(currentField).add(s);
                ret.get(TITLE_TERMS).add("(" + s + ")");
            }
        }

        return ret;
    }

    /**
     *
     * @param query
     * @param facetString
     * @param template Advanced search fields template
     * @param language
     * @return Parsed {@link SearchQueryGroup}
     * @should parse phrase search query correctly
     * @should parse regular search query correctly
     * @should parse drop down items correctly
     * @should parse range items correctly
     * @should parse items from facet string correctly
     * @should parse mixed search query correctly
     */
    public static SearchQueryGroup parseSearchQueryGroupFromQuery(final String query, String facetString, String template, String language) {
        logger.trace("parseSearchQueryGroupFromQuery: {}", query);
        SearchQueryGroup ret =
                new SearchQueryGroup(DataManager.getInstance().getConfiguration().getAdvancedSearchFields(template, true, language), template);

        List<List<StringPair>> allPairs = new ArrayList<>();
        List<Set<String>> allFieldNames = new ArrayList<>();
        List<SearchItemOperator> operators = new ArrayList<>();
        String q = query;

        // Remove outer parentheses
        if (q.startsWith("((") && q.endsWith("))")) {
            q = q.substring(1, q.length() - 1);
        }

        String queryRemainder = q;
        Matcher mAllItems = PATTERN_ALL_ITEMS.matcher(q);
        while (mAllItems.find()) {
            String itemQuery = mAllItems.group();
            logger.trace("item query: {}", itemQuery);
            queryRemainder = queryRemainder.replace(itemQuery, "");

            Matcher mPhraseItem = PATTERN_PHRASE_ITEMS.matcher(itemQuery);

            Matcher mRegularItem = PATTERN_REGULAR_ITEMS.matcher(itemQuery);

            Matcher mRangeItem = PATTERN_RANGE_ITEMS.matcher(itemQuery);

            if (mPhraseItem.find()) {
                // Phrase search
                logger.trace("phrase item: {}", itemQuery);
                String op = mPhraseItem.group(1);
                SearchItemOperator operator = SearchItemOperator.OR;
                if (StringUtils.isNotEmpty(op)) {
                    switch (op) {
                        case "+":
                            operator = SearchItemOperator.AND;
                            break;
                        case "-":
                            operator = SearchItemOperator.NOT;
                            break;
                        default:
                            break;
                    }
                }

                Matcher mPairs = PATTERN_PHRASE_PAIRS.matcher(itemQuery);
                Set<String> fieldNames = new HashSet<>();
                List<StringPair> pairs = new ArrayList<>();
                while (mPairs.find()) {
                    String pair = mPairs.group(1);
                    logger.trace("pair: {}", pair);
                    String[] pairSplit = pair.split(":");
                    if (pairSplit.length == 2) {
                        pairs.add(new StringPair(pairSplit[0], pairSplit[1].replace("\"", "").trim()));
                        fieldNames.add(pairSplit[0]);
                    }
                }
                if (!pairs.isEmpty()) {
                    allPairs.add(pairs);
                    allFieldNames.add(fieldNames);
                    operators.add(operator);
                }
            } else if (mRangeItem.find()) {
                // Range search
                logger.trace("range item: {}", itemQuery);
                String op = mRangeItem.group(1);
                SearchItemOperator operator = SearchItemOperator.OR;
                if (StringUtils.isNotEmpty(op)) {
                    switch (op) {
                        case "+":
                            operator = SearchItemOperator.AND;
                            break;
                        case "-":
                            operator = SearchItemOperator.NOT;
                            break;
                        default:
                            break;
                    }
                }

                Matcher mPairs = PATTERN_RANGE_PAIRS.matcher(itemQuery);

                Set<String> fieldNames = new HashSet<>();
                List<StringPair> pairs = new ArrayList<>();
                while (mPairs.find()) {
                    String pair = mPairs.group(1);
                    logger.trace("pair: {}", pair);
                    String[] pairSplit = pair.split(":");
                    if (pairSplit.length == 2) {
                        pairs.add(new StringPair(pairSplit[0],
                                pairSplit[1].substring(2, pairSplit[1].length() - 2).trim()));
                        fieldNames.add(pairSplit[0]);
                    }
                }
                if (!pairs.isEmpty()) {
                    allPairs.add(pairs);
                    allFieldNames.add(fieldNames);
                    operators.add(operator);
                }
            } else if (mRegularItem.find()) {
                // Regular search
                logger.trace("regular item: {}", itemQuery);
                String op = mRegularItem.group(1);
                SearchItemOperator operator = SearchItemOperator.OR;
                if (StringUtils.isNotEmpty(op)) {
                    switch (op) {
                        case "+":
                            operator = SearchItemOperator.AND;
                            break;
                        case "-":
                            operator = SearchItemOperator.NOT;
                            break;
                        default:
                            break;
                    }
                }

                Matcher mPairs = PATTERN_REGULAR_PAIRS.matcher(itemQuery);

                Set<String> fieldNames = new HashSet<>();
                List<StringPair> pairs = new ArrayList<>();
                while (mPairs.find()) {
                    String pair = mPairs.group(1);
                    logger.trace("pair: {}", pair);
                    String[] pairSplit = pair.split(":");
                    if (pairSplit.length == 2) {
                        pairs.add(new StringPair(pairSplit[0],
                                pairSplit[1]
                                        .replace("(", "")
                                        .replace(")", "")
                                        .replace(" OR", "")
                                        .replace(" AND", "")
                                        .trim()));
                        fieldNames.add(pairSplit[0]);
                    }
                }
                if (!pairs.isEmpty()) {
                    allPairs.add(pairs);
                    allFieldNames.add(fieldNames);
                    operators.add(operator);
                }
            }
        }

        // Parse facet string
        if (StringUtils.isNotEmpty(facetString)) {

            Matcher mFacetString = PATTERN_FACET_STRING.matcher(facetString);

            Set<String> fieldNames = new HashSet<>();
            while (mFacetString.find()) {
                String pair = mFacetString.group(1);
                logger.trace("pair: {}", pair);
                String[] pairSplit = pair.split(":");
                if (pairSplit.length == 2) {
                    fieldNames.add(pairSplit[0]);
                    allPairs.add(Collections.singletonList(new StringPair(pairSplit[0],
                            pairSplit[1].replace("(", "").replace(")", "").trim())));
                    allFieldNames.add(fieldNames);
                    operators.add(SearchItemOperator.AND);
                }
            }
        }

        // Add/reassign query items out of collected fields
        for (int i = 0; i < allPairs.size(); ++i) {
            List<StringPair> pairs = allPairs.get(i);
            Set<String> fieldNames = allFieldNames.get(i);
            SearchItemOperator operator = operators.get(i);
            SearchQueryItem item;
            if (ret.getQueryItems().size() > i) {
                // Re-use existing all-fields item, if available
                item = ret.getQueryItems().get(i);
            } else {
                item = new SearchQueryItem();
                ret.getQueryItems().add(item);
            }
            if (fieldNames.contains(SolrConstants.DEFAULT) && fieldNames.contains(SolrConstants.FULLTEXT)
                    && fieldNames.contains(SolrConstants.NORMDATATERMS) && fieldNames.contains(SolrConstants.UGCTERMS)
                    && fieldNames.contains(SolrConstants.SEARCHTERMS_ARCHIVE) && fieldNames.contains(SolrConstants.CMS_TEXT_ALL)) {
                // All fields
                item.setOperator(operator);
                item.setLabel(SearchHelper.SEARCH_FILTER_ALL.getLabel());
                item.setField(SearchHelper.SEARCH_FILTER_ALL.getField());
                item.setValue(pairs.get(0).getTwo());
                logger.trace("added item: {}:{}", SearchHelper.SEARCH_FILTER_ALL.getLabel(), pairs.get(0).getTwo());
            } else {
                for (StringPair pair : pairs) {
                    switch (pair.getOne()) {
                        case SolrConstants.SUPERDEFAULT:
                        case SolrConstants.SUPERFULLTEXT:
                        case SolrConstants.SUPERUGCTERMS:
                        case SolrConstants.SUPERSEARCHTERMS_ARCHIVE:
                            break;
                        default:
                            item.setOperator(operator);
                            item.setField(pair.getOne());
                            if (DataManager.getInstance().getConfiguration().isAdvancedSearchFieldRange(pair.getOne(), template, true)) {
                                String[] valueSplit = pair.getTwo().split(" TO ");
                                item.setValue(valueSplit[0]);
                                item.setValue2(valueSplit[1]);
                            } else {
                                item.setValue(pair.getTwo());
                            }
                            logger.trace("added item: {}", pair);
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Remove '*' at the start or end of the given value
     *
     * @param value
     * @return value without truncation
     */
    public static String removeTruncation(final String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }

        String ret = value;

        // Remove left truncation
        if (ret.charAt(0) == '*' && ret.length() > 1) {
            ret = ret.substring(1);
        }
        // Remove right truncation
        if (ret.charAt(ret.length() - 1) == '*' && ret.length() > 1) {
            ret = ret.substring(0, ret.length() - 1);
        }

        return ret;
    }

    /**
     * <p>
     * generateQueryParams.
     * </p>
     *
     * @param termQuery
     * @return a {@link java.util.Map} object.
     */
    public static Map<String, String> generateQueryParams(String termQuery) {
        Map<String, String> params = new HashMap<>();
        // Add a boost query to promote anchors and works to the top of the list (Extended DisMax query parser is required for this)
        params.put("defType", "edismax");
        params.put("uf", "* _query_");
        String bq = StringUtils.isNotEmpty(termQuery) ? BOOSTING_QUERY_TEMPLATE.replace("{0}", ClientUtils.escapeQueryChars(termQuery)) : null;
        if (bq != null) {
            params.put("bq", bq);
            logger.trace("bq: {}", bq);
        }

        return params;
    }

    /**
     * <p>
     * facetifyList.
     * </p>
     *
     * @param sourceList a {@link java.util.List} object.
     * @return a {@link java.util.List} object. * @should facetify correctly
     */
    public static List<String> facetifyList(List<String> sourceList) {
        if (sourceList == null) {
            return Collections.emptyList();
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
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldNamemae not be null");
        }

        if (fieldName.startsWith(SolrConstants.PREFIX_BOOL) || fieldName.startsWith(SolrConstants.PREFIX_MDNUM)
                || fieldName.equals(SolrConstants.CALENDAR_YEAR) || fieldName.equals(SolrConstants.CALENDAR_MONTH)
                || fieldName.equals(SolrConstants.CALENDAR_DAY)) {
            return fieldName;
        }
        return adaptField(fieldName, SolrConstants.PREFIX_FACET);
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
        return adaptField(fieldName, SolrConstants.PREFIX_SORT);
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
        return adaptField(fieldName, SolrConstants.PREFIX_BOOL);
    }

    /**
     *
     * @param fieldName
     * @return Normalized fieldName
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
    static String adaptField(final String fieldName, final String prefix) {
        if (fieldName == null) {
            return null;
        }

        String usePrefix = prefix;
        if (usePrefix == null) {
            usePrefix = "";
        }

        switch (fieldName) {
            case SolrConstants.DC:
            case SolrConstants.DOCSTRCT:
            case SolrConstants.DOCSTRCT_SUB:
            case SolrConstants.DOCSTRCT_TOP:
                return usePrefix + fieldName;
            case SolrConstants.CALENDAR_YEAR:
            case SolrConstants.CALENDAR_MONTH:
            case SolrConstants.CALENDAR_DAY:
                if (SolrConstants.PREFIX_SORT.equals(usePrefix)) {
                    return "SORTNUM_" + fieldName;
                }
                String ret = applyPrefix(fieldName, usePrefix);
                ret = ret.replace(SolrConstants.SUFFIX_UNTOKENIZED, "");
                return ret;
            default:
                ret = applyPrefix(fieldName, usePrefix);
                ret = ret.replace(SolrConstants.SUFFIX_UNTOKENIZED, "");
                return ret;
        }
    }

    /**
     *
     * @param fieldName
     * @param prefix
     * @return fieldName with prefix
     * @should apply prefix correctly
     */
    static String applyPrefix(final String fieldName, String prefix) {
        if (StringUtils.isEmpty(fieldName)) {
            return fieldName;
        }
        if (StringUtils.isEmpty(prefix)) {
            return fieldName;
        }

        String ret = fieldName;
        if (ret.startsWith("MD_")) {
            ret = ret.replace("MD_", prefix);
        } else if (ret.startsWith("MD2_")) {
            if (SolrConstants.PREFIX_FACET.equals(prefix)) {
                ret = SolrConstants.PREFIX_FACET + ret;
            } else {
                ret = ret.replace("MD2_", prefix);
            }
        } else if (ret.startsWith(SolrConstants.PREFIX_MDNUM)) {
            if (SolrConstants.PREFIX_SORT.equals(prefix)) {
                ret = ret.replace(SolrConstants.PREFIX_MDNUM, "SORTNUM_");
            } else if (SolrConstants.PREFIX_FACET.equals(prefix)) {
                ret = SolrConstants.PREFIX_FACET + ret;
            } else {
                ret = ret.replace(SolrConstants.PREFIX_MDNUM, prefix);
            }
        } else if (ret.startsWith("NE_")) {
            if (SolrConstants.PREFIX_FACET.equals(prefix)) {
                ret = SolrConstants.PREFIX_FACET + ret;
            } else {
                ret = ret.replace("NE_", prefix);
            }
        } else if (ret.startsWith(SolrConstants.PREFIX_BOOL)) {
            ret = ret.replace(SolrConstants.PREFIX_BOOL, prefix);
        } else if (ret.startsWith(SolrConstants.PREFIX_SORT)) {
            ret = ret.replace(SolrConstants.PREFIX_SORT, prefix);
        }

        return ret;
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

        /**
         * If the given fieldname is a facetified version of a configured facet field, return the configured field
         */
        for (String field : DataManager.getInstance().getConfiguration().getAllFacetFields()) {
            String facetField = facetifyField(field);
            if (fieldName.equals(facetField)) {
                return field;
            }
        }

        switch (fieldName) {
            case SolrConstants.FACET_DC:
            case SolrConstants.PREFIX_FACET + SolrConstants.DOCSTRCT:
            case SolrConstants.PREFIX_FACET + SolrConstants.DOCSTRCT_SUB:
            case SolrConstants.PREFIX_FACET + SolrConstants.DOCSTRCT_TOP:
            case SolrConstants.PREFIX_FACET + SolrConstants.CALENDAR_YEAR:
            case SolrConstants.PREFIX_FACET + SolrConstants.CALENDAR_MONTH:
            case SolrConstants.PREFIX_FACET + SolrConstants.CALENDAR_DAY:
                return fieldName.substring(6);
            default:
                if (fieldName.startsWith(SolrConstants.PREFIX_FACET)) {
                    if (fieldName.startsWith(SolrConstants.PREFIX_FACET + "MD2_")
                            || fieldName.startsWith(SolrConstants.PREFIX_FACET + SolrConstants.PREFIX_MDNUM)) {
                        return fieldName.substring(6);
                    }
                    return fieldName.replace(SolrConstants.PREFIX_FACET, "MD_");
                }
                return fieldName;
        }
    }

    /**
     * Creates a Solr expand query string out of lists of fields and terms.
     *
     * @param fields a {@link java.util.List} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param proximitySearchDistance
     * @return a {@link java.lang.String} object.
     * @should generate query correctly
     * @should return empty string if no fields match
     * @should skip reserved fields
     * @should escape reserved characters
     * @should not escape asterisks
     * @should not escape truncation
     * @should add quotation marks if phraseSearch is true
     * @should add proximity search token correctly
     */
    public static String generateExpandQuery(List<String> fields, Map<String, Set<String>> searchTerms, int proximitySearchDistance) {
        logger.trace("generateExpandQuery");
        if (searchTerms.isEmpty()) {
            return "";
        }

        StringBuilder sbOuter = new StringBuilder();
        logger.trace("fields: {}", fields);
        logger.trace("searchTerms: {}", searchTerms);
        boolean moreThanOne = false;
        for (final String field : fields) {
            // Skip fields that exist in all child docs (e.g. PI_TOPSTRUCT) so that searches within a record don't return every single doc
            switch (field) {
                case SolrConstants.PI_TOPSTRUCT:
                case SolrConstants.PI_ANCHOR:
                case SolrConstants.DC:
                case SolrConstants.DOCSTRCT:
                    continue;
                default:
                    if (field.startsWith(SolrConstants.PREFIX_GROUPID)) {
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
                sbOuter.append(SolrConstants.SOLR_QUERY_OR);
            }
            StringBuilder sbInner = new StringBuilder();
            boolean multipleTerms = false;
            for (final String t : terms) {
                if (sbInner.length() > 0) {
                    sbInner.append(SolrConstants.SOLR_QUERY_OR);
                    multipleTerms = true;
                }
                String term = t;
                if (!"*".equals(term)) {
                    boolean quotationMarksApplied = false;
                    if ((term.startsWith("\"") && term.endsWith("\""))) {
                        quotationMarksApplied = true;
                    }
                    if (!quotationMarksApplied) {
                        term = ClientUtils.escapeQueryChars(term);
                    }
                    term = term.replace("\\*", "*");
                    //unescape fuzzy search token
                    term = term.replaceAll("\\\\~(\\d)", "~$1");
                    // logger.trace("term: {}", term); //NOSONAR Debug
                    //                    if (phraseSearch && !quotationMarksApplied) {
                    //                        term = '"' + term + '"';
                    //                    }
                }
                if (SolrConstants.FULLTEXT.equals(field) && proximitySearchDistance > 0) {
                    term = term.replace("\\\"", "\""); // unescape quotation marks
                    term = addProximitySearchToken(term, proximitySearchDistance);
                }
                // logger.trace("term: {}", term); //NOSONAR Debug
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

        logger.trace("expand query generated: {}", sbOuter);
        return sbOuter.toString();
    }

    /**
     * Creates a Solr expand query string out of advanced search query item groups.
     *
     * @param group
     * @param allowFuzzySearch
     * @return a {@link java.lang.String} object.
     * @should generate query correctly
     * @should skip reserved fields
     * @should switch to OR operator on fulltext items
     */
    public static String generateAdvancedExpandQuery(SearchQueryGroup group, boolean allowFuzzySearch) {
        logger.trace("generateAdvancedExpandQuery");
        if (group == null) {
            return "";
        }
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
                case SolrConstants.SEARCHTERMS_ARCHIVE:
                case SEARCH_FILTER_ALL_FIELD:
                    orMode = true;
                    break;
                default:
                    break;
            }
        }

        for (SearchQueryItem item : group.getQueryItems()) {
            if (item.getField() == null) {
                continue;
            }
            // logger.trace("item field: {}", item.getField()); //NOSONAR Debug
            // Skip fields that exist in all child docs (e.g. PI_TOPSTRUCT) so that searches within a record don't return every single doc
            switch (item.getField()) {
                case SolrConstants.PI_TOPSTRUCT:
                case SolrConstants.PI_ANCHOR:
                case SolrConstants.DC:
                case SolrConstants.DOCSTRCT:
                case SolrConstants.BOOKMARKS:
                    continue;
                default:
                    if (item.getField().startsWith(SolrConstants.PREFIX_GROUPID)) {
                        continue;
                    }
            }
            String itemQuery = item.generateQuery(new HashSet<>(), false, allowFuzzySearch);
            if (StringUtils.isNotEmpty(itemQuery)) {
                if (orMode && itemQuery.charAt(0) == '+') {
                    itemQuery = itemQuery.substring(1);
                }
                if (sbGroup.length() > 0) {
                    sbGroup.append(' ');
                }
                sbGroup.append(itemQuery);
            }
        }
        if (sbGroup.length() > 0) {
            return " +(" + sbGroup.toString() + ')';
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
     * @param queryGroup a {@link SearchQueryGroup} object.
     * @param additionalFields Optinal additional fields to return
     * @return a {@link java.util.List} object.
     */
    public static List<String> getExpandQueryFieldList(int searchType, SearchFilter searchFilter, SearchQueryGroup queryGroup,
            List<String> additionalFields) {
        List<String> ret = new ArrayList<>();
        // logger.trace("searchType: {}", searchType); //NOSONAR Debug
        switch (searchType) {
            case SearchHelper.SEARCH_TYPE_ADVANCED:
                if (queryGroup != null) {
                    for (SearchQueryItem item : queryGroup.getQueryItems()) {
                        if (SEARCH_FILTER_ALL_FIELD.equals(item.getField())) {
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
                            if (!ret.contains(SolrConstants.SEARCHTERMS_ARCHIVE)) {
                                ret.add(SolrConstants.SEARCHTERMS_ARCHIVE);
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
                        } else if (SolrConstants.SEARCHTERMS_ARCHIVE.equals(item.getField())
                                || SolrConstants.SUPERSEARCHTERMS_ARCHIVE.equals(item.getField())
                                        && !ret.contains(SolrConstants.SEARCHTERMS_ARCHIVE)) {
                            ret.add(SolrConstants.UGCTERMS);
                        } else if (SolrConstants.CMS_TEXT_ALL.equals(item.getField()) && !ret.contains(SolrConstants.CMS_TEXT_ALL)) {
                            ret.add(SolrConstants.CMS_TEXT_ALL);
                        } else if (!ret.contains(item.getField())) {
                            ret.add(item.getField());
                        }
                    }
                }
                break;
            case SearchHelper.SEARCH_TYPE_CALENDAR:
                ret.add(SolrConstants.CALENDAR_DAY);
                ret.add(SolrConstants.CALENDAR_MONTH);
                ret.add(SolrConstants.CALENDAR_YEAR);
                break;
            default:
                if (searchFilter == null || searchFilter.equals(SEARCH_FILTER_ALL)) {
                    // No filters defined or ALL: use DEFAULT + FULLTEXT + UGCTERMS
                    ret.add(SolrConstants.DEFAULT);
                    ret.add(SolrConstants.FULLTEXT);
                    ret.add(SolrConstants.NORMDATATERMS);
                    ret.add(SolrConstants.UGCTERMS);
                    ret.add(SolrConstants.SEARCHTERMS_ARCHIVE);
                    ret.add(SolrConstants.CMS_TEXT_ALL);
                    ret.add(SolrConstants.CALENDAR_DAY);
                } else {
                    ret.add(searchFilter.getField());
                }
                break;
        }

        if (additionalFields != null) {
            for (String field : additionalFields) {
                if (!ret.contains(field)) {
                    ret.add(field);
                }
            }
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
     * @should wrap query correctly
     */
    public static String prepareQuery(String query) {
        StringBuilder sbQuery = new StringBuilder();
        if (StringUtils.isNotEmpty(query)) {
            sbQuery.append("+(").append(query).append(')');
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
     *
     * @param searchTerms
     * @return Term query from searchTerms
     */
    public static String buildTermQuery(Collection<String> searchTerms) {
        return buildTermQuery(searchTerms, true);
    }

    /**
     *
     * @param searchTerms
     * @param addOperators
     * @return Term query from searchTerms
     */
    public static String buildTermQuery(Collection<String> searchTerms, boolean addOperators) {
        if (searchTerms == null || searchTerms.isEmpty()) {
            return "";
        }

        // Construct inner query part
        StringBuilder sbInner = new StringBuilder();
        for (String term : searchTerms) {
            if (sbInner.length() > 0) {
                if (addOperators) {
                    sbInner.append(SolrConstants.SOLR_QUERY_AND);
                } else {
                    sbInner.append(' ');
                }
            }
            if (!term.contains(SolrConstants.SOLR_QUERY_OR)) {
                sbInner.append(term);
            } else {
                sbInner.append('(').append(term).append(')');
            }
        }

        return sbInner.toString();
    }

    /**
     * Constructs the complete query using the raw query and adding all available suffixes.
     *
     * @param rawQuery a {@link java.lang.String} object.
     * @param boostTopLevelDocstructs If true, query elements for boosting will be added
     * @param aggregationType {@link SearchAggregationType}
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     *
     */
    public static String buildFinalQuery(String rawQuery, boolean boostTopLevelDocstructs, SearchAggregationType aggregationType) {
        return buildFinalQuery(rawQuery, boostTopLevelDocstructs, null, aggregationType);
    }

    /**
     * Constructs the complete query using the raw query and adding all available suffixes.
     *
     * @param rawQuery a {@link java.lang.String} object.
     * @param boostTopLevelDocstructs If true, query elements for boosting will be added
     * @param request
     * @param aggregationType {@link SearchAggregationType}
     * @return a {@link java.lang.String} object.
     * @should add embedded query template if boostTopLevelDocstructs true
     * @should add query prefix if boostTopLevelDocstructs true and termQuery not empty
     * @should escape quotation marks in embedded query
     * @should add join statement if aggregateHits true
     * @should not add join statement if aggregateHits false
     * @should remove existing join statement
     */
    public static String buildFinalQuery(final String rawQuery, boolean boostTopLevelDocstructs, HttpServletRequest request,
            SearchAggregationType aggregationType) {
        if (rawQuery == null) {
            throw new IllegalArgumentException("rawQuery may not be null");
        }

        // logger.trace("rawQuery: {}", rawQuery); //NOSONAR Debug
        StringBuilder sbQuery = new StringBuilder();
        if (SearchAggregationType.AGGREGATE_TO_TOPSTRUCT.equals(aggregationType)) {
            sbQuery.append(AGGREGATION_QUERY_PREFIX);
            // https://wiki.apache.org/solr/FieldCollapsing
            // https://wiki.apache.org/solr/Join
        }
        if (StringUtils.isNotBlank(rawQuery)) {
            sbQuery.append("+(").append(rawQuery.replace(AGGREGATION_QUERY_PREFIX, "")).append(")");
        }

        // Boosting
        if (boostTopLevelDocstructs) {
            String template = "+(" + EMBEDDED_QUERY_TEMPLATE.replace("{0}", sbQuery.toString().replace("\"", "\\\"")) + ")";
            sbQuery = new StringBuilder(template);
        }

        // Suffixes
        String suffixes = getAllSuffixes(request, true, true);
        if (StringUtils.isNotBlank(suffixes)) {
            sbQuery.append(suffixes);
        }

        return sbQuery.toString();
    }

    /**
     * @param request
     * @param privilege Privilege to check (Connector checks a different privilege)
     * @return Filter query suffix string from the HTTP session
     */
    static String getFilterQuerySuffix(final HttpServletRequest request, String privilege) {
        HttpServletRequest req = request;
        if (req == null) {
            req = BeanUtils.getRequest();
        }
        if (req == null) {
            return "";
        }
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }

        String ret = (String) session.getAttribute(PARAM_NAME_FILTER_QUERY_SUFFIX);
        // If not suffix generated yet, initiate update
        if (ret == null) {
            try {
                updateFilterQuerySuffix(request, privilege);
                ret = (String) session.getAttribute(PARAM_NAME_FILTER_QUERY_SUFFIX);
            } catch (IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage(), e);
            } catch (PresentationException e) {
                logger.error(e.getMessage());
            }
        }

        return ret;
    }

    /**
     * <p>
     * exportSearchAsExcel.
     * </p>
     *
     * @param wb {@link SXSSFWorkbook} to populate
     * @param finalQuery Complete query with suffixes.
     * @param exportQuery Query constructed from the user's input, without any secret suffixes.
     * @param sortFields a {@link java.util.List} object.
     * @param filterQueries a {@link java.util.List} object.
     * @param params a {@link java.util.Map} object.
     * @param searchTerms a {@link java.util.Map} object.
     * @param locale a {@link java.util.Locale} object.
     * @param proximitySearchDistance
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @should create excel workbook correctly
     */
    public static void exportSearchAsExcel(SXSSFWorkbook wb, String finalQuery, String exportQuery, List<StringPair> sortFields,
            List<String> filterQueries, Map<String, String> params, Map<String, Set<String>> searchTerms, Locale locale, int proximitySearchDistance)
            throws IndexUnreachableException, DAOException, PresentationException, ViewerConfigurationException {
        if (wb == null) {
            throw new IllegalArgumentException("wb may not be null");
        }

        SXSSFSheet currentSheet = wb.createSheet("Goobi_viewer_search");
        CellStyle styleBold = wb.createCellStyle();
        Font font2 = wb.createFont();
        font2.setFontHeightInPoints((short) 10);
        font2.setBold(true);
        styleBold.setFont(font2);

        int currentRowIndex = 0;
        int currentCellIndex = 0;

        // Query row
        SXSSFRow qRow = currentSheet.createRow(currentRowIndex++);
        SXSSFCell qCell = qRow.createCell(currentCellIndex++);
        qCell.setCellStyle(styleBold);
        qCell.setCellValue(new XSSFRichTextString("Query:"));
        qCell = qRow.createCell(currentCellIndex);
        qCell.setCellValue(new XSSFRichTextString(exportQuery));
        currentCellIndex = 0;

        // Title row
        SXSSFRow row = currentSheet.createRow(currentRowIndex++);
        for (ExportFieldConfiguration field : DataManager.getInstance().getConfiguration().getSearchExcelExportFields()) {
            SXSSFCell cell = row.createCell(currentCellIndex++);
            cell.setCellStyle(styleBold);
            cell.setCellValue(new XSSFRichTextString(ViewerResourceBundle.getTranslation(field.getField(), locale)));
        }

        List<ExportFieldConfiguration> exportFields = DataManager.getInstance().getConfiguration().getSearchExcelExportFields();
        List<String> exportFieldNames = new ArrayList<>(exportFields.size());
        for (ExportFieldConfiguration field : exportFields) {
            exportFieldNames.add(field.getField());
        }
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
            List<SearchHit> batch =
                    searchWithAggregation(finalQuery, first, batchSize, sortFields, null, filterQueries, params, searchTerms, exportFieldNames,
                            Configuration.METADATA_LIST_TYPE_SEARCH_HIT, locale, false, proximitySearchDistance);

            for (SearchHit hit : batch) {
                // Create row
                currentCellIndex = 0;
                row = currentSheet.createRow(currentRowIndex++);
                for (ExportFieldConfiguration field : exportFields) {
                    SXSSFCell cell = row.createCell(currentCellIndex++);
                    String value = hit.getExportMetadata().get(field.getField());
                    cell.setCellValue(new XSSFRichTextString(value != null ? value : ""));
                }
            }
        }
    }

    /**
     * <p>
     * parseSortString.
     * </p>
     *
     * @param sortString a {@link java.lang.String} object.
     * @param navigationHelper a {@link io.goobi.viewer.managedbeans.NavigationHelper} object.
     * @return a {@link java.util.List} object.
     * @should parse string correctly
     */
    public static List<StringPair> parseSortString(String sortString, NavigationHelper navigationHelper) {
        if (StringUtils.isEmpty(sortString)) {
            return Collections.emptyList();
        }

        List<StringPair> ret = new ArrayList<>();
        String[] sortStringSplit = sortString.split(";");
        if (sortStringSplit.length > 0) {
            for (String field : sortStringSplit) {
                ret.add(new StringPair(field.replace("!", ""), field.charAt(0) == '!' ? "desc" : "asc"));
                String s = field.replaceAll("[\n\r]", "_");
                logger.trace("Added sort field: {}", s);
                // add translated sort fields
                if (navigationHelper != null && field.startsWith(SolrConstants.PREFIX_SORT)) {
                    Iterable<Locale> locales = navigationHelper::getSupportedLocales;
                    StreamSupport.stream(locales.spliterator(), false)
                            .sorted(new LocaleComparator(BeanUtils.getLocale()))
                            .map(locale -> field + SolrConstants.MIDFIX_LANG + locale.getLanguage().toUpperCase())
                            .peek(language -> logger.trace("Adding sort field: {}", language))
                            .forEach(language -> ret.add(new StringPair(language.replace("!", ""), language.charAt(0) == '!' ? "desc" : "asc")));
                }
            }
        }

        return ret;
    }

    /**
     * @param expandQuery the query used to filter the expanded hits
     * @return Map&lt;String, String&gt;
     */
    public static Map<String, String> getExpandQueryParams(String expandQuery) {
        return getExpandQueryParams(expandQuery, SolrSearchIndex.MAX_HITS_EXPANDED);
    }

    /**
     * @param expandQuery the query used to filter the expanded hits
     * @param maxHits maximum number of expanded hits per main hits
     * @return Map&lt;String, String&gt;
     */
    public static Map<String, String> getExpandQueryParams(String expandQuery, Integer maxHits) {
        Map<String, String> params = new HashMap<>();
        params.put(ExpandParams.EXPAND, "true");
        params.put(ExpandParams.EXPAND_Q, expandQuery);
        params.put(ExpandParams.EXPAND_FIELD, SolrConstants.PI_TOPSTRUCT);
        if (maxHits != null) {
            params.put(ExpandParams.EXPAND_ROWS, String.valueOf(maxHits));
        }
        params.put(ExpandParams.EXPAND_SORT, SolrConstants.ORDER + " asc");
        params.put(ExpandParams.EXPAND_FQ, ""); // The main filter query may not apply to the expand query to produce child hits
        return params;
    }

    /**
     * Removes illegal characters from an individual search term. Do not use on whole queries!
     *
     * @param term The term to clean up.
     * @return Cleaned up term.
     * @should remove illegal chars correctly
     * @should remove trailing punctuation
     * @should preserve truncation
     * @should preserve negation
     */
    public static String cleanUpSearchTerm(final String term) {
        String s = term;
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

            // Remove trailing punctuation
            boolean done = false;
            while (s.length() > 1) {
                if (done) {
                    break;
                }
                char last = s.charAt(s.length() - 1);
                switch (last) {
                    case '.':
                    case ',':
                    case ':':
                    case ';':
                        s = s.substring(0, s.length() - 1);
                        break;
                    default:
                        done = true;
                        break;
                }

            }

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
     * @return Constructed Solr query
     * @should build escaped query correctly
     * @should build not escaped query correctly
     */
    public static String getQueryForAccessCondition(final String accessCondition, boolean escapeAccessCondition) {
        String ac = escapeAccessCondition ? BeanUtils.escapeCriticalUrlChracters(accessCondition) : accessCondition;
        return "+(" + SolrConstants.ISWORK + ":true " + SolrConstants.ISANCHOR + ":true " + SolrConstants.DOCTYPE
                + ":(" + DocType.UGC.name() + " " + DocType.METADATA.name() + " " + DocType.ARCHIVE.name() + ")) +"
                + SolrConstants.ACCESSCONDITION + ":\"" + ac + "\"";
    }

    /**
     * Adds a fuzzy search token to the given term. The maximal Damerau-Levenshtein is calculated from term length
     *
     * @param term the search term
     * @param prefix
     * @param suffix
     * @return the given term with a fuzzy search token appended
     */
    public static String addFuzzySearchToken(String term, String prefix, String suffix) {
        int distance = FuzzySearchTerm.calculateOptimalDistance(term);
        return addFuzzySearchToken(term, distance, prefix, suffix);
    }

    /**
     * @param term the search term. Must be a single word
     * @param distance the maximum Damerau-Levenshtein distance to a matching word. Must be from 0 to 2, where 0 means no fuzzy search
     * @param prefix
     * @param suffix
     * @return term with fuzzy search token
     */
    public static String addFuzzySearchToken(String term, int distance, String prefix, String suffix) {
        if (distance < 0 || distance > 2) {
            throw new IllegalArgumentException("Edit distance in fuzzy search must be in the range from 0 to 2. The given distance is " + distance);
        } else if (distance == 0) {
            return prefix + term + suffix;
        } else if (StringUtils.isBlank(term) || term.contains(" ")) {
            throw new IllegalArgumentException(
                    "For fuzzy search, term must not be empty and must consist only of a single word. The given term is " + term);
        } else {
            return FUZZY_SEARCH_TERM_TEMPLATE_WITH_BOOST
                    .replace("{t}", term)
                    .replace("{d}", Integer.toString(distance))
                    .replace("{p}", StringUtils.isBlank(prefix) ? "" : prefix)
                    .replace("{s}", StringUtils.isBlank(suffix) ? "" : suffix);
        }
    }

    /**
     *
     * @param term
     * @param distance
     * @return {@link String}
     * @should term with proximity search token
     */
    public static String addProximitySearchToken(String term, int distance) {
        if (StringUtils.isEmpty(term)) {
            return term;
        }

        boolean addQuotationMarks = false;
        if (!term.startsWith("\"") && !term.endsWith("\"")) {
            addQuotationMarks = true;
        }

        StringBuilder sb = new StringBuilder();
        if (addQuotationMarks) {
            sb.append('"');
        }
        sb.append(term);
        if (addQuotationMarks) {
            sb.append('"');
        }
        sb.append('~');
        sb.append(distance);

        return sb.toString();
    }

    /**
     *
     * @param term Search term containing proximity search token
     * @return term without proximity search token
     * @should remove token correctly
     * @should return unmodified term if no token found
     */
    public static String removeProximitySearchToken(String term) {
        if (StringUtils.isEmpty(term) || !term.contains("\"~")) {
            return term;
        }

        Matcher m = SearchHelper.PATTERN_PROXIMITY_SEARCH_TOKEN.matcher(term);
        if (m.find()) {
            String num = m.group(1);
            if (StringUtils.isNotBlank(num)) {
                return term.replace("~" + num, "");
            }
        }

        return term;
    }

    /**
     *
     * @param query
     * @return Extracted search distance
     * @should return 0 if query empty
     * @should return 0 if query does not contain token
     * @should return 0 if query not phrase search
     * @should extract distance correctly
     */
    public static int extractProximitySearchDistanceFromQuery(String query) {
        if (StringUtils.isEmpty(query) || !query.contains("\"~")) {
            return 0;
        }

        int ret = 0;
        Matcher m = PATTERN_PROXIMITY_SEARCH_TOKEN.matcher(query);
        if (m.find()) {
            String num = m.group(1);
            if (StringUtils.isNotBlank(num)) {
                try {
                    ret = Integer.valueOf(num);
                    logger.trace("Extracted proximity search distance: {}", ret);
                } catch (NumberFormatException e) {
                    logger.warn(e.getMessage());
                }
            }
        }

        return ret;
    }

    /**
     * Determines whether the given string is a quoted search phrase, optionally with a proximity distance.
     *
     * @param s Search terms
     * @return true if phrase; false otherwise
     * @should detect phrase correctly
     * @should detect phrase with proximity correctly
     */
    public static boolean isPhrase(String s) {
        if (StringUtils.isBlank(s)) {
            return false;
        }

        Matcher m = PATTERN_PHRASE.matcher(s.trim());
        return m.find();
    }

    /**
     * Separate leading and trailing wildcard token ('*') from the actual term and return an array of length 3 with the values [leadingWildCard,
     * tokenWithoutWildcards, trailingWildcard] If leading/trailing wildcards are missing, the corresponding array entries are empty strings
     *
     * @param term
     * @return array of prefix, token, suffix
     */
    public static String[] getWildcardsTokens(String term) {
        String prefix = term.startsWith("*") ? "*" : "";
        String suffix = term.endsWith("*") ? "*" : "";
        String cleanedTerm = term.substring(prefix.length(), term.length() - suffix.length());
        return new String[] { prefix, cleanedTerm, suffix };
    }

    /**
     * Constructs an expand query from given facet queries. Constrains the query to DOCSTRCT doc types only.
     *
     * @param allFacetQueries
     * @param allowedFacetQueryRegexes Optional list containing regexes for allowed facet queries
     * @return Expand query
     * @should return empty string if list null or empty
     * @should construct query correctly
     * @should only use queries that match allowed regex
     * @should return empty string of no query allowed
     */
    public static String buildExpandQueryFromFacets(List<String> allFacetQueries, List<String> allowedFacetQueryRegexes) {
        if (allFacetQueries == null || allFacetQueries.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String q : allFacetQueries) {
            if (StringUtils.isBlank(q)) {
                continue;
            }
            if (allowedFacetQueryRegexes == null || allowedFacetQueryRegexes.isEmpty()) {
                sb.append(" +").append(q);
            } else {
                for (String allowedFacetQuery : allowedFacetQueryRegexes) {
                    Pattern p = Pattern.compile(allowedFacetQuery);
                    Matcher m = p.matcher(q);
                    if (m.matches()) {
                        sb.append(" +").append(q);
                    }
                }
            }
        }
        if (sb.length() > 0) {
            sb.append(" +").append(SolrConstants.DOCTYPE).append(':').append(DocType.DOCSTRCT.name());
        }

        return sb.toString().trim();
    }
}
