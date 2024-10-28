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
package io.goobi.viewer.api.rest.filters;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.unigoettingen.sub.commons.contentlib.servlet.model.PdfInformation;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerPdfInfoBinding;
import io.goobi.viewer.messages.ViewerResourceBundle;

/**
 * <p>
 * Response filter for PDF info requests. Translates the DocStruct name of the information request
 * </p>
 */
@Provider
@ContentServerPdfInfoBinding
public class PdfInformationFilter implements ContainerResponseFilter {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(PdfInformationFilter.class);

    @Context
    private HttpServletRequest servletRequest;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {

        Object content = response.getEntity();
        if (content instanceof PdfInformation info && info.getDiv() != null) {
            info.setDiv(ViewerResourceBundle.getTranslation(info.getDiv(), null));
        }
    }
}
