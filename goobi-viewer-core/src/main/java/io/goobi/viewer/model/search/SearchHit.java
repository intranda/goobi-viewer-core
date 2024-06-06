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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.JDOMException;
import org.jsoup.Jsoup;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.api.rest.model.ner.TagCount;
import io.goobi.viewer.controller.ALTOTools;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.TEITools;
import io.goobi.viewer.exceptions.AccessDeniedException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.archives.SolrEADParser;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.pages.content.TranslatableCMSContent;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.DocType;
import io.goobi.viewer.solr.SolrTools;

/**
 * Wrapper class for search hits. Contains the corresponding <code>BrowseElement</code>
 */
public class SearchHit implements Comparable<SearchHit> {

    private static final Logger logger = LogManager.getLogger(SearchHit.class);

    private static final String SEARCH_HIT_TYPE_PREFIX = "searchHitType_";

    private final HitType type;
    /** Translated label for the search hit type. */
    private final BrowseElement browseElement;
    /** Number of this hit in the current hit list. */
    private long hitNumber = 1;
    private List<SolrDocument> childDocs;
    private final Map<String, SearchHit> ownerHits = new HashMap<>();
    private final Map<String, SolrDocument> ownerDocs = new HashMap<>();
    private final Set<String> ugcDocIddocs = new HashSet<>();
    private final Map<String, Set<String>> searchTerms;
    /** Docstruct metadata that matches the search terms. */
    private final List<StringPair> foundMetadata = new ArrayList<>();
    /** Metadata for Excel export. */
    private final Map<String, String> exportMetadata = new HashMap<>();
    private String url;
    /** Secondary URL */
    private String altUrl;
    /** Secondary label. */
    private String altLabel;
    @JsonIgnore
    private final Locale locale;
    private final List<SearchHit> children = new ArrayList<>();
    private final Map<HitType, Integer> hitTypeCounts = new EnumMap<>(HitType.class);
    /**
     * Hits generated from {@link #childDocs} in {@link #populateChildren(int, int, Locale, HttpServletRequest)}
     */
    private int hitsPopulated = 0;
    /**
     * Hits generated when hit is created in
     * {@link SearchHelper#searchWithAggregation(String, int, int, List, List, List, Map, Map, List, String, Locale, boolean, int)} This hits are part
     * of the total hit count returned by {@link #getHitCount()} but don't count towards {@link #hitsPopulated}
     */
    private int hitsPreloaded = 0;
    private SolrDocument solrDoc = null;
    private int proximitySearchDistance = 0;
    private SearchHitFactory factory;
    private boolean containsSearchTerms = true;
    /** If this hit was found via an authority data identifier, use said identifier to highlight the corresponding word via the named entity tag. */
    private String authorityDataIdentifier = null;

