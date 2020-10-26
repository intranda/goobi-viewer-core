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
package io.goobi.viewer.model.crowdsourcing.questions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.goobi.viewer.api.rest.serialization.TranslationListSerializer;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.misc.IPolyglott;
import io.goobi.viewer.model.misc.TranslationList;

/**
 * An annotation generator to create a specific type of annotation for a specific question. One or more of these may be contained within a
 * {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}
 *
 * @author florian
 */
@Entity
@Table(name = "cs_questions")
@JsonInclude(Include.NON_EMPTY)
public class Question {

    private static final String URI_ID_TEMPLATE =
            DataManager.getInstance().getConfiguration().getRestApiUrl() + "crowdsourcing/campaigns/{campaignId}/questions/{questionId}";
    private static final String URI_ID_REGEX = ".*/crowdsourcing/campaigns/(\\d+)/questions/(\\d+)/?$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private Campaign owner;

    /** Translated metadata. */
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    @JsonSerialize(using = TranslationListSerializer.class)
    private List<QuestionTranslation> translations = new ArrayList<>();
    @Transient 
    private TranslationList tempTranslations;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_selector", nullable = false)
    private TargetSelector targetSelector;

    @Column(name = "target_frequency", nullable = false)
    private int targetFrequency;

    /**
     * Empty constructor.
     */
    public Question() {
        tempTranslations = new TranslationList("text", IPolyglott.getCurrentLocale(), IPolyglott.getLocalesStatic());
    }

    /**
     * constructor setting the owning campaign
     *
     * @param owner a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     */
    public Question(Campaign owner) {
        this();
        this.owner = owner;
    }

    /**
     *
     * constructor setting the owning campaign as well ass the type of annotation to be generated
     *
     * @param questionType a {@link io.goobi.viewer.model.crowdsourcing.questions.QuestionType} object.
     * @param targetSelector a {@link io.goobi.viewer.model.crowdsourcing.questions.TargetSelector} object.
     * @param targetFrequency a int.
     * @param owner a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     */
    public Question(QuestionType questionType, TargetSelector targetSelector, int targetFrequency, Campaign owner) {
        this(owner);
        this.questionType = questionType;
        this.targetSelector = targetSelector;
        this.targetFrequency = targetFrequency;
    }
    
    @PrePersist
    public void onPrePersist() {
        serializeTranslations();
    }
    
    @PreUpdate
    public void onPreUpdate() {
        serializeTranslations();
    }

    
    @PostLoad
    public void onPostLoad() {
        deserializeTranslations();
    }

    private void serializeTranslations() {
        this.translations = this.tempTranslations.stream().map(t -> new QuestionTranslation(t, this)).collect(Collectors.toList());
    }
    
    private void deserializeTranslations() {
        this.tempTranslations = new TranslationList("text", BeanUtils.getLocale(), IPolyglott.getLocalesStatic());
        for (QuestionTranslation translation : translations) {
            Locale locale = Locale.forLanguageTag(translation.getLanguage());
            if(this.tempTranslations.hasLocale(locale)) {                
                this.tempTranslations.setValueForLocale(locale, translation.getValue());
            }
        }
    }
    
    /**
     * <p>
     * getAvailableQuestionTypes.
     * </p>
     *
     * @return available values of the QuestionType enum
     */
    @JsonIgnore
    public List<QuestionType> getAvailableQuestionTypes() {
        return Arrays.asList(QuestionType.values());
    }

    /**
     * <p>
     * getAvailableTargetSelectors.
     * </p>
     *
     * @return available values of the TargetSelector enum
     */
    @JsonIgnore
    public List<TargetSelector> getAvailableTargetSelectors() {
        return Arrays.asList(TargetSelector.values());
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    @JsonIgnore
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>owner</code>.
     * </p>
     *
     * @return the owner
     */
    public Campaign getOwner() {
        return owner;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     *
     * @param owner the owner to set
     */
    public void setOwner(Campaign owner) {
        this.owner = owner;
    }

    /**
     * <p>
     * Getter for the field <code>translations</code>.
     * </p>
     *
     * @return the translations
     */
    public TranslationList getTranslations() {
        return tempTranslations;
    }

    /**
     * <p>
     * Getter for the field <code>questionType</code>.
     * </p>
     *
     * @return the questionType
     */
    public QuestionType getQuestionType() {
        return questionType;
    }

    /**
     * <p>
     * Setter for the field <code>questionType</code>.
     * </p>
     *
     * @param questionType the questionType to set
     */
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    /**
     * <p>
     * Getter for the field <code>targetSelector</code>.
     * </p>
     *
     * @return the targetSelector
     */
    public TargetSelector getTargetSelector() {
        return targetSelector;
    }

    /**
     * <p>
     * Setter for the field <code>targetSelector</code>.
     * </p>
     *
     * @param targetSelector the targetSelector to set
     */
    public void setTargetSelector(TargetSelector targetSelector) {
        this.targetSelector = targetSelector;
    }

    /**
     * <p>
     * Getter for the field <code>targetFrequency</code>.
     * </p>
     *
     * @return the targetFrequency
     */
    public int getTargetFrequency() {
        return targetFrequency;
    }

    /**
     * <p>
     * Setter for the field <code>targetFrequency</code>.
     * </p>
     *
     * @param targetFrequency the targetFrequency to set
     */
    public void setTargetFrequency(int targetFrequency) {
        this.targetFrequency = targetFrequency;
    }

    /**
     * get the {@link #id} of a question from an URI identifier
     *
     * @param idAsURI a {@link java.net.URI} object.
     * @return a {@link java.lang.Long} object.
     */
    public static Long getQuestionId(URI idAsURI) {
        Matcher matcher = Pattern.compile(URI_ID_REGEX).matcher(idAsURI.toString());
        if (matcher.find()) {
            String idString = matcher.group(2);
            return Long.parseLong(idString);
        }

        return null;
    }

    /**
     * get the {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign#id} of the owning campaign from an URI identifier of a question
     *
     * @param idAsURI a {@link java.net.URI} object.
     * @return a {@link java.lang.Long} object.
     */
    public static Long getCampaignId(URI idAsURI) {
        Matcher matcher = Pattern.compile(URI_ID_REGEX).matcher(idAsURI.toString());
        if (matcher.find()) {
            String idString = matcher.group(1);
            return Long.parseLong(idString);
        }

        return null;
    }

    /**
     * <p>
     * getIdAsURI.
     * </p>
     *
     * @return The URI identifier for this question from the question and campaign id
     */
    @JsonProperty("id")
    public URI getIdAsURI() {
        return URI
                .create(URI_ID_TEMPLATE.replace("{campaignId}", this.getOwner().getId().toString()).replace("{questionId}", this.getId().toString()));
    }

}
