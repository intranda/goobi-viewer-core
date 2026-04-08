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
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
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
 * A class representing persistent configurations for a collection. A collections is identified by a SOLR-field name and a label. The most common
 * SOLR-field is "DC" and label is the internal name of the collection to edit. This class allows setting a representative image, names in multiple
 * languages and a uri linking to a collection page.
 *
 * @author Florian Alpers
 */
@Entity
@Table(name = "cms_collections", uniqueConstraints = { @UniqueConstraint(columnNames = { "solr_field", "solr_value" }) })
public class CMSCollection implements Comparable<CMSCollection>, BrowseElementInfo, CMSMediaHolder, IPolyglott, Serializable {

    private static final long serialVersionUID = 4674623800509560656L;

    private static final Logger logger = LogManager.getLogger(CMSCollection.class);

    private static final String LABEL_TAG = "label";
    private static final String DESCRIPTION_TAG = "description";

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_collection_id")
    private Long id;

    @Column(name = "solr_field")
    private String solrField;

    @Column(name = "solr_value")
    private String solrFieldValue;

    /** Media item reference for media content items. */
    @JoinColumn(name = "media_item_id")
    private CMSMediaItem mediaItem;

    @Column(name = "representative_work_pi")
    private String representativeWorkPI = "";

    @Column(name = "collection_url")
    private String collectionUrl;

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<CMSCollectionTranslation> translations = new ArrayList<>();

    @Transient
    private Locale selectedLocale = BeanUtils.getLocale();

    @Transient
    private AccessPermission accessPermissionThumbnail = null;

    /**
     * Creates a new CMSCollection instance.
     */
    public CMSCollection() {
        // TODO Is this in use?
    }

    /**
     * Default constructor, creating a Collection from the identifying fields {@link io.goobi.viewer.model.cms.collections.CMSCollection#solrField}
     * and {@link io.goobi.viewer.model.cms.collections.CMSCollection#solrFieldValue}.
     *
     * @param solrField The name of the SOLR field holding the values for the collection
     * @param solrFieldValue The value of the solrField identifying this collection
     * @throws java.lang.IllegalArgumentException If either argument returns true for
     *             {@link org.apache.commons.lang3.StringUtils#isBlank(CharSequence)}
     */
    public CMSCollection(String solrField, String solrFieldValue) {
        if (StringUtils.isBlank(solrField) || StringUtils.isBlank(solrFieldValue)) {
            throw new IllegalArgumentException("The constructor paramters of CMSCollections may not be null, empty or blank");
        }
        this.solrField = solrField;
        this.solrFieldValue = solrFieldValue;
    }

    /**
     * Cloning constructor.
     *
     * @param orig the CMSCollection to clone
     */
    public CMSCollection(CMSCollection orig) {
        this.solrField = orig.solrField;
        this.solrFieldValue = orig.solrFieldValue;
        this.id = orig.id;
        this.collectionUrl = orig.collectionUrl;
        this.mediaItem = orig.mediaItem;
        this.representativeWorkPI = orig.representativeWorkPI;
        this.selectedLocale = orig.selectedLocale;
        this.translations = orig.translations.stream().map(tr -> new CMSCollectionTranslation(tr, this)).collect(Collectors.toList());
    }

    /**
     * Loads representative image info from Solr.
     * 
     * @return this
     */
    public CMSCollection loadRepresentativeImage() {
        if (hasRepresentativeWork()) {
            // Check thumbnail access permission if representative record set
            try {
                SolrDocument doc = DataManager.getInstance()
                        .getSearchIndex()
                        .getFirstDoc(SolrConstants.PI + ":\"" + getRepresentativeWorkPI() + '"', null);
                if (doc != null) {
                    logger.trace("loaded record: {}", getRepresentativeWorkPI());
                    PhysicalElement pe = ThumbnailHandler.getPage(getRepresentativeWorkPI(),
                            SolrTools.getSingleFieldIntegerValue(doc, SolrConstants.THUMBPAGENO));
                    if (pe != null) {
                        setAccessPermissionThumbnail(pe.getAccessPermission(IPrivilegeHolder.PRIV_VIEW_THUMBNAILS));
                    }
                }
            } catch (PresentationException | IndexUnreachableException | DAOException e) {
                logger.error(e.getMessage());
            }
        }

        return this;
    }

