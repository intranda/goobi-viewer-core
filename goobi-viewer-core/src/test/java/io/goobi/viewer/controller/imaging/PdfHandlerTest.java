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
package io.goobi.viewer.controller.imaging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.ConfigurationTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.imaging.PdfHandler;
import io.goobi.viewer.controller.imaging.WatermarkHandler;

/**
 * @author Florian Alpers
 *
 */
public class PdfHandlerTest extends AbstractTest{

    PdfHandler handler;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Configuration configuration = DataManager.getInstance().getConfiguration();
        AbstractApiUrlManager urls = new ApiUrls("https://viewer.goobi.io/api/v1/");
        handler = new PdfHandler(new WatermarkHandler(configuration, "http://localhost:8080/viewer/"), urls);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        String pi = "1234";
        Optional<String> divId = Optional.ofNullable("LOG_0003");
        Optional<String> label = Optional.ofNullable("output-filename.pdf");

        String url = handler.getPdfUrl(pi, divId, label);
        Assertions.assertEquals(ConfigurationTest.APPLICATION_ROOT_URL
                + "api/v1/records/1234/sections/LOG_0003/pdf/", url);
    }
}
