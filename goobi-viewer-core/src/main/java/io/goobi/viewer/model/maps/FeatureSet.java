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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_geomap_featureset")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "feature_source")
public abstract class FeatureSet implements Serializable {

    private static final float DEFAULT_FILL_OPACITY = 0.2f;
    private static final String DEFAULT_MARKER_COLOR = "#FF5F1F";
    private static final long serialVersionUID = -5349708948761030268L;
    protected static final String DEFAULT_MARKER_NAME = "default";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "featureset_id")
    private Long id;

    @Column(name = "name", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText name = new TranslatedText();

    @Column(name = "marker")
    private String marker = DEFAULT_MARKER_NAME;

    protected FeatureSet() {

    }

    protected FeatureSet(FeatureSet blueprint) {
        this.id = blueprint.id;
        this.name = new TranslatedText(blueprint.name);
        this.marker = blueprint.marker;
    }

    public abstract FeatureSet copy();

    public abstract String getFeaturesAsString() throws PresentationException;

    public abstract void updateFeatures();

    public abstract boolean hasFeatures();

    public abstract boolean isQueryResultSet();

    /**
     * @return the marker
     */
    public String getMarker() {
        return Optional.ofNullable(this.marker).orElse(DEFAULT_MARKER_NAME);
    }

    /**
     * @param marker the marker to set
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }

    public String getMarkerAsJSON() throws JsonProcessingException {
        if (StringUtils.isNotBlank(marker)) {
            GeoMapMarker m = DataManager.getInstance().getConfiguration().getGeoMapMarker(this.marker);
            if (m != null) {
                return m.toJSONString();
            }
        }
        return "{}";
    }

    public TranslatedText getName() {
        return name;
    }

    public void setName(TranslatedText name) {
        this.name = name;
    }

    public String getColor() {
        if (StringUtils.isNotBlank(marker)) {
            GeoMapMarker m = DataManager.getInstance().getConfiguration().getGeoMapMarker(this.marker);
            if (m != null) {
                return m.getMarkerColor();
            }
        }
        return DEFAULT_MARKER_COLOR;
    }

    public float getFillOpacity() {
        return DEFAULT_FILL_OPACITY;
    }

    public Long getId() {
        return id;
    }

    public abstract String getType();

}
