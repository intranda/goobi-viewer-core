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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.controller.SolrConstants.DocType;
import io.goobi.viewer.controller.SolrSearchIndex;
import io.goobi.viewer.controller.TEITools;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.CmsElementNotFoundException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Wrapper class for search hits. Contains the corresponding <code>BrowseElement</code>
 */
public class SearchHit implements Comparable<SearchHit> {

    public enum HitType {
        ACCESSDENIED,
        DOCSTRCT,
        PAGE,
        METADATA, // grouped metadata
        UGC, // user-generated content
        PERSON, // UGC/metadata person
        CORPORATION, // UGC/meadata corporation
        ADDRESS, // UGC address
        COMMENT, // UGC comment
        EVENT, // LIDO event
        GROUP, // convolute/series
        CMS; // CMS page type for search hits

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
                    case "CMS":
                    case "OVERVIEWPAGE":
                        return CMS;
                    case "UGC":
                        return UGC;
                    case "METADATA":
                        return METADATA;
                    case "PERSON":
                        return PERSON;
                    case "CORPORATION":
                        return CORPORATION;
                    case "ADDRESS":
                        return ADDRESS;
                    case "COMMENT":
                        return COMMENT;
                    default:
                        return null;
                }
            }

            return null;
        }

        public String getLabel(Locale locale) {
            return ViewerResourceBundle.getTranslation(new StringBuilder("doctype_").append(name()).toString(), locale);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(SearchHit.class);

    private static final String SEARCH_HIT_TYPE_PREFIX = "searchHitType_";

    private final HitType type;
    /** Translated label for the search hit type. */
    private final String translatedType;
    private final BrowseElement browseElement;
    @JsonIgnore
    private List<SolrDocument> childDocs;
    @JsonIgnore
    private final Map<String, SearchHit> ownerHits = new HashMap<>();
    @JsonIgnore
    private final Map<String, SolrDocument> ownerDocs = new HashMap<>();
    @JsonIgnore
    private final Set<String> ugcDocIddocs = new HashSet<>();
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
    @JsonIgnore
    private SolrDocument solrDoc = null;

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
        this.translatedType = type != null ? ViewerResourceBundle.getTranslation(SEARCH_HIT_TYPE_PREFIX + type.name(), locale) : null;
        this.browseElement = browseElement;
        this.searchTerms = searchTerms;
        this.locale = locale;
        if (browseElement != null) {
            // Add self to owner hits to avoid adding self to child hits
            this.ownerHits.put(Long.toString(browseElement.getIddoc()), this);
            if (searchTerms != null) {
                addLabelHighlighting();
            } else {
                String label = browseElement.getLabel(locale);
                // Escape HTML tags
                label = StringEscapeUtils.escapeHtml4(label);

                IMetadataValue labelShort = new MultiLanguageMetadataValue();
                labelShort.setValue(label, locale);
                browseElement.setLabelShort(labelShort);
            }
            this.url = browseElement.getUrl();
        } else {
            this.url = null;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public int compareTo(SearchHit other) {
        return Integer.compare(this.getBrowseElement().getImageNo(), other.getBrowseElement().getImageNo());
    }

    /**
     * <p>
     * createSearchHit.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param ownerDoc a {@link org.apache.solr.common.SolrDocument} object.
     * @param locale a {@link java.util.Locale} object.
     * @param fulltext Optional fulltext (page docs only).
     * @param searchTerms a {@link java.util.Map} object.
     * @param exportFields Optional fields for (Excel) export purposes.
     * @param sortFields
     * @param useThumbnail a boolean.
     * @param ignoreAdditionalFields a {@link java.util.Set} object.
     * @param translateAdditionalFields a {@link java.util.Set} object.
     * @param overrideType a {@link io.goobi.viewer.model.search.SearchHit.HitType} object.
     * @should add export fields correctly
     * @return a {@link io.goobi.viewer.model.search.SearchHit} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static SearchHit createSearchHit(SolrDocument doc, SolrDocument ownerDoc, Locale locale, String fulltext,
            Map<String, Set<String>> searchTerms, List<String> exportFields, List<StringPair> sortFields, boolean useThumbnail,
            Set<String> ignoreAdditionalFields,
            Set<String> translateAdditionalFields, HitType overrideType)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        List<String> fulltextFragments =
                (fulltext == null || searchTerms == null) ? null : SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.FULLTEXT), fulltext,
                        DataManager.getInstance().getConfiguration().getFulltextFragmentLength(), true, true);
        StructElement se = new StructElement(Long.valueOf((String) doc.getFieldValue(SolrConstants.IDDOC)), doc, ownerDoc);
        String docstructType = se.getDocStructType();
        if (DocType.METADATA.name().equals(se.getMetadataValue(SolrConstants.DOCTYPE))) {
            docstructType = DocType.METADATA.name();
        }

        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate(docstructType);
        BrowseElement browseElement = new BrowseElement(se, metadataList, locale,
                (fulltextFragments != null && !fulltextFragments.isEmpty()) ? fulltextFragments.get(0) : null, useThumbnail, searchTerms,
                BeanUtils.getImageDeliveryBean().getThumbs());
        // Add additional metadata fields that aren't configured for search hits but contain search term values
        browseElement.addAdditionalMetadataContainingSearchTerms(se, searchTerms, ignoreAdditionalFields, translateAdditionalFields);
        // Add sorting fields (should be added after all other metadata to avoid duplicates)
        browseElement.addSortFieldsToMetadata(se, sortFields, ignoreAdditionalFields);

        // Determine hit type
        String docType = se.getMetadataValue(SolrConstants.DOCTYPE);
        if (docType == null) {
            docType = (String) doc.getFieldValue(SolrConstants.DOCTYPE);
        }
        // logger.trace("docType: {}", docType);
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

        SearchHit hit = new SearchHit(hitType, browseElement, searchTerms, locale);
        hit.populateFoundMetadata(doc, browseElement.getExistingMetadataValues(), ignoreAdditionalFields, translateAdditionalFields);

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

        IMetadataValue labelShort = new MultiLanguageMetadataValue();
        for (Locale locale : ViewerResourceBundle.getAllLocales()) {

            String label = browseElement.getLabel(locale);

            if (searchTerms.get(SolrConstants.DEFAULT) != null) {
                label = SearchHelper.applyHighlightingToPhrase(label, searchTerms.get(SolrConstants.DEFAULT));
            } else if (searchTerms.get("MD_TITLE") != null) {
                label = SearchHelper.applyHighlightingToPhrase(label, searchTerms.get("MD_TITLE"));
            }

            // Escape HTML tags
            label = StringEscapeUtils.escapeHtml4(label);

            // Then replace highlighting placeholders with HTML tags
            label = SearchHelper.replaceHighlightingPlaceholders(label);

            labelShort.setValue(label, locale);
        }

        browseElement.setLabelShort(labelShort);
    }

    /**
     * Creates child hit elements for each hit matching a CMS page text, if CMS page texts were also searched.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void addCMSPageChildren() throws DAOException {
        if (searchTerms == null || !searchTerms.containsKey(SolrConstants.CMS_TEXT_ALL)) {
            return;
        }

        List<CMSPage> cmsPages = DataManager.getInstance().getDao().getCMSPagesForRecord(browseElement.getPi(), null);
        if (cmsPages.isEmpty()) {
            return;
        }

        SortedMap<CMSPage, List<String>> hitPages = new TreeMap<>();
        try {
            // Collect relevant texts
            for (CMSPage page : cmsPages) {
                if (page.getDefaultLanguage() == null) {
                    continue;
                }

                // Iterate over all default and global language version items
                List<CMSContentItem> items = page.getDefaultLanguage().getContentItems();
                items.addAll(page.getGlobalContentItems());
                if (items.isEmpty()) {
                    continue;
                }
                for (CMSContentItem item : items) {
                    if (item.getType() == null) {
                        continue;
                    }
                    String value = null;
                    switch (item.getType()) {
                        case HTML:
                        case TEXT:
                            if (StringUtils.isEmpty(item.getHtmlFragment())) {
                                continue;
                            }
                            value = item.getHtmlFragment();
                            break;
                        case MEDIA:
                            if (item.getMediaItem() == null || !item.getMediaItem().isHasExportableText()) {
                                continue;
                            }
                            try {
                                value = CmsMediaBean.getMediaFileAsString(item.getMediaItem());
                            } catch (ViewerConfigurationException e) {
                                logger.error(e.getMessage(), e);
                            }
                            break;
                        default:
                            continue;
                    }
                    if (StringUtils.isEmpty(value)) {
                        continue;
                    }

                    value = Jsoup.parse(value).text();
                    String highlightedValue = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(SolrConstants.CMS_TEXT_ALL));
                    if (!highlightedValue.equals(value)) {
                        List<String> truncatedStrings = hitPages.get(page);
                        if (truncatedStrings == null) {
                            truncatedStrings = new ArrayList<>();
                            hitPages.put(page, truncatedStrings);
                        }
                        truncatedStrings.addAll(SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.CMS_TEXT_ALL), highlightedValue,
                                DataManager.getInstance().getConfiguration().getFulltextFragmentLength(), false, true));

                    }
                }
            }

            // Add hits (one for each page)
            if (!hitPages.isEmpty()) {
                for (CMSPage page : hitPages.keySet()) {
                    int count = 0;
                    SearchHit cmsPageHit = new SearchHit(HitType.CMS, new BrowseElement(browseElement.getPi(), 1,
                            ViewerResourceBundle.getTranslation(page.getMenuTitle(), locale), null, locale, null, page.getRelativeUrlPath()),
                            searchTerms, locale);
                    children.add(cmsPageHit);
                    for (String text : hitPages.get(page)) {
                        cmsPageHit.getChildren()
                                .add(new SearchHit(HitType.CMS, new BrowseElement(browseElement.getPi(), 1, page.getMenuTitle(), text, locale, null,
                                        page.getRelativeUrlPath()), searchTerms, locale));
                        count++;
                    }
                    hitTypeCounts.put(HitType.CMS, count);
                    logger.trace("Added {} CMS page child hits", count);
                }
            }
        } catch (CmsElementNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Creates a child hit element for TEI full-texts, with child hits of its own for each truncated fragment containing search terms.
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param language a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void addFulltextChild(SolrDocument doc, String language) throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }

        if (searchTerms == null) {
            return;
        }
        if (!searchTerms.containsKey(SolrConstants.FULLTEXT)) {
            return;
        }

        if (language == null) {
            language = "en";
        }

        // Check whether TEI is available at all
        String teiFilename = (String) doc.getFirstValue(SolrConstants.FILENAME_TEI + SolrConstants._LANG_ + language.toUpperCase());
        if (StringUtils.isEmpty(teiFilename)) {
            teiFilename = (String) doc.getFirstValue(SolrConstants.FILENAME_TEI);
        }
        if (StringUtils.isEmpty(teiFilename)) {
            return;
        }

        try {
            String fulltext = DataFileTools.loadTei((String) doc.getFieldValue(SolrConstants.PI), language);
            if (fulltext != null) {
                fulltext = TEITools.getTeiFulltext(fulltext);
                fulltext = Jsoup.parse(fulltext).text();
            }
            // logger.trace(fulltext);
            List<String> fulltextFragments = fulltext == null ? null : SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.FULLTEXT),
                    fulltext, DataManager.getInstance().getConfiguration().getFulltextFragmentLength(), false, false);

            int count = 0;
            if (fulltextFragments != null && !fulltextFragments.isEmpty()) {
                SearchHit hit = new SearchHit(HitType.PAGE,
                        new BrowseElement(browseElement.getPi(), 1, ViewerResourceBundle.getTranslation("TEI", locale), null, locale, null, null),
                        searchTerms,
                        locale);
                for (String fragment : fulltextFragments) {
                    hit.getChildren()
                            .add(new SearchHit(HitType.PAGE, new BrowseElement(browseElement.getPi(), 1, "TEI", fragment, locale, null, null),
                                    searchTerms, locale));
                    count++;
                }
                children.add(hit);
                // logger.trace("Added {} fragments", count);
                int oldCount = hit.getHitTypeCounts().get(HitType.PAGE) != null ? hit.getHitTypeCounts().get(HitType.PAGE) : 0;
                hitTypeCounts.put(HitType.PAGE, oldCount + count);
            }
        } catch (AccessDeniedException e) {
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (JDOMException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * <p>
     * populateChildren.
     * </p>
     *
     * @param number a int.
     * @param locale a {@link java.util.Locale} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void populateChildren(int number, Locale locale, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        populateChildren(number, 0, locale, request);
    }

    /**
     * <p>
     * populateChildren.
     * </p>
     *
     * @param number a int.
     * @param locale a {@link java.util.Locale} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param skip a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void populateChildren(int number, int skip, Locale locale, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("populateChildren START");

        // Create child hits
        String pi = browseElement.getPi();
        if (pi != null && childDocs != null) {
            logger.trace("{} child hits found for {}", childDocs.size(), pi);
            if (number + skip > childDocs.size()) {
                number = childDocs.size() - skip;
            }
            Set<String> ignoreFields = new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataIgnoreFields());
            Set<String> translateFields = new HashSet<>(DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataTranslateFields());
            List<SolrDocument> ugcDocs = null;
            for (int i = 0; i < number; ++i) {
                SolrDocument childDoc = childDocs.get(i + skip);
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
                            fulltext = DataFileTools.loadFulltext(browseElement.getDataRepository(),
                                    (String) childDoc.getFirstValue(SolrConstants.FILENAME_ALTO),
                                    (String) childDoc.getFirstValue(SolrConstants.FILENAME_FULLTEXT), true, request);
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
                    case UGC:
                    case EVENT: {
                        String ownerIddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC_OWNER);
                        SearchHit ownerHit = ownerHits.get(ownerIddoc);
                        boolean populateHit = false;
                        if (ownerHit == null) {
                            SolrDocument ownerDoc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc(ownerIddoc);
                            if (ownerDoc != null) {
                                ownerHit = createSearchHit(ownerDoc, null, locale, fulltext, searchTerms, null, null, false, ignoreFields,
                                        translateFields,
                                        null);
                                children.add(ownerHit);
                                ownerHits.put(ownerIddoc, ownerHit);
                                ownerDocs.put(ownerIddoc, ownerDoc);
                                populateHit = true;
                                // logger.trace("owner doc: {}", ownerDoc.getFieldValue("LOGID"));
                            }
                        }
                        if (ownerHit == null) {
                            logger.error("No document found for IDDOC {}", ownerIddoc);
                            continue;
                        }
                        {
                            {
                                SearchHit childHit =
                                        createSearchHit(childDoc, ownerDocs.get(ownerIddoc), locale, fulltext, searchTerms, null, null, false,
                                                ignoreFields, translateFields, acccessDeniedType ? HitType.ACCESSDENIED : null);
                                if (!DocType.UGC.equals(docType)) {
                                    // Add all found additional metadata to the owner doc (minus duplicates) so it can be displayed
                                    for (StringPair metadata : childHit.getFoundMetadata()) {
                                        // Found metadata lists will usually be very short, so it's ok to iterate through the list on every check
                                        if (!ownerHit.getFoundMetadata().contains(metadata)) {
                                            ownerHit.getFoundMetadata().add(metadata);
                                        }
                                    }
                                }
                                //                                if (!(DocType.METADATA.equals(docType))) {
                                ownerHit.getChildren().add(childHit);
                                populateHit = true;
                                //                                }
                                if (populateHit) {
                                    hitsPopulated++;
                                }
                            }
                        }
                    }
                        break;
                    case DOCSTRCT:
                        // Docstruct hits are immediate children of the main hit
                        String iddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC);
                        if (!ownerHits.containsKey(iddoc)) {
                            SearchHit childHit =
                                    createSearchHit(childDoc, null, locale, fulltext, searchTerms, null, null, false, ignoreFields, translateFields,
                                            null);
                            children.add(childHit);
                            ownerHits.put(iddoc, childHit);
                            ownerDocs.put(iddoc, childDoc);
                            hitsPopulated++;
                        }
                        break;
                    case GROUP:
                    default:
                        break;
                }
            }

            //            childDocs = childDocs.subList(number, childDocs.size());
            if (childDocs.isEmpty()) {
                ownerDocs.clear();
                ownerHits.clear();
            }
            logger.trace("Remaning child docs: {}", childDocs.size());
        }
        logger.trace("populateChildren END");
    }

    /**
     * <p>
     * populateFoundMetadata.
     * </p>
     *
     * @param doc a {@link org.apache.solr.common.SolrDocument} object.
     * @param ownerMetadataValues List of metadata field+value combos that the owner already has
     * @param ignoreFields Fields to be skipped
     * @param translateFields Fields to be translated
     * @should add field values pairs that match search terms
     * @should add MD fields that contain terms from DEFAULT
     * @should not add duplicate values
     * @should not add ignored fields
     * @should not add field values that equal the label
     * @should translate configured field values correctly
     */
    public void populateFoundMetadata(SolrDocument doc, Set<String> ownerMetadataValues, Set<String> ignoreFields, Set<String> translateFields) {
        logger.trace("populateFoundMetadata: {}", searchTerms);
        if (searchTerms == null) {
            return;
        }

        for (String termsFieldName : searchTerms.keySet()) {
            // Skip fields that are in the ignore list
            if (ignoreFields != null && ignoreFields.contains(termsFieldName)) {
                continue;
            }
            switch (termsFieldName) {
                case SolrConstants.DEFAULT:
                case SolrConstants.NORMDATATERMS:
                    // If searching in DEFAULT, add all fields that contain any of the terms (instead of DEFAULT)
                    for (String docFieldName : doc.getFieldNames()) {
                        if (!(docFieldName.startsWith("MD_") || docFieldName.startsWith("NORM_"))
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
                            if (ownerMetadataValues.contains(docFieldName + ":" + fieldValue)) {
                                continue;
                            }
                            String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                            if (!highlightedValue.equals(fieldValue)) {
                                // Translate values for certain fields, keeping the highlighting
                                if (translateFields != null && (translateFields.contains(termsFieldName)
                                        || translateFields.contains(SearchHelper.adaptField(termsFieldName, null)))) {
                                    String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                    highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                            "$1" + translatedValue + "$3");
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                foundMetadata.add(new StringPair(ViewerResourceBundle.getTranslation(docFieldName, locale), highlightedValue));
                                // Only add one instance of NORM_ALTNAME (as there can be dozens)
                                if ("NORM_ALTNAME".equals(docFieldName)) {
                                    break;
                                }
                                logger.trace("found metadata: {}:{}", docFieldName, fieldValue);
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
                            if (ownerMetadataValues.contains(termsFieldName + ":" + fieldValue)) {
                                continue;
                            }
                            String highlightedValue = SearchHelper.applyHighlightingToPhrase(fieldValue, searchTerms.get(termsFieldName));
                            if (!highlightedValue.equals(fieldValue)) {
                                // Translate values for certain fields, keeping the highlighting
                                if (translateFields != null && (translateFields.contains(termsFieldName)
                                        || translateFields.contains(SearchHelper.adaptField(termsFieldName, null)))) {
                                    String translatedValue = ViewerResourceBundle.getTranslation(fieldValue, locale);
                                    highlightedValue = highlightedValue.replaceAll("(\\W)(" + Pattern.quote(fieldValue) + ")(\\W)",
                                            "$1" + translatedValue + "$3");
                                }
                                highlightedValue = SearchHelper.replaceHighlightingPlaceholders(highlightedValue);
                                foundMetadata.add(new StringPair(ViewerResourceBundle.getTranslation(termsFieldName, locale), highlightedValue));
                            }
                        }
                    }
                    break;
            }

        }
    }

    /**
     * 
     * @param pi
     * @param order
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    @Deprecated
    List<SolrDocument> getUgcDocsForPage(String pi, int order) throws PresentationException, IndexUnreachableException {
        String ugcQuery = new StringBuilder().append(SolrConstants.DOCTYPE)
                .append(':')
                .append(DocType.UGC.name())
                .append(" AND ")
                .append(SolrConstants.PI_TOPSTRUCT)
                .append(':')
                .append(pi)
                .append(" AND ")
                .append(SolrConstants.ORDER)
                .append(':')
                .append(order)
                .toString();
        logger.trace("ugc query: {}", ugcQuery);
        SolrDocumentList ugcDocList = DataManager.getInstance().getSearchIndex().search(ugcQuery);
        if (!ugcDocList.isEmpty()) {
            List<SolrDocument> ret = new ArrayList<>(ugcDocList.size());
            for (SolrDocument doc : ugcDocList) {
                boolean added = false;
                for (String field : doc.getFieldNames()) {
                    if (added) {
                        break;
                    }
                    String value = SolrSearchIndex.getSingleFieldStringValue(doc, field);
                    if (value != null) {
                        for (String term : searchTerms.get(SolrConstants.UGCTERMS)) {
                            if (value.toLowerCase().contains(term)) {
                                ret.add(doc);
                                added = true;
                                break;
                            }
                        }
                    }
                }
            }
            logger.trace("Found {} UGC documents for page {}", ret.size(), order);
            return ret;
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     *
     * @return the type
     */
    public HitType getType() {
        return type;
    }

    /**
     * <p>
     * Getter for the field <code>translatedType</code>.
     * </p>
     *
     * @return the translatedType
     */
    public String getTranslatedType() {
        return translatedType;
    }

    /**
     * <p>
     * Getter for the field <code>browseElement</code>.
     * </p>
     *
     * @return the browseElement
     */
    public BrowseElement getBrowseElement() {
        return browseElement;
    }

    /**
     * <p>
     * Getter for the field <code>childDocs</code>.
     * </p>
     *
     * @return the childDocs
     */
    public List<SolrDocument> getChildDocs() {
        return childDocs;
    }

    /**
     * <p>
     * Getter for the field <code>hitsPopulated</code>.
     * </p>
     *
     * @return the hitsPopulated
     */
    public int getHitsPopulated() {
        return hitsPopulated;
    };

    /**
     * <p>
     * Setter for the field <code>childDocs</code>.
     * </p>
     *
     * @param childDocs the childDocs to set
     */
    public void setChildDocs(SolrDocumentList childDocs) {
        this.childDocs = childDocs;
    }

    /**
     * Returns true if this hit has populated child elements.
     *
     * @return a boolean.
     */
    public boolean isHasChildren() {
        return children != null && !children.isEmpty();
    }

    /**
     * Returns true if this hit has any unpopulated child hits left.
     *
     * @return a boolean.
     */
    public boolean isHasMoreChildren() {
        return childDocs != null && !childDocs.isEmpty() && getHitsPopulated() < childDocs.size();
    }

    /**
     * <p>
     * Getter for the field <code>ugcDocIddocs</code>.
     * </p>
     *
     * @return the ugcDocIddocs
     */
    public Set<String> getUgcDocIddocs() {
        return ugcDocIddocs;
    }

    /**
     * <p>
     * Getter for the field <code>children</code>.
     * </p>
     *
     * @return the children
     */
    public List<SearchHit> getChildren() {
        return children;
    }

    /**
     * <p>
     * Getter for the field <code>hitTypeCounts</code>.
     * </p>
     *
     * @return the hitTypeCounts
     */
    public Map<HitType, Integer> getHitTypeCounts() {
        return hitTypeCounts;
    }

    /**
     * <p>
     * isHasHitCount.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasHitCount() {
        for (HitType key : hitTypeCounts.keySet()) {
            if (hitTypeCounts.get(key) > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * getCmsPageHitCount.
     * </p>
     *
     * @return a int.
     */
    public int getCmsPageHitCount() {
        if (hitTypeCounts.get(HitType.CMS) != null) {
            return hitTypeCounts.get(HitType.CMS);
        }

        return 0;
    }

    /**
     * <p>
     * getDocstructHitCount.
     * </p>
     *
     * @return a int.
     */
    public int getDocstructHitCount() {
        if (hitTypeCounts.get(HitType.DOCSTRCT) != null) {
            return hitTypeCounts.get(HitType.DOCSTRCT);
        }

        return 0;
    }

    /**
     * <p>
     * getPageHitCount.
     * </p>
     *
     * @return a int.
     */
    public int getPageHitCount() {
        if (hitTypeCounts.get(HitType.PAGE) != null) {
            return hitTypeCounts.get(HitType.PAGE);
        }

        return 0;
    }

    /**
     * <p>
     * getMetadataHitCount.
     * </p>
     *
     * @return a int.
     */
    public int getMetadataHitCount() {
        if (hitTypeCounts.get(HitType.METADATA) != null) {
            return hitTypeCounts.get(HitType.METADATA);
        }

        return 0;
    }

    /**
     * <p>
     * getEventHitCount.
     * </p>
     *
     * @return a int.
     */
    public int getEventHitCount() {
        if (hitTypeCounts.get(HitType.EVENT) != null) {
            return hitTypeCounts.get(HitType.EVENT);
        }

        return 0;
    }

    /**
     * <p>
     * getUgcHitCount.
     * </p>
     *
     * @return a int.
     */
    public int getUgcHitCount() {
        if (hitTypeCounts.get(HitType.UGC) != null) {
            return hitTypeCounts.get(HitType.UGC);
        }

        return 0;
    }

    /**
     * <p>
     * Getter for the field <code>foundMetadata</code>.
     * </p>
     *
     * @return the foundMetadata
     */
    public List<StringPair> getFoundMetadata() {
        return foundMetadata;
    }

    /**
     * <p>
     * Getter for the field <code>url</code>.
     * </p>
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>
     * Getter for the field <code>exportMetadata</code>.
     * </p>
     *
     * @return the exportMetadata
     */
    public Map<String, String> getExportMetadata() {
        return exportMetadata;
    }

    /**
     * Generates HTML fragment for this search hit for notification mails.
     *
     * @param count a int.
     * @return a {@link java.lang.String} object.
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

    /**
     * @param doc
     */
    public void setSolrDoc(SolrDocument doc) {
        this.solrDoc = doc;
    }

    public SolrDocument getSolrDoc() {
        return this.solrDoc;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getBrowseElement().getLabelShort();
    }
}
