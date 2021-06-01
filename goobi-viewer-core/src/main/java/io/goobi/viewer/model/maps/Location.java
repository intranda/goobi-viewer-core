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
package io.goobi.viewer.model.maps;

import java.net.URI;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * A location on a map. Contains a long/lat points, a label and a link
 * 
 * @author florian
 *
 */
public class Location {

    private final double lng;
    private final double lat;
    private final String label;
    private final URI uri;
    
    public Location(double lng, double lat, String label, URI uri) {
        this.lng = lng;
        this.lat = lat;
        this.label = label;
        this.uri = uri;
    }
    
    public static URI getRecordURI(String pi) {
        String prettyId = "image1";
        
        URL mappedUrl =
                PrettyContext.getCurrentInstance().getConfig().getMappingById(prettyId).getPatternParser().getMappedURL(pi);
        return URI.create(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + mappedUrl.toString());
    }
    
    /**
     * @return the lat
     */
    public double getLat() {
        return lat;
    }
    
    /**
     * @return the lng
     */
    public double getLng() {
        return lng;
    }
    
    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * @return the uri
     */
    public URI getLink() {
        return uri;
    }
    
}
