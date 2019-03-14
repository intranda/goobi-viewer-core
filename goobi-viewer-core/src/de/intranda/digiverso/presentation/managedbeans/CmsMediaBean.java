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
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.RollbackException;
import javax.servlet.http.Part;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.dao.IDAO;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.ViewerConfigurationException;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataProvider;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataProvider.SortOrder;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataSource;
import de.intranda.digiverso.presentation.managedbeans.tabledata.TableDataSourceException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.TranslatedSelectable;
import de.intranda.digiverso.presentation.model.cms.CMSCategory;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItemMetadata;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.Selectable;
import de.intranda.digiverso.presentation.model.security.user.User;

@Named
@SessionScoped
public class CmsMediaBean implements Serializable {

	private static final String GENERAL_FILTER = "GENERAL";
	private static final String FILENAME_FILTER = "FILENAME";

	private static final int ENTRIES_PER_PAGE = 40;

	private static final long serialVersionUID = 1156829371382069634L;

	private static final Logger logger = LoggerFactory.getLogger(CmsMediaBean.class);

	@Inject
	protected UserBean userBean;

	private String selectedTag;
//    private List<Selectable<CMSMediaItem>> mediaItems = null;
	private final TableDataProvider<TranslatedSelectable<CMSMediaItem>> dataProvider;
	private TranslatedSelectable<CMSMediaItem> selectedMediaItem = null;
	private String filter = "";
	private String filenameFilter = "";
	private boolean allSelected = false;
	

	public CmsMediaBean() {
		super();
		dataProvider = initDataProvider();
	}

	/**
	 * 
	 */
	private TableDataProvider<TranslatedSelectable<CMSMediaItem>> initDataProvider() {
		TableDataProvider<TranslatedSelectable<CMSMediaItem>> dataProvider = new TableDataProvider<TranslatedSelectable<CMSMediaItem>>(
				new TableDataSource<TranslatedSelectable<CMSMediaItem>>() {

					private List<TranslatedSelectable<CMSMediaItem>> items = null;
					private boolean reloadNeeded = true;
					
					@Override
					public List<TranslatedSelectable<CMSMediaItem>> getEntries(int first, int pageSize, String sortField,
							SortOrder sortOrder, Map<String, String> filters) throws TableDataSourceException {

						Stream<TranslatedSelectable<CMSMediaItem>> stream = getItems(filters).stream();

						
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
						List<TranslatedSelectable<CMSMediaItem>> list = stream.skip(first).limit(pageSize).collect(Collectors.toList());

						return list;
					}

					@Override
					public long getTotalNumberOfRecords(Map<String, String> filters) {
						return getItems(filters).size();
					}

					@Override
					public void resetTotalNumberOfRecords() {
						reloadNeeded = true;
					}
					
					private List<TranslatedSelectable<CMSMediaItem>> getItems(Map<String, String> filters) {
						if (this.items == null || this.reloadNeeded) {
							try {
								Stream<CMSMediaItem> stream = getAllMedia().stream();
		
								if(filters != null && !filters.isEmpty()) {
								String generalFilter = filters.get(GENERAL_FILTER);
								String filenameFilter = filters.get(FILENAME_FILTER);
								
									if(StringUtils.isNotBlank(generalFilter)) {								
										stream = stream.filter(
												item -> item.getMetadata().stream().anyMatch(md -> md.getName() != null && md.getName().toLowerCase().contains(generalFilter.toLowerCase())) 
												|| 
												item.getCategories().stream().anyMatch(cat -> cat.getName().toLowerCase().contains(generalFilter.toLowerCase())));
									}
									
									if(StringUtils.isNotBlank(filenameFilter)) {								
										stream = stream.filter(item -> item.getFileName().matches(filenameFilter));
									}
								}
									
								
								this.items = stream.map(item -> new TranslatedSelectable<CMSMediaItem>(item, false)).collect(Collectors.toList());
								reloadNeeded = false;
								
							} catch (DAOException e) {
								throw new TableDataSourceException("Failed to load CMSMediaItems", e);
							}
						}
						return this.items;
					}

				});
		dataProvider.setEntriesPerPage(ENTRIES_PER_PAGE);
		dataProvider.addFilter(GENERAL_FILTER);
		dataProvider.addFilter(FILENAME_FILTER);
		return dataProvider;
	}

