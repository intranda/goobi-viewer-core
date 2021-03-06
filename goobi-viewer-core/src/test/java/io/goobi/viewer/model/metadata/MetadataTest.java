package io.goobi.viewer.model.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.managedbeans.NavigationHelper;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrConstants.MetadataGroupType;

public class MetadataTest extends AbstractTest {

    /**
     * @see Metadata#filterMetadata(List,Locale)
     * @verifies return language-specific version of a field
     */
    @Test
    public void filterMetadata_shouldReturnLanguagespecificVersionOfAField() throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata("", "MD_TITLE_LANG_EN", "", "foo"));
        metadataList.add(new Metadata("", "MD_TITLE", "", "bar"));
        List<Metadata> filteredList = Metadata.filterMetadata(metadataList, "en", null);
        Assert.assertEquals(1, filteredList.size());
        Assert.assertEquals("MD_TITLE_LANG_EN", filteredList.get(0).getLabel());
    }

    /**
     * @see Metadata#filterMetadata(List,Locale)
     * @verifies return generic version if no language specific version is found
     */
    @Test
    public void filterMetadata_shouldReturnGenericVersionIfNoLanguageSpecificVersionIsFound() throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata("", "MD_TITLE_LANG_DE", "", "foo"));
        metadataList.add(new Metadata("", "MD_TITLE", "", "bar"));
        List<Metadata> filteredList = Metadata.filterMetadata(metadataList, "en", null);
        Assert.assertEquals(1, filteredList.size());
        Assert.assertEquals("MD_TITLE", filteredList.get(0).getLabel());
    }

    /**
     * @see Metadata#filterMetadata(List,String)
     * @verifies preserve metadata field order
     */
    @Test
    public void filterMetadata_shouldPreserveMetadataFieldOrder() throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata("", "MD_TITLE_LANG_EN", "", "foo"));
        metadataList.add(new Metadata("", "MD_TITLE_LANG_DE", "", "foo"));
        metadataList.add(new Metadata("", SolrConstants.PI, "", "PPN123"));
        metadataList.add(new Metadata("", "MD_DESCRIPTION_LANG_EN", "", "foo"));
        List<Metadata> filteredList = Metadata.filterMetadata(metadataList, "en", null);
        Assert.assertEquals(3, filteredList.size());
        Assert.assertEquals("MD_TITLE_LANG_EN", filteredList.get(0).getLabel());
        Assert.assertEquals(SolrConstants.PI, filteredList.get(1).getLabel());
        Assert.assertEquals("MD_DESCRIPTION_LANG_EN", filteredList.get(2).getLabel());
    }

    /**
     * @see Metadata#filterMetadata(List,String,String)
     * @verifies filter by desired field name correctly
     */
    @Test
    public void filterMetadata_shouldFilterByDesiredFieldNameCorrectly() throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata("", "MD_TITLE_LANG_EN", "", "foo"));
        metadataList.add(new Metadata("", "MD_TITLE_LANG_DE", "", "foo"));
        metadataList.add(new Metadata("", SolrConstants.PI, "", "PPN123"));
        metadataList.add(new Metadata("", "MD_DESCRIPTION_LANG_EN", "", "foo"));
        List<Metadata> filteredList = Metadata.filterMetadata(metadataList, "en", "MD_DESCRIPTION");
        Assert.assertEquals(1, filteredList.size());
        Assert.assertEquals("MD_DESCRIPTION_LANG_EN", filteredList.get(0).getLabel());
    }

    /**
     * @see Metadata#buildHierarchicalValue(String,Locale,NavigationHelper)
     * @verifies build value correctly
     */
    @Test
    public void buildHierarchicalValue_shouldBuildValueCorrectly() throws Exception {
        {
            String value = Metadata.buildHierarchicalValue("DC", "a.b", null, "http://localhost:8080/");
            Assert.assertEquals(
                    "<a href=\"http://localhost:8080/browse/-/1/-/DC:a/\">a</a> > <a href=\"http://localhost:8080/browse/-/1/-/DC:a.b/\">a.b</a>",
                    value);
        }
        {
            // No root URL
            String value = Metadata.buildHierarchicalValue("DC", "a.b.c.d", null, null);
            Assert.assertEquals("a > a.b > a.b.c > a.b.c.d", value);
        }
    }

    /**
     * @see Metadata#isBlank()
     * @verifies return true if all paramValues are empty
     */
    @Test
    public void isBlank_shouldReturnTrueIfAllParamValuesAreEmpty() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", "");
        Assert.assertEquals(1, metadata.getValues().size());
        Assert.assertTrue(metadata.isBlank());
    }

    /**
     * @see Metadata#isBlank()
     * @verifies return false if at least one paramValue is not empty
     */
    @Test
    public void isBlank_shouldReturnFalseIfAtLeastOneParamValueIsNotEmpty() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", "val");
        Assert.assertEquals(1, metadata.getValues().size());
        Assert.assertFalse(metadata.isBlank());
    }

    /**
     * @see Metadata#setParamValue(int,int,List,String,String,Map,Locale)
     * @verifies add multivalued param values correctly
     */
    @Test
    public void setParamValue_shouldAddMultivaluedParamValuesCorrectly() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", null);
        String[] values = new String[] { "val1", "val2" };
        metadata.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setPrefix("pre_").setSuffix("_suf"));
        metadata.setParamValue(0, 0, Arrays.asList(values), "", null, null, null, null);
        Assert.assertEquals(1, metadata.getValues().size());
        Assert.assertEquals(1, metadata.getValues().get(0).getParamValues().size());
        Assert.assertEquals(2, metadata.getValues().get(0).getParamValues().get(0).size());
        Assert.assertEquals("val1", metadata.getValues().get(0).getParamValues().get(0).get(0));
        Assert.assertEquals("val2", metadata.getValues().get(0).getParamValues().get(0).get(1));
    }

    /**
     * @see Metadata#setParamValue(int,int,List,String,String,Map,String,Locale)
     * @verifies set group type correctly
     */
    @Test
    public void setParamValue_shouldSetGroupTypeCorrectly() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", null);
        String[] values = new String[] { "val1", "val2" };
        metadata.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD).setPrefix("pre_").setSuffix("_suf"));
        metadata.setParamValue(0, 0, Arrays.asList(values), "", null, null, MetadataGroupType.CORPORATION.name(), null);
        Assert.assertEquals(1, metadata.getValues().size());
        Assert.assertEquals(MetadataGroupType.CORPORATION.name(), metadata.getValues().get(0).getGroupTypeForUrl());
    }
}