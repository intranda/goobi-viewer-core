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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import de.intranda.api.annotation.wa.TypedResource;
import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.controller.model.FeatureSetConfiguration;
import io.goobi.viewer.controller.model.LabeledValue;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.ContentBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent.ContentType;
import io.goobi.viewer.model.maps.coordinates.CoordinateReaderProvider;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataBuilder;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Contains data to create a geomap for a record containing complex metadata (metadata documents) with geo coordinates
 * 
 * @author florian
 *
 */
public class RecordGeoMap {

    private static final Logger logger = LogManager.getLogger(RecordGeoMap.class);

    private final StructElement mainStruct;
    private final List<MetadataContainer> relatedDocuments;
    private final GeoMap geoMap;
    private final IDAO dao;
    private final List<FeatureSetConfiguration> featureSetConfigs;

    /**
     * Create a new geomap with features from the given StructElement and related documents.
     * 
     * @param struct
     * @param relatedDocuments
     */
    public RecordGeoMap(StructElement struct, List<MetadataContainer> relatedDocuments) throws DAOException {
        this(struct, relatedDocuments, DataManager.getInstance().getDao(),
                DataManager.getInstance().getConfiguration().getRecordGeomapFeatureSetConfigs(struct.getDocStructType()));
    }

    /**
     * Create a new geomap with features from the given StructElement and related documents.
     * 
     * @param struct
     * @param relatedDocuments
     * @param dao
     * @param featureSetConfigs
     */
    public RecordGeoMap(StructElement struct, List<MetadataContainer> relatedDocuments, IDAO dao, List<FeatureSetConfiguration> featureSetConfigs) {
        this.dao = dao;
        this.mainStruct = struct;
        this.relatedDocuments = new ArrayList<>(relatedDocuments);
        this.featureSetConfigs = featureSetConfigs;
        this.geoMap = createMap();
    }

    /**
     * empty geomap without features
     */
    public RecordGeoMap() {
        this.dao = null;
        this.mainStruct = null;
        this.relatedDocuments = new ArrayList<>();
        this.featureSetConfigs = new ArrayList<>();
        this.geoMap = new GeoMap();
    }

    private GeoMap createMap() {
        GeoMap map = new GeoMap();

        //        RecordGeoMap.createDocStructFeatureSet(map, mainStruct);
        this.featureSetConfigs.stream()
                .filter(config -> "docStruct".equals(config.getType()))
                .forEach(config -> createDocStructFeatureSet(map, mainStruct, config));
        this.featureSetConfigs.stream()
                .filter(config -> "docStructs".equals(config.getType()))
                .forEach(config -> createAllDocStructFeatureSet(map, mainStruct, config));
        this.featureSetConfigs.stream()
                .filter(config -> "relation".equals(config.getType()))
                .forEach(config -> createRelatedDocumentFeatureSet(map, relatedDocuments, config));
        this.featureSetConfigs.stream()
                .filter(config -> "metadata".equals(config.getType()))
                .forEach(config -> createMetadataFeatureSet(map, mainStruct, config));
        this.featureSetConfigs.stream()
                .filter(config -> "annotations".equals(config.getType()))
                .forEach(config -> createAnnotationFeatureSet(map, mainStruct.getPi(), config));

        return map;
    }