    /**
     * Package-private constructor. Clients should use SearchHitFactory to create SearchHit instances.
     *
     * @param type
     * @param browseElement
     * @param doc
     * @param searchTerms
     * @param locale
     * @param factory
     * @should set authorityDataIdentifier correctly
     */
    SearchHit(HitType type, BrowseElement browseElement, SolrDocument doc, Map<String, Set<String>> searchTerms, Locale locale,
            SearchHitFactory factory) {
        this.type = type;
        this.browseElement = browseElement;
        this.searchTerms = searchTerms;
        this.locale = locale;
        if (browseElement != null) {
            // Add self to owner hits to avoid adding self to child hits
            this.ownerHits.put(Long.toString(browseElement.getIddoc()), this);
            this.ownerDocs.put(Long.toString(browseElement.getIddoc()), doc);
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
        this.factory = factory;
        // If NORM_IDENTIFIER is among the terms
        if (searchTerms != null && searchTerms.containsKey(NormDataImporter.FIELD_IDENTIFIER)) {
            Set<String> identifiers = searchTerms.get(NormDataImporter.FIELD_IDENTIFIER);
            if (identifiers != null && !identifiers.isEmpty()) {
                authorityDataIdentifier = identifiers.iterator().next();
            }
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
     * First truncate and unescape the label, then add highlighting (overrides BrowseElement.labelShort).
     *
     * @should modify label correctly from default
     * @should modify label correctly from title
     * @should do nothing if searchTerms null
     */
    void addLabelHighlighting() {
        if (searchTerms == null) {
            return;
        }

        IMetadataValue labelShort = new MultiLanguageMetadataValue();
        for (Locale loc : ViewerResourceBundle.getAllLocales()) {

            String label = browseElement.getLabel(loc);

            if (searchTerms.get(SolrConstants.DEFAULT) != null) {
                label = SearchHelper.applyHighlightingToPhrase(label, searchTerms.get(SolrConstants.DEFAULT));
            } else if (searchTerms.get("MD_TITLE") != null) {
                label = SearchHelper.applyHighlightingToPhrase(label, searchTerms.get("MD_TITLE"));
            }

            // Escape HTML tags
            label = StringEscapeUtils.escapeHtml4(label);

            // Then replace highlighting placeholders with HTML tags
            label = SearchHelper.replaceHighlightingPlaceholders(label);

            labelShort.setValue(label, loc);
        }

        browseElement.setLabelShort(labelShort);
    }

    public void setHitsPopulated(int hitsPopulated) {
        this.hitsPopulated = hitsPopulated;
    }

    public void setHitsPreloaded(int hitsPreloaded) {
        this.hitsPreloaded = hitsPreloaded;
    }

    public int getHitsPreloaded() {
        return hitsPreloaded;
    }

    /**
     * Creates child hit elements for each hit matching a CMS page text, if CMS page texts were also searched.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should do nothing if searchTerms do not contain key
     * @should do nothing if no cms pages for record found
     */
    public int addCMSPageChildren() throws DAOException {
        if (searchTerms == null || !searchTerms.containsKey(SolrConstants.CMS_TEXT_ALL)) {
            return 0;
        }

        List<CMSPage> cmsPages = DataManager.getInstance().getDao().getCMSPagesForRecord(browseElement.getPi(), null);
        if (cmsPages.isEmpty()) {
            return 0;
        }

        SortedMap<CMSPage, List<String>> hitPages = new TreeMap<>();
        // Collect relevant texts
        for (CMSPage page : cmsPages) {
            List<String> texts = new ArrayList<>();
            for (PersistentCMSComponent component : page.getPersistentComponents()) {
                for (CMSContent content : component.getContentItems()) {
                    if (content instanceof TranslatableCMSContent trCont) {
                        for (Locale loc : trCont.getText().getLocales()) {
                            texts.add(trCont.getText().getText(loc));
                        }
                    } else if (content instanceof CMSMediaHolder cmsMediaHolder) {
                        CMSMediaItem media = cmsMediaHolder.getMediaItem();
                        if (media != null && media.isHasExportableText()) {
                            texts.add(CmsMediaBean.getMediaFileAsString(media));

                        }
                    }
                }
            }
            List<String> truncatedStrings = texts.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(s -> {
                        String value = Jsoup.parse(s).text();
                        String highlightedValue = SearchHelper.applyHighlightingToPhrase(value, searchTerms.get(SolrConstants.CMS_TEXT_ALL));
                        if (!highlightedValue.equals(value)) {
                            return SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.CMS_TEXT_ALL), highlightedValue,
                                    DataManager.getInstance().getConfiguration().getFulltextFragmentLength(), false, true, proximitySearchDistance);
                        }
                        return new ArrayList<String>();
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            if (!truncatedStrings.isEmpty()) {
                hitPages.put(page, truncatedStrings);
            }
        }

        // Add hits (one for each page)
        if (!hitPages.isEmpty()) {
            int count = 0;
            for (Entry<CMSPage, List<String>> entry : hitPages.entrySet()) {
                SearchHit cmsPageHit = new SearchHit(HitType.CMS,
                        new BrowseElement(browseElement.getPi(), 1, ViewerResourceBundle.getTranslation(entry.getKey().getMenuTitle(), locale), null,
                                locale, null, entry.getKey().getRelativeUrlPath()),
                        null,
                        null,

                        locale, factory);
                children.add(cmsPageHit);
                for (String text : entry.getValue()) {
                    cmsPageHit.getChildren()
                            .add(new SearchHit(HitType.CMS,
                                    new BrowseElement(browseElement.getPi(), 1, entry.getKey().getMenuTitle(), text, locale, null,
                                            entry.getKey().getRelativeUrlPath()),
                                    null, searchTerms, locale, factory));
                    count++;
                }
                hitTypeCounts.put(HitType.CMS, count);
            }
            logger.trace("Added {} CMS page child hits", count);
            return count;
        }

        return 0;
    }

    /**
     * Creates a child hit element for TEI full-texts, with child hits of its own for each truncated fragment containing search terms.
     *
     * @param doc Solr page doc
     * @param language a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     * @should throw IllegalArgumentException if doc null
     * @should do nothing if searchTerms does not contain fulltext
     * @should do nothing if tei file name not found
     */
    public int addFulltextChild(SolrDocument doc, final String language)
            throws IndexUnreachableException, DAOException, ViewerConfigurationException {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }

        if (searchTerms == null || !searchTerms.containsKey(SolrConstants.FULLTEXT)) {
            return 0;
        }

        String lang = language;
        if (lang == null) {
            lang = "en";
        }

        // Check whether TEI is available at all
        String teiFilename = (String) doc.getFirstValue(SolrConstants.FILENAME_TEI + SolrConstants.MIDFIX_LANG + lang.toUpperCase());
        if (StringUtils.isEmpty(teiFilename)) {
            teiFilename = (String) doc.getFirstValue(SolrConstants.FILENAME_TEI);
        }
        if (StringUtils.isEmpty(teiFilename)) {
            return 0;
        }

        try {
            String fulltext = null;
            if (BeanUtils.getRequest() != null
                    && AccessConditionUtils.checkAccess(BeanUtils.getRequest(), "text", browseElement.getPi(), teiFilename, false).isGranted()) {
                fulltext = DataFileTools.loadTei((String) doc.getFieldValue(SolrConstants.PI), lang);
            }
            if (fulltext != null) {
                fulltext = TEITools.getTeiFulltext(fulltext);
                fulltext = Jsoup.parse(fulltext).text();
            }
            // logger.trace(fulltext); //NOSONAR Sometimes used for debugging
            List<String> fulltextFragments = fulltext == null ? null : SearchHelper.truncateFulltext(searchTerms.get(SolrConstants.FULLTEXT),
                    fulltext, DataManager.getInstance().getConfiguration().getFulltextFragmentLength(), false, false, proximitySearchDistance);

            int count = 0;
            if (fulltextFragments != null && !fulltextFragments.isEmpty()) {
                SearchHit hit = new SearchHit(HitType.PAGE,
                        new BrowseElement(browseElement.getPi(), 1, ViewerResourceBundle.getTranslation("TEI", locale), null, locale, null, null),
                        doc, searchTerms, locale, factory);
                for (String fragment : fulltextFragments) {
                    hit.getChildren()
                            .add(new SearchHit(HitType.PAGE, new BrowseElement(browseElement.getPi(), 1, "TEI", fragment, locale, null, null), doc,
                                    searchTerms, locale, factory));
                    count++;
                }
                children.add(hit);
                // logger.trace("Added {} fragments", count); //NOSONAR Sometimes used for debugging
                int oldCount = hit.getHitTypeCounts().get(HitType.PAGE) != null ? hit.getHitTypeCounts().get(HitType.PAGE) : 0;
                hitTypeCounts.put(HitType.PAGE, oldCount + count);
                return count;
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage(), e);
        }
        return 0;
    }

    public int getChildDocCount() {
        return this.childDocs.size();
    }

    /**
     * <p>
     * populateChildren.
     * </p>
     *
     * @param number a int.
     * @param skip a int.
     * @param locale a {@link java.util.Locale} object.
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public void populateChildren(final int number, int skip, Locale locale, HttpServletRequest request)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        logger.trace("populateChildren START");

        // Create child hits
        String pi = browseElement.getPi();
        if (pi == null || childDocs == null) {
            logger.trace("Nothing to populate");
            return;
        }

        logger.trace("{} child hit(s) found for {}", childDocs.size(), pi);
        int num = number;
        if (num + skip > childDocs.size()) {
            num = childDocs.size() - skip;
        }
        int childDocIndex = skip;
        int hitCount = getHitCount() - getHitsPreloaded();
        while (childDocIndex < childDocs.size() && hitsPopulated < Math.min(hitCount, num + skip)) {
            SolrDocument childDoc = childDocs.get(childDocIndex);
            childDocIndex++;
            String fulltext = null;
            DocType docType = DocType.getByName((String) childDoc.getFieldValue(SolrConstants.DOCTYPE));
            if (docType != null) {
                boolean acccessDeniedType = false;
                switch (docType) {
                    case PAGE: //NOSONAR, no break on purpose to run through all cases
                        try {
                            fulltext = getFulltext(request, pi, authorityDataIdentifier, childDoc);
                        } catch (AccessDeniedException e) {
                            acccessDeniedType = true;
                        } catch (PresentationException | FileNotFoundException e) {
                            fulltext = null;
                        }
                        // Skip page hits without a proper full-text
                        if (StringUtils.isBlank(fulltext)) {
                            continue;
                        }
                    case METADATA:
                    case UGC:
                    case EVENT:
                        handleMetadataHit(childDoc, fulltext, docType, acccessDeniedType);
                        break;
                    case DOCSTRCT:
                        // Docstruct hits are immediate children of the main hit
                        String iddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC);
                        if (!ownerHits.containsKey(iddoc)) {
                            SearchHit childHit = factory.createSearchHit(childDoc, null, fulltext, null);
                            children.add(childHit);
                            ownerHits.put(iddoc, childHit);
                            ownerDocs.put(iddoc, childDoc);
                            hitsPopulated++;
                        }
                        break;
                    case ARCHIVE:
                        // EAD archive
                        iddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC);
                        if (!ownerHits.containsKey(iddoc) && DataManager.getInstance().getConfiguration().isArchivesEnabled()) {
                            SearchHit childHit = factory.createSearchHit(childDoc, null, fulltext, null);
                            children.add(childHit);
                            ownerHits.put(iddoc, childHit);
                            ownerDocs.put(iddoc, childDoc);
                            hitsPopulated++;
                            
                            // Check and add link to related record, if exists
                            String entryId = SolrTools.getSingleFieldStringValue(childDoc, SolrConstants.EAD_NODE_ID);
                            if (StringUtils.isNotEmpty(entryId)) {
                                childHit.url = "archives/" + SolrEADParser.DATABASE_NAME + "/" + pi + "/?selected=" + entryId + "#selected";
                                SolrDocument relatedRecordDoc =
                                        DataManager.getInstance()
                                                .getSearchIndex()
                                                .getFirstDoc(
                                                        '+' + SolrConstants.DOCTYPE + ':' + DocType.DOCSTRCT.name() + " +" + SolrConstants.EAD_NODE_ID
                                                                + ":\"" + entryId + '"',
                                                        Arrays.asList(SolrConstants.LABEL, SolrConstants.PI_TOPSTRUCT));
                                if (relatedRecordDoc != null) {
                                    childHit.setAltUrl(
                                            "piresolver?id=" + SolrTools.getSingleFieldStringValue(relatedRecordDoc, SolrConstants.PI_TOPSTRUCT));
                                    childHit.setAltLabel(SolrTools.getSingleFieldStringValue(relatedRecordDoc, SolrConstants.LABEL));
                                    if (StringUtils.isEmpty(childHit.getAltLabel())) {
                                        childHit.setAltLabel(SolrTools.getSingleFieldStringValue(relatedRecordDoc, SolrConstants.PI_TOPSTRUCT));
                                    }
                                    logger.trace("altUrl: {}", childHit.getAltUrl());
                                }
                            }
                        }
                        break;
                    case GROUP:
                    default:
                        break;
                }
            }
        }

        if (childDocs.isEmpty()) {
            ownerDocs.clear();
            ownerHits.clear();
        }
    }

    public void handleMetadataHit(SolrDocument childDoc, String fulltext, DocType docType, boolean acccessDeniedType)
            throws IndexUnreachableException, PresentationException {
        String ownerIddoc = (String) childDoc.getFieldValue(SolrConstants.IDDOC_OWNER);
        SearchHit ownerHit = ownerHits.get(ownerIddoc);
        if (ownerHit == null) {
            SolrDocument ownerDoc = DataManager.getInstance().getSearchIndex().getDocumentByIddoc(ownerIddoc);
            if (ownerDoc != null) {
                ownerHit = factory.createSearchHit(ownerDoc, null, fulltext, null);
                ownerHit.containsSearchTerms = false;
                children.add(ownerHit);
                ownerHits.put(ownerIddoc, ownerHit);
                ownerDocs.put(ownerIddoc, ownerDoc);
                logger.trace("owner doc found: {}", ownerDoc.getFieldValue("LOGID")); //NOSONAR Sometimes used for debugging
            }
        }
        if (ownerHit != null) {
            // If the owner hit the is the main element, create an intermediary to avoid the child label being displayed twice
            if (ownerHit.equals(this)) {
                SearchHit newOwnerHit = factory.createSearchHit(ownerDocs.get(ownerIddoc), null, fulltext, null);
                ownerHit.getChildren().add(newOwnerHit);
                ownerHit = newOwnerHit;
                ownerHits.put(ownerIddoc, newOwnerHit);
            }
            // logger.trace("owner doc of {}: {}", childDoc.getFieldValue(SolrConstants.IDDOC),
            // ownerHit.getBrowseElement().getIddoc()); //NOSONAR Sometimes used for debugging

            SearchHit childHit =
                    factory.createSearchHit(childDoc, ownerDocs.get(ownerIddoc), fulltext,
                            acccessDeniedType ? HitType.ACCESSDENIED : null);

            // Skip grouped metadata child hits that have no additional (unique) metadata to display
            if (!(DocType.METADATA.equals(docType) && childHit.getFoundMetadata().isEmpty()
                    && ownerHit.getFoundMetadata().isEmpty())) {
                if (!DocType.UGC.equals(docType)) {
                    // Add all found additional metadata to the owner doc (minus duplicates) so it can be displayed
                    for (StringPair metadata : childHit.getFoundMetadata()) {
                        // Found metadata lists will usually be very short, so it's ok to iterate through the list on every check
                        if (!ownerHit.getFoundMetadata().contains(metadata)) {
                            ownerHit.addFoundMetadata(metadata);
                        }
                    }
                }
                ownerHit.getChildren().add(childHit);
                hitsPopulated++;
            }
        }
    }

    /**
     * 
     * @param request
     * @param pi
     * @param authorityIdentifier
     * @param childDoc
     * @return Full-text for this search hit
     * @throws FileNotFoundException If the fulltext resource is not found or not accessible
     * @throws AccessDeniedException If the request is missing access rights to the fulltext resource
     * @throws PresentationException I an internal error occurs when trying to retrieve access rights or the fulltext resource
     */
    public String getFulltext(HttpServletRequest request, String pi, String authorityIdentifier, SolrDocument childDoc)
            throws FileNotFoundException, PresentationException, AccessDeniedException {
        String fulltext = null;
        String altoFilename = (String) childDoc.getFirstValue(SolrConstants.FILENAME_ALTO);
        String plaintextFilename = (String) childDoc.getFirstValue(SolrConstants.FILENAME_FULLTEXT);
        try {
            if (StringUtils.isNotBlank(plaintextFilename)) {
                boolean access = AccessConditionUtils.checkAccess(request, "text", pi, plaintextFilename, false).isGranted();
                if (access) {
                    fulltext = DataFileTools.loadFulltext(null, plaintextFilename, false);
                } else {
                    throw new AccessDeniedException("Access denied to resource " + pi + " / " + plaintextFilename);
                }
            } else if (StringUtils.isNotBlank(altoFilename)) {
                boolean access = AccessConditionUtils.checkAccess(request, "text", pi, altoFilename, false).isGranted();
                if (access) {
                    if (StringUtils.isNotEmpty(authorityIdentifier)) {
                        // If authority identifier is used, load NE tags and match word with identifier
                        try {
                            StringPair alto = DataFileTools.loadAlto(altoFilename);
                            fulltext = ALTOTools.getFulltext(alto.getOne(), alto.getTwo(), true);
                            List<TagCount> tags = ALTOTools.getNERTags(alto.getOne(), alto.getTwo(), null);
                            // logger.trace("found {} entity tags", tags.size());
                            String highlightWord = null;
                            for (TagCount tag : tags) {
                                if (tag.getIdentifier() != null) {
                                    //logger.trace("tag identifier: {}", tag.getIdentifier());
                                }
                                if (authorityIdentifier.equals(tag.getIdentifier())) {
                                    highlightWord = tag.getValue();
                                    break;
                                }
                            }
                            if (StringUtils.isNotEmpty(highlightWord)) {
                                searchTerms.put(SolrConstants.FULLTEXT, Collections.singleton(highlightWord));
                            }
                        } catch (ContentNotFoundException | PresentationException e) {
                            logger.error(e.getMessage());
                        }
                    } else {
                        // Just load the full-text
                        fulltext = DataFileTools.loadFulltext(altoFilename, null, false);
                    }
                } else {
                    throw new AccessDeniedException("Access denied to resource " + pi + " / " + altoFilename);
                }
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IndexUnreachableException | DAOException | IOException e) {
            throw new PresentationException("Error reading fulltext for " + pi + ", page " + childDoc.getFirstValue(SolrConstants.ORDER), e);
        }

        return fulltext;
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
        return type != null ? SEARCH_HIT_TYPE_PREFIX + type.name() : "";
    }

    public String getIconClassForType() {
        if (type != null) {
            switch (type) {
                case PAGE:
                    return "fa fa-file-text";
                case PERSON:
                    return "fa fa-user";
                case CORPORATION:
                    return "fa fa-university";
                case LOCATION:
                case ADDRESS:
                    return "fa fa-envelope";
                case COMMENT:
                    return "fa fa-comment-o";
                case CMS:
                    return "fa fa-file-text-o";
                case EVENT:
                    return "fa fa-calendar";
                case ACCESSDENIED:
                    return "fa fa-lock";
                default:
                    return "fa fa-file-text";
            }
        }

        return "";
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
     * @return the hitNumber
     */
    public long getHitNumber() {
        return hitNumber;
    }

    /**
     * @param hitNumber the hitNumber to set
     */
    public void setHitNumber(long hitNumber) {
        this.hitNumber = hitNumber;
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
    }

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
        return !children.isEmpty();
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
        for (Entry<HitType, Integer> entry : hitTypeCounts.entrySet()) {
            if (entry.getValue() > 0) {
                return true;
            }
        }

        return false;
    }

    public int getHitCount() {
        int total = 0;
        for (Integer num : hitTypeCounts.values()) {
            total += num;
        }
        return total;
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

    public int getMetadataAndDocstructHitCount() {
        return getDocstructHitCount() + getMetadataHitCount();
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
     * getArchiveHitCount.
     * </p>
     *
     * @return a int.
     */
    public int getArchiveHitCount() {
        if (hitTypeCounts.get(HitType.ARCHIVE) != null) {
            return hitTypeCounts.get(HitType.ARCHIVE);
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
        return Collections.unmodifiableList(foundMetadata);
    }

    public void addFoundMetadata(StringPair valuePair) {
        this.foundMetadata.add(valuePair);
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
     * @return the altUrl
     */
    public String getAltUrl() {
        return altUrl;
    }

    /**
     * @param altUrl the altUrl to set
     */
    public void setAltUrl(String altUrl) {
        this.altUrl = altUrl;
    }

    /**
     * @return the altLabel
     */
    public String getAltLabel() {
        return altLabel;
    }

    /**
     * @param altLabel the altLabel to set
     */
    public void setAltLabel(String altLabel) {
        this.altLabel = altLabel;
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
     * @should generate fragment correctly
     */
    public String generateNotificationFragment(int count) {
        return "<tr><td>" + count + ".</td><td><img src=\"" + browseElement.getThumbnailUrl() + "\" alt=\"" + browseElement.getLabel()
                + "\" /></td><td>" + browseElement.getLabel()
                + "</td></tr>";
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

    public String getCssClass() {
        String docStructType = this.getBrowseElement().getDocStructType();
        if (StringUtils.isNotBlank(docStructType)) {
            return "docstructtype__" + docStructType;
        }

        return "";
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getBrowseElement().getLabelShort();
    }

    public void loadChildHits(int numChildren)
            throws PresentationException, IndexUnreachableException, DAOException, ViewerConfigurationException {
        HttpServletRequest servletRequest = BeanUtils.getRequest();
        populateChildren(numChildren, getHitsPopulated(), locale, servletRequest);
        Collections.sort(getChildren());

    }

    public String getDisplayText() {
        if (this.type != null) {
            switch (this.type) {
                case CMS:
                case ACCESSDENIED:
                case PAGE:
                    return this.getBrowseElement().getFulltextForHtml();
                default:
                    return this.getBrowseElement().getLabelShort();

            }
        }

        return "";
    }

    public boolean includeMetadata() {
        if (this.containsSearchTerms && this.type != null) {
            switch (this.type) {
                case DOCSTRCT:
                    if (this.browseElement.isWork() || this.browseElement.isAnchor()) {
                        return false;
                    }
                    // fall through
                case METADATA:
                case PERSON:
                case LOCATION:
                case SHAPE:
                case SUBJECT:
                case CORPORATION:
                case EVENT:
                    String label = this.getDisplayText();
                    return !label.contains("search-list--highlight");
                default:
                    return false;

            }
        }

        return false;
    }

    /**
     * Getter for unit tests.
     * 
     * @return the authorityDataIdentifier
     */
    String getAuthorityDataIdentifier() {
        return authorityDataIdentifier;
    }
}
