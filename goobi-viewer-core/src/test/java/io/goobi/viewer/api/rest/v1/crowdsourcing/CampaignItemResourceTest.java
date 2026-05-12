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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.StatisticMode;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;

/**
 * Unit tests for {@link CampaignItemResource}. Focuses on the ownership-hardening of
 * {@link CampaignItemResource#setItemForManifest(CampaignItem, String, int)}: the actor of a status
 * update must be resolved strictly from the authenticated session, never from the request body.
 */
class CampaignItemResourceTest extends AbstractTest {

    private IDAO daoMock;
    private IDAO previousDao;

    /**
     * AbstractTest declares {@code @BeforeEach setUp()} (configuration reset) and a static
     * {@code @BeforeAll setUpClass()} (RestApiManager bootstrap). We extend the per-test setUp
     * to also swap in a mockable DAO. {@code AbstractDatabaseEnabledTest} is intentionally NOT
     * used because it injects a real {@code JPADAO} which would defeat Mockito-based verification.
     */
    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        previousDao = DataManager.getInstance().getDao();
        daoMock = mock(IDAO.class);
        DataManager.getInstance().injectDao(daoMock);
    }

    @AfterEach
    public void tearDown() {
        // Restore the original DAO so subsequent test classes are unaffected.
        DataManager.getInstance().injectDao(previousDao);
    }

    /**
     * Builds a {@link CampaignItemResource} with a mocked {@link Campaign} (RECORD statistics mode)
     * already registered in the DAO mock under the given campaign id.
     *
     * @param campaignId the campaign id to register on the DAO mock
     * @return a fresh resource instance ready for invocation
     * @throws Exception propagated DAO setup error
     */
    private CampaignItemResource newResource(long campaignId) throws Exception {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getStatisticMode()).thenReturn(StatisticMode.RECORD);
        when(daoMock.getCampaign(campaignId)).thenReturn(campaign);
        HttpServletRequest request = mock(HttpServletRequest.class);
        return new CampaignItemResource(request, campaignId);
    }

    /**
     * @see CampaignItemResource#setItemForManifest(CampaignItem, String, int)
     * @verifies ignore creatorURI from body
     */
    @Test
    void setItemForManifest_shouldIgnoreCreatorUriFromBody() throws Exception {
        long bodyClaimedUserId = 999L;
        CampaignItem item = new CampaignItem();
        item.setRecordStatus(CrowdsourcingStatus.ANNOTATE);
        item.setCreatorURI(URI.create("https://example.test/users/" + bodyClaimedUserId));

        CampaignItemResource resource = newResource(1L);
        resource.setItemForManifest(item, "PI_X", 0);

        // After the fix, the body-URI lookup is gone — the DAO must never be asked
        // to resolve that specific user id as a consequence of the request body.
        verify(daoMock, never()).getUser(bodyClaimedUserId);
    }

    /**
     * The annotation URIs we feed into the resource must match the URL pattern produced
     * by {@code AbstractApiUrlManager.path(ANNOTATIONS, ANNOTATIONS_ANNOTATION)} so that
     * {@code CampaignItemResource.createPersistentAnnotation} can extract the database id.
     * The test config (config_viewer.test.xml) configures
     * {@code <rest>http://localhost:8080/viewer/api/v1/</rest>}, trailing slashes are
     * stripped by {@code ApiUrls}, so the prefix is {@code .../api/v1/annotations/annotation_}.
     * A trailing slash is added by the API URL builder, so the URI must end with {@code /}.
     */
    private static URI annotationUri(long id) {
        return URI.create("http://localhost:8080/viewer/api/v1/annotations/annotation_" + id + "/");
    }

    /**
     * Builds a canvas target id matching {@code RECORDS_PAGES} / {@code RECORDS_PAGES_CANVAS}.
     * The {@code setAnnotationsForManifest} loop parses {@code {pageNo}} from this URI.
     */
    private static String canvasUri(String pi, int pageOrder) {
        return "http://localhost:8080/viewer/api/v1/records/" + pi + "/pages/" + pageOrder + "/canvas/";
    }

    /**
     * @see CampaignItemResource#setAnnotationsForManifest(java.util.List, String)
     * @verifies reject annotation id that belongs to a different campaign
     */
    @Test
    void setAnnotationsForManifest_shouldRejectAnnotationIdThatBelongsToADifferentCampaign() throws Exception {
        long thisCampaignId = 1L;
        long otherCampaignId = 42L;
        long victimAnnotationId = 5000L;

        Campaign foreignCampaign = mock(Campaign.class);
        when(foreignCampaign.getId()).thenReturn(otherCampaignId);
        Question foreignQuestion = mock(Question.class);
        when(foreignQuestion.getOwner()).thenReturn(foreignCampaign);

        CrowdsourcingAnnotation foreign = mock(CrowdsourcingAnnotation.class);
        when(foreign.getId()).thenReturn(victimAnnotationId);
        when(foreign.getTargetPI()).thenReturn("OTHER_PI");
        when(foreign.getTargetPageOrder()).thenReturn(0);
        when(foreign.getGenerator()).thenReturn(foreignQuestion);
        when(daoMock.getAnnotation(victimAnnotationId)).thenReturn(foreign);

        WebAnnotation poisonPill = new WebAnnotation();
        poisonPill.setId(annotationUri(victimAnnotationId));
        CampaignItemResource.AnnotationPage page = new CampaignItemResource.AnnotationPage();
        page.setId(canvasUri("PI_X", 0));
        page.setAnnotations(List.of(poisonPill));

        CampaignItemResource resource = newResource(thisCampaignId);

        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> resource.setAnnotationsForManifest(List.of(page), "PI_X"));
        assertEquals(403, ex.getResponse().getStatus());

        verify(daoMock, never()).updateAnnotation(any());
        verify(daoMock, never()).deleteAnnotation(any());
    }

    /**
     * @see CampaignItemResource#setAnnotationsForManifest(java.util.List, String)
     * @verifies reject annotation id that does not exist
     */
    @Test
    void setAnnotationsForManifest_shouldRejectAnnotationIdThatDoesNotExist() throws Exception {
        long thisCampaignId = 1L;
        long unknownId = 9999L;
        when(daoMock.getAnnotation(unknownId)).thenReturn(null);

        WebAnnotation poison = new WebAnnotation();
        poison.setId(annotationUri(unknownId));
        CampaignItemResource.AnnotationPage page = new CampaignItemResource.AnnotationPage();
        page.setId(canvasUri("PI_X", 0));
        page.setAnnotations(List.of(poison));

        CampaignItemResource resource = newResource(thisCampaignId);
        WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> resource.setAnnotationsForManifest(List.of(page), "PI_X"));
        assertEquals(403, ex.getResponse().getStatus());
        verify(daoMock, never()).updateAnnotation(any());
    }

    /**
     * @see CampaignItemResource#setAnnotationsForManifest(java.util.List, String)
     * @verifies update annotation that belongs to the same campaign and target
     */
    @Test
    void setAnnotationsForManifest_shouldUpdateAnnotationThatBelongsToTheSameCampaignAndTarget() throws Exception {
        long thisCampaignId = 1L;
        long ownAnnotationId = 7L;

        Campaign campaign = mock(Campaign.class);
        when(campaign.getId()).thenReturn(thisCampaignId);
        Question question = mock(Question.class);
        when(question.getOwner()).thenReturn(campaign);

        CrowdsourcingAnnotation own = mock(CrowdsourcingAnnotation.class);
        when(own.getId()).thenReturn(ownAnnotationId);
        when(own.getTargetPI()).thenReturn("PI_X");
        when(own.getTargetPageOrder()).thenReturn(0);
        when(own.getGenerator()).thenReturn(question);
        when(daoMock.getAnnotation(ownAnnotationId)).thenReturn(own);

        WebAnnotation update = new WebAnnotation();
        update.setId(annotationUri(ownAnnotationId));
        CampaignItemResource.AnnotationPage page = new CampaignItemResource.AnnotationPage();
        page.setId(canvasUri("PI_X", 0));
        page.setAnnotations(List.of(update));

        CampaignItemResource resource = newResource(thisCampaignId);
        resource.setAnnotationsForManifest(List.of(page), "PI_X");

        ArgumentCaptor<CrowdsourcingAnnotation> captor = ArgumentCaptor.forClass(CrowdsourcingAnnotation.class);
        verify(daoMock).updateAnnotation(captor.capture());
        assertEquals(ownAnnotationId, captor.getValue().getId());
    }
}
