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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
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
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.goobi.viewer.api.rest.serialization.TranslationListSerializer;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.SolrQueryValidator;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSMediaHolder;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.misc.IPolyglott;
import io.goobi.viewer.model.misc.Translation;
import io.goobi.viewer.model.security.ILicenseType;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.user.User;

/**
 *
 * A Campaign is a template to create annotations of specific types for a limited set of target resources and optionally by limited user group within
 * a limited time frame. The types of annotations created are determined by the {@link Question Questions} contained in this Campaign
 *
 * @author florian
 */
@Entity
@Table(name = "cs_campaigns")
@JsonInclude(Include.NON_EMPTY)
public class Campaign implements CMSMediaHolder, ILicenseType, IPolyglott {

    /**
     * The visibility of the campaign to other users
     */
    public enum CampaignVisibility {
        /**
         * Hidden for all users except in the admin backend
         */
        PRIVATE,
        /**
         * Hidden to all except users having the appropritate rights
         */
        @Deprecated
        RESTRICTED,
        /**
         * Visible by all users
         */
        PUBLIC,
        /**
         * Visible to all, but no creation of annotations possible
         */
        @Deprecated
        FINISHED;

        public static CampaignVisibility getByName(String name) {
            for (CampaignVisibility viz : CampaignVisibility.values()) {
                if (viz.name().equals(name)) {
                    return viz;
                }
            }

            return null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(Campaign.class);

    private static final String URI_ID_TEMPLATE =
            DataManager.getInstance().getConfiguration().getRestApiUrl().replace("/rest", "/api/v1") + "crowdsourcing/campaigns/{id}";
    private static final String URI_ID_REGEX = ".*/crowdsourcing/campaigns/(\\d+)/?$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false)
    @JsonIgnore
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_updated")
    @JsonIgnore
    private Date dateUpdated;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    @JsonIgnore
    private CampaignVisibility visibility = CampaignVisibility.PRIVATE;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_start")
    @JsonIgnore
    private Date dateStart;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_end")
    @JsonIgnore
    private Date dateEnd;

    /** Media item reference for media content items. */
    @JoinColumn(name = "media_item_id")
    @JsonIgnore
    private CMSMediaItem mediaItem;

    @Column(name = "solr_query", nullable = false)
    @JsonIgnore
    private String solrQuery;

    @Column(name = "permalink")
    @JsonIgnore
    private String permalink;

    /**
     * The id of the parent page. This is usually the id (as String) of the parent cms page, or NULL if the parent page is the start page The system
     * could be extended to set any page type name as parent page (so this page is a breadcrumb-child of e.g. "image view")
     */
    @Column(name = "breadcrumb_parent_page")
    @JsonIgnore
    private String breadcrumbParentCmsPageId = null;

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    @JsonSerialize(using = TranslationListSerializer.class)
    private List<CampaignTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @PrivateOwned
    private List<Question> questions = new ArrayList<>();

    /** Status entry for each record that has been worked on in the context of this campaign. The PI of each record is the map key. */
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    @MapKeyColumn(name = "pi", insertable = false, updatable = false) // CampaignRecordStatistic.pi may not be writable here
    @JsonIgnore
    private Map<String, CampaignRecordStatistic> statistics = new HashMap<>();

    @Transient
    @JsonIgnore
    private Locale selectedLocale;

    /**
     * temporary storage for results from {@link #solrQuery}, reduced to PIs. Will be initialized if required by {@link #getSolrQueryResults()} and
     * reset to null by {@link #setSolrQuery(String)}
     */
    @Transient
    @JsonIgnore
    private List<String> solrQueryResults = null;

    /**
     * Empty constructor.
     */
    public Campaign() {
        this.selectedLocale = BeanUtils.getLocale();
    }

    /**
     * Locale constructor.
     *
     * @param selectedLocale a {@link java.util.Locale} object.
     */
    public Campaign(Locale selectedLocale) {
        this.selectedLocale = selectedLocale;
    }
    
    public Campaign(Campaign orig) {
        
        this.id = orig.id;
        this.translations = orig.translations.stream().map(t -> new CampaignTranslation(t, this)).collect(Collectors.toList());
        this.breadcrumbParentCmsPageId = orig.breadcrumbParentCmsPageId;
        this.dateCreated = orig.dateCreated;
        this.dateUpdated = orig.dateUpdated;
        this.dateEnd = orig.dateEnd;
        this.dateStart = orig.dateStart;
        this.mediaItem = orig.mediaItem; //no need for deep copy since it can't be changed within the campaign
        this.permalink = orig.permalink;
        this.selectedLocale = orig.selectedLocale;
        this.solrQuery = orig.solrQuery;
        this.solrQueryResults = orig.solrQueryResults;
        this.visibility = orig.visibility;
        this.statistics = orig.statistics; //no need for deep copy since it can't be changed in campaign editor
        this.questions = orig.questions.stream().map(q -> new Question(q, this)).collect(Collectors.toList());
    }

    /**
     * No @PrePersist annotation because it is called explicitly in {@link IDAO#addCampaign(Campaign)}
     */
    public void onPrePersist() {
        this.questions.forEach(Question::onPrePersist);
    }
    
    /**
     * No @PreUpdate annotation because it is called explicitly in {@link IDAO#updateCampaign(Campaign)}
     */
    public void onPreUpdate() {
        this.questions.forEach(Question::onPreUpdate);
    }
    
    @PostLoad
    public void onPostLoad() {
        this.questions.forEach(Question::onPostLoad);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Campaign other = (Campaign) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    /**
     * <p>
     * getCampaignVisibilityValues.
     * </p>
     *
     * @return available values of the CampaignVisibility enum
     */
    @JsonIgnore
    public List<CampaignVisibility> getCampaignVisibilityValues() {
        return Arrays.asList(CampaignVisibility.PUBLIC, CampaignVisibility.PRIVATE);
    }

    /**
     * <p>
     * getNumRecords.
     * </p>
     *
     * @return total number of records encompassed by the configured solrQuery
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public long getNumRecords() throws IndexUnreachableException {
        try {
            return getSolrQueryResults().size();
        } catch (PresentationException e) {
            logger.warn("Error getting number of records for campaign:" + e.toString());
            return 0;
        }
    }

    /**
     * <p>
     * getNumRecordsForStatus.
     * </p>
     *
     * @param status a {@link java.lang.String} object.
     * @return number of records with the given status
     */
    public long getNumRecordsForStatus(String status) {
        if (status == null) {
            return 0;
        }

        long count = 0;
        for (String pi : statistics.keySet()) {
            CampaignRecordStatistic statistic = statistics.get(pi);
            if (statistic.getStatus().name().equals(status)) {
                count++;
            }
        }

        return count;
    }

    /**
     * <p>
     * getNumRecordsToAnnotate.
     * </p>
     *
     * @return Number of records whose status is neither REVIEW nor FINISHED
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public long getNumRecordsToAnnotate() throws IndexUnreachableException {
        long all = getNumRecords();
        long count = 0;
        for (String pi : statistics.keySet()) {
            CampaignRecordStatistic statistic = statistics.get(pi);
            switch (statistic.getStatus()) {
                case REVIEW:
                case FINISHED:
                    count++;
                default:
                    break;
            }
        }

        return all - count;
    }

    /**
     * Determines the number of distinct users that have created or reviewed annotations in the context of this campaign.
     *
     * @return number of users
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getContributorCount() throws DAOException {
        Set<Long> userIds = new HashSet<>();
        for (String pi : statistics.keySet()) {
            for (User u : statistics.get(pi).getAnnotators()) {
                userIds.add(u.getId());
            }
            for (User u : statistics.get(pi).getReviewers()) {
                userIds.add(u.getId());
            }
        }

        return userIds.size();
    }

    /**
     * FINISHED records in percent
     *
     * @return percentage of records marked as finished relative to the total number or records
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public int getProgress() throws IndexUnreachableException, PresentationException {
        float numRecords = getNumRecords();
        float finished = getNumRecordsForStatus(CampaignRecordStatus.FINISHED.getName());
        return Math.round(finished / numRecords * 100);
    }

    /**
     * Returns the number of whole days between today and the starting date for this campaign.
     *
     * @return whole days left between today and dateStart; -1 if no dateStart
     * @should return -1 if no dateStart
     * @should calculate days correctly
     */
    public long getDaysBeforeStart() {
        if (dateStart == null) {
            return -1;
        }

        LocalDateTime now = LocalDate.now().atStartOfDay();
        LocalDateTime start = DateTools.convertDateToLocalDateTimeViaInstant(dateStart);
        return Math.max(0L, Duration.between(now, start).toDays());
    }

    /**
     * Returns the number of whole days between today and the end date for this campaign. Because this method only returns the number of whole days
     * left, its main purpose is for displaying the number of days to the user, and it shouldn't be used for access control.
     *
     * @return whole days left between today and dateEnd; -1 if no dateEnd
     * @should return -1 if no dateEnd
     * @should calculate days correctly
     */
    public long getDaysLeft() {
        if (dateEnd == null) {
            return -1;
        }

        LocalDateTime now = LocalDate.now().atStartOfDay();
        LocalDateTime end = DateTools.convertDateToLocalDateTimeViaInstant(dateEnd);
        return Math.max(0L, Duration.between(now, end).toDays());
    }

    /**
     * <p>
     * getDaysLeftAsString.
     * </p>
     *
     * @return number of days left as string; infinity symbol if no dateEnd
     */
    public String getDaysLeftAsString() {
        if (getDateEnd() != null) {
            long days = getDaysLeft();
            return Long.toString(days);
        }
        return "\u221e";
    }

    /**
     * <p>
     * isHasStarted.
     * </p>
     *
     * @return true if dateStart lies after now; false otherwise
     * @should return true if dateStart null
     * @should return true if dateStart equals now
     * @should return true if dateStart before now
     * @should return false if dateStart after now
     */
    public boolean isHasStarted() {
        if (dateStart == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = DateTools.convertDateToLocalDateTimeViaInstant(dateStart);

        return now.isEqual(start) || now.isAfter(start);
    }

    /**
     * <p>
     * isHasEnded.
     * </p>
     *
     * @return true if dateEnd lies before now; false otherwise
     * @should return false if dateEnd null
     * @should return false if dateEnd after now
     * @should return true if dateEnd before now
     */
    public boolean isHasEnded() {
        if (dateEnd == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = DateTools.convertDateToLocalDateTimeViaInstant(dateEnd);

        return now.isAfter(end);
    }

    /**
     * Checks whether the given user may annotate or review records based on the given status.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return true if the given user is allowed to perform the action associated with the given status; false otherwise
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isUserAllowedAction(User user, CampaignRecordStatus status) throws PresentationException, IndexUnreachableException, DAOException {
        // logger.trace("isUserAllowedAction: {}", status);
        if (CampaignVisibility.PUBLIC.equals(visibility)) {
            return true;
        }
        if (user == null || status == null) {
            return false;
        }
        switch (status) {
            case ANNOTATE:
                return user.isHasCrowdsourcingPrivilege(IPrivilegeHolder.PRIV_CROWDSOURCING_ANNOTATE_CAMPAIGN);
            case REVIEW:
                return user.isHasCrowdsourcingPrivilege(IPrivilegeHolder.PRIV_CROWDSOURCING_REVIEW_CAMPAIGN);
            default:
                return false;
        }
    }

    @Override
    public String getName() {
        return getTitle();
    }

    /**
     * Returns the title value in the current language of the campaign object (current tab). This is meant to be used for campaign editing only, since
     * the language will not be in sync with the selected locale!
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getTitle() {
        return Translation.getTranslation(translations, selectedLocale.getLanguage(), "title");
    }

    /**
     * <p>
     * setTitle.
     * </p>
     *
     * @param title a {@link java.lang.String} object.
     * @should set value correctly
     */
    public void setTitle(String title) {
        CampaignTranslation.setTranslation(translations, selectedLocale.getLanguage(), title, "title", this);
    }

    /**
     * Returns the menu title value in the current language of the campaign object (current tab). This is meant to be used for campaign editing only,
     * since the language will not be in sync with the selected locale!
     *
     * @should return correct value
     * @return a {@link java.lang.String} object.
     */
    public String getMenuTitle() {
        return Translation.getTranslation(translations, selectedLocale.getLanguage(), "menu_title");
    }

    /**
     * <p>
     * getMenuTitleOrElseTitle.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMenuTitleOrElseTitle() {
        String title = getMenuTitle();
        if (StringUtils.isBlank(title)) {
            title = getTitle();
        }
        return title;
    }

    /**
     * <p>
     * getMenuTitleOrElseTitle.
     * </p>
     *
     * @param lang a {@link java.lang.String} object.
     * @param useFallback a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String getMenuTitleOrElseTitle(String lang, boolean useFallback) {
        String title = getMenuTitle(lang, useFallback);
        if (StringUtils.isBlank(title)) {
            title = getTitle(lang, useFallback);
        }
        return title;
    }

    /**
     * <p>
     * setMenuTitle.
     * </p>
     *
     * @should set value correctly
     * @param menuTitle a {@link java.lang.String} object.
     */
    public void setMenuTitle(String menuTitle) {
        CampaignTranslation.setTranslation(translations, selectedLocale.getLanguage(), menuTitle, "menu_title", this);
    }

    /**
     * Returns the description value in the current language of the campaign object (current tab). This is meant to be used for campaign editing only,
     * since the language will not be in sync with the selected locale!
     *
     * @return Description value in the current language in the campaign object
     * @should return correct value
     */
    public String getDescription() {
        return Translation.getTranslation(translations, selectedLocale.getLanguage(), "description");
    }

    /**
     * <p>
     * setDescription.
     * </p>
     *
     * @should set value correctly
     * @param description a {@link java.lang.String} object.
     */
    public void setDescription(String description) {
        CampaignTranslation.setTranslation(translations, selectedLocale.getLanguage(), description, "description", this);
    }

    /**
     * <p>
     * getTitle.
     * </p>
     *
     * @param lang a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getTitle(String lang) {
        return getTitle(lang, false);
    }

    /**
     * <p>
     * getTitle.
     * </p>
     *
     * @param lang a {@link java.lang.String} object.
     * @return the title of the given language or if it doesn't exist the title of the default language
     * @param useFallback a boolean.
     */
    public String getTitle(String lang, boolean useFallback) {
        return Translation.getTranslation(translations, lang, "title", useFallback);
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @param lang a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getDescription(String lang) {
        return getDescription(lang, false);
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @param lang a {@link java.lang.String} object.
     * @param useFallback a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String getDescription(String lang, boolean useFallback) {
        return Translation.getTranslation(translations, lang, "description", useFallback);
    }

    /**
     * <p>
     * getMenuTitle.
     * </p>
     *
     * @param lang a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getMenuTitle(String lang) {
        return getMenuTitle(lang, false);
    }

    /**
     * <p>
     * getMenuTitle.
     * </p>
     *
     * @param lang a {@link java.lang.String} object.
     * @param useFallback a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String getMenuTitle(String lang, boolean useFallback) {
        return Translation.getTranslation(translations, lang, "menu_title", useFallback);
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
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @param idAsURI a {@link java.net.URI} object.
     * @return a {@link java.lang.Long} object.
     */
    public static Long getId(URI idAsURI) {

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
     * @return a {@link java.net.URI} object.
     */
    @JsonProperty("id")
    public URI getIdAsURI() {
        return URI.create(URI_ID_TEMPLATE.replace("{id}", this.getId().toString()));
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
     * Getter for the field <code>dateCreated</code>.
     * </p>
     *
     * @return the dateCreated
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * <p>
     * Setter for the field <code>dateCreated</code>.
     * </p>
     *
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * <p>
     * Getter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @return the dateUpdated
     */
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * <p>
     * Setter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * <p>
     * Getter for the field <code>visibility</code>.
     * </p>
     *
     * @return the visibility
     */
    public CampaignVisibility getVisibility() {
        return visibility;
    }

    /**
     * <p>
     * Setter for the field <code>visibility</code>.
     * </p>
     *
     * @param visibility the visibility to set
     */
    public void setVisibility(CampaignVisibility visibility) {
        this.visibility = visibility;
    }

    /**
     * <p>
     * Getter for the field <code>dateStart</code>.
     * </p>
     *
     * @return the dateStart
     */
    public Date getDateStart() {
        return dateStart;
    }

    /**
     * <p>
     * Setter for the field <code>dateStart</code>.
     * </p>
     *
     * @param dateStart the dateStart to set
     */
    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * <p>
     * getDateStartString.
     * </p>
     *
     * @return formatted ISO string representation of stateStart
     */
    public String getDateStartString() {
        if (dateStart == null) {
            return null;
        }

        return DateTools.format(dateStart, DateTools.formatterISO8601Date, false);
    }

    /**
     * <p>
     * setDateStartString.
     * </p>
     *
     * @param dateStartString a {@link java.lang.String} object.
     * @should parse string correctly
     */
    public void setDateStartString(String dateStartString) {
        logger.trace("setDateStartString: {}", dateStartString);
        if (dateStartString != null) {
            this.dateStart = DateTools.parseDateFromString(dateStartString);
        } else {
            this.dateStart = null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>dateEnd</code>.
     * </p>
     *
     * @return the dateEnd
     */
    public Date getDateEnd() {
        return dateEnd;
    }

    /**
     * <p>
     * Setter for the field <code>dateEnd</code>.
     * </p>
     *
     * @param dateEnd the dateEnd to set
     */
    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * <p>
     * getDateEndString.
     * </p>
     *
     * @return formatted ISO string representation of dateEnd
     */
    public String getDateEndString() {
        if (dateEnd == null) {
            return null;
        }

        return DateTools.format(dateEnd, DateTools.formatterISO8601Date, false);
    }

    /**
     * <p>
     * setDateEndString.
     * </p>
     *
     * @param dateEndString a {@link java.lang.String} object.
     * @should parse string correctly
     */
    public void setDateEndString(String dateEndString) {
        if (dateEndString != null) {
            this.dateEnd = DateTools.parseDateFromString(dateEndString);
        } else {
            this.dateEnd = null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>solrQuery</code>.
     * </p>
     *
     * @return the solrQuery
     */
    public String getSolrQuery() {
        return solrQuery;
    }

    /**
     * <p>
     * Setter for the field <code>solrQuery</code>.
     * </p>
     *
     * @param solrQuery the solrQuery to set
     */
    public void setSolrQuery(String solrQuery) {
        this.solrQuery = solrQuery;
        this.solrQueryResults = null;
    }

    /**
     * <p>
     * Getter for the field <code>permalink</code>.
     * </p>
     *
     * @return the permalink
     */
    public String getPermalink() {
        return permalink;
    }

    /**
     * <p>
     * Setter for the field <code>permalink</code>.
     * </p>
     *
     * @param permalink the permalink to set
     */
    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    /**
     * <p>
     * Getter for the field <code>breadcrumbParentCmsPageId</code>.
     * </p>
     *
     * @return the breadcrumbParentCmsPageId
     */
    public String getBreadcrumbParentCmsPageId() {
        return breadcrumbParentCmsPageId;
    }

    /**
     * <p>
     * Setter for the field <code>breadcrumbParentCmsPageId</code>.
     * </p>
     *
     * @param breadcrumbParentCmsPageId the breadcrumbParentCmsPageId to set
     */
    public void setBreadcrumbParentCmsPageId(String breadcrumbParentCmsPageId) {
        this.breadcrumbParentCmsPageId = breadcrumbParentCmsPageId;
    }

    /**
     * <p>
     * Getter for the field <code>translations</code>.
     * </p>
     *
     * @return the translations
     */
    public List<CampaignTranslation> getTranslations() {
        return translations;
    }

    /**
     * <p>
     * Setter for the field <code>translations</code>.
     * </p>
     *
     * @param translations the translations to set
     */
    public void setTranslations(List<CampaignTranslation> translations) {
        this.translations = translations;
    }

    /**
     * <p>
     * Getter for the field <code>questions</code>.
     * </p>
     *
     * @return the questions
     */
    public List<Question> getQuestions() {
        return questions;
    }

    /**
     * <p>
     * Setter for the field <code>questions</code>.
     * </p>
     *
     * @param questions the questions to set
     */
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    /**
     * <p>
     * Getter for the field <code>statistics</code>.
     * </p>
     *
     * @return the statistics
     */
    public Map<String, CampaignRecordStatistic> getStatistics() {
        return statistics;
    }

    /**
     * <p>
     * Setter for the field <code>statistics</code>.
     * </p>
     *
     * @param statistics the statistics to set
     */
    public void setStatistics(Map<String, CampaignRecordStatistic> statistics) {
        this.statistics = statistics;
    }

    /**
     * <p>
     * Getter for the field <code>selectedLocale</code>.
     * </p>
     *
     * @return the selectedLocale
     */
    @Override
    public Locale getSelectedLocale() {
        return selectedLocale;
    }

    /**
     * <p>
     * Setter for the field <code>selectedLocale</code>.
     * </p>
     *
     * @param selectedLocale the selectedLocale to set
     */
    @Override
    public void setSelectedLocale(Locale selectedLocale) {
        this.selectedLocale = selectedLocale;
    }

    /**
     * Return true if the campaign is ready for use. For this, the title in the default language must exists 
     * and there must be at least one question
     * 
     */
    @JsonIgnore
    public boolean isReady() {
        return isComplete(IPolyglott.getDefaultLocale()) && !getQuestions().isEmpty();
    }
    
    @Override
    public boolean isComplete(Locale locale) {
        return StringUtils.isNotBlank(getTitle(locale.getLanguage(), false));
    }

    /**
     * Get the targetIdentifier to a random PI from the Solr query result list.
     *
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus} object.
     * @param piToIgnore a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getRandomizedTarget(CampaignRecordStatus status, String piToIgnore) throws PresentationException, IndexUnreachableException {
        User user = BeanUtils.getUserBean().getUser();
        List<String> pis = getSolrQueryResults().stream()
                .filter(result -> !result.equals(piToIgnore))
                .filter(result -> isRecordStatus(result, status))
                .filter(result -> isEligibleToEdit(result, status, user))
                .collect(Collectors.toList());
        if (pis.isEmpty()) {
            return "";
        }
        String pi = pis.get(new Random(System.nanoTime()).nextInt(pis.size()));
        return pi;
    }

    /**
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    private List<String> getSolrQueryResults() throws PresentationException, IndexUnreachableException {
        if (this.solrQueryResults == null) {
            String query = "+" + SolrConstants.ISWORK + ":true";
            // Validate campaign query before adding it
            try {
                SolrQueryValidator.getHitCount(solrQuery);
                query += " +(" + solrQuery + ")";
            } catch (SolrServerException e) {
                logger.error(e.getMessage());
            } catch (IOException e) {
                logger.error(e.getMessage());
            } catch (RemoteSolrException e) {
                logger.error(e.getMessage());
            }
            this.solrQueryResults = DataManager.getInstance()
                    .getSearchIndex()
                    .search(query, Collections.singletonList(SolrConstants.PI))
                    .stream()
                    .map(doc -> doc.getFieldValue(SolrConstants.PI).toString())
                    .collect(Collectors.toList());

        }
        return this.solrQueryResults;
    }
    
    public void resetSolrQueryResults() {
        this.solrQueryResults = null;
    }

    /**
     * Check if the given user may annotate/review (depending on status) a specific pi within this campaign
     *
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return true if
     *         <ul>
     *         <li>the status is {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus#REVIEW REVIEW} and
     *         the user is not contained in the annotaters list</li> or
     *         <li>the status is {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus#ANNOTATE ANNOTATE}
     *         and the user is not contained in the reviewers list</li> or
     *         <li>The user is admin</li> or
     *         <li>The user is null</li>
     * @param pi a {@link java.lang.String} object.
     */
    public boolean isEligibleToEdit(String pi, CampaignRecordStatus status, User user) {
        if (user != null) {
            if (user.isSuperuser()) {
                return true;
            }
            if (status.equals(CampaignRecordStatus.ANNOTATE)) {
                return !Optional.ofNullable(this.statistics.get(pi)).map(s -> s.getReviewers()).orElse(Collections.emptyList()).contains(user);
            } else if (status.equals(CampaignRecordStatus.REVIEW)) {
                return !Optional.ofNullable(this.statistics.get(pi)).map(s -> s.getAnnotators()).orElse(Collections.emptyList()).contains(user);
            } else {
                return true;
            }
        }

        return true;
    }

    /**
     * check if the given user is eligible to review any records
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return true if there are any records in review status for which {@link #isEligibleToEdit(String, CampaignRecordStatus, User)} returns true
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean hasRecordsToReview(User user) throws PresentationException, IndexUnreachableException {
        return getSolrQueryResults().stream()
                .filter(result -> isRecordStatus(result, CampaignRecordStatus.REVIEW))
                .filter(result -> isEligibleToEdit(result, CampaignRecordStatus.REVIEW, user))
                .count() > 0;
    }

    /**
     * check if the given user is eligible to annotate any records
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return true if there are any records in annotate status for which {@link #isEligibleToEdit(String, CampaignRecordStatus, User)} returns true
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean hasRecordsToAnnotate(User user) throws PresentationException, IndexUnreachableException {
        return getSolrQueryResults().stream()
                .filter(result -> isRecordStatus(result, CampaignRecordStatus.ANNOTATE))
                .filter(result -> isEligibleToEdit(result, CampaignRecordStatus.ANNOTATE, user))
                .count() > 0;
    }

    /**
     * check if the user is allowed to annotate the given pi for this campaign
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param pi a {@link java.lang.String} object.
     * @return true if the pi is ready for annotation and the user hasn't reviewed it or is a superuser
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean mayAnnotate(User user, String pi) throws PresentationException, IndexUnreachableException {
        return isRecordStatus(pi, CampaignRecordStatus.ANNOTATE) && isEligibleToEdit(pi, CampaignRecordStatus.ANNOTATE, user);
    }

    /**
     * check if the user is allowed to review the given pi for this campaign
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param pi a {@link java.lang.String} object.
     * @return true if the pi is ready for review and the user hasn't annotated it or is a superuser
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean mayReview(User user, String pi) throws PresentationException, IndexUnreachableException {
        return isRecordStatus(pi, CampaignRecordStatus.REVIEW) && isEligibleToEdit(pi, CampaignRecordStatus.REVIEW, user);

    }

    /**
     * @param result
     * @return true if record status for the given pi equals status; false otherwise
     */
    private boolean isRecordStatus(String pi, CampaignRecordStatus status) {
        return Optional.ofNullable(statistics.get(pi)).map(CampaignRecordStatistic::getStatus).orElse(CampaignRecordStatus.ANNOTATE).equals(status);
    }

    /**
     * <p>
     * getRecordStatus.
     * </p>
     *
     * @return record status for the given pi
     * @param pi a {@link java.lang.String} object.
     */
    public CampaignRecordStatus getRecordStatus(String pi) {
        return Optional.ofNullable(statistics.get(pi)).map(CampaignRecordStatistic::getStatus).orElse(CampaignRecordStatus.ANNOTATE);

    }

    /**
     * Updates record status in the campaign statistics.
     *
     * @param pi a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus} object.
     * @param user a {@link java.util.Optional} object.
     */
    public void setRecordStatus(String pi, CampaignRecordStatus status, Optional<User> user) {
        CampaignRecordStatistic statistic = statistics.get(pi);
        if (statistic == null) {
            statistic = new CampaignRecordStatistic();
            statistic.setOwner(this);
            statistic.setDateCreated(new Date());
            statistic.setStatus(CampaignRecordStatus.ANNOTATE);
        }
        if (CampaignRecordStatus.ANNOTATE.equals(statistic.getStatus())) {
            user.ifPresent(statistic::addAnnotater);
        } else {
            user.ifPresent(statistic::addReviewer);
        }
        statistic.setPi(pi);
        statistic.setStatus(status);
        statistic.setDateUpdated(new Date());
        statistics.put(pi, statistic);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#setMediaItem(io.goobi.viewer.model.cms.CMSMediaItem)
     */
    /** {@inheritDoc} */
    @Override
    public void setMediaItem(CMSMediaItem item) {
        this.mediaItem = item;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#getMediaItem()
     */
    /** {@inheritDoc} */
    @Override
    public CMSMediaItem getMediaItem() {
        return this.mediaItem;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#getMediaFilter()
     */
    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    public String getMediaFilter() {
        return CmsMediaBean.getImageFilter();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#hasMediaItem()
     */
    /** {@inheritDoc} */
    @Override
    public boolean hasMediaItem() {
        return this.mediaItem != null;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#getMediaItemWrapper()
     */
    /** {@inheritDoc} */
    @Override
    @JsonIgnore
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(mediaItem, true,
                    mediaItem.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }
}
