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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import javax.persistence.Table;

import org.eclipse.persistence.annotations.PrivateOwned;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.misc.Translation;
import io.goobi.viewer.servlets.rest.serialization.TranslationListSerializer;

/**
 * @author florian
 *
 */
@Entity
@Table(name = "cs_questions")
@JsonInclude(Include.NON_EMPTY)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Campaign owner;

    /** Translated metadata. */
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    @JsonSerialize(using = TranslationListSerializer.class)
    private List<QuestionTranslation> translations = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_frequency")
    private TargetFrequency targetFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_selector", nullable = false)
    private TargetSelector targetSelector;

    public Question() {
    }

    public Question(Campaign owner) {
        this.owner = owner;
    }

    public Question(QuestionType questionType, TargetFrequency targetFrequency, TargetSelector targetSelector, Campaign owner) {
        this.questionType = questionType;
        this.targetFrequency = targetFrequency;
        this.targetSelector = targetSelector;
        this.owner = owner;
    }

    public String getLabel() {
        return Translation.getTranslation(translations, owner.getSelectedLocale().getLanguage(), "label");
    }

    public void setLabel(String label) {
        QuestionTranslation.setTranslation(translations, owner.getSelectedLocale().getLanguage(), label, "label", this);
    }

    public String getDescription() {
        return Translation.getTranslation(translations, owner.getSelectedLocale().getLanguage(), "description");
    }

    public void setDescription(String description) {
        QuestionTranslation.setTranslation(translations, owner.getSelectedLocale().getLanguage(), description, "description", this);
    }

    public String getHelp() {
        return Translation.getTranslation(translations, owner.getSelectedLocale().getLanguage(), "help");
    }

    public void setHelp(String help) {
        QuestionTranslation.setTranslation(translations, owner.getSelectedLocale().getLanguage(), help, "help", this);
    }

    @Deprecated
    public void setLabel(String lang, String value) {
        QuestionTranslation.setTranslation(translations, lang, value, "label", this);
    }

    @Deprecated
    public void setDescription(String lang, String value) {
        QuestionTranslation.setTranslation(translations, lang, value, "description", this);
    }
    
    public List<QuestionType> getAvailableQuestionTypes() {
        System.out.println(QuestionType.values().toString());
        return Arrays.asList(QuestionType.values());
    }

    public List<TargetSelector> getAvailableTargetSelectors() {
        return Arrays.asList(TargetSelector.values());
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the owner
     */
    public Campaign getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Campaign owner) {
        this.owner = owner;
    }

    /**
     * @return the translations
     */
    public List<QuestionTranslation> getTranslations() {
        return translations;
    }

    /**
     * @param translations the translations to set
     */
    public void setTranslations(List<QuestionTranslation> translations) {
        this.translations = translations;
    }

    /**
     * @return the questionType
     */
    public QuestionType getQuestionType() {
        return questionType;
    }

    /**
     * @param questionType the questionType to set
     */
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    /**
     * @return the targetType
     */
    public TargetFrequency getTargetFrequency() {
        return targetFrequency;
    }

    /**
     * @param targetType the targetType to set
     */
    public void setTargetFrequency(TargetFrequency targetFrequency) {
        this.targetFrequency = targetFrequency;
    }

    /**
     * @return the targetSelector
     */
    public TargetSelector getTargetSelector() {
        return targetSelector;
    }

    /**
     * @param targetSelector the targetSelector to set
     */
    public void setTargetSelector(TargetSelector targetSelector) {
        this.targetSelector = targetSelector;
    }
}
