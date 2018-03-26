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

import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.Configuration;
import de.intranda.digiverso.presentation.controller.DataManager;

/**
 * @author Florian Alpers
 *
 */
public class PdfHandlerTest {
    
    PdfHandler handler;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
        Configuration configuration = DataManager.getInstance().getConfiguration();
        handler = new PdfHandler(new WatermarkHandler(configuration, "http://localhost:8080/viewer/"), configuration);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
       String pi = "1234";
    Optional<String> divId = Optional.ofNullable("LOG_0003");
    Optional<String> watermarkId = Optional.ofNullable("footerId");
    Optional<String> watermarkText = Optional.ofNullable("watermark text");
    Optional<String> label = Optional.ofNullable("output-filename.pdf");
    
    String url = handler.getPdfUrl(pi, divId, watermarkId, watermarkText, label);
    Assert.assertEquals("http://localhost:8080/viewer/rest/pdf/mets/1234.xml/LOG_0003/outputfilenamepdf.pdf?watermarkText=watermark text&watermarkId=footerId", url);
    }
}
