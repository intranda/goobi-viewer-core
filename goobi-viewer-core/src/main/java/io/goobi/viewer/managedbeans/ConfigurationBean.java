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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageType;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.model.ViewAttributes;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.job.download.DownloadOption;
import io.goobi.viewer.model.maps.GeoMapMarker;
import io.goobi.viewer.model.maps.GeomapItemFilter;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.model.misc.EmailRecipient;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.search.SearchResultGroup;
import io.goobi.viewer.model.translations.language.Language;
import io.goobi.viewer.model.viewer.MimeType;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.modules.IModule;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrSearchIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.annotation.FacesConfig;
import jakarta.faces.model.SelectItem;
import jakarta.inject.Named;

/**
 * This is a wrapper class for the <code>Configuration</code> class for access from HTML.
 */
@FacesConfig
@Named
@ApplicationScoped
public class ConfigurationBean implements Serializable {

    private static final long serialVersionUID = -1371688138567741188L;

    private static final Logger logger = LogManager.getLogger(ConfigurationBean.class);

    /**
     * Empty constructor.
     */
    public ConfigurationBean() {
        // the emptiness inside
    }

    /**
     * getModules.
     *
     * @return a {@link java.util.List} object.
     */
    public List<IModule> getModules() {
        return DataManager.getInstance().getModules();
    }

    /**
     * getName.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return DataManager.getInstance().getConfiguration().getName();
    }

    /**
     * isBookmarksEnabled.
     *
     * @return true if the bookmarks feature is enabled in the configuration, false otherwise
     */
    public boolean isBookmarksEnabled() {
        return DataManager.getInstance().getConfiguration().isBookmarksEnabled();
    }

