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
package de.intranda.digiverso.presentation.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSCategory;
import de.intranda.digiverso.presentation.model.cms.CMSContentItem;
import de.intranda.digiverso.presentation.model.cms.CMSMediaItem;
import de.intranda.digiverso.presentation.model.cms.CMSPage;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement;

/**
 * Includes methods to convert deprecated database structures into a form
 * compatible with the current viewer version
 * 
 * @author florian
 *
 */
public class DatabaseUpdater {

	/**
	 * Separates the individual classifications in the classification string
	 */
	private static final String CLASSIFICATION_SEPARATOR = "::";

	private static final Logger logger = LoggerFactory.getLogger(DatabaseUpdater.class);

	private final IDAO dao;

	/**
	 * 
	 */
	public DatabaseUpdater(IDAO dao) {
		this.dao = dao;
	}

	public void update() throws DAOException {
		createDiscriminatorRow();
		convertCMSCategories();
	}

	/**
	 * 
	 * @throws DAOException
	 * 
	 */
	@SuppressWarnings({ "unchecked" })
	private void convertCMSCategories() throws DAOException {
		Map<String, Map<String, List<Long>>> entityMap = new HashMap<>();
		try {
			if (dao.tableExists("cms_page_classifications")) {
				dao.startTransaction();
				String query = "SELECT * FROM cms_page_classifications";
				Query qClassifications = dao.createNativeQuery(query);
				List<Object[]> classificationResults = qClassifications.getResultList();
				Map<String, List<Long>> classifications = classificationResults.stream()
						.collect(Collectors.toMap(array -> (String) array[1],
								array -> new ArrayList<Long>(Arrays.asList((Long) array[0])), (list1, list2) -> {
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
				Map<String, List<Long>> tags = tagResults.stream().filter(array -> !array[1].equals("-")).collect(Collectors.toMap(array -> (String) array[1],
						array -> new ArrayList<Long>(Arrays.asList((Long) array[0])), (list1, list2) -> {
							list1.addAll(list2);
							return list1;
						}));
				entityMap.put("page", tags);
				dao.commitTransaction();
			}

			if (dao.tableExists("cms_content_items")) {
				dao.startTransaction();
				String query = "SELECT cms_content_item_id, allowed_tags FROM cms_content_items";
				Query qClassifications = dao.createNativeQuery(query);
				List<Object[]> classificationsResults = qClassifications.getResultList();
				Map<String, List<Long>> classifications = classificationsResults.stream()
						.collect(Collectors.toMap(array -> (String) array[1],
								array -> new ArrayList<Long>(Arrays.asList((Long) array[0])), (list1, list2) -> {
									list1.addAll(list2);
									return list1;
								}));
				entityMap.put("content", classifications);
				dao.commitTransaction();
			}
			List<CMSCategory> categories = createCategories(entityMap);
			synchronizeWithDatabase(categories);
			
			dao.startTransaction();
			
			linkToPages(categories, entityMap.get("page"));
			linkToContentItems(categories, entityMap.get("content"));
			linkToMedia(categories, entityMap.get("media"));
			
			dao.createNativeQuery("DROP TABLE cms_media_item_tags").executeUpdate();
			dao.createNativeQuery("DOP TABLE cms_page_classifications").executeUpdate();
			
			dao.commitTransaction();
			
			
			
		} catch (PersistenceException | SQLException e) {
			throw new DAOException(e.toString());
		}
	}

	/**
	 * @param categories
	 * @param map
	 */
	private void linkToPages(List<CMSCategory> categories, Map<String, List<Long>> map) {
		for (String categoryName : map.keySet()) {
			CMSCategory category = categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
					.findFirst().orElse(null);
			if (category == null) {
				logger.error("Error creating categories. No category by name {} found or created", categoryName);
			} else {
				List<Long> pageIds = map.get(categoryName);
				for (Long pageId : pageIds) {
					try {
						CMSPage page = dao.getCMSPage(pageId);
						if (page == null) {
							throw new DAOException("No page found by Id " + pageId);
						} else {
							page.addCategory(category);
						}
					} catch (DAOException e) {
						logger.error("Error getting pages for category {}. Failed to load page for id {}", categoryName,
								pageId);
					}
				}
			}
		}
	}
	
	private void linkToMedia(List<CMSCategory> categories, Map<String, List<Long>> map) {
		for (String categoryName : map.keySet()) {
			CMSCategory category = categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
					.findFirst().orElse(null);
			if (category == null) {
				logger.error("Error creating categories. No category by name {} found or created", categoryName);
			} else {
				List<Long> mediaIds = map.get(categoryName);
				for (Long mediaId : mediaIds) {
					try {
						CMSMediaItem media = dao.getCMSMediaItem(mediaId);
						if (media == null) {
							throw new DAOException("No page found by Id " + mediaId);
						} else {
							media.addCategory(category);
						}
					} catch (DAOException e) {
						logger.error("Error getting mediaItems for category {}. Failed to load mediaItem for id {}", categoryName,
								mediaId);
					}
				}
			}
		}
	}
	
	private void linkToContentItems(List<CMSCategory> categories, Map<String, List<Long>> map) {
		for (String categoryName : map.keySet()) {
			CMSCategory category = categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
					.findFirst().orElse(null);
			if (category == null) {
				logger.error("Error creating categories. No category by name {} found or created", categoryName);
			} else {
				List<Long> itemIds = map.get(categoryName);
				for (Long itemId : itemIds) {
					try {
						Query query = dao.createQuery("SELECT item FROM CMSContentItem WHERE item.id = :itemId");
						query.setParameter("itemId", itemId);
						CMSContentItem item = (CMSContentItem)query.getSingleResult();
						if (item == null) {
							throw new DAOException("No item found by Id " + itemId);
						} else {
							item.addCategory(category);
						}
					} catch (DAOException e) {
						logger.error("Error getting mediaItems for category {}. Failed to load contentItem for id {}", categoryName,
								itemId);
					}
				}
			}
		}
	}

	/**
	 * @param categories
	 * @throws DAOException
	 */
	private void synchronizeWithDatabase(List<CMSCategory> categories) throws DAOException {
		ListIterator<CMSCategory> iterator = categories.listIterator();
		while (iterator.hasNext()) {
			CMSCategory category = iterator.next();
			if (dao.getCategoryByName(category.getName()) == null) {
				dao.addCategory(category);
			} else {
				iterator.set(dao.getCategoryByName(category.getName()));
			}
		}
	}

	/**
	 * @param collect
	 * @return
	 */
	protected List<CMSCategory> createCategories(Map<String, Map<String, List<Long>>> entityMap) {
		return entityMap.values().stream().flatMap(map -> map.keySet().stream())
				.flatMap(name -> Arrays.stream(name.split("::"))).filter(name -> StringUtils.isNotBlank(name))
				.distinct().map(name -> new CMSCategory(name)).collect(Collectors.toList());
	}

	/**
	 * Merges the categories from the second map into the first map under the name
	 * given in entityName
	 * 
	 * @param entityMap
	 * @param classifications
	 * @param page
	 */
	private void populateCategoryMap(Map<String, Map<String, List<Long>>> entityMap, Map<String, List<Long>> categories,
			String entityName) {
		for (String category : categories.keySet()) {
			Map<String, List<Long>> mappings = entityMap.get(category);
			if (mappings == null) {
				mappings = new HashMap<>();
				entityMap.put(category, mappings);
			}
			mappings.put(entityName, categories.get(category));
		}
	}

	/**
	 * @throws DAOException
	 * 
	 */
	private void createDiscriminatorRow() throws DAOException {
		dao.startTransaction();
		try {
			Query q = dao.createQuery("UPDATE CMSSidebarElement element SET element.widgetType = '"
					+ CMSSidebarElement.class.getSimpleName() + "' WHERE element.widgetType IS NULL");
			q.executeUpdate();
		} finally {
			dao.commitTransaction();
		}

	}

}
