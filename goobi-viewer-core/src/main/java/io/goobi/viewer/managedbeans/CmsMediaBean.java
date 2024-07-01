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

import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.Part;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.v1.cms.CMSMediaResource;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.imaging.ThumbnailHandler;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.tabledata.TableDataSourceException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CategorizableTranslatedSelectable;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.cms.TranslatedSelectable;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.media.CMSMediaItemMetadata;
import io.goobi.viewer.model.security.user.User;
import jakarta.persistence.RollbackException;

/**
 * <p>
 * CmsMediaBean class.
 * </p>
 */
@Named
@SessionScoped
public class CmsMediaBean implements Serializable {

    private static final String GENERAL_FILTER = "GENERAL";
    private static final String FILENAME_FILTER = "FILENAME";

    private static final String[] IMAGE_FILE_TYPES = { "png", "jpeg", "gif", "tiff", "jp2", "svg", "ico" };
    private static final String IMAGE_FILE_TYPE_VALIDATION_REGEX = "png|jpe?g|gif|tiff?|jp2|svg|ico";
    private static final String[] VIDEO_FILE_TYPES = { "mpeg4", "avi", "mov", "wmv" };
    private static final String VIDEO_FILE_TYPE_VALIDATION_REGEX = "mp4|mpeg4|avi|mov|wmv";
    private static final String[] AUDIO_FILE_TYPES = { "mp3", "mpeg", "wav", "ogg", "wma" };
    private static final String AUDIO_FILE_TYPE_VALIDATION_REGEX = "mp3|mpeg|wav|ogg|wma";
    private static final String[] DOCUMENT_FILE_TYPES = { "pdf" };
    private static final String DOCUMENT_FILE_TYPE_VALIDATION_REGEX = "pdf";
    private static final String FILE_TYPE_REGEX_TEMPLATE = "(?i).*\\.(%s)";

    private static final int ENTRIES_PER_PAGE = 40;

    private static final long serialVersionUID = 1156829371382069634L;

    private static final Logger logger = LogManager.getLogger(CmsMediaBean.class);

    @Inject
    protected UserBean userBean;

    private String selectedTag;
    //    private List<Selectable<CMSMediaItem>> mediaItems = null;
    private TableDataProvider<CategorizableTranslatedSelectable<CMSMediaItem>> dataProvider;
    private CategorizableTranslatedSelectable<CMSMediaItem> selectedMediaItem = null;
    private String filter = "";
    private String filenameFilter = "";
    private boolean allSelected = false;

    /**
     * <p>
     * Constructor for CmsMediaBean.
     * </p>
     */
    public CmsMediaBean() {
        super();
        resetData();
    }

    /**
     * Reload all media items, along with the available categories
     */
    public void resetData() {
        dataProvider = initDataProvider();
    }

