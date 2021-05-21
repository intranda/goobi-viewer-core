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
package io.goobi.viewer.model.search;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author florian
 *
 */
public class GeoFacetItem {
    
    private static final Logger logger = LoggerFactory.getLogger(GeoFacetItem.class);
    
    private GeoCoordinateFeature feature = null;
    private final String solrField;

    public GeoFacetItem(String solrField) {
        this.solrField = solrField;
    }

    public GeoFacetItem(GeoFacetItem orig) {
        this.solrField = orig.solrField;
        if(StringUtils.isBlank(orig.getFeature())) {
            this.feature = null;
        } else {            
            this.feature = new GeoCoordinateFeature(orig.getFeature());
        }
    }
    
    public String getFeature() {
        return feature == null ? "" : feature.getFeatureAsString();
    }

    /**
     * Sets {@link #currentGeoFacettingFeature} and sets the matching search string to the WKT_COORDS facet if available
     * 
     * @param feature
     */
    public void setFeature(String feature) {
            try {
                if(StringUtils.isNotBlank(feature)) {            
                    this.feature = new GeoCoordinateFeature(feature);
                } else {
                    this.feature = null;
                }
            } catch(JSONException e) {
                logger.error("Faild to parse JSON object {}", feature);
            }
    }
    
    public void setFeatureFromContext() {
        Map<String, String> params = FacesContext.getCurrentInstance().
                getExternalContext().getRequestParameterMap();
        
        String feature = params.get("feature");
        setFeature(feature);
    }
    
    public String getFacetQuery() {
        if(isActive() && feature != null) {
            return solrField + ":" + getValue();
        } else {
            return "";
        }
    }
    
    public String getValue() {
        return "\"" + feature.getSearchString() + "\"";
    }
    
    public boolean isActive() {
        return StringUtils.isNotBlank(solrField);
    }

    /**
     * @return
     */
    public Object getEscapedFacetQuery() {
        if(isActive() && feature != null) {
            return solrField + ":" + FacetItem.getEscapedValue(getValue());
        } else {
            return "";
        }
    }

    /**
     * @return the solrField
     */
    public String getSolrField() {
        return solrField;
    }
    
    public void clear() {
        this.feature = null;
    }

    /**
     * Create a polygon feature from the given vertices
     * 
     * @param vertices
     */
    public void setVertices(double[][] vertices) {
        this.feature = new GeoCoordinateFeature(vertices);
    }
}
