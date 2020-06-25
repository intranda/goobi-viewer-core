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
package io.goobi.viewer.servlets.rest.iiif.presentation;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.IPresentationModelElement;
import de.intranda.api.iiif.presentation.Layer;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.Range;
import de.intranda.api.iiif.presentation.enums.AnnotationType;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.servlets.rest.iiif.presentation.ManifestResource;

/**
 * @author Florian Alpers
 *
 */
public class ManifestResourceTest extends AbstractDatabaseAndSolrEnabledTest {

    private ManifestResource resource;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        resource = new ManifestResource(request, response);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link io.goobi.viewer.servlets.rest.iiif.presentation.ManifestResource#getManifest(java.lang.String)}.
     * 
     * @throws DAOException
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ContentNotFoundException
     * @throws ViewerConfigurationException
     */
    @Test
    public void testGetManifest() throws ViewerConfigurationException, ContentNotFoundException, PresentationException, IndexUnreachableException,
            URISyntaxException, DAOException {
        IPresentationModelElement manifest = resource.getManifest(PI_KLEIUNIV);
        Assert.assertTrue(manifest instanceof Manifest);
    }

    /**
     * Test method for {@link io.goobi.viewer.servlets.rest.iiif.presentation.ManifestResource#getRange(java.lang.String, java.lang.String)}.
     * 
     * @throws DAOException
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ContentNotFoundException
     * @throws ViewerConfigurationException
     */
    @Test
    public void testGetRange() throws ViewerConfigurationException, ContentNotFoundException, PresentationException, IndexUnreachableException,
            URISyntaxException, DAOException {
        Range range = resource.getRange(PI_KLEIUNIV, "LOG_0003");
        Assert.assertTrue(range instanceof Range);
    }

    /**
     * Test method for {@link io.goobi.viewer.servlets.rest.iiif.presentation.ManifestResource#getCanvas(java.lang.String, int)}.
     * 
     * @throws DAOException
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws ContentNotFoundException
     * @throws ViewerConfigurationException
     */
    @Test
    public void testGetCanvas() throws ViewerConfigurationException, ContentNotFoundException, PresentationException, IndexUnreachableException,
            URISyntaxException, DAOException {
        Canvas canvas = resource.getCanvas(PI_KLEIUNIV, 1);
        Assert.assertTrue(canvas instanceof Canvas);
    }

    /**
     * Test method for
     * {@link io.goobi.viewer.servlets.rest.iiif.presentation.ManifestResource#getOtherContent(java.lang.String, int, java.lang.String)}.
     * 
     * @throws DAOException
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws IllegalRequestException
     * @throws ContentNotFoundException
     * @throws ViewerConfigurationException
     */
    @Test
    public void testGetOtherContent() throws ViewerConfigurationException, ContentNotFoundException, IllegalRequestException, PresentationException,
            IndexUnreachableException, URISyntaxException, DAOException {
        try {
            AnnotationList annoList = resource.getOtherContent(PI_KLEIUNIV, 1, AnnotationType.FULLTEXT.name());
            Assert.assertTrue(annoList instanceof AnnotationList);
        } catch (ContentNotFoundException e) {
            //may be thrown if no fulltext content exists. Do not fail test
        }
    }

    /**
     * Test method for {@link io.goobi.viewer.servlets.rest.iiif.presentation.ManifestResource#getLayer(java.lang.String, java.lang.String)}.
     * 
     * @throws IOException
     * @throws DAOException
     * @throws URISyntaxException
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws IllegalRequestException
     * @throws ContentNotFoundException
     * @throws ViewerConfigurationException
     */
    @Test
    public void testGetLayer() throws ViewerConfigurationException, ContentNotFoundException, IllegalRequestException, PresentationException,
            IndexUnreachableException, URISyntaxException, DAOException, IOException {
        Layer layer = resource.getLayer(PI_KLEIUNIV, AnnotationType.FULLTEXT.name());
        Assert.assertTrue(layer instanceof Layer);
    }

}
