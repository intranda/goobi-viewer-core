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

import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.iiif.ImageInformation;
import de.unigoettingen.sub.commons.util.datasource.media.PageSource.IllegalPathSyntaxException;

/**
 * @author Florian Alpers
 *
 */
public class ImageHandlerTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetImageInformation() throws IllegalPathSyntaxException, URISyntaxException, ContentLibException {
        String url1 = "http://localhost:8081/ics/iiif/image/18979459-1830/00375666.png/info.json";
        String url2 = "18979459-1830/00375666.png";

        ImageInformation info = new ImageHandler().getImageInformation(url1);
        Assert.assertNotNull(info);
        
        info = new ImageHandler().getImageInformation(url2);
        Assert.assertNotNull(info);

    }

}