	/**
	 * @param mediaFile2
	 * @param contentType
	 * @return false if the content type is html or xml and the file contains the
	 *         string "<script" (case insensitive)
	 * @throws IOException
	 */
	private static boolean validate(File file, String contentType) throws IOException {
		if (CMSMediaItem.CONTENT_TYPE_HTML.equals(contentType) || CMSMediaItem.CONTENT_TYPE_XML.equals(contentType)) {
			String content = FileUtils.readFileToString(file);
			return !content.toLowerCase().contains("<script");
		}

		return true;
	}

	/**
	 * @param contentType
	 * @param fileName
	 * @return true if supported; false otherwise
	 * @should return true for tiff
	 * @should return true for jpeg
	 * @should return true for jpeg 2000
	 * @should return true for png
	 * @should return true for docx
	 */
	private static boolean isValidMediaType(String contentType, String fileName) {
		logger.trace("isValidMediaType: {} - {}", contentType, fileName);
		switch (contentType) {
		case "image/tiff":
		case "image/jpeg":
		case "image/jp2":
		case "image/png":
		case CMSMediaItem.CONTENT_TYPE_DOC: // RTF
		case CMSMediaItem.CONTENT_TYPE_DOCX:
		case CMSMediaItem.CONTENT_TYPE_HTML:
		case CMSMediaItem.CONTENT_TYPE_RTF:
		case CMSMediaItem.CONTENT_TYPE_RTF2:
		case CMSMediaItem.CONTENT_TYPE_RTF3:
		case CMSMediaItem.CONTENT_TYPE_RTF4:
		case CMSMediaItem.CONTENT_TYPE_XML:
			return true;
		default:
			logger.warn("Unsupported media type: {}", contentType);
			return false;
		}
	}

	public CMSMediaItem createMediaItem() {
		CMSMediaItem item = new CMSMediaItem();
		for (Locale locale : CmsBean.getAllLocales()) {
			CMSMediaItemMetadata metadata = new CMSMediaItemMetadata();
			metadata.setLanguage(locale.getLanguage());
			item.addMetadata(metadata);
		}
		return item;
	}

	public List<CMSPage> getMediaOwnerPages(CMSMediaItem item) throws DAOException {
		IDAO dao = DataManager.getInstance().getDao();
		List<CMSPage> owners = new ArrayList<>();
		if (dao != null) {
			owners = dao.getMediaOwners(item);
		}
		return owners;
	}

	public void deleteMedia(CMSMediaItem item) throws DAOException {
		IDAO dao = DataManager.getInstance().getDao();
		if (dao != null) {
			try {
				dao.deleteCMSMediaItem(item);		
				if (item.getFileName() != null) {
					try {
						Path mediaFile = item.getFilePath();
						Files.delete(mediaFile);
						if (Files.exists(mediaFile)) {
							throw new IOException("Cannot delete file " + mediaFile.toAbsolutePath());
						}
					} catch (IOException e) {
						logger.error("Failed to delete media file: " + e.getMessage());
					}
				}
			} catch(RollbackException e) {				
				if(e.getMessage() != null && e.getMessage().toLowerCase().contains("cannot delete or update a parent row")) {
					Messages.error(null, "admin__media_delete_error_inuse", item.getFileName());
				} else {
					logger.error("Error deleting category ", e);
					Messages.error(null, "admin__media_delete_error", item.getFileName(), e.getMessage());
				}
			}

		}
		if(this.selectedMediaItem != null && this.selectedMediaItem.getValue() == item ) {
			this.selectedMediaItem = null;
		}
		reloadMediaList(false);
	}

