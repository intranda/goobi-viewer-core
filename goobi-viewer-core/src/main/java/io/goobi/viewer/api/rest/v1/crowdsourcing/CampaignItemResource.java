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
package io.goobi.viewer.api.rest.v1.crowdsourcing;

import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS;
import static io.goobi.viewer.api.rest.v1.ApiUrls.ANNOTATIONS_ANNOTATION;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PAGES_CANVAS;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.intranda.api.annotation.wa.WebAnnotation;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.Version;
import io.goobi.viewer.api.rest.bindings.CrowdsourcingCampaignBinding;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.filters.CrowdsourcingCampaignFilter;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.StatisticMode;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordPageStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.iiif.presentation.v2.builder.ManifestBuilder;
import io.goobi.viewer.model.log.LogMessage;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Rest resources to create a frontend-view for a campaign to annotate or review a work, and to process the created annotations and/or changes to the
 * campaign status.
 *
 * The following api points are defined:
 * <ul>
 * <li>/crowdsourcing/campaigns/{campaignId}/{pi}/ <br/>
 * GET a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem} for the given campaignId and pi, or PUT the status for that
 * combination</li>
 * <li>/crowdsourcing/campaigns/{campaignId}/{pi}/annotations/ <br/>
 * GET a list of annotations for the given campaignId and pi, sorted by target, or PUT the annotations for this combination</li>
 *
 * @author florian
 */
@Path("/crowdsourcing/campaigns/{campaignId}")
@ViewerRestServiceBinding
@CrowdsourcingCampaignBinding
public class CampaignItemResource {

    private static final Logger logger = LogManager.getLogger(CampaignItemResource.class);

    protected AbstractApiUrlManager urls = DataManager.getInstance().getRestApiManager().getDataApiManager(Version.v1).orElse(null);

    private final Long campaignId;

    @Context
    private HttpServletRequest servletRequest;

    /**
     * <p>
     * Constructor for CampaignItemResource.
     * </p>
     * 
     * @param servletRequest
     * @param campaignId
     */
    public CampaignItemResource(@Context HttpServletRequest servletRequest, @PathParam("campaignId") Long campaignId) {
        this.campaignId = campaignId;
        servletRequest.setAttribute(CrowdsourcingCampaignFilter.CAMPAIGN_ID_REQUEST_ATTRIBUTE, campaignId);
    }

    /**
     * 
     * @param servletRequest
     * @param urls
     * @param campaignId
     */
    public CampaignItemResource(@Context HttpServletRequest servletRequest, AbstractApiUrlManager urls, @PathParam("campaignId") Long campaignId) {
        this.urls = urls;
        this.campaignId = campaignId;
        servletRequest.setAttribute(CrowdsourcingCampaignFilter.CAMPAIGN_ID_REQUEST_ATTRIBUTE, campaignId);
    }

