/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
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
package io.goobi.viewer.connector.oai;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Document;

import io.goobi.viewer.connector.oai.servlets.OaiServlet;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Drives {@link OaiServlet} with a mocked request and returns the response it wrote.
 */
public final class OaiServletInvoker {

    private OaiServletInvoker() {
    }

    /**
     * Puts a DAO without any license types in place. The servlet builds an access filter for every request; without a
     * DAO that would fail on the persistence unit, and the license types themselves do not affect the envelope.
     */
    public static void injectEmptyDao() throws DAOException {
        IDAO dao = mock(IDAO.class);
        when(dao.getRecordLicenseTypes()).thenReturn(Collections.emptyList());
        when(dao.getAllLicenseTypes()).thenReturn(Collections.emptyList());
        io.goobi.viewer.controller.DataManager.getInstance().injectDao(dao);
    }

    /**
     * Runs the servlet with the given request parameters and asserts that the response is schema-valid.
     *
     * @param parameters query parameters of the protocol request
     * @return the parsed response
     */
    public static Document call(Map<String, String> parameters) throws Exception {
        return call(parameters, Map.of());
    }

    /**
     * Runs the servlet with the given request parameters and asserts that the response is schema-valid.
     *
     * @param parameters query parameters of the protocol request
     * @param repeatedValues values for arguments that are supplied more than once
     * @return the parsed response
     */
    public static Document call(Map<String, String> parameters, Map<String, String[]> repeatedValues) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/viewer/oai"));
        when(request.getQueryString()).thenReturn(null);

        Map<String, String[]> parameterMap = new HashMap<>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String[] values = repeatedValues.getOrDefault(entry.getKey(), new String[] { entry.getValue() });
            parameterMap.put(entry.getKey(), values);
            when(request.getParameter(entry.getKey())).thenReturn(entry.getValue());
            when(request.getParameterValues(entry.getKey())).thenReturn(values);
        }
        when(request.getParameterMap()).thenReturn(parameterMap);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // no asynchronous writing in this test
            }
        });

        new OaiServlet().doGet(request, response);
        return OaiResponseValidator.assertValid(out.toString(StandardCharsets.UTF_8));
    }

    /**
     * Builds a parameter map from alternating keys and values.
     *
     * @param keyValuePairs alternating argument names and values
     * @return the parameter map
     */
    public static Map<String, String> params(String... keyValuePairs) {
        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            ret.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return ret;
    }
}
