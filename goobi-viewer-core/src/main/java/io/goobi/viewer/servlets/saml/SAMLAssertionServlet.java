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
package io.goobi.viewer.servlets.saml;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.model.security.authentication.SAMLProvider;

/**
 * This servlet receives the POST requests from the SAML IdP and extracts the SAMLresponse from the request, which is then passed on to the
 * {@link io.goobi.viewer.model.security.authentication.SAMLProvider} to conclude the authentication flow.
 * 
 * @author Oliver Paetzel
 *
 */
public class SAMLAssertionServlet extends HttpServlet {

    private static final long serialVersionUID = 2145328520265969738L;
    public static final String URL = "saml/";

    private static final Logger logger = LoggerFactory.getLogger(SAMLAssertionServlet.class);

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String encodedResponse = request.getParameter("SAMLResponse");
        SAMLProvider samlProvider = (SAMLProvider) request.getSession().getAttribute("SAMLProvider");

        Future<Boolean> redirectDoneFuture = samlProvider.completeLogin(encodedResponse, request, response);
        try {
            redirectDoneFuture.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Unexpected error while waiting for redirect", e);
        }
    }
}