    private static void createRelatedDocumentFeatureSet(GeoMap geoMap, List<MetadataContainer> docs, FeatureSetConfiguration config) {
        ManualFeatureSet featureSet = new ManualFeatureSet();
        featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations(config.getName(), true)));
        featureSet.setMarker(config.getMarker());
        geoMap.addFeatureSet(featureSet);

        //TODO:create related document features

    }

    private static boolean matchesQuery(MetadataContainer container, String query) {
        if (StringUtils.isBlank(query)) {
            return true;
        } else if (query.contains(":")) {
            String field = query.substring(0, query.indexOf(":"));
            String value = query.substring(query.indexOf(":") + 1);
            if (StringUtils.isNoneBlank(field, value)) {
                String fieldValue = container.getFirstValue(field);
                return fieldValue.equalsIgnoreCase(value);
            }
            return false;
        } else {
            return query.equalsIgnoreCase(container.getLabel().getValue().orElse(""));
        }
    }

    private static void createMetadataFeatureSet(GeoMap geoMap, StructElement mainStruct, FeatureSetConfiguration config) {
        SolrFeatureSet featureSet = new SolrFeatureSet(false);
        featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations(config.getName(), true)));
        featureSet.setSolrQuery(String.format("+DOCTYPE:METADATA +LABEL:(%s) +PI_TOPSTRUCT:%s", config.getQuery(), mainStruct.getPi()));
        featureSet.setMarkerTitleField(config.getLabelConfig());
        featureSet.setSearchScope(SolrSearchScope.METADATA);
        featureSet.setMarker(config.getMarker());
        geoMap.addFeatureSet(featureSet);
    }

    private static void createDocStructFeatureSet(GeoMap geoMap, StructElement docStruct, FeatureSetConfiguration config) {
        if (matchesQuery(MetadataContainer.createMetadataEntity(docStruct), config.getQuery())) {
            ManualFeatureSet featureSet = new ManualFeatureSet();
            featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations(config.getName(), true)));
            featureSet.setMarker(config.getMarker());

            List<String> coordinateFields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();
            List<String> coordinateValues = coordinateFields.stream()
                    .flatMap(field -> docStruct.getMetadataValues(field).stream())
                    .filter(StringUtils::isNotBlank)
                    .toList();
            List<GeoMapFeature> features = new GeoCoordinateConverter().getFeatures(coordinateValues);

            Metadata labelConfig =
                    DataManager.getInstance().getConfiguration().getGeoMapFeatureConfiguration(config.getLabelConfig(), docStruct.getDocStructType());
            IMetadataValue label = new MetadataBuilder(docStruct).build(labelConfig);
            features.forEach(f -> f.setTitle(label));
            featureSet.setFeatures(features.stream().map(GeoMapFeature::getJsonObject).map(JSONObject::toString).toList());

            geoMap.addFeatureSet(featureSet);
        }
    }

    private static void createAllDocStructFeatureSet(GeoMap geoMap, StructElement docStruct, FeatureSetConfiguration config) {
        if (matchesQuery(MetadataContainer.createMetadataEntity(docStruct), config.getQuery())) {
            SolrFeatureSet featureSet = new SolrFeatureSet(false);
            featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations(config.getName(), true)));
            featureSet.setMarker(config.getMarker());
            featureSet.setMarkerTitleField(config.getLabelConfig());
            featureSet.setSolrQuery(String.format("+PI_TOPSTRUCT:%s +DOCTYPE:DOCSTRCT", docStruct.getPi()));
            featureSet.setSearchScope(SolrSearchScope.RECORDS);
            geoMap.addFeatureSet(featureSet);
        }
    }

    private void createAnnotationFeatureSet(GeoMap geoMap, String pi, FeatureSetConfiguration config) {
        ManualFeatureSet featureSet = new ManualFeatureSet();
        featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations("annotations", true)));
        featureSet.setMarker(config.getMarker());
        geoMap.addFeatureSet(featureSet);
        try {
            List<String> features = new ArrayList<>();
            List<DisplayUserGeneratedContent> annos = dao
                    .getAnnotationsForWork(pi)
                    .stream()
                    .filter(a -> PublicationStatus.PUBLISHED.equals(a.getPublicationStatus()))
                    .filter(a -> StringUtils.isNotBlank(a.getBody()))
                    .map(DisplayUserGeneratedContent::new)
                    .filter(a -> ContentType.GEOLOCATION.equals(a.getType()))
                    .filter(a -> ContentBean.isAccessible(a, BeanUtils.getRequest()))
                    .toList();
            CoordinateReaderProvider coordinateReaderProvider = new CoordinateReaderProvider();
            for (DisplayUserGeneratedContent anno : annos) {
                if (anno.getAnnotationBody() instanceof TypedResource tr) {
                    GeoMapFeature feature = new GeoMapFeature(coordinateReaderProvider.getReader(tr.toString()).read(tr.toString()));
                    feature.setPageNo(anno.getPage());
                    feature.setDocumentId(anno.getId().toString());
                    features.add(feature.getJsonObject().toString());
                }
            }
            featureSet.setFeatures(features);
        } catch (DAOException e) {
            logger.error("Error loading anntations for geomap of record {}: {}", pi, e.toString());
        }
    }

    private static TranslatedText createLabel(StructElement docStruct) {
        TranslatedText label = new TranslatedText(IPolyglott.getLocalesStatic());
        for (Locale language : label.getLocales()) {
            label.setValue(docStruct.getLabel(language.getLanguage()), language);
        }
        return label;
    }

    public String getGeomapFiltersAsJson() {
        Locale locale = BeanUtils.getLocale();
        Map<String, List<LabeledValue>> map = new HashMap<>();
        for (FeatureSetConfiguration config : featureSetConfigs) {
            map.put(config.getName(), config.getFilters());
        }
        Map<String, List<LabeledValue>> translatedMap = new HashMap<>();
        for (Entry<String, List<LabeledValue>> entry : map.entrySet()) {
            List<LabeledValue> translatedValues = entry.getValue()
                    .stream()
                    .map(v -> new LabeledValue(v.getValue(), ViewerResourceBundle.getTranslation(v.getLabel(), locale), v.getStyleClass()))
                    .collect(Collectors.toList());
            translatedMap.put(entry.getKey(), translatedValues);

        }
        return new JSONObject(translatedMap).toString();
    }

    public GeoMap getGeoMap() {
        return geoMap;
    }
}
