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
package de.intranda.digiverso.presentation.controller.imaging;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.model.viewer.PhysicalElement;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;

/**
 * @author Florian Alpers
 *
 */
public class ImageHandlerTest {

    ImageHandler handler;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
        
        handler = new ImageHandler();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

//    @Test
    public void testGetImageInformation() throws IllegalPathSyntaxException, URISyntaxException, ContentLibException {
        String url1 = "http://localhost:8081/ics/iiif/image/18979459-1830/00375666.png/info.json";
        String url2 = "18979459-1830/00375666.png";

        ImageInformation info = handler.getImageInformation(url1);
        Assert.assertNotNull(info);
        
        info = handler.getImageInformation(url2);
        Assert.assertNotNull(info);

    }
    
    @Test
    public void testGetImageUrlLocal() {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "00000001.tif", 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getImageUrl(page);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/1234/00000001.tif/info.json", url);   
    }
    
    @Test
    public void testGetImageUrlExternal() {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "http://otherServer/images/00000001.tif/info.json", 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getImageUrl(page);
        Assert.assertEquals("http://otherServer/images/00000001.tif/info.json", url);   
    }
    
    @Test
    public void testGetImageUrlInternal() {
        PhysicalElement page = new PhysicalElement("PHYS_0001", "http://exteral/restricted/images/00000001.tif", 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);

        String url = handler.getImageUrl(page);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/-/http:U002FU002FexteralU002FrestrictedU002FimagesU002F00000001.tif/info.json", url);   
    }
    
    @Test
    public void testResolveURIs() throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
        String stringExternal ="https://localhost:8080/a/b/c d";
        String stringInternal = "file:/a/b/c d#yxwg=123,52,564,213";
        String stringRelative = "a/b/c d [1]-falls.jpg";
        
        URI uriExternal = ImageHandler.toURI(stringExternal);
        Assert.assertEquals("https://localhost:8080/a/b/c%20d", uriExternal.toString());
        URI uriInternal = ImageHandler.toURI(stringInternal);
        Assert.assertEquals("file:///a/b/c%20d#yxwg=123,52,564,213", uriInternal.toString());
        URI uriRelative = ImageHandler.toURI(stringRelative);
        Assert.assertEquals("a/b/c%20d%20%5B1%5D-falls.jpg", uriRelative.toString());
                
        Path pathExternal = ImageHandler.getPath(uriExternal);
        Assert.assertEquals("/a/b/c d", pathExternal.toString());
        Path pathInternal = ImageHandler.getPath(uriInternal);
        Assert.assertEquals("/a/b/c d", pathInternal.toString());
        Path pathRelative = ImageHandler.getPath(uriRelative);
        Assert.assertEquals("a/b/c d [1]-falls.jpg", pathRelative.toString());

    }

}
