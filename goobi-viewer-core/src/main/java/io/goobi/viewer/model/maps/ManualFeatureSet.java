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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

import io.goobi.viewer.exceptions.PresentationException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Transient;

@Entity
@DiscriminatorValue("manual")
public class ManualFeatureSet extends FeatureSet {

    private static final long serialVersionUID = 7602548008217909956L;

    private static final Logger logger = LogManager.getLogger(ManualFeatureSet.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cms_geomap_features", joinColumns = @JoinColumn(name = "featureset_id"))
    @Column(name = "features", columnDefinition = "LONGTEXT")
    private List<String> features = new ArrayList<>();

    @Transient
    private String featuresAsString = null;

    public ManualFeatureSet() {
        super();
    }

    public ManualFeatureSet(ManualFeatureSet blueprint) {
        super(blueprint);
        this.features = blueprint.features;
    }

    @Override
    public FeatureSet copy() {
        return new ManualFeatureSet(this);
    }

    /**
     * @return the features
     */
    public List<String> getFeatures() {
        return features;
    }

    /**
     * @param features the features to set
     */
    public void setFeatures(List<String> features) {
        this.features = features;
        this.featuresAsString = null;
    }

    public void setFeaturesAsString(String features) {
        JSONArray array = new JSONArray(features);
        this.features = new ArrayList<>();
        this.featuresAsString = null;
        for (Object object : array) {
            this.features.add(object.toString());
        }
    }

    public String getFeaturesAsString() throws PresentationException {
        if (this.featuresAsString == null) {
            this.featuresAsString = "[" + this.features.stream().collect(Collectors.joining(",")) + "]";
        }
        return this.featuresAsString;
    }

    @Override
    public String getFeaturesAsJsonString() throws PresentationException {
        return "[" + this.features.stream()
                .map(string -> StringEscapeUtils.escapeJson(string))
                .collect(Collectors.joining(",")) + "]";
    }

    @Override
    public void updateFeatures() {
        this.featuresAsString = null;
    }

    @Override
    public boolean hasFeatures() {
        return !this.features.isEmpty();
    }

    @Override
    public boolean isQueryResultSet() {
        return false;
    }

    @Override
    public String getType() {
        return "MANUAL";
    }

    @Override
    public boolean isUseHeatmap() {
        return false;
    }

    @Override
    public void setUseHeatmap(boolean useHeatmap) {
        // Do nothing
    }
}
