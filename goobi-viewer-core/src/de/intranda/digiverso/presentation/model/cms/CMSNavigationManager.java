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
package de.intranda.digiverso.presentation.model.cms;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.faces.validators.CMSTitleValidator;

public class CMSNavigationManager {

    private static final Logger logger = LoggerFactory.getLogger(CMSNavigationManager.class);
    private static volatile long availableItemIdCounter;

    private List<CMSNavigationItem> availableItems;
    private List<CMSNavigationItem> visibleItems;

    //    public static CMSNavigationManager getInstance() {
    //        return instance;
    //    }

    public CMSNavigationManager() {
        try {
            loadItems();
        } catch (DAOException e) {
            logger.error("Could not load items: {}", e.getMessage());
        }
    }

    public final List<CMSNavigationItem> loadItems() throws DAOException {
        logger.trace("Loading available navigation items");
        availableItems = new ArrayList<>();
        availableItemIdCounter = 0;
        CMSNavigationItem start = new CMSNavigationItem("index", "home");
        addAvailableItem(start);
        CMSNavigationItem search = new CMSNavigationItem("search", "search");
        addAvailableItem(search);
        CMSNavigationItem searchadvanced = new CMSNavigationItem("searchadvanced", "searchAdvanced");
        addAvailableItem(searchadvanced);
        CMSNavigationItem searchtimeline = new CMSNavigationItem("searchtimeline", "searchTimeline");
        addAvailableItem(searchtimeline);
        CMSNavigationItem searchcalendar = new CMSNavigationItem("searchcalendar", "searchCalendar");
        addAvailableItem(searchcalendar);
        CMSNavigationItem browse = new CMSNavigationItem("browse", "browse");
        addAvailableItem(browse);
        CMSNavigationItem timematrix = new CMSNavigationItem("timematrix", "timematrix");
        addAvailableItem(timematrix);
        CMSNavigationItem statistics = new CMSNavigationItem("statistics", "statistics");
        addAvailableItem(statistics);
        CMSNavigationItem tags = new CMSNavigationItem("tags", "tagclouds");
        addAvailableItem(tags);
        CMSNavigationItem user = new CMSNavigationItem("user", "user");
        user.setDisplayForUsersOnly(true);
        addAvailableItem(user);

        addModuleItems();
        addCMSPageItems();
        
        visibleItems = loadVisibleItems();
        return availableItems;
    }

    /**
     * @throws DAOException
     */
    public void addCMSPageItems() throws DAOException {
        List<CMSPage> cmsPages = DataManager.getInstance().getDao().getAllCMSPages();
        for (CMSPage cmsPage : cmsPages) {
            if (cmsPage != null && PageValidityStatus.VALID.equals(cmsPage.getValidityStatus())) {
                CMSNavigationItem item = new CMSNavigationItem(cmsPage);
                addAvailableItem(item);
            }
        }
    }

    /**
     * 
     */
    private void addModuleItems() {
        DataManager.getInstance().getModules().stream()
        .flatMap(module -> module.getCmsMenuContributions().entrySet().stream())
        .map(entry -> new CMSNavigationItem(entry.getValue(), entry.getKey()))
        .forEach(item -> addAvailableItem(item));

    }

    public void addAvailableItem(CMSNavigationItem item) {
        if (!availableItems.contains(item)) {
            item.setAvailableItemId(++availableItemIdCounter);
            availableItems.add(item);
        }
    }

    public List<CMSNavigationItem> getAvailableItems() {
        return availableItems;
    }

    /**
     * @return the sublist of available menu items matching the navigationMenuItem-list from the database (in label and url)
     * @throws DAOException
     */
    public final List<CMSNavigationItem> loadVisibleItems() throws DAOException {
        List<CMSNavigationItem> daoList = DataManager.getInstance().getDao().getAllTopCMSNavigationItems();
        setVisibleItems(daoList);
        return visibleItems;
    }

    public List<CMSNavigationItem> getVisibleItems() {
        return visibleItems;
    }

