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
package io.goobi.viewer.api.rest.v1.search;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;

import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.XmlTools;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.servlets.utils.ServletUtils;
import io.swagger.v3.oas.annotations.Operation;

/**
 * <p>
 * Endpoint for opensearch (https://opensearchfoundation.org/) within the viewer instance. The url is referenced in the header of the template.html in
 * the viewer-theme. This resource returns the xml-document in /resources/opensearch/opensearch.xml
 * </p>
 */
@Path(ApiUrls.OPENSEARCH)
@ViewerRestServiceBinding
public class OpenSearchResource {

    private static final String RESOURCE_URL_REGEX = "\\{resourceUrl:(.+?)\\}";

    private static final Logger logger = LogManager.getLogger(OpenSearchResource.class);

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
    @Operation(tags = { "search" }, summary = "Endpoint for opensearch api")
    public String getXml() {
        String xml = null;
        try {
            String rootUrl = ServletUtils.getServletPathWithHostAsUrlFromRequest(servletRequest);
            //            String url = rootUrl + "/resources/opensearch/opensearch.xml";
            //            logger.trace(url);

            java.nio.file.Path xmlFile = Paths.get("opensearch.xml");
            Document doc = XmlTools.readXmlFile(xmlFile);
            if (doc != null) {
                xml = XmlTools.getStringFromElement(doc, StandardCharsets.UTF_8.name());
                // xml = NetTools.getWebContentGET(url);
                xml = xml.replace("{name}", DataManager.getInstance().getConfiguration().getName())
                        .replace("{description}", DataManager.getInstance().getConfiguration().getDescription())
                        .replace("{applicationUrl}", rootUrl);
                Matcher resourceUrlMatcher = Pattern.compile(RESOURCE_URL_REGEX).matcher(xml);
                Optional<NavigationHelper> onh = BeanUtils.getBeanFromRequest(servletRequest, "navigationHelper", NavigationHelper.class);
                while (resourceUrlMatcher.find()) {
                    String path = resourceUrlMatcher.group(1);
                    String resourcePath = onh.map(nh -> nh.getResource(path))
                            .orElse(rootUrl + "/resources/themes/" + DataManager.getInstance().getConfiguration().getTheme() + path);
                    xml = xml.replaceFirst(RESOURCE_URL_REGEX, resourcePath);
                }
            }
        } catch (ClientProtocolException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException | JDOMException e) {
            logger.error(e.getMessage());
        }

        return xml;

    }
}
