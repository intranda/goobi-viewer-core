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

import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.goobi.viewer.AbstractDatabaseAndSolrEnabledTest;
import io.goobi.viewer.controller.XmlTools;

class MetsResolverTest extends AbstractDatabaseAndSolrEnabledTest {

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return METS file correctly via pi
     */
    @Test
    @Disabled("Make sure the actual METS file is located in the indexed data repo (/opt/digiverso/viewer/data/2)")
    void doGet_shouldReturnMETSFileCorrectlyViaPi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("id", PI_KLEIUNIV);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MetsResolver resolver = new MetsResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        String xml = response.getContentAsString();
        Document mets = XmlTools.getDocumentFromString(xml, StandardCharsets.UTF_8.name());
        Assertions.assertNotNull(mets);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return METS file correctly via urn
     */
    @Test
    @Disabled("Make sure the actual METS file is located in the indexed data repo (/opt/digiverso/viewer/data/1)")
    void doGet_shouldReturnMETSFileCorrectlyViaUrn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("urn", "urn:nbn:de:gbv:9-g-4882158");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MetsResolver resolver = new MetsResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        String xml = response.getContentAsString();
        Document mets = XmlTools.getDocumentFromString(xml, StandardCharsets.UTF_8.name());
        Assertions.assertNotNull(mets);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return LIDO file correctly
     */
    @Test
    @Disabled("Make sure the actual LIDO file is located in the indexed data repo (/opt/digiverso/viewer/data/2)")
    void doGet_shouldReturnLIDOFileCorrectly() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("id", "455820");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MetsResolver resolver = new MetsResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        String xml = response.getContentAsString();
        Document lido = XmlTools.getDocumentFromString(xml, StandardCharsets.UTF_8.name());
        Assertions.assertNotNull(lido);
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if record not in index
     */
    @Test
    void doGet_shouldReturn404IfRecordNotInIndex() throws Exception {
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("id", "NOTFOUND");
            MockHttpServletResponse response = new MockHttpServletResponse();

            MetsResolver resolver = new MetsResolver();
            resolver.doGet(request, response);
            Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
            Assertions.assertEquals(MetsResolver.ERRTXT_DOC_NOT_FOUND, response.getErrorMessage());
        }
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("urn", "urn:nbn:foo:bar");
            MockHttpServletResponse response = new MockHttpServletResponse();

            MetsResolver resolver = new MetsResolver();
            resolver.doGet(request, response);
            Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
            Assertions.assertEquals(MetsResolver.ERRTXT_DOC_NOT_FOUND, response.getErrorMessage());
        }
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 404 if file not found
     */
    @Test
    void doGet_shouldReturn404IfFileNotFound() throws Exception {
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("id", "AC13451894");
            MockHttpServletResponse response = new MockHttpServletResponse();

            MetsResolver resolver = new MetsResolver();
            resolver.doGet(request, response);
            Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
            Assertions.assertTrue(response.getErrorMessage().contains("AC13451894.xml"));
        }
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("urn", "urn:nbn:at:at-akw:g-1016069");
            MockHttpServletResponse response = new MockHttpServletResponse();

            MetsResolver resolver = new MetsResolver();
            resolver.doGet(request, response);
            Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
            Assertions.assertTrue(response.getErrorMessage().contains("AC13451894.xml"));
        }
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 409 if more than one record matched
     */
    @Test
    @Disabled("No URN collisions currently in test index")
    void doGet_shouldReturn409IfMoreThanOneRecordMatched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("urn", "test:1234:goobi:3431");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MetsResolver resolver = new MetsResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_CONFLICT, response.getStatus());
        Assertions.assertEquals(MetsResolver.ERRTXT_MULTIMATCH, response.getErrorMessage());
    }

    /**
     * @see MetsResolver#doGet(HttpServletRequest,HttpServletResponse)
     * @verifies return 400 if record identifier bad
     */
    @Test
    void doGet_shouldReturn400IfRecordIdentifierBad() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("id", "a:b");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MetsResolver resolver = new MetsResolver();
        resolver.doGet(request, response);
        Assertions.assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        Assertions.assertEquals(MetsResolver.ERRTXT_ILLEGAL_IDENTIFIER + ": a:b", response.getErrorMessage());
    }
}
