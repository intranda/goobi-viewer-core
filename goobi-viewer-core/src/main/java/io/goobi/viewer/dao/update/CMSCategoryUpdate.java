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
package io.goobi.viewer.dao.update;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.CMSCategoryHolder;
import io.goobi.viewer.model.cms.pages.content.CMSContent;

/**
 * Converts all Tags and Classifications from previous viewer-cms versions to the {@link io.goobi.viewer.model.cms.CMSCategory} system. This includes
 * updating references in CMSPages CMSContentItems and CMSMediaItems to keep the existing structure intact
 *
 * @author florian
 */
public class CMSCategoryUpdate implements IModelUpdate {

    private static final String MAP_KEY_CONTENT = "content";

    private static final String TABLENAME_CMS_CONTENT_ITEMS = "cms_content_items";

    private static final String MAP_KEY_CMS_PAGE = "page";

    private static final String MAP_KEY_MEDIA_ITEMS = "media";

    /**
     * Separates the individual classifications in the classification string
     */
    private static final String CLASSIFICATION_SEPARATOR_REGEX = "::|\\$";

    private static final Logger logger = LogManager.getLogger(CMSCategoryUpdate.class);

    protected Map<String, Map<String, List<Long>>> entityMap = null;
    protected List<CMSMediaItem> media = null;
    protected List<CMSPage> pages = null;
    protected List<CMSContent> content = null;
    protected List<CMSCategory> categories = null;

    /** {@inheritDoc} */
    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        loadData(dao);
        if (convertData()) {
            persistData(dao);
            return true;
        }

