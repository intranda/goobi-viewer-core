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

import org.json.JSONObject;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.PageType;

/**
 * A location on a map. Contains a long/lat points, a label and a link
 * 
 * @author florian
 *
 */
public class Location {

    private final IArea area;
    private final String label;
    private final URI uri;
    
    public Location(IArea area, String label, URI uri) {
        this.area = area;
        this.label = label;
        this.uri = uri;
    }
    
    public static URI getRecordURI(String pi, PageType pageType) {
        String prettyId = "";
        switch(pageType) {
            case viewMetadata:
                prettyId = "metadata1";
                break;
            case viewToc:
                prettyId = "toc1";
                break;
            case viewObject:
            case viewImage:
            default:
                prettyId = "image1";
        }
        
        URL mappedUrl =
                PrettyContext.getCurrentInstance().getConfig().getMappingById(prettyId).getPatternParser().getMappedURL(pi);
        return URI.create(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + mappedUrl.toString());
    }
    
    /**
     * @return the area
     */
    public IArea getArea() {
        return area;
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
    
    public String getGeoJson() {
        JSONObject feature = new JSONObject();
        JSONObject geometry = new JSONObject(area.getGeoJson());
        JSONObject properties = new JSONObject();
        properties.put("title", getLabel());
        properties.put("link", getLink());
        feature.put("properties", properties);
        feature.put("geometry", geometry);
        feature.put("type", "Feature");
        return feature.toString();
    }
    
}
