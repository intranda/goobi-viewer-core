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
package io.goobi.viewer.api.rest.v1.monitoring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.Version;
import io.goobi.viewer.api.rest.model.MonitoringStatus;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.DAOException;
import io.swagger.v3.oas.annotations.Operation;

@Path(ApiUrls.MONITORING)
public class MonitoringResource {

    private static final Logger logger = LogManager.getLogger(MonitoringResource.class);
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    @Context
    private ContainerRequestContext requestContext;

    /**
     *
     * @param content
     * @param thumbs
     * @param pdf
     * @return
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Checks and reports the availability of relevant data providing services", tags = { "monitoring" })
    public MonitoringStatus checkServices() {
        logger.trace("checkServices");
        MonitoringStatus ret = new MonitoringStatus();

        // Check Solr
        if (!DataManager.getInstance().getSearchIndex().pingSolrIndex()) {
            ret.setSolr(MonitoringStatus.STATUS_ERROR);
            logger.warn("Solr monitoring check failed.");
        }

        // Check DB
        try {
            if (!DataManager.getInstance().getDao().checkAvailability()) {
                ret.setDatabase(MonitoringStatus.STATUS_ERROR);
                logger.warn("DB monitoring check failed.");
            }
        } catch (DAOException e) {
            ret.setDatabase(MonitoringStatus.STATUS_ERROR);
            logger.warn("DB monitoring check failed.");
        }

        // Check image delivery
        try {
            //            new FooterResource(servletRequest).getImage(requestContext, servletRequest, "full", "100,", "0", "default", "jpg");
            NetTools.getWebContentGET(
                    DataManager.getInstance().getConfiguration().getRestApiUrl() + "records/-/files/footer/-/full/100,/0/default.jpg");
        } catch (Exception e) {
            ret.setImages(MonitoringStatus.STATUS_ERROR);
            logger.warn("Image delivery monitoring check failed.");
        }

        return ret;
    }

    @GET
    @Path(ApiUrls.MONITORING_CORE_VERSION)
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Reports the Goobi viewer core version", tags = { "monitoring" })
    public String getCoreVersion() {
        return JsonTools.formatVersionString(Version.asJSON());
    }

}
