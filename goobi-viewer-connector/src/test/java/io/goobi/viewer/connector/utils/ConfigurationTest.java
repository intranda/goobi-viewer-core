/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.connector.utils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.connector.AbstractSolrEnabledTest;
import io.goobi.viewer.connector.AbstractTest;
import io.goobi.viewer.connector.DataManager;
import io.goobi.viewer.connector.oai.enums.Metadata;
import io.goobi.viewer.connector.oai.model.Set;
import io.goobi.viewer.connector.oai.model.formats.Format;
import io.goobi.viewer.solr.SolrConstants;

class ConfigurationTest extends AbstractTest {

    /**
     * @see Configuration#getAdditionalDocstructTypes()
     * @verifies return all values
     */
    @Test
    void getAdditionalDocstructTypes_shouldReturnAllValues() {
        List<String> values = DataManager.getInstance().getConfiguration().getAdditionalDocstructTypes();
        Assertions.assertNotNull(values);
        Assertions.assertEquals(2, values.size());
        Assertions.assertEquals("Article", values.get(0));
        Assertions.assertEquals("Photograph", values.get(1));
    }

    /**
     * @see Configuration#getAdditionalSets()
     * @verifies return all values
     */
    @Test
    void getAdditionalSets_shouldReturnAllValues() {
        List<Set> values = DataManager.getInstance().getConfiguration().getAdditionalSets();
        Assertions.assertNotNull(values);
        Assertions.assertEquals(1, values.size());
        Set set = values.get(0);
        Assertions.assertEquals("test", set.getSetName());
        Assertions.assertEquals("testspec", set.getSetSpec());
        Assertions.assertEquals("DC:a.b.c.d", set.getSetQuery());
    }

    /**
     * @see Configuration#getDocumentResolverUrl()
     * @verifies return correct value
     */
    @Test
    void getDocumentResolverUrl_shouldReturnCorrectValue() {
        Assertions.assertEquals("http://localhost/viewer/metsresolver?id=", DataManager.getInstance().getConfiguration().getDocumentResolverUrl());
    }

    /**
     * @see Configuration#getEseDataProviderField()
     * @verifies return correct value
     */
    @Test
    void getEseDataProviderField_shouldReturnCorrectValue() {
        Assertions.assertEquals("MD_DEFAULTPROVIDER", DataManager.getInstance().getConfiguration().getEseDataProviderField());
    }

    /**
     * @see Configuration#getEseDefaultProvider()
     * @verifies return correct value
     */
    @Test
    void getEseDefaultProvider_shouldReturnCorrectValue() {
        Assertions.assertEquals("Institution XYZ", DataManager.getInstance().getConfiguration().getEseDefaultProvider());
    }

    /**
     * @see Configuration#getEseDefaultRightsUrl()
     * @verifies return correct value
     */
    @Test
    void getEseDefaultRightsUrl_shouldReturnCorrectValue() {
        Assertions.assertEquals("http://www.example.com/rights", DataManager.getInstance().getConfiguration().getEseDefaultRightsUrl());
    }

    /**
     * @see Configuration#getEseProviderField()
     * @verifies return correct value
     */
    @Test
    void getEseProviderField_shouldReturnCorrectValue() {
        Assertions.assertEquals("MD_ACCESSCONDITIONCOPYRIGHT", DataManager.getInstance().getConfiguration().getEseProviderField());
    }

    /**
     * @see Configuration#getEseRightsField()
     * @verifies return correct value
     */
    @Test
    void getEseRightsField_shouldReturnCorrectValue() {
        Assertions.assertEquals("TODO", DataManager.getInstance().getConfiguration().getEseRightsField());
    }

    /**
     * @see Configuration#getEseTypes()
     * @verifies return all values
     */
    @Test
    void getEseTypes_shouldReturnAllValues() {
        Map<String, String> values = DataManager.getInstance().getConfiguration().getEseTypes();
        Assertions.assertNotNull(values);
        Assertions.assertEquals(6, values.size());
        Assertions.assertEquals("VIDEO", values.get("video"));
    }

    /**
     * @see Configuration#getIndexUrl()
     * @verifies return correct value
     */
    @Test
    void getIndexUrl_shouldReturnCorrectValue() {
        Assertions.assertEquals(AbstractSolrEnabledTest.SOLR_TEST_URL, DataManager.getInstance().getConfiguration().getIndexUrl());
    }

    /**
     * @see Configuration#getMods2MarcXsl()
     * @verifies return correct value
     */
    @Test
    void getMods2MarcXsl_shouldReturnCorrectValue() {
        Assertions.assertEquals("src/test/resources/oai/MODS2MARC21slim.xsl", DataManager.getInstance().getConfiguration().getMods2MarcXsl());
    }

    /**
     * @see Configuration#getPiResolverUrl()
     * @verifies return correct value
     */
    @Test
    void getPiResolverUrl_shouldReturnCorrectValue() {
        Assertions.assertEquals("http://localhost/viewer/piresolver?id=", DataManager.getInstance().getConfiguration().getPiResolverUrl());
    }

