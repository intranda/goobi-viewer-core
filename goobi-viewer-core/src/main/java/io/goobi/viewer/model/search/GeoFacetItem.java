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
package io.goobi.viewer.model.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.SearchBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.search.FacetItem.FacetType;

/**
 * @author florian
 *
 */
public class GeoFacetItem implements IFacetItem {

    private static final Logger logger = LogManager.getLogger(GeoFacetItem.class);
    public static final GeoCoordinateFeature NO_AREA = new GeoCoordinateFeature(new double[0][2], "", "");

    private GeoCoordinateFeature feature = null;
    private String solrField;

    public GeoFacetItem(String solrField) {
        this.solrField = solrField;
    }

    public GeoFacetItem(GeoFacetItem orig) {
        this.solrField = orig.solrField;
        if (StringUtils.isBlank(orig.getFeature())) {
            this.feature = null;
        } else if (!orig.feature.hasVertices()) {
            this.feature = NO_AREA;
        } else {
            this.feature = new GeoCoordinateFeature(orig.getFeature(), orig.getSearchPredicate(), orig.getSearchAreaShape());
        }
    }

    public String getFeature() {
        return hasArea() ? feature.getFeatureAsString() : "";
    }

    public boolean hasFeature() {
        return feature != null;
    }

    public boolean hasArea() {
        return feature != null && feature.hasVertices();
    }

    public String getSearchPredicate() {
        if (this.feature != null) {
            return this.feature.getPredicate();
        }
        return "";
    }

    public String getSearchAreaShape() {
        if (this.feature != null) {
            return this.feature.getShape();
        }
        return "";
    }

    /**
     * Sets {@link #currentGeoFacettingFeature} and sets the matching search string to the WKT_COORDS facet if available
     *
     * @param feature
     */
    public void setFeature(String feature) {
        try {
            if (StringUtils.isNotBlank(feature)) {
                this.feature = new GeoCoordinateFeature(feature, getDefaultSearchPredicate(), GeoCoordinateFeature.SHAPE_POLYGON);
            } else {
                this.feature = NO_AREA;
            }
        } catch (JSONException e) {
            logger.error("Faild to parse JSON object {}", feature);
        }
    }

    @Deprecated
    public void setFeatureFromContext() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        String feat = params.get("feature");
        setFeature(feat);
    }

    public String getFacetQuery() {
        if (isActive() && feature != null && feature.hasVertices()) {
            return solrField + ":" + getValue();
        }
        return "";
    }

    public String getValue() {
        return "\"" + feature.getSearchString() + "\"";
    }

    public boolean isActive() {
        return StringUtils.isNotBlank(solrField);
    }

    /**
     * @return {@link String}
     */
    public String getEscapedFacetQuery() {
        if (isActive() && feature != null && feature.hasVertices()) {
            return solrField + ":" + FacetItem.getEscapedValue(getValue());
        }
        return "";
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
        if (vertices == null || vertices.length == 0) {
            this.feature = NO_AREA;
        } else {
            this.feature = new GeoCoordinateFeature(vertices, getDefaultSearchPredicate(), GeoCoordinateFeature.SHAPE_POLYGON);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getQueryEscapedLink()
     */
    @Override
    public String getQueryEscapedLink() {
        return getEscapedFacetQuery();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getEscapedLink()
     */
    @Override
    public String getEscapedLink() {
        return BeanUtils.escapeCriticalUrlChracters(getFacetQuery());
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getUrlEscapedLink()
     */
    @Override
    public String getUrlEscapedLink() {
        String ret = getEscapedLink();
        try {
            return URLEncoder.encode(ret, SearchBean.URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return ret;
        }
    }
    

    @Override
    public FacetType getType() {
        return FacetType.GEO;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getField()
     */
    @Override
    public String getField() {
        return getSolrField();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setField(java.lang.String)
     */
    @Override
    public void setField(String field) {
        this.solrField = field;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getFullValue()
     */
    @Override
    public String getFullValue() {
        return getValue();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setValue(java.lang.String)
     */
    @Override
    public void setValue(String value) {
        String searchPredicate = GeoCoordinateFeature.getPredicate(value);
        String searchShape = GeoCoordinateFeature.getShape(value);
        this.feature = new GeoCoordinateFeature(GeoCoordinateFeature.getGeoSearchPoints(value), searchPredicate, searchShape);
    }

    private String getDefaultSearchPredicate() {
        return DataManager.getInstance().getConfiguration().getGeoFacetFieldPredicate(solrField);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getValue2()
     */
    @Override
    public String getValue2() {
        return "";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setValue2(java.lang.String)
     */
    @Override
    public void setValue2(String value2) {
        //NOOP
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getLink()
     */
    @Override
    public String getLink() {
        return getFacetQuery();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setLink(java.lang.String)
     */
    @Override
    public void setLink(String link) {
        int separatorIndex = link.indexOf(":");
        if (separatorIndex > 0) {
            this.solrField = link.substring(0, separatorIndex);
            setValue(link.substring(separatorIndex + 1));
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getLabel()
     */
    @Override
    public String getLabel() {
        return "";
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setLabel(java.lang.String)
     */
    @Override
    public IFacetItem setLabel(String label) {
        //NOOP
        return this;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getTranslatedLabel()
     */
    @Override
    public String getTranslatedLabel() {
        return getLabel();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setTranslatedLabel(java.lang.String)
     */
    @Override
    public void setTranslatedLabel(String translatedLabel) {
        //NOOP
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#getCount()
     */
    @Override
    public long getCount() {
        return 0;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    @Override
    public IFacetItem setGroup(boolean group) {
        return this;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#setCount(long)
     */
    @Override
    public IFacetItem setCount(long count) {
        //NOOP
        return this;
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.search.IFacetItem#isHierarchial()
     */
    @Override
    public boolean isHierarchial() {
        return false;
    }
}
