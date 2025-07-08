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
     * <p>
     * getModules.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<IModule> getModules() {
        return DataManager.getInstance().getModules();
    }

    /**
     * <p>
     * getName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return DataManager.getInstance().getConfiguration().getName();
    }

    /**
     * <p>
     * isBookmarksEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isBookmarksEnabled() {
        return DataManager.getInstance().getConfiguration().isBookmarksEnabled();
    }

    public boolean isSearchSavingEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchSavingEnabled();
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.getByName(pageType), getImageType(mimeType).getFormat().getMimeType());
    }

    /**
     * whether to show a navigator element in the openseadragon viewe
     * 
     * @param pageType get settings for this pageType
     * @param mimeType get settings for this image type
     * @return true if navigator should be shown
     * @throws ViewerConfigurationException
     */
    public boolean showImageNavigator(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .showImageNavigator(PageType.getByName(pageType), mimeType);
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .getFooterHeight(PageType.getByName(pageType), mimeType);
    }

    /**
     * <p>
     * getImageSizes.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public List<String> getImageSizes(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .getImageViewZoomScales(PageType.getByName(pageType), mimeType);
    }

    /**
     * <p>
     * getTileSizes.
     * </p>
     *
     * @param pageType a {@link java.lang.String} object.
     * @param mimeType a {@link java.lang.String} object.
     * @return a {@link java.util.Map} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public Map<Integer, List<Integer>> getTileSizes(String pageType, String mimeType) throws ViewerConfigurationException {
        return DataManager.getInstance()
                .getConfiguration()
                .getTileSizes(PageType.getByName(pageType), mimeType);
    }

    /**
     * <p>
     * useTiles.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTiles() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles();
    }

    /**
     * <p>
     * useTilesFullscreen.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesFullscreen() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * useTilesCrowd.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public boolean useTilesCrowd() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().useTiles(PageType.editContent, null);
    }

    /**
     * <p>
     * getFooterHeight.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeight() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight();
    }

    /**
     * <p>
     * getFooterHeightFullscreen.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeightFullscreen() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.viewFullscreen, null);
    }

    /**
     * <p>
     * getFooterHeightCrowd.
     * </p>
     *
     * @return a int.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public int getFooterHeightCrowd() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getFooterHeight(PageType.editContent, null);
    }

    /**
     * <p>
     * isRememberImageZoom.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isRememberImageZoom() {
        return DataManager.getInstance().getConfiguration().isRememberImageZoom();
    }

    /**
     * <p>
     * isRememberImageRotation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isRememberImageRotation() {
        return DataManager.getInstance().getConfiguration().isRememberImageRotation();
    }

    /**
     * <p>
     * isDisplayStatistics.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayStatistics() {
        return DataManager.getInstance().getConfiguration().isDisplayStatistics();
    }

    /**
     * <p>
     * isDisplaySearchRssLinks.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isDisplaySearchRssLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchRssLinks();
    }

    /**
     * <p>
     * isAdvancedSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isAdvancedSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isAdvancedSearchEnabled();
    }

    /**
     * <p>
     * isTimelineSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isTimelineSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isTimelineSearchEnabled();
    }

    /**
     * <p>
     * isCalendarSearchEnabled.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isCalendarSearchEnabled() {
        return DataManager.getInstance().getConfiguration().isCalendarSearchEnabled();
    }

    /**
     * <p>
     * isDisplayBreadcrumbs.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayBreadcrumbs() {
        return DataManager.getInstance().getConfiguration().getDisplayBreadcrumbs();
    }

    /**
     * <p>
     * isDisplayMetadataPageLinkBlock.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayMetadataPageLinkBlock() {
        return DataManager.getInstance().getConfiguration().getDisplayMetadataPageLinkBlock();
    }

    /**
     * <p>
     * isPagePdfEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPagePdfEnabled() {
        return DataManager.getInstance().getConfiguration().isPagePdfEnabled();
    }

    /**
     * <p>
     * getRssTitle.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getRssTitle() {
        return DataManager.getInstance().getConfiguration().getRssTitle();
    }

    /**
     * <p>
     * isDisplayTagCloudStartpage.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTagCloudStartpage() {
        return DataManager.getInstance().getConfiguration().isDisplayTagCloudStartpage();
    }

    /**
     * <p>
     * isDisplaySearchResultNavigation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplaySearchResultNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplaySearchResultNavigation();
    }

    /**
     * <p>
     * isDisplayStructType.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayStructType() {
        return DataManager.getInstance().getConfiguration().getDisplayStructType();
    }

    /**
     * <p>
     * isDisplayCollectionBrowsing.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayCollectionBrowsing() {
        return DataManager.getInstance().getConfiguration().isDisplayCollectionBrowsing();
    }

    /**
     * <p>
     * isDisplayUserNavigation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayUserNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplayUserNavigation();
    }

    /**
     * <p>
     * isDisplayTagCloudNavigation.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTagCloudNavigation() {
        return DataManager.getInstance().getConfiguration().isDisplayTagCloudNavigation();
    }

    /**
     * <p>
     * isDisplayTitlePURL.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTitlePURL() {
        return DataManager.getInstance().getConfiguration().isDisplayTitlePURL();
    }

    /**
     * <p>
     * isSidebarTocWidgetVisible.
     * </p>
     *
     * @return a boolean.
     * @deprecated Widgets are now implicitly enabled by being added to a record view configuration
     */
    @Deprecated(since = "25.06")
    public boolean isSidebarTocWidgetVisible() {
        return true;
    }

    /**
     * <p>
     * isSidebarTocWidgetVisibleInFullscreen.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarTocWidgetVisibleInFullscreen() {
        return DataManager.getInstance().getConfiguration().isSidebarTocWidgetVisibleInFullscreen();
    }

    /**
     * <p>
     * isSidebarOpacLinkVisible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarOpacLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetOpacLinkVisible();
    }

    /**
     * <p>
     * isSidebarTocPageNumbersVisible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarTocPageNumbersVisible() {
        return DataManager.getInstance().getConfiguration().getSidebarTocPageNumbersVisible();
    }

    /**
     * <p>
     * isSidebarPageLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarPageLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetObjectViewLinkVisible();
    }

    /**
     * <p>
     * isSidebarCalendarLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarCalendarLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetCalendarViewLinkVisible();
    }

    /**
     * <p>
     * isSidebarMetadataLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarMetadataLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetMetadataViewLinkVisible();
    }

    /**
     * <p>
     * isSidebarThumbsLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarThumbsLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetThumbsViewLinkVisible();
    }

    /**
     * <p>
     * isSidebarFulltextLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
     */
    public boolean isSidebarFulltextLinkVisible() {
        return DataManager.getInstance().getConfiguration().isSidebarViewsWidgetFulltextLinkVisible();
    }

    /**
     * <p>
     * isTocTreeView.
     * </p>
     *
     * @param docStructType a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isTocTreeView(String docStructType) {
        return DataManager.getInstance().getConfiguration().isTocTreeView(docStructType);
    }

    /**
     * <p>
     * isSidebarTocTreeView.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSidebarTocTreeView() {
        return DataManager.getInstance().getConfiguration().isSidebarTocTreeView();
    }

    /**
     * <p>
     * getSidebarTocLengthBeforeCut.
     * </p>
     *
     * @return a int.
     */
    public int getSidebarTocLengthBeforeCut() {
        return DataManager.getInstance().getConfiguration().getSidebarTocLengthBeforeCut();
    }

    /**
     * <p>
     * isDisplayTitleBreadcrumbs.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTitleBreadcrumbs() {
        return DataManager.getInstance().getConfiguration().getDisplayTitleBreadcrumbs();
    }

    /**
     * <p>
     * getTitleBreadcrumbsMaxTitleLength.
     * </p>
     *
     * @return a int.
     */
    public int getTitleBreadcrumbsMaxTitleLength() {
        return DataManager.getInstance().getConfiguration().getTitleBreadcrumbsMaxTitleLength();
    }

    /**
     * <p>
     * isDisplayTimeMatrix.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayTimeMatrix() {
        return DataManager.getInstance().getConfiguration().isDisplayTimeMatrix();
    }

    /**
     * <p>
     * isDisplayCrowdsourcingModuleLinks.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplayCrowdsourcingModuleLinks() {
        return DataManager.getInstance().getConfiguration().isDisplayCrowdsourcingModuleLinks();
    }

    /**
     * <p>
     * getTimeMatrixStartYear.
     * </p>
     * 
     * @param subTheme
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
     * <p>
     * getTimeMatrixEndYear.
     * </p>
     * 
     * @param subTheme
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
     * <p>
     * getTimeMatrixHits.
     * </p>
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
     * <p>
     * isPiwikTracking.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPiwikTrackingEnabled() {
        return DataManager.getInstance().getConfiguration().isPiwikTrackingEnabled();
    }

    /**
     * <p>
     * getPiwikBaseURL.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikBaseURL() {
        return DataManager.getInstance().getConfiguration().getPiwikBaseURL();
    }

    /**
     * <p>
     * getPiwikSiteID.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPiwikSiteID() {
        return DataManager.getInstance().getConfiguration().getPiwikSiteID();
    }

    /**
     * <p>
     * getAnchorThumbnailMode.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAnchorThumbnailMode() {
        return DataManager.getInstance().getConfiguration().getAnchorThumbnailMode();
    }

    /**
     * <p>
     * getSortFields.
     * </p>
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
     * <p>
     * getTocIndentation.
     * </p>
     *
     * @return a int.
     */
    public int getTocIndentation() {
        return DataManager.getInstance().getConfiguration().getTocIndentation();
    }

    /**
     * <p>
     * isPageBrowseEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPageBrowseEnabled() {
        return DataManager.getInstance().getConfiguration().isPageBrowseEnabled();
    }

    public List<Integer> getPageBrowseSteps() {
        return DataManager.getInstance().getConfiguration().getPageBrowseSteps();
    }

    /**
     * <p>
     * isPageBrowseStep1Visible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPageBrowseStep1Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        return steps != null && !steps.isEmpty() && steps.get(0) > 0;
    }

    /**
     * <p>
     * isPageBrowseStep2Visible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPageBrowseStep2Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        return steps != null && steps.size() > 1 && steps.get(1) > 0;
    }

    /**
     * <p>
     * isPageBrowseStep3Visible.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPageBrowseStep3Visible() {
        List<Integer> steps = DataManager.getInstance().getConfiguration().getPageBrowseSteps();
        return steps != null && steps.size() > 2 && steps.get(2) > 1;
    }

    /**
     * <p>
     * getPageBrowseStep1.
     * </p>
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
     * <p>
     * getPageBrowseStep2.
     * </p>
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
     * <p>
     * getPageBrowseStep3.
     * </p>
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
     * @return Configured value
     */
    public int getPageSelectDropdownDisplayMinPages() {
        return DataManager.getInstance().getConfiguration().getPageSelectDropdownDisplayMinPages();
    }

    /**
     * <p>
     * getReCaptchaSiteKey.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReCaptchaSiteKey() {
        return DataManager.getInstance().getConfiguration().getReCaptchaSiteKey();
    }

    /**
     * <p>
     * isUseReCaptcha.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isUseReCaptcha() {
        return DataManager.getInstance().getConfiguration().isUseReCaptcha();
    }

    /**
     * <p>
     * isTocEpubEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isTocEpubEnabled() {
        return DataManager.getInstance().getConfiguration().isTocEpubEnabled() && isGeneratePdfInMessageQueue();
    }

    /**
     * <p>
     * isGeneratePdfInTaskManager.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isGeneratePdfInMessageQueue() {
        return DataManager.getInstance().getConfiguration().isGeneratePdfInMessageQueue();
    }

    /**
     * <p>
     * isDocHierarchyPdfEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDocHierarchyPdfEnabled() {
        return DataManager.getInstance().getConfiguration().isDocHierarchyPdfEnabled();
    }

    /**
     * <p>
     * isShowSearchInItem.
     * </p>
     *
     * @return a boolean.
     * @deprecated Widgets are now implicitly enabled by being added to a record view configuration
     */
    @Deprecated(since = "25.06")
    public boolean isShowSearchInItem() {
        return true;
    }

    /**
     * <p>
     * isShowSearchInItemOnlyIfFullTextAvailable.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isShowSearchInItemOnlyIfFullTextAvailable() {
        return DataManager.getInstance().getConfiguration().isSearchInItemOnlyIfFullTextAvailable();
    }

    /**
     * <p>
     * isContentUploadEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isContentUploadEnabled() {
        return DataManager.getInstance().getConfiguration().isContentUploadEnabled();
    }

    /**
     * <p>
     * isTranskribusEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isTranskribusEnabled() {
        return DataManager.getInstance().getConfiguration().isTranskribusEnabled();
    }

    /**
     * <p>
     * isSearchExcelExportEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSearchExcelExportEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchExcelExportEnabled();
    }

    /**
     * <p>
     * isSearchRisExportEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSearchRisExportEnabled() {
        return DataManager.getInstance().getConfiguration().isSearchRisExportEnabled();
    }

    /**
     * <p>
     * isSitelinksEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSitelinksEnabled() {
        return DataManager.getInstance().getConfiguration().isSitelinksEnabled();
    }

    /**
     * <p>
     * getRestApiUrl.
     * </p>
     *
     * @return REST API URL
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public String getRestApiUrl() throws ViewerConfigurationException {
        return DataManager.getInstance().getConfiguration().getRestApiUrl();
    }

    /**
     *
     * @return Configured value
     */
    public String getRestApiUrlForIIIFPresention() {
        return DataManager.getInstance().getRestApiManager().getIIIFDataApiUrl();
    }

    /**
     * <p>
     * getIiifApiUrl.
     * </p>
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
     * <p>
     * getIso6391.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    @Deprecated(since = "25.04")
    public String getIso6391(String language) {
        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(language);
        if (lang != null) {
            return lang.getIsoCodeOld();
        }

        return language;
    }

    /**
     * <p>
     * getIso6392B.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    @Deprecated(since = "25.04")
    public String getIso6392B(String language) {
        Language lang = DataManager.getInstance().getLanguageHelper().getLanguage(language);
        if (lang != null) {
            return lang.getIsoCode();
        }

        return language;
    }

    /**
     * <p>
     * getTranslation.
     * </p>
     *
     * @param language a {@link java.lang.String} object.
     * @param locale a {@link java.lang.String} object.
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
     * <p>
     * isDisplaySidebarBrowsingTerms.
     * </p>
     *
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarBrowsingTerms() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarBrowsingTerms();
    }

    /**
     * <p>
     * isDisplaySidebarRssFeed.
     * </p>
     * s
     * 
     * @return a boolean.
     * @should return correct value
     */
    public boolean isDisplaySidebarRssFeed() {
        return DataManager.getInstance().getConfiguration().isSidebarRssFeedWidgetEnabled();
    }

    /**
     * <p>
     * isDisplaySidebarWidgetUsageCitationLinks.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isDisplaySidebarWidgetUsageCitationLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetCitationCitationLinks();
    }

    /**
     * <p>
     * isDisplaySidebarWidgetUsageCitationRecommendation.
     * </p>
     *
     * @return a boolean.
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
     * <p>
     * isSubthemeDiscriminatorFieldSet.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isSubthemeDiscriminatorFieldSet() {
        return StringUtils.isNotEmpty(DataManager.getInstance().getConfiguration().getSubthemeDiscriminatorField());
    }

    /**
     * 
     * @return a boolean.
     */
    public boolean isPullThemeEnabled() {
        return DataManager.getInstance().getConfiguration().isPullThemeEnabled();
    }

    /**
     * <p>
     * getTwitterName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTwitterName() {
        return DataManager.getInstance().getConfiguration().getTwitterUserName();
    }

    /**
     * 
     * @return Configured value
     * @deprecated Widgets are now implicitly enabled by being added to a record view configuration
     */
    @Deprecated(since = "25.06")
    public boolean isCopyrightIndicatorEnabled() {
        return true;
    }

    /**
     * 
     * @return Configured value
     */
    public String getCopyrightIndicatorStyle() {
        return DataManager.getInstance().getConfiguration().getCopyrightIndicatorStyle();
    }

    /**
     * 
     * @return Configured value
     */
    public boolean isDisplaySocialMediaShareLinks() {
        return DataManager.getInstance().getConfiguration().isDisplaySocialMediaShareLinks();
    }

    /**
     * 
     * @return Configured value
     */
    public String getMapBoxToken() {
        return DataManager.getInstance().getConfiguration().getMapBoxToken();
    }

    /**
     * 
     * @return Configured value
     */
    public String getMapBoxUser() {
        return DataManager.getInstance().getConfiguration().getMapBoxUser();
    }

    /**
     * 
     * @return Configured value
     */
    public String getMapBoxStyleId() {
        return DataManager.getInstance().getConfiguration().getMapBoxStyleId();
    }

    /**
     * 
     * @return Configured value
     */
    public List<Integer> getSearchHitsPerPageValues() {
        return DataManager.getInstance().getConfiguration().getSearchHitsPerPageValues();
    }

    /**
     * 
     * @return Configured value
     */
    public int getSearchChildHitsInitialLoadLimit() {
        return DataManager.getInstance().getConfiguration().getSearchChildHitsInitialLoadLimit();
    }

    public int getSearchChildHitsToLoadOnExpand() {
        return DataManager.getInstance().getConfiguration().getSearchChildHitsToLoadOnExpand();
    }

    /**
     * 
     * @return Configured value
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
     * @return Configured value
     */
    public boolean isDisplayUserGeneratedContentBelowImage() {
        return DataManager.getInstance().getConfiguration().isDisplayUserGeneratedContentBelowImage();
    }

    /**
     * @param template
     * @param fallbackToDefaultTemplate
     * @return true if docstruct navigation is enabled and properly configured; false otherwise
     */
    public boolean isDisplayDocstructNavigation(String template, boolean fallbackToDefaultTemplate) {
        return DataManager.getInstance().getConfiguration().isDocstructNavigationEnabled()
                && !DataManager.getInstance().getConfiguration().getDocstructNavigationTypes(template, fallbackToDefaultTemplate).isEmpty();
    }

    /**
     * 
     * @return Configured value
     */
    public boolean isDisplayAnnotationTextInImage() {
        return DataManager.getInstance().getConfiguration().isDisplayAnnotationTextInImage();
    }

    /**
     * 
     * @return Configured value
     */
    public boolean isDisplayAddressSearchInMap() {
        return DataManager.getInstance().getConfiguration().isDisplayAddressSearchInMap();
    }

    /**
     * @param field
     * @return Configured value
     */
    public String getSearchSortingAscendingKey(String field) {
        return DataManager.getInstance().getConfiguration().getSearchSortingKeyAscending(field).orElse("searchSortingDropdown_ascending");
    }

    /**
     * @param field
     * @return Configured value
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
        logger.trace("getSearchResultGroupNames");
        return DataManager.getInstance()
                .getConfiguration()
                .getSearchResultGroups()
                .stream()
                .map(SearchResultGroup::getName)
                .toList();
    }

    /**
     *
     * @param facetField
     * @return Configured value
     */
    public boolean isTranslateFacetFieldLabels(String facetField) {
        return DataManager.getInstance().getConfiguration().isTranslateFacetFieldLabels(facetField);
    }

    /**
     * 
     * @return Configured value
     */
    public boolean useHeatmapForMapSearch() {
        return DataManager.getInstance().getConfiguration().useHeatmapForMapSearch();
    }

    /**
     * 
     * @return Configured value
     */
    public GeoMapMarker getMarkerForMapSearch() {
        return DataManager.getInstance().getConfiguration().getMarkerForMapSearch();
    }

    /**
     * 
     * @return Configured value
     */
    public String getSelectionColorForMapSearch() {
        return DataManager.getInstance().getConfiguration().getSelectionColorForMapSearch();
    }

    /**
     * 
     * @return Configured value
     */
    public boolean useHeatmapForFacetting() {
        return DataManager.getInstance().getConfiguration().useHeatmapForFacetting();
    }

    /**
     * 
     * @return Configured value
     */
    public GeoMapMarker getMarkerForFacetting() {
        return DataManager.getInstance().getConfiguration().getMarkerForFacetting();
    }

    /**
     * 
     * @return Configured value
     */
    public String getSelectionColorForFacetting() {
        return DataManager.getInstance().getConfiguration().getSelectionColorForFacetting();
    }

    /**
     * 
     * @return Configured value
     */
    public boolean useHeatmapForCMSMaps() {
        return DataManager.getInstance().getConfiguration().useHeatmapForCMSMaps();
    }

    /**
     * 
     * @return Configured value
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
     * @return Configured value
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
     * @return Configured value
     */
    public String getCampaignGeomapTilesource() {
        return DataManager.getInstance().getConfiguration().getCrowdsourcingCampaignGeomapTilesource();
    }

    /**
     * 
     * @return Configured value
     */
    public boolean isConfigEditorEnabled() {
        return DataManager.getInstance().getConfiguration().isConfigEditorEnabled();
    }

    /**
     * 
     * @return Configured value
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
     * @param name
     * @return Configured value
     */
    public String getPageType(String name) {
        return DataManager.getInstance().getConfiguration().getPageType(PageType.getByName(name));
    }

    /**
     *
     * @param facetField
     * @return Configured value
     */
    public boolean isFacetFieldDisplayValueFilter(String facetField) {
        return DataManager.getInstance().getConfiguration().isFacetFieldDisplayValueFilter(facetField);
    }

    /**
     *
     * @param facetField
     * @return Configured value
     * @should return correct value
     */
    public boolean isFacetFieldTypeBoolean(String facetField) {
        return DataManager.getInstance().getConfiguration().getBooleanFacetFields().contains(facetField);
    }

    /**
     * 
     * @param facetField
     * @return Configured value; null if none found
     */
    public String getFacetFieldDescriptionKey(String facetField) {
        return DataManager.getInstance().getConfiguration().getFacetFieldDescriptionKey(facetField);
    }

    public boolean isPdfPageRangeEnabled() {
        return DataManager.getInstance().getConfiguration().isDisplaySidebarWidgetDownloadsPdfPageRange();
    }

    /**
     * <p>
     * isSidebarTocViewLinkVisible.
     * </p>
     *
     * @should return correct value
     * @return a boolean.
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
}