    /**
     * Getter for the field <code>mediaItem</code>.
     *

     */
    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }

    /** {@inheritDoc} */
    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    /**
     * Getter for the field <code>collectionUrl</code>.
     *

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
    public void addLabel(CMSCollectionTranslation label) {
        label.setTag(LABEL_TAG);
        label.setOwner(this);
        translations.add(label);
    }

    /**
     * Adds a translation for the collection description.
     *
     * @param description translation to add as a collection description
     */
    public void addDescription(CMSCollectionTranslation description) {
        description.setTag(DESCRIPTION_TAG);
        description.setOwner(this);
        translations.add(description);
    }

    /**
     * returns all translations of this page with the tag {@link #LABEL_TAG}.
     *
     * @return all labels for this collections
     */
    public List<CMSCollectionTranslation> getLabels() {
        return translations.stream().filter(translation -> LABEL_TAG.equals(translation.getTag())).collect(Collectors.toList());
    }

    /**
     * returns all translations of this page with the tag {@link #DESCRIPTION_TAG}.
     *
     * @return all descriptions for this collections
     */
    public List<CMSCollectionTranslation> getDescriptions() {
        return translations.stream().filter(translation -> DESCRIPTION_TAG.equals(translation.getTag())).collect(Collectors.toList());
    }

    /**
     * Get the label for the given {@code locale}, or an empty string if no matching label exists.
     *
     * @param locale a {@link java.util.Locale} object.
     * @return The string value of the label of the given locale, or an empty string
     */
    public String getLabel(Locale locale) {
        return ViewerResourceBundle.getTranslation(getSolrFieldValue(), locale);
    }

    /**
     * getLabelAsTranslation.
     *
     * @param language ISO language code to filter label translations by
     * @return a {@link io.goobi.viewer.model.cms.collections.CMSCollectionTranslation} object.
     */
    public CMSCollectionTranslation getLabelAsTranslation(String language) {
        return getLabels().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().orElse(null);
    }

    /**
     * getDescriptionAsTranslation.
     *
     * @param language ISO language code to filter description translations by
     * @return a {@link io.goobi.viewer.model.cms.collections.CMSCollectionTranslation} object.
     */
    public CMSCollectionTranslation getDescriptionAsTranslation(String language) {
        return getDescriptions().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().orElse(null);
    }

    /**
     *
     * @return {@link CMSCollectionTranslation}
     */
    public CMSCollectionTranslation getDescriptionAsTranslation() {
        // logger.trace("getDescriptionAsTranslation: {}", selectedLocale.getLanguage()); //NOSONAR Debug
        return getDescriptionAsTranslation(selectedLocale.getLanguage());
    }

    /**
     * setLabel.
     *
     * @param language ISO language code identifying the label to update
     * @param value new label text to set for the given language
     */
    public void setLabel(String value, String language) {
        getLabels().stream()
                .filter(label -> label.getLanguage().equalsIgnoreCase(language))
                .findFirst()
                .ifPresent(label -> label.setTranslationValue(value));
    }

    /**
     * get the description for the given {@code language}, or an empty string if no matching description exists the language should be the language
     * code of a {@link java.util.Locale} and is case insensitive.
     *
     * @param language ISO language code identifying the desired description
     * @return The string value of the description of the given language, or an empty string
     */
    public String getDescription(String language) {
        return getDescriptions().stream()
                .filter(translation -> language.equalsIgnoreCase(translation.getLanguage()))
                //                .filter(translation -> StringUtils.isNotBlank(translation.getValue()))
                .findFirst()
                .map(Translation::getTranslationValue)
                .orElse("");
        //                .orElse(ViewerResourceBundle.getTranslation(getSolrFieldValue() + "_DESCRIPTION", null));
    }

    /**
     * Get the description for the given {@code locale}, or an empty string if no matching description exists.
     *
     * @param locale a {@link java.util.Locale} object.
     * @return The string value of the description of the given locale, or an empty string
     */
    public String getDescription(Locale locale) {
        return getDescription(locale.getLanguage());
    }

    /**
     * get the description for the current locale (given by {@link io.goobi.viewer.managedbeans.utils.BeanUtils#getLocale()}, or an empty string if no
     * matching description exists.
     *
     * @return The string value of the description of the current locale, or an empty string
     */
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
     * Getter for the field <code>solrField</code>.
     *
     * @return the solrField. Guaranteed to hold a non-blank value
     */
    public String getSolrField() {
        return solrField;
    }

    /**
     * Getter for the field <code>solrFieldValue</code>.
     *
     * @return the solrFieldValue. Guaranteed to hold a non-blank value
     */
    public String getSolrFieldValue() {
        return solrFieldValue;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compares collection by the alphabatical sorting of their {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public int compareTo(CMSCollection other) {
        return getSolrFieldValue().compareTo(other.getSolrFieldValue());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the hashCode of {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public int hashCode() {
        return getSolrFieldValue().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>A {@link CMSCollection} is equal to any other object if that is also a CMSCollection and returns the same values for
     * {@link CMSCollection#getSolrField()} and {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return getSolrField().equals(((CMSCollection) obj).getSolrField())
                    && getSolrFieldValue().equals(((CMSCollection) obj).getSolrFieldValue());
        }

        return false;
    }

    /**
     *
     * @param other the other CMSCollection to compare content with
     * @return a boolean
     */
    public boolean contentEquals(CMSCollection other) {
        return Objects.equals(this.mediaItem, other.mediaItem)
                && Strings.CS.equals(this.representativeWorkPI, other.representativeWorkPI)
                && Strings.CS.equals(this.solrField, other.solrField)
                && Strings.CS.equals(this.collectionUrl, other.collectionUrl)
                && Strings.CS.equals(this.solrFieldValue, other.solrFieldValue)
                && translationsEquals(this.translations, other.translations);
    }

    /**
     * @param tr1 first list of translations to compare
     * @param tr2 second list of translations to compare
     * @return true if translations equal; false otherwise
     */
    private static boolean translationsEquals(List<CMSCollectionTranslation> tr1, List<CMSCollectionTranslation> tr2) {
        if (tr1.size() == tr2.size()) {
            for (CMSCollectionTranslation tr : tr1) {
                CMSCollectionTranslation otr = tr2.stream()
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
     * populateLabels.
     */
    public void populateLabels() {
        List<String> languages = BeanUtils.getNavigationHelper().getSupportedLanguages();
        for (String language : languages) {
            if (getLabels().stream().noneMatch(label -> label.getLanguage().equalsIgnoreCase(language))) {
                addLabel(new CMSCollectionTranslation(language, ""));
            }
        }
    }

    /**
     * populateDescriptions.
     */
    public void populateDescriptions() {
        this.populateDescriptions(BeanUtils.getNavigationHelper().getSupportedLanguages());
    }

    /**
     * populateDescriptions.
     *
     * @param languages list of language codes to ensure descriptions exist for
     */
    public void populateDescriptions(List<String> languages) {
        logger.trace("populateDescriptions");
        for (String language : languages) {
            if (getDescriptions().stream().noneMatch(description -> description.getLanguage().equalsIgnoreCase(language))) {
                addDescription(new CMSCollectionTranslation(language, ""));
            }
        }
    }

    /**
     * hasMediaItem.
     *
     * @return a boolean.
     */
    public boolean hasMediaItem() {
        return getMediaItem() != null;
    }

    /**
     * hasRepresentativeWork.
     *
     * @return a boolean.
     */
    public boolean hasRepresentativeWork() {
        return StringUtils.isNotBlank(getRepresentativeWorkPI());
    }

    /**
     * hasImage.
     *
     * @return a boolean.
     */
    public boolean hasImage() {
        return hasRepresentativeWork() || hasMediaItem();
    }

    /**
     * Getter for the field <code>id</code>.
     *

     */
    public Long getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getSolrField() + "/" + getSolrFieldValue();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getSolrFieldValue();
    }

    /** {@inheritDoc} */
    @Override
    public URI getLinkURI() {
        return getLinkURI(BeanUtils.getRequest());
    }

    /** {@inheritDoc} */
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

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI() {
        logger.trace("getIconURI for {}: {}", getSolrFieldValue(), getRepresentativeWork().isPresent() ? getRepresentativeWorkPI() : "-");
        return getRepresentativeWork().map(work -> URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(work)))
                .orElse(Optional.ofNullable(getMediaItem()).map(item -> item.getIconURI()).orElse(null));
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

    /**
     * getRepresentativeWork.
     *
     * @return a {@link java.util.Optional} object.
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
     * Getter for the field <code>representativeWorkPI</code>.
     *

     */
    public String getRepresentativeWorkPI() {
        return representativeWorkPI;
    }

    /**
     * Setter for the field <code>representativeWorkPI</code>.
     *

     */
    public void setRepresentativeWorkPI(String representativeWorkPI) {
        this.representativeWorkPI = representativeWorkPI;
    }

    /** {@inheritDoc} */
    @Override
    public IMetadataValue getTranslationsForName() {
        Map<String, String> labels = getLabels().stream()
                .filter(l -> StringUtils.isNotBlank(l.getTranslationValue()))
                .collect(Collectors.toMap(l -> l.getLanguage(), l -> l.getTranslationValue()));
        if (labels.isEmpty()) {
            return ViewerResourceBundle.getTranslations(getSolrFieldValue());
        }

        return new MultiLanguageMetadataValue(labels);
    }

    @Override
    public IMetadataValue getTranslationsForDescription() {
        Map<String, String> descriptions = getDescriptions().stream()
                .filter(l -> StringUtils.isNotBlank(l.getTranslationValue()))
                .collect(Collectors.toMap(l -> l.getLanguage(), l -> l.getTranslationValue()));
        if (descriptions.isEmpty()) {
            return null;
        }

        return new MultiLanguageMetadataValue(descriptions);
    }

    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getImageFilter();
    }

    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getImageTypes();
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }

    @Override
    public boolean isComplete(Locale locale) {
        return !isEmpty(locale);
    }

    @Override
    public boolean isValid(Locale locale) {
        return !isEmpty(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("locale may not be null");
        }

        CMSCollectionTranslation translation = getDescriptionAsTranslation(locale.getLanguage());
        if (translation == null) {
            return true;
        }

        return StringUtils.isBlank(translation.getTranslationValue());
    }

    @Override
    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        logger.trace("setSelectedLocale: {}", locale);
        this.selectedLocale = locale;
    }

    
    public AccessPermission getAccessPermissionThumbnail() {
        return accessPermissionThumbnail;
    }

    
    public void setAccessPermissionThumbnail(AccessPermission accessPermissionThumbnail) {
        this.accessPermissionThumbnail = accessPermissionThumbnail;
    }
}
