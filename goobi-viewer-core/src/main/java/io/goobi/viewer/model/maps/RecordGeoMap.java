package io.goobi.viewer.model.maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.intranda.api.annotation.wa.TypedResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.ContentBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.PublicationStatus;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent;
import io.goobi.viewer.model.crowdsourcing.DisplayUserGeneratedContent.ContentType;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import io.goobi.viewer.model.viewer.StructElement;

/**
 * Contains data to create a geomap for a record containing complex metadata (metadata documents) with geo coordinates
 * @author florian
 *
 */
public class RecordGeoMap {

    private final StructElement mainStruct;
    private final List<String> metadataTypes;
    private final GeoMap geoMap;
    private final IDAO dao;
    
    public RecordGeoMap(StructElement struct, List<String> metadataTypes) throws DAOException {
        this(struct, metadataTypes, DataManager.getInstance().getDao());
    }

    
    public RecordGeoMap(StructElement struct, List<String> metadataTypes, IDAO dao) throws DAOException {
        this.dao = dao;
        this.mainStruct = struct;
        this.metadataTypes = new ArrayList<>(metadataTypes);
        this.geoMap = createMap();
    }
    
    private GeoMap createMap() throws DAOException {
        GeoMap geoMap = new GeoMap();
        
        createDocStructFeatureSet(geoMap, mainStruct);
        
        for (String md : metadataTypes) {
            createMetadataFeatureSet(geoMap, md, mainStruct.getPi());
        }
        
        createAnnotationFeatureSet(geoMap, mainStruct.getPi());
        
        return geoMap;
    }

    private void createMetadataFeatureSet(GeoMap geoMap, String md, String pi) {
        SolrFeatureSet featureSet = new SolrFeatureSet();
        featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations(md, false)));
        featureSet.setSolrQuery(String.format("+DOCTYPE:METADATA +LABEL:%s* +PI_TOPSTRUCT:%s", md, pi));
        featureSet.setAggregateResults(true);
        geoMap.addFeatureSet(featureSet);
    }
    
    private void createDocStructFeatureSet(GeoMap geoMap, StructElement docStruct) {
        SolrFeatureSet featureSet = new SolrFeatureSet();
        featureSet.setName(createLabel(docStruct));
        featureSet.setSolrQuery(String.format("+PI_TOPSTRUCT:%s +DOCTYPE:DOCSTRCT", docStruct.getPi()));
        featureSet.setAggregateResults(false);
        geoMap.addFeatureSet(featureSet);
    }
    
    private void createAnnotationFeatureSet(GeoMap geoMap, String pi) throws DAOException {
        ManualFeatureSet featureSet = new ManualFeatureSet();
        featureSet.setName(new TranslatedText(ViewerResourceBundle.getTranslations("annotations", true)));
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