    public boolean isSearchSavingEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchSavingEnabled();
    }

    /**
     * useTiles.
     *
     * @param pageType name of the page type to look up settings for
     * @param mimeType MIME type of the image being displayed
     * @return true if tiles should be used for the given page type and MIME type, false otherwise
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .useTiles(getImageViewAttributes(PageType.getByName(pageType), getImageType(mimeType).getFormat().getMimeType()));
    }

    /**
     * Returns whether a navigator element should be shown in the OpenSeadragon viewer.
     * 
     * @param pageType get settings for this pageType
     * @param mimeType get settings for this image type
     * @return true if navigator should be shown
     * @throws ViewerConfigurationException
     */
    public boolean showImageNavigator(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .showImageNavigator(getImageViewAttributes(PageType.getByName(pageType), mimeType));
    }

    /**
     * getFooterHeight.
     *
     * @param pageType name of the page type to look up settings for
     * @param mimeType MIME type of the image being displayed
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .getFooterHeight(getImageViewAttributes(PageType.getByName(pageType), mimeType));
    }

    /**
     * getImageSizes.
     *
     * @param pageType name of the page type to look up settings for
     * @param mimeType MIME type of the image being displayed
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageSizes(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .getImageViewZoomScales(getImageViewAttributes(PageType.getByName(pageType), mimeType));
    }

    /**
     * getTileSizes.
     *
     * @param pageType name of the page type to look up settings for
     * @param mimeType MIME type of the image being displayed
     * @return a {@link java.util.Map} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .getTileSizes(getImageViewAttributes(PageType.getByName(pageType), mimeType));
    }

    /**
     * useTiles.
     *
     * @return true if tiles should be used for the default image view, false otherwise
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles();
    }

    /**
     * useTilesFullscreen.
     *
     * @return true if tiles should be used in fullscreen view, false otherwise
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesFullscreen() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(getImageViewAttributes(PageType.viewFullscreen, null));
    }

    /**
     * useTilesCrowd.
     *
     * @return true if tiles should be used in the crowdsourcing edit view, false otherwise
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesCrowd() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(getImageViewAttributes(PageType.editContent, null));
    }

    /**
     * getFooterHeight.
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight();
    }

    /**
     * getFooterHeightFullscreen.
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeightFullscreen() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(getImageViewAttributes(PageType.viewFullscreen, null));
    }

    /**
     * getFooterHeightCrowd.
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeightCrowd() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(getImageViewAttributes(PageType.editContent, null));
    }

    /**
     * isRememberImageZoom.
     *
     * @return true if the image zoom level should be remembered across page navigation, false otherwise
     */
    public boolean isRememberImageZoom() {
        return DataManager.getInstance().getConfiguration().isRememberImageZoom();
    }

    /**
     * isRememberImageRotation.
     *
     * @return true if the image rotation angle should be remembered across page navigation, false otherwise
     */
    public boolean isRememberImageRotation() {
        return DataManager.getInstance().getConfiguration().isRememberImageRotation();
    }

    /**
     * isDisplayStatistics.
     *
     * @return true if the statistics widget should be displayed, false otherwise
     */
    public boolean isDisplayStatistics() {
        return DataManager.getInstance().getConfiguration().isDisplayStatistics();
    }

    /**
     * isDisplaySearchRssLinks.
     *
     * @should return correct value
     * @return true if RSS feed links should be displayed on search result pages, false otherwise
     */
    public boolean isDisplaySearchRssLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchRssLinks();
    }

    /**
     * isAdvancedSearchEnabled.
     *
     * @should return correct value
     * @return true if the advanced search feature is enabled, false otherwise
     */
    public boolean isAdvancedSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchEnabled();
    }

    /**
     * isTimelineSearchEnabled.
     *
     * @should return correct value
     * @return true if the timeline search feature is enabled, false otherwise
     */
    public boolean isTimelineSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isTimelineSearchEnabled();
    }

    /**
     * isCalendarSearchEnabled.
     *
     * @should return correct value
     * @return true if the calendar search feature is enabled, false otherwise
     */
    public boolean isCalendarSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isCalendarSearchEnabled();
    }

    /**
     * isDisplayBreadcrumbs.
     *
     * @return true if breadcrumb navigation should be displayed, false otherwise
     */
    public boolean isDisplayBreadcrumbs() {
        return DataManager.getInstance().getConfiguration().getDisplayBreadcrumbs();
    }

    /**
     * isDisplayMetadataPageLinkBlock.
     *
     * @return true if the metadata page link block should be displayed, false otherwise
     */
    public boolean isDisplayMetadataPageLinkBlock() {
        return DataManager.getInstance().getConfiguration().getDisplayMetadataPageLinkBlock();
    }

    /**
     * isPagePdfEnabled.
     *
     * @return true if PDF download for individual pages is enabled, false otherwise
     */
    public boolean isPagePdfEnabled() {
        return DataManager.getInstance().getConfiguration().isPagePdfEnabled();
    }

    /**
     * getRssTitle.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRssTitle() {
        return DataManager.getInstance().getConfiguration().getRssTitle();
    }

    /**
     * isDisplayTagCloudStartpage.
     *
     * @return true if the tag cloud widget should be displayed on the start page, false otherwise
     */
    public boolean isDisplayTagCloudStartpage() {
        return DataManager.getInstance().getConfiguration().isDisplayTagCloudStartpage();
    }

    /**
     * isDisplaySearchResultNavigation.
     *
     * @return true if navigation arrows between search results should be displayed in record view, false otherwise
     */
    public boolean isDisplaySearchResultNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchResultNavigation();
    }

    /**
     * isDisplayStructType.
     *
     * @return true if the document structure type label should be displayed, false otherwise
     */
    public boolean isDisplayStructType() {
        return DataManager.getInstance().getConfiguration().getDisplayStructType();
    }

    /**
     * isDisplayCollectionBrowsing.
     *
     * @return true if the collection browsing widget should be displayed, false otherwise
     */
    public boolean isDisplayCollectionBrowsing() {
        return DataManager.getInstance().getConfiguration().isDisplayCollectionBrowsing();
    }

    /**
     * isDisplayUserNavigation.
     *
     * @return true if the user account navigation links should be displayed, false otherwise
     */
    public boolean isDisplayUserNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplayUserNavigation();
    }

    /**
     * isDisplayTagCloudNavigation.
     *
     * @return true if the tag cloud widget should be displayed in the navigation area, false otherwise
     */
    public boolean isDisplayTagCloudNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplayTagCloudNavigation();
    }

    /**
     * isDisplayTitlePURL.
     *
     * @return true if the persistent URL should be displayed in the record title area, false otherwise
     */
    public boolean isDisplayTitlePURL() {
        return DataManager.getInstance().getConfiguration().isDisplayTitlePURL();
    }

    /**
     * isSidebarTocWidgetVisibleInFullscreen.
     *
     * @return true if the sidebar table-of-contents widget should be visible in fullscreen view, false otherwise
     */
    public boolean isSidebarTocWidgetVisibleInFullscreen() {
        return DataManager.getInstance().getConfiguration().isSidebarTocWidgetVisibleInFullscreen();
    }

    /**
     * isSidebarOpacLinkVisible.
     *
     * @return true if the OPAC link should be visible in the sidebar views widget, false otherwise
     */
    public boolean isSidebarOpacLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetOpacLinkVisible();
    }

    /**
     * isSidebarTocPageNumbersVisible.
     *
     * @return true if page numbers should be visible in the sidebar table-of-contents, false otherwise
     */
    public boolean isSidebarTocPageNumbersVisible() {
        return DataManager.getInstance().getConfiguration().getSidebarTocPageNumbersVisible();
    }

    /**
     * isSidebarPageLinkVisible.
     *
     * @should return correct value
     * @return true if the object view link should be visible in the sidebar views widget, false otherwise
     */
    public boolean isSidebarPageLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetObjectViewLinkVisible();
    }

    /**
     * isSidebarCalendarLinkVisible.
     *
     * @should return correct value
     * @return true if the calendar view link should be visible in the sidebar views widget, false otherwise
     * @deprecated View has been retired
     */
    @Deprecated(since = "26.03")
    public boolean isSidebarCalendarLinkVisible() {
        return false;
    }

    /**
     * isSidebarMetadataLinkVisible.
     *
     * @should return correct value
     * @return true if the metadata view link should be visible in the sidebar views widget, false otherwise
     */
    public boolean isSidebarMetadataLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetMetadataViewLinkVisible();
    }

    /**
     * isSidebarThumbsLinkVisible.
     *
     * @should return correct value
     * @return true if the thumbnail view link should be visible in the sidebar views widget, false otherwise
     */
    public boolean isSidebarThumbsLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetThumbsViewLinkVisible();
    }

    /**
     * isSidebarFulltextLinkVisible.
     *
     * @should return correct value
     * @return true if the full-text view link should be visible in the sidebar views widget, false otherwise
     */
    public boolean isSidebarFulltextLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetFulltextLinkVisible();
    }

    /**
     * isTocTreeView.
     *
     * @param docStructType document structure type name to check
     * @return true if the table-of-contents for the given document structure type should be rendered as a tree, false otherwise
     */
    public boolean isTocTreeView(String docStructType) {
        return DataManager.getInstance().getConfiguration().isTocTreeView(docStructType);
    }

    /**
     * isSidebarTocTreeView.
     *
     * @return true if the sidebar table-of-contents should be rendered as a tree, false otherwise
     */
    public boolean isSidebarTocTreeView() {
        return DataManager.getInstance().getConfiguration().isSidebarTocTreeView();
    }

    /**
     * getSidebarTocLengthBeforeCut.
     *
     * @return a int.
     */
    public int getSidebarTocLengthBeforeCut() {
        return DataManager.getInstance().getConfiguration().getSidebarTocLengthBeforeCut();
    }

    /**
     * Return the layout type for TOCs of anchor records. Dafaults to 'list'
     * 
     * @return a string
     */
    public String getTocAnchorLayout() {
        return DataManager.getInstance().getConfiguration().getTocAnchorLayout();
    }

    /**
     * isDisplayTitleBreadcrumbs.
     *
     * @return true if title breadcrumbs should be displayed in the record view, false otherwise
     */
    public boolean isDisplayTitleBreadcrumbs() {
        return DataManager.getInstance().getConfiguration().getDisplayTitleBreadcrumbs();
    }

    /**
     * getTitleBreadcrumbsMaxTitleLength.
     *
     * @return a int.
     */
    public int getTitleBreadcrumbsMaxTitleLength() {
        return DataManager.getInstance().getConfiguration().getTitleBreadcrumbsMaxTitleLength();
    }

    /**
     * isDisplayTimeMatrix.
     *
     * @return true if the time matrix widget should be displayed, false otherwise
     */
    public boolean isDisplayTimeMatrix() {
        return DataManager.getInstance().getConfiguration().isDisplayTimeMatrix();
    }

    /**
     * isDisplayCrowdsourcingModuleLinks.
     *
     * @return true if links to the crowdsourcing module should be displayed, false otherwise
     */
    public boolean isDisplayCrowdsourcingModuleLinks() {
        return DataManager.getInstance().getConfiguration().isDisplayCrowdsourcingModuleLinks();
    }

    /**
     * getTimeMatrixStartYear.
     *
     * @param subTheme sub-theme discriminator value used to restrict the Solr query
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getTimeMatrixStartYear(String subTheme) throws PresentationException, IndexUnreachableException {
        String value = DataManager.getInstance().getConfiguration().getStartYearForTimeline();
        if ("MIN".equals(value)) {
            String subQuery = null;
            if (StringUtils.isNotBlank(subTheme)) {
                subQuery = String.format("+%s:%s", DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField(), subTheme);
            }
            return SearchHelper.getMinMaxYears(subQuery)[0];
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logger.error("'{}' is not a valid value for 'startyear'", value);
            return 0;
        }
    }

    /**
     * getTimeMatrixEndYear.
     *
     * @param subTheme sub-theme discriminator value used to restrict the Solr query
     * @return a int.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     */
    public int getTimeMatrixEndYear(String subTheme) throws PresentationException, IndexUnreachableException {
        String value = DataManager.getInstance().getConfiguration().getEndYearForTimeline();
        if ("MAX".equals(value)) {
            String subQuery = null;
            if (StringUtils.isNotBlank(subTheme)) {
                subQuery = String.format("+%s:%s", DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField(), subTheme);
            }
            return SearchHelper.getMinMaxYears(subQuery)[1];
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logger.error("'{}' is not a valid value for 'startyear'", value);
            return 2500;
        }
    }

    /**
     * getTimeMatrixHits.
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

    /**
     * isPiwikTracking.
     *
     * @return true if Matomo (Piwik) tracking is enabled, false otherwise
     */
    public boolean isPiwikTrackingEnabled() {
        return DataManager.getInstance().getConfiguration().isPiwikTrackingEnabled();
    }

    /**
     * getPiwikBaseURL.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikBaseURL() {
        return DataManager.getInstance().getConfiguration().getPiwikBaseURL();
    }

    /**
     * getPiwikSiteID.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikSiteID() {
        return DataManager.getInstance().getConfiguration().getPiwikSiteID();
    }

    /**
     * getAnchorThumbnailMode.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAnchorThumbnailMode() {
        return DataManager.getInstance().getConfiguration().getAnchorThumbnailMode();
    }

    /**
     * getSortFields.
     *
     * @return a {@link java.util.List} object.
     */
    public List<String> getSortFields() {
        return DataManager.getInstance()
                .getConfiguration()
                .getSortFields()
                .stream()
                .filter(field -> !Configuration.isLanguageVersionOtherThan(field, BeanUtils.getLocale().getLanguage()))
                .toList();
    }

    /**
     * getTocIndentation.
     *
     * @return a int.
     */
    public int getTocIndentation() {
        return DataManager.getInstance().getConfiguration().getTocIndentation();
    }

    /**
     * isPageBrowseEnabled.
     *
     * @return true if the page browse feature (jumping by fixed step sizes) is enabled, false otherwise
     */
    public boolean isPageBrowseEnabled() {
        return DataManager.getInstance().getConfiguration().isPageBrowseEnabled();
    }

    public List<Integer> getPageBrowseSteps() {
        return DataManager.getInstance().getConfiguration().getPageBrowseSteps();
    }

    /**
     * isPageBrowseStep1Visible.
     *
     * @return true if the first configured page browse step is valid and should be displayed, false otherwise
     */
    public boolean isPageBrowseStep1Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        return steps != null && !steps.isEmpty() && steps.get(0) > 0;
    }

    /**
     * isPageBrowseStep2Visible.
     *
     * @return true if the second configured page browse step is valid and should be displayed, false otherwise
     */
    public boolean isPageBrowseStep2Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        return steps != null && steps.size() > 1 && steps.get(1) > 0;
    }

    /**
     * isPageBrowseStep3Visible.
     *
     * @return true if the third configured page browse step is valid and should be displayed, false otherwise
     */
    public boolean isPageBrowseStep3Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        return steps != null && steps.size() > 2 && steps.get(2) > 1;
    }

    /**
     * getPageBrowseStep1.
     *
     * @return a int.
     */
    public int getPageBrowseStep1() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && !steps.isEmpty() && steps.get(0) > 1) {
            return steps.get(0);
        }
        return 0;
    }

    /**
     * getPageBrowseStep2.
     *
     * @return a int.
     */
    public int getPageBrowseStep2() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 1 && steps.get(1) > 1) {
            return steps.get(1);
        }
        return 0;
    }

    /**
     * getPageBrowseStep3.
     *
     * @return a int.
     */
    public int getPageBrowseStep3() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        if (steps != null && steps.size() > 2 && steps.get(2) > 1) {
            return steps.get(2);
        }
        return 0;
    }

    /**
     *
     * @return the configured minimum number of pages required to display the page-select dropdown
     */
    public int getPageSelectDropdownDisplayMinPages() {
        return DataManager.getInstance().getConfiguration().getPageSelectDropdownDisplayMinPages();
    }

    /**
     * getReCaptchaSiteKey.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReCaptchaSiteKey() {
        return DataManager.getInstance().getConfiguration().getReCaptchaSiteKey();
    }

    /**
     * isUseReCaptcha.
     *
     * @return true if reCAPTCHA verification is enabled for forms, false otherwise
     */
    public boolean isUseReCaptcha() {
        return DataManager.getInstance().getConfiguration().isUseReCaptcha();
    }

    /**
     * isTocEpubEnabled.
     *
     * @return true if EPUB download via the table-of-contents is enabled and PDF generation via message queue is active, false otherwise
     */
    public boolean isTocEpubEnabled() {
        return DataManager.getInstance().getConfiguration().isTocEpubEnabled() && isGeneratePdfInMessageQueue();
    }

    /**
     * isGeneratePdfInTaskManager.
     *
     * @return true if PDF generation is handled via the message queue, false otherwise
     */
    public boolean isGeneratePdfInMessageQueue() {
        return DataManager.getInstance().getConfiguration().isGeneratePdfInMessageQueue();
    }

    /**
     * isDocHierarchyPdfEnabled.
     *
     * @return true if PDF download for entire document hierarchies is enabled, false otherwise
     */
    public boolean isDocHierarchyPdfEnabled() {
        return DataManager.getInstance().getConfiguration().isDocHierarchyPdfEnabled();
    }

    /**
     * isShowSearchInItemOnlyIfFullTextAvailable.
     *
     * @return true if the in-record search feature should only be offered when full-text is available, false otherwise
     */
    public boolean isShowSearchInItemOnlyIfFullTextAvailable() {
        return DataManager.getInstance().getConfiguration().isSearchInItemOnlyIfFullTextAvailable();
    }

    /**
     * isContentUploadEnabled.
     *
     * @return true if content upload by users is enabled, false otherwise
     */
    public boolean isContentUploadEnabled() {
        return DataManager.getInstance().getConfiguration().isContentUploadEnabled();
    }

    /**
     * isTranskribusEnabled.
     *
     * @return true if the Transkribus integration is enabled, false otherwise
     */
    public boolean isTranskribusEnabled() {
        return DataManager.getInstance().getConfiguration().isTranskribusEnabled();
    }

    /**
     * isSearchExcelExportEnabled.
     *
     * @return true if exporting search results as an Excel file is enabled, false otherwise
     */
    public boolean isSearchExcelExportEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchExcelExportEnabled();
    }

    /**
     * isSearchRisExportEnabled.
     *
     * @return true if exporting search results in RIS format is enabled, false otherwise
     */
    public boolean isSearchRisExportEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchRisExportEnabled();
    }

    /**
     * isSitelinksEnabled.
     *
     * @return true if Wikidata Sitelinks integration is enabled, false otherwise
     */
    public boolean isSitelinksEnabled() {
        return DataManager.getInstance().getConfiguration().isSitelinksEnabled();
    }

    /**
     * getRestApiUrl.
     *
     * @return REST API URL
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRestApiUrl() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getRestApiUrl();
    }

    /**
     *
     * @return the configured REST API URL for IIIF Presentation
     */
    public String getRestApiUrlForIIIFPresention() {
        return DataManager.getInstance().getRestApiManager().getIIIFDataApiUrl();
    }

    /**
     * getIiifApiUrl.
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getIiifApiUrl() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getIIIFApiUrl();
    }

    /**
     * @return The url to the /api/v1 Rest Api
     */
    public String getRestApiUrlV1() {
        return DataManager.getInstance().getConfiguration().getRestApiUrl().replace("/rest", "/api/v1");
    }

    /**
     * getTranslation.
     *
     * @param language ISO 639 language code to translate
     * @param locale target locale code for the output language name
     * @return a {@link java.lang.String} object.
     */
    public String getTranslation(String language, String locale) {
        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(language);
        if (lang == null) {
            return language;
        }
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

    /**
     * 
     * @param view Record view name
     * @return List of sidebar widget names to display in the given view (in the intended order)
     */
    public List<String> getSidebarWidgetsForView(String view) {
        return DataManager.getInstance().getConfiguration().getSidebarWidgetsForView(view);
    }

    /**
     * 
     * @param view Record view name
     * @param widget Widget name
     * @return true if widget configured as collapsible; false otherwise; default is false
     */
    public boolean isSidebarWidgetForViewCollapsible(String view, String widget) {
        return DataManager.getInstance().getConfiguration().isSidebarWidgetForViewCollapsible(view, widget);
    }

    /**
     * 
     * @param view Record view name
     * @param widget Widget name
     * @return true if widget configured as collapsed by default and collapsible in general; false otherwise; default is false
     */
    public boolean isSidebarWidgetForViewCollapsedByDefault(String view, String widget) {
        return isSidebarWidgetForViewCollapsible(view, widget)
                && DataManager.getInstance().getConfiguration().isSidebarWidgetForViewCollapsedByDefault(view, widget);
    }

    /**
     * isDisplaySidebarBrowsingTerms.
     *
     * @return true if the browsing terms widget should be displayed in the sidebar, false otherwise
     * @should return correct value
     */
    public boolean isDisplaySidebarBrowsingTerms() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarBrowsingTerms();
    }

    /**
     * isDisplaySidebarRssFeed.
     *
     * <p>s
     *
     * @return true if the RSS feed widget should be displayed in the sidebar, false otherwise
     * @should return correct value
     */
    public boolean isDisplaySidebarRssFeed() {
        return DataManager.getInstance().getConfiguration().isSidebarRssFeedWidgetEnabled();
    }

    /**
     * isDisplaySidebarWidgetUsageCitationLinks.
     *
     * @return true if citation links should be displayed in the sidebar usage widget, false otherwise
     */
    public boolean isDisplaySidebarWidgetUsageCitationLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetCitationCitationLinks();
    }

    /**
     * isDisplaySidebarWidgetUsageCitationRecommendation.
     *
     * @return true if citation recommendations should be displayed in the sidebar usage widget, false otherwise
     */
    public boolean isDisplaySidebarWidgetUsageCitationRecommendation() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetCitationCitationRecommendation();
    }

    /**
     *
     * @return List of configured <code>DownloadOption</code> items
     */
    public List<DownloadOption> getSidebarWidgetUsagePageDownloadOptions() {
        return DataManager.getInstance().getConfiguration().getSidebarWidgetDownloadsPageDownloadOptions();
    }

    public boolean isDisplaySidebarWidgetUsagePageDownloadOptions() {
        return DataManager.getInstance().getConfiguration().isDisplayWidgetDownloadsDownloadOptions();
    }

    /**
     *
     * @return List of available citation style names
     */
    public List<String> getSidebarWidgetUsageCitationRecommendationStyles() {
        return DataManager.getInstance().getConfiguration().getSidebarWidgetCitationCitationRecommendationStyles();
    }

    /**
     * isSubthemeDiscriminatorFieldSet.
     *
     * @return true if a sub-theme discriminator field has been configured, false otherwise
     */
    public boolean isSubthemeDiscriminatorFieldSet() {
        return StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    /**
     *
     * @return true if sub-theme selection via URL parameter pull is enabled, false otherwise
     */
    public boolean isPullThemeEnabled() {
        return DataManager.getInstance().getConfiguration().isPullThemeEnabled();
    }

    /**
     * getTwitterName.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTwitterName() {
        return DataManager.getInstance().getConfiguration().getTwitterUserName();
    }

    /**
     *
     * @return the configured copyright indicator style name
     */
    public String getCopyrightIndicatorStyle() {
        return DataManager.getInstance().getConfiguration().getCopyrightIndicatorStyle();
    }

    /**
     *
     * @return true if social media share links should be displayed, false otherwise
     */
    public boolean isDisplaySocialMediaShareLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySocialMediaShareLinks();
    }

    /**
     *
     * @return the configured MapBox API token
     */
    public String getMapBoxToken() {
        return DataManager.getInstance().getConfiguration().getMapBoxToken();
    }

    /**
     *
     * @return the configured MapBox username
     */
    public String getMapBoxUser() {
        return DataManager.getInstance().getConfiguration().getMapBoxUser();
    }

    /**
     *
     * @return the configured MapBox style ID
     */
    public String getMapBoxStyleId() {
        return DataManager.getInstance().getConfiguration().getMapBoxStyleId();
    }

    /**
     *
     * @return the configured list of selectable search hits-per-page values
     */
    public List<Integer> getSearchHitsPerPageValues() {
        return DataManager.getInstance().getConfiguration().getSearchHitsPerPageValues();
    }

    /**
     *
     * @return the configured maximum number of child hits loaded initially per search hit
     */
    public int getSearchChildHitsInitialLoadLimit() {
        return DataManager.getInstance().getConfiguration().getSearchChildHitsInitialLoadLimit();
    }

    public int getSearchChildHitsToLoadOnExpand() {
        return DataManager.getInstance().getConfiguration().getSearchChildHitsToLoadOnExpand();
    }

    /**
     *
     * @return the configured list of e-mail recipients for user feedback messages
     */
    public List<EmailRecipient> getFeedbackEmailRecipients() {
        return DataManager.getInstance().getConfiguration().getFeedbackEmailRecipients();
    }

    /**
     *
     * @return true if default sorting field is 'RANDOM'; false otherwise
     */
    public boolean isDefaultSortFieldRandom() {
        return SolrConstants.SORT_RANDOM.equals(DataManager.getInstance().getConfiguration().getDefaultSortField(null));
    }

    /**
     *
     * @return true if user-generated content should be displayed below the image, false otherwise
     */
    public boolean isDisplayUserGeneratedContentBelowImage() {
        return DataManager.getInstance().getConfiguration().isDisplayUserGeneratedContentBelowImage();
    }

    /**
     * @param template metadata template name to look up navigation types for
     * @param fallbackToDefaultTemplate if true, falls back to the default template when the given template has no configuration
     * @return true if docstruct navigation is enabled and properly configured; false otherwise
     */
    public boolean isDisplayDocstructNavigation(String template, boolean fallbackToDefaultTemplate) {
        return DataManager.getInstance().getConfiguration().isDocstructNavigationEnabled()
                && !DataManager.getInstance().getConfiguration().getDocstructNavigationTypes(template, fallbackToDefaultTemplate).isEmpty();
    }

    /**
     *
     * @return true if annotation text should be displayed overlaid on the image, false otherwise
     */
    public boolean isDisplayAnnotationTextInImage() {
        return DataManager.getInstance().getConfiguration().isDisplayAnnotationTextInImage();
    }

    /**
     *
     * @return true if the address search input should be shown in map views, false otherwise
     */
    public boolean isDisplayAddressSearchInMap() {
        return DataManager.getInstance().getConfiguration().isDisplayAddressSearchInMap();
    }

    /**
     * @param field Solr sort field name
     * @return the configured message key for the ascending sort label of the given field
     */
    public String getSearchSortingAscendingKey(String field) {
        return DataManager.getInstance().getConfiguration().getSearchSortingKeyAscending(field).orElse("searchSortingDropdown_ascending");
    }

    /**
     * @param field Solr sort field name
     * @return the configured message key for the descending sort label of the given field
     */
    public String getSearchSortingDescendingKey(String field) {
        return DataManager.getInstance().getConfiguration().getSearchSortingKeyDescending(field).orElse("searchSortingDropdown_descending");
    }

    /**
     * 
     * @return List of configured advanced search template names
     */
    public List<String> getAdvancedSearchTemplateNames() {
        return DataManager.getInstance().getConfiguration().getAdvancedSearchTemplateNames();
    }

    /**
     * 
     * @return true if number of configured advanced search templates greater than 1; false otherwise
     */
    public boolean isAdvancedSearchTemplatesEnabled() {
        return DataManager.getInstance().getConfiguration().getAdvancedSearchTemplateNames().size() > 1;
    }

    /**
     * 
     * @return true if result groups enabled; false otherwise
     */
    public boolean isSearchResultGroupsEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchResultGroupsEnabled();
    }

    /**
     * 
     * @return List of names of the configured search result groups
     * @should return all values
     */
    public List<String> getSearchResultGroupNames() {
        // logger.trace("getSearchResultGroupNames");
        return DataManager.getInstance()
                .getConfiguration()
                .getSearchResultGroups()
                .stream()
                .map(SearchResultGroup::getName)
                .toList();
    }

    /**
     *
     * @param facetField Solr facet field name
     * @return true if facet field labels for the given field should be translated, false otherwise
     */
    public boolean isTranslateFacetFieldLabels(String facetField) {
        return DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels(facetField);
    }

    /**
     *
     * @return true if a heatmap should be used instead of individual markers for the map search, false otherwise
     */
    public boolean useHeatmapForMapSearch() {
        return DataManager.getInstance().getConfiguration().useHeatmapForMapSearch();
    }

    /**
     *
     * @return the configured geo map marker used for the map search
     */
    public GeoMapMarker getMarkerForMapSearch() {
        return DataManager.getInstance().getConfiguration().getMarkerForMapSearch();
    }

    /**
     *
     * @return the configured selection highlight color for the map search
     */
    public String getSelectionColorForMapSearch() {
        return DataManager.getInstance().getConfiguration().getSelectionColorForMapSearch();
    }

    /**
     *
     * @return true if a heatmap should be used instead of individual markers for geo facetting, false otherwise
     */
    public boolean useHeatmapForFacetting() {
        return DataManager.getInstance().getConfiguration().useHeatmapForFacetting();
    }

    /**
     *
     * @return the configured geo map marker used for geo facetting
     */
    public GeoMapMarker getMarkerForFacetting() {
        return DataManager.getInstance().getConfiguration().getMarkerForFacetting();
    }

    /**
     *
     * @return the configured selection highlight color for geo facetting
     */
    public String getSelectionColorForFacetting() {
        return DataManager.getInstance().getConfiguration().getSelectionColorForFacetting();
    }

    /**
     *
     * @return true if a heatmap should be used instead of individual markers for CMS map components, false otherwise
     */
    public boolean useHeatmapForCMSMaps() {
        return DataManager.getInstance().getConfiguration().useHeatmapForCMSMaps();
    }

    /**
     *
     * @return the configured default geo map marker for CMS map components, falling back to a marker named "default"
     */
    public GeoMapMarker getDefaultMarkerForCMSMaps() {
        List<GeoMapMarker> markers = DataManager.getInstance().getConfiguration().getGeoMapMarkers();
        return markers.stream()
                .filter(m -> m.getName().equalsIgnoreCase("default"))
                .findAny()
                .orElse(new GeoMapMarker("default"));
    }

    /**
     *
     * @return the configured default zoom level used when centering the map on a geo annotation
     */
    public int getGeomapAnnotationZoom() {
        return DataManager.getInstance().getConfiguration().getGeomapAnnotationZoom();
    }

    public String getCampaignGeomapInitialViewAsJson() {
        int zoom = DataManager.getInstance().getConfiguration().getCrowdsourcingCampaignGeomapZoom();
        String lngLatString = DataManager.getInstance().getConfiguration().getCrowdsourcingCampaignGeomapLngLat();

        JSONArray lngLatArray = new JSONArray("[" + lngLatString + "]");
        JSONObject view = new JSONObject();
        view.put("zoom", zoom);
        view.put("center", lngLatArray);

        return view.toString();
    }

    /**
     *
     * @return the configured tile source URL for the crowdsourcing campaign geo map
     */
    public String getCampaignGeomapTilesource() {
        return DataManager.getInstance().getConfiguration().getCrowdsourcingCampaignGeomapTilesource();
    }

    /**
     *
     * @return true if the admin configuration editor is enabled, false otherwise
     */
    public boolean isConfigEditorEnabled() {
        return DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
    }

    /**
     *
     * @return true if sequential hit numbers should be displayed next to search results, false otherwise
     */
    public boolean isDisplaySearchHitNumbers() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchHitNumbers();
    }

    public List<SelectItem> getGeomapFeatureTitleOptions() {
        return DataManager.getInstance()
                .getConfiguration()
                .getGeomapFeatureTitleOptions()
                .stream()
                .map(item -> new SelectItem(item.getValue(), ViewerResourceBundle.getTranslation(item.getLabel(), BeanUtils.getLocale())))
                .toList();
    }

    public List<Metadata> getMetadataConfiguration(String type) {
        return getMetadataConfiguration(type, "_DEFAULT");
    }

    public List<Metadata> getMetadataConfiguration(String type, String template) {
        return DataManager.getInstance().getConfiguration().getMetadataConfigurationForTemplate(type, template, true, true);
    }

    /**
     *
     * @param name PageType name string to look up
     * @return the configured URL path segment for the given page type name
     */
    public String getPageType(String name) {
        return DataManager.getInstance().getConfiguration().getPageType(PageType.getByName(name));
    }

    /**
     *
     * @param facetField Solr facet field name
     * @return true if a value filter input should be displayed for the given facet field, false otherwise
     */
    public boolean isFacetFieldDisplayValueFilter(String facetField) {
        return DataManager.getInstance().getConfiguration().isFacetFieldDisplayValueFilter(facetField);
    }

    /**
     *
     * @param facetField Solr facet field name
     * @return true if the given facet field is configured as a boolean field, false otherwise
     * @should return correct value
     */
    public boolean isFacetFieldTypeBoolean(String facetField) {
        return DataManager.getInstance().getConfiguration().getBooleanFacetFields().contains(facetField);
    }

    /**
     *
     * @param facetField Solr facet field name
     * @return the configured message key for the description tooltip of the given facet field; null if none configured
     */
    public String getFacetFieldDescriptionKey(String facetField) {
        return DataManager.getInstance().getConfiguration().getFacetFieldDescriptionKey(facetField);
    }

    public boolean isPdfPageRangeEnabled() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetDownloadsPdfPageRange();
    }

    /**
     * isSidebarTocViewLinkVisible.
     *
     * @should return correct value
     * @return true if the table-of-contents view link should be visible in the sidebar views widget, false otherwise
     */
    public boolean isSidebarTocViewLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetTocViewLinkVisible();
    }

    public List<String> getGeomapFilters() {
        return DataManager.getInstance()
                .getConfiguration()
                .getGeomapFilters()
                .stream()
                .filter(GeomapItemFilter::isVisible)
                .map(f -> f.getName())
                .toList();
    }

    public float getGeomapClusterDistanceMultiplier() {
        return DataManager.getInstance().getConfiguration().getGeomapClusterDistanceMultiplier();
    }

    public int getGeomapClusterRadius() {
        return DataManager.getInstance().getConfiguration().getGeomapClusterRadius();
    }

    public Integer getGeomapDisableClusteringAtZoom() {
        return DataManager.getInstance().getConfiguration().getGeomapDisableClusteringAtZoom();
    }

    private ViewAttributes getImageViewAttributes(PageType pageType, String mimeType) {
        return new ViewAttributes(new MimeType(mimeType), null, null, null, pageType);
    }
}
