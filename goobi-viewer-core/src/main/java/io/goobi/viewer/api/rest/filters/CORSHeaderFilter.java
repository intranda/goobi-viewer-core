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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;

/**
 * <p>
 * Adds an "Access-Control-Allow-Origin" header to a REST response with the value configured in {@link Configuration#getCORSHeaderValue()}
 * </p>
 */
@Provider
@CORSBinding
public class CORSHeaderFilter implements ContainerResponseFilter {

    private static final Configuration config = DataManager.getInstance().getConfiguration();

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (config.isAddCORSHeader()) {
            addAccessControlHeader(responseContext, config.getCORSHeaderValue());
        }
    }

    private static void addAccessControlHeader(ContainerResponseContext response, String content) {
        response.getHeaders().add("Access-Control-Allow-Origin", content);
    }

}
