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
package io.goobi.viewer.api.rest.v1.records.media;

import static io.goobi.viewer.api.rest.v1.ApiUrls.RECORDS_FILES_FOOTER;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.FooterResource;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * @author florian
 *
 */
@Path(RECORDS_FILES_FOOTER)
@ContentServerBinding
@CORSBinding
public class RecordFilesFooterResource extends FooterResource {

    private static final Logger logger = LogManager.getLogger(RecordFilesFooterResource.class);

    /**
     * @param context
     * @param request
     * @param response
     * @param urls
     * @param pi
     * @param filename
     * @throws ContentLibException
     */
    public RecordFilesFooterResource(
            @Context ContainerRequestContext context, @Context HttpServletRequest request, @Context HttpServletResponse response,
            @Context AbstractApiUrlManager urls,
            @Parameter(description = "Persistent identifier of the record") @PathParam("pi") String pi,
            @Parameter(description = "Filename of the image") @PathParam("filename") String filename) throws ContentLibException {
        super(request);
    }

}
