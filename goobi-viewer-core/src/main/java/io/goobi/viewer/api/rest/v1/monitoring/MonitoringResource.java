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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentNotFoundException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ApplicationInfo;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ApplicationResource;
import io.goobi.viewer.Version;
import io.goobi.viewer.api.rest.model.monitoring.MonitoringStatus;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.JsonTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.solr.SolrTools;
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
    @Inject
    private MessageQueueManager messageBroker;

    /**
     * @return {@link MonitoringStatus} as JSON
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Checks and reports the availability of relevant data providing services", tags = { "monitoring" })
    public MonitoringStatus checkServices() {
        logger.trace("checkServices");
        MonitoringStatus ret = new MonitoringStatus();

        // Check Solr
        if (!DataManager.getInstance().getSearchIndex().pingSolrIndex()) {
            ret.getMonitoring().put(MonitoringStatus.KEY_SOLR, MonitoringStatus.STATUS_ERROR);
            logger.warn("Solr monitoring check failed.");
        }

        // Check Solr schema version
        String[] result = SolrTools.checkSolrSchemaName();
        int status = Integer.parseInt(result[0]);
        if (status != 200) {
            ret.getMonitoring().put(MonitoringStatus.KEY_SOLRSCHEMA, result[1]);
        }

        // Check DB
        try {
            if (!DataManager.getInstance().getDao().checkAvailability()) {
                ret.getMonitoring().put(MonitoringStatus.KEY_DATABASE, MonitoringStatus.STATUS_ERROR);
                logger.warn("DB monitoring check failed.");
            }
        } catch (DAOException e) {
            ret.getMonitoring().put(MonitoringStatus.KEY_DATABASE, MonitoringStatus.STATUS_ERROR);
            logger.warn("DB monitoring check failed.");
        }

        // Check image delivery
        try {
            NetTools.getWebContentGET(
                    DataManager.getInstance().getConfiguration().getRestApiUrl() + "records/-/files/footer/-/full/100,/0/default.jpg");
        } catch (HTTPException | IOException e) {
            ret.getMonitoring().put(MonitoringStatus.KEY_IMAGES, MonitoringStatus.STATUS_ERROR);
            logger.warn("Image delivery monitoring check failed.");
        }

        // Check message queue status
        if (messageBroker != null) {
            if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
                ret.getMonitoring()
                        .put(MonitoringStatus.KEY_MESSAGE_QUEUE,
                                messageBroker.isQueueRunning() ? MonitoringStatus.STATUS_OK : MonitoringStatus.STATUS_ERROR);
            } else {
                ret.getMonitoring()
                        .put(MonitoringStatus.KEY_MESSAGE_QUEUE, MonitoringStatus.STATUS_DISABLED);
            }
        } else {
            logger.warn("MessageQueueManager injection failed.");
        }

        // viewer-core version
        Map<String, String> coreVersion = ret.getVersions().computeIfAbsent("core", k -> new HashMap<>(2));
        coreVersion.put("version", Version.VERSION);
        coreVersion.put("hash", Version.BUILDVERSION);

        // connector version
        Map<String, String> connectorVersion = ret.getVersions().computeIfAbsent("connector", k -> new HashMap<>(2));
        setVersionValues(connectorVersion, DataManager.getInstance().getConnectorVersion());

        // indexer version
        Map<String, String> indexerVersion = ret.getVersions().computeIfAbsent("indexer", k -> new HashMap<>(2));
        setVersionValues(indexerVersion, DataManager.getInstance().getIndexerVersion());

        // ICS version
        Map<String, String> icsVersion = ret.getVersions().computeIfAbsent("contentserver", k -> new HashMap<>(2));
        try {
            ApplicationInfo info = new ApplicationResource().getApplicationInfo();
            String json = new ObjectMapper().writeValueAsString(info);
            setVersionValues(icsVersion, json);
        } catch (ContentNotFoundException | IOException e) {
            logger.error(e.getMessage());
        }

        //  module versions
        for (IModule module : DataManager.getInstance().getModules()) {
            Map<String, String> moduleVersion = ret.getVersions().computeIfAbsent(module.getId(), k -> new HashMap<>(2));
            setVersionValues(moduleVersion, module.getVersionJson());
        }

        return ret;
    }

    /**
     * 
     * @param versionMap
     * @param versionJson
     */
    private static void setVersionValues(Map<String, String> versionMap, String versionJson) {
        versionMap.put("version", JsonTools.getVersion(versionJson));
        versionMap.put("hash", JsonTools.getGitRevision(versionJson));
    }
}
