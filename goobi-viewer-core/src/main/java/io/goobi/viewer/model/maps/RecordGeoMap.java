package io.goobi.viewer.model.maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.annotation.wa.TypedResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.GeoCoordinateConverter;
import io.goobi.viewer.controller.model.FeatureSetConfiguration;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.ContentBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent.ContentType;
import io.goobi.viewer.model.metadata.MetadataContainer;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrTools;

/**
 * Contains data to create a geomap for a record containing complex metadata (metadata documents) with geo coordinates
 * @author florian
 *
 */
public class RecordGeoMap {

    private final StructElement mainStruct;
    private final List<MetadataContainer> relatedDocuments;
    private final GeoMap geoMap;
    private final IDAO dao;
    private final List<FeatureSetConfiguration> featureSetConfigs;
    
    public RecordGeoMap(StructElement struct, List<MetadataContainer> relatedDocuments) throws DAOException {
        this(struct, relatedDocuments, DataManager.getInstance().getDao(), DataManager.getInstance().getConfiguration().getRecordGeomapFeatureSetConfigs());
    }

    
    public RecordGeoMap(StructElement struct, List<MetadataContainer> relatedDocuments, IDAO dao, List<FeatureSetConfiguration> featureSetConfigs) throws DAOException {
        this.dao = dao;
        this.mainStruct = struct;
        this.relatedDocuments = new ArrayList<>(relatedDocuments);
        this.featureSetConfigs = featureSetConfigs;
        this.geoMap = createMap();
    }
    
    private GeoMap createMap() throws DAOException {
        GeoMap geoMap = new GeoMap();
        
        this.createDocStructFeatureSet(geoMap, mainStruct);
        this.featureSetConfigs.stream().filter(config -> "metadata".equals(config.getType())).forEach(config -> createMetadataFeatureSet(geoMap, mainStruct, config));
        this.featureSetConfigs.stream().filter(config -> "relation".equals(config.getType())).forEach(config -> createRelatedDocumentFeatureSet(geoMap, relatedDocuments, config));

        return geoMap;
    }


    private void createRelatedDocumentFeatureSet(GeoMap geoMap, List<MetadataContainer> docs, FeatureSetConfiguration config) {
        ManualFeatureSet featureSet = new ManualFeatureSet();
        featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations(config.getName(), true)));
        featureSet.setMarker(config.getMarker());
        geoMap.addFeatureSet(featureSet);        

        GeoCoordinateConverter converter = new GeoCoordinateConverter(config.getLabelConfig());
        featureSet.setFeatures(docs.stream()
                .distinct()
                .filter(d -> StringUtils.isNotBlank(d.getFirstValue("NORM_COORDS_GEOJSON")))
                .map(doc -> converter.getGeojsonPoints(doc, "NORM_COORDS_GEOJSON", "MD_VALUE"))
                .flatMap(Collection::stream)
                .map(GeoMapFeature::getJsonObject)
                .map(Object::toString)
                .collect(Collectors.toList()));
  
    }


    private void createMetadataFeatureSet(GeoMap geoMap, StructElement mainStruct, FeatureSetConfiguration config) {
        SolrFeatureSet featureSet = new SolrFeatureSet();
        featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations(config.getName(), false)));
        featureSet.setSolrQuery(String.format("+DOCTYPE:METADATA +LABEL:(%s) +PI_TOPSTRUCT:%s", config.getQuery(), mainStruct.getPi()));
        featureSet.setMarkerTitleField(config.getLabelConfig());
        featureSet.setAggregateResults(true);
        featureSet.setMarker(config.getMarker());
        geoMap.addFeatureSet(featureSet);
    }
    
    private void createDocStructFeatureSet(GeoMap geoMap, StructElement docStruct) {
            SolrFeatureSet featureSet = new SolrFeatureSet();
            featureSet.setName(createLabel(docStruct));
            featureSet.setMarkerTitleField("MD_TITLE");
            featureSet.setSolrQuery(String.format("+PI_TOPSTRUCT:%s +DOCTYPE:DOCSTRCT", docStruct.getPi()));
            featureSet.setAggregateResults(false);
            featureSet.setMarker(DataManager.getInstance().getConfiguration().getRecordGeomapMarker(""));
            geoMap.addFeatureSet(featureSet);
        }
    
    private void createAnnotationFeatureSet(GeoMap geoMap, String pi, FeatureSetConfiguration config, GeoCoordinateConverter converter) throws DAOException {
        ManualFeatureSet featureSet = new ManualFeatureSet();
        featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations("annotations", true)));
        featureSet.setMarker(config.getMarker());
        geoMap.addFeatureSet(featureSet);
        
        List<String> features = new ArrayList<>();
        List<DisplayUserGeneratedContent> annos = this.dao
                .getAnnotationsForWork(pi)
                .stream()
                .filter(a -> PublicationStatus.PUBLISHED.equals(a.getPublicationStatus()))
                .filter(a -> StringUtils.isNotBlank(a.getBody()))
                .map(a -> new DisplayUserGeneratedContent(a))
                .filter(a -> ContentType.GEOLOCATION.equals(a.getType()))
                .filter(a -> ContentBean.isAccessible(a, BeanUtils.getRequest()))
                .collect(Collectors.toList());
        for (DisplayUserGeneratedContent anno : annos) {
            if (anno.getAnnotationBody() instanceof TypedResource) {
                GeoMapFeature feature = new GeoMapFeature(((TypedResource) anno.getAnnotationBody()).asJson());
                 features.add(feature.getJsonObject().toString());
            }
        }
        featureSet.setFeatures(features);
    }
    
    private TranslatedText createLabel(StructElement docStruct) {
        TranslatedText label = new TranslatedText(IPolyglott.getLocalesStatic());
        for (Locale language : label.getLocales()) {
            label.setValue(docStruct.getLabel(language.getLanguage()), language);
        }
        return label;
    }

    public GeoMap getGeoMap() {
        return geoMap;
    };
}