        return false;
    }

    /**
     * <p>
     * persistData.
     * </p>
     *
     * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void persistData(IDAO dao) throws DAOException {

        if (this.entityMap == null || this.media == null || this.pages == null || this.content == null || this.categories == null) {
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

        if (this.entityMap.containsKey(MAP_KEY_MEDIA_ITEMS)) {
            dao.executeUpdate("DROP TABLE cms_media_item_tags");
        }
        if (this.entityMap.containsKey(MAP_KEY_CMS_PAGE)) {
            dao.executeUpdate("DROP TABLE cms_page_classifications");
        }
        try {
            if (dao.tableExists(TABLENAME_CMS_CONTENT_ITEMS) && this.entityMap.containsKey(MAP_KEY_CONTENT)) {
                dao.executeUpdate("ALTER TABLE cms_content_items DROP COLUMN allowed_tags");
                dao.executeUpdate("ALTER TABLE cms_content_items DROP COLUMN page_classification");
            }
        } catch (SQLException e) {
            throw new DAOException(e.toString());
        }
    }

    /**
     * <p>
     * loadData.
     * </p>
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
        this.content = this.pages.stream()
                .flatMap(page -> page.getPersistentComponents().stream())
                .flatMap(c -> c.getContentItems().stream())
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * convertData.
     * </p>
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean convertData() throws DAOException {

        if (this.entityMap == null || this.media == null || this.pages == null || this.content == null || this.categories == null) {
            throw new IllegalStateException("Must successfully run loadData() before calling convertData()");
        }

        if (!this.entityMap.containsKey(MAP_KEY_MEDIA_ITEMS) && !this.entityMap.containsKey(MAP_KEY_CONTENT)
                && !this.entityMap.containsKey(MAP_KEY_CMS_PAGE)) {
            // no update required
            return false;
        }

        List<CMSCategory> cats = createCategories(entityMap);
        synchronize(cats, this.categories);

        linkToPages(this.categories, entityMap.get(MAP_KEY_CMS_PAGE), this.pages);
        linkToContentItems(cats, entityMap.get(MAP_KEY_CONTENT), this.content);
        linkToMedia(cats, entityMap.get(MAP_KEY_MEDIA_ITEMS), this.media);

        return true;
    }

    /**
     * @param dao
     * @throws SQLException
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, List<Long>>> createEntityMap(IDAO dao) throws SQLException, DAOException {
        Map<String, Map<String, List<Long>>> entityMap = new HashMap<>();
        if (dao.tableExists("cms_page_classifications")) {
            String query = "SELECT * FROM cms_page_classifications";
            List<Object[]> classificationResults = dao.getNativeQueryResults(query);
            Map<String, List<Long>> classifications = classificationResults.stream()
                    .collect(
                            Collectors.toMap(array -> (String) array[1], array -> new ArrayList<>(Arrays.asList((Long) array[0])), (list1, list2) -> {
                                list1.addAll(list2);
                                return list1;
                            }));
            entityMap.put(MAP_KEY_CMS_PAGE, classifications);
        }

        if (dao.tableExists("cms_media_item_tags")) {
            String query = "SELECT * FROM cms_media_item_tags";
            List<Object[]> tagResults = dao.getNativeQueryResults(query);
            Map<String, List<Long>> tags = tagResults.stream()
                    .collect(
                            Collectors.toMap(array -> (String) array[1], array -> new ArrayList<>(Arrays.asList((Long) array[0])), (list1, list2) -> {
                                list1.addAll(list2);
                                return list1;
                            }));
            entityMap.put(MAP_KEY_MEDIA_ITEMS, tags);
        }

        if (dao.columnsExists(TABLENAME_CMS_CONTENT_ITEMS, "allowed_tags") && dao.columnsExists(TABLENAME_CMS_CONTENT_ITEMS, "page_classification")) {
            String query = "SELECT cms_content_item_id, allowed_tags, page_classification FROM cms_content_items";
            List<Object[]> classificationsResults = dao.getNativeQueryResults(query);
            Map<String, List<Long>> classifications = classificationsResults.stream()
                    .map(array -> new Object[] { array[0], (String) array[1] + "$" + (String) array[2] })
                    .flatMap(array -> Arrays.stream(((String) array[1]).split(CLASSIFICATION_SEPARATOR_REGEX))
                            .filter(string -> StringUtils.isNotBlank(string) && !"-".equals(string) && !"null".equals(string))
                            .map(string -> new Object[] { array[0], string }))
                    .collect(
                            Collectors.toMap(array -> (String) array[1], array -> new ArrayList<>(Arrays.asList((Long) array[0])), (list1, list2) -> {
                                list1.addAll(list2);
                                return list1;
                            }));

            entityMap.put(MAP_KEY_CONTENT, classifications);
        }
        return entityMap;
    }

    /**
     * @param categories
     * @param map
     */
    private static void linkToPages(List<CMSCategory> categories, Map<String, List<Long>> map, List<CMSPage> pages) {
        if (map == null) {
            return;
        }

        for (Entry<String, List<Long>> entry : map.entrySet()) {
            categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(entry.getKey())).findFirst().ifPresent(category -> {
                List<Long> pageIds = entry.getValue();
                for (Long pageId : pageIds) {
                    if (pageId != null) {
                        try {
                            CMSPage page = pages.stream()
                                    .filter(p -> p.getId() != null)
                                    .filter(p -> pageId.equals(p.getId()))
                                    .findFirst()
                                    .orElseThrow(() -> new DAOException("No page found by Id " + pageId));
                            page.addCategory(category);
                        } catch (DAOException e) {
                            logger.warn("Error getting pages for category {}. Failed to load page for id {}", entry.getKey(), pageId, e);
                        }
                    }
                }
            });
        }
    }

    /**
     * 
     * @param categories
     * @param map
     * @param mediaItems
     */
    private static void linkToMedia(List<CMSCategory> categories, Map<String, List<Long>> map, List<CMSMediaItem> mediaItems) {
        if (map != null) {
            for (Entry<String, List<Long>> entry : map.entrySet()) {
                categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(entry.getKey())).findFirst().ifPresent(category -> {
                    List<Long> mediaIds = entry.getValue();
                    for (Long mediaId : mediaIds) {
                        if (mediaId != null) {
                            mediaItems.stream()
                                    .filter(p -> p.getId() != null)
                                    .filter(p -> mediaId.equals(p.getId()))
                                    .findFirst()
                                    .ifPresent(media -> media.addCategory(category));
                        }
                    }
                });
            }
        }
    }

    /**
     * 
     * @param categories
     * @param map
     * @param items
     */
    private static void linkToContentItems(List<CMSCategory> categories, Map<String, List<Long>> map, List<CMSContent> items) {
        if (map == null) {
            return;
        }

        for (Entry<String, List<Long>> entry : map.entrySet()) {
            CMSCategory category = categories.stream().filter(cat -> cat.getName().equalsIgnoreCase(entry.getKey())).findFirst().orElse(null);
            if (category == null) {
                logger.error("Error creating categories. No category by name {} found or created", entry.getKey());
            } else {
                List<Long> itemIds = entry.getValue();
                for (Long itemId : itemIds) {
                    try {
                        CMSCategoryHolder item = items.stream()
                                .filter(i -> itemId.equals(i.getId()))
                                .filter(CMSCategoryHolder.class::isInstance)
                                .map(CMSCategoryHolder.class::cast)
                                .findFirst()
                                .orElseThrow(() -> new DAOException("No contentItem found by Id " + itemId));
                        item.addCategory(category);
                    } catch (DAOException e) {
                        logger.error("Error getting mediaItems for category {}. Failed to load contentItem for id {}", entry.getKey(), itemId);
                    }
                }
            }
        }
    }

    /**
     * @param categories
     * @throws DAOException
     */
    private static List<CMSCategory> synchronize(List<CMSCategory> categories, List<CMSCategory> existingCategories) {
        ListIterator<CMSCategory> iterator = categories.listIterator();
        while (iterator.hasNext()) {
            CMSCategory category = iterator.next();
            Optional<CMSCategory> existingCategory =
                    existingCategories.stream().filter(cat -> cat.getName().equalsIgnoreCase(category.getName())).findFirst();
            if (existingCategory.isPresent()) {
                iterator.set(existingCategory.get());
            } else {
                existingCategories.add(category);
            }
        }
        return existingCategories;
    }

    /**
     * <p>
     * createCategories.
     * </p>
     *
     * @param entityMap a {@link java.util.Map} object.
     * @return a {@link java.util.List} object.
     */
    protected List<CMSCategory> createCategories(Map<String, Map<String, List<Long>>> entityMap) {
        return entityMap.values()
                .stream()
                .flatMap(map -> map.keySet().stream())
                .flatMap(name -> Arrays.stream(name.split(CLASSIFICATION_SEPARATOR_REGEX)))
                .filter(StringUtils::isNotBlank)
                .filter(name -> !name.equals("-"))
                .map(String::toLowerCase)
                .distinct()
                .map(CMSCategory::new)
                .collect(Collectors.toList());
    }

}
