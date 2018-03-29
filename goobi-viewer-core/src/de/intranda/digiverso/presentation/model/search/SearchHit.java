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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
import de.intranda.digiverso.presentation.exceptions.AccessDeniedException;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.ViewerResourceBundle;
import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.overviewpage.OverviewPage;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Wrapper class for search hits. Contains the corresponding <code>BrowseElement</code>
 */
public class SearchHit implements Comparable<SearchHit> {

    public enum HitType {
        ACCESSDENIED,
        DOCSTRCT,
        PAGE,
        METADATA, // grouped metadata
        PERSON,
        EVENT, // LIDO event
        UGC, // user-generated content
        GROUP, // convolute/series
        OVERVIEWPAGE; // overview page type for search hits

        public static HitType getByName(String name) {
            if (name != null) {
                switch (name) {
                    case "ACCESSDENIED":
                        return ACCESSDENIED;
                    case "DOCSTRCT":
                        return DOCSTRCT;
                    case "PAGE":
                        return PAGE;
                    case "EVENT":
                        return EVENT;
                    case "OVERVIEWPAGE":
                        return OVERVIEWPAGE;
                    case "METADATA":
                        return METADATA;
                    case "PERSON":
                        return PERSON;
                    default:
                        return null;
                }
            }

            return null;
        }

