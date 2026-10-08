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
package io.goobi.viewer.api.rest.v1.archives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.api.rest.bindings.AccessRightsBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.archives.CardCatalogDrawer;
import io.goobi.viewer.model.archives.CardCatalogIndex;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ForbiddenException;

/**
 * Integration tests for the card catalog endpoints against the EAD archive of the test index ("Akte_Koch_-_Humboldt_Universitaet").
 */
class CardCatalogResourceTest extends AbstractRestApiTest {

    private static final String PI = "Akte_Koch_-_Humboldt_Universitaet";

    /**
     * @verifies return catalog info with drawers
     */
    @Test
    void getInfo_shouldReturnCatalogInfoWithDrawers() {
        try (Response response = target("/archives/" + PI + "/cardcatalog/").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject info = new JSONObject(response.readEntity(String.class));
            assertEquals(PI, info.getString("pi"));
            assertTrue(info.getInt("totalEntries") > 0);
            assertFalse(info.getJSONArray("drawers").isEmpty());
            assertTrue(info.getJSONObject("config").getInt("zoomFactor") >= 2);
            assertTrue(info.getJSONObject("config").getInt("zoomFactor") < 25);
            assertTrue(info.getJSONObject("config").getInt("fullViewThreshold") >= 2);
            assertEquals("private, no-store", response.getHeaderString("Cache-Control"));
        }
    }

    @Test
    void catalogAccess_shouldRequireRootPrivilegeAndRejectRestrictedDescendants() {
        AccessRightsBinding binding = CardCatalogResource.class.getAnnotation(AccessRightsBinding.class);
        assertNotNull(binding);
        assertEquals(List.of(IPrivilegeHolder.PRIV_ARCHIVE_DISPLAY_NODE), Arrays.asList(binding.value()));
        CardCatalogIndex index = new CardCatalogIndex(PI, "Catalog", 1, List.of(), true);
        assertThrows(ForbiddenException.class, () -> CardCatalogResource.requirePublicDescendants(index));
    }

    /**
     * @verifies return boundaries and entries for a drawer
     */
    @Test
    void getBoundariesAndEntries_shouldReturnCardsForADrawer() {
        String drawerId;
        int entryCount;
        String firstEntryId = null;
        int firstEntryPosition = 0;
        try (Response response = target("/archives/" + PI + "/cardcatalog/").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject info = new JSONObject(response.readEntity(String.class));
            drawerId = info.getJSONArray("drawers").getJSONObject(0).getString("id");
            entryCount = info.getJSONArray("drawers").getJSONObject(0).getInt("entryCount");
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/drawers/" + drawerId + "/boundaries").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject section = new JSONObject(response.readEntity(String.class));
            assertFalse(section.getJSONArray("items").isEmpty());
            assertEquals(1, section.getJSONArray("items").getJSONObject(0).getInt("position"));
            assertEquals(entryCount, section.getJSONArray("items").getJSONObject(section.getJSONArray("items").length() - 1).getInt("position"));
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/drawers/" + drawerId + "/entries")
                .queryParam("from", 1)
                .queryParam("to", 3)
                .request()
                .get()) {
            assertEquals(200, response.getStatus());
            JSONObject section = new JSONObject(response.readEntity(String.class));
            assertEquals(Math.min(3, entryCount), section.getJSONArray("items").length());
            for (int i = 0; i < section.getJSONArray("items").length(); i++) {
                assertEquals(i + 1, section.getJSONArray("items").getJSONObject(i).getInt("position"));
            }
            for (int i = 0; i < section.getJSONArray("items").length(); i++) {
                if (!section.getJSONArray("items").getJSONObject(i).isNull("entryId")) {
                    firstEntryId = section.getJSONArray("items").getJSONObject(i).getString("entryId");
                    firstEntryPosition = i + 1;
                    break;
                }
            }
        }
        assertNotNull(firstEntryId);
        try (Response response = target("/archives/" + PI + "/cardcatalog/lookup")
                .queryParam("entryId", firstEntryId)
                .request()
                .get()) {
            assertEquals(200, response.getStatus());
            JSONObject location = new JSONObject(response.readEntity(String.class));
            assertEquals(drawerId, location.getString("drawerId"));
            assertEquals(firstEntryPosition, location.getInt("position"));
        }
        if (entryCount > 1) {
            try (Response response = target("/archives/" + PI + "/cardcatalog/drawers/" + drawerId + "/entries")
                    .queryParam("from", 2)
                    .queryParam("to", 2)
                    .request()
                    .get()) {
                assertEquals(200, response.getStatus());
                JSONObject section = new JSONObject(response.readEntity(String.class));
                assertEquals(1, section.getJSONArray("items").length());
                assertEquals(2, section.getJSONArray("items").getJSONObject(0).getInt("position"));
            }
        }
    }

