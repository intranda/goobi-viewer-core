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
package io.goobi.viewer.api.rest.v1.records.media;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.StringUtils;

import de.unigoettingen.sub.commons.contentlib.servlet.rest.CORSBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ContentServerBinding;
import de.unigoettingen.sub.commons.contentlib.servlet.rest.ImageResource;

/**
 * @author florian
 *
 */
@Path("/records/{pi}/media/images/{filename}")
@ContentServerBinding
@CORSBinding
public class ViewerImageResource extends ImageResource {

    /**
     * @param request
     * @param directory
     * @param filename
     */
    public ViewerImageResource(@Context HttpServletRequest request, @PathParam("pi") String pi, @PathParam("filename") String filename) {
        super(request, pi, filename);
        request.setAttribute("pi", pi);
        request.setAttribute("filename", filename);
        
        String info = request.getPathInfo();
        String basePath = "/records/{pi}/media/images/{filename}".replace("{pi}", pi).replace("{filename}", filename);
        info = info.replace(basePath, "");
        List<String> parts = Arrays.stream(info.split("/")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        if(parts.size() == 4) {
            //image request
            request.setAttribute("region", parts.get(0));
            request.setAttribute("size", parts.get(1));
            request.setAttribute("rotation", parts.get(2));
            request.setAttribute("format", parts.get(3));
        }
    }

}
