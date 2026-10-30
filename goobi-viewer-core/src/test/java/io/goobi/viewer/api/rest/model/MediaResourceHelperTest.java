/*
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
package io.goobi.viewer.api.rest.model;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class MediaResourceHelperTest extends AbstractTest {

    @TempDir
    Path viewerHome;

    private MediaResourceHelper helper;
    private HttpServletResponse response;
    private Path pdfFile;
    private Path textFile;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        DataManager.getInstance().getConfiguration().overrideValue("viewerHome", viewerHome.toString());
        Configuration config = mock(Configuration.class);
        when(config.getMediaTypeHandling(any())).thenReturn("attachment");
        helper = new MediaResourceHelper(config);
        response = mock(HttpServletResponse.class);
        pdfFile = Files.writeString(viewerHome.resolve("file.pdf"), "%PDF-1.4");
        textFile = Files.writeString(viewerHome.resolve("file.txt"), "text");
    }

    private static HttpServletRequest clientRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(ClientApplicationManager.CLIENT_IDENTIFIER_HEADER)).thenReturn("test-client-id");
        return request;
    }

    /**
     * @see MediaResourceHelper#setContentHeaders(HttpServletRequest, HttpServletResponse, String, Path, boolean)
     * @verifies set inline disposition for pdf if requested by client application
     */
    @Test
    void setContentHeaders_shouldSetInlineDispositionForPdfIfRequestedByClientApplication() throws IOException {
        helper.setContentHeaders(clientRequest(), response, "file.pdf", pdfFile, false);
        verify(response).setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, "inline");
    }

    /**
     * @see MediaResourceHelper#setContentHeaders(HttpServletRequest, HttpServletResponse, String, Path, boolean)
     * @verifies set inline disposition for any type if requested by client application and inlineForAllTypes is true
     */
    @Test
    void setContentHeaders_shouldSetInlineDispositionForAnyTypeIfRequestedByClientApplicationAndInlineForAllTypesIsTrue() throws IOException {
        helper.setContentHeaders(clientRequest(), response, "file.txt", textFile, true);
        verify(response).setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, "inline");
    }

    /**
     * @see MediaResourceHelper#setContentHeaders(HttpServletRequest, HttpServletResponse, String, Path, boolean)
     * @verifies set configured disposition for non pdf if requested by client application and inlineForAllTypes is false
     */
    @Test
    void setContentHeaders_shouldSetConfiguredDispositionForNonPdfIfRequestedByClientApplicationAndInlineForAllTypesIsFalse() throws IOException {
        helper.setContentHeaders(clientRequest(), response, "file.txt", textFile, false);
        verify(response).setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.buildAttachmentContentDisposition("file.txt"));
    }

    /**
     * @see MediaResourceHelper#setContentHeaders(HttpServletRequest, HttpServletResponse, String, Path, boolean)
     * @verifies set configured disposition for regular request
     */
    @Test
    void setContentHeaders_shouldSetConfiguredDispositionForRegularRequest() throws IOException {
        helper.setContentHeaders(mock(HttpServletRequest.class), response, "file.pdf", pdfFile, true);
        verify(response).setHeader(NetTools.HTTP_HEADER_CONTENT_DISPOSITION, NetTools.buildAttachmentContentDisposition("file.pdf"));
    }
}