    /**
     * Get the {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem} for a campaign and work, containing the URL of the targeted resource
     * (IIIF manifest) and all information to create a GUI for the campaign's questions.
     *
     * @param persistentIdentifier a {@link java.lang.String} object.
     * @param servletRequest
     * @return a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem}
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException if any.
     */
    @GET
    @Path("/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public CampaignItem getItemForManifest(@PathParam("pi") final String persistentIdentifier, @Context HttpServletRequest servletRequest)
            throws URISyntaxException, DAOException, ContentNotFoundException {
        if (persistentIdentifier == null) {
            return null;
        }

        String pi = StringTools.stripPatternBreakingChars(persistentIdentifier);
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        if (campaign == null) {
            throw new ContentNotFoundException("No campaign found with id " + campaignId);
        }

        CampaignItem item = new CampaignItem();
        URI manifestURI = new ManifestBuilder(urls).getManifestURI(pi);
        item.setSource(manifestURI);
        item.setRecordIdentifier(pi);
        item.setCampaign(campaign);
        item.setPageStatisticMode(StatisticMode.PAGE.equals(campaign.getStatisticMode()));
        if (item.isPageStatisticMode() && campaign.getStatistics().get(pi) != null) {
            for (String key : campaign.getStatistics().get(pi).getPageStatistics().keySet()) {
                CampaignRecordPageStatistic pageStatistic = campaign.getStatistics().get(pi).getPageStatistics().get(key);
                if (pageStatistic.getPage() != null) {
                    item.getPageStatusMap().put(pageStatistic.getPage(), pageStatistic.getStatus().name());
                }
            }
            logger.debug("pageStatusMap set");
        }
        String herePi = pi;
        if (campaign.isShowLog()) {
            item.setLog(campaign.getLogMessages()
                    .stream()
                    .filter(m -> m.getPi().equals(herePi))
                    .map(clm -> new LogMessage(clm, servletRequest))
                    .collect(Collectors.toList()));
        }
        try {
            List<String> allMetadataFields =
                    campaign.getQuestions().stream().flatMap(q -> q.getMetadataFields().stream()).distinct().collect(Collectors.toList());
            String query = SolrConstants.PI + ":" + pi;
            SolrDocument doc = DataManager.getInstance().getSearchIndex().getFirstDoc(query, allMetadataFields);
            if (doc == null) {
                throw new ContentNotFoundException("Record not found: " + pi);
            }
            Map<String, List<String>> fieldValueMap =
                    doc.getFieldNames().stream().collect(Collectors.toMap(field -> field, field -> getFieldValues(doc, field)));
            item.setMetadata(fieldValueMap);
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Error getting metadata valued for campaign item ", e);
        }

        return item;
    }

    private static List<String> getFieldValues(SolrDocument doc, String field) {
        return doc.getFieldValues(field).stream().map(SolrTools::getAsString).collect(Collectors.toList());
    }

    /**
     * Sets the {@link io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus} for the given campaign and work and
     * records the {@link io.goobi.viewer.model.security.user.User} who made the change.
     *
     * @param item a {@link io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem} object.
     * @param persistentIdentifier a {@link java.lang.String} object.
     * @param page
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @PUT
    @Path("/{pi}/{page}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public void setItemForManifest(CampaignItem item, @PathParam("pi") final String persistentIdentifier, @PathParam("page") int page)
            throws DAOException {
        if (item == null) {
            throw new IllegalArgumentException("item may not be null");
        }

        String pi = persistentIdentifier;
        if (pi != null) {
            pi = StringTools.stripPatternBreakingChars(pi);
        }
        // logger.debug("setItemForManifest: {}/{}", pi, page);
        CrowdsourcingStatus status = item.getRecordStatus();
        if (status == null) {
            logger.error("Status not found: {}", item.getRecordStatus());
            return;
        }
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        if (campaign == null) {
            logger.error("Campaign not found: {}", campaignId);
            return;
        }

        User user = null;
        Long userId = User.getId(item.getCreatorURI());
        if (userId != null) {
            user = DataManager.getInstance().getDao().getUser(userId);
        }

        switch (campaign.getStatisticMode()) {
            case RECORD:
                campaign.setRecordStatus(pi, status, Optional.ofNullable(user));
                updateAnnotationStatusForRecord(campaign, pi, status, Optional.ofNullable(user));
                break;
            case PAGE:
                CrowdsourcingStatus pageStatus = CrowdsourcingStatus.forName(status.getName());
                campaign.setRecordPageStatus(pi, page, pageStatus, Optional.ofNullable(user));
                updateAnnotationStatusForPage(campaign, pi, page, pageStatus, Optional.ofNullable(user));
                break;
            default:
                logger.warn("Wrong campaign statistic mode: {}", campaign.getStatisticMode().name());
                break;
        }
        DataManager.getInstance().getDao().updateCampaign(campaign);
    }

    /**
     * 
     * @param campaign
     * @param pi
     * @param page
     * @param crowdsourcingStatus
     * @param user
     * @throws DAOException
     */
    private static void updateAnnotationStatusForPage(Campaign campaign, String pi, int page, CrowdsourcingStatus crowdsourcingStatus,
            Optional<User> user) throws DAOException {
        List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, pi, page);
        for (CrowdsourcingAnnotation anno : annotations) {
            anno.setPublicationStatus(getPublicationStatus(crowdsourcingStatus));
            if (CrowdsourcingStatus.FINISHED.equals(crowdsourcingStatus)) {
                anno.setAccessCondition(getPublishedAccessCondition(campaign));
                if (campaign.isReviewModeActive() && user.isPresent()) {
                    anno.setReviewer(user.get());
                }
            }
            DataManager.getInstance().getDao().updateAnnotation(anno);
        }
    }

    private static String getPublishedAccessCondition(Campaign campaign) {
        return campaign.isRestrictAnnotationAccess() ? campaign.getAccessConditionValue() : SolrConstants.OPEN_ACCESS_VALUE;
    }

