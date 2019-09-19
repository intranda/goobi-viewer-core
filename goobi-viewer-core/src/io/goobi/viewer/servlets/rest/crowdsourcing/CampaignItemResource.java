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
package io.goobi.viewer.servlets.rest.crowdsourcing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.wa.WebAnnotation;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.streams.Try;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.model.iiif.presentation.builder.BuildMode;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * @author florian
 *
 */
@Path("/crowdsourcing/campaigns")
@ViewerRestServiceBinding
public class CampaignItemResource {

    private static final Logger logger = LoggerFactory.getLogger(CampaignItemResource.class);

    
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    private final URI requestURI;

    public CampaignItemResource() {
        this.requestURI = URI.create(DataManager.getInstance().getConfiguration().getRestApiUrl());
    }

    @GET
    @Path("/{campaignId}/{pi}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public CampaignItem getItemForManifest(@PathParam("campaignId") Long campaignId, @PathParam("pi") String pi) throws URISyntaxException, DAOException, ContentNotFoundException {
        URI servletURI = URI.create(ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest));
        URI manifestURI = new ManifestBuilder(servletURI, requestURI).getManifestURI(pi, BuildMode.IIIF_SIMPLE);

        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        if(campaign != null) {
            CampaignItem item = new CampaignItem();
            item.setSource(manifestURI);
            item.setCampaign(campaign);
            return item;            
        } else {
            throw new ContentNotFoundException("No campaign found with id " + campaignId);
        }
    }
    
    @PUT
    @Path("/{campaignId}/{pi}/")
    @Consumes({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public void setItemForManifest(CampaignItem item, @PathParam("campaignId") Long campaignId, @PathParam("pi") String pi) throws URISyntaxException, DAOException {
        CampaignRecordStatus status = item.getRecordStatus();
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        
        User user = null;
        Long userId = User.getId(item.getCreatorURI());
        if(userId != null) {
            user = DataManager.getInstance().getDao().getUser(userId);
        }
        if(status != null && campaign != null) {
            campaign.setRecordStatus(pi, status, Optional.ofNullable(user));
            DataManager.getInstance().getDao().updateCampaign(campaign);
        }
    }
    
    /**
     * 
     * @param campaignId
     * @param pi
     * @return  A map of target URIs (manifest or canvas) mapped to a submap of question URIs mapped to questions
     * @throws URISyntaxException
     * @throws DAOException
     */
    @GET
    @Path("/{campaignId}/{pi}/annotations")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public List<WebAnnotation> getAnnotationsForManifest(@PathParam("campaignId") Long campaignId, @PathParam("pi") String pi) throws URISyntaxException, DAOException {
    
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(campaignId);
        List<PersistentAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, pi);
        
        List webAnnotations = annotations.stream()
                .map(Try.lift(PersistentAnnotation::getAsAnnotation))
                .filter(Try::isSuccess)
                .map(Try::getValue)
                .map(o -> o.get())
                .collect(Collectors.toList());
        webAnnotations.forEach(o -> o.toString());
        
        return webAnnotations;
    }
    
    /**
     * Takes a map of annotation target (canvas/manifest) ids 
     * and replaces all annotations for the given campaign, pi and targeted pages (if target is canvas) with the ones
     * contained in the map
     * 
     * @param map
     * @param campaignId
     * @param pi
     * @throws URISyntaxException
     * @throws DAOException 
     */
    @PUT
    @Path("/{campaignId}/{pi}/annotations")
    @Consumes({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public void setAnnotationsForManifest(List<AnnotationPage> pages, @PathParam("campaignId") Long campaignId, @PathParam("pi") String pi) throws URISyntaxException, DAOException {

        IDAO dao = DataManager.getInstance().getDao();
        Campaign campaign = dao.getCampaign(campaignId);
        
        for (AnnotationPage page : pages) {
            URI targetURI = URI.create(page.getId());
            Integer pageOrder = PersistentAnnotation.parsePageOrder(targetURI);
            List<PersistentAnnotation> existingAnnotations = dao.getAnnotationsForCampaignAndTarget(campaign, pi, pageOrder);
            List<PersistentAnnotation> newAnnotations = page.annotations.stream().map(PersistentAnnotation::new).collect(Collectors.toList());
                  
            //delete existing annotations not in the new annotations list
            List persistenceExceptions =  existingAnnotations.stream()
                    .filter(anno -> newAnnotations.stream().noneMatch(annoNew -> anno.getId().equals(annoNew.getId())))
                    .map(Try.lift(dao::deleteAnnotation))
                    .filter(t -> t.isException()).map(t -> t.getException().get())
                    .collect(Collectors.toList());
            for (Object exception : persistenceExceptions) {
                logger.error("Error deleting annotation " + exception.toString());
            }
            
            //add entirely new annotations
            persistenceExceptions =  newAnnotations.stream()
                    .filter(anno -> anno.getId() == null)
                    .map(Try.lift(dao::addAnnotation))
                    .filter(either -> either.isException())
                    .map(either -> either.getException().get())
                    .collect(Collectors.toList());
            for (Object exception : persistenceExceptions) {
                logger.error("Error adding annotation " + exception.toString());
            }
            
            //update changed annotations
            persistenceExceptions =  newAnnotations.stream()
                    .filter(anno -> anno.getId() != null)
                    .map(Try.lift(dao::updateAnnotation))
                    .filter(either -> either.isException())
                    .map(either -> either.getException().get())
                    .collect(Collectors.toList());
            for (Object exception : persistenceExceptions) {
                logger.error("Error updating annotation " + exception.toString());
            }
        }
    }

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
