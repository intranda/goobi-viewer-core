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
package io.goobi.viewer.model.maps.features;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.model.maps.GeoMapFeature;
import io.goobi.viewer.model.maps.GeoMapFeatureItem;
import io.goobi.viewer.model.maps.coordinates.CoordinateReaderProvider;
import io.goobi.viewer.model.metadata.ComplexMetadata;
import io.goobi.viewer.model.metadata.ComplexMetadataContainer;
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
    private final CoordinateReaderProvider coordinateReaderProvider;

    public FeatureGenerator(List<String> coordinateFields, LabelCreator featureTitleCreator, LabelCreator entityTitleCreator) {
        super();
        this.coordinateFields = coordinateFields;
        this.featureTitleCreator = featureTitleCreator;
        this.entityTitleCreator = entityTitleCreator;
        this.coordinateReaderProvider = new CoordinateReaderProvider();
    }

    public Collection<GeoMapFeature> getFeatures(MetadataDocument document) {

        Collection<GeoMapFeature> features = new ArrayList<>();
        if (document.getMetadataGroups().isEmpty()) {
            features = getFeatures(document.getMainDocMetadata());
        } else {
            features = getFeatures(document.getMetadataGroups(), document.getMainDocMetadata());
        }

        Map<GeoMapFeature, List<GeoMapFeature>> featureMap = features
                .stream()
                .collect(Collectors.groupingBy(Function.identity()));

        features = featureMap.entrySet()
                .stream()
                .map(e -> {
                    GeoMapFeature f = e.getKey();
                    f.setCount(e.getValue().size());
                    f.setItems(e.getValue().stream().flatMap(f1 -> f1.getItems().stream()).collect(Collectors.toList()));
                    return f;
                })
                .collect(Collectors.toList());

        return features;
    }

    private Collection<GeoMapFeature> getFeatures(ComplexMetadataContainer metadataGroups, MetadataContainer topDocument) {

        Collection<GeoMapFeature> features = new ArrayList<>();
        for (ComplexMetadata group : metadataGroups.getAllGroups()) {
            MetadataContainer container = new MetadataContainer(group.getMetadata());
            List<String> coordinates = getCoordinates(group, coordinateFields);
            Collection<GeoMapFeature> f = getFeatures(container, topDocument, coordinates);
            features.addAll(f);
        }

        return features;
    }

    private List<String> getCoordinates(ComplexMetadata group, List<String> coordinateFields) {
        return coordinateFields.stream()
                .map(field -> group.getFirstValue(field))
                .filter(v -> v != null && !v.isEmpty())
                .map(v -> v.getValue().orElse(""))
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
        if (coordinates == null || coordinates.isEmpty()) {
            return Collections.emptyList();
        }

        IMetadataValue featureTitle = getTitle(metadata, topDocument, this.featureTitleCreator);
        IMetadataValue entityTitle = getTitle(metadata, topDocument, this.entityTitleCreator);
        URI link = createLink(metadata, topDocument);
        URI searchLink = new FeatureQueryGenerator().createSearchLink(metadata, getAppropriateTemplate(metadata), this.featureTitleCreator);

        return coordinates.stream().map(coords -> {
            GeoMapFeature feature = getFeature(coords);
            feature.setTitle(featureTitle);
            feature.addItem(new GeoMapFeatureItem(entityTitle, link != null ? link.toString() : ""));
            feature.setLink(searchLink.toString());
            return feature;
        }).toList();
    }

    protected IMetadataValue getTitle(MetadataContainer metadata, MetadataContainer topDocument, LabelCreator labelCreator) {
        IMetadataValue title = labelCreator.getValue(metadata, topDocument, getAppropriateTemplate(metadata));
        if (title.isEmpty() && topDocument != null) {
            title = labelCreator.getValue(topDocument, getAppropriateTemplate(topDocument));
        }
        return title;
    }

    private String getAppropriateTemplate(MetadataContainer metadata) {
        String docType = SolrTools.getBaseFieldName(metadata.getFirstValue(SolrConstants.DOCTYPE));
        String docStrct = SolrTools.getBaseFieldName(metadata.getFirstValue(SolrConstants.DOCSTRCT));
        String label = SolrTools.getBaseFieldName(metadata.getFirstValue(SolrConstants.LABEL));
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
    private List<GeoMapFeature> getFeatures(List<String> points) {
        List<GeoMapFeature> docFeatures = new ArrayList<>();
        for (String point : points) {
            try {
                Geometry geometry = this.coordinateReaderProvider.getReader(point).read(point);
                docFeatures.add(new GeoMapFeature(geometry));
            } catch (IllegalArgumentException e) {
                logger.error(e.toString());
            }
        }
        return docFeatures;
    }

    protected GeoMapFeature getFeature(String point) {
        Geometry geometry = this.coordinateReaderProvider.getReader(point).read(point);
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