	public List<CMSMediaItem> getAllMedia() throws DAOException {
		List<CMSMediaItem> items = new ArrayList<>(DataManager.getInstance().getDao().getAllCMSMediaItems());

		if (userBean != null && userBean.getUser() != null && userBean.getUser().isCmsAdmin()) {
			User user = userBean.getUser();
			if (user.hasPriviledgeForAllCategories()) {
				return items;
			} else {
				List<CMSCategory> allowedCategories = user
						.getAllowedCategories(DataManager.getInstance().getDao().getAllCategories());
				items = items.stream()
						.filter(item -> ListUtils.intersection(item.getCategories(), allowedCategories).size() > 0)
						.collect(Collectors.toList());
				return items;
			}
		} else {
			return new ArrayList<>();
		}

	}
	
	public TableDataProvider<TranslatedSelectable<CMSMediaItem>> getDataProvider() {
		return this.dataProvider;
	}

	public List<TranslatedSelectable<CMSMediaItem>> getMediaItems() throws DAOException {
		return this.dataProvider.getPaginatorList();
	}

	public void reloadMediaList() {
		reloadMediaList(true);
	}
	
	public void reloadMediaList(boolean resetCurrentPage) {
		long page = this.dataProvider.getPageNumberCurrent();
		this.dataProvider.resetAll();
		this.dataProvider.getFilter(GENERAL_FILTER).setValue(filter);
		this.dataProvider.getFilter(FILENAME_FILTER).setValue(filenameFilter);
		if(!resetCurrentPage) {			
			this.dataProvider.setTxtMoveTo((int)page);
		}
	}

