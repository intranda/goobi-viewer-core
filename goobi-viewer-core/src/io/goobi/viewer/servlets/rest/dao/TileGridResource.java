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
package io.goobi.viewer.servlets.rest.dao;

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

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.UserBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.tilegrid.TileGrid;
import io.goobi.viewer.model.cms.tilegrid.TileGridBuilder;
import io.goobi.viewer.servlets.rest.ViewerRestServiceBinding;

/**
 * <p>TileGridResource class.</p>
 *
 */
@Path("/tilegrid")
@ViewerRestServiceBinding
public class TileGridResource {

    /** Constant <code>TAG_SEPARATOR_REGEX="\\$"</code> */
    public static final String TAG_SEPARATOR_REGEX = "\\$";
    /** Constant <code>TAG_SEPARATOR="$"</code> */
    public static final String TAG_SEPARATOR = "$";

    @Context
    private HttpServletRequest servletRequest;
    
    /**
     * <p>getTileGrid.</p>
     *
     * @param language a {@link java.lang.String} object.
     * @param gridSize a int.
     * @param priorityPlaces a int.
     * @param tagString a {@link java.lang.String} object.
     * @return a {@link io.goobi.viewer.model.cms.tilegrid.TileGrid} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @GET
    @Path("/{language}/{size}/{priorityPlaces}/{tags}/")
    @Produces({ MediaType.APPLICATION_JSON })
    @SuppressWarnings("unchecked")
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
