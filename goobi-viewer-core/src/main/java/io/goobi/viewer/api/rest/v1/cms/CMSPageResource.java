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
package io.goobi.viewer.api.rest.v1.cms;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.bindings.ViewerRestServiceBinding;
import io.goobi.viewer.api.rest.model.ViewerPage;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSPage;

/**
 * Return basic information about cms pages. Used for sliders
 *
 * @author florian
 *
 */

@jakarta.ws.rs.Path("/cms/pages/{pageId}")
@ViewerRestServiceBinding
public class CMSPageResource {

    private static final Logger logger = LogManager.getLogger(CMSPageResource.class);

    private final CMSPage page;

    public CMSPageResource(@PathParam("pageId") Long pageId) throws DAOException {
        this.page = DataManager.getInstance().getDao().getCMSPage(pageId);
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public ViewerPage getPage() {
        return new ViewerPage(page);
    }

}
