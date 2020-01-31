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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSContentItem;
import io.goobi.viewer.model.cms.CMSMediaItem;
import io.goobi.viewer.model.cms.CMSPage;

/**
 * Converts all Tags and Classifications from previous viewer-cms versions to
 * the {@link io.goobi.viewer.model.cms.CMSCategory} system. This includes updating references in CMSPages
 * CMSContentItems and CMSMediaItems to keep the existing structure intact
 *
 * @author florian
 */
public class CMSCategoryUpdate implements IModelUpdate {

	/**
	 * Separates the individual classifications in the classification string
	 */
	private static final String CLASSIFICATION_SEPARATOR_REGEX = "::|\\$";

	private static final Logger logger = LoggerFactory.getLogger(DatabaseUpdater.class);

	protected Map<String, Map<String, List<Long>>> entityMap = null;
	protected List<CMSMediaItem> media = null;
	protected List<CMSPage> pages = null;
	protected List<CMSContentItem> content = null;
	protected List<CMSCategory> categories = null;

	/** {@inheritDoc} */
	@Override
	public boolean update(IDAO dao) throws DAOException, SQLException {
		loadData(dao);
		if (convertData()) {
			persistData(dao);
			return true;
		}

		return false;
	}

	/**
	 * <p>persistData.</p>
	 *
	 * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
	 * @throws io.goobi.viewer.exceptions.DAOException if any.
	 */
	public void persistData(IDAO dao) throws DAOException {

		if (this.entityMap == null || this.media == null || this.pages == null || this.content == null
				|| this.categories == null) {
			throw new IllegalStateException("Muse successfully run loadData() before calling persistData()");
		}

		for (CMSCategory category : categories) {
			if (category.getId() == null) {
				dao.addCategory(category);
			}
		}

		for (CMSMediaItem cmsMediaItem : media) {
			dao.updateCMSMediaItem(cmsMediaItem);
		}

		for (CMSPage cmsPage : pages) {
			dao.updateCMSPage(cmsPage);
		}

		dao.startTransaction();
		if (this.entityMap.containsKey("media")) {
			dao.createNativeQuery("DROP TABLE cms_media_item_tags").executeUpdate();
		}
		if (this.entityMap.containsKey("page")) {
			dao.createNativeQuery("DROP TABLE cms_page_classifications").executeUpdate();
		}
		if (this.entityMap.containsKey("content")) {
			dao.createNativeQuery("ALTER TABLE cms_content_items DROP COLUMN allowed_tags").executeUpdate();
			dao.createNativeQuery("ALTER TABLE cms_content_items DROP COLUMN page_classification").executeUpdate();
		}
		dao.commitTransaction();
	}

	/**
	 * <p>loadData.</p>
	 *
	 * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
	 * @throws io.goobi.viewer.exceptions.DAOException if any.
	 * @throws java.sql.SQLException if any.
	 */
	public void loadData(IDAO dao) throws DAOException, SQLException {

		this.entityMap = createEntityMap(dao);
		this.media = dao.getAllCMSMediaItems();
		this.pages = dao.getAllCMSPages();
		this.categories = dao.getAllCategories();
		this.content = this.pages.stream().flatMap(page -> page.getGlobalContentItems().stream())
				.collect(Collectors.toList());
	}

	/**
	 * <p>convertData.</p>
	 *
	 * @return a boolean.
	 * @throws io.goobi.viewer.exceptions.DAOException if any.
	 */
	public boolean convertData() throws DAOException {

		if (this.entityMap == null || this.media == null || this.pages == null || this.content == null
				|| this.categories == null) {
			throw new IllegalStateException("Must successfully run loadData() before calling convertData()");
		}

		if (!this.entityMap.containsKey("media") && !this.entityMap.containsKey("content")
				&& !this.entityMap.containsKey("page")) {
			// no update required
			return false;
		}

		List<CMSCategory> categories = createCategories(entityMap);
		this.categories = synchronize(categories, this.categories);

		linkToPages(this.categories, entityMap.get("page"), this.pages);
		linkToContentItems(categories, entityMap.get("content"), this.content);
		linkToMedia(categories, entityMap.get("media"), this.media);

		return true;
	}

