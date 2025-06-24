package io.goobi.viewer.model.maps.features;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ocpsoft.pretty.faces.util.StringUtils;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.maps.GeoMapFeature;
import io.goobi.viewer.model.maps.coordinates.CoordinateReaderProvider;
import io.goobi.viewer.model.metadata.ComplexMetadata;
import io.goobi.viewer.model.metadata.ComplexMetadataContainer;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.servlets.IdentifierResolver;
import io.goobi.viewer.solr.SolrConstants;
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

    public Collection<GeoMapFeature> getFeatures(MetadataDocument document) {

        Collection<GeoMapFeature> mainDocFeatures = getFeatures(document.getMainDocMetadata());
        Collection<GeoMapFeature> metadataDocFeatures = getFeatures(document.getMetadataGroups(), document.getMainDocMetadata());

        @SuppressWarnings("unchecked")
        Collection<GeoMapFeature> features = CollectionUtils.union(mainDocFeatures, metadataDocFeatures);

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

    private Collection<GeoMapFeature> getFeatures(ComplexMetadataContainer metadataGroups, MetadataContainer topDocument) {
        List<ComplexMetadata> groups = metadataGroups.getAllGroups();
        List<String> coordinates =
                coordinateFields.stream()
                        .flatMap(field -> groups.stream().map(g -> g.getFirstValue(field)))
                        .filter(v -> v != null && !v.isEmpty())
                        .map(v -> v.getValue().orElse(""))
                        .toList();

        return metadataGroups.getAllGroups()
                .stream()
                .map(group -> getFeatures(new MetadataContainer(group.getMetadata()), topDocument, coordinates))
                .flatMap(Collection::stream)
                .toList();
    }

    private Collection<GeoMapFeature> getFeatures(MetadataContainer metadata) {
        List<String> coordinates =
                coordinateFields.stream()
                        .map(metadata::getValues)
                        .flatMap(List::stream)
                        .filter(v -> !v.isEmpty())
                        .toList();
        return getFeatures(metadata, null, coordinates);
    }

    protected Collection<GeoMapFeature> getFeatures(MetadataContainer metadata, MetadataContainer topDocument, List<String> coordinates) {
        IMetadataValue featureTitle = getTitle(metadata, topDocument, this.featureTitleCreator);
        IMetadataValue entityTitle = getTitle(metadata, topDocument, this.entityTitleCreator);
        URI link = createLink(metadata, topDocument);

        return coordinates.stream().map(coords -> {
            GeoMapFeature feature = getFeature(coords);
            feature.setTitle(featureTitle);
            feature.addEntity(new MetadataContainer(metadata.getFirstValue(SolrConstants.IDDOC), entityTitle, metadata.getMetadata()));
            if (link != null) {
                feature.setLink(link.toString());
            }
            return feature;
        }).toList();
    }

    protected IMetadataValue getTitle(MetadataContainer metadata, MetadataContainer topDocument, LabelCreator labelCreator) {
        IMetadataValue title = labelCreator.getValue(metadata, getAppropriateTemplate(metadata));
        if (title.isEmpty() && topDocument != null) {
            title = labelCreator.getValue(topDocument, getAppropriateTemplate(topDocument));
        }
        return title;
    }

    private String getAppropriateTemplate(MetadataContainer metadata) {
        String docType = metadata.getFirstValue(SolrConstants.DOCTYPE);
        String docStrct = metadata.getFirstValue(SolrConstants.DOCSTRCT);
        String label = metadata.getFirstValue(SolrConstants.LABEL);
        if (SolrConstants.DocType.DOCSTRCT.name().equals(docType) && StringUtils.isNotBlank(docStrct)) {
            return docStrct;
        } else if (SolrConstants.DocType.METADATA.name().equals(docType) && StringUtils.isNotBlank(label)) {
            return label;
        } else {
            return StringConstants.DEFAULT_NAME;
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
    private static List<GeoMapFeature> getFeatures(List<String> points) {
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

    protected static GeoMapFeature getFeature(String point) {
        Geometry geometry = CoordinateReaderProvider.getReader(point).read(point);
        return new GeoMapFeature(geometry);
    }

    private static URI createLink(MetadataContainer doc, MetadataContainer topDoc) {
        if (doc.containsField(SolrConstants.PI) || doc.containsField(SolrConstants.LOGID)) {
            return getLinkURI(doc);
        } else {
            //metadata document
            return getLinkURI(topDoc);
        }
    }

    private static URI getLinkURI(MetadataContainer doc) {
        URI link = UriBuilder.fromUri(DataManager.getInstance().getConfiguration().getViewerBaseUrl())
                .path(IdentifierResolver.constructUrl(doc, false))
                .build();
        return link;
    }

}
