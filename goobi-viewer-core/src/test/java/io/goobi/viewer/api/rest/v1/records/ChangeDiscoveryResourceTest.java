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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
public class ChangeDiscoveryResourceTest extends AbstractRestApiTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetChanges() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_CHANGES).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            OrderedCollection<Activity> activities = new OrderedCollection<>();
            activities = mapper.readValue(entity, OrderedCollection.class);
            assertTrue(activities.getTotalItems() > 0);
            Assert.assertEquals(urls.path(RECORDS_CHANGES).build(), activities.getId().toString());
            Assert.assertEquals(urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(0).build(), activities.getFirst().getId().toString());

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetChangesPage() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(0).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            OrderedCollectionPage<Activity> activities = new OrderedCollectionPage<>();
            activities = mapper.readValue(entity, activities.getClass());
            Assert.assertEquals(urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(0).build(), activities.getId().toString());
            Assert.assertEquals(urls.path(RECORDS_CHANGES, RECORDS_CHANGES_PAGE).params(1).build(), activities.getNext().getId().toString());
            Assert.assertEquals(urls.path(RECORDS_CHANGES).build(), activities.getPartOf().getId().toString());
            Assert.assertFalse(activities.getOrderedItems().isEmpty());

        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetChangePageCount() throws JsonMappingException, JsonProcessingException {
        try (Response response = target(urls.path(RECORDS_CHANGES).build())
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get()) {
            assertEquals("Should return status 200", 200, response.getStatus());
            assertNotNull("Should return user object as json", response.getEntity());
            String entity = response.readEntity(String.class);
            OrderedCollection<Activity> activities = new OrderedCollection<>();
            activities = mapper.readValue(entity, activities.getClass());
            long itemCount = activities.getTotalItems();
            int itemsPerPage = DataManager.getInstance().getConfiguration().getIIIFDiscoveryAvtivitiesPerPage();
            int numPages = (int) (itemCount / itemsPerPage) + 1;

            String lastPageUrl = activities.getLast().getId().toString();
            lastPageUrl = lastPageUrl.substring(0, lastPageUrl.length() - 1);
            String pageNo = lastPageUrl.substring(lastPageUrl.lastIndexOf("/") + 1);
            Assert.assertEquals(numPages - 1, Integer.parseInt(pageNo), 0);
        }
    }

}
