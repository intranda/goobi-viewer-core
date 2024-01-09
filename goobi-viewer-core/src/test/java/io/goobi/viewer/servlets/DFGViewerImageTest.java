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
package io.goobi.viewer.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.controller.DataManager;

/**
 * @author florian
 *
 */
public class DFGViewerImageTest extends AbstractTest {

    DFGViewerImage servlet;


    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        servlet = new DFGViewerImage();
    }

    @Test
    public void testForwardToImageApiUrl() throws ServletException, IOException {

        String requestUrl = "/1574750503285_37/800/0/1575272395963.jpg";
        String expectedForwardUrl = DataManager.getInstance().getConfiguration().getIIIFApiUrl() + "records/1574750503285_37/files/images/1575272395963/full/800,/0/default.jpg";

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(request.getPathInfo()).thenReturn(requestUrl);

        servlet.doGet(request, response);

        Mockito.verify(response).sendRedirect(expectedForwardUrl);
    }

}
