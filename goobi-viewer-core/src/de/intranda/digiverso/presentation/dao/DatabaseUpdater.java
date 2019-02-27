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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement;

/**
 * Includes methods to convert deprecated database structures into a form
 * compatible with the current viewer version
 * 
 * @author florian
 *
 */
public class DatabaseUpdater {

	private final EntityManager em;
	private final IDAO dao;

	/**
	 * 
	 */
	public DatabaseUpdater(IDAO dao, EntityManager em) {
		this.em = em;
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
		em.getTransaction().begin();
		try {
			if (tableExists("cms_page_classifications")) {
				Query qClassifications = em.createNativeQuery("SELECT * FROM cms_page_classifications");
				List<Object[]> classificationResults = qClassifications.getResultList();
				Map<String, List<Long>> classifications = classificationResults.stream()
						.collect(Collectors.toMap(array -> (String) array[1],
								array -> new ArrayList<Long>(Arrays.asList((Long) array[0])), (list1, list2) -> {
									list1.addAll(list2);
									return list1;
								}));
				populateCategoryMap(entityMap, classifications, "page");
			}
			
			if (tableExists("cms_media_item_tags")) {
				Query qTags = em.createNativeQuery("SELECT * FROM cms_media_item_tags");
				List<Object[]> tagResults = qTags.getResultList();
				Map<String, List<Long>> tags = tagResults.stream().collect(Collectors.toMap(array -> (String) array[1],
						array -> new ArrayList<Long>(Arrays.asList((Long) array[0])), (list1, list2) -> {
							list1.addAll(list2);
							return list1;
						}));
				populateCategoryMap(entityMap, tags, "media");
			}
			
			if (tableExists("cms_media_item_tags")) {
				Query qTags = em.createNativeQuery("SELECT * FROM cms_media_item_tags");
				List<Object[]> tagResults = qTags.getResultList();
				Map<String, List<Long>> tags = tagResults.stream().collect(Collectors.toMap(array -> (String) array[1],
						array -> new ArrayList<Long>(Arrays.asList((Long) array[0])), (list1, list2) -> {
							list1.addAll(list2);
							return list1;
						}));
				populateCategoryMap(entityMap, tags, "media");
			}
			
		} catch (PersistenceException | SQLException e) {
			throw new DAOException(e.toString());
		} finally {
			em.getTransaction().commit();
		}
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
	 * @param string
	 * @return
	 * @throws SQLException
	 */
	private boolean tableExists(String tableName) throws SQLException {
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		Connection connection = em.unwrap(Connection.class);
		DatabaseMetaData metaData = connection.getMetaData();
		try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
			return tables.next();
		} finally {
			transaction.commit();
		}
	}

	/**
	 * @throws DAOException
	 * 
	 */
	private void createDiscriminatorRow() throws DAOException {
		em.getTransaction().begin();
		try {
			Query q = em.createQuery("UPDATE CMSSidebarElement element SET element.widgetType = '"
					+ CMSSidebarElement.class.getSimpleName() + "' WHERE element.widgetType IS NULL");
			q.executeUpdate();
		} finally {
			em.getTransaction().commit();
		}

	}

}
