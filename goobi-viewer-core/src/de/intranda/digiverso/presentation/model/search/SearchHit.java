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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrConstants.DocType;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Wrapper class for search hits. Contains the corresponding <code>BrowseElement</code>
 */
public class SearchHit implements Comparable<SearchHit> {

    public enum HitType {
        DOCSTRCT,
        PAGE,
        METADATA, // grouped metadata
        EVENT, // LIDO event
        UGC, // user-generated content
        GROUP, // convolute/series
        OVERVIEWPAGE; // overview page type for search hits

        public static HitType getByName(String name) {
            if (name != null) {
                switch (name) {
                    case "DOCSTRCT":
                        return DOCSTRCT;
                    case "PAGE":
                        return PAGE;
                    case "EVENT":
                        return EVENT;
                    case "OVERVIEWPAGE":
                        return OVERVIEWPAGE;
                    default:
                        return null;
                }
            }

            return null;
        }

        public String getLabel(Locale locale) {
            return Helper.getTranslation(new StringBuilder("doctype_").append(name()).toString(), locale);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SearchHit.class);

    private final HitType type;
    private final BrowseElement browseElement;
    @JsonIgnore
    private List<SolrDocument> childDocs;
    @JsonIgnore
    private final Map<String, SearchHit> ownerHits = new HashMap<>();
    @JsonIgnore
    private final Map<String, SolrDocument> ownerDocs = new HashMap<>();
    @JsonIgnore
    private final Map<String, Set<String>> searchTerms;
    /** Docstruct metadata that matches the search terms. */
    private final List<StringPair> foundMetadata = new ArrayList<>();
    private final String url;
    @JsonIgnore
    private final Locale locale;
    private final List<SearchHit> children = new ArrayList<>();
    private final Map<HitType, Integer> hitTypeCounts = new HashMap<>();
    /** Metadata for Excel export. */
    @JsonIgnore
    private final Map<String, String> exportMetadata = new HashMap<>();
    @JsonIgnore
    private int hitsPopulated = 0;

    /**
     * Private constructor. Use createSearchHit() from other classes.
     * 
     * @param type
     * @param browseElement
     * @param searchTerms
     * @param locale
     */
    private SearchHit(HitType type, BrowseElement browseElement, Map<String, Set<String>> searchTerms, Locale locale) {
        this.type = type;
        this.browseElement = browseElement;
        this.searchTerms = searchTerms;
        this.locale = locale;
        if (searchTerms != null) {
            addLabelHighlighting();
        }
        if (browseElement != null) {
            this.url = browseElement.getUrl();
        } else {
            this.url = null;
        }
    }

