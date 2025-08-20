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
package io.goobi.viewer.model.maps;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.ocpsoft.pretty.PrettyContext;
import com.ocpsoft.pretty.faces.url.URL;

import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.modules.interfaces.IURLBuilder;

/**
 * A location on a map. Contains a long/lat points, a label and a link
 *
 * @author florian
 *
 */
public class Location implements Serializable {

    private static final long serialVersionUID = 1628708143715554466L;

    private static final Logger logger = LogManager.getLogger(Location.class);

    private final IArea area;
    private final String label;
    private final URI uri;

    /**
     *
     * @param area
     * @param label
     * @param uri
     */
    public Location(IArea area, String label, URI uri) {
        this.area = area;
        this.label = label;
        this.uri = uri;
    }

    /**
     *
     * @param pi
     * @param pageType
     * @param urlBuilder If not null, the URL will be build using the URL builder, otherwise manually
     * @return {@link URI}
     */
    public static URI getRecordURI(String pi, PageType pageType, IURLBuilder urlBuilder) {
        if (urlBuilder != null) {
            try {
                return URI.create(BeanUtils.getServletPathWithHostAsUrlFromJsfContext() + '/' + urlBuilder.buildPageUrl(pi, 1, null, pageType, true));
            } catch (IllegalArgumentException e) {
                logger.error(e.getMessage());
            }
        }

        String prettyId = "";
        switch (pageType) {
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

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            Location other = (Location) obj;
            return Strings.CS.equals(this.label, other.label)
                    && Objects.equals(this.uri, other.uri)
                    && Objects.equals(this.area, other.area);
        }

        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.label == null ? 0 : this.label.hashCode();
    }
}
