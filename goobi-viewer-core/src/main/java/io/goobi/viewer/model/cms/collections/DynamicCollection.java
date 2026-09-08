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
package io.goobi.viewer.model.cms.collections;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.eclipse.persistence.annotations.PrivateOwned;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.Translation;
import io.goobi.viewer.model.viewer.PhysicalElement;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.model.viewer.collections.BrowseElementInfo;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.servlet.http.HttpServletRequest;

/**
 * A digital collection defined entirely in the database. Unlike {@link CMSCollection}, whose membership is derived from a single Solr field value,
 * a dynamic collection is identified by a unique {@link #identifier} and its contents are given by an arbitrary {@link #solrQuery}. It carries a
 * representative image, a label and description in multiple languages and an optional URL linking to a collection page.
 */
@Entity
@Table(name = "dynamic_collections", uniqueConstraints = { @UniqueConstraint(columnNames = { "identifier" }) })
public class DynamicCollection implements Comparable<DynamicCollection>, BrowseElementInfo, CMSMediaHolder, IPolyglott, Serializable {

    private static final long serialVersionUID = -8703131458920215939L;

    private static final Logger logger = LogManager.getLogger(DynamicCollection.class);

    private static final String LABEL_TAG = "label";
    private static final String DESCRIPTION_TAG = "description";

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dynamic_collection_id")
    private Long id;

    /** Unique, immutable, URL-safe identifier used in the {@code DC_DYNAMIC:&lt;identifier&gt;} facet token and the browse URL. */
    @Column(name = "identifier", nullable = false, unique = true)
    private String identifier;

    /** Arbitrary Solr query defining the records belonging to this collection. */
    @Column(name = "solr_query", columnDefinition = "LONGTEXT")
    private String solrQuery = "";

    /** Ordering position within the flat list of dynamic collections. */
    @Column(name = "sort_order")
    private int sortOrder = 0;

    /** Media item reference for the representative image. */
    @JoinColumn(name = "media_item_id")
    private CMSMediaItem mediaItem;

    @Column(name = "representative_work_pi")
    private String representativeWorkPI = "";

    @Column(name = "collection_url")
    private String collectionUrl;

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<DynamicCollectionTranslation> translations = new ArrayList<>();

    @Transient
    private Locale selectedLocale = BeanUtils.getLocale();

    @Transient
    private AccessPermission accessPermissionThumbnail = null;

    /**
     * Creates a new empty DynamicCollection instance.
     */
    public DynamicCollection() {
        //
    }

