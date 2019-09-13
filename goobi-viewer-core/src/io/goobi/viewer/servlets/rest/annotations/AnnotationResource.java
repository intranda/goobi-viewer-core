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
package io.goobi.viewer.servlets.rest.annotations;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.IAnnotation;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.PersistentAnnotation;

/**
 * @author florian
 *
 */
@Path("/annotations")
public class AnnotationResource {
    
    @Context
    private HttpServletRequest servletRequest;
    @Context
    private HttpServletResponse servletResponse;
    
    @GET
    @Path("/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public IAnnotation getAnnotation(@PathParam("id") Long id, @QueryParam("type") String type) throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {
    
        PersistentAnnotation data = DataManager.getInstance().getDao().getAnnotation(id);
        
        IAnnotation anno;
        if("OpenAnnotation".equalsIgnoreCase(type) || "oa".equalsIgnoreCase(type)) {
            anno = data.getAsOpenAnnotation();
        } else {            
            anno = data.getAsAnnotation();
        }
        
        return anno;
    }
    
    @GET
    @Path("/{type}/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    @CORSBinding
    public IAnnotation getOpenAnnotation(@PathParam("type") String type, @PathParam("id") Long id) throws URISyntaxException, DAOException, JsonParseException, JsonMappingException, IOException {
    
        PersistentAnnotation data = DataManager.getInstance().getDao().getAnnotation(id);
        
        IAnnotation anno;
        if("OpenAnnotation".equalsIgnoreCase(type) || "oa".equalsIgnoreCase(type)) {
            anno = data.getAsOpenAnnotation();
        } else {            
            anno = data.getAsAnnotation();
        }
        
        return anno;
    }

}
