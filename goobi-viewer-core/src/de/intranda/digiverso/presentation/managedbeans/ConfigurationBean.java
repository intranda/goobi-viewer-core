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
package de.intranda.digiverso.presentation.managedbeans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.annotation.FacesConfig;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.controller.language.Language;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.viewer.PageType;
import de.intranda.digiverso.presentation.modules.IModule;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

/**
 * This is a wrapper class for the <code>Configuration</code> class for access from HTML.
 */
@FacesConfig
@Named
@ApplicationScoped
public class ConfigurationBean implements Serializable {

    private static final long serialVersionUID = -1371688138567741188L;

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationBean.class);

    /** Empty constructor. */
    public ConfigurationBean() {
        // the emptiness inside
    }

    /**
     * 
     * @return
     */
    public List<IModule> getModules() {
        return DataManager.getInstance().getModules();
    }

    @Deprecated
    public String getContentServletUrl() {
        return DataManager.getInstance().getConfiguration().getContentServerWrapperUrl();
    }

    /**
     * Access the height of the image Footer for OpenLayers. With apache commons it is not possible to read the xml root element, therefore this
     * method uses jdom.
     *
     * @return the Height of the Footer
     */
    public Double getRelativeImageFooterHeight() {
        double height = 0;

        if (!ContentServerConfiguration.getInstance().getWatermarkUse()) {
            return height;
        }

        // Load Height of the Footer from the config_imageFooter.xml
        String watermarkConfigFilePath = ContentServerConfiguration.getInstance().getWatermarkConfigFilePath();

        File fileConfigImageFooter = null;
        try {
            fileConfigImageFooter = new File(new URI(watermarkConfigFilePath));
        } catch (URISyntaxException e) {
            logger.error("Error while reading the watermark attribut from the " + watermarkConfigFilePath + " file.", e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
        }
        logger.debug("Reading path to the file 'config_imageFooter.xml' from the file: {}", watermarkConfigFilePath);
        try (FileInputStream fis = new FileInputStream(fileConfigImageFooter)) {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setValidating(false);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document xmldoc = docBuilder.parse(fis);

            // iterate over all nodes and read nodes
            Node topmostelement = xmldoc.getDocumentElement(); // get uppermost
            if (topmostelement.getNodeName().equals("watermarks")) {
                Node child = topmostelement.getFirstChild();
                while (child != null && !"watermark".equals(child.getNodeName())) {
                    child = child.getNextSibling();
                }
                if (child != null) {
                    topmostelement = child;
                }
            }
            if (!topmostelement.getNodeName().equals("watermark")) {
                logger.error("Don't get correct xml response - topelement is NOT <watermark>");
            }

            // iterate over attributes
            NamedNodeMap nnm = topmostelement.getAttributes();
            if (nnm != null) {
                Node heightnode = nnm.getNamedItem("height"); // read heigth
                Node widthnode = nnm.getNamedItem("width"); // read heigth

                if (heightnode != null && widthnode != null) {
                    try {
                        int absHeight = Integer.parseInt(heightnode.getNodeValue());
                        int absWidth = Integer.parseInt(widthnode.getNodeValue());
                        height = (double) absHeight / (double) absWidth;
                        logger.debug("Red '{}px' for the footer from the {} file.", height, watermarkConfigFilePath);
                    } catch (Exception e) {
                        logger.error("Invalid value for watermark's height.");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error("Can't read XML configuration for Watermark stream due to {}", e.getMessage());
        } catch (ParserConfigurationException e) {
            logger.error("Can't parse xml configuration file.", e);
        } catch (SAXException e) {
            logger.error("Error in xml file.", e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
        }

        return height;
    }

    /**
     * Access the height of the image Footer for OpenLayers. With apache commons it is not possible to read the xml root element, therefore this
     * method uses jdom.
     *
     * @return the Height of the Footer
     */
    public Integer getImageFooterHeight() {
        int height = 0;

        if (!ContentServerConfiguration.getInstance().getWatermarkUse()) {
            return height;
        }

        // Load Height of the Footer from the config_imageFooter.xml
        String watermarkConfigFilePath = ContentServerConfiguration.getInstance().getWatermarkConfigFilePath();
        File fileConfigImageFooter = null;
        try {
            fileConfigImageFooter = new File(new URI(watermarkConfigFilePath));
        } catch (URISyntaxException e) {
            logger.error("Error while reading the watermark attribut from the " + watermarkConfigFilePath + " file.", e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
        }
        logger.debug("Reading path to the file 'config_imageFooter.xml' from the file: {}", watermarkConfigFilePath);
        try (FileInputStream fis = new FileInputStream(fileConfigImageFooter)) {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setValidating(false);
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document xmldoc = docBuilder.parse(fis);

            // iterate over all nodes and read nodes
            Node topmostelement = xmldoc.getDocumentElement(); // get uppermost
            if (topmostelement.getNodeName().equals("watermarks")) {
                Node child = topmostelement.getFirstChild();
                while (child != null && !"watermark".equals(child.getNodeName())) {
                    child = child.getNextSibling();
                }
                if (child != null) {
                    topmostelement = child;
                }
            }
            if (!topmostelement.getNodeName().equals("watermark")) {
                logger.error("Don't get correct xml response - topelement is NOT <watermark>");
            }

            // iterate over attributes
            NamedNodeMap nnm = topmostelement.getAttributes();
            if (nnm != null) {
                Node heightnode = nnm.getNamedItem("height"); // read heigth

                if (heightnode != null) {
                    String value = heightnode.getNodeValue();
                    try {
                        height = Integer.parseInt(value);
                        logger.debug("Red '{}px' for the footer from the {} file.", height, watermarkConfigFilePath);
                    } catch (Exception e) {
                        logger.error("Invalid value for watermark's height.");
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } catch (IOException e) {
            logger.error("Can't read XML configuration for Watermark stream due to {}", e.getMessage());
        } catch (ParserConfigurationException e) {
            logger.error("Can't parse xml configuration file.", e);
        } catch (SAXException e) {
            logger.error("Error in xml file.", e);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
        }

        return height;
    }

    public boolean isShowSidebarEventMetadata() {
        return DataManager.getInstance().getConfiguration().isShowSidebarEventMetadata();
    }

    @Deprecated
    public String getContentServerWrapperUrl() {
        return DataManager.getInstance().getConfiguration().getContentServerWrapperUrl();
    }

    @Deprecated
    public String getContentServerWrapperUrlWithoutLastSlash() {
        String csWrapperUrl = DataManager.getInstance().getConfiguration().getContentServerWrapperUrl();
        if (csWrapperUrl != null) {
            // delete the last /
            int endIndex = csWrapperUrl.lastIndexOf('/');
            csWrapperUrl = csWrapperUrl.substring(0, endIndex);
        }
        return csWrapperUrl;
    }

    public boolean isBookshelvesEnabled() {
        return DataManager.getInstance().getConfiguration().isBookshelvesEnabled();
    }

    public boolean isUserCommentsEnabled() {
        return DataManager.getInstance().getConfiguration().isUserCommentsEnabled();
    }

    public static boolean isCmsEnabledStatic() {
        return DataManager.getInstance().getConfiguration().isCmsEnabled();
    }

    public boolean isCmsEnabled() {
        return isCmsEnabledStatic();
    }

    public boolean isUseCustomNavBar() {
        return isCmsEnabledStatic() && DataManager.getInstance().getConfiguration().useCustomNavBar();
    }

    public boolean isMenuBrowsingVisibleInSearchList() {
        return !DataManager.getInstance().getConfiguration().isDisableMenuBrowsingOnSearchList();
    }

    public boolean useOpenLayers() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().useOpenLayers();
    }

    public boolean useOpenSeadragon() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().useOpenSeadragon();
    }

    public boolean useTiles(String pageType, String mimeType) throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.getByName(pageType), getImageType(mimeType));
    }

    public int getFooterHeight(String pageType, String mimeType) throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.getByName(pageType), getImageType(mimeType));
    }

    public List<String> getImageSizes(String pageType, String mimeType) throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().getImageViewZoomScales(PageType.getByName(pageType), getImageType(mimeType));
    }

    public Map<Integer, List<Integer>> getTileSizes(String pageType, String mimeType) throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().getTileSizes(PageType.getByName(pageType), getImageType(mimeType));
    }

    public boolean useTiles() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles();
    }

    public boolean useTilesFullscreen() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.viewFullscreen, null);
    }

    public boolean useTilesCrowd() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.editContent, null);
    }

    public int getFooterHeight() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight();
    }

    public int getFooterHeightFullscreen() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.viewFullscreen, null);
    }

    public int getFooterHeightCrowd() throws ConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.editContent, null);
    }

    public boolean isRememberImageZoom() {
        return DataManager.getInstance().getConfiguration().isRememberImageZoom();
    }

    public boolean isRememberImageRotation() {
        return DataManager.getInstance().getConfiguration().isRememberImageRotation();
    }

    public boolean isDisplayStatistics() {
        return DataManager.getInstance().getConfiguration().isDisplayStatistics();
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isDisplaySearchRssLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchRssLinks();
    }

    @Deprecated
    public boolean isTocTreeView() {
        return false;
    }

    public boolean showThumbnailsInToc() {
        return DataManager.getInstance().getConfiguration().showThumbnailsInToc();
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isAdvancedSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchEnabled();
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isTimelineSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isTimelineSearchEnabled();
    }

    /**
     *
     * @return
     * @should return correct value
     */
    public boolean isCalendarSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isCalendarSearchEnabled();
    }

    public boolean isDisplayBreadcrumbs() {
        return DataManager.getInstance().getConfiguration().getDisplayBreadcrumbs();
    }

    public boolean isDisplayMetadataPageLinkBlock() {
        return DataManager.getInstance().getConfiguration().getDisplayMetadataPageLinkBlock();
    }

    public boolean isPagePdfEnabled() {
        return DataManager.getInstance().getConfiguration().isPagePdfEnabled();
    }

    public String getRssTitle() {
        return DataManager.getInstance().getConfiguration().getRssTitle();
    }

    public boolean isDisplayTagCloudStartpage() {
        return DataManager.getInstance().getConfiguration().isDisplayTagCloudStartpage();
    }

    public boolean isDisplaySearchResultNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchResultNavigation();
    }

    public boolean isDisplayStructType() {
        return DataManager.getInstance().getConfiguration().getDisplayStructType();
    }

    public boolean isDisplayCollectionBrowsing() {
        return DataManager.getInstance().getConfiguration().isDisplayCollectionBrowsing();
    }

    public boolean isDisplayUserNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplayUserNavigation();
    }

    public boolean isDisplayTagCloudNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplayTagCloudNavigation();
    }

    public boolean isDisplayTitlePURL() {
        return DataManager.getInstance().getConfiguration().isDisplayTitlePURL();
    }

    public boolean isSidebarTocVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarTocVisible();
    }

    public boolean isDisplayEmptyTocInSidebar() {
        return DataManager.getInstance().getConfiguration().isDisplayEmptyTocInSidebar();
    }

    public boolean isSidebarTocPageNumbersVisible() {
        return DataManager.getInstance().getConfiguration().getSidebarTocPageNumbersVisible();
    }

    public boolean isSidebarTocTreeView() {
        return DataManager.getInstance().getConfiguration().isSidebarTocTreeView();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarOverviewLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarOverviewLinkVisible();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarPageLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarPageLinkVisible();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarTocLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarTocLinkVisible();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarCalendarLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarCalendarLinkVisible();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarMetadataLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarMetadataLinkVisible();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarThumbsLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarThumbsLinkVisible();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarFulltextLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarFulltextLinkVisible();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarDfgLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarDfgLinkVisible();
    }

    /**
     * @return
     * @should return correct value
     */
    public boolean isSidebarOpacLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarOpacLinkVisible();
    }

    public boolean isTocTreeView(String docStructType) {
        return DataManager.getInstance().getConfiguration().isTocTreeView(docStructType);
    }

    public int getSidebarTocLengthBeforeCut() {
        return DataManager.getInstance().getConfiguration().getSidebarTocLengthBeforeCut();
    }

    public int getImageViewHeight() {
        return DataManager.getInstance().getConfiguration().getViewImageHeight();
    }

    public int getImageViewWidth() {
        return DataManager.getInstance().getConfiguration().getViewImageWidth();
    }

    public int getFullscreenImageHeight() {
        return DataManager.getInstance().getConfiguration().getFullscreenImageHeight();
    }

    public int getFullscreenImageWidth() {
        return DataManager.getInstance().getConfiguration().getFullscreenImageWidth();
    }

    public boolean isDisplayTitleBreadcrumbs() {
        return DataManager.getInstance().getConfiguration().getDisplayTitleBreadcrumbs();
    }

    public int getTitleBreadcrumbsMaxTitleLength() {
        return DataManager.getInstance().getConfiguration().getTitleBreadcrumbsMaxTitleLength();
    }

    @Deprecated
    public int getBibdataBreadcrumbsMaxTitleLength() {
        return DataManager.getInstance().getConfiguration().getBibdataBreadcrumbsMaxTitleLength();
    }

    public boolean isDisplayTimeMatrix() {
        return DataManager.getInstance().getConfiguration().isDisplayTimeMatrix();
    }

    /**
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public int getTimeMatrixStartYear() throws PresentationException, IndexUnreachableException {
        String value = DataManager.getInstance().getConfiguration().getStartYearForTimeline();
        if ("MIN".equals(value)) {
            return SearchHelper.getMinMaxYears(null)[0];
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logger.error("'{}' is not a valid value for 'startyear'", value);
            return 0;
        }
    }

    /**
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public int getTimeMatrixEndYear() throws PresentationException, IndexUnreachableException {
        String value = DataManager.getInstance().getConfiguration().getEndYearForTimeline();
        if ("MAX".equals(value)) {
            return SearchHelper.getMinMaxYears(null)[1];
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logger.error("'{}' is not a valid value for 'startyear'", value);
            return 2500;
        }
    }

    /**
     * 
     * @return Total hit number for the time matrix
     */
    public int getTimeMatrixHits() {
        String value = DataManager.getInstance().getConfiguration().getTimelineHits();
        if ("MAX".equals(value)) {
            return SolrSearchIndex.MAX_HITS;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logger.error("'{}' is not a valid value for 'startyear'", value);
            return 108;
        }
    }

    public boolean isPiwikTracking() {
        return DataManager.getInstance().getConfiguration().isPiwikTrackingEnabled();
    }

    public String getPiwikBaseURL() {
        return DataManager.getInstance().getConfiguration().getPiwikBaseURL();
    }

    public String getPiwikSiteID() {
        return DataManager.getInstance().getConfiguration().getPiwikSiteID();
    }

    public String getAnchorThumbnailMode() {
        return DataManager.getInstance().getConfiguration().getAnchorThumbnailMode();
    }

    public List<String> getSortFields() {
        return DataManager.getInstance()
                .getConfiguration()
                .getSortFields()
                .stream()
                .filter(field -> !isLanguageVersionOtherThan(field, BeanUtils.getLocale().getLanguage()))
                .collect(Collectors.toList());
    }

    /**
     * @param field
     * @param language
     * @return
     */
    private boolean isLanguageVersionOtherThan(String field, String language) {
        return field.matches(".*_LANG_[A-Z][A-Z]") && !field.matches(".*_LANG_" + language.toUpperCase());
    }

    public int getTocIndentation() {
        return DataManager.getInstance().getConfiguration().getTocIndentation();
    }

    public int getSidebarTocIndentation() {
        return DataManager.getInstance().getConfiguration().getSidebarTocIndentation();
    }

    public boolean isPageBrowseEnabled() {
        return DataManager.getInstance().getConfiguration().isPageBrowseEnabled();
    }

    public boolean isPageBrowseStep1Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 0 && steps.get(0) > 0) {
            return true;
        }
        return false;
    }

    public boolean isPageBrowseStep2Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 1 && steps.get(1) > 0) {
            return true;
        }
        return false;
    }

    public boolean isPageBrowseStep3Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 2 && steps.get(2) > 1) {
            return true;
        }
        return false;
    }

    public int getPageBrowseStep1() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 0 && steps.get(0) > 1) {
            return steps.get(0);
        }
        return 0;
    }

    public int getPageBrowseStep2() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 1 && steps.get(1) > 1) {
            return steps.get(1);
        }
        return 0;
    }

    public int getPageBrowseStep3() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 2 && steps.get(2) > 1) {
            return steps.get(2);
        }
        return 0;
    }

    public String getReCaptchaSiteKey() {
        return DataManager.getInstance().getConfiguration().getReCaptchaSiteKey();
    }

    public boolean isUseReCaptcha() {
        return DataManager.getInstance().getConfiguration().isUseReCaptcha();
    }

    public boolean isTocEpubEnabled() {
        return DataManager.getInstance().getConfiguration().isTocEpubEnabled() && isGeneratePdfInTaskManager();
    }

    public boolean isGeneratePdfInTaskManager() {
        return DataManager.getInstance().getConfiguration().isGeneratePdfInTaskManager();
    }

    public boolean isDocHierarchyPdfEnabled() {
        return DataManager.getInstance().getConfiguration().isDocHierarchyPdfEnabled();
    }

    public boolean isShowSearchInItem() {
        return DataManager.getInstance().getConfiguration().isSearchInItemEnabled();
    }

    public String getDefaultBrowseIcon(String collection) {
        return DataManager.getInstance().getConfiguration().getDefaultBrowseIcon(collection);
    }

    /**
     * 
     * @return
     */
    public boolean isTranskribusEnabled() {
        return DataManager.getInstance().getConfiguration().isTranskribusEnabled();
    }

    /**
     * 
     * @return
     */
    public boolean isSearchExcelExportEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchExcelExportEnabled();
    }

    public boolean isDoublePageModeEnabled() {
        return DataManager.getInstance().getConfiguration().isDoublePageModeEnabled();
    }

    public String getIiifApiUrl() {
        return DataManager.getInstance().getConfiguration().getIiifUrl();
    }

    public String getIso639_1(String language) {
        return DataManager.getInstance().getLanguageHelper().getLanguage(language).getIsoCodeOld();
    }

    public String getIso639_2B(String language) {
        return DataManager.getInstance().getLanguageHelper().getLanguage(language).getIsoCode();
    }

    public String getTranslation(String language, String locale) {
        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(language);
        switch (locale.toLowerCase()) {
            case "de":
            case "ger":
                return lang.getGermanName();
            case "fre":
            case "fra":
            case "fr":
                return lang.getFrenchName();
            default:
                return lang.getEnglishName();
        }
    }

    private static ImageType getImageType(String mimeType) {
        ImageType imageType = new ImageType(false);
        imageType.setFormat(ImageFileFormat.getImageFileFormatFromMimeType(mimeType));
        return imageType;
    }

    public boolean isAddDublinCoreMetaTags() {
        return DataManager.getInstance().getConfiguration().isAddDublinCoreMetaTags();
    }

    public boolean isAddHighwirePressMetaTags() {
        return DataManager.getInstance().getConfiguration().isAddHighwirePressMetaTags();
    }
}