    /**
     * 
     * @param campaign
     * @param pi
     * @param crowdsourcingStatus
     * @param user
     * @throws DAOException
     */
    private static void updateAnnotationStatusForRecord(Campaign campaign, String pi, CrowdsourcingStatus crowdsourcingStatus,
            Optional<User> user) throws DAOException {
        List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, pi);
        for (CrowdsourcingAnnotation anno : annotations) {
            anno.setPublicationStatus(getPublicationStatus(crowdsourcingStatus));
            if (CrowdsourcingStatus.FINISHED.equals(crowdsourcingStatus)) {
                anno.setAccessCondition(getPublishedAccessCondition(campaign));
                if (campaign.isReviewModeActive() && user.isPresent()) {
                    anno.setReviewer(user.get());
                }
            }
            DataManager.getInstance().getDao().updateAnnotation(anno);
        }
    }

    /**
     * 
     * @param crowdsourcingStatus
     * @return {@link PublicationStatus}
     */
    private static PublicationStatus getPublicationStatus(CrowdsourcingStatus crowdsourcingStatus) {
        switch (crowdsourcingStatus) {
            case ANNOTATE:
                return PublicationStatus.CREATING;
            case REVIEW:
                return PublicationStatus.REVIEW;
            case FINISHED:
                return PublicationStatus.PUBLISHED;
            default:
                return PublicationStatus.CREATING;
        }

    }

    /**
     * Get all annotations for the given campaign and work, sorted by target.
     *
     * @param pi a {@link java.lang.String} object.
     * @param request
     * @return A map of target URIs (manifest or canvas) mapped to a submap of question URIs mapped to questions
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @Path("/{pi}/annotations")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public List<WebAnnotation> getAnnotationsForManifest(@PathParam("pi") final String pi, @Context HttpServletRequest request)
            throws URISyntaxException, DAOException {
        // logger.debug("getAnnotationsForManifest: {}", pi);
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        List<CrowdsourcingAnnotation> annotations =
                DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, StringTools.stripPatternBreakingChars(pi));

        List<WebAnnotation> webAnnotations = new ArrayList<>();
        for (CrowdsourcingAnnotation anno : annotations) {
            if (StringUtils.isNotBlank(anno.getBody())) {
                WebAnnotation webAnno = new AnnotationConverter(urls).getAsWebAnnotation(anno);
                webAnnotations.add(webAnno);
            }
        }

        return webAnnotations;
    }

    /**
     * Takes a map of annotation target (canvas/manifest) ids and replaces all annotations for the given campaign, pi and targeted pages if target is
     * canvas) with the ones contained in the map.
     *
     * @param pages a {@link java.util.List} object.
     * @param pi a {@link java.lang.String} object.
     * @throws java.net.URISyntaxException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @PUT
    @Path("/{pi}/annotations")
    @Consumes({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public void setAnnotationsForManifest(List<AnnotationPage> pages, @PathParam("pi") final String pi)
            throws URISyntaxException, DAOException {
        // logger.debug("setAnnotationsForManifest: {}", pi);
        IDAO dao = DataManager.getInstance().getDao();
        Campaign campaign = dao.getCampaign(campaignId);

        String herePi = pi;
        for (AnnotationPage page : pages) {
            URI targetURI = URI.create(page.getId());
            String pageOrderString = urls.parseParameter(
                    urls.path(RECORDS_PAGES, RECORDS_PAGES_CANVAS).build(),
                    targetURI.toString(),
                    "{pageNo}");
            Integer pageOrder = StringUtils.isBlank(pageOrderString) ? null : Integer.parseInt(pageOrderString);

            List<CrowdsourcingAnnotation> existingAnnotations =
                    dao.getAnnotationsForCampaignAndTarget(campaign, StringTools.stripPatternBreakingChars(pi), pageOrder);
            List<CrowdsourcingAnnotation> newAnnotations =
                    page.annotations.stream().map(anno -> createPersistentAnnotation(herePi, pageOrder, anno)).collect(Collectors.toList());

            //delete existing annotations not contained in response
            for (CrowdsourcingAnnotation anno : existingAnnotations) {
                if (newAnnotations.stream().noneMatch(annoNew -> anno.getId().equals(annoNew.getId()))) {
                    try {
                        dao.deleteAnnotation(anno);
                    } catch (DAOException e) {
                        logger.error("Error deleting annotation: {}", e.getMessage());
                    }
                }
            }

            //add new annotation and update existing ones
            for (CrowdsourcingAnnotation anno : newAnnotations) {
                anno.setAccessCondition(CrowdsourcingStatus.ANNOTATE.name());
                try {
                    if (anno.getId() == null) {
                        dao.addAnnotation(anno);
                    } else {
                        dao.updateAnnotation(anno);
                    }
                } catch (DAOException e) {
                    logger.error("Error persisting annotation: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * @param pi
     * @param pageOrder
     * @param anno
     * @return a {@link CrowdsourcingAnnotation}. Either with an existing database id, or without id if ann doesn't has an empty id property
     */
    public CrowdsourcingAnnotation createPersistentAnnotation(String pi, Integer pageOrder, WebAnnotation anno) {
        Long id = null;
        if (anno.getId() != null) {
            String uri = anno.getId().toString();
            String idString = urls.parseParameter(urls.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION).build(), uri, "{id}");
            if (StringUtils.isNotBlank(idString)) {
                id = Long.parseLong(idString);
            }
        }
        return new CrowdsourcingAnnotation(anno, id, pi, pageOrder);
    }

    /**
     * Used to create or read a list of WebAnnotations sorted by their target (a iiif manifest or canvas).
     *
     * @author florian
     *
     */
    public static class AnnotationPage {
        private String id;
        private List<WebAnnotation> annotations;

        /**
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @return the annotations
         */
        public List<WebAnnotation> getAnnotations() {
            return annotations;
        }

        /**
         * @param annotations the annotations to set
         */
        public void setAnnotations(List<WebAnnotation> annotations) {
            this.annotations = annotations;
        }
    }

}
