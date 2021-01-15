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
package io.goobi.viewer.api.rest.v1.search;

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

import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;

/**
 * <p>
 * Endpoint for opensearch (https://opensearchfoundation.org/) within the viewer instance. 
 * The url is referenced in the header of the template.html in the viewer-theme.
 * This resource returns the xml-document in /resources/opensearch/opensearch.xml
 * </p>
 */
@Path(ApiUrls.OPENSEARCH)
@ViewerRestServiceBinding
public class OpenSearchResource {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchResource.class);

    @Context
    private HttpServletRequest servletRequest;

    /**
     * <p>
     * getXml.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    @GET
    @Produces({ MediaType.TEXT_XML })
    @Operation(tags= {"search"}, summary="Endpoint for opensearch api")
    public String getXml() {
        String xml = null;
        try {
            String rootUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest);
            String url = rootUrl + "/resources/opensearch/opensearch.xml";
            logger.trace(url);
            xml = NetTools.getWebContentGET(url);
            xml = xml.replace("{name}", DataManager.getInstance().getConfiguration().getName())
                    .replace("{description}", DataManager.getInstance().getConfiguration().getDescription())
                    .replace("{applicationUrl}", rootUrl);
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (HTTPException e) {
            logger.error(e.getMessage());
        }

        return xml;

    }
}
