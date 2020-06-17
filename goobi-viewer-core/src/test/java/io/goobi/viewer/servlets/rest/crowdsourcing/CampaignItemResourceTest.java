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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.WebAnnotation;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.ApiUrlManager;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic.CampaignRecordStatus;
import io.goobi.viewer.model.iiif.presentation.builder.ManifestBuilder;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.servlets.rest.crowdsourcing.CampaignItemResource.AnnotationPage;

/**
 * @author florian
 *
 */
public class CampaignItemResourceTest extends AbstractDatabaseAndSolrEnabledTest {

    private CampaignItemResource resource;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        resource = new CampaignItemResource(request, response);
    }

    @Test
    public void testGetItemForManifest() throws ContentNotFoundException, URISyntaxException, DAOException {
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1l);
        CampaignItem item = resource.getItemForManifest(1l, "PPN1234");
        Assert.assertNotNull(item);
        Assert.assertEquals(campaign, item.getCampaign());

        URI manifestUrl = new ManifestBuilder(new ApiUrlManager(DataManager.getInstance().getConfiguration().getRestApiUrl()))
                .getManifestURI("PPN1234");
        Assert.assertEquals(manifestUrl, item.getSource());
    }

    /**
     * Note: setting a user for the status update breaks h2 database
     * 
     * @throws ContentNotFoundException
     * @throws URISyntaxException
     * @throws DAOException
     */
    @Test
    public void testSetItemForManifest() throws ContentNotFoundException, URISyntaxException, DAOException {

        String pi = "PPN1234";
        CampaignItem item = resource.getItemForManifest(1l, pi);

        User user = DataManager.getInstance().getDao().getUser(1l);
        item.setCreatorURI(user.getIdAsURI());
        item.setRecordStatus(CampaignRecordStatus.REVIEW);
        Assert.assertEquals(user.getIdAsURI(), item.getCreatorURI());

        resource.setItemForManifest(item, 1l, pi);

        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1l);

        Assert.assertEquals(CampaignRecordStatus.REVIEW, campaign.getRecordStatus(pi));
        Assert.assertTrue(campaign.getStatistics().get(pi).getAnnotators().contains(user));

    }

    @Test
    public void testGetAnnotationsForManifest() throws ContentNotFoundException, URISyntaxException, DAOException {
        String pi = "PI 1";
        List<WebAnnotation> annotationList = resource.getAnnotationsForManifest(1l, pi);
        Assert.assertEquals(2, annotationList.size());
    }

    @Test
    public void testSetAnnotationsForManifest()
            throws ContentNotFoundException, URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {
        String pi = "PI_10";
        URI manifestUrl =
                new ManifestBuilder(new ApiUrlManager(DataManager.getInstance().getConfiguration().getRestApiUrl())).getManifestURI(pi);
        Campaign campaign = DataManager.getInstance().getDao().getCampaign(1l);

        WebAnnotation anno = new WebAnnotation();
        anno.setBody(new SimpleResource(URI.create("F")));
        anno.setTarget(new SimpleResource(manifestUrl));
        anno.setGenerator(new Agent(campaign.getQuestions().get(0).getIdAsURI(), AgentType.SOFTWARE, campaign.getTitle()));

        AnnotationPage page = new AnnotationPage();
        page.setId(manifestUrl.toString());
        page.setAnnotations(Collections.singletonList(anno));
        resource.setAnnotationsForManifest(Collections.singletonList(page), campaign.getId(), pi);

        Assert.assertFalse(DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, pi).isEmpty());
        WebAnnotation anno2 = DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, pi).get(0).getAsAnnotation();
        Assert.assertEquals(anno.getBody(), anno2.getBody());
        Assert.assertEquals(anno.getTarget(), anno2.getTarget());
        Assert.assertEquals(anno.getGenerator(), anno2.getGenerator());
    }

}