    /**
     * Creates a new DynamicCollection with the given identifier.
     *
     * @param identifier the unique identifier of the collection
     * @throws java.lang.IllegalArgumentException if the identifier is null, empty or blank
     */
    public DynamicCollection(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("The identifier of a DynamicCollection may not be null, empty or blank");
        }
        this.identifier = identifier;
    }

    /**
     * Cloning constructor.
     *
     * @param orig the DynamicCollection to clone
     */
    public DynamicCollection(DynamicCollection orig) {
        this.id = orig.id;
        this.identifier = orig.identifier;
        this.solrQuery = orig.solrQuery;
        this.sortOrder = orig.sortOrder;
        this.collectionUrl = orig.collectionUrl;
        this.mediaItem = orig.mediaItem;
        this.representativeWorkPI = orig.representativeWorkPI;
        this.selectedLocale = orig.selectedLocale;
        this.translations = orig.translations.stream().map(tr -> new DynamicCollectionTranslation(tr, this)).collect(Collectors.toList());
    }

    /**
     * Loads representative image access info from Solr.
     *
     * @return this
     */
    public DynamicCollection loadRepresentativeImage() {
        if (hasRepresentativeWork()) {
            try {
                SolrDocument doc = DataManager.getInstance()
                        .getSearchIndex()
                        .getFirstDoc(SolrConstants.PI + ":\"" + getRepresentativeWorkPI() + '"', null);
                return loadRepresentativeImage(doc);
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error(e.getMessage());
            }
        }

        return this;
    }

    /**
     * Sets the thumbnail access permission from an already-fetched representative record document.
     *
     * <p>
     * Overload for callers that have loaded the representative record's Solr document in a single batched query,
     * avoiding a per-collection {@code getFirstDoc} round-trip. The document is reused for the page lookup as well.
     *
     * @param recordDoc top-level Solr document of the representative work, or null to skip
     * @return this
     */
    public DynamicCollection loadRepresentativeImage(SolrDocument recordDoc) {
        if (hasRepresentativeWork() && recordDoc != null) {
            try {
                logger.trace("loaded record: {}", getRepresentativeWorkPI());
                PhysicalElement pe = ThumbnailHandler.getPage(recordDoc, SolrTools.getSingleFieldIntegerValue(recordDoc, SolrConstants.THUMBPAGENO));
                if (pe != null) {
                    setAccessPermissionThumbnail(pe.getAccessPermission(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS));
                }
            } catch (PresentationException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage());
            }
        }

        return this;
    }

    /**
     * Getter for the field <code>id</code>.
     *
     * @return the database primary key of this collection
     */
    public Long getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@link BrowseElementInfo} "name" is the collection's immutable identifier.
     */
    @Override
    public String getName() {
        return identifier;
    }

    /**
     * Getter for the field <code>identifier</code>.
     *
     * @return the unique, immutable identifier of the collection
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Setter for the field <code>identifier</code>.
     *
     * @param identifier the unique, immutable identifier of the collection
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Getter for the field <code>solrQuery</code>.
     *
     * @return the Solr query defining the contents of this collection
     */
    public String getSolrQuery() {
        return solrQuery;
    }

    /**
     * Setter for the field <code>solrQuery</code>.
     *
     * @param solrQuery the Solr query defining the contents of this collection
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
    }

    /**
     * Getter for the field <code>sortOrder</code>.
     *
     * @return the ordering position within the flat list of dynamic collections
     */
    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * Setter for the field <code>sortOrder</code>.
     *
     * @param sortOrder the ordering position within the flat list of dynamic collections
     */
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** {@inheritDoc} */
    @Override
    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }

    /** {@inheritDoc} */
    @Override
    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    /**
     * Getter for the field <code>collectionUrl</code>.
     *
     * @return the custom URL overriding the default collection browse URL, or null if not set
     */
    public String getCollectionUrl() {
        return collectionUrl;
    }

    /**
     * Setter for the field <code>collectionUrl</code>.
     *
     * @param collectionUrl custom URL overriding the default collection browse URL
     */
    public void setCollectionUrl(String collectionUrl) {
        this.collectionUrl = collectionUrl;
    }

    /**
     * Adds a translation for the collection label.
     *
     * @param label translation to add as a collection label
     */
    public void addLabel(DynamicCollectionTranslation label) {
        label.setTag(LABEL_TAG);
        label.setOwner(this);
        translations.add(label);
    }

    /**
     * Adds a translation for the collection description.
     *
     * @param description translation to add as a collection description
     */
    public void addDescription(DynamicCollectionTranslation description) {
        description.setTag(DESCRIPTION_TAG);
        description.setOwner(this);
        translations.add(description);
    }

    /**
     * Removes all label and description translations whose value is blank, so that empty translation rows are not persisted (they are re-created in
     * memory for editing via {@link #populateLabels()} / {@link #populateDescriptions()}).
     */
    public void pruneEmptyTranslations() {
        translations.removeIf(translation -> StringUtils.isBlank(translation.getTranslationValue()));
    }

    /**
     * returns all translations of this collection with the tag {@link #LABEL_TAG}.
     *
     * @return all labels for this collection
     */
    public List<DynamicCollectionTranslation> getLabels() {
        return translations.stream().filter(translation -> LABEL_TAG.equals(translation.getTag())).collect(Collectors.toList());
    }

    /**
     * returns all translations of this collection with the tag {@link #DESCRIPTION_TAG}.
     *
     * @return all descriptions for this collection
     */
    public List<DynamicCollectionTranslation> getDescriptions() {
        return translations.stream().filter(translation -> DESCRIPTION_TAG.equals(translation.getTag())).collect(Collectors.toList());
    }

    /**
     * Get the label for the given {@code locale}, falling back to the collection {@link #identifier} if no matching label exists.
     *
     * @param locale a {@link java.util.Locale} object.
     * @return the label string for the given locale, or the collection identifier
     */
    public String getLabel(Locale locale) {
        String language = locale != null ? locale.getLanguage() : selectedLocale.getLanguage();
        return getLabels().stream()
                .filter(translation -> language.equalsIgnoreCase(translation.getLanguage()))
                .map(Translation::getTranslationValue)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(identifier);
    }

    /**
     * getLabelAsTranslation.
     *
     * @param language ISO language code to filter label translations by
     * @return the label translation for the given language, or null if none exists
     */
    public DynamicCollectionTranslation getLabelAsTranslation(String language) {
        return getLabels().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().orElse(null);
    }

    /**
     * getDescriptionAsTranslation.
     *
     * @param language ISO language code to filter description translations by
     * @return the description translation for the given language, or null if none exists
     */
    public DynamicCollectionTranslation getDescriptionAsTranslation(String language) {
        return getDescriptions().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().orElse(null);
    }

    /**
     * @return {@link DynamicCollectionTranslation}
     */
    public DynamicCollectionTranslation getDescriptionAsTranslation() {
        return getDescriptionAsTranslation(selectedLocale.getLanguage());
    }

    /**
     * getLabelAsTranslation for the currently selected locale.
     *
     * @return {@link DynamicCollectionTranslation}
     */
    public DynamicCollectionTranslation getLabelAsTranslation() {
        return getLabelAsTranslation(selectedLocale.getLanguage());
    }

    /**
     * setLabel.
     *
     * @param value new label text to set for the given language
     * @param language ISO language code identifying the label to update
     */
    public void setLabel(String value, String language) {
        getLabels().stream()
                .filter(label -> label.getLanguage().equalsIgnoreCase(language))
                .findFirst()
                .ifPresent(label -> label.setTranslationValue(value));
    }

    /**
     * get the description for the given {@code language}, or an empty string if no matching description exists.
     *
     * @param language ISO language code identifying the desired description
     * @return the description string for the given language, or an empty string
     */
    @Override
    public String getDescription(String language) {
        return getDescriptions().stream()
                .filter(translation -> language.equalsIgnoreCase(translation.getLanguage()))
                .findFirst()
                .map(Translation::getTranslationValue)
                .orElse("");
    }

    /**
     * Get the description for the given {@code locale}, or an empty string if no matching description exists.
     *
     * @param locale a {@link java.util.Locale} object.
     * @return the description string for the given locale, or an empty string
     */
    public String getDescription(Locale locale) {
        return getDescription(locale.getLanguage());
    }

    /**
     * get the description for the current locale, or an empty string if no matching description exists.
     *
     * @return the description string for the current locale, or an empty string
     */
    @Override
    public String getDescription() {
        return getDescription(BeanUtils.getLocale());
    }

    /**
     * setDescription.
     *
     * @param value new description text to set for the given language
     * @param language ISO language code identifying the description to update
     */
    public void setDescription(String value, String language) {
        getDescriptions().stream()
                .filter(label -> label.getLanguage().equalsIgnoreCase(language))
                .findFirst()
                .ifPresent(desc -> desc.setTranslationValue(value));
    }

    /**
     * populateLabels. Ensures a (possibly empty) label translation exists for every supported language.
     */
    public void populateLabels() {
        NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
        if (navigationHelper != null) {
            this.populateLabels(navigationHelper.getSupportedLanguages());
        }
    }

    /**
     * populateLabels.
     *
     * @param languages list of language codes to ensure labels exist for
     */
    public void populateLabels(List<String> languages) {
        for (String language : languages) {
            if (getLabels().stream().noneMatch(label -> label.getLanguage().equalsIgnoreCase(language))) {
                addLabel(new DynamicCollectionTranslation(language, ""));
            }
        }
    }

    /**
     * populateDescriptions. Ensures a (possibly empty) description translation exists for every supported language.
     */
    public void populateDescriptions() {
        NavigationHelper navigationHelper = BeanUtils.getNavigationHelper();
        if (navigationHelper != null) {
            this.populateDescriptions(navigationHelper.getSupportedLanguages());
        }
    }

    /**
     * populateDescriptions.
     *
     * @param languages list of language codes to ensure descriptions exist for
     */
    public void populateDescriptions(List<String> languages) {
        for (String language : languages) {
            if (getDescriptions().stream().noneMatch(description -> description.getLanguage().equalsIgnoreCase(language))) {
                addDescription(new DynamicCollectionTranslation(language, ""));
            }
        }
    }

    /**
     * hasMediaItem.
     *
     * @return true if this collection has an associated media item, false otherwise
     */
    @Override
    public boolean hasMediaItem() {
        return getMediaItem() != null;
    }

    /**
     * hasRepresentativeWork.
     *
     * @return true if this collection has a non-blank representative work persistent identifier, false otherwise
     */
    public boolean hasRepresentativeWork() {
        return StringUtils.isNotBlank(getRepresentativeWorkPI());
    }

    /**
     * hasImage.
     *
     * @return true if this collection has a representative work or an associated media item, false otherwise
     */
    public boolean hasImage() {
        return hasRepresentativeWork() || hasMediaItem();
    }

    /**
     * Getter for the field <code>representativeWorkPI</code>.
     *
     * @return the persistent identifier of the record whose thumbnail represents this collection
     */
    public String getRepresentativeWorkPI() {
        return representativeWorkPI;
    }

    /**
     * Setter for the field <code>representativeWorkPI</code>.
     *
     * @param representativeWorkPI the persistent identifier of the record whose thumbnail represents this collection
     */
    public void setRepresentativeWorkPI(String representativeWorkPI) {
        this.representativeWorkPI = representativeWorkPI;
    }

    /**
     * getRepresentativeWork.
     *
     * @return an Optional containing the representative StructElement, or empty if none is configured or found
     */
    public Optional<StructElement> getRepresentativeWork() {
        if (hasRepresentativeWork()) {
            try {
                SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(getRepresentativeWorkPI());
                if (doc != null) {
                    return Optional.ofNullable(new StructElement((String) doc.getFieldValue(SolrConstants.IDDOC), doc));
                }
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error(e.toString(), e);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the search facet token identifying this collection, i.e. {@code DC_DYNAMIC:<identifier>}. Passed as the facets segment of a search URL,
     * it makes the search list the collection's records with the collection selected as the active facet.
     *
     * @return the unencoded facet token of this collection
     */
    public String getFacetString() {
        return SolrConstants.DC_DYNAMIC + ":" + identifier;
    }

    /** {@inheritDoc} */
    @Override
    public URI getLinkURI() {
        return getLinkURI(BeanUtils.getRequest());
    }

    /**
     * {@inheritDoc}
     *
     * @should honor a custom collection url
     * @should build a facet search url from the request without a faces context
     * @should return null when no query and no custom url
     */
    @Override
    public URI getLinkURI(HttpServletRequest request) {
        if (StringUtils.isNotBlank(getCollectionUrl())) {
            URI applicationUri;
            if (request != null) {
                applicationUri = URI.create(ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + "/");
            } else {
                applicationUri = URI.create(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + "/");
            }
            return applicationUri.resolve(getCollectionUrl().replaceAll("^\\/", "").trim());
        }

        // No custom collection URL: default to a search filtered by this collection's DC_DYNAMIC facet value, so that the collection shows up as
        // the selected facet in the search sidebar. The URL is built directly from the request rather than via PrettyUrlTools, because this method is
        // also called from the IIIF collection REST endpoint, which runs without a FacesContext (PrettyContext.getCurrentInstance() would throw
        // there). The path mirrors the "newSearch5" pretty mapping /search/{context}/{query}/{page}/{sort}/{facets}/.
        if (StringUtils.isNotBlank(getSolrQuery())) {
            String applicationUrl = getApplicationUrl(request);
            if (StringUtils.isNotBlank(applicationUrl)) {
                String facetString = StringTools.encodeUrl(getFacetString(), true);
                return URI.create(applicationUrl + "/search/-/-/1/-/" + facetString + "/");
            }
        }

        return null;
    }

    /**
     * Resolves the application base URL (scheme, host, context path) preferring the given request, falling back to the current request and finally to
     * the JSF context. Works both inside a JSF render and inside a REST request (which has no FacesContext).
     *
     * @param request the servlet request, may be null
     * @return the application base URL, or an empty string if none could be resolved
     */
    private static String getApplicationUrl(HttpServletRequest request) {
        HttpServletRequest req = request != null ? request : BeanUtils.getRequest();
        if (req != null) {
            return ServletUtils.getServletPathWithHostAsUrlFromRequest(req);
        }
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI() {
        return getRepresentativeWork().map(work -> URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(work)))
                .orElse(Optional.ofNullable(getMediaItem()).map(CMSMediaItem::getIconURI).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI(int width, int height) {
        return getRepresentativeWork().map(work -> URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(work, width, height)))
                .orElse(Optional.ofNullable(getMediaItem()).map(item -> item.getIconURI(width, height)).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI(int size) {
        return getRepresentativeWork().map(work -> URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getSquareThumbnailUrl(work, size)))
                .orElse(Optional.ofNullable(getMediaItem()).map(item -> item.getIconURI(size)).orElse(null));
    }

    /** {@inheritDoc} */
    @Override
    public IMetadataValue getTranslationsForName() {
        Map<String, String> labels = getLabels().stream()
                .filter(l -> StringUtils.isNotBlank(l.getTranslationValue()))
                .collect(Collectors.toMap(Translation::getLanguage, Translation::getTranslationValue));
        if (labels.isEmpty()) {
            return new SimpleMetadataValue(identifier);
        }

        return new MultiLanguageMetadataValue(labels);
    }

    /** {@inheritDoc} */
    @Override
    public IMetadataValue getTranslationsForDescription() {
        Map<String, String> descriptions = getDescriptions().stream()
                .filter(l -> StringUtils.isNotBlank(l.getTranslationValue()))
                .collect(Collectors.toMap(Translation::getLanguage, Translation::getTranslationValue));
        if (descriptions.isEmpty()) {
            return null;
        }

        return new MultiLanguageMetadataValue(descriptions);
    }

    /** {@inheritDoc} */
    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getImageFilter();
    }

    /** {@inheritDoc} */
    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getImageTypes();
    }

    /** {@inheritDoc} */
    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isComplete(Locale locale) {
        return !isEmpty(locale);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid(Locale locale) {
        return !isEmpty(locale);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("locale may not be null");
        }

        DynamicCollectionTranslation translation = getDescriptionAsTranslation(locale.getLanguage());
        if (translation == null) {
            return true;
        }

        return StringUtils.isBlank(translation.getTranslationValue());
    }

    /** {@inheritDoc} */
    @Override
    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    /** {@inheritDoc} */
    @Override
    public void setSelectedLocale(Locale locale) {
        this.selectedLocale = locale;
    }

    public AccessPermission getAccessPermissionThumbnail() {
        return accessPermissionThumbnail;
    }

    public void setAccessPermissionThumbnail(AccessPermission accessPermissionThumbnail) {
        this.accessPermissionThumbnail = accessPermissionThumbnail;
    }

    /**
     * @param other the other DynamicCollection to compare content with
     * @return true if the persisted content of this collection matches the given collection, false otherwise
     */
    public boolean contentEquals(DynamicCollection other) {
        return Objects.equals(this.mediaItem, other.mediaItem)
                && Strings.CS.equals(this.representativeWorkPI, other.representativeWorkPI)
                && Strings.CS.equals(this.identifier, other.identifier)
                && Strings.CS.equals(this.solrQuery, other.solrQuery)
                && this.sortOrder == other.sortOrder
                && Strings.CS.equals(this.collectionUrl, other.collectionUrl)
                && translationsEquals(this.translations, other.translations);
    }

    private static boolean translationsEquals(List<DynamicCollectionTranslation> tr1, List<DynamicCollectionTranslation> tr2) {
        if (tr1.size() == tr2.size()) {
            for (DynamicCollectionTranslation tr : tr1) {
                DynamicCollectionTranslation otr = tr2.stream()
                        .filter(t -> Strings.CS.equals(t.getTag(), tr.getTag()))
                        .filter(t -> Strings.CS.equals(t.getLanguage(), tr.getLanguage()))
                        .findAny()
                        .orElse(null);
                if (otr == null && StringUtils.isNotBlank(tr.getTranslationValue())) {
                    return false;
                } else if (otr != null && !Strings.CS.equals(otr.getTranslationValue(), tr.getTranslationValue())) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compares collections by the alphabetical sorting of their {@link #getName()}
     */
    @Override
    public int compareTo(DynamicCollection other) {
        return getName().compareTo(other.getName());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return identifier == null ? 0 : identifier.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return Objects.equals(getName(), ((DynamicCollection) obj).getName());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DynamicCollection: " + getName();
    }
}