	/**
	 * Deletes all mediaItems from {@link #mediaItems} which are are marked as
	 * selected. Reloads the media list
	 * 
	 * @throws DAOException
	 */
	public void deleteSelectedItems() throws DAOException {
			Stream<TranslatedSelectable<CMSMediaItem>> stream = this.dataProvider.getPaginatorList().stream();
			if(!isAllSelected()) {
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
	 * Saves all mediaItems from {@link #mediaItems} which are are marked as
	 * selected. Reloads the media list
	 * 
	 * @throws DAOException
	 */
	public void saveSelectedItems() throws DAOException {
			List<CMSMediaItem> itemsToSave = this.dataProvider.getPaginatorList().stream().filter(Selectable::isSelected)
					.map(Selectable::getValue).collect(Collectors.toList());
			for (CMSMediaItem item : itemsToSave) {
				saveMedia(item);
			}
	}

	public static String getMediaUrl(CMSMediaItem item) throws NumberFormatException, ViewerConfigurationException {
		return getMediaUrl(item, null, null);
	}

	/**
	 * @param item
	 * @param width
	 * @param height
	 * @return
	 * @throws ViewerConfigurationException
	 */
	public static String getMediaUrl(CMSMediaItem item, String width, String height)
			throws ViewerConfigurationException {
		if (item == null || StringUtils.isEmpty(item.getFileName())) {
			return "";
		}

		if (item.isHasExportableText()) {
			StringBuilder sbUri = new StringBuilder();
			sbUri.append(DataManager.getInstance().getConfiguration().getRestApiUrl()).append("cms/media/get/item/")
					.append(item.getId());
			return sbUri.toString();
		}

		String url = BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(Optional.ofNullable(item),
				StringUtils.isNotBlank(width) ? Integer.parseInt(width) : 0,
				StringUtils.isNotBlank(height) ? Integer.parseInt(height) : 0);

		return url;
	}

	/**
	 * 
	 * @param item
	 * @return
	 * @throws ViewerConfigurationException
	 */
	public static String getMediaFileAsString(CMSMediaItem item) throws ViewerConfigurationException {
		if (item == null || StringUtils.isEmpty(item.getFileName())) {
			return "";
		}

		StringBuilder sbUri = new StringBuilder();
		sbUri.append(DataManager.getInstance().getConfiguration().getRestApiUrl()).append("cms/media/get/item/")
				.append(item.getId());
		try {
			String ret = Helper.getWebContentGET(sbUri.toString());
			return ret;
		} catch (ClientProtocolException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (HTTPException e) {
			logger.error(e.getMessage(), e);
		}

		return "";
	}

	public static String getMediaPreviewUrl(CMSMediaItem item)
			throws NumberFormatException, ViewerConfigurationException {
		if (item != null && item.getFileName() != null) {
			return getMediaUrl(item, null, "160");
		}
		return "";
	}

	public boolean isImage(CMSMediaItem item) {
		return item.getFileName().matches(getImageFilter());
	}

	public boolean isText(CMSMediaItem item) {
		return !item.getFileName().matches(getImageFilter());
	}

	public void saveMedia(CMSMediaItem media) throws DAOException {
		if (media != null) {
			media.serializeCategories();
			if (media.getId() == null) {
				DataManager.getInstance().getDao().addCMSMediaItem(media);
			} else {
				media.processMediaFile(media.getFilePath());
				DataManager.getInstance().getDao().updateCMSMediaItem(media);
			}
		}
		reloadMediaList(false);
	}

	public static String getFileName(Part filePart) {
		if (filePart != null) {
			// String basename = filePart.getName();
			// if (basename.startsWith(".")) {
			// basename = basename.substring(1);
			// }
			// if (basename.contains("/")) {
			// basename = basename.substring(basename.lastIndexOf("/") + 1);
			// }
			// if (basename.contains("\\")) {
			// basename = basename.substring(basename.lastIndexOf("\\") + 1);
			// }
			//
			// return basename;
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
	 * @param selectedTag the selectedTag to set
	 */
	public void setSelectedTag(String selectedTag) {
		this.selectedTag = selectedTag;
	}

	/**
	 * @return the selectedTag
	 */
	public String getSelectedTag() {
		return selectedTag;
	}

	/**
	 * @return
	 * @throws DAOException
	 */
	public List<CMSCategory> getAllMediaCategories() throws DAOException {
		return DataManager.getInstance().getDao().getAllCategories();
	}

	public Collection<CMSMediaItem.DisplaySize> getMediaItemDisplaySizes() {
		Set<CMSMediaItem.DisplaySize> sizes = EnumSet.allOf(CMSMediaItem.DisplaySize.class);
		return sizes;
	}

	/**
	 * 
	 * @return a regex matching only filenames ending with one of the supported
	 *         image format suffixes
	 */
	public String getImageFilter() {
		return "(?i).*\\.(png|jpe?g|gif|tiff?|jp2)";
	}
	
	public String getDocumentFilter() {
		return "(?i).*\\.(docx?|rtf|x?h?tml|txt)";
	}

	/**
	 * @return the filter
	 */
	public String getFilter() {
		return filter;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(String filter) {
		if(!this.filter.equals(filter)) {			
			this.filter = filter == null ? "" : filter;
			reloadMediaList(true);
		}
	}
	
	/**
	 * @return the filter
	 */
	public String getFilenameFilter() {
		return filenameFilter;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilenameFilter(String filter) {
		if(!this.filenameFilter.equals(filter)) {			
			this.filenameFilter = filter == null ? "" : filter;
			reloadMediaList(true);
		}
	}
	
	/**
	 * @param selectedMediaItem the selectedMediaItem to set
	 */
	public void setSelectedMediaItem(TranslatedSelectable<CMSMediaItem> selectedMediaItem) {
		this.selectedMediaItem = selectedMediaItem;
	}
	
	public void toggleSelectedMediaItem(TranslatedSelectable<CMSMediaItem> selectedMediaItem) {
		if(this.selectedMediaItem == selectedMediaItem) {
			setSelectedMediaItem(null);
		} else {			
			setSelectedMediaItem(selectedMediaItem);
		}
	}
	
	/**
	 * @return the selectedMediaItem
	 */
	public TranslatedSelectable<CMSMediaItem> getSelectedMediaItem() {
		return selectedMediaItem;
	}
	
	/**
	 * @param allSelected the allSelected to set
	 */
	public void setAllSelected(boolean allSelected) {
		this.allSelected = allSelected;
	}
	
	/**
	 * @return the allSelected
	 */
	public boolean isAllSelected() {
		return allSelected;
	}
	
	/**
	 * 
	 * @return true if there is more than one page in the data-provider. False otherwise
	 */
	public boolean needsPaginator() {
		if(this.getDataProvider() != null) {
			return this.getDataProvider().getLastPageNumber() > 0;
		} else {
			return false;
		}
	}

}
