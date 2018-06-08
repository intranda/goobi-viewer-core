package de.intranda.digiverso.presentation.model.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.managedbeans.NavigationHelper;

public class MetadataTest {

    /**
     * @see Metadata#filterMetadataByLanguage(List,Locale)
     * @verifies return language-specific version of a field
     */
    @Test
    public void filterMetadataByLanguage_shouldReturnLanguagespecificVersionOfAField() throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata("MD_TITLE_LANG_EN", "", "foo"));
        metadataList.add(new Metadata("MD_TITLE", "", "bar"));
        List<Metadata> filteredList = Metadata.filterMetadataByLanguage(metadataList, "en");
        Assert.assertEquals(1, filteredList.size());
        Assert.assertEquals("MD_TITLE_LANG_EN", filteredList.get(0).getLabel());
    }

    /**
     * @see Metadata#filterMetadataByLanguage(List,Locale)
     * @verifies return generic version if no language specific version is found
     */
    @Test
    public void filterMetadataByLanguage_shouldReturnGenericVersionIfNoLanguageSpecificVersionIsFound() throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata("MD_TITLE_LANG_DE", "", "foo"));
        metadataList.add(new Metadata("MD_TITLE", "", "bar"));
        List<Metadata> filteredList = Metadata.filterMetadataByLanguage(metadataList, "en");
        Assert.assertEquals(1, filteredList.size());
        Assert.assertEquals("MD_TITLE", filteredList.get(0).getLabel());
    }

    /**
     * @see Metadata#filterMetadataByLanguage(List,String)
     * @verifies preserve metadata field order
     */
    @Test
    public void filterMetadataByLanguage_shouldPreserveMetadataFieldOrder() throws Exception {
        List<Metadata> metadataList = new ArrayList<>();
        metadataList.add(new Metadata("MD_TITLE_LANG_EN", "", "foo"));
        metadataList.add(new Metadata("MD_TITLE_LANG_DE", "", "foo"));
        metadataList.add(new Metadata(SolrConstants.PI, "", "PPN123"));
        metadataList.add(new Metadata("MD_DESCRIPTION_LANG_EN", "", "foo"));
        List<Metadata> filteredList = Metadata.filterMetadataByLanguage(metadataList, "en");
        Assert.assertEquals(3, filteredList.size());
        Assert.assertEquals("MD_TITLE_LANG_EN", filteredList.get(0).getLabel());
        Assert.assertEquals(SolrConstants.PI, filteredList.get(1).getLabel());
        Assert.assertEquals("MD_DESCRIPTION_LANG_EN", filteredList.get(2).getLabel());
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
                    "<a href=\"http://localhost:8080/browse/DC:a/-/1/-/-/\">a</a> > <a href=\"http://localhost:8080/browse/DC:a.b/-/1/-/-/\">a.b</a>",
                    value);
        }
        {
            // No root URL
            String value = Metadata.buildHierarchicalValue("DC", "a.b.c.d", null, null);
            Assert.assertEquals("a > a.b > a.b.c > a.b.c.d", value);
        }
    }
}