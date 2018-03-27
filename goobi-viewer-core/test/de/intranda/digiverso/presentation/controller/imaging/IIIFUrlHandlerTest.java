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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Florian Alpers
 *
 */
public class IIIFUrlHandlerTest {

    IIIFUrlHandler handler;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        handler = new IIIFUrlHandler();
        
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link de.intranda.digiverso.presentation.controller.imaging.IIIFUrlHandler#getIIIFImageUrl(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testGetIIIFImageUrl() {
        
        String fileUrl = "filename.tif";
        String pi = "1234";
        String region = "full";
        String size = "max";
        String rotation = "0";
        String quality = "default";
        String format = "jpg";
        int thumbCompression = 75;
        
        String url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format, thumbCompression);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/1234/filename.tif/full/max/0/default.jpg?compression=75", url);
 
        fileUrl = "http://localhost/image/filename.tif";
        url = handler.getIIIFImageUrl(fileUrl, pi, region, size, rotation, quality, format, thumbCompression);
        Assert.assertEquals("http://localhost:8080/viewer/rest/image/-/http:U002FU002FlocalhostU002FimageU002Ffilename.tif/full/max/0/default.jpg?compression=75", url);

    }


}
