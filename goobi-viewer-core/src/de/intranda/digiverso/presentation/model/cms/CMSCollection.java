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
package de.intranda.digiverso.presentation.model.cms;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.org.apache.xml.internal.serializer.utils.Messages;

import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import sun.misc.resources.Messages_zh_HK;

/**
 * A class representing persistent configurations for a collection.
 * A collections is identified by a SOLR-field name and a label. The most common SOLR-field is "DC" and label is the internal 
 * name of the collection to edit.
 * This class allows setting a representative image, names in multiple languages and a uri linking to a collection page.
 * 
 * @author Florian Alpers
 *
 */
@Entity
@Table(name = "cms_collections")
public class CMSCollection implements Comparable<CMSCollection>{
    
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
    
    @Column(name = "collection_url")
    private String collectionUrl;
    
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<Translation> translations = new ArrayList<>();
    
        
    public CMSCollection() {
        
    }
    
    /**
     * Default constructor, creating a Collection from the identifying fields {@link CMSCollection#solrField} and {@link CMSCollection#solrFieldValue}
     * 
     * @param solrField         The name of the SOLR field holding the values for the collection
     * @param solrFieldValue    The value of the solrField identifying this collection
     * 
     * @throws IllegalArgumentException     If either argument returns true for {@link StringUtils#isBlank(CharSequence)}
     */
    public CMSCollection(String solrField, String solrFieldValue) {
        if(StringUtils.isBlank(solrField) || StringUtils.isBlank(solrFieldValue)) {
            throw new IllegalArgumentException("The constructor paramters of CMSCollections may not be null, empty or blank");
        }
        this.solrField = solrField;
        this.solrFieldValue = solrFieldValue;
    }
    
    /**
     * @return the mediaItem
     */
    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }
    
    /**
     * @param mediaItem the mediaItem to set
     */
    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }
    
    /**
     * @return the collectionUri
     */
    public String getCollectionUrl() {
        return collectionUrl;
    }
    
    /**
     * @param collectionUri the collectionUri to set
     */
    public void setCollectionUrl(String collectionUrl) {
        this.collectionUrl = collectionUrl;
    }
    
    /**
     * Adds a translation for the collection label
     * 
     * @param label
     */
    public void addLabel(Translation label) {
        label.setTag(LABEL_TAG);
        translations.add(label);
    }
    
    /**
     * Adds a translation for the collection description
     * 
     * @param label
     */
    public void addDescription(Translation description) {
        description.setTag(DESCRIPTION_TAG);
        translations.add(description);
    }
    
    /**
     * returns all translations of this page with the tag {@link #LABEL_TAG}
     * 
     * @return  all labels for this collections
     */
    public List<Translation> getLabels() {
        return translations.stream().filter(translation -> LABEL_TAG.equals(translation.getTag())).collect(Collectors.toList());
    }
    
    /**
     * returns all translations of this page with the tag {@link #DESCRIPTION_TAG}
     * 
     * @return  all descriptions for this collections
     */
    public List<Translation> getDescriptions() {
        return translations.stream().filter(translation -> DESCRIPTION_TAG.equals(translation.getTag())).collect(Collectors.toList());
    }
    
    /**
     * get the label for the given {@code language}, or an empty string if no matching label exists
     * the language should be the language code of a {@link Locale} and is case insensitive
     * 
     * @param language
     * @return  The string value of the label of the given language, or an empty string
     */
    public String getLabel(String language) {
        return getLabels().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().map(translation -> translation.getValue()).orElse(Helper.getTranslation(getSolrFieldValue(), null));
    }
    
    /**
     * get the label for the given {@code locale}, or an empty string if no matching label exists
     * 
     * @param locale
     * @return  The string value of the label of the given locale, or an empty string
     */
    public String getLabel(Locale locale) {
        return getLabel(locale.getLanguage());
    }
    
    /**
     * get the label for the current locale (given by {@link BeanUtils#getLocale()}, or an empty string if no matching label exists
     * 
     * @return  The string value of the label of the current locale, or an empty string
     */
    public String getLabel() {
        return getLabel(BeanUtils.getLocale());
    }
    
    
    public Translation getLabelAsTranslation(String language) {
        return getLabels().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().orElse(null);
    }
    

    /**
     * @param label
     * @param language
     * @return
     */
    public void setLabel(String value, String language) {
        getLabels().stream().filter(label -> label.getLanguage().equalsIgnoreCase(language)).findFirst().ifPresent(label -> label.setValue(value));
    }
    
    /**
     * get the description for the given {@code language}, or an empty string if no matching description exists
     * the language should be the language code of a {@link Locale} and is case insensitive
     * 
     * @param language
     * @return  The string value of the description of the given language, or an empty string
     */
    public String getDescription(String language) {
        return getDescriptions().stream().filter(translation -> language.equalsIgnoreCase(translation.getLanguage())).findFirst().map(translation -> translation.getValue()).orElse(Helper.getTranslation(getSolrFieldValue() + "_DESCRIPTION", null));
    }
    
    /**
     * get the description for the given {@code locale}, or an empty string if no matching description exists
     * 
     * @param locale
     * @return  The string value of the description of the given locale, or an empty string
     */
    public String getDescription(Locale locale) {
        return getDescription(locale.getLanguage());
    }
    
    /**
     * get the description for the current locale (given by {@link BeanUtils#getLocale()}, or an empty string if no matching description exists
     * 
     * @return  The string value of the description of the current locale, or an empty string
     */
    public String getDescription() {
        return getDescription(BeanUtils.getLocale());
    }
    
    /**
     * @return the solrField. Guaranteed to hold a non-blank value
     */
    public String getSolrField() {
        return solrField;
    }
    
    /**
     * @return the solrFieldValue. Guaranteed to hold a non-blank value
     */
    public String getSolrFieldValue() {
        return solrFieldValue;
    }

    /**
     * Compares collection by the alphabatical sorting of their {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public int compareTo(CMSCollection other) {
        return getSolrFieldValue().compareTo(other.getSolrFieldValue());
    }
    
    /**
     * Returns the hashCode of {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public int hashCode() {
        return getSolrFieldValue().hashCode();
    }
    
    /**
     * A {@link CMSCollection} is equal to any other object if that is also a CMSCollection and returns the same
     * values for {@link CMSCollection#getSolrField()} and {@link CMSCollection#getSolrFieldValue()}
     */
    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj.getClass().equals(this.getClass())) {
            return getSolrField().equals(((CMSCollection) obj).getSolrField()) &&
                    getSolrFieldValue().equals(((CMSCollection) obj).getSolrFieldValue());
        } else {
            return false;
        }
    }
    
    public void populateLabels() {
        List<String> languages = BeanUtils.getNavigationHelper().getSupportedLanguages();
        for (String language : languages) {
            if(getLabels().stream().noneMatch(label -> label.getLanguage().equalsIgnoreCase(language))) {
                addLabel(new Translation(language, ""));
            }
        }
    }
    
    public void populateDescriptions() {
        List<String> languages = BeanUtils.getNavigationHelper().getSupportedLanguages();
        for (String language : languages) {
            if(getDescriptions().stream().noneMatch(description -> description.getLanguage().equalsIgnoreCase(language))) {
                addDescription(new Translation(language, ""));
            }
        }
    }
    
    public boolean hasMediaItem() {
        return getMediaItem() != null;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getSolrField() + "/" + getSolrFieldValue();
    }

    

}
