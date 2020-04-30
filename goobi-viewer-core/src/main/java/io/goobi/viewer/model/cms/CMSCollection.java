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
package io.goobi.viewer.model.cms;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.viewer.BrowseElementInfo;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * A class representing persistent configurations for a collection. A collections is identified by a SOLR-field name and a label. The most common
 * SOLR-field is "DC" and label is the internal name of the collection to edit. This class allows setting a representative image, names in multiple
 * languages and a uri linking to a collection page.
 *
 * @author Florian Alpers
 */
@Entity
@Table(name = "cms_collections")
public class CMSCollection implements Comparable<CMSCollection>, BrowseElementInfo, CMSMediaHolder {

    private static final Logger logger = LoggerFactory.getLogger(CMSCollection.class);

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

    /**
     * <p>
     * Constructor for CMSCollection.
     * </p>
     */
    public CMSCollection() {

    }

    /**
     * Default constructor, creating a Collection from the identifying fields {@link io.goobi.viewer.model.cms.CMSCollection#solrField} and
     * {@link io.goobi.viewer.model.cms.CMSCollection#solrFieldValue}
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
     * <p>
     * Getter for the field <code>mediaItem</code>.
     * </p>
     *
     * @return the mediaItem
     */
    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }

    /** {@inheritDoc} */
    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    /**
     * <p>
     * Getter for the field <code>collectionUrl</code>.
     * </p>
     *
     * @return the collectionUri
     */
    public String getCollectionUrl() {
        return collectionUrl;
    }

    /**
     * <p>
     * Setter for the field <code>collectionUrl</code>.
     * </p>
     *
     * @param collectionUrl a {@link java.lang.String} object.
     */
    public void setCollectionUrl(String collectionUrl) {
        this.collectionUrl = collectionUrl;
    }

    /**
     * Adds a translation for the collection label
     *
     * @param label a {@link io.goobi.viewer.model.cms.CMSCollectionTranslation} object.
     */
    public void addLabel(CMSCollectionTranslation label) {
        label.setTag(LABEL_TAG);
        label.setOwner(this);
        translations.add(label);
    }

    /**
     * Adds a translation for the collection description
     *
     * @param description a {@link io.goobi.viewer.model.cms.CMSCollectionTranslation} object.
     */
    public void addDescription(CMSCollectionTranslation description) {
        description.setTag(DESCRIPTION_TAG);
        description.setOwner(this);
        translations.add(description);
    }

    /**
     * returns all translations of this page with the tag {@link #LABEL_TAG}
     *
     * @return all labels for this collections
     */
    public List<CMSCollectionTranslation> getLabels() {
        return translations.stream().filter(translation -> LABEL_TAG.equals(translation.getTag())).collect(Collectors.toList());
    }

    /**
     * returns all translations of this page with the tag {@link #DESCRIPTION_TAG}
     *
     * @return all descriptions for this collections
     */
    public List<CMSCollectionTranslation> getDescriptions() {
        return translations.stream().filter(translation -> DESCRIPTION_TAG.equals(translation.getTag())).collect(Collectors.toList());
    }

    /**
     * get the label for the given {@code language}, or an empty string if no matching label exists the language should be the language code of a
     * {@link java.util.Locale} and is case insensitive
     *
     * @param language a {@link java.lang.String} object.
     * @return The string value of the label of the given language, or an empty string
     */
    public String getLabel(String language) {
        return getLabels().stream()
                .filter(translation -> language.equalsIgnoreCase(translation.getLanguage()))
                .filter(translation -> StringUtils.isNotBlank(translation.getValue()))
                .findFirst()
                .map(translation -> translation.getValue())
                .orElse(ViewerResourceBundle.getTranslation(getSolrFieldValue(), null));
    }

    /**
     * get the label for the given {@code locale}, or an empty string if no matching label exists
     *
     * @param locale a {@link java.util.Locale} object.
     * @return The string value of the label of the given locale, or an empty string
     */
    public String getLabel(Locale locale) {
        return getLabel(locale.getLanguage());
    }

    /**
     * get the label for the current locale (given by {@link io.goobi.viewer.managedbeans.utils.BeanUtils#getLocale()}, or an empty string if no
     * matching label exists
     *
     * @return The string value of the label of the current locale, or an empty string
     */
    public String getLabel() {
        return getLabel(BeanUtils.getLocale());
    }

    /**
     * <p>
     * getLabelAsTranslation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSCollectionTranslation} object.
     */
    public CMSCollectionTranslation getLabelAsTranslation(String language) {
        return getLabels().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().orElse(null);
    }

    /**
     * <p>
     * getDescriptionAsTranslation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.CMSCollectionTranslation} object.
     */
    public CMSCollectionTranslation getDescriptionAsTranslation(String language) {
        return getDescriptions().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().orElse(null);
    }

    /**
     * <p>
     * setLabel.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @param value a {@link java.lang.String} object.
     */
    public void setLabel(String value, String language) {
        getLabels().stream().filter(label -> label.getLanguage().equalsIgnoreCase(language)).findFirst().ifPresent(label -> label.setValue(value));
    }

    /**
     * get the description for the given {@code language}, or an empty string if no matching description exists the language should be the language
     * code of a {@link java.util.Locale} and is case insensitive
     *
     * @param language a {@link java.lang.String} object.
     * @return The string value of the description of the given language, or an empty string
     */
    public String getDescription(String language) {
        return getDescriptions().stream()
                .filter(translation -> language.equalsIgnoreCase(translation.getLanguage()))
                //                .filter(translation -> StringUtils.isNotBlank(translation.getValue()))
                .findFirst()
                .map(translation -> translation.getValue())
                .orElse("");
        //                .orElse(ViewerResourceBundle.getTranslation(getSolrFieldValue() + "_DESCRIPTION", null));
    }

    /**
     * get the description for the given {@code locale}, or an empty string if no matching description exists
     *
     * @param locale a {@link java.util.Locale} object.
     * @return The string value of the description of the given locale, or an empty string
     */
    public String getDescription(Locale locale) {
        return getDescription(locale.getLanguage());
    }

    /**
     * get the description for the current locale (given by {@link io.goobi.viewer.managedbeans.utils.BeanUtils#getLocale()}, or an empty string if no
     * matching description exists
     *
     * @return The string value of the description of the current locale, or an empty string
     */
    public String getDescription() {
        return getDescription(BeanUtils.getLocale());
    }

    /**
     * <p>
     * setDescription.
     * </p>
     *
     * @param value a {@link java.lang.String} object.
     * @param language a {@link java.lang.String} object.
     */
    public void setDescription(String value, String language) {
        getDescriptions().stream()
                .filter(label -> label.getLanguage().equalsIgnoreCase(language))
                .findFirst()
                .ifPresent(desc -> desc.setValue(value));
    }

    /**
     * <p>
     * Getter for the field <code>solrField</code>.
     * </p>
     *
     * @return the solrField. Guaranteed to hold a non-blank value
     */
    public String getSolrField() {
        return solrField;
    }

    /**
     * <p>
     * Getter for the field <code>solrFieldValue</code>.
     * </p>
     *
     * @return the solrFieldValue. Guaranteed to hold a non-blank value
     */
    public String getSolrFieldValue() {
        return solrFieldValue;
    }

    /**
     * {@inheritDoc}
     *
     * Compares collection by the alphabatical sorting of their {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public int compareTo(CMSCollection other) {
        return getSolrFieldValue().compareTo(other.getSolrFieldValue());
    }

    /**
     * {@inheritDoc}
     *
     * Returns the hashCode of {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public int hashCode() {
        return getSolrFieldValue().hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * A {@link CMSCollection} is equal to any other object if that is also a CMSCollection and returns the same values for
     * {@link CMSCollection#getSolrField()} and {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return getSolrField().equals(((CMSCollection) obj).getSolrField())
                    && getSolrFieldValue().equals(((CMSCollection) obj).getSolrFieldValue());
        } else {
            return false;
        }
    }

    /**
     * <p>
     * populateLabels.
     * </p>
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
     * <p>
     * populateDescriptions.
     * </p>
     */
    public void populateDescriptions() {
        List<String> languages = BeanUtils.getNavigationHelper().getSupportedLanguages();
        for (String language : languages) {
            if (getDescriptions().stream().noneMatch(description -> description.getLanguage().equalsIgnoreCase(language))) {
                addDescription(new CMSCollectionTranslation(language, ""));
            }
        }
    }

    /**
     * <p>
     * hasMediaItem.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasMediaItem() {
        return getMediaItem() != null;
    }

    /**
     * <p>
     * hasRepresentativeWork.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasRepresentativeWork() {
        return StringUtils.isNotBlank(getRepresentativeWorkPI());
    }

    /**
     * <p>
     * hasImage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasImage() {
        return hasRepresentativeWork() || hasMediaItem();
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getSolrField() + "/" + getSolrFieldValue();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#getName()
     */
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return getLabel();
    }

    /** {@inheritDoc} */
    @Override
    public URI getLinkURI() {
        return getLinkURI(BeanUtils.getRequest());
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#getLinkURI(javax.servlet.http.HttpServletRequest)
     */
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
            URI uri = applicationUri.resolve(getCollectionUrl().replaceAll("^\\/", "").trim());
            return uri;
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#getIconURI()
     */
    /** {@inheritDoc} */
    @Override
    public URI getIconURI() {
        return getRepresentativeWork().map(work -> URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(work)))
                .orElse(Optional.ofNullable(getMediaItem()).map(item -> item.getIconURI()).orElse(getDefaultIcon(getSolrFieldValue())));

        //        return getMediaItem().getIconURI();
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI(int width, int height) {
        return getRepresentativeWork().map(work -> URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(work, width, height)))
                .orElse(Optional.ofNullable(getMediaItem()).map(item -> item.getIconURI(width, height)).orElse(getDefaultIcon(getSolrFieldValue())));

        //        return getMediaItem().getIconURI();
    }

    /** {@inheritDoc} */
    @Override
    public URI getIconURI(int size) {
        return getRepresentativeWork().map(work -> URI.create(BeanUtils.getImageDeliveryBean().getThumbs().getSquareThumbnailUrl(work, size)))
                .orElse(Optional.ofNullable(getMediaItem()).map(item -> item.getIconURI(size)).orElse(getDefaultIcon(getSolrFieldValue())));

        //        return getMediaItem().getIconURI();
    }

    /**
     * @param solrFieldValue2
     * @return
     */
    private URI getDefaultIcon(String collectionName) {
        return BeanUtils.getImageDeliveryBean()
                .getThumbs()
                .getThumbnailPath(DataManager.getInstance().getConfiguration().getDefaultBrowseIcon(collectionName));
    }

    /**
     * <p>
     * getRepresentativeWork.
     * </p>
     *
     * @return a {@link java.util.Optional} object.
     */
    public Optional<StructElement> getRepresentativeWork() {
        if (hasRepresentativeWork()) {
            try {
                SolrDocument doc = DataManager.getInstance().getSearchIndex().getDocumentByPI(getRepresentativeWorkPI());
                if (doc != null) {
                    return Optional.ofNullable(new StructElement(Long.parseLong((String) doc.getFieldValue(SolrConstants.IDDOC)), doc));
                }
            } catch (PresentationException | IndexUnreachableException e) {
                logger.error(e.toString(), e);
            }
        }
        return Optional.empty();
    }

    /**
     * <p>
     * Getter for the field <code>representativeWorkPI</code>.
     * </p>
     *
     * @return the representativeWorkPI
     */
    public String getRepresentativeWorkPI() {
        return representativeWorkPI;
    }

    /**
     * <p>
     * Setter for the field <code>representativeWorkPI</code>.
     * </p>
     *
     * @param representativeWorkPI the representativeWorkPI to set
     */
    public void setRepresentativeWorkPI(String representativeWorkPI) {
        this.representativeWorkPI = representativeWorkPI;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.viewer.BrowseElementInfo#getTranslationsForName()
     */
    /** {@inheritDoc} */
    @Override
    public IMetadataValue getTranslationsForName() {
        Map<String, String> labels = getLabels().stream()
                .filter(l -> StringUtils.isNotBlank(l.getValue()))
                .collect(Collectors.toMap(l -> l.getLanguage(), l -> l.getValue()));
        if (labels.isEmpty()) {
            return ViewerResourceBundle.getTranslations(getSolrField());
        } else {
            IMetadataValue value = new MultiLanguageMetadataValue(labels);
            return value;
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#getMediaFilter()
     */
    /** {@inheritDoc} */
    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getImageFilter();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#getMediaItemWrapper()
     */
    /** {@inheritDoc} */
    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<CMSMediaItem>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        } else {
            return null;
        }
    }

}