    /**
     * 
     * @param doc
     * @param ownerDoc
     * @param locale
     * @param fulltext Optional fulltext (page docs only).
     * @param searchTerms
     * @param exportFields Optional fields for (Excel) export purposes.
     * @param useThumbnail
     * @param matchTermsToMetadataValues
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should add export fields correctly
     */
    public static SearchHit createSearchHit(SolrDocument doc, SolrDocument ownerDoc, Locale locale, String fulltext,
            Map<String, Set<String>> searchTerms, List<String> exportFields, boolean useThumbnail) throws PresentationException,
            IndexUnreachableException, DAOException {
        String fulltextFragment = fulltext == null ? null : SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.FULLTEXT), fulltext,
                DataManager.getInstance().getConfiguration().getFulltextFragmentLength());
        StructElement se = new StructElement(Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC)), doc, ownerDoc);
        String docstructType = se.getDocStructType();
        if (DocType.METADATA.name().equals(se.getMetadataValue(SolrConstants.DOCTYPE))) {
            docstructType = DocType.METADATA.name();
        }
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate(docstructType);
        BrowseElement browseElement = new BrowseElement(se, metadataList, locale, fulltextFragment, useThumbnail, searchTerms);
        // Add additional metadata fields that aren't configured for search hits but contain search term values
        browseElement.addAdditionalMetadataContainingSearchTerms(se, searchTerms);
        SearchHit hit = new SearchHit(HitType.getByName(se.getMetadataValue(SolrConstants.DOCTYPE)), browseElement, searchTerms, locale);
        hit.populateFoundMetadata(doc);

        // Export fields for Excel export
        if (exportFields != null && !exportFields.isEmpty()) {
            for (String field : exportFields) {
                String value = se.getMetadataValue(field);
                if (value != null) {
                    hit.getExportMetadata().put(field, value);
                }
            }
        }
        return hit;
    }

    /**
     * First truncate and unescape the label, then add highlighting (overrides BrowseElement.labelShort).
     * 
     * @should modify label correctly
     */
    void addLabelHighlighting() {
        if (searchTerms == null) {
            return;
        }
        String labelShort = browseElement.getLabel();
        if (searchTerms.get(SolrConstants.DEFAULT) != null) {
            labelShort = SearchHelper.applyHighlightingToPhrase(labelShort, searchTerms.get(SolrConstants.DEFAULT));
        } else if (searchTerms.get("MD_TITLE") != null) {
            labelShort = SearchHelper.applyHighlightingToPhrase(labelShort, searchTerms.get("MD_TITLE"));
        }
        // Escape HTML tags
        labelShort = StringEscapeUtils.escapeHtml(labelShort);
        // Then replace highlighting placeholders with HTML tags
        labelShort = SearchHelper.replaceHighlightingPlaceholders(labelShort);
        browseElement.setLabelShort(labelShort);
    }

    /**
     * Creates a child hit element for the overview page, if overview page texts were also searched.
     */
    public void addOverviewPageChild() {
        if (searchTerms == null) {
            return;
        }
        if (searchTerms.containsKey(SolrConstants.OVERVIEWPAGE_DESCRIPTION) || searchTerms.containsKey(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT)) {
            try {
                OverviewPage overviewPage = DataManager.getInstance().getDao().getOverviewPageForRecord(browseElement.getPi(), null, null);
                if (overviewPage != null) {
                    String descriptionText = null;
                    if (overviewPage.getDescription() != null) {
                        String value = Jsoup.parse(overviewPage.getDescription()).text();
                        String highlightedValue = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(
                                SolrConstants.OVERVIEWPAGE_DESCRIPTION));
                        if (!highlightedValue.equals(value)) {
                            descriptionText = SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.OVERVIEWPAGE_DESCRIPTION), highlightedValue,
                                    DataManager.getInstance().getConfiguration().getFulltextFragmentLength());
                            //                            metadataList.add(new Metadata("viewOverviewDescription", "", highlightedValue));

                        }
                    }
                    String publicationText = null;
                    if (overviewPage.getPublicationText() != null) {
                        String value = Jsoup.parse(overviewPage.getPublicationText()).text();
                        String highlightedValue = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(
                                SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT));
                        if (!highlightedValue.equals(value)) {
                            publicationText = SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT),
                                    highlightedValue, DataManager.getInstance().getConfiguration().getFulltextFragmentLength());
                            //                            metadataList.add(new Metadata("viewOverviewPublication_publication", "", highlightedValue));
                        }
                    }
                    if (descriptionText != null || publicationText != null) {
                        int count = 0;
                        SearchHit overviewPageHit = new SearchHit(HitType.METADATA, new BrowseElement(browseElement.getPi(), 1, Helper.getTranslation(
                                "overviewPage", locale), null, true), searchTerms, locale);
                        children.add(overviewPageHit);
                        if (descriptionText != null) {
                            overviewPageHit.getChildren().add(new SearchHit(HitType.PAGE, new BrowseElement(browseElement.getPi(), 1,
                                    "viewOverviewDescription", descriptionText, true), searchTerms, locale));
                            count++;
                        }
                        if (publicationText != null) {
                            overviewPageHit.getChildren().add(new SearchHit(HitType.PAGE, new BrowseElement(browseElement.getPi(), 1,
                                    "viewOverviewPublication_publication", publicationText, true), searchTerms, locale));
                            count++;
                        }
                        hitTypeCounts.put(HitType.OVERVIEWPAGE, count);
                        logger.trace("Added {} overview page child hits", count);
                    }
                }
            } catch (DAOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 
     * @param number
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public void populateChildren(int number) throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("populateChildren START");

        // Create child hits
        String pi = browseElement.getPi();
        if (pi != null && childDocs != null) {
            logger.trace("{} child hits found for {}", childDocs.size(), pi);
            if (number > childDocs.size()) {
                number = childDocs.size();
            }
            for (int i = 0; i < number; ++i) {
                SolrDocument childDoc = childDocs.get(i);
                String fulltext = null;
                DocType docType = DocType.getByName((String) childDoc.getFieldValue(SolrConstants.DOCTYPE));
                if (docType == null) {
                    logger.warn("Document {} has no DOCTYPE field, cannot add to child search hits.", childDoc.getFieldValue(SolrConstants.IDDOC));
                    continue;
                }
                //                    logger.trace("Found child doc: {}", docType);
                switch (docType) {
                    case PAGE:
                        fulltext = (String) childDoc.getFirstValue("MD_FULLTEXT");
                        if (fulltext == null) {
                            fulltext = (String) childDoc.getFieldValue(SolrConstants.FULLTEXT);
                        }
                    case METADATA:
                    case EVENT:
                        String ownerIddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC_OWNER);
                        SearchHit ownerHit = ownerHits.get(ownerIddoc);
                        if (ownerHit == null) {
                            SolrDocument ownerDoc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc(ownerIddoc);
                            if (ownerDoc != null) {
                                ownerHit = createSearchHit(ownerDoc, null, locale, fulltext, searchTerms, null, false);
                                children.add(ownerHit);
                                ownerHits.put(ownerIddoc, ownerHit);
                                ownerDocs.put(ownerIddoc, ownerDoc);
                                logger.trace("owner doc: {}", ownerDoc.getFieldValue("LOGID"));
                            }
                        }
                        if (ownerHit == null) {
                            logger.error("No document found for IDDOC {}", ownerIddoc);
                            continue;
                        } {
                        SearchHit childHit = createSearchHit(childDoc, ownerDocs.get(ownerIddoc), locale, fulltext, searchTerms, null, false);
                        ownerHit.getChildren().add(childHit);
                        hitsPopulated++;
                    }
                        break;
                    case DOCSTRCT:
                        // Docstruct hits are immediate children of the main hit
                        String iddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC);
                        if (!ownerHits.containsKey(iddoc)) {
                            SearchHit childHit = createSearchHit(childDoc, null, locale, fulltext, searchTerms, null, false);
                            children.add(childHit);
                            ownerHits.put(iddoc, childHit);
                            ownerDocs.put(iddoc, childDoc);
                            hitsPopulated++;
                        }
                        break;
                    case GROUP:
                        break;
                    case UGC:
                        break;
                    default:
                        break;
                }
            }
            childDocs = childDocs.subList(number, childDocs.size());
            if (childDocs.isEmpty()) {
                ownerDocs.clear();
                ownerHits.clear();
            }
            logger.trace("Remaning child docs: {}", childDocs.size());
        }
        logger.trace("populateChildren END");
    }

    /**
     * 
     * @param doc
     * @should add field values pairs that match search terms
     * @should add MD fields that contain terms from DEFAULT
     * @should not add duplicate values
     */
    public void populateFoundMetadata(SolrDocument doc) {
        if (searchTerms == null) {
            return;
        }
        boolean overviewPageFetched = false;
        for (String fieldName : searchTerms.keySet()) {
            switch (fieldName) {
                case SolrConstants._CALENDAR_DAY:
                case SolrConstants._CALENDAR_MONTH:
                case SolrConstants.ISANCHOR:
                case SolrConstants.ISWORK:
                case SolrConstants.NORMDATATERMS:
                case SolrConstants.PI_ANCHOR:
                case SolrConstants.UGCTERMS:
                    break;
                case SolrConstants.DEFAULT:
                    // If searching in DEFAULT, add all fields that contain any of the terms (instead of DEFAULT)
                    for (String docFieldName : doc.getFieldNames()) {
                        if (!docFieldName.startsWith("MD_") || docFieldName.endsWith(SolrConstants._UNTOKENIZED)) {
                            continue;
                        }
                        List<String> fieldValues = SolrSearchIndex.getMetadataValues(doc, docFieldName);
                        for (String fieldValue : fieldValues) {
                            // Skip values that are equal to the hit label
                            if (fieldValue.equals(browseElement.getLabel())) {
                                continue;
                            }
                            String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(fieldName));
                            if (!highlightedValue.equals(fieldValue)) {
                                // Translate values for certain fields
                                if (SolrConstants.DC.equals(docFieldName) || SolrConstants.DOCSTRCT.equals(docFieldName)) {
                                    String translatedValue = Helper.getTranslation(fieldValue, locale);
                                    highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)", "$1"
                                            + translatedValue + "$3");
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                foundMetadata.add(new StringPair(Helper.getTranslation(docFieldName, locale), highlightedValue));
                            }
                        }
                    }
                    break;
                default:
                    // Look up the exact field name in he Solr doc and add its values that contain any of the terms for that field
                    if (doc.containsKey(fieldName)) {
                        List<String> fieldValues = SolrSearchIndex.getMetadataValues(doc, fieldName);
                        for (String fieldValue : fieldValues) {
                            String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(fieldName));
                            if (!highlightedValue.equals(fieldValue)) {
                                // Translate values for certain fields
                                if (SolrConstants.DC.equals(fieldName) || SolrConstants.DOCSTRCT.equals(fieldName)) {
                                    String translatedValue = Helper.getTranslation(fieldValue, locale);
                                    highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)", "$1"
                                            + translatedValue + "$3");
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                foundMetadata.add(new StringPair(Helper.getTranslation(fieldName, locale), highlightedValue));
                            }
                        }
                    }
                    break;
            }

        }
    }

    /**
     * @return the type
     */
    public HitType getType() {
        return type;
    }

    /**
     * @return the browseElement
     */
    public BrowseElement getBrowseElement() {
        return browseElement;
    }

    /**
     * @return the childDocs
     */
    public List<SolrDocument> getChildDocs() {
        return childDocs;
    }

    /**
     * @return the hitsPopulated
     */
    public int getHitsPopulated() {
        return hitsPopulated;
    };

    /**
     * @param childDocs the childDocs to set
     */
    public void setChildDocs(SolrDocumentList childDocs) {
        this.childDocs = childDocs;
    }

    /**
     * Returns true if this hit has populated child elements.
     * 
     * @return
     */
    public boolean isHasChildren() {
        return children != null && !children.isEmpty();
    }

    /**
     * Returns true if this hit has any unpopulated child hits left.
     * 
     * @return
     */
    public boolean isHasMoreChildren() {
        return childDocs != null && !childDocs.isEmpty();
    }

    /**
     * @return the children
     */
    public List<SearchHit> getChildren() {
        return children;
    }

    /**
     * @return the hitTypeCounts
     */
    public Map<HitType, Integer> getHitTypeCounts() {
        return hitTypeCounts;
    }

    /**
     * 
     * @return
     */
    public boolean isHasHitCount() {
        for (HitType key : hitTypeCounts.keySet()) {
            if (hitTypeCounts.get(key) > 0) {
                return true;
            }
        }

        return false;
    }

    public int getOverviewPageHitCount() {
        if (hitTypeCounts.get(HitType.OVERVIEWPAGE) != null) {
            return hitTypeCounts.get(HitType.OVERVIEWPAGE);
        }

        return 0;
    }

    public int getDocstructHitCount() {
        if (hitTypeCounts.get(HitType.DOCSTRCT) != null) {
            return hitTypeCounts.get(HitType.DOCSTRCT);
        }

        return 0;
    }

    public int getPageHitCount() {
        if (hitTypeCounts.get(HitType.PAGE) != null) {
            return hitTypeCounts.get(HitType.PAGE);
        }

        return 0;
    }

    public int getEventHitCount() {
        if (hitTypeCounts.get(HitType.EVENT) != null) {
            return hitTypeCounts.get(HitType.EVENT);
        }

        return 0;
    }

    /**
     * @return the foundMetadata
     */
    public List<StringPair> getFoundMetadata() {
        return foundMetadata;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the exportMetadata
     */
    public Map<String, String> getExportMetadata() {
        return exportMetadata;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(SearchHit other) {
        return Integer.compare(this.getBrowseElement().getImageNo(), other.getBrowseElement().getImageNo());
    }

}
