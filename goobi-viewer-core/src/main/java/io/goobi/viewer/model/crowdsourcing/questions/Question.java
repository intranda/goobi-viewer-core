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
package io.goobi.viewer.model.crowdsourcing.questions;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.converter.StringListConverter;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.normdata.NormdataAuthority;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * An annotation generator to create a specific type of annotation for a specific question. One or more of these may be contained within a
 * {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}
 *
 * @author florian
 */
@Entity
@Table(name = "cs_questions")
@JsonInclude(Include.NON_EMPTY)
public class Question implements Serializable {

    private static final long serialVersionUID = 1230983333894006339L;

    private static final Logger logger = LogManager.getLogger(Question.class);

    private static final String URI_ID_TEMPLATE =
            DataManager.getInstance().getConfiguration().getRestApiUrl() + "crowdsourcing/campaigns/{campaignId}/questions/{questionId}";
    private static final String URI_ID_REGEX = "/crowdsourcing/campaigns/(\\d{1,19})/questions/(\\d{1,19})/?$";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private Campaign owner;

    @Column(name = "text", nullable = true, columnDefinition = "LONGTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText text;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_selector", nullable = false)
    private TargetSelector targetSelector;

    @Column(name = "target_frequency", nullable = false)
    private int targetFrequency;

    @Column(name = "metadata_fields", nullable = true, columnDefinition = "LONGTEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> metadataFields = new ArrayList<>();

    @Transient
    private Map<String, Boolean> metadataFieldSelection = null;

    /**
     * Empty constructor.
     */
    public Question() {
        text = new TranslatedText();
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

    /**
     * 
     * @param orig
     */
    public Question(Question orig) {
        this.id = orig.id;
        this.owner = orig.owner;
        this.questionType = orig.questionType;
        this.targetFrequency = orig.targetFrequency;
        this.targetSelector = orig.targetSelector;
        this.text = new TranslatedText(orig.text, IPolyglott.getLocalesStatic(), IPolyglott.getCurrentLocale());
        this.metadataFields = new ArrayList<>(orig.metadataFields);
    }

    /**
     * Create a clone of the given question with the given campaign as owner
     *
     * @param orig
     * @param campaign
     */
    public Question(Question orig, Campaign campaign) {
        this(orig);
        this.owner = campaign;
    }

    /**
     * Call when metadata list changes
     */
    public void serializeMetadataFields() {
        if (QuestionType.METADATA.equals(getQuestionType())) {
            this.metadataFields =
                    getMetadataFieldSelection().entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(Collectors.toList());
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
     * @return the tempTranslations
     */
    public TranslatedText getText() {
        return text;
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
     * @return the metadataFields
     */
    public List<String> getMetadataFields() {
        return metadataFields;
    }

    /**
     * @param metadataFields the metadataFields to set
     */
    public void setMetadataFields(List<String> metadataFields) {
        this.metadataFields = new ArrayList<>(metadataFields);
    }

    public void addMetadataField(String field) {
        this.metadataFields.add(field);
    }

    public void removeMetadataField(String field) {
        this.metadataFields.remove(field);
    }

    /**
     * @param metadataToAdd the metadataToAdd to set
     */
    public void setMetadataToAdd(String metadataToAdd) {
        if (StringUtils.isNotBlank(metadataToAdd)) {
            addMetadataField(metadataToAdd);
        }
    }

    /**
     * @return the metadataToAdd
     */
    public String getMetadataToAdd() {
        return "";
    }

    /**
     *
     * @return a list of all "MD_" fields from solr
     * @throws IOException
     * @throws SolrServerException
     */
    @JsonIgnore
    public List<String> getAvailableMetadataFields() throws IndexUnreachableException {
        Locale locale = BeanUtils.getLocale();
        return DataManager.getInstance()
                .getSearchIndex()
                .getAllFieldNames()
                .stream()
                .filter(field -> field.startsWith("MD_"))
                .filter(field -> !field.endsWith("_UNTOKENIZED"))
                .map(field -> field.replaceAll("_LANG_.*", ""))
                //        .filter(field -> !this.metadataFields.contains(field))
                .distinct()
                .sorted((f1, f2) -> Messages.translate(f1, locale).compareToIgnoreCase(Messages.translate(f2, locale)))
                .collect(Collectors.toList());
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
     * @should extract id correctly
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
     * @should extract id correctly
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

    /**
     * Currently only returns GND authority data.
     *
     * @return Normdata authority data if the question type is NORMDATA, otherwise null
     */
    @JsonProperty("authorityData")
    public NormdataAuthority getAuthorityData() {
        if (QuestionType.NORMDATA.equals(getQuestionType())) {
            return NormdataAuthority.GND;
        }

        return null;
    }

    /**
     * @return the metadataFieldSelection
     * @throws IOException
     * @throws SolrServerException
     */
    public Map<String, Boolean> getMetadataFieldSelection() {
        if (this.metadataFieldSelection == null) {
            try {
                this.metadataFieldSelection =
                        getAvailableMetadataFields().stream().collect(Collectors.toMap(field -> field, field -> this.metadataFields.contains(field)));
            } catch (IndexUnreachableException e) {
                //If the possible fields cannot be retrieved from solr, just show the already selected ones
                logger.error("Failed to load all possible metadata fields: {}", e.getMessage());
                this.metadataFieldSelection = this.metadataFields.stream().collect(Collectors.toMap(field -> field, field -> Boolean.TRUE));
            }
        }
        return metadataFieldSelection;
    }

    public List<String> getSelectedMetadataFields() {
        return this.getMetadataFieldSelection().entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(Collectors.toList());
    }

}
