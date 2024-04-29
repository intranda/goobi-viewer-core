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
package io.goobi.viewer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.model.SelectItem;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.TestUtils;
import io.goobi.viewer.controller.config.filter.IFilterConfiguration;
import io.goobi.viewer.controller.model.LabeledValue;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.citation.CitationLink;
import io.goobi.viewer.model.citation.CitationLink.CitationLinkLevel;
import io.goobi.viewer.model.citation.CitationLink.CitationLinkType;
import io.goobi.viewer.model.export.ExportFieldConfiguration;
import io.goobi.viewer.model.job.download.DownloadOption;
import io.goobi.viewer.model.maps.GeoMapMarker;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataParameter.MetadataParameterType;
import io.goobi.viewer.model.metadata.MetadataReplaceRule.MetadataReplaceRuleType;
import io.goobi.viewer.model.metadata.MetadataView;
import io.goobi.viewer.model.misc.EmailRecipient;
import io.goobi.viewer.model.search.AdvancedSearchFieldConfiguration;
import io.goobi.viewer.model.search.SearchFilter;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.search.SearchSortingOption;
import io.goobi.viewer.model.security.CopyrightIndicatorLicense;
import io.goobi.viewer.model.security.CopyrightIndicatorStatus;
import io.goobi.viewer.model.security.SecurityQuestion;
import io.goobi.viewer.model.security.authentication.HttpAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.translations.admin.TranslationGroup;
import io.goobi.viewer.model.translations.admin.TranslationGroup.TranslationGroupType;
import io.goobi.viewer.model.translations.admin.TranslationGroupItem;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;
import io.goobi.viewer.solr.SolrConstants;

class ConfigurationTest extends AbstractTest {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(ConfigurationTest.class);

    /**
     * @see Configuration#getConfigLocalPath()
     * @verifies return environment variable value if available
     */
    @Test
    void getConfigLocalPath_shouldReturnEnvironmentVariableValueIfAvailable() throws Exception {
        try {
            System.setProperty("configFolder", "/opt/digiverso/viewer/config_other/");
            assertTrue(DataManager.getInstance().getConfiguration().getConfigLocalPath().endsWith("/opt/digiverso/viewer/config_other/"));
        } finally {
            System.clearProperty("configFolder");
        }
    }

    /**
     * @see Configuration#getConfigLocalPath()
     * @verifies add trailing slash
     */
    @Test
    void getConfigLocalPath_shouldAddTrailingSlash() throws Exception {
        assertEquals("target/configFolder_value/", DataManager.getInstance().getConfiguration().getConfigLocalPath());
    }

    /**
     * @see Configuration#getBreadcrumbsClipping()
     * @verifies return correct value
     */
    @Test
    void getBreadcrumbsClipping_shouldReturnCorrectValue() throws Exception {
        Assertions.assertEquals(24, DataManager.getInstance().getConfiguration().getBreadcrumbsClipping());
    }

    /**
     * @see Configuration#getBrowsingMenuFields()
     * @verifies return all configured elements
     */
    @Test
    void getBrowsingMenuFields_shouldReturnAllConfiguredElements() throws Exception {
        assertEquals(6, DataManager.getInstance().getConfiguration().getBrowsingMenuFields().size());
    }

    /**
     * @see Configuration#getBrowsingMenuHitsPerPage()
     * @verifies return correct value
     */
    @Test
    void getBrowsingMenuHitsPerPage_shouldReturnCorrectValue() throws Exception {
        assertEquals(19, DataManager.getInstance().getConfiguration().getBrowsingMenuHitsPerPage());
    }

    /**
     * @see Configuration#getBrowsingMenuIndexSizeThreshold()
     * @verifies return correct value
     */
    @Test
    void getBrowsingMenuIndexSizeThreshold_shouldReturnCorrectValue() throws Exception {
        assertEquals(50000, DataManager.getInstance().getConfiguration().getBrowsingMenuIndexSizeThreshold());
    }

    /**
     * @see Configuration#getBrowsingMenuSortingIgnoreLeadingChars()
     * @verifies return correct value
     */
    @Test
    void getBrowsingMenuSortingIgnoreLeadingChars_shouldReturnCorrectValue() throws Exception {
        assertEquals(".[]", DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars());
    }

    /**
     * @see Configuration#getCollectionBlacklist()
     * @verifies return all configured elements
     */
    @Test
    void getCollectionBlacklist_shouldReturnAllConfiguredElements() throws Exception {
        List<String> ret = DataManager.getInstance().getConfiguration().getCollectionBlacklist(SolrConstants.DC);
        Assertions.assertNotNull(ret);
        assertEquals(2, ret.size());
    }

    @Test
    void getCollectionDefaultSortFields_shouldReturnAllFields() {
        Map<String, String> sortFields = DataManager.getInstance().getConfiguration().getCollectionDefaultSortFields(SolrConstants.DC);
        assertEquals(2, sortFields.size());
        assertEquals("SORT_CREATOR", sortFields.get("collection1*"));
        assertEquals("SORT_TITLE", sortFields.get("collection1"));
    }

    /**
     * @see Configuration#getCollectionDisplayNumberOfVolumesLevel()
     * @verifies return correct value
     */
    @Test
    void getCollectionDisplayNumberOfVolumesLevel_shouldReturnCorrectValue() throws Exception {
        assertEquals(16, DataManager.getInstance().getConfiguration().getCollectionDisplayNumberOfVolumesLevel(SolrConstants.DC));
    }

    /**
     * @see Configuration#getCollectionSorting()
     * @verifies return all configured elements
     */
    @Test
    void getCollectionSorting_shouldReturnAllConfiguredElements() throws Exception {
        assertEquals(3, DataManager.getInstance().getConfiguration().getCollectionSorting(SolrConstants.DC).size());
    }

