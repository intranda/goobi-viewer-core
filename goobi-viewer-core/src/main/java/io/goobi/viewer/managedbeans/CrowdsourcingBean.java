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
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import jakarta.persistence.PersistenceException;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.SolrQueryValidator;
import io.goobi.viewer.managedbeans.tabledata.TableDataFilter;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.crowdsourcing.CrowdsourcingTools;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.ReviewMode;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.StatisticMode;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItemOrder;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.solr.SolrConstants;

/**
 * <p>
 * CrowdsourcingBean class.
 * </p>
 */
@Named
@SessionScoped
public class CrowdsourcingBean implements Serializable {

    private static final long serialVersionUID = -6452528640177147828L;

    private static final Logger logger = LogManager.getLogger(CrowdsourcingBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    protected UserBean userBean;

    private TableDataProvider<Campaign> lazyModelCampaigns;

    private CrowdsourcingStatus targetStatus = null;

    /**
     * The campaign selected in backend
     */
    private Campaign selectedCampaign;
    /**
     * The campaign being annotated/reviewed
     */
    private Campaign targetCampaign = null;
    /** The identifier (PI) of the record currently targeted by this campaign */
    private String targetIdentifier;
    /** Current page of the current record. */
    private int targetPage;

    private final Configuration viewerConfig;
    private final IDAO dao;

    public CrowdsourcingBean() {
        this.viewerConfig = DataManager.getInstance().getConfiguration();
        try {
            this.dao = DataManager.getInstance().getDao();
        } catch (DAOException e) {
            throw new IllegalStateException("Cannot get instance of DAO");
        }
    }

    public CrowdsourcingBean(Configuration viewerConfig, IDAO dao) {
        this.viewerConfig = viewerConfig;
        this.dao = dao;
    }

    /**
     * Initialize all campaigns as lazily loaded list
     */
    @PostConstruct
    public void init() {
        if (lazyModelCampaigns == null) {
            lazyModelCampaigns = new TableDataProvider<>(new TableDataSource<Campaign>() {

                private Optional<Long> numCreatedPages = Optional.empty();

                @Override
                public List<Campaign> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters) {
                    try {
                        if (StringUtils.isBlank(sortField)) {
                            sortField = "id";
                            sortOrder = SortOrder.DESCENDING;
                        }
                        // Permanent filtering for non-admin group owners
                        if (userBean.getUser() != null && !userBean.getUser().isSuperuser()) {
                            filters.put("groupOwner", String.valueOf(userBean.getUser().getId()));
                        }
                        return dao.getCampaigns(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (!numCreatedPages.isPresent()) {
                        try {
                            // Permanent filtering for non-admin group owners
                            if (userBean.getUser() != null && !userBean.getUser().isSuperuser()) {
                                filters.put("groupOwner", String.valueOf(userBean.getUser().getId()));
                            }
                            numCreatedPages = Optional.ofNullable(dao.getCampaignCount(filters));
                        } catch (DAOException e) {
                            logger.error("Unable to retrieve total number of campaigns", e);
                        }
                    }
                    return numCreatedPages.orElse(0l);
                }

                @Override
                public void resetTotalNumberOfRecords() {
                    numCreatedPages = Optional.empty();
                }
            });
            lazyModelCampaigns.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
            lazyModelCampaigns.getFilter("name");
        }

    }

    /**
     * <p>
     * getCampaignCount.
     * </p>
     *
     * @param visibility a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility} object.
     * @return The total number of campaigns of a certain {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility}
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public long getCampaignCount(CampaignVisibility visibility) throws DAOException {
        Map<String, String> filters = new HashMap<>();
        if (visibility != null) {
            filters.put("visibility", visibility.name());
        }
        if (userBean.getUser() != null && !userBean.getUser().isSuperuser()) {
            filters.put("groupOwner", String.valueOf(userBean.getUser().getId()));
        }
        return dao.getCampaignCount(filters);
    }

    /**
     * Filter the loaded campaigns by {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility}
     *
     * @param visibility a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility} object.
     * @return a {@link java.lang.String} object.
     */
    public String filterCampaignsAction(CampaignVisibility visibility) {
        lazyModelCampaigns.resetFilters();
        if (visibility != null) {
            TableDataFilter filter = new TableDataFilter(lazyModelCampaigns, "visibility");
            lazyModelCampaigns.addFilter(filter);
        }

        return "";
    }

    /**
     * <p>
     * getAllLocales.
     * </p>
     *
     * @return A list of all locales supported by this viewer application
     */
    public static Collection<Locale> getAllLocales() {
        return IPolyglott.getLocalesStatic();
    }

    /**
     * Sets a new {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} as the {@link #selectedCampaign} and returns a pretty url to the view
     * for creating a new campaign
     *
     * @return a {@link java.lang.String} object.
     */
    public String createNewCampaignAction() {
        selectedCampaign = new Campaign(ViewerResourceBundle.getDefaultLocale());
        return "";
    }

    /**
     * Sets the given {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} as the {@link #selectedCampaign} and returns a pretty url to the
     * view for editing this campaign
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a {@link java.lang.String} object.
     */
    public String editCampaignAction(Campaign campaign) {
        selectedCampaign = campaign;
        return "pretty:adminCrowdEditCampaign";
    }

    /**
     * Delete the given {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} from the database and the loaded list of campaigns
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteCampaignAction(Campaign campaign) throws DAOException {
        if (campaign != null) {
            if (dao.deleteCampaign(campaign)) {
                Messages.info("admin__crowdsoucing_campaign_deleteSuccess");
                lazyModelCampaigns.update();
            }
        }

        return "";
    }

    /**
     * Add a new {@link io.goobi.viewer.model.crowdsourcing.questions.Question} to the selected campaign
     *
     * @return a {@link java.lang.String} object.
     */
    public String addNewQuestionAction() {
        if (selectedCampaign != null) {
            selectedCampaign.getQuestions().add(new Question(selectedCampaign));
        }

        return "";
    }

    /**
     * Remove the given {@link io.goobi.viewer.model.crowdsourcing.questions.Question} from the selected campaign
     *
     * @param question a {@link io.goobi.viewer.model.crowdsourcing.questions.Question} object.
     * @return a {@link java.lang.String} object.
     */
    public String removeQuestionAction(Question question) {
        if (selectedCampaign != null && question != null) {
            selectedCampaign.getQuestions().remove(question);
        }

        return "";
    }

    /**
     * Resets dateStart + dateEnd to null.
     *
     * @return a {@link java.lang.String} object.
     */
    public String resetDurationAction() {
        if (selectedCampaign != null) {
            selectedCampaign.setDateStart(null);
            selectedCampaign.setDateEnd(null);
        }

        return "";
    }

    /**
     * <p>
     * getAllCampaigns.
     * </p>
     *
     * @return All campaigns from the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Campaign> getAllCampaigns() throws DAOException {
        List<Campaign> pages = dao.getAllCampaigns();
        return pages;
    }

    /**
     * <p>
     * getAllCampaigns.
     * </p>
     *
     * @param visibility a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility} object.
     * @return All camapaigns of the given {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.CampaignVisibility} from the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Campaign> getAllCampaigns(CampaignVisibility visibility) throws DAOException {
        List<Campaign> pages = DataManager.getInstance()
                .getDao()
                .getAllCampaigns()
                .stream()
                .filter(camp -> visibility.equals(camp.getVisibility()))
                .collect(Collectors.toList());
        return pages;
    }

    /**
     * Returns the list of campaigns that are visible to the given user.
     *
     * @param user
     * @return
     * @throws DAOException
     */
    public List<Campaign> getAllowedCampaigns(User user) throws DAOException {
        return getAllowedCampaigns(user, getAllCampaigns());
    }

    /**
     * Returns the list of campaigns that are visible to the given user based on the given list of campaigns.
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param allCampaigns List of all available campaigns
     * @return list of campaigns visible to the given user; only public campaigns if user is null
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return all public campaigns if user not logged in
     * @should return private campaigns within time period if user not logged in
     * @should return private campaigns within time period if user logged in
     */
    List<Campaign> getAllowedCampaigns(User user, List<Campaign> allCampaigns) throws DAOException {
        logger.trace("getAllowedCampaigns");
        if (allCampaigns == null || allCampaigns.isEmpty()) {
            logger.trace("No campaigns found");
            return Collections.emptyList();
        }

        List<Campaign> ret = new ArrayList<>(allCampaigns.size());
        for (Campaign campaign : allCampaigns) {
            if (isAllowed(user, campaign)) {
                ret.add(campaign);
            }
        }

        return ret;
    }

    /**
     *
     * @param user
     * @return
     * @throws DAOException
     */
    @Deprecated
    public boolean isUserOwnsAnyCampaigns(User user) throws DAOException {
        return CrowdsourcingTools.isUserOwnsAnyCampaigns(user);
    }

    /**
     * Check if the given user is allowed access to the given campaign from a rights management standpoint alone. If the user is null, access is
     * granted for public campaigns only, otherwise access is granted if the user has the appropriate rights
     *
     * @param user a {@link io.goobi.viewer.model.security.user.User} object.
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return true if campaign is allowed to the given user; false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @should return true for public campaigns
     * @should return false if private campaign within time period but boolean false
     * @should return true if private campaign within time period and user null
     * @should return true if private campaign within time period and user not null
     * @should return false if private campaign outside time period
     * @should return false if user group set and user null
     * @should return false if user group set and user not member
     * @should return true if user group set and user owner
     * @should return false if user group set but boolean false
     */
    public static boolean isAllowed(User user, Campaign campaign) throws DAOException {
        if (campaign == null) {
            return false;
        }
        // Skip inactive campaigns
        if (!campaign.isHasStarted() || campaign.isHasEnded()) {
            return false;
        }
        if (CampaignVisibility.PUBLIC.equals(campaign.getVisibility())) {
            return true;
        }

        // Allow campaigns with a set time frame, but no user group
        if (campaign.isTimePeriodEnabled() && campaign.getDateStart() != null && campaign.getDateEnd() != null
                && !campaign.isLimitToGroup()) {
            return true;
        }

        switch (campaign.getVisibility()) {
            case PRIVATE:
                if (user == null) {
                    return false;
                }
                if (user.isSuperuser()) {
                    return true;
                }
                // Only logged in members may access campaigns limited to a user group
                if (!campaign.isLimitToGroup() || campaign.getUserGroup() == null) {
                    return false;
                }
                try {
                    return campaign.getUserGroup().getMembersAndOwner().contains(user);
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            default:
                break;
        }

        return false;
    }

    /**
     * Adds the current page to the database, if it doesn't exist or updates it otherwise
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public String saveSelectedCampaignAction() throws DAOException, PresentationException, IndexUnreachableException {
        logger.trace("saveSelectedCampaign");
        if (selectedCampaign == null) {
            Messages.error("admin__crowdsourcing_campaign_save_failure");
            return "";
        }
        if (userBean == null || !selectedCampaign.isUserMayEdit(userBean.getUser())) {
            // Only authorized admins may save
            Messages.error("admin__crowdsourcing_campaign_save_failure");
            return "";
        }

        // Save
        boolean success = false;
        LocalDateTime now = LocalDateTime.now();
        if (selectedCampaign.getDateCreated() == null) {
            selectedCampaign.setDateCreated(now);
        }
        selectedCampaign.setDateUpdated(now);
        if (selectedCampaign.getId() != null) {
            try {
                success = dao.updateCampaign(selectedCampaign);
            } catch (PersistenceException e) {
                logger.error("Updating campaign " + selectedCampaign + " in database failed ", e);
                success = false;
            }
        } else {
            success = dao.addCampaign(selectedCampaign);
        }
        if (success) {
            Messages.info("admin__crowdsourcing_campaign_save_success");
            setSelectedCampaign(selectedCampaign);
            lazyModelCampaigns.update();
            // Update the map of active campaigns for record identifiers (in case a new Solr query changes the set)
            updateActiveCampaigns();
            return "pretty:adminCrowdCampaigns";
        }

        Messages.error("admin__crowdsourcing_campaign_save_failure");
        return "";
    }

    /**
     * <p>
     * getCampaignsRootUrl.
     * </p>
     *
     * @return root URL for the permalink value
     */
    public String getCampaignsRootUrl() {
        return navigationHelper.getApplicationUrl() + "campaigns/";
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelCampaigns</code>.
     * </p>
     *
     * @return the lazyModelCampaigns
     */
    public TableDataProvider<Campaign> getLazyModelCampaigns() {
        return lazyModelCampaigns;
    }

    /**
     * <p>
     * Getter for the field <code>selectedCampaign</code>.
     * </p>
     *
     * @return the selectedCampaign
     */
    public Campaign getSelectedCampaign() {
        return selectedCampaign;
    }

    /**
     * Set the selected campaign to a clone of the given campaign
     *
     * @param selectedCampaign the selectedCampaign to set
     */
    public void setSelectedCampaign(Campaign campaign) {

        if (campaign == null) {
            this.selectedCampaign = null;
        } else if (selectedCampaign == null || ObjectUtils.notEqual(selectedCampaign.getId(), campaign.getId())) {
            this.selectedCampaign = new Campaign(campaign);
        }
    }

    /**
     * @param campaign
     * @return
     */
    @Deprecated
    private boolean isSelected(Campaign campaign) {
        return campaign != null && this.selectedCampaign != null && ObjectUtils.equals(campaign.getId(), this.selectedCampaign.getId());
    }

    /**
     * <p>
     * getSelectedCampaignId.
     * </p>
     *
     * @return The id of the {@link io.goobi.viewer.managedbeans.CrowdsourcingBean#selectedCampaign} as String
     */
    public String getSelectedCampaignId() {
        Long id = Optional.ofNullable(getSelectedCampaign()).map(Campaign::getId).orElse(null);
        return id == null ? null : id.toString();
    }

    /**
     * Set the {@link io.goobi.viewer.managedbeans.CrowdsourcingBean#selectedCampaign} by a String containing the campaign id
     *
     * @param id a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void setSelectedCampaignId(String id) throws DAOException {
        if (id != null) {
            Campaign campaign = dao.getCampaign(Long.parseLong(id));
            setSelectedCampaign(campaign);
        } else {
            setSelectedCampaign(null);
        }
    }

    /**
     * <p>
     * isEditMode.
     * </p>
     *
     * @return the editMode
     */
    public boolean isEditMode() {
        return selectedCampaign != null && selectedCampaign.getId() != null;
    }

    /**
     * <p>
     * Getter for the field <code>targetCampaign</code>.
     * </p>
     *
     * @return the {@link #targetCampaign}
     */
    public Campaign getTargetCampaign() {
        return targetCampaign;
    }

    /**
     * <p>
     * Setter for the field <code>targetCampaign</code>.
     * </p>
     *
     * @param targetCampaign the targetCampaign to set
     */
    public void setTargetCampaign(Campaign targetCampaign) {
        if (this.targetCampaign != null && !this.targetCampaign.equals(targetCampaign)) {
            resetTarget();
        }

        this.targetCampaign = targetCampaign;
    }

    /**
     * <p>
     * getTargetCampaignId.
     * </p>
     *
     * @return the identifier of the {@link #targetCampaign}
     */
    public String getTargetCampaignId() {
        Long id = Optional.ofNullable(getTargetCampaign()).map(Campaign::getId).orElse(null);
        return id == null ? null : id.toString();
    }

    /**
     * <p>
     * setTargetCampaignId.
     * </p>
     *
     * @param id a {@link java.lang.String} object.
     * @throws java.lang.NumberFormatException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void setTargetCampaignId(String id) throws NumberFormatException, DAOException {
        if (id != null) {
            Campaign campaign = dao.getCampaign(Long.parseLong(id));
            setTargetCampaign(campaign);
        } else {
            setTargetCampaign(null);
        }
    }

    public void setNextIdentifierForAnnotation() throws PresentationException, IndexUnreachableException {
        if (getTargetCampaign() != null) {
            CampaignItemOrder order = CampaignItemOrder.of(viewerConfig.getCrowdsourcingCampaignItemOrder()).orElse(CampaignItemOrder.FIXED);
            String pi = getNextTargetIdentifier(getTargetCampaign(), getTargetIdentifier(), CrowdsourcingStatus.ANNOTATE, order);
            setTargetIdentifier(pi);
        }
    }

    public void setNextIdentifierForReview() throws PresentationException, IndexUnreachableException {
        if (getTargetCampaign() != null) {
            CampaignItemOrder order = CampaignItemOrder.of(viewerConfig.getCrowdsourcingCampaignItemOrder()).orElse(CampaignItemOrder.FIXED);
            String pi = getNextTargetIdentifier(getTargetCampaign(), getTargetIdentifier(), CrowdsourcingStatus.REVIEW, order);
            setTargetIdentifier(pi);
        }
    }

    private String getNextTargetIdentifier(Campaign campaign, String currentIdentifier, CrowdsourcingStatus status, CampaignItemOrder ordering)
            throws PresentationException, IndexUnreachableException {
        switch (ordering) {
            case RANDOM:
                return campaign.getRandomizedTarget(status, currentIdentifier, userBean.getUser());
            case FIXED:
            default:
                return campaign.getNextTarget(status, currentIdentifier, userBean.getUser());
        }
    }

    /**
     * removes the target identifier (pi) from the bean, so that pi can be targeted again by random target resolution
     */
    public void resetTarget() {
        setTargetIdentifier(null);
    }

    /**
     * <p>
     * forwardToAnnotationTarget.
     * </p>
     *
     * @return the pretty url to annotatate the {@link #targetIdentifier} by the {@link #targetCampaign}
     */
    public String forwardToAnnotationTarget() {
        return "pretty:crowdCampaignAnnotate2";
    }

    /**
     * <p>
     * forwardToReviewTarget.
     * </p>
     *
     * @return the pretty url to review the {@link #targetIdentifier} for the {@link #targetCampaign}
     */
    public String forwardToReviewTarget() {
        return "pretty:crowdCampaignReview2";
    }

    /**
     * <p>
     * Getter for the field <code>targetIdentifier</code>.
     * </p>
     *
     * @return the PI of a work selected for editing
     */
    public String getTargetIdentifier() {
        return this.targetIdentifier;
    }

    /**
     * <p>
     * getTargetIdentifierForUrl.
     * </p>
     *
     * @return the PI of a work selected for editing or "-" if no targetIdentifier exists
     */
    public String getTargetIdentifierForUrl() {
        return StringUtils.isBlank(this.targetIdentifier) ? "-" : this.targetIdentifier;
    }

    /**
     * <p>
     * setTargetIdentifierForUrl.
     * </p>
     *
     * @param pi a {@link java.lang.String} object.
     */
    public void setTargetIdentifierForUrl(String pi) {
        this.targetIdentifier = "-".equals(pi) ? null : pi;
    }

    /**
     * <p>
     * Setter for the field <code>targetIdentifier</code>.
     * </p>
     *
     * @param targetIdentifier the targetIdentifier to set
     */
    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    /**
     * @return the targetPage
     */
    public int getTargetPage() {
        return targetPage;
    }

    /**
     * @param targetPage the targetPage to set
     */
    public void setTargetPage(int targetPage) {
        this.targetPage = targetPage;
    }

    /**
     * <p>
     * forwardToCrowdsourcingAnnotation.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a pretty url to annotate a random work with the given {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}
     */
    public String forwardToCrowdsourcingAnnotation(Campaign campaign) {
        setTargetCampaign(campaign);
        return "pretty:crowdCampaignAnnotate1";
    }

    /**
     * <p>
     * forwardToCrowdsourcingReview.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @return a pretty url to review a random work with the given {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}
     */
    public String forwardToCrowdsourcingReview(Campaign campaign) {
        setTargetCampaign(campaign);
        return "pretty:crowdCampaignReview1";
    }

    /**
     * <p>
     * forwardToCrowdsourcingAnnotation.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @param pi a {@link java.lang.String} object.
     * @return a pretty url to annotate the work with the given pi with the given {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}
     */
    public String forwardToCrowdsourcingAnnotation(Campaign campaign, String pi) {
        setTargetCampaign(campaign);
        setTargetIdentifier(pi);
        return "pretty:crowdCampaignAnnotate2";
    }

    /**
     * <p>
     * forwardToCrowdsourcingReview.
     * </p>
     *
     * @param campaign a {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign} object.
     * @param pi a {@link java.lang.String} object.
     * @return a pretty url to review the work with the given pi with the given {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}
     */
    public String forwardToCrowdsourcingReview(Campaign campaign, String pi) {
        setTargetCampaign(campaign);
        setTargetIdentifier(pi);
        return "pretty:crowdCampaignReview2";
    }

    /**
     * <p>
     * getRandomItemUrl.
     * </p>
     *
     * @param campaign The campaign with which to annotate/review
     * @param status if {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus#REVIEW}, return a url for
     *            reviewing, otherwise for annotating
     * @return The pretty url to either review or annotate a random work with the given {@link io.goobi.viewer.model.crowdsourcing.campaigns.Campaign}
     */
    public String getNextItemUrl(Campaign campaign, CrowdsourcingStatus status) {
        String mappingId = CrowdsourcingStatus.REVIEW.equals(status) ? "crowdCampaignReview1" : "crowdCampaignAnnotate1";
        URL mappedUrl = PrettyContext.getCurrentInstance().getConfig().getMappingById(mappingId).getPatternParser().getMappedURL(campaign.getId());
        logger.debug("Mapped URL {}", mappedUrl);
        return BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + mappedUrl.toString();
    }

    /**
     * <p>
     * getTargetRecordStatus.
     * </p>
     *
     * @return the {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CrowdsourcingStatus} of the {@link #targetCampaign}
     *         for the {@link #targetIdentifier}
     */
    public CrowdsourcingStatus getTargetRecordStatus() {
        if (getTargetCampaign() != null && StringUtils.isNotBlank(getTargetIdentifier())) {
            return getTargetCampaign().getRecordStatus(getTargetIdentifier());
        }
        return null;
    }

    /**
     * <p>
     * handleInvalidTarget.
     * </p>
     *
     * @return the pretty URL to the crowdsourcing campaigns page if {@link io.goobi.viewer.managedbeans.UserBean#getUser()} is not eligible for
     *         viewing the {@link #targetCampaign}
     */
    public String handleInvalidTarget() {
        if (StringUtils.isBlank(getTargetIdentifier()) || "-".equals(getTargetIdentifier())) {
            return "pretty:crowdCampaigns";
        } else if (getTargetCampaign() == null) {
            return "pretty:crowdCampaigns";
        } else if (StatisticMode.RECORD == getTargetCampaign().getStatisticMode() && CrowdsourcingStatus.FINISHED.equals(getTargetRecordStatus())) {
            return "pretty:crowdCampaigns";
        } else if (getTargetCampaign().isHasEnded() || !getTargetCampaign().isHasStarted()) {
            return "pretty:crowdCampaigns";
        } else
            try {
                if (userBean == null || !isAllowed(userBean.getUser(), getTargetCampaign())
                        || !getTargetCampaign().isEligibleToEdit(getTargetIdentifier(), getTargetRecordStatus(), userBean.getUser())) {
                    return "pretty:crowdCampaigns";
                }
                return "";
            } catch (DAOException e) {
                logger.error(e.toString(), e);
                return "";
            }
    }

    /**
     * Returns a list of active campaigns for the given identifier that are visible to the current user.
     *
     * @return List of campaigns
     * @param pi a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public List<Campaign> getActiveCampaignsForRecord(String pi) throws DAOException, PresentationException, IndexUnreachableException {
        logger.trace("getActiveCampaignsForRecord: {}", pi);
        if (pi == null) {
            return Collections.emptyList();
        }

        // If the map has not yet been initialized during the application's life cycle, make it so
        if (DataManager.getInstance().getRecordCampaignMap() == null) {
            updateActiveCampaigns();
        }

        List<Campaign> allActiveCampaigns = DataManager.getInstance().getRecordCampaignMap().get(pi);
        if (allActiveCampaigns == null || allActiveCampaigns.isEmpty()) {
            logger.trace("No campaigns found for {}", pi);
            return Collections.emptyList();
        }
        logger.trace("Found {} total campaigns for {}", allActiveCampaigns.size(), pi);

        List<Campaign> ret = new ArrayList<>(allActiveCampaigns.size());
        for (Campaign campaign : allActiveCampaigns) {
            if (isAllowed(userBean.getUser(), campaign)) {
                ret.add(campaign);
            }
        }

        logger.trace("Returning {} public campaigns for {}", ret.size(), pi);
        return ret;
    }

    /**
     * Searches for all identifiers that are encompassed by the Solr query of each active campaign and initializes and fills a map of active campaigns
     * for each identifier. Should be called once after the application first starts (or upon first access) or when a campaign has been updated.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public void updateActiveCampaigns() throws DAOException, PresentationException, IndexUnreachableException {
        logger.trace("updateActiveCampaigns");
        DataManager.getInstance().setRecordCampaignMap(new HashMap<>());

        List<Campaign> allCampaigns = getAllCampaigns();
        if (allCampaigns.isEmpty()) {
            return;
        }

        for (Campaign campaign : allCampaigns) {
            //            if (!CampaignVisibility.PUBLIC.equals(campaign.getVisibility()) && !CampaignVisibility.RESTRICTED.equals(campaign.getVisibility())) {
            //                continue;
            //            }

            // Skip if query invalid
            try {
                SolrQueryValidator.getHitCount(campaign.getSolrQuery());
            } catch (IOException | SolrServerException | RemoteSolrException e) {
                logger.error(e.getMessage());
                continue;
            }

            QueryResponse qr = DataManager.getInstance()
                    .getSearchIndex()
                    .searchFacetsAndStatistics(campaign.getSolrQuery(), null, Collections.singletonList(SolrConstants.PI_TOPSTRUCT), 1, false);
            if (qr.getFacetField(SolrConstants.PI_TOPSTRUCT) != null) {
                for (Count count : qr.getFacetField(SolrConstants.PI_TOPSTRUCT).getValues()) {
                    String pi = count.getName();
                    List<Campaign> list = DataManager.getInstance().getRecordCampaignMap().get(pi);
                    if (list == null) {
                        list = new ArrayList<>();
                        DataManager.getInstance().getRecordCampaignMap().put(pi, list);
                    }
                    if (!list.contains(campaign)) {
                        list.add(campaign);
                    }
                }
            }
        }
        logger.trace("Added {} identifiers to the map.", DataManager.getInstance().getRecordCampaignMap().size());
    }

    public Set<ReviewMode> getPossibleReviewModes() {
        return EnumSet.allOf(ReviewMode.class);
    }

    /**
     * @return List of enum values
     */
    public Set<StatisticMode> getAvailableStatisticModes() {
        return EnumSet.allOf(StatisticMode.class);
    }

    /**
     * @return the targetStatus
     */
    public CrowdsourcingStatus getTargetStatus() {
        return targetStatus;
    }

    /**
     * @param targetStatus the targetStatus to set
     */
    public void setTargetStatus(CrowdsourcingStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = CrowdsourcingStatus.forName(targetStatus);
    }
}