    private void addToList(List<CMSNavigationItem> list, CMSNavigationItem daoItem) {
        list.add(getAvailableItem(daoItem));
        for (CMSNavigationItem child : daoItem.getChildItems()) {
            addToList(list, child);
        }

    }

    /**
     * Adds the given item, along with all their descendants to the visible item list
     *
     * @param items
     */
    public void setVisibleItems(List<CMSNavigationItem> items) {
        List<CMSNavigationItem> visibleList = new ArrayList<>(items.size());
        for (CMSNavigationItem item : items) {
            addToList(visibleList, item);
        }
        this.visibleItems = visibleList;
    }

    /**
     * Replaces the complete navigation-item database table with the elements of 'visibleItems'
     *
     * @param selectedItems
     * @throws DAOException
     */
    public void saveVisibleItems() throws DAOException {
        List<CMSNavigationItem> oldItems = DataManager.getInstance().getDao().getAllTopCMSNavigationItems();
        for (CMSNavigationItem oldItem : oldItems) {
            deleteItem(oldItem);
        }
        for (CMSNavigationItem item : getVisibleItems()) {
            if (item.getLevel() == 0) {
                addItem(item);
            }
        }
    }

    private static void addItem(CMSNavigationItem item) throws DAOException {
        if (item.getId() != null && DataManager.getInstance().getDao().getCMSNavigationItem(item.getId()) != null) {
            DataManager.getInstance().getDao().updateCMSNavigationItem(item);
        } else {
            DataManager.getInstance().getDao().addCMSNavigationItem(item);
        }
    }

    /**
     * Deletes a navigationitem and all its children from the database
     *
     * @throws DAOException
     */
    private void deleteItem(CMSNavigationItem oldItem) throws ConcurrentModificationException, DAOException {
        if (oldItem.getChildItems() != null && !oldItem.getChildItems().isEmpty()) {
            List<CMSNavigationItem> itemsToDelete = new ArrayList<>();
            List<CMSNavigationItem> deletedItems = new ArrayList<>();
            try {
                for (CMSNavigationItem child : oldItem.getChildItems()) {
                    //                deleteItem(child);
                    itemsToDelete.add(child);
                }
                for (CMSNavigationItem child : itemsToDelete) {
                    deleteItem(child);
                    deletedItems.add(child);
                }
            } catch (ConcurrentModificationException e) {
                for (CMSNavigationItem deletedItem : deletedItems) {
                    addItem(deletedItem);
                }
                throw e;
            }
        }
        DataManager.getInstance().getDao().deleteCMSNavigationItem(oldItem);

    }

    public CMSNavigationItem getAvailableItem(String id) {
        for (CMSNavigationItem availableItem : getAvailableItems()) {
            if (availableItem.getAvailableItemId().equals(Long.parseLong(id))) {
                return availableItem;
            }
        }
        return null;
    }

    /**
     * supplies the item with the availableItemId of the matching available item. If no such item exists, one is created
     *
     * @param item
     * @return the item itself
     */
    public CMSNavigationItem getAvailableItem(CMSNavigationItem item) {
        for (CMSNavigationItem availableItem : getAvailableItems()) {
            if (item.getItemLabel().equals(availableItem.getItemLabel()) && item.getPageUrl().equals(availableItem.getPageUrl())) {
                item.setAvailableItemId(availableItem.getAvailableItemId());
                return item;
            }
        }
        addAvailableItem(item);
        return item;
    }

    /**
     * @param navigationItem
     */
    public void addVisibleItem(CMSNavigationItem navigationItem) {
        if (navigationItem.getItemLabel() == null || navigationItem.getPageUrl() == null) {
            logger.error("Missing fields of navigation item: Cannot save");
        } else {
            visibleItems.add(0, navigationItem);
            addAvailableItem(navigationItem);
        }
    }

    /**
     * @throws DAOException
     *
     */
    public void reload() throws DAOException {
        loadItems();
    }
}
