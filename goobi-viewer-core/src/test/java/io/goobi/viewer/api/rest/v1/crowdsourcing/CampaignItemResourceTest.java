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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign.StatisticMode;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignItem;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import jakarta.servlet.http.HttpServletRequest;

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
}
