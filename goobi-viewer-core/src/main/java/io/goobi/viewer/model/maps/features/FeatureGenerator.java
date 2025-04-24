package io.goobi.viewer.model.maps.features;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.maps.GeoMapFeature;
import io.goobi.viewer.model.maps.coordinates.CoordinateReaderProvider;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.servlets.IdentifierResolver;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.ws.rs.core.UriBuilder;
import mil.nga.sf.geojson.Geometry;

public class FeatureGenerator {

    private static final Logger logger = LogManager.getLogger(FeatureGenerator.class);

    protected static final String POINT_LAT_LNG_PATTERN = "([\\dE.-]+)[\\s/]*([\\dE.-]+)";
    protected static final String POLYGON_LAT_LNG_PATTERN = "POLYGON\\(\\(([\\dE.-]+[\\s/]*[\\dE.-]+[,\\s]*)+\\)\\)"; //NOSONAR

    private final List<String> coordinateFields;
    private final LabelCreator featureTitleCreator;
    private final LabelCreator entityTitleCreator;

    public FeatureGenerator(List<String> coordinateFields, LabelCreator featureTitleCreator, LabelCreator entityTitleCreator) {
        super();
        this.coordinateFields = coordinateFields;
        this.featureTitleCreator = featureTitleCreator;
        this.entityTitleCreator = entityTitleCreator;
    }

    public List<GeoMapFeature> getFeaturesFromSolrDocs(Map<SolrDocument, List<SolrDocument>> docs) {
        List<GeoMapFeature> features = new ArrayList<>();
        for (Entry<SolrDocument, List<SolrDocument>> entry : docs.entrySet()) {
            SolrDocument doc = entry.getKey();
            List<SolrDocument> children = entry.getValue();

            for (String field : this.coordinateFields) {
                features.addAll(getFeaturesFromDoc(doc, field));
                Map<String, List<SolrDocument>> metadataDocs =
                        children.stream().collect(Collectors.toMap(SolrTools::getReferenceId, List::of, ListUtils::union));
                for (List<SolrDocument> childDocs : metadataDocs.values()) {
                    Collection<GeoMapFeature> tempFeatures = getFeaturesFromDocs(doc, childDocs, field);
                    features.addAll(tempFeatures);
                }
            }
        }

        Map<GeoMapFeature, List<GeoMapFeature>> featureMap = features
                .stream()
                .collect(Collectors.groupingBy(Function.identity()));

        features = featureMap.entrySet()
                .stream()
                .map(e -> {
                    GeoMapFeature f = e.getKey();
                    f.setCount(e.getValue().size());
                    f.setEntities(e.getValue().stream().flatMap(f1 -> f1.getEntities().stream()).collect(Collectors.toList()));
                    return f;
                })
                .collect(Collectors.toList());

        return features;
    }

    private List<GeoMapFeature> getFeaturesFromDoc(SolrDocument doc, String coordinateField) {
        return getGeojsonPoints(doc, Collections.emptyList(), coordinateField).stream().toList();
    }

    private List<GeoMapFeature> getFeaturesFromDocs(SolrDocument doc, List<SolrDocument> metadataDocs, String coordinateField) {
        return getGeojsonPoints(doc, metadataDocs, coordinateField).stream().toList();
    }

    public Collection<GeoMapFeature> getGeojsonPoints(SolrDocument mainDocument, List<SolrDocument> metadataDocs, String metadataField) {
        List<String> points = new ArrayList<>();
        for (SolrDocument metadataDoc : metadataDocs) {
            List<String> mdPoints = new ArrayList<>();
            mdPoints.addAll(SolrTools.getMetadataValues(metadataDoc, metadataField));
            List<GeoMapFeature> docFeatures = getFeatures(points);

        }
    }

    /**
     * Collect all point coordinate in the given metadata field within the given solr document
     * 
     * @param doc the document containing the coordinates
     * @param metadataField The name of the solr field to search in
     * @return Collection<GeoMapFeature>
     */
    public Collection<GeoMapFeature> getGeojsonPoints(SolrDocument doc, String metadataField) {
        List<String> points = new ArrayList<>();
        points.addAll(SolrTools.getMetadataValues(doc, metadataField));
        List<GeoMapFeature> docFeatures = getFeatures(points);
        addEntityToFeatures(doc, Collections.emptyList(), docFeatures);
        docFeatures.forEach(feature -> setLink(feature, doc));
        docFeatures.forEach(f -> f.setDocumentId((String) doc.getFieldValue(SolrConstants.LOGID)));
        docFeatures.forEach(this::setLabels);

        return docFeatures;
    }

    private void setLabels(GeoMapFeature feature) {
        Optional.ofNullable(feature.getEntities())
                .filter(l -> !l.isEmpty())
                .map(l -> l.get(0))
                .map(container -> this.featureTitleCreator.getValue(container, getType(container)))
                .ifPresent(feature::setTitle);

        for (MetadataContainer entity : feature.getEntities()) {
            IMetadataValue value = this.entityTitleCreator.getValue(entity.getMetadata(), getType(entity));
            if (!value.isEmpty()) {
                entity.setLabel(value);
            }
        }

    }

    private String getType(MetadataContainer container) {
        if (container.containsField(SolrConstants.DOCSTRCT)) {
            return container.getFirstValue(SolrConstants.DOCSTRCT);
        } else if (container.containsField(SolrConstants.LABEL)) {
            return container.getFirstValue(SolrConstants.LABEL);
        } else {
            return "_DEFAULT";
        }

    }

    /**
     * Generate a list of {@link GeoMapFeature geoMapFeatures} from a list of metadata values which may represent geographic coordinates. The
     * coordinate strings may take one of three forms:
     * <ul>
     * <li>Point: 'x y'</li>
     * <li>Polygon: 'POLYGON((x1 y1, x2 y2,...))'</li>
     * <li>Geojon: a json object following the geojson format</li>
     * </ul>
     *
     * @param points A list of strings that represent two-dimensional coordinates or an array of such.
     * @return The coordinates in form of {@link GeoMapFeature geoMapFeatures}
     */
    public static List<GeoMapFeature> getFeatures(List<String> points) {
        List<GeoMapFeature> docFeatures = new ArrayList<>();
        for (String point : points) {
            try {
                Geometry geometry = CoordinateReaderProvider.getReader(point).read(point);
                docFeatures.add(new GeoMapFeature(geometry));
            } catch (IllegalArgumentException e) {
                logger.error(e.toString());
            }
        }
        return docFeatures;
    }

    private static void addEntityToFeatures(SolrDocument doc, List<SolrDocument> children, List<GeoMapFeature> docFeatures) {
        MetadataContainer entity = MetadataContainer.createMetadataEntity(doc, children, f -> true, f -> true);
        docFeatures.forEach(f -> f.addEntity(entity));
    }

    private static void setLink(GeoMapFeature feature, SolrDocument doc) {
        URI link = UriBuilder.fromUri(DataManager.getInstance().getConfiguration().getViewerBaseUrl())
                .path(IdentifierResolver.constructUrl(doc, false))
                .build();
        feature.setLink(link.toString());
    }

}
