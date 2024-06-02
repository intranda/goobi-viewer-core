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
package io.goobi.viewer.api.rest.v1.records;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CHANGES;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_CHANGES_PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.iiif.discovery.Activity;
import de.intranda.api.iiif.discovery.OrderedCollection;
import de.intranda.api.iiif.discovery.OrderedCollectionPage;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
class ChangeDiscoveryResourceTest extends AbstractRestApiTest {

    /**
     * <p>setUp.</p>
     *
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * <p>tearDown.</p>
     *
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetChanges() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_CHANGES).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            OrderedCollection<Activity> activities = new OrderedCollection<>();
            activities = mapper.readValue(entity, OrderedCollection.class);
            assertTrue(activities.getTotalItems() > 0);
            Assertions.assertEquals(urls.path(RECORDS_CHANGES).build(), activities.getId().toString());
            Assertions.assertEquals(urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(0).build(), activities.getFirst().getId().toString());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetChangesPage() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(0).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            OrderedCollectionPage<Activity> activities = new OrderedCollectionPage<>();
            activities = mapper.readValue(entity, activities.getClass());
            Assertions.assertEquals(urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(0).build(), activities.getId().toString());
            Assertions.assertEquals(urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(1).build(), activities.getNext().getId().toString());
            Assertions.assertEquals(urls.path(RECORDS_CHANGES).build(), activities.getPartOf().getId().toString());
            Assertions.assertFalse(activities.getOrderedItems().isEmpty());

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetChangePageCount() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_CHANGES).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals(200, response.getStatus(), "Should return status 200");
            assertNotNull(response.getEntity(), "Should return user object as JSON");
            String entity = response.readEntity(String.class);
            OrderedCollection<Activity> activities = new OrderedCollection<>();
            activities = mapper.readValue(entity, activities.getClass());
            long itemCount = activities.getTotalItems();
            int itemsPerPage = DataManager.getInstance().getConfiguration().getIIIFDiscoveryAvtivitiesPerPage();
            int numPages = (int) (itemCount / itemsPerPage) + 1;

            String lastPageUrl = activities.getLast().getId().toString();
            lastPageUrl = lastPageUrl.substring(0, lastPageUrl.length() - 1);
            String pageNo = lastPageUrl.substring(lastPageUrl.lastIndexOf("/") + 1);
            Assertions.assertEquals(numPages - 1, Integer.parseInt(pageNo), 0);
        }
    }
}
