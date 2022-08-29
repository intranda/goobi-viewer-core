package io.goobi.viewer.model.metadata;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.viewer.StructElement;

public class MetadataParameterTest {

    @Test
    public void testReadMetadata() throws IndexUnreachableException, PresentationException {
        MetadataParameter param = new MetadataParameter();
        param.setKey("TEST_FIELD");
        param.setType(MetadataParameterType.FIELD);
        MetadataParameter param2 = new MetadataParameter();
        param2.setKey("TEST_FIELD_2");
        param2.setType(MetadataParameterType.FIELD);
        param2.setPrefix(" ");
        
        StructElement ele = new StructElement();
        ele.setMetadataFields(Map.of("TEST_FIELD", Arrays.asList("foo", "boo"), "TEST_FIELD_2", Collections.singletonList("bar")));
        
        Metadata md = new Metadata("", "", Arrays.asList(param, param2));
        md.populate(ele, "1234", null, null);
        assertEquals("foo", md.getValues().get(0).getComboValueShort(0));
        assertEquals(" bar", md.getValues().get(0).getComboValueShort(1));
        assertEquals("boo", md.getValues().get(1).getComboValueShort(0));
        assertEquals("foo bar", md.getValues().get(0).getCombinedValue());
        assertEquals("foo bar, boo", md.getCombinedValue(", "));
    }

}