    /**
     * @see Configuration#getOaiFolder()
     * @verifies return correct value
     */
    @Test
    void getOaiFolder_shouldReturnCorrectValue() {
        Assertions.assertEquals("src/test/resources/oai/", DataManager.getInstance().getConfiguration().getOaiFolder());
    }

    /**
     * @see Configuration#getResumptionTokenFolder()
     * @verifies return correct value
     */
    @Test
    void getResumptionTokenFolder_shouldReturnCorrectValue() {
        Assertions.assertEquals("src/test/resources/oai/token/", DataManager.getInstance().getConfiguration().getResumptionTokenFolder());
    }

    /**
     * @see Configuration#getUrnResolverUrl()
     * @verifies return correct value
     */
    @Test
    void getUrnResolverUrl_shouldReturnCorrectValue() {
        Assertions.assertEquals("http://localhost/viewer/resolver?urn=", DataManager.getInstance().getConfiguration().getUrnResolverUrl());
    }

    /**
     * @see Configuration#getViewerConfigFolder()
     * @verifies add trailing slash
     */
    @Test
    void getViewerConfigFolder_shouldAddTrailingSlash() {
        Assertions.assertEquals("src/test/resources/", DataManager.getInstance().getConfiguration().getViewerConfigFolder());
    }

    /**
     * @see Configuration#getViewerConfigFolder()
     * @verifies return environment variable value if available
     */
    @Test
    void getViewerConfigFolder_shouldReturnEnvironmentVariableValueIfAvailable() {
        try {
            System.setProperty("configFolder", "/opt/digiverso/viewer/config_other/");
            Assertions
                    .assertTrue(DataManager.getInstance().getConfiguration().getViewerConfigFolder().endsWith("/opt/digiverso/viewer/config_other/"));
        } finally {
            System.clearProperty("configFolder");
        }
    }

    /**
     * @see Configuration#getUrnPrefixBlacklist()
     * @verifies return all values
     */
    @Test
    void getUrnPrefixBlacklist_shouldReturnAllValues() {
        List<String> values = DataManager.getInstance().getConfiguration().getUrnPrefixBlacklist();
        Assertions.assertNotNull(values);
        Assertions.assertEquals(2, values.size());
        Assertions.assertEquals("urn:nbn:de:test_", values.get(0));
        Assertions.assertEquals("urn:nbn:de:hidden_", values.get(1));
    }

    /**
     * @see Configuration#getHitsPerToken()
     * @verifies return correct value
     */
    @Test
    void getHitsPerToken_shouldReturnCorrectValue() {
        Assertions.assertEquals(23, DataManager.getInstance().getConfiguration().getHitsPerToken());
    }

