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
package io.goobi.viewer.model.viewer;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.ContextMocker;

public class PhysicalElementTest extends AbstractDatabaseAndSolrEnabledTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(PhysicalElementTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));

        FacesContext facesContext = ContextMocker.mockFacesContext();
    }

    /**
     * @see PhysicalElement#determineFileName(String)
     * @verifies cut off everything but the file name for normal file paths
     */
    @Test
    public void determineFileName_shouldCutOffEverythingButTheFileNameForNormalFilePaths() throws Exception {
        Assert.assertEquals("image.jpg", PhysicalElement.determineFileName("image.jpg"));
        Assert.assertEquals("image.jpg", PhysicalElement.determineFileName("/opt/digiverso/viewer/media/123/image.jpg"));
    }

    /**
     * @see PhysicalElement#determineFileName(String)
     * @verifies leave external urls intact
     */
    @Test
    public void determineFileName_shouldLeaveExternalUrlsIntact() throws Exception {
        Assert.assertEquals("http://www.example.com/image.jpg", PhysicalElement.determineFileName("http://www.example.com/image.jpg"));
    }

    @Test
    public void isAdaptImageViewHeight_test() {
        PhysicalElement page =
                new PhysicalElement("PHYS_0001", "00000001.tif", 1, "Seite 1", "urn:234235:3423", "http://purl", "1234", "image/tiff", null);
        Assert.assertEquals(0, page.getImageWidth());
        Assert.assertEquals(0, page.getImageHeight());
        Assert.assertTrue(page.isAdaptImageViewHeight());
    }
}