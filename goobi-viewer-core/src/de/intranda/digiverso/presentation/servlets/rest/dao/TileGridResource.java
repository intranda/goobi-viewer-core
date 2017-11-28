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
package de.intranda.digiverso.presentation.servlets.rest.dao;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.UserBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.tilegrid.TileGrid;
import de.intranda.digiverso.presentation.model.cms.tilegrid.TileGridBuilder;
import de.intranda.digiverso.presentation.servlets.rest.ViewerRestServiceBinding;

@Path("/tilegrid")
@ViewerRestServiceBinding
public class TileGridResource {

    public static final String TAG_SEPARATOR_REGEX = "\\$";
    public static final String TAG_SEPARATOR = "$";

    @Context
    private HttpServletRequest servletRequest;
    
    @SuppressWarnings("unchecked")
    @GET
    @Path("/{language}/{size}/{priorityPlaces}/{tags}/")
    @Produces({ MediaType.APPLICATION_JSON })
    public TileGrid getTileGrid(@PathParam("language") String language, @PathParam("size") int gridSize,
            @PathParam("priorityPlaces") int priorityPlaces, @PathParam("tags") String tagString) throws DAOException {
        List items = DataManager.getInstance().getDao().getAllCMSMediaItems();
        Collections.sort(items, new Comparator<CMSMediaItem>() {
            @Override
            public int compare(CMSMediaItem item1, CMSMediaItem item2) {
                return Integer.compare(item1.getDisplayOrder(), item2.getDisplayOrder());
            }
        });
        String[] tags = new String[0];
        if (!tagString.equals("-")) {
            tags = tagString.split(TAG_SEPARATOR_REGEX);
        }

        TileGrid grid = new TileGridBuilder(servletRequest).language(language).size(gridSize).reserveForHighPriority(priorityPlaces).tags(tags).build(items);
        return grid;
    }
}
