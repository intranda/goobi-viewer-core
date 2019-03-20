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
package de.intranda.digiverso.presentation.servlets.rest.search;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

@Path("/opensearch")
@ViewerRestServiceBinding
public class OpenSearchResource {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchResource.class);

    @Context
    private HttpServletRequest servletRequest;

    @GET
    @Path("/getxml")
    @Produces({ MediaType.TEXT_XML })
    public String getXml() {
        String xml = null;
        try {
            String rootUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest);
            String url = rootUrl + "/resources/opensearch/opensearch.xml";
            logger.trace(url);
            xml = Helper.getWebContentGET(url);
            xml = xml.replace("{name}", DataManager.getInstance().getConfiguration().getName())
                    .replace("{description}", DataManager.getInstance().getConfiguration().getDescription())
                    .replace("{applicationUrl}", rootUrl);
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (HTTPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return xml;

    }
}