	/**
	 * @param dao
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Map<String, List<Long>>> createEntityMap(IDAO dao) throws SQLException {
		Map<String, Map<String, List<Long>>> entityMap = new HashMap<>();
		if (dao.tableExists("cms_page_classifications")) {
			dao.startTransaction();
			String query = "SELECT * FROM cms_page_classifications";
			Query qClassifications = dao.createNativeQuery(query);
			List<Object[]> classificationResults = qClassifications.getResultList();
			Map<String, List<Long>> classifications = classificationResults.stream()
					.collect(Collectors.toMap(array -> (String) array[1],
							array -> new ArrayList<>(Arrays.asList((Long) array[0])), (list1, list2) -> {
								list1.addAll(list2);
								return list1;
							}));
			entityMap.put("page", classifications);
			dao.commitTransaction();
		}

		if (dao.tableExists("cms_media_item_tags")) {
			dao.startTransaction();
			String query = "SELECT * FROM cms_media_item_tags";
			Query qTags = dao.createNativeQuery(query);
			List<Object[]> tagResults = qTags.getResultList();
			Map<String, List<Long>> tags = tagResults.stream().collect(Collectors.toMap(array -> (String) array[1],
					array -> new ArrayList<>(Arrays.asList((Long) array[0])), (list1, list2) -> {
						list1.addAll(list2);
						return list1;
					}));
			entityMap.put("media", tags);
			dao.commitTransaction();
		}

		if (dao.columnsExists("cms_content_items", "allowed_tags")
				&& dao.columnsExists("cms_content_items", "page_classification")) {
			dao.startTransaction();
			String query = "SELECT cms_content_item_id, allowed_tags, page_classification FROM cms_content_items";
			Query qClassifications = dao.createNativeQuery(query);
			List<Object[]> classificationsResults = qClassifications.getResultList();
			Map<String, List<Long>> classifications = classificationsResults.stream()
					.map(array -> new Object[] { array[0], (String) array[1] + "$" + (String) array[2] })
					.flatMap(array -> Arrays.stream(((String) array[1]).split(CLASSIFICATION_SEPARATOR_REGEX))
							.filter(string -> StringUtils.isNotBlank(string) && !"-".equals(string)
									&& !"null".equals(string))
							.map(string -> new Object[] { array[0], string }))
					.collect(Collectors.toMap(array -> (String) array[1],
							array -> new ArrayList<>(Arrays.asList((Long) array[0])), (list1, list2) -> {
								list1.addAll(list2);
								return list1;
							}));

			entityMap.put("content", classifications);
			dao.commitTransaction();
		}
		return entityMap;
	}

	/**
	 * @param categories
	 * @param map
	 */
	private static void linkToPages(List<CMSCategory> categories, Map<String, List<Long>> map, List<CMSPage> pages) {
		if (map != null) {
			for (String categoryName : map.keySet()) {
				CMSCategory category = categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
						.findFirst().orElse(null);
				if (category == null) {
					logger.error("Error creating categories. No category by name {} found or created", categoryName);
				} else {
					List<Long> pageIds = map.get(categoryName);
					for (Long pageId : pageIds) {
						if (pageId != null) {
							try {
								CMSPage page = pages.stream().filter(p -> p.getId() != null)
										.filter(p -> pageId.equals(p.getId())).findFirst()
										.orElseThrow(() -> new DAOException("No page found by Id " + pageId));
								page.addCategory(category);
							} catch (DAOException e) {
								logger.warn("Error getting pages for category {}. Failed to load page for id {}",
										categoryName, pageId, e);
							}
						}
					}
				}
			}
		}
	}

	private static void linkToMedia(List<CMSCategory> categories, Map<String, List<Long>> map,
			List<CMSMediaItem> mediaItems) {
		if (map != null) {
			for (String categoryName : map.keySet()) {
				CMSCategory category = categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
						.findFirst().orElse(null);
				if (category == null) {
					logger.error("Error creating categories. No category by name {} found or created", categoryName);
				} else {
					List<Long> mediaIds = map.get(categoryName);
					for (Long mediaId : mediaIds) {
						if (mediaId != null) {
							try {
								CMSMediaItem media = mediaItems.stream()
										.filter(p -> p.getId() != null)
										.filter(p -> mediaId.equals(p.getId())).findFirst()
										.orElseThrow(() -> new DAOException("No mediaItem found by Id " + mediaId));
								if (media == null) {
									throw new DAOException("No page found by Id " + mediaId);
								}
								media.addCategory(category);
							} catch (DAOException e) {
								logger.error(
										"Error getting mediaItems for category {}. Failed to load mediaItem for id {}",
										categoryName, mediaId);
							}
						}
					}
				}
			}
		}
	}

	private static void linkToContentItems(List<CMSCategory> categories, Map<String, List<Long>> map,
			List<CMSContentItem> items) {
		if (map != null) {
			for (String categoryName : map.keySet()) {
				CMSCategory category = categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
						.findFirst().orElse(null);
				if (category == null) {
					logger.error("Error creating categories. No category by name {} found or created", categoryName);
				} else {
					List<Long> itemIds = map.get(categoryName);
					for (Long itemId : itemIds) {
						try {
							CMSContentItem item = items.stream().filter(i -> itemId.equals(i.getId())).findFirst()
									.orElseThrow(() -> new DAOException("No contentItem found by Id " + itemId));
							item.addCategory(category);
						} catch (DAOException e) {
							logger.error(
									"Error getting mediaItems for category {}. Failed to load contentItem for id {}",
									categoryName, itemId);
						}
					}
				}
			}
		}
	}

	/**
	 * @param categories
	 * @throws DAOException
	 */
	private static List<CMSCategory> synchronize(List<CMSCategory> categories, List<CMSCategory> existingCategories)
			throws DAOException {
		ListIterator<CMSCategory> iterator = categories.listIterator();
		while (iterator.hasNext()) {
			CMSCategory category = iterator.next();
			Optional<CMSCategory> existingCategory = existingCategories.stream()
					.filter(cat -> cat.getName().equalsIgnoreCase(category.getName())).findFirst();
			if (existingCategory.isPresent()) {
				iterator.set(existingCategory.get());
			} else {
				existingCategories.add(category);
			}
		}
		return existingCategories;
	}

	/**
	 * <p>createCategories.</p>
	 *
	 * @param entityMap a {@link java.util.Map} object.
	 * @return a {@link java.util.List} object.
	 */
	protected List<CMSCategory> createCategories(Map<String, Map<String, List<Long>>> entityMap) {
		return entityMap.values().stream().flatMap(map -> map.keySet().stream())
				.flatMap(name -> Arrays.stream(name.split(CLASSIFICATION_SEPARATOR_REGEX)))
				.filter(name -> StringUtils.isNotBlank(name)).filter(name -> !name.equals("-")).map(String::toLowerCase)
				.distinct().map(name -> new CMSCategory(name)).collect(Collectors.toList());
	}

}
