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
package io.goobi.viewer.model.crowdsourcing.campaigns;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.goobi.viewer.api.rest.serialization.TranslationListSerializer;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.SolrQueryValidator;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.log.LogMessage;
import io.goobi.viewer.model.security.ILicenseType;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.Translation;
import io.goobi.viewer.solr.SolrConstants;

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
public class Campaign implements CMSMediaHolder, ILicenseType, IPolyglott, Serializable {

    private static final long serialVersionUID = 3169479611322444516L;

    /**
     * The visibility of the campaign to other users
     */
    public enum CampaignVisibility {
        /**
         * Hidden for all users except in the admin backend
         */
        PRIVATE,
        /**
         * Hidden to all except users having the appropriate rights
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

    public enum ReviewMode {
        REQUIRE_REVIEW("label__require_review"),
        NO_REVIEW("label__no_review"),
        LIMIT_REVIEW_TO_USERGROUP("label__limit_review_to_usergroup");

        private final String label;

        private ReviewMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return this.label;
        }
    }

    /**
     * Statistics calculation mode (status per record or per record page).
     */
    public enum StatisticMode {
        RECORD("label__crowdsourcing_campaign_statistic_mode_record"),
        PAGE("label__crowdsourcing_campaign_statistic_mode_page");

        private final String label;

        private StatisticMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return this.label;
        }
    }

    private static final Logger logger = LogManager.getLogger(Campaign.class);

    private static final String URI_ID_TEMPLATE =
            DataManager.getInstance().getConfiguration().getRestApiUrl().replace("/rest", "/api/v1") + "crowdsourcing/campaigns/{id}";
    private static final String URI_ID_REGEX = ".*/crowdsourcing/campaigns/(\\d+)/?$";

    private static final Random random = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    private Long id;

    @Column(name = "date_created", nullable = false)
    @JsonIgnore
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    @JsonIgnore
    private LocalDateTime dateUpdated;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    @JsonIgnore
    private CampaignVisibility visibility = CampaignVisibility.PRIVATE;

    @Column(name = "date_start")
    @JsonIgnore
    private LocalDateTime dateStart;

    @Column(name = "date_end")
    @JsonIgnore
    private LocalDateTime dateEnd;

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

    @Column(name = "show_log")
    private boolean showLog = false;

    @Column(name = "limit_to_group")
    private boolean limitToGroup = false;

    @ManyToOne
    @JoinColumn(name = "user_group_id")
    @JsonIgnore
    private UserGroup userGroup;

