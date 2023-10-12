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
        metadataList.add(new Metadata("", "MD_TITLE_LANG_DE", "", "föö"));
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
     * @see Metadata#buildHierarchicalValue(String,String,Locale,String)
     * @verifies add configured collection sort field
     */
    @Test
    public void buildHierarchicalValue_shouldAddConfiguredCollectionSortField() throws Exception {
        String value = Metadata.buildHierarchicalValue("DC", "collection1", null, "http://localhost:8080/");
        Assert.assertEquals("<a href=\"http://localhost:8080/browse/-/1/SORT_TITLE/DC:collection1/\">collection1</a>", value);
    }

    /**
     * @see Metadata#isBlank(String)
     * @verifies return true if all paramValues are empty
     */
    @Test
    public void isBlank_shouldReturnTrueIfAllParamValuesAreEmpty() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", "");
        Assert.assertEquals(1, metadata.getValues().size());
        Assert.assertTrue(metadata.isBlank(null));
    }

    /**
     * @see Metadata#isBlank(String)
     * @verifies return false if at least one paramValue is not empty
     */
    @Test
    public void isBlank_shouldReturnFalseIfAtLeastOneParamValueIsNotEmpty() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", "val");
        Assert.assertEquals(1, metadata.getValues().size());
        Assert.assertFalse(metadata.isBlank(null));
    }

    /**
     * @see Metadata#isBlank(String)
     * @verifies return true if all values have different ownerIddoc
     */
    @Test
    public void isBlank_shouldReturnTrueIfAllValuesHaveDifferentOwnerIddoc() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", null);
        metadata.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD));
        String[] values = new String[] { "val1", "val2" };
        metadata.setParamValue(0, 0, Arrays.asList(values), "", null, null, null, null);
        metadata.setParamValue(1, 0, Arrays.asList(values), "", null, null, null, null);
        Assert.assertEquals(2, metadata.getValues().size());
        metadata.getValues().get(0).setOwnerIddoc("123");
        metadata.getValues().get(1).setOwnerIddoc("456");

        Assert.assertTrue(metadata.isBlank("789"));
    }

    /**
     * @see Metadata#isBlank(String)
     * @verifies return true if at least one value has same ownerIddoc
     */
    @Test
    public void isBlank_shouldReturnTrueIfAtLeastOneValueHasSameOwnerIddoc() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", null);
        metadata.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD));
        String[] values = new String[] { "val1", "val2" };
        metadata.setParamValue(0, 0, Arrays.asList(values), "", null, null, null, null);
        metadata.setParamValue(1, 0, Arrays.asList(values), "", null, null, null, null);
        Assert.assertEquals(2, metadata.getValues().size());
        metadata.getValues().get(0).setOwnerIddoc("123");
        metadata.getValues().get(1).setOwnerIddoc("456");

        Assert.assertFalse(metadata.isBlank("456"));
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

    /**
     * @see Metadata#getValuesForOwner(String)
     * @verifies return all values if ownerIddoc null
     */
    @Test
    public void getValuesForOwner_shouldReturnAllValuesIfOwnerIddocNull() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", null);
        metadata.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD));
        String[] values = new String[] { "val1", "val2" };
        metadata.setParamValue(0, 0, Arrays.asList(values), "", null, null, null, null);
        metadata.setParamValue(1, 0, Arrays.asList(values), "", null, null, null, null);
        Assert.assertEquals(2, metadata.getValues().size());
        metadata.getValues().get(0).setOwnerIddoc("123");
        metadata.getValues().get(1).setOwnerIddoc("456");

        Assert.assertEquals(2, metadata.getValuesForOwner(null).size());
    }

    /**
     * @see Metadata#getValuesForOwner(String)
     * @verifies return only values for the given ownerIddoc
     */
    @Test
    public void getValuesForOwner_shouldReturnOnlyValuesForTheGivenOwnerIddoc() throws Exception {
        Metadata metadata = new Metadata("", "MD_FIELD", "", null);
        metadata.getParams().add(new MetadataParameter().setType(MetadataParameterType.FIELD));
        String[] values = new String[] { "val1", "val2" };
        metadata.setParamValue(0, 0, Arrays.asList(values), "", null, null, null, null);
        metadata.setParamValue(1, 0, Arrays.asList(values), "", null, null, null, null);
        Assert.assertEquals(2, metadata.getValues().size());
        metadata.getValues().get(0).setOwnerIddoc("123");
        metadata.getValues().get(1).setOwnerIddoc("456");

        List<MetadataValue> mdValues = metadata.getValuesForOwner("456");
        Assert.assertEquals(1, mdValues.size());
        Assert.assertEquals("456", mdValues.get(0).getOwnerIddoc());
    }

    /**
     * @see Metadata#getMasterValue()
     * @verifies return placeholders for every parameter for group metadata if masterValue empty
     */
    @Test
    public void getMasterValue_shouldReturnPlaceholdersForEveryParameterForGroupMetadataIfMasterValueEmpty() throws Exception {
        List<MetadataParameter> params = new ArrayList<>(3);
        params.add(new MetadataParameter());
        params.add(new MetadataParameter());
        params.add(new MetadataParameter());
        params.add(new MetadataParameter());
        params.add(new MetadataParameter());
        Assert.assertEquals("{1}{3}{5}{7}{9}", new Metadata("foo", null, params).setGroup(true).getMasterValue());
    }

    /**
     * @see Metadata#getMasterValue()
     * @verifies return single placeholder for non group metadata if masterValue empty
     */
    @Test
    public void getMasterValue_shouldReturnSinglePlaceholderForNonGroupMetadataIfMasterValueEmpty() throws Exception {
        Assert.assertEquals("{0}", new Metadata().getMasterValue());

    }
}
