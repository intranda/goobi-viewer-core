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
package de.intranda.digiverso.presentation.servlets.rest.iiif.image;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerResource;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ImageResource;

public class ContentServerApplicationTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private URI requestURI;
    private String sampleFileName;

    @Before
    public void before() throws URISyntaxException {

        File baseDir = new File("resources/test");
        ContentServerConfiguration.setBasePath(baseDir.getAbsolutePath());
        //    	Assert.assertTrue("Config path " + ContentServerConfiguration.getBasePathAsFile().getAbsolutePath() + " is no directory",
        //    			ContentServerConfiguration.getBasePathAsFile().isDirectory());
        //    	Assert.assertTrue("Config file " + ContentServerConfiguration.getBasePathAsFile().getAbsolutePath() + "contentServerConfig.xml" + " is no file",
        //    			new File(ContentServerConfiguration.getBaseBathAsString(), "contentServerConfig.xml").isFile());
        File sampleFile = new File("stuff/images/multivolume_thumbnail.png");
        sampleFileName = "file://" + sampleFile.getAbsolutePath();
        sampleFileName = sampleFileName.replaceAll("\\s", "%20");
        sampleFileName = sampleFileName.replace("\\", "/");
        sampleFileName = new URI(sampleFileName).toString();

        requestURI = new URI("http://intranda/viewer/iiif");

        request = Mockito.mock(HttpServletRequest.class);
        Mockito.doReturn(requestURI.getScheme()).when(request).getScheme();
        Mockito.doReturn(requestURI.getHost()).when(request).getServerName();
        Mockito.doReturn(requestURI.getPath()).when(request).getServletPath();
        Mockito.doReturn("").when(request).getContextPath();
        Mockito.doReturn(requestURI.getPort()).when(request).getServerPort();
        Mockito.doReturn(ContentServerResource.MEDIA_TYPE_APPLICATION_JSONLD).when(request).getHeader("Accept");

        response = Mockito.mock(HttpServletResponse.class);

    }

    @Test
    public void testResolveRepositoryURI() throws ContentLibException {

        ImageResource service = new ImageResource(request, "sampleDir", "sampleImage.tif");
        Assert.assertEquals("sampleDir/sampleImage.tif", service.getImageURI().toString());
        Assert.assertEquals(requestURI.toString() + "/image/sampleDir/sampleImage.tif", service.getResourceURI().toString());
    }

    @Test
    public void testResolveAbsoluteURI() throws ContentLibException, UnsupportedEncodingException {
        ImageResource service = new ImageResource(request, "-", URLEncoder.encode(sampleFileName, "utf-8"));
        Assert.assertEquals(requestURI.toString() + "/image/-/" + URLEncoder.encode(sampleFileName, "utf-8"), service.getResourceURI().toString());
        Assert.assertEquals(sampleFileName, service.getImageURI().toString());
    }

}
