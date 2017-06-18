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
package de.intranda.digiverso.presentation.servlets.rest.iiif.image;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerImageInfoBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ImageResource;

@ContentServerImageInfoBinding
@Path("/fullscreen")
public class FullscreenResource {

    private static final Logger logger = LoggerFactory.getLogger(ImageResource.class);

    @Context
    private ResourceContext resourceContext;

    @Path("/image/{directory}/{filename}")
    public ImageResource forwardToContentServer(@Context ContainerRequestContext request) {
        return resourceContext.getResource(ImageResource.class);
    }

}