    /**
     * @return TableDataProvider<CategorizableTranslatedSelectable<CMSMediaItem>>
     */
    private TableDataProvider<CategorizableTranslatedSelectable<CMSMediaItem>> initDataProvider() {
        TableDataProvider<CategorizableTranslatedSelectable<CMSMediaItem>> dp =
                new TableDataProvider<>(new TableDataSource<CategorizableTranslatedSelectable<CMSMediaItem>>() {

                    private List<CategorizableTranslatedSelectable<CMSMediaItem>> items = null;
                    private boolean reloadNeeded = true;

                    @Override
                    public List<CategorizableTranslatedSelectable<CMSMediaItem>> getEntries(int first, int pageSize, String sortField,
                            SortOrder sortOrder, Map<String, String> filters) throws TableDataSourceException {

                        Stream<CategorizableTranslatedSelectable<CMSMediaItem>> stream = getItems(filters).stream();

                        if (StringUtils.isNotBlank(sortField)) {
                            Comparator<Selectable<CMSMediaItem>> comparator;
                            switch (sortField.toUpperCase()) {
                                case "TITLE":
                                    comparator = (i1, i2) -> i1.getValue().getName().compareTo(i2.getValue().getName());
                                    break;
                                case "DATE":
                                default:
                                    comparator = (i1, i2) -> i1.getValue().getLastModifiedTime().compareTo(i2.getValue().getLastModifiedTime());
                                    break;
                            }
                            if (SortOrder.DESCENDING.equals(sortOrder)) {
                                comparator = comparator.reversed();
                            }
                            stream = stream.sorted(comparator);
                        } else {
                            stream = stream.sorted((i1, i2) -> i2.getValue().getLastModifiedTime().compareTo(i1.getValue().getLastModifiedTime()));
                        }
                        return stream.skip(first).limit(pageSize).collect(Collectors.toList());
                    }

                    @Override
                    public long getTotalNumberOfRecords(Map<String, String> filters) {
                        return getItems(filters).size();
                    }

                    @Override
                    public void resetTotalNumberOfRecords() {
                        reloadNeeded = true;
                    }

                    private List<CategorizableTranslatedSelectable<CMSMediaItem>> getItems(Map<String, String> filters) {
                        if (this.items == null || this.reloadNeeded) {
                            try {
                                Stream<CMSMediaItem> stream = getAllMedia().stream();

                                if (filters != null && !filters.isEmpty()) {
                                    String generalFilter = filters.get(GENERAL_FILTER);
                                    String fFilter = filters.get(FILENAME_FILTER);

                                    if (StringUtils.isNotBlank(generalFilter)) {
                                        stream = stream.filter(item -> item.getMetadata()
                                                .stream()
                                                .anyMatch(md -> md.getName() != null
                                                        && md.getName().toLowerCase().contains(generalFilter.toLowerCase()))
                                                || item.getCategories()
                                                        .stream()
                                                        .anyMatch(cat -> cat.getName().toLowerCase().contains(generalFilter.toLowerCase()))
                                                || item.getFileName().toLowerCase().contains(generalFilter.toLowerCase()));

                                    }

                                    if (StringUtils.isNotBlank(fFilter)) {
                                        stream = stream.filter(item -> item.getFileName().matches(fFilter));
                                    }
                                }

                                List<CMSCategory> categories = userBean.getUser().getAllowedCategories(getAllMediaCategories());
                                this.items = stream.map(item -> new CategorizableTranslatedSelectable<>(item, false,
                                        item.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()),
                                        item.wrapCategories(categories))).collect(Collectors.toList());
                                reloadNeeded = false;

                            } catch (DAOException e) {
                                throw new TableDataSourceException("Failed to load CMSMediaItems", e);
                            }
                        }
                        return this.items;
                    }

                });
        dp.setEntriesPerPage(ENTRIES_PER_PAGE);
        dp.getFilter(GENERAL_FILTER);
        dp.getFilter(FILENAME_FILTER);
        return dp;
    }

    /**
     * <p>
     * createMediaItem.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     */
    public CMSMediaItem createMediaItem() {
        CMSMediaItem item = new CMSMediaItem();
        for (Locale locale : CmsBean.getAllLocales()) {
            CMSMediaItemMetadata metadata = new CMSMediaItemMetadata();
            metadata.setLanguage(locale.getLanguage());
            item.addMetadata(metadata);
        }
        return item;
    }

    /**
     * <p>
     * deleteMedia.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteMedia(CMSMediaItem item) throws DAOException {
        IDAO dao = DataManager.getInstance().getDao();
        if (dao == null) {
            return;
        }

        try {
            boolean deleted = dao.deleteCMSMediaItem(item);
            if (deleted && item.getFileName() != null) {
                try {
                    Path mediaFile = item.getFilePath();
                    Files.delete(mediaFile);
                    if (Files.exists(mediaFile)) {
                        throw new IOException("Cannot delete file " + mediaFile.toAbsolutePath());
                    }
                } catch (IOException e) {
                    logger.error("Failed to delete media file: {}", e.getMessage());
                }
            }
            if (this.selectedMediaItem != null && this.selectedMediaItem.getValue() == item) {
                this.selectedMediaItem = null;
            }
            if (!deleted) {
                Messages.error(null, "admin__media_delete_error_inuse", item.getFileName());
            } else {
                CMSMediaResource.removeFromImageCache(item);
            }
            reloadMediaList(false);
        } catch (RollbackException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("cannot delete or update a parent row")) {
                Messages.error(null, "admin__media_delete_error_inuse", item.getFileName());
            } else {
                logger.error("Error deleting category ", e);
                Messages.error(null, "admin__media_delete_error", item.getFileName(), e.getMessage());
            }
        }
    }

    /**
     * <p>
     * getAllMedia.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSMediaItem> getAllMedia() throws DAOException {
        List<CMSMediaItem> items = new ArrayList<>(DataManager.getInstance().getDao().getAllCMSMediaItems());

        if (userBean != null && userBean.getUser() != null && userBean.getUser().isCmsAdmin()) {
            User user = userBean.getUser();
            if (user.hasPrivilegeForAllCategories()) {
                return items;
            }
            List<CMSCategory> allowedCategories = user.getAllowedCategories(DataManager.getInstance().getDao().getAllCategories());
            items = items.stream()
                    .filter(item -> !ListUtils.intersection(item.getCategories(), allowedCategories).isEmpty())
                    .collect(Collectors.toList());
            return items;
        }

        return Collections.emptyList();
    }

    /**
     * <p>
     * Getter for the field <code>dataProvider</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.tabledata.TableDataProvider} object.
     */
    public TableDataProvider<CategorizableTranslatedSelectable<CMSMediaItem>> getDataProvider() {
        return this.dataProvider;
    }

    /**
     * <p>
     * getMediaItems.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CategorizableTranslatedSelectable<CMSMediaItem>> getMediaItems() throws DAOException {
        return this.dataProvider.getPaginatorList();
    }

    /**
     * <p>
     * reloadMediaList.
     * </p>
     */
    public void reloadMediaList() {
        reloadMediaList(true);
    }

    /**
     * <p>
     * reloadMediaList.
     * </p>
     *
     * @param resetCurrentPage a boolean.
     */
    public void reloadMediaList(boolean resetCurrentPage) {
        long page = this.dataProvider.getPageNumberCurrent();
        this.dataProvider.resetAll();
        this.dataProvider.getFilter(GENERAL_FILTER).setValue(filter);
        this.dataProvider.getFilter(FILENAME_FILTER).setValue(filenameFilter);
        if (!resetCurrentPage) {
            this.dataProvider.setTxtMoveTo((int) page);
        }
    }

    /**
     * Deletes all mediaItems from {@link #getMediaItems()} which are are marked as selected. Reloads the media list
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void deleteSelectedItems() throws DAOException {
        Stream<CategorizableTranslatedSelectable<CMSMediaItem>> stream = this.dataProvider.getPaginatorList().stream();
        if (!isAllSelected()) {
            stream = stream.filter(Selectable::isSelected);
        }
        List<CMSMediaItem> itemsToDelete = stream.map(Selectable::getValue).collect(Collectors.toList());
        for (CMSMediaItem item : itemsToDelete) {
            deleteMedia(item);
        }
        //reset selected status after gobal action
        setAllSelected(false);
    }

    /**
     * Saves all mediaItems from {@link #getMediaItems()} which are are marked as selected. Reloads the media list
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveSelectedItems() throws DAOException {
        List<CategorizableTranslatedSelectable<CMSMediaItem>> itemsToSave =
                this.dataProvider.getPaginatorList().stream().filter(Selectable::isSelected).collect(Collectors.toList());
        for (CategorizableTranslatedSelectable<CMSMediaItem> item : itemsToSave) {
            saveMedia(item.getValue(), item.getCategories());
        }
    }

    /**
     * <p>
     * getMediaUrl.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a {@link java.lang.String} object.
     * @throws java.lang.NumberFormatException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static String getMediaUrl(CMSMediaItem item) throws NumberFormatException {
        return getMediaUrl(item, null, null);
    }

    /**
     * <p>
     * getMediaUrl.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @param width a {@link java.lang.String} object.
     * @param height a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static String getMediaUrl(CMSMediaItem item, String width, String height) {
        if (item == null || StringUtils.isEmpty(item.getFileName())) {
            return "";
        }

        if (item.isHasExportableText()) {
            StringBuilder sbUri = new StringBuilder();
            sbUri.append(DataManager.getInstance().getConfiguration().getRestApiUrl()).append("cms/media/get/item/").append(item.getId());
            return sbUri.toString();
        }

        switch (item.getContentType()) {
            case CMSMediaItem.CONTENT_TYPE_PDF:
            case CMSMediaItem.CONTENT_TYPE_XML:
            case CMSMediaItem.CONTENT_TYPE_SVG:
            case CMSMediaItem.CONTENT_TYPE_ICO:
                return ThumbnailHandler.getCMSMediaImageApiUrl(item.getFileName());
            case CMSMediaItem.CONTENT_TYPE_GIF:
                return ThumbnailHandler.getCMSMediaImageApiUrl(item.getFileName()) + "/full.gif";
            default:
                Dimension imageSize = getRequestImageSize(width, height);
                return BeanUtils.getImageDeliveryBean()
                        .getThumbs()
                        .getThumbnailUrl(Optional.ofNullable(item), imageSize.width, imageSize.height);

        }
    }

    /**
     * If both with and height are blank, return a size of 0x0, which will be interpreted as 'max' size for IIIF If one dimension is blank and the
     * other not, fill the blank dimension with the configured maximal image size Otherwise return a size matching both arguments
     * 
     * @param width
     * @param height
     * @return Dimension
     */
    private static Dimension getRequestImageSize(String width, String height) {
        Dimension imageSize;
        if (StringUtils.isAllBlank(width, height)) {
            imageSize = new Dimension(0, 0);
        } else if (StringUtils.isBlank(height)) {
            imageSize = new Dimension(Integer.parseInt(width), DataManager.getInstance().getConfiguration().getViewerMaxImageHeight());
        } else if (StringUtils.isBlank(width)) {
            imageSize = new Dimension(DataManager.getInstance().getConfiguration().getViewerMaxImageWidth(), Integer.parseInt(height));
        } else {
            imageSize = new Dimension(Integer.parseInt(width), Integer.parseInt(height));
        }
        return imageSize;
    }

    /**
     * <p>
     * getMediaFileAsString.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static String getMediaFileAsString(CMSMediaItem item) {
        if (item == null || StringUtils.isEmpty(item.getFileName())) {
            return "";
        }

        StringBuilder sbUri = new StringBuilder();
        sbUri.append(DataManager.getInstance().getConfiguration().getRestApiUrl()).append("cms/media/get/item/").append(item.getId());
        try {
            return NetTools.getWebContentGET(sbUri.toString());
        } catch (IOException | HTTPException e) {
            logger.error(e.getMessage(), e);
        }

        return "";
    }

    /**
     * <p>
     * getMediaPreviewUrl.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a {@link java.lang.String} object.
     * @throws java.lang.NumberFormatException if any.
     * @throws io.goobi.viewer.exceptions.ViewerConfigurationException if any.
     */
    public static String getMediaPreviewUrl(CMSMediaItem item) throws NumberFormatException {
        if (item != null && item.getFileName() != null) {
            return getMediaUrl(item, null, "160");
        }
        return "";
    }

    /**
     * <p>
     * isImage.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     */
    public boolean isImage(CMSMediaItem item) {
        return item != null && item.getFileName().matches(getImageFilter());
    }

    /**
     * <p>
     * isVideo.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     */
    public boolean isVideo(CMSMediaItem item) {
        return item != null && item.getFileName().matches(getVideoFilter());
    }

    /**
     * <p>
     * isAudio.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     */
    public boolean isAudio(CMSMediaItem item) {
        return item != null && item.getFileName().matches(getAudioFilter());
    }

    /**
     * <p>
     * isText.
     * </p>
     *
     * @param item a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @return a boolean.
     */
    public boolean isText(CMSMediaItem item) {
        return item != null && item.getFileName().matches(getDocumentFilter());
    }

    /**
     * <p>
     * saveSelectedMediaItem.
     * </p>
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveSelectedMediaItem() throws DAOException {
        if (this.selectedMediaItem != null) {
            saveMedia(this.selectedMediaItem.getValue());
        }
    }

    /**
     * Save media item, adding or removing the given categories, depending wether they are selected or not. if
     * {@link io.goobi.viewer.model.security.user.User#hasPrivilegeForAllSubthemeDiscriminatorValues()} is false for the current user and none of the
     * given categories is selected, then don't change the media categories since doing so would break category restrictions
     *
     * @param media a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @param categories a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveMedia(CMSMediaItem media, List<Selectable<CMSCategory>> categories) throws DAOException {
        if (media == null) {
            return;
        }

        if (categories != null) {
            if (BeanUtils.getUserBean().getUser().hasPrivilegeForAllSubthemeDiscriminatorValues()
                    || categories.stream().anyMatch(Selectable::isSelected)) {
                for (Selectable<CMSCategory> category : categories) {
                    if (category.isSelected()) {
                        media.addCategory(category.getValue());
                    } else {
                        media.removeCategory(category.getValue());
                    }
                }
            } else {
                Messages.error(null, "admin__media_save_error_must_have_category", media.toString());
            }

        }
        saveMedia(media);
    }

    /**
     * <p>
     * saveMedia.
     * </p>
     *
     * @param media a {@link io.goobi.viewer.model.cms.media.CMSMediaItem} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void saveMedia(CMSMediaItem media) throws DAOException {
        if (media != null) {
            if (media.getId() == null) {
                DataManager.getInstance().getDao().addCMSMediaItem(media);
            } else {
                DataManager.getInstance().getDao().updateCMSMediaItem(media);
            }
        }
        reloadMediaList(false);
    }

    /**
     * <p>
     * getFileName.
     * </p>
     *
     * @param filePart a {@link javax.servlet.http.Part} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getFileName(Part filePart) {
        if (filePart != null) {
            String header = filePart.getHeader("content-disposition");
            for (String headerPart : header.split(";")) {
                if (headerPart.trim().startsWith("filename")) {
                    return headerPart.substring(headerPart.indexOf('=') + 1).trim().replace("\"", "");
                }
            }
        }
        return null;
    }

    /**
     * <p>
     * Setter for the field <code>selectedTag</code>.
     * </p>
     *
     * @param selectedTag the selectedTag to set
     */
    public void setSelectedTag(String selectedTag) {
        this.selectedTag = selectedTag;
    }

    /**
     * <p>
     * Getter for the field <code>selectedTag</code>.
     * </p>
     *
     * @return the selectedTag
     */
    public String getSelectedTag() {
        return selectedTag;
    }

    /**
     * <p>
     * getAllMediaCategories.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCategory> getAllMediaCategories() throws DAOException {
        return DataManager.getInstance().getDao().getAllCategories();
    }

    /**
     * <p>
     * getImageFilter.
     * </p>
     *
     * @return a regex matching only filenames ending with one of the supported image format suffixes
     */
    public static String getImageFilter() {
        return String.format(FILE_TYPE_REGEX_TEMPLATE, IMAGE_FILE_TYPE_VALIDATION_REGEX);
    }

    public static String getVideoFilter() {
        return String.format(FILE_TYPE_REGEX_TEMPLATE, VIDEO_FILE_TYPE_VALIDATION_REGEX);
    }

    public static String getAudioFilter() {
        return String.format(FILE_TYPE_REGEX_TEMPLATE, AUDIO_FILE_TYPE_VALIDATION_REGEX);
    }

    public static String getDocumentFilter() {
        return String.format(FILE_TYPE_REGEX_TEMPLATE, DOCUMENT_FILE_TYPE_VALIDATION_REGEX);
    }

    public static String getMediaFilter() {
        return String.format("(%s)|(%s)|(%s)|(%s)", getImageFilter(), getVideoFilter(), getAudioFilter(), getDocumentFilter());
    }

    public static String getImageTypes() {
        return Stream.of(IMAGE_FILE_TYPES).collect(Collectors.joining(", "));
    }

    public static String getVideoTypes() {
        return Stream.of(VIDEO_FILE_TYPES).collect(Collectors.joining(", "));
    }

    public static String getAudioTypes() {
        return Stream.of(AUDIO_FILE_TYPES).collect(Collectors.joining(", "));
    }

    public static String getDocumentTypes() {
        return Stream.of(DOCUMENT_FILE_TYPES).collect(Collectors.joining(", "));
    }

    public static String getAllTypes() {
        return Stream.of(getImageTypes(), getVideoTypes(), getAudioTypes(), getDocumentTypes()).collect(Collectors.joining(", "));
    }

    /**
     * <p>
     * Getter for the field <code>filter</code>.
     * </p>
     *
     * @return the filter
     */
    public String getFilter() {
        return filter;
    }

    /**
     * <p>
     * Setter for the field <code>filter</code>.
     * </p>
     *
     * @param filter the filter to set
     */
    public void setFilter(String filter) {
        if (!this.filter.equals(filter)) {
            this.filter = filter == null ? "" : filter;
            reloadMediaList(true);
        }
    }

    /**
     * <p>
     * Getter for the field <code>filenameFilter</code>.
     * </p>
     *
     * @return the filter
     */
    public String getFilenameFilter() {
        return filenameFilter;
    }

    /**
     * <p>
     * Setter for the field <code>filenameFilter</code>.
     * </p>
     *
     * @param filter the filter to set
     */
    public void setFilenameFilter(String filter) {
        if (!this.filenameFilter.equals(filter)) {
            this.filenameFilter = filter == null ? "" : filter;
            reloadMediaList(true);
        }
    }

    /**
     * <p>
     * Setter for the field <code>selectedMediaItem</code>.
     * </p>
     *
     * @param selectedMediaItem the selectedMediaItem to set
     */
    public void setSelectedMediaItem(CategorizableTranslatedSelectable<CMSMediaItem> selectedMediaItem) {
        this.selectedMediaItem = selectedMediaItem;
    }

    /**
     * <p>
     * toggleSelectedMediaItem.
     * </p>
     *
     * @param selectedMediaItem a {@link io.goobi.viewer.model.cms.CategorizableTranslatedSelectable} object.
     */
    public void toggleSelectedMediaItem(CategorizableTranslatedSelectable<CMSMediaItem> selectedMediaItem) {
        if (this.selectedMediaItem != null && this.selectedMediaItem.equals(selectedMediaItem)) {
            setSelectedMediaItem(null);
        } else {
            setSelectedMediaItem(selectedMediaItem);
        }
    }

    /**
     * <p>
     * Getter for the field <code>selectedMediaItem</code>.
     * </p>
     *
     * @return the selectedMediaItem
     */
    public TranslatedSelectable<CMSMediaItem> getSelectedMediaItem() {
        return selectedMediaItem;
    }

    /**
     * <p>
     * Setter for the field <code>allSelected</code>.
     * </p>
     *
     * @param allSelected the allSelected to set
     */
    public void setAllSelected(boolean allSelected) {
        this.allSelected = allSelected;
    }

    /**
     * <p>
     * isAllSelected.
     * </p>
     *
     * @return the allSelected
     */
    public boolean isAllSelected() {
        return allSelected;
    }

    /**
     * <p>
     * needsPaginator.
     * </p>
     *
     * @return true if there is more than one page in the data-provider. False otherwise
     */
    public boolean needsPaginator() {
        if (this.getDataProvider() != null) {
            return this.getDataProvider().getLastPageNumber() > 0;
        }

        return false;
    }
}
