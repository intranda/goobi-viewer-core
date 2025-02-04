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
package io.goobi.viewer.api.rest.v1.index;

import static io.goobi.viewer.api.rest.v1.ApiUrls.INDEXER;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.IndexerDataRequestParameters;
import io.goobi.viewer.api.rest.model.SuccessMessage;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.MessageQueueException;
import io.goobi.viewer.managedbeans.AdminBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.job.mq.RefreshArchiveTreeHandler;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * Resource for communicating with the indexer process.
 */
@Path(INDEXER)
@ViewerRestServiceBinding
public class IndexerResource {

    private static final Logger logger = LogManager.getLogger(IndexerResource.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @Inject
    private MessageQueueManager mqm;

    /**
     * Used by the Solr indexer to submit its current version and hotfolder file count.
     * 
     * @param params
     * @return {@link SuccessMessage}
     * @throws IllegalRequestException
     * @throws MessageQueueException
     */
    @PUT
    @Path("/version")
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    public SuccessMessage setIndexerVersion(IndexerDataRequestParameters params) throws IllegalRequestException, MessageQueueException {
        try {
            DataManager.getInstance().setIndexerVersion(new ObjectMapper().writeValueAsString(params));
            DataManager.getInstance().setHotfolderFileCount(params.getHotfolderFileCount());
            if (params.getRecordIdentifiers() != null && !params.getRecordIdentifiers().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String pi : params.getRecordIdentifiers()) {
                    logger.trace("Received PI: {}", pi);
                    sb.append(pi).append(" ");
                }
                ViewerMessage message = new ViewerMessage(TaskType.REFRESH_ARCHIVE_TREE.name());
                message.getProperties().put(RefreshArchiveTreeHandler.PARAMETER_IDENTIFIERS, sb.toString().trim());
                mqm.addToQueue(message);
            }
            AdminBean ab = BeanUtils.getAdminBean();
            if (ab != null) {
                ab.updateHotfolderFileCount();
            } else {
                logger.warn("AdminBean null");
            }

            return new SuccessMessage(true);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw new IllegalRequestException("Cannot parse request body to json ", e);
        }
    }
}