    @Column(name = "review_mode")
    private ReviewMode reviewMode = ReviewMode.REQUIRE_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "statistic_mode")
    private StatisticMode statisticMode = StatisticMode.RECORD;

    @ManyToOne
    @JoinColumn(name = "revewier_user_group_id")
    @JsonIgnore
    private UserGroup reviewerUserGroup;

    @Column(name = "time_period_enabled")
    @JsonIgnore
    private boolean timePeriodEnabled = false;

    /**
     * If this is set to true, annotations generated by this campaign get the campaign name as ACCESSCONDITION
     */
    @Column(name = "restrict_annotation_access")
    @JsonIgnore
    private boolean restrictAnnotationAccess = false;

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

    /** Status entry for each record that has been worked on in the context of this campaign. The PI of each record is the map key. */
    @OneToMany(mappedBy = "campaign", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    @JsonIgnore
    private List<CampaignLogMessage> logMessages = new ArrayList<>();

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

    @Transient
    @JsonIgnore
    private Integer pageCount = null;

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
        this.pageCount = orig.pageCount;
        this.visibility = orig.visibility;
        this.statistics = orig.statistics; //no need for deep copy since it can't be changed in campaign editor
        this.showLog = orig.showLog;
        this.logMessages = orig.logMessages; //no need for deep copy since it can't be changed in campaign editor
        this.questions = orig.questions.stream().map(q -> new Question(q, this)).collect(Collectors.toList());
        this.restrictAnnotationAccess = orig.restrictAnnotationAccess;
        this.timePeriodEnabled = orig.timePeriodEnabled;
        this.userGroup = orig.userGroup;
        this.limitToGroup = orig.limitToGroup;
        this.reviewMode = orig.reviewMode;
        this.reviewerUserGroup = orig.reviewerUserGroup;
        this.statisticMode = orig.statisticMode;
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
            if (StatisticMode.RECORD.equals(getStatisticMode())) {
                return getSolrQueryResults().size();
            }
            return getTotalPageCount();
        } catch (PresentationException e) {
            logger.warn("Error getting number of records for campaign:" + e.toString());
            return 0;
        }
    }

    /**
     * @return the total number of pages within the records found by {@link #solrQuery}
     */
    private long getTotalPageCount() {
        if (this.pageCount == null) {
            String query = "+" + SolrConstants.ISWORK + ":true +" + SolrConstants.BOOL_IMAGEAVAILABLE + ":true";
            // Validate campaign query before adding it
            try {
                query += " +(" + solrQuery + ")";
                int pages = DataManager.getInstance()
                        .getSearchIndex()
                        .search(query, Collections.singletonList(SolrConstants.NUMPAGES))
                        .stream()
                        .filter(doc -> doc.getFieldValue(SolrConstants.NUMPAGES) != null)
                        .mapToInt(doc -> (Integer) doc.getFieldValue(SolrConstants.NUMPAGES))
                        .sum();
                this.pageCount = pages;
            } catch (RemoteSolrException | PresentationException | IndexUnreachableException e) {
                logger.error(e.getMessage());
                return 0;
            }
        }
        return this.pageCount;
    }

    /**
     * <p>
     * getNumRecordsForStatus.
     * </p>
     *
     * @param status a {@link java.lang.String} object.
     * @return number of records with the given status
     * @should do record-based count correctly
     * @should do page-based count correctly
     */
    public long getNumRecordsForStatus(String status) {
        if (status == null) {
            return 0;
        }

        long count = 0;
        for (Entry<String, CampaignRecordStatistic> entry : statistics.entrySet()) {
            CampaignRecordStatistic statistic = entry.getValue();
            if (statistic == null) {
                continue;
            }
            if (StatisticMode.PAGE.equals(statisticMode)) {
                // Page-based count
                for (String key : statistic.getPageStatistics().keySet()) {
                    if (statistic.getPageStatistics().get(key).getStatus().name().equals(status)) {
                        count++;
                    }
                }
            } else if (statistic.getStatus() != null && statistic.getStatus().name().equals(status)) {
                // Record-based count
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
        for (Entry<String, CampaignRecordStatistic> entry : statistics.entrySet()) {
            CampaignRecordStatistic statistic = entry.getValue();
            switch (statistic.getStatus()) {
                case REVIEW:
                case FINISHED:
                    count++;
                    break;
                default:
                    break;
            }
        }

        return all - count;
    }

    /**
     * Determines the number of distinct users that have created or reviewed annotations in the context of this campaign.
     *
     * @return number of users who either annotated or reviewed annotations
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getContributorCount() throws DAOException {
        Set<Long> userIds = new HashSet<>();
        if (StatisticMode.PAGE.equals(statisticMode)) {
            for (String pi : statistics.keySet()) {
                for (String key : statistics.get(pi).getPageStatistics().keySet()) {
                    CampaignRecordPageStatistic pageStatistic = statistics.get(pi).getPageStatistics().get(key);
                    for (User u : pageStatistic.getAnnotators()) {
                        userIds.add(u.getId());
                    }
                    for (User u : pageStatistic.getReviewers()) {
                        userIds.add(u.getId());
                    }
                }
            }
        } else {
            for (String pi : statistics.keySet()) {
                for (User u : statistics.get(pi).getAnnotators()) {
                    userIds.add(u.getId());
                }
                for (User u : statistics.get(pi).getReviewers()) {
                    userIds.add(u.getId());
                }
            }
        }

        return userIds.size();
    }

    /**
     *
     * @return true if this campaign has at least one annotation; false otherwise
     */
    public boolean isHasAnnotations() {
        if (StatisticMode.PAGE.equals(statisticMode)) {
            for (String pi : statistics.keySet()) {
                for (String key : statistics.get(pi).getPageStatistics().keySet()) {
                    if (!statistics.get(pi).getPageStatistics().get(key).getAnnotators().isEmpty()) {
                        return true;
                    }
                }
            }
        } else {
            for (String pi : statistics.keySet()) {
                if (!statistics.get(pi).getAnnotators().isEmpty()) {
                    return true;
                }
            }
        }

        return false;
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
        float finished = getNumRecordsForStatus(CrowdsourcingStatus.FINISHED.getName());
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
        return Math.max(0L, Duration.between(now, dateStart).toDays());
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
        return Math.max(0L, Duration.between(now, dateEnd).toDays());
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
     * @should return true if timePeriodEnabled false
     */
    public boolean isHasStarted() {
        if (dateStart == null || !timePeriodEnabled) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        return now.isEqual(dateStart) || now.isAfter(dateStart);
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
     * @should return false if timePeriodEnabled false
     */
    public boolean isHasEnded() {
        if (dateEnd == null || !timePeriodEnabled) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(dateEnd);
    }

    /**
     * Checks whether the given user may annotate or review records based on the given status.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return true if the given user is allowed to perform the action associated with the given status; false otherwise
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return true if campaign public
     * @should return false if outside time period
     * @should return true if user member of group
     * @should return true if user owner of group
     * @should return false if user not in group
     */
    public boolean isUserAllowedAction(User user, CrowdsourcingStatus status) throws PresentationException, IndexUnreachableException, DAOException {
        // logger.trace("isUserAllowedAction: {}", status);
        if (status == null) {
            return false;
        }
        if (getQuestions().isEmpty()) {
            return false;
        }
        if (!isHasStarted() || isHasEnded()) {
            return false;
        }
        if (user != null && user.isSuperuser()) {
            return true;
        }
        switch (status) {
            case ANNOTATE:
                if (CampaignVisibility.PUBLIC.equals(visibility)) {
                    return true;
                } else if (user == null) {
                    return false;
                } else if (CampaignVisibility.PRIVATE.equals(visibility) && isGroupLimitActive()) {
                    return userGroup.getMembersAndOwner().contains(user);
                } else {
                    return true;
                }
            case REVIEW:
                if (isReviewGroupLimitActive()) {
                    return user != null && reviewerUserGroup.getMembersAndOwner().contains(user);
                } else if (CampaignVisibility.PUBLIC.equals(visibility)) {
                    return true;
                } else if (user == null) {
                    return false;
                } else {
                    return true;
                }
            default:
                return false;
        }
    }

    /**
     *
     * @param user User for whom to check access
     * @return
     * @throws DAOException
     * @should return false if user null
     * @should return true if user superuser
     * @should return false if visibility not private
     * @should return false if boolean false
     * @should return false if userGroup not set
     * @should return true if user owner
     * @should return true if user member
     */
    public boolean isUserMayEdit(User user) throws DAOException {
        if (user == null) {
            return false;
        }
        if (user.isSuperuser()) {
            return true;
        }
        if (!(CampaignVisibility.PRIVATE.equals(visibility) && isGroupLimitActive())) {
            return false;
        }

        return userGroup.getMembersAndOwner().contains(user);
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

    public String getDisplayTitle() {
        return getTitle(BeanUtils.getLocale().getLanguage(), true);
    }

    public String getDisplayDescription() {
        return getDescription(BeanUtils.getLocale().getLanguage(), true);

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

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @param idAsURI a {@link java.net.URI} object.
     * @return a {@link java.lang.Long} object.
     */
    public static Long getId(URI idAsURI) {

        Matcher matcher = Pattern.compile(URI_ID_REGEX).matcher(idAsURI.toString()); //NOSONAR  no catastrophic backtracking detected
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
    @JsonProperty("url")
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
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * <p>
     * Setter for the field <code>dateCreated</code>.
     * </p>
     *
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
     * <p>
     * Getter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @return the dateUpdated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * <p>
     * Setter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
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
     *
     * @return
     */
    public LocalDate getDateOnlyStart() {
        if (dateStart == null) {
            return null;
        }

        return dateStart.toLocalDate();
    }

    /**
     *
     * @param dateStart
     */
    public void setDateOnlyStart(LocalDate dateStart) {
        if (dateStart == null) {
            this.dateStart = null;
        } else {
            this.dateStart = LocalDateTime.of(dateStart, LocalTime.of(0, 0));
        }
    }

    /**
     * <p>
     * Getter for the field <code>dateStart</code>.
     * </p>
     *
     * @return the dateStart
     */
    public LocalDateTime getDateStart() {
        return dateStart;
    }

    /**
     * <p>
     * Setter for the field <code>dateStart</code>.
     * </p>
     *
     * @param dateStart the dateStart to set
     */
    public void setDateStart(LocalDateTime dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * <p>
     * getDateStartString.
     * </p>
     *
     * @return formatted ISO string representation of stateStart
     */
    @Deprecated
    public String getDateStartString() {
        if (dateStart == null) {
            return null;
        }

        return DateTools.format(dateStart, DateTools.FORMATTERISO8601DATE, false);
    }

    /**
     * <p>
     * setDateStartString.
     * </p>
     *
     * @param dateStartString a {@link java.lang.String} object.
     * @should parse string correctly
     */
    @Deprecated
    public void setDateStartString(String dateStartString) {
        logger.trace("setDateStartString: {}", dateStartString);
        if (dateStartString != null) {
            this.dateStart = DateTools.parseDateTimeFromString(dateStartString, false);
        } else {
            this.dateStart = null;
        }
    }

    /**
     *
     * @return
     */
    public LocalDate getDateOnlyEnd() {
        if (dateEnd == null) {
            return null;
        }

        return dateEnd.toLocalDate();
    }

    /**
     *
     * @param dateEnd
     */
    public void setDateOnlyEnd(LocalDate dateEnd) {
        if (dateEnd == null) {
            this.dateEnd = null;
        } else {
            this.dateEnd = LocalDateTime.of(dateEnd, LocalTime.of(0, 0));
        }
    }

    /**
     * <p>
     * Getter for the field <code>dateEnd</code>.
     * </p>
     *
     * @return the dateEnd
     */
    public LocalDateTime getDateEnd() {
        return dateEnd;
    }

    /**
     * <p>
     * Setter for the field <code>dateEnd</code>.
     * </p>
     *
     * @param dateEnd the dateEnd to set
     */
    public void setDateEnd(LocalDateTime dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * <p>
     * getDateEndString.
     * </p>
     *
     * @return formatted ISO string representation of dateEnd
     */
    @Deprecated
    public String getDateEndString() {
        if (dateEnd == null) {
            return null;
        }

        return DateTools.format(dateEnd, DateTools.FORMATTERISO8601DATE, false);
    }

    /**
     * <p>
     * setDateEndString.
     * </p>
     *
     * @param dateEndString a {@link java.lang.String} object.
     * @should parse string correctly
     */
    @Deprecated
    public void setDateEndString(String dateEndString) {
        if (dateEndString != null) {
            this.dateEnd = DateTools.parseDateTimeFromString(dateEndString, false);
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
        this.pageCount = null;
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
     * Return true if the campaign is ready for use. For this, the title in the default language must exists and there must be at least one question
     *
     */
    @JsonIgnore
    public boolean isReady() {
        return isComplete(IPolyglott.getDefaultLocale()) && !getQuestions().isEmpty();
    }

    @Override
    public boolean isComplete(Locale locale) {
        if (isValid(locale)) {
            String defaultLanguage = IPolyglott.getDefaultLocale().getLanguage();
            if (locale.getLanguage().equals(defaultLanguage)) {
                return true;
            } else if (StringUtils.isBlank(getDescription(defaultLanguage))) {
                return true;
            } else {
                return StringUtils.isNotBlank(getDescription(locale.getLanguage()));
            }
        }

        return false;
    }

    /**
     * @return true if the title is not empty for the given locale
     */
    @Override
    public boolean isValid(Locale locale) {
        return StringUtils.isNotBlank(getTitle(locale.getLanguage(), false));

    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.translations.IPolyglott#isEmpty(java.util.Locale)
     */
    @Override
    public boolean isEmpty(Locale locale) {
        return StringUtils.isBlank(getDescription(locale.getLanguage())) &&
                StringUtils.isBlank(getTitle(locale.getLanguage()));
    }

    /**
     * Get the targetIdentifier to a random PI from the Solr query result list.
     *
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus} object.
     * @param piToIgnore a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getRandomizedTarget(CrowdsourcingStatus status, String piToIgnore, User user)
            throws PresentationException, IndexUnreachableException {
        List<String> pis = getSolrQueryResults().stream()
                .filter(result -> !result.equals(piToIgnore))
                .filter(result -> isRecordStatus(result, status))
                .filter(result -> isEligibleToEdit(result, status, user))
                .collect(Collectors.toList());
        if (pis.isEmpty()) {
            return "";
        }
        String pi = pis.get(random.nextInt(pis.size()));
        return pi;
    }

    /**
     * Get the targetIdentifier to a random PI from the Solr query result list.
     *
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus} object.
     * @param piToIgnore a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String getNextTarget(CrowdsourcingStatus status, String currentPi, User user) throws PresentationException, IndexUnreachableException {
        List<String> piList = getSolrQueryResults().stream()
                .filter(result -> isRecordStatus(result, status))
                .filter(result -> isEligibleToEdit(result, status, user))
                .collect(Collectors.toList());
        int currentIndex = piList.indexOf(currentPi);
        if (piList.isEmpty()) {
            return "";
        }
        if (currentIndex + 1 < piList.size()) {
            return piList.get(currentIndex + 1);
        } else if (currentIndex != 0) {
            return piList.get(0);
        } else {
            return "";
        }
    }

    /**
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    List<String> getSolrQueryResults() throws PresentationException, IndexUnreachableException {
        if (this.solrQueryResults == null) {
            String query = "+" + SolrConstants.ISWORK + ":true +" + SolrConstants.BOOL_IMAGEAVAILABLE + ":true";
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
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus} object.
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return true if
     *         <ul>
     *         <li>the status is {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus#REVIEW REVIEW} and
     *         the user is not contained in the annotators list</li> or
     *         <li>the status is {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus#ANNOTATE ANNOTATE}
     *         and the user is not contained in the reviewers list</li> or
     *         <li>The user is admin</li> or
     *         <li>The user is null</li>
     * @param pi a {@link java.lang.String} object.
     */
    public boolean isEligibleToEdit(String pi, CrowdsourcingStatus status, User user) {
        if (user != null) {
            if (user.isSuperuser()) {
                return true;
            }
            // TODO page-based
            if (status.equals(CrowdsourcingStatus.ANNOTATE)) {
                return !Optional.ofNullable(this.statistics.get(pi)).map(s -> s.getReviewers()).orElse(Collections.emptyList()).contains(user);
            } else if (status.equals(CrowdsourcingStatus.REVIEW)) {
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
     * @return true if there are any records in review status for which {@link #isEligibleToEdit(String, CrowdsourcingStatus, User)} returns true
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean hasRecordsToReview(User user) throws PresentationException, IndexUnreachableException {
        return getSolrQueryResults().stream()
                .filter(result -> isRecordStatus(result, CrowdsourcingStatus.REVIEW))
                .filter(result -> isEligibleToEdit(result, CrowdsourcingStatus.REVIEW, user))
                .count() > 0;
    }

    /**
     * check if the given user is eligible to annotate any records
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @return true if there are any records in annotate status for which {@link #isEligibleToEdit(String, CrowdsourcingStatus, User)} returns true
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public boolean hasRecordsToAnnotate(User user) throws PresentationException, IndexUnreachableException {
        return getSolrQueryResults().stream()
                .filter(result -> isRecordStatus(result, CrowdsourcingStatus.ANNOTATE))
                .filter(result -> isEligibleToEdit(result, CrowdsourcingStatus.ANNOTATE, user))
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
        return isRecordStatus(pi, CrowdsourcingStatus.ANNOTATE) && isEligibleToEdit(pi, CrowdsourcingStatus.ANNOTATE, user);
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
        return isRecordStatus(pi, CrowdsourcingStatus.REVIEW) && isEligibleToEdit(pi, CrowdsourcingStatus.REVIEW, user);

    }

    /**
     * @param result
     * @return true if record status for the given pi equals status; false otherwise. If no record
     */
    boolean isRecordStatus(String pi, CrowdsourcingStatus status) {
        boolean ret = Optional.ofNullable(statistics.get(pi))
                .map(stat -> StatisticMode.RECORD.equals(this.statisticMode) ? status.equals(stat.getStatus()) : stat.containsPageStatus(status))
                .orElse(CrowdsourcingStatus.ANNOTATE.equals(status));
        return ret;
    }

    /**
     * <p>
     * getRecordStatus.
     * </p>
     *
     * @return record status for the given pi
     * @param pi a {@link java.lang.String} object.
     */
    public CrowdsourcingStatus getRecordStatus(String pi) {
        return Optional.ofNullable(statistics.get(pi)).map(CampaignRecordStatistic::getStatus).orElse(CrowdsourcingStatus.ANNOTATE);
    }

    public CrowdsourcingStatus getPageStatus(String pi, int page) {
        return Optional.ofNullable(statistics.get(pi))
                .map(s -> s.getPageStatistics().get(pi + "_" + Integer.toString(page)))
                .map(CampaignRecordPageStatistic::getStatus)
                .orElse(CrowdsourcingStatus.ANNOTATE);
    }

    /**
     *
     * @return
     * @should return true if boolean true and userGroup not null
     * @should return false if boolean false
     * @should return false if userGroup null
     */
    public boolean isGroupLimitActive() {
        return limitToGroup && userGroup != null;
    }

    public boolean isReviewGroupLimitActive() {
        return ReviewMode.LIMIT_REVIEW_TO_USERGROUP.equals(this.reviewMode) && reviewerUserGroup != null;
    }

    public boolean isReviewModeActive() {
        return !ReviewMode.NO_REVIEW.equals(this.reviewMode);
    }

    /**
     * Updates record status in the campaign statistics.
     *
     * @param pi a {@link java.lang.String} object.
     * @param status a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus} object.
     * @param user a {@link java.util.Optional} object.
     */
    public void setRecordStatus(String pi, CrowdsourcingStatus status, Optional<User> user) {
        CampaignRecordStatistic statistic = statistics.get(pi);
        if (statistic == null) {
            statistic = new CampaignRecordStatistic();
            statistic.setOwner(this);
            statistic.setDateCreated(LocalDateTime.now());
            statistic.setStatus(CrowdsourcingStatus.ANNOTATE);
        }
        if (CrowdsourcingStatus.ANNOTATE.equals(statistic.getStatus())) {
            user.ifPresent(statistic::addAnnotater);
        } else {
            user.ifPresent(statistic::addReviewer);
        }
        statistic.setPi(pi);
        statistic.setStatus(status);
        statistic.setDateUpdated(LocalDateTime.now());
        statistics.put(pi, statistic);
    }

    /**
     *
     * @param pi
     * @param page
     * @param status
     * @param user
     */
    public void setRecordPageStatus(String pi, int page, CrowdsourcingStatus status, Optional<User> user) {
        // logger.trace("setRecordPageStatus: {}/{}", pi, page); // Sonar considers this log msg a security issue, so leave it commented out when not needed
        LocalDateTime now = LocalDateTime.now();
        CampaignRecordStatistic statistic = statistics.get(pi);
        if (statistic == null) {
            statistic = new CampaignRecordStatistic();
            statistic.setPi(pi);
            statistic.setOwner(this);
            statistic.setDateCreated(now);
            statistic.setStatus(CrowdsourcingStatus.ANNOTATE);
        }

        String key = pi + "_" + page;
        CampaignRecordPageStatistic pageStatistic = statistic.getPageStatistics().get(key);
        if (pageStatistic == null) {
            pageStatistic = new CampaignRecordPageStatistic();
            pageStatistic.setOwner(statistic);
            pageStatistic.setPi(pi);
            pageStatistic.setPage(page);
            pageStatistic.setDateCreated(now);
            pageStatistic.setKey(key);
            pageStatistic.setStatus(CrowdsourcingStatus.ANNOTATE);
            statistic.getPageStatistics().put(key, pageStatistic);
        }
        if (CrowdsourcingStatus.ANNOTATE.equals(pageStatistic.getStatus())) {
            user.ifPresent(pageStatistic::addAnnotater);
        } else {
            user.ifPresent(pageStatistic::addReviewer);
        }

        pageStatistic.setStatus(status);
        pageStatistic.setDateUpdated(now);
        statistic.setDateUpdated(now);
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

    @Override
    @JsonIgnore
    public String getMediaTypes() {
        return CmsMediaBean.getImageTypes();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.cms.CMSMediaHolder#hasMediaItem()
     */
    /** {@inheritDoc} */
    @Override
    public boolean hasMediaItem() {
        return this.mediaItem != null;
    }

    /**
     * @param showLog the showLog to set
     */
    public void setShowLog(boolean showLog) {
        this.showLog = showLog;
    }

    /**
     * @return the showLog
     */
    public boolean isShowLog() {
        return showLog;
    }

    /**
     * @return the limitToGroup
     */
    public boolean isLimitToGroup() {
        return limitToGroup;
    }

    /**
     * @param limitToGroup the limitToGroup to set
     */
    public void setLimitToGroup(boolean limitToGroup) {
        this.limitToGroup = limitToGroup;
    }

    /**
     * @return the reviewMode
     */
    public ReviewMode getReviewMode() {
        return reviewMode;
    }

    /**
     * @param reviewMode the reviewMode to set
     */
    public void setReviewMode(ReviewMode reviewMode) {
        this.reviewMode = reviewMode;
    }

    /**
     * @return the statisticMode
     */
    public StatisticMode getStatisticMode() {
        return statisticMode != null ? statisticMode : StatisticMode.RECORD;
    }

    /**
     * @param statisticMode the statisticMode to set
     */
    public void setStatisticMode(StatisticMode statisticMode) {
        this.statisticMode = statisticMode;
    }

    /**
     * @return the userGroup
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * @param userGroup the userGroup to set
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
    }

    /**
     * @return the reviewerUserGroup
     */
    public UserGroup getReviewerUserGroup() {
        return reviewerUserGroup;
    }

    /**
     * @param reviewerUserGroup the reviewerUserGroup to set
     */
    public void setReviewerUserGroup(UserGroup reviewerUserGroup) {
        this.reviewerUserGroup = reviewerUserGroup;
    }

    /**
     * @return the timePeriodEnabled
     */
    public boolean isTimePeriodEnabled() {
        return timePeriodEnabled;
    }

    /**
     * @return the {@link #restrictAnnotationAccess}
     */
    public boolean isRestrictAnnotationAccess() {
        return restrictAnnotationAccess;
    }

    /**
     * @param restrictAnnotationAccess the {@link #restrictAnnotationAccess} to set
     */
    public void setRestrictAnnotationAccess(boolean restrictAnnotationAccess) {
        this.restrictAnnotationAccess = restrictAnnotationAccess;
    }

    /**
     * @param timePeriodEnabled the timePeriodEnabled to set
     */
    public void setTimePeriodEnabled(boolean timePeriodEnabled) {
        logger.trace("setTimePeriodEnabled: {}", timePeriodEnabled);
        this.timePeriodEnabled = timePeriodEnabled;
    }

    /**
     * @return the logMessages
     */
    public List<CampaignLogMessage> getLogMessages() {
        return logMessages;
    }

    public CampaignLogMessage addLogMessage(LogMessage message, String pi) {
        if (message.getId() == null) {
            CampaignLogMessage campaignMessage = new CampaignLogMessage(message, this, pi);
            logMessages.add(campaignMessage);
            return campaignMessage;
        }
        //Log messages may not be changed, only new ones added. So only accept messages without id
        throw new IllegalArgumentException("Log messages with non null id may not be added to log");
    }

    /**
     * @param messageId
     */
    public void deleteLogMessage(Long messageId) {
        if (messageId != null) {
            Optional<CampaignLogMessage> message = this.logMessages.stream().filter(m -> messageId.equals(m.getId())).findAny();
            message.ifPresent(this.logMessages::remove);
        }

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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getTitle();
    }

    /**
     * @return
     */
    public String getAccessConditionValue() {
        return getTitle(IPolyglott.getDefaultLocale().getLanguage());
    }

}
