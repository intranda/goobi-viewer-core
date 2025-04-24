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
package io.goobi.viewer.api.rest.v1.records.media;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_PDF;
import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_RECORD;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.api.rest.v1.AbstractRestApiTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * @author florian
 *
 */
class ViewerRecordPDFResourceTest extends AbstractRestApiTest {
    private static final String PI_ACCESS_RESTRICTED = "557335825";
    private static final String PI = "02008031921530";

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractDatabaseAndSolrEnabledTest.setUpClass();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        DataManager.getInstance()
                .injectConfiguration(new Configuration(new File("src/test/resources/config_viewer_no_local_access.test.xml").getAbsolutePath()));
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testGetPdf() throws PresentationException, IndexUnreachableException, ContentLibException, IOException {
        String url = urls.path(RECORDS_RECORD, RECORDS_PDF).params(PI).build();
        Path repository = Path.of(DataFileTools.getDataRepositoryPathForRecord(PI));
        Map<String, String[]> requestParams = new HashMap<>();
        requestParams.put("imageSource",
                new String[] { repository.resolve(DataManager.getInstance().getConfiguration().getMediaFolder()).toUri().toString() });
        requestParams.put("pdfSource",
                new String[] { repository.resolve(DataManager.getInstance().getConfiguration().getPdfFolder()).toUri().toString() });
        requestParams.put("altoSource",
                new String[] { repository.resolve(DataManager.getInstance().getConfiguration().getAltoFolder()).toUri().toString() });
        requestParams.put("metsSource",
                new String[] { repository.resolve(DataManager.getInstance().getConfiguration().getIndexedMetsFolder()).toUri().toString() });

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn(url);
        Mockito.when(request.getParameterMap())
                .thenReturn(requestParams);

        HttpServletResponse response = Mockito.spy(HttpServletResponse.class);
        ContainerRequestContext context = Mockito.mock(ContainerRequestContext.class);

        ContentServerCacheManager cacheManager = ContentServerCacheManager.noCache();
        ViewerRecordPDFResource resource = new ViewerRecordPDFResource(context, request, response, urls, PI, cacheManager);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            resource.getPdf().write(baos);
            assertTrue(baos.size() > 5 * 5 * 8 * 3);
        }
        String expectedContentDisposition = "attachment; filename=\"" + PI + ".pdf" + "\"";
        Mockito.verify(response).addHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, expectedContentDisposition);
    }

}
