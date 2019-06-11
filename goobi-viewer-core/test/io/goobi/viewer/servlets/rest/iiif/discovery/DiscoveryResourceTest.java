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
package io.goobi.viewer.servlets.rest.iiif.discovery;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.intranda.api.iiif.discovery.Activity;
import de.intranda.api.iiif.discovery.OrderedCollection;
import de.intranda.api.iiif.discovery.OrderedCollectionPage;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.servlets.rest.iiif.discovery.DiscoveryResource;

/**
 * @author Florian Alpers
 *
 */
public class DiscoveryResourceTest extends AbstractSolrEnabledTest {

    private DiscoveryResource resource = new DiscoveryResource();
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getScheme()).thenReturn("https");
        Mockito.when(request.getServerName()).thenReturn("testServer");
        Mockito.when(request.getServerPort()).thenReturn(80);
        resource.servletRequest = request;

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetAllChanges() throws PresentationException, IndexUnreachableException {
        Mockito.when(resource.servletRequest.getRequestURI()).thenReturn("/viewer/rest/iiif/discovery/activities");
        OrderedCollection<Activity> collection = resource.getAllChanges();
        Assert.assertEquals("https://testServer:80/viewer/rest/iiif/discovery/activities", collection.getId().toString());
        Assert.assertTrue("No activities in collection", collection.getTotalItems() > 0);
        Assert.assertEquals("https://testServer:80/viewer/rest/iiif/discovery/activities/0", collection.getFirst().getId().toString());
    }
    
    @Test
    public void testGetPage() throws PresentationException, IndexUnreachableException {
        Mockito.when(resource.servletRequest.getRequestURI()).thenReturn("/viewer/rest/iiif/discovery/activities/0");
        OrderedCollectionPage<Activity> page = resource.getPage(0);
        Assert.assertEquals("https://testServer:80/viewer/rest/iiif/discovery/activities/0", page.getId().toString());
        Assert.assertEquals("https://testServer:80/viewer/rest/iiif/discovery/activities/1", page.getNext().getId().toString());
        Assert.assertEquals("https://testServer:80/viewer/rest/iiif/discovery/activities", page.getPartOf().getId().toString());
        Assert.assertEquals(DataManager.getInstance().getConfiguration().getIIIFDiscoveryAvtivitiesPerPage(), page.getOrderedItems().size());
    }

    @Test
    public void testPageCount() throws PresentationException, IndexUnreachableException {
        Mockito.when(resource.servletRequest.getRequestURI()).thenReturn("/viewer/rest/iiif/discovery/activities");
        OrderedCollection<Activity> collection = resource.getAllChanges();
        long itemCount = collection.getTotalItems();
        int itemsPerPage = DataManager.getInstance().getConfiguration().getIIIFDiscoveryAvtivitiesPerPage();
        int numPages = (int) (itemCount/itemsPerPage)+1;
        
        String lastPageUrl = collection.getLast().getId().toString();
        String pageNo = lastPageUrl.substring(lastPageUrl.lastIndexOf("/")+1);
        Assert.assertEquals(numPages-1, Integer.parseInt(pageNo), 0);
    }
}
