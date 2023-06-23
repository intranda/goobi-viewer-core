package io.goobi.viewer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.SimpleMetadataValue;
import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.model.metadata.Metadata;

public class GeoCoordinateConverterTest extends AbstractTest {

    @Before
    public void setup() throws Exception {
        super.setUp();
    }
    
    @Test
    public void test_createTitle() {
        Metadata md = DataManager.getInstance().getConfiguration().getRecordGeomapFeatureConfiguration("MD_BIOGRAPHY_JOURNEY");
        Map<String, List<IMetadataValue>> mdMap = Map.of(
                "MD_LOCATION", List.of(new SimpleMetadataValue("Mexico"))
                );
                
        IMetadataValue value = GeoCoordinateConverter.createTitle(md, mdMap);
        assertEquals("Journey to Mexico", value.getValueOrFallback(Locale.ENGLISH));
    }

}
