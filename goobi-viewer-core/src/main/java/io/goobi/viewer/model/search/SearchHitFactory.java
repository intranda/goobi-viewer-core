package io.goobi.viewer.model.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataWrapper;
import io.goobi.viewer.model.search.SearchHit.HitType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;

public class SearchHitFactory {

    private static final Logger logger = LogManager.getLogger(SearchHitFactory.class);

    private Map<String, Set<String>> searchTerms;
    private String additionalMetadataListType;
    private List<String> exportFields;
    private List<StringPair> sortFields;
    private Set<String> additionalMetadataIgnoreFields =
            new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataIgnoreFields());
    private Set<String> additionalMetadataTranslateFields =
            new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataTranslateFields());
    private Set<String> additionalMetadataOneLineFields =
            new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataOnelineFields());
    private Set<String> additionalMetadataSnippetFields =
            new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataSnippetFields());
    private int proximitySearchDistance;

    private ThumbnailHandler thumbnailHandler;
    private Locale locale;

    /**
     * 
     * @param searchTerms
     * @param sortFields
     * @param exportFields
     * @param proximitySearchDistance
     * @param thumbnailHandler
     * @param locale
     */
    public SearchHitFactory(Map<String, Set<String>> searchTerms, List<StringPair> sortFields, List<String> exportFields, int proximitySearchDistance,
            ThumbnailHandler thumbnailHandler, Locale locale) {
        this.searchTerms = searchTerms;
        this.sortFields = sortFields;
        this.exportFields = exportFields;
        this.proximitySearchDistance = proximitySearchDistance;
        this.thumbnailHandler = thumbnailHandler;
        this.locale = locale;
    }

    /**
     * <p>
     * createSearchHit.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param ownerDoc a {@link org.apache.solr.common.SolrDocument} object.
     * @param fulltext Optional fulltext (page docs only).
     * @param overrideType a {@link io.goobi.viewer.model.search.SearchHit.HitType} object.
     * @return a {@link io.goobi.viewer.model.search.SearchHit} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @should add export fields correctly
     */
    public SearchHit createSearchHit(SolrDocument doc, SolrDocument ownerDoc, String fulltext,
            HitType overrideType) throws PresentationException, IndexUnreachableException {
        List<String> fulltextFragments =
                (fulltext == null || searchTerms == null) ? null : SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.FULLTEXT), fulltext,
                        DataManager.getInstance().getConfiguration().getFulltextFragmentLength(), true, true, proximitySearchDistance);
        StructElement se = new StructElement(Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC)), doc, ownerDoc);
        String docstructType = se.getDocStructType();
        if (DocType.METADATA.name().equals(se.getMetadataValue(SolrConstants.DOCTYPE))) {
            docstructType = DocType.METADATA.name();
        }

        Map<String, List<String>> searchedFields = new HashMap<>(se.getMetadataFields());
        searchedFields.put(SolrConstants.FULLTEXT, Collections.singletonList(fulltext));
        Map<String, Set<String>> cleanedUpSearchTerms = getActualSearchTerms(searchTerms, searchedFields);

        Map<String, List<Metadata>> metadataListMap = new HashMap<>();
        List<Metadata> metadataList =
                DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate(docstructType);
        metadataListMap.put(Configuration.METADATA_LIST_TYPE_SEARCH_HIT, metadataList);

        // If an additional metadata list type is provided, add a second metadata list
        if (StringUtils.isNotBlank(additionalMetadataListType)) {
            List<Metadata> altMetadataList =
                    DataManager.getInstance()
                            .getConfiguration()
                            .getMetadataConfigurationForTemplate(additionalMetadataListType, docstructType, true, true);
            metadataListMap.put(additionalMetadataListType, altMetadataList);
        }

        BrowseElement browseElement = new BrowseElement(se, metadataListMap, locale,
                (fulltextFragments != null && !fulltextFragments.isEmpty()) ? fulltextFragments.get(0) : null, cleanedUpSearchTerms,
                thumbnailHandler);

        // Add additional metadata fields that aren't configured for search hits but contain search term values
        if (DataManager.getInstance().getConfiguration().isDisplayAdditionalMetadataEnabled()) {
            //            browseElement.addAdditionalMetadataContainingSearchTerms(se, foundSearchTerms, additionalMetadataIgnoreFields,
            //                    additionalMetadataTranslateFields, additionalMetadataOneLineFields, additionalMetadataSnippetFields, proximitySearchDistance);

            Optional<String> labelValue = browseElement.getLabelAsMetadataValue().getValue();
            List<MetadataWrapper> additionalMetadata = findAdditionalMetadataFieldsContainingSearchTerms(metadataList,
                    se.getMetadataFields(),
                    String.valueOf(browseElement.getIddoc()),
                    labelValue.isPresent() ? labelValue.get() : "",
                    cleanedUpSearchTerms);
            if (!additionalMetadata.isEmpty()) {
                for (MetadataWrapper mw : additionalMetadata) {
                    browseElement.getMetadataList().add(mw.getMetadata());
                }
            }
        }

        // Add sorting fields (should be added after all other metadata to avoid duplicates)
        browseElement.addSortFieldsToMetadata(se, sortFields, additionalMetadataIgnoreFields);

        // Determine hit type
        String docType = se.getMetadataValue(SolrConstants.DOCTYPE);
        if (docType == null) {
            docType = (String) doc.getFieldValue(SolrConstants.DOCTYPE);
        }
        // logger.trace("docType: {}", docType); //NOSONAR Sometimes used for debugging
        HitType hitType = overrideType;
        if (hitType == null) {
            hitType = HitType.getByName(docType);
            if (DocType.METADATA.name().equals(docType)) {
                // For metadata hits use the metadata type for the hit type
                String metadataType = se.getMetadataValue(SolrConstants.METADATATYPE);
                if (StringUtils.isNotEmpty(metadataType)) {
                    hitType = HitType.getByName(metadataType);
                }
            } else if (DocType.UGC.name().equals(docType)) {
                // For user-generated content hits use the metadata type for the hit type
                String ugcType = se.getMetadataValue(SolrConstants.UGCTYPE);
                logger.trace("ugcType: {}", ugcType);
                if (StringUtils.isNotEmpty(ugcType)) {
                    hitType = HitType.getByName(ugcType);
                    logger.trace("hit type found: {}", hitType);
                }
            }
        }

        SearchHit hit = new SearchHit(hitType, browseElement, doc, searchTerms, locale, this);
        //        hit.populateFoundMetadata(doc, ownerAlreadyHasMetadata, additionalMetadataIgnoreFields, additionalMetadataTranslateFields,
        //                additionalMetadataOneLineFields, additionalMetadataSnippetFields);

        Optional<String> labelValue = browseElement.getLabelAsMetadataValue().getValue();
        List<MetadataWrapper> additionalMetadata = findAdditionalMetadataFieldsContainingSearchTerms(metadataList,
                se.getMetadataFields(),
                String.valueOf(browseElement.getIddoc()),
                labelValue.isPresent() ? labelValue.get() : "",
                cleanedUpSearchTerms);
        if (!additionalMetadata.isEmpty()) {
            for (MetadataWrapper mw : additionalMetadata) {
                hit.getFoundMetadata().add(mw.getValuePair());
            }
        }

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
     * @param additionalMetadataListType the additionalMetadataListType to set
     * @return this
     */
    public SearchHitFactory setAdditionalMetadataListType(String additionalMetadataListType) {
        this.additionalMetadataListType = additionalMetadataListType;
        return this;
    }

    /**
     * replaces any terms with a fuzzy search token with the matching strings found in the values of fields
     *
     * @param origTerms
     * @param fields
     * @return
     */
    private static Map<String, Set<String>> getActualSearchTerms(Map<String, Set<String>> origTerms, Map<String, List<String>> resultFields) {
        Map<String, Set<String>> newFieldTerms = new HashMap<>();
        if (origTerms == null) {
            return newFieldTerms;
        }

        String foundValues = resultFields.values().stream().flatMap(Collection::stream).collect(Collectors.joining(" "));
        for (Entry<String, Set<String>> entry : origTerms.entrySet()) {
            Set<String> newTerms = new HashSet<>();
            Set<String> terms = entry.getValue();
            for (String term : terms) {
                term = term.replaceAll("(^\\()|(\\)$)", "");
                term = StringTools.removeDiacriticalMarks(term);
                if (FuzzySearchTerm.isFuzzyTerm(term)) {
                    FuzzySearchTerm fuzzy = new FuzzySearchTerm(term);
                    Matcher m = Pattern.compile(FuzzySearchTerm.WORD_PATTERN).matcher(foundValues);
                    while (m.find()) {
                        String word = m.group();
                        if (fuzzy.matches(word)) {
                            newTerms.add(word);
                        }
                    }
                } else {
                    newTerms.add(term);
                }
            }
            newFieldTerms.put(entry.getKey(), newTerms);
        }

        return newFieldTerms;
    }

    /**
     * 
     * @param existingMetadataList
     * @param availableMetadata
     * @param iddoc
     * @param searchHitLabel
     * @param searchTerms
     * @param ignoreFields
     * @param translateFields
     * @param oneLineFields
     * @param snippetFields
     * @param proximitySearchDistance
     * @param locale
     * @return
     * @should add metadata fields that match search terms
     * @should not add duplicates from default terms
     * @should not add duplicates from explicit terms
     * @should not add ignored fields
     * @should translate configured field values correctly
     * @should write one line fields into a single string
     * @should truncate snippet fields correctly
     */
    List<MetadataWrapper> findAdditionalMetadataFieldsContainingSearchTerms(List<Metadata> existingMetadataList,
            Map<String, List<String>> availableMetadata, String iddoc, String searchHitLabel, Map<String, Set<String>> searchTerms) {
        // logger.trace("findAdditionalMetadataFieldsContainingSearchTerms");
        if (existingMetadataList == null) {
            throw new IllegalArgumentException("existingMetadataList may not be null");
        }
        if (availableMetadata == null) {
            throw new IllegalArgumentException("availableMetadata may not be null");
        }
        if (searchTerms == null) {
            return Collections.emptyList();
        }

        List<MetadataWrapper> ret = new ArrayList<>();
        for (Entry<String, Set<String>> entry : searchTerms.entrySet()) {
            // Skip fields that are in the ignore list
            if (additionalMetadataIgnoreFields != null && additionalMetadataIgnoreFields.contains(entry.getKey())) {
                continue;
            }
            // Skip fields that are already in the list
            boolean skip = false;
            for (Metadata md : existingMetadataList) {
                if (md.getLabel().equals(entry.getKey())) {
                    skip = true;
                    break;
                }
            }
            if (skip) {
                continue;
            }
            switch (entry.getKey()) {
                case SolrConstants.DEFAULT:
                case SolrConstants.NORMDATATERMS:
                    // If searching in DEFAULT, add all fields that contain any of the terms (instead of DEFAULT)
                    for (String docFieldName : availableMetadata.keySet()) {
                        // Skip fields that are in the ignore list
                        if (additionalMetadataIgnoreFields != null && additionalMetadataIgnoreFields.contains(docFieldName)) {
                            continue;
                        }
                        if (!docFieldName.startsWith("MD_") || docFieldName.endsWith(SolrConstants.SUFFIX_UNTOKENIZED)) {
                            continue;
                        }
                        // Skip fields that are already in the list
                        for (Metadata md : existingMetadataList) {
                            if (md.getLabel().equals(docFieldName)) {
                                skip = true;
                                break;
                            }
                        }
                        if (skip) {
                            skip = false;
                            continue;
                        }
                        List<String> fieldValues = availableMetadata.get(docFieldName);

                        if (additionalMetadataOneLineFields != null && additionalMetadataOneLineFields.contains(docFieldName)) {
                            // All values into a single field value
                            StringBuilder sb = new StringBuilder();
                            for (String fieldValue : fieldValues) {
                                // Skip values that are equal to the hit label
                                if (StringUtils.isNotEmpty(searchHitLabel) && fieldValue.equals(searchHitLabel)) {
                                    continue;
                                }
                                String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, entry.getValue());
                                if (!highlightedValue.equals(fieldValue)) {
                                    // Translate values for certain fields, keeping the highlighting
                                    if (additionalMetadataTranslateFields != null && (additionalMetadataTranslateFields.contains(docFieldName)
                                            || additionalMetadataTranslateFields.contains(SearchHelper.adaptField(docFieldName, null)))) {
                                        String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                        highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                                "$1" + translatedValue + "$3");
                                    }
                                    highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                    if (sb.length() > 0) {
                                        sb.append(", ");
                                    }
                                    sb.append(highlightedValue);
                                }
                            }
                            if (sb.length() > 0) {
                                ret.add(new MetadataWrapper().setMetadata(new Metadata(iddoc, docFieldName, "", sb.toString()))
                                        .setValuePair(new StringPair(ViewerResourceBundle.getTranslation(docFieldName, locale), sb.toString())));
                                // existingMetadataFields.add(docFieldName);
                                logger.trace("added existing field: {}", docFieldName);
                            }
                        } else {
                            for (String fieldValue : fieldValues) {
                                // Skip values that are equal to the hit label
                                if (StringUtils.isNotEmpty(searchHitLabel) && fieldValue.equals(searchHitLabel)) {
                                    continue;
                                }

                                String highlightedValue = null;

                                // Truncate snippet field values
                                if (additionalMetadataSnippetFields != null && additionalMetadataSnippetFields.contains(docFieldName)) {
                                    List<String> truncatedValues =
                                            SearchHelper.truncateFulltext(entry.getValue(), fieldValue,
                                                    DataManager.getInstance().getConfiguration().getFulltextFragmentLength(), false, false,
                                                    proximitySearchDistance);
                                    if (!truncatedValues.isEmpty()) {
                                        highlightedValue = "[...] " + truncatedValues.get(0).trim() + " [...]";
                                    }
                                }

                                if (highlightedValue == null) {
                                    highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, entry.getValue());
                                }
                                if (!highlightedValue.equals(fieldValue)) {
                                    // Translate values for certain fields, keeping the highlighting
                                    if (additionalMetadataTranslateFields != null && (additionalMetadataTranslateFields.contains(entry.getKey())
                                            || additionalMetadataTranslateFields.contains(SearchHelper.adaptField(entry.getKey(), null)))) {
                                        String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                        highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                                "$1" + translatedValue + "$3");
                                    }
                                    highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                    ret.add(new MetadataWrapper().setMetadata(new Metadata(iddoc, docFieldName, "", highlightedValue))
                                            .setValuePair(
                                                    new StringPair(ViewerResourceBundle.getTranslation(docFieldName, locale), highlightedValue)));
                                    // existingMetadataFields.add(docFieldName);
                                    logger.trace("added existing field: {}", docFieldName);
                                }
                            }
                        }
                    }
                    break;
                default:
                    // Skip fields that are already in the list
                    for (Metadata md : existingMetadataList) {
                        if (md.getLabel().equals(entry.getKey())) {
                            skip = true;
                            break;
                        }
                    }
                    // Look up the exact field name in the Solr doc and add its values that contain any of the terms for that field
                    if (!skip && availableMetadata.containsKey(entry.getKey())) {
                        List<String> fieldValues = availableMetadata.get(entry.getKey());
                        if (additionalMetadataOneLineFields != null && additionalMetadataOneLineFields.contains(entry.getKey())) {
                            // All values into a single field value
                            StringBuilder sb = new StringBuilder();
                            for (String fieldValue : fieldValues) {
                                String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, entry.getValue());
                                if (!highlightedValue.equals(fieldValue)) {
                                    // Translate values for certain fields, keeping the highlighting
                                    if (additionalMetadataTranslateFields != null && (additionalMetadataTranslateFields.contains(entry.getKey())
                                            || additionalMetadataTranslateFields.contains(SearchHelper.adaptField(entry.getKey(), null)))) {
                                        String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                        highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                                "$1" + translatedValue + "$3");
                                    }
                                    highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                    if (sb.length() > 0) {
                                        sb.append(", ");
                                    }
                                    sb.append(highlightedValue);
                                }
                            }
                            if (sb.length() > 0) {
                                ret.add(new MetadataWrapper()
                                        .setMetadata(new Metadata(iddoc, entry.getKey(), "", sb.toString()))
                                        .setValuePair(new StringPair(ViewerResourceBundle.getTranslation(entry.getKey(), locale), sb.toString())));
                                // existingMetadataFields.add(entry.getKey());
                            }
                        } else {
                            for (String fieldValue : fieldValues) {
                                String highlightedValue = null;

                                // Truncate snippet field values
                                if (additionalMetadataSnippetFields != null && additionalMetadataSnippetFields.contains(entry.getKey())) {
                                    List<String> truncatedValues =
                                            SearchHelper.truncateFulltext(entry.getValue(), fieldValue,
                                                    DataManager.getInstance().getConfiguration().getFulltextFragmentLength(), false, false,
                                                    proximitySearchDistance);
                                    if (!truncatedValues.isEmpty()) {
                                        highlightedValue = truncatedValues.get(0).trim();
                                    }
                                }

                                if (highlightedValue == null) {
                                    highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, entry.getValue());
                                }
                                if (!highlightedValue.equals(fieldValue)) {
                                    // Translate values for certain fields, keeping the highlighting
                                    if (additionalMetadataTranslateFields != null && (additionalMetadataTranslateFields.contains(entry.getKey())
                                            || additionalMetadataTranslateFields.contains(SearchHelper.adaptField(entry.getKey(), null)))) {
                                        String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                        highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                                "$1" + translatedValue + "$3");
                                    }
                                    highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                    ret.add(new MetadataWrapper().setMetadata(new Metadata(iddoc, entry.getKey(), "", highlightedValue))
                                            .setValuePair(
                                                    new StringPair(ViewerResourceBundle.getTranslation(entry.getKey(), locale), highlightedValue)));
                                    // existingMetadataFields.add(entry.getKey());
                                }
                            }
                        }
                    }
                    break;
            }
        }

        return ret;
    }

    /**
     * @return the additionalMetadataIgnoreFields
     */
    public Set<String> getAdditionalMetadataIgnoreFields() {
        return additionalMetadataIgnoreFields;
    }

    /**
     * @return the additionalMetadataTranslateFields
     */
    public Set<String> getAdditionalMetadataTranslateFields() {
        return additionalMetadataTranslateFields;
    }

    /**
     * @return the additionalMetadataOneLineFields
     */
    public Set<String> getAdditionalMetadataOneLineFields() {
        return

        additionalMetadataOneLineFields;
    }

    /**
     * @return the additionalMetadataSnippetFields
     */
    public Set<String> getAdditionalMetadataSnippetFields() {
        return additionalMetadataSnippetFields;
    }

    /**
     * @param additionalMetadataOneLineFields the additionalMetadataOneLineFields to set
     */
    public void setAdditionalMetadataOneLineFields(Set<String> additionalMetadataOneLineFields) {
        this.additionalMetadataOneLineFields = additionalMetadataOneLineFields;
    }
}
