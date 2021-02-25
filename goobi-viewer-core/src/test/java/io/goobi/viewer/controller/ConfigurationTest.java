/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
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
package io.goobi.viewer.controller;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.download.DownloadOption;
import io.goobi.viewer.model.maps.GeoMapMarker;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.metadata.MetadataParameter;
import io.goobi.viewer.model.metadata.MetadataReplaceRule.MetadataReplaceRuleType;
import io.goobi.viewer.model.metadata.MetadataView;
import io.goobi.viewer.model.search.AdvancedSearchFieldConfiguration;
import io.goobi.viewer.model.security.SecurityQuestion;
import io.goobi.viewer.model.security.authentication.HttpAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.StringPair;

public class ConfigurationTest extends AbstractTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTest.class);

    public static final String APPLICATION_ROOT_URL = "https://viewer.goobi.io/";

    @Override
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * @see Configuration#getBreadcrumbsClipping()
     * @verifies return correct value
     */
    @Test
    public void getBreadcrumbsClipping_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(24, DataManager.getInstance().getConfiguration().getBreadcrumbsClipping());
    }

    /**
     * @see Configuration#getBrowsingMenuFields()
     * @verifies return all configured elements
     */
    @Test
    public void getBrowsingMenuFields_shouldReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getConfiguration().getBrowsingMenuFields().size());
    }

    /**
     * @see Configuration#getBrowsingMenuHitsPerPage()
     * @verifies return correct value
     */
    @Test
    public void getBrowsingMenuHitsPerPage_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(19, DataManager.getInstance().getConfiguration().getBrowsingMenuHitsPerPage());
    }

    /**
     * @see Configuration#getBrowsingMenuIndexSizeThreshold()
     * @verifies return correct value
     */
    @Test
    public void getBrowsingMenuIndexSizeThreshold_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(50000, DataManager.getInstance().getConfiguration().getBrowsingMenuIndexSizeThreshold());
    }

    /**
     * @see Configuration#getBrowsingMenuSortingIgnoreLeadingChars()
     * @verifies return correct value
     */
    @Test
    public void getBrowsingMenuSortingIgnoreLeadingChars_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(".[]", DataManager.getInstance().getConfiguration().getBrowsingMenuSortingIgnoreLeadingChars());
    }

    /**
     * @see Configuration#getCollectionBlacklist()
     * @verifies return all configured elements
     */
    @Test
    public void getCollectionBlacklist_shouldReturnAllConfiguredElements() throws Exception {
        List<String> ret = DataManager.getInstance().getConfiguration().getCollectionBlacklist(SolrConstants.DC);
        Assert.assertNotNull(ret);
        Assert.assertEquals(2, ret.size());
    }

    /**
     * @see Configuration#getCollectionDefaultSortField(String)
     * @verifies return correct field for collection
     */
    @Test
    public void getCollectionDefaultSortField_shouldReturnCorrectFieldForCollection() throws Exception {
        Assert.assertEquals("SORT_CREATOR",
                DataManager.getInstance().getConfiguration().getCollectionDefaultSortField(SolrConstants.DC, "collection1.sub1"));
    }

    /**
     * @see Configuration#getCollectionDefaultSortField(String)
     * @verifies give priority to exact matches
     */
    @Test
    public void getCollectionDefaultSortField_shouldGivePriorityToExactMatches() throws Exception {
        Assert.assertEquals("SORT_TITLE",
                DataManager.getInstance().getConfiguration().getCollectionDefaultSortField(SolrConstants.DC, "collection1"));
    }

    /**
     * @see Configuration#getCollectionDefaultSortField(String)
     * @verifies return hyphen if collection not found
     */
    @Test
    public void getCollectionDefaultSortField_shouldReturnHyphenIfCollectionNotFound() throws Exception {
        Assert.assertEquals("-",
                DataManager.getInstance().getConfiguration().getCollectionDefaultSortField(SolrConstants.DC, "nonexistingcollection"));
    }

    /**
     * @see Configuration#getCollectionDisplayNumberOfVolumesLevel()
     * @verifies return correct value
     */
    @Test
    public void getCollectionDisplayNumberOfVolumesLevel_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(16, DataManager.getInstance().getConfiguration().getCollectionDisplayNumberOfVolumesLevel(SolrConstants.DC));
    }

    /**
     * @see Configuration#getCollectionSorting()
     * @verifies return all configured elements
     */
    @Test
    public void getCollectionSorting_shouldReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getConfiguration().getCollectionSorting(SolrConstants.DC).size());
    }

    /**
     * @see Configuration#getContentServerRealUrl()
     * @verifies return correct value
     */
    @Test
    public void getContentServerRealUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("contentServer_value", DataManager.getInstance().getConfiguration().getContentServerRealUrl());
    }

    /**
     * @see Configuration#getContentServerWrapperUrl()
     * @verifies return correct value
     */
    @Deprecated
    @Test
    public void getContentServerWrapperUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("contentServerWrapper_value", DataManager.getInstance().getConfiguration().getContentServerWrapperUrl());
    }

    /**
     * @see Configuration#getDownloadUrl()
     * @verifies return correct value
     */
    @Test
    public void getDownloadUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("https://viewer.goobi.io/download/", DataManager.getInstance().getConfiguration().getDownloadUrl());
    }

    /**
     * @see Configuration#getDataRepositoriesHome()
     * @verifies return correct value
     */
    @Test
    @Deprecated
    public void getDataRepositoriesHome_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("src/test/resources/data/viewer/data/", DataManager.getInstance().getConfiguration().getDataRepositoriesHome());
    }

    /**
     * @see Configuration#getDcUrl()
     * @verifies return correct value
     */
    @Test
    public void getDcUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("dc_value", DataManager.getInstance().getConfiguration().getDcUrl());
    }

    /**
     * @see Configuration#getDisplayBreadcrumbs()
     * @verifies return correct value
     */
    @Test
    public void getDisplayBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().getDisplayBreadcrumbs());
    }

    /**
     * @see Configuration#getDisplayMetadataPageLinkBlock()
     * @verifies return correct value
     */
    @Test
    public void getDisplayMetadataPageLinkBlock_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().getDisplayMetadataPageLinkBlock());
    }

    /**
     * @see Configuration#getDisplayStructType()
     * @verifies return correct value
     */
    @Test
    public void getDisplayStructType_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().getDisplayStructType());
    }

    /**
     * @see Configuration#getEseUrl()
     * @verifies return correct value
     */
    @Test
    public void getEseUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("ese_value", DataManager.getInstance().getConfiguration().getEseUrl());
    }

    /**
     * @see Configuration#getFeedbackEmailAddress()
     * @verifies return correct value
     */
    @Test
    public void getFeedbackEmailAddress_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("feedbackEmailAddress_value", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress());
    }

    /**
     * @see Configuration#getSearchHitsPerPageDefaultValue()
     * @verifies return correct value
     */
    @Test
    public void getSearchHitsPerPageDefaultValue_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(15, DataManager.getInstance().getConfiguration().getSearchHitsPerPageDefaultValue());
    }

    /**
     * @see Configuration#getSearchHitsPerPageValues()
     * @verifies return all values
     */
    @Test
    public void getSearchHitsPerPageValues_shouldReturnAllValues() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getConfiguration().getSearchHitsPerPageValues().size());
    }

    /**
     * @see Configuration#getFulltextFragmentLength()
     * @verifies return correct value
     */
    @Test
    public void getFulltextFragmentLength_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(50, DataManager.getInstance().getConfiguration().getFulltextFragmentLength());
    }

    /**
     * @see Configuration#getHotfolder()
     * @verifies return correct value
     */
    @Test
    public void getHotfolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("hotfolder", DataManager.getInstance().getConfiguration().getHotfolder());
    }

    /**
     * @see Configuration#getIndexedLidoFolder()
     * @verifies return correct value
     */
    @Test
    public void getIndexedLidoFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("indexed_lido", DataManager.getInstance().getConfiguration().getIndexedLidoFolder());
    }

    /**
     * @see Configuration#getIndexedMetsFolder()
     * @verifies return correct value
     */
    @Test
    public void getIndexedMetsFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("indexed_mets", DataManager.getInstance().getConfiguration().getIndexedMetsFolder());
    }

    /**
     * @see Configuration#getIndexedDenkxwebFolder()
     * @verifies return correct value
     */
    @Test
    public void getIndexedDenkxwebFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("indexed_denkxweb", DataManager.getInstance().getConfiguration().getIndexedDenkxwebFolder());
    }

    /**
     * @see Configuration#getIndexedDublinCoreFolder()
     * @verifies return correct value
     */
    @Test
    public void getIndexedDublinCoreFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("indexed_dublincore", DataManager.getInstance().getConfiguration().getIndexedDublinCoreFolder());
    }

    /**
     * @see Configuration#getMetadataViews()
     * @verifies return all configured values
     */
    @Test
    public void getMetadataViews_shouldReturnAllConfiguredValues() throws Exception {
        List<MetadataView> result = DataManager.getInstance().getConfiguration().getMetadataViews();
        Assert.assertEquals(2, result.size());
        MetadataView view = result.get(1);
        Assert.assertEquals(1, view.getIndex());
        Assert.assertEquals("label__metadata_other", view.getLabel());
        Assert.assertEquals("_other", view.getUrl());
        Assert.assertEquals("foo:bar", view.getCondition());
    }

    /**
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    public void getMainMetadataForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        Assert.assertEquals(1, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, "Chapter").size());
    }

    /**
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return default template configuration if template not found
     */
    @Test
    public void getMainMetadataForTemplate_shouldReturnDefaultTemplateConfigurationIfTemplateNotFound() throws Exception {
        Assert.assertEquals(6, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, "nonexisting").size());
    }

    /**
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return default template if template is null
     */
    @Test
    public void getMainMetadataForTemplate_shouldReturnDefaultTemplateIfTemplateIsNull() throws Exception {
        Assert.assertEquals(6, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, null).size());
    }

    /**
     * @see Configuration#getTocLabelConfiguration(String)
     * @verifies return correct template configuration
     */
    @Test
    public void getTocLabelConfiguration_shouldReturnCorrectTemplateConfiguration() throws Exception {
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getTocLabelConfiguration("PeriodicalVolume");
        Assert.assertNotNull(metadataList);
        Assert.assertEquals(1, metadataList.size());
        Metadata metadata = metadataList.get(0);
        Assert.assertEquals("", metadata.getLabel());
        Assert.assertEquals("{CURRENTNO}{MD_TITLE}", metadata.getMasterValue());
        List<MetadataParameter> params = metadata.getParams();
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("CURRENTNO", params.get(0).getKey());
        Assert.assertEquals("Number ", params.get(0).getPrefix());
        Assert.assertEquals("MD_TITLE", params.get(1).getKey());
        Assert.assertEquals("LABEL", params.get(1).getAltKey());
        Assert.assertEquals(": ", params.get(1).getPrefix());
    }

    /**
     * @see Configuration#getTocLabelConfiguration(String)
     * @verifies return default template configuration if template not found
     */
    @Test
    public void getTocLabelConfiguration_shouldReturnDefaultTemplateConfigurationIfTemplateNotFound() throws Exception {
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getTocLabelConfiguration("notfound");
        Assert.assertNotNull(metadataList);
        Assert.assertEquals(1, metadataList.size());
        Metadata metadata = metadataList.get(0);
        Assert.assertEquals("", metadata.getLabel());
        Assert.assertEquals("{LABEL}{MD_CREATOR}", metadata.getMasterValue());
        List<MetadataParameter> params = metadata.getParams();
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("LABEL", params.get(0).getKey());
        Assert.assertEquals("MD_CREATOR", params.get(1).getKey());
        Assert.assertEquals("/", params.get(1).getPrefix());
    }

    /**
     * @see Configuration#getMarcUrl()
     * @verifies return correct value
     */
    @Test
    public void getMarcUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("marc_value", DataManager.getInstance().getConfiguration().getMarcUrl());
    }

    /**
     * @see Configuration#getMediaFolder()
     * @verifies return correct value
     */
    @Test
    public void getMediaFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("media", DataManager.getInstance().getConfiguration().getMediaFolder());
    }

    /**
     * @see Configuration#getPdfFolder()
     * @verifies return correct value
     */
    @Test
    public void getPdfFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("PDF", DataManager.getInstance().getConfiguration().getPdfFolder());
    }

    @Test
    public void getVocabulariesFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("vocabularies", DataManager.getInstance().getConfiguration().getVocabulariesFolder());
    }

    /**
     * @see Configuration#getMetsUrl()
     * @verifies return correct value
     */
    @Test
    public void getMetsUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("mets_value", DataManager.getInstance().getConfiguration().getMetsUrl());
    }

    /**
     * @see Configuration#getMultivolumeThumbnailHeight()
     * @verifies return correct value
     */
    @Test
    public void getMultivolumeThumbnailHeight_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(13, DataManager.getInstance().getConfiguration().getMultivolumeThumbnailHeight());
    }

    /**
     * @see Configuration#getMultivolumeThumbnailWidth()
     * @verifies return correct value
     */
    @Test
    public void getMultivolumeThumbnailWidth_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(12, DataManager.getInstance().getConfiguration().getMultivolumeThumbnailWidth());
    }

    /**
     * @see Configuration#isUserRegistrationEnabled()
     * @verifies return correct value
     */
    @Test
    public void isUserRegistrationEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isUserRegistrationEnabled());
    }

    /**
     * @see Configuration#getSecurityQuestions()
     * @verifies return all configured elements
     */
    @Test
    public void getSecurityQuestions_shouldReturnAllConfiguredElements() throws Exception {
        List<SecurityQuestion> result = DataManager.getInstance().getConfiguration().getSecurityQuestions();
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        {
            SecurityQuestion q = result.get(0);
            Assert.assertEquals("user__security_question__1", q.getQuestionKey());
            Assert.assertEquals(3, q.getCorrectAnswers().size());
            Assert.assertTrue(q.getCorrectAnswers().contains("foo"));
            Assert.assertTrue(q.getCorrectAnswers().contains("f00"));
            Assert.assertTrue(q.getCorrectAnswers().contains("phoo"));
        }
    }

    /**
     * @see Configuration#isShowOpenIdConnect()
     * @verifies return correct value
     */
    @Test
    public void isShowOpenIdConnect_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isShowOpenIdConnect());
    }

    /**
     * @see Configuration#getAuthenticationProviders()
     * @verifies return all properly configured elements
     */
    @Test
    public void getAuthenticationProviders_shouldReturnAllProperlyConfiguredElements() throws Exception {
        List<IAuthenticationProvider> providers = DataManager.getInstance().getConfiguration().getAuthenticationProviders();
        Assert.assertEquals(5, providers.size());

        //google openid
        Assert.assertEquals("Google", providers.get(0).getName());
        Assert.assertEquals("openid", providers.get(0).getType().toLowerCase());
        Assert.assertEquals("https://accounts.google.com/o/oauth2/auth", ((OpenIdProvider) providers.get(0)).getUrl());
        Assert.assertEquals("id_google", ((OpenIdProvider) providers.get(0)).getClientId());
        Assert.assertEquals("secret_google", ((OpenIdProvider) providers.get(0)).getClientSecret());
        Assert.assertEquals("google.png", ((OpenIdProvider) providers.get(0)).getImage());
        Assert.assertEquals("Google", ((OpenIdProvider) providers.get(0)).getLabel());

        //vuFind
        Assert.assertEquals("VuFind", providers.get(2).getName());
        Assert.assertEquals("userpassword", providers.get(2).getType().toLowerCase());
        Assert.assertEquals(7000l, ((HttpAuthenticationProvider) providers.get(2)).getTimeoutMillis());
        Assert.assertEquals("VuFind-label", ((HttpAuthenticationProvider) providers.get(2)).getLabel());

        // bibliotheca
        Assert.assertEquals("Bibliotheca", providers.get(3).getName());
        Assert.assertEquals("userpassword", providers.get(3).getType().toLowerCase());

        //local
        Assert.assertEquals("Goobi viewer", providers.get(4).getName());
        Assert.assertEquals("local", providers.get(4).getType().toLowerCase());
    }

    /**
     * @see Configuration#getAuthenticationProviders()
     * @verifies load user group names correctly
     */
    @Test
    public void getAuthenticationProviders_shouldLoadUserGroupNamesCorrectly() throws Exception {
        List<IAuthenticationProvider> providers = DataManager.getInstance().getConfiguration().getAuthenticationProviders();
        Assert.assertEquals(5, providers.size());
        List<String> groups = providers.get(2).getAddUserToGroups();
        Assert.assertEquals(2, groups.size());
    }

    /**
     * @see Configuration#getOrigContentFolder()
     * @verifies return correct value
     */
    @Test
    public void getOrigContentFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("source", DataManager.getInstance().getConfiguration().getOrigContentFolder());
    }

    /**
     * @see Configuration#getPageLoaderThreshold()
     * @verifies return correct value
     */
    @Test
    public void getPageLoaderThreshold_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(1000, DataManager.getInstance().getConfiguration().getPageLoaderThreshold());
    }

    /**
     * @see Configuration#getPageType(PageType)
     * @verifies return the correct value for the given type
     */
    @Test
    public void getPageType_shouldReturnTheCorrectValueForTheGivenType() throws Exception {
        Assert.assertEquals("viewImage_value", DataManager.getInstance().getConfiguration().getPageType(PageType.viewImage));
    }

    /**
     * @see Configuration#getRssCopyrightText()
     * @verifies return correct value
     */
    @Test
    public void getRssCopyrightText_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("copyright_value", DataManager.getInstance().getConfiguration().getRssCopyrightText());
    }

    /**
     * @see Configuration#getRssDescription()
     * @verifies return correct value
     */
    @Test
    public void getRssDescription_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("description_value", DataManager.getInstance().getConfiguration().getRssDescription());
    }

    /**
     * @see Configuration#getRssFeedItems()
     * @verifies return correct value
     */
    @Test
    public void getRssFeedItems_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(25, DataManager.getInstance().getConfiguration().getRssFeedItems());
    }

    /**
     * @see Configuration#getRssTitle()
     * @verifies return correct value
     */
    @Test
    public void getRssTitle_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("title_value", DataManager.getInstance().getConfiguration().getRssTitle());
    }

    /**
     * @see Configuration#getSearchHitMetadataForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    public void getSearchHitMetadataForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        Assert.assertEquals(1, DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate("Chapter").size());
    }

    /**
     * @see Configuration#getSearchHitMetadataForTemplate(String)
     * @verifies return default template configuration if requested not found
     */
    @Test
    public void getSearchHitMetadataForTemplate_shouldReturnDefaultTemplateConfigurationIfRequestedNotFound() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate("nonexisting").size());
    }

    /**
     * @see Configuration#getSearchHitMetadataForTemplate(String)
     * @verifies return default template if template is null
     */
    @Test
    public void getSearchHitMetadataForTemplate_shouldReturnDefaultTemplateIfTemplateIsNull() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate(null).size());
    }

    /**
     * @see Configuration#getSearchHitMetadataValueLength()
     * @verifies return correct value
     */
    @Test
    public void getSearchHitMetadataValueLength_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(18, DataManager.getInstance().getConfiguration().getSearchHitMetadataValueLength());
    }

    /**
     * @see Configuration#getSearchHitMetadataValueNumber()
     * @verifies return correct value
     */
    @Test
    public void getSearchHitMetadataValueNumber_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(17, DataManager.getInstance().getConfiguration().getSearchHitMetadataValueNumber());
    }

    /**
     * @see Configuration#getSidebarTocInitialCollapseLevel()
     * @verifies return correct value
     */
    @Test
    public void getSidebarTocInitialCollapseLevel_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(22, DataManager.getInstance().getConfiguration().getSidebarTocInitialCollapseLevel());
    }

    /**
     * @see Configuration#getSidebarTocLengthBeforeCut()
     * @verifies return correct value
     */
    @Test
    public void getSidebarTocLengthBeforeCut_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(21, DataManager.getInstance().getConfiguration().getSidebarTocLengthBeforeCut());
    }

    /**
     * @see Configuration#getSidebarTocPageNumbersVisible()
     * @verifies return correct value
     */
    @Test
    public void getSidebarTocPageNumbersVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().getSidebarTocPageNumbersVisible());
    }

    /**
     * @see Configuration#isSidebarTocTreeView()
     * @verifies return correct value
     */
    @Test
    public void isSidebarTocTreeView_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isSidebarTocTreeView());
    }

    /**
     * @see Configuration#isTocTreeView(String)
     * @verifies return true for allowed docstructs
     */
    @Test
    public void isTocTreeView_shouldReturnTrueForAllowedDocstructs() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isTocTreeView("Monograph"));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isTocTreeView("Manuscript"));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isTocTreeView("MusicSupplies"));
    }

    /**
     * @see Configuration#isTocTreeView(String)
     * @verifies return false for other docstructs
     */
    @Test
    public void isTocTreeView_shouldReturnFalseForOtherDocstructs() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isTocTreeView("Volume"));
    }

    /**
     * @see Configuration#getSmtpPassword()
     * @verifies return correct value
     */
    @Test
    public void getSmtpPassword_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("smtpPassword_value", DataManager.getInstance().getConfiguration().getSmtpPassword());
    }

    /**
     * @see Configuration#getSmtpSecurity()
     * @verifies return correct value
     */
    @Test
    public void getSmtpSecurity_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("smtpSecurity_value", DataManager.getInstance().getConfiguration().getSmtpSecurity());
    }

    /**
     * @see Configuration#getSmtpPort()
     * @verifies return correct value
     */
    @Test
    public void getSmtpPort_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(25, DataManager.getInstance().getConfiguration().getSmtpPort());
    }

    /**
     * @see Configuration#getSmtpSenderAddress()
     * @verifies return correct value
     */
    @Test
    public void getSmtpSenderAddress_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("smtpSenderAddress_value", DataManager.getInstance().getConfiguration().getSmtpSenderAddress());
    }

    /**
     * @see Configuration#getSmtpSenderName()
     * @verifies return correct value
     */
    @Test
    public void getSmtpSenderName_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("smtpSenderName_value", DataManager.getInstance().getConfiguration().getSmtpSenderName());
    }

    /**
     * @see Configuration#getSmtpServer()
     * @verifies return correct value
     */
    @Test
    public void getSmtpServer_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("smtpServer_value", DataManager.getInstance().getConfiguration().getSmtpServer());
    }

    /**
     * @see Configuration#getSmtpUser()
     * @verifies return correct value
     */
    @Test
    public void getSmtpUser_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("smtpUser_value", DataManager.getInstance().getConfiguration().getSmtpUser());
    }

    /**
     * @see Configuration#getAnonymousUserEmailAddress()
     * @verifies return correct value
     */
    @Test
    public void getAnonymousUserEmailAddress_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("we.are@anonymous.lulz", DataManager.getInstance().getConfiguration().getAnonymousUserEmailAddress());
    }

    /**
     * @see Configuration#getSolrUrl()
     * @verifies return correct value
     */
    @Test
    public void getSolrUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("https://viewer-testing-index.goobi.io/solr/collection1", DataManager.getInstance().getConfiguration().getSolrUrl());
    }

    /**
     * @see Configuration#getCollectionSplittingChar(String)
     * @verifies return correct value
     */
    @Test
    public void getCollectionSplittingChar_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(".", DataManager.getInstance().getConfiguration().getCollectionSplittingChar(SolrConstants.DC));
        Assert.assertEquals("/", DataManager.getInstance().getConfiguration().getCollectionSplittingChar("MD_KNOWLEDGEFIELD"));
        Assert.assertEquals(".", DataManager.getInstance().getConfiguration().getCollectionSplittingChar(SolrConstants.DOCTYPE));
    }

    /**
     * @see Configuration#getPageSelectionFormat()
     * @verifies return correct value
     */
    @Test
    public void getPageSelectionFormat_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("{order} {msg.of} {numpages}", DataManager.getInstance().getConfiguration().getPageSelectionFormat());
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies load all stopwords
     */
    @Test
    public void loadStopwords_shouldLoadAllStopwords() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("src/test/resources/stopwords.txt");
        Assert.assertNotNull(stopwords);
        Assert.assertEquals(5, stopwords.size());
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies remove parts starting with pipe
     */
    @Test
    public void loadStopwords_shouldRemovePartsStartingWithPipe() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("src/test/resources/stopwords.txt");
        Assert.assertNotNull(stopwords);
        Assert.assertTrue(stopwords.contains("one"));
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies not add empty stopwords
     */
    @Test
    public void loadStopwords_shouldNotAddEmptyStopwords() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("src/test/resources/stopwords.txt");
        Assert.assertNotNull(stopwords);
        Assert.assertFalse(stopwords.contains(""));
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies throw IllegalArgumentException if stopwordsFilePath empty
     */
    @Test(expected = IllegalArgumentException.class)
    public void loadStopwords_shouldThrowIllegalArgumentExceptionIfStopwordsFilePathEmpty() throws Exception {
        Configuration.loadStopwords(null);
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies throw FileNotFoundException if file does not exist
     */
    @Test(expected = FileNotFoundException.class)
    public void loadStopwords_shouldThrowFileNotFoundExceptionIfFileDoesNotExist() throws Exception {
        Configuration.loadStopwords("src/test/resources/startwords.txt");
    }

    /**
     * @see Configuration#getStopwords()
     * @verifies return all stopwords
     */
    @Test
    public void getStopwords_shouldReturnAllStopwords() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getConfiguration().getStopwords().size());
    }

    /**
     * @see Configuration#getStopwordsFilePath()
     * @verifies return correct value
     */
    @Test
    public void getStopwordsFilePath_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("src/test/resources/stopwords.txt", DataManager.getInstance().getConfiguration().getStopwordsFilePath());
    }

    /**
     * @see Configuration#getSubthemeMainTheme()
     * @verifies return correct value
     */
    @Test
    public void getSubthemeMainTheme_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("mainTheme_value", DataManager.getInstance().getConfiguration().getSubthemeMainTheme());
    }

    /**
     * @see Configuration#getSubthemeDiscriminatorField()
     * @verifies return correct value
     */
    @Test
    public void getSubthemeDiscriminatorField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("FACET_VIEWERSUBTHEME", DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    //    /**
    //     * @see ConfigurationHelper#getSubthemeMap()
    //     * @verifies return all configured elements
    //     */
    //    @Test
    //    public void getSubthemeMap_shouldReturnAllConfiguredElements() throws Exception {
    //        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getSubthemeMap().size());
    //    }

    /**
     * @see Configuration#getTagCloudSampleSize(String)
     * @verifies return correct value for existing fields
     */
    @Test
    public void getTagCloudSampleSize_shouldReturnCorrectValueForExistingFields() throws Exception {
        Assert.assertEquals(20, DataManager.getInstance().getConfiguration().getTagCloudSampleSize("MD_TITLE"));
    }

    /**
     * @see Configuration#getTagCloudSampleSize(String)
     * @verifies return INT_MAX for other fields
     */
    @Test
    public void getTagCloudSampleSize_shouldReturnINT_MAXForOtherFields() throws Exception {
        Assert.assertEquals(Integer.MAX_VALUE, DataManager.getInstance().getConfiguration().getTagCloudSampleSize("NONEXISTING_FIELD"));
    }

    /**
     * @see Configuration#getTheme()
     * @verifies return correct value
     */
    @Test
    public void getTheme_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("mainTheme_value", DataManager.getInstance().getConfiguration().getTheme());
    }

    /**
     * @see Configuration#getName()
     * @verifies return correct value
     */
    @Test
    public void getName_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("Goobi viewer TEST", DataManager.getInstance().getConfiguration().getName());
    }

    /**
     * @see Configuration#getDescription()
     * @verifies return correct value
     */
    @Test
    public void getDescription_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("Goobi viewer TEST desc", DataManager.getInstance().getConfiguration().getDescription());
    }

    /**
     * @see Configuration#getThumbnailsHeight()
     * @verifies return correct value
     */
    @Test
    public void getThumbnailsHeight_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(11, DataManager.getInstance().getConfiguration().getThumbnailsHeight());
    }

    /**
     * @see Configuration#getThumbnailsWidth()
     * @verifies return correct value
     */
    @Test
    public void getThumbnailsWidth_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(10, DataManager.getInstance().getConfiguration().getThumbnailsWidth());
    }

    @Test
    public void getThumbnailsCompressionTest() throws Exception {
        Assert.assertEquals(30, DataManager.getInstance().getConfiguration().getThumbnailsCompression());
    }

    /**
     * @see Configuration#getTitleBarMetadata()
     * @verifies return all configured metadata elements
     */
    @Test
    public void getTitleBarMetadata_shouldReturnAllConfiguredMetadataElements() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getTitleBarMetadata().size());
    }

    /**
     * @see Configuration#getUnconditionalImageAccessMaxWidth()
     * @verifies return correct value
     */
    @Test
    public void getUnconditionalImageAccessMaxWidth_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(1, DataManager.getInstance().getConfiguration().getUnconditionalImageAccessMaxWidth());
    }

    /**
     * @see Configuration#getViewerHome()
     * @verifies return correct value
     */
    @Test
    public void getViewerHome_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("src/test/resources/data/viewer/", DataManager.getInstance().getConfiguration().getViewerHome());
    }

    /**
     * @see Configuration#getViewerThumbnailsPerPage()
     * @verifies return correct value
     */
    @Test
    public void getViewerThumbnailsPerPage_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(9, DataManager.getInstance().getConfiguration().getViewerThumbnailsPerPage());
    }

    /**
     * @see Configuration#getWatermarkIdField()
     * @verifies return correct value
     */
    @Test
    public void getWatermarkIdField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(Collections.singletonList("watermarkIdField_value"), DataManager.getInstance().getConfiguration().getWatermarkIdField());
    }

    /**
     * @see Configuration#getWatermarkTextConfiguration()
     * @verifies return all configured elements in the correct order
     */
    @Test
    public void getWatermarkTextConfiguration_shouldReturnAllConfiguredElementsInTheCorrectOrder() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getConfiguration().getWatermarkTextConfiguration().size());
    }

    /**
     * @see Configuration#getZoomFullscreenViewType()
     * @verifies return correct value
     */
    @Test
    public void getZoomFullscreenViewType_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("classic", DataManager.getInstance().getConfiguration().getZoomFullscreenViewType());
    }

    /**
     * @see Configuration#getZoomImageViewType()
     * @verifies return correct value
     */
    @Test
    public void getZoomImageViewType_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("openSeadragon", DataManager.getInstance().getConfiguration().getImageViewType());
    }

    /**
     * @see Configuration#isBookmarksEnabled()
     * @verifies return correct value
     */
    @Test
    public void isBookshelvesEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isBookmarksEnabled());
    }

    /**
     * @see Configuration#isBrowsingMenuEnabled()
     * @verifies return correct value
     */
    @Test
    public void isBrowsingMenuEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isBrowsingMenuEnabled());
    }

    /**
     * @see Configuration#isDisplaySearchResultNavigation()
     * @verifies return correct value
     */
    @Test
    public void isDisplaySearchResultNavigation_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isDisplaySearchResultNavigation());
    }

    /**
     * @see Configuration#isDisplayStatistics()
     * @verifies return correct value
     */
    @Test
    public void isDisplayStatistics_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayStatistics());
    }

    /**
     * @see Configuration#isDisplayTagCloudNavigation()
     * @verifies return correct value
     */
    @Test
    public void isDisplayTagCloudNavigation_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayTagCloudNavigation());
    }

    /**
     * @see Configuration#isDisplayTagCloudStartpage()
     * @verifies return correct value
     */
    @Test
    public void isDisplayTagCloudStartpage_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayTagCloudStartpage());
    }

    /**
     * @see Configuration#isDisplayUserNavigation()
     * @verifies return correct value
     */
    @Test
    public void isDisplayUserNavigation_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayUserNavigation());
    }

    /**
     * @see Configuration#isAddDublinCoreMetaTags()
     * @verifies return correct value
     */
    @Test
    public void isAddDublinCoreMetaTags_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isAddDublinCoreMetaTags());
    }

    /**
     * @see Configuration#isAddHighwirePressMetaTags()
     * @verifies return correct value
     */
    @Test
    public void isAddHighwirePressMetaTags_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isAddHighwirePressMetaTags());
    }

    /**
     * @see Configuration#getMetadataParamNumber()
     * @verifies return correct value
     */
    @Test
    public void getMetadataParamNumber_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getConfiguration().getMetadataParamNumber());
    }

    /**
     * @see Configuration#isMetadataPdfEnabled()
     * @verifies return correct value
     */
    @Test
    public void isMetadataPdfEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isMetadataPdfEnabled());
    }

    /**
     * @see Configuration#isOriginalContentDownloads()
     * @verifies return correct value
     */
    @Test
    public void isDisplaySidebarWidgetDownloads_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetDownloads());
    }

    /**
     * @see Configuration#getSidebarWidgetDownloadsIntroductionText()
     * @verifies return correct value
     */
    @Test
    public void getSidebarWidgetDownloadsIntroductionText_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("MASTERVALUE_DOWNLOADS_INTRO", DataManager.getInstance().getConfiguration().getSidebarWidgetDownloadsIntroductionText());
    }

    @Test
    public void getHideDownloadFileRegex_returnConfiguredValue() throws Exception {
        Assert.assertEquals("(wug_.*|AK_.*)", DataManager.getInstance().getConfiguration().getHideDownloadFileRegex());
    }

    /**
     * @see Configuration#isGeneratePdfInTaskManager()
     * @verifies return correct value
     */
    @Test
    public void isGeneratePdfInTaskManager_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isGeneratePdfInTaskManager());
    }

    /**
     * @see Configuration#isPdfApiDisabled()
     * @verifies return correct value
     */
    @Test
    public void isPdfApiDisabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isPdfApiDisabled());
    }

    /**
     * @see Configuration#isTitlePdfEnabled()
     * @verifies return correct value
     */
    @Test
    public void isTitlePdfEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isTitlePdfEnabled());
    }

    /**
     * @see Configuration#isTocPdfEnabled()
     * @verifies return correct value
     */
    @Test
    public void isTocPdfEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isTocPdfEnabled());
    }

    /**
     * @see Configuration#isPagePdfEnabled()
     * @verifies return correct value
     */
    @Test
    public void isPagePdfEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isPagePdfEnabled());
    }

    /**
     * @see Configuration#isDocHierarchyPdfEnabled()
     * @verifies return correct value
     */
    @Test
    public void isDocHierarchyPdfEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isDocHierarchyPdfEnabled());
    }

    /**
     * @see Configuration#isTitleEpubEnabled()
     * @verifies return correct value
     */
    @Test
    public void isTitleEpubEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isTitleEpubEnabled());
    }

    /**
     * @see Configuration#isTocEpubEnabled()
     * @verifies return correct value
     */
    @Test
    public void isTocEpubEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isTocEpubEnabled());
    }

    /**
     * @see Configuration#isMetadataEpubEnabled()
     * @verifies return correct value
     */
    @Test
    public void isMetadataEpubEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isMetadataEpubEnabled());
    }

    /**
     * @see Configuration#getDownloadFolder(String)
     * @verifies return correct value for pdf
     */
    @Test
    public void getDownloadFolder_shouldReturnCorrectValueForPdf() throws Exception {
        Assert.assertEquals("/opt/digiverso/viewer/download_test_pdf", DataManager.getInstance().getConfiguration().getDownloadFolder("pdf"));
    }

    /**
     * @see Configuration#getDownloadFolder(String)
     * @verifies return correct value for epub
     */
    @Test
    public void getDownloadFolder_shouldReturnCorrectValueForEpub() throws Exception {
        Assert.assertEquals("/opt/digiverso/viewer/download_test_epub", DataManager.getInstance().getConfiguration().getDownloadFolder("epub"));
    }

    /**
     * @see Configuration#getDownloadFolder(String)
     * @verifies return empty string if type unknown
     */
    @Test
    public void getDownloadFolder_shouldReturnEmptyStringIfTypeUnknown() throws Exception {
        Assert.assertEquals("", DataManager.getInstance().getConfiguration().getDownloadFolder("xxx"));
    }

    /**
     * @see Configuration#isPreventProxyCaching()
     * @verifies return correct value
     */
    @Test
    public void isPreventProxyCaching_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isPreventProxyCaching());
    }

    /**
     * @see Configuration#isSolrCompressionEnabled()
     * @verifies return correct value
     */
    @Test
    public void isSolrCompressionEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isSolrCompressionEnabled());
    }

    /**
     * @see Configuration#isSolrBackwardsCompatible()
     * @verifies return correct value
     */
    @Test
    public void isSolrBackwardsCompatible_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isSolrBackwardsCompatible());
    }

    /**
     * @see Configuration#isShowSidebarEventMetadata()
     * @verifies return correct value
     */
    @Test
    public void isShowSidebarEventMetadata_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isShowSidebarEventMetadata());
    }

    /**
     * @see Configuration#isShowRecordLabelIfNoOtherViews()
     * @verifies return correct value
     */
    @Test
    public void isShowRecordLabelIfNoOtherViews_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isShowRecordLabelIfNoOtherViews());
    }

    /**
     * @see Configuration#isSidebarFulltextLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarFulltextLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarFulltextLinkVisible());
    }

    /**
     * @see Configuration#isSidebarMetadataViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarMetadataLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarMetadataViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarPageViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarPageLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarPageViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarCalendarViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarCalendarLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarCalendarViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarThumbsViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarThumbsLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarThumbsViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarOpacLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarOpacLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isSidebarOpacLinkVisible());
    }

    /**
     * @see Configuration#isSidebarTocViewLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarTocLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarTocViewLinkVisible());
    }

    /**
     * @see Configuration#isSidebarTocWidgetVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarTocVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarTocWidgetVisible());
    }

    /**
     * @see Configuration#isSidebarTocWidgetVisibleInFullscreen()
     * @verifies return correct value
     */
    @Test
    public void isSidebarTocVisibleInFullscreen_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarTocWidgetVisibleInFullscreen());
    }

    /**
     * @see Configuration#isSortingEnabled()
     * @verifies return correct value
     */
    @Test
    public void isSortingEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSortingEnabled());
    }

    /**
     * @see Configuration#getDefaultSortField()
     * @verifies return correct value
     */
    @Test
    public void getDefaultSortField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("SORT_DEFAULT1;SORT_DEFAULT2;SORT_DEFAULT3", DataManager.getInstance().getConfiguration().getDefaultSortField());
    }

    /**
     * @see Configuration#isUrnDoRedirect()
     * @verifies return correct value
     */
    @Test
    public void isUrnDoRedirect_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isUrnDoRedirect());
    }

    /**
     * @see Configuration#isUserCommentsEnabled()
     * @verifies return correct value
     */
    @Test
    public void isUserCommentsEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isUserCommentsEnabled());
    }

    /**
     * @see Configuration#getUserCommentsConditionalQuery()
     * @verifies return correct value
     */
    @Test
    public void getUserCommentsConditionalQuery_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("DC:varia", DataManager.getInstance().getConfiguration().getUserCommentsConditionalQuery());
    }

    /**
     * @see Configuration#useTiles()
     * @verifies return correct value
     */
    @Test
    public void useTiles_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().useTiles());
    }

    /**
     * @see Configuration#useTilesFullscreen()
     * @verifies return correct value
     */
    @Test
    public void useTilesFullscreen_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().useTilesFullscreen());
    }

    /**
     * @see Configuration#getPageType(PageType)
     * @verifies return null for non configured type
     */
    @Test
    public void getPageType_shouldReturnNullForNonConfiguredType() throws Exception {
        Assert.assertNull(DataManager.getInstance().getConfiguration().getPageType(PageType.term));
    }

    /**
     * @see Configuration#getSidebarMetadataForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    public void getSidebarMetadataForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate("Map").size());
    }

    /**
     * @see Configuration#getSortFields()
     * @verifies return return all configured elements
     */
    @Test
    public void getSortFields_shouldReturnReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getConfiguration().getSortFields().size());
    }

    /**
     * @see Configuration#getStaticSortFields()
     * @verifies return return all configured elements
     */
    @Test
    public void getStaticSortFields_shouldReturnReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(1, DataManager.getInstance().getConfiguration().getStaticSortFields().size());
    }

    /**
     * @see Configuration#getViewerMaxImageHeight()
     * @verifies return correct value
     */
    @Test
    public void getViewerMaxImageHeight_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(7, DataManager.getInstance().getConfiguration().getViewerMaxImageHeight());
    }

    /**
     * @see Configuration#getViewerMaxImageScale()
     * @verifies return correct value
     */
    @Test
    public void getViewerMaxImageScale_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(8, DataManager.getInstance().getConfiguration().getViewerMaxImageScale());
    }

    /**
     * @see Configuration#getViewerMaxImageWidth()
     * @verifies return correct value
     */
    @Test
    public void getViewerMaxImageWidth_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(6, DataManager.getInstance().getConfiguration().getViewerMaxImageWidth());
    }

    /**
     * @see Configuration#getSidebarMetadataForTemplate(String)
     * @verifies return empty list if template not found
     */
    @Test
    public void getSidebarMetadataForTemplate_shouldReturnEmptyListIfTemplateNotFound() throws Exception {
        Assert.assertEquals(0, DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate("nonexistant").size());
    }

    /**
     * @see Configuration#getSidebarMetadataForTemplate(String)
     * @verifies return empty list if template is null
     */
    @Test
    public void getSidebarMetadataForTemplate_shouldReturnEmptyListIfTemplateIsNull() throws Exception {
        Assert.assertEquals(0, DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(null).size());
    }

    /**
     * @see Configuration#getNormdataFieldsForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    public void getNormdataFieldsForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getNormdataFieldsForTemplate("CORPORATION").size());
    }

    /**
     * @see Configuration#isDisplayTopstructLabel()
     * @verifies return correct value
     */
    @Test
    public void isDisplayTopstructLabel_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isDisplayTopstructLabel());
    }

    /**
     * @see Configuration#getAdvancedSearchDefaultItemNumber()
     * @verifies return correct value
     */
    @Test
    public void getAdvancedSearchDefaultItemNumber_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getConfiguration().getAdvancedSearchDefaultItemNumber());
    }

    /**
     * @see Configuration#getAdvancedSearchFields()
     * @verifies return all values
     */
    @Test
    public void getAdvancedSearchFields_shouldReturnAllValues() throws Exception {
        List<AdvancedSearchFieldConfiguration> result = DataManager.getInstance().getConfiguration().getAdvancedSearchFields();
        Assert.assertEquals(11, result.size());
        Assert.assertTrue(result.get(0).isHierarchical());
        Assert.assertTrue(result.get(5).isRange());
        Assert.assertTrue(result.get(1).isUntokenizeForPhraseSearch());
        Assert.assertEquals("#SEPARATOR1#", result.get(7).getField());
        Assert.assertEquals("-----", result.get(7).getLabel());
        Assert.assertTrue(result.get(7).isDisabled());
    }

    /**
     * @see Configuration#isAdvancedSearchFieldHierarchical(String)
     * @verifies return correct value
     */
    @Test
    public void isAdvancedSearchFieldHierarchical_shouldReturnCorrectValue() throws Exception {

    }

    /**
     * @see Configuration#isAdvancedSearchFieldRange(String)
     * @verifies return correct value
     */
    @Test
    public void isAdvancedSearchFieldRange_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldRange(SolrConstants.DC));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldRange("MD_YEARPUBLISH"));
    }

    /**
     * @see Configuration#isAdvancedSearchFieldUntokenizeForPhraseSearch(String)
     * @verifies return correct value
     */
    @Test
    public void isAdvancedSearchFieldUntokenizeForPhraseSearch_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldUntokenizeForPhraseSearch(SolrConstants.DC));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldUntokenizeForPhraseSearch("MD_TITLE"));
    }

    /**
     * @see Configuration#getAdvancedSearchFieldSeparatorLabel(String)
     * @verifies return correct value
     */
    @Test
    public void getAdvancedSearchFieldSeparatorLabel_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("-----", DataManager.getInstance().getConfiguration().getAdvancedSearchFieldSeparatorLabel("#SEPARATOR1#"));
    }

    /**
     * @see Configuration#getSidebarTocCollapseLengthThreshold()
     * @verifies return correct value
     */
    @Test
    public void getSidebarTocCollapseLengthThreshold_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(141, DataManager.getInstance().getConfiguration().getSidebarTocCollapseLengthThreshold());
    }

    /**
     * @see Configuration#getSidebarTocLowestLevelToCollapseForLength()
     * @verifies return correct value
     */
    @Test
    public void getSidebarTocLowestLevelToCollapseForLength_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(333, DataManager.getInstance().getConfiguration().getSidebarTocLowestLevelToCollapseForLength());
    }

    /**
     * @see Configuration#getDisplayTitleBreadcrumbs()
     * @verifies return correct value
     */
    @Test
    public void getDisplayTitleBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().getDisplayTitleBreadcrumbs());
    }

    /**
     * @see Configuration#getIncludeAnchorInTitleBreadcrumbs()
     * @verifies return correct value
     */
    @Test
    public void getIncludeAnchorInTitleBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().getIncludeAnchorInTitleBreadcrumbs());
    }

    /**
     * @see Configuration#getTitleBreadcrumbsMaxTitleLength()
     * @verifies return correct value
     */
    @Test
    public void getTitleBreadcrumbsMaxTitleLength_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(20, DataManager.getInstance().getConfiguration().getTitleBreadcrumbsMaxTitleLength());
    }

    /**
     * @see Configuration#isDisplaySearchRssLinks()
     * @verifies return correct value
     */
    @Test
    public void isDisplaySearchRssLinks_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isDisplaySearchRssLinks());
    }

    /**
     * @see Configuration#getCalendarDocStructTypes()
     * @verifies return all configured elements
     */
    @Test
    public void getCalendarDocStructTypes_shouldReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getCalendarDocStructTypes().size());
    }

    /**
     * @see Configuration#getAllDrillDownFields()
     * @verifies return correct order
     */
    @Test
    public void getAllDrillDownFields_shouldReturnCorrectOrder() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getAllDrillDownFields();
        Assert.assertEquals(4, result.size());
        Assert.assertEquals("DC", result.get(0));
        Assert.assertEquals("YEAR", result.get(1));
        Assert.assertEquals("MD_CREATOR", result.get(2));
        Assert.assertEquals("MD_PLACEPUBLISH", result.get(3));
    }

    /**
     * @see Configuration#getDrillDownFields()
     * @verifies return all values
     */
    @Test
    public void getDrillDownFields_shouldReturnAllValues() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getDrillDownFields().size());
    }

    /**
     * @see Configuration#getHierarchicalDrillDownFields()
     * @verifies return all values
     */
    @Test
    public void getHierarchicalDrillDownFields_shouldReturnAllValues() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getHierarchicalDrillDownFields().size());
    }

    /**
     * @see Configuration#getInitialDrillDownElementNumber()
     * @verifies return correct value
     */
    @Test
    public void getInitialDrillDownElementNumber_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber(SolrConstants.DC));
        Assert.assertEquals(16, DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber("MD_PLACEPUBLISH"));
        Assert.assertEquals(23, DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber(null));
    }

    /**
     * @see Configuration#getInitialDrillDownElementNumber(String)
     * @verifies return default value if field not found
     */
    @Test
    public void getInitialDrillDownElementNumber_shouldReturnDefaultValueIfFieldNotFound() throws Exception {
        Assert.assertEquals(-1, DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber("YEAR"));
    }

    /**
     * @see Configuration#getPriorityValuesForDrillDownField(String)
     * @verifies return return all configured elements for regular fields
     */
    @Test
    public void getPriorityValuesForDrillDownField_shouldReturnReturnAllConfiguredElementsForRegularFields() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getPriorityValuesForDrillDownField("MD_PLACEPUBLISH");
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("val1", result.get(0));
        Assert.assertEquals("val2", result.get(1));
        Assert.assertEquals("val3", result.get(2));
    }

    /**
     * @see Configuration#getPriorityValuesForDrillDownField(String)
     * @verifies return return all configured elements for hierarchical fields
     */
    @Test
    public void getPriorityValuesForDrillDownField_shouldReturnReturnAllConfiguredElementsForHierarchicalFields() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getPriorityValuesForDrillDownField("DC");
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("collection2", result.get(0));
        Assert.assertEquals("collection1", result.get(1));
    }

    /**
     * @see Configuration#getLabelFieldForDrillDownField(String)
     * @verifies return correct value
     */
    @Test
    public void getLabelFieldForDrillDownField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("MD_FIELDLABEL", DataManager.getInstance().getConfiguration().getLabelFieldForDrillDownField(SolrConstants.YEAR));
        Assert.assertEquals("MD_FIRSTNAME", DataManager.getInstance().getConfiguration().getLabelFieldForDrillDownField("MD_CREATOR"));
    }

    /**
     * @see Configuration#getLabelFieldForDrillDownField(String)
     * @verifies return null if no value found
     */
    @Test
    public void getLabelFieldForDrillDownField_shouldReturnNullIfNoValueFound() throws Exception {
        Assert.assertNull(DataManager.getInstance().getConfiguration().getLabelFieldForDrillDownField("MD_PLACEPUBLISH"));
    }

    @Test
    public void getSortOrderTest() {
        Assert.assertEquals("numerical", DataManager.getInstance().getConfiguration().getSortOrder("YEAR"));
        Assert.assertEquals("default", DataManager.getInstance().getConfiguration().getSortOrder("MD_PLACEPUBLISH"));
        Assert.assertEquals("alphabetical", DataManager.getInstance().getConfiguration().getSortOrder("MD_CREATOR"));
    }

    /**
     * @see Configuration#isAdvancedSearchEnabled()
     * @verifies return correct value
     */
    @Test
    public void isAdvancedSearchEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isAdvancedSearchEnabled());
    }

    /**
     * @see Configuration#isCalendarSearchEnabled()
     * @verifies return correct value
     */
    @Test
    public void isCalendarSearchEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isCalendarSearchEnabled());
    }

    /**
     * @see Configuration#isTimelineSearchEnabled()
     * @verifies return correct value
     */
    @Test
    public void isTimelineSearchEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isTimelineSearchEnabled());
    }

    /**
     * @see Configuration#getStaticQuerySuffix()
     * @verifies return correct value
     */
    @Test
    public void getStaticQuerySuffix_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("-BOOL_HIDE:true", DataManager.getInstance().getConfiguration().getStaticQuerySuffix());
    }

    /**
     * @see Configuration#getNextVersionIdentifierField()
     * @verifies return correct value
     */
    @Test
    public void getNextVersionIdentifierField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("MD_PREVIOUS_VERSION", DataManager.getInstance().getConfiguration().getPreviousVersionIdentifierField());
    }

    /**
     * @see Configuration#getPreviousVersionIdentifierField()
     * @verifies return correct value
     */
    @Test
    public void getPreviousVersionIdentifierField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("MD_NEXT_VERSION", DataManager.getInstance().getConfiguration().getNextVersionIdentifierField());
    }

    /**
     * @see Configuration#getVersionLabelField()
     * @verifies return correct value
     */
    @Test
    public void getVersionLabelField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("MD_VERSIONLABEL", DataManager.getInstance().getConfiguration().getVersionLabelField());
    }

    /**
     * @see Configuration#getCmsTextFolder()
     * @verifies return correct value
     */
    @Test
    public void getCmsTextFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("cms", DataManager.getInstance().getConfiguration().getCmsTextFolder());
    }

    /**
     * @see Configuration#isForceJpegConversion()
     * @verifies return correct value
     */
    @Test
    public void isForceJpegConversion_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isForceJpegConversion());
    }

    /**
     * @see Configuration#getUserCommentsNotificationEmailAddresses()
     * @verifies return all configured elements
     */
    @Test
    public void getUserCommentsNotificationEmailAddresses_shouldReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getUserCommentsNotificationEmailAddresses().size());
    }

    /**
     * @see Configuration#getAltoFolder()
     * @verifies return correct value
     */
    @Test
    public void getAltoFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("alto", DataManager.getInstance().getConfiguration().getAltoFolder());
    }

    /**
     * @see Configuration#getAltoCrowdsourcingFolder()
     * @verifies return correct value
     */
    @Test
    public void getAltoCrowdsourcingFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("alto_crowd", DataManager.getInstance().getConfiguration().getAltoCrowdsourcingFolder());
    }

    /**
     * @see Configuration#getFulltextFolder()
     * @verifies return correct value
     */
    @Test
    public void getFulltextFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("fulltext", DataManager.getInstance().getConfiguration().getFulltextFolder());
    }

    /**
     * @see Configuration#getFulltextCrowdsourcingFolder()
     * @verifies return correct value
     */
    @Test
    public void getFulltextCrowdsourcingFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("fulltext_crowd", DataManager.getInstance().getConfiguration().getFulltextCrowdsourcingFolder());
    }

    /**
     * @see Configuration#getAbbyyFolder()
     * @verifies return correct value
     */
    @Test
    public void getAbbyyFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("abbyy", DataManager.getInstance().getConfiguration().getAbbyyFolder());
    }

    /**
     * @see Configuration#getTeiFolder()
     * @verifies return correct value
     */
    @Test
    public void getTeiFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("tei", DataManager.getInstance().getConfiguration().getTeiFolder());
    }

    /**
     * @see Configuration#getCmdiFolder()
     * @verifies return correct value
     */
    @Test
    public void getCmdiFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("cmdi", DataManager.getInstance().getConfiguration().getCmdiFolder());
    }

    /**
     * @see Configuration#getAnnotationFolder()
     * @verifies return correct value
     */
    @Test
    public void getAnnotationFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("annotations", DataManager.getInstance().getConfiguration().getAnnotationFolder());
    }

    /**
     * @see Configuration#getEndYearForTimeline()
     * @verifies return correct value
     */
    @Test
    public void getEndYearForTimeline_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("1865", DataManager.getInstance().getConfiguration().getEndYearForTimeline());
    }

    /**
     * @see Configuration#getStartYearForTimeline()
     * @verifies return correct value
     */
    @Test
    public void getStartYearForTimeline_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("1861", DataManager.getInstance().getConfiguration().getStartYearForTimeline());
    }

    /**
     * @see Configuration#getTimelineHits()
     * @verifies return correct value
     */
    @Test
    public void getTimelineHits_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("120", DataManager.getInstance().getConfiguration().getTimelineHits());
    }

    /**
     * @see Configuration#isDisplayTimeMatrix()
     * @verifies return correct value
     */
    @Test
    public void isDisplayTimeMatrix_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isDisplayTimeMatrix());
    }

    /**
     * @see Configuration#showThumbnailsInToc()
     * @verifies return correct value
     */
    @Test
    public void showThumbnailsInToc_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().showThumbnailsInToc());
    }

    /**
     * @see Configuration#getPiwikBaseURL()
     * @verifies return correct value
     */
    @Test
    public void getPiwikBaseURL_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("baseURL_value", DataManager.getInstance().getConfiguration().getPiwikBaseURL());
    }

    /**
     * @see Configuration#getPiwikSiteID()
     * @verifies return correct value
     */
    @Test
    public void getPiwikSiteID_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("siteID_value", DataManager.getInstance().getConfiguration().getPiwikSiteID());
    }

    /**
     * @see Configuration#isPiwikTrackingEnabled()
     * @verifies return correct value
     */
    @Test
    public void isPiwikTrackingEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isPiwikTrackingEnabled());
    }

    /**
     * @see Configuration#getSearchFilters()
     * @verifies return all configured elements
     */
    @Test
    public void getSearchFilters_shouldReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getConfiguration().getSearchFilters().size());
    }

    /**
     * @see Configuration#getAnchorThumbnailMode()
     * @verifies return correct value
     */
    @Test
    public void getAnchorThumbnailMode_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("FIRSTVOLUME", DataManager.getInstance().getConfiguration().getAnchorThumbnailMode());
    }

    /**
     * @see Configuration#isDisplayCollectionBrowsing()
     * @verifies return correct value
     */
    @Test
    public void isDisplayCollectionBrowsing_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayCollectionBrowsing());
    }

    /**
     * @see Configuration#getDisplayTitlePURL()
     * @verifies return correct value
     */
    @Test
    public void getDisplayTitlePURL_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isDisplayTitlePURL());
    }

    /**
     * @see Configuration#getWebApiFields()
     * @verifies return all configured elements
     */
    @Test
    public void getWebApiFields_shouldReturnAllConfiguredElements() throws Exception {
        List<Map<String, String>> fields = DataManager.getInstance().getConfiguration().getWebApiFields();
        Assert.assertEquals(2, fields.size());
        Assert.assertEquals("json1", fields.get(0).get("jsonField"));
        Assert.assertEquals("lucene1", fields.get(0).get("luceneField"));
        Assert.assertEquals("true", fields.get(0).get("multivalue"));
        Assert.assertEquals(null, fields.get(1).get("multivalue"));
    }

    /**
     * @see Configuration#getDbPersistenceUnit()
     * @verifies return correct value
     */
    @Test
    public void getDbPersistenceUnit_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("intranda_viewer_test", DataManager.getInstance().getConfiguration().getDbPersistenceUnit());
    }

    /**
     * @see Configuration#useCustomNavBar()
     * @verifies return correct value
     */
    @Test
    public void useCustomNavBar_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().useCustomNavBar());
    }

    /**
     * @see Configuration#getCmsMediaFolder()
     * @verifies return correct value
     */
    @Test
    public void getCmsMediaFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("cmsMediaFolder_value", DataManager.getInstance().getConfiguration().getCmsMediaFolder());
    }

    /**
     * @see Configuration#getCmsMediaFolder()
     * @verifies return correct value
     */
    @Test
    public void getCmsTemplateFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("cmsTemplateFolder_value", DataManager.getInstance().getConfiguration().getCmsTemplateFolder());
    }

    /**
     * @see Configuration#getCmsClassifications()
     * @verifies return all configured elements
     */
    @Test
    public void getCmsClassifications_shouldReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getConfiguration().getCmsClassifications().size());
        Assert.assertEquals("classification1", DataManager.getInstance().getConfiguration().getCmsClassifications().get(0));
        Assert.assertEquals("classification2", DataManager.getInstance().getConfiguration().getCmsClassifications().get(1));
        Assert.assertEquals("classification3", DataManager.getInstance().getConfiguration().getCmsClassifications().get(2));
        Assert.assertEquals("classification4", DataManager.getInstance().getConfiguration().getCmsClassifications().get(3));
    }

    @Test
    public void getCmsMediaDisplayWidthTest() {
        Assert.assertEquals(600, DataManager.getInstance().getConfiguration().getCmsMediaDisplayWidth());
    }

    @Test
    public void getCmsMediaDisplaHeightTest() {
        Assert.assertEquals(800, DataManager.getInstance().getConfiguration().getCmsMediaDisplayHeight());
    }

    /**
     * @see Configuration#isSearchSavingEnabled()
     * @verifies return correct value
     */
    @Test
    public void isSearchSavingEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isSearchSavingEnabled());
    }

    @Test
    public void getImageViewZoomScalesTest() throws ViewerConfigurationException {
        List<String> scales = DataManager.getInstance().getConfiguration().getImageViewZoomScales();
        Assert.assertEquals("600", scales.get(0));
        Assert.assertEquals("1000", scales.get(1));
        Assert.assertEquals("1500", scales.get(2));
        Assert.assertEquals("3000", scales.get(3));
    }

    @Test
    public void getFullscreenViewZoomScalesTest() throws ViewerConfigurationException {
        List<String> scales = DataManager.getInstance().getConfiguration().getImageViewZoomScales(PageType.viewFullscreen, null);
        Assert.assertEquals("1000", scales.get(0));
        Assert.assertEquals("2000", scales.get(1));
        Assert.assertEquals("3000", scales.get(2));
    }

    @Test
    public void getImageViewTileSizesTest() throws ViewerConfigurationException {
        Map<Integer, List<Integer>> tiles = DataManager.getInstance().getConfiguration().getTileSizes();
        Assert.assertEquals(512, tiles.keySet().iterator().next(), 0);
        Assert.assertEquals(1, tiles.get(512).get(0), 0);
        Assert.assertEquals(2, tiles.get(512).get(1), 0);
        Assert.assertEquals(3, tiles.get(512).get(2), 0);
    }

    @Test
    public void getFullscreenTileSizesTest() throws ViewerConfigurationException {
        Map<Integer, List<Integer>> tiles = DataManager.getInstance().getConfiguration().getTileSizes(PageType.viewFullscreen, null);
        Assert.assertEquals(1024, tiles.keySet().iterator().next(), 0);
        Assert.assertEquals(2, tiles.get(1024).get(0), 0);
        Assert.assertEquals(4, tiles.get(1024).get(1), 0);
        Assert.assertEquals(8, tiles.get(1024).get(2), 0);
    }

    @Test
    public void getFooterHeightTest() throws ViewerConfigurationException {
        Assert.assertEquals(50, DataManager.getInstance().getConfiguration().getFooterHeight());
    }

    @Test
    public void getCrowdsourcingFooterHeightTest() throws ViewerConfigurationException {
        Assert.assertEquals(0, DataManager.getInstance().getConfiguration().getFooterHeight(PageType.editContent, null));
    }

    /**
     * @see Configuration#getUrnResolverUrl()
     * @verifies return correct value
     */
    @Test
    public void getUrnResolverUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("urnResolver_value", DataManager.getInstance().getConfiguration().getUrnResolverUrl());
    }

    /**
     * @see Configuration#getTocVolumeSortFieldsForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    public void getTocVolumeSortFieldsForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        List<StringPair> fields = DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate("CustomDocstruct");
        Assert.assertEquals(2, fields.size());
        Assert.assertEquals("CURRENTNOSORT", fields.get(0).getOne());
        Assert.assertEquals("desc", fields.get(0).getTwo());
        Assert.assertEquals("SORT_TITLE", fields.get(1).getOne());
        Assert.assertEquals("asc", fields.get(1).getTwo());
    }

    /**
     * @see Configuration#getTocVolumeSortFieldsForTemplate(String)
     * @verifies return default template configuration if template not found
     */
    @Test
    public void getTocVolumeSortFieldsForTemplate_shouldReturnDefaultTemplateConfigurationIfTemplateNotFound() throws Exception {
        List<StringPair> fields = DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate("notfound");
        Assert.assertEquals(1, fields.size());
        Assert.assertEquals("CURRENTNOSORT", fields.get(0).getOne());
        Assert.assertEquals("asc", fields.get(0).getTwo());
    }

    /**
     * @see Configuration#getTocVolumeSortFieldsForTemplate(String)
     * @verifies return default template configuration if template is null
     */
    @Test
    public void getTocVolumeSortFieldsForTemplate_shouldReturnDefaultTemplateConfigurationIfTemplateIsNull() throws Exception {
        List<StringPair> fields = DataManager.getInstance().getConfiguration().getTocVolumeSortFieldsForTemplate(null);
        Assert.assertEquals(1, fields.size());
        Assert.assertEquals("CURRENTNOSORT", fields.get(0).getOne());
        Assert.assertEquals("asc", fields.get(0).getTwo());
    }

    /**
     * @see Configuration#getTocVolumeGroupFieldForTemplate(String)
     * @verifies return correct value
     */
    @Test
    public void getTocVolumeGroupFieldForTemplate_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("GROUP", DataManager.getInstance().getConfiguration().getTocVolumeGroupFieldForTemplate("CustomDocstruct"));
    }

    /**
     * @see Configuration#isBoostTopLevelDocstructs()
     * @verifies return correct value
     */
    @Test
    public void isBoostTopLevelDocstructs_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isBoostTopLevelDocstructs());
    }

    /**
     * @see Configuration#isGroupDuplicateHits()
     * @verifies return correct value
     */
    @Test
    public void isGroupDuplicateHits_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isGroupDuplicateHits());
    }

    /**
     * @see Configuration#getRecordGroupIdentifierFields()
     * @verifies return all configured values
     */
    @Test
    public void getRecordGroupIdentifierFields_shouldReturnAllConfiguredValues() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getRecordGroupIdentifierFields().size());
    }

    /**
     * @see Configuration#getAncestorIdentifierFields()
     * @verifies return all configured values
     */
    @Test
    public void getAncestorIdentifierFields_shouldReturnAllConfiguredValues() throws Exception {
        List<String> list = DataManager.getInstance().getConfiguration().getAncestorIdentifierFields();
        Assert.assertNotNull(list);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(SolrConstants.PI_PARENT, list.get(0));
    }

    /**
     * @see Configuration#isTocListSiblingRecords()
     * @verifies return correctValue
     */
    @Test
    public void isTocListSiblingRecords_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isTocListSiblingRecords());
    }

    /**
     * @see Configuration#getTocAnchorGroupElementsPerPage()
     * @verifies return correct value
     */
    @Test
    public void getTocAnchorGroupElementsPerPage_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(10, DataManager.getInstance().getConfiguration().getTocAnchorGroupElementsPerPage());
    }

    /**
     * @see Configuration#getCollectionDisplayDepthForSearch(String)
     * @verifies return correct value
     */
    @Test
    public void getCollectionDisplayDepthForSearch_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getConfiguration().getCollectionDisplayDepthForSearch(SolrConstants.DC));
    }

    /**
     * @see Configuration#getCollectionDisplayDepthForSearch(String)
     * @verifies return -1 if no collection config was found
     */
    @Test
    public void getCollectionDisplayDepthForSearch_shouldReturn1IfNoCollectionConfigWasFound() throws Exception {
        Assert.assertEquals(-1, DataManager.getInstance().getConfiguration().getCollectionDisplayDepthForSearch("MD_NOSUCHFIELD"));
    }

    /**
     * @see Configuration#getCollectionHierarchyField()
     * @verifies return first field where hierarchy enabled
     */
    @Test
    public void getCollectionHierarchyField_shouldReturnFirstFieldWhereHierarchyEnabled() throws Exception {
        Assert.assertEquals("MD_KNOWLEDGEFIELD", DataManager.getInstance().getConfiguration().getCollectionHierarchyField());
    }

    /**
     * @see Configuration#isAddCollectionHierarchyToBreadcrumbs(String)
     * @verifies return correct value
     */
    @Test
    public void isAddCollectionHierarchyToBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isAddCollectionHierarchyToBreadcrumbs("DC"));
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isAddCollectionHierarchyToBreadcrumbs("MD_KNOWLEDGEFIELD"));
    }

    /**
     * @see Configuration#isAddCollectionHierarchyToBreadcrumbs(String)
     * @verifies return false if no collection config was found
     */
    @Test
    public void isAddCollectionHierarchyToBreadcrumbs_shouldReturnFalseIfNoCollectionConfigWasFound() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isAddCollectionHierarchyToBreadcrumbs("MD_NOSUCHFIELD"));
    }

    @Test
    public void testBrokenConfig() {
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer_broken.test.xml"));
        String localConfig = DataManager.getInstance().getConfiguration().getConfigLocalPath();
        Assert.assertEquals(localConfig, "src/test/resources/localConfig/");
        String viewerHome = DataManager.getInstance().getConfiguration().getViewerHome();
        Assert.assertEquals(viewerHome, "src/test/resources/data/viewer/");
        String dataRepositories = DataManager.getInstance().getConfiguration().getDataRepositoriesHome();
        Assert.assertEquals(dataRepositories, "src/test/resources/data/viewer/data/");
        DataManager.getInstance().injectConfiguration(new Configuration("src/test/resources/config_viewer.test.xml"));

    }

    /**
     * @see Configuration#getTranskribusDefaultCollection()
     * @verifies return correct value
     */
    @Test
    public void getTranskribusDefaultCollection_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("intranda_viewer", DataManager.getInstance().getConfiguration().getTranskribusDefaultCollection());
    }

    /**
     * @see Configuration#getTranskribusAllowedDocumentTypes()
     * @verifies return all configured elements
     */
    @Test
    public void getTranskribusAllowedDocumentTypes_shouldReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(2, DataManager.getInstance().getConfiguration().getTranskribusAllowedDocumentTypes().size());
    }

    /**
     * @see Configuration#getTranskribusRestApiUrl()
     * @verifies return correct value
     */
    @Test
    public void getTranskribusRestApiUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("https://transkribus.eu/TrpServerTesting/rest/", DataManager.getInstance().getConfiguration().getTranskribusRestApiUrl());
    }

    /**
     * @see Configuration#isTranskribusEnabled()
     * @verifies return correct value
     */
    @Test
    public void isTranskribusEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isTranskribusEnabled());
    }

    @Test
    public void isRememberImageRotation_test() {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isRememberImageRotation());
    }

    @Test
    public void isRememberImageZoom_test() {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isRememberImageZoom());
    }

    /**
     * @see Configuration#getDocstructTargetPageType(String)
     * @verifies return correct value
     */
    @Test
    public void getDocstructTargetPageType_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("toc", DataManager.getInstance().getConfiguration().getDocstructTargetPageType("Catalogue"));
    }

    /**
     * @see Configuration#getDocstructTargetPageType(String)
     * @verifies return null if docstruct not found
     */
    @Test
    public void getDocstructTargetPageType_shouldReturnNullIfDocstructNotFound() throws Exception {
        Assert.assertNull(DataManager.getInstance().getConfiguration().getDocstructTargetPageType("notfound"));
    }

    /**
     * @see Configuration#getFulltextPercentageWarningThreshold()
     * @verifies return correct value
     */
    @Test
    public void getFulltextPercentageWarningThreshold_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(99, DataManager.getInstance().getConfiguration().getFulltextPercentageWarningThreshold());
    }

    /**
     * @see Configuration#isUseViewerLocaleAsRecordLanguage()
     * @verifies return correct value
     */
    @Test
    public void isUseViewerLocaleAsRecordLanguage_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isUseViewerLocaleAsRecordLanguage());
    }

    /**
     * @see Configuration#getSearchExcelExportFields()
     * @verifies return all values
     */
    @Test
    public void getSearchExcelExportFields_shouldReturnAllValues() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getSearchExcelExportFields();
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(SolrConstants.PI, result.get(0));
        Assert.assertEquals(SolrConstants.LABEL, result.get(1));
    }

    /**
     * @see Configuration#isSearchExcelExportEnabled()
     * @verifies return correct value
     */
    @Test
    public void isSearchExcelExportEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isSearchExcelExportEnabled());
    }

    /**
     * @see Configuration#isAggregateHits()
     * @verifies return correct value
     */
    @Test
    public void isAggregateHits_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isAggregateHits());
    }

    /**
     * @see Configuration#isDisplayAdditionalMetadataEnabled()
     * @verifies return correct value
     */
    @Test
    public void isDisplayAdditionalMetadataEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isDisplayAdditionalMetadataEnabled());
    }

    /**
     * @see Configuration#getDisplayAdditionalMetadataIgnoreFields()
     * @verifies return correct values
     */
    @Test
    public void getDisplayAdditionalMetadataIgnoreFields_shouldReturnCorrectValues() throws Exception {
        List<String> results = DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataIgnoreFields();
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());
        Assert.assertEquals(SolrConstants.ISANCHOR, results.get(0));
        Assert.assertEquals(SolrConstants.ISWORK, results.get(1));
        Assert.assertEquals(SolrConstants.PI_TOPSTRUCT, results.get(2));
    }

    /**
     * @see Configuration#getDisplayAdditionalMetadataTranslateFields()
     * @verifies return correct values
     */
    @Test
    public void getDisplayAdditionalMetadataTranslateFields_shouldReturnCorrectValues() throws Exception {
        List<String> results = DataManager.getInstance().getConfiguration().getDisplayAdditionalMetadataTranslateFields();
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());
        Assert.assertEquals(SolrConstants.DC, results.get(0));
        Assert.assertEquals(SolrConstants.DOCSTRCT, results.get(1));
        Assert.assertEquals("MD_LANGUAGE", results.get(2));
    }

    /**
     * @see Configuration#isDisplayEmptyTocInSidebar()
     * @verifies return correct value
     */
    @Test
    public void isDisplayEmptyTocInSidebar_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isDisplayEmptyTocInSidebar());
    }

    @Test
    public void isDoublePageModeEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isDoublePageModeEnabled());
    }

    /**
     * @see Configuration#getSitelinksFilterQuery()
     * @verifies return correct value
     */
    @Test
    public void getSitelinksFilterQuery_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("ISWORK:true", DataManager.getInstance().getConfiguration().getSitelinksFilterQuery());
    }

    /**
     * @see Configuration#getSitelinksField()
     * @verifies return correct value
     */
    @Test
    public void getSitelinksField_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(SolrConstants._CALENDAR_YEAR, DataManager.getInstance().getConfiguration().getSitelinksField());
    }

    @Test
    public void getGetConfiguredCollections() {
        List<String> fields = DataManager.getInstance().getConfiguration().getConfiguredCollections();
        Assert.assertEquals(fields.size(), 3);
        Assert.assertTrue(fields.contains("DC"));
        Assert.assertTrue(fields.contains("MD_KNOWLEDGEFIELD"));
        Assert.assertTrue(fields.contains("MD_HIERARCHICALFIELD"));
    }

    /**
     * @see Configuration#isFullAccessForLocalhost()
     * @verifies return correct value
     */
    @Test
    public void isFullAccessForLocalhost_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isFullAccessForLocalhost());
    }

    /**
     * @see Configuration#getDocstrctWhitelistFilterSuffix()
     * @verifies return correct value
     */
    @Test
    public void getDocstrctWhitelistFilterSuffix_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("ISWORK:true OR ISANCHOR:true", DataManager.getInstance().getConfiguration().getDocstrctWhitelistFilterQuery());
    }

    /**
     * @see Configuration#getIIIFMetadataLabel(String)
     * @verifies return correct values
     */
    @Test
    public void getIIIFMetadataLabel_shouldReturnCorrectValues() throws Exception {
        Assert.assertEquals("", DataManager.getInstance().getConfiguration().getIIIFMetadataLabel("MD_*"));
        Assert.assertEquals("label_year", DataManager.getInstance().getConfiguration().getIIIFMetadataLabel("YEAR"));
        Assert.assertEquals("label_provenienz", DataManager.getInstance().getConfiguration().getIIIFMetadataLabel("Provenienz/MD_EVENT_DETAILS"));
        Assert.assertEquals("", DataManager.getInstance().getConfiguration().getIIIFMetadataLabel("/YEAR"));
    }

    /**
     * @see Configuration#getTwitterUserName()
     * @verifies return correct value
     */
    @Test
    public void getTwitterUserName_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("@goobi", DataManager.getInstance().getConfiguration().getTwitterUserName());
    }

    /**
     * @see Configuration#getMetadataFromSubnodeConfig(HierarchicalConfiguration,boolean)
     * @verifies load replace rules correctly
     */
    @Test
    public void getMetadataFromSubnodeConfig_shouldLoadReplaceRulesCorrectly() throws Exception {
        List<Metadata> metadataList = DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, "_DEFAULT");
        Assert.assertEquals(6, metadataList.size());
        Metadata mdTitle = metadataList.get(2);
        Assert.assertEquals("MD_TITLE", mdTitle.getLabel());
        Assert.assertEquals(1, mdTitle.getParams().size());
        Assert.assertEquals("foo", mdTitle.getParams().get(0).getReplaceRules().get(0).getKey());
        Assert.assertEquals("bar", mdTitle.getParams().get(0).getReplaceRules().get(0).getReplacement());
        Assert.assertEquals(MetadataReplaceRuleType.STRING, mdTitle.getParams().get(0).getReplaceRules().get(0).getType());
    }

    /**
     * @see Configuration#getDocstrctWhitelistFilterQuery()
     * @verifies return correct value
     */
    @Test
    public void getDocstrctWhitelistFilterQuery_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("ISWORK:true OR ISANCHOR:true", DataManager.getInstance().getConfiguration().getDocstrctWhitelistFilterQuery());
    }

    /**
     * @see Configuration#getReCaptchaSiteKey()
     * @verifies return correct value
     */
    @Test
    public void getReCaptchaSiteKey_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("6LetEyITAAAAAEAj7NTxgRXR6S_uhZrk9rn5HyB3", DataManager.getInstance().getConfiguration().getReCaptchaSiteKey());
    }

    /**
     * @see Configuration#getTaskManagerRestUrl()
     * @verifies return correct value
     */
    @Test
    public void getTaskManagerRestUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("taskmanager_url/rest", DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
    }

    /**
     * @see Configuration#getTaskManagerServiceUrl()
     * @verifies return correct value
     */
    @Test
    public void getTaskManagerServiceUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("taskmanager_url/service", DataManager.getInstance().getConfiguration().getTaskManagerServiceUrl());
    }

    /**
     * @see Configuration#getThemeRootPath()
     * @verifies return correct value
     */
    @Test
    public void getThemeRootPath_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("/opt/digiverso/goobi-viewer-theme-test/goobi-viewer-theme-mest/WebContent/resources/themes/",
                DataManager.getInstance().getConfiguration().getThemeRootPath());
    }

    /**
     * @see Configuration#getTocIndentation()
     * @verifies return correct value
     */
    @Test
    public void getTocIndentation_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(15, DataManager.getInstance().getConfiguration().getTocIndentation());
    }

    /**
     * @see Configuration#getTranskribusUserName()
     * @verifies return correct value
     */
    @Test
    public void getTranskribusUserName_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("transkribus_user", DataManager.getInstance().getConfiguration().getTranskribusUserName());
    }

    /**
     * @see Configuration#getTranskribusPassword()
     * @verifies return correct value
     */
    @Test
    public void getTranskribusPassword_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("transkribus_pwd", DataManager.getInstance().getConfiguration().getTranskribusPassword());
    }

    /**
     * @see Configuration#getDfgViewerUrl()
     * @verifies return correct value
     */
    @Test
    public void getDfgViewerUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("dfg-viewer_value", DataManager.getInstance().getConfiguration().getDfgViewerUrl());
    }

    /**
     * @see Configuration#isDisplayCrowdsourcingModuleLinks()
     * @verifies return correct value
     */
    @Test
    public void isDisplayCrowdsourcingModuleLinks_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isDisplayCrowdsourcingModuleLinks());
    }

    /**
     * @see Configuration#isDisplayTitlePURL()
     * @verifies return correct value
     */
    @Test
    public void isDisplayTitlePURL_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isDisplayTitlePURL());
    }

    /**
     * @see Configuration#isSearchInItemEnabled()
     * @verifies return true if the search field to search the current item/work is configured to be visible
     */
    @Test
    public void isSearchInItemEnabled_shouldReturnTrueIfTheSearchFieldToSearchTheCurrentItemworkIsConfiguredToBeVisible() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isSearchInItemEnabled());
    }

    /**
     * @see Configuration#isUseReCaptcha()
     * @verifies return correct value
     */
    @Test
    public void isUseReCaptcha_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isUseReCaptcha());
    }

    /**
     * @see Configuration#getWebApiToken()
     * @verifies return correct value
     */
    @Test
    public void getWebApiToken_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("test", DataManager.getInstance().getConfiguration().getWebApiToken());
    }

    /**
     * @see Configuration#isAddCORSHeader()
     * @verifies return correct value
     */
    @Test
    public void isAddCORSHeader_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isAddCORSHeader());
    }

    /**
     * @see Configuration#isAllowRedirectCollectionToWork()
     * @verifies return correct value
     */
    @Test
    public void isAllowRedirectCollectionToWork_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isAllowRedirectCollectionToWork());
    }

    /**
     * @see Configuration#isLimitImageHeight()
     * @verifies return correct value
     */
    @Test
    public void isLimitImageHeight_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isLimitImageHeight());
    }

    /**
     * @see Configuration#getLimitImageHeightUpperRatioThreshold()
     * @verifies return correct value
     */
    @Test
    public void getLimitImageHeightUpperRatioThreshold_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(0.2f == DataManager.getInstance().getConfiguration().getLimitImageHeightLowerRatioThreshold());
    }

    /**
     * @see Configuration#getLimitImageHeightLowerRatioThreshold()
     * @verifies return correct value
     */
    @Test
    public void getLimitImageHeightLowerRatioThreshold_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(2.0f == DataManager.getInstance().getConfiguration().getLimitImageHeightUpperRatioThreshold());
    }

    @Test
    public void testReadMapBoxToken() {
        Assert.assertEquals("some.token", DataManager.getInstance().getConfiguration().getMapBoxToken());
    }

    @Test
    public void testGetLicenseDescriptions() {
        List<LicenseDescription> licenses = DataManager.getInstance().getConfiguration().getLicenseDescriptions();
        Assert.assertEquals(2, licenses.size());
        Assert.assertEquals("CC0 1.0", licenses.get(0).getLabel());
        Assert.assertEquals("http://rightsstatements.org/vocab/InC/1.0/", licenses.get(1).getUrl());
        Assert.assertEquals("", licenses.get(0).getIcon());
    }

    @Test
    public void testGetGeoMapMarkerFields() {
        List<String> fields = DataManager.getInstance().getConfiguration().getGeoMapMarkerFields();
        Assert.assertEquals(3, fields.size());
        Assert.assertTrue(fields.contains("MD_GEOJSON_POINT"));
        Assert.assertTrue(fields.contains("NORM_COORDS_GEOJSON"));
        Assert.assertTrue(fields.contains("MD_COORDINATES"));
    }

    @Test
    public void testGetGeoMapMarkers() {
        List<GeoMapMarker> markers = DataManager.getInstance().getConfiguration().getGeoMapMarkers();
        Assert.assertEquals(5, markers.size());
        Assert.assertEquals("maps__marker_1", markers.get(0).getName());
        Assert.assertEquals("fa-circle", markers.get(0).getIcon());
        Assert.assertEquals("fa-search", markers.get(1).getIcon());
    }

    @Test
    public void testGetGeoMapMarker() {
        GeoMapMarker marker = DataManager.getInstance().getConfiguration().getGeoMapMarker("maps__marker_2");
        Assert.assertNotNull(marker);
        Assert.assertEquals("maps__marker_2", marker.getName());
        Assert.assertEquals("fa-search", marker.getIcon());
    }

    /**
     * @see Configuration#getConnectorVersionUrl()
     * @verifies return correct value
     */
    @Test
    public void getConnectorVersionUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost:8081/M2M/oai/tools?action=getVersion",
                DataManager.getInstance().getConfiguration().getConnectorVersionUrl());
    }

    /**
     * @see Configuration#isDisplaySidebarBrowsingTerms()
     * @verifies return correct value
     */
    @Test
    public void isDisplaySidebarBrowsingTerms_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isDisplaySidebarBrowsingTerms());
    }

    /**
     * @see Configuration#isDisplaySidebarRssFeed()
     * @verifies return correct value
     */
    @Test
    public void isDisplaySidebarRssFeed_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isDisplaySidebarRssFeed());
    }

    /**
     * @see Configuration#isDisplayWidgetUsage()
     * @verifies return correct value
     */
    @Test
    public void isDisplayWidgetUsage_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isDisplayWidgetUsage());
    }

    /**
     * @see Configuration#getSidebarWidgetUsageIntroductionText()
     * @verifies return correct value
     */
    @Test
    public void getSidebarWidgetUsageIntroductionText_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("MASTERVALUE_USAGE_INTRO", DataManager.getInstance().getConfiguration().getSidebarWidgetUsageIntroductionText());
    }

    /**
     * @see Configuration#getSidebarWidgetUsageCitationStyles()
     * @verifies return all configured values
     */
    @Test
    public void getSidebarWidgetUsageCitationStyles_shouldReturnAllConfiguredValues() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getSidebarWidgetUsageCitationStyles();
        Assert.assertEquals(3, result.size());
    }

    /**
     * @see Configuration#getSidebarWidgetUsagePageDownloadOptions()
     * @verifies return all configured elements
     */
    @Test
    public void getSidebarWidgetUsagePageDownloadOptions_shouldReturnAllConfiguredElements() throws Exception {
        List<DownloadOption> result = DataManager.getInstance().getConfiguration().getSidebarWidgetUsagePageDownloadOptions();
        Assert.assertEquals(5, result.size());
        DownloadOption option = result.get(4);
        Assert.assertEquals("label__download_option_large_4096", option.getLabel());
        Assert.assertEquals("jpg", option.getFormat());
        Assert.assertEquals("4096" + DownloadOption.TIMES_SYMBOL + "4096", option.getBoxSizeLabel());
    }

    /**
     * @see Configuration#getPageSelectDropdownDisplayMinPages()
     * @verifies return correct value
     */
    @Test
    public void getPageSelectDropdownDisplayMinPages_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(1, DataManager.getInstance().getConfiguration().getPageSelectDropdownDisplayMinPages());
    }
}