    /**
     * @see Configuration#getDownloadUrl()
     * @verifies return correct value
     */
    @Test
    void getDownloadUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("https://viewer.goobi.io/download/", DataManager.getInstance().getConfiguration().getDownloadUrl());
    }

    /**
     * @see Configuration#getDataRepositoriesHome()
     * @verifies return correct value
     */
    @Test
    void getDataRepositoriesHome_shouldReturnCorrectValue() throws Exception {
        assertEquals("src/test/resources/data/viewer/data/", DataManager.getInstance().getConfiguration().getDataRepositoriesHome());
    }

    /**
     * @see Configuration#getDcUrl()
     * @verifies return correct value
     */
    @Test
    void getDcUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("dc_value", DataManager.getInstance().getConfiguration().getDcUrl());
    }

    /**
     * @see Configuration#getDisplayBreadcrumbs()
     * @verifies return correct value
     */
    @Test
    void getDisplayBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().getDisplayBreadcrumbs());
    }

    /**
     * @see Configuration#getDisplayMetadataPageLinkBlock()
     * @verifies return correct value
     */
    @Test
    void getDisplayMetadataPageLinkBlock_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().getDisplayMetadataPageLinkBlock());
    }

    /**
     * @see Configuration#getDisplayStructType()
     * @verifies return correct value
     */
    @Test
    void getDisplayStructType_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().getDisplayStructType());
    }

    /**
     * @see Configuration#getEseUrl()
     * @verifies return correct value
     */
    @Test
    void getEseUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("ese_value", DataManager.getInstance().getConfiguration().getEseUrl());
    }

    /**
     * @see Configuration#getFeedbackEmailRecipients()
     * @verifies return correct values
     */
    @Test
    void getFeedbackEmailRecipients_shouldReturnCorrectValues() throws Exception {
        List<EmailRecipient> result = DataManager.getInstance().getConfiguration().getFeedbackEmailRecipients();
        assertEquals(2, result.size());
        {
            EmailRecipient recipient = result.get(0);
            assertEquals("Everyone", recipient.getLabel());
            assertEquals("everyone@example.com", recipient.getEmailAddress());
            assertEquals("genId_1", recipient.getId());
            assertTrue(recipient.isDefaultRecipient());
        }
        {
            EmailRecipient recipient = result.get(1);
            assertEquals("someone@example.com", recipient.getLabel()); // No label defined, using address
            assertEquals("someone@example.com", recipient.getEmailAddress());
            assertEquals("someid", recipient.getId());
            assertFalse(recipient.isDefaultRecipient());
        }
    }

    /**
     * @see Configuration#getSearchHitsPerPageDefaultValue()
     * @verifies return correct value
     */
    @Test
    void getSearchHitsPerPageDefaultValue_shouldReturnCorrectValue() throws Exception {
        assertEquals(15, DataManager.getInstance().getConfiguration().getSearchHitsPerPageDefaultValue());
    }

    /**
     * @see Configuration#getSearchHitsPerPageValues()
     * @verifies return all values
     */
    @Test
    void getSearchHitsPerPageValues_shouldReturnAllValues() throws Exception {
        assertEquals(4, DataManager.getInstance().getConfiguration().getSearchHitsPerPageValues().size());
    }

    /**
     * @see Configuration#isDisplaySearchHitNumbers()
     * @verifies return correct value
     */
    @Test
    void isDisplaySearchHitNumbers_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isDisplaySearchHitNumbers());
    }

    /**
     * @see Configuration#getFulltextFragmentLength()
     * @verifies return correct value
     */
    @Test
    void getFulltextFragmentLength_shouldReturnCorrectValue() throws Exception {
        assertEquals(50, DataManager.getInstance().getConfiguration().getFulltextFragmentLength());
    }

    /**
     * @see Configuration#getHotfolder()
     * @verifies return correct value
     */
    @Test
    void getHotfolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("target/hotfolder", DataManager.getInstance().getConfiguration().getHotfolder());
    }

    /**
     * @see Configuration#getIndexedLidoFolder()
     * @verifies return correct value
     */
    @Test
    void getIndexedLidoFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("indexed_lido", DataManager.getInstance().getConfiguration().getIndexedLidoFolder());
    }

    /**
     * @see Configuration#getIndexedEadFolder()
     * @verifies return correct value
     */
    @Test
    void getIndexedEadFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("indexed_ead", DataManager.getInstance().getConfiguration().getIndexedEadFolder());
    }

    /**
     * @see Configuration#getIndexedMetsFolder()
     * @verifies return correct value
     */
    @Test
    void getIndexedMetsFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("indexed_mets", DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
    }

    /**
     * @see Configuration#getIndexedDenkxwebFolder()
     * @verifies return correct value
     */
    @Test
    void getIndexedDenkxwebFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("indexed_denkxweb", DataManager.getInstance().getConfiguration().getIndexedDenkxwebFolder());
    }

    /**
     * @see Configuration#getIndexedDublinCoreFolder()
     * @verifies return correct value
     */
    @Test
    void getIndexedDublinCoreFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("indexed_dublincore", DataManager.getInstance().getConfiguration().getIndexedDublinCoreFolder());
    }

    /**
     * @see Configuration#getMetadataViews()
     * @verifies return all configured values
     */
    @Test
    void getMetadataViews_shouldReturnAllConfiguredValues() throws Exception {
        List<MetadataView> result = DataManager.getInstance().getConfiguration().getMetadataViews();
        assertEquals(2, result.size());
        MetadataView view = result.get(1);
        assertEquals(1, view.getIndex());
        assertEquals("label__metadata_other", view.getLabel());
        assertEquals("_other", view.getUrl());
        assertEquals("foo:bar", view.getCondition());
    }

    /**
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    void getMainMetadataForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        assertEquals(1, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, "Chapter").size());
    }

    /**
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return default template configuration if template not found
     */
    @Test
    void getMainMetadataForTemplate_shouldReturnDefaultTemplateConfigurationIfTemplateNotFound() throws Exception {
        assertEquals(6, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, "nonexisting").size());
    }

    /**
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return default template if template is null
     */
    @Test
    void getMainMetadataForTemplate_shouldReturnDefaultTemplateIfTemplateIsNull() throws Exception {
        assertEquals(6, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, null).size());
    }
    
    /**
     * @see Configuration#getArchiveMetadataForTemplate(String)
     * @verifies return default template configuration if template not found
     */
    @Test
    void getArchiveMetadataForTemplate_shouldReturnDefaultTemplateConfigurationIfTemplateNotFound() throws Exception {
        assertEquals(9, DataManager.getInstance().getConfiguration().getArchiveMetadataForTemplate("nonexisting").size());
    }

    /**
     * @see Configuration#getTocLabelConfiguration(String)
     * @verifies return correct template configuration
     */
    @Test
    void getTocLabelConfiguration_shouldReturnCorrectTemplateConfiguration() throws Exception {
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getTocLabelConfiguration("PeriodicalVolume");
        Assertions.assertNotNull(metadataList);
        assertEquals(1, metadataList.size());
        Metadata metadata = metadataList.get(0);
        assertEquals("", metadata.getLabel());
        assertEquals("{CURRENTNO}{MD_TITLE}", metadata.getMasterValue());
        List<MetadataParameter> params = metadata.getParams();
        assertEquals(2, params.size());
        assertEquals("CURRENTNO", params.get(0).getKey());
        assertEquals("Number ", params.get(0).getPrefix());
        assertEquals("MD_TITLE", params.get(1).getKey());
        assertEquals("LABEL", params.get(1).getAltKey());
        assertEquals(": ", params.get(1).getPrefix());
    }

    /**
     * @see Configuration#getTocLabelConfiguration(String)
     * @verifies return default template configuration if template not found
     */
    @Test
    void getTocLabelConfiguration_shouldReturnDefaultTemplateConfigurationIfTemplateNotFound() throws Exception {
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getTocLabelConfiguration("notfound");
        Assertions.assertNotNull(metadataList);
        assertEquals(1, metadataList.size());
        Metadata metadata = metadataList.get(0);
        assertEquals("", metadata.getLabel());
        assertEquals("{LABEL}{MD_CREATOR}", metadata.getMasterValue());
        List<MetadataParameter> params = metadata.getParams();
        assertEquals(2, params.size());
        assertEquals("LABEL", params.get(0).getKey());
        assertEquals("MD_CREATOR", params.get(1).getKey());
        assertEquals(" / ", params.get(1).getPrefix());
    }

    /**
     * @see Configuration#getMarcUrl()
     * @verifies return correct value
     */
    @Test
    void getMarcUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("marc_value", DataManager.getInstance().getConfiguration().getMarcUrl());
    }

    /**
     * @see Configuration#getMediaFolder()
     * @verifies return correct value
     */
    @Test
    void getMediaFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("media", DataManager.getInstance().getConfiguration().getMediaFolder());
    }

    /**
     * @see Configuration#getPdfFolder()
     * @verifies return correct value
     */
    @Test
    void getPdfFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("PDF", DataManager.getInstance().getConfiguration().getPdfFolder());
    }

    @Test
    void getVocabulariesFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("vocabularies", DataManager.getInstance().getConfiguration().getVocabulariesFolder());
    }

    /**
     * @see Configuration#getSourceFileUrl()
     * @verifies return correct value
     */
    @Test
    void getSourceFileUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("sourcefile_value", DataManager.getInstance().getConfiguration().getSourceFileUrl());
    }

    /**
     * @see Configuration#isUserRegistrationEnabled()
     * @verifies return correct value
     */
    @Test
    void isUserRegistrationEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isUserRegistrationEnabled());
    }

    /**
     * @see Configuration#getSecurityQuestions()
     * @verifies return all configured elements
     */
    @Test
    void getSecurityQuestions_shouldReturnAllConfiguredElements() throws Exception {
        List<SecurityQuestion> result = DataManager.getInstance().getConfiguration().getSecurityQuestions();
        Assertions.assertNotNull(result);
        assertEquals(2, result.size());
        {
            SecurityQuestion q = result.get(0);
            assertEquals("user__security_question__1", q.getQuestionKey());
            assertEquals(3, q.getCorrectAnswers().size());
            assertTrue(q.getCorrectAnswers().contains("foo"));
            assertTrue(q.getCorrectAnswers().contains("f00"));
            assertTrue(q.getCorrectAnswers().contains("phoo"));
        }
    }

    /**
     * @see Configuration#isShowOpenIdConnect()
     * @verifies return correct value
     */
    @Test
    void isShowOpenIdConnect_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isShowOpenIdConnect());
    }

    /**
     * @see Configuration#getAuthenticationProviders()
     * @verifies return all properly configured elements
     */
    @Test
    void getAuthenticationProviders_shouldReturnAllProperlyConfiguredElements() throws Exception {
        List<IAuthenticationProvider> providers = DataManager.getInstance().getConfiguration().getAuthenticationProviders();
        assertEquals(6, providers.size());

        // OpenID Connect
        assertEquals("Custom OIDC", providers.get(2).getName());
        assertEquals("openid", providers.get(2).getType().toLowerCase());
        assertEquals("https://example.com/oauth/auth", ((OpenIdProvider) providers.get(2)).getUrl());
        assertEquals("https://example.com/oauth/token", ((OpenIdProvider) providers.get(2)).getTokenEndpoint());
        assertEquals("https://example.com/viewer/oauth", ((OpenIdProvider) providers.get(2)).getRedirectionEndpoint());
        assertEquals("email", ((OpenIdProvider) providers.get(2)).getScope());
        assertEquals("my_id", ((OpenIdProvider) providers.get(2)).getClientId());
        assertEquals("my_secret", ((OpenIdProvider) providers.get(2)).getClientSecret());
        assertEquals("custom.png", ((OpenIdProvider) providers.get(2)).getImage());
        assertEquals("Custom OIDC", ((OpenIdProvider) providers.get(2)).getLabel());

        // vuFind
        assertEquals("VuFind", providers.get(3).getName());
        assertEquals("userpassword", providers.get(3).getType().toLowerCase());
        assertEquals(7000l, ((HttpAuthenticationProvider) providers.get(3)).getTimeoutMillis());
        assertEquals("VuFind-label", ((HttpAuthenticationProvider) providers.get(3)).getLabel());

        // bibliotheca
        assertEquals("Bibliotheca", providers.get(4).getName());
        assertEquals("userpassword", providers.get(4).getType().toLowerCase());

        //local
        assertEquals("Goobi viewer", providers.get(5).getName());
        assertEquals("local", providers.get(5).getType().toLowerCase());
    }

    /**
     * @see Configuration#getAuthenticationProviders()
     * @verifies load user group names correctly
     */
    @Test
    void getAuthenticationProviders_shouldLoadUserGroupNamesCorrectly() throws Exception {
        List<IAuthenticationProvider> providers = DataManager.getInstance().getConfiguration().getAuthenticationProviders();
        assertEquals(6, providers.size());
        List<String> groups = providers.get(3).getAddUserToGroups();
        assertEquals(2, groups.size());
    }

    /**
     * @see Configuration#getOrigContentFolder()
     * @verifies return correct value
     */
    @Test
    void getOrigContentFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("source", DataManager.getInstance().getConfiguration().getOrigContentFolder());
    }

    /**
     * @see Configuration#getPageLoaderThreshold()
     * @verifies return correct value
     */
    @Test
    void getPageLoaderThreshold_shouldReturnCorrectValue() throws Exception {
        assertEquals(1000, DataManager.getInstance().getConfiguration().getPageLoaderThreshold());
    }

    /**
     * @see Configuration#getPageType(PageType)
     * @verifies return the correct value for the given type
     */
    @Test
    void getPageType_shouldReturnTheCorrectValueForTheGivenType() throws Exception {
        assertEquals("viewImage_value", DataManager.getInstance().getConfiguration().getPageType(PageType.viewImage));
    }

    /**
     * @see Configuration#getRssCopyrightText()
     * @verifies return correct value
     */
    @Test
    void getRssCopyrightText_shouldReturnCorrectValue() throws Exception {
        assertEquals("copyright_value", DataManager.getInstance().getConfiguration().getRssCopyrightText());
    }

    /**
     * @see Configuration#getRssDescription()
     * @verifies return correct value
     */
    @Test
    void getRssDescription_shouldReturnCorrectValue() throws Exception {
        assertEquals("description_value", DataManager.getInstance().getConfiguration().getRssDescription());
    }

    /**
     * @see Configuration#getRssFeedItems()
     * @verifies return correct value
     */
    @Test
    void getRssFeedItems_shouldReturnCorrectValue() throws Exception {
        assertEquals(25, DataManager.getInstance().getConfiguration().getRssFeedItems());
    }

    /**
     * @see Configuration#getRssTitle()
     * @verifies return correct value
     */
    @Test
    void getRssTitle_shouldReturnCorrectValue() throws Exception {
        assertEquals("title_value", DataManager.getInstance().getConfiguration().getRssTitle());
    }

    /**
     * @see Configuration#getMetadataListTypes(String)
     * @verifies return all metadataList types if prefix empty
     */
    @Test
    void getMetadataListTypes_shouldReturnAllMetadataListTypesIfPrefixEmpty() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getMetadataListTypes(null);
        assertEquals(5, result.size());
    }

    /**
     * @see Configuration#getMetadataListTypes(String)
     * @verifies filter by prefix correctly
     */
    @Test
    void getMetadataListTypes_shouldFilterByPrefixCorrectly() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getMetadataListTypes("cms_");
        assertEquals(1, result.size());
        assertEquals("cms_fooBar", result.get(0));
    }

    /**
     * @see Configuration#getMetadataConfigurationForTemplate(String,String,boolean,boolean)
     * @verifies throw IllegalArgumentException if type null
     */
    @Test
    void getMetadataConfigurationForTemplate_shouldThrowIllegalArgumentExceptionIfTypeNull() throws Exception {
        Configuration config = DataManager.getInstance().getConfiguration();
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> config.getMetadataConfigurationForTemplate(null, Configuration.VALUE_DEFAULT, false, false));
        assertEquals("type may not be null", e.getMessage());
    }

    /**
     * @see Configuration#getMetadataConfigurationForTemplate(String,String,boolean,boolean)
     * @verifies return empty list if list type not found
     */
    @Test
    void getMetadataConfigurationForTemplate_shouldReturnEmptyListIfListTypeNotFound() throws Exception {
        List<Metadata> result = DataManager.getInstance()
                .getConfiguration()
                .getMetadataConfigurationForTemplate("sometype", Configuration.VALUE_DEFAULT, false, false);
        assertTrue(result.isEmpty());
        result.add(new Metadata()); // Make sure list is mutable
    }

    /**
     * @see Configuration#getSearchHitMetadataForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    void getSearchHitMetadataForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        assertEquals(1, DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate("Chapter").size());
    }

    /**
     * @see Configuration#getSearchHitMetadataForTemplate(String)
     * @verifies return default template configuration if requested not found
     */
    @Test
    void getSearchHitMetadataForTemplate_shouldReturnDefaultTemplateConfigurationIfRequestedNotFound() throws Exception {
        assertEquals(5, DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate("nonexisting").size());
    }

    /**
     * @see Configuration#getSearchHitMetadataForTemplate(String)
     * @verifies return default template if template is null
     */
    @Test
    void getSearchHitMetadataForTemplate_shouldReturnDefaultTemplateIfTemplateIsNull() throws Exception {
        assertEquals(5, DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate(null).size());
    }

    /**
     * @see Configuration#getHighlightMetadataForTemplate(String)
     * @verifies return default template configuration if requested not found
     */
    @Test
    void getHighlightMetadataForTemplate_shouldReturnDefaultTemplateConfigurationIfRequestedNotFound() throws Exception {
        assertEquals(2, DataManager.getInstance().getConfiguration().getHighlightMetadataForTemplate("notfound").size());
    }

    /**
     * @see Configuration#getSearchHitMetadataValueLength()
     * @verifies return correct value
     */
    @Test
    void getSearchHitMetadataValueLength_shouldReturnCorrectValue() throws Exception {
        assertEquals(18, DataManager.getInstance().getConfiguration().getSearchHitMetadataValueLength());
    }

    /**
     * @see Configuration#getSearchHitMetadataValueNumber()
     * @verifies return correct value
     */
    @Test
    void getSearchHitMetadataValueNumber_shouldReturnCorrectValue() throws Exception {
        assertEquals(17, DataManager.getInstance().getConfiguration().getSearchHitMetadataValueNumber());
    }

    /**
     * @see Configuration#getSidebarTocInitialCollapseLevel()
     * @verifies return correct value
     */
    @Test
    void getSidebarTocInitialCollapseLevel_shouldReturnCorrectValue() throws Exception {
        assertEquals(22, DataManager.getInstance().getConfiguration().getSidebarTocInitialCollapseLevel());
    }

    /**
     * @see Configuration#getSidebarTocLengthBeforeCut()
     * @verifies return correct value
     */
    @Test
    void getSidebarTocLengthBeforeCut_shouldReturnCorrectValue() throws Exception {
        assertEquals(21, DataManager.getInstance().getConfiguration().getSidebarTocLengthBeforeCut());
    }

    /**
     * @see Configuration#getSidebarTocPageNumbersVisible()
     * @verifies return correct value
     */
    @Test
    void getSidebarTocPageNumbersVisible_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().getSidebarTocPageNumbersVisible());
    }

    /**
     * @see Configuration#isSidebarTocTreeView()
     * @verifies return correct value
     */
    @Test
    void isSidebarTocTreeView_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isSidebarTocTreeView());
    }

    /**
     * @see Configuration#isTocTreeView(String)
     * @verifies return true for allowed docstructs
     */
    @Test
    void isTocTreeView_shouldReturnTrueForAllowedDocstructs() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isTocTreeView("Monograph"));
        assertTrue(DataManager.getInstance().getConfiguration().isTocTreeView("Manuscript"));
        assertTrue(DataManager.getInstance().getConfiguration().isTocTreeView("MusicSupplies"));
    }

    /**
     * @see Configuration#isTocTreeView(String)
     * @verifies return false for other docstructs
     */
    @Test
    void isTocTreeView_shouldReturnFalseForOtherDocstructs() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isTocTreeView("Volume"));
    }

    /**
     * @see Configuration#getSmtpPassword()
     * @verifies return correct value
     */
    @Test
    void getSmtpPassword_shouldReturnCorrectValue() throws Exception {
        assertEquals("smtpPassword_value", DataManager.getInstance().getConfiguration().getSmtpPassword());
    }

    /**
     * @see Configuration#getSmtpSecurity()
     * @verifies return correct value
     */
    @Test
    void getSmtpSecurity_shouldReturnCorrectValue() throws Exception {
        assertEquals("smtpSecurity_value", DataManager.getInstance().getConfiguration().getSmtpSecurity());
    }

    /**
     * @see Configuration#getSmtpPort()
     * @verifies return correct value
     */
    @Test
    void getSmtpPort_shouldReturnCorrectValue() throws Exception {
        assertEquals(25, DataManager.getInstance().getConfiguration().getSmtpPort());
    }

    /**
     * @see Configuration#getSmtpSenderAddress()
     * @verifies return correct value
     */
    @Test
    void getSmtpSenderAddress_shouldReturnCorrectValue() throws Exception {
        assertEquals("smtpSenderAddress_value", DataManager.getInstance().getConfiguration().getSmtpSenderAddress());
    }

    /**
     * @see Configuration#getSmtpSenderName()
     * @verifies return correct value
     */
    @Test
    void getSmtpSenderName_shouldReturnCorrectValue() throws Exception {
        assertEquals("smtpSenderName_value", DataManager.getInstance().getConfiguration().getSmtpSenderName());
    }

    /**
     * @see Configuration#getSmtpServer()
     * @verifies return correct value
     */
    @Test
    void getSmtpServer_shouldReturnCorrectValue() throws Exception {
        assertEquals("smtpServer_value", DataManager.getInstance().getConfiguration().getSmtpServer());
    }

    /**
     * @see Configuration#getSmtpUser()
     * @verifies return correct value
     */
    @Test
    void getSmtpUser_shouldReturnCorrectValue() throws Exception {
        assertEquals("smtpUser_value", DataManager.getInstance().getConfiguration().getSmtpUser());
    }

    /**
     * @see Configuration#getSolrUrl()
     * @verifies return correct value
     */
    @Test
    void getSolrUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("https://viewer-testing-index.goobi.io/solr/collection1", DataManager.getInstance().getConfiguration().getSolrUrl());
    }

    /**
     * @see Configuration#getCollectionSplittingChar(String)
     * @verifies return correct value
     */
    @Test
    void getCollectionSplittingChar_shouldReturnCorrectValue() throws Exception {
        assertEquals(".", DataManager.getInstance().getConfiguration().getCollectionSplittingChar(SolrConstants.DC));
        assertEquals("/", DataManager.getInstance().getConfiguration().getCollectionSplittingChar("MD_KNOWLEDGEFIELD"));
        assertEquals(".", DataManager.getInstance().getConfiguration().getCollectionSplittingChar(SolrConstants.DOCTYPE));
    }

    /**
     * @see Configuration#getPageSelectionFormat()
     * @verifies return correct value
     */
    @Test
    void getPageSelectionFormat_shouldReturnCorrectValue() throws Exception {
        assertEquals("{order} {msg.of} {numpages}", DataManager.getInstance().getConfiguration().getPageSelectionFormat());
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies load all stopwords
     */
    @Test
    void loadStopwords_shouldLoadAllStopwords() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("src/test/resources/stopwords.txt");
        Assertions.assertNotNull(stopwords);
        assertEquals(5, stopwords.size());
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies remove parts starting with pipe
     */
    @Test
    void loadStopwords_shouldRemovePartsStartingWithPipe() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("src/test/resources/stopwords.txt");
        Assertions.assertNotNull(stopwords);
        assertTrue(stopwords.contains("one"));
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies not add empty stopwords
     */
    @Test
    void loadStopwords_shouldNotAddEmptyStopwords() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("src/test/resources/stopwords.txt");
        Assertions.assertNotNull(stopwords);
        assertFalse(stopwords.contains(""));
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies throw IllegalArgumentException if stopwordsFilePath empty
     */
    @Test
    void loadStopwords_shouldThrowIllegalArgumentExceptionIfStopwordsFilePathEmpty() throws Exception {
        Exception e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Configuration.loadStopwords(null));
        assertEquals("stopwordsFilePath may not be null or empty", e.getMessage());
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies throw FileNotFoundException if file does not exist
     */
    @Test
    void loadStopwords_shouldThrowFileNotFoundExceptionIfFileDoesNotExist() throws Exception {
        Assertions.assertThrows(FileNotFoundException.class, () -> Configuration.loadStopwords("src/test/resources/startwords.txt"));
    }

    /**
     * @see Configuration#getStopwords()
     * @verifies return all stopwords
     */
    @Test
    void getStopwords_shouldReturnAllStopwords() throws Exception {
        assertEquals(5, DataManager.getInstance().getConfiguration().getStopwords().size());
    }

    /**
     * @see Configuration#getStopwordsFilePath()
     * @verifies return correct value
     */
    @Test
    void getStopwordsFilePath_shouldReturnCorrectValue() throws Exception {
        assertEquals("src/test/resources/stopwords.txt", DataManager.getInstance().getConfiguration().getStopwordsFilePath());
    }

    /**
     * @see Configuration#getSubthemeMainTheme()
     * @verifies return correct value
     */
    @Test
    void getSubthemeMainTheme_shouldReturnCorrectValue() throws Exception {
        assertEquals("mainTheme_value", DataManager.getInstance().getConfiguration().getSubthemeMainTheme());
    }

    /**
     * @see Configuration#getSubthemeDiscriminatorField()
     * @verifies return correct value
     */
    @Test
    void getSubthemeDiscriminatorField_shouldReturnCorrectValue() throws Exception {
        assertEquals("MD2_VIEWERSUBTHEME", DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    //    /**
    //     * @see ConfigurationHelper#getSubthemeMap()
    //     * @verifies return all configured elements
    //     */
    //    @Test
    //    void getSubthemeMap_shouldReturnAllConfiguredElements() throws Exception {
    //        assertEquals(2, DataManager.getInstance().getConfiguration().getSubthemeMap().size());
    //    }

    /**
     * @see Configuration#getTagCloudSampleSize(String)
     * @verifies return correct value for existing fields
     */
    @Test
    void getTagCloudSampleSize_shouldReturnCorrectValueForExistingFields() throws Exception {
        assertEquals(20, DataManager.getInstance().getConfiguration().getTagCloudSampleSize("MD_TITLE"));
    }

    /**
     * @see Configuration#getTagCloudSampleSize(String)
     * @verifies return INT_MAX for other fields
     */
    @Test
    void getTagCloudSampleSize_shouldReturnINT_MAXForOtherFields() throws Exception {
        assertEquals(Integer.MAX_VALUE, DataManager.getInstance().getConfiguration().getTagCloudSampleSize("NONEXISTING_FIELD"));
    }

    /**
     * @see Configuration#getTheme()
     * @verifies return correct value
     */
    @Test
    void getTheme_shouldReturnCorrectValue() throws Exception {
        assertEquals("mainTheme_value", DataManager.getInstance().getConfiguration().getTheme());
    }

    /**
     * @see Configuration#getName()
     * @verifies return correct value
     */
    @Test
    void getName_shouldReturnCorrectValue() throws Exception {
        assertEquals("Goobi viewer TEST", DataManager.getInstance().getConfiguration().getName());
    }

    /**
     * @see Configuration#getDescription()
     * @verifies return correct value
     */
    @Test
    void getDescription_shouldReturnCorrectValue() throws Exception {
        assertEquals("Goobi viewer TEST desc", DataManager.getInstance().getConfiguration().getDescription());
    }

    /**
     * @see Configuration#getThumbnailsHeight()
     * @verifies return correct value
     */
    @Test
    void getThumbnailsHeight_shouldReturnCorrectValue() throws Exception {
        assertEquals(11, DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }

    /**
     * @see Configuration#getThumbnailsWidth()
     * @verifies return correct value
     */
    @Test
    void getThumbnailsWidth_shouldReturnCorrectValue() throws Exception {
        assertEquals(10, DataManager.getInstance().getConfiguration().getThumbnailsWidth());
    }

    /**
     * @see Configuration#getUnconditionalImageAccessMaxWidth()
     * @verifies return correct value
     */
    @Test
    void getThumbnailImageAccessMaxWidth_shouldReturnCorrectValue() throws Exception {
        assertEquals(1, DataManager.getInstance().getConfiguration().getThumbnailImageAccessMaxWidth());
    }

    @Test
    void getUnzoomedImageAccessMaxWidth_shouldReturnCorrectValue() throws Exception {
        assertEquals(2, DataManager.getInstance().getConfiguration().getUnzoomedImageAccessMaxWidth());
    }

    /**
     * @see Configuration#getViewerHome()
     * @verifies return correct value
     */
    @Test
    void getViewerHome_shouldReturnCorrectValue() throws Exception {
        assertEquals("src/test/resources/data/viewer/", DataManager.getInstance().getConfiguration().getViewerHome());
    }

    /**
     * @see Configuration#getViewerThumbnailsPerPage()
     * @verifies return correct value
     */
    @Test
    void getViewerThumbnailsPerPage_shouldReturnCorrectValue() throws Exception {
        assertEquals(9, DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage());
    }

    /**
     * @see Configuration#getWatermarkIdField()
     * @verifies return correct value
     */
    @Test
    void getWatermarkIdField_shouldReturnCorrectValue() throws Exception {
        assertEquals(Collections.singletonList("watermarkIdField_value"), DataManager.getInstance().getConfiguration().getWatermarkIdField());
    }

    /**
     * @see Configuration#isWatermarkTextConfigurationEnabled()
     * @verifies return correct value
     */
    @Test
    void isWatermarkTextConfigurationEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isWatermarkTextConfigurationEnabled());
    }

    /**
     * @see Configuration#getWatermarkTextConfiguration()
     * @verifies return all configured elements in the correct order
     */
    @Test
    void getWatermarkTextConfiguration_shouldReturnAllConfiguredElementsInTheCorrectOrder() throws Exception {
        assertEquals(3, DataManager.getInstance().getConfiguration().getWatermarkTextConfiguration().size());
    }

    /**
     * @see Configuration#getZoomFullscreenViewType()
     * @verifies return correct value
     */
    @Test
    void getZoomFullscreenViewType_shouldReturnCorrectValue() throws Exception {
        assertEquals("classic", DataManager.getInstance().getConfiguration().getZoomFullscreenViewType());
    }

    /**
     * @see Configuration#getZoomImageViewType()
     * @verifies return correct value
     */
    @Test
    void getZoomImageViewType_shouldReturnCorrectValue() throws Exception {
        assertEquals("openSeadragon", DataManager.getInstance().getConfiguration().getImageViewType());
    }

    /**
     * @see Configuration#isBookmarksEnabled()
     * @verifies return correct value
     */
    @Test
    void isBookshelvesEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isBookmarksEnabled());
    }

    /**
     * @see Configuration#isBrowsingMenuEnabled()
     * @verifies return correct value
     */
    @Test
    void isBrowsingMenuEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isBrowsingMenuEnabled());
    }

    /**
     * @see Configuration#isDisplaySearchResultNavigation()
     * @verifies return correct value
     */
    @Test
    void isDisplaySearchResultNavigation_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isDisplaySearchResultNavigation());
    }

    /**
     * @see Configuration#isDisplayStatistics()
     * @verifies return correct value
     */
    @Test
    void isDisplayStatistics_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayStatistics());
    }

    /**
     * @see Configuration#isDisplayTagCloudNavigation()
     * @verifies return correct value
     */
    @Test
    void isDisplayTagCloudNavigation_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayTagCloudNavigation());
    }

    /**
     * @see Configuration#isDisplayTagCloudStartpage()
     * @verifies return correct value
     */
    @Test
    void isDisplayTagCloudStartpage_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayTagCloudStartpage());
    }

    /**
     * @see Configuration#isDisplayUserNavigation()
     * @verifies return correct value
     */
    @Test
    void isDisplayUserNavigation_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayUserNavigation());
    }

    /**
     * @see Configuration#isMetadataPdfEnabled()
     * @verifies return correct value
     */
    @Test
    void isMetadataPdfEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isMetadataPdfEnabled());
    }

    /**
     * @see Configuration#isDisplaySidebarWidgetAdditionalFiles()
     * @verifies return correct value
     */
    @Test
    void isDisplaySidebarWidgetAdditionalFiles_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetAdditionalFiles());
    }

    @Test
    void getHideDownloadFileRegex_returnConfiguredValue() throws Exception {
        List<IFilterConfiguration> filters = DataManager.getInstance().getConfiguration().getAdditionalFilesDisplayFilters();
        assertEquals(2, filters.size());
        assertEquals("(wug_.*|AK_.*)", filters.get(0).getMatchRegex());
        assertEquals(1, filters.get(1).getFilterConditions().size());
    }

    /**
     * @see Configuration#isGeneratePdfInTaskManager()
     * @verifies return correct value
     */
    @Test
    void isGeneratePdfInTaskManager_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isGeneratePdfInTaskManager());
    }

    /**
     * @see Configuration#isPdfApiDisabled()
     * @verifies return correct value
     */
    @Test
    void isPdfApiDisabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isPdfApiDisabled());
    }

    /**
     * @see Configuration#isTitlePdfEnabled()
     * @verifies return correct value
     */
    @Test
    void isTitlePdfEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isTitlePdfEnabled());
    }

    /**
     * @see Configuration#isTocPdfEnabled()
     * @verifies return correct value
     */
    @Test
    void isTocPdfEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isTocPdfEnabled());
    }

    /**
     * @see Configuration#isPagePdfEnabled()
     * @verifies return correct value
     */
    @Test
    void isPagePdfEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isPagePdfEnabled());
    }

    /**
     * @see Configuration#isDocHierarchyPdfEnabled()
     * @verifies return correct value
     */
    @Test
    void isDocHierarchyPdfEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isDocHierarchyPdfEnabled());
    }

    /**
     * @see Configuration#isTitleEpubEnabled()
     * @verifies return correct value
     */
    @Test
    void isTitleEpubEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isTitleEpubEnabled());
    }

    /**
     * @see Configuration#isTocEpubEnabled()
     * @verifies return correct value
     */
    @Test
    void isTocEpubEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isTocEpubEnabled());
    }

    /**
     * @see Configuration#isMetadataEpubEnabled()
     * @verifies return correct value
     */
    @Test
    void isMetadataEpubEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isMetadataEpubEnabled());
    }

    /**
     * @see Configuration#getDownloadFolder(String)
     * @verifies return correct value for pdf
     */
    @Test
    void getDownloadFolder_shouldReturnCorrectValueForPdf() throws Exception {
        assertEquals("/opt/digiverso/viewer/download_test_pdf", DataManager.getInstance().getConfiguration().getDownloadFolder("pdf"));
    }

    /**
     * @see Configuration#getDownloadFolder(String)
     * @verifies return correct value for epub
     */
    @Test
    void getDownloadFolder_shouldReturnCorrectValueForEpub() throws Exception {
        assertEquals("/opt/digiverso/viewer/download_test_epub", DataManager.getInstance().getConfiguration().getDownloadFolder("epub"));
    }

    /**
     * @see Configuration#getDownloadFolder(String)
     * @verifies return empty string if type unknown
     */
    @Test
    void getDownloadFolder_shouldReturnEmptyStringIfTypeUnknown() throws Exception {
        assertEquals("", DataManager.getInstance().getConfiguration().getDownloadFolder("xxx"));
    }

    /**
     * @see Configuration#isPreventProxyCaching()
     * @verifies return correct value
     */
    @Test
    void isPreventProxyCaching_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isPreventProxyCaching());
    }

    /**
     * @see Configuration#isSolrCompressionEnabled()
     * @verifies return correct value
     */
    @Test
    void isSolrCompressionEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isSolrCompressionEnabled());
    }

    /**
     * @see Configuration#isSolrBackwardsCompatible()
     * @verifies return correct value
     */
    @Test
    void isSolrBackwardsCompatible_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isSolrBackwardsCompatible());
    }

    /**
     * @see Configuration#isSidebarFulltextLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarFulltextLinkVisible_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarFulltextLinkVisible());
    }

    /**
     * @see Configuration#isSidebarMetadataViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarMetadataLinkVisible_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarMetadataViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarPageViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarPageLinkVisible_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarPageViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarCalendarViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarCalendarLinkVisible_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarCalendarViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarThumbsViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarThumbsLinkVisible_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarThumbsViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarOpacLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarOpacLinkVisible_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isSidebarOpacLinkVisible());
    }

    /**
     * @see Configuration#isSidebarTocViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarTocLinkVisible_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarTocViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarTocWidgetVisible()
     * @verifies return correct value
     */
    @Test
    void isSidebarTocVisible_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarTocWidgetVisible());
    }

    /**
     * @see Configuration#isSidebarTocWidgetVisibleInFullscreen()
     * @verifies return correct value
     */
    @Test
    void isSidebarTocVisibleInFullscreen_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarTocWidgetVisibleInFullscreen());
    }

    /**
     * @see Configuration#isSortingEnabled()
     * @verifies return correct value
     */
    @Test
    void isSortingEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isSortingEnabled());
    }

    /**
     * @see Configuration#getDefaultSortField()
     * @verifies return correct value
     */
    @Test
    void getDefaultSortField_shouldReturnCorrectValue() throws Exception {
        assertEquals("SORT_TITLE_LANG_DE", DataManager.getInstance().getConfiguration().getDefaultSortField(null));
    }

    /**
     * @see Configuration#getDefaultSortField(String)
     * @verifies return correct language value
     */
    @Test
    void getDefaultSortField_shouldReturnCorrectLanguageValue() throws Exception {
        assertEquals("SORT_TITLE_LANG_EN", DataManager.getInstance().getConfiguration().getDefaultSortField("en"));
    }

    /**
     * @see Configuration#getSearchSortingOptions()
     * @verifies place default sorting field on top
     */
    @Test
    void getSearchSortingOptions_shouldPlaceDefaultSortingFieldOnTop() throws Exception {
        List<SearchSortingOption> result = DataManager.getInstance().getConfiguration().getSearchSortingOptions(null);
        assertEquals(TestUtils.NUM_ALL_SEARCH_SORTING_OPTIONS, result.size());
        assertEquals("SORT_TITLE_LANG_DE", result.get(0).getField());
    }

    /**
     * @see Configuration#getSearchSortingOptions()
     * @verifies handle descending configurations correctly
     */
    @Test
    void getSearchSortingOptions_shouldHandleDescendingConfigurationsCorrectly() throws Exception {
        List<SearchSortingOption> result = DataManager.getInstance().getConfiguration().getSearchSortingOptions(null);
        assertEquals(TestUtils.NUM_ALL_SEARCH_SORTING_OPTIONS, result.size());
        assertEquals(SolrConstants.DATECREATED, result.get(8).getField());
        assertEquals(SolrConstants.DATECREATED, result.get(9).getField());
    }

    /**
     * @see Configuration#getSearchSortingOptions()
     * @verifies ignore secondary fields from default config
     */
    @Test
    void getSearchSortingOptions_shouldIgnoreSecondaryFieldsFromDefaultConfig() throws Exception {
        List<SearchSortingOption> result = DataManager.getInstance().getConfiguration().getSearchSortingOptions(null);
        assertEquals(TestUtils.NUM_ALL_SEARCH_SORTING_OPTIONS, result.size());
        assertEquals("SORT_YEARPUBLISH", result.get(10).getField());
        assertEquals("SORT_YEARPUBLISH", result.get(11).getField());
    }

    /**
     * @see Configuration#getSearchSortingOptions(String)
     * @verifies ignore fields with mismatched language
     */
    @Test
    void getSearchSortingOptions_shouldIgnoreFieldsWithMismatchedLanguage() throws Exception {
        List<SearchSortingOption> result = DataManager.getInstance().getConfiguration().getSearchSortingOptions("en");
        assertEquals(TestUtils.NUM_ALL_SEARCH_SORTING_OPTIONS - 2, result.size());
        assertEquals("SORT_YEARPUBLISH", result.get(8).getField());
        assertEquals("SORT_YEARPUBLISH", result.get(9).getField());
    }

    /**
     * @see Configuration#getUrnResolverFields()
     * @verifies return all configured elements
     */
    @Test
    void getUrnResolverFields_shouldReturnAllConfiguredElements() throws Exception {
        assertEquals(3, DataManager.getInstance().getConfiguration().getUrnResolverFields().size());
    }

    /**
     * @see Configuration#isUrnDoRedirect()
     * @verifies return correct value
     */
    @Test
    void isUrnDoRedirect_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isUrnDoRedirect());
    }

    /**
     * @see Configuration#useTiles()
     * @verifies return correct value
     */
    @Test
    void useTiles_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().useTiles());
    }

    /**
     * @see Configuration#useTilesFullscreen()
     * @verifies return correct value
     */
    @Test
    void useTilesFullscreen_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().useTilesFullscreen());
    }

    /**
     * @see Configuration#getPageType(PageType)
     * @verifies return null for non configured type
     */
    @Test
    void getPageType_shouldReturnNullForNonConfiguredType() throws Exception {
        Assertions.assertNull(DataManager.getInstance().getConfiguration().getPageType(PageType.term));
    }

    /**
     * @see Configuration#getSidebarMetadataForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    void getSidebarMetadataForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        assertEquals(5, DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate("Map").size());
    }

    /**
     * @see Configuration#getSortFields()
     * @verifies return return all configured elements
     */
    @Test
    void getSortFields_shouldReturnReturnAllConfiguredElements() throws Exception {
        List<String> fields = DataManager.getInstance().getConfiguration().getSortFields();
        assertEquals(7, fields.size());
        assertEquals("!" + SolrConstants.DATECREATED, fields.get(5));
        assertEquals("SORT_YEARPUBLISH;SORT_TITLE", fields.get(6));
    }

    /**
     * @see Configuration#getStaticSortFields()
     * @verifies return return all configured elements
     */
    @Test
    void getStaticSortFields_shouldReturnReturnAllConfiguredElements() throws Exception {
        assertEquals(1, DataManager.getInstance().getConfiguration().getStaticSortFields().size());
    }

    /**
     * @see Configuration#getViewerMaxImageHeight()
     * @verifies return correct value
     */
    @Test
    void getViewerMaxImageHeight_shouldReturnCorrectValue() throws Exception {
        assertEquals(7, DataManager.getInstance().getConfiguration().getViewerMaxImageHeight());
    }

    /**
     * @see Configuration#getViewerMaxImageScale()
     * @verifies return correct value
     */
    @Test
    void getViewerMaxImageScale_shouldReturnCorrectValue() throws Exception {
        assertEquals(8, DataManager.getInstance().getConfiguration().getViewerMaxImageScale());
    }

    /**
     * @see Configuration#getViewerMaxImageWidth()
     * @verifies return correct value
     */
    @Test
    void getViewerMaxImageWidth_shouldReturnCorrectValue() throws Exception {
        assertEquals(6, DataManager.getInstance().getConfiguration().getViewerMaxImageWidth());
    }

    /**
     * @see Configuration#getSidebarMetadataForTemplate(String)
     * @verifies return empty list if template not found
     */
    @Test
    void getSidebarMetadataForTemplate_shouldReturnEmptyListIfTemplateNotFound() throws Exception {
        assertEquals(0, DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate("nonexistant").size());
    }

    /**
     * @see Configuration#getSidebarMetadataForTemplate(String)
     * @verifies return empty list if template is null
     */
    @Test
    void getSidebarMetadataForTemplate_shouldReturnEmptyListIfTemplateIsNull() throws Exception {
        assertEquals(0, DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(null).size());
    }

    /**
     * @see Configuration#getNormdataFieldsForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    void getNormdataFieldsForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        assertEquals(2, DataManager.getInstance().getConfiguration().getNormdataFieldsForTemplate("CORPORATION").size());
    }

    /**
     * @see Configuration#getAdvancedSearchFields()
     * @verifies return all values
     */
    @Test
    void getAdvancedSearchFields_shouldReturnAllValues() throws Exception {
        List<AdvancedSearchFieldConfiguration> result =
                DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, Locale.ENGLISH.getLanguage());
        assertEquals(10, result.size());
        assertTrue(result.get(0).isHierarchical());
        assertTrue(result.get(0).isVisible());
        assertTrue(result.get(1).isUntokenizeForPhraseSearch());
        assertTrue(result.get(5).isRange());
        assertEquals("#SEPARATOR1#", result.get(7).getField());
        assertEquals("-----", result.get(7).getLabel());
        assertTrue(result.get(7).isDisabled());
        assertEquals(20, result.get(9).getDisplaySelectItemsThreshold());
        assertEquals(AdvancedSearchFieldConfiguration.SELECT_TYPE_BADGES, result.get(9).getSelectType());
    }

    /**
     * @see Configuration#getAdvancedSearchFields(String,boolean,String)
     * @verifies return skip fields that don't match given language
     */
    @Test
    void getAdvancedSearchFields_shouldReturnSkipFieldsThatDontMatchGivenLanguage() throws Exception {
        List<AdvancedSearchFieldConfiguration> result =
                DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "en");
        assertEquals(10, result.size());

        assertEquals("MD_FOO_LANG_EN", result.get(8).getField());
        assertEquals("MD_FOO_LANG_DE",
                DataManager.getInstance().getConfiguration().getAdvancedSearchFields(null, true, "de").get(8).getField());
    }

    /**
     * @see Configuration#isAdvancedSearchFieldHierarchical(String)
     * @verifies return correct value
     */
    @Test
    void isAdvancedSearchFieldHierarchical_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldHierarchical(SolrConstants.DC, null, true));
    }

    /**
     * @see Configuration#isAdvancedSearchFieldRange(String)
     * @verifies return correct value
     */
    @Test
    void isAdvancedSearchFieldRange_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldRange(SolrConstants.DC, null, true));
        assertTrue(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldRange("MD_YEARPUBLISH", null, true));
    }

    /**
     * @see Configuration#isAdvancedSearchFieldUntokenizeForPhraseSearch(String)
     * @verifies return correct value
     */
    @Test
    void isAdvancedSearchFieldUntokenizeForPhraseSearch_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldUntokenizeForPhraseSearch(SolrConstants.DC, null, true));
        assertTrue(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldUntokenizeForPhraseSearch("MD_TITLE", null, true));
    }

    /**
     * @see Configuration#getAdvancedSearchFieldSeparatorLabel(String)
     * @verifies return correct value
     */
    @Test
    void getAdvancedSearchFieldSeparatorLabel_shouldReturnCorrectValue() throws Exception {
        assertEquals("-----", DataManager.getInstance().getConfiguration().getAdvancedSearchFieldSeparatorLabel("#SEPARATOR1#", null, true));
    }

    /**
     * @see Configuration#getAdvancedSearchFieldDisplaySelectItemsThreshold(String,String,boolean)
     * @verifies return correct value
     */
    @Test
    void getAdvancedSearchFieldDisplaySelectItemsThreshold_shouldReturnCorrectValue() throws Exception {
        assertEquals(20,
                DataManager.getInstance()
                        .getConfiguration()
                        .getAdvancedSearchFieldDisplaySelectItemsThreshold(SolrConstants.DOCSTRCT, StringConstants.DEFAULT_NAME, false));
    }

    /**
     * @see Configuration#getAdvancedSearchFieldSelectType(String,String,boolean)
     * @verifies return correct value
     */
    @Test
    void getAdvancedSearchFieldSelectType_shouldReturnCorrectValue() throws Exception {
        assertEquals(AdvancedSearchFieldConfiguration.SELECT_TYPE_BADGES,
                DataManager.getInstance()
                        .getConfiguration()
                        .getAdvancedSearchFieldSelectType(SolrConstants.DOCSTRCT, StringConstants.DEFAULT_NAME, false));
    }

    /**
     * @see Configuration#getSidebarTocCollapseLengthThreshold()
     * @verifies return correct value
     */
    @Test
    void getSidebarTocCollapseLengthThreshold_shouldReturnCorrectValue() throws Exception {
        assertEquals(141, DataManager.getInstance().getConfiguration().getSidebarTocCollapseLengthThreshold());
    }

    /**
     * @see Configuration#getSidebarTocLowestLevelToCollapseForLength()
     * @verifies return correct value
     */
    @Test
    void getSidebarTocLowestLevelToCollapseForLength_shouldReturnCorrectValue() throws Exception {
        assertEquals(333, DataManager.getInstance().getConfiguration().getSidebarTocLowestLevelToCollapseForLength());
    }

    /**
     * @see Configuration#getDisplayTitleBreadcrumbs()
     * @verifies return correct value
     */
    @Test
    void getDisplayTitleBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().getDisplayTitleBreadcrumbs());
    }

    /**
     * @see Configuration#getIncludeAnchorInTitleBreadcrumbs()
     * @verifies return correct value
     */
    @Test
    void getIncludeAnchorInTitleBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().getIncludeAnchorInTitleBreadcrumbs());
    }

    /**
     * @see Configuration#getTitleBreadcrumbsMaxTitleLength()
     * @verifies return correct value
     */
    @Test
    void getTitleBreadcrumbsMaxTitleLength_shouldReturnCorrectValue() throws Exception {
        assertEquals(20, DataManager.getInstance().getConfiguration().getTitleBreadcrumbsMaxTitleLength());
    }

    /**
     * @see Configuration#isDisplaySearchRssLinks()
     * @verifies return correct value
     */
    @Test
    void isDisplaySearchRssLinks_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplaySearchRssLinks());
    }

    /**
     * @see Configuration#getCalendarDocStructTypes()
     * @verifies return all configured elements
     */
    @Test
    void getCalendarDocStructTypes_shouldReturnAllConfiguredElements() throws Exception {
        assertEquals(2, DataManager.getInstance().getConfiguration().getCalendarDocStructTypes().size());
    }

    /**
     * @see Configuration#getAllFacetFields()
     * @verifies return correct order
     */
    @Test
    void getAllFacetFields_shouldReturnCorrectOrder() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getAllFacetFields();
        assertEquals(6, result.size());
        assertEquals("DC", result.get(0));
        assertEquals("YEAR", result.get(1));
        assertEquals("MD_CREATOR", result.get(2));
        assertEquals("MD_PLACEPUBLISH", result.get(3));
        assertEquals("WKT_COORDS", result.get(4));
        assertEquals("MD_PERSON", result.get(5));
    }

    /**
     * @see Configuration#getHierarchicalFacetFields()
     * @verifies return all values
     */
    @Test
    void getHierarchicalFacetFields_shouldReturnAllValues() throws Exception {
        assertEquals(2, DataManager.getInstance().getConfiguration().getHierarchicalFacetFields().size());
    }

    /**
     * @see Configuration#getRangeFacetFields()
     * @verifies return all values
     */
    @Test
    void getRangeFacetFields_shouldReturnAllValues() throws Exception {
        assertEquals(1, DataManager.getInstance().getConfiguration().getRangeFacetFields().size());
    }

    /**
     * @see Configuration#getRangeFacetFieldMinValue()
     * @verifies return correct value
     */
    @Test
    void getRangeFacetFieldMinValue_shouldReturnCorrectValue() throws Exception {
        assertEquals(-1000, DataManager.getInstance().getConfiguration().getRangeFacetFieldMinValue(SolrConstants.YEAR));
    }

    /**
     * @see Configuration#getRangeFacetFieldMinValue()
     * @verifies return INT_MIN if no value configured
     */
    @Test
    void getRangeFacetFieldMinValue_shouldReturnINT_MINIfNoValueConfigured() throws Exception {
        assertEquals(Integer.MIN_VALUE, DataManager.getInstance().getConfiguration().getRangeFacetFieldMinValue("MD_NOSUCHFIELD"));
    }

    /**
     * @see Configuration#getRangeFacetFieldMaxValue()
     * @verifies return correct value
     */
    @Test
    void getRangeFacetFieldMaxValue_shouldReturnCorrectValue() throws Exception {
        assertEquals(2050, DataManager.getInstance().getConfiguration().getRangeFacetFieldMaxValue(SolrConstants.YEAR));
    }

    /**
     * @see Configuration#getRangeFacetFieldMaxValue()
     * @verifies return INT_MAX if no value configured
     */
    @Test
    void getRangeFacetFieldMaxValue_shouldReturnINT_MAXIfNoValueConfigured() throws Exception {
        assertEquals(Integer.MAX_VALUE, DataManager.getInstance().getConfiguration().getRangeFacetFieldMaxValue("MD_NOSUCHFIELD"));
    }

    /**
     * @see Configuration#getInitialFacetElementNumber()
     * @verifies return correct value
     */
    @Test
    void getInitialFacetElementNumber_shouldReturnCorrectValue() throws Exception {
        assertEquals(4, DataManager.getInstance().getConfiguration().getInitialFacetElementNumber(SolrConstants.DC));
        assertEquals(16, DataManager.getInstance().getConfiguration().getInitialFacetElementNumber("MD_PLACEPUBLISH"));
        assertEquals(23, DataManager.getInstance().getConfiguration().getInitialFacetElementNumber(null));
    }

    /**
     * @see Configuration#getInitialFacetElementNumber(String)
     * @verifies return default value if field not found
     */
    @Test
    void getInitialFacetElementNumber_shouldReturnDefaultValueIfFieldNotFound() throws Exception {
        assertEquals(-1, DataManager.getInstance().getConfiguration().getInitialFacetElementNumber("YEAR"));
    }

    /**
     * @see Configuration#getFacetFieldStyle(String)
     * @verifies return correct value
     */
    @Test
    void getFacetFieldStyle_shouldReturnCorrectValue() throws Exception {
        assertEquals("graph", DataManager.getInstance().getConfiguration().getFacetFieldStyle(SolrConstants.YEAR));
    }

    /**
     * @see Configuration#getPriorityValuesForFacetField(String)
     * @verifies return return all configured elements for regular fields
     */
    @Test
    void getPriorityValuesForFacetField_shouldReturnReturnAllConfiguredElementsForRegularFields() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getPriorityValuesForFacetField("MD_PLACEPUBLISH");
        Assertions.assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("val1", result.get(0));
        assertEquals("val2", result.get(1));
        assertEquals("val3", result.get(2));
    }

    /**
     * @see Configuration#getPriorityValuesForFacetField(String)
     * @verifies return return all configured elements for hierarchical fields
     */
    @Test
    void getPriorityValuesForFacetField_shouldReturnReturnAllConfiguredElementsForHierarchicalFields() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getPriorityValuesForFacetField("DC");
        Assertions.assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("collection2", result.get(0));
        assertEquals("collection1", result.get(1));
    }

    /**
     * @see Configuration#getLabelFieldForFacetField(String)
     * @verifies return correct value
     */
    @Test
    void getLabelFieldForFacetField_shouldReturnCorrectValue() throws Exception {
        assertEquals("MD_FIELDLABEL", DataManager.getInstance().getConfiguration().getLabelFieldForFacetField(SolrConstants.YEAR));
        assertEquals("MD_FIRSTNAME", DataManager.getInstance().getConfiguration().getLabelFieldForFacetField("MD_CREATOR"));
    }

    /**
     * @see Configuration#getLabelFieldForFacetField(String)
     * @verifies return null if no value found
     */
    @Test
    void getLabelFieldForFacetField_shouldReturnNullIfNoValueFound() throws Exception {
        Assertions.assertNull(DataManager.getInstance().getConfiguration().getLabelFieldForFacetField("MD_PLACEPUBLISH"));
    }

    /**
     * @see Configuration#isTranslateFacetFieldLabels(String)
     * @verifies return correct value
     */
    @Test
    void isTranslateFacetFieldLabels_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels("YEAR"));
        assertFalse(DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels("MD_CREATOR"));
        assertTrue(DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels("MD_PLACEPUBLISH"));
    }

    /**
     * @see Configuration#getGroupToLengthForFacetField(String)
     * @verifies return correct value
     */
    @Test
    void getGroupToLengthForFacetField_shouldReturnCorrectValue() throws Exception {
        assertEquals(1, DataManager.getInstance().getConfiguration().getGroupToLengthForFacetField("MD_PERSON"));
    }

    /**
     * @see Configuration#isAlwaysApplyFacetFieldToUnfilteredHits(String)
     * @verifies return correct value
     */
    @Test
    void isAlwaysApplyFacetFieldToUnfilteredHits_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isAlwaysApplyFacetFieldToUnfilteredHits("MD_PERSON"));
    }

    /**
     * @see Configuration#isFacetFieldSkipInWidget(String)
     * @verifies return correct value
     */
    @Test
    void isFacetFieldSkipInWidget_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isFacetFieldSkipInWidget("MD_PERSON"));
    }

    /**
     * @see Configuration#isFacetFieldDisplayValueFilter(String)
     * @verifies return correct value
     */
    @Test
    void isFacetFieldDisplayValueFilter_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isFacetFieldDisplayValueFilter("MD_PLACEPUBLISH"));
        assertTrue(DataManager.getInstance().getConfiguration().isFacetFieldDisplayValueFilter("MD_CREATOR"));
    }

    /**
     * @see Configuration#getGeoFacetFieldPredicate()
     * @verifies return correct value
     */
    @Test
    void getGeoFacetFieldPredicate_shouldReturnCorrectValue() throws Exception {
        assertEquals("ISWITHIN", DataManager.getInstance().getConfiguration().getGeoFacetFieldPredicate("WKT_COORDS"));
    }

    /**
     * @see Configuration#isShowSearchHitsInGeoFacetMap()
     * @verifies return correct value
     */
    @Test
    void isShowSearchHitsInGeoFacetMap_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isShowSearchHitsInGeoFacetMap("WKT_COORDS"));
    }

    @Test
    void getSortOrderTest() {
        assertEquals("numerical", DataManager.getInstance().getConfiguration().getSortOrder("YEAR"));
        assertEquals("default", DataManager.getInstance().getConfiguration().getSortOrder("MD_PLACEPUBLISH"));
        assertEquals("alphabetical", DataManager.getInstance().getConfiguration().getSortOrder("MD_CREATOR"));
    }

    /**
     * @see Configuration#isAdvancedSearchEnabled()
     * @verifies return correct value
     */
    @Test
    void isAdvancedSearchEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isAdvancedSearchEnabled());
    }

    /**
     * @see Configuration#isCalendarSearchEnabled()
     * @verifies return correct value
     */
    @Test
    void isCalendarSearchEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isCalendarSearchEnabled());
    }

    /**
     * @see Configuration#isTimelineSearchEnabled()
     * @verifies return correct value
     */
    @Test
    void isTimelineSearchEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isTimelineSearchEnabled());
    }

    /**
     * @see Configuration#getStaticQuerySuffix()
     * @verifies return correct value
     */
    @Test
    void getStaticQuerySuffix_shouldReturnCorrectValue() throws Exception {
        assertEquals("-BOOL_HIDE:true", DataManager.getInstance().getConfiguration().getStaticQuerySuffix());
    }

    /**
     * @see Configuration#getNextVersionIdentifierField()
     * @verifies return correct value
     */
    @Test
    void getNextVersionIdentifierField_shouldReturnCorrectValue() throws Exception {
        assertEquals("MD_PREVIOUS_VERSION", DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField());
    }

    /**
     * @see Configuration#getPreviousVersionIdentifierField()
     * @verifies return correct value
     */
    @Test
    void getPreviousVersionIdentifierField_shouldReturnCorrectValue() throws Exception {
        assertEquals("MD_NEXT_VERSION", DataManager.getInstance().getConfiguration().getNextVersionIdentifierField());
    }

    /**
     * @see Configuration#getVersionLabelField()
     * @verifies return correct value
     */
    @Test
    void getVersionLabelField_shouldReturnCorrectValue() throws Exception {
        assertEquals("MD_VERSIONLABEL", DataManager.getInstance().getConfiguration().getVersionLabelField());
    }

    /**
     * @see Configuration#getCmsTextFolder()
     * @verifies return correct value
     */
    @Test
    void getCmsTextFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("cms", DataManager.getInstance().getConfiguration().getCmsTextFolder());
    }

    /**
     * @see Configuration#getAltoFolder()
     * @verifies return correct value
     */
    @Test
    void getAltoFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("alto", DataManager.getInstance().getConfiguration().getAltoFolder());
    }

    /**
     * @see Configuration#getAltoCrowdsourcingFolder()
     * @verifies return correct value
     */
    @Test
    void getAltoCrowdsourcingFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("alto_crowd", DataManager.getInstance().getConfiguration().getAltoCrowdsourcingFolder());
    }

    /**
     * @see Configuration#getFulltextFolder()
     * @verifies return correct value
     */
    @Test
    void getFulltextFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("fulltext", DataManager.getInstance().getConfiguration().getFulltextFolder());
    }

    /**
     * @see Configuration#getFulltextCrowdsourcingFolder()
     * @verifies return correct value
     */
    @Test
    void getFulltextCrowdsourcingFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("fulltext_crowd", DataManager.getInstance().getConfiguration().getFulltextCrowdsourcingFolder());
    }

    /**
     * @see Configuration#getAbbyyFolder()
     * @verifies return correct value
     */
    @Test
    void getAbbyyFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("abbyy", DataManager.getInstance().getConfiguration().getAbbyyFolder());
    }

    /**
     * @see Configuration#getTeiFolder()
     * @verifies return correct value
     */
    @Test
    void getTeiFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("tei", DataManager.getInstance().getConfiguration().getTeiFolder());
    }

    /**
     * @see Configuration#getCmdiFolder()
     * @verifies return correct value
     */
    @Test
    void getCmdiFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("cmdi", DataManager.getInstance().getConfiguration().getCmdiFolder());
    }

    /**
     * @see Configuration#getAnnotationFolder()
     * @verifies return correct value
     */
    @Test
    void getAnnotationFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("annotations", DataManager.getInstance().getConfiguration().getAnnotationFolder());
    }

    /**
     * @see Configuration#getEndYearForTimeline()
     * @verifies return correct value
     */
    @Test
    void getEndYearForTimeline_shouldReturnCorrectValue() throws Exception {
        assertEquals("1865", DataManager.getInstance().getConfiguration().getEndYearForTimeline());
    }

    /**
     * @see Configuration#getStartYearForTimeline()
     * @verifies return correct value
     */
    @Test
    void getStartYearForTimeline_shouldReturnCorrectValue() throws Exception {
        assertEquals("1861", DataManager.getInstance().getConfiguration().getStartYearForTimeline());
    }

    /**
     * @see Configuration#getTimelineHits()
     * @verifies return correct value
     */
    @Test
    void getTimelineHits_shouldReturnCorrectValue() throws Exception {
        assertEquals("120", DataManager.getInstance().getConfiguration().getTimelineHits());
    }

    /**
     * @see Configuration#isDisplayTimeMatrix()
     * @verifies return correct value
     */
    @Test
    void isDisplayTimeMatrix_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isDisplayTimeMatrix());
    }

    /**
     * @see Configuration#getPiwikBaseURL()
     * @verifies return correct value
     */
    @Test
    void getPiwikBaseURL_shouldReturnCorrectValue() throws Exception {
        assertEquals("baseURL_value", DataManager.getInstance().getConfiguration().getPiwikBaseURL());
    }

    /**
     * @see Configuration#getPiwikSiteID()
     * @verifies return correct value
     */
    @Test
    void getPiwikSiteID_shouldReturnCorrectValue() throws Exception {
        assertEquals("siteID_value", DataManager.getInstance().getConfiguration().getPiwikSiteID());
    }

    /**
     * @see Configuration#isPiwikTrackingEnabled()
     * @verifies return correct value
     */
    @Test
    void isPiwikTrackingEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isPiwikTrackingEnabled());
    }

    /**
     * @see Configuration#getSearchFilters()
     * @verifies return all configured elements
     */
    @Test
    void getSearchFilters_shouldReturnAllConfiguredElements() throws Exception {
        List<SearchFilter> result = DataManager.getInstance().getConfiguration().getSearchFilters();
        assertEquals(6, result.size());
        assertEquals("filter_ALL", result.get(0).getLabel());
        assertEquals("ALL", result.get(0).getField());
        assertTrue(result.get(0).isDefaultFilter());
    }

    /**
     * @see Configuration#getAnchorThumbnailMode()
     * @verifies return correct value
     */
    @Test
    void getAnchorThumbnailMode_shouldReturnCorrectValue() throws Exception {
        assertEquals("FIRSTVOLUME", DataManager.getInstance().getConfiguration().getAnchorThumbnailMode());
    }

    /**
     * @see Configuration#isDisplayCollectionBrowsing()
     * @verifies return correct value
     */
    @Test
    void isDisplayCollectionBrowsing_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayCollectionBrowsing());
    }

    /**
     * @see Configuration#getDisplayTitlePURL()
     * @verifies return correct value
     */
    @Test
    void getDisplayTitlePURL_shouldReturnCorrectValue() throws Exception {
        assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayTitlePURL());
    }

    /**
     * @see Configuration#getWebApiFields()
     * @verifies return all configured elements
     */
    @Test
    void getWebApiFields_shouldReturnAllConfiguredElements() throws Exception {
        List<Map<String, String>> fields = DataManager.getInstance().getConfiguration().getWebApiFields();
        assertEquals(2, fields.size());
        assertEquals("json1", fields.get(0).get("jsonField"));
        assertEquals("lucene1", fields.get(0).get("luceneField"));
        assertEquals("true", fields.get(0).get("multivalue"));
        assertEquals(null, fields.get(1).get("multivalue"));
    }

    /**
     * @see Configuration#getDbPersistenceUnit()
     * @verifies return correct value
     */
    @Test
    void getDbPersistenceUnit_shouldReturnCorrectValue() throws Exception {
        assertEquals("intranda_viewer_test", DataManager.getInstance().getConfiguration().getDbPersistenceUnit());
    }

    /**
     * @see Configuration#getCmsMediaFolder()
     * @verifies return correct value
     */
    @Test
    void getCmsMediaFolder_shouldReturnCorrectValue() throws Exception {
        assertEquals("cmsMediaFolder_value", DataManager.getInstance().getConfiguration().getCmsMediaFolder());
    }

    @Test
    void getCmsMediaDisplayWidthTest() {
        assertEquals(600, DataManager.getInstance().getConfiguration().getCmsMediaDisplayWidth());
    }

    @Test
    void getCmsMediaDisplaHeightTest() {
        assertEquals(800, DataManager.getInstance().getConfiguration().getCmsMediaDisplayHeight());
    }

    /**
     * @see Configuration#isSearchSavingEnabled()
     * @verifies return correct value
     */
    @Test
    void isSearchSavingEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isSearchSavingEnabled());
    }

    @Test
    void getImageViewZoomScalesTest() throws ViewerConfigurationException {
        List<String> scales = DataManager.getInstance().getConfiguration().getImageViewZoomScales();
        assertEquals("600", scales.get(0));
        assertEquals("1000", scales.get(1));
        assertEquals("1500", scales.get(2));
        assertEquals("3000", scales.get(3));
    }

    @Test
    void getFullscreenViewZoomScalesTest() throws ViewerConfigurationException {
        List<String> scales = DataManager.getInstance().getConfiguration().getImageViewZoomScales(PageType.viewFullscreen, null);
        assertEquals("1000", scales.get(0));
        assertEquals("2000", scales.get(1));
        assertEquals("3000", scales.get(2));
    }

    @Test
    void getImageViewTileSizesTest() throws ViewerConfigurationException {
        Map<Integer, List<Integer>> tiles = DataManager.getInstance().getConfiguration().getTileSizes();
        assertEquals(512, tiles.keySet().iterator().next(), 0);
        assertEquals(3, tiles.get(512).size());
        assertEquals(1, tiles.get(512).get(0), 0);
        assertEquals(2, tiles.get(512).get(1), 0);
        assertEquals(3, tiles.get(512).get(2), 0);
    }

    @Test
    void getFullscreenTileSizesTest() throws ViewerConfigurationException {
        Map<Integer, List<Integer>> tiles = DataManager.getInstance().getConfiguration().getTileSizes(PageType.viewFullscreen, null);
        assertEquals(1024, tiles.keySet().iterator().next(), 0);
        assertEquals(3, tiles.get(1024).size());
        assertEquals(2, tiles.get(1024).get(0), 0);
        assertEquals(4, tiles.get(1024).get(1), 0);
        assertEquals(8, tiles.get(1024).get(2), 0);
    }

    @Test
    void getFooterHeightTest() throws ViewerConfigurationException {
        assertEquals(50, DataManager.getInstance().getConfiguration().getFooterHeight());
    }

    @Test
    void getCrowdsourcingFooterHeightTest() throws ViewerConfigurationException {
        assertEquals(0, DataManager.getInstance().getConfiguration().getFooterHeight(PageType.editContent, null));
    }

    /**
     * @see Configuration#getUrnResolverUrl()
     * @verifies return correct value
     */
    @Test
    void getUrnResolverUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("urnResolver_value", DataManager.getInstance().getConfiguration().getUrnResolverUrl());
    }

    /**
     * @see Configuration#getTocVolumeSortFieldsForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    void getTocVolumeSortFieldsForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        List<StringPair> fields = DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate("CustomDocstruct");
        assertEquals(2, fields.size());
        assertEquals("CURRENTNOSORT", fields.get(0).getOne());
        assertEquals("desc", fields.get(0).getTwo());
        assertEquals("SORT_TITLE", fields.get(1).getOne());
        assertEquals("asc", fields.get(1).getTwo());
    }

    /**
     * @see Configuration#getTocVolumeSortFieldsForTemplate(String)
     * @verifies return default template configuration if template not found
     */
    @Test
    void getTocVolumeSortFieldsForTemplate_shouldReturnDefaultTemplateConfigurationIfTemplateNotFound() throws Exception {
        List<StringPair> fields = DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate("notfound");
        assertEquals(1, fields.size());
        assertEquals("CURRENTNOSORT", fields.get(0).getOne());
        assertEquals("asc", fields.get(0).getTwo());
    }

    /**
     * @see Configuration#getTocVolumeSortFieldsForTemplate(String)
     * @verifies return default template configuration if template is null
     */
    @Test
    void getTocVolumeSortFieldsForTemplate_shouldReturnDefaultTemplateConfigurationIfTemplateIsNull() throws Exception {
        List<StringPair> fields = DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate(null);
        assertEquals(1, fields.size());
        assertEquals("CURRENTNOSORT", fields.get(0).getOne());
        assertEquals("asc", fields.get(0).getTwo());
    }

    /**
     * @see Configuration#getTocVolumeGroupFieldForTemplate(String)
     * @verifies return correct value
     */
    @Test
    void getTocVolumeGroupFieldForTemplate_shouldReturnCorrectValue() throws Exception {
        assertEquals("GROUP", DataManager.getInstance().getConfiguration().getTocVolumeGroupFieldForTemplate("CustomDocstruct"));
    }

    /**
     * @see Configuration#getRecordGroupIdentifierFields()
     * @verifies return all configured values
     */
    @Test
    void getRecordGroupIdentifierFields_shouldReturnAllConfiguredValues() throws Exception {
        assertEquals(2, DataManager.getInstance().getConfiguration().getRecordGroupIdentifierFields().size());
    }

    /**
     * @see Configuration#getAncestorIdentifierFields()
     * @verifies return all configured values
     */
    @Test
    void getAncestorIdentifierFields_shouldReturnAllConfiguredValues() throws Exception {
        List<String> list = DataManager.getInstance().getConfiguration().getAncestorIdentifierFields();
        Assertions.assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(SolrConstants.PI_PARENT, list.get(0));
    }

    /**
     * @see Configuration#isTocListSiblingRecords()
     * @verifies return correctValue
     */
    @Test
    void isTocListSiblingRecords_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isTocListSiblingRecords());
    }

    /**
     * @see Configuration#getTocAnchorGroupElementsPerPage()
     * @verifies return correct value
     */
    @Test
    void getTocAnchorGroupElementsPerPage_shouldReturnCorrectValue() throws Exception {
        assertEquals(10, DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage());
    }

    /**
     * @see Configuration#getCollectionDisplayDepthForSearch(String)
     * @verifies return correct value
     */
    @Test
    void getCollectionDisplayDepthForSearch_shouldReturnCorrectValue() throws Exception {
        assertEquals(5, DataManager.getInstance().getConfiguration().getCollectionDisplayDepthForSearch(SolrConstants.DC));
    }

    /**
     * @see Configuration#getCollectionDisplayDepthForSearch(String)
     * @verifies return -1 if no collection config was found
     */
    @Test
    void getCollectionDisplayDepthForSearch_shouldReturn1IfNoCollectionConfigWasFound() throws Exception {
        assertEquals(-1, DataManager.getInstance().getConfiguration().getCollectionDisplayDepthForSearch("MD_NOSUCHFIELD"));
    }

    /**
     * @see Configuration#getCollectionHierarchyField()
     * @verifies return first field where hierarchy enabled
     */
    @Test
    void getCollectionHierarchyField_shouldReturnFirstFieldWhereHierarchyEnabled() throws Exception {
        assertEquals("MD_KNOWLEDGEFIELD", DataManager.getInstance().getConfiguration().getCollectionHierarchyField());
    }

    /**
     * @see Configuration#isAddCollectionHierarchyToBreadcrumbs(String)
     * @verifies return correct value
     */
    @Test
    void isAddCollectionHierarchyToBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isAddCollectionHierarchyToBreadcrumbs("DC"));
        assertTrue(DataManager.getInstance().getConfiguration().isAddCollectionHierarchyToBreadcrumbs("MD_KNOWLEDGEFIELD"));
    }

    /**
     * @see Configuration#isAddCollectionHierarchyToBreadcrumbs(String)
     * @verifies return false if no collection config was found
     */
    @Test
    void isAddCollectionHierarchyToBreadcrumbs_shouldReturnFalseIfNoCollectionConfigWasFound() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isAddCollectionHierarchyToBreadcrumbs("MD_NOSUCHFIELD"));
    }

    @Test
    void testBrokenConfig() {
        DataManager.getInstance()
                .injectConfiguration(new Configuration(new File("src/test/resources/config_viewer_broken.test.xml").getAbsolutePath()));
        assertEquals("src/test/resources/localConfig/", DataManager.getInstance().getConfiguration().getConfigLocalPath());
        assertEquals("src/test/resources/data/viewer/", DataManager.getInstance().getConfiguration().getViewerHome());
        assertEquals("src/test/resources/data/viewer/data/", DataManager.getInstance().getConfiguration().getDataRepositoriesHome());

    }

    /**
     * @see Configuration#getTranskribusDefaultCollection()
     * @verifies return correct value
     */
    @Test
    void getTranskribusDefaultCollection_shouldReturnCorrectValue() throws Exception {
        assertEquals("intranda_viewer", DataManager.getInstance().getConfiguration().getTranskribusDefaultCollection());
    }

    /**
     * @see Configuration#getTranskribusAllowedDocumentTypes()
     * @verifies return all configured elements
     */
    @Test
    void getTranskribusAllowedDocumentTypes_shouldReturnAllConfiguredElements() throws Exception {
        assertEquals(2, DataManager.getInstance().getConfiguration().getTranskribusAllowedDocumentTypes().size());
    }

    /**
     * @see Configuration#getTranskribusRestApiUrl()
     * @verifies return correct value
     */
    @Test
    void getTranskribusRestApiUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("https://transkribus.eu/TrpServerTesting/rest/", DataManager.getInstance().getConfiguration().getTranskribusRestApiUrl());
    }

    /**
     * @see Configuration#isTranskribusEnabled()
     * @verifies return correct value
     */
    @Test
    void isTranskribusEnabled_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isTranskribusEnabled());
    }

    /**
     * @see Configuration#getRecordTargetPageType(String)
     * @verifies return correct value
     */
    @Test
    void getRecordTargetPageType_shouldReturnCorrectValue() throws Exception {
        assertEquals("toc", DataManager.getInstance().getConfiguration().getRecordTargetPageType("Catalogue"));
    }

    /**
     * @see Configuration#getRecordTargetPageType(String)
     * @verifies return null if docstruct not found
     */
    @Test
    void getRecordTargetPageType_shouldReturnNullIfDocstructNotFound() throws Exception {
        Assertions.assertNull(DataManager.getInstance().getConfiguration().getRecordTargetPageType("notfound"));
    }

    /**
     * @see Configuration#getFulltextPercentageWarningThreshold()
     * @verifies return correct value
     */
    @Test
    void getFulltextPercentageWarningThreshold_shouldReturnCorrectValue() throws Exception {
        assertEquals(99, DataManager.getInstance().getConfiguration().getFulltextPercentageWarningThreshold());
    }

    /**
     * @see Configuration#getFallbackDefaultLanguage()
     * @verifies return correct value
     */
    @Test
    void getFallbackDefaultLanguage_shouldReturnCorrectValue() throws Exception {
        assertEquals("de", DataManager.getInstance().getConfiguration().getFallbackDefaultLanguage());
    }

    /**
     * @see Configuration#getSearchExcelExportFields()
     * @verifies return all values
     */
    @Test
    void getSearchExcelExportFields_shouldReturnAllValues() throws Exception {
        List<ExportFieldConfiguration> result = DataManager.getInstance().getConfiguration().getSearchExcelExportFields();
        Assertions.assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(SolrConstants.PI, result.get(0).getField());
        assertEquals(SolrConstants.LABEL, result.get(1).getField());
    }

    /**
     * @see Configuration#isSearchExcelExportEnabled()
     * @verifies return correct value
     */
    @Test
    void isSearchExcelExportEnabled_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isSearchExcelExportEnabled());
    }

    /**
     * @see Configuration#isDisplayAdditionalMetadataEnabled()
     * @verifies return correct value
     */
    @Test
    void isDisplayAdditionalMetadataEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplayAdditionalMetadataEnabled());
    }

    /**
     * @see Configuration#getDisplayAdditionalMetadataIgnoreFields()
     * @verifies return correct values
     */
    @Test
    void getDisplayAdditionalMetadataIgnoreFields_shouldReturnCorrectValues() throws Exception {
        List<String> results = DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataIgnoreFields();
        Assertions.assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals(SolrConstants.ISANCHOR, results.get(0));
        assertEquals(SolrConstants.ISWORK, results.get(1));
        assertEquals(SolrConstants.PI_TOPSTRUCT, results.get(2));
    }

    /**
     * @see Configuration#getDisplayAdditionalMetadataTranslateFields()
     * @verifies return correct values
     */
    @Test
    void getDisplayAdditionalMetadataTranslateFields_shouldReturnCorrectValues() throws Exception {
        List<String> results = DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataTranslateFields();
        Assertions.assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals(SolrConstants.DC, results.get(0));
        assertEquals(SolrConstants.DOCSTRCT, results.get(1));
        assertEquals("MD_LANGUAGE", results.get(2));
    }

    /**
     * @see Configuration#getDisplayAdditionalMetadataOnelineFields()
     * @verifies return correct values
     */
    @Test
    void getDisplayAdditionalMetadataOnelineFields_shouldReturnCorrectValues() throws Exception {
        List<String> results = DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataOnelineFields();
        Assertions.assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("MD_ACCESSLOCATIONS", results.get(0));
    }

    /**
     * @see Configuration#getDisplayAdditionalMetadataSnippetFields()
     * @verifies return correct values
     */
    @Test
    void getDisplayAdditionalMetadataSnippetFields_shouldReturnCorrectValues() throws Exception {
        List<String> results = DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataSnippetFields();
        Assertions.assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("MD_DESCRIPTION", results.get(0));
    }

    /**
     * @see Configuration#getDisplayAdditionalMetadataNoHighlightFields()
     * @verifies return correct values
     */
    @Test
    void getDisplayAdditionalMetadataNoHighlightFields_shouldReturnCorrectValues() throws Exception {
        List<String> results = DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataNoHighlightFields();
        Assertions.assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("MD_REFID", results.get(0));
    }

    @Test
    void isDoublePageNavigationEnabled_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isDoublePageNavigationEnabled());
    }

    /**
     * @see Configuration#isSitelinksEnabled()
     * @verifies return correct value
     */
    @Test
    void isSitelinksEnabled_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isSitelinksEnabled());
    }

    /**
     * @see Configuration#getSitelinksFilterQuery()
     * @verifies return correct value
     */
    @Test
    void getSitelinksFilterQuery_shouldReturnCorrectValue() throws Exception {
        assertEquals("ISWORK:true", DataManager.getInstance().getConfiguration().getSitelinksFilterQuery());
    }

    /**
     * @see Configuration#getSitelinksField()
     * @verifies return correct value
     */
    @Test
    void getSitelinksField_shouldReturnCorrectValue() throws Exception {
        assertEquals(SolrConstants.CALENDAR_YEAR, DataManager.getInstance().getConfiguration().getSitelinksField());
    }

    @Test
    void getGetConfiguredCollections() {
        List<String> fields = DataManager.getInstance().getConfiguration().getConfiguredCollections();
        assertEquals(3, fields.size());
        assertTrue(fields.contains("DC"));
        assertTrue(fields.contains("MD_KNOWLEDGEFIELD"));
        assertTrue(fields.contains("MD_HIERARCHICALFIELD"));
    }

    /**
     * @see Configuration#isFullAccessForLocalhost()
     * @verifies return correct value
     */
    @Test
    void isFullAccessForLocalhost_shouldReturnCorrectValue() throws Exception {
        assertEquals(true, DataManager.getInstance().getConfiguration().isFullAccessForLocalhost());
    }

    /**
     * @see Configuration#getDocstrctWhitelistFilterSuffix()
     * @verifies return correct value
     */
    @Test
    void getDocstrctWhitelistFilterSuffix_shouldReturnCorrectValue() throws Exception {
        assertEquals("ISWORK:true OR ISANCHOR:true", DataManager.getInstance().getConfiguration().getDocstrctWhitelistFilterQuery());
    }

    /**
     * @see Configuration#getIIIFMetadataLabel(String)
     * @verifies return correct values
     */
    @Test
    void getIIIFMetadataLabel_shouldReturnCorrectValues() throws Exception {
        assertEquals("", DataManager.getInstance().getConfiguration().getIIIFMetadataLabel("MD_*"));
        assertEquals("label_year", DataManager.getInstance().getConfiguration().getIIIFMetadataLabel("YEAR"));
        assertEquals("label_provenienz", DataManager.getInstance().getConfiguration().getIIIFMetadataLabel("Provenienz/MD_EVENT_DETAILS"));
        assertEquals("", DataManager.getInstance().getConfiguration().getIIIFMetadataLabel("/YEAR"));
    }

    /**
     * @see Configuration#getTwitterUserName()
     * @verifies return correct value
     */
    @Test
    void getTwitterUserName_shouldReturnCorrectValue() throws Exception {
        assertEquals("@goobi", DataManager.getInstance().getConfiguration().getTwitterUserName());
    }

    /**
     * @see Configuration#getMetadataFromSubnodeConfig(HierarchicalConfiguration,boolean)
     * @verifies load replace rules correctly
     */
    @Test
    void getMetadataFromSubnodeConfig_shouldLoadReplaceRulesCorrectly() throws Exception {
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, Configuration.VALUE_DEFAULT);
        assertEquals(6, metadataList.size());
        Metadata mdTitle = metadataList.get(2);
        assertEquals("MD_TITLE", mdTitle.getLabel());
        assertEquals(1, mdTitle.getParams().size());
        assertEquals("foo", mdTitle.getParams().get(0).getReplaceRules().get(0).getKey());
        assertEquals("bar", mdTitle.getParams().get(0).getReplaceRules().get(0).getReplacement());
        assertEquals(MetadataReplaceRuleType.STRING, mdTitle.getParams().get(0).getReplaceRules().get(0).getType());
    }

    /**
     * @see Configuration#getDocstrctWhitelistFilterQuery()
     * @verifies return correct value
     */
    @Test
    void getDocstrctWhitelistFilterQuery_shouldReturnCorrectValue() throws Exception {
        assertEquals("ISWORK:true OR ISANCHOR:true", DataManager.getInstance().getConfiguration().getDocstrctWhitelistFilterQuery());
    }

    /**
     * @see Configuration#getReCaptchaSiteKey()
     * @verifies return correct value
     */
    @Test
    void getReCaptchaSiteKey_shouldReturnCorrectValue() throws Exception {
        assertEquals("6LetEyITAAAAAEAj7NTxgRXR6S_uhZrk9rn5HyB3", DataManager.getInstance().getConfiguration().getReCaptchaSiteKey());
    }

    /**
     * @see Configuration#getWorkflowRestUrl()
     * @verifies return correct value
     */
    @Test
    void getWorkflowRestUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("https://example.com/goobi/api/", DataManager.getInstance().getConfiguration().getWorkflowRestUrl());
    }

    /**
     * @see Configuration#getTaskManagerRestUrl()
     * @verifies return correct value
     */
    @Test
    void getTaskManagerRestUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("taskmanager_url/rest", DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
    }

    /**
     * @see Configuration#getTaskManagerServiceUrl()
     * @verifies return correct value
     */
    @Test
    void getTaskManagerServiceUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("taskmanager_url/service", DataManager.getInstance().getConfiguration().getTaskManagerServiceUrl());
    }

    /**
     * @see Configuration#getThemeRootPath()
     * @verifies return correct value
     */
    @Test
    void getThemeRootPath_shouldReturnCorrectValue() throws Exception {
        assertEquals("/opt/digiverso/goobi-viewer-theme-test/goobi-viewer-theme-mest/WebContent/resources/themes/",
                DataManager.getInstance().getConfiguration().getThemeRootPath());
    }

    /**
     * @see Configuration#getTocIndentation()
     * @verifies return correct value
     */
    @Test
    void getTocIndentation_shouldReturnCorrectValue() throws Exception {
        assertEquals(15, DataManager.getInstance().getConfiguration().getTocIndentation());
    }

    /**
     * @see Configuration#getTranskribusUserName()
     * @verifies return correct value
     */
    @Test
    void getTranskribusUserName_shouldReturnCorrectValue() throws Exception {
        assertEquals("transkribus_user", DataManager.getInstance().getConfiguration().getTranskribusUserName());
    }

    /**
     * @see Configuration#getTranskribusPassword()
     * @verifies return correct value
     */
    @Test
    void getTranskribusPassword_shouldReturnCorrectValue() throws Exception {
        assertEquals("transkribus_pwd", DataManager.getInstance().getConfiguration().getTranskribusPassword());
    }

    /**
     * @see Configuration#getDfgViewerUrl()
     * @verifies return correct value
     */
    @Test
    void getDfgViewerUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("dfg-viewer_value", DataManager.getInstance().getConfiguration().getDfgViewerUrl());
    }

    /**
     * @see Configuration#getDfgViewerSourcefileField()
     * @verifies return correct value
     */
    @Test
    void getDfgViewerSourcefileField_shouldReturnCorrectValue() throws Exception {
        assertEquals("MD2_DFGVIEWERURL", DataManager.getInstance().getConfiguration().getDfgViewerSourcefileField());
    }

    /**
     * @see Configuration#isDisplayCrowdsourcingModuleLinks()
     * @verifies return correct value
     */
    @Test
    void isDisplayCrowdsourcingModuleLinks_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isDisplayCrowdsourcingModuleLinks());
    }

    /**
     * @see Configuration#isDisplayTitlePURL()
     * @verifies return correct value
     */
    @Test
    void isDisplayTitlePURL_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplayTitlePURL());
    }

    /**
     * @see Configuration#isSearchInItemEnabled()
     * @verifies return true if the search field to search the current item/work is configured to be visible
     */
    @Test
    void isSearchInItemEnabled_shouldReturnTrueIfTheSearchFieldToSearchTheCurrentItemworkIsConfiguredToBeVisible() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isSearchInItemEnabled());
    }

    /**
     * @see Configuration#isUseReCaptcha()
     * @verifies return correct value
     */
    @Test
    void isUseReCaptcha_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isUseReCaptcha());
    }

    /**
     * @see Configuration#getWebApiToken()
     * @verifies return correct value
     */
    @Test
    void getWebApiToken_shouldReturnCorrectValue() throws Exception {
        assertEquals("test", DataManager.getInstance().getConfiguration().getWebApiToken());
    }

    /**
     * @see Configuration#isAddCORSHeader()
     * @verifies return correct value
     */
    @Test
    void isAddCORSHeader_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isAddCORSHeader());
    }

    /**
     * @see Configuration#isAllowRedirectCollectionToWork()
     * @verifies return correct value
     */
    @Test
    void isAllowRedirectCollectionToWork_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isAllowRedirectCollectionToWork());
    }

    /**
     * @see Configuration#isLimitImageHeight()
     * @verifies return correct value
     */
    @Test
    void isLimitImageHeight_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isLimitImageHeight());
    }

    /**
     * @see Configuration#getLimitImageHeightUpperRatioThreshold()
     * @verifies return correct value
     */
    @Test
    void getLimitImageHeightUpperRatioThreshold_shouldReturnCorrectValue() throws Exception {
        assertEquals(0.2f, DataManager.getInstance().getConfiguration().getLimitImageHeightLowerRatioThreshold());
    }

    /**
     * @see Configuration#getLimitImageHeightLowerRatioThreshold()
     * @verifies return correct value
     */
    @Test
    void getLimitImageHeightLowerRatioThreshold_shouldReturnCorrectValue() throws Exception {
        assertEquals(2.0f, DataManager.getInstance().getConfiguration().getLimitImageHeightUpperRatioThreshold());
    }

    @Test
    void testReadMapBoxToken() {
        assertEquals("some.token", DataManager.getInstance().getConfiguration().getMapBoxToken());
    }

    @Test
    void testGetLicenseDescriptions() {
        List<LicenseDescription> licenses = DataManager.getInstance().getConfiguration().getLicenseDescriptions();
        assertEquals(2, licenses.size());
        assertEquals("CC0 1.0", licenses.get(0).getLabel());
        assertEquals("http://rightsstatements.org/vocab/InC/1.0/", licenses.get(1).getUrl());
    }

    @Test
    void testGetGeoMapMarkerFields() {
        List<String> fields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();
        assertEquals(3, fields.size());
        assertTrue(fields.contains("MD_GEOJSON_POINT"));
        assertTrue(fields.contains("NORM_COORDS_GEOJSON"));
        assertTrue(fields.contains("MD_COORDINATES"));
    }

    @Test
    void testGetGeoMapMarkers() {
        List<GeoMapMarker> markers = DataManager.getInstance().getConfiguration().getGeoMapMarkers();
        assertEquals(5, markers.size());
        assertEquals("maps__marker_1", markers.get(0).getName());
        assertEquals("fa-circle", markers.get(0).getIcon());
        assertEquals("fa-search", markers.get(1).getIcon());
    }

    @Test
    void testGetGeoMapMarker() {
        GeoMapMarker marker = DataManager.getInstance().getConfiguration().getGeoMapMarker("maps__marker_2");
        Assertions.assertNotNull(marker);
        assertEquals("maps__marker_2", marker.getName());
        assertEquals("fa-search", marker.getIcon());
    }

    /**
     * @see Configuration#isDisplaySidebarBrowsingTerms()
     * @verifies return correct value
     */
    @Test
    void isDisplaySidebarBrowsingTerms_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplaySidebarBrowsingTerms());
    }

    /**
     * @see Configuration#isDisplaySidebarRssFeed()
     * @verifies return correct value
     */
    @Test
    void isDisplaySidebarRssFeed_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplaySidebarRssFeed());
    }

    /**
     * @see Configuration#isDisplaySidebarWidgetUsage()
     * @verifies return correct value
     */
    @Test
    void isDisplaySidebarWidgetUsage_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetUsage());
    }

    /**
     * @see Configuration#isDisplayWidgetUsageDownloadOptions()
     * @verifies return correct value
     */
    @Test
    void isDisplayWidgetUsageDownloadOptions_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplayWidgetUsageDownloadOptions());
    }

    /**
     * @see Configuration#isDisplaySidebarWidgetUsageCitationRecommendation()
     * @verifies return correct value
     */
    @Test
    void isDisplaySidebarWidgetUsageCitationRecommendation_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetUsageCitationRecommendation());
    }

    /**
     * @see Configuration#isDisplaySidebarWidgetUsageCitationLinks()
     * @verifies return correct value
     */
    @Test
    void isDisplaySidebarWidgetUsageCitationLinks_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetUsageCitationLinks());
    }

    /**
     * @see Configuration#getSidebarWidgetUsageCitationStyles()
     * @verifies return all configured values
     */
    @Test
    void getSidebarWidgetUsageCitationRecommendationStyles_shouldReturnAllConfiguredValues() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getSidebarWidgetUsageCitationRecommendationStyles();
        assertEquals(3, result.size());
    }

    /**
     * @see Configuration#getSidebarWidgetUsageCitationLinks()
     * @verifies return all configured values
     */
    @Test
    void getSidebarWidgetUsageCitationLinks_shouldReturnAllConfiguredValues() throws Exception {
        List<CitationLink> result = DataManager.getInstance().getConfiguration().getSidebarWidgetUsageCitationLinks();
        assertEquals(3, result.size());
        {
            CitationLink link = result.get(0);
            assertEquals(CitationLinkType.URL, link.getType());
            assertEquals(CitationLinkLevel.RECORD, link.getLevel());
            assertEquals("LABEL_URN", link.getLabel());
            assertEquals("URN", link.getField());
            assertEquals("https://nbn-resolving.org/{value}/", link.getPattern());
            assertTrue(link.isTopstructValueFallback());
        }
        {
            CitationLink link = result.get(1);
            assertEquals(CitationLinkType.INTERNAL, link.getType());
            assertEquals(CitationLinkLevel.DOCSTRUCT, link.getLevel());
        }
        {
            CitationLink link = result.get(2);
            assertEquals(CitationLinkLevel.IMAGE, link.getLevel());
        }
    }

    /**
     * @see Configuration#getSidebarWidgetUsagePageDownloadOptions()
     * @verifies return all configured elements
     */
    @Test
    void getSidebarWidgetUsagePageDownloadOptions_shouldReturnAllConfiguredElements() throws Exception {
        List<DownloadOption> result = DataManager.getInstance().getConfiguration().getSidebarWidgetUsagePageDownloadOptions();
        assertEquals(5, result.size());
        DownloadOption option = result.get(4);
        assertEquals("label__download_option_large_4096", option.getLabel());
        assertEquals("jpg", option.getFormat());
        assertEquals("4096" + DownloadOption.TIMES_SYMBOL + "4096", option.getBoxSizeLabel());
    }

    /**
     * @see Configuration#getPageSelectDropdownDisplayMinPages()
     * @verifies return correct value
     */
    @Test
    void getPageSelectDropdownDisplayMinPages_shouldReturnCorrectValue() throws Exception {
        assertEquals(1, DataManager.getInstance().getConfiguration().getPageSelectDropdownDisplayMinPages());
    }

    @Test
    void testGetConfiguredCollectionFields() {
        List<String> fields = DataManager.getInstance().getConfiguration().getConfiguredCollectionFields();
        assertEquals(3, fields.size());
        assertTrue(fields.contains("DC"));
        assertTrue(fields.contains("MD_KNOWLEDGEFIELD"));
        assertTrue(fields.contains("MD_HIERARCHICALFIELD"));
    }

    /**
     * @see Configuration#isDocstructNavigationEnabled()
     * @verifies return correct value
     */
    @Test
    void isDocstructNavigationEnabled_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isDocstructNavigationEnabled());
    }

    /**
     * @see Configuration#getDocstructNavigationTypes()
     * @verifies return all configured values
     */
    @Test
    void getDocstructNavigationTypes_shouldReturnAllConfiguredValues() throws Exception {
        {
            List<String> result = DataManager.getInstance().getConfiguration().getDocstructNavigationTypes(Configuration.VALUE_DEFAULT, true);
            assertEquals(2, result.size());
            assertEquals("prologue", result.get(0));
            assertEquals("chapter", result.get(1));
        }
        {
            List<String> result = DataManager.getInstance().getConfiguration().getDocstructNavigationTypes("notfound", true);
            assertEquals(2, result.size());
            assertEquals("prologue", result.get(0));
            assertEquals("chapter", result.get(1));
        }
    }

    /**
     * @see Configuration#getTranslationGroups()
     * @verifies read config items correctly
     */
    @Test
    void getTranslationGroups_shouldReadConfigItemsCorrectly() throws Exception {
        List<TranslationGroup> result = DataManager.getInstance().getConfiguration().getTranslationGroups();
        Assertions.assertNotNull(result);
        assertEquals(3, result.size());
        {
            TranslationGroup group = result.get(0);
            assertEquals(TranslationGroupType.SOLR_FIELD_NAMES, group.getType());
            assertEquals("label__translation_group_1", group.getName());
            assertEquals("desc__translation_group_1", group.getDescription());
            assertEquals(5, group.getItems().size());

            TranslationGroupItem item = group.getItems().get(4);
            assertEquals("MD_.*", item.getKey());
            assertTrue(item.isRegex());
        }
        {
            TranslationGroup group = result.get(1);
            assertEquals(TranslationGroupType.SOLR_FIELD_VALUES, group.getType());
            assertEquals("label__translation_group_2", group.getName());
            assertEquals("desc__translation_group_2", group.getDescription());
            assertEquals(2, group.getItems().size());
        }
    }

    /**
     * @see Configuration#isPageBrowseEnabled()
     * @verifies return correct value
     */
    @Test
    void isPageBrowseEnabled_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isPageBrowseEnabled());
    }

    /**
     * @see Configuration#getMetadataFromSubnodeConfig(HierarchicalConfiguration,boolean,int)
     * @verifies load metadata config attributes correctly
     */
    @Test
    void getMetadataFromSubnodeConfig_shouldLoadMetadataConfigAttributesCorrectly() throws Exception {
        HierarchicalConfiguration<ImmutableNode> metadataConfig =
                DataManager.getInstance().getConfiguration().getLocalConfigurationAt("metadata.metadataView(0).template(0).metadata(4)");
        Assertions.assertNotNull(metadataConfig);
        Metadata md = Configuration.getMetadataFromSubnodeConfig(metadataConfig, false, 0);
        Assertions.assertNotNull(md);
        assertEquals("MD_CATALOGIDSOURCE", md.getLabel());
        assertEquals("LINK_CATALOGIDSOURCE", md.getMasterValue());
        assertEquals("; ", md.getSeparator());
        assertTrue(md.isTopstructOnly());
    }

    /**
     * @see Configuration#getMetadataFromSubnodeConfig(HierarchicalConfiguration,boolean)
     * @verifies load parameters correctly
     */
    @Test
    void getMetadataFromSubnodeConfig_shouldLoadParametersCorrectly() throws Exception {
        HierarchicalConfiguration<ImmutableNode> metadataConfig =
                DataManager.getInstance().getConfiguration().getLocalConfigurationAt("metadata.metadataView(1).template(0).metadata(1)");
        Assertions.assertNotNull(metadataConfig);
        Metadata md = Configuration.getMetadataFromSubnodeConfig(metadataConfig, false, 0);
        Assertions.assertNotNull(md);
        assertEquals(5, md.getParams().size());
        assertEquals("EVENTTYPE", md.getParams().get(0).getKey());
        assertEquals(MetadataParameterType.FIELD, md.getParams().get(0).getType());
        assertEquals("EVENTTYPE", md.getLabelField());
    }

    /**
     * @see Configuration#getMetadataFromSubnodeConfig(HierarchicalConfiguration,boolean)
     * @verifies load child metadata configurations recursively
     */
    @Test
    void getMetadataFromSubnodeConfig_shouldLoadChildMetadataConfigurationsRecursively() throws Exception {
        List<HierarchicalConfiguration<ImmutableNode>> metadataConfig =
                DataManager.getInstance().getConfiguration().getLocalConfigurationsAt("metadata.metadataView(1).template(0).metadata(1)");
        Assertions.assertNotNull(metadataConfig);
        assertFalse(metadataConfig.isEmpty());
        Metadata md = Configuration.getMetadataFromSubnodeConfig(metadataConfig.get(0), false, 0);
        Assertions.assertNotNull(md);
        assertEquals(0, md.getIndentation());
        assertEquals(1, md.getChildMetadata().size());
        Metadata childMd = md.getChildMetadata().get(0);
        assertEquals(1, childMd.getIndentation());
        assertEquals(md, childMd.getParentMetadata());
        assertEquals("MD_ARTIST", childMd.getLabel());
        assertEquals("SORT_NAME", childMd.getSortField());
        assertTrue(childMd.isGroup());
        assertFalse(childMd.isSingleString());
        assertEquals(7, childMd.getParams().size());
    }

    /**
     * @see Configuration#isVisibleIIIFRenderingAlto()
     * @verifies return correct value
     */
    @Test
    void isVisibleIIIFRenderingAlto_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingAlto());
    }

    /**
     * @see Configuration#isVisibleIIIFRenderingPDF()
     * @verifies return correct value
     */
    @Test
    void isVisibleIIIFRenderingPDF_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPDF());
    }

    /**
     * @see Configuration#isVisibleIIIFRenderingPlaintext()
     * @verifies return correct value
     */
    @Test
    void isVisibleIIIFRenderingPlaintext_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingPlaintext());
    }

    /**
     * @see Configuration#isVisibleIIIFRenderingViewer()
     * @verifies return correct value
     */
    @Test
    void isVisibleIIIFRenderingViewer_shouldReturnCorrectValue() throws Exception {
        assertFalse(DataManager.getInstance().getConfiguration().isVisibleIIIFRenderingViewer());
    }

    /**
     * @see Configuration#isRememberImageRotation()
     * @verifies return correct value
     */
    @Test
    void isRememberImageRotation_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isRememberImageRotation());
    }

    /**
     * @see Configuration#isRememberImageZoom()
     * @verifies return correct value
     */
    @Test
    void isRememberImageZoom_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isRememberImageZoom());
    }

    /**
     * @see Configuration#isContentUploadEnabled()
     * @verifies return correct value
     */
    @Test
    void isContentUploadEnabled_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isContentUploadEnabled());
    }

    /**
     * @see Configuration#getContentUploadToken()
     * @verifies return correct value
     */
    @Test
    void getContentUploadToken_shouldReturnCorrectValue() throws Exception {
        assertEquals("12345-GOOBI-WORKFLOW-REST-TOKEN-67890", DataManager.getInstance().getConfiguration().getContentUploadToken());
    }

    /**
     * @see Configuration#getContentUploadDocstruct()
     * @verifies return correct value
     */
    @Test
    void getContentUploadDocstruct_shouldReturnCorrectValue() throws Exception {
        assertEquals("manuscript", DataManager.getInstance().getConfiguration().getContentUploadDocstruct());
    }

    /**
     * @see Configuration#getContentUploadTemplateName()
     * @verifies return correct value
     */
    @Test
    void getContentUploadTemplateName_shouldReturnCorrectValue() throws Exception {
        assertEquals("Sample_workflow", DataManager.getInstance().getConfiguration().getContentUploadTemplateName());
    }

    /**
     * @see Configuration#getContentUploadRejectionPropertyName()
     * @verifies return correct value
     */
    @Test
    void getContentUploadRejectionPropertyName_shouldReturnCorrectValue() throws Exception {
        assertEquals("uploadRejected", DataManager.getInstance().getConfiguration().getContentUploadRejectionPropertyName());
    }

    /**
     * @see Configuration#getContentUploadRejectionReasonPropertyName()
     * @verifies return correct value
     */
    @Test
    void getContentUploadRejectionReasonPropertyName_shouldReturnCorrectValue() throws Exception {
        assertEquals("uploadRejectedInformation", DataManager.getInstance().getConfiguration().getContentUploadRejectionReasonPropertyName());
    }

    /**
     * @see Configuration#isUseFacetsAsExpandQuery()
     * @verifies return correct value
     */
    @Test
    void isUseFacetsAsExpandQuery_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isUseFacetsAsExpandQuery());
    }

    /**
     * @see Configuration#getAllowedFacetsForExpandQuery()
     * @verifies return all configured elements
     */
    @Test
    void getAllowedFacetsForExpandQuery_shouldReturnAllConfiguredElements() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getAllowedFacetsForExpandQuery();
        assertEquals(2, result.size());
        assertEquals("(FACET_DC:\"foo\" OR FACET_DC:foo.*)", result.get(0));
        assertEquals("(FACET_DC:\"bar\" OR FACET_DC:bar.*)", result.get(1));
    }

    /**
     * @see Configuration#isSearchResultGroupsEnabled()
     * @verifies return correct value
     */
    @Test
    void isSearchResultGroupsEnabled_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled());
    }

    /**
     * @see Configuration#getSearchResultGroups()
     * @verifies return all configured elements
     */
    @Test
    void getSearchResultGroups_shouldReturnAllConfiguredElements() throws Exception {
        List<SearchResultGroup> groups = DataManager.getInstance().getConfiguration().getSearchResultGroups();
        assertEquals(3, groups.size());

        assertEquals("lido_objects", groups.get(0).getName());
        assertEquals("SOURCEDOCFORMAT:LIDO", groups.get(0).getQuery());
        assertTrue(groups.get(0).isUseAsAdvancedSearchTemplate());
    }

    /**
     * @see Configuration#isCopyrightIndicatorEnabled()
     * @verifies return correct value
     */
    @Test
    void isCopyrightIndicatorEnabled_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isCopyrightIndicatorEnabled());
    }

    /**
     * @see Configuration#getCopyrightIndicatorStyle()
     * @verifies return correct value
     */
    @Test
    void getCopyrightIndicatorStyle_shouldReturnCorrectValue() throws Exception {
        assertEquals("trafficlight", DataManager.getInstance().getConfiguration().getCopyrightIndicatorStyle());
    }

    /**
     * @see Configuration#getCopyrightIndicatorStatusField()
     * @verifies return correct value
     */
    @Test
    void getCopyrightIndicatorStatusField_shouldReturnCorrectValue() throws Exception {
        assertEquals("MD_ACCESSCONDITION", DataManager.getInstance().getConfiguration().getCopyrightIndicatorStatusField());
    }

    /**
     * @see Configuration#getCopyrightIndicatorStatusForValue(String)
     * @verifies return correct value
     */
    @Test
    void getCopyrightIndicatorStatusForValue_shouldReturnCorrectValue() throws Exception {
        CopyrightIndicatorStatus status = DataManager.getInstance().getConfiguration().getCopyrightIndicatorStatusForValue("Freier Zugang");
        Assertions.assertNotNull(status);
        assertEquals(CopyrightIndicatorStatus.Status.OPEN, status.getStatus());
        assertEquals("COPYRIGHT_STATUS_OPEN", status.getDescription());

        status = DataManager.getInstance().getConfiguration().getCopyrightIndicatorStatusForValue("Eingeschränker Zugang");
        Assertions.assertNotNull(status);
        assertEquals(CopyrightIndicatorStatus.Status.PARTIAL, status.getStatus());
        assertEquals("COPYRIGHT_STATUS_PARTIAL", status.getDescription());

        status = DataManager.getInstance().getConfiguration().getCopyrightIndicatorStatusForValue("Gesperrter Zugang");
        Assertions.assertNotNull(status);
        assertEquals(CopyrightIndicatorStatus.Status.LOCKED, status.getStatus());
        assertEquals("COPYRIGHT_STATUS_LOCKED", status.getDescription());

    }

    /**
     * @see Configuration#getCopyrightIndicatorLicenseField()
     * @verifies return correct value
     */
    @Test
    void getCopyrightIndicatorLicenseField_shouldReturnCorrectValue() throws Exception {
        assertEquals("MD_ACCESSCONDITIONCOPYRIGHT", DataManager.getInstance().getConfiguration().getCopyrightIndicatorLicenseField());
    }

    /**
     * @see Configuration#getCopyrightIndicatorLicenseForValue(String)
     * @verifies return correct value
     */
    @Test
    void getCopyrightIndicatorLicenseForValue_shouldReturnCorrectValue() throws Exception {
        CopyrightIndicatorLicense result = DataManager.getInstance().getConfiguration().getCopyrightIndicatorLicenseForValue("VGWORT");
        Assertions.assertNotNull(result);
        assertEquals("COPYRIGHT_DESCRIPTION_VGWORT", result.getDescription());
        assertEquals(1, result.getIcons().size());
        assertEquals("paragraph50.svg", result.getIcons().get(0));
    }

    /**
     * @see Configuration#isProxyEnabled()
     * @verifies return correct value
     */
    @Test
    void isProxyEnabled_shouldReturnCorrectValue() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isProxyEnabled());
    }

    /**
     * @see Configuration#getProxyUrl()
     * @verifies return correct value
     */
    @Test
    void getProxyUrl_shouldReturnCorrectValue() throws Exception {
        assertEquals("my.proxy", DataManager.getInstance().getConfiguration().getProxyUrl());
    }

    /**
     * @see Configuration#getProxyPort()
     * @verifies return correct value
     */
    @Test
    void getProxyPort_shouldReturnCorrectValue() throws Exception {
        assertEquals(9999, DataManager.getInstance().getConfiguration().getProxyPort());
    }

    /**
     * @see Configuration#isHostProxyWhitelisted(String)
     * @verifies return true if host whitelisted
     */
    @Test
    void isHostProxyWhitelisted_shouldReturnTrueIfHostWhitelisted() throws Exception {
        assertTrue(DataManager.getInstance().getConfiguration().isHostProxyWhitelisted("http://localhost:1234"));
    }

    @Test
    void test_getGeomapFeatureTitleOptions() {
        List<SelectItem> items = DataManager.getInstance().getConfiguration().getGeomapFeatureTitleOptions();
        assertEquals(3, items.size());

        assertEquals("cms__geomaps__popup_content__option__none", items.get(0).getLabel());
        assertEquals("", items.get(0).getValue());

        assertEquals("cms__geomaps__popup_content__option__place", items.get(1).getLabel());
        assertEquals("NORM_NAME", items.get(1).getValue());

        assertEquals("cms__geomaps__popup_content__option__metadata", items.get(2).getLabel());
        assertEquals("MD_VALUE", items.get(2).getValue());

    }

    @Test
    void test_getGeomapFilters() {
        Map<String, List<LabeledValue>> filters = DataManager.getInstance().getConfiguration().getGeomapFilters();
        assertEquals(3, filters.size());
        assertEquals("D", filters.get("").get(0).getValue());
        assertEquals("", filters.get("").get(0).getLabel());
        assertEquals("A", filters.get("X").get(0).getValue());
        assertEquals("", filters.get("X").get(0).getLabel());
        assertEquals("B", filters.get("Y").get(0).getValue());
        assertEquals("b", filters.get("Y").get(0).getLabel());
        assertEquals("C", filters.get("Y").get(1).getValue());
        assertEquals("c", filters.get("Y").get(1).getLabel());
    }

    @Test
    void testGetDateFormat() {
        assertEquals("dd/MM/yyyy", DataManager.getInstance().getConfiguration().getStringFormat("date", Locale.ENGLISH).orElse("Not configured"));
    }

    /**
     * @see Configuration#isArchivesEnabled()
     * @verifies return correct value
     */
    @Test
    void isArchivesEnabled_shouldReturnCorrectValue() throws Exception {
        Assertions.assertTrue(DataManager.getInstance().getConfiguration().isArchivesEnabled());
    }
}
