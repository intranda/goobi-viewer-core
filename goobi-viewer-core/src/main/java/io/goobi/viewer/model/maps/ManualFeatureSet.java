package io.goobi.viewer.model.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

import io.goobi.viewer.exceptions.IndexUnreachableException;
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

}
