package io.goobi.viewer.model.maps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.maps.GeoMap.GeoMapType;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

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
    
}