    /**
     * @see Configuration#getHitsPerTokenForMetadataFormat(String)
     * @verifies return correct value
     */
    @Test
    void getHitsPerTokenForMetadataFormat_shouldReturnCorrectValue() {
        Assertions.assertEquals(11,
                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix()));
        Assertions.assertEquals(12, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.ESE.name().toLowerCase()));
        Assertions.assertEquals(13, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.METS.getMetadataPrefix()));
        Assertions.assertEquals(14, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.LIDO.getMetadataPrefix()));
        Assertions.assertEquals(15,
                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.MARCXML.getMetadataPrefix()));
        Assertions.assertEquals(16,
                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.EPICUR.getMetadataPrefix()));
        Assertions.assertEquals(17,
                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.IV_OVERVIEWPAGE.getMetadataPrefix()));
        Assertions.assertEquals(18,
                DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat(Metadata.IV_CROWDSOURCING.getMetadataPrefix()));
    }

    /**
     * @see Configuration#getHitsPerTokenForMetadataFormat(String)
     * @verifies return default value for unknown formats
     */
    @Test
    void getHitsPerTokenForMetadataFormat_shouldReturnDefaultValueForUnknownFormats() {
        Assertions.assertEquals(23, DataManager.getInstance().getConfiguration().getHitsPerTokenForMetadataFormat("notfound"));
    }

    /**
     * @see Configuration#getVersionDisriminatorFieldForMetadataFormat(String)
     * @verifies return correct value
     */
    @Test
    void getVersionDisriminatorFieldForMetadataFormat_shouldReturnCorrectValue() {
        Assertions.assertEquals(SolrConstants.LANGUAGE,
                DataManager.getInstance().getConfiguration().getVersionDisriminatorFieldForMetadataFormat("tei"));
    }

    /**
     * @see Configuration#isMetadataFormatEnabled(String)
     * @verifies return correct value
     */
    @Test
    void isMetadataFormatEnabled_shouldReturnCorrectValue() {
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.OAI_DC.getMetadataPrefix()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.ESE.name().toLowerCase()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.METS.getMetadataPrefix()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.LIDO.getMetadataPrefix()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.MARCXML.getMetadataPrefix()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.EPICUR.getMetadataPrefix()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.IV_OVERVIEWPAGE.getMetadataPrefix()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.IV_CROWDSOURCING.getMetadataPrefix()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.TEI.getMetadataPrefix()));
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled(Metadata.CMDI.getMetadataPrefix()));
    }

    /**
     * @see Configuration#isMetadataFormatEnabled(String)
     * @verifies return false for unknown formats
     */
    @Test
    void isMetadataFormatEnabled_shouldReturnFalseForUnknownFormats() {
        Assertions.assertFalse(DataManager.getInstance().getConfiguration().isMetadataFormatEnabled("notfound"));
    }

    /**
     * @see Configuration#getAllValuesSets()
     * @verifies return all values
     */
    @Test
    void getAllValuesSets_shouldReturnAllValues() {
        List<Set> values = DataManager.getInstance().getConfiguration().getAllValuesSets();
        Assertions.assertNotNull(values);
        Assertions.assertEquals(2, values.size());
        Assertions.assertEquals("DC", values.get(0).getSetName());
        Assertions.assertFalse(values.get(0).isTranslate());
        Assertions.assertEquals("MD_WISSENSGEBIET", values.get(1).getSetName());
        Assertions.assertFalse(values.get(1).isTranslate());
    }

    /**
     * @see Configuration#getMetadataConfiguration(String,String)
     * @verifies return default template configuration if template not found
     */
    @Test
    void getMetadataConfiguration_shouldReturnDefaultTemplateConfigurationOfTemplateNotFound() {
        List<io.goobi.viewer.connector.oai.model.metadata.Metadata> metadataList =
                DataManager.getInstance().getConfiguration().getMetadataConfiguration(Metadata.OAI_DC.getMetadataPrefix(), "monograph");
        Assertions.assertEquals(14, metadataList.size());
    }

    /**
     * @see Configuration#getBaseURL()
     * @verifies return correct value
     */
    @Test
    void getBaseURL_shouldReturnCorrectValue() {
        Assertions.assertEquals("http://localhost:8080/viewer/oai", DataManager.getInstance().getConfiguration().getBaseURL());
    }

    /**
     * @see Configuration#isBaseUrlUseInRequestElement()
     * @verifies return correct value
     */
    @Test
    void isBaseUrlUseInRequestElement_shouldReturnCorrectValue() {
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isBaseUrlUseInRequestElement());
    }

    /**
     * @see Configuration#getDefaultLocale()
     * @verifies return correct value
     */
    @Test
    void getDefaultLocale_shouldReturnCorrectValue() {
        Assertions.assertEquals(Locale.GERMAN, DataManager.getInstance().getConfiguration().getDefaultLocale());
    }

    /**
     * @see Configuration#getSetSpecFieldsForMetadataFormat(String)
     * @verifies return all values
     */
    @Test
    void getSetSpecFieldsForMetadataFormat_shouldReturnAllValues() {
        List<String> values = DataManager.getInstance().getConfiguration().getSetSpecFieldsForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix());
        Assertions.assertNotNull(values);
        Assertions.assertEquals(2, values.size());
        Assertions.assertEquals(SolrConstants.DC, values.get(0));
        Assertions.assertEquals(SolrConstants.DOCSTRCT, values.get(1));
    }

    /**
     * @see Configuration#getRestApiUrl()
     * @verifies return correct value
     */
    @Test
    void getRestApiUrl_shouldReturnCorrectValue() {
        Assertions.assertEquals("http://localhost/viewer/api/v1/", DataManager.getInstance().getConfiguration().getRestApiUrl());
    }

    /**
     * @see Configuration#getHarvestUrl()
     * @verifies return correct value
     */
    @Test
    void getHarvestUrl_shouldReturnCorrectValue() {
        Assertions.assertEquals("http://localhost/viewer/harvest", DataManager.getInstance().getConfiguration().getHarvestUrl());
    }

    /**
     * @see Configuration#getAccessConditionMappingForMetadataFormat(String,String)
     * @verifies return correct value
     */
    @Test
    void getAccessConditionMappingForMetadataFormat_shouldReturnCorrectValue() {
        Assertions.assertEquals(Format.ACCESSCONDITION_OPENACCESS,
                DataManager.getInstance()
                        .getConfiguration()
                        .getAccessConditionMappingForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix(), "Public Domain Mark 1.0"));
        Assertions.assertEquals(Format.ACCESSCONDITION_OPENACCESS,
                DataManager.getInstance()
                        .getConfiguration()
                        .getAccessConditionMappingForMetadataFormat(Metadata.OAI_DC.getMetadataPrefix(), "Rechte vorbehalten - Freier Zugang"));
    }

    /**
     * @see Configuration#getOaiIdentifier()
     * @verifies read config values correctly
     */
    @Test
    void getOaiIdentifier_shouldReadConfigValuesCorrectly() {
        Map<String, String> map = DataManager.getInstance().getConfiguration().getOaiIdentifier();
        Assertions.assertEquals("http://www.openarchives.org/OAI/2.0/", map.get("xmlns"));
        Assertions.assertEquals("repo", map.get("repositoryIdentifier"));
    }
}
