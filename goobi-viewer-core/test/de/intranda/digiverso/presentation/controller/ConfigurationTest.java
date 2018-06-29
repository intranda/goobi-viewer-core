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
package de.intranda.digiverso.presentation.controller;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.model.metadata.Metadata;
import de.intranda.digiverso.presentation.model.metadata.MetadataParameter;
import de.intranda.digiverso.presentation.model.security.OpenIdProvider;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.model.viewer.StringPair;
import net.sf.ehcache.config.ConfigurationHelper;

public class ConfigurationTest {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTest.class);;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Initialize the instance with a custom config file
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));
    }

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
        Assert.assertEquals("http://localhost:8080/viewer/download/", DataManager.getInstance().getConfiguration().getDownloadUrl());
    }

    /**
     * @see Configuration#getContentRestApiUrl()
     * @verifies return correct value
     */
    @Test
    public void getContentRestApiUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("http://localhost:8080/viewer/rest/content/", DataManager.getInstance().getConfiguration().getContentRestApiUrl());
    }

    /**
     * @see Configuration#getDataRepositoriesHome()
     * @verifies return correct value
     */
    @Test
    @Deprecated
    public void getDataRepositoriesHome_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("resources/test/data/viewer/data/", DataManager.getInstance().getConfiguration().getDataRepositoriesHome());
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
     * @see Configuration#getDefaultCollection()
     * @verifies return correct value
     */
    @Test
    public void getDefaultCollection_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("defaultCollection_value", DataManager.getInstance().getConfiguration().getDefaultCollection());
    }

    /**
     * @see Configuration#getDefaultImageFullscreenHeight()
     * @verifies return correct value
     */
    @Test
    public void getDefaultImageFullscreenHeight_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(5, DataManager.getInstance().getConfiguration().getDefaultImageFullscreenHeight());
    }

    /**
     * @see Configuration#getDefaultImageFullscreenWidth()
     * @verifies return correct value
     */
    @Test
    public void getDefaultImageFullscreenWidth_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(4, DataManager.getInstance().getConfiguration().getDefaultImageFullscreenWidth());
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
     * @see Configuration#getDocStructWhiteList()
     * @verifies return all configured elements
     */
    @Test
    public void getDocStructWhiteList_shouldReturnAllConfiguredElements() throws Exception {
        Assert.assertEquals(3, DataManager.getInstance().getConfiguration().getDocStructWhiteList().size());
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
     * @see Configuration#getSearchHitsPerPage()
     * @verifies return correct value
     */
    @Test
    public void getSearchHitsPerPage_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(15, DataManager.getInstance().getConfiguration().getSearchHitsPerPage());
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
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return correct template configuration
     */
    @Test
    public void getMainMetadataForTemplate_shouldReturnCorrectTemplateConfiguration() throws Exception {
        Assert.assertEquals(1, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate("Chapter").size());
    }

    /**
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return default template configuration if template not found
     */
    @Test
    public void getMainMetadataForTemplate_shouldReturnDefaultTemplateConfigurationIfTemplateNotFound() throws Exception {
        Assert.assertEquals(6, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate("nonexisting").size());
    }

    /**
     * @see Configuration#getMainMetadataForTemplate(String)
     * @verifies return default template if template is null
     */
    @Test
    public void getMainMetadataForTemplate_shouldReturnDefaultTemplateIfTemplateIsNull() throws Exception {
        Assert.assertEquals(6, DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(null).size());
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
        Assert.assertEquals(" / ", params.get(1).getPrefix());
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
     * @see Configuration#isShowOpenIdConnect()
     * @verifies return correct value
     */
    @Test
    public void isShowOpenIdConnect_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isShowOpenIdConnect());
    }

    /**
     * @see Configuration#getOpenIdConnectProviders()
     * @verifies return all properly configured elements
     */
    @Test
    public void getOpenIdConnectProviders_shouldReturnAllProperlyConfiguredElements() throws Exception {
        List<OpenIdProvider> providers = DataManager.getInstance().getConfiguration().getOpenIdConnectProviders();
        Assert.assertEquals(2, providers.size());
        Assert.assertEquals("Google", providers.get(0).getName());
        Assert.assertEquals("https://accounts.google.com/o/oauth2/auth", providers.get(0).getUrl());
        Assert.assertEquals("id_google", providers.get(0).getClientId());
        Assert.assertEquals("secret_google", providers.get(0).getClientSecret());
        Assert.assertEquals("google.png", providers.get(0).getImage());
    }

    /**
     * @see Configuration#getOrigContentFolder()
     * @verifies return correct value
     */
    @Test
    public void getOrigContentFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("src", DataManager.getInstance().getConfiguration().getOrigContentFolder());
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
     * @see Configuration#getRulesetFilePath()
     * @verifies return correct value
     */
    @Test
    public void getRulesetFilePath_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("ruleset_value", DataManager.getInstance().getConfiguration().getRulesetFilePath());
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
     * @see Configuration#getSolrUrl()
     * @verifies return correct value
     */
    @Test
    public void getSolrUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("solr_value", DataManager.getInstance().getConfiguration().getSolrUrl());
    }

    /**
     * @see Configuration#getSplittingCharacter()
     * @verifies return correct value
     */
    @Test
    public void getSplittingCharacter_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(".", DataManager.getInstance().getConfiguration().getSplittingCharacter());
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies load all stopwords
     */
    @Test
    public void loadStopwords_shouldLoadAllStopwords() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("resources/test/stopwords.txt");
        Assert.assertNotNull(stopwords);
        Assert.assertEquals(5, stopwords.size());
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies remove parts starting with pipe
     */
    @Test
    public void loadStopwords_shouldRemovePartsStartingWithPipe() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("resources/test/stopwords.txt");
        Assert.assertNotNull(stopwords);
        Assert.assertTrue(stopwords.contains("one"));
    }

    /**
     * @see Configuration#loadStopwords(String)
     * @verifies not add empty stopwords
     */
    @Test
    public void loadStopwords_shouldNotAddEmptyStopwords() throws Exception {
        Set<String> stopwords = Configuration.loadStopwords("resources/test/stopwords.txt");
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
        Configuration.loadStopwords("resources/test/startwords.txt");
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
        Assert.assertEquals("resources/test/stopwords.txt", DataManager.getInstance().getConfiguration().getStopwordsFilePath());
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
        Assert.assertEquals("discriminatorField_value", DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    /**
     * @see ConfigurationHelper#isSubthemeAutoSwitch()
     * @verifies return correct value
     */
    @Test
    public void isSubthemeAutoSwitch_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isSubthemeAutoSwitch());
    }

    /**
     * @see Configuration#isSubthemeAddFilterQuery()
     * @verifies return correct value
     */
    @Test
    public void isSubthemeAddFilterQuery_shouldReturnCorrectValue() throws Exception {
        //TODO auto-generated
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isSubthemeAddFilterQuery());
    }

    /**
     * @see ConfigurationHelper#isSubthemeFilterQueryVisible()
     * @verifies return correct value
     */
    @Test
    public void isSubthemeFilterQueryVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isSubthemeFilterQueryVisible());
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
     * @see Configuration#getViewerDfgViewerUrl()
     * @verifies return correct value
     */
    @Test
    public void getViewerDfgViewerUrl_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("dfg-viewer_value", DataManager.getInstance().getConfiguration().getViewerDfgViewerUrl());
    }

    /**
     * @see Configuration#getViewerHome()
     * @verifies return correct value
     */
    @Test
    public void getViewerHome_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("resources/test/data/viewer/", DataManager.getInstance().getConfiguration().getViewerHome());
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
        Assert.assertEquals("watermarkIdField_value", DataManager.getInstance().getConfiguration().getWatermarkIdField());
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
     * @see Configuration#isBookshelvesEnabled()
     * @verifies return correct value
     */
    @Test
    public void isBookshelvesEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isBookshelvesEnabled());
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
     * @see Configuration#isDisableMenuBrowsingOnSearchList()
     * @verifies return correct value
     */
    @Test
    public void isDisableMenuBrowsingOnSearchList_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isDisableMenuBrowsingOnSearchList());
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
     * @see Configuration#isMetadataPdfEnabled()
     * @verifies return correct value
     */
    @Test
    public void isMetadataPdfEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isMetadataPdfEnabled());
    }

    /**
     * @see Configuration#isOriginalContentDownload()
     * @verifies return correct value
     */
    @Test
    public void isOriginalContentDownload_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isOriginalContentDownload());
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
     * @see Configuration#isPdfApiDisabled()
     * @verifies return correct value
     */
    @Test
    public void isPdfApiDisabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isPdfApiDisabled());
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
     * @see Configuration#isPreventProxyCaching()
     * @verifies return correct value
     */
    @Test
    public void isPreventProxyCaching_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isPreventProxyCaching());
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
     * @see Configuration#isSidebarDfgLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarDfgLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarDfgLinkVisible());
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
     * @see Configuration#isSidebarMetadataLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarMetadataLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarMetadataLinkVisible());
    }

    /**
     * @see Configuration#isSidebarOpacLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarOpacLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarOpacLinkVisible());
    }

    /**
     * @see Configuration#isSidebarPageLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarPageLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarPageLinkVisible());
    }

    /**
     * @see Configuration#isSidebarCalendarLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarCalendarLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarCalendarLinkVisible());
    }

    /**
     * @see Configuration#isSidebarThumbsLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarThumbsLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarThumbsLinkVisible());
    }

    /**
     * @see Configuration#isSidebarTocLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarTocLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarTocLinkVisible());
    }

    /**
     * @see Configuration#isSidebarTocVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarTocVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(false, DataManager.getInstance().getConfiguration().isSidebarTocVisible());
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
     * @see Configuration#isSubthemesEnabled()
     * @verifies return correct value
     */
    @Test
    public void isSubthemesEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isSubthemesEnabled());
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
        Assert.assertEquals(7, DataManager.getInstance().getConfiguration().getAdvancedSearchFields().size());
    }

    /**
     * @see Configuration#isAdvancedSearchFieldHierarchical(String)
     * @verifies return correct value
     */
    @Test
    public void isAdvancedSearchFieldHierarchical_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldHierarchical(SolrConstants.DC));
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isAdvancedSearchFieldHierarchical("MD_TITLE"));
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
     * @see Configuration#isUseExternalCS()
     * @verifies return correct value
     */
    @Test
    public void getUseExternalCS_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().isUseExternalCS());
    }

    /**
     * @see Configuration#useOpenLayers()
     * @verifies return correct value
     */
    @Test
    public void useOpenLayers_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().useOpenLayers());
    }

    /**
     * @see Configuration#useOpenLayersFullscreen()
     * @verifies return correct value
     */
    @Test
    public void useOpenLayersFullscreen_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().useOpenLayersFullscreen());
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
     * @see Configuration#getDisplayBibdataBreadcrumbs()
     * @verifies return correct value
     */
    @Test
    @Deprecated
    public void getDisplayBibdataBreadcrumbs_shouldReturnCorrectValue() throws Exception {
        Assert.assertTrue(DataManager.getInstance().getConfiguration().getDisplayBibdataBreadcrumbs());
    }

    /**
     * @see Configuration#getBibdataBreadcrumbsMaxTitleLength()
     * @verifies return correct value
     */
    @Test
    @Deprecated
    public void getBibdataBreadcrumbsMaxTitleLength_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(200, DataManager.getInstance().getConfiguration().getBibdataBreadcrumbsMaxTitleLength());
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
        Assert.assertEquals("FIELD1", result.get(1));
        Assert.assertEquals("FIELD3", result.get(2));
        Assert.assertEquals("FIELD2", result.get(3));
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
        Assert.assertEquals(16, DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber("FIELD2"));
        Assert.assertEquals(23, DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber(null));
    }

    /**
     * @see Configuration#getInitialDrillDownElementNumber(String)
     * @verifies return default value if field not found
     */
    @Test
    public void getInitialDrillDownElementNumber_shouldReturnDefaultValueIfFieldNotFound() throws Exception {
        Assert.assertEquals(-1, DataManager.getInstance().getConfiguration().getInitialDrillDownElementNumber("FIELD1"));
    }

    /**
     * @see Configuration#getPriorityValuesForDrillDownField(String)
     * @verifies return return all configured elements for regular fields
     */
    @Test
    public void getPriorityValuesForDrillDownField_shouldReturnReturnAllConfiguredElementsForRegularFields() throws Exception {
        List<String> result = DataManager.getInstance().getConfiguration().getPriorityValuesForDrillDownField("FIELD2");
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

    @Test
    public void getSortOrderTest() {
        Assert.assertEquals("numerical", DataManager.getInstance().getConfiguration().getSortOrder("FIELD1"));
        Assert.assertEquals("default", DataManager.getInstance().getConfiguration().getSortOrder("FIELD2"));
        Assert.assertEquals("numerical", DataManager.getInstance().getConfiguration().getSortOrder("FIELD3"));
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
        Assert.assertEquals("AND -BOOL_HIDE:true", DataManager.getInstance().getConfiguration().getStaticQuerySuffix());
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
     * @see Configuration#getSelectedRecords()
     * @verifies return all configured elements
     */
    @Test
    public void getSelectedRecords_shouldReturnAllConfiguredElements() throws Exception {
        List<String> ret = DataManager.getInstance().getConfiguration().getSelectedRecords(SolrConstants.DC);
        Assert.assertNotNull(ret);
        Assert.assertEquals(4, ret.size());
    }

    /**
     * @see Configuration#getOverviewFolder()
     * @verifies return correct value
     */
    @Test
    public void getOverviewFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("overview", DataManager.getInstance().getConfiguration().getOverviewFolder());
    }

    /**
     * @see Configuration#isSidebarOverviewLinkVisible()
     * @verifies return correct value
     */
    @Test
    public void isSidebarOverviewLinkVisible_shouldReturnCorrectValue() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isSidebarOverviewLinkVisible());
    }

    /**
     * @see Configuration#getSidebarOverviewLinkCondition()
     * @verifies return correct value
     */
    @Test
    public void getSidebarOverviewLinkCondition_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("condition_value", DataManager.getInstance().getConfiguration().getSidebarOverviewLinkCondition());
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
     * @see Configuration#getFulltextFolder()
     * @verifies return correct value
     */
    @Test
    public void getFulltextFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("fulltext", DataManager.getInstance().getConfiguration().getFulltextFolder());
    }

    /**
     * @see Configuration#getWcFolder()
     * @verifies return correct value
     */
    @Test
    public void getWcFolder_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals("wc", DataManager.getInstance().getConfiguration().getWcFolder());
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
     * @see Configuration#isCmsEnabled()
     * @verifies return correct value
     */
    @Test
    public void isCmsEnabled_shouldReturnCorrectValue() throws Exception {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isCmsEnabled());
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
    public void getImageViewZoomScalesTest() throws ConfigurationException {
        List<String> scales = DataManager.getInstance().getConfiguration().getImageViewZoomScales();
        Assert.assertEquals("600", scales.get(0));
        Assert.assertEquals("1000", scales.get(1));
        Assert.assertEquals("1500", scales.get(2));
        Assert.assertEquals("3000", scales.get(3));
    }

    @Test
    public void getFullscreenViewZoomScalesTest() throws ConfigurationException {
        List<String> scales = DataManager.getInstance().getConfiguration().getImageViewZoomScales(PageType.viewFullscreen, null);
        Assert.assertEquals("1000", scales.get(0));
        Assert.assertEquals("2000", scales.get(1));
        Assert.assertEquals("3000", scales.get(2));
    }

    @Test
    public void getImageViewTileSizesTest() throws ConfigurationException {
        Map<Integer, List<Integer>> tiles = DataManager.getInstance().getConfiguration().getTileSizes();
        Assert.assertEquals(512, tiles.keySet().iterator().next(), 0);
        Assert.assertEquals(1, tiles.get(512).get(0), 0);
        Assert.assertEquals(2, tiles.get(512).get(1), 0);
        Assert.assertEquals(3, tiles.get(512).get(2), 0);
    }

    @Test
    public void getFullscreenTileSizesTest() throws ConfigurationException {
        Map<Integer, List<Integer>> tiles = DataManager.getInstance().getConfiguration().getTileSizes(PageType.viewFullscreen, null);
        Assert.assertEquals(1024, tiles.keySet().iterator().next(), 0);
        Assert.assertEquals(2, tiles.get(1024).get(0), 0);
        Assert.assertEquals(4, tiles.get(1024).get(1), 0);
        Assert.assertEquals(8, tiles.get(1024).get(2), 0);
    }

    @Test
    public void getFooterHeightTest() throws ConfigurationException {
        Assert.assertEquals(50, DataManager.getInstance().getConfiguration().getFooterHeight());
    }

    @Test
    public void getCrowdsourcingFooterHeightTest() throws ConfigurationException {
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

    @Test
    public void testGetTaskManagerRestUrl() {
        Assert.assertEquals("taskmanager_url/rest", DataManager.getInstance().getConfiguration().getTaskManagerRestUrl());
    }

    @Test
    public void testGetTaskManagerServiceUrl() {
        Assert.assertEquals("taskmanager_url/service", DataManager.getInstance().getConfiguration().getTaskManagerServiceUrl());
    }

    @Test
    public void testGetReCaptchaSiteKey() {
        Assert.assertEquals("6LetEyITAAAAAEAj7NTxgRXR6S_uhZrk9rn5HyB3", DataManager.getInstance().getConfiguration().getReCaptchaSiteKey());
    }

    @Test
    public void testIsTocEpubEnabled() {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isTocEpubEnabled());
    }

    @Test
    public void testIsSearchInItemEnabled() {
        Assert.assertEquals(true, DataManager.getInstance().getConfiguration().isSearchInItemEnabled());
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

    @Test
    public void testGetIIIFUrl() {
        Assert.assertEquals("http://localhost:8080/viewer/rest/", DataManager.getInstance().getConfiguration().getIiifUrl());
    }

    @Test
    public void testGetDownloadUrl() {
        Assert.assertEquals("http://localhost:8080/viewer/download/", DataManager.getInstance().getConfiguration().getDownloadUrl());
    }

    @Test
    public void testBrokenConfig() {
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer_broken.test.xml"));
        String localConfig = DataManager.getInstance().getConfiguration().getConfigLocalPath();
        Assert.assertEquals(localConfig, "resources/test/localConfig/");
        String viewerHome = DataManager.getInstance().getConfiguration().getViewerHome();
        Assert.assertEquals(viewerHome, "resources/test/data/viewer/");
        String dataRepositories = DataManager.getInstance().getConfiguration().getDataRepositoriesHome();
        Assert.assertEquals(dataRepositories, "resources/test/data/viewer/data/");
        DataManager.getInstance().injectConfiguration(new Configuration("resources/test/config_viewer.test.xml"));

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
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(SolrConstants.DC, results.get(0));
        Assert.assertEquals(SolrConstants.DOCSTRCT, results.get(1));
    }

    @Test
    public void testSidebarTocVisibleIfEmpty() throws Exception {
        Assert.assertFalse(DataManager.getInstance().getConfiguration().isDisplayEmptyTocInSidebar());
    }

    @Test
    public void testIsDoublePageModeEnabled_shouldReturnCorrectValue() throws Exception {
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
}
