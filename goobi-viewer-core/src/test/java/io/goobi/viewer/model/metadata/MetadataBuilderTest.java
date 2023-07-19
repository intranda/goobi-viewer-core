package io.goobi.viewer.model.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.intranda.metadata.multilanguage.IMetadataValue;
import de.intranda.metadata.multilanguage.MultiLanguageMetadataValue;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;

class MetadataBuilderTest {

    @Test
    void test() {
        MultiLanguageMetadataValue value = new MultiLanguageMetadataValue(Map.of("en", "Gallery", "de", "Gallerie"));
        Map<String, List<IMetadataValue>> metadata = Map.of(
                "MD_ROLE", List.of(value)
                );
        
        MetadataBuilder builder = new MetadataBuilder(metadata);
        
        MetadataParameter param = new MetadataParameter();
        param.setKey("MD_ROLE");
        param.setPrefix(" (");
        param.setSuffix(")");
        param.setType(MetadataParameterType.FIELD);
        Metadata config = new Metadata("MD_ROLE", "{MD_ROLE}", List.of(param));
        IMetadataValue translation = builder.build(config);
        
        assertEquals(" (Gallery)", translation.getValue("en").orElse(""));
        assertEquals("Gallery", value.getValue("en").orElse(""));
    }

}