        public String getLabel(Locale locale) {
            return Helper.getTranslation(new StringBuilder("doctype_").append(name())
                    .toString(), locale);
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
     * @param ignoreAdditionalFields
     * @param translateAdditionalFields
     * @param overrideType
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     * @should add export fields correctly
     */
    public static SearchHit createSearchHit(SolrDocument doc, SolrDocument ownerDoc, Locale locale, String fulltext,
            Map<String, Set<String>> searchTerms, List<String> exportFields, boolean useThumbnail, Set<String> ignoreAdditionalFields,
            Set<String> translateAdditionalFields, HitType overrideType) throws PresentationException, IndexUnreachableException, DAOException {
        List<String> fulltextFragments = fulltext == null ? null
                : SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.FULLTEXT), fulltext, DataManager.getInstance()
                        .getConfiguration()
                        .getFulltextFragmentLength(), true);
        StructElement se = new StructElement(Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC)), doc, ownerDoc);
        String docstructType = se.getDocStructType();
        if (DocType.METADATA.name()
                .equals(se.getMetadataValue(SolrConstants.DOCTYPE))) {
            docstructType = DocType.METADATA.name();
        }
        
        
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate(docstructType);
        BrowseElement browseElement = new BrowseElement(se, metadataList, locale, (fulltextFragments != null && !fulltextFragments.isEmpty())
                ? fulltextFragments.get(0) : null, useThumbnail, searchTerms, BeanUtils.getImageDeliveryBean().getThumb());
        // Add additional metadata fields that aren't configured for search hits but contain search term values
        browseElement.addAdditionalMetadataContainingSearchTerms(se, searchTerms, ignoreAdditionalFields, translateAdditionalFields);

        // Determine hit type
        String docType = se.getMetadataValue(SolrConstants.DOCTYPE);
        HitType hitType = overrideType;
        if (hitType == null) {
            hitType = HitType.getByName(docType);
            if (DocType.METADATA.name()
                    .equals(docType)) {
                // For metadata hits use the metadata type for the hit type
                String metadataType = se.getMetadataValue(SolrConstants.METADATATYPE);
                if (StringUtils.isNotEmpty(metadataType)) {
                    hitType = HitType.getByName(metadataType);
                }
            }
        }

        SearchHit hit = new SearchHit(hitType, browseElement, searchTerms, locale);
        hit.populateFoundMetadata(doc, ignoreAdditionalFields, translateAdditionalFields);

        // Export fields for Excel export
        if (exportFields != null && !exportFields.isEmpty()) {
            for (String field : exportFields) {
                String value = se.getMetadataValue(field);
                if (value != null) {
                    hit.getExportMetadata()
                            .put(field, value);
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
                OverviewPage overviewPage = DataManager.getInstance()
                        .getDao()
                        .getOverviewPageForRecord(browseElement.getPi(), null, null);
                if (overviewPage != null) {
                    List<String> descriptionTexts = null;
                    if (overviewPage.getDescription() != null) {
                        String value = Jsoup.parse(overviewPage.getDescription())
                                .text();
                        String highlightedValue =
                                SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(SolrConstants.OVERVIEWPAGE_DESCRIPTION));
                        if (!highlightedValue.equals(value)) {
                            descriptionTexts = SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.OVERVIEWPAGE_DESCRIPTION),
                                    highlightedValue, DataManager.getInstance()
                                            .getConfiguration()
                                            .getFulltextFragmentLength(),
                                    false);

                        }
                    }
                    List<String> publicationTexts = null;
                    if (overviewPage.getPublicationText() != null) {
                        String value = Jsoup.parse(overviewPage.getPublicationText())
                                .text();
                        String highlightedValue =
                                SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT));
                        if (!highlightedValue.equals(value)) {
                            publicationTexts = SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.OVERVIEWPAGE_PUBLICATIONTEXT),
                                    highlightedValue, DataManager.getInstance()
                                            .getConfiguration()
                                            .getFulltextFragmentLength(),
                                    false);
                        }
                    }
                    if ((descriptionTexts != null && !descriptionTexts.isEmpty()) || (publicationTexts != null && !publicationTexts.isEmpty())) {
                        int count = 0;
                        SearchHit overviewPageHit = new SearchHit(HitType.METADATA,
                                new BrowseElement(browseElement.getPi(), 1, Helper.getTranslation("overviewPage", locale), null, true, locale, null),
                                searchTerms, locale);
                        children.add(overviewPageHit);
                        if (descriptionTexts != null && !descriptionTexts.isEmpty()) {
                            for (String descriptionText : descriptionTexts) {
                                overviewPageHit.getChildren()
                                        .add(new SearchHit(HitType.PAGE, new BrowseElement(browseElement.getPi(), 1, "viewOverviewDescription",
                                                descriptionText, true, locale, null), searchTerms, locale));
                                count++;
                            }
                        }
                        if (publicationTexts != null && !publicationTexts.isEmpty()) {
                            for (String publicationText : publicationTexts) {
                                overviewPageHit.getChildren()
                                        .add(new SearchHit(HitType.PAGE, new BrowseElement(browseElement.getPi(), 1,
                                                "viewOverviewPublication_publication", publicationText, true, locale, null), searchTerms, locale));
                                count++;
                            }
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
     * @param locale
     * @param request
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public void populateChildren(int number, Locale locale, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, DAOException {
        logger.trace("populateChildren START");

        // Create child hits
        String pi = browseElement.getPi();
        if (pi != null && childDocs != null) {
            logger.trace("{} child hits found for {}", childDocs.size(), pi);
            if (number > childDocs.size()) {
                number = childDocs.size();
            }
            Set<String> ignoreFields = new HashSet<>(DataManager.getInstance()
                    .getConfiguration()
                    .getDisplayAdditionalMetadataIgnoreFields());
            Set<String> translateFields = new HashSet<>(DataManager.getInstance()
                    .getConfiguration()
                    .getDisplayAdditionalMetadataTranslateFields());
            for (int i = 0; i < number; ++i) {
                SolrDocument childDoc = childDocs.get(i);
                String fulltext = null;
                DocType docType = DocType.getByName((String) childDoc.getFieldValue(SolrConstants.DOCTYPE));
                if (docType == null) {
                    logger.warn("Document {} has no DOCTYPE field, cannot add to child search hits.", childDoc.getFieldValue(SolrConstants.IDDOC));
                    continue;
                }
                //                    logger.trace("Found child doc: {}", docType);
                boolean acccessDeniedType = false;
                switch (docType) {
                    case PAGE:
                        try {
                            fulltext = Helper.loadFulltext(pi, browseElement.getDataRepository(),
                                    (String) childDoc.getFirstValue(SolrConstants.FILENAME_ALTO),
                                    (String) childDoc.getFirstValue(SolrConstants.FILENAME_FULLTEXT), request);
                        } catch (AccessDeniedException e) {
                            acccessDeniedType = true;
                            fulltext = ViewerResourceBundle.getTranslation(e.getMessage(), locale);
                        } catch (FileNotFoundException e) {
                            logger.error(e.getMessage());
                        } catch (IOException e) {
                            logger.error(e.getMessage(), e);
                        }

                        // Skip page hits without a proper full-text
                        if (StringUtils.isBlank(fulltext)) {
                            continue;
                        }
                    case METADATA:
                    case EVENT:
                        String ownerIddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC_OWNER);
                        SearchHit ownerHit = ownerHits.get(ownerIddoc);
                        if (ownerHit == null) {
                            SolrDocument ownerDoc = DataManager.getInstance()
                                    .getSearchIndex()
                                    .getDocumentByIddoc(ownerIddoc);
                            if (ownerDoc != null) {
                                ownerHit = createSearchHit(ownerDoc, null, locale, fulltext, searchTerms, null, false, ignoreFields, translateFields,
                                        null);
                                children.add(ownerHit);
                                ownerHits.put(ownerIddoc, ownerHit);
                                ownerDocs.put(ownerIddoc, ownerDoc);
                                // logger.trace("owner doc: {}", ownerDoc.getFieldValue("LOGID"));
                            }
                        }
                        if (ownerHit == null) {
                            logger.error("No document found for IDDOC {}", ownerIddoc);
                            continue;
                        } {
                        SearchHit childHit = createSearchHit(childDoc, ownerDocs.get(ownerIddoc), locale, fulltext, searchTerms, null, false,
                                ignoreFields, translateFields, acccessDeniedType ? HitType.ACCESSDENIED : null);
                        // Add all found additional metadata to the owner doc (minus duplicates) so it can be displayed
                        for (StringPair metadata : childHit.getFoundMetadata()) {
                            // Found metadata lists will usually be very short, so it's ok to iterate through the list on every check
                            if (!ownerHit.getFoundMetadata()
                                    .contains(metadata)) {
                                ownerHit.getFoundMetadata()
                                        .add(metadata);
                            }
                        }

                        ownerHit.getChildren()
                                .add(childHit);
                        hitsPopulated++;
                    }
                        break;
                    case DOCSTRCT:
                        // Docstruct hits are immediate children of the main hit
                        String iddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC);
                        if (!ownerHits.containsKey(iddoc)) {
                            SearchHit childHit =
                                    createSearchHit(childDoc, null, locale, fulltext, searchTerms, null, false, ignoreFields, translateFields, null);
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
     * @param ignoreFields Fields to be skipped
     * @param translateFields Fields to be translated
     * @should add field values pairs that match search terms
     * @should add MD fields that contain terms from DEFAULT
     * @should not add duplicate values
     * @should not add ignored fields
     * @should not add field values that equal the label
     * @should translate configured field values correctly
     */
    public void populateFoundMetadata(SolrDocument doc, Set<String> ignoreFields, Set<String> translateFields) {
        // logger.trace("populateFoundMetadata");
        if (searchTerms == null) {
            return;
        }

        //        boolean overviewPageFetched = false;
        for (String termsFieldName : searchTerms.keySet()) {
            // Skip fields that are in the ignore list
            if (ignoreFields != null && ignoreFields.contains(termsFieldName)) {
                continue;
            }
            switch (termsFieldName) {
                case SolrConstants.DEFAULT:
                    // If searching in DEFAULT, add all fields that contain any of the terms (instead of DEFAULT)
                    for (String docFieldName : doc.getFieldNames()) {
                        if (!(docFieldName.startsWith("MD_") || docFieldName.equals("NORM_ALTNAME"))
                                || docFieldName.endsWith(SolrConstants._UNTOKENIZED)) {
                            continue;
                        }
                        if (ignoreFields != null && ignoreFields.contains(docFieldName)) {
                            continue;
                        }
                        List<String> fieldValues = SolrSearchIndex.getMetadataValues(doc, docFieldName);
                        for (String fieldValue : fieldValues) {
                            // Skip values that are equal to the hit label
                            if (fieldValue.equals(browseElement.getLabel())) {
                                continue;
                            }
                            String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                            if (!highlightedValue.equals(fieldValue)) {
                                // Translate values for certain fields
                                if (translateFields != null && translateFields.contains(docFieldName)) {
                                    String translatedValue = Helper.getTranslation(fieldValue, locale);
                                    highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                            "$1" + translatedValue + "$3");
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                foundMetadata.add(new StringPair(Helper.getTranslation(docFieldName, locale), highlightedValue));
                                // Only add one instance of NORM_ALTNAME (as there can be dozens)
                                if ("NORM_ALTNAME".equals(docFieldName)) {
                                    break;
                                }
                                // logger.trace("found {}:{}", docFieldName, fieldValue);
                            }
                        }
                    }
                    break;
                default:
                    // Look up the exact field name in he Solr doc and add its values that contain any of the terms for that field
                    if (doc.containsKey(termsFieldName)) {
                        List<String> fieldValues = SolrSearchIndex.getMetadataValues(doc, termsFieldName);
                        for (String fieldValue : fieldValues) {
                            // Skip values that are equal to the hit label
                            if (fieldValue.equals(browseElement.getLabel())) {
                                continue;
                            }
                            String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                            if (!highlightedValue.equals(fieldValue)) {
                                // Translate values for certain fields
                                if (translateFields != null && translateFields.contains(termsFieldName)) {
                                    String translatedValue = Helper.getTranslation(fieldValue, locale);
                                    highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                            "$1" + translatedValue + "$3");
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                foundMetadata.add(new StringPair(Helper.getTranslation(termsFieldName, locale), highlightedValue));
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

    public int getMetadataHitCount() {
        if (hitTypeCounts.get(HitType.METADATA) != null) {
            return hitTypeCounts.get(HitType.METADATA);
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
        return Integer.compare(this.getBrowseElement()
                .getImageNo(),
                other.getBrowseElement()
                        .getImageNo());
    }

    /**
     * Generates HTML fragment for this search hit for notification mails.
     * 
     * @param count
     * @return
     */
    public String generateNotificationFragment(int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr><td>")
                .append(count)
                .append(".</td><td><img src=\"")
                .append(browseElement.getThumbnailUrl())
                .append("\" alt=\"")
                .append(browseElement.getLabel())
                .append("\" /></td><td>")
                .append(browseElement.getLabel())
                .append("</td></tr>");

        return sb.toString();
    }

}
