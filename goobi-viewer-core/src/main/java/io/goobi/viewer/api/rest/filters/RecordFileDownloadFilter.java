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
package io.goobi.viewer.api.rest.filters;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.api.rest.bindings.RecordFileDownloadBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.statistics.usage.RequestType;

/**
 * @author florian
 *
 */
@Provider
@RecordFileDownloadBinding
public class RecordFileDownloadFilter implements ContainerRequestFilter {

    private static final Logger logger = LogManager.getLogger(RecordFileDownloadFilter.class);

    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            if (servletRequest.getAttribute("pi") != null) {
                String pi = servletRequest.getAttribute("pi").toString();
                DataManager.getInstance().getUsageStatisticsRecorder().recordRequest(RequestType.FILE_DOWNLOAD, pi, servletRequest);
            }
        } catch (Throwable e) {
            logger.error("Error recording file download: {}", e.toString());
        }
    }
}