    /**
     * @verifies return register below the configured threshold
     */
    @Test
    void getRegister_shouldReturnRegisterBelowThreshold() {
        // test config: registerThreshold=2000, the test archive has a few hundred nodes
        String drawerId;
        try (Response response = target("/archives/" + PI + "/cardcatalog/").request().get()) {
            assertEquals(200, response.getStatus());
            drawerId = new JSONObject(response.readEntity(String.class)).getJSONArray("drawers").getJSONObject(0).getString("id");
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/register").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject register = new JSONObject(response.readEntity(String.class));
            assertTrue(register.getBoolean("available"));
            assertFalse(register.getJSONArray("drawers").isEmpty());
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/drawers/" + drawerId + "/register").request().get()) {
            assertEquals(200, response.getStatus());
            JSONObject register = new JSONObject(response.readEntity(String.class));
            assertTrue(register.getBoolean("available"));
            assertEquals(1, register.getJSONArray("drawers").length());
            assertEquals(drawerId, register.getJSONArray("drawers").getJSONObject(0).getString("id"));
            assertFalse(register.getJSONArray("drawers").getJSONObject(0).getJSONArray("ranges").isEmpty());
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/drawers/nosuchdrawer/register").request().get()) {
            assertEquals(404, response.getStatus());
        }
    }

    /**
     * @verifies return 404 for unknown archives and entries
     */
    @Test
    void endpoints_shouldRejectUnknownArchivesAndReturn404ForUnknownEntries() {
        try (Response response = target("/archives/NOSUCHARCHIVE/cardcatalog/").request().get()) {
            assertEquals(403, response.getStatus());
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/lookup")
                .queryParam("entryId", "nosuchentry")
                .request()
                .get()) {
            assertEquals(404, response.getStatus());
        }
        try (Response response = target("/archives/" + PI + "/cardcatalog/lookup")
                .queryParam("entryId", "nosuchentry\" OR *:*")
                .request()
                .get()) {
            assertEquals(404, response.getStatus());
        }
    }

    @Test
    void solrRankQueries_shouldResolveDenseSiblingPositions() throws Exception {
        CardCatalogDrawer drawer = DataManager.getInstance().getCardCatalogManager().getCatalog(PI).orElseThrow().getDrawers().get(0);
        String base = "+" + SolrConstants.DOCTYPE + ":ARCHIVE +" + SolrConstants.IDDOC_PARENT + ":\""
                + ClientUtils.escapeQueryChars(drawer.getIddoc()) + "\"";
        int last = Math.min(2, drawer.getEntryCount() - 1);
        SolrDocumentList range = DataManager.getInstance().getSearchIndex()
                .search(base + " +SORTNUM_ARCHIVE_ORDER:[0 TO " + last + "]", 0, last + 1,
                        List.of(new StringPair("SORTNUM_ARCHIVE_ORDER", "asc")), null,
                        Arrays.asList(SolrConstants.EAD_NODE_ID, "SORTNUM_ARCHIVE_ORDER"))
                .getResults();
        assertEquals(last + 1, range.getNumFound());
        for (int i = 0; i <= last; i++) {
            assertEquals(i, ((Number) range.get(i).getFieldValue("SORTNUM_ARCHIVE_ORDER")).intValue());
        }
        SolrDocumentList terms = DataManager.getInstance().getSearchIndex()
                .search(base + " +SORTNUM_ARCHIVE_ORDER:(0 " + last + ")", 0, 2,
                        List.of(new StringPair("SORTNUM_ARCHIVE_ORDER", "asc")), null,
                        Arrays.asList(SolrConstants.EAD_NODE_ID, "SORTNUM_ARCHIVE_ORDER"))
                .getResults();
        assertEquals(last == 0 ? 1 : 2, terms.getNumFound());
    }
}
